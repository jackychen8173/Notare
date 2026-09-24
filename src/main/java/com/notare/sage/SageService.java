package com.notare.sage;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.TextBlock;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notare.quiz.QuestionType;
import com.notare.quizattempt.QuizAnswer;
import com.notare.quizattempt.QuizAnswerRepository;
import com.notare.quizattempt.QuizAttempt;
import com.notare.quizattempt.QuizAttemptRepository;
import com.notare.quizattempt.QuizAttemptService;
import com.notare.quizattempt.dto.QuizAttemptResponse;
import com.notare.sage.dto.PendingReviewsResponse;
import com.notare.sage.dto.ProgressSummaryResponse;
import com.notare.session.Session;
import com.notare.session.SessionNote;
import com.notare.session.SessionNoteRepository;
import com.notare.session.SessionRepository;
import com.notare.session.dto.SessionNoteResponse;
import com.notare.submission.Submission;
import com.notare.submission.SubmissionCriterionScoreRepository;
import com.notare.submission.SubmissionRepository;
import com.notare.submission.dto.RubricScoreItem;
import com.notare.submission.dto.SubmissionResponse;
import com.notare.submission.FeedbackStatus;
import com.notare.user.User;
import com.notare.user.UserRepository;
import com.notare.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class SageService {

    private static final Model MODEL = Model.CLAUDE_SONNET_4_6;

    private static final String FEEDBACK_SYSTEM_PROMPT = """
            You are Sage, an AI teaching assistant embedded in a tutoring platform for AP Computer Science A \
            (Java) students. Review the student's Java code submission and provide feedback covering \
            correctness, code style against AP CSA conventions, concrete suggestions for improvement, and a \
            brief encouraging note. Reference specific parts of the submitted code rather than speaking in \
            generalities.""";

    private static final String NOTES_SYSTEM_PROMPT = """
            You are Sage, an AI assistant that turns a tutor's raw, shorthand tutoring session notes into a \
            polished, well-organized summary for the tutor's own records. Preserve all factual content from \
            the raw notes; never invent details that weren't present in them. Write in clear prose paragraphs, \
            not bullet points.""";

    private static final String PROGRESS_SYSTEM_PROMPT = """
            You are Sage, an AI assistant summarizing a student's tutoring progress for their tutor. Given a \
            list of the student's tutoring sessions and assignment submissions, write a concise 2-4 sentence \
            progress summary highlighting trends, strengths, and any areas needing attention.""";

    private static final String QUIZ_ANSWER_FEEDBACK_SYSTEM_PROMPT = """
            You are Sage, an AI teaching assistant embedded in a tutoring platform. Grade a student's \
            free-text answer to a quiz question. You are given the question prompt, how many points it's \
            worth, the tutor's reference answer or grading guide (if one was provided), and the student's \
            answer. Suggest a point score (a number between 0 and the points possible, fractional points are \
            fine) and write brief, specific feedback explaining the score. This is a draft for the tutor to \
            review, edit, and approve before it's ever shown to the student - be honest and specific rather \
            than generically generous.""";

    private final AnthropicClient anthropicClient;
    private final ObjectMapper objectMapper;
    private final SessionRepository sessionRepository;
    private final SessionNoteRepository sessionNoteRepository;
    private final SubmissionRepository submissionRepository;
    private final SubmissionCriterionScoreRepository criterionScoreRepository;
    private final UserRepository userRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final QuizAttemptService quizAttemptService;

    public SageService(
            AnthropicClient anthropicClient,
            ObjectMapper objectMapper,
            SessionRepository sessionRepository,
            SessionNoteRepository sessionNoteRepository,
            SubmissionRepository submissionRepository,
            SubmissionCriterionScoreRepository criterionScoreRepository,
            UserRepository userRepository,
            QuizAttemptRepository quizAttemptRepository,
            QuizAnswerRepository quizAnswerRepository,
            QuizAttemptService quizAttemptService
    ) {
        this.anthropicClient = anthropicClient;
        this.objectMapper = objectMapper;
        this.sessionRepository = sessionRepository;
        this.sessionNoteRepository = sessionNoteRepository;
        this.submissionRepository = submissionRepository;
        this.criterionScoreRepository = criterionScoreRepository;
        this.userRepository = userRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.quizAnswerRepository = quizAnswerRepository;
        this.quizAttemptService = quizAttemptService;
    }

    public SessionNoteResponse draftSessionNotes(UUID sessionId, String tutorEmail) {
        Session session = requireOwnedSession(sessionId, tutorEmail);

        SessionNote note = sessionNoteRepository.findBySessionId(session.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No raw session notes to draft from - save notes first"));

        note.setFormattedNotes(complete(NOTES_SYSTEM_PROMPT, note.getRawNotes()));
        sessionNoteRepository.save(note);

        return SessionNoteResponse.from(note);
    }

    public SubmissionResponse reviewSubmission(UUID submissionId, String tutorEmail) {
        Submission submission = requireOwnedSubmission(submissionId, tutorEmail);

        SageFeedback feedback = completeStructured(FEEDBACK_SYSTEM_PROMPT, submission.getContent(), SageFeedback.class);
        submission.setSageFeedback(toJson(feedback));
        submissionRepository.save(submission);

        List<RubricScoreItem> rubricScores = criterionScoreRepository.findBySubmissionId(submission.getId()).stream()
                .map(score -> new RubricScoreItem(
                        score.getCriterion().getId(),
                        score.getCriterion().getName(),
                        score.getPointsAwarded(),
                        score.getCriterion().getPointsPossible()
                ))
                .toList();

        return SubmissionResponse.from(submission, rubricScores);
    }

    /**
     * Drafts a suggested score/feedback for one SHORT_ANSWER/ESSAY quiz answer, stored on
     * QuizAnswer.sageSuggestion - never shown to the student until the tutor releases the attempt
     * (same trust boundary as reviewSubmission's sageFeedback). Ownership of the attempt is checked
     * here, before any write, rather than only relying on quizAttemptService.getAttempt's own check
     * at the end - see requireOwnedQuizAttempt.
     */
    public QuizAttemptResponse draftQuizAnswerFeedback(UUID attemptId, UUID questionId, String tutorEmail) {
        QuizAttempt attempt = requireOwnedQuizAttempt(attemptId, tutorEmail);

        QuizAnswer answer = quizAnswerRepository.findByAttemptIdAndQuestionId(attempt.getId(), questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No answer for this question on this attempt"));

        QuestionType type = answer.getQuestion().getType();
        if (type != QuestionType.SHORT_ANSWER && type != QuestionType.ESSAY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Sage can only draft feedback for short answer or essay questions");
        }

        QuizAnswerFeedback feedback = completeStructured(
                QUIZ_ANSWER_FEEDBACK_SYSTEM_PROMPT, buildQuizAnswerPrompt(answer), QuizAnswerFeedback.class);
        answer.setSageSuggestion(toJson(feedback));
        quizAnswerRepository.save(answer);

        return quizAttemptService.getAttempt(attemptId, tutorEmail);
    }

    private String buildQuizAnswerPrompt(QuizAnswer answer) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Question: ").append(answer.getQuestion().getPrompt()).append("\n");
        prompt.append("Points possible: ").append(answer.getQuestion().getPointsPossible()).append("\n");
        if (answer.getQuestion().getReferenceAnswer() != null && !answer.getQuestion().getReferenceAnswer().isBlank()) {
            prompt.append("Tutor's reference answer / grading guide: ")
                    .append(answer.getQuestion().getReferenceAnswer()).append("\n");
        }
        prompt.append("Student's answer: ").append(
                answer.getTextResponse() != null && !answer.getTextResponse().isBlank()
                        ? answer.getTextResponse() : "(left blank)");
        return prompt.toString();
    }

    @Transactional(readOnly = true)
    public ProgressSummaryResponse generateProgressSummary(UUID studentId, String tutorEmail) {
        User tutor = requireTutor(tutorEmail);
        User student = userRepository.findById(studentId)
                .filter(user -> user.getRole() == UserRole.STUDENT)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        List<Session> sessions = sessionRepository.findByTutorIdAndStudentId(tutor.getId(), student.getId());
        List<Submission> submissions =
                submissionRepository.findByStudentIdAndAssignment_Course_Tutor_Id(student.getId(), tutor.getId());

        if (sessions.isEmpty() && submissions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No session or submission history for this student");
        }

        String summary = complete(PROGRESS_SYSTEM_PROMPT, buildProgressPrompt(student.getName(), sessions, submissions));

        return new ProgressSummaryResponse(student.getId(), summary);
    }

    @Transactional(readOnly = true)
    public PendingReviewsResponse pendingReviewsCount(String tutorEmail) {
        User tutor = requireTutor(tutorEmail);
        long count = submissionRepository.countByFeedbackStatusAndAssignment_Course_Tutor_Id(
                FeedbackStatus.PENDING, tutor.getId());
        return new PendingReviewsResponse(count);
    }

    private String buildProgressPrompt(String studentName, List<Session> sessions, List<Submission> submissions) {
        StringBuilder prompt = new StringBuilder("Student: ").append(studentName).append("\n\n");

        prompt.append("Sessions:\n");
        if (sessions.isEmpty()) {
            prompt.append("(none)\n");
        } else {
            for (Session session : sessions) {
                prompt.append("- ").append(session.getDate())
                        .append(" (").append(session.getStatus()).append("): ")
                        .append(session.getSubject())
                        .append(" (").append(session.getDuration()).append(" min)\n");
            }
        }

        prompt.append("\nAssignment submissions:\n");
        if (submissions.isEmpty()) {
            prompt.append("(none)\n");
        } else {
            for (Submission submission : submissions) {
                prompt.append("- \"").append(submission.getAssignment().getTitle()).append("\" submitted ")
                        .append(submission.getSubmittedAt())
                        .append(", status ").append(submission.getFeedbackStatus());
                if (submission.getGrade() != null) {
                    prompt.append(", grade ").append(submission.getGrade());
                }
                prompt.append("\n");
            }
        }

        return prompt.toString();
    }

    private String complete(String systemPrompt, String userMessage) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(1024L)
                .system(systemPrompt)
                .addUserMessage(userMessage)
                .build();

        return anthropicClient.messages().create(params).content().stream()
                .flatMap(block -> block.text().stream())
                .map(TextBlock::text)
                .collect(Collectors.joining("\n"))
                .strip();
    }

    private <T> T completeStructured(String systemPrompt, String userMessage, Class<T> outputType) {
        StructuredMessageCreateParams<T> params = MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(2048L)
                .system(systemPrompt)
                .outputConfig(outputType)
                .addUserMessage(userMessage)
                .build();

        var block = anthropicClient.messages().create(params).content().stream()
                .flatMap(cb -> cb.text().stream())
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Sage did not return feedback"));

        return block.text();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to serialize Sage feedback", e);
        }
    }

    private User requireTutor(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private Session requireOwnedSession(UUID sessionId, String tutorEmail) {
        User tutor = requireTutor(tutorEmail);
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (!session.getTutor().getId().equals(tutor.getId())) {
            // 404, not 403 - avoid confirming another tutor's session exists
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
        }

        return session;
    }

    private Submission requireOwnedSubmission(UUID submissionId, String tutorEmail) {
        User tutor = requireTutor(tutorEmail);
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));

        if (!submission.getAssignment().getCourse().getTutor().getId().equals(tutor.getId())) {
            // 404, not 403 - avoid confirming another tutor's submission exists
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found");
        }

        return submission;
    }

    private QuizAttempt requireOwnedQuizAttempt(UUID attemptId, String tutorEmail) {
        User tutor = requireTutor(tutorEmail);
        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found"));

        if (!attempt.getQuiz().getCourse().getTutor().getId().equals(tutor.getId())) {
            // 404, not 403 - avoid confirming another tutor's attempt exists
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found");
        }

        return attempt;
    }
}
