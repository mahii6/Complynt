package com.example.Hackathon.service;

import com.example.Hackathon.entity.Agent;
import com.example.Hackathon.entity.Complaint;
import com.example.Hackathon.enums.ComplaintStatus;
import com.example.Hackathon.repository.AgentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class RoutingService {

    @Autowired
    private AgentRepository agentRepository;

    /** Rotate among least-loaded agents so cases are not always assigned to the same person. */
    private final AtomicInteger roundRobin = new AtomicInteger(0);

    private long countOpen(Agent agent) {
        if (agent.getAssignedComplaints() == null) {
            return 0L;
        }
        return agent.getAssignedComplaints().stream()
                .filter(c -> c.getStatus() != ComplaintStatus.RESOLVED
                        && c.getStatus() != ComplaintStatus.CLOSED)
                .count();
    }

    public Optional<Agent> autoAssign(Complaint complaint) {
        String category = complaint.getProductType() != null ? complaint.getProductType().name() : "OTHER";

        // 1. Find active agents in matching team
        List<Agent> teamAgents = agentRepository.findByTeamCategoryAndActive(category, true);

        // 2. Fallback to any active agent
        if (teamAgents.isEmpty()) {
            teamAgents = agentRepository.findByActive(true);
        }

        if (teamAgents.isEmpty()) {
            return Optional.empty();
        }

        // 3. Among agents with minimum open load, rotate (round-robin) so work is spread evenly
        long minLoad = teamAgents.stream().mapToLong(this::countOpen).min().orElse(0L);
        List<Agent> candidates = teamAgents.stream()
                .filter(a -> countOpen(a) == minLoad)
                .sorted(Comparator.comparing(Agent::getId))
                .collect(Collectors.toList());
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        int idx = Math.floorMod(roundRobin.getAndIncrement(), candidates.size());
        return Optional.of(candidates.get(idx));
    }
}
