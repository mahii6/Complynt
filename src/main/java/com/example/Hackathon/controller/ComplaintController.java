package com.example.Hackathon.controller;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.Hackathon.dto.AgentNotifyDTO;
import com.example.Hackathon.dto.AssignAgentDTO;
import com.example.Hackathon.dto.CommentCreateDTO;
import com.example.Hackathon.dto.ComplaintCommentDTO;
import com.example.Hackathon.dto.ComplaintCreateDTO;
import com.example.Hackathon.dto.ComplaintFilterDTO;
import com.example.Hackathon.dto.ComplaintResponseDTO;
import com.example.Hackathon.dto.EscalateComplaintDTO;
import com.example.Hackathon.dto.StatusUpdateDTO;
import com.example.Hackathon.enums.ComplaintStatus;
import com.example.Hackathon.enums.ProductType;
import com.example.Hackathon.enums.Severity;
import com.example.Hackathon.service.ComplaintService;
import com.example.Hackathon.service.SlaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/complaints")
@CrossOrigin("*")
public class ComplaintController {

    @Autowired
    private ComplaintService complaintService;

    @Autowired
    private SlaService slaService;

    @PostMapping
    public ResponseEntity<ComplaintResponseDTO> create(@Valid @RequestBody ComplaintCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(complaintService.create(dto));
    }

    @GetMapping
    public ResponseEntity<Page<ComplaintResponseDTO>> getAll(
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false) ProductType productType,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        ComplaintFilterDTO filter = ComplaintFilterDTO.builder()
                .status(status)
                .productType(productType)
                .severity(severity)
                .agentId(agentId)
                .fromDate(fromDate)
                .toDate(toDate)
                .page(page)
                .size(size)
                .build();

        return ResponseEntity.ok(complaintService.getAll(filter));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComplaintResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(complaintService.getById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ComplaintResponseDTO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateDTO dto) {
        return ResponseEntity.ok(complaintService.updateStatus(id, dto, "AGENT"));
    }

    /**
     * Sets status to ESCALATED, assigns {@link EscalateComplaintDTO#getTargetAgentId()},
     * stores optional handoff note, and emails the assignee when enabled.
     */
    @PostMapping("/{id}/escalate")
    public ResponseEntity<ComplaintResponseDTO> escalate(
            @PathVariable Long id,
            @Valid @RequestBody EscalateComplaintDTO dto) {
        return ResponseEntity.ok(complaintService.escalateComplaint(id, dto, "AGENT"));
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<ComplaintResponseDTO> assignAgent(
            @PathVariable Long id,
            @Valid @RequestBody AssignAgentDTO dto) {
        return ResponseEntity.ok(complaintService.assignAgent(id, dto.getAgentId(), "AGENT"));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<ComplaintCommentDTO> addComment(
            @PathVariable Long id,
            @RequestBody CommentCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(complaintService.addComment(id, dto));
    }

    @PostMapping("/{id}/classify")
    public ResponseEntity<ComplaintResponseDTO> classify(@PathVariable Long id) {
        return ResponseEntity.ok(complaintService.classify(id));
    }

    @PostMapping("/{id}/notify")
    public ResponseEntity<?> notifyCustomer(
            @PathVariable Long id,
            @Valid @RequestBody AgentNotifyDTO dto) {
        try {
            return ResponseEntity.ok(complaintService.notifyCustomer(id, dto));
        } catch (IllegalStateException | IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg == null && e.getCause() != null) {
                msg = e.getCause().getMessage();
            }
            return ResponseEntity.badRequest().body(Map.of("message", msg != null ? msg : "Notification failed"));
        }
    }

    @GetMapping("/breached-sla")
    public ResponseEntity<?> getBreachedSla() {
        var breached = slaService.getBreachedComplaints().stream()
                .map(c -> complaintService.getById(c.getId()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(breached);
    }
}
