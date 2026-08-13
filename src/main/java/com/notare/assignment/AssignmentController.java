package com.notare.assignment;

import com.notare.assignment.dto.AssignmentResponse;
import com.notare.assignment.dto.CreateAssignmentRequest;
import com.notare.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@PreAuthorize("hasRole('TUTOR')")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping("/api/courses/{courseId}/assignments")
    public ResponseEntity<ApiResponse<AssignmentResponse>> createAssignment(
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateAssignmentRequest request,
            Authentication authentication
    ) {
        AssignmentResponse response = assignmentService.createAssignment(courseId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/api/courses/{courseId}/assignments")
    public ResponseEntity<ApiResponse<List<AssignmentResponse>>> listAssignments(
            @PathVariable UUID courseId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(assignmentService.listAssignments(courseId, authentication.getName())));
    }

    @GetMapping("/api/assignments/{id}")
    public ResponseEntity<ApiResponse<AssignmentResponse>> getAssignment(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(assignmentService.getAssignment(id, authentication.getName())));
    }
}
