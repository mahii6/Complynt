package com.example.Hackathon.entity;

import com.example.Hackathon.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "complaints")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String ticketNumber;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String email;
    private String providedCustomerId;
    private String priority;

    @Enumerated(EnumType.STRING)
    private ProductType productType;

    @Enumerated(EnumType.STRING)
    private IssueType issueType;

    @Enumerated(EnumType.STRING)
    private ComplaintStatus status;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    private Channel channel;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean regulatoryFlag;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isDuplicate;

    private Long duplicateOfId;

    // AI fields — stubbed, filled by Python later
    private String sentimentLabel;
    private Double sentimentScore;
    private Double aiConfidence; // DistilBERT or LLM confidence
    private String classifiedBy; // "DISTILBERT" or "GROQ_LLM"
    private Integer criticalScore; // 1-10 criticality from LLM

    @Column(columnDefinition = "TEXT")
    private String aiSuggestedResponse;

    @Column(columnDefinition = "TEXT")
    private String extractedEntities; // JSON string

    /** JSON array of HTTPS URLs (screenshots, statements, code snippets hosted as images). */
    @Column(columnDefinition = "TEXT")
    private String attachmentUrls;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime slaDeadline;
    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_agent_id")
    private Agent assignedAgent;

    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt DESC")
    private List<ComplaintComment> comments;

    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("timestamp DESC")
    private List<AuditLog> auditLogs;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        if (this.regulatoryFlag == null) this.regulatoryFlag = false;
        if (this.isDuplicate == null) this.isDuplicate = false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
