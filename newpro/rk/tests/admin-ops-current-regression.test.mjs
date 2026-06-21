import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

async function readSource(relativePath) {
  return readFile(new URL(relativePath, import.meta.url), 'utf8')
}

test('service monitor should use backend monitoring data and expose instance details', async () => {
  const source = await readSource('../src/views/admin/operation/ServiceMonitor.vue')

  assert.equal(source.includes('getMonitoringOverview'), true)
  assert.equal(source.includes('type="expand"'), true)
  assert.equal(source.includes('instanceDetails'), true)
  assert.equal(source.includes('/auth/login'), false)
})

test('admin dashboard should tolerate slow content APIs without showing a page-level timeout', async () => {
  const source = await readSource('../src/views/admin/Dashboard.vue')

  assert.equal(source.includes('Promise.allSettled'), true)
  assert.equal(source.includes('withDashboardFallback'), true)
  assert.equal(source.includes('getNewsList({ page: 1, size: 5 })'), true)
  assert.equal(source.includes('getActivityPage({ page: 1, size: 5 })'), true)
})

test('backup page should show current table selections and backup progress', async () => {
  const source = await readSource('../src/views/admin/operation/Backup.vue')
  const apiSource = await readSource('../src/api/admin-ops.js')

  assert.equal(source.includes('backupProgress'), true)
  assert.equal(source.includes('tableSummary'), true)
  assert.equal(source.includes('manualSelectedTables'), true)
  assert.equal(source.includes('downloadBackupFile'), true)
  assert.equal(source.includes('URL.createObjectURL'), true)
  assert.equal(source.includes('window.open(getBackupDownloadUrl'), false)
  assert.equal(apiSource.includes('responseType: \'blob\''), true)
})

test('manual backup should open a dedicated selection dialog instead of reusing schedule scope', async () => {
  const source = await readSource('../src/views/admin/operation/Backup.vue')

  assert.equal(source.includes('showManualBackup'), true)
  assert.equal(source.includes('manualBackup.databases'), true)
  assert.equal(source.includes('openManualBackupDialog'), true)
  assert.equal(source.includes('databases: manualBackup.value.databases'), true)
})

test('admin routes and menu should expose an internal Jenkins console', async () => {
  const routerSource = await readSource('../src/router/index.js')
  const layoutSource = await readSource('../src/layouts/AdminLayout.vue')

  assert.equal(routerSource.includes("path: 'operation/jenkins'"), true)
  assert.equal(routerSource.includes('Jenkins'), true)
  assert.equal(layoutSource.includes('/admin/operation/jenkins'), true)
  assert.equal(layoutSource.includes('ensureOperationMenuEntries'), true)
})

test('jenkins console should document trigger modes for local and registry deployment', async () => {
  const source = await readSource('../src/views/admin/operation/Jenkins.vue')

  assert.equal(source.includes('触发构建教程'), true)
  assert.equal(source.includes('RK_K8S_IMAGE_MODE=local'), true)
  assert.equal(source.includes('RK_K8S_IMAGE_MODE=registry'), true)
  assert.equal(source.includes('阿里云镜像仓库'), true)
  assert.equal(source.includes('SERVICES'), true)
})

test('jenkins console should expose parameterized trigger progress and log polling', async () => {
  const source = await readSource('../src/views/admin/operation/Jenkins.vue')
  const apiSource = await readSource('../src/api/admin-ops.js')

  assert.equal(source.includes('buildForm.imageMode'), true)
  assert.equal(source.includes('buildStatus.logTail'), true)
  assert.equal(source.includes('preflightSummary'), true)
  assert.equal(source.includes('startBuildPolling'), true)
  assert.equal(source.includes('reconcileActiveBuildFromHistory'), true)
  assert.equal(source.includes('normalizeBuildStatus'), true)
  assert.equal(apiSource.includes('getJenkinsBuildStatus'), true)
  assert.equal(apiSource.includes('/build-status'), true)
})

test('jenkins backend should persist preflight disk snapshots with build records', async () => {
  const schema = await readSource('../../../rk-user/src/main/java/com/tianji/user/config/AdminOpsSchemaUpdater.java')
  const triggerVo = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/vo/adminops/AdminJenkinsBuildTriggerVO.java')
  const recordVo = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/vo/adminops/AdminJenkinsBuildRecordVO.java')
  const service = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  assert.equal(schema.includes('preflight_summary'), true)
  assert.equal(triggerVo.includes('preflightSummary'), true)
  assert.equal(recordVo.includes('preflightSummary'), true)
  assert.equal(service.includes('captureJenkinsPreflightSnapshot'), true)
  assert.equal(service.includes('runRemoteCommandLenient'), true)
  assert.equal(service.includes('PREFLIGHT_DISK'), true)
})

test('jenkins backend should reconcile stale running build history before listing', async () => {
  const service = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  assert.equal(service.includes('reconcileRecentJenkinsBuildRecords'), true)
  assert.equal(service.includes("status IN ('queued', 'running')"), true)
  assert.equal(service.includes('fillJenkinsBuildDetail'), true)
  assert.equal(service.includes('persistJenkinsBuildStatus'), true)
})

test('jenkins backend should recover expired queue items from recent job builds', async () => {
  const service = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  assert.equal(service.includes('findRecentJenkinsBuildNumberByQueueId'), true)
  assert.equal(service.includes('builds[number,queueId,actions[causes[queueId],parameters[name,value]]]'), true)
  assert.equal(service.includes('matchesJenkinsQueueId'), true)
  assert.equal(service.includes('findJenkinsBuildNumberByQueueId(jobName, queueId, client)'), true)
})

test('jenkins console should select git source and load branches dynamically', async () => {
  const source = await readSource('../src/views/admin/operation/Jenkins.vue')
  const apiSource = await readSource('../src/api/admin-ops.js')
  const branchVoSource = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/vo/adminops/AdminJenkinsGitBranchesVO.java')

  assert.equal(source.includes('代码仓库'), true)
  assert.equal(source.includes('本地 Gogs'), true)
  assert.equal(source.includes('Gitee'), true)
  assert.equal(source.includes('buildForm.gitSource'), true)
  assert.equal(source.includes('branchOptions'), true)
  assert.equal(source.includes('branchFallback'), true)
  assert.equal(source.includes('res.data.fallback'), true)
  assert.equal(branchVoSource.includes('private Boolean fallback'), true)
  assert.equal(branchVoSource.includes('private String failureReason'), true)
  assert.equal(source.includes('handleGitSourceChange'), true)
  assert.equal(source.includes('getJenkinsGitSources'), true)
  assert.equal(source.includes('getJenkinsGitBranches'), true)
  assert.equal(apiSource.includes('/admin/ops/jenkins/git-sources'), true)
  assert.equal(apiSource.includes('/admin/ops/jenkins/git-branches'), true)
})

test('jenkins build history should expose operator branch and time range filters', async () => {
  const source = await readSource('../src/views/admin/operation/Jenkins.vue')
  const apiSource = await readSource('../src/api/admin-ops.js')
  const controllerSource = await readSource('../../../rk-user/src/main/java/com/tianji/user/controller/AdminOpsController.java')

  assert.equal(source.includes('historyFilters.branchName'), true)
  assert.equal(source.includes('historyFilters.operatorKeyword'), true)
  assert.equal(source.includes('historyFilters.timeRange'), true)
  assert.equal(source.includes('startTime'), true)
  assert.equal(source.includes('endTime'), true)
  assert.equal(apiSource.includes('operatorKeyword'), true)
  assert.equal(apiSource.includes('startTime'), true)
  assert.equal(apiSource.includes('endTime'), true)
  assert.equal(controllerSource.includes('@RequestParam(required = false) String operatorKeyword'), true)
  assert.equal(controllerSource.includes('@RequestParam(required = false) String startTime'), true)
  assert.equal(controllerSource.includes('@RequestParam(required = false) String endTime'), true)
})

test('k8s page should expose node filtered cleanup console backed by API', async () => {
  const pageSource = await readSource('../src/views/admin/operation/K8s.vue')
  const apiSource = await readSource('../src/api/admin-ops.js')

  assert.equal(pageSource.includes('cleanupForm.nodeIp'), true)
  assert.equal(pageSource.includes('cleanupForm.reason'), true)
  assert.equal(pageSource.includes('confirmText'), true)
  assert.equal(pageSource.includes('IMAGE_PRUNE'), true)
  assert.equal(pageSource.includes('LOG_CLEAN'), true)
  assert.equal(pageSource.includes('runK8sCleanup'), true)
  assert.equal(apiSource.includes('listOpsAuditLogs'), true)
  assert.equal(apiSource.includes('runK8sCleanup'), true)
  assert.equal(apiSource.includes('/admin/ops/k8s/cleanup'), true)
})

test('k8s workload actions should require confirmation and surface operation audit logs', async () => {
  const pageSource = await readSource('../src/views/admin/operation/K8s.vue')
  const apiSource = await readSource('../src/api/admin-ops.js')
  const dtoSource = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/dto/adminops/AdminK8sWorkloadActionDTO.java')
  const resultSource = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/vo/adminops/AdminK8sActionResultVO.java')
  const schemaSource = await readSource('../../../rk-user/src/main/java/com/tianji/user/config/AdminOpsSchemaUpdater.java')

  assert.equal(pageSource.includes('dangerConfirmText'), true)
  assert.equal(pageSource.includes('auditLogs'), true)
  assert.equal(pageSource.includes('fetchAuditLogs'), true)
  assert.equal(apiSource.includes('/admin/ops/audit-logs'), true)
  assert.equal(dtoSource.includes('confirmText'), true)
  assert.equal(dtoSource.includes('reason'), true)
  assert.equal(resultSource.includes('auditId'), true)
  assert.equal(schemaSource.includes('ops_operation_audit'), true)
})

test('nacos config page should expose history and save error details', async () => {
  const pageSource = await readSource('../src/views/admin/operation/NacosConfig.vue')
  const apiSource = await readSource('../src/api/admin-ops.js')
  const serviceSource = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/IAdminOpsService.java')

  assert.equal(pageSource.includes('configHistory'), true)
  assert.equal(pageSource.includes('fetchConfigHistory'), true)
  assert.equal(pageSource.includes('saveError'), true)
  assert.equal(apiSource.includes('getNacosConfigHistory'), true)
  assert.equal(apiSource.includes('/admin/ops/nacos/config-history'), true)
  assert.equal(serviceSource.includes('getNacosConfigHistory'), true)
})

test('topology page should provide click-through links to logs config and build history', async () => {
  const pageSource = await readSource('../src/views/admin/operation/Topology.vue')
  const serviceSource = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  assert.equal(pageSource.includes('openNodeLink'), true)
  assert.equal(pageSource.includes('node.links'), true)
  assert.equal(pageSource.includes('buildHistory'), true)
  assert.equal(pageSource.includes('configCenter'), true)
  assert.equal(pageSource.includes('logs'), true)
  assert.equal(serviceSource.includes('topologyLinks'), true)
})

test('backend runtime image should install git through a domestic apt mirror with retries', async () => {
  const dockerfile = await readSource('../../../ci/docker/backend/Dockerfile')

  assert.match(dockerfile, /mirrors\.aliyun\.com/)
  assert.match(dockerfile, /sources\.list\.d\/\*\.sources/)
  assert.match(dockerfile, /Acquire::Retries=5/)
  assert.match(dockerfile, /Acquire::ForceIPv4=true/)
  assert.match(dockerfile, /Acquire::http::Timeout=20/)
  assert.match(dockerfile, /apt-get\s+install[\s\S]*\bgit\b/)
})

test('jenkins branch discovery should use an in-cluster Gogs URL separate from checkout URL', async () => {
  const serviceSource = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  assert.equal(serviceSource.includes('jenkinsGitLocalBranchUrl'), true)
  assert.equal(serviceSource.includes('rk.ops.jenkins.git.local-branch-url:http://rk-gogs:3000/tjxt/rk-web.git'), true)
  assert.equal(serviceSource.includes('resolveJenkinsGitBranchRepoUrl'), true)
})

test('monitoring center should expose pod log viewer backed by ops API', async () => {
  const pageSource = await readSource('../src/views/admin/operation/Monitoring.vue')
  const apiSource = await readSource('../src/api/admin-ops.js')
  const controllerSource = await readSource('../../../rk-user/src/main/java/com/tianji/user/controller/AdminOpsController.java')
  const serviceSource = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/IAdminOpsService.java')

  assert.equal(pageSource.includes('handleViewLogs'), true)
  assert.equal(pageSource.includes('logDialogVisible'), true)
  assert.equal(pageSource.includes('getMonitoringLogs'), true)
  assert.equal(apiSource.includes('/admin/ops/monitoring/logs'), true)
  assert.equal(controllerSource.includes('/monitoring/logs'), true)
  assert.equal(serviceSource.includes('getMonitoringLogs'), true)
})

test('traffic center should expose backend endpoints, frontend APIs and admin route', async () => {
  const apiSource = await readSource('../src/api/admin-ops.js')
  const routerSource = await readSource('../src/router/index.js')
  const layoutSource = await readSource('../src/layouts/AdminLayout.vue')
  const controllerSource = await readSource('../../../rk-user/src/main/java/com/tianji/user/controller/AdminOpsController.java')
  const serviceSource = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/IAdminOpsService.java')

  assert.equal(controllerSource.includes('/traffic/overview'), true)
  assert.equal(controllerSource.includes('/traffic/drilldown'), true)
  assert.equal(controllerSource.includes('/traffic/online-users'), true)
  assert.equal(controllerSource.includes('/traffic/default-tenant'), true)
  assert.equal(serviceSource.includes('getTrafficOverview'), true)
  assert.equal(serviceSource.includes('getTrafficOnlineUsers'), true)
  assert.equal(serviceSource.includes('saveTrafficDefaultTenant'), true)
  assert.equal(apiSource.includes('getTrafficOverview'), true)
  assert.equal(apiSource.includes('getTrafficDrilldown'), true)
  assert.equal(apiSource.includes('getTrafficOnlineUsers'), true)
  assert.equal(apiSource.includes('getTrafficDefaultTenant'), true)
  assert.equal(apiSource.includes('saveTrafficDefaultTenant'), true)
  assert.equal(routerSource.includes("path: 'operation/traffic-center'"), true)
  assert.equal(routerSource.includes('TrafficCenter.vue'), true)
  assert.equal(layoutSource.includes('/admin/operation/traffic-center'), true)
  assert.equal(layoutSource.includes("t('admin.trafficCenter')"), true)
})
