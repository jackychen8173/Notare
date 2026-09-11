package com.notare.material;

import com.notare.course.Course;
import com.notare.course.CourseRepository;
import com.notare.course.EnrollmentRepository;
import com.notare.material.dto.CreateMaterialRequest;
import com.notare.material.dto.MaterialResponse;
import com.notare.topic.Topic;
import com.notare.topic.TopicRepository;
import com.notare.user.User;
import com.notare.user.UserRepository;
import com.notare.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final CourseRepository courseRepository;
    private final TopicRepository topicRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    public MaterialService(
            MaterialRepository materialRepository,
            CourseRepository courseRepository,
            TopicRepository topicRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository
    ) {
        this.materialRepository = materialRepository;
        this.courseRepository = courseRepository;
        this.topicRepository = topicRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
    }

    public MaterialResponse createMaterial(UUID courseId, CreateMaterialRequest request, String tutorEmail) {
        Course course = requireOwnedCourse(courseId, tutorEmail);

        if (course.getArchivedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot add materials to an archived course");
        }

        Topic topic = null;
        if (request.topicId() != null) {
            topic = topicRepository.findById(request.topicId())
                    .filter(candidate -> candidate.getCourse().getId().equals(course.getId()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
        }

        Material material = Material.builder()
                .course(course)
                .topic(topic)
                .title(request.title())
                .description(request.description())
                .url(request.url())
                .build();

        materialRepository.save(material);

        return MaterialResponse.from(material);
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> listMaterials(UUID courseId, String tutorEmail) {
        Course course = requireOwnedCourse(courseId, tutorEmail);
        return materialRepository.findByCourseIdOrderByCreatedAtDesc(course.getId()).stream()
                .map(MaterialResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> listMaterialsForEnrolledCourse(UUID courseId, String studentEmail) {
        Course course = requireEnrolledCourse(courseId, studentEmail);
        return materialRepository.findByCourseIdOrderByCreatedAtDesc(course.getId()).stream()
                .map(MaterialResponse::from)
                .toList();
    }

    public void deleteMaterial(UUID materialId, String tutorEmail) {
        Material material = requireOwnedMaterial(materialId, tutorEmail);
        materialRepository.delete(material);
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

    private Course requireEnrolledCourse(UUID courseId, String studentEmail) {
        User student = requireStudentUser(studentEmail);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (!enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), course.getId())) {
            // 404, not 403 - avoid confirming the course exists if the student isn't enrolled
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }

        return course;
    }

    private Material requireOwnedMaterial(UUID materialId, String tutorEmail) {
        User tutor = requireTutor(tutorEmail);
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material not found"));

        if (!material.getCourse().getTutor().getId().equals(tutor.getId())) {
            // 404, not 403 - avoid confirming another tutor's material exists
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Material not found");
        }

        return material;
    }
}
