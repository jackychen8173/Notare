package com.notare.demo;

import com.notare.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * Demo accounts can't use Sage (Claude API) or the code Run button (Vercel Sandbox), since both cost
 * money per call and the demo is a public one-click login. Enforced here, in one place, rather than
 * in each Sage/Run service. Every POST under /api/sage calls Claude, plus the one GET that does
 * (student progress summary); the other Sage GETs only read the database and stay open.
 */
@Component
public class DemoRestrictionInterceptor implements HandlerInterceptor {

    static final List<String> PAID_ENDPOINTS = List.of("/api/sage/**", "/api/assignments/*/run");

    private static final List<String> PAID_GET_ENDPOINTS = List.of("/api/sage/student-progress/*");

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private final UserRepository userRepository;

    public DemoRestrictionInterceptor(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        boolean paid = HttpMethod.POST.matches(request.getMethod())
                ? PAID_ENDPOINTS.stream().anyMatch(pattern -> MATCHER.match(pattern, path))
                : HttpMethod.GET.matches(request.getMethod())
                        && PAID_GET_ENDPOINTS.stream().anyMatch(pattern -> MATCHER.match(pattern, path));
        if (!paid) {
            return true;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return true;
        }
        boolean demo = userRepository.findByEmail(authentication.getName()).map(user -> user.isDemo()).orElse(false);
        if (demo) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Sage and code Run are turned off in the demo. Sign up for a free account to use them.");
        }
        return true;
    }
}
