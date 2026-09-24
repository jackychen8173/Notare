package com.notare.discussion.dto;

/** Either field may be omitted to leave that setting unchanged. */
public record ModerateThreadRequest(
        Boolean pinned,
        Boolean locked
) {
}
