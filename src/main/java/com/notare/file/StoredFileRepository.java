package com.notare.file;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {

    List<StoredFile> findByMaterialIdIn(Collection<UUID> materialIds);

    List<StoredFile> findByMaterialId(UUID materialId);

    List<StoredFile> findBySubmissionIdOrderByCreatedAtAsc(UUID submissionId);

    /** A student's drafts for their next submission to an assignment. */
    List<StoredFile> findByAssignmentIdAndUploaderIdAndSubmissionIsNullOrderByCreatedAtAsc(UUID assignmentId, UUID uploaderId);

    @Query("SELECT COALESCE(SUM(f.sizeBytes), 0) FROM StoredFile f WHERE f.uploader.id = :uploaderId")
    long totalBytesUploadedBy(@Param("uploaderId") UUID uploaderId);
}
