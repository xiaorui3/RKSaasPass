package com.tianji.user.service.impl;

import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.TenantContext;
import com.tianji.common.utils.UserContext;
import com.tianji.user.domain.dto.adminops.AdminReleaseApplyUpdateDTO;
import com.tianji.user.domain.dto.adminops.AdminReleaseDeployDTO;
import com.tianji.user.domain.dto.adminops.AdminReleaseDeploymentContext;
import com.tianji.user.domain.dto.adminops.AdminReleasePublishCurrentDTO;
import com.tianji.user.domain.dto.adminops.AdminReleaseUpdateManifestDTO;
import com.tianji.user.domain.vo.adminops.AdminReleaseDeploymentResult;
import com.tianji.user.domain.vo.adminops.AdminReleaseRegistryPublishResult;
import com.tianji.user.service.IAdminOpsService;
import com.tianji.user.service.IAdminReleaseDeploymentExecutor;
import com.tianji.user.service.IAdminReleaseRegistryPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminReleaseCenterServiceImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private IAdminOpsService adminOpsService;

    @Mock
    private IAdminReleaseRegistryPublisher registryPublisher;

    @Mock
    private IAdminReleaseDeploymentExecutor k3sDeploymentExecutor;

    @Mock
    private IAdminReleaseDeploymentExecutor composeDeploymentExecutor;

    private AdminReleaseCenterServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminReleaseCenterServiceImpl(
                jdbcTemplate,
                adminOpsService,
                registryPublisher,
                k3sDeploymentExecutor,
                composeDeploymentExecutor
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        UserContext.removeUser();
    }

    @Test
    void listReleaseServices_shouldMergeRegistryServicesCurrentImagesAndRecordedVersions() {
        TenantContext.setTenantId(1L);
        when(jdbcTemplate.queryForList(contains("FROM ops_service_registry")))
                .thenReturn(List.of(serviceRow()));
        when(jdbcTemplate.queryForList(contains("FROM ops_release_version")))
                .thenReturn(List.of(Map.of(
                        "id", 11L,
                        "service_code", "rk-user",
                        "version_tag", "20260618.1",
                        "registry_image", "registry.example.com/rk-web/rk-user:20260618.1",
                        "source_image", "registry.example.com/rk-web/rk-user:old",
                        "source_type", "manual",
                        "status", "success",
                        "create_time", "2026-06-18 16:00:00"
                )));

        var result = service.listReleaseServices("shetuanguanlixitong");

        assertEquals(1, result.size());
        assertEquals("rk-user", result.get(0).getServiceCode());
        assertEquals("registry.example.com/rk-web/rk-user:old", result.get(0).getCurrentImage());
        assertEquals("old", result.get(0).getCurrentVersion());
        assertEquals(1, result.get(0).getAvailableVersions().size());
    }

    @Test
    void publishCurrentImage_shouldRecordVersionAndTaskForCurrentRunningImage() {
        TenantContext.setTenantId(1L);
        UserContext.setUser(9L);
        when(jdbcTemplate.queryForList(contains("FROM ops_service_registry"), eq("rk-user")))
                .thenReturn(List.of(serviceRow()));
        when(jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class))
                .thenReturn(100L, 200L);
        AdminReleasePublishCurrentDTO dto = new AdminReleasePublishCurrentDTO();
        dto.setCurrentImage("registry.example.com/rk-web/rk-user:old");
        dto.setRegistryPrefix("registry.example.com/rk-web");
        dto.setVersionTag("20260618.1");
        dto.setDryRun(true);
        when(registryPublisher.publishCurrentImage(
                eq("rk-user"),
                eq("registry.example.com/rk-web/rk-user:old"),
                eq("registry.example.com/rk-web/rk-user:20260618.1"),
                eq(true)
        )).thenReturn(AdminReleaseRegistryPublishResult.success(
                "registry-push",
                100,
                "dry-run registry push: docker tag registry.example.com/rk-web/rk-user:old registry.example.com/rk-web/rk-user:20260618.1 && docker push registry.example.com/rk-web/rk-user:20260618.1\n"
        ));

        var result = service.publishCurrentImage("rk-user", dto);

        assertEquals(200L, result.getVersionId());
        assertEquals("success", result.getStatus());
        assertTrue(result.getLogs().contains("dry-run registry push"));
        verify(jdbcTemplate).update(
                contains("INSERT INTO ops_release_task"),
                eq("publish-current"),
                eq("rk-user"),
                eq(null),
                eq(null),
                eq("success"),
                any(),
                eq(100),
                any(),
                eq(9L)
        );
        verify(jdbcTemplate).update(
                contains("INSERT INTO ops_release_version"),
                eq("rk-user"),
                eq("20260618.1"),
                eq("registry.example.com/rk-web/rk-user:old"),
                eq("registry.example.com/rk-web/rk-user:20260618.1"),
                eq("running-image"),
                any(),
                eq("success"),
                eq(9L)
        );
    }

    @Test
    void publishCurrentImage_shouldUseRegistryPublisherForRealPushAndRecordVersionAfterSuccess() {
        TenantContext.setTenantId(1L);
        UserContext.setUser(9L);
        when(jdbcTemplate.queryForList(contains("FROM ops_service_registry"), eq("rk-user")))
                .thenReturn(List.of(serviceRow()));
        when(jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class))
                .thenReturn(100L, 200L);
        AdminReleasePublishCurrentDTO dto = new AdminReleasePublishCurrentDTO();
        dto.setCurrentImage("registry.example.com/rk-web/rk-user:old");
        dto.setRegistryPrefix("registry.example.com/rk-web");
        dto.setVersionTag("20260618.2");
        dto.setDryRun(false);
        when(registryPublisher.publishCurrentImage(
                eq("rk-user"),
                eq("registry.example.com/rk-web/rk-user:old"),
                eq("registry.example.com/rk-web/rk-user:20260618.2"),
                eq(false)
        )).thenReturn(AdminReleaseRegistryPublishResult.success(
                "registry-push",
                100,
                "docker login registry.example.com --username ****** --password-stdin\n"
                        + "__RK_RELEASE_REGISTRY_PUSH__ DONE 1 1 registry.example.com/rk-web/rk-user:20260618.2\n"
        ));

        var result = service.publishCurrentImage("rk-user", dto);

        assertEquals(200L, result.getVersionId());
        assertEquals("success", result.getStatus());
        assertEquals("registry-push", result.getCurrentStep());
        assertTrue(result.getLogs().contains("--password-stdin"));
        verify(registryPublisher).publishCurrentImage(
                eq("rk-user"),
                eq("registry.example.com/rk-web/rk-user:old"),
                eq("registry.example.com/rk-web/rk-user:20260618.2"),
                eq(false)
        );
        verify(jdbcTemplate).update(
                contains("INSERT INTO ops_release_version"),
                eq("rk-user"),
                eq("20260618.2"),
                eq("registry.example.com/rk-web/rk-user:old"),
                eq("registry.example.com/rk-web/rk-user:20260618.2"),
                eq("running-image"),
                any(),
                eq("success"),
                eq(9L)
        );
    }

    @Test
    void publishCurrentImage_shouldRecordFailedTaskWithoutVersionAndRedactSensitiveLogs() {
        TenantContext.setTenantId(1L);
        UserContext.setUser(9L);
        when(jdbcTemplate.queryForList(contains("FROM ops_service_registry"), eq("rk-user")))
                .thenReturn(List.of(serviceRow()));
        when(jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class))
                .thenReturn(100L);
        AdminReleasePublishCurrentDTO dto = new AdminReleasePublishCurrentDTO();
        dto.setCurrentImage("registry.example.com/rk-web/rk-user:old");
        dto.setRegistryPrefix("registry.example.com/rk-web");
        dto.setVersionTag("20260618.3");
        dto.setDryRun(false);
        String fakePassword = "super" + "-secret";
        String fakeToken = "abc" + "123";
        when(registryPublisher.publishCurrentImage(
                eq("rk-user"),
                eq("registry.example.com/rk-web/rk-user:old"),
                eq("registry.example.com/rk-web/rk-user:20260618.3"),
                eq(false)
        )).thenReturn(AdminReleaseRegistryPublishResult.failed(
                "registry-push",
                65,
                "docker login --username admin --password " + fakePassword + "\n"
                        + "REGISTRY_PASSWORD=" + fakePassword + "\n"
                        + "access token=" + fakeToken + "\n"
        ));

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> service.publishCurrentImage("rk-user", dto));

        assertTrue(error.getMessage().contains("taskId=100"));
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(
                contains("INSERT INTO ops_release_task"),
                eq("publish-current"),
                eq("rk-user"),
                eq(null),
                eq(null),
                eq("failed"),
                eq("registry-push"),
                eq(65),
                logCaptor.capture(),
                eq(9L)
        );
        String persistedLogs = logCaptor.getValue();
        assertFalse(persistedLogs.contains(fakePassword));
        assertFalse(persistedLogs.contains(fakeToken));
        assertTrue(persistedLogs.contains("******"));
        verify(jdbcTemplate, never()).update(
                contains("INSERT INTO ops_release_version"),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void deployServiceVersion_shouldRejectImagesOutsideAllowedRegistry() {
        TenantContext.setTenantId(1L);
        AdminReleaseDeployDTO dto = new AdminReleaseDeployDTO();
        dto.setTargetImage("evil.example.com/rk-user:latest");
        dto.setConfirmText("rk-user");

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> service.deployServiceVersion("rk-user", dto));

        assertTrue(error.getMessage().contains("registry"));
    }

    @Test
    void deployServiceVersion_shouldRecordDryRunDeploymentAndAudit() {
        TenantContext.setTenantId(1L);
        UserContext.setUser(9L);
        when(k3sDeploymentExecutor.supports("k3s")).thenReturn(true);
        when(k3sDeploymentExecutor.deploy(any(AdminReleaseDeploymentContext.class)))
                .thenReturn(AdminReleaseDeploymentResult.success("k3s-rollout", 100,
                        "kubectl set image statefulset/rk-server-rk-user rk-user=registry.example.com/rk-web/rk-user:20260618.1 -n shetuanguanlixitong\n"
                                + "dry-run only, command not executed\n"));
        when(jdbcTemplate.queryForList(contains("FROM ops_service_registry"), eq("rk-user")))
                .thenReturn(List.of(serviceRow()));
        when(jdbcTemplate.queryForList(contains("FROM ops_release_version"), eq(11L)))
                .thenReturn(List.of(Map.of(
                        "id", 11L,
                        "service_code", "rk-user",
                        "version_tag", "20260618.1",
                        "registry_image", "registry.example.com/rk-web/rk-user:20260618.1",
                        "source_image", "registry.example.com/rk-web/rk-user:old",
                        "source_type", "running-image",
                        "status", "success",
                        "create_time", "2026-06-18 16:00:00"
                )));
        when(jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class))
                .thenReturn(300L, 400L);
        AdminReleaseDeployDTO dto = new AdminReleaseDeployDTO();
        dto.setVersionId(11L);
        dto.setPreviousImage("registry.example.com/rk-web/rk-user:old");
        dto.setConfirmText("rk-user");
        dto.setDryRun(true);

        var result = service.deployServiceVersion("rk-user", dto);

        assertEquals(300L, result.getTaskId());
        assertEquals("success", result.getStatus());
        assertTrue(result.getLogs().contains("kubectl set image statefulset/rk-server-rk-user rk-user=registry.example.com/rk-web/rk-user:20260618.1"));
        assertEquals("k3s", result.getRuntimeMode());
        ArgumentCaptor<AdminReleaseDeploymentContext> contextCaptor = ArgumentCaptor.forClass(AdminReleaseDeploymentContext.class);
        verify(k3sDeploymentExecutor).deploy(contextCaptor.capture());
        assertEquals("k3s", contextCaptor.getValue().getRuntimeMode());
        assertEquals("registry.example.com/rk-web/rk-user:20260618.1", contextCaptor.getValue().getTargetImage());
        verify(jdbcTemplate).update(
                contains("INSERT INTO ops_release_deployment"),
                eq("rk-user"),
                eq(11L),
                eq("k3s"),
                eq("shetuanguanlixitong"),
                eq("StatefulSet"),
                eq("rk-server-rk-user"),
                eq("rk-user"),
                eq("registry.example.com/rk-web/rk-user:old"),
                eq("registry.example.com/rk-web/rk-user:20260618.1"),
                eq("success"),
                any(),
                eq(9L)
        );
        ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, atLeastOnce()).update(contains("INSERT INTO ops_operation_audit"),
                eq("RELEASE_CENTER"),
                eq("shetuanguanlixitong"),
                eq("StatefulSet"),
                eq("rk-server-rk-user"),
                actionCaptor.capture(),
                eq("success"),
                any(),
                any(),
                any(),
                eq(9L),
                eq("9")
        );
        assertFalse(actionCaptor.getAllValues().isEmpty());
    }

    @Test
    void deployServiceVersion_shouldRouteComposeRuntimeToComposeExecutor() {
        TenantContext.setTenantId(1L);
        UserContext.setUser(9L);
        when(composeDeploymentExecutor.supports("docker-compose")).thenReturn(true);
        when(composeDeploymentExecutor.deploy(any(AdminReleaseDeploymentContext.class)))
                .thenReturn(AdminReleaseDeploymentResult.success("compose-rollout", 100,
                        "docker compose -p rk-web -f docker-compose.yaml pull rk-user\n"
                                + "docker compose -p rk-web -f docker-compose.yaml up -d --no-deps rk-user\n"
                                + "dry-run only, command not executed\n"));
        when(jdbcTemplate.queryForList(contains("FROM ops_service_registry"), eq("rk-user")))
                .thenReturn(List.of(serviceRow()));
        when(jdbcTemplate.queryForList(contains("FROM ops_release_version"), eq(11L)))
                .thenReturn(List.of(Map.of(
                        "id", 11L,
                        "service_code", "rk-user",
                        "version_tag", "20260618.1",
                        "registry_image", "registry.example.com/rk-web/rk-user:20260618.1",
                        "source_image", "registry.example.com/rk-web/rk-user:old",
                        "source_type", "running-image",
                        "status", "success",
                        "create_time", "2026-06-18 16:00:00"
                )));
        when(jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class))
                .thenReturn(300L, 400L);
        AdminReleaseDeployDTO dto = new AdminReleaseDeployDTO();
        dto.setVersionId(11L);
        dto.setRuntimeMode("docker-compose");
        dto.setComposeProjectName("rk-web");
        dto.setComposeServiceName("rk-user");
        dto.setComposeFile("docker-compose.yaml");
        dto.setPreviousImage("registry.example.com/rk-web/rk-user:old");
        dto.setConfirmText("rk-user");
        dto.setDryRun(true);

        var result = service.deployServiceVersion("rk-user", dto);

        assertEquals(300L, result.getTaskId());
        assertEquals("success", result.getStatus());
        assertEquals("docker-compose", result.getRuntimeMode());
        assertEquals("compose-rollout", result.getCurrentStep());
        assertTrue(result.getLogs().contains("docker compose -p rk-web"));
        ArgumentCaptor<AdminReleaseDeploymentContext> contextCaptor = ArgumentCaptor.forClass(AdminReleaseDeploymentContext.class);
        verify(composeDeploymentExecutor).deploy(contextCaptor.capture());
        assertEquals("docker-compose", contextCaptor.getValue().getRuntimeMode());
        assertEquals("rk-web", contextCaptor.getValue().getComposeProjectName());
        assertEquals("rk-user", contextCaptor.getValue().getComposeServiceName());
        verify(jdbcTemplate).update(
                contains("INSERT INTO ops_release_deployment"),
                eq("rk-user"),
                eq(11L),
                eq("docker-compose"),
                eq("shetuanguanlixitong"),
                eq("StatefulSet"),
                eq("rk-server-rk-user"),
                eq("rk-user"),
                eq("registry.example.com/rk-web/rk-user:old"),
                eq("registry.example.com/rk-web/rk-user:20260618.1"),
                eq("success"),
                any(),
                eq(9L)
        );
    }

    @Test
    void deployServiceVersion_shouldRecordFailedTaskWhenExecutorFails() {
        TenantContext.setTenantId(1L);
        UserContext.setUser(9L);
        when(k3sDeploymentExecutor.supports("k3s")).thenReturn(true);
        when(k3sDeploymentExecutor.deploy(any(AdminReleaseDeploymentContext.class)))
                .thenReturn(AdminReleaseDeploymentResult.failed("k3s-rollout", 80,
                        "kubectl set image statefulset/rk-server-rk-user rk-user=registry.example.com/rk-web/rk-user:20260618.1 -n shetuanguanlixitong\n"
                                + "deploy failed: rollout timeout\n"));
        when(jdbcTemplate.queryForList(contains("FROM ops_service_registry"), eq("rk-user")))
                .thenReturn(List.of(serviceRow()));
        when(jdbcTemplate.queryForList(contains("FROM ops_release_version"), eq(11L)))
                .thenReturn(List.of(Map.of(
                        "id", 11L,
                        "service_code", "rk-user",
                        "version_tag", "20260618.1",
                        "registry_image", "registry.example.com/rk-web/rk-user:20260618.1",
                        "source_image", "registry.example.com/rk-web/rk-user:old",
                        "source_type", "running-image",
                        "status", "success",
                        "create_time", "2026-06-18 16:00:00"
                )));
        when(jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class))
                .thenReturn(300L, 400L);
        AdminReleaseDeployDTO dto = new AdminReleaseDeployDTO();
        dto.setVersionId(11L);
        dto.setPreviousImage("registry.example.com/rk-web/rk-user:old");
        dto.setConfirmText("rk-user");
        dto.setDryRun(false);

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> service.deployServiceVersion("rk-user", dto));

        assertTrue(error.getMessage().contains("taskId=300"));
        verify(jdbcTemplate).update(
                contains("INSERT INTO ops_release_task"),
                eq("deploy-version"),
                eq("rk-user"),
                eq(11L),
                eq("k3s"),
                eq("failed"),
                eq("k3s-rollout"),
                eq(80),
                any(),
                eq(9L)
        );
        verify(jdbcTemplate).update(
                contains("INSERT INTO ops_release_deployment"),
                eq("rk-user"),
                eq(11L),
                eq("k3s"),
                eq("shetuanguanlixitong"),
                eq("StatefulSet"),
                eq("rk-server-rk-user"),
                eq("rk-user"),
                eq("registry.example.com/rk-web/rk-user:old"),
                eq("registry.example.com/rk-web/rk-user:20260618.1"),
                eq("failed"),
                any(),
                eq(9L)
        );
    }

    @Test
    void deployServiceVersion_shouldRejectUnsupportedRuntimeBeforeExecuting() {
        TenantContext.setTenantId(1L);
        AdminReleaseDeployDTO dto = new AdminReleaseDeployDTO();
        dto.setRuntimeMode("nomad");
        dto.setTargetImage("registry.example.com/rk-web/rk-user:new");
        dto.setConfirmText("rk-user");

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> service.deployServiceVersion("rk-user", dto));

        assertTrue(error.getMessage().contains("runtime"));
        verifyNoInteractions(k3sDeploymentExecutor, composeDeploymentExecutor);
    }

    @Test
    void buildUpdateManifest_shouldGenerateDeclarativeActionsFromSuccessfulReleaseVersions() {
        TenantContext.setTenantId(1L);
        UserContext.setUser(9L);
        when(jdbcTemplate.queryForList(contains("FROM ops_release_version WHERE is_deleted = 0 AND status = 'success'")))
                .thenReturn(List.of(Map.of(
                        "id", 11L,
                        "service_code", "rk-user",
                        "version_tag", "20260618.1",
                        "registry_image", "registry.example.com/rk-web/rk-user:20260618.1",
                        "source_image", "registry.example.com/rk-web/rk-user:old",
                        "source_type", "running-image",
                        "status", "success",
                        "create_time", "2026-06-18 16:00:00"
                )));
        when(jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class))
                .thenReturn(501L);
        AdminReleaseUpdateManifestDTO dto = new AdminReleaseUpdateManifestDTO();
        dto.setUpdateVersion("20260618.4");
        dto.setChannel("stable");
        dto.setRuntimeMode("k3s");
        dto.setCommitId("abc1234");

        var result = service.buildUpdateManifest(dto);

        assertEquals("20260618.4", result.getUpdateVersion());
        assertEquals("stable", result.getChannel());
        assertEquals("k3s", result.getRuntimeMode());
        assertEquals(1, result.getActionCount());
        assertTrue(result.getManifestJson().contains("\"type\":\"image-rollout\""));
        assertTrue(result.getManifestJson().contains("\"serviceCode\":\"rk-user\""));
        assertFalse(result.getManifestJson().contains("bash"));
        assertFalse(result.getManifestJson().contains("powershell"));
        verify(jdbcTemplate).update(
                contains("INSERT INTO ops_release_update_package"),
                eq("20260618.4"),
                eq("stable"),
                eq("k3s"),
                any(),
                eq("draft"),
                eq(9L)
        );
    }

    @Test
    void applyUpdateManifest_shouldRejectArbitraryShellActions() {
        TenantContext.setTenantId(1L);
        AdminReleaseApplyUpdateDTO dto = new AdminReleaseApplyUpdateDTO();
        dto.setDryRun(true);
        dto.setRuntimeMode("k3s");
        dto.setManifestJson("{\"schemaVersion\":\"rk-update-manifest/v1\",\"updateVersion\":\"20260618.bad\",\"channel\":\"stable\",\"runtimeMode\":\"k3s\",\"actions\":[{\"type\":\"shell\",\"command\":\"bash -lc rm -rf /\"}]}");

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> service.applyUpdateManifest(dto));

        assertTrue(error.getMessage().contains("forbidden") || error.getMessage().contains("not allowed"));
        verify(jdbcTemplate, never()).update(
                contains("INSERT INTO ops_release_task"),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void applyUpdateManifest_shouldRecordDryRunTaskWithoutExecutingActions() {
        TenantContext.setTenantId(1L);
        UserContext.setUser(9L);
        when(jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class))
                .thenReturn(601L);
        AdminReleaseApplyUpdateDTO dto = new AdminReleaseApplyUpdateDTO();
        dto.setDryRun(true);
        dto.setRuntimeMode("docker-compose");
        dto.setConfirmText("APPLY UPDATE");
        dto.setManifestJson("{\"schemaVersion\":\"rk-update-manifest/v1\",\"updateVersion\":\"20260618.5\",\"channel\":\"stable\",\"runtimeMode\":\"docker-compose\",\"actions\":[{\"type\":\"sql-migrate\",\"name\":\"menu seed\",\"path\":\"sql/20260618_menu.sql\"},{\"type\":\"nacos-import\",\"dataId\":\"rk-user.yaml\",\"group\":\"DEFAULT_GROUP\",\"path\":\"nacos/rk-user.yaml\"},{\"type\":\"minio-sync\",\"bucket\":\"rk-public\",\"prefix\":\"release/20260618/\"},{\"type\":\"jenkins-build\",\"jobName\":\"rk-web-cloud-master-new\",\"serviceCode\":\"rk-user\"}]}");

        var result = service.applyUpdateManifest(dto);

        assertEquals(601L, result.getTaskId());
        assertEquals("apply-update", result.getTaskType());
        assertEquals("docker-compose", result.getRuntimeMode());
        assertEquals("success", result.getStatus());
        assertTrue(result.getLogs().contains("dry-run update apply"));
        assertTrue(result.getLogs().contains("sql-migrate"));
        assertTrue(result.getLogs().contains("nacos-import"));
        assertTrue(result.getLogs().contains("minio-sync"));
        assertTrue(result.getLogs().contains("jenkins-build"));
        verify(jdbcTemplate).update(
                contains("INSERT INTO ops_release_task"),
                eq("apply-update"),
                eq("all"),
                eq(null),
                eq("docker-compose"),
                eq("success"),
                eq("dry-run update apply"),
                eq(100),
                any(),
                eq(9L)
        );
    }

    private Map<String, Object> serviceRow() {
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
        row.put("image_name", "registry.example.com/rk-web/rk-user:old");
        row.put("namespace_name", "shetuanguanlixitong");
        row.put("workload_type", "StatefulSet");
        row.put("workload_name", "rk-server-rk-user");
        row.put("container_name", "rk-user");
        row.put("health_check_path", "/actuator/health");
        row.put("resource_limits", "512Mi");
        row.put("enabled", Boolean.TRUE);
        row.put("sort_order", 20);
        return row;
    }
}
