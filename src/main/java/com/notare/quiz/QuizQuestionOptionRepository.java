package com.notare.quiz;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizQuestionOptionRepository extends JpaRepository<QuizQuestionOption, UUID> {

    List<QuizQuestionOption> findByQuestionIdOrderByPositionAsc(UUID questionId);

    void deleteByQuestionId(UUID questionId);
}
