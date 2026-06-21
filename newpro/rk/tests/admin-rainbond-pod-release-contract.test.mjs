import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

async function readSource(relativePath) {
  return readFile(new URL(relativePath, import.meta.url), 'utf8')
}

test('backend admin ops should expose pod exec and Rainbond admin APIs', async () => {
  const controller = await readSource('../../../rk-user/src/main/java/com/tianji/user/controller/AdminOpsController.java')
  const service = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/IAdminOpsService.java')
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  assert.equal(controller.includes('/k8s/pods/exec'), true)
  assert.equal(controller.includes('runK8sPodExec'), true)
  assert.equal(service.includes('runK8sPodExec'), true)
  assert.equal(impl.includes('sendK8sExecRequest'), true)

  assert.equal(controller.includes('/rainbond/config'), true)
  assert.equal(controller.includes('/rainbond/catalog'), true)
  assert.equal(controller.includes('/rainbond/discovery'), true)
  assert.equal(controller.includes('/rainbond/call'), true)
  assert.equal(service.includes('saveRainbondConfig'), true)
  assert.equal(service.includes('callRainbondApi'), true)
  assert.equal(impl.includes('/openapi/v1/teams'), true)
  assert.equal(impl.includes('/openapi/v2/manage/enterprises'), true)
})

test('pod terminal runtime should avoid slow first-use kubectl bootstrap', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const dockerfile = await readSource('../../../ci/docker/backend/Dockerfile')

  assert.equal(dockerfile.includes('KUBECTL_VERSION'), false)
  assert.equal(dockerfile.includes('/usr/local/bin/kubectl'), true)
  assert.equal(dockerfile.includes('dl.k8s.io'), false)
  assert.match(dockerfile, /COPY kubectl \/usr\/local\/bin\/kubectl/)
  assert.equal(impl.includes('KUBECTL_URL'), false)
  assert.equal(impl.includes('KUBECTL_BOOTSTRAP_TIMEOUT'), false)
  assert.equal(impl.includes('curl --max-time'), false)
  assert.equal(impl.includes('wget -q'), false)
  assert.match(impl, /command -v kubectl/)
})

test('Rainbond catalog should cover every documented API operation without environment hard coding', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const endpointMatches = [...impl.matchAll(/rb\("([^"]+)",\s*"([^"]+)",\s*"([^"]+)",\s*"([^"]+)"/g)]
  const endpoints = endpointMatches.map((match) => ({
    category: match[1],
    name: match[2],
    method: match[3],
    path: match[4]
  }))

  assert.equal(endpoints.length >= 73, true)
  assert.equal(new Set(endpoints.map((item) => item.category)).size, 6)
  assert.deepEqual(
    new Set(endpoints.map((item) => item.category)),
    new Set(['enterprise', 'team', 'region', 'application', 'gateway', 'user'])
  )
  assert.equal(
    endpoints.some((item) =>
      item.category === 'application' &&
      item.name === 'buildHelmApp' &&
      item.method === 'POST' &&
      item.path === '/openapi/v1/teams/{team_id}/regions/{region_name}/apps/{app_id}/helm_chart'
    ),
    true
  )
  assert.equal(
    endpoints.some((item) =>
      item.category === 'application' &&
      item.name === 'createComponent' &&
      item.method === 'POST' &&
      item.path === '/openapi/v1/teams/{team_name}/regions/{region_name}/apps/{group_id}/services'
    ),
    true
  )
  assert.equal(impl.includes('127.0.0.1/openapi'), false)
  assert.equal(impl.includes('rainbond.example.com/openapi'), false)
})

test('frontend should provide K8s pod terminal and Rainbond visual console controls', async () => {
  const api = await readSource('../src/api/admin-ops.js')
  const k8sPage = await readSource('../src/views/admin/operation/K8s.vue')
  const monitoringPage = await readSource('../src/views/admin/operation/Monitoring.vue')
  const serviceMonitorPage = await readSource('../src/views/admin/operation/ServiceMonitor.vue')
  const rainbondConsole = await readSource('../src/views/admin/operation/RainbondConsole.vue')

  assert.equal(api.includes('runK8sPodExec'), true)
  assert.equal(api.includes('/admin/ops/k8s/pods/exec'), true)
  assert.equal(k8sPage.includes('podTerminalForm'), true)
  assert.equal(k8sPage.includes('runK8sPodExec'), true)
  assert.equal(k8sPage.includes('execOutput'), true)
  assert.equal(monitoringPage.includes('runK8sPodExec'), true)
  assert.equal(monitoringPage.includes('handleOpenPodTerminal'), true)
  assert.equal(serviceMonitorPage.includes('runK8sPodExec'), true)
  assert.equal(serviceMonitorPage.includes('handleOpenPodTerminal'), true)

  assert.equal(api.includes('getRainbondConfig'), true)
  assert.equal(api.includes('saveRainbondConfig'), true)
  assert.equal(api.includes('getRainbondCatalog'), true)
  assert.equal(api.includes('getRainbondDiscovery'), true)
  assert.equal(api.includes('callRainbondApi'), true)
  assert.equal(rainbondConsole.includes('resourceLanes'), true)
  assert.equal(rainbondConsole.includes('officialWebVisible'), true)
  assert.equal(rainbondConsole.includes('iframe'), true)
  assert.equal(rainbondConsole.includes('openOfficialWeb'), true)
  assert.equal(rainbondConsole.includes('callResourceAction'), true)
  assert.equal(rainbondConsole.includes('categoryLabel'), true)
  assert.equal(rainbondConsole.includes('enterprise'), true)
  assert.equal(rainbondConsole.includes('team'), true)
  assert.equal(rainbondConsole.includes('region'), true)
  assert.equal(rainbondConsole.includes('application'), true)
  assert.equal(rainbondConsole.includes('gateway'), true)
  assert.equal(rainbondConsole.includes('user'), true)
  assert.equal(rainbondConsole.includes('callRainbondApi'), true)
})

test('one key deploy package should expose self contained packaging and live progress affordances', async () => {
  const dto = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/dto/adminops/AdminDeployPackageCreateDTO.java')
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const page = await readSource('../src/views/admin/operation/DeployPackage.vue')
  const router = await readSource('../src/router/index.js')
  const menuSeed = await readSource('../../../basedata/05_insert_menu_data.sql')

  assert.equal(dto.includes('selfContained'), true)
  assert.equal(dto.includes('targetClusterType'), true)
  assert.equal(dto.includes('targetNamespace'), true)
  assert.equal(dto.includes('targetDomain'), true)
  assert.equal(impl.includes('bundle-mode'), true)
  assert.equal(impl.includes('scripts/package-online.sh'), true)
  assert.equal(impl.includes('scripts/import-databases.sh'), true)
  assert.equal(impl.includes('images/README.md'), true)
  assert.equal(impl.includes('databases/README.md'), true)
  assert.equal(impl.includes('get svc -o wide'), true)
  assert.equal(impl.includes('get ingress -o wide'), true)
  assert.equal(page.includes('selfContained'), true)
  assert.equal(page.includes('targetClusterType'), true)
  assert.equal(page.includes('pollTimer'), true)
  assert.equal(page.includes('refreshRunningRecords'), true)
  assert.equal(page.includes('latestLogLine'), true)
  assert.equal(page.includes('images/*.tar'), true)
  assert.equal(page.includes('databases/*.sql.gz'), true)
  assert.equal(impl.includes('addZipEntry(zip, "images/README.md"'), true)
  assert.equal(impl.includes('addZipEntry(zip, "databases/README.md"'), true)
  assert.equal(router.includes("path: 'operation/rainbond'"), true)
  assert.equal(router.includes("path: 'operation/deploy-package'"), true)
  assert.equal(menuSeed.includes('/admin/operation/rainbond'), true)
  assert.equal(menuSeed.includes('/admin/operation/deploy-package'), true)
  assert.equal(page.includes('鎵撳寘杩涘害'), true)
})

test('mobile release auto build should persist records, stream logs, upload apk, and use git notes', async () => {
  const controller = await readSource('../../../rk-user/src/main/java/com/tianji/user/controller/MobileReleaseController.java')
  const service = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/IMobileReleaseService.java')
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/MobileReleaseServiceImpl.java')
  const schema = await readSource('../../../rk-user/src/main/java/com/tianji/user/config/AdminOpsSchemaUpdater.java')
  const dto = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/dto/AdminMobileReleaseConfigDTO.java')
  const vo = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/vo/MobileReleaseConfigVO.java')
  const frontendApi = await readSource('../src/api/mobile-release.js')
  const page = await readSource('../src/views/admin/operation/NotificationCenter.vue')
  const buildScript = await readSource('../../../android-client/rk-club-android/build-apk.ps1')
  const linuxBuildScript = await readSource('../../../android-client/rk-club-android/build-apk.sh')

  assert.equal(controller.includes('/admin/mobile/releases/builds'), true)
  assert.equal(controller.includes('/admin/mobile/releases/builds/latest'), true)
  assert.equal(controller.includes('/admin/mobile/releases/builds/{buildId}/logs'), true)
  assert.equal(service.includes('getAdminBuildHistory'), true)
  assert.equal(service.includes('getLatestBuildRecord'), true)
  assert.equal(service.includes('getBuildLogs'), true)
  assert.equal(schema.includes('mobile_release_build_record'), true)
  assert.equal(schema.includes('mobile_release_build_log'), true)

  assert.equal(impl.includes('build-apk.ps1'), true)
  assert.equal(impl.includes('ProcessBuilder'), true)
  assert.equal(impl.includes('git log'), true)
  assert.equal(impl.includes('uploadApk'), true)
  assert.equal(impl.includes('-VersionName'), true)
  assert.equal(impl.includes('-VersionCode'), true)
  assert.equal(impl.includes('CompletableFuture.supplyAsync'), true)
  assert.equal(impl.includes('androidToolsUrl'), true)
  assert.equal(impl.includes('rk.mobile.release.android-tools-url'), true)
  assert.equal(impl.includes('k8sEnv("ANDROID_TOOLS_URL"'), true)
  assert.equal(impl.includes('hasCompleteK8sApkArtifact'), true)
  assert.equal(impl.includes('return logs;'), true)
  assert.equal(dto.includes('gitCommit'), true)
  assert.equal(vo.includes('buildId'), true)
  assert.equal(buildScript.includes('[string]$VersionName'), true)
  assert.equal(buildScript.includes('[int]$VersionCode'), true)
  assert.equal(linuxBuildScript.includes('ANDROID_TOOLS_TIMEOUT_SECONDS'), true)
  assert.equal(linuxBuildScript.includes('--max-time'), true)
  assert.equal(linuxBuildScript.includes('[ ! -s "${tools_zip}" ]'), true)

  assert.equal(frontendApi.includes('getMobileReleaseBuildHistory'), true)
  assert.equal(frontendApi.includes('getLatestMobileReleaseBuild'), true)
  assert.equal(frontendApi.includes('getMobileReleaseBuildLogs'), true)
  assert.equal(page.includes('buildPollTimer'), true)
  assert.equal(page.includes('mobileReleaseBuildLogs'), true)
  assert.equal(page.includes('releaseNotes'), true)
})

test('mobile release history should expose apk urls and manual download actions', async () => {
  const page = await readSource('../src/views/admin/operation/NotificationCenter.vue')

  assert.equal(page.includes('apkDownloadUrl'), true)
  assert.equal(page.includes('handleDownloadMobileRelease'), true)
  assert.equal(page.includes('row.downloadUrl || row.apkPath'), true)
  assert.equal(page.includes('涓嬭浇鍦板潃'), true)
  assert.equal(page.includes('鎵嬪姩涓嬭浇'), true)
})

test('computed slot table cells should render explicit non-empty tooltip content', async () => {
  const notificationPage = await readSource('../src/views/admin/operation/NotificationCenter.vue')
  const minioPage = await readSource('../src/views/admin/operation/MinioBrowser.vue')
  const k8sPage = await readSource('../src/views/admin/operation/K8s.vue')
  const creditReportPage = await readSource('../src/views/CreditReport.vue')

  assert.equal(/<el-table-column\s+label="涓嬭浇鍦板潃"[^>]*show-overflow-tooltip/.test(notificationPage), false)
  assert.equal(notificationPage.includes(':content="apkDownloadUrl(row)"'), true)
  assert.equal(notificationPage.includes(':disabled="!apkDownloadUrl(row)"'), true)

  assert.equal(/<el-table-column\s+label="涓嬭浇鍦板潃"[^>]*show-overflow-tooltip/.test(minioPage), false)
  assert.equal(minioPage.includes(':content="objectDownloadUrl(row)"'), true)
  assert.equal(minioPage.includes(':disabled="!objectDownloadUrl(row)"'), true)

  assert.equal(/<el-table-column\s+label="寮曠敤宸ヤ綔璐熻浇"[^>]*show-overflow-tooltip/.test(k8sPage), false)
  assert.equal(/<el-table-column\s+label="鎺у埗鍣?[^>]*show-overflow-tooltip/.test(k8sPage), false)
  assert.equal(k8sPage.includes(':content="imageWorkloadRefsText(row)"'), true)
  assert.equal(k8sPage.includes(':content="podControllerText(row)"'), true)

  assert.equal(/<el-table-column\s+label="鏉ユ簮"[^>]*show-overflow-tooltip/.test(creditReportPage), false)
  assert.equal(creditReportPage.includes(':content="creditSourceText(row)"'), true)
})

test('admin ops should expose MinIO browser and Kubernetes image registry management', async () => {
  const controller = await readSource('../../../rk-user/src/main/java/com/tianji/user/controller/AdminOpsController.java')
  const service = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/IAdminOpsService.java')
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const api = await readSource('../src/api/admin-ops.js')
  const router = await readSource('../src/router/index.js')
  const menuSeed = await readSource('../../../basedata/05_insert_menu_data.sql')
  const layout = await readSource('../src/layouts/AdminLayout.vue')
  const minioPage = await readSource('../src/views/admin/operation/MinioBrowser.vue')
  const k8sPage = await readSource('../src/views/admin/operation/K8s.vue')

  for (const marker of [
    '/minio/buckets',
    '/minio/objects',
    '/minio/object/download',
    'listMinioBuckets',
    'listMinioObjects',
    'streamMinioObject'
  ]) {
    assert.equal(controller.includes(marker) || service.includes(marker) || impl.includes(marker) || api.includes(marker), true, `missing MinIO marker ${marker}`)
  }

  for (const marker of [
    '/k8s/images',
    '/k8s/images/cleanup',
    '/k8s/images/export-script',
    'listK8sImages',
    'cleanupK8sImages',
    'downloadK8sImageExportScript',
    'AdminK8sImageVO',
    'AdminK8sImageCleanupDTO'
  ]) {
    assert.equal(controller.includes(marker) || service.includes(marker) || impl.includes(marker) || api.includes(marker), true, `missing image management marker ${marker}`)
  }

  assert.equal(router.includes("path: 'operation/minio-browser'"), true)
  assert.equal(menuSeed.includes('/admin/operation/minio-browser'), true)
  assert.equal(layout.includes('/admin/operation/minio-browser'), true)
  assert.equal(minioPage.includes('MinIO 瀛樺偍娴忚鍣?), true)
  assert.equal(minioPage.includes('downloadMinioObject'), true)
  assert.equal(k8sPage.includes('闀滃儚浠撳簱娓呯悊'), true)
  assert.equal(k8sPage.includes('fetchK8sImages'), true)
  assert.equal(k8sPage.includes('handleDownloadImageScript'), true)
})

test('one-key deploy runtime image export should exclude Rainbond and platform namespaces by default', async () => {
  const dto = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/dto/adminops/AdminDeployPackageCreateDTO.java')
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  assert.equal(dto.includes('includePlatformImages'), true)
  assert.equal(impl.includes('shouldIncludeDeployPackagePodImage'), true)
  assert.equal(impl.includes('isDeployPackagePlatformNamespace'), true)
  assert.equal(impl.includes('isDeployPackagePlatformImage'), true)
  assert.equal(impl.includes('includePlatformImages'), true)
  for (const namespace of ['rainbond', 'rbd-system', 'rbd-gateway', 'kube-system', 'monitoring']) {
    assert.equal(impl.includes(namespace), true, `missing platform namespace exclusion ${namespace}`)
  }
  assert.equal(impl.includes('platform images are excluded by default'), true)
})

test('android update dialog should use a custom immediate/flexible style with download progress details', async () => {
  const mainActivity = await readSource('../../../android-client/rk-club-android/app/src/main/java/com/rkclub/app/MainActivity.java')

  for (const marker of [
    'buildUpdateDialogContent',
    'showStyledUpdateDialog',
    'buildDownloadProgressContent',
    'formatDownloadSize',
    'updateDialogProgressText',
    '绔嬪嵆鏇存柊',
    '绋嶅悗鍐嶈',
    '寮哄埗鏇存柊',
    '鍙€夋洿鏂?
  ]) {
    assert.equal(mainActivity.includes(marker), true, `missing Android update UI marker ${marker}`)
  }
})
