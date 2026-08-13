package com.notare.submission;

import com.notare.assignment.Assignment;
import com.notare.assignment.AssignmentRepository;
import com.notare.course.EnrollmentRepository;
import com.notare.submission.dto.ReleaseFeedbackRequest;
import com.notare.submission.dto.SubmissionResponse;
import com.notare.submission.dto.SubmitAssignmentRequest;
import com.notare.user.User;
import com.notare.user.UserRepository;
import com.notare.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    public SubmissionService(
            SubmissionRepository submissionRepository,
            AssignmentRepository assignmentRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository
    ) {
        this.submissionRepository = submissionRepository;
        this.assignmentRepository = assignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
    }

    public SubmissionResponse submitAssignment(UUID assignmentId, SubmitAssignmentRequest request, String studentEmail) {
        User student = requireStudent(studentEmail);

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        boolean enrolled = enrollmentRepository.existsByStudentIdAndCourseId(
                student.getId(), assignment.getCourse().getId());
        if (!enrolled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not enrolled in this assignment's course");
        }

        Submission submission = Submission.builder()
                .assignment(assignment)
                .student(student)
                .content(request.content())
                .feedbackStatus(FeedbackStatus.PENDING)
                .build();

        submissionRepository.save(submission);

        return SubmissionResponse.from(submission);
    }

    public SubmissionResponse releaseFeedback(UUID submissionId, ReleaseFeedbackRequest request, String tutorEmail) {
        Submission submission = requireOwnedSubmission(submissionId, tutorEmail);

        submission.setTutorFeedback(request.tutorFeedback());
        submission.setGrade(request.grade());
        submission.setFeedbackStatus(FeedbackStatus.APPROVED);
        submission.setReleasedAt(LocalDateTime.now());

        submissionRepository.save(submission);

        return SubmissionResponse.from(submission);
    }

    private User requireStudent(String email) {
        return userRepository.findByEmail(email)
                .filter(user -> user.getRole() == UserRole.STUDENT)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private User requireTutor(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private Submission requireOwnedSubmission(UUID submissionId, String tutorEmail) {
        User tutor = requireTutor(tutorEmail);
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));

        if (!submission.getAssignment().getCourse().getTutor().getId().equals(tutor.getId())) {
            // 404, not 403 - avoid confirming another tutor's submission exists
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found");
        }

        return submission;
    }
}
