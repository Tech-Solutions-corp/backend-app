package org.tech_solutions.application.aiinsights.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tech_solutions.application.aiinsights.model.AiInsight;

import java.util.List;

@Repository
public interface AiInsightRepository extends JpaRepository<AiInsight, Long> {
    List<AiInsight> findByUserId(Long userId);
}


