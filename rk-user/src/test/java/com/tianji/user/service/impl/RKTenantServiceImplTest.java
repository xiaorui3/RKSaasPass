package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.tianji.api.client.search.SearchClient;
import com.tianji.user.domain.dto.TenantDTO;
import com.tianji.user.domain.po.RKTenant;
import com.tianji.user.domain.po.SystemConfig;
import com.tianji.user.mapper.RKTenantMapper;
import com.tianji.user.mapper.SystemConfigMapper;
import com.tianji.user.service.ITenantWorkflowConfigService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RKTenantServiceImplTest {

    @Mock
    private RKTenantMapper tenantMapper;
    @Mock
    private ITenantWorkflowConfigService tenantWorkflowConfigService;
    @Mock
    private SystemConfigMapper systemConfigMapper;
    @Mock
    private SearchClient searchClient;

    private RKTenantServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), RKTenant.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SystemConfig.class);
        service = new RKTenantServiceImpl(tenantWorkflowConfigService, systemConfigMapper, searchClient);
        ReflectionTestUtils.setField(service, "baseMapper", tenantMapper);
    }

    @Test
    void createTenant_shouldPersistDefaultWorkflowConfigForNewTenant() {
        when(tenantMapper.selectCount(any(Wrapper.class))).thenReturn(0);
        when(tenantMapper.insert(any(RKTenant.class))).thenAnswer(invocation -> {
            RKTenant tenant = invocation.getArgument(0);
            tenant.setId(88L);
            return 1;
        });

        TenantDTO dto = new TenantDTO();
        dto.setTenantCode("tenant-88");
        dto.setTenantName("Tenant 88");

        Long tenantId = service.createTenant(dto);

        assertEquals(88L, tenantId);
        verify(tenantWorkflowConfigService).saveCurrentConfig(eq(88L), isNull());
    }

    @Test
    void createTenant_shouldRejectDuplicateTenantCodeBeforeBootstrap() {
        when(tenantMapper.selectCount(any(Wrapper.class))).thenReturn(1);

        TenantDTO dto = new TenantDTO();
        dto.setTenantCode("tenant-dup");
        dto.setTenantName("Tenant Dup");

        try {
            service.createTenant(dto);
        } catch (RuntimeException ex) {
            assertTrue(ex.getMessage().contains("租户编码已存在"));
        }

        verify(tenantWorkflowConfigService, never()).saveCurrentConfig(any(), any());
    }

    @Test
    void getTenantInitStatus_shouldReflectWorkflowConfigReadiness() {
        RKTenant tenant = new RKTenant();
        tenant.setId(5L);
        tenant.setTenantName("Tenant 5");
        tenant.setStatus(1);
        tenant.setIsDeleted(0);
        when(tenantMapper.selectById(5L)).thenReturn(tenant);

        when(systemConfigMapper.selectOne(any(Wrapper.class))).thenReturn(new SystemConfig().setId(7L));
        Map<String, Object> ready = service.getTenantInitStatus(5L);
        assertEquals(1, ready.get("initStatus"));
        assertEquals(Boolean.TRUE, ready.get("workflowConfigReady"));
        verify(tenantWorkflowConfigService, never()).saveCurrentConfig(eq(5L), isNull());
    }

    @Test
    void getTenantInitStatus_shouldBackfillDefaultWorkflowConfigForActiveTenantWhenMissing() {
        RKTenant tenant = new RKTenant();
        tenant.setId(15L);
        tenant.setTenantName("Tenant 15");
        tenant.setStatus(1);
        tenant.setIsDeleted(0);
        when(tenantMapper.selectById(15L)).thenReturn(tenant);
        when(systemConfigMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        Map<String, Object> status = service.getTenantInitStatus(15L);

        assertEquals(1, status.get("initStatus"));
        assertEquals(Boolean.TRUE, status.get("workflowConfigReady"));
        verify(tenantWorkflowConfigService).saveCurrentConfig(eq(15L), isNull());
    }

    @Test
    void getTenantInitStatus_shouldNotBackfillWorkflowConfigForDisabledTenant() {
        RKTenant tenant = new RKTenant();
        tenant.setId(16L);
        tenant.setTenantName("Tenant 16");
        tenant.setStatus(0);
        tenant.setIsDeleted(0);
        when(tenantMapper.selectById(16L)).thenReturn(tenant);
        when(systemConfigMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        Map<String, Object> status = service.getTenantInitStatus(16L);

        assertEquals(0, status.get("initStatus"));
        assertEquals(Boolean.FALSE, status.get("workflowConfigReady"));
        verify(tenantWorkflowConfigService, never()).saveCurrentConfig(eq(16L), isNull());
    }

    @Test
    void reinitTenant_shouldResetDefaultWorkflowConfig() {
        RKTenant tenant = new RKTenant();
        tenant.setId(6L);
        tenant.setTenantName("Tenant 6");
        tenant.setIsDeleted(0);
        when(tenantMapper.selectById(6L)).thenReturn(tenant);
        when(tenantMapper.updateById(any(RKTenant.class))).thenReturn(1);

        assertTrue(service.reinitTenant(6L));

        verify(tenantWorkflowConfigService).saveCurrentConfig(eq(6L), isNull());
    }

    @Test
    void repairTenant_shouldBackfillWorkflowConfigUsingCurrentPolicy() {
        RKTenant tenant = new RKTenant();
        tenant.setId(9L);
        tenant.setTenantName("Tenant 9");
        tenant.setIsDeleted(0);
        when(tenantMapper.selectById(9L)).thenReturn(tenant);
        when(tenantMapper.updateById(any(RKTenant.class))).thenReturn(1);
        when(tenantWorkflowConfigService.loadCurrentConfig(9L)).thenReturn(null);

        assertTrue(service.repairTenant(9L));

        verify(tenantWorkflowConfigService).loadCurrentConfig(9L);
        verify(tenantWorkflowConfigService).saveCurrentConfig(9L, null);
    }
}
