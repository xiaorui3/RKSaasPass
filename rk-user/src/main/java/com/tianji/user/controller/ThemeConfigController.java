package com.tianji.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.common.domain.R;
import com.tianji.common.utils.TenantContext;
import com.tianji.user.domain.dto.TenantThemeConfigDTO;
import com.tianji.user.domain.po.SystemConfig;
import com.tianji.user.mapper.SystemConfigMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Api(tags = "租户主题配置")
@RestController
@RequestMapping("/api/config/theme")
@RequiredArgsConstructor
public class ThemeConfigController {

    private static final String CONFIG_KEY = "tenant.theme.config";
    private static final String STYLE_PORTAL = "portal";
    private static final String STYLE_CLASSIC = "classic";
    private static final String STYLE_BIP = "bip";

    private final SystemConfigMapper systemConfigMapper;
    private final ObjectMapper objectMapper;

    @ApiOperation("获取当前租户主题配置")
    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    public R<TenantThemeConfigDTO> getCurrent() {
        return R.ok(loadConfig(resolveTenantId()));
    }

    @ApiOperation("保存当前租户主题配置")
    @PutMapping("/current")
    @PreAuthorize("hasAuthority('system:config:edit')")
    public R<TenantThemeConfigDTO> saveCurrent(@RequestBody TenantThemeConfigDTO dto) {
        Long tenantId = resolveTenantId();
        SystemConfig config = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getTenantId, tenantId)
                .eq(SystemConfig::getConfigKey, CONFIG_KEY)
                .last("LIMIT 1"));

        if (config == null) {
            config = new SystemConfig();
            config.setTenantId(tenantId);
            config.setConfigKey(CONFIG_KEY);
            config.setDescription("tenant theme config");
            config.setIsEnabled(true);
            config.setIsDeleted(false);
            config.setCreateTime(LocalDateTime.now());
        }
        config.setConfigValue(toJson(normalize(dto)));
        config.setUpdateTime(LocalDateTime.now());

        if (config.getId() == null) {
            systemConfigMapper.insert(config);
        } else {
            systemConfigMapper.updateById(config);
        }
        return R.ok(loadConfig(tenantId));
    }

    @ApiOperation("公开获取指定租户主题配置")
    @GetMapping("/public")
    public R<TenantThemeConfigDTO> getPublic(@RequestParam(value = "tenantId", required = false) Long tenantId) {
        return R.ok(loadConfig(tenantId == null ? resolveTenantId() : tenantId));
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    private TenantThemeConfigDTO loadConfig(Long tenantId) {
        SystemConfig config = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getTenantId, tenantId)
                .eq(SystemConfig::getConfigKey, CONFIG_KEY)
                .last("LIMIT 1"));
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()) {
            return defaultConfig();
        }
        try {
            return normalize(objectMapper.readValue(config.getConfigValue(), TenantThemeConfigDTO.class));
        } catch (JsonProcessingException e) {
            return defaultConfig();
        }
    }

    private TenantThemeConfigDTO normalize(TenantThemeConfigDTO dto) {
        TenantThemeConfigDTO config = dto == null ? new TenantThemeConfigDTO() : dto;
        if (config.getFrontendTheme() == null || config.getFrontendTheme().isBlank()) {
            config.setFrontendTheme("default");
        }
        if (config.getAdminTheme() == null || config.getAdminTheme().isBlank()) {
            config.setAdminTheme("default");
        }
        config.setFrontendStyle(normalizeFrontendStyle(config.getFrontendStyle()));
        config.setAdminStyle(normalizeAdminStyle(config.getAdminStyle()));
        if (config.getFrontendBgOpacity() == null) {
            config.setFrontendBgOpacity(15);
        }
        if (config.getAdminBgOpacity() == null) {
            config.setAdminBgOpacity(15);
        }
        return config;
    }

    private TenantThemeConfigDTO defaultConfig() {
        TenantThemeConfigDTO dto = new TenantThemeConfigDTO();
        dto.setFrontendTheme("default");
        dto.setAdminTheme("default");
        dto.setFrontendStyle(STYLE_PORTAL);
        dto.setAdminStyle(STYLE_CLASSIC);
        dto.setFrontendBgOpacity(15);
        dto.setAdminBgOpacity(15);
        return dto;
    }

    private String normalizeFrontendStyle(String style) {
        if (STYLE_PORTAL.equalsIgnoreCase(style)) {
            return STYLE_PORTAL;
        }
        if (STYLE_BIP.equalsIgnoreCase(style)) {
            return STYLE_BIP;
        }
        if (STYLE_CLASSIC.equalsIgnoreCase(style)) {
            return STYLE_CLASSIC;
        }
        return STYLE_PORTAL;
    }

    private String normalizeAdminStyle(String style) {
        if (STYLE_BIP.equalsIgnoreCase(style)) {
            return STYLE_BIP;
        }
        return STYLE_CLASSIC;
    }

    private String toJson(TenantThemeConfigDTO dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("tenant theme config serialize failed", e);
        }
    }
}
