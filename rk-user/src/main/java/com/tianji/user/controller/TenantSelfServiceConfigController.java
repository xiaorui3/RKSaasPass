package com.tianji.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.api.dto.user.TenantSelfServiceAdmissionPolicyDTO;
import com.tianji.common.domain.R;
import com.tianji.common.utils.TenantContext;
import com.tianji.user.domain.dto.TenantSelfServiceConfigDTO;
import com.tianji.user.domain.po.SystemConfig;
import com.tianji.user.mapper.SystemConfigMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Api(tags = "租户自助配置")
@RestController
@RequestMapping("/api/config/tenant-self-service")
@RequiredArgsConstructor
public class TenantSelfServiceConfigController {

    private static final String CONFIG_KEY = "tenant.self-service.config";

    private final SystemConfigMapper systemConfigMapper;
    private final ObjectMapper objectMapper;

    @ApiOperation("获取当前租户自助配置")
    @GetMapping("/current")
    @PreAuthorize("hasAnyAuthority('system:config:view', 'system:config:query', 'system:config:list')")
    public R<TenantSelfServiceConfigDTO> getCurrent() {
        Long tenantId = resolveTenantId();
        return R.ok(loadConfig(tenantId).setTenantId(tenantId));
    }

    @ApiOperation("公开获取指定租户自助配置")
    @GetMapping("/public")
    public R<TenantSelfServiceConfigDTO> getPublic(@RequestParam(value = "tenantId", required = false) Long tenantId) {
        Long resolvedTenantId = tenantId == null ? resolveTenantId() : tenantId;
        return R.ok(loadConfig(resolvedTenantId).setTenantId(resolvedTenantId));
    }

    @ApiOperation("内部获取租户入社注册策略")
    @GetMapping("/internal/admission-policy")
    public TenantSelfServiceAdmissionPolicyDTO getInternalAdmissionPolicy(@RequestParam("tenantId") Long tenantId) {
        Long resolvedTenantId = tenantId == null ? resolveTenantId() : tenantId;
        TenantSelfServiceConfigDTO config = loadConfig(resolvedTenantId);
        return new TenantSelfServiceAdmissionPolicyDTO()
                .setTenantId(resolvedTenantId)
                .setAllowPublicRegister(config.getAdmissionSettings().getAllowPublicRegister())
                .setAllowJoinApplication(config.getAdmissionSettings().getAllowJoinApplication())
                .setRequireTeacherReview(config.getReviewSettings().getRequireTeacherReview())
                .setRequireClubManagerReview(config.getReviewSettings().getRequireClubManagerReview())
                .setEmailNoticeEnabled(config.getNotificationSettings().getEmailNoticeEnabled())
                .setSiteNoticeEnabled(config.getNotificationSettings().getSiteNoticeEnabled())
                .setApprovalNoticeEnabled(config.getNotificationSettings().getApprovalNoticeEnabled())
                .setMaxClubMembers(config.getQuotaSettings().getMaxClubMembers())
                .setMaxActiveActivities(config.getQuotaSettings().getMaxActiveActivities())
                .setMaxMonthlyNews(config.getQuotaSettings().getMaxMonthlyNews())
                .setMaxStorageMb(config.getQuotaSettings().getMaxStorageMb());
    }

    @ApiOperation("保存当前租户自助配置")
    @PutMapping("/current")
    @PreAuthorize("hasAuthority('system:config:edit')")
    public R<TenantSelfServiceConfigDTO> saveCurrent(@RequestBody TenantSelfServiceConfigDTO dto) {
        Long tenantId = resolveTenantId();
        SystemConfig config = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getTenantId, tenantId)
                .eq(SystemConfig::getConfigKey, CONFIG_KEY)
                .last("LIMIT 1"));

        if (config == null) {
            config = new SystemConfig()
                    .setTenantId(tenantId)
                    .setConfigKey(CONFIG_KEY)
                    .setDescription("tenant self service config")
                    .setIsEnabled(true)
                    .setIsDeleted(false)
                    .setCreateTime(LocalDateTime.now());
        }
        config.setConfigValue(toJson(normalize(dto).setTenantId(tenantId)));
        config.setUpdateTime(LocalDateTime.now());

        if (config.getId() == null) {
            systemConfigMapper.insert(config);
        } else {
            systemConfigMapper.updateById(config);
        }
        return R.ok(loadConfig(tenantId).setTenantId(tenantId));
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    private TenantSelfServiceConfigDTO loadConfig(Long tenantId) {
        SystemConfig config = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getTenantId, tenantId)
                .eq(SystemConfig::getConfigKey, CONFIG_KEY)
                .last("LIMIT 1"));
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()) {
            return defaultConfig(tenantId);
        }
        try {
            return normalize(objectMapper.readValue(config.getConfigValue(), TenantSelfServiceConfigDTO.class));
        } catch (JsonProcessingException e) {
            return defaultConfig(tenantId);
        }
    }

    private TenantSelfServiceConfigDTO normalize(TenantSelfServiceConfigDTO dto) {
        TenantSelfServiceConfigDTO config = dto == null ? new TenantSelfServiceConfigDTO() : dto;
        TenantSelfServiceConfigDTO defaults = defaultConfig(config.getTenantId());
        if (config.getBrandSettings() == null) config.setBrandSettings(defaults.getBrandSettings());
        if (config.getPortalSettings() == null) config.setPortalSettings(defaults.getPortalSettings());
        if (config.getAdmissionSettings() == null) config.setAdmissionSettings(defaults.getAdmissionSettings());
        if (config.getReviewSettings() == null) config.setReviewSettings(defaults.getReviewSettings());
        if (config.getNotificationSettings() == null) config.setNotificationSettings(defaults.getNotificationSettings());
        if (config.getQuotaSettings() == null) config.setQuotaSettings(defaults.getQuotaSettings());

        normalizeBrand(config.getBrandSettings(), defaults.getBrandSettings());
        normalizePortal(config.getPortalSettings(), defaults.getPortalSettings());
        normalizeAdmission(config.getAdmissionSettings(), defaults.getAdmissionSettings());
        normalizeReview(config.getReviewSettings(), defaults.getReviewSettings());
        normalizeNotification(config.getNotificationSettings(), defaults.getNotificationSettings());
        normalizeQuota(config.getQuotaSettings(), defaults.getQuotaSettings());
        return config;
    }

    private void normalizeBrand(TenantSelfServiceConfigDTO.BrandSettings settings, TenantSelfServiceConfigDTO.BrandSettings defaults) {
        if (settings.getTenantDisplayName() == null || settings.getTenantDisplayName().isBlank()) {
            settings.setTenantDisplayName(defaults.getTenantDisplayName());
        }
        if (settings.getSlogan() == null || settings.getSlogan().isBlank()) {
            settings.setSlogan(defaults.getSlogan());
        }
        if (settings.getContactEmail() == null || settings.getContactEmail().isBlank()) {
            settings.setContactEmail(defaults.getContactEmail());
        }
        if (settings.getContactAddress() == null || settings.getContactAddress().isBlank()) {
            settings.setContactAddress(defaults.getContactAddress());
        }
    }

    private void normalizePortal(TenantSelfServiceConfigDTO.PortalSettings settings, TenantSelfServiceConfigDTO.PortalSettings defaults) {
        if (settings.getShowNews() == null) settings.setShowNews(defaults.getShowNews());
        if (settings.getShowActivities() == null) settings.setShowActivities(defaults.getShowActivities());
        if (settings.getShowCompetitions() == null) settings.setShowCompetitions(defaults.getShowCompetitions());
        if (settings.getShowAlumni() == null) settings.setShowAlumni(defaults.getShowAlumni());
        if (settings.getShowWorks() == null) settings.setShowWorks(defaults.getShowWorks());
        if (settings.getAllowPublicSearch() == null) settings.setAllowPublicSearch(defaults.getAllowPublicSearch());
    }

    private void normalizeAdmission(TenantSelfServiceConfigDTO.AdmissionSettings settings, TenantSelfServiceConfigDTO.AdmissionSettings defaults) {
        if (settings.getAllowPublicRegister() == null) settings.setAllowPublicRegister(defaults.getAllowPublicRegister());
        if (settings.getAllowJoinApplication() == null) settings.setAllowJoinApplication(defaults.getAllowJoinApplication());
        if (settings.getRequireEmailVerification() == null) settings.setRequireEmailVerification(defaults.getRequireEmailVerification());
        if (settings.getAllowReferralCode() == null) settings.setAllowReferralCode(defaults.getAllowReferralCode());
        if (settings.getMaxPendingApplications() == null || settings.getMaxPendingApplications() < 1) {
            settings.setMaxPendingApplications(defaults.getMaxPendingApplications());
        }
    }

    private void normalizeReview(TenantSelfServiceConfigDTO.ReviewSettings settings, TenantSelfServiceConfigDTO.ReviewSettings defaults) {
        if (settings.getRequireTeacherReview() == null) settings.setRequireTeacherReview(defaults.getRequireTeacherReview());
        if (settings.getRequireClubManagerReview() == null) settings.setRequireClubManagerReview(defaults.getRequireClubManagerReview());
        if (settings.getAutoRejectExpired() == null) settings.setAutoRejectExpired(defaults.getAutoRejectExpired());
        if (settings.getReviewSlaHours() == null || settings.getReviewSlaHours() < 1) {
            settings.setReviewSlaHours(defaults.getReviewSlaHours());
        }
    }

    private void normalizeNotification(TenantSelfServiceConfigDTO.NotificationSettings settings, TenantSelfServiceConfigDTO.NotificationSettings defaults) {
        if (settings.getEmailNoticeEnabled() == null) settings.setEmailNoticeEnabled(defaults.getEmailNoticeEnabled());
        if (settings.getSiteNoticeEnabled() == null) settings.setSiteNoticeEnabled(defaults.getSiteNoticeEnabled());
        if (settings.getApprovalNoticeEnabled() == null) settings.setApprovalNoticeEnabled(defaults.getApprovalNoticeEnabled());
        if (settings.getWeeklyDigestEnabled() == null) settings.setWeeklyDigestEnabled(defaults.getWeeklyDigestEnabled());
    }

    private void normalizeQuota(TenantSelfServiceConfigDTO.QuotaSettings settings, TenantSelfServiceConfigDTO.QuotaSettings defaults) {
        if (settings.getMaxClubMembers() == null || settings.getMaxClubMembers() < 1) settings.setMaxClubMembers(defaults.getMaxClubMembers());
        if (settings.getMaxActiveActivities() == null || settings.getMaxActiveActivities() < 1) settings.setMaxActiveActivities(defaults.getMaxActiveActivities());
        if (settings.getMaxMonthlyNews() == null || settings.getMaxMonthlyNews() < 1) settings.setMaxMonthlyNews(defaults.getMaxMonthlyNews());
        if (settings.getMaxStorageMb() == null || settings.getMaxStorageMb() < 100) settings.setMaxStorageMb(defaults.getMaxStorageMb());
    }

    private TenantSelfServiceConfigDTO defaultConfig(Long tenantId) {
        return new TenantSelfServiceConfigDTO()
                .setTenantId(tenantId)
                .setBrandSettings(new TenantSelfServiceConfigDTO.BrandSettings()
                        .setTenantDisplayName("当前租户")
                        .setSlogan("社团业务自助运营配置")
                        .setContactEmail("contact@rk-web.org")
                        .setContactAddress("黑河学院"))
                .setPortalSettings(new TenantSelfServiceConfigDTO.PortalSettings()
                        .setShowNews(true)
                        .setShowActivities(true)
                        .setShowCompetitions(true)
                        .setShowAlumni(true)
                        .setShowWorks(true)
                        .setAllowPublicSearch(true))
                .setAdmissionSettings(new TenantSelfServiceConfigDTO.AdmissionSettings()
                        .setAllowPublicRegister(true)
                        .setAllowJoinApplication(true)
                        .setRequireEmailVerification(true)
                        .setAllowReferralCode(true)
                        .setMaxPendingApplications(200))
                .setReviewSettings(new TenantSelfServiceConfigDTO.ReviewSettings()
                        .setRequireTeacherReview(true)
                        .setRequireClubManagerReview(true)
                        .setAutoRejectExpired(false)
                        .setReviewSlaHours(72))
                .setNotificationSettings(new TenantSelfServiceConfigDTO.NotificationSettings()
                        .setEmailNoticeEnabled(true)
                        .setSiteNoticeEnabled(true)
                        .setApprovalNoticeEnabled(true)
                        .setWeeklyDigestEnabled(false))
                .setQuotaSettings(new TenantSelfServiceConfigDTO.QuotaSettings()
                        .setMaxClubMembers(500)
                        .setMaxActiveActivities(30)
                        .setMaxMonthlyNews(60)
                        .setMaxStorageMb(2048));
    }

    private String toJson(TenantSelfServiceConfigDTO dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("tenant self service config serialize failed", e);
        }
    }
}
