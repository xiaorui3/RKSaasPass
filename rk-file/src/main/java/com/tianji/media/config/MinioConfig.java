package com.tianji.media.config;

import com.tianji.media.storage.IFileStorage;
import com.tianji.media.storage.minio.MinioFileStorage;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
public class MinioConfig {

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "minio")
    public MinioProperties minioProperties() {
        return new MinioProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    public MinioClient minioClient(MinioProperties properties) {
        validateRequiredProperties(properties);
        try {
            return MinioClient.builder()
                    .endpoint(properties.getEndpoint())
                    .credentials(properties.getAccessKey(), properties.getSecretKey())
                    .build();
        } catch (Exception e) {
            log.error("failed to create MinIO client", e);
            throw new IllegalStateException("failed to create MinIO client", e);
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public IFileStorage fileStorage(MinioClient minioClient, MinioProperties properties) {
        if (!StringUtils.hasText(properties.getBucketName())) {
            throw new IllegalStateException("minio configuration is incomplete");
        }
        return new MinioFileStorage(minioClient, properties.getBucketName().trim());
    }

    private void validateRequiredProperties(MinioProperties properties) {
        if (properties == null
                || !StringUtils.hasText(properties.getEndpoint())
                || !StringUtils.hasText(properties.getAccessKey())
                || !StringUtils.hasText(properties.getSecretKey())) {
            throw new IllegalStateException("minio configuration is incomplete");
        }
    }

    public static class MinioProperties {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucketName;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getBucketName() {
            return bucketName;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }
    }
}
