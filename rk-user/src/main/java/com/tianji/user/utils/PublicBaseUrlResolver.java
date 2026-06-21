package com.tianji.user.utils;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Locale;

public final class PublicBaseUrlResolver {

    private static final int[] REJECTED_PUBLIC_PORTS = {8081, 8082, 8083, 8090, 8848, 9000, 9001, 10010};

    private PublicBaseUrlResolver() {
    }

    public static String resolve(String configuredBaseUrl) {
        if (isUsablePublicBaseUrl(configuredBaseUrl)) {
            return normalize(configuredBaseUrl);
        }
        String requestBaseUrl = resolveFromCurrentRequest();
        if (isUsablePublicBaseUrl(requestBaseUrl)) {
            return normalize(requestBaseUrl);
        }
        return "";
    }

    private static String resolveFromCurrentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes)) {
            return "";
        }
        HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
        if (request == null) {
            return "";
        }

        String origin = request.getHeader("Origin");
        if (isUsablePublicBaseUrl(origin)) {
            return origin;
        }

        String forwardedProto = firstHeaderValue(request, "X-Forwarded-Proto");
        String forwardedHost = firstHeaderValue(request, "X-Forwarded-Host");
        if (!forwardedHost.isBlank()) {
            String proto = forwardedProto.isBlank() ? "https" : forwardedProto;
            return proto + "://" + forwardedHost;
        }

        String host = request.getHeader("Host");
        if (host != null && !host.isBlank()) {
            String scheme = request.getScheme() == null || request.getScheme().isBlank() ? "https" : request.getScheme();
            return scheme + "://" + host;
        }

        return "";
    }

    private static String firstHeaderValue(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        if (value == null || value.isBlank()) {
            return "";
        }
        int commaIndex = value.indexOf(',');
        return (commaIndex >= 0 ? value.substring(0, commaIndex) : value).trim();
    }

    private static boolean isUsablePublicBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(value.trim());
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return false;
            }
            int port = uri.getPort();
            if (isRejectedPublicPort(port)) {
                return false;
            }
            String normalizedHost = host.trim().toLowerCase(Locale.ROOT);
            if ("localhost".equals(normalizedHost) || normalizedHost.endsWith(".localhost")) {
                return false;
            }
            if ("0.0.0.0".equals(normalizedHost) || "::1".equals(normalizedHost)) {
                return false;
            }
            return !isPrivateOrLoopbackIpv4(normalizedHost) && !isPrivateOrLoopbackIpv6(normalizedHost);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static boolean isRejectedPublicPort(int port) {
        if (port <= 0) {
            return false;
        }
        for (int rejectedPort : REJECTED_PUBLIC_PORTS) {
            if (rejectedPort == port) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPrivateOrLoopbackIpv4(String host) {
        String[] parts = host.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        int[] octets = new int[4];
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].matches("\\d{1,3}")) {
                return false;
            }
            int octet = Integer.parseInt(parts[i]);
            if (octet < 0 || octet > 255) {
                return false;
            }
            octets[i] = octet;
        }
        return octets[0] == 10
                || octets[0] == 127
                || (octets[0] == 169 && octets[1] == 254)
                || (octets[0] == 172 && octets[1] >= 16 && octets[1] <= 31)
                || (octets[0] == 192 && octets[1] == 168);
    }

    private static boolean isPrivateOrLoopbackIpv6(String host) {
        String normalized = host;
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        return lower.equals("::1")
                || lower.startsWith("fe80:")
                || lower.startsWith("fc")
                || lower.startsWith("fd");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("/+$", "");
    }
}
