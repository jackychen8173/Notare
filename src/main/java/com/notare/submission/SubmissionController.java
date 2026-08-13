package com.notare.submission;

import com.notare.common.ApiResponse;
import com.notare.submission.dto.ReleaseFeedbackRequest;
import com.notare.submission.dto.SubmissionResponse;
import com.notare.submission.dto.SubmitAssignmentRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping("/api/assignments/{id}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<SubmissionResponse>> submitAssignment(
            @PathVariable UUID id,
            @Valid @RequestBody SubmitAssignmentRequest request,
            Authentication authentication
    ) {
        SubmissionResponse response = submissionService.submitAssignment(id, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/api/submissions/{id}")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<ApiResponse<SubmissionResponse>> getSubmission(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(submissionService.getSubmission(id, authentication.getName())));
    }

    @GetMapping("/api/assignments/{id}/submissions")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> listSubmissionsForAssignment(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(submissionService.listSubmissionsForAssignment(id, authentication.getName())));
    }

    @PatchMapping("/api/submissions/{id}/release")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<ApiResponse<SubmissionResponse>> releaseFeedback(
            @PathVariable UUID id,
            @Valid @RequestBody ReleaseFeedbackRequest request,
            Authentication authentication
    ) {
        SubmissionResponse response = submissionService.releaseFeedback(id, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
