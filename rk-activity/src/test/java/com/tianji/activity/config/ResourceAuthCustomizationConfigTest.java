package com.tianji.activity.config;

import com.tianji.authsdk.resource.config.ResourceAuthProperties;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceAuthCustomizationConfigTest {

    @Test
    void shouldAddSharedEndpointsToExcludeLoginPaths() {
        ResourceAuthProperties properties = new ResourceAuthProperties();
        properties.setExcludeLoginPaths(new ArrayList<>());

        ResourceAuthCustomizationConfig config = new ResourceAuthCustomizationConfig(properties);
        config.ensurePublicPaths();

        assertTrue(properties.getExcludeLoginPaths().contains("/api/activity"));
        assertTrue(properties.getExcludeLoginPaths().contains("/api/activity/list"));
        assertTrue(properties.getExcludeLoginPaths().contains("/api/activity/*"));
        assertTrue(properties.getExcludeLoginPaths().contains("/api/activity/shared"));
        assertTrue(properties.getExcludeLoginPaths().contains("/api/activity/hot"));
        assertTrue(properties.getExcludeLoginPaths().contains("/api/activity/top"));
        assertTrue(properties.getExcludeLoginPaths().contains("/api/activity/search"));
        assertTrue(properties.getExcludeLoginPaths().contains("/api/competition/list"));
        assertTrue(properties.getExcludeLoginPaths().contains("/api/competition/shared"));
        assertTrue(properties.getExcludeLoginPaths().contains("/api/competition/published"));
        assertTrue(properties.getExcludeLoginPaths().contains("/api/competition/featured"));
        assertTrue(properties.getExcludeLoginPaths().contains("/api/competition/status/*"));
        assertTrue(properties.getExcludeLoginPaths().contains("/api/competition/search"));
        assertTrue(properties.getExcludeLoginPaths().contains("/api/competition/*"));
    }
}
