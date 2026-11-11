package com.notare.coderun;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssignmentDataFileRepository extends JpaRepository<AssignmentDataFile, UUID> {

    List<AssignmentDataFile> findByAssignmentIdOrderByNameAsc(UUID assignmentId);

    boolean existsByAssignmentIdAndName(UUID assignmentId, String name);

    long countByAssignmentId(UUID assignmentId);
}
