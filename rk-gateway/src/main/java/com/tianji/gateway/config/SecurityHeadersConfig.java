package com.tianji.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;

/**
 * 安全头配置
 * P0-2修复: 添加安全响应头，防止XSS、点击劫持等攻击
 */
@Configuration
public class SecurityHeadersConfig {

    /**
     * 添加安全响应头的WebFilter
     */
    @Bean
    public WebFilter securityHeadersFilter() {
        return new WebFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
                exchange.getResponse().getHeaders().add("X-Frame-Options", "DENY");
                exchange.getResponse().getHeaders().add("X-Content-Type-Options", "nosniff");
                exchange.getResponse().getHeaders().add("X-XSS-Protection", "1; mode=block");
                exchange.getResponse().getHeaders().add("Referrer-Policy", "strict-origin-when-cross-origin");
                exchange.getResponse().getHeaders().add("Permissions-Policy", "geolocation=(self), microphone=()");
                exchange.getResponse().getHeaders().add("Content-Security-Policy", "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:; connect-src 'self'; frame-ancestors 'none';");

                // 仅在HTTPS环境下添加HSTS头
                // String scheme = exchange.getRequest().getURI().getScheme();
                // if ("https".equalsIgnoreCase(scheme)) {
                //     exchange.getResponse().getHeaders().add("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
                // }

                return chain.filter(exchange);
            }
        };
    }
}
