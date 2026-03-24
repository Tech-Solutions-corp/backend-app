package org.tech_solutions.application.imports.service;

import org.springframework.stereotype.Service;
import org.tech_solutions.application.imports.enums.ImportStatus;
import org.tech_solutions.application.imports.model.ImportFile;
import org.tech_solutions.application.imports.repository.ImportFileRepository;
import org.tech_solutions.application.shared.exception.EntityNotFoundException;
import org.tech_solutions.application.user.model.User;
import org.tech_solutions.application.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ImportFileService {

    private final ImportFileRepository importFileRepository;
    private final UserRepository userRepository;

    public ImportFileService(ImportFileRepository importFileRepository, UserRepository userRepository) {
        this.importFileRepository = importFileRepository;
        this.userRepository = userRepository;
    }

    public ImportFile create(ImportFile importFile, Long userId) {
        importFile.setUser(findUser(userId));
        importFile.setImportedAt(LocalDateTime.now());
        if (importFile.getStatus() == null) {
            importFile.setStatus(ImportStatus.PROCESSING);
        }
        return importFileRepository.save(importFile);
    }

    public List<ImportFile> listAll() {
        return importFileRepository.findAll();
    }

    public List<ImportFile> listByUser(Long userId) {
        findUser(userId);
        return importFileRepository.findByUserId(userId);
    }

    public ImportFile findById(Long id) {
        return importFileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Importacao nao encontrada"));
    }

    public ImportFile update(Long id, ImportFile updated, Long userId) {
        ImportFile current = findById(id);
        current.setUser(findUser(userId));
        current.setFileName(updated.getFileName());
        current.setStatus(updated.getStatus() == null ? current.getStatus() : updated.getStatus());
        return importFileRepository.save(current);
    }

    public void delete(Long id) {
        importFileRepository.delete(findById(id));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado"));
    }
}


