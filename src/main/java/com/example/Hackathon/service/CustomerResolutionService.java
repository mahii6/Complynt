package com.example.Hackathon.service;

import com.example.Hackathon.dto.AiClassificationDTO;
import com.example.Hackathon.entity.Customer;
import com.example.Hackathon.enums.Channel;
import com.example.Hackathon.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

/**
 * Links inbound complaints to {@link Customer} rows using Groq extractions, sender identity,
 * regex ids, and name matching against existing records.
 */
@Service
@RequiredArgsConstructor
public class CustomerResolutionService {

    private final CustomerRepository customerRepository;

    public Customer resolveForInboundComplaint(
            AiClassificationDTO classification,
            Channel channel,
            String senderEmail,
            String sessionOrSenderPhone,
            String regexCustomerOrAccountId) {

        String extEmail = classification != null ? classification.getExtractedEmail() : null;
        String extPhone = classification != null ? classification.getExtractedPhone() : null;
        String extAcct = classification != null ? classification.getExtractedAccountNumber() : null;
        String extName = classification != null ? classification.getExtractedName() : null;

        String email = firstNonBlank(extEmail, senderEmail);
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }

        String phone = firstNonBlank(sessionOrSenderPhone, extPhone);
        if (phone != null) {
            phone = normalizePhone(phone);
        }

        String lookupToken = firstNonBlank(regexCustomerOrAccountId, extAcct);
        if ("UNKNOWN".equalsIgnoreCase(lookupToken)) {
            lookupToken = extAcct;
        }

        Optional<Customer> found = findByAnyIdentifier(lookupToken);
        if (found.isEmpty() && email != null) {
            found = customerRepository.findByEmail(email);
        }
        if (found.isEmpty() && phone != null) {
            found = customerRepository.findByPhone(phone);
        }
        if (found.isEmpty() && extName != null && !extName.isBlank()) {
            found = customerRepository.findByFullNameIgnoreCase(extName.trim());
        }

        if (found.isPresent()) {
            Customer c = found.get();
            boolean dirty = false;
            if (c.getEmail() == null && email != null) {
                c.setEmail(email);
                dirty = true;
            }
            if (c.getPhone() == null && phone != null) {
                c.setPhone(phone);
                dirty = true;
            }
            if (c.getAccountNumber() == null && extAcct != null) {
                c.setAccountNumber(extAcct.trim());
                dirty = true;
            }
            if (extName != null && !extName.isBlank()) {
                String trimmed = extName.trim();
                if (c.getFullName() == null || c.getFullName().isBlank()
                        || isGenericPlaceholderName(c.getFullName())) {
                    c.setFullName(trimmed);
                    dirty = true;
                }
            }
            return dirty ? customerRepository.save(c) : c;
        }

        String derivedFromEmail = deriveDisplayNameFromEmail(senderEmail);
        String fullName = firstNonBlank(
                extName != null ? extName.trim() : null,
                derivedFromEmail,
                channel == Channel.EMAIL ? "Email customer" : "Messaging customer");

        Customer created = Customer.builder()
                .fullName(fullName)
                .email(email)
                .phone(phone)
                .accountNumber(extAcct != null ? extAcct.trim() : null)
                .build();
        created = customerRepository.save(created);
        created.setCustomerNumber("CUST-" + String.format("%04d", created.getId()));
        return customerRepository.save(created);
    }

    private static boolean isGenericPlaceholderName(String name) {
        if (name == null) {
            return true;
        }
        String n = name.trim().toLowerCase(Locale.ROOT);
        return n.equals("email customer") || n.equals("messaging customer");
    }

    private static String deriveDisplayNameFromEmail(String senderEmail) {
        if (senderEmail == null || !senderEmail.contains("@")) {
            return null;
        }
        String local = senderEmail.substring(0, senderEmail.indexOf('@')).replace(".", " ").replace("_", " ").trim();
        if (local.length() < 2) {
            return null;
        }
        return local.substring(0, 1).toUpperCase(Locale.ROOT) + local.substring(1);
    }

    private Optional<Customer> findByAnyIdentifier(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String t = raw.trim();
        Optional<Customer> o = customerRepository.findByCustomerNumber(t);
        if (o.isPresent()) {
            return o;
        }
        if (t.matches("\\d{6,14}")) {
            o = customerRepository.findByCustomerNumber("CUST-" + t);
            if (o.isPresent()) {
                return o;
            }
            o = customerRepository.findByAccountNumber(t);
            if (o.isPresent()) {
                return o;
            }
        }
        return Optional.empty();
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }

    private static String firstNonBlank(String a, String b, String c) {
        String x = firstNonBlank(a, b);
        return firstNonBlank(x, c);
    }

    private static String normalizePhone(String phone) {
        String p = phone.replaceAll("[\\s-]", "");
        if (p.startsWith("+")) {
            return p;
        }
        if (p.length() == 10) {
            return "+91" + p;
        }
        return p;
    }
}
