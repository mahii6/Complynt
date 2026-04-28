package com.example.Hackathon.repository;

import com.example.Hackathon.entity.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AgentRepository extends JpaRepository<Agent, Long> {
    List<Agent> findByTeamCategoryAndActive(String category, boolean active);
    List<Agent> findByActive(boolean active);
}
