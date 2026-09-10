package com.notare.material.dto;

import com.notare.material.Material;

import java.time.LocalDateTime;
import java.util.UUID;

public record MaterialResponse(
        UUID id,
        UUID courseId,
        UUID topicId,
        String topicName,
        String title,
        String description,
        String url,
        LocalDateTime createdAt
) {
    public static MaterialResponse from(Material material) {
        return new MaterialResponse(
                material.getId(),
                material.getCourse().getId(),
                material.getTopic() != null ? material.getTopic().getId() : null,
                material.getTopic() != null ? material.getTopic().getName() : null,
                material.getTitle(),
                material.getDescription(),
                material.getUrl(),
                material.getCreatedAt()
        );
    }
}
