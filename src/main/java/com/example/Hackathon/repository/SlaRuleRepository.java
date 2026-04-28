package com.example.Hackathon.repository;

import com.example.Hackathon.entity.SlaRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SlaRuleRepository extends JpaRepository<SlaRule, Long> {
    Optional<SlaRule> findBySeverityAndProductType(String severity, String productType);
    Optional<SlaRule> findBySeverityAndProductTypeIsNull(String severity);
}
