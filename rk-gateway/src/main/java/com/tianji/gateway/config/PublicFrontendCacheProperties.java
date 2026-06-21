package com.tianji.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "rk.frontend-cache")
public class PublicFrontendCacheProperties {

    private boolean enabled = true;
    private long ttlSeconds = 900;
    private long ttlJitterSeconds = 300;
    private long nullTtlSeconds = 90;
    private int maxBodyBytes = 1024 * 1024;
    private List<String> cacheableExactPaths = new ArrayList<>();
    private List<String> cacheablePathPrefixes = new ArrayList<>(List.of(
            "/api/news",
            "/api/news/latest",
            "/api/news/top",
            "/api/news/shared",
            "/api/news/statistics",
            "/api/works",
            "/api/works/featured",
            "/api/works/latest",
            "/api/works/popular",
            "/api/works/statistics",
            "/api/comments",
            "/api/activity",
            "/api/activities",
            "/api/competition",
            "/api/competitions",
            "/api/notices",
            "/notifications/api/notices",
            "/api/history",
            "/api/alumni/overview",
            "/api/alumni/statistics",
            "/tenants/list",
            "/api/tenants/list"
    ));
}
