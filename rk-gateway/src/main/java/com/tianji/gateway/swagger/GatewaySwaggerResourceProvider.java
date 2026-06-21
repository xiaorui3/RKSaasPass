package com.tianji.gateway.swagger;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import springfox.documentation.swagger.web.SwaggerResource;
import springfox.documentation.swagger.web.SwaggerResourcesProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Primary
@RequiredArgsConstructor
public class GatewaySwaggerResourceProvider implements SwaggerResourcesProvider {

    /**
     * swagger2默认的url后缀
     */
    private static final String SWAGGER2_URL = "/v2/api-docs";

    /**
     * 路由定位器
     */
    private final RouteLocator routeLocator;

    /**
     * 网关应用名称
     */
    @Value("${spring.application.name}")
    private String gatewayName;

    /**
     * 服务名到路径前缀的映射
     */
    private static final Map<String, String> SERVICE_PATH_MAP = new HashMap<>();
    static {
        SERVICE_PATH_MAP.put("rk-auth", "auth");
        SERVICE_PATH_MAP.put("rk-user", "users");
        SERVICE_PATH_MAP.put("rk-content", "content");
        SERVICE_PATH_MAP.put("rk-activity", "activities");
        SERVICE_PATH_MAP.put("rk-file", "files");
        SERVICE_PATH_MAP.put("rk-message", "notifications");
        SERVICE_PATH_MAP.put("rk-exam", "exam");
        SERVICE_PATH_MAP.put("rk-pay", "pay");
        SERVICE_PATH_MAP.put("rk-search", "search");
        SERVICE_PATH_MAP.put("rk-trade", "trade");
        SERVICE_PATH_MAP.put("rk-data", "data");
    }

    /**
     * 获取 Swagger 资源
     */
    @Override
    public List<SwaggerResource> get() {
        List<SwaggerResource> resources = new ArrayList<>();
        Map<String, String> servers = new HashMap<>();
        // 1.获取路由 Uri中的 Host 作为服务名，把路由id作为请求路径，这里要确保路由id与路由path前缀一致
        routeLocator.getRoutes()
                .filter(route -> route.getUri().getHost() != null)
                .filter(route -> !gatewayName.equals(route.getUri().getHost()))
                .subscribe( r -> servers.put(r.getUri().getHost(), r.getId()));
        // 2.创建自定义资源
        servers.forEach((name, path) -> {
            // 创建Swagger 资源
            SwaggerResource swaggerResource = new SwaggerResource();
            // 使用服务名映射到路径前缀
            String pathPrefix = SERVICE_PATH_MAP.getOrDefault(name, path);
            // 设置访问地址
            swaggerResource.setUrl("/" + pathPrefix + SWAGGER2_URL);
            // 设置名称
            swaggerResource.setName(name);
            swaggerResource.setSwaggerVersion("3.0.0");
            resources.add(swaggerResource);
        });
        return resources;
    }
}