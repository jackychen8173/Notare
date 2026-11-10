package com.notare.auth;

import com.notare.common.RateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;

/**
 * Limits on the public account endpoints, against password guessing, sign-up spam and using the
 * password-reset form to flood someone's inbox. Per-IP limits stop one client; per-email limits
 * stop many clients aiming at one account. The integration tests turn this off (rate-limit.enabled)
 * because they create many accounts from one address; AuthRateLimitTest turns it back on.
 */
@Component
public class AuthRateLimits {

    private static final Duration FIFTEEN_MINUTES = Duration.ofMinutes(15);
    private static final Duration HOUR = Duration.ofHours(1);
    private static final String TOO_MANY = "Too many attempts. Wait a few minutes and try again.";

    private final boolean enabled;
    private final RateLimiter loginPerIp = new RateLimiter(30, FIFTEEN_MINUTES, TOO_MANY);
    private final RateLimiter loginPerEmail = new RateLimiter(10, FIFTEEN_MINUTES, TOO_MANY);
    private final RateLimiter googlePerIp = new RateLimiter(30, FIFTEEN_MINUTES, TOO_MANY);
    private final RateLimiter registerPerIp = new RateLimiter(10, HOUR, "Too many new accounts from this network. Try again later.");
    private final RateLimiter resetPerIp = new RateLimiter(10, HOUR, TOO_MANY);
    private final RateLimiter resetPerEmail = new RateLimiter(3, HOUR, "A reset email was sent recently. Check your inbox, or try again in an hour.");
    private final RateLimiter tokenConfirmPerIp = new RateLimiter(20, HOUR, TOO_MANY);
    private final RateLimiter verificationResendPerUser = new RateLimiter(3, HOUR, "A confirmation email was sent recently. Check your inbox, or try again in an hour.");

    public AuthRateLimits(@Value("${rate-limit.enabled:true}") boolean enabled) {
        this.enabled = enabled;
    }

    public void login(String ip, String email) {
        if (!enabled) return;
        loginPerIp.check(ip);
        loginPerEmail.check(normalize(email));
    }

    public void google(String ip) {
        if (enabled) googlePerIp.check(ip);
    }

    public void register(String ip) {
        if (enabled) registerPerIp.check(ip);
    }

    public void passwordResetRequest(String ip, String email) {
        if (!enabled) return;
        resetPerIp.check(ip);
        resetPerEmail.check(normalize(email));
    }

    public void tokenConfirm(String ip) {
        if (enabled) tokenConfirmPerIp.check(ip);
    }

    public void verificationResend(String userEmail) {
        if (enabled) verificationResendPerUser.check(normalize(userEmail));
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
