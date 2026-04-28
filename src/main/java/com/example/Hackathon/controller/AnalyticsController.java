package com.example.Hackathon.controller;

import com.example.Hackathon.dto.AnalyticsSummaryDTO;
import com.example.Hackathon.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin("*")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummaryDTO> getSummary() {
        return ResponseEntity.ok(analyticsService.getSummary());
    }

    @GetMapping("/trend")
    public ResponseEntity<List<Map<String, Object>>> getDailyTrend(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(analyticsService.getDailyTrend(days));
    }
}
