package com.tianji.user.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.TenantContext;
import com.tianji.common.utils.UserContext;
import com.tianji.user.domain.dto.adminops.AdminJenkinsBuildRequestDTO;
import com.tianji.user.domain.dto.adminops.AdminReleaseApplyUpdateDTO;
import com.tianji.user.domain.dto.adminops.AdminReleaseBuildDTO;
import com.tianji.user.domain.dto.adminops.AdminReleaseDeploymentContext;
import com.tianji.user.domain.dto.adminops.AdminReleaseDeployDTO;
import com.tianji.user.domain.dto.adminops.AdminReleasePublishCurrentDTO;
import com.tianji.user.domain.dto.adminops.AdminReleaseUpdateManifestDTO;
import com.tianji.user.domain.vo.adminops.AdminJenkinsBuildTriggerVO;
import com.tianji.user.domain.vo.adminops.AdminReleaseDeploymentResult;
import com.tianji.user.domain.vo.adminops.AdminReleaseRegistryPublishResult;
import com.tianji.user.domain.vo.adminops.AdminReleaseServiceVO;
import com.tianji.user.domain.vo.adminops.AdminReleaseTaskVO;
import com.tianji.user.domain.vo.adminops.AdminReleaseUpdateManifestVO;
import com.tianji.user.domain.vo.adminops.AdminReleaseVersionVO;
import com.tianji.user.service.IAdminOpsService;
import com.tianji.user.service.IAdminReleaseCenterService;
import com.tianji.user.service.IAdminReleaseDeploymentExecutor;
import com.tianji.user.service.IAdminReleaseRegistryPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AdminReleaseCenterServiceImpl implements IAdminReleaseCenterService {

    private static final String DEFAULT_REGISTRY_PREFIX = "registry.example.com/rk-web";
    private static final String DEFAULT_NAMESPACE = "shetuanguanlixitong";
    private static final String DEFAULT_JENKINS_JOB = "rk-web-cloud-master-new";
    private static final Pattern IMAGE_PATTERN = Pattern.compile("^[A-Za-z0-9._:/@-]+$");
    private static final Pattern VERSION_PATTERN = Pattern.compile("^[A-Za-z0-9._-]+$");
    private static final DateTimeFormatter VERSION_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd.HHmmss");
    private static final Set<String> ALLOWED_UPDATE_ACTION_TYPES = Set.of(
            "image-rollout",
            "sql-migrate",
            "nacos-import",
            "minio-sync",
            "jenkins-build"
    );
    private static final Set<String> FORBIDDEN_UPDATE_TOKENS = Set.of(
            "shell",
            "bash",
            "powershell",
            "cmd",
            "ssh",
            "script"
    );

    private final JdbcTemplate jdbcTemplate;
    private final IAdminOpsService adminOpsService;
    private final IAdminReleaseRegistryPublisher registryPublisher;
    private final IAdminReleaseDeploymentExecutor k3sDeploymentExecutor;
    private final IAdminReleaseDeploymentExecutor composeDeploymentExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${rk.ops.release.registry-prefix:${rk.ops.deploy-package.registry-prefix:registry.example.com/rk-web}}")
    private String configuredRegistryPrefix;

    public AdminReleaseCenterServiceImpl(JdbcTemplate jdbcTemplate,
                                         IAdminOpsService adminOpsService,
                                         IAdminReleaseRegistryPublisher registryPublisher,
                                         @Qualifier("adminReleaseK3sDeploymentExecutor")
                                         IAdminReleaseDeploymentExecutor k3sDeploymentExecutor,
                                         @Qualifier("adminReleaseComposeDeploymentExecutor")
                                         IAdminReleaseDeploymentExecutor composeDeploymentExecutor) {
        this.jdbcTemplate = jdbcTemplate;
        this.adminOpsService = adminOpsService;
        this.registryPublisher = registryPublisher;
        this.k3sDeploymentExecutor = k3sDeploymentExecutor;
        this.composeDeploymentExecutor = composeDeploymentExecutor;
    }

    @Override
    public List<AdminReleaseServiceVO> listReleaseServices(String namespace) {
        requireTenantOneOpsAccess();
        List<Map<String, Object>> serviceRows = jdbcTemplate.queryForList(
                "SELECT id, service_code, display_name, service_type, git_source, git_repo, branch_name, module_path, build_mode, image_name, namespace_name, workload_type, workload_name, container_name, health_check_path, resource_limits, enabled, sort_order " +
                        "FROM ops_service_registry WHERE is_deleted = 0 AND enabled = 1 ORDER BY sort_order ASC, service_code ASC"
        );
        List<AdminReleaseVersionVO> versions = queryRecentVersions();
        Map<String, List<AdminReleaseVersionVO>> versionsByService = versions.stream()
                .collect(Collectors.groupingBy(AdminReleaseVersionVO::getServiceCode, LinkedHashMap::new, Collectors.toList()));

        String namespaceFilter = trimToNull(namespace);
        List<AdminReleaseServiceVO> result = new ArrayList<>();
        for (Map<String, Object> row : serviceRows) {
            ServiceRegistryRow service = mapServiceRow(row);
            if (StringUtils.hasText(namespaceFilter) && !Objects.equals(namespaceFilter, service.namespaceName)) {
                continue;
            }
            AdminReleaseServiceVO vo = new AdminReleaseServiceVO();
            vo.setId(service.id);
            vo.setServiceCode(service.serviceCode);
            vo.setDisplayName(service.displayName);
            vo.setServiceType(service.serviceType);
            vo.setNamespaceName(service.namespaceName);
            vo.setWorkloadType(service.workloadType);
            vo.setWorkloadName(service.workloadName);
            vo.setContainerName(service.containerName);
            vo.setCurrentImage(service.imageName);
            vo.setCurrentVersion(extractImageTag(service.imageName));
            vo.setStatus(Boolean.TRUE.equals(service.enabled) ? "enabled" : "disabled");
            vo.setAvailableVersions(new ArrayList<>(versionsByService.getOrDefault(service.serviceCode, List.of())));
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<AdminReleaseVersionVO> listServiceVersions(String serviceCode) {
        requireTenantOneOpsAccess();
        String code = requireText(serviceCode, "serviceCode");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, service_code, version_tag, source_image, registry_image, source_type, git_source, branch_name, commit_id, jenkins_record_id, build_number, status, create_time " +
                        "FROM ops_release_version WHERE is_deleted = 0 AND service_code = ? ORDER BY create_time DESC, id DESC LIMIT 100",
                code
        );
        return rows.stream().map(this::mapVersion).collect(Collectors.toList());
    }

    @Override
    public AdminReleaseTaskVO publishCurrentImage(String serviceCode, AdminReleasePublishCurrentDTO dto) {
        requireTenantOneOpsAccess();
        String code = requireText(serviceCode, "serviceCode");
        AdminReleasePublishCurrentDTO request = dto == null ? new AdminReleasePublishCurrentDTO() : dto;
        ServiceRegistryRow service = loadServiceByCode(code);
        String sourceImage = firstText(request.getCurrentImage(), service.imageName);
        validateImageSyntax(sourceImage, "current image");
        String versionTag = normalizeVersionTag(request.getVersionTag());
        String registryPrefix = resolveRegistryPrefix(request.getRegistryPrefix());
        String targetImage = registryPrefix + "/" + resolveImageRepository(code, sourceImage) + ":" + versionTag;
        validateReleaseImage(targetImage);

        boolean dryRun = Boolean.TRUE.equals(request.getDryRun());
        StringBuilder logs = new StringBuilder();
        logs.append("release publish source=").append(sourceImage).append('\n');
        logs.append("target registry image=").append(targetImage).append('\n');

        AdminReleaseRegistryPublishResult publishResult = registryPublisher.publishCurrentImage(code, sourceImage, targetImage, dryRun);
        logs.append(publishResult == null ? "" : publishResult.getLogs());
        String safeLogs = redactSensitive(logs.toString());
        String currentStep = firstText(publishResult == null ? null : publishResult.getCurrentStep(), "registry-push");
        int progress = normalizeProgress(publishResult == null ? null : publishResult.getProgress());
        if (publishResult == null || !publishResult.isSuccess()) {
            // record failed task
            Long taskId = recordReleaseTask("publish-current", code, null, "failed", currentStep, progress,
                    safeLogs, UserContext.getUser());
            throw new BadRequestException("publish current image failed, taskId=" + taskId);
        }

        // record successful task
        Long taskId = recordReleaseTask("publish-current", code, null, "success", "registry-push", 100,
                safeLogs, UserContext.getUser());
        Long versionId = insertReleaseVersion(code, versionTag, sourceImage, targetImage, "running-image",
                buildPublishRequestJson(request, dryRun), "success", UserContext.getUser());

        AdminReleaseTaskVO result = new AdminReleaseTaskVO();
        result.setId(taskId);
        result.setTaskId(taskId);
        result.setTaskType("publish-current");
        result.setServiceCode(code);
        result.setTargetVersionId(versionId);
        result.setVersionId(versionId);
        result.setStatus("success");
        result.setCurrentStep(currentStep);
        result.setProgress(100);
        result.setLogs(safeLogs);
        return result;
    }

    @Override
    public AdminReleaseTaskVO triggerServiceBuild(String serviceCode, AdminReleaseBuildDTO dto) {
        requireTenantOneOpsAccess();
        String code = requireText(serviceCode, "serviceCode");
        AdminReleaseBuildDTO request = dto == null ? new AdminReleaseBuildDTO() : dto;
        loadServiceByCode(code);
        String jobName = firstText(request.getJobName(), DEFAULT_JENKINS_JOB);
        StringBuilder logs = new StringBuilder();
        logs.append("trigger Jenkins build for ").append(code).append('\n');
        logs.append("job=").append(jobName).append('\n');
        try {
            AdminJenkinsBuildRequestDTO buildRequest = new AdminJenkinsBuildRequestDTO();
            buildRequest.setServices(code);
            buildRequest.setTargetBranch(trimToNull(request.getTargetBranch()));
            buildRequest.setGitSource(trimToNull(request.getGitSource()));
            buildRequest.setImageMode(firstText(request.getImageMode(), "registry"));
            buildRequest.setDeployJobSuffix(trimToNull(request.getDeployJobSuffix()));
            buildRequest.setRemoteHost(trimToNull(request.getRemoteHost()));
            buildRequest.setRemotePort(trimToNull(request.getRemotePort()));
            buildRequest.setRemoteUser(trimToNull(request.getRemoteUser()));
            buildRequest.setRemotePassword(trimToNull(request.getRemotePassword()));
            AdminJenkinsBuildTriggerVO trigger = adminOpsService.triggerJenkinsJob(jobName, buildRequest);
            logs.append("Jenkins queueId=").append(trigger.getQueueId()).append('\n');
            logs.append("Jenkins recordId=").append(trigger.getRecordId()).append('\n');
            Long taskId = recordReleaseTask("jenkins-build", code, null, "queued", "jenkins-queued", 10,
                    logs.toString(), UserContext.getUser());
            AdminReleaseTaskVO result = new AdminReleaseTaskVO();
            result.setId(taskId);
            result.setTaskId(taskId);
            result.setTaskType("jenkins-build");
            result.setServiceCode(code);
            result.setStatus("queued");
            result.setCurrentStep("jenkins-queued");
            result.setProgress(10);
            result.setLogs(logs.toString());
            return result;
        } catch (Exception e) {
            logs.append("Jenkins trigger failed: ").append(e.getMessage()).append('\n');
            Long taskId = recordReleaseTask("jenkins-build", code, null, "failed", "jenkins-trigger", 100,
                    logs.toString(), UserContext.getUser());
            throw new BadRequestException("trigger Jenkins build failed, taskId=" + taskId + ": " + e.getMessage());
        }
    }

    @Override
    public AdminReleaseTaskVO deployServiceVersion(String serviceCode, AdminReleaseDeployDTO dto) {
        requireTenantOneOpsAccess();
        String code = requireText(serviceCode, "serviceCode");
        AdminReleaseDeployDTO request = dto == null ? new AdminReleaseDeployDTO() : dto;
        if (StringUtils.hasText(request.getTargetImage())) {
            validateReleaseImage(request.getTargetImage());
        }

        String runtimeMode = normalizeRuntimeMode(request.getRuntimeMode());
        ServiceRegistryRow service = loadServiceByCode(code);
        AdminReleaseVersionVO version = request.getVersionId() == null ? null : loadVersionById(request.getVersionId());
        String targetImage = firstText(request.getTargetImage(), version == null ? null : version.getRegistryImage());
        validateReleaseImage(targetImage);
        requireDeployConfirmation(request.getConfirmText(), service);

        String namespace = firstText(request.getNamespace(), service.namespaceName, DEFAULT_NAMESPACE);
        String kind = normalizeWorkloadType(service.workloadType);
        String previousImage = firstText(request.getPreviousImage(), service.imageName);
        AdminReleaseDeploymentContext context = buildDeploymentContext(code, runtimeMode, service, request, targetImage, previousImage, namespace, kind);
        AdminReleaseDeploymentResult deployResult = selectDeploymentExecutor(runtimeMode).deploy(context);
        StringBuilder logs = new StringBuilder();
        logs.append(deployResult == null ? "" : firstText(deployResult.getLogs(), ""));
        String status = deployResult != null && deployResult.isSuccess() ? "success" : "failed";
        String currentStep = firstText(deployResult == null ? null : deployResult.getCurrentStep(), runtimeMode + "-rollout");
        Integer progress = normalizeProgress(deployResult == null ? null : deployResult.getProgress());
        if (Boolean.TRUE.equals(request.getDryRun()) && !logs.toString().contains("dry-run only")) {
            logs.append("dry-run only, command not executed").append('\n');
        }

        Long taskId = recordReleaseTask("deploy-version", code, request.getVersionId(), runtimeMode, status, currentStep, progress,
                logs.toString(), UserContext.getUser());
        insertReleaseDeployment(code, request.getVersionId(), runtimeMode, namespace, kind, service.workloadName, service.containerName,
                previousImage, targetImage, status, request.getReason(), UserContext.getUser());
        insertOperationAudit("RELEASE_CENTER", namespace, kind, service.workloadName,
                Boolean.TRUE.equals(request.getDryRun()) ? "DEPLOY_DRY_RUN" : "DEPLOY",
                status,
                request.getReason(),
                toJson(buildDeployAuditPayload(request, runtimeMode, targetImage, previousImage)),
                logs.toString());

        AdminReleaseTaskVO result = new AdminReleaseTaskVO();
        result.setId(taskId);
        result.setTaskId(taskId);
        result.setTaskType("deploy-version");
        result.setServiceCode(code);
        result.setTargetVersionId(request.getVersionId());
        result.setVersionId(request.getVersionId());
        result.setRuntimeMode(runtimeMode);
        result.setStatus(status);
        result.setCurrentStep(currentStep);
        result.setProgress(progress);
        result.setLogs(logs.toString());
        if (!"success".equals(status)) {
            throw new BadRequestException("deploy service version failed, taskId=" + taskId);
        }
        return result;
    }

    @Override
    public AdminReleaseUpdateManifestVO buildUpdateManifest(AdminReleaseUpdateManifestDTO dto) {
        requireTenantOneOpsAccess();
        AdminReleaseUpdateManifestDTO request = dto == null ? new AdminReleaseUpdateManifestDTO() : dto;
        String updateVersion = normalizeVersionTag(request.getUpdateVersion());
        String channel = normalizeUpdateChannel(request.getChannel());
        String runtimeMode = normalizeRuntimeMode(request.getRuntimeMode());
        Set<String> serviceFilter = normalizeServiceFilter(request.getServiceCodes());
        List<AdminReleaseVersionVO> versions = querySuccessfulReleaseVersions();

        ObjectNode manifest = objectMapper.createObjectNode();
        manifest.put("schemaVersion", "rk-update-manifest/v1");
        manifest.put("updateVersion", updateVersion);
        manifest.put("channel", channel);
        manifest.put("runtimeMode", runtimeMode);
        manifest.put("commitId", trimToNull(request.getCommitId()));
        manifest.put("note", trimToNull(request.getNote()));
        ArrayNode actions = manifest.putArray("actions");
        Set<String> includedServices = new HashSet<>();
        for (AdminReleaseVersionVO version : versions) {
            String serviceCode = trimToNull(version.getServiceCode());
            if (serviceCode == null || includedServices.contains(serviceCode)) {
                continue;
            }
            if (!serviceFilter.isEmpty() && !serviceFilter.contains(serviceCode)) {
                continue;
            }
            String image = trimToNull(version.getRegistryImage());
            if (image == null) {
                continue;
            }
            validateReleaseImage(image);
            ObjectNode action = actions.addObject();
            action.put("type", "image-rollout");
            action.put("serviceCode", serviceCode);
            if (version.getId() != null) {
                action.put("versionId", version.getId());
            }
            action.put("versionTag", version.getVersionTag());
            action.put("image", image);
            action.put("runtimeMode", runtimeMode);
            includedServices.add(serviceCode);
        }
        String manifestJson = toJson(manifest);
        validateUpdateManifest(manifestJson, runtimeMode);
        Long packageId = recordUpdatePackage(updateVersion, channel, runtimeMode, manifestJson, "draft", UserContext.getUser());

        AdminReleaseUpdateManifestVO result = new AdminReleaseUpdateManifestVO();
        result.setPackageId(packageId);
        result.setUpdateVersion(updateVersion);
        result.setChannel(channel);
        result.setRuntimeMode(runtimeMode);
        result.setManifestJson(manifestJson);
        result.setActionCount(actions.size());
        result.setStatus("draft");
        return result;
    }

    @Override
    public AdminReleaseTaskVO applyUpdateManifest(AdminReleaseApplyUpdateDTO dto) {
        requireTenantOneOpsAccess();
        AdminReleaseApplyUpdateDTO request = dto == null ? new AdminReleaseApplyUpdateDTO() : dto;
        String runtimeMode = normalizeRuntimeMode(request.getRuntimeMode());
        JsonNode manifest = validateUpdateManifest(request.getManifestJson(), runtimeMode);
        boolean dryRun = !Boolean.FALSE.equals(request.getDryRun());
        if (!dryRun) {
            String confirmText = trimToNull(request.getConfirmText());
            if (!"APPLY UPDATE".equals(confirmText)) {
                throw new BadRequestException("confirm required: input APPLY UPDATE");
            }
            throw new BadRequestException("real update apply is not enabled for SQL/Nacos/MinIO actions; use dry-run first");
        }

        StringBuilder logs = new StringBuilder();
        logs.append("dry-run update apply\n");
        logs.append("updateVersion=").append(textAt(manifest, "updateVersion")).append('\n');
        logs.append("runtimeMode=").append(runtimeMode).append('\n');
        ArrayNode actions = (ArrayNode) manifest.path("actions");
        for (int i = 0; i < actions.size(); i++) {
            JsonNode action = actions.get(i);
            logs.append("plan #").append(i + 1)
                    .append(' ')
                    .append(textAt(action, "type"))
                    .append(' ')
                    .append(summaryForUpdateAction(action))
                    .append('\n');
        }
        String safeLogs = redactSensitive(logs.toString());
        Long taskId = recordReleaseTask("apply-update", "all", null, runtimeMode, "success",
                "dry-run update apply", 100, safeLogs, UserContext.getUser());

        AdminReleaseTaskVO result = new AdminReleaseTaskVO();
        result.setId(taskId);
        result.setTaskId(taskId);
        result.setTaskType("apply-update");
        result.setServiceCode("all");
        result.setRuntimeMode(runtimeMode);
        result.setStatus("success");
        result.setCurrentStep("dry-run update apply");
        result.setProgress(100);
        result.setLogs(safeLogs);
        return result;
    }

    @Override
    public AdminReleaseTaskVO getReleaseTask(Long taskId) {
        requireTenantOneOpsAccess();
        if (taskId == null) {
            throw new BadRequestException("taskId is required");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, task_type, service_code, target_version_id, runtime_mode, status, current_step, progress, logs, create_time, update_time " +
                        "FROM ops_release_task WHERE id = ? AND is_deleted = 0",
                taskId
        );
        if (rows.isEmpty()) {
            throw new BadRequestException("release task not found");
        }
        return mapTask(rows.get(0));
    }

    private List<AdminReleaseVersionVO> queryRecentVersions() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, service_code, version_tag, source_image, registry_image, source_type, git_source, branch_name, commit_id, jenkins_record_id, build_number, status, create_time " +
                        "FROM ops_release_version WHERE is_deleted = 0 ORDER BY create_time DESC, id DESC LIMIT 200"
        );
        return rows.stream().map(this::mapVersion).collect(Collectors.toList());
    }

    private List<AdminReleaseVersionVO> querySuccessfulReleaseVersions() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, service_code, version_tag, source_image, registry_image, source_type, git_source, branch_name, commit_id, jenkins_record_id, build_number, status, create_time " +
                        "FROM ops_release_version WHERE is_deleted = 0 AND status = 'success' ORDER BY service_code ASC, create_time DESC, id DESC LIMIT 500"
        );
        return rows.stream().map(this::mapVersion).collect(Collectors.toList());
    }

    private ServiceRegistryRow loadServiceByCode(String serviceCode) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, service_code, display_name, service_type, git_source, git_repo, branch_name, module_path, build_mode, image_name, namespace_name, workload_type, workload_name, container_name, health_check_path, resource_limits, enabled, sort_order " +
                        "FROM ops_service_registry WHERE is_deleted = 0 AND service_code = ?",
                serviceCode
        );
        if (rows.isEmpty()) {
            throw new BadRequestException("release service not found: " + serviceCode);
        }
        return mapServiceRow(rows.get(0));
    }

    private AdminReleaseVersionVO loadVersionById(Long versionId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, service_code, version_tag, source_image, registry_image, source_type, git_source, branch_name, commit_id, jenkins_record_id, build_number, status, create_time " +
                        "FROM ops_release_version WHERE is_deleted = 0 AND id = ?",
                versionId
        );
        if (rows.isEmpty()) {
            throw new BadRequestException("release version not found");
        }
        return mapVersion(rows.get(0));
    }

    private ServiceRegistryRow mapServiceRow(Map<String, Object> row) {
        ServiceRegistryRow service = new ServiceRegistryRow();
        service.id = valueAsLongObject(row.get("id"));
        service.serviceCode = valueAsString(row.get("service_code"));
        service.displayName = valueAsString(row.get("display_name"));
        service.serviceType = valueAsString(row.get("service_type"));
        service.imageName = valueAsString(row.get("image_name"));
        service.namespaceName = firstText(valueAsString(row.get("namespace_name")), DEFAULT_NAMESPACE);
        service.workloadType = firstText(valueAsString(row.get("workload_type")), "StatefulSet");
        service.workloadName = firstText(valueAsString(row.get("workload_name")), "rk-server-" + service.serviceCode);
        service.containerName = firstText(valueAsString(row.get("container_name")), service.serviceCode);
        service.enabled = valueAsBoolean(row.get("enabled"));
        return service;
    }

    private AdminReleaseVersionVO mapVersion(Map<String, Object> row) {
        AdminReleaseVersionVO vo = new AdminReleaseVersionVO();
        vo.setId(valueAsLongObject(row.get("id")));
        vo.setServiceCode(valueAsString(row.get("service_code")));
        vo.setVersionTag(valueAsString(row.get("version_tag")));
        vo.setSourceImage(valueAsString(row.get("source_image")));
        vo.setRegistryImage(valueAsString(row.get("registry_image")));
        vo.setSourceType(valueAsString(row.get("source_type")));
        vo.setGitSource(valueAsString(row.get("git_source")));
        vo.setBranchName(valueAsString(row.get("branch_name")));
        vo.setCommitId(valueAsString(row.get("commit_id")));
        vo.setJenkinsRecordId(valueAsLongObject(row.get("jenkins_record_id")));
        vo.setBuildNumber(valueAsInteger(row.get("build_number")));
        vo.setStatus(valueAsString(row.get("status")));
        vo.setCreateTime(valueAsString(row.get("create_time")));
        return vo;
    }

    private AdminReleaseTaskVO mapTask(Map<String, Object> row) {
        AdminReleaseTaskVO vo = new AdminReleaseTaskVO();
        Long id = valueAsLongObject(row.get("id"));
        vo.setId(id);
        vo.setTaskId(id);
        vo.setTaskType(valueAsString(row.get("task_type")));
        vo.setServiceCode(valueAsString(row.get("service_code")));
        vo.setTargetVersionId(valueAsLongObject(row.get("target_version_id")));
        vo.setVersionId(valueAsLongObject(row.get("target_version_id")));
        vo.setRuntimeMode(valueAsString(row.get("runtime_mode")));
        vo.setStatus(valueAsString(row.get("status")));
        vo.setCurrentStep(valueAsString(row.get("current_step")));
        vo.setProgress(valueAsInteger(row.get("progress")));
        vo.setLogs(valueAsString(row.get("logs")));
        vo.setCreateTime(valueAsString(row.get("create_time")));
        vo.setUpdateTime(valueAsString(row.get("update_time")));
        return vo;
    }

    private Long recordReleaseTask(String taskType, String serviceCode, Long targetVersionId, String status,
                                   String currentStep, Integer progress, String logs, Long userId) {
        return recordReleaseTask(taskType, serviceCode, targetVersionId, null, status, currentStep, progress, logs, userId);
    }

    private Long recordReleaseTask(String taskType, String serviceCode, Long targetVersionId, String runtimeMode, String status,
                                   String currentStep, Integer progress, String logs, Long userId) {
        jdbcTemplate.update(
                "INSERT INTO ops_release_task (task_type, service_code, target_version_id, runtime_mode, status, current_step, progress, logs, created_by) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                taskType,
                serviceCode,
                targetVersionId,
                runtimeMode,
                status,
                currentStep,
                progress,
                limitText(logs, 20000),
                userId
        );
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private Long insertReleaseVersion(String serviceCode, String versionTag, String sourceImage, String registryImage,
                                      String sourceType, String metadataJson, String status, Long userId) {
        jdbcTemplate.update(
                "INSERT INTO ops_release_version (service_code, version_tag, source_image, registry_image, source_type, metadata_json, status, created_by) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                serviceCode,
                versionTag,
                sourceImage,
                registryImage,
                sourceType,
                metadataJson,
                status,
                userId
        );
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private Long insertReleaseDeployment(String serviceCode, Long versionId, String runtimeMode, String namespace, String workloadType,
                                         String workloadName, String containerName, String previousImage,
                                         String targetImage, String status, String reason, Long userId) {
        jdbcTemplate.update(
                "INSERT INTO ops_release_deployment (service_code, version_id, runtime_mode, namespace_name, workload_type, workload_name, container_name, previous_image, target_image, status, reason, created_by) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                serviceCode,
                versionId,
                runtimeMode,
                namespace,
                workloadType,
                workloadName,
                containerName,
                previousImage,
                targetImage,
                status,
                reason,
                userId
        );
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private Long recordUpdatePackage(String updateVersion, String channel, String runtimeMode, String manifestJson,
                                     String status, Long userId) {
        jdbcTemplate.update(
                "INSERT INTO ops_release_update_package (update_version, channel, runtime_mode, manifest_json, status, created_by) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                updateVersion,
                channel,
                runtimeMode,
                manifestJson,
                status,
                userId
        );
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void insertOperationAudit(String operationType, String targetNamespace, String targetKind, String targetName,
                                      String action, String status, String reason, String requestPayload, String resultOutput) {
        Long userId = UserContext.getUser();
        jdbcTemplate.update(
                "INSERT INTO ops_operation_audit (operation_type, target_namespace, target_kind, target_name, action, status, reason, request_payload, result_output, operator_user_id, operator_user_name) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                operationType,
                targetNamespace,
                targetKind,
                targetName,
                action,
                status,
                reason,
                requestPayload,
                limitText(resultOutput, 12000),
                userId,
                userId == null ? null : String.valueOf(userId)
        );
    }

    private void validateReleaseImage(String image) {
        validateImageSyntax(image, "release image");
        String prefix = resolveRegistryPrefix(null);
        if (!image.startsWith(prefix + "/")) {
            throw new BadRequestException("image registry is not allowed: " + image + ", expected prefix " + prefix);
        }
    }

    private void validateImageSyntax(String image, String label) {
        if (!StringUtils.hasText(image)) {
            throw new BadRequestException(label + " is required");
        }
        String value = image.trim();
        if (value.length() > 512 || !IMAGE_PATTERN.matcher(value).matches() || !value.contains("/")) {
            throw new BadRequestException(label + " format is invalid");
        }
    }

    private void requireDeployConfirmation(String confirmText, ServiceRegistryRow service) {
        String value = trimToNull(confirmText);
        if (!Objects.equals(value, service.serviceCode) && !Objects.equals(value, service.workloadName)) {
            throw new BadRequestException("confirm required: input " + service.serviceCode + " or " + service.workloadName);
        }
    }

    private String normalizeRuntimeMode(String runtimeMode) {
        String value = firstText(runtimeMode, "k3s").toLowerCase(Locale.ROOT);
        if ("k8s".equals(value) || "kubernetes".equals(value)) {
            value = "k3s";
        }
        if (!"k3s".equals(value) && !"docker-compose".equals(value)) {
            throw new BadRequestException("runtimeMode only supports k3s or docker-compose");
        }
        return value;
    }

    private String normalizeUpdateChannel(String channel) {
        String value = firstText(channel, "stable").toLowerCase(Locale.ROOT);
        if (!"stable".equals(value) && !"beta".equals(value) && !"local-dev".equals(value)) {
            throw new BadRequestException("update channel only supports stable, beta or local-dev");
        }
        return value;
    }

    private Set<String> normalizeServiceFilter(List<String> serviceCodes) {
        Set<String> result = new HashSet<>();
        if (serviceCodes == null) {
            return result;
        }
        for (String serviceCode : serviceCodes) {
            String value = trimToNull(serviceCode);
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    private IAdminReleaseDeploymentExecutor selectDeploymentExecutor(String runtimeMode) {
        if (k3sDeploymentExecutor != null && k3sDeploymentExecutor.supports(runtimeMode)) {
            return k3sDeploymentExecutor;
        }
        if (composeDeploymentExecutor != null && composeDeploymentExecutor.supports(runtimeMode)) {
            return composeDeploymentExecutor;
        }
        throw new BadRequestException("release deployment executor not found for runtime: " + runtimeMode);
    }

    private AdminReleaseDeploymentContext buildDeploymentContext(String serviceCode,
                                                                 String runtimeMode,
                                                                 ServiceRegistryRow service,
                                                                 AdminReleaseDeployDTO request,
                                                                 String targetImage,
                                                                 String previousImage,
                                                                 String namespace,
                                                                 String workloadType) {
        AdminReleaseDeploymentContext context = new AdminReleaseDeploymentContext();
        context.setServiceCode(serviceCode);
        context.setRuntimeMode(runtimeMode);
        context.setTargetImage(targetImage);
        context.setPreviousImage(previousImage);
        context.setNamespace(namespace);
        context.setWorkloadType(workloadType);
        context.setWorkloadResourceType(resourceName(workloadType));
        context.setWorkloadName(service.workloadName);
        context.setContainerName(service.containerName);
        context.setDryRun(Boolean.TRUE.equals(request.getDryRun()));
        context.setRolloutTimeoutSeconds(request.getRolloutTimeoutSeconds());
        context.setComposeProjectName(firstText(request.getComposeProjectName(), "rk-web"));
        context.setComposeServiceName(firstText(request.getComposeServiceName(), composeServiceName(service)));
        context.setComposeFile(firstText(request.getComposeFile(), "docker-compose.yaml"));
        return context;
    }

    private String composeServiceName(ServiceRegistryRow service) {
        if ("frontend".equals(service.serviceCode)) {
            return "rk-web-frontend";
        }
        return service.serviceCode;
    }

    private void requireTenantOneOpsAccess() {
        Long tenantId = TenantContext.getTenantId();
        long resolvedTenantId = tenantId == null ? 1L : tenantId;
        if (resolvedTenantId != 1L && !TenantContext.isSuperAdmin()) {
            throw new BadRequestException("release center only supports tenant 1 or super admin");
        }
    }

    private String buildPublishRequestJson(AdminReleasePublishCurrentDTO request, boolean dryRun) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("currentImage", request.getCurrentImage());
        payload.put("registryPrefix", request.getRegistryPrefix());
        payload.put("versionTag", request.getVersionTag());
        payload.put("dryRun", dryRun);
        payload.put("reason", request.getReason());
        return toJson(payload);
    }

    private Map<String, Object> buildDeployAuditPayload(AdminReleaseDeployDTO request, String runtimeMode, String targetImage, String previousImage) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("versionId", request.getVersionId());
        payload.put("runtimeMode", runtimeMode);
        payload.put("targetImage", targetImage);
        payload.put("previousImage", previousImage);
        payload.put("namespace", request.getNamespace());
        payload.put("composeProjectName", request.getComposeProjectName());
        payload.put("composeServiceName", request.getComposeServiceName());
        payload.put("composeFile", request.getComposeFile());
        payload.put("dryRun", Boolean.TRUE.equals(request.getDryRun()));
        return payload;
    }

    private JsonNode validateUpdateManifest(String manifestJson, String expectedRuntimeMode) {
        String json = requireText(manifestJson, "manifestJson");
        JsonNode manifest;
        try {
            manifest = objectMapper.readTree(json);
        } catch (Exception e) {
            throw new BadRequestException("manifestJson is invalid JSON");
        }
        if (!manifest.isObject()) {
            throw new BadRequestException("manifestJson root must be object");
        }
        rejectForbiddenUpdateTokens(manifest, "");
        String schemaVersion = textAt(manifest, "schemaVersion");
        if (!"rk-update-manifest/v1".equals(schemaVersion)) {
            throw new BadRequestException("manifest schemaVersion is not allowed");
        }
        requireText(textAt(manifest, "updateVersion"), "updateVersion");
        normalizeUpdateChannel(textAt(manifest, "channel"));
        String runtimeMode = normalizeRuntimeMode(firstText(textAt(manifest, "runtimeMode"), expectedRuntimeMode));
        if (StringUtils.hasText(expectedRuntimeMode) && !Objects.equals(runtimeMode, expectedRuntimeMode)) {
            throw new BadRequestException("manifest runtimeMode mismatch");
        }
        JsonNode actions = manifest.path("actions");
        if (!actions.isArray()) {
            throw new BadRequestException("manifest actions must be array");
        }
        for (JsonNode action : actions) {
            validateUpdateAction(action, runtimeMode);
        }
        return manifest;
    }

    private void validateUpdateAction(JsonNode action, String runtimeMode) {
        if (!action.isObject()) {
            throw new BadRequestException("update action must be object");
        }
        String type = textAt(action, "type");
        if (!ALLOWED_UPDATE_ACTION_TYPES.contains(type)) {
            throw new BadRequestException("update action type not allowed: " + type);
        }
        if ("image-rollout".equals(type)) {
            requireText(textAt(action, "serviceCode"), "serviceCode");
            validateReleaseImage(textAt(action, "image"));
            String actionRuntime = firstText(textAt(action, "runtimeMode"), runtimeMode);
            if (!Objects.equals(normalizeRuntimeMode(actionRuntime), runtimeMode)) {
                throw new BadRequestException("image-rollout runtimeMode mismatch");
            }
            return;
        }
        if ("sql-migrate".equals(type)) {
            validateRelativeUpdatePath(textAt(action, "path"), "sql migration path");
            return;
        }
        if ("nacos-import".equals(type)) {
            requireText(textAt(action, "dataId"), "dataId");
            requireText(textAt(action, "group"), "group");
            validateRelativeUpdatePath(textAt(action, "path"), "nacos config path");
            return;
        }
        if ("minio-sync".equals(type)) {
            requireText(textAt(action, "bucket"), "bucket");
            String prefix = trimToNull(textAt(action, "prefix"));
            if (prefix != null && (prefix.startsWith("/") || prefix.contains(".."))) {
                throw new BadRequestException("minio prefix is invalid");
            }
            return;
        }
        if ("jenkins-build".equals(type)) {
            requireText(textAt(action, "jobName"), "jobName");
            requireText(textAt(action, "serviceCode"), "serviceCode");
        }
    }

    private void rejectForbiddenUpdateTokens(JsonNode node, String path) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                rejectForbiddenText(key, path + "." + key);
                rejectForbiddenUpdateTokens(entry.getValue(), path + "." + key);
            });
            return;
        }
        if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                rejectForbiddenUpdateTokens(node.get(i), path + "[" + i + "]");
            }
            return;
        }
        if (node.isTextual()) {
            rejectForbiddenText(node.asText(), path);
        }
    }

    private void rejectForbiddenText(String value, String path) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        for (String token : FORBIDDEN_UPDATE_TOKENS) {
            if (lower.contains(token)) {
                throw new BadRequestException("forbidden update token at " + path + ": " + token);
            }
        }
        if (lower.contains("command")) {
            throw new BadRequestException("forbidden update token at " + path + ": command");
        }
    }

    private void validateRelativeUpdatePath(String path, String label) {
        String value = requireText(path, label);
        String normalized = value.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.contains("..") || normalized.contains(":")) {
            throw new BadRequestException(label + " is invalid");
        }
    }

    private String summaryForUpdateAction(JsonNode action) {
        String type = textAt(action, "type");
        if ("image-rollout".equals(type)) {
            return firstText(textAt(action, "serviceCode"), "-") + " -> " + firstText(textAt(action, "image"), "-");
        }
        if ("sql-migrate".equals(type)) {
            return firstText(textAt(action, "path"), "-");
        }
        if ("nacos-import".equals(type)) {
            return firstText(textAt(action, "dataId"), "-");
        }
        if ("minio-sync".equals(type)) {
            return firstText(textAt(action, "bucket"), "-") + "/" + firstText(textAt(action, "prefix"), "");
        }
        if ("jenkins-build".equals(type)) {
            return firstText(textAt(action, "jobName"), "-") + ":" + firstText(textAt(action, "serviceCode"), "-");
        }
        return type;
    }

    private String textAt(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
            return null;
        }
        return node.get(fieldName).asText();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("JSON serialize failed", e);
        }
    }

    private String normalizeVersionTag(String versionTag) {
        String value = firstText(versionTag, VERSION_TIME_FORMATTER.format(LocalDateTime.now()));
        if (!VERSION_PATTERN.matcher(value).matches()) {
            throw new BadRequestException("version tag format is invalid");
        }
        return value;
    }

    private String resolveImageRepository(String serviceCode, String image) {
        String candidate = trimToNull(image);
        if (candidate == null) {
            return serviceCode;
        }
        int slash = candidate.lastIndexOf('/');
        String repo = slash >= 0 ? candidate.substring(slash + 1) : candidate;
        int colon = repo.lastIndexOf(':');
        if (colon > 0) {
            repo = repo.substring(0, colon);
        }
        return StringUtils.hasText(repo) ? repo : serviceCode;
    }

    private String resolveRegistryPrefix(String override) {
        String prefix = firstText(override, configuredRegistryPrefix, DEFAULT_REGISTRY_PREFIX);
        return prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
    }

    private String extractImageTag(String image) {
        if (!StringUtils.hasText(image)) {
            return null;
        }
        int slash = image.lastIndexOf('/');
        int colon = image.lastIndexOf(':');
        return colon > slash ? image.substring(colon + 1) : null;
    }

    private String normalizeWorkloadType(String kind) {
        String value = firstText(kind, "Deployment").toLowerCase(Locale.ROOT);
        if ("statefulset".equals(value) || "statefulsets".equals(value) || "sts".equals(value)) {
            return "StatefulSet";
        }
        if ("deployment".equals(value) || "deployments".equals(value) || "deploy".equals(value)) {
            return "Deployment";
        }
        throw new BadRequestException("workload type only supports Deployment or StatefulSet");
    }

    private String resourceName(String workloadType) {
        return "StatefulSet".equals(workloadType) ? "statefulset" : "deployment";
    }

    private String quoteForShell(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    private int normalizeProgress(Integer progress) {
        if (progress == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, progress));
    }

    String redactSensitive(String text) {
        if (text == null) {
            return "";
        }
        String safe = text;
        safe = safe.replaceAll("(?i)(--password(?:=|\\s+))\\S+", "$1******");
        safe = safe.replaceAll("(?i)(--username(?:=|\\s+))\\S+", "$1******");
        safe = safe.replaceAll("(?i)((?:password|passwd|token|secret|authorization)\\s*[=:]\\s*)\\S+", "$1******");
        safe = safe.replaceAll("(?i)((?:REGISTRY_PASSWORD|REGISTRY_USERNAME)\\s*[=:]\\s*)\\S+", "$1******");
        safe = safe.replaceAll("(?im)(\\.dockerconfigjson\\s*[=:]\\s*).+$", "$1******");
        return safe;
    }

    private String requireText(String value, String label) {
        String text = trimToNull(value);
        if (text == null) {
            throw new BadRequestException(label + " is required");
        }
        return text;
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String text = trimToNull(value);
            if (text != null) {
                return text;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String valueAsString(Object value) {
        return value == null ? null : value.toString();
    }

    private Long valueAsLongObject(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer valueAsInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean valueAsBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        return value != null && Boolean.parseBoolean(value.toString());
    }

    private String limitText(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private static final class ServiceRegistryRow {
        private Long id;
        private String serviceCode;
        private String displayName;
        private String serviceType;
        private String imageName;
        private String namespaceName;
        private String workloadType;
        private String workloadName;
        private String containerName;
        private Boolean enabled;
    }
}
