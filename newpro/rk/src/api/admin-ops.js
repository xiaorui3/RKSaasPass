import request from '@/utils/request'

export function getMonitoringOverview() {
  return request({
    url: '/admin/ops/monitoring/overview',
    method: 'get'
  })
}

export function getMonitoringLogs(params = {}) {
  return request({
    url: '/admin/ops/monitoring/logs',
    method: 'get',
    params
  })
}

export function getTaskOverview() {
  return request({
    url: '/admin/ops/tasks/overview',
    method: 'get'
  })
}

export function pauseTask(taskId) {
  return request({
    url: `/admin/ops/tasks/${taskId}/pause`,
    method: 'post'
  })
}

export function resumeTask(taskId) {
  return request({
    url: `/admin/ops/tasks/${taskId}/resume`,
    method: 'post'
  })
}

export function triggerTask(taskId) {
  return request({
    url: `/admin/ops/tasks/${taskId}/trigger`,
    method: 'post'
  })
}

export function getJenkinsOverview() {
  return request({
    url: '/admin/ops/jenkins/overview',
    method: 'get'
  })
}

export function getJenkinsGitSources() {
  return request({
    url: '/admin/ops/jenkins/git-sources',
    method: 'get'
  })
}

export function getJenkinsGitBranches(source = 'local') {
  return request({
    url: '/admin/ops/jenkins/git-branches',
    method: 'get',
    params: { source }
  })
}

export function triggerJenkinsJob(jobName, data = {}) {
  return request({
    url: `/admin/ops/jenkins/jobs/${encodeURIComponent(jobName)}/trigger`,
    method: 'post',
    data
  })
}

export function getJenkinsBuildStatus(jobName, params = {}) {
  return request({
    url: `/admin/ops/jenkins/jobs/${encodeURIComponent(jobName)}/build-status`,
    method: 'get',
    params
  })
}

export function getJenkinsBuildHistory(params = {}) {
  const query = {
    serviceCode: params.serviceCode,
    branchName: params.branchName,
    gitSource: params.gitSource,
    status: params.status,
    operatorKeyword: params.operatorKeyword,
    startTime: params.startTime,
    endTime: params.endTime
  }
  if (Array.isArray(params.timeRange)) {
    const [startTime, endTime] = params.timeRange
    query.startTime = query.startTime || startTime
    query.endTime = query.endTime || endTime
  }
  return request({
    url: '/admin/ops/jenkins/build-history',
    method: 'get',
    params: query
  })
}

export function listOpsServices() {
  return request({
    url: '/admin/ops/jenkins/services',
    method: 'get'
  })
}

export function listReleaseCenterServices(params = {}) {
  return request({
    url: '/admin/ops/release-center/services',
    method: 'get',
    params
  })
}

export function listReleaseCenterVersions(serviceCode) {
  return request({
    url: `/admin/ops/release-center/services/${encodeURIComponent(serviceCode)}/versions`,
    method: 'get'
  })
}

export function publishReleaseCurrentImage(serviceCode, data = {}) {
  return request({
    url: `/admin/ops/release-center/services/${encodeURIComponent(serviceCode)}/publish-current`,
    method: 'post',
    data
  })
}

export function triggerReleaseServiceBuild(serviceCode, data = {}) {
  return request({
    url: `/admin/ops/release-center/services/${encodeURIComponent(serviceCode)}/build`,
    method: 'post',
    data
  })
}

export function deployReleaseServiceVersion(serviceCode, data = {}) {
  return request({
    url: `/admin/ops/release-center/services/${encodeURIComponent(serviceCode)}/deploy`,
    method: 'post',
    data
  })
}

export function buildReleaseUpdateManifest(data = {}) {
  return request({
    url: '/admin/ops/release-center/updates/manifest',
    method: 'post',
    data
  })
}

export function applyReleaseUpdateManifest(data = {}) {
  return request({
    url: '/admin/ops/release-center/updates/apply',
    method: 'post',
    data
  })
}

export function getReleaseCenterTask(taskId) {
  return request({
    url: `/admin/ops/release-center/tasks/${taskId}`,
    method: 'get'
  })
}

export function saveOpsService(data) {
  return request({
    url: '/admin/ops/jenkins/services',
    method: 'post',
    data
  })
}

export function deleteOpsService(id) {
  return request({
    url: `/admin/ops/jenkins/services/${id}/delete`,
    method: 'post'
  })
}

export function listNacosConfigs(params = {}) {
  return request({
    url: '/admin/ops/nacos/configs',
    method: 'get',
    params
  })
}

export function getNacosConfig(params = {}) {
  return request({
    url: '/admin/ops/nacos/config',
    method: 'get',
    params
  })
}

export function getNacosConfigHistory(params = {}) {
  return request({
    url: '/admin/ops/nacos/config-history',
    method: 'get',
    params
  })
}

export function saveNacosConfig(data) {
  return request({
    url: '/admin/ops/nacos/configs',
    method: 'post',
    data
  })
}

export function getApiWorkbenchResources() {
  return request({
    url: '/admin/ops/api-workbench/resources',
    method: 'get'
  })
}

export function getApiWorkbenchDoc(url) {
  return request({
    url: '/admin/ops/api-workbench/docs',
    method: 'get',
    params: { url }
  })
}

export function getBackupOverview() {
  return request({
    url: '/admin/ops/backup/overview',
    method: 'get'
  })
}

export function getBackupTables(databases = []) {
  return request({
    url: '/admin/ops/backup/tables',
    method: 'get',
    params: { databases }
  })
}

export function createBackup(data) {
  return request({
    url: '/admin/ops/backup/create',
    method: 'post',
    data
  })
}

export function saveBackupSchedule(data) {
  return request({
    url: '/admin/ops/backup/schedule',
    method: 'post',
    data
  })
}

export function deleteBackup(backupId) {
  return request({
    url: `/admin/ops/backup/${backupId}/delete`,
    method: 'post'
  })
}

export function getBackupDownloadUrl(backupId) {
  return `/admin/ops/backup/${backupId}/download`
}

export function downloadBackupFile(backupId) {
  return request({
    url: getBackupDownloadUrl(backupId),
    method: 'get',
    responseType: 'blob'
  })
}

export function listDeployPackages() {
  return request({
    url: '/admin/ops/deploy-package/records',
    method: 'get'
  })
}

export function getDeployPackageDetail(id) {
  return request({
    url: `/admin/ops/deploy-package/${id}`,
    method: 'get'
  })
}

export function getDeployPackageKubeconfig(id) {
  return request({
    url: `/admin/ops/deploy-package/${id}/kubeconfig`,
    method: 'get'
  })
}

export function diagnoseDeployPackage(id) {
  return request({
    url: `/admin/ops/deploy-package/${id}/diagnose`,
    method: 'post'
  })
}

export function repairDeployPackage(id, data = {}) {
  return request({
    url: `/admin/ops/deploy-package/${id}/repair`,
    method: 'post',
    data
  })
}

export function createDeployPackage(data) {
  return request({
    url: '/admin/ops/deploy-package/create',
    method: 'post',
    data
  })
}

export function createRemoteDeploy(data) {
  return request({
    url: '/admin/ops/deploy-package/remote-deploy',
    method: 'post',
    data
  })
}

export function preflightDeployPackage(data) {
  return request({
    url: '/admin/ops/deploy-package/preflight',
    method: 'post',
    data
  })
}

export function listRemoteMigrations() {
  return request({
    url: '/admin/ops/remote-migration/records',
    method: 'get'
  })
}

export function getRemoteMigrationDetail(id) {
  return request({
    url: `/admin/ops/remote-migration/${id}`,
    method: 'get'
  })
}

export function getRemoteMigrationKubeconfig(id) {
  return request({
    url: `/admin/ops/remote-migration/${id}/kubeconfig`,
    method: 'get'
  })
}

export function diagnoseRemoteMigration(id) {
  return request({
    url: `/admin/ops/remote-migration/${id}/diagnose`,
    method: 'post'
  })
}

export function repairRemoteMigration(id, data = {}) {
  return request({
    url: `/admin/ops/remote-migration/${id}/repair`,
    method: 'post',
    data
  })
}

export function createRemoteMigration(data) {
  return request({
    url: '/admin/ops/remote-migration/kubeconfig-deploy',
    method: 'post',
    data
  })
}

export function preflightRemoteMigration(data) {
  return request({
    url: '/admin/ops/remote-migration/preflight',
    method: 'post',
    data
  })
}

export function createLinuxSshMigration(data) {
  return request({
    url: '/admin/ops/remote-migration/linux-ssh-deploy',
    method: 'post',
    data
  })
}

export function getActiveGlobalMigration() {
  return request({
    url: '/api/ops/migration/active',
    method: 'get',
    silentError: true,
    showLoginOn401: false
  })
}

export function getDeployPackageDownloadUrl(packageId) {
  return `/admin/ops/deploy-package/${packageId}/download`
}

export function downloadDeployPackage(packageId) {
  return request({
    url: getDeployPackageDownloadUrl(packageId),
    method: 'get',
    responseType: 'blob'
  })
}

export function listMinioBuckets() {
  return request({
    url: '/admin/ops/minio/buckets',
    method: 'get'
  })
}

export function listMinioObjects(params = {}) {
  return request({
    url: '/admin/ops/minio/objects',
    method: 'get',
    params
  })
}

export function getMinioObjectDownloadUrl(bucket, objectName) {
  const query = new URLSearchParams({
    bucket: bucket || '',
    object: objectName || ''
  })
  return `/admin/ops/minio/object/download?${query.toString()}`
}

export function downloadMinioObject(bucket, objectName) {
  return request({
    url: getMinioObjectDownloadUrl(bucket, objectName),
    method: 'get',
    responseType: 'blob'
  })
}

export function getK8sOverview() {
  return request({
    url: '/admin/ops/k8s/overview',
    method: 'get'
  })
}

export function runK8sCleanup(data) {
  return request({
    url: '/admin/ops/k8s/cleanup',
    method: 'post',
    data
  })
}

export function listK8sImages(params = {}) {
  return request({
    url: '/admin/ops/k8s/images',
    method: 'get',
    params
  })
}

export function cleanupK8sImages(data) {
  return request({
    url: '/admin/ops/k8s/images/cleanup',
    method: 'post',
    data
  })
}

export function getK8sImageExportScriptUrl(params = {}) {
  const query = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      query.set(key, value)
    }
  })
  return `/admin/ops/k8s/images/export-script?${query.toString()}`
}

export function downloadK8sImageExportScript(params = {}) {
  return request({
    url: getK8sImageExportScriptUrl(params),
    method: 'get',
    responseType: 'blob'
  })
}

export function listK8sAbnormalPods(params = {}) {
  return request({
    url: '/admin/ops/k8s/abnormal-pods',
    method: 'get',
    params
  })
}

export function cleanupK8sAbnormalPods(data) {
  return request({
    url: '/admin/ops/k8s/abnormal-pods/cleanup',
    method: 'post',
    data
  })
}

export function runK8sClusterMaintenance(data) {
  return request({
    url: '/admin/ops/k8s/maintenance/run',
    method: 'post',
    data
  })
}

export function ensureK8sMaintenanceSchedule(data = {}) {
  return request({
    url: '/admin/ops/k8s/maintenance/schedule',
    method: 'post',
    data
  })
}

export function getK8sWorkloads(params = {}) {
  return request({
    url: '/admin/ops/k8s/workloads',
    method: 'get',
    params
  })
}

export function runK8sWorkloadAction(data) {
  return request({
    url: '/admin/ops/k8s/workloads/action',
    method: 'post',
    data
  })
}

export function runK8sPodExec(data) {
  return request({
    url: '/admin/ops/k8s/pods/exec',
    method: 'post',
    data
  })
}

export function getRainbondConfig() {
  return request({
    url: '/admin/ops/rainbond/config',
    method: 'get'
  })
}

export function saveRainbondConfig(data) {
  return request({
    url: '/admin/ops/rainbond/config',
    method: 'post',
    data
  })
}

export function getRainbondCatalog() {
  return request({
    url: '/admin/ops/rainbond/catalog',
    method: 'get'
  })
}

export function getRainbondDiscovery() {
  return request({
    url: '/admin/ops/rainbond/discovery',
    method: 'get'
  })
}

export function callRainbondApi(data) {
  return request({
    url: '/admin/ops/rainbond/call',
    method: 'post',
    data
  })
}

export function listOpsAuditLogs(params = {}) {
  return request({
    url: '/admin/ops/audit-logs',
    method: 'get',
    params
  })
}

export function getOpsTopology(params = {}) {
  return request({
    url: '/admin/ops/topology',
    method: 'get',
    params
  })
}

export function getTrafficOverview(params = {}) {
  return request({
    url: '/admin/ops/traffic/overview',
    method: 'get',
    params
  })
}

export function getTrafficDrilldown(params = {}) {
  return request({
    url: '/admin/ops/traffic/drilldown',
    method: 'get',
    params
  })
}

export function getTrafficOnlineUsers(params = {}) {
  return request({
    url: '/admin/ops/traffic/online-users',
    method: 'get',
    params
  })
}

export function getTrafficCurrentOrigin() {
  return request({
    url: '/admin/ops/traffic/current-origin',
    method: 'get'
  })
}

export function getTrafficDefaultTenant() {
  return request({
    url: '/admin/ops/traffic/default-tenant',
    method: 'get'
  })
}

export function saveTrafficDefaultTenant(data) {
  return request({
    url: '/admin/ops/traffic/default-tenant',
    method: 'post',
    data
  })
}

export function getFrontendCacheOverview() {
  return request({
    url: '/admin/ops/frontend-cache/overview',
    method: 'get'
  })
}

export function warmupFrontendCache(data = {}) {
  return request({
    url: '/admin/ops/frontend-cache/warmup',
    method: 'post',
    data
  })
}

export function clearFrontendCache(data = {}) {
  return request({
    url: '/admin/ops/frontend-cache/clear',
    method: 'post',
    data
  })
}

export function getVisualScreenOverview(params = {}) {
  return request({
    url: '/admin/ops/visual-screen/overview',
    method: 'get',
    params
  })
}
