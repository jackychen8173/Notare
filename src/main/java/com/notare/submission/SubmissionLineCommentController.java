package com.notare.submission;

import com.notare.common.ApiResponse;
import com.notare.submission.dto.CreateLineCommentRequest;
import com.notare.submission.dto.LineCommentResponse;
import com.notare.submission.dto.UpdateLineCommentRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@PreAuthorize("hasRole('TUTOR')")
public class SubmissionLineCommentController {

    private final SubmissionService submissionService;

    public SubmissionLineCommentController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @GetMapping("/api/submissions/{id}/line-comments")
    public ResponseEntity<ApiResponse<List<LineCommentResponse>>> listLineComments(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(submissionService.listLineComments(id, authentication.getName())));
    }

    @PostMapping("/api/submissions/{id}/line-comments")
    public ResponseEntity<ApiResponse<LineCommentResponse>> addLineComment(
            @PathVariable UUID id,
            @Valid @RequestBody CreateLineCommentRequest request,
            Authentication authentication
    ) {
        LineCommentResponse response = submissionService.addLineComment(id, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PatchMapping("/api/line-comments/{commentId}")
    public ResponseEntity<ApiResponse<LineCommentResponse>> updateLineComment(
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdateLineCommentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                submissionService.updateLineComment(commentId, request, authentication.getName())));
    }

    @DeleteMapping("/api/line-comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteLineComment(
            @PathVariable UUID commentId,
            Authentication authentication
    ) {
        submissionService.deleteLineComment(commentId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Comment deleted", null));
    }
}
