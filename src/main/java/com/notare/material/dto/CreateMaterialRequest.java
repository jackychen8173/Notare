package com.notare.material.dto;

import com.notare.material.MaterialType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateMaterialRequest(
        @NotBlank String title,
        String description,
        String url,
        UUID topicId,
        @NotNull MaterialType type
) {
}
