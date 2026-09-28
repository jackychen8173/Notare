package com.notare.report;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ProblemReportRepository extends JpaRepository<ProblemReport, UUID> {

    List<ProblemReport> findAllByOrderByCreatedAtDesc();

    List<ProblemReport> findByStatusOrderByCreatedAtDesc(ProblemReportStatus status);

    long countByStatus(ProblemReportStatus status);

    long countByReporterIdAndCreatedAtAfter(UUID reporterId, LocalDateTime after);
}
