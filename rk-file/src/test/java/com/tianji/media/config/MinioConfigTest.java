package com.tianji.media.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MinioConfigTest {

    @Test
    void minioProperties_shouldNotEmbedLegacyEndpointOrCredentials() {
        MinioConfig.MinioProperties properties = new MinioConfig.MinioProperties();

        assertNull(properties.getEndpoint());
        assertNull(properties.getAccessKey());
        assertNull(properties.getSecretKey());
        assertNull(properties.getBucketName());
    }

    @Test
    void minioClient_shouldFailFastWhenRequiredConfigurationIsMissing() {
        MinioConfig config = new MinioConfig();
        MinioConfig.MinioProperties properties = new MinioConfig.MinioProperties();

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> config.minioClient(properties)
        );

        assertEquals("minio configuration is incomplete", ex.getMessage());
    }
}
