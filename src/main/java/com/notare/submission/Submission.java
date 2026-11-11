package com.notare.submission;

import com.notare.assignment.Assignment;
import com.notare.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "submissions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 1 for the first submission; each resubmission after released feedback is the next number. */
    @Column(name = "attempt_number", nullable = false)
    @Builder.Default
    private int attemptNumber = 1;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sage_feedback", columnDefinition = "jsonb")
    private String sageFeedback;

    @Column(name = "tutor_feedback", columnDefinition = "TEXT")
    private String tutorFeedback;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_status", nullable = false)
    private FeedbackStatus feedbackStatus;

    private String grade;

    /** Points before any late penalty. Null until the tutor gives a score in points. */
    @Column(name = "points_earned", precision = 7, scale = 2)
    private BigDecimal pointsEarned;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    /** submittedAt is written in the JVM's zone (see onCreate); this is the moment it means. */
    public Instant submittedInstant() {
        return submittedAt.atZone(ZoneId.systemDefault()).toInstant();
    }

    @PrePersist
    protected void onCreate() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }
}
