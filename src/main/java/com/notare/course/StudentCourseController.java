package com.notare.course;

import com.notare.common.ApiResponse;
import com.notare.course.dto.CourseResponse;
import com.notare.course.dto.JoinCourseRequest;
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
@RequestMapping("/api/student/courses")
@PreAuthorize("hasRole('STUDENT')")
public class StudentCourseController {

    private final CourseService courseService;

    public StudentCourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseResponse>>> listEnrolledCourses(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(courseService.listEnrolledCourses(authentication.getName())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> getEnrolledCourse(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(courseService.getEnrolledCourse(id, authentication.getName())));
    }

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<Void>> joinCourse(
            @Valid @RequestBody JoinCourseRequest request,
            Authentication authentication
    ) {
        courseService.joinCourseByCode(request.code(), authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Joined course", null));
    }
}
