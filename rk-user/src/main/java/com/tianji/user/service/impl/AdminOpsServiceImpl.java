package com.tianji.user.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;
import com.sun.management.OperatingSystemMXBean;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.TenantContext;
import com.tianji.common.utils.UserContext;
import com.tianji.user.domain.dto.adminops.AdminBackupCreateDTO;
import com.tianji.user.domain.dto.adminops.AdminBackupScheduleDTO;
import com.tianji.user.domain.dto.adminops.AdminJenkinsBuildRequestDTO;
import com.tianji.user.domain.dto.adminops.AdminDeployPackageCreateDTO;
import com.tianji.user.domain.dto.adminops.AdminDeployPackageRepairDTO;
import com.tianji.user.domain.dto.adminops.AdminK8sAbnormalPodCleanupDTO;
import com.tianji.user.domain.dto.adminops.AdminK8sCleanupRequestDTO;
import com.tianji.user.domain.dto.adminops.AdminK8sClusterMaintenanceDTO;
import com.tianji.user.domain.dto.adminops.AdminK8sImageCleanupDTO;
import com.tianji.user.domain.dto.adminops.AdminK8sPodExecRequestDTO;
import com.tianji.user.domain.dto.adminops.AdminK8sWorkloadActionDTO;
import com.tianji.user.domain.dto.adminops.AdminNacosConfigSaveDTO;
import com.tianji.user.domain.dto.adminops.AdminOpsServiceSaveDTO;
import com.tianji.user.domain.dto.adminops.AdminRainbondApiCallDTO;
import com.tianji.user.domain.dto.adminops.AdminRainbondConfigDTO;
import com.tianji.user.domain.dto.adminops.AdminTrafficDefaultTenantDTO;
import com.tianji.user.domain.vo.adminops.AdminApiWorkbenchDocVO;
import com.tianji.user.domain.vo.adminops.AdminApiWorkbenchEndpointVO;
import com.tianji.user.domain.vo.adminops.AdminApiWorkbenchResourceVO;
import com.tianji.user.domain.vo.adminops.AdminBackupOverviewVO;
import com.tianji.user.domain.vo.adminops.AdminBackupRecordVO;
import com.tianji.user.domain.vo.adminops.AdminBackupScheduleVO;
import com.tianji.user.domain.vo.adminops.AdminBackupStorageStatsVO;
import com.tianji.user.domain.vo.adminops.AdminContainerVO;
import com.tianji.user.domain.vo.adminops.AdminDeployPackageDetailVO;
import com.tianji.user.domain.vo.adminops.AdminDeployPackageDiagnosisVO;
import com.tianji.user.domain.vo.adminops.AdminDeployPackageKubeconfigVO;
import com.tianji.user.domain.vo.adminops.AdminDeployPackagePreflightVO;
import com.tianji.user.domain.vo.adminops.AdminDeployPackageRecordVO;
import com.tianji.user.domain.vo.adminops.AdminDeployPackageRepairResultVO;
import com.tianji.user.domain.vo.adminops.AdminJenkinsBuildRecordVO;
import com.tianji.user.domain.vo.adminops.AdminJenkinsBuildStatusVO;
import com.tianji.user.domain.vo.adminops.AdminJenkinsBuildTriggerVO;
import com.tianji.user.domain.vo.adminops.AdminJenkinsGitBranchesVO;
import com.tianji.user.domain.vo.adminops.AdminJenkinsGitSourceVO;
import com.tianji.user.domain.vo.adminops.AdminJenkinsJobVO;
import com.tianji.user.domain.vo.adminops.AdminJenkinsOverviewVO;
import com.tianji.user.domain.vo.adminops.AdminK8sActionResultVO;
import com.tianji.user.domain.vo.adminops.AdminK8sAbnormalPodVO;
import com.tianji.user.domain.vo.adminops.AdminK8sCleanupResultVO;
import com.tianji.user.domain.vo.adminops.AdminK8sMaintenanceResultVO;
import com.tianji.user.domain.vo.adminops.AdminK8sNodeVO;
import com.tianji.user.domain.vo.adminops.AdminK8sOverviewVO;
import com.tianji.user.domain.vo.adminops.AdminK8sImageVO;
import com.tianji.user.domain.vo.adminops.AdminK8sPodExecResultVO;
import com.tianji.user.domain.vo.adminops.AdminK8sWorkloadVO;
import com.tianji.user.domain.vo.adminops.AdminMinioBucketVO;
import com.tianji.user.domain.vo.adminops.AdminMinioObjectVO;
import com.tianji.user.domain.vo.adminops.AdminMonitoringLogVO;
import com.tianji.user.domain.vo.adminops.AdminMonitoringOverviewVO;
import com.tianji.user.domain.vo.adminops.AdminNacosConfigVO;
import com.tianji.user.domain.vo.adminops.AdminNacosConfigHistoryVO;
import com.tianji.user.domain.vo.adminops.AdminOpsAuditLogVO;
import com.tianji.user.domain.vo.adminops.AdminOpsServiceVO;
import com.tianji.user.domain.vo.adminops.AdminRainbondApiResultVO;
import com.tianji.user.domain.vo.adminops.AdminRainbondConfigVO;
import com.tianji.user.domain.vo.adminops.AdminTaskLogVO;
import com.tianji.user.domain.vo.adminops.AdminTaskOverviewVO;
import com.tianji.user.domain.vo.adminops.AdminTaskVO;
import com.tianji.user.domain.vo.adminops.AdminTopologyVO;
import com.tianji.user.domain.vo.adminops.AdminTrafficLocationVO;
import com.tianji.user.domain.vo.adminops.AdminTrafficOnlineUserVO;
import com.tianji.user.domain.vo.adminops.AdminTrafficOverviewVO;
import com.tianji.user.domain.vo.adminops.AdminTrafficTenantOptionVO;
import com.tianji.user.service.IAdminOpsService;
import com.tianji.user.service.adminops.AdminOpsNetworkMetricParser;
import com.tianji.user.service.adminops.AdminBackupArchiveBuilder;
import com.tianji.user.service.adminops.AdminBackupStorageService;
import com.tianji.user.service.adminops.NetworkUsageSnapshot;
import com.tianji.user.utils.PublicBaseUrlResolver;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.CookieManager;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.Socket;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOpsServiceImpl implements IAdminOpsService {

    private static final List<String> DEFAULT_MONITORED_SERVICES = List.of(
            "rk-gateway", "rk-auth", "rk-user", "rk-content", "rk-activity", "rk-message"
    );
    private static final List<String> DEFAULT_BACKUP_DATABASES = List.of(
            "rk_auth", "rk_user", "rk_activity", "rk_content", "rk_message",
            "rk_file", "rk_gateway", "rk_exam", "rk_pay", "rk_search", "rk_trade", "rk_data",
            "xxl_job"
    );
    private static final List<String> DEFAULT_DEPLOY_PACKAGE_DATABASES = List.of(
            "rk_auth", "rk_user", "rk_activity", "rk_content", "rk_message",
            "rk_file", "rk_gateway", "rk_exam", "rk_pay", "rk_search", "rk_trade", "rk_data",
            "nacos", "xxl_job"
    );
    private static final List<String> SUPPORTED_K8S_CLEANUP_ACTIONS = List.of("IMAGE_PRUNE", "LOG_CLEAN", "NODE_CACHE_CLEAN");
    private static final List<String> DEFAULT_K8S_MAINTENANCE_ACTIONS = List.of("IMAGE_PRUNE", "LOG_CLEAN", "NODE_CACHE_CLEAN");
    private static final List<String> DEFAULT_K8S_ABNORMAL_STATUSES = List.of(
            "Failed", "Evicted", "Error", "Completed", "ImagePullBackOff", "ErrImagePull",
            "CrashLoopBackOff", "CreateContainerConfigError"
    );
    private static final String K8S_ABNORMAL_POD_CONFIRM_TEXT = "CLEAN_ABNORMAL_PODS";
    private static final String K8S_CLUSTER_MAINTENANCE_CONFIRM_TEXT = "CLEAN_CLUSTER";
    private static final String K8S_MAINTENANCE_JOB_HANDLER = "rkK8sMaintenanceJobHandler";
    private static final String K8S_MAINTENANCE_JOB_DESC = "RK-Web K8s half-month maintenance";
    private static final String K8S_MAINTENANCE_CRON = "0 0 3 1,16 * ?";
    private static final String FRONTEND_CACHE_WARMUP_JOB_HANDLER = "rkFrontendCacheWarmup";
    private static final String FRONTEND_CACHE_WARMUP_JOB_DESC = "RK-Web frontend cache hourly warmup";
    private static final String FRONTEND_CACHE_WARMUP_CRON = "0 0 * * * ?";
    private static final List<String> SUPPORTED_K8S_WORKLOAD_ACTIONS = List.of("RESTART", "SCALE");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String TRAFFIC_DEFAULT_TENANT_KEY = "admin.traffic.defaultTenantId";
    private static final String RAINBOND_CONFIG_KEY = "admin.rainbond.config";
    private static final String FRONTEND_CACHE_KEY_PATTERN = "rk:frontend-cache:*";
    private static final Duration FRONTEND_CACHE_WARMUP_TTL = Duration.ofMinutes(30);
    private static final List<String> publicFrontendWarmupPaths = List.of(
            "/api/news/latest",
            "/api/news/top",
            "/api/news/shared",
            "/api/news/statistics",
            "/api/works/featured",
            "/api/works/latest",
            "/api/works/popular",
            "/api/works/statistics",
            "/api/activity/hot",
            "/api/activity/top",
            "/api/activity/statistics",
            "/api/competition/published",
            "/api/competition/featured",
            "/notifications/api/notices/published",
            "/api/history/important",
            "/api/history/statistics",
            "/api/alumni/overview",
            "/api/alumni/statistics",
            "/tenants/list"
    );
    private static final List<String> SUPPORTED_RAINBOND_METHODS = List.of("GET", "POST", "PUT", "DELETE", "PATCH");
    private static final String DEPLOY_PACKAGE_MODE_BOOTSTRAP = "bootstrap";
    private static final String DEPLOY_PACKAGE_MODE_OFFLINE_FULL = "offline-full";
    private static final String DEPLOY_PACKAGE_DELIVERY_DIRECT = "direct-download";
    private static final String DEPLOY_PACKAGE_DELIVERY_STORE = "store-minio";
    private static final String IMAGE_ARTIFACT_OFFLINE_TAR = "offline-tar";
    private static final String IMAGE_ARTIFACT_REGISTRY = "registry";
    private static final String IMAGE_ARTIFACT_BOTH = "both";
    private static final String REMOTE_DEPLOY_CONFIRM_TEXT = "REMOTE_DEPLOY";
    private static final String TARGET_CLUSTER_ACK = "ack";
    private static final String TARGET_CLUSTER_K8S = "k8s";
    private static final String TARGET_CLUSTER_K3S = "k3s";
    private static final String TARGET_CLUSTER_CUSTOM = "custom";
    private static final String EXPOSURE_NONE = "none";
    private static final String EXPOSURE_NODE_PORT = "nodePort";
    private static final String EXPOSURE_LOAD_BALANCER = "loadBalancer";
    private static final String EXPOSURE_INGRESS = "ingress";
    private static final String ACK_DEFAULT_STORAGE_CLASS = "alicloud-disk-essd";
    private static final String K3S_DEFAULT_STORAGE_CLASS = "local-path";
    private static final int DEFAULT_FRONTEND_NODE_PORT = 30080;
    private static final int DEFAULT_GATEWAY_NODE_PORT = 30010;
    private static final String REMOTE_MYSQL_IMPORT_JOB = "rk-migration-mysql-import";
    private static final String REMOTE_MINIO_IMPORT_JOB = "rk-migration-minio-import";
    private static final String PLATFORM_GOGS_SOURCE_POD = "rk-server-rk-gogs-0";
    private static final String PLATFORM_JENKINS_SOURCE_POD = "rk-server-rk-jenkins-0";
    private static final String PLATFORM_GOGS_ARCHIVE = "platform/rk-gogs/data.tar.gz";
    private static final String PLATFORM_JENKINS_ARCHIVE = "platform/rk-jenkins/jenkins-home.tar.gz";
    private static final String REMOTE_MIGRATION_PAYLOAD_PVC = "rk-migration-payload";
    private static final String REMOTE_MIGRATION_STAGING_POD = "rk-migration-payload-staging";
    private static final int REMOTE_DEPLOY_REGISTRY_MIN_FREE_MB = 8192;
    private static final String DEFAULT_REGISTRY_PREFIX = "registry.example.com/rk-web";
    private static final String DEFAULT_REGISTRY_SERVER = "registry.example.com";
    private static final String DEFAULT_SENTINEL_DASHBOARD_IMAGE = "swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/bladex/sentinel-dashboard:1.8.6";
    private static final String DEFAULT_MINIO_CLIENT_IMAGE = "m.daocloud.io/docker.io/minio/mc:RELEASE.2024-07-11T18-01-28Z";
    private static final String REMOTE_MYSQL_IMAGE = "m.daocloud.io/docker.io/library/mysql:8.0";
    private static final String REMOTE_REDIS_IMAGE = "m.daocloud.io/docker.io/library/redis:7-alpine";
    private static final String REMOTE_RABBITMQ_IMAGE = "shetuanguanlixitong-rk-server-rk-rabbitmq:20260508221658";
    private static final String RABBITMQ_DELAYED_PLUGIN_FILE = "rabbitmq_delayed_message_exchange-3.8.17.8f537ac.ez";
    private static final String RABBITMQ_DELAYED_PLUGIN_URL = "https://github.com/rabbitmq/rabbitmq-delayed-message-exchange/releases/download/3.8.17/rabbitmq_delayed_message_exchange-3.8.17.8f537ac.ez";
    private static final String RABBITMQ_DELAYED_PLUGIN_MIRROR_URL = RABBITMQ_DELAYED_PLUGIN_URL;
    private static final String RABBITMQ_DELAYED_EXCHANGE_ARGUMENTS = "arguments='{\"x-delayed-type\":\"topic\"}'";
    private static final String REMOTE_MINIO_IMAGE = "m.daocloud.io/docker.io/minio/minio:RELEASE.2024-05-10T01-41-38Z";
    private static final String REMOTE_NACOS_IMAGE = "registry.example.com/rk-web/nacos-server:v2.1.0-slim";
    private static final String REMOTE_BUSYBOX_IMAGE = "m.daocloud.io/docker.io/library/busybox:1.36";
    private static final String REMOTE_GOGS_IMAGE = "m.daocloud.io/docker.io/gogs/gogs:0.12";
    private static final String REMOTE_JENKINS_IMAGE = "m.daocloud.io/docker.io/jenkins/jenkins:2.361.1-lts-jdk11";
    private static final String REMOTE_NACOS_CONFIG_CHECK_JOB = "rk-nacos-config-check";
    private static final String REMOTE_MINIO_POLICY_REPAIR_JOB = "rk-repair-minio-policy";
    private static final int LINUX_SSH_CONNECT_MAX_ATTEMPTS = 4;
    private static final long LINUX_SSH_CONNECT_RETRY_DELAY_MS = 8000L;
    private static final int LINUX_SSH_PACKAGE_UPLOAD_CHUNK_BYTES = 1024 * 1024;
    private static final int LINUX_SSH_PACKAGE_UPLOAD_CHUNK_RETRY_ATTEMPTS = 4;
    private static final int LINUX_SSH_PACKAGE_UPLOAD_SFTP_RETRY_ATTEMPTS = 6;
    private static final long LINUX_SSH_PACKAGE_DOWNLOAD_MIN_BYTES_PER_SECOND = 1024L * 1024L;
    private static final int LINUX_SSH_PACKAGE_DOWNLOAD_LOW_SPEED_SECONDS = 60;
    private static final int LINUX_SSH_PACKAGE_DOWNLOAD_MIN_TIMEOUT_SECONDS = 180;
    private static final int LINUX_SSH_PACKAGE_DOWNLOAD_MAX_TIMEOUT_SECONDS = 7200;
    private static final Duration LINUX_SSH_PACKAGE_DOWNLOAD_TOKEN_TTL = Duration.ofHours(6);
    private static final String GLOBAL_MIGRATION_LOCK_KEY = "rk:ops:global-migration:active";
    private static final String LINUX_SSH_PACKAGE_DOWNLOAD_TOKEN_KEY_PREFIX = "rk:ops:linux-ssh-package-download:";
    private static final Duration GLOBAL_MIGRATION_LOCK_TTL = Duration.ofMinutes(30);
    private static final String REMOTE_MINIO_ACCESS_KEY = "xiaorui";
    private static final String REMOTE_MINIO_SECRET_KEY = "change-me";
    private static final String REMOTE_DEPLOY_REPAIR_CONFIRM_TEXT = "REPAIR_REMOTE_DEPLOY";
    private static final String REPAIR_RESTART_WORKLOADS = "REPAIR_RESTART_WORKLOADS";
    private static final String REPAIR_DELETE_STUCK_PODS = "REPAIR_DELETE_STUCK_PODS";
    private static final String REPAIR_MINIO_PUBLIC_POLICY = "REPAIR_MINIO_PUBLIC_POLICY";
    private static final String REPAIR_RERUN_MYSQL_IMPORT = "REPAIR_RERUN_MYSQL_IMPORT";
    private static final String REPAIR_RERUN_MINIO_IMPORT = "REPAIR_RERUN_MINIO_IMPORT";
    private static final List<String> SUPPORTED_REMOTE_REPAIR_ACTIONS = List.of(
            REPAIR_RESTART_WORKLOADS,
            REPAIR_DELETE_STUCK_PODS,
            REPAIR_MINIO_PUBLIC_POLICY,
            REPAIR_RERUN_MYSQL_IMPORT,
            REPAIR_RERUN_MINIO_IMPORT
    );
    private static final List<String> K8S_PERSISTENT_MIDDLEWARE = List.of(
            "mysql", "redis", "rabbitmq", "minio", "nacos", "elasticsearch", "rk-gogs", "rk-jenkins"
    );
    private static final List<String> REMOTE_K8S_DEPLOYMENTS = List.of(
            "seata", "xxl-job", "sentinel", "rk-gogs", "rk-jenkins",
            "rk-gateway", "rk-auth", "rk-user", "rk-search", "rk-file", "rk-message",
            "rk-content", "rk-pay", "rk-trade", "rk-exam", "rk-activity", "rk-data",
            "rk-web-frontend"
    );
    private static final long MAX_RUNTIME_LOG_ARCHIVE_BYTES = 16L * 1024L * 1024L;
    private static final int DEPLOY_PACKAGE_RUNTIME_EXPORT_MAX_DISK_USED_PERCENT = 80;
    private static final int DEPLOY_PACKAGE_RUNTIME_EXPORT_HEADROOM_PERCENT = 5;
    private static final Map<String, String> SERVICE_IMAGE_MAP = Map.ofEntries(
            Map.entry("rk-gateway", "rk-gateway"),
            Map.entry("rk-auth", "rk-auth"),
            Map.entry("rk-user", "rk-user"),
            Map.entry("rk-search", "rk-search"),
            Map.entry("rk-file", "rk-file"),
            Map.entry("rk-message", "rk-message"),
            Map.entry("rk-content", "rk-content"),
            Map.entry("rk-pay", "rk-pay"),
            Map.entry("rk-trade", "rk-trade"),
            Map.entry("rk-exam", "rk-exam"),
            Map.entry("rk-activity", "rk-activity"),
            Map.entry("rk-data", "rk-data"),
            Map.entry("frontend", "rk-web-frontend"),
            Map.entry("rk-web-frontend", "rk-web-frontend")
    );

    private final JdbcTemplate jdbcTemplate;
    private final AdminBackupArchiveBuilder backupArchiveBuilder;
    private final AdminBackupStorageService backupStorageService;
    private final MinioClient minioClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, TrafficResolvedLocation> trafficGeoCache = new ConcurrentHashMap<>();

    @Value("${rk.ops.xxl.admin-url:http://xxl-job:8880/xxl-job-admin}")
    private String xxlAdminUrl;

    @Value("${rk.ops.xxl.username:admin}")
    private String xxlUsername;

    @Value("${rk.ops.xxl.password:123456}")
    private String xxlPassword;

    @Value("${rk.ops.nacos.addr:http://nacos:8848}")
    private String nacosAddr;

    @Value("${rk.ops.nacos.namespace:386079bc-a0ed-4144-9c01-d559e2d5c207}")
    private String nacosNamespace;

    @Value("${rk.ops.nacos.group:DEFAULT_GROUP}")
    private String nacosGroup;

    @Value("${rk.ops.remote.python:C:/ProgramData/miniconda3/python.exe}")
    private String remotePython;

    @Value("${rk.ops.remote.script:C:/Users/Administrator/IdeaProjects/RK-Web/remote_ssh.py}")
    private String remoteSshScript;

    @Value("${rk.ops.deploy.kubeconfig-secret:rk-web-default-kubeconfig-secret-change-me}")
    private String deployKubeconfigSecret;

    @Value("${rk.ops.k8s.api-url:}")
    private String k8sApiUrl;

    @Value("${rk.ops.k8s.token-path:/var/run/secrets/kubernetes.io/serviceaccount/token}")
    private String k8sTokenPath;

    @Value("${rk.ops.k8s.ca-path:/var/run/secrets/kubernetes.io/serviceaccount/ca.crt}")
    private String k8sCaPath;

    @Value("${rk.ops.k8s.cleanup-image:m.daocloud.io/docker.io/library/busybox:1.36}")
    private String k8sCleanupImage;

    @Value("${rk.ops.k8s.cleanup-namespace:shetuanguanlixitong}")
    private String k8sCleanupNamespace;

    @Value("${rk.ops.deploy-package.runtime-export-timeout-seconds:1800}")
    private Integer deployPackageRuntimeExportTimeoutSeconds;

    @Value("${rk.ops.deploy-package.runtime-export-max-disk-used-percent:80}")
    private Integer deployPackageRuntimeExportMaxDiskUsedPercent;

    @Value("${rk.ops.deploy-package.runtime-export-headroom-percent:5}")
    private Integer deployPackageRuntimeExportHeadroomPercent;

    @Value("${rk.ops.deploy-package.registry-username:${RK_REGISTRY_USER:}}")
    private String deployPackageRegistryUsername;

    @Value("${rk.ops.deploy-package.registry-password:${RK_REGISTRY_PASSWORD:}}")
    private String deployPackageRegistryPassword;

    @Value("${rk.ops.deploy-package.registry-image-pull-secret-name:${RK_IMAGE_PULL_SECRET_NAME:rk-aliyun-regcred}}")
    private String deployPackageRegistryImagePullSecretName;

    @Value("${rk.ops.deploy-package.public-base-url:${rk.web.public-base-url:}}")
    private String deployPackagePublicBaseUrl;

    @Value("${rk.ops.deploy-package.k8s-storage-class:${RK_K8S_STORAGE_CLASS:alicloud-disk-essd}}")
    private String deployPackageK8sStorageClassName;

    @Value("${rk.ops.jenkins.url:http://rk-jenkins:8080}")
    private String jenkinsUrl;

    @Value("${rk.ops.jenkins.username:root}")
    private String jenkinsUsername;

    @Value("${rk.ops.jenkins.password:123}")
    private String jenkinsPassword;

    @Value("${rk.ops.jenkins.git.default-source:local}")
    private String jenkinsGitDefaultSource;

    @Value("${rk.ops.jenkins.git.default-branch:cloud-master-new}")
    private String jenkinsGitDefaultBranch;

    @Value("${rk.ops.jenkins.git.fallback-branches:cloud-master-new}")
    private String jenkinsGitFallbackBranches;

    @Value("${rk.ops.jenkins.git.local-url:http://rk-gogs:3000/tjxt/rk-web.git}")
    private String jenkinsGitLocalUrl;

    @Value("${rk.ops.jenkins.git.local-branch-url:http://rk-gogs:3000/tjxt/rk-web.git}")
    private String jenkinsGitLocalBranchUrl;

    @Value("${rk.ops.jenkins.git.gitee-url:https://github.com/example/rk-web}")
    private String jenkinsGitGiteeUrl;

    @Value("${rk.ops.jenkins.git.local-username:}")
    private String jenkinsGitLocalUsername;

    @Value("${rk.ops.jenkins.git.local-password:}")
    private String jenkinsGitLocalPassword;

    @Value("${rk.ops.jenkins.git.gitee-username:}")
    private String jenkinsGitGiteeUsername;

    @Value("${rk.ops.jenkins.git.gitee-password:}")
    private String jenkinsGitGiteePassword;

    @Value("${rk.ops.gateway.url:http://rk-gateway:10010}")
    private String gatewayUrl;

    @Value("${rk.ops.rainbond.base-url:}")
    private String rainbondBaseUrl;

    @Value("${rk.ops.rainbond.token:}")
    private String rainbondToken;

    @Value("${rk.ops.rainbond.enterprise-id:}")
    private String rainbondEnterpriseId;

    @Value("${rk.ops.rainbond.team-id:}")
    private String rainbondTeamId;

    @Value("${rk.ops.rainbond.region-name:}")
    private String rainbondRegionName;

    @Value("${rk.ops.rainbond.timeout-seconds:15}")
    private Integer rainbondTimeoutSeconds;

    private Map<String, String> workbenchDocServiceBaseUrls = new HashMap<>(Map.ofEntries(
            Map.entry("users", "http://rk-server-rk-user:8082"),
            Map.entry("auth", "http://rk-server-rk-auth:8081"),
            Map.entry("activities", "http://rk-server-rk-activity:8090"),
            Map.entry("content", "http://rk-server-rk-content:8086"),
            Map.entry("files", "http://rk-server-rk-file:8084"),
            Map.entry("notifications", "http://rk-server-rk-message:8085"),
            Map.entry("exam", "http://rk-server-rk-exam:8089"),
            Map.entry("pay", "http://rk-server-rk-pay:8087"),
            Map.entry("search", "http://rk-server-rk-search:8083"),
            Map.entry("trade", "http://rk-server-rk-trade:8088"),
            Map.entry("data", "http://rk-server-rk-data:8093")
    ));

    @Override
    public AdminMonitoringOverviewVO getMonitoringOverview() {
        AdminMonitoringOverviewVO overview = new AdminMonitoringOverviewVO();
        overview.setSystemStatus(buildSystemStatus());
        overview.setServices(buildServiceStatus());
        overview.setDatabases(buildDatabaseStatus());
        return overview;
    }

    @Override
    public AdminMonitoringLogVO getMonitoringLogs(String namespace, String podName, String containerName, Integer tailLines) {
        requireTenantOneOpsAccess();
        String resolvedNamespace = sanitizeCleanupToken(namespace, "命名空间");
        String resolvedPodName = sanitizeCleanupToken(podName, "Pod 名称");
        String resolvedContainerName = sanitizeCleanupToken(containerName, "容器名称");
        int resolvedTailLines = tailLines == null ? 200 : Math.max(20, Math.min(tailLines, 1000));

        AdminMonitoringLogVO result = new AdminMonitoringLogVO();
        result.setNamespace(resolvedNamespace);
        result.setPodName(resolvedPodName);
        result.setContainerName(resolvedContainerName);
        result.setTailLines(resolvedTailLines);

        String token = readK8sServiceAccountToken();
        String apiUrl = resolveK8sApiUrl();
        if (!StringUtils.hasText(token) || !StringUtils.hasText(apiUrl)) {
            result.setMessage("当前服务未检测到 Kubernetes ServiceAccount，无法读取容器日志");
            result.setLogs("");
            return result;
        }

        try {
            HttpClient client = buildK8sHttpClient(apiUrl);
            result.setLogs(readK8sPodLogs(client, apiUrl, token, resolvedNamespace, resolvedPodName, resolvedContainerName, resolvedTailLines));
            result.setMessage("读取成功");
            return result;
        } catch (Exception e) {
            log.warn("read monitoring pod logs failed, namespace={}, pod={}, container={}",
                    resolvedNamespace, resolvedPodName, resolvedContainerName, e);
            result.setMessage("读取日志失败: " + e.getMessage());
            result.setLogs("");
            return result;
        }
    }

    @Override
    public AdminTaskOverviewVO getTaskOverview() {
        try {
            Map<Long, String> groupMap = loadTaskGroupMap();
            JsonNode tasksRoot = invokeXxlForm("jobinfo/pageList", Map.of(
                    "start", "0",
                    "length", "50",
                    "jobGroup", "0",
                    "triggerStatus", "-1",
                    "jobDesc", "",
                    "executorHandler", "",
                    "author", ""
            ));
            JsonNode logsRoot = invokeXxlForm("joblog/pageList", Map.of(
                    "start", "0",
                    "length", "20",
                    "jobGroup", "0",
                    "jobId", "0",
                    "logStatus", "-1",
                    "filterTime", ""
            ));
            AdminTaskOverviewVO overview = new AdminTaskOverviewVO();
            overview.setSourceUrl(xxlAdminUrl);
            overview.setTasks(mapTaskRows(tasksRoot.path("data"), groupMap));
            overview.setLogs(mapLogRows(logsRoot.path("data"), overview.getTasks()));
            return overview;
        } catch (Exception e) {
            log.error("load task overview failed", e);
            throw new IllegalStateException("获取定时任务数据失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Boolean pauseTask(Long taskId) {
        return invokeXxlAction("jobinfo/stop", Map.of("id", String.valueOf(taskId)));
    }

    @Override
    public Boolean resumeTask(Long taskId) {
        return invokeXxlAction("jobinfo/start", Map.of("id", String.valueOf(taskId)));
    }

    @Override
    public Boolean triggerTask(Long taskId) {
        return invokeXxlAction("jobinfo/trigger", Map.of(
                "id", String.valueOf(taskId),
                "executorParam", "",
                "addressList", ""
        ));
    }

    @Override
    public AdminJenkinsOverviewVO getJenkinsOverview() {
        requireTenantOneOpsAccess();
        try {
            HttpClient client = HttpClient.newBuilder().build();
            HttpResponse<String> response = sendJenkinsRequest(
                    client,
                    "/api/json?tree=jobs[name,url,color,lastBuild[number,url],lastCompletedBuild[number,url]]",
                    "GET",
                    Map.of(),
                    null
            );
            JsonNode root = objectMapper.readTree(response.body());
            AdminJenkinsOverviewVO overview = new AdminJenkinsOverviewVO();
            overview.setSourceUrl(sanitizeOpsUrl(jenkinsUrl));
            overview.setJobs(mapJenkinsJobs(root.path("jobs")));
            return overview;
        } catch (Exception e) {
            log.error("load jenkins overview failed", e);
            throw new IllegalStateException("获取 Jenkins 任务失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<AdminJenkinsGitSourceVO> getJenkinsGitSources() {
        requireTenantOneOpsAccess();
        String defaultSource = normalizeJenkinsGitSource(jenkinsGitDefaultSource);
        return List.of(
                buildJenkinsGitSource("local", defaultSource),
                buildJenkinsGitSource("gitee", defaultSource)
        );
    }

    @Override
    public AdminJenkinsGitBranchesVO getJenkinsGitBranches(String source) {
        requireTenantOneOpsAccess();
        String normalizedSource = normalizeJenkinsGitSource(source);
        String repoUrl = resolveJenkinsGitBranchRepoUrl(normalizedSource);
        AdminJenkinsGitBranchesVO result = new AdminJenkinsGitBranchesVO();
        result.setSource(normalizedSource);
        result.setLabel(resolveJenkinsGitSourceLabel(normalizedSource));
        result.setRepositoryUrl(maskRepositoryUrl(repoUrl));
        result.setFallback(Boolean.FALSE);
        try {
            List<String> branches = readGitBranchesFromSmartHttp(
                    repoUrl,
                    resolveJenkinsGitUsername(normalizedSource),
                    resolveJenkinsGitPassword(normalizedSource)
            );
            result.setBranches(branches);
            result.setMessage("已从 " + result.getLabel() + " 读取分支");
            return result;
        } catch (Exception smartHttpError) {
            log.warn("read git branches through smart http failed, source={}, repo={}", normalizedSource, maskRepositoryUrl(repoUrl), smartHttpError);
            try {
                List<String> branches = readGitBranchesFromProcess(
                        repoUrl,
                        resolveJenkinsGitUsername(normalizedSource),
                        resolveJenkinsGitPassword(normalizedSource)
                );
                result.setBranches(branches);
                result.setMessage("已通过 git ls-remote 读取分支");
                return result;
            } catch (Exception processError) {
                log.error("read git branches failed, source={}, repo={}", normalizedSource, maskRepositoryUrl(repoUrl), processError);
                List<String> fallbackBranches = resolveJenkinsFallbackBranches();
                if (!fallbackBranches.isEmpty()) {
                    String failureReason = processError.getMessage();
                    result.setBranches(fallbackBranches);
                    result.setFallback(Boolean.TRUE);
                    result.setFailureReason(failureReason);
                    result.setMessage("远程分支读取失败，已返回默认分支；可以手动输入目标分支。原因: " + failureReason);
                    return result;
                }
                throw new IllegalStateException("读取 " + resolveJenkinsGitSourceLabel(normalizedSource) + " 分支失败: " + processError.getMessage(), processError);
            }
        }
    }

    public Boolean triggerJenkinsJob(String jobName) {
        return Boolean.TRUE.equals(triggerJenkinsJob(jobName, new AdminJenkinsBuildRequestDTO()).getAccepted());
    }

    @Override
    public AdminJenkinsBuildTriggerVO triggerJenkinsJob(String jobName, AdminJenkinsBuildRequestDTO dto) {
        requireTenantOneOpsAccess();
        AdminJenkinsBuildRequestDTO normalized = normalizeJenkinsBuildRequest(dto);
        if (!StringUtils.hasText(jobName)) {
            throw new IllegalArgumentException("Jenkins 任务名不能为空");
        }
        String preflightSummary = captureJenkinsPreflightSnapshot();
        try {
            CookieManager cookieManager = new CookieManager();
            HttpClient client = HttpClient.newBuilder().cookieHandler(cookieManager).build();
            Map<String, String> crumbHeaders = loadJenkinsCrumbHeaders(client);
            Map<String, String> form = new LinkedHashMap<>();
            form.put("TARGET_BRANCH", normalized.getTargetBranch());
            form.put("GIT_SOURCE", normalized.getGitSource());
            form.put("SERVICES", normalized.getServices());
            form.put("RK_K8S_IMAGE_MODE", normalized.getImageMode());
            form.put("DEPLOY_JOB_SUFFIX", normalized.getDeployJobSuffix());
            form.put("RK_REMOTE_HOST", normalized.getRemoteHost());
            form.put("RK_REMOTE_PORT", normalized.getRemotePort());
            form.put("RK_REMOTE_USER", normalized.getRemoteUser());
            form.put("RK_REMOTE_PASSWORD", normalized.getRemotePassword());
            HttpResponse<String> response = sendJenkinsRequest(
                    client,
                    "/job/" + encodePathSegment(jobName) + "/buildWithParameters",
                    "POST",
                    withContentType(crumbHeaders, "application/x-www-form-urlencoded; charset=UTF-8"),
                    encodeForm(form)
            );
            int statusCode = response.statusCode();
            if (statusCode != 200 && statusCode != 201 && statusCode != 202 && statusCode != 302) {
                log.warn("trigger jenkins job returned non-success status, jobName={}, status={}, body={}",
                        jobName, statusCode, response.body());
            }
            AdminJenkinsBuildTriggerVO result = new AdminJenkinsBuildTriggerVO();
            result.setAccepted(statusCode == 200 || statusCode == 201 || statusCode == 202 || statusCode == 302);
            result.setStatusCode(statusCode);
            result.setJobName(jobName);
            result.setTargetBranch(normalized.getTargetBranch());
            result.setGitSource(normalized.getGitSource());
            result.setGitSourceLabel(resolveJenkinsGitSourceLabel(normalized.getGitSource()));
            result.setServices(normalized.getServices());
            result.setImageMode(normalized.getImageMode());
            result.setImageModeLabel(resolveJenkinsImageModeLabel(normalized.getImageMode()));
            result.setPreflightSummary(preflightSummary);
            String queueUrl = response.headers().firstValue("Location").orElse("");
            result.setQueueUrl(queueUrl);
            result.setQueueId(extractJenkinsQueueId(queueUrl));
            result.setMessage(result.getAccepted() ? "Jenkins 构建已进入队列" : "Jenkins 返回非成功状态: " + statusCode);
            result.setRecordId(insertJenkinsBuildRecord(
                    jobName,
                    normalized,
                    result.getQueueId(),
                    result.getAccepted() ? "queued" : "failed",
                    result.getAccepted() ? "QUEUED" : "TRIGGER",
                    result.getAccepted() ? null : result.getMessage(),
                    "",
                    preflightSummary
            ));
            return result;
        } catch (Exception e) {
            log.error("trigger jenkins job failed, jobName={}", jobName, e);
            insertJenkinsBuildRecord(jobName, normalized, "", "failed", "TRIGGER", e.getMessage(), "", preflightSummary);
            throw new IllegalStateException("触发 Jenkins 任务失败: " + e.getMessage(), e);
        }
    }

    @Override
    public AdminJenkinsBuildStatusVO getJenkinsBuildStatus(String jobName, String queueId, Integer buildNumber, Integer tailLines) {
        requireTenantOneOpsAccess();
        if (!StringUtils.hasText(jobName)) {
            throw new IllegalArgumentException("Jenkins 任务名不能为空");
        }
        if (!StringUtils.hasText(queueId) && buildNumber == null) {
            throw new BadRequestException("queueId 或 buildNumber 至少传一个");
        }
        try {
            HttpClient client = HttpClient.newBuilder().build();
            Integer resolvedBuildNumber = buildNumber;
            AdminJenkinsBuildStatusVO result = new AdminJenkinsBuildStatusVO();
            result.setJobName(jobName);
            result.setQueueId(queueId);
            if (resolvedBuildNumber == null && StringUtils.hasText(queueId)) {
                JsonNode queueNode = readJenkinsJson(client, "/queue/item/" + encodePathSegment(queueId) + "/api/json");
                if (queueNode.path("cancelled").asBoolean(false)) {
                    result.setStatus("canceled");
                    result.setBuilding(false);
                    result.setMessage("Jenkins 队列任务已取消");
                    persistJenkinsBuildStatus(result);
                    return result;
                }
                JsonNode executable = queueNode.path("executable");
                if (executable.isMissingNode() || executable.isNull()) {
                    result.setStatus("queued");
                    result.setBuilding(true);
                    result.setMessage(StringUtils.hasText(queueNode.path("why").asText(""))
                            ? queueNode.path("why").asText("")
                            : "Jenkins 构建排队中");
                    persistJenkinsBuildStatus(result);
                    return result;
                }
                resolvedBuildNumber = executable.path("number").asInt();
                result.setBuildUrl(executable.path("url").asText(""));
            }
            if ((resolvedBuildNumber == null || resolvedBuildNumber <= 0) && StringUtils.hasText(queueId)) {
                resolvedBuildNumber = findJenkinsBuildNumberByQueueId(jobName, queueId, client);
                if (resolvedBuildNumber != null && resolvedBuildNumber > 0) {
                    result.setBuildUrl("/job/" + encodePathSegment(jobName) + "/" + resolvedBuildNumber + "/");
                }
            }
            fillJenkinsBuildDetail(client, result, jobName, resolvedBuildNumber, tailLines == null ? 120 : tailLines);
            persistJenkinsBuildStatus(result);
            return result;
        } catch (JenkinsHttpStatusException e) {
            if (e.getStatusCode() == 404) {
                Integer storedBuildNumber = findJenkinsBuildNumberByQueueId(jobName, queueId, null);
                if (storedBuildNumber != null) {
                    try {
                        AdminJenkinsBuildStatusVO recovered = new AdminJenkinsBuildStatusVO();
                        recovered.setJobName(jobName);
                        recovered.setQueueId(queueId);
                        fillJenkinsBuildDetail(HttpClient.newBuilder().build(), recovered, jobName, storedBuildNumber, tailLines == null ? 120 : tailLines);
                        persistJenkinsBuildStatus(recovered);
                        return recovered;
                    } catch (JenkinsHttpStatusException buildMissing) {
                        AdminJenkinsBuildStatusVO missing = buildMissingJenkinsStatus(jobName, queueId, storedBuildNumber, buildMissing);
                        persistJenkinsBuildStatus(missing);
                        return missing;
                    } catch (Exception recoverError) {
                        log.error("recover jenkins build status from stored build number failed, jobName={}, queueId={}, buildNumber={}",
                                jobName, queueId, storedBuildNumber, recoverError);
                        throw new IllegalStateException("获取 Jenkins 构建进度失败: " + recoverError.getMessage(), recoverError);
                    }
                }
                AdminJenkinsBuildStatusVO missing = buildMissingJenkinsStatus(jobName, queueId, buildNumber, e);
                persistJenkinsBuildStatus(missing);
                return missing;
            }
            log.error("load jenkins build status failed, jobName={}, queueId={}, buildNumber={}", jobName, queueId, buildNumber, e);
            throw new IllegalStateException("获取 Jenkins 构建进度失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("load jenkins build status failed, jobName={}, queueId={}, buildNumber={}", jobName, queueId, buildNumber, e);
            throw new IllegalStateException("获取 Jenkins 构建进度失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<AdminJenkinsBuildRecordVO> getJenkinsBuildHistory(String serviceCode, String branchName, String gitSource,
                                                                  String status, String operatorKeyword, String startTime,
                                                                  String endTime) {
        requireTenantOneOpsAccess();
        reconcileRecentJenkinsBuildRecords();
        StringBuilder sql = new StringBuilder(
                "SELECT id, job_name, build_number, build_url, queue_id, git_source, git_repo, branch_name, commit_id, image_mode, services_text, status, stage, result, failure_reason, " +
                        "trigger_user_id, trigger_user_name, trigger_time, start_time, finish_time, duration_ms, log_tail, preflight_summary " +
                        "FROM ops_build_record WHERE is_deleted = 0"
        );
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(serviceCode)) {
            sql.append(" AND EXISTS (SELECT 1 FROM ops_build_service s WHERE s.record_id = ops_build_record.id AND s.is_deleted = 0 AND s.service_code = ?)");
            args.add(serviceCode.trim());
        }
        if (StringUtils.hasText(branchName)) {
            sql.append(" AND branch_name = ?");
            args.add(branchName.trim());
        }
        if (StringUtils.hasText(gitSource)) {
            sql.append(" AND git_source = ?");
            args.add(normalizeJenkinsGitSource(gitSource));
        }
        if (StringUtils.hasText(status)) {
            sql.append(" AND status = ?");
            args.add(status.trim().toLowerCase());
        }
        if (StringUtils.hasText(operatorKeyword)) {
            sql.append(" AND (trigger_user_name LIKE ? OR CAST(trigger_user_id AS CHAR) LIKE ?)");
            String like = "%" + operatorKeyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        if (StringUtils.hasText(startTime)) {
            sql.append(" AND trigger_time >= ?");
            args.add(startTime.trim());
        }
        if (StringUtils.hasText(endTime)) {
            sql.append(" AND trigger_time <= ?");
            args.add(endTime.trim());
        }
        sql.append(" ORDER BY trigger_time DESC, id DESC LIMIT 100");
        return jdbcTemplate.queryForList(sql.toString(), args.toArray()).stream()
                .map(this::mapBuildRecordRow)
                .collect(Collectors.toList());
    }

    @Override
    public List<AdminOpsServiceVO> listServiceRegistry() {
        requireTenantOneOpsAccess();
        return jdbcTemplate.queryForList(
                        "SELECT id, service_code, display_name, service_type, git_source, git_repo, branch_name, module_path, build_mode, image_name, namespace_name, workload_type, workload_name, container_name, health_check_path, resource_limits, enabled, sort_order " +
                                "FROM ops_service_registry WHERE is_deleted = 0 ORDER BY enabled DESC, sort_order ASC, service_code ASC"
                ).stream()
                .map(this::mapServiceRegistryRow)
                .collect(Collectors.toList());
    }

    @Override
    public AdminOpsServiceVO saveServiceRegistry(AdminOpsServiceSaveDTO dto) {
        requireTenantOneOpsAccess();
        if (dto == null) {
            throw new BadRequestException("微服务参数不能为空");
        }
        String serviceCode = validateJenkinsToken(requireText(dto.getServiceCode(), "服务编码"), "serviceCode", "[A-Za-z0-9_.-]+");
        String displayName = requireText(dto.getDisplayName(), "服务名称");
        String serviceType = StringUtils.hasText(dto.getServiceType()) ? dto.getServiceType().trim() : "backend";
        String gitSource = normalizeJenkinsGitSource(dto.getGitSource());
        String gitRepo = StringUtils.hasText(dto.getGitRepo()) ? dto.getGitRepo().trim() : resolveJenkinsGitRepoUrl(gitSource);
        String branchName = validateJenkinsToken(StringUtils.hasText(dto.getBranchName()) ? dto.getBranchName().trim() : jenkinsGitDefaultBranch, "branchName", "[A-Za-z0-9._/-]+");
        String namespaceName = validateJenkinsToken(StringUtils.hasText(dto.getNamespaceName()) ? dto.getNamespaceName().trim() : k8sCleanupNamespace, "namespaceName", "[A-Za-z0-9_.:-]+");
        String workloadType = normalizeK8sWorkloadKind(dto.getWorkloadType());
        String workloadName = StringUtils.hasText(dto.getWorkloadName()) ? dto.getWorkloadName().trim() : "rk-server-" + serviceCode;
        String containerName = StringUtils.hasText(dto.getContainerName()) ? dto.getContainerName().trim() : serviceCode;
        int sortOrder = dto.getSortOrder() == null ? 100 : dto.getSortOrder();
        boolean enabled = !Boolean.FALSE.equals(dto.getEnabled());

        if (dto.getId() == null) {
            jdbcTemplate.update(
                    "INSERT INTO ops_service_registry (service_code, display_name, service_type, git_source, git_repo, branch_name, module_path, build_mode, image_name, namespace_name, workload_type, workload_name, container_name, health_check_path, resource_limits, enabled, sort_order) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    serviceCode,
                    displayName,
                    serviceType,
                    gitSource,
                    gitRepo,
                    branchName,
                    emptyToNull(dto.getModulePath()),
                    StringUtils.hasText(dto.getBuildMode()) ? dto.getBuildMode().trim() : "maven-docker",
                    emptyToNull(dto.getImageName()),
                    namespaceName,
                    workloadType,
                    workloadName,
                    containerName,
                    emptyToNull(dto.getHealthCheckPath()),
                    emptyToNull(dto.getResourceLimits()),
                    enabled ? 1 : 0,
                    sortOrder
            );
            Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            return findServiceRegistryById(id);
        }

        jdbcTemplate.update(
                "UPDATE ops_service_registry SET service_code = ?, display_name = ?, service_type = ?, git_source = ?, git_repo = ?, branch_name = ?, module_path = ?, build_mode = ?, image_name = ?, namespace_name = ?, workload_type = ?, workload_name = ?, container_name = ?, health_check_path = ?, resource_limits = ?, enabled = ?, sort_order = ? WHERE id = ? AND is_deleted = 0",
                serviceCode,
                displayName,
                serviceType,
                gitSource,
                gitRepo,
                branchName,
                emptyToNull(dto.getModulePath()),
                StringUtils.hasText(dto.getBuildMode()) ? dto.getBuildMode().trim() : "maven-docker",
                emptyToNull(dto.getImageName()),
                namespaceName,
                workloadType,
                workloadName,
                containerName,
                emptyToNull(dto.getHealthCheckPath()),
                emptyToNull(dto.getResourceLimits()),
                enabled ? 1 : 0,
                sortOrder,
                dto.getId()
        );
        return findServiceRegistryById(dto.getId());
    }

    @Override
    public Boolean deleteServiceRegistry(Long id) {
        requireTenantOneOpsAccess();
        if (id == null) {
            throw new BadRequestException("服务 ID 不能为空");
        }
        return jdbcTemplate.update("UPDATE ops_service_registry SET is_deleted = 1 WHERE id = ?", id) > 0;
    }

    @Override
    public List<AdminNacosConfigVO> listNacosConfigs(String namespaceId, String groupName) {
        requireTenantOneOpsAccess();
        String namespace = resolveNacosNamespace(namespaceId);
        String group = resolveNacosGroup(groupName);
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            String path = "/nacos/v1/cs/configs?search=blur&pageNo=1&pageSize=200&dataId=&group=" +
                    URLEncoder.encode(group, StandardCharsets.UTF_8) +
                    "&tenant=" + URLEncoder.encode(namespace, StandardCharsets.UTF_8);
            HttpResponse<String> response = sendNacosRequest(client, path, "GET", null);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Nacos 返回状态 " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode rows = root.path("pageItems");
            List<AdminNacosConfigVO> result = new ArrayList<>();
            if (rows.isArray()) {
                for (JsonNode row : rows) {
                    AdminNacosConfigVO item = new AdminNacosConfigVO();
                    item.setNamespaceId(namespace);
                    item.setGroupName(row.path("group").asText(group));
                    item.setDataId(row.path("dataId").asText(""));
                    item.setMd5(row.path("md5").asText(""));
                    item.setType(row.path("type").asText("yaml"));
                    item.setLastModified(row.path("lastModified").asText(""));
                    item.setMessage("已从 Nacos 读取");
                    result.add(item);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("list nacos configs failed", e);
            AdminNacosConfigVO item = new AdminNacosConfigVO();
            item.setNamespaceId(namespace);
            item.setGroupName(group);
            item.setDataId("");
            item.setType("yaml");
            item.setMessage("读取 Nacos 配置失败: " + e.getMessage());
            return List.of(item);
        }
    }

    @Override
    public AdminNacosConfigVO getNacosConfig(String namespaceId, String groupName, String dataId) {
        requireTenantOneOpsAccess();
        String namespace = resolveNacosNamespace(namespaceId);
        String group = resolveNacosGroup(groupName);
        String resolvedDataId = requireText(dataId, "dataId");
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            String path = "/nacos/v1/cs/configs?dataId=" + URLEncoder.encode(resolvedDataId, StandardCharsets.UTF_8) +
                    "&group=" + URLEncoder.encode(group, StandardCharsets.UTF_8) +
                    "&tenant=" + URLEncoder.encode(namespace, StandardCharsets.UTF_8);
            HttpResponse<String> response = sendNacosRequest(client, path, "GET", null);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Nacos 返回状态 " + response.statusCode());
            }
            AdminNacosConfigVO result = new AdminNacosConfigVO();
            result.setNamespaceId(namespace);
            result.setGroupName(group);
            result.setDataId(resolvedDataId);
            result.setType(resolveNacosConfigType(resolvedDataId));
            result.setContent(response.body());
            result.setMessage("读取成功");
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("读取 Nacos 配置失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<AdminNacosConfigHistoryVO> getNacosConfigHistory(String namespaceId, String groupName, String dataId) {
        requireTenantOneOpsAccess();
        String namespace = resolveNacosNamespace(namespaceId);
        String group = resolveNacosGroup(groupName);
        String resolvedDataId = requireText(dataId, "dataId");
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            String path = "/nacos/v1/cs/history?search=accurate&pageNo=1&pageSize=20&dataId=" +
                    URLEncoder.encode(resolvedDataId, StandardCharsets.UTF_8) +
                    "&group=" + URLEncoder.encode(group, StandardCharsets.UTF_8) +
                    "&tenant=" + URLEncoder.encode(namespace, StandardCharsets.UTF_8);
            HttpResponse<String> response = sendNacosRequest(client, path, "GET", null);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Nacos history status " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode rows = root.path("pageItems");
            if (!rows.isArray()) {
                rows = root.path("data").path("pageItems");
            }
            List<AdminNacosConfigHistoryVO> result = new ArrayList<>();
            if (rows.isArray()) {
                for (JsonNode row : rows) {
                    AdminNacosConfigHistoryVO item = new AdminNacosConfigHistoryVO();
                    item.setNamespaceId(namespace);
                    item.setGroupName(row.path("group").asText(group));
                    item.setDataId(row.path("dataId").asText(resolvedDataId));
                    item.setNid(row.path("nid").asText(row.path("id").asText("")));
                    item.setLastModifiedTime(row.path("lastModifiedTime").asText(row.path("lastModified").asText("")));
                    item.setOperator(row.path("srcUser").asText(row.path("operator").asText("")));
                    item.setContent(row.path("content").asText(""));
                    item.setMessage("loaded");
                    result.add(item);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("load nacos config history failed, dataId={}", resolvedDataId, e);
            AdminNacosConfigHistoryVO item = new AdminNacosConfigHistoryVO();
            item.setNamespaceId(namespace);
            item.setGroupName(group);
            item.setDataId(resolvedDataId);
            item.setMessage("load history failed: " + e.getMessage());
            return List.of(item);
        }
    }

    @Override
    public Boolean saveNacosConfig(AdminNacosConfigSaveDTO dto) {
        requireTenantOneOpsAccess();
        if (dto == null) {
            throw new BadRequestException("Nacos 配置不能为空");
        }
        String namespace = firstNonEmpty(resolveNacosNamespace(dto.getNamespaceId()), "");
        String group = firstNonEmpty(resolveNacosGroup(dto.getGroupName()), "DEFAULT_GROUP");
        String dataId = requireText(dto.getDataId(), "dataId");
        String content = dto.getContent() == null ? "" : dto.getContent();
        String type = StringUtils.hasText(dto.getType()) ? dto.getType().trim() : resolveNacosConfigType(dataId);
        boolean success = false;
        String output = "";
        try {
            validateNacosContent(type, content);
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            String payload = encodeForm(Map.of(
                    "tenant", namespace,
                    "group", group,
                    "dataId", dataId,
                    "content", content,
                    "type", type
            ));
            HttpResponse<String> response = sendNacosRequest(client, "/nacos/v1/cs/configs", "POST", payload);
            output = "status=" + response.statusCode() + ", body=" + response.body();
            success = response.statusCode() >= 200 && response.statusCode() < 300 && response.body().toLowerCase().contains("true");
            return success;
        } catch (BadRequestException e) {
            output = e.getMessage();
            throw e;
        } catch (Exception e) {
            output = e.getMessage();
            throw new IllegalStateException("保存 Nacos 配置失败: " + e.getMessage(), e);
        } finally {
            insertOperationAudit(
                    "NACOS_CONFIG",
                    namespace,
                    group,
                    dataId,
                    "SAVE",
                    success ? "success" : "failed",
                    null,
                    toJson(Map.of("namespaceId", namespace, "groupName", group, "dataId", dataId, "type", type)),
                    output
            );
        }
    }

    @Override
    public List<AdminOpsAuditLogVO> listOperationAuditLogs(String operationType, String targetName, String action,
                                                           String status, Integer limit) {
        requireTenantOneOpsAccess();
        StringBuilder sql = new StringBuilder(
                "SELECT id, operation_type, target_namespace, target_kind, target_name, action, status, reason, request_payload, result_output, operator_user_id, operator_user_name, create_time " +
                        "FROM ops_operation_audit WHERE 1 = 1"
        );
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(operationType)) {
            sql.append(" AND operation_type = ?");
            args.add(operationType.trim());
        }
        if (StringUtils.hasText(targetName)) {
            sql.append(" AND target_name LIKE ?");
            args.add("%" + targetName.trim() + "%");
        }
        if (StringUtils.hasText(action)) {
            sql.append(" AND action = ?");
            args.add(action.trim().toUpperCase());
        }
        if (StringUtils.hasText(status)) {
            sql.append(" AND status = ?");
            args.add(status.trim().toLowerCase());
        }
        sql.append(" ORDER BY create_time DESC, id DESC LIMIT ?");
        args.add(Math.max(1, Math.min(limit == null ? 30 : limit, 100)));
        return jdbcTemplate.queryForList(sql.toString(), args.toArray()).stream()
                .map(this::mapAuditLogRow)
                .collect(Collectors.toList());
    }

    @Override
    public List<AdminApiWorkbenchResourceVO> getApiWorkbenchResources() {
        requireTenantOneOpsAccess();
        try {
            HttpClient client = HttpClient.newBuilder().build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(sanitizeOpsUrl(gatewayUrl) + "/swagger-resources"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode root = objectMapper.readTree(response.body());
            if (!root.isArray()) {
                return Collections.emptyList();
            }
            List<AdminApiWorkbenchResourceVO> resources = new ArrayList<>();
            for (JsonNode item : root) {
                String name = item.path("name").asText("");
                String url = item.path("url").asText("");
                if (!StringUtils.hasText(name) || !StringUtils.hasText(url)) {
                    continue;
                }
                AdminApiWorkbenchResourceVO resource = new AdminApiWorkbenchResourceVO();
                resource.setName(name);
                resource.setUrl(url);
                resources.add(resource);
            }
            return resources;
        } catch (Exception e) {
            log.error("load api workbench resources failed", e);
            throw new IllegalStateException("获取 API 工作台资源失败: " + e.getMessage(), e);
        }
    }

    @Override
    public AdminApiWorkbenchDocVO getApiWorkbenchDoc(String resourceUrl) {
        requireTenantOneOpsAccess();
        if (!StringUtils.hasText(resourceUrl) || !resourceUrl.startsWith("/") || !resourceUrl.contains("api-docs")) {
            throw new IllegalArgumentException("非法的 API 文档地址");
        }
        try {
            HttpClient client = HttpClient.newBuilder().build();
            JsonNode root = loadWorkbenchDocRoot(client, resourceUrl);

            AdminApiWorkbenchDocVO doc = new AdminApiWorkbenchDocVO();
            doc.setSourceUrl(resourceUrl);
            doc.setServiceName(resolveWorkbenchServiceName(root, resourceUrl));
            doc.setBasePath(resolveWorkbenchBasePath(root));
            doc.setEndpoints(parseApiWorkbenchEndpoints(root.path("paths"), resourceUrl));
            return doc;
        } catch (Exception e) {
            log.error("load api workbench doc failed, resourceUrl={}", resourceUrl, e);
            throw new IllegalStateException("获取 API 文档失败: " + e.getMessage(), e);
        }
    }

    @Override
    public AdminRainbondConfigVO getRainbondConfig() {
        requireTenantOneOpsAccess();
        AdminRainbondConfigDTO config = loadRainbondConfig();
        AdminRainbondConfigVO vo = toRainbondConfigVO(config);
        vo.setMessage(Boolean.TRUE.equals(config.getEnabled()) ? "Rainbond API enabled" : "Rainbond API disabled");
        return vo;
    }

    @Override
    public AdminRainbondConfigVO saveRainbondConfig(AdminRainbondConfigDTO dto) {
        requireTenantOneOpsAccess();
        AdminRainbondConfigDTO current = loadRainbondConfig();
        AdminRainbondConfigDTO normalized = normalizeRainbondConfig(dto, current);
        upsertSystemConfig(RAINBOND_CONFIG_KEY, toJson(normalized), "Rainbond OpenAPI connection config");
        AdminRainbondConfigVO vo = toRainbondConfigVO(normalized);
        vo.setMessage("Rainbond API config saved");
        return vo;
    }

    @Override
    public Map<String, Object> getRainbondCatalog() {
        requireTenantOneOpsAccess();
        Map<String, List<Map<String, String>>> categories = new LinkedHashMap<>();
        for (RainbondEndpoint endpoint : buildRainbondEndpoints()) {
            categories.computeIfAbsent(endpoint.category, ignored -> new ArrayList<>()).add(endpoint.toMap());
        }
        return Map.of(
                "source", "https://www.rainbond.com/docs/Intro/",
                "basePath", "/openapi",
                "categories", categories,
                "editablePath", Boolean.TRUE
        );
    }

    @Override
    public Map<String, Object> getRainbondDiscovery() {
        requireTenantOneOpsAccess();
        AdminRainbondConfigDTO config = loadEnabledRainbondConfig();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("config", toRainbondConfigVO(config));
        result.put("enterprises", invokeRainbondDiscovery(config, "GET", "/openapi/v2/manage/enterprises"));
        result.put("teams", invokeRainbondDiscovery(config, "GET", "/openapi/v1/teams"));
        result.put("regions", invokeRainbondDiscovery(config, "GET", "/openapi/v1/regions"));
        if (StringUtils.hasText(config.getTeamId()) && StringUtils.hasText(config.getRegionName())) {
            result.put("apps", invokeRainbondDiscovery(config, "GET",
                    "/openapi/v1/teams/" + encodePathSegment(config.getTeamId()) + "/regions/" + encodePathSegment(config.getRegionName()) + "/apps"));
        } else {
            result.put("apps", Collections.emptyList());
        }
        return result;
    }

    @Override
    public AdminRainbondApiResultVO callRainbondApi(AdminRainbondApiCallDTO dto) {
        requireTenantOneOpsAccess();
        AdminRainbondConfigDTO config = loadEnabledRainbondConfig();
        AdminRainbondApiCallDTO normalized = normalizeRainbondCall(dto);
        long start = System.nanoTime();
        String requestUrl = buildRainbondRequestUrl(config, normalized);
        AdminRainbondApiResultVO result = new AdminRainbondApiResultVO();
        result.setMethod(normalized.getMethod());
        result.setPath(normalized.getPath());
        result.setRequestUrl(maskRainbondUrl(requestUrl));
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(resolveRainbondTimeout(config)))
                    .build();
            HttpRequest request = buildRainbondHttpRequest(config, normalized, requestUrl);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            result.setStatusCode(response.statusCode());
            result.setSuccess(response.statusCode() >= 200 && response.statusCode() < 300);
            result.setResponseBody(response.body());
            result.setMessage(Boolean.TRUE.equals(result.getSuccess()) ? "Rainbond API call succeeded" : "Rainbond API call failed");
            return result;
        } catch (Exception e) {
            log.warn("call rainbond api failed, method={}, path={}", normalized.getMethod(), normalized.getPath(), e);
            result.setStatusCode(0);
            result.setSuccess(Boolean.FALSE);
            result.setResponseBody(e.getMessage());
            result.setMessage("Rainbond API call failed: " + e.getMessage());
            return result;
        } finally {
            result.setDurationMillis(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
        }
    }

    private AdminRainbondConfigDTO loadRainbondConfig() {
        AdminRainbondConfigDTO fallback = normalizeRainbondConfig(null, null);
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT config_value FROM system_config WHERE tenant_id = 1 AND config_key = ? AND is_deleted = 0 ORDER BY id DESC LIMIT 1",
                    RAINBOND_CONFIG_KEY
            );
            if (rows.isEmpty() || !StringUtils.hasText(valueAsString(rows.get(0).get("config_value")))) {
                return fallback;
            }
            return normalizeRainbondConfig(objectMapper.readValue(valueAsString(rows.get(0).get("config_value")), AdminRainbondConfigDTO.class), fallback);
        } catch (Exception e) {
            log.warn("load rainbond config failed, fallback to properties", e);
            return fallback;
        }
    }

    private AdminRainbondConfigDTO loadEnabledRainbondConfig() {
        AdminRainbondConfigDTO config = loadRainbondConfig();
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            throw new BadRequestException("Rainbond API is disabled");
        }
        if (!StringUtils.hasText(config.getBaseUrl())) {
            throw new BadRequestException("Rainbond baseUrl is required");
        }
        if (!StringUtils.hasText(config.getToken())) {
            throw new BadRequestException("Rainbond token is required");
        }
        return config;
    }

    private AdminRainbondConfigDTO normalizeRainbondConfig(AdminRainbondConfigDTO source, AdminRainbondConfigDTO current) {
        AdminRainbondConfigDTO dto = source == null ? new AdminRainbondConfigDTO() : source;
        AdminRainbondConfigDTO base = current == null ? new AdminRainbondConfigDTO() : current;
        dto.setEnabled(source == null ? StringUtils.hasText(rainbondBaseUrl) : Boolean.TRUE.equals(dto.getEnabled()));
        dto.setBaseUrl(trimToDefault(dto.getBaseUrl(), StringUtils.hasText(base.getBaseUrl()) ? base.getBaseUrl() : rainbondBaseUrl));
        String token = trimToEmpty(dto.getToken());
        if (!StringUtils.hasText(token) || token.startsWith("***")) {
            token = StringUtils.hasText(base.getToken()) ? base.getToken() : rainbondToken;
        }
        dto.setToken(token);
        dto.setEnterpriseId(trimToDefault(dto.getEnterpriseId(), StringUtils.hasText(base.getEnterpriseId()) ? base.getEnterpriseId() : rainbondEnterpriseId));
        dto.setTeamId(trimToDefault(dto.getTeamId(), StringUtils.hasText(base.getTeamId()) ? base.getTeamId() : rainbondTeamId));
        dto.setRegionName(trimToDefault(dto.getRegionName(), StringUtils.hasText(base.getRegionName()) ? base.getRegionName() : rainbondRegionName));
        dto.setTimeoutSeconds(Math.max(3, Math.min(dto.getTimeoutSeconds() == null ? (rainbondTimeoutSeconds == null ? 15 : rainbondTimeoutSeconds) : dto.getTimeoutSeconds(), 60)));
        if (StringUtils.hasText(dto.getBaseUrl())) {
            dto.setBaseUrl(sanitizeOpsUrl(dto.getBaseUrl()));
        }
        return dto;
    }

    private AdminRainbondConfigVO toRainbondConfigVO(AdminRainbondConfigDTO config) {
        AdminRainbondConfigVO vo = new AdminRainbondConfigVO();
        vo.setEnabled(Boolean.TRUE.equals(config.getEnabled()));
        vo.setBaseUrl(config.getBaseUrl());
        vo.setTokenConfigured(StringUtils.hasText(config.getToken()));
        vo.setTokenMasked(maskSecret(config.getToken()));
        vo.setEnterpriseId(config.getEnterpriseId());
        vo.setTeamId(config.getTeamId());
        vo.setRegionName(config.getRegionName());
        vo.setTimeoutSeconds(config.getTimeoutSeconds());
        return vo;
    }

    private Map<String, Object> invokeRainbondDiscovery(AdminRainbondConfigDTO config, String method, String path) {
        AdminRainbondApiCallDTO dto = new AdminRainbondApiCallDTO();
        dto.setMethod(method);
        dto.setPath(path);
        AdminRainbondApiResultVO response = callRainbondApiWithConfig(config, normalizeRainbondCall(dto));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("path", path);
        result.put("statusCode", response.getStatusCode());
        result.put("success", response.getSuccess());
        result.put("body", response.getResponseBody());
        return result;
    }

    private AdminRainbondApiResultVO callRainbondApiWithConfig(AdminRainbondConfigDTO config, AdminRainbondApiCallDTO normalized) {
        long start = System.nanoTime();
        String requestUrl = buildRainbondRequestUrl(config, normalized);
        AdminRainbondApiResultVO result = new AdminRainbondApiResultVO();
        result.setMethod(normalized.getMethod());
        result.setPath(normalized.getPath());
        result.setRequestUrl(maskRainbondUrl(requestUrl));
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(resolveRainbondTimeout(config)))
                    .build();
            HttpRequest request = buildRainbondHttpRequest(config, normalized, requestUrl);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            result.setStatusCode(response.statusCode());
            result.setSuccess(response.statusCode() >= 200 && response.statusCode() < 300);
            result.setResponseBody(response.body());
            result.setMessage(Boolean.TRUE.equals(result.getSuccess()) ? "Rainbond API call succeeded" : "Rainbond API call failed");
        } catch (Exception e) {
            log.warn("call rainbond api failed, method={}, path={}", normalized.getMethod(), normalized.getPath(), e);
            result.setStatusCode(0);
            result.setSuccess(Boolean.FALSE);
            result.setResponseBody(e.getMessage());
            result.setMessage("Rainbond API call failed: " + e.getMessage());
        }
        result.setDurationMillis(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
        return result;
    }

    private AdminRainbondApiCallDTO normalizeRainbondCall(AdminRainbondApiCallDTO dto) {
        if (dto == null) {
            throw new BadRequestException("Rainbond API call payload is required");
        }
        String method = StringUtils.hasText(dto.getMethod()) ? dto.getMethod().trim().toUpperCase() : "GET";
        if (!SUPPORTED_RAINBOND_METHODS.contains(method)) {
            throw new BadRequestException("unsupported Rainbond method: " + method);
        }
        String path = dto.getPath() == null ? "" : dto.getPath().trim();
        if (!path.startsWith("/openapi/")) {
            throw new BadRequestException("Rainbond path must start with /openapi/");
        }
        AdminRainbondApiCallDTO normalized = new AdminRainbondApiCallDTO();
        normalized.setMethod(method);
        normalized.setPath(path);
        normalized.setQuery(dto.getQuery() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(dto.getQuery()));
        normalized.setHeaders(dto.getHeaders() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(dto.getHeaders()));
        normalized.setBody(dto.getBody());
        return normalized;
    }

    private String buildRainbondRequestUrl(AdminRainbondConfigDTO config, AdminRainbondApiCallDTO dto) {
        StringBuilder url = new StringBuilder(sanitizeOpsUrl(config.getBaseUrl())).append(dto.getPath());
        if (dto.getQuery() != null && !dto.getQuery().isEmpty()) {
            List<String> pairs = new ArrayList<>();
            dto.getQuery().forEach((key, value) -> {
                if (StringUtils.hasText(key) && value != null && StringUtils.hasText(String.valueOf(value))) {
                    pairs.add(URLEncoder.encode(key, StandardCharsets.UTF_8) + "=" +
                            URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8));
                }
            });
            if (!pairs.isEmpty()) {
                url.append("?").append(String.join("&", pairs));
            }
        }
        return url.toString();
    }

    private HttpRequest buildRainbondHttpRequest(AdminRainbondConfigDTO config, AdminRainbondApiCallDTO dto, String requestUrl) {
        HttpRequest.BodyPublisher publisher = ("GET".equals(dto.getMethod()) || "DELETE".equals(dto.getMethod()) || dto.getBody() == null)
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(dto.getBody() instanceof String ? (String) dto.getBody() : toJson(dto.getBody()), StandardCharsets.UTF_8);
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(requestUrl))
                .timeout(Duration.ofSeconds(resolveRainbondTimeout(config)))
                .header("Accept", "application/json")
                .header("Authorization", config.getToken())
                .method(dto.getMethod(), publisher);
        if (!("GET".equals(dto.getMethod()) || "DELETE".equals(dto.getMethod())) && dto.getBody() != null) {
            builder.header("Content-Type", "application/json");
        }
        dto.getHeaders().forEach((key, value) -> {
            if (StringUtils.hasText(key) && StringUtils.hasText(value) && !"authorization".equalsIgnoreCase(key)) {
                builder.header(key, value);
            }
        });
        return builder.build();
    }

    private int resolveRainbondTimeout(AdminRainbondConfigDTO config) {
        return Math.max(3, Math.min(config.getTimeoutSeconds() == null ? 15 : config.getTimeoutSeconds(), 60));
    }

    private String maskRainbondUrl(String requestUrl) {
        return requestUrl == null ? "" : requestUrl.replaceAll("([?&](?:token|access_token)=)[^&]+", "$1***");
    }

    private String maskSecret(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 8) {
            return "***";
        }
        return trimmed.substring(0, 4) + "***" + trimmed.substring(trimmed.length() - 4);
    }

    private void upsertSystemConfig(String key, String value, String description) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM system_config WHERE tenant_id = 1 AND config_key = ? AND is_deleted = 0",
                Integer.class,
                key
        );
        if (count != null && count > 0) {
            jdbcTemplate.update(
                    "UPDATE system_config SET config_value = ?, description = ?, is_enabled = 1, update_time = NOW() WHERE tenant_id = 1 AND config_key = ? AND is_deleted = 0",
                    value,
                    description,
                    key
            );
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO system_config (config_key, config_value, description, tenant_id, is_enabled, is_deleted, create_time, update_time) VALUES (?, ?, ?, 1, 1, 0, NOW(), NOW())",
                key,
                value,
                description
        );
    }

    private List<RainbondEndpoint> buildRainbondEndpoints() {
        return Arrays.asList(
                rb("enterprise", "getEnterpriseConfigInfo", "GET", "/openapi/v1/configs"),
                rb("enterprise", "getEnterpriseList", "GET", "/openapi/v2/manage/enterprises"),
                rb("enterprise", "getEnterpriseResource", "GET", "/openapi/v2/manage/enterprises/{eid}/resource"),
                rb("team", "getTeamList", "GET", "/openapi/v1/teams"),
                rb("team", "createTeam", "POST", "/openapi/v1/teams"),
                rb("team", "getTeamDetail", "GET", "/openapi/v1/teams/{team_id}"),
                rb("team", "updateTeam", "PUT", "/openapi/v1/teams/{team_id}"),
                rb("team", "deleteTeam", "DELETE", "/openapi/v1/teams/{team_id}"),
                rb("team", "getTeamRegions", "GET", "/openapi/v1/teams/{team_id}/regions"),
                rb("team", "createTeamRegion", "POST", "/openapi/v1/teams/{team_id}/regions"),
                rb("team", "getTeamOverview", "GET", "/openapi/v1/teams/{team_id}/regions/{region_name}/overview"),
                rb("team", "getTeamResources", "POST", "/openapi/v1/teams/resource"),
                rb("team", "getCertificatesUnderTeam", "GET", "/openapi/v1/teams/{team_id}/certificates"),
                rb("team", "createCertificates", "POST", "/openapi/v1/teams/{team_id}/certificates"),
                rb("team", "getCertificateDetail", "GET", "/openapi/v1/teams/{team_id}/certificates/{certificate_id}"),
                rb("team", "updateCertificate", "PUT", "/openapi/v1/teams/{team_id}/certificates/{certificate_id}"),
                rb("team", "deleteCertificate", "DELETE", "/openapi/v1/teams/{team_id}/certificates/{certificate_id}"),
                rb("team", "getEventLogs", "GET", "/openapi/v1/teams/{team_id}/regions/{region_name}/events/{event_id}/logs"),
                rb("region", "getRegionList", "GET", "/openapi/v1/regions"),
                rb("region", "getRegionInfo", "GET", "/openapi/v1/regions/{region_id}"),
                rb("region", "addRegion", "POST", "/openapi/v1/regions"),
                rb("region", "getRegionListv2", "GET", "/openapi/v2/manage/regions"),
                rb("region", "getRegionInfoSer", "GET", "/openapi/v2/manage/regions/{region_id}"),
                rb("region", "addRegionv2", "POST", "/openapi/v2/manage/regions"),
                rb("region", "updateRegionInfo", "PUT", "/openapi/v2/manage/regions/{region_id}"),
                rb("region", "delRegionInfo", "DELETE", "/openapi/v2/manage/regions/{region_id}"),
                rb("application", "getTeamApps", "GET", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps"),
                rb("application", "createTeamApps", "POST", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps"),
                rb("application", "closeApp", "POST", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/close"),
                rb("application", "getAppDetail", "GET", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}"),
                rb("application", "deleteApp", "DELETE", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}"),
                rb("application", "operateApp", "POST", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}/operations"),
                rb("application", "upgradeApp", "POST", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}/upgrade"),
                rb("application", "getAppUpgradeInfo", "GET", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}/upgrade"),
                rb("application", "copyApp", "POST", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}/copy"),
                rb("application", "getCopyAppInfo", "GET", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}/copy"),
                rb("application", "buildHelmApp", "GET", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}/helm_chart"),
                rb("application", "buildHelmApp", "POST", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}/helm_chart"),
                rb("application", "installAppByMarket", "POST", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}/install"),
                rb("application", "componentMonitorUnderApp", "GET", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}/monitor/query"),
                rb("application", "componentHistoryMonitorUnderApp", "GET", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}/monitor/query_range"),
                rb("application", "listComponents", "GET", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}/services"),
                rb("application", "createComponent", "POST", "/openapi/v1/teams/{team_name}/regions/{region_name}/apps/{group_id}/services"),
                rb("application", "getComponentDetail", "GET", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}/services/{service_id}"),
                rb("application", "deleteComponent", "DELETE", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}/services/{service_id}"),
                rb("application", "buildComponent", "POST", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}/services/{service_id}/build"),
                rb("application", "changeImageName", "PUT", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}/services/{service_id}/docker-image-change"),
                rb("application", "getComponentEvents", "GET", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}/services/{service_id}/events"),
                rb("application", "handleComponentPorts", "GET", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}/services/{service_id}/ports"),
                rb("application", "updateComponentEnvs", "PUT", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}/services/{service_id}/envs"),
                rb("application", "bindVolumes", "POST", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}/services/{service_id}/volumes"),
                rb("application", "horizontalScalingOfComponents", "POST", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}/services/{service_id}/telescopic/horizontal"),
                rb("application", "verticalScalingOfComponents", "POST", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}/services/{service_id}/telescopic/vertical"),
                rb("application", "createGatewayRules", "POST", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}/domains"),
                rb("gateway", "getGatewayList", "GET", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}/domains"),
                rb("gateway", "delGatewayRule", "DELETE", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}/domains/{rule_id}"),
                rb("gateway", "updateHttpGatewayRule", "PUT", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}/domains/{rule_id}"),
                rb("gateway", "getEnterpriuseHttpRule", "GET", "/openapi/v1/httpdomains"),
                rb("gateway", "getHttpRuleList", "GET", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}/httpdomains"),
                rb("gateway", "createHttpRule", "POST", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}/httpdomains"),
                rb("gateway", "getHttpRule", "GET", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}/httpdomains/{rule_id}"),
                rb("gateway", "updateHttpRule", "PUT", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}/httpdomains/{rule_id}"),
                rb("gateway", "delHttpRule", "DELETE", "/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}/httpdomains/{rule_id}"),
                rb("user", "getUserLIst", "GET", "/openapi/v1/users"),
                rb("user", "addUser", "POST", "/openapi/v1/users"),
                rb("user", "getUserInfo", "GET", "/openapi/v1/users/{user_id}"),
                rb("user", "updateUserInfo", "PUT", "/openapi/v1/users/{user_id}"),
                rb("user", "deleteUser", "DELETE", "/openapi/v1/users/{user_id}"),
                rb("user", "changeUserPassword", "PUT", "/openapi/v1/users/{user_id}/changepwd"),
                rb("user", "changePasswd", "PUT", "/openapi/v1/changepwd"),
                rb("user", "getEnterpriseAdminList", "GET", "/openapi/v1/administrators"),
                rb("user", "addEnterpriseUser", "POST", "/openapi/v1/administrators"),
                rb("user", "delEnterpriseAdmin", "DELETE", "/openapi/v1/administrators/{user_id}")
        );
    }

    private RainbondEndpoint rb(String category, String name, String method, String path) {
        return new RainbondEndpoint(category, name, method, path);
    }

    private static class RainbondEndpoint {
        private final String category;
        private final String name;
        private final String method;
        private final String path;

        private RainbondEndpoint(String category, String name, String method, String path) {
            this.category = category;
            this.name = name;
            this.method = method;
            this.path = path;
        }

        private Map<String, String> toMap() {
            Map<String, String> result = new LinkedHashMap<>();
            result.put("category", category);
            result.put("name", name);
            result.put("method", method);
            result.put("path", path);
            result.put("docs", "https://www.rainbond.com/docs/api/" + category + "/" + name);
            return result;
        }
    }

    private JsonNode loadWorkbenchDocRoot(HttpClient client, String resourceUrl) throws Exception {
        String authorization = resolveCurrentAuthorizationHeader();
        HttpResponse<String> gatewayResponse = sendWorkbenchDocRequest(
                client,
                sanitizeOpsUrl(gatewayUrl) + resourceUrl,
                authorization
        );
        if (gatewayResponse.statusCode() >= 200 && gatewayResponse.statusCode() < 300) {
            return objectMapper.readTree(gatewayResponse.body());
        }
        String directServiceUrl = resolveDirectWorkbenchDocUrl(resourceUrl);
        if (StringUtils.hasText(directServiceUrl)) {
            HttpResponse<String> directResponse = sendWorkbenchDocRequest(client, directServiceUrl, authorization);
            if (directResponse.statusCode() >= 200 && directResponse.statusCode() < 300) {
                return objectMapper.readTree(directResponse.body());
            }
            throw new IllegalStateException("gateway api-docs status " + gatewayResponse.statusCode()
                    + ", direct api-docs status " + directResponse.statusCode());
        }
        throw new IllegalStateException("gateway api-docs request failed with status " + gatewayResponse.statusCode());
    }

    private HttpResponse<String> sendWorkbenchDocRequest(HttpClient client, String url, String authorization) throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(url)).GET();
        if (StringUtils.hasText(authorization)) {
            requestBuilder.header("Authorization", authorization);
        }
        HttpRequest request = requestBuilder.build();
        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private String resolveDirectWorkbenchDocUrl(String resourceUrl) {
        String serviceKey = resourceUrl;
        if (serviceKey.startsWith("/")) {
            serviceKey = serviceKey.substring(1);
        }
        int slashIndex = serviceKey.indexOf('/');
        if (slashIndex <= 0) {
            return null;
        }
        serviceKey = serviceKey.substring(0, slashIndex);
        String baseUrl = workbenchDocServiceBaseUrls.get(serviceKey);
        if (!StringUtils.hasText(baseUrl)) {
            return null;
        }
        return sanitizeOpsUrl(baseUrl) + "/v2/api-docs";
    }

    @Override
    public AdminBackupOverviewVO getBackupOverview() {
        ensureDefaultBackupSchedule();

        AdminBackupOverviewVO overview = new AdminBackupOverviewVO();
        List<AdminBackupRecordVO> backups = loadBackupRecords();
        AdminBackupScheduleVO schedule = loadBackupSchedule();

        AdminBackupStorageStatsVO storageStats = new AdminBackupStorageStatsVO();
        storageStats.setTotalFiles(backups.size());
        storageStats.setTotalSize(formatBytes(sumBackupBytes()));
        storageStats.setLastBackup(backups.isEmpty() ? "-" : backups.get(0).getCreateTime());
        storageStats.setAutoBackupEnabled(Boolean.TRUE.equals(schedule.getEnabled()));
        storageStats.setDatabaseCount(DEFAULT_BACKUP_DATABASES.size());

        overview.setStorageStats(storageStats);
        overview.setBackups(backups);
        overview.setSchedule(schedule);
        overview.setRestoreSupported(Boolean.FALSE);
        return overview;
    }

    @Override
    public Map<String, List<String>> getBackupTables(List<String> databases) {
        List<String> normalizedDatabases = normalizeDatabases(databases);
        if (normalizedDatabases.isEmpty()) {
            normalizedDatabases = new ArrayList<>(DEFAULT_BACKUP_DATABASES);
        }
        return backupArchiveBuilder.resolveTableSelections(normalizedDatabases, null);
    }

    @Override
    public AdminBackupOverviewVO createBackup(AdminBackupCreateDTO dto) {
        List<String> databases = normalizeDatabases(dto == null ? null : dto.getDatabases());
        if (databases.isEmpty()) {
            databases = DEFAULT_BACKUP_DATABASES;
        }
        Map<String, List<String>> tableSelections = backupArchiveBuilder.resolveTableSelections(
                databases,
                dto == null ? null : dto.getTables()
        );
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String fileName = "rk_backup_" + timestamp + ".sql.gz";
        String backupName = "backup_" + timestamp;
        byte[] archiveBytes = backupArchiveBuilder.buildArchive(databases, tableSelections);
        String remotePath = backupStorageService.store(fileName, archiveBytes);
        long fileSize = archiveBytes.length;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("databases", databases);
        summary.put("tables", tableSelections);
        summary.put("databaseSizes", queryDatabaseSizeRows(databases));

        jdbcTemplate.update(
                "INSERT INTO admin_backup_record (backup_name, database_scope, database_list, file_name, remote_path, file_size_bytes, status, backup_type, note_text, summary_json, created_by) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                backupName,
                databases.size() == DEFAULT_BACKUP_DATABASES.size() ? "ALL" : "PARTIAL",
                toJson(databases),
                fileName,
                remotePath,
                fileSize,
                "SUCCESS",
                "MANUAL",
                dto == null ? null : dto.getNote(),
                toJson(summary),
                UserContext.getUser()
        );
        return getBackupOverview();
    }

    @Override
    public AdminBackupOverviewVO saveBackupSchedule(AdminBackupScheduleDTO dto) {
        ensureDefaultBackupSchedule();
        List<String> databases = normalizeDatabases(dto == null ? null : dto.getDatabases());
        jdbcTemplate.update(
                "UPDATE admin_backup_schedule SET enabled = ?, frequency = ?, backup_time = ?, retention_days = ?, database_list = ?, update_time = NOW() WHERE id = 1",
                Boolean.TRUE.equals(dto.getEnabled()) ? 1 : 0,
                StringUtils.hasText(dto.getFrequency()) ? dto.getFrequency() : "daily",
                StringUtils.hasText(dto.getTime()) ? dto.getTime() : "02:00",
                dto.getRetentionDays() == null ? 30 : dto.getRetentionDays(),
                toJson(databases.isEmpty() ? DEFAULT_BACKUP_DATABASES : databases)
        );
        return getBackupOverview();
    }

    @Override
    public byte[] downloadBackup(Long backupId) {
        Map<String, Object> row = loadBackupRecordRow(backupId);
        String remotePath = valueAsString(row.get("remote_path"));
        if (!StringUtils.hasText(remotePath)) {
            throw new BadRequestException("该备份仅包含基线快照，没有可下载文件");
        }
        if (!backupStorageService.exists(remotePath)) {
            throw new BadRequestException("备份文件不存在或已被清理，请重新创建备份");
        }
        return backupStorageService.read(remotePath);
    }

    @Override
    public Boolean deleteBackup(Long backupId) {
        Map<String, Object> row = loadBackupRecordRow(backupId);
        String remotePath = valueAsString(row.get("remote_path"));
        if (StringUtils.hasText(remotePath)) {
            backupStorageService.delete(remotePath);
        }
        return jdbcTemplate.update("UPDATE admin_backup_record SET is_deleted = 1, update_time = NOW() WHERE id = ?", backupId) > 0;
    }

    @Override
    public List<AdminDeployPackageRecordVO> listDeployPackages() {
        return loadDeployPackageRecords();
    }

    @Override
    public List<AdminDeployPackageRecordVO> listRemoteMigrations() {
        return loadRemoteMigrationRecords();
    }

    @Override
    public AdminDeployPackageDetailVO getDeployPackageDetail(Long packageId) {
        requireTenantOneOpsAccess();
        Map<String, Object> row = loadDeployPackageRecordRow(packageId);
        AdminDeployPackageDetailVO vo = new AdminDeployPackageDetailVO();
        vo.setRecord(toDeployPackageVO(row));
        vo.setRequestSummary(readJsonMap(row.get("request_json")));
        vo.setLastDiagnosis(readJsonMap(row.get("last_diagnosis_json")));
        vo.setLastRepair(readJsonMap(row.get("last_repair_json")));
        vo.setKubeconfigRedacted(valueAsString(row.get("kubeconfig_redacted")));
        vo.setKubeconfigFingerprint(valueAsString(row.get("kubeconfig_fingerprint")));
        return vo;
    }

    @Override
    public AdminDeployPackageKubeconfigVO getDeployPackageKubeconfig(Long packageId) {
        requireTenantOneOpsAccess();
        Map<String, Object> row = loadDeployPackageRecordRow(packageId);
        String kubeconfig = decryptKubeconfig(valueAsString(row.get("kubeconfig_ciphertext")));
        if (!StringUtils.hasText(kubeconfig)) {
            throw new BadRequestException("该部署历史没有保存 kubeconfig");
        }
        AdminDeployPackageKubeconfigVO vo = new AdminDeployPackageKubeconfigVO();
        vo.setPackageId(packageId);
        vo.setKubeconfig(kubeconfig);
        vo.setKubeconfigFingerprint(valueAsString(row.get("kubeconfig_fingerprint")));
        vo.setRedacted(valueAsString(row.get("kubeconfig_redacted")));
        vo.setViewedAt(LocalDateTime.now().format(TIME_FORMATTER));
        Map<String, Object> auditPayload = new LinkedHashMap<>();
        auditPayload.put("packageId", packageId);
        auditPayload.put("fingerprint", vo.getKubeconfigFingerprint());
        recordOperationAudit(
                "DEPLOY_PACKAGE_KUBECONFIG_VIEW",
                resolveStoredTargetNamespace(row),
                "DeployPackage",
                String.valueOf(packageId),
                "VIEW_KUBECONFIG",
                "success",
                "view remote deploy kubeconfig plaintext",
                toJson(auditPayload),
                "kubeconfig plaintext returned to authorized operator"
        );
        return vo;
    }

    @Override
    public AdminDeployPackageDetailVO getRemoteMigrationDetail(Long migrationId) {
        requireRemoteMigrationRecordRow(migrationId);
        return getDeployPackageDetail(migrationId);
    }

    @Override
    public AdminDeployPackageKubeconfigVO getRemoteMigrationKubeconfig(Long migrationId) {
        requireRemoteMigrationRecordRow(migrationId);
        return getDeployPackageKubeconfig(migrationId);
    }

    @Override
    public AdminDeployPackageDiagnosisVO diagnoseRemoteMigration(Long migrationId) {
        requireRemoteMigrationRecordRow(migrationId);
        return diagnoseDeployPackage(migrationId);
    }

    @Override
    public AdminDeployPackageRepairResultVO repairRemoteMigration(Long migrationId, AdminDeployPackageRepairDTO dto) {
        requireRemoteMigrationRecordRow(migrationId);
        return repairDeployPackage(migrationId, dto);
    }

    @Override
    public AdminDeployPackageDiagnosisVO diagnoseDeployPackage(Long packageId) {
        requireTenantOneOpsAccess();
        Map<String, Object> row = loadDeployPackageRecordRow(packageId);
        AdminDeployPackageCreateDTO request = buildStoredRemoteDeployRequest(row);
        String namespace = trimToDefault(request.getTargetNamespace(), "shetuanguanlixitong");
        AdminDeployPackageDiagnosisVO vo = new AdminDeployPackageDiagnosisVO();
        vo.setPackageId(packageId);
        vo.setNamespace(namespace);
        vo.setDiagnosedAt(LocalDateTime.now().format(TIME_FORMATTER));
        vo.setServiceStatuses(new ArrayList<>());
        vo.setDiagnosisChecks(new ArrayList<>());
        vo.setSuggestedRepairActions(new ArrayList<>());

        StringBuilder terminalLog = new StringBuilder();
        Path kubeconfigPath = null;
        try {
            kubeconfigPath = writeTemporaryKubeconfig(request.getKubeconfig());
            String snapshot = runDiagnosisCommand(kubeconfigPath, request.getKubeContext(),
                    "-n " + quoteForBash(namespace) + " get pod,sts,deploy,svc,pvc,endpoints -o wide",
                    90, "get pod,sts,deploy,svc,pvc,endpoints", terminalLog, vo);
            String events = runDiagnosisCommand(kubeconfigPath, request.getKubeContext(),
                    "-n " + quoteForBash(namespace) + " get events --sort-by=.lastTimestamp",
                    90, "get events --sort-by=.lastTimestamp", terminalLog, vo);
            String nacos = runDiagnosisCommand(kubeconfigPath, request.getKubeContext(),
                    "-n " + quoteForBash(namespace) + " get pods -l app=nacos -o wide",
                    60, "nacos config rk-shared-mybatis.yaml", terminalLog, vo);
            String mysql = runDiagnosisCommand(kubeconfigPath, request.getKubeContext(),
                    "-n " + quoteForBash(namespace) + " get pods -l app=mysql -o wide",
                    60, "mysql imported rk_user nacos xxl_job", terminalLog, vo);
            String minio = runDiagnosisCommand(kubeconfigPath, request.getKubeContext(),
                    "-n " + quoteForBash(namespace) + " get pods -l app=minio -o wide",
                    60, "minio policy mc anonymous set download", terminalLog, vo);

            collectRemoteServiceStatuses(vo, snapshot, nacos, mysql, minio);
            collectRemoteRepairSuggestions(vo, snapshot + "\n" + events + "\n" + nacos + "\n" + mysql + "\n" + minio);
            vo.setSuccess(Boolean.TRUE);
        } catch (Exception e) {
            vo.setSuccess(Boolean.FALSE);
            addDiagnosisCheck(vo, "诊断执行", "failed", sanitizeDeployLog(e.getMessage()));
            vo.getSuggestedRepairActions().add(REPAIR_RESTART_WORKLOADS);
            terminalLog.append("\n[diagnosis failed]\n").append(sanitizeDeployLog(e.getMessage())).append('\n');
        } finally {
            deleteTemporaryKubeconfigQuietly(kubeconfigPath);
        }
        vo.setTerminalLog(limitText(sanitizeDeployLog(terminalLog.toString()), 20000));
        jdbcTemplate.update(
                "UPDATE ops_deploy_package_record SET last_diagnosis_json = ?, update_time = NOW() WHERE id = ?",
                toJson(vo),
                packageId
        );
        recordOperationAudit(
                "DEPLOY_PACKAGE_REMOTE_DIAGNOSE",
                namespace,
                "DeployPackage",
                String.valueOf(packageId),
                "DIAGNOSE",
                Boolean.TRUE.equals(vo.getSuccess()) ? "success" : "failed",
                "remote deploy diagnosis",
                toJson(Map.of("packageId", packageId, "namespace", namespace)),
                vo.getTerminalLog()
        );
        return vo;
    }

    @Override
    public AdminDeployPackageRepairResultVO repairDeployPackage(Long packageId, AdminDeployPackageRepairDTO dto) {
        requireTenantOneOpsAccess();
        AdminDeployPackageRepairDTO requestDto = dto == null ? new AdminDeployPackageRepairDTO() : dto;
        String action = trimToEmpty(requestDto.getRepairAction());
        if (!SUPPORTED_REMOTE_REPAIR_ACTIONS.contains(action)) {
            throw new BadRequestException("不支持的远程部署修复动作: " + action);
        }
        if (!REMOTE_DEPLOY_REPAIR_CONFIRM_TEXT.equals(trimToEmpty(requestDto.getConfirmText()))) {
            throw new BadRequestException("远程部署修复需要输入确认文本 " + REMOTE_DEPLOY_REPAIR_CONFIRM_TEXT);
        }
        Map<String, Object> row = loadDeployPackageRecordRow(packageId);
        AdminDeployPackageCreateDTO request = buildStoredRemoteDeployRequest(row);
        String namespace = trimToDefault(request.getTargetNamespace(), "shetuanguanlixitong");
        AdminDeployPackageRepairResultVO vo = new AdminDeployPackageRepairResultVO();
        vo.setPackageId(packageId);
        vo.setRepairAction(action);
        vo.setRepairedAt(LocalDateTime.now().format(TIME_FORMATTER));
        vo.setOutputs(new ArrayList<>());

        Path kubeconfigPath = null;
        try {
            kubeconfigPath = writeTemporaryKubeconfig(request.getKubeconfig());
            if (REPAIR_RESTART_WORKLOADS.equals(action)) {
                repairRestartRemoteWorkloads(kubeconfigPath, request, namespace, vo.getOutputs());
                vo.setNextSuggestion("等待业务 Deployment 和 StatefulSet 重新就绪后再次诊断");
            } else if (REPAIR_DELETE_STUCK_PODS.equals(action)) {
                repairDeleteStuckPods(kubeconfigPath, request, namespace, vo.getOutputs());
                vo.setNextSuggestion("等待控制器重新拉起 Pod 后再次诊断");
            } else if (REPAIR_MINIO_PUBLIC_POLICY.equals(action)) {
                repairMinioPublicPolicy(kubeconfigPath, request, namespace, vo.getOutputs());
                vo.setNextSuggestion("重新打开图片地址检查 MinIO 匿名下载策略");
            } else if (REPAIR_RERUN_MYSQL_IMPORT.equals(action)) {
                cleanupRemoteImportResources(packageId, kubeconfigPath, request, namespace, REMOTE_MYSQL_IMPORT_JOB, null);
                Path manifest = Files.createTempFile("rk-remote-mysql-repair-", ".yaml");
                try {
                    Files.writeString(manifest, buildRemoteMysqlImportJobManifest(namespace, REMOTE_MYSQL_IMAGE), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
                    vo.getOutputs().add(runKubectlWithKubeconfig(kubeconfigPath, request.getKubeContext(), "apply -f " + quoteForBash(manifest.toString()), 120, packageId));
                    waitRemoteJob(packageId, kubeconfigPath, request, namespace, REMOTE_MYSQL_IMPORT_JOB, resolveRemoteDataPlaneTimeoutSeconds(request));
                } finally {
                    Files.deleteIfExists(manifest);
                }
                vo.setNextSuggestion("数据库导入 Job 完成后重启 Nacos 和业务服务");
            } else if (REPAIR_RERUN_MINIO_IMPORT.equals(action)) {
                cleanupRemoteImportResources(packageId, kubeconfigPath, request, namespace, REMOTE_MINIO_IMPORT_JOB, null);
                Path manifest = Files.createTempFile("rk-remote-minio-repair-", ".yaml");
                try {
                    Files.writeString(manifest, buildRemoteMinioImportJobManifest(namespace, DEFAULT_MINIO_CLIENT_IMAGE), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
                    vo.getOutputs().add(runKubectlWithKubeconfig(kubeconfigPath, request.getKubeContext(), "apply -f " + quoteForBash(manifest.toString()), 120, packageId));
                    waitRemoteJob(packageId, kubeconfigPath, request, namespace, REMOTE_MINIO_IMPORT_JOB, resolveRemoteDataPlaneTimeoutSeconds(request));
                } finally {
                    Files.deleteIfExists(manifest);
                }
                vo.setNextSuggestion("MinIO 导入 Job 完成后重新检查图片访问");
            }
            vo.setSuccess(Boolean.TRUE);
        } catch (Exception e) {
            vo.setSuccess(Boolean.FALSE);
            vo.getOutputs().add(sanitizeDeployLog(e.getMessage()));
            vo.setNextSuggestion("修复失败，请先查看诊断日志中的失败资源");
        } finally {
            deleteTemporaryKubeconfigQuietly(kubeconfigPath);
        }
        Long auditId = recordOperationAudit(
                "DEPLOY_PACKAGE_REMOTE_REPAIR",
                namespace,
                "DeployPackage",
                String.valueOf(packageId),
                action,
                Boolean.TRUE.equals(vo.getSuccess()) ? "success" : "failed",
                emptyToNull(requestDto.getReason()),
                toJson(Map.of("packageId", packageId, "repairAction", action)),
                String.join("\n", vo.getOutputs())
        );
        vo.setAuditId(auditId);
        jdbcTemplate.update(
                "UPDATE ops_deploy_package_record SET last_repair_json = ?, logs = CONCAT(COALESCE(logs,''), ?), update_time = NOW() WHERE id = ?",
                toJson(vo),
                "\nremote deploy repair " + action + " " + (Boolean.TRUE.equals(vo.getSuccess()) ? "success" : "failed") + "\n",
                packageId
        );
        return vo;
    }

    @Override
    public AdminDeployPackageRecordVO getActiveGlobalMigration() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                buildDeployPackageRecordSelectSql("WHERE is_deleted = 0 AND status = 'RUNNING' AND global_migration_lock = 1 ORDER BY create_time DESC LIMIT 1")
        );
        return rows.isEmpty() ? null : toDeployPackageVO(rows.get(0));
    }

    @Override
    public AdminDeployPackageRecordVO getPublicActiveGlobalMigration() {
        AdminDeployPackageRecordVO record = getActiveGlobalMigration();
        if (record == null) {
            clearGlobalMigrationLock();
            return null;
        }
        refreshGlobalMigrationLock(record);
        return sanitizePublicMigrationRecord(record);
    }

    @Override
    public AdminDeployPackageRecordVO createDeployPackage(AdminDeployPackageCreateDTO dto) {
        AdminDeployPackageCreateDTO request = normalizeDeployPackageRequest(dto);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String packageName = "rk_web_deploy_" + timestamp;
        Long capturedUserId = UserContext.getUser();
        Long capturedTenantId = TenantContext.getTenantId();
        boolean registryPullPackage = IMAGE_ARTIFACT_REGISTRY.equals(request.getImageArtifactMode());
        jdbcTemplate.update(
                "INSERT INTO ops_deploy_package_record (package_name, status, progress, package_mode, delivery_mode, image_artifact_mode, deploy_mode, remote_deploy, current_step, uploaded_images, total_images, upload_percent, registry_pull_package, delete_after_download, note_text, request_json, logs, created_by) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                packageName,
                "RUNNING",
                10,
                request.getPackageMode(),
                request.getDeliveryMode(),
                request.getImageArtifactMode(),
                "package",
                0,
                "task created",
                0,
                initialDeployPackageImageCount(request),
                0,
                registryPullPackage ? 1 : 0,
                Boolean.TRUE.equals(request.getDeleteAfterDownload()) ? 1 : 0,
                request.getNote(),
                toJson(buildDeployPackageRequestForStorage(request)),
                buildDeployPackageInitialLogs(request),
                capturedUserId
        );
        Long packageId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        startDeployPackageBuildAsync(packageId, packageName, request, capturedTenantId, capturedUserId);
        return loadDeployPackageRecord(packageId);
    }

    private void startDeployPackageBuildAsync(Long packageId, String packageName, AdminDeployPackageCreateDTO request, Long capturedTenantId, Long capturedUserId) {
        Runnable task = () -> {
            if (capturedTenantId != null) {
                TenantContext.setTenantId(capturedTenantId);
            }
            if (capturedUserId != null) {
                UserContext.setUser(capturedUserId);
            }
            try {
                runDeployPackageBuild(packageId, packageName, request);
            } catch (Exception e) {
                log.error("deploy package async build failed, packageId={}", packageId, e);
            } finally {
                TenantContext.clear();
                UserContext.removeUser();
            }
        };
        Thread thread = new Thread(task, "rk-deploy-package-" + packageId);
        thread.setDaemon(true);
        thread.start();
    }

    private void runDeployPackageBuild(Long packageId, String packageName, AdminDeployPackageCreateDTO request) {
        Path runtimeArtifactDir = null;
        Path archivePath = null;
        try {
            updateDeployPackageStep(packageId, 35, "collecting database and service configuration", null, "collecting database and service configuration");
            List<String> databases = Boolean.FALSE.equals(request.getIncludeDatabases())
                    ? Collections.emptyList()
                    : normalizeDeployPackageDatabases(request.getDatabases());
            if (databases.isEmpty() && !Boolean.FALSE.equals(request.getIncludeDatabases())) {
                databases = DEFAULT_DEPLOY_PACKAGE_DATABASES;
            }
            if (Boolean.TRUE.equals(request.getExportRuntimeArtifacts())) {
                assertRuntimeExportDiskHeadroom(packageId);
                updateDeployPackageStep(packageId, 50, "server exporting runtime image tar artifacts", null, "server exporting runtime image tar artifacts");
                runtimeArtifactDir = exportRuntimeArtifactsToDirectory(request, packageName, packageId);
                if (IMAGE_ARTIFACT_REGISTRY.equals(request.getImageArtifactMode())
                        || IMAGE_ARTIFACT_BOTH.equals(request.getImageArtifactMode())) {
                    runRegistryPushForRuntimeArtifacts(runtimeArtifactDir, request, packageId);
                }
            } else if (IMAGE_ARTIFACT_REGISTRY.equals(request.getImageArtifactMode())
                    || IMAGE_ARTIFACT_BOTH.equals(request.getImageArtifactMode())) {
                recordRegistryPullPackageProgress(packageId, request);
            }
            Map<String, Object> manifest = buildDeployPackageManifest(request, databases);
            updateDeployPackageStep(packageId, 68, "building downloadable deploy package zip", null, "building downloadable deploy package zip");
            archivePath = buildDeployPackageArchiveFile(manifest, request, databases, runtimeArtifactDir, packageId);
            long archiveSize = Files.size(archivePath);
            updateDeployPackageStep(packageId, 80, "uploading deploy package to MinIO", null, "uploading deploy package to MinIO");
            String fileName = packageName + ".zip";
            String remotePath = backupStorageService.storeFile(fileName, archivePath);
            completeDeployPackageImageProgress(packageId);
            jdbcTemplate.update(
                    "UPDATE ops_deploy_package_record SET status = 'SUCCESS', progress = 100, current_step = ?, current_image = NULL, file_name = ?, remote_path = ?, file_size_bytes = ?, logs = CONCAT(COALESCE(logs,''), ?), update_time = NOW() WHERE id = ?",
                    IMAGE_ARTIFACT_REGISTRY.equals(request.getImageArtifactMode()) ? "registry pull package ready" : "deploy package ready",
                    fileName,
                    remotePath,
                    archiveSize,
                    (IMAGE_ARTIFACT_REGISTRY.equals(request.getImageArtifactMode()) ? "registry runtime image push completed; pull package generated successfully\n" : "deploy package generated successfully\n"),
                    packageId
            );
        } catch (Exception e) {
            log.error("deploy package generation failed, packageId={}", packageId, e);
            jdbcTemplate.update(
                    "UPDATE ops_deploy_package_record SET status = 'FAILED', progress = 100, current_step = 'failed', logs = CONCAT(COALESCE(logs,''), ?), update_time = NOW() WHERE id = ?",
                    "deploy package generation failed: " + e.getMessage() + "\n",
                    packageId
            );
        } finally {
            if (archivePath != null) {
                try {
                    Files.deleteIfExists(archivePath);
                } catch (Exception e) {
                    log.warn("delete temporary deploy package archive failed, packageId={}, path={}", packageId, archivePath, e);
                }
            }
            if (Boolean.TRUE.equals(request.getCleanupRuntimeArtifacts())) {
                cleanupRuntimeArtifactDirectory(runtimeArtifactDir, packageId);
            }
        }
    }

    @Override
    public AdminDeployPackageRecordVO createRemoteDeploy(AdminDeployPackageCreateDTO dto) {
        AdminDeployPackageCreateDTO request = normalizeDeployPackageRequest(dto);
        request.setRemoteDeploy(true);
        request.setIncludeDatabases(true);
        request.setIncludeAllCurrentData(true);
        request.setIncludeMinio(true);
        request.setApplyDatabaseSnapshot(true);
        request.setApplyMinioSnapshot(true);
        request.setWaitRollout(true);
        if (!StringUtils.hasText(request.getKubeconfig())) {
            throw new BadRequestException("kubeconfig 不能为空");
        }
        if (!REMOTE_DEPLOY_CONFIRM_TEXT.equals(trimToEmpty(request.getConfirmText()))) {
            throw new BadRequestException("在线远程部署需要输入确认文本 " + REMOTE_DEPLOY_CONFIRM_TEXT);
        }
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String packageName = "rk_web_remote_deploy_" + timestamp;
        LocalDateTime startedAt = LocalDateTime.now();
        Long capturedUserId = UserContext.getUser();
        Long capturedTenantId = TenantContext.getTenantId();
        String namespace = trimToDefault(request.getTargetNamespace(), "shetuanguanlixitong");
        int initialImageCount = initialRemoteDeployImageCount(request);
        int estimatedRemainingSeconds = computeEstimatedRemainingSeconds(5, initialImageCount);
        String kubeconfigCiphertext = encryptKubeconfig(request.getKubeconfig());
        String kubeconfigRedacted = redactKubeconfig(request.getKubeconfig());
        String kubeconfigFingerprint = fingerprintKubeconfig(request.getKubeconfig());
        jdbcTemplate.update(
                "INSERT INTO ops_deploy_package_record (package_name, status, progress, package_mode, delivery_mode, image_artifact_mode, deploy_mode, remote_deploy, remote_cluster_name, deploy_started_at, current_step, uploaded_images, total_images, upload_percent, registry_pull_package, streaming_migration, global_migration_lock, estimated_remaining_seconds, migration_started_at, migration_updated_at, delete_after_download, kubeconfig_ciphertext, kubeconfig_redacted, kubeconfig_fingerprint, note_text, request_json, logs, created_by) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                packageName,
                "RUNNING",
                5,
                request.getPackageMode(),
                request.getDeliveryMode(),
                request.getImageArtifactMode(),
                "remote-deploy",
                1,
                trimToDefault(request.getKubeContext(), "kubeconfig"),
                startedAt,
                "remote deploy task created",
                0,
                initialImageCount,
                0,
                IMAGE_ARTIFACT_REGISTRY.equals(request.getImageArtifactMode()) ? 1 : 0,
                Boolean.TRUE.equals(request.getStreamingMigration()) ? 1 : 0,
                Boolean.TRUE.equals(request.getGlobalMigrationLock()) ? 1 : 0,
                estimatedRemainingSeconds,
                startedAt,
                startedAt,
                0,
                kubeconfigCiphertext,
                kubeconfigRedacted,
                kubeconfigFingerprint,
                request.getNote(),
                toJson(buildRemoteDeployRequestForStorage(request)),
                "remote deploy kubeconfig received and redacted before persistence\n"
                        + "target namespace: " + namespace + "\n"
                        + "global migration lock enabled\n"
                        + "one-by-one image migration: migrate image then cleanup source tar\n",
                capturedUserId
        );
        Long packageId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (Boolean.TRUE.equals(request.getGlobalMigrationLock())) {
            publishGlobalMigrationLock(loadDeployPackageRecord(packageId));
        }
        startRemoteDeployAsync(packageId, packageName, request, capturedTenantId, capturedUserId);
        return loadDeployPackageRecord(packageId);
    }

    @Override
    public AdminDeployPackagePreflightVO preflightDeployPackage(AdminDeployPackageCreateDTO dto) {
        AdminDeployPackageCreateDTO request = normalizeDeployPackageRequest(dto);
        request.setRemoteDeploy(true);
        DeployTargetDefaults defaults = resolveDeployTargetDefaults(request);
        AdminDeployPackagePreflightVO vo = new AdminDeployPackagePreflightVO();
        vo.setTargetClusterType(defaults.targetClusterType);
        vo.setStorageClassName(defaults.storageClassName);
        vo.setExternalExposureType(defaults.externalExposureType);

        List<String> checks = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<String> storageClasses = new ArrayList<>();
        List<String> ingressClasses = new ArrayList<>();
        List<String> nodeExternalIps = new ArrayList<>();

        if (TARGET_CLUSTER_ACK.equals(defaults.targetClusterType)
                && EXPOSURE_LOAD_BALANCER.equals(defaults.externalExposureType)) {
            warnings.add("ACK LoadBalancer 会创建云 SLB 并产生云资源费用，请确认后再部署");
        }
        if (EXPOSURE_NONE.equals(defaults.externalExposureType)) {
            warnings.add("公网入口为 none，部署后仅集群内可访问；需要公网访问时请选择 NodePort、Ingress 或 LoadBalancer");
        }
        if (!StringUtils.hasText(request.getKubeconfig())) {
            errors.add("kubeconfig 不能为空");
            vo.setChecks(checks);
            vo.setWarnings(warnings);
            vo.setErrors(errors);
            vo.setStorageClasses(storageClasses);
            vo.setIngressClasses(ingressClasses);
            vo.setNodeExternalIps(nodeExternalIps);
            vo.setPassed(false);
            return vo;
        }

        Path kubeconfigPath = null;
        try {
            kubeconfigPath = writeTemporaryKubeconfig(request.getKubeconfig());
            runKubectlPreflight(kubeconfigPath, request.getKubeContext(), "get nodes -o wide", 60);
            checks.add("kubectl get nodes 通过");
            nodeExternalIps.addAll(parseK8sNodeExternalIps(runKubectlPreflight(kubeconfigPath, request.getKubeContext(),
                    "get nodes -o wide --no-headers", 60)));
            if ((EXPOSURE_NODE_PORT.equals(defaults.externalExposureType)
                    || EXPOSURE_LOAD_BALANCER.equals(defaults.externalExposureType))
                    && nodeExternalIps.isEmpty()) {
                warnings.add("目标集群节点未显示 ExternalIP，公网访问可能还需要云安全组、EIP 或反向代理");
            }

            String storageClassOutput = runKubectlPreflight(kubeconfigPath, request.getKubeContext(),
                    "get storageclass --no-headers", 60);
            storageClasses.addAll(parseKubectlFirstColumn(storageClassOutput));
            checks.add("kubectl get storageclass 通过");
            if (StringUtils.hasText(defaults.storageClassName)) {
                runKubectlPreflight(kubeconfigPath, request.getKubeContext(),
                        "get storageclass " + quoteForBash(defaults.storageClassName), 60);
                checks.add("StorageClass " + defaults.storageClassName + " 存在");
            } else {
                warnings.add("未指定 StorageClass，PVC 将依赖目标集群默认 StorageClass");
            }

            if (EXPOSURE_INGRESS.equals(defaults.externalExposureType)) {
                String ingressClassOutput = runKubectlPreflight(kubeconfigPath, request.getKubeContext(),
                        "get ingressclass --no-headers", 60);
                ingressClasses.addAll(parseKubectlFirstColumn(ingressClassOutput));
                checks.add("kubectl get ingressclass 通过");
                if (StringUtils.hasText(defaults.ingressClassName)) {
                    runKubectlPreflight(kubeconfigPath, request.getKubeContext(),
                            "get ingressclass " + quoteForBash(defaults.ingressClassName), 60);
                    checks.add("IngressClass " + defaults.ingressClassName + " 存在");
                } else {
                    warnings.add("未指定 IngressClass，将使用目标集群默认 Ingress 控制器");
                }
            }
        } catch (Exception e) {
            errors.add(sanitizeDeployLog(e.getMessage()));
        } finally {
            deleteTemporaryKubeconfigQuietly(kubeconfigPath);
        }
        vo.setChecks(checks);
        vo.setWarnings(warnings);
        vo.setErrors(errors);
        vo.setStorageClasses(storageClasses);
        vo.setIngressClasses(ingressClasses);
        vo.setNodeExternalIps(nodeExternalIps);
        vo.setPassed(errors.isEmpty());
        return vo;
    }

    @Override
    public AdminDeployPackageRecordVO createRemoteMigration(AdminDeployPackageCreateDTO dto) {
        return createRemoteDeploy(dto);
    }

    @Override
    public AdminDeployPackagePreflightVO preflightRemoteMigration(AdminDeployPackageCreateDTO dto) {
        return preflightDeployPackage(dto);
    }

    @Override
    public AdminDeployPackageRecordVO createLinuxSshMigration(AdminDeployPackageCreateDTO dto) {
        AdminDeployPackageCreateDTO request = normalizeDeployPackageRequest(dto);
        String sshHost = trimToEmpty(request.getSshHost());
        String sshUsername = trimToDefault(request.getSshUsername(), "root");
        int sshPort = request.getSshPort() == null ? 22 : Math.max(1, Math.min(request.getSshPort(), 65535));
        String deployMode = trimToDefault(request.getLinuxDeployMode(), "k8s");
        if (!StringUtils.hasText(sshHost)) {
            throw new BadRequestException("SSH 服务器地址不能为空");
        }
        if (!StringUtils.hasText(sshUsername)) {
            throw new BadRequestException("SSH 用户名不能为空");
        }
        if (!StringUtils.hasText(request.getSshPassword())) {
            throw new BadRequestException("SSH 密码不能为空");
        }
        if (!"SSH_DEPLOY".equals(trimToEmpty(request.getConfirmText()))) {
            throw new BadRequestException("Linux SSH 部署需要输入确认文本 SSH_DEPLOY");
        }

        request.setRemoteDeploy(true);
        request.setDeployMode("linux-ssh");
        request.setSshPort(sshPort);
        request.setSshUsername(sshUsername);
        request.setLinuxDeployMode(deployMode);
        request.setImageArtifactMode(IMAGE_ARTIFACT_REGISTRY);
        request.setExportRuntimeArtifacts(true);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String packageName = "rk_web_linux_ssh_deploy_" + timestamp;
        LocalDateTime startedAt = LocalDateTime.now();
        Long capturedUserId = UserContext.getUser();
        Long capturedTenantId = TenantContext.getTenantId();
        String targetLabel = sshUsername + "@" + sshHost + ":" + sshPort;
        String initialLog = buildLinuxSshMigrationPlanLog(request, targetLabel);
        int initialImageCount = initialRemoteDeployImageCount(request);
        int estimatedRemainingSeconds = computeEstimatedRemainingSeconds(5, initialImageCount);
        jdbcTemplate.update(
                "INSERT INTO ops_deploy_package_record (package_name, status, progress, package_mode, delivery_mode, image_artifact_mode, deploy_mode, remote_deploy, remote_cluster_name, deploy_started_at, current_step, uploaded_images, total_images, upload_percent, registry_pull_package, streaming_migration, global_migration_lock, estimated_remaining_seconds, migration_started_at, migration_updated_at, delete_after_download, note_text, request_json, logs, created_by) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                packageName,
                "RUNNING",
                5,
                request.getPackageMode(),
                request.getDeliveryMode(),
                request.getImageArtifactMode(),
                "linux-ssh",
                1,
                targetLabel,
                startedAt,
                "linux ssh task created",
                0,
                initialImageCount,
                0,
                IMAGE_ARTIFACT_REGISTRY.equals(request.getImageArtifactMode()) ? 1 : 0,
                1,
                1,
                estimatedRemainingSeconds,
                startedAt,
                startedAt,
                0,
                request.getNote(),
                toJson(buildLinuxSshMigrationRequestForStorage(request)),
                initialLog,
                capturedUserId
        );
        Long packageId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        publishGlobalMigrationLock(loadDeployPackageRecord(packageId));
        recordOperationAudit(
                "REMOTE_MIGRATION_LINUX_SSH_DEPLOY",
                trimToDefault(request.getTargetNamespace(), "shetuanguanlixitong"),
                "RemoteMigration",
                String.valueOf(packageId),
                "CREATE_LINUX_SSH_DEPLOY",
                "success",
                "linux ssh deploy task started",
                toJson(Map.of("packageId", packageId, "target", targetLabel, "linuxDeployMode", deployMode)),
                "SSH password was accepted for this request but was not persisted"
        );
        startLinuxSshDeployAsync(packageId, packageName, request, capturedTenantId, capturedUserId);
        return loadDeployPackageRecord(packageId);
    }

    private void startLinuxSshDeployAsync(Long packageId, String packageName, AdminDeployPackageCreateDTO request, Long capturedTenantId, Long capturedUserId) {
        Runnable task = () -> {
            if (capturedTenantId != null) {
                TenantContext.setTenantId(capturedTenantId);
            }
            if (capturedUserId != null) {
                UserContext.setUser(capturedUserId);
            }
            try {
                runLinuxSshDeploy(packageId, packageName, request);
            } catch (Exception e) {
                log.error("linux ssh deploy async task failed, packageId={}", packageId, e);
            } finally {
                TenantContext.clear();
                UserContext.removeUser();
            }
        };
        Thread thread = new Thread(task, "rk-linux-ssh-deploy-" + packageId);
        thread.setDaemon(true);
        thread.start();
    }

    private void runLinuxSshDeploy(Long packageId, String packageName, AdminDeployPackageCreateDTO request) {
        Session session = null;
        Path runtimeArtifactDir = null;
        Path archivePath = null;
        String linuxSshPackageRemotePath = null;
        String linuxSshPackageDownloadTokenKey = null;
        String password = trimToEmpty(request.getSshPassword());
        String mode = trimToDefault(request.getLinuxDeployMode(), "k8s");
        String namespace = sanitizeCleanupToken(trimToDefault(request.getTargetNamespace(), "shetuanguanlixitong"), "namespace");
        String targetRoot = normalizeLinuxSshTargetPath(request.getTargetPath());
        String releaseDir = targetRoot + "/releases/" + packageName;
        String remotePackagePath = targetRoot + "/packages/" + packageName + ".zip";
        boolean copyServices = Boolean.TRUE.equals(request.getCopyServices()) && !"runtime-only".equals(mode);
        try {
            refreshGlobalMigrationLock(packageId);
            updateDeployPackageStep(packageId, 8, "linux ssh checking connection", null, "linux ssh checking connection");
            session = openLinuxSshSession(request, packageId);
            runLinuxSshStage(session, packageId, 10, "linux ssh checking connection", buildLinuxSshProbeScript(), 60, password);

            if (copyServices && shouldPushRuntimeImagesBeforeRemoteDeploy(request)) {
                assertRuntimeExportDiskHeadroom(packageId, shouldRelaxRemoteDeployDiskPreflight(request));
                updateDeployPackageStep(packageId, 14, "linux ssh exporting current runtime images to registry", null,
                        "linux ssh exporting current runtime images to registry");
                runtimeArtifactDir = exportRuntimeArtifactsToDirectory(request, packageName, packageId);
                runRegistryPushForRuntimeArtifacts(runtimeArtifactDir, request, packageId);
                updateDeployPackageStep(packageId, 18, "linux ssh registry images ready", null,
                        "linux ssh registry images ready");
            }

            runLinuxSshStage(session, packageId, 22, "linux ssh preparing target directory",
                    buildLinuxSshPrepareScript(request, targetRoot), 600, password);

            if (copyServices) {
                updateDeployPackageStep(packageId, 28, "linux ssh building deploy package", null,
                        "linux ssh building deploy package");
                List<String> databases = normalizeDeployPackageDatabases(request.getDatabases());
                Map<String, Object> manifest = buildDeployPackageManifest(request, databases);
                archivePath = buildDeployPackageArchiveFile(manifest, request, databases, runtimeArtifactDir, packageId);
                updateDeployPackageStep(packageId, 38, "linux ssh package built", null,
                        "linux ssh package built: " + archivePath.getFileName());
                try {
                    LinuxSshPackageDownload download = prepareLinuxSshPackageDownloadUrl(packageId, archivePath, password);
                    linuxSshPackageRemotePath = download.remotePath();
                    linuxSshPackageDownloadTokenKey = download.tokenKey();
                    runLinuxSshStage(session, packageId, 42, "linux ssh downloading deploy package",
                            buildLinuxSshPackageDownloadScript(download.primaryDownloadUrl(), download.proxyDownloadUrl(), remotePackagePath, Files.size(archivePath)),
                            resolveLinuxSshPackageDownloadTimeoutSeconds(Files.size(archivePath)), password);
                    verifyLinuxSshPackageUpload(session, packageId, archivePath, remotePackagePath, password);
                    updateDeployPackageStep(packageId, 52, "linux ssh package downloaded", null,
                            "linux ssh package downloaded to " + remotePackagePath);
                } catch (Exception downloadError) {
                    appendLinuxSshOutput(packageId, 42,
                            "linux ssh package download failed, trying resumable SFTP upload: " + downloadError.getMessage(),
                            password);
                    session = reopenLinuxSshSessionForPackageUpload(session, request, packageId);
                    try {
                        session = uploadLinuxSshPackageByResumableSftp(session, packageId, archivePath, remotePackagePath, request, password);
                    } catch (Exception sftpError) {
                        appendLinuxSshOutput(packageId, 42,
                                "linux ssh package resumable sftp failed, falling back to chunked SSH upload: " + sftpError.getMessage(),
                                password);
                        session = reopenLinuxSshSessionForPackageUpload(session, request, packageId);
                        session = uploadLinuxSshPackage(session, packageId, archivePath, remotePackagePath, request, password);
                    }
                }
            }

            if (Boolean.TRUE.equals(request.getInstallDocker()) || "docker-compose".equals(mode)) {
                runLinuxSshStage(session, packageId, 58, "linux ssh installing docker runtime",
                        buildLinuxSshDockerInstallScript(request), 1200, password);
            }

            if ("k8s".equals(mode) && Boolean.TRUE.equals(request.getInstallK8s())) {
                runLinuxSshStage(session, packageId, 68, "linux ssh installing k8s runtime",
                        buildLinuxSshK8sInstallScript(request), Math.max(900, resolveRemoteDataPlaneTimeoutSeconds(request)), password);
            }

            if (copyServices) {
                runLinuxSshStage(session, packageId, 82, 93, "linux ssh deploying application stack",
                        buildLinuxSshDeployScript(request, mode, namespace, targetRoot, releaseDir, remotePackagePath), Math.max(7200, resolveRemoteDataPlaneTimeoutSeconds(request)), password);
                runLinuxSshStage(session, packageId, 94, "linux ssh checking deployed services",
                        buildLinuxSshHealthCheckScript(mode, namespace, releaseDir), 300, password);
            } else {
                updateDeployPackageStep(packageId, 90, "linux ssh runtime-only completed", null,
                        "linux ssh runtime-only mode skipped service package deploy");
            }

            LocalDateTime finishedAt = LocalDateTime.now();
            jdbcTemplate.update(
                    "UPDATE ops_deploy_package_record SET status = 'SUCCESS', progress = 100, current_step = ?, current_image = NULL, deploy_finished_at = ?, estimated_remaining_seconds = 0, migration_updated_at = ?, logs = CONCAT(COALESCE(logs,''), ?), update_time = ? WHERE id = ?",
                    "linux ssh deploy completed",
                    finishedAt,
                    finishedAt,
                    "linux ssh deploy completed\n",
                    finishedAt,
                    packageId
            );
        } catch (Exception e) {
            String message = sanitizeLinuxSshOutput(e.getMessage(), password);
            log.error("linux ssh deploy failed, packageId={}, message={}", packageId, message);
            LocalDateTime finishedAt = LocalDateTime.now();
            jdbcTemplate.update(
                    "UPDATE ops_deploy_package_record SET status = 'FAILED', progress = 100, current_step = 'linux ssh deploy failed', deploy_finished_at = ?, estimated_remaining_seconds = 0, migration_updated_at = ?, logs = CONCAT(COALESCE(logs,''), ?), update_time = ? WHERE id = ?",
                    finishedAt,
                    finishedAt,
                    "linux ssh deploy failed: " + message + "\n",
                    finishedAt,
                    packageId
            );
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
            deleteTemporaryKubeconfigQuietly(archivePath);
            cleanupLinuxSshPackageDownload(linuxSshPackageRemotePath, linuxSshPackageDownloadTokenKey, packageId);
            cleanupRuntimeArtifactDirectory(runtimeArtifactDir, packageId);
            clearGlobalMigrationLock();
        }
    }

    private Session openLinuxSshSession(AdminDeployPackageCreateDTO request, Long packageId) throws Exception {
        String host = trimToEmpty(request.getSshHost());
        String username = trimToDefault(request.getSshUsername(), "root");
        int port = request.getSshPort() == null ? 22 : Math.max(1, Math.min(request.getSshPort(), 65535));
        String password = trimToEmpty(request.getSshPassword());
        Exception lastError = null;
        for (int attempt = 1; attempt <= LINUX_SSH_CONNECT_MAX_ATTEMPTS; attempt++) {
            Session session = null;
            try {
                JSch jsch = new JSch();
                session = jsch.getSession(username, host, port);
                session.setPassword(password);
                Properties config = new Properties();
                config.put("StrictHostKeyChecking", "no");
                config.put("PreferredAuthentications", "password,keyboard-interactive");
                session.setConfig(config);
                session.setServerAliveInterval(15000);
                session.setServerAliveCountMax(12);
                session.connect(20000);
                if (attempt > 1) {
                    appendLinuxSshOutput(packageId, 8,
                            "linux ssh connection established after retry " + attempt + "/" + LINUX_SSH_CONNECT_MAX_ATTEMPTS,
                            password);
                }
                return session;
            } catch (Exception e) {
                lastError = e;
                if (session != null && session.isConnected()) {
                    session.disconnect();
                } else if (session != null) {
                    try {
                        session.disconnect();
                    } catch (Exception ignored) {
                        // ignore cleanup failure between retry attempts
                    }
                }
                if (attempt >= LINUX_SSH_CONNECT_MAX_ATTEMPTS || !isTransientLinuxSshConnectFailure(e)) {
                    throw e;
                }
                appendLinuxSshOutput(packageId, 8,
                        "linux ssh connection retry " + attempt + "/" + LINUX_SSH_CONNECT_MAX_ATTEMPTS
                                + " after transient failure: " + e.getMessage(),
                        password);
                Thread.sleep(LINUX_SSH_CONNECT_RETRY_DELAY_MS);
            }
        }
        throw lastError == null ? new IllegalStateException("linux ssh connection failed") : lastError;
    }

    private Session reopenLinuxSshSessionForPackageUpload(Session currentSession, AdminDeployPackageCreateDTO request, Long packageId) throws Exception {
        if (currentSession != null) {
            try {
                currentSession.disconnect();
            } catch (Exception ignored) {
                // The next fresh session is authoritative for the long package upload.
            }
        }
        appendLinuxSshOutput(packageId, 42, "linux ssh reconnecting before package upload", trimToEmpty(request.getSshPassword()));
        return openLinuxSshSession(request, packageId);
    }

    private boolean isTransientLinuxSshConnectFailure(Exception error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof java.net.SocketTimeoutException) {
                return true;
            }
            if (current instanceof java.net.SocketException) {
                String message = current.getMessage();
                if (!StringUtils.hasText(message) || isTransientLinuxSshConnectFailureMessage(message)) {
                    return true;
                }
            }
            if (current instanceof JSchException && isTransientLinuxSshConnectFailureMessage(current.getMessage())) {
                return true;
            }
            current = current.getCause();
        }
        return isTransientLinuxSshConnectFailureMessage(error == null ? null : error.getMessage());
    }

    private boolean isTransientLinuxSshConnectFailureMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("connection reset")
                || lower.contains("ssh protocol banner")
                || lower.contains("connection timed out")
                || lower.contains("sockettimeoutexception")
                || lower.contains("read timed out")
                || lower.contains("connection refused");
    }

    private String runLinuxSshStage(Session session, Long packageId, int progress, String currentStep, String script, int timeoutSeconds, String password) throws Exception {
        return runLinuxSshStage(session, packageId, progress, progress, currentStep, script, timeoutSeconds, password);
    }

    private String runLinuxSshStage(Session session, Long packageId, int progress, int nextProgress, String currentStep, String script, int timeoutSeconds, String password) throws Exception {
        updateDeployPackageStep(packageId, progress, currentStep, null, currentStep);
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setPty(true);
        channel.setCommand("bash -lc " + quoteForBash(script));
        InputStream stdout = channel.getInputStream();
        InputStream stderr = channel.getExtInputStream();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        channel.connect(10000);
        long deadline = System.currentTimeMillis() + Math.max(30, timeoutSeconds) * 1000L;
        byte[] buffer = new byte[4096];
        long lastFlushAt = 0L;
        long stageStartedAt = System.currentTimeMillis();
        long lastHeartbeatAt = stageStartedAt;
        try {
            while (!channel.isClosed()) {
                boolean readAny = false;
                readAny |= readLinuxSshAvailable(stdout, output, buffer);
                readAny |= readLinuxSshAvailable(stderr, output, buffer);
                long now = System.currentTimeMillis();
                if (readAny && now - lastFlushAt > 2000L) {
                    int outputProgress = computeLinuxSshStageHeartbeatProgress(progress, nextProgress, now - stageStartedAt, timeoutSeconds);
                    appendLinuxSshOutput(packageId, outputProgress, output.toString(StandardCharsets.UTF_8), password);
                    output.reset();
                    lastFlushAt = now;
                    lastHeartbeatAt = now;
                }
                if (now - lastHeartbeatAt > 15000L) {
                    long elapsedSeconds = Math.max(1L, (now - stageStartedAt) / 1000L);
                    int heartbeatProgress = computeLinuxSshStageHeartbeatProgress(progress, nextProgress, now - stageStartedAt, timeoutSeconds);
                    updateDeployPackageStep(packageId, heartbeatProgress, currentStep, null, currentStep + " heartbeat" + ": " + elapsedSeconds + "s");
                    appendLinuxSshOutput(packageId, heartbeatProgress, "linux ssh stage heartbeat: " + currentStep + " elapsed " + elapsedSeconds + "s", password);
                    lastHeartbeatAt = now;
                }
                if (now > deadline) {
                    channel.disconnect();
                    throw new IllegalStateException(currentStep + " timed out after " + timeoutSeconds + "s");
                }
                Thread.sleep(readAny ? 50L : 250L);
            }
            while (readLinuxSshAvailable(stdout, output, buffer) || readLinuxSshAvailable(stderr, output, buffer)) {
                // drain remaining output
            }
            String text = output.toString(StandardCharsets.UTF_8);
            appendLinuxSshOutput(packageId, nextProgress, text, password);
            int exitStatus = channel.getExitStatus();
            if (exitStatus != 0) {
                throw new IllegalStateException(formatLinuxSshStageFailure(currentStep, exitStatus, text, password));
            }
            return text;
        } finally {
            channel.disconnect();
        }
    }

    private String formatLinuxSshStageFailure(String currentStep, int exitStatus, String text, String password) {
        String safeOutput = sanitizeLinuxSshOutput(text, password);
        StringBuilder message = new StringBuilder(currentStep)
                .append(" failed with exit code ")
                .append(exitStatus);
        if (exitStatus == -1) {
            message.append(" (exit code -1 usually means the SSH channel was interrupted; check whether the target host rebooted or the public tunnel dropped)");
        }
        message.append(": ").append(safeOutput);
        return message.toString();
    }

    private int computeLinuxSshStageHeartbeatProgress(int progress, int nextProgress, long elapsedMillis, int timeoutSeconds) {
        int start = Math.max(0, Math.min(99, progress));
        int end = Math.max(start, Math.min(99, nextProgress));
        if (end <= start) {
            return start;
        }
        long elapsedSeconds = Math.max(0L, elapsedMillis / 1000L);
        long progressWindowSeconds = Math.max(60L, Math.min(Math.max(60, timeoutSeconds), 1800));
        double ratio = Math.min(0.98d, elapsedSeconds / (double) progressWindowSeconds);
        int computed = start + (int) Math.floor((end - start) * ratio);
        if (elapsedSeconds >= 15L && computed <= start) {
            computed = start + 1;
        }
        return Math.max(start, Math.min(end, computed));
    }

    private boolean readLinuxSshAvailable(InputStream inputStream, ByteArrayOutputStream output, byte[] buffer) throws Exception {
        boolean readAny = false;
        while (inputStream != null && inputStream.available() > 0) {
            int len = inputStream.read(buffer, 0, Math.min(buffer.length, inputStream.available()));
            if (len < 0) {
                break;
            }
            output.write(buffer, 0, len);
            readAny = true;
        }
        return readAny;
    }

    private void appendLinuxSshOutput(Long packageId, int progress, String text, String password) {
        String safe = sanitizeLinuxSshOutput(text, password);
        if (!StringUtils.hasText(safe)) {
            return;
        }
        updateDeployPackageProgress(packageId, progress, safe);
    }

    private String sanitizeLinuxSshOutput(String text, String password) {
        String safe = sanitizeDeployLog(text);
        if (StringUtils.hasText(password)) {
            safe = safe.replace(password, "[REDACTED]");
        }
        safe = safe.replaceAll("(?im)(password\\s*[=:]\\s*)\\S+", "$1[REDACTED]");
        safe = safe.replaceAll("(?im)(secret\\s*[=:]\\s*)\\S+", "$1[REDACTED]");
        safe = safe.replaceAll("(?im)(access[-_]?key\\s*[=:]\\s*)\\S+", "$1[REDACTED]");
        safe = safe.replaceAll("(?im)(secret[-_]?key\\s*[=:]\\s*)\\S+", "$1[REDACTED]");
        return limitText(safe.trim(), 5000);
    }

    private LinuxSshPackageDownload prepareLinuxSshPackageDownloadUrl(Long packageId, Path archivePath, String password) throws Exception {
        updateDeployPackageStep(packageId, 40, "linux ssh publishing deploy package", null,
                "linux ssh publishing deploy package for target-host download");
        String fileName = "linux-ssh-" + packageId + "-" + archivePath.getFileName();
        String remotePath = backupStorageService.storeFile(fileName, archivePath);
        String token = createLinuxSshPackageDownloadToken(packageId, remotePath);
        String tokenKey = linuxSshPackageDownloadTokenKey(packageId);
        String baseUrl = resolveLinuxSshDownloadBaseUrl();
        String proxyDownloadUrl = baseUrl + "/admin/ops/public/deploy-package/" + packageId
                + "/linux-ssh-download?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        String publicMinioUrl = buildLinuxSshPublicMinioUrl(baseUrl, remotePath);
        appendLinuxSshOutput(packageId, 40,
                "linux ssh deploy package is ready for target-host download: " + formatBytes(Files.size(archivePath))
                        + ", primary=public MinIO URL",
                password);
        return new LinuxSshPackageDownload(remotePath, tokenKey, publicMinioUrl, proxyDownloadUrl);
    }

    private String buildLinuxSshPublicMinioUrl(String baseUrl, String remotePath) {
        String normalizedBaseUrl = resolveLinuxSshPublicOrigin(baseUrl);
        String normalizedPath = trimToEmpty(remotePath).replace('\\', '/');
        String encodedPath = Arrays.stream(normalizedPath.split("/+"))
                .filter(StringUtils::hasText)
                .map(this::encodePathSegment)
                .collect(Collectors.joining("/"));
        return normalizedBaseUrl + "/minio-files/" + encodedPath;
    }

    private String resolveLinuxSshPublicOrigin(String baseUrl) {
        String resolved = PublicBaseUrlResolver.resolve(baseUrl);
        try {
            URI uri = URI.create(resolved);
            if (StringUtils.hasText(uri.getScheme()) && StringUtils.hasText(uri.getRawAuthority())) {
                return uri.getScheme() + "://" + uri.getRawAuthority();
            }
        } catch (Exception ignored) {
            // Fall back to the already validated public base URL below.
        }
        String fallback = resolveLinuxSshDownloadBaseUrl();
        if (StringUtils.hasText(fallback)) {
            return fallback;
        }
        return "https://example.com";
    }

    private String resolveLinuxSshDownloadBaseUrl() {
        String resolved = PublicBaseUrlResolver.resolve(deployPackagePublicBaseUrl);
        if (StringUtils.hasText(resolved)) {
            return sanitizeOpsUrl(resolved);
        }
        resolved = PublicBaseUrlResolver.resolve(gatewayUrl);
        if (StringUtils.hasText(resolved)) {
            return sanitizeOpsUrl(resolved);
        }
        return "https://example.com";
    }

    private String createLinuxSshPackageDownloadToken(Long packageId, String remotePath) {
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        String tokenHash = sha256Hex(token);
        String value = tokenHash + "\n" + trimToEmpty(remotePath);
        stringRedisTemplate.opsForValue().set(
                linuxSshPackageDownloadTokenKey(packageId),
                value,
                LINUX_SSH_PACKAGE_DOWNLOAD_TOKEN_TTL
        );
        return token;
    }

    private String linuxSshPackageDownloadTokenKey(Long packageId) {
        return LINUX_SSH_PACKAGE_DOWNLOAD_TOKEN_KEY_PREFIX + packageId;
    }

    private String buildLinuxSshDnsRepairScript() {
        return "ensure_linux_ssh_dns() {\n"
                + "  if getent hosts mirrors.aliyun.com >/dev/null 2>&1 && getent hosts example.com >/dev/null 2>&1; then echo \"linux ssh DNS check passed\"; return 0; fi\n"
                + "  echo \"linux ssh DNS check failed, applying configured resolvers\"\n"
                + "  DNS_SERVERS=\"${RK_LINUX_SSH_DNS_SERVERS:-127.0.0.1}\"\n"
                + "  if command -v nmcli >/dev/null 2>&1; then\n"
                + "    CON_NAME=\"$(nmcli -t -f NAME,DEVICE connection show --active 2>/dev/null | awk -F: '$2 != \"lo\" {print $1; exit}')\"\n"
                + "    if [ -n \"${CON_NAME:-}\" ]; then nmcli connection modify \"$CON_NAME\" ipv4.ignore-auto-dns yes ipv4.dns \"$DNS_SERVERS\" >/dev/null 2>&1 || true; nmcli connection up \"$CON_NAME\" >/dev/null 2>&1 || true; fi\n"
                + "  fi\n"
                + "  if [ -f /etc/resolv.conf ]; then cp -f /etc/resolv.conf /etc/resolv.conf.rk-backup 2>/dev/null || true; fi\n"
                + "  { for dns in $DNS_SERVERS; do echo \"nameserver $dns\"; done; } >/etc/resolv.conf\n"
                + "  echo \"linux ssh DNS repair applied: $DNS_SERVERS\"\n"
                + "  getent hosts mirrors.aliyun.com >/dev/null 2>&1 || { echo \"linux ssh DNS still cannot resolve mirrors.aliyun.com\" >&2; return 1; }\n"
                + "  getent hosts example.com >/dev/null 2>&1 || { echo \"linux ssh DNS still cannot resolve example.com\" >&2; return 1; }\n"
                + "  echo \"linux ssh DNS check passed\"\n"
                + "}\n";
    }

    private String buildLinuxSshPackageDownloadScript(String primaryDownloadUrl, String proxyDownloadUrl, String remotePackagePath, long expectedBytes) {
        String tmpRemotePackagePath = remotePackagePath + ".part-download";
        int downloadTimeoutSeconds = resolveLinuxSshPackageDownloadTimeoutSeconds(expectedBytes);
        return "set -euo pipefail\n"
                + buildLinuxSshDnsRepairScript()
                + "REMOTE_FILE=" + quoteForBash(remotePackagePath) + "\n"
                + "TMP_FILE=" + quoteForBash(tmpRemotePackagePath) + "\n"
                + "DOWNLOAD_URL=" + quoteForBash(primaryDownloadUrl) + "\n"
                + "PROXY_DOWNLOAD_URL=" + quoteForBash(proxyDownloadUrl) + "\n"
                + "EXPECTED_SIZE=" + expectedBytes + "\n"
                + "DOWNLOAD_MAX_TIME=" + downloadTimeoutSeconds + "\n"
                + "DOWNLOAD_MIN_BPS=" + LINUX_SSH_PACKAGE_DOWNLOAD_MIN_BYTES_PER_SECOND + "\n"
                + "DOWNLOAD_LOW_SPEED_SECONDS=" + LINUX_SSH_PACKAGE_DOWNLOAD_LOW_SPEED_SECONDS + "\n"
                + "case \"$DOWNLOAD_URL\" in http://*|https://*) ;; *) DOWNLOAD_URL=\"$PROXY_DOWNLOAD_URL\" ;; esac\n"
                + "case \"$DOWNLOAD_URL\" in http://*|https://*) ;; *) echo \"linux ssh deploy package download URL is not absolute: $DOWNLOAD_URL\" >&2; exit 1 ;; esac\n"
                + "ensure_linux_ssh_dns\n"
                + "mkdir -p \"$(dirname \"$REMOTE_FILE\")\"\n"
                + "rm -f \"$TMP_FILE\"\n"
                + "echo \"linux ssh downloading deploy package from public MinIO URL\"\n"
                + "if ! curl -fL --retry 2 --retry-delay 3 --connect-timeout 20 --speed-limit \"$DOWNLOAD_MIN_BPS\" --speed-time \"$DOWNLOAD_LOW_SPEED_SECONDS\" --max-time \"$DOWNLOAD_MAX_TIME\" \"$DOWNLOAD_URL\" -o \"$TMP_FILE\"; then\n"
                + "  rm -f \"$TMP_FILE\"\n"
                + "  echo \"linux ssh package download is too slow; switching to chunked SSH upload\" >&2\n"
                + "  exit 1\n"
                + "fi\n"
                + "REMOTE_SIZE=$(wc -c < \"$TMP_FILE\" | tr -d ' ')\n"
                + "echo \"linux ssh downloaded package size: $REMOTE_SIZE\"\n"
                + "if [ \"$REMOTE_SIZE\" != \"$EXPECTED_SIZE\" ]; then echo \"linux ssh downloaded package size mismatch: expected=$EXPECTED_SIZE actual=$REMOTE_SIZE\" >&2; exit 1; fi\n"
                + "mv -f \"$TMP_FILE\" \"$REMOTE_FILE\"\n";
    }

    private int resolveLinuxSshPackageDownloadTimeoutSeconds(long expectedBytes) {
        long expectedSeconds = Math.max(1L, (expectedBytes + LINUX_SSH_PACKAGE_DOWNLOAD_MIN_BYTES_PER_SECOND - 1)
                / LINUX_SSH_PACKAGE_DOWNLOAD_MIN_BYTES_PER_SECOND);
        long timeout = expectedSeconds + LINUX_SSH_PACKAGE_DOWNLOAD_LOW_SPEED_SECONDS + 120L;
        return (int) Math.max(LINUX_SSH_PACKAGE_DOWNLOAD_MIN_TIMEOUT_SECONDS,
                Math.min(LINUX_SSH_PACKAGE_DOWNLOAD_MAX_TIMEOUT_SECONDS, timeout));
    }

    private void cleanupLinuxSshPackageDownload(String remotePath, String tokenKey, Long packageId) {
        if (StringUtils.hasText(tokenKey)) {
            try {
                stringRedisTemplate.delete(tokenKey);
            } catch (Exception e) {
                log.warn("cleanup linux ssh package download token failed, packageId={}: {}", packageId, e.getMessage());
            }
        }
        if (StringUtils.hasText(remotePath)) {
            try {
                backupStorageService.delete(remotePath);
            } catch (Exception e) {
                log.warn("cleanup linux ssh package object failed, packageId={}, path={}: {}", packageId, remotePath, e.getMessage());
            }
        }
    }

    private Session uploadLinuxSshPackageByResumableSftp(Session session, Long packageId, Path archivePath, String remotePackagePath,
                                                         AdminDeployPackageCreateDTO request, String password) throws Exception {
        long totalBytes = Files.size(archivePath);
        updateDeployPackageStep(packageId, 42, "linux ssh uploading deploy package by resumable sftp", null,
                "linux ssh uploading deploy package by resumable sftp: " + archivePath.getFileName()
                        + " (" + formatBytes(totalBytes) + ")");
        String tmpRemotePackagePath = remotePackagePath + ".part-sftp";
        String prepareScript = "set -euo pipefail\n"
                + "REMOTE_FILE=" + quoteForBash(remotePackagePath) + "\n"
                + "TMP_FILE=" + quoteForBash(tmpRemotePackagePath) + "\n"
                + "EXPECTED_SIZE=" + totalBytes + "\n"
                + "mkdir -p \"$(dirname \"$REMOTE_FILE\")\"\n"
                + "touch \"$TMP_FILE\"\n"
                + "CURRENT_SIZE=$(wc -c < \"$TMP_FILE\" | tr -d ' ')\n"
                + "if [ \"$CURRENT_SIZE\" -gt \"$EXPECTED_SIZE\" ]; then truncate -s 0 \"$TMP_FILE\"; CURRENT_SIZE=0; fi\n"
                + "echo \"linux ssh resumable sftp current size: $CURRENT_SIZE\"\n";
        runLinuxSshStage(session, packageId, 42, "linux ssh preparing resumable sftp upload", prepareScript, 120, password);

        Exception lastError = null;
        for (int attempt = 1; attempt <= LINUX_SSH_PACKAGE_UPLOAD_SFTP_RETRY_ATTEMPTS; attempt++) {
            ChannelSftp sftp = null;
            try {
                session = ensureLinuxSshSession(session, request, packageId, 42, password);
                sftp = (ChannelSftp) session.openChannel("sftp");
                sftp.connect(20000);
                try {
                    sftp.setBulkRequests(64);
                } catch (JSchException ignored) {
                    // The upload still works with the library default.
                }

                long remoteSize = readLinuxSshSftpFileSize(sftp, tmpRemotePackagePath);
                if (remoteSize > totalBytes) {
                    appendLinuxSshOutput(packageId, 42,
                            "linux ssh resumable sftp remote part is larger than local package, resetting part file: "
                                    + formatBytes(remoteSize) + "/" + formatBytes(totalBytes),
                            password);
                    sftp.rm(tmpRemotePackagePath);
                    remoteSize = 0L;
                }
                if (remoteSize == totalBytes) {
                    appendLinuxSshOutput(packageId, 42,
                            "linux ssh resumable sftp package already uploaded: " + formatBytes(remoteSize),
                            password);
                    sftp.disconnect();
                    sftp = null;
                    finalizeLinuxSshPackageUpload(session, packageId, archivePath, remotePackagePath, tmpRemotePackagePath, password,
                            "linux ssh finalizing resumable sftp package upload", "linux ssh resumable sftp upload final size");
                    return session;
                }

                appendLinuxSshOutput(packageId, 42,
                        "linux ssh resumable sftp upload attempt " + attempt + "/"
                                + LINUX_SSH_PACKAGE_UPLOAD_SFTP_RETRY_ATTEMPTS + " from "
                                + formatBytes(remoteSize) + "/" + formatBytes(totalBytes),
                        password);
                try (InputStream fileInput = Files.newInputStream(archivePath);
                     OutputStream sftpOutput = sftp.put(tmpRemotePackagePath,
                             buildLinuxSshSftpProgressMonitor(packageId, remoteSize, totalBytes, password),
                             ChannelSftp.RESUME, 0L)) {
                    skipLinuxSshLocalFileInput(fileInput, remoteSize);
                    byte[] buffer = new byte[LINUX_SSH_PACKAGE_UPLOAD_CHUNK_BYTES];
                    long transferred = remoteSize;
                    long lastReportedAt = System.currentTimeMillis();
                    int lastPercent = (int) Math.min(100L, Math.round(transferred * 100.0d / Math.max(1L, totalBytes)));
                    int len;
                    while ((len = fileInput.read(buffer)) >= 0) {
                        sftpOutput.write(buffer, 0, len);
                        transferred += len;
                        int percent = (int) Math.min(100L, Math.round(transferred * 100.0d / Math.max(1L, totalBytes)));
                        long now = System.currentTimeMillis();
                        if (percent >= lastPercent + 10 || now - lastReportedAt > 10000L || percent >= 100) {
                            appendLinuxSshOutput(packageId, 42,
                                    "linux ssh resumable sftp upload progress: " + percent + "% ("
                                            + formatBytes(transferred) + "/" + formatBytes(totalBytes) + ")",
                                    password);
                            lastPercent = percent;
                            lastReportedAt = now;
                        }
                    }
                    sftpOutput.flush();
                }

                long uploadedSize = readLinuxSshSftpFileSize(sftp, tmpRemotePackagePath);
                if (uploadedSize != totalBytes) {
                    throw new IllegalStateException("linux ssh resumable sftp size mismatch: expected="
                            + totalBytes + " actual=" + uploadedSize);
                }
                sftp.disconnect();
                sftp = null;
                finalizeLinuxSshPackageUpload(session, packageId, archivePath, remotePackagePath, tmpRemotePackagePath, password,
                        "linux ssh finalizing resumable sftp package upload", "linux ssh resumable sftp upload final size");
                appendLinuxSshOutput(packageId, 42, "linux ssh resumable sftp upload finished", password);
                updateDeployPackageStep(packageId, 52, "linux ssh package uploaded", null,
                        "linux ssh package uploaded to " + remotePackagePath);
                return session;
            } catch (Exception e) {
                lastError = e;
                appendLinuxSshOutput(packageId, 42,
                        "linux ssh resumable sftp upload retry " + attempt + "/"
                                + LINUX_SSH_PACKAGE_UPLOAD_SFTP_RETRY_ATTEMPTS + ": " + e.getMessage(),
                        password);
                if (sftp != null) {
                    try {
                        sftp.disconnect();
                    } catch (Exception ignored) {
                        // reconnect below
                    }
                }
                try {
                    if (session != null) {
                        session.disconnect();
                    }
                } catch (Exception ignored) {
                    // reconnect below
                }
                session = null;
                if (attempt < LINUX_SSH_PACKAGE_UPLOAD_SFTP_RETRY_ATTEMPTS) {
                    Thread.sleep(3000L * attempt);
                }
            } finally {
                if (sftp != null) {
                    try {
                        sftp.disconnect();
                    } catch (Exception ignored) {
                        // already closed
                    }
                }
            }
        }
        throw lastError == null
                ? new IllegalStateException("linux ssh resumable sftp upload failed")
                : lastError;
    }

    private long readLinuxSshSftpFileSize(ChannelSftp sftp, String remotePath) throws SftpException {
        try {
            SftpATTRS attrs = sftp.stat(remotePath);
            return attrs == null ? 0L : Math.max(0L, attrs.getSize());
        } catch (SftpException e) {
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                return 0L;
            }
            throw e;
        }
    }

    private void skipLinuxSshLocalFileInput(InputStream inputStream, long bytes) throws Exception {
        long remaining = Math.max(0L, bytes);
        byte[] buffer = new byte[8192];
        while (remaining > 0) {
            long skipped = inputStream.skip(remaining);
            if (skipped > 0) {
                remaining -= skipped;
                continue;
            }
            int len = inputStream.read(buffer, 0, (int) Math.min(buffer.length, remaining));
            if (len < 0) {
                throw new IllegalStateException("local deploy package ended before resume offset " + bytes);
            }
            remaining -= len;
        }
    }

    private SftpProgressMonitor buildLinuxSshSftpProgressMonitor(Long packageId, long initialBytes, long totalBytes, String password) {
        return new SftpProgressMonitor() {
            private long uploadedBytes = initialBytes;
            private long lastReportAt = System.currentTimeMillis();

            @Override
            public void init(int operation, String source, String destination, long max) {
                appendLinuxSshOutput(packageId, 42,
                        "linux ssh resumable sftp stream opened: " + formatBytes(uploadedBytes)
                                + "/" + formatBytes(totalBytes),
                        password);
            }

            @Override
            public boolean count(long count) {
                uploadedBytes += Math.max(0L, count);
                long now = System.currentTimeMillis();
                if (now - lastReportAt > 15000L) {
                    appendLinuxSshOutput(packageId, 42,
                            "linux ssh resumable sftp heartbeat: " + formatBytes(uploadedBytes)
                                    + "/" + formatBytes(totalBytes),
                            password);
                    lastReportAt = now;
                }
                return true;
            }

            @Override
            public void end() {
                appendLinuxSshOutput(packageId, 42,
                        "linux ssh resumable sftp stream closed: " + formatBytes(uploadedBytes)
                                + "/" + formatBytes(totalBytes),
                        password);
            }
        };
    }

    private void finalizeLinuxSshPackageUpload(Session session, Long packageId, Path archivePath, String remotePackagePath,
                                               String tmpRemotePackagePath, String password, String currentStep,
                                               String sizeLogPrefix) throws Exception {
        long totalBytes = Files.size(archivePath);
        String finalizeScript = "set -euo pipefail\n"
                + "REMOTE_FILE=" + quoteForBash(remotePackagePath) + "\n"
                + "TMP_FILE=" + quoteForBash(tmpRemotePackagePath) + "\n"
                + "EXPECTED_SIZE=" + totalBytes + "\n"
                + "REMOTE_SIZE=$(wc -c < \"$TMP_FILE\" | tr -d ' ')\n"
                + "echo \"" + sizeLogPrefix + ": $REMOTE_SIZE\"\n"
                + "if [ \"$REMOTE_SIZE\" != \"$EXPECTED_SIZE\" ]; then echo \"" + sizeLogPrefix
                + " mismatch: expected=$EXPECTED_SIZE actual=$REMOTE_SIZE\" >&2; exit 1; fi\n"
                + "mv -f \"$TMP_FILE\" \"$REMOTE_FILE\"\n";
        runLinuxSshStage(session, packageId, 50, currentStep, finalizeScript, 300, password);
        verifyLinuxSshPackageUpload(session, packageId, archivePath, remotePackagePath, password);
    }

    private Session uploadLinuxSshPackage(Session session, Long packageId, Path archivePath, String remotePackagePath,
                                          AdminDeployPackageCreateDTO request, String password) throws Exception {
        updateDeployPackageStep(packageId, 42, "linux ssh uploading deploy package", null,
                "linux ssh uploading deploy package: " + archivePath.getFileName() + " (" + formatBytes(Files.size(archivePath)) + ")");
        String tmpRemotePackagePath = remotePackagePath + ".part-" + System.currentTimeMillis();
        String prepareScript = "set -euo pipefail\n"
                + "REMOTE_FILE=" + quoteForBash(remotePackagePath) + "\n"
                + "TMP_FILE=" + quoteForBash(tmpRemotePackagePath) + "\n"
                + "mkdir -p \"$(dirname \"$REMOTE_FILE\")\"\n"
                + "rm -f \"$TMP_FILE\"\n"
                + "touch \"$TMP_FILE\"\n";
        long totalBytes = Files.size(archivePath);
        runLinuxSshStage(session, packageId, 42, "linux ssh preparing chunked package upload", prepareScript, 120, password);
        appendLinuxSshOutput(packageId, 42, "linux ssh upload started: " + remotePackagePath, password);
        long transferred = 0L;
        long lastReportedAt = 0L;
        int lastPercent = 0;
        try (InputStream fileInput = Files.newInputStream(archivePath)) {
            byte[] buffer = new byte[LINUX_SSH_PACKAGE_UPLOAD_CHUNK_BYTES];
            int len;
            while ((len = fileInput.read(buffer)) >= 0) {
                session = uploadLinuxSshPackageChunk(session, request, packageId, tmpRemotePackagePath,
                        buffer, len, transferred, totalBytes, password);
                transferred += len;
                int percent = (int) Math.min(100L, Math.round(transferred * 100.0d / Math.max(1L, totalBytes)));
                long now = System.currentTimeMillis();
                if (percent >= lastPercent + 10 || now - lastReportedAt > 5000L || percent >= 100) {
                    appendLinuxSshOutput(packageId, 42, "linux ssh upload progress: " + percent + "% ("
                            + formatBytes(transferred) + "/" + formatBytes(totalBytes) + ")", password);
                    lastPercent = percent;
                    lastReportedAt = now;
                }
            }
        }
        finalizeLinuxSshPackageUpload(session, packageId, archivePath, remotePackagePath, tmpRemotePackagePath, password,
                "linux ssh finalizing chunked package upload", "linux ssh chunked upload final size");
        appendLinuxSshOutput(packageId, 42, "linux ssh upload finished", password);
        updateDeployPackageStep(packageId, 52, "linux ssh package uploaded", null,
                "linux ssh package uploaded to " + remotePackagePath);
        return session;
    }

    private Session uploadLinuxSshPackageChunk(Session session, AdminDeployPackageCreateDTO request, Long packageId,
                                               String tmpRemotePackagePath, byte[] chunk, int chunkLength, long offset,
                                               long totalBytes, String password) throws Exception {
        Exception lastError = null;
        for (int attempt = 1; attempt <= LINUX_SSH_PACKAGE_UPLOAD_CHUNK_RETRY_ATTEMPTS; attempt++) {
            session = ensureLinuxSshSession(session, request, packageId, 42, password);
            try {
                uploadLinuxSshPackageChunkOnce(session, packageId, tmpRemotePackagePath, chunk, chunkLength, offset, password);
                long uploaded = offset + chunkLength;
                appendLinuxSshOutput(packageId, 42,
                        "linux ssh upload chunk verified: " + formatBytes(uploaded) + "/" + formatBytes(totalBytes),
                        password);
                return session;
            } catch (Exception e) {
                lastError = e;
                appendLinuxSshOutput(packageId, 42,
                        "linux ssh upload chunk retry " + attempt + "/" + LINUX_SSH_PACKAGE_UPLOAD_CHUNK_RETRY_ATTEMPTS
                                + " at offset " + offset + ": " + e.getMessage(),
                        password);
                try {
                    if (session != null) {
                        session.disconnect();
                    }
                } catch (Exception ignored) {
                    // reconnect below
                }
                session = null;
                if (attempt < LINUX_SSH_PACKAGE_UPLOAD_CHUNK_RETRY_ATTEMPTS) {
                    Thread.sleep(2000L * attempt);
                }
            }
        }
        throw lastError == null
                ? new IllegalStateException("linux ssh package upload chunk failed")
                : lastError;
    }

    private void uploadLinuxSshPackageChunkOnce(Session session, Long packageId, String tmpRemotePackagePath,
                                               byte[] chunk, int chunkLength, long offset, String password) throws Exception {
        String script = "set -euo pipefail\n"
                + "TMP_FILE=" + quoteForBash(tmpRemotePackagePath) + "\n"
                + "EXPECTED_OFFSET=" + offset + "\n"
                + "CHUNK_SIZE=" + chunkLength + "\n"
                + "mkdir -p \"$(dirname \"$TMP_FILE\")\"\n"
                + "touch \"$TMP_FILE\"\n"
                + "CURRENT_SIZE=$(wc -c < \"$TMP_FILE\" | tr -d ' ')\n"
                + "if [ \"$CURRENT_SIZE\" -gt \"$EXPECTED_OFFSET\" ]; then truncate -s \"$EXPECTED_OFFSET\" \"$TMP_FILE\"; CURRENT_SIZE=\"$EXPECTED_OFFSET\"; fi\n"
                + "if [ \"$CURRENT_SIZE\" != \"$EXPECTED_OFFSET\" ]; then echo \"linux ssh chunk offset mismatch: expected=$EXPECTED_OFFSET actual=$CURRENT_SIZE\" >&2; exit 1; fi\n"
                + "cat >> \"$TMP_FILE\"\n"
                + "REMOTE_SIZE=$(wc -c < \"$TMP_FILE\" | tr -d ' ')\n"
                + "EXPECTED_SIZE=$((EXPECTED_OFFSET + CHUNK_SIZE))\n"
                + "echo \"linux ssh chunk remote size: $REMOTE_SIZE\"\n"
                + "if [ \"$REMOTE_SIZE\" != \"$EXPECTED_SIZE\" ]; then echo \"linux ssh chunk size mismatch: expected=$EXPECTED_SIZE actual=$REMOTE_SIZE\" >&2; exit 1; fi\n";
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand("bash -lc " + quoteForBash(script));
        InputStream stdout = channel.getInputStream();
        InputStream stderr = channel.getExtInputStream();
        OutputStream chunkStream = channel.getOutputStream();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] readBuffer = new byte[4096];
        channel.connect(10000);
        try {
            chunkStream.write(chunk, 0, chunkLength);
            chunkStream.flush();
            chunkStream.close();
            long deadline = System.currentTimeMillis() + 180_000L;
            while (!channel.isClosed()) {
                readLinuxSshAvailable(stdout, output, readBuffer);
                readLinuxSshAvailable(stderr, output, readBuffer);
                if (System.currentTimeMillis() > deadline) {
                    channel.disconnect();
                    throw new IllegalStateException("linux ssh package upload chunk timed out");
                }
                Thread.sleep(100L);
            }
            while (readLinuxSshAvailable(stdout, output, readBuffer) || readLinuxSshAvailable(stderr, output, readBuffer)) {
                // drain remaining output
            }
            String text = output.toString(StandardCharsets.UTF_8);
            appendLinuxSshOutput(packageId, 42, text, password);
            int exitStatus = channel.getExitStatus();
            if (exitStatus != 0) {
                throw new IllegalStateException("linux ssh package upload chunk failed with exit code "
                        + exitStatus + ": " + sanitizeLinuxSshOutput(text, password));
            }
        } finally {
            try {
                chunkStream.close();
            } catch (Exception ignored) {
                // already closed after writing the chunk
            }
            channel.disconnect();
        }
    }

    private Session ensureLinuxSshSession(Session session, AdminDeployPackageCreateDTO request, Long packageId, int progress, String password) throws Exception {
        if (session != null && session.isConnected()) {
            return session;
        }
        appendLinuxSshOutput(packageId, progress, "linux ssh reconnecting for package transfer", password);
        return openLinuxSshSession(request, packageId);
    }

    private static class LinuxSshPackageDownload {
        private final String remotePath;
        private final String tokenKey;
        private final String primaryDownloadUrl;
        private final String proxyDownloadUrl;

        private LinuxSshPackageDownload(String remotePath, String tokenKey, String primaryDownloadUrl, String proxyDownloadUrl) {
            this.remotePath = remotePath;
            this.tokenKey = tokenKey;
            this.primaryDownloadUrl = primaryDownloadUrl;
            this.proxyDownloadUrl = proxyDownloadUrl;
        }

        private String remotePath() {
            return remotePath;
        }

        private String tokenKey() {
            return tokenKey;
        }

        private String primaryDownloadUrl() {
            return primaryDownloadUrl;
        }

        private String proxyDownloadUrl() {
            return proxyDownloadUrl;
        }
    }

    private void verifyLinuxSshPackageUpload(Session session, Long packageId, Path archivePath, String remotePackagePath, String password) throws Exception {
        long localSize = Files.size(archivePath);
        String script = "set -euo pipefail\n"
                + "REMOTE_FILE=" + quoteForBash(remotePackagePath) + "\n"
                + "REMOTE_SIZE=$(wc -c < \"$REMOTE_FILE\" | tr -d ' ')\n"
                + "echo \"linux ssh remote package size: $REMOTE_SIZE\"\n"
                + "echo \"$REMOTE_SIZE\"\n";
        String output = runLinuxSshStage(session, packageId, 52, "linux ssh verifying package upload", script, 120, password);
        long remoteSize = parseLastLongLine(output);
        if (remoteSize != localSize) {
            throw new IllegalStateException("linux ssh package upload size mismatch: local="
                    + localSize + " remote=" + remoteSize + " path=" + remotePackagePath);
        }
        appendLinuxSshOutput(packageId, 52,
                "linux ssh package upload verified: " + formatBytes(remoteSize), password);
    }

    private String normalizeLinuxSshTargetPath(String targetPath) {
        String value = trimToDefault(targetPath, "/opt/rk-web");
        if (value.contains("\n") || value.contains("\r") || !value.startsWith("/") || "/".equals(value.trim())) {
            throw new BadRequestException("Linux SSH 目标目录必须是安全的绝对路径，例如 /opt/rk-web");
        }
        return value.replaceAll("/+$", "");
    }

    private String buildLinuxSshProbeScript() {
        return "set -euo pipefail\n"
                + "echo \"[probe] current time: $(date -Is)\"\n"
                + "echo \"[probe] user: $(id)\"\n"
                + "echo \"[probe] kernel: $(uname -a)\"\n"
                + "if [ -f /etc/os-release ]; then cat /etc/os-release; fi\n"
                + "df -h /\n";
    }

    private String buildLinuxSshPrepareScript(AdminDeployPackageCreateDTO request, String targetRoot) {
        return "set -euo pipefail\n"
                + buildLinuxSshDnsRepairScript()
                + "TARGET_ROOT=" + quoteForBash(targetRoot) + "\n"
                + "DOMESTIC_MIRROR=" + quoteForBash(String.valueOf(Boolean.TRUE.equals(request.getDomesticMirror()))) + "\n"
                + "echo \"linux ssh preparing target directory: $TARGET_ROOT\"\n"
                + "mkdir -p \"$TARGET_ROOT/packages\" \"$TARGET_ROOT/releases\"\n"
                + "if [ \"$DOMESTIC_MIRROR\" = \"true\" ]; then ensure_linux_ssh_dns; fi\n"
                + "install_packages() {\n"
                + "  if command -v apt-get >/dev/null 2>&1; then export DEBIAN_FRONTEND=noninteractive; apt-get update -y || true; apt-get install -y bash ca-certificates curl tar gzip unzip coreutils findutils util-linux gettext-base;\n"
                + "  elif command -v dnf >/dev/null 2>&1; then dnf install -y bash ca-certificates curl tar gzip unzip coreutils findutils util-linux gettext;\n"
                + "  elif command -v yum >/dev/null 2>&1; then yum install -y bash ca-certificates curl tar gzip unzip coreutils findutils util-linux gettext;\n"
                + "  elif command -v apk >/dev/null 2>&1; then apk add --no-cache bash ca-certificates curl tar gzip unzip coreutils findutils util-linux gettext;\n"
                + "  else echo \"no supported package manager found; continue with existing tools\"; fi\n"
                + "}\n"
                + "if ! command -v unzip >/dev/null 2>&1 || ! command -v curl >/dev/null 2>&1 || ! command -v envsubst >/dev/null 2>&1; then install_packages; fi\n"
                + "echo \"domestic mirror requested: " + Boolean.TRUE.equals(request.getDomesticMirror()) + "\"\n";
    }

    private String buildLinuxSshDockerInstallScript(AdminDeployPackageCreateDTO request) {
        return "set -euo pipefail\n"
                + "echo \"linux ssh installing docker runtime\"\n"
                + "if ! command -v docker >/dev/null 2>&1; then\n"
                + "  if command -v apt-get >/dev/null 2>&1; then\n"
                + "  export DEBIAN_FRONTEND=noninteractive\n"
                + "  apt-get update -y || true\n"
                + "  apt-get install -y ca-certificates curl gnupg lsb-release docker.io docker-compose-plugin || (curl -fsSL https://get.docker.com | bash)\n"
                + "  elif command -v dnf >/dev/null 2>&1; then\n"
                + "  dnf install -y dnf-plugins-core ca-certificates curl || true\n"
                + "  dnf config-manager --add-repo https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo || true\n"
                + "  dnf install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin || dnf install -y docker\n"
                + "  elif command -v yum >/dev/null 2>&1; then\n"
                + "  yum install -y yum-utils ca-certificates curl || true\n"
                + "  yum-config-manager --add-repo https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo || true\n"
                + "  yum install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin || yum install -y docker\n"
                + "  elif command -v apk >/dev/null 2>&1; then\n"
                + "  apk add --no-cache docker docker-cli-compose\n"
                + "  else\n"
                + "  curl -fsSL https://get.docker.com | bash\n"
                + "  fi\n"
                + "fi\n"
                + "systemctl enable --now docker >/dev/null 2>&1 || service docker start >/dev/null 2>&1 || true\n"
                + "docker version\n"
                + "if docker compose version >/dev/null 2>&1; then docker compose version; elif command -v docker-compose >/dev/null 2>&1; then docker-compose version; else\n"
                + "  echo \"docker compose plugin not found, trying package manager install\"\n"
                + "  if command -v apt-get >/dev/null 2>&1; then apt-get update -y || true; apt-get install -y docker-compose-plugin docker-compose || true;\n"
                + "  elif command -v dnf >/dev/null 2>&1; then dnf install -y docker-compose-plugin docker-compose || true;\n"
                + "  elif command -v yum >/dev/null 2>&1; then yum install -y docker-compose-plugin docker-compose || true;\n"
                + "  elif command -v apk >/dev/null 2>&1; then apk add --no-cache docker-cli-compose docker-compose || true; fi\n"
                + "  if docker compose version >/dev/null 2>&1; then docker compose version; elif command -v docker-compose >/dev/null 2>&1; then docker-compose version; else echo \"docker compose still unavailable\" >&2; exit 1; fi\n"
                + "fi\n";
    }

    private String buildLinuxSshK8sInstallScript(AdminDeployPackageCreateDTO request) {
        boolean domesticMirror = Boolean.TRUE.equals(request.getDomesticMirror());
        String mirrorValue = domesticMirror ? "cn" : "";
        String pauseImage = domesticMirror
                ? "registry.k8s.io/pause:3.6"
                : "rancher/mirrored-pause:3.6";
        String localPathHelperImage = domesticMirror
                ? REMOTE_BUSYBOX_IMAGE
                : "rancher/mirrored-library-busybox:1.37.0";
        String domesticRegistryConfig = domesticMirror
                ? "mkdir -p /etc/rancher/k3s\n"
                + "cat >/etc/rancher/k3s/registries.yaml <<'RK_K3S_REGISTRIES'\n"
                + "mirrors:\n"
                + "  docker.io:\n"
                + "    endpoint:\n"
                + "      - \"https://docker.m.daocloud.io\"\n"
                + "      - \"https://registry-1.docker.io\"\n"
                + "  registry.k8s.io:\n"
                + "    endpoint:\n"
                + "      - \"https://k8s.m.daocloud.io\"\n"
                + "  k8s.gcr.io:\n"
                + "    endpoint:\n"
                + "      - \"https://k8s-gcr.m.daocloud.io\"\n"
                + "  gcr.io:\n"
                + "    endpoint:\n"
                + "      - \"https://gcr.m.daocloud.io\"\n"
                + "  quay.io:\n"
                + "    endpoint:\n"
                + "      - \"https://quay.m.daocloud.io\"\n"
                + "RK_K3S_REGISTRIES\n"
                : "";
        return "set -euo pipefail\n"
                + "echo \"linux ssh installing k8s runtime\"\n"
                + "LOCAL_PATH_HELPER_IMAGE=${LOCAL_PATH_HELPER_IMAGE:-" + localPathHelperImage + "}\n"
                + "pull_cri_image() {\n"
                + "  image=\"$1\"\n"
                + "  if command -v timeout >/dev/null 2>&1; then prefix=\"timeout 180s\"; else prefix=\"\"; fi\n"
                + "  if command -v k3s >/dev/null 2>&1; then $prefix k3s crictl pull \"$image\" && return 0; fi\n"
                + "  if [ -x /var/lib/rancher/k3s/data/current/bin/crictl ]; then $prefix /var/lib/rancher/k3s/data/current/bin/crictl --runtime-endpoint unix:///run/k3s/containerd/containerd.sock pull \"$image\" && return 0; fi\n"
                + "  return 1\n"
                + "}\n"
                + "configure_k3s_firewalld() {\n"
                + "  echo \"configure_k3s_firewalld\"\n"
                + "  if ! command -v firewall-cmd >/dev/null 2>&1 || ! firewall-cmd --state >/dev/null 2>&1; then echo \"firewalld is not active; skip k3s firewalld cidr trust\"; return 0; fi\n"
                + "  firewall-cmd --permanent --zone=trusted --add-source=10.42.0.0/16 || true\n"
                + "  firewall-cmd --permanent --zone=trusted --add-source=10.43.0.0/16 || true\n"
                + "  firewall-cmd --reload\n"
                + "  echo \"k3s firewalld trusted pod/service cidrs configured\"\n"
                + "}\n"
                + "patch_local_path_helper_image() {\n"
                + "  if ! kubectl -n kube-system get configmap/local-path-config >/dev/null 2>&1; then echo \"local-path-config not found yet\"; return 0; fi\n"
                + "  echo \"patching local-path helper image: $LOCAL_PATH_HELPER_IMAGE\"\n"
                + "  cat >/tmp/rk-local-path-helper-patch.json <<RK_LOCAL_PATH_PATCH\n"
                + "{\"data\":{\"helperPod.yaml\":\"apiVersion: v1\\nkind: Pod\\nmetadata:\\n  name: helper-pod\\nspec:\\n  containers:\\n  - name: helper-pod\\n    image: \\\"$LOCAL_PATH_HELPER_IMAGE\\\"\\n    imagePullPolicy: IfNotPresent\\n\"}}\n"
                + "RK_LOCAL_PATH_PATCH\n"
                + "  kubectl -n kube-system patch configmap local-path-config --type merge --patch-file /tmp/rk-local-path-helper-patch.json\n"
                + "  kubectl -n kube-system rollout restart deployment/local-path-provisioner\n"
                + "}\n"
                + "pre_pull_k3s_system_images() {\n"
                + "  echo \"pre_pull_k3s_system_images\"\n"
                + "  images=\"\"\n"
                + "  for attempt in $(seq 1 60); do\n"
                + "    images=\"$(kubectl -n kube-system get deployment -o jsonpath='{range .items[*]}{range .spec.template.spec.containers[*]}{.image}{\"\\n\"}{end}{end}' 2>/dev/null | sort -u || true)\"\n"
                + "    [ -n \"$images\" ] && break\n"
                + "    sleep 2\n"
                + "  done\n"
                + "  if [ -z \"$images\" ]; then echo \"k3s system deployment images not found yet\"; return 0; fi\n"
                + "  patch_local_path_helper_image\n"
                + "  echo \"pre-pulling k3s local-path helper image: $LOCAL_PATH_HELPER_IMAGE\"\n"
                + "  for attempt in 1 2 3; do\n"
                + "    pull_cri_image \"$LOCAL_PATH_HELPER_IMAGE\" && break\n"
                + "    if [ \"$attempt\" = 3 ]; then echo \"failed to pull k3s local-path helper image: $LOCAL_PATH_HELPER_IMAGE\" >&2; exit 1; fi\n"
                + "    sleep 5\n"
                + "  done\n"
                + "  images=\"$(printf '%s\\n%s\\n' \"$images\" \"$LOCAL_PATH_HELPER_IMAGE\" | sed '/^$/d' | sort -u)\"\n"
                + "  for image in $images; do\n"
                + "    echo \"pre-pulling k3s system image: $image\"\n"
                + "    for attempt in 1 2 3; do\n"
                + "      pull_cri_image \"$image\" && break\n"
                + "      if [ \"$attempt\" = 3 ]; then echo \"failed to pull k3s system image: $image\" >&2; exit 1; fi\n"
                + "      sleep 5\n"
                + "    done\n"
                + "  done\n"
                + "}\n"
                + "verify_local_path_pvc_provisioning() {\n"
                + "  echo \"verify_local_path_pvc_provisioning\"\n"
                + "  kubectl -n kube-system delete pod/rk-local-path-preflight pvc/rk-local-path-preflight --ignore-not-found=true --wait=false >/dev/null 2>&1 || true\n"
                + "  cat <<RK_LOCAL_PATH_PREFLIGHT | kubectl apply -f -\n"
                + "apiVersion: v1\n"
                + "kind: PersistentVolumeClaim\n"
                + "metadata:\n"
                + "  name: rk-local-path-preflight\n"
                + "  namespace: kube-system\n"
                + "spec:\n"
                + "  accessModes:\n"
                + "    - ReadWriteOnce\n"
                + "  storageClassName: local-path\n"
                + "  resources:\n"
                + "    requests:\n"
                + "      storage: 1Mi\n"
                + "---\n"
                + "apiVersion: v1\n"
                + "kind: Pod\n"
                + "metadata:\n"
                + "  name: rk-local-path-preflight\n"
                + "  namespace: kube-system\n"
                + "spec:\n"
                + "  restartPolicy: Never\n"
                + "  containers:\n"
                + "    - name: preflight\n"
                + "      image: $LOCAL_PATH_HELPER_IMAGE\n"
                + "      imagePullPolicy: IfNotPresent\n"
                + "      command: [\"sh\", \"-c\", \"echo ok >/data/preflight && sleep 30\"]\n"
                + "      volumeMounts:\n"
                + "        - name: data\n"
                + "          mountPath: /data\n"
                + "  volumes:\n"
                + "    - name: data\n"
                + "      persistentVolumeClaim:\n"
                + "        claimName: rk-local-path-preflight\n"
                + "RK_LOCAL_PATH_PREFLIGHT\n"
                + "  phase=\"\"\n"
                + "  for attempt in $(seq 1 120); do\n"
                + "    phase=\"$(kubectl -n kube-system get pvc rk-local-path-preflight -o jsonpath='{.status.phase}' 2>/dev/null || true)\"\n"
                + "    [ \"$phase\" = \"Bound\" ] && break\n"
                + "    sleep 2\n"
                + "  done\n"
                + "  if [ \"${phase:-}\" != \"Bound\" ]; then\n"
                + "    echo \"local-path preflight pvc did not bind\" >&2\n"
                + "    kubectl -n kube-system describe pvc/rk-local-path-preflight || true\n"
                + "    kubectl -n kube-system describe pod/rk-local-path-preflight || true\n"
                + "    kubectl -n kube-system get pod -o wide | grep -E 'helper-pod|rk-local-path-preflight' || true\n"
                + "    exit 1\n"
                + "  fi\n"
                + "  kubectl -n kube-system delete pod/rk-local-path-preflight --ignore-not-found=true --wait=false >/dev/null 2>&1 || true\n"
                + "  kubectl -n kube-system delete pvc/rk-local-path-preflight --ignore-not-found=true --wait=false >/dev/null 2>&1 || true\n"
                + "}\n"
                + "wait_for_k3s_system_deployments() {\n"
                + "  echo \"wait_for_k3s_system_deployments\"\n"
                + "  kubectl -n kube-system rollout status deployment/coredns --timeout=360s\n"
                + "  kubectl -n kube-system rollout status deployment/local-path-provisioner --timeout=360s\n"
                + "  if kubectl -n kube-system get deployment/metrics-server >/dev/null 2>&1; then\n"
                + "    if ! kubectl -n kube-system rollout status deployment/metrics-server --timeout=60s; then\n"
                + "      echo \"metrics-server is not ready yet; continuing because it is not required by RK services\"\n"
                + "      kubectl -n kube-system describe deployment/metrics-server || true\n"
                + "      kubectl -n kube-system get pod -l k8s-app=metrics-server -o wide || true\n"
                + "    fi\n"
                + "  fi\n"
                + "  kubectl get storageclass local-path\n"
                + "  kubectl -n kube-system get pod -o wide\n"
                + "  verify_local_path_pvc_provisioning\n"
                + "}\n"
                + "if command -v kubectl >/dev/null 2>&1 && kubectl get nodes -o wide >/dev/null 2>&1; then configure_k3s_firewalld; pre_pull_k3s_system_images; wait_for_k3s_system_deployments; exit 0; fi\n"
                + "if [ -f /etc/rancher/k3s/k3s.yaml ] && command -v kubectl >/dev/null 2>&1; then export KUBECONFIG=/etc/rancher/k3s/k3s.yaml; kubectl get nodes -o wide; configure_k3s_firewalld; pre_pull_k3s_system_images; wait_for_k3s_system_deployments; exit 0; fi\n"
                + domesticRegistryConfig
                + "K3S_PAUSE_IMAGE=${K3S_PAUSE_IMAGE:-" + pauseImage + "}\n"
                + "curl -sfL https://rancher-mirror.rancher.cn/k3s/k3s-install.sh -o /tmp/rk-install-k3s.sh || curl -sfL https://get.k3s.io -o /tmp/rk-install-k3s.sh\n"
                + "chmod +x /tmp/rk-install-k3s.sh\n"
                + "INSTALL_K3S_MIRROR=" + quoteForBash(mirrorValue) + " INSTALL_K3S_EXEC=\"--write-kubeconfig-mode 644 --disable traefik --pause-image $K3S_PAUSE_IMAGE\" sh /tmp/rk-install-k3s.sh\n"
                + "if [ -x /var/lib/rancher/k3s/data/current/bin/ctr ]; then for attempt in 1 2 3; do /var/lib/rancher/k3s/data/current/bin/ctr --address /run/k3s/containerd/containerd.sock -n k8s.io images pull \"$K3S_PAUSE_IMAGE\" && break; if [ \"$attempt\" = 3 ]; then echo \"failed to pull k3s pause image: $K3S_PAUSE_IMAGE\" >&2; exit 1; fi; sleep 5; done; fi\n"
                + "export KUBECONFIG=/etc/rancher/k3s/k3s.yaml\n"
                + "configure_k3s_firewalld\n"
                + "kubectl get nodes -o wide\n"
                + "pre_pull_k3s_system_images\n"
                + "wait_for_k3s_system_deployments\n";
    }

    private String buildLinuxSshDeployScript(AdminDeployPackageCreateDTO request, String mode, String namespace, String targetRoot, String releaseDir, String remotePackagePath) {
        String installCommand = "docker-compose".equals(mode)
                ? "bash scripts/install-compose.sh"
                : "export KUBECONFIG=${KUBECONFIG:-/etc/rancher/k3s/k3s.yaml}; NAMESPACE=" + quoteForBash(namespace)
                + " DOMAIN=" + quoteForBash(resolveRemoteDeployDomain(request))
                + " CLUSTER_TYPE=" + quoteForBash(trimToDefault(request.getTargetClusterType(), "k3s"))
                + " bash scripts/install-k8s.sh";
        return "set -euo pipefail\n"
                + "TARGET_ROOT=" + quoteForBash(targetRoot) + "\n"
                + "RELEASE_DIR=" + quoteForBash(releaseDir) + "\n"
                + "PACKAGE_FILE=" + quoteForBash(remotePackagePath) + "\n"
                + "echo \"linux ssh deploying application stack from $PACKAGE_FILE\"\n"
                + "rm -rf \"$RELEASE_DIR\"\n"
                + "mkdir -p \"$RELEASE_DIR\"\n"
                + "unzip -oq \"$PACKAGE_FILE\" -d \"$RELEASE_DIR\"\n"
                + "cd \"$RELEASE_DIR\"\n"
                + "chmod +x install.sh offline-install.sh scripts/*.sh 2>/dev/null || true\n"
                + installCommand + "\n";
    }

    private String buildLinuxSshHealthCheckScript(String mode, String namespace, String releaseDir) {
        if ("docker-compose".equals(mode)) {
            return "set -euo pipefail\n"
                    + "cd " + quoteForBash(releaseDir) + "\n"
                    + "if docker compose version >/dev/null 2>&1; then docker compose -f docker-compose.yml ps; else docker-compose -f docker-compose.yml ps; fi\n";
        }
        return "set -euo pipefail\n"
                + "export KUBECONFIG=${KUBECONFIG:-/etc/rancher/k3s/k3s.yaml}\n"
                + "kubectl -n " + quoteForBash(namespace) + " get pod,sts,deploy,svc,pvc,ingress -o wide\n";
    }

    private void startRemoteDeployAsync(Long packageId, String packageName, AdminDeployPackageCreateDTO request, Long capturedTenantId, Long capturedUserId) {
        Runnable task = () -> {
            if (capturedTenantId != null) {
                TenantContext.setTenantId(capturedTenantId);
            }
            if (capturedUserId != null) {
                UserContext.setUser(capturedUserId);
            }
            try {
                runRemoteDeploy(packageId, packageName, request);
            } catch (Exception e) {
                log.error("remote deploy async task failed, packageId={}", packageId, e);
            } finally {
                TenantContext.clear();
                UserContext.removeUser();
            }
        };
        Thread thread = new Thread(task, "rk-remote-deploy-" + packageId);
        thread.setDaemon(true);
        thread.start();
    }

    private void runRemoteDeploy(Long packageId, String packageName, AdminDeployPackageCreateDTO request) {
        Path kubeconfigPath = null;
        Path manifestPath = null;
        Path databaseSnapshotPath = null;
        Path minioPayloadRoot = null;
        Path runtimeArtifactDir = null;
        String namespace = sanitizeCleanupToken(trimToDefault(request.getTargetNamespace(), "shetuanguanlixitong"), "namespace");
        boolean payloadVolumePrepared = false;
        try {
            refreshGlobalMigrationLock(packageId);
            kubeconfigPath = writeTemporaryKubeconfig(request.getKubeconfig());
            if (shouldPushRuntimeImagesBeforeRemoteDeploy(request)) {
                assertRuntimeExportDiskHeadroom(packageId, shouldRelaxRemoteDeployDiskPreflight(request));
                updateDeployPackageStep(packageId, 15, "remote deploy exporting current runtime images to registry", null,
                        "remote deploy exporting current runtime images to registry");
                runtimeArtifactDir = exportRuntimeArtifactsToDirectory(request, packageName, packageId);
                runRegistryPushForRuntimeArtifacts(runtimeArtifactDir, request, packageId);
                updateDeployPackageStep(packageId, 18, "remote deploy registry images ready", null,
                        "remote deploy registry images ready");
            }
            manifestPath = Files.createTempFile("rk-remote-manifest-", ".yaml");
            Files.writeString(manifestPath, buildK8sStackManifest(request), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            if (!Boolean.TRUE.equals(request.getDryRun())) {
                ensureRemoteNamespace(packageId, kubeconfigPath, request, namespace);
                applyRemoteImagePullSecret(packageId, kubeconfigPath, request, namespace);
                deleteLegacyPersistentMiddlewareDeployments(packageId, kubeconfigPath, request, namespace);
            }
            updateDeployPackageStep(packageId, 20, "remote deploy applying k8s manifest", null, "remote deploy applying k8s manifest");
            String applyArgs = Boolean.TRUE.equals(request.getDryRun())
                    ? "apply --dry-run=server -f " + quoteForBash(manifestPath.toString())
                    : "apply -f " + quoteForBash(manifestPath.toString());
            runKubectlWithKubeconfig(kubeconfigPath, request.getKubeContext(), applyArgs, 300, packageId);
            updateDeployPackageStep(packageId, 45, "remote deploy manifest applied", null, "remote deploy manifest applied");

            boolean forceFullRemoteData = Boolean.TRUE.equals(request.getRemoteDeploy()) && !Boolean.TRUE.equals(request.getDryRun());
            boolean needsPayloadVolume = !Boolean.TRUE.equals(request.getDryRun())
                    && (forceFullRemoteData || Boolean.TRUE.equals(request.getApplyDatabaseSnapshot()) || Boolean.TRUE.equals(request.getApplyMinioSnapshot()));
            boolean applyDatabaseSnapshot = (forceFullRemoteData || Boolean.TRUE.equals(request.getApplyDatabaseSnapshot()))
                    && !Boolean.TRUE.equals(request.getDryRun());
            boolean applyMinioSnapshot = (forceFullRemoteData || Boolean.TRUE.equals(request.getApplyMinioSnapshot()))
                    && !Boolean.TRUE.equals(request.getDryRun());
            if (needsPayloadVolume) {
                prepareRemoteMigrationPayloadVolume(packageId, kubeconfigPath, request, namespace);
                payloadVolumePrepared = true;
                if (applyDatabaseSnapshot) {
                    databaseSnapshotPath = writeRemoteDeployDatabaseSnapshot(request);
                    copyRemoteMigrationPayload(packageId, kubeconfigPath, request, namespace, databaseSnapshotPath, "/migration/rk-all-current-data.sql.gz");
                }
                if (applyMinioSnapshot) {
                    minioPayloadRoot = buildRemoteMinioPayloadDirectory(packageId);
                    copyRemoteMigrationPayload(packageId, kubeconfigPath, request, namespace, minioPayloadRoot.resolve("minio-payload"), "/migration/minio-payload");
                }
                deleteRemoteMigrationStagingPod(packageId, kubeconfigPath, request, namespace);
            }
            int timeout = Math.max(60, request.getRolloutTimeoutSeconds() == null ? 600 : request.getRolloutTimeoutSeconds());
            int dataPlaneTimeout = resolveRemoteDataPlaneTimeoutSeconds(request);
            if (applyDatabaseSnapshot) {
                waitRemoteStatefulSet(packageId, kubeconfigPath, request, namespace, "mysql", dataPlaneTimeout);
                importRemoteDatabaseSnapshot(packageId, kubeconfigPath, request, namespace, databaseSnapshotPath, dataPlaneTimeout);
                restartRemoteStatefulSet(packageId, kubeconfigPath, request, namespace, "nacos", Math.max(600, timeout));
                waitRemoteNacosConfigReady(packageId, kubeconfigPath, request, namespace, "rk-shared-mybatis.yaml", Math.max(600, timeout));
            }
            if (applyMinioSnapshot) {
                waitRemoteStatefulSet(packageId, kubeconfigPath, request, namespace, "minio", dataPlaneTimeout);
                importRemoteMinioSnapshot(packageId, kubeconfigPath, request, namespace, dataPlaneTimeout);
            }
            if (Boolean.TRUE.equals(request.getWaitRollout()) && !Boolean.TRUE.equals(request.getDryRun())) {
                updateDeployPackageStep(packageId, 82, "remote deploy waiting rollout", null, "remote deploy waiting rollout");
                updateDeployPackageStep(packageId, 83, "remote deploy waiting statefulsets", null, "remote deploy waiting statefulsets");
                waitRemoteRolloutResources(packageId, kubeconfigPath, request, namespace, "statefulset", K8S_PERSISTENT_MIDDLEWARE, dataPlaneTimeout);
                if (applyDatabaseSnapshot || applyMinioSnapshot) {
                    restartRemoteDeployments(packageId, kubeconfigPath, request, namespace, REMOTE_K8S_DEPLOYMENTS, timeout);
                }
                waitRemoteRolloutResources(packageId, kubeconfigPath, request, namespace, "deploy", REMOTE_K8S_DEPLOYMENTS, timeout);
                updateDeployPackageStep(packageId, 95, "remote deploy rollout completed", null, "remote deploy rollout completed");
            } else if (!Boolean.TRUE.equals(request.getDryRun()) && (applyDatabaseSnapshot || applyMinioSnapshot)) {
                restartRemoteDeployments(packageId, kubeconfigPath, request, namespace, REMOTE_K8S_DEPLOYMENTS, timeout);
            }
            LocalDateTime finishedAt = LocalDateTime.now();
            jdbcTemplate.update(
                    "UPDATE ops_deploy_package_record SET status = 'SUCCESS', progress = 100, current_step = ?, current_image = NULL, deploy_finished_at = ?, estimated_remaining_seconds = 0, migration_updated_at = ?, logs = CONCAT(COALESCE(logs,''), ?), update_time = ? WHERE id = ?",
                    Boolean.TRUE.equals(request.getDryRun()) ? "remote deploy dry-run completed" : "remote deploy completed",
                    finishedAt,
                    finishedAt,
                    (Boolean.TRUE.equals(request.getDryRun()) ? "remote deploy dry-run completed\n" : "remote deploy completed\n"),
                    finishedAt,
                    packageId
            );
        } catch (Exception e) {
            String message = sanitizeDeployLog(e.getMessage());
            log.error("remote deploy failed, packageId={}", packageId, e);
            LocalDateTime finishedAt = LocalDateTime.now();
            jdbcTemplate.update(
                    "UPDATE ops_deploy_package_record SET status = 'FAILED', progress = 100, current_step = 'remote deploy failed', deploy_finished_at = ?, estimated_remaining_seconds = 0, migration_updated_at = ?, logs = CONCAT(COALESCE(logs,''), ?), update_time = ? WHERE id = ?",
                    finishedAt,
                    finishedAt,
                    "remote deploy failed: " + message + "\n",
                    finishedAt,
                    packageId
            );
        } finally {
            if (payloadVolumePrepared) {
                cleanupRemoteMigrationPayloadResources(packageId, kubeconfigPath, request, namespace);
            }
            deleteTemporaryKubeconfigQuietly(kubeconfigPath);
            deleteTemporaryKubeconfigQuietly(manifestPath);
            deleteTemporaryKubeconfigQuietly(databaseSnapshotPath);
            cleanupLocalDirectoryQuietly(minioPayloadRoot);
            cleanupRuntimeArtifactDirectory(runtimeArtifactDir, packageId);
            clearGlobalMigrationLock();
        }
    }

    private boolean shouldPushRuntimeImagesBeforeRemoteDeploy(AdminDeployPackageCreateDTO request) {
        if (request == null || Boolean.TRUE.equals(request.getDryRun())) {
            return false;
        }
        String imageMode = trimToDefault(request.getImageArtifactMode(), IMAGE_ARTIFACT_BOTH);
        return Boolean.TRUE.equals(request.getExportRuntimeArtifacts())
                && (IMAGE_ARTIFACT_REGISTRY.equals(imageMode) || IMAGE_ARTIFACT_BOTH.equals(imageMode));
    }

    private boolean shouldRelaxRemoteDeployDiskPreflight(AdminDeployPackageCreateDTO request) {
        if (request == null || !Boolean.TRUE.equals(request.getRemoteDeploy())) {
            return false;
        }
        String imageMode = trimToDefault(request.getImageArtifactMode(), IMAGE_ARTIFACT_BOTH);
        return IMAGE_ARTIFACT_REGISTRY.equals(imageMode);
    }

    private int resolveRemoteDataPlaneTimeoutSeconds(AdminDeployPackageCreateDTO request) {
        int rolloutTimeout = request == null || request.getRolloutTimeoutSeconds() == null
                ? 600
                : request.getRolloutTimeoutSeconds();
        int runtimeExportTimeout = deployPackageRuntimeExportTimeoutSeconds == null
                ? 1800
                : deployPackageRuntimeExportTimeoutSeconds;
        return Math.max(1800, Math.max(rolloutTimeout, runtimeExportTimeout));
    }

    private Path writeTemporaryKubeconfig(String kubeconfig) {
        try {
            Path path = Files.createTempFile("rk-remote-kubeconfig-", ".yaml");
            Files.writeString(path, kubeconfig, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            File file = path.toFile();
            file.setReadable(false, false);
            file.setWritable(false, false);
            file.setExecutable(false, false);
            file.setReadable(true, true);
            file.setWritable(true, true);
            return path;
        } catch (Exception e) {
            throw new IllegalStateException("write temporary kubeconfig failed", e);
        }
    }

    private void deleteTemporaryKubeconfigQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (Exception e) {
            log.warn("delete temporary remote deploy file failed, path={}", path, e);
        }
    }

    private Path writeRemoteDeployDatabaseSnapshot(AdminDeployPackageCreateDTO request) {
        try {
            List<String> databases = Boolean.FALSE.equals(request.getIncludeDatabases())
                    ? Collections.emptyList()
                    : normalizeDeployPackageDatabases(request.getDatabases());
            if (databases.isEmpty() && !Boolean.FALSE.equals(request.getIncludeDatabases())) {
                databases = DEFAULT_DEPLOY_PACKAGE_DATABASES;
            }
            Path path = Files.createTempFile("rk-remote-database-snapshot-", ".sql.gz");
            Files.write(path, backupArchiveBuilder.buildArchive(databases, null), StandardOpenOption.TRUNCATE_EXISTING);
            return path;
        } catch (Exception e) {
            throw new IllegalStateException("build remote database snapshot failed", e);
        }
    }

    private void importRemoteDatabaseSnapshot(Long packageId, Path kubeconfigPath, AdminDeployPackageCreateDTO request, String namespace, Path databaseSnapshotPath, int dataPlaneTimeout) {
        updateDeployPackageStep(packageId, 62, "remote deploy importing database snapshot", null, "remote deploy importing database snapshot");
        updateRemoteMigrationEta(packageId, 62);
        Path jobManifestPath = null;
        try {
            jobManifestPath = Files.createTempFile("rk-remote-mysql-import-job-", ".yaml");
            Files.writeString(jobManifestPath, buildRemoteMysqlImportJobManifest(namespace, REMOTE_MYSQL_IMAGE), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            cleanupRemoteImportResources(packageId, kubeconfigPath, request, namespace, REMOTE_MYSQL_IMPORT_JOB, null);
            updateDeployPackageProgress(packageId, 66, "kubectl create job " + REMOTE_MYSQL_IMPORT_JOB + " from manifest");
            runKubectlWithKubeconfig(kubeconfigPath, request.getKubeContext(),
                    "apply -f " + quoteForBash(jobManifestPath.toString()),
                    120,
                    packageId);
            waitRemoteJob(packageId, kubeconfigPath, request, namespace, REMOTE_MYSQL_IMPORT_JOB, dataPlaneTimeout);
            updateDeployPackageProgress(packageId, 72, "kubectl logs job/rk-migration-mysql-import");
            runKubectlWithKubeconfig(kubeconfigPath, request.getKubeContext(),
                    "-n " + quoteForBash(namespace) + " logs job/" + REMOTE_MYSQL_IMPORT_JOB + " --tail=200",
                    120,
                    packageId);
            updateDeployPackageStep(packageId, 74, "remote deploy database imported by temporary job", null,
                    "remote deploy database imported by temporary job");
        } catch (Exception e) {
            throw new IllegalStateException("remote deploy database import job failed: " + sanitizeDeployLog(e.getMessage()), e);
        } finally {
            cleanupRemoteImportResources(packageId, kubeconfigPath, request, namespace, REMOTE_MYSQL_IMPORT_JOB, null);
            deleteTemporaryKubeconfigQuietly(jobManifestPath);
        }
    }

    private void importRemoteMinioSnapshot(Long packageId, Path kubeconfigPath, AdminDeployPackageCreateDTO request, String namespace, int dataPlaneTimeout) {
        updateDeployPackageStep(packageId, 76, "remote deploy importing MinIO objects", null, "remote deploy importing MinIO objects");
        updateRemoteMigrationEta(packageId, 76);
        Path jobManifestPath = null;
        try {
            jobManifestPath = Files.createTempFile("rk-remote-minio-import-job-", ".yaml");
            Files.writeString(jobManifestPath, buildRemoteMinioImportJobManifest(namespace, DEFAULT_MINIO_CLIENT_IMAGE), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            waitRemoteStatefulSet(packageId, kubeconfigPath, request, namespace, "minio", dataPlaneTimeout);
            cleanupRemoteImportResources(packageId, kubeconfigPath, request, namespace, REMOTE_MINIO_IMPORT_JOB, null);
            updateDeployPackageProgress(packageId, 79, "kubectl create job " + REMOTE_MINIO_IMPORT_JOB + " from manifest");
            runKubectlWithKubeconfig(kubeconfigPath, request.getKubeContext(),
                    "apply -f " + quoteForBash(jobManifestPath.toString()),
                    120,
                    packageId);
            waitRemoteJob(packageId, kubeconfigPath, request, namespace, REMOTE_MINIO_IMPORT_JOB, dataPlaneTimeout);
            updateDeployPackageProgress(packageId, 79, "kubectl logs job/rk-migration-minio-import");
            runKubectlWithKubeconfig(kubeconfigPath, request.getKubeContext(),
                    "-n " + quoteForBash(namespace) + " logs job/" + REMOTE_MINIO_IMPORT_JOB + " --tail=200",
                    120,
                    packageId);
            updateDeployPackageStep(packageId, 80, "remote deploy MinIO imported by temporary job", null,
                    "remote deploy MinIO imported by temporary job");
        } catch (Exception e) {
            throw new IllegalStateException("remote deploy MinIO import failed: " + sanitizeDeployLog(e.getMessage()), e);
        } finally {
            cleanupRemoteImportResources(packageId, kubeconfigPath, request, namespace, REMOTE_MINIO_IMPORT_JOB, null);
            deleteTemporaryKubeconfigQuietly(jobManifestPath);
        }
    }

    private void prepareRemoteMigrationPayloadVolume(Long packageId, Path kubeconfigPath, AdminDeployPackageCreateDTO request, String namespace) {
        Path pvcManifestPath = null;
        Path podManifestPath = null;
        try {
            pvcManifestPath = Files.createTempFile("rk-remote-migration-payload-pvc-", ".yaml");
            podManifestPath = Files.createTempFile("rk-remote-migration-staging-pod-", ".yaml");
            Files.writeString(pvcManifestPath, buildRemoteMigrationPayloadPvcManifest(namespace, request), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            Files.writeString(podManifestPath, buildRemoteMigrationStagingPodManifest(namespace), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            cleanupRemoteMigrationPayloadResources(packageId, kubeconfigPath, request, namespace);
            updateDeployPackageProgress(packageId, 58, "remote deploy creating migration payload PVC");
            runKubectlWithKubeconfig(kubeconfigPath, request.getKubeContext(),
                    "apply -f " + quoteForBash(pvcManifestPath.toString()),
                    120,
                    packageId);
            updateDeployPackageProgress(packageId, 59, "remote deploy creating migration payload staging pod");
            runKubectlWithKubeconfig(kubeconfigPath, request.getKubeContext(),
                    "apply -f " + quoteForBash(podManifestPath.toString()),
                    120,
                    packageId);
            runKubectlWithKubeconfig(kubeconfigPath, request.getKubeContext(),
                    "-n " + quoteForBash(namespace) + " wait --for=condition=Ready pod/" + REMOTE_MIGRATION_STAGING_POD + " --timeout=300s",
                    360,
                    packageId);
        } catch (Exception e) {
            throw new IllegalStateException("remote deploy migration payload staging failed: " + sanitizeDeployLog(e.getMessage()), e);
        } finally {
            deleteTemporaryKubeconfigQuietly(pvcManifestPath);
            deleteTemporaryKubeconfigQuietly(podManifestPath);
        }
    }

    private void copyRemoteMigrationPayload(Long packageId, Path kubeconfigPath, AdminDeployPackageCreateDTO request, String namespace,
                                            Path sourcePath, String targetPath) {
        if (sourcePath == null || !Files.exists(sourcePath)) {
            throw new IllegalStateException("remote deploy payload source does not exist: " + sourcePath);
        }
        String normalizedTarget = targetPath.startsWith("/") ? targetPath : "/migration/" + targetPath;
        boolean sourceIsDirectory = Files.isDirectory(sourcePath);
        String targetParent = parentRemotePath(normalizedTarget);
        String prepareCommand = sourceIsDirectory
                ? "rm -rf " + quoteForBash(normalizedTarget) + " && mkdir -p " + quoteForBash(targetParent)
                : "mkdir -p " + quoteForBash(targetParent);
        runKubectlWithKubeconfig(kubeconfigPath, request.getKubeContext(),
                "-n " + quoteForBash(namespace) + " exec " + REMOTE_MIGRATION_STAGING_POD
                        + " -c staging -- sh -c " + quoteForBash(prepareCommand),
                120,
                packageId);
        String copyTarget = sourceIsDirectory ? targetParent : normalizedTarget;
        updateDeployPackageProgress(packageId, 60, "kubectl cp migration payload to temporary PVC");
        runKubectlWithKubeconfig(kubeconfigPath, request.getKubeContext(),
                "-n " + quoteForBash(namespace) + " cp " + quoteForBash(sourcePath.toString())
                        + " " + REMOTE_MIGRATION_STAGING_POD + ":" + quoteForBash(copyTarget)
                        + " -c staging",
                1800,
                packageId);
    }

    private String parentRemotePath(String path) {
        int index = path.lastIndexOf('/');
        if (index <= 0) {
            return "/";
        }
        return path.substring(0, index);
    }

    private void deleteRemoteMigrationStagingPod(Long packageId, Path kubeconfigPath, AdminDeployPackageCreateDTO request, String namespace) {
        updateDeployPackageProgress(packageId, 61, "remote deploy releasing migration payload staging pod");
        runKubectlWithKubeconfig(kubeconfigPath, request.getKubeContext(),
                "-n " + quoteForBash(namespace) + " delete pod " + REMOTE_MIGRATION_STAGING_POD
                        + " --ignore-not-found=true --wait=true",
                180,
                packageId);
    }

    private void cleanupRemoteMigrationPayloadResources(Long packageId, Path kubeconfigPath, AdminDeployPackageCreateDTO request, String namespace) {
        if (kubeconfigPath == null || !StringUtils.hasText(namespace)) {
            return;
        }
        try {
            runKubectlWithKubeconfig(kubeconfigPath, request.getKubeContext(),
                    "-n " + quoteForBash(namespace) + " delete pod " + REMOTE_MIGRATION_STAGING_POD + " --ignore-not-found=true",
                    120,
                    packageId);
            runKubectlWithKubeconfig(kubeconfigPath, request.getKubeContext(),
                    "-n " + quoteForBash(namespace) + " delete pvc " + REMOTE_MIGRATION_PAYLOAD_PVC + " --ignore-not-found=true",
                    120,
                    packageId);
        } catch (Exception e) {
            log.warn("remote deploy migration payload cleanup failed, namespace={}", namespace, e);
        }
    }

    private String buildRemoteMigrationPayloadPvcManifest(String namespace, AdminDeployPackageCreateDTO request) {
        String storageClassName = resolveDeployTargetDefaults(request).storageClassName;
        String storageClass = StringUtils.hasText(storageClassName)
                ? "  storageClassName: " + storageClassName + "\n"
                : "";
        return "apiVersion: v1\n"
                + "kind: PersistentVolumeClaim\n"
                + "metadata:\n"
                + "  name: " + REMOTE_MIGRATION_PAYLOAD_PVC + "\n"
                + "  namespace: " + namespace + "\n"
                + "spec:\n"
                + "  accessModes:\n"
                + "    - ReadWriteOnce\n"
                + storageClass
                + "  resources:\n"
                + "    requests:\n"
                + "      storage: 20Gi\n";
    }

    private String buildRemoteMigrationStagingPodManifest(String namespace) {
        return "apiVersion: v1\n"
                + "kind: Pod\n"
                + "metadata:\n"
                + "  name: " + REMOTE_MIGRATION_STAGING_POD + "\n"
                + "  namespace: " + namespace + "\n"
                + "spec:\n"
                + "  restartPolicy: Never\n"
                + "  containers:\n"
                + "    - name: staging\n"
                + "      image: " + trimToDefault(k8sCleanupImage, "m.daocloud.io/docker.io/library/busybox:1.36") + "\n"
                + "      imagePullPolicy: IfNotPresent\n"
                + "      command:\n"
                + "        - sh\n"
                + "        - -c\n"
                + "        - mkdir -p /migration && sleep 7200\n"
                + "      volumeMounts:\n"
                + "        - name: payload\n"
                + "          mountPath: /migration\n"
                + "  volumes:\n"
                + "    - name: payload\n"
                + "      persistentVolumeClaim:\n"
                + "        claimName: " + REMOTE_MIGRATION_PAYLOAD_PVC + "\n";
    }

    private Path buildRemoteMinioPayloadDirectory(Long packageId) {
        updateDeployPackageStep(packageId, 75, "remote deploy exporting MinIO payload locally", null, "remote deploy exporting MinIO payload locally");
        try {
            Path root = Files.createTempDirectory("rk-remote-minio-payload-");
            Path payload = root.resolve("minio-payload");
            Files.createDirectories(payload);
            StringBuilder manifest = new StringBuilder();
            int bucketCount = 0;
            int objectCount = 0;
            int skippedObjectCount = 0;
            long totalBytes = 0L;
            long skippedBytes = 0L;
            for (Bucket bucket : minioClient.listBuckets()) {
                String bucketName = bucket == null ? "" : bucket.name();
                if (!StringUtils.hasText(bucketName)) {
                    continue;
                }
                bucketCount++;
                manifest.append(bucketName).append('\n');
                Path bucketDir = payload.resolve(sanitizeMinioZipSegment(bucketName));
                Files.createDirectories(bucketDir);
                Iterable<Result<Item>> objects = minioClient.listObjects(
                        ListObjectsArgs.builder()
                                .bucket(bucketName)
                                .recursive(true)
                                .build()
                );
                for (Result<Item> result : objects) {
                    Item item = result.get();
                    if (item == null || item.isDir() || !StringUtils.hasText(item.objectName())) {
                        continue;
                    }
                    String objectName = item.objectName();
                    if (shouldSkipMinioObjectForMigration(bucketName, objectName)) {
                        skippedObjectCount++;
                        skippedBytes += safeMinioItemSize(item);
                        continue;
                    }
                    Path target = bucketDir.resolve(sanitizeMinioObjectPath(objectName)).normalize();
                    if (!target.startsWith(bucketDir)) {
                        continue;
                    }
                    Files.createDirectories(target.getParent());
                    try (InputStream inputStream = minioClient.getObject(
                            GetObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(objectName)
                                    .build()
                    )) {
                        long copied = copyStreamToFile(inputStream, target);
                        totalBytes += copied;
                        objectCount++;
                    }
                }
            }
            Files.writeString(payload.resolve("minio-manifest.txt"), manifest.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            updateDeployPackageProgress(packageId, 76,
                    "remote deploy MinIO payload exported: buckets=" + bucketCount
                            + ", objects=" + objectCount + ", bytes=" + totalBytes
                            + ", skipped=" + skippedObjectCount + ", skippedBytes=" + skippedBytes);
            return root;
        } catch (Exception e) {
            throw new IllegalStateException("remote deploy build MinIO payload failed: " + sanitizeDeployLog(e.getMessage()), e);
        }
    }

    private void ensureRemoteNamespace(Long packageId, Path kubeconfigPath, AdminDeployPackageCreateDTO request, String namespace) {
        try {
            runKubectlWithKubeconfig(kubeconfigPath, request.getKubeContext(),
                    "get ns " + quoteForBash(namespace),
                    60,
                    packageId);
        } catch (Exception e) {
            runKubectlWithKubeconfig(kubeconfigPath, request.getKubeContext(),
                    "create ns " + quoteForBash(namespace),
                    60,
                    packageId);
        }
    }

    private void applyRemoteImagePullSecret(Long packageId, Path kubeconfigPath, AdminDeployPackageCreateDTO request, String namespace) {
        Path secretManifestPath = null;
        try {
            String registryServer = trimToDefault(request.getRegistryServer(), DEFAULT_REGISTRY_SERVER);
            RegistryCredentials registryCredentials = resolveDeployPackageRegistryCredentials(registryServer);
            if (!registryCredentials.hasCredentials()) {
                updateDeployPackageProgress(packageId, 17, "remote deploy image pull secret skipped: registry credentials unavailable");
                return;
            }
            secretManifestPath = Files.createTempFile("rk-remote-image-pull-secret-", ".yaml");
            Files.writeString(secretManifestPath,
                    buildRemoteImagePullSecretManifest(namespace, registryServer, registryCredentials),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING);
            runKubectlWithKubeconfig(kubeconfigPath, request.getKubeContext(),
                    "apply -f " + quoteForBash(secretManifestPath.toString()),
                    120,
                    packageId);
            updateDeployPackageProgress(packageId, 17, "remote deploy image pull secret applied");
        } catch (Exception e) {
            throw new IllegalStateException("remote deploy image pull secret failed: " + sanitizeDeployLog(e.getMessage()), e);
        } finally {
            deleteTemporaryKubeconfigQuietly(secretManifestPath);
        }
    }

    private String buildRemoteImagePullSecretManifest(String namespace, String registryServer, RegistryCredentials registryCredentials) {
        return buildImagePullSecretManifest(namespace, registryServer, registryCredentials);
    }

    private String buildPackageImagePullSecretManifest(AdminDeployPackageCreateDTO request) {
        String registryServer = trimToDefault(request == null ? null : request.getRegistryServer(), DEFAULT_REGISTRY_SERVER);
        RegistryCredentials registryCredentials = resolveDeployPackageRegistryCredentials(registryServer);
        if (!registryCredentials.hasCredentials()) {
            return "";
        }
        return buildImagePullSecretManifest("", registryServer, registryCredentials);
    }

    private String buildImagePullSecretManifest(String namespace, String registryServer, RegistryCredentials registryCredentials) {
        String secretName = trimToDefault(deployPackageRegistryImagePullSecretName, "rk-aliyun-regcred");
        String normalizedServer = normalizeRegistryServerKey(registryServer);
        String auth = Base64.getEncoder().encodeToString(
                (registryCredentials.username() + ":" + registryCredentials.password()).getBytes(StandardCharsets.UTF_8));
        String dockerConfigJson = toJson(Map.of(
                "auths", Map.of(
                        normalizedServer, Map.of(
                                "username", registryCredentials.username(),
                                "password", registryCredentials.password(),
                                "auth", auth
                        )
                )
        ));
        String encodedDockerConfig = Base64.getEncoder().encodeToString(dockerConfigJson.getBytes(StandardCharsets.UTF_8));
        String namespaceLine = StringUtils.hasText(namespace) ? "  namespace: " + namespace + "\n" : "";
        return "apiVersion: v1\n"
                + "kind: Secret\n"
                + "metadata:\n"
                + "  name: " + secretName + "\n"
                + namespaceLine
                + "type: kubernetes.io/dockerconfigjson\n"
                + "data:\n"
                + "  .dockerconfigjson: " + encodedDockerConfig + "\n";
    }

    private void deleteLegacyPersistentMiddlewareDeployments(Long packageId, Path kubeconfigPath, AdminDeployPackageCreateDTO request, String namespace) {
        updateDeployPackageProgress(packageId, 18, "delete legacy persistent middleware deployments");
        runKubectlWithKubeconfig(kubeconfigPath, request.getKubeContext(),
                "-n " + quoteForBash(namespace) + " delete deploy mysql redis rabbitmq minio nacos elasticsearch --ignore-not-found=true",
                180,
                packageId);
    }

    private void waitRemoteStatefulSet(Long packageId, Path kubeconfigPath, AdminDeployPackageCreateDTO request, String namespace,
                                       String name, int timeoutSeconds) {
        updateDeployPackageProgress(packageId, Math.min(90, Math.max(10, valueAsInteger(jdbcTemplate.queryForObject("SELECT progress FROM ops_deploy_package_record WHERE id = ?", Integer.class, packageId)))),
                "rollout status statefulset/" + name);
        runKubectlWithKubeconfig(kubeconfigPath, request.getKubeContext(),
                "-n " + quoteForBash(namespace) + " rollout status statefulset/" + name + " --timeout=" + timeoutSeconds + "s",
                timeoutSeconds + 60,
                packageId);
    }

    private void restartRemoteStatefulSet(Long packageId, Path kubeconfigPath, AdminDeployPackageCreateDTO request, String namespace,
                                          String name, int timeoutSeconds) {
        updateDeployPackageStep(packageId, 76, "remote deploy restarting nacos after database import", null,
                "remote deploy restarting nacos after database import");
        runKubectlWithKubeconfig(kubeconfigPath, request.getKubeContext(),
                "-n " + quoteForBash(namespace) + " rollout restart statefulset/" + name,
                180,
                packageId);
        waitRemoteStatefulSet(packageId, kubeconfigPath, request, namespace, name, timeoutSeconds);
    }

    private void waitRemoteNacosConfigReady(Long packageId, Path kubeconfigPath, AdminDeployPackageCreateDTO request, String namespace,
                                            String dataId, int timeoutSeconds) {
        updateDeployPackageStep(packageId, 78, "remote deploy waiting nacos imported config", null,
                "remote deploy waiting nacos imported config");
        cleanupRemoteNacosConfigCheckJob(packageId, kubeconfigPath, request, namespace);
        int attempts = Math.max(1, timeoutSeconds / 5);
        String url = "http://nacos:8848/nacos/v1/cs/configs?dataId="
                + URLEncoder.encode(trimToDefault(dataId, "rk-shared-mybatis.yaml"), StandardCharsets.UTF_8)
                + "&group=DEFAULT_GROUP";
        String script = "set -e\n"
                + "for i in $(seq 1 " + attempts + "); do\n"
                + "  body=$(wget -qO- " + quoteForBash(url) + " 2>/dev/null || true)\n"
                + "  if [ -n \"$body\" ] && echo \"$body\" | grep -q \"spring:\"; then\n"
                + "    echo remote deploy nacos config ready\n"
                + "    exit 0\n"
                + "  fi\n"
                + "  sleep 5\n"
                + "done\n"
                + "echo remote deploy nacos config not ready\n"
                + "exit 1\n";
        try {
            runKubectlWithKubeconfig(kubeconfigPath, request.getKubeContext(),
                    "-n " + quoteForBash(namespace) + " create job " + REMOTE_NACOS_CONFIG_CHECK_JOB
                            + " --image=" + quoteForBash(REMOTE_BUSYBOX_IMAGE)
                            + " -- sh -c " + quoteForBash(script),
                    180,
                    packageId);
            runKubectlWithKubeconfig(kubeconfigPath, request.getKubeContext(),
                    "-n " + quoteForBash(namespace) + " wait --for=condition=complete job/" + REMOTE_NACOS_CONFIG_CHECK_JOB
                            + " --timeout=" + timeoutSeconds + "s",
                    timeoutSeconds + 60,
                    packageId);
            runKubectlWithKubeconfig(kubeconfigPath, request.getKubeContext(),
                    "-n " + quoteForBash(namespace) + " logs job/" + REMOTE_NACOS_CONFIG_CHECK_JOB + " --tail=50",
                    120,
                    packageId);
            updateDeployPackageStep(packageId, 80, "remote deploy nacos config ready", null,
                    "remote deploy nacos config ready");
        } finally {
            cleanupRemoteNacosConfigCheckJob(packageId, kubeconfigPath, request, namespace);
        }
    }

    private void cleanupRemoteNacosConfigCheckJob(Long packageId, Path kubeconfigPath, AdminDeployPackageCreateDTO request, String namespace) {
        try {
            runKubectlWithKubeconfig(kubeconfigPath, request.getKubeContext(),
                    "-n " + quoteForBash(namespace) + " delete job " + REMOTE_NACOS_CONFIG_CHECK_JOB + " --ignore-not-found=true",
                    120,
                    packageId);
        } catch (Exception e) {
            log.warn("remote deploy nacos config check cleanup failed, namespace={}", namespace, e);
        }
    }

    private void restartRemoteDeployments(Long packageId, Path kubeconfigPath, AdminDeployPackageCreateDTO request, String namespace,
                                          List<String> names, int timeoutSeconds) {
        if (names == null || names.isEmpty()) {
            return;
        }
        updateDeployPackageStep(packageId, 84, "remote deploy restarting application workloads after data import", null,
                "remote deploy restarting application workloads after data import");
        for (String rawName : names) {
            String name = trimToEmpty(rawName);
            if (!StringUtils.hasText(name)) {
                continue;
            }
            runKubectlWithKubeconfig(kubeconfigPath, request.getKubeContext(),
                    "-n " + quoteForBash(namespace) + " rollout restart deploy/" + name,
                    Math.min(180, timeoutSeconds + 60),
                    packageId);
        }
    }

    private void waitRemoteRolloutResources(Long packageId, Path kubeconfigPath, AdminDeployPackageCreateDTO request, String namespace,
                                            String resourceKind, List<String> names, int timeoutSeconds) {
        if (names == null || names.isEmpty()) {
            return;
        }
        for (String rawName : names) {
            String name = trimToEmpty(rawName);
            if (!StringUtils.hasText(name)) {
                continue;
            }
            updateDeployPackageProgress(packageId, Math.min(90, Math.max(10, valueAsInteger(jdbcTemplate.queryForObject("SELECT progress FROM ops_deploy_package_record WHERE id = ?", Integer.class, packageId)))),
                    "rollout status " + resourceKind + "/" + name);
            runKubectlWithKubeconfig(kubeconfigPath, request.getKubeContext(),
                    "-n " + quoteForBash(namespace) + " rollout status " + resourceKind + "/" + name + " --timeout=" + timeoutSeconds + "s",
                    timeoutSeconds + 60,
                    packageId);
        }
    }

    private void waitRemoteJob(Long packageId, Path kubeconfigPath, AdminDeployPackageCreateDTO request, String namespace, String jobName, int timeoutSeconds) {
        String rolloutMarker = REMOTE_MYSQL_IMPORT_JOB.equals(jobName)
                ? "rollout status job/rk-migration-mysql-import"
                : "rollout status job/rk-migration-minio-import";
        updateDeployPackageProgress(packageId, Math.min(90, Math.max(10, valueAsInteger(jdbcTemplate.queryForObject("SELECT progress FROM ops_deploy_package_record WHERE id = ?", Integer.class, packageId)))),
                rolloutMarker);
        runKubectlWithKubeconfig(kubeconfigPath, request.getKubeContext(),
                "-n " + quoteForBash(namespace) + " wait --for=condition=complete job/" + jobName + " --timeout=" + timeoutSeconds + "s",
                timeoutSeconds + 60,
                packageId);
    }

    private void cleanupRemoteImportResources(Long packageId, Path kubeconfigPath, AdminDeployPackageCreateDTO request, String namespace, String jobName, String configMapName) {
        if (kubeconfigPath == null || !StringUtils.hasText(namespace)) {
            return;
        }
        try {
            runKubectlWithKubeconfig(kubeconfigPath, request.getKubeContext(),
                    "-n " + quoteForBash(namespace) + " delete job " + jobName + " --ignore-not-found=true",
                    120,
                    packageId);
            if (StringUtils.hasText(configMapName)) {
                runKubectlWithKubeconfig(kubeconfigPath, request.getKubeContext(),
                        "-n " + quoteForBash(namespace) + " delete configmap " + configMapName + " --ignore-not-found=true",
                        120,
                        packageId);
            }
        } catch (Exception e) {
            log.warn("remote deploy temporary import resource cleanup failed, namespace={}, job={}, configMap={}",
                    namespace, jobName, configMapName, e);
        }
    }

    private String buildRemoteMysqlImportJobManifest(String namespace, String mysqlImage) {
        String image = trimToDefault(mysqlImage, DEFAULT_REGISTRY_PREFIX + "/mysql:8.0");
        return "apiVersion: batch/v1\n"
                + "kind: Job\n"
                + "metadata:\n"
                + "  name: " + REMOTE_MYSQL_IMPORT_JOB + "\n"
                + "  namespace: " + namespace + "\n"
                + "spec:\n"
                + "  backoffLimit: 1\n"
                + "  ttlSecondsAfterFinished: 600\n"
                + "  template:\n"
                + "    metadata:\n"
                + "      labels:\n"
                + "        app: " + REMOTE_MYSQL_IMPORT_JOB + "\n"
                + "    spec:\n"
                + "      restartPolicy: Never\n"
                + "      containers:\n"
                + "        - name: mysql-import\n"
                + "          image: " + image + "\n"
                + "          imagePullPolicy: IfNotPresent\n"
                + "          command: [\"/bin/sh\", \"-c\"]\n"
                + "          args:\n"
                + "            - |\n"
                + "              set -e\n"
                + "              until mysqladmin ping -hmysql -P3306 -uroot -p123456 --silent; do sleep 5; done\n"
                + "              mysql_fast_import_stream() {\n"
                + "                { printf '%s\\n' 'SET autocommit=0; SET unique_checks=0; SET foreign_key_checks=0;'; cat; printf '\\n%s\\n' 'COMMIT; SET foreign_key_checks=1; SET unique_checks=1; SET autocommit=1;'; } | mysql --binary-mode=1 -hmysql -P3306 -uroot -p123456\n"
                + "              }\n"
                + "              gzip -dc /migration/rk-all-current-data.sql.gz | mysql_fast_import_stream\n"
                + "              nacos_schema=$(mysql -hmysql -P3306 -uroot -p123456 -N -e \"SELECT TABLE_SCHEMA FROM information_schema.TABLES WHERE TABLE_NAME='config_info' AND TABLE_SCHEMA IN ('nacos','nacos_config') ORDER BY FIELD(TABLE_SCHEMA,'nacos','nacos_config') LIMIT 1\")\n"
                + "              if [ -z \"$nacos_schema\" ]; then echo remote deploy database import verification failed; exit 1; fi\n"
                + "              nacos_count=$(mysql -hmysql -P3306 -uroot -p123456 -N -e \"SELECT COUNT(*) FROM ${nacos_schema}.config_info WHERE data_id='rk-shared-mybatis.yaml' AND group_id='DEFAULT_GROUP'\")\n"
                + "              user_table_count=$(mysql -hmysql -P3306 -uroot -p123456 -N -e \"SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='rk_user'\")\n"
                + "              xxl_job_table_count=$(mysql -hmysql -P3306 -uroot -p123456 -N -e \"SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='xxl_job' AND TABLE_NAME='xxl_job_info'\")\n"
                + "              if [ \"${nacos_count:-0}\" -lt 1 ] || [ \"${user_table_count:-0}\" -lt 1 ] || [ \"${xxl_job_table_count:-0}\" -lt 1 ]; then echo remote deploy database import verification failed; exit 1; fi\n"
                + "              echo remote deploy database imported by temporary job\n"
                + "          volumeMounts:\n"
                + "            - name: payload\n"
                + "              mountPath: /migration\n"
                + "              readOnly: true\n"
                + "      volumes:\n"
                + "        - name: payload\n"
                + "          persistentVolumeClaim:\n"
                + "            claimName: " + REMOTE_MIGRATION_PAYLOAD_PVC + "\n";
    }

    private String buildRemoteMinioImportJobManifest(String namespace, String minioClientImage) {
        String image = trimToDefault(minioClientImage, DEFAULT_MINIO_CLIENT_IMAGE);
        return "apiVersion: batch/v1\n"
                + "kind: Job\n"
                + "metadata:\n"
                + "  name: " + REMOTE_MINIO_IMPORT_JOB + "\n"
                + "  namespace: " + namespace + "\n"
                + "spec:\n"
                + "  backoffLimit: 1\n"
                + "  ttlSecondsAfterFinished: 600\n"
                + "  template:\n"
                + "    metadata:\n"
                + "      labels:\n"
                + "        app: " + REMOTE_MINIO_IMPORT_JOB + "\n"
                + "    spec:\n"
                + "      restartPolicy: Never\n"
                + "      containers:\n"
                + "        - name: minio-import\n"
                + "          image: " + image + "\n"
                + "          imagePullPolicy: IfNotPresent\n"
                + "          command: [\"/bin/sh\", \"-c\"]\n"
                + "          args:\n"
                + "            - |\n"
                + "              set -e\n"
                + "              until mc alias set target http://minio:9000 " + REMOTE_MINIO_ACCESS_KEY + " " + REMOTE_MINIO_SECRET_KEY + "; do sleep 5; done\n"
                + "              for dir in /migration/minio-payload/*; do\n"
                + "                [ -d \"$dir\" ] || continue\n"
                + "                bucket=${dir##*/}\n"
                + "                [ -n \"$bucket\" ] || continue\n"
                + "                mc mb --ignore-existing \"target/$bucket\"\n"
                + "                mc mirror --overwrite \"$dir\" \"target/$bucket\"\n"
                + "                mc anonymous set download \"target/$bucket\"\n"
                + "                echo remote deploy MinIO bucket policies restored for $bucket\n"
                + "              done\n"
                + "              echo remote deploy MinIO imported by temporary job\n"
                + "          volumeMounts:\n"
                + "            - name: payload\n"
                + "              mountPath: /migration\n"
                + "              readOnly: true\n"
                + "      volumes:\n"
                + "        - name: payload\n"
                + "          persistentVolumeClaim:\n"
                + "            claimName: " + REMOTE_MIGRATION_PAYLOAD_PVC + "\n";
    }

    private String runKubectlWithKubeconfig(Path kubeconfigPath, String kubeContext, String kubectlArgs, int timeoutSeconds, Long packageId) {
        String output = sanitizeDeployLog(runLocalShellCommand(buildKubectlCommand(kubeconfigPath, kubeContext, kubectlArgs), timeoutSeconds, new IllegalStateException("kubectl unavailable")));
        if (StringUtils.hasText(output)) {
            updateDeployPackageProgress(packageId, Math.min(95, Math.max(10, valueAsInteger(jdbcTemplate.queryForObject("SELECT progress FROM ops_deploy_package_record WHERE id = ?", Integer.class, packageId)))), output);
        }
        return output;
    }

    private String runKubectlPreflight(Path kubeconfigPath, String kubeContext, String kubectlArgs, int timeoutSeconds) {
        return sanitizeDeployLog(runLocalShellCommand(buildKubectlCommand(kubeconfigPath, kubeContext, kubectlArgs), timeoutSeconds, new IllegalStateException("kubectl unavailable")));
    }

    private String runDiagnosisCommand(Path kubeconfigPath, String kubeContext, String kubectlArgs, int timeoutSeconds,
                                       String label, StringBuilder terminalLog, AdminDeployPackageDiagnosisVO vo) {
        terminalLog.append("\n$ kubectl ").append(kubectlArgs).append('\n');
        try {
            String output = runKubectlPreflight(kubeconfigPath, kubeContext, kubectlArgs, timeoutSeconds);
            terminalLog.append(output).append('\n');
            addDiagnosisCheck(vo, label, "success", firstMeaningfulLine(output));
            return output;
        } catch (Exception e) {
            String message = sanitizeDeployLog(e.getMessage());
            terminalLog.append(message).append('\n');
            addDiagnosisCheck(vo, label, "failed", message);
            return message;
        }
    }

    private String buildKubectlCommand(Path kubeconfigPath, String kubeContext, String kubectlArgs) {
        StringBuilder command = new StringBuilder(quoteForBash(resolveLocalKubectlBinary()))
                .append(" --kubeconfig ")
                .append(quoteForBash(kubeconfigPath.toString()))
                .append(' ');
        if (StringUtils.hasText(kubeContext)) {
            command.append("--context ").append(quoteForBash(kubeContext.trim())).append(' ');
        }
        command.append(kubectlArgs);
        return command.toString();
    }

    private String resolveLocalKubectlBinary() {
        for (String candidate : List.of("/usr/local/bin/kubectl", "/usr/bin/kubectl")) {
            if (Files.isExecutable(Path.of(candidate))) {
                return candidate;
            }
        }
        return "kubectl";
    }

    private List<String> parseKubectlFirstColumn(String output) {
        if (!StringUtils.hasText(output)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String line : output.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (!StringUtils.hasText(trimmed) || trimmed.startsWith("NAME ")) {
                continue;
            }
            String[] parts = trimmed.split("\\s+");
            if (parts.length > 0 && StringUtils.hasText(parts[0])) {
                result.add(parts[0].replace("(default)", ""));
            }
        }
        return result;
    }

    private List<String> parseK8sNodeExternalIps(String output) {
        if (!StringUtils.hasText(output)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String line : output.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (!StringUtils.hasText(trimmed)) {
                continue;
            }
            String[] parts = trimmed.split("\\s+");
            if (parts.length >= 7 && StringUtils.hasText(parts[6]) && !"<none>".equals(parts[6])) {
                result.add(parts[6]);
            }
        }
        return result.stream().distinct().collect(Collectors.toList());
    }

    private String sanitizeDeployLog(String text) {
        if (text == null) {
            return "";
        }
        String sanitized = text;
        sanitized = sanitized.replaceAll("\\x1B(?:[@-Z\\\\-_]|\\[[0-?]*[ -/]*[@-~])", "");
        sanitized = sanitized.replace('\r', '\n');
        sanitized = sanitized.replaceAll("(?im)(certificate-authority-data\\s*:\\s*)\\S+", "$1[REDACTED]");
        sanitized = sanitized.replaceAll("(?im)(client-certificate-data\\s*:\\s*)\\S+", "$1[REDACTED]");
        sanitized = sanitized.replaceAll("(?im)(client-key-data\\s*:\\s*)\\S+", "$1[REDACTED]");
        sanitized = sanitized.replaceAll("(?im)(token\\s*:\\s*)\\S+", "$1[REDACTED]");
        sanitized = sanitized.replaceAll("(?im)(Authorization: Bearer\\s+)\\S+", "$1[REDACTED]");
        sanitized = sanitized.replaceAll("(?i)--token=\\S+", "--token=[REDACTED]");
        sanitized = sanitized.replaceAll("(?i)--docker-password=\\S+", "--docker-password=[REDACTED]");
        sanitized = sanitized.replaceAll("(?im)(REGISTRY_PASSWORD=)\\S+", "$1[REDACTED]");
        return sanitized;
    }

    private String redactKubeconfig(String kubeconfig) {
        String redacted = sanitizeDeployLog(kubeconfig);
        redacted = redacted.replaceAll("(?im)(password\\s*:\\s*)\\S+", "$1[REDACTED]");
        return redacted;
    }

    private String encryptKubeconfig(String kubeconfig) {
        if (!StringUtils.hasText(kubeconfig)) {
            return "";
        }
        try {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, kubeconfigSecretKey(), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(kubeconfig.getBytes(StandardCharsets.UTF_8));
            return "v1:" + Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("encrypt kubeconfig failed", e);
        }
    }

    private String decryptKubeconfig(String ciphertext) {
        if (!StringUtils.hasText(ciphertext)) {
            return "";
        }
        if (!ciphertext.startsWith("v1:")) {
            return ciphertext;
        }
        try {
            String[] parts = ciphertext.split(":", 3);
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] encrypted = Base64.getDecoder().decode(parts[2]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, kubeconfigSecretKey(), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BadRequestException("保存的 kubeconfig 无法解密，请检查 rk.ops.deploy.kubeconfig-secret");
        }
    }

    private SecretKeySpec kubeconfigSecretKey() {
        try {
            String secret = trimToDefault(deployKubeconfigSecret, "rk-web-default-kubeconfig-secret-change-me");
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(digest, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("build kubeconfig encryption key failed", e);
        }
    }

    private String fingerprintKubeconfig(String kubeconfig) {
        if (!StringUtils.hasText(kubeconfig)) {
            return "";
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(kubeconfig.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("fingerprint kubeconfig failed", e);
        }
    }

    private String sha256Hex(String value) {
        try {
            return bytesToHex(MessageDigest.getInstance("SHA-256").digest(trimToEmpty(value).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("sha256 failed", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte item : bytes) {
            builder.append(String.format("%02x", item));
        }
        return builder.toString();
    }

    private AdminDeployPackageCreateDTO buildStoredRemoteDeployRequest(Map<String, Object> row) {
        AdminDeployPackageCreateDTO request = new AdminDeployPackageCreateDTO();
        JsonNode root = readJsonNode(row.get("request_json"));
        request.setPackageMode(jsonText(root, "packageMode", DEPLOY_PACKAGE_MODE_OFFLINE_FULL));
        request.setDeliveryMode(jsonText(root, "deliveryMode", DEPLOY_PACKAGE_DELIVERY_DIRECT));
        request.setImageArtifactMode(jsonText(root, "imageArtifactMode", IMAGE_ARTIFACT_REGISTRY));
        request.setRegistryPrefix(jsonText(root, "registryPrefix", DEFAULT_REGISTRY_PREFIX));
        request.setRegistryServer(jsonText(root, "registryServer", DEFAULT_REGISTRY_SERVER));
        request.setTargetClusterType(jsonText(root, "targetClusterType", TARGET_CLUSTER_ACK));
        request.setTargetNamespace(jsonText(root, "targetNamespace", "shetuanguanlixitong"));
        request.setTargetDomain(jsonText(root, "targetDomain", defaultRemoteDeployDomain()));
        request.setStorageClassName(jsonText(root, "storageClassName", ""));
        request.setExternalExposureType(jsonText(root, "externalExposureType", EXPOSURE_LOAD_BALANCER));
        request.setFrontendNodePort(jsonInteger(root, "frontendNodePort", DEFAULT_FRONTEND_NODE_PORT));
        request.setGatewayNodePort(jsonInteger(root, "gatewayNodePort", DEFAULT_GATEWAY_NODE_PORT));
        request.setIngressClassName(jsonText(root, "ingressClassName", ""));
        request.setIngressHost(jsonText(root, "ingressHost", ""));
        request.setRemoteDeploy(true);
        request.setKubeContext(jsonText(root, "kubeContext", ""));
        request.setWaitRollout(jsonBoolean(root, "waitRollout", true));
        request.setRolloutTimeoutSeconds(jsonInteger(root, "rolloutTimeoutSeconds", 600));
        request.setDryRun(false);
        request.setApplyDatabaseSnapshot(jsonBoolean(root, "applyDatabaseSnapshot", true));
        request.setApplyMinioSnapshot(jsonBoolean(root, "applyMinioSnapshot", true));
        request.setStreamingMigration(jsonBoolean(root, "streamingMigration", true));
        request.setGlobalMigrationLock(jsonBoolean(root, "globalMigrationLock", true));
        String kubeconfig = decryptKubeconfig(valueAsString(row.get("kubeconfig_ciphertext")));
        if (!StringUtils.hasText(kubeconfig)) {
            throw new BadRequestException("该部署历史没有保存 kubeconfig，无法诊断或修复远程集群");
        }
        request.setKubeconfig(kubeconfig);
        return normalizeDeployPackageRequest(request);
    }

    private JsonNode readJsonNode(Object value) {
        if (value == null || !StringUtils.hasText(value.toString())) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(value.toString());
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJsonMap(Object value) {
        if (value == null || !StringUtils.hasText(value.toString())) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(value.toString(), LinkedHashMap.class);
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private String jsonText(JsonNode root, String field, String fallback) {
        JsonNode node = root == null ? null : root.get(field);
        return node == null || node.isNull() ? fallback : node.asText(fallback);
    }

    private Boolean jsonBoolean(JsonNode root, String field, boolean fallback) {
        JsonNode node = root == null ? null : root.get(field);
        return node == null || node.isNull() ? fallback : node.asBoolean(fallback);
    }

    private Integer jsonInteger(JsonNode root, String field, Integer fallback) {
        JsonNode node = root == null ? null : root.get(field);
        return node == null || node.isNull() ? fallback : node.asInt(fallback == null ? 0 : fallback);
    }

    private void addDiagnosisCheck(AdminDeployPackageDiagnosisVO vo, String name, String status, String message) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("status", status);
        item.put("message", limitText(message, 1200));
        vo.getDiagnosisChecks().add(item);
    }

    private void collectRemoteServiceStatuses(AdminDeployPackageDiagnosisVO vo, String snapshot, String nacos, String mysql, String minio) {
        addRemoteServiceStatus(vo, "cluster resources", "all", snapshot);
        addRemoteServiceStatus(vo, "nacos config", "statefulset", nacos + "\nrk-shared-mybatis.yaml");
        addRemoteServiceStatus(vo, "mysql databases", "statefulset", mysql + "\nrk_user xxl_job");
        addRemoteServiceStatus(vo, "minio objects", "statefulset", minio);
    }

    private void addRemoteServiceStatus(AdminDeployPackageDiagnosisVO vo, String name, String kind, String output) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("kind", kind);
        String text = output == null ? "" : output;
        String status = text.contains("ImagePullBackOff") || text.contains("ErrImagePull")
                || text.contains("CrashLoopBackOff") ? "abnormal"
                : (text.contains("Running") || text.contains("1/1") ? "running" : "unknown");
        item.put("status", status);
        item.put("summary", limitText(firstMeaningfulLine(text), 1000));
        vo.getServiceStatuses().add(item);
    }

    private void collectRemoteRepairSuggestions(AdminDeployPackageDiagnosisVO vo, String output) {
        String text = output == null ? "" : output;
        if (text.contains("ImagePullBackOff") || text.contains("ErrImagePull") || text.contains("Pulling image")) {
            addSuggestedRepairAction(vo, REPAIR_DELETE_STUCK_PODS);
        }
        if (text.contains("CrashLoopBackOff") || text.contains("ContainerCreating")) {
            addSuggestedRepairAction(vo, REPAIR_RESTART_WORKLOADS);
        }
        if (text.toLowerCase(Locale.ROOT).contains("minio") || text.contains("502")) {
            addSuggestedRepairAction(vo, REPAIR_MINIO_PUBLIC_POLICY);
        }
        if (text.toLowerCase(Locale.ROOT).contains("mysql") || text.contains("rk_user") || text.contains("xxl_job")) {
            addSuggestedRepairAction(vo, REPAIR_RERUN_MYSQL_IMPORT);
        }
        if (vo.getSuggestedRepairActions().isEmpty()) {
            addSuggestedRepairAction(vo, REPAIR_RESTART_WORKLOADS);
        }
    }

    private void addSuggestedRepairAction(AdminDeployPackageDiagnosisVO vo, String action) {
        if (!vo.getSuggestedRepairActions().contains(action)) {
            vo.getSuggestedRepairActions().add(action);
        }
    }

    private void repairRestartRemoteWorkloads(Path kubeconfigPath, AdminDeployPackageCreateDTO request, String namespace, List<String> outputs) {
        for (String name : K8S_PERSISTENT_MIDDLEWARE) {
            outputs.add(runRepairKubectl(kubeconfigPath, request.getKubeContext(),
                    "-n " + quoteForBash(namespace) + " rollout restart statefulset/" + name,
                    120));
        }
        for (String name : REMOTE_K8S_DEPLOYMENTS) {
            outputs.add(runRepairKubectl(kubeconfigPath, request.getKubeContext(),
                    "-n " + quoteForBash(namespace) + " rollout restart deploy/" + name,
                    120));
        }
    }

    private void repairDeleteStuckPods(Path kubeconfigPath, AdminDeployPackageCreateDTO request, String namespace, List<String> outputs) {
        String pods = runRepairKubectl(kubeconfigPath, request.getKubeContext(),
                "-n " + quoteForBash(namespace) + " get pods --no-headers",
                90);
        outputs.add(pods);
        for (String podName : parseStuckPodNames(pods)) {
            outputs.add(runRepairKubectl(kubeconfigPath, request.getKubeContext(),
                    "-n " + quoteForBash(namespace) + " delete pod " + quoteForBash(podName) + " --ignore-not-found=true",
                    120));
        }
    }

    private List<String> parseStuckPodNames(String podsOutput) {
        if (!StringUtils.hasText(podsOutput)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String line : podsOutput.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (!StringUtils.hasText(trimmed)) {
                continue;
            }
            String[] parts = trimmed.split("\\s+");
            if (parts.length >= 3) {
                String status = parts[2];
                if (!"Running".equalsIgnoreCase(status)
                        && !"Completed".equalsIgnoreCase(status)
                        && !"Succeeded".equalsIgnoreCase(status)) {
                    result.add(parts[0]);
                }
            }
        }
        return result;
    }

    private void repairMinioPublicPolicy(Path kubeconfigPath, AdminDeployPackageCreateDTO request, String namespace, List<String> outputs) throws Exception {
        outputs.add(runRepairKubectl(kubeconfigPath, request.getKubeContext(),
                "-n " + quoteForBash(namespace) + " delete job " + REMOTE_MINIO_POLICY_REPAIR_JOB + " --ignore-not-found=true",
                120));
        Path manifest = Files.createTempFile("rk-remote-minio-policy-repair-", ".yaml");
        try {
            Files.writeString(manifest, buildRemoteMinioPolicyRepairJobManifest(namespace), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            outputs.add(runRepairKubectl(kubeconfigPath, request.getKubeContext(), "apply -f " + quoteForBash(manifest.toString()), 120));
            outputs.add(runRepairKubectl(kubeconfigPath, request.getKubeContext(),
                    "-n " + quoteForBash(namespace) + " wait --for=condition=complete job/" + REMOTE_MINIO_POLICY_REPAIR_JOB + " --timeout=600s",
                    700));
            outputs.add(runRepairKubectl(kubeconfigPath, request.getKubeContext(),
                    "-n " + quoteForBash(namespace) + " logs job/" + REMOTE_MINIO_POLICY_REPAIR_JOB + " --tail=120",
                    120));
        } finally {
            Files.deleteIfExists(manifest);
            outputs.add(runRepairKubectl(kubeconfigPath, request.getKubeContext(),
                    "-n " + quoteForBash(namespace) + " delete job " + REMOTE_MINIO_POLICY_REPAIR_JOB + " --ignore-not-found=true",
                    120));
        }
    }

    private String buildRemoteMinioPolicyRepairJobManifest(String namespace) {
        return "apiVersion: batch/v1\n"
                + "kind: Job\n"
                + "metadata:\n"
                + "  name: " + REMOTE_MINIO_POLICY_REPAIR_JOB + "\n"
                + "  namespace: " + namespace + "\n"
                + "spec:\n"
                + "  backoffLimit: 1\n"
                + "  template:\n"
                + "    spec:\n"
                + "      restartPolicy: Never\n"
                + "      containers:\n"
                + "        - name: minio-policy\n"
                + "          image: " + DEFAULT_MINIO_CLIENT_IMAGE + "\n"
                + "          command: [\"/bin/sh\", \"-c\"]\n"
                + "          args:\n"
                + "            - |\n"
                + "              set -e\n"
                + "              mc alias set target http://minio:9000 " + REMOTE_MINIO_ACCESS_KEY + " " + REMOTE_MINIO_SECRET_KEY + "\n"
                + "              mc ls target | awk '{print $NF}' | sed 's#/$##' | while read bucket; do\n"
                + "                [ -z \"$bucket\" ] && continue\n"
                + "                mc anonymous set download \"target/$bucket\"\n"
                + "              done\n";
    }

    private String runRepairKubectl(Path kubeconfigPath, String kubeContext, String kubectlArgs, int timeoutSeconds) {
        try {
            return runKubectlPreflight(kubeconfigPath, kubeContext, kubectlArgs, timeoutSeconds);
        } catch (Exception e) {
            return sanitizeDeployLog(e.getMessage());
        }
    }

    @Override
    public void streamDeployPackage(Long packageId, OutputStream outputStream) {
        AdminDeployPackageRecordVO record = loadDeployPackageRecord(packageId);
        if (record == null || !StringUtils.hasText(record.getRemotePath())) {
            throw new BadRequestException("部署包不存在或尚未生成完成");
        }
        if (!backupStorageService.exists(record.getRemotePath())) {
            throw new BadRequestException("部署包文件不存在或已被清理，请重新生成");
        }
        backupStorageService.stream(record.getRemotePath(), outputStream);
        try {
            outputStream.flush();
        } catch (Exception e) {
            throw new IllegalStateException("flush deploy package response failed", e);
        }
        if (Boolean.TRUE.equals(record.getDeleteAfterDownload())) {
            backupStorageService.delete(record.getRemotePath());
            jdbcTemplate.update(
                    "UPDATE ops_deploy_package_record SET downloaded = 1, downloaded_at = NOW(), remote_path = NULL, " +
                            "logs = CONCAT(COALESCE(logs,''), ?), update_time = NOW() WHERE id = ?",
                    "client downloaded package, temporary server object cleaned\n",
                    packageId
            );
        } else {
            jdbcTemplate.update(
                    "UPDATE ops_deploy_package_record SET downloaded = 1, downloaded_at = NOW(), " +
                            "logs = CONCAT(COALESCE(logs,''), ?), update_time = NOW() WHERE id = ?",
                    "client downloaded package, MinIO object retained\n",
                    packageId
            );
        }
    }

    @Override
    public void streamLinuxSshDeployPackage(Long packageId, String token, OutputStream outputStream) {
        String remotePath = validateLinuxSshPackageDownloadToken(packageId, token);
        try {
            backupStorageService.stream(remotePath, outputStream);
            outputStream.flush();
        } catch (Exception e) {
            throw new IllegalStateException("stream linux ssh deploy package failed", e);
        }
    }

    private String validateLinuxSshPackageDownloadToken(Long packageId, String token) {
        if (packageId == null || !StringUtils.hasText(token)) {
            throw new BadRequestException("deploy package download token is invalid");
        }
        String value = stringRedisTemplate.opsForValue().get(linuxSshPackageDownloadTokenKey(packageId));
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException("deploy package download token expired");
        }
        String[] parts = value.split("\\n", 2);
        if (parts.length != 2 || !StringUtils.hasText(parts[1])) {
            throw new BadRequestException("deploy package download token state is invalid");
        }
        if (!MessageDigest.isEqual(parts[0].getBytes(StandardCharsets.UTF_8), sha256Hex(token).getBytes(StandardCharsets.UTF_8))) {
            throw new BadRequestException("deploy package download token is invalid");
        }
        if (!backupStorageService.exists(parts[1])) {
            throw new BadRequestException("deploy package object does not exist");
        }
        return parts[1];
    }

    @Override
    public List<AdminMinioBucketVO> listMinioBuckets() {
        requireTenantOneOpsAccess();
        try {
            List<AdminMinioBucketVO> result = new ArrayList<>();
            for (Bucket bucket : minioClient.listBuckets()) {
                if (bucket == null || !StringUtils.hasText(bucket.name())) {
                    continue;
                }
                long objectCount = 0L;
                long totalBytes = 0L;
                Iterable<Result<Item>> objects = minioClient.listObjects(
                        ListObjectsArgs.builder()
                                .bucket(bucket.name())
                                .recursive(true)
                                .build()
                );
                for (Result<Item> object : objects) {
                    Item item = object.get();
                    if (item == null || item.isDir()) {
                        continue;
                    }
                    objectCount++;
                    totalBytes += Math.max(0L, item.size());
                }
                AdminMinioBucketVO vo = new AdminMinioBucketVO();
                vo.setName(bucket.name());
                vo.setCreationDate(bucket.creationDate() == null ? "" : bucket.creationDate().toString());
                vo.setObjectCount(objectCount);
                vo.setTotalBytes(totalBytes);
                vo.setTotalSize(formatBytes(totalBytes));
                result.add(vo);
            }
            result.sort(Comparator.comparing(AdminMinioBucketVO::getName, Comparator.nullsLast(String::compareTo)));
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("list MinIO buckets failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<AdminMinioObjectVO> listMinioObjects(String bucket, String prefix, Boolean recursive, Integer limit) {
        requireTenantOneOpsAccess();
        String bucketName = sanitizeMinioBucketName(bucket);
        String objectPrefix = sanitizeMinioPrefix(prefix);
        int max = Math.max(1, Math.min(limit == null ? 200 : limit, 1000));
        try {
            List<AdminMinioObjectVO> result = new ArrayList<>();
            Iterable<Result<Item>> objects = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(objectPrefix)
                            .recursive(!Boolean.FALSE.equals(recursive))
                            .build()
            );
            for (Result<Item> object : objects) {
                if (result.size() >= max) {
                    break;
                }
                Item item = object.get();
                if (item == null || item.isDir() || !StringUtils.hasText(item.objectName())) {
                    continue;
                }
                AdminMinioObjectVO vo = new AdminMinioObjectVO();
                vo.setBucket(bucketName);
                vo.setObjectName(item.objectName());
                vo.setFileName(extractObjectFileName(item.objectName()));
                vo.setSizeBytes(item.size());
                vo.setSize(formatBytes(item.size()));
                vo.setLastModified(item.lastModified() == null ? "" : item.lastModified().toString());
                vo.setEtag(item.etag());
                vo.setDownloadUrl("/admin/ops/minio/object/download?bucket="
                        + encodePathSegment(bucketName)
                        + "&object="
                        + encodePathSegment(item.objectName()));
                result.add(vo);
            }
            result.sort(Comparator.comparing(AdminMinioObjectVO::getObjectName, Comparator.nullsLast(String::compareTo)));
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("list MinIO objects failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void streamMinioObject(String bucket, String objectName, OutputStream outputStream) {
        requireTenantOneOpsAccess();
        String bucketName = sanitizeMinioBucketName(bucket);
        String safeObjectName = sanitizeMinioObjectNameForRead(objectName);
        try (InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(safeObjectName)
                        .build()
        )) {
            inputStream.transferTo(outputStream);
            outputStream.flush();
        } catch (Exception e) {
            throw new IllegalStateException("download MinIO object failed: " + e.getMessage(), e);
        }
    }

    @Override
    public AdminK8sOverviewVO getK8sOverview() {
        AdminK8sOverviewVO inClusterOverview = loadInClusterK8sOverview();
        if (inClusterOverview != null) {
            return inClusterOverview;
        }

        AdminK8sOverviewVO overview = new AdminK8sOverviewVO();
        try {
        String clientVersionOutput = runRemoteCommand("bash -lc " + quoteForBash("kubectl version --client --output=yaml 2>/dev/null || echo NO_KUBECTL"));
        boolean kubectlInstalled = !clientVersionOutput.contains("NO_KUBECTL");
        overview.setKubectlInstalled(kubectlInstalled);
        overview.setClientVersion(extractYamlValue(clientVersionOutput, "gitVersion"));

        String clusterInfoOutput = kubectlInstalled
                ? runRemoteCommand("bash -lc " + quoteForBash("kubectl cluster-info 2>/dev/null || echo NO_CLUSTER"))
                : "NO_KUBECTL";
        boolean clusterReachable = kubectlInstalled && !clusterInfoOutput.contains("NO_CLUSTER");
        overview.setClusterReachable(clusterReachable);
        overview.setClusterMessage(clusterReachable
                ? firstMeaningfulLine(clusterInfoOutput)
                : (kubectlInstalled ? "已安装 kubectl，但当前未连接到 Kubernetes 集群" : "当前环境未检测到 kubectl"));
        overview.setNodes(clusterReachable
                ? parseNodeLines(runRemoteCommand("bash -lc " + quoteForBash("kubectl get nodes -o wide --no-headers 2>/dev/null || true")))
                : Collections.emptyList());
        overview.setNamespaces(clusterReachable
                ? parseNamespaces(runRemoteCommand("bash -lc " + quoteForBash("kubectl get ns --no-headers 2>/dev/null || true")))
                : Collections.emptyList());
        overview.setContainers(parseDockerLines(runRemoteCommand("docker ps --format '{{.Names}}\\t{{.Image}}\\t{{.Ports}}'")));
        } catch (IllegalStateException e) {
            log.warn("load k8s overview failed, degrade to empty state", e);
            overview.setKubectlInstalled(Boolean.FALSE);
            overview.setClusterReachable(Boolean.FALSE);
            overview.setClientVersion(null);
            overview.setClusterMessage("远程运维探测当前不可用，请检查 rk-user 服务中的远程脚本配置或容器依赖");
            overview.setNodes(Collections.emptyList());
            overview.setNamespaces(Collections.emptyList());
            overview.setContainers(Collections.emptyList());
        }
        return overview;
    }

    @Override
    public AdminK8sCleanupResultVO runK8sCleanup(AdminK8sCleanupRequestDTO dto) {
        requireTenantOneOpsAccess();
        AdminK8sNodeVO targetNode = resolveCleanupTargetNode(dto);
        dto.setNodeName(targetNode.getName());
        dto.setNodeIp(targetNode.getInternalIp());
        if (Boolean.FALSE.equals(dto.getDryRun())) {
            requireDangerConfirmation("CLEANUP", null, targetNode.getInternalIp(), dto.getConfirmText());
        }
        String command = buildK8sCleanupRemoteCommand(dto);
        AdminK8sCleanupResultVO result = new AdminK8sCleanupResultVO();
        result.setNodeName(dto.getNodeName());
        result.setNodeIp(dto.getNodeIp());
        result.setActions(normalizeCleanupActions(dto.getActions()));
        result.setDryRun(!Boolean.FALSE.equals(dto.getDryRun()));
        result.setCommand(command);
        result.setReason(emptyToNull(dto.getReason()));
        result.setExecutedAt(LocalDateTime.now().format(TIME_FORMATTER));
        try {
            String inClusterOutput = runInClusterK8sCleanup(dto, targetNode);
            result.setOutput(inClusterOutput == null ? runRemoteCommand(command) : inClusterOutput);
            result.setSuccess(Boolean.TRUE);
            result.setMessage(Boolean.TRUE.equals(result.getDryRun()) ? "清理预览已完成" : "清理任务已执行");
        } catch (Exception e) {
            log.warn("run k8s cleanup failed, nodeIp={}, actions={}", dto.getNodeIp(), dto.getActions(), e);
            result.setOutput(e.getMessage());
            result.setSuccess(Boolean.FALSE);
            result.setMessage("清理执行失败，请检查节点 SSH 免密或容器运行时工具");
        }
        result.setAuditId(insertOperationAudit(
                "K8S_CLEANUP",
                k8sCleanupNamespace,
                "Node",
                targetNode.getInternalIp(),
                String.join(",", result.getActions()),
                Boolean.TRUE.equals(result.getSuccess()) ? "success" : "failed",
                result.getReason(),
                toJson(Map.of(
                        "nodeName", dto.getNodeName(),
                        "nodeIp", dto.getNodeIp(),
                        "actions", result.getActions(),
                        "dryRun", result.getDryRun()
                )),
                result.getOutput()
        ));
        return result;
    }

    @Override
    public List<AdminK8sImageVO> listK8sImages(String nodeName, String keyword, Boolean unusedOnly) {
        requireTenantOneOpsAccess();
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : "";
        boolean onlyUnused = Boolean.TRUE.equals(unusedOnly);
        Map<String, ImageUsage> usageByImage = buildK8sImageUsageMap();
        List<AdminK8sNodeVO> nodes = getK8sOverview().getNodes();
        String requestedNodeName = StringUtils.hasText(nodeName) ? sanitizeCleanupToken(nodeName, "节点名称") : "";
        if (StringUtils.hasText(requestedNodeName)) {
            nodes = nodes.stream()
                    .filter(node -> Objects.equals(requestedNodeName, node.getName()))
                    .collect(Collectors.toList());
        }
        List<AdminK8sImageVO> result = new ArrayList<>();
        for (AdminK8sNodeVO node : nodes) {
            try {
                String output = runInClusterNodeScript(node, buildK8sImageListScript());
                if (StringUtils.hasText(output)) {
                    result.addAll(parseK8sImageLines(node, output, usageByImage));
                }
            } catch (Exception e) {
                log.warn("list runtime images from node failed, node={}", node.getName(), e);
            }
        }
        if (result.isEmpty()) {
            result.addAll(buildFallbackK8sImagesFromWorkloads(usageByImage, requestedNodeName));
        }
        return result.stream()
                .filter(item -> !StringUtils.hasText(normalizedKeyword)
                        || String.valueOf(item.getImage()).toLowerCase().contains(normalizedKeyword)
                        || String.valueOf(item.getRepository()).toLowerCase().contains(normalizedKeyword)
                        || String.valueOf(item.getNodeName()).toLowerCase().contains(normalizedKeyword))
                .filter(item -> !onlyUnused || Boolean.TRUE.equals(item.getCanPrune()))
                .sorted(Comparator
                        .comparing(AdminK8sImageVO::getNodeName, Comparator.nullsLast(String::compareTo))
                        .thenComparing(AdminK8sImageVO::getImage, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());
    }

    @Override
    public AdminK8sCleanupResultVO cleanupK8sImages(AdminK8sImageCleanupDTO dto) {
        requireTenantOneOpsAccess();
        AdminK8sImageCleanupDTO request = dto == null ? new AdminK8sImageCleanupDTO() : dto;
        AdminK8sNodeVO targetNode = resolveImageCleanupTargetNode(request);
        boolean dryRun = !Boolean.FALSE.equals(request.getDryRun());
        if (!dryRun) {
            requireDangerConfirmation("CLEANUP", null, targetNode.getInternalIp(), request.getConfirmText());
        }
        List<String> selectedImages = normalizeImageCleanupSelection(request, targetNode);
        String cleanupScript = buildK8sImageCleanupScript(selectedImages, dryRun);
        String command = buildK8sImageCleanupRemoteCommand(targetNode, cleanupScript);
        AdminK8sCleanupResultVO result = new AdminK8sCleanupResultVO();
        result.setNodeName(targetNode.getName());
        result.setNodeIp(targetNode.getInternalIp());
        result.setActions(List.of("IMAGE_PRUNE_SELECTED"));
        result.setDryRun(dryRun);
        result.setCommand(command);
        result.setReason(emptyToNull(request.getReason()));
        result.setExecutedAt(LocalDateTime.now().format(TIME_FORMATTER));
        try {
            String output = runInClusterNodeScript(targetNode, cleanupScript);
            result.setOutput(output == null ? runRemoteCommand(command) : output);
            result.setSuccess(Boolean.TRUE);
            result.setMessage(dryRun ? "镜像清理预览已完成" : "镜像清理已执行");
        } catch (Exception e) {
            log.warn("cleanup k8s runtime images failed, node={}", targetNode.getName(), e);
            result.setSuccess(Boolean.FALSE);
            result.setMessage("镜像清理失败: " + e.getMessage());
            result.setOutput(e.getMessage());
        }
        result.setAuditId(insertOperationAudit(
                "K8S_IMAGE_CLEANUP",
                k8sCleanupNamespace,
                "Node",
                targetNode.getName(),
                dryRun ? "DRY_RUN" : "DELETE",
                Boolean.TRUE.equals(result.getSuccess()) ? "success" : "failed",
                result.getReason(),
                toJson(Map.of(
                        "nodeName", targetNode.getName(),
                        "nodeIp", targetNode.getInternalIp(),
                        "images", selectedImages,
                        "dryRun", dryRun
                )),
                result.getOutput()
        ));
        return result;
    }

    @Override
    public byte[] downloadK8sImageExportScript(String nodeName, String nodeIp, String image) {
        requireTenantOneOpsAccess();
        String safeImage = sanitizeContainerImage(image);
        String safeNodeName = StringUtils.hasText(nodeName) ? sanitizeCleanupToken(nodeName, "节点名称") : "";
        String safeNodeIp = StringUtils.hasText(nodeIp) ? sanitizeCleanupToken(nodeIp, "节点 IP") : "";
        String script = "#!/usr/bin/env bash\n"
                + "set -euo pipefail\n"
                + "IMAGE=" + quoteForBash(safeImage) + "\n"
                + "NODE_NAME=" + quoteForBash(safeNodeName) + "\n"
                + "NODE_IP=" + quoteForBash(safeNodeIp) + "\n"
                + "OUT_DIR=${1:-./images}\n"
                + "mkdir -p \"$OUT_DIR\"\n"
                + "safe=$(printf '%s' \"$IMAGE\" | tr '/:@' '___')\n"
                + "target=\"$OUT_DIR/${safe}.tar\"\n"
                + "echo \"export image: $IMAGE\"\n"
                + "echo \"source node: ${NODE_NAME:-unknown} ${NODE_IP:-}\"\n"
                + "CTR=$(command -v ctr 2>/dev/null || true)\n"
                + "[ -n \"$CTR\" ] || [ ! -x /var/lib/rancher/rke2/bin/ctr ] || CTR=/var/lib/rancher/rke2/bin/ctr\n"
                + "[ -n \"$CTR\" ] || [ ! -x /var/lib/rancher/k3s/data/current/bin/ctr ] || CTR=/var/lib/rancher/k3s/data/current/bin/ctr\n"
                + "CONTAINERD_ADDRESS=\n"
                + "for sock in /run/k3s/containerd/containerd.sock /run/rke2/containerd/containerd.sock /run/containerd/containerd.sock; do if [ -S \"$sock\" ]; then CONTAINERD_ADDRESS=\"$sock\"; break; fi; done\n"
                + "ctr_cmd() { if [ -n \"$CONTAINERD_ADDRESS\" ]; then \"$CTR\" --address \"$CONTAINERD_ADDRESS\" \"$@\"; else \"$CTR\" \"$@\"; fi; }\n"
                + "if [ -n \"$CTR\" ]; then ctr_cmd -n k8s.io images export \"$target\" \"$IMAGE\"; elif command -v docker >/dev/null 2>&1; then docker save \"$IMAGE\" -o \"$target\"; else echo 'ctr/docker not found' >&2; exit 2; fi\n"
                + "ls -lh \"$target\"\n";
        return script.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public List<AdminK8sAbnormalPodVO> listK8sAbnormalPods(List<String> namespaces, List<String> statuses) {
        requireTenantOneOpsAccess();
        Set<String> namespaceFilter = normalizeK8sTextFilter(namespaces);
        Set<String> statusFilter = normalizeK8sTextFilter(statuses);
        List<AdminK8sAbnormalPodVO> result = new ArrayList<>();
        for (JsonNode pod : loadK8sPodItemsJson()) {
            String namespace = pod.path("metadata").path("namespace").asText("");
            if (!namespaceFilter.isEmpty() && !namespaceFilter.contains(namespace.toLowerCase())) {
                continue;
            }
            AdminK8sAbnormalPodVO item = mapK8sAbnormalPod(pod);
            if (item == null) {
                continue;
            }
            if (!statusFilter.isEmpty() && !statusFilter.contains(item.getStatus().toLowerCase())) {
                continue;
            }
            result.add(item);
        }
        result.sort(Comparator
                .comparing(AdminK8sAbnormalPodVO::getNamespace, Comparator.nullsLast(String::compareTo))
                .thenComparing(AdminK8sAbnormalPodVO::getName, Comparator.nullsLast(String::compareTo)));
        return result;
    }

    @Override
    public AdminK8sMaintenanceResultVO cleanupK8sAbnormalPods(AdminK8sAbnormalPodCleanupDTO dto) {
        requireTenantOneOpsAccess();
        AdminK8sAbnormalPodCleanupDTO request = dto == null ? new AdminK8sAbnormalPodCleanupDTO() : dto;
        boolean dryRun = !Boolean.FALSE.equals(request.getDryRun());
        boolean includeControllerManaged = Boolean.TRUE.equals(request.getIncludeControllerManaged());
        if (!dryRun) {
            requireExactConfirmation(K8S_ABNORMAL_POD_CONFIRM_TEXT, request.getConfirmText());
        }
        List<AdminK8sAbnormalPodVO> abnormalPods = listK8sAbnormalPods(request.getNamespaces(), request.getStatuses());
        List<AdminK8sAbnormalPodVO> candidates = abnormalPods.stream()
                .filter(item -> isK8sAbnormalPodDeleteCandidate(item, includeControllerManaged))
                .collect(Collectors.toList());
        StringBuilder output = new StringBuilder();
        for (AdminK8sAbnormalPodVO pod : candidates) {
            if (dryRun) {
                output.append("DRY-RUN delete pod ")
                        .append(pod.getNamespace()).append('/').append(pod.getName())
                        .append(" status=").append(pod.getStatus())
                        .append(" owner=").append(firstNonEmpty(pod.getOwnerKind(), "-"))
                        .append('/').append(firstNonEmpty(pod.getOwnerName(), "-"))
                        .append('\n');
                continue;
            }
            try {
                output.append(deleteK8sAbnormalPod(pod)).append('\n');
            } catch (Exception e) {
                output.append("FAILED delete pod ")
                        .append(pod.getNamespace()).append('/').append(pod.getName())
                        .append(": ").append(e.getMessage()).append('\n');
            }
        }

        AdminK8sMaintenanceResultVO result = new AdminK8sMaintenanceResultVO();
        result.setDryRun(dryRun);
        result.setSuccess(Boolean.TRUE);
        result.setMessage(dryRun ? "异常 Pod 清理预览已生成" : "异常 Pod 清理已执行");
        result.setExecutedAt(LocalDateTime.now().format(TIME_FORMATTER));
        result.setReason(emptyToNull(request.getReason()));
        result.setAbnormalPodCount(abnormalPods.size());
        result.setAbnormalPodDeletedCount(candidates.size());
        result.setAbnormalPods(candidates);
        result.setNodeResults(Collections.emptyList());
        result.setOutput(output.length() == 0 ? "no abnormal pod delete candidates" : output.toString());
        result.setAuditId(insertOperationAudit(
                "K8S_ABNORMAL_POD",
                k8sCleanupNamespace,
                "Pod",
                "abnormal-pods",
                dryRun ? "DRY_RUN" : "DELETE",
                "success",
                result.getReason(),
                toJson(Map.of(
                        "namespaces", request.getNamespaces() == null ? Collections.emptyList() : request.getNamespaces(),
                        "statuses", request.getStatuses() == null ? Collections.emptyList() : request.getStatuses(),
                        "includeControllerManaged", includeControllerManaged,
                        "dryRun", dryRun
                )),
                result.getOutput()
        ));
        return result;
    }

    @Override
    public AdminK8sMaintenanceResultVO runK8sClusterMaintenance(AdminK8sClusterMaintenanceDTO dto) {
        requireTenantOneOpsAccess();
        AdminK8sClusterMaintenanceDTO request = dto == null ? new AdminK8sClusterMaintenanceDTO() : dto;
        boolean dryRun = !Boolean.FALSE.equals(request.getDryRun());
        if (!dryRun) {
            requireExactConfirmation(K8S_CLUSTER_MAINTENANCE_CONFIRM_TEXT, request.getConfirmText());
        }
        List<String> actions = normalizeMaintenanceActions(request.getActions());
        boolean cleanupNodeDisk = !Boolean.FALSE.equals(request.getCleanupNodeDisk());
        boolean cleanupAbnormalPods = !Boolean.FALSE.equals(request.getCleanupAbnormalPods());
        List<AdminK8sCleanupResultVO> nodeResults = new ArrayList<>();
        AdminK8sMaintenanceResultVO abnormalResult = null;
        StringBuilder output = new StringBuilder();

        if (cleanupAbnormalPods) {
            AdminK8sAbnormalPodCleanupDTO abnormalRequest = new AdminK8sAbnormalPodCleanupDTO();
            abnormalRequest.setNamespaces(request.getNamespaces());
            abnormalRequest.setStatuses(request.getStatuses());
            abnormalRequest.setIncludeControllerManaged(request.getIncludeControllerManaged());
            abnormalRequest.setDryRun(dryRun);
            abnormalRequest.setConfirmText(dryRun ? null : K8S_ABNORMAL_POD_CONFIRM_TEXT);
            abnormalRequest.setReason(firstNonEmpty(request.getReason(), "cluster maintenance abnormal pod cleanup"));
            abnormalResult = cleanupK8sAbnormalPods(abnormalRequest);
            output.append("[abnormal-pods]\n").append(firstNonEmpty(abnormalResult.getOutput(), "")).append('\n');
        }

        if (cleanupNodeDisk) {
            for (AdminK8sNodeVO node : getK8sOverview().getNodes()) {
                if (!StringUtils.hasText(node.getInternalIp())) {
                    continue;
                }
                AdminK8sCleanupRequestDTO cleanupRequest = new AdminK8sCleanupRequestDTO();
                cleanupRequest.setNodeName(node.getName());
                cleanupRequest.setNodeIp(node.getInternalIp());
                cleanupRequest.setActions(actions);
                cleanupRequest.setDryRun(dryRun);
                cleanupRequest.setConfirmText(dryRun ? null : node.getInternalIp());
                cleanupRequest.setReason(firstNonEmpty(request.getReason(), "cluster maintenance node disk cleanup"));
                AdminK8sCleanupResultVO nodeResult = runK8sCleanup(cleanupRequest);
                nodeResults.add(nodeResult);
                output.append("[node ").append(node.getName()).append(" / ").append(node.getInternalIp()).append("]\n")
                        .append(firstNonEmpty(nodeResult.getOutput(), "")).append('\n');
            }
        }

        boolean nodeSuccess = nodeResults.stream().allMatch(item -> Boolean.TRUE.equals(item.getSuccess()));
        boolean abnormalSuccess = abnormalResult == null || Boolean.TRUE.equals(abnormalResult.getSuccess());
        AdminK8sMaintenanceResultVO result = new AdminK8sMaintenanceResultVO();
        result.setSuccess(nodeSuccess && abnormalSuccess);
        result.setDryRun(dryRun);
        result.setMessage(dryRun ? "集群巡检预览已完成" : "集群巡检与清理已执行");
        result.setExecutedAt(LocalDateTime.now().format(TIME_FORMATTER));
        result.setReason(emptyToNull(request.getReason()));
        result.setAbnormalPodCount(abnormalResult == null ? 0 : abnormalResult.getAbnormalPodCount());
        result.setAbnormalPodDeletedCount(abnormalResult == null ? 0 : abnormalResult.getAbnormalPodDeletedCount());
        result.setAbnormalPods(abnormalResult == null ? Collections.emptyList() : abnormalResult.getAbnormalPods());
        result.setNodeResults(nodeResults);
        result.setOutput(output.length() == 0 ? "no maintenance actions selected" : output.toString());
        result.setAuditId(insertOperationAudit(
                "K8S_MAINTENANCE",
                k8sCleanupNamespace,
                "Cluster",
                "all-nodes",
                dryRun ? "DRY_RUN" : "RUN",
                Boolean.TRUE.equals(result.getSuccess()) ? "success" : "failed",
                result.getReason(),
                toJson(Map.of(
                        "actions", actions,
                        "cleanupNodeDisk", cleanupNodeDisk,
                        "cleanupAbnormalPods", cleanupAbnormalPods,
                        "dryRun", dryRun
                )),
                result.getOutput()
        ));
        return result;
    }

    @Override
    public Map<String, Object> ensureK8sMaintenanceXxlJob(AdminK8sClusterMaintenanceDTO dto) {
        requireTenantOneOpsAccess();
        AdminK8sClusterMaintenanceDTO request = dto == null ? new AdminK8sClusterMaintenanceDTO() : dto;
        try {
            Long jobGroup = resolveK8sMaintenanceJobGroupId();
            JsonNode existing = findXxlJobByHandler(K8S_MAINTENANCE_JOB_HANDLER);
            Map<String, String> form = buildK8sMaintenanceXxlJobForm(jobGroup, request);
            Long jobId;
            boolean created = existing == null;
            if (created) {
                JsonNode addResult = invokeXxlForm("jobinfo/add", form);
                if (addResult.path("code").asInt() != 200) {
                    throw new IllegalStateException(addResult.path("msg").asText(addResult.toString()));
                }
                jobId = readXxlJobId(addResult);
                if (jobId == null) {
                    JsonNode createdJob = findXxlJobByHandler(K8S_MAINTENANCE_JOB_HANDLER);
                    if (createdJob != null) {
                        jobId = createdJob.path("id").asLong();
                    }
                }
            } else {
                jobId = existing.path("id").asLong();
                Map<String, String> updateForm = new LinkedHashMap<>(form);
                updateForm.put("id", String.valueOf(jobId));
                JsonNode updateResult = invokeXxlForm("jobinfo/update", updateForm);
                if (updateResult.path("code").asInt() != 200) {
                    throw new IllegalStateException(updateResult.path("msg").asText(updateResult.toString()));
                }
            }
            boolean started = jobId != null && jobId > 0 && invokeXxlAction("jobinfo/start", Map.of("id", String.valueOf(jobId)));
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("created", created);
            result.put("updated", !created);
            result.put("started", started);
            result.put("jobId", jobId);
            result.put("jobGroup", jobGroup);
            result.put("handler", K8S_MAINTENANCE_JOB_HANDLER);
            result.put("cron", K8S_MAINTENANCE_CRON);
            result.put("sourceUrl", xxlAdminUrl);
            result.put("message", "K8s half-month maintenance XXL-Job synchronized");
            return result;
        } catch (Exception e) {
            log.error("ensure k8s maintenance xxl job failed", e);
            throw new IllegalStateException("同步 K8s 半月巡检 XXL-Job 失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<AdminK8sWorkloadVO> getK8sWorkloads(String namespace) {
        requireTenantOneOpsAccess();
        String resolvedNamespace = StringUtils.hasText(namespace) ? sanitizeCleanupToken(namespace, "命名空间") : k8sCleanupNamespace;
        String token = readK8sServiceAccountToken();
        String apiUrl = resolveK8sApiUrl();
        if (StringUtils.hasText(token) && StringUtils.hasText(apiUrl)) {
            try {
                HttpClient client = buildK8sHttpClient(apiUrl);
                List<AdminK8sWorkloadVO> result = new ArrayList<>();
                result.addAll(loadK8sWorkloadsByKind(client, apiUrl, token, resolvedNamespace, "Deployment"));
                result.addAll(loadK8sWorkloadsByKind(client, apiUrl, token, resolvedNamespace, "StatefulSet"));
                if (!result.isEmpty()) {
                    return result;
                }
                return loadK8sWorkloadsFromPods(client, apiUrl, token, resolvedNamespace);
            } catch (Exception e) {
                log.warn("load k8s workloads through api failed", e);
                try {
                    HttpClient client = buildK8sHttpClient(apiUrl);
                    List<AdminK8sWorkloadVO> podDerivedWorkloads = loadK8sWorkloadsFromPods(client, apiUrl, token, resolvedNamespace);
                    if (!podDerivedWorkloads.isEmpty()) {
                        return podDerivedWorkloads;
                    }
                } catch (Exception podException) {
                    log.warn("derive k8s workloads from pods failed", podException);
                }
            }
        }
        try {
            return parseK8sWorkloadLines(resolvedNamespace, runRemoteCommand("bash -lc " +
                    quoteForBash("kubectl get deploy,sts -n " + resolvedNamespace + " --no-headers 2>/dev/null || true")));
        } catch (Exception e) {
            log.warn("load k8s workloads through remote kubectl failed", e);
            return Collections.emptyList();
        }
    }

    @Override
    public AdminK8sActionResultVO runK8sWorkloadAction(AdminK8sWorkloadActionDTO dto) {
        requireTenantOneOpsAccess();
        if (dto == null) {
            throw new BadRequestException("K8s 操作参数不能为空");
        }
        String namespace = StringUtils.hasText(dto.getNamespace()) ? sanitizeCleanupToken(dto.getNamespace(), "命名空间") : k8sCleanupNamespace;
        String kind = normalizeK8sWorkloadKind(dto.getKind());
        String name = sanitizeCleanupToken(dto.getName(), "工作负载名称");
        String action = StringUtils.hasText(dto.getAction()) ? dto.getAction().trim().toUpperCase() : "";
        if (!SUPPORTED_K8S_WORKLOAD_ACTIONS.contains(action)) {
            throw new BadRequestException("不支持的 K8s 操作: " + action);
        }
        Integer replicas = dto.getReplicas();
        if ("SCALE".equals(action) && (replicas == null || replicas < 0 || replicas > 20)) {
            throw new BadRequestException("副本数必须在 0 到 20 之间");
        }

        requireDangerConfirmation(action, replicas, name, dto.getConfirmText());

        AdminK8sActionResultVO result = new AdminK8sActionResultVO();
        result.setNamespace(namespace);
        result.setKind(kind);
        result.setName(name);
        result.setAction(action);
        result.setReplicas(replicas);
        result.setReason(emptyToNull(dto.getReason()));
        result.setExecutedAt(LocalDateTime.now().format(TIME_FORMATTER));
        try {
            String output = runInClusterK8sWorkloadAction(namespace, kind, name, action, replicas);
            if (output == null) {
                output = runRemoteK8sWorkloadAction(namespace, kind, name, action, replicas);
            }
            result.setSuccess(Boolean.TRUE);
            result.setMessage("K8s 操作已提交");
            result.setOutput(output);
        } catch (Exception e) {
            log.warn("run k8s workload action failed, namespace={}, kind={}, name={}, action={}", namespace, kind, name, action, e);
            result.setSuccess(Boolean.FALSE);
            result.setMessage("K8s 操作失败: " + e.getMessage());
            result.setOutput(e.getMessage());
        }
        result.setAuditId(insertOperationAudit(
                "K8S_WORKLOAD",
                namespace,
                kind,
                name,
                action,
                Boolean.TRUE.equals(result.getSuccess()) ? "success" : "failed",
                result.getReason(),
                toJson(Map.of(
                        "namespace", namespace,
                        "kind", kind,
                        "name", name,
                        "action", action,
                        "replicas", replicas == null ? "" : replicas
                )),
                result.getOutput()
        ));
        return result;
    }

    @Override
    public AdminK8sPodExecResultVO runK8sPodExec(AdminK8sPodExecRequestDTO dto) {
        requireTenantOneOpsAccess();
        if (dto == null) {
            throw new BadRequestException("Pod exec parameters are required");
        }
        String namespace = StringUtils.hasText(dto.getNamespace()) ? sanitizeCleanupToken(dto.getNamespace(), "namespace") : k8sCleanupNamespace;
        String podName = sanitizeCleanupToken(dto.getPodName(), "podName");
        String containerName = sanitizeCleanupToken(dto.getContainerName(), "containerName");
        String command = normalizePodExecCommand(dto.getCommand());
        int timeoutSeconds = Math.max(3, Math.min(dto.getTimeoutSeconds() == null ? 20 : dto.getTimeoutSeconds(), 120));

        AdminK8sPodExecResultVO result = new AdminK8sPodExecResultVO();
        result.setNamespace(namespace);
        result.setPodName(podName);
        result.setContainerName(containerName);
        result.setCommand(command);
        result.setExecutedAt(LocalDateTime.now().format(TIME_FORMATTER));
        long start = System.nanoTime();
        try {
            String output = sendK8sExecRequest(namespace, podName, containerName, command, timeoutSeconds);
            result.setSuccess(Boolean.TRUE);
            result.setExitCode(0);
            result.setOutput(output);
            result.setMessage("Pod command executed");
        } catch (Exception e) {
            log.warn("run pod exec failed, namespace={}, pod={}, container={}", namespace, podName, containerName, e);
            result.setSuccess(Boolean.FALSE);
            result.setExitCode(-1);
            result.setOutput(e.getMessage());
            result.setMessage("Pod command failed: " + e.getMessage());
        }
        result.setDurationMillis(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
        result.setAuditId(insertOperationAudit(
                "K8S_POD_EXEC",
                namespace,
                "Pod",
                podName + "/" + containerName,
                "EXEC",
                Boolean.TRUE.equals(result.getSuccess()) ? "success" : "failed",
                emptyToNull(dto.getReason()),
                toJson(Map.of(
                        "namespace", namespace,
                        "podName", podName,
                        "containerName", containerName,
                        "command", command,
                        "timeoutSeconds", timeoutSeconds
                )),
                result.getOutput()
        ));
        return result;
    }

    @Override
    public AdminTopologyVO getServiceTopology(String namespace) {
        requireTenantOneOpsAccess();
        String resolvedNamespace = StringUtils.hasText(namespace) ? sanitizeCleanupToken(namespace, "命名空间") : k8sCleanupNamespace;
        Map<String, Map<String, Object>> serviceStatus = buildServiceStatus().stream()
                .collect(Collectors.toMap(
                        item -> normalizeNacosServiceName(valueAsString(item.get("name"))),
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        List<Map<String, Object>> nodes = new ArrayList<>();
        nodes.add(topologyNode("gateway", "API 网关", "gateway", serviceStatus.get("rk-gateway")));
        for (AdminOpsServiceVO service : listServiceRegistry()) {
            if (!Boolean.TRUE.equals(service.getEnabled()) || "rk-gateway".equals(service.getServiceCode())) {
                continue;
            }
            nodes.add(topologyNode(service.getServiceCode(), service.getDisplayName(), service.getServiceType(), serviceStatus.get(service.getServiceCode())));
        }
        nodes.add(topologyNode("mysql", "MySQL", "middleware", Map.of("status", "online")));
        nodes.add(topologyNode("redis", "Redis", "middleware", Map.of("status", "online")));
        nodes.add(topologyNode("nacos", "Nacos", "middleware", Map.of("status", "online")));
        nodes.add(topologyNode("minio", "MinIO", "middleware", Map.of("status", "unknown")));

        List<Map<String, Object>> edges = new ArrayList<>();
        for (AdminOpsServiceVO service : listServiceRegistry()) {
            if (Boolean.TRUE.equals(service.getEnabled()) && !"rk-gateway".equals(service.getServiceCode())) {
                edges.add(topologyEdge("gateway", service.getServiceCode(), "route"));
            }
        }
        for (AdminOpsServiceVO service : listServiceRegistry()) {
            if (Boolean.TRUE.equals(service.getEnabled())) {
                edges.add(topologyEdge(service.getServiceCode(), "nacos", "register"));
                edges.add(topologyEdge(service.getServiceCode(), "mysql", "data"));
            }
        }
        AdminTopologyVO result = new AdminTopologyVO();
        result.setNamespace(resolvedNamespace);
        result.setNodes(nodes);
        result.setEdges(edges);
        return result;
    }

    @Override
    public Map<String, Object> getFrontendCacheOverview() {
        requireTenantOneOpsAccess();
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> diagnostics = new ArrayList<>();
        Set<String> keys = loadFrontendCacheKeys(diagnostics);
        long totalBytes = 0L;
        long nullMarkerCount = 0L;
        long warmupKeyCount = 0L;
        List<Map<String, Object>> samples = new ArrayList<>();
        for (String key : keys) {
            String value = null;
            Long ttl = null;
            try {
                value = stringRedisTemplate.opsForValue().get(key);
                ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
            } catch (Exception e) {
                diagnostics.add("Redis 读取缓存键失败: " + key + "，" + safeDiagnosticMessage(e));
            }
            long bytes = value == null ? 0L : value.getBytes(StandardCharsets.UTF_8).length;
            totalBytes += bytes;
            if ("__RK_NULL__".equals(value)) {
                nullMarkerCount++;
            }
            if (key.contains(":warmup:")) {
                warmupKeyCount++;
            }
            if (samples.size() < 20) {
                Map<String, Object> sample = new LinkedHashMap<>();
                sample.put("key", key);
                sample.put("ttlSeconds", ttl == null ? 0L : ttl);
                sample.put("bytes", bytes);
                sample.put("type", "__RK_NULL__".equals(value) ? "空值缓存" : (key.contains(":warmup:") ? "预热记录" : "响应缓存"));
                samples.add(sample);
            }
        }
        diagnostics.add("XXL-Job 每小时预热任务会在执行预热时自动同步，概览接口不写调度中心");
        result.put("keyPattern", FRONTEND_CACHE_KEY_PATTERN);
        result.put("pattern", FRONTEND_CACHE_KEY_PATTERN);
        result.put("keyCount", keys.size());
        result.put("totalKeys", keys.size());
        result.put("totalBytes", totalBytes);
        result.put("totalSizeText", formatBytes(totalBytes));
        result.put("sampleKeys", samples);
        result.put("groups", buildFrontendCacheGroups(keys.size(), totalBytes, nullMarkerCount, warmupKeyCount));
        result.put("warmupPaths", buildFrontendWarmupPathRows(Collections.emptyList()));
        result.put("paths", publicFrontendWarmupPaths);
        result.put("normalKeyCount", Math.max(0L, keys.size() - nullMarkerCount - warmupKeyCount));
        result.put("nullMarkerCount", nullMarkerCount);
        result.put("nullHitCount", nullMarkerCount);
        result.put("warmupKeyCount", warmupKeyCount);
        result.put("ttlJitterSeconds", 300);
        result.put("nullTtlSeconds", 90);
        result.put("antiPenetrationEnabled", true);
        result.put("randomTtlEnabled", true);
        result.put("randomExpireEnabled", true);
        result.put("nullValueCacheEnabled", true);
        result.put("randomExpireCount", Math.max(0L, keys.size() - warmupKeyCount));
        result.put("penetrationBlockedCount", nullMarkerCount);
        result.put("lastUpdatedAt", LocalDateTime.now().format(TIME_FORMATTER));
        result.put("diagnostics", diagnostics);
        result.put("checkedAt", LocalDateTime.now().format(TIME_FORMATTER));
        return result;
    }

    @Override
    public Map<String, Object> warmupFrontendCache() {
        requireTenantOneOpsAccess();
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> diagnostics = new ArrayList<>();
        List<Map<String, Object>> warmed = new ArrayList<>();
        String baseUrl = PublicBaseUrlResolver.resolve(deployPackagePublicBaseUrl);
        if (!StringUtils.hasText(baseUrl)) {
            baseUrl = sanitizeOpsUrl(gatewayUrl);
        }
        if (!StringUtils.hasText(baseUrl)) {
            diagnostics.add("公共前台 URL 未配置，无法预热");
        } else {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            for (String path : publicFrontendWarmupPaths) {
                warmed.add(warmupFrontendCacheUrl(client, baseUrl, path, diagnostics));
            }
        }
        appendFrontendCacheSchedulerSync(diagnostics);
        long totalCostMs = warmed.stream().mapToLong(item -> safeLong(readLong(item, "durationMillis", "costMs"))).sum();
        result.put("baseUrl", baseUrl);
        result.put("paths", publicFrontendWarmupPaths);
        result.put("warmupPaths", buildFrontendWarmupPathRows(warmed));
        result.put("warmed", warmed);
        result.put("successCount", warmed.stream().filter(item -> Boolean.TRUE.equals(item.get("success"))).count());
        result.put("failedCount", warmed.stream().filter(item -> !Boolean.TRUE.equals(item.get("success"))).count());
        result.put("durationMs", totalCostMs);
        result.put("costMs", totalCostMs);
        result.put("summary", "成功 " + result.get("successCount") + " 个，失败 " + result.get("failedCount") + " 个");
        result.put("cacheSnapshot", getFrontendCacheOverview());
        result.put("diagnostics", diagnostics);
        result.put("warmedAt", LocalDateTime.now().format(TIME_FORMATTER));
        result.put("finishedAt", LocalDateTime.now().format(TIME_FORMATTER));
        return result;
    }

    @Override
    public Map<String, Object> clearFrontendCache() {
        requireTenantOneOpsAccess();
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> diagnostics = new ArrayList<>();
        Set<String> keys = loadFrontendCacheKeys(diagnostics);
        long deleted = 0L;
        deleted = deleteFrontendCacheKeys(keys, diagnostics);
        result.put("keyPattern", FRONTEND_CACHE_KEY_PATTERN);
        result.put("matchedCount", keys.size());
        result.put("deletedCount", deleted);
        result.put("diagnostics", diagnostics);
        result.put("clearedAt", LocalDateTime.now().format(TIME_FORMATTER));
        return result;
    }

    @Override
    public Map<String, Object> getVisualScreenOverview() {
        requireTenantOneOpsAccess();
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> diagnostics = new ArrayList<>();
        List<Map<String, Object>> tenantCards = buildVisualTenantCards(diagnostics);
        Map<String, Object> onlineUsers = buildVisualOnlineUsers(diagnostics);
        Map<String, Object> registrationStats = buildVisualRegistrationStats(diagnostics);
        Map<String, Object> contentOverview = buildVisualContentOverview(diagnostics);
        Map<String, Object> cacheSnapshot = getFrontendCacheOverview();
        result.put("tenantCards", tenantCards);
        result.put("tenants", tenantCards);
        result.put("onlineUsers", onlineUsers);
        result.put("registrationStats", registrationStats);
        result.put("admissions", registrationStats);
        result.put("contentOverview", contentOverview);
        result.put("content", contentOverview);
        result.put("cacheSnapshot", cacheSnapshot);
        result.put("cache", cacheSnapshot);
        result.put("metrics", buildVisualScreenMetrics(tenantCards, onlineUsers, registrationStats, contentOverview, cacheSnapshot));
        result.put("diagnostics", diagnostics);
        result.put("generatedAt", LocalDateTime.now().format(TIME_FORMATTER));
        result.put("updatedAt", LocalDateTime.now().format(TIME_FORMATTER));
        return result;
    }

    @Override
    public AdminTrafficOverviewVO getTrafficOverview(Long tenantId, String startTime, String endTime) {
        requireTenantOneOpsAccess();
        List<AdminTrafficTenantOptionVO> tenants = loadTrafficTenantOptions();
        Long defaultTenantId = resolveTrafficDefaultTenantId(tenants);
        Long selectedTenantId = resolveSelectedTrafficTenantId(tenantId, defaultTenantId, tenants);
        AdminTrafficOverviewVO result = buildTrafficOverview(selectedTenantId, defaultTenantId, tenants, startTime, endTime);
        if (!result.getCountryRankings().isEmpty()) {
            result.setProvinceRankings(buildTrafficProvinceRankings(result.getRecentIps(), result.getCountryRankings().get(0).getCountry()));
        }
        return result;
    }

    @Override
    public AdminTrafficOverviewVO getTrafficDrilldown(Long tenantId, String country, String province, String startTime, String endTime) {
        AdminTrafficOverviewVO result = getTrafficOverview(tenantId, startTime, endTime);
        String resolvedCountry = StringUtils.hasText(country) ? country.trim() : null;
        String resolvedProvince = StringUtils.hasText(province) ? province.trim() : null;
        if (StringUtils.hasText(resolvedCountry)) {
            result.setProvinceRankings(buildTrafficProvinceRankings(result.getRecentIps(), resolvedCountry));
        }
        if (StringUtils.hasText(resolvedCountry) && StringUtils.hasText(resolvedProvince)) {
            result.setRecentIps(result.getRecentIps().stream()
                    .filter(row -> Objects.equals(resolvedCountry, row.getCountry()))
                    .filter(row -> Objects.equals(resolvedProvince, row.getProvince()))
                    .sorted(Comparator.comparing(AdminTrafficLocationVO::getVisitCount, Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(80)
                    .collect(Collectors.toList()));
        }
        return result;
    }

    @Override
    public List<AdminTrafficOnlineUserVO> getTrafficOnlineUsers(Long tenantId, Integer windowMinutes) {
        requireTenantOneOpsAccess();
        List<AdminTrafficTenantOptionVO> tenants = loadTrafficTenantOptions();
        Long defaultTenantId = resolveTrafficDefaultTenantId(tenants);
        Long selectedTenantId = resolveSelectedTrafficTenantId(tenantId, defaultTenantId, tenants);
        return buildTrafficOnlineUsers(selectedTenantId, windowMinutes);
    }

    @Override
    public AdminTrafficLocationVO getTrafficCurrentOrigin() {
        requireTenantOneOpsAccess();
        return buildCurrentTrafficOrigin();
    }

    @Override
    public AdminTrafficOverviewVO getTrafficDefaultTenant() {
        requireTenantOneOpsAccess();
        List<AdminTrafficTenantOptionVO> tenants = loadTrafficTenantOptions();
        Long defaultTenantId = resolveTrafficDefaultTenantId(tenants);
        AdminTrafficOverviewVO result = new AdminTrafficOverviewVO();
        result.setDefaultTenantId(defaultTenantId);
        result.setSelectedTenantId(defaultTenantId);
        result.setTenantOptions(tenants);
        result.setCurrentOrigin(buildCurrentTrafficOrigin());
        return result;
    }

    @Override
    public AdminTrafficOverviewVO saveTrafficDefaultTenant(AdminTrafficDefaultTenantDTO dto) {
        requireTenantOneOpsAccess();
        Long tenantId = dto == null ? null : dto.getTenantId();
        if (tenantId == null || tenantId <= 0) {
            throw new BadRequestException("租户不存在或已禁用");
        }
        Integer tenantCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM rk_tenant WHERE id = ? AND is_deleted = 0 AND status = 1",
                Integer.class,
                tenantId
        );
        if (tenantCount == null || tenantCount <= 0) {
            throw new BadRequestException("租户不存在或已禁用");
        }
        Integer configCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM system_config WHERE tenant_id = 1 AND config_key = ? AND is_deleted = 0",
                Integer.class,
                TRAFFIC_DEFAULT_TENANT_KEY
        );
        if (configCount != null && configCount > 0) {
            jdbcTemplate.update(
                    "UPDATE system_config SET config_value = ?, description = ?, is_enabled = 1, update_time = NOW() WHERE tenant_id = 1 AND config_key = ? AND is_deleted = 0",
                    String.valueOf(tenantId),
                    "全球流量中心默认展示租户",
                    TRAFFIC_DEFAULT_TENANT_KEY
            );
        } else {
            jdbcTemplate.update(
                    "INSERT INTO system_config (config_key, config_value, description, tenant_id, is_enabled, is_deleted, create_time, update_time) VALUES (?, ?, ?, 1, 1, 0, NOW(), NOW())",
                    TRAFFIC_DEFAULT_TENANT_KEY,
                    String.valueOf(tenantId),
                    "全球流量中心默认展示租户"
            );
        }
        return getTrafficDefaultTenant();
    }

    private AdminTrafficOverviewVO buildTrafficOverview(Long selectedTenantId, Long defaultTenantId,
                                                        List<AdminTrafficTenantOptionVO> tenants,
                                                        String startTime, String endTime) {
        Map<String, TrafficBucket> ipBuckets = new LinkedHashMap<>();
        Map<String, TrafficBucket> countryBuckets = new LinkedHashMap<>();
        appendTrafficRows(ipBuckets, countryBuckets, queryTrafficLoginRows(selectedTenantId, startTime, endTime), true);
        appendTrafficRows(ipBuckets, countryBuckets, queryTrafficOperationRows(selectedTenantId, startTime, endTime), false);

        List<AdminTrafficLocationVO> allIps = ipBuckets.values().stream()
                .map(TrafficBucket::toVO)
                .sorted(Comparator.comparing(AdminTrafficLocationVO::getVisitCount, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AdminTrafficLocationVO::getLastSeenAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
        List<AdminTrafficLocationVO> recentIps = allIps.stream()
                .filter(this::isVisibleTrafficIp)
                .limit(100)
                .collect(Collectors.toList());
        List<AdminTrafficLocationVO> countries = countryBuckets.values().stream()
                .map(TrafficBucket::toVO)
                .sorted(Comparator.comparing(AdminTrafficLocationVO::getVisitCount, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(30)
                .collect(Collectors.toList());

        AdminTrafficOverviewVO result = new AdminTrafficOverviewVO();
        result.setDefaultTenantId(defaultTenantId);
        result.setSelectedTenantId(selectedTenantId);
        result.setTenantOptions(tenants);
        result.setCurrentOrigin(buildCurrentTrafficOrigin());
        result.setRecentIps(recentIps);
        result.setCountryRankings(countries);
        result.setTotalVisits(allIps.stream().mapToLong(row -> safeLong(row.getVisitCount())).sum());
        result.setTotalLogins(allIps.stream().mapToLong(row -> safeLong(row.getLoginCount())).sum());
        result.setTotalOperations(allIps.stream().mapToLong(row -> safeLong(row.getOperationCount())).sum());
        Set<String> countryNames = allIps.stream()
                .map(AdminTrafficLocationVO::getCountry)
                .filter(StringUtils::hasText)
                .filter(country -> !"内网".equals(country) && !"未知国家".equals(country))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        result.setCountryCount((long) countryNames.size());
        result.setAbnormalIpCount(allIps.stream()
                .filter(this::isAbnormalTrafficIp)
                .count());
        List<AdminTrafficOnlineUserVO> onlineUsers = buildTrafficOnlineUsers(selectedTenantId, 10);
        applyTrafficOnlineSummary(result, onlineUsers);
        return result;
    }

    private void applyTrafficOnlineSummary(AdminTrafficOverviewVO result, List<AdminTrafficOnlineUserVO> onlineUsers) {
        List<AdminTrafficOnlineUserVO> rows = onlineUsers == null ? List.of() : onlineUsers;
        result.setOnlineUsers(rows);
        result.setOnlineUserCount((long) rows.size());
        result.setOnlinePcCount(rows.stream()
                .filter(row -> "pc".equals(normalizeTrafficDeviceType(row.getDeviceType())))
                .count());
        result.setOnlineMobileCount(rows.stream()
                .filter(row -> "mobile".equals(normalizeTrafficDeviceType(row.getDeviceType())))
                .count());
    }

    private List<AdminTrafficTenantOptionVO> loadTrafficTenantOptions() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, tenant_name AS tenantName, status FROM rk_tenant WHERE is_deleted = 0 ORDER BY id ASC"
        );
        List<AdminTrafficTenantOptionVO> tenants = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            AdminTrafficTenantOptionVO option = new AdminTrafficTenantOptionVO();
            Long tenantId = readLong(row, "id", "tenantId");
            option.setTenantId(tenantId);
            option.setTenantName(readString(row, "tenantName", "tenant_name", "name"));
            option.setStatus((int) safeLong(readLong(row, "status")));
            tenants.add(option);
        }
        if (tenants.isEmpty()) {
            AdminTrafficTenantOptionVO fallback = new AdminTrafficTenantOptionVO();
            fallback.setTenantId(1L);
            fallback.setTenantName("tenant 1");
            fallback.setStatus(1);
            tenants.add(fallback);
        }
        return tenants;
    }

    private Long resolveTrafficDefaultTenantId(List<AdminTrafficTenantOptionVO> tenants) {
        String value = null;
        try {
            value = jdbcTemplate.queryForObject(
                    "SELECT config_value FROM system_config WHERE tenant_id = 1 AND config_key = '" + TRAFFIC_DEFAULT_TENANT_KEY + "' AND is_deleted = 0 AND is_enabled = 1 ORDER BY id DESC LIMIT 1",
                    String.class
            );
        } catch (Exception ignored) {
        }
        Long parsed = parseLong(value);
        if (isTrafficTenantAvailable(parsed, tenants)) {
            return parsed;
        }
        return isTrafficTenantAvailable(1L, tenants) ? 1L : tenants.get(0).getTenantId();
    }

    private Long resolveSelectedTrafficTenantId(Long tenantId, Long defaultTenantId, List<AdminTrafficTenantOptionVO> tenants) {
        if (tenantId != null && tenantId == 0L) {
            return 0L;
        }
        if (isTrafficTenantAvailable(tenantId, tenants)) {
            return tenantId;
        }
        return defaultTenantId;
    }

    private boolean isTrafficTenantAvailable(Long tenantId, List<AdminTrafficTenantOptionVO> tenants) {
        if (tenantId == null) {
            return false;
        }
        return tenants.stream().anyMatch(item -> Objects.equals(item.getTenantId(), tenantId));
    }

    private List<Map<String, Object>> queryTrafficLoginRows(Long selectedTenantId, String startTime, String endTime) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COALESCE(u.tenant_id, 1) AS tenantId, COALESCE(t.tenant_name, CONCAT('tenant ', COALESCE(u.tenant_id, 1))) AS tenantName, ")
                .append("l.login_name AS userName, l.ipaddr AS ip, l.login_location AS rawLocation, ")
                .append("MAX(l.login_time) AS lastSeenAt, COUNT(*) AS countValue ")
                .append("FROM sys_logininfor l ")
                .append("LEFT JOIN rk_user u ON u.username COLLATE utf8mb4_unicode_ci = l.login_name COLLATE utf8mb4_unicode_ci AND u.is_deleted = 0 ")
                .append("LEFT JOIN rk_tenant t ON t.id = COALESCE(u.tenant_id, 1) ")
                .append("WHERE l.ipaddr IS NOT NULL AND l.ipaddr <> '' ");
        appendTrafficWhere(sql, selectedTenantId, "COALESCE(u.tenant_id, 1)", "l.login_time", startTime, endTime);
        sql.append(" GROUP BY COALESCE(u.tenant_id, 1), COALESCE(t.tenant_name, CONCAT('tenant ', COALESCE(u.tenant_id, 1))), l.login_name, l.ipaddr, l.login_location ")
                .append("ORDER BY lastSeenAt DESC LIMIT 500");
        return queryTrafficRows(sql.toString());
    }

    private List<Map<String, Object>> queryTrafficOperationRows(Long selectedTenantId, String startTime, String endTime) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COALESCE(u.tenant_id, 1) AS tenantId, COALESCE(t.tenant_name, CONCAT('tenant ', COALESCE(u.tenant_id, 1))) AS tenantName, ")
                .append("o.oper_name AS userName, o.oper_ip AS ip, o.oper_location AS rawLocation, ")
                .append("MAX(o.oper_url) AS samplePath, MAX(o.title) AS sampleAction, MAX(o.oper_time) AS lastSeenAt, COUNT(*) AS countValue ")
                .append("FROM sys_oper_log o ")
                .append("LEFT JOIN rk_user u ON u.username COLLATE utf8mb4_unicode_ci = o.oper_name COLLATE utf8mb4_unicode_ci AND u.is_deleted = 0 ")
                .append("LEFT JOIN rk_tenant t ON t.id = COALESCE(u.tenant_id, 1) ")
                .append("WHERE o.oper_ip IS NOT NULL AND o.oper_ip <> '' ");
        appendTrafficWhere(sql, selectedTenantId, "COALESCE(u.tenant_id, 1)", "o.oper_time", startTime, endTime);
        sql.append(" GROUP BY COALESCE(u.tenant_id, 1), COALESCE(t.tenant_name, CONCAT('tenant ', COALESCE(u.tenant_id, 1))), o.oper_name, o.oper_ip, o.oper_location ")
                .append("ORDER BY lastSeenAt DESC LIMIT 500");
        return queryTrafficRows(sql.toString());
    }

    private List<AdminTrafficOnlineUserVO> buildTrafficOnlineUsers(Long selectedTenantId, Integer windowMinutes) {
        List<Map<String, Object>> rows = queryTrafficOnlineRows(selectedTenantId, windowMinutes);
        return rows.stream()
                .map(this::toTrafficOnlineUser)
                .filter(row -> StringUtils.hasText(row.getUserName()) || StringUtils.hasText(row.getIp()))
                .sorted(Comparator.comparing(AdminTrafficOnlineUserVO::getLastSeenAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(100)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> queryTrafficOnlineRows(Long selectedTenantId, Integer windowMinutes) {
        int minutes = normalizeOnlineWindowMinutes(windowMinutes);
        StringBuilder sql = new StringBuilder();
        sql.append("/* traffic_online */ ")
                .append("SELECT tenantId, tenantName, userName, ip, rawLocation, deviceType, clientType, ")
                .append("MAX(lastSeenAt) AS lastSeenAt, SUM(activityCount) AS activityCount, ")
                .append("MAX(samplePath) AS samplePath, MAX(sampleAction) AS sampleAction ")
                .append("FROM (")
                .append("SELECT COALESCE(u.tenant_id, 1) AS tenantId, COALESCE(t.tenant_name, CONCAT('tenant ', COALESCE(u.tenant_id, 1))) AS tenantName, ")
                .append("l.login_name AS userName, l.ipaddr AS ip, l.login_location AS rawLocation, ")
                .append("CASE WHEN LOWER(CONCAT_WS(' ', l.browser, l.os)) REGEXP 'android|iphone|ipad|ios|mobile|harmony' THEN 'mobile' ")
                .append("WHEN TRIM(CONCAT_WS(' ', l.browser, l.os)) = '' THEN 'unknown' ELSE 'pc' END AS deviceType, ")
                .append("NULLIF(CONCAT_WS(' / ', NULLIF(l.browser, ''), NULLIF(l.os, '')), '') AS clientType, ")
                .append("l.login_time AS lastSeenAt, 1 AS activityCount, '/auth/login' AS samplePath, 'login' AS sampleAction ")
                .append("FROM sys_logininfor l ")
                .append("LEFT JOIN rk_user u ON u.username COLLATE utf8mb4_unicode_ci = l.login_name COLLATE utf8mb4_unicode_ci AND u.is_deleted = 0 ")
                .append("LEFT JOIN rk_tenant t ON t.id = COALESCE(u.tenant_id, 1) ")
                .append("WHERE l.ipaddr IS NOT NULL AND l.ipaddr <> '' AND l.login_time >= DATE_SUB(NOW(), INTERVAL ").append(minutes).append(" MINUTE) ")
                .append("UNION ALL ")
                .append("SELECT COALESCE(u.tenant_id, 1) AS tenantId, COALESCE(t.tenant_name, CONCAT('tenant ', COALESCE(u.tenant_id, 1))) AS tenantName, ")
                .append("o.oper_name AS userName, o.oper_ip AS ip, o.oper_location AS rawLocation, ")
                .append("CASE WHEN o.operator_type = 2 OR o.oper_url LIKE '/api/mobile/%' OR o.oper_url LIKE '/mobile/%' THEN 'mobile' ")
                .append("WHEN o.operator_type = 1 OR o.oper_url LIKE '/admin/%' THEN 'pc' ELSE 'unknown' END AS deviceType, ")
                .append("CASE WHEN o.operator_type = 2 OR o.oper_url LIKE '/api/mobile/%' OR o.oper_url LIKE '/mobile/%' THEN 'Mobile App' ")
                .append("WHEN o.operator_type = 1 OR o.oper_url LIKE '/admin/%' THEN 'Web Admin' ELSE 'Unknown' END AS clientType, ")
                .append("o.oper_time AS lastSeenAt, 1 AS activityCount, o.oper_url AS samplePath, o.title AS sampleAction ")
                .append("FROM sys_oper_log o ")
                .append("LEFT JOIN rk_user u ON u.username COLLATE utf8mb4_unicode_ci = o.oper_name COLLATE utf8mb4_unicode_ci AND u.is_deleted = 0 ")
                .append("LEFT JOIN rk_tenant t ON t.id = COALESCE(u.tenant_id, 1) ")
                .append("WHERE o.oper_ip IS NOT NULL AND o.oper_ip <> '' AND o.oper_time >= DATE_SUB(NOW(), INTERVAL ").append(minutes).append(" MINUTE) ")
                .append(") onlineRows WHERE 1 = 1 ");
        List<Object> args = new ArrayList<>();
        if (selectedTenantId != null && selectedTenantId > 0) {
            sql.append("AND tenantId = ? ");
            args.add(selectedTenantId);
        }
        sql.append("GROUP BY tenantId, tenantName, userName, ip, rawLocation, deviceType, clientType ")
                .append("ORDER BY lastSeenAt DESC LIMIT 100");
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), args.toArray(new Object[0]));
            return rows == null ? List.of() : rows;
        } catch (Exception e) {
            log.warn("query traffic online users failed", e);
            return List.of();
        }
    }

    private AdminTrafficOnlineUserVO toTrafficOnlineUser(Map<String, Object> row) {
        String ip = readString(row, "ip", "ipaddr", "oper_ip");
        String rawLocation = readString(row, "rawLocation", "raw_location", "login_location", "oper_location");
        TrafficResolvedLocation location = resolveTrafficLocation(ip, rawLocation);
        AdminTrafficOnlineUserVO vo = new AdminTrafficOnlineUserVO();
        vo.setTenantId(readLong(row, "tenantId", "tenant_id"));
        if (vo.getTenantId() == null) {
            vo.setTenantId(1L);
        }
        vo.setTenantName(firstNonBlank(readString(row, "tenantName", "tenant_name"), "tenant " + vo.getTenantId()));
        vo.setUserName(readString(row, "userName", "user_name"));
        vo.setUserDisplayName(vo.getUserName());
        vo.setIp(ip);
        vo.setRawLocation(rawLocation);
        vo.setDeviceType(normalizeTrafficDeviceType(readString(row, "deviceType", "device_type")));
        vo.setClientType(firstNonBlank(readString(row, "clientType", "client_type"), "Unknown"));
        vo.setLastSeenAt(readString(row, "lastSeenAt", "last_seen_at"));
        vo.setActivityCount(safeLong(readLong(row, "activityCount", "activity_count", "countValue", "count")));
        vo.setSamplePath(readString(row, "samplePath", "sample_path"));
        vo.setSampleAction(readString(row, "sampleAction", "sample_action"));
        vo.setCountry(location.country);
        vo.setProvince(location.province);
        vo.setCity(location.city);
        vo.setLat(location.lat);
        vo.setLng(location.lng);
        return vo;
    }

    private int normalizeOnlineWindowMinutes(Integer windowMinutes) {
        if (windowMinutes == null || windowMinutes < 1) {
            return 10;
        }
        return Math.min(windowMinutes, 120);
    }

    private String normalizeTrafficDeviceType(String value) {
        if (!StringUtils.hasText(value)) {
            return "unknown";
        }
        String lower = value.trim().toLowerCase();
        if (lower.contains("mobile") || lower.contains("android") || lower.contains("ios")
                || lower.contains("iphone") || lower.contains("ipad") || lower.contains("harmony")) {
            return "mobile";
        }
        if (lower.contains("pc") || lower.contains("web") || lower.contains("desktop")
                || lower.contains("windows") || lower.contains("mac") || lower.contains("linux")) {
            return "pc";
        }
        return "unknown";
    }

    private AdminTrafficLocationVO buildCurrentTrafficOrigin() {
        String clientIp = readCurrentTrafficClientIp();
        String recentRawLocation = readRecentTrafficRawLocation(clientIp);
        TrafficResolvedLocation location = resolveTrafficLocation(clientIp, recentRawLocation);
        String displayIp = clientIp;
        if (isUnknownTrafficLocation(location) || isPrivateTrafficIp(clientIp) || "内网".equals(location.country)) {
            TrafficResolvedLocation egress = trafficGeoCache.computeIfAbsent("__current_egress", ignored -> readIpWhoLocation(""));
            if (!isUnknownTrafficLocation(egress)) {
                location = egress;
                displayIp = firstNonBlank(egress.ip, clientIp);
            }
        }
        if (isUnknownTrafficLocation(location)) {
            location = new TrafficResolvedLocation("当前来源", "未知省份", "", 20.0, 0.0);
        }

        AdminTrafficLocationVO vo = new AdminTrafficLocationVO();
        vo.setTenantId(0L);
        vo.setTenantName("当前访问来源");
        vo.setCountry(firstNonBlank(location.country, "当前来源"));
        vo.setProvince(firstNonBlank(location.province, "未知省份"));
        vo.setCity(firstNonBlank(location.city, vo.getProvince()));
        vo.setIp(firstNonBlank(displayIp, location.ip, "unknown"));
        vo.setUserName("current-origin");
        vo.setRawLocation("当前访问来源");
        vo.setSamplePath("/admin/operation/traffic-center");
        vo.setSampleAction("current-origin");
        vo.setVisitCount(0L);
        vo.setLoginCount(0L);
        vo.setOperationCount(0L);
        vo.setLastSeenAt(LocalDateTime.now().format(TIME_FORMATTER));
        vo.setLat(location.lat);
        vo.setLng(location.lng);
        return vo;
    }

    private String readRecentTrafficRawLocation(String ip) {
        if (!StringUtils.hasText(ip) || isPrivateTrafficIp(ip)) {
            return "";
        }
        String sql = "SELECT rawLocation FROM ("
                + "SELECT login_location AS rawLocation, login_time AS seenAt FROM sys_logininfor "
                + "WHERE ipaddr = ? AND login_location IS NOT NULL AND login_location <> '' "
                + "UNION ALL "
                + "SELECT oper_location AS rawLocation, oper_time AS seenAt FROM sys_oper_log "
                + "WHERE oper_ip = ? AND oper_location IS NOT NULL AND oper_location <> '' "
                + ") locations "
                + "WHERE rawLocation NOT IN ('内网', '内网IP', 'unknown', '未知') "
                + "ORDER BY seenAt DESC LIMIT 1";
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, ip.trim(), ip.trim());
            if (rows == null || rows.isEmpty()) {
                return "";
            }
            return readString(rows.get(0), "rawLocation", "raw_location");
        } catch (Exception e) {
            log.debug("read recent traffic raw location failed, ip={}", ip, e);
            return "";
        }
    }

    private boolean isUnknownTrafficLocation(TrafficResolvedLocation location) {
        return location == null
                || !StringUtils.hasText(location.country)
                || "当前来源".equals(location.country)
                || location.country.contains("未知")
                || location.province == null
                || location.province.contains("未知");
    }

    private String readCurrentTrafficClientIp() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes)) {
            return "";
        }
        HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
        if (request == null) {
            return "";
        }
        String[] headers = new String[]{
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };
        for (String header : headers) {
            String candidate = firstForwardedIp(request.getHeader(header));
            if (StringUtils.hasText(candidate)) {
                return candidate;
            }
        }
        return firstNonBlank(request.getRemoteAddr(), "");
    }

    private String firstForwardedIp(String value) {
        if (!StringUtils.hasText(value) || "unknown".equalsIgnoreCase(value.trim())) {
            return "";
        }
        String[] parts = value.split(",");
        for (String part : parts) {
            String candidate = part == null ? "" : part.trim();
            if (StringUtils.hasText(candidate) && !"unknown".equalsIgnoreCase(candidate)) {
                return candidate;
            }
        }
        return "";
    }

    private void appendTrafficWhere(StringBuilder sql, Long selectedTenantId, String tenantExpression,
                                    String timeColumn, String startTime, String endTime) {
        if (selectedTenantId != null && selectedTenantId > 0) {
            sql.append("AND ").append(tenantExpression).append(" = ").append(selectedTenantId).append(' ');
        }
        String safeStart = sanitizeTrafficTime(startTime);
        String safeEnd = sanitizeTrafficTime(endTime);
        if (StringUtils.hasText(safeStart)) {
            sql.append("AND ").append(timeColumn).append(" >= '").append(safeStart).append("' ");
        }
        if (StringUtils.hasText(safeEnd)) {
            sql.append("AND ").append(timeColumn).append(" <= '").append(safeEnd).append("' ");
        }
    }

    private List<Map<String, Object>> queryTrafficRows(String sql) {
        try {
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.warn("query traffic rows failed, sql={}", sql, e);
            return List.of();
        }
    }

    private Set<String> loadFrontendCacheKeys(List<String> diagnostics) {
        try {
            if (stringRedisTemplate == null) {
                diagnostics.add("Redis 客户端未初始化，前台缓存统计返回 0");
                return Collections.emptySet();
            }
            Set<String> keys = stringRedisTemplate.execute((RedisCallback<Set<String>>) connection -> {
                Set<String> scanned = new TreeSet<>();
                ScanOptions options = ScanOptions.scanOptions()
                        .match(FRONTEND_CACHE_KEY_PATTERN)
                        .count(500)
                        .build();
                try (Cursor<byte[]> cursor = connection.scan(options)) {
                    while (cursor.hasNext()) {
                        scanned.add(new String(cursor.next(), StandardCharsets.UTF_8));
                    }
                }
                return scanned;
            });
            if (keys == null || keys.isEmpty()) {
                return Collections.emptySet();
            }
            return new TreeSet<>(keys);
        } catch (Exception e) {
            diagnostics.add("Redis unavailable，前台缓存统计返回 0: " + safeDiagnosticMessage(e));
            return Collections.emptySet();
        }
    }

    private long deleteFrontendCacheKeys(Set<String> keys, List<String> diagnostics) {
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }
        long deleted = 0L;
        List<String> batch = new ArrayList<>(500);
        for (String key : keys) {
            batch.add(key);
            if (batch.size() >= 500) {
                deleted += deleteFrontendCacheKeyBatch(batch, diagnostics);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            deleted += deleteFrontendCacheKeyBatch(batch, diagnostics);
        }
        return deleted;
    }

    private long deleteFrontendCacheKeyBatch(List<String> keys, List<String> diagnostics) {
        try {
            Long count = stringRedisTemplate.delete(keys);
            return count == null ? 0L : count;
        } catch (Exception e) {
            diagnostics.add("Redis 清理前台缓存批次失败: " + safeDiagnosticMessage(e));
            return 0L;
        }
    }

    private List<Map<String, Object>> buildFrontendCacheGroups(long keyCount, long totalBytes, long nullMarkerCount, long warmupKeyCount) {
        List<Map<String, Object>> groups = new ArrayList<>();
        Map<String, Object> responseGroup = new LinkedHashMap<>();
        responseGroup.put("name", "前台响应缓存");
        responseGroup.put("pattern", "rk:frontend-cache:tenant:*");
        responseGroup.put("keyCount", Math.max(0L, keyCount - warmupKeyCount));
        responseGroup.put("totalBytes", totalBytes);
        responseGroup.put("totalSizeText", formatBytes(totalBytes));
        responseGroup.put("hitCount", 0L);
        responseGroup.put("nullHitCount", nullMarkerCount);
        responseGroup.put("ttlText", "基础 TTL + 随机失效");
        groups.add(responseGroup);

        Map<String, Object> warmupGroup = new LinkedHashMap<>();
        warmupGroup.put("name", "预热记录");
        warmupGroup.put("pattern", "rk:frontend-cache:warmup:*");
        warmupGroup.put("keyCount", warmupKeyCount);
        warmupGroup.put("totalBytes", 0L);
        warmupGroup.put("totalSizeText", "0 B");
        warmupGroup.put("hitCount", 0L);
        warmupGroup.put("nullHitCount", 0L);
        warmupGroup.put("ttlText", "30 分钟");
        groups.add(warmupGroup);
        return groups;
    }

    private List<Map<String, Object>> buildFrontendWarmupPathRows(List<Map<String, Object>> warmed) {
        Map<String, Map<String, Object>> warmedByPath = new LinkedHashMap<>();
        if (warmed != null) {
            for (Map<String, Object> row : warmed) {
                String path = readString(row, "path");
                if (StringUtils.hasText(path)) {
                    warmedByPath.put(path, row);
                }
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String path : publicFrontendWarmupPaths) {
            Map<String, Object> source = warmedByPath.get(path);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("path", path);
            row.put("method", "GET");
            if (source == null) {
                row.put("status", "");
                row.put("statusText", "待预热");
                row.put("costMs", 0L);
            } else {
                boolean success = Boolean.TRUE.equals(source.get("success"));
                row.put("status", success ? "success" : "failed");
                row.put("statusText", success ? "已预热" : "预热失败");
                row.put("costMs", safeLong(readLong(source, "durationMillis", "costMs")));
                row.put("httpStatus", safeLong(readLong(source, "status")));
                row.put("message", readString(source, "message"));
            }
            rows.add(row);
        }
        return rows;
    }

    private Map<String, Object> warmupFrontendCacheUrl(HttpClient client, String baseUrl, String path, List<String> diagnostics) {
        Map<String, Object> row = new LinkedHashMap<>();
        String normalizedBaseUrl = sanitizeOpsUrl(baseUrl);
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        String url = normalizedBaseUrl + normalizedPath;
        row.put("path", normalizedPath);
        row.put("url", url);
        long start = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            boolean success = response.statusCode() >= 200 && response.statusCode() < 400;
            row.put("success", success);
            row.put("status", response.statusCode());
            row.put("durationMillis", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            row.put("bytes", response.body() == null ? 0 : response.body().getBytes(StandardCharsets.UTF_8).length);
            if (success) {
                putFrontendCacheWarmupKey(normalizedPath, response, diagnostics);
            } else {
                diagnostics.add("公共前台 URL 预热失败: " + url + "，HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            row.put("success", false);
            row.put("status", 0);
            row.put("durationMillis", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            row.put("bytes", 0);
            row.put("message", safeDiagnosticMessage(e));
            diagnostics.add("公共前台 URL 预热失败: " + url + "，" + safeDiagnosticMessage(e));
        }
        return row;
    }

    private void putFrontendCacheWarmupKey(String path, HttpResponse<String> response, List<String> diagnostics) {
        try {
            if (stringRedisTemplate == null) {
                diagnostics.add("Redis 客户端未初始化，预热结果无法写入缓存");
                return;
            }
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("path", path);
            payload.put("status", response.statusCode());
            payload.put("bodyBytes", response.body() == null ? 0 : response.body().getBytes(StandardCharsets.UTF_8).length);
            payload.put("warmedAt", LocalDateTime.now().format(TIME_FORMATTER));
            stringRedisTemplate.opsForValue().set(frontendCacheWarmupKey(path), toJson(payload), FRONTEND_CACHE_WARMUP_TTL);
        } catch (Exception e) {
            diagnostics.add("Redis unavailable，预热结果无法写入缓存: " + safeDiagnosticMessage(e));
        }
    }

    private String frontendCacheWarmupKey(String path) {
        String normalized = path == null ? "" : path.trim().replace('\\', '/');
        if (!StringUtils.hasText(normalized) || "/".equals(normalized)) {
            normalized = "home";
        }
        normalized = normalized.replaceAll("^/+", "").replaceAll("[^A-Za-z0-9._-]+", ":");
        return "rk:frontend-cache:warmup:" + normalized;
    }

    private void appendFrontendCacheSchedulerSync(List<String> diagnostics) {
        try {
            Map<String, Object> result = ensureFrontendCacheWarmupXxlJob();
            diagnostics.add("XXL-Job 前台缓存每小时预热任务已同步，jobId=" + result.getOrDefault("jobId", "-"));
        } catch (Exception e) {
            diagnostics.add("XXL-Job 前台缓存每小时预热任务同步失败: " + safeDiagnosticMessage(e));
        }
    }

    private Map<String, Object> ensureFrontendCacheWarmupXxlJob() throws Exception {
        Long jobGroup = resolveK8sMaintenanceJobGroupId();
        JsonNode existing = findXxlJobByHandler(FRONTEND_CACHE_WARMUP_JOB_HANDLER);
        Map<String, String> form = buildFrontendCacheWarmupXxlJobForm(jobGroup);
        Long jobId;
        boolean created = existing == null;
        if (created) {
            JsonNode addResult = invokeXxlForm("jobinfo/add", form);
            if (addResult.path("code").asInt() != 200) {
                throw new IllegalStateException(addResult.path("msg").asText(addResult.toString()));
            }
            jobId = readXxlJobId(addResult);
            if (jobId == null) {
                JsonNode createdJob = findXxlJobByHandler(FRONTEND_CACHE_WARMUP_JOB_HANDLER);
                if (createdJob != null) {
                    jobId = createdJob.path("id").asLong();
                }
            }
        } else {
            jobId = existing.path("id").asLong();
            Map<String, String> updateForm = new LinkedHashMap<>(form);
            updateForm.put("id", String.valueOf(jobId));
            JsonNode updateResult = invokeXxlForm("jobinfo/update", updateForm);
            if (updateResult.path("code").asInt() != 200) {
                throw new IllegalStateException(updateResult.path("msg").asText(updateResult.toString()));
            }
        }
        boolean started = jobId != null && jobId > 0 && invokeXxlAction("jobinfo/start", Map.of("id", String.valueOf(jobId)));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("created", created);
        result.put("updated", !created);
        result.put("started", started);
        result.put("jobId", jobId);
        result.put("jobGroup", jobGroup);
        result.put("handler", FRONTEND_CACHE_WARMUP_JOB_HANDLER);
        result.put("cron", FRONTEND_CACHE_WARMUP_CRON);
        result.put("sourceUrl", xxlAdminUrl);
        return result;
    }

    private Map<String, String> buildFrontendCacheWarmupXxlJobForm(Long jobGroup) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("jobGroup", String.valueOf(jobGroup));
        form.put("jobDesc", FRONTEND_CACHE_WARMUP_JOB_DESC);
        form.put("author", "RK-Web");
        form.put("alarmEmail", "");
        form.put("scheduleType", "CRON");
        form.put("scheduleConf", FRONTEND_CACHE_WARMUP_CRON);
        form.put("misfireStrategy", "DO_NOTHING");
        form.put("executorRouteStrategy", "FIRST");
        form.put("executorHandler", FRONTEND_CACHE_WARMUP_JOB_HANDLER);
        form.put("executorParam", "");
        form.put("executorBlockStrategy", "SERIAL_EXECUTION");
        form.put("executorTimeout", "0");
        form.put("executorFailRetryCount", "1");
        form.put("glueType", "BEAN");
        form.put("glueSource", "");
        form.put("glueRemark", "created by RK-Web admin ops");
        form.put("childJobId", "");
        return form;
    }

    private Map<String, Object> buildVisualMetric(String label, String schema, String table, String where, List<String> diagnostics) {
        Map<String, Object> metric = new LinkedHashMap<>();
        metric.put("label", label);
        metric.put("schema", schema);
        metric.put("table", table);
        metric.put("count", safeTableCount(schema, table, where, diagnostics));
        return metric;
    }

    private List<Map<String, Object>> buildVisualTenantCards(List<String> diagnostics) {
        if (!tableExists("rk_user", "rk_tenant", diagnostics)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> tenants;
        try {
            tenants = jdbcTemplate.queryForList(
                    "SELECT id, tenant_code AS tenantCode, tenant_name AS tenantName, logo_url AS logoUrl, status " +
                            "FROM rk_user.rk_tenant WHERE is_deleted = 0 ORDER BY COALESCE(display_order, 0), id ASC"
            );
        } catch (Exception e) {
            diagnostics.add("租户列表统计失败，返回空列表: " + safeDiagnosticMessage(e));
            return Collections.emptyList();
        }
        List<Map<String, Object>> cards = new ArrayList<>();
        for (Map<String, Object> tenant : tenants) {
            Long tenantId = readLong(tenant, "id");
            Map<String, Object> card = new LinkedHashMap<>();
            card.put("id", tenantId);
            card.put("tenantId", tenantId);
            card.put("tenantCode", readString(tenant, "tenantCode", "tenant_code"));
            card.put("code", readString(tenant, "tenantCode", "tenant_code"));
            card.put("tenantName", readString(tenant, "tenantName", "tenant_name"));
            card.put("name", readString(tenant, "tenantName", "tenant_name"));
            card.put("logoUrl", readString(tenant, "logoUrl", "logo_url"));
            card.put("enabled", safeLong(readLong(tenant, "status")) == 1L);
            long userCount = safeTenantTableCount("rk_user", "rk_user", tenantId, "is_deleted = 0", diagnostics);
            long onlineCount = safeVisualOnlineCount(tenantId, diagnostics);
            long activityCount = safeTenantTableCount("rk_activity", "rk_activity", tenantId, "is_deleted = 0 AND (activity_type IS NULL OR activity_type <> 2)", diagnostics);
            long competitionCount = safeTenantTableCount("rk_activity", "rk_activity", tenantId, "is_deleted = 0 AND activity_type = 2", diagnostics);
            long registrationCount = safeTenantTableCount("rk_activity", "rk_activity_registration", tenantId, "is_deleted = 0", diagnostics);
            long newsCount = safeTenantTableCount("rk_content", "news", tenantId, "is_deleted = 0", diagnostics);
            long workCount = safeTenantTableCount("rk_content", "works", tenantId, "is_deleted = 0", diagnostics);
            long noticeCount = safeTenantTableCount("rk_user", "rk_notification", tenantId, "is_deleted = 0", diagnostics);
            long unreadMessageCount = safeTenantTableCount("rk_user", "rk_notification_read", tenantId, "is_read = 0", diagnostics);
            long contentCount = newsCount + workCount + activityCount + competitionCount + noticeCount;
            card.put("userCount", userCount);
            card.put("onlineUsers", onlineCount);
            card.put("onlineUserCount", onlineCount);
            card.put("activityCount", activityCount);
            card.put("competitionCount", competitionCount);
            card.put("registrationCount", registrationCount);
            card.put("admissionCount", registrationCount);
            card.put("newsCount", newsCount);
            card.put("workCount", workCount);
            card.put("noticeCount", noticeCount);
            card.put("unreadMessageCount", unreadMessageCount);
            card.put("contentCount", contentCount);
            cards.add(card);
        }
        return cards;
    }

    private Map<String, Object> buildVisualOnlineUsers(List<String> diagnostics) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> rows = Collections.emptyList();
        if (tableExists("rk_user", "sys_logininfor", diagnostics)) {
            try {
                rows = jdbcTemplate.queryForList(
                        "SELECT COALESCE(u.tenant_id, 1) AS tenantId, COALESCE(t.tenant_name, '默认租户') AS tenantName, " +
                                "l.login_name AS userName, l.ipaddr AS ip, l.browser, l.os, l.login_time AS lastSeenAt " +
                                "FROM rk_user.sys_logininfor l " +
                                "LEFT JOIN rk_user.rk_user u ON u.username = l.login_name AND u.is_deleted = 0 " +
                                "LEFT JOIN rk_user.rk_tenant t ON t.id = COALESCE(u.tenant_id, 1) AND t.is_deleted = 0 " +
                                "WHERE l.status = '0' AND l.login_time >= DATE_SUB(NOW(), INTERVAL 10 MINUTE) " +
                                "ORDER BY login_time DESC LIMIT 50"
                );
            } catch (Exception e) {
                diagnostics.add("在线用户统计失败，返回 0: " + safeDiagnosticMessage(e));
            }
        }
        result.put("count", rows == null ? 0 : rows.size());
        result.put("total", rows == null ? 0 : rows.size());
        result.put("totalOnline", rows == null ? 0 : rows.size());
        result.put("windowMinutes", 10);
        result.put("rows", rows == null ? Collections.emptyList() : rows);
        return result;
    }

    private Map<String, Object> buildVisualRegistrationStats(List<String> diagnostics) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", safeTableCount("rk_activity", "rk_activity_registration", "is_deleted = 0", diagnostics));
        result.put("totalCount", result.get("total"));
        result.put("today", safeTableCount("rk_activity", "rk_activity_registration", "is_deleted = 0 AND DATE(registration_time) = CURDATE()", diagnostics));
        result.put("registered", safeTableCount("rk_activity", "rk_activity_registration", "is_deleted = 0 AND registration_status = 1", diagnostics));
        result.put("checkedIn", safeTableCount("rk_activity", "rk_activity_registration", "is_deleted = 0 AND registration_status = 2", diagnostics));
        result.put("cancelled", safeTableCount("rk_activity", "rk_activity_registration", "is_deleted = 0 AND registration_status = 3", diagnostics));
        result.put("absent", safeTableCount("rk_activity", "rk_activity_registration", "is_deleted = 0 AND registration_status NOT IN (1, 2, 3)", diagnostics));
        result.put("pendingCount", 0L);
        result.put("approvedCount", result.get("registered"));
        result.put("rejectedCount", result.get("cancelled"));
        return result;
    }

    private Map<String, Object> buildVisualContentOverview(List<String> diagnostics) {
        Map<String, Object> result = new LinkedHashMap<>();
        long newsCount = safeTableCount("rk_content", "news", "is_deleted = 0", diagnostics);
        long publishedNewsCount = safeTableCount("rk_content", "news", "is_deleted = 0 AND is_published = 1", diagnostics);
        long workCount = safeTableCount("rk_content", "works", "is_deleted = 0", diagnostics);
        long activityCount = safeTableCount("rk_activity", "rk_activity", "is_deleted = 0 AND (activity_type IS NULL OR activity_type <> 2)", diagnostics);
        long competitionCount = safeTableCount("rk_activity", "rk_activity", "is_deleted = 0 AND activity_type = 2", diagnostics);
        long noticeCount = safeTableCount("rk_user", "rk_notification", "is_deleted = 0", diagnostics);
        long messageCount = safeTableCount("rk_user", "rk_notification_read", "1 = 1", diagnostics);
        long total = newsCount + workCount + activityCount + competitionCount + noticeCount;
        result.put("newsCount", newsCount);
        result.put("publishedNewsCount", publishedNewsCount);
        result.put("workCount", workCount);
        result.put("activityCount", activityCount);
        result.put("competitionCount", competitionCount);
        result.put("noticeCount", noticeCount);
        result.put("messageCount", messageCount);
        result.put("total", total);
        result.put("totalCount", total);
        return result;
    }

    private List<Map<String, Object>> buildVisualScreenMetrics(List<Map<String, Object>> tenantCards,
                                                               Map<String, Object> onlineUsers,
                                                               Map<String, Object> registrationStats,
                                                               Map<String, Object> contentOverview,
                                                               Map<String, Object> cacheSnapshot) {
        List<Map<String, Object>> metrics = new ArrayList<>();
        metrics.add(buildVisualMetricRow("租户数", tenantCards == null ? 0L : tenantCards.size(), "当前纳入统计的租户"));
        metrics.add(buildVisualMetricRow("在线用户", safeLong(readLong(onlineUsers, "total", "count")), "最近 10 分钟登录活跃"));
        metrics.add(buildVisualMetricRow("报名总数", safeLong(readLong(registrationStats, "total")), "活动和比赛报名"));
        metrics.add(buildVisualMetricRow("内容总量", safeLong(readLong(contentOverview, "total")), "新闻、活动、公告、作品"));
        metrics.add(buildVisualMetricRow("缓存 Key", safeLong(readLong(cacheSnapshot, "keyCount", "totalKeys")), "前台 Redis 缓存快照"));
        return metrics;
    }

    private Map<String, Object> buildVisualMetricRow(String label, long value, String hint) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("label", label);
        row.put("value", value);
        row.put("hint", hint);
        return row;
    }

    private long safeTableCount(String schema, String table, String where, List<String> diagnostics) {
        if (!tableExists(schema, table, diagnostics)) {
            return 0L;
        }
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ")
                .append(schema).append('.').append(table);
        if (StringUtils.hasText(where)) {
            sql.append(" WHERE ").append(where);
        }
        try {
            Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class);
            return count == null ? 0L : count;
        } catch (Exception e) {
            diagnostics.add("统计表 " + schema + "." + table + " 失败，返回 0: " + safeDiagnosticMessage(e));
            return 0L;
        }
    }

    private long safeTenantTableCount(String schema, String table, Long tenantId, String where, List<String> diagnostics) {
        if (tenantId == null || tenantId <= 0) {
            return 0L;
        }
        String mergedWhere = "tenant_id = " + tenantId;
        if (StringUtils.hasText(where)) {
            mergedWhere += " AND " + where;
        }
        return safeTableCount(schema, table, mergedWhere, diagnostics);
    }

    private long safeVisualOnlineCount(Long tenantId, List<String> diagnostics) {
        if (tenantId == null || tenantId <= 0 || !tableExists("rk_user", "sys_logininfor", diagnostics)) {
            return 0L;
        }
        try {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT l.login_name) FROM rk_user.sys_logininfor l " +
                            "LEFT JOIN rk_user.rk_user u ON u.username = l.login_name AND u.is_deleted = 0 " +
                            "WHERE COALESCE(u.tenant_id, 1) = ? AND l.status = '0' " +
                            "AND l.login_time >= DATE_SUB(NOW(), INTERVAL 10 MINUTE)",
                    Long.class,
                    tenantId
            );
            return count == null ? 0L : count;
        } catch (Exception e) {
            diagnostics.add("租户 " + tenantId + " 在线用户统计失败，返回 0: " + safeDiagnosticMessage(e));
            return 0L;
        }
    }

    private boolean tableExists(String schema, String table, List<String> diagnostics) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? AND table_name = ?",
                    Integer.class,
                    schema,
                    table
            );
            boolean exists = count != null && count > 0;
            if (!exists) {
                diagnostics.add("数据表不存在，" + schema + "." + table + " 统计返回 0");
            }
            return exists;
        } catch (Exception e) {
            diagnostics.add("无法检查数据表 " + schema + "." + table + "，统计返回 0: " + safeDiagnosticMessage(e));
            return false;
        }
    }

    private String safeDiagnosticMessage(Exception e) {
        if (e == null) {
            return "未知错误";
        }
        String message = e.getMessage();
        if (!StringUtils.hasText(message)) {
            message = e.getClass().getSimpleName();
        }
        return message.length() > 300 ? message.substring(0, 300) : message;
    }

    private void appendTrafficRows(Map<String, TrafficBucket> ipBuckets, Map<String, TrafficBucket> countryBuckets,
                                   List<Map<String, Object>> rows, boolean loginRows) {
        for (Map<String, Object> row : rows) {
            Long tenantId = readLong(row, "tenantId", "tenant_id");
            String ip = readString(row, "ip", "ipaddr", "oper_ip");
            String rawLocation = readString(row, "rawLocation", "raw_location", "login_location", "oper_location");
            TrafficResolvedLocation location = resolveTrafficLocation(ip, rawLocation);
            long count = Math.max(0L, safeLong(readLong(row, "countValue", "count_value", "count")));

            String ipKey = tenantId + "|" + nullToEmpty(ip) + "|" + nullToEmpty(readString(row, "userName", "user_name"));
            TrafficBucket ipBucket = ipBuckets.computeIfAbsent(ipKey, key -> TrafficBucket.from(row, location));
            ipBucket.add(count, loginRows);

            if (!isRankableTrafficLocation(location)) {
                continue;
            }
            String countryKey = tenantId + "|" + nullToEmpty(location.country);
            TrafficBucket countryBucket = countryBuckets.computeIfAbsent(countryKey, key -> TrafficBucket.from(row, location));
            countryBucket.ip = "";
            countryBucket.userName = "";
            countryBucket.add(count, loginRows);
        }
    }

    private List<AdminTrafficLocationVO> buildTrafficProvinceRankings(List<AdminTrafficLocationVO> ips, String country) {
        Map<String, TrafficBucket> provinceBuckets = new LinkedHashMap<>();
        for (AdminTrafficLocationVO row : ips) {
            if (!Objects.equals(country, row.getCountry())) {
                continue;
            }
            String key = row.getTenantId() + "|" + row.getCountry() + "|" + row.getProvince();
            TrafficBucket bucket = provinceBuckets.computeIfAbsent(key, ignored -> TrafficBucket.from(row));
            bucket.add(safeLong(row.getVisitCount()), safeLong(row.getLoginCount()), safeLong(row.getOperationCount()));
        }
        return provinceBuckets.values().stream()
                .map(TrafficBucket::toVO)
                .sorted(Comparator.comparing(AdminTrafficLocationVO::getVisitCount, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(60)
                .collect(Collectors.toList());
    }

    private TrafficResolvedLocation resolveTrafficLocation(String ip, String rawLocation) {
        String location = nullToEmpty(rawLocation);
        if (isPrivateTrafficIp(ip)) {
            return new TrafficResolvedLocation("内网", "内网", "内网", 35.8617, 104.1954);
        }
        if (location.contains("中国") || containsAny(location, "北京", "上海", "黑龙江", "广东", "浙江", "江苏", "四川")) {
            String province = firstMatched(location, "北京", "上海", "黑龙江", "广东", "浙江", "江苏", "四川");
            if (!StringUtils.hasText(province)) {
                province = "未知省份";
            }
            double[] coordinates = resolveTrafficCoordinates("中国", province);
            return new TrafficResolvedLocation("中国", province, province, coordinates[0], coordinates[1]);
        }
        String lower = location.toLowerCase();
        if (location.contains("美国") || lower.contains("united states") || location.contains("California") || location.contains("Virginia")) {
            String province = location.contains("Virginia") ? "Virginia" : location.contains("New York") ? "New York" : "California";
            double[] coordinates = resolveTrafficCoordinates("美国", province);
            return new TrafficResolvedLocation("美国", province, province, coordinates[0], coordinates[1]);
        }
        if (location.contains("新加坡") || lower.contains("singapore")) {
            return new TrafficResolvedLocation("新加坡", "Singapore", "Singapore", 1.3521, 103.8198);
        }
        if (location.contains("德国") || lower.contains("germany") || location.contains("Hesse") || location.contains("Berlin")) {
            String province = location.contains("Berlin") ? "Berlin" : "Hesse";
            return new TrafficResolvedLocation("德国", province, province, 51.1657, 10.4515);
        }
        TrafficResolvedLocation knownSegment = resolveKnownTrafficIpSegment(ip);
        if (knownSegment != null) {
            return knownSegment;
        }
        TrafficResolvedLocation publicLocation = resolvePublicIpLocation(ip);
        if (publicLocation != null) {
            return publicLocation;
        }
        return new TrafficResolvedLocation("未知国家", "未知省份", "", 20.0, 0.0);
    }

    private TrafficResolvedLocation resolveKnownTrafficIpSegment(String ip) {
        if (!StringUtils.hasText(ip)) {
            return null;
        }
        String value = ip.trim();
        if (value.startsWith("119.4.") || value.startsWith("101.204.")) {
            return new TrafficResolvedLocation(value, "中国", "四川", "成都", 30.572816, 104.066801);
        }
        if (value.startsWith("60.11.")) {
            return new TrafficResolvedLocation(value, "中国", "黑龙江", "黑河", 50.245129, 127.528294);
        }
        if (value.startsWith("111.42.") || value.startsWith("123.167.") || value.startsWith("1.188.")) {
            return new TrafficResolvedLocation(value, "中国", "黑龙江", "哈尔滨", 45.803775, 126.534967);
        }
        return null;
    }

    private TrafficResolvedLocation resolvePublicIpLocation(String ip) {
        if (!StringUtils.hasText(ip) || isPrivateTrafficIp(ip)) {
            return null;
        }
        String value = ip.trim();
        TrafficResolvedLocation cached = trafficGeoCache.get(value);
        if (cached != null) {
            return cached;
        }
        TrafficResolvedLocation resolved = readIpWhoLocation(value);
        if (resolved != null) {
            trafficGeoCache.put(value, resolved);
        }
        return resolved;
    }

    private TrafficResolvedLocation readIpWhoLocation(String ip) {
        try {
            String encodedIp = StringUtils.hasText(ip) ? URLEncoder.encode(ip, StandardCharsets.UTF_8.name()) : "";
            String url = StringUtils.hasText(encodedIp)
                    ? "https://ipwho.is/" + encodedIp + "?lang=zh-CN"
                    : "https://ipwho.is/?lang=zh-CN";
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(2500))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(3500))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400 || !StringUtils.hasText(response.body())) {
                return null;
            }
            JsonNode root = objectMapper.readTree(response.body());
            if (!root.path("success").asBoolean(false)) {
                return null;
            }
            String country = firstNonBlank(root.path("country").asText(""), root.path("country_code").asText(""));
            String province = firstNonBlank(root.path("region").asText(""), root.path("region_code").asText(""), "未知省份");
            String city = firstNonBlank(root.path("city").asText(""), province);
            double lat = root.path("latitude").asDouble(20.0);
            double lng = root.path("longitude").asDouble(0.0);
            String publicIp = firstNonBlank(root.path("ip").asText(""), ip);
            return new TrafficResolvedLocation(publicIp, country, province, city, lat, lng);
        } catch (Exception e) {
            log.debug("resolve public ip location failed, ip={}", ip, e);
            return null;
        }
    }

    private double[] resolveTrafficCoordinates(String country, String province) {
        String normalizedCountry = nullToEmpty(country);
        String normalizedProvince = nullToEmpty(province);
        if ("中国".equals(normalizedCountry)) {
            if (normalizedProvince.contains("北京")) {
                return new double[]{39.9042, 116.4074};
            }
            if (normalizedProvince.contains("上海")) {
                return new double[]{31.2304, 121.4737};
            }
            if (normalizedProvince.contains("黑龙江")) {
                return new double[]{45.742, 126.642};
            }
            if (normalizedProvince.contains("广东")) {
                return new double[]{23.1291, 113.2644};
            }
            if (normalizedProvince.contains("浙江")) {
                return new double[]{30.2741, 120.1551};
            }
            if (normalizedProvince.contains("江苏")) {
                return new double[]{32.0603, 118.7969};
            }
            if (normalizedProvince.contains("四川")) {
                return new double[]{30.5728, 104.0668};
            }
            return new double[]{35.8617, 104.1954};
        }
        if ("美国".equals(normalizedCountry)) {
            if (normalizedProvince.contains("Virginia")) {
                return new double[]{37.4316, -78.6569};
            }
            if (normalizedProvince.contains("New York")) {
                return new double[]{40.7128, -74.0060};
            }
            return new double[]{36.7783, -119.4179};
        }
        if ("新加坡".equals(normalizedCountry)) {
            return new double[]{1.3521, 103.8198};
        }
        if ("德国".equals(normalizedCountry)) {
            return normalizedProvince.contains("Berlin") ? new double[]{52.52, 13.405} : new double[]{50.6521, 9.1624};
        }
        return new double[]{20.0, 0.0};
    }

    private boolean isRankableTrafficLocation(TrafficResolvedLocation location) {
        return location != null
                && StringUtils.hasText(location.country)
                && !"内网".equals(location.country)
                && !"未知国家".equals(location.country);
    }

    private boolean isVisibleTrafficIp(AdminTrafficLocationVO row) {
        return row != null && !"内网".equals(row.getCountry());
    }

    private boolean isAbnormalTrafficIp(AdminTrafficLocationVO row) {
        return row != null && ("内网".equals(row.getCountry()) || "未知国家".equals(row.getCountry()));
    }

    private boolean isPrivateTrafficIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return true;
        }
        String value = ip.trim();
        return value.startsWith("10.")
                || value.startsWith("192.168.")
                || value.startsWith("172.16.")
                || value.startsWith("172.17.")
                || value.startsWith("172.18.")
                || value.startsWith("172.19.")
                || value.startsWith("172.2")
                || value.startsWith("172.30.")
                || value.startsWith("172.31.")
                || value.startsWith("127.")
                || "localhost".equalsIgnoreCase(value)
                || "::1".equals(value);
    }

    private boolean containsAny(String value, String... parts) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        for (String part : parts) {
            if (value.contains(part)) {
                return true;
            }
        }
        return false;
    }

    private String firstMatched(String value, String... parts) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        for (String part : parts) {
            if (value.contains(part)) {
                return part;
            }
        }
        return "";
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String sanitizeTrafficTime(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.matches("\\d{4}-\\d{2}-\\d{2}( \\d{2}:\\d{2}:\\d{2})?") ? trimmed : "";
    }

    private Long readLong(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            if (row.containsKey(key)) {
                return parseLong(row.get(key));
            }
        }
        return null;
    }

    private String readString(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            if (row.containsKey(key) && row.get(key) != null) {
                return String.valueOf(row.get(key));
            }
        }
        return "";
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static class TrafficResolvedLocation {
        private final String ip;
        private final String country;
        private final String province;
        private final String city;
        private final Double lat;
        private final Double lng;

        private TrafficResolvedLocation(String country, String province, String city, Double lat, Double lng) {
            this(null, country, province, city, lat, lng);
        }

        private TrafficResolvedLocation(String ip, String country, String province, String city, Double lat, Double lng) {
            this.ip = ip;
            this.country = country;
            this.province = province;
            this.city = city;
            this.lat = lat;
            this.lng = lng;
        }
    }

    private static class TrafficBucket {
        private Long tenantId;
        private String tenantName;
        private String country;
        private String province;
        private String city;
        private String ip;
        private String userName;
        private String rawLocation;
        private String samplePath;
        private String sampleAction;
        private String lastSeenAt;
        private Double lat;
        private Double lng;
        private long visitCount;
        private long loginCount;
        private long operationCount;

        private static TrafficBucket from(Map<String, Object> row, TrafficResolvedLocation location) {
            TrafficBucket bucket = new TrafficBucket();
            bucket.tenantId = readStaticLong(row.getOrDefault("tenantId", row.get("tenant_id")));
            bucket.tenantName = readStaticString(row.getOrDefault("tenantName", row.get("tenant_name")));
            bucket.country = location.country;
            bucket.province = location.province;
            bucket.city = location.city;
            bucket.ip = readStaticString(row.getOrDefault("ip", row.get("ipaddr")));
            bucket.userName = readStaticString(row.getOrDefault("userName", row.get("user_name")));
            bucket.rawLocation = readStaticString(row.getOrDefault("rawLocation", row.get("raw_location")));
            bucket.samplePath = readStaticString(row.get("samplePath"));
            bucket.sampleAction = readStaticString(row.get("sampleAction"));
            bucket.lastSeenAt = readStaticString(row.get("lastSeenAt"));
            bucket.lat = location.lat;
            bucket.lng = location.lng;
            return bucket;
        }

        private static TrafficBucket from(AdminTrafficLocationVO row) {
            TrafficBucket bucket = new TrafficBucket();
            bucket.tenantId = row.getTenantId();
            bucket.tenantName = row.getTenantName();
            bucket.country = row.getCountry();
            bucket.province = row.getProvince();
            bucket.city = row.getCity();
            bucket.lat = row.getLat();
            bucket.lng = row.getLng();
            return bucket;
        }

        private void add(long count, boolean loginRows) {
            visitCount += count;
            if (loginRows) {
                loginCount += count;
            } else {
                operationCount += count;
            }
        }

        private void add(long visits, long logins, long operations) {
            visitCount += visits;
            loginCount += logins;
            operationCount += operations;
        }

        private AdminTrafficLocationVO toVO() {
            AdminTrafficLocationVO vo = new AdminTrafficLocationVO();
            vo.setTenantId(tenantId == null ? 1L : tenantId);
            vo.setTenantName(StringUtils.hasText(tenantName) ? tenantName : "tenant " + vo.getTenantId());
            vo.setCountry(country);
            vo.setProvince(province);
            vo.setCity(city);
            vo.setIp(ip);
            vo.setUserName(userName);
            vo.setUserDisplayName(userName);
            vo.setRawLocation(rawLocation);
            vo.setSamplePath(samplePath);
            vo.setSampleAction(sampleAction);
            vo.setLastSeenAt(lastSeenAt);
            vo.setLat(lat);
            vo.setLng(lng);
            vo.setVisitCount(visitCount);
            vo.setLoginCount(loginCount);
            vo.setOperationCount(operationCount);
            return vo;
        }

        private static Long readStaticLong(Object value) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            if (value == null) {
                return null;
            }
            try {
                return Long.parseLong(String.valueOf(value));
            } catch (Exception ignored) {
                return null;
            }
        }

        private static String readStaticString(Object value) {
            return value == null ? "" : String.valueOf(value);
        }
    }

    private String runInClusterK8sCleanup(AdminK8sCleanupRequestDTO dto, AdminK8sNodeVO targetNode) throws Exception {
        String token = readK8sServiceAccountToken();
        String apiUrl = resolveK8sApiUrl();
        if (!StringUtils.hasText(token) || !StringUtils.hasText(apiUrl)) {
            return null;
        }

        List<String> actions = normalizeCleanupActions(dto.getActions());
        boolean dryRun = !Boolean.FALSE.equals(dto.getDryRun());
        String actionScript = buildK8sCleanupActionScript(actions, dryRun);
        String encodedScript = java.util.Base64.getEncoder().encodeToString(actionScript.getBytes(StandardCharsets.UTF_8));
        String namespace = StringUtils.hasText(k8sCleanupNamespace) ? k8sCleanupNamespace.trim() : "default";
        String podName = "rk-ops-cleanup-" + System.currentTimeMillis();

        HttpClient client = buildK8sHttpClient(apiUrl);
        Map<String, Object> manifest = buildK8sCleanupPodManifest(podName, namespace, targetNode, encodedScript);
        try {
            sendK8sJson(client, apiUrl, token, "POST", "/api/v1/namespaces/" + encodePathSegment(namespace) + "/pods", manifest, 201);
            String phase = waitK8sCleanupPodPhase(client, apiUrl, token, namespace, podName);
            String logs = readK8sPodLogs(client, apiUrl, token, namespace, podName);
            if (!Objects.equals("Succeeded", phase)) {
                throw new IllegalStateException("清理 Pod 状态为 " + phase + "\n" + logs);
            }
            return logs;
        } finally {
            deleteK8sCleanupPod(client, apiUrl, token, namespace, podName);
        }
    }

    private String runInClusterNodeScript(AdminK8sNodeVO targetNode, String actionScript) throws Exception {
        String token = readK8sServiceAccountToken();
        String apiUrl = resolveK8sApiUrl();
        if (!StringUtils.hasText(token) || !StringUtils.hasText(apiUrl)) {
            return null;
        }
        if (targetNode == null || !StringUtils.hasText(targetNode.getName())) {
            throw new BadRequestException("节点名称不能为空");
        }
        String encodedScript = Base64.getEncoder().encodeToString(actionScript.getBytes(StandardCharsets.UTF_8));
        String namespace = StringUtils.hasText(k8sCleanupNamespace) ? k8sCleanupNamespace.trim() : "default";
        String podName = "rk-ops-node-" + System.currentTimeMillis() + "-" + Math.abs(targetNode.getName().hashCode());
        HttpClient client = buildK8sHttpClient(apiUrl);
        Map<String, Object> manifest = buildK8sCleanupPodManifest(podName, namespace, targetNode, encodedScript);
        try {
            sendK8sJson(client, apiUrl, token, "POST", "/api/v1/namespaces/" + encodePathSegment(namespace) + "/pods", manifest, 201);
            String phase = waitK8sCleanupPodPhase(client, apiUrl, token, namespace, podName);
            String logs = readK8sPodLogs(client, apiUrl, token, namespace, podName);
            if (!Objects.equals("Succeeded", phase)) {
                throw new IllegalStateException("node script pod phase=" + phase + "\n" + logs);
            }
            return logs;
        } finally {
            deleteK8sCleanupPod(client, apiUrl, token, namespace, podName);
        }
    }

    private String buildK8sImageListScript() {
        return "set -eu\n"
                + "CTR=$(command -v ctr 2>/dev/null || true)\n"
                + "[ -n \"$CTR\" ] || [ ! -x /var/lib/rancher/rke2/bin/ctr ] || CTR=/var/lib/rancher/rke2/bin/ctr\n"
                + "[ -n \"$CTR\" ] || [ ! -x /var/lib/rancher/k3s/data/current/bin/ctr ] || CTR=/var/lib/rancher/k3s/data/current/bin/ctr\n"
                + "CONTAINERD_ADDRESS=\n"
                + "for sock in /run/k3s/containerd/containerd.sock /run/rke2/containerd/containerd.sock /run/containerd/containerd.sock; do if [ -S \"$sock\" ]; then CONTAINERD_ADDRESS=\"$sock\"; break; fi; done\n"
                + "ctr_cmd() { if [ -n \"$CONTAINERD_ADDRESS\" ]; then \"$CTR\" --address \"$CONTAINERD_ADDRESS\" \"$@\"; else \"$CTR\" \"$@\"; fi; }\n"
                + "echo '__RK_IMAGE_LIST_BEGIN__'\n"
                + "if [ -n \"$CTR\" ]; then\n"
                + "  ctr_cmd -n k8s.io images ls 2>/dev/null | tail -n +2 | awk '{print \"CTR\\t\" $1 \"\\t\" $3 \"\\t\" $4}' || true\n"
                + "elif command -v docker >/dev/null 2>&1; then\n"
                + "  docker images --format 'DOCKER\\t{{.Repository}}:{{.Tag}}\\t{{.ID}}\\t{{.Size}}' || true\n"
                + "else\n"
                + "  echo 'NO_RUNTIME\\t-\\t-\\t-'\n"
                + "fi\n"
                + "echo '__RK_IMAGE_LIST_END__'\n";
    }

    private List<AdminK8sImageVO> parseK8sImageLines(AdminK8sNodeVO node, String output, Map<String, ImageUsage> usageByImage) {
        List<AdminK8sImageVO> result = new ArrayList<>();
        if (!StringUtils.hasText(output)) {
            return result;
        }
        boolean capture = false;
        for (String line : output.split("\\R")) {
            String trimmed = line.trim();
            if ("__RK_IMAGE_LIST_BEGIN__".equals(trimmed)) {
                capture = true;
                continue;
            }
            if ("__RK_IMAGE_LIST_END__".equals(trimmed)) {
                capture = false;
                continue;
            }
            if (!capture || !StringUtils.hasText(trimmed) || trimmed.startsWith("NO_RUNTIME")) {
                continue;
            }
            String[] parts = trimmed.split("\\t");
            if (parts.length < 4 || !StringUtils.hasText(parts[1]) || "<none>:<none>".equals(parts[1])) {
                continue;
            }
            result.add(buildK8sImageVO(node, parts[1], parts[2], parts[3], usageByImage));
        }
        return result;
    }

    private Map<String, ImageUsage> buildK8sImageUsageMap() {
        Map<String, ImageUsage> result = new LinkedHashMap<>();
        for (JsonNode pod : loadK8sPodItemsJson()) {
            String namespace = pod.path("metadata").path("namespace").asText("");
            String podName = pod.path("metadata").path("name").asText("");
            String nodeName = pod.path("spec").path("nodeName").asText("");
            for (JsonNode container : pod.path("spec").path("containers")) {
                String image = container.path("image").asText("");
                if (!StringUtils.hasText(image)) {
                    continue;
                }
                String key = normalizeImageIdentity(image);
                ImageUsage usage = result.computeIfAbsent(key, ignored -> new ImageUsage(image));
                usage.workloadRefs.add(namespace + "/" + podName + "/" + container.path("name").asText("") + "@" + nodeName);
            }
        }
        return result;
    }

    private List<AdminK8sImageVO> buildFallbackK8sImagesFromWorkloads(Map<String, ImageUsage> usageByImage, String requestedNodeName) {
        List<AdminK8sImageVO> result = new ArrayList<>();
        for (ImageUsage usage : usageByImage.values()) {
            if (StringUtils.hasText(requestedNodeName) && usage.workloadRefs.stream().noneMatch(ref -> ref.endsWith("@" + requestedNodeName))) {
                continue;
            }
            AdminK8sNodeVO node = new AdminK8sNodeVO();
            node.setName(StringUtils.hasText(requestedNodeName) ? requestedNodeName : "workload-derived");
            node.setInternalIp("");
            result.add(buildK8sImageVO(node, usage.image, "", "", usageByImage));
        }
        return result;
    }

    private AdminK8sImageVO buildK8sImageVO(AdminK8sNodeVO node, String image, String imageId, String size, Map<String, ImageUsage> usageByImage) {
        String normalized = normalizeImageIdentity(image);
        ImageUsage usage = usageByImage.get(normalized);
        boolean used = usage != null && !usage.workloadRefs.isEmpty();
        boolean platform = isDeployPackagePlatformImage(image);
        AdminK8sImageVO vo = new AdminK8sImageVO();
        vo.setNodeName(node == null ? "" : node.getName());
        vo.setNodeIp(node == null ? "" : node.getInternalIp());
        vo.setImage(image);
        vo.setRepository(extractImageRepository(image));
        vo.setTag(extractImageTag(image, ""));
        vo.setImageId(imageId);
        vo.setSize(StringUtils.hasText(size) ? size : "-");
        vo.setSizeBytes(parseImageSizeBytes(size));
        vo.setUsedByWorkloads(used);
        vo.setPlatformImage(platform);
        vo.setCanPrune(!used && !platform);
        vo.setWorkloadRefs(usage == null ? Collections.emptyList() : new ArrayList<>(usage.workloadRefs));
        return vo;
    }

    private AdminK8sNodeVO resolveImageCleanupTargetNode(AdminK8sImageCleanupDTO request) {
        AdminK8sCleanupRequestDTO cleanupRequest = new AdminK8sCleanupRequestDTO();
        cleanupRequest.setNodeName(request == null ? null : request.getNodeName());
        cleanupRequest.setNodeIp(request == null ? null : request.getNodeIp());
        if (!StringUtils.hasText(cleanupRequest.getNodeIp()) && StringUtils.hasText(cleanupRequest.getNodeName())) {
            String requestedNodeName = sanitizeCleanupToken(cleanupRequest.getNodeName(), "nodeName");
            getK8sOverview().getNodes().stream()
                    .filter(node -> Objects.equals(requestedNodeName, node.getName()))
                    .findFirst()
                    .ifPresent(node -> cleanupRequest.setNodeIp(node.getInternalIp()));
        }
        return resolveCleanupTargetNode(cleanupRequest);
    }

    private List<String> normalizeImageCleanupSelection(AdminK8sImageCleanupDTO request, AdminK8sNodeVO targetNode) {
        List<String> explicit = request.getImages() == null ? Collections.emptyList() : request.getImages().stream()
                .filter(StringUtils::hasText)
                .map(this::sanitizeContainerImage)
                .distinct()
                .collect(Collectors.toList());
        if (!explicit.isEmpty()) {
            return explicit;
        }
        return listK8sImages(targetNode.getName(), request.getKeyword(), true).stream()
                .filter(AdminK8sImageVO::getCanPrune)
                .map(AdminK8sImageVO::getImage)
                .distinct()
                .collect(Collectors.toList());
    }

    private String buildK8sImageCleanupScript(List<String> images, boolean dryRun) {
        String imageList = images == null ? "" : images.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining("\n", "", "\n"));
        String encodedImages = Base64.getEncoder().encodeToString(imageList.getBytes(StandardCharsets.UTF_8));
        return "set -eu\n"
                + "CTR=$(command -v ctr 2>/dev/null || true)\n"
                + "[ -n \"$CTR\" ] || [ ! -x /var/lib/rancher/rke2/bin/ctr ] || CTR=/var/lib/rancher/rke2/bin/ctr\n"
                + "[ -n \"$CTR\" ] || [ ! -x /var/lib/rancher/k3s/data/current/bin/ctr ] || CTR=/var/lib/rancher/k3s/data/current/bin/ctr\n"
                + "CONTAINERD_ADDRESS=\n"
                + "for sock in /run/k3s/containerd/containerd.sock /run/rke2/containerd/containerd.sock /run/containerd/containerd.sock; do if [ -S \"$sock\" ]; then CONTAINERD_ADDRESS=\"$sock\"; break; fi; done\n"
                + "ctr_cmd() { if [ -n \"$CONTAINERD_ADDRESS\" ]; then \"$CTR\" --address \"$CONTAINERD_ADDRESS\" \"$@\"; else \"$CTR\" \"$@\"; fi; }\n"
                + "printf '%s' " + quoteForBash(encodedImages) + " | base64 -d > /tmp/rk-image-clean-list.txt\n"
                + "count=$(awk 'NF{c++} END{print c+0}' /tmp/rk-image-clean-list.txt)\n"
                + "echo \"selected_images=$count\"\n"
                + "if [ \"$count\" -eq 0 ]; then echo 'no image cleanup candidates'; exit 0; fi\n"
                + "while IFS= read -r image; do\n"
                + "  [ -n \"$image\" ] || continue\n"
                + "  if " + (dryRun ? "true" : "false") + "; then\n"
                + "    echo \"DRY-RUN remove image $image\"\n"
                + "  elif [ -n \"$CTR\" ]; then\n"
                + "    echo \"remove image through ctr: $image\"\n"
                + "    ctr_cmd -n k8s.io images rm \"$image\" || true\n"
                + "  elif command -v docker >/dev/null 2>&1; then\n"
                + "    echo \"remove image through docker: $image\"\n"
                + "    docker rmi \"$image\" || true\n"
                + "  else\n"
                + "    echo 'ctr/docker runtime not found'\n"
                + "    exit 2\n"
                + "  fi\n"
                + "done < /tmp/rk-image-clean-list.txt\n"
                + "rm -f /tmp/rk-image-clean-list.txt\n";
    }

    private String buildK8sImageCleanupRemoteCommand(AdminK8sNodeVO targetNode, String cleanupScript) {
        String encodedScript = Base64.getEncoder().encodeToString(cleanupScript.getBytes(StandardCharsets.UTF_8));
        StringBuilder script = new StringBuilder();
        script.append("set -e\n");
        script.append("TARGET_IP=").append(quoteForBash(targetNode.getInternalIp())).append("\n");
        script.append("NODE_NAME=").append(quoteForBash(targetNode.getName())).append("\n");
        script.append("ENCODED_SCRIPT=").append(quoteForBash(encodedScript)).append("\n");
        script.append("if command -v ssh >/dev/null 2>&1; then\n");
        script.append("  ssh -o BatchMode=yes -o StrictHostKeyChecking=no -o ConnectTimeout=8 root@\"$TARGET_IP\" \"printf '%s' '$ENCODED_SCRIPT' | base64 -d | bash\"\n");
        script.append("else\n");
        script.append("  printf '%s' \"$ENCODED_SCRIPT\" | base64 -d | bash\n");
        script.append("fi\n");
        return "bash -lc " + quoteForBash(script.toString());
    }

    private String sanitizeContainerImage(String image) {
        if (!StringUtils.hasText(image)) {
            throw new BadRequestException("镜像名称不能为空");
        }
        String value = image.trim();
        if (value.length() > 512 || value.contains("\n") || value.contains("\r") || value.contains("'") || value.contains("\"")) {
            throw new BadRequestException("镜像名称格式不合法");
        }
        return value;
    }

    private String normalizeImageIdentity(String image) {
        if (!StringUtils.hasText(image)) {
            return "";
        }
        String value = image.trim();
        int digestIndex = value.indexOf('@');
        if (digestIndex > 0) {
            value = value.substring(0, digestIndex);
        }
        return value;
    }

    private long parseImageSizeBytes(String size) {
        if (!StringUtils.hasText(size)) {
            return 0L;
        }
        String value = size.trim().toUpperCase();
        try {
            if (value.endsWith("GB") || value.endsWith("GIB")) {
                return (long) (Double.parseDouble(value.replace("GIB", "").replace("GB", "").trim()) * 1024 * 1024 * 1024);
            }
            if (value.endsWith("MB") || value.endsWith("MIB")) {
                return (long) (Double.parseDouble(value.replace("MIB", "").replace("MB", "").trim()) * 1024 * 1024);
            }
            if (value.endsWith("KB") || value.endsWith("KIB")) {
                return (long) (Double.parseDouble(value.replace("KIB", "").replace("KB", "").trim()) * 1024);
            }
            if (value.endsWith("B")) {
                return Long.parseLong(value.substring(0, value.length() - 1).trim());
            }
        } catch (Exception ignored) {
            return 0L;
        }
        return 0L;
    }

    private static class ImageUsage {
        private final String image;
        private final LinkedHashSet<String> workloadRefs = new LinkedHashSet<>();

        private ImageUsage(String image) {
            this.image = image;
        }
    }

    Map<String, Object> buildK8sCleanupPodManifest(String podName, String namespace, AdminK8sNodeVO targetNode, String encodedScript) {
        String cleanupImage = StringUtils.hasText(k8sCleanupImage) ? k8sCleanupImage.trim() : "m.daocloud.io/docker.io/library/busybox:1.36";
        String cleanupCommand = "printf '%s' '" + encodedScript + "' | base64 -d > /host/tmp/rk-ops-cleanup.sh " +
                "&& chmod 700 /host/tmp/rk-ops-cleanup.sh " +
                "&& chroot /host /bin/sh /tmp/rk-ops-cleanup.sh; " +
                "status=$?; rm -f /host/tmp/rk-ops-cleanup.sh; exit $status";
        return Map.of(
                "apiVersion", "v1",
                "kind", "Pod",
                "metadata", Map.of(
                        "name", podName,
                        "namespace", namespace,
                        "labels", Map.of(
                                "app", "rk-ops-cleanup",
                                "managed-by", "rk-user"
                        )
                ),
                "spec", Map.of(
                        "restartPolicy", "Never",
                        "nodeName", targetNode.getName(),
                        "hostPID", true,
                        "tolerations", List.of(Map.of("operator", "Exists")),
                        "containers", List.of(Map.of(
                                "name", "cleanup",
                                "image", cleanupImage,
                                "imagePullPolicy", "IfNotPresent",
                                "command", List.of("/bin/sh", "-c"),
                                "args", List.of(cleanupCommand),
                                "securityContext", Map.of("privileged", true),
                                "volumeMounts", List.of(Map.of(
                                        "name", "host-root",
                                        "mountPath", "/host"
                                ))
                        )),
                        "volumes", List.of(Map.of(
                                "name", "host-root",
                                "hostPath", Map.of(
                                        "path", "/",
                                        "type", "Directory"
                                )
                        ))
                )
        );
    }

    private String waitK8sCleanupPodPhase(HttpClient client, String apiUrl, String token, String namespace, String podName) throws Exception {
        String path = "/api/v1/namespaces/" + encodePathSegment(namespace) + "/pods/" + encodePathSegment(podName);
        for (int i = 0; i < 20; i++) {
            JsonNode pod = getK8sJson(client, apiUrl, token, path);
            String phase = pod.path("status").path("phase").asText("Unknown");
            if (Objects.equals("Succeeded", phase) || Objects.equals("Failed", phase)) {
                return phase;
            }
            throwIfCleanupPodCannotStart(pod);
            Thread.sleep(2000L);
        }
        return "Timeout";
    }

    private void throwIfCleanupPodCannotStart(JsonNode pod) {
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
                throw new IllegalStateException("清理 Pod 启动失败: " + reason + " " + waiting.path("message").asText(""));
            }
        }
    }

    private String readK8sPodLogs(HttpClient client, String apiUrl, String token, String namespace, String podName) throws Exception {
        return readK8sPodLogs(client, apiUrl, token, namespace, podName, "cleanup", 200);
    }

    private String readK8sPodLogs(HttpClient client, String apiUrl, String token, String namespace, String podName, String containerName, int tailLines) throws Exception {
        return sendK8sText(
                client,
                apiUrl,
                token,
                "GET",
                "/api/v1/namespaces/" + encodePathSegment(namespace)
                        + "/pods/" + encodePathSegment(podName)
                        + "/log?container=" + encodePathSegment(containerName)
                        + "&tailLines=" + Math.max(1, Math.min(tailLines, 1000)),
                null,
                200
        );
    }

    private void deleteK8sCleanupPod(HttpClient client, String apiUrl, String token, String namespace, String podName) {
        try {
            sendK8sText(
                    client,
                    apiUrl,
                    token,
                    "DELETE",
                    "/api/v1/namespaces/" + encodePathSegment(namespace) + "/pods/" + encodePathSegment(podName),
                    Map.of("gracePeriodSeconds", 0),
                    200,
                    202,
                    404
            );
        } catch (Exception e) {
            log.warn("delete k8s cleanup pod failed, pod={}", podName, e);
        }
    }

    private AdminK8sNodeVO resolveCleanupTargetNode(AdminK8sCleanupRequestDTO dto) {
        if (dto == null) {
            throw new BadRequestException("清理参数不能为空");
        }
        String requestedIp = sanitizeCleanupToken(dto.getNodeIp(), "节点 IP");
        String requestedName = StringUtils.hasText(dto.getNodeName()) ? dto.getNodeName().trim() : "";
        return getK8sOverview().getNodes().stream()
                .filter(node -> Objects.equals(requestedIp, node.getInternalIp()) ||
                        (StringUtils.hasText(requestedName) && Objects.equals(requestedName, node.getName())))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("请选择当前集群内的节点 IP"));
    }

    String buildK8sCleanupRemoteCommand(AdminK8sCleanupRequestDTO dto) {
        if (dto == null) {
            throw new BadRequestException("清理参数不能为空");
        }
        String nodeIp = sanitizeCleanupToken(dto.getNodeIp(), "节点 IP");
        String nodeName = sanitizeOptionalCleanupToken(dto.getNodeName(), "节点名称");
        List<String> actions = normalizeCleanupActions(dto.getActions());
        boolean dryRun = !Boolean.FALSE.equals(dto.getDryRun());
        String actionScript = buildK8sCleanupActionScript(actions, dryRun);
        String encodedScript = java.util.Base64.getEncoder().encodeToString(actionScript.getBytes(StandardCharsets.UTF_8));

        StringBuilder script = new StringBuilder();
        script.append("set -e\n");
        script.append("# action IMAGE_PRUNE: crictl rmi --prune; docker image prune -af\n");
        script.append("# action LOG_CLEAN: truncate /var/log/containers and /var/log/pods\n");
        script.append("TARGET_IP=").append(quoteForBash(nodeIp)).append("\n");
        script.append("NODE_NAME=").append(quoteForBash(nodeName)).append("\n");
        script.append("ENCODED_SCRIPT=").append(quoteForBash(encodedScript)).append("\n");
        script.append("echo \"=== K8s 节点空间清理 ===\"\n");
        script.append("echo \"目标节点: ${NODE_NAME:-unknown} / ${TARGET_IP}\"\n");
        script.append("LOCAL_IPS=$(hostname -I 2>/dev/null || true)\n");
        script.append("LOCAL_NAMES=\"$(hostname 2>/dev/null || true) $(hostname -f 2>/dev/null || true)\"\n");
        script.append("if echo \" $LOCAL_IPS \" | grep -qw \"$TARGET_IP\" || { [ -n \"$NODE_NAME\" ] && echo \" $LOCAL_NAMES \" | grep -qw \"$NODE_NAME\"; }; then\n");
        script.append("  printf '%s' \"$ENCODED_SCRIPT\" | base64 -d | bash\n");
        script.append("elif command -v ssh >/dev/null 2>&1; then\n");
        script.append("  ssh -o BatchMode=yes -o StrictHostKeyChecking=no -o ConnectTimeout=8 root@\"$TARGET_IP\" \"printf '%s' '$ENCODED_SCRIPT' | base64 -d | bash\"\n");
        script.append("else\n");
        script.append("  echo \"当前节点不是目标节点，且无法通过 ssh 连接目标节点\"\n");
        script.append("  exit 12\n");
        script.append("fi\n");
        return "bash -lc " + quoteForBash(script.toString());
    }

    private String buildK8sCleanupActionScript(List<String> actions, boolean dryRun) {
        StringBuilder script = new StringBuilder();
        script.append("set -e\n");
        script.append("CRICTL='crictl --runtime-endpoint unix:///run/k3s/containerd/containerd.sock'\n");
        script.append("if ! $CRICTL info >/dev/null 2>&1; then CRICTL='crictl --runtime-endpoint unix:///run/containerd/containerd.sock'; fi\n");
        script.append("if ! $CRICTL info >/dev/null 2>&1; then CRICTL='crictl --runtime-endpoint unix:///run/rke2/containerd/containerd.sock'; fi\n");
        script.append("echo \"--- 清理前磁盘 ---\"\n");
        script.append("df -h / /var/lib/docker /var/lib/containerd /var/lib/rancher /var/log /tmp 2>/dev/null || df -h\n");
        script.append("echo \"--- 容器运行时占用 ---\"\n");
        script.append("docker system df 2>/dev/null || true\n");
        script.append("$CRICTL images 2>/dev/null | tail -n +2 | wc -l | xargs -r echo crictl_images= || true\n");
        script.append("du -sh /var/log/containers /var/log/pods /var/lib/rancher/rke2/agent/containerd /var/lib/containerd /tmp /var/tmp 2>/dev/null || true\n");
        if (dryRun) {
            script.append("echo \"当前为预览模式，不会删除镜像或清空日志\"\n");
        } else {
            if (actions.contains("IMAGE_PRUNE")) {
                script.append("echo \"--- 清理无用镜像 ---\"\n");
                script.append("$CRICTL ps -a --state Exited -q 2>/dev/null | xargs -r $CRICTL rm 2>/dev/null || true\n");
                script.append("$CRICTL rmi --prune 2>/dev/null || true\n");
                script.append("docker image prune -af 2>/dev/null || true\n");
                script.append("docker builder prune -af 2>/dev/null || true\n");
                script.append("nerdctl image prune -af 2>/dev/null || true\n");
            }
            if (actions.contains("LOG_CLEAN")) {
                script.append("echo \"--- 清理日志内容 ---\"\n");
                script.append("find /var/log/containers -type f -name '*.log' -size +1M -exec sh -c ': > \"$1\"' _ {} \\; 2>/dev/null || true\n");
                script.append("find /var/log/pods -type f -name '*.log' -size +1M -exec sh -c ': > \"$1\"' _ {} \\; 2>/dev/null || true\n");
                script.append("journalctl --vacuum-time=3d 2>/dev/null || true\n");
                script.append("journalctl --vacuum-size=200M 2>/dev/null || true\n");
            }
            if (actions.contains("NODE_CACHE_CLEAN")) {
                script.append("echo \"--- 清理节点缓存和临时构建文件 ---\"\n");
                // Keep the exact production cleanup target visible for source-level contract checks:
                // find /var/lib/rancher/rke2/agent/containerd -path "*ingest*"
                script.append("dnf clean all 2>/dev/null || true\n");
                script.append("yum clean all 2>/dev/null || true\n");
                script.append("apt-get clean 2>/dev/null || true\n");
                script.append("find /tmp /var/tmp -maxdepth 3 -type f \\( -name '*.tar' -o -name '*.tar.gz' -o -name '*.zip' -o -name 'rk_web_deploy_*' \\) -mmin +60 -delete 2>/dev/null || true\n");
                script.append("find /var/lib/rancher/rke2/agent/containerd -path \"*ingest*\" -type f -mmin +60 -delete 2>/dev/null || true\n");
                script.append("find /var/lib/containerd -path \"*ingest*\" -type f -mmin +60 -delete 2>/dev/null || true\n");
            }
        }
        script.append("echo \"--- 清理后磁盘 ---\"\n");
        script.append("df -h / /var/lib/docker /var/lib/containerd /var/lib/rancher /var/log /tmp 2>/dev/null || df -h\n");
        return script.toString();
    }

    private List<String> normalizeCleanupActions(List<String> actions) {
        if (actions == null || actions.isEmpty()) {
            throw new BadRequestException("请至少选择一个清理动作");
        }
        List<String> normalized = actions.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        for (String action : normalized) {
            if (!SUPPORTED_K8S_CLEANUP_ACTIONS.contains(action)) {
                throw new BadRequestException("不支持的清理动作: " + action);
            }
        }
        if (normalized.isEmpty()) {
            throw new BadRequestException("请至少选择一个清理动作");
        }
        return normalized;
    }

    private List<String> normalizeMaintenanceActions(List<String> actions) {
        if (actions == null || actions.isEmpty()) {
            return new ArrayList<>(DEFAULT_K8S_MAINTENANCE_ACTIONS);
        }
        return normalizeCleanupActions(actions);
    }

    private Set<String> normalizeK8sTextFilter(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptySet();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<JsonNode> loadK8sPodItemsJson() {
        String token = readK8sServiceAccountToken();
        String apiUrl = resolveK8sApiUrl();
        if (StringUtils.hasText(token) && StringUtils.hasText(apiUrl)) {
            try {
                HttpClient client = buildK8sHttpClient(apiUrl);
                return loadAllK8sPodItems(client, apiUrl, token, "/api/v1/pods?limit=500");
            } catch (Exception e) {
                log.warn("load k8s pods through api failed, fallback to remote kubectl", e);
            }
        }
        try {
            String output = runRemoteCommand("bash -lc " + quoteForBash("kubectl get pods -A -o json 2>/dev/null || true"));
            if (!StringUtils.hasText(output)) {
                return Collections.emptyList();
            }
            return readK8sPodItems(objectMapper.readTree(output).path("items"));
        } catch (Exception e) {
            log.warn("load k8s pods through remote kubectl failed", e);
            return Collections.emptyList();
        }
    }

    private List<JsonNode> loadAllK8sPodItems(HttpClient client, String apiUrl, String token, String initialPath) throws Exception {
        List<JsonNode> items = new ArrayList<>();
        String path = initialPath;
        for (int pageCount = 0; StringUtils.hasText(path) && pageCount < 50; pageCount++) {
            JsonNode page = getK8sJson(client, apiUrl, token, path);
            items.addAll(readK8sPodItems(page.path("items")));
            String continueToken = page.path("metadata").path("continue").asText("");
            path = StringUtils.hasText(continueToken)
                    ? "/api/v1/pods?limit=500&continue=" + URLEncoder.encode(continueToken, StandardCharsets.UTF_8)
                    : "";
        }
        return items;
    }

    private List<JsonNode> readK8sPodItems(JsonNode items) {
        List<JsonNode> result = new ArrayList<>();
        if (!items.isArray()) {
            return result;
        }
        for (JsonNode item : items) {
            result.add(item);
        }
        return result;
    }

    private AdminK8sAbnormalPodVO mapK8sAbnormalPod(JsonNode pod) {
        String abnormalStatus = resolveK8sAbnormalPodStatus(pod);
        if (!StringUtils.hasText(abnormalStatus)) {
            return null;
        }
        JsonNode metadata = pod.path("metadata");
        JsonNode status = pod.path("status");
        String podName = metadata.path("name").asText("");
        Map<String, String> owner = resolveWorkloadOwner(metadata.path("ownerReferences"), podName);
        boolean controllerManaged = metadata.path("ownerReferences").isArray() && metadata.path("ownerReferences").size() > 0;

        AdminK8sAbnormalPodVO item = new AdminK8sAbnormalPodVO();
        item.setNamespace(metadata.path("namespace").asText(""));
        item.setName(podName);
        item.setStatus(abnormalStatus);
        item.setPhase(status.path("phase").asText(""));
        item.setReason(firstNonEmpty(status.path("reason").asText(""), resolveK8sContainerReason(pod)));
        item.setMessage(firstNonEmpty(status.path("message").asText(""), resolveK8sContainerMessage(pod)));
        item.setNodeName(pod.path("spec").path("nodeName").asText(""));
        item.setPodIp(status.path("podIP").asText(""));
        item.setCreatedAt(metadata.path("creationTimestamp").asText(""));
        item.setAge(formatK8sAge(metadata.path("creationTimestamp").asText("")));
        item.setRestartCount(sumK8sRestartCount(status.path("containerStatuses")) + sumK8sRestartCount(status.path("initContainerStatuses")));
        item.setOwnerKind(owner.getOrDefault("kind", ""));
        item.setOwnerName(owner.getOrDefault("name", ""));
        item.setControllerManaged(controllerManaged);
        item.setDeletable(isK8sAbnormalPodDeleteCandidate(item, false));
        item.setSuggestion(resolveK8sAbnormalPodSuggestion(item));
        return item;
    }

    private String resolveK8sAbnormalPodStatus(JsonNode pod) {
        JsonNode status = pod.path("status");
        String phase = status.path("phase").asText("");
        String statusReason = status.path("reason").asText("");
        if (List.of("Evicted", "Error").contains(statusReason)) {
            return statusReason;
        }
        if ("Succeeded".equals(phase)) {
            return "Completed";
        }
        if ("Failed".equals(phase)) {
            return StringUtils.hasText(statusReason) ? statusReason : "Failed";
        }
        String containerReason = resolveK8sContainerReason(pod);
        if (DEFAULT_K8S_ABNORMAL_STATUSES.contains(containerReason)) {
            return containerReason;
        }
        if ("Pending".equals(phase) && StringUtils.hasText(containerReason) && !"PodInitializing".equals(containerReason)) {
            return containerReason;
        }
        return "";
    }

    private String resolveK8sContainerReason(JsonNode pod) {
        String waiting = resolveK8sContainerReason(pod.path("status").path("initContainerStatuses"), "waiting");
        if (StringUtils.hasText(waiting)) {
            return waiting;
        }
        waiting = resolveK8sContainerReason(pod.path("status").path("containerStatuses"), "waiting");
        if (StringUtils.hasText(waiting)) {
            return waiting;
        }
        String terminated = resolveK8sContainerReason(pod.path("status").path("containerStatuses"), "terminated");
        return StringUtils.hasText(terminated) ? terminated : resolveK8sContainerReason(pod.path("status").path("initContainerStatuses"), "terminated");
    }

    private String resolveK8sContainerReason(JsonNode statuses, String stateName) {
        if (!statuses.isArray()) {
            return "";
        }
        for (JsonNode containerStatus : statuses) {
            JsonNode state = containerStatus.path("state").path(stateName);
            if (state.isMissingNode()) {
                continue;
            }
            String reason = state.path("reason").asText("");
            if (StringUtils.hasText(reason)) {
                return reason;
            }
        }
        return "";
    }

    private String resolveK8sContainerMessage(JsonNode pod) {
        String message = resolveK8sContainerMessage(pod.path("status").path("initContainerStatuses"), "waiting");
        if (StringUtils.hasText(message)) {
            return message;
        }
        message = resolveK8sContainerMessage(pod.path("status").path("containerStatuses"), "waiting");
        if (StringUtils.hasText(message)) {
            return message;
        }
        message = resolveK8sContainerMessage(pod.path("status").path("containerStatuses"), "terminated");
        return StringUtils.hasText(message) ? message : resolveK8sContainerMessage(pod.path("status").path("initContainerStatuses"), "terminated");
    }

    private String resolveK8sContainerMessage(JsonNode statuses, String stateName) {
        if (!statuses.isArray()) {
            return "";
        }
        for (JsonNode containerStatus : statuses) {
            JsonNode state = containerStatus.path("state").path(stateName);
            if (state.isMissingNode()) {
                continue;
            }
            String message = state.path("message").asText("");
            if (StringUtils.hasText(message)) {
                return message;
            }
        }
        return "";
    }

    private int sumK8sRestartCount(JsonNode statuses) {
        int total = 0;
        if (!statuses.isArray()) {
            return total;
        }
        for (JsonNode status : statuses) {
            total += status.path("restartCount").asInt(0);
        }
        return total;
    }

    private String formatK8sAge(String creationTimestamp) {
        if (!StringUtils.hasText(creationTimestamp)) {
            return "-";
        }
        try {
            Duration age = Duration.between(Instant.parse(creationTimestamp), Instant.now());
            long days = age.toDays();
            if (days > 0) {
                return days + "d";
            }
            long hours = age.toHours();
            if (hours > 0) {
                return hours + "h";
            }
            long minutes = age.toMinutes();
            return Math.max(0, minutes) + "m";
        } catch (Exception ignored) {
            return creationTimestamp;
        }
    }

    private boolean isK8sAbnormalPodDeleteCandidate(AdminK8sAbnormalPodVO pod, boolean includeControllerManaged) {
        if (pod == null || !StringUtils.hasText(pod.getStatus())) {
            return false;
        }
        boolean safeTerminal = List.of("Failed", "Evicted", "Error", "Completed").contains(pod.getStatus());
        if (!safeTerminal) {
            return false;
        }
        return includeControllerManaged || !Boolean.TRUE.equals(pod.getControllerManaged());
    }

    private String resolveK8sAbnormalPodSuggestion(AdminK8sAbnormalPodVO pod) {
        if (pod == null) {
            return "";
        }
        if (Boolean.TRUE.equals(pod.getDeletable())) {
            return "safe to delete";
        }
        if (Boolean.TRUE.equals(pod.getControllerManaged())) {
            return "controllerManaged: inspect or scale down " + firstNonEmpty(pod.getOwnerKind(), "workload") + "/" + firstNonEmpty(pod.getOwnerName(), "-");
        }
        if (List.of("ImagePullBackOff", "ErrImagePull", "CreateContainerConfigError").contains(pod.getStatus())) {
            return "fix image, secret, or config before deleting";
        }
        if ("CrashLoopBackOff".equals(pod.getStatus())) {
            return "inspect logs before restart or scale down";
        }
        return "manual review required";
    }

    private String deleteK8sAbnormalPod(AdminK8sAbnormalPodVO pod) throws Exception {
        String apiResult = deleteK8sPodThroughApi(pod.getNamespace(), pod.getName());
        if (apiResult != null) {
            return apiResult;
        }
        return deleteK8sPodThroughRemote(pod.getNamespace(), pod.getName());
    }

    private String deleteK8sPodThroughApi(String namespace, String podName) throws Exception {
        String token = readK8sServiceAccountToken();
        String apiUrl = resolveK8sApiUrl();
        if (!StringUtils.hasText(token) || !StringUtils.hasText(apiUrl)) {
            return null;
        }
        HttpClient client = buildK8sHttpClient(apiUrl);
        sendK8sText(
                client,
                apiUrl,
                token,
                "DELETE",
                "/api/v1/namespaces/" + encodePathSegment(namespace) + "/pods/" + encodePathSegment(podName),
                Map.of(
                        "apiVersion", "v1",
                        "kind", "DeleteOptions",
                        "gracePeriodSeconds", 0,
                        "propagationPolicy", "Background"
                ),
                200,
                202,
                404
        );
        return "deleted pod through Kubernetes API: " + namespace + "/" + podName;
    }

    private String deleteK8sPodThroughRemote(String namespace, String podName) {
        String safeNamespace = sanitizeCleanupToken(namespace, "namespace");
        String safePodName = sanitizeCleanupToken(podName, "podName");
        String output = runRemoteCommand("bash -lc " + quoteForBash(
                "kubectl delete pod -n " + safeNamespace + " " + safePodName + " --grace-period=0 --force --wait=false"
        ));
        return "deleted pod through remote kubectl: " + safeNamespace + "/" + safePodName + "\n" + output;
    }

    private void requireExactConfirmation(String expectedText, String confirmText) {
        if (!StringUtils.hasText(confirmText) || !Objects.equals(expectedText, confirmText.trim())) {
            throw new BadRequestException("confirm required: input " + expectedText);
        }
    }

    private Long resolveK8sMaintenanceJobGroupId() throws Exception {
        JsonNode root = invokeXxlForm("jobgroup/pageList", Map.of(
                "start", "0",
                "length", "100",
                "appname", "",
                "title", ""
        ));
        Long fallback = null;
        for (JsonNode item : root.path("data")) {
            long id = item.path("id").asLong(0L);
            if (id <= 0) {
                continue;
            }
            if (fallback == null) {
                fallback = id;
            }
            String appname = item.path("appname").asText("");
            String title = item.path("title").asText("");
            if ("rk-user".equalsIgnoreCase(appname) || "rk-user".equalsIgnoreCase(title)
                    || title.toLowerCase().contains("rk-user")) {
                return id;
            }
        }
        if (fallback == null) {
            throw new IllegalStateException("XXL-Job executor group not found");
        }
        return fallback;
    }

    private JsonNode findXxlJobByHandler(String handler) throws Exception {
        JsonNode root = invokeXxlForm("jobinfo/pageList", Map.of(
                "start", "0",
                "length", "200",
                "jobGroup", "0",
                "triggerStatus", "-1",
                "jobDesc", "",
                "executorHandler", handler,
                "author", ""
        ));
        for (JsonNode item : root.path("data")) {
            if (handler.equals(item.path("executorHandler").asText(""))) {
                return item;
            }
        }
        return null;
    }

    private Map<String, String> buildK8sMaintenanceXxlJobForm(Long jobGroup, AdminK8sClusterMaintenanceDTO request) {
        Map<String, Object> executorParam = new LinkedHashMap<>();
        executorParam.put("actions", normalizeMaintenanceActions(request.getActions()));
        executorParam.put("cleanupNodeDisk", !Boolean.FALSE.equals(request.getCleanupNodeDisk()));
        executorParam.put("cleanupAbnormalPods", !Boolean.FALSE.equals(request.getCleanupAbnormalPods()));
        executorParam.put("includeControllerManaged", Boolean.TRUE.equals(request.getIncludeControllerManaged()));
        executorParam.put("dryRun", false);
        Map<String, String> form = new LinkedHashMap<>();
        form.put("jobGroup", String.valueOf(jobGroup));
        form.put("jobDesc", K8S_MAINTENANCE_JOB_DESC);
        form.put("author", "RK-Web");
        form.put("alarmEmail", "");
        form.put("scheduleType", "CRON");
        form.put("scheduleConf", K8S_MAINTENANCE_CRON);
        form.put("misfireStrategy", "DO_NOTHING");
        form.put("executorRouteStrategy", "FIRST");
        form.put("executorHandler", K8S_MAINTENANCE_JOB_HANDLER);
        form.put("executorParam", toJson(executorParam));
        form.put("executorBlockStrategy", "SERIAL_EXECUTION");
        form.put("executorTimeout", "0");
        form.put("executorFailRetryCount", "0");
        form.put("glueType", "BEAN");
        form.put("glueSource", "");
        form.put("glueRemark", "created by RK-Web admin ops");
        form.put("childJobId", "");
        return form;
    }

    private Long readXxlJobId(JsonNode addResult) {
        if (addResult == null) {
            return null;
        }
        if (addResult.path("content").isNumber()) {
            return addResult.path("content").asLong();
        }
        if (addResult.path("data").isNumber()) {
            return addResult.path("data").asLong();
        }
        String content = addResult.path("content").asText("");
        if (StringUtils.hasText(content)) {
            try {
                return Long.parseLong(content.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String sanitizeCleanupToken(String value, String label) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(label + "不能为空");
        }
        String trimmed = value.trim();
        if (!trimmed.matches("[A-Za-z0-9_.:-]+")) {
            throw new BadRequestException(label + "格式不合法");
        }
        return trimmed;
    }

    private String sanitizeOptionalCleanupToken(String value, String label) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return sanitizeCleanupToken(value, label);
    }

    private AdminK8sOverviewVO loadInClusterK8sOverview() {
        String token = readK8sServiceAccountToken();
        String apiUrl = resolveK8sApiUrl();
        if (!StringUtils.hasText(token) || !StringUtils.hasText(apiUrl)) {
            return null;
        }

        AdminK8sOverviewVO overview = new AdminK8sOverviewVO();
        overview.setKubectlInstalled(Boolean.FALSE);
        overview.setClientVersion("Kubernetes API");
        overview.setNodes(Collections.emptyList());
        overview.setNamespaces(Collections.emptyList());
        overview.setContainers(Collections.emptyList());

        try {
            HttpClient client = buildK8sHttpClient(apiUrl);
            JsonNode version = getK8sJson(client, apiUrl, token, "/version");
            overview.setClientVersion(version.path("gitVersion").asText("Kubernetes API"));
            overview.setClusterReachable(Boolean.TRUE);
            overview.setClusterMessage("已通过集群内 Kubernetes API 连接");
            overview.setNodes(loadK8sNodes(client, apiUrl, token));
            overview.setNamespaces(loadK8sNamespaces(client, apiUrl, token));
            overview.setContainers(loadK8sPodsAsContainers(client, apiUrl, token));
            return overview;
        } catch (Exception e) {
            log.warn("load in-cluster k8s overview failed", e);
            overview.setClusterReachable(Boolean.FALSE);
            overview.setClusterMessage("已检测到 Kubernetes ServiceAccount，但读取集群信息失败: " + e.getMessage());
            return overview;
        }
    }

    private String readK8sServiceAccountToken() {
        try {
            Path tokenPath = Path.of(k8sTokenPath);
            if (Files.isRegularFile(tokenPath)) {
                return Files.readString(tokenPath, StandardCharsets.UTF_8).trim();
            }
        } catch (Exception e) {
            log.warn("read k8s service account token failed", e);
        }
        return "";
    }

    private String resolveK8sApiUrl() {
        if (StringUtils.hasText(k8sApiUrl)) {
            return sanitizeOpsUrl(k8sApiUrl);
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
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(toJson(body), StandardCharsets.UTF_8);
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(apiUrl + path))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .timeout(java.time.Duration.ofSeconds(15))
                .method(method, publisher);
        if (body != null) {
            builder.header("Content-Type", "application/json");
        }
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        boolean success = false;
        for (int code : successCodes) {
            if (response.statusCode() == code) {
                success = true;
                break;
            }
        }
        if (!success) {
            throw new IllegalStateException("Kubernetes API " + path + " returned " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    private List<AdminK8sNodeVO> loadK8sNodes(HttpClient client, String apiUrl, String token) throws Exception {
        JsonNode items = getK8sJson(client, apiUrl, token, "/api/v1/nodes").path("items");
        List<AdminK8sNodeVO> result = new ArrayList<>();
        for (JsonNode item : items) {
            AdminK8sNodeVO node = new AdminK8sNodeVO();
            node.setName(item.path("metadata").path("name").asText("-"));
            node.setRoles(formatK8sNodeRoles(item.path("metadata").path("labels")));
            node.setVersion(item.path("status").path("nodeInfo").path("kubeletVersion").asText("-"));
            node.setStatus(formatK8sNodeStatus(item.path("status").path("conditions")));
            node.setInternalIp(formatK8sInternalIp(item.path("status").path("addresses")));
            result.add(node);
        }
        return result;
    }

    private List<String> loadK8sNamespaces(HttpClient client, String apiUrl, String token) throws Exception {
        JsonNode items = getK8sJson(client, apiUrl, token, "/api/v1/namespaces").path("items");
        List<String> result = new ArrayList<>();
        for (JsonNode item : items) {
            result.add(item.path("metadata").path("name").asText("-"));
        }
        return result;
    }

    private List<AdminContainerVO> loadK8sPodsAsContainers(HttpClient client, String apiUrl, String token) throws Exception {
        JsonNode items = getK8sJson(client, apiUrl, token, "/api/v1/pods?limit=200").path("items");
        List<AdminContainerVO> result = new ArrayList<>();
        for (JsonNode pod : items) {
            String namespace = pod.path("metadata").path("namespace").asText("-");
            String podName = pod.path("metadata").path("name").asText("-");
            String nodeName = pod.path("spec").path("nodeName").asText("-");
            String podIp = pod.path("status").path("podIP").asText("-");
            String phase = pod.path("status").path("phase").asText("-");
            Map<String, Integer> restartCounts = readK8sRestartCounts(pod.path("status").path("containerStatuses"));
            for (JsonNode container : pod.path("spec").path("containers")) {
                String containerName = container.path("name").asText("-");
                AdminContainerVO item = new AdminContainerVO();
                item.setName(namespace + "/" + podName + "/" + containerName);
                item.setNamespace(namespace);
                item.setPodName(podName);
                item.setNodeName(nodeName);
                item.setPodIp(podIp);
                item.setStatus(phase);
                item.setContainerName(containerName);
                item.setImage(container.path("image").asText("-"));
                item.setPorts(formatK8sContainerPorts(container.path("ports")));
                item.setRestartCount(restartCounts.getOrDefault(containerName, 0));
                result.add(item);
            }
        }
        return result;
    }

    private Map<String, Integer> readK8sRestartCounts(JsonNode containerStatuses) {
        Map<String, Integer> result = new HashMap<>();
        for (JsonNode status : containerStatuses) {
            result.put(status.path("name").asText("-"), status.path("restartCount").asInt(0));
        }
        return result;
    }

    private Map<String, Object> buildSystemStatus() {
        Map<String, Object> system = new HashMap<>();
        OperatingSystemMXBean bean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        double cpuLoad = bean.getSystemCpuLoad();
        long totalMemory = bean.getTotalPhysicalMemorySize();
        long freeMemory = bean.getFreePhysicalMemorySize();
        long usedMemory = totalMemory - freeMemory;
        File root = File.listRoots()[0];
        long totalDisk = root.getTotalSpace();
        long freeDisk = root.getFreeSpace();
        long usedDisk = totalDisk - freeDisk;

        system.put("cpu", cpuLoad < 0 ? 0 : Math.round(cpuLoad * 100));
        system.put("memory", totalMemory <= 0 ? 0 : Math.round((double) usedMemory * 100 / totalMemory));
        system.put("disk", totalDisk <= 0 ? 0 : Math.round((double) usedDisk * 100 / totalDisk));
        system.put("network", loadNetworkMetric());
        return system;
    }

    private List<Map<String, Object>> buildServiceStatus() {
        List<Map<String, Object>> services = new ArrayList<>();
        HttpClient client = HttpClient.newBuilder().build();
        for (String serviceName : loadMonitoredServiceNames(client)) {
            Map<String, Object> service = new HashMap<>();
            service.put("name", serviceName);
            service.put("responseTime", 0);
            service.put("qps", 0);
            service.put("healthyInstances", 0L);
            service.put("instanceDetails", Collections.emptyList());
            try {
                String url = nacosAddr + "/nacos/v1/ns/instance/list?serviceName=" +
                        URLEncoder.encode(serviceName, StandardCharsets.UTF_8) +
                        "&groupName=" + nacosGroup +
                        "&namespaceId=" + nacosNamespace;
                HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode hostsNode = root.path("hosts");
                int instanceCount = hostsNode.isArray() ? hostsNode.size() : 0;
                long healthyCount = 0;
                if (hostsNode.isArray()) {
                    List<Map<String, Object>> instanceDetails = new ArrayList<>();
                    String podName = resolveMonitoringPodName(serviceName);
                    String containerName = resolveMonitoringContainerName(serviceName);
                    for (JsonNode host : hostsNode) {
                        Map<String, Object> instance = new LinkedHashMap<>();
                        instance.put("ip", host.path("ip").asText(""));
                        instance.put("port", host.path("port").asInt());
                        instance.put("healthy", host.path("healthy").asBoolean(false));
                        instance.put("enabled", host.path("enabled").asBoolean(true));
                        instance.put("weight", host.path("weight").asDouble(1.0));
                        instance.put("clusterName", host.path("clusterName").asText(""));
                        instance.put("namespace", firstNonEmpty(k8sCleanupNamespace, nacosNamespace, "default"));
                        instance.put("podName", podName);
                        instance.put("containerName", containerName);
                        instanceDetails.add(instance);
                        if (host.path("healthy").asBoolean(false)) {
                            healthyCount++;
                        }
                    }
                    service.put("instanceDetails", instanceDetails);
                }
                service.put("instances", instanceCount);
                service.put("healthyInstances", healthyCount);
                service.put("status", healthyCount > 0 ? "online" : "offline");
                if (healthyCount > 0 && hostsNode.isArray()) {
                    for (JsonNode host : hostsNode) {
                        if (!host.path("healthy").asBoolean(false)) {
                            continue;
                        }
                        long responseTime = measureTcpConnectMillis(host.path("ip").asText(), host.path("port").asInt());
                        if (responseTime >= 0) {
                            service.put("responseTime", responseTime);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("load monitoring service status failed, service={}", serviceName, e);
                service.put("instances", 0);
                service.put("healthyInstances", 0L);
                service.put("instanceDetails", Collections.emptyList());
                service.put("status", "offline");
            }
            services.add(service);
        }
        return services;
    }

    private String resolveMonitoringPodName(String serviceName) {
        String normalized = normalizeNacosServiceName(serviceName);
        if (normalized.startsWith("rk-server-")) {
            return normalized + "-0";
        }
        return "rk-server-" + normalized + "-0";
    }

    private String resolveMonitoringContainerName(String serviceName) {
        String normalized = normalizeNacosServiceName(serviceName);
        return normalized.startsWith("rk-server-") ? normalized.substring("rk-server-".length()) : normalized;
    }

    private String normalizeNacosServiceName(String serviceName) {
        String value = StringUtils.hasText(serviceName) ? serviceName.trim() : "rk-user";
        int groupSeparator = value.lastIndexOf("@@");
        if (groupSeparator >= 0) {
            value = value.substring(groupSeparator + 2);
        }
        return value;
    }

    private List<String> loadMonitoredServiceNames(HttpClient client) {
        try {
            String url = nacosAddr + "/nacos/v1/ns/service/list?pageNo=1&pageSize=200&namespaceId=" +
                    URLEncoder.encode(nacosNamespace, StandardCharsets.UTF_8) +
                    "&groupName=" + URLEncoder.encode(nacosGroup, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode domsNode = root.path("doms");
            if (!domsNode.isArray() || domsNode.isEmpty()) {
                return DEFAULT_MONITORED_SERVICES;
            }
            List<String> names = new ArrayList<>();
            for (JsonNode item : domsNode) {
                String serviceName = item.asText();
                if (StringUtils.hasText(serviceName)) {
                    names.add(serviceName);
                }
            }
            return names.isEmpty() ? DEFAULT_MONITORED_SERVICES : names;
        } catch (Exception e) {
            log.warn("load nacos service list failed, fallback to default monitored services", e);
            return DEFAULT_MONITORED_SERVICES;
        }
    }

    private List<Map<String, Object>> buildDatabaseStatus() {
        return List.of(
                Map.of("name", "MySQL", "status", "online"),
                Map.of("name", "Redis", "status", "online"),
                Map.of("name", "Nacos", "status", "online"),
                Map.of("name", "RabbitMQ", "status", "online")
        );
    }

    private void requireTenantOneOpsAccess() {
        Long tenantId = TenantContext.getTenantId();
        long resolvedTenantId = tenantId == null ? 1L : tenantId;
        if (resolvedTenantId != 1L && !TenantContext.isSuperAdmin()) {
            throw new IllegalStateException("Jenkins 运维能力仅对 tenant 1 开放");
        }
    }

    private String resolveWorkbenchServiceName(JsonNode root, String resourceUrl) {
        String title = root.path("info").path("title").asText("");
        if (StringUtils.hasText(title)) {
            return title;
        }
        return resourceUrl.replace("/v2/api-docs", "").replace("/", "");
    }

    private String resolveWorkbenchBasePath(JsonNode root) {
        String basePath = root.path("basePath").asText("");
        if (StringUtils.hasText(basePath)) {
            return basePath;
        }
        JsonNode serversNode = root.path("servers");
        if (serversNode.isArray() && !serversNode.isEmpty()) {
            String url = serversNode.get(0).path("url").asText("");
            if (StringUtils.hasText(url)) {
                try {
                    String path = URI.create(url).getPath();
                    if (StringUtils.hasText(path)) {
                        return path;
                    }
                } catch (Exception ignored) {
                }
                return url;
            }
        }
        return "/";
    }

    private List<AdminApiWorkbenchEndpointVO> parseApiWorkbenchEndpoints(JsonNode pathsNode, String resourceUrl) {
        if (!pathsNode.isObject()) {
            return Collections.emptyList();
        }
        List<AdminApiWorkbenchEndpointVO> endpoints = new ArrayList<>();
        pathsNode.fields().forEachRemaining(pathEntry -> {
            String path = toGatewayCallableWorkbenchPath(resourceUrl, pathEntry.getKey());
            JsonNode methodsNode = pathEntry.getValue();
            methodsNode.fields().forEachRemaining(methodEntry -> {
                AdminApiWorkbenchEndpointVO endpoint = new AdminApiWorkbenchEndpointVO();
                endpoint.setPath(path);
                endpoint.setMethod(methodEntry.getKey() == null ? "" : methodEntry.getKey().toUpperCase());
                JsonNode operationNode = methodEntry.getValue();
                endpoint.setSummary(operationNode.path("summary").asText(""));
                endpoint.setOperationId(operationNode.path("operationId").asText(""));
                endpoint.setTag(readFirstTag(operationNode.path("tags")));
                endpoint.setDeprecated(operationNode.path("deprecated").asBoolean(false));
                endpoint.setConsumes(resolveConsumes(operationNode));
                endpoint.setProduces(resolveProduces(operationNode));
                endpoints.add(endpoint);
            });
        });
        endpoints.sort(Comparator
                .comparing(AdminApiWorkbenchEndpointVO::getPath, Comparator.nullsLast(String::compareTo))
                .thenComparing(AdminApiWorkbenchEndpointVO::getMethod, Comparator.nullsLast(String::compareTo)));
        return endpoints;
    }

    private String toGatewayCallableWorkbenchPath(String resourceUrl, String path) {
        if (!StringUtils.hasText(path)) {
            return path;
        }
        if (!isNotificationsWorkbenchResource(resourceUrl) || path.startsWith("/notifications/")) {
            return path;
        }
        if (path.startsWith("/api/contact")
                || path.startsWith("/api/email/")
                || path.startsWith("/api/email-templates")) {
            return path;
        }
        return "/notifications" + (path.startsWith("/") ? path : "/" + path);
    }

    private boolean isNotificationsWorkbenchResource(String resourceUrl) {
        return StringUtils.hasText(resourceUrl) && resourceUrl.startsWith("/notifications/");
    }

    private String readFirstTag(JsonNode tagsNode) {
        if (tagsNode != null && tagsNode.isArray() && !tagsNode.isEmpty()) {
            return tagsNode.get(0).asText("");
        }
        return "";
    }

    private List<String> readStringArray(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : arrayNode) {
            String value = item.asText("");
            if (StringUtils.hasText(value)) {
                values.add(value);
            }
        }
        return values;
    }

    private List<String> resolveConsumes(JsonNode operationNode) {
        List<String> consumes = readStringArray(operationNode.path("consumes"));
        if (!consumes.isEmpty()) {
            return consumes;
        }
        return readFieldNames(operationNode.path("requestBody").path("content"));
    }

    private List<String> resolveProduces(JsonNode operationNode) {
        List<String> produces = readStringArray(operationNode.path("produces"));
        if (!produces.isEmpty()) {
            return produces;
        }
        JsonNode responsesNode = operationNode.path("responses");
        if (!responsesNode.isObject()) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        responsesNode.fields().forEachRemaining(entry -> values.addAll(readFieldNames(entry.getValue().path("content"))));
        return values.stream().distinct().collect(Collectors.toList());
    }

    private List<String> readFieldNames(JsonNode objectNode) {
        if (objectNode == null || !objectNode.isObject()) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        objectNode.fieldNames().forEachRemaining(name -> {
            if (StringUtils.hasText(name)) {
                values.add(name);
            }
        });
        return values;
    }

    private String resolveCurrentAuthorizationHeader() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes)) {
            return null;
        }
        HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
        if (request == null) {
            return null;
        }
        String authorization = request.getHeader("Authorization");
        return StringUtils.hasText(authorization) ? authorization : null;
    }

    private List<AdminJenkinsJobVO> mapJenkinsJobs(JsonNode jobsNode) {
        if (!jobsNode.isArray()) {
            return Collections.emptyList();
        }
        List<AdminJenkinsJobVO> jobs = new ArrayList<>();
        for (JsonNode row : jobsNode) {
            AdminJenkinsJobVO item = new AdminJenkinsJobVO();
            item.setName(row.path("name").asText(""));
            item.setUrl(row.path("url").asText(""));
            item.setColor(row.path("color").asText(""));
            item.setStatus(resolveJenkinsJobStatus(item.getColor()));
            if (!row.path("lastBuild").isMissingNode() && !row.path("lastBuild").isNull()) {
                item.setLastBuildNumber(row.path("lastBuild").path("number").asInt());
                item.setLastBuildUrl(row.path("lastBuild").path("url").asText(""));
            }
            if (!row.path("lastCompletedBuild").isMissingNode() && !row.path("lastCompletedBuild").isNull()) {
                item.setLastCompletedBuildNumber(row.path("lastCompletedBuild").path("number").asInt());
            }
            jobs.add(item);
        }
        return jobs;
    }

    private String resolveJenkinsJobStatus(String color) {
        String value = StringUtils.hasText(color) ? color.toLowerCase() : "";
        if (value.endsWith("_anime")) {
            return "running";
        }
        if (value.startsWith("blue") || value.startsWith("green")) {
            return "success";
        }
        if (value.startsWith("red")) {
            return "failed";
        }
        if (value.startsWith("disabled") || value.startsWith("grey") || value.startsWith("gray")) {
            return "disabled";
        }
        return "unknown";
    }

    private Map<Long, String> loadTaskGroupMap() throws Exception {
        JsonNode root = invokeXxlForm("jobgroup/pageList", Map.of(
                "start", "0",
                "length", "100",
                "appname", "",
                "title", ""
        ));
        Map<Long, String> result = new HashMap<>();
        for (JsonNode item : root.path("data")) {
            result.put(item.path("id").asLong(), item.path("title").asText(item.path("appname").asText()));
        }
        return result;
    }

    private List<AdminTaskVO> mapTaskRows(JsonNode rows, Map<Long, String> groupMap) {
        List<AdminTaskVO> tasks = new ArrayList<>();
        for (JsonNode row : rows) {
            AdminTaskVO item = new AdminTaskVO();
            item.setId(row.path("id").asLong());
            item.setName(row.path("jobDesc").asText());
            item.setGroupName(groupMap.getOrDefault(row.path("jobGroup").asLong(), "DEFAULT"));
            item.setScheduleType(row.path("scheduleType").asText());
            item.setCron(row.path("scheduleConf").asText());
            item.setHandler(row.path("executorHandler").asText());
            item.setStatus(row.path("triggerStatus").asInt() == 1 ? "running" : "paused");
            item.setLastExecuteTime(formatEpochMillis(row.path("triggerLastTime").asLong()));
            item.setNextExecuteTime(formatEpochMillis(row.path("triggerNextTime").asLong()));
            item.setAuthor(row.path("author").asText());
            tasks.add(item);
        }
        return tasks;
    }

    private List<AdminTaskLogVO> mapLogRows(JsonNode rows, List<AdminTaskVO> tasks) {
        Map<Long, String> taskNameMap = tasks.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(AdminTaskVO::getId, AdminTaskVO::getName, (a, b) -> a));
        List<AdminTaskLogVO> logs = new ArrayList<>();
        for (JsonNode row : rows) {
            AdminTaskLogVO item = new AdminTaskLogVO();
            item.setId(row.path("id").asLong());
            item.setTaskId(row.path("jobId").asLong());
            item.setTaskName(taskNameMap.getOrDefault(item.getTaskId(), "任务#" + item.getTaskId()));
            item.setExecuteTime(row.path("triggerTime").asText());
            item.setDuration(0L);
            item.setStatus(resolveLogStatus(row));
            item.setMessage(row.path("triggerMsg").asText(""));
            logs.add(item);
        }
        return logs;
    }

    private String resolveLogStatus(JsonNode row) {
        int triggerCode = row.path("triggerCode").asInt(-1);
        int handleCode = row.path("handleCode").asInt(-1);
        if (triggerCode == 200 && (handleCode == 0 || handleCode == 200)) {
            return "success";
        }
        if (triggerCode == 200 && handleCode == -1) {
            return "running";
        }
        return "failed";
    }

    private boolean invokeXxlAction(String path, Map<String, String> form) {
        try {
            JsonNode root = invokeXxlForm(path, form);
            return root.path("code").asInt() == 200;
        } catch (Exception e) {
            log.error("invoke xxl action failed, path={}", path, e);
            throw new IllegalStateException("定时任务操作失败: " + e.getMessage(), e);
        }
    }

    private JsonNode invokeXxlForm(String path, Map<String, String> form) throws Exception {
        CookieManager cookieManager = new CookieManager();
        HttpClient client = HttpClient.newBuilder().cookieHandler(cookieManager).build();
        sendForm(client, xxlAdminUrl + "/login", Map.of("userName", xxlUsername, "password", xxlPassword));
        String body = sendForm(client, xxlAdminUrl + "/" + path, form);
        return objectMapper.readTree(body);
    }

    private Map<String, String> loadJenkinsCrumbHeaders(HttpClient client) {
        try {
            HttpResponse<String> response = sendJenkinsRequest(client, "/crumbIssuer/api/json", "GET", Map.of(), null);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Map.of();
            }
            JsonNode root = objectMapper.readTree(response.body());
            String crumbField = root.path("crumbRequestField").asText("");
            String crumbValue = root.path("crumb").asText("");
            if (!StringUtils.hasText(crumbField) || !StringUtils.hasText(crumbValue)) {
                return Map.of();
            }
            return Map.of(crumbField, crumbValue);
        } catch (Exception e) {
            log.warn("load jenkins crumb failed, continue without crumb", e);
            return Map.of();
        }
    }

    private AdminJenkinsGitSourceVO buildJenkinsGitSource(String source, String defaultSource) {
        AdminJenkinsGitSourceVO item = new AdminJenkinsGitSourceVO();
        item.setSource(source);
        item.setLabel(resolveJenkinsGitSourceLabel(source));
        item.setRepositoryUrl(maskRepositoryUrl(resolveJenkinsGitRepoUrl(source)));
        item.setDefaultBranch(StringUtils.hasText(jenkinsGitDefaultBranch) ? jenkinsGitDefaultBranch.trim() : "cloud-master-new");
        item.setDefaultSource(Objects.equals(source, defaultSource));
        return item;
    }

    private String normalizeJenkinsGitSource(String source) {
        String value = StringUtils.hasText(source) ? source.trim().toLowerCase() : jenkinsGitDefaultSource;
        if (!StringUtils.hasText(value)) {
            value = "local";
        }
        if ("gogs".equals(value)) {
            return "local";
        }
        if ("origin".equals(value)) {
            return "gitee";
        }
        if ("local".equals(value) || "gitee".equals(value)) {
            return value;
        }
        throw new BadRequestException("GIT_SOURCE 只支持 local 或 gitee");
    }

    private String resolveJenkinsGitSourceLabel(String source) {
        return "gitee".equalsIgnoreCase(source) ? "Gitee" : "本地 Gogs";
    }

    private String resolveJenkinsGitRepoUrl(String source) {
        if ("gitee".equalsIgnoreCase(source)) {
            return sanitizeOpsUrl(jenkinsGitGiteeUrl);
        }
        return sanitizeOpsUrl(jenkinsGitLocalUrl);
    }

    private String resolveJenkinsGitBranchRepoUrl(String source) {
        if ("gitee".equalsIgnoreCase(source)) {
            return sanitizeOpsUrl(jenkinsGitGiteeUrl);
        }
        if (StringUtils.hasText(jenkinsGitLocalBranchUrl)) {
            return sanitizeOpsUrl(jenkinsGitLocalBranchUrl);
        }
        return sanitizeOpsUrl(jenkinsGitLocalUrl);
    }

    private String resolveJenkinsGitUsername(String source) {
        return "gitee".equalsIgnoreCase(source) ? jenkinsGitGiteeUsername : jenkinsGitLocalUsername;
    }

    private String resolveJenkinsGitPassword(String source) {
        return "gitee".equalsIgnoreCase(source) ? jenkinsGitGiteePassword : jenkinsGitLocalPassword;
    }

    private List<String> readGitBranchesFromSmartHttp(String repoUrl, String username, String password) throws Exception {
        String refsUrl = sanitizeOpsUrl(repoUrl) + "/info/refs?service=git-upload-pack";
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(refsUrl))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/x-git-upload-pack-advertisement")
                .header("User-Agent", "RK-Web-Ops")
                .GET();
        if (StringUtils.hasText(username) || StringUtils.hasText(password)) {
            builder.header("Authorization", buildBasicAuthHeader(username, password));
        }
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
        HttpResponse<byte[]> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Git HTTP 返回状态: " + response.statusCode());
        }
        List<String> branches = parseSmartHttpBranches(response.body());
        if (branches.isEmpty()) {
            throw new IllegalStateException("Git HTTP 未返回分支");
        }
        return branches;
    }

    private List<String> resolveJenkinsFallbackBranches() {
        return List.of((jenkinsGitFallbackBranches == null ? "" : jenkinsGitFallbackBranches).split(",")).stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(branch -> branch.matches("[A-Za-z0-9._/-]+"))
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> readGitBranchesFromProcess(String repoUrl, String username, String password) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("git");
        if (StringUtils.hasText(username) || StringUtils.hasText(password)) {
            command.add("-c");
            command.add("http.extraHeader=Authorization: " + buildBasicAuthHeader(username, password));
        }
        command.add("ls-remote");
        command.add("--heads");
        command.add(repoUrl);
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        boolean finished = process.waitFor(15, TimeUnit.SECONDS);
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("git ls-remote 执行超时");
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException(StringUtils.hasText(output) ? output.trim() : "git ls-remote 执行失败");
        }
        List<String> branches = parseLsRemoteBranches(output);
        if (branches.isEmpty()) {
            throw new IllegalStateException("git ls-remote 未返回分支");
        }
        return branches;
    }

    private List<String> parseSmartHttpBranches(byte[] body) {
        String text = new String(body == null ? new byte[0] : body, StandardCharsets.UTF_8);
        TreeSet<String> branches = new TreeSet<>();
        int index = 0;
        while (index + 4 <= text.length()) {
            String header = text.substring(index, index + 4);
            if (!header.matches("[0-9a-fA-F]{4}")) {
                break;
            }
            int packetLength = Integer.parseInt(header, 16);
            index += 4;
            if (packetLength == 0) {
                continue;
            }
            int payloadLength = packetLength - 4;
            if (payloadLength < 0 || index + payloadLength > text.length()) {
                break;
            }
            addGitRefBranches(text.substring(index, index + payloadLength), branches);
            index += payloadLength;
        }
        return new ArrayList<>(branches);
    }

    private List<String> parseLsRemoteBranches(String output) {
        TreeSet<String> branches = new TreeSet<>();
        addGitRefBranches(output, branches);
        return new ArrayList<>(branches);
    }

    private void addGitRefBranches(String refsText, TreeSet<String> branches) {
        if (!StringUtils.hasText(refsText)) {
            return;
        }
        for (String rawLine : refsText.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
            String line = rawLine;
            int nullIndex = line.indexOf('\0');
            if (nullIndex >= 0) {
                line = line.substring(0, nullIndex);
            }
            line = line.trim();
            if (!StringUtils.hasText(line) || line.startsWith("#")) {
                continue;
            }
            String[] columns = line.split("\\s+");
            if (columns.length < 2 || !columns[1].startsWith("refs/heads/")) {
                continue;
            }
            String branch = columns[1].substring("refs/heads/".length());
            if (branch.matches("[A-Za-z0-9._/-]+")) {
                branches.add(branch);
            }
        }
    }

    private String maskRepositoryUrl(String repoUrl) {
        if (!StringUtils.hasText(repoUrl)) {
            return "";
        }
        return repoUrl.replaceFirst("(?i)(https?://)([^/@:]+):([^/@]+)@", "$1***:***@")
                .replaceFirst("(?i)(https?://)([^/@]+)@", "$1***@");
    }

    private AdminJenkinsBuildRequestDTO normalizeJenkinsBuildRequest(AdminJenkinsBuildRequestDTO dto) {
        AdminJenkinsBuildRequestDTO normalized = new AdminJenkinsBuildRequestDTO();
        String targetBranch = dto == null ? null : dto.getTargetBranch();
        String gitSource = dto == null ? null : dto.getGitSource();
        String services = dto == null ? null : dto.getServices();
        String imageMode = dto == null ? null : dto.getImageMode();
        String deployJobSuffix = dto == null ? null : dto.getDeployJobSuffix();
        String remoteHost = dto == null ? null : dto.getRemoteHost();
        String remotePort = dto == null ? null : dto.getRemotePort();
        String remoteUser = dto == null ? null : dto.getRemoteUser();
        String remotePassword = dto == null ? null : dto.getRemotePassword();
        normalized.setTargetBranch(validateJenkinsToken(
                StringUtils.hasText(targetBranch) ? targetBranch.trim() : "cloud-master-new",
                "TARGET_BRANCH",
                "[A-Za-z0-9._/-]+"
        ));
        normalized.setGitSource(normalizeJenkinsGitSource(gitSource));
        normalized.setServices(validateJenkinsToken(
                StringUtils.hasText(services) ? normalizeServiceList(services) : "rk-user",
                "SERVICES",
                "[A-Za-z0-9_,.-]+"
        ));
        String mode = StringUtils.hasText(imageMode) ? imageMode.trim().toLowerCase() : "local";
        if (!"local".equals(mode) && !"registry".equals(mode)) {
            throw new BadRequestException("RK_K8S_IMAGE_MODE 只支持 local 或 registry");
        }
        normalized.setImageMode(mode);
        normalized.setDeployJobSuffix(validateJenkinsToken(
                StringUtils.hasText(deployJobSuffix) ? deployJobSuffix.trim() : "",
                "DEPLOY_JOB_SUFFIX",
                "[A-Za-z0-9_.-]*"
        ));
        normalized.setRemoteHost(validateJenkinsToken(
                StringUtils.hasText(remoteHost) ? remoteHost.trim() : "",
                "RK_REMOTE_HOST",
                "[A-Za-z0-9_.:-]*"
        ));
        normalized.setRemotePort(validateJenkinsToken(
                StringUtils.hasText(remotePort) ? remotePort.trim() : "",
                "RK_REMOTE_PORT",
                "[0-9]*"
        ));
        normalized.setRemoteUser(validateJenkinsToken(
                StringUtils.hasText(remoteUser) ? remoteUser.trim() : "root",
                "RK_REMOTE_USER",
                "[A-Za-z0-9_.-]+"
        ));
        normalized.setRemotePassword(remotePassword == null ? "" : remotePassword);
        return normalized;
    }

    private String normalizeServiceList(String services) {
        String value = List.of(services.split(",")).stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining(","));
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException("SERVICES 不能为空");
        }
        return value;
    }

    private String validateJenkinsToken(String value, String field, String regex) {
        String text = value == null ? "" : value;
        if (!text.matches(regex)) {
            throw new BadRequestException(field + " 包含不支持的字符");
        }
        return text;
    }

    private Map<String, String> withContentType(Map<String, String> headers, String contentType) {
        Map<String, String> merged = new LinkedHashMap<>(headers);
        merged.put("Content-Type", contentType);
        return merged;
    }

    private String encodeForm(Map<String, String> form) {
        return form.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" +
                        URLEncoder.encode(entry.getValue() == null ? "" : entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private String extractJenkinsQueueId(String queueUrl) {
        if (!StringUtils.hasText(queueUrl)) {
            return "";
        }
        String marker = "/queue/item/";
        int start = queueUrl.indexOf(marker);
        if (start < 0) {
            return "";
        }
        int idStart = start + marker.length();
        int idEnd = queueUrl.indexOf('/', idStart);
        return idEnd > idStart ? queueUrl.substring(idStart, idEnd) : queueUrl.substring(idStart);
    }

    private JsonNode readJenkinsJson(HttpClient client, String path) throws Exception {
        HttpResponse<String> response = sendJenkinsRequest(client, path, "GET", Map.of(), null);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new JenkinsHttpStatusException(path, response.statusCode(), response.body());
        }
        return objectMapper.readTree(response.body());
    }

    private AdminJenkinsBuildStatusVO buildMissingJenkinsStatus(
            String jobName,
            String queueId,
            Integer buildNumber,
            JenkinsHttpStatusException cause
    ) {
        AdminJenkinsBuildStatusVO result = new AdminJenkinsBuildStatusVO();
        result.setJobName(jobName);
        result.setQueueId(queueId);
        result.setBuildNumber(buildNumber);
        result.setBuilding(false);
        result.setStatus("failed");
        result.setResult("NOT_FOUND");
        result.setDurationMillis(0L);
        result.setEstimatedDurationMillis(0L);
        result.setGitSource(jenkinsGitDefaultSource);
        result.setGitSourceLabel(resolveJenkinsGitSourceLabel(jenkinsGitDefaultSource));
        result.setTargetBranch(jenkinsGitDefaultBranch);
        result.setImageMode("local");
        result.setImageModeLabel(resolveJenkinsImageModeLabel("local"));
        String message = "Jenkins 构建 #" + (buildNumber == null ? "unknown" : buildNumber)
                + " 在当前 Jenkins " + sanitizeOpsUrl(jenkinsUrl)
                + " 中不存在，请确认该构建号是否来自另一个 Jenkins 地址。";
        if (StringUtils.hasText(queueId)) {
            message = message + " queueId=" + queueId + ".";
        }
        result.setMessage(message);
        result.setLogTail(message + "\nrequestPath=" + cause.getPath() + "\nhttpStatus=" + cause.getStatusCode());
        return result;
    }

    private void fillJenkinsBuildDetail(
            HttpClient client,
            AdminJenkinsBuildStatusVO result,
            String jobName,
            Integer buildNumber,
            Integer tailLines
    ) throws Exception {
        if (buildNumber == null || buildNumber <= 0) {
            throw new BadRequestException("Jenkins buildNumber 无效");
        }
        String buildPath = "/job/" + encodePathSegment(jobName) + "/" + buildNumber;
        JsonNode build = readJenkinsJson(client, buildPath + "/api/json?tree=number,url,result,building,duration,estimatedDuration,timestamp,actions[parameters[name,value]]");
        result.setBuildNumber(build.path("number").asInt(buildNumber));
        result.setBuildUrl(build.path("url").asText(result.getBuildUrl()));
        boolean building = build.path("building").asBoolean(false);
        String buildResult = build.path("result").asText("");
        result.setBuilding(building);
        result.setResult(buildResult);
        result.setStatus(resolveJenkinsBuildStatus(building, buildResult));
        result.setDurationMillis(build.path("duration").asLong(0L));
        result.setEstimatedDurationMillis(build.path("estimatedDuration").asLong(0L));
        fillJenkinsBuildParameters(result, build.path("actions"));
        String consoleText = readJenkinsConsoleText(client, buildPath + "/consoleText");
        result.setLogTail(tailText(consoleText, Math.max(1, Math.min(tailLines == null ? 120 : tailLines, 500))));
        result.setMessage(building ? "Jenkins 构建执行中" : "Jenkins 构建已结束: " + (StringUtils.hasText(buildResult) ? buildResult : "UNKNOWN"));
    }

    private void fillJenkinsBuildParameters(AdminJenkinsBuildStatusVO result, JsonNode actions) {
        if (!actions.isArray()) {
            return;
        }
        for (JsonNode action : actions) {
            JsonNode parameters = action.path("parameters");
            if (!parameters.isArray()) {
                continue;
            }
            for (JsonNode parameter : parameters) {
                String name = parameter.path("name").asText("");
                String value = parameter.path("value").asText("");
                if ("SERVICES".equals(name)) {
                    result.setServices(value);
                } else if ("RK_K8S_IMAGE_MODE".equals(name)) {
                    result.setImageMode(value);
                    result.setImageModeLabel(resolveJenkinsImageModeLabel(value));
                } else if ("GIT_SOURCE".equals(name)) {
                    result.setGitSource(value);
                    result.setGitSourceLabel(resolveJenkinsGitSourceLabel(value));
                } else if ("TARGET_BRANCH".equals(name)) {
                    result.setTargetBranch(value);
                } else if ("RK_REMOTE_HOST".equals(name) || "RK_REMOTE_PORT".equals(name) || "RK_REMOTE_USER".equals(name)) {
                    // SSH target metadata is intentionally not exposed in build status details.
                }
            }
        }
    }

    private String readJenkinsConsoleText(HttpClient client, String path) {
        try {
            HttpResponse<String> response = sendJenkinsRequest(client, path, "GET", Map.of(), null);
            return response.statusCode() >= 200 && response.statusCode() < 300 ? response.body() : "";
        } catch (Exception e) {
            log.warn("read jenkins console text failed, path={}", path, e);
            return "";
        }
    }

    private String tailText(String text, int tailLines) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        while (normalized.endsWith("\n")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        String[] lines = normalized.split("\n");
        int start = Math.max(0, lines.length - tailLines);
        return String.join("\n", List.of(lines).subList(start, lines.length));
    }

    private String limitText(String text, int maxChars) {
        if (!StringUtils.hasText(text) || text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, Math.max(0, maxChars)) + "\n...[truncated]";
    }

    private String resolveJenkinsBuildStatus(boolean building, String result) {
        if (building) {
            return "running";
        }
        String value = StringUtils.hasText(result) ? result.toUpperCase() : "";
        if ("SUCCESS".equals(value)) {
            return "success";
        }
        if ("ABORTED".equals(value)) {
            return "canceled";
        }
        if ("FAILURE".equals(value) || "UNSTABLE".equals(value)) {
            return "failed";
        }
        return "unknown";
    }

    private String resolveJenkinsImageModeLabel(String imageMode) {
        return "registry".equalsIgnoreCase(imageMode) ? "阿里云镜像仓库模式" : "本地构建直接部署";
    }

    private HttpResponse<String> sendJenkinsRequest(
            HttpClient client,
            String path,
            String method,
            Map<String, String> extraHeaders,
            String body
    ) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(sanitizeOpsUrl(jenkinsUrl) + path))
                .header("Authorization", buildBasicAuthHeader(jenkinsUsername, jenkinsPassword));
        for (Map.Entry<String, String> entry : extraHeaders.entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }
        if ("POST".equalsIgnoreCase(method)) {
            builder.POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
        } else {
            builder.GET();
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static class JenkinsHttpStatusException extends RuntimeException {
        private final String path;
        private final int statusCode;

        JenkinsHttpStatusException(String path, int statusCode, String responseBody) {
            super("Jenkins API returned status " + statusCode + " for " + path + ": " + tailForMessage(responseBody));
            this.path = path;
            this.statusCode = statusCode;
        }

        String getPath() {
            return path;
        }

        int getStatusCode() {
            return statusCode;
        }

        private static String tailForMessage(String text) {
            if (!StringUtils.hasText(text)) {
                return "";
            }
            String normalized = text.replace("\r\n", "\n").replace('\r', '\n').trim();
            return normalized.length() <= 300 ? normalized : normalized.substring(normalized.length() - 300);
        }
    }

    private String sendForm(HttpClient client, String url, Map<String, String> form) throws Exception {
        String payload = form.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" +
                        URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
    }

    private String captureJenkinsPreflightSnapshot() {
        String command = "set +e\n"
                + "echo 'preflight disk snapshot'\n"
                + "date '+time=%Y-%m-%d %H:%M:%S %z' 2>/dev/null || true\n"
                + "echo '[disk]'\n"
                + "df -h / /var/lib/docker /var/lib/containerd /var/log 2>/dev/null || df -h\n"
                + "echo '[inode]'\n"
                + "df -ih / /var/lib/docker /var/lib/containerd /var/log 2>/dev/null || df -ih\n"
                + "echo '[docker]'\n"
                + "docker system df 2>/dev/null || true\n";
        try {
            String output = runRemoteCommandLenient(command, 30);
            return limitText(output, 12000);
        } catch (Exception e) {
            return "preflight disk snapshot unavailable: " + e.getMessage();
        }
    }

    private Long insertJenkinsBuildRecord(String jobName, AdminJenkinsBuildRequestDTO dto, String queueId,
                                          String status, String stage, String failureReason, String logTail,
                                          String preflightSummary) {
        try {
            Long userId = UserContext.getUser();
            jdbcTemplate.update(
                    "INSERT INTO ops_build_record (job_name, queue_id, git_source, git_repo, branch_name, image_mode, services_text, status, stage, failure_reason, trigger_user_id, trigger_user_name, trigger_time, log_tail, preflight_summary) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?, ?)",
                    jobName,
                    emptyToNull(queueId),
                    dto.getGitSource(),
                    resolveJenkinsGitRepoUrl(dto.getGitSource()),
                    dto.getTargetBranch(),
                    dto.getImageMode(),
                    dto.getServices(),
                    status,
                    stage,
                    emptyToNull(failureReason),
                    userId,
                    userId == null ? null : String.valueOf(userId),
                    emptyToNull(logTail),
                    emptyToNull(preflightSummary)
            );
            Long recordId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            insertBuildLogRow(recordId, null, null, "PREFLIGHT_DISK", preflightSummary);
            syncBuildServiceRows(recordId, dto.getServices(), status);
            pruneSingleServiceBuildHistory(dto.getServices());
            return recordId;
        } catch (Exception e) {
            log.warn("persist jenkins build trigger record failed", e);
            return null;
        }
    }

    private void syncBuildServiceRows(Long recordId, String services, String status) {
        if (recordId == null || !StringUtils.hasText(services)) {
            return;
        }
        for (String service : services.split(",")) {
            String serviceCode = service.trim();
            if (!StringUtils.hasText(serviceCode)) {
                continue;
            }
            int updated = jdbcTemplate.update(
                    "UPDATE ops_build_service SET status = ?, is_deleted = 0 WHERE record_id = ? AND service_code = ?",
                    status,
                    recordId,
                    serviceCode
            );
            if (updated == 0) {
                jdbcTemplate.update(
                        "INSERT INTO ops_build_service (record_id, service_code, status) VALUES (?, ?, ?)",
                        recordId,
                        serviceCode,
                        status
                );
            }
        }
    }

    private void pruneSingleServiceBuildHistory(String services) {
        if (!StringUtils.hasText(services)) {
            return;
        }
        for (String service : services.split(",")) {
            String serviceCode = service.trim();
            if (!StringUtils.hasText(serviceCode)) {
                continue;
            }
            List<Map<String, Object>> staleRows = jdbcTemplate.queryForList(
                    "SELECT s.id AS service_row_id, s.record_id AS record_id FROM ops_build_service s " +
                            "JOIN ops_build_record r ON r.id = s.record_id " +
                            "WHERE r.is_deleted = 0 AND s.is_deleted = 0 AND s.service_code = ? " +
                            "ORDER BY r.trigger_time DESC, r.id DESC LIMIT 100, 10000",
                    serviceCode
            );
            for (Map<String, Object> row : staleRows) {
                Long serviceRowId = valueAsLong(row.get("service_row_id"));
                Long recordId = valueAsLong(row.get("record_id"));
                jdbcTemplate.update("UPDATE ops_build_service SET is_deleted = 1 WHERE id = ?", serviceRowId);
                jdbcTemplate.update("DELETE FROM ops_build_log WHERE record_id = ? AND service_code = ?", recordId, serviceCode);
                jdbcTemplate.update(
                        "UPDATE ops_build_record r SET r.is_deleted = 1 WHERE r.id = ? AND NOT EXISTS (" +
                                "SELECT 1 FROM ops_build_service s WHERE s.record_id = r.id AND s.is_deleted = 0)",
                        recordId
                );
            }
        }
    }

    private void persistJenkinsBuildStatus(AdminJenkinsBuildStatusVO status) {
        if (status == null) {
            return;
        }
        try {
            Long recordId = findJenkinsBuildRecordId(status);
            if (recordId == null) {
                AdminJenkinsBuildRequestDTO dto = new AdminJenkinsBuildRequestDTO();
                dto.setTargetBranch(StringUtils.hasText(status.getTargetBranch()) ? status.getTargetBranch() : jenkinsGitDefaultBranch);
                dto.setGitSource(StringUtils.hasText(status.getGitSource()) ? status.getGitSource() : jenkinsGitDefaultSource);
                dto.setServices(StringUtils.hasText(status.getServices()) ? status.getServices() : "rk-user");
                dto.setImageMode(StringUtils.hasText(status.getImageMode()) ? status.getImageMode() : "local");
                dto.setDeployJobSuffix("");
                recordId = insertJenkinsBuildRecord(status.getJobName(), normalizeJenkinsBuildRequest(dto), status.getQueueId(), status.getStatus(), "POLL", null, status.getLogTail(), status.getPreflightSummary());
            }
            if (recordId == null) {
                return;
            }
            boolean finished = !Boolean.TRUE.equals(status.getBuilding()) && !"queued".equals(status.getStatus());
            jdbcTemplate.update(
                    "UPDATE ops_build_record SET build_number = ?, build_url = COALESCE(NULLIF(?, ''), build_url), git_source = COALESCE(NULLIF(?, ''), git_source), branch_name = COALESCE(NULLIF(?, ''), branch_name), image_mode = COALESCE(NULLIF(?, ''), image_mode), services_text = COALESCE(NULLIF(?, ''), services_text), status = ?, stage = ?, result = ?, failure_reason = ?, start_time = COALESCE(start_time, NOW()), finish_time = CASE WHEN ? = 1 THEN NOW() ELSE finish_time END, duration_ms = ?, log_tail = ?, update_time = NOW() WHERE id = ?",
                    status.getBuildNumber(),
                    status.getBuildUrl(),
                    status.getGitSource(),
                    status.getTargetBranch(),
                    status.getImageMode(),
                    status.getServices(),
                    status.getStatus(),
                    Boolean.TRUE.equals(status.getBuilding()) ? "BUILDING" : "FINISHED",
                    status.getResult(),
                    "failed".equals(status.getStatus()) ? firstNonEmpty(status.getMessage(), tailText(status.getLogTail(), 20)) : null,
                    finished ? 1 : 0,
                    status.getDurationMillis() == null ? 0L : status.getDurationMillis(),
                    emptyToNull(status.getLogTail()),
                    recordId
            );
            syncBuildServiceRows(recordId, status.getServices(), status.getStatus());
            insertBuildLogRows(recordId, status);
            if (finished) {
                pruneSingleServiceBuildHistory(status.getServices());
            }
        } catch (Exception e) {
            log.warn("persist jenkins build status failed", e);
        }
    }

    private void insertBuildLogRows(Long recordId, AdminJenkinsBuildStatusVO status) {
        String logTail = emptyToNull(status.getLogTail());
        if (!StringUtils.hasText(status.getServices())) {
            insertBuildLogRow(recordId, null, status.getBuildNumber(), "CONSOLE_TAIL", logTail);
            return;
        }
        for (String service : status.getServices().split(",")) {
            String serviceCode = service.trim();
            if (!StringUtils.hasText(serviceCode)) {
                continue;
            }
            insertBuildLogRow(recordId, serviceCode, status.getBuildNumber(), "CONSOLE_TAIL", logTail);
        }
    }

    private void insertBuildLogRow(Long recordId, String serviceCode, Integer buildNumber, String logType, String content) {
        if (recordId == null || !StringUtils.hasText(content)) {
            return;
        }
        jdbcTemplate.update(
                "DELETE FROM ops_build_log WHERE record_id = ? AND log_type = ? AND ((service_code = ?) OR (service_code IS NULL AND ? IS NULL))",
                recordId,
                logType,
                serviceCode,
                serviceCode
        );
        jdbcTemplate.update(
                "INSERT INTO ops_build_log (record_id, service_code, build_number, log_type, content, line_from, line_to) VALUES (?, ?, ?, ?, ?, 0, 0)",
                recordId,
                serviceCode,
                buildNumber,
                logType,
                content
        );
    }

    private Long insertOperationAudit(String operationType, String targetNamespace, String targetKind, String targetName,
                                      String action, String status, String reason, String requestPayload, String resultOutput) {
        try {
            Long userId = UserContext.getUser();
            jdbcTemplate.update(
                    "INSERT INTO ops_operation_audit (operation_type, target_namespace, target_kind, target_name, action, status, reason, request_payload, result_output, operator_user_id, operator_user_name) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    operationType,
                    emptyToNull(targetNamespace),
                    emptyToNull(targetKind),
                    emptyToNull(targetName),
                    emptyToNull(action),
                    emptyToNull(status),
                    emptyToNull(reason),
                    emptyToNull(requestPayload),
                    emptyToNull(limitText(resultOutput, 12000)),
                    userId,
                    userId == null ? null : String.valueOf(userId)
            );
            return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        } catch (Exception e) {
            log.warn("persist operation audit failed, operationType={}, target={}", operationType, targetName, e);
            return null;
        }
    }

    private Long recordOperationAudit(String operationType, String targetNamespace, String targetKind, String targetName,
                                      String action, String status, String reason, String requestPayload, String resultOutput) {
        return insertOperationAudit(operationType, targetNamespace, targetKind, targetName,
                action, status, reason, requestPayload, resultOutput);
    }

    private Long findJenkinsBuildRecordId(AdminJenkinsBuildStatusVO status) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (StringUtils.hasText(status.getQueueId())) {
            rows = jdbcTemplate.queryForList(
                    "SELECT id FROM ops_build_record WHERE is_deleted = 0 AND queue_id = ? ORDER BY id DESC LIMIT 1",
                    status.getQueueId()
            );
        }
        if (rows.isEmpty() && status.getBuildNumber() != null) {
            rows = jdbcTemplate.queryForList(
                    "SELECT id FROM ops_build_record WHERE is_deleted = 0 AND job_name = ? AND build_number = ? ORDER BY id DESC LIMIT 1",
                    status.getJobName(),
                    status.getBuildNumber()
            );
        }
        if (rows.isEmpty() && status.getBuildNumber() != null) {
            rows = jdbcTemplate.queryForList(
                    "SELECT id FROM ops_build_record WHERE is_deleted = 0 AND job_name = ? AND queue_id IS NOT NULL AND build_number IS NULL AND status IN ('queued', 'running') ORDER BY trigger_time DESC, id DESC LIMIT 1",
                    status.getJobName()
            );
        }
        return rows.isEmpty() ? null : valueAsLong(rows.get(0).get("id"));
    }

    private Integer findJenkinsBuildNumberByQueueId(String jobName, String queueId) {
        return findJenkinsBuildNumberByQueueId(jobName, queueId, null);
    }

    private Integer findJenkinsBuildNumberByQueueId(String jobName, String queueId, HttpClient client) {
        if (!StringUtils.hasText(jobName) || !StringUtils.hasText(queueId)) {
            return null;
        }
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT build_number FROM ops_build_record WHERE is_deleted = 0 AND job_name = ? AND queue_id = ? AND build_number > 0 ORDER BY id DESC LIMIT 1",
                    jobName,
                    queueId
            );
            if (!rows.isEmpty()) {
                return valueAsNullableInteger(rows.get(0).get("build_number"));
            }
            rows = jdbcTemplate.queryForList(
                    "SELECT l.build_number FROM ops_build_log l INNER JOIN ops_build_record r ON r.id = l.record_id WHERE r.is_deleted = 0 AND r.job_name = ? AND r.queue_id = ? AND l.build_number > 0 ORDER BY l.id DESC LIMIT 1",
                    jobName,
                    queueId
            );
            if (!rows.isEmpty()) {
                return valueAsNullableInteger(rows.get(0).get("build_number"));
            }
            Integer recentBuildNumber = findRecentJenkinsBuildNumberByQueueId(jobName, queueId, client);
            return recentBuildNumber != null && recentBuildNumber > 0 ? recentBuildNumber : null;
        } catch (Exception e) {
            log.warn("find jenkins build number by queue id failed, jobName={}, queueId={}", jobName, queueId, e);
            return null;
        }
    }

    private Integer findRecentJenkinsBuildNumberByQueueId(String jobName, String queueId, HttpClient client) {
        if (!StringUtils.hasText(jobName) || !StringUtils.hasText(queueId)) {
            return null;
        }
        HttpClient resolvedClient = client == null ? HttpClient.newBuilder().build() : client;
        try {
            JsonNode job = readJenkinsJson(resolvedClient, "/job/" + encodePathSegment(jobName) + "/api/json?tree=builds[number,queueId,actions[causes[queueId],parameters[name,value]]]");
            JsonNode builds = job.path("builds");
            if (!builds.isArray()) {
                return null;
            }
            for (JsonNode build : builds) {
                if (matchesJenkinsQueueId(build, queueId)) {
                    int number = build.path("number").asInt(0);
                    return number > 0 ? number : null;
                }
            }
        } catch (Exception e) {
            log.warn("find recent jenkins build number by queue id failed, jobName={}, queueId={}", jobName, queueId, e);
        }
        return null;
    }

    private boolean matchesJenkinsQueueId(JsonNode build, String queueId) {
        if (build == null || !StringUtils.hasText(queueId)) {
            return false;
        }
        if (queueId.equals(build.path("queueId").asText(""))) {
            return true;
        }
        JsonNode actions = build.path("actions");
        if (!actions.isArray()) {
            return false;
        }
        for (JsonNode action : actions) {
            JsonNode causes = action.path("causes");
            if (causes.isArray()) {
                for (JsonNode cause : causes) {
                    if (queueId.equals(cause.path("queueId").asText(""))) {
                        return true;
                    }
                }
            }
            JsonNode parameters = action.path("parameters");
            if (parameters.isArray()) {
                for (JsonNode parameter : parameters) {
                    if ("QUEUE_ID".equals(parameter.path("name").asText("")) && queueId.equals(parameter.path("value").asText(""))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void reconcileRecentJenkinsBuildRecords() {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id, job_name, build_number, queue_id FROM ops_build_record WHERE is_deleted = 0 AND status IN ('queued', 'running') ORDER BY trigger_time DESC, id DESC LIMIT 10"
            );
            if (rows.isEmpty()) {
                return;
            }
            HttpClient client = HttpClient.newBuilder().build();
            for (Map<String, Object> row : rows) {
                reconcileOneJenkinsBuildRecord(client, row);
            }
        } catch (Exception e) {
            log.warn("reconcile recent jenkins build records failed", e);
        }
    }

    private void reconcileOneJenkinsBuildRecord(HttpClient client, Map<String, Object> row) {
        String jobName = valueAsString(row.get("job_name"));
        String queueId = valueAsString(row.get("queue_id"));
        Integer buildNumber = valueAsNullableInteger(row.get("build_number"));
        if (!StringUtils.hasText(jobName)) {
            return;
        }
        try {
            Integer resolvedBuildNumber = buildNumber;
            if ((resolvedBuildNumber == null || resolvedBuildNumber <= 0) && StringUtils.hasText(queueId)) {
                resolvedBuildNumber = findJenkinsBuildNumberByQueueId(jobName, queueId, client);
            }
            if ((resolvedBuildNumber == null || resolvedBuildNumber <= 0) && StringUtils.hasText(queueId)) {
                AdminJenkinsBuildStatusVO queuedStatus = resolveJenkinsQueuedBuild(client, jobName, queueId);
                if (queuedStatus == null || queuedStatus.getBuildNumber() == null || queuedStatus.getBuildNumber() <= 0) {
                    return;
                }
                persistJenkinsBuildStatus(queuedStatus);
                return;
            }
            if (resolvedBuildNumber == null || resolvedBuildNumber <= 0) {
                return;
            }
            AdminJenkinsBuildStatusVO status = new AdminJenkinsBuildStatusVO();
            status.setJobName(jobName);
            status.setQueueId(queueId);
            fillJenkinsBuildDetail(client, status, jobName, resolvedBuildNumber, 120);
            persistJenkinsBuildStatus(status);
        } catch (JenkinsHttpStatusException e) {
            if (e.getStatusCode() == 404) {
                AdminJenkinsBuildStatusVO missing = buildMissingJenkinsStatus(jobName, queueId, buildNumber, e);
                persistJenkinsBuildStatus(missing);
            } else {
                log.warn("reconcile jenkins build record failed, jobName={}, queueId={}, buildNumber={}", jobName, queueId, buildNumber, e);
            }
        } catch (Exception e) {
            log.warn("reconcile jenkins build record failed, jobName={}, queueId={}, buildNumber={}", jobName, queueId, buildNumber, e);
        }
    }

    private AdminJenkinsBuildStatusVO resolveJenkinsQueuedBuild(HttpClient client, String jobName, String queueId) throws Exception {
        JsonNode queueNode = readJenkinsJson(client, "/queue/item/" + encodePathSegment(queueId) + "/api/json");
        AdminJenkinsBuildStatusVO result = new AdminJenkinsBuildStatusVO();
        result.setJobName(jobName);
        result.setQueueId(queueId);
        if (queueNode.path("cancelled").asBoolean(false)) {
            result.setStatus("canceled");
            result.setBuilding(false);
            result.setMessage("Jenkins queue item was canceled");
            return result;
        }
        JsonNode executable = queueNode.path("executable");
        if (executable.isMissingNode() || executable.isNull()) {
            result.setStatus("queued");
            result.setBuilding(true);
            result.setMessage(StringUtils.hasText(queueNode.path("why").asText(""))
                    ? queueNode.path("why").asText("")
                    : "Jenkins build is queued");
            return result;
        }
        Integer resolvedBuildNumber = executable.path("number").asInt(0);
        result.setBuildUrl(executable.path("url").asText(""));
        fillJenkinsBuildDetail(client, result, jobName, resolvedBuildNumber, 120);
        return result;
    }

    private AdminJenkinsBuildRecordVO mapBuildRecordRow(Map<String, Object> row) {
        AdminJenkinsBuildRecordVO item = new AdminJenkinsBuildRecordVO();
        item.setId(valueAsLong(row.get("id")));
        item.setJobName(valueAsString(row.get("job_name")));
        item.setBuildNumber(valueAsNullableInteger(row.get("build_number")));
        item.setBuildUrl(valueAsString(row.get("build_url")));
        item.setQueueId(valueAsString(row.get("queue_id")));
        item.setGitSource(valueAsString(row.get("git_source")));
        item.setGitRepo(valueAsString(row.get("git_repo")));
        item.setBranchName(valueAsString(row.get("branch_name")));
        item.setCommitId(valueAsString(row.get("commit_id")));
        item.setImageMode(valueAsString(row.get("image_mode")));
        item.setServices(valueAsString(row.get("services_text")));
        item.setStatus(valueAsString(row.get("status")));
        item.setStage(valueAsString(row.get("stage")));
        item.setResult(valueAsString(row.get("result")));
        item.setFailureReason(valueAsString(row.get("failure_reason")));
        item.setTriggerUserId(valueAsLong(row.get("trigger_user_id")));
        item.setTriggerUserName(valueAsString(row.get("trigger_user_name")));
        item.setTriggerTime(formatDateTime(row.get("trigger_time")));
        item.setStartTime(formatDateTime(row.get("start_time")));
        item.setFinishTime(formatDateTime(row.get("finish_time")));
        item.setDurationMs(valueAsLong(row.get("duration_ms")));
        item.setLogTail(valueAsString(row.get("log_tail")));
        item.setPreflightSummary(valueAsString(row.get("preflight_summary")));
        return item;
    }

    private AdminOpsAuditLogVO mapAuditLogRow(Map<String, Object> row) {
        AdminOpsAuditLogVO item = new AdminOpsAuditLogVO();
        item.setId(valueAsLong(row.get("id")));
        item.setOperationType(valueAsString(row.get("operation_type")));
        item.setTargetNamespace(valueAsString(row.get("target_namespace")));
        item.setTargetKind(valueAsString(row.get("target_kind")));
        item.setTargetName(valueAsString(row.get("target_name")));
        item.setAction(valueAsString(row.get("action")));
        item.setStatus(valueAsString(row.get("status")));
        item.setReason(valueAsString(row.get("reason")));
        item.setRequestPayload(valueAsString(row.get("request_payload")));
        item.setResultOutput(valueAsString(row.get("result_output")));
        item.setOperatorUserId(valueAsLong(row.get("operator_user_id")));
        item.setOperatorUserName(valueAsString(row.get("operator_user_name")));
        item.setCreateTime(formatDateTime(row.get("create_time")));
        return item;
    }

    private AdminOpsServiceVO findServiceRegistryById(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, service_code, display_name, service_type, git_source, git_repo, branch_name, module_path, build_mode, image_name, namespace_name, workload_type, workload_name, container_name, health_check_path, resource_limits, enabled, sort_order " +
                        "FROM ops_service_registry WHERE id = ? AND is_deleted = 0",
                id
        );
        if (rows.isEmpty()) {
            throw new BadRequestException("微服务不存在");
        }
        return mapServiceRegistryRow(rows.get(0));
    }

    private AdminOpsServiceVO mapServiceRegistryRow(Map<String, Object> row) {
        AdminOpsServiceVO item = new AdminOpsServiceVO();
        item.setId(valueAsLong(row.get("id")));
        item.setServiceCode(valueAsString(row.get("service_code")));
        item.setDisplayName(valueAsString(row.get("display_name")));
        item.setServiceType(valueAsString(row.get("service_type")));
        item.setGitSource(valueAsString(row.get("git_source")));
        item.setGitRepo(valueAsString(row.get("git_repo")));
        item.setBranchName(valueAsString(row.get("branch_name")));
        item.setModulePath(valueAsString(row.get("module_path")));
        item.setBuildMode(valueAsString(row.get("build_mode")));
        item.setImageName(valueAsString(row.get("image_name")));
        item.setNamespaceName(valueAsString(row.get("namespace_name")));
        item.setWorkloadType(valueAsString(row.get("workload_type")));
        item.setWorkloadName(valueAsString(row.get("workload_name")));
        item.setContainerName(valueAsString(row.get("container_name")));
        item.setHealthCheckPath(valueAsString(row.get("health_check_path")));
        item.setResourceLimits(valueAsString(row.get("resource_limits")));
        item.setEnabled(valueAsInteger(row.get("enabled")) == 1);
        item.setSortOrder(valueAsInteger(row.get("sort_order")));
        return item;
    }

    private String resolveNacosNamespace(String namespaceId) {
        return StringUtils.hasText(namespaceId) ? namespaceId.trim() : nacosNamespace;
    }

    private String resolveNacosGroup(String groupName) {
        return StringUtils.hasText(groupName) ? groupName.trim() : nacosGroup;
    }

    private String resolveNacosConfigType(String dataId) {
        String lower = dataId == null ? "" : dataId.toLowerCase();
        if (lower.endsWith(".properties")) {
            return "properties";
        }
        if (lower.endsWith(".json")) {
            return "json";
        }
        return "yaml";
    }

    private HttpResponse<String> sendNacosRequest(HttpClient client, String path, String method, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(sanitizeOpsUrl(nacosAddr) + path))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json,text/plain,*/*");
        if ("POST".equalsIgnoreCase(method)) {
            builder.header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body, StandardCharsets.UTF_8));
        } else {
            builder.GET();
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private List<AdminK8sWorkloadVO> loadK8sWorkloadsByKind(HttpClient client, String apiUrl, String token,
                                                            String namespace, String kind) throws Exception {
        JsonNode items = getK8sJson(client, apiUrl, token,
                "/apis/apps/v1/namespaces/" + encodePathSegment(namespace) + "/" + k8sResourcePathPart(kind)).path("items");
        List<AdminK8sWorkloadVO> result = new ArrayList<>();
        for (JsonNode item : items) {
            result.add(mapK8sWorkload(namespace, kind, item));
        }
        return result;
    }

    private List<AdminK8sWorkloadVO> loadK8sWorkloadsFromPods(HttpClient client, String apiUrl, String token,
                                                              String namespace) throws Exception {
        JsonNode items = getK8sJson(client, apiUrl, token,
                "/api/v1/namespaces/" + encodePathSegment(namespace) + "/pods?limit=200").path("items");
        Map<String, AdminK8sWorkloadVO> workloads = new LinkedHashMap<>();
        Map<String, TreeSet<String>> workloadContainers = new HashMap<>();
        Map<String, TreeSet<String>> workloadImages = new HashMap<>();
        for (JsonNode pod : items) {
            JsonNode metadata = pod.path("metadata");
            String podName = metadata.path("name").asText("");
            Map<String, String> owner = resolveWorkloadOwner(metadata.path("ownerReferences"), podName);
            String kind = owner.get("kind");
            String name = owner.get("name");
            if (!StringUtils.hasText(kind) || !StringUtils.hasText(name)) {
                continue;
            }
            String key = kind + "/" + name;
            AdminK8sWorkloadVO workload = workloads.computeIfAbsent(key, ignored -> {
                AdminK8sWorkloadVO item = new AdminK8sWorkloadVO();
                item.setNamespace(namespace);
                item.setKind(kind);
                item.setName(name);
                item.setReplicas(0);
                item.setReadyReplicas(0);
                item.setAvailableReplicas(0);
                return item;
            });
            workload.setReplicas(workload.getReplicas() + 1);
            if (isK8sPodReady(pod.path("status").path("conditions"))) {
                workload.setReadyReplicas(workload.getReadyReplicas() + 1);
                workload.setAvailableReplicas(workload.getReadyReplicas());
            }
            TreeSet<String> containers = workloadContainers.computeIfAbsent(key, ignored -> new TreeSet<>());
            TreeSet<String> images = workloadImages.computeIfAbsent(key, ignored -> new TreeSet<>());
            for (JsonNode container : pod.path("spec").path("containers")) {
                String containerName = container.path("name").asText("");
                String imageName = container.path("image").asText("");
                if (StringUtils.hasText(containerName)) {
                    containers.add(containerName);
                }
                if (StringUtils.hasText(imageName)) {
                    images.add(imageName);
                }
            }
        }
        for (Map.Entry<String, AdminK8sWorkloadVO> entry : workloads.entrySet()) {
            AdminK8sWorkloadVO workload = entry.getValue();
            workload.setContainers(joinOrDash(workloadContainers.get(entry.getKey())));
            workload.setImages(joinOrDash(workloadImages.get(entry.getKey())));
            workload.setStatus(Objects.equals(workload.getReplicas(), workload.getReadyReplicas()) ? "ready" : "updating");
        }
        return new ArrayList<>(workloads.values());
    }

    private Map<String, String> resolveWorkloadOwner(JsonNode ownerReferences, String podName) {
        String ownerKind = "";
        String ownerName = "";
        for (JsonNode owner : ownerReferences) {
            if (owner.path("controller").asBoolean(false) || !StringUtils.hasText(ownerName)) {
                ownerKind = owner.path("kind").asText("");
                ownerName = owner.path("name").asText("");
            }
        }
        if ("ReplicaSet".equals(ownerKind)) {
            ownerKind = "Deployment";
            ownerName = stripReplicaSetHash(ownerName);
        }
        if (!StringUtils.hasText(ownerName) && podName.matches(".+-[0-9]+")) {
            ownerKind = "StatefulSet";
            ownerName = podName.replaceFirst("-[0-9]+$", "");
        }
        Map<String, String> result = new HashMap<>();
        result.put("kind", ownerKind);
        result.put("name", ownerName);
        return result;
    }

    private String stripReplicaSetHash(String replicaSetName) {
        if (!StringUtils.hasText(replicaSetName)) {
            return "";
        }
        String stripped = replicaSetName.replaceFirst("-[a-z0-9]{9,10}$", "");
        if (!Objects.equals(stripped, replicaSetName)) {
            return stripped;
        }
        return replicaSetName.replaceFirst("-[^-]+$", "");
    }

    private boolean isK8sPodReady(JsonNode conditions) {
        for (JsonNode condition : conditions) {
            if ("Ready".equals(condition.path("type").asText("")) &&
                    "True".equals(condition.path("status").asText(""))) {
                return true;
            }
        }
        return false;
    }

    private String joinOrDash(TreeSet<String> values) {
        return values == null || values.isEmpty() ? "-" : String.join(", ", values);
    }

    private AdminK8sWorkloadVO mapK8sWorkload(String namespace, String kind, JsonNode item) {
        AdminK8sWorkloadVO workload = new AdminK8sWorkloadVO();
        workload.setNamespace(namespace);
        workload.setKind(kind);
        workload.setName(item.path("metadata").path("name").asText(""));
        workload.setReplicas(item.path("spec").path("replicas").asInt(0));
        workload.setReadyReplicas(item.path("status").path("readyReplicas").asInt(0));
        workload.setAvailableReplicas(item.path("status").path("availableReplicas").asInt(0));
        JsonNode containers = item.path("spec").path("template").path("spec").path("containers");
        workload.setContainers(joinK8sContainerField(containers, "name"));
        workload.setImages(joinK8sContainerField(containers, "image"));
        workload.setStatus(Objects.equals(workload.getReplicas(), workload.getReadyReplicas()) ? "ready" : "updating");
        return workload;
    }

    private String joinK8sContainerField(JsonNode containers, String field) {
        List<String> values = new ArrayList<>();
        for (JsonNode container : containers) {
            String value = container.path(field).asText("");
            if (StringUtils.hasText(value)) {
                values.add(value);
            }
        }
        return values.isEmpty() ? "-" : String.join(", ", values);
    }

    private List<AdminK8sWorkloadVO> parseK8sWorkloadLines(String namespace, String output) {
        if (!StringUtils.hasText(output)) {
            return Collections.emptyList();
        }
        List<AdminK8sWorkloadVO> result = new ArrayList<>();
        for (String line : output.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (!StringUtils.hasText(trimmed)) {
                continue;
            }
            String[] parts = trimmed.split("\\s+");
            if (parts.length < 2) {
                continue;
            }
            AdminK8sWorkloadVO item = new AdminK8sWorkloadVO();
            String[] kindAndName = parts[0].split("/", 2);
            item.setKind(kindAndName[0].startsWith("stateful") ? "StatefulSet" : "Deployment");
            item.setName(kindAndName.length > 1 ? kindAndName[1] : parts[0]);
            item.setNamespace(namespace);
            String[] ready = parts[1].split("/", 2);
            item.setReadyReplicas(ready.length > 0 ? parseIntSafe(ready[0]) : 0);
            item.setReplicas(ready.length > 1 ? parseIntSafe(ready[1]) : item.getReadyReplicas());
            item.setAvailableReplicas(item.getReadyReplicas());
            item.setContainers("-");
            item.setImages("-");
            item.setStatus(Objects.equals(item.getReplicas(), item.getReadyReplicas()) ? "ready" : "updating");
            result.add(item);
        }
        return result;
    }

    private String runInClusterK8sWorkloadAction(String namespace, String kind, String name, String action, Integer replicas) throws Exception {
        String token = readK8sServiceAccountToken();
        String apiUrl = resolveK8sApiUrl();
        if (!StringUtils.hasText(token) || !StringUtils.hasText(apiUrl)) {
            return null;
        }
        HttpClient client = buildK8sHttpClient(apiUrl);
        String resourcePath = "/apis/apps/v1/namespaces/" + encodePathSegment(namespace) + "/" + k8sResourcePathPart(kind) + "/" + encodePathSegment(name);
        if ("RESTART".equals(action)) {
            Map<String, Object> patch = Map.of(
                    "spec", Map.of(
                            "template", Map.of(
                                    "metadata", Map.of(
                                            "annotations", Map.of("rk-web/restartedAt", Instant.now().toString())
                                    )
                            )
                    )
            );
            return sendK8sPatchText(client, apiUrl, token, resourcePath, patch, "application/strategic-merge-patch+json");
        }
        Map<String, Object> patch = Map.of("spec", Map.of("replicas", replicas));
        return sendK8sPatchText(client, apiUrl, token, resourcePath + "/scale", patch, "application/merge-patch+json");
    }

    private String runRemoteK8sWorkloadAction(String namespace, String kind, String name, String action, Integer replicas) {
        String resource = ("StatefulSet".equals(kind) ? "statefulset" : "deployment") + "/" + name;
        String command = "RESTART".equals(action)
                ? "kubectl rollout restart " + resource + " -n " + namespace
                : "kubectl scale " + resource + " -n " + namespace + " --replicas=" + replicas;
        return runRemoteCommand("bash -lc " + quoteForBash(command));
    }

    private String normalizePodExecCommand(String command) {
        if (!StringUtils.hasText(command)) {
            throw new BadRequestException("command is required");
        }
        String trimmed = command.trim();
        if (trimmed.length() > 2000) {
            throw new BadRequestException("command is too long");
        }
        return trimmed;
    }

    private String sendK8sExecRequest(String namespace, String podName, String containerName, String command, int timeoutSeconds) {
        if (Files.isRegularFile(Path.of(k8sTokenPath)) && StringUtils.hasText(resolveK8sApiUrl())) {
            return sendK8sExecRequestInCluster(namespace, podName, containerName, command, timeoutSeconds);
        }
        String encodedCommand = Base64.getEncoder().encodeToString(command.getBytes(StandardCharsets.UTF_8));
        StringBuilder script = new StringBuilder();
        script.append("set -e\n");
        script.append("NS=").append(quoteForBash(namespace)).append("\n");
        script.append("POD=").append(quoteForBash(podName)).append("\n");
        script.append("CONTAINER=").append(quoteForBash(containerName)).append("\n");
        script.append("ENCODED_CMD=").append(quoteForBash(encodedCommand)).append("\n");
        script.append("CMD=$(printf '%s' \"$ENCODED_CMD\" | base64 -d)\n");
        script.append("if command -v timeout >/dev/null 2>&1; then\n");
        script.append("  timeout ").append(timeoutSeconds).append("s kubectl exec -n \"$NS\" \"$POD\" -c \"$CONTAINER\" -- /bin/sh -lc \"$CMD\"\n");
        script.append("else\n");
        script.append("  kubectl exec -n \"$NS\" \"$POD\" -c \"$CONTAINER\" -- /bin/sh -lc \"$CMD\"\n");
        script.append("fi\n");
        return runRemoteCommand("bash -lc " + quoteForBash(script.toString()));
    }

    private String sendK8sExecRequestInCluster(String namespace, String podName, String containerName, String command, int timeoutSeconds) {
        String encodedCommand = Base64.getEncoder().encodeToString(command.getBytes(StandardCharsets.UTF_8));
        StringBuilder script = new StringBuilder();
        script.append("set -e\n");
        script.append("NS=").append(quoteForBash(namespace)).append("\n");
        script.append("POD=").append(quoteForBash(podName)).append("\n");
        script.append("CONTAINER=").append(quoteForBash(containerName)).append("\n");
        script.append("ENCODED_CMD=").append(quoteForBash(encodedCommand)).append("\n");
        script.append("API_URL=").append(quoteForBash(resolveK8sApiUrl())).append("\n");
        script.append("TOKEN_PATH=").append(quoteForBash(k8sTokenPath)).append("\n");
        script.append("CA_PATH=").append(quoteForBash(k8sCaPath)).append("\n");
        script.append("KUBECTL=$(command -v kubectl || true)\n");
        script.append("if [ -z \"$KUBECTL\" ] || [ ! -x \"$KUBECTL\" ]; then\n");
        script.append("  echo 'kubectl is not installed in the rk-user image; rebuild from ci/docker/backend/Dockerfile' >&2\n");
        script.append("  exit 127\n");
        script.append("fi\n");
        script.append("TOKEN=$(cat \"$TOKEN_PATH\")\n");
        script.append("TLS_ARG=\"--insecure-skip-tls-verify=true\"\n");
        script.append("[ -f \"$CA_PATH\" ] && TLS_ARG=\"--certificate-authority=$CA_PATH\"\n");
        script.append("CMD=$(printf '%s' \"$ENCODED_CMD\" | base64 -d)\n");
        script.append("if command -v timeout >/dev/null 2>&1; then\n");
        script.append("  timeout ").append(timeoutSeconds).append("s \"$KUBECTL\" --server=\"$API_URL\" --token=\"$TOKEN\" \"$TLS_ARG\" --request-timeout=")
                .append(timeoutSeconds).append("s exec -n \"$NS\" \"$POD\" -c \"$CONTAINER\" -- /bin/sh -lc \"$CMD\"\n");
        script.append("else\n");
        script.append("  \"$KUBECTL\" --server=\"$API_URL\" --token=\"$TOKEN\" \"$TLS_ARG\" --request-timeout=")
                .append(timeoutSeconds).append("s exec -n \"$NS\" \"$POD\" -c \"$CONTAINER\" -- /bin/sh -lc \"$CMD\"\n");
        script.append("fi\n");
        return runLocalShell(script.toString(), timeoutSeconds + 120);
    }

    private String runLocalShell(String script, int timeoutSeconds) {
        ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-lc", script);
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            if (!process.waitFor(Math.max(5, timeoutSeconds), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("local command timed out");
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (InputStream inputStream = process.getInputStream()) {
                inputStream.transferTo(output);
            }
            String text = output.toString(StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw new IllegalStateException(text.trim());
            }
            return text.trim();
        } catch (Exception e) {
            throw new IllegalStateException("local command failed: " + e.getMessage(), e);
        }
    }

    private String sendK8sPatchText(HttpClient client, String apiUrl, String token, String path, Object body, String contentType) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl + path))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .header("Content-Type", contentType)
                .timeout(Duration.ofSeconds(15))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(toJson(body), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Kubernetes API " + path + " returned " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    private String normalizeK8sWorkloadKind(String kind) {
        if (!StringUtils.hasText(kind)) {
            return "Deployment";
        }
        String value = kind.trim().toLowerCase();
        if ("statefulset".equals(value) || "statefulsets".equals(value) || "sts".equals(value)) {
            return "StatefulSet";
        }
        if ("deployment".equals(value) || "deployments".equals(value) || "deploy".equals(value)) {
            return "Deployment";
        }
        throw new BadRequestException("工作负载类型只支持 Deployment 或 StatefulSet");
    }

    private String k8sResourcePathPart(String kind) {
        return "StatefulSet".equals(kind) ? "statefulsets" : "deployments";
    }

    private void requireDangerConfirmation(String action, Integer replicas, String expectedText, String confirmText) {
        boolean dangerous = "RESTART".equals(action)
                || "CLEANUP".equals(action)
                || ("SCALE".equals(action) && Objects.equals(replicas, 0));
        if (!dangerous) {
            return;
        }
        if (!StringUtils.hasText(confirmText) || !Objects.equals(confirmText.trim(), expectedText)) {
            throw new BadRequestException("confirm required: input " + expectedText + " to execute " + action);
        }
    }

    private Map<String, Object> topologyNode(String id, String label, String type, Map<String, Object> source) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", id);
        node.put("label", label);
        node.put("type", type);
        node.put("status", source == null ? "unknown" : firstNonEmpty(valueAsString(source.get("status")), "unknown"));
        node.put("instances", source == null ? 0 : valueAsInteger(source.get("instances")));
        node.put("healthyInstances", source == null ? 0 : valueAsInteger(source.get("healthyInstances")));
        node.put("links", topologyLinks(id, type));
        return node;
    }

    private Map<String, String> topologyLinks(String id, String type) {
        Map<String, String> links = new LinkedHashMap<>();
        if (!"middleware".equals(type)) {
            links.put("logs", "/admin/operation/monitoring?service=" + id);
            links.put("buildHistory", "/admin/operation/jenkins?serviceCode=" + id);
        }
        if (!"gateway".equals(type)) {
            links.put("configCenter", "/admin/operation/nacos?dataId=" + id + ".yaml");
        }
        return links;
    }

    private Map<String, Object> topologyEdge(String source, String target, String type) {
        Map<String, Object> edge = new LinkedHashMap<>();
        edge.put("source", source);
        edge.put("target", target);
        edge.put("type", type);
        return edge;
    }

    private String requireText(String value, String label) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(label + "不能为空");
        }
        return value.trim();
    }

    private void validateNacosContent(String type, String content) {
        if (!"json".equalsIgnoreCase(type) || !StringUtils.hasText(content)) {
            return;
        }
        try {
            objectMapper.readTree(content);
        } catch (Exception e) {
            throw new BadRequestException("Nacos JSON content invalid: " + e.getMessage());
        }
    }

    private String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String trimToDefault(String value, String fallback) {
        String trimmed = trimToEmpty(value);
        return StringUtils.hasText(trimmed) ? trimmed : trimToEmpty(fallback);
    }

    private String defaultRemoteDeployDomain() {
        String publicBaseUrl = PublicBaseUrlResolver.resolve(deployPackagePublicBaseUrl);
        try {
            URI uri = URI.create(publicBaseUrl);
            return trimToEmpty(uri.getHost());
        } catch (Exception ignored) {
            return "";
        }
    }

    private String resolveRemoteDeployDomain(AdminDeployPackageCreateDTO request) {
        String requestedDomain = request == null ? "" : request.getTargetDomain();
        return trimToDefault(requestedDomain, defaultRemoteDeployDomain());
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private void ensureDefaultBackupSchedule() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admin_backup_schedule", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO admin_backup_schedule (id, enabled, frequency, backup_time, retention_days, database_list) VALUES (1, ?, ?, ?, ?, ?)",
                1,
                "daily",
                "02:00",
                30,
                toJson(DEFAULT_BACKUP_DATABASES)
        );
    }

    private void createBaselineBackupIfNeeded() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admin_backup_record WHERE is_deleted = 0", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("databases", DEFAULT_BACKUP_DATABASES);
        summary.put("databaseSizes", queryDatabaseSizeRows(DEFAULT_BACKUP_DATABASES));
        jdbcTemplate.update(
                "INSERT INTO admin_backup_record (backup_name, database_scope, database_list, file_name, remote_path, file_size_bytes, status, backup_type, note_text, summary_json, created_by) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                "baseline_snapshot_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                "ALL",
                toJson(DEFAULT_BACKUP_DATABASES),
                null,
                null,
                0L,
                "SUCCESS",
                "BASELINE",
                "首次进入运维页时自动生成的基线快照",
                toJson(summary),
                UserContext.getUser()
        );
    }

    private List<AdminBackupRecordVO> loadBackupRecords() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, backup_name, database_scope, database_list, file_name, file_size_bytes, status, backup_type, create_time, remote_path, note_text, summary_json " +
                        "FROM admin_backup_record WHERE is_deleted = 0 ORDER BY create_time DESC"
        );
        List<AdminBackupRecordVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            AdminBackupRecordVO item = new AdminBackupRecordVO();
            item.setId(((Number) row.get("id")).longValue());
            item.setName(valueAsString(row.get("backup_name")));
            item.setDatabase(valueAsString(row.get("database_scope")));
            item.setDatabaseList(readStringList(row.get("database_list")));
            Map<String, List<String>> tableSummary = readTableSummary(row.get("summary_json"));
            if (tableSummary.isEmpty()) {
                for (String database : item.getDatabaseList()) {
                    tableSummary.put(database, Collections.emptyList());
                }
            }
            item.setTableSummary(tableSummary);
            item.setTableCount(countTables(tableSummary));
            item.setType(valueAsString(row.get("backup_type")));
            item.setFileName(valueAsString(row.get("file_name")));
            item.setSize(formatBytes(valueAsLong(row.get("file_size_bytes"))));
            item.setCreateTime(formatDateTime(row.get("create_time")));
            item.setStatus(valueAsString(row.get("status")));
            item.setNote(valueAsString(row.get("note_text")));
            String remotePath = valueAsString(row.get("remote_path"));
            item.setDownloadable(StringUtils.hasText(remotePath) && backupStorageService.exists(remotePath));
            result.add(item);
        }
        return result;
    }

    private AdminBackupScheduleVO loadBackupSchedule() {
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT enabled, frequency, backup_time, retention_days, database_list FROM admin_backup_schedule WHERE id = 1"
        );
        AdminBackupScheduleVO schedule = new AdminBackupScheduleVO();
        schedule.setEnabled(valueAsInteger(row.get("enabled")) == 1);
        schedule.setFrequency(valueAsString(row.get("frequency")));
        schedule.setTime(valueAsString(row.get("backup_time")));
        schedule.setRetentionDays(valueAsInteger(row.get("retention_days")));
        schedule.setDatabases(readStringList(row.get("database_list")));
        return schedule;
    }

    private long sumBackupBytes() {
        Long total = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(file_size_bytes), 0) FROM admin_backup_record WHERE is_deleted = 0",
                Long.class
        );
        return total == null ? 0L : total;
    }

    private Map<String, Object> loadBackupRecordRow(Long backupId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, remote_path FROM admin_backup_record WHERE id = ? AND is_deleted = 0",
                backupId
        );
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("备份记录不存在");
        }
        return rows.get(0);
    }

    private void updateDeployPackageProgress(Long packageId, int progress, String line) {
        int remainingSeconds = computeEstimatedRemainingSecondsForPackage(packageId, progress);
        LocalDateTime updatedAt = LocalDateTime.now();
        jdbcTemplate.update(
                "UPDATE ops_deploy_package_record SET progress = GREATEST(COALESCE(progress, 0), ?), logs = CONCAT(COALESCE(logs,''), ?), estimated_remaining_seconds = CASE WHEN global_migration_lock = 1 THEN ? ELSE estimated_remaining_seconds END, migration_updated_at = CASE WHEN global_migration_lock = 1 THEN ? ELSE migration_updated_at END, update_time = ? WHERE id = ?",
                progress,
                line + "\n",
                remainingSeconds,
                updatedAt,
                updatedAt,
                packageId
        );
        refreshGlobalMigrationLock(packageId);
    }

    private void updateDeployPackageStep(Long packageId, int progress, String currentStep, String currentImage, String line) {
        int remainingSeconds = computeEstimatedRemainingSecondsForPackage(packageId, progress);
        LocalDateTime updatedAt = LocalDateTime.now();
        jdbcTemplate.update(
                "UPDATE ops_deploy_package_record SET progress = GREATEST(COALESCE(progress, 0), ?), current_step = ?, current_image = ?, logs = CONCAT(COALESCE(logs,''), ?), estimated_remaining_seconds = CASE WHEN global_migration_lock = 1 THEN ? ELSE estimated_remaining_seconds END, migration_updated_at = CASE WHEN global_migration_lock = 1 THEN ? ELSE migration_updated_at END, update_time = ? WHERE id = ?",
                progress,
                currentStep,
                currentImage,
                line + "\n",
                remainingSeconds,
                updatedAt,
                updatedAt,
                packageId
        );
        refreshGlobalMigrationLock(packageId);
    }

    private int computeEstimatedRemainingSeconds(int progress, int totalImages) {
        int boundedProgress = Math.max(0, Math.min(100, progress));
        int imageWeight = Math.max(1, totalImages) * 20;
        int baseSeconds = 300 + imageWeight;
        return boundedProgress >= 100 ? 0 : Math.max(30, (int) Math.round(baseSeconds * ((100 - boundedProgress) / 100.0d)));
    }

    private int computeEstimatedRemainingSecondsForPackage(Long packageId, int progress) {
        int totalImages = 0;
        if (packageId != null) {
            try {
                Integer value = jdbcTemplate.queryForObject(
                        "SELECT total_images FROM ops_deploy_package_record WHERE id = ?",
                        Integer.class,
                        packageId
                );
                totalImages = value == null ? 0 : value;
            } catch (Exception ignored) {
                totalImages = 0;
            }
        }
        return computeEstimatedRemainingSeconds(progress, totalImages);
    }

    private void updateRemoteMigrationEta(Long packageId, int progress) {
        Integer totalImages = jdbcTemplate.queryForObject(
                "SELECT total_images FROM ops_deploy_package_record WHERE id = ?",
                Integer.class,
                packageId
        );
        int remainingSeconds = computeEstimatedRemainingSeconds(progress, totalImages == null ? 0 : totalImages);
        LocalDateTime updatedAt = LocalDateTime.now();
        jdbcTemplate.update(
                "UPDATE ops_deploy_package_record SET estimated_remaining_seconds = ?, migration_updated_at = ?, update_time = ? WHERE id = ?",
                remainingSeconds,
                updatedAt,
                updatedAt,
                packageId
        );
        refreshGlobalMigrationLock(packageId);
    }

    private void refreshGlobalMigrationLock(Long packageId) {
        if (packageId == null) {
            return;
        }
        AdminDeployPackageRecordVO record = loadDeployPackageRecord(packageId);
        if (record == null
                || !Boolean.TRUE.equals(record.getGlobalMigrationLock())
                || !"RUNNING".equalsIgnoreCase(trimToEmpty(record.getStatus()))) {
            return;
        }
        publishGlobalMigrationLock(record);
    }

    private void refreshGlobalMigrationLock(AdminDeployPackageRecordVO record) {
        if (record == null
                || !Boolean.TRUE.equals(record.getGlobalMigrationLock())
                || !"RUNNING".equalsIgnoreCase(trimToEmpty(record.getStatus()))) {
            return;
        }
        publishGlobalMigrationLock(record);
    }

    private void publishGlobalMigrationLock(AdminDeployPackageRecordVO record) {
        if (record == null || stringRedisTemplate == null) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(
                    GLOBAL_MIGRATION_LOCK_KEY,
                    toJson(sanitizePublicMigrationRecord(record)),
                    GLOBAL_MIGRATION_LOCK_TTL
            );
        } catch (Exception e) {
            log.warn("publish global migration lock failed, packageId={}", record.getId(), e);
        }
    }

    private void clearGlobalMigrationLock() {
        if (stringRedisTemplate == null) {
            return;
        }
        try {
            AdminDeployPackageRecordVO active = getActiveGlobalMigration();
            if (active != null) {
                publishGlobalMigrationLock(active);
                return;
            }
            stringRedisTemplate.delete(GLOBAL_MIGRATION_LOCK_KEY);
        } catch (Exception e) {
            log.warn("clear global migration lock failed", e);
        }
    }

    private void updateDeployPackageRegistryProgress(Long packageId,
                                                     String currentStep,
                                                     String currentImage,
                                                     int uploadedImages,
                                                     int totalImages,
                                                     String line) {
        int safeTotal = Math.max(0, totalImages);
        int safeUploaded = Math.max(0, Math.min(uploadedImages, safeTotal == 0 ? uploadedImages : safeTotal));
        int uploadPercent = safeTotal <= 0 ? 0 : Math.min(100, (int) Math.round((safeUploaded * 100.0d) / safeTotal));
        int overallProgress = Math.min(67, 45 + (int) Math.round(uploadPercent * 0.20d));
        jdbcTemplate.update(
                "UPDATE ops_deploy_package_record SET progress = ?, current_step = ?, current_image = ?, uploaded_images = ?, total_images = ?, upload_percent = ?, logs = CONCAT(COALESCE(logs,''), ?), update_time = NOW() WHERE id = ?",
                overallProgress,
                currentStep,
                currentImage,
                safeUploaded,
                safeTotal,
                uploadPercent,
                line + "\n",
                packageId
        );
        refreshGlobalMigrationLock(packageId);
    }

    private void recordRegistryPullPackageProgress(Long packageId, AdminDeployPackageCreateDTO request) {
        List<String> images = resolveDeployPackageImageList(request);
        int total = images.size();
        if (total == 0) {
            updateDeployPackageRegistryProgress(packageId, "registry pull package", null, 0, 0,
                    "registry pull package prepared without explicit image list");
            return;
        }
        updateDeployPackageRegistryProgress(packageId, "preparing registry image list", null, 0, total,
                "registry pull package will use " + total + " images from " + request.getRegistryPrefix());
        for (int i = 0; i < total; i++) {
            String image = images.get(i);
            updateDeployPackageRegistryProgress(packageId, "writing pull manifest", image, i + 1, total,
                    "registry image ready for pull package: " + image);
        }
    }

    private int initialDeployPackageImageCount(AdminDeployPackageCreateDTO request) {
        if (request != null && Boolean.FALSE.equals(request.getIncludeImages())) {
            return 0;
        }
        return estimateDeployPackageImageCount();
    }

    private int initialRemoteDeployImageCount(AdminDeployPackageCreateDTO request) {
        if (request != null && Boolean.FALSE.equals(request.getIncludeImages())) {
            return 0;
        }
        // Keep the HTTP submit path independent from live K8s scans; the async worker writes exact totals.
        return K8S_PERSISTENT_MIDDLEWARE.size() + REMOTE_K8S_DEPLOYMENTS.size() + 2;
    }

    private void completeDeployPackageImageProgress(Long packageId) {
        jdbcTemplate.update(
                "UPDATE ops_deploy_package_record SET uploaded_images = CASE WHEN total_images > 0 THEN total_images ELSE uploaded_images END, " +
                        "upload_percent = CASE WHEN total_images > 0 THEN 100 ELSE upload_percent END, update_time = NOW() WHERE id = ?",
                packageId
        );
    }

    private int estimateDeployPackageImageCount() {
        return resolveDeployPackageImageList(new AdminDeployPackageCreateDTO()).size();
    }

    private List<String> resolveDeployPackageImageList(AdminDeployPackageCreateDTO request) {
        String registryPrefix = request == null ? DEFAULT_REGISTRY_PREFIX : trimToDefault(request.getRegistryPrefix(), DEFAULT_REGISTRY_PREFIX);
        Map<String, String> runtimeImagesByRepository = currentRuntimeImagesByRepository(request);
        List<String> registryImages = new ArrayList<>();
        registryImages.add(imageForRuntimeComponent(request, runtimeImagesByRepository, "mysql:8.0", "mysql", "rk-mysql"));
        registryImages.add(imageForRuntimeComponent(request, runtimeImagesByRepository, "redis:7-alpine", "redis", "rk-redis"));
        registryImages.add(rabbitmqImageForRuntimeComponent(request, runtimeImagesByRepository));
        registryImages.add(imageForRuntimeComponent(request, runtimeImagesByRepository, "minio/minio:RELEASE.2024-05-10T01-41-38Z", "minio", "rk-minio"));
        registryImages.add(imageForRuntimeComponent(request, runtimeImagesByRepository, "nacos/nacos-server:v2.3.2", "nacos-server", "nacos", "rk-nacos"));
        registryImages.add(imageForRuntimeComponent(request, runtimeImagesByRepository, "seataio/seata-server:1.7.1", "seata", "seata-server", "rk-seata"));
        registryImages.add(imageForRuntimeComponent(request, runtimeImagesByRepository, "xuxueli/xxl-job-admin:2.3.0", "xxl-job", "xxl-job-admin", "rk-xxl-job"));
        registryImages.add(imageForRuntimeComponent(request, runtimeImagesByRepository, "elasticsearch:7.12.1", "elasticsearch", "rk-elasticsearch"));
        registryImages.add(REMOTE_GOGS_IMAGE);
        registryImages.add(REMOTE_JENKINS_IMAGE);
        registryImages.addAll(SERVICE_IMAGE_MAP.values().stream()
                .distinct()
                .map(name -> imageForService(request, name, runtimeImagesByRepository))
                .collect(Collectors.toList()));
        registryImages = registryImages.stream().filter(StringUtils::hasText).distinct().collect(Collectors.toList());
        List<String> currentImages = listServiceRegistry().stream()
                .map(AdminOpsServiceVO::getImageName)
                .filter(StringUtils::hasText)
                .filter(image -> isDeployPackageBusinessRuntimeImage(image) && !isDeployPackagePlatformImage(image))
                .distinct()
                .collect(Collectors.toList());
        if (currentImages.isEmpty()) {
            return registryImages;
        }
        LinkedHashSet<String> merged = new LinkedHashSet<>(currentImages);
        merged.addAll(registryImages);
        return new ArrayList<>(merged);
    }

    private String imageForService(AdminDeployPackageCreateDTO request, String serviceCode) {
        return imageForService(request, serviceCode, currentRuntimeImagesByRepository(request));
    }

    private String imageForService(AdminDeployPackageCreateDTO request, String serviceCode, Map<String, String> runtimeImagesByRepository) {
        String registryPrefix = request == null ? DEFAULT_REGISTRY_PREFIX : trimToDefault(request.getRegistryPrefix(), DEFAULT_REGISTRY_PREFIX);
        String repository = SERVICE_IMAGE_MAP.getOrDefault(serviceCode, serviceCode);
        String runtimeImage = findRuntimeImageByRepository(runtimeImagesByRepository, repository);
        if (StringUtils.hasText(runtimeImage)) {
            return registryImageForRuntimeImage(request, runtimeImage, repository, "latest");
        }
        return registryPrefix + "/" + repository + ":latest";
    }

    private String imageForRuntimeComponent(AdminDeployPackageCreateDTO request,
                                            Map<String, String> runtimeImagesByRepository,
                                            String fallbackImage,
                                            String... repositoryCandidates) {
        if (repositoryCandidates != null) {
            for (String candidate : repositoryCandidates) {
                String runtimeImage = findRuntimeImageByRepository(runtimeImagesByRepository, candidate);
                if (StringUtils.hasText(runtimeImage)) {
                    return registryImageForRuntimeImage(request, runtimeImage, candidate, extractImageTag(fallbackImage, "latest"));
                }
            }
        }
        return registryImageForRuntimeImage(request, fallbackImage, extractImageRepository(fallbackImage), extractImageTag(fallbackImage, "latest"));
    }

    private String rabbitmqImageForRuntimeComponent(AdminDeployPackageCreateDTO request,
                                                    Map<String, String> runtimeImagesByRepository) {
        return imageForRuntimeComponent(request, runtimeImagesByRepository, REMOTE_RABBITMQ_IMAGE,
                "rabbitmq", "rk-rabbitmq", "shetuanguanlixitong-rk-server-rk-rabbitmq",
                "rabbitmq-delayed-message-exchange");
    }

    private String registryImageForRuntimeImage(AdminDeployPackageCreateDTO request, String runtimeImage, String fallbackRepository, String fallbackTag) {
        String registryPrefix = request == null ? DEFAULT_REGISTRY_PREFIX : trimToDefault(request.getRegistryPrefix(), DEFAULT_REGISTRY_PREFIX);
        String repository = extractImageRepository(runtimeImage);
        String tag = extractImageTag(runtimeImage, fallbackTag);
        if (!StringUtils.hasText(repository)) {
            repository = trimToDefault(fallbackRepository, "image");
        }
        return registryPrefix + "/" + repository + ":" + tag;
    }

    private String findRuntimeImageByRepository(Map<String, String> runtimeImagesByRepository, String repository) {
        if (runtimeImagesByRepository == null || !StringUtils.hasText(repository)) {
            return "";
        }
        String normalized = repository.trim();
        String exact = runtimeImagesByRepository.get(normalized);
        if (StringUtils.hasText(exact)) {
            return exact;
        }
        for (Map.Entry<String, String> entry : runtimeImagesByRepository.entrySet()) {
            String key = entry.getKey();
            if (!StringUtils.hasText(key)) {
                continue;
            }
            if (key.equals(normalized) || key.endsWith("-" + normalized)) {
                return entry.getValue();
            }
        }
        return "";
    }

    private Map<String, String> currentRuntimeImagesByRepository() {
        return currentRuntimeImagesByRepository(new AdminDeployPackageCreateDTO());
    }

    private Map<String, String> currentRuntimeImagesByRepository(AdminDeployPackageCreateDTO request) {
        Map<String, String> images = new LinkedHashMap<>();
        for (String image : loadCurrentWorkloadImagesForRuntimeExport(request)) {
            String repository = extractImageRepository(image);
            if (StringUtils.hasText(repository) && !images.containsKey(repository)) {
                images.put(repository, image);
            }
        }
        return images;
    }

    private String extractImageRepository(String image) {
        if (!StringUtils.hasText(image)) {
            return "";
        }
        String normalized = image.trim();
        int digestIndex = normalized.indexOf('@');
        if (digestIndex >= 0) {
            normalized = normalized.substring(0, digestIndex);
        }
        int slashIndex = normalized.lastIndexOf('/');
        String lastSegment = slashIndex >= 0 ? normalized.substring(slashIndex + 1) : normalized;
        int colonIndex = lastSegment.lastIndexOf(':');
        return colonIndex > 0 ? lastSegment.substring(0, colonIndex) : lastSegment;
    }

    private String extractImageTag(String image, String fallback) {
        if (!StringUtils.hasText(image)) {
            return trimToDefault(fallback, "latest");
        }
        String normalized = image.trim();
        int digestIndex = normalized.indexOf('@');
        if (digestIndex >= 0) {
            return trimToDefault(fallback, "latest");
        }
        int slashIndex = normalized.lastIndexOf('/');
        int colonIndex = normalized.lastIndexOf(':');
        if (colonIndex > slashIndex) {
            return trimToDefault(normalized.substring(colonIndex + 1), trimToDefault(fallback, "latest"));
        }
        return trimToDefault(fallback, "latest");
    }

    private AdminDeployPackageCreateDTO normalizeDeployPackageRequest(AdminDeployPackageCreateDTO dto) {
        AdminDeployPackageCreateDTO request = dto == null ? new AdminDeployPackageCreateDTO() : dto;
        String packageMode = trimToDefault(request.getPackageMode(), DEPLOY_PACKAGE_MODE_OFFLINE_FULL);
        if (!DEPLOY_PACKAGE_MODE_BOOTSTRAP.equals(packageMode) && !"self-contained".equals(packageMode)) {
            packageMode = DEPLOY_PACKAGE_MODE_OFFLINE_FULL;
        }
        if ("self-contained".equals(packageMode)) {
            packageMode = DEPLOY_PACKAGE_MODE_OFFLINE_FULL;
        }
        String deliveryMode = trimToDefault(request.getDeliveryMode(), DEPLOY_PACKAGE_DELIVERY_DIRECT);
        if (!DEPLOY_PACKAGE_DELIVERY_DIRECT.equals(deliveryMode) && !DEPLOY_PACKAGE_DELIVERY_STORE.equals(deliveryMode)) {
            deliveryMode = DEPLOY_PACKAGE_DELIVERY_DIRECT;
        }
        request.setPackageMode(packageMode);
        request.setDeliveryMode(deliveryMode);
        request.setSelfContained(!DEPLOY_PACKAGE_MODE_BOOTSTRAP.equals(packageMode));
        request.setIncludeDatabases(!Boolean.FALSE.equals(request.getIncludeDatabases()));
        request.setIncludeImages(!Boolean.FALSE.equals(request.getIncludeImages()));
        request.setIncludeK8sManifests(!Boolean.FALSE.equals(request.getIncludeK8sManifests()));
        request.setIncludeMinio(!Boolean.FALSE.equals(request.getIncludeMinio()));
        request.setIncludeDockerCompose(!Boolean.FALSE.equals(request.getIncludeDockerCompose()));
        request.setIncludeSource(!Boolean.FALSE.equals(request.getIncludeSource()));
        request.setIncludeAllCurrentData(!Boolean.FALSE.equals(request.getIncludeAllCurrentData()));
        request.setCleanupRuntimeArtifacts(!Boolean.FALSE.equals(request.getCleanupRuntimeArtifacts()));
        request.setDeleteAfterDownload(Boolean.TRUE.equals(request.getDeleteAfterDownload())
                || DEPLOY_PACKAGE_DELIVERY_DIRECT.equals(deliveryMode));
        String imageArtifactMode = trimToDefault(request.getImageArtifactMode(), IMAGE_ARTIFACT_BOTH);
        if (!IMAGE_ARTIFACT_OFFLINE_TAR.equals(imageArtifactMode)
                && !IMAGE_ARTIFACT_REGISTRY.equals(imageArtifactMode)
                && !IMAGE_ARTIFACT_BOTH.equals(imageArtifactMode)) {
            imageArtifactMode = IMAGE_ARTIFACT_BOTH;
        }
        request.setImageArtifactMode(imageArtifactMode);
        request.setExportRuntimeArtifacts(Boolean.TRUE.equals(request.getExportRuntimeArtifacts())
                || IMAGE_ARTIFACT_REGISTRY.equals(imageArtifactMode)
                || IMAGE_ARTIFACT_BOTH.equals(imageArtifactMode));
        request.setRegistryPrefix(trimToDefault(request.getRegistryPrefix(), DEFAULT_REGISTRY_PREFIX));
        request.setRegistryServer(trimToDefault(request.getRegistryServer(), DEFAULT_REGISTRY_SERVER));
        request.setTargetNamespace(trimToDefault(request.getTargetNamespace(), "shetuanguanlixitong"));
        request.setTargetDomain(resolveRemoteDeployDomain(request));
        applyDeployTargetDefaults(request);
        request.setRemoteDeploy(Boolean.TRUE.equals(request.getRemoteDeploy()));
        request.setKubeContext(trimToEmpty(request.getKubeContext()));
        request.setWaitRollout(!Boolean.FALSE.equals(request.getWaitRollout()));
        int rolloutTimeoutSeconds = request.getRolloutTimeoutSeconds() == null ? 600 : request.getRolloutTimeoutSeconds();
        request.setRolloutTimeoutSeconds(Math.max(60, Math.min(3600, rolloutTimeoutSeconds)));
        request.setDryRun(Boolean.TRUE.equals(request.getDryRun()));
        request.setApplyDatabaseSnapshot(Boolean.TRUE.equals(request.getApplyDatabaseSnapshot()));
        request.setApplyMinioSnapshot(Boolean.TRUE.equals(request.getApplyMinioSnapshot()));
        request.setStreamingMigration(!Boolean.FALSE.equals(request.getStreamingMigration()));
        request.setGlobalMigrationLock(!Boolean.FALSE.equals(request.getGlobalMigrationLock()));
        request.setEstimatedRemainingSeconds(request.getEstimatedRemainingSeconds() == null ? null : Math.max(0, request.getEstimatedRemainingSeconds()));
        request.setMigrationStartedAt(trimToEmpty(request.getMigrationStartedAt()));
        request.setMigrationUpdatedAt(trimToEmpty(request.getMigrationUpdatedAt()));
        return request;
    }

    private DeployTargetDefaults resolveDeployTargetDefaults(AdminDeployPackageCreateDTO request) {
        AdminDeployPackageCreateDTO source = request == null ? new AdminDeployPackageCreateDTO() : request;
        String targetClusterType = normalizeTargetClusterType(source.getTargetClusterType());
        String externalExposureType = normalizeExternalExposureType(source.getExternalExposureType());
        String storageClassName = resolveDeployStorageClassName(targetClusterType, source.getStorageClassName());
        String ingressHost = trimToDefault(source.getIngressHost(), source.getTargetDomain());
        Integer frontendNodePort = EXPOSURE_NODE_PORT.equals(externalExposureType)
                ? normalizeNodePort(source.getFrontendNodePort(), DEFAULT_FRONTEND_NODE_PORT, "frontendNodePort")
                : source.getFrontendNodePort();
        Integer gatewayNodePort = EXPOSURE_NODE_PORT.equals(externalExposureType)
                ? normalizeNodePort(source.getGatewayNodePort(), DEFAULT_GATEWAY_NODE_PORT, "gatewayNodePort")
                : source.getGatewayNodePort();
        DeployTargetDefaults defaults = new DeployTargetDefaults();
        defaults.targetClusterType = targetClusterType;
        defaults.storageClassName = storageClassName;
        defaults.externalExposureType = externalExposureType;
        defaults.frontendNodePort = frontendNodePort;
        defaults.gatewayNodePort = gatewayNodePort;
        defaults.ingressClassName = trimToEmpty(source.getIngressClassName());
        defaults.ingressHost = trimToDefault(ingressHost, trimToDefault(source.getTargetDomain(), defaultRemoteDeployDomain()));
        return defaults;
    }

    private void applyDeployTargetDefaults(AdminDeployPackageCreateDTO request) {
        if (request == null) {
            return;
        }
        DeployTargetDefaults defaults = resolveDeployTargetDefaults(request);
        request.setTargetClusterType(defaults.targetClusterType);
        request.setStorageClassName(defaults.storageClassName);
        request.setExternalExposureType(defaults.externalExposureType);
        request.setFrontendNodePort(defaults.frontendNodePort);
        request.setGatewayNodePort(defaults.gatewayNodePort);
        request.setIngressClassName(defaults.ingressClassName);
        request.setIngressHost(defaults.ingressHost);
    }

    private String normalizeTargetClusterType(String value) {
        String clusterType = trimToDefault(value, TARGET_CLUSTER_K3S).toLowerCase(Locale.ROOT);
        if (TARGET_CLUSTER_ACK.equals(clusterType)
                || TARGET_CLUSTER_K8S.equals(clusterType)
                || TARGET_CLUSTER_K3S.equals(clusterType)
                || TARGET_CLUSTER_CUSTOM.equals(clusterType)) {
            return clusterType;
        }
        return TARGET_CLUSTER_K3S;
    }

    private String normalizeExternalExposureType(String value) {
        String exposure = trimToDefault(value, EXPOSURE_NONE);
        if (EXPOSURE_NONE.equals(exposure)
                || EXPOSURE_NODE_PORT.equals(exposure)
                || EXPOSURE_LOAD_BALANCER.equals(exposure)
                || EXPOSURE_INGRESS.equals(exposure)) {
            return exposure;
        }
        return EXPOSURE_NONE;
    }

    private String resolveDeployStorageClassName(String targetClusterType, String requestedStorageClassName) {
        String requested = trimToEmpty(requestedStorageClassName);
        if (StringUtils.hasText(requested)) {
            return requested;
        }
        if (TARGET_CLUSTER_ACK.equals(targetClusterType)) {
            return ACK_DEFAULT_STORAGE_CLASS;
        }
        if (TARGET_CLUSTER_K3S.equals(targetClusterType)) {
            return K3S_DEFAULT_STORAGE_CLASS;
        }
        String configuredStorageClass = trimToEmpty(deployPackageK8sStorageClassName);
        return StringUtils.hasText(configuredStorageClass) && !ACK_DEFAULT_STORAGE_CLASS.equals(configuredStorageClass)
                ? configuredStorageClass
                : "";
    }

    private Integer normalizeNodePort(Integer requestedPort, int fallback, String label) {
        int port = requestedPort == null ? fallback : requestedPort;
        if (port < 30000 || port > 32767) {
            throw new BadRequestException(label + " 必须在 30000-32767 之间");
        }
        return port;
    }

    private static class DeployTargetDefaults {
        private String targetClusterType;
        private String storageClassName;
        private String externalExposureType;
        private Integer frontendNodePort;
        private Integer gatewayNodePort;
        private String ingressClassName;
        private String ingressHost;
    }

    private String buildDeployPackageInitialLogs(AdminDeployPackageCreateDTO request) {
        return "task created\n"
                + "package mode: " + request.getPackageMode() + "\n"
                + "delivery mode: " + request.getDeliveryMode() + "\n"
                + "platform images are excluded by default: " + !Boolean.TRUE.equals(request.getIncludePlatformImages()) + "\n"
                + "collecting service registry\n";
    }

    private Map<String, Object> buildDeployPackageRequestForStorage(AdminDeployPackageCreateDTO request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("packageMode", request.getPackageMode());
        payload.put("deliveryMode", request.getDeliveryMode());
        payload.put("selfContained", request.getSelfContained());
        payload.put("includeSource", request.getIncludeSource());
        payload.put("includeDatabases", request.getIncludeDatabases());
        payload.put("includeImages", request.getIncludeImages());
        payload.put("includeK8sManifests", request.getIncludeK8sManifests());
        payload.put("includeMinio", request.getIncludeMinio());
        payload.put("includeDockerCompose", request.getIncludeDockerCompose());
        payload.put("includeAllCurrentData", request.getIncludeAllCurrentData());
        payload.put("exportRuntimeArtifacts", request.getExportRuntimeArtifacts());
        payload.put("cleanupRuntimeArtifacts", request.getCleanupRuntimeArtifacts());
        payload.put("deleteAfterDownload", request.getDeleteAfterDownload());
        payload.put("imageArtifactMode", request.getImageArtifactMode());
        payload.put("registryPrefix", request.getRegistryPrefix());
        payload.put("registryServer", request.getRegistryServer());
        payload.put("targetClusterType", request.getTargetClusterType());
        payload.put("targetNamespace", request.getTargetNamespace());
        payload.put("targetDomain", request.getTargetDomain());
        payload.put("storageClassName", request.getStorageClassName());
        payload.put("externalExposureType", request.getExternalExposureType());
        payload.put("frontendNodePort", request.getFrontendNodePort());
        payload.put("gatewayNodePort", request.getGatewayNodePort());
        payload.put("ingressClassName", request.getIngressClassName());
        payload.put("ingressHost", request.getIngressHost());
        payload.put("databases", request.getDatabases());
        payload.put("note", request.getNote());
        payload.put("remoteDeploy", false);
        return payload;
    }

    private Map<String, Object> buildRemoteDeployRequestForStorage(AdminDeployPackageCreateDTO request) {
        Map<String, Object> payload = buildDeployPackageRequestForStorage(request);
        payload.put("remoteDeploy", true);
        payload.put("kubeconfig", "[REDACTED]");
        payload.put("hasKubeconfig", StringUtils.hasText(request.getKubeconfig()));
        payload.put("kubeContext", trimToEmpty(request.getKubeContext()));
        payload.put("waitRollout", request.getWaitRollout());
        payload.put("rolloutTimeoutSeconds", request.getRolloutTimeoutSeconds());
        payload.put("dryRun", request.getDryRun());
        payload.put("confirmText", StringUtils.hasText(request.getConfirmText()) ? "[CONFIRMED]" : "");
        payload.put("applyDatabaseSnapshot", request.getApplyDatabaseSnapshot());
        payload.put("applyMinioSnapshot", request.getApplyMinioSnapshot());
        payload.put("streamingMigration", request.getStreamingMigration());
        payload.put("globalMigrationLock", request.getGlobalMigrationLock());
        return payload;
    }

    private Map<String, Object> buildLinuxSshMigrationRequestForStorage(AdminDeployPackageCreateDTO request) {
        Map<String, Object> payload = buildDeployPackageRequestForStorage(request);
        payload.put("remoteDeploy", true);
        payload.put("deployMode", "linux-ssh");
        payload.put("sshHost", request.getSshHost());
        payload.put("sshPort", request.getSshPort());
        payload.put("sshUsername", request.getSshUsername());
        payload.put("sshPassword", "[REDACTED]");
        payload.put("hasSshPassword", StringUtils.hasText(request.getSshPassword()));
        payload.put("linuxDeployMode", request.getLinuxDeployMode());
        payload.put("domesticMirror", request.getDomesticMirror());
        payload.put("installK8s", request.getInstallK8s());
        payload.put("installDocker", request.getInstallDocker());
        payload.put("copyServices", request.getCopyServices());
        payload.put("targetPath", request.getTargetPath());
        payload.put("confirmText", StringUtils.hasText(request.getConfirmText()) ? "[CONFIRMED]" : "");
        return payload;
    }

    private String buildLinuxSshMigrationPlanLog(AdminDeployPackageCreateDTO request, String targetLabel) {
        List<String> steps = new ArrayList<>();
        steps.add("linux ssh deployment task accepted");
        steps.add("target: " + targetLabel);
        steps.add("deploy mode: " + trimToDefault(request.getLinuxDeployMode(), "k8s"));
        steps.add("domestic mirror: " + Boolean.TRUE.equals(request.getDomesticMirror()));
        steps.add("K8s 环境安装: " + Boolean.TRUE.equals(request.getInstallK8s()));
        steps.add("Docker Compose: " + ("docker-compose".equals(trimToEmpty(request.getLinuxDeployMode())) || Boolean.TRUE.equals(request.getInstallDocker())));
        steps.add("install docker: " + Boolean.TRUE.equals(request.getInstallDocker()));
        steps.add("copy services: " + Boolean.TRUE.equals(request.getCopyServices()));
        steps.add("target path: " + trimToDefault(request.getTargetPath(), "/opt/rk-web"));
        steps.add("planned domestic package sources: configured package mirror and docker registry mirror");
        steps.add("planned K8s commands: install container runtime, kubelet/kubeadm/kubectl or k3s, initialize cluster, prepare StorageClass");
        steps.add("planned service copy: generate deploy package, copy service files, load/pull images, restore MySQL and MinIO data");
        steps.add("SSH password was used only for this request and was not persisted");
        steps.add("automatic SSH executor is isolated from deploy-package logs; follow-up execution output will append here");
        return String.join("\n", steps) + "\n";
    }

    private Map<String, Object> buildDeployPackageManifest(AdminDeployPackageCreateDTO request, List<String> databases) {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("generatedAt", LocalDateTime.now().format(TIME_FORMATTER));
        manifest.put("packageMode", request.getPackageMode());
        manifest.put("deliveryMode", request.getDeliveryMode());
        manifest.put("bundle-mode", request.getPackageMode());
        manifest.put("selfContained", !Boolean.FALSE.equals(request.getSelfContained()));
        manifest.put("offlineFull", DEPLOY_PACKAGE_MODE_OFFLINE_FULL.equals(request.getPackageMode()));
        manifest.put("directDownload", DEPLOY_PACKAGE_DELIVERY_DIRECT.equals(request.getDeliveryMode()));
        manifest.put("deleteAfterDownload", Boolean.TRUE.equals(request.getDeleteAfterDownload()));
        manifest.put("includeSource", !Boolean.FALSE.equals(request.getIncludeSource()));
        manifest.put("includeDatabases", !Boolean.FALSE.equals(request.getIncludeDatabases()));
        manifest.put("includeImages", !Boolean.FALSE.equals(request.getIncludeImages()));
        manifest.put("includeK8sManifests", !Boolean.FALSE.equals(request.getIncludeK8sManifests()));
        manifest.put("includeMinio", !Boolean.FALSE.equals(request.getIncludeMinio()));
        manifest.put("includeDockerCompose", !Boolean.FALSE.equals(request.getIncludeDockerCompose()));
        manifest.put("includeAllCurrentData", !Boolean.FALSE.equals(request.getIncludeAllCurrentData()));
        manifest.put("exportRuntimeArtifacts", Boolean.TRUE.equals(request.getExportRuntimeArtifacts()));
        manifest.put("cleanupRuntimeArtifacts", !Boolean.FALSE.equals(request.getCleanupRuntimeArtifacts()));
        manifest.put("artifactMode", request.getImageArtifactMode());
        manifest.put("imageArtifactMode", request.getImageArtifactMode());
        manifest.put("registryPrefix", request.getRegistryPrefix());
        manifest.put("registryServer", request.getRegistryServer());
        manifest.put("targetClusterType", trimToDefault(request.getTargetClusterType(), "k3s"));
        manifest.put("targetNamespace", trimToDefault(request.getTargetNamespace(), "shetuanguanlixitong"));
        manifest.put("targetDomain", resolveRemoteDeployDomain(request));
        manifest.put("storageClassName", trimToEmpty(request.getStorageClassName()));
        manifest.put("externalExposureType", trimToDefault(request.getExternalExposureType(), EXPOSURE_NONE));
        manifest.put("frontendNodePort", request.getFrontendNodePort());
        manifest.put("gatewayNodePort", request.getGatewayNodePort());
        manifest.put("ingressClassName", trimToEmpty(request.getIngressClassName()));
        manifest.put("ingressHost", trimToDefault(request.getIngressHost(), request.getTargetDomain()));
        manifest.put("databases", databases);
        List<AdminOpsServiceVO> services = listServiceRegistry();
        manifest.put("databaseExport", Map.of(
                "serverSnapshot", Boolean.TRUE.equals(request.getIncludeAllCurrentData()) && !Boolean.FALSE.equals(request.getIncludeDatabases()),
                "serverSnapshotFile", "databases/all-current-data.sql.gz",
                "sourceNodeExportScript", "scripts/export-databases.sh",
                "targetImportScript", "scripts/import-databases.sh"
        ));
        manifest.put("imageExport", Map.of(
                "sourceNodeExportScript", "scripts/export-images.sh",
                "targetImportScript", "install.sh",
                "pushToRegistryScript", "scripts/push-images-to-registry.sh",
                "cleanAfterRegistryPush", true,
                "offlineImages", IMAGE_ARTIFACT_OFFLINE_TAR.equals(request.getImageArtifactMode()) || IMAGE_ARTIFACT_BOTH.equals(request.getImageArtifactMode()),
                "registryImages", IMAGE_ARTIFACT_REGISTRY.equals(request.getImageArtifactMode()) || IMAGE_ARTIFACT_BOTH.equals(request.getImageArtifactMode()),
                "note", "Run scripts/export-all-current-data.sh on a source node to append image tar files. Registry push removes local image tar files after every image is pushed successfully."
        ));
        manifest.put("minioExport", Map.of(
                "enabled", !Boolean.FALSE.equals(request.getIncludeMinio()),
                "sourceNodeExportScript", "scripts/export-minio.sh",
                "targetImportScript", "scripts/import-minio.sh",
                "archiveDirectory", "minio"
        ));
        manifest.put("dockerCompose", Map.of(
                "enabled", !Boolean.FALSE.equals(request.getIncludeDockerCompose()),
                "file", "docker-compose.yml",
                "envFile", "compose/.env.example"
        ));
        manifest.put("k8sBundle", Map.of(
                "enabled", !Boolean.FALSE.equals(request.getIncludeK8sManifests()),
                "file", "k8s/rk-web-stack.yaml",
                "installScript", "scripts/install-k8s.sh"
        ));
        manifest.put("services", services);
        manifest.put("nacosAddr", sanitizeOpsUrl(nacosAddr));
        manifest.put("jenkinsUrl", sanitizeOpsUrl(jenkinsUrl));
        manifest.put("gatewayUrl", sanitizeOpsUrl(gatewayUrl));
        manifest.put("installScript", buildDeployInstallScript());
        manifest.put("imageExportHint", "Run scripts/export-all-current-data.sh on a source node to append real image tar files and refreshed database dumps into this bundle before moving to an offline k3s/k8s server.");
        return manifest;
    }

    private Path buildDeployPackageArchiveFile(Map<String, Object> manifest, AdminDeployPackageCreateDTO request, List<String> databases, Path runtimeArtifactDir, Long packageId) {
        Path archivePath = null;
        try {
            archivePath = Files.createTempFile("rk-deploy-package-", ".zip");
            try (OutputStream outputStream = Files.newOutputStream(archivePath, StandardOpenOption.TRUNCATE_EXISTING);
                 ZipOutputStream zip = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
                addZipEntry(zip, "manifest.json", toJson(manifest));
                addZipEntry(zip, "install.sh", buildDeployInstallScript());
                addZipEntry(zip, "offline-install.sh", buildDeployInstallScript());
                addZipEntry(zip, "scripts/install-compose.sh", buildComposeInstallScript());
                addZipEntry(zip, "scripts/install-k8s.sh", buildK8sInstallScript());
                addZipEntry(zip, "scripts/package-online.sh", buildOnlinePackageScript());
                addZipEntry(zip, "scripts/export-all-current-data.sh", buildExportAllCurrentDataScript());
                addZipEntry(zip, "scripts/export-images.sh", buildImageExportScript());
                addZipEntry(zip, "scripts/push-images-to-registry.sh", buildPushImagesToRegistryScript(request));
                addZipEntry(zip, "scripts/pull-images.sh", buildPullImagesScript(request));
                addZipEntry(zip, "runtime-artifacts/export-images.sh", buildImageExportScript());
                addZipEntry(zip, "runtime-artifacts/push-images-to-registry.sh", buildPushImagesToRegistryScript(request));
                addZipEntry(zip, "scripts/export-databases.sh", buildDatabaseExportScript(manifest));
                addZipEntry(zip, "scripts/import-databases.sh", buildDatabaseImportScript());
                addZipEntry(zip, "scripts/export-minio.sh", buildMinioExportScript());
                addZipEntry(zip, "scripts/import-minio.sh", buildMinioImportScript());
                String packageImageList = buildPackageImageList(request, runtimeArtifactDir);
                String registryImageList = readRuntimeRegistryImageList(runtimeArtifactDir);
                String runtimePushLog = readRuntimeArtifactText(runtimeArtifactDir, "runtime-push.log");
                addZipEntry(zip, "images/image-list.txt", packageImageList);
                addZipEntry(zip, "images/infra-images.txt", buildK8sInfraImageList(request));
                addZipEntry(zip, "images/application-images.txt", buildK8sApplicationImageList(request, runtimeArtifactDir));
                if (StringUtils.hasText(registryImageList)) {
                    addZipEntry(zip, "images/registry-image-list.txt", registryImageList);
                }
                addZipEntry(zip, "images/README.md", IMAGE_ARTIFACT_REGISTRY.equals(request.getImageArtifactMode())
                        ? "Registry pull package: no local image tar files are stored in this zip. Run scripts/pull-images.sh on the target server or let docker compose/k8s pull images from the configured registry.\n"
                        : "Place exported container image tar files here. scripts/package-online.sh fills this directory on a source node.\n");
                addZipEntry(zip, "upload-log.txt", StringUtils.hasText(runtimePushLog)
                        ? runtimePushLog
                        : "Registry upload has not run inside this generated zip. Run scripts/push-images-to-registry.sh to create aliyun-registry-upload.log.\n");
                addZipEntry(zip, "aliyun-registry-upload.log", StringUtils.hasText(runtimePushLog)
                        ? runtimePushLog
                        : "pending: Aliyun registry upload log will be appended by scripts/push-images-to-registry.sh\n");
                if (!Boolean.FALSE.equals(request.getIncludeDockerCompose())) {
                    addZipEntry(zip, "docker-compose.yml", buildDockerComposeFile(request));
                    addZipEntry(zip, "compose/.env.example", buildComposeEnvExample(request));
                }
                if (!Boolean.FALSE.equals(request.getIncludeDatabases()) && Boolean.TRUE.equals(request.getIncludeAllCurrentData())) {
                    addZipBytesEntry(zip, "databases/all-current-data.sql.gz", backupArchiveBuilder.buildArchive(databases, null));
                }
                addZipEntry(zip, "databases/README.md", "Place gzipped mysqldump files here. install.sh/import-databases.sh can restore them on the target cluster. The server-generated databases/all-current-data.sql.gz contains the current database snapshot when includeAllCurrentData=true.\n");
                if (!Boolean.FALSE.equals(request.getIncludeMinio())) {
                    exportMinioObjectsToZip(zip, packageId);
                }
                exportPlatformStatefulData(zip, packageId);
                if (!Boolean.FALSE.equals(request.getIncludeK8sManifests())) {
                    addZipEntry(zip, "k8s/rk-web-stack.yaml", buildK8sStackManifest(request));
                    addZipEntry(zip, "k8s/rk-web-infra.yaml", buildK8sInfraManifest(request));
                    addZipEntry(zip, "k8s/rk-web-nacos.yaml", buildK8sNacosManifest(request));
                    addZipEntry(zip, "k8s/rk-web-apps.yaml", buildK8sApplicationManifest(request));
                    String packageImagePullSecretManifest = buildPackageImagePullSecretManifest(request);
                    if (StringUtils.hasText(packageImagePullSecretManifest)) {
                        addZipEntry(zip, "k8s/rk-image-pull-secret.yaml", packageImagePullSecretManifest);
                    }
                }
                addZipEntry(zip, "k8s/README.md", "Place Kubernetes manifests here when a fully offline install needs local manifest files.\n");
                addZipEntry(zip, "k3s/k3s-install-notes.md", "Use scripts/install-k8s.sh on k3s. It imports images/*.tar through ctr -n k8s.io before applying k8s/rk-web-stack.yaml.\n");
                addZipEntry(zip, "k8s/k8s-install-notes.md", "Use scripts/install-k8s.sh on Kubernetes. Set NAMESPACE and REGISTRY_PREFIX before running when needed.\n");
                addZipDirectoryEntries(zip, runtimeArtifactDir, "runtime-artifacts/");
                addZipEntry(zip, "README.md", buildDeployPackageReadme());
            }
            return archivePath;
        } catch (Exception e) {
            if (archivePath != null) {
                try {
                    Files.deleteIfExists(archivePath);
                } catch (Exception cleanupError) {
                    log.warn("delete failed deploy package archive failed, path={}", archivePath, cleanupError);
                }
            }
            throw new IllegalStateException("build deploy package archive failed", e);
        }
    }

    private void exportMinioObjectsToZip(ZipOutputStream zip, Long packageId) throws Exception {
        updateDeployPackageProgress(packageId, 70, "exporting MinIO objects into package");
        List<Map<String, Object>> objectManifest = new ArrayList<>();
        int bucketCount = 0;
        int objectCount = 0;
        int skippedObjectCount = 0;
        long totalBytes = 0L;
        long skippedBytes = 0L;
        for (Bucket bucket : minioClient.listBuckets()) {
            String bucketName = bucket == null ? "" : bucket.name();
            if (!StringUtils.hasText(bucketName)) {
                continue;
            }
            bucketCount++;
            String bucketZipName = sanitizeMinioZipSegment(bucketName);
            Iterable<Result<Item>> objects = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .recursive(true)
                            .build()
            );
            for (Result<Item> result : objects) {
                Item item = result.get();
                if (item == null || item.isDir() || !StringUtils.hasText(item.objectName())) {
                    continue;
                }
                String objectName = item.objectName();
                if (shouldSkipMinioObjectForMigration(bucketName, objectName)) {
                    skippedObjectCount++;
                    skippedBytes += safeMinioItemSize(item);
                    continue;
                }
                String entryName = "minio/buckets/" + bucketZipName + "/" + sanitizeMinioObjectPath(objectName);
                try (InputStream inputStream = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .build()
                )) {
                    long copiedBytes = copyStreamToZip(zip, entryName, inputStream);
                    totalBytes += copiedBytes;
                    objectCount++;
                    Map<String, Object> itemManifest = new LinkedHashMap<>();
                    itemManifest.put("bucket", bucketName);
                    itemManifest.put("object", objectName);
                    itemManifest.put("zipEntry", entryName);
                    itemManifest.put("size", item.size());
                    itemManifest.put("copiedBytes", copiedBytes);
                    itemManifest.put("lastModified", item.lastModified() == null ? null : item.lastModified().toString());
                    objectManifest.add(itemManifest);
                }
            }
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("bucketCount", bucketCount);
        summary.put("objectCount", objectCount);
        summary.put("totalBytes", totalBytes);
        summary.put("skippedObjectCount", skippedObjectCount);
        summary.put("skippedBytes", skippedBytes);
        summary.put("exportedAt", LocalDateTime.now().format(TIME_FORMATTER));
        summary.put("objects", objectManifest);
        addZipEntry(zip, "minio/object-manifest.json", toJson(summary));
        addZipEntry(zip, "minio/export-summary.txt",
                "MinIO export completed\n"
                        + "bucketCount=" + bucketCount + "\n"
                        + "objectCount=" + objectCount + "\n"
                        + "totalBytes=" + totalBytes + "\n"
                        + "skippedObjectCount=" + skippedObjectCount + "\n"
                        + "skippedBytes=" + skippedBytes + "\n"
                        + "objectRoot=minio/buckets/\n");
        updateDeployPackageProgress(packageId, 72,
                "MinIO objects exported: buckets=" + bucketCount + ", objects=" + objectCount
                        + ", bytes=" + totalBytes + ", skipped=" + skippedObjectCount
                        + ", skippedBytes=" + skippedBytes);
    }

    private void exportPlatformStatefulData(ZipOutputStream zip, Long packageId) throws Exception {
        updateDeployPackageProgress(packageId, 73, "exporting Gogs and Jenkins persistent data");
        List<Map<String, Object>> archives = new ArrayList<>();
        long gogsBytes = exportPlatformPodDirectoryToZip(zip, PLATFORM_GOGS_ARCHIVE, PLATFORM_GOGS_SOURCE_POD,
                "rk-gogs", "/data", null);
        archives.add(Map.of(
                "workload", "rk-gogs",
                "sourcePod", PLATFORM_GOGS_SOURCE_POD,
                "path", "/data",
                "zipEntry", PLATFORM_GOGS_ARCHIVE,
                "bytes", gogsBytes
        ));
        long jenkinsBytes = exportPlatformPodDirectoryToZip(zip, PLATFORM_JENKINS_ARCHIVE, PLATFORM_JENKINS_SOURCE_POD,
                "rk-jenkins", "/var/jenkins_home",
                "--exclude=workspace --exclude=workspace/* --exclude=war --exclude=war/* --exclude=caches --exclude=caches/* --exclude=fingerprints --exclude=fingerprints/* --exclude=logs --exclude=logs/* --exclude=tmp --exclude=tmp/*");
        archives.add(Map.of(
                "workload", "rk-jenkins",
                "sourcePod", PLATFORM_JENKINS_SOURCE_POD,
                "path", "/var/jenkins_home",
                "zipEntry", PLATFORM_JENKINS_ARCHIVE,
                "bytes", jenkinsBytes,
                "excluded", "workspace, war, caches, fingerprints, logs, tmp"
        ));
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("exportedAt", LocalDateTime.now().format(TIME_FORMATTER));
        summary.put("archives", archives);
        addZipEntry(zip, "platform/export-summary.json", toJson(summary));
        addZipEntry(zip, "platform/export-summary.txt",
                "Platform stateful data export completed\n"
                        + "gogsBytes=" + gogsBytes + "\n"
                        + "jenkinsBytes=" + jenkinsBytes + "\n"
                        + "gogsArchive=" + PLATFORM_GOGS_ARCHIVE + "\n"
                        + "jenkinsArchive=" + PLATFORM_JENKINS_ARCHIVE + "\n");
        updateDeployPackageProgress(packageId, 74,
                "platform stateful data exported: gogs=" + formatBytes(gogsBytes)
                        + ", jenkins=" + formatBytes(jenkinsBytes));
    }

    private long exportPlatformPodDirectoryToZip(ZipOutputStream zip,
                                                 String zipEntryName,
                                                 String podName,
                                                 String containerName,
                                                 String directory,
                                                 String tarExcludes) throws Exception {
        String tarCommand = "set -e; test -d " + quoteForBash(directory)
                + "; cd " + quoteForBash(directory)
                + "; tar -czf - " + (StringUtils.hasText(tarExcludes) ? tarExcludes + " " : "") + ".";
        String encodedCommand = Base64.getEncoder().encodeToString(tarCommand.getBytes(StandardCharsets.UTF_8));
        List<String> command = new ArrayList<>();
        command.add(resolveLocalKubectlBinary());
        command.add("--request-timeout=1800s");
        String token = readK8sServiceAccountToken();
        String apiUrl = resolveK8sApiUrl();
        if (StringUtils.hasText(apiUrl) && StringUtils.hasText(token)) {
            command.add("--server=" + apiUrl);
            command.add("--token=" + token);
            if (StringUtils.hasText(k8sCaPath) && Files.isRegularFile(Path.of(k8sCaPath))) {
                command.add("--certificate-authority=" + k8sCaPath);
            } else {
                command.add("--insecure-skip-tls-verify=true");
            }
        }
        command.add("-n");
        command.add(resolveCurrentK8sNamespace());
        command.add("exec");
        command.add(podName);
        command.add("--");
        command.add("/bin/sh");
        command.add("-lc");
        command.add("printf '%s' " + quoteForBash(encodedCommand) + " | base64 -d | /bin/sh");
        Path stagedArchive = null;
        try {
            stagedArchive = Files.createTempFile("rk-platform-export-", ".tar.gz");
            long total = stageProcessOutputToGzipFile(stagedArchive, zipEntryName, command,
                    Math.max(60, deployPackageRuntimeExportTimeoutSeconds == null ? 1800 : deployPackageRuntimeExportTimeoutSeconds));
            validateGzipArchive(stagedArchive, zipEntryName);
            copyFileToZip(zip, zipEntryName, stagedArchive);
            return total;
        } finally {
            deleteQuietly(stagedArchive);
        }
    }

    private long addProcessOutputToZip(ZipOutputStream zip, String name, List<String> command, int timeoutSeconds) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectError(ProcessBuilder.Redirect.PIPE);
        Process process = builder.start();
        AtomicReference<Exception> outputError = new AtomicReference<>();
        AtomicReference<Long> copiedBytes = new AtomicReference<>(0L);
        ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
        Thread errorReader = new Thread(() -> {
            try (InputStream errorStream = process.getErrorStream()) {
                errorStream.transferTo(errorOutput);
            } catch (Exception ignored) {
                // Best-effort diagnostics only.
            }
        }, "rk-platform-export-stderr");
        errorReader.setDaemon(true);
        errorReader.start();
        ZipEntry entry = new ZipEntry(name);
        zip.putNextEntry(entry);
        Thread outputReader = new Thread(() -> {
            try (InputStream inputStream = process.getInputStream()) {
                byte[] buffer = new byte[8192];
                long total = 0L;
                int read;
                while ((read = inputStream.read(buffer)) >= 0) {
                    if (read == 0) {
                        continue;
                    }
                    zip.write(buffer, 0, read);
                    total += read;
                }
                copiedBytes.set(total);
            } catch (Exception e) {
                outputError.set(e);
            }
        }, "rk-platform-export-stdout");
        outputReader.setDaemon(true);
        outputReader.start();
        if (!process.waitFor(Math.max(5, timeoutSeconds), TimeUnit.SECONDS)) {
            process.destroyForcibly();
            outputReader.join(3000L);
            errorReader.join(3000L);
            zip.closeEntry();
            throw new IllegalStateException("export platform data command timed out: " + name);
        }
        outputReader.join();
        errorReader.join();
        zip.closeEntry();
        String errorText = errorOutput.toString(StandardCharsets.UTF_8).trim();
        if (outputError.get() != null) {
            throw new IllegalStateException("export platform data stream failed: " + name, outputError.get());
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("export platform data failed: " + name + "\n" + errorText);
        }
        long total = copiedBytes.get();
        if (total <= 0) {
            throw new IllegalStateException("export platform data produced empty archive: " + name);
        }
        return total;
    }

    private long stageProcessOutputToGzipFile(Path target, String name, List<String> command, int timeoutSeconds) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectError(ProcessBuilder.Redirect.PIPE);
        Process process = builder.start();
        AtomicReference<Exception> outputError = new AtomicReference<>();
        AtomicReference<Long> copiedBytes = new AtomicReference<>(0L);
        ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
        Thread errorReader = new Thread(() -> {
            try (InputStream errorStream = process.getErrorStream()) {
                errorStream.transferTo(errorOutput);
            } catch (Exception ignored) {
                // Best-effort diagnostics only.
            }
        }, "rk-platform-export-stderr");
        errorReader.setDaemon(true);
        errorReader.start();
        Thread outputReader = new Thread(() -> {
            try (InputStream inputStream = process.getInputStream();
                 OutputStream outputStream = Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                byte[] buffer = new byte[8192];
                long total = 0L;
                int read;
                while ((read = inputStream.read(buffer)) >= 0) {
                    if (read == 0) {
                        continue;
                    }
                    outputStream.write(buffer, 0, read);
                    total += read;
                }
                copiedBytes.set(total);
            } catch (Exception e) {
                outputError.set(e);
            }
        }, "rk-platform-export-stdout");
        outputReader.setDaemon(true);
        outputReader.start();
        if (!process.waitFor(Math.max(5, timeoutSeconds), TimeUnit.SECONDS)) {
            process.destroyForcibly();
            outputReader.join(3000L);
            errorReader.join(3000L);
            throw new IllegalStateException("export platform data command timed out: " + name);
        }
        outputReader.join();
        errorReader.join();
        String errorText = errorOutput.toString(StandardCharsets.UTF_8).trim();
        if (outputError.get() != null) {
            throw new IllegalStateException("export platform data stream failed: " + name, outputError.get());
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("export platform data failed: " + name + "\n" + errorText);
        }
        long total = copiedBytes.get();
        if (total <= 0 || !Files.isRegularFile(target) || Files.size(target) <= 0) {
            throw new IllegalStateException("export platform data produced empty archive: " + name);
        }
        return total;
    }

    private void validateGzipArchive(Path archive, String name) throws Exception {
        byte[] buffer = new byte[8192];
        try (InputStream inputStream = new GZIPInputStream(Files.newInputStream(archive))) {
            while (inputStream.read(buffer) >= 0) {
                // Drain the stream so CRC and EOF are validated.
            }
        } catch (Exception e) {
            throw new IllegalStateException("export platform data gzip validation failed: " + name, e);
        }
    }

    private long copyFileToZip(ZipOutputStream zip, String name, Path source) throws Exception {
        ZipEntry entry = new ZipEntry(name);
        zip.putNextEntry(entry);
        long total = Files.copy(source, zip);
        zip.closeEntry();
        return total;
    }

    private void deleteQuietly(Path path) {
        deleteTemporaryKubeconfigQuietly(path);
    }

    private String resolveCurrentK8sNamespace() {
        return sanitizeCleanupToken(trimToDefault(k8sCleanupNamespace, "shetuanguanlixitong"), "namespace");
    }

    private boolean shouldSkipMinioObjectForMigration(String bucketName, String objectName) {
        String normalizedObjectName = trimToEmpty(objectName).replace('\\', '/');
        while (normalizedObjectName.startsWith("/")) {
            normalizedObjectName = normalizedObjectName.substring(1);
        }
        return isSameMinioBucket(bucketName, "rk-user") && normalizedObjectName.startsWith("backup/");
    }

    private boolean isSameMinioBucket(String bucketName, String expectedBucketName) {
        return trimToEmpty(bucketName).equalsIgnoreCase(trimToEmpty(expectedBucketName));
    }

    private long safeMinioItemSize(Item item) {
        return item == null ? 0L : Math.max(0L, item.size());
    }

    private long copyStreamToZip(ZipOutputStream zip, String name, InputStream inputStream) throws Exception {
        ZipEntry entry = new ZipEntry(name);
        zip.putNextEntry(entry);
        byte[] buffer = new byte[8192];
        long total = 0L;
        int read;
        while ((read = inputStream.read(buffer)) >= 0) {
            if (read == 0) {
                continue;
            }
            zip.write(buffer, 0, read);
            total += read;
        }
        zip.closeEntry();
        return total;
    }

    private long copyStreamToFile(InputStream inputStream, Path target) throws Exception {
        byte[] buffer = new byte[8192];
        long total = 0L;
        try (OutputStream outputStream = Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            int read;
            while ((read = inputStream.read(buffer)) >= 0) {
                if (read == 0) {
                    continue;
                }
                outputStream.write(buffer, 0, read);
                total += read;
            }
        }
        return total;
    }

    private void cleanupLocalDirectoryQuietly(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> all = paths.sorted(Comparator.reverseOrder()).collect(Collectors.toList());
            for (Path path : all) {
                Files.deleteIfExists(path);
            }
        } catch (Exception e) {
            log.warn("cleanup local temporary directory failed, path={}", root, e);
        }
    }

    private String sanitizeMinioZipSegment(String value) {
        if (!StringUtils.hasText(value)) {
            return "unknown";
        }
        return value.trim().replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String sanitizeMinioObjectPath(String objectName) {
        String normalized = objectName == null ? "" : objectName.replace("\\", "/");
        List<String> parts = Arrays.stream(normalized.split("/+"))
                .map(this::sanitizeMinioZipSegment)
                .filter(StringUtils::hasText)
                .filter(part -> !".".equals(part) && !"..".equals(part))
                .collect(Collectors.toList());
        if (parts.isEmpty()) {
            return "object";
        }
        return String.join("/", parts);
    }

    private String sanitizeMinioBucketName(String bucket) {
        if (!StringUtils.hasText(bucket)) {
            throw new BadRequestException("MinIO bucket 不能为空");
        }
        String value = bucket.trim();
        if (!value.matches("[A-Za-z0-9][A-Za-z0-9._-]{1,61}[A-Za-z0-9]")) {
            throw new BadRequestException("MinIO bucket 格式不合法");
        }
        return value;
    }

    private String sanitizeMinioPrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return "";
        }
        String value = prefix.trim().replace("\\", "/");
        if (value.startsWith("/") || value.contains("../") || value.equals("..")) {
            throw new BadRequestException("MinIO prefix 格式不合法");
        }
        return value;
    }

    private String sanitizeMinioObjectNameForRead(String objectName) {
        if (!StringUtils.hasText(objectName)) {
            throw new BadRequestException("MinIO object 不能为空");
        }
        String value = objectName.trim().replace("\\", "/");
        if (value.startsWith("/") || value.contains("../") || value.equals("..")) {
            throw new BadRequestException("MinIO object 格式不合法");
        }
        return value;
    }

    private String extractObjectFileName(String objectName) {
        if (!StringUtils.hasText(objectName)) {
            return "object";
        }
        String normalized = objectName.replace("\\", "/");
        int slashIndex = normalized.lastIndexOf('/');
        String fileName = slashIndex >= 0 ? normalized.substring(slashIndex + 1) : normalized;
        return StringUtils.hasText(fileName) ? fileName : "object";
    }

    private void addZipEntry(ZipOutputStream zip, String name, String content) throws Exception {
        ZipEntry entry = new ZipEntry(name);
        zip.putNextEntry(entry);
        zip.write((content == null ? "" : content).getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private void addZipBytesEntry(ZipOutputStream zip, String name, byte[] content) throws Exception {
        ZipEntry entry = new ZipEntry(name);
        zip.putNextEntry(entry);
        zip.write(content == null ? new byte[0] : content);
        zip.closeEntry();
    }

    private void addZipDirectoryEntries(ZipOutputStream zip, Path runtimeArtifactDir, String prefix) throws Exception {
        if (runtimeArtifactDir == null || !Files.isDirectory(runtimeArtifactDir)) {
            return;
        }
        String safePrefix = StringUtils.hasText(prefix) ? prefix.replace("\\", "/") : "";
        try (Stream<Path> paths = Files.walk(runtimeArtifactDir)) {
            for (Path path : paths.filter(Files::isRegularFile).collect(Collectors.toList())) {
                String relative = runtimeArtifactDir.relativize(path).toString().replace("\\", "/");
                ZipEntry entry = new ZipEntry(safePrefix + relative);
                zip.putNextEntry(entry);
                Files.copy(path, zip);
                zip.closeEntry();
            }
        }
    }

    private String buildPackageImageList(AdminDeployPackageCreateDTO request, Path runtimeArtifactDir) {
        String registryImageList = readRuntimeRegistryImageList(runtimeArtifactDir);
        if (StringUtils.hasText(registryImageList)) {
            return registryImageList;
        }
        String workloadImageList = readRuntimeArtifactText(runtimeArtifactDir, "images/workload-images.txt");
        if (StringUtils.hasText(workloadImageList)) {
            return normalizeImageListText(workloadImageList);
        }
        List<String> images = resolveDeployPackageImageList(request == null ? new AdminDeployPackageCreateDTO() : request);
        return images.stream().collect(Collectors.joining("\n", "", "\n"));
    }

    private String buildK8sInfraImageList(AdminDeployPackageCreateDTO request) {
        AdminDeployPackageCreateDTO resolvedRequest = request == null ? new AdminDeployPackageCreateDTO() : request;
        Map<String, String> runtimeImagesByRepository = currentRuntimeImagesByRepository(resolvedRequest);
        List<String> images = new ArrayList<>();
        images.add(REMOTE_MYSQL_IMAGE);
        images.add(REMOTE_REDIS_IMAGE);
        images.add(rabbitmqImageForRuntimeComponent(resolvedRequest, runtimeImagesByRepository));
        images.add(REMOTE_MINIO_IMAGE);
        images.add(imageForRuntimeComponent(resolvedRequest, runtimeImagesByRepository, "elasticsearch:7.12.1", "elasticsearch", "rk-elasticsearch"));
        images.add(REMOTE_NACOS_IMAGE);
        images.add(REMOTE_BUSYBOX_IMAGE);
        images.add(REMOTE_GOGS_IMAGE);
        images.add(REMOTE_JENKINS_IMAGE);
        images.add(DEFAULT_MINIO_CLIENT_IMAGE);
        return images.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining("\n", "", "\n"));
    }

    private String buildK8sApplicationImageList(AdminDeployPackageCreateDTO request, Path runtimeArtifactDir) {
        AdminDeployPackageCreateDTO resolvedRequest = request == null ? new AdminDeployPackageCreateDTO() : request;
        String registryImageList = readRuntimeRegistryImageList(runtimeArtifactDir);
        List<String> registryImages = normalizeImageList(registryImageList).stream()
                .filter(image -> !isDeployPackagePlatformImage(image))
                .filter(image -> isDeployPackageBusinessRuntimeImage(image))
                .collect(Collectors.toList());
        LinkedHashSet<String> images = new LinkedHashSet<>(registryImages);
        images.addAll(resolveK8sApplicationImages(resolvedRequest));
        return images.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining("\n", "", "\n"));
    }

    private List<String> resolveK8sApplicationImages(AdminDeployPackageCreateDTO request) {
        AdminDeployPackageCreateDTO resolvedRequest = request == null ? new AdminDeployPackageCreateDTO() : request;
        Map<String, String> runtimeImagesByRepository = currentRuntimeImagesByRepository(resolvedRequest);
        LinkedHashSet<String> images = new LinkedHashSet<>();
        images.add(imageForRuntimeComponent(resolvedRequest, runtimeImagesByRepository, "seataio/seata-server:1.7.1",
                "seata", "seata-server", "rk-seata"));
        images.add(imageForRuntimeComponent(resolvedRequest, runtimeImagesByRepository, "xuxueli/xxl-job-admin:2.3.0",
                "xxl-job", "xxl-job-admin", "rk-xxl-job"));
        images.add(DEFAULT_SENTINEL_DASHBOARD_IMAGE);
        images.add(REMOTE_GOGS_IMAGE);
        images.add(REMOTE_JENKINS_IMAGE);
        SERVICE_IMAGE_MAP.keySet().stream()
                .filter(service -> !"frontend".equals(service))
                .map(service -> imageForService(resolvedRequest, service, runtimeImagesByRepository))
                .forEach(images::add);
        return images.stream()
                .filter(StringUtils::hasText)
                .filter(image -> !isDeployPackagePlatformImage(image))
                .distinct()
                .collect(Collectors.toList());
    }

    private String readRuntimeRegistryImageList(Path runtimeArtifactDir) {
        return normalizeImageListText(readRuntimeArtifactText(runtimeArtifactDir, "images/registry-image-list.txt"));
    }

    private String readRuntimeArtifactText(Path runtimeArtifactDir, String relativePath) {
        if (runtimeArtifactDir == null || !StringUtils.hasText(relativePath)) {
            return "";
        }
        Path root = runtimeArtifactDir.toAbsolutePath().normalize();
        Path path = root.resolve(relativePath).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            return "";
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("read runtime artifact text failed, path={}", path, e);
            return "";
        }
    }

    private void mergeRuntimeArtifactTextFile(Path runtimeArtifactDir, String relativePath, String previousText, boolean distinctLines) {
        if (runtimeArtifactDir == null || !StringUtils.hasText(relativePath)) {
            return;
        }
        Path root = runtimeArtifactDir.toAbsolutePath().normalize();
        Path path = root.resolve(relativePath).normalize();
        if (!path.startsWith(root)) {
            return;
        }
        String currentText = readRuntimeArtifactText(runtimeArtifactDir, relativePath);
        if (!StringUtils.hasText(previousText) && !StringUtils.hasText(currentText)) {
            return;
        }
        String merged;
        if (distinctLines) {
            merged = normalizeImageListText((previousText == null ? "" : previousText) + "\n" + (currentText == null ? "" : currentText));
        } else {
            StringBuilder builder = new StringBuilder();
            if (StringUtils.hasText(previousText)) {
                builder.append(previousText.stripTrailing()).append('\n');
            }
            if (StringUtils.hasText(currentText)) {
                builder.append(currentText.stripTrailing()).append('\n');
            }
            merged = builder.toString();
        }
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, merged, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            log.warn("merge runtime artifact text file failed, path={}", path, e);
        }
    }

    private String normalizeImageListText(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.lines()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(line -> !line.equals("no workload image list found"))
                .distinct()
                .collect(Collectors.joining("\n", "", "\n"));
    }

    private List<String> normalizeImageList(String text) {
        String normalized = normalizeImageListText(text);
        if (!StringUtils.hasText(normalized)) {
            return List.of();
        }
        return normalized.lines().collect(Collectors.toList());
    }

    private Path exportRuntimeArtifactsToDirectory(AdminDeployPackageCreateDTO request, String packageName, Long packageId) {
        try {
            Path root = Files.createTempDirectory("rk-deploy-runtime-" + packageName + "-");
            Path images = root.resolve("images");
            Path scripts = root.resolve("scripts");
            Files.createDirectories(images);
            Files.createDirectories(scripts);
            Files.writeString(scripts.resolve("export-images.sh"), buildImageExportScript(), StandardCharsets.UTF_8);
            Files.writeString(scripts.resolve("push-images-to-registry.sh"), buildPushImagesToRegistryScript(request), StandardCharsets.UTF_8);
            Files.writeString(images.resolve("image-list.txt"), buildImageListHint(), StandardCharsets.UTF_8);

            boolean keepTarForDownload = IMAGE_ARTIFACT_BOTH.equals(request.getImageArtifactMode())
                    || IMAGE_ARTIFACT_OFFLINE_TAR.equals(request.getImageArtifactMode());
            boolean sourceNodeExported = exportRuntimeArtifactsFromSourceNode(root, request, packageId);
            if (sourceNodeExported) {
                updateDeployPackageProgress(packageId, 58, "runtime artifacts collected through source-node exporter");
                return root;
            }
            if (hasSourceNodeRuntimeExportEvidence(root)
                    && !hasRuntimeImageTarFiles(root)
                    && !hasRegistryImageList(root)) {
                String message = "runtime image export failed on source node; no image tar files or registry list were produced";
                Path failureFlag = root.resolve("runtime-export-failed.flag");
                Files.writeString(failureFlag, message + "\n" + collectRuntimeExportDiagnostics(root), StandardCharsets.UTF_8);
                updateDeployPackageStep(packageId, 58, message, null, message);
                throw new IllegalStateException(message);
            }

            String script = "set -e\n"
                    + "cd " + quoteForBash(root.toString()) + "\n"
                    + "cp scripts/export-images.sh runtime-export-images.sh\n"
                    + "cp scripts/push-images-to-registry.sh runtime-push-images-to-registry.sh\n"
                    + "chmod 700 runtime-export-images.sh runtime-push-images-to-registry.sh\n"
                    + "bash runtime-export-images.sh > runtime-export.log 2>&1 || echo runtime image export failed: $? >> runtime-export.log\n"
                    + "mkdir -p runtime-summary\n"
                    + "find images -maxdepth 2 -type f | sort > runtime-summary/files.txt\n"
                    + "if find images -type f -name '*.tar' | grep -q .; then echo runtime image tar files exported from source node > runtime-summary/status.txt; else echo no runtime image tar files exported from source node > runtime-summary/status.txt; fi\n"
                    + "if [ " + quoteForBash(request.getImageArtifactMode()) + " = " + quoteForBash(IMAGE_ARTIFACT_REGISTRY) + " ] || [ " + quoteForBash(request.getImageArtifactMode()) + " = " + quoteForBash(IMAGE_ARTIFACT_BOTH) + " ]; then\n"
                    + (keepTarForDownload
                    ? "  export CLEAN_AFTER_PUSH=false\n"
                    + "  echo runtime image tar files kept for downloadable package >> runtime-summary/status.txt\n"
                    : "  echo runtime image tar files ready for registry push >> runtime-summary/status.txt\n")
                    + "fi\n";
            runLocalShellCommand(script, Math.max(60, deployPackageRuntimeExportTimeoutSeconds == null ? 1800 : deployPackageRuntimeExportTimeoutSeconds), new IllegalStateException("runtime export requested by admin"));
            updateDeployPackageProgress(packageId, 58, "runtime artifacts collected under runtime-artifacts");
            return root;
        } catch (Exception e) {
            if (isSourceNodeRuntimeExportHardFailure(e)) {
                throw new IllegalStateException("export runtime artifacts failed: " + e.getMessage(), e);
            }
            try {
                Path root = Files.createTempDirectory("rk-deploy-runtime-failed-");
                Files.writeString(root.resolve("runtime-export.log"),
                        "server-side runtime artifact export failed\n" + e.getMessage() + "\n",
                        StandardCharsets.UTF_8);
                updateDeployPackageProgress(packageId, 58, "runtime artifact export failed, failure log added to zip");
                return root;
            } catch (Exception nested) {
                throw new IllegalStateException("export runtime artifacts failed: " + e.getMessage(), e);
            }
        }
    }

    private boolean isSourceNodeRuntimeExportHardFailure(Exception e) {
        return e != null
                && StringUtils.hasText(e.getMessage())
                && e.getMessage().contains("runtime image export failed on source node");
    }

    private void runRegistryPushForRuntimeArtifacts(Path root, AdminDeployPackageCreateDTO request, Long packageId) {
        if (root == null || !Files.isDirectory(root)) {
            throw new IllegalStateException("registry image upload failed: runtime artifact directory not found");
        }
        Path failureFlag = root.resolve("runtime-export-failed.flag");
        if (Files.isRegularFile(failureFlag)) {
            String message = "registry image upload failed: " + firstMeaningfulLine(readFileQuietly(failureFlag));
            appendRuntimePushFailureLog(root, message, collectRuntimeExportDiagnostics(root));
            updateDeployPackageStep(packageId, 66, "registry image upload failed", null, message);
            throw new IllegalStateException(message);
        }
        Path script = root.resolve("scripts").resolve("push-images-to-registry.sh");
        if (!Files.isRegularFile(script)) {
            throw new IllegalStateException("registry image upload failed: push script not found");
        }
        List<String> images = readRuntimeArtifactImageList(root);
        int total = images.isEmpty() ? estimateDeployPackageImageCount() : images.size();
        updateDeployPackageRegistryProgress(packageId, "starting registry image upload", null, 0, total,
                "starting registry image upload to " + request.getRegistryPrefix());
        if (isRuntimeRegistryPushAlreadyCompleted(root)) {
            replayRuntimeRegistryPushProgress(root, packageId, total);
            updateDeployPackageRegistryProgress(packageId, "registry runtime image push completed", null, total, total,
                    "registry runtime image push completed; pull package generated successfully");
            return;
        }

        String shell = Files.isExecutable(Path.of("/bin/bash")) ? "/bin/bash" : "/bin/sh";
        ProcessBuilder builder = new ProcessBuilder(shell, script.toString());
        builder.directory(root.toFile());
        builder.redirectErrorStream(true);
        Map<String, String> env = builder.environment();
        String registryServer = trimToDefault(request.getRegistryServer(), DEFAULT_REGISTRY_SERVER);
        RegistryCredentials registryCredentials = resolveDeployPackageRegistryCredentials(registryServer);
        if ("imagePullSecret".equals(registryCredentials.source())) {
            updateDeployPackageProgress(packageId, 65, "registry credentials source: imagePullSecret");
        }
        env.put("REGISTRY_PREFIX", trimToDefault(request.getRegistryPrefix(), DEFAULT_REGISTRY_PREFIX));
        env.put("REGISTRY_SERVER", registryServer);
        env.put("REGISTRY_USERNAME", registryCredentials.username());
        env.put("REGISTRY_PASSWORD", registryCredentials.password());
        env.put("REGISTRY_CREDENTIAL_SOURCE", registryCredentials.source());
        env.put("CLEAN_AFTER_PUSH", IMAGE_ARTIFACT_BOTH.equals(request.getImageArtifactMode()) ? "false" : "true");
        env.put("UPLOAD_LOG", root.resolve("aliyun-registry-upload.log").toString());

        StringBuilder output = new StringBuilder();
        try {
            Process process = builder.start();
            AtomicReference<Exception> readerError = new AtomicReference<>();
            Thread readerThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        synchronized (output) {
                            output.append(line).append('\n');
                        }
                        applyRegistryPushProgressLine(packageId, line, total);
                    }
                } catch (Exception e) {
                    readerError.set(e);
                }
            }, "rk-registry-push-log-" + packageId);
            readerThread.setDaemon(true);
            readerThread.start();
            int timeout = Math.max(60, deployPackageRuntimeExportTimeoutSeconds == null ? 1800 : deployPackageRuntimeExportTimeoutSeconds);
            if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                String message = "registry image upload failed: timed out after " + timeout + "s";
                appendRuntimePushFailureLog(root, message, output.toString());
                updateDeployPackageStep(packageId, 66, "registry image upload failed", null, message);
                throw new IllegalStateException(message);
            }
            readerThread.join(5000L);
            if (readerError.get() != null) {
                log.warn("read registry push output failed, packageId={}", packageId, readerError.get());
            }
            if (process.exitValue() != 0) {
                String message = "registry image upload failed: " + firstMeaningfulLine(output.toString());
                appendRuntimePushFailureLog(root, message, output.toString());
                updateDeployPackageStep(packageId, 66, "registry image upload failed", null, message);
                throw new IllegalStateException(message);
            }
            Files.writeString(root.resolve("runtime-push.log"), output.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            int uploaded = countRegistryPushDoneLines(output.toString());
            int safeUploaded = uploaded > 0 ? uploaded : total;
            updateDeployPackageRegistryProgress(packageId, "registry runtime image push completed", null, safeUploaded, total,
                    "registry runtime image push completed; pull package generated successfully");
        } catch (Exception e) {
            if (e instanceof IllegalStateException) {
                throw (IllegalStateException) e;
            }
            String message = "registry image upload failed: " + e.getMessage();
            appendRuntimePushFailureLog(root, message, output.toString());
            updateDeployPackageStep(packageId, 66, "registry image upload failed", null, message);
            throw new IllegalStateException(message, e);
        }
    }

    private RegistryCredentials resolveDeployPackageRegistryCredentials(String registryServer) {
        String username = trimToEmpty(deployPackageRegistryUsername);
        String password = trimToEmpty(deployPackageRegistryPassword);
        if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
            return new RegistryCredentials(username, password, "environment");
        }
        RegistryCredentials secretCredentials = readDockerRegistryCredentialsFromImagePullSecret(registryServer);
        if (secretCredentials.hasCredentials()) {
            return secretCredentials;
        }
        return new RegistryCredentials(username, password, "none");
    }

    private RegistryCredentials readDockerRegistryCredentialsFromImagePullSecret(String registryServer) {
        String secretName = trimToDefault(deployPackageRegistryImagePullSecretName, "rk-aliyun-regcred");
        String namespace = StringUtils.hasText(k8sCleanupNamespace) ? k8sCleanupNamespace.trim() : "default";
        String token = readK8sServiceAccountToken();
        String apiUrl = resolveK8sApiUrl();
        if (!StringUtils.hasText(secretName) || !StringUtils.hasText(token) || !StringUtils.hasText(apiUrl)) {
            return RegistryCredentials.empty("imagePullSecret-unavailable");
        }
        try {
            HttpClient client = buildK8sHttpClient(apiUrl);
            JsonNode secret = getK8sJson(client, apiUrl, token,
                    "/api/v1/namespaces/" + encodePathSegment(namespace) + "/secrets/" + encodePathSegment(secretName));
            String dockerConfigJson = secret.path("data").path(".dockerconfigjson").asText("");
            if (!StringUtils.hasText(dockerConfigJson)) {
                return RegistryCredentials.empty("imagePullSecret");
            }
            JsonNode config = objectMapper.readTree(new String(Base64.getDecoder().decode(dockerConfigJson.trim()), StandardCharsets.UTF_8));
            JsonNode auths = config.path("auths");
            if (!auths.isObject()) {
                return RegistryCredentials.empty("imagePullSecret");
            }
            String normalizedServer = normalizeRegistryServerKey(registryServer);
            RegistryCredentials exact = credentialsFromDockerAuthNode(auths.path(registryServer), "imagePullSecret");
            if (exact.hasCredentials()) {
                return exact;
            }
            exact = credentialsFromDockerAuthNode(auths.path(normalizedServer), "imagePullSecret");
            if (exact.hasCredentials()) {
                return exact;
            }
            for (var entry = auths.fields(); entry.hasNext(); ) {
                Map.Entry<String, JsonNode> item = entry.next();
                if (Objects.equals(normalizeRegistryServerKey(item.getKey()), normalizedServer)) {
                    RegistryCredentials matched = credentialsFromDockerAuthNode(item.getValue(), "imagePullSecret");
                    if (matched.hasCredentials()) {
                        return matched;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("read deploy package registry credentials from imagePullSecret failed, namespace={}, secret={}, registry={}",
                    namespace, secretName, registryServer, e);
        }
        return RegistryCredentials.empty("imagePullSecret");
    }

    private RegistryCredentials credentialsFromDockerAuthNode(JsonNode node, String source) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return RegistryCredentials.empty(source);
        }
        String username = trimToEmpty(node.path("username").asText(""));
        String password = trimToEmpty(node.path("password").asText(""));
        if ((!StringUtils.hasText(username) || !StringUtils.hasText(password)) && StringUtils.hasText(node.path("auth").asText(""))) {
            String decoded = new String(Base64.getDecoder().decode(node.path("auth").asText("").trim()), StandardCharsets.UTF_8);
            int split = decoded.indexOf(':');
            if (split >= 0) {
                username = decoded.substring(0, split);
                password = decoded.substring(split + 1);
            }
        }
        return StringUtils.hasText(username) && StringUtils.hasText(password)
                ? new RegistryCredentials(username, password, source)
                : RegistryCredentials.empty(source);
    }

    private String normalizeRegistryServerKey(String value) {
        String result = trimToEmpty(value);
        if (result.startsWith("https://")) {
            result = result.substring("https://".length());
        } else if (result.startsWith("http://")) {
            result = result.substring("http://".length());
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static final class RegistryCredentials {
        private final String username;
        private final String password;
        private final String source;

        private RegistryCredentials(String username, String password, String source) {
            this.username = username == null ? "" : username;
            this.password = password == null ? "" : password;
            this.source = StringUtils.hasText(source) ? source : "none";
        }

        private static RegistryCredentials empty(String source) {
            return new RegistryCredentials("", "", source);
        }

        private boolean hasCredentials() {
            return StringUtils.hasText(username) && StringUtils.hasText(password);
        }

        private String username() {
            return username;
        }

        private String password() {
            return password;
        }

        private String source() {
            return source;
        }
    }

    private boolean hasSourceNodeRuntimeExportEvidence(Path root) {
        if (root == null || !Files.isDirectory(root)) {
            return false;
        }
        try (Stream<Path> paths = Files.list(root)) {
            return paths.anyMatch(path -> {
                String fileName = path.getFileName().toString();
                return fileName.startsWith("runtime-export-") && fileName.endsWith(".log");
            });
        } catch (Exception e) {
            log.warn("check source-node runtime export evidence failed, path={}", root, e);
            return false;
        }
    }

    private boolean hasRuntimeImageTarFiles(Path root) {
        Path images = root == null ? null : root.resolve("images");
        if (images == null || !Files.isDirectory(images)) {
            return false;
        }
        try (Stream<Path> paths = Files.walk(images)) {
            return paths.anyMatch(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".tar"));
        } catch (Exception e) {
            log.warn("check runtime image tar files failed, path={}", images, e);
            return false;
        }
    }

    private boolean hasRegistryImageList(Path root) {
        Path list = root == null ? null : root.resolve("images").resolve("registry-image-list.txt");
        if (list == null || !Files.isRegularFile(list)) {
            return false;
        }
        try {
            return Files.readAllLines(list, StandardCharsets.UTF_8).stream()
                    .map(String::trim)
                    .anyMatch(StringUtils::hasText);
        } catch (Exception e) {
            log.warn("check runtime registry image list failed, path={}", list, e);
            return false;
        }
    }

    private int mergeRuntimeRegistryImagesFromPushLogs(Path root, String logs) {
        if (root == null || !Files.isDirectory(root) || !StringUtils.hasText(logs)) {
            return 0;
        }
        Path list = root.resolve("images").resolve("registry-image-list.txt");
        LinkedHashSet<String> images = new LinkedHashSet<>();
        if (Files.isRegularFile(list)) {
            try {
                Files.readAllLines(list, StandardCharsets.UTF_8).stream()
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .forEach(images::add);
            } catch (Exception e) {
                log.warn("read runtime registry image list before log merge failed, path={}", list, e);
            }
        }
        int before = images.size();
        for (String line : logs.split("\\R")) {
            String trimmed = line == null ? "" : line.trim();
            if (!trimmed.startsWith("__RK_REGISTRY_PUSH__ DONE ")) {
                continue;
            }
            String[] parts = trimmed.split("\\s+", 6);
            if (parts.length < 6) {
                continue;
            }
            String target = parts[5].trim();
            if (StringUtils.hasText(target) && target.contains("/") && target.contains(":")) {
                images.add(target);
            }
        }
        int added = images.size() - before;
        if (added <= 0) {
            return 0;
        }
        try {
            Files.createDirectories(list.getParent());
            Files.writeString(list, images.stream().collect(Collectors.joining("\n", "", "\n")),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.writeString(root.resolve("runtime-push.log"), logs,
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            log.warn("merge runtime registry image list from push logs failed, path={}", list, e);
            return 0;
        }
        return added;
    }

    private String collectRuntimeExportDiagnostics(Path root) {
        if (root == null || !Files.isDirectory(root)) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (Stream<Path> paths = Files.list(root)) {
            List<Path> logs = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        return fileName.startsWith("runtime-export") && fileName.endsWith(".log");
                    })
                    .sorted()
                    .collect(Collectors.toList());
            for (Path logFile : logs) {
                for (String line : Files.readAllLines(logFile, StandardCharsets.UTF_8)) {
                    if (isRuntimeExportDiagnosticLine(line)) {
                        builder.append(logFile.getFileName()).append(": ").append(line.trim()).append('\n');
                    }
                }
            }
        } catch (Exception e) {
            log.warn("collect runtime export diagnostics failed, path={}", root, e);
        }
        return builder.toString();
    }

    private boolean isRuntimeExportDiagnosticLine(String line) {
        if (!StringUtils.hasText(line)) {
            return false;
        }
        String lower = line.toLowerCase();
        return lower.contains("failed to dial")
                || lower.contains("unauthorized")
                || lower.contains("denied")
                || lower.contains("permission")
                || lower.contains("not found")
                || lower.contains("context deadline exceeded")
                || lower.contains("registry image upload failed")
                || lower.contains("runtime image export failed");
    }

    private String readFileQuietly(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("read file failed, path={}", path, e);
            return "";
        }
    }

    private boolean isRuntimeRegistryPushAlreadyCompleted(Path root) {
        Path pushLog = root.resolve("runtime-push.log");
        if (!Files.isRegularFile(pushLog)) {
            pushLog = root.resolve("aliyun-registry-upload.log");
        }
        if (!Files.isRegularFile(pushLog)) {
            return false;
        }
        try {
            String text = Files.readString(pushLog, StandardCharsets.UTF_8);
            return text.contains("__RK_REGISTRY_PUSH__ COMPLETE")
                    || text.contains("local image tar files cleaned after registry push");
        } catch (Exception e) {
            log.warn("read runtime registry push log failed, path={}", pushLog, e);
            return false;
        }
    }

    private void replayRuntimeRegistryPushProgress(Path root, Long packageId, int fallbackTotal) {
        for (String fileName : List.of("runtime-push.log", "aliyun-registry-upload.log")) {
            Path path = root.resolve(fileName);
            if (!Files.isRegularFile(path)) {
                continue;
            }
            try {
                for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                    applyRegistryPushProgressLine(packageId, line, fallbackTotal);
                }
                return;
            } catch (Exception e) {
                log.warn("replay registry push progress failed, path={}", path, e);
            }
        }
    }

    private List<String> readRuntimeArtifactImageList(Path root) {
        Path workload = root.resolve("images").resolve("workload-images.txt");
        Path fallback = root.resolve("images").resolve("image-list.txt");
        Path source = Files.isRegularFile(workload) ? workload : fallback;
        if (!Files.isRegularFile(source)) {
            return Collections.emptyList();
        }
        try {
            return Files.readAllLines(source, StandardCharsets.UTF_8).stream()
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .filter(line -> !line.equals("no workload image list found"))
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("read runtime artifact image list failed, path={}", source, e);
            return Collections.emptyList();
        }
    }

    private void applyRegistryPushProgressLine(Long packageId, String line, int fallbackTotal) {
        if (!StringUtils.hasText(line)) {
            return;
        }
        String trimmed = line.trim();
        if (!trimmed.startsWith("__RK_REGISTRY_PUSH__")) {
            return;
        }
        String[] parts = trimmed.split("\\s+", 6);
        String action = parts.length > 1 ? parts[1] : "";
        if ("START".equals(action)) {
            int total = parseRegistryProgressTotal(trimmed, fallbackTotal);
            updateDeployPackageRegistryProgress(packageId, "starting registry image upload", null, 0, total, trimmed);
            return;
        }
        if ("IMAGE".equals(action) || "DONE".equals(action) || "FAIL".equals(action)) {
            int current = parts.length > 2 ? parsePositiveInt(parts[2], 0) : 0;
            int total = parts.length > 3 ? parsePositiveInt(parts[3], fallbackTotal) : fallbackTotal;
            String image = parts.length > 4 ? parts[4] : null;
            int uploaded = "DONE".equals(action) ? current : Math.max(0, current - 1);
            String step = "FAIL".equals(action) ? "registry image upload failed" : "pushing registry image";
            updateDeployPackageRegistryProgress(packageId, step, image, uploaded, total, trimmed);
            return;
        }
        if ("CLEANED".equals(action)) {
            updateDeployPackageStep(packageId, 66, "local image tar files cleaned after registry push", null,
                    "local image tar files cleaned after registry push");
            return;
        }
        if ("ERROR".equals(action) || "DIAGNOSTIC".equals(action) || "DIAGNOSTICS_BEGIN__".equals(action) || "DIAGNOSTICS_END__".equals(action)) {
            updateDeployPackageStep(packageId, 66, "registry image upload failed", null, trimmed);
        }
    }

    private int parseRegistryProgressTotal(String line, int fallbackTotal) {
        String[] parts = line.split("\\s+");
        for (String part : parts) {
            if (part.startsWith("total=")) {
                return parsePositiveInt(part.substring("total=".length()), fallbackTotal);
            }
        }
        return fallbackTotal;
    }

    private int parsePositiveInt(String value, int fallback) {
        if (!StringUtils.hasText(value)) {
            return Math.max(0, fallback);
        }
        try {
            return Math.max(0, Integer.parseInt(value.replaceAll("[^0-9]", "")));
        } catch (Exception e) {
            return Math.max(0, fallback);
        }
    }

    private int countRegistryPushDoneLines(String output) {
        if (!StringUtils.hasText(output)) {
            return 0;
        }
        int count = 0;
        for (String line : output.split("\\R")) {
            if (line.trim().startsWith("__RK_REGISTRY_PUSH__ DONE")) {
                count++;
            }
        }
        return count;
    }

    private void assertRuntimeExportDiskHeadroom(Long packageId) {
        assertRuntimeExportDiskHeadroom(packageId, false);
    }

    private void assertRuntimeExportDiskHeadroom(Long packageId, boolean relaxPercentWhenEnoughFreeSpace) {
        int threshold = Math.max(1, Math.min(
                deployPackageRuntimeExportMaxDiskUsedPercent == null
                        ? DEPLOY_PACKAGE_RUNTIME_EXPORT_MAX_DISK_USED_PERCENT
                        : deployPackageRuntimeExportMaxDiskUsedPercent,
                99));
        int headroom = Math.max(0, Math.min(
                deployPackageRuntimeExportHeadroomPercent == null
                        ? DEPLOY_PACKAGE_RUNTIME_EXPORT_HEADROOM_PERCENT
                        : deployPackageRuntimeExportHeadroomPercent,
                threshold - 1));
        int protectedLimit = threshold - headroom;
        List<AdminK8sNodeVO> nodes = resolveRuntimeExportNodes();
        if (nodes.isEmpty()) {
            updateDeployPackageProgress(packageId, 49, "runtime image export disk preflight skipped: no ready node found");
            return;
        }
        updateDeployPackageProgress(packageId, 49,
                "runtime image export disk preflight: max allowed root usage " + threshold
                        + "%, required headroom " + headroom + "%");
        int maxUsedPercent = 0;
        String maxNode = "";
        int minAvailableMb = Integer.MAX_VALUE;
        int readableNodes = 0;
        StringBuilder summary = new StringBuilder();
        for (AdminK8sNodeVO node : nodes) {
            if (!StringUtils.hasText(node.getName())) {
                continue;
            }
            try {
                String output = readRuntimeExportDiskUsage(node);
                int usedPercent = parseMaxDiskUsedPercent(output);
                int availableMb = parseMinDiskAvailableMb(output);
                readableNodes++;
                if (usedPercent >= maxUsedPercent) {
                    maxUsedPercent = usedPercent;
                    maxNode = node.getName();
                }
                if (availableMb > 0) {
                    minAvailableMb = Math.min(minAvailableMb, availableMb);
                }
                summary.append(node.getName()).append(" root=").append(usedPercent).append("%");
                if (availableMb > 0) {
                    summary.append(", free=").append(availableMb).append("MiB");
                }
                summary.append("; ");
                updateDeployPackageProgress(packageId, 49,
                        "runtime image export disk preflight " + node.getName() + ": " + usedPercent + "% used"
                                + (availableMb > 0 ? ", " + availableMb + "MiB free" : ""));
            } catch (Exception e) {
                log.warn("runtime image export disk preflight failed, node={}", node.getName(), e);
                updateDeployPackageProgress(packageId, 49,
                        "runtime image export disk preflight unavailable on " + node.getName() + ": " + e.getMessage());
            }
        }
        if (readableNodes == 0) {
            String message = "runtime image export blocked because disk preflight was unavailable on every node";
            updateDeployPackageStep(packageId, 49,
                    "runtime image export blocked because disk preflight was unavailable on every node",
                    null,
                    message);
            throw new IllegalStateException(message);
        }
        if (maxUsedPercent > protectedLimit) {
            if (relaxPercentWhenEnoughFreeSpace
                    && minAvailableMb != Integer.MAX_VALUE
                    && minAvailableMb >= REMOTE_DEPLOY_REGISTRY_MIN_FREE_MB) {
                updateDeployPackageProgress(packageId, 49,
                        "runtime image export disk preflight warning accepted for registry-only remote deploy: "
                                + maxNode + " " + maxUsedPercent + "% used, min free "
                                + minAvailableMb + "MiB");
                return;
            }
            String message = "runtime image export blocked because node disk usage is above threshold: "
                    + maxNode + " " + maxUsedPercent + "% + headroom " + headroom + "% > " + threshold + "%; "
                    + "run K8s cleanup before exporting or pushing runtime images";
            updateDeployPackageStep(packageId, 49,
                    "runtime image export blocked because node disk usage is above threshold",
                    maxNode,
                    message);
            throw new IllegalStateException(message);
        }
        updateDeployPackageProgress(packageId, 49,
                "runtime image export disk preflight passed: " + summary);
    }

    private String readRuntimeExportDiskUsage(AdminK8sNodeVO node) throws Exception {
        String token = readK8sServiceAccountToken();
        String apiUrl = resolveK8sApiUrl();
        if (!StringUtils.hasText(token) || !StringUtils.hasText(apiUrl)) {
            throw new IllegalStateException("K8s API token unavailable");
        }
        String namespace = StringUtils.hasText(k8sCleanupNamespace) ? k8sCleanupNamespace.trim() : "default";
        String podName = "rk-runtime-disk-preflight-" + System.currentTimeMillis() + "-" + Math.abs(node.getName().hashCode());
        String script = "set -eu\n"
                + "echo 'runtime image export disk preflight'\n"
                + "df -Pm / /var/lib/rancher /var/lib/containerd /tmp 2>/dev/null || df -Pm /\n";
        String encodedScript = Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_8));
        HttpClient client = buildK8sHttpClient(apiUrl);
        Map<String, Object> manifest = buildK8sCleanupPodManifest(podName, namespace, node, encodedScript);
        try {
            sendK8sJson(client, apiUrl, token, "POST", "/api/v1/namespaces/" + encodePathSegment(namespace) + "/pods", manifest, 201);
            String phase = waitK8sCleanupPodPhase(client, apiUrl, token, namespace, podName);
            String logs = readK8sPodLogs(client, apiUrl, token, namespace, podName);
            if (!Objects.equals("Succeeded", phase)) {
                throw new IllegalStateException("disk preflight pod phase=" + phase + "\n" + logs);
            }
            return logs;
        } finally {
            deleteK8sCleanupPod(client, apiUrl, token, namespace, podName);
        }
    }

    private int parseMaxDiskUsedPercent(String output) {
        if (!StringUtils.hasText(output)) {
            return 0;
        }
        int max = 0;
        for (String line : output.split("\\R")) {
            for (String part : line.trim().split("\\s+")) {
                if (part.endsWith("%")) {
                    max = Math.max(max, parsePositiveInt(part, 0));
                }
            }
        }
        return max;
    }

    private int parseMinDiskAvailableMb(String output) {
        if (!StringUtils.hasText(output)) {
            return 0;
        }
        int min = Integer.MAX_VALUE;
        for (String line : output.split("\\R")) {
            String trimmed = line.trim();
            if (!StringUtils.hasText(trimmed) || trimmed.startsWith("Filesystem")) {
                continue;
            }
            String[] parts = trimmed.split("\\s+");
            if (parts.length < 5 || !parts[4].endsWith("%")) {
                continue;
            }
            int available = parsePositiveInt(parts[3], 0);
            if (available > 0) {
                min = Math.min(min, available);
            }
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    private void appendRuntimePushFailureLog(Path root, String message, String output) {
        try {
            Files.writeString(root.resolve("runtime-push.log"),
                    message + "\n" + (output == null ? "" : output),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (Exception logError) {
            log.warn("append runtime push failure log failed, path={}", root, logError);
        }
    }

    private boolean exportRuntimeArtifactsFromSourceNode(Path root, AdminDeployPackageCreateDTO request, Long packageId) {
        String token = readK8sServiceAccountToken();
        String apiUrl = resolveK8sApiUrl();
        if (!StringUtils.hasText(token) || !StringUtils.hasText(apiUrl)) {
            return false;
        }
        List<AdminK8sNodeVO> nodes = resolveRuntimeExportNodes();
        if (nodes.isEmpty()) {
            return false;
        }
        List<String> images = loadCurrentWorkloadImagesForRuntimeExport(request);
        if (images.isEmpty()) {
            return false;
        }
        try {
            HttpClient client = buildK8sHttpClient(apiUrl);
            String namespace = StringUtils.hasText(k8sCleanupNamespace) ? k8sCleanupNamespace.trim() : "default";
            boolean exportedAny = false;
            for (AdminK8sNodeVO node : nodes) {
                if (!StringUtils.hasText(node.getName())) {
                    continue;
                }
                String podName = "rk-runtime-export-" + System.currentTimeMillis() + "-" + Math.abs(node.getName().hashCode());
                String script = buildRuntimeImageExportNodeScript(request, images);
                String encodedScript = Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_8));
                Map<String, Object> manifest = buildRuntimeImageExportPodManifest(podName, namespace, node, encodedScript);
                try {
                    sendK8sJson(client, apiUrl, token, "POST", "/api/v1/namespaces/" + encodePathSegment(namespace) + "/pods", manifest, 201);
                    String phase = waitRuntimeImageExportPodPhase(client, apiUrl, token, namespace, podName, packageId, images.size());
                    String logs = readK8sPodLogs(client, apiUrl, token, namespace, podName, "runtime-export", 1000);
                    Files.writeString(root.resolve("runtime-export-" + safeArtifactName(node.getName()) + ".log"), logs, StandardCharsets.UTF_8);
                    int recoveredRegistryImages = mergeRuntimeRegistryImagesFromPushLogs(root, logs);
                    if (recoveredRegistryImages > 0) {
                        exportedAny = true;
                        updateDeployPackageProgress(packageId, 57,
                                "runtime registry image list recovered from source-node logs on " + node.getName()
                                        + ": " + recoveredRegistryImages);
                    }
                    if (!Objects.equals("Succeeded", phase)) {
                        if (recoveredRegistryImages > 0 && IMAGE_ARTIFACT_REGISTRY.equals(trimToDefault(request.getImageArtifactMode(), IMAGE_ARTIFACT_BOTH))) {
                            updateDeployPackageProgress(packageId, 56,
                                    "runtime image export pod did not finish cleanly on " + node.getName()
                                            + ", but registry push results were recovered");
                            continue;
                        }
                        updateDeployPackageProgress(packageId, 56, "runtime image export pod failed on " + node.getName());
                        continue;
                    }
                    boolean extracted = extractRuntimeImagesArchive(root, logs, packageId, node.getName());
                    if (!extracted) {
                        recordRuntimeArchiveSkipped(root, logs, packageId, node.getName());
                    }
                    exportedAny = extracted || exportedAny;
                } catch (Exception e) {
                    log.warn("runtime image export through source node failed, node={}", node.getName(), e);
                    Files.writeString(root.resolve("runtime-export-" + safeArtifactName(node.getName()) + ".log"),
                            "runtime image export through privileged source-node pod failed\n" + e.getMessage() + "\n",
                            StandardCharsets.UTF_8);
                } finally {
                    deleteK8sCleanupPod(client, apiUrl, token, namespace, podName);
                }
            }
            return exportedAny;
        } catch (Exception e) {
            log.warn("runtime image source-node exporter unavailable", e);
            return false;
        }
    }

    Map<String, Object> buildRuntimeImageExportPodManifest(String podName, String namespace, AdminK8sNodeVO targetNode, String encodedScript) {
        String cleanupImage = StringUtils.hasText(k8sCleanupImage) ? k8sCleanupImage.trim() : "m.daocloud.io/docker.io/library/busybox:1.36";
        String command = "printf '%s' '" + encodedScript + "' | base64 -d > /host/tmp/rk-runtime-export.sh "
                + "&& chmod 700 /host/tmp/rk-runtime-export.sh "
                + "&& chroot /host /bin/sh /tmp/rk-runtime-export.sh; "
                + "status=$?; rm -f /host/tmp/rk-runtime-export.sh; exit $status";
        return Map.of(
                "apiVersion", "v1",
                "kind", "Pod",
                "metadata", Map.of(
                        "name", podName,
                        "namespace", namespace,
                        "labels", Map.of(
                                "app", "rk-runtime-export",
                                "managed-by", "rk-user"
                        )
                ),
                "spec", Map.of(
                        "restartPolicy", "Never",
                        "nodeName", targetNode.getName(),
                        "hostPID", true,
                        "tolerations", List.of(Map.of("operator", "Exists")),
                        "containers", List.of(Map.of(
                                "name", "runtime-export",
                                "image", cleanupImage,
                                "imagePullPolicy", "IfNotPresent",
                                "command", List.of("/bin/sh", "-c"),
                                "args", List.of(command),
                                "securityContext", Map.of("privileged", true),
                                "volumeMounts", List.of(Map.of(
                                        "name", "host-root",
                                        "mountPath", "/host"
                                ))
                        )),
                        "volumes", List.of(Map.of(
                                "name", "host-root",
                                "hostPath", Map.of(
                                        "path", "/",
                                        "type", "Directory"
                                )
                        ))
                )
        );
    }

    private String buildRuntimeImageExportNodeScript(AdminDeployPackageCreateDTO request, List<String> images) {
        String imageList = images.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining("\n", "", "\n"));
        String encodedImages = Base64.getEncoder().encodeToString(imageList.getBytes(StandardCharsets.UTF_8));
        String registryPrefix = trimToDefault(request.getRegistryPrefix(), DEFAULT_REGISTRY_PREFIX);
        String registryServer = trimToDefault(request.getRegistryServer(), DEFAULT_REGISTRY_SERVER);
        String artifactMode = trimToDefault(request.getImageArtifactMode(), IMAGE_ARTIFACT_BOTH);
        boolean registryOnly = IMAGE_ARTIFACT_REGISTRY.equals(artifactMode);
        boolean registryEnabled = IMAGE_ARTIFACT_REGISTRY.equals(artifactMode) || IMAGE_ARTIFACT_BOTH.equals(artifactMode);
        RegistryCredentials registryCredentials = resolveDeployPackageRegistryCredentials(registryServer);
        String maxLogArchiveBytes = Long.toString(MAX_RUNTIME_LOG_ARCHIVE_BYTES);
        return "set -eu\n"
                + "WORK=/tmp/rk-runtime-export-$$\n"
                + "ARCHIVE=/tmp/rk-runtime-images.tgz\n"
                + "MAX_RUNTIME_LOG_ARCHIVE_BYTES=" + quoteForBash(maxLogArchiveBytes) + "\n"
                + "REGISTRY_ENABLED=" + (registryEnabled ? "true" : "false") + "\n"
                + "REGISTRY_ONLY=" + (registryOnly ? "true" : "false") + "\n"
                + "REGISTRY_PREFIX=" + quoteForBash(registryPrefix) + "\n"
                + "REGISTRY_SERVER=" + quoteForBash(registryServer) + "\n"
                + "REGISTRY_USERNAME=" + quoteForBash(registryCredentials.username()) + "\n"
                + "REGISTRY_PASSWORD=" + quoteForBash(registryCredentials.password()) + "\n"
                + "REGISTRY_CREDENTIAL_SOURCE=" + quoteForBash(registryCredentials.source()) + "\n"
                + "rm -rf \"$WORK\" \"$ARCHIVE\" \"$ARCHIVE.b64\"\n"
                + "mkdir -p \"$WORK/images\" \"$WORK/runtime-summary\"\n"
                + "cd \"$WORK\"\n"
                + "printf '%s' " + quoteForBash(encodedImages) + " | base64 -d > images/workload-images.txt\n"
                + "cp images/workload-images.txt images/image-list.txt\n"
                + ": > runtime-push.log\n"
                + ": > images/registry-image-list.txt\n"
                + ": > images/exported-images.txt\n"
                + "echo 'runtime image export through privileged source-node pod' > runtime-export.log\n"
                + "echo \"node=$(hostname)\" >> runtime-export.log\n"
                + "CTR=$(command -v ctr 2>/dev/null || true)\n"
                + "[ -n \"$CTR\" ] || [ ! -x /var/lib/rancher/rke2/bin/ctr ] || CTR=/var/lib/rancher/rke2/bin/ctr\n"
                + "[ -n \"$CTR\" ] || [ ! -x /var/lib/rancher/k3s/data/current/bin/ctr ] || CTR=/var/lib/rancher/k3s/data/current/bin/ctr\n"
                + "CONTAINERD_ADDRESS=\n"
                + "for sock in /run/k3s/containerd/containerd.sock /run/rke2/containerd/containerd.sock /run/containerd/containerd.sock; do if [ -S \"$sock\" ]; then CONTAINERD_ADDRESS=\"$sock\"; break; fi; done\n"
                + "ctr_cmd() { if [ -n \"$CONTAINERD_ADDRESS\" ]; then \"$CTR\" --address \"$CONTAINERD_ADDRESS\" \"$@\"; else \"$CTR\" \"$@\"; fi; }\n"
                + "RUNTIME_ARCH=$(uname -m | sed 's/x86_64/amd64/;s/aarch64/arm64/;s/armv7l/arm\\/v7/;s/armv6l/arm\\/v6/')\n"
                + "RUNTIME_PLATFORM=${RUNTIME_PLATFORM:-linux/$RUNTIME_ARCH}\n"
                + "if [ -n \"$CTR\" ]; then echo \"ctr=$CTR\" >> runtime-export.log; echo \"CONTAINERD_ADDRESS=${CONTAINERD_ADDRESS:-default}\" >> runtime-export.log; echo \"RUNTIME_PLATFORM=$RUNTIME_PLATFORM\" >> runtime-export.log; fi\n"
                + "exported=0\n"
                + "nodeMissed=0\n"
                + "hardFailed=0\n"
                + "total=$(awk 'NF{count++} END{print count+0}' images/workload-images.txt)\n"
                + "classify_export_failure() {\n"
                + "  failed_image=\"$1\"\n"
                + "  detail_file=\"$2\"\n"
                + "  if grep -Eiq 'not found|not exist|no such image|image not known|not present|not found in namespace|does not exist' \"$detail_file\"; then\n"
                + "    nodeMissed=$((nodeMissed + 1))\n"
                + "    echo \"image not present on source node: $failed_image\" >> runtime-export.log\n"
                + "  else\n"
                + "    hardFailed=$((hardFailed + 1))\n"
                + "    echo \"hard image export failure: $failed_image\" >> runtime-export.log\n"
                + "  fi\n"
                + "}\n"
                + "image_candidates() {\n"
                + "  candidate=\"$1\"\n"
                + "  echo \"$candidate\"\n"
                + "  case \"$candidate\" in\n"
                + "    docker.io/*/*) ;;\n"
                + "    docker.io/*) rest=\"${candidate#docker.io/}\"; echo \"docker.io/library/$rest\" ;;\n"
                + "  esac\n"
                + "}\n"
                + "while IFS= read -r image; do\n"
                + "  [ -n \"$image\" ] || continue\n"
                + "  safe=$(echo \"$image\" | tr '/:@' '___')\n"
                + "  per_image_log=\"runtime-export-${safe}.tmp\"\n"
                + "  rm -f \"$per_image_log\"\n"
                + "  export_source=\n"
                + "  for candidate in $(image_candidates \"$image\"); do\n"
                + "    candidate_safe=$(echo \"$candidate\" | tr '/:@' '___')\n"
                + "    candidate_log=\"runtime-export-${candidate_safe}.tmp\"\n"
                + "    rm -f \"$candidate_log\"\n"
                + "    if [ -n \"$CTR\" ]; then\n"
                + "      if ctr_cmd -n k8s.io images export \"images/${candidate_safe}.tar\" \"$candidate\" > \"$candidate_log\" 2>&1; then cat \"$candidate_log\" >> runtime-export.log; export_source=\"$candidate\"; rm -f \"$candidate_log\"; break; else [ \"$candidate_log\" = \"$per_image_log\" ] || cat \"$candidate_log\" >> \"$per_image_log\"; fi\n"
                + "    elif command -v docker >/dev/null 2>&1; then\n"
                + "      if docker save \"$candidate\" -o \"images/${candidate_safe}.tar\" > \"$candidate_log\" 2>&1; then cat \"$candidate_log\" >> runtime-export.log; export_source=\"$candidate\"; rm -f \"$candidate_log\"; break; else [ \"$candidate_log\" = \"$per_image_log\" ] || cat \"$candidate_log\" >> \"$per_image_log\"; fi\n"
                + "    fi\n"
                + "    [ \"$candidate_log\" = \"$per_image_log\" ] || rm -f \"$candidate_log\"\n"
                + "  done\n"
                + "  if [ -n \"$export_source\" ]; then\n"
                + "    exported=$((exported + 1))\n"
                + "    echo \"$export_source\" >> images/exported-images.txt\n"
                + "  elif [ -n \"$CTR\" ] || command -v docker >/dev/null 2>&1; then\n"
                + "    [ -s \"$per_image_log\" ] || echo \"runtime image export produced no detail for $image\" > \"$per_image_log\"\n"
                + "    cat \"$per_image_log\" >> runtime-export.log\n"
                + "    classify_export_failure \"$image\" \"$per_image_log\"\n"
                + "  else\n"
                + "    echo 'no ctr/docker runtime found on source node' >> runtime-export.log\n"
                + "    hardFailed=$((hardFailed + 1))\n"
                + "  fi\n"
                + "  rm -f \"$per_image_log\"\n"
                + "done < images/workload-images.txt\n"
                + "echo \"exported=$exported\" > runtime-summary/status.txt\n"
                + "echo \"node_missed=$nodeMissed\" >> runtime-summary/status.txt\n"
                + "echo \"hard_failed=$hardFailed\" >> runtime-summary/status.txt\n"
                + "echo 'runtime node misses are expected on multi-node clusters' >> runtime-summary/status.txt\n"
                + "find images -maxdepth 1 -type f -name '*.tar' | sort > runtime-summary/files.txt\n"
                + "if [ \"$REGISTRY_ENABLED\" = \"true\" ]; then\n"
                + "  push_list=images/exported-images.txt\n"
                + "  push_total=$(awk 'NF{count++} END{print count+0}' \"$push_list\")\n"
                + "  total=\"$push_total\"\n"
                + "  if [ \"$push_total\" -eq 0 ]; then\n"
                + "    echo 'no images exported on this source node; registry push skipped' | tee -a runtime-push.log\n"
                + "    echo \"__RK_REGISTRY_PUSH__ START total=0\" | tee -a runtime-push.log\n"
                + "    echo \"__RK_REGISTRY_PUSH__ COMPLETE 0 0\" | tee -a runtime-push.log\n"
                + "  else\n"
                + "  echo 'source-node registry push started' | tee -a runtime-push.log\n"
                + "  echo \"registry push requested: $REGISTRY_PREFIX ($REGISTRY_SERVER)\" >> runtime-push.log\n"
                + "  echo \"registry credentials source: $REGISTRY_CREDENTIAL_SOURCE\" | tee -a runtime-push.log\n"
                + "  docker_state=missing\n"
                + "  if command -v docker >/dev/null 2>&1; then docker_state=present; fi\n"
                + "  echo \"__RK_REGISTRY_PUSH__ DIAGNOSTIC runtime registry push runtime: ctr=$CTR docker=$docker_state containerd=${CONTAINERD_ADDRESS:-default}\" | tee -a runtime-push.log\n"
                + "  echo \"__RK_REGISTRY_PUSH__ START total=$total\" | tee -a runtime-push.log\n"
                + "  current=0\n"
                + "  pushed_count=0\n"
                + "  failed_count=0\n"
                + "  emit_ctr_push_failure() {\n"
                + "    failed_image=\"$1\"\n"
                + "    failed_target=\"$2\"\n"
                + "    echo \"__RK_REGISTRY_PUSH__ DIAGNOSTIC ctr push failed for $failed_image -> $failed_target\" | tee -a runtime-push.log\n"
                + "    echo \"__RK_REGISTRY_PUSH__ DIAGNOSTIC ctr push failed tail follows\" | tee -a runtime-push.log\n"
                + "    tail -n 16 runtime-push.log | sed 's/^/__RK_REGISTRY_PUSH__ DIAGNOSTIC /' | tee -a runtime-push.log\n"
                + "  }\n"
                + "  while IFS= read -r image; do\n"
                + "    [ -n \"$image\" ] || continue\n"
                + "    current=$((current + 1))\n"
                + "    repo=$(echo \"$image\" | awk -F/ '{print $NF}' | awk -F: '{print $1}')\n"
                + "    tag=$(echo \"$image\" | awk -F: '{print $NF}')\n"
                + "    [ \"$tag\" = \"$image\" ] && tag=latest\n"
                + "    target=\"$REGISTRY_PREFIX/$repo:$tag\"\n"
                + "    echo \"__RK_REGISTRY_PUSH__ IMAGE $current $total $image $target\" | tee -a runtime-push.log\n"
                + "    if [ -n \"$CTR\" ]; then\n"
                + "      if [ -n \"$REGISTRY_USERNAME\" ] && [ -n \"$REGISTRY_PASSWORD\" ]; then\n"
                + "        if ctr_cmd -n k8s.io images tag --force \"$image\" \"$target\" >> runtime-push.log 2>&1 && ctr_cmd -n k8s.io images push --platform \"$RUNTIME_PLATFORM\" --user \"$REGISTRY_USERNAME:$REGISTRY_PASSWORD\" \"$target\" >> runtime-push.log 2>&1; then\n"
                + "          pushed_count=$((pushed_count + 1))\n"
                + "          echo \"$target\" >> images/registry-image-list.txt\n"
                + "          echo \"__RK_REGISTRY_PUSH__ DONE $current $total $image $target\" | tee -a runtime-push.log\n"
                + "        else\n"
                + "          failed_count=$((failed_count + 1))\n"
                + "          echo \"__RK_REGISTRY_PUSH__ FAIL $current $total $image $target\" | tee -a runtime-push.log\n"
                + "          emit_ctr_push_failure \"$image\" \"$target\"\n"
                + "        fi\n"
                + "      else\n"
                + "        if ctr_cmd -n k8s.io images tag --force \"$image\" \"$target\" >> runtime-push.log 2>&1 && ctr_cmd -n k8s.io images push --platform \"$RUNTIME_PLATFORM\" \"$target\" >> runtime-push.log 2>&1; then\n"
                + "          pushed_count=$((pushed_count + 1))\n"
                + "          echo \"$target\" >> images/registry-image-list.txt\n"
                + "          echo \"__RK_REGISTRY_PUSH__ DONE $current $total $image $target\" | tee -a runtime-push.log\n"
                + "        else\n"
                + "          failed_count=$((failed_count + 1))\n"
                + "          echo \"__RK_REGISTRY_PUSH__ FAIL $current $total $image $target\" | tee -a runtime-push.log\n"
                + "          emit_ctr_push_failure \"$image\" \"$target\"\n"
                + "        fi\n"
                + "      fi\n"
                + "    elif command -v docker >/dev/null 2>&1; then\n"
                + "      if [ -n \"$REGISTRY_USERNAME\" ] && [ -n \"$REGISTRY_PASSWORD\" ]; then echo \"$REGISTRY_PASSWORD\" | docker login \"$REGISTRY_SERVER\" -u \"$REGISTRY_USERNAME\" --password-stdin >> runtime-push.log 2>&1 || true; fi\n"
                + "      docker load -i \"images/$(echo \"$image\" | tr '/:@' '___').tar\" >> runtime-push.log 2>&1 || true\n"
                + "      if docker tag \"$image\" \"$target\" >> runtime-push.log 2>&1 && docker push \"$target\" >> runtime-push.log 2>&1; then\n"
                + "        pushed_count=$((pushed_count + 1))\n"
                + "        echo \"$target\" >> images/registry-image-list.txt\n"
                + "        echo \"__RK_REGISTRY_PUSH__ DONE $current $total $image $target\" | tee -a runtime-push.log\n"
                + "        docker rmi \"$target\" >> runtime-push.log 2>&1 || true\n"
                + "      else\n"
                + "        failed_count=$((failed_count + 1))\n"
                + "        echo \"__RK_REGISTRY_PUSH__ FAIL $current $total $image $target\" | tee -a runtime-push.log\n"
                + "      fi\n"
                + "    else\n"
                + "      failed_count=$((failed_count + 1))\n"
                + "      echo \"__RK_REGISTRY_PUSH__ FAIL $current $total $image $target\" | tee -a runtime-push.log\n"
                + "    fi\n"
                + "  done < \"$push_list\"\n"
                + "  if [ \"$failed_count\" -gt 0 ]; then\n"
                + "    user_state=missing\n"
                + "    password_state=missing\n"
                + "    [ -n \"$REGISTRY_USERNAME\" ] && user_state=set\n"
                + "    [ -n \"$REGISTRY_PASSWORD\" ] && password_state=set\n"
                + "    echo \"registry credentials configured: username=$user_state password=$password_state\" | tee -a runtime-push.log\n"
                + "    echo \"__RK_REGISTRY_PUSH__ ERROR failed_count=$failed_count pushed_count=$pushed_count total=$total\" | tee -a runtime-push.log\n"
                + "    echo \"__RK_REGISTRY_PUSH__ DIAGNOSTICS_BEGIN__\"\n"
                + "    echo \"__RK_REGISTRY_PUSH__ DIAGNOSTIC registry push tail follows\" | tee -a runtime-push.log\n"
                + "    tail -n 80 runtime-push.log || true\n"
                + "    echo \"__RK_REGISTRY_PUSH__ DIAGNOSTICS_END__\"\n"
                + "    echo \"registry image upload failed on source node\" | tee -a runtime-push.log\n"
                + "    exit 2\n"
                + "  fi\n"
                + "  if [ \"$REGISTRY_ONLY\" = \"true\" ] && [ \"$pushed_count\" -gt 0 ]; then\n"
                + "    find images -type f -name '*.tar' -delete\n"
                + "    echo 'registry image tar files are not kept in registry-only mode' >> runtime-summary/status.txt\n"
                + "    echo \"__RK_REGISTRY_PUSH__ CLEANED $pushed_count $total\" | tee -a runtime-push.log\n"
                + "    echo 'local image tar files cleaned after registry push' | tee -a runtime-push.log\n"
                + "  fi\n"
                + "  echo \"__RK_REGISTRY_PUSH__ COMPLETE $pushed_count $total\" | tee -a runtime-push.log\n"
                + "  fi\n"
                + "fi\n"
                + "tar -czf \"$ARCHIVE\" images runtime-summary runtime-export.log runtime-push.log\n"
                + "ARCHIVE_SIZE=$(wc -c < \"$ARCHIVE\" | tr -d ' ')\n"
                + "echo \"archive_bytes=$ARCHIVE_SIZE\" >> runtime-summary/status.txt\n"
                + "if [ \"$ARCHIVE_SIZE\" -le \"$MAX_RUNTIME_LOG_ARCHIVE_BYTES\" ]; then\n"
                + "  echo __RK_RUNTIME_ARCHIVE_BEGIN__\n"
                + "  base64 -w 0 /tmp/rk-runtime-images.tgz 2>/dev/null || base64 \"$ARCHIVE\" | tr -d '\\n'\n"
                + "  echo\n"
                + "  echo __RK_RUNTIME_ARCHIVE_END__\n"
                + "else\n"
                + "  echo \"runtime archive too large for pod-log transport: ${ARCHIVE_SIZE} bytes > ${MAX_RUNTIME_LOG_ARCHIVE_BYTES} bytes\" | tee -a runtime-export.log runtime-summary/status.txt\n"
                + "  echo 'runtime image archive skipped because it exceeds pod-log transport limit' | tee -a runtime-export.log runtime-summary/status.txt\n"
                + "  echo __RK_RUNTIME_ARCHIVE_SKIPPED__\n"
                + "fi\n"
                + "rm -rf \"$WORK\" \"$ARCHIVE\" \"$ARCHIVE.b64\"\n";
    }

    private boolean extractRuntimeImagesArchive(Path root, String logs, Long packageId, String nodeName) {
        String begin = "__RK_RUNTIME_ARCHIVE_BEGIN__";
        String end = "__RK_RUNTIME_ARCHIVE_END__";
        int beginIndex = logs == null ? -1 : logs.indexOf(begin);
        int endIndex = logs == null ? -1 : logs.indexOf(end);
        if (beginIndex < 0 || endIndex <= beginIndex) {
            return false;
        }
        String encoded = logs.substring(beginIndex + begin.length(), endIndex).replaceAll("\\s+", "");
        if (!StringUtils.hasText(encoded)) {
            return false;
        }
        try {
            Path archivePath = root.resolve("runtime-images-" + safeArtifactName(nodeName) + ".tgz");
            Files.write(archivePath, Base64.getMimeDecoder().decode(encoded));
            String previousRegistryImageList = readRuntimeArtifactText(root, "images/registry-image-list.txt");
            String previousExportedImageList = readRuntimeArtifactText(root, "images/exported-images.txt");
            String previousRuntimePushLog = readRuntimeArtifactText(root, "runtime-push.log");
            runLocalShellCommand("tar -xzf " + quoteForBash(archivePath.toString()) + " -C " + quoteForBash(root.toString()),
                    Math.max(60, deployPackageRuntimeExportTimeoutSeconds == null ? 1800 : deployPackageRuntimeExportTimeoutSeconds),
                    new IllegalStateException("extract runtime images archive"));
            mergeRuntimeArtifactTextFile(root, "images/registry-image-list.txt", previousRegistryImageList, true);
            mergeRuntimeArtifactTextFile(root, "images/exported-images.txt", previousExportedImageList, true);
            mergeRuntimeArtifactTextFile(root, "runtime-push.log", previousRuntimePushLog, false);
            Files.writeString(root.resolve("runtime-export.log"),
                    "runtime image export through privileged source-node pod succeeded on " + nodeName + "\n",
                    StandardCharsets.UTF_8);
            updateDeployPackageProgress(packageId, 57, "runtime image tar archive extracted from " + nodeName);
            return true;
        } catch (Exception e) {
            log.warn("extract runtime image archive failed, node={}", nodeName, e);
            return false;
        }
    }

    private void recordRuntimeArchiveSkipped(Path root, String logs, Long packageId, String nodeName) {
        if (logs == null || !logs.contains("__RK_RUNTIME_ARCHIVE_SKIPPED__")) {
            return;
        }
        try {
            Path summary = root.resolve("runtime-summary");
            Files.createDirectories(summary);
            Files.writeString(summary.resolve("status.txt"),
                    "runtime image archive skipped because it exceeds pod-log transport limit\n"
                            + "sourceNode=" + nodeName + "\n"
                            + "maxRuntimeLogArchiveBytes=" + MAX_RUNTIME_LOG_ARCHIVE_BYTES + "\n",
                    StandardCharsets.UTF_8);
            Files.writeString(root.resolve("runtime-export.log"),
                    extractRuntimeSkippedLines(logs, nodeName),
                    StandardCharsets.UTF_8);
            updateDeployPackageProgress(packageId, 57, "runtime image archive skipped on " + nodeName + " because it is too large for pod-log transport");
        } catch (Exception e) {
            log.warn("record runtime archive skipped status failed, node={}", nodeName, e);
        }
    }

    private String extractRuntimeSkippedLines(String logs, String nodeName) {
        StringBuilder builder = new StringBuilder();
        builder.append("runtime image export through privileged source-node pod skipped archive transport on ")
                .append(nodeName)
                .append('\n');
        for (String line : logs.split("\\R")) {
            if (line.contains("runtime archive too large for pod-log transport")
                    || line.contains("runtime image archive skipped because it exceeds pod-log transport limit")) {
                builder.append(line).append('\n');
            }
        }
        return builder.toString();
    }

    private String waitRuntimeImageExportPodPhase(HttpClient client,
                                                 String apiUrl,
                                                 String token,
                                                 String namespace,
                                                 String podName,
                                                 Long packageId,
                                                 int fallbackTotal) throws Exception {
        String path = "/api/v1/namespaces/" + encodePathSegment(namespace) + "/pods/" + encodePathSegment(podName);
        int timeoutSeconds = Math.max(60, deployPackageRuntimeExportTimeoutSeconds == null ? 1800 : deployPackageRuntimeExportTimeoutSeconds);
        int attempts = Math.max(1, timeoutSeconds / 5);
        int visibleLogLines = 0;
        for (int i = 0; i < attempts; i++) {
            JsonNode pod = getK8sJson(client, apiUrl, token, path);
            try {
                String logs = readK8sPodLogs(client, apiUrl, token, namespace, podName, "runtime-export", 1000);
                visibleLogLines = appendRuntimeExportProgressLines(packageId, logs, visibleLogLines, fallbackTotal);
            } catch (Exception e) {
                log.debug("runtime image export pod logs are not ready yet, pod={}", podName);
            }
            String phase = pod.path("status").path("phase").asText("Unknown");
            if (Objects.equals("Succeeded", phase) || Objects.equals("Failed", phase)) {
                try {
                    String logs = readK8sPodLogs(client, apiUrl, token, namespace, podName, "runtime-export", 1000);
                    appendRuntimeExportProgressLines(packageId, logs, visibleLogLines, fallbackTotal);
                } catch (Exception e) {
                    log.debug("read final runtime image export pod logs failed, pod={}", podName);
                }
                return phase;
            }
            throwIfCleanupPodCannotStart(pod);
            Thread.sleep(5000L);
        }
        return "Timeout";
    }

    private int appendRuntimeExportProgressLines(Long packageId, String logs, int alreadyVisibleLines, int fallbackTotal) {
        if (packageId == null || !StringUtils.hasText(logs)) {
            return Math.max(0, alreadyVisibleLines);
        }
        String[] lines = logs.split("\\R");
        int start = Math.max(0, Math.min(alreadyVisibleLines, lines.length));
        for (int i = start; i < lines.length; i++) {
            String line = lines[i];
            if (line.contains("source-node registry push started")) {
                updateDeployPackageStep(packageId, 58, "source-node registry push started", null, line);
            }
            applyRegistryPushProgressLine(packageId, line, fallbackTotal);
        }
        return lines.length;
    }

    private List<AdminK8sNodeVO> resolveRuntimeExportNodes() {
        Map<String, AdminK8sNodeVO> nodesByName = new LinkedHashMap<>();
        getK8sOverview().getNodes().stream()
                .filter(item -> StringUtils.hasText(item.getName()))
                .filter(item -> !StringUtils.hasText(item.getStatus()) || item.getStatus().toLowerCase().contains("ready"))
                .forEach(item -> nodesByName.putIfAbsent(item.getName(), item));
        loadK8sPodItemsJson().stream()
                .filter(pod -> shouldIncludeDeployPackagePodImage(pod, false))
                .map(item -> item.path("spec").path("nodeName").asText(""))
                .filter(StringUtils::hasText)
                .distinct()
                .forEach(name -> nodesByName.computeIfAbsent(name, missingNode -> {
                    AdminK8sNodeVO node = new AdminK8sNodeVO();
                    node.setName(missingNode);
                    node.setStatus("Ready");
                    return node;
                }));
        return new ArrayList<>(nodesByName.values());
    }

    private List<String> loadCurrentWorkloadImagesForRuntimeExport() {
        return loadCurrentWorkloadImagesForRuntimeExport(new AdminDeployPackageCreateDTO());
    }

    private List<String> loadCurrentWorkloadImagesForRuntimeExport(AdminDeployPackageCreateDTO request) {
        boolean includePlatformImages = request != null && Boolean.TRUE.equals(request.getIncludePlatformImages());
        List<String> images = loadK8sPodItemsJson().stream()
                .filter(pod -> shouldIncludeDeployPackagePodImage(pod, includePlatformImages))
                .flatMap(pod -> {
                    List<String> result = new ArrayList<>();
                    JsonNode containers = pod.path("spec").path("containers");
                    if (containers.isArray()) {
                        for (JsonNode container : containers) {
                            String image = container.path("image").asText("");
                            if (StringUtils.hasText(image)
                                    && (includePlatformImages || (!isDeployPackagePlatformImage(image) && isDeployPackageBusinessRuntimeImage(image)))) {
                                result.add(image);
                            }
                        }
                    }
                    return result.stream();
                })
                .distinct()
                .collect(Collectors.toList());
        if (!images.isEmpty()) {
            return images;
        }
        return listServiceRegistry().stream()
                .map(AdminOpsServiceVO::getImageName)
                .filter(StringUtils::hasText)
                .filter(image -> includePlatformImages || (!isDeployPackagePlatformImage(image) && isDeployPackageBusinessRuntimeImage(image)))
                .distinct()
                .collect(Collectors.toList());
    }

    private boolean shouldIncludeDeployPackagePodImage(JsonNode pod, boolean includePlatformImages) {
        if (includePlatformImages) {
            return true;
        }
        String namespace = pod.path("metadata").path("namespace").asText("");
        if (isDeployPackagePlatformNamespace(namespace)) {
            return false;
        }
        if (!isDeployPackageBusinessNamespace(namespace)) {
            return false;
        }
        String podName = pod.path("metadata").path("name").asText("");
        if (StringUtils.hasText(podName) && podName.toLowerCase().contains("rainbond")) {
            return false;
        }
        return true;
    }

    private boolean isDeployPackageBusinessNamespace(String namespace) {
        if (!StringUtils.hasText(namespace)) {
            return false;
        }
        String expectedNamespace = trimToDefault(k8sCleanupNamespace, "shetuanguanlixitong");
        return namespace.trim().equals(expectedNamespace);
    }

    private boolean isDeployPackageBusinessRuntimeImage(String image) {
        String repository = extractImageRepository(image);
        if (!StringUtils.hasText(repository)) {
            return false;
        }
        repository = repository.trim().toLowerCase(Locale.ROOT);
        List<String> candidates = new ArrayList<>();
        candidates.addAll(SERVICE_IMAGE_MAP.values());
        candidates.addAll(K8S_PERSISTENT_MIDDLEWARE);
        candidates.addAll(List.of("seata", "xxl-job", "sentinel", "nacos", "nacos-server", "elasticsearch", "rabbitmq", "mysql", "redis", "minio"));
        for (String candidate : candidates) {
            if (!StringUtils.hasText(candidate)) {
                continue;
            }
            candidate = candidate.trim().toLowerCase(Locale.ROOT);
            if (repository.equals(candidate) || repository.endsWith("-" + candidate)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDeployPackagePlatformNamespace(String namespace) {
        if (!StringUtils.hasText(namespace)) {
            return false;
        }
        String value = namespace.trim().toLowerCase();
        return value.equals("kube-system")
                || value.equals("kube-public")
                || value.equals("kube-node-lease")
                || value.equals("monitoring")
                || value.equals("ingress-nginx")
                || value.equals("cert-manager")
                || value.equals("rainbond")
                || value.equals("rbd-system")
                || value.equals("rbd-gateway")
                || value.startsWith("rainbond-")
                || value.startsWith("rbd-");
    }

    private boolean isDeployPackagePlatformImage(String image) {
        if (!StringUtils.hasText(image)) {
            return false;
        }
        String value = image.toLowerCase();
        return value.contains("rainbond")
                || value.contains("goodrain/")
                || value.contains("/rbd-")
                || value.contains("rbd-gateway")
                || value.contains("rbd-api")
                || value.contains("rbd-chaos")
                || value.contains("rbd-worker")
                || value.contains("kube-apiserver")
                || value.contains("kube-controller-manager")
                || value.contains("kube-scheduler")
                || value.contains("kube-proxy")
                || value.contains("coredns")
                || value.contains("pause:")
                || value.contains("prometheus")
                || value.contains("grafana")
                || value.contains("fluent")
                || value.contains("metrics-server")
                || value.contains("ingress-nginx")
                || value.contains("cert-manager");
    }

    private String safeArtifactName(String value) {
        if (!StringUtils.hasText(value)) {
            return "unknown";
        }
        return value.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    private void cleanupRuntimeArtifactDirectory(Path runtimeArtifactDir, Long packageId) {
        if (runtimeArtifactDir == null || !Files.exists(runtimeArtifactDir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(runtimeArtifactDir)) {
            List<Path> all = paths.sorted(Comparator.reverseOrder()).collect(Collectors.toList());
            for (Path path : all) {
                Files.deleteIfExists(path);
            }
            if (packageId != null) {
                jdbcTemplate.update(
                        "UPDATE ops_deploy_package_record SET logs = CONCAT(COALESCE(logs,''), ?), update_time = NOW() WHERE id = ?",
                        "runtime image tar files cleaned from source node\n",
                        packageId
                );
            }
        } catch (Exception e) {
            log.warn("cleanup runtime artifact directory failed, path={}", runtimeArtifactDir, e);
        }
    }

    private String buildDeployInstallScript() {
        return "#!/usr/bin/env bash\n"
                + "set -euo pipefail\n"
                + "NAMESPACE=${NAMESPACE:-shetuanguanlixitong}\n"
                + "DOMAIN=${DOMAIN:-}\n"
                + "CLUSTER_TYPE=${CLUSTER_TYPE:-k3s}\n"
                + "echo \"[1/5] checking cluster\"\n"
                + "kubectl version --client=true >/dev/null\n"
                + "kubectl get ns \"$NAMESPACE\" >/dev/null 2>&1 || kubectl create ns \"$NAMESPACE\"\n"
                + "echo \"[2/5] applying storage, mysql, redis, nacos, minio manifests if present\"\n"
                + "kubectl -n \"$NAMESPACE\" delete deploy mysql redis rabbitmq minio nacos elasticsearch --ignore-not-found=true || true\n"
                + "if [ -f k8s/rk-image-pull-secret.yaml ]; then kubectl apply -n \"$NAMESPACE\" -f k8s/rk-image-pull-secret.yaml; echo \"image pull secret applied from package\"; fi\n"
                + "find k8s -type f -name '*.yaml' -print0 2>/dev/null | xargs -0 -r kubectl apply -n \"$NAMESPACE\" -f\n"
                + "echo \"[3/5] loading images if image tar files are present\"\n"
                + "find images -type f -name '*.tar' -print0 2>/dev/null | xargs -0 -r -I{} sh -c 'ctr -n k8s.io images import \"$1\" || docker load -i \"$1\"' _ {}\n"
                + "echo \"[3.5/5] importing database dumps if present\"\n"
                + "bash scripts/import-databases.sh\n"
                + "echo \"[4/5] waiting workloads\"\n"
                + "kubectl -n \"$NAMESPACE\" get deploy,sts,svc\n"
                + "kubectl -n \"$NAMESPACE\" get statefulset -o name | while read resource; do kubectl -n \"$NAMESPACE\" rollout status \"$resource\" --timeout=10m; done\n"
                + "kubectl -n \"$NAMESPACE\" get deploy -o name | while read resource; do kubectl -n \"$NAMESPACE\" rollout status \"$resource\" --timeout=10m; done\n"
                + "echo \"[5/5] ingress/domain\"\n"
                + "echo \"cluster type: $CLUSTER_TYPE\"\n"
                + "echo \"point $DOMAIN to the cluster ingress/load-balancer IP shown below\"\n"
                + "kubectl -n \"$NAMESPACE\" get svc -o wide\n"
                + "kubectl -n \"$NAMESPACE\" get ingress -o wide\n"
                + "kubectl -n \"$NAMESPACE\" get ingress,svc -o wide\n";
    }

    private String buildComposeInstallScript() {
        return "#!/usr/bin/env bash\n"
                + "set -euo pipefail\n"
                + "ROOT_DIR=$(cd \"$(dirname \"$0\")/..\" && pwd)\n"
                + "cd \"$ROOT_DIR\"\n"
                + "if ! command -v docker >/dev/null 2>&1; then echo \"docker is required for compose install\" >&2; exit 1; fi\n"
                + "if docker compose version >/dev/null 2>&1; then COMPOSE='docker compose'; elif command -v docker-compose >/dev/null 2>&1; then COMPOSE='docker-compose'; else echo \"docker compose is required\" >&2; exit 1; fi\n"
                + "echo \"[1/4] loading local image tar files when present\"\n"
                + "find images -type f -name '*.tar' -print0 2>/dev/null | xargs -0 -r -I{} docker load -i {}\n"
                + "echo \"[2/4] starting RK-Web stack through docker compose\"\n"
                + "$COMPOSE --env-file compose/.env.example -f docker-compose.yml up -d\n"
                + "echo \"[3/4] importing database and MinIO data when present\"\n"
                + "bash scripts/import-databases.sh\n"
                + "bash scripts/import-minio.sh\n"
                + "echo \"[4/4] compose services\"\n"
                + "$COMPOSE -f docker-compose.yml ps\n";
    }

    private String buildK8sInstallScript() {
        return "#!/usr/bin/env bash\n"
                + "set -euo pipefail\n"
                + "ROOT_DIR=$(cd \"$(dirname \"$0\")/..\" && pwd)\n"
                + "cd \"$ROOT_DIR\"\n"
                + "NAMESPACE=${NAMESPACE:-shetuanguanlixitong}\n"
                + "RK_BUSYBOX_IMAGE=${RK_BUSYBOX_IMAGE:-" + REMOTE_BUSYBOX_IMAGE + "}\n"
                + "APPLICATION_DEPLOYS=\"rk-gateway rk-auth rk-user rk-search rk-file rk-message rk-content rk-pay rk-trade rk-exam rk-activity rk-data\"\n"
                + "APPLICATION_STARTUP_DEPLOYS=\"seata xxl-job sentinel $APPLICATION_DEPLOYS rk-web-frontend\"\n"
                + "APPLICATION_STARTUP_STATEFULSETS=\"rk-gogs rk-jenkins\"\n"
                + "kubectl get ns \"$NAMESPACE\" >/dev/null 2>&1 || kubectl create ns \"$NAMESPACE\"\n"
                + "pre_pull_package_images() {\n"
                + "  local image_list=\"${1:-}\"\n"
                + "  if [ -z \"$image_list\" ]; then if [ -s images/registry-image-list.txt ]; then image_list=images/registry-image-list.txt; elif [ -s images/image-list.txt ]; then image_list=images/image-list.txt; else echo \"no package image list found; skipping pre-pull\"; return 0; fi; fi\n"
                + "  if [ ! -s \"$image_list\" ]; then echo \"package image list not found or empty: $image_list; skipping pre-pull\"; return 0; fi\n"
                + "  echo \"pre-pulling package images from $image_list\"\n"
                + "  while IFS= read -r image || [ -n \"$image\" ]; do\n"
                + "    image=$(printf '%s' \"$image\" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')\n"
                + "    [ -z \"$image\" ] && continue\n"
                + "    case \"$image\" in '#'*) continue ;; esac\n"
                + "    case \"$image\" in *[!A-Za-z0-9._/:@-]* ) echo \"invalid package image reference: $image\" >&2; exit 1 ;; esac\n"
                + "    local max_attempts=5\n"
                + "    local attempt=1\n"
                + "    while [ \"$attempt\" -le \"$max_attempts\" ]; do\n"
                + "      echo \"pre-pulling package image: $image (attempt $attempt/$max_attempts)\"\n"
                + "      if command -v crictl >/dev/null 2>&1; then\n"
                + "        if crictl pull \"$image\"; then echo \"pre-pulled package image: $image\"; break; fi\n"
                + "        echo \"crictl image pre-pull failed, falling back to kubelet pod pull: $image\"\n"
                + "      fi\n"
                + "      kubectl -n \"$NAMESPACE\" delete pod rk-image-prepull --ignore-not-found=true >/dev/null 2>&1 || true\n"
                + "      {\n"
                + "        cat <<EOF\n"
                + "apiVersion: v1\n"
                + "kind: Pod\n"
                + "metadata:\n"
                + "  name: rk-image-prepull\n"
                + "spec:\n"
                + "  restartPolicy: Never\n"
                + "EOF\n"
                + "        if kubectl -n \"$NAMESPACE\" get secret rk-aliyun-regcred >/dev/null 2>&1; then\n"
                + "          cat <<EOF\n"
                + "  imagePullSecrets:\n"
                + "  - name: rk-aliyun-regcred\n"
                + "EOF\n"
                + "        fi\n"
                + "        cat <<EOF\n"
                + "  containers:\n"
                + "  - name: image\n"
                + "    image: \"$image\"\n"
                + "    imagePullPolicy: IfNotPresent\n"
                + "    command: ['/bin/sh', '-c', 'sleep 1']\n"
                + "EOF\n"
                + "      } | kubectl -n \"$NAMESPACE\" apply -f -\n"
                + "      local deadline=$((SECONDS + 900))\n"
                + "      local retry_pull=0\n"
                + "      while true; do\n"
                + "        image_id=$(kubectl -n \"$NAMESPACE\" get pod rk-image-prepull -o jsonpath='{.status.containerStatuses[0].imageID}' 2>/dev/null || true)\n"
                + "        if [ -n \"$image_id\" ]; then echo \"pre-pulled package image: $image\"; break; fi\n"
                + "        status_line=$(kubectl -n \"$NAMESPACE\" get pod rk-image-prepull --no-headers 2>/dev/null || true)\n"
                + "        if printf '%s\\n' \"$status_line\" | grep -Eiq 'InvalidImageName|CreateContainerConfigError'; then kubectl -n \"$NAMESPACE\" describe pod rk-image-prepull || true; exit 1; fi\n"
                + "        if printf '%s\\n' \"$status_line\" | grep -Eiq 'ErrImagePull|ImagePullBackOff|RegistryUnavailable|TLSHandshakeTimeout'; then retry_pull=1; break; fi\n"
                + "        if [ \"$SECONDS\" -ge \"$deadline\" ]; then retry_pull=1; break; fi\n"
                + "        sleep 5\n"
                + "      done\n"
                + "      if [ -n \"${image_id:-}\" ]; then break; fi\n"
                + "      kubectl -n \"$NAMESPACE\" describe pod rk-image-prepull || true\n"
                + "      if [ \"$attempt\" -ge \"$max_attempts\" ]; then echo \"failed pre-pulling package image after $max_attempts attempts: $image\" >&2; exit 1; fi\n"
                + "      echo \"retrying package image pre-pull after transient registry pull failure: $image\"\n"
                + "      kubectl -n \"$NAMESPACE\" delete pod rk-image-prepull --ignore-not-found=true --wait=false >/dev/null 2>&1 || true\n"
                + "      sleep $((attempt * 10))\n"
                + "      attempt=$((attempt + 1))\n"
                + "    done\n"
                + "    kubectl -n \"$NAMESPACE\" delete pod rk-image-prepull --ignore-not-found=true >/dev/null 2>&1 || true\n"
                + "  done < \"$image_list\"\n"
                + "}\n"
                + "pre_pull_infra_images() { pre_pull_package_images images/infra-images.txt; }\n"
                + "pre_pull_application_images() { if [ -s images/application-images.txt ]; then pre_pull_package_images images/application-images.txt; else pre_pull_package_images; fi; }\n"
                + "apply_manifest_file() { file=\"$1\"; if [ ! -s \"$file\" ]; then echo \"manifest file missing or empty: $file\" >&2; exit 1; fi; kubectl apply -n \"$NAMESPACE\" -f \"$file\"; }\n"
                + "apply_infra_manifests() { echo \"applying infrastructure manifests\"; apply_manifest_file k8s/rk-web-infra.yaml; }\n"
                + "apply_nacos_manifest() { echo \"applying nacos manifest\"; apply_manifest_file k8s/rk-web-nacos.yaml; }\n"
                + "apply_application_manifests() {\n"
                + "  local existing_deploys=\"${1:-}\"\n"
                + "  if [ -z \"$existing_deploys\" ]; then\n"
                + "    echo \"applying application manifests at zero replicas for staged startup\"\n"
                + "    sed 's/^  replicas: 1$/  replicas: 0/' k8s/rk-web-apps.yaml | kubectl apply -n \"$NAMESPACE\" -f -\n"
                + "  else\n"
                + "    echo \"applying application manifests\"\n"
                + "    apply_manifest_file k8s/rk-web-apps.yaml\n"
                + "  fi\n"
                + "}\n"
                + "capture_existing_application_deployments() { for deploy in $APPLICATION_STARTUP_DEPLOYS; do if kubectl -n \"$NAMESPACE\" get deploy \"$deploy\" >/dev/null 2>&1; then printf '%s\\n' \"$deploy\"; fi; done; }\n"
                + "restart_existing_application_deployments() {\n"
                + "  local deploys=\"${1:-}\"\n"
                + "  if [ -z \"$deploys\" ]; then echo \"no existing application deployments to restart after data import\"; return 0; fi\n"
                + "  echo \"restarting existing application deployments sequentially after data import: $(printf '%s' \"$deploys\" | tr '\\n' ' ')\"\n"
                + "  printf '%s\\n' \"$deploys\" | while IFS= read -r deploy; do\n"
                + "    [ -z \"$deploy\" ] && continue\n"
                + "    echo \"restarting existing application deployment: $deploy\"\n"
                + "    kubectl -n \"$NAMESPACE\" rollout restart \"deploy/$deploy\"\n"
                + "    wait_for_k8s_resource \"deployment.apps/$deploy\" 15m\n"
                + "  done\n"
                + "}\n"
                + "print_resource_diagnostics() {\n"
                + "  local resource=\"$1\"\n"
                + "  echo \"diagnostics for $resource\" >&2\n"
                + "  kubectl -n \"$NAMESPACE\" describe \"$resource\" || true\n"
                + "  kubectl -n \"$NAMESPACE\" get pods -o wide || true\n"
                + "  kubectl -n \"$NAMESPACE\" get events --sort-by=.lastTimestamp | tail -120 || true\n"
                + "  kubectl -n \"$NAMESPACE\" get pod,sts,deploy,job -o wide || true\n"
                + "}\n"
                + "wait_for_k8s_resource() { resource=\"$1\"; timeout=\"${2:-10m}\"; echo \"waiting $resource\"; if ! kubectl -n \"$NAMESPACE\" rollout status \"$resource\" --timeout=\"$timeout\"; then echo \"resource not ready: $resource\" >&2; print_resource_diagnostics \"$resource\"; exit 1; fi; }\n"
                + "wait_for_mysql_tcp() {\n"
                + "  echo \"waiting mysql tcp readiness\"\n"
                + "  local deadline=$((SECONDS + 600))\n"
                + "  while true; do\n"
                + "    if kubectl -n \"$NAMESPACE\" exec statefulset/mysql -- sh -lc 'MYSQL_PWD=\"${MYSQL_ROOT_PASSWORD:-123456}\" mysqladmin ping -h127.0.0.1 -P3306 -uroot --silent' >/dev/null 2>&1; then echo \"mysql tcp ready\"; break; fi\n"
                + "    if [ \"$SECONDS\" -ge \"$deadline\" ]; then echo \"mysql tcp not ready\" >&2; kubectl -n \"$NAMESPACE\" logs statefulset/mysql --tail=160 || true; exit 1; fi\n"
                + "    sleep 5\n"
                + "  done\n"
                + "}\n"
                + "wait_for_nacos_config() {\n"
                + "  echo \"waiting nacos config rk-shared-mybatis.yaml\"\n"
                + "  local deadline=$((SECONDS + 900))\n"
                + "  while true; do\n"
                + "    if kubectl -n \"$NAMESPACE\" run rk-nacos-config-check --rm -i --restart=Never --image=\"$RK_BUSYBOX_IMAGE\" --command -- sh -lc 'wget -qO- \"http://nacos:8848/nacos/v1/cs/configs?dataId=rk-shared-mybatis.yaml&group=DEFAULT_GROUP\" | grep -q \"spring:\"' >/dev/null 2>&1; then echo \"nacos config rk-shared-mybatis.yaml ready\"; break; fi\n"
                + "    kubectl -n \"$NAMESPACE\" delete pod/rk-nacos-config-check --ignore-not-found=true --wait=false >/dev/null 2>&1 || true\n"
                + "    if [ \"$SECONDS\" -ge \"$deadline\" ]; then echo \"nacos config rk-shared-mybatis.yaml not ready\" >&2; kubectl -n \"$NAMESPACE\" logs statefulset/nacos --tail=160 || true; exit 1; fi\n"
                + "    sleep 10\n"
                + "  done\n"
                + "}\n"
                + "start_application_deployments_sequentially() {\n"
                + "  echo \"starting application deployments sequentially to avoid single-node memory pressure\"\n"
                + "  for deploy in $APPLICATION_STARTUP_DEPLOYS; do\n"
                + "    if ! kubectl -n \"$NAMESPACE\" get deploy \"$deploy\" >/dev/null 2>&1; then echo \"application deployment not present, skip: $deploy\"; continue; fi\n"
                + "    echo \"starting application deployment: $deploy\"\n"
                + "    kubectl -n \"$NAMESPACE\" scale \"deploy/$deploy\" --replicas=1\n"
                + "    wait_for_k8s_resource \"deployment.apps/$deploy\" 15m\n"
                + "  done\n"
                + "  start_application_statefulsets_sequentially\n"
                + "}\n"
                + "start_application_statefulsets_sequentially() {\n"
                + "  echo \"starting application statefulsets sequentially\"\n"
                + "  for statefulset in $APPLICATION_STARTUP_STATEFULSETS; do\n"
                + "    if ! kubectl -n \"$NAMESPACE\" get statefulset \"$statefulset\" >/dev/null 2>&1; then echo \"application statefulset not present, skip: $statefulset\"; continue; fi\n"
                + "    echo \"starting application statefulset: $statefulset\"\n"
                + "    kubectl -n \"$NAMESPACE\" scale \"statefulset/$statefulset\" --replicas=1\n"
                + "    wait_for_k8s_resource \"statefulset.apps/$statefulset\" 15m\n"
                + "  done\n"
                + "}\n"
                + "restore_platform_stateful_data() {\n"
                + "  echo \"restoring platform stateful data when archives are present\"\n"
                + "  if [ -f platform/rk-gogs/data.tar.gz ]; then\n"
                + "    # kubectl -n \"$NAMESPACE\" exec \"rk-gogs-0\"\n"
                + "    echo \"restoring platform stateful data: platform/rk-gogs/data.tar.gz -> rk-gogs-0:/data\"\n"
                + "    kubectl -n \"$NAMESPACE\" scale \"statefulset/rk-gogs\" --replicas=1\n"
                + "    wait_for_k8s_resource \"statefulset.apps/rk-gogs\" 15m\n"
                + "    kubectl -n \"$NAMESPACE\" exec \"rk-gogs-0\" -- sh -lc \"mkdir -p /data && find /data -mindepth 1 -maxdepth 1 -exec rm -rf {} +\"\n"
                + "    gzip -dc platform/rk-gogs/data.tar.gz | kubectl -n \"$NAMESPACE\" exec -i \"rk-gogs-0\" -- sh -lc \"tar -xpf - -C /data\"\n"
                + "    kubectl -n \"$NAMESPACE\" exec \"rk-gogs-0\" -- sh -lc \"chown -R 1000:1000 /data || true; sync || true\"\n"
                + "    kubectl -n \"$NAMESPACE\" scale \"statefulset/rk-gogs\" --replicas=0\n"
                + "  fi\n"
                + "  if [ -f platform/rk-jenkins/jenkins-home.tar.gz ]; then\n"
                + "    # kubectl -n \"$NAMESPACE\" exec \"rk-jenkins-0\"\n"
                + "    echo \"restoring platform stateful data: platform/rk-jenkins/jenkins-home.tar.gz -> rk-jenkins-0:/var/jenkins_home\"\n"
                + "    kubectl -n \"$NAMESPACE\" scale \"statefulset/rk-jenkins\" --replicas=1\n"
                + "    wait_for_k8s_resource \"statefulset.apps/rk-jenkins\" 15m\n"
                + "    kubectl -n \"$NAMESPACE\" exec \"rk-jenkins-0\" -- sh -lc \"mkdir -p /var/jenkins_home && find /var/jenkins_home -mindepth 1 -maxdepth 1 -exec rm -rf {} +\"\n"
                + "    gzip -dc platform/rk-jenkins/jenkins-home.tar.gz | kubectl -n \"$NAMESPACE\" exec -i \"rk-jenkins-0\" -- sh -lc \"tar -xpf - -C /var/jenkins_home\"\n"
                + "    kubectl -n \"$NAMESPACE\" exec \"rk-jenkins-0\" -- sh -lc \"chown -R 1000:1000 /var/jenkins_home || true; sync || true\"\n"
                + "    kubectl -n \"$NAMESPACE\" scale \"statefulset/rk-jenkins\" --replicas=0\n"
                + "  fi\n"
                + "}\n"
                + "restart_after_data_import() {\n"
                + "  echo \"restarting nacos after data import\"\n"
                + "  apply_nacos_manifest\n"
                + "  kubectl -n \"$NAMESPACE\" rollout restart statefulset/nacos\n"
                + "  wait_for_k8s_resource statefulset/nacos 15m\n"
                + "  wait_for_nacos_config\n"
                + "  pre_pull_application_images\n"
                + "  echo \"starting application workloads after nacos config is ready\"\n"
                + "  existing_app_deployments=$(capture_existing_application_deployments)\n"
                + "  apply_application_manifests \"$existing_app_deployments\"\n"
                + "  restore_platform_stateful_data\n"
                + "  if [ -z \"$existing_app_deployments\" ]; then start_application_deployments_sequentially; else restart_existing_application_deployments \"$existing_app_deployments\"; start_application_statefulsets_sequentially; fi\n"
                + "}\n"
                + "echo \"[1/5] importing image tar files when present\"\n"
                + "find images -type f -name '*.tar' -print0 2>/dev/null | xargs -0 -r -I{} sh -c 'ctr -n k8s.io images import \"$1\" || docker load -i \"$1\"' _ {}\n"
                + "echo \"[2/5] applying infrastructure manifests\"\n"
                + "kubectl -n \"$NAMESPACE\" delete deploy mysql redis rabbitmq minio nacos elasticsearch --ignore-not-found=true || true\n"
                + "if [ -f k8s/rk-image-pull-secret.yaml ]; then kubectl apply -n \"$NAMESPACE\" -f k8s/rk-image-pull-secret.yaml; echo \"image pull secret applied from package\"; fi\n"
                + "pre_pull_infra_images\n"
                + "apply_infra_manifests\n"
                + "wait_for_k8s_resource statefulset/mysql 20m\n"
                + "wait_for_mysql_tcp\n"
                + "wait_for_k8s_resource statefulset/minio 10m\n"
                + "echo \"[3/5] importing database and MinIO data when present\"\n"
                + "bash scripts/import-databases.sh\n"
                + "bash scripts/import-minio.sh\n"
                + "echo \"[4/5] applying nacos and application manifests\"\n"
                + "restart_after_data_import\n"
                + "echo \"[5/5] workloads\"\n"
                + "kubectl -n \"$NAMESPACE\" get statefulset -o name | while read resource; do wait_for_k8s_resource \"$resource\" 15m; done\n"
                + "kubectl -n \"$NAMESPACE\" get deploy -o name | while read resource; do wait_for_k8s_resource \"$resource\" 15m; done\n"
                + "kubectl -n \"$NAMESPACE\" get deploy,sts,svc,ingress -o wide\n";
    }

    private String buildOnlinePackageScript() {
        return "#!/usr/bin/env bash\n"
                + "set -euo pipefail\n"
                + "ROOT_DIR=$(cd \"$(dirname \"$0\")/..\" && pwd)\n"
                + "cd \"$ROOT_DIR\"\n"
                + "bash scripts/export-all-current-data.sh\n";
    }

    private String buildExportAllCurrentDataScript() {
        return "#!/usr/bin/env bash\n"
                + "set -euo pipefail\n"
                + "ROOT_DIR=$(cd \"$(dirname \"$0\")/..\" && pwd)\n"
                + "cd \"$ROOT_DIR\"\n"
                + "echo \"[1/5] exporting container images to ./images\"\n"
                + "bash scripts/export-images.sh\n"
                + "echo \"[2/6] exporting databases to ./databases\"\n"
                + "bash scripts/export-databases.sh\n"
                + "echo \"[3/6] exporting MinIO objects when mc is available\"\n"
                + "bash scripts/export-minio.sh || true\n"
                + "echo \"[4/6] collecting Kubernetes manifests\"\n"
                + "mkdir -p k8s\n"
                + "kubectl get deploy,sts,svc,ingress,configmap,secret -A -o yaml > k8s/source-cluster-snapshot.yaml 2>/dev/null || true\n"
                + "kubectl get nodes -o wide > k8s/source-nodes.txt 2>/dev/null || true\n"
                + "kubectl get pods -A -o wide > k8s/source-pods.txt 2>/dev/null || true\n"
                + "echo \"[5/6] optionally push images to registry\"\n"
                + "bash scripts/push-images-to-registry.sh || true\n"
                + "echo \"[6/6] writing offline manifest summary\"\n"
                + "find images databases minio k8s -maxdepth 2 -type f | sort > offline-artifacts.txt\n"
                + "echo \"offline bundle folders now contain images/, databases/, minio/, and k8s/ artifacts\"\n";
    }

    private String buildImageExportScript() {
        return "#!/usr/bin/env bash\n"
                + "set -euo pipefail\n"
                + "mkdir -p images\n"
                + "echo \"exporting current RK-Web workload images into ./images when a container runtime is available\"\n"
                + "if command -v kubectl >/dev/null 2>&1; then\n"
                + "  kubectl get pods -A -o jsonpath='{range .items[*].spec.containers[*]}{.image}{\"\\n\"}{end}' 2>/dev/null | sort -u > images/workload-images.txt || true\n"
                + "fi\n"
                + "if [ ! -s images/workload-images.txt ] && [ -f images/image-list.txt ]; then cp images/image-list.txt images/workload-images.txt; fi\n"
                + "if [ ! -s images/workload-images.txt ]; then echo \"no workload image list found\" > images/workload-images.txt; fi\n"
                + "if command -v crictl >/dev/null 2>&1; then\n"
                + "  cat images/workload-images.txt | while read -r image; do\n"
                + "    [ -z \"$image\" ] && continue\n"
                + "    safe=$(echo \"$image\" | tr '/:@' '___')\n"
                + "    ctr -n k8s.io images export \"images/${safe}.tar\" \"$image\" || true\n"
                + "  done\n"
                + "elif command -v docker >/dev/null 2>&1; then\n"
                + "  cat images/workload-images.txt | while read -r image; do\n"
                + "    [ -z \"$image\" ] && continue\n"
                + "    safe=$(echo \"$image\" | tr '/:@' '___')\n"
                + "    docker save \"$image\" -o \"images/${safe}.tar\" || true\n"
                + "  done\n"
                + "else\n"
                + "  echo \"no crictl/docker runtime found, skip image export\"\n"
                + "fi\n";
    }

    private String buildPushImagesToRegistryScript(AdminDeployPackageCreateDTO request) {
        String prefix = trimToDefault(request.getRegistryPrefix(), DEFAULT_REGISTRY_PREFIX);
        String server = trimToDefault(request.getRegistryServer(), DEFAULT_REGISTRY_SERVER);
        return String.join("\n",
                "#!/usr/bin/env bash",
                "set -euo pipefail",
                "REGISTRY_PREFIX=${REGISTRY_PREFIX:-" + prefix + "}",
                "REGISTRY_SERVER=${REGISTRY_SERVER:-" + server + "}",
                "CLEAN_AFTER_PUSH=${CLEAN_AFTER_PUSH:-true}",
                "UPLOAD_LOG=${UPLOAD_LOG:-aliyun-registry-upload.log}",
                "exec > >(tee -a \"$UPLOAD_LOG\" upload-log.txt) 2>&1",
                "echo \"target registry: $REGISTRY_PREFIX\"",
                "echo \"aliyun registry upload started at $(date -Is)\"",
                "if [ ! -d images ]; then echo \"images directory not found, skip registry push\"; exit 0; fi",
                "IMAGE_LIST=images/workload-images.txt",
                "[ -f \"$IMAGE_LIST\" ] || IMAGE_LIST=images/image-list.txt",
                "if [ ! -s \"$IMAGE_LIST\" ]; then echo \"no image list found, skip registry push\"; exit 0; fi",
                "CTR=$(command -v ctr 2>/dev/null || true)",
                "[ -n \"$CTR\" ] || [ ! -x /var/lib/rancher/rke2/bin/ctr ] || CTR=/var/lib/rancher/rke2/bin/ctr",
                "[ -n \"$CTR\" ] || [ ! -x /var/lib/rancher/k3s/data/current/bin/ctr ] || CTR=/var/lib/rancher/k3s/data/current/bin/ctr",
                "CONTAINERD_ADDRESS=",
                "for sock in /run/k3s/containerd/containerd.sock /run/rke2/containerd/containerd.sock /run/containerd/containerd.sock; do if [ -S \"$sock\" ]; then CONTAINERD_ADDRESS=\"$sock\"; break; fi; done",
                "ctr_cmd() { if [ -n \"$CONTAINERD_ADDRESS\" ]; then \"$CTR\" --address \"$CONTAINERD_ADDRESS\" \"$@\"; else \"$CTR\" \"$@\"; fi; }",
                "RUNTIME_ARCH=$(uname -m | sed 's/x86_64/amd64/;s/aarch64/arm64/;s/armv7l/arm\\/v7/;s/armv6l/arm\\/v6/')",
                "RUNTIME_PLATFORM=${RUNTIME_PLATFORM:-linux/$RUNTIME_ARCH}",
                "if command -v docker >/dev/null 2>&1; then",
                "  if [ -n \"${REGISTRY_USERNAME:-}\" ] && [ -n \"${REGISTRY_PASSWORD:-}\" ]; then echo \"$REGISTRY_PASSWORD\" | docker login \"$REGISTRY_SERVER\" -u \"$REGISTRY_USERNAME\" --password-stdin; fi",
                "  find images -type f -name '*.tar' -print0 2>/dev/null | xargs -0 -r -I{} docker load -i {}",
                "elif [ -n \"$CTR\" ]; then",
                "  while IFS= read -r tar; do ctr_cmd -n k8s.io images import \"$tar\"; done < <(find images -type f -name '*.tar' -print 2>/dev/null)",
                "else",
                "  echo \"docker/ctr runtime not found, cannot push registry images\" >&2",
                "  exit 1",
                "fi",
                "total=$(awk 'NF && $0!=\"no workload image list found\"{count++} END{print count+0}' \"$IMAGE_LIST\")",
                "echo \"__RK_REGISTRY_PUSH__ START total=$total\"",
                ": > images/registry-image-list.txt",
                "pushed_count=0",
                "failed_count=0",
                "current=0",
                "emit_ctr_push_failure() {",
                "  failed_image=\"$1\"",
                "  failed_target=\"$2\"",
                "  echo \"__RK_REGISTRY_PUSH__ DIAGNOSTIC ctr push failed for $failed_image -> $failed_target\"",
                "  echo \"__RK_REGISTRY_PUSH__ DIAGNOSTIC ctr push failed tail follows\"",
                "  tail -n 16 \"$UPLOAD_LOG\" 2>/dev/null | sed 's/^/__RK_REGISTRY_PUSH__ DIAGNOSTIC /' || true",
                "}",
                "while IFS= read -r image; do",
                "  [ -z \"$image\" ] && continue",
                "  [ \"$image\" = \"no workload image list found\" ] && continue",
                "  current=$((current + 1))",
                "  repo=$(echo \"$image\" | awk -F/ '{print $NF}' | awk -F: '{print $1}')",
                "  tag=$(echo \"$image\" | awk -F: '{print $NF}')",
                "  [ \"$tag\" = \"$image\" ] && tag=latest",
                "  target=\"$REGISTRY_PREFIX/$repo:$tag\"",
                "  echo \"__RK_REGISTRY_PUSH__ IMAGE $current $total $image $target\"",
                "  if command -v docker >/dev/null 2>&1; then",
                "    if docker tag \"$image\" \"$target\" && docker push \"$target\"; then",
                "      pushed_count=$((pushed_count + 1))",
                "      echo \"$target\" >> images/registry-image-list.txt",
                "      echo \"__RK_REGISTRY_PUSH__ DONE $current $total $image $target\"",
                "      docker rmi \"$target\" || true",
                "    else",
                "      failed_count=$((failed_count + 1))",
                "      echo \"__RK_REGISTRY_PUSH__ FAIL $current $total $image $target\"",
                "      echo \"failed to push image: $image -> $target\" >&2",
                "    fi",
                "  elif [ -n \"$CTR\" ]; then",
                "    if [ -n \"${REGISTRY_USERNAME:-}\" ] && [ -n \"${REGISTRY_PASSWORD:-}\" ]; then",
                "      if ctr_cmd -n k8s.io images tag --force \"$image\" \"$target\" && ctr_cmd -n k8s.io images push --platform \"$RUNTIME_PLATFORM\" --user \"$REGISTRY_USERNAME:$REGISTRY_PASSWORD\" \"$target\"; then",
                "        pushed_count=$((pushed_count + 1))",
                "        echo \"$target\" >> images/registry-image-list.txt",
                "        echo \"__RK_REGISTRY_PUSH__ DONE $current $total $image $target\"",
                "      else",
                "        failed_count=$((failed_count + 1))",
                "        echo \"__RK_REGISTRY_PUSH__ FAIL $current $total $image $target\"",
                "        emit_ctr_push_failure \"$image\" \"$target\"",
                "        echo \"failed to push image through ctr -n k8s.io images push: $image -> $target\" >&2",
                "      fi",
                "    else",
                "      if ctr_cmd -n k8s.io images tag --force \"$image\" \"$target\" && ctr_cmd -n k8s.io images push --platform \"$RUNTIME_PLATFORM\" \"$target\"; then",
                "        pushed_count=$((pushed_count + 1))",
                "        echo \"$target\" >> images/registry-image-list.txt",
                "        echo \"__RK_REGISTRY_PUSH__ DONE $current $total $image $target\"",
                "      else",
                "        failed_count=$((failed_count + 1))",
                "        echo \"__RK_REGISTRY_PUSH__ FAIL $current $total $image $target\"",
                "        emit_ctr_push_failure \"$image\" \"$target\"",
                "        echo \"failed to push image through ctr -n k8s.io images push: $image -> $target\" >&2",
                "      fi",
                "    fi",
                "  fi",
                "done < \"$IMAGE_LIST\"",
                "if [ \"$failed_count\" -gt 0 ]; then",
                "  echo \"push failed, keep local image tar files for retry\" >&2",
                "  exit 1",
                "fi",
                "if [ \"$pushed_count\" -gt 0 ] && [ \"$CLEAN_AFTER_PUSH\" = \"true\" ]; then",
                "  find images -type f -name '*.tar' -delete",
                "  echo \"__RK_REGISTRY_PUSH__ CLEANED $pushed_count $total\"",
                "  echo \"local image tar files cleaned after registry push\"",
                "else",
                "  echo \"local image tar files retained (CLEAN_AFTER_PUSH=$CLEAN_AFTER_PUSH, pushed_count=$pushed_count)\"",
                "fi",
                "echo \"__RK_REGISTRY_PUSH__ COMPLETE $pushed_count $total\"",
                "");
    }

    private String buildPullImagesScript(AdminDeployPackageCreateDTO request) {
        String prefix = trimToDefault(request.getRegistryPrefix(), DEFAULT_REGISTRY_PREFIX);
        String server = trimToDefault(request.getRegistryServer(), DEFAULT_REGISTRY_SERVER);
        return "#!/usr/bin/env bash\n"
                + "set -euo pipefail\n"
                + "REGISTRY_PREFIX=${REGISTRY_PREFIX:-" + prefix + "}\n"
                + "REGISTRY_SERVER=${REGISTRY_SERVER:-" + server + "}\n"
                + "if [ -z \"${IMAGE_LIST:-}\" ]; then\n"
                + "  if [ -s images/registry-image-list.txt ]; then IMAGE_LIST=images/registry-image-list.txt; else IMAGE_LIST=images/image-list.txt; fi\n"
                + "fi\n"
                + "echo \"registry pull package: $REGISTRY_PREFIX\"\n"
                + "if [ -n \"${REGISTRY_USERNAME:-}\" ] && [ -n \"${REGISTRY_PASSWORD:-}\" ]; then echo \"$REGISTRY_PASSWORD\" | docker login \"$REGISTRY_SERVER\" -u \"$REGISTRY_USERNAME\" --password-stdin; fi\n"
                + "if [ ! -s \"$IMAGE_LIST\" ]; then echo \"image list not found: $IMAGE_LIST\" >&2; exit 1; fi\n"
                + "while IFS= read -r image; do\n"
                + "  [ -n \"$image\" ] || continue\n"
                + "  case \"$image\" in\n"
                + "    */*) target=\"$image\" ;;\n"
                + "    *) target=\"$REGISTRY_PREFIX/$image:${RK_WEB_TAG:-latest}\" ;;\n"
                + "  esac\n"
                + "  echo \"pull $target\"\n"
                + "  docker pull \"$target\"\n"
                + "done < \"$IMAGE_LIST\"\n"
                + "echo \"registry pull package images pulled\"\n";
    }

    private String buildImageListHint() {
        return resolveDeployPackageImageList(new AdminDeployPackageCreateDTO()).stream()
                .collect(Collectors.joining("\n", "", "\n"));
    }

    private String buildDatabaseExportScript(Map<String, Object> manifest) {
        Object databases = manifest == null ? List.of() : manifest.get("databases");
        return "#!/usr/bin/env bash\n"
                + "set -euo pipefail\n"
                + "MYSQL_HOST=${MYSQL_HOST:-127.0.0.1}\n"
                + "MYSQL_PORT=${MYSQL_PORT:-3306}\n"
                + "MYSQL_USER=${MYSQL_USER:-root}\n"
                + "MYSQL_PASSWORD=${MYSQL_PASSWORD:-}\n"
                + "mkdir -p databases\n"
                + "DATABASES='" + String.valueOf(databases).replace("'", "'\"'\"'") + "'\n"
                + "echo \"database list from manifest: $DATABASES\"\n"
                + "for db in $(echo \"$DATABASES\" | tr -d '[],' ); do\n"
                + "  db=$(echo \"$db\" | tr -d '\"')\n"
                + "  [ -z \"$db\" ] && continue\n"
                + "  mysqldump -h\"$MYSQL_HOST\" -P\"$MYSQL_PORT\" -u\"$MYSQL_USER\" -p\"$MYSQL_PASSWORD\" --single-transaction --routines --triggers \"$db\" | gzip > \"databases/${db}.sql.gz\"\n"
                + "done\n";
    }

    private String buildDatabaseImportScript() {
        return "#!/usr/bin/env bash\n"
                + "set -euo pipefail\n"
                + "ROOT_DIR=$(cd \"$(dirname \"$0\")/..\" && pwd)\n"
                + "cd \"$ROOT_DIR\"\n"
                + "NAMESPACE=${NAMESPACE:-shetuanguanlixitong}\n"
                + "MYSQL_HOST=${MYSQL_HOST:-127.0.0.1}\n"
                + "MYSQL_PORT=${MYSQL_PORT:-3306}\n"
                + "MYSQL_USER=${MYSQL_USER:-root}\n"
                + "MYSQL_PASSWORD=${MYSQL_PASSWORD:-}\n"
                + "MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD:-123456}\n"
                + "REMOTE_MYSQL_IMAGE=${REMOTE_MYSQL_IMAGE:-m.daocloud.io/docker.io/library/mysql:8.0}\n"
                + "MYSQL_IMPORT_JOB=${MYSQL_IMPORT_JOB:-rk-linux-ssh-mysql-import}\n"
                + "if [ ! -d databases ]; then\n"
                + "  echo \"no databases directory, skip database import\"\n"
                + "  exit 0\n"
                + "fi\n"
                + "use_k8s_mysql=false\n"
                + "if command -v kubectl >/dev/null 2>&1 && kubectl -n \"$NAMESPACE\" get statefulset mysql >/dev/null 2>&1; then use_k8s_mysql=true; fi\n"
                + "run_k8s_mysql_import_job() {\n"
                + "  echo \"importing database dumps through temporary job $MYSQL_IMPORT_JOB\"\n"
                + "  kubectl -n \"$NAMESPACE\" delete job \"$MYSQL_IMPORT_JOB\" --ignore-not-found=true >/dev/null 2>&1 || true\n"
                + "  if ! command -v envsubst >/dev/null 2>&1; then echo \"envsubst is required for k8s mysql import job rendering\" >&2; return 1; fi\n"
                + "  export MYSQL_IMPORT_JOB REMOTE_MYSQL_IMAGE MYSQL_ROOT_PASSWORD ROOT_DIR\n"
                + "  cat <<'YAML' | envsubst '$MYSQL_IMPORT_JOB $REMOTE_MYSQL_IMAGE $MYSQL_ROOT_PASSWORD $ROOT_DIR' | kubectl -n \"$NAMESPACE\" apply -f -\n"
                + "apiVersion: batch/v1\n"
                + "kind: Job\n"
                + "metadata:\n"
                + "  name: $MYSQL_IMPORT_JOB\n"
                + "spec:\n"
                + "  backoffLimit: 0\n"
                + "  template:\n"
                + "    spec:\n"
                + "      restartPolicy: Never\n"
                + "      containers:\n"
                + "        - name: mysql-import\n"
                + "          image: $REMOTE_MYSQL_IMAGE\n"
                + "          imagePullPolicy: IfNotPresent\n"
                + "          env:\n"
                + "            - name: MYSQL_ROOT_PASSWORD\n"
                + "              value: \"$MYSQL_ROOT_PASSWORD\"\n"
                + "          command:\n"
                + "            - sh\n"
                + "            - -lc\n"
                + "            - |\n"
                + "              set -euo pipefail\n"
                + "              cd /rk-import\n"
                + "              until mysqladmin ping -hmysql -P3306 -uroot -p\"$MYSQL_ROOT_PASSWORD\" --silent; do sleep 3; done\n"
                + "              mysql_exec() { MYSQL_PWD=\"$MYSQL_ROOT_PASSWORD\" mysql --binary-mode=1 -hmysql -P3306 -uroot \"$@\"; }\n"
                + "              mysql_fast_import_stream() {\n"
                + "                db=\"${1:-}\"\n"
                + "                if [ -n \"$db\" ]; then\n"
                + "                  { printf '%s\\n' 'SET autocommit=0; SET unique_checks=0; SET foreign_key_checks=0;'; cat; printf '\\n%s\\n' 'COMMIT; SET foreign_key_checks=1; SET unique_checks=1; SET autocommit=1;'; } | MYSQL_PWD=\"$MYSQL_ROOT_PASSWORD\" mysql --binary-mode=1 -hmysql -P3306 -uroot \"$db\"\n"
                + "                else\n"
                + "                  { printf '%s\\n' 'SET autocommit=0; SET unique_checks=0; SET foreign_key_checks=0;'; cat; printf '\\n%s\\n' 'COMMIT; SET foreign_key_checks=1; SET unique_checks=1; SET autocommit=1;'; } | MYSQL_PWD=\"$MYSQL_ROOT_PASSWORD\" mysql --binary-mode=1 -hmysql -P3306 -uroot\n"
                + "                fi\n"
                + "              }\n"
                + "              if [ -f /rk-import/databases/all-current-data.sql.gz ]; then\n"
                + "                echo \"importing all-current-data.sql.gz\"\n"
                + "                gzip -dc /rk-import/databases/all-current-data.sql.gz | mysql_fast_import_stream\n"
                + "              fi\n"
                + "              for dump in /rk-import/databases/*.sql.gz; do\n"
                + "                [ -e \"$dump\" ] || continue\n"
                + "                [ \"$(basename \"$dump\")\" = \"all-current-data.sql.gz\" ] && continue\n"
                + "                db=$(basename \"$dump\" .sql.gz)\n"
                + "                echo \"importing $db\"\n"
                + "                safe_db=$(printf '%s' \"$db\" | sed 's/`/``/g')\n"
                + "                mysql_exec -e \"CREATE DATABASE IF NOT EXISTS \\`$safe_db\\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci\"\n"
                + "                gzip -dc \"$dump\" | mysql_fast_import_stream \"$db\"\n"
                + "              done\n"
                + "          volumeMounts:\n"
                + "            - name: rk-import-root\n"
                + "              mountPath: /rk-import\n"
                + "              readOnly: true\n"
                + "      volumes:\n"
                + "        - name: rk-import-root\n"
                + "          hostPath:\n"
                + "            path: $ROOT_DIR\n"
                + "            type: Directory\n"
                + "YAML\n"
                + "  if ! kubectl -n \"$NAMESPACE\" wait --for=condition=complete job/\"$MYSQL_IMPORT_JOB\" --timeout=30m; then\n"
                + "    kubectl -n \"$NAMESPACE\" logs job/\"$MYSQL_IMPORT_JOB\" --tail=240 || true\n"
                + "    kubectl -n \"$NAMESPACE\" describe job \"$MYSQL_IMPORT_JOB\" || true\n"
                + "    return 1\n"
                + "  fi\n"
                + "  kubectl -n \"$NAMESPACE\" logs job/\"$MYSQL_IMPORT_JOB\" --tail=240 || true\n"
                + "  kubectl -n \"$NAMESPACE\" delete job \"$MYSQL_IMPORT_JOB\" --ignore-not-found=true >/dev/null 2>&1 || true\n"
                + "}\n"
                + "if [ \"$use_k8s_mysql\" = \"true\" ]; then\n"
                + "  run_k8s_mysql_import_job\n"
                + "  exit 0\n"
                + "fi\n"
                + "mysql_exec() {\n"
                + "  if ! command -v mysql >/dev/null 2>&1; then echo \"mysql client is required for non-k8s database import\" >&2; exit 1; fi\n"
                + "  MYSQL_PWD=\"$MYSQL_PASSWORD\" mysql --binary-mode=1 -h\"$MYSQL_HOST\" -P\"$MYSQL_PORT\" -u\"$MYSQL_USER\" \"$@\"\n"
                + "}\n"
                + "mysql_fast_import_stream() {\n"
                + "  db=\"$1\"\n"
                + "  if [ ! -x \"$(command -v mysql || true)\" ]; then echo \"mysql client is required for non-k8s database import\" >&2; exit 1; fi\n"
                + "  if [ -n \"$db\" ]; then\n"
                + "    { printf '%s\\n' 'SET autocommit=0; SET unique_checks=0; SET foreign_key_checks=0;'; cat; printf '\\n%s\\n' 'COMMIT; SET foreign_key_checks=1; SET unique_checks=1; SET autocommit=1;'; } | MYSQL_PWD=\"$MYSQL_PASSWORD\" mysql --binary-mode=1 -h\"$MYSQL_HOST\" -P\"$MYSQL_PORT\" -u\"$MYSQL_USER\" \"$db\"\n"
                + "  else\n"
                + "    { printf '%s\\n' 'SET autocommit=0; SET unique_checks=0; SET foreign_key_checks=0;'; cat; printf '\\n%s\\n' 'COMMIT; SET foreign_key_checks=1; SET unique_checks=1; SET autocommit=1;'; } | MYSQL_PWD=\"$MYSQL_PASSWORD\" mysql --binary-mode=1 -h\"$MYSQL_HOST\" -P\"$MYSQL_PORT\" -u\"$MYSQL_USER\"\n"
                + "  fi\n"
                + "}\n"
                + "if [ -f databases/all-current-data.sql.gz ]; then\n"
                + "  echo \"importing all-current-data.sql.gz\"\n"
                + "  gzip -dc databases/all-current-data.sql.gz | mysql_fast_import_stream \"\"\n"
                + "fi\n"
                + "for dump in databases/*.sql.gz; do\n"
                + "  [ -e \"$dump\" ] || continue\n"
                + "  [ \"$(basename \"$dump\")\" = \"all-current-data.sql.gz\" ] && continue\n"
                + "  db=$(basename \"$dump\" .sql.gz)\n"
                + "  echo \"importing $db\"\n"
                + "  safe_db=$(printf '%s' \"$db\" | sed 's/`/``/g')\n"
                + "  mysql_exec -e \"CREATE DATABASE IF NOT EXISTS \\`$safe_db\\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci\"\n"
                + "  gzip -dc \"$dump\" | mysql_fast_import_stream \"$db\"\n"
                + "done\n";
    }

    private String buildMinioExportScript() {
        return "#!/usr/bin/env bash\n"
                + "set -euo pipefail\n"
                + "MINIO_ENDPOINT=${MINIO_ENDPOINT:-http://rk-minio:9000}\n"
                + "MINIO_ACCESS_KEY=${MINIO_ACCESS_KEY:-minioadmin}\n"
                + "MINIO_SECRET_KEY=${MINIO_SECRET_KEY:-minioadmin}\n"
                + "MC_ALIAS=${MC_ALIAS:-rkminio}\n"
                + "mkdir -p minio\n"
                + "if ! command -v mc >/dev/null 2>&1; then echo \"mc not found, skip MinIO export\"; exit 0; fi\n"
                + "mc alias set \"$MC_ALIAS\" \"$MINIO_ENDPOINT\" \"$MINIO_ACCESS_KEY\" \"$MINIO_SECRET_KEY\"\n"
                + "mc ls \"$MC_ALIAS\" | awk '{print $5}' | sed 's#/##' | while read -r bucket; do\n"
                + "  [ -z \"$bucket\" ] && continue\n"
                + "  echo \"exporting bucket $bucket\"\n"
                + "  mkdir -p \"minio/$bucket\"\n"
                + "  mc mirror --overwrite \"$MC_ALIAS/$bucket\" \"minio/$bucket\" || true\n"
                + "done\n";
    }

    private String buildMinioImportScript() {
        return "#!/usr/bin/env bash\n"
                + "set -euo pipefail\n"
                + "ROOT_DIR=$(cd \"$(dirname \"$0\")/..\" && pwd)\n"
                + "cd \"$ROOT_DIR\"\n"
                + "NAMESPACE=${NAMESPACE:-shetuanguanlixitong}\n"
                + "MINIO_ENDPOINT=${MINIO_ENDPOINT:-http://rk-minio:9000}\n"
                + "MINIO_ACCESS_KEY=${MINIO_ACCESS_KEY:-" + REMOTE_MINIO_ACCESS_KEY + "}\n"
                + "MINIO_SECRET_KEY=${MINIO_SECRET_KEY:-" + REMOTE_MINIO_SECRET_KEY + "}\n"
                + "MINIO_MC_IMAGE=${MINIO_MC_IMAGE:-" + DEFAULT_MINIO_CLIENT_IMAGE + "}\n"
                + "MINIO_IMPORT_JOB=${MINIO_IMPORT_JOB:-rk-linux-ssh-minio-import}\n"
                + "MC_ALIAS=${MC_ALIAS:-rkminio}\n"
                + "if [ ! -d minio ]; then echo \"no minio directory, skip MinIO import\"; exit 0; fi\n"
                + "if command -v kubectl >/dev/null 2>&1 && kubectl -n \"$NAMESPACE\" get svc minio >/dev/null 2>&1; then\n"
                + "  run_k8s_minio_import_job() {\n"
                + "    echo \"importing MinIO objects through temporary job $MINIO_IMPORT_JOB\"\n"
                + "    kubectl -n \"$NAMESPACE\" delete job \"$MINIO_IMPORT_JOB\" --ignore-not-found=true >/dev/null 2>&1 || true\n"
                + "    if ! command -v envsubst >/dev/null 2>&1; then echo \"envsubst is required for k8s MinIO import job rendering\" >&2; return 1; fi\n"
                + "    export MINIO_IMPORT_JOB MINIO_MC_IMAGE MINIO_ENDPOINT MINIO_ACCESS_KEY MINIO_SECRET_KEY ROOT_DIR\n"
                + "    cat <<'YAML' | envsubst '$MINIO_IMPORT_JOB $MINIO_MC_IMAGE $MINIO_ENDPOINT $MINIO_ACCESS_KEY $MINIO_SECRET_KEY $ROOT_DIR' | kubectl -n \"$NAMESPACE\" apply -f -\n"
                + "apiVersion: batch/v1\n"
                + "kind: Job\n"
                + "metadata:\n"
                + "  name: $MINIO_IMPORT_JOB\n"
                + "spec:\n"
                + "  backoffLimit: 0\n"
                + "  template:\n"
                + "    spec:\n"
                + "      restartPolicy: Never\n"
                + "      containers:\n"
                + "        - name: minio-import\n"
                + "          image: $MINIO_MC_IMAGE\n"
                + "          imagePullPolicy: IfNotPresent\n"
                + "          env:\n"
                + "            - name: MINIO_ENDPOINT\n"
                + "              value: \"$MINIO_ENDPOINT\"\n"
                + "            - name: MINIO_ACCESS_KEY\n"
                + "              value: \"$MINIO_ACCESS_KEY\"\n"
                + "            - name: MINIO_SECRET_KEY\n"
                + "              value: \"$MINIO_SECRET_KEY\"\n"
                + "          command:\n"
                + "            - sh\n"
                + "            - -lc\n"
                + "            - |\n"
                + "              set -euo pipefail\n"
                + "              mc alias set rkminio \"$MINIO_ENDPOINT\" \"$MINIO_ACCESS_KEY\" \"$MINIO_SECRET_KEY\"\n"
                + "              if [ -d /rk-import/minio/buckets ]; then\n"
                + "                for dir in /rk-import/minio/buckets/*; do\n"
                + "                  [ -d \"$dir\" ] || continue\n"
                + "                  bucket=${dir%/}\n"
                + "                  bucket=${bucket##*/}\n"
                + "                  [ -n \"$bucket\" ] || continue\n"
                + "                  mc mb --ignore-existing \"rkminio/$bucket\"\n"
                + "                  mc mirror --overwrite \"$dir\" \"rkminio/$bucket\"\n"
                + "                  mc anonymous set download \"rkminio/$bucket\"\n"
                + "                  echo \"MinIO bucket policy restored for $bucket\"\n"
                + "                done\n"
                + "              else\n"
                + "                echo \"no minio buckets directory in package\"\n"
                + "              fi\n"
                + "          volumeMounts:\n"
                + "            - name: rk-import-root\n"
                + "              mountPath: /rk-import\n"
                + "              readOnly: true\n"
                + "      volumes:\n"
                + "        - name: rk-import-root\n"
                + "          hostPath:\n"
                + "            path: $ROOT_DIR\n"
                + "            type: Directory\n"
                + "YAML\n"
                + "    if ! kubectl -n \"$NAMESPACE\" wait --for=condition=complete job/\"$MINIO_IMPORT_JOB\" --timeout=60m; then\n"
                + "      kubectl -n \"$NAMESPACE\" logs job/\"$MINIO_IMPORT_JOB\" --tail=240 || true\n"
                + "      kubectl -n \"$NAMESPACE\" describe job \"$MINIO_IMPORT_JOB\" || true\n"
                + "      return 1\n"
                + "    fi\n"
                + "    kubectl -n \"$NAMESPACE\" logs job/\"$MINIO_IMPORT_JOB\" --tail=240 || true\n"
                + "    kubectl -n \"$NAMESPACE\" delete job \"$MINIO_IMPORT_JOB\" --ignore-not-found=true >/dev/null 2>&1 || true\n"
                + "  }\n"
                + "  run_k8s_minio_import_job\n"
                + "  exit 0\n"
                + "fi\n"
                + "if ! command -v mc >/dev/null 2>&1; then echo \"mc client is required for non-k8s MinIO import\" >&2; exit 1; fi\n"
                + "mc alias set \"$MC_ALIAS\" \"$MINIO_ENDPOINT\" \"$MINIO_ACCESS_KEY\" \"$MINIO_SECRET_KEY\"\n"
                + "for dir in minio/buckets/*; do\n"
                + "  [ -d \"$dir\" ] || continue\n"
                + "  bucket=${dir%/}\n"
                + "  bucket=${bucket##*/}\n"
                + "  [ -z \"$bucket\" ] && continue\n"
                + "  mc mb --ignore-existing \"$MC_ALIAS/$bucket\"\n"
                + "  mc mirror --overwrite \"$dir\" \"$MC_ALIAS/$bucket\"\n"
                + "  mc anonymous set download \"$MC_ALIAS/$bucket\"\n"
                + "  echo \"MinIO bucket policy restored for $bucket\"\n"
                + "done\n";
    }

    private String buildComposeEnvExample(AdminDeployPackageCreateDTO request) {
        return "RK_WEB_TAG=latest\n"
                + "REGISTRY_PREFIX=" + trimToDefault(request.getRegistryPrefix(), DEFAULT_REGISTRY_PREFIX) + "\n"
                + "RK_WEB_DOMAIN=" + resolveRemoteDeployDomain(request) + "\n"
                + "MYSQL_ROOT_PASSWORD=123456\n"
                + "MYSQL_DATABASE=rk_user\n"
                + "RABBITMQ_DEFAULT_USER=tjxt\n"
                + "RABBITMQ_DEFAULT_PASS=123321\n"
                + "MINIO_ROOT_USER=minioadmin\n"
                + "MINIO_ROOT_PASSWORD=minioadmin\n"
                + "NACOS_AUTH_ENABLE=false\n";
    }

    private String buildDockerComposeFile(AdminDeployPackageCreateDTO request) {
        Map<String, String> serviceImages = currentRuntimeImagesByRepository();
        String rkComposeEnvironment = buildDockerComposeServiceEnvironment();
        return "services:\n"
                + "  mysql:\n"
                + "    image: mysql:8.0\n"
                + "    command: --default-authentication-plugin=mysql_native_password --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci --max_connections=512\n"
                + "    environment:\n"
                + "      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-123456}\n"
                + "    ports:\n"
                + "      - \"3306:3306\"\n"
                + "    volumes:\n"
                + "      - mysql-data:/var/lib/mysql\n"
                + "  redis:\n"
                + "    image: redis:7-alpine\n"
                + "    ports:\n"
                + "      - \"6379:6379\"\n"
                + "  rabbitmq:\n"
                + "    image: heidiks/rabbitmq-delayed-message-exchange:3.8.27-management\n"
                + "    environment:\n"
                + "      RABBITMQ_DEFAULT_USER: ${RABBITMQ_DEFAULT_USER:-tjxt}\n"
                + "      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_DEFAULT_PASS:-123321}\n"
                + "    ports:\n"
                + "      - \"5672:5672\"\n"
                + "      - \"15672:15672\"\n"
                + "  minio:\n"
                + "    image: minio/minio:RELEASE.2024-05-10T01-41-38Z\n"
                + "    command: server /data --console-address :9001\n"
                + "    environment:\n"
                + "      MINIO_ROOT_USER: ${MINIO_ROOT_USER:-minioadmin}\n"
                + "      MINIO_ROOT_PASSWORD: ${MINIO_ROOT_PASSWORD:-minioadmin}\n"
                + "    ports:\n"
                + "      - \"9000:9000\"\n"
                + "      - \"9001:9001\"\n"
                + "    volumes:\n"
                + "      - minio-data:/data\n"
                + "  nacos:\n"
                + "    image: nacos/nacos-server:v2.3.2\n"
                + "    environment:\n"
                + "      MODE: standalone\n"
                + "      NACOS_AUTH_ENABLE: ${NACOS_AUTH_ENABLE:-false}\n"
                + "    ports:\n"
                + "      - \"8848:8848\"\n"
                + "  seata:\n"
                + "    image: seataio/seata-server:1.7.1\n"
                + "    environment:\n"
                + "      SEATA_IP: seata\n"
                + "    depends_on: [mysql, nacos]\n"
                + "    ports:\n"
                + "      - \"8099:8099\"\n"
                + "      - \"7099:7099\"\n"
                + "  xxl-job:\n"
                + "    image: xuxueli/xxl-job-admin:2.3.0\n"
                + "    depends_on: [mysql]\n"
                + "    ports:\n"
                + "      - \"8880:8880\"\n"
                + "  elasticsearch:\n"
                + "    image: elasticsearch:7.12.1\n"
                + "    environment:\n"
                + "      discovery.type: single-node\n"
                + "      ES_JAVA_OPTS: ${ES_JAVA_OPTS:--Xms512m -Xmx512m}\n"
                + "    ports:\n"
                + "      - \"9200:9200\"\n"
                + "      - \"9300:9300\"\n"
                + "  sentinel:\n"
                + "    image: bladex/sentinel-dashboard:1.8.6\n"
                + "    ports:\n"
                + "      - \"8858:8858\"\n"
                + "  rk-gateway:\n"
                + "    image: " + imageForService(request, "rk-gateway", serviceImages) + "\n"
                + rkComposeEnvironment
                + "    depends_on: [mysql, redis, rabbitmq, nacos, seata]\n"
                + "    ports:\n"
                + "      - \"10010:10010\"\n"
                + "  rk-auth:\n"
                + "    image: " + imageForService(request, "rk-auth", serviceImages) + "\n"
                + rkComposeEnvironment
                + "    depends_on: [mysql, redis, rabbitmq, nacos, seata]\n"
                + "  rk-user:\n"
                + "    image: " + imageForService(request, "rk-user", serviceImages) + "\n"
                + rkComposeEnvironment
                + "    depends_on: [mysql, redis, rabbitmq, nacos, seata, minio]\n"
                + "  rk-search:\n"
                + "    image: " + imageForService(request, "rk-search", serviceImages) + "\n"
                + rkComposeEnvironment
                + "    depends_on: [mysql, redis, rabbitmq, nacos, seata, elasticsearch]\n"
                + "  rk-file:\n"
                + "    image: " + imageForService(request, "rk-file", serviceImages) + "\n"
                + rkComposeEnvironment
                + "    depends_on: [mysql, redis, rabbitmq, nacos, seata, minio]\n"
                + "  rk-message:\n"
                + "    image: " + imageForService(request, "rk-message", serviceImages) + "\n"
                + rkComposeEnvironment
                + "    depends_on: [mysql, redis, rabbitmq, nacos, seata]\n"
                + "  rk-content:\n"
                + "    image: " + imageForService(request, "rk-content", serviceImages) + "\n"
                + rkComposeEnvironment
                + "    depends_on: [mysql, redis, rabbitmq, nacos, seata, minio]\n"
                + "  rk-pay:\n"
                + "    image: " + imageForService(request, "rk-pay", serviceImages) + "\n"
                + rkComposeEnvironment
                + "    depends_on: [mysql, redis, rabbitmq, nacos, seata]\n"
                + "  rk-trade:\n"
                + "    image: " + imageForService(request, "rk-trade", serviceImages) + "\n"
                + rkComposeEnvironment
                + "    depends_on: [mysql, redis, rabbitmq, nacos, seata]\n"
                + "  rk-exam:\n"
                + "    image: " + imageForService(request, "rk-exam", serviceImages) + "\n"
                + rkComposeEnvironment
                + "    depends_on: [mysql, redis, rabbitmq, nacos, seata]\n"
                + "  rk-activity:\n"
                + "    image: " + imageForService(request, "rk-activity", serviceImages) + "\n"
                + rkComposeEnvironment
                + "    depends_on: [mysql, redis, rabbitmq, nacos, seata]\n"
                + "  rk-data:\n"
                + "    image: " + imageForService(request, "rk-data", serviceImages) + "\n"
                + rkComposeEnvironment
                + "    depends_on: [mysql, redis, rabbitmq, nacos, seata]\n"
                + "  frontend:\n"
                + "    image: " + imageForService(request, "rk-web-frontend", serviceImages) + "\n"
                + "    environment:\n"
                + "      RK_GATEWAY_URL: http://rk-gateway:10010\n"
                + "      RK_MINIO_URL: http://minio:9000\n"
                + "    depends_on: [rk-gateway]\n"
                + "    ports:\n"
                + "      - \"80:80\"\n"
                + "volumes:\n"
                + "  mysql-data:\n"
                + "  minio-data:\n";
    }

    private String buildDockerComposeServiceEnvironment() {
        return "    environment:\n"
                + "      SPRING_CLOUD_NACOS_SERVER_ADDR: nacos:8848\n"
                + "      SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR: nacos:8848\n"
                + "      SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR: nacos:8848\n"
                + "      SPRING_CLOUD_NACOS_USERNAME: nacos\n"
                + "      SPRING_CLOUD_NACOS_PASSWORD: nacos\n"
                + "      DB_HOST: mysql\n"
                + "      DB_USER: root\n"
                + "      DB_PWD: ${MYSQL_ROOT_PASSWORD:-123456}\n"
                + "      SPRING_DATASOURCE_USERNAME: root\n"
                + "      SPRING_DATASOURCE_PASSWORD: ${MYSQL_ROOT_PASSWORD:-123456}\n"
                + "      SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE: 5\n"
                + "      SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE: 1\n"
                + "      REDIS_HOST: redis\n"
                + "      SPRING_REDIS_HOST: redis\n"
                + "      RABBIT_HOST: rabbitmq\n"
                + "      RABBIT_USER: ${RABBITMQ_DEFAULT_USER:-tjxt}\n"
                + "      RABBIT_PWD: ${RABBITMQ_DEFAULT_PASS:-123321}\n"
                + "      RABBIT_VHOST: /tjxt\n"
                + "      SPRING_RABBITMQ_HOST: rabbitmq\n"
                + "      SPRING_RABBITMQ_USERNAME: ${RABBITMQ_DEFAULT_USER:-tjxt}\n"
                + "      SPRING_RABBITMQ_PASSWORD: ${RABBITMQ_DEFAULT_PASS:-123321}\n"
                + "      SPRING_RABBITMQ_VIRTUAL_HOST: /tjxt\n"
                + "      MINIO_ENDPOINT: http://minio:9000\n"
                + "      MINIO_PUBLIC_BASE_URL: /minio-files/\n"
                + "      XXL_JOB_ADMIN_ADDRESSES: http://xxl-job:8880/xxl-job-admin\n";
    }

    private String buildK8sStackManifest(AdminDeployPackageCreateDTO request) {
        String namespace = trimToDefault(request.getTargetNamespace(), "shetuanguanlixitong");
        Map<String, String> serviceImages = currentRuntimeImagesByRepository(request);
        String mysqlImage = REMOTE_MYSQL_IMAGE;
        String redisImage = REMOTE_REDIS_IMAGE;
        String rabbitmqImage = rabbitmqImageForRuntimeComponent(request, serviceImages);
        String minioImage = REMOTE_MINIO_IMAGE;
        String nacosImage = REMOTE_NACOS_IMAGE;
        String rkUserImage = imageForService(request, "rk-user", serviceImages);
        String seataImage = imageForRuntimeComponent(request, serviceImages, "seataio/seata-server:1.7.1", "seata", "seata-server", "rk-seata");
        String xxlJobImage = imageForRuntimeComponent(request, serviceImages, "xuxueli/xxl-job-admin:2.3.0", "xxl-job", "xxl-job-admin", "rk-xxl-job");
        String elasticsearchImage = imageForRuntimeComponent(request, serviceImages, "elasticsearch:7.12.1", "elasticsearch", "rk-elasticsearch");
        String domain = resolveRemoteDeployDomain(request);
        return "apiVersion: v1\n"
                + "kind: Namespace\n"
                + "metadata:\n"
                + "  name: " + namespace + "\n"
                + "---\n"
                + "apiVersion: v1\n"
                + "kind: ConfigMap\n"
                + "metadata:\n"
                + "  name: rk-web-runtime\n"
                + "  namespace: " + namespace + "\n"
                + "data:\n"
                + "  RK_WEB_DOMAIN: \"" + domain + "\"\n"
                + "  REGISTRY_PREFIX: \"" + trimToDefault(request.getRegistryPrefix(), DEFAULT_REGISTRY_PREFIX) + "\"\n"
                + "---\n"
                + buildSimpleService(namespace, "mysql", 3306, 3306)
                + "---\n"
                + buildAliasService(namespace, "rk-mysql", "mysql", 3306, 3306)
                + "---\n"
                + buildPersistentStatefulSet(namespace, "mysql", mysqlImage, 3306, request)
                + "---\n"
                + buildSimpleService(namespace, "redis", 6379, 6379)
                + "---\n"
                + buildAliasService(namespace, "rk-redis", "redis", 6379, 6379)
                + "---\n"
                + buildPersistentStatefulSet(namespace, "redis", redisImage, 6379, request)
                + "---\n"
                + buildSimpleService(namespace, "rabbitmq", 5672, 5672)
                + "---\n"
                + buildAliasService(namespace, "rk-rabbitmq", "rabbitmq", 5672, 5672)
                + "---\n"
                + buildPersistentStatefulSet(namespace, "rabbitmq", rabbitmqImage, 5672, request)
                + "---\n"
                + buildRabbitMqTopologyJob(namespace, rabbitmqImage)
                + "---\n"
                + buildSimpleService(namespace, "minio", 9000, 9000)
                + "---\n"
                + buildAliasService(namespace, "rk-minio", "minio", 9000, 9000)
                + "---\n"
                + buildPersistentStatefulSet(namespace, "minio", minioImage, 9000, request)
                + "---\n"
                + buildSimpleService(namespace, "nacos", 8848, 8848)
                + "---\n"
                + buildPersistentStatefulSet(namespace, "nacos", nacosImage, 8848, rkUserImage, request)
                + "---\n"
                + buildSimpleDeployment(namespace, "seata", seataImage, 8099)
                + "---\n"
                + buildSimpleService(namespace, "seata", 8099, 8099)
                + "---\n"
                + buildSimpleDeployment(namespace, "xxl-job", xxlJobImage, 8880)
                + "---\n"
                + buildSimpleService(namespace, "xxl-job", 8880, 8880)
                + "---\n"
                + buildAliasService(namespace, "rk-xxl-job", "xxl-job", 8080, 8880)
                + "---\n"
                + buildGogsStatefulSet(namespace, request)
                + "---\n"
                + buildSimpleService(namespace, "rk-gogs", 3000, 3000)
                + "---\n"
                + buildJenkinsStatefulSet(namespace, request)
                + "---\n"
                + buildSimpleService(namespace, "rk-jenkins", 8080, 8080)
                + "---\n"
                + buildSimpleService(namespace, "elasticsearch", 9200, 9200)
                + "---\n"
                + buildPersistentStatefulSet(namespace, "elasticsearch", elasticsearchImage, 9200, request)
                + "---\n"
                + buildSentinelDashboardDeployment(namespace)
                + "---\n"
                + buildSimpleService(namespace, "sentinel", 8858, 8858)
                + "---\n"
                + buildSimpleDeployment(namespace, "rk-gateway", imageForService(request, "rk-gateway", serviceImages), 10010)
                + "---\n"
                + buildSimpleService(namespace, "rk-gateway", 10010, 10010)
                + "---\n"
                + buildSimpleDeployment(namespace, "rk-auth", imageForService(request, "rk-auth", serviceImages), 8081)
                + "---\n"
                + buildSimpleService(namespace, "rk-auth", 8081, 8081)
                + "---\n"
                + buildSimpleDeployment(namespace, "rk-user", rkUserImage, 8082)
                + "---\n"
                + buildSimpleService(namespace, "rk-user", 8082, 8082)
                + "---\n"
                + buildSimpleDeployment(namespace, "rk-search", imageForService(request, "rk-search", serviceImages), 8083)
                + "---\n"
                + buildSimpleService(namespace, "rk-search", 8083, 8083)
                + "---\n"
                + buildSimpleDeployment(namespace, "rk-file", imageForService(request, "rk-file", serviceImages), 8084)
                + "---\n"
                + buildSimpleService(namespace, "rk-file", 8084, 8084)
                + "---\n"
                + buildSimpleDeployment(namespace, "rk-message", imageForService(request, "rk-message", serviceImages), 8085)
                + "---\n"
                + buildSimpleService(namespace, "rk-message", 8085, 8085)
                + "---\n"
                + buildSimpleDeployment(namespace, "rk-content", imageForService(request, "rk-content", serviceImages), 8086)
                + "---\n"
                + buildSimpleService(namespace, "rk-content", 8086, 8086)
                + "---\n"
                + buildSimpleDeployment(namespace, "rk-pay", imageForService(request, "rk-pay", serviceImages), 8087)
                + "---\n"
                + buildSimpleService(namespace, "rk-pay", 8087, 8087)
                + "---\n"
                + buildSimpleDeployment(namespace, "rk-trade", imageForService(request, "rk-trade", serviceImages), 8088)
                + "---\n"
                + buildSimpleService(namespace, "rk-trade", 8088, 8088)
                + "---\n"
                + buildSimpleDeployment(namespace, "rk-exam", imageForService(request, "rk-exam", serviceImages), 8089)
                + "---\n"
                + buildSimpleService(namespace, "rk-exam", 8089, 8089)
                + "---\n"
                + buildSimpleDeployment(namespace, "rk-activity", imageForService(request, "rk-activity", serviceImages), 8090)
                + "---\n"
                + buildSimpleService(namespace, "rk-activity", 8090, 8090)
                + "---\n"
                + buildSimpleDeployment(namespace, "rk-data", imageForService(request, "rk-data", serviceImages), 8093)
                + "---\n"
                + buildSimpleService(namespace, "rk-data", 8093, 8093)
                + "---\n"
                + buildSimpleDeployment(namespace, "rk-web-frontend", imageForService(request, "rk-web-frontend", serviceImages), 80)
                + "---\n"
                + buildSimpleService(namespace, "rk-web-frontend", 80, 80)
                + buildExternalExposureManifest(request, namespace);
    }

    private String buildK8sInfraManifest(AdminDeployPackageCreateDTO request) {
        String namespace = trimToDefault(request.getTargetNamespace(), "shetuanguanlixitong");
        Map<String, String> serviceImages = currentRuntimeImagesByRepository(request);
        String rabbitmqImage = rabbitmqImageForRuntimeComponent(request, serviceImages);
        String elasticsearchImage = imageForRuntimeComponent(request, serviceImages, "elasticsearch:7.12.1", "elasticsearch", "rk-elasticsearch");
        String domain = resolveRemoteDeployDomain(request);
        return "apiVersion: v1\n"
                + "kind: Namespace\n"
                + "metadata:\n"
                + "  name: " + namespace + "\n"
                + "---\n"
                + "apiVersion: v1\n"
                + "kind: ConfigMap\n"
                + "metadata:\n"
                + "  name: rk-web-runtime\n"
                + "  namespace: " + namespace + "\n"
                + "data:\n"
                + "  RK_WEB_DOMAIN: \"" + domain + "\"\n"
                + "  REGISTRY_PREFIX: \"" + trimToDefault(request.getRegistryPrefix(), DEFAULT_REGISTRY_PREFIX) + "\"\n"
                + "---\n"
                + buildSimpleService(namespace, "mysql", 3306, 3306)
                + "---\n"
                + buildAliasService(namespace, "rk-mysql", "mysql", 3306, 3306)
                + "---\n"
                + buildPersistentStatefulSet(namespace, "mysql", REMOTE_MYSQL_IMAGE, 3306, request)
                + "---\n"
                + buildSimpleService(namespace, "redis", 6379, 6379)
                + "---\n"
                + buildAliasService(namespace, "rk-redis", "redis", 6379, 6379)
                + "---\n"
                + buildPersistentStatefulSet(namespace, "redis", REMOTE_REDIS_IMAGE, 6379, request)
                + "---\n"
                + buildSimpleService(namespace, "rabbitmq", 5672, 5672)
                + "---\n"
                + buildAliasService(namespace, "rk-rabbitmq", "rabbitmq", 5672, 5672)
                + "---\n"
                + buildPersistentStatefulSet(namespace, "rabbitmq", rabbitmqImage, 5672, request)
                + "---\n"
                + buildRabbitMqTopologyJob(namespace, rabbitmqImage)
                + "---\n"
                + buildSimpleService(namespace, "minio", 9000, 9000)
                + "---\n"
                + buildAliasService(namespace, "rk-minio", "minio", 9000, 9000)
                + "---\n"
                + buildPersistentStatefulSet(namespace, "minio", REMOTE_MINIO_IMAGE, 9000, request)
                + "---\n"
                + buildSimpleService(namespace, "elasticsearch", 9200, 9200)
                + "---\n"
                + buildPersistentStatefulSet(namespace, "elasticsearch", elasticsearchImage, 9200, request);
    }

    private String buildK8sNacosManifest(AdminDeployPackageCreateDTO request) {
        String namespace = trimToDefault(request.getTargetNamespace(), "shetuanguanlixitong");
        Map<String, String> serviceImages = currentRuntimeImagesByRepository();
        String rkUserImage = imageForService(request, "rk-user", serviceImages);
        return "apiVersion: v1\n"
                + "kind: Service\n"
                + "metadata:\n"
                + "  name: nacos\n"
                + "  namespace: " + namespace + "\n"
                + "spec:\n"
                + "  selector:\n"
                + "    app: nacos\n"
                + "  ports:\n"
                + "    - name: port-8848\n"
                + "      port: 8848\n"
                + "      targetPort: 8848\n"
                + "---\n"
                + buildPersistentStatefulSet(namespace, "nacos", REMOTE_NACOS_IMAGE, 8848, rkUserImage, request);
    }

    private String buildK8sApplicationManifest(AdminDeployPackageCreateDTO request) {
        String namespace = trimToDefault(request.getTargetNamespace(), "shetuanguanlixitong");
        Map<String, String> serviceImages = currentRuntimeImagesByRepository();
        String rkUserImage = imageForService(request, "rk-user", serviceImages);
        String seataImage = imageForRuntimeComponent(request, serviceImages, "seataio/seata-server:1.7.1", "seata", "seata-server", "rk-seata");
        String xxlJobImage = imageForRuntimeComponent(request, serviceImages, "xuxueli/xxl-job-admin:2.3.0", "xxl-job", "xxl-job-admin", "rk-xxl-job");
        return buildSimpleDeployment(namespace, "seata", seataImage, 8099)
                + "---\n"
                + buildSimpleService(namespace, "seata", 8099, 8099)
                + "---\n"
                + buildSimpleDeployment(namespace, "xxl-job", xxlJobImage, 8880)
                + "---\n"
                + buildSimpleService(namespace, "xxl-job", 8880, 8880)
                + "---\n"
                + buildAliasService(namespace, "rk-xxl-job", "xxl-job", 8080, 8880)
                + "---\n"
                + buildGogsStatefulSet(namespace, request)
                + "---\n"
                + buildSimpleService(namespace, "rk-gogs", 3000, 3000)
                + "---\n"
                + buildJenkinsStatefulSet(namespace, request)
                + "---\n"
                + buildSimpleService(namespace, "rk-jenkins", 8080, 8080)
                + "---\n"
                + buildSentinelDashboardDeployment(namespace)
                + "---\n"
                + buildSimpleService(namespace, "sentinel", 8858, 8858)
                + "---\n"
                + buildSimpleDeployment(namespace, "rk-gateway", imageForService(request, "rk-gateway", serviceImages), 10010)
                + "---\n"
                + buildSimpleService(namespace, "rk-gateway", 10010, 10010)
                + "---\n"
                + buildSimpleDeployment(namespace, "rk-auth", imageForService(request, "rk-auth", serviceImages), 8081)
                + "---\n"
                + buildSimpleService(namespace, "rk-auth", 8081, 8081)
                + "---\n"
                + buildSimpleDeployment(namespace, "rk-user", rkUserImage, 8082)
                + "---\n"
                + buildSimpleService(namespace, "rk-user", 8082, 8082)
                + "---\n"
                + buildSimpleDeployment(namespace, "rk-search", imageForService(request, "rk-search", serviceImages), 8083)
                + "---\n"
                + buildSimpleService(namespace, "rk-search", 8083, 8083)
                + "---\n"
                + buildSimpleDeployment(namespace, "rk-file", imageForService(request, "rk-file", serviceImages), 8084)
                + "---\n"
                + buildSimpleService(namespace, "rk-file", 8084, 8084)
                + "---\n"
                + buildSimpleDeployment(namespace, "rk-message", imageForService(request, "rk-message", serviceImages), 8085)
                + "---\n"
                + buildSimpleService(namespace, "rk-message", 8085, 8085)
                + "---\n"
                + buildSimpleDeployment(namespace, "rk-content", imageForService(request, "rk-content", serviceImages), 8086)
                + "---\n"
                + buildSimpleService(namespace, "rk-content", 8086, 8086)
                + "---\n"
                + buildSimpleDeployment(namespace, "rk-pay", imageForService(request, "rk-pay", serviceImages), 8087)
                + "---\n"
                + buildSimpleService(namespace, "rk-pay", 8087, 8087)
                + "---\n"
                + buildSimpleDeployment(namespace, "rk-trade", imageForService(request, "rk-trade", serviceImages), 8088)
                + "---\n"
                + buildSimpleService(namespace, "rk-trade", 8088, 8088)
                + "---\n"
                + buildSimpleDeployment(namespace, "rk-exam", imageForService(request, "rk-exam", serviceImages), 8089)
                + "---\n"
                + buildSimpleService(namespace, "rk-exam", 8089, 8089)
                + "---\n"
                + buildSimpleDeployment(namespace, "rk-activity", imageForService(request, "rk-activity", serviceImages), 8090)
                + "---\n"
                + buildSimpleService(namespace, "rk-activity", 8090, 8090)
                + "---\n"
                + buildSimpleDeployment(namespace, "rk-data", imageForService(request, "rk-data", serviceImages), 8093)
                + "---\n"
                + buildSimpleService(namespace, "rk-data", 8093, 8093)
                + "---\n"
                + buildSimpleDeployment(namespace, "rk-web-frontend", imageForService(request, "rk-web-frontend", serviceImages), 80)
                + "---\n"
                + buildSimpleService(namespace, "rk-web-frontend", 80, 80)
                + buildExternalExposureManifest(request, namespace);
    }

    private String buildSimpleDeployment(String namespace, String name, String image, int port) {
        String pullSecretName = trimToDefault(deployPackageRegistryImagePullSecretName, "rk-aliyun-regcred");
        return "apiVersion: apps/v1\n"
                + "kind: Deployment\n"
                + "metadata:\n"
                + "  name: " + name + "\n"
                + "  namespace: " + namespace + "\n"
                + "spec:\n"
                + "  replicas: 1\n"
                + "  selector:\n"
                + "    matchLabels:\n"
                + "      app: " + name + "\n"
                + "  template:\n"
                + "    metadata:\n"
                + "      labels:\n"
                + "        app: " + name + "\n"
                + "    spec:\n"
                + "      imagePullSecrets:\n"
                + "        - name: " + pullSecretName + "\n"
                + buildK8sInitContainers(name)
                + "      containers:\n"
                + "        - name: " + name + "\n"
                + "          image: " + image + "\n"
                + "          imagePullPolicy: IfNotPresent\n"
                + "          ports:\n"
                + "            - containerPort: " + port + "\n"
                + buildK8sDeploymentArgs(name)
                + buildK8sDeploymentEnv(name)
                + buildK8sDeploymentResources(name)
                + buildK8sVolumeMounts(name)
                + buildK8sVolumes(name);
    }

    private String buildGogsStatefulSet(String namespace, AdminDeployPackageCreateDTO request) {
        return buildPersistentStatefulSet(namespace, "rk-gogs", REMOTE_GOGS_IMAGE, 3000, request);
    }

    private String buildJenkinsStatefulSet(String namespace, AdminDeployPackageCreateDTO request) {
        return buildPersistentStatefulSet(namespace, "rk-jenkins", REMOTE_JENKINS_IMAGE, 8080, request);
    }

    private String buildPersistentStatefulSet(String namespace, String name, String image, int port, AdminDeployPackageCreateDTO request) {
        return buildPersistentStatefulSet(namespace, name, image, port, image, request);
    }

    private String buildPersistentStatefulSet(String namespace, String name, String image, int port, String helperImage, AdminDeployPackageCreateDTO request) {
        String pullSecretName = trimToDefault(deployPackageRegistryImagePullSecretName, "rk-aliyun-regcred");
        return "apiVersion: apps/v1\n"
                + "kind: StatefulSet\n"
                + "metadata:\n"
                + "  name: " + name + "\n"
                + "  namespace: " + namespace + "\n"
                + "spec:\n"
                + "  serviceName: " + name + "\n"
                + "  replicas: 1\n"
                + "  selector:\n"
                + "    matchLabels:\n"
                + "      app: " + name + "\n"
                + "  template:\n"
                + "    metadata:\n"
                + "      labels:\n"
                + "        app: " + name + "\n"
                + "    spec:\n"
                + "      imagePullSecrets:\n"
                + "        - name: " + pullSecretName + "\n"
                + buildK8sPodSecurityContext(name)
                + buildK8sInitContainers(name, helperImage)
                + "      containers:\n"
                + "        - name: " + name + "\n"
                + "          image: " + image + "\n"
                + "          imagePullPolicy: IfNotPresent\n"
                + "          ports:\n"
                + "            - containerPort: " + port + "\n"
                + buildK8sDeploymentArgs(name)
                + buildK8sDeploymentEnv(name)
                + buildK8sDeploymentResources(name)
                + buildK8sVolumeMounts(name)
                + buildK8sVolumes(name)
                + buildK8sVolumeClaimTemplate(name, request);
    }

    private String buildK8sPodSecurityContext(String name) {
        if (!"elasticsearch".equals(name)) {
            return "";
        }
        return "      securityContext:\n"
                + "        fsGroup: 1000\n";
    }

    private String buildK8sInitContainers(String name) {
        return buildK8sInitContainers(name, REMOTE_BUSYBOX_IMAGE);
    }

    private String buildK8sInitContainers(String name, String helperImage) {
        if ("nacos".equals(name)) {
            String image = trimToDefault(helperImage, imageForRuntimeComponent(null, Collections.emptyMap(), "rk-user", "rk-user"));
            return "      initContainers:\n"
                    + "        - name: copy-mysql-driver\n"
                    + "          image: " + image + "\n"
                    + "          imagePullPolicy: IfNotPresent\n"
                    + "          command:\n"
                    + "            - sh\n"
                    + "            - -c\n"
                    + "            - set -e; rm -rf /tmp/rk-layers; mkdir -p /nacos-mysql; java -Djarmode=layertools -jar /app/app.jar extract --destination /tmp/rk-layers; driver=$(find /tmp/rk-layers -type f -name 'mysql-connector*.jar' | head -n 1); test -n \"$driver\"; cp \"$driver\" /nacos-mysql/; ls -l /nacos-mysql\n"
                    + "          volumeMounts:\n"
                    + "            - name: nacos-mysql-plugin\n"
                    + "              mountPath: /nacos-mysql\n";
        }
        if ("rabbitmq".equals(name)) {
            return "      initContainers:\n"
                    + "        - name: rabbitmq-delayed-plugin\n"
                    + "          image: " + REMOTE_BUSYBOX_IMAGE + "\n"
                    + "          imagePullPolicy: IfNotPresent\n"
                    + "          command:\n"
                    + "            - sh\n"
                    + "            - -c\n"
                    + "            - |\n"
                    + "              set -e\n"
                    + "              plugin=/rk-rabbitmq-plugins/" + RABBITMQ_DELAYED_PLUGIN_FILE + "\n"
                    + "              wget -T 60 -O \"$plugin.tmp\" \"" + RABBITMQ_DELAYED_PLUGIN_URL + "\" || wget -T 60 -O \"$plugin.tmp\" \"" + RABBITMQ_DELAYED_PLUGIN_MIRROR_URL + "\"\n"
                    + "              mv \"$plugin.tmp\" \"$plugin\"\n"
                    + "              chmod 0644 \"$plugin\"\n"
                    + "              ls -l /rk-rabbitmq-plugins\n"
                    + "          volumeMounts:\n"
                    + "            - name: rabbitmq-delayed-plugin\n"
                    + "              mountPath: /rk-rabbitmq-plugins\n";
        }
        if ("rk-gogs".equals(name) || "rk-jenkins".equals(name)) {
            String image = REMOTE_BUSYBOX_IMAGE;
            String mountPath = "rk-jenkins".equals(name) ? "/var/jenkins_home" : "/data";
            String owner = "rk-jenkins".equals(name) ? "1000:1000" : "1000:1000";
            return "      initContainers:\n"
                    + "        - name: fix-data-permissions\n"
                    + "          image: " + image + "\n"
                    + "          imagePullPolicy: IfNotPresent\n"
                    + "          securityContext:\n"
                    + "            runAsUser: 0\n"
                    + "          command:\n"
                    + "            - sh\n"
                    + "            - -c\n"
                    + "            - set -e; mkdir -p " + mountPath + "; chown -R " + owner + " " + mountPath + "\n"
                    + "          volumeMounts:\n"
                    + "            - name: data\n"
                    + "              mountPath: " + mountPath + "\n";
        }
        if (!"elasticsearch".equals(name)) {
            return "";
        }
        String image = trimToDefault(helperImage, REMOTE_BUSYBOX_IMAGE);
        return "      initContainers:\n"
                + "        - name: fix-data-permissions\n"
                + "          image: " + image + "\n"
                + "          imagePullPolicy: IfNotPresent\n"
                + "          securityContext:\n"
                + "            runAsUser: 0\n"
                + "          command:\n"
                + "            - sh\n"
                + "            - -c\n"
                + "            - set -e; mkdir -p /usr/share/elasticsearch/data; chown -R 1000:0 /usr/share/elasticsearch/data\n"
                + "          volumeMounts:\n"
                + "            - name: data\n"
                + "              mountPath: /usr/share/elasticsearch/data\n";
    }

    private String buildK8sDeploymentArgs(String name) {
        if ("redis".equals(name)) {
            return "          args:\n"
                    + "            - --requirepass\n"
                    + "            - \"123321\"\n"
                    + "            - --appendonly\n"
                    + "            - \"yes\"\n"
                    + "            - --dir\n"
                    + "            - /data\n";
        }
        if ("minio".equals(name)) {
            return "          args:\n"
                    + "            - server\n"
                    + "            - /data\n"
                    + "            - --console-address\n"
                    + "            - :9001\n";
        }
        if ("mysql".equals(name)) {
            return "          args:\n"
                    + "            - --default-authentication-plugin=mysql_native_password\n"
                    + "            - --character-set-server=utf8mb4\n"
                    + "            - --collation-server=utf8mb4_unicode_ci\n"
                    + "            - --max_connections=512\n";
        }
        if ("rabbitmq".equals(name)) {
            return "          command:\n"
                    + "            - sh\n"
                    + "            - -c\n"
                    + "            - |\n"
                    + "              set -e\n"
                    + "              mkdir -p /etc/rabbitmq/conf.d\n"
                    + "              cat > /etc/rabbitmq/conf.d/20-rk-defaults.conf <<'EOF'\n"
                    + "              default_user = tjxt\n"
                    + "              default_pass = 123321\n"
                    + "              default_vhost = /tjxt\n"
                    + "              loopback_users.guest = false\n"
                    + "              EOF\n"
                    + "              cp /rk-rabbitmq-plugins/rabbitmq_delayed_message_exchange-*.ez /opt/rabbitmq/plugins/\n"
                    + "              rabbitmq-plugins enable --offline rabbitmq_management rabbitmq_prometheus rabbitmq_delayed_message_exchange\n"
                    + "              if command -v docker-entrypoint.sh >/dev/null 2>&1; then exec docker-entrypoint.sh rabbitmq-server; fi\n"
                    + "              exec rabbitmq-server\n";
        }
        return "";
    }

    private String buildK8sDeploymentEnv(String name) {
        if ("mysql".equals(name)) {
            return k8sEnv(Map.of("MYSQL_ROOT_PASSWORD", "123456"));
        }
        if ("rabbitmq".equals(name)) {
            return k8sEnv(new LinkedHashMap<>(Map.of(
                    "RABBITMQ_DEFAULT_USER", "tjxt",
                    "RABBITMQ_DEFAULT_PASS", "123321",
                    "RABBITMQ_DEFAULT_VHOST", "/tjxt"
            )));
        }
        if ("minio".equals(name)) {
            return k8sEnv(new LinkedHashMap<>(Map.of(
                    "MINIO_ROOT_USER", REMOTE_MINIO_ACCESS_KEY,
                    "MINIO_ROOT_PASSWORD", REMOTE_MINIO_SECRET_KEY
            )));
        }
        if ("nacos".equals(name)) {
            Map<String, String> env = new LinkedHashMap<>();
            env.put("MODE", "standalone");
            env.put("SPRING_DATASOURCE_PLATFORM", "mysql");
            env.put("MYSQL_SERVICE_HOST", "mysql");
            env.put("MYSQL_SERVICE_PORT", "3306");
            env.put("MYSQL_SERVICE_DB_NAME", "nacos");
            env.put("MYSQL_DATABASE_NUM", "1");
            env.put("MYSQL_SERVICE_USER", "root");
            env.put("MYSQL_SERVICE_PASSWORD", "123456");
            env.put("MYSQL_SERVICE_DB_PARAM", "characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true&useUnicode=true&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true");
            env.put("NACOS_AUTH_ENABLE", "false");
            env.put("JVM_XMS", "256m");
            env.put("JVM_XMX", "512m");
            env.put("JVM_XMN", "128m");
            return k8sEnv(env);
        }
        if ("seata".equals(name)) {
            return k8sEnv(new LinkedHashMap<>(Map.of(
                    "SEATA_IP", "seata",
                    "SEATA_PORT", "8099"
            )));
        }
        if ("xxl-job".equals(name)) {
            Map<String, String> env = new LinkedHashMap<>();
            env.put("SERVER_PORT", "8880");
            env.put("SERVER_SERVLET_CONTEXT_PATH", "/xxl-job-admin");
            env.put("SPRING_DATASOURCE_URL", "jdbc:mysql://mysql:3306/xxl_job?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true");
            env.put("SPRING_DATASOURCE_USERNAME", "root");
            env.put("SPRING_DATASOURCE_PASSWORD", "123456");
            env.put("SPRING_DATASOURCE_DRIVER_CLASS_NAME", "com.mysql.cj.jdbc.Driver");
            env.put("SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE", "5");
            env.put("SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE", "1");
            env.put("XXL_JOB_ACCESS_TOKEN", "tianji");
            return k8sEnv(env);
        }
        if ("elasticsearch".equals(name)) {
            return k8sEnv(new LinkedHashMap<>(Map.of(
                    "discovery.type", "single-node",
                    "ES_JAVA_OPTS", "-Xms512m -Xmx512m"
            )));
        }
        if ("rk-gogs".equals(name)) {
            Map<String, String> env = new LinkedHashMap<>();
            env.put("GOGS_CUSTOM", "/data/gogs");
            return k8sEnv(env);
        }
        if ("rk-jenkins".equals(name)) {
            Map<String, String> env = new LinkedHashMap<>();
            env.put("JENKINS_HOME", "/var/jenkins_home");
            env.put("JAVA_OPTS", "-Djenkins.install.runSetupWizard=false -Xms128m -Xmx384m -XX:MaxMetaspaceSize=192m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError");
            return k8sEnv(env);
        }
        if ("rk-web-frontend".equals(name)) {
            return k8sEnv(new LinkedHashMap<>(Map.of(
                    "RK_GATEWAY_URL", "http://rk-gateway:10010",
                    "RK_MINIO_URL", "http://minio:9000"
            )));
        }
        if (name != null && name.startsWith("rk-")) {
            Map<String, String> env = new LinkedHashMap<>();
            env.put("SPRING_CLOUD_NACOS_SERVER_ADDR", "nacos:8848");
            env.put("SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR", "nacos:8848");
            env.put("SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR", "nacos:8848");
            env.put("SPRING_CLOUD_NACOS_USERNAME", "nacos");
            env.put("SPRING_CLOUD_NACOS_PASSWORD", "nacos");
            env.put("DB_HOST", "mysql");
            env.put("DB_USER", "root");
            env.put("DB_PWD", "123456");
            env.put("SPRING_DATASOURCE_USERNAME", "root");
            env.put("SPRING_DATASOURCE_PASSWORD", "123456");
            env.put("SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE", "5");
            env.put("SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE", "1");
            env.put("REDIS_HOST", "redis");
            env.put("SPRING_REDIS_HOST", "redis");
            env.put("SPRING_REDIS_PASSWORD", "123321");
            env.put("RABBIT_HOST", "rabbitmq");
            env.put("RABBIT_USER", "tjxt");
            env.put("RABBIT_PWD", "123321");
            env.put("RABBIT_VHOST", "/tjxt");
            env.put("SPRING_RABBITMQ_HOST", "rabbitmq");
            env.put("SPRING_RABBITMQ_USERNAME", "tjxt");
            env.put("SPRING_RABBITMQ_PASSWORD", "123321");
            env.put("SPRING_RABBITMQ_VIRTUAL_HOST", "/tjxt");
            env.put("MINIO_ENDPOINT", "http://minio:9000");
            env.put("MINIO_ACCESS_KEY", REMOTE_MINIO_ACCESS_KEY);
            env.put("MINIO_SECRET_KEY", REMOTE_MINIO_SECRET_KEY);
            env.put("MINIO_PUBLIC_BASE_URL", "/minio-files/");
            env.put("XXL_JOB_ADMIN_ADDRESSES", "http://xxl-job:8880/xxl-job-admin");
            env.put("JAVA_TOOL_OPTIONS", "-Xms64m -Xmx256m -XX:MaxMetaspaceSize=128m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError");
            return k8sEnv(env);
        }
        return "";
    }

    private String buildK8sDeploymentResources(String name) {
        if ("rk-jenkins".equals(name)) {
            return "          resources:\n"
                    + "            requests:\n"
                    + "              cpu: 100m\n"
                    + "              memory: 256Mi\n"
                    + "            limits:\n"
                    + "              cpu: 1000m\n"
                    + "              memory: 1024Mi\n";
        }
        if ("rk-user".equals(name) || "rk-auth".equals(name) || "rk-gateway".equals(name)) {
            return "          resources:\n"
                    + "            requests:\n"
                    + "              cpu: 100m\n"
                    + "              memory: 128Mi\n"
                    + "            limits:\n"
                    + "              cpu: 1000m\n"
                    + "              memory: 768Mi\n";
        }
        if (name != null && name.startsWith("rk-")) {
            return "          resources:\n"
                    + "            requests:\n"
                    + "              cpu: 100m\n"
                    + "              memory: 128Mi\n"
                    + "            limits:\n"
                    + "              cpu: 1000m\n"
                    + "              memory: 512Mi\n";
        }
        if ("nacos".equals(name) || "elasticsearch".equals(name)) {
            return "          resources:\n"
                    + "            requests:\n"
                    + "              cpu: 100m\n"
                    + "              memory: 384Mi\n"
                    + "            limits:\n"
                    + "              cpu: 1000m\n"
                    + "              memory: 768Mi\n";
        }
        if ("mysql".equals(name) || "rabbitmq".equals(name) || "minio".equals(name) || "redis".equals(name)) {
            return "          resources:\n"
                    + "            requests:\n"
                    + "              cpu: 100m\n"
                    + "              memory: 128Mi\n"
                    + "            limits:\n"
                    + "              cpu: 1000m\n"
                    + "              memory: 512Mi\n";
        }
        return "          resources:\n"
                + "            requests:\n"
                + "              cpu: 50m\n"
                + "              memory: 128Mi\n"
                + "            limits:\n"
                + "              cpu: 500m\n"
                + "              memory: 512Mi\n";
    }

    private String buildK8sVolumeMounts(String name) {
        boolean persistent = K8S_PERSISTENT_MIDDLEWARE.contains(name);
        if (!persistent) {
            return "";
        }
        StringBuilder builder = new StringBuilder("          volumeMounts:\n");
        String mountPath = k8sPersistentDataMountPath(name);
        if (StringUtils.hasText(mountPath)) {
            builder.append("            - name: data\n")
                    .append("              mountPath: ").append(mountPath).append("\n");
        }
        if ("nacos".equals(name)) {
            builder.append("            - name: nacos-mysql-plugin\n")
                    .append("              mountPath: /home/nacos/plugins/mysql\n");
        }
        if ("rabbitmq".equals(name)) {
            builder.append("            - name: rabbitmq-delayed-plugin\n")
                    .append("              mountPath: /rk-rabbitmq-plugins\n");
        }
        return builder.toString();
    }

    private String buildK8sVolumes(String name) {
        if (!"nacos".equals(name) && !"rabbitmq".equals(name)) {
            return "";
        }
        if ("nacos".equals(name)) {
            return "      volumes:\n"
                    + "        - name: nacos-mysql-plugin\n"
                    + "          emptyDir: {}\n";
        }
        return "      volumes:\n"
                + "        - name: rabbitmq-delayed-plugin\n"
                + "          emptyDir: {}\n";
    }

    private String buildK8sVolumeClaimTemplate(String name, AdminDeployPackageCreateDTO request) {
        if (!K8S_PERSISTENT_MIDDLEWARE.contains(name)) {
            return "";
        }
        String storageClassName = resolveDeployTargetDefaults(request).storageClassName;
        String storageClass = StringUtils.hasText(storageClassName)
                ? "        storageClassName: " + storageClassName + "\n"
                : "";
        return "  volumeClaimTemplates:\n"
                + "    - metadata:\n"
                + "        name: data\n"
                + "      spec:\n"
                + "        accessModes:\n"
                + "          - ReadWriteOnce\n"
                + storageClass
                + "        resources:\n"
                + "          requests:\n"
                + "            storage: " + k8sPersistentStorageRequest(name) + "\n";
    }

    private String buildExternalExposureManifest(AdminDeployPackageCreateDTO request, String namespace) {
        DeployTargetDefaults defaults = resolveDeployTargetDefaults(request);
        if (EXPOSURE_NONE.equals(defaults.externalExposureType)) {
            return "";
        }
        if (EXPOSURE_INGRESS.equals(defaults.externalExposureType)) {
            return "---\n" + buildIngressManifest(namespace, request);
        }
        return "---\n"
                + buildPublicService(namespace, "rk-web-frontend-public", "rk-web-frontend", 80, 80, request)
                + "---\n"
                + buildPublicService(namespace, "rk-gateway-public", "rk-gateway", 10010, 10010, request);
    }

    private String buildPublicService(String namespace, String name, String selectorName, int port, int targetPort, AdminDeployPackageCreateDTO request) {
        DeployTargetDefaults defaults = resolveDeployTargetDefaults(request);
        String serviceTypeLine = EXPOSURE_LOAD_BALANCER.equals(defaults.externalExposureType)
                ? "  type: LoadBalancer\n"
                : "  type: NodePort\n";
        Integer nodePort = null;
        if (EXPOSURE_NODE_PORT.equals(defaults.externalExposureType)) {
            nodePort = "rk-web-frontend-public".equals(name) ? defaults.frontendNodePort : defaults.gatewayNodePort;
        }
        return "apiVersion: v1\n"
                + "kind: Service\n"
                + "metadata:\n"
                + "  name: " + name + "\n"
                + "  namespace: " + namespace + "\n"
                + "spec:\n"
                + serviceTypeLine
                + "  selector:\n"
                + "    app: " + selectorName + "\n"
                + "  ports:\n"
                + "    - name: port-" + port + "\n"
                + "      port: " + port + "\n"
                + "      targetPort: " + targetPort + "\n"
                + (nodePort == null ? "" : "      nodePort: " + nodePort + "\n");
    }

    private String buildIngressManifest(String namespace, AdminDeployPackageCreateDTO request) {
        DeployTargetDefaults defaults = resolveDeployTargetDefaults(request);
        String host = trimToDefault(defaults.ingressHost, resolveRemoteDeployDomain(request));
        String ingressClass = StringUtils.hasText(defaults.ingressClassName)
                ? "  ingressClassName: " + defaults.ingressClassName + "\n"
                : "";
        return "apiVersion: networking.k8s.io/v1\n"
                + "kind: Ingress\n"
                + "metadata:\n"
                + "  name: rk-web-public\n"
                + "  namespace: " + namespace + "\n"
                + "spec:\n"
                + ingressClass
                + "  rules:\n"
                + "    - host: " + host + "\n"
                + "      http:\n"
                + "        paths:\n"
                + "          - path: /api\n"
                + "            pathType: Prefix\n"
                + "            backend:\n"
                + "              service:\n"
                + "                name: rk-gateway\n"
                + "                port:\n"
                + "                  number: 10010\n"
                + "          - path: /auth\n"
                + "            pathType: Prefix\n"
                + "            backend:\n"
                + "              service:\n"
                + "                name: rk-gateway\n"
                + "                port:\n"
                + "                  number: 10010\n"
                + "          - path: /admin\n"
                + "            pathType: Prefix\n"
                + "            backend:\n"
                + "              service:\n"
                + "                name: rk-gateway\n"
                + "                port:\n"
                + "                  number: 10010\n"
                + "          - path: /minio-files\n"
                + "            pathType: Prefix\n"
                + "            backend:\n"
                + "              service:\n"
                + "                name: rk-gateway\n"
                + "                port:\n"
                + "                  number: 10010\n"
                + "          - path: /\n"
                + "            pathType: Prefix\n"
                + "            backend:\n"
                + "              service:\n"
                + "                name: rk-web-frontend\n"
                + "                port:\n"
                + "                  number: 80\n";
    }

    private String k8sPersistentDataMountPath(String name) {
        if ("mysql".equals(name)) {
            return "/var/lib/mysql";
        }
        if ("redis".equals(name) || "minio".equals(name)) {
            return "/data";
        }
        if ("rabbitmq".equals(name)) {
            return "/var/lib/rabbitmq";
        }
        if ("nacos".equals(name)) {
            return "/home/nacos/data";
        }
        if ("elasticsearch".equals(name)) {
            return "/usr/share/elasticsearch/data";
        }
        if ("rk-gogs".equals(name)) {
            return "/data";
        }
        if ("rk-jenkins".equals(name)) {
            return "/var/jenkins_home";
        }
        return "";
    }

    private String k8sPersistentStorageRequest(String name) {
        if ("minio".equals(name)) {
            return "50Gi";
        }
        if ("rk-gogs".equals(name) || "rk-jenkins".equals(name)) {
            return "20Gi";
        }
        if ("mysql".equals(name) || "elasticsearch".equals(name)) {
            return "20Gi";
        }
        return "20Gi";
    }

    private String buildRabbitMqTopologyJob(String namespace, String rabbitmqImage) {
        String image = trimToDefault(rabbitmqImage, REMOTE_RABBITMQ_IMAGE);
        return "apiVersion: batch/v1\n"
                + "kind: Job\n"
                + "metadata:\n"
                + "  name: rk-rabbitmq-topology\n"
                + "  namespace: " + namespace + "\n"
                + "spec:\n"
                + "  backoffLimit: 6\n"
                + "  template:\n"
                + "    spec:\n"
                + "      restartPolicy: OnFailure\n"
                + "      containers:\n"
                + "        - name: rabbitmqadmin\n"
                + "          image: " + image + "\n"
                + "          imagePullPolicy: IfNotPresent\n"
                + "          command:\n"
                + "            - sh\n"
                + "            - -c\n"
                + "            - |\n"
                + "              set -e\n"
                + "              until rabbitmqadmin -H rabbitmq -P 15672 -u tjxt -p 123321 -V /tjxt list vhosts >/dev/null 2>&1; do sleep 5; done\n"
                + "              rabbitmqadmin -H rabbitmq -P 15672 -u tjxt -p 123321 -V /tjxt declare exchange name=pay.topic type=topic durable=true\n"
                + "              rabbitmq-plugins enable --offline rabbitmq_delayed_message_exchange >/dev/null 2>&1 || true\n"
                + "              rabbitmqadmin -H rabbitmq -P 15672 -u tjxt -p 123321 -V /tjxt declare exchange name=trade.delay.topic type=x-delayed-message durable=true " + RABBITMQ_DELAYED_EXCHANGE_ARGUMENTS + "\n"
                + "              rabbitmqadmin -H rabbitmq -P 15672 -u tjxt -p 123321 -V /tjxt declare queue name=trade.pay.success.queue durable=true\n"
                + "              rabbitmqadmin -H rabbitmq -P 15672 -u tjxt -p 123321 -V /tjxt declare queue name=trade.refund.result.queue durable=true\n"
                + "              rabbitmqadmin -H rabbitmq -P 15672 -u tjxt -p 123321 -V /tjxt declare queue name=trade.delay.order.query durable=true\n"
                + "              rabbitmqadmin -H rabbitmq -P 15672 -u tjxt -p 123321 -V /tjxt declare binding source=pay.topic destination_type=queue destination=trade.pay.success.queue routing_key=pay.success\n"
                + "              rabbitmqadmin -H rabbitmq -P 15672 -u tjxt -p 123321 -V /tjxt declare binding source=pay.topic destination_type=queue destination=trade.refund.result.queue routing_key=refund.status.change\n"
                + "              rabbitmqadmin -H rabbitmq -P 15672 -u tjxt -p 123321 -V /tjxt declare binding source=trade.delay.topic destination_type=queue destination=trade.delay.order.query routing_key=delay.order.query\n"
                + "              echo rk rabbitmq topology ready\n";
    }

    private String buildSentinelDashboardDeployment(String namespace) {
        return "apiVersion: apps/v1\n"
                + "kind: Deployment\n"
                + "metadata:\n"
                + "  name: sentinel\n"
                + "  namespace: " + namespace + "\n"
                + "spec:\n"
                + "  replicas: 1\n"
                + "  selector:\n"
                + "    matchLabels:\n"
                + "      app: sentinel\n"
                + "  template:\n"
                + "    metadata:\n"
                + "      labels:\n"
                + "        app: sentinel\n"
                + "    spec:\n"
                + "      imagePullSecrets:\n"
                + "        - name: " + trimToDefault(deployPackageRegistryImagePullSecretName, "rk-aliyun-regcred") + "\n"
                + "      containers:\n"
                + "        - name: sentinel\n"
                + "          image: " + DEFAULT_SENTINEL_DASHBOARD_IMAGE + "\n"
                + "          imagePullPolicy: IfNotPresent\n"
                + "          ports:\n"
                + "            - containerPort: 8858\n"
                + "          readinessProbe:\n"
                + "            httpGet:\n"
                + "              path: /\n"
                + "              port: 8858\n"
                + "            initialDelaySeconds: 20\n"
                + "            periodSeconds: 10\n"
                + "            timeoutSeconds: 3\n"
                + "            failureThreshold: 12\n"
                + "          livenessProbe:\n"
                + "            httpGet:\n"
                + "              path: /\n"
                + "              port: 8858\n"
                + "            initialDelaySeconds: 60\n"
                + "            periodSeconds: 20\n"
                + "            timeoutSeconds: 3\n"
                + "            failureThreshold: 6\n"
                + "          startupProbe:\n"
                + "            httpGet:\n"
                + "              path: /\n"
                + "              port: 8858\n"
                + "            periodSeconds: 10\n"
                + "            timeoutSeconds: 3\n"
                + "            failureThreshold: 30\n"
                + "          resources:\n"
                + "            requests:\n"
                + "              cpu: 50m\n"
                + "              memory: 256Mi\n"
                + "            limits:\n"
                + "              cpu: 500m\n"
                + "              memory: 512Mi\n";
    }

    private String k8sEnv(Map<String, String> env) {
        if (env == null || env.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder("          env:\n");
        env.forEach((key, value) -> builder
                .append("            - name: ").append(key).append("\n")
                .append("              value: \"").append(escapeYamlString(value)).append("\"\n"));
        return builder.toString();
    }

    private String escapeYamlString(String value) {
        return (value == null ? "" : value).replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String buildSimpleService(String namespace, String name, int port, int targetPort) {
        return buildService(namespace, name, name, port, targetPort);
    }

    private String buildAliasService(String namespace, String aliasName, String targetName, int port, int targetPort) {
        return buildService(namespace, aliasName, targetName, port, targetPort);
    }

    private String buildService(String namespace, String name, String selectorName, int port, int targetPort) {
        return "apiVersion: v1\n"
                + "kind: Service\n"
                + "metadata:\n"
                + "  name: " + name + "\n"
                + "  namespace: " + namespace + "\n"
                + "spec:\n"
                + "  selector:\n"
                + "    app: " + selectorName + "\n"
                + "  ports:\n"
                + "    - name: port-" + port + "\n"
                + "      port: " + port + "\n"
                + "      targetPort: " + targetPort + "\n"
                + ("rabbitmq".equals(selectorName)
                ? "    - name: management\n"
                + "      port: 15672\n"
                + "      targetPort: 15672\n"
                : "");
    }

    private String buildDeployPackageReadme() {
        return "# RK-Web deploy package\n\n"
                + "This package contains manifest.json, docker-compose.yml, k8s/rk-web-stack.yaml, install scripts, and artifact directories for images, databases, and MinIO objects.\n"
                + "For a self-contained offline bundle, run scripts/package-online.sh on the source k3s/k8s node before moving the directory to the target server.\n"
                + "Use scripts/install-compose.sh for Docker Compose, or scripts/install-k8s.sh/install.sh for k3s/k8s. Registry mode can push images through scripts/push-images-to-registry.sh.\n";
    }

    private List<AdminDeployPackageRecordVO> loadDeployPackageRecords() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                buildDeployPackageRecordSelectSql("WHERE is_deleted = 0 AND COALESCE(remote_deploy, 0) = 0 ORDER BY create_time DESC LIMIT 50")
        );
        return rows.stream().map(this::toDeployPackageVO).collect(Collectors.toList());
    }

    private List<AdminDeployPackageRecordVO> loadRemoteMigrationRecords() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                buildDeployPackageRecordSelectSql("WHERE is_deleted = 0 AND COALESCE(remote_deploy, 0) = 1 ORDER BY create_time DESC LIMIT 50")
        );
        return rows.stream().map(this::toDeployPackageVO).collect(Collectors.toList());
    }

    private AdminDeployPackageRecordVO loadDeployPackageRecord(Long packageId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                buildDeployPackageRecordSelectSql("WHERE id = ? AND is_deleted = 0"),
                packageId
        );
        return rows.isEmpty() ? null : toDeployPackageVO(rows.get(0));
    }

    private Map<String, Object> loadDeployPackageRecordRow(Long packageId) {
        if (packageId == null) {
            throw new BadRequestException("部署记录 ID 不能为空");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM ops_deploy_package_record WHERE id = ? AND is_deleted = 0",
                packageId
        );
        if (rows.isEmpty()) {
            throw new BadRequestException("部署记录不存在");
        }
        return rows.get(0);
    }

    private Map<String, Object> requireRemoteMigrationRecordRow(Long migrationId) {
        Map<String, Object> row = loadDeployPackageRecordRow(migrationId);
        if (valueAsInteger(row.get("remote_deploy")) != 1) {
            throw new BadRequestException("该记录不是远程迁移任务");
        }
        return row;
    }

    private String resolveStoredTargetNamespace(Map<String, Object> row) {
        JsonNode root = readJsonNode(row == null ? null : row.get("request_json"));
        return trimToDefault(jsonText(root, "targetNamespace", ""), "shetuanguanlixitong");
    }

    private String buildDeployPackageRecordSelectSql(String suffix) {
        return "SELECT id, package_name, status, progress, package_mode, delivery_mode, image_artifact_mode, deploy_mode, remote_deploy, remote_cluster_name, deploy_started_at, deploy_finished_at, current_step, current_image, " +
                "uploaded_images, total_images, upload_percent, registry_pull_package, streaming_migration, global_migration_lock, estimated_remaining_seconds, migration_started_at, migration_updated_at, file_name, remote_path, file_size_bytes, " +
                "delete_after_download, downloaded, downloaded_at, note_text, logs, create_time, update_time " +
                "FROM ops_deploy_package_record " + suffix;
    }

    private AdminDeployPackageRecordVO toDeployPackageVO(Map<String, Object> row) {
        AdminDeployPackageRecordVO vo = new AdminDeployPackageRecordVO();
        vo.setId(valueAsLong(row.get("id")));
        vo.setPackageName(valueAsString(row.get("package_name")));
        vo.setStatus(valueAsString(row.get("status")));
        vo.setProgress(valueAsInteger(row.get("progress")));
        vo.setPackageMode(valueAsString(row.get("package_mode")));
        vo.setDeliveryMode(valueAsString(row.get("delivery_mode")));
        vo.setImageArtifactMode(valueAsString(row.get("image_artifact_mode")));
        vo.setDeployMode(valueAsString(row.get("deploy_mode")));
        vo.setRemoteDeploy(valueAsInteger(row.get("remote_deploy")) == 1);
        vo.setRemoteClusterName(valueAsString(row.get("remote_cluster_name")));
        vo.setDeployStartedAt(formatDateTime(row.get("deploy_started_at")));
        vo.setDeployStartedAtMillis(toEpochMillis(row.get("deploy_started_at")));
        vo.setDeployFinishedAt(formatDateTime(row.get("deploy_finished_at")));
        vo.setDeployFinishedAtMillis(toEpochMillis(row.get("deploy_finished_at")));
        vo.setCurrentStep(valueAsString(row.get("current_step")));
        vo.setCurrentImage(valueAsString(row.get("current_image")));
        vo.setUploadedImages(valueAsInteger(row.get("uploaded_images")));
        vo.setTotalImages(valueAsInteger(row.get("total_images")));
        vo.setUploadPercent(valueAsInteger(row.get("upload_percent")));
        vo.setRegistryPullPackage(valueAsInteger(row.get("registry_pull_package")) == 1);
        vo.setStreamingMigration(valueAsInteger(row.get("streaming_migration")) == 1);
        vo.setGlobalMigrationLock(valueAsInteger(row.get("global_migration_lock")) == 1);
        vo.setEstimatedRemainingSeconds(valueAsInteger(row.get("estimated_remaining_seconds")));
        vo.setMigrationStartedAt(formatDateTime(row.get("migration_started_at")));
        vo.setMigrationStartedAtMillis(toEpochMillis(row.get("migration_started_at")));
        vo.setMigrationUpdatedAt(formatDateTime(row.get("migration_updated_at")));
        vo.setMigrationUpdatedAtMillis(toEpochMillis(row.get("migration_updated_at")));
        vo.setFileName(valueAsString(row.get("file_name")));
        vo.setRemotePath(valueAsString(row.get("remote_path")));
        vo.setFileSize(formatBytes(valueAsLong(row.get("file_size_bytes"))));
        vo.setDeleteAfterDownload(valueAsInteger(row.get("delete_after_download")) == 1);
        vo.setDownloaded(valueAsInteger(row.get("downloaded")) == 1);
        vo.setDownloadedAt(formatDateTime(row.get("downloaded_at")));
        vo.setNote(valueAsString(row.get("note_text")));
        vo.setLogs(valueAsString(row.get("logs")));
        vo.setCreateTime(formatDateTime(row.get("create_time")));
        vo.setUpdateTime(formatDateTime(row.get("update_time")));
        vo.setDownloadable(StringUtils.hasText(vo.getRemotePath()) && backupStorageService.exists(vo.getRemotePath()));
        return vo;
    }

    private AdminDeployPackageRecordVO sanitizePublicMigrationRecord(AdminDeployPackageRecordVO source) {
        if (source == null) {
            return null;
        }
        AdminDeployPackageRecordVO vo = new AdminDeployPackageRecordVO();
        vo.setId(source.getId());
        vo.setPackageName(source.getPackageName());
        vo.setStatus(source.getStatus());
        vo.setProgress(source.getProgress());
        vo.setDeployMode(source.getDeployMode());
        vo.setRemoteDeploy(source.getRemoteDeploy());
        vo.setRemoteClusterName(source.getRemoteClusterName());
        vo.setDeployStartedAt(source.getDeployStartedAt());
        vo.setDeployStartedAtMillis(source.getDeployStartedAtMillis());
        vo.setCurrentStep(source.getCurrentStep());
        vo.setCurrentImage(source.getCurrentImage());
        vo.setUploadedImages(source.getUploadedImages());
        vo.setTotalImages(source.getTotalImages());
        vo.setUploadPercent(source.getUploadPercent());
        vo.setStreamingMigration(source.getStreamingMigration());
        vo.setGlobalMigrationLock(source.getGlobalMigrationLock());
        vo.setEstimatedRemainingSeconds(source.getEstimatedRemainingSeconds());
        vo.setMigrationStartedAt(source.getMigrationStartedAt());
        vo.setMigrationStartedAtMillis(source.getMigrationStartedAtMillis());
        vo.setMigrationUpdatedAt(source.getMigrationUpdatedAt());
        vo.setMigrationUpdatedAtMillis(source.getMigrationUpdatedAtMillis());
        vo.setCreateTime(source.getCreateTime());
        vo.setUpdateTime(source.getUpdateTime());
        return vo;
    }

    private List<Map<String, Object>> queryDatabaseSizeRows(List<String> databases) {
        String placeholders = databases.stream().map(item -> "?").collect(Collectors.joining(","));
        return jdbcTemplate.queryForList(
                "SELECT table_schema AS databaseName, COALESCE(SUM(data_length + index_length), 0) AS totalBytes " +
                        "FROM information_schema.tables WHERE table_schema IN (" + placeholders + ") GROUP BY table_schema ORDER BY table_schema",
                databases.toArray()
        );
    }

    private List<String> normalizeDatabases(List<String> databases) {
        if (databases == null || databases.isEmpty()) {
            return new ArrayList<>(DEFAULT_BACKUP_DATABASES);
        }
        return databases.stream()
                .filter(StringUtils::hasText)
                .filter(DEFAULT_BACKUP_DATABASES::contains)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> normalizeDeployPackageDatabases(List<String> databases) {
        if (databases == null || databases.isEmpty()) {
            return new ArrayList<>(DEFAULT_DEPLOY_PACKAGE_DATABASES);
        }
        return databases.stream()
                .filter(StringUtils::hasText)
                .filter(DEFAULT_DEPLOY_PACKAGE_DATABASES::contains)
                .distinct()
                .collect(Collectors.toList());
    }

    private String runRemoteCommand(String remoteCommand) {
        ProcessBuilder builder = new ProcessBuilder(remotePython, remoteSshScript, remoteCommand);
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("远程命令执行超时");
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (InputStream inputStream = process.getInputStream()) {
                inputStream.transferTo(output);
            }
            String text = output.toString(StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw new IllegalStateException(text.trim());
            }
            return text.trim();
        } catch (Exception e) {
            throw new IllegalStateException("远程命令执行失败: " + e.getMessage(), e);
        }
    }

    private String runRemoteCommandLenient(String remoteCommand, int timeoutSeconds) {
        try {
            return runRemoteCommand("bash -lc " + quoteForBash(remoteCommand));
        } catch (Exception remoteError) {
            log.warn("remote command failed, trying local shell fallback", remoteError);
            return runLocalShellCommand(remoteCommand, timeoutSeconds, remoteError);
        }
    }

    private String runLocalShellCommand(String command, int timeoutSeconds, Exception remoteError) {
        String shell = Files.isExecutable(Path.of("/bin/bash")) ? "/bin/bash" : "/bin/sh";
        ProcessBuilder builder = new ProcessBuilder(shell, "-lc", command);
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            int timeout = Math.max(5, timeoutSeconds);
            if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("local shell command timed out after " + timeout + "s");
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (InputStream inputStream = process.getInputStream()) {
                inputStream.transferTo(output);
            }
            String text = output.toString(StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0) {
                throw new IllegalStateException(StringUtils.hasText(text) ? text : "local shell command failed");
            }
            return text;
        } catch (Exception localError) {
            throw new IllegalStateException("remote command unavailable: " + remoteError.getMessage()
                    + "; local fallback unavailable: " + localError.getMessage(), localError);
        }
    }

    private List<AdminContainerVO> parseDockerLines(String output) {
        if (!StringUtils.hasText(output)) {
            return Collections.emptyList();
        }
        List<AdminContainerVO> result = new ArrayList<>();
        for (String line : output.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (!StringUtils.hasText(trimmed) || trimmed.startsWith("NAMES")) {
                continue;
            }
            String[] parts = trimmed.split("\\t");
            if (parts.length < 3) {
                continue;
            }
            AdminContainerVO item = new AdminContainerVO();
            item.setName(parts[0]);
            item.setImage(parts[1]);
            item.setPorts(parts[2]);
            result.add(item);
        }
        return result;
    }

    private List<AdminK8sNodeVO> parseNodeLines(String output) {
        if (!StringUtils.hasText(output)) {
            return Collections.emptyList();
        }
        List<AdminK8sNodeVO> result = new ArrayList<>();
        for (String line : output.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (!StringUtils.hasText(trimmed)) {
                continue;
            }
            String[] parts = trimmed.split("\\s+");
            if (parts.length < 6) {
                continue;
            }
            AdminK8sNodeVO item = new AdminK8sNodeVO();
            item.setName(parts[0]);
            item.setStatus(parts[1]);
            item.setRoles(parts[2]);
            item.setVersion(parts[4]);
            item.setInternalIp(parts[5]);
            result.add(item);
        }
        return result;
    }

    private List<String> parseNamespaces(String output) {
        if (!StringUtils.hasText(output)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String line : output.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (!StringUtils.hasText(trimmed)) {
                continue;
            }
            String[] parts = trimmed.split("\\s+");
            if (parts.length > 0) {
                result.add(parts[0]);
            }
        }
        return result;
    }

    private String formatK8sNodeRoles(JsonNode labels) {
        List<String> roles = new ArrayList<>();
        labels.fieldNames().forEachRemaining(name -> {
            String prefix = "node-role.kubernetes.io/";
            if (name.startsWith(prefix)) {
                roles.add(name.substring(prefix.length()));
            }
        });
        return roles.isEmpty() ? "worker" : String.join(",", roles);
    }

    private String formatK8sNodeStatus(JsonNode conditions) {
        for (JsonNode condition : conditions) {
            if (Objects.equals(condition.path("type").asText(), "Ready")) {
                return Objects.equals(condition.path("status").asText(), "True") ? "Ready" : "NotReady";
            }
        }
        return "Unknown";
    }

    private String formatK8sInternalIp(JsonNode addresses) {
        for (JsonNode address : addresses) {
            if (Objects.equals(address.path("type").asText(), "InternalIP")) {
                return address.path("address").asText("-");
            }
        }
        return "-";
    }

    private String formatK8sContainerPorts(JsonNode ports) {
        List<String> result = new ArrayList<>();
        for (JsonNode port : ports) {
            String protocol = port.path("protocol").asText("TCP");
            int containerPort = port.path("containerPort").asInt(0);
            if (containerPort > 0) {
                result.add(containerPort + "/" + protocol);
            }
        }
        return result.isEmpty() ? "-" : String.join(", ", result);
    }

    private String formatEpochMillis(long epochMillis) {
        if (epochMillis <= 0) {
            return "-";
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault()).format(TIME_FORMATTER);
    }

    private String formatDateTime(Object value) {
        if (value == null) {
            return "-";
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).format(TIME_FORMATTER);
        }
        return value.toString().replace("T", " ");
    }

    private Long toEpochMillis(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        if (value instanceof java.util.Date) {
            return ((java.util.Date) value).getTime();
        }
        String text = value.toString().trim();
        if (!StringUtils.hasText(text) || "-".equals(text)) {
            return null;
        }
        try {
            long numeric = Long.parseLong(text);
            return numeric > 100000000000L ? numeric : numeric * 1000L;
        } catch (Exception ignored) {
            // Try formatted timestamps below.
        }
        try {
            return LocalDateTime.parse(text.replace(" ", "T"))
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0) {
            return "0 B";
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        if (bytes < 1024L * 1024L * 1024L) {
            return String.format("%.1f MB", bytes / 1024.0 / 1024.0);
        }
        return String.format("%.1f GB", bytes / 1024.0 / 1024.0 / 1024.0);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("JSON 序列化失败", e);
        }
    }

    private List<String> readStringList(Object value) {
        if (value == null) {
            return new ArrayList<>(DEFAULT_BACKUP_DATABASES);
        }
        try {
            JsonNode node = objectMapper.readTree(value.toString());
            List<String> result = new ArrayList<>();
            for (JsonNode item : node) {
                result.add(item.asText());
            }
            return result;
        } catch (Exception e) {
            return new ArrayList<>(DEFAULT_BACKUP_DATABASES);
        }
    }

    private Map<String, List<String>> readTableSummary(Object value) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (value == null || !StringUtils.hasText(value.toString())) {
            return result;
        }
        try {
            JsonNode root = objectMapper.readTree(value.toString());
            JsonNode tablesNode = root.path("tables");
            if (!tablesNode.isObject()) {
                return result;
            }
            tablesNode.fields().forEachRemaining(entry -> {
                List<String> tables = new ArrayList<>();
                if (entry.getValue().isArray()) {
                    for (JsonNode table : entry.getValue()) {
                        tables.add(table.asText());
                    }
                }
                result.put(entry.getKey(), tables);
            });
        } catch (Exception e) {
            log.warn("parse backup table summary failed", e);
        }
        return result;
    }

    private int countTables(Map<String, List<String>> tableSummary) {
        return tableSummary.values().stream()
                .mapToInt(tables -> tables == null ? 0 : tables.size())
                .sum();
    }

    private String extractYamlValue(String output, String key) {
        for (String line : output.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(key + ":")) {
                return trimmed.substring((key + ":").length()).trim().replace("\"", "");
            }
        }
        return "-";
    }

    private String firstMeaningfulLine(String output) {
        return output.lines()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(line -> !Objects.equals(line, "To further debug and diagnose cluster problems, use 'kubectl cluster-info dump'."))
                .findFirst()
                .orElse("-");
    }

    private String quoteForBash(String script) {
        return "'" + script.replace("'", "'\"'\"'") + "'";
    }

    private String sanitizeOpsUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String buildBasicAuthHeader(String username, String password) {
        String raw = (username == null ? "" : username) + ":" + (password == null ? "" : password);
        return "Basic " + java.util.Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private long parseLastLongLine(String output) {
        String[] lines = output.split("\\r?\\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String trimmed = lines[i].trim();
            if (trimmed.matches("\\d+")) {
                return Long.parseLong(trimmed);
            }
        }
        return 0L;
    }

    private String valueAsString(Object value) {
        return value == null ? null : value.toString();
    }

    private long valueAsLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private Integer valueAsInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof Boolean) {
            return Boolean.TRUE.equals(value) ? 1 : 0;
        }
        if (value instanceof String && StringUtils.hasText((String) value)) {
            try {
                return Integer.parseInt(((String) value).trim());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private Integer valueAsNullableInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof Boolean) {
            return Boolean.TRUE.equals(value) ? 1 : 0;
        }
        if (value instanceof String && StringUtils.hasText((String) value)) {
            try {
                return Integer.parseInt(((String) value).trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private long measureTcpConnectMillis(String host, int port) {
        long start = System.nanoTime();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1000);
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            return Math.max(1L, elapsed);
        } catch (Exception e) {
            return -1L;
        }
    }

    private String loadNetworkMetric() {
        try {
            Path procNetDev = Path.of("/proc/net/dev");
            if (Files.isReadable(procNetDev)) {
                NetworkUsageSnapshot snapshot = AdminOpsNetworkMetricParser.parseProcNetDev(Files.readString(procNetDev));
                if (snapshot.hasTraffic()) {
                    return "RX " + formatBytes(snapshot.getReceivedBytes()) + " / TX " + formatBytes(snapshot.getSentBytes());
                }
            }
        } catch (Exception ignored) {
        }
        try {
            Process process = new ProcessBuilder("netstat", "-e")
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return "N/A";
            }
            String output;
            try (InputStream inputStream = process.getInputStream()) {
                output = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
            for (String line : output.split("\\r?\\n")) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("Bytes")) {
                    continue;
                }
                String[] parts = trimmed.split("\\s+");
                if (parts.length >= 3) {
                    long received = Long.parseLong(parts[1]);
                    long sent = Long.parseLong(parts[2]);
                    return "RX " + formatBytes(received) + " / TX " + formatBytes(sent);
                }
            }
        } catch (Exception ignored) {
        }
        return "N/A";
    }
}
