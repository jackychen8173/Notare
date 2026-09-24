package com.notare.discussion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DiscussionThreadReadRepository extends JpaRepository<DiscussionThreadRead, UUID> {

    List<DiscussionThreadRead> findByUserIdAndThreadIdIn(UUID userId, Collection<UUID> threadIds);

    // A native upsert rather than find-then-save: the thread page polls, so two overlapping requests
    // for the same (thread, user) would otherwise race on the unique constraint and 500.
    @Modifying
    @Query(value = """
            INSERT INTO discussion_thread_reads (id, thread_id, user_id, last_read_at)
            VALUES (gen_random_uuid(), :threadId, :userId, :readAt)
            ON CONFLICT (thread_id, user_id)
            DO UPDATE SET last_read_at = GREATEST(discussion_thread_reads.last_read_at, EXCLUDED.last_read_at)
            """, nativeQuery = true)
    void markRead(@Param("threadId") UUID threadId, @Param("userId") UUID userId, @Param("readAt") LocalDateTime readAt);
}
