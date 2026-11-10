package com.notare.auth;

import com.notare.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;

@TestPropertySource(properties =
        "CORS_ALLOWED_ORIGINS=https://notare-frontend.vercel.app, https://notare-frontend-*-team.vercel.app")
class CorsTest extends IntegrationTest {

    private MvcResult preflight(String origin) throws Exception {
        return mvc.perform(options("/api/auth/login")
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "POST")).andReturn();
    }

    @Test
    void exactAndWildcardOriginsAreAllowed() throws Exception {
        for (String origin : new String[]{
                "https://notare-frontend.vercel.app",
                "https://notare-frontend-git-feature-x-team.vercel.app"}) {
            MvcResult result = preflight(origin);
            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            assertThat(result.getResponse().getHeader("Access-Control-Allow-Origin")).isEqualTo(origin);
        }
    }

    @Test
    void otherOriginsAreRejected() throws Exception {
        assertThat(preflight("https://evil.example").getResponse().getStatus()).isEqualTo(403);
        assertThat(preflight("https://notare-frontend-x-team.vercel.app.evil.example").getResponse().getStatus()).isEqualTo(403);
        assertThat(preflight("https://other-app-x-team.vercel.app").getResponse().getStatus()).isEqualTo(403);
        // Note: '*' also matches hyphens, so "...-x-other-team.vercel.app" WOULD match. Auth is a
        // bearer token, not a cookie, so CORS isn't what keeps another site from acting as a user.
    }
}
