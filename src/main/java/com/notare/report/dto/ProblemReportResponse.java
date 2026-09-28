package com.notare.report.dto;

import com.notare.report.ProblemReport;
import com.notare.report.ProblemReportCategory;
import com.notare.report.ProblemReportStatus;
import com.notare.user.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProblemReportResponse(
        UUID id,
        UUID reporterId,
        String reporterName,
        String reporterEmail,
        UserRole reporterRole,
        boolean reporterDemo,
        ProblemReportCategory category,
        String message,
        String pageUrl,
        String userAgent,
        ProblemReportStatus status,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt
) {
    public static ProblemReportResponse from(ProblemReport report) {
        return new ProblemReportResponse(
                report.getId(),
                report.getReporter().getId(),
                report.getReporter().getName(),
                report.getReporter().getEmail(),
                report.getReporter().getRole(),
                report.getReporter().isDemo(),
                report.getCategory(),
                report.getMessage(),
                report.getPageUrl(),
                report.getUserAgent(),
                report.getStatus(),
                report.getCreatedAt(),
                report.getResolvedAt()
        );
    }
}
