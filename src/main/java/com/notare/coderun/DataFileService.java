package com.notare.coderun;

import com.notare.assignment.Assignment;
import com.notare.coderun.dto.DataFileRequest;
import com.notare.coderun.dto.DataFileResponse;
import com.notare.coderun.dto.SandboxRunRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    private final AssignmentAccess access;

    public DataFileService(AssignmentDataFileRepository dataFileRepository, AssignmentAccess access) {
        this.dataFileRepository = dataFileRepository;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<DataFileResponse> listForTutor(UUID assignmentId, String tutorEmail) {
        Assignment assignment = access.requireOwned(assignmentId, tutorEmail);
        return list(assignment);
    }

    @Transactional(readOnly = true)
    public List<DataFileResponse> listForStudent(UUID assignmentId, String studentEmail) {
        Assignment assignment = access.requireVisibleToStudent(assignmentId, studentEmail);
        return list(assignment);
    }

    public DataFileResponse create(UUID assignmentId, DataFileRequest request, String tutorEmail) {
        Assignment assignment = access.requireEditable(assignmentId, tutorEmail);
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
        Assignment assignment = access.requireEditable(assignmentId, tutorEmail);
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
        Assignment assignment = access.requireEditable(assignmentId, tutorEmail);
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
}
