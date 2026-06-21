package com.tianji.gateway.filter;

import com.tianji.authsdk.gateway.util.AuthUtil;
import com.tianji.common.domain.dto.LoginUserDTO;
import com.tianji.gateway.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Gateway tenant filter.
 * Reads tenant and role information from the JWT token and forwards it
 * downstream through headers for multi-tenant isolation and RBAC checks.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantFilter implements GlobalFilter, Ordered {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String SUPER_ADMIN_HEADER = "X-Super-Admin";
    private static final String USER_HEADER = "user-info";
    private static final String ROLE_HEADER = "X-Role-Id";
    private static final Long SUPER_ADMIN_ROLE_ID = 1L;

    private final AuthUtil authUtil;
    private final AuthProperties authProperties;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();
        String method = exchange.getRequest().getMethodValue();
        String antPath = method + ":" + path;
        log.debug("TenantFilter processing: {}", antPath);

        if (isExcludePath(antPath, path)) {
            log.debug("TenantFilter: Path {} is excluded, skipping tenant injection", antPath);
            return chain.filter(exchange);
        }

        String token = exchange.getRequest().getHeaders().getFirst("authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            log.warn("No Bearer token found for {}, rejecting request to prevent unauthorized access", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String jwtToken = token.substring(7);
        try {
            var result = authUtil.parseToken(jwtToken);
            if (result == null || !result.success()) {
                log.warn(
                        "Token parse failed for {}, code={}, msg={}, rejecting request",
                        path,
                        result != null ? result.getCode() : "null",
                        result != null ? result.getMsg() : "null"
                );
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            LoginUserDTO userDTO = result.getData();
            boolean isSuperAdmin = SUPER_ADMIN_ROLE_ID.equals(userDTO.getRoleId());
            Long tenantId = userDTO.getTenantId() != null ? userDTO.getTenantId() : 1L;
            String requestedTenantHeader = exchange.getRequest().getHeaders().getFirst(TENANT_HEADER);
            if (isSuperAdmin && requestedTenantHeader != null && !requestedTenantHeader.isBlank()) {
                try {
                    tenantId = Long.parseLong(requestedTenantHeader.trim());
                } catch (NumberFormatException e) {
                    log.warn("TenantFilter: invalid requested tenant header {}, fallback to token tenant {}", requestedTenantHeader, tenantId);
                }
            }

            log.info(
                    "TenantFilter: userId={}, tenantId={}, roleId={}, isSuperAdmin={}",
                    userDTO.getUserId(),
                    tenantId,
                    userDTO.getRoleId(),
                    isSuperAdmin
            );

            ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate()
                    .header(TENANT_HEADER, tenantId.toString())
                    .header(USER_HEADER, userDTO.getUserId().toString())
                    .header(ROLE_HEADER, userDTO.getRoleId() != null ? userDTO.getRoleId().toString() : "0");

            if (isSuperAdmin) {
                requestBuilder.header(SUPER_ADMIN_HEADER, "true");
            }

            return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
        } catch (Exception e) {
            log.error("Token parse error for {}: {}, rejecting request", path, e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean isExcludePath(String antPath, String rawPath) {
        for (String pattern : authProperties.getExcludePath()) {
            if (pattern.contains(":")) {
                if (antPathMatcher.match(pattern, antPath)) {
                    return true;
                }
            } else if (antPathMatcher.match(pattern, rawPath)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        return -10;
    }
}
