package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.api.dto.user.TenantWorkflowConfigDTO;
import com.tianji.api.dto.user.WorkflowPolicyDTO;
import com.tianji.common.utils.TenantContext;
import com.tianji.user.domain.po.SystemConfig;
import com.tianji.user.mapper.SystemConfigMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantWorkflowConfigServiceImplTest {

    @Mock
    private SystemConfigMapper systemConfigMapper;

    private TenantWorkflowConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SystemConfig.class);
        service = new TenantWorkflowConfigServiceImpl(systemConfigMapper, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void loadCurrentConfig_shouldTemporarilySwitchIntoRequestedTenantScope() {
        AtomicReference<Long> tenantSeenByMapper = new AtomicReference<>();
        AtomicReference<Boolean> superAdminSeenByMapper = new AtomicReference<>();
        SystemConfig config = new SystemConfig();
        config.setId(12L);
        config.setTenantId(33L);
        config.setConfigKey(TenantWorkflowConfigServiceImpl.CONFIG_KEY);
        config.setConfigValue(writeOpenRegistrationConfigJson());
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());

        when(systemConfigMapper.selectOne(any(Wrapper.class))).thenAnswer(invocation -> {
            tenantSeenByMapper.set(TenantContext.getTenantId());
            superAdminSeenByMapper.set(TenantContext.isSuperAdmin());
            return config;
        });

        TenantContext.setTenantId(1L);
        TenantContext.setSuperAdmin(false);

        TenantWorkflowConfigDTO result = service.loadCurrentConfig(33L);

        assertTrue(Boolean.TRUE.equals(result.getRegistration().getOpenRegistration()));
        assertFalse(Boolean.TRUE.equals(result.getRegistration().getRequireApproval()));
        assertEquals(33L, tenantSeenByMapper.get());
        assertEquals(false, superAdminSeenByMapper.get());
        assertEquals(1L, TenantContext.getTenantId());
        assertEquals(false, TenantContext.isSuperAdmin());
    }

    private String writeOpenRegistrationConfigJson() {
        TenantWorkflowConfigDTO dto = new TenantWorkflowConfigDTO();
        WorkflowPolicyDTO registration = new WorkflowPolicyDTO();
        registration.setOpenRegistration(true);
        registration.setRequireApproval(false);
        registration.setNotifyAdmins(false);
        registration.setNotifyApplicantOnFailure(false);
        registration.setNotifyOnSuccess(true);
        registration.setAdvisorMode("ANY_ONE");
        registration.setDesignatedAdvisorRoleIds(Collections.emptyList());
        dto.setRegistration(registration);
        try {
            return new ObjectMapper().writeValueAsString(dto);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
