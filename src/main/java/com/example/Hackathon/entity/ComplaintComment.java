package com.example.Hackathon.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaint_comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String authorName;

    private String authorRole; // AGENT, CUSTOMER, SYSTEM

    private Boolean isInternal; // internal note vs customer-visible

    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id")
    private Complaint complaint;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.isInternal == null) this.isInternal = false;
    }
}
