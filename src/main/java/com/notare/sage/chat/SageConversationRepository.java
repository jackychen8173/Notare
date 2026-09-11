package com.notare.sage.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SageConversationRepository extends JpaRepository<SageConversation, UUID> {

    List<SageConversation> findByTutorIdOrderByUpdatedAtDesc(UUID tutorId);
}
