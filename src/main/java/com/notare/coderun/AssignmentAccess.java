package com.notare.coderun;

import com.notare.assignment.Assignment;
import com.notare.assignment.AssignmentRepository;
import com.notare.course.EnrollmentRepository;
import com.notare.submission.Submission;
import com.notare.submission.SubmissionRepository;
import com.notare.user.User;
import com.notare.user.UserRepository;
import com.notare.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

/**
 * Who may see or change an assignment's Run setup (data files, tests). The tutor who owns the
 * course manages it; enrolled students see it once the assignment is open, by the same rule as
 * the assignment itself (Assignment.isVisibleToStudents). Every miss is a 404, so nobody learns
 * that someone else's assignment exists.
 */
@Component
public class AssignmentAccess {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    public AssignmentAccess(
            AssignmentRepository assignmentRepository,
            SubmissionRepository submissionRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository
    ) {
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
    }

    public Assignment requireOwned(UUID assignmentId, String tutorEmail) {
        User tutor = requireUser(tutorEmail);
        return assignmentRepository.findById(assignmentId)
                .filter(assignment -> assignment.getCourse().getTutor().getId().equals(tutor.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
    }

    /** Owned, and the course isn't archived (archived courses are read-only). */
    public Assignment requireEditable(UUID assignmentId, String tutorEmail) {
        Assignment assignment = requireOwned(assignmentId, tutorEmail);
        if (assignment.getCourse().getArchivedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot edit assignments in an archived course");
        }
        return assignment;
    }

    public Assignment requireVisibleToStudent(UUID assignmentId, String studentEmail) {
        User student = requireUser(studentEmail);
        if (student.getRole() != UserRole.STUDENT) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }
        return assignmentRepository.findById(assignmentId)
                .filter(assignment -> enrollmentRepository.existsByStudentIdAndCourseId(
                        student.getId(), assignment.getCourse().getId()))
                .filter(assignment -> assignment.isVisibleToStudents(Instant.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
    }

    public Submission requireOwnedSubmission(UUID submissionId, String tutorEmail) {
        User tutor = requireUser(tutorEmail);
        return submissionRepository.findById(submissionId)
                .filter(submission -> submission.getAssignment().getCourse().getTutor().getId().equals(tutor.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
