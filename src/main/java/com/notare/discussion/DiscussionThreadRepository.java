package com.notare.discussion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiscussionThreadRepository extends JpaRepository<DiscussionThread, UUID> {

    List<DiscussionThread> findByCourseIdOrderByPinnedDescLastActivityAtDesc(UUID courseId);

    // The private-thread rule lives in the query itself, not in each caller: a student only ever
    // gets back PUBLIC threads plus their own PRIVATE ones.
    @Query("""
            SELECT t FROM DiscussionThread t
            WHERE t.course.id = :courseId
              AND (t.visibility = com.notare.discussion.DiscussionVisibility.PUBLIC OR t.author.id = :studentId)
            ORDER BY t.pinned DESC, t.lastActivityAt DESC
            """)
    List<DiscussionThread> findVisibleToStudent(@Param("courseId") UUID courseId, @Param("studentId") UUID studentId);

    @Query("""
            SELECT t FROM DiscussionThread t
            WHERE t.id = :threadId
              AND (t.visibility = com.notare.discussion.DiscussionVisibility.PUBLIC OR t.author.id = :studentId)
            """)
    Optional<DiscussionThread> findByIdVisibleToStudent(@Param("threadId") UUID threadId, @Param("studentId") UUID studentId);
}
