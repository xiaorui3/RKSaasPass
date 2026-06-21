package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.tianji.api.client.search.SearchClient;
import com.tianji.common.utils.TenantContext;
import com.tianji.user.domain.po.ClubMember;
import com.tianji.user.domain.po.RKTenant;
import com.tianji.user.mapper.ClubMemberMapper;
import com.tianji.user.mapper.RKTenantMapper;
import com.tianji.user.service.IAdmissionService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ClubMemberServiceImplTenantScopeTest {

    @Mock
    private ClubMemberMapper clubMemberMapper;
    @Mock
    private IAdmissionService admissionService;
    @Mock
    private RKTenantMapper tenantMapper;
    @Mock
    private UserServiceImpl userService;
    @Mock
    private SearchClient searchClient;

    private ClubMemberServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ClubMember.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), RKTenant.class);
        service = new ClubMemberServiceImpl(admissionService, tenantMapper, userService, searchClient);
        ReflectionTestUtils.setField(service, "baseMapper", clubMemberMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getStatistics_shouldStayTenantScopedForTenantOneWithoutSuperAdminHeader() {
        when(clubMemberMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        TenantContext.setTenantId(1L);
        TenantContext.setSuperAdmin(false);

        service.getStatistics();

        ArgumentCaptor<Wrapper> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(clubMemberMapper).selectList(wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getCustomSqlSegment();

        verify(userService).reconcilePeopleDomainForCurrentScope();
        assertTrue(sqlSegment.contains("tenant_id"), "tenant-1 member statistics must stay tenant-scoped without super-admin header");
    }

    @Test
    void getStatistics_shouldUseAllActiveTenantsForSuperAdminView() {
        when(clubMemberMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(tenantMapper.selectList(any(Wrapper.class))).thenReturn(List.of(activeTenant(1L), activeTenant(2L)));
        TenantContext.setTenantId(1L);
        TenantContext.setSuperAdmin(true);

        service.getStatistics();

        ArgumentCaptor<Wrapper> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(clubMemberMapper).selectList(wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getCustomSqlSegment();

        verify(userService, atLeastOnce()).reconcilePeopleDomainForCurrentScope();
        verify(tenantMapper).selectList(any(Wrapper.class));
        assertTrue(sqlSegment.contains("IN"), "super-admin member statistics should expand to all active tenants");
    }

    @Test
    void getAllApplicationsWithTenantId_shouldNotExpandSuperAdminQueryAcrossTenants() {
        when(clubMemberMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        TenantContext.setTenantId(30L);
        TenantContext.setSuperAdmin(true);

        service.getAllApplications(1L);

        ArgumentCaptor<Wrapper> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(clubMemberMapper).selectList(wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getCustomSqlSegment();

        assertTrue(sqlSegment.contains("tenant_id"), "explicit tenant member list should include tenant_id condition");
        assertFalse(sqlSegment.contains("IN"), "explicit tenant member list must not expand to all active tenants");
        assertTrue(TenantContext.isSuperAdmin(), "caller super-admin context should be restored after scoped query");
    }

    private RKTenant activeTenant(Long id) {
        RKTenant tenant = new RKTenant();
        tenant.setId(id);
        tenant.setStatus(1);
        tenant.setIsDeleted(0);
        return tenant;
    }
}
