package com.notare.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;

@Configuration
public class FileStorageConfig {

    private static final Logger log = LoggerFactory.getLogger(FileStorageConfig.class);

    @Bean
    FileStorage fileStorage(
            @Value("${files.s3.bucket:}") String bucket,
            @Value("${files.s3.endpoint:}") String endpoint,
            @Value("${files.s3.region:auto}") String region,
            @Value("${files.s3.access-key-id:}") String accessKeyId,
            @Value("${files.s3.secret-access-key:}") String secretAccessKey,
            @Value("${files.local-dir:}") String localDir,
            @Value("${jwt.secret}") String signingSecret
    ) {
        if (!bucket.isBlank()) {
            if (endpoint.isBlank() || accessKeyId.isBlank() || secretAccessKey.isBlank()) {
                throw new IllegalStateException("S3_BUCKET is set but S3_ENDPOINT, S3_ACCESS_KEY_ID or S3_SECRET_ACCESS_KEY isn't");
            }
            log.info("File uploads: S3 bucket {}", bucket);
            return new S3FileStorage(endpoint, region, bucket, accessKeyId, secretAccessKey);
        }
        if (!localDir.isBlank()) {
            log.info("File uploads: local directory (development only)");
            return new LocalFileStorage(Path.of(localDir), signingSecret);
        }
        log.info("File uploads: off (no S3_BUCKET or FILES_LOCAL_DIR)");
        return new DisabledFileStorage();
    }

    /** No storage configured: uploads answer 503 instead of writing to a disk that a redeploy wipes. */
    static final class DisabledFileStorage implements FileStorage {
        @Override
        public boolean enabled() {
            return false;
        }

        @Override
        public void put(String key, String contentType, long size, InputStream content) {
            throw off();
        }

        @Override
        public String downloadUrl(String key, String fileName, String contentType, boolean inline, Duration lifetime) {
            throw off();
        }

        @Override
        public void delete(String key) {
            // Nothing stored, nothing to delete.
        }

        private static ResponseStatusException off() {
            return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "File uploads aren't set up yet");
        }
    }
}
