package com.notare.topic.dto;

import jakarta.validation.constraints.NotBlank;

public record RenameTopicRequest(
        @NotBlank String name
) {
}
