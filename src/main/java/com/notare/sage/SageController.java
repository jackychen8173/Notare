package com.notare.sage;

import com.notare.common.ApiResponse;
import com.notare.sage.dto.DraftNotesRequest;
import com.notare.sage.dto.PendingReviewsResponse;
import com.notare.sage.dto.ProgressSummaryResponse;
import com.notare.sage.dto.ReviewSubmissionRequest;
import com.notare.session.dto.SessionNoteResponse;
import com.notare.submission.dto.SubmissionResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/sage")
@PreAuthorize("hasRole('TUTOR')")
public class SageController {

    private final SageService sageService;

    public SageController(SageService sageService) {
        this.sageService = sageService;
    }

    @PostMapping("/draft-notes")
    public ResponseEntity<ApiResponse<SessionNoteResponse>> draftNotes(
            @Valid @RequestBody DraftNotesRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                sageService.draftSessionNotes(request.sessionId(), authentication.getName())));
    }

    @PostMapping("/review-submission")
    public ResponseEntity<ApiResponse<SubmissionResponse>> reviewSubmission(
            @Valid @RequestBody ReviewSubmissionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                sageService.reviewSubmission(request.submissionId(), authentication.getName())));
    }

    @GetMapping("/student-progress/{id}")
    public ResponseEntity<ApiResponse<ProgressSummaryResponse>> studentProgress(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                sageService.generateProgressSummary(id, authentication.getName())));
    }

    @GetMapping("/pending-reviews")
    public ResponseEntity<ApiResponse<PendingReviewsResponse>> pendingReviews(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(sageService.pendingReviewsCount(authentication.getName())));
    }
}
