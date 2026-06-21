import test from 'node:test'
import assert from 'node:assert/strict'
import { access, readFile } from 'node:fs/promises'

async function readSource(relativePath) {
  const source = await readFile(new URL(relativePath, import.meta.url), 'utf8')
  return source.replace(/\r\n/g, '\n')
}

async function readOptionalSource(relativePath) {
  const url = new URL(relativePath, import.meta.url)
  try {
    await access(url)
    const source = await readFile(url, 'utf8')
    return source.replace(/\r\n/g, '\n')
  } catch (error) {
    if (error?.code === 'ENOENT') return ''
    throw error
  }
}

test('deploy package should expose an online remote deploy endpoint with audited kubeconfig retention', async () => {
  const dto = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/dto/adminops/AdminDeployPackageCreateDTO.java')
  const api = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/IAdminOpsService.java')
  const controller = await readSource('../../../rk-user/src/main/java/com/tianji/user/controller/AdminOpsController.java')
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const schema = await readSource('../../../rk-user/src/main/java/com/tianji/user/config/AdminOpsSchemaUpdater.java')
  const vo = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/vo/adminops/AdminDeployPackageRecordVO.java')

  for (const field of [
    'remoteDeploy',
    'kubeconfig',
    'kubeContext',
    'waitRollout',
    'rolloutTimeoutSeconds',
    'dryRun',
    'confirmText',
    'applyDatabaseSnapshot',
    'applyMinioSnapshot'
  ]) {
    assert.equal(dto.includes(field), true, `DTO missing ${field}`)
  }

  assert.equal(api.includes('createRemoteDeploy(AdminDeployPackageCreateDTO dto)'), true)
  assert.equal(controller.includes('@PostMapping("/deploy-package/remote-deploy")'), true)
  assert.equal(controller.includes('createRemoteDeploy'), true)

  for (const column of [
    'deploy_mode',
    'remote_deploy',
    'remote_cluster_name',
    'deploy_started_at',
    'deploy_finished_at',
    'kubeconfig_ciphertext',
    'kubeconfig_redacted',
    'kubeconfig_fingerprint'
  ]) {
    assert.equal(schema.includes(column), true, `schema missing ${column}`)
  }

  for (const field of [
    'deployMode',
    'remoteDeploy',
    'remoteClusterName',
    'deployStartedAt',
    'deployFinishedAt'
  ]) {
    assert.equal(vo.includes(field), true, `VO missing ${field}`)
  }

  for (const marker of [
    'createRemoteDeploy',
    'request.setIncludeDatabases(true)',
    'request.setIncludeAllCurrentData(true)',
    'request.setIncludeMinio(true)',
    'request.setApplyDatabaseSnapshot(true)',
    'request.setApplyMinioSnapshot(true)',
    'request.setWaitRollout(true)',
    'startRemoteDeployAsync',
    'runRemoteDeploy',
    'writeTemporaryKubeconfig',
    'deleteTemporaryKubeconfigQuietly',
    'runKubectlWithKubeconfig',
    'encryptKubeconfig',
    'decryptKubeconfig',
    'fingerprintKubeconfig',
    'redactKubeconfig',
    'sanitizeDeployLog',
    'buildRemoteDeployRequestForStorage',
    'Files.createTempFile("rk-remote-kubeconfig-"',
    'Files.createTempFile("rk-remote-manifest-"',
    'buildKubectlCommand(kubeconfigPath, kubeContext, kubectlArgs)',
    'apply -f',
    'waitRemoteRolloutResources(packageId, kubeconfigPath, request, namespace, "deploy"',
    'REMOTE_DEPLOY',
    'remote deploy kubeconfig received and redacted before persistence',
    'remote deploy manifest applied',
    'remote deploy rollout completed',
    'importRemoteMinioSnapshot',
    'buildRemoteMysqlImportJobManifest',
    'buildRemoteMinioImportJobManifest',
    'remote deploy MinIO imported by temporary job'
  ]) {
    assert.equal(impl.includes(marker), true, `implementation missing ${marker}`)
  }

  assert.equal(
    impl.includes('remote deploy MinIO object import is not automated yet'),
    false,
    'MinIO remote deploy must be automated when applyMinioSnapshot=true'
  )

  for (const secretMarker of [
    'certificate-authority-data',
    'client-certificate-data',
    'client-key-data',
    'Authorization: Bearer',
    '--token='
  ]) {
    assert.equal(impl.includes(secretMarker), true, `sanitize contract missing ${secretMarker}`)
  }

  assert.equal(impl.includes('toJson(request)'), false, 'raw request must not be persisted because it may contain kubeconfig')
  assert.equal(impl.includes('toJson(buildDeployPackageRequestForStorage(request))'), true)
  assert.equal(impl.includes('toJson(buildRemoteDeployRequestForStorage(request))'), true)
  assert.equal(impl.includes('payload.put("kubeconfig", "[REDACTED]")'), true, 'request_json must keep kubeconfig redacted')
  assert.equal(impl.includes('getDeployPackageKubeconfig'), true, 'stored kubeconfig must only be returned through the audited view endpoint')
})

test('remote deploy history should expose detail, kubeconfig view, diagnosis, and repair endpoints', async () => {
  const api = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/IAdminOpsService.java')
  const controller = await readSource('../../../rk-user/src/main/java/com/tianji/user/controller/AdminOpsController.java')
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const schema = await readSource('../../../rk-user/src/main/java/com/tianji/user/config/AdminOpsSchemaUpdater.java')
  const frontendApi = await readSource('../src/api/admin-ops.js')
  const page = await readSource('../src/views/admin/operation/RemoteMigration.vue')
  const repairDto = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/dto/adminops/AdminDeployPackageRepairDTO.java')
  const detailVo = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/vo/adminops/AdminDeployPackageDetailVO.java')
  const kubeconfigVo = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/vo/adminops/AdminDeployPackageKubeconfigVO.java')
  const diagnosisVo = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/vo/adminops/AdminDeployPackageDiagnosisVO.java')
  const repairVo = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/vo/adminops/AdminDeployPackageRepairResultVO.java')

  for (const marker of [
    'getDeployPackageDetail(Long packageId)',
    'getDeployPackageKubeconfig(Long packageId)',
    'diagnoseDeployPackage(Long packageId)',
    'repairDeployPackage(Long packageId, AdminDeployPackageRepairDTO dto)'
  ]) {
    assert.equal(api.includes(marker), true, `service contract missing ${marker}`)
  }

  for (const marker of [
    '@GetMapping("/deploy-package/{packageId}")',
    '@GetMapping("/deploy-package/{packageId}/kubeconfig")',
    '@PostMapping("/deploy-package/{packageId}/diagnose")',
    '@PostMapping("/deploy-package/{packageId}/repair")',
    'AdminDeployPackageDetailVO',
    'AdminDeployPackageKubeconfigVO',
    'AdminDeployPackageDiagnosisVO',
    'AdminDeployPackageRepairResultVO'
  ]) {
    assert.equal(controller.includes(marker), true, `controller contract missing ${marker}`)
  }

  for (const marker of [
    'last_diagnosis_json',
    'last_repair_json'
  ]) {
    assert.equal(schema.includes(marker), true, `schema missing ${marker}`)
  }

  for (const marker of [
    'SUPPORTED_REMOTE_REPAIR_ACTIONS',
    'REPAIR_RESTART_WORKLOADS',
    'REPAIR_DELETE_STUCK_PODS',
    'REPAIR_MINIO_PUBLIC_POLICY',
    'REPAIR_RERUN_MYSQL_IMPORT',
    'REPAIR_RERUN_MINIO_IMPORT',
    'get pod,sts,deploy,svc,pvc,endpoints',
    'get events --sort-by=.lastTimestamp',
    'nacos config',
    'rk-shared-mybatis.yaml',
    'xxl_job',
    'mc anonymous set download',
    'ImagePullBackOff',
    'ContainerCreating',
    'Pulling image',
    'recordOperationAudit'
  ]) {
    assert.equal(impl.includes(marker), true, `diagnosis/repair implementation missing ${marker}`)
  }

  for (const marker of [
    'repairAction',
    'confirmText'
  ]) {
    assert.equal(repairDto.includes(marker), true, `repair DTO missing ${marker}`)
  }

  for (const marker of [
    'record',
    'requestSummary',
    'lastDiagnosis',
    'lastRepair',
    'kubeconfigRedacted',
    'kubeconfigFingerprint'
  ]) {
    assert.equal(detailVo.includes(marker), true, `detail VO missing ${marker}`)
  }

  for (const marker of [
    'kubeconfig',
    'kubeconfigFingerprint',
    'redacted',
    'viewedAt'
  ]) {
    assert.equal(kubeconfigVo.includes(marker), true, `kubeconfig VO missing ${marker}`)
  }

  for (const marker of [
    'serviceStatuses',
    'diagnosisChecks',
    'suggestedRepairActions',
    'terminalLog'
  ]) {
    assert.equal(diagnosisVo.includes(marker), true, `diagnosis VO missing ${marker}`)
  }

  for (const marker of [
    'repairAction',
    'success',
    'outputs',
    'nextSuggestion'
  ]) {
    assert.equal(repairVo.includes(marker), true, `repair VO missing ${marker}`)
  }

  for (const marker of [
    'getDeployPackageDetail',
    'getDeployPackageKubeconfig',
    'diagnoseDeployPackage',
    'repairDeployPackage',
    '/admin/ops/deploy-package/${id}/kubeconfig',
    '/admin/ops/deploy-package/${id}/diagnose',
    '/admin/ops/deploy-package/${id}/repair',
    'migrationHistoryDrawer',
    'kubeconfigDialog',
    'remoteDiagnosis',
    'repairRemoteMigrationAction'
  ]) {
    assert.equal(frontendApi.includes(marker) || page.includes(marker), true, `frontend history/diagnosis contract missing ${marker}`)
  }
})

test('linux ssh deploy package download should not hit the default mvc async timeout', async () => {
  const webMvcConfig = await readSource('../../../rk-user/src/main/java/com/tianji/user/config/WebMvcAuditConfig.java')
  const controller = await readSource('../../../rk-user/src/main/java/com/tianji/user/controller/AdminOpsController.java')

  assert.equal(
    controller.includes('@GetMapping("/public/deploy-package/{packageId}/linux-ssh-download")'),
    true,
    'linux ssh public download endpoint must stay available'
  )
  assert.equal(
    controller.includes('StreamingResponseBody body = outputStream -> adminOpsService.streamLinuxSshDeployPackage(packageId, token, outputStream)'),
    true,
    'linux ssh download endpoint must keep streaming package content'
  )

  for (const marker of [
    'configureAsyncSupport(AsyncSupportConfigurer configurer)',
    'setDefaultTimeout',
    'LINUX_SSH_STREAMING_TIMEOUT_MILLIS',
    'Duration.ofHours(2).toMillis()'
  ]) {
    assert.equal(webMvcConfig.includes(marker), true, `mvc async timeout contract missing ${marker}`)
  }
})

test('linux ssh deploy package download should avoid slow proxy downloads and quickly fall back', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const shellImpl = impl.replaceAll('\\"', '"')

  for (const marker of [
    'buildLinuxSshPublicMinioUrl',
    'resolveLinuxSshPublicOrigin',
    'resolveLinuxSshDownloadBaseUrl',
    'https://example.com',
    '"/minio-files/"',
    'publicMinioUrl',
    'download.primaryDownloadUrl()',
    'linux ssh downloading deploy package from public MinIO URL',
    '--speed-limit',
    '--speed-time',
    'LINUX_SSH_PACKAGE_DOWNLOAD_MIN_BYTES_PER_SECOND',
    'LINUX_SSH_PACKAGE_DOWNLOAD_LOW_SPEED_SECONDS',
    'resolveLinuxSshPackageDownloadTimeoutSeconds',
    'linux ssh package download is too slow; switching to chunked SSH upload',
    'falling back to chunked SSH upload'
  ]) {
    assert.equal(impl.includes(marker), true, `linux ssh slow download fallback contract missing ${marker}`)
  }

  assert.equal(
    shellImpl.includes('case "$DOWNLOAD_URL" in http://*|https://*) ;; *) DOWNLOAD_URL="$PROXY_DOWNLOAD_URL" ;; esac'),
    true,
    'linux ssh slow download fallback contract missing proxy URL fallback'
  )

  assert.match(
    impl,
    /runLinuxSshStage\(session,\s*packageId,\s*42,\s*"linux ssh downloading deploy package",[\s\S]*resolveLinuxSshPackageDownloadTimeoutSeconds\(Files\.size\(archivePath\)\)/,
    'linux ssh package download stage must use a package-size-aware timeout instead of a fixed 1800 seconds'
  )
  assert.equal(
    impl.includes('--max-time 1800 "$DOWNLOAD_URL"'),
    false,
    'linux ssh package download must not wait the old fixed 1800 seconds before fallback'
  )
  assert.doesNotMatch(
    impl,
    /return sanitizeOpsUrl\(resolved\);\s*}\s*private String createLinuxSshPackageDownloadToken/,
    'linux ssh public MinIO URL must not fall back to an empty/relative base URL when async requests have no public request context'
  )
})

test('linux ssh k3s bootstrap should not fail only because metrics-server is not ready', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const marker of [
    'metrics-server is not ready yet; continuing because it is not required by RK services',
    'kubectl -n kube-system describe deployment/metrics-server || true',
    'sanitizeDeployLog',
    'replaceAll("\\\\x1B(?:[@-Z\\\\\\\\-_]|\\\\[[0-?]*[ -/]*[@-~])", "")'
  ]) {
    assert.equal(impl.includes(marker), true, `linux ssh k3s metrics/log hardening missing ${marker}`)
  }

  assert.equal(
    impl.includes('kubectl -n kube-system rollout status deployment/metrics-server --timeout=360s; fi'),
    false,
    'metrics-server readiness must not block SSH full-machine deployment'
  )
})

test('remote deploy should force full data payload preparation and verify imported Nacos config', async () => {
  const dto = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/dto/adminops/AdminDeployPackageCreateDTO.java')
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const page = await readSource('../src/views/admin/operation/RemoteMigration.vue')

  for (const marker of [
    'private Boolean applyDatabaseSnapshot = true',
    'private Boolean applyMinioSnapshot = true'
  ]) {
    assert.equal(dto.includes(marker), true, `remote deploy DTO default missing ${marker}`)
  }

  for (const marker of [
    'boolean forceFullRemoteData = Boolean.TRUE.equals(request.getRemoteDeploy()) && !Boolean.TRUE.equals(request.getDryRun())',
    'boolean needsPayloadVolume = !Boolean.TRUE.equals(request.getDryRun())\n                    && (forceFullRemoteData || Boolean.TRUE.equals(request.getApplyDatabaseSnapshot()) || Boolean.TRUE.equals(request.getApplyMinioSnapshot()))',
    'boolean applyDatabaseSnapshot = (forceFullRemoteData || Boolean.TRUE.equals(request.getApplyDatabaseSnapshot()))',
    'boolean applyMinioSnapshot = (forceFullRemoteData || Boolean.TRUE.equals(request.getApplyMinioSnapshot()))',
    'SELECT TABLE_SCHEMA FROM information_schema.TABLES WHERE TABLE_NAME=\'config_info\'',
    'TABLE_SCHEMA IN (\'nacos\',\'nacos_config\')',
    'SELECT COUNT(*) FROM ${nacos_schema}.config_info WHERE data_id=',
    'rk-shared-mybatis.yaml',
    'SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=',
    'remote deploy database import verification failed',
    'env.put("MYSQL_SERVICE_DB_NAME", "nacos")'
  ]) {
    assert.equal(impl.includes(marker), true, `forced remote data import contract missing ${marker}`)
  }

  for (const marker of [
    'includeDatabases: true',
    'includeAllCurrentData: true',
    'includeMinio: true',
    'applyDatabaseSnapshot: true',
    'applyMinioSnapshot: true'
  ]) {
    assert.equal(page.includes(marker), true, `remote deploy frontend payload missing ${marker}`)
  }

  assert.equal(
    impl.includes('env.put("MYSQL_SERVICE_DB_NAME", "nacos_config")'),
    false,
    'Nacos must default to the exported nacos database; nacos_config is only tolerated by import verification'
  )
})

test('remote deploy submission should create a task before heavy runtime image scans', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const methodStart = impl.indexOf('public AdminDeployPackageRecordVO createRemoteDeploy(AdminDeployPackageCreateDTO dto)')
  assert.notEqual(methodStart, -1, 'missing createRemoteDeploy implementation')
  const insertStart = impl.indexOf('jdbcTemplate.update(', methodStart)
  assert.notEqual(insertStart, -1, 'createRemoteDeploy must insert a deploy record')
  const beforeInsert = impl.slice(methodStart, insertStart)

  assert.equal(
    beforeInsert.includes('estimateDeployPackageImageCount()'),
    false,
    'remote deploy HTTP submission must not scan live runtime images before creating a task id'
  )
  assert.equal(
    impl.includes('initialRemoteDeployImageCount(request)'),
    true,
    'remote deploy should use a lightweight initial image estimate and let the async worker calculate exact totals'
  )
})

test('runtime image export should only scan RK business workloads from the configured namespace', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const marker of [
    'private boolean isDeployPackageBusinessNamespace(String namespace)',
    'trimToDefault(k8sCleanupNamespace, "shetuanguanlixitong")',
    'if (!isDeployPackageBusinessNamespace(namespace))',
    'private boolean isDeployPackageBusinessRuntimeImage(String image)',
    'isDeployPackageBusinessRuntimeImage(image)',
    'SERVICE_IMAGE_MAP.values()',
    'K8S_PERSISTENT_MIDDLEWARE',
    'value.contains("goodrain/")',
    'repository.endsWith("-" + candidate)'
  ]) {
    assert.equal(impl.includes(marker), true, `runtime image export business scope missing ${marker}`)
  }
})

test('remote deploy should export and verify xxl-job scheduler database', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const listMatch = impl.match(/private static final List<String> DEFAULT_DEPLOY_PACKAGE_DATABASES = List\.of\(([\s\S]*?)\);/)

  assert.notEqual(listMatch, null, 'missing DEFAULT_DEPLOY_PACKAGE_DATABASES declaration')
  assert.equal(listMatch[1].includes('"xxl_job"'), true, 'remote deploy database snapshot must include xxl_job')

  assert.match(
    impl,
    /xxl_job_table_count=\$\(mysql[\s\S]*TABLE_SCHEMA='xxl_job' AND TABLE_NAME='xxl_job_info'/,
    'remote deploy import job must verify xxl_job.xxl_job_info after import'
  )
  assert.equal(
    impl.includes('xxl_job_table_count:-0'),
    true,
    'remote deploy import job must fail when xxl_job table verification is empty'
  )
})

test('remote deploy kubeconfig commands should not depend on the container PATH', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const marker of [
    'private String resolveLocalKubectlBinary()',
    'List.of("/usr/local/bin/kubectl", "/usr/bin/kubectl")',
    'Files.isExecutable(Path.of(candidate))',
    'private String buildKubectlCommand(Path kubeconfigPath, String kubeContext, String kubectlArgs)'
  ]) {
    assert.equal(impl.includes(marker), true, `kubectl binary resolver missing ${marker}`)
  }

  assert.equal(
    impl.includes('new StringBuilder("kubectl --kubeconfig ")'),
    false,
    'remote deploy must not assume kubectl is on PATH because rk-user runtime may only have /usr/local/bin/kubectl'
  )
})

test('linux ssh deploy should make target host pull package by token and keep retryable chunk upload fallback', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const controller = await readSource('../../../rk-user/src/main/java/com/tianji/user/controller/AdminOpsController.java')
  const permissionFilter = await readSource('../../../rk-user/src/main/java/com/tianji/user/config/PermissionFilter.java')
  const authCustomization = await readSource('../../../rk-user/src/main/java/com/tianji/user/config/ResourceAuthCustomizationConfig.java')
  const gatewayLockFilter = await readSource('../../../rk-gateway/src/main/java/com/tianji/gateway/filter/GlobalMigrationLockFilter.java')
  const gatewayBootstrap = await readSource('../../../rk-gateway/src/main/resources/bootstrap.yml')

  for (const marker of [
    'session = uploadLinuxSshPackageByResumableSftp(session, packageId, archivePath, remotePackagePath, request, password)',
    'session = uploadLinuxSshPackage(session, packageId, archivePath, remotePackagePath, request, password)',
    'private LinuxSshPackageDownload prepareLinuxSshPackageDownloadUrl',
    'createLinuxSshPackageDownloadToken',
    'streamLinuxSshDeployPackage',
    'linux ssh downloading deploy package from public MinIO URL',
    'curl -fL --retry 2 --retry-delay 3',
    'backupStorageService.storeFile',
    'backupStorageService.delete',
    'private Session reopenLinuxSshSessionForPackageUpload(Session currentSession, AdminDeployPackageCreateDTO request, Long packageId)',
    'private Session uploadLinuxSshPackageByResumableSftp(Session session, Long packageId, Path archivePath, String remotePackagePath,',
    'sftp = (ChannelSftp) session.openChannel("sftp")',
    'ChannelSftp.RESUME',
    'linux ssh resumable sftp upload retry',
    'linux ssh package resumable sftp failed, falling back to chunked SSH upload',
    'private void skipLinuxSshLocalFileInput(InputStream inputStream, long bytes)',
    'LINUX_SSH_PACKAGE_UPLOAD_CHUNK_BYTES = 1024 * 1024',
    'LINUX_SSH_PACKAGE_UPLOAD_CHUNK_RETRY_ATTEMPTS',
    'private Session ensureLinuxSshSession(Session session, AdminDeployPackageCreateDTO request, Long packageId, int progress, String password)',
    'verifyLinuxSshPackageUpload(session, packageId, archivePath, remotePackagePath, password)',
    'private void verifyLinuxSshPackageUpload(Session session, Long packageId, Path archivePath, String remotePackagePath, String password)',
    'Files.newInputStream(archivePath)',
    'truncate -s \\"$EXPECTED_OFFSET\\" \\"$TMP_FILE\\"',
    'cat >> \\"$TMP_FILE\\"',
    'linux ssh upload chunk retry',
    'linux ssh upload chunk verified',
    'wc -c <',
    'linux ssh package upload verified',
    'linux ssh package upload size mismatch'
  ]) {
    assert.equal(impl.includes(marker), true, `linux ssh upload hardening missing ${marker}`)
  }
  assert.match(
    impl,
    /private Session uploadLinuxSshPackageChunk\(Session session,\s*AdminDeployPackageCreateDTO request,\s*Long packageId,[\s\S]*byte\[\] chunk,[\s\S]*long offset,[\s\S]*long totalBytes,[\s\S]*String password\)/,
    'linux ssh upload fallback must chunk the package and retry each chunk independently'
  )
  assert.equal(
    impl.includes('sftp.put(archivePath.toString()'),
    false,
    'linux ssh large package upload must not use one-shot JSch SFTP because target links can close the input stream'
  )
  assert.match(
    impl,
    /catch \(Exception sftpError\)[\s\S]*linux ssh package resumable sftp failed, falling back to chunked SSH upload[\s\S]*uploadLinuxSshPackage\(session, packageId, archivePath, remotePackagePath, request, password\)/,
    'linux ssh deploy must try resumable SFTP before falling back to the slower chunked SSH uploader'
  )
  assert.equal(
    impl.includes('cat > \\"$TMP_FILE\\"'),
    false,
    'linux ssh large package upload must not depend on one long exec stdin stream because the target closes that stream after a small write'
  )
  assert.equal(
    impl.includes('OutputStream stdin = channel.getOutputStream()'),
    false,
    'linux ssh large package upload must avoid one long JSch stdin stream; chunks should be short and independently retryable'
  )
  assert.equal(
    impl.includes('.skipNBytes('),
    false,
    'linux ssh deploy must stay Java 11 compatible because the production Jenkins build image uses Java 11'
  )
  assert.equal(
    controller.includes('@GetMapping("/public/deploy-package/{packageId}/linux-ssh-download")'),
    true,
    'linux ssh target host needs a public token download endpoint so SSH is not used for the large zip payload'
  )
  assert.equal(
    controller.includes('streamLinuxSshDeployPackage(packageId, token, outputStream)'),
    true,
    'public linux ssh download endpoint must stream through a one-time token'
  )
  assert.equal(
    permissionFilter.includes('requestURI.startsWith("/admin/ops/public/deploy-package/")'),
    true,
    'public linux ssh download endpoint must bypass user-service permission filter'
  )
  assert.equal(
    authCustomization.includes('/admin/ops/public/deploy-package/*/linux-ssh-download'),
    true,
    'public linux ssh download endpoint must bypass resource-server login filtering'
  )
  assert.equal(
    gatewayLockFilter.includes('/admin/ops/public/deploy-package/') && gatewayLockFilter.includes('/linux-ssh-download'),
    true,
    'global migration lock must allow the target host to download the deploy package while the lock is active'
  )
  assert.equal(
    gatewayBootstrap.includes('/admin/ops/public/deploy-package/**'),
    true,
    'gateway route must expose public deploy package download endpoints'
  )
})

test('remote migration MinIO exports should skip generated ops backup artifacts', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  assert.equal(
    impl.includes('private boolean shouldSkipMinioObjectForMigration(String bucketName, String objectName)'),
    true,
    'MinIO migration export must centralize skip rules for generated artifacts'
  )
  assert.match(
    impl,
    /buildRemoteMinioPayloadDirectory[\s\S]*shouldSkipMinioObjectForMigration\(bucketName,\s*objectName\)/,
    'kubeconfig remote deploy MinIO payload export must skip generated artifacts'
  )
  assert.match(
    impl,
    /exportMinioObjectsToZip[\s\S]*shouldSkipMinioObjectForMigration\(bucketName,\s*objectName\)/,
    'linux ssh deploy package MinIO zip export must skip generated artifacts'
  )
  assert.equal(
    impl.includes('isSameMinioBucket(bucketName, "rk-user") && normalizedObjectName.startsWith("backup/")'),
    true,
    'rk-user/backup contains generated deploy packages and must not be recursively migrated'
  )
  for (const marker of [
    'skippedObjectCount',
    'skippedBytes',
    'MinIO objects exported: buckets=',
    ', skipped=',
  ]) {
    assert.equal(impl.includes(marker), true, `MinIO export summary missing ${marker}`)
  }
})

test('remote migration page should allow pasting kubeconfig for online remote deployment', async () => {
  const page = await readSource('../src/views/admin/operation/RemoteMigration.vue')
  const frontendApi = await readSource('../src/api/admin-ops.js')

  for (const marker of [
    'createRemoteMigration',
    '/admin/ops/remote-migration/kubeconfig-deploy',
    'remoteDeploy',
    'kubeconfigText',
    'kubeContext',
    'waitRollout',
    'dryRun',
    'confirmText',
    'applyDatabaseSnapshot',
    'applyMinioSnapshot',
    '在线 Kubernetes 迁移',
    '粘贴 kubeconfig',
    'REMOTE_DEPLOY',
    '不会保存 kubeconfig 明文'
  ]) {
    assert.equal(page.includes(marker) || frontendApi.includes(marker), true, `frontend missing ${marker}`)
  }
})

test('remote migration page should default to ACK one-click migration options', async () => {
  const page = await readSource('../src/views/admin/operation/RemoteMigration.vue')
  const formMatch = page.match(/const form = reactive\(\{([\s\S]*?)\n\}\)/)
  const remoteFormMatch = page.match(/const remoteForm = reactive\(\{([\s\S]*?)\n\}\)/)

  assert.notEqual(formMatch, null, 'remote migration page must keep a reactive form defaults block')
  assert.notEqual(remoteFormMatch, null, 'remote migration page must keep a remote form defaults block')

  const defaults = formMatch[1]
  for (const marker of [
    "imageArtifactMode: 'registry'",
    "targetClusterType: 'ack'",
    "targetNamespace: 'shetuanguanlixitong'",
    "storageClassName: 'alicloud-disk-essd'",
    "externalExposureType: 'loadBalancer'",
    'exportRuntimeArtifacts: true',
    'includeAllCurrentData: true',
    'includeDatabases: true',
    'includeMinio: true'
  ]) {
    assert.equal(defaults.includes(marker), true, `ACK remote deploy default missing ${marker}`)
  }

  const remoteDefaults = remoteFormMatch[1]
  for (const marker of [
    'waitRollout: true',
    'dryRun: false',
    'applyDatabaseSnapshot: true',
    'applyMinioSnapshot: true',
    "confirmText: 'REMOTE_DEPLOY'"
  ]) {
    assert.equal(remoteDefaults.includes(marker), true, `ACK remote deploy remote default missing ${marker}`)
  }
})

test('remote deploy should expose target cluster profile, storage class, external entry, and preflight controls', async () => {
  const dto = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/dto/adminops/AdminDeployPackageCreateDTO.java')
  const api = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/IAdminOpsService.java')
  const controller = await readSource('../../../rk-user/src/main/java/com/tianji/user/controller/AdminOpsController.java')
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const page = await readSource('../src/views/admin/operation/RemoteMigration.vue')
  const frontendApi = await readSource('../src/api/admin-ops.js')

  for (const field of [
    'storageClassName',
    'externalExposureType',
    'frontendNodePort',
    'gatewayNodePort',
    'ingressClassName',
    'ingressHost'
  ]) {
    assert.equal(dto.includes(field), true, `DTO missing ${field}`)
  }

  for (const marker of [
    'preflightDeployPackage(AdminDeployPackageCreateDTO dto)',
    '@PostMapping("/deploy-package/preflight")',
    'preflightDeployPackage',
    'AdminDeployPackagePreflightVO',
    '/admin/ops/deploy-package/preflight',
    'preflightDeployPackage(data)'
  ]) {
    assert.equal(api.includes(marker) || controller.includes(marker) || impl.includes(marker) || frontendApi.includes(marker), true, `preflight contract missing ${marker}`)
  }

  for (const marker of [
    'value="ack"',
    'value="k8s"',
    'value="k3s"',
    'value="custom"',
    'value="none"',
    'value="nodePort"',
    'value="loadBalancer"',
    'value="ingress"',
    'storageClassName',
    'externalExposureType',
    'frontendNodePort',
    'gatewayNodePort',
    'ingressClassName',
    'ingressHost',
    '部署前预检',
    'ACK',
    '标准 Kubernetes',
    '公网入口'
  ]) {
    assert.equal(page.includes(marker), true, `remote deploy UI missing ${marker}`)
  }

  for (const marker of [
    'resolveDeployTargetDefaults(request)',
    'applyDeployTargetDefaults(request)',
    'buildExternalExposureManifest(request, namespace)',
    'buildPublicService(namespace, "rk-web-frontend-public", "rk-web-frontend", 80, 80, request)',
    'buildPublicService(namespace, "rk-gateway-public", "rk-gateway", 10010, 10010, request)',
    'kind: Ingress',
    'ingressClassName:',
    'type: NodePort',
    'type: LoadBalancer',
    'nodePort: ',
    'storageClassName: " + storageClassName + "\\n"',
    'get storageclass',
    'get ingressclass',
    'get nodes',
    'ACK LoadBalancer 会创建云 SLB'
  ]) {
    assert.equal(impl.includes(marker), true, `remote deploy target profile implementation missing ${marker}`)
  }

  assert.equal(
    impl.includes('String storageClassName = trimToEmpty(deployPackageK8sStorageClassName);'),
    false,
    'remote manifest must not only use the global ACK storage class default'
  )
})

test('remote deploy records should show no offline package size instead of 0 B', async () => {
  const page = await readSource('../src/views/admin/operation/RemoteMigration.vue')

  assert.equal(
    page.includes('<el-table-column prop="fileSize" label="大小"'),
    false,
    'remote deploy rows must not use the raw fileSize column because remote deploy has no downloadable archive'
  )

  for (const marker of [
    'remote-migration-log',
    'deployMode',
    '远程迁移记录',
    '迁移进度'
  ]) {
    assert.equal(page.includes(marker), true, `remote deploy size display missing ${marker}`)
  }
})

test('remote migration should use target-cluster import jobs instead of execing middleware pods directly', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const page = await readSource('../src/views/admin/operation/RemoteMigration.vue')

  for (const marker of [
    'buildRemoteMysqlImportJobManifest',
    'buildRemoteMinioImportJobManifest',
    'rk-migration-mysql-import',
    'rk-migration-minio-import',
    'rk-migration-payload',
    'rk-migration-payload-staging',
    'buildRemoteMigrationPayloadPvcManifest',
    'buildRemoteMigrationStagingPodManifest',
    'kubectl cp',
    'remote deploy releasing migration payload staging pod',
    '--ignore-not-found=true --wait=true',
    'persistentVolumeClaim',
    'kubectl create job',
    'rollout status job/rk-migration-mysql-import',
    'rollout status job/rk-migration-minio-import',
    'kubectl logs job/rk-migration-mysql-import',
    'kubectl logs job/rk-migration-minio-import',
    'mysql_fast_import_stream',
    'SET autocommit=0; SET unique_checks=0; SET foreign_key_checks=0;',
    'COMMIT; SET foreign_key_checks=1; SET unique_checks=1; SET autocommit=1;',
    'gzip -dc /migration/rk-all-current-data.sql.gz | mysql_fast_import_stream',
    'mc alias set target http://minio:9000',
    'mc mirror --overwrite',
    'mc anonymous set download \\"target/$bucket\\"',
    '/migration/minio-payload',
    'remote deploy database imported by temporary job',
    'remote deploy MinIO imported by temporary job',
    'remote deploy MinIO bucket policies restored'
  ]) {
    assert.equal(impl.includes(marker), true, `remote importer job contract missing ${marker}`)
  }

  assert.equal(
    impl.includes('exec " + quoteForBash(podName) + " -- sh -c " + quoteForBash("gzip -dc /tmp/rk-all-current-data.sql.gz | mysql'),
    false,
    'remote DB import must not exec directly into the mysql service pod'
  )
  assert.equal(
    impl.includes('gzip -dc /migration/rk-all-current-data.sql.gz | mysql --binary-mode=1 -hmysql -P3306'),
    false,
    'remote DB import must not stream large snapshots through a bare per-statement mysql import'
  )
  assert.equal(
    impl.includes('startKubectlPortForward(kubeconfigPath, request.getKubeContext(), namespace, "svc/minio"'),
    false,
    'remote MinIO import must run inside the target cluster instead of local port-forward streaming'
  )
  assert.equal(
    impl.includes('kubectl create configmap rk-migration-database-snapshot'),
    false,
    'remote DB import must not push large snapshots through ConfigMap'
  )
  assert.equal(
    impl.includes('kubectl create configmap rk-migration-minio-manifest'),
    false,
    'remote MinIO import must not rely on ConfigMap-only bucket manifests'
  )

  for (const marker of [
    '临时导入 Pod',
    'MySQL 导入 Job',
    'MinIO 导入 Job',
    '导入完成后自动清理临时资源'
  ]) {
    assert.equal(page.includes(marker), true, `remote deploy page missing importer job copy ${marker}`)
  }
})

test('remote migration should wait stateful middleware and clean legacy ephemeral deployments', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const marker of [
    'deleteLegacyPersistentMiddlewareDeployments(packageId, kubeconfigPath, request, namespace)',
    'delete deploy mysql redis rabbitmq minio nacos elasticsearch --ignore-not-found=true',
    'waitRemoteStatefulSet(packageId, kubeconfigPath, request, namespace, "mysql", dataPlaneTimeout)',
    'waitRemoteStatefulSet(packageId, kubeconfigPath, request, namespace, "minio", dataPlaneTimeout)',
    'rollout status statefulset/" + name',
    'waitRemoteRolloutResources(packageId, kubeconfigPath, request, namespace, "statefulset"',
    'remote deploy waiting statefulsets',
    'wait_for_k8s_resource() { resource=\\"$1\\"',
    'kubectl -n \\"$NAMESPACE\\" get statefulset -o name | while read resource; do wait_for_k8s_resource \\"$resource\\" 15m; done'
  ]) {
    assert.equal(impl.includes(marker), true, `stateful remote deploy contract missing ${marker}`)
  }

  assert.equal(
    impl.includes('rollout status deploy/minio --timeout=300s'),
    false,
    'MinIO import must wait for statefulset/minio after persistent migration'
  )
  assert.equal(
    impl.includes('kubectl -n \\"$NAMESPACE\\" get statefulset -o name | while read resource; do kubectl -n \\"$NAMESPACE\\" rollout status \\"$resource\\" --timeout=10m || true; done'),
    false,
    'K8s installer must not hide statefulset rollout failures'
  )
  assert.equal(impl.includes('rollout status statefulset --all'), false, 'ACK kubectl does not support rollout status --all for statefulsets')
  assert.equal(impl.includes('rollout status deploy --all'), false, 'ACK kubectl does not support rollout status --all for deployments')
})

test('remote migration should tolerate slow ACK first-time middleware image pulls before importing data', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const marker of [
    'resolveRemoteDataPlaneTimeoutSeconds(request)',
    'int dataPlaneTimeout = resolveRemoteDataPlaneTimeoutSeconds(request)',
    'Math.max(1800, Math.max(rolloutTimeout, runtimeExportTimeout))',
    'waitRemoteStatefulSet(packageId, kubeconfigPath, request, namespace, "mysql", dataPlaneTimeout)',
    'waitRemoteStatefulSet(packageId, kubeconfigPath, request, namespace, "minio", dataPlaneTimeout)',
    'waitRemoteJob(packageId, kubeconfigPath, request, namespace, REMOTE_MYSQL_IMPORT_JOB, dataPlaneTimeout)',
    'waitRemoteJob(packageId, kubeconfigPath, request, namespace, REMOTE_MINIO_IMPORT_JOB, dataPlaneTimeout)'
  ]) {
    assert.equal(impl.includes(marker), true, `ACK slow-pull remote deploy timeout guard missing ${marker}`)
  }

  assert.equal(
    impl.includes('waitRemoteStatefulSet(packageId, kubeconfigPath, request, namespace, "mysql", 600)'),
    false,
    'remote database import must not fail before ACK finishes slow first-time mysql image pull'
  )
})

test('remote migration should restart nacos and workloads after snapshot import', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const marker of [
    'restartRemoteStatefulSet(packageId, kubeconfigPath, request, namespace, "nacos", Math.max(600, timeout))',
    'waitRemoteNacosConfigReady(packageId, kubeconfigPath, request, namespace, "rk-shared-mybatis.yaml", Math.max(600, timeout))',
    'restartRemoteDeployments(packageId, kubeconfigPath, request, namespace, REMOTE_K8S_DEPLOYMENTS, timeout)',
    'remote deploy restarting nacos after database import',
    'remote deploy nacos config ready',
    'remote deploy restarting application workloads after data import'
  ]) {
    assert.equal(impl.includes(marker), true, `remote deploy import restart barrier missing ${marker}`)
  }

  assert.equal(
    impl.indexOf('importRemoteDatabaseSnapshot(packageId, kubeconfigPath, request, namespace, databaseSnapshotPath)') <
      impl.indexOf('restartRemoteStatefulSet(packageId, kubeconfigPath, request, namespace, "nacos", Math.max(600, timeout))'),
    true,
    'Nacos must restart after the imported nacos database exists'
  )
  assert.equal(
    impl.indexOf('waitRemoteNacosConfigReady(packageId, kubeconfigPath, request, namespace, "rk-shared-mybatis.yaml", Math.max(600, timeout))') <
      impl.indexOf('restartRemoteDeployments(packageId, kubeconfigPath, request, namespace, REMOTE_K8S_DEPLOYMENTS, timeout)'),
    true,
    'Workloads must restart only after Nacos can serve imported config'
  )
})

test('remote deploy should push current runtime images before applying target cluster manifests', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const marker of [
    'remote deploy exporting current runtime images to registry',
    'exportRuntimeArtifactsToDirectory(request, packageName, packageId)',
    'runRegistryPushForRuntimeArtifacts(runtimeArtifactDir, request, packageId)',
    'cleanupRuntimeArtifactDirectory(runtimeArtifactDir, packageId)',
    'shouldPushRuntimeImagesBeforeRemoteDeploy',
    'assertRuntimeExportDiskHeadroom(packageId, shouldRelaxRemoteDeployDiskPreflight(request))',
    'parseMinDiskAvailableMb',
    'runtime image export disk preflight warning accepted for registry-only remote deploy',
    'mergeRuntimeRegistryImagesFromPushLogs',
    'docker.io/library/$rest',
    'runtime registry image list recovered from source-node logs'
  ]) {
    assert.equal(impl.includes(marker), true, `remote deploy registry image handoff missing ${marker}`)
  }

  assert.equal(
    impl.indexOf('runRegistryPushForRuntimeArtifacts(runtimeArtifactDir, request, packageId)') <
      impl.indexOf('Files.writeString(manifestPath, buildK8sStackManifest(request)'),
    true,
    'remote deploy must push source runtime images before generating/applying the target manifest'
  )
})

test('source-node runtime exporter should keep per-image failure logs for node misses', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const marker of [
    '[ \\"$candidate_log\\" = \\"$per_image_log\\" ] || rm -f \\"$candidate_log\\"',
    '[ -s \\"$per_image_log\\" ] || echo \\"runtime image export produced no detail for $image\\" > \\"$per_image_log\\"',
    'classify_export_failure \\"$image\\" \\"$per_image_log\\"'
  ]) {
    assert.equal(impl.includes(marker), true, `runtime exporter node-miss log guard missing ${marker}`)
  }

  assert.equal(
    impl.includes('+ "    rm -f \\"$candidate_log\\"\\n"\n'
      + '                + "  done\\n"\n'
      + '                + "  if [ -n \\"$export_source\\" ]; then'),
    false,
    'runtime exporter must not blindly remove per_image_log before classifying expected node misses'
  )
})

test('source-node runtime exporter should scan workload pod nodes in addition to overview nodes', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const marker of [
    'Map<String, AdminK8sNodeVO> nodesByName = new LinkedHashMap<>()',
    'nodesByName.putIfAbsent(item.getName(), item)',
    'loadK8sPodItemsJson().stream()',
    'nodesByName.computeIfAbsent(name, missingNode -> {',
    'return new ArrayList<>(nodesByName.values())'
  ]) {
    assert.equal(impl.includes(marker), true, `runtime exporter node resolver missing ${marker}`)
  }

  assert.equal(
    impl.includes('if (!nodes.isEmpty()) {\n'
      + '            return nodes;\n'
      + '        }\n'
      + '        return loadK8sPodItemsJson().stream()'),
    false,
    'runtime exporter must not stop at overview nodes when workload pods run on additional nodes'
  )
})

test('runtime image export should read all k8s pod pages before resolving nodes and images', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const marker of [
    'loadAllK8sPodItems(client, apiUrl, token, "/api/v1/pods?limit=500")',
    'String continueToken = page.path("metadata").path("continue").asText("")',
    'URLEncoder.encode(continueToken, StandardCharsets.UTF_8)',
    'path = StringUtils.hasText(continueToken)',
    '"/api/v1/pods?limit=500&continue=" +',
    'items.addAll(readK8sPodItems(page.path("items")))'
  ]) {
    assert.equal(impl.includes(marker), true, `runtime pod pagination missing ${marker}`)
  }
})

test('remote k8s deploy should use reachable mirror images for public core middleware', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const marker of [
    'private static final String REMOTE_MYSQL_IMAGE = "m.daocloud.io/docker.io/library/mysql:8.0"',
    'private static final String REMOTE_REDIS_IMAGE = "m.daocloud.io/docker.io/library/redis:7-alpine"',
    'private static final String REMOTE_MINIO_IMAGE = "m.daocloud.io/docker.io/minio/minio:RELEASE.2024-05-10T01-41-38Z"',
    'private static final String REMOTE_NACOS_IMAGE = "registry.example.com/rk-web/nacos-server:v2.1.0-slim"',
    'private static final String REMOTE_MINIO_ACCESS_KEY = "xiaorui"',
    'private static final String REMOTE_MINIO_SECRET_KEY = "change-me"',
    'String mysqlImage = REMOTE_MYSQL_IMAGE',
    'String redisImage = REMOTE_REDIS_IMAGE',
    'String minioImage = REMOTE_MINIO_IMAGE',
    'String nacosImage = REMOTE_NACOS_IMAGE',
    'buildRemoteMysqlImportJobManifest(namespace, REMOTE_MYSQL_IMAGE)',
    'buildAliasService(namespace, "rk-mysql", "mysql", 3306, 3306)',
    'buildAliasService(namespace, "rk-redis", "redis", 6379, 6379)',
    'buildAliasService(namespace, "rk-rabbitmq", "rabbitmq", 5672, 5672)',
    'buildAliasService(namespace, "rk-minio", "minio", 9000, 9000)',
    'buildAliasService(namespace, "rk-xxl-job", "xxl-job", 8080, 8880)',
    'MINIO_ROOT_USER", REMOTE_MINIO_ACCESS_KEY',
    'MINIO_ROOT_PASSWORD", REMOTE_MINIO_SECRET_KEY',
    'mc alias set target http://minio:9000 " + REMOTE_MINIO_ACCESS_KEY + " " + REMOTE_MINIO_SECRET_KEY'
  ]) {
    assert.equal(impl.includes(marker), true, `remote middleware mirror image contract missing ${marker}`)
  }

  for (const staleMarker of [
    'String mysqlImage = imageForRuntimeComponent(request, serviceImages, "mysql:8.0", "mysql", "rk-mysql")',
    'String redisImage = imageForRuntimeComponent(request, serviceImages, "redis:7-alpine", "redis", "rk-redis")',
    'String minioImage = imageForRuntimeComponent(request, serviceImages, "minio/minio:RELEASE.2024-05-10T01-41-38Z", "minio", "rk-minio")',
    'String mysqlImage = imageForRuntimeComponent(request, currentRuntimeImagesByRepository(request), "mysql:8.0", "mysql", "rk-mysql")'
  ]) {
    assert.equal(impl.includes(staleMarker), false, `public core middleware must not depend on runtime registry image ${staleMarker}`)
  }
})

test('remote k8s deploy should explicitly configure xxl-job admin datasource', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const marker of [
    'if ("xxl-job".equals(name))',
    'SPRING_DATASOURCE_URL',
    'jdbc:mysql://mysql:3306/xxl_job',
    'SPRING_DATASOURCE_USERNAME',
    'SPRING_DATASOURCE_PASSWORD',
    'SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE',
    'SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE',
    'serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true',
    'XXL_JOB_ACCESS_TOKEN'
  ]) {
    assert.equal(impl.includes(marker), true, `remote xxl-job datasource env missing ${marker}`)
  }
})

test('remote k8s deploy should avoid missing helper jars and minimal shells', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const marker of [
    'private static final String REMOTE_BUSYBOX_IMAGE = "m.daocloud.io/docker.io/library/busybox:1.36"',
    'copy-mysql-driver',
    'java -Djarmode=layertools -jar /app/app.jar extract',
    "find /tmp/rk-layers -type f -name 'mysql-connector*.jar'",
    'buildPersistentStatefulSet(namespace, "nacos", nacosImage, 8848, rkUserImage, request)',
    '/home/nacos/plugins/mysql',
    'fix-data-permissions',
    'chown -R 1000:0 /usr/share/elasticsearch/data',
    'mountPath: /usr/share/elasticsearch/data',
    'for dir in /migration/minio-payload/*; do',
    'bucket=${dir##*/}',
    'mc mirror --overwrite \\"$dir\\" \\"target/$bucket\\"'
  ]) {
    assert.equal(impl.includes(marker), true, `remote runtime dependency fix missing ${marker}`)
  }

  for (const forbidden of [
    'download-mysql-driver',
    'REMOTE_MAVEN_IMAGE',
    'mvn -q',
    'maven.aliyun.com/repository/public',
    'dependency:copy -Dartifact=mysql:mysql-connector-java:8.0.23',
    '/seata-server/libs/mysql-connector-java-8.0.23.jar',
    'm.daocloud.io/docker.io/nacos/nacos-server:v2.1.0-slim',
    "awk '{print $1}' /migration/minio-payload/minio-manifest.txt",
    'find /migration/minio-payload'
  ]) {
    assert.equal(impl.includes(forbidden), false, `remote deploy should not depend on ${forbidden}`)
  }
})

test('remote migration should create target namespace image pull secret without logging registry password', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const marker of [
    'applyRemoteImagePullSecret(packageId, kubeconfigPath, request, namespace)',
    'Files.createTempFile("rk-remote-image-pull-secret-"',
    'buildRemoteImagePullSecretManifest',
    'kind: Secret',
    'type: kubernetes.io/dockerconfigjson',
    '.dockerconfigjson',
    'resolveDeployPackageRegistryCredentials(registryServer)',
    'remote deploy image pull secret applied',
    'deleteTemporaryKubeconfigQuietly(secretManifestPath)',
    '--docker-password=[REDACTED]'
  ]) {
    assert.equal(impl.includes(marker), true, `remote image pull secret contract missing ${marker}`)
  }
})

test('remote migration UI should expose resumable global migration overlay and one-by-one image migration guidance', async () => {
  const dto = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/dto/adminops/AdminDeployPackageCreateDTO.java')
  const vo = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/vo/adminops/AdminDeployPackageRecordVO.java')
  const schema = await readSource('../../../rk-user/src/main/java/com/tianji/user/config/AdminOpsSchemaUpdater.java')
  const api = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/IAdminOpsService.java')
  const controller = await readSource('../../../rk-user/src/main/java/com/tianji/user/controller/AdminOpsController.java')
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const page = await readSource('../src/views/admin/operation/RemoteMigration.vue')
  const app = await readSource('../src/App.vue')
  const overlay = await readSource('../src/components/GlobalMigrationOverlay.vue')
  const frontendApi = await readSource('../src/api/admin-ops.js')

  for (const field of [
    'streamingMigration',
    'globalMigrationLock',
    'estimatedRemainingSeconds',
    'migrationStartedAt',
    'migrationUpdatedAt',
    'migrationStartedAtMillis',
    'migrationUpdatedAtMillis'
  ]) {
    assert.equal(dto.includes(field) || vo.includes(field), true, `migration DTO/VO missing ${field}`)
  }

  for (const column of [
    'streaming_migration',
    'global_migration_lock',
    'estimated_remaining_seconds',
    'migration_started_at',
    'migration_updated_at'
  ]) {
    assert.equal(schema.includes(column), true, `migration schema missing ${column}`)
  }

  for (const marker of [
    'getActiveGlobalMigration',
    'GET /deploy-package/active-migration',
    '@GetMapping("/deploy-package/active-migration")',
    'computeEstimatedRemainingSeconds',
    'updateRemoteMigrationEta',
    'migration_updated_at = CASE WHEN global_migration_lock = 1 THEN ? ELSE migration_updated_at END',
    'estimated_remaining_seconds = CASE WHEN global_migration_lock = 1 THEN ? ELSE estimated_remaining_seconds END',
    'one-by-one image migration',
    'migrate image then cleanup source tar',
    'global migration lock enabled'
  ]) {
    assert.equal(api.includes(marker) || controller.includes(marker) || impl.includes(marker), true, `migration backend missing ${marker}`)
  }
  assert.equal(
    impl.includes('migration_updated_at = CASE WHEN global_migration_lock = 1 THEN NOW() ELSE migration_updated_at END'),
    false,
    'migration update timestamps must come from the Java runtime timezone, not database NOW()'
  )

  for (const marker of [
    'getActiveGlobalMigration',
    '/api/ops/migration/active',
    'GlobalMigrationOverlay',
    'migrationOverlay',
    '全局迁移进行中',
    '源集群',
    '目标集群',
    '预计剩余',
    '逐个镜像导出、推送、部署并清理',
    '关闭页面后会自动恢复',
    'global-migration-overlay',
    '迁移期间其他租户也会看到此提示'
  ]) {
    assert.equal(
      page.includes(marker) || frontendApi.includes(marker) || app.includes(marker) || overlay.includes(marker),
      true,
      `migration frontend missing ${marker}`
    )
  }
})

test('remote deploy defaults must not pin production domain into new clusters', async () => {
  const deployPage = await readSource('../src/views/admin/operation/DeployPackage.vue')
  const remotePage = await readSource('../src/views/admin/operation/RemoteMigration.vue')
  const dto = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/dto/adminops/AdminDeployPackageCreateDTO.java')
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const source of [deployPage, remotePage]) {
    assert.equal(source.includes("targetDomain: 'example.com'"), false)
    assert.equal(source.includes("ingressHost: 'example.com'"), false)
    assert.match(source, /resolveDefaultTargetDomain\(\)/)
    assert.match(source, /DEFAULT_TARGET_DOMAIN/)
  }

  assert.equal(dto.includes('private String targetDomain = "example.com"'), false)
  assert.match(dto, /private String targetDomain;/)
  assert.equal(impl.includes('trimToDefault(request.getTargetDomain(), "example.com")'), false)
  assert.match(impl, /defaultRemoteDeployDomain\(/)
})

test('ssh deploy frontend runtime templates must not default to old fixed hosts', async () => {
  const remoteCommon = await readOptionalSource('../../../ci/remote/lib/common.sh')
  const devConfig = await readSource('../vite.config.js')
  const nginxConfig = await readSource('../nginx.conf')

  const oldPrivateNode = ['192', '168', '20', '50'].join('.')
  const oldPublicHost = ['203', '0', '113', '10'].join('.')

  for (const source of [remoteCommon, devConfig, nginxConfig].filter(Boolean)) {
    assert.equal(source.includes(oldPrivateNode), false)
    assert.equal(source.includes(oldPublicHost), false)
    assert.equal(source.includes('example.com'), false)
  }

  if (remoteCommon) {
    assert.match(remoteCommon, /RK_FRONTEND_GATEWAY_URL="\$\{RK_FRONTEND_GATEWAY_URL:-http:\/\/rk-gateway:10010\}"/)
    assert.match(remoteCommon, /RK_FRONTEND_MINIO_URL="\$\{RK_FRONTEND_MINIO_URL:-http:\/\/minio:9000\}"/)
  }
  assert.match(devConfig, /127\.0\.0\.1:10010/)
  assert.match(devConfig, /127\.0\.0\.1:9000/)
  assert.match(devConfig, /allowedHosts: true/)
  assert.match(nginxConfig, /proxy_pass http:\/\/rk-gateway:10010;/)
  assert.match(nginxConfig, /proxy_pass http:\/\/minio:9000\//)
})

test('one-key deploy enables delayed RabbitMQ topology required by trade delayed exchange', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  assert.equal(impl.includes('"""'), false, 'rk-user is built with JDK 11, so Java text blocks must not be used')
  assert.match(impl, /rabbitmq_delayed_message_exchange/)
  assert.match(impl, /rabbitmq-plugins enable --offline rabbitmq_delayed_message_exchange/)
  assert.match(impl, /RABBITMQ_DELAYED_EXCHANGE_ARGUMENTS = "arguments='\{\\"x-delayed-type\\":\\"topic\\"\}'"/)
  assert.match(impl, /declare exchange name=trade\.delay\.topic type=x-delayed-message/)
  assert.match(impl, /type=x-delayed-message durable=true " \+ RABBITMQ_DELAYED_EXCHANGE_ARGUMENTS/)
})

test('ssh and remote one-key deploy include local CI services for Jenkins validation', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  assert.match(impl, /REMOTE_GOGS_IMAGE/)
  assert.match(impl, /REMOTE_JENKINS_IMAGE/)
  assert.match(impl, /buildGogsStatefulSet\(/)
  assert.match(impl, /buildJenkinsStatefulSet\(/)
  assert.match(impl, /buildSimpleService\(namespace, "rk-gogs", 3000, 3000\)/)
  assert.match(impl, /buildSimpleService\(namespace, "rk-jenkins", 8080, 8080\)/)
  assert.match(impl, /GOGS_CUSTOM/)
  assert.match(impl, /JENKINS_HOME/)
})

test('backend Jenkins trigger forwards explicit SSH target parameters without hardcoded secrets', async () => {
  const dto = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/dto/adminops/AdminJenkinsBuildRequestDTO.java')
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const jenkinsView = await readSource('../src/views/admin/operation/Jenkins.vue')

  const jenkinsTriggerSection = impl.slice(
    impl.indexOf('public AdminJenkinsBuildTriggerVO triggerJenkinsJob'),
    impl.indexOf('public AdminJenkinsBuildStatusVO getJenkinsBuildStatus')
  )
  const jenkinsNormalizeSection = impl.slice(
    impl.indexOf('private AdminJenkinsBuildRequestDTO normalizeJenkinsBuildRequest'),
    impl.indexOf('private String normalizeServiceList')
  )

  for (const source of [dto, jenkinsTriggerSection, jenkinsNormalizeSection, jenkinsView]) {
    assert.equal(source.includes('192.168.20.30'), false)
    assert.equal(source.includes('203.0.113.10'), false)
    assert.equal(source.includes('change-me'), false)
  }

  assert.match(dto, /private String remoteHost;/)
  assert.match(dto, /private String remotePort;/)
  assert.match(dto, /private String remoteUser;/)
  assert.match(dto, /private String remotePassword;/)
  assert.match(impl, /form\.put\("RK_REMOTE_HOST", normalized\.getRemoteHost\(\)\)/)
  assert.match(impl, /form\.put\("RK_REMOTE_PORT", normalized\.getRemotePort\(\)\)/)
  assert.match(impl, /form\.put\("RK_REMOTE_USER", normalized\.getRemoteUser\(\)\)/)
  assert.match(impl, /form\.put\("RK_REMOTE_PASSWORD", normalized\.getRemotePassword\(\)\)/)
  assert.match(jenkinsView, /remoteHost: ''/)
  assert.match(jenkinsView, /remotePort: ''/)
  assert.match(jenkinsView, /remoteUser: 'root'/)
  assert.match(jenkinsView, /remotePassword: ''/)
  assert.match(jenkinsView, /RK_REMOTE_HOST/)
  assert.match(jenkinsView, /RK_REMOTE_PASSWORD/)
})
