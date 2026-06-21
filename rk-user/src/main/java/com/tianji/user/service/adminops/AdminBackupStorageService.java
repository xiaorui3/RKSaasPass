package com.tianji.user.service.adminops;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.StatObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@RequiredArgsConstructor
public class AdminBackupStorageService {

    private static final String BUCKET_NAME = "rk-user";
    private static final String OBJECT_PREFIX = "backup";

    private final MinioClient minioClient;

    public String store(String fileName, byte[] content) {
        String objectName = buildObjectName(fileName);
        try {
            ensureBucketExists(BUCKET_NAME);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(objectName)
                            .stream(new ByteArrayInputStream(content), content.length, -1)
                            .contentType("application/gzip")
                            .build()
            );
            return BUCKET_NAME + "/" + objectName;
        } catch (Exception e) {
            throw new IllegalStateException("store backup artifact failed", e);
        }
    }

    public String storeFile(String fileName, Path sourcePath) {
        String objectName = buildObjectName(fileName);
        try {
            ensureBucketExists(BUCKET_NAME);
            long size = Files.size(sourcePath);
            try (var inputStream = Files.newInputStream(sourcePath)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(BUCKET_NAME)
                                .object(objectName)
                                .stream(inputStream, size, -1)
                                .contentType("application/zip")
                                .build()
                );
            }
            return BUCKET_NAME + "/" + objectName;
        } catch (Exception e) {
            throw new IllegalStateException("store backup artifact failed", e);
        }
    }

    public byte[] read(String relativePath) {
        StoredObject storedObject = parse(relativePath);
        try (GetObjectResponse response = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(storedObject.bucketName())
                        .object(storedObject.objectName())
                        .build()
        )) {
            return response.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("read backup artifact failed", e);
        }
    }

    public void stream(String relativePath, OutputStream outputStream) {
        StoredObject storedObject = parse(relativePath);
        try (GetObjectResponse response = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(storedObject.bucketName())
                        .object(storedObject.objectName())
                        .build()
        )) {
            response.transferTo(outputStream);
        } catch (Exception e) {
            throw new IllegalStateException("read backup artifact failed", e);
        }
    }

    public boolean exists(String relativePath) {
        StoredObject storedObject = parse(relativePath);
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(storedObject.bucketName())
                            .object(storedObject.objectName())
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void delete(String relativePath) {
        StoredObject storedObject = parse(relativePath);
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(storedObject.bucketName())
                            .object(storedObject.objectName())
                            .build()
            );
        } catch (Exception e) {
            throw new IllegalStateException("delete backup artifact failed", e);
        }
    }

    private void ensureBucketExists(String bucketName) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (exists) {
            return;
        }
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder()
                        .bucket(bucketName)
                        .config("{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"*\"},\"Action\":[\"s3:GetObject\"],\"Resource\":[\"arn:aws:s3:::" + bucketName + "/*\"]}]}")
                        .build()
        );
    }

    private String buildObjectName(String fileName) {
        LocalDate today = LocalDate.now();
        return String.format(
                "%s/%04d/%02d/%02d/%s",
                OBJECT_PREFIX,
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                fileName
        );
    }

    private StoredObject parse(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            throw new IllegalArgumentException("backup path is empty");
        }
        String normalized = relativePath.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        int slashIndex = normalized.indexOf('/');
        if (slashIndex <= 0 || slashIndex == normalized.length() - 1) {
            throw new IllegalArgumentException("invalid backup path: " + relativePath);
        }
        return new StoredObject(normalized.substring(0, slashIndex), normalized.substring(slashIndex + 1));
    }

    private static class StoredObject {
        private final String bucketName;
        private final String objectName;

        private StoredObject(String bucketName, String objectName) {
            this.bucketName = bucketName;
            this.objectName = objectName;
        }

        private String bucketName() {
            return bucketName;
        }

        private String objectName() {
            return objectName;
        }
    }
}
