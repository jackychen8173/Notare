package com.notare.material;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MaterialRepository extends JpaRepository<Material, UUID> {

    List<Material> findByCourseIdOrderByCreatedAtDesc(UUID courseId);
}
