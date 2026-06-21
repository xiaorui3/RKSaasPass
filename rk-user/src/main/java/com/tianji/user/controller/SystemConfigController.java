package com.tianji.user.controller;

import com.tianji.common.domain.R;
import com.tianji.user.domain.dto.SystemConfigDTO;
import com.tianji.user.domain.vo.SystemConfigVO;
import com.tianji.user.service.ISystemConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统配置管理控制器（/admin/config API）
 * 仅超级管理员（role_id=1）可访问
 *
 * @author RK-Web
 * @since 2026-03-29
 */
@Slf4j
@Api(tags = "系统配置管理-管理员专用")
@RestController
@RequestMapping("/admin/config")
@RequiredArgsConstructor
public class SystemConfigController {

    private final ISystemConfigService systemConfigService;

    /**
     * 获取系统配置列表（超级管理员专用）
     */
    @ApiOperation("获取系统配置列表")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('system:config:list')")
    public R<List<SystemConfigVO>> getConfigList() {
        log.info("管理员获取系统配置列表");
        List<SystemConfigVO> configs = systemConfigService.getConfigList();
        return R.ok(configs);
    }

    /**
     * 根据配置键获取配置详情（超级管理员专用）
     */
    @ApiOperation("根据配置键获取配置详情")
    @GetMapping("/key/{configKey}")
    @PreAuthorize("hasAuthority('system:config:query')")
    public R<SystemConfigVO> getConfigByKey(
            @ApiParam("配置键") @PathVariable String configKey) {
        log.info("管理员获取配置详情: {}", configKey);
        SystemConfigVO config = systemConfigService.getConfigByKey(configKey);
        if (config == null) {
            return R.error("配置不存在: " + configKey);
        }
        return R.ok(config);
    }

    /**
     * 根据ID获取配置详情（超级管理员专用）
     */
    @ApiOperation("根据ID获取配置详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:config:query')")
    public R<SystemConfigVO> getConfigById(
            @ApiParam("配置ID") @PathVariable Long id) {
        log.info("管理员获取配置详情: id={}", id);
        SystemConfigVO config = systemConfigService.getById(id);
        if (config == null) {
            return R.error("配置不存在: " + id);
        }
        return R.ok(config);
    }

    /**
     * 创建系统配置（超级管理员专用）
     */
    @ApiOperation("创建系统配置")
    @PostMapping
    @PreAuthorize("hasAuthority('system:config:add')")
    public R<SystemConfigVO> createConfig(@Validated @RequestBody SystemConfigDTO dto) {
        log.info("管理员创建系统配置: key={}", dto.getConfigKey());
        try {
            Long configId = systemConfigService.createConfig(dto);
            SystemConfigVO config = systemConfigService.getById(configId);
            return R.ok(config);
        } catch (RuntimeException e) {
            log.error("创建系统配置失败: {}", e.getMessage());
            return R.error(e.getMessage());
        }
    }

    /**
     * 更新系统配置（超级管理员专用）
     */
    @ApiOperation("更新系统配置")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:config:edit')")
    public R<Void> updateConfig(
            @ApiParam("配置ID") @PathVariable Long id,
            @Validated @RequestBody SystemConfigDTO dto) {
        log.info("管理员更新系统配置: id={}, key={}", id, dto.getConfigKey());
        try {
            boolean success = systemConfigService.updateConfig(id, dto);
            return success ? R.ok() : R.error("更新失败");
        } catch (RuntimeException e) {
            log.error("更新系统配置失败: {}", e.getMessage());
            return R.error(e.getMessage());
        }
    }

    /**
     * 删除系统配置（超级管理员专用）
     */
    @ApiOperation("删除系统配置")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:config:remove')")
    public R<Void> deleteConfig(@ApiParam("配置ID") @PathVariable Long id) {
        log.info("管理员删除系统配置: id={}", id);
        try {
            boolean success = systemConfigService.deleteConfig(id);
            return success ? R.ok() : R.error("删除失败");
        } catch (RuntimeException e) {
            log.error("删除系统配置失败: {}", e.getMessage());
            return R.error(e.getMessage());
        }
    }

    /**
     * 切换配置启用状态（超级管理员专用）
     */
    @ApiOperation("切换配置启用状态")
    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('system:config:edit')")
    public R<Void> toggleConfigStatus(@ApiParam("配置ID") @PathVariable Long id) {
        log.info("管理员切换配置状态: id={}", id);
        try {
            boolean success = systemConfigService.toggleConfigStatus(id);
            return success ? R.ok() : R.error("切换失败");
        } catch (RuntimeException e) {
            log.error("切换配置状态失败: {}", e.getMessage());
            return R.error(e.getMessage());
        }
    }

    /**
     * 获取配置值（超级管理员专用）
     */
    @ApiOperation("获取配置值")
    @GetMapping("/value/{configKey}")
    @PreAuthorize("hasAuthority('system:config:query')")
    public R<String> getConfigValue(
            @ApiParam("配置键") @PathVariable String configKey) {
        log.info("管理员获取配置值: {}", configKey);
        String value = systemConfigService.getConfigValue(configKey);
        if (value == null) {
            return R.error("配置不存在: " + configKey);
        }
        return R.ok(value);
    }
}
