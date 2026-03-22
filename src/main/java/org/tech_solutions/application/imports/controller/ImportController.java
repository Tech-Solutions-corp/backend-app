package org.tech_solutions.application.imports.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tech_solutions.application.imports.dto.ImportDataDTO;
import org.tech_solutions.application.imports.dto.ImportRequestDTO;
import org.tech_solutions.application.imports.mapper.ImportMapper;
import org.tech_solutions.application.imports.model.ImportFile;
import org.tech_solutions.application.imports.service.ImportFileService;

import java.math.BigInteger;
import java.util.List;

@RestController
@RequestMapping("api/v1/imports")
public class ImportController {

    private final ImportFileService importFileService;

    public ImportController(ImportFileService importFileService) {
        this.importFileService = importFileService;
    }

    @PostMapping
    public ResponseEntity<ImportDataDTO> create(@Valid @RequestBody ImportRequestDTO request) {
        ImportFile created = importFileService.create(ImportMapper.toModel(request), request.userId());
        return ResponseEntity.status(201).body(ImportMapper.toDTO(created));
    }

    @GetMapping
    public ResponseEntity<List<ImportDataDTO>> listAll() {
        List<ImportFile> imports = importFileService.listAll();
        return imports.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(ImportMapper.toDTO(imports));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ImportDataDTO>> listByUser(@PathVariable BigInteger userId) {
        List<ImportFile> imports = importFileService.listByUser(userId);
        return imports.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(ImportMapper.toDTO(imports));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImportDataDTO> findById(@PathVariable BigInteger id) {
        return ResponseEntity.ok(ImportMapper.toDTO(importFileService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ImportDataDTO> update(
            @PathVariable BigInteger id,
            @Valid @RequestBody ImportRequestDTO request
    ) {
        ImportFile updated = importFileService.update(id, ImportMapper.toModel(request), request.userId());
        return ResponseEntity.ok(ImportMapper.toDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable BigInteger id) {
        importFileService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

