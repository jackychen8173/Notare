package com.notare.quizattempt;

import com.notare.quiz.QuizQuestion;
import com.notare.quiz.QuizQuestionOption;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "quiz_answers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    // MULTIPLE_CHOICE/TRUE_FALSE only.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_option_id")
    private QuizQuestionOption selectedOption;

    // SHORT_ANSWER/ESSAY only.
    @Column(name = "text_response", columnDefinition = "TEXT")
    private String textResponse;

    // Set automatically for MC/TF at finalize time; stays null for free-text questions.
    @Column(name = "is_correct")
    private Boolean correct;

    // Auto-set for MC/TF at finalize time; tutor-set for SHORT_ANSWER/ESSAY, null until graded.
    @Column(name = "points_awarded")
    private BigDecimal pointsAwarded;

    @Column(name = "tutor_feedback", columnDefinition = "TEXT")
    private String tutorFeedback;

    // Sage's drafted suggestion for a free-text answer - mirrors Submission.sageFeedback exactly,
    // never shown to the student until the owning attempt is released.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sage_suggestion", columnDefinition = "jsonb")
    private String sageSuggestion;
}
