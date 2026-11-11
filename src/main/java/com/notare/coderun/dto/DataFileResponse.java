package com.notare.coderun.dto;

import com.notare.coderun.AssignmentDataFile;

import java.time.LocalDateTime;
import java.util.UUID;

public record DataFileResponse(UUID id, String name, String content, LocalDateTime updatedAt) {

    public static DataFileResponse from(AssignmentDataFile file) {
        return new DataFileResponse(file.getId(), file.getName(), file.getContent(), file.getUpdatedAt());
    }
}
