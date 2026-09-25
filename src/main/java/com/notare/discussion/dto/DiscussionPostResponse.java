package com.notare.discussion.dto;

import com.notare.discussion.DiscussionPost;

import java.time.LocalDateTime;
import java.util.UUID;

public record DiscussionPostResponse(
        UUID id,
        UUID threadId,
        String body,
        DiscussionAuthor author,
        LocalDateTime createdAt,
        LocalDateTime editedAt
) {
    public static DiscussionPostResponse from(DiscussionPost post, DiscussionAuthor author) {
        return new DiscussionPostResponse(
                post.getId(),
                post.getThread().getId(),
                post.getBody(),
                author,
                post.getCreatedAt(),
                post.getEditedAt()
        );
    }
}
