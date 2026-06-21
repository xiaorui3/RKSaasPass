import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

async function readSource(relativePath) {
  return readFile(new URL(relativePath, import.meta.url), 'utf8')
}

test('deploy package should support offline direct download, registry pull packages, and cleanup fields', async () => {
  const dto = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/dto/adminops/AdminDeployPackageCreateDTO.java')
  const vo = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/vo/adminops/AdminDeployPackageRecordVO.java')
  const schema = await readSource('../../../rk-user/src/main/java/com/tianji/user/config/AdminOpsSchemaUpdater.java')
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const field of [
    'packageMode',
    'deliveryMode',
    'deleteAfterDownload',
    'includeAllCurrentData',
    'includeImages',
    'includeK8sManifests',
    'includeMinio',
    'includeDockerCompose',
    'imageArtifactMode',
    'registryPrefix',
    'registryServer',
    'exportRuntimeArtifacts'
  ]) {
    assert.equal(dto.includes(field), true, `DTO missing ${field}`)
  }

  for (const field of [
    'packageMode',
    'deliveryMode',
    'deleteAfterDownload',
    'downloaded',
    'downloadedAt',
    'imageArtifactMode',
    'currentStep',
    'currentImage',
    'uploadedImages',
    'totalImages',
    'uploadPercent',
    'registryPullPackage'
  ]) {
    assert.equal(vo.includes(field), true, `VO missing ${field}`)
  }

  for (const column of [
    'package_mode',
    'delivery_mode',
    'delete_after_download',
    'downloaded_at',
    'current_step',
    'current_image',
    'uploaded_images',
    'total_images',
    'upload_percent',
    'registry_pull_package'
  ]) {
    assert.equal(schema.includes(column), true, `schema missing ${column}`)
  }

  for (const marker of [
    'normalizeDeployPackageRequest',
    'offline-full',
    'direct-download',
    'downloaded_at = NOW()',
    'backupStorageService.delete(record.getRemotePath())',
    'docker-compose.yml',
    'SPRING_CLOUD_NACOS_SERVER_ADDR: nacos:8848',
    'DB_HOST: mysql',
    'RABBIT_HOST: rabbitmq',
    'MINIO_ENDPOINT: http://minio:9000',
    'k8s/rk-web-stack.yaml',
    'scripts/export-minio.sh',
    'scripts/import-minio.sh',
    'scripts/push-images-to-registry.sh',
    'scripts/pull-images.sh',
    'CLEAN_AFTER_PUSH=${CLEAN_AFTER_PUSH:-true}',
    'push failed, keep local image tar files',
    "find images -type f -name '*.tar' -delete",
    'local image tar files cleaned after registry push',
    'scripts/install-compose.sh',
    'scripts/install-k8s.sh',
    'export-all-current-data.sh',
    'offline-install.sh',
    'import-databases.sh',
    '--binary-mode=1',
    'artifactMode',
    'offlineImages',
    'registryImages',
    'minioExport',
    'registry.example.com/rk-web',
    'deployPackageRegistryUsername',
    'deployPackageRegistryPassword',
    'deployPackageRegistryImagePullSecretName',
    'resolveDeployPackageRegistryCredentials',
    'readDockerRegistryCredentialsFromImagePullSecret',
    '.dockerconfigjson',
    'registry credentials source: imagePullSecret',
    'REGISTRY_CREDENTIAL_SOURCE',
    'updateDeployPackageStep',
    'updateDeployPackageRegistryProgress',
    'registry image upload failed',
    'runRegistryPushForRuntimeArtifacts',
    'registry runtime image push completed',
    'REGISTRY_USERNAME',
    'REGISTRY_PASSWORD',
    'ctr -n k8s.io images push',
    '__RK_REGISTRY_PUSH__',
    'registry pull package',
    'uploadPercent',
    '"rk_file"',
    '"rk_gateway"',
    '"nacos"'
  ]) {
    assert.equal(impl.includes(marker), true, `implementation missing ${marker}`)
  }
  assert.equal(
    impl.includes('docker rmi \\"$target\\" || true') || impl.includes('docker rmi "$target" || true'),
    true,
    'implementation missing docker target image cleanup after registry push'
  )
})

test('deploy package should support server-side runtime artifact export and cleanup', async () => {
  const dto = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/dto/adminops/AdminDeployPackageCreateDTO.java')
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const page = await readSource('../src/views/admin/operation/DeployPackage.vue')

  assert.match(dto, /private Boolean exportRuntimeArtifacts/)
  assert.match(dto, /private Boolean cleanupRuntimeArtifacts/)

  for (const marker of [
    'exportRuntimeArtifactsToDirectory',
    'exportRuntimeArtifactsFromSourceNode',
    'buildRuntimeImageExportPodManifest',
    'runtime image export through privileged source-node pod',
    'source-node registry push started',
    'DEPLOY_PACKAGE_RUNTIME_EXPORT_MAX_DISK_USED_PERCENT',
    'DEPLOY_PACKAGE_RUNTIME_EXPORT_HEADROOM_PERCENT',
    'assertRuntimeExportDiskHeadroom',
    'runtime image export disk preflight',
    'runtime image export blocked because node disk usage is above threshold',
    'runtime image export blocked because disk preflight was unavailable on every node',
    'parseMaxDiskUsedPercent',
    'MAX_RUNTIME_LOG_ARCHIVE_BYTES',
    'runtime archive too large for pod-log transport',
    'runtime image archive skipped because it exceeds pod-log transport limit',
    'base64 -w 0 /tmp/rk-runtime-images.tgz',
    'extractRuntimeImagesArchive',
    'CONTAINERD_ADDRESS',
    '/run/k3s/containerd/containerd.sock',
    '/run/rke2/containerd/containerd.sock',
    'ctr_cmd -n k8s.io images export',
    'image_candidates()',
    'docker.io/library/$rest',
    '[ \\"$candidate_log\\" = \\"$per_image_log\\" ] || cat \\"$candidate_log\\" >> \\"$per_image_log\\"',
    'images/exported-images.txt',
    'push_list=images/exported-images.txt',
    'push_total=$(awk',
    'no images exported on this source node; registry push skipped',
    'mergeRuntimeArtifactTextFile',
    'registry-image-list.txt',
    'ctr_cmd -n k8s.io images push',
    'registry image upload failed on source node',
    'registry credentials configured: username=$user_state password=$password_state',
    'registry credentials source: $REGISTRY_CREDENTIAL_SOURCE',
    '__RK_REGISTRY_PUSH__ ERROR failed_count=$failed_count pushed_count=$pushed_count total=$total',
    '__RK_REGISTRY_PUSH__ DIAGNOSTICS_BEGIN__',
    'tail -n 80 runtime-push.log',
    'hasSourceNodeRuntimeExportEvidence',
    'hasRuntimeImageTarFiles',
    'hasRegistryImageList',
    'runtime image export failed on source node; no image tar files or registry list were produced',
    'isSourceNodeRuntimeExportHardFailure',
    'throw new IllegalStateException("export runtime artifacts failed: " + e.getMessage(), e)',
    'isRuntimeExportDiagnosticLine',
    'failed to dial',
    'unauthorized',
    'denied',
    'failed to push image',
    'addZipDirectoryEntries(zip, runtimeArtifactDir',
    'runtime-artifacts/export-images.sh',
    'runtime-artifacts/push-images-to-registry.sh',
    'runtime image tar files kept for downloadable package',
    'export CLEAN_AFTER_PUSH=false',
    'registry image tar files are not kept in registry-only mode',
    "find images -type f -name '*.tar' -delete",
    'images/registry-image-list.txt',
    'buildPackageImageList',
    'appendRuntimeExportProgressLines',
    'waitRuntimeImageExportPodPhase(client, apiUrl, token, namespace, podName, packageId',
    'cleanupRuntimeArtifactDirectory',
    'runtime image tar files cleaned from source node',
    'registry image upload failed'
  ]) {
    assert.equal(impl.includes(marker), true, `runtime export missing ${marker}`)
  }
  assert.equal(
    impl.includes('echo "$export_source" >> images/exported-images.txt') || impl.includes('echo \\"$export_source\\" >> images/exported-images.txt'),
    true,
    'runtime export missing exported image append after successful source-node export'
  )
  assert.equal(
    impl.includes('done < "$push_list"') || impl.includes('done < \\"$push_list\\"'),
    true,
    'runtime export missing registry push loop over exported image list'
  )

  assert.equal(page.includes('exportRuntimeArtifacts'), true)
  assert.equal(page.includes('cleanupRuntimeArtifacts'), true)
  assert.equal(page.includes('后台直接导出镜像 tar'), true)
  assert.equal(page.includes('推送后清理源节点临时 tar'), true)
})

test('deploy package registry mode should push on source nodes, stream progress, and ship pull-only image lists', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const marker of [
    'source-node registry push started',
    'RUNTIME_PLATFORM',
    '--platform \\"$RUNTIME_PLATFORM\\"',
    'ctr push failed for $failed_image -> $failed_target',
    'ctr push failed tail follows',
    '__RK_REGISTRY_PUSH__ START total=$total',
    '__RK_REGISTRY_PUSH__ IMAGE $current $total $image $target',
    '__RK_REGISTRY_PUSH__ DONE $current $total $image $target',
    '__RK_REGISTRY_PUSH__ FAIL $current $total $image $target',
    '__RK_REGISTRY_PUSH__ ERROR failed_count=$failed_count pushed_count=$pushed_count total=$total',
    '__RK_REGISTRY_PUSH__ DIAGNOSTICS_BEGIN__',
    '__RK_REGISTRY_PUSH__ CLEANED $pushed_count $total',
    '__RK_REGISTRY_PUSH__ COMPLETE $pushed_count $total',
    'ctr -n k8s.io images push',
    'CONTAINERD_ADDRESS',
    'ctr_cmd -n k8s.io images import',
    'ctr_cmd -n k8s.io images push',
    'REGISTRY_USERNAME=',
    'REGISTRY_PASSWORD=',
    "find images -type f -name '*.tar' -delete",
    'registry image tar files are not kept in registry-only mode',
    'images/registry-image-list.txt',
    'IMAGE_LIST=images/registry-image-list.txt',
    'readRuntimeRegistryImageList',
    'appendRuntimeExportProgressLines'
  ]) {
    assert.equal(impl.includes(marker), true, `source-node registry push contract missing ${marker}`)
  }
  assert.equal(
    impl.includes('docker push "$target"') || impl.includes('docker push \\"$target\\"'),
    true,
    'source-node registry push contract missing docker push "$target"'
  )

  assert.equal(impl.includes('if [ -z'), true)
  assert.equal(impl.includes('IMAGE_LIST:-'), true)
  assert.equal(impl.includes('if [ -s images/registry-image-list.txt ]; then IMAGE_LIST=images/registry-image-list.txt; else IMAGE_LIST=images/image-list.txt; fi'), true)
})

test('deploy package should embed real MinIO objects instead of README-only placeholders', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const marker of [
    'private final MinioClient minioClient',
    'exportMinioObjectsToZip(zip, packageId)',
    'minio/object-manifest.json',
    'minio/export-summary.txt',
    'minio/buckets/',
    'minioClient.listBuckets()',
    'ListObjectsArgs.builder()',
    'GetObjectArgs.builder()',
    'minioClient.getObject',
    'copyStreamToZip',
    'sanitizeMinioZipSegment'
  ]) {
    assert.equal(impl.includes(marker), true, `MinIO export contract missing ${marker}`)
  }
})

test('deploy package should stream large archives through temp files instead of heap byte arrays', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const storage = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/adminops/AdminBackupStorageService.java')
  const api = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/IAdminOpsService.java')
  const controller = await readSource('../../../rk-user/src/main/java/com/tianji/user/controller/AdminOpsController.java')

  for (const marker of [
    'buildDeployPackageArchiveFile',
    'Files.createTempFile("rk-deploy-package-"',
    'Files.newOutputStream(archivePath',
    'backupStorageService.storeFile(fileName, archivePath',
    'Files.size(archivePath)',
    'Files.deleteIfExists(archivePath)',
    'streamDeployPackage(packageId, outputStream)',
    'StreamingResponseBody'
  ]) {
    assert.equal(impl.includes(marker) || controller.includes(marker), true, `streaming deploy package contract missing ${marker}`)
  }

  assert.equal(impl.includes('byte[] bytes = buildDeployPackageArchive('), false, 'deploy package build must not hold the full zip in heap')
  assert.equal(impl.includes('ByteArrayOutputStream output = new ByteArrayOutputStream();\n            try (ZipOutputStream zip'), false, 'deploy package archive must be written to a file stream')
  assert.equal(storage.includes('storeFile(String fileName, Path sourcePath'), true, 'storage service missing file streaming upload')
  assert.equal(storage.includes('stream(String relativePath, OutputStream outputStream'), true, 'storage service missing streaming download')
  assert.equal(api.includes('void streamDeployPackage(Long packageId, OutputStream outputStream)'), true, 'admin ops service missing streaming download contract')
})

test('source-node registry push should prefer host containerd ctr over docker when ctr is available', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const sourceNodeMethodStart = impl.indexOf('private String buildRuntimeImageExportNodeScript')
  const sourceNodeMethodEnd = impl.indexOf('private boolean extractRuntimeImagesArchive', sourceNodeMethodStart)
  assert.notEqual(sourceNodeMethodStart, -1, 'source-node runtime export method missing')
  assert.notEqual(sourceNodeMethodEnd, -1, 'source-node runtime export method end marker missing')

  const sourceNodeMethod = impl.slice(sourceNodeMethodStart, sourceNodeMethodEnd)
  const registryPushStart = sourceNodeMethod.indexOf('source-node registry push started')
  assert.notEqual(registryPushStart, -1, 'source-node registry push block missing')

  const registryPushBlock = sourceNodeMethod.slice(registryPushStart)
  const ctrPushBranch = registryPushBlock.indexOf('if [ -n \\"$CTR\\" ]; then')
  const dockerFallbackBranch = registryPushBlock.indexOf('elif command -v docker >/dev/null 2>&1; then')

  assert.notEqual(ctrPushBranch, -1, 'source-node push branch missing ctr-first runtime path')
  assert.notEqual(dockerFallbackBranch, -1, 'source-node push branch missing docker fallback path')
  assert.ok(
    ctrPushBranch < dockerFallbackBranch,
    'source-node push must use ctr first so RKE2/k3s containerd images are pushed with the detected socket'
  )
  assert.equal(registryPushBlock.includes('runtime registry push runtime: ctr=$CTR docker=$docker_state containerd=${CONTAINERD_ADDRESS:-default}'), true)
  assert.equal(registryPushBlock.includes('--platform \\"$RUNTIME_PLATFORM\\"'), true)
  assert.equal(registryPushBlock.includes('ctr push failed tail follows'), true)
  assert.equal(registryPushBlock.includes('registry push tail follows'), true)
})

test('deploy package creation should return a running record and build asynchronously', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  assert.equal(impl.includes('startDeployPackageBuildAsync'), true)
  assert.equal(impl.includes('runDeployPackageBuild'), true)
  assert.equal(impl.includes('new Thread(task, "rk-deploy-package-"'), true)
  assert.equal(impl.includes('return loadDeployPackageRecord(packageId)'), true)
  assert.equal(impl.includes('TenantContext.setTenantId(capturedTenantId)'), true)
  assert.equal(impl.includes('UserContext.setUser(capturedUserId)'), true)
  assert.equal(impl.includes("status = 'FAILED'"), true)
})

test('deploy package startup should close stale running records left by service restarts', async () => {
  const updater = await readSource('../../../rk-user/src/main/java/com/tianji/user/config/AdminOpsSchemaUpdater.java')

  assert.equal(updater.includes('closeStaleRunningDeployPackages()'), true)
  assert.equal(updater.includes("status = 'RUNNING'"), true)
  assert.equal(updater.includes("status = 'FAILED'"), true)
  assert.equal(updater.includes('interrupted by service restart'), true)
  assert.equal(updater.includes("current_step = 'interrupted'"), true)
  assert.equal(updater.includes('progress = 100'), true)
})

test('deploy package admin page should expose offline bundle, registry progress, and server cleanup options', async () => {
  const page = await readSource('../src/views/admin/operation/DeployPackage.vue')

  for (const marker of [
    'packageMode',
    'deliveryMode',
    'deleteAfterDownload',
    'includeAllCurrentData',
    'imageArtifactMode',
    'includeMinio',
    'includeDockerCompose',
    'registryPrefix',
    'currentStep',
    'currentImage',
    'uploadPercent',
    'uploadedImages',
    'totalImages',
    'registryPullPackage',
    '拉取镜像部署包',
    '离线完整包',
    '直接下载到客户端',
    '镜像模式',
    '离线镜像 tar',
    '阿里云镜像仓库',
    '同时生成',
    '后台会实际导出镜像并推送到镜像仓库',
    'exportRuntimeArtifacts: true',
    '包含 MinIO 对象导出和导入脚本',
    '包含 Docker Compose 部署文件',
    '下载后清理服务器临时包',
    '数据库一键导出',
    '导入脚本',
    'downloadedAt',
    'application/json',
    '部署包下载失败'
  ]) {
    assert.equal(page.includes(marker), true, `admin page missing ${marker}`)
  }
})

test('deploy package downloaded rows and image progress should use explicit completed states', async () => {
  const page = await readSource('../src/views/admin/operation/DeployPackage.vue')
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const marker of [
    'downloadActionLabel',
    'isDownloadDisabled',
    'isRetainedDownloadedPackage',
    'imageProgressTitle',
    'imageProgressCount',
    'imageProgressText'
  ]) {
    assert.equal(page.includes(marker), true, `deploy package page missing ${marker}`)
  }

  assert.equal(
    page.includes(':disabled="isDownloadDisabled(row)"'),
    true,
    'download button must not rely only on row.downloadable'
  )
  assert.equal(
    page.includes('{{ downloadActionLabel(row) }}'),
    true,
    'download button label should reflect downloaded/retained/cleaned state'
  )

  for (const marker of [
    'initialDeployPackageImageCount(request)',
    'completeDeployPackageImageProgress(packageId)',
    'uploaded_images = CASE WHEN total_images > 0 THEN total_images ELSE uploaded_images END',
    'upload_percent = CASE WHEN total_images > 0 THEN 100 ELSE upload_percent END'
  ]) {
    assert.equal(impl.includes(marker), true, `deploy package backend missing ${marker}`)
  }
})
