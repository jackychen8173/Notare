package com.notare.sage;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.TextBlock;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notare.sage.dto.PendingReviewsResponse;
import com.notare.sage.dto.ProgressSummaryResponse;
import com.notare.session.Session;
import com.notare.session.SessionNote;
import com.notare.session.SessionNoteRepository;
import com.notare.session.SessionRepository;
import com.notare.session.dto.SessionNoteResponse;
import com.notare.submission.Submission;
import com.notare.submission.SubmissionRepository;
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

    private final AnthropicClient anthropicClient;
    private final ObjectMapper objectMapper;
    private final SessionRepository sessionRepository;
    private final SessionNoteRepository sessionNoteRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;

    public SageService(
            AnthropicClient anthropicClient,
            ObjectMapper objectMapper,
            SessionRepository sessionRepository,
            SessionNoteRepository sessionNoteRepository,
            SubmissionRepository submissionRepository,
            UserRepository userRepository
    ) {
        this.anthropicClient = anthropicClient;
        this.objectMapper = objectMapper;
        this.sessionRepository = sessionRepository;
        this.sessionNoteRepository = sessionNoteRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
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

        SageFeedback feedback = completeStructured(FEEDBACK_SYSTEM_PROMPT, submission.getContent());
        submission.setSageFeedback(toJson(feedback));
        submissionRepository.save(submission);

        return SubmissionResponse.from(submission);
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

    private SageFeedback completeStructured(String systemPrompt, String userMessage) {
        StructuredMessageCreateParams<SageFeedback> params = MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(2048L)
                .system(systemPrompt)
                .outputConfig(SageFeedback.class)
                .addUserMessage(userMessage)
                .build();

        var block = anthropicClient.messages().create(params).content().stream()
                .flatMap(cb -> cb.text().stream())
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Sage did not return feedback"));

        return block.text();
    }

    private String toJson(SageFeedback feedback) {
        try {
            return objectMapper.writeValueAsString(feedback);
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
}
