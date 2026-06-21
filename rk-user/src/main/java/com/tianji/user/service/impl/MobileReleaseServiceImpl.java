package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.common.autoconfigure.media.MediaPathHelper;
import com.tianji.common.utils.TenantContext;
import com.tianji.common.utils.UserContext;
import com.tianji.user.domain.dto.AdminMobileReleaseConfigDTO;
import com.tianji.user.domain.dto.AdminMobileReleaseRollbackDTO;
import com.tianji.user.domain.po.SystemConfig;
import com.tianji.user.domain.vo.MobileReleaseBuildLogVO;
import com.tianji.user.domain.vo.MobileReleaseBuildRecordVO;
import com.tianji.user.domain.vo.MobileReleaseConfigVO;
import com.tianji.user.domain.vo.MobileReleaseHistoryVO;
import com.tianji.user.mapper.SystemConfigMapper;
import com.tianji.user.service.adminops.AdminMobileReleaseArtifactStorageService;
import com.tianji.user.service.IMobileReleaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

@Slf4j
@Service
@RequiredArgsConstructor
public class MobileReleaseServiceImpl implements IMobileReleaseService {

    public static final long CONFIG_TENANT_ID = 1L;
    public static final String CONFIG_KEY = "mobile.release.config";
    private static final int MAX_HISTORY_SIZE = 10;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SystemConfigMapper systemConfigMapper;
    private final ObjectMapper objectMapper;
    private final MediaPathHelper mediaPathHelper;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private AdminMobileReleaseArtifactStorageService artifactStorageService;

    @Value("${rk.mobile.release.android-project:android-client/rk-club-android}")
    private String androidProjectPath;

    @Value("${rk.mobile.release.build-command:}")
    private String androidBuildCommand;

    @Value("${rk.mobile.release.build-timeout-minutes:20}")
    private Integer androidBuildTimeoutMinutes;

    @Value("${rk.mobile.release.build-mode:auto}")
    private String androidBuildMode;

    @Value("${rk.mobile.release.k8s.namespace:shetuanguanlixitong}")
    private String androidBuildNamespace;

    @Value("${rk.mobile.release.k8s.builder-image:m.daocloud.io/docker.io/library/maven:3.9.9-eclipse-temurin-17}")
    private String androidBuildImage;

    @Value("${rk.mobile.release.k8s.git-url:https://github.com/example/rk-web}")
    private String androidBuildGitUrl;

    @Value("${rk.mobile.release.k8s.git-branch:cloud-master-new}")
    private String androidBuildGitBranch;

    @Value("${rk.mobile.release.k8s.git-username:${rk.ops.jenkins.git.gitee-username:}}")
    private String jenkinsGitGiteeUsername;

    @Value("${rk.mobile.release.k8s.git-password:${rk.ops.jenkins.git.gitee-password:}}")
    private String jenkinsGitGiteePassword;

    @Value("${rk.mobile.release.android-tools-url:http://rk-minio:9000/rk-user/mobile-sdk/commandlinetools-linux-11076708_latest.zip}")
    private String androidToolsUrl;

    @Value("${rk.mobile.release.public-base-url:${rk.web.public-base-url:https://example.com}}")
    private String mobileReleasePublicBaseUrl;

    @Value("${rk.mobile.release.public-file-prefix:/minio-files/}")
    private String mobileReleasePublicFilePrefix;

    @Value("${rk.mobile.release.k8s.image-pull-secret:}")
    private String androidBuildImagePullSecret;

    @Value("${rk.mobile.release.k8s.api-url:}")
    private String k8sApiUrl;

    @Value("${rk.mobile.release.k8s.token-path:/var/run/secrets/kubernetes.io/serviceaccount/token}")
    private String k8sTokenPath;

    @Value("${rk.mobile.release.k8s.ca-path:/var/run/secrets/kubernetes.io/serviceaccount/ca.crt}")
    private String k8sCaPath;

    @Value("${rk.mobile.release.k8s.cleanup-pod:true}")
    private Boolean cleanupK8sBuildPod;

    private final ExecutorService buildExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "rk-mobile-release-builder");
        thread.setDaemon(true);
        return thread;
    });
    private final ConcurrentHashMap<Long, Boolean> runningBuilds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AdminMobileReleaseConfigDTO> pendingBuildConfigs = new ConcurrentHashMap<>();

    @Override
    public MobileReleaseConfigVO getAdminConfig() {
        SystemConfig row = loadConfigRow();
        if (row == null) {
            MobileReleaseConfigVO vo = defaultConfig();
            vo.setMessage("绉诲姩绔崌绾ф湭閰嶇疆");
            return vo;
        }
        MobileReleaseConfigVO vo = toVO(row, readConfig(row));
        vo.setMessage(Boolean.TRUE.equals(row.getIsEnabled()) ? "绉诲姩绔崌绾ч厤缃凡鍚敤" : "绉诲姩绔崌绾ч厤缃凡鍋滅敤");
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MobileReleaseConfigVO saveAdminConfig(AdminMobileReleaseConfigDTO dto) {
        AdminMobileReleaseConfigDTO normalized = normalize(dto);
        SystemConfig existing = loadConfigRow();
        AdminMobileReleaseConfigDTO current = existing == null ? normalize(null) : readConfig(existing);
        normalized.setHistory(copyHistory(current.getHistory()));
        normalized.setReleaseSource(trimToDefault(normalized.getReleaseSource(), "manual-upload"));
        normalized.setBuildStatus(trimToDefault(normalized.getBuildStatus(), "uploaded"));
        normalized.setCreatedAt(trimToDefault(normalized.getCreatedAt(), now()));
        appendHistory(normalized, normalized.getReleaseSource(), "active");
        return persistConfig(existing, normalized, true);
    }

    @Override
    public List<MobileReleaseHistoryVO> getAdminHistory() {
        SystemConfig row = loadConfigRow();
        if (row == null) {
            return new ArrayList<>();
        }
        return copyHistory(readConfig(row).getHistory());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MobileReleaseConfigVO withdrawAdminConfig() {
        SystemConfig existing = loadConfigRow();
        AdminMobileReleaseConfigDTO config = existing == null ? normalize(null) : readConfig(existing);
        config.setEnabled(false);
        config.setReleaseSource("withdraw");
        config.setBuildStatus("withdrawn");
        config.setBuildLog("release withdrawn");
        config.setCreatedAt(now());
        appendHistory(config, "withdraw", "withdrawn");
        MobileReleaseConfigVO vo = persistConfig(existing, config, false);
        vo.setHasUpdate(false);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MobileReleaseConfigVO rollbackAdminConfig(AdminMobileReleaseRollbackDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getHistoryId())) {
            throw new IllegalArgumentException("historyId is required");
        }
        SystemConfig existing = loadConfigRow();
        if (existing == null) {
            throw new IllegalArgumentException("mobile release config not found");
        }
        AdminMobileReleaseConfigDTO config = readConfig(existing);
        MobileReleaseHistoryVO target = config.getHistory().stream()
                .filter(item -> Objects.equals(item.getHistoryId(), dto.getHistoryId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("release history not found"));
        applyHistoryToConfig(target, config);
        config.setEnabled(true);
        config.setReleaseSource("rollback");
        config.setBuildStatus("rolled-back");
        config.setBuildLog("rollback to " + target.getVersionName() + "(" + target.getVersionCode() + ")");
        config.setCreatedAt(now());
        appendHistory(config, "rollback", "active");
        return persistConfig(existing, config, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MobileReleaseConfigVO autoBuildAdminConfig() {
        SystemConfig existing = loadConfigRow();
        AdminMobileReleaseConfigDTO activeConfig = existing == null ? normalize(null) : readConfig(existing);
        AdminMobileReleaseConfigDTO buildConfig = prepareQueuedBuildConfig(activeConfig);
        Long buildId = createMobileBuildRecord(buildConfig);
        buildConfig.setBuildId(buildId);
        if (buildId != null && buildId > 0) {
            pendingBuildConfigs.put(buildId, copyConfig(buildConfig));
        }
        MobileReleaseConfigVO vo = existing == null ? defaultConfig() : toVO(existing, activeConfig);
        vo.setReleaseSource("auto-build");
        vo.setBuildStatus("queued");
        vo.setBuildLog("auto build queued");
        vo.setBuildId(buildId);
        vo.setMessage("auto build queued; active APK release will change after upload succeeds");
        scheduleMobileBuild(buildId);
        return vo;
    }

    @Override
    public List<MobileReleaseBuildRecordVO> getAdminBuildHistory() {
        if (jdbcTemplate == null) {
            return new ArrayList<>();
        }
        return jdbcTemplate.queryForList(
                        "SELECT * FROM mobile_release_build_record WHERE is_deleted = 0 ORDER BY create_time DESC, id DESC LIMIT 50"
                ).stream()
                .map(this::mapBuildRecord)
                .collect(Collectors.toList());
    }

    @Override
    public MobileReleaseBuildRecordVO getLatestBuildRecord() {
        List<MobileReleaseBuildRecordVO> rows = getAdminBuildHistory();
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Override
    public List<MobileReleaseBuildLogVO> getBuildLogs(Long buildId, Integer afterLineNo) {
        if (jdbcTemplate == null || buildId == null || buildId <= 0) {
            return new ArrayList<>();
        }
        int lineNo = afterLineNo == null ? 0 : Math.max(0, afterLineNo);
        return jdbcTemplate.queryForList(
                        "SELECT id, build_id, line_no, log_type, content, created_at FROM mobile_release_build_log WHERE build_id = ? AND line_no > ? ORDER BY line_no ASC, id ASC LIMIT 500",
                        buildId,
                        lineNo
                ).stream()
                .map(this::mapBuildLog)
                .collect(Collectors.toList());
    }

    @Override
    public MobileReleaseConfigVO getLatestRelease(Long tenantId, Long roleId, Integer currentVersionCode) {
        SystemConfig row = loadConfigRow();
        if (row == null) {
            MobileReleaseConfigVO vo = defaultConfig();
            vo.setMessage("绉诲姩绔崌绾ф湭閰嶇疆");
            return vo;
        }
        AdminMobileReleaseConfigDTO config = readConfig(row);
        MobileReleaseConfigVO vo = toVO(row, config);
        if (!vo.isEnabled()) {
            vo.setHasUpdate(false);
            vo.setMessage("绉诲姩绔崌绾у凡鍏抽棴");
            return vo;
        }
        Long resolvedTenantId = tenantId == null ? TenantContext.getTenantId() : tenantId;
        if (!isTargetMatched(config, resolvedTenantId, roleId)) {
            vo.setHasUpdate(false);
            vo.setMessage(targetMismatchMessage(config));
            return vo;
        }
        String downloadUrl = resolveDownloadUrl(config);
        if (!StringUtils.hasText(downloadUrl)) {
            vo.setHasUpdate(false);
            vo.setMessage("绉诲姩绔崌绾у寘鏈笂浼?);
            return vo;
        }
        vo.setDownloadUrl(downloadUrl);
        int latestCode = safeInt(config.getVersionCode());
        int currentCode = currentVersionCode == null ? 0 : Math.max(0, currentVersionCode);
        int minSupportedCode = safeInt(config.getMinSupportedVersionCode());
        boolean belowMinimum = minSupportedCode > 0 && currentCode < minSupportedCode;
        boolean newer = latestCode > currentCode;
        vo.setHasUpdate(newer || belowMinimum);
        vo.setForceUpgrade(vo.isHasUpdate() && (Boolean.TRUE.equals(config.getForceUpgrade()) || belowMinimum));
        vo.setMessage(vo.isHasUpdate() ? "鍙戠幇鏂扮増鏈? : "褰撳墠宸叉槸鏈€鏂扮増鏈?);
        return vo;
    }

    private MobileReleaseConfigVO persistConfig(SystemConfig existing, AdminMobileReleaseConfigDTO config, boolean rowEnabled) {
        return executeInConfigTenantScope(() -> {
            AdminMobileReleaseConfigDTO normalized = normalize(config);
            String json = writeConfig(normalized);
            if (existing == null) {
                SystemConfig created = new SystemConfig();
                created.setTenantId(CONFIG_TENANT_ID);
                created.setConfigKey(CONFIG_KEY);
                created.setConfigValue(json);
                created.setDescription("绉诲姩绔?APK 鍗囩骇閰嶇疆");
                created.setIsEnabled(rowEnabled);
                created.setIsDeleted(Boolean.FALSE);
                created.setCreateTime(LocalDateTime.now());
                created.setUpdateTime(LocalDateTime.now());
                systemConfigMapper.insert(created);
                return toVO(created, normalized);
            }
            existing.setTenantId(CONFIG_TENANT_ID);
            existing.setConfigKey(CONFIG_KEY);
            existing.setConfigValue(json);
            existing.setDescription("绉诲姩绔?APK 鍗囩骇閰嶇疆");
            existing.setIsEnabled(rowEnabled);
            existing.setUpdateTime(LocalDateTime.now());
            systemConfigMapper.updateById(existing);
            return toVO(existing, normalized);
        });
    }

    private SystemConfig loadConfigRow() {
        return executeInConfigTenantScope(() -> systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getTenantId, CONFIG_TENANT_ID)
                .eq(SystemConfig::getConfigKey, CONFIG_KEY)));
    }

    private <T> T executeInConfigTenantScope(Supplier<T> supplier) {
        Long previousTenantId = TenantContext.getTenantId();
        Boolean previousSuperAdmin = TenantContext.isSuperAdmin();
        try {
            TenantContext.setTenantId(CONFIG_TENANT_ID);
            TenantContext.setSuperAdmin(true);
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

    private AdminMobileReleaseConfigDTO readConfig(SystemConfig row) {
        if (row == null || !StringUtils.hasText(row.getConfigValue())) {
            return normalize(null);
        }
        try {
            return normalize(objectMapper.readValue(row.getConfigValue(), AdminMobileReleaseConfigDTO.class));
        } catch (Exception e) {
            return normalize(null);
        }
    }

    private String writeConfig(AdminMobileReleaseConfigDTO dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (Exception e) {
            throw new IllegalArgumentException("serialize mobile release config failed", e);
        }
    }

    private AdminMobileReleaseConfigDTO normalize(AdminMobileReleaseConfigDTO source) {
        AdminMobileReleaseConfigDTO dto = source == null ? new AdminMobileReleaseConfigDTO() : source;
        dto.setEnabled(Boolean.TRUE.equals(dto.getEnabled()));
        dto.setForceUpgrade(Boolean.TRUE.equals(dto.getForceUpgrade()));
        dto.setVersionName(trimToDefault(dto.getVersionName(), "1.0.0"));
        dto.setVersionCode(Math.max(0, safeInt(dto.getVersionCode())));
        dto.setMinSupportedVersionCode(Math.max(0, safeInt(dto.getMinSupportedVersionCode())));
        dto.setDownloadUrl(trimToEmpty(dto.getDownloadUrl()));
        dto.setApkPath(trimToEmpty(dto.getApkPath()));
        dto.setReleaseNotes(trimToEmpty(dto.getReleaseNotes()));
        dto.setReleaseSource(trimToEmpty(dto.getReleaseSource()));
        dto.setBuildStatus(trimToEmpty(dto.getBuildStatus()));
        dto.setBuildLog(trimToEmpty(dto.getBuildLog()));
        dto.setBuildId(dto.getBuildId() == null ? null : Math.max(0L, dto.getBuildId()));
        dto.setGitCommit(trimToEmpty(dto.getGitCommit()));
        dto.setGitRange(trimToEmpty(dto.getGitRange()));
        dto.setCreatedAt(trimToEmpty(dto.getCreatedAt()));
        dto.setFileSize(dto.getFileSize() == null ? 0L : Math.max(0L, dto.getFileSize()));
        dto.setTargetMode(normalizeTargetMode(dto.getTargetMode()));
        dto.setTenantIds(normalizeIds(dto.getTenantIds()));
        dto.setRoleIds(normalizeIds(dto.getRoleIds()));
        dto.setHistory(copyHistory(dto.getHistory()));
        return dto;
    }

    private MobileReleaseConfigVO toVO(SystemConfig row, AdminMobileReleaseConfigDTO config) {
        MobileReleaseConfigVO vo = defaultConfig();
        vo.setConfigId(row == null ? null : row.getId());
        vo.setEnabled(config.getEnabled() != null && config.getEnabled());
        vo.setForceUpgrade(config.getForceUpgrade() != null && config.getForceUpgrade());
        vo.setVersionName(config.getVersionName());
        vo.setVersionCode(config.getVersionCode());
        vo.setMinSupportedVersionCode(config.getMinSupportedVersionCode());
        vo.setDownloadUrl(resolveDownloadUrl(config));
        vo.setApkPath(config.getApkPath());
        vo.setFileSize(config.getFileSize());
        vo.setReleaseNotes(config.getReleaseNotes());
        vo.setReleaseSource(config.getReleaseSource());
        vo.setBuildStatus(config.getBuildStatus());
        vo.setBuildLog(config.getBuildLog());
        vo.setBuildId(config.getBuildId());
        vo.setGitCommit(config.getGitCommit());
        vo.setGitRange(config.getGitRange());
        vo.setCreatedAt(config.getCreatedAt());
        vo.setTargetMode(config.getTargetMode());
        vo.setTenantIds(config.getTenantIds());
        vo.setRoleIds(config.getRoleIds());
        vo.setHistory(copyHistory(config.getHistory()));
        return vo;
    }

    private MobileReleaseConfigVO defaultConfig() {
        MobileReleaseConfigVO vo = new MobileReleaseConfigVO();
        vo.setEnabled(false);
        vo.setHasUpdate(false);
        vo.setForceUpgrade(false);
        vo.setVersionName("1.0.0");
        vo.setVersionCode(0);
        vo.setMinSupportedVersionCode(0);
        vo.setDownloadUrl("");
        vo.setApkPath("");
        vo.setFileSize(0L);
        vo.setReleaseNotes("");
        vo.setReleaseSource("");
        vo.setBuildStatus("");
        vo.setBuildLog("");
        vo.setBuildId(null);
        vo.setGitCommit("");
        vo.setGitRange("");
        vo.setCreatedAt("");
        vo.setTargetMode("all");
        vo.setTenantIds(new ArrayList<>());
        vo.setRoleIds(new ArrayList<>());
        vo.setHistory(new ArrayList<>());
        return vo;
    }

    private void appendHistory(AdminMobileReleaseConfigDTO config, String releaseSource, String status) {
        List<MobileReleaseHistoryVO> history = copyHistory(config.getHistory());
        MobileReleaseHistoryVO item = new MobileReleaseHistoryVO();
        item.setHistoryId(UUID.randomUUID().toString());
        item.setReleaseSource(trimToDefault(releaseSource, "manual-upload"));
        item.setStatus(trimToDefault(status, "active"));
        item.setBuildId(config.getBuildId());
        item.setVersionName(config.getVersionName());
        item.setVersionCode(config.getVersionCode());
        item.setMinSupportedVersionCode(config.getMinSupportedVersionCode());
        item.setForceUpgrade(config.getForceUpgrade());
        item.setDownloadUrl(config.getDownloadUrl());
        item.setApkPath(config.getApkPath());
        item.setFileSize(config.getFileSize());
        item.setReleaseNotes(config.getReleaseNotes());
        item.setGitCommit(config.getGitCommit());
        item.setGitRange(config.getGitRange());
        item.setTargetMode(config.getTargetMode());
        item.setTenantIds(new ArrayList<>(normalizeIds(config.getTenantIds())));
        item.setRoleIds(new ArrayList<>(normalizeIds(config.getRoleIds())));
        item.setBuildStatus(config.getBuildStatus());
        item.setBuildLog(config.getBuildLog());
        item.setCreatedAt(trimToDefault(config.getCreatedAt(), now()));
        history.add(0, item);
        if (history.size() > MAX_HISTORY_SIZE) {
            List<MobileReleaseHistoryVO> pruned = new ArrayList<>(history.subList(MAX_HISTORY_SIZE, history.size()));
            history = new ArrayList<>(history.subList(0, MAX_HISTORY_SIZE));
            cleanupPrunedApkArtifacts(config, history, pruned);
        }
        config.setHistory(history);
    }

    private void cleanupPrunedApkArtifacts(AdminMobileReleaseConfigDTO config,
                                           List<MobileReleaseHistoryVO> retainedHistory,
                                           List<MobileReleaseHistoryVO> prunedHistory) {
        if (artifactStorageService == null || prunedHistory == null || prunedHistory.isEmpty()) {
            return;
        }
        Set<String> retainedPaths = new LinkedHashSet<>();
        retainedPaths.add(extractMobileReleaseObjectPath(config.getApkPath()));
        retainedPaths.add(extractMobileReleaseObjectPath(config.getDownloadUrl()));
        for (MobileReleaseHistoryVO item : retainedHistory) {
            retainedPaths.add(extractMobileReleaseObjectPath(item.getApkPath()));
            retainedPaths.add(extractMobileReleaseObjectPath(item.getDownloadUrl()));
        }
        retainedPaths.removeIf(path -> !StringUtils.hasText(path));

        Set<String> pathsToDelete = prunedHistory.stream()
                .filter(Objects::nonNull)
                .flatMap(item -> List.of(
                        extractMobileReleaseObjectPath(item.getApkPath()),
                        extractMobileReleaseObjectPath(item.getDownloadUrl())
                ).stream())
                .filter(StringUtils::hasText)
                .filter(path -> !retainedPaths.contains(path))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (String path : pathsToDelete) {
            try {
                artifactStorageService.deleteApk(path);
            } catch (Exception e) {
                log.warn("delete pruned mobile apk artifact failed, path={}", path, e);
            }
        }
    }

    private List<MobileReleaseHistoryVO> copyHistory(List<MobileReleaseHistoryVO> history) {
        if (history == null) {
            return new ArrayList<>();
        }
        return history.stream()
                .filter(Objects::nonNull)
                .map(this::copyHistoryItem)
                .limit(MAX_HISTORY_SIZE)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private MobileReleaseHistoryVO copyHistoryItem(MobileReleaseHistoryVO source) {
        MobileReleaseHistoryVO item = new MobileReleaseHistoryVO();
        item.setHistoryId(trimToDefault(source.getHistoryId(), UUID.randomUUID().toString()));
        item.setReleaseSource(trimToEmpty(source.getReleaseSource()));
        item.setStatus(trimToDefault(source.getStatus(), "active"));
        item.setBuildId(source.getBuildId());
        item.setVersionName(trimToDefault(source.getVersionName(), "1.0.0"));
        item.setVersionCode(Math.max(0, safeInt(source.getVersionCode())));
        item.setMinSupportedVersionCode(Math.max(0, safeInt(source.getMinSupportedVersionCode())));
        item.setForceUpgrade(Boolean.TRUE.equals(source.getForceUpgrade()));
        item.setApkPath(trimToEmpty(source.getApkPath()));
        String downloadUrl = normalizeMobileReleaseDownloadUrl(source.getDownloadUrl());
        if (!StringUtils.hasText(downloadUrl) && StringUtils.hasText(item.getApkPath())) {
            downloadUrl = toMobileReleasePublicDownloadUrl(item.getApkPath());
        }
        item.setDownloadUrl(trimToEmpty(downloadUrl));
        item.setFileSize(source.getFileSize() == null ? 0L : Math.max(0L, source.getFileSize()));
        item.setReleaseNotes(trimToEmpty(source.getReleaseNotes()));
        item.setGitCommit(trimToEmpty(source.getGitCommit()));
        item.setGitRange(trimToEmpty(source.getGitRange()));
        item.setTargetMode(normalizeTargetMode(source.getTargetMode()));
        item.setTenantIds(normalizeIds(source.getTenantIds()));
        item.setRoleIds(normalizeIds(source.getRoleIds()));
        item.setBuildStatus(trimToEmpty(source.getBuildStatus()));
        item.setBuildLog(trimToEmpty(source.getBuildLog()));
        item.setCreatedAt(trimToEmpty(source.getCreatedAt()));
        return item;
    }

    private void applyHistoryToConfig(MobileReleaseHistoryVO history, AdminMobileReleaseConfigDTO config) {
        config.setVersionName(history.getVersionName());
        config.setVersionCode(history.getVersionCode());
        config.setMinSupportedVersionCode(history.getMinSupportedVersionCode());
        config.setForceUpgrade(Boolean.TRUE.equals(history.getForceUpgrade()));
        config.setDownloadUrl(history.getDownloadUrl());
        config.setApkPath(history.getApkPath());
        config.setFileSize(history.getFileSize());
        config.setReleaseNotes(history.getReleaseNotes());
        config.setBuildId(history.getBuildId());
        config.setGitCommit(history.getGitCommit());
        config.setGitRange(history.getGitRange());
        config.setTargetMode(history.getTargetMode());
        config.setTenantIds(normalizeIds(history.getTenantIds()));
        config.setRoleIds(normalizeIds(history.getRoleIds()));
    }

    private Long createMobileBuildRecord(AdminMobileReleaseConfigDTO config) {
        if (jdbcTemplate == null) {
            return null;
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO mobile_release_build_record " +
                            "(status, version_name, version_code, min_supported_version_code, force_upgrade, release_notes, release_source, build_log, created_by, create_time, update_time) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, "queued");
            statement.setString(2, config.getVersionName());
            statement.setInt(3, safeInt(config.getVersionCode()));
            statement.setInt(4, safeInt(config.getMinSupportedVersionCode()));
            statement.setInt(5, Boolean.TRUE.equals(config.getForceUpgrade()) ? 1 : 0);
            statement.setString(6, config.getReleaseNotes());
            statement.setString(7, "auto-build");
            statement.setString(8, "auto build queued");
            Long userId = UserContext.getUser();
            if (userId == null) {
                statement.setObject(9, null);
            } else {
                statement.setLong(9, userId);
            }
            return statement;
        }, keyHolder);
        Number id = keyHolder.getKey();
        if (id == null) {
            return null;
        }
        appendBuildLog(id.longValue(), "SYSTEM", "auto build queued");
        return id.longValue();
    }

    private void scheduleMobileBuild(Long buildId) {
        if (buildId == null || buildId <= 0 || jdbcTemplate == null || artifactStorageService == null) {
            if (buildId != null) {
                appendBuildLog(buildId, "ERROR", "auto build dependencies are not available in current runtime");
                updateBuildFailure(buildId, "auto build dependencies are not available in current runtime");
            }
            return;
        }
        if (runningBuilds.putIfAbsent(buildId, Boolean.TRUE) != null) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                runMobileBuild(buildId);
            } finally {
                runningBuilds.remove(buildId);
                pendingBuildConfigs.remove(buildId);
            }
        }, buildExecutor);
    }

    private void runMobileBuild(Long buildId) {
        boolean injectedTenantContext = TenantContext.getTenantId() == null;
        if (injectedTenantContext) {
            TenantContext.setTenantId(CONFIG_TENANT_ID);
        }
        try {
            long started = System.nanoTime();
            updateBuildStatus(buildId, "running", "build started");
            appendBuildLog(buildId, "SYSTEM", "build started");
            try {
                SystemConfig existing = loadConfigRow();
                AdminMobileReleaseConfigDTO activeConfig = existing == null ? normalize(null) : readConfig(existing);
                AdminMobileReleaseConfigDTO config = resolvePendingBuildConfig(buildId, activeConfig);
                String previousCommit = findPreviousSuccessfulCommit();
                MobileBuildArtifact artifact = runAndroidBuild(buildId, config, previousCommit);
                updateBuildGitInfo(buildId, artifact.gitCommit, artifact.gitRange, artifact.releaseNotes);
                appendBuildLog(buildId, "GIT", "commit: " + artifact.gitCommit);
                appendBuildLog(buildId, "GIT", artifact.releaseNotes);

                String apkPath = artifactStorageService.uploadApk(artifact.fileName, artifact.bytes);
                String downloadUrl = toMobileReleasePublicDownloadUrl(apkPath);
                if (!StringUtils.hasText(downloadUrl)) {
                    downloadUrl = mediaPathHelper == null ? apkPath : mediaPathHelper.toPublicUrl(apkPath);
                }

                config.setHistory(copyHistory(activeConfig.getHistory()));
                config.setEnabled(true);
                config.setBuildId(buildId);
                config.setDownloadUrl(downloadUrl);
                config.setApkPath(apkPath);
                config.setFileSize((long) artifact.bytes.length);
                config.setReleaseNotes(artifact.releaseNotes);
                config.setReleaseSource("auto-build");
                config.setBuildStatus("success");
                config.setBuildLog("auto build success: " + apkPath);
                config.setGitCommit(artifact.gitCommit);
                config.setGitRange(artifact.gitRange);
                config.setCreatedAt(now());
                appendHistory(config, "auto-build", "active");
                persistConfig(existing, config, true);

                updateBuildSuccess(buildId, config, downloadUrl, apkPath, artifact.bytes.length,
                        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
                appendBuildLog(buildId, "SYSTEM", "apk uploaded: " + downloadUrl);
            } catch (Exception e) {
                log.error("mobile auto build failed, buildId={}", buildId, e);
                appendBuildLog(buildId, "ERROR", e.getMessage());
                updateBuildFailure(buildId, e.getMessage());
                markConfigBuildFailed(buildId, e.getMessage());
            }
        } finally {
            if (injectedTenantContext) {
                TenantContext.removeTenantId();
            }
        }
    }

    private AdminMobileReleaseConfigDTO prepareQueuedBuildConfig(AdminMobileReleaseConfigDTO activeConfig) {
        AdminMobileReleaseConfigDTO config = copyConfig(activeConfig);
        config.setVersionName(incrementVersionName(config.getVersionName()));
        config.setVersionCode(Math.max(1, safeInt(config.getVersionCode()) + 1));
        config.setReleaseSource("auto-build");
        config.setBuildStatus("queued");
        config.setBuildLog("auto build queued");
        config.setCreatedAt(now());
        return config;
    }

    private AdminMobileReleaseConfigDTO resolvePendingBuildConfig(Long buildId, AdminMobileReleaseConfigDTO activeConfig) {
        AdminMobileReleaseConfigDTO pending = buildId == null ? null : pendingBuildConfigs.get(buildId);
        if (pending != null) {
            return copyConfig(pending);
        }
        AdminMobileReleaseConfigDTO fallback = prepareQueuedBuildConfig(activeConfig);
        fallback.setBuildId(buildId);
        return fallback;
    }

    private AdminMobileReleaseConfigDTO copyConfig(AdminMobileReleaseConfigDTO source) {
        if (source == null) {
            return normalize(null);
        }
        try {
            return normalize(objectMapper.readValue(objectMapper.writeValueAsString(source), AdminMobileReleaseConfigDTO.class));
        } catch (Exception e) {
            throw new IllegalArgumentException("copy mobile release config failed", e);
        }
    }

    private MobileBuildArtifact runAndroidBuild(Long buildId, AdminMobileReleaseConfigDTO config, String previousCommit) throws Exception {
        if (shouldUseK8sBuild()) {
            appendBuildLog(buildId, "SYSTEM", "build mode: kubernetes pod");
            return runK8sAndroidBuild(buildId, config, previousCommit);
        }
        appendBuildLog(buildId, "SYSTEM", "build mode: local runtime");
        return runLocalAndroidBuild(buildId, config, previousCommit);
    }

    private boolean shouldUseK8sBuild() {
        String mode = trimToDefault(androidBuildMode, "auto").toLowerCase(Locale.ROOT);
        if ("k8s".equals(mode) || "kubernetes".equals(mode)) {
            return true;
        }
        if ("local".equals(mode)) {
            return false;
        }
        return !hasLocalAndroidProject();
    }

    private boolean hasLocalAndroidProject() {
        try {
            resolveAndroidProjectDir();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private MobileBuildArtifact runLocalAndroidBuild(Long buildId, AdminMobileReleaseConfigDTO config, String previousCommit) throws Exception {
        Path projectDir = resolveAndroidProjectDir();
        appendBuildLog(buildId, "SYSTEM", "android project: " + projectDir);
        Path repoRoot = resolveGitRoot(projectDir);
        String currentCommit = runProcessForText(repoRoot, List.of("git", "rev-parse", "--short=12", "HEAD"), 20);
        String releaseNotes = buildGitReleaseNotes(repoRoot, previousCommit);
        String gitRange = StringUtils.hasText(previousCommit) ? previousCommit + ".." + currentCommit : "HEAD~10.." + currentCommit;

        ProcessResult processResult = runAndroidBuildCommand(buildId, projectDir, config);
        if (processResult.exitCode != 0) {
            throw new IllegalStateException("Android build failed with exit code " + processResult.exitCode);
        }
        Path apk = findLatestApk(projectDir);
        appendBuildLog(buildId, "SYSTEM", "apk found: " + apk);
        return new MobileBuildArtifact(
                Files.readAllBytes(apk),
                apk.getFileName().toString(),
                currentCommit,
                gitRange,
                releaseNotes
        );
    }

    private MobileBuildArtifact runK8sAndroidBuild(Long buildId, AdminMobileReleaseConfigDTO config, String previousCommit) throws Exception {
        String apiUrl = resolveK8sApiUrl();
        if (!StringUtils.hasText(apiUrl)) {
            throw new IllegalStateException("Kubernetes API is not available for mobile auto build");
        }
        Path tokenPath = Path.of(k8sTokenPath);
        if (!Files.isRegularFile(tokenPath)) {
            throw new IllegalStateException("Kubernetes service account token not found: " + k8sTokenPath);
        }
        String token = Files.readString(tokenPath, StandardCharsets.UTF_8).trim();
        String namespace = sanitizeK8sName(trimToDefault(androidBuildNamespace, "shetuanguanlixitong"), "namespace");
        String podName = sanitizeK8sPodName("rk-mobile-build-" + buildId);
        HttpClient client = buildK8sHttpClient(apiUrl);
        String buildScript = buildK8sMobileBuildScript();
        Map<String, Object> manifest = buildK8sBuildPodManifest(podName, namespace, buildScript, config, previousCommit);
        int timeoutMinutes = Math.max(1, Math.min(androidBuildTimeoutMinutes == null ? 20 : androidBuildTimeoutMinutes, 120));
        appendBuildLog(buildId, "SYSTEM", "k8s pod: " + namespace + "/" + podName);
        appendBuildLog(buildId, "SYSTEM", "builder image: " + trimToDefault(androidBuildImage, "maven"));
        try {
            deleteK8sBuildPod(client, apiUrl, token, namespace, podName);
            sendK8sJson(client, apiUrl, token, "POST",
                    "/api/v1/namespaces/" + encodePathSegment(namespace) + "/pods",
                    manifest,
                    201,
                    409);
            MobileBuildArtifact artifact = waitForK8sBuildPod(client, apiUrl, token, namespace, podName, buildId, timeoutMinutes);
            appendBuildLog(buildId, "SYSTEM", "k8s apk received: " + artifact.fileName + " (" + artifact.bytes.length + " bytes)");
            return artifact;
        } finally {
            if (!Boolean.FALSE.equals(cleanupK8sBuildPod)) {
                deleteK8sBuildPod(client, apiUrl, token, namespace, podName);
            }
        }
    }

    private Map<String, Object> buildK8sBuildPodManifest(String podName,
                                                          String namespace,
                                                          String buildScript,
                                                          AdminMobileReleaseConfigDTO config,
                                                          String previousCommit) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("name", podName);
        metadata.put("namespace", namespace);
        metadata.put("labels", Map.of("app", "rk-mobile-release-build", "build-id", podName));

        List<Map<String, String>> env = new ArrayList<>();
        String gitUrl = trimToDefault(androidBuildGitUrl, "https://github.com/example/rk-web");
        env.add(k8sEnv("RK_GIT_URL", gitUrl));
        env.add(k8sEnv("RK_GIT_BRANCH", trimToDefault(androidBuildGitBranch, "cloud-master-new")));
        env.add(k8sEnv("RK_GIT_USERNAME", resolveMobileBuildGitUsername(gitUrl)));
        env.add(k8sEnv("RK_GIT_PASSWORD", resolveMobileBuildGitPassword(gitUrl)));
        env.add(k8sEnv("RK_PREVIOUS_COMMIT", trimToEmpty(previousCommit)));
        env.add(k8sEnv("RK_VERSION_NAME", trimToDefault(config == null ? null : config.getVersionName(), "0.0.1")));
        env.add(k8sEnv("RK_VERSION_CODE", String.valueOf(Math.max(1, safeInt(config == null ? null : config.getVersionCode())))));
        if (StringUtils.hasText(androidToolsUrl)) {
            env.add(k8sEnv("ANDROID_TOOLS_URL", androidToolsUrl.trim()));
        }

        Map<String, Object> container = new LinkedHashMap<>();
        container.put("name", "builder");
        container.put("image", trimToDefault(androidBuildImage, "m.daocloud.io/docker.io/library/maven:3.9.9-eclipse-temurin-17"));
        container.put("imagePullPolicy", "IfNotPresent");
        container.put("command", List.of("/bin/sh", "-lc"));
        container.put("args", List.of(buildScript));
        container.put("env", env);

        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("restartPolicy", "Never");
        spec.put("containers", List.of(container));
        if (StringUtils.hasText(androidBuildImagePullSecret)) {
            spec.put("imagePullSecrets", List.of(Map.of("name", androidBuildImagePullSecret.trim())));
        }

        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("apiVersion", "v1");
        manifest.put("kind", "Pod");
        manifest.put("metadata", metadata);
        manifest.put("spec", spec);
        return manifest;
    }

    private Map<String, String> k8sEnv(String name, String value) {
        Map<String, String> env = new LinkedHashMap<>();
        env.put("name", name);
        env.put("value", value == null ? "" : value);
        return env;
    }

    private String resolveMobileBuildGitUsername(String gitUrl) {
        if (!shouldUseConfiguredGiteeCredentials(gitUrl)) {
            return "";
        }
        return trimToEmpty(jenkinsGitGiteeUsername);
    }

    private String resolveMobileBuildGitPassword(String gitUrl) {
        if (!shouldUseConfiguredGiteeCredentials(gitUrl)) {
            return "";
        }
        return trimToEmpty(jenkinsGitGiteePassword);
    }

    private boolean shouldUseConfiguredGiteeCredentials(String gitUrl) {
        String value = trimToEmpty(gitUrl);
        if (!StringUtils.hasText(value)) {
            return false;
        }
        try {
            String host = URI.create(value).getHost();
            return host != null && host.toLowerCase(Locale.ROOT).endsWith("gitee.com");
        } catch (Exception ignored) {
            return value.toLowerCase(Locale.ROOT).contains("gitee.com");
        }
    }

    private String buildK8sMobileBuildScript() {
        return String.join("\n",
                "set -eu",
                "WORK=/tmp/rk-mobile-build",
                "REPO=${WORK}/repo",
                "ANDROID_SDK=${WORK}/android-sdk",
                "NETRC=${WORK}/.netrc",
                "rm -rf \"${WORK}\"",
                "mkdir -p \"${WORK}\"",
                "export HOME=\"${WORK}\"",
                "export GIT_TERMINAL_PROMPT=0",
                "if [ -n \"${RK_GIT_USERNAME:-}\" ] && [ -n \"${RK_GIT_PASSWORD:-}\" ]; then",
                "  GIT_HOST=$(printf '%s\\n' \"${RK_GIT_URL}\" | sed -E 's#^[a-zA-Z][a-zA-Z0-9+.-]*://([^/@]+@)?([^/:]+).*#\\2#')",
                "  printf 'machine %s\\nlogin %s\\npassword %s\\n' \"${GIT_HOST}\" \"${RK_GIT_USERNAME}\" \"${RK_GIT_PASSWORD}\" > \"${NETRC}\"",
                "  chmod 600 \"${NETRC}\"",
                "  echo \"git credentials: configured for ${GIT_HOST}\"",
                "else",
                "  echo \"git credentials: not configured\"",
                "fi",
                "echo \"clone ${RK_GIT_URL} branch ${RK_GIT_BRANCH}\"",
                "git clone --depth 100 --branch \"${RK_GIT_BRANCH}\" \"${RK_GIT_URL}\" \"${REPO}\"",
                "rm -f \"${NETRC}\"",
                "cd \"${REPO}\"",
                "git config --global --add safe.directory \"${REPO}\" || true",
                "COMMIT=$(git rev-parse --short=12 HEAD)",
                "echo \"__RK_GIT_URL__=${RK_GIT_URL}\"",
                "echo \"__RK_GIT_COMMIT__=${COMMIT}\"",
                "if [ -n \"${RK_PREVIOUS_COMMIT}\" ] && git cat-file -e \"${RK_PREVIOUS_COMMIT}^{commit}\" 2>/dev/null; then",
                "  RANGE=\"${RK_PREVIOUS_COMMIT}..${COMMIT}\"",
                "  NOTES=$(git log \"${RK_PREVIOUS_COMMIT}..HEAD\" --oneline --no-merges || true)",
                "else",
                "  RANGE=\"HEAD~10..${COMMIT}\"",
                "  NOTES=$(git log -10 --oneline --no-merges || true)",
                "fi",
                "if [ -z \"${NOTES}\" ]; then NOTES='No git commits since previous build.'; fi",
                "echo \"__RK_GIT_RANGE__=${RANGE}\"",
                "echo \"__RK_GIT_NOTES_BEGIN__\"",
                "printf '%s\\n' \"${NOTES}\"",
                "echo \"__RK_GIT_NOTES_END__\"",
                "echo \"__RK_BUILD_PROJECT__=android-client/rk-club-android\"",
                "cd android-client/rk-club-android",
                "chmod +x ./build-apk.sh",
                "export ANDROID_SDK ANDROID_HOME=\"${ANDROID_SDK}\" ANDROID_SDK_ROOT=\"${ANDROID_SDK}\"",
                "./build-apk.sh --android-sdk \"${ANDROID_SDK}\" --version-name \"${RK_VERSION_NAME}\" --version-code \"${RK_VERSION_CODE}\"",
                "mkdir -p app/build/outputs/apk",
                "APK=$(find dist app/build/outputs/apk -type f -name '*.apk' -printf '%T@ %p\\n' 2>/dev/null | sort -nr | head -1 | cut -d' ' -f2-)",
                "if [ -z \"${APK}\" ] || [ ! -f \"${APK}\" ]; then echo 'APK output not found' >&2; exit 10; fi",
                "echo \"__RK_APK_FILE__=$(basename \"${APK}\")\"",
                "echo \"__RK_APK_SIZE__=$(wc -c < \"${APK}\" | tr -d ' ')\"",
                "echo \"__RK_APK_B64_BEGIN__\"",
                "base64 \"${APK}\" | fold -w 76",
                "echo \"__RK_APK_B64_END__\""
        );
    }

    private MobileBuildArtifact waitForK8sBuildPod(HttpClient client,
                                                   String apiUrl,
                                                   String token,
                                                   String namespace,
                                                   String podName,
                                                   Long buildId,
                                                   int timeoutMinutes) throws Exception {
        int maxLoops = timeoutMinutes * 12;
        int visibleLogLines = 0;
        String phase = "Pending";
        for (int i = 0; i < maxLoops; i++) {
            JsonNode pod = getK8sJson(client, apiUrl, token,
                    "/api/v1/namespaces/" + encodePathSegment(namespace) + "/pods/" + encodePathSegment(podName));
            phase = pod.path("status").path("phase").asText("Pending");
            throwIfK8sBuildPodCannotStart(pod);
            if ("Succeeded".equals(phase) || "Failed".equals(phase)) {
                break;
            }
            String logs = readK8sBuildPodVisibleLogs(client, apiUrl, token, namespace, podName);
            visibleLogLines = appendNewK8sBuildLogLines(buildId, logs, visibleLogLines);
            Thread.sleep(5000L);
        }
        if (!"Succeeded".equals(phase)) {
            String logs = readK8sBuildPodVisibleLogs(client, apiUrl, token, namespace, podName);
            appendNewK8sBuildLogLines(buildId, logs, visibleLogLines);
            throw new IllegalStateException("Kubernetes build pod did not succeed, phase=" + phase);
        }
        return readK8sBuildPodArtifact(client, apiUrl, token, namespace, podName);
    }

    private int appendNewK8sBuildLogLines(Long buildId, String logs, int alreadyVisibleLines) {
        List<String> visibleLines = extractVisibleK8sBuildLogLines(logs);
        for (int i = Math.max(0, alreadyVisibleLines); i < visibleLines.size(); i++) {
            appendBuildLog(buildId, "K8S", visibleLines.get(i));
        }
        return visibleLines.size();
    }

    private List<String> extractVisibleK8sBuildLogLines(String logs) {
        List<String> result = new ArrayList<>();
        if (!StringUtils.hasText(logs)) {
            return result;
        }
        boolean inArtifact = false;
        for (String line : logs.split("\\r?\\n")) {
            String trimmed = line.trim();
            if ("__RK_APK_B64_BEGIN__".equals(trimmed)) {
                inArtifact = true;
                result.add("APK artifact stream started");
                continue;
            }
            if ("__RK_APK_B64_END__".equals(trimmed)) {
                inArtifact = false;
                result.add("APK artifact stream finished");
                continue;
            }
            if (inArtifact) {
                continue;
            }
            if (isLikelyBase64ArtifactLine(trimmed)) {
                continue;
            }
            if (StringUtils.hasText(line)) {
                result.add(line);
            }
        }
        return result;
    }

    private boolean isLikelyBase64ArtifactLine(String value) {
        return value != null && value.length() >= 60 && value.matches("[A-Za-z0-9+/=]+");
    }

    private MobileBuildArtifact parseK8sBuildArtifact(InputStream logs) throws Exception {
        String fileName = "rk-club-debug.apk";
        String commit = "unknown";
        String gitRange = "HEAD";
        StringBuilder notes = new StringBuilder();
        ByteArrayOutputStream apkBytes = new ByteArrayOutputStream();
        boolean inNotes = false;
        boolean inArtifact = false;
        boolean sawArtifactEnd = false;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(logs, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("__RK_APK_FILE__=")) {
                    fileName = trimToDefault(trimmed.substring("__RK_APK_FILE__=".length()), fileName);
                    continue;
                }
                if (trimmed.startsWith("__RK_GIT_COMMIT__=")) {
                    commit = trimToDefault(trimmed.substring("__RK_GIT_COMMIT__=".length()), commit);
                    continue;
                }
                if (trimmed.startsWith("__RK_GIT_RANGE__=")) {
                    gitRange = trimToDefault(trimmed.substring("__RK_GIT_RANGE__=".length()), gitRange);
                    continue;
                }
                if ("__RK_GIT_NOTES_BEGIN__".equals(trimmed)) {
                    inNotes = true;
                    continue;
                }
                if ("__RK_GIT_NOTES_END__".equals(trimmed)) {
                    inNotes = false;
                    continue;
                }
                if ("__RK_APK_B64_BEGIN__".equals(trimmed)) {
                    inArtifact = true;
                    continue;
                }
                if ("__RK_APK_B64_END__".equals(trimmed)) {
                    inArtifact = false;
                    sawArtifactEnd = true;
                    continue;
                }
                if (inNotes) {
                    if (notes.length() > 0) {
                        notes.append('\n');
                    }
                    notes.append(line);
                    continue;
                }
                if (inArtifact && StringUtils.hasText(trimmed)) {
                    apkBytes.write(Base64.getMimeDecoder().decode(trimmed));
                }
            }
        }
        byte[] bytes = apkBytes.toByteArray();
        if (!sawArtifactEnd || bytes.length == 0) {
            throw new IllegalStateException("Kubernetes build returned an empty APK artifact");
        }
        return new MobileBuildArtifact(
                bytes,
                fileName,
                commit,
                gitRange,
                trimToDefault(notes.toString(), "No git commits since previous build.")
        );
    }

    private String extractMarkerValue(String text, String marker) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        for (String line : text.split("\\r?\\n")) {
            if (line.startsWith(marker)) {
                return line.substring(marker.length()).trim();
            }
        }
        return "";
    }

    private String extractBetweenMarkers(String text, String startMarker, String endMarker) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        int start = text.indexOf(startMarker);
        if (start < 0) {
            return "";
        }
        int bodyStart = start + startMarker.length();
        int end = text.indexOf(endMarker, bodyStart);
        if (end < 0) {
            return "";
        }
        return text.substring(bodyStart, end).trim();
    }

    private void throwIfK8sBuildPodCannotStart(JsonNode pod) {
        JsonNode statuses = pod.path("status").path("containerStatuses");
        if (!statuses.isArray()) {
            return;
        }
        for (JsonNode status : statuses) {
            JsonNode waiting = status.path("state").path("waiting");
            if (waiting.isMissingNode()) {
                continue;
            }
            String reason = waiting.path("reason").asText("");
            if (List.of("ErrImagePull", "ImagePullBackOff", "InvalidImageName", "CreateContainerConfigError").contains(reason)) {
                throw new IllegalStateException("Kubernetes build pod cannot start: " + reason + " " + waiting.path("message").asText(""));
            }
        }
    }

    private String readK8sBuildPodVisibleLogs(HttpClient client, String apiUrl, String token, String namespace, String podName) {
        try {
            return sendK8sText(client, apiUrl, token, "GET",
                    "/api/v1/namespaces/" + encodePathSegment(namespace)
                            + "/pods/" + encodePathSegment(podName)
                            + "/log?container=builder&limitBytes=65536",
                    null,
                    200);
        } catch (Exception e) {
            return "";
        }
    }

    private MobileBuildArtifact readK8sBuildPodArtifact(HttpClient client, String apiUrl, String token, String namespace, String podName) throws Exception {
        String path = "/api/v1/namespaces/" + encodePathSegment(namespace)
                + "/pods/" + encodePathSegment(podName)
                + "/log?container=builder";
        int maxAttempts = 4;
        int lastStatusCode = 0;
        String lastBody = "";
        Exception lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl + path))
                        .header("Authorization", "Bearer " + token)
                        .header("Accept", "text/plain,application/json")
                        .timeout(Duration.ofSeconds(60))
                        .GET()
                        .build();
                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                lastStatusCode = response.statusCode();
                try (InputStream body = response.body()) {
                    if (lastStatusCode == 200) {
                        return parseK8sBuildArtifact(body);
                    }
                    lastBody = new String(body.readNBytes(4096), StandardCharsets.UTF_8);
                }
                if (!isRetryableK8sApiFailure(lastStatusCode, lastBody) || attempt == maxAttempts) {
                    throw k8sApiStatusException(path, lastStatusCode, lastBody);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            } catch (Exception e) {
                lastException = e;
                if (!isRetryableK8sApiException(e) || attempt == maxAttempts) {
                    throw e;
                }
            }
            sleepBeforeK8sRetry(attempt);
        }
        if (lastException != null) {
            throw lastException;
        }
        throw k8sApiStatusException(path, lastStatusCode, lastBody);
    }

    private void deleteK8sBuildPod(HttpClient client, String apiUrl, String token, String namespace, String podName) {
        try {
            sendK8sText(client, apiUrl, token, "DELETE",
                    "/api/v1/namespaces/" + encodePathSegment(namespace) + "/pods/" + encodePathSegment(podName),
                    Map.of("gracePeriodSeconds", 0),
                    200,
                    202,
                    404);
        } catch (Exception e) {
            log.debug("delete mobile build pod ignored, pod={}/{}", namespace, podName, e);
        }
    }

    private String resolveK8sApiUrl() {
        if (StringUtils.hasText(k8sApiUrl)) {
            return sanitizeUrl(k8sApiUrl);
        }
        String host = System.getenv("KUBERNETES_SERVICE_HOST");
        String port = System.getenv().getOrDefault("KUBERNETES_SERVICE_PORT", "443");
        return StringUtils.hasText(host) ? "https://" + host + ":" + port : "";
    }

    private HttpClient buildK8sHttpClient(String apiUrl) throws Exception {
        HttpClient.Builder builder = HttpClient.newBuilder();
        if (apiUrl.startsWith("https://") && Files.isRegularFile(Path.of(k8sCaPath))) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate;
            try (InputStream inputStream = Files.newInputStream(Path.of(k8sCaPath))) {
                certificate = (X509Certificate) certificateFactory.generateCertificate(inputStream);
            }
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("kubernetes", certificate);
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
            builder.sslContext(sslContext);
        }
        return builder.build();
    }

    private JsonNode getK8sJson(HttpClient client, String apiUrl, String token, String path) throws Exception {
        String body = sendK8sText(client, apiUrl, token, "GET", path, null, 200);
        return objectMapper.readTree(body);
    }

    private JsonNode sendK8sJson(HttpClient client, String apiUrl, String token, String method, String path, Object body, int... successCodes) throws Exception {
        String responseBody = sendK8sText(client, apiUrl, token, method, path, body, successCodes);
        return objectMapper.readTree(responseBody);
    }

    private String sendK8sText(HttpClient client, String apiUrl, String token, String method, String path, Object body, int... successCodes) throws Exception {
        int maxAttempts = 4;
        int lastStatusCode = 0;
        String lastBody = "";
        Exception lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpRequest.BodyPublisher publisher = body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8);
                HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(apiUrl + path))
                        .header("Authorization", "Bearer " + token)
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .method(method, publisher);
                if (body != null) {
                    builder.header("Content-Type", "application/json");
                }
                HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                lastStatusCode = response.statusCode();
                lastBody = response.body();
                for (int code : successCodes) {
                    if (lastStatusCode == code) {
                        return lastBody;
                    }
                }
                if (!isRetryableK8sApiFailure(lastStatusCode, lastBody) || attempt == maxAttempts) {
                    throw k8sApiStatusException(path, lastStatusCode, lastBody);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            } catch (Exception e) {
                lastException = e;
                if (!isRetryableK8sApiException(e) || attempt == maxAttempts) {
                    throw e;
                }
            }
            sleepBeforeK8sRetry(attempt);
        }
        if (lastException != null) {
            throw lastException;
        }
        throw k8sApiStatusException(path, lastStatusCode, lastBody);
    }

    private boolean isRetryableK8sApiFailure(int statusCode, String body) {
        if (List.of(429, 500, 502, 503, 504).contains(statusCode)) {
            return true;
        }
        return body != null && body.toLowerCase(Locale.ROOT).contains("leader changed");
    }

    private boolean isRetryableK8sApiException(Exception e) {
        String message = e.getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains("leader changed");
    }

    private void sleepBeforeK8sRetry(int attempt) throws InterruptedException {
        Thread.sleep(Math.min(2000L, 300L * Math.max(1, attempt)));
    }

    private IllegalStateException k8sApiStatusException(String path, int statusCode, String body) {
        return new IllegalStateException("Kubernetes API " + path + " returned " + statusCode + ": " + body);
    }

    private String sanitizeK8sName(String value, String field) {
        String text = trimToDefault(value, "");
        if (!text.matches("[A-Za-z0-9_.:-]+")) {
            throw new IllegalStateException("invalid " + field + ": " + value);
        }
        return text;
    }

    private String sanitizeK8sPodName(String value) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]", "-");
        normalized = normalized.replaceAll("^-+", "").replaceAll("-+$", "");
        if (!StringUtils.hasText(normalized)) {
            normalized = "rk-mobile-build";
        }
        return normalized.length() <= 63 ? normalized : normalized.substring(0, 63).replaceAll("-+$", "");
    }

    private String sanitizeUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return "";
        }
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private Path resolveAndroidProjectDir() {
        List<Path> candidates = new ArrayList<>();
        if (StringUtils.hasText(androidProjectPath)) {
            Path configured = Path.of(androidProjectPath.trim());
            candidates.add(configured);
            if (!configured.isAbsolute()) {
                candidates.add(Path.of(System.getProperty("user.dir")).resolve(configured));
            }
        }
        Path cursor = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (int i = 0; i < 6 && cursor != null; i++) {
            candidates.add(cursor.resolve("android-client").resolve("rk-club-android"));
            cursor = cursor.getParent();
        }
        return candidates.stream()
                .map(Path::toAbsolutePath)
                .filter(Files::isDirectory)
                .filter(path -> Files.isRegularFile(path.resolve("build-apk.ps1")))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("android-client/rk-club-android build-apk.ps1 not found"));
    }

    private Path resolveGitRoot(Path projectDir) {
        try {
            String root = runProcessForText(projectDir, List.of("git", "rev-parse", "--show-toplevel"), 20);
            if (StringUtils.hasText(root)) {
                return Path.of(root.trim());
            }
        } catch (Exception ignored) {
        }
        Path cursor = projectDir.toAbsolutePath();
        while (cursor != null) {
            if (Files.isDirectory(cursor.resolve(".git"))) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        return projectDir;
    }

    private ProcessResult runAndroidBuildCommand(Long buildId, Path projectDir, AdminMobileReleaseConfigDTO config) throws Exception {
        List<String> command = buildAndroidCommand(projectDir, config);
        appendBuildLog(buildId, "SYSTEM", "command: " + String.join(" ", command));
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(projectDir.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        CompletableFuture<Integer> outputReader = CompletableFuture.supplyAsync(() -> {
            try {
                return readProcessOutput(buildId, process);
            } catch (Exception e) {
                appendBuildLog(buildId, "ERROR", "read build output failed: " + e.getMessage());
                throw new CompletionException(e);
            }
        });
        int timeoutMinutes = Math.max(1, Math.min(androidBuildTimeoutMinutes == null ? 20 : androidBuildTimeoutMinutes, 120));
        boolean finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            appendBuildLog(buildId, "ERROR", "build timed out after " + timeoutMinutes + " minutes");
            Integer lines = waitForBuildOutputReader(outputReader, buildId);
            return new ProcessResult(-1, lines == null ? 0 : lines);
        }
        Integer lines = waitForBuildOutputReader(outputReader, buildId);
        return new ProcessResult(process.exitValue(), lines == null ? 0 : lines);
    }

    private Integer waitForBuildOutputReader(CompletableFuture<Integer> outputReader, Long buildId) throws Exception {
        try {
            return outputReader.get(10, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            appendBuildLog(buildId, "ERROR", "build output reader timed out");
            return null;
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new IllegalStateException(cause);
        }
    }

    private List<String> buildAndroidCommand(Path projectDir, AdminMobileReleaseConfigDTO config) {
        String versionName = trimToDefault(config == null ? null : config.getVersionName(), "0.0.1");
        String versionCode = String.valueOf(Math.max(1, safeInt(config == null ? null : config.getVersionCode())));
        if (StringUtils.hasText(androidBuildCommand)) {
            String command = androidBuildCommand
                    .replace("{projectDir}", projectDir.toString())
                    .replace("{versionName}", versionName)
                    .replace("{versionCode}", versionCode);
            if (isWindows()) {
                return List.of("cmd.exe", "/c", command);
            }
            return List.of("/bin/sh", "-lc", command);
        }
        Path script = projectDir.resolve("build-apk.ps1");
        if (isWindows()) {
            return List.of("powershell.exe", "-ExecutionPolicy", "Bypass", "-File", script.toString(),
                    "-VersionName", versionName, "-VersionCode", versionCode);
        }
        Path bashScript = projectDir.resolve("build-apk.sh");
        if (Files.isRegularFile(bashScript)) {
            return List.of("/bin/sh", bashScript.toString(),
                    "--version-name", versionName, "--version-code", versionCode);
        }
        return List.of("pwsh", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", script.toString(),
                "-VersionName", versionName, "-VersionCode", versionCode);
    }

    private int readProcessOutput(Long buildId, Process process) throws Exception {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                count++;
                appendBuildLog(buildId, "CONSOLE", line);
            }
        }
        return count;
    }

    private String runProcessForText(Path dir, List<String> command, int timeoutSeconds) throws Exception {
        Process process = new ProcessBuilder(command)
                .directory(dir.toFile())
                .redirectErrorStream(true)
                .start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (output.length() > 0) {
                    output.append('\n');
                }
                output.append(line);
            }
        }
        if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException(String.join(" ", command) + " timed out");
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException(output.toString());
        }
        return output.toString().trim();
    }

    private String buildGitReleaseNotes(Path repoRoot, String previousCommit) {
        String commandName = "git log";
        try {
            List<String> command;
            if (StringUtils.hasText(previousCommit)) {
                command = List.of("git", "log", previousCommit + "..HEAD", "--oneline", "--no-merges");
            } else {
                command = List.of("git", "log", "-10", "--oneline", "--no-merges");
            }
            String log = runProcessForText(repoRoot, command, 30);
            return StringUtils.hasText(log) ? log : "No git commits since previous build.";
        } catch (Exception e) {
            return commandName + " unavailable: " + e.getMessage();
        }
    }

    private String findPreviousSuccessfulCommit() {
        if (jdbcTemplate == null) {
            return "";
        }
        try {
            List<String> rows = jdbcTemplate.queryForList(
                    "SELECT git_commit FROM mobile_release_build_record WHERE status = 'success' AND git_commit IS NOT NULL AND git_commit <> '' AND is_deleted = 0 ORDER BY id DESC LIMIT 1",
                    String.class
            );
            return rows.isEmpty() ? "" : rows.get(0);
        } catch (Exception e) {
            return "";
        }
    }

    private Path findLatestApk(Path projectDir) throws Exception {
        List<Path> roots = List.of(
                projectDir.resolve("dist"),
                projectDir.resolve("app").resolve("build").resolve("outputs").resolve("apk")
        );
        List<Path> apks = new ArrayList<>();
        for (Path root : roots) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (java.util.stream.Stream<Path> stream = Files.walk(root)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".apk"))
                        .forEach(apks::add);
            }
        }
        return apks.stream()
                .max(Comparator.comparing(path -> path.toFile().lastModified()))
                .orElseThrow(() -> new IllegalStateException("APK output not found after build"));
    }

    private synchronized void appendBuildLog(Long buildId, String type, String content) {
        if (jdbcTemplate == null || buildId == null) {
            return;
        }
        Integer nextLine = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(line_no), 0) + 1 FROM mobile_release_build_log WHERE build_id = ?",
                Integer.class,
                buildId
        );
        jdbcTemplate.update(
                "INSERT INTO mobile_release_build_log (build_id, line_no, log_type, content, created_at) VALUES (?, ?, ?, ?, NOW())",
                buildId,
                nextLine == null ? 1 : nextLine,
                trimToDefault(type, "CONSOLE"),
                content == null ? "" : content
        );
    }

    private void updateBuildStatus(Long buildId, String status, String log) {
        if (jdbcTemplate == null || buildId == null) {
            return;
        }
        jdbcTemplate.update(
                "UPDATE mobile_release_build_record SET status = ?, build_log = ?, started_at = COALESCE(started_at, NOW()), update_time = NOW() WHERE id = ?",
                status,
                log,
                buildId
        );
    }

    private void updateBuildGitInfo(Long buildId, String commit, String gitRange, String notes) {
        if (jdbcTemplate == null || buildId == null) {
            return;
        }
        jdbcTemplate.update(
                "UPDATE mobile_release_build_record SET git_commit = ?, git_range = ?, release_notes = ?, update_time = NOW() WHERE id = ?",
                commit,
                gitRange,
                notes,
                buildId
        );
    }

    private void updateBuildSuccess(Long buildId, AdminMobileReleaseConfigDTO config, String downloadUrl,
                                    String apkPath, long fileSize, long durationMillis) {
        if (jdbcTemplate == null || buildId == null) {
            return;
        }
        jdbcTemplate.update(
                "UPDATE mobile_release_build_record SET status = 'success', version_name = ?, version_code = ?, min_supported_version_code = ?, force_upgrade = ?, download_url = ?, apk_path = ?, file_size_bytes = ?, release_notes = ?, git_commit = ?, git_range = ?, build_log = ?, finished_at = NOW(), duration_ms = ?, update_time = NOW() WHERE id = ?",
                config.getVersionName(),
                config.getVersionCode(),
                config.getMinSupportedVersionCode(),
                Boolean.TRUE.equals(config.getForceUpgrade()) ? 1 : 0,
                downloadUrl,
                apkPath,
                fileSize,
                config.getReleaseNotes(),
                config.getGitCommit(),
                config.getGitRange(),
                config.getBuildLog(),
                durationMillis,
                buildId
        );
    }

    private void updateBuildFailure(Long buildId, String message) {
        if (jdbcTemplate == null || buildId == null) {
            return;
        }
        jdbcTemplate.update(
                "UPDATE mobile_release_build_record SET status = 'failed', build_log = ?, finished_at = NOW(), update_time = NOW() WHERE id = ?",
                message,
                buildId
        );
    }

    private void markConfigBuildFailed(Long buildId, String message) {
        SystemConfig existing = loadConfigRow();
        if (existing == null) {
            return;
        }
        AdminMobileReleaseConfigDTO config = readConfig(existing);
        if (!Objects.equals(config.getBuildId(), buildId)) {
            return;
        }
        config.setReleaseSource("auto-build");
        config.setBuildStatus("failed");
        config.setBuildLog(message);
        config.setCreatedAt(now());
        appendHistory(config, "auto-build", "failed");
        persistConfig(existing, config, Boolean.TRUE.equals(config.getEnabled()));
    }

    private MobileReleaseBuildRecordVO mapBuildRecord(Map<String, Object> row) {
        MobileReleaseBuildRecordVO vo = new MobileReleaseBuildRecordVO();
        vo.setId(asLong(row.get("id")));
        vo.setStatus(asString(row.get("status")));
        vo.setVersionName(asString(row.get("version_name")));
        vo.setVersionCode(asInteger(row.get("version_code")));
        vo.setMinSupportedVersionCode(asInteger(row.get("min_supported_version_code")));
        vo.setForceUpgrade(asInteger(row.get("force_upgrade")) == 1);
        vo.setDownloadUrl(asString(row.get("download_url")));
        vo.setApkPath(asString(row.get("apk_path")));
        vo.setFileSize(asLong(row.get("file_size_bytes")));
        vo.setReleaseNotes(asString(row.get("release_notes")));
        vo.setReleaseSource(asString(row.get("release_source")));
        vo.setGitCommit(asString(row.get("git_commit")));
        vo.setGitRange(asString(row.get("git_range")));
        vo.setBuildLog(asString(row.get("build_log")));
        vo.setStartedAt(formatObjectTime(row.get("started_at")));
        vo.setFinishedAt(formatObjectTime(row.get("finished_at")));
        vo.setDurationMillis(asLong(row.get("duration_ms")));
        vo.setCreatedAt(formatObjectTime(row.get("create_time")));
        return vo;
    }

    private MobileReleaseBuildLogVO mapBuildLog(Map<String, Object> row) {
        MobileReleaseBuildLogVO vo = new MobileReleaseBuildLogVO();
        vo.setId(asLong(row.get("id")));
        vo.setBuildId(asLong(row.get("build_id")));
        vo.setLineNo(asInteger(row.get("line_no")));
        vo.setLogType(asString(row.get("log_type")));
        vo.setContent(asString(row.get("content")));
        vo.setCreatedAt(formatObjectTime(row.get("created_at")));
        return vo;
    }

    private String incrementVersionName(String versionName) {
        String value = trimToDefault(versionName, "0.0.0");
        String[] parts = value.split("\\.");
        if (parts.length == 0) {
            return "0.0.1";
        }
        try {
            int last = Integer.parseInt(parts[parts.length - 1]);
            parts[parts.length - 1] = String.valueOf(last + 1);
            return String.join(".", parts);
        } catch (Exception e) {
            return value + ".1";
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private Long asLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String formatObjectTime(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).format(TIME_FORMATTER);
        }
        return String.valueOf(value).replace("T", " ");
    }

    private static class ProcessResult {
        private final int exitCode;
        private final int lines;

        private ProcessResult(int exitCode, int lines) {
            this.exitCode = exitCode;
            this.lines = lines;
        }
    }

    private static class MobileBuildArtifact {
        private final byte[] bytes;
        private final String fileName;
        private final String gitCommit;
        private final String gitRange;
        private final String releaseNotes;

        private MobileBuildArtifact(byte[] bytes, String fileName, String gitCommit, String gitRange, String releaseNotes) {
            this.bytes = bytes;
            this.fileName = fileName;
            this.gitCommit = gitCommit;
            this.gitRange = gitRange;
            this.releaseNotes = releaseNotes;
        }
    }

    private String resolveDownloadUrl(AdminMobileReleaseConfigDTO config) {
        if (StringUtils.hasText(config.getDownloadUrl())) {
            return normalizeMobileReleaseDownloadUrl(config.getDownloadUrl());
        }
        if (StringUtils.hasText(config.getApkPath())) {
            String downloadUrl = toMobileReleasePublicDownloadUrl(config.getApkPath());
            if (StringUtils.hasText(downloadUrl)) {
                return downloadUrl;
            }
            if (mediaPathHelper != null) {
                return mediaPathHelper.toPublicUrl(config.getApkPath());
            }
        }
        return "";
    }

    private String normalizeMobileReleaseDownloadUrl(String value) {
        String objectPath = extractMobileReleaseObjectPath(value);
        if (StringUtils.hasText(objectPath)) {
            return toMobileReleasePublicDownloadUrl(objectPath);
        }
        return value;
    }

    private String toMobileReleasePublicDownloadUrl(String value) {
        String objectPath = extractMobileReleaseObjectPath(value);
        if (!StringUtils.hasText(objectPath)) {
            return "";
        }
        String baseUrl = trimTrailingSlash(trimToDefault(mobileReleasePublicBaseUrl, "https://example.com"));
        String filePrefix = "/" + trimSlashes(trimToDefault(mobileReleasePublicFilePrefix, "/minio-files/")) + "/";
        return baseUrl + filePrefix + trimSlashes(objectPath);
    }

    private String extractMobileReleaseObjectPath(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String trimmed = value.trim();
        String path = trimmed;
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            try {
                path = URI.create(trimmed).getPath();
            } catch (Exception ignored) {
                return "";
            }
        }
        path = trimSlashes(path);
        String prefix = trimSlashes(trimToDefault(mobileReleasePublicFilePrefix, "/minio-files/")) + "/";
        int minioIndex = path.indexOf(prefix);
        if (minioIndex >= 0) {
            path = path.substring(minioIndex + prefix.length());
        }
        return path.startsWith("rk-user/mobile-apk/") ? path : "";
    }

    private boolean isTargetMatched(AdminMobileReleaseConfigDTO config, Long tenantId, Long roleId) {
        String mode = normalizeTargetMode(config.getTargetMode());
        if ("tenant".equals(mode)) {
            return tenantId != null && normalizeIds(config.getTenantIds()).contains(tenantId);
        }
        if ("role".equals(mode)) {
            return roleId != null && normalizeIds(config.getRoleIds()).contains(roleId);
        }
        return true;
    }

    private String targetMismatchMessage(AdminMobileReleaseConfigDTO config) {
        return "role".equals(normalizeTargetMode(config.getTargetMode())) ? "褰撳墠瑙掕壊涓嶅湪鍗囩骇鑼冨洿鍐? : "褰撳墠绉熸埛涓嶅湪鍗囩骇鑼冨洿鍐?;
    }

    private String normalizeTargetMode(String value) {
        String mode = trimToDefault(value, "all").toLowerCase(Locale.ROOT);
        if ("tenant".equals(mode) || "role".equals(mode)) {
            return mode;
        }
        return "all";
    }

    private List<Long> normalizeIds(List<Long> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .filter(value -> value > 0)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String trimToDefault(String value, String fallback) {
        String trimmed = trimToEmpty(value);
        return StringUtils.hasText(trimmed) ? trimmed : fallback;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimSlashes(String value) {
        String result = trimToEmpty(value);
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String trimTrailingSlash(String value) {
        String result = trimToEmpty(value);
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String now() {
        return LocalDateTime.now().format(TIME_FORMATTER);
    }
}
