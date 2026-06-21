package com.tianji.auth.service.impl;

import com.tianji.auth.domain.po.Role;
import com.tianji.auth.mapper.RoleMapper;
import com.tianji.auth.service.IRoleMenuService;
import com.tianji.auth.service.IRolePrivilegeService;
import com.tianji.auth.util.PrivilegeCache;
import com.tianji.common.utils.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTenantScopeTest {

    @Mock
    private RoleMapper roleMapper;
    @Mock
    private IRoleMenuService roleMenuService;
    @Mock
    private IRolePrivilegeService rolePrivilegeService;
    @Mock
    private PrivilegeCache privilegeCache;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void queryRoleByIdAcrossTenants_shouldTemporarilyBypassTenantIsolation() {
        RoleServiceImpl service = new RoleServiceImpl(roleMenuService, rolePrivilegeService, privilegeCache);
        ReflectionTestUtils.setField(service, "baseMapper", roleMapper);

        Role tenantRole = new Role();
        tenantRole.setId(153L);
        tenantRole.setTenantId(30L);
        tenantRole.setCode("CLUB_MANAGER");
        tenantRole.setName("\u793e\u56e2\u8d1f\u8d23\u4eba");

        TenantContext.setTenantId(1L);
        TenantContext.setSuperAdmin(false);
        when(roleMapper.selectById(153L)).thenAnswer(invocation -> {
            assertNull(TenantContext.getTenantId());
            assertTrue(TenantContext.isSuperAdmin());
            return tenantRole;
        });

        Role result = service.queryRoleByIdAcrossTenants(153L);

        assertSame(tenantRole, result);
        assertEquals(1L, TenantContext.getTenantId());
        assertFalse(TenantContext.isSuperAdmin());
    }
}
