package com.tianji.activity.config;

import com.tianji.authsdk.resource.config.ResourceAuthProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * Ensures rk-activity public activity/competition endpoints stay excluded from login interception
 * even if external configuration omits them.
 */
@Configuration
public class ResourceAuthCustomizationConfig {

    private final ResourceAuthProperties resourceAuthProperties;

    public ResourceAuthCustomizationConfig(ResourceAuthProperties resourceAuthProperties) {
        this.resourceAuthProperties = resourceAuthProperties;
    }

    @PostConstruct
    public void ensurePublicPaths() {
        List<String> excludeLoginPaths = resourceAuthProperties.getExcludeLoginPaths();
        if (excludeLoginPaths == null) {
            excludeLoginPaths = new ArrayList<>();
            resourceAuthProperties.setExcludeLoginPaths(excludeLoginPaths);
        }

        addIfMissing(excludeLoginPaths, "/api/activity");
        addIfMissing(excludeLoginPaths, "/api/activity/list");
        addIfMissing(excludeLoginPaths, "/api/activity/*");
        addIfMissing(excludeLoginPaths, "/api/activity/shared");
        addIfMissing(excludeLoginPaths, "/api/activity/hot");
        addIfMissing(excludeLoginPaths, "/api/activity/top");
        addIfMissing(excludeLoginPaths, "/api/activity/search");
        addIfMissing(excludeLoginPaths, "/api/achievement/list");
        addIfMissing(excludeLoginPaths, "/api/achievement/public");
        addIfMissing(excludeLoginPaths, "/api/achievement/search");
        addIfMissing(excludeLoginPaths, "/api/achievement/statistics");
        addIfMissing(excludeLoginPaths, "/api/achievement/type/*");
        addIfMissing(excludeLoginPaths, "/api/achievement/level/*");
        addIfMissing(excludeLoginPaths, "/api/achievement/*");

        addIfMissing(excludeLoginPaths, "/api/competition/list");
        addIfMissing(excludeLoginPaths, "/api/competition/shared");
        addIfMissing(excludeLoginPaths, "/api/competition/published");
        addIfMissing(excludeLoginPaths, "/api/competition/featured");
        addIfMissing(excludeLoginPaths, "/api/competition/status/*");
        addIfMissing(excludeLoginPaths, "/api/competition/search");
        addIfMissing(excludeLoginPaths, "/api/competition/*");
        addIfMissing(excludeLoginPaths, "/api/search-documents/internal/export");
    }

    private void addIfMissing(List<String> paths, String path) {
        if (!paths.contains(path)) {
            paths.add(path);
        }
    }
}
