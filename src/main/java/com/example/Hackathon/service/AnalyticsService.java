package com.example.Hackathon.service;

import com.example.Hackathon.dto.AnalyticsSummaryDTO;
import com.example.Hackathon.enums.*;
import com.example.Hackathon.repository.ComplaintRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AnalyticsService {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private SlaService slaService;

    public AnalyticsSummaryDTO getSummary() {
        Map<String, Long> byProduct = new LinkedHashMap<>();
        for (ProductType pt : ProductType.values()) {
            long count = complaintRepository.countByProductType(pt);
            if (count > 0) byProduct.put(pt.name(), count);
        }

        Map<String, Long> bySeverity = new LinkedHashMap<>();
        for (Severity s : Severity.values()) {
            long count = complaintRepository.countBySeverity(s);
            if (count > 0) bySeverity.put(s.name(), count);
        }

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ComplaintStatus cs : ComplaintStatus.values()) {
            long count = complaintRepository.countByStatus(cs);
            if (count > 0) byStatus.put(cs.name(), count);
        }

        return AnalyticsSummaryDTO.builder()
                .totalComplaints(complaintRepository.count())
                .openCount(complaintRepository.countByStatus(ComplaintStatus.OPEN))
                .inProgressCount(complaintRepository.countByStatus(ComplaintStatus.IN_PROGRESS))
                .resolvedCount(complaintRepository.countByStatus(ComplaintStatus.RESOLVED))
                .escalatedCount(complaintRepository.countByStatus(ComplaintStatus.ESCALATED))
                .breachedSlaCount(slaService.getBreachedComplaints().size())
                .byProduct(byProduct)
                .bySeverity(bySeverity)
                .byStatus(byStatus)
                .build();
    }

    public List<Map<String, Object>> getDailyTrend(int days) {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);
        List<Object[]> results = complaintRepository.getDailyTrend(fromDate);

        List<Map<String, Object>> trend = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("date", row[0] != null ? row[0].toString() : "");
            entry.put("count", row[1] != null ? ((Number) row[1]).longValue() : 0);
            trend.add(entry);
        }
        return trend;
    }
}
