package org.tech_solutions.application.aiinsights.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tech_solutions.application.aiinsights.controller.dto.AiInsightHistoricalData;
import org.tech_solutions.application.aiinsights.dto.AiInsightDataDTO;
import org.tech_solutions.application.aiinsights.dto.AiInsightRequestDTO;
import org.tech_solutions.application.aiinsights.dto.GenerateAiInsightRequestDTO;
import org.tech_solutions.application.aiinsights.generator.AiInsightGenerator;
import org.tech_solutions.application.aiinsights.mapper.AiInsightMapper;
import org.tech_solutions.application.aiinsights.model.AiInsight;
import org.tech_solutions.application.aiinsights.service.AiInsightService;
import org.tech_solutions.application.dashboard.dto.BalancePerMonthDto;
import org.tech_solutions.application.security.CurrentUserService;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("api/v1/ai-insights")
public class AiInsightController {

    private final AiInsightService aiInsightService;
    private final AiInsightGenerator aiInsightGenerator;
    private final CurrentUserService currentUserService;

    public AiInsightController(
            AiInsightService aiInsightService,
            AiInsightGenerator aiInsightGenerator,
            CurrentUserService currentUserService) {
        this.aiInsightService = aiInsightService;
        this.aiInsightGenerator = aiInsightGenerator;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<AiInsightDataDTO> create(@Valid @RequestBody AiInsightRequestDTO request) {
        AiInsight created = aiInsightService.create(AiInsightMapper.toModel(request), request.userId());
        return ResponseEntity.status(201).body(AiInsightMapper.toDTO(created));
    }

    @GetMapping
    public ResponseEntity<List<AiInsightDataDTO>> listAll() {
        List<AiInsight> insights = aiInsightService.listAll();
        return insights.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(AiInsightMapper.toDTO(insights));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AiInsightDataDTO>> listByUser(@PathVariable Long userId) {
        List<AiInsight> insights = aiInsightService.listByUser(userId);
        return insights.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(AiInsightMapper.toDTO(insights));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AiInsightDataDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(AiInsightMapper.toDTO(aiInsightService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AiInsightDataDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody AiInsightRequestDTO request) {
        AiInsight updated = aiInsightService.update(id, AiInsightMapper.toModel(request), request.userId());
        return ResponseEntity.ok(AiInsightMapper.toDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        aiInsightService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/generate/{userId}")
    public ResponseEntity<AiInsightDataDTO> generate(@PathVariable Long userId) {
        AiInsight generated = aiInsightGenerator.generateAndSave(userId);
        return ResponseEntity.status(201).body(AiInsightMapper.toDTO(generated));
    }

    @PostMapping("/generate")
    public ResponseEntity<AiInsightDataDTO> generateWithSpecification(
            @Valid @RequestBody GenerateAiInsightRequestDTO request) {
        Long userId = currentUserService.requireCurrentUserId();
        AiInsight generated = aiInsightGenerator.generateAndSaveWithSpecification(
                userId,
                request.insightType(),
                request.specification());
        return ResponseEntity.status(201).body(AiInsightMapper.toDTO(generated));
    }

    @GetMapping("/historical-data")
    public ResponseEntity<List<AiInsightHistoricalData>> generateInsightHistoricalData() {
        List<AiInsightHistoricalData> insights = aiInsightService.generateInsightHistoricalData();

        if (insights.isEmpty()) {
            return ResponseEntity.status(204).body(Collections.emptyList());
        }
        return ResponseEntity.ok(insights);
    }
}
