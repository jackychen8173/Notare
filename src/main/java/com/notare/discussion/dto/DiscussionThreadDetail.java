package com.notare.discussion.dto;

import com.notare.discussion.DiscussionThread;
import com.notare.discussion.DiscussionVisibility;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DiscussionThreadDetail(
        UUID id,
        UUID courseId,
        String title,
        String body,
        DiscussionVisibility visibility,
        boolean pinned,
        boolean locked,
        DiscussionAuthor author,
        LocalDateTime createdAt,
        LocalDateTime editedAt,
        LocalDateTime lastActivityAt,
        List<DiscussionPostResponse> posts
) {
    public static DiscussionThreadDetail from(DiscussionThread thread, DiscussionAuthor author, List<DiscussionPostResponse> posts) {
        return new DiscussionThreadDetail(
                thread.getId(),
                thread.getCourse().getId(),
                thread.getTitle(),
                thread.getBody(),
                thread.getVisibility(),
                thread.isPinned(),
                thread.isLocked(),
                author,
                thread.getCreatedAt(),
                thread.getEditedAt(),
                thread.getLastActivityAt(),
                posts
        );
    }
}
