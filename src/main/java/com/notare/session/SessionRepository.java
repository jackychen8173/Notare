package com.notare.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    List<Session> findByTutorId(UUID tutorId);

    List<Session> findByTutorIdAndStudentId(UUID tutorId, UUID studentId);

    List<Session> findByStudentId(UUID studentId);
}
