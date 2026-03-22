package org.tech_solutions.application.imports.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tech_solutions.application.imports.model.ImportFile;

import java.math.BigInteger;
import java.util.List;

@Repository
public interface ImportFileRepository extends JpaRepository<ImportFile, BigInteger> {
    List<ImportFile> findByUserId(BigInteger userId);
}

