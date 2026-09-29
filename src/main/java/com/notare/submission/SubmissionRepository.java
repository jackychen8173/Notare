package com.notare.submission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    List<Submission> findByStudentIdAndAssignment_Course_Tutor_Id(UUID studentId, UUID tutorId);

    List<Submission> findByAssignmentId(UUID assignmentId);

    long countByFeedbackStatusAndAssignment_Course_Tutor_Id(FeedbackStatus feedbackStatus, UUID tutorId);

    Optional<Submission> findFirstByStudentIdAndAssignmentIdOrderBySubmittedAtDesc(
            UUID studentId, UUID assignmentId);

    List<Submission> findByStudentIdAndAssignmentIdOrderByAttemptNumberDesc(UUID studentId, UUID assignmentId);

    List<Submission> findByAssignment_Course_Tutor_Id(UUID tutorId);

    long countByReleasedAtIsNullAndStudent_DemoFalse();

    long countByReleasedAtIsNotNullAndStudent_DemoFalse();

    /** The tutor's review queue: everything not yet released, in active courses, oldest first. */
    @Query("""
            SELECT s FROM Submission s
            WHERE s.assignment.course.tutor.id = :tutorId
              AND s.assignment.course.archivedAt IS NULL
              AND s.releasedAt IS NULL
            ORDER BY s.submittedAt ASC
            """)
    List<Submission> findUnreleasedForTutor(@Param("tutorId") UUID tutorId);

    List<Submission> findByStudentId(UUID studentId);
}
