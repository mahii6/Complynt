package com.example.Hackathon.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignAgentDTO {
    @NotNull
    private Long agentId;
}
