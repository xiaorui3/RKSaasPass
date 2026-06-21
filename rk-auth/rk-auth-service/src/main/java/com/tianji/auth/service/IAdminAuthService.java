package com.tianji.auth.service;

import java.util.Map;

/**
 * 管理员认证服务接口
 * 基于master分支的AdmissionController管理员认证功能
 */
public interface IAdminAuthService {

    // 验证管理员密钥
    boolean validateAdminKey(String key);

    // 生成会话令牌
    String generateSessionToken(String studentId, String email);

    // 验证会话令牌
    boolean isValidSessionToken(String token);

    // 验证管理员身份
    Map<String, Object> authenticateAdmin(String studentId, String email, String key);

    // 验证管理员会话
    boolean verifyAdminSession(String token);

    // 使会话令牌失效
    void invalidateSessionToken(String token);

    // 获取管理员信息
    Map<String, Object> getAdminInfo(String studentId, String email);
}