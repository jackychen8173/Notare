package com.notare.sage.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SageMessageRepository extends JpaRepository<SageMessage, UUID> {

    List<SageMessage> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);
}
