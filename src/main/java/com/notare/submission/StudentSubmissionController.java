package com.notare.submission;

import com.notare.common.ApiResponse;
import com.notare.submission.dto.SubmissionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@PreAuthorize("hasRole('STUDENT')")
public class StudentSubmissionController {

    private final SubmissionService submissionService;

    public StudentSubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @GetMapping("/api/student/assignments/{id}/submission")
    public ResponseEntity<ApiResponse<SubmissionResponse>> getMySubmission(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(submissionService.getMySubmission(id, authentication.getName())));
    }
}
