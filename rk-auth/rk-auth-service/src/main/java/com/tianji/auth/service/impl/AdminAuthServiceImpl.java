package com.tianji.auth.service.impl;

import com.tianji.auth.service.IAdminAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 管理员认证服务实现类
 * 基于master分支的AdmissionController管理员认证功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements IAdminAuthService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ADMIN_KEY_PREFIX = "admin:key:";
    private static final String SESSION_TOKEN_PREFIX = "admin:session:";
    private static final String ADMIN_INFO_PREFIX = "admin:info:";
    private static final long SESSION_EXPIRE_HOURS = 24;

    // 预设管理员密钥（可以从配置文件或数据库读取）
    private static final String VALID_ADMIN_KEY = "RK_ADMIN_2025_SECURE_KEY";

    // 预设管理员邮箱列表
    private static final Map<String, String> ADMIN_USERS = Map.of(
            "3505469466@qq.com", "admin_001",
            "ruimeilademaye@163.com", "admin_002",
            "2148906016@qq.com", "admin_003",
            "lxy521521456@163.com", "admin_004",
            "w87027619332@163.com", "admin_005"
    );

    @Override
    public boolean validateAdminKey(String key) {
        boolean isValid = VALID_ADMIN_KEY.equals(key);
        if (isValid) {
            log.info("✅ 管理员密钥验证成功");
        } else {
            log.warn("管理员密钥验证失败");
        }
        return isValid;
    }

    @Override
    public String generateSessionToken(String studentId, String email) {
        try {
            // 生成随机令牌
            SecureRandom random = new SecureRandom();
            byte[] tokenBytes = new byte[32];
            random.nextBytes(tokenBytes);
            String token = Base64.getEncoder().encodeToString(tokenBytes);

            // 生成会话数据
            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("studentId", studentId);
            sessionData.put("email", email);
            sessionData.put("createTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            sessionData.put("adminId", ADMIN_USERS.get(email));

            // 存储会话信息
            String sessionKey = SESSION_TOKEN_PREFIX + token;
            redisTemplate.opsForValue().set(sessionKey, sessionData, SESSION_EXPIRE_HOURS, TimeUnit.HOURS);

            // 存储用户当前会话（单点登录）
            String userSessionKey = ADMIN_INFO_PREFIX + studentId + ":" + email;
            redisTemplate.opsForValue().set(userSessionKey, token, SESSION_EXPIRE_HOURS, TimeUnit.HOURS);

            log.info("✅ 管理员会话令牌生成成功：学号={}, 邮箱={}", studentId, email);
            return token;
        } catch (Exception e) {
            log.error("生成会话令牌失败", e);
            throw new RuntimeException("生成会话令牌失败", e);
        }
    }

    @Override
    public boolean isValidSessionToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return false;
            }

            String sessionKey = SESSION_TOKEN_PREFIX + token;
            Object sessionData = redisTemplate.opsForValue().get(sessionKey);

            boolean isValid = sessionData != null;
            if (!isValid) {
                log.warn("会话令牌无效或已过期");
            }

            return isValid;
        } catch (Exception e) {
            log.error("验证会话令牌失败", e);
            return false;
        }
    }

    @Override
    public Map<String, Object> authenticateAdmin(String studentId, String email, String key) {
        try {
            Map<String, Object> result = new HashMap<>();

            // 验证密钥
            if (!validateAdminKey(key)) {
                result.put("success", false);
                result.put("message", "管理员密钥错误");
                return result;
            }

            // 验证用户是否是管理员
            if (!ADMIN_USERS.containsKey(email)) {
                result.put("success", false);
                result.put("message", "您没有管理员权限");
                return result;
            }

            // 生成会话令牌
            String token = generateSessionToken(studentId, email);

            result.put("success", true);
            result.put("message", "管理员身份验证成功");
            result.put("token", token);
            result.put("redirectUrl", "/2025/admin.html");
            result.put("adminId", ADMIN_USERS.get(email));

            log.info("✅ 管理员身份验证成功：学号={}, 邮箱={}", studentId, email);
            return result;

        } catch (Exception e) {
            log.error("管理员身份验证失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "验证失败: " + e.getMessage());
            return result;
        }
    }

    @Override
    public boolean verifyAdminSession(String token) {
        return isValidSessionToken(token);
    }

    @Override
    public void invalidateSessionToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return;
            }

            String sessionKey = SESSION_TOKEN_PREFIX + token;
            Object sessionData = redisTemplate.opsForValue().get(sessionKey);

            if (sessionData instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) sessionData;
                String studentId = (String) data.get("studentId");
                String email = (String) data.get("email");

                // 删除会话令牌
                redisTemplate.delete(sessionKey);

                // 删除用户当前会话
                String userSessionKey = ADMIN_INFO_PREFIX + studentId + ":" + email;
                redisTemplate.delete(userSessionKey);

                log.info("✅ 管理员会话令牌已失效：学号={}", studentId);
            }
        } catch (Exception e) {
            log.error("使会话令牌失效失败", e);
        }
    }

    @Override
    public Map<String, Object> getAdminInfo(String studentId, String email) {
        try {
            String userSessionKey = ADMIN_INFO_PREFIX + studentId + ":" + email;
            Object token = redisTemplate.opsForValue().get(userSessionKey);

            Map<String, Object> adminInfo = new HashMap<>();
            adminInfo.put("studentId", studentId);
            adminInfo.put("email", email);
            adminInfo.put("adminId", ADMIN_USERS.get(email));
            adminInfo.put("isAdmin", ADMIN_USERS.containsKey(email));
            adminInfo.put("hasActiveSession", token != null);

            return adminInfo;
        } catch (Exception e) {
            log.error("获取管理员信息失败", e);
            return new HashMap<>();
        }
    }
}