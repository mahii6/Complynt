package com.example.Hackathon.controller;

import com.example.Hackathon.dto.AgentSummaryDTO;
import com.example.Hackathon.dto.ComplaintResponseDTO;
import com.example.Hackathon.entity.Agent;
import com.example.Hackathon.repository.AgentRepository;
import com.example.Hackathon.service.ComplaintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/agents")
@CrossOrigin("*")
public class AgentController {

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private ComplaintService complaintService;

    @GetMapping
    public ResponseEntity<List<AgentSummaryDTO>> getAll() {
        List<AgentSummaryDTO> agents = agentRepository.findByActive(true).stream()
                .map(a -> AgentSummaryDTO.builder()
                        .id(a.getId())
                        .name(a.getName())
                        .email(a.getEmail())
                        .role(a.getRole())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(agents);
    }

    @PostMapping
    public ResponseEntity<AgentSummaryDTO> create(@RequestBody Agent agent) {
        agent.setActive(true);
        Agent saved = agentRepository.save(agent);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                AgentSummaryDTO.builder()
                        .id(saved.getId())
                        .name(saved.getName())
                        .email(saved.getEmail())
                        .role(saved.getRole())
                        .build()
        );
    }

    @GetMapping("/{id}/complaints")
    public ResponseEntity<Page<ComplaintResponseDTO>> getComplaints(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(complaintService.getByAgent(id, page, size));
    }
}
