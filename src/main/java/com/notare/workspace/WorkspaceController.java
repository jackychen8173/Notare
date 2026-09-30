package com.notare.workspace;

import com.notare.coderun.CodeRunService;
import com.notare.coderun.dto.CodeRunResponse;
import com.notare.coderun.dto.RunCodeRequest;
import com.notare.common.ApiResponse;
import com.notare.workspace.dto.PracticeFileResponse;
import com.notare.workspace.dto.SavePracticeFileRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@PreAuthorize("hasRole('STUDENT')")
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final CodeRunService codeRunService;

    public WorkspaceController(WorkspaceService workspaceService, CodeRunService codeRunService) {
        this.workspaceService = workspaceService;
        this.codeRunService = codeRunService;
    }

    @GetMapping("/api/student/workspace/files")
    public ResponseEntity<ApiResponse<List<PracticeFileResponse>>> listFiles(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(workspaceService.listFiles(authentication.getName())));
    }

    @PostMapping("/api/student/workspace/files")
    public ResponseEntity<ApiResponse<PracticeFileResponse>> createFile(
            @Valid @RequestBody SavePracticeFileRequest request,
            Authentication authentication
    ) {
        PracticeFileResponse response = workspaceService.createFile(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/api/student/workspace/files/{id}")
    public ResponseEntity<ApiResponse<PracticeFileResponse>> updateFile(
            @PathVariable UUID id,
            @Valid @RequestBody SavePracticeFileRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(workspaceService.updateFile(id, request, authentication.getName())));
    }

    @DeleteMapping("/api/student/workspace/files/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable UUID id, Authentication authentication) {
        workspaceService.deleteFile(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("File deleted", null));
    }

    /** Runs practice code. Blocked for demo accounts by DemoRestrictionInterceptor, like assignment Run. */
    @PostMapping("/api/student/workspace/run")
    public ResponseEntity<ApiResponse<CodeRunResponse>> run(
            @Valid @RequestBody RunCodeRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(codeRunService.runPractice(request, authentication.getName())));
    }
}
