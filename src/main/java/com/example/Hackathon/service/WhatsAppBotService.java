package com.example.Hackathon.service;

import com.example.Hackathon.dto.AiClassificationDTO;
import com.example.Hackathon.dto.BotState;
import com.example.Hackathon.dto.WhatsAppSession;
import com.example.Hackathon.entity.Complaint;
import com.example.Hackathon.entity.Customer;
import com.example.Hackathon.enums.*;
import com.example.Hackathon.repository.ComplaintRepository;
import com.example.Hackathon.repository.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class WhatsAppBotService {

    private final WhatsAppSessionService sessionService;
    private final CustomerRepository customerRepository;
    private final ComplaintRepository complaintRepository;
    private final AiGatewayService aiGatewayService;
    private final RoutingService routingService;
    private final SlaService slaService;

    private static final ObjectMapper JSON = new ObjectMapper();

    // ═══════════════════════════════════════════
    //  Auto-reply instructions for Level 1/2
    // ═══════════════════════════════════════════
    private static final Map<String, String> SELF_HELP_INSTRUCTIONS = Map.of(
        "CREDIT_CARD", "💳 *Self-Help: Credit Card Issues*\n" +
            "• Block/unblock your card via Net Banking → Cards → Block Card\n" +
            "• For billing disputes, download your statement and verify the transaction\n" +
            "• Reset PIN at the nearest ATM using your registered mobile number\n" +
            "• For limit enhancement, apply through your mobile app",
        "DEBIT_CARD", "💳 *Self-Help: Debit Card Issues*\n" +
            "• Temporary block via Mobile App → Cards → Manage\n" +
            "• For failed ATM transactions, amount auto-reverses in 5-7 business days\n" +
            "• Apply for a replacement card via Net Banking → Service Requests",
        "NET_BANKING", "🌐 *Self-Help: Net Banking Issues*\n" +
            "• Reset password at the login page → Forgot Password\n" +
            "• Clear browser cache and cookies, then retry\n" +
            "• If account locked, wait 24 hours or call our helpline\n" +
            "• Check service status at our website for maintenance schedules",
        "SAVINGS_ACCOUNT", "🏦 *Self-Help: Savings Account*\n" +
            "• Download recent statements from Net Banking → Accounts → Statement\n" +
            "• For KYC updates, visit nearest branch with valid ID proof\n" +
            "• Update mobile/email via Mobile App → Profile → Update Details",
        "HOME_LOAN", "🏠 *Self-Help: Home Loan*\n" +
            "• Download amortization schedule from Net Banking → Loans\n" +
            "• For prepayment queries, contact your Relationship Manager\n" +
            "• EMI auto-debit issues: ensure sufficient balance 2 days before due date",
        "PERSONAL_LOAN", "💰 *Self-Help: Personal Loan*\n" +
            "• Check EMI schedule via Mobile App → Loans → Personal Loan\n" +
            "• For foreclosure, submit request through Net Banking → Service Requests\n" +
            "• NOC after closure is dispatched within 15 business days"
    );

    public String processMessage(Map<String, String> payload) {
        String from = payload.get("From");
        String messageBody = payload.get("Body") != null ? payload.get("Body").trim() : "";
        int numMedia = Integer.parseInt(payload.getOrDefault("NumMedia", "0"));

        WhatsAppSession session = sessionService.getSession(from);

        // Check for 5-minute expiration (300,000 milliseconds)
        long currentTime = System.currentTimeMillis();
        if (currentTime - session.getLastInteractionTime() > 300_000) {
            session.setCurrentState(BotState.MAIN_MENU);
        }
        session.setLastInteractionTime(currentTime);

        // Allow manual reset
        if (messageBody.equalsIgnoreCase("reset") || messageBody.equalsIgnoreCase("restart") || messageBody.equalsIgnoreCase("menu")) {
            session.setCurrentState(BotState.MAIN_MENU);
            messageBody = "hi"; // Force show welcome
        }

        String responseMessage = handleState(session, messageBody, numMedia);
        
        sessionService.updateSession(session);

        return buildTwiML(responseMessage);
    }

    private String handleState(WhatsAppSession session, String message, int numMedia) {
        switch (session.getCurrentState()) {
            case MAIN_MENU:
                return handleMainMenu(session, message);
            case AWAITING_CUSTOMER_ID_BALANCE:
                return handleCustomerBalance(session, message);
            case AWAITING_CUSTOMER_ID_COMPLAINT:
                return handleCustomerIdForComplaint(session, message);
            case AWAITING_NAME:
                return handleNameInput(session, message);
            case AWAITING_EMAIL:
                return handleEmailInput(session, message);
            case AWAITING_MOBILE:
                return handleMobileInput(session, message);
            case AWAITING_DESCRIPTION:
                return handleComplaintDescription(session, message);
            case AWAITING_COMPLAINT_ID:
                return handleComplaintStatus(session, message);
            default:
                session.setCurrentState(BotState.MAIN_MENU);
                return getWelcomeMessage();
        }
    }

    // ═══════════════════════════════════════════
    //  MAIN MENU — Professional Greeting
    // ═══════════════════════════════════════════
    private String handleMainMenu(WhatsAppSession session, String message) {
        if (message.equalsIgnoreCase("1")) {
            session.setCurrentState(BotState.AWAITING_CUSTOMER_ID_BALANCE);
            return "🔐 *Account Balance Inquiry*\n\nPlease enter your Customer ID (6–12 digit number):";
        } else if (message.equalsIgnoreCase("2")) {
            session.setCurrentState(BotState.AWAITING_CUSTOMER_ID_COMPLAINT);
            return "📝 *Register a New Complaint*\n\n" +
                   "We'll guide you through a quick process.\n" +
                   "Please enter your *Customer ID* (6–12 digit number):";
        } else if (message.equalsIgnoreCase("3")) {
            session.setCurrentState(BotState.AWAITING_COMPLAINT_ID);
            return "🔎 *Track Complaint Status*\n\nPlease enter your *Complaint ID* (e.g., CMP-2026-000001):";
        } else {
            boolean isGreeting = message.equalsIgnoreCase("hi") || message.equalsIgnoreCase("hello") ||
                                 message.equalsIgnoreCase("hey") || message.equalsIgnoreCase("start");
            if (isGreeting) {
                return getWelcomeMessage();
            }
            return "⚠️ I didn't understand that. Please select a valid option.\n\n" + getMenuMessage();
        }
    }

    private String getWelcomeMessage() {
        return "━━━━━━━━━━━━━━━━━━━━━━\n" +
               "🏦 *UBI Smart Support*\n" +
               "━━━━━━━━━━━━━━━━━━━━━━\n\n" +
               "Welcome! I'm your AI-powered banking assistant. " +
               "I can help you with account queries, complaint registration, and status tracking.\n\n" +
               getMenuMessage() + "\n\n" +
               "💡 Type *menu* anytime to return here.";
    }

    private String getMenuMessage() {
        return "*How can I assist you today?*\n\n" +
               "1️⃣ Check Account Balance\n" +
               "2️⃣ Register a Complaint\n" +
               "3️⃣ Track Complaint Status";
    }

    // ═══════════════════════════════════════════
    //  BALANCE INQUIRY
    // ═══════════════════════════════════════════
    private String handleCustomerBalance(WhatsAppSession session, String message) {
        if (!isNumeric(message) || message.length() < 6 || message.length() > 12) {
            return "❌ Invalid Customer ID. Must be a number with 6–12 digits.\n\nPlease try again:";
        }

        Optional<Customer> customer = customerRepository.findByCustomerNumber(message);
        session.setCurrentState(BotState.MAIN_MENU);

        if (customer.isPresent()) {
            return "━━━━━━━━━━━━━━━━━━━━━━\n" +
                   "💰 *Account Balance*\n" +
                   "━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                   "👤 " + customer.get().getFullName() + "\n" +
                   "🆔 " + message + "\n" +
                   "💰 Balance: ₹XXXXX (Demo)\n\n" +
                   getMenuMessage();
        }
        return "💰 Balance: ₹XXXXX (Demo Data)\n\n" + getMenuMessage();
    }

    // ═══════════════════════════════════════════
    //  COMPLAINT FLOW: Step 1 — Customer ID
    // ═══════════════════════════════════════════
    private String handleCustomerIdForComplaint(WhatsAppSession session, String message) {
        if (!isNumeric(message) || message.length() < 6 || message.length() > 12) {
            return "❌ Invalid Customer ID. Must be a number with 6–12 digits.\n\nPlease try again:";
        }
        
        session.setCustomerId(message);
        session.setCurrentState(BotState.AWAITING_NAME);
        return "✅ Customer ID noted.\n\n👤 Please enter your *full name*:";
    }

    // ═══════════════════════════════════════════
    //  COMPLAINT FLOW: Step 2 — Name
    // ═══════════════════════════════════════════
    private String handleNameInput(WhatsAppSession session, String message) {
        if (message.length() < 2 || message.length() > 100) {
            return "❌ Invalid name. Please enter a valid full name (2–100 characters):";
        }
        session.setCustomerName(message);
        session.setCurrentState(BotState.AWAITING_EMAIL);
        return "✅ Thank you, " + message + ".\n\n📧 Please enter your *email address*:";
    }

    // ═══════════════════════════════════════════
    //  COMPLAINT FLOW: Step 3 — Email
    // ═══════════════════════════════════════════
    private String handleEmailInput(WhatsAppSession session, String message) {
        if (!isValidEmail(message)) {
            return "❌ Invalid email format. Please enter a valid email address (e.g., user@example.com):";
        }
        session.setCustomerEmail(message.trim().toLowerCase());
        session.setCurrentState(BotState.AWAITING_MOBILE);
        return "✅ Email recorded.\n\n📱 Please enter your *mobile number* (10 digits):";
    }

    // ═══════════════════════════════════════════
    //  COMPLAINT FLOW: Step 4 — Mobile
    // ═══════════════════════════════════════════
    private String handleMobileInput(WhatsAppSession session, String message) {
        String cleaned = message.replaceAll("[\\s\\-+]", "");
        // Accept 10-digit or 12-digit (with country code) or 13-digit (with +91)
        if (!isNumeric(cleaned) || (cleaned.length() != 10 && cleaned.length() != 12 && cleaned.length() != 13)) {
            return "❌ Invalid mobile number. Please enter a valid 10-digit mobile number:";
        }
        session.setCustomerMobile(cleaned);
        session.setCurrentState(BotState.AWAITING_DESCRIPTION);
        return "✅ Mobile number recorded.\n\n" +
               "━━━━━━━━━━━━━━━━━━━━━━\n" +
               "📝 *Describe Your Issue*\n" +
               "━━━━━━━━━━━━━━━━━━━━━━\n\n" +
               "Please describe your complaint in detail.\n" +
               "Our AI system will automatically categorize and prioritize your issue.\n\n" +
               "💡 _Example: \"My credit card was charged twice for a transaction of ₹5,000 at Amazon on 20th March\"_";
    }

    // ═══════════════════════════════════════════
    //  COMPLAINT FLOW: Step 5 — AI Classification 
    // ═══════════════════════════════════════════
    private String handleComplaintDescription(WhatsAppSession session, String message) {
        if (message.length() < 10) {
            return "❌ Description is too short. Please provide more details (at least 10 characters):";
        }
        
        session.setComplaintDescription(message);

        // ── Duplicate Detection ──
        Optional<Customer> existingCustomer = customerRepository.findByCustomerNumber(session.getCustomerId());
        if (existingCustomer.isPresent()) {
            // Search for ANY recent complaints by this customer (last 48h)
            List<Complaint> recentComplaints = complaintRepository.findByCustomerIdAndCreatedAtAfter(
                    existingCustomer.get().getId(), LocalDateTime.now().minusHours(48));

            // Check for same description similarity
            boolean isDuplicate = recentComplaints.stream()
                    .anyMatch(c -> c.getDescription() != null &&
                            calculateSimilarity(c.getDescription(), message) > 0.7);

            if (isDuplicate) {
                session.setCurrentState(BotState.MAIN_MENU);
                return "⚠️ *Duplicate Complaint Detected*\n\n" +
                       "Hi " + session.getCustomerName() + ", it looks like you've already raised a very similar complaint in the last 48 hours.\n\n" +
                       "To check its status, please select option *3* from the menu.\n\n" +
                       "If this is a completely different issue, please try again with more specific details.\n\n" +
                       getMenuMessage();
            }
        }

        // ── AI Classification (DistilBERT → Groq) + Groq severity & entity extraction (same as email flow) ──
        AiClassificationDTO classification;
        try {
            classification = aiGatewayService.classifyWithGroqEnrichment(message);
        } catch (Exception e) {
            classification = null;
        }

        try {
            String ticketNumber = registerComplaintWithAI(session, classification);
            
            // ── Build response ──
            StringBuilder response = new StringBuilder();
            response.append("━━━━━━━━━━━━━━━━━━━━━━\n");
            response.append("✅ *Complaint Registered*\n");
            response.append("━━━━━━━━━━━━━━━━━━━━━━\n\n");
            response.append("📌 *Ticket ID:* ").append(ticketNumber).append("\n");

            if (classification != null) {
                String productLabel = classification.getProductType() != null 
                    ? classification.getProductType().replace("_", " ") : "Other";
                response.append("🏷️ *Category:* ").append(productLabel).append("\n");
                response.append("⚡ *Priority:* ").append(classification.getSeverity() != null ? classification.getSeverity() : "P3").append("\n");
                response.append("🤖 *Classified by:* ").append(classification.getClassifiedBy() != null ? classification.getClassifiedBy() : "AI").append("\n");
                
                if (classification.getCriticalScore() != null) {
                    response.append("🔥 *Critical Score:* ").append(classification.getCriticalScore()).append("/10\n");
                }
                response.append("\n");

                // ── Auto-reply for Level 1/2 (P3/P4 = low severity) ──
                String severity = classification.getSeverity();
                if ("P3".equals(severity) || "P4".equals(severity)) {
                    String selfHelp = SELF_HELP_INSTRUCTIONS.getOrDefault(
                            classification.getProductType(), null);
                    if (selfHelp != null) {
                        response.append("━━━━━━━━━━━━━━━━━━━━━━\n");
                        response.append("💡 *Quick Self-Help Tips*\n");
                        response.append("━━━━━━━━━━━━━━━━━━━━━━\n\n");
                        response.append(selfHelp).append("\n\n");
                        response.append("_If the above steps don't resolve your issue, our team will contact you within 24–48 hours._\n\n");
                    } else {
                        response.append("📋 Our team will review and resolve your complaint within 24–48 hours.\n\n");
                    }
                } else {
                    // P1/P2 — High priority
                    response.append("🚨 *This is marked as HIGH PRIORITY.*\n");
                    response.append("A specialist will be assigned shortly and will contact you within 4–8 hours.\n\n");
                }

                // AI suggested response
                if (classification.getSuggestedResponse() != null && !classification.getSuggestedResponse().isBlank()) {
                    response.append("💬 ").append(classification.getSuggestedResponse()).append("\n\n");
                }
            } else {
                response.append("\n📋 Our team will review and resolve your complaint within 24–48 hours.\n\n");
            }

            response.append("━━━━━━━━━━━━━━━━━━━━━━\n");
            response.append(getMenuMessage());

            session.setCurrentState(BotState.MAIN_MENU);
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            session.setCurrentState(BotState.MAIN_MENU);
            return "❌ We're sorry — there was a system error while registering your complaint.\n\n" +
                   "Please try again or contact us at ubi.customer.help@gmail.com\n\n" + getMenuMessage();
        }
    }

    // ═══════════════════════════════════════════
    //  COMPLAINT STATUS CHECK
    // ═══════════════════════════════════════════
    private String handleComplaintStatus(WhatsAppSession session, String message) {
        if (message.isEmpty()) {
            return "❌ Invalid input. Please enter a valid Complaint ID:";
        }
        
        Optional<Complaint> complaint = complaintRepository.findByTicketNumber(message.trim().toUpperCase());
        session.setCurrentState(BotState.MAIN_MENU);
        
        if (complaint.isPresent()) {
            Complaint c = complaint.get();
            StringBuilder sb = new StringBuilder();
            sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");
            sb.append("📊 *Complaint Status*\n");
            sb.append("━━━━━━━━━━━━━━━━━━━━━━\n\n");
            sb.append("📌 *Ticket:* ").append(c.getTicketNumber()).append("\n");
            sb.append("📋 *Status:* ").append(formatStatus(c.getStatus())).append("\n");
            sb.append("⚡ *Priority:* ").append(c.getSeverity() != null ? c.getSeverity().name() : "N/A").append("\n");
            sb.append("🏷️ *Category:* ").append(c.getProductType() != null ? c.getProductType().name().replace("_", " ") : "N/A").append("\n");
            
            if (c.getAssignedAgent() != null) {
                sb.append("👤 *Assigned Agent:* ").append(c.getAssignedAgent().getName()).append("\n");
            }
            if (c.getSlaDeadline() != null) {
                sb.append("⏰ *SLA Deadline:* ").append(c.getSlaDeadline().toLocalDate()).append("\n");
            }
            if (c.getCreatedAt() != null) {
                sb.append("📅 *Filed On:* ").append(c.getCreatedAt().toLocalDate()).append("\n");
            }
            sb.append("\n");

            if (c.getStatus() == ComplaintStatus.RESOLVED || c.getStatus() == ComplaintStatus.CLOSED) {
                sb.append("✅ Your complaint has been resolved. If you face further issues, please register a new complaint.\n\n");
            } else {
                sb.append("🔄 Your complaint is being actively worked on. We'll update you once resolved.\n\n");
            }

            sb.append(getMenuMessage());
            return sb.toString();
        } else {
            return "❌ No complaint found with ID: *" + message.trim() + "*\n\n" +
                   "Please check the ID and try again.\n\n" + getMenuMessage();
        }
    }

    // ═══════════════════════════════════════════
    //  REGISTER COMPLAINT WITH AI CLASSIFICATION
    // ═══════════════════════════════════════════
    private String registerComplaintWithAI(WhatsAppSession session, AiClassificationDTO classification) {
        String ticketNumber = "CMP-" + LocalDateTime.now().getYear() + "-" + String.format("%06d", generateRandomId());
        
        // Find or create customer
        Customer customer = customerRepository.findByCustomerNumber(session.getCustomerId()).orElse(null);
        if (customer == null && session.getCustomerEmail() != null) {
            customer = customerRepository.findByEmail(session.getCustomerEmail()).orElse(null);
        }
        if (customer == null) {
            // Create new customer with collected details
            customer = Customer.builder()
                    .customerNumber("CUST-" + session.getCustomerId())
                    .fullName(session.getCustomerName())
                    .email(session.getCustomerEmail())
                    .phone(session.getCustomerMobile())
                    .build();
            customer = customerRepository.save(customer);
        }

        if (classification != null && classification.getExtractedName() != null
                && !classification.getExtractedName().isBlank()) {
            customer.setFullName(classification.getExtractedName().trim());
            customer = customerRepository.save(customer);
        }

        Complaint complaint = new Complaint();
        complaint.setTicketNumber(ticketNumber);
        
        // Use AI-suggested title if available, otherwise generic
        String finalTitle = (classification != null && classification.getTitle() != null) 
                ? classification.getTitle() 
                : "WhatsApp Complaint — " + (classification != null ? classification.getProductType() : "General");
                
        complaint.setTitle(finalTitle);
        complaint.setDescription(session.getComplaintDescription());
        complaint.setEmail(session.getCustomerEmail());
        complaint.setProvidedCustomerId(session.getCustomerId());
        complaint.setStatus(ComplaintStatus.OPEN);
        complaint.setChannel(Channel.WHATSAPP);
        complaint.setCustomer(customer);
        complaint.setIsDuplicate(false);

        // Apply AI classification
        if (classification != null) {
            try { complaint.setProductType(ProductType.valueOf(classification.getProductType())); }
            catch (Exception e) { complaint.setProductType(ProductType.OTHER); }
            
            try { complaint.setIssueType(IssueType.valueOf(classification.getIssueType())); }
            catch (Exception e) { complaint.setIssueType(IssueType.GENERAL_INQUIRY); }

            try { complaint.setSeverity(Severity.valueOf(classification.getSeverity())); }
            catch (Exception e) { complaint.setSeverity(Severity.P3); }

            complaint.setAiConfidence(classification.getConfidence());
            complaint.setClassifiedBy(classification.getClassifiedBy());
            complaint.setCriticalScore(classification.getCriticalScore());
            complaint.setSentimentLabel(classification.getSentimentLabel());
            complaint.setSentimentScore(classification.getSentimentScore());
            complaint.setRegulatoryFlag(classification.getRegulatoryFlag());
            complaint.setAiSuggestedResponse(classification.getSuggestedResponse());
            complaint.setExtractedEntities(buildWhatsAppExtractedEntitiesJson(classification, session));
        } else {
            complaint.setProductType(ProductType.OTHER);
            complaint.setIssueType(IssueType.GENERAL_INQUIRY);
            complaint.setSeverity(Severity.P3);
            complaint.setClassifiedBy("FALLBACK");
            complaint.setAiConfidence(0.0);
            complaint.setCriticalScore(5);
        }

        complaint.setPriority(complaint.getSeverity().name());
        complaint = complaintRepository.save(complaint);

        complaint.setSlaDeadline(slaService.calculateDeadline(complaint));
        Complaint persisted = complaint;
        routingService.autoAssign(complaint).ifPresent(agent -> {
            persisted.setAssignedAgent(agent);
            persisted.setStatus(ComplaintStatus.IN_PROGRESS);
        });
        complaintRepository.save(persisted);

        return ticketNumber;
    }

    /**
     * Persists Groq extractions from the complaint description together with the WhatsApp session fields.
     */
    private String buildWhatsAppExtractedEntitiesJson(AiClassificationDTO classification, WhatsAppSession session) {
        try {
            ObjectNode root = JSON.createObjectNode();
            root.put("whatsappSessionCustomerId", session.getCustomerId());
            root.put("whatsappSessionEmail", session.getCustomerEmail());
            root.put("whatsappSessionPhone", session.getCustomerMobile());
            if (classification.getExtractedEmail() != null) {
                root.put("descriptionExtractedEmail", classification.getExtractedEmail());
            }
            if (classification.getExtractedPhone() != null) {
                root.put("descriptionExtractedPhone", classification.getExtractedPhone());
            }
            if (classification.getExtractedAccountNumber() != null) {
                root.put("descriptionExtractedAccountNumber", classification.getExtractedAccountNumber());
            }
            if (classification.getExtractedName() != null) {
                root.put("descriptionExtractedName", classification.getExtractedName());
            }
            if (classification.getExtractedEntities() != null && !classification.getExtractedEntities().isBlank()) {
                try {
                    root.set("groqEnrichment", JSON.readTree(classification.getExtractedEntities()));
                } catch (Exception e) {
                    root.put("groqEnrichmentRaw", classification.getExtractedEntities());
                }
            }
            return JSON.writeValueAsString(root);
        } catch (Exception e) {
            return classification.getExtractedEntities() != null ? classification.getExtractedEntities() : "{}";
        }
    }

    // ═══════════════════════════════════════════
    //  UTILITY METHODS
    // ═══════════════════════════════════════════
    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    }

    /**
     * Simple text similarity using Jaccard similarity on words.
     */
    private double calculateSimilarity(String text1, String text2) {
        var words1 = java.util.Set.of(text1.toLowerCase().split("\\s+"));
        var words2 = java.util.Set.of(text2.toLowerCase().split("\\s+"));

        long intersection = words1.stream().filter(words2::contains).count();
        long union = words1.size() + words2.size() - intersection;
        return union > 0 ? (double) intersection / union : 0.0;
    }

    private String formatStatus(ComplaintStatus status) {
        if (status == null) return "Unknown";
        return switch (status) {
            case OPEN -> "🔵 Open";
            case IN_PROGRESS -> "🟡 In Progress";
            case RESOLVED -> "🟢 Resolved";
            case CLOSED -> "⚪ Closed";
            default -> status.name();
        };
    }

    private int generateRandomId() {
        return 100000 + new Random().nextInt(900000); // 6 digits
    }

    private String buildTwiML(String message) {
        return "<Response><Message>" + escapeXml(message) + "</Message></Response>";
    }

    private String escapeXml(String s) {
        return s.replaceAll("&", "&amp;")
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;")
                .replaceAll("'", "&apos;");
    }
}
