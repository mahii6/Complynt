package com.example.Hackathon.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlaStatusDTO {
    private LocalDateTime deadline;
    private Long hoursElapsed;
    private Long totalHours;
    private Double percentElapsed;
    private String status; // ON_TRACK, AT_RISK, BREACHED
}
