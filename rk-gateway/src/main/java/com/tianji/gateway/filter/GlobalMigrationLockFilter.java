package com.tianji.gateway.filter;

import com.tianji.common.domain.R;
import com.tianji.common.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalMigrationLockFilter implements GlobalFilter, Ordered {

    private static final String GLOBAL_MIGRATION_LOCK_KEY = "rk:ops:global-migration:active";
    private static final String SERVICE_MIGRATION_IN_PROGRESS = "SERVICE_MIGRATION_IN_PROGRESS";
    private static final List<String> AUTH_PATHS = List.of("/auth/", "/api/auth/", "/accounts/");
    private static final List<String> READINESS_PATHS = List.of(
            "/actuator/", "/health", "/error", "/favicon.ico", "/doc.html",
            "/v2/api-docs", "/v3/api-docs", "/swagger-resources/", "/webjars/"
    );

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();
        String method = exchange.getRequest().getMethodValue();
        if (HttpMethod.OPTIONS.matches(method)
                || isMigrationStatusPath(path)
                || isAuthPath(path)
                || isReadinessPath(path)
                || isTenantBootstrapPath(path)
                || isMobileReleasePath(path)
                || (HttpMethod.GET.matches(method) && isPublicMediaPath(path))) {
            return chain.filter(exchange);
        }

        String lockPayload = readMigrationLock();
        if (!StringUtils.hasText(lockPayload)) {
            return chain.filter(exchange);
        }
        return rejectForMigration(exchange);
    }

    private String readMigrationLock() {
        try {
            return stringRedisTemplate.opsForValue().get(GLOBAL_MIGRATION_LOCK_KEY);
        } catch (Exception e) {
            log.warn("read global migration lock failed, fail open: {}", e.getMessage());
            return "";
        }
    }

    private Mono<Void> rejectForMigration(ServerWebExchange exchange) {
        var response = exchange.getResponse();
        response.setRawStatusCode(423);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        R<Object> body = R.error(423, "服务迁移正在进行，请等待迁移完成后再操作");
        byte[] bytes = JsonUtils.toJsonStr(body).getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.fromSupplier(() -> response.bufferFactory().wrap(bytes)));
    }

    private boolean isMigrationStatusPath(String path) {
        return "/api/ops/migration/active".equals(path)
                || "/admin/ops/public/deploy-package/active-migration".equals(path)
                || (path.startsWith("/admin/ops/public/deploy-package/") && path.endsWith("/linux-ssh-download"));
    }

    private boolean isAuthPath(String path) {
        return AUTH_PATHS.stream().anyMatch(path::startsWith);
    }

    private boolean isReadinessPath(String path) {
        return READINESS_PATHS.stream().anyMatch(path::startsWith);
    }

    private boolean isTenantBootstrapPath(String path) {
        return "/tenants/list".equals(path) || "/api/tenants/list".equals(path);
    }

    private boolean isMobileReleasePath(String path) {
        return "/mobile/releases/latest".equals(path) || "/api/mobile/releases/latest".equals(path);
    }

    private boolean isPublicMediaPath(String path) {
        return "/minio-files".equals(path) || path.startsWith("/minio-files/");
    }

    @Override
    public int getOrder() {
        return -20;
    }
}
