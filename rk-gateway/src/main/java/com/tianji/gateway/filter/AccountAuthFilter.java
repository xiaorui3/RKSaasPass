package com.tianji.gateway.filter;

import com.tianji.authsdk.gateway.util.AuthUtil;
import com.tianji.common.domain.R;
import com.tianji.common.domain.dto.LoginUserDTO;
import com.tianji.gateway.config.AuthProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.tianji.auth.common.constants.JwtConstants.AUTHORIZATION_HEADER;
import static com.tianji.auth.common.constants.JwtConstants.USER_HEADER;

@Slf4j
@Component
public class AccountAuthFilter implements GlobalFilter, Ordered {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String ROLE_HEADER = "X-Role-Id";
    private static final String SUPER_ADMIN_HEADER = "X-Super-Admin";
    private static final Long SUPER_ADMIN_ROLE_ID = 1L;

    private final AuthUtil authUtil;
    private final AuthProperties authProperties;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public AccountAuthFilter(AuthUtil authUtil, AuthProperties authProperties) {
        this.authUtil = authUtil;
        this.authProperties = authProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethodValue();
        String path = request.getPath().toString();
        String antPath = method + ":" + path;

        log.debug("AccountAuthFilter processing: {}", antPath);

        if (isExcludePath(antPath)) {
            log.debug("AccountAuthFilter: Path {} is excluded, trying optional token propagation", antPath);
            exchange = propagateUserHeaderIfPossible(exchange);
            return chain.filter(exchange);
        }

        List<String> authHeaders = exchange.getRequest().getHeaders().get(AUTHORIZATION_HEADER);
        String authHeader = authHeaders == null || authHeaders.isEmpty() ? "" : authHeaders.get(0);
        String token = "";
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        R<LoginUserDTO> r = authUtil.parseToken(token);

        if (r.success()) {
            Long userId = r.getData().getUserId();
            log.info("AccountAuthFilter: User logged in, userId={}, setting user-info header for path={}", userId, path);
            exchange = exchange.mutate()
                    .request(builder -> builder.header(USER_HEADER, userId.toString()))
                    .build();
        } else {
            log.debug("AccountAuthFilter: User not logged in for path={}", path);
        }

        authUtil.checkAuth(antPath, r);
        return chain.filter(exchange);
    }

    private boolean isExcludePath(String antPath) {
        for (String pathPattern : authProperties.getExcludePath()) {
            if (antPathMatcher.match(pathPattern, antPath)) {
                return true;
            }
        }
        return false;
    }

    private ServerWebExchange propagateUserHeaderIfPossible(ServerWebExchange exchange) {
        List<String> authHeaders = exchange.getRequest().getHeaders().get(AUTHORIZATION_HEADER);
        String authHeader = authHeaders == null || authHeaders.isEmpty() ? "" : authHeaders.get(0);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return exchange;
        }

        String token = authHeader.substring(7);
        try {
            R<LoginUserDTO> result = authUtil.parseToken(token);
            if (result != null && result.success() && result.getData() != null && result.getData().getUserId() != null) {
                LoginUserDTO user = result.getData();
                Long tenantId = user.getTenantId() != null ? user.getTenantId() : 1L;
                ServerHttpRequest.Builder builder = exchange.getRequest().mutate()
                        .header(USER_HEADER, user.getUserId().toString())
                        .header(TENANT_HEADER, tenantId.toString())
                        .header(ROLE_HEADER, user.getRoleId() != null ? user.getRoleId().toString() : "0");
                if (SUPER_ADMIN_ROLE_ID.equals(user.getRoleId())) {
                    builder.header(SUPER_ADMIN_HEADER, "true");
                }
                return exchange.mutate().request(builder.build()).build();
            }
        } catch (Exception e) {
            log.warn("AccountAuthFilter: optional token propagation failed for {}: {}", exchange.getRequest().getPath(), e.getMessage());
        }
        return exchange;
    }

    @Override
    public int getOrder() {
        return 1000;
    }
}
