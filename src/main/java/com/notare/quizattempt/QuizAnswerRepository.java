package com.notare.quizattempt;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, UUID> {

    List<QuizAnswer> findByAttemptId(UUID attemptId);

    Optional<QuizAnswer> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId);
}
