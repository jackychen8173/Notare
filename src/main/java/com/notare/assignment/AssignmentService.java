package com.notare.assignment;

import com.notare.assignment.dto.AssignmentResponse;
import com.notare.assignment.dto.CreateAssignmentRequest;
import com.notare.course.Course;
import com.notare.course.CourseRepository;
import com.notare.user.User;
import com.notare.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public AssignmentService(
            AssignmentRepository assignmentRepository,
            CourseRepository courseRepository,
            UserRepository userRepository
    ) {
        this.assignmentRepository = assignmentRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    public AssignmentResponse createAssignment(UUID courseId, CreateAssignmentRequest request, String tutorEmail) {
        Course course = requireOwnedCourse(courseId, tutorEmail);

        Assignment assignment = Assignment.builder()
                .course(course)
                .title(request.title())
                .description(request.description())
                .dueDate(request.dueDate())
                .build();

        assignmentRepository.save(assignment);

        return AssignmentResponse.from(assignment);
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> listAssignments(UUID courseId, String tutorEmail) {
        Course course = requireOwnedCourse(courseId, tutorEmail);
        return assignmentRepository.findByCourseId(course.getId()).stream()
                .map(AssignmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AssignmentResponse getAssignment(UUID assignmentId, String tutorEmail) {
        return AssignmentResponse.from(requireOwnedAssignment(assignmentId, tutorEmail));
    }

    private User requireTutor(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private Course requireOwnedCourse(UUID courseId, String tutorEmail) {
        User tutor = requireTutor(tutorEmail);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (!course.getTutor().getId().equals(tutor.getId())) {
            // 404, not 403 - avoid confirming another tutor's course exists
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }

        return course;
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
}
