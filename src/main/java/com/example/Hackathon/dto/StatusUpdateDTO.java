package com.example.Hackathon.dto;

import com.example.Hackathon.enums.ComplaintStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusUpdateDTO {
    @NotNull
    private ComplaintStatus status;
    /** Internal investigation note (stored as internal comment + audit). */
    private String note;
    /** Reassign case to another agent (e.g. when escalating workload). */
    private Long assignToAgentId;
    /** When true, send {@link #customerNotifyMessage} to the customer after save. */
    private Boolean notifyCustomer;
    /** Message body emailed to customer if {@link #notifyCustomer} is true. */
    private String customerNotifyMessage;
    /** EMAIL, WHATSAPP, or AUTO — same as agent notify. */
    private String notifyChannel;
}
