package com.notare.rubric;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RubricRepository extends JpaRepository<Rubric, UUID> {

    Optional<Rubric> findByAssignmentId(UUID assignmentId);
}
