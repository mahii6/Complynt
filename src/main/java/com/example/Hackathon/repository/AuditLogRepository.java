package com.example.Hackathon.repository;

import com.example.Hackathon.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByComplaintIdOrderByTimestampDesc(Long complaintId);
}
