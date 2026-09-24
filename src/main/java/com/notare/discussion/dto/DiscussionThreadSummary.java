package com.notare.discussion.dto;

import com.notare.discussion.DiscussionThread;
import com.notare.discussion.DiscussionVisibility;

import java.time.LocalDateTime;
import java.util.UUID;

public record DiscussionThreadSummary(
        UUID id,
        UUID courseId,
        String title,
        DiscussionVisibility visibility,
        boolean pinned,
        boolean locked,
        DiscussionAuthor author,
        long replyCount,
        LocalDateTime createdAt,
        LocalDateTime lastActivityAt,
        boolean unread
) {
    public static DiscussionThreadSummary from(DiscussionThread thread, DiscussionAuthor author, long replyCount, boolean unread) {
        return new DiscussionThreadSummary(
                thread.getId(),
                thread.getCourse().getId(),
                thread.getTitle(),
                thread.getVisibility(),
                thread.isPinned(),
                thread.isLocked(),
                author,
                replyCount,
                thread.getCreatedAt(),
                thread.getLastActivityAt(),
                unread
        );
    }
}
