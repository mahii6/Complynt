package com.example.Hackathon.controller;

import com.example.Hackathon.dto.EmailWebhookPayload;
import com.example.Hackathon.service.EmailComplaintService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class EmailWebhookController {

    private final EmailComplaintService emailComplaintService;

    @Value("${webhook.api.key:default-secret-key-123}")
    private String expectedApiKey;

    @PostMapping({"/email/receive", "/email/webhook"})
    public ResponseEntity<String> receiveEmailWebhook(
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey,
            @RequestBody Map<String, Object> payload) {
        
        if (apiKey == null || !apiKey.equals(expectedApiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized Request");
        }

        try {
            EmailWebhookPayload emailPayload = EmailWebhookPayload.fromMap(payload);
            emailComplaintService.processEmailComplaint(emailPayload);
            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok("Webhook received but parsing failed: " + e.getMessage());
        }
    }
}
