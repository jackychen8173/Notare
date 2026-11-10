package com.notare.common;

import com.anthropic.models.messages.MessageCreateParams;
import com.notare.support.IntegrationTest;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * With a DSN set, a 5xx reaches Sentry (through the logged ERROR) without the caller's token,
 * and a 4xx doesn't. Events are captured in beforeSend and dropped, so nothing leaves the test.
 */
@TestPropertySource(properties = "sentry.dsn=https://public@o0.ingest.sentry.io/0")
@Import(SentryReportingTest.Capture.class)
class SentryReportingTest extends IntegrationTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class Capture {
        @Bean
        List<SentryEvent> sentEvents() {
            return new CopyOnWriteArrayList<>();
        }

        @Bean
        SentryOptions.BeforeSendCallback captureAndDrop(List<SentryEvent> sentEvents) {
            return (event, hint) -> {
                sentEvents.add(event);
                return null;
            };
        }
    }

    @Autowired
    private List<SentryEvent> sentEvents;

    @BeforeEach
    void clear() {
        sentEvents.clear();
    }

    @Test
    void serverErrorIsReportedWithoutTheToken() throws Exception {
        Account tutor = register("TUTOR");
        when(anthropicClient.messages().create(any(MessageCreateParams.class)))
                .thenThrow(new RuntimeException("upstream failed (test)"));

        call(post("/api/sage/chat"), tutor.token(), Map.of("message", "hi"), 502);

        assertThat(sentEvents).hasSize(1);
        SentryEvent event = sentEvents.get(0);
        assertThat(event.getThrowable()).hasMessageContaining("Sage is unavailable");
        assertThat(event.getEnvironment()).isEqualTo("local");
        String request = String.valueOf(event.getRequest() == null ? null : event.getRequest().getHeaders());
        assertThat(request).doesNotContain(tutor.token());
        assertThat(event.getUser() == null || event.getUser().getEmail() == null).isTrue();
    }

    @Test
    void clientErrorIsNotReported() throws Exception {
        call(post("/api/auth/login"), null, Map.of("email", "nobody@test.notare", "password", "wrong-password"), 401);
        assertThat(sentEvents).isEmpty();
    }
}
