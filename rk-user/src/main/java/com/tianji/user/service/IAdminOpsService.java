package com.tianji.user.service;

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
import com.tianji.user.domain.vo.adminops.AdminBackupOverviewVO;
import com.tianji.user.domain.vo.adminops.AdminDeployPackageDetailVO;
import com.tianji.user.domain.vo.adminops.AdminDeployPackageDiagnosisVO;
import com.tianji.user.domain.vo.adminops.AdminDeployPackageKubeconfigVO;
import com.tianji.user.domain.vo.adminops.AdminDeployPackagePreflightVO;
import com.tianji.user.domain.vo.adminops.AdminDeployPackageRecordVO;
import com.tianji.user.domain.vo.adminops.AdminDeployPackageRepairResultVO;
import com.tianji.user.domain.vo.adminops.AdminApiWorkbenchDocVO;
import com.tianji.user.domain.vo.adminops.AdminApiWorkbenchResourceVO;
import com.tianji.user.domain.vo.adminops.AdminJenkinsBuildRecordVO;
import com.tianji.user.domain.vo.adminops.AdminJenkinsBuildStatusVO;
import com.tianji.user.domain.vo.adminops.AdminJenkinsBuildTriggerVO;
import com.tianji.user.domain.vo.adminops.AdminJenkinsGitBranchesVO;
import com.tianji.user.domain.vo.adminops.AdminJenkinsGitSourceVO;
import com.tianji.user.domain.vo.adminops.AdminK8sCleanupResultVO;
import com.tianji.user.domain.vo.adminops.AdminJenkinsOverviewVO;
import com.tianji.user.domain.vo.adminops.AdminK8sActionResultVO;
import com.tianji.user.domain.vo.adminops.AdminK8sAbnormalPodVO;
import com.tianji.user.domain.vo.adminops.AdminK8sOverviewVO;
import com.tianji.user.domain.vo.adminops.AdminK8sImageVO;
import com.tianji.user.domain.vo.adminops.AdminK8sMaintenanceResultVO;
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
import com.tianji.user.domain.vo.adminops.AdminTaskOverviewVO;
import com.tianji.user.domain.vo.adminops.AdminTopologyVO;
import com.tianji.user.domain.vo.adminops.AdminTrafficLocationVO;
import com.tianji.user.domain.vo.adminops.AdminTrafficOnlineUserVO;
import com.tianji.user.domain.vo.adminops.AdminTrafficOverviewVO;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public interface IAdminOpsService {

    AdminMonitoringOverviewVO getMonitoringOverview();

    AdminMonitoringLogVO getMonitoringLogs(String namespace, String podName, String containerName, Integer tailLines);

    AdminTaskOverviewVO getTaskOverview();

    Boolean pauseTask(Long taskId);

    Boolean resumeTask(Long taskId);

    Boolean triggerTask(Long taskId);

    AdminJenkinsOverviewVO getJenkinsOverview();

    List<AdminJenkinsGitSourceVO> getJenkinsGitSources();

    AdminJenkinsGitBranchesVO getJenkinsGitBranches(String source);

    AdminJenkinsBuildTriggerVO triggerJenkinsJob(String jobName, AdminJenkinsBuildRequestDTO dto);

    AdminJenkinsBuildStatusVO getJenkinsBuildStatus(String jobName, String queueId, Integer buildNumber, Integer tailLines);

    List<AdminJenkinsBuildRecordVO> getJenkinsBuildHistory(String serviceCode, String branchName, String gitSource,
                                                           String status, String operatorKeyword, String startTime,
                                                           String endTime);

    List<AdminOpsServiceVO> listServiceRegistry();

    AdminOpsServiceVO saveServiceRegistry(AdminOpsServiceSaveDTO dto);

    Boolean deleteServiceRegistry(Long id);

    List<AdminNacosConfigVO> listNacosConfigs(String namespaceId, String groupName);

    AdminNacosConfigVO getNacosConfig(String namespaceId, String groupName, String dataId);

    List<AdminNacosConfigHistoryVO> getNacosConfigHistory(String namespaceId, String groupName, String dataId);

    Boolean saveNacosConfig(AdminNacosConfigSaveDTO dto);

    List<AdminOpsAuditLogVO> listOperationAuditLogs(String operationType, String targetName, String action,
                                                    String status, Integer limit);

    List<AdminApiWorkbenchResourceVO> getApiWorkbenchResources();

    AdminApiWorkbenchDocVO getApiWorkbenchDoc(String resourceUrl);

    AdminBackupOverviewVO getBackupOverview();

    Map<String, List<String>> getBackupTables(List<String> databases);

    AdminBackupOverviewVO createBackup(AdminBackupCreateDTO dto);

    AdminBackupOverviewVO saveBackupSchedule(AdminBackupScheduleDTO dto);

    byte[] downloadBackup(Long backupId);

    Boolean deleteBackup(Long backupId);

    List<AdminDeployPackageRecordVO> listDeployPackages();

    AdminDeployPackageDetailVO getDeployPackageDetail(Long packageId);

    AdminDeployPackageKubeconfigVO getDeployPackageKubeconfig(Long packageId);

    AdminDeployPackageDiagnosisVO diagnoseDeployPackage(Long packageId);

    AdminDeployPackageRepairResultVO repairDeployPackage(Long packageId, AdminDeployPackageRepairDTO dto);

    AdminDeployPackageRecordVO createDeployPackage(AdminDeployPackageCreateDTO dto);

    AdminDeployPackageRecordVO createRemoteDeploy(AdminDeployPackageCreateDTO dto);

    AdminDeployPackagePreflightVO preflightDeployPackage(AdminDeployPackageCreateDTO dto);

    List<AdminDeployPackageRecordVO> listRemoteMigrations();

    AdminDeployPackageDetailVO getRemoteMigrationDetail(Long migrationId);

    AdminDeployPackageKubeconfigVO getRemoteMigrationKubeconfig(Long migrationId);

    AdminDeployPackageDiagnosisVO diagnoseRemoteMigration(Long migrationId);

    AdminDeployPackageRepairResultVO repairRemoteMigration(Long migrationId, AdminDeployPackageRepairDTO dto);

    AdminDeployPackageRecordVO createRemoteMigration(AdminDeployPackageCreateDTO dto);

    AdminDeployPackagePreflightVO preflightRemoteMigration(AdminDeployPackageCreateDTO dto);

    AdminDeployPackageRecordVO createLinuxSshMigration(AdminDeployPackageCreateDTO dto);

    AdminDeployPackageRecordVO getActiveGlobalMigration();

    AdminDeployPackageRecordVO getPublicActiveGlobalMigration();

    void streamDeployPackage(Long packageId, OutputStream outputStream);

    void streamLinuxSshDeployPackage(Long packageId, String token, OutputStream outputStream);

    List<AdminMinioBucketVO> listMinioBuckets();

    List<AdminMinioObjectVO> listMinioObjects(String bucket, String prefix, Boolean recursive, Integer limit);

    void streamMinioObject(String bucket, String objectName, OutputStream outputStream);

    AdminK8sOverviewVO getK8sOverview();

    AdminK8sCleanupResultVO runK8sCleanup(AdminK8sCleanupRequestDTO dto);

    List<AdminK8sImageVO> listK8sImages(String nodeName, String keyword, Boolean unusedOnly);

    AdminK8sCleanupResultVO cleanupK8sImages(AdminK8sImageCleanupDTO dto);

    byte[] downloadK8sImageExportScript(String nodeName, String nodeIp, String image);

    List<AdminK8sAbnormalPodVO> listK8sAbnormalPods(List<String> namespaces, List<String> statuses);

    AdminK8sMaintenanceResultVO cleanupK8sAbnormalPods(AdminK8sAbnormalPodCleanupDTO dto);

    AdminK8sMaintenanceResultVO runK8sClusterMaintenance(AdminK8sClusterMaintenanceDTO dto);

    Map<String, Object> ensureK8sMaintenanceXxlJob(AdminK8sClusterMaintenanceDTO dto);

    List<AdminK8sWorkloadVO> getK8sWorkloads(String namespace);

    AdminK8sActionResultVO runK8sWorkloadAction(AdminK8sWorkloadActionDTO dto);

    AdminK8sPodExecResultVO runK8sPodExec(AdminK8sPodExecRequestDTO dto);

    AdminTopologyVO getServiceTopology(String namespace);

    AdminRainbondConfigVO getRainbondConfig();

    AdminRainbondConfigVO saveRainbondConfig(AdminRainbondConfigDTO dto);

    Map<String, Object> getRainbondCatalog();

    Map<String, Object> getRainbondDiscovery();

    AdminRainbondApiResultVO callRainbondApi(AdminRainbondApiCallDTO dto);

    AdminTrafficOverviewVO getTrafficOverview(Long tenantId, String startTime, String endTime);

    AdminTrafficOverviewVO getTrafficDrilldown(Long tenantId, String country, String province, String startTime, String endTime);

    List<AdminTrafficOnlineUserVO> getTrafficOnlineUsers(Long tenantId, Integer windowMinutes);

    AdminTrafficLocationVO getTrafficCurrentOrigin();

    AdminTrafficOverviewVO getTrafficDefaultTenant();

    AdminTrafficOverviewVO saveTrafficDefaultTenant(AdminTrafficDefaultTenantDTO dto);

    Map<String, Object> getFrontendCacheOverview();

    Map<String, Object> warmupFrontendCache();

    Map<String, Object> clearFrontendCache();

    Map<String, Object> getVisualScreenOverview();
}
