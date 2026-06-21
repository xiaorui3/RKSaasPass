package com.tianji.user.service.impl;

import com.tianji.common.utils.UserContext;
import com.tianji.common.utils.TenantContext;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.user.domain.dto.adminops.AdminBackupCreateDTO;
import com.tianji.user.domain.dto.adminops.AdminTrafficDefaultTenantDTO;
import com.tianji.user.domain.dto.adminops.AdminK8sCleanupRequestDTO;
import com.tianji.user.domain.dto.adminops.AdminK8sWorkloadActionDTO;
import com.tianji.user.domain.dto.adminops.AdminJenkinsBuildRequestDTO;
import com.tianji.user.domain.dto.adminops.AdminNacosConfigSaveDTO;
import com.tianji.user.domain.vo.adminops.AdminApiWorkbenchDocVO;
import com.tianji.user.domain.vo.adminops.AdminTrafficOnlineUserVO;
import com.tianji.user.domain.vo.adminops.AdminTrafficOverviewVO;
import com.tianji.user.service.adminops.AdminBackupArchiveBuilder;
import com.tianji.user.service.adminops.AdminBackupStorageService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOpsServiceImplBackupTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private AdminBackupArchiveBuilder backupArchiveBuilder;
    @Mock
    private AdminBackupStorageService backupStorageService;

    @InjectMocks
    private AdminOpsServiceImpl service;

    @AfterEach
    void tearDown() {
        UserContext.removeUser();
        TenantContext.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void getTrafficOverview_shouldFallbackToTenantOneWhenConfigMissing() {
        TenantContext.setTenantId(1L);
        when(jdbcTemplate.queryForList(contains("FROM rk_tenant")))
                .thenReturn(List.of(Map.of(
                        "id", 1L,
                        "tenantName", "Tenant A",
                        "status", 1
                )));
        when(jdbcTemplate.queryForObject(contains("admin.traffic.defaultTenantId"), eq(String.class)))
                .thenReturn(null);
        when(jdbcTemplate.queryForList(contains("FROM sys_logininfor")))
                .thenReturn(List.of());
        when(jdbcTemplate.queryForList(contains("FROM sys_oper_log")))
                .thenReturn(List.of());

        AdminTrafficOverviewVO result = service.getTrafficOverview(null, null, null);

        assertEquals(1L, result.getDefaultTenantId());
        assertEquals(1L, result.getSelectedTenantId());
        assertEquals("Tenant A", result.getTenantOptions().get(0).getTenantName());
        assertEquals(0L, result.getTotalVisits());
    }

    @Test
    void getTrafficOverview_shouldAggregateLoginAndOperationRows() {
        TenantContext.setTenantId(1L);
        when(jdbcTemplate.queryForList(contains("FROM rk_tenant")))
                .thenReturn(List.of(Map.of(
                        "id", 1L,
                        "tenantName", "Tenant A",
                        "status", 1
                )));
        when(jdbcTemplate.queryForObject(contains("admin.traffic.defaultTenantId"), eq(String.class)))
                .thenReturn("1");
        when(jdbcTemplate.queryForList(contains("FROM sys_logininfor")))
                .thenReturn(List.of(
                        Map.of(
                                "tenantId", 1L,
                                "tenantName", "Tenant A",
                                "userName", "admin_a",
                                "ip", "203.0.113.8",
                                "rawLocation", "美国 California Mountain View",
                                "lastSeenAt", "2026-05-19 10:00:00",
                                "countValue", 2L
                        ),
                        Map.of(
                                "tenantId", 1L,
                                "tenantName", "Tenant A",
                                "userName", "admin_a",
                                "ip", "10.0.0.5",
                                "rawLocation", "",
                                "lastSeenAt", "2026-05-19 10:05:00",
                                "countValue", 1L
                        )
                ));
        when(jdbcTemplate.queryForList(contains("FROM sys_oper_log")))
                .thenReturn(List.of(Map.of(
                        "tenantId", 1L,
                        "tenantName", "Tenant A",
                        "userName", "admin_a",
                        "ip", "203.0.113.8",
                        "rawLocation", "美国 California Mountain View",
                        "samplePath", "/admin/operation/logs",
                        "sampleAction", "日志管理",
                        "lastSeenAt", "2026-05-19 10:03:00",
                        "countValue", 3L
                )));

        AdminTrafficOverviewVO result = service.getTrafficOverview(1L, null, null);

        assertEquals(6L, result.getTotalVisits());
        assertEquals(3L, result.getTotalLogins());
        assertEquals(3L, result.getTotalOperations());
        assertEquals("美国", result.getCountryRankings().get(0).getCountry());
        assertEquals("California", result.getCountryRankings().get(0).getProvince());
        assertEquals(5L, result.getCountryRankings().get(0).getVisitCount());
        assertEquals(1L, result.getAbnormalIpCount());
        assertFalse(result.getRecentIps().stream().anyMatch(row -> "内网".equals(row.getCountry())));
    }

    @Test
    void getTrafficOverview_shouldKeepPrivateProxyIpsOutOfCountryRankings() {
        TenantContext.setTenantId(1L);
        when(jdbcTemplate.queryForList(contains("FROM rk_tenant")))
                .thenReturn(List.of(Map.of(
                        "id", 1L,
                        "tenantName", "Tenant A",
                        "status", 1
                )));
        when(jdbcTemplate.queryForObject(contains("admin.traffic.defaultTenantId"), eq(String.class)))
                .thenReturn("1");
        when(jdbcTemplate.queryForList(contains("FROM sys_logininfor")))
                .thenReturn(List.of(
                        Map.of(
                                "tenantId", 1L,
                                "tenantName", "Tenant A",
                                "userName", "admin_a",
                                "ip", "10.42.0.12",
                                "rawLocation", "",
                                "lastSeenAt", "2026-05-19 10:00:00",
                                "countValue", 20L
                        ),
                        Map.of(
                                "tenantId", 1L,
                                "tenantName", "Tenant A",
                                "userName", "admin_a",
                                "ip", "203.0.113.8",
                                "rawLocation", "美国 California Mountain View",
                                "lastSeenAt", "2026-05-19 10:05:00",
                                "countValue", 2L
                        )
                ));
        when(jdbcTemplate.queryForList(contains("FROM sys_oper_log"))).thenReturn(List.of());

        AdminTrafficOverviewVO result = service.getTrafficOverview(1L, null, null);

        assertEquals(22L, result.getTotalVisits());
        assertEquals(1L, result.getAbnormalIpCount());
        assertEquals(1, result.getCountryRankings().size());
        assertEquals("美国", result.getCountryRankings().get(0).getCountry());
        assertEquals(1, result.getRecentIps().size());
        assertEquals("203.0.113.8", result.getRecentIps().get(0).getIp());
        assertFalse(result.getCountryRankings().stream().anyMatch(row -> "内网".equals(row.getCountry())));
        assertFalse(result.getRecentIps().stream().anyMatch(row -> "内网".equals(row.getCountry())));
    }

    @Test
    void getTrafficOverview_shouldUseExplicitCollationForLogUserJoins() {
        TenantContext.setTenantId(1L);
        when(jdbcTemplate.queryForList(contains("FROM rk_tenant")))
                .thenReturn(List.of(Map.of(
                        "id", 1L,
                        "tenantName", "Tenant A",
                        "status", 1
                )));
        when(jdbcTemplate.queryForObject(contains("admin.traffic.defaultTenantId"), eq(String.class)))
                .thenReturn("1");
        when(jdbcTemplate.queryForList(contains("FROM sys_logininfor"))).thenReturn(List.of());
        when(jdbcTemplate.queryForList(contains("FROM sys_oper_log"))).thenReturn(List.of());

        service.getTrafficOverview(1L, null, null);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, org.mockito.Mockito.atLeastOnce()).queryForList(sqlCaptor.capture());
        List<String> trafficSql = sqlCaptor.getAllValues().stream()
                .filter(sql -> sql.contains("FROM sys_logininfor") || sql.contains("FROM sys_oper_log"))
                .collect(Collectors.toList());
        assertEquals(2, trafficSql.size());
        assertTrue(trafficSql.get(0).contains("u.username COLLATE utf8mb4_unicode_ci = l.login_name COLLATE utf8mb4_unicode_ci"));
        assertTrue(trafficSql.get(1).contains("u.username COLLATE utf8mb4_unicode_ci = o.oper_name COLLATE utf8mb4_unicode_ci"));
    }

    @Test
    void getTrafficOverview_shouldExposeOnlineUsersWithDeviceBreakdown() {
        TenantContext.setTenantId(1L);
        when(jdbcTemplate.queryForList(contains("FROM rk_tenant")))
                .thenReturn(List.of(Map.of(
                        "id", 1L,
                        "tenantName", "Tenant A",
                        "status", 1
                )));
        when(jdbcTemplate.queryForObject(contains("admin.traffic.defaultTenantId"), eq(String.class)))
                .thenReturn("1");
        when(jdbcTemplate.queryForList(contains("FROM sys_logininfor")))
                .thenReturn(List.of());
        when(jdbcTemplate.queryForList(contains("FROM sys_oper_log")))
                .thenReturn(List.of());
        when(jdbcTemplate.queryForList(contains("/* traffic_online */"), eq(1L)))
                .thenReturn(List.of(
                        Map.ofEntries(
                                Map.entry("tenantId", 1L),
                                Map.entry("tenantName", "Tenant A"),
                                Map.entry("userName", "admin_a"),
                                Map.entry("ip", "198.51.100.24"),
                                Map.entry("rawLocation", "中国 四川 成都"),
                                Map.entry("deviceType", "pc"),
                                Map.entry("clientType", "Chrome / Windows"),
                                Map.entry("lastSeenAt", "2026-05-20 08:20:00"),
                                Map.entry("activityCount", 3L),
                                Map.entry("samplePath", "/admin/dashboard"),
                                Map.entry("sampleAction", "Dashboard")
                        ),
                        Map.ofEntries(
                                Map.entry("tenantId", 1L),
                                Map.entry("tenantName", "Tenant A"),
                                Map.entry("userName", "member_m"),
                                Map.entry("ip", "203.0.113.8"),
                                Map.entry("rawLocation", "United States California Mountain View"),
                                Map.entry("deviceType", "mobile"),
                                Map.entry("clientType", "Android App"),
                                Map.entry("lastSeenAt", "2026-05-20 08:19:30"),
                                Map.entry("activityCount", 2L),
                                Map.entry("samplePath", "/api/mobile/releases/latest"),
                                Map.entry("sampleAction", "Mobile")
                        )
                ));

        AdminTrafficOverviewVO result = service.getTrafficOverview(1L, null, null);

        assertEquals(2L, result.getOnlineUserCount());
        assertEquals(1L, result.getOnlinePcCount());
        assertEquals(1L, result.getOnlineMobileCount());
        List<AdminTrafficOnlineUserVO> onlineUsers = result.getOnlineUsers();
        assertEquals(2, onlineUsers.size());
        assertEquals("admin_a", onlineUsers.get(0).getUserName());
        assertEquals("pc", onlineUsers.get(0).getDeviceType());
        assertEquals("Chrome / Windows", onlineUsers.get(0).getClientType());
        assertEquals("member_m", onlineUsers.get(1).getUserName());
        assertEquals("mobile", onlineUsers.get(1).getDeviceType());
    }

    @Test
    void saveTrafficDefaultTenant_shouldRejectUnknownTenant() {
        TenantContext.setTenantId(1L);
        AdminTrafficDefaultTenantDTO dto = new AdminTrafficDefaultTenantDTO();
        dto.setTenantId(99L);
        when(jdbcTemplate.queryForObject(contains("FROM rk_tenant"), eq(Integer.class), eq(99L)))
                .thenReturn(0);

        BadRequestException error = assertThrows(BadRequestException.class, () -> service.saveTrafficDefaultTenant(dto));

        assertTrue(error.getMessage().contains("租户不存在"));
    }

    @Test
    void getBackupTables_shouldReturnAvailableTablesForSelectedDatabases() {
        when(backupArchiveBuilder.resolveTableSelections(List.of("rk_user"), null))
                .thenReturn(Map.of("rk_user", List.of("users", "club_members")));

        Map<String, List<String>> result = service.getBackupTables(List.of("rk_user"));

        assertEquals(List.of("users", "club_members"), result.get("rk_user"));
    }

    @Test
    void createBackup_shouldStoreArchiveInMinioAndPersistRelativePath() {
        UserContext.setUser(9L);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admin_backup_schedule", Integer.class)).thenReturn(1);
        when(jdbcTemplate.queryForList(
                "SELECT table_schema AS databaseName, COALESCE(SUM(data_length + index_length), 0) AS totalBytes " +
                        "FROM information_schema.tables WHERE table_schema IN (?) GROUP BY table_schema ORDER BY table_schema",
                "rk_user"
        )).thenReturn(List.of(Map.of("databaseName", "rk_user", "totalBytes", 1024L)));
        when(backupArchiveBuilder.resolveTableSelections(eq(List.of("rk_user")), eq(null))).thenReturn(Map.of());
        when(backupArchiveBuilder.buildArchive(eq(List.of("rk_user")), eq(Map.of()))).thenReturn("gzip".getBytes());
        when(backupStorageService.store(anyString(), eq("gzip".getBytes())))
                .thenReturn("rk-user/backup/2026/04/25/rk_backup_20260425010101.sql.gz");
        Map<String, Object> backupRow = new LinkedHashMap<>();
        backupRow.put("id", 1L);
        backupRow.put("backup_name", "backup_20260425010101");
        backupRow.put("database_scope", "PARTIAL");
        backupRow.put("database_list", "[\"rk_user\"]");
        backupRow.put("file_name", "rk_backup_20260425010101.sql.gz");
        backupRow.put("file_size_bytes", 4L);
        backupRow.put("status", "SUCCESS");
        backupRow.put("backup_type", "MANUAL");
        backupRow.put("create_time", "2026-04-25 01:01:01");
        backupRow.put("remote_path", "rk-user/backup/2026/04/25/rk_backup_20260425010101.sql.gz");
        backupRow.put("note_text", "manual backup");
        backupRow.put("summary_json", "{\"databases\":[\"rk_user\"],\"tables\":{}}");
        when(jdbcTemplate.queryForList("SELECT id, backup_name, database_scope, database_list, file_name, file_size_bytes, status, backup_type, create_time, remote_path, note_text, summary_json FROM admin_backup_record WHERE is_deleted = 0 ORDER BY create_time DESC"))
                .thenReturn(List.of(backupRow));
        when(jdbcTemplate.queryForMap("SELECT enabled, frequency, backup_time, retention_days, database_list FROM admin_backup_schedule WHERE id = 1"))
                .thenReturn(Map.of(
                        "enabled", 1,
                        "frequency", "daily",
                        "backup_time", "02:00",
                        "retention_days", 30,
                        "database_list", "[\"rk_user\"]"
                ));
        when(jdbcTemplate.queryForObject("SELECT COALESCE(SUM(file_size_bytes), 0) FROM admin_backup_record WHERE is_deleted = 0", Long.class))
                .thenReturn(4L);

        AdminBackupCreateDTO dto = new AdminBackupCreateDTO();
        dto.setDatabases(List.of("rk_user"));

        service.createBackup(dto);

        verify(backupArchiveBuilder).buildArchive(List.of("rk_user"), Map.of());
        verify(backupStorageService).store(anyString(), eq("gzip".getBytes()));

        verify(jdbcTemplate).update(
                eq("INSERT INTO admin_backup_record (backup_name, database_scope, database_list, file_name, remote_path, file_size_bytes, status, backup_type, note_text, summary_json, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"),
                any(),
                any(),
                any(),
                any(),
                eq("rk-user/backup/2026/04/25/rk_backup_20260425010101.sql.gz"),
                any(),
                any(),
                any(),
                any(),
                any(),
                eq(9L)
        );
    }

    @Test
    void downloadBackup_shouldReadBytesFromObjectStorage() {
        when(jdbcTemplate.queryForList(
                "SELECT id, remote_path FROM admin_backup_record WHERE id = ? AND is_deleted = 0",
                8L
        )).thenReturn(List.of(Map.of("id", 8L, "remote_path", "rk-user/backup/archive.sql.gz")));
        when(backupStorageService.exists("rk-user/backup/archive.sql.gz")).thenReturn(true);
        when(backupStorageService.read("rk-user/backup/archive.sql.gz")).thenReturn("abc".getBytes());

        byte[] result = service.downloadBackup(8L);

        assertArrayEquals("abc".getBytes(), result);
    }

    @Test
    void getBackupOverview_shouldMarkMissingBackupArtifactAsNotDownloadable() {
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admin_backup_schedule", Integer.class)).thenReturn(1);
        Map<String, Object> backupRow = new LinkedHashMap<>();
        backupRow.put("id", 11L);
        backupRow.put("backup_name", "backup_20260512183303");
        backupRow.put("database_scope", "PARTIAL");
        backupRow.put("database_list", "[\"rk_user\"]");
        backupRow.put("file_name", "rk_backup_20260512183303.sql.gz");
        backupRow.put("file_size_bytes", 1536L);
        backupRow.put("status", "SUCCESS");
        backupRow.put("backup_type", "MANUAL");
        backupRow.put("create_time", "2026-05-12 18:34:25");
        backupRow.put("remote_path", "rk-user/backup/2026/05/12/rk_backup_20260512183303.sql.gz");
        backupRow.put("note_text", "before release");
        backupRow.put("summary_json", "{\"databases\":[\"rk_user\"],\"tables\":{\"rk_user\":[\"user\",\"club_member\"]}}");
        when(jdbcTemplate.queryForList("SELECT id, backup_name, database_scope, database_list, file_name, file_size_bytes, status, backup_type, create_time, remote_path, note_text, summary_json FROM admin_backup_record WHERE is_deleted = 0 ORDER BY create_time DESC"))
                .thenReturn(List.of(backupRow));
        when(jdbcTemplate.queryForMap("SELECT enabled, frequency, backup_time, retention_days, database_list FROM admin_backup_schedule WHERE id = 1"))
                .thenReturn(Map.of(
                        "enabled", 0,
                        "frequency", "daily",
                        "backup_time", "02:00",
                        "retention_days", 30,
                        "database_list", "[\"rk_user\"]"
                ));
        when(jdbcTemplate.queryForObject("SELECT COALESCE(SUM(file_size_bytes), 0) FROM admin_backup_record WHERE is_deleted = 0", Long.class))
                .thenReturn(1536L);
        when(backupStorageService.exists("rk-user/backup/2026/05/12/rk_backup_20260512183303.sql.gz")).thenReturn(false);

        var overview = service.getBackupOverview();

        assertFalse(overview.getBackups().get(0).getDownloadable());
        assertEquals(List.of("rk_user"), overview.getBackups().get(0).getDatabaseList());
        assertEquals(List.of("user", "club_member"), overview.getBackups().get(0).getTableSummary().get("rk_user"));
        assertEquals(2, overview.getBackups().get(0).getTableCount());
        assertEquals("before release", overview.getBackups().get(0).getNote());
    }

    @Test
    void downloadBackup_shouldRejectMissingBackupArtifactWithoutServerError() {
        when(jdbcTemplate.queryForList(
                "SELECT id, remote_path FROM admin_backup_record WHERE id = ? AND is_deleted = 0",
                11L
        )).thenReturn(List.of(Map.of("id", 11L, "remote_path", "rk-user/backup/missing.sql.gz")));
        when(backupStorageService.exists("rk-user/backup/missing.sql.gz")).thenReturn(false);

        BadRequestException error = assertThrows(BadRequestException.class, () -> service.downloadBackup(11L));

        assertTrue(error.getMessage().contains("备份文件不存在"));
    }

    @Test
    void downloadBackup_shouldRejectBaselineSnapshotWithoutDownloadFile() {
        when(jdbcTemplate.queryForList(
                "SELECT id, remote_path FROM admin_backup_record WHERE id = ? AND is_deleted = 0",
                1L
        )).thenReturn(List.of(Map.of("id", 1L, "remote_path", "")));

        BadRequestException error = assertThrows(BadRequestException.class, () -> service.downloadBackup(1L));

        assertTrue(error.getMessage().contains("没有可下载文件"));
    }

    @Test
    void getK8sOverview_shouldDegradeGracefullyWhenRemoteProbeToolingIsUnavailable() {
        ReflectionTestUtils.setField(service, "remotePython", "Z:/missing/python.exe");
        ReflectionTestUtils.setField(service, "remoteSshScript", "Z:/missing/remote_ssh.py");

        var result = assertDoesNotThrow(() -> service.getK8sOverview());

        assertFalse(Boolean.TRUE.equals(result.getKubectlInstalled()));
        assertFalse(Boolean.TRUE.equals(result.getClusterReachable()));
        assertNotNull(result.getNodes());
        assertTrue(result.getNodes().isEmpty());
        assertNotNull(result.getNamespaces());
        assertTrue(result.getNamespaces().isEmpty());
        assertNotNull(result.getContainers());
        assertTrue(result.getContainers().isEmpty());
        assertTrue(result.getClusterMessage().contains("远程"));
    }

    @Test
    void buildK8sCleanupRemoteCommand_shouldOnlyIncludeWhitelistedCleanupActions() {
        AdminK8sCleanupRequestDTO dto = new AdminK8sCleanupRequestDTO();
        dto.setNodeName("worker-1");
        dto.setNodeIp("10.0.0.51");
        dto.setActions(List.of("IMAGE_PRUNE", "LOG_CLEAN"));

        String command = service.buildK8sCleanupRemoteCommand(dto);

        assertTrue(command.contains("10.0.0.51"));
        assertTrue(command.contains("crictl rmi --prune"));
        assertTrue(command.contains("docker image prune -af"));
        assertTrue(command.contains("/var/log/containers"));
        assertFalse(command.contains("rm -rf"));
    }

    @Test
    void buildK8sCleanupRemoteCommand_shouldRejectUnknownCleanupAction() {
        AdminK8sCleanupRequestDTO dto = new AdminK8sCleanupRequestDTO();
        dto.setNodeName("worker-1");
        dto.setNodeIp("10.0.0.51");
        dto.setActions(List.of("IMAGE_PRUNE", "rm -rf /"));

        BadRequestException error = assertThrows(BadRequestException.class, () -> service.buildK8sCleanupRemoteCommand(dto));

        assertTrue(error.getMessage().contains("不支持的清理动作"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildK8sCleanupPodManifest_shouldRunPrivilegedCleanupOnSelectedNode() {
        var node = new com.tianji.user.domain.vo.adminops.AdminK8sNodeVO();
        node.setName("worker-1");
        node.setInternalIp("10.0.0.51");

        Map<String, Object> manifest = service.buildK8sCleanupPodManifest(
                "rk-ops-cleanup-test",
                "shetuanguanlixitong",
                node,
                "c2V0IC1lCg=="
        );

        Map<String, Object> spec = (Map<String, Object>) manifest.get("spec");
        assertEquals("worker-1", spec.get("nodeName"));
        List<Map<String, Object>> containers = (List<Map<String, Object>>) spec.get("containers");
        Map<String, Object> container = containers.get(0);
        assertEquals("cleanup", container.get("name"));
        assertEquals("m.daocloud.io/docker.io/library/busybox:1.36", container.get("image"));
        assertEquals(Map.of("privileged", true), container.get("securityContext"));
        assertTrue(container.toString().contains("chroot /host"));
        assertTrue(manifest.toString().contains("hostPath"));
        assertFalse(manifest.toString().contains("rm -rf"));
    }

    @Test
    void getMonitoringOverview_shouldIncludeDynamicServicesFromNacosList() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/nacos/v1/ns/service/list", exchange -> {
            byte[] body = "{\"doms\":[\"rk-auth\",\"rk-file\",\"rk-pay\"]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/nacos/v1/ns/instance/list", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            String bodyText = query != null && query.contains("serviceName=rk-file")
                    ? "{\"hosts\":[{\"ip\":\"127.0.0.1\",\"port\":65530,\"healthy\":true}]}"
                    : "{\"hosts\":[]}";
            byte[] body = bodyText.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            ReflectionTestUtils.setField(service, "nacosAddr", "http://127.0.0.1:" + server.getAddress().getPort());
            ReflectionTestUtils.setField(service, "nacosNamespace", "test-namespace");
            ReflectionTestUtils.setField(service, "nacosGroup", "DEFAULT_GROUP");

            var overview = service.getMonitoringOverview();
            var names = overview.getServices().stream()
                    .map(item -> String.valueOf(item.get("name")))
                    .collect(Collectors.toList());

            assertEquals(List.of("rk-auth", "rk-file", "rk-pay"), names);
            assertTrue(overview.getServices().stream().anyMatch(item -> "rk-file".equals(item.get("name"))));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void getMonitoringOverview_shouldExposeHealthyInstanceDetailsFromNacosHosts() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/nacos/v1/ns/service/list", exchange -> {
            byte[] body = "{\"doms\":[\"rk-user\"]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/nacos/v1/ns/instance/list", exchange -> {
            byte[] body = ("{\"hosts\":["
                    + "{\"ip\":\"10.0.0.11\",\"port\":8080,\"healthy\":true,\"enabled\":true,\"weight\":1.0,\"clusterName\":\"DEFAULT\"},"
                    + "{\"ip\":\"10.0.0.12\",\"port\":8081,\"healthy\":false,\"enabled\":true,\"weight\":1.0,\"clusterName\":\"DEFAULT\"}"
                    + "]}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            ReflectionTestUtils.setField(service, "nacosAddr", "http://127.0.0.1:" + server.getAddress().getPort());
            ReflectionTestUtils.setField(service, "nacosNamespace", "test-namespace");
            ReflectionTestUtils.setField(service, "nacosGroup", "DEFAULT_GROUP");

            var overview = service.getMonitoringOverview();
            var serviceRow = overview.getServices().get(0);

            assertEquals(2, serviceRow.get("instances"));
            assertEquals(1L, serviceRow.get("healthyInstances"));
            var details = (List<?>) serviceRow.get("instanceDetails");
            assertEquals(2, details.size());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void getJenkinsOverview_shouldLoadJobsForTenantOne() throws Exception {
        TenantContext.setTenantId(1L);
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/json", exchange -> {
            String bodyText = "{"
                    + "\"jobs\":["
                    + "{"
                    + "\"name\":\"rk-user\","
                    + "\"url\":\"http://jenkins.local/job/rk-user/\","
                    + "\"color\":\"blue\","
                    + "\"lastBuild\":{\"number\":18,\"url\":\"http://jenkins.local/job/rk-user/18/\"},"
                    + "\"lastCompletedBuild\":{\"number\":18,\"url\":\"http://jenkins.local/job/rk-user/18/\"}"
                    + "},"
                    + "{"
                    + "\"name\":\"rk-gateway\","
                    + "\"url\":\"http://jenkins.local/job/rk-gateway/\","
                    + "\"color\":\"red_anime\","
                    + "\"lastBuild\":{\"number\":7,\"url\":\"http://jenkins.local/job/rk-gateway/7/\"},"
                    + "\"lastCompletedBuild\":{\"number\":6,\"url\":\"http://jenkins.local/job/rk-gateway/6/\"}"
                    + "}"
                    + "]"
                    + "}";
            byte[] body = bodyText.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            ReflectionTestUtils.setField(service, "jenkinsUrl", "http://127.0.0.1:" + server.getAddress().getPort());
            ReflectionTestUtils.setField(service, "jenkinsUsername", "root");
            ReflectionTestUtils.setField(service, "jenkinsPassword", "123");

            var result = service.getJenkinsOverview();

            assertEquals("http://127.0.0.1:" + server.getAddress().getPort(), result.getSourceUrl());
            assertEquals(2, result.getJobs().size());
            assertEquals("rk-user", result.getJobs().get(0).getName());
            assertEquals("success", result.getJobs().get(0).getStatus());
            assertEquals("running", result.getJobs().get(1).getStatus());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void triggerJenkinsJob_shouldUseCrumbProtectedBuildEndpoint() throws Exception {
        TenantContext.setTenantId(1L);
        AtomicBoolean crumbHeaderSeen = new AtomicBoolean(false);
        AtomicBoolean cookieHeaderSeen = new AtomicBoolean(false);
        AtomicBoolean authHeaderSeen = new AtomicBoolean(false);
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/crumbIssuer/api/json", exchange -> {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            String expected = "Basic " + Base64.getEncoder().encodeToString("root:123".getBytes(StandardCharsets.UTF_8));
            authHeaderSeen.set(expected.equals(auth));
            byte[] body = "{\"crumbRequestField\":\"Jenkins-Crumb\",\"crumb\":\"crumb-123\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Set-Cookie", "JSESSIONID=test-session; Path=/");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/job/rk-user/buildWithParameters", exchange -> {
            crumbHeaderSeen.set("crumb-123".equals(exchange.getRequestHeaders().getFirst("Jenkins-Crumb")));
            String cookie = exchange.getRequestHeaders().getFirst("Cookie");
            cookieHeaderSeen.set(cookie != null && cookie.contains("JSESSIONID=test-session"));
            int status = crumbHeaderSeen.get() && cookieHeaderSeen.get() ? 201 : 403;
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
        try {
            ReflectionTestUtils.setField(service, "jenkinsUrl", "http://127.0.0.1:" + server.getAddress().getPort());
            ReflectionTestUtils.setField(service, "jenkinsUsername", "root");
            ReflectionTestUtils.setField(service, "jenkinsPassword", "123");

            assertTrue(service.triggerJenkinsJob("rk-user"));
            assertTrue(authHeaderSeen.get());
            assertTrue(crumbHeaderSeen.get());
            assertTrue(cookieHeaderSeen.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void triggerJenkinsJob_shouldSubmitParameterizedBuildAndExposeQueueId() throws Exception {
        TenantContext.setTenantId(1L);
        AtomicBoolean parametersSeen = new AtomicBoolean(false);
        AtomicBoolean crumbHeaderSeen = new AtomicBoolean(false);
        AtomicBoolean cookieHeaderSeen = new AtomicBoolean(false);
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/crumbIssuer/api/json", exchange -> {
            byte[] body = "{\"crumbRequestField\":\"Jenkins-Crumb\",\"crumb\":\"crumb-456\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Set-Cookie", "JSESSIONID=parameterized-session; Path=/");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/job/rk-web-cloud-master-new/buildWithParameters", exchange -> {
            String payload = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            parametersSeen.set(payload.contains("TARGET_BRANCH=cloud-master-new")
                    && payload.contains("GIT_SOURCE=gitee")
                    && payload.contains("SERVICES=rk-user%2Cfrontend")
                    && payload.contains("RK_K8S_IMAGE_MODE=registry"));
            crumbHeaderSeen.set("crumb-456".equals(exchange.getRequestHeaders().getFirst("Jenkins-Crumb")));
            String cookie = exchange.getRequestHeaders().getFirst("Cookie");
            cookieHeaderSeen.set(cookie != null && cookie.contains("JSESSIONID=parameterized-session"));
            int status = parametersSeen.get() && crumbHeaderSeen.get() && cookieHeaderSeen.get() ? 201 : 403;
            exchange.getResponseHeaders().add("Location", "http://jenkins.local/queue/item/321/");
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
        try {
            ReflectionTestUtils.setField(service, "jenkinsUrl", "http://127.0.0.1:" + server.getAddress().getPort());
            ReflectionTestUtils.setField(service, "jenkinsUsername", "root");
            ReflectionTestUtils.setField(service, "jenkinsPassword", "123");
            AdminJenkinsBuildRequestDTO dto = new AdminJenkinsBuildRequestDTO();
            dto.setTargetBranch("cloud-master-new");
            dto.setGitSource("gitee");
            dto.setServices("rk-user,frontend");
            dto.setImageMode("registry");

            var result = service.triggerJenkinsJob("rk-web-cloud-master-new", dto);

            assertTrue(Boolean.TRUE.equals(result.getAccepted()));
            assertEquals("321", result.getQueueId());
            assertEquals("gitee", result.getGitSource());
            assertEquals("registry", result.getImageMode());
            assertTrue(parametersSeen.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void triggerJenkinsJob_shouldPersistPreflightDiskSnapshotBeforeQueueRecord() throws Exception {
        TenantContext.setTenantId(1L);
        UserContext.setUser(9L);
        when(jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class)).thenReturn(42L);
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/crumbIssuer/api/json", exchange -> {
            byte[] body = "{\"crumbRequestField\":\"Jenkins-Crumb\",\"crumb\":\"crumb-789\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/job/rk-web-cloud-master-new/buildWithParameters", exchange -> {
            exchange.getResponseHeaders().add("Location", "http://jenkins.local/queue/item/654/");
            exchange.sendResponseHeaders(201, -1);
            exchange.close();
        });
        server.start();
        try {
            ReflectionTestUtils.setField(service, "jenkinsUrl", "http://127.0.0.1:" + server.getAddress().getPort());
            ReflectionTestUtils.setField(service, "remotePython", "missing-python-for-test.exe");
            AdminJenkinsBuildRequestDTO dto = new AdminJenkinsBuildRequestDTO();
            dto.setTargetBranch("cloud-master-new");
            dto.setGitSource("local");
            dto.setServices("rk-user,frontend");
            dto.setImageMode("local");

            var result = service.triggerJenkinsJob("rk-web-cloud-master-new", dto);

            assertEquals(42L, result.getRecordId());
            assertNotNull(result.getPreflightSummary());
            assertTrue(result.getPreflightSummary().contains("disk") || result.getPreflightSummary().contains("preflight"));
            verify(jdbcTemplate).update(
                    contains("INSERT INTO ops_build_log"),
                    eq(42L),
                    eq(null),
                    eq(null),
                    eq("PREFLIGHT_DISK"),
                    contains("disk")
            );
        } finally {
            server.stop(0);
        }
    }

    @Test
    void getJenkinsBuildStatus_shouldResolveQueuedBuildAndReturnConsoleTail() throws Exception {
        TenantContext.setTenantId(1L);
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/queue/item/321/api/json", exchange -> {
            byte[] body = ("{\"id\":321,\"why\":null,\"cancelled\":false,"
                    + "\"executable\":{\"number\":15,\"url\":\"http://jenkins.local/job/rk-web-cloud-master-new/15/\"}}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/job/rk-web-cloud-master-new/15/api/json", exchange -> {
            byte[] body = ("{\"number\":15,\"building\":false,\"result\":\"SUCCESS\",\"duration\":120000,"
                    + "\"estimatedDuration\":180000,"
                    + "\"actions\":[{\"parameters\":["
                    + "{\"name\":\"SERVICES\",\"value\":\"rk-user\"},"
                    + "{\"name\":\"RK_K8S_IMAGE_MODE\",\"value\":\"local\"},"
                    + "{\"name\":\"GIT_SOURCE\",\"value\":\"local\"},"
                    + "{\"name\":\"TARGET_BRANCH\",\"value\":\"cloud-master-new\"}"
                    + "]}]}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/job/rk-web-cloud-master-new/15/consoleText", exchange -> {
            byte[] body = "line1\nline2\nline3\nline4\n".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            ReflectionTestUtils.setField(service, "jenkinsUrl", "http://127.0.0.1:" + server.getAddress().getPort());

            var result = service.getJenkinsBuildStatus("rk-web-cloud-master-new", "321", null, 2);

            assertEquals(15, result.getBuildNumber());
            assertEquals("success", result.getStatus());
            assertEquals("local", result.getGitSource());
            assertEquals("local", result.getImageMode());
            assertEquals("rk-user", result.getServices());
            assertEquals("line3\nline4", result.getLogTail());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void getJenkinsBuildStatus_shouldReturnFailedStatusWhenBuildNumberIsMissingFromCurrentJenkins() throws Exception {
        TenantContext.setTenantId(1L);
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/job/rk-web-cloud-master-new/161/api/json", exchange -> {
            byte[] body = "missing build".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            ReflectionTestUtils.setField(service, "jenkinsUrl", "http://127.0.0.1:" + server.getAddress().getPort());

            var result = assertDoesNotThrow(() -> service.getJenkinsBuildStatus("rk-web-cloud-master-new", null, 161, 20));

            assertEquals(161, result.getBuildNumber());
            assertEquals("failed", result.getStatus());
            assertEquals("NOT_FOUND", result.getResult());
            assertFalse(Boolean.TRUE.equals(result.getBuilding()));
            assertTrue(result.getMessage().contains("当前 Jenkins"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void getJenkinsBuildStatus_shouldRecoverBuildNumberFromRecordWhenQueueItemExpired() throws Exception {
        TenantContext.setTenantId(1L);
        when(jdbcTemplate.queryForList(
                "SELECT build_number FROM ops_build_record WHERE is_deleted = 0 AND job_name = ? AND queue_id = ? AND build_number > 0 ORDER BY id DESC LIMIT 1",
                "rk-web-cloud-master-new",
                "152"
        )).thenReturn(List.of(Map.of("build_number", 115)));
        when(jdbcTemplate.queryForList(
                "SELECT id FROM ops_build_record WHERE is_deleted = 0 AND queue_id = ? ORDER BY id DESC LIMIT 1",
                "152"
        )).thenReturn(List.of(Map.of("id", 3L)));

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/queue/item/152/api/json", exchange -> {
            byte[] body = "expired queue".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/job/rk-web-cloud-master-new/115/api/json", exchange -> {
            byte[] body = ("{\"number\":115,\"building\":false,\"result\":\"SUCCESS\",\"duration\":278864,"
                    + "\"estimatedDuration\":180000,"
                    + "\"actions\":[{\"parameters\":["
                    + "{\"name\":\"SERVICES\",\"value\":\"rk-user\"},"
                    + "{\"name\":\"RK_K8S_IMAGE_MODE\",\"value\":\"local\"},"
                    + "{\"name\":\"GIT_SOURCE\",\"value\":\"gitee\"},"
                    + "{\"name\":\"TARGET_BRANCH\",\"value\":\"cloud-master-new\"}"
                    + "]}]}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/job/rk-web-cloud-master-new/115/consoleText", exchange -> {
            byte[] body = "Finished: SUCCESS\n".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            ReflectionTestUtils.setField(service, "jenkinsUrl", "http://127.0.0.1:" + server.getAddress().getPort());

            var result = service.getJenkinsBuildStatus("rk-web-cloud-master-new", "152", null, 20);

            assertEquals(115, result.getBuildNumber());
            assertEquals("success", result.getStatus());
            assertEquals("SUCCESS", result.getResult());
            assertEquals("gitee", result.getGitSource());
            assertEquals("rk-user", result.getServices());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void getJenkinsBuildStatus_shouldRecoverBuildNumberFromLogWhenRecordMissesBuildNumber() throws Exception {
        TenantContext.setTenantId(1L);
        when(jdbcTemplate.queryForList(
                "SELECT build_number FROM ops_build_record WHERE is_deleted = 0 AND job_name = ? AND queue_id = ? AND build_number > 0 ORDER BY id DESC LIMIT 1",
                "rk-web-cloud-master-new",
                "152"
        )).thenReturn(List.of());
        when(jdbcTemplate.queryForList(
                "SELECT l.build_number FROM ops_build_log l INNER JOIN ops_build_record r ON r.id = l.record_id WHERE r.is_deleted = 0 AND r.job_name = ? AND r.queue_id = ? AND l.build_number > 0 ORDER BY l.id DESC LIMIT 1",
                "rk-web-cloud-master-new",
                "152"
        )).thenReturn(List.of(Map.of("build_number", 115)));
        when(jdbcTemplate.queryForList(
                "SELECT id FROM ops_build_record WHERE is_deleted = 0 AND queue_id = ? ORDER BY id DESC LIMIT 1",
                "152"
        )).thenReturn(List.of(Map.of("id", 3L)));

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/queue/item/152/api/json", exchange -> {
            byte[] body = "expired queue".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/job/rk-web-cloud-master-new/115/api/json", exchange -> {
            byte[] body = ("{\"number\":115,\"building\":false,\"result\":\"SUCCESS\",\"duration\":278864,"
                    + "\"estimatedDuration\":180000,"
                    + "\"actions\":[{\"parameters\":["
                    + "{\"name\":\"SERVICES\",\"value\":\"rk-user\"},"
                    + "{\"name\":\"RK_K8S_IMAGE_MODE\",\"value\":\"local\"},"
                    + "{\"name\":\"GIT_SOURCE\",\"value\":\"gitee\"},"
                    + "{\"name\":\"TARGET_BRANCH\",\"value\":\"cloud-master-new\"}"
                    + "]}]}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/job/rk-web-cloud-master-new/115/consoleText", exchange -> {
            byte[] body = "Finished: SUCCESS\n".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            ReflectionTestUtils.setField(service, "jenkinsUrl", "http://127.0.0.1:" + server.getAddress().getPort());

            var result = service.getJenkinsBuildStatus("rk-web-cloud-master-new", "152", null, 20);

            assertEquals(115, result.getBuildNumber());
            assertEquals("success", result.getStatus());
            assertEquals("SUCCESS", result.getResult());
            assertEquals("gitee", result.getGitSource());
            assertEquals("rk-user", result.getServices());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void getJenkinsBuildHistory_shouldNotPersistBuildZeroWhenQueueItemExpiredWithoutStoredBuildNumber() throws Exception {
        TenantContext.setTenantId(1L);
        when(jdbcTemplate.queryForList(
                "SELECT id, job_name, build_number, queue_id FROM ops_build_record WHERE is_deleted = 0 AND status IN ('queued', 'running') ORDER BY trigger_time DESC, id DESC LIMIT 10"
        )).thenReturn(List.of(new LinkedHashMap<>() {{
            put("id", 45L);
            put("job_name", "rk-web-cloud-master-new");
            put("build_number", null);
            put("queue_id", "461");
        }}));
        when(jdbcTemplate.queryForList(
                "SELECT build_number FROM ops_build_record WHERE is_deleted = 0 AND job_name = ? AND queue_id = ? AND build_number > 0 ORDER BY id DESC LIMIT 1",
                "rk-web-cloud-master-new",
                "461"
        )).thenReturn(List.of());
        when(jdbcTemplate.queryForList(
                "SELECT l.build_number FROM ops_build_log l INNER JOIN ops_build_record r ON r.id = l.record_id WHERE r.is_deleted = 0 AND r.job_name = ? AND r.queue_id = ? AND l.build_number > 0 ORDER BY l.id DESC LIMIT 1",
                "rk-web-cloud-master-new",
                "461"
        )).thenReturn(List.of());
        when(jdbcTemplate.queryForList(
                "SELECT id FROM ops_build_record WHERE is_deleted = 0 AND queue_id = ? ORDER BY id DESC LIMIT 1",
                "461"
        )).thenReturn(List.of(Map.of("id", 45L)));
        when(jdbcTemplate.queryForList(
                contains("FROM ops_build_record WHERE is_deleted = 0 ORDER BY trigger_time DESC"),
                any(Object[].class)
        )).thenReturn(List.of());

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/queue/item/461/api/json", exchange -> {
            byte[] body = "expired queue".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            ReflectionTestUtils.setField(service, "jenkinsUrl", "http://127.0.0.1:" + server.getAddress().getPort());

            service.getJenkinsBuildHistory(null, null, null, null, null, null, null);

            verify(jdbcTemplate).update(
                    contains("UPDATE ops_build_record SET build_number = ?"),
                    eq(null),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    eq("failed"),
                    eq("FINISHED"),
                    eq("NOT_FOUND"),
                    contains("queueId=461"),
                    eq(1),
                    eq(0L),
                    contains("requestPath=/queue/item/461/api/json"),
                    eq(45L)
            );
        } finally {
            server.stop(0);
        }
    }

    @Test
    void getJenkinsBuildHistory_shouldResolveLeftQueueItemBeforeListing() throws Exception {
        TenantContext.setTenantId(1L);
        when(jdbcTemplate.queryForList(
                "SELECT id, job_name, build_number, queue_id FROM ops_build_record WHERE is_deleted = 0 AND status IN ('queued', 'running') ORDER BY trigger_time DESC, id DESC LIMIT 10"
        )).thenReturn(List.of(Map.of(
                "id", 33L,
                "job_name", "rk-web-cloud-master-new",
                "queue_id", "423"
        )));
        when(jdbcTemplate.queryForList(
                "SELECT id FROM ops_build_record WHERE is_deleted = 0 AND queue_id = ? ORDER BY id DESC LIMIT 1",
                "423"
        )).thenReturn(List.of(Map.of("id", 33L)));
        when(jdbcTemplate.queryForList(
                contains("FROM ops_build_record WHERE is_deleted = 0 ORDER BY trigger_time DESC"),
                any(Object[].class)
        )).thenReturn(List.of());

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/queue/item/423/api/json", exchange -> {
            byte[] body = ("{\"id\":423,\"blocked\":false,\"cancelled\":false,"
                    + "\"executable\":{\"number\":163,\"url\":\"http://jenkins.local/job/rk-web-cloud-master-new/163/\"}}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/job/rk-web-cloud-master-new/163/api/json", exchange -> {
            byte[] body = ("{\"number\":163,\"building\":false,\"result\":\"SUCCESS\",\"duration\":626692,"
                    + "\"estimatedDuration\":775670,"
                    + "\"actions\":[{\"parameters\":["
                    + "{\"name\":\"SERVICES\",\"value\":\"rk-user,frontend\"},"
                    + "{\"name\":\"RK_K8S_IMAGE_MODE\",\"value\":\"local\"},"
                    + "{\"name\":\"GIT_SOURCE\",\"value\":\"gitee\"},"
                    + "{\"name\":\"TARGET_BRANCH\",\"value\":\"cloud-master-new\"}"
                    + "]}]}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/job/rk-web-cloud-master-new/163/consoleText", exchange -> {
            byte[] body = "Finished: SUCCESS\n".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            ReflectionTestUtils.setField(service, "jenkinsUrl", "http://127.0.0.1:" + server.getAddress().getPort());

            service.getJenkinsBuildHistory(null, null, null, null, null, null, null);

            verify(jdbcTemplate).update(
                    contains("UPDATE ops_build_record SET build_number = ?"),
                    eq(163),
                    any(),
                    eq("gitee"),
                    eq("cloud-master-new"),
                    eq("local"),
                    eq("rk-user,frontend"),
                    eq("success"),
                    eq("FINISHED"),
                    eq("SUCCESS"),
                    eq(null),
                    eq(1),
                    eq(626692L),
                    contains("Finished: SUCCESS"),
                    eq(33L)
            );
        } finally {
            server.stop(0);
        }
    }

    @Test
    void getJenkinsBuildStatus_shouldAttachBuildNumberPollToLatestQueueOnlyRecord() throws Exception {
        TenantContext.setTenantId(1L);
        when(jdbcTemplate.queryForList(
                "SELECT id FROM ops_build_record WHERE is_deleted = 0 AND job_name = ? AND build_number = ? ORDER BY id DESC LIMIT 1",
                "rk-web-cloud-master-new",
                164
        )).thenReturn(List.of());
        when(jdbcTemplate.queryForList(
                "SELECT id FROM ops_build_record WHERE is_deleted = 0 AND job_name = ? AND queue_id IS NOT NULL AND build_number IS NULL AND status IN ('queued', 'running') ORDER BY trigger_time DESC, id DESC LIMIT 1",
                "rk-web-cloud-master-new"
        )).thenReturn(List.of(Map.of("id", 35L)));

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/job/rk-web-cloud-master-new/164/api/json", exchange -> {
            byte[] body = ("{\"number\":164,\"building\":false,\"result\":\"SUCCESS\",\"duration\":355000,"
                    + "\"estimatedDuration\":700000,"
                    + "\"actions\":[{\"parameters\":["
                    + "{\"name\":\"SERVICES\",\"value\":\"rk-user\"},"
                    + "{\"name\":\"RK_K8S_IMAGE_MODE\",\"value\":\"local\"},"
                    + "{\"name\":\"GIT_SOURCE\",\"value\":\"gitee\"},"
                    + "{\"name\":\"TARGET_BRANCH\",\"value\":\"cloud-master-new\"}"
                    + "]}]}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/job/rk-web-cloud-master-new/164/consoleText", exchange -> {
            byte[] body = "Finished: SUCCESS\n".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            ReflectionTestUtils.setField(service, "jenkinsUrl", "http://127.0.0.1:" + server.getAddress().getPort());

            var result = service.getJenkinsBuildStatus("rk-web-cloud-master-new", null, 164, 20);

            assertEquals(164, result.getBuildNumber());
            assertEquals("success", result.getStatus());
            verify(jdbcTemplate).update(
                    contains("UPDATE ops_build_record SET build_number = ?"),
                    eq(164),
                    any(),
                    eq("gitee"),
                    eq("cloud-master-new"),
                    eq("local"),
                    eq("rk-user"),
                    eq("success"),
                    eq("FINISHED"),
                    eq("SUCCESS"),
                    eq(null),
                    eq(1),
                    eq(355000L),
                    contains("Finished: SUCCESS"),
                    eq(35L)
            );
        } finally {
            server.stop(0);
        }
    }

    @Test
    void getJenkinsGitBranches_shouldReadBranchesFromGitSmartHttp() throws Exception {
        TenantContext.setTenantId(1L);
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/repo.git/info/refs", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            byte[] body = "service=git-upload-pack".equals(query)
                    ? gitInfoRefs(
                    "# service=git-upload-pack\n",
                    "888e57e0c8c2e1c82ae2a8fcbcbea685d90ea668 refs/heads/feature/demo\n",
                    "6c862c37ae147e1a5515674dcdab053d5a80de1b refs/heads/cloud-master-new\u0000multi_ack\n")
                    : new byte[0];
            exchange.getResponseHeaders().add("Content-Type", "application/x-git-upload-pack-advertisement");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            ReflectionTestUtils.setField(service, "jenkinsGitLocalUrl", "http://127.0.0.1:" + server.getAddress().getPort() + "/repo.git");

            var result = service.getJenkinsGitBranches("local");

            assertEquals("local", result.getSource());
            assertEquals("本地 Gogs", result.getLabel());
            assertEquals(List.of("cloud-master-new", "feature/demo"), result.getBranches());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void getJenkinsGitBranches_shouldFallbackToConfiguredBranchesWhenRemoteUnavailable() {
        TenantContext.setTenantId(1L);
        ReflectionTestUtils.setField(service, "jenkinsGitLocalUrl", "http://127.0.0.1:9/missing.git");
        ReflectionTestUtils.setField(service, "jenkinsGitFallbackBranches", "cloud-master-new,feature/demo");

        var result = service.getJenkinsGitBranches("local");

        assertEquals(List.of("cloud-master-new", "feature/demo"), result.getBranches());
        assertTrue(Boolean.TRUE.equals(result.getFallback()));
        assertNotNull(result.getFailureReason());
        assertTrue(result.getMessage().contains("远程分支读取失败"));
    }

    @Test
    void saveNacosConfig_shouldAuditInvalidJsonSaveAttempt() {
        TenantContext.setTenantId(1L);
        UserContext.setUser(9L);
        AdminNacosConfigSaveDTO dto = new AdminNacosConfigSaveDTO();
        dto.setNamespaceId("");
        dto.setGroupName("DEFAULT_GROUP");
        dto.setDataId("rk-user.json");
        dto.setType("json");
        dto.setContent("{broken-json");

        BadRequestException error = assertThrows(BadRequestException.class, () -> service.saveNacosConfig(dto));

        assertTrue(error.getMessage().contains("Nacos JSON content invalid"));
        verify(jdbcTemplate).update(
                contains("INSERT INTO ops_operation_audit"),
                eq("NACOS_CONFIG"),
                eq(null),
                eq("DEFAULT_GROUP"),
                eq("rk-user.json"),
                eq("SAVE"),
                eq("failed"),
                eq(null),
                contains("\"type\":\"json\""),
                contains("Nacos JSON content invalid"),
                eq(9L),
                eq("9")
        );
    }

    @Test
    void mapServiceRegistryRow_shouldTreatBooleanTinyintAsEnabled() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", 1L);
        row.put("service_code", "rk-user");
        row.put("display_name", "用户服务");
        row.put("service_type", "backend");
        row.put("git_source", "local");
        row.put("git_repo", "http://rk-gogs:3000/tjxt/rk-web.git");
        row.put("branch_name", "cloud-master-new");
        row.put("module_path", "rk-user");
        row.put("build_mode", "maven-docker");
        row.put("image_name", "rk-web/rk-user");
        row.put("namespace_name", "shetuanguanlixitong");
        row.put("workload_type", "StatefulSet");
        row.put("workload_name", "rk-server-rk-user");
        row.put("container_name", "rk-user");
        row.put("health_check_path", "/actuator/health");
        row.put("resource_limits", "512Mi");
        row.put("enabled", Boolean.TRUE);
        row.put("sort_order", 20);

        var result = ReflectionTestUtils.<com.tianji.user.domain.vo.adminops.AdminOpsServiceVO>invokeMethod(service, "mapServiceRegistryRow", row);

        assertTrue(Boolean.TRUE.equals(result.getEnabled()));
    }

    @Test
    void getJenkinsOverview_shouldRejectNonTenantOne() {
        TenantContext.setTenantId(2L);

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.getJenkinsOverview());

        assertTrue(error.getMessage().contains("tenant 1"));
    }

    @Test
    void runK8sWorkloadAction_shouldRequireDangerConfirmationForRestart() {
        TenantContext.setTenantId(1L);
        AdminK8sWorkloadActionDTO dto = new AdminK8sWorkloadActionDTO();
        dto.setNamespace("shetuanguanlixitong");
        dto.setKind("StatefulSet");
        dto.setName("rk-server-rk-user");
        dto.setAction("RESTART");

        BadRequestException error = assertThrows(BadRequestException.class, () -> service.runK8sWorkloadAction(dto));

        assertTrue(error.getMessage().contains("confirm"));
    }

    @Test
    void runK8sWorkloadAction_shouldPersistAuditLogWhenConfirmed() {
        TenantContext.setTenantId(1L);
        UserContext.setUser(9L);
        when(jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class)).thenReturn(77L);
        ReflectionTestUtils.setField(service, "remotePython", "missing-python-for-test.exe");
        AdminK8sWorkloadActionDTO dto = new AdminK8sWorkloadActionDTO();
        dto.setNamespace("shetuanguanlixitong");
        dto.setKind("StatefulSet");
        dto.setName("rk-server-rk-user");
        dto.setAction("RESTART");
        dto.setConfirmText("rk-server-rk-user");
        dto.setReason("release rollout");

        var result = service.runK8sWorkloadAction(dto);

        assertFalse(Boolean.TRUE.equals(result.getSuccess()));
        assertEquals(77L, result.getAuditId());
        assertEquals("release rollout", result.getReason());
        verify(jdbcTemplate).update(
                contains("INSERT INTO ops_operation_audit"),
                eq("K8S_WORKLOAD"),
                eq("shetuanguanlixitong"),
                eq("StatefulSet"),
                eq("rk-server-rk-user"),
                eq("RESTART"),
                eq("failed"),
                eq("release rollout"),
                contains("\"action\":\"RESTART\""),
                anyString(),
                eq(9L),
                eq("9")
        );
    }

    @Test
    void getApiWorkbenchResources_shouldLoadGatewaySwaggerResourcesForTenantOne() throws Exception {
        TenantContext.setTenantId(1L);
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/swagger-resources", exchange -> {
            byte[] body = ("["
                    + "{\"name\":\"rk-user\",\"url\":\"/users/v2/api-docs\"},"
                    + "{\"name\":\"rk-content\",\"url\":\"/content/v2/api-docs\"}"
                    + "]").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            ReflectionTestUtils.setField(service, "gatewayUrl", "http://127.0.0.1:" + server.getAddress().getPort());

            var result = service.getApiWorkbenchResources();

            assertEquals(2, result.size());
            assertEquals("rk-user", result.get(0).getName());
            assertEquals("/users/v2/api-docs", result.get(0).getUrl());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void getApiWorkbenchDoc_shouldParseSwaggerPathsIntoEndpointRows() throws Exception {
        TenantContext.setTenantId(1L);
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/users/v2/api-docs", exchange -> {
            String bodyText = "{"
                    + "\"basePath\":\"/\","
                    + "\"paths\":{"
                    + "\"/admin/ops/monitoring/overview\":{"
                    + "\"get\":{\"summary\":\"Monitoring overview\",\"operationId\":\"getMonitoringOverview\",\"tags\":[\"Admin Ops\"]}"
                    + "},"
                    + "\"/api/members/{id}\":{"
                    + "\"put\":{\"summary\":\"Update member\",\"operationId\":\"updateMember\",\"tags\":[\"Club Member\"]}"
                    + "}"
                    + "}"
                    + "}";
            byte[] body = bodyText.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            ReflectionTestUtils.setField(service, "gatewayUrl", "http://127.0.0.1:" + server.getAddress().getPort());

            AdminApiWorkbenchDocVO result = service.getApiWorkbenchDoc("/users/v2/api-docs");

            assertEquals("/", result.getBasePath());
            assertEquals(2, result.getEndpoints().size());
            assertEquals("/admin/ops/monitoring/overview", result.getEndpoints().get(0).getPath());
            assertEquals("GET", result.getEndpoints().get(0).getMethod());
            assertEquals("Monitoring overview", result.getEndpoints().get(0).getSummary());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void getApiWorkbenchDoc_shouldExposeGatewayCallableNoticePaths() throws Exception {
        TenantContext.setTenantId(1L);
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/notifications/v2/api-docs", exchange -> {
            String bodyText = "{"
                    + "\"basePath\":\"/\","
                    + "\"paths\":{"
                    + "\"/api/notices/list\":{"
                    + "\"get\":{\"summary\":\"Notice list\",\"operationId\":\"getNoticeList\",\"tags\":[\"Notice\"]}"
                    + "}"
                    + "}"
                    + "}";
            byte[] body = bodyText.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            ReflectionTestUtils.setField(service, "gatewayUrl", "http://127.0.0.1:" + server.getAddress().getPort());

            AdminApiWorkbenchDocVO result = service.getApiWorkbenchDoc("/notifications/v2/api-docs");

            assertEquals(1, result.getEndpoints().size());
            assertEquals("/notifications/api/notices/list", result.getEndpoints().get(0).getPath());
            assertEquals("GET", result.getEndpoints().get(0).getMethod());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void getApiWorkbenchDoc_shouldForwardCurrentAuthorizationHeaderToGatewayDocRequest() throws Exception {
        TenantContext.setTenantId(1L);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer workbench-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/users/v2/api-docs", exchange -> {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            String bodyText = "Bearer workbench-token".equals(auth)
                    ? "{"
                    + "\"basePath\":\"/\","
                    + "\"paths\":{"
                    + "\"/users/page\":{\"get\":{\"summary\":\"Query users\",\"operationId\":\"pageUsers\",\"tags\":[\"User\"]}}"
                    + "}"
                    + "}"
                    : "{\"code\":401,\"msg\":\"Unauthorized\"}";
            int status = "Bearer workbench-token".equals(auth) ? 200 : 401;
            byte[] body = bodyText.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            ReflectionTestUtils.setField(service, "gatewayUrl", "http://127.0.0.1:" + server.getAddress().getPort());

            AdminApiWorkbenchDocVO result = service.getApiWorkbenchDoc("/users/v2/api-docs");

            assertEquals(1, result.getEndpoints().size());
            assertEquals("/users/page", result.getEndpoints().get(0).getPath());
            assertEquals("GET", result.getEndpoints().get(0).getMethod());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void getApiWorkbenchDoc_shouldFallbackToDirectServiceDocWhenGatewayDocRouteReturns404() throws Exception {
        TenantContext.setTenantId(1L);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer workbench-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        HttpServer gatewayServer = HttpServer.create(new InetSocketAddress(0), 0);
        gatewayServer.createContext("/users/v2/api-docs", exchange -> {
            byte[] body = "{\"code\":404,\"msg\":\"Not Found\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        HttpServer serviceServer = HttpServer.create(new InetSocketAddress(0), 0);
        serviceServer.createContext("/v2/api-docs", exchange -> {
            String bodyText = "{"
                    + "\"basePath\":\"/\","
                    + "\"paths\":{"
                    + "\"/users/detail\":{\"get\":{\"summary\":\"User detail\",\"operationId\":\"userDetail\",\"tags\":[\"User\"]}}"
                    + "}"
                    + "}";
            byte[] body = bodyText.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        gatewayServer.start();
        serviceServer.start();
        try {
            ReflectionTestUtils.setField(service, "gatewayUrl", "http://127.0.0.1:" + gatewayServer.getAddress().getPort());
            ReflectionTestUtils.setField(service, "workbenchDocServiceBaseUrls", Map.of(
                    "users", "http://127.0.0.1:" + serviceServer.getAddress().getPort()
            ));

            AdminApiWorkbenchDocVO result = service.getApiWorkbenchDoc("/users/v2/api-docs");

            assertEquals(1, result.getEndpoints().size());
            assertEquals("/users/detail", result.getEndpoints().get(0).getPath());
            assertEquals("GET", result.getEndpoints().get(0).getMethod());
        } finally {
            gatewayServer.stop(0);
            serviceServer.stop(0);
        }
    }

    @Test
    void resolveDirectWorkbenchDocUrl_shouldUseK8sHeadlessServiceDnsNames() {
        assertEquals(
                "http://rk-server-rk-user:8082/v2/api-docs",
                ReflectionTestUtils.invokeMethod(service, "resolveDirectWorkbenchDocUrl", "/users/v2/api-docs")
        );
        assertEquals(
                "http://rk-server-rk-auth:8081/v2/api-docs",
                ReflectionTestUtils.invokeMethod(service, "resolveDirectWorkbenchDocUrl", "/auth/v2/api-docs")
        );
        assertEquals(
                "http://rk-server-rk-file:8084/v2/api-docs",
                ReflectionTestUtils.invokeMethod(service, "resolveDirectWorkbenchDocUrl", "/files/v2/api-docs")
        );
    }

    @Test
    void getApiWorkbenchResources_shouldRejectNonTenantOne() {
        TenantContext.setTenantId(2L);

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.getApiWorkbenchResources());

        assertTrue(error.getMessage().contains("tenant 1"));
    }

    private static byte[] gitInfoRefs(String... payloads) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (String payload : payloads) {
            byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
            String header = String.format("%04x", bytes.length + 4);
            out.writeBytes(header.getBytes(StandardCharsets.UTF_8));
            out.writeBytes(bytes);
        }
        out.writeBytes("0000".getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }
}
