package com.notare.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.notare.auth.dto.AuthResponse;
import com.notare.auth.dto.GoogleAuthRequest;
import com.notare.auth.dto.GoogleAuthResult;
import com.notare.auth.dto.LoginRequest;
import com.notare.auth.dto.RegisterRequest;
import com.notare.auth.token.AuthTokenPurpose;
import com.notare.auth.token.AuthTokenRepository;
import com.notare.auth.token.AuthTokenService;
import com.notare.email.AuthEmails;
import com.notare.user.User;
import com.notare.user.UserRepository;
import com.notare.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.GeneralSecurityException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final AuthTokenService authTokenService;
    private final AuthTokenRepository authTokenRepository;
    private final AuthEmails authEmails;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            GoogleIdTokenVerifier googleIdTokenVerifier,
            AuthTokenService authTokenService,
            AuthTokenRepository authTokenRepository,
            AuthEmails authEmails
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.googleIdTokenVerifier = googleIdTokenVerifier;
        this.authTokenService = authTokenService;
        this.authTokenRepository = authTokenRepository;
        this.authEmails = authEmails;
    }

    public AuthResponse register(RegisterRequest request) {
        if (request.role() == UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin accounts cannot self-register");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();

        userRepository.save(user);
        sendVerificationEmail(user);

        return toAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is deactivated");
        }

        return toAuthResponse(user);
    }

    // Single combined sign-in-or-sign-up. An existing account (by verified email) always just
    // logs in, regardless of whether a role was passed. A brand-new account needs a role: if none
    // was supplied yet, returns needsRole=true so the frontend can ask for one and re-POST the
    // same idToken - no second Google popup needed, since verification already happened once.
    public GoogleAuthResult authenticateWithGoogle(GoogleAuthRequest request) {
        GoogleIdToken.Payload payload = verifyGoogleIdToken(request.idToken());

        var existing = userRepository.findByEmail(payload.getEmail());
        if (existing.isPresent()) {
            User user = existing.get();
            if (!user.isActive()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is deactivated");
            }
            // Google only signs in with a verified address, so this also confirms the email.
            if (!user.isEmailVerified()) {
                user.setEmailVerified(true);
                userRepository.save(user);
            }
            return new GoogleAuthResult(false, toAuthResponse(user));
        }

        if (request.role() == null) {
            return new GoogleAuthResult(true, null);
        }
        if (request.role() == UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin accounts cannot self-register");
        }

        // Google-created accounts get an unguessable, properly-hashed placeholder rather than a
        // nullable password column - satisfies the existing NOT NULL constraint on User.password
        // without weakening it, and makes password-login on this account cryptographically
        // infeasible (a documented v1 limitation: no "set a password" flow exists yet).
        User user = User.builder()
                .name((String) payload.get("name"))
                .email(payload.getEmail())
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(request.role())
                .emailVerified(true)
                .build();

        userRepository.save(user);

        return new GoogleAuthResult(false, toAuthResponse(user));
    }

    /**
     * Sends a reset link if the email belongs to an active, non-demo account. Always returns
     * normally, and the email goes out on another thread, so the response doesn't reveal whether
     * an account exists. This is also how a Google-only account sets its first password.
     */
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email)
                .filter(user -> user.isActive() && !user.isDemo())
                .ifPresent(user -> authEmails.sendPasswordReset(
                        user.getEmail(), user.getName(), authTokenService.issue(user, AuthTokenPurpose.PASSWORD_RESET)));
    }

    /** Sets the new password and signs out every existing session (see JwtAuthenticationFilter). */
    @Transactional
    public void confirmPasswordReset(String token, String newPassword) {
        User user = authTokenService.redeem(token, AuthTokenPurpose.PASSWORD_RESET);
        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is deactivated");
        }
        LocalDateTime now = LocalDateTime.now();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(now);
        // Opening the emailed link proves the inbox is theirs.
        user.setEmailVerified(true);
        userRepository.save(user);
        authTokenRepository.invalidateOpenTokens(user.getId(), AuthTokenPurpose.PASSWORD_RESET, now);
    }

    @Transactional
    public void confirmEmailVerification(String token) {
        User user = authTokenService.redeem(token, AuthTokenPurpose.EMAIL_VERIFICATION);
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    /** Re-sends the confirmation link to the signed-in user; does nothing if already confirmed. */
    public void resendEmailVerification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in again"));
        if (!user.isEmailVerified() && !user.isDemo()) {
            sendVerificationEmail(user);
        }
    }

    private void sendVerificationEmail(User user) {
        authEmails.sendEmailVerification(
                user.getEmail(), user.getName(), authTokenService.issue(user, AuthTokenPurpose.EMAIL_VERIFICATION));
    }

    private GoogleIdToken.Payload verifyGoogleIdToken(String idToken) {
        GoogleIdToken googleIdToken;
        try {
            googleIdToken = googleIdTokenVerifier.verify(idToken);
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Google credential", e);
        }

        if (googleIdToken == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Google credential");
        }

        GoogleIdToken.Payload payload = googleIdToken.getPayload();
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Google account email is not verified");
        }

        return payload;
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole(), user.isDemo(),
                user.isEmailVerified());
    }
}
