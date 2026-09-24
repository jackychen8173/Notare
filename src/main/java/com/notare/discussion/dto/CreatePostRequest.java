package com.notare.discussion.dto;

import jakarta.validation.constraints.NotBlank;

/** anonymous is only honored for a student replying in a PUBLIC thread. */
public record CreatePostRequest(
        @NotBlank String body,
        Boolean anonymous
) {
}
