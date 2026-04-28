package com.example.Hackathon.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentNotifyDTO {

    /**
     * EMAIL, WHATSAPP, or AUTO (match the complaint channel — WhatsApp-originated cases prefer WhatsApp).
     */
    private String channel;

    @NotBlank
    private String message;

    /** INFO_REQUEST | RESOLUTION | OTHER */
    private String messageType;
}
