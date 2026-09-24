package com.notare.quizattempt;

import com.notare.course.EnrollmentRepository;
import com.notare.quiz.QuestionType;
import com.notare.quiz.Quiz;
import com.notare.quiz.QuizQuestion;
import com.notare.quiz.QuizQuestionOption;
import com.notare.quiz.QuizQuestionOptionRepository;
import com.notare.quiz.QuizQuestionRepository;
import com.notare.quiz.QuizRepository;
import com.notare.quizattempt.dto.GradeAnswerRequest;
import com.notare.quizattempt.dto.QuizAnswerResponse;
import com.notare.quizattempt.dto.QuizAttemptResponse;
import com.notare.quizattempt.dto.UpsertAnswerRequest;
import com.notare.user.User;
import com.notare.user.UserRepository;
import com.notare.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Every method that touches an existing attempt calls finalizeIfExpired first, so a hard deadline is
 * enforced server-side regardless of whether the student's own auto-submit timer ever fires - see
 * the plan's "Deadline enforcement, no scheduled job" decision. Nothing here is @Transactional
 * (readOnly = true): finalizeIfExpired writes when it fires, so every entry point needs a
 * read-write transaction.
 */
@Service
@Transactional
public class QuizAttemptService {

    private final QuizAttemptRepository attemptRepository;
    private final QuizAnswerRepository answerRepository;
    private final QuizRepository quizRepository;
    private final QuizQuestionRepository questionRepository;
    private final QuizQuestionOptionRepository optionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    public QuizAttemptService(
            QuizAttemptRepository attemptRepository,
            QuizAnswerRepository answerRepository,
            QuizRepository quizRepository,
            QuizQuestionRepository questionRepository,
            QuizQuestionOptionRepository optionRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository
    ) {
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
    }

    // ---- student ----

    public QuizAttemptResponse startAttempt(UUID quizId, String studentEmail) {
        User student = requireStudentUser(studentEmail);
        Quiz quiz = requireEnrolledPublishedQuiz(quizId, student);

        QuizAttempt attempt = attemptRepository.findByQuizIdAndStudentId(quizId, student.getId())
                .orElseGet(() -> {
                    LocalDateTime now = LocalDateTime.now();
                    QuizAttempt created = QuizAttempt.builder()
                            .quiz(quiz)
                            .student(student)
                            .status(AttemptStatus.IN_PROGRESS)
                            .startedAt(now)
                            .deadlineAt(quiz.getTimeLimitMinutes() != null
                                    ? now.plusMinutes(quiz.getTimeLimitMinutes()) : null)
                            .build();
                    attemptRepository.save(created);
                    return created;
                });

        finalizeIfExpired(attempt);
        return QuizAttemptResponse.forStudent(attempt, loadStudentAnswerResponses(attempt));
    }

    public QuizAttemptResponse getMyAttempt(UUID quizId, String studentEmail) {
        User student = requireStudentUser(studentEmail);
        QuizAttempt attempt = attemptRepository.findByQuizIdAndStudentId(quizId, student.getId()).orElse(null);
        if (attempt == null) {
            return null;
        }
        finalizeIfExpired(attempt);
        return QuizAttemptResponse.forStudent(attempt, loadStudentAnswerResponses(attempt));
    }

    public QuizAttemptResponse getAttemptForStudent(UUID attemptId, String studentEmail) {
        QuizAttempt attempt = requireOwnedAttemptForStudent(attemptId, studentEmail);
        finalizeIfExpired(attempt);
        return QuizAttemptResponse.forStudent(attempt, loadStudentAnswerResponses(attempt));
    }

    public QuizAttemptResponse upsertAnswer(
            UUID attemptId, UUID questionId, UpsertAnswerRequest request, String studentEmail
    ) {
        QuizAttempt attempt = requireOwnedAttemptForStudent(attemptId, studentEmail);
        finalizeIfExpired(attempt);

        if (attempt.getStatus() == AttemptStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This attempt has already been submitted");
        }

        QuizQuestion question = questionRepository.findById(questionId)
                .filter(candidate -> candidate.getQuiz().getId().equals(attempt.getQuiz().getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));

        QuizQuestionOption selectedOption = null;
        if (request.selectedOptionId() != null) {
            selectedOption = optionRepository.findById(request.selectedOptionId())
                    .filter(candidate -> candidate.getQuestion().getId().equals(question.getId()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Option not found on this question"));
        }

        QuizAnswer answer = answerRepository.findByAttemptIdAndQuestionId(attempt.getId(), question.getId())
                .orElseGet(() -> QuizAnswer.builder().attempt(attempt).question(question).build());
        answer.setSelectedOption(selectedOption);
        answer.setTextResponse(request.textResponse());
        answerRepository.save(answer);

        return QuizAttemptResponse.forStudent(attempt, loadStudentAnswerResponses(attempt));
    }

    public QuizAttemptResponse submitAttempt(UUID attemptId, String studentEmail) {
        QuizAttempt attempt = requireOwnedAttemptForStudent(attemptId, studentEmail);
        finalizeIfExpired(attempt);

        // Idempotent: handles the race between the client's auto-submit timer and a manual click.
        if (attempt.getStatus() != AttemptStatus.SUBMITTED) {
            finalizeAttempt(attempt, LocalDateTime.now(), false);
        }

        return QuizAttemptResponse.forStudent(attempt, loadStudentAnswerResponses(attempt));
    }

    // ---- tutor ----

    public List<QuizAttemptResponse> listAttemptsForQuiz(UUID quizId, String tutorEmail) {
        Quiz quiz = requireOwnedQuiz(quizId, tutorEmail);
        List<QuizAttempt> attempts = attemptRepository.findByQuizId(quiz.getId());
        attempts.forEach(this::finalizeIfExpired);
        return attempts.stream()
                .map(attempt -> QuizAttemptResponse.from(attempt, loadAnswerResponses(attempt)))
                .toList();
    }

    public QuizAttemptResponse getAttempt(UUID attemptId, String tutorEmail) {
        QuizAttempt attempt = requireOwnedAttemptForTutor(attemptId, tutorEmail);
        finalizeIfExpired(attempt);
        return QuizAttemptResponse.from(attempt, loadAnswerResponses(attempt));
    }

    public QuizAttemptResponse gradeAnswer(
            UUID attemptId, UUID questionId, GradeAnswerRequest request, String tutorEmail
    ) {
        QuizAttempt attempt = requireOwnedAttemptForTutor(attemptId, tutorEmail);
        finalizeIfExpired(attempt);

        QuizAnswer answer = answerRepository.findByAttemptIdAndQuestionId(attempt.getId(), questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No answer for this question on this attempt"));

        answer.setPointsAwarded(request.pointsAwarded());
        answer.setTutorFeedback(request.tutorFeedback());
        answerRepository.save(answer);

        return QuizAttemptResponse.from(attempt, loadAnswerResponses(attempt));
    }

    public QuizAttemptResponse releaseAttempt(UUID attemptId, String tutorEmail) {
        QuizAttempt attempt = requireOwnedAttemptForTutor(attemptId, tutorEmail);
        finalizeIfExpired(attempt);

        if (attempt.getStatus() != AttemptStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot release an attempt that hasn't been submitted yet");
        }

        attempt.setReleasedAt(LocalDateTime.now());
        attemptRepository.save(attempt);

        return QuizAttemptResponse.from(attempt, loadAnswerResponses(attempt));
    }

    // ---- shared finalize/grading ----

    private void finalizeIfExpired(QuizAttempt attempt) {
        if (attempt.getStatus() == AttemptStatus.IN_PROGRESS
                && attempt.getDeadlineAt() != null
                && LocalDateTime.now().isAfter(attempt.getDeadlineAt())) {
            finalizeAttempt(attempt, attempt.getDeadlineAt(), true);
        }
    }

    private void finalizeAttempt(QuizAttempt attempt, LocalDateTime submittedAt, boolean autoSubmitted) {
        autoGradeObjectiveAnswers(attempt);
        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(submittedAt);
        attempt.setAutoSubmitted(autoSubmitted);
        attemptRepository.save(attempt);
    }

    private void autoGradeObjectiveAnswers(QuizAttempt attempt) {
        for (QuizAnswer answer : answerRepository.findByAttemptId(attempt.getId())) {
            QuestionType type = answer.getQuestion().getType();
            if (type == QuestionType.MULTIPLE_CHOICE || type == QuestionType.TRUE_FALSE) {
                boolean correct = answer.getSelectedOption() != null && answer.getSelectedOption().isCorrect();
                answer.setCorrect(correct);
                answer.setPointsAwarded(correct ? answer.getQuestion().getPointsPossible() : BigDecimal.ZERO);
                answerRepository.save(answer);
            }
        }
    }

    private List<QuizAnswerResponse> loadAnswerResponses(QuizAttempt attempt) {
        List<QuizQuestion> questions = questionRepository.findByQuizIdOrderByPositionAsc(attempt.getQuiz().getId());
        Map<UUID, QuizAnswer> answersByQuestionId = answerRepository.findByAttemptId(attempt.getId()).stream()
                .collect(Collectors.toMap(answer -> answer.getQuestion().getId(), answer -> answer));

        return questions.stream()
                .map(question -> QuizAnswerResponse.from(
                        question,
                        answersByQuestionId.get(question.getId()),
                        optionRepository.findByQuestionIdOrderByPositionAsc(question.getId())))
                .toList();
    }

    private List<QuizAnswerResponse> loadStudentAnswerResponses(QuizAttempt attempt) {
        boolean released = attempt.getReleasedAt() != null;
        List<QuizQuestion> questions = questionRepository.findByQuizIdOrderByPositionAsc(attempt.getQuiz().getId());
        Map<UUID, QuizAnswer> answersByQuestionId = answerRepository.findByAttemptId(attempt.getId()).stream()
                .collect(Collectors.toMap(answer -> answer.getQuestion().getId(), answer -> answer));

        return questions.stream()
                .map(question -> QuizAnswerResponse.forStudent(
                        question,
                        answersByQuestionId.get(question.getId()),
                        optionRepository.findByQuestionIdOrderByPositionAsc(question.getId()),
                        released))
                .toList();
    }

    // ---- ownership/enrollment checks (duplicated per-service, matching this codebase's existing
    // pattern - e.g. SageService/SageToolExecutor each keep their own copies rather than sharing
    // QuizService's/SubmissionService's private helpers) ----

    private User requireTutor(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private User requireStudentUser(String email) {
        return userRepository.findByEmail(email)
                .filter(user -> user.getRole() == UserRole.STUDENT)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private Quiz requireEnrolledPublishedQuiz(UUID quizId, User student) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));

        boolean enrolled = enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), quiz.getCourse().getId());
        if (!enrolled || quiz.getPublishedAt() == null) {
            // 404, not 403 - masks "not enrolled" and "still a draft" the same way as elsewhere
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found");
        }

        return quiz;
    }

    private Quiz requireOwnedQuiz(UUID quizId, String tutorEmail) {
        User tutor = requireTutor(tutorEmail);
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));

        if (!quiz.getCourse().getTutor().getId().equals(tutor.getId())) {
            // 404, not 403 - avoid confirming another tutor's quiz exists
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found");
        }

        return quiz;
    }

    private QuizAttempt requireOwnedAttemptForStudent(UUID attemptId, String studentEmail) {
        User student = requireStudentUser(studentEmail);
        QuizAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found"));

        if (!attempt.getStudent().getId().equals(student.getId())) {
            // 404, not 403 - avoid confirming another student's attempt exists
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found");
        }

        return attempt;
    }

    private QuizAttempt requireOwnedAttemptForTutor(UUID attemptId, String tutorEmail) {
        User tutor = requireTutor(tutorEmail);
        QuizAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found"));

        if (!attempt.getQuiz().getCourse().getTutor().getId().equals(tutor.getId())) {
            // 404, not 403 - avoid confirming another tutor's attempt exists
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found");
        }

        return attempt;
    }
}
