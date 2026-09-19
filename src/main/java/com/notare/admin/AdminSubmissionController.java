package com.notare.admin;

import com.notare.common.ApiResponse;
import com.notare.submission.dto.SubmissionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class AdminSubmissionController {

    private final AdminSubmissionService adminSubmissionService;

    public AdminSubmissionController(AdminSubmissionService adminSubmissionService) {
        this.adminSubmissionService = adminSubmissionService;
    }

    @GetMapping("/api/admin/submissions/{id}")
    public ResponseEntity<ApiResponse<SubmissionResponse>> getSubmission(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminSubmissionService.getSubmission(id)));
    }

    @GetMapping("/api/admin/assignments/{id}/submissions")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> listSubmissionsForAssignment(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminSubmissionService.listSubmissionsForAssignment(id)));
    }
}
