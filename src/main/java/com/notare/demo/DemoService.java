package com.notare.demo;

import com.notare.auth.JwtUtil;
import com.notare.auth.dto.AuthResponse;
import com.notare.user.User;
import com.notare.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One-click demo login. Every call seeds a brand-new private classroom (see {@link DemoSeeder}) and
 * signs in as its teacher or student, so visitors never see or disturb each other's data.
 * Rate-limited per client IP, since each call writes a few dozen rows.
 */
@Service
public class DemoService {

    static final int MAX_DEMOS_PER_WINDOW = 10;
    // Across all clients, so rotating IPs can't create unbounded demos either.
    static final int MAX_DEMOS_PER_WINDOW_TOTAL = 300;
    static final Duration WINDOW = Duration.ofHours(1);
    private static final String ALL_CLIENTS = "*";

    private final DemoSeeder demoSeeder;
    private final JwtUtil jwtUtil;
    // In-memory is enough for a single backend instance; it resets on redeploy, which is fine here.
    private final Map<String, Deque<Instant>> recentDemosByClient = new ConcurrentHashMap<>();

    public DemoService(DemoSeeder demoSeeder, JwtUtil jwtUtil) {
        this.demoSeeder = demoSeeder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse startDemo(UserRole role, String clientKey) {
        if (role != UserRole.TUTOR && role != UserRole.STUDENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Demo role must be TUTOR or STUDENT");
        }
        recordAttempt(clientKey, MAX_DEMOS_PER_WINDOW, "Too many demos started from this network. Please try again later.");
        recordAttempt(ALL_CLIENTS, MAX_DEMOS_PER_WINDOW_TOTAL, "The demo is busy right now. Please try again later.");

        DemoSeeder.DemoAccounts accounts = demoSeeder.seed();
        User user = role == UserRole.TUTOR ? accounts.tutor() : accounts.student();
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole(), true);
    }

    private void recordAttempt(String clientKey, int limit, String message) {
        Instant now = Instant.now();
        Deque<Instant> recent = recentDemosByClient.computeIfAbsent(clientKey, key -> new ArrayDeque<>());
        synchronized (recent) {
            while (!recent.isEmpty() && recent.peekFirst().isBefore(now.minus(WINDOW))) {
                recent.pollFirst();
            }
            if (recent.size() >= limit) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, message);
            }
            recent.addLast(now);
        }
    }
}
