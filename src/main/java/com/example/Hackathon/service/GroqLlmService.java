package com.example.Hackathon.service;

import com.example.Hackathon.dto.AiClassificationDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Groq LLM Service — called when DistilBERT confidence is below threshold.
 * Uses Groq API (OpenAI-compatible) with llama-3.3-70b-versatile model.
 */
@Service
public class GroqLlmService {

    private final RestTemplate restTemplate;

    @Value("${app.groq.api-key:}")
    private String groqApiKey;

    @Value("${app.groq.model:llama-3.3-70b-versatile}")
    private String groqModel;

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

    private static final ObjectMapper JSON = new ObjectMapper();

    public GroqLlmService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * After primary categorization, use Groq to assign severity (P1–P4) and extract
     * email, phone, and account/customer identifiers mentioned in the complaint text.
     * Merges results into a copy of {@code prior}.
     */
    public AiClassificationDTO enrichAfterClassification(String description, AiClassificationDTO prior) {
        if (prior == null || !isGroqConfigured()) {
            return prior;
        }
        try {
            String systemPrompt = """
                    You analyze a bank complaint AFTER it has been automatically categorized.
                    Use the complaint text to:
                    1) Assign severity P1 (critical) through P4 (low).
                    2) Extract ONLY information explicitly present in the text: customer full name (if signed),
                       email, phone, bank account or customer ID numbers.
                    Do not guess or invent values. Use null for any field not clearly stated.

                    Return ONLY valid JSON with this exact shape:
                    {
                      "severity": "P1" | "P2" | "P3" | "P4",
                      "extractedName": string or null,
                      "extractedEmail": string or null,
                      "extractedPhone": string or null,
                      "extractedAccountNumber": string or null
                    }
                    """;

            String userPrompt = String.format(
                    "Preliminary classification: productType=%s, issueType=%s, classifiedBy=%s%n%nComplaint text:%n\"%s\"",
                    prior.getProductType(),
                    prior.getIssueType(),
                    prior.getClassifiedBy(),
                    description.replace("\"", "'"));

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", groqModel);
            requestBody.put("temperature", 0.1);
            requestBody.put("max_tokens", 400);
            requestBody.put("response_format", Map.of("type", "json_object"));

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userPrompt));
            requestBody.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    GROQ_API_URL, HttpMethod.POST, entity, Map.class);

            if (response.getBody() == null) {
                return prior;
            }

            return mergeEnrichment(description, prior, response.getBody());
        } catch (Exception e) {
            System.out.println("⚠️ Groq enrichment (severity/extraction) failed: " + e.getMessage());
            return prior;
        }
    }

    private boolean isGroqConfigured() {
        return groqApiKey != null && !groqApiKey.isBlank() && !"Your token".equalsIgnoreCase(groqApiKey.trim());
    }

    @SuppressWarnings("unchecked")
    private AiClassificationDTO mergeEnrichment(String description, AiClassificationDTO prior, Map<String, Object> responseBody) {
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices == null || choices.isEmpty()) {
                return prior;
            }
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");
            Map<String, Object> parsed = JSON.readValue(content, Map.class);

            String rawSev = getStringOrDefault(parsed, "severity", prior.getSeverity());
            String finalSeverity = normalizeSeverity(rawSev, prior.getSeverity());
            String extName = nullIfBlank(getStringOrDefault(parsed, "extractedName", null));
            String extEmail = nullIfBlank(getStringOrDefault(parsed, "extractedEmail", null));
            String extPhone = nullIfBlank(getStringOrDefault(parsed, "extractedPhone", null));
            String extAcct = nullIfBlank(getStringOrDefault(parsed, "extractedAccountNumber", null));

            if (extName != null) {
                extName = extName.trim();
            }
            if (extEmail != null) {
                extEmail = extEmail.trim();
            }
            if (extPhone != null) {
                extPhone = extPhone.replaceAll("[\\s-]", "");
            }
            if (extAcct != null) {
                extAcct = extAcct.trim();
            }

            String mergedEntitiesJson = buildMergedEntitiesJson(prior, extName, extEmail, extPhone, extAcct);

            return AiClassificationDTO.builder()
                    .productType(prior.getProductType())
                    .issueType(prior.getIssueType())
                    .title(prior.getTitle())
                    .severity(finalSeverity)
                    .sentimentLabel(prior.getSentimentLabel())
                    .sentimentScore(prior.getSentimentScore())
                    .confidence(prior.getConfidence())
                    .classifiedBy(prior.getClassifiedBy())
                    .criticalScore(mapSeverityToCriticalScore(finalSeverity))
                    .regulatoryFlag(prior.getRegulatoryFlag())
                    .suggestedResponse(prior.getSuggestedResponse())
                    .extractedEntities(mergedEntitiesJson)
                    .extractedName(extName)
                    .extractedEmail(extEmail)
                    .extractedPhone(extPhone)
                    .extractedAccountNumber(extAcct)
                    .build();
        } catch (Exception e) {
            System.out.println("⚠️ Failed to parse Groq enrichment: " + e.getMessage());
            return prior;
        }
    }

    private String nullIfBlank(String s) {
        if (s == null || s.isBlank() || "null".equalsIgnoreCase(s.trim())) {
            return null;
        }
        return s;
    }

    private String normalizeSeverity(String s, String fallback) {
        if (s == null || s.isBlank()) {
            return fallback != null ? fallback : "P3";
        }
        String t = s.trim().toUpperCase();
        if (t.matches("P[1-4]")) {
            return t;
        }
        return fallback != null ? fallback : "P3";
    }

    private int mapSeverityToCriticalScore(String severity) {
        if (severity == null) {
            return 5;
        }
        return switch (severity) {
            case "P1" -> 9;
            case "P2" -> 7;
            case "P3" -> 5;
            case "P4" -> 3;
            default -> 5;
        };
    }

    private String buildMergedEntitiesJson(AiClassificationDTO prior, String extName, String extEmail, String extPhone, String extAcct) {
        try {
            ObjectNode root = JSON.createObjectNode();
            root.put("source", "groq_post_enrichment");
            if (prior.getExtractedEntities() != null && !prior.getExtractedEntities().isBlank()) {
                try {
                    JsonNode existing = JSON.readTree(prior.getExtractedEntities());
                    root.set("priorExtracted", existing);
                } catch (Exception ignored) {
                    root.put("priorExtractedRaw", prior.getExtractedEntities());
                }
            }
            if (extName != null) {
                root.put("extractedName", extName);
            }
            if (extEmail != null) {
                root.put("extractedEmail", extEmail);
            }
            if (extPhone != null) {
                root.put("extractedPhone", extPhone);
            }
            if (extAcct != null) {
                root.put("extractedAccountNumber", extAcct);
            }
            return JSON.writeValueAsString(root);
        } catch (Exception e) {
            return prior.getExtractedEntities() != null ? prior.getExtractedEntities() : "{}";
        }
    }

    /**
     * Classify a complaint using Groq LLM when DistilBERT confidence is low.
     * Returns full classification with product type, issue type, severity,
     * sentiment, criticality score, and suggested response.
     */
    public AiClassificationDTO classify(String description) {
        try {
            String systemPrompt = buildSystemPrompt();
            String userPrompt = "Classify this bank complaint:\n\n\"" + description + "\"";

            // Build request body (OpenAI-compatible format)
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", groqModel);
            requestBody.put("temperature", 0.1);
            requestBody.put("max_tokens", 500);
            requestBody.put("response_format", Map.of("type", "json_object"));

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userPrompt));
            requestBody.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    GROQ_API_URL, HttpMethod.POST, entity, Map.class);

            if (response.getBody() == null) {
                System.out.println("⚠️ Groq API returned null body");
                return getFallbackResponse();
            }

            return parseGroqResponse(response.getBody());
        } catch (Exception e) {
            System.out.println("⚠️ Groq LLM classification failed: " + e.getMessage());
            e.printStackTrace();
            return getFallbackResponse();
        }
    }

    private String buildSystemPrompt() {
        return """
                You are an AI complaint classifier for a banking system.
                Analyze the complaint and return a JSON object with these fields:
                
                {
                  "productType": one of ["CREDIT_CARD","DEBIT_CARD","HOME_LOAN","NET_BANKING","SAVINGS_ACCOUNT","FIXED_DEPOSIT","INSURANCE","PERSONAL_LOAN","OTHER"],
                  "issueType": one of ["TRANSACTION_DISPUTE","FEE_COMPLAINT","KYC_ISSUE","SERVICE_OUTAGE","FRAUD_REPORT","GENERAL_INQUIRY","OTHER"],
                  "title": "A short professional title for the complaint (e.g., 'Unauthorized Transaction', 'Delayed Refund')",
                  "severity": one of ["P1","P2","P3","P4"] where P1=critical, P2=high, P3=medium, P4=low,
                  "sentimentLabel": one of ["POSITIVE","NEGATIVE","NEUTRAL","FRUSTRATED","ANGRY"],
                  "sentimentScore": float between 0.0 (very negative) and 1.0 (very positive),
                  "criticalScore": integer 1-10  (10 = most critical for the bank to address immediately),
                  "regulatoryFlag": boolean true if the complaint involves regulatory/compliance issues,
                  "suggestedResponse": a professional response message to send to the customer acknowledging their complaint,
                  "extractedEntities": a JSON string of key entities like account numbers, transaction IDs, amounts mentioned,
                  "classificationConfidence": float 0.0-1.0 — your confidence that productType and issueType are correct for this complaint
                }
                
                Return ONLY valid JSON, no markdown or extra text.
                """;
    }

    @SuppressWarnings("unchecked")
    private AiClassificationDTO parseGroqResponse(Map<String, Object> responseBody) {
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices == null || choices.isEmpty()) {
                return getFallbackResponse();
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");

            // Parse the JSON content from LLM
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> parsed = mapper.readValue(content, Map.class);

            double clsConf = getDoubleOrDefault(parsed, "classificationConfidence", 0.0);
            if (clsConf <= 0.0) {
                clsConf = 0.85;
            }

            return AiClassificationDTO.builder()
                    .productType(getStringOrDefault(parsed, "productType", "OTHER"))
                    .issueType(getStringOrDefault(parsed, "issueType", "GENERAL_INQUIRY"))
                    .title(getStringOrDefault(parsed, "title", "Bank Complaint"))
                    .severity(getStringOrDefault(parsed, "severity", "P3"))
                    .sentimentLabel(getStringOrDefault(parsed, "sentimentLabel", "NEUTRAL"))
                    .sentimentScore(getDoubleOrDefault(parsed, "sentimentScore", 0.5))
                    .confidence(clsConf)
                    .classifiedBy("GROQ_LLM")
                    .criticalScore(getIntOrDefault(parsed, "criticalScore", 5))
                    .regulatoryFlag(getBoolOrDefault(parsed, "regulatoryFlag", false))
                    .suggestedResponse(getStringOrDefault(parsed, "suggestedResponse",
                            "Thank you for reaching out. We have received your complaint and our team is reviewing it."))
                    .extractedEntities(getStringOrDefault(parsed, "extractedEntities", "{}"))
                    .extractedName(null)
                    .extractedEmail(null)
                    .extractedPhone(null)
                    .extractedAccountNumber(null)
                    .build();
        } catch (Exception e) {
            System.out.println("⚠️ Failed to parse Groq response: " + e.getMessage());
            return getFallbackResponse();
        }
    }

    private AiClassificationDTO getFallbackResponse() {
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
                .suggestedResponse("Thank you for reaching out. We have received your complaint " +
                        "and our team is reviewing it. We will get back to you within " +
                        "the specified SLA timeline.")
                .extractedEntities("{}")
                .extractedName(null)
                .extractedEmail(null)
                .extractedPhone(null)
                .extractedAccountNumber(null)
                .build();
    }

    private String getStringOrDefault(Map<String, Object> map, String key, String defaultVal) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultVal;
    }

    private Double getDoubleOrDefault(Map<String, Object> map, String key, Double defaultVal) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        return defaultVal;
    }

    private Integer getIntOrDefault(Map<String, Object> map, String key, Integer defaultVal) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        return defaultVal;
    }

    private Boolean getBoolOrDefault(Map<String, Object> map, String key, Boolean defaultVal) {
        Object val = map.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        return defaultVal;
    }
}
