package com.tianji.user.service.adminops;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AdminMobileReleaseArtifactStorageService {

    private static final String BUCKET_NAME = "rk-user";
    private static final String OBJECT_PREFIX = "mobile-apk";
    private static final String APK_CONTENT_TYPE = "application/vnd.android.package-archive";

    private final MinioClient minioClient;

    public String uploadApk(String sourceFileName, byte[] content) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("apk file is empty");
        }
        String objectName = buildObjectName(sourceFileName);
        try {
            ensureBucketExists(BUCKET_NAME);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(objectName)
                            .stream(new ByteArrayInputStream(content), content.length, -1)
                            .contentType(APK_CONTENT_TYPE)
                            .build()
            );
            return BUCKET_NAME + "/" + objectName;
        } catch (Exception e) {
            throw new IllegalStateException("upload apk artifact failed", e);
        }
    }

    public void deleteApk(String apkPath) {
        String objectName = normalizeApkObjectName(apkPath);
        if (objectName.isBlank()) {
            return;
        }
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new IllegalStateException("delete apk artifact failed", e);
        }
    }

    private String buildObjectName(String sourceFileName) {
        LocalDate today = LocalDate.now();
        String fileName = sourceFileName == null || sourceFileName.isBlank()
                ? UUID.randomUUID().toString().replace("-", "") + ".apk"
                : sourceFileName.replaceAll("[^A-Za-z0-9._-]", "_");
        if (!fileName.toLowerCase().endsWith(".apk")) {
            fileName = fileName + ".apk";
        }
        return String.format(
                "%s/%04d/%02d/%02d/%s-%s",
                OBJECT_PREFIX,
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                UUID.randomUUID().toString().replace("-", ""),
                fileName
        );
    }

    private String normalizeApkObjectName(String apkPath) {
        if (apkPath == null || apkPath.isBlank()) {
            return "";
        }
        String path = apkPath.trim().replace("\\", "/");
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        String bucketPrefix = BUCKET_NAME + "/";
        if (path.startsWith(bucketPrefix)) {
            path = path.substring(bucketPrefix.length());
        }
        if (!path.startsWith(OBJECT_PREFIX + "/") || !path.toLowerCase().endsWith(".apk")) {
            return "";
        }
        return path;
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
}
