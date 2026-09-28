package com.notare.admin;

import com.notare.common.ApiResponse;
import com.notare.report.ProblemReportService;
import com.notare.report.ProblemReportStatus;
import com.notare.report.dto.ProblemReportResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProblemReportController {

    private final ProblemReportService problemReportService;

    public AdminProblemReportController(ProblemReportService problemReportService) {
        this.problemReportService = problemReportService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProblemReportResponse>>> listReports(
            @RequestParam(required = false) ProblemReportStatus status
    ) {
        return ResponseEntity.ok(ApiResponse.success(problemReportService.listReports(status)));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<ProblemReportResponse>> resolve(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(problemReportService.resolve(id)));
    }

    @PostMapping("/{id}/reopen")
    public ResponseEntity<ApiResponse<ProblemReportResponse>> reopen(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(problemReportService.reopen(id)));
    }
}
