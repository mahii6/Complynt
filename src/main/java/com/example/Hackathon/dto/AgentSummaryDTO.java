package com.example.Hackathon.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentSummaryDTO {
    private Long id;
    private String name;
    private String email;
    private String role;
}
