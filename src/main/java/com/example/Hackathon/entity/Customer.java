package com.example.Hackathon.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String customerNumber;

    private String fullName;

    @Column(unique = true)
    private String email;

    private String phone;

    private String accountNumber;

    /** Demo / display-only — fake bank identity for passbook-style UI */
    private String bankName;
    private String branchName;
    private String ifscCode;
    private String accountType;
    /** Fake formatted balance for agent screens (not transactional) */
    private String availableBalanceDisplay;
    /** e.g. "18-Mar-2025" — last passbook print line */
    private String passbookLastPrinted;
    /** Optional decorative image (e.g. branch seal / passbook header photo) */
    private String passbookDecorImageUrl;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Complaint> complaints;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
