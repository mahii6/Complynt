package com.example.Hackathon.service;

import com.example.Hackathon.dto.SlaStatusDTO;
import com.example.Hackathon.entity.Complaint;
import com.example.Hackathon.entity.SlaRule;
import com.example.Hackathon.enums.ComplaintStatus;
import com.example.Hackathon.repository.ComplaintRepository;
import com.example.Hackathon.repository.SlaRuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class SlaService {

    @Autowired
    private SlaRuleRepository slaRuleRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    public LocalDateTime calculateDeadline(Complaint complaint) {
        SlaRule rule = findApplicableRule(complaint);
        return complaint.getCreatedAt().plusHours(rule.getSlaHours());
    }

    public SlaStatusDTO getStatus(Complaint complaint) {
        if (complaint.getSlaDeadline() == null) {
            return SlaStatusDTO.builder()
                    .status("UNKNOWN")
                    .build();
        }

        SlaRule rule = findApplicableRule(complaint);
        long totalHours = rule.getSlaHours();
        long hoursElapsed = ChronoUnit.HOURS.between(complaint.getCreatedAt(), LocalDateTime.now());
        double percentElapsed = totalHours > 0 ? ((double) hoursElapsed / totalHours) * 100 : 0;

        String status;
        if (complaint.getStatus() == ComplaintStatus.RESOLVED || complaint.getStatus() == ComplaintStatus.CLOSED) {
            status = "RESOLVED";
        } else if (percentElapsed >= 100) {
            status = "BREACHED";
        } else if (percentElapsed >= 80) {
            status = "AT_RISK";
        } else {
            status = "ON_TRACK";
        }

        return SlaStatusDTO.builder()
                .deadline(complaint.getSlaDeadline())
                .hoursElapsed(hoursElapsed)
                .totalHours(totalHours)
                .percentElapsed(percentElapsed)
                .status(status)
                .build();
    }

    public SlaStatusDTO getStatusById(Long complaintId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint not found: " + complaintId));
        return getStatus(complaint);
    }

    public List<Complaint> getBreachedComplaints() {
        return complaintRepository.findBySlaDeadlineBeforeAndStatusNot(
                LocalDateTime.now(), ComplaintStatus.RESOLVED);
    }

    private SlaRule findApplicableRule(Complaint complaint) {
        String severity = complaint.getSeverity() != null ? complaint.getSeverity().name() : "P4";
        String productType = complaint.getProductType() != null ? complaint.getProductType().name() : null;

        // Try specific rule first
        if (productType != null) {
            var specific = slaRuleRepository.findBySeverityAndProductType(severity, productType);
            if (specific.isPresent()) return specific.get();
        }

        // Fallback to generic rule
        return slaRuleRepository.findBySeverityAndProductTypeIsNull(severity)
                .orElse(SlaRule.builder().slaHours(720).warnAtPercent(80).build()); // 30 day default
    }
}
