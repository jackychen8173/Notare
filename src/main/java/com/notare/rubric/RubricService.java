package com.notare.rubric;

import com.notare.assignment.Assignment;
import com.notare.assignment.AssignmentRepository;
import com.notare.course.EnrollmentRepository;
import com.notare.rubric.dto.CreateRubricRequest;
import com.notare.rubric.dto.RubricCriterionResponse;
import com.notare.rubric.dto.RubricResponse;
import com.notare.user.User;
import com.notare.user.UserRepository;
import com.notare.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@Transactional
public class RubricService {

    private final RubricRepository rubricRepository;
    private final RubricCriterionRepository criterionRepository;
    private final AssignmentRepository assignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    public RubricService(
            RubricRepository rubricRepository,
            RubricCriterionRepository criterionRepository,
            AssignmentRepository assignmentRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository
    ) {
        this.rubricRepository = rubricRepository;
        this.criterionRepository = criterionRepository;
        this.assignmentRepository = assignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
    }

    public RubricResponse createRubric(UUID assignmentId, CreateRubricRequest request, String tutorEmail) {
        Assignment assignment = requireOwnedAssignment(assignmentId, tutorEmail);

        if (rubricRepository.findByAssignmentId(assignment.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This assignment already has a rubric - delete it before creating a new one");
        }

        Rubric rubric = Rubric.builder()
                .assignment(assignment)
                .title(request.title())
                .build();
        rubricRepository.save(rubric);

        int position = 0;
        for (CreateRubricRequest.CriterionInput input : request.criteria()) {
            RubricCriterion criterion = RubricCriterion.builder()
                    .rubric(rubric)
                    .name(input.name())
                    .description(input.description())
                    .pointsPossible(input.pointsPossible())
                    .position(position++)
                    .build();
            criterionRepository.save(criterion);
        }

        return loadRubricResponse(rubric);
    }

    @Transactional(readOnly = true)
    public RubricResponse getRubric(UUID assignmentId, String tutorEmail) {
        Assignment assignment = requireOwnedAssignment(assignmentId, tutorEmail);
        Rubric rubric = requireRubric(assignment.getId());
        return loadRubricResponse(rubric);
    }

    @Transactional(readOnly = true)
    public RubricResponse getRubricForStudent(UUID assignmentId, String studentEmail) {
        Assignment assignment = requireEnrolledAssignment(assignmentId, studentEmail);
        Rubric rubric = requireRubric(assignment.getId());
        return loadRubricResponse(rubric);
    }

    public void deleteRubric(UUID assignmentId, String tutorEmail) {
        Assignment assignment = requireOwnedAssignment(assignmentId, tutorEmail);
        Rubric rubric = requireRubric(assignment.getId());
        rubricRepository.delete(rubric);
    }

    private RubricResponse loadRubricResponse(Rubric rubric) {
        var criteria = criterionRepository.findByRubricIdOrderByPositionAsc(rubric.getId()).stream()
                .map(RubricCriterionResponse::from)
                .toList();
        return RubricResponse.from(rubric, criteria);
    }

    private Rubric requireRubric(UUID assignmentId) {
        return rubricRepository.findByAssignmentId(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "This assignment has no rubric"));
    }

    private User requireTutor(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private User requireStudentUser(String email) {
        return userRepository.findByEmail(email)
                .filter(user -> user.getRole() == UserRole.STUDENT)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private Assignment requireOwnedAssignment(UUID assignmentId, String tutorEmail) {
        User tutor = requireTutor(tutorEmail);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        if (!assignment.getCourse().getTutor().getId().equals(tutor.getId())) {
            // 404, not 403 - avoid confirming another tutor's assignment exists
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found");
        }

        return assignment;
    }

    private Assignment requireEnrolledAssignment(UUID assignmentId, String studentEmail) {
        User student = requireStudentUser(studentEmail);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        if (!enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), assignment.getCourse().getId())) {
            // 404, not 403 - avoid confirming the assignment exists if the student isn't enrolled
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found");
        }

        return assignment;
    }
}
