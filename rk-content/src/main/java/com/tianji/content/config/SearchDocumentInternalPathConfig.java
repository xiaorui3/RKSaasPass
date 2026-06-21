package com.tianji.content.config;

import com.tianji.authsdk.resource.config.ResourceAuthProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class SearchDocumentInternalPathConfig {

    private final ResourceAuthProperties resourceAuthProperties;

    public SearchDocumentInternalPathConfig(ResourceAuthProperties resourceAuthProperties) {
        this.resourceAuthProperties = resourceAuthProperties;
    }

    @PostConstruct
    public void ensureInternalSearchDocumentPath() {
        List<String> excludeLoginPaths = resourceAuthProperties.getExcludeLoginPaths();
        if (excludeLoginPaths == null) {
            excludeLoginPaths = new ArrayList<>();
            resourceAuthProperties.setExcludeLoginPaths(excludeLoginPaths);
        }
        if (!excludeLoginPaths.contains("/api/search-documents/internal/export")) {
            excludeLoginPaths.add("/api/search-documents/internal/export");
        }
    }
}
