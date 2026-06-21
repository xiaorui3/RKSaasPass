package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.api.dto.user.TenantWorkflowConfigDTO;
import com.tianji.api.dto.user.WorkflowPolicyDTO;
import com.tianji.common.utils.TenantContext;
import com.tianji.user.domain.po.SystemConfig;
import com.tianji.user.mapper.SystemConfigMapper;
import com.tianji.user.service.ITenantWorkflowConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class TenantWorkflowConfigServiceImpl implements ITenantWorkflowConfigService {

    public static final String CONFIG_KEY = "tenant.workflow.config";

    private final SystemConfigMapper systemConfigMapper;
    private final ObjectMapper objectMapper;

    @Override
    public TenantWorkflowConfigDTO loadCurrentConfig(Long tenantId) {
        Long resolvedTenantId = tenantId == null ? 1L : tenantId;
        SystemConfig config = executeInTenantScope(resolvedTenantId, () -> systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getTenantId, resolvedTenantId)
                .eq(SystemConfig::getConfigKey, CONFIG_KEY)
                .last("LIMIT 1")));
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()) {
            return defaultConfig();
        }
        try {
            return normalize(objectMapper.readValue(config.getConfigValue(), TenantWorkflowConfigDTO.class));
        } catch (JsonProcessingException e) {
            return defaultConfig();
        }
    }

    @Override
    public TenantWorkflowConfigDTO saveCurrentConfig(Long tenantId, TenantWorkflowConfigDTO dto) {
        Long resolvedTenantId = tenantId == null ? 1L : tenantId;
        TenantWorkflowConfigDTO normalized = normalize(dto);
        executeInTenantScope(resolvedTenantId, () -> {
            SystemConfig config = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                    .eq(SystemConfig::getTenantId, resolvedTenantId)
                    .eq(SystemConfig::getConfigKey, CONFIG_KEY)
                    .last("LIMIT 1"));
            if (config == null) {
                config = new SystemConfig();
                config.setTenantId(resolvedTenantId);
                config.setConfigKey(CONFIG_KEY);
                config.setDescription("tenant workflow config");
                config.setIsEnabled(true);
                config.setIsDeleted(false);
                config.setCreateTime(LocalDateTime.now());
            }
            config.setConfigValue(writeJson(normalized));
            config.setUpdateTime(LocalDateTime.now());
            if (config.getId() == null) {
                systemConfigMapper.insert(config);
            } else {
                systemConfigMapper.updateById(config);
            }
            return null;
        });
        return normalized;
    }

    private <T> T executeInTenantScope(Long tenantId, Supplier<T> supplier) {
        Long previousTenantId = TenantContext.getTenantId();
        Boolean previousSuperAdmin = TenantContext.isSuperAdmin();
        try {
            TenantContext.setTenantId(tenantId);
            TenantContext.setSuperAdmin(false);
            return supplier.get();
        } finally {
            if (previousTenantId == null) {
                TenantContext.removeTenantId();
            } else {
                TenantContext.setTenantId(previousTenantId);
            }
            TenantContext.setSuperAdmin(previousSuperAdmin);
        }
    }

    private String writeJson(TenantWorkflowConfigDTO dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("serialize tenant workflow config failed", e);
        }
    }

    private TenantWorkflowConfigDTO normalize(TenantWorkflowConfigDTO dto) {
        TenantWorkflowConfigDTO target = dto == null ? new TenantWorkflowConfigDTO() : dto;
        TenantWorkflowConfigDTO defaults = defaultConfig();
        target.setRegistration(normalizePolicy(target.getRegistration(), defaults.getRegistration()));
        target.setJoinReview(normalizePolicy(target.getJoinReview(), defaults.getJoinReview()));
        target.setActivityPublish(normalizePolicy(target.getActivityPublish(), defaults.getActivityPublish()));
        target.setCompetitionPublish(normalizePolicy(target.getCompetitionPublish(), defaults.getCompetitionPublish()));
        target.setNewsPublish(normalizePolicy(target.getNewsPublish(), defaults.getNewsPublish()));
        target.setActivitySignup(normalizePolicy(target.getActivitySignup(), defaults.getActivitySignup()));
        target.setCompetitionSignup(normalizePolicy(target.getCompetitionSignup(), defaults.getCompetitionSignup()));
        return target;
    }

    private WorkflowPolicyDTO normalizePolicy(WorkflowPolicyDTO value, WorkflowPolicyDTO defaults) {
        WorkflowPolicyDTO policy = value == null ? new WorkflowPolicyDTO() : value;
        policy.setOpenRegistration(policy.getOpenRegistration() != null ? policy.getOpenRegistration() : defaults.getOpenRegistration());
        policy.setRequireApproval(policy.getRequireApproval() != null ? policy.getRequireApproval() : defaults.getRequireApproval());
        policy.setNotifyAdmins(policy.getNotifyAdmins() != null ? policy.getNotifyAdmins() : defaults.getNotifyAdmins());
        policy.setNotifyApplicantOnFailure(policy.getNotifyApplicantOnFailure() != null ? policy.getNotifyApplicantOnFailure() : defaults.getNotifyApplicantOnFailure());
        policy.setNotifyOnSuccess(policy.getNotifyOnSuccess() != null ? policy.getNotifyOnSuccess() : defaults.getNotifyOnSuccess());
        policy.setAdvisorMode(policy.getAdvisorMode() != null && !policy.getAdvisorMode().isBlank() ? policy.getAdvisorMode() : defaults.getAdvisorMode());
        if (policy.getDesignatedAdvisorRoleIds() == null) {
            policy.setDesignatedAdvisorRoleIds(new ArrayList<>(defaults.getDesignatedAdvisorRoleIds()));
        }
        return policy;
    }

    private TenantWorkflowConfigDTO defaultConfig() {
        TenantWorkflowConfigDTO dto = new TenantWorkflowConfigDTO();
        dto.setRegistration(policy(false, true, true, true, false, "ANY_ONE"));
        dto.setJoinReview(policy(false, true, true, true, false, "ANY_ONE"));
        dto.setActivityPublish(policy(false, true, true, true, true, "ANY_ONE"));
        dto.setCompetitionPublish(policy(false, true, true, true, true, "ANY_ONE"));
        dto.setNewsPublish(policy(false, true, true, true, true, "ANY_ONE"));
        dto.setActivitySignup(policy(false, false, false, false, true, "ANY_ONE"));
        dto.setCompetitionSignup(policy(false, false, false, false, true, "ANY_ONE"));
        return dto;
    }

    private WorkflowPolicyDTO policy(boolean openRegistration, boolean requireApproval, boolean notifyAdmins,
                                     boolean notifyApplicantOnFailure, boolean notifyOnSuccess, String advisorMode) {
        WorkflowPolicyDTO policy = new WorkflowPolicyDTO();
        policy.setOpenRegistration(openRegistration);
        policy.setRequireApproval(requireApproval);
        policy.setNotifyAdmins(notifyAdmins);
        policy.setNotifyApplicantOnFailure(notifyApplicantOnFailure);
        policy.setNotifyOnSuccess(notifyOnSuccess);
        policy.setAdvisorMode(advisorMode);
        policy.setDesignatedAdvisorRoleIds(new ArrayList<>());
        return policy;
    }
}
