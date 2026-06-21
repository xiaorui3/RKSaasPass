package com.tianji.auth.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.auth.constants.AuthConstants;
import com.tianji.auth.domain.po.AccountRole;
import com.tianji.auth.domain.po.Menu;
import com.tianji.auth.domain.po.Privilege;
import com.tianji.auth.domain.po.Role;
import com.tianji.auth.domain.po.RoleMenu;
import com.tianji.auth.domain.po.RolePrivilege;
import com.tianji.auth.mapper.MenuMapper;
import com.tianji.auth.service.IPrivilegeService;
import com.tianji.auth.service.IAccountRoleService;
import com.tianji.auth.service.IMenuService;
import com.tianji.auth.service.IRoleMenuService;
import com.tianji.auth.service.IRolePrivilegeService;
import com.tianji.auth.service.IRoleService;
import com.tianji.auth.util.PrivilegeCache;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.CommonException;
import com.tianji.common.utils.RoleContext;
import com.tianji.common.utils.TenantContext;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static com.tianji.auth.common.constants.AuthErrorInfo.Msg.MENU_NOT_FOUND;
import static com.tianji.auth.common.constants.AuthErrorInfo.Msg.ROLE_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements IMenuService {

    private final IRoleMenuService roleMenuService;
    private final IRoleService roleService;
    private final IAccountRoleService accountRoleService;
    private final IPrivilegeService privilegeService;
    private final IRolePrivilegeService rolePrivilegeService;
    private final PrivilegeCache privilegeCache;

    @Override
    public List<Menu> listMenuByUser() {
        Long userId = UserContext.getUser();
        List<AccountRole> accountRoles = accountRoleService.lambdaQuery().eq(AccountRole::getAccountId, userId).list();
        if (CollUtil.isEmpty(accountRoles)) {
            return Collections.emptyList();
        }
        Long currentTenantId = TenantContext.getTenantId() == null ? 1L : TenantContext.getTenantId();
        Long currentRoleId = RoleContext.getRoleId();
        List<Long> rawRoleIds = accountRoles.stream()
                .map(AccountRole::getRoleId)
                .filter(roleId -> roleId != null && roleId > 0)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(rawRoleIds)) {
            return Collections.emptyList();
        }
        if (currentRoleId != null && rawRoleIds.contains(currentRoleId)) {
            rawRoleIds = Collections.singletonList(currentRoleId);
        } else if (currentRoleId != null) {
            return Collections.emptyList();
        }
        List<Long> roleIds = roleService.lambdaQuery()
                .in(Role::getId, rawRoleIds)
                .and(wrapper -> wrapper.eq(Role::getDepId, currentTenantId)
                        .or()
                        .eq(Role::getTenantId, currentTenantId))
                .list()
                .stream()
                .map(Role::getId)
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        return getBaseMapper().listByRoles(roleIds);
    }

    @Override
    public List<Long> listRoleMenuIds(Long roleId) {
        ensureRoleAccessible(roleId);
        return roleMenuService.lambdaQuery()
                .eq(RoleMenu::getRoleId, roleId)
                .list()
                .stream()
                .map(RoleMenu::getMenuId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void saveMenu(Menu menu) {
        ensureSuperAdminMenuCatalogAccess();
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        if (menu.getMenuType() == null) {
            menu.setMenuType(menu.getParentId() == 0 ? 1 : 2);
        }
        if (menu.getVisible() == null) {
            menu.setVisible(1);
        }
        if (menu.getStatus() == null) {
            menu.setStatus(1);
        }
        if (menu.getPriority() == null) {
            menu.setPriority(0);
        }
        if (menu.getMenuCode() == null || menu.getMenuCode().isBlank()) {
            String source = menu.getPath() != null && !menu.getPath().isBlank() ? menu.getPath() : menu.getLabel();
            String normalized = source == null ? "menu" : source.replaceAll("[^a-zA-Z0-9]+", "_").toLowerCase(Locale.ROOT);
            menu.setMenuCode(normalized + "_" + System.currentTimeMillis());
        }
        save(menu);
        RoleMenu roleMenu = new RoleMenu();
        roleMenu.setMenuId(menu.getId());
        roleMenu.setRoleId(AuthConstants.ADMIN_ROLE_ID);
        roleMenuService.save(roleMenu);
    }

    @Override
    @Transactional
    public void updateMenu(Menu menu) {
        ensureSuperAdminMenuCatalogAccess();
        updateById(menu);
    }

    @Override
    @Transactional
    public void deleteMenu(Long id) {
        ensureSuperAdminMenuCatalogAccess();
        Menu menu = getById(id);
        if (menu == null) {
            return;
        }

        List<Long> delIds;
        long childCount = lambdaQuery().eq(Menu::getParentId, id).count();
        if (childCount > 0) {
            delIds = lambdaQuery()
                    .eq(Menu::getParentId, id)
                    .list()
                    .stream()
                    .map(Menu::getId)
                    .collect(Collectors.toList());
            delIds.add(id);
        } else {
            delIds = Collections.singletonList(id);
        }

        removeByIds(delIds);
        roleMenuService.remove(new LambdaQueryWrapper<RoleMenu>().in(RoleMenu::getMenuId, delIds));
    }

    @Override
    @Transactional
    public void replaceRoleMenus(Long roleId, List<Long> menuIds) {
        Role role = ensureRoleAccessible(roleId);
        if (CollUtil.isEmpty(menuIds)) {
            roleMenuService.removeByRoleId(roleId);
            rolePrivilegeService.removeByRoleId(roleId);
            privilegeCache.initPrivilegesCache(privilegeService.listPrivilegeRoles());
            return;
        }

        List<Long> distinctMenuIds = menuIds.stream().distinct().collect(Collectors.toList());
        List<Menu> menus = lambdaQuery().in(Menu::getId, distinctMenuIds).list();
        if (menus.size() != distinctMenuIds.size()) {
            throw new CommonException(MENU_NOT_FOUND);
        }
        ensureMenusAssignableToRole(role, menuIds);

        roleMenuService.removeByRoleId(roleId);
        rolePrivilegeService.removeByRoleId(roleId);

        List<RoleMenu> roleMenus = new ArrayList<>(distinctMenuIds.size());
        for (Long menuId : distinctMenuIds) {
            roleMenus.add(new RoleMenu(roleId, menuId));
        }
        roleMenuService.saveBatch(roleMenus);

        List<Privilege> privileges = privilegeService.lambdaQuery()
                .in(Privilege::getMenuId, distinctMenuIds)
                .list();
        if (!privileges.isEmpty()) {
            List<RolePrivilege> rolePrivileges = new ArrayList<>(privileges.size());
            for (Privilege privilege : privileges) {
                rolePrivileges.add(new RolePrivilege(roleId, privilege.getId()));
            }
            rolePrivilegeService.saveBatch(rolePrivileges);
        }

        privilegeCache.initPrivilegesCache(privilegeService.listPrivilegeRoles());
    }

    @Override
    @Transactional
    public void bindRoleMenus(Long roleId, List<Long> menuIds) {
        Role role = ensureRoleAccessible(roleId);
        if (CollUtil.isEmpty(menuIds)) {
            return;
        }
        List<Long> distinctMenuIds = menuIds.stream().distinct().collect(Collectors.toList());
        Integer menuCount = lambdaQuery().in(Menu::getId, distinctMenuIds).count();
        if (menuCount != distinctMenuIds.size()) {
            throw new CommonException(MENU_NOT_FOUND);
        }
        ensureMenusAssignableToRole(role, menuIds);
        List<RoleMenu> roleMenus = new ArrayList<>(distinctMenuIds.size());
        for (Long menuId : distinctMenuIds) {
            roleMenus.add(new RoleMenu(roleId, menuId));
        }
        roleMenuService.saveBatch(roleMenus);
        privilegeCache.initPrivilegesCache(privilegeService.listPrivilegeRoles());
    }

    @Override
    @Transactional
    public void deleteRoleMenus(Long roleId, List<Long> menuIds) {
        ensureRoleAccessible(roleId);
        if (CollUtil.isEmpty(menuIds)) {
            return;
        }
        List<Long> privilegeIds = privilegeService.lambdaQuery()
                .in(Privilege::getMenuId, menuIds)
                .list()
                .stream()
                .map(Privilege::getId)
                .collect(Collectors.toList());
        rolePrivilegeService.deleteRolePrivileges(roleId, privilegeIds);
        roleMenuService.deleteRoleMenus(roleId, menuIds);
        privilegeCache.initPrivilegesCache(privilegeService.listPrivilegeRoles());
    }

    private Role ensureRoleAccessible(Long roleId) {
        try {
            return roleService.requireAccessibleRole(roleId);
        } catch (BadRequestException e) {
            throw new CommonException(ROLE_NOT_FOUND);
        }
    }

    private void ensureMenusAssignableToRole(Role role, List<Long> menuIds) {
        if (TenantContext.isSuperAdmin() || CollUtil.isEmpty(menuIds)) {
            return;
        }
        List<Menu> menus = lambdaQuery().in(Menu::getId, menuIds).list();
        boolean hasSuperAdminOnlyOperationMenu = menus.stream().anyMatch(menu -> isSuperAdminOnlyOperationMenu(menu));
        if (hasSuperAdminOnlyOperationMenu) {
            throw new BadRequestException("Tenant roles cannot bind super-admin operation menus");
        }
    }

    private boolean isSuperAdminOnlyOperationMenu(Menu menu) {
        if (menu == null) {
            return false;
        }
        if (Long.valueOf(700L).equals(menu.getId()) || Long.valueOf(700L).equals(menu.getParentId())) {
            return true;
        }
        String path = menu.getPath();
        return path != null && path.startsWith("/admin/operation/");
    }

    private void ensureSuperAdminMenuCatalogAccess() {
        if (!TenantContext.isSuperAdmin()) {
            throw new BadRequestException("只有超级管理员可以维护菜单目录");
        }
    }
}
