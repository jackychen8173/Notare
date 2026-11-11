package com.notare.file;

import com.notare.common.ApiResponse;
import com.notare.file.dto.FileResponse;
import com.notare.material.dto.MaterialResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@RestController
public class FileController {

    private final FileService fileService;
    private final FileStorage storage;

    public FileController(FileService fileService, FileStorage storage) {
        this.fileService = fileService;
        this.storage = storage;
    }

    /** Whether uploads are on here (no storage configured: off), so the UI can hide upload buttons. */
    @GetMapping("/api/files/enabled")
    public ResponseEntity<ApiResponse<java.util.Map<String, Boolean>>> enabled() {
        return ResponseEntity.ok(ApiResponse.success(java.util.Map.of("enabled", storage.enabled())));
    }

    /** A course material that's an uploaded file (multipart: file, title, description, topicId). */
    @PostMapping(value = "/api/courses/{courseId}/materials/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<ApiResponse<MaterialResponse>> uploadMaterial(
            @PathVariable UUID courseId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "topicId", required = false) UUID topicId,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                fileService.uploadMaterial(courseId, file, title, description, topicId, authentication.getName())));
    }

    /** A file for the student's next submission to this assignment. */
    @PostMapping(value = "/api/student/assignments/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<FileResponse>> uploadDraft(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                fileService.uploadDraft(id, file, authentication.getName())));
    }

    /** The student's files waiting to go in with their next submission. */
    @GetMapping("/api/student/assignments/{id}/files")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<FileResponse>>> drafts(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(fileService.drafts(id, authentication.getName())));
    }

    @GetMapping("/api/submissions/{id}/files")
    @PreAuthorize("hasAnyRole('TUTOR', 'STUDENT')")
    public ResponseEntity<ApiResponse<List<FileResponse>>> submissionFiles(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(fileService.submissionFiles(id, authentication.getName())));
    }

    /** A download link that works for a few minutes. */
    @GetMapping("/api/files/{id}/link")
    @PreAuthorize("hasAnyRole('TUTOR', 'STUDENT')")
    public ResponseEntity<ApiResponse<FileLinkResponse>> link(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(new FileLinkResponse(fileService.link(id, authentication.getName()))));
    }

    @DeleteMapping("/api/files/{id}")
    @PreAuthorize("hasAnyRole('TUTOR', 'STUDENT')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id, Authentication authentication) {
        fileService.delete(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("File removed", null));
    }

    /**
     * Downloads for local-disk storage only (development and tests): the signed, expiring link
     * stands in for an S3 presigned URL. Public in SecurityConfig, like a presigned URL; 404 when
     * files aren't on local disk.
     */
    @GetMapping("/api/files/local")
    public ResponseEntity<Resource> local(
            @RequestParam String key,
            @RequestParam String name,
            @RequestParam boolean inline,
            @RequestParam long expires,
            @RequestParam String signature
    ) {
        if (!(storage instanceof LocalFileStorage local)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
        Path file = local.verify(key, name, inline, expires, signature);
        String contentType = inline ? local.contentTypeOf(file) : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, FileNames.contentDisposition(name, inline))
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(contentType))
                .body(new FileSystemResource(file));
    }

    public record FileLinkResponse(String url) {
    }
}
