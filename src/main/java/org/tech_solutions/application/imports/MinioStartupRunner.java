package org.tech_solutions.application.imports;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.tech_solutions.application.imports.service.ImportFileService;

@Component
public class MinioStartupRunner implements ApplicationRunner {
    private final ImportFileService minioPresignedService;

    public MinioStartupRunner(ImportFileService minioPresignedService) {
        this.minioPresignedService = minioPresignedService;
    }

    @Override
    public void run(ApplicationArguments args) {
        minioPresignedService.inicializarBucket();
    }
}
