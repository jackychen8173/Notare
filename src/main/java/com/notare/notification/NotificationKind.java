package com.notare.notification;

import com.notare.user.UserRole;

import java.util.List;

/** What a notification is about. Each person can mute any of the kinds their role gets. */
public enum NotificationKind {
    /** Student: an assignment opened or a quiz was published. */
    NEW_WORK,
    /** Student: feedback or a quiz score was returned. */
    FEEDBACK,
    /** Student: the tutor posted an announcement. */
    ANNOUNCEMENT,
    /** Student: work due in the next 24 hours that they haven't handed in. */
    DUE_SOON,
    /** Student: replies in a thread they started. Tutor: student posts in their courses. */
    DISCUSSION,
    /** Tutor: a student handed in an assignment or finished a quiz. */
    SUBMISSION,
    /** Tutor: a student joined a course. */
    JOINED;

    public static List<NotificationKind> forRole(UserRole role) {
        return switch (role) {
            case TUTOR -> List.of(SUBMISSION, DISCUSSION, JOINED);
            case STUDENT -> List.of(NEW_WORK, FEEDBACK, ANNOUNCEMENT, DUE_SOON, DISCUSSION);
            default -> List.of();
        };
    }
}
