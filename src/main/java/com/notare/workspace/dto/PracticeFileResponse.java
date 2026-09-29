package com.notare.workspace.dto;

import com.notare.workspace.PracticeFile;

import java.time.LocalDateTime;
import java.util.UUID;

public record PracticeFileResponse(
        UUID id,
        String name,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PracticeFileResponse from(PracticeFile file) {
        return new PracticeFileResponse(
                file.getId(), file.getName(), file.getContent(), file.getCreatedAt(), file.getUpdatedAt());
    }
}
