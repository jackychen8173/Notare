package com.notare.file;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Files on local disk, for development and tests (FILES_LOCAL_DIR). Download links point at the
 * backend's own /api/files/local, signed with an HMAC and expiring like a presigned S3 URL, so the
 * rest of the app works the same as in production.
 */
public class LocalFileStorage implements FileStorage {

    private final Path root;
    private final byte[] signingKey;

    public LocalFileStorage(Path root, String signingSecret) {
        this.root = root.toAbsolutePath().normalize();
        this.signingKey = signingSecret.getBytes(StandardCharsets.UTF_8);
        try {
            Files.createDirectories(this.root);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public void put(String key, String contentType, long size, InputStream content) {
        try {
            Path target = resolve(key);
            Files.createDirectories(target.getParent());
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
            Files.writeString(metaPath(target), contentType);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String downloadUrl(String key, String fileName, String contentType, boolean inline, Duration lifetime) {
        long expires = Instant.now().plus(lifetime).getEpochSecond();
        String payload = payload(key, fileName, inline, expires);
        return "/api/files/local?key=" + encode(key) + "&name=" + encode(fileName) + "&inline=" + inline
                + "&expires=" + expires + "&signature=" + sign(payload);
    }

    @Override
    public void delete(String key) {
        try {
            Path target = resolve(key);
            Files.deleteIfExists(target);
            Files.deleteIfExists(metaPath(target));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Checks a download link from {@link #downloadUrl}; 404 for anything wrong or expired. */
    Path verify(String key, String fileName, boolean inline, long expires, String signature) {
        boolean valid = signature != null
                && MessageDigest.isEqual(sign(payload(key, fileName, inline, expires)).getBytes(StandardCharsets.UTF_8),
                        signature.getBytes(StandardCharsets.UTF_8))
                && Instant.now().getEpochSecond() <= expires;
        Path target = valid ? resolve(key) : null;
        if (target == null || !Files.exists(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
        return target;
    }

    String contentTypeOf(Path file) {
        try {
            return Files.readString(metaPath(file));
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    private Path resolve(String key) {
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
        return target;
    }

    private static Path metaPath(Path file) {
        return file.resolveSibling(file.getFileName() + ".type");
    }

    private static String payload(String key, String fileName, boolean inline, long expires) {
        return key + "\n" + fileName + "\n" + inline + "\n" + expires;
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String encode(String value) {
        // %20, not "+": not every client decodes "+" in a query string as a space.
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
