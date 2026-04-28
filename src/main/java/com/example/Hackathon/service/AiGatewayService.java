package com.example.Hackathon.service;

import com.example.Hackathon.dto.AiClassificationDTO;
import com.example.Hackathon.enums.IssueType;
import com.example.Hackathon.enums.ProductType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * AI Gateway — runs <strong>both</strong> the HF/DistilBERT microservice and Groq, maps labels to
 * DB {@link ProductType}/{@link IssueType}, then picks the classifier with the higher confidence score.
 * Groq enrichment (severity + entity extraction) runs afterward in {@link #classifyWithGroqEnrichment}.
 */
@Service
public class AiGatewayService {

    private final RestTemplate restTemplate;
    private final GroqLlmService groqLlmService;
    private final CategoryMappingService categoryMappingService;

    @Value("${app.ai-service-url}")
    private String aiUrl;

    public AiGatewayService(
            RestTemplate restTemplate,
            GroqLlmService groqLlmService,
            CategoryMappingService categoryMappingService) {
        this.restTemplate = restTemplate;
        this.groqLlmService = groqLlmService;
        this.categoryMappingService = categoryMappingService;
    }

    /**
     * Inbound complaints: fusion classify + Groq post-step for severity and structured extraction.
     */
    public AiClassificationDTO classifyWithGroqEnrichment(String description) {
        AiClassificationDTO base = classify(description);
        try {
            return groqLlmService.enrichAfterClassification(description, base);
        } catch (Exception e) {
            System.out.println("⚠️ Groq enrichment skipped: " + e.getMessage());
            return base;
        }
    }

    public AiClassificationDTO classifyForWhatsApp(String description) {
        return classifyWithGroqEnrichment(description);
    }

    public AiClassificationDTO classifyForEmail(String description) {
        return classifyWithGroqEnrichment(description);
    }

    /**
     * Runs HF + Groq in parallel, normalizes categories to DB enums, keeps the higher-confidence result.
     */
    public AiClassificationDTO classify(String description) {
        AiClassificationDTO groq = null;
        try {
            groq = groqLlmService.classify(description);
            groq = categoryMappingService.normalizeClassification(groq);
        } catch (Exception e) {
            System.out.println("⚠️ Groq classification failed: " + e.getMessage());
        }

        Map<String, Object> hfResponse = null;
        try {
            hfResponse = callDistilBert(description);
        } catch (Exception e) {
            System.out.println("⚠️ HF/DistilBERT service failed: " + e.getMessage());
        }

        if (hfResponse == null && groq == null) {
            return getStubResponse();
        }
        if (hfResponse == null) {
            groq.setClassifiedBy("GROQ_LLM");
            return groq;
        }
        if (groq == null) {
            return buildFromHfResponse(hfResponse);
        }

        String rawLabel = (String) hfResponse.get("label");
        double hfConf = ((Number) hfResponse.get("confidence")).doubleValue();
        ProductType pt = categoryMappingService.mapRawLabelToProductType(rawLabel);
        IssueType it = categoryMappingService.mapRawLabelToIssueType(rawLabel, pt);
        AiClassificationDTO hfDto = buildFromHfModel(pt, it, hfConf);

        double groqConf = groq.getConfidence() != null ? groq.getConfidence() : 0.0;

        System.out.println("🤖 Fusion: HF label=" + rawLabel + " score=" + String.format("%.3f", hfConf)
                + " | Groq product=" + groq.getProductType() + " score=" + String.format("%.3f", groqConf));

        if (hfConf >= groqConf) {
            System.out.println("✅ Fusion: using HF (higher or equal score)");
            return mergeGroqTitleIntoHf(hfDto, groq, "FUSION_HF");
        }
        System.out.println("✅ Fusion: using Groq (higher score)");
        groq.setClassifiedBy("FUSION_GROQ");
        return groq;
    }

    private AiClassificationDTO mergeGroqTitleIntoHf(AiClassificationDTO hf, AiClassificationDTO groq, String classifiedBy) {
        String title = groq.getTitle() != null && !groq.getTitle().isBlank() ? groq.getTitle() : hf.getTitle();
        return AiClassificationDTO.builder()
                .productType(hf.getProductType())
                .issueType(hf.getIssueType())
                .title(title)
                .severity(hf.getSeverity())
                .sentimentLabel(hf.getSentimentLabel())
                .sentimentScore(hf.getSentimentScore())
                .confidence(hf.getConfidence())
                .classifiedBy(classifiedBy)
                .criticalScore(hf.getCriticalScore())
                .regulatoryFlag(hf.getRegulatoryFlag())
                .suggestedResponse(hf.getSuggestedResponse())
                .extractedEntities(hf.getExtractedEntities())
                .extractedName(hf.getExtractedName())
                .extractedEmail(hf.getExtractedEmail())
                .extractedPhone(hf.getExtractedPhone())
                .extractedAccountNumber(hf.getExtractedAccountNumber())
                .build();
    }

    private AiClassificationDTO buildFromHfResponse(Map<String, Object> hfResponse) {
        String rawLabel = (String) hfResponse.get("label");
        double hfConf = ((Number) hfResponse.get("confidence")).doubleValue();
        ProductType pt = categoryMappingService.mapRawLabelToProductType(rawLabel);
        IssueType it = categoryMappingService.mapRawLabelToIssueType(rawLabel, pt);
        return buildFromHfModel(pt, it, hfConf);
    }

    private AiClassificationDTO buildFromHfModel(ProductType pt, IssueType it, double confidence) {
        String severity = mapProductTypeToSeverity(pt);
        String label = pt.name();
        return AiClassificationDTO.builder()
                .productType(pt.name())
                .issueType(it.name())
                .title(mapProductTypeToTitle(pt))
                .severity(severity)
                .sentimentLabel("NEUTRAL")
                .sentimentScore(0.5)
                .confidence(confidence)
                .classifiedBy("DISTILBERT")
                .criticalScore(mapSeverityToCriticalScore(severity))
                .regulatoryFlag(false)
                .suggestedResponse(buildSuggestedResponse(label, severity))
                .extractedEntities("{}")
                .extractedName(null)
                .extractedEmail(null)
                .extractedPhone(null)
                .extractedAccountNumber(null)
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callDistilBert(String description) {
        return restTemplate.postForObject(
                aiUrl + "/classify",
                Map.of("description", description),
                Map.class
        );
    }

    private String mapProductTypeToTitle(ProductType pt) {
        return pt.name().replace("_", " ") + " incident";
    }

    private String mapProductTypeToSeverity(ProductType pt) {
        return switch (pt) {
            case CREDIT_CARD, DEBIT_CARD -> "P2";
            case HOME_LOAN, PERSONAL_LOAN -> "P2";
            case NET_BANKING -> "P3";
            case SAVINGS_ACCOUNT -> "P3";
            default -> "P3";
        };
    }

    private Integer mapSeverityToCriticalScore(String severity) {
        return switch (severity) {
            case "P1" -> 9;
            case "P2" -> 7;
            case "P3" -> 5;
            case "P4" -> 3;
            default -> 5;
        };
    }

    private String buildSuggestedResponse(String label, String severity) {
        String productName = label.replace("_", " ").toLowerCase();
        if ("P1".equals(severity) || "P2".equals(severity)) {
            return "We have received your " + productName + " complaint and marked it as high priority. "
                    + "A specialist will be assigned to your case shortly. We aim to resolve this within 24 hours.";
        }
        return "Thank you for reaching out regarding your " + productName + " concern. "
                + "We have logged your complaint and our team will review it within the SLA timeline.";
    }

    private AiClassificationDTO getStubResponse() {
        return AiClassificationDTO.builder()
                .productType("OTHER")
                .issueType("GENERAL_INQUIRY")
                .severity("P3")
                .sentimentLabel("NEUTRAL")
                .sentimentScore(0.5)
                .confidence(0.0)
                .classifiedBy("FALLBACK")
                .criticalScore(5)
                .regulatoryFlag(false)
                .suggestedResponse("Thank you for reaching out. We have received your complaint "
                        + "and our team is reviewing it. We will get back to you within "
                        + "the specified SLA timeline.")
                .extractedEntities("{}")
                .extractedName(null)
                .extractedEmail(null)
                .extractedPhone(null)
                .extractedAccountNumber(null)
                .build();
    }
}
