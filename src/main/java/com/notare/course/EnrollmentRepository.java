package com.notare.course;

import com.notare.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, EnrollmentId> {

    List<Enrollment> findByCourseId(UUID courseId);

    List<Enrollment> findByStudentId(UUID studentId);

    boolean existsByStudentIdAndCourseId(UUID studentId, UUID courseId);

    void deleteByStudentIdAndCourseId(UUID studentId, UUID courseId);

    boolean existsByStudentIdAndCourse_Tutor_Id(UUID studentId, UUID tutorId);

    @Query("SELECT DISTINCT e.student FROM Enrollment e WHERE e.course.tutor.id = :tutorId")
    List<User> findDistinctStudentsByCourseTutorId(UUID tutorId);
}
