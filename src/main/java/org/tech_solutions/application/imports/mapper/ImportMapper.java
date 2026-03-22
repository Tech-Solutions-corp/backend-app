package org.tech_solutions.application.imports.mapper;

import org.tech_solutions.application.imports.dto.ImportDataDTO;
import org.tech_solutions.application.imports.dto.ImportRequestDTO;
import org.tech_solutions.application.imports.model.ImportFile;

import java.util.List;

public class ImportMapper {

    private ImportMapper() {
    }

    public static ImportFile toModel(ImportRequestDTO dto) {
        ImportFile importFile = new ImportFile();
        importFile.setFileName(dto.fileName());
        importFile.setStatus(dto.status());
        return importFile;
    }

    public static ImportDataDTO toDTO(ImportFile importFile) {
        return new ImportDataDTO(
                importFile.getId(),
                importFile.getUser().getId(),
                importFile.getFileName(),
                importFile.getImportedAt(),
                importFile.getStatus()
        );
    }

    public static List<ImportDataDTO> toDTO(List<ImportFile> imports) {
        return imports.stream().map(ImportMapper::toDTO).toList();
    }
}

