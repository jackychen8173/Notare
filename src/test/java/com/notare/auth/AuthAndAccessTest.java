package com.notare.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.notare.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class AuthAndAccessTest extends IntegrationTest {

    @Test
    void registerThenLoginReturnsAWorkingToken() throws Exception {
        Account tutor = register("TUTOR");
        JsonNode login = data(post("/api/auth/login"), null,
                Map.of("email", tutor.email(), "password", "password123"), 200);
        assertThat(login.get("role").asText()).isEqualTo("TUTOR");
        assertThat(login.get("demo").asBoolean()).isFalse();

        getJson("/api/courses", login.get("token").asText(), 200);
    }

    @Test
    void badCredentialsAndDuplicatesAreRejected() throws Exception {
        Account tutor = register("TUTOR");
        call(post("/api/auth/login"), null, Map.of("email", tutor.email(), "password", "wrong-password"), 401);
        call(post("/api/auth/login"), null, Map.of("email", "nobody@test.notare", "password", "password123"), 401);
        call(post("/api/auth/register"), null,
                Map.of("name", "Again", "email", tutor.email(), "password", "password123", "role", "TUTOR"), 409);
    }

    @Test
    void adminAccountsCannotSelfRegister() throws Exception {
        call(post("/api/auth/register"), null, Map.of("name", "Mallory",
                "email", "admin-" + UUID.randomUUID() + "@test.notare", "password", "password123", "role", "ADMIN"), 403);
    }

    @Test
    void protectedEndpointsNeedAValidToken() throws Exception {
        call(get("/api/courses"), null, null, 401);
        call(get("/api/courses"), "not-a-real-jwt", null, 401);
    }

    @Test
    void roleGatedEndpointsReturn403NotA500() throws Exception {
        Account student = register("STUDENT");
        Account tutor = register("TUTOR");
        getJson("/api/courses", student.token(), 403);
        getJson("/api/student/courses", tutor.token(), 403);
        getJson("/api/admin/users", tutor.token(), 403);
    }

    @Test
    void tutorsOnlySeeTheirOwnCourses() throws Exception {
        Account owner = register("TUTOR");
        Account other = register("TUTOR");
        JsonNode course = createCourse(owner);

        getJson("/api/courses/" + course.get("id").asText(), other.token(), 404);
        assertThat(getJson("/api/courses", other.token(), 200).get("data")).isEmpty();
    }
}
