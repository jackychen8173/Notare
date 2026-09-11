package com.notare.gradecategory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GradeCategoryRepository extends JpaRepository<GradeCategory, UUID> {

    List<GradeCategory> findByCourseIdOrderByCreatedAtAsc(UUID courseId);
}
