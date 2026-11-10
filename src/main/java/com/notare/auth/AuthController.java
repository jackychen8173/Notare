package com.notare.auth;

import com.notare.auth.dto.AuthResponse;
import com.notare.auth.dto.EmailVerificationConfirmRequest;
import com.notare.auth.dto.GoogleAuthRequest;
import com.notare.auth.dto.GoogleAuthResult;
import com.notare.auth.dto.LoginRequest;
import com.notare.auth.dto.PasswordResetConfirmRequest;
import com.notare.auth.dto.PasswordResetRequest;
import com.notare.auth.dto.RegisterRequest;
import com.notare.common.ApiResponse;
import com.notare.common.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthRateLimits rateLimits;

    public AuthController(AuthService authService, AuthRateLimits rateLimits) {
        this.authService = authService;
        this.rateLimits = rateLimits;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        rateLimits.register(ClientIp.of(http));
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        rateLimits.login(ClientIp.of(http), request.email());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<GoogleAuthResult>> authenticateWithGoogle(
            @Valid @RequestBody GoogleAuthRequest request, HttpServletRequest http) {
        rateLimits.google(ClientIp.of(http));
        GoogleAuthResult result = authService.authenticateWithGoogle(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /** Always 200 with the same message, whether or not the email has an account. */
    @PostMapping("/password-reset/request")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request, HttpServletRequest http) {
        rateLimits.passwordResetRequest(ClientIp.of(http), request.email());
        authService.requestPasswordReset(request.email());
        return ResponseEntity.ok(ApiResponse.success(
                "If an account uses that email, a reset link is on its way.", null));
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request, HttpServletRequest http) {
        rateLimits.tokenConfirm(ClientIp.of(http));
        authService.confirmPasswordReset(request.token(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success("Password updated. Sign in with your new password.", null));
    }

    @PostMapping("/verify-email/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmEmailVerification(
            @Valid @RequestBody EmailVerificationConfirmRequest request, HttpServletRequest http) {
        rateLimits.tokenConfirm(ClientIp.of(http));
        authService.confirmEmailVerification(request.token());
        return ResponseEntity.ok(ApiResponse.success("Email confirmed.", null));
    }

    /**
     * Needs a signed-in user. /api/auth/** is public in SecurityConfig, but the JWT filter still
     * sets the authentication when a valid token is sent, so it's checked here.
     */
    @PostMapping("/verify-email/resend")
    public ResponseEntity<ApiResponse<Void>> resendEmailVerification(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to resend the confirmation email");
        }
        rateLimits.verificationResend(authentication.getName());
        authService.resendEmailVerification(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Confirmation email sent.", null));
    }
}
