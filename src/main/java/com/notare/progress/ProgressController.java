package com.notare.progress;

import com.notare.common.ApiResponse;
import com.notare.progress.dto.CourseProgressResponse;
import com.notare.progress.dto.StudentProgressResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping("/api/courses/{id}/progress")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<ApiResponse<CourseProgressResponse>> courseProgress(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(progressService.courseProgress(id, authentication.getName())));
    }

    @GetMapping("/api/student/courses/{id}/progress")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<StudentProgressResponse>> studentProgress(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(progressService.studentProgress(id, authentication.getName())));
    }
}
