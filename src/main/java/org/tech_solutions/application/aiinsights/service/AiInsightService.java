package org.tech_solutions.application.aiinsights.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.tech_solutions.application.aiinsights.model.AiInsight;
import org.tech_solutions.application.aiinsights.repository.AiInsightRepository;
import org.tech_solutions.application.security.CurrentUserService;
import org.tech_solutions.application.shared.exception.EntityNotFoundException;
import org.tech_solutions.application.user.model.User;
import org.tech_solutions.application.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AiInsightService {

    private final AiInsightRepository aiInsightRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public AiInsightService(AiInsightRepository aiInsightRepository, UserRepository userRepository, CurrentUserService currentUserService) {
        this.aiInsightRepository = aiInsightRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    public AiInsight create(AiInsight insight, Long userId) {
        insight.setUser(currentUserService.requireCurrentUser());
        insight.setGeneratedAt(LocalDateTime.now());
        return aiInsightRepository.save(insight);
    }

    public List<AiInsight> listAll() {
        return aiInsightRepository.findByUserId(currentUserService.requireCurrentUserId());
    }

    public List<AiInsight> listByUser(Long userId) {
        return aiInsightRepository.findByUserId(currentUserService.requireCurrentUserId());
    }

    public AiInsight findById(Long id) {
        AiInsight insight = aiInsightRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Insight nao encontrado"));
        assertOwnedByCurrentUser(insight);
        return insight;
    }

    public AiInsight update(Long id, AiInsight updated, Long userId) {
        AiInsight current = findById(id);
        current.setUser(currentUserService.requireCurrentUser());
        current.setInsightType(updated.getInsightType());
        current.setContent(updated.getContent());
        return aiInsightRepository.save(current);
    }

    public void delete(Long id) {
        aiInsightRepository.delete(findById(id));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado"));
    }

    private void assertOwnedByCurrentUser(AiInsight insight) {
        Long currentUserId = currentUserService.requireCurrentUserId();
        if (insight.getUser() == null || !currentUserId.equals(insight.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Recurso nao pertence ao usuario autenticado");
        }
    }
}


