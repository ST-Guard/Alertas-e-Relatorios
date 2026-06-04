package com.sptech.school.s3;

import com.sptech.school.config.AppConfig;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;

public class S3StorageService implements AutoCloseable {
    private final AppConfig config;
    private final S3Client s3Client;

    public S3StorageService(AppConfig config) {
        this.config = config;
        this.s3Client = S3Client.builder()
                .region(config.region())
                .credentialsProvider(config.credentialsProvider())
                .build();
    }

    public String baixarJson() {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(config.bucket())
                .key(config.jsonKey())
                .build();

        ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
        return response.asString(StandardCharsets.UTF_8);
    }

    public void enviarPdf(String key, byte[] pdf) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(config.bucket())
                .key(key)
                .contentType("application/pdf")
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(pdf));
    }

    @Override
    public void close() {
        s3Client.close();
    }
}
