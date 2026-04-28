package com.example.Hackathon.service;

import com.example.Hackathon.dto.AiClassificationDTO;
import com.example.Hackathon.enums.IssueType;
import com.example.Hackathon.enums.ProductType;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

/**
 * Maps HuggingFace / DistilBERT labels (often a small consumer-complaint taxonomy),
 * Groq outputs, and free text into valid {@link ProductType} / {@link IssueType} values stored in the DB.
 */
@Service
public class CategoryMappingService {

    /** HF Python service (CoolHatt model) + common aliases → ProductType */
    private static final Map<String, ProductType> LABEL_TO_PRODUCT = Map.ofEntries(
            Map.entry("CREDIT_CARD", ProductType.CREDIT_CARD),
            Map.entry("DEBIT_CARD", ProductType.DEBIT_CARD),
            Map.entry("RETAIL_BANKING", ProductType.SAVINGS_ACCOUNT),
            Map.entry("SAVINGS_ACCOUNT", ProductType.SAVINGS_ACCOUNT),
            Map.entry("NET_BANKING", ProductType.NET_BANKING),
            Map.entry("CREDIT_REPORTING", ProductType.OTHER),
            Map.entry("MORTGAGES_AND_LOANS", ProductType.HOME_LOAN),
            Map.entry("HOME_LOAN", ProductType.HOME_LOAN),
            Map.entry("PERSONAL_LOAN", ProductType.PERSONAL_LOAN),
            Map.entry("DEBT_COLLECTION", ProductType.PERSONAL_LOAN),
            Map.entry("FIXED_DEPOSIT", ProductType.FIXED_DEPOSIT),
            Map.entry("INSURANCE", ProductType.INSURANCE),
            Map.entry("OTHER", ProductType.OTHER)
    );

    public ProductType mapRawLabelToProductType(String raw) {
        if (raw == null || raw.isBlank()) {
            return ProductType.OTHER;
        }
        String key = normalizeKey(raw);
        if (LABEL_TO_PRODUCT.containsKey(key)) {
            return LABEL_TO_PRODUCT.get(key);
        }
        try {
            return ProductType.valueOf(key);
        } catch (IllegalArgumentException ignored) {
            // fuzzy hints
            String u = key.toUpperCase(Locale.ROOT);
            if (u.contains("MORTGAGE") || u.contains("HOME_LOAN")) {
                return ProductType.HOME_LOAN;
            }
            if (u.contains("PERSONAL") && u.contains("LOAN")) {
                return ProductType.PERSONAL_LOAN;
            }
            if (u.contains("CREDIT") && u.contains("CARD")) {
                return ProductType.CREDIT_CARD;
            }
            if (u.contains("DEBIT") && u.contains("CARD")) {
                return ProductType.DEBIT_CARD;
            }
            if (u.contains("NET") && u.contains("BANK")) {
                return ProductType.NET_BANKING;
            }
            if (u.contains("RETAIL") || u.contains("SAVING")) {
                return ProductType.SAVINGS_ACCOUNT;
            }
            return ProductType.OTHER;
        }
    }

    public IssueType mapRawLabelToIssueType(String raw, ProductType productType) {
        if (raw == null || raw.isBlank()) {
            return IssueType.GENERAL_INQUIRY;
        }
        String key = normalizeKey(raw);
        return switch (key) {
            case "CREDIT_CARD", "DEBIT_CARD" -> IssueType.TRANSACTION_DISPUTE;
            case "DEBT_COLLECTION" -> IssueType.FEE_COMPLAINT;
            case "CREDIT_REPORTING" -> IssueType.KYC_ISSUE;
            case "MORTGAGES_AND_LOANS", "HOME_LOAN", "PERSONAL_LOAN" -> IssueType.FEE_COMPLAINT;
            case "NET_BANKING" -> IssueType.SERVICE_OUTAGE;
            case "RETAIL_BANKING", "SAVINGS_ACCOUNT" -> IssueType.GENERAL_INQUIRY;
            default -> defaultIssueForProduct(productType);
        };
    }

    private IssueType defaultIssueForProduct(ProductType productType) {
        if (productType == null) {
            return IssueType.GENERAL_INQUIRY;
        }
        return switch (productType) {
            case CREDIT_CARD, DEBIT_CARD -> IssueType.TRANSACTION_DISPUTE;
            case HOME_LOAN, PERSONAL_LOAN -> IssueType.FEE_COMPLAINT;
            case NET_BANKING -> IssueType.SERVICE_OUTAGE;
            default -> IssueType.GENERAL_INQUIRY;
        };
    }

    public IssueType coerceIssueType(String raw, ProductType productType) {
        if (raw == null || raw.isBlank()) {
            return defaultIssueForProduct(productType);
        }
        String key = normalizeKey(raw);
        try {
            return IssueType.valueOf(key);
        } catch (IllegalArgumentException e) {
            return mapRawLabelToIssueType(key, productType);
        }
    }

    public ProductType coerceProductType(String raw) {
        return mapRawLabelToProductType(raw);
    }

    /**
     * Ensures Groq / fusion output strings match DB enums.
     */
    public AiClassificationDTO normalizeClassification(AiClassificationDTO d) {
        if (d == null) {
            return null;
        }
        ProductType pt = coerceProductType(d.getProductType());
        IssueType it = coerceIssueType(d.getIssueType(), pt);
        return AiClassificationDTO.builder()
                .productType(pt.name())
                .issueType(it.name())
                .title(d.getTitle())
                .severity(d.getSeverity())
                .sentimentLabel(d.getSentimentLabel())
                .sentimentScore(d.getSentimentScore())
                .confidence(d.getConfidence())
                .classifiedBy(d.getClassifiedBy())
                .criticalScore(d.getCriticalScore())
                .regulatoryFlag(d.getRegulatoryFlag())
                .suggestedResponse(d.getSuggestedResponse())
                .extractedEntities(d.getExtractedEntities())
                .extractedName(d.getExtractedName())
                .extractedEmail(d.getExtractedEmail())
                .extractedPhone(d.getExtractedPhone())
                .extractedAccountNumber(d.getExtractedAccountNumber())
                .build();
    }

    private static String normalizeKey(String raw) {
        return raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }
}
