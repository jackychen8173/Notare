package com.notare.report.dto;

import com.notare.report.ProblemReportCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProblemReportRequest(
        @NotNull ProblemReportCategory category,
        @NotBlank @Size(max = 5000) String message,
        // Filled in by the frontend: the page the reporter was on.
        @Size(max = 2048) String pageUrl
) {
}
