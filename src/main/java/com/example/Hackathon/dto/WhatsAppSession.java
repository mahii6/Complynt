package com.example.Hackathon.dto;

import com.example.Hackathon.enums.IssueType;
import com.example.Hackathon.enums.ProductType;
import lombok.Data;

@Data
public class WhatsAppSession {
    private String phoneNumber;
    private BotState currentState;
    private String customerId; // Temporary storage during flow
    private String customerName; // Collected during complaint flow
    private String customerEmail; // Collected during complaint flow
    private String customerMobile; // Collected during complaint flow
    private ProductType selectedCategory; // Temporary storage
    private IssueType selectedIssueType; // Temporary storage
    private String complaintDescription; // Temporary storage
    private long lastInteractionTime;

    public WhatsAppSession(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        this.currentState = BotState.MAIN_MENU;
        this.lastInteractionTime = System.currentTimeMillis();
    }
}
