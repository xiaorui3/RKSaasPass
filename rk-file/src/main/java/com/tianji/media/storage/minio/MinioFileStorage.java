package com.tianji.media.storage.minio;

import com.tianji.common.exceptions.CommonException;
import com.tianji.common.utils.AssertUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.media.storage.IFileStorage;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.MinioException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Slf4j
public class MinioFileStorage implements IFileStorage {

    private final MinioClient minioClient;
    private final String bucketName;

    public MinioFileStorage(MinioClient minioClient, String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
    }

    @Override
    public String uploadFile(String key, InputStream inputStream, long contentLength) {
        return uploadFile(bucketName, key, inputStream, contentLength, null);
    }

    @Override
    public String uploadFile(String bucket, String key, InputStream inputStream, long contentLength, String contentType) {
        AssertUtils.isNotBlank(bucket, "bucket cannot be blank");
        AssertUtils.isNotBlank(key, "file key cannot be blank");
        AssertUtils.isNotNull(inputStream, "file stream cannot be null");
        try {
            ensureBucketExists(bucket);
            PutObjectArgs.Builder builder = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(inputStream, contentLength, -1);
            if (contentType != null && !contentType.isBlank()) {
                builder.contentType(contentType);
            }
            ObjectWriteResponse response = minioClient.putObject(builder.build());
            log.info("Uploaded file {}/{} successfully", bucket, key);
            return response.etag();
        } catch (MinioException e) {
            log.error("MinIO upload failed for {}/{}: {}", bucket, key, e.getMessage(), e);
            throw new CommonException("upload file failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Upload failed for {}/{}", bucket, key, e);
            throw new CommonException("upload file failed", e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                log.warn("Failed to close upload stream", e);
            }
        }
    }

    @Override
    public InputStream downloadFile(String key) {
        return downloadFile(bucketName, key);
    }

    @Override
    public InputStream downloadFile(String bucket, String key) {
        AssertUtils.isNotBlank(bucket, "bucket cannot be blank");
        AssertUtils.isNotBlank(key, "file key cannot be blank");
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .build()
            );
        } catch (MinioException e) {
            log.error("MinIO download failed for {}/{}: {}", bucket, key, e.getMessage(), e);
            throw new CommonException("download file failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Download failed for {}/{}", bucket, key, e);
            throw new CommonException("download file failed", e);
        }
    }

    @Override
    public void deleteFile(String key) {
        deleteFile(bucketName, key);
    }

    @Override
    public void deleteFile(String bucket, String key) {
        AssertUtils.isNotBlank(bucket, "bucket cannot be blank");
        AssertUtils.isNotBlank(key, "file key cannot be blank");
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .build()
            );
            log.info("Deleted file {}/{}", bucket, key);
        } catch (MinioException e) {
            log.error("MinIO delete failed for {}/{}: {}", bucket, key, e.getMessage(), e);
            throw new CommonException("delete file failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Delete failed for {}/{}", bucket, key, e);
            throw new CommonException("delete file failed", e);
        }
    }

    @Override
    public void deleteFiles(List<String> keys) {
        if (CollUtils.isEmpty(keys)) {
            return;
        }
        for (String key : keys) {
            deleteFile(key);
        }
    }

    private void ensureBucketExists(String bucket) throws ServerException, InsufficientDataException,
            ErrorResponseException, IOException, NoSuchAlgorithmException,
            InvalidKeyException, InvalidResponseException, XmlParserException,
            InternalException {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (found) {
            return;
        }
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        String policy = "{\n" +
                "  \"Version\": \"2012-10-17\",\n" +
                "  \"Statement\": [\n" +
                "    {\n" +
                "      \"Effect\": \"Allow\",\n" +
                "      \"Principal\": {\"AWS\": \"*\"},\n" +
                "      \"Action\": [\"s3:GetObject\"],\n" +
                "      \"Resource\": [\"arn:aws:s3:::" + bucket + "/*\"]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder()
                        .bucket(bucket)
                        .config(policy)
                        .build()
        );
        log.info("Created MinIO bucket {}", bucket);
    }
}
