package com.example.Hackathon.scheduler;

import com.example.Hackathon.service.EscalationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SlaScheduler {

    @Autowired
    private EscalationService escalationService;

    @Scheduled(fixedRate = 300000) // every 5 minutes
    public void runSlaCheck() {
        escalationService.checkAllOpen();
    }
}
