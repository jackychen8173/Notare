package com.notare.course;

import com.notare.common.ApiResponse;
import com.notare.course.dto.CourseResponse;
import com.notare.course.dto.CreateCourseRequest;
import com.notare.course.dto.EnrollStudentRequest;
import com.notare.student.dto.StudentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
@PreAuthorize("hasRole('TUTOR')")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(
            @Valid @RequestBody CreateCourseRequest request,
            Authentication authentication
    ) {
        CourseResponse response = courseService.createCourse(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseResponse>>> listCourses(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(courseService.listCourses(authentication.getName())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> getCourse(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(courseService.getCourse(id, authentication.getName())));
    }

    @PostMapping("/{id}/enroll")
    public ResponseEntity<ApiResponse<Void>> enrollStudent(
            @PathVariable UUID id,
            @Valid @RequestBody EnrollStudentRequest request,
            Authentication authentication
    ) {
        courseService.enrollStudent(id, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Student enrolled", null));
    }

    @GetMapping("/{id}/students")
    public ResponseEntity<ApiResponse<List<StudentResponse>>> listEnrolled(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(courseService.listEnrolledStudents(id, authentication.getName())));
    }
}
