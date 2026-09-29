package com.notare.submission.dto;

import jakarta.validation.constraints.NotBlank;

/** Edits a comment's text. Saving a Sage suggestion this way also publishes it. */
public record UpdateLineCommentRequest(
        @NotBlank String body
) {
}
