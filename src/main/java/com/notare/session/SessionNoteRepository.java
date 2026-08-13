package com.notare.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SessionNoteRepository extends JpaRepository<SessionNote, UUID> {

    Optional<SessionNote> findBySessionId(UUID sessionId);
}
