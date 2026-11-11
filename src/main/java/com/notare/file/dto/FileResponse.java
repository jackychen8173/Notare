package com.notare.file.dto;

import com.notare.file.StoredFile;

import java.time.Instant;
import java.util.UUID;

/** An uploaded file's details. The bytes come from GET /api/files/{id}/link. */
public record FileResponse(
        UUID id,
        String fileName,
        String contentType,
        long sizeBytes,
        Instant createdAt
) {
    public static FileResponse from(StoredFile file) {
        return new FileResponse(file.getId(), file.getFileName(), file.getContentType(), file.getSizeBytes(), file.getCreatedAt());
    }
}
