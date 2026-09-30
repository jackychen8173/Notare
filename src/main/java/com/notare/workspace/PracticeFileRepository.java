package com.notare.workspace;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PracticeFileRepository extends JpaRepository<PracticeFile, UUID> {

    List<PracticeFile> findByStudentIdOrderByUpdatedAtDesc(UUID studentId);

    long countByStudentId(UUID studentId);
}
