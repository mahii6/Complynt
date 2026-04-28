package com.example.Hackathon.util;

import java.util.Arrays;
import java.util.List;

public class EmailValidationUtil {

    private static final List<String> INVALID_SENDERS = Arrays.asList(
            "noreply@", "no-reply@", "do-not-reply@", "system@", "mailer-daemon@"
    );

    /**
     * Single words that strongly suggest automated / transactional mail (not user complaints).
     * Do NOT include "account" or "security" — legitimate banking complaints often say
     * "bank account", "account frozen", "security concern", etc.
     */
    private static final List<String> SUBJECT_FORBIDDEN_KEYWORDS = Arrays.asList(
            "otp", "welcome"
    );

    /** Phrases typical of system mailers (substring match on normalized lower subject). */
    private static final List<String> SUBJECT_FORBIDDEN_PHRASES = Arrays.asList(
            "verify your account",
            "login to your account",
            "reset your password",
            "password reset",
            "two-factor",
            "2fa code",
            "do not reply",
            "automatically generated"
    );


    public static boolean isValidSender(String email) {
        if (email == null || email.isEmpty()) return false;
        String lowerEmail = email.toLowerCase();
        for (String invalidSender : INVALID_SENDERS) {
            if (lowerEmail.contains(invalidSender)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isValidSubject(String subject) {
        if (subject == null || subject.trim().isEmpty()) {
            return false;
        }
        String lowerSubject = subject.toLowerCase();

        for (String forbidden : SUBJECT_FORBIDDEN_KEYWORDS) {
            if (lowerSubject.contains(forbidden)) {
                return false;
            }
        }

        for (String phrase : SUBJECT_FORBIDDEN_PHRASES) {
            if (lowerSubject.contains(phrase)) {
                return false;
            }
        }

        return true;
    }

    public static boolean isValidBody(String body) {
        if (body == null) {
            return false;
        }
        String trimmed = body.trim();
        return trimmed.length() >= 20;
    }

    public static String normalizeSubject(String subject) {
        if (subject == null) return "No Subject";
        // Remove prefixes like "Re:", "Fwd:", "FW:", etc.
        return subject.replaceAll("(?i)^(re|fwd|fw|reply):\\s*", "").trim();
    }
}
