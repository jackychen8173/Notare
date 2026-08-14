package com.notare.assignment;

import com.notare.assignment.dto.AssignmentResponse;
import com.notare.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@PreAuthorize("hasRole('STUDENT')")
public class StudentAssignmentController {

    private final AssignmentService assignmentService;

    public StudentAssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping("/api/student/courses/{id}/assignments")
    public ResponseEntity<ApiResponse<List<AssignmentResponse>>> listAssignments(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(assignmentService.listAssignmentsForEnrolledCourse(id, authentication.getName())));
    }

    @GetMapping("/api/student/assignments/{id}")
    public ResponseEntity<ApiResponse<AssignmentResponse>> getAssignment(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(assignmentService.getAssignmentForStudent(id, authentication.getName())));
    }
}
