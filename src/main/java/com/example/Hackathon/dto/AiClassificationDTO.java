package com.example.Hackathon.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiClassificationDTO {
    private String productType;
    private String issueType;
    private String title;           // AI-generated professional title
    private String severity;
    private String sentimentLabel;
    private Double sentimentScore;
    private Double confidence;       // Classification confidence (0.0 - 1.0)
    private String classifiedBy;     // "DISTILBERT" or "GROQ_LLM"
    private Integer criticalScore;   // 1-10 criticality score from LLM
    private Boolean regulatoryFlag;
    private String suggestedResponse;
    private String extractedEntities;

    /** Populated by Groq post-enrichment (e.g. WhatsApp) from complaint text */
    private String extractedEmail;
    private String extractedPhone;
    private String extractedAccountNumber;
    /** Customer name if explicitly stated in the complaint body */
    private String extractedName;
}
