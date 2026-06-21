package com.tianji.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.common.domain.R;
import com.tianji.common.utils.TenantContext;
import com.tianji.user.domain.dto.ClubProfileConfigDTO;
import com.tianji.user.domain.dto.ClubProfileSectionItemDTO;
import com.tianji.user.domain.po.SystemConfig;
import com.tianji.user.mapper.SystemConfigMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Api(tags = "社团概况配置")
@RestController
@RequestMapping("/api/config/club-profile")
@RequiredArgsConstructor
public class ClubProfileConfigController {

    private static final String CONFIG_KEY = "club.profile.config";

    private final SystemConfigMapper systemConfigMapper;
    private final ObjectMapper objectMapper;

    @ApiOperation("获取当前租户社团概况配置")
    @GetMapping("/current")
    @PreAuthorize("hasAnyAuthority('content:statistics:view')")
    public R<ClubProfileConfigDTO> getCurrent() {
        return R.ok(loadConfig(resolveTenantId()));
    }

    @ApiOperation("保存当前租户社团概况配置")
    @PutMapping("/current")
    @PreAuthorize("hasAnyAuthority('content:news:edit')")
    public R<ClubProfileConfigDTO> saveCurrent(@RequestBody ClubProfileConfigDTO dto) {
        Long tenantId = resolveTenantId();
        SystemConfig config = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getTenantId, tenantId)
                .eq(SystemConfig::getConfigKey, CONFIG_KEY)
                .last("LIMIT 1"));

        if (config == null) {
            config = new SystemConfig();
            config.setTenantId(tenantId);
            config.setConfigKey(CONFIG_KEY);
            config.setDescription("club profile config");
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

    @ApiOperation("公开获取指定租户社团概况配置")
    @GetMapping("/public")
    public R<ClubProfileConfigDTO> getPublic(@RequestParam(value = "tenantId", required = false) Long tenantId) {
        return R.ok(loadConfig(tenantId == null ? resolveTenantId() : tenantId));
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    private ClubProfileConfigDTO loadConfig(Long tenantId) {
        SystemConfig config = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getTenantId, tenantId)
                .eq(SystemConfig::getConfigKey, CONFIG_KEY)
                .last("LIMIT 1"));
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()) {
            return defaultConfig();
        }
        try {
            return normalize(objectMapper.readValue(config.getConfigValue(), ClubProfileConfigDTO.class));
        } catch (JsonProcessingException e) {
            return defaultConfig();
        }
    }

    private String toJson(ClubProfileConfigDTO dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("社团概况配置序列化失败", e);
        }
    }

    private ClubProfileConfigDTO normalize(ClubProfileConfigDTO dto) {
        ClubProfileConfigDTO config = dto == null ? new ClubProfileConfigDTO() : dto;
        ClubProfileConfigDTO defaults = defaultConfig();
        if (config.getPageTitle() == null || config.getPageTitle().isBlank()) config.setPageTitle(defaults.getPageTitle());
        if (config.getPageDescription() == null || config.getPageDescription().isBlank()) config.setPageDescription(defaults.getPageDescription());
        if (config.getIntroTitle() == null || config.getIntroTitle().isBlank()) config.setIntroTitle(defaults.getIntroTitle());
        if (config.getIntroParagraphs() == null || config.getIntroParagraphs().isEmpty()) config.setIntroParagraphs(defaults.getIntroParagraphs());
        if (config.getHistory() == null || config.getHistory().isEmpty()) config.setHistory(defaults.getHistory());
        if (config.getMissionTitle() == null || config.getMissionTitle().isBlank()) config.setMissionTitle(defaults.getMissionTitle());
        if (config.getMissionCards() == null || config.getMissionCards().isEmpty()) config.setMissionCards(defaults.getMissionCards());
        if (config.getContactEmail() == null || config.getContactEmail().isBlank()) config.setContactEmail(defaults.getContactEmail());
        if (config.getContactAddress() == null || config.getContactAddress().isBlank()) config.setContactAddress(defaults.getContactAddress());
        return config;
    }

    private ClubProfileConfigDTO defaultConfig() {
        ClubProfileConfigDTO dto = new ClubProfileConfigDTO();
        dto.setPageTitle("关于我们");
        dto.setPageDescription("了解当前社团的定位、发展历程与主要方向");
        dto.setIntroTitle("社团简介");
        dto.setIntroParagraphs(List.of(
                "RK-Web 面向多租户社团场景，支持内容发布、活动组织、成员协作与作品展示。",
                "每个租户都可以维护自己的社团概况、成员与内容，不再共用同一套固定介绍文案。"
        ));
        dto.setHistory(List.of(
                item("2020", "社团成立", "由一批热爱软件开发和项目协作的成员共同发起。"),
                item("2022", "组织成型", "逐步形成活动、内容、成员与作品协同运作模式。"),
                item("2024", "多租户化", "开始支持不同社团独立运营与独立展示。")
        ));
        dto.setMissionTitle("核心方向");
        dto.setMissionCards(List.of(
                item(null, "技术成长", "围绕真实项目与活动积累稳定的技术能力。"),
                item(null, "组织协作", "建立可持续的成员分工、审核与内容运营流程。"),
                item(null, "成果展示", "通过新闻、活动、比赛与作品展示对外输出成果。")
        ));
        dto.setContactEmail("contact@rk-web.org");
        dto.setContactAddress("黑河学院");
        return dto;
    }

    private ClubProfileSectionItemDTO item(String year, String title, String description) {
        ClubProfileSectionItemDTO item = new ClubProfileSectionItemDTO();
        item.setYear(year);
        item.setTitle(title);
        item.setDescription(description);
        return item;
    }
}
