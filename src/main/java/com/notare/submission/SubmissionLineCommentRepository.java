package com.notare.submission;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubmissionLineCommentRepository extends JpaRepository<SubmissionLineComment, UUID> {

    List<SubmissionLineComment> findBySubmissionIdOrderByLineNumberAscCreatedAtAsc(UUID submissionId);

    List<SubmissionLineComment> findBySubmissionIdAndStatusOrderByLineNumberAscCreatedAtAsc(
            UUID submissionId, LineCommentStatus status);

    boolean existsBySubmissionIdAndStatus(UUID submissionId, LineCommentStatus status);

    boolean existsBySubmissionIdAndSourceAndStatus(UUID submissionId, LineCommentSource source, LineCommentStatus status);

    void deleteBySubmissionIdAndStatus(UUID submissionId, LineCommentStatus status);
}
