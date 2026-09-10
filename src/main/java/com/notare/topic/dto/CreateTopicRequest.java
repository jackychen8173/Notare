package com.notare.topic.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTopicRequest(
        @NotBlank String name
) {
}
