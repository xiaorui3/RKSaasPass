package com.tianji.user.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.TenantContext;
import com.tianji.user.domain.dto.ApiCatalogDebugRequestDTO;
import com.tianji.user.domain.vo.ApiCatalogVO;
import com.tianji.user.service.IApiCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiCatalogServiceImpl implements IApiCatalogService {

    private static final List<String> SUPPORTED_METHODS = List.of("GET", "POST", "PUT", "DELETE", "PATCH");
    private static final int MAX_DEBUG_BODY_CHARS = 20_000;
    private static final Duration CATALOG_CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration CATALOG_REQUEST_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration DEBUG_REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private final ObjectMapper objectMapper;

    @Value("${rk.ops.gateway.url:http://rk-gateway:10010}")
    private String gatewayUrl;

    private final Map<String, String> apiDocServiceBaseUrls = buildApiDocServiceBaseUrls();

    @Override
    public ApiCatalogVO getCatalog() {
        List<String> warnings = new ArrayList<>();
        List<ApiCatalogVO.ServiceGroup> services = loadSwaggerResources(warnings);
        List<ApiCatalogVO.Endpoint> endpoints = services.stream()
                .flatMap(service -> service.getEndpoints().stream())
                .collect(Collectors.toList());
        return new ApiCatalogVO()
                .setGeneratedAt(LocalDateTime.now())
                .setTenantScope(resolveTenantScope())
                .setServiceCount(services.size())
                .setEndpointCount(endpoints.size())
                .setPublicApiCount((int) endpoints.stream().filter(item -> "PUBLIC".equals(item.getCategory())).count())
                .setAdminApiCount((int) endpoints.stream().filter(item -> "ADMIN".equals(item.getCategory())).count())
                .setInternalApiCount((int) endpoints.stream().filter(item -> "INTERNAL".equals(item.getCategory())).count())
                .setServices(services)
                .setWarnings(warnings);
    }

    @Override
    public ApiCatalogVO.DebugResult debug(ApiCatalogDebugRequestDTO request) {
        ApiCatalogDebugRequestDTO normalized = normalizeDebugRequest(request);
        ApiCatalogVO.Endpoint endpoint = findDebuggableEndpoint(normalized.getMethod(), normalized.getPath());
        if (endpoint == null) {
            throw new BadRequestException("接口目录中不存在该接口，不能绕过目录直接调试");
        }
        try {
            URI uri = buildDebugUri(normalized.getPath(), normalized.getQueryJson());
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .timeout(DEBUG_REQUEST_TIMEOUT);
            applyCurrentContextHeaders(builder);
            applyDebugMethod(builder, normalized);
            HttpResponse<String> response = HttpClient.newBuilder().build()
                    .send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new ApiCatalogVO.DebugResult()
                    .setMethod(normalized.getMethod())
                    .setPath(uri.getRawPath() + (StringUtils.hasText(uri.getRawQuery()) ? "?" + uri.getRawQuery() : ""))
                    .setStatusCode(response.statusCode())
                    .setBody(limitBody(response.body()))
                    .setResponseHeaders(flattenResponseHeaders(response.headers().map()))
                    .setCalledAt(LocalDateTime.now());
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("api catalog debug failed, method={}, path={}", normalized.getMethod(), normalized.getPath(), e);
            throw new IllegalStateException("接口调试失败: " + e.getMessage(), e);
        }
    }

    List<ApiCatalogVO.ServiceGroup> loadSwaggerResources(List<String> warnings) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(CATALOG_CONNECT_TIMEOUT)
                    .build();
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(sanitizeUrl(gatewayUrl) + "/swagger-resources"))
                    .timeout(CATALOG_REQUEST_TIMEOUT)
                    .GET();
            applyCatalogContextHeaders(builder);
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode root = objectMapper.readTree(response.body());
            if (!root.isArray()) {
                warnings.add("网关 Swagger 资源不是数组，已返回空目录");
                return List.of();
            }
            List<ApiCatalogVO.ServiceGroup> services = new ArrayList<>();
            for (JsonNode item : root) {
                String name = item.path("name").asText("");
                String url = item.path("url").asText("");
                if (!StringUtils.hasText(name) || !StringUtils.hasText(url) || !url.contains("api-docs")) {
                    continue;
                }
                services.add(loadServiceGroup(client, name, url, warnings));
            }
            services.sort(Comparator.comparing(ApiCatalogVO.ServiceGroup::getName, Comparator.nullsLast(String::compareTo)));
            return services;
        } catch (Exception e) {
            warnings.add("加载网关 Swagger 资源失败: " + e.getMessage());
            return List.of();
        }
    }

    private ApiCatalogVO.ServiceGroup loadServiceGroup(HttpClient client, String name, String url, List<String> warnings) {
        ApiCatalogVO.ServiceGroup group = new ApiCatalogVO.ServiceGroup()
                .setName(name)
                .setSourceUrl(url)
                .setBasePath("/")
                .setEndpoints(new ArrayList<>())
                .setEndpointCount(0);
        try {
            String authorization = resolveCurrentAuthorizationHeader();
            JsonNode root = loadApiDocRoot(client, url, authorization);
            group.setBasePath(resolveBasePath(root));
            List<ApiCatalogVO.Endpoint> endpoints = parseEndpoint(name, url, root.path("paths"));
            group.setEndpoints(endpoints);
            group.setEndpointCount(endpoints.size());
            return group;
        } catch (Exception e) {
            warnings.add(name + " 文档解析失败: " + e.getMessage());
            return group;
        }
    }

    private JsonNode loadApiDocRoot(HttpClient client, String resourceUrl, String authorization) throws Exception {
        HttpResponse<String> gatewayResponse = sendApiDocRequest(client, resolveGatewayApiDocUrl(resourceUrl), authorization);
        if (gatewayResponse.statusCode() >= 200 && gatewayResponse.statusCode() < 300) {
            return objectMapper.readTree(gatewayResponse.body());
        }
        String directServiceUrl = resolveDirectApiDocUrl(resourceUrl);
        if (StringUtils.hasText(directServiceUrl)) {
            HttpResponse<String> directResponse = sendApiDocRequest(client, directServiceUrl, authorization);
            if (directResponse.statusCode() >= 200 && directResponse.statusCode() < 300) {
                return objectMapper.readTree(directResponse.body());
            }
            throw new IllegalStateException("gateway api-docs status " + gatewayResponse.statusCode()
                    + ", direct api-docs status " + directResponse.statusCode());
        }
        throw new IllegalStateException("gateway api-docs request failed with status " + gatewayResponse.statusCode());
    }

    private HttpResponse<String> sendApiDocRequest(HttpClient client, String url, String authorization) throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(url));
        requestBuilder.timeout(CATALOG_REQUEST_TIMEOUT);
        requestBuilder.GET();
        if (StringUtils.hasText(authorization)) {
            requestBuilder.header("Authorization", authorization);
        }
        String tenantId = resolveCurrentTenantHeader();
        if (StringUtils.hasText(tenantId)) {
            requestBuilder.header("X-Tenant-Id", tenantId);
        }
        return client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private void applyCatalogContextHeaders(HttpRequest.Builder builder) {
        String authorization = resolveCurrentAuthorizationHeader();
        if (StringUtils.hasText(authorization)) {
            builder.header("Authorization", authorization);
        }
        String tenantId = resolveCurrentTenantHeader();
        if (StringUtils.hasText(tenantId)) {
            builder.header("X-Tenant-Id", tenantId);
        }
    }

    private String resolveGatewayApiDocUrl(String resourceUrl) {
        if (!StringUtils.hasText(resourceUrl)) {
            return sanitizeUrl(gatewayUrl);
        }
        if (resourceUrl.startsWith("http://") || resourceUrl.startsWith("https://")) {
            return resourceUrl;
        }
        return sanitizeUrl(gatewayUrl) + (resourceUrl.startsWith("/") ? resourceUrl : "/" + resourceUrl);
    }

    private String resolveDirectApiDocUrl(String resourceUrl) {
        if (!StringUtils.hasText(resourceUrl)) {
            return null;
        }
        String serviceKey = resourceUrl;
        if (serviceKey.startsWith("http://") || serviceKey.startsWith("https://")) {
            try {
                serviceKey = URI.create(serviceKey).getPath();
            } catch (Exception e) {
                return null;
            }
        }
        if (serviceKey.startsWith("/")) {
            serviceKey = serviceKey.substring(1);
        }
        int slashIndex = serviceKey.indexOf('/');
        if (slashIndex <= 0) {
            return null;
        }
        serviceKey = serviceKey.substring(0, slashIndex);
        String baseUrl = apiDocServiceBaseUrls.get(serviceKey);
        if (!StringUtils.hasText(baseUrl)) {
            return null;
        }
        return sanitizeUrl(baseUrl) + "/v2/api-docs";
    }

    private Map<String, String> buildApiDocServiceBaseUrls() {
        Map<String, String> urls = new LinkedHashMap<>();
        urls.put("users", "http://rk-server-rk-user:8082");
        urls.put("auth", "http://rk-server-rk-auth:8081");
        urls.put("activities", "http://rk-server-rk-activity:8090");
        urls.put("content", "http://rk-server-rk-content:8086");
        urls.put("files", "http://rk-server-rk-file:8084");
        urls.put("notifications", "http://rk-server-rk-message:8085");
        urls.put("exam", "http://rk-server-rk-exam:8089");
        urls.put("pay", "http://rk-server-rk-pay:8087");
        urls.put("search", "http://rk-server-rk-search:8083");
        urls.put("trade", "http://rk-server-rk-trade:8088");
        urls.put("data", "http://rk-server-rk-data:8093");
        return urls;
    }

    List<ApiCatalogVO.Endpoint> parseEndpoint(String serviceName, String resourceUrl, JsonNode pathsNode) {
        if (pathsNode == null || !pathsNode.isObject()) {
            return List.of();
        }
        List<ApiCatalogVO.Endpoint> endpoints = new ArrayList<>();
        pathsNode.fields().forEachRemaining(pathEntry -> {
            String path = toGatewayPath(resourceUrl, pathEntry.getKey());
            JsonNode methodsNode = pathEntry.getValue();
            methodsNode.fields().forEachRemaining(methodEntry -> {
                String method = methodEntry.getKey() == null ? "" : methodEntry.getKey().toUpperCase(Locale.ROOT);
                if (!SUPPORTED_METHODS.contains(method)) {
                    return;
                }
                JsonNode operationNode = methodEntry.getValue();
                String category = classifyEndpoint(path);
                endpoints.add(new ApiCatalogVO.Endpoint()
                        .setServiceName(serviceName)
                        .setMethod(method)
                        .setPath(path)
                        .setSummary(operationNode.path("summary").asText(""))
                        .setOperationId(operationNode.path("operationId").asText(""))
                        .setTag(readFirstTag(operationNode.path("tags")))
                        .setCategory(category)
                        .setPermissionPoint(resolvePermissionPoint(category, method, path))
                        .setSampleQueryJson(sampleQueryJson(method))
                        .setSampleBodyJson(sampleBodyJson(method))
                        .setDeprecated(operationNode.path("deprecated").asBoolean(false))
                        .setConsumes(resolveConsumes(operationNode))
                        .setProduces(resolveProduces(operationNode)));
            });
        });
        endpoints.sort(Comparator
                .comparing(ApiCatalogVO.Endpoint::getCategory, Comparator.nullsLast(String::compareTo))
                .thenComparing(ApiCatalogVO.Endpoint::getPath, Comparator.nullsLast(String::compareTo))
                .thenComparing(ApiCatalogVO.Endpoint::getMethod, Comparator.nullsLast(String::compareTo)));
        return endpoints;
    }

    String classifyEndpoint(String path) {
        String value = path == null ? "" : path.toLowerCase(Locale.ROOT);
        if (value.contains("/internal/") || value.endsWith("/internal") || value.contains("/search-documents/internal")) {
            return "INTERNAL";
        }
        if (value.startsWith("/admin/") || value.contains("/admin/") || value.contains("/review/")
                || value.contains("/approval") || value.contains("/manage") || value.contains("/config")) {
            return "ADMIN";
        }
        return "PUBLIC";
    }

    private ApiCatalogVO.Endpoint findDebuggableEndpoint(String method, String path) {
        return getCatalog().getServices().stream()
                .flatMap(service -> service.getEndpoints().stream())
                .filter(endpoint -> method.equals(endpoint.getMethod()) && path.equals(endpoint.getPath()))
                .findFirst()
                .orElse(null);
    }

    private ApiCatalogDebugRequestDTO normalizeDebugRequest(ApiCatalogDebugRequestDTO request) {
        if (request == null) {
            throw new BadRequestException("缺少调试请求");
        }
        String method = request.getMethod() == null ? "GET" : request.getMethod().trim().toUpperCase(Locale.ROOT);
        String path = request.getPath() == null ? "" : request.getPath().trim();
        if (!SUPPORTED_METHODS.contains(method)) {
            throw new BadRequestException("不支持的请求方法");
        }
        if (!StringUtils.hasText(path) || !path.startsWith("/") || path.contains("://") || path.contains("..")) {
            throw new BadRequestException("非法的请求路径");
        }
        return new ApiCatalogDebugRequestDTO()
                .setMethod(method)
                .setPath(path)
                .setQueryJson(request.getQueryJson())
                .setBodyJson(request.getBodyJson());
    }

    private URI buildDebugUri(String path, String queryJson) throws Exception {
        StringBuilder url = new StringBuilder(sanitizeUrl(gatewayUrl)).append(path);
        Map<String, Object> query = parseJsonObject(queryJson);
        if (!query.isEmpty()) {
            String queryString = query.entrySet().stream()
                    .filter(entry -> entry.getValue() != null && StringUtils.hasText(String.valueOf(entry.getValue())))
                    .map(entry -> encode(entry.getKey()) + "=" + encode(String.valueOf(entry.getValue())))
                    .collect(Collectors.joining("&"));
            if (StringUtils.hasText(queryString)) {
                url.append("?").append(queryString);
            }
        }
        return URI.create(url.toString());
    }

    private void applyCurrentContextHeaders(HttpRequest.Builder builder) {
        String authorization = resolveCurrentAuthorizationHeader();
        if (StringUtils.hasText(authorization)) {
            builder.header("Authorization", authorization);
        }
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            builder.header("X-Tenant-Id", String.valueOf(tenantId));
        }
        builder.header("Accept", "application/json");
    }

    private void applyDebugMethod(HttpRequest.Builder builder, ApiCatalogDebugRequestDTO request) throws Exception {
        String method = request.getMethod();
        if ("GET".equals(method) || "DELETE".equals(method)) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
            return;
        }
        String body = "{}";
        if (StringUtils.hasText(request.getBodyJson())) {
            objectMapper.readTree(request.getBodyJson());
            body = request.getBodyJson();
        }
        builder.header("Content-Type", "application/json");
        builder.method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
    }

    private Map<String, Object> parseJsonObject(String json) throws Exception {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        JsonNode node = objectMapper.readTree(json);
        if (!node.isObject()) {
            throw new BadRequestException("Query JSON 必须是对象");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> result.put(entry.getKey(), entry.getValue().isValueNode() ? entry.getValue().asText() : entry.getValue().toString()));
        return result;
    }

    private String resolveBasePath(JsonNode root) {
        String basePath = root.path("basePath").asText("");
        if (StringUtils.hasText(basePath)) {
            return basePath;
        }
        JsonNode serversNode = root.path("servers");
        if (serversNode.isArray() && !serversNode.isEmpty()) {
            String url = serversNode.get(0).path("url").asText("");
            if (StringUtils.hasText(url)) {
                try {
                    String path = URI.create(url).getPath();
                    return StringUtils.hasText(path) ? path : "/";
                } catch (Exception ignored) {
                    return "/";
                }
            }
        }
        return "/";
    }

    private String toGatewayPath(String resourceUrl, String path) {
        if (!StringUtils.hasText(path)) {
            return path;
        }
        if (StringUtils.hasText(resourceUrl)
                && resourceUrl.startsWith("/notifications/")
                && !path.startsWith("/notifications/")
                && !path.startsWith("/api/contact")
                && !path.startsWith("/api/email/")
                && !path.startsWith("/api/email-templates")) {
            return "/notifications" + (path.startsWith("/") ? path : "/" + path);
        }
        return path;
    }

    private String readFirstTag(JsonNode tagsNode) {
        if (tagsNode != null && tagsNode.isArray() && !tagsNode.isEmpty()) {
            return tagsNode.get(0).asText("");
        }
        return "";
    }

    private List<String> resolveConsumes(JsonNode operationNode) {
        List<String> consumes = readStringArray(operationNode.path("consumes"));
        if (!consumes.isEmpty()) {
            return consumes;
        }
        return readFieldNames(operationNode.path("requestBody").path("content"));
    }

    private List<String> resolveProduces(JsonNode operationNode) {
        List<String> produces = readStringArray(operationNode.path("produces"));
        if (!produces.isEmpty()) {
            return produces;
        }
        JsonNode responsesNode = operationNode.path("responses");
        if (!responsesNode.isObject()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        responsesNode.fields().forEachRemaining(entry -> values.addAll(readFieldNames(entry.getValue().path("content"))));
        return values.stream().distinct().collect(Collectors.toList());
    }

    private List<String> readStringArray(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : arrayNode) {
            String value = item.asText("");
            if (StringUtils.hasText(value)) {
                values.add(value);
            }
        }
        return values;
    }

    private List<String> readFieldNames(JsonNode objectNode) {
        if (objectNode == null || !objectNode.isObject()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        objectNode.fieldNames().forEachRemaining(name -> {
            if (StringUtils.hasText(name)) {
                values.add(name);
            }
        });
        return values;
    }

    private String resolvePermissionPoint(String category, String method, String path) {
        if ("PUBLIC".equals(category)) {
            return "公开接口";
        }
        String action = "view";
        if ("POST".equals(method)) {
            action = "create";
        } else if ("PUT".equals(method) || "PATCH".equals(method)) {
            action = "edit";
        } else if ("DELETE".equals(method)) {
            action = "delete";
        }
        String normalized = path == null ? "api" : path.replaceAll("\\{[^}]+}", "")
                .replaceAll("[^a-zA-Z0-9]+", ":")
                .replaceAll("^:+|:+$", "")
                .toLowerCase(Locale.ROOT);
        return normalized + ":" + action;
    }

    private String sampleQueryJson(String method) {
        return "GET".equals(method) ? "{\"page\":1,\"pageSize\":10}" : "{}";
    }

    private String sampleBodyJson(String method) {
        return ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) ? "{}" : "";
    }

    private String resolveCurrentAuthorizationHeader() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes)) {
            return null;
        }
        HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
        if (request == null) {
            return null;
        }
        String authorization = request.getHeader("Authorization");
        return StringUtils.hasText(authorization) ? authorization : null;
    }

    private String resolveCurrentTenantHeader() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return String.valueOf(tenantId);
        }
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes)) {
            return null;
        }
        HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
        if (request == null) {
            return null;
        }
        String tenantHeader = request.getHeader("X-Tenant-Id");
        return StringUtils.hasText(tenantHeader) ? tenantHeader : null;
    }

    private String resolveTenantScope() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? "ALL" : String.valueOf(tenantId);
    }

    private Map<String, String> flattenResponseHeaders(Map<String, List<String>> headers) {
        Map<String, String> result = new LinkedHashMap<>();
        headers.forEach((key, values) -> {
            if (StringUtils.hasText(key) && values != null && !values.isEmpty()) {
                result.put(key, String.join(",", values));
            }
        });
        return result;
    }

    private String limitBody(String body) {
        if (body == null || body.length() <= MAX_DEBUG_BODY_CHARS) {
            return body;
        }
        return body.substring(0, MAX_DEBUG_BODY_CHARS) + "\n...响应内容过长，已截断";
    }

    private String sanitizeUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
