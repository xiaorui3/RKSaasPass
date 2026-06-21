import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

async function readSource(relativePath) {
  return readFile(new URL(relativePath, import.meta.url), 'utf8')
}

test('backend should expose release center APIs and persistence contracts', async () => {
  const controller = await readSource('../../../rk-user/src/main/java/com/tianji/user/controller/AdminReleaseCenterController.java')
  const service = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/IAdminReleaseCenterService.java')
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminReleaseCenterServiceImpl.java')
  const publisher = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/IAdminReleaseRegistryPublisher.java')
  const publisherImpl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminReleaseRegistryPublisher.java')
  const registryResult = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/vo/adminops/AdminReleaseRegistryPublishResult.java')
  const deployDto = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/dto/adminops/AdminReleaseDeployDTO.java')
  const deploymentExecutor = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/IAdminReleaseDeploymentExecutor.java')
  const deploymentContext = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/dto/adminops/AdminReleaseDeploymentContext.java')
  const deploymentResult = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/vo/adminops/AdminReleaseDeploymentResult.java')
  const k3sExecutor = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminReleaseK3sDeploymentExecutor.java')
  const composeExecutor = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminReleaseComposeDeploymentExecutor.java')
  const schema = await readSource('../../../rk-user/src/main/java/com/tianji/user/config/AdminOpsSchemaUpdater.java')

  for (const marker of [
    '@RequestMapping("/admin/ops/release-center")',
    '@GetMapping("/services")',
    '@GetMapping("/services/{serviceCode}/versions")',
    '@PostMapping("/services/{serviceCode}/publish-current")',
    '@PostMapping("/services/{serviceCode}/build")',
    '@PostMapping("/services/{serviceCode}/deploy")',
    '@GetMapping("/tasks/{taskId}")'
  ]) {
    assert.equal(controller.includes(marker), true, `controller missing ${marker}`)
  }

  for (const marker of [
    'listReleaseServices',
    'listServiceVersions',
    'publishCurrentImage',
    'triggerServiceBuild',
    'deployServiceVersion',
    'getReleaseTask'
  ]) {
    assert.equal(service.includes(marker), true, `service interface missing ${marker}`)
  }

  for (const marker of [
    'ops_release_version',
    'ops_release_task',
    'ops_release_deployment',
    'runtime_mode',
    'version_tag',
    'registry_image',
    'previous_image',
    'target_image'
  ]) {
    assert.equal(schema.includes(marker), true, `schema missing ${marker}`)
  }

  for (const marker of [
    'validateReleaseImage',
    'recordReleaseTask',
    'insertReleaseDeployment',
    'ops_operation_audit',
    'IAdminReleaseRegistryPublisher',
    'IAdminReleaseDeploymentExecutor',
    'runtimeMode',
    'redactSensitive',
    'record failed task',
    'record successful task'
  ]) {
    assert.equal(impl.includes(marker), true, `implementation missing ${marker}`)
  }

  for (const marker of [
    'interface IAdminReleaseRegistryPublisher',
    'publishCurrentImage(String serviceCode'
  ]) {
    assert.equal(publisher.includes(marker), true, `publisher interface missing ${marker}`)
  }

  for (const marker of [
    'class AdminReleaseRegistryPublisher',
    'rk.ops.release.registry-username',
    'rk.ops.release.registry-password',
    'docker login',
    '--password-stdin',
    'docker tag',
    'docker push',
    'dry-run registry push',
    'redactSensitive',
    'PublishProgress'
  ]) {
    assert.equal(publisherImpl.includes(marker), true, `publisher impl missing ${marker}`)
  }

  for (const marker of [
    'class AdminReleaseRegistryPublishResult',
    'success(',
    'failed(',
    'PublishProgress'
  ]) {
    assert.equal(registryResult.includes(marker), true, `registry publish result missing ${marker}`)
  }

  for (const marker of [
    'private String runtimeMode',
    'private String composeProjectName',
    'private String composeServiceName',
    'private String composeFile'
  ]) {
    assert.equal(deployDto.includes(marker), true, `deploy dto missing ${marker}`)
  }

  for (const marker of [
    'interface IAdminReleaseDeploymentExecutor',
    'supports(String runtimeMode)',
    'deploy(AdminReleaseDeploymentContext context)'
  ]) {
    assert.equal(deploymentExecutor.includes(marker), true, `deployment executor interface missing ${marker}`)
  }

  for (const marker of [
    'class AdminReleaseDeploymentContext',
    'private String runtimeMode',
    'private String targetImage',
    'private String composeServiceName'
  ]) {
    assert.equal(deploymentContext.includes(marker), true, `deployment context missing ${marker}`)
  }

  for (const marker of [
    'class AdminReleaseDeploymentResult',
    'success(',
    'failed('
  ]) {
    assert.equal(deploymentResult.includes(marker), true, `deployment result missing ${marker}`)
  }

  for (const marker of [
    'class AdminReleaseK3sDeploymentExecutor',
    'supports(String runtimeMode)',
    'kubectl set image',
    'rollout status',
    'ProcessBuilder'
  ]) {
    assert.equal(k3sExecutor.includes(marker), true, `k3s executor missing ${marker}`)
  }

  for (const marker of [
    'class AdminReleaseComposeDeploymentExecutor',
    'supports(String runtimeMode)',
    'docker compose',
    'composeProjectName',
    'composeServiceName',
    '--no-deps',
    'ProcessBuilder'
  ]) {
    assert.equal(composeExecutor.includes(marker), true, `compose executor missing ${marker}`)
  }
})

test('backend should expose update manifest APIs with declarative action safeguards', async () => {
  const controller = await readSource('../../../rk-user/src/main/java/com/tianji/user/controller/AdminReleaseCenterController.java')
  const service = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/IAdminReleaseCenterService.java')
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminReleaseCenterServiceImpl.java')
  const schema = await readSource('../../../rk-user/src/main/java/com/tianji/user/config/AdminOpsSchemaUpdater.java')
  const manifestDto = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/dto/adminops/AdminReleaseUpdateManifestDTO.java')
  const applyDto = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/dto/adminops/AdminReleaseApplyUpdateDTO.java')
  const manifestVo = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/vo/adminops/AdminReleaseUpdateManifestVO.java')

  for (const marker of [
    '@PostMapping("/updates/manifest")',
    '@PostMapping("/updates/apply")',
    'buildUpdateManifest',
    'applyUpdateManifest'
  ]) {
    assert.equal(controller.includes(marker) || service.includes(marker), true, `update API missing ${marker}`)
  }

  for (const marker of [
    'ops_release_update_package',
    'update_version',
    'manifest_json',
    'runtime_mode',
    'channel'
  ]) {
    assert.equal(schema.includes(marker), true, `update schema missing ${marker}`)
  }

  for (const marker of [
    'AdminReleaseUpdateManifestDTO',
    'private String updateVersion',
    'private String channel',
    'private String runtimeMode',
    'private String commitId'
  ]) {
    assert.equal(manifestDto.includes(marker), true, `manifest DTO missing ${marker}`)
  }

  for (const marker of [
    'AdminReleaseApplyUpdateDTO',
    'private String manifestJson',
    'private Boolean dryRun',
    'private String runtimeMode',
    'private String confirmText'
  ]) {
    assert.equal(applyDto.includes(marker), true, `apply DTO missing ${marker}`)
  }

  for (const marker of [
    'AdminReleaseUpdateManifestVO',
    'private String updateVersion',
    'private String manifestJson',
    'private Integer actionCount'
  ]) {
    assert.equal(manifestVo.includes(marker), true, `manifest VO missing ${marker}`)
  }

  for (const marker of [
    'ALLOWED_UPDATE_ACTION_TYPES',
    'image-rollout',
    'sql-migrate',
    'nacos-import',
    'minio-sync',
    'jenkins-build',
    'FORBIDDEN_UPDATE_TOKENS',
    'validateUpdateManifest',
    'recordUpdatePackage',
    'apply-update',
    'dry-run update apply'
  ]) {
    assert.equal(impl.includes(marker), true, `update implementation missing ${marker}`)
  }
})

test('frontend should provide an operation release center page with per-service version actions', async () => {
  const api = await readSource('../src/api/admin-ops.js')
  const router = await readSource('../src/router/index.js')
  const page = await readSource('../src/views/admin/operation/ReleaseCenter.vue')
  const menuSeed = await readSource('../../../basedata/05_insert_menu_data.sql')
  const authMenu = await readSource('../../../rk-auth/rk-auth-service/src/main/java/com/tianji/auth/config/AuthMenuCatalogUpdater.java')

  for (const marker of [
    'listReleaseCenterServices',
    'listReleaseCenterVersions',
    'publishReleaseCurrentImage',
    'triggerReleaseServiceBuild',
    'deployReleaseServiceVersion',
    'buildReleaseUpdateManifest',
    'applyReleaseUpdateManifest',
    'getReleaseCenterTask',
    '/admin/ops/release-center/services'
  ]) {
    assert.equal(api.includes(marker), true, `api missing ${marker}`)
  }

  assert.equal(router.includes("path: 'operation/release-center'"), true)
  assert.equal(router.includes("component: () => import('@/views/admin/operation/ReleaseCenter.vue')"), true)
  assert.equal(menuSeed.includes('/admin/operation/release-center'), true)
  assert.equal(authMenu.includes('/admin/operation/release-center'), true)

  for (const marker of [
    'selectedVersions',
    'taskLog',
    'fetchReleaseServices',
    'handleDeployVersion',
    'runtimeMode',
    'deployMode',
    'docker-compose',
    'k3s',
    'handlePublishCurrent',
    'publishMode',
    'dry-run',
    'real-run',
    'ElMessageBox.confirm',
    'publishReleaseCurrentImage',
    "dryRun: publishMode.value === 'dry-run'",
    'runtimeMode: runtimeMode.value',
    "dryRun: deployMode.value === 'dry-run'",
    'updateManifestText',
    'handleBuildUpdateManifest',
    'handleApplyUpdateManifest',
    'buildReleaseUpdateManifest',
    'applyReleaseUpdateManifest',
    '/updates/manifest',
    '/updates/apply'
  ]) {
    assert.equal(page.includes(marker), true, `release page missing ${marker}`)
  }
})
