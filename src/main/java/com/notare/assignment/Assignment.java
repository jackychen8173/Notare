package com.notare.assignment;

import com.notare.course.Course;
import com.notare.gradecategory.GradeCategory;
import com.notare.topic.Topic;
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

@Entity
@Table(name = "assignments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_category_id")
    private GradeCategory gradeCategory;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** When it's due. Shown and edited as a wall-clock time in the course's time zone. */
    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    /** Students can't see the assignment before this. Null: visible as soon as it's created. */
    @Column(name = "opens_at")
    private Instant opensAt;

    /** No submissions after this. Null: late work is accepted any time; equal to dueAt: locks at the due time. */
    @Column(name = "closes_at")
    private Instant closesAt;

    /** Null: not scored in points (the free-text grade still works). */
    @Column(name = "points_possible")
    private Integer pointsPossible;

    /** Percent of the points taken off per day late, or part of a day (0-100). */
    @Column(name = "late_penalty_percent", nullable = false)
    @Builder.Default
    private int latePenaltyPercent = 0;

    @Column(name = "allow_resubmission", nullable = false)
    @Builder.Default
    private boolean allowResubmission = false;

    /** When it was posted; null for assignments posted before V29 (unknown). */
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /** The one rule for whether students can see this assignment yet. */
    public boolean isVisibleToStudents(Instant now) {
        return opensAt == null || !now.isBefore(opensAt);
    }

    public boolean isAcceptingSubmissions(Instant now) {
        return isVisibleToStudents(now) && (closesAt == null || now.isBefore(closesAt));
    }
}
