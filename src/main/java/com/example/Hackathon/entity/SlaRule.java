package com.example.Hackathon.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sla_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlaRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String severity; // P1, P2, P3, P4

    private String productType; // nullable — null = applies to all

    private Integer slaHours; // P1=24, P2=72, P3=168, P4=720

    @Column(columnDefinition = "INT DEFAULT 80")
    private Integer warnAtPercent;
}
