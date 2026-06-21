package com.tianji.user.config;

import com.tianji.authsdk.resource.config.ResourceAuthProperties;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceAuthCustomizationConfigTest {

    @Test
    void ensureAdmissionPublicPaths_shouldExposeClubProfilePublicEndpoint() {
        ResourceAuthProperties properties = new ResourceAuthProperties();
        properties.setExcludeLoginPaths(new ArrayList<>());

        ResourceAuthCustomizationConfig config = new ResourceAuthCustomizationConfig(properties);

        config.ensureAdmissionPublicPaths();

        assertTrue(properties.getExcludeLoginPaths().contains("/api/config/club-profile/public"));
        assertTrue(properties.getExcludeLoginPaths().contains("/api/email-center/internal/send"));
        assertTrue(properties.getExcludeLoginPaths().contains("/api/alumni/profile-form/**"));
        assertTrue(properties.getExcludeLoginPaths().contains("/api/alumni/show-overview"));
        assertTrue(properties.getExcludeLoginPaths().contains("/api/alumni/show-grouped-by-generation"));
        assertTrue(properties.getExcludeLoginPaths().contains("/api/history/timeline"));
        assertTrue(properties.getExcludeLoginPaths().contains("/api/history/search"));
        assertTrue(properties.getExcludeLoginPaths().contains("/api/history/statistics"));
    }
}
