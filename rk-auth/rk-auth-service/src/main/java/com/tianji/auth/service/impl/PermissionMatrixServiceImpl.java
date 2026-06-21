package com.tianji.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tianji.auth.domain.po.Menu;
import com.tianji.auth.domain.po.Role;
import com.tianji.auth.domain.po.RoleMenu;
import com.tianji.auth.domain.vo.PermissionMatrixVO;
import com.tianji.auth.service.IMenuService;
import com.tianji.auth.service.IPermissionMatrixService;
import com.tianji.auth.service.IRoleMenuService;
import com.tianji.auth.service.IRoleService;
import com.tianji.common.utils.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionMatrixServiceImpl implements IPermissionMatrixService {

    private final IRoleService roleService;
    private final IMenuService menuService;
    private final IRoleMenuService roleMenuService;

    @Override
    public PermissionMatrixVO buildMatrix(Long tenantId) {
        Long scopedTenantId = resolveTenantId(tenantId);
        List<Role> roles = roleService.listAssignableRoles(scopedTenantId);
        List<Menu> menus = menuService.list()
                .stream()
                .filter(this::isVisibleMenu)
                .sorted(Comparator
                        .comparing((Menu menu) -> menu.getParentId() == null ? 0L : menu.getParentId())
                        .thenComparing(menu -> menu.getPriority() == null ? 0 : menu.getPriority())
                        .thenComparing(menu -> menu.getId() == null ? 0L : menu.getId()))
                .collect(Collectors.toList());

        Set<Long> roleIds = roles.stream().map(Role::getId).collect(Collectors.toSet());
        Set<Long> menuIds = menus.stream().map(Menu::getId).collect(Collectors.toSet());
        Map<Long, Set<Long>> roleMenuMap = loadRoleMenuMap(roleIds, menuIds);
        List<Menu> flatMenus = flattenMenus(menus);

        PermissionMatrixVO matrix = new PermissionMatrixVO();
        matrix.setTenantId(scopedTenantId);
        matrix.setRoles(buildRoleColumns(roles, roleMenuMap, flatMenus.size()));
        matrix.setMenus(buildMenuRows(flatMenus, roleMenuMap, roles.size()));
        matrix.setCells(buildCells(roles, flatMenus, roleMenuMap));
        matrix.setSummary(buildSummary(matrix.getRoles(), matrix.getMenus(), matrix.getCells()));
        return matrix;
    }

    private Long resolveTenantId(Long tenantId) {
        Long currentTenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return tenantId;
        }
        return currentTenantId == null ? 1L : currentTenantId;
    }

    private boolean isVisibleMenu(Menu menu) {
        if (menu == null || menu.getId() == null) {
            return false;
        }
        boolean visible = menu.getVisible() == null || menu.getVisible() == 1;
        boolean enabled = menu.getStatus() == null || menu.getStatus() == 1;
        return visible && enabled;
    }

    private Map<Long, Set<Long>> loadRoleMenuMap(Set<Long> roleIds, Set<Long> menuIds) {
        if (roleIds.isEmpty() || menuIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<RoleMenu> bindings = roleMenuService.list(new LambdaQueryWrapper<RoleMenu>()
                .in(RoleMenu::getRoleId, roleIds)
                .in(RoleMenu::getMenuId, menuIds));
        Map<Long, Set<Long>> result = new HashMap<>();
        for (RoleMenu binding : bindings) {
            if (binding.getRoleId() == null || binding.getMenuId() == null) {
                continue;
            }
            result.computeIfAbsent(binding.getRoleId(), ignored -> new HashSet<>()).add(binding.getMenuId());
        }
        return result;
    }

    private List<Menu> flattenMenus(List<Menu> menus) {
        Map<Long, List<Menu>> childrenByParentId = menus.stream()
                .collect(Collectors.groupingBy(menu -> menu.getParentId() == null ? 0L : menu.getParentId(), LinkedHashMap::new, Collectors.toList()));
        for (List<Menu> children : childrenByParentId.values()) {
            children.sort(Comparator
                    .comparing((Menu menu) -> menu.getPriority() == null ? 0 : menu.getPriority())
                    .thenComparing(menu -> menu.getId() == null ? 0L : menu.getId()));
        }
        List<Menu> result = new ArrayList<>();
        appendMenuTree(result, childrenByParentId, 0L);
        Set<Long> includedIds = result.stream().map(Menu::getId).collect(Collectors.toSet());
        menus.stream()
                .filter(menu -> !includedIds.contains(menu.getId()))
                .sorted(Comparator
                        .comparing((Menu menu) -> menu.getPriority() == null ? 0 : menu.getPriority())
                        .thenComparing(menu -> menu.getId() == null ? 0L : menu.getId()))
                .forEach(result::add);
        return result;
    }

    private void appendMenuTree(List<Menu> result, Map<Long, List<Menu>> childrenByParentId, Long parentId) {
        List<Menu> children = childrenByParentId.get(parentId);
        if (children == null || children.isEmpty()) {
            return;
        }
        for (Menu child : children) {
            result.add(child);
            appendMenuTree(result, childrenByParentId, child.getId());
        }
    }

    private List<PermissionMatrixVO.RoleColumn> buildRoleColumns(List<Role> roles, Map<Long, Set<Long>> roleMenuMap, int menuCount) {
        List<PermissionMatrixVO.RoleColumn> columns = new ArrayList<>();
        for (Role role : roles) {
            Set<Long> assignedMenuIds = roleMenuMap.getOrDefault(role.getId(), Collections.emptySet());
            PermissionMatrixVO.RoleColumn column = new PermissionMatrixVO.RoleColumn();
            column.setId(role.getId());
            column.setTenantId(resolveRoleTenantId(role));
            column.setCode(role.getCode());
            column.setRoleCode(role.getRoleCode());
            column.setName(role.getName());
            column.setType(role.getType());
            column.setBuiltIn(isBuiltInRole(role));
            column.setAssignedMenuIds(new ArrayList<>(assignedMenuIds));
            column.setAssignedCount(assignedMenuIds.size());
            column.setTotalMenuCount(menuCount);
            column.setCoverageRate(coverageRate(assignedMenuIds.size(), menuCount));
            columns.add(column);
        }
        return columns;
    }

    private List<PermissionMatrixVO.MenuRow> buildMenuRows(List<Menu> menus, Map<Long, Set<Long>> roleMenuMap, int roleCount) {
        Map<Long, Menu> menuById = menus.stream().collect(Collectors.toMap(Menu::getId, menu -> menu, (left, right) -> left));
        List<PermissionMatrixVO.MenuRow> rows = new ArrayList<>();
        for (Menu menu : menus) {
            List<Long> assignedRoleIds = roleMenuMap.entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().contains(menu.getId()))
                    .map(Map.Entry::getKey)
                    .sorted()
                    .collect(Collectors.toList());
            PermissionMatrixVO.MenuRow row = new PermissionMatrixVO.MenuRow();
            row.setId(menu.getId());
            row.setParentId(menu.getParentId() == null ? 0L : menu.getParentId());
            row.setLabel(menu.getLabel());
            row.setMenuCode(menu.getMenuCode());
            row.setMenuType(menu.getMenuType());
            row.setPath(menu.getPath());
            row.setIcon(menu.getIcon());
            row.setPriority(menu.getPriority());
            row.setVisible(menu.getVisible());
            row.setStatus(menu.getStatus());
            row.setDepth(resolveDepth(menu, menuById));
            row.setFullPath(resolveFullPath(menu, menuById));
            row.setAssignedRoleIds(assignedRoleIds);
            row.setAssignedRoleCount(assignedRoleIds.size());
            row.setTotalRoleCount(roleCount);
            row.setCoverageRate(coverageRate(assignedRoleIds.size(), roleCount));
            rows.add(row);
        }
        return rows;
    }

    private List<PermissionMatrixVO.MatrixCell> buildCells(List<Role> roles, List<Menu> menus, Map<Long, Set<Long>> roleMenuMap) {
        List<PermissionMatrixVO.MatrixCell> cells = new ArrayList<>(roles.size() * menus.size());
        for (Role role : roles) {
            Set<Long> assignedMenuIds = roleMenuMap.getOrDefault(role.getId(), Collections.emptySet());
            for (Menu menu : menus) {
                PermissionMatrixVO.MatrixCell cell = new PermissionMatrixVO.MatrixCell();
                cell.setRoleId(role.getId());
                cell.setMenuId(menu.getId());
                cell.setAssigned(assignedMenuIds.contains(menu.getId()));
                cells.add(cell);
            }
        }
        return cells;
    }

    private PermissionMatrixVO.Summary buildSummary(
            List<PermissionMatrixVO.RoleColumn> roles,
            List<PermissionMatrixVO.MenuRow> menus,
            List<PermissionMatrixVO.MatrixCell> cells
    ) {
        int assignedCount = (int) cells.stream().filter(PermissionMatrixVO.MatrixCell::isAssigned).count();
        int totalCount = cells.size();
        PermissionMatrixVO.Summary summary = new PermissionMatrixVO.Summary();
        summary.setRoleCount(roles.size());
        summary.setMenuCount(menus.size());
        summary.setAssignedCount(assignedCount);
        summary.setUnassignedCount(totalCount - assignedCount);
        summary.setFullyCoveredRoleCount((int) roles.stream()
                .filter(role -> role.getTotalMenuCount() > 0 && role.getAssignedCount() == role.getTotalMenuCount())
                .count());
        summary.setUncoveredMenuCount((int) menus.stream()
                .filter(menu -> menu.getAssignedRoleCount() == 0)
                .count());
        summary.setCoverageRate(coverageRate(assignedCount, totalCount));
        summary.setRoleCoverageRate(coverageRate(summary.getFullyCoveredRoleCount(), roles.size()));
        summary.setMenuCoverageRate(coverageRate(menus.size() - summary.getUncoveredMenuCount(), menus.size()));
        return summary;
    }

    private int resolveDepth(Menu menu, Map<Long, Menu> menuById) {
        int depth = 0;
        Long parentId = menu.getParentId();
        Set<Long> visited = new HashSet<>();
        while (parentId != null && parentId > 0 && menuById.containsKey(parentId) && visited.add(parentId)) {
            depth++;
            parentId = menuById.get(parentId).getParentId();
        }
        return depth;
    }

    private String resolveFullPath(Menu menu, Map<Long, Menu> menuById) {
        List<String> labels = new ArrayList<>();
        Menu current = menu;
        Set<Long> visited = new HashSet<>();
        while (current != null && current.getId() != null && visited.add(current.getId())) {
            labels.add(current.getLabel());
            Long parentId = current.getParentId();
            current = parentId == null ? null : menuById.get(parentId);
        }
        Collections.reverse(labels);
        return labels.stream().filter(label -> label != null && !label.isBlank()).collect(Collectors.joining(" / "));
    }

    private Long resolveRoleTenantId(Role role) {
        if (role.getDepId() != null && role.getDepId() > 0) {
            return role.getDepId();
        }
        return role.getTenantId();
    }

    private boolean isBuiltInRole(Role role) {
        Integer type = role.getType();
        String code = role.getCode() == null ? "" : role.getCode().trim().toUpperCase();
        String roleCode = role.getRoleCode() == null ? "" : role.getRoleCode().trim().toUpperCase();
        return Integer.valueOf(0).equals(type)
                || "ADMIN".equals(code)
                || "USER".equals(code)
                || "CLUB_MANAGER".equals(code)
                || "TEACHER".equals(code)
                || "ADMIN".equals(roleCode)
                || "USER".equals(roleCode)
                || "CLUB_MANAGER".equals(roleCode)
                || "TEACHER".equals(roleCode);
    }

    private String coverageRate(int numerator, int denominator) {
        if (denominator <= 0) {
            return "0.00%";
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP)
                .toPlainString() + "%";
    }
}
