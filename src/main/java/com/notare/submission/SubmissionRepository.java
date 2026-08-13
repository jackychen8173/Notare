package com.notare.submission;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    List<Submission> findByStudentIdAndAssignment_Course_Tutor_Id(UUID studentId, UUID tutorId);

    List<Submission> findByAssignmentId(UUID assignmentId);

    long countByFeedbackStatusAndAssignment_Course_Tutor_Id(FeedbackStatus feedbackStatus, UUID tutorId);
}
