package com.notare.file;

import com.notare.assignment.Assignment;
import com.notare.course.Course;
import com.notare.material.Material;
import com.notare.submission.Submission;
import com.notare.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * An uploaded file: either a course material (material set) or a student's file for an assignment
 * (assignment set; submission null while it's a draft for their next submission). See V30.
 */
@Entity
@Table(name = "stored_files")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    private Material material;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id")
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id")
    private Submission submission;

    @Column(name = "object_key", nullable = false, unique = true)
    private String objectKey;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
