package com.tianji.gateway.filter;

import com.tianji.gateway.config.PublicFrontendCacheProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class PublicFrontendCacheFilter implements GlobalFilter, Ordered {

    private static final String CACHE_PREFIX = "rk:frontend-cache:";
    private static final String NULL_MARKER = "__RK_NULL__";
    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String CACHE_HEADER = "X-RK-Cache";
    private static final String CACHE_CONTROL_HEADER = "Cache-Control";

    private final StringRedisTemplate stringRedisTemplate;
    private final PublicFrontendCacheProperties properties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!isCacheableRequest(exchange)) {
            return chain.filter(exchange);
        }

        String key = buildCacheKey(exchange);
        String cached = readCache(key);
        if (StringUtils.hasText(cached)) {
            ServerHttpResponse response = exchange.getResponse();
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            response.getHeaders().set(CACHE_CONTROL_HEADER, "public, max-age=60");
            if (NULL_MARKER.equals(cached)) {
                response.getHeaders().set(CACHE_HEADER, "NULL");
                response.setStatusCode(HttpStatus.NOT_FOUND);
                byte[] empty = "{\"code\":404,\"msg\":\"No data\"}".getBytes(StandardCharsets.UTF_8);
                return response.writeWith(Mono.just(response.bufferFactory().wrap(empty)));
            }
            response.getHeaders().set(CACHE_HEADER, "HIT");
            byte[] bytes = cached.getBytes(StandardCharsets.UTF_8);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        }

        ServerHttpResponse originalResponse = exchange.getResponse();
        DataBufferFactory bufferFactory = originalResponse.bufferFactory();
        ServerHttpResponseDecorator decorated = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
                if (!isCacheableResponse(getDelegate())) {
                    return super.writeWith(body);
                }
                return DataBufferUtils.join(Flux.from(body))
                        .flatMap(dataBuffer -> {
                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(bytes);
                            DataBufferUtils.release(dataBuffer);
                            String bodyText = new String(bytes, StandardCharsets.UTF_8);
                            storeCache(key, bodyText);
                            getDelegate().getHeaders().set(CACHE_HEADER, "MISS");
                            return super.writeWith(Mono.just(bufferFactory.wrap(bytes)));
                        });
            }
        };
        return chain.filter(exchange.mutate().response(decorated).build());
    }

    private boolean isCacheableRequest(ServerWebExchange exchange) {
        if (!properties.isEnabled()) {
            return false;
        }
        if (!HttpMethod.GET.equals(exchange.getRequest().getMethod())) {
            return false;
        }
        HttpHeaders headers = exchange.getRequest().getHeaders();
        if (StringUtils.hasText(headers.getFirst(HttpHeaders.AUTHORIZATION))
                || headers.getOrEmpty(HttpHeaders.COOKIE).stream().anyMatch(this::containsLoginCookie)) {
            return false;
        }
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        return properties.getCacheableExactPaths().contains(path)
                || properties.getCacheablePathPrefixes().stream().anyMatch(path::startsWith);
    }

    private boolean containsLoginCookie(String cookie) {
        if (!StringUtils.hasText(cookie)) {
            return false;
        }
        String lower = cookie.toLowerCase(Locale.ROOT);
        return lower.contains("authorization=")
                || lower.contains("access_token=")
                || lower.contains("refresh_token=")
                || lower.contains("satoken=")
                || lower.contains("token=");
    }

    private boolean isCacheableResponse(ServerHttpResponse response) {
        HttpStatus status = response.getStatusCode();
        if (status == null) {
            return true;
        }
        int code = status.value();
        return code == 200 || code == 204 || code == 404;
    }

    private String buildCacheKey(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getRawPath();
        String query = exchange.getRequest().getURI().getRawQuery();
        String tenantId = exchange.getRequest().getHeaders().getFirst(TENANT_HEADER);
        if (!StringUtils.hasText(tenantId)) {
            tenantId = exchange.getRequest().getQueryParams().getFirst("tenantId");
        }
        if (!StringUtils.hasText(tenantId)) {
            tenantId = "public";
        }
        String raw = path + "?" + (query == null ? "" : query);
        String digest = DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
        return CACHE_PREFIX + "tenant:" + tenantId + ":" + digest;
    }

    private String readCache(String key) {
        try {
            return stringRedisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("frontend cache read failed, fail open: {}", e.getMessage());
            return null;
        }
    }

    private void storeCache(String key, String bodyText) {
        try {
            if (!StringUtils.hasText(bodyText)) {
                storeNullMarker(key);
                return;
            }
            if (bodyText.getBytes(StandardCharsets.UTF_8).length > properties.getMaxBodyBytes()) {
                return;
            }
            String lower = bodyText.toLowerCase(Locale.ROOT);
            if (lower.contains("\"code\":404") || lower.contains("\"data\":null")) {
                storeNullMarker(key);
                return;
            }
            stringRedisTemplate.opsForValue().set(key, bodyText, ttlWithJitter());
        } catch (Exception e) {
            log.warn("frontend cache store failed, fail open: {}", e.getMessage());
        }
    }

    private void storeNullMarker(String key) {
        stringRedisTemplate.opsForValue().set(key, NULL_MARKER, Duration.ofSeconds(Math.max(10, properties.getNullTtlSeconds())));
    }

    private Duration ttlWithJitter() {
        long base = Math.max(60, properties.getTtlSeconds());
        long jitter = Math.max(0, properties.getTtlJitterSeconds());
        long seconds = base + (jitter == 0 ? 0 : ThreadLocalRandom.current().nextLong(jitter + 1));
        return Duration.ofSeconds(seconds);
    }

    @Override
    public int getOrder() {
        return NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER - 1;
    }
}
