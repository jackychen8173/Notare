package com.notare.discussion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
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

    /** Every thread in the tutor's active courses, newest activity first (home page). */
    @Query("""
            SELECT t FROM DiscussionThread t
            WHERE t.course.tutor.id = :tutorId AND t.course.archivedAt IS NULL
            ORDER BY t.lastActivityAt DESC
            """)
    List<DiscussionThread> findInActiveCoursesOfTutor(@Param("tutorId") UUID tutorId);

    /** Same private-thread rule as findVisibleToStudent, across several courses (home page). */
    @Query("""
            SELECT t FROM DiscussionThread t
            WHERE t.course.id IN :courseIds
              AND (t.visibility = com.notare.discussion.DiscussionVisibility.PUBLIC OR t.author.id = :studentId)
            ORDER BY t.lastActivityAt DESC
            """)
    List<DiscussionThread> findVisibleToStudentInCourses(
            @Param("courseIds") Collection<UUID> courseIds, @Param("studentId") UUID studentId);
}
