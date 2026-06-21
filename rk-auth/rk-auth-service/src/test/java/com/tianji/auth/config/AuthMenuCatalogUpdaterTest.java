package com.tianji.auth.config;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthMenuCatalogUpdaterTest {

    @Test
    void currentMenus_shouldUseCurrentAdminRoutesInsteadOfLegacyRoutes() {
        Set<String> paths = AuthMenuCatalogUpdater.currentMenus()
                .stream()
                .map(AuthMenuCatalogUpdater.MenuSeed::path)
                .collect(Collectors.toSet());

        assertTrue(paths.contains("/admin/system/users"));
        assertTrue(paths.contains("/admin/system/tenant-self-service"));
        assertTrue(paths.contains("/admin/system/open-source-health"));
        assertTrue(paths.contains("/admin/system/api-catalog"));
        assertTrue(paths.contains("/admin/system/demo-data"));
        assertTrue(paths.contains("/admin/operation/k8s"));
        assertTrue(paths.contains("/admin/finance"));
        assertTrue(paths.contains("/admin/finance/budget"));
        assertTrue(paths.contains("/admin/finance/allocation"));
        assertTrue(paths.contains("/admin/finance/reimbursement"));
        assertTrue(paths.contains("/admin/finance/voucher"));
        assertTrue(paths.contains("/admin/finance/ledger"));
        assertTrue(paths.contains("/admin/finance/report"));
        assertTrue(paths.contains("/admin/finance/audit"));
        assertTrue(paths.contains("/admin/operation/rainbond"));
        assertTrue(paths.contains("/admin/operation/deploy-package"));
        assertTrue(paths.contains("/admin/operation/release-center"));
        assertTrue(paths.contains("/admin/operation/traffic-center"));
        assertTrue(paths.contains("/admin/statistics/tenant-operations"));
        assertTrue(paths.contains("/admin/statistics/approval-center"));
        assertTrue(paths.contains("/admin/statistics/data-quality"));
        assertTrue(paths.contains("/admin/statistics/security-audit"));
        assertTrue(paths.contains("/admin/statistics/participation"));
        assertTrue(paths.contains("/admin/statistics/growth-retention"));
        assertTrue(paths.contains("/admin/statistics/content-calendar"));
        assertTrue(paths.contains("/admin/activity/photo-gallery"));
        assertTrue(paths.contains("/admin/content/achievements"));
        assertFalse(paths.contains("/admin/users"));
        assertFalse(paths.contains("/club/members"));
        assertFalse(paths.contains("/admin/club/finance"));
        assertFalse(paths.contains("/menu-probe"));
    }

    @Test
    void currentMenus_shouldKeepOperationMenusSuperAdminOnly() {
        AuthMenuCatalogUpdater.MenuSeed operation = AuthMenuCatalogUpdater.currentMenus()
                .stream()
                .filter(item -> "/admin/operation".equals(item.path()))
                .findFirst()
                .orElseThrow();

        assertTrue(operation.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.SUPER_ADMIN));
        assertFalse(operation.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.TENANT_ADMIN));
        assertFalse(operation.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.CLUB_MANAGER));
        assertFalse(operation.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.TEACHER));
    }

    @Test
    void currentMenus_shouldExposeMenuManagementToSameTenantAdminAudienceAsSystemDirectory() {
        AuthMenuCatalogUpdater.MenuSeed menuManagement = AuthMenuCatalogUpdater.currentMenus()
                .stream()
                .filter(item -> "/admin/system/menus".equals(item.path()))
                .findFirst()
                .orElseThrow();

        assertTrue(menuManagement.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.SUPER_ADMIN));
        assertTrue(menuManagement.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.TENANT_ADMIN));
        assertFalse(menuManagement.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.CLUB_MANAGER));
        assertFalse(menuManagement.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.TEACHER));
    }

    @Test
    void currentMenus_shouldExposeTenantSelfServiceConfigToTenantAdminAudience() {
        AuthMenuCatalogUpdater.MenuSeed selfService = AuthMenuCatalogUpdater.currentMenus()
                .stream()
                .filter(item -> "/admin/system/tenant-self-service".equals(item.path()))
                .findFirst()
                .orElseThrow();

        assertEquals("租户自助配置", selfService.label());
        assertTrue(selfService.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.SUPER_ADMIN));
        assertTrue(selfService.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.TENANT_ADMIN));
        assertFalse(selfService.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.CLUB_MANAGER));
        assertFalse(selfService.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.TEACHER));
    }

    @Test
    void currentMenus_shouldExposeOpenSourceHealthToTenantAdminAudience() {
        AuthMenuCatalogUpdater.MenuSeed health = AuthMenuCatalogUpdater.currentMenus()
                .stream()
                .filter(item -> "/admin/system/open-source-health".equals(item.path()))
                .findFirst()
                .orElseThrow();

        assertEquals("系统自检", health.label());
        assertTrue(health.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.SUPER_ADMIN));
        assertTrue(health.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.TENANT_ADMIN));
        assertFalse(health.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.CLUB_MANAGER));
        assertFalse(health.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.TEACHER));
    }

    @Test
    void currentMenus_shouldExposeApiCatalogToTenantAdminAudience() {
        AuthMenuCatalogUpdater.MenuSeed apiCatalog = AuthMenuCatalogUpdater.currentMenus()
                .stream()
                .filter(item -> "/admin/system/api-catalog".equals(item.path()))
                .findFirst()
                .orElseThrow();

        assertEquals("接口目录", apiCatalog.label());
        assertTrue(apiCatalog.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.SUPER_ADMIN));
        assertTrue(apiCatalog.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.TENANT_ADMIN));
        assertFalse(apiCatalog.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.CLUB_MANAGER));
        assertFalse(apiCatalog.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.TEACHER));
    }

    @Test
    void currentMenus_shouldExposeDemoDataCenterToSuperAdminOnly() {
        AuthMenuCatalogUpdater.MenuSeed demoData = AuthMenuCatalogUpdater.currentMenus()
                .stream()
                .filter(item -> "/admin/system/demo-data".equals(item.path()))
                .findFirst()
                .orElseThrow();

        assertEquals("演示数据中心", demoData.label());
        assertTrue(demoData.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.SUPER_ADMIN));
        assertFalse(demoData.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.TENANT_ADMIN));
        assertFalse(demoData.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.CLUB_MANAGER));
        assertFalse(demoData.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.TEACHER));
    }

    @Test
    void currentMenus_shouldContainAllCurrentPlatformOperationPages() {
        Set<String> paths = AuthMenuCatalogUpdater.currentMenus()
                .stream()
                .map(AuthMenuCatalogUpdater.MenuSeed::path)
                .collect(Collectors.toSet());

        assertTrue(paths.contains("/admin/operation/jenkins"));
        assertTrue(paths.contains("/admin/operation/release-center"));
        assertTrue(paths.contains("/admin/operation/remote-migration"));
        assertTrue(paths.contains("/admin/operation/visual-screen"));
        assertTrue(paths.contains("/admin/operation/redis-cache"));
        assertTrue(paths.contains("/admin/operation/minio-browser"));
    }

    @Test
    void currentMenus_shouldExposeTenantOperationsCenterToBackendAudience() {
        AuthMenuCatalogUpdater.MenuSeed tenantOperations = AuthMenuCatalogUpdater.currentMenus()
                .stream()
                .filter(item -> "/admin/statistics/tenant-operations".equals(item.path()))
                .findFirst()
                .orElseThrow();

        assertTrue(tenantOperations.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.SUPER_ADMIN));
        assertTrue(tenantOperations.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.TENANT_ADMIN));
        assertTrue(tenantOperations.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.CLUB_MANAGER));
        assertTrue(tenantOperations.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.TEACHER));
    }

    @Test
    void currentMenus_shouldExposeApprovalTaskCenterToBackendAudience() {
        AuthMenuCatalogUpdater.MenuSeed approvalCenter = AuthMenuCatalogUpdater.currentMenus()
                .stream()
                .filter(item -> "/admin/statistics/approval-center".equals(item.path()))
                .findFirst()
                .orElseThrow();

        assertTrue(approvalCenter.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.SUPER_ADMIN));
        assertTrue(approvalCenter.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.TENANT_ADMIN));
        assertTrue(approvalCenter.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.CLUB_MANAGER));
        assertTrue(approvalCenter.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.TEACHER));
    }

    @Test
    void currentMenus_shouldExposeDataQualityCenterToBackendAudience() {
        AuthMenuCatalogUpdater.MenuSeed dataQuality = AuthMenuCatalogUpdater.currentMenus()
                .stream()
                .filter(item -> "/admin/statistics/data-quality".equals(item.path()))
                .findFirst()
                .orElseThrow();

        assertTrue(dataQuality.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.SUPER_ADMIN));
        assertTrue(dataQuality.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.TENANT_ADMIN));
        assertTrue(dataQuality.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.CLUB_MANAGER));
        assertTrue(dataQuality.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.TEACHER));
    }

    @Test
    void currentMenus_shouldExposeSecurityAuditCenterToBackendAudience() {
        AuthMenuCatalogUpdater.MenuSeed securityAudit = AuthMenuCatalogUpdater.currentMenus()
                .stream()
                .filter(item -> "/admin/statistics/security-audit".equals(item.path()))
                .findFirst()
                .orElseThrow();

        assertTrue(securityAudit.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.SUPER_ADMIN));
        assertTrue(securityAudit.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.TENANT_ADMIN));
        assertTrue(securityAudit.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.CLUB_MANAGER));
        assertTrue(securityAudit.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.TEACHER));
    }

    @Test
    void currentMenus_shouldExposeParticipationAnalysisCenterToBackendAudience() {
        AuthMenuCatalogUpdater.MenuSeed participation = AuthMenuCatalogUpdater.currentMenus()
                .stream()
                .filter(item -> "/admin/statistics/participation".equals(item.path()))
                .findFirst()
                .orElseThrow();

        assertTrue(participation.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.SUPER_ADMIN));
        assertTrue(participation.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.TENANT_ADMIN));
        assertTrue(participation.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.CLUB_MANAGER));
        assertTrue(participation.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.TEACHER));
    }

    @Test
    void currentMenus_shouldExposeGrowthRetentionCenterToBackendAudience() {
        AuthMenuCatalogUpdater.MenuSeed growthRetention = AuthMenuCatalogUpdater.currentMenus()
                .stream()
                .filter(item -> "/admin/statistics/growth-retention".equals(item.path()))
                .findFirst()
                .orElseThrow();

        assertTrue(growthRetention.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.SUPER_ADMIN));
        assertTrue(growthRetention.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.TENANT_ADMIN));
        assertTrue(growthRetention.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.CLUB_MANAGER));
        assertTrue(growthRetention.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.TEACHER));
    }

    @Test
    void currentMenus_shouldExposeContentPublishCalendarToBackendAudience() {
        AuthMenuCatalogUpdater.MenuSeed contentCalendar = AuthMenuCatalogUpdater.currentMenus()
                .stream()
                .filter(item -> "/admin/statistics/content-calendar".equals(item.path()))
                .findFirst()
                .orElseThrow();

        assertTrue(contentCalendar.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.SUPER_ADMIN));
        assertTrue(contentCalendar.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.TENANT_ADMIN));
        assertTrue(contentCalendar.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.CLUB_MANAGER));
        assertTrue(contentCalendar.audiences().contains(AuthMenuCatalogUpdater.MenuAudience.TEACHER));
    }

    @Test
    void currentMenus_shouldNameClubHistoryMenuDifferentlyFromAlumniMenu() {
        AuthMenuCatalogUpdater.MenuSeed history = AuthMenuCatalogUpdater.currentMenus()
                .stream()
                .filter(item -> "/admin/club/history".equals(item.path()))
                .findFirst()
                .orElseThrow();
        AuthMenuCatalogUpdater.MenuSeed alumni = AuthMenuCatalogUpdater.currentMenus()
                .stream()
                .filter(item -> "/admin/club/alumni".equals(item.path()))
                .findFirst()
                .orElseThrow();

        assertEquals("历程管理", history.label());
        assertEquals("校友管理", alumni.label());
    }

    @Test
    void menuCatalogUpdater_shouldPreserveCustomizedMenuDisplayFieldsOnExistingRows() throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/tianji/auth/config/AuthMenuCatalogUpdater.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(source.contains("menu_name = CASE"));
        assertTrue(source.contains("path = CASE"));
        assertFalse(source.contains("menu_name = VALUES(menu_name),"));
    }

    @Test
    void menuMapper_shouldMapMenuNameToLabelForRoleMenus() throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("mapper/MenuMapper.xml")) {
            assertNotNull(input);
            String xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(xml.contains("<select id=\"listByRoles\" resultMap=\"BaseResultMap\">"));
            assertTrue(xml.contains("m.is_deleted = 0"));
            assertTrue(xml.contains("m.visible = 1"));
            assertTrue(xml.contains("m.status = 1"));
        }
    }

    @Test
    void menuCatalogUpdater_shouldUseExplicitTransactionAndBatchRoleBindingSync() throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/tianji/auth/config/AuthMenuCatalogUpdater.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(source.contains("TransactionTemplate"));
        assertTrue(source.contains("transactionTemplate.executeWithoutResult"));
        assertTrue(source.contains("jdbcTemplate.batchUpdate("));
        assertFalse(source.contains("@Transactional"));
    }

    @Test
    void menuCatalogUpdater_shouldDeleteStaleManagedRoleMenuBindingsBeforeInsert() throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/tianji/auth/config/AuthMenuCatalogUpdater.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(source.contains("BUILT_IN_ROLE_CODES"));
        assertTrue(source.contains("DELETE rm FROM role_menu rm"));
        assertTrue(source.contains("NOT IN"));
    }
}
