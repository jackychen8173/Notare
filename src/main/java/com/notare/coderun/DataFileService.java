package com.notare.coderun;

import com.notare.assignment.Assignment;
import com.notare.assignment.AssignmentRepository;
import com.notare.coderun.dto.DataFileRequest;
import com.notare.coderun.dto.DataFileResponse;
import com.notare.coderun.dto.SandboxRunRequest;
import com.notare.course.EnrollmentRepository;
import com.notare.user.User;
import com.notare.user.UserRepository;
import com.notare.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Data files on an assignment. The tutor who owns the course manages them; enrolled students can
 * read them once the assignment is open (Assignment.isVisibleToStudents), the same rule as the
 * assignment itself, and every Run of that assignment gets them.
 */
@Service
@Transactional
public class DataFileService {

    static final int MAX_FILES_PER_ASSIGNMENT = 5;

    private final AssignmentDataFileRepository dataFileRepository;
    private final AssignmentRepository assignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    public DataFileService(
            AssignmentDataFileRepository dataFileRepository,
            AssignmentRepository assignmentRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository
    ) {
        this.dataFileRepository = dataFileRepository;
        this.assignmentRepository = assignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<DataFileResponse> listForTutor(UUID assignmentId, String tutorEmail) {
        Assignment assignment = requireOwnedAssignment(assignmentId, tutorEmail);
        return list(assignment);
    }

    @Transactional(readOnly = true)
    public List<DataFileResponse> listForStudent(UUID assignmentId, String studentEmail) {
        Assignment assignment = requireVisibleAssignment(assignmentId, studentEmail);
        return list(assignment);
    }

    public DataFileResponse create(UUID assignmentId, DataFileRequest request, String tutorEmail) {
        Assignment assignment = requireEditableAssignment(assignmentId, tutorEmail);
        String name = validName(request.name());
        if (dataFileRepository.countByAssignmentId(assignment.getId()) >= MAX_FILES_PER_ASSIGNMENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An assignment can have up to " + MAX_FILES_PER_ASSIGNMENT + " data files");
        }
        if (dataFileRepository.existsByAssignmentIdAndName(assignment.getId(), name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This assignment already has a file named " + name);
        }
        AssignmentDataFile file = dataFileRepository.save(AssignmentDataFile.builder()
                .assignment(assignment)
                .name(name)
                .content(request.content())
                .build());
        return DataFileResponse.from(file);
    }

    public DataFileResponse update(UUID assignmentId, UUID fileId, DataFileRequest request, String tutorEmail) {
        Assignment assignment = requireEditableAssignment(assignmentId, tutorEmail);
        AssignmentDataFile file = requireFile(assignment, fileId);
        String name = validName(request.name());
        if (!name.equals(file.getName()) && dataFileRepository.existsByAssignmentIdAndName(assignment.getId(), name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This assignment already has a file named " + name);
        }
        file.setName(name);
        file.setContent(request.content());
        return DataFileResponse.from(dataFileRepository.save(file));
    }

    public void delete(UUID assignmentId, UUID fileId, String tutorEmail) {
        Assignment assignment = requireEditableAssignment(assignmentId, tutorEmail);
        dataFileRepository.delete(requireFile(assignment, fileId));
    }

    /** The files a Run of this assignment gets. Callers have already checked who's running it. */
    @Transactional(readOnly = true)
    public List<SandboxRunRequest.File> filesForRun(UUID assignmentId) {
        return dataFileRepository.findByAssignmentIdOrderByNameAsc(assignmentId).stream()
                .map(file -> new SandboxRunRequest.File(file.getName(), file.getContent()))
                .toList();
    }

    private List<DataFileResponse> list(Assignment assignment) {
        return dataFileRepository.findByAssignmentIdOrderByNameAsc(assignment.getId()).stream()
                .map(DataFileResponse::from)
                .toList();
    }

    /** Source and class files would clash with the student's program, so they're not data. */
    private static String validName(String name) {
        String trimmed = name.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".java") || lower.endsWith(".class")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Data files can't be .java or .class files");
        }
        return trimmed;
    }

    private AssignmentDataFile requireFile(Assignment assignment, UUID fileId) {
        return dataFileRepository.findById(fileId)
                .filter(file -> file.getAssignment().getId().equals(assignment.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Data file not found"));
    }

    private Assignment requireEditableAssignment(UUID assignmentId, String tutorEmail) {
        Assignment assignment = requireOwnedAssignment(assignmentId, tutorEmail);
        if (assignment.getCourse().getArchivedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot edit assignments in an archived course");
        }
        return assignment;
    }

    private Assignment requireOwnedAssignment(UUID assignmentId, String tutorEmail) {
        User tutor = userRepository.findByEmail(tutorEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        return assignmentRepository.findById(assignmentId)
                // 404, not 403 - avoid confirming another tutor's assignment exists
                .filter(assignment -> assignment.getCourse().getTutor().getId().equals(tutor.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
    }

    private Assignment requireVisibleAssignment(UUID assignmentId, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .filter(user -> user.getRole() == UserRole.STUDENT)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        return assignmentRepository.findById(assignmentId)
                // 404 before it opens or when not enrolled, like the assignment itself
                .filter(assignment -> enrollmentRepository.existsByStudentIdAndCourseId(
                        student.getId(), assignment.getCourse().getId()))
                .filter(assignment -> assignment.isVisibleToStudents(Instant.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
    }
}
