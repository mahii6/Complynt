package com.example.Hackathon.controller;

import com.example.Hackathon.service.WhatsAppBotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class WhatsAppController {

    private final WhatsAppBotService botService;

    @PostMapping(value = "/webhook/whatsapp", produces = MediaType.APPLICATION_XML_VALUE)
    public String handleWhatsAppWebhook(@RequestParam Map<String, String> payload) {
        return botService.processMessage(payload);
    }
}
