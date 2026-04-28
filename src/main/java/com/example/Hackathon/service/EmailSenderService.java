package com.example.Hackathon.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailSenderService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendComplaintConfirmation(String toEmail, String complaintId, String subject) {
        SimpleMailMessage message = new SimpleMailMessage();
        
        message.setTo(toEmail);
        message.setSubject("Complaint Registered Successfully - " + complaintId);
        
        String body = "Hello,\n\n" +
                      "Your complaint has been successfully registered.\n\n" +
                      "Complaint ID: " + complaintId + "\n" +
                      "Subject: " + subject + "\n" +
                      "Status: OPEN\n\n" +
                      "Our team will get back to you shortly.\n\n" +
                      "Regards,\n" +
                      "Unified Complaint Management System";
                      
        message.setText(body);
        
        try {
            mailSender.send(message);
        } catch (Exception e) {
            // Log it but don't crash the incoming webhook request
            System.err.println("Failed to send email to " + toEmail + ": " + e.getMessage());
        }
    }

    /**
     * Agent-initiated email to a customer (info request, resolution notice, etc.).
     */
    public void sendAgentMessage(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send email to " + toEmail + ": " + e.getMessage(), e);
        }
    }
}
