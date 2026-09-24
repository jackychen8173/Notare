package com.notare.discussion.dto;

import com.notare.discussion.DiscussionVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * visibility/anonymous are only honored for students - a tutor's thread is always PUBLIC and named.
 * visibility defaults to PUBLIC when omitted; anonymous is ignored (forced false) on a PRIVATE thread.
 */
public record CreateThreadRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank String body,
        DiscussionVisibility visibility,
        Boolean anonymous
) {
}
