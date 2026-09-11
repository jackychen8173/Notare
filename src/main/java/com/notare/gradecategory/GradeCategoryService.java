package com.notare.gradecategory;

import com.notare.course.Course;
import com.notare.course.CourseRepository;
import com.notare.gradecategory.dto.CreateGradeCategoryRequest;
import com.notare.gradecategory.dto.GradeCategoryResponse;
import com.notare.gradecategory.dto.UpdateGradeCategoryRequest;
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
public class GradeCategoryService {

    private final GradeCategoryRepository gradeCategoryRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public GradeCategoryService(
            GradeCategoryRepository gradeCategoryRepository,
            CourseRepository courseRepository,
            UserRepository userRepository
    ) {
        this.gradeCategoryRepository = gradeCategoryRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    public GradeCategoryResponse createCategory(UUID courseId, CreateGradeCategoryRequest request, String tutorEmail) {
        Course course = requireOwnedCourse(courseId, tutorEmail);
        requireNotArchived(course);

        GradeCategory category = GradeCategory.builder()
                .course(course)
                .name(request.name())
                .weightPercent(request.weightPercent())
                .build();

        gradeCategoryRepository.save(category);

        return GradeCategoryResponse.from(category);
    }

    @Transactional(readOnly = true)
    public List<GradeCategoryResponse> listCategories(UUID courseId, String tutorEmail) {
        Course course = requireOwnedCourse(courseId, tutorEmail);
        return gradeCategoryRepository.findByCourseIdOrderByCreatedAtAsc(course.getId()).stream()
                .map(GradeCategoryResponse::from)
                .toList();
    }

    public GradeCategoryResponse updateCategory(UUID categoryId, UpdateGradeCategoryRequest request, String tutorEmail) {
        GradeCategory category = requireOwnedCategory(categoryId, tutorEmail);
        requireNotArchived(category.getCourse());

        category.setName(request.name());
        category.setWeightPercent(request.weightPercent());
        gradeCategoryRepository.save(category);

        return GradeCategoryResponse.from(category);
    }

    public void deleteCategory(UUID categoryId, String tutorEmail) {
        GradeCategory category = requireOwnedCategory(categoryId, tutorEmail);
        requireNotArchived(category.getCourse());

        gradeCategoryRepository.delete(category);
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

    private GradeCategory requireOwnedCategory(UUID categoryId, String tutorEmail) {
        User tutor = requireTutor(tutorEmail);
        GradeCategory category = gradeCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grade category not found"));

        if (!category.getCourse().getTutor().getId().equals(tutor.getId())) {
            // 404, not 403 - avoid confirming another tutor's category exists
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Grade category not found");
        }

        return category;
    }

    private void requireNotArchived(Course course) {
        if (course.getArchivedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot modify grade categories in an archived course");
        }
    }
}
