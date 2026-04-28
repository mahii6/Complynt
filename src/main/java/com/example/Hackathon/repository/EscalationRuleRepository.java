package com.example.Hackathon.repository;

import com.example.Hackathon.entity.EscalationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EscalationRuleRepository extends JpaRepository<EscalationRule, Long> {
    List<EscalationRule> findBySeverity(String severity);
}
