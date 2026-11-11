package com.notare.coderun;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssignmentTestRepository extends JpaRepository<AssignmentTest, UUID> {

    List<AssignmentTest> findByAssignmentIdOrderByPositionAsc(UUID assignmentId);

    long countByAssignmentId(UUID assignmentId);
}
