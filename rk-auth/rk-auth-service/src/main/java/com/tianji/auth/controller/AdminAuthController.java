package com.tianji.auth.controller;

import com.tianji.auth.service.IAdminAuthService;
import com.tianji.common.domain.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员认证控制器
 * 基于master分支的AdmissionController管理员认证功能
 */
@Slf4j
@Api(tags = "管理员认证接口")
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final IAdminAuthService adminAuthService;

    /**
     * 管理员身份验证
     */
    @ApiOperation("管理员身份验证")
    @PostMapping("/authenticate")
    public R<Map<String, Object>> authenticateAdmin(@RequestBody Map<String, String> request) {
        try {
            String studentId = request.get("studentId");
            String email = request.get("key");
            String key = request.get("key");

            if (studentId == null || studentId.trim().isEmpty()) {
                return R.error("学号不能为空");
            }
            if (email == null || email.trim().isEmpty()) {
                return R.error("邮箱不能为空");
            }
            if (key == null || key.trim().isEmpty()) {
                return R.error("管理员密钥不能为空");
            }

            Map<String, Object> result = adminAuthService.authenticateAdmin(studentId, email, key);

            if (Boolean.TRUE.equals(result.get("success"))) {
                return R.ok(result);
            } else {
                return R.error((String) result.get("message"));
            }
        } catch (Exception e) {
            log.error("管理员身份验证失败", e);
            return R.error("管理员身份验证失败: " + e.getMessage());
        }
    }

    /**
     * 验证管理员会话
     */
    @ApiOperation("验证管理员会话")
    @PostMapping("/verify")
    public R<Boolean> verifyAdminSession(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            if (token == null || token.trim().isEmpty()) {
                return R.error("会话令牌不能为空");
            }

            boolean isValid = adminAuthService.verifyAdminSession(token);
            return R.ok(isValid);
        } catch (Exception e) {
            log.error("验证管理员会话失败", e);
            return R.error("验证管理员会话失败: " + e.getMessage());
        }
    }

    /**
     * 获取管理员信息
     */
    @ApiOperation("获取管理员信息")
    @GetMapping("/info")
    public R<Map<String, Object>> getAdminInfo(@RequestParam String studentId, @RequestParam String email) {
        try {
            if (studentId == null || studentId.trim().isEmpty()) {
                return R.error("学号不能为空");
            }
            if (email == null || email.trim().isEmpty()) {
                return R.error("邮箱不能为空");
            }

            Map<String, Object> adminInfo = adminAuthService.getAdminInfo(studentId, email);
            return R.ok(adminInfo);
        } catch (Exception e) {
            log.error("获取管理员信息失败", e);
            return R.error("获取管理员信息失败: " + e.getMessage());
        }
    }

    /**
     * 使会话令牌失效
     */
    @ApiOperation("使会话令牌失效")
    @PostMapping("/logout")
    public R<String> invalidateSessionToken(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            if (token == null || token.trim().isEmpty()) {
                return R.error("会话令牌不能为空");
            }

            adminAuthService.invalidateSessionToken(token);
            log.info("✅ 管理员登出成功");
            return R.ok("登出成功");
        } catch (Exception e) {
            log.error("管理员登出失败", e);
            return R.error("管理员登出失败: " + e.getMessage());
        }
    }

    /**
     * 验证管理员密钥
     */
    @ApiOperation("验证管理员密钥")
    @PostMapping("/validate-key")
    public R<Boolean> validateAdminKey(@RequestBody Map<String, String> request) {
        try {
            String key = request.get("key");
            if (key == null || key.trim().isEmpty()) {
                return R.error("管理员密钥不能为空");
            }

            boolean isValid = adminAuthService.validateAdminKey(key);
            return R.ok(isValid);
        } catch (Exception e) {
            log.error("验证管理员密钥失败", e);
            return R.error("验证管理员密钥失败: " + e.getMessage());
        }
    }
}