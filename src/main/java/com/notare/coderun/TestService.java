package com.notare.coderun;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notare.assignment.Assignment;
import com.notare.coderun.dto.SandboxRunRequest;
import com.notare.coderun.dto.SandboxTestResponse;
import com.notare.coderun.dto.TestRequest;
import com.notare.coderun.dto.TestResponse;
import com.notare.coderun.dto.TestRunResponse;
import com.notare.submission.Submission;
import com.notare.submission.SubmissionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Teacher-written output tests on an assignment. Students can check their code against them
 * while they work (hidden tests show only pass or fail). The tutor runs them on a submission from
 * the review page, which stores the result there and suggests a score; nothing about that run
 * reaches the student until the tutor releases a score of their own.
 *
 * <p>Expected outputs stay in the backend: the sandbox gets only the inputs and returns what the
 * program printed, and the comparison (OutputMatch) happens here.
 *
 * <p>Not @Transactional: a run takes seconds, so the database work happens in short transactions
 * before and after the sandbox call (open-in-view is off, so lazy relations load only inside one).
 */
@Service
public class TestService {

    static final int MAX_TESTS_PER_ASSIGNMENT = 20;

    private final AssignmentTestRepository testRepository;
    private final SubmissionRepository submissionRepository;
    private final AssignmentAccess access;
    private final DataFileService dataFileService;
    private final SandboxClient sandboxClient;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transaction;
    private final TransactionTemplate readOnly;

    public TestService(
            AssignmentTestRepository testRepository,
            SubmissionRepository submissionRepository,
            AssignmentAccess access,
            DataFileService dataFileService,
            SandboxClient sandboxClient,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager
    ) {
        this.testRepository = testRepository;
        this.submissionRepository = submissionRepository;
        this.access = access;
        this.dataFileService = dataFileService;
        this.sandboxClient = sandboxClient;
        this.objectMapper = objectMapper;
        this.transaction = new TransactionTemplate(transactionManager);
        this.readOnly = new TransactionTemplate(transactionManager);
        this.readOnly.setReadOnly(true);
    }

    // ---- authoring (tutor) ----

    public List<TestResponse> listForTutor(UUID assignmentId, String tutorEmail) {
        return readOnly.execute(status -> {
            Assignment assignment = access.requireOwned(assignmentId, tutorEmail);
            return testRepository.findByAssignmentIdOrderByPositionAsc(assignment.getId()).stream()
                    .map(TestResponse::forTutor).toList();
        });
    }

    public List<TestResponse> listForStudent(UUID assignmentId, String studentEmail) {
        return readOnly.execute(status -> {
            Assignment assignment = access.requireVisibleToStudent(assignmentId, studentEmail);
            return testRepository.findByAssignmentIdOrderByPositionAsc(assignment.getId()).stream()
                    .map(TestResponse::forStudent).toList();
        });
    }

    public TestResponse create(UUID assignmentId, TestRequest request, String tutorEmail) {
        return transaction.execute(status -> {
            Assignment assignment = access.requireEditable(assignmentId, tutorEmail);
            long count = testRepository.countByAssignmentId(assignment.getId());
            if (count >= MAX_TESTS_PER_ASSIGNMENT) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "An assignment can have up to " + MAX_TESTS_PER_ASSIGNMENT + " tests");
            }
            AssignmentTest test = AssignmentTest.builder()
                    .assignment(assignment)
                    .position((int) count)
                    .build();
            apply(test, request);
            return TestResponse.forTutor(testRepository.save(test));
        });
    }

    public TestResponse update(UUID assignmentId, UUID testId, TestRequest request, String tutorEmail) {
        return transaction.execute(status -> {
            Assignment assignment = access.requireEditable(assignmentId, tutorEmail);
            AssignmentTest test = requireTest(assignment, testId);
            apply(test, request);
            return TestResponse.forTutor(testRepository.save(test));
        });
    }

    public void delete(UUID assignmentId, UUID testId, String tutorEmail) {
        transaction.executeWithoutResult(status -> {
            Assignment assignment = access.requireEditable(assignmentId, tutorEmail);
            testRepository.delete(requireTest(assignment, testId));
        });
    }

    // ---- running ----

    /** A student checks their current code. Nothing is stored. */
    public TestRunResponse check(UUID assignmentId, String code, String studentEmail) {
        Plan plan = readOnly.execute(status -> {
            Assignment assignment = access.requireVisibleToStudent(assignmentId, studentEmail);
            return plan(assignment, code);
        });
        return run(plan, null).forStudent();
    }

    /** The tutor runs the tests on a submission; the result is stored on it, for the tutor only. */
    public TestRunResponse runOnSubmission(UUID submissionId, String tutorEmail) {
        Plan plan = readOnly.execute(status -> {
            Submission submission = access.requireOwnedSubmission(submissionId, tutorEmail);
            if (submission.getContent() == null || submission.getContent().isBlank()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This submission has no code to test");
            }
            return plan(submission.getAssignment(), submission.getContent());
        });
        TestRunResponse result = run(plan, LocalDateTime.now());

        transaction.executeWithoutResult(status -> {
            Submission submission = access.requireOwnedSubmission(submissionId, tutorEmail);
            submission.setTestResults(toJson(result));
            submission.setTestsRunAt(result.ranAt());
            submissionRepository.save(submission);
        });
        return result;
    }

    /** The stored run on a submission, or null if the tests haven't been run on it yet. */
    public TestRunResponse storedForSubmission(UUID submissionId, String tutorEmail) {
        String json = readOnly.execute(status -> access.requireOwnedSubmission(submissionId, tutorEmail).getTestResults());
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, TestRunResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stored test results are unreadable", e);
        }
    }

    /** Everything a run needs, read inside the transaction as plain values. */
    private record Plan(String code, List<AssignmentTest> tests, List<SandboxRunRequest.File> files, Integer assignmentPoints) {
    }

    private Plan plan(Assignment assignment, String code) {
        List<AssignmentTest> tests = testRepository.findByAssignmentIdOrderByPositionAsc(assignment.getId());
        if (tests.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This assignment has no tests yet");
        }
        return new Plan(code, tests, dataFileService.filesForRun(assignment.getId()), assignment.getPointsPossible());
    }

    private TestRunResponse run(Plan plan, LocalDateTime ranAt) {
        SandboxTestResponse sandbox = sandboxClient.runEach(new SandboxRunRequest(
                plan.code(), "", plan.files(), plan.tests().stream().map(AssignmentTest::getStdin).toList()));

        List<SandboxTestResponse.Run> runs = sandbox.runs() == null ? List.of() : sandbox.runs();
        List<TestRunResponse.Result> results = new ArrayList<>();
        int earned = 0;
        int possible = 0;
        for (int i = 0; i < plan.tests().size(); i++) {
            AssignmentTest test = plan.tests().get(i);
            SandboxTestResponse.Run run = sandbox.compileError() == null && i < runs.size() ? runs.get(i) : null;
            boolean passed = run != null && !run.timedOut() && OutputMatch.matches(test.getExpectedOutput(), run.stdout());
            possible += test.getPoints();
            if (passed) {
                earned += test.getPoints();
            }
            results.add(new TestRunResponse.Result(test.getId(), test.getName(), test.getPoints(), test.isHidden(),
                    passed, run != null && run.timedOut(), test.getStdin(), test.getExpectedOutput(),
                    run == null ? null : run.stdout(), run == null ? null : run.stderr()));
        }
        return new TestRunResponse(sandbox.compileError(), results, earned, possible,
                suggestedScore(plan.assignmentPoints(), earned, possible), ranAt);
    }

    static BigDecimal suggestedScore(Integer assignmentPoints, int earned, int possible) {
        if (assignmentPoints == null || possible == 0) {
            return null;
        }
        return BigDecimal.valueOf(assignmentPoints)
                .multiply(BigDecimal.valueOf(earned))
                .divide(BigDecimal.valueOf(possible), 2, RoundingMode.HALF_UP);
    }

    private static void apply(AssignmentTest test, TestRequest request) {
        test.setName(request.name().trim());
        test.setStdin(request.stdin() == null ? "" : request.stdin());
        test.setExpectedOutput(request.expectedOutput());
        test.setPoints(request.points());
        test.setHidden(request.hidden());
    }

    private AssignmentTest requireTest(Assignment assignment, UUID testId) {
        return testRepository.findById(testId)
                .filter(test -> test.getAssignment().getId().equals(assignment.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found"));
    }

    private String toJson(TestRunResponse result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Couldn't store test results", e);
        }
    }
}
