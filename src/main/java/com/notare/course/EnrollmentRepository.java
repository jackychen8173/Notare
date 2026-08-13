package com.notare.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, EnrollmentId> {

    List<Enrollment> findByCourseId(UUID courseId);

    boolean existsByStudentIdAndCourseId(UUID studentId, UUID courseId);
}
