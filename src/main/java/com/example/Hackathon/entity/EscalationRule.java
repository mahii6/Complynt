package com.example.Hackathon.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "escalation_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EscalationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String severity; // P1, P2, P3, P4

    private Integer triggerAtPercent; // 80 = warn, 100 = breach

    private String escalateTo; // SUPERVISOR, BRANCH_HEAD, COMPLIANCE

    private String notifyMethod; // DASHBOARD_ALERT, EMAIL
}
