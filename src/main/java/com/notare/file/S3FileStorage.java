package com.notare.file;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

/**
 * A Railway bucket through the S3 API. Uploads pass through the backend (no bucket CORS to set up,
 * and 10 MB files are small); downloads are presigned URLs straight from the bucket, which Railway
 * doesn't charge egress for.
 */
public class S3FileStorage implements FileStorage {

    private final S3Client client;
    private final S3Presigner presigner;
    private final String bucket;

    public S3FileStorage(String endpoint, String region, String bucket, String accessKeyId, String secretAccessKey) {
        StaticCredentialsProvider credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKeyId, secretAccessKey));
        Region awsRegion = Region.of(region == null || region.isBlank() ? "auto" : region);
        this.bucket = bucket;
        this.client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(awsRegion)
                .credentialsProvider(credentials)
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();
        this.presigner = S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(awsRegion)
                .credentialsProvider(credentials)
                .build();
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public void put(String key, String contentType, long size, InputStream content) {
        client.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
                RequestBody.fromInputStream(content, size));
    }

    @Override
    public String downloadUrl(String key, String fileName, String contentType, boolean inline, Duration lifetime) {
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .responseContentType(inline ? contentType : "application/octet-stream")
                .responseContentDisposition(FileNames.contentDisposition(fileName, inline))
                .build();
        return presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(lifetime)
                        .getObjectRequest(get)
                        .build())
                .url()
                .toString();
    }

    @Override
    public void delete(String key) {
        client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }
}
