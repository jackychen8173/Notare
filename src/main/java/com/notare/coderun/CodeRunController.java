package com.notare.coderun;

import com.notare.coderun.dto.CodeRunResponse;
import com.notare.coderun.dto.RunCodeRequest;
import com.notare.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@PreAuthorize("hasRole('STUDENT')")
public class CodeRunController {

    private final CodeRunService codeRunService;

    public CodeRunController(CodeRunService codeRunService) {
        this.codeRunService = codeRunService;
    }

    @PostMapping("/api/assignments/{id}/run")
    public ResponseEntity<ApiResponse<CodeRunResponse>> runCode(
            @PathVariable UUID id,
            @Valid @RequestBody RunCodeRequest request,
            Authentication authentication
    ) {
        CodeRunResponse response = codeRunService.runCode(id, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
