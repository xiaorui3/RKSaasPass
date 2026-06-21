package com.tianji.user.controller;

import com.tianji.common.domain.R;
import com.tianji.user.service.IConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 配置接口控制器
 * 提供导航配置、系统配置等接口
 * 
 * 已连接真实数据库，支持多租户隔离
 */
@Slf4j
@Api(tags = "配置接口")
@RestController
@RequestMapping({"/config", "/api/config"})
@RequiredArgsConstructor
public class ConfigController {

    private final IConfigService configService;

    /**
     * 获取导航配置
     */
    @ApiOperation("获取导航配置")
    @GetMapping("/navigation")
    public R<List<Map<String, Object>>> getNavigation() {
        try {
            List<Map<String, Object>> navigation = configService.getNavigationTree();
            return R.ok(navigation);
        } catch (Exception e) {
            log.error("获取导航配置失败", e);
            return R.error("获取导航配置失败：" + e.getMessage());
        }
    }

    /**
     * 获取系统配置
     */
    @ApiOperation("获取系统配置")
    @GetMapping("/system")
    public R<Map<String, Object>> getSystemConfig() {
        try {
            Map<String, Object> config = configService.getSystemConfig();
            return R.ok(config);
        } catch (Exception e) {
            log.error("获取系统配置失败", e);
            return R.error("获取系统配置失败：" + e.getMessage());
        }
    }

    /**
     * 更新系统配置
     */
    @ApiOperation("更新系统配置")
    @PutMapping("/system/{configKey}")
    public R<String> updateSystemConfig(
            @PathVariable String configKey,
            @RequestBody String configValue) {
        try {
            boolean success = configService.updateConfig(configKey, configValue);
            if (success) {
                return R.ok("更新成功");
            } else {
                return R.error("更新失败");
            }
        } catch (Exception e) {
            log.error("更新系统配置失败", e);
            return R.error("更新系统配置失败：" + e.getMessage());
        }
    }
}
