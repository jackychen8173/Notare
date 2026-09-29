package com.notare.quizattempt;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

    Optional<QuizAttempt> findByQuizIdAndStudentId(UUID quizId, UUID studentId);

    List<QuizAttempt> findByQuizId(UUID quizId);

    List<QuizAttempt> findByStudentId(UUID studentId);

    List<QuizAttempt> findByQuiz_Course_Id(UUID courseId);

    /**
     * The tutor's review queue: finished attempts not yet released, in active courses. An attempt
     * still IN_PROGRESS past its deadline counts as finished (it's only finalized lazily, the next
     * time something touches it).
     */
    @Query("""
            SELECT a FROM QuizAttempt a
            WHERE a.quiz.course.tutor.id = :tutorId
              AND a.quiz.course.archivedAt IS NULL
              AND a.releasedAt IS NULL
              AND (a.status = com.notare.quizattempt.AttemptStatus.SUBMITTED
                   OR (a.deadlineAt IS NOT NULL AND a.deadlineAt < :now))
            ORDER BY a.startedAt ASC
            """)
    List<QuizAttempt> findUnreleasedFinishedForTutor(@Param("tutorId") UUID tutorId, @Param("now") LocalDateTime now);
}
