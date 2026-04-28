package com.example.Hackathon.service;

import com.example.Hackathon.dto.SlaStatusDTO;
import com.example.Hackathon.entity.Complaint;
import com.example.Hackathon.entity.EscalationRule;
import com.example.Hackathon.enums.ComplaintStatus;
import com.example.Hackathon.repository.ComplaintRepository;
import com.example.Hackathon.repository.EscalationRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class EscalationService {

    private static final Logger log = LoggerFactory.getLogger(EscalationService.class);

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private EscalationRuleRepository escalationRuleRepository;

    @Autowired
    private SlaService slaService;

    @Autowired
    private AuditLogService auditLogService;

    /**
     * Self-reference so {@link #checkAndEscalate(Long)} runs in {@link Propagation#REQUIRES_NEW}.
     * Same-class calls bypass Spring proxies and would otherwise keep one transaction for all rows
     * (long locks → deadlocks with concurrent complaint updates).
     */
    @Autowired
    @Lazy
    private EscalationService self;

    /**
     * One transaction per complaint: short row locks, avoids deadlocks with API/concurrent writers.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkAndEscalate(Long complaintId) {
        Complaint complaint = complaintRepository.findById(complaintId).orElse(null);
        if (complaint == null) {
            return;
        }
        if (complaint.getStatus() == ComplaintStatus.RESOLVED
                || complaint.getStatus() == ComplaintStatus.CLOSED) {
            return;
        }

        SlaStatusDTO sla = slaService.getStatus(complaint);
        String severity = complaint.getSeverity() != null ? complaint.getSeverity().name() : "P4";
        List<EscalationRule> rules = escalationRuleRepository.findBySeverity(severity);

        for (EscalationRule rule : rules) {
            if (sla.getPercentElapsed() != null
                    && sla.getPercentElapsed() >= rule.getTriggerAtPercent()
                    && complaint.getStatus() != ComplaintStatus.ESCALATED) {

                complaint.setStatus(ComplaintStatus.ESCALATED);
                complaintRepository.save(complaint);

                auditLogService.log(complaint,
                        "Auto-escalated to " + rule.getEscalateTo() +
                                " (SLA at " + String.format("%.0f", sla.getPercentElapsed()) + "%)",
                        "SYSTEM", "OPEN", "ESCALATED");
                break;
            }
        }
    }

    public void checkAllOpen() {
        List<Complaint> openComplaints = complaintRepository.findByStatusNot(ComplaintStatus.RESOLVED);
        openComplaints.sort(Comparator.comparing(Complaint::getId));
        for (Complaint complaint : openComplaints) {
            if (complaint.getStatus() != ComplaintStatus.CLOSED) {
                try {
                    self.checkAndEscalate(complaint.getId());
                } catch (CannotAcquireLockException e) {
                    log.warn("SLA escalation skipped for complaint {} due to lock contention; will retry on next run",
                            complaint.getId(), e);
                }
            }
        }
    }
}
