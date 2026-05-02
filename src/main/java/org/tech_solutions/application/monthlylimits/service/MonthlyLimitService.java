package org.tech_solutions.application.monthlylimits.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.tech_solutions.application.monthlylimits.model.MonthlyLimit;
import org.tech_solutions.application.monthlylimits.repository.MonthlyLimitRepository;
import org.tech_solutions.application.security.CurrentUserService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MonthlyLimitService {

    private final MonthlyLimitRepository monthlyLimitRepository;
    private final CurrentUserService currentUserService;

    public MonthlyLimitService(
            MonthlyLimitRepository monthlyLimitRepository,
            CurrentUserService currentUserService) {
        this.monthlyLimitRepository = monthlyLimitRepository;
        this.currentUserService = currentUserService;
    }

    public MonthlyLimit save(MonthlyLimit limit, Long userId) {
        Long currentUserId = currentUserService.requireCurrentUserId();
        if (!currentUserId.equals(userId)) {
            throw new EntityNotFoundException("Limite nao encontrado");
        }

        LocalDate normalizedMonth = limit.getReferenceMonth().withDayOfMonth(1);
        MonthlyLimit current = monthlyLimitRepository
                .findByUserIdAndReferenceMonth(currentUserId, normalizedMonth)
                .orElseGet(MonthlyLimit::new);

        current.setUser(currentUserService.requireCurrentUser());
        current.setReferenceMonth(normalizedMonth);
        current.setAmount(limit.getAmount());

        LocalDateTime now = LocalDateTime.now();
        if (current.getCreatedAt() == null) {
            current.setCreatedAt(now);
        }
        current.setUpdatedAt(now);

        return monthlyLimitRepository.save(current);
    }

    public List<MonthlyLimit> listByUser(Long userId) {
        return monthlyLimitRepository.findByUserIdOrderByReferenceMonthDesc(currentUserService.requireCurrentUserId());
    }
}