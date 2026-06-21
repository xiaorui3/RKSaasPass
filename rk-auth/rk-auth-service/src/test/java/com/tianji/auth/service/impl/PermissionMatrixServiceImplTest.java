package com.tianji.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tianji.auth.domain.po.Menu;
import com.tianji.auth.domain.po.Role;
import com.tianji.auth.domain.po.RoleMenu;
import com.tianji.auth.domain.vo.PermissionMatrixVO;
import com.tianji.auth.service.IMenuService;
import com.tianji.auth.service.IRoleMenuService;
import com.tianji.auth.service.IRoleService;
import com.tianji.common.utils.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionMatrixServiceImplTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void buildMatrix_shouldAggregateTenantRolesMenusBindingsAndCoverage() {
        IRoleService roleService = mock(IRoleService.class);
        IMenuService menuService = mock(IMenuService.class);
        IRoleMenuService roleMenuService = mock(IRoleMenuService.class);
        PermissionMatrixServiceImpl service = new PermissionMatrixServiceImpl(roleService, menuService, roleMenuService);

        Role admin = role(10L, "ADMIN", "\u7ba1\u7406\u5458", 1L, 0);
        Role custom = role(11L, "CLUB_REVIEWER", "\u793e\u56e2\u5ba1\u6838\u5458", 1L, 1);
        Menu system = menu(100L, 0L, "\u7cfb\u7edf\u7ba1\u7406", "admin", "/admin/system", 1);
        Menu users = menu(101L, 100L, "\u7528\u6237\u7ba1\u7406", "admin_users", "/admin/system/users", 2);
        Menu roles = menu(102L, 100L, "\u89d2\u8272\u7ba1\u7406", "admin_roles", "/admin/system/roles", 3);

        when(roleService.listAssignableRoles(1L)).thenReturn(List.of(admin, custom));
        when(menuService.list()).thenReturn(List.of(system, users, roles));
        when(roleMenuService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                new RoleMenu(10L, 100L),
                new RoleMenu(10L, 101L),
                new RoleMenu(11L, 102L)
        ));

        PermissionMatrixVO matrix = service.buildMatrix(1L);

        assertEquals(1L, matrix.getTenantId());
        assertEquals(2, matrix.getRoles().size());
        assertEquals(3, matrix.getMenus().size());
        assertEquals(6, matrix.getCells().size());
        assertEquals(3, matrix.getSummary().getAssignedCount());
        assertEquals(3, matrix.getSummary().getUnassignedCount());
        assertEquals("50.00%", matrix.getSummary().getCoverageRate());

        PermissionMatrixVO.RoleColumn adminColumn = matrix.getRoles().get(0);
        assertEquals(2, adminColumn.getAssignedCount());
        assertEquals("66.67%", adminColumn.getCoverageRate());
        assertTrue(adminColumn.getAssignedMenuIds().contains(101L));

        PermissionMatrixVO.MenuRow rolesRow = matrix.getMenus().stream()
                .filter(item -> item.getId().equals(102L))
                .findFirst()
                .orElseThrow();
        assertEquals(1, rolesRow.getAssignedRoleCount());
        assertEquals("50.00%", rolesRow.getCoverageRate());

        PermissionMatrixVO.MatrixCell allowed = matrix.getCells().stream()
                .filter(item -> item.getRoleId().equals(10L) && item.getMenuId().equals(101L))
                .findFirst()
                .orElseThrow();
        PermissionMatrixVO.MatrixCell denied = matrix.getCells().stream()
                .filter(item -> item.getRoleId().equals(11L) && item.getMenuId().equals(101L))
                .findFirst()
                .orElseThrow();
        assertTrue(allowed.isAssigned());
        assertFalse(denied.isAssigned());
    }

    private Role role(Long id, String code, String name, Long tenantId, Integer type) {
        Role role = new Role();
        role.setId(id);
        role.setCode(code);
        role.setRoleCode(code);
        role.setName(name);
        role.setTenantId(tenantId);
        role.setDepId(tenantId);
        role.setType(type);
        return role;
    }

    private Menu menu(Long id, Long parentId, String label, String code, String path, Integer priority) {
        Menu menu = new Menu();
        menu.setId(id);
        menu.setParentId(parentId);
        menu.setLabel(label);
        menu.setMenuCode(code);
        menu.setPath(path);
        menu.setPriority(priority);
        menu.setVisible(1);
        menu.setStatus(1);
        return menu;
    }
}
