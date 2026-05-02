package org.tech_solutions.application.monthlylimits.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tech_solutions.application.monthlylimits.dto.MonthlyLimitDataDTO;
import org.tech_solutions.application.monthlylimits.dto.MonthlyLimitRequestDTO;
import org.tech_solutions.application.monthlylimits.mapper.MonthlyLimitMapper;
import org.tech_solutions.application.monthlylimits.model.MonthlyLimit;
import org.tech_solutions.application.monthlylimits.service.MonthlyLimitService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/monthly-limits")
public class MonthlyLimitController {

    private final MonthlyLimitService monthlyLimitService;

    public MonthlyLimitController(MonthlyLimitService monthlyLimitService) {
        this.monthlyLimitService = monthlyLimitService;
    }

    @PostMapping
    public ResponseEntity<MonthlyLimitDataDTO> create(@Valid @RequestBody MonthlyLimitRequestDTO request) {
        MonthlyLimit created = monthlyLimitService.save(MonthlyLimitMapper.toModel(request), request.userId());
        return ResponseEntity.status(201).body(MonthlyLimitMapper.toDTO(created));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<MonthlyLimitDataDTO>> listByUser(@PathVariable Long userId) {
        List<MonthlyLimit> limits = monthlyLimitService.listByUser(userId);
        return limits.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(MonthlyLimitMapper.toDTO(limits));
    }
}