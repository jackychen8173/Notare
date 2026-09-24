package com.notare.discussion.dto;

import com.notare.user.User;
import com.notare.user.UserRole;

import java.util.UUID;

/**
 * Who wrote a thread or post, as the current viewer is allowed to see it. Anonymity is enforced here,
 * at the DTO boundary, rather than trusting every caller/page to remember to hide the name - the same
 * idea as {@code SubmissionResponse.forStudent()}.
 */
public record DiscussionAuthor(
        UUID id,
        String name,
        UserRole role,
        boolean anonymous,
        boolean mine
) {
    private static final String ANONYMOUS_NAME = "Anonymous";

    /** The tutor always sees the real author, plus whether they posted anonymously to classmates. */
    public static DiscussionAuthor forTutor(User author, boolean anonymous, UUID tutorId) {
        return new DiscussionAuthor(author.getId(), author.getName(), author.getRole(), anonymous,
                author.getId().equals(tutorId));
    }

    /** A student sees an anonymous author's identity only if it's their own post. */
    public static DiscussionAuthor forStudent(User author, boolean anonymous, UUID studentId) {
        boolean mine = author.getId().equals(studentId);
        if (anonymous && !mine) {
            return new DiscussionAuthor(null, ANONYMOUS_NAME, UserRole.STUDENT, true, false);
        }
        return new DiscussionAuthor(author.getId(), author.getName(), author.getRole(), anonymous, mine);
    }
}
