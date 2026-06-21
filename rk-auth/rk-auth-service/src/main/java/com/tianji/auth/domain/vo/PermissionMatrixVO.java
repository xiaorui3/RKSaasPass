package com.tianji.auth.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PermissionMatrixVO {

    private Long tenantId;
    private List<RoleColumn> roles = new ArrayList<>();
    private List<MenuRow> menus = new ArrayList<>();
    private List<MatrixCell> cells = new ArrayList<>();
    private Summary summary = new Summary();

    @Data
    public static class RoleColumn {
        private Long id;
        private Long tenantId;
        private String code;
        private String roleCode;
        private String name;
        private Integer type;
        private boolean builtIn;
        private List<Long> assignedMenuIds = new ArrayList<>();
        private int assignedCount;
        private int totalMenuCount;
        private String coverageRate;
    }

    @Data
    public static class MenuRow {
        private Long id;
        private Long parentId;
        private String label;
        private String menuCode;
        private Integer menuType;
        private String path;
        private String icon;
        private Integer priority;
        private Integer visible;
        private Integer status;
        private int depth;
        private String fullPath;
        private List<Long> assignedRoleIds = new ArrayList<>();
        private int assignedRoleCount;
        private int totalRoleCount;
        private String coverageRate;
    }

    @Data
    public static class MatrixCell {
        private Long roleId;
        private Long menuId;
        private boolean assigned;
    }

    @Data
    public static class Summary {
        private int roleCount;
        private int menuCount;
        private int assignedCount;
        private int unassignedCount;
        private int fullyCoveredRoleCount;
        private int uncoveredMenuCount;
        private String coverageRate;
        private String roleCoverageRate;
        private String menuCoverageRate;
    }
}
