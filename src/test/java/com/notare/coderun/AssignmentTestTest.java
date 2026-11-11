package com.notare.coderun;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notare.support.IntegrationTest;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Teacher-written output tests. The fake sandbox stands in for the Next.js route: for each input
 * it "prints" "Hello " + the input, so a test passes when its expected output says that.
 */
class AssignmentTestTest extends IntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final AtomicReference<String> lastSandboxBody = new AtomicReference<>();
    private static final HttpServer sandbox = startFakeSandbox();

    private static HttpServer startFakeSandbox() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/code-run", exchange -> {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                lastSandboxBody.set(body);
                List<Map<String, Object>> runs = new ArrayList<>();
                for (JsonNode input : JSON.readTree(body).get("inputs")) {
                    runs.add(Map.of("stdout", "Hello " + input.asText().strip() + "\n", "stderr", "",
                            "exitCode", 0, "timedOut", false));
                }
                byte[] reply = JSON.writeValueAsBytes(Map.of("runs", runs));
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, reply.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(reply);
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void sandboxUrl(DynamicPropertyRegistry registry) {
        registry.add("code-run.service-url", () -> "http://127.0.0.1:" + sandbox.getAddress().getPort());
    }

    @AfterAll
    static void stopSandbox() {
        sandbox.stop(0);
    }

    private static final String CODE = "public class Main { public static void main(String[] a) {} }";

    private UUID scoredAssignment(Account tutor, JsonNode course) throws Exception {
        JsonNode assignment = data(post("/api/courses/" + course.get("id").asText() + "/assignments"), tutor.token(),
                Map.of("title", "Greeter", "dueDate", "2030-01-15", "pointsPossible", 10), 201);
        return UUID.fromString(assignment.get("id").asText());
    }

    private void addTest(Account tutor, UUID assignmentId, String name, String stdin, String expected,
                         int points, boolean hidden) throws Exception {
        data(post("/api/assignments/" + assignmentId + "/tests"), tutor.token(),
                Map.of("name", name, "stdin", stdin, "expectedOutput", expected, "points", points, "hidden", hidden), 201);
    }

    @Test
    void studentsCheckAgainstTestsWithoutSeeingHiddenAnswers() throws Exception {
        Account tutor = register("TUTOR");
        Account student = register("STUDENT");
        JsonNode course = createCourse(tutor);
        join(student, course);
        UUID assignmentId = scoredAssignment(tutor, course);
        addTest(tutor, assignmentId, "Greets Ada", "Ada\n", "Hello Ada", 2, false);
        addTest(tutor, assignmentId, "Greets Bo", "Bo\n", "Hi Bo", 1, false);
        // Trailing spaces and blank lines don't count against the student.
        addTest(tutor, assignmentId, "Secret", "Cy\n", "Hello Cy   \n\n", 1, true);

        JsonNode tests = getJson("/api/student/assignments/" + assignmentId + "/tests", student.token(), 200).get("data");
        assertThat(tests).hasSize(3);
        assertThat(tests.get(0).get("expectedOutput").asText()).isEqualTo("Hello Ada");
        assertThat(tests.get(2).get("hidden").asBoolean()).isTrue();
        assertThat(tests.get(2).get("stdin").isNull()).isTrue();
        assertThat(tests.get(2).get("expectedOutput").isNull()).isTrue();

        JsonNode check = data(post("/api/assignments/" + assignmentId + "/check"), student.token(),
                Map.of("code", CODE), 200);
        JsonNode results = check.get("results");
        assertThat(results.get(0).get("passed").asBoolean()).isTrue();
        assertThat(results.get(1).get("passed").asBoolean()).isFalse();
        assertThat(results.get(1).get("actualOutput").asText()).isEqualTo("Hello Bo\n");
        assertThat(results.get(2).get("passed").asBoolean()).isTrue();
        assertThat(results.get(2).get("stdin").isNull()).isTrue();
        assertThat(results.get(2).get("expectedOutput").isNull()).isTrue();
        assertThat(results.get(2).get("actualOutput").isNull()).isTrue();
        assertThat(check.get("pointsEarned").asInt()).isEqualTo(3);
        assertThat(check.get("pointsPossible").asInt()).isEqualTo(4);
        assertThat(check.get("suggestedScore").isNull()).isTrue(); // the score is the teacher's call

        // The sandbox got the inputs, never the expected outputs.
        assertThat(lastSandboxBody.get()).contains("Cy").doesNotContain("Hi Bo").doesNotContain("Hello Ada");
    }

    @Test
    void tutorRunsTestsOnASubmissionAndGetsASuggestedScore() throws Exception {
        Account tutor = register("TUTOR");
        Account otherTutor = register("TUTOR");
        Account student = register("STUDENT");
        JsonNode course = createCourse(tutor);
        join(student, course);
        UUID assignmentId = scoredAssignment(tutor, course);
        addTest(tutor, assignmentId, "Greets Ada", "Ada", "Hello Ada", 3, false);
        addTest(tutor, assignmentId, "Greets Bo", "Bo", "Hi Bo", 1, true);
        UUID submissionId = submit(student, assignmentId, CODE);

        String storedPath = "/api/submissions/" + submissionId + "/test-results";
        assertThat(getJson(storedPath, tutor.token(), 200).get("data").isNull()).isTrue();

        JsonNode run = data(post("/api/submissions/" + submissionId + "/run-tests"), tutor.token(), null, 200);
        assertThat(run.get("pointsEarned").asInt()).isEqualTo(3);
        assertThat(new BigDecimal(run.get("suggestedScore").asText())).isEqualByComparingTo("7.50"); // 10 x 3/4
        // The teacher sees hidden tests in full.
        assertThat(run.get("results").get(1).get("expectedOutput").asText()).isEqualTo("Hi Bo");

        JsonNode stored = getJson(storedPath, tutor.token(), 200).get("data");
        assertThat(stored.get("pointsEarned").asInt()).isEqualTo(3);
        assertThat(stored.get("ranAt").isNull()).isFalse();

        // Teacher-only: not for the student, not for another tutor, and not in the student's view
        // of their own submission.
        getJson(storedPath, student.token(), 403);
        getJson(storedPath, otherTutor.token(), 404);
        call(post("/api/submissions/" + submissionId + "/run-tests"), otherTutor.token(), null, 404);
        JsonNode mine = getJson("/api/student/assignments/" + assignmentId + "/submission", student.token(), 200);
        assertThat(mine.toString()).doesNotContain("Hi Bo").doesNotContain("testResults");
    }

    @Test
    void testLimitsAndPermissions() throws Exception {
        Account tutor = register("TUTOR");
        Account student = register("STUDENT");
        JsonNode course = createCourse(tutor);
        join(student, course);
        UUID assignmentId = createAssignment(tutor, course);

        // No tests yet: there's nothing to check.
        call(post("/api/assignments/" + assignmentId + "/check"), student.token(), Map.of("code", CODE), 409);
        // Students can't write tests; points stay within 0-100.
        call(post("/api/assignments/" + assignmentId + "/tests"), student.token(),
                Map.of("name", "x", "expectedOutput", "y", "points", 1, "hidden", false), 403);
        call(post("/api/assignments/" + assignmentId + "/tests"), tutor.token(),
                Map.of("name", "x", "expectedOutput", "y", "points", 101, "hidden", false), 400);
    }
}
