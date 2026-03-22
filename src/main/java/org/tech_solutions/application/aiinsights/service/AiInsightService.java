package org.tech_solutions.application.aiinsights.service;

import org.springframework.stereotype.Service;
import org.tech_solutions.application.aiinsights.model.AiInsight;
import org.tech_solutions.application.aiinsights.repository.AiInsightRepository;
import org.tech_solutions.application.shared.exception.EntityNotFoundException;
import org.tech_solutions.application.user.model.User;
import org.tech_solutions.application.user.repository.UserRepository;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AiInsightService {

    private final AiInsightRepository aiInsightRepository;
    private final UserRepository userRepository;

    public AiInsightService(AiInsightRepository aiInsightRepository, UserRepository userRepository) {
        this.aiInsightRepository = aiInsightRepository;
        this.userRepository = userRepository;
    }

    public AiInsight create(AiInsight insight, BigInteger userId) {
        insight.setUser(findUser(userId));
        insight.setGeneratedAt(LocalDateTime.now());
        return aiInsightRepository.save(insight);
    }

    public List<AiInsight> listAll() {
        return aiInsightRepository.findAll();
    }

    public List<AiInsight> listByUser(BigInteger userId) {
        findUser(userId);
        return aiInsightRepository.findByUserId(userId);
    }

    public AiInsight findById(BigInteger id) {
        return aiInsightRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Insight nao encontrado"));
    }

    public AiInsight update(BigInteger id, AiInsight updated, BigInteger userId) {
        AiInsight current = findById(id);
        current.setUser(findUser(userId));
        current.setInsightType(updated.getInsightType());
        current.setContent(updated.getContent());
        return aiInsightRepository.save(current);
    }

    public void delete(BigInteger id) {
        aiInsightRepository.delete(findById(id));
    }

    private User findUser(BigInteger userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado"));
    }
}

