package com.notare.support;

import com.anthropic.client.AnthropicClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Base class for integration tests: the whole app against a throwaway Postgres container
 * (Flyway runs every real migration), driven over HTTP through MockMvc. The Anthropic client is
 * a mock, so no test ever calls the real API. One Spring context is shared by every subclass.
 */
@SpringBootTest(properties = {
        // application.yml has no defaults for these; the container's connection details
        // replace the datasource values, the rest just need to be present.
        "DATABASE_URL=jdbc:postgresql://replaced-by-testcontainers/notare",
        "DATABASE_USERNAME=unused",
        "DATABASE_PASSWORD=unused",
        "JWT_SECRET=integration-test-secret-key-0123456789-abcdefghijklmnopqrstuvwxyz",
        "GOOGLE_OAUTH_CLIENT_ID=test-client.apps.googleusercontent.com",
        "CODE_RUN_INTERNAL_SECRET=test-secret",
})
@AutoConfigureMockMvc
@Import(IntegrationTest.Containers.class)
public abstract class IntegrationTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class Containers {
        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            return new PostgreSQLContainer("postgres:16-alpine");
        }
    }

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    protected MockMvc mvc;

    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
    protected AnthropicClient anthropicClient;

    protected final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void resetAnthropicMock() {
        org.mockito.Mockito.reset(anthropicClient);
    }

    // ---- HTTP helpers ----

    protected JsonNode call(MockHttpServletRequestBuilder request, String token, Object body, int expectedStatus)
            throws Exception {
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        }
        MvcResult result = mvc.perform(request).andReturn();
        int status = result.getResponse().getStatus();
        String text = result.getResponse().getContentAsString();
        if (status != expectedStatus) {
            throw new AssertionError("Expected HTTP " + expectedStatus + " but got " + status + ": " + text);
        }
        return text.isBlank() ? json.nullNode() : json.readTree(text);
    }

    /** The {@code data} field of the app's ApiResponse wrapper. */
    protected JsonNode data(MockHttpServletRequestBuilder request, String token, Object body, int expectedStatus)
            throws Exception {
        return call(request, token, body, expectedStatus).path("data");
    }

    // ---- fixtures ----

    protected record Account(String token, UUID id, String email, String name) {
    }

    protected Account register(String role) throws Exception {
        int n = SEQ.incrementAndGet();
        String email = role.toLowerCase() + n + "-" + UUID.randomUUID().toString().substring(0, 8) + "@test.notare";
        String name = (role.equals("TUTOR") ? "Tutor " : "Student ") + n;
        JsonNode auth = data(post("/api/auth/register"), null,
                Map.of("name", name, "email", email, "password", "password123", "role", role), 201);
        return new Account(auth.get("token").asText(), UUID.fromString(auth.get("userId").asText()), email, name);
    }

    protected JsonNode createCourse(Account tutor) throws Exception {
        return data(post("/api/courses"), tutor.token(),
                Map.of("name", "AP CSA " + SEQ.incrementAndGet(), "subject", "Computer Science"), 201);
    }

    protected void join(Account student, JsonNode course) throws Exception {
        call(post("/api/student/courses/join"), student.token(),
                Map.of("code", course.get("joinCode").asText()), 201);
    }

    protected UUID createAssignment(Account tutor, JsonNode course) throws Exception {
        JsonNode assignment = data(post("/api/courses/" + course.get("id").asText() + "/assignments"), tutor.token(),
                Map.of("title", "Loops " + SEQ.incrementAndGet(), "dueDate", "2030-01-15"), 201);
        return UUID.fromString(assignment.get("id").asText());
    }

    protected UUID submit(Account student, UUID assignmentId, String content) throws Exception {
        JsonNode submission = data(post("/api/assignments/" + assignmentId + "/submit"), student.token(),
                Map.of("content", content), 201);
        return UUID.fromString(submission.get("id").asText());
    }

    protected JsonNode getJson(String path, String token, int expectedStatus) throws Exception {
        return call(get(path), token, null, expectedStatus);
    }
}
