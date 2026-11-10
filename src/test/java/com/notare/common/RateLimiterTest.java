package com.notare.common;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimiterTest {

    /** A clock the test moves by hand. */
    private static final class TestClock extends Clock {
        private Instant now = Instant.parse("2026-11-10T12:00:00Z");

        void advance(Duration d) { now = now.plus(d); }
        @Override public Instant instant() { return now; }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
    }

    @Test
    void allowsUpToTheLimitThenRejectsUntilTheWindowPasses() {
        TestClock clock = new TestClock();
        RateLimiter limiter = new RateLimiter(2, Duration.ofMinutes(10), "slow down", clock);

        limiter.check("a");
        limiter.check("a");
        assertThatThrownBy(() -> limiter.check("a"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("slow down");

        // Other keys have their own budget.
        assertThatCode(() -> limiter.check("b")).doesNotThrowAnyException();

        clock.advance(Duration.ofMinutes(11));
        assertThatCode(() -> limiter.check("a")).doesNotThrowAnyException();
    }

    @Test
    void rejectedAttemptsDoNotExtendTheWindow() {
        TestClock clock = new TestClock();
        RateLimiter limiter = new RateLimiter(1, Duration.ofMinutes(10), "slow down", clock);

        limiter.check("a");
        clock.advance(Duration.ofMinutes(9));
        assertThatThrownBy(() -> limiter.check("a")).isInstanceOf(ResponseStatusException.class);
        clock.advance(Duration.ofMinutes(2));
        assertThatCode(() -> limiter.check("a")).doesNotThrowAnyException();
    }
}
