package org.tech_solutions.application.imports.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.tech_solutions.application.imports.model.ImportFile;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ImportFileRepository extends JpaRepository<ImportFile, Long> {
    List<ImportFile> findByUserId(Long userId);

    @Query("""
    SELECT imp FROM ImportFile imp
    JOIN imp.user u
    WHERE u.id = :userId
      AND imp.status = 'COMPLETED'
      AND imp.importedAt BETWEEN :start AND :end
""")
    List<ImportFile> findCompletedByUserAndPeriod(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}


