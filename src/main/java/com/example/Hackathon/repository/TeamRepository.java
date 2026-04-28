package com.example.Hackathon.repository;

import com.example.Hackathon.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByCategory(String category);
}
