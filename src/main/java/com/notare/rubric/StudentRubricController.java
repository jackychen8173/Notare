package com.notare.rubric;

import com.notare.common.ApiResponse;
import com.notare.rubric.dto.RubricResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@PreAuthorize("hasRole('STUDENT')")
public class StudentRubricController {

    private final RubricService rubricService;

    public StudentRubricController(RubricService rubricService) {
        this.rubricService = rubricService;
    }

    @GetMapping("/api/student/assignments/{id}/rubric")
    public ResponseEntity<ApiResponse<RubricResponse>> getRubric(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(rubricService.getRubricForStudent(id, authentication.getName())));
    }
}
