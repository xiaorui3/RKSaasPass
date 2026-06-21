package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.common.utils.TenantContext;
import com.tianji.common.utils.UserContext;
import com.tianji.user.domain.dto.AdminMobileReleaseConfigDTO;
import com.tianji.user.domain.dto.AdminMobileReleaseRollbackDTO;
import com.tianji.user.domain.po.SystemConfig;
import com.tianji.user.domain.vo.MobileReleaseConfigVO;
import com.tianji.user.domain.vo.MobileReleaseHistoryVO;
import com.tianji.user.mapper.SystemConfigMapper;
import com.tianji.user.service.adminops.AdminMobileReleaseArtifactStorageService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MobileReleaseServiceImplTest {

    @Mock
    private SystemConfigMapper systemConfigMapper;

    private MobileReleaseServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SystemConfig.class);
        service = new MobileReleaseServiceImpl(systemConfigMapper, new ObjectMapper(), null);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        UserContext.removeUser();
    }

    @Test
    void getLatestRelease_shouldReturnNoUpdateWhenConfigMissing() {
        when(systemConfigMapper.selectOne(any())).thenReturn(null);

        MobileReleaseConfigVO result = service.getLatestRelease(1L, 1L, 1);

        assertFalse(result.isEnabled());
        assertFalse(result.isHasUpdate());
        assertEquals("绉诲姩绔崌绾ф湭閰嶇疆", result.getMessage());
    }

    @Test
    void saveAdminConfig_shouldInsertSystemConfigWhenMissing() {
        when(systemConfigMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            SystemConfig config = invocation.getArgument(0);
            config.setId(99L);
            return 1;
        }).when(systemConfigMapper).insert(any(SystemConfig.class));

        AdminMobileReleaseConfigDTO dto = releaseDto();
        MobileReleaseConfigVO result = service.saveAdminConfig(dto);

        assertEquals(99L, result.getConfigId());
        assertEquals("1.2.0", result.getVersionName());
        assertTrue(result.isEnabled());

        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(systemConfigMapper).insert(captor.capture());
        SystemConfig saved = captor.getValue();
        assertEquals(1L, saved.getTenantId());
        assertEquals("mobile.release.config", saved.getConfigKey());
        assertTrue(saved.getConfigValue().contains("\"versionCode\":12"));
        assertTrue(saved.getConfigValue().contains("\"targetMode\":\"all\""));
    }

    @Test
    void getLatestRelease_shouldOfferUpdateWhenVersionCodeIsOlder() throws Exception {
        when(systemConfigMapper.selectOne(any())).thenReturn(config(releaseDto()));

        MobileReleaseConfigVO result = service.getLatestRelease(1L, 1L, 1);

        assertTrue(result.isEnabled());
        assertTrue(result.isHasUpdate());
        assertEquals("1.2.0", result.getVersionName());
        assertEquals(12, result.getVersionCode());
        assertEquals("https://example.com/minio-files/rk-user/mobile-apk/app.apk", result.getDownloadUrl());
    }

    @Test
    void getLatestRelease_shouldRewriteLegacyMinioDownloadUrlToPublicDomain() throws Exception {
        AdminMobileReleaseConfigDTO dto = releaseDto();
        dto.setDownloadUrl("http://203.0.113.10:30001/minio-files/rk-user/mobile-apk/app.apk");
        when(systemConfigMapper.selectOne(any())).thenReturn(config(dto));

        MobileReleaseConfigVO result = service.getLatestRelease(1L, 1L, 1);

        assertEquals("https://example.com/minio-files/rk-user/mobile-apk/app.apk", result.getDownloadUrl());
    }

    @Test
    void getAdminConfig_shouldRewriteHistoryDownloadUrlsToPublicDomain() throws Exception {
        AdminMobileReleaseConfigDTO dto = releaseDto();
        MobileReleaseHistoryVO history = new MobileReleaseHistoryVO();
        history.setDownloadUrl("http://203.0.113.10:30001/minio-files/rk-user/mobile-apk/history.apk");
        history.setApkPath("rk-user/mobile-apk/history.apk");
        dto.setHistory(List.of(history));
        when(systemConfigMapper.selectOne(any())).thenReturn(config(dto));

        MobileReleaseConfigVO result = service.getAdminConfig();

        assertEquals("https://example.com/minio-files/rk-user/mobile-apk/history.apk",
                result.getHistory().get(0).getDownloadUrl());
    }

    @Test
    void getLatestRelease_shouldForceUpdateWhenBelowMinimumSupportedVersion() throws Exception {
        AdminMobileReleaseConfigDTO dto = releaseDto();
        dto.setForceUpgrade(false);
        dto.setMinSupportedVersionCode(8);
        when(systemConfigMapper.selectOne(any())).thenReturn(config(dto));

        MobileReleaseConfigVO result = service.getLatestRelease(1L, 1L, 7);

        assertTrue(result.isHasUpdate());
        assertTrue(result.isForceUpgrade());
    }

    @Test
    void getLatestRelease_shouldNotForceUpdateWhenCurrentVersionIsLatest() throws Exception {
        AdminMobileReleaseConfigDTO dto = releaseDto();
        dto.setForceUpgrade(true);
        dto.setVersionCode(12);
        dto.setMinSupportedVersionCode(12);
        when(systemConfigMapper.selectOne(any())).thenReturn(config(dto));

        MobileReleaseConfigVO result = service.getLatestRelease(1L, 1L, 12);

        assertFalse(result.isHasUpdate());
        assertFalse(result.isForceUpgrade());
    }

    @Test
    void getLatestRelease_shouldRespectTenantTargeting() throws Exception {
        AdminMobileReleaseConfigDTO dto = releaseDto();
        dto.setTargetMode("tenant");
        dto.setTenantIds(List.of(2L, 30L));
        when(systemConfigMapper.selectOne(any())).thenReturn(config(dto));

        MobileReleaseConfigVO hidden = service.getLatestRelease(1L, 1L, 1);
        MobileReleaseConfigVO visible = service.getLatestRelease(30L, 1L, 1);

        assertFalse(hidden.isHasUpdate());
        assertEquals("褰撳墠绉熸埛涓嶅湪鍗囩骇鑼冨洿鍐?, hidden.getMessage());
        assertTrue(visible.isHasUpdate());
    }

    @Test
    void getLatestRelease_shouldUseTenantContextWhenTenantParamMissing() throws Exception {
        TenantContext.setTenantId(30L);
        AdminMobileReleaseConfigDTO dto = releaseDto();
        dto.setTargetMode("tenant");
        dto.setTenantIds(List.of(2L, 30L));
        when(systemConfigMapper.selectOne(any())).thenReturn(config(dto));

        MobileReleaseConfigVO result = service.getLatestRelease(null, 1L, 1);

        assertTrue(result.isHasUpdate());
        assertEquals("tenant", result.getTargetMode());
        assertEquals(List.of(2L, 30L), result.getTenantIds());
    }

    @Test
    void getLatestRelease_shouldLoadGlobalConfigWhileMatchingRequestTenant() throws Exception {
        TenantContext.setTenantId(30L);
        AdminMobileReleaseConfigDTO dto = releaseDto();
        dto.setTargetMode("tenant");
        dto.setTenantIds(List.of(30L));
        when(systemConfigMapper.selectOne(any())).thenAnswer(invocation -> {
            assertEquals(1L, TenantContext.getTenantId());
            return config(dto);
        });

        MobileReleaseConfigVO result = service.getLatestRelease(null, 1L, 1);

        assertEquals(30L, TenantContext.getTenantId());
        assertTrue(result.isHasUpdate());
    }

    @Test
    void getLatestRelease_shouldLoadGlobalConfigWithSuperAdminScopeWhileMatchingRequestTenant() throws Exception {
        TenantContext.setTenantId(30L);
        TenantContext.setSuperAdmin(false);
        AdminMobileReleaseConfigDTO dto = releaseDto();
        dto.setTargetMode("tenant");
        dto.setTenantIds(List.of(30L));
        when(systemConfigMapper.selectOne(any())).thenAnswer(invocation -> {
            assertEquals(1L, TenantContext.getTenantId());
            assertTrue(TenantContext.isSuperAdmin());
            return config(dto);
        });

        MobileReleaseConfigVO result = service.getLatestRelease(null, 1L, 1);

        assertEquals(30L, TenantContext.getTenantId());
        assertFalse(TenantContext.isSuperAdmin());
        assertTrue(result.isHasUpdate());
    }

    @Test
    void getLatestRelease_shouldRespectRoleTargeting() throws Exception {
        AdminMobileReleaseConfigDTO dto = releaseDto();
        dto.setTargetMode("role");
        dto.setRoleIds(List.of(2L, 3L));
        when(systemConfigMapper.selectOne(any())).thenReturn(config(dto));

        MobileReleaseConfigVO hidden = service.getLatestRelease(1L, 1L, 1);
        MobileReleaseConfigVO visible = service.getLatestRelease(1L, 3L, 1);

        assertFalse(hidden.isHasUpdate());
        assertEquals("褰撳墠瑙掕壊涓嶅湪鍗囩骇鑼冨洿鍐?, hidden.getMessage());
        assertTrue(visible.isHasUpdate());
    }

    @Test
    void saveAdminConfig_shouldAppendManualUploadHistory() {
        when(systemConfigMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            SystemConfig config = invocation.getArgument(0);
            config.setId(99L);
            return 1;
        }).when(systemConfigMapper).insert(any(SystemConfig.class));

        AdminMobileReleaseConfigDTO dto = releaseDto();
        dto.setReleaseSource("manual-upload");
        MobileReleaseConfigVO result = service.saveAdminConfig(dto);

        assertEquals(1, result.getHistory().size());
        MobileReleaseHistoryVO row = result.getHistory().get(0);
        assertEquals("manual-upload", row.getReleaseSource());
        assertEquals("1.2.0", row.getVersionName());
        assertEquals(12, row.getVersionCode());
        assertEquals("active", row.getStatus());
    }

    @Test
    void saveAdminConfig_shouldKeepOnlyLatestTenHistoryEntriesAndDeletePrunedApks() throws Exception {
        AdminMobileReleaseArtifactStorageService artifactStorage = mock(AdminMobileReleaseArtifactStorageService.class);
        inject("artifactStorageService", artifactStorage);

        AdminMobileReleaseConfigDTO existing = releaseDto();
        existing.setApkPath("rk-user/mobile-apk/current.apk");
        existing.setHistory(historyRows(10));
        when(systemConfigMapper.selectOne(any())).thenReturn(config(existing));

        AdminMobileReleaseConfigDTO next = releaseDto();
        next.setVersionName("1.3.0");
        next.setVersionCode(13);
        next.setDownloadUrl("https://example.com/minio-files/rk-user/mobile-apk/new.apk");
        next.setApkPath("rk-user/mobile-apk/new.apk");
        next.setReleaseSource("manual-upload");

        MobileReleaseConfigVO result = service.saveAdminConfig(next);

        assertEquals(10, result.getHistory().size());
        assertEquals("rk-user/mobile-apk/new.apk", result.getHistory().get(0).getApkPath());
        assertFalse(result.getHistory().stream()
                .anyMatch(item -> "rk-user/mobile-apk/history-10.apk".equals(item.getApkPath())));
        verify(artifactStorage, times(1)).deleteApk("rk-user/mobile-apk/history-10.apk");
        verify(artifactStorage, never()).deleteApk("rk-user/mobile-apk/new.apk");
        verify(artifactStorage, never()).deleteApk("rk-user/mobile-apk/current.apk");
    }

    @Test
    void autoBuildAdminConfig_shouldNotReplaceActiveReleaseUntilBuildSucceeds() throws Exception {
        AdminMobileReleaseConfigDTO dto = releaseDto();
        dto.setVersionName("0.0.30");
        dto.setVersionCode(31);
        dto.setDownloadUrl("https://example.com/minio-files/rk-user/mobile-apk/old.apk");
        dto.setApkPath("rk-user/mobile-apk/old.apk");
        when(systemConfigMapper.selectOne(any())).thenReturn(config(dto));

        MobileReleaseConfigVO result = service.autoBuildAdminConfig();

        assertEquals("0.0.30", result.getVersionName());
        assertEquals(31, result.getVersionCode());
        assertEquals("https://example.com/minio-files/rk-user/mobile-apk/old.apk", result.getDownloadUrl());
        assertEquals("auto-build", result.getReleaseSource());
        assertEquals("queued", result.getBuildStatus());
        verify(systemConfigMapper, never()).updateById(any(SystemConfig.class));
    }

    @Test
    void withdrawAdminConfig_shouldDisableReleaseAndRecordWithdrawHistory() throws Exception {
        AdminMobileReleaseConfigDTO dto = releaseDto();
        when(systemConfigMapper.selectOne(any())).thenReturn(config(dto));

        MobileReleaseConfigVO result = service.withdrawAdminConfig();

        assertFalse(result.isEnabled());
        assertFalse(result.isHasUpdate());
        assertTrue(result.getHistory().stream().anyMatch(row -> "withdrawn".equals(row.getStatus())));
        verify(systemConfigMapper).updateById(any(SystemConfig.class));
    }

    @Test
    void rollbackAdminConfig_shouldRestoreSelectedHistoryRelease() throws Exception {
        AdminMobileReleaseConfigDTO dto = releaseDto();
        dto.setReleaseSource("manual-upload");
        SystemConfig row = config(dto);
        when(systemConfigMapper.selectOne(any())).thenReturn(row);

        MobileReleaseConfigVO saved = service.saveAdminConfig(dto);
        AdminMobileReleaseRollbackDTO rollback = new AdminMobileReleaseRollbackDTO();
        rollback.setHistoryId(saved.getHistory().get(0).getHistoryId());

        MobileReleaseConfigVO result = service.rollbackAdminConfig(rollback);

        assertTrue(result.isEnabled());
        assertEquals("rollback", result.getReleaseSource());
        assertTrue(result.getHistory().stream().anyMatch(item -> "rollback".equals(item.getReleaseSource())));
    }

    @Test
    void runMobileBuild_shouldPrepareTenantContextForWorkerThread() throws Exception {
        inject("androidBuildMode", "k8s");

        CountDownLatch observed = new CountDownLatch(1);
        AtomicReference<Long> tenantSeen = new AtomicReference<>();
        when(systemConfigMapper.selectOne(any())).thenAnswer(invocation -> {
            tenantSeen.compareAndSet(null, TenantContext.getTenantId());
            observed.countDown();
            return config(releaseDto());
        });

        Method run = MobileReleaseServiceImpl.class.getDeclaredMethod("runMobileBuild", Long.class);
        run.setAccessible(true);
        Thread worker = new Thread(() -> {
            try {
                run.invoke(service, 99L);
            } catch (Exception ignored) {
            }
        }, "mobile-build-test");
        worker.start();

        assertTrue(observed.await(5, TimeUnit.SECONDS));
        worker.join(1000L);
        assertEquals(1L, tenantSeen.get());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void sendK8sText_shouldRetryTransientKubernetesApiFailure() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse<String> first = mock(HttpResponse.class);
        HttpResponse<String> second = mock(HttpResponse.class);
        when(first.statusCode()).thenReturn(500);
        when(first.body()).thenReturn("{\"message\":\"etcdserver: leader changed\"}");
        when(second.statusCode()).thenReturn(200);
        when(second.body()).thenReturn("ok");
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(first, second);

        Method send = MobileReleaseServiceImpl.class.getDeclaredMethod(
                "sendK8sText",
                HttpClient.class,
                String.class,
                String.class,
                String.class,
                String.class,
                Object.class,
                int[].class
        );
        send.setAccessible(true);

        String result = (String) send.invoke(service, client, "https://kubernetes.default", "token", "GET", "/api", null, new int[]{200});

        assertEquals("ok", result);
        verify(client, org.mockito.Mockito.times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void buildK8sMobileBuildScript_shouldWrapArtifactBase64InsteadOfOneHugeLine() throws Exception {
        Method scriptMethod = MobileReleaseServiceImpl.class.getDeclaredMethod("buildK8sMobileBuildScript");
        scriptMethod.setAccessible(true);

        String script = (String) scriptMethod.invoke(service);

        assertTrue(script.contains("base64 \"${APK}\" | fold -w 76"));
        assertFalse(script.contains("base64 \"${APK}\" | tr -d '\\n'"));
    }

    @Test
    void parseK8sBuildArtifact_shouldDecodeWrappedBase64Stream() throws Exception {
        byte[] apkBytes = "fake-apk-bytes".getBytes(StandardCharsets.UTF_8);
        String b64 = Base64.getEncoder().encodeToString(apkBytes);
        String wrappedB64 = b64.substring(0, 8) + "\n" + b64.substring(8);
        String logs = String.join("\n",
                "__RK_GIT_COMMIT__=bc0ce781f763",
                "__RK_GIT_RANGE__=dcd14ff6462d..bc0ce781f763",
                "__RK_GIT_NOTES_BEGIN__",
                "bc0ce78 淇绉诲姩绔姩鎬佺瓫閫夊拰璇勮鐢ㄦ埛璺宠浆",
                "__RK_GIT_NOTES_END__",
                "__RK_APK_FILE__=rk-club-debug.apk",
                "__RK_APK_SIZE__=" + apkBytes.length,
                "__RK_APK_B64_BEGIN__",
                wrappedB64,
                "__RK_APK_B64_END__"
        );
        Method parse = MobileReleaseServiceImpl.class.getDeclaredMethod("parseK8sBuildArtifact", java.io.InputStream.class);
        parse.setAccessible(true);

        Object artifact = parse.invoke(service, new ByteArrayInputStream(logs.getBytes(StandardCharsets.UTF_8)));

        assertEquals("rk-club-debug.apk", artifactField(artifact, "fileName"));
        assertEquals("bc0ce781f763", artifactField(artifact, "gitCommit"));
        assertEquals("dcd14ff6462d..bc0ce781f763", artifactField(artifact, "gitRange"));
        assertEquals("bc0ce78 淇绉诲姩绔姩鎬佺瓫閫夊拰璇勮鐢ㄦ埛璺宠浆", artifactField(artifact, "releaseNotes"));
        assertArrayEquals(apkBytes, (byte[]) artifactField(artifact, "bytes"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildK8sBuildPodManifest_shouldUseConfiguredGiteeCredentialsWithoutEmbeddingThemInCloneUrl() throws Exception {
        inject("androidBuildGitUrl", "https://github.com/example/rk-web");
        inject("jenkinsGitGiteeUsername", "gitee-user");
        inject("jenkinsGitGiteePassword", "secret-token");

        Method scriptMethod = MobileReleaseServiceImpl.class.getDeclaredMethod("buildK8sMobileBuildScript");
        scriptMethod.setAccessible(true);
        String script = (String) scriptMethod.invoke(service);

        Method manifestMethod = MobileReleaseServiceImpl.class.getDeclaredMethod(
                "buildK8sBuildPodManifest",
                String.class,
                String.class,
                String.class,
                AdminMobileReleaseConfigDTO.class,
                String.class
        );
        manifestMethod.setAccessible(true);
        Map<String, Object> manifest = (Map<String, Object>) manifestMethod.invoke(
                service,
                "rk-mobile-build-1",
                "shetuanguanlixitong",
                script,
                releaseDto(),
                null
        );

        List<Map<String, String>> env = extractContainerEnv(manifest);
        assertEquals("https://github.com/example/rk-web", findEnv(env, "RK_GIT_URL"));
        assertEquals("gitee-user", findEnv(env, "RK_GIT_USERNAME"));
        assertEquals("secret-token", findEnv(env, "RK_GIT_PASSWORD"));
        assertTrue(script.contains(".netrc"));
        assertTrue(script.contains("git credentials: configured"));
        assertTrue(script.contains("git clone --depth 100 --branch \"${RK_GIT_BRANCH}\" \"${RK_GIT_URL}\" \"${REPO}\""));
        assertFalse(script.contains("secret-token"));
        assertFalse(findEnv(env, "RK_GIT_URL").contains("secret-token"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildK8sBuildPodManifest_shouldNotApplyGiteeCredentialsToLocalGitUrl() throws Exception {
        inject("androidBuildGitUrl", "https://github.com/example/rk-web.git");
        inject("jenkinsGitGiteeUsername", "gitee-user");
        inject("jenkinsGitGiteePassword", "secret-token");

        Method scriptMethod = MobileReleaseServiceImpl.class.getDeclaredMethod("buildK8sMobileBuildScript");
        scriptMethod.setAccessible(true);
        String script = (String) scriptMethod.invoke(service);

        Method manifestMethod = MobileReleaseServiceImpl.class.getDeclaredMethod(
                "buildK8sBuildPodManifest",
                String.class,
                String.class,
                String.class,
                AdminMobileReleaseConfigDTO.class,
                String.class
        );
        manifestMethod.setAccessible(true);
        Map<String, Object> manifest = (Map<String, Object>) manifestMethod.invoke(
                service,
                "rk-mobile-build-2",
                "shetuanguanlixitong",
                script,
                releaseDto(),
                null
        );

        List<Map<String, String>> env = extractContainerEnv(manifest);
        assertEquals("", findEnv(env, "RK_GIT_USERNAME"));
        assertEquals("", findEnv(env, "RK_GIT_PASSWORD"));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> extractContainerEnv(Map<String, Object> manifest) {
        Map<String, Object> spec = (Map<String, Object>) manifest.get("spec");
        List<Map<String, Object>> containers = (List<Map<String, Object>>) spec.get("containers");
        return (List<Map<String, String>>) (Object) containers.get(0).get("env");
    }

    private String findEnv(List<Map<String, String>> env, String name) {
        return env.stream()
                .filter(item -> name.equals(item.get("name")))
                .map(item -> item.get("value"))
                .findFirst()
                .orElse(null);
    }

    private Object artifactField(Object artifact, String fieldName) throws Exception {
        Field field = artifact.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(artifact);
    }

    private void inject(String fieldName, Object value) throws Exception {
        Field field = MobileReleaseServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(service, value);
    }

    private AdminMobileReleaseConfigDTO releaseDto() {
        AdminMobileReleaseConfigDTO dto = new AdminMobileReleaseConfigDTO();
        dto.setEnabled(true);
        dto.setVersionName("1.2.0");
        dto.setVersionCode(12);
        dto.setMinSupportedVersionCode(5);
        dto.setForceUpgrade(false);
        dto.setDownloadUrl("https://example.com/rk-user/mobile-apk/app.apk");
        dto.setApkPath("rk-user/mobile-apk/app.apk");
        dto.setFileSize(786432L);
        dto.setReleaseNotes("淇鐧诲綍鍜岄€氱煡浣撻獙");
        dto.setTargetMode("all");
        return dto;
    }

    private List<MobileReleaseHistoryVO> historyRows(int count) {
        List<MobileReleaseHistoryVO> rows = new ArrayList<>();
        for (int index = 1; index <= count; index += 1) {
            MobileReleaseHistoryVO item = new MobileReleaseHistoryVO();
            item.setHistoryId("history-" + index);
            item.setReleaseSource("manual-upload");
            item.setStatus("active");
            item.setVersionName("1.2." + index);
            item.setVersionCode(index);
            item.setDownloadUrl("https://example.com/minio-files/rk-user/mobile-apk/history-" + index + ".apk");
            item.setApkPath("rk-user/mobile-apk/history-" + index + ".apk");
            item.setTargetMode("all");
            rows.add(item);
        }
        return rows;
    }

    private SystemConfig config(AdminMobileReleaseConfigDTO dto) throws Exception {
        SystemConfig config = new SystemConfig();
        config.setId(12L);
        config.setTenantId(1L);
        config.setConfigKey("mobile.release.config");
        config.setConfigValue(new ObjectMapper().writeValueAsString(dto));
        config.setIsEnabled(true);
        return config;
    }
}
