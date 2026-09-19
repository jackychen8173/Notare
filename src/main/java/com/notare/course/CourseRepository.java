package com.notare.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    List<Course> findByTutorIdAndArchivedAtIsNull(UUID tutorId);

    List<Course> findByTutorIdAndArchivedAtIsNotNull(UUID tutorId);

    Optional<Course> findByJoinCode(String joinCode);

    boolean existsByJoinCode(String joinCode);
}
