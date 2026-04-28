package com.example.Hackathon.service;

import com.example.Hackathon.entity.AuditLog;
import com.example.Hackathon.entity.Complaint;
import com.example.Hackathon.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void log(Complaint complaint, String action, String performedBy,
                    String oldValue, String newValue) {
        AuditLog auditLog = AuditLog.builder()
                .complaint(complaint)
                .action(action)
                .performedBy(performedBy)
                .oldValue(oldValue)
                .newValue(newValue)
                .build();
        auditLogRepository.save(auditLog);
    }

    public List<AuditLog> getLogsForComplaint(Long complaintId) {
        return auditLogRepository.findByComplaintIdOrderByTimestampDesc(complaintId);
    }
}
