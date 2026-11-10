package com.notare.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window limiter: at most {@code limit} attempts per key within {@code window}.
 * The backend runs as a single instance, so memory is enough; a restart simply resets the counts.
 * Keys whose attempts have all aged out are dropped, so the map doesn't grow without bound.
 */
public class RateLimiter {

    private final int limit;
    private final Duration window;
    private final String message;
    private final Clock clock;
    private final Map<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();

    public RateLimiter(int limit, Duration window, String message) {
        this(limit, window, message, Clock.systemUTC());
    }

    RateLimiter(int limit, Duration window, String message, Clock clock) {
        this.limit = limit;
        this.window = window;
        this.message = message;
        this.clock = clock;
    }

    /** Records an attempt for {@code key}, or throws 429 if the key is already at its limit. */
    public void check(String key) {
        Instant now = clock.instant();
        Instant cutoff = now.minus(window);
        attempts.compute(key, (k, recent) -> {
            Deque<Instant> deque = recent == null ? new ArrayDeque<>() : recent;
            while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
                deque.pollFirst();
            }
            if (deque.size() >= limit) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, message);
            }
            deque.addLast(now);
            return deque;
        });
        if (attempts.size() > 10_000) {
            attempts.values().removeIf(deque -> {
                Instant last = deque.peekLast();
                return last == null || last.isBefore(cutoff);
            });
        }
    }
}
