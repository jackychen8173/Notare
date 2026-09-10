package com.notare.submission;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubmissionCriterionScoreRepository extends JpaRepository<SubmissionCriterionScore, UUID> {

    List<SubmissionCriterionScore> findBySubmissionId(UUID submissionId);

    void deleteBySubmissionId(UUID submissionId);
}
