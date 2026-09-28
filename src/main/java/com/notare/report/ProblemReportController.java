package com.notare.report;

import com.notare.common.ApiResponse;
import com.notare.report.dto.CreateProblemReportRequest;
import com.notare.report.dto.ProblemReportResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** "Report a problem", open to any signed-in user (tutor, student, admin, demo). */
@RestController
@RequestMapping("/api/reports")
public class ProblemReportController {

    private final ProblemReportService problemReportService;

    public ProblemReportController(ProblemReportService problemReportService) {
        this.problemReportService = problemReportService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProblemReportResponse>> createReport(
            @Valid @RequestBody CreateProblemReportRequest request,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent,
            Authentication authentication
    ) {
        ProblemReportResponse response = problemReportService.createReport(request, userAgent, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}
