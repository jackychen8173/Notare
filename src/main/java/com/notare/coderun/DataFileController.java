package com.notare.coderun;

import com.notare.coderun.dto.DataFileRequest;
import com.notare.coderun.dto.DataFileResponse;
import com.notare.common.ApiResponse;
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
public class DataFileController {

    private final DataFileService dataFileService;

    public DataFileController(DataFileService dataFileService) {
        this.dataFileService = dataFileService;
    }

    @GetMapping("/api/assignments/{assignmentId}/data-files")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<ApiResponse<List<DataFileResponse>>> list(
            @PathVariable UUID assignmentId, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                dataFileService.listForTutor(assignmentId, authentication.getName())));
    }

    @PostMapping("/api/assignments/{assignmentId}/data-files")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<ApiResponse<DataFileResponse>> create(
            @PathVariable UUID assignmentId,
            @Valid @RequestBody DataFileRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                dataFileService.create(assignmentId, request, authentication.getName())));
    }

    @PutMapping("/api/assignments/{assignmentId}/data-files/{fileId}")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<ApiResponse<DataFileResponse>> update(
            @PathVariable UUID assignmentId,
            @PathVariable UUID fileId,
            @Valid @RequestBody DataFileRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                dataFileService.update(assignmentId, fileId, request, authentication.getName())));
    }

    @DeleteMapping("/api/assignments/{assignmentId}/data-files/{fileId}")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID assignmentId, @PathVariable UUID fileId, Authentication authentication) {
        dataFileService.delete(assignmentId, fileId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/api/student/assignments/{assignmentId}/data-files")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<DataFileResponse>>> listForStudent(
            @PathVariable UUID assignmentId, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                dataFileService.listForStudent(assignmentId, authentication.getName())));
    }
}
