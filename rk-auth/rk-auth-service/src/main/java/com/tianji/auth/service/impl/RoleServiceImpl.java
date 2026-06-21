package com.tianji.auth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.dto.auth.RoleDTO;
import com.tianji.auth.domain.po.Role;
import com.tianji.auth.domain.po.RoleMenu;
import com.tianji.auth.domain.po.RolePrivilege;
import com.tianji.auth.mapper.RoleMapper;
import com.tianji.auth.service.IRoleMenuService;
import com.tianji.auth.service.IRolePrivilegeService;
import com.tianji.auth.service.IRoleService;
import com.tianji.auth.util.PrivilegeCache;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * <p>
 * 角色表 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2022-06-16
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements IRoleService {

    private final IRoleMenuService roleMenuService;
    private final IRolePrivilegeService rolePrivilegeService;
    private final PrivilegeCache privilegeCache;

    private static final List<RoleSeedDefinition> REQUIRED_TENANT_ROLES = List.of(
            new RoleSeedDefinition("ADMIN", "管理员"),
            new RoleSeedDefinition("USER", "普通用户"),
            new RoleSeedDefinition("CLUB_MANAGER", "社团负责人"),
            new RoleSeedDefinition("TEACHER", "指导老师")
    );

    @Override
    public boolean exists(Long roleId) {
        Integer count = lambdaQuery().eq(Role::getId, roleId).count();
        return count > 0;
    }

    @Override
    public boolean exists(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return true;
        }
        List<Long> distinctRoleIds = roleIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        boolean hasInvalidRoleId = roleIds.stream().anyMatch(id -> id == null || id <= 0);
        if (hasInvalidRoleId) {
            return false;
        }
        Integer count = lambdaQuery().in(Role::getId, distinctRoleIds).count();
        return count == distinctRoleIds.size();
    }

    @Override
    @Transactional
    public List<Role> listAssignableRoles(Long tenantId) {
        Long scopedTenantId = resolveTenantScope(tenantId);
        ensureTenantBaseRoles(scopedTenantId);
        repairLegacyRoleTenantScope(scopedTenantId);
        return lambdaQuery()
                .select(Role::getId, Role::getTenantId, Role::getCode, Role::getRoleCode, Role::getName, Role::getType, Role::getDepId)
                .eq(Role::getDepId, scopedTenantId)
                .orderByAsc(Role::getId)
                .list();
    }

    @Override
    public Role queryAccessibleRoleById(Long id) {
        if (id == null) {
            return null;
        }
        return requireAccessibleRole(id);
    }

    @Override
    public Role queryRoleByIdAcrossTenants(Long id) {
        if (id == null) {
            return null;
        }
        return executeWithoutTenantIsolation(() -> getById(id));
    }

    @Override
    public Role requireAccessibleRole(Long roleId) {
        if (roleId == null) {
            throw new BadRequestException("角色不存在");
        }
        Role role = executeWithoutTenantIsolation(() -> getById(roleId));
        if (role == null || role.getIsDeleted() != null && role.getIsDeleted() == 1) {
            throw new BadRequestException("角色不存在");
        }
        Long currentTenantId = TenantContext.getTenantId();
        if (!TenantContext.isSuperAdmin()) {
            Long roleTenantId = resolveRoleTenantId(role);
            Long scopedTenantId = currentTenantId == null ? 1L : currentTenantId;
            if (roleTenantId == null || !scopedTenantId.equals(roleTenantId)) {
                throw new BadRequestException("无权操作其他租户角色");
            }
        }
        return role;
    }

    @Override
    @Transactional
    public RoleDTO createTenantRole(RoleDTO roleDTO) {
        if (roleDTO == null) {
            throw new BadRequestException("角色参数不能为空");
        }
        Long scopedTenantId = resolveTenantScope(roleDTO.getTenantId());
        ensureTenantBaseRoles(scopedTenantId);
        String roleCode = normalizeCustomRoleCode(roleDTO.getCode());
        String roleName = roleDTO.getName() == null ? "" : roleDTO.getName().trim();
        if (roleName.isEmpty()) {
            throw new BadRequestException("角色名称不能为空");
        }
        ensureRoleCodeUnique(scopedTenantId, roleCode, null);

        Role role = new Role();
        role.setTenantId(scopedTenantId);
        role.setDepId(scopedTenantId);
        role.setCode(roleCode);
        role.setRoleCode(roleCode);
        role.setName(roleName);
        role.setType(Role.RoleType.CUSTOM.getValue());
        role.setIsDeleted(0);
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        save(role);
        return role.toDTO();
    }

    @Override
    @Transactional
    public void updateTenantRole(Long id, RoleDTO roleDTO) {
        if (roleDTO == null) {
            throw new BadRequestException("角色参数不能为空");
        }
        Role existing = requireAccessibleRole(id);
        if (isBuiltInRole(existing)) {
            throw new BadRequestException("默认角色不能修改");
        }
        Long roleTenantId = resolveRoleTenantId(existing);
        String roleCode = normalizeCustomRoleCode(roleDTO.getCode());
        String roleName = roleDTO.getName() == null ? "" : roleDTO.getName().trim();
        if (roleName.isEmpty()) {
            throw new BadRequestException("角色名称不能为空");
        }
        ensureRoleCodeUnique(roleTenantId, roleCode, id);
        lambdaUpdate()
                .eq(Role::getId, id)
                .set(Role::getCode, roleCode)
                .set(Role::getRoleCode, roleCode)
                .set(Role::getName, roleName)
                .set(Role::getUpdateTime, LocalDateTime.now())
                .update();
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        Role role = requireAccessibleRole(id);
        if (isBuiltInRole(role)) {
            throw new BadRequestException("默认角色不能删除");
        }
        // 1.删除角色
        boolean removed = removeById(id);
        if (!removed) {
            return;
        }
        // 2.删除角色与权限的关联信息
        roleMenuService.removeByRoleId(id);
        rolePrivilegeService.removeByRoleId(id);
        // 3.清理缓存
        privilegeCache.removeCacheByRoleId(id);
    }

    private Long resolveTenantScope(Long tenantId) {
        Long currentTenantId = TenantContext.getTenantId();
        Long targetTenantId = tenantId != null ? tenantId : (currentTenantId == null ? 1L : currentTenantId);
        if (!TenantContext.isSuperAdmin() && currentTenantId != null && !currentTenantId.equals(targetTenantId)) {
            throw new BadRequestException("无权查看其他租户角色");
        }
        return targetTenantId;
    }

    private void repairLegacyRoleTenantScope(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            return;
        }
        lambdaUpdate()
                .eq(Role::getTenantId, tenantId)
                .and(wrapper -> wrapper.isNull(Role::getDepId).or().eq(Role::getDepId, 0L))
                .set(Role::getDepId, tenantId)
                .set(Role::getUpdateTime, LocalDateTime.now())
                .update();
    }

    private void ensureRoleCodeUnique(Long tenantId, String roleCode, Long ignoredRoleId) {
        long count = lambdaQuery()
                .eq(Role::getDepId, tenantId)
                .and(wrapper -> wrapper.eq(Role::getRoleCode, roleCode).or().eq(Role::getCode, roleCode))
                .ne(ignoredRoleId != null, Role::getId, ignoredRoleId)
                .count();
        if (count > 0) {
            throw new BadRequestException("当前租户已存在相同角色标识");
        }
    }

    private String normalizeCustomRoleCode(String code) {
        String normalized = code == null ? "" : code.trim().replaceAll("[^A-Za-z0-9_\\-]", "_");
        if (normalized.isEmpty()) {
            throw new BadRequestException("角色标识不能为空");
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private Long resolveRoleTenantId(Role role) {
        if (role == null) {
            return null;
        }
        if (role.getDepId() != null && role.getDepId() > 0) {
            return role.getDepId();
        }
        return role.getTenantId();
    }

    private boolean isBuiltInRole(Role role) {
        return role != null
                && (Role.RoleType.CONSTANT.getValue() == (role.getType() == null ? Role.RoleType.CUSTOM.getValue() : role.getType())
                || REQUIRED_TENANT_ROLES.stream().anyMatch(seed -> seed.code.equalsIgnoreCase(role.getCode()) || seed.code.equalsIgnoreCase(role.getRoleCode())));
    }

    private void ensureTenantBaseRoles(Long tenantId) {
        for (RoleSeedDefinition seed : REQUIRED_TENANT_ROLES) {
            ensureTenantRole(tenantId, seed);
        }
    }

    private Role ensureTenantRole(Long tenantId, RoleSeedDefinition seed) {
        Role template = loadTemplateRole(seed.code);
        Role existing = lambdaQuery()
                .eq(Role::getDepId, tenantId)
                .and(wrapper -> wrapper.eq(Role::getRoleCode, seed.code).or().eq(Role::getCode, seed.code))
                .last("LIMIT 1")
                .one();
        if (existing != null) {
            if (template != null && !existing.getId().equals(template.getId())) {
                repairRoleBindingsIfMissing(existing.getId(), template.getId());
            }
            return existing;
        }

        Role role = new Role();
        role.setTenantId(tenantId);
        role.setDepId(tenantId);
        role.setCode(seed.code);
        role.setRoleCode(seed.code);
        role.setName(seed.name);
        role.setType(Role.RoleType.CONSTANT.getValue());
        role.setIsDeleted(0);
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        save(role);

        if (template != null) {
            cloneRoleBindings(template.getId(), role.getId());
        }
        return role;
    }

    private Role loadTemplateRole(String code) {
        return executeWithoutTenantIsolation(() -> lambdaQuery()
                .and(wrapper -> wrapper.eq(Role::getRoleCode, code).or().eq(Role::getCode, code))
                .orderByAsc(Role::getId)
                .last("LIMIT 1")
                .one());
    }

    private void cloneRoleBindings(Long sourceRoleId, Long targetRoleId) {
        if (sourceRoleId == null || sourceRoleId.equals(targetRoleId)) {
            return;
        }
        List<RoleMenu> sourceMenus = roleMenuService.lambdaQuery()
                .eq(RoleMenu::getRoleId, sourceRoleId)
                .list();
        if (!sourceMenus.isEmpty()) {
            List<RoleMenu> clonedMenus = new ArrayList<>(sourceMenus.size());
            for (RoleMenu sourceMenu : sourceMenus) {
                clonedMenus.add(new RoleMenu(targetRoleId, sourceMenu.getMenuId()));
            }
            roleMenuService.saveBatch(clonedMenus);
        }

        List<RolePrivilege> sourcePrivileges = rolePrivilegeService.lambdaQuery()
                .eq(RolePrivilege::getRoleId, sourceRoleId)
                .list();
        if (!sourcePrivileges.isEmpty()) {
            List<RolePrivilege> clonedPrivileges = new ArrayList<>(sourcePrivileges.size());
            for (RolePrivilege sourcePrivilege : sourcePrivileges) {
                clonedPrivileges.add(new RolePrivilege(targetRoleId, sourcePrivilege.getPrivilegeId()));
            }
            rolePrivilegeService.saveBatch(clonedPrivileges);
        }
    }

    private void repairRoleBindingsIfMissing(Long targetRoleId, Long templateRoleId) {
        long menuCount = roleMenuService.lambdaQuery().eq(RoleMenu::getRoleId, targetRoleId).count();
        if (menuCount == 0) {
            List<RoleMenu> templateMenus = roleMenuService.lambdaQuery()
                    .eq(RoleMenu::getRoleId, templateRoleId)
                    .list();
            if (!templateMenus.isEmpty()) {
                List<RoleMenu> repairedMenus = new ArrayList<>(templateMenus.size());
                for (RoleMenu templateMenu : templateMenus) {
                    repairedMenus.add(new RoleMenu(targetRoleId, templateMenu.getMenuId()));
                }
                roleMenuService.saveBatch(repairedMenus);
            }
        }

        long privilegeCount = rolePrivilegeService.lambdaQuery().eq(RolePrivilege::getRoleId, targetRoleId).count();
        if (privilegeCount == 0) {
            List<RolePrivilege> templatePrivileges = rolePrivilegeService.lambdaQuery()
                    .eq(RolePrivilege::getRoleId, templateRoleId)
                    .list();
            if (!templatePrivileges.isEmpty()) {
                List<RolePrivilege> repairedPrivileges = new ArrayList<>(templatePrivileges.size());
                for (RolePrivilege templatePrivilege : templatePrivileges) {
                    repairedPrivileges.add(new RolePrivilege(targetRoleId, templatePrivilege.getPrivilegeId()));
                }
                rolePrivilegeService.saveBatch(repairedPrivileges);
            }
        }
    }

    private <T> T executeWithoutTenantIsolation(Supplier<T> supplier) {
        Long previousTenantId = TenantContext.getTenantId();
        Boolean previousSuperAdmin = TenantContext.isSuperAdmin();
        try {
            TenantContext.setSuperAdmin(true);
            TenantContext.removeTenantId();
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

    private static final class RoleSeedDefinition {
        private final String code;
        private final String name;

        private RoleSeedDefinition(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }
}
