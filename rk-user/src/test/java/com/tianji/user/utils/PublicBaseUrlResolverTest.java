package com.tianji.user.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PublicBaseUrlResolverTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void resolve_shouldPreferRequestOriginWhenItIsPublic() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "https://203.0.113.10:5174");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        String value = PublicBaseUrlResolver.resolve("http://localhost:5173");

        assertEquals("https://203.0.113.10:5174", value);
    }

    @Test
    void resolve_shouldPreferConfiguredPublicBaseUrlOverRequestHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "https://203.0.113.10:5174");
        request.addHeader("X-Forwarded-Proto", "https");
        request.addHeader("X-Forwarded-Host", "public.example.test");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        String value = PublicBaseUrlResolver.resolve("https://example.com");

        assertEquals("https://example.com", value);
    }

    @Test
    void resolve_shouldUseForwardedHeadersWhenOriginIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-Proto", "https");
        request.addHeader("X-Forwarded-Host", "example.com");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        String value = PublicBaseUrlResolver.resolve("");

        assertEquals("https://example.com", value);
    }

    @Test
    void resolve_shouldIgnoreLocalhostConfigAndFallbackToPublicDefault() {
        String value = PublicBaseUrlResolver.resolve("http://localhost:5173");

        assertEquals("https://example.com", value);
    }

    @Test
    void resolve_shouldIgnorePrivateBackendHostAndFallbackToPublicDefault() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Host", "10.0.0.18:8082");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        String value = PublicBaseUrlResolver.resolve("");

        assertEquals("https://example.com", value);
    }

    @Test
    void resolve_shouldIgnorePrivateConfiguredBaseUrlAndFallbackToPublicDefault() {
        String value = PublicBaseUrlResolver.resolve("http://10.0.0.18:8082");

        assertEquals("https://example.com", value);
    }

    @Test
    void resolve_shouldIgnorePublicGatewayPortAndFallbackToPublicDefault() {
        String value = PublicBaseUrlResolver.resolve("https://203.0.113.10:10010");

        assertEquals("https://example.com", value);
    }

    @Test
    void resolve_shouldIgnoreBackendServicePortEvenWhenHostIsPublicDomain() {
        String value = PublicBaseUrlResolver.resolve("https://example.com:8082");

        assertEquals("https://example.com", value);
    }
}
