package com.notare.quiz;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    List<Quiz> findByCourseId(UUID courseId);

    List<Quiz> findByCourseIdAndPublishedAtIsNotNull(UUID courseId);
}
