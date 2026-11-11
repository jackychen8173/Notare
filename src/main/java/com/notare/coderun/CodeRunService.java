package com.notare.coderun;

import com.notare.assignment.Assignment;
import com.notare.assignment.AssignmentRepository;
import com.notare.coderun.dto.CodeRunResponse;
import com.notare.coderun.dto.RunCodeRequest;
import com.notare.coderun.dto.SandboxRunRequest;
import com.notare.course.EnrollmentRepository;
import com.notare.user.User;
import com.notare.user.UserRepository;
import com.notare.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class CodeRunService {

    private final SandboxClient sandboxClient;
    private final AssignmentRepository assignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final DataFileService dataFileService;

    public CodeRunService(
            SandboxClient sandboxClient,
            AssignmentRepository assignmentRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            DataFileService dataFileService
    ) {
        this.sandboxClient = sandboxClient;
        this.assignmentRepository = assignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.dataFileService = dataFileService;
    }

    // Same requireStudent + enrollment check as SubmissionService.submitAssignment - duplicated
    // rather than shared, matching this codebase's existing style (e.g. SageToolExecutor
    // duplicates ownership checks that also live on the owning service).
    public CodeRunResponse runCode(UUID assignmentId, RunCodeRequest request, String studentEmail) {
        User student = requireStudent(studentEmail);

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        boolean enrolled = enrollmentRepository.existsByStudentIdAndCourseId(
                student.getId(), assignment.getCourse().getId());
        if (!enrolled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not enrolled in this assignment's course");
        }
        if (!assignment.isVisibleToStudents(java.time.Instant.now())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found");
        }

        // The assignment's data files come from the database, never from the request.
        return sandboxClient.run(SandboxRunRequest.single(request.code(), stdinOf(request),
                dataFileService.filesForRun(assignment.getId())));
    }

    /** Workspace practice code: any student may run it, since it isn't tied to a course. */
    public CodeRunResponse runPractice(RunCodeRequest request, String studentEmail) {
        requireStudent(studentEmail);
        return sandboxClient.run(SandboxRunRequest.single(request.code(), stdinOf(request), List.of()));
    }

    private static String stdinOf(RunCodeRequest request) {
        return request.stdin() == null ? "" : request.stdin();
    }

    private User requireStudent(String email) {
        return userRepository.findByEmail(email)
                .filter(user -> user.getRole() == UserRole.STUDENT)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
