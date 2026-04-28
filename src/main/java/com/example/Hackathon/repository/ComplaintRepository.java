package com.example.Hackathon.repository;

import com.example.Hackathon.entity.Complaint;
import com.example.Hackathon.enums.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    Page<Complaint> findByStatus(ComplaintStatus status, Pageable pageable);

    Page<Complaint> findByCustomerId(Long customerId, Pageable pageable);

    Page<Complaint> findByAssignedAgentId(Long agentId, Pageable pageable);

    List<Complaint> findByStatusNot(ComplaintStatus status);

    Optional<Complaint> findByTicketNumber(String ticketNumber);

    List<Complaint> findBySlaDeadlineBeforeAndStatusNot(LocalDateTime time, ComplaintStatus status);

    List<Complaint> findByStatusOrderByCreatedAtDesc(ComplaintStatus status);

    List<Complaint> findByCustomerIdAndProductTypeAndCreatedAtAfter(
            Long customerId, ProductType productType, LocalDateTime after);

    List<Complaint> findByCustomerIdAndCreatedAtAfter(Long customerId, LocalDateTime after);

    long countByStatus(ComplaintStatus status);

    long countByProductType(ProductType type);

    long countBySeverity(Severity severity);

    @Query("SELECT c FROM Complaint c WHERE " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:productType IS NULL OR c.productType = :productType) AND " +
            "(:severity IS NULL OR c.severity = :severity) AND " +
            "(:agentId IS NULL OR c.assignedAgent.id = :agentId) AND " +
            "(:fromDate IS NULL OR c.createdAt >= :fromDate) AND " +
            "(:toDate IS NULL OR c.createdAt <= :toDate)")
    Page<Complaint> findWithFilters(
            @Param("status") ComplaintStatus status,
            @Param("productType") ProductType productType,
            @Param("severity") Severity severity,
            @Param("agentId") Long agentId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

    @Query("SELECT DATE(c.createdAt) as date, COUNT(c) as count FROM Complaint c " +
            "WHERE c.createdAt >= :fromDate GROUP BY DATE(c.createdAt) ORDER BY DATE(c.createdAt)")
    List<Object[]> getDailyTrend(@Param("fromDate") LocalDateTime fromDate);

    @Query("SELECT c FROM Complaint c WHERE c.createdAt >= :fromDate AND c.createdAt <= :toDate")
    List<Complaint> findByDateRange(@Param("fromDate") LocalDateTime fromDate,
                                     @Param("toDate") LocalDateTime toDate);

    @Query("SELECT c.title FROM Complaint c WHERE c.title IS NOT NULL AND TRIM(c.title) <> '' ORDER BY c.createdAt DESC")
    List<String> findRecentTitles(Pageable pageable);
}
