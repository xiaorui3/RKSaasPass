package com.tianji.user.controller;

import com.tianji.common.domain.R;
import com.tianji.user.domain.dto.AdminLoginDTO;
import com.tianji.user.domain.vo.AdminVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 管理员认证接口控制器
 * 提供管理员登录、验证、登出等接口
 */
@Slf4j
@Api(tags = "管理员认证接口")
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    // 管理员密钥（实际应该从配置或数据库读取）
    private static final String ADMIN_KEY = "rk-admin-2024-secret-key";

    /**
     * 管理员身份认证
     */
    @ApiOperation("管理员身份认证")
    @PostMapping("/authenticate")
    public R<AdminVO> authenticateAdmin(@Validated @RequestBody AdminLoginDTO dto) {
        try {
            // 验证管理员密钥
            if (dto.getAdminKey() != null && !dto.getAdminKey().equals(ADMIN_KEY)) {
                log.warn("管理员密钥验证失败: {}", dto.getUsername());
                return R.error("管理员密钥无效");
            }

            // 模拟认证（实际应该调用认证服务）
            // 这里简化处理，实际需要验证用户名密码
            if (!"admin".equals(dto.getUsername())) {
                return R.error("用户名或密码错误");
            }

            AdminVO adminVO = new AdminVO();
            adminVO.setId(1L);
            adminVO.setUsername(dto.getUsername());
            adminVO.setNickname("系统管理员");
            adminVO.setAvatar("/assets/default-avatar.png");
            adminVO.setEmail("admin@rkweb.com");
            adminVO.setMobile("13800138000");
            adminVO.setRoles(Arrays.asList("admin", "super_admin"));
            adminVO.setPermissions(Arrays.asList("*:*:*"));
            adminVO.setLastLoginTime(LocalDateTime.now());
            adminVO.setToken(UUID.randomUUID().toString().replace("-", ""));
            adminVO.setTokenExpireTime(System.currentTimeMillis() + 7200000); // 2小时

            log.info("✅ 管理员认证成功: {}", dto.getUsername());
            return R.ok(adminVO);
        } catch (Exception e) {
            log.error("管理员认证失败", e);
            return R.error("认证失败：" + e.getMessage());
        }
    }

    /**
     * 验证管理员会话
     */
    @ApiOperation("验证管理员会话")
    @PostMapping("/verify")
    public R<Map<String, Object>> verifyAdminSession(@RequestBody Map<String, String> body) {
        try {
            String token = body.get("token");
            if (token == null || token.isEmpty()) {
                return R.error("Token不能为空");
            }

            // 模拟验证（实际应该调用认证服务验证Token）
            Map<String, Object> result = new HashMap<>();
            result.put("valid", true);
            result.put("userId", 1L);
            result.put("username", "admin");
            result.put("roles", Arrays.asList("admin", "super_admin"));
            result.put("expireTime", System.currentTimeMillis() + 7200000);

            return R.ok(result);
        } catch (Exception e) {
            log.error("验证管理员会话失败", e);
            return R.error("验证失败：" + e.getMessage());
        }
    }

    /**
     * 获取管理员信息
     */
    @ApiOperation("获取管理员信息")
    @GetMapping("/info")
    public R<AdminVO> getAdminInfo() {
        try {
            // 模拟获取管理员信息（实际应该从认证上下文获取）
            AdminVO adminVO = new AdminVO();
            adminVO.setId(1L);
            adminVO.setUsername("admin");
            adminVO.setNickname("系统管理员");
            adminVO.setAvatar("/assets/default-avatar.png");
            adminVO.setEmail("admin@rkweb.com");
            adminVO.setMobile("13800138000");
            adminVO.setRoles(Arrays.asList("admin", "super_admin"));
            adminVO.setPermissions(Arrays.asList("*:*:*"));
            adminVO.setLastLoginTime(LocalDateTime.now());

            return R.ok(adminVO);
        } catch (Exception e) {
            log.error("获取管理员信息失败", e);
            return R.error("获取管理员信息失败：" + e.getMessage());
        }
    }

    /**
     * 管理员登出
     */
    @ApiOperation("管理员登出")
    @PostMapping("/logout")
    public R<String> adminLogout(@RequestBody Map<String, String> body) {
        try {
            String token = body.get("token");
            log.info("✅ 管理员登出成功, token: {}", token);
            return R.ok("登出成功");
        } catch (Exception e) {
            log.error("管理员登出失败", e);
            return R.error("登出失败：" + e.getMessage());
        }
    }

    /**
     * 验证管理员密钥
     */
    @ApiOperation("验证管理员密钥")
    @PostMapping("/validate-key")
    public R<Map<String, Object>> validateAdminKey(@RequestBody Map<String, String> body) {
        try {
            String adminKey = body.get("adminKey");
            if (adminKey == null || adminKey.isEmpty()) {
                return R.error("管理员密钥不能为空");
            }

            Map<String, Object> result = new HashMap<>();
            if (adminKey.equals(ADMIN_KEY)) {
                result.put("valid", true);
                result.put("message", "管理员密钥验证成功");
                log.info("✅ 管理员密钥验证成功");
            } else {
                result.put("valid", false);
                result.put("message", "管理员密钥无效");
                log.warn("⚠️ 管理员密钥验证失败");
            }

            return R.ok(result);
        } catch (Exception e) {
            log.error("验证管理员密钥失败", e);
            return R.error("验证失败：" + e.getMessage());
        }
    }
}
