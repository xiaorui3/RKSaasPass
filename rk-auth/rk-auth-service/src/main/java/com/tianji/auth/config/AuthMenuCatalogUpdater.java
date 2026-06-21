package com.tianji.auth.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthMenuCatalogUpdater {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private static final Set<String> BUILT_IN_ROLE_CODES = Set.of("ADMIN", "CLUB_MANAGER", "TEACHER", "USER");
    private static final Set<String> SYNC_MENU_NAME_CODES = Set.of(
            "admin_operation_notification_center",
            "admin_operation_rainbond",
            "admin_operation_deploy_package",
            "admin_operation_release_center",
            "admin_operation_remote_migration",
            "admin_operation_visual_screen",
            "admin_operation_redis_cache",
            "admin_operation_minio_browser",
            "admin_statistics_tenant_operations",
            "admin_statistics_approval_center",
            "admin_statistics_data_quality",
            "admin_statistics_security_audit",
            "admin_statistics_participation",
            "admin_statistics_growth_retention",
            "admin_statistics_content_calendar",
            "admin_permission_matrix",
            "admin_open_source_health",
            "admin_api_catalog",
            "admin_demo_data_center",
            "admin_operation_logs",
            "admin_finance",
            "admin_finance_dashboard",
            "admin_finance_budget",
            "admin_finance_allocation",
            "admin_finance_reimbursement",
            "admin_finance_voucher",
            "admin_finance_ledger",
            "admin_finance_report",
            "admin_finance_audit"
    );
    private static final String SYNC_MENU_NAME_CODE_SQL = SYNC_MENU_NAME_CODES.stream()
            .map(code -> "'" + code + "'")
            .collect(Collectors.joining(","));

    @PostConstruct
    public void syncCurrentMenuCatalog() {
        List<MenuSeed> menus = currentMenus();
        transactionTemplate.executeWithoutResult(status -> {
            for (MenuSeed menu : menus) {
                upsertMenu(menu);
            }
            deactivateLegacyMenus(menus);
            deactivateKnownLegacyAdminMenus(menus);
            removeDeletedMenuBindings();
            syncRoleBindings(menus);
        });
        log.info("auth menu catalog synchronized, menuCount={}", menus.size());
    }

    static List<MenuSeed> currentMenus() {
        List<MenuSeed> menus = new ArrayList<>();

        menus.add(menu(100L, 0L, "系统管理", "admin", 1, "/admin/system", "Setting", 1, tenantAdmin()));
        menus.add(menu(102L, 100L, "用户管理", "admin_users", 2, "/admin/system/users", "UserFilled", 1, tenantAdmin()));
        menus.add(menu(103L, 100L, "角色管理", "admin_roles", 2, "/admin/system/roles", "User", 2, tenantAdmin()));
        menus.add(menu(104L, 100L, "菜单管理", "admin_menus", 2, "/admin/system/menus", "Document", 3, tenantAdmin()));
        menus.add(menu(101L, 100L, "租户管理", "admin_tenants", 2, "/admin/system/tenants", "Setting", 4, tenantAdmin()));
        menus.add(menu(105L, 100L, "系统配置", "admin_config", 2, "/admin/system/config", "Setting", 5, tenantAdmin()));
        menus.add(menu(107L, 100L, "主题配置", "admin_theme", 2, "/admin/system/theme", "Document", 6, tenantAdmin()));
        menus.add(menu(108L, 100L, "租户自助配置", "admin_tenant_self_service_config", 2, "/admin/system/tenant-self-service", "Setting", 7, tenantAdmin()));
        menus.add(menu(109L, 100L, "权限矩阵", "admin_permission_matrix", 2, "/admin/system/permission-matrix", "DataLine", 8, tenantAdmin()));
        menus.add(menu(110L, 100L, "系统自检", "admin_open_source_health", 2, "/admin/system/open-source-health", "Monitor", 9, tenantAdmin()));
        menus.add(menu(112L, 100L, "接口目录", "admin_api_catalog", 2, "/admin/system/api-catalog", "Document", 10, tenantAdmin()));
        menus.add(menu(111L, 100L, "演示数据中心", "admin_demo_data_center", 2, "/admin/system/demo-data", "DataLine", 11, superAdminOnly()));

        menus.add(menu(700L, 0L, "运维管理", "admin_operation", 1, "/admin/operation", "Monitor", 2, superAdminOnly()));
        menus.add(menu(106L, 700L, "日志管理", "admin_operation_logs", 2, "/admin/operation/logs", "Document", 1, superAdminOnly()));
        menus.add(menu(701L, 700L, "监控中心", "admin_operation_monitoring", 2, "/admin/operation/monitoring", "Monitor", 2, superAdminOnly()));
        menus.add(menu(702L, 700L, "定时任务", "admin_operation_tasks", 2, "/admin/operation/tasks", "Timer", 3, superAdminOnly()));
        menus.add(menu(710L, 700L, "Jenkins 控制台", "admin_operation_jenkins", 2, "/admin/operation/jenkins", "Monitor", 4, superAdminOnly()));
        menus.add(menu(719L, 700L, "发版中心", "admin_operation_release_center", 2, "/admin/operation/release-center", "DataLine", 5, superAdminOnly()));
        menus.add(menu(703L, 700L, "数据备份", "admin_operation_backup", 2, "/admin/operation/backup", "Folder", 6, superAdminOnly()));
        menus.add(menu(704L, 700L, "K8s 管理", "admin_operation_k8s", 2, "/admin/operation/k8s", "Monitor", 7, superAdminOnly()));
        menus.add(menu(711L, 700L, "Nacos 配置中心", "admin_operation_nacos", 2, "/admin/operation/nacos", "Document", 8, superAdminOnly()));
        menus.add(menu(712L, 700L, "微服务拓扑", "admin_operation_topology", 2, "/admin/operation/topology", "Monitor", 9, superAdminOnly()));
        menus.add(menu(713L, 700L, "全球流量中心", "admin_operation_traffic_center", 2, "/admin/operation/traffic-center", "DataLine", 10, superAdminOnly()));
        menus.add(menu(714L, 700L, "通知升级管理", "admin_operation_notification_center", 2, "/admin/operation/notification-center", "Message", 11, superAdminOnly()));
        menus.add(menu(705L, 700L, "API 调试台", "admin_operation_api_workbench", 2, "/admin/operation/api-workbench", "Document", 12, superAdminOnly()));
        menus.add(menu(708L, 700L, "Rainbond 控制台", "admin_operation_rainbond", 2, "/admin/operation/rainbond", "Monitor", 13, superAdminOnly()));
        menus.add(menu(709L, 700L, "一键打包部署", "admin_operation_deploy_package", 2, "/admin/operation/deploy-package", "Folder", 14, superAdminOnly()));
        menus.add(menu(717L, 700L, "远程迁移部署", "admin_operation_remote_migration", 2, "/admin/operation/remote-migration", "Monitor", 15, superAdminOnly()));
        menus.add(menu(718L, 700L, "可视化大屏", "admin_operation_visual_screen", 2, "/admin/operation/visual-screen", "DataLine", 16, superAdminOnly()));
        menus.add(menu(706L, 700L, "微服务监控", "admin_operation_service_monitor", 2, "/admin/operation/service-monitor", "Monitor", 17, superAdminOnly()));
        menus.add(menu(707L, 700L, "数据导出", "admin_operation_data_export", 2, "/admin/operation/data-export", "Folder", 18, superAdminOnly()));
        menus.add(menu(715L, 700L, "Redis 缓存管理", "admin_operation_redis_cache", 2, "/admin/operation/redis-cache", "DataLine", 19, superAdminOnly()));
        menus.add(menu(716L, 700L, "MinIO 存储浏览器", "admin_operation_minio_browser", 2, "/admin/operation/minio-browser", "Folder", 20, superAdminOnly()));

        menus.add(menu(200L, 0L, "社团管理", "club", 1, "/admin/club", "UserFilled", 3, backend()));
        menus.add(menu(201L, 200L, "成员管理", "club_members", 2, "/admin/club/members", "UserFilled", 1, backend()));
        menus.add(menu(202L, 200L, "入社申请", "club_applications", 2, "/admin/club/applications", "Document", 2, contentAdmin()));
        menus.add(menu(204L, 200L, "入社表单配置", "club_admission_form", 2, "/admin/club/admission-form", "Setting", 3, contentAdmin()));
        menus.add(menu(205L, 200L, "社团概况配置", "club_profile_config", 2, "/admin/club/profile-config", "Document", 4, contentAdmin()));
        menus.add(menu(206L, 200L, "历程管理", "club_history", 2, "/admin/club/history", "Calendar", 5, contentAdmin()));
        menus.add(menu(203L, 200L, "校友管理", "club_alumni", 2, "/admin/club/alumni", "User", 6, contentAdmin()));
        menus.add(menu(207L, 200L, "内推码管理", "club_referral_codes", 2, "/admin/club/referral-codes", "Document", 7, contentAdmin()));
        menus.add(menu(208L, 200L, "邮件发送中心", "club_email_center", 2, "/admin/club/email-center", "Message", 8, backend()));
        menus.add(menu(209L, 200L, "成员关系图", "admin_club_member_graph", 2, "/admin/club/member-graph", "DataLine", 9, backend()));
        menus.add(menu(210L, 200L, "资源预约", "admin_club_resources", 2, "/admin/club/resources", "Calendar", 10, contentAdmin()));
        menus.add(menu(212L, 200L, "留言管理", "club_contact_messages", 2, "/admin/club/contact-messages", "Message", 11, contentAdmin()));

        menus.add(menu(300L, 0L, "活动管理", "activity", 1, "/admin/activity", "Calendar", 4, backend()));
        menus.add(menu(301L, 300L, "活动列表", "activity_list", 2, "/admin/activity/list", "Calendar", 1, contentAdmin()));
        menus.add(menu(302L, 300L, "活动审批", "activity_approval", 2, "/admin/activity/approval", "Document", 2, backend()));
        menus.add(menu(303L, 300L, "比赛管理", "activity_competition", 2, "/admin/activity/competition", "Trophy", 3, contentAdmin()));
        menus.add(menu(304L, 300L, "比赛审批", "activity_competition_approval", 2, "/admin/activity/competition-approval", "Document", 4, backend()));
        menus.add(menu(305L, 300L, "活动相册", "admin_activity_photo_gallery", 2, "/admin/activity/photo-gallery", "Folder", 5, contentAdmin()));
        menus.add(menu(306L, 300L, "活动投票", "admin_activity_vote", 2, "/admin/activity/vote", "DataLine", 6, contentAdmin()));
        menus.add(menu(307L, 300L, "学分管理", "admin_activity_credit", 2, "/admin/activity/credit", "Trophy", 7, backend()));
        menus.add(menu(308L, 300L, "志愿服务审核", "admin_activity_volunteer", 2, "/admin/activity/volunteer", "Calendar", 8, backend()));

        menus.add(menu(400L, 0L, "内容管理", "content", 1, "/admin/content", "Document", 5, backend()));
        menus.add(menu(401L, 400L, "新闻管理", "content_news", 2, "/admin/content/news", "Document", 1, contentAdmin()));
        menus.add(menu(402L, 400L, "新闻审核", "content_news_approval", 2, "/admin/content/news-approval", "Document", 2, tenantAdminAndTeacher()));
        menus.add(menu(403L, 400L, "作品管理", "content_works", 2, "/admin/content/works", "Folder", 3, contentAdmin()));
        menus.add(menu(404L, 400L, "公告管理", "content_notices", 2, "/admin/content/notices", "Message", 4, backend()));
        menus.add(menu(405L, 400L, "成就管理", "content_achievements", 2, "/admin/content/achievements", "Trophy", 5, contentAdmin()));
        menus.add(menu(406L, 400L, "文档中心", "admin_content_wiki", 2, "/admin/content/wiki", "Document", 6, contentAdmin()));
        menus.add(menu(407L, 400L, "数据对比", "admin_content_data_diff", 2, "/admin/content/data-diff", "DataLine", 7, backend()));

        menus.add(menu(500L, 0L, "数据统计", "statistics", 1, "/admin/statistics", "DataLine", 6, backend()));
        menus.add(menu(501L, 500L, "数据看板", "statistics_board", 2, "/admin/statistics/board", "DataLine", 1, backend()));
        menus.add(menu(502L, 500L, "租户运营中心", "admin_statistics_tenant_operations", 2, "/admin/statistics/tenant-operations", "DataLine", 2, backend()));
        menus.add(menu(503L, 500L, "统一审批任务中心", "admin_statistics_approval_center", 2, "/admin/statistics/approval-center", "Document", 3, backend()));
        menus.add(menu(504L, 500L, "数据质量与对账中心", "admin_statistics_data_quality", 2, "/admin/statistics/data-quality", "Monitor", 4, backend()));
        menus.add(menu(505L, 500L, "安全审计中心", "admin_statistics_security_audit", 2, "/admin/statistics/security-audit", "Lock", 5, backend()));
        menus.add(menu(506L, 500L, "参与度分析中心", "admin_statistics_participation", 2, "/admin/statistics/participation", "DataLine", 6, backend()));
        menus.add(menu(507L, 500L, "租户增长与留存看板", "admin_statistics_growth_retention", 2, "/admin/statistics/growth-retention", "DataLine", 7, backend()));
        menus.add(menu(508L, 500L, "内容发布日历", "admin_statistics_content_calendar", 2, "/admin/statistics/content-calendar", "Calendar", 8, backend()));
        menus.add(menu(724L, 0L, "财务管理", "admin_finance", 1, "/admin/finance", "DataLine", 7, backend()));
        menus.add(menu(725L, 724L, "财务工作台", "admin_finance_dashboard", 2, "/admin/finance", "DataLine", 1, backend()));
        menus.add(menu(726L, 724L, "预算管理", "admin_finance_budget", 2, "/admin/finance/budget", "DataLine", 2, backend()));
        menus.add(menu(727L, 724L, "经费拨款", "admin_finance_allocation", 2, "/admin/finance/allocation", "DataLine", 3, backend()));
        menus.add(menu(728L, 724L, "报销审批", "admin_finance_reimbursement", 2, "/admin/finance/reimbursement", "Document", 4, backend()));
        menus.add(menu(729L, 724L, "凭证中心", "admin_finance_voucher", 2, "/admin/finance/voucher", "Document", 5, backend()));
        menus.add(menu(730L, 724L, "总账账簿", "admin_finance_ledger", 2, "/admin/finance/ledger", "Folder", 6, backend()));
        menus.add(menu(731L, 724L, "报表中心", "admin_finance_report", 2, "/admin/finance/report", "DataLine", 7, backend()));
        menus.add(menu(732L, 724L, "审计合规", "admin_finance_audit", 2, "/admin/finance/audit", "Monitor", 8, backend()));

        return Collections.unmodifiableList(menus);
    }

    private void upsertMenu(MenuSeed menu) {
        jdbcTemplate.update(
                "INSERT INTO menu " +
                        "(id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, create_time, update_time, is_deleted) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 1, NOW(), NOW(), 0) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "parent_id = VALUES(parent_id), " +
                        "menu_name = CASE " +
                        "WHEN menu_code IN (" + SYNC_MENU_NAME_CODE_SQL + ") THEN VALUES(menu_name) " +
                        "WHEN menu_code = 'club_history' AND menu_name = '校友管理' THEN VALUES(menu_name) " +
                        "WHEN menu_name IS NULL OR menu_name = '' THEN VALUES(menu_name) " +
                        "ELSE menu_name END, " +
                        "menu_code = VALUES(menu_code), " +
                        "menu_type = VALUES(menu_type), " +
                        "path = CASE WHEN menu_code IN (" + SYNC_MENU_NAME_CODE_SQL + ") THEN VALUES(path) WHEN path IS NULL OR path = '' THEN VALUES(path) ELSE path END, " +
                        "component = VALUES(component), " +
                        "icon = CASE WHEN menu_code IN (" + SYNC_MENU_NAME_CODE_SQL + ") THEN VALUES(icon) WHEN icon IS NULL OR icon = '' THEN VALUES(icon) ELSE icon END, " +
                        "sort = CASE WHEN menu_code IN (" + SYNC_MENU_NAME_CODE_SQL + ") THEN VALUES(sort) WHEN sort IS NULL THEN VALUES(sort) ELSE sort END, " +
                        "visible = 1, " +
                        "status = 1, " +
                        "is_deleted = 0, " +
                        "update_time = NOW()",
                menu.id,
                menu.parentId,
                menu.label,
                menu.code,
                menu.type,
                menu.path,
                menu.component,
                menu.icon,
                menu.sort
        );
    }

    private void deactivateLegacyMenus(List<MenuSeed> menus) {
        String ids = menus.stream().map(menu -> String.valueOf(menu.id)).collect(Collectors.joining(","));
        jdbcTemplate.update(
                "UPDATE menu " +
                        "SET visible = 0, status = 0, is_deleted = 1, update_time = NOW() " +
                        "WHERE is_deleted = 0 " +
                        "AND id NOT IN (" + ids + ") " +
                        "AND (path IS NULL OR path NOT LIKE '/admin/%' OR path LIKE '/menu-probe%' OR path LIKE '/probe-menu%' OR path LIKE '/e2e-menu%')"
        );
    }

    private void deactivateKnownLegacyAdminMenus(List<MenuSeed> menus) {
        String ids = menus.stream().map(menu -> String.valueOf(menu.id)).collect(Collectors.joining(","));
        jdbcTemplate.update(
                "UPDATE menu " +
                        "SET visible = 0, status = 0, is_deleted = 1, update_time = NOW() " +
                        "WHERE is_deleted = 0 " +
                        "AND id NOT IN (" + ids + ") " +
                        "AND path IN ('/admin/club/finance')"
        );
    }

    private void removeDeletedMenuBindings() {
        jdbcTemplate.update(
                "DELETE rm FROM role_menu rm " +
                        "LEFT JOIN menu m ON rm.menu_id = m.id " +
                        "WHERE m.id IS NULL OR m.is_deleted = 1 OR m.visible = 0 OR m.status = 0"
        );
    }

    private void syncRoleBindings(List<MenuSeed> menus) {
        List<Long> managedMenuIds = menus.stream()
                .map(menu -> menu.id)
                .collect(Collectors.toList());
        List<RoleRow> roles = jdbcTemplate.query(
                "SELECT id, UPPER(COALESCE(NULLIF(role_code, ''), NULLIF(code, ''), '')) AS role_code FROM role WHERE is_deleted = 0",
                (rs, rowNum) -> new RoleRow(rs.getLong("id"), rs.getString("role_code"))
        );
        List<Object[]> bindings = new ArrayList<>();
        for (RoleRow role : roles) {
            Set<Long> menuIds = resolveMenuIdsForRole(menus, role);
            if (role.isBuiltInRole()) {
                deleteStaleManagedRoleBindings(role.id, managedMenuIds, menuIds);
            }
            for (Long menuId : menuIds) {
                bindings.add(new Object[]{role.id, menuId});
            }
        }
        if (!bindings.isEmpty()) {
            jdbcTemplate.batchUpdate(
                    "INSERT IGNORE INTO role_menu (role_id, menu_id, create_time) VALUES (?, ?, NOW())",
                    bindings
            );
        }
    }

    private void deleteStaleManagedRoleBindings(Long roleId, List<Long> managedMenuIds, Set<Long> allowedMenuIds) {
        if (roleId == null || managedMenuIds == null || managedMenuIds.isEmpty()) {
            return;
        }
        String managedIds = managedMenuIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        if (allowedMenuIds == null || allowedMenuIds.isEmpty()) {
            jdbcTemplate.update(
                    "DELETE rm FROM role_menu rm " +
                            "WHERE rm.role_id = ? " +
                            "AND rm.menu_id IN (" + managedIds + ")",
                    roleId
            );
            return;
        }
        String allowedIds = allowedMenuIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        jdbcTemplate.update(
                "DELETE rm FROM role_menu rm " +
                        "WHERE rm.role_id = ? " +
                        "AND rm.menu_id IN (" + managedIds + ") " +
                        "AND rm.menu_id NOT IN (" + allowedIds + ")",
                roleId
        );
    }

    private Set<Long> resolveMenuIdsForRole(List<MenuSeed> menus, RoleRow role) {
        Set<MenuAudience> audiences = EnumSet.noneOf(MenuAudience.class);
        if (role.id == 1L) {
            audiences.add(MenuAudience.SUPER_ADMIN);
        }
        if ("ADMIN".equals(role.code)) {
            audiences.add(MenuAudience.TENANT_ADMIN);
        } else if ("CLUB_MANAGER".equals(role.code)) {
            audiences.add(MenuAudience.CLUB_MANAGER);
        } else if ("TEACHER".equals(role.code)) {
            audiences.add(MenuAudience.TEACHER);
        }
        if (audiences.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> menuIds = new LinkedHashSet<>();
        for (MenuSeed menu : menus) {
            if (!Collections.disjoint(menu.audiences, audiences)) {
                menuIds.add(menu.id);
            }
        }
        return menuIds;
    }

    private static MenuSeed menu(
            Long id,
            Long parentId,
            String label,
            String code,
            Integer type,
            String path,
            String icon,
            Integer sort,
            Set<MenuAudience> audiences
    ) {
        return new MenuSeed(id, parentId, label, code, type, path, null, icon, sort, audiences);
    }

    private static Set<MenuAudience> superAdminOnly() {
        return EnumSet.of(MenuAudience.SUPER_ADMIN);
    }

    private static Set<MenuAudience> tenantAdmin() {
        return EnumSet.of(MenuAudience.SUPER_ADMIN, MenuAudience.TENANT_ADMIN);
    }

    private static Set<MenuAudience> backend() {
        return EnumSet.of(MenuAudience.SUPER_ADMIN, MenuAudience.TENANT_ADMIN, MenuAudience.CLUB_MANAGER, MenuAudience.TEACHER);
    }

    private static Set<MenuAudience> contentAdmin() {
        return EnumSet.of(MenuAudience.SUPER_ADMIN, MenuAudience.TENANT_ADMIN, MenuAudience.CLUB_MANAGER);
    }

    private static Set<MenuAudience> tenantAdminAndTeacher() {
        return EnumSet.of(MenuAudience.SUPER_ADMIN, MenuAudience.TENANT_ADMIN, MenuAudience.TEACHER);
    }

    enum MenuAudience {
        SUPER_ADMIN,
        TENANT_ADMIN,
        CLUB_MANAGER,
        TEACHER
    }

    static final class MenuSeed {
        private final Long id;
        private final Long parentId;
        private final String label;
        private final String code;
        private final Integer type;
        private final String path;
        private final String component;
        private final String icon;
        private final Integer sort;
        private final Set<MenuAudience> audiences;

        private MenuSeed(
                Long id,
                Long parentId,
                String label,
                String code,
                Integer type,
                String path,
                String component,
                String icon,
                Integer sort,
                Set<MenuAudience> audiences
        ) {
            this.id = id;
            this.parentId = parentId;
            this.label = label;
            this.code = code;
            this.type = type;
            this.path = path;
            this.component = component;
            this.icon = icon;
            this.sort = sort;
            this.audiences = EnumSet.copyOf(audiences);
        }

        Long id() {
            return id;
        }

        String path() {
            return path;
        }

        String label() {
            return label;
        }

        Set<MenuAudience> audiences() {
            return EnumSet.copyOf(audiences);
        }
    }

    private static final class RoleRow {
        private final Long id;
        private final String code;

        private RoleRow(Long id, String code) {
            this.id = id;
            this.code = code == null ? "" : code.toUpperCase(Locale.ROOT);
        }

        private boolean isBuiltInRole() {
            return BUILT_IN_ROLE_CODES.contains(code);
        }
    }
}
