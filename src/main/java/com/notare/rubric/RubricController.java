package com.notare.rubric;

import com.notare.common.ApiResponse;
import com.notare.rubric.dto.CreateRubricRequest;
import com.notare.rubric.dto.RubricResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@PreAuthorize("hasRole('TUTOR')")
public class RubricController {

    private final RubricService rubricService;

    public RubricController(RubricService rubricService) {
        this.rubricService = rubricService;
    }

    @PostMapping("/api/assignments/{id}/rubric")
    public ResponseEntity<ApiResponse<RubricResponse>> createRubric(
            @PathVariable UUID id,
            @Valid @RequestBody CreateRubricRequest request,
            Authentication authentication
    ) {
        RubricResponse response = rubricService.createRubric(id, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/api/assignments/{id}/rubric")
    public ResponseEntity<ApiResponse<RubricResponse>> getRubric(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(rubricService.getRubric(id, authentication.getName())));
    }

    @DeleteMapping("/api/assignments/{id}/rubric")
    public ResponseEntity<ApiResponse<Void>> deleteRubric(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        rubricService.deleteRubric(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Rubric deleted", null));
    }
}
