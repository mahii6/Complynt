package com.example.Hackathon.service;

import com.example.Hackathon.dto.AiClassificationDTO;
import com.example.Hackathon.dto.EmailWebhookPayload;
import com.example.Hackathon.entity.Complaint;
import com.example.Hackathon.entity.Customer;
import com.example.Hackathon.enums.Channel;
import com.example.Hackathon.enums.ComplaintStatus;
import com.example.Hackathon.enums.IssueType;
import com.example.Hackathon.enums.Severity;
import com.example.Hackathon.enums.ProductType;
import com.example.Hackathon.repository.ComplaintRepository;
import com.example.Hackathon.util.EmailValidationUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmailComplaintService {

    private final ComplaintRepository complaintRepository;
    private final EmailSenderService emailSenderService;
    private final AiGatewayService aiGatewayService;
    private final CustomerResolutionService customerResolutionService;
    private final RoutingService routingService;
    private final SlaService slaService;

    private static final ObjectMapper JSON = new ObjectMapper();

    // Matches cases like: Customer ID: 12345, CustID=12345, ID: 12345, Customer-Id 12345 (Strictly Digits)
    private static final Pattern CUSTOMER_ID_PATTERN = Pattern.compile("(?i)(?:customer[-\\s]?id|custid|id)\\s*[:=]?\\s*(\\d+)");
    private static final Pattern ANGLE_BRACKET_EMAIL = Pattern.compile("<([^<>\\s]+@[^<>\\s]+)>");
    private static final Pattern LOOSE_EMAIL = Pattern.compile("[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}");

    private static final Set<String> SUBJECT_STOPWORDS = new HashSet<>(Arrays.asList(
            "re", "fwd", "fw", "the", "a", "an", "of", "and", "or", "to", "for", "in", "on", "at", "is", "with", "my", "our"));

    public boolean processEmailComplaint(EmailWebhookPayload payload) {
        String email = extractEmailAddress(payload.getSenderEmail());
        String originalSubject = payload.getSubject();
        String rawBody = payload.getEmailBody();
        String cleanBody = cleanBody(rawBody);

        // 1. Validation Pipeline
        if (email == null || email.isBlank()) {
            System.out.println("❌ Email Registration Blocked [No sender address]: " + payload.getSenderEmail());
            return false;
        }

        if (!EmailValidationUtil.isValidSender(email)) {
            System.out.println("❌ Email Registration Blocked [Invalid Sender]: " + email);
            return false;
        }

        if (!EmailValidationUtil.isValidSubject(originalSubject)) {
            if (isSubjectSimilarToRecentComplaints(originalSubject)) {
                System.out.println("✅ Subject allowed via similarity to existing complaint titles: " + originalSubject);
            } else {
                System.out.println("❌ Email Registration Blocked [Invalid Subject]: " + originalSubject);
                return false;
            }
        }

        if (!EmailValidationUtil.isValidBody(cleanBody)) {
            System.out.println("❌ Email Registration Blocked [Invalid Body - Length/Keywords missing]");
            return false;
        }

        String normalizedSubject = EmailValidationUtil.normalizeSubject(originalSubject);

        // 2. Detect duplicates within the last 10 minutes
        LocalDateTime tenMinsAgo = LocalDateTime.now().minusMinutes(10);
        List<Complaint> recentComplaints = complaintRepository.findByDateRange(tenMinsAgo, LocalDateTime.now());
        boolean isDuplicate = recentComplaints.stream()
                .anyMatch(c -> email.equalsIgnoreCase(c.getEmail()) && normalizedSubject.equalsIgnoreCase(c.getTitle()));

        if (isDuplicate) {
            System.out.println("⚠️ Duplicate complaint detected and ignored from " + email + " for subject: " + normalizedSubject);
            return false; // Safely exit without storing duplicate
        }

        // 3. Extract Customer ID (regex) — Groq may refine in step 4
        String regexCustomerId = extractCustomerId(cleanBody);

        // 4. AI classification + Groq enrichment (severity + extract email / phone / account / name from body)
        AiClassificationDTO classification;
        try {
            classification = aiGatewayService.classifyForEmail(cleanBody);
            System.out.println("🤖 AI Classification: " + classification.getProductType() +
                    " | " + classification.getSeverity() +
                    " | Classified by: " + classification.getClassifiedBy() +
                    " | Confidence: " + classification.getConfidence());
        } catch (Exception e) {
            System.out.println("⚠️ AI classification failed, using fallback: " + e.getMessage());
            classification = null;
        }

        // 5. Severity / product / issue (Groq-enriched severity when configured)
        Severity severityEnum;
        ProductType productType;
        IssueType issueType;
        String priority;

        if (classification != null) {
            try { severityEnum = Severity.valueOf(classification.getSeverity()); }
            catch (Exception e) { severityEnum = Severity.P3; }

            try { productType = ProductType.valueOf(classification.getProductType()); }
            catch (Exception e) { productType = ProductType.OTHER; }

            try { issueType = IssueType.valueOf(classification.getIssueType()); }
            catch (Exception e) { issueType = IssueType.GENERAL_INQUIRY; }

            priority = severityEnum.name();
        } else {
            priority = determinePriority(cleanBody);
            severityEnum = mapPriorityToSeverity(priority);
            productType = ProductType.OTHER;
            issueType = IssueType.GENERAL_INQUIRY;
        }

        String providedCustomerId = regexCustomerId;
        if (classification != null && classification.getExtractedAccountNumber() != null
                && "UNKNOWN".equals(regexCustomerId)) {
            providedCustomerId = classification.getExtractedAccountNumber();
        }

        // 6. Resolve or create customer (sender email + Groq extractions)
        Customer customer = customerResolutionService.resolveForInboundComplaint(
                classification, Channel.EMAIL, email, null, regexCustomerId);

        // 7. Create Complaint
        String ticketNumber = "CMP-" + LocalDateTime.now().getYear() + "-" + generateRandomId();

        Complaint complaint = new Complaint();
        complaint.setTicketNumber(ticketNumber);
        
        // Use AI title if available, otherwise fallback to normalized email subject
        String finalTitle = (classification != null && classification.getTitle() != null) 
                ? classification.getTitle() : normalizedSubject;
        
        complaint.setTitle(finalTitle);
        complaint.setDescription(cleanBody);
        complaint.setEmail(email);
        complaint.setProvidedCustomerId(providedCustomerId);
        complaint.setPriority(priority);
        complaint.setStatus(ComplaintStatus.OPEN);
        complaint.setSeverity(severityEnum);
        complaint.setProductType(productType);
        complaint.setIssueType(issueType);
        complaint.setChannel(Channel.EMAIL);
        complaint.setCustomer(customer);
        complaint.setIsDuplicate(false);

        // Apply AI classification fields
        if (classification != null) {
            complaint.setAiConfidence(classification.getConfidence());
            complaint.setClassifiedBy(classification.getClassifiedBy());
            complaint.setCriticalScore(classification.getCriticalScore());
            complaint.setSentimentLabel(classification.getSentimentLabel());
            complaint.setSentimentScore(classification.getSentimentScore());
            complaint.setRegulatoryFlag(classification.getRegulatoryFlag());
            complaint.setAiSuggestedResponse(classification.getSuggestedResponse());
            complaint.setExtractedEntities(buildEmailExtractedEntitiesJson(classification, email));
        }

        complaint = complaintRepository.save(complaint);

        complaint.setSlaDeadline(slaService.calculateDeadline(complaint));
        Complaint persisted = complaint;
        routingService.autoAssign(complaint).ifPresent(agent -> {
            persisted.setAssignedAgent(agent);
            persisted.setStatus(ComplaintStatus.IN_PROGRESS);
        });
        complaintRepository.save(persisted);
        System.out.println("✅ Accepted and saved complaint " + ticketNumber + " from " + email +
                " [AI: " + (classification != null ? classification.getClassifiedBy() : "NONE") + "]");

        // 7. Send Confirmation ONLY to the user (the sender)
        emailSenderService.sendComplaintConfirmation(email, ticketNumber, normalizedSubject);
        return true;
    }

    private String cleanBody(String body) {
        if (body == null) return "";
        String cleaned = body.replaceAll("\\s+", " ").trim();
        int sigIndex = cleaned.indexOf("-- ");
        if (sigIndex != -1) {
            cleaned = cleaned.substring(0, sigIndex);
        }
        return cleaned;
    }

    private String extractCustomerId(String body) {
        Matcher matcher = CUSTOMER_ID_PATTERN.matcher(body);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "UNKNOWN";
    }

    private String determinePriority(String body) {
        String lowerBody = body.toLowerCase();
        if (lowerBody.contains("urgent") || lowerBody.contains("asap") || lowerBody.contains("immediately")) {
            return "HIGH";
        } else if (lowerBody.contains("minor") || lowerBody.contains("not urgent")) {
            return "LOW";
        }
        return "MEDIUM";
    }

    private Severity mapPriorityToSeverity(String priority) {
        switch (priority) {
            case "HIGH": return Severity.P1;
            case "LOW": return Severity.P4;
            default: return Severity.P3; // MEDIUM
        }
    }

    private int generateRandomId() {
        return 1000 + new Random().nextInt(9000);
    }

    private String buildEmailExtractedEntitiesJson(AiClassificationDTO classification, String senderEmail) {
        try {
            ObjectNode root = JSON.createObjectNode();
            root.put("channel", "EMAIL");
            root.put("senderEmail", senderEmail);
            if (classification.getExtractedName() != null) {
                root.put("descriptionExtractedName", classification.getExtractedName());
            }
            if (classification.getExtractedEmail() != null) {
                root.put("descriptionExtractedEmail", classification.getExtractedEmail());
            }
            if (classification.getExtractedPhone() != null) {
                root.put("descriptionExtractedPhone", classification.getExtractedPhone());
            }
            if (classification.getExtractedAccountNumber() != null) {
                root.put("descriptionExtractedAccountNumber", classification.getExtractedAccountNumber());
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

    /**
     * IMAP / webhooks often send {@code Name <user@gmail.com>}; duplicate checks need the bare address.
     */
    private static String extractEmailAddress(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        Matcher angle = ANGLE_BRACKET_EMAIL.matcher(s);
        if (angle.find()) {
            return angle.group(1).trim().toLowerCase(Locale.ROOT);
        }
        Matcher loose = LOOSE_EMAIL.matcher(s);
        if (loose.find()) {
            return loose.group().trim().toLowerCase(Locale.ROOT);
        }
        return null;
    }

    /**
     * If strict subject rules fail, allow inbound mail whose subject is close to prior complaint titles (Jaccard on words).
     */
    private boolean isSubjectSimilarToRecentComplaints(String subject) {
        String norm = EmailValidationUtil.normalizeSubject(subject).toLowerCase(Locale.ROOT);
        if (norm.isBlank()) {
            return false;
        }
        Set<String> words = tokenizeSubjectWords(norm);
        if (words.size() < 2) {
            return false;
        }
        List<String> titles = complaintRepository.findRecentTitles(PageRequest.of(0, 120));
        for (String t : titles) {
            if (t == null || t.isBlank()) {
                continue;
            }
            String nt = EmailValidationUtil.normalizeSubject(t).toLowerCase(Locale.ROOT);
            Set<String> tw = tokenizeSubjectWords(nt);
            double sim = jaccardSimilarity(words, tw);
            if (sim >= 0.28) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> tokenizeSubjectWords(String s) {
        return Arrays.stream(s.split("\\s+"))
                .map(w -> w.replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT))
                .filter(w -> w.length() > 1 && !SUBJECT_STOPWORDS.contains(w))
                .collect(Collectors.toSet());
    }

    private static double jaccardSimilarity(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        long inter = a.stream().filter(b::contains).count();
        long union = a.size() + b.size() - inter;
        return union == 0 ? 0.0 : (double) inter / union;
    }
}
