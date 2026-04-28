package com.example.Hackathon.dto;

import lombok.*;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsSummaryDTO {
    private long totalComplaints;
    private long openCount;
    private long inProgressCount;
    private long resolvedCount;
    private long escalatedCount;
    private long breachedSlaCount;
    private Map<String, Long> byProduct;
    private Map<String, Long> bySeverity;
    private Map<String, Long> byStatus;
}
