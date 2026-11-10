package com.notare.auth.token;

import com.notare.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Issues and redeems the one-time tokens behind emailed links. The raw token only ever exists in
 * the email; the database keeps its SHA-256 hash, so reading the table doesn't yield usable links.
 */
@Service
public class AuthTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthTokenRepository repository;

    public AuthTokenService(AuthTokenRepository repository) {
        this.repository = repository;
    }

    /** Creates a new token for the user, voiding any earlier unused one of the same purpose. */
    @Transactional
    public String issue(User user, AuthTokenPurpose purpose) {
        LocalDateTime now = LocalDateTime.now();
        repository.invalidateOpenTokens(user.getId(), purpose, now);

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        repository.save(AuthToken.builder()
                .user(user)
                .purpose(purpose)
                .tokenHash(hash(raw))
                .expiresAt(now.plus(purpose.lifetime()))
                .build());
        return raw;
    }

    /**
     * Marks the token used and returns its owner. Unknown, expired and already-used tokens all get
     * the same message, so the response says nothing about which one it was.
     */
    @Transactional
    public User redeem(String raw, AuthTokenPurpose purpose) {
        AuthToken token = repository.findByTokenHashAndPurpose(hash(raw), purpose)
                .filter(t -> t.getUsedAt() == null && t.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "This link is invalid or has expired. Request a new one."));
        token.setUsedAt(LocalDateTime.now());
        return token.getUser();
    }

    static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }
}
