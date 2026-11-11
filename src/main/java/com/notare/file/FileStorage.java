package com.notare.file;

import java.io.InputStream;
import java.time.Duration;

/**
 * Where uploaded bytes live. {@link S3FileStorage} in production (a Railway bucket), {@link
 * LocalFileStorage} for development and tests, {@link DisabledFileStorage} when neither is set up.
 * Callers have already checked permissions; this layer only moves bytes.
 */
public interface FileStorage {

    boolean enabled();

    void put(String key, String contentType, long size, InputStream content);

    /**
     * A short-lived URL that downloads the object directly (no auth header needed, so a browser can
     * just open it). inline shows images and PDFs in the browser; anything else downloads.
     */
    String downloadUrl(String key, String fileName, String contentType, boolean inline, Duration lifetime);

    void delete(String key);
}
