package com.tianji.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.api.dto.user.TenantSelfServiceAdmissionPolicyDTO;
import com.tianji.common.domain.R;
import com.tianji.common.utils.TenantContext;
import com.tianji.user.domain.dto.TenantSelfServiceConfigDTO;
import com.tianji.user.domain.po.SystemConfig;
import com.tianji.user.mapper.SystemConfigMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantSelfServiceConfigControllerTest {

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SystemConfig.class);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getCurrentReturnsDefaultConfigForCurrentTenantWhenMissing() {
        TenantContext.setTenantId(9L);
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        when(mapper.selectOne(ArgumentMatchers.<LambdaQueryWrapper<SystemConfig>>any())).thenReturn(null);
        TenantSelfServiceConfigController controller = new TenantSelfServiceConfigController(mapper, new ObjectMapper());

        R<TenantSelfServiceConfigDTO> response = controller.getCurrent();

        TenantSelfServiceConfigDTO data = response.getData();
        assertEquals(9L, data.getTenantId());
        assertEquals("当前租户", data.getBrandSettings().getTenantDisplayName());
        assertTrue(data.getPortalSettings().getShowNews());
        assertTrue(data.getAdmissionSettings().getAllowPublicRegister());
        assertTrue(data.getReviewSettings().getRequireTeacherReview());
        assertTrue(data.getNotificationSettings().getEmailNoticeEnabled());
        assertEquals(500, data.getQuotaSettings().getMaxClubMembers());
    }

    @Test
    void saveCurrentInsertsTenantScopedSystemConfigWhenMissing() {
        TenantContext.setTenantId(12L);
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        when(mapper.selectOne(ArgumentMatchers.<LambdaQueryWrapper<SystemConfig>>any())).thenReturn(null);
        TenantSelfServiceConfigController controller = new TenantSelfServiceConfigController(mapper, new ObjectMapper());
        TenantSelfServiceConfigDTO payload = new TenantSelfServiceConfigDTO();
        payload.getBrandSettings()
                .setTenantDisplayName("黑河学院测试社团")
                .setSlogan("自助配置")
                .setContactEmail("club@example.com")
                .setContactAddress("黑河学院");
        payload.getPortalSettings().setShowNews(false);
        payload.getAdmissionSettings().setMaxPendingApplications(0);
        payload.getReviewSettings().setReviewSlaHours(0);
        payload.getNotificationSettings().setWeeklyDigestEnabled(true);
        payload.getQuotaSettings().setMaxClubMembers(0);

        R<TenantSelfServiceConfigDTO> response = controller.saveCurrent(payload);

        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(mapper).insert(captor.capture());
        SystemConfig saved = captor.getValue();
        assertEquals(12L, saved.getTenantId());
        assertEquals("tenant.self-service.config", saved.getConfigKey());
        assertTrue(saved.getIsEnabled());
        assertFalse(saved.getConfigValue().contains("\"tenantId\":null"));
        assertTrue(saved.getConfigValue().contains("\"tenantId\":12"));
        assertTrue(saved.getConfigValue().contains("\"maxPendingApplications\":200"));
        assertEquals(12L, response.getData().getTenantId());
    }

    @Test
    void saveCurrentUpdatesExistingSystemConfig() throws Exception {
        TenantContext.setTenantId(15L);
        SystemConfig existing = new SystemConfig()
                .setId(88L)
                .setTenantId(15L)
                .setConfigKey("tenant.self-service.config")
                .setConfigValue("{}")
                .setIsEnabled(true);
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        when(mapper.selectOne(ArgumentMatchers.<LambdaQueryWrapper<SystemConfig>>any()))
                .thenReturn(existing)
                .thenReturn(existing);
        TenantSelfServiceConfigController controller = new TenantSelfServiceConfigController(mapper, new ObjectMapper());
        TenantSelfServiceConfigDTO payload = new TenantSelfServiceConfigDTO();
        payload.getNotificationSettings().setEmailNoticeEnabled(false);

        controller.saveCurrent(payload);

        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(mapper).updateById(captor.capture());
        SystemConfig saved = captor.getValue();
        assertEquals(88L, saved.getId());
        assertNotNull(saved.getUpdateTime());
        assertTrue(saved.getConfigValue().contains("\"emailNoticeEnabled\":false"));
    }

    @Test
    void getPublicReturnsRequestedTenantConfigWithoutCurrentTenantContext() throws Exception {
        TenantContext.setTenantId(1L);
        TenantSelfServiceConfigDTO stored = new TenantSelfServiceConfigDTO();
        stored.getBrandSettings().setTenantDisplayName("租户二社团");
        stored.getAdmissionSettings().setAllowPublicRegister(false);
        SystemConfig existing = new SystemConfig()
                .setId(99L)
                .setTenantId(2L)
                .setConfigKey("tenant.self-service.config")
                .setConfigValue(new ObjectMapper().writeValueAsString(stored))
                .setIsEnabled(true);
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        when(mapper.selectOne(ArgumentMatchers.<LambdaQueryWrapper<SystemConfig>>any())).thenReturn(existing);
        TenantSelfServiceConfigController controller = new TenantSelfServiceConfigController(mapper, new ObjectMapper());

        R<TenantSelfServiceConfigDTO> response = controller.getPublic(2L);

        assertEquals(2L, response.getData().getTenantId());
        assertEquals("租户二社团", response.getData().getBrandSettings().getTenantDisplayName());
        assertFalse(response.getData().getAdmissionSettings().getAllowPublicRegister());
    }

    @Test
    void getInternalAdmissionPolicyReturnsRegisterAndJoinSwitches() throws Exception {
        TenantSelfServiceConfigDTO stored = new TenantSelfServiceConfigDTO();
        stored.getAdmissionSettings()
                .setAllowPublicRegister(false)
                .setAllowJoinApplication(false);
        SystemConfig existing = new SystemConfig()
                .setId(100L)
                .setTenantId(3L)
                .setConfigKey("tenant.self-service.config")
                .setConfigValue(new ObjectMapper().writeValueAsString(stored))
                .setIsEnabled(true);
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        when(mapper.selectOne(ArgumentMatchers.<LambdaQueryWrapper<SystemConfig>>any())).thenReturn(existing);
        TenantSelfServiceConfigController controller = new TenantSelfServiceConfigController(mapper, new ObjectMapper());

        TenantSelfServiceAdmissionPolicyDTO policy = controller.getInternalAdmissionPolicy(3L);

        assertEquals(3L, policy.getTenantId());
        assertFalse(policy.getAllowPublicRegister());
        assertFalse(policy.getAllowJoinApplication());
    }
}
