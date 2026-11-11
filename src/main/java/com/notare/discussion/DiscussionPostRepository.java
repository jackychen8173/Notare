package com.notare.discussion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DiscussionPostRepository extends JpaRepository<DiscussionPost, UUID> {

    List<DiscussionPost> findByThreadIdOrderByCreatedAtAsc(UUID threadId);

    List<DiscussionPost> findByThreadIdAndAuthorId(UUID threadId, UUID authorId);

    long countByThreadId(UUID threadId);

    @Query("SELECT p.thread.id, COUNT(p) FROM DiscussionPost p WHERE p.thread.id IN :threadIds GROUP BY p.thread.id")
    List<Object[]> countByThreadIds(@Param("threadIds") Collection<UUID> threadIds);

    /** Replies by other people in threads this person started, in active courses (notifications). */
    @Query("""
            SELECT p FROM DiscussionPost p
            WHERE p.thread.author.id = :userId AND p.author.id <> :userId
              AND p.thread.course.archivedAt IS NULL AND p.createdAt > :since
            """)
    List<DiscussionPost> findRepliesToThreadsStartedBy(@Param("userId") UUID userId, @Param("since") java.time.LocalDateTime since);

    /** Replies by anyone but the tutor in the tutor's active courses (notifications). */
    @Query("""
            SELECT p FROM DiscussionPost p
            WHERE p.thread.course.tutor.id = :tutorId AND p.author.id <> :tutorId
              AND p.thread.course.archivedAt IS NULL AND p.createdAt > :since
            """)
    List<DiscussionPost> findStudentPostsForTutor(@Param("tutorId") UUID tutorId, @Param("since") java.time.LocalDateTime since);
}
