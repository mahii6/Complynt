package com.example.Hackathon.dto;

import lombok.Data;
import java.util.Map;
import java.util.List;

@Data
public class EmailWebhookPayload {
    private String senderEmail;
    private String subject;
    private String emailBody;

    // This method will safely extract fields whether Brevo sends them as flat JSON or nested `items` array
    public static EmailWebhookPayload fromMap(Map<String, Object> payload) {
        EmailWebhookPayload emailPayload = new EmailWebhookPayload();
        
        // 1. Check for standard Brevo inbound parse payload (items array)
        if (payload.containsKey("items") && payload.get("items") instanceof List) {
            List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");
            if (!items.isEmpty()) {
                Map<String, Object> item = items.get(0);
                
                // Extract Sender
                if (item.containsKey("From") && item.get("From") instanceof Map) {
                    Map<String, Object> from = (Map<String, Object>) item.get("From");
                    emailPayload.setSenderEmail((String) from.get("Address"));
                }
                
                // Extract Subject
                emailPayload.setSubject((String) item.get("Subject"));
                
                // Extract Body
                if (item.containsKey("RawTextBody") && item.get("RawTextBody") != null) {
                    emailPayload.setEmailBody((String) item.get("RawTextBody"));
                } else if (item.containsKey("RawHtmlBody")) {
                    emailPayload.setEmailBody((String) item.get("RawHtmlBody"));
                }
            }
        } else {
            // 2. Fallback to flat simple structure (in case the webhook sends it cleanly as requested)
            // Checks "from" first, then "sender", then "email"
            emailPayload.setSenderEmail((String) payload.getOrDefault("from", payload.getOrDefault("sender", payload.get("email"))));
            emailPayload.setSubject((String) payload.get("subject"));
            
            // Checks "text", falls back to "body", falls back to "html"
            String textContent = (String) payload.getOrDefault("text", payload.get("body"));
            if (textContent == null || textContent.trim().isEmpty()) {
                textContent = (String) payload.get("html");
            }
            emailPayload.setEmailBody(textContent);
        }

        return emailPayload;
    }
}
