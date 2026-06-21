package com.tianji.gateway.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthPropertiesTest {

    @Test
    void afterPropertiesSet_shouldExposeClubProfilePublicEndpointWithoutLogin() {
        AuthProperties properties = new AuthProperties();

        properties.afterPropertiesSet();

        assertTrue(properties.getExcludePath().contains("GET:/api/config/club-profile/public"));
        assertTrue(properties.getExcludePath().contains("GET:/api/config/tenant-self-service/public"));
        assertTrue(properties.getExcludePath().contains("GET:/api/contact/public/shield"));
    }

    @Test
    void afterPropertiesSet_shouldExposeEmailLoginEndpointsWithoutLogin() {
        AuthProperties properties = new AuthProperties();

        properties.afterPropertiesSet();

        assertTrue(properties.getExcludePath().contains("POST:/auth/email-login/prepare"));
        assertTrue(properties.getExcludePath().contains("POST:/auth/email-login/confirm"));
    }

    @Test
    void afterPropertiesSet_shouldExposeInternalGatewayCallbacksWithoutLogin() {
        AuthProperties properties = new AuthProperties();

        properties.afterPropertiesSet();

        assertTrue(properties.getExcludePath().contains("POST:/api/admission/internal/register-success-notify"));
        assertTrue(properties.getExcludePath().contains("POST:/api/email-center/internal/invitations/register-success"));
        assertTrue(properties.getExcludePath().contains("POST:/api/email-center/internal/send"));
        assertTrue(properties.getExcludePath().contains("POST:/api/email-center/internal/tasks/**"));
        assertTrue(properties.getExcludePath().contains("POST:/api/email-center/internal/tasks/*/recipients/*/success"));
        assertTrue(properties.getExcludePath().contains("POST:/api/email-center/internal/tasks/*/recipients/*/failure"));
        assertTrue(properties.getExcludePath().contains("POST:/api/referral-codes/internal/validate"));
        assertTrue(properties.getExcludePath().contains("POST:/api/referral-codes/internal/register-success"));
        assertTrue(properties.getExcludePath().contains("GET:/api/alumni/profile-form/**"));
        assertTrue(properties.getExcludePath().contains("POST:/api/alumni/profile-form/**"));
        assertTrue(properties.getExcludePath().contains("GET:/api/workflow/config/current/internal"));
        assertTrue(properties.getExcludePath().contains("POST:/api/credit/internal/grants/batch"));
    }

    @Test
    void afterPropertiesSet_shouldExposePortalHistoryAndAlumniDisplayEndpointsWithoutLogin() {
        AuthProperties properties = new AuthProperties();

        properties.afterPropertiesSet();

        assertTrue(properties.getExcludePath().contains("GET:/api/alumni/show-overview"));
        assertTrue(properties.getExcludePath().contains("GET:/api/alumni/show-grouped-by-generation"));
        assertTrue(properties.getExcludePath().contains("GET:/api/history/timeline"));
        assertTrue(properties.getExcludePath().contains("GET:/api/history/search"));
        assertTrue(properties.getExcludePath().contains("GET:/api/history/statistics"));
    }

    @Test
    void afterPropertiesSet_shouldExposePublicMigrationStatusWithoutLogin() {
        AuthProperties properties = new AuthProperties();

        properties.afterPropertiesSet();

        assertTrue(properties.getExcludePath().contains("GET:/api/ops/migration/active"));
    }
}
