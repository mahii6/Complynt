package com.example.Hackathon.service;

import com.example.Hackathon.dto.BotState;
import com.example.Hackathon.dto.WhatsAppSession;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class WhatsAppSessionService {

    private final ConcurrentHashMap<String, WhatsAppSession> sessions = new ConcurrentHashMap<>();

    public WhatsAppSession getSession(String phoneNumber) {
        return sessions.computeIfAbsent(phoneNumber, WhatsAppSession::new);
    }

    public void updateSession(WhatsAppSession session) {
        sessions.put(session.getPhoneNumber(), session);
    }

    public void clearSession(String phoneNumber) {
        sessions.remove(phoneNumber);
    }
}
