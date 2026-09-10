package com.notare.material.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateMaterialRequest(
        @NotBlank String title,
        String description,
        String url,
        UUID topicId
) {
}
