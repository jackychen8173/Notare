package com.notare.gradebook;

import com.notare.common.ApiResponse;
import com.notare.gradebook.dto.GradebookResponse;
import com.notare.gradebook.dto.StudentGradesResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class GradebookController {

    private final GradebookService gradebookService;

    public GradebookController(GradebookService gradebookService) {
        this.gradebookService = gradebookService;
    }

    @GetMapping("/api/courses/{id}/gradebook")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<ApiResponse<GradebookResponse>> gradebook(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(gradebookService.gradebook(id, authentication.getName())));
    }

    @GetMapping("/api/student/courses/{id}/grades")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<StudentGradesResponse>> myGrades(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(gradebookService.myGrades(id, authentication.getName())));
    }
}
