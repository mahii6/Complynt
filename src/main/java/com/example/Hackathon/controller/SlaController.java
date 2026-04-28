package com.example.Hackathon.controller;

import com.example.Hackathon.dto.ComplaintResponseDTO;
import com.example.Hackathon.dto.SlaStatusDTO;
import com.example.Hackathon.entity.SlaRule;
import com.example.Hackathon.repository.SlaRuleRepository;
import com.example.Hackathon.service.ComplaintService;
import com.example.Hackathon.service.SlaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sla")
@CrossOrigin("*")
public class SlaController {

    @Autowired
    private SlaService slaService;

    @Autowired
    private SlaRuleRepository slaRuleRepository;

    @Autowired
    private ComplaintService complaintService;

    @GetMapping("/{complaintId}")
    public ResponseEntity<SlaStatusDTO> getStatus(@PathVariable Long complaintId) {
        return ResponseEntity.ok(slaService.getStatusById(complaintId));
    }

    @GetMapping("/breached")
    public ResponseEntity<?> getBreached() {
        var breached = slaService.getBreachedComplaints().stream()
                .map(c -> complaintService.getById(c.getId()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(breached);
    }

    @GetMapping("/escalated")
    public ResponseEntity<List<ComplaintResponseDTO>> getEscalated() {
        return ResponseEntity.ok(complaintService.getEscalatedComplaints());
    }

    @GetMapping("/rules")
    public ResponseEntity<List<SlaRule>> getRules() {
        return ResponseEntity.ok(slaRuleRepository.findAll());
    }

    @PostMapping("/rules")
    public ResponseEntity<SlaRule> createRule(@RequestBody SlaRule rule) {
        return ResponseEntity.status(HttpStatus.CREATED).body(slaRuleRepository.save(rule));
    }
}
