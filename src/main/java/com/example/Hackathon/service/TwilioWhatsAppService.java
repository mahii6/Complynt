package com.example.Hackathon.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Optional outbound WhatsApp via Twilio REST API. Disabled when credentials are blank.
 */
@Service
public class TwilioWhatsAppService {

    private final RestTemplate restTemplate;

    @Value("${app.twilio.account-sid:}")
    private String accountSid;

    @Value("${app.twilio.auth-token:}")
    private String authToken;

    /** e.g. whatsapp:+14155238886 */
    @Value("${app.twilio.whatsapp-from:}")
    private String whatsappFrom;

    public TwilioWhatsAppService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean isConfigured() {
        return accountSid != null && !accountSid.isBlank()
                && authToken != null && !authToken.isBlank()
                && whatsappFrom != null && !whatsappFrom.isBlank();
    }

    /**
     * @param toPhone E.164 or digits; will be prefixed with whatsapp:
     */
    public boolean sendWhatsApp(String toPhone, String body) {
        if (!isConfigured()) {
            System.err.println("Twilio WhatsApp: not configured (set app.twilio.* in application.properties)");
            return false;
        }
        String to = toPhone.startsWith("whatsapp:") ? toPhone : "whatsapp:" + ensureE164(toPhone);
        String from = formatWhatsAppAddress(whatsappFrom);

        String url = String.format(
                "https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json",
                accountSid);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(accountSid, authToken);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", to);
        form.add("From", from);
        form.add("Body", body);

        try {
            restTemplate.postForEntity(url, new HttpEntity<>(form, headers), String.class);
            return true;
        } catch (Exception e) {
            System.err.println("Twilio WhatsApp send failed: " + e.getMessage());
            return false;
        }
    }

    private static String formatWhatsAppAddress(String configured) {
        if (configured == null || configured.isBlank()) {
            return "";
        }
        if (configured.startsWith("whatsapp:")) {
            return configured;
        }
        return "whatsapp:" + ensureE164(configured);
    }

    private static String ensureE164(String raw) {
        String p = raw.replaceAll("[\\s-]", "");
        if (p.startsWith("+")) {
            return p;
        }
        if (p.length() == 10) {
            return "+91" + p;
        }
        return p;
    }
}
