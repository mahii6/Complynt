package com.example.Hackathon.service;

import com.example.Hackathon.dto.EmailWebhookPayload;
import jakarta.mail.*;
import jakarta.mail.search.FlagTerm;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
@RequiredArgsConstructor
public class GmailImapService {

    private final EmailComplaintService emailComplaintService;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    // Check for new emails every 30 seconds
    @Scheduled(fixedDelay = 30000)
    public void fetchUnreadEmails() {
        if (username.isEmpty() || password.isEmpty() || username.contains("YOUR_BREVO")) {
            // Stop if user hasn't configured it yet
            return;
        }

        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imaps");
        properties.put("mail.imaps.host", "imap.gmail.com");
        properties.put("mail.imaps.port", "993");

        try {
            Session session = Session.getInstance(properties, null);
            Store store = session.getStore("imaps");
            store.connect("imap.gmail.com", username, password);

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE); // Must be READ_WRITE to mark messages as Read

            // Search only for unseen messages
            Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

            for (Message message : messages) {
                try {
                    String sender = message.getFrom()[0].toString();
                    String subject = message.getSubject();
                    String content = extractContent(message);

                    EmailWebhookPayload payload = new EmailWebhookPayload();
                    payload.setSenderEmail(sender);
                    payload.setSubject(subject);
                    payload.setEmailBody(content != null ? content : "");

                    boolean registered = emailComplaintService.processEmailComplaint(payload);

                    message.setFlag(Flags.Flag.SEEN, true);
                    if (registered) {
                        System.out.println("✅ IMAP received and registered complaint from: " + sender);
                    } else {
                        System.out.println("⚠️ IMAP email processed but not registered as complaint for: " + sender);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to process IMAP email: " + e.getMessage());
                }
            }

            inbox.close(false);
            store.close();
        } catch (Exception e) {
            System.err.println("IMAP Connection Error: " + e.getMessage());
        }
    }

    private String extractContent(Part part) throws Exception {
        if (part.isMimeType("text/plain")) {
            return part.getContent().toString();
        } else if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                String result = extractContent(multipart.getBodyPart(i));
                if (result != null) return result;
            }
        }
        return null;
    }
}
