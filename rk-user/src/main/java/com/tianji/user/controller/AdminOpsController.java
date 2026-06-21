package com.tianji.user.controller;

import com.tianji.common.domain.R;
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
import com.tianji.user.domain.vo.adminops.AdminApiWorkbenchResourceVO;
import com.tianji.user.domain.vo.adminops.AdminBackupOverviewVO;
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
import com.tianji.user.domain.vo.adminops.AdminJenkinsOverviewVO;
import com.tianji.user.domain.vo.adminops.AdminK8sActionResultVO;
import com.tianji.user.domain.vo.adminops.AdminK8sAbnormalPodVO;
import com.tianji.user.domain.vo.adminops.AdminK8sCleanupResultVO;
import com.tianji.user.domain.vo.adminops.AdminK8sImageVO;
import com.tianji.user.domain.vo.adminops.AdminK8sMaintenanceResultVO;
import com.tianji.user.domain.vo.adminops.AdminK8sOverviewVO;
import com.tianji.user.domain.vo.adminops.AdminK8sPodExecResultVO;
import com.tianji.user.domain.vo.adminops.AdminK8sWorkloadVO;
import com.tianji.user.domain.vo.adminops.AdminMonitoringLogVO;
import com.tianji.user.domain.vo.adminops.AdminMonitoringOverviewVO;
import com.tianji.user.domain.vo.adminops.AdminMinioBucketVO;
import com.tianji.user.domain.vo.adminops.AdminMinioObjectVO;
import com.tianji.user.domain.vo.adminops.AdminNacosConfigVO;
import com.tianji.user.domain.vo.adminops.AdminNacosConfigHistoryVO;
import com.tianji.user.domain.vo.adminops.AdminOpsAuditLogVO;
import com.tianji.user.domain.vo.adminops.AdminOpsServiceVO;
import com.tianji.user.domain.vo.adminops.AdminRainbondApiResultVO;
import com.tianji.user.domain.vo.adminops.AdminRainbondConfigVO;
import com.tianji.user.domain.vo.adminops.AdminTaskOverviewVO;
import com.tianji.user.domain.vo.adminops.AdminTopologyVO;
import com.tianji.user.domain.vo.adminops.AdminTrafficLocationVO;
import com.tianji.user.domain.vo.adminops.AdminTrafficOnlineUserVO;
import com.tianji.user.domain.vo.adminops.AdminTrafficOverviewVO;
import com.tianji.user.service.IAdminOpsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.Map;

@Api(tags = "Admin Ops")
@RestController
@RequestMapping("/admin/ops")
@RequiredArgsConstructor
public class AdminOpsController {

    private final IAdminOpsService adminOpsService;

    @ApiOperation("Monitoring overview")
    @GetMapping("/monitoring/overview")
    public R<AdminMonitoringOverviewVO> getMonitoringOverview() {
        return R.ok(adminOpsService.getMonitoringOverview());
    }

    @ApiOperation("Monitoring pod logs")
    @GetMapping("/monitoring/logs")
    public R<AdminMonitoringLogVO> getMonitoringLogs(
            @RequestParam String namespace,
            @RequestParam String podName,
            @RequestParam String containerName,
            @RequestParam(required = false, defaultValue = "200") Integer tailLines
    ) {
        return R.ok(adminOpsService.getMonitoringLogs(namespace, podName, containerName, tailLines));
    }

    @ApiOperation("Task overview")
    @GetMapping("/tasks/overview")
    public R<AdminTaskOverviewVO> getTaskOverview() {
        return R.ok(adminOpsService.getTaskOverview());
    }

    @ApiOperation("Pause task")
    @PostMapping("/tasks/{taskId}/pause")
    public R<Boolean> pauseTask(@PathVariable Long taskId) {
        return R.ok(adminOpsService.pauseTask(taskId));
    }

    @ApiOperation("Resume task")
    @PostMapping("/tasks/{taskId}/resume")
    public R<Boolean> resumeTask(@PathVariable Long taskId) {
        return R.ok(adminOpsService.resumeTask(taskId));
    }

    @ApiOperation("Trigger task")
    @PostMapping("/tasks/{taskId}/trigger")
    public R<Boolean> triggerTask(@PathVariable Long taskId) {
        return R.ok(adminOpsService.triggerTask(taskId));
    }

    @ApiOperation("Jenkins overview")
    @GetMapping("/jenkins/overview")
    public R<AdminJenkinsOverviewVO> getJenkinsOverview() {
        return R.ok(adminOpsService.getJenkinsOverview());
    }

    @ApiOperation("Jenkins git source options")
    @GetMapping("/jenkins/git-sources")
    public R<List<AdminJenkinsGitSourceVO>> getJenkinsGitSources() {
        return R.ok(adminOpsService.getJenkinsGitSources());
    }

    @ApiOperation("Jenkins git branch options")
    @GetMapping("/jenkins/git-branches")
    public R<AdminJenkinsGitBranchesVO> getJenkinsGitBranches(@RequestParam(defaultValue = "local") String source) {
        return R.ok(adminOpsService.getJenkinsGitBranches(source));
    }

    @ApiOperation("Trigger Jenkins job")
    @PostMapping("/jenkins/jobs/{jobName}/trigger")
    public R<AdminJenkinsBuildTriggerVO> triggerJenkinsJob(
            @PathVariable String jobName,
            @RequestBody(required = false) AdminJenkinsBuildRequestDTO dto
    ) {
        return R.ok(adminOpsService.triggerJenkinsJob(jobName, dto == null ? new AdminJenkinsBuildRequestDTO() : dto));
    }

    @ApiOperation("Jenkins build status")
    @GetMapping("/jenkins/jobs/{jobName}/build-status")
    public R<AdminJenkinsBuildStatusVO> getJenkinsBuildStatus(
            @PathVariable String jobName,
            @RequestParam(required = false) String queueId,
            @RequestParam(required = false) Integer buildNumber,
            @RequestParam(required = false, defaultValue = "120") Integer tailLines
    ) {
        return R.ok(adminOpsService.getJenkinsBuildStatus(jobName, queueId, buildNumber, tailLines));
    }

    @ApiOperation("Jenkins build history")
    @GetMapping("/jenkins/build-history")
    public R<List<AdminJenkinsBuildRecordVO>> getJenkinsBuildHistory(
            @RequestParam(required = false) String serviceCode,
            @RequestParam(required = false) String branchName,
            @RequestParam(required = false) String gitSource,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String operatorKeyword,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        return R.ok(adminOpsService.getJenkinsBuildHistory(serviceCode, branchName, gitSource, status, operatorKeyword, startTime, endTime));
    }

    @ApiOperation("Jenkins deployable services")
    @GetMapping("/jenkins/services")
    public R<List<AdminOpsServiceVO>> listServiceRegistry() {
        return R.ok(adminOpsService.listServiceRegistry());
    }

    @ApiOperation("Save Jenkins deployable service")
    @PostMapping("/jenkins/services")
    public R<AdminOpsServiceVO> saveServiceRegistry(@RequestBody AdminOpsServiceSaveDTO dto) {
        return R.ok(adminOpsService.saveServiceRegistry(dto));
    }

    @ApiOperation("Delete Jenkins deployable service")
    @PostMapping("/jenkins/services/{id}/delete")
    public R<Boolean> deleteServiceRegistry(@PathVariable Long id) {
        return R.ok(adminOpsService.deleteServiceRegistry(id));
    }

    @ApiOperation("Nacos config list")
    @GetMapping("/nacos/configs")
    public R<List<AdminNacosConfigVO>> listNacosConfigs(
            @RequestParam(required = false) String namespaceId,
            @RequestParam(required = false) String groupName
    ) {
        return R.ok(adminOpsService.listNacosConfigs(namespaceId, groupName));
    }

    @ApiOperation("Nacos config detail")
    @GetMapping("/nacos/config")
    public R<AdminNacosConfigVO> getNacosConfig(
            @RequestParam(required = false) String namespaceId,
            @RequestParam(required = false) String groupName,
            @RequestParam String dataId
    ) {
        return R.ok(adminOpsService.getNacosConfig(namespaceId, groupName, dataId));
    }

    @ApiOperation("Nacos config history")
    @GetMapping("/nacos/config-history")
    public R<List<AdminNacosConfigHistoryVO>> getNacosConfigHistory(
            @RequestParam(required = false) String namespaceId,
            @RequestParam(required = false) String groupName,
            @RequestParam String dataId
    ) {
        return R.ok(adminOpsService.getNacosConfigHistory(namespaceId, groupName, dataId));
    }

    @ApiOperation("Save Nacos config")
    @PostMapping("/nacos/configs")
    public R<Boolean> saveNacosConfig(@RequestBody AdminNacosConfigSaveDTO dto) {
        return R.ok(adminOpsService.saveNacosConfig(dto));
    }

    @ApiOperation("Traffic center overview")
    @GetMapping("/traffic/overview")
    public R<AdminTrafficOverviewVO> getTrafficOverview(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        return R.ok(adminOpsService.getTrafficOverview(tenantId, startTime, endTime));
    }

    @ApiOperation("Traffic center drilldown")
    @GetMapping("/traffic/drilldown")
    public R<AdminTrafficOverviewVO> getTrafficDrilldown(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        return R.ok(adminOpsService.getTrafficDrilldown(tenantId, country, province, startTime, endTime));
    }

    @ApiOperation("Traffic center online users")
    @GetMapping("/traffic/online-users")
    public R<List<AdminTrafficOnlineUserVO>> getTrafficOnlineUsers(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false, defaultValue = "10") Integer windowMinutes
    ) {
        return R.ok(adminOpsService.getTrafficOnlineUsers(tenantId, windowMinutes));
    }

    @ApiOperation("Traffic center current origin")
    @GetMapping("/traffic/current-origin")
    public R<AdminTrafficLocationVO> getTrafficCurrentOrigin() {
        return R.ok(adminOpsService.getTrafficCurrentOrigin());
    }

    @ApiOperation("Traffic center default tenant")
    @GetMapping("/traffic/default-tenant")
    public R<AdminTrafficOverviewVO> getTrafficDefaultTenant() {
        return R.ok(adminOpsService.getTrafficDefaultTenant());
    }

    @ApiOperation("Save traffic center default tenant")
    @PostMapping("/traffic/default-tenant")
    public R<AdminTrafficOverviewVO> saveTrafficDefaultTenant(@RequestBody AdminTrafficDefaultTenantDTO dto) {
        return R.ok(adminOpsService.saveTrafficDefaultTenant(dto));
    }

    @ApiOperation("Ops operation audit logs")
    @GetMapping("/audit-logs")
    public R<List<AdminOpsAuditLogVO>> listOperationAuditLogs(
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String targetName,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "30") Integer limit
    ) {
        return R.ok(adminOpsService.listOperationAuditLogs(operationType, targetName, action, status, limit));
    }

    @ApiOperation("API workbench resources")
    @GetMapping("/api-workbench/resources")
    public R<List<AdminApiWorkbenchResourceVO>> getApiWorkbenchResources() {
        return R.ok(adminOpsService.getApiWorkbenchResources());
    }

    @ApiOperation("API workbench doc")
    @GetMapping("/api-workbench/docs")
    public R<AdminApiWorkbenchDocVO> getApiWorkbenchDoc(@RequestParam("url") String resourceUrl) {
        return R.ok(adminOpsService.getApiWorkbenchDoc(resourceUrl));
    }

    @ApiOperation("Backup overview")
    @GetMapping("/backup/overview")
    public R<AdminBackupOverviewVO> getBackupOverview() {
        return R.ok(adminOpsService.getBackupOverview());
    }

    @ApiOperation("Backup table options")
    @GetMapping("/backup/tables")
    public R<Map<String, List<String>>> getBackupTables(@RequestParam(required = false) List<String> databases) {
        return R.ok(adminOpsService.getBackupTables(databases));
    }

    @ApiOperation("Create backup")
    @PostMapping("/backup/create")
    public R<AdminBackupOverviewVO> createBackup(@RequestBody(required = false) AdminBackupCreateDTO dto) {
        return R.ok(adminOpsService.createBackup(dto == null ? new AdminBackupCreateDTO() : dto));
    }

    @ApiOperation("Save backup schedule")
    @PostMapping("/backup/schedule")
    public R<AdminBackupOverviewVO> saveBackupSchedule(@RequestBody AdminBackupScheduleDTO dto) {
        return R.ok(adminOpsService.saveBackupSchedule(dto));
    }

    @ApiOperation("Delete backup")
    @PostMapping("/backup/{backupId}/delete")
    public R<Boolean> deleteBackup(@PathVariable Long backupId) {
        return R.ok(adminOpsService.deleteBackup(backupId));
    }

    @ApiOperation("Download backup")
    @GetMapping("/backup/{backupId}/download")
    public ResponseEntity<byte[]> downloadBackup(@PathVariable Long backupId) {
        byte[] bytes = adminOpsService.downloadBackup(backupId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("backup-" + backupId + ".sql.gz").build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @ApiOperation("Deploy package records")
    @GetMapping("/deploy-package/records")
    public R<List<AdminDeployPackageRecordVO>> listDeployPackages() {
        return R.ok(adminOpsService.listDeployPackages());
    }

    @ApiOperation("Deploy package detail")
    @GetMapping("/deploy-package/{packageId}")
    public R<AdminDeployPackageDetailVO> getDeployPackageDetail(@PathVariable Long packageId) {
        return R.ok(adminOpsService.getDeployPackageDetail(packageId));
    }

    @ApiOperation("View stored remote deploy kubeconfig")
    @GetMapping("/deploy-package/{packageId}/kubeconfig")
    public R<AdminDeployPackageKubeconfigVO> getDeployPackageKubeconfig(@PathVariable Long packageId) {
        return R.ok(adminOpsService.getDeployPackageKubeconfig(packageId));
    }

    @ApiOperation("Diagnose remote deploy target cluster")
    @PostMapping("/deploy-package/{packageId}/diagnose")
    public R<AdminDeployPackageDiagnosisVO> diagnoseDeployPackage(@PathVariable Long packageId) {
        return R.ok(adminOpsService.diagnoseDeployPackage(packageId));
    }

    @ApiOperation("Repair remote deploy target cluster")
    @PostMapping("/deploy-package/{packageId}/repair")
    public R<AdminDeployPackageRepairResultVO> repairDeployPackage(
            @PathVariable Long packageId,
            @RequestBody(required = false) AdminDeployPackageRepairDTO dto
    ) {
        return R.ok(adminOpsService.repairDeployPackage(packageId, dto == null ? new AdminDeployPackageRepairDTO() : dto));
    }

    @ApiOperation("Create deploy package")
    @PostMapping("/deploy-package/create")
    public R<AdminDeployPackageRecordVO> createDeployPackage(@RequestBody(required = false) AdminDeployPackageCreateDTO dto) {
        return R.ok(adminOpsService.createDeployPackage(dto == null ? new AdminDeployPackageCreateDTO() : dto));
    }

    @ApiOperation("Create online remote deploy")
    @PostMapping("/deploy-package/remote-deploy")
    public R<AdminDeployPackageRecordVO> createRemoteDeploy(@RequestBody(required = false) AdminDeployPackageCreateDTO dto) {
        return R.ok(adminOpsService.createRemoteDeploy(dto == null ? new AdminDeployPackageCreateDTO() : dto));
    }

    @ApiOperation("Preflight online remote deploy")
    @PostMapping("/deploy-package/preflight")
    public R<AdminDeployPackagePreflightVO> preflightDeployPackage(@RequestBody(required = false) AdminDeployPackageCreateDTO dto) {
        return R.ok(adminOpsService.preflightDeployPackage(dto == null ? new AdminDeployPackageCreateDTO() : dto));
    }

    @ApiOperation("Remote migration records")
    @GetMapping("/remote-migration/records")
    public R<List<AdminDeployPackageRecordVO>> listRemoteMigrations() {
        return R.ok(adminOpsService.listRemoteMigrations());
    }

    @ApiOperation("Remote migration detail")
    @GetMapping("/remote-migration/{migrationId}")
    public R<AdminDeployPackageDetailVO> getRemoteMigrationDetail(@PathVariable Long migrationId) {
        return R.ok(adminOpsService.getRemoteMigrationDetail(migrationId));
    }

    @ApiOperation("View stored remote migration kubeconfig")
    @GetMapping("/remote-migration/{migrationId}/kubeconfig")
    public R<AdminDeployPackageKubeconfigVO> getRemoteMigrationKubeconfig(@PathVariable Long migrationId) {
        return R.ok(adminOpsService.getRemoteMigrationKubeconfig(migrationId));
    }

    @ApiOperation("Diagnose remote migration target cluster")
    @PostMapping("/remote-migration/{migrationId}/diagnose")
    public R<AdminDeployPackageDiagnosisVO> diagnoseRemoteMigration(@PathVariable Long migrationId) {
        return R.ok(adminOpsService.diagnoseRemoteMigration(migrationId));
    }

    @ApiOperation("Repair remote migration target cluster")
    @PostMapping("/remote-migration/{migrationId}/repair")
    public R<AdminDeployPackageRepairResultVO> repairRemoteMigration(
            @PathVariable Long migrationId,
            @RequestBody(required = false) AdminDeployPackageRepairDTO dto
    ) {
        return R.ok(adminOpsService.repairRemoteMigration(migrationId, dto == null ? new AdminDeployPackageRepairDTO() : dto));
    }

    @ApiOperation("Create remote migration by kubeconfig")
    @PostMapping("/remote-migration/kubeconfig-deploy")
    public R<AdminDeployPackageRecordVO> createRemoteMigration(@RequestBody(required = false) AdminDeployPackageCreateDTO dto) {
        return R.ok(adminOpsService.createRemoteMigration(dto == null ? new AdminDeployPackageCreateDTO() : dto));
    }

    @ApiOperation("Preflight remote migration")
    @PostMapping("/remote-migration/preflight")
    public R<AdminDeployPackagePreflightVO> preflightRemoteMigration(@RequestBody(required = false) AdminDeployPackageCreateDTO dto) {
        return R.ok(adminOpsService.preflightRemoteMigration(dto == null ? new AdminDeployPackageCreateDTO() : dto));
    }

    @ApiOperation("Create remote migration by Linux SSH")
    @PostMapping("/remote-migration/linux-ssh-deploy")
    public R<AdminDeployPackageRecordVO> createLinuxSshMigration(@RequestBody(required = false) AdminDeployPackageCreateDTO dto) {
        return R.ok(adminOpsService.createLinuxSshMigration(dto == null ? new AdminDeployPackageCreateDTO() : dto));
    }

    @ApiOperation("GET /deploy-package/active-migration")
    @GetMapping("/deploy-package/active-migration")
    public R<AdminDeployPackageRecordVO> getActiveGlobalMigration() {
        return R.ok(adminOpsService.getActiveGlobalMigration());
    }

    @ApiOperation("GET /public/deploy-package/active-migration")
    @GetMapping("/public/deploy-package/active-migration")
    public R<AdminDeployPackageRecordVO> getPublicActiveGlobalMigration() {
        return R.ok(adminOpsService.getPublicActiveGlobalMigration());
    }

    @ApiOperation("Download deploy package")
    @GetMapping("/deploy-package/{packageId}/download")
    public ResponseEntity<StreamingResponseBody> downloadDeployPackage(@PathVariable Long packageId) {
        StreamingResponseBody body = outputStream -> adminOpsService.streamDeployPackage(packageId, outputStream);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("rk-web-deploy-package-" + packageId + ".zip").build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }

    @ApiOperation("Public Linux SSH deploy package download")
    @GetMapping("/public/deploy-package/{packageId}/linux-ssh-download")
    public ResponseEntity<StreamingResponseBody> downloadLinuxSshDeployPackage(
            @PathVariable Long packageId,
            @RequestParam String token
    ) {
        StreamingResponseBody body = outputStream -> adminOpsService.streamLinuxSshDeployPackage(packageId, token, outputStream);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("rk-web-linux-ssh-deploy-" + packageId + ".zip").build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }

    @ApiOperation("MinIO bucket list")
    @GetMapping("/minio/buckets")
    public R<List<AdminMinioBucketVO>> listMinioBuckets() {
        return R.ok(adminOpsService.listMinioBuckets());
    }

    @ApiOperation("MinIO object list")
    @GetMapping("/minio/objects")
    public R<List<AdminMinioObjectVO>> listMinioObjects(
            @RequestParam String bucket,
            @RequestParam(required = false) String prefix,
            @RequestParam(required = false, defaultValue = "true") Boolean recursive,
            @RequestParam(required = false, defaultValue = "200") Integer limit
    ) {
        return R.ok(adminOpsService.listMinioObjects(bucket, prefix, recursive, limit));
    }

    @ApiOperation("Download MinIO object")
    @GetMapping("/minio/object/download")
    public ResponseEntity<StreamingResponseBody> downloadMinioObject(
            @RequestParam String bucket,
            @RequestParam("object") String objectName
    ) {
        StreamingResponseBody body = outputStream -> adminOpsService.streamMinioObject(bucket, objectName, outputStream);
        String fileName = objectName == null ? "minio-object" : objectName.replace("\\", "/").replaceFirst("^.*/", "");
        if (!org.springframework.util.StringUtils.hasText(fileName)) {
            fileName = "minio-object";
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }

    @ApiOperation("K8s overview")
    @GetMapping("/k8s/overview")
    public R<AdminK8sOverviewVO> getK8sOverview() {
        return R.ok(adminOpsService.getK8sOverview());
    }

    @ApiOperation("Run K8s node cleanup")
    @PostMapping("/k8s/cleanup")
    public R<AdminK8sCleanupResultVO> runK8sCleanup(@RequestBody AdminK8sCleanupRequestDTO dto) {
        return R.ok(adminOpsService.runK8sCleanup(dto));
    }

    @ApiOperation("List K8s runtime images")
    @GetMapping("/k8s/images")
    public R<List<AdminK8sImageVO>> listK8sImages(
            @RequestParam(required = false) String nodeName,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "false") Boolean unusedOnly
    ) {
        return R.ok(adminOpsService.listK8sImages(nodeName, keyword, unusedOnly));
    }

    @ApiOperation("Cleanup K8s runtime images")
    @PostMapping("/k8s/images/cleanup")
    public R<AdminK8sCleanupResultVO> cleanupK8sImages(@RequestBody(required = false) AdminK8sImageCleanupDTO dto) {
        return R.ok(adminOpsService.cleanupK8sImages(dto == null ? new AdminK8sImageCleanupDTO() : dto));
    }

    @ApiOperation("Download K8s image export script")
    @GetMapping("/k8s/images/export-script")
    public ResponseEntity<byte[]> downloadK8sImageExportScript(
            @RequestParam(required = false) String nodeName,
            @RequestParam(required = false) String nodeIp,
            @RequestParam String image
    ) {
        byte[] bytes = adminOpsService.downloadK8sImageExportScript(nodeName, nodeIp, image);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("export-image.sh").build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @ApiOperation("List K8s abnormal pods")
    @GetMapping("/k8s/abnormal-pods")
    public R<List<AdminK8sAbnormalPodVO>> listK8sAbnormalPods(
            @RequestParam(required = false) List<String> namespaces,
            @RequestParam(required = false) List<String> statuses
    ) {
        return R.ok(adminOpsService.listK8sAbnormalPods(namespaces, statuses));
    }

    @ApiOperation("Cleanup K8s abnormal pods")
    @PostMapping("/k8s/abnormal-pods/cleanup")
    public R<AdminK8sMaintenanceResultVO> cleanupK8sAbnormalPods(@RequestBody(required = false) AdminK8sAbnormalPodCleanupDTO dto) {
        return R.ok(adminOpsService.cleanupK8sAbnormalPods(dto == null ? new AdminK8sAbnormalPodCleanupDTO() : dto));
    }

    @ApiOperation("Run K8s cluster maintenance")
    @PostMapping("/k8s/maintenance/run")
    public R<AdminK8sMaintenanceResultVO> runK8sClusterMaintenance(@RequestBody(required = false) AdminK8sClusterMaintenanceDTO dto) {
        return R.ok(adminOpsService.runK8sClusterMaintenance(dto == null ? new AdminK8sClusterMaintenanceDTO() : dto));
    }

    @ApiOperation("Ensure K8s maintenance XXL-Job")
    @PostMapping("/k8s/maintenance/schedule")
    public R<Map<String, Object>> ensureK8sMaintenanceXxlJob(@RequestBody(required = false) AdminK8sClusterMaintenanceDTO dto) {
        return R.ok(adminOpsService.ensureK8sMaintenanceXxlJob(dto == null ? new AdminK8sClusterMaintenanceDTO() : dto));
    }

    @ApiOperation("K8s workloads")
    @GetMapping("/k8s/workloads")
    public R<List<AdminK8sWorkloadVO>> getK8sWorkloads(@RequestParam(required = false) String namespace) {
        return R.ok(adminOpsService.getK8sWorkloads(namespace));
    }

    @ApiOperation("Run K8s workload action")
    @PostMapping("/k8s/workloads/action")
    public R<AdminK8sActionResultVO> runK8sWorkloadAction(@RequestBody AdminK8sWorkloadActionDTO dto) {
        return R.ok(adminOpsService.runK8sWorkloadAction(dto));
    }

    @ApiOperation("Run K8s pod exec command")
    @PostMapping("/k8s/pods/exec")
    public R<AdminK8sPodExecResultVO> runK8sPodExec(@RequestBody AdminK8sPodExecRequestDTO dto) {
        return R.ok(adminOpsService.runK8sPodExec(dto));
    }

    @ApiOperation("Ops service topology")
    @GetMapping("/topology")
    public R<AdminTopologyVO> getServiceTopology(@RequestParam(required = false) String namespace) {
        return R.ok(adminOpsService.getServiceTopology(namespace));
    }

    @ApiOperation("Rainbond API config")
    @GetMapping("/rainbond/config")
    public R<AdminRainbondConfigVO> getRainbondConfig() {
        return R.ok(adminOpsService.getRainbondConfig());
    }

    @ApiOperation("Save Rainbond API config")
    @PostMapping("/rainbond/config")
    public R<AdminRainbondConfigVO> saveRainbondConfig(@RequestBody AdminRainbondConfigDTO dto) {
        return R.ok(adminOpsService.saveRainbondConfig(dto));
    }

    @ApiOperation("Rainbond API catalog")
    @GetMapping("/rainbond/catalog")
    public R<Map<String, Object>> getRainbondCatalog() {
        return R.ok(adminOpsService.getRainbondCatalog());
    }

    @ApiOperation("Frontend cache overview")
    @GetMapping("/frontend-cache/overview")
    public R<Map<String, Object>> getFrontendCacheOverview() {
        return R.ok(adminOpsService.getFrontendCacheOverview());
    }

    @ApiOperation("Warmup frontend cache")
    @PostMapping("/frontend-cache/warmup")
    public R<Map<String, Object>> warmupFrontendCache() {
        return R.ok(adminOpsService.warmupFrontendCache());
    }

    @ApiOperation("Clear frontend cache")
    @PostMapping("/frontend-cache/clear")
    public R<Map<String, Object>> clearFrontendCache() {
        return R.ok(adminOpsService.clearFrontendCache());
    }

    @ApiOperation("Visual screen overview")
    @GetMapping("/visual-screen/overview")
    public R<Map<String, Object>> getVisualScreenOverview() {
        return R.ok(adminOpsService.getVisualScreenOverview());
    }

    @ApiOperation("Rainbond environment discovery")
    @GetMapping("/rainbond/discovery")
    public R<Map<String, Object>> getRainbondDiscovery() {
        return R.ok(adminOpsService.getRainbondDiscovery());
    }

    @ApiOperation("Call Rainbond API")
    @PostMapping("/rainbond/call")
    public R<AdminRainbondApiResultVO> callRainbondApi(@RequestBody AdminRainbondApiCallDTO dto) {
        return R.ok(adminOpsService.callRainbondApi(dto));
    }
}
