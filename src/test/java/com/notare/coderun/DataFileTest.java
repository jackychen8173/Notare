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
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Data files on an assignment: who can manage and read them, and that a student's Run of that
 * assignment sends them (and the typed input) to the sandbox. The sandbox route is stood in for
 * by a local HTTP server that records what it was sent.
 */
class DataFileTest extends IntegrationTest {

    private static final AtomicReference<String> lastSandboxBody = new AtomicReference<>();
    private static final HttpServer sandbox = startFakeSandbox();

    private static HttpServer startFakeSandbox() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/code-run", exchange -> {
                lastSandboxBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                byte[] reply = """
                        {"compileError":null,"stdout":"ok\\n","stderr":"","exitCode":0,"timedOut":false}"""
                        .getBytes(StandardCharsets.UTF_8);
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

    private String filesPath(UUID assignmentId) {
        return "/api/assignments/" + assignmentId + "/data-files";
    }

    @Test
    void tutorManagesFilesAndEnrolledStudentsCanReadThem() throws Exception {
        Account tutor = register("TUTOR");
        Account student = register("STUDENT");
        Account outsider = register("STUDENT");
        Account otherTutor = register("TUTOR");
        JsonNode course = createCourse(tutor);
        join(student, course);
        UUID assignmentId = createAssignment(tutor, course);

        JsonNode created = data(post(filesPath(assignmentId)), tutor.token(),
                Map.of("name", "scores.txt", "content", "90\n85\n"), 201);
        String fileId = created.get("id").asText();

        // Names are plain file names, never Java files, and unique per assignment.
        call(post(filesPath(assignmentId)), tutor.token(), Map.of("name", "scores.txt", "content", ""), 409);
        call(post(filesPath(assignmentId)), tutor.token(), Map.of("name", "Main.java", "content", ""), 400);
        call(post(filesPath(assignmentId)), tutor.token(), Map.of("name", "../etc/passwd", "content", ""), 400);

        // Another tutor can't see or touch them; a student who isn't enrolled can't read them.
        call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(filesPath(assignmentId)),
                otherTutor.token(), null, 404);
        call(put(filesPath(assignmentId) + "/" + fileId), otherTutor.token(),
                Map.of("name", "x.txt", "content", ""), 404);
        String studentPath = "/api/student/assignments/" + assignmentId + "/data-files";
        getJson(studentPath, outsider.token(), 404);
        // Students can't manage them.
        call(post(filesPath(assignmentId)), student.token(), Map.of("name", "mine.txt", "content", ""), 403);

        JsonNode seen = getJson(studentPath, student.token(), 200).get("data");
        assertThat(seen).hasSize(1);
        assertThat(seen.get(0).get("content").asText()).isEqualTo("90\n85\n");

        data(put(filesPath(assignmentId) + "/" + fileId), tutor.token(),
                Map.of("name", "grades.csv", "content", "a,b\n"), 200);
        assertThat(getJson(studentPath, student.token(), 200).get("data").get(0).get("name").asText())
                .isEqualTo("grades.csv");

        call(delete(filesPath(assignmentId) + "/" + fileId), tutor.token(), null, 200);
        assertThat(getJson(studentPath, student.token(), 200).get("data")).isEmpty();
    }

    @Test
    void runSendsTheTypedInputAndTheAssignmentsDataFiles() throws Exception {
        Account tutor = register("TUTOR");
        Account student = register("STUDENT");
        JsonNode course = createCourse(tutor);
        join(student, course);
        UUID assignmentId = createAssignment(tutor, course);
        data(post(filesPath(assignmentId)), tutor.token(), Map.of("name", "words.txt", "content", "apple\n"), 201);

        JsonNode result = data(post("/api/assignments/" + assignmentId + "/run"), student.token(),
                Map.of("code", "public class Main { public static void main(String[] a) {} }", "stdin", "5\n"), 200);
        assertThat(result.get("stdout").asText()).isEqualTo("ok\n");

        JsonNode sent = new ObjectMapper().readTree(lastSandboxBody.get());
        assertThat(sent.get("stdin").asText()).isEqualTo("5\n");
        assertThat(sent.get("files")).hasSize(1);
        assertThat(sent.get("files").get(0).get("name").asText()).isEqualTo("words.txt");
        assertThat(sent.get("files").get(0).get("content").asText()).isEqualTo("apple\n");

        // Practice runs aren't tied to an assignment, so they never carry its files.
        data(post("/api/student/workspace/run"), student.token(),
                Map.of("code", "public class Main { public static void main(String[] a) {} }"), 200);
        JsonNode practice = new ObjectMapper().readTree(lastSandboxBody.get());
        assertThat(practice.get("files")).isEmpty();
        assertThat(practice.get("stdin").asText()).isEmpty();
    }
}
