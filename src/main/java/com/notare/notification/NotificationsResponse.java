package com.notare.notification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The signed-in person's notifications, newest first. availableKinds is what their role gets (for
 * the settings list); mutedKinds is the part of it they turned off.
 */
public record NotificationsResponse(
        List<Notification> items,
        int unreadCount,
        List<NotificationKind> availableKinds,
        List<NotificationKind> mutedKinds
) {

    /** id is stable across reads, for list keys. href is a frontend path. */
    public record Notification(
            String id,
            NotificationKind kind,
            String title,
            String detail,
            UUID courseId,
            String courseName,
            String href,
            Instant at,
            boolean unread
    ) {
        Notification withUnread(boolean value) {
            return new Notification(id, kind, title, detail, courseId, courseName, href, at, value);
        }
    }
}
