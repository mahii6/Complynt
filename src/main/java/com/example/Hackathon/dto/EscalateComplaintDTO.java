package com.example.Hackathon.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EscalateComplaintDTO {

    @NotNull
    private Long targetAgentId;

    /** Internal handoff note (stored as internal comment + audit). */
    private String internalNote;

    /** When not false, sends the assigned agent an email with ticket summary (default on). */
    private Boolean notifyAssignedAgent;
}
