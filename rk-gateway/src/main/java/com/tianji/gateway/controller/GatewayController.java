package com.tianji.gateway.controller;

import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * RK-Gateway 通用接口
 * 提供基础功能和状态检查
 */
@RestController
@RequestMapping("/api/v1")
public class GatewayController {

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", "rk-gateway");
        result.put("timestamp", LocalDateTime.now());
        result.put("message", "Gateway is running");
        return result;
    }

    /**
     * 获取网关配置信息
     */
    @GetMapping("/config/navigation")
    public Map<String, Object> getNavigation() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", getNavigationData());
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /**
     * 获取系统配置
     */
    @GetMapping("/config/system")
    public Map<String, Object> getSystemConfig() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        
        Map<String, Object> config = new HashMap<>();
        config.put("systemName", "RK社团管理平台");
        config.put("version", "1.0.0");
        config.put("multiTenant", true);
        config.put("features", new String[]{
            "用户管理", "新闻管理", "活动管理", 
            "比赛管理", "文件管理", "消息通知"
        });
        
        result.put("data", config);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /**
     * 导航数据（示例）
     */
    private Map<String, Object> getNavigationData() {
        Map<String, Object> navigation = new HashMap<>();
        navigation.put("home", "/home");
        navigation.put("news", "/news");
        navigation.put("activities", "/activities");
        navigation.put("competitions", "/competitions");
        navigation.put("about", "/about");
        navigation.put("contact", "/contact");
        return navigation;
    }

    /**
     * 统一响应格式
     */
    @Data
    public static class ApiResponse<T> {
        private Integer code;
        private String message;
        private T data;
        private Long timestamp;

        public static <T> ApiResponse<T> success(T data) {
            ApiResponse<T> response = new ApiResponse<>();
            response.setCode(200);
            response.setMessage("success");
            response.setData(data);
            response.setTimestamp(System.currentTimeMillis());
            return response;
        }

        public static <T> ApiResponse<T> error(String message) {
            ApiResponse<T> response = new ApiResponse<>();
            response.setCode(500);
            response.setMessage(message);
            response.setTimestamp(System.currentTimeMillis());
            return response;
        }
    }
}