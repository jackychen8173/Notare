package com.notare.discussion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DiscussionPostRepository extends JpaRepository<DiscussionPost, UUID> {

    List<DiscussionPost> findByThreadIdOrderByCreatedAtAsc(UUID threadId);

    List<DiscussionPost> findByThreadIdAndAuthorId(UUID threadId, UUID authorId);

    long countByThreadId(UUID threadId);

    @Query("SELECT p.thread.id, COUNT(p) FROM DiscussionPost p WHERE p.thread.id IN :threadIds GROUP BY p.thread.id")
    List<Object[]> countByThreadIds(@Param("threadIds") Collection<UUID> threadIds);
}
