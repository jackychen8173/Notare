package com.notare.home.dto;

import com.notare.discussion.DiscussionThread;
import com.notare.discussion.DiscussionVisibility;

import java.time.LocalDateTime;
import java.util.UUID;

/** An unread discussion thread. No author: the title alone never reveals an anonymous poster. */
public record HomeThread(
        UUID threadId,
        String title,
        HomeCourse course,
        DiscussionVisibility visibility,
        LocalDateTime lastActivityAt
) {
    public static HomeThread from(DiscussionThread thread) {
        return new HomeThread(thread.getId(), thread.getTitle(), HomeCourse.from(thread.getCourse()),
                thread.getVisibility(), thread.getLastActivityAt());
    }
}
