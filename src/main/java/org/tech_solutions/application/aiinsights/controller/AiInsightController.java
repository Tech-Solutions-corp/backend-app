package org.tech_solutions.application.aiinsights.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tech_solutions.application.aiinsights.dto.AiInsightDataDTO;
import org.tech_solutions.application.aiinsights.dto.AiInsightRequestDTO;
import org.tech_solutions.application.aiinsights.mapper.AiInsightMapper;
import org.tech_solutions.application.aiinsights.model.AiInsight;
import org.tech_solutions.application.aiinsights.service.AiInsightService;

import java.util.List;

@RestController
@RequestMapping("api/v1/ai-insights")
public class AiInsightController {

    private final AiInsightService aiInsightService;

    public AiInsightController(AiInsightService aiInsightService) {
        this.aiInsightService = aiInsightService;
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
            @Valid @RequestBody AiInsightRequestDTO request
    ) {
        AiInsight updated = aiInsightService.update(id, AiInsightMapper.toModel(request), request.userId());
        return ResponseEntity.ok(AiInsightMapper.toDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        aiInsightService.delete(id);
        return ResponseEntity.noContent().build();
    }
}


