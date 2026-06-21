import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

async function readSource(relativePath) {
  return readFile(path.resolve(__dirname, relativePath), 'utf8')
}

test('remote migration must live on a separate admin page and menu node', async () => {
  const router = await readSource('../src/router/index.js')
  const layout = await readSource('../src/layouts/AdminLayout.vue')
  const updater = await readSource('../../../rk-auth/rk-auth-service/src/main/java/com/tianji/auth/config/AuthMenuCatalogUpdater.java')

  for (const marker of [
    "path: 'operation/remote-migration'",
    "name: 'AdminRemoteMigration'",
    "import('@/views/admin/operation/RemoteMigration.vue')",
    "meta: { title: '远程迁移部署'"
  ]) {
    assert.equal(router.includes(marker), true, `router missing ${marker}`)
  }

  assert.equal(layout.includes("name: '远程迁移部署'"), false, 'AdminLayout must not hard-code the remote migration menu title')
  assert.equal(updater.includes('"远程迁移部署"'), true, 'menu catalog missing remote migration title')
  assert.equal(updater.includes('"/admin/operation/remote-migration"'), true, 'menu catalog missing remote migration path')
  assert.equal(updater.includes('"admin_operation_remote_migration"'), true, 'menu catalog missing remote migration permission')
  assert.match(updater, /"远程迁移部署"[\s\S]*"admin_operation_remote_migration"[\s\S]*"\/admin\/operation\/remote-migration"/)
})

test('deploy package page should only handle offline package export and download', async () => {
  const deployPage = await readSource('../src/views/admin/operation/DeployPackage.vue')

  for (const forbidden of [
    '在线远程部署',
    'kubeconfig-input',
    'createRemoteDeploy',
    'preflightDeployPackage',
    'diagnoseDeployPackage',
    'repairDeployPackage',
    'getDeployPackageKubeconfig',
    'remoteDiagnosis',
    'kubeconfigPlaintext',
    'REPAIR_REMOTE_DEPLOY'
  ]) {
    assert.equal(deployPage.includes(forbidden), false, `DeployPackage.vue must not keep remote migration marker ${forbidden}`)
  }

  for (const marker of [
    '一键打包部署',
    'createDeployPackage',
    'downloadDeployPackage',
    'listDeployPackages',
    '部署包下载',
    'package-log'
  ]) {
    assert.equal(deployPage.includes(marker), true, `DeployPackage.vue missing package marker ${marker}`)
  }
})

test('remote migration page and api should use independent remote-migration endpoints and logs', async () => {
  const remotePage = await readSource('../src/views/admin/operation/RemoteMigration.vue')
  const api = await readSource('../src/api/admin-ops.js')
  const controller = await readSource('../../../rk-user/src/main/java/com/tianji/user/controller/AdminOpsController.java')
  const service = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/IAdminOpsService.java')

  for (const marker of [
    '远程迁移部署',
    '在线 Kubernetes 迁移',
    'Linux SSH 部署',
    'kubeconfig-input',
    'remote-migration-log',
    '迁移日志',
    '国内源',
    'Docker Compose',
    'K8s 环境安装'
  ]) {
    assert.equal(remotePage.includes(marker), true, `RemoteMigration.vue missing ${marker}`)
  }

  for (const marker of [
    'listRemoteMigrations',
    'getRemoteMigrationDetail',
    'getRemoteMigrationKubeconfig',
    'diagnoseRemoteMigration',
    'repairRemoteMigration',
    'createRemoteMigration',
    'preflightRemoteMigration',
    'createLinuxSshMigration',
    '/admin/ops/remote-migration/records',
    '/admin/ops/remote-migration/${id}/kubeconfig',
    '/admin/ops/remote-migration/${id}/diagnose',
    '/admin/ops/remote-migration/${id}/repair',
    '/admin/ops/remote-migration/kubeconfig-deploy',
    '/admin/ops/remote-migration/preflight',
    '/admin/ops/remote-migration/linux-ssh-deploy'
  ]) {
    assert.equal(api.includes(marker), true, `frontend api missing ${marker}`)
  }

  for (const marker of [
    '@GetMapping("/remote-migration/records")',
    '@GetMapping("/remote-migration/{migrationId}")',
    '@GetMapping("/remote-migration/{migrationId}/kubeconfig")',
    '@PostMapping("/remote-migration/{migrationId}/diagnose")',
    '@PostMapping("/remote-migration/{migrationId}/repair")',
    '@PostMapping("/remote-migration/kubeconfig-deploy")',
    '@PostMapping("/remote-migration/preflight")',
    '@PostMapping("/remote-migration/linux-ssh-deploy")',
    'listRemoteMigrations()',
    'createLinuxSshMigration'
  ]) {
    assert.equal(controller.includes(marker) || service.includes(marker), true, `backend remote migration contract missing ${marker}`)
  }
})

test('linux ssh deployment should run asynchronously with progress and without persisting the password', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const pom = await readSource('../../../rk-user/pom.xml')

  const start = impl.indexOf('public AdminDeployPackageRecordVO createLinuxSshMigration')
  const end = impl.indexOf('private void startRemoteDeployAsync', start)
  assert.notEqual(start, -1, 'createLinuxSshMigration method missing')
  assert.notEqual(end, -1, 'startRemoteDeployAsync marker missing')
  const method = impl.slice(start, end)

  assert.match(pom, /<groupId>com\.github\.mwiede<\/groupId>[\s\S]*<artifactId>jsch<\/artifactId>/, 'rk-user must use the maintained JSch fork for modern OpenSSH compatibility')
  assert.match(impl, /import\s+com\.jcraft\.jsch\./, 'AdminOpsServiceImpl must use a Java SSH client')
  assert.match(method, /"RUNNING"/, 'Linux SSH deployment must create a RUNNING record')
  assert.match(method, /startLinuxSshDeployAsync\(/, 'Linux SSH deployment must start an async executor')
  assert.doesNotMatch(method, /"SUCCESS"\s*,\s*100/, 'Linux SSH deployment must not immediately mark the task as SUCCESS/100')
  assert.doesNotMatch(method, /"linux ssh deployment plan generated"/, 'Linux SSH deployment must not stop at plan generation')
  assert.match(impl, /sanitizeLinuxSshOutput\(/, 'Linux SSH output must be sanitized before persistence or exception logging')
  assert.doesNotMatch(impl, /log\.error\("linux ssh deploy failed, packageId=\{\}", packageId, e\)/, 'Linux SSH failure logging must not write raw remote output through the exception object')
  assert.doesNotMatch(impl, /limitText\(text,\s*4000\)/, 'Linux SSH stage exceptions must not include unsanitized raw output')
  assert.match(impl, /formatLinuxSshStageFailure\(/, 'Linux SSH stage failures must be normalized before persistence')
  assert.match(impl, /exit code -1 usually means the SSH channel was interrupted/, 'Linux SSH channel interruptions must tell the operator to check target reboot or tunnel drops')

  const storageStart = impl.indexOf('private Map<String, Object> buildLinuxSshMigrationRequestForStorage')
  const storageEnd = impl.indexOf('private String buildLinuxSshMigration', storageStart)
  assert.notEqual(storageStart, -1, 'Linux SSH request storage sanitizer missing')
  assert.notEqual(storageEnd, -1, 'Linux SSH storage sanitizer end marker missing')
  const storageMethod = impl.slice(storageStart, storageEnd)
  assert.match(storageMethod, /"sshPassword",\s*"\[REDACTED\]"/, 'stored SSH request must redact the password')
  assert.doesNotMatch(storageMethod, /"sshPassword",\s*request\.getSshPassword\(\)/, 'stored SSH request must not contain the plaintext password')

  for (const marker of [
    'runLinuxSshDeploy',
    'runLinuxSshStage',
    'appendLinuxSshOutput',
    'linux ssh checking connection',
    'linux ssh installing k8s runtime',
    'linux ssh deploying application stack'
  ]) {
    assert.equal(impl.includes(marker), true, `Linux SSH executor missing ${marker}`)
  }
})

test('linux ssh deployment should force registry-only image artifacts while still pushing runtime images', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const page = await readSource('../src/views/admin/operation/RemoteMigration.vue')

  const start = impl.indexOf('public AdminDeployPackageRecordVO createLinuxSshMigration')
  const end = impl.indexOf('private void startLinuxSshDeployAsync', start)
  assert.notEqual(start, -1, 'createLinuxSshMigration method missing')
  assert.notEqual(end, -1, 'startLinuxSshDeployAsync marker missing')
  const method = impl.slice(start, end)

  assert.match(page, /imageArtifactMode: 'registry'/, 'Linux SSH page should default to registry-only image artifacts')
  assert.match(method, /request\.setImageArtifactMode\(IMAGE_ARTIFACT_REGISTRY\)/, 'Linux SSH backend should force registry-only image artifacts')
  assert.match(method, /request\.setExportRuntimeArtifacts\(true\)/, 'Linux SSH registry-only deploy must still export and push current runtime images')
  assert.doesNotMatch(method, /request\.setExportRuntimeArtifacts\(false\)/, 'Linux SSH deploy must not skip runtime export because generated manifests reference current image tags')
  assert.doesNotMatch(method, /request\.setImageArtifactMode\(IMAGE_ARTIFACT_BOTH\)/, 'Linux SSH backend must not default to both image artifacts')
})

test('linux ssh connection should retry transient banner and reset failures before failing the task', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  assert.match(impl, /LINUX_SSH_CONNECT_MAX_ATTEMPTS\s*=\s*[3-9]/, 'Linux SSH connection must retry at least 3 attempts')
  assert.match(impl, /LINUX_SSH_CONNECT_RETRY_DELAY_MS/, 'Linux SSH connection retry delay constant missing')
  assert.match(impl, /openLinuxSshSession\(request,\s*packageId\)/, 'Linux SSH deploy must pass packageId into connection retry logging')

  const start = impl.indexOf('private Session openLinuxSshSession(AdminDeployPackageCreateDTO request, Long packageId)')
  const end = impl.indexOf('private String runLinuxSshStage', start)
  assert.notEqual(start, -1, 'retry-aware Linux SSH session opener missing')
  assert.notEqual(end, -1, 'Linux SSH session opener end marker missing')
  const method = impl.slice(start, end)

  assert.match(method, /for\s*\(\s*int attempt\s*=\s*1\s*;\s*attempt\s*<=\s*LINUX_SSH_CONNECT_MAX_ATTEMPTS\s*;\s*attempt\+\+\s*\)/, 'Linux SSH connection must use a bounded retry loop')
  assert.match(method, /isTransientLinuxSshConnectFailure\(/, 'Linux SSH connection retry must be limited to transient connection failures')
  assert.match(method, /appendLinuxSshOutput\(packageId,[\s\S]*linux ssh connection retry/, 'Linux SSH connection retry must be visible in task logs')
  assert.match(method, /Thread\.sleep\(LINUX_SSH_CONNECT_RETRY_DELAY_MS/, 'Linux SSH connection retry must pause before retrying')
  assert.match(method, /session\.disconnect\(\)/, 'Failed SSH sessions must be closed between retry attempts')
  assert.match(impl, /Connection reset|SSH protocol banner|Connection timed out|SocketTimeoutException/, 'Transient Linux SSH failure classifier must include observed reset and banner failures')
})

test('linux ssh long running exec sessions should use real JSch keepalive packets', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const start = impl.indexOf('private Session openLinuxSshSession(AdminDeployPackageCreateDTO request, Long packageId)')
  const end = impl.indexOf('private Session reopenLinuxSshSessionForPackageUpload', start)
  assert.notEqual(start, -1, 'openLinuxSshSession missing')
  assert.notEqual(end, -1, 'openLinuxSshSession end marker missing')
  const method = impl.slice(start, end)

  assert.match(method, /session\.setServerAliveInterval\(15000\)/, 'JSch keepalive must be enabled through the Session API, not only config strings')
  assert.match(method, /session\.setServerAliveCountMax\(\d+\)/, 'JSch keepalive must tolerate several missed replies during long kubectl waits')
})

test('linux ssh k8s deploy must restore data through cluster workloads and fail on import errors', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const minioImportStart = impl.indexOf('private String buildMinioImportScript()')
  const minioImportEnd = impl.indexOf('private String buildComposeEnvExample', minioImportStart)
  assert.notEqual(minioImportStart, -1, 'MinIO import script builder missing')
  assert.notEqual(minioImportEnd, -1, 'MinIO import script builder end marker missing')
  const minioImportScript = impl.slice(minioImportStart, minioImportEnd)

  assert.doesNotMatch(impl, /bash scripts\/import-databases\.sh \|\| true/, 'database import failures must not be swallowed')
  assert.doesNotMatch(impl, /bash scripts\/import-minio\.sh \|\| true/, 'MinIO import failures must not be swallowed')
  assert.doesNotMatch(impl, /mc not found, skip MinIO import/, 'MinIO import must not silently skip when host mc is absent')
  assert.doesNotMatch(impl, /gzip -dc databases\/all-current-data\.sql\.gz \| mysql --binary-mode=1/, 'K8s database import must not require a host mysql client')
  assert.doesNotMatch(impl, /kubectl -n "\$NAMESPACE" exec -i statefulset\/mysql/, 'K8s database import must not stream large dumps through kubectl exec -i')
  assert.doesNotMatch(impl, /apply -f - <<YAML/, 'K8s database import job manifest must not use an unquoted heredoc that expands pod-side shell variables')
  assert.doesNotMatch(impl, /name: \$\{MYSQL_IMPORT_JOB\}/, 'K8s database import job name must use $MYSQL_IMPORT_JOB so the envsubst whitelist renders it')
  assert.doesNotMatch(impl, /image: \$\{REMOTE_MYSQL_IMAGE\}/, 'K8s database import job image must use $REMOTE_MYSQL_IMAGE so the envsubst whitelist renders it')
  assert.doesNotMatch(impl, /value: \\"\$\{MYSQL_ROOT_PASSWORD\}\\"/, 'K8s database import job password must use $MYSQL_ROOT_PASSWORD so the envsubst whitelist renders it')
  assert.doesNotMatch(impl, /path: \$\{ROOT_DIR\}/, 'K8s database import hostPath must use $ROOT_DIR so the envsubst whitelist renders it')
  assert.doesNotMatch(impl, /MINIO_MC_IMAGE=\$\{MINIO_MC_IMAGE:-m\.daocloud\.io\/docker\.io\/minio\/mc:latest\}/, 'K8s MinIO import must not pull an unpinned mc:latest image')
  assert.doesNotMatch(minioImportScript, /MINIO_ACCESS_KEY=\$\{MINIO_ACCESS_KEY:-minioadmin\}/, 'K8s MinIO import must default to the generated target MinIO access key')
  assert.doesNotMatch(minioImportScript, /MINIO_SECRET_KEY=\$\{MINIO_SECRET_KEY:-minioadmin\}/, 'K8s MinIO import must default to the generated target MinIO secret key')
  assert.doesNotMatch(minioImportScript, /tar -C minio -cf - \| kubectl/, 'K8s MinIO import must not stream object data through kubectl stdin because mc images may not contain tar')
  assert.doesNotMatch(minioImportScript, /tar -C \/work\/minio -xf -/, 'K8s MinIO import pod must not require tar inside the mc client image')
  assert.doesNotMatch(minioImportScript, /kubectl -n "\$NAMESPACE" run "\$pod"/, 'K8s MinIO import must use a waitable Job instead of kubectl run --rm')
  assert.doesNotMatch(minioImportScript, /find \/rk-import\/minio\/buckets/, 'K8s MinIO import must not require find inside the mc client image')
  assert.match(impl, /mysql --binary-mode=1 -hmysql -P3306 -uroot/, 'K8s database import job must force TCP through the mysql service instead of relying on a Unix socket')
  assert.match(impl, /mysql_fast_import_stream\(\)[\s\S]*SET autocommit=0; SET unique_checks=0; SET foreign_key_checks=0;[\s\S]*COMMIT; SET foreign_key_checks=1; SET unique_checks=1; SET autocommit=1;/, 'K8s database import must wrap large snapshots in a fast transaction and disable checks only for the import stream')
  assert.doesNotMatch(impl, /gzip -dc \/rk-import\/databases\/all-current-data\.sql\.gz \| mysql --binary-mode=1 -hmysql -P3306 -uroot/, 'Linux SSH k8s import must not stream snapshots through bare per-statement mysql imports')
  assert.doesNotMatch(impl, /kubectl -n "\$NAMESPACE" exec -i statefulset\/mysql -- sh -lc 'MYSQL_PWD="\$\{MYSQL_ROOT_PASSWORD:-123456\}" mysql --binary-mode=1 -uroot/, 'K8s database import must not use the mysql client default socket path')

  for (const marker of [
    'wait_for_k8s_resource statefulset/mysql',
    'wait_for_mysql_tcp',
    'wait_for_k8s_resource statefulset/minio',
    'restart_after_data_import',
    'kubectl -n \\"$NAMESPACE\\" rollout restart statefulset/nacos',
    'APPLICATION_DEPLOYS=\\"rk-gateway rk-auth rk-user rk-search rk-file rk-message rk-content rk-pay rk-trade rk-exam rk-activity rk-data\\"',
    'APPLICATION_STARTUP_DEPLOYS=\\"seata xxl-job sentinel $APPLICATION_DEPLOYS rk-web-frontend\\"',
    'capture_existing_application_deployments',
    'restart_existing_application_deployments \\"$existing_app_deployments\\"',
    'MYSQL_IMPORT_JOB=${MYSQL_IMPORT_JOB:-rk-linux-ssh-mysql-import}',
    'REMOTE_MYSQL_IMAGE=${REMOTE_MYSQL_IMAGE:-m.daocloud.io/docker.io/library/mysql:8.0}',
    'run_k8s_mysql_import_job',
    'export MYSQL_IMPORT_JOB REMOTE_MYSQL_IMAGE MYSQL_ROOT_PASSWORD ROOT_DIR',
    "envsubst '$MYSQL_IMPORT_JOB $REMOTE_MYSQL_IMAGE $MYSQL_ROOT_PASSWORD $ROOT_DIR'",
    'name: $MYSQL_IMPORT_JOB',
    'image: $REMOTE_MYSQL_IMAGE',
    'value: \\"$MYSQL_ROOT_PASSWORD\\"',
    'hostPath:',
    'path: $ROOT_DIR',
    'mountPath: /rk-import',
    'mysql_fast_import_stream',
    'SET autocommit=0; SET unique_checks=0; SET foreign_key_checks=0;',
    'COMMIT; SET foreign_key_checks=1; SET unique_checks=1; SET autocommit=1;',
    'gzip -dc /rk-import/databases/all-current-data.sql.gz | mysql_fast_import_stream',
    "cat <<'YAML'",
    'kubectl -n \\"$NAMESPACE\\" apply -f -',
    'kubectl -n \\"$NAMESPACE\\" wait --for=condition=complete job/\\"$MYSQL_IMPORT_JOB\\"',
    'kubectl -n \\"$NAMESPACE\\" logs job/\\"$MYSQL_IMPORT_JOB\\"',
    'MINIO_ACCESS_KEY=${MINIO_ACCESS_KEY:-" + REMOTE_MINIO_ACCESS_KEY + "}',
    'MINIO_SECRET_KEY=${MINIO_SECRET_KEY:-" + REMOTE_MINIO_SECRET_KEY + "}',
    'MINIO_MC_IMAGE=${MINIO_MC_IMAGE:-" + DEFAULT_MINIO_CLIENT_IMAGE + "}',
    'MINIO_IMPORT_JOB=${MINIO_IMPORT_JOB:-rk-linux-ssh-minio-import}',
    'run_k8s_minio_import_job',
    'export MINIO_IMPORT_JOB MINIO_MC_IMAGE MINIO_ENDPOINT MINIO_ACCESS_KEY MINIO_SECRET_KEY ROOT_DIR',
    "envsubst '$MINIO_IMPORT_JOB $MINIO_MC_IMAGE $MINIO_ENDPOINT $MINIO_ACCESS_KEY $MINIO_SECRET_KEY $ROOT_DIR'",
    'name: $MINIO_IMPORT_JOB',
    'image: $MINIO_MC_IMAGE',
    'mountPath: /rk-import',
    'path: $ROOT_DIR',
    'for dir in /rk-import/minio/buckets/*; do',
    'bucket=${dir%/}',
    'bucket=${bucket##*/}',
    'mc mirror --overwrite \\"$dir\\" \\"rkminio/$bucket\\"',
    'mc anonymous set download \\"rkminio/$bucket\\"',
    'kubectl -n \\"$NAMESPACE\\" wait --for=condition=complete job/\\"$MINIO_IMPORT_JOB\\"',
    'kubectl -n \\"$NAMESPACE\\" logs job/\\"$MINIO_IMPORT_JOB\\"'
  ]) {
    assert.equal(impl.includes(marker), true, `cluster data restore script missing ${marker}`)
  }
})

test('linux ssh k8s deploy must import data before starting nacos and application workloads', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const start = impl.indexOf('private String buildK8sInstallScript()')
  const end = impl.indexOf('private String buildOnlinePackageScript()', start)
  assert.notEqual(start, -1, 'K8s install script builder missing')
  assert.notEqual(end, -1, 'K8s install script builder end marker missing')
  const script = impl.slice(start, end)

  for (const marker of [
    'apply_infra_manifests',
    'apply_nacos_manifest',
    'wait_for_nacos_config',
    'apply_application_manifests',
    'pre_pull_infra_images',
    'pre_pull_application_images',
    'k8s/rk-web-infra.yaml',
    'k8s/rk-web-nacos.yaml',
    'k8s/rk-web-apps.yaml'
  ]) {
    assert.equal(script.includes(marker), true, `linux ssh staged k8s install missing ${marker}`)
  }

  assert.equal(
    script.includes('kubectl apply -n \\"$NAMESPACE\\" -f k8s/rk-web-stack.yaml\\n'),
    false,
    'Linux SSH K8s install must not apply the full stack before database import'
  )
  const execution = script.slice(script.indexOf('echo \\"[1/5] importing image tar files when present\\"'))
  const infraPrePullCall = execution.indexOf('pre_pull_infra_images\\n')
  const infraApplyCall = execution.indexOf('apply_infra_manifests\\n')
  const mysqlRolloutCall = execution.indexOf('wait_for_k8s_resource statefulset/mysql')
  const mysqlTcpWaitCall = execution.indexOf('wait_for_mysql_tcp\\n')
  const databaseImportCall = execution.indexOf('bash scripts/import-databases.sh')
  const restartAfterImportCall = execution.indexOf('restart_after_data_import\\n')
  const restartFunction = script.slice(script.indexOf('restart_after_data_import()'))
  const applicationPrePullCall = restartFunction.indexOf('pre_pull_application_images\\n')
  const nacosApplyCall = restartFunction.indexOf('apply_nacos_manifest\\n')
  const nacosConfigWaitCall = restartFunction.indexOf('wait_for_nacos_config\\n')
  const applicationApplyCall = restartFunction.indexOf('apply_application_manifests \\"$existing_app_deployments\\"')

  assert.ok(
    infraPrePullCall >= 0 && infraPrePullCall < databaseImportCall,
    'database import should only wait for infrastructure image pre-pulls'
  )
  assert.ok(
    infraApplyCall >= 0 && databaseImportCall >= 0 && infraApplyCall < databaseImportCall,
    'database import must wait until infrastructure manifests are applied'
  )
  assert.ok(
    mysqlRolloutCall >= 0 && mysqlTcpWaitCall > mysqlRolloutCall && mysqlTcpWaitCall < databaseImportCall,
    'database import must wait for MySQL TCP connectivity after StatefulSet rollout'
  )
  assert.ok(
    databaseImportCall < restartAfterImportCall,
    'Nacos and applications must start only after the imported nacos database exists'
  )
  assert.ok(
    nacosApplyCall >= 0 && nacosApplyCall < nacosConfigWaitCall,
    'Nacos config check must run after Nacos manifest is applied'
  )
  assert.ok(
    nacosConfigWaitCall >= 0 && applicationPrePullCall >= 0 && nacosConfigWaitCall < applicationPrePullCall,
    'application image pre-pulls must start only after Nacos can serve imported config'
  )
  assert.ok(
    applicationPrePullCall < applicationApplyCall,
    'application workloads must start only after application images are pre-pulled'
  )
  assert.ok(
    applicationApplyCall >= 0,
    'application workloads must start only after Nacos can serve imported config'
  )
})

test('linux ssh first-time k8s app apply must not immediately double replicas through rollout restart', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const start = impl.indexOf('private String buildK8sInstallScript()')
  const end = impl.indexOf('private String buildOnlinePackageScript()', start)
  assert.notEqual(start, -1, 'K8s install script builder missing')
  assert.notEqual(end, -1, 'K8s install script builder end marker missing')
  const script = impl.slice(start, end)
  const restartStart = script.indexOf('restart_after_data_import()')
  const restartEnd = script.indexOf('echo \\"[1/5] importing image tar files when present\\"', restartStart)
  assert.notEqual(restartStart, -1, 'restart_after_data_import function missing')
  assert.notEqual(restartEnd, -1, 'restart_after_data_import end marker missing')
  const restartFunction = script.slice(restartStart, restartEnd)

  assert.match(restartFunction, /existing_app_deployments=\$\(capture_existing_application_deployments\)/, 'script must capture applications that existed before applying manifests')
  assert.match(restartFunction, /apply_application_manifests/, 'script must still apply application manifests')
  assert.match(restartFunction, /restart_existing_application_deployments \\"\$existing_app_deployments\\"/, 'script must only restart deployments that existed before the apply')
  assert.doesNotMatch(
    restartFunction,
    /kubectl -n "\$NAMESPACE" rollout restart deploy\/rk-gateway deploy\/rk-auth deploy\/rk-user/,
    'fresh Linux SSH deploy must not restart all applications immediately after creating them because single-node targets briefly double memory'
  )
  assert.match(script, /no existing application deployments to restart after data import/, 'fresh deployment skip should be visible in logs')
})

test('linux ssh k8s app startup should be staged for small single-node targets', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const start = impl.indexOf('private String buildK8sInstallScript()')
  const end = impl.indexOf('private String buildOnlinePackageScript()', start)
  assert.notEqual(start, -1, 'K8s install script builder missing')
  assert.notEqual(end, -1, 'K8s install script builder end marker missing')
  const script = impl.slice(start, end)

  for (const marker of [
    'applying application manifests at zero replicas for staged startup',
    'sed \'s/^  replicas: 1$/  replicas: 0/\' k8s/rk-web-apps.yaml',
    'start_application_deployments_sequentially',
    'starting application deployments sequentially to avoid single-node memory pressure',
    'kubectl -n \\"$NAMESPACE\\" scale \\"deploy/$deploy\\" --replicas=1',
    'wait_for_k8s_resource \\"deployment.apps/$deploy\\" 15m',
    'rk-web-frontend'
  ]) {
    assert.equal(script.includes(marker), true, `staged app startup missing ${marker}`)
  }

  const zeroApplyCall = script.indexOf('applying application manifests at zero replicas for staged startup')
  const sequentialStartCall = script.indexOf('start_application_deployments_sequentially')
  assert.ok(zeroApplyCall >= 0 && sequentialStartCall > zeroApplyCall, 'application manifests must be applied at zero replicas before sequential startup')
})

test('linux ssh k8s existing app restart should also be sequential', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const start = impl.indexOf('private String buildK8sInstallScript()')
  const end = impl.indexOf('private String buildOnlinePackageScript()', start)
  assert.notEqual(start, -1, 'K8s install script builder missing')
  assert.notEqual(end, -1, 'K8s install script builder end marker missing')
  const script = impl.slice(start, end)
  const restartStart = script.indexOf('restart_existing_application_deployments()')
  const restartEnd = script.indexOf('print_resource_diagnostics()', restartStart)
  assert.notEqual(restartStart, -1, 'restart_existing_application_deployments function missing')
  assert.notEqual(restartEnd, -1, 'restart_existing_application_deployments end marker missing')
  const restartFunction = script.slice(restartStart, restartEnd)

  assert.match(restartFunction, /restarting existing application deployments sequentially after data import/, 'existing app restart must disclose sequential mode')
  assert.match(restartFunction, /kubectl -n \\"\$NAMESPACE\\" rollout restart \\"deploy\/\$deploy\\"/, 'existing app restart must restart one deployment at a time')
  assert.match(restartFunction, /wait_for_k8s_resource \\"deployment\.apps\/\$deploy\\" 15m/, 'existing app restart must wait each deployment before the next')
  assert.doesNotMatch(
    restartFunction,
    /while IFS= read -r deploy; do \[ -z "\$deploy" \] && continue; kubectl -n "\$NAMESPACE" rollout restart "deploy\/\$deploy"; done/,
    'existing app restart must not fire all rollout restarts without waiting'
  )
})

test('linux ssh k8s wait failures should print actionable diagnostics', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const start = impl.indexOf('private String buildK8sInstallScript()')
  const end = impl.indexOf('private String buildOnlinePackageScript()', start)
  assert.notEqual(start, -1, 'K8s install script builder missing')
  assert.notEqual(end, -1, 'K8s install script builder end marker missing')
  const script = impl.slice(start, end)

  for (const marker of [
    'print_resource_diagnostics()',
    'diagnostics for $resource',
    'kubectl -n \\"$NAMESPACE\\" describe \\"$resource\\" || true',
    'kubectl -n \\"$NAMESPACE\\" get events --sort-by=.lastTimestamp | tail -120 || true',
    'kubectl -n \\"$NAMESPACE\\" get pods -o wide || true',
    'print_resource_diagnostics \\"$resource\\"'
  ]) {
    assert.equal(script.includes(marker), true, `wait failure diagnostics missing ${marker}`)
  }
})

test('linux ssh k3s deploy should avoid docker hub sandbox pulls in domestic installs', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const marker of [
    'mkdir -p /etc/rancher/k3s',
    'cat >/etc/rancher/k3s/registries.yaml',
    'docker.m.daocloud.io',
    'registry.k8s.io/pause:3.6',
    'K3S_PAUSE_IMAGE=${K3S_PAUSE_IMAGE:-" + pauseImage + "}',
    '--pause-image $K3S_PAUSE_IMAGE'
  ]) {
    assert.equal(impl.includes(marker), true, `linux ssh k3s domestic install missing ${marker}`)
  }
})

test('linux ssh domestic deploy should repair target DNS before package download and k3s install', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const marker of [
    'ensure_linux_ssh_dns',
    'RK_LINUX_SSH_DNS_SERVERS',
    'getent hosts mirrors.aliyun.com',
    'getent hosts example.com',
    'linux ssh DNS check passed',
    'linux ssh DNS repair applied',
    'ensure_linux_ssh_dns',
    'linux ssh downloading deploy package from public MinIO URL'
  ]) {
    assert.equal(impl.includes(marker), true, `linux ssh domestic DNS repair missing ${marker}`)
  }

  const prepareStart = impl.indexOf('private String buildLinuxSshPrepareScript')
  const prepareEnd = impl.indexOf('private String buildLinuxSshDockerInstallScript', prepareStart)
  const prepareScript = impl.slice(prepareStart, prepareEnd)
  assert.equal(
    prepareScript.includes('if [ \\"$DOMESTIC_MIRROR\\" = \\"true\\" ]; then ensure_linux_ssh_dns; fi'),
    true,
    'target DNS must be repaired during prepare stage for domestic Linux SSH deploys'
  )

  const downloadStart = impl.indexOf('private String buildLinuxSshPackageDownloadScript')
  const downloadEnd = impl.indexOf('private void cleanupLinuxSshPackageDownload', downloadStart)
  const downloadScript = impl.slice(downloadStart, downloadEnd)
  assert.match(
    downloadScript,
    /ensure_linux_ssh_dns[\s\S]*curl -fL --retry 2 --retry-delay 3[\s\S]*--speed-limit/,
    'target DNS must be repaired immediately before the large package download'
  )
})

test('linux ssh k3s deploy should wait for system storage before applying workloads', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const marker of [
    'pre_pull_k3s_system_images',
    'wait_for_k3s_system_deployments',
    'kubectl -n kube-system rollout status deployment/local-path-provisioner',
    'kubectl -n kube-system rollout status deployment/coredns',
    'kubectl get storageclass local-path'
  ]) {
    assert.equal(impl.includes(marker), true, `linux ssh k3s install must wait for storage readiness: ${marker}`)
  }
})

test('linux ssh k3s deploy should trust pod and service cidrs when firewalld is active', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const marker of [
    'configure_k3s_firewalld',
    'firewall-cmd --permanent --zone=trusted --add-source=10.42.0.0/16',
    'firewall-cmd --permanent --zone=trusted --add-source=10.43.0.0/16',
    'firewall-cmd --reload',
    'k3s firewalld trusted pod/service cidrs configured'
  ]) {
    assert.equal(impl.includes(marker), true, `linux ssh k3s firewalld hardening missing ${marker}`)
  }
})

test('linux ssh k3s deploy should make local-path helper image ready before workloads', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const marker of [
    'LOCAL_PATH_HELPER_IMAGE=${LOCAL_PATH_HELPER_IMAGE:-',
    'patch_local_path_helper_image',
    'kubectl -n kube-system patch configmap local-path-config',
    'kubectl -n kube-system rollout restart deployment/local-path-provisioner',
    'pre-pulling k3s local-path helper image: $LOCAL_PATH_HELPER_IMAGE',
    'verify_local_path_pvc_provisioning',
    'rk-local-path-preflight'
  ]) {
    assert.equal(impl.includes(marker), true, `linux ssh k3s install must prepare local-path helper before workloads: ${marker}`)
  }
})

test('linux ssh long running stages should heartbeat while remote output is quiet', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const marker of [
    'lastHeartbeatAt',
    'linux ssh stage heartbeat',
    'updateDeployPackageStep(packageId, heartbeatProgress, currentStep, null, currentStep + " heartbeat"'
  ]) {
    assert.equal(impl.includes(marker), true, `linux ssh stage heartbeat missing ${marker}`)
  }
})

test('linux ssh application stack heartbeats should advance public progress and eta before completion', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const marker of [
    'computeLinuxSshStageHeartbeatProgress',
    'int heartbeatProgress = computeLinuxSshStageHeartbeatProgress(progress, nextProgress',
    'updateDeployPackageStep(packageId, heartbeatProgress, currentStep, null, currentStep + " heartbeat"',
    'runLinuxSshStage(session, packageId, 82, 93, "linux ssh deploying application stack"'
  ]) {
    assert.equal(impl.includes(marker), true, `linux ssh application stack progress heartbeat missing ${marker}`)
  }
})

test('linux ssh k8s deploy package should apply registry pull secret before workloads', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const marker of [
    'buildPackageImagePullSecretManifest(request)',
    'addZipEntry(zip, "k8s/rk-image-pull-secret.yaml"',
    'kubectl apply -n \\"$NAMESPACE\\" -f k8s/rk-image-pull-secret.yaml',
    'image pull secret applied from package'
  ]) {
    assert.equal(impl.includes(marker), true, `linux ssh k8s package pull secret missing ${marker}`)
  }
})

test('linux ssh k8s deploy package should serially pre-pull registry images before workloads', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const marker of [
    'pre_pull_package_images',
    'images/registry-image-list.txt',
    'rk-image-prepull',
    'imagePullSecrets',
    "jsonpath='{.status.containerStatuses[0].imageID}'"
  ]) {
    assert.equal(impl.includes(marker), true, `linux ssh k8s package image pre-pull missing ${marker}`)
  }

  const executionStart = impl.indexOf('echo \\"[1/5] importing image tar files when present\\"')
  const execution = impl.slice(executionStart)
  const prePullCall = execution.indexOf('pre_pull_infra_images\\n')
  const workloadApply = execution.indexOf('restart_after_data_import\\n')
  assert.ok(executionStart >= 0, 'linux ssh k8s package execution block missing')
  assert.ok(prePullCall >= 0, 'linux ssh k8s package image pre-pull call missing')
  assert.ok(workloadApply >= 0, 'linux ssh k8s workload apply call missing')
  assert.ok(prePullCall < workloadApply, 'package images must be pre-pulled before applying workload manifests')
})

test('linux ssh application image list should not pre-pull platform runtime images', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const methodStart = impl.indexOf('private String buildK8sApplicationImageList')
  const methodEnd = impl.indexOf('private String readRuntimeRegistryImageList', methodStart)
  assert.notEqual(methodStart, -1, 'buildK8sApplicationImageList method missing')
  assert.notEqual(methodEnd, -1, 'buildK8sApplicationImageList method end marker missing')
  const method = impl.slice(methodStart, methodEnd)

  assert.equal(method.includes('resolveK8sApplicationImages'), true, 'application image list must be generated from actual RK application images')
  assert.equal(method.includes('buildPackageImageList'), false, 'application image list must not reuse the full package registry image list')
  assert.equal(method.includes('readRuntimeRegistryImageList'), true, 'application image list may inspect runtime registry output for pushed tags')
  assert.equal(method.includes('!isDeployPackagePlatformImage(image)'), true, 'application image list must filter Rainbond/KubeVirt/platform images')
  assert.equal(method.includes('isDeployPackageBusinessRuntimeImage(image)'), true, 'application image list must keep only RK business/runtime images from registry output')
  assert.equal(method.includes('DEFAULT_SENTINEL_DASHBOARD_IMAGE'), true, 'application image list must still include Sentinel dashboard')
  assert.equal(method.includes('REMOTE_GOGS_IMAGE'), true, 'application image list must still include migrated local Gogs')
  assert.equal(method.includes('REMOTE_JENKINS_IMAGE'), true, 'application image list must still include migrated local Jenkins')
})

test('linux ssh k8s image pre-pull should retry transient registry dns and pull failures', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const start = impl.indexOf('pre_pull_package_images() {')
  const end = impl.indexOf('pre_pull_infra_images()', start)
  assert.notEqual(start, -1, 'pre_pull_package_images function missing')
  assert.notEqual(end, -1, 'pre_pull_package_images function end marker missing')
  const script = impl.slice(start, end)

  for (const marker of [
    'local max_attempts=5',
    'local attempt=1',
    'while [ \\"$attempt\\" -le \\"$max_attempts\\" ]; do',
    'retrying package image pre-pull',
    'crictl pull \\"$image\\"',
    'kubectl -n \\"$NAMESPACE\\" delete pod rk-image-prepull --ignore-not-found=true',
    'kubectl -n \\"$NAMESPACE\\" describe pod rk-image-prepull || true',
    'ErrImagePull|ImagePullBackOff|RegistryUnavailable|TLSHandshakeTimeout',
    'attempt=$((attempt + 1))'
  ]) {
    assert.equal(script.includes(marker), true, `pre-pull retry script missing ${marker}`)
  }

  assert.doesNotMatch(
    script,
    /grep -Eiq 'ErrImagePull\|ImagePullBackOff\|InvalidImageName\|CreateContainerConfigError'; then kubectl -n "\$NAMESPACE" describe pod rk-image-prepull \|\| true; exit 1;/,
    'pre-pull must not fail immediately on transient ErrImagePull/ImagePullBackOff'
  )
})

test('platform stateful archives must be staged and gzip-validated before entering deploy zip', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  const start = impl.indexOf('private long exportPlatformPodDirectoryToZip')
  const end = impl.indexOf('private long addProcessOutputToZip', start)
  assert.notEqual(start, -1, 'platform directory export method missing')
  assert.notEqual(end, -1, 'platform directory export method end marker missing')
  const method = impl.slice(start, end)

  assert.match(method, /stageProcessOutputToGzipFile\(/, 'platform export must first stage kubectl tar output to a temporary gzip file')
  assert.match(method, /validateGzipArchive\(/, 'platform export must validate gzip integrity before adding it to the deploy zip')
  assert.match(method, /copyFileToZip\(/, 'platform export must copy only a verified archive into the deploy zip')
  assert.match(method, /deleteQuietly\(stagedArchive\)/, 'platform export must remove temporary staged archives')
  assert.doesNotMatch(method, /addProcessOutputToZip\(/, 'platform export must not stream kubectl output directly into the final zip entry')

  for (const marker of [
    'Files.createTempFile("rk-platform-export-"',
    'private long stageProcessOutputToGzipFile',
    'private void validateGzipArchive',
    'new GZIPInputStream',
    'private long copyFileToZip',
    'export platform data gzip validation failed'
  ]) {
    assert.equal(impl.includes(marker), true, `implementation missing ${marker}`)
  }
})

test('linux ssh k8s rabbitmq image should not depend on daocloud heidiks mirror', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  assert.equal(
    impl.includes('m.daocloud.io/docker.io/heidiks/rabbitmq-delayed-message-exchange:3.8.27-management'),
    false,
    'RabbitMQ delayed-exchange image from DaoCloud currently returns 403 and must not be used in generated SSH deploy packages'
  )

  for (const marker of [
    'rabbitmqImageForRuntimeComponent(resolvedRequest, runtimeImagesByRepository)',
    'rabbitmqImageForRuntimeComponent(request, serviceImages)',
    'rabbitmq-delayed-message-exchange'
  ]) {
    assert.equal(impl.includes(marker), true, `RabbitMQ deploy image resolution missing ${marker}`)
  }
})

test('linux ssh k8s rabbitmq startup should install delayed exchange plugin automatically', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const marker of [
    'RABBITMQ_DELAYED_PLUGIN_FILE',
    'RABBITMQ_DELAYED_PLUGIN_URL',
    'rabbitmq-delayed-plugin',
    'wget -T 60 -O \\"$plugin.tmp\\"',
    'cp /rk-rabbitmq-plugins/rabbitmq_delayed_message_exchange-*.ez /opt/rabbitmq/plugins/',
    'rabbitmq-plugins enable --offline rabbitmq_management rabbitmq_prometheus rabbitmq_delayed_message_exchange'
  ]) {
    assert.equal(impl.includes(marker), true, `RabbitMQ delayed exchange plugin install missing ${marker}`)
  }

  const rabbitmqInit = impl.slice(
    impl.indexOf('if ("rabbitmq".equals(name)) {'),
    impl.indexOf('if ("rk-gogs".equals(name) || "rk-jenkins".equals(name)) {')
  )
  const rabbitmqVolumes = impl.slice(
    impl.indexOf('private String buildK8sVolumeMounts(String name)'),
    impl.indexOf('private String buildK8sVolumeClaimTemplate(String name, AdminDeployPackageCreateDTO request)')
  )

  assert.match(rabbitmqInit, /rabbitmq-delayed-plugin/)
  assert.equal(rabbitmqInit.includes('wget -T 60 -O \\"$plugin.tmp\\"'), true)
  assert.match(
    rabbitmqInit,
    /image: "\s*\+ REMOTE_BUSYBOX_IMAGE/,
    'RabbitMQ delayed plugin init container must use the BusyBox helper image because the RabbitMQ runtime image does not include wget'
  )
  assert.doesNotMatch(
    rabbitmqInit,
    /name: rabbitmq-delayed-plugin[\s\S]*image: "\s*\+ image/,
    'RabbitMQ delayed plugin init container must not reuse the RabbitMQ runtime image for plugin download'
  )
  assert.match(rabbitmqVolumes, /rabbitmq-delayed-plugin/)
})

test('linux ssh k8s rabbitmq startup should preserve expected application credentials', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const rabbitmqArgs = impl.slice(
    impl.indexOf('if ("rabbitmq".equals(name)) {', impl.indexOf('private String buildK8sDeploymentArgs')),
    impl.indexOf('return "";', impl.indexOf('private String buildK8sDeploymentArgs'))
  )

  for (const marker of [
    'default_user = tjxt',
    'default_pass = 123321',
    'default_vhost = /tjxt'
  ]) {
    assert.equal(
      rabbitmqArgs.includes(marker),
      true,
      `RabbitMQ startup must write ${marker} because overriding the image command bypasses Docker entrypoint credential initialization`
    )
  }
})

test('linux ssh k8s nacos config check should use domestic reachable helper image', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const start = impl.indexOf('private String buildK8sInstallScript()')
  const end = impl.indexOf('private String buildOnlinePackageScript()', start)
  const script = impl.slice(start, end)

  assert.equal(script.includes('curlimages/curl'), false, 'Linux SSH Nacos config check must not depend on Docker Hub curlimages/curl')
  assert.equal(script.includes('image=\\"$RK_BUSYBOX_IMAGE\\"'), true, 'Linux SSH Nacos config check should use the configured BusyBox mirror')
  assert.equal(script.includes('wget -qO-'), true, 'Linux SSH Nacos config check should use BusyBox wget')
})

test('linux ssh application stack stage should allow slow serial package image pulls', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const deployStage = impl.slice(
    impl.indexOf('"linux ssh deploying application stack"'),
    impl.indexOf('"linux ssh checking deployed services"')
  )

  assert.match(
    deployStage,
    /Math\.max\(7200,\s*resolveRemoteDataPlaneTimeoutSeconds\(request\)\)/,
    'linux ssh application stack stage must allow at least 7200 seconds for slow image pulls plus data restore'
  )
})

test('linux ssh staged startup should scale local CI statefulsets back to one replica', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const script = impl.slice(
    impl.indexOf('private String buildK8sInstallScript()'),
    impl.indexOf('private String buildOnlinePackageScript()')
  )

  assert.match(script, /APPLICATION_STARTUP_STATEFULSETS=\\"rk-gogs rk-jenkins\\"/)
  assert.match(script, /for statefulset in \$APPLICATION_STARTUP_STATEFULSETS; do/)
  assert.match(script, /kubectl -n \\"\$NAMESPACE\\" scale \\"statefulset\/\$statefulset\\" --replicas=1/)
  assert.match(script, /wait_for_k8s_resource \\"statefulset\.apps\/\$statefulset\\" 15m/)
})

test('linux ssh migrated Jenkins runtime must not overrun its pod memory limit', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const envBlock = impl.slice(
    impl.indexOf('if ("rk-jenkins".equals(name)) {'),
    impl.indexOf('if ("rk-web-frontend".equals(name)) {')
  )
  const resources = impl.slice(
    impl.indexOf('private String buildK8sDeploymentResources(String name)'),
    impl.indexOf('private String buildK8sVolumeMounts(String name)')
  )

  assert.match(envBlock, /JAVA_OPTS", "-Djenkins\.install\.runSetupWizard=false -Xms128m -Xmx384m/, 'migrated Jenkins heap must stay below the pod memory limit')
  assert.doesNotMatch(envBlock, /-Xmx1024m/, 'migrated Jenkins must not keep a 1024Mi heap under a 512Mi pod limit')
  assert.match(resources, /"rk-jenkins"\.equals\(name\)[\s\S]*memory: 256Mi[\s\S]*memory: 1024Mi/, 'migrated Jenkins should have enough pod memory for restored job history and builds')
})

test('linux ssh deploy package should migrate local Gogs and Jenkins persistent data', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const archiveBuilder = impl.slice(
    impl.indexOf('private Path buildDeployPackageArchiveFile'),
    impl.indexOf('private void addZipEntry', impl.indexOf('private Path buildDeployPackageArchiveFile'))
  )
  const deployScript = impl.slice(
    impl.indexOf('private String buildK8sInstallScript()'),
    impl.indexOf('private String buildOnlinePackageScript()')
  )

  for (const marker of [
    'exportPlatformStatefulData',
    'platform/rk-gogs/data.tar.gz',
    'platform/rk-jenkins/jenkins-home.tar.gz',
    'rk-server-rk-gogs-0',
    'rk-server-rk-jenkins-0'
  ]) {
    assert.equal(archiveBuilder.includes(marker) || impl.includes(marker), true, `deploy package must export platform data marker ${marker}`)
  }

  for (const marker of [
    'restore_platform_stateful_data',
    'platform/rk-gogs/data.tar.gz',
    'platform/rk-jenkins/jenkins-home.tar.gz',
    'kubectl -n \\"$NAMESPACE\\" exec \\"rk-gogs-0\\"',
    'kubectl -n \\"$NAMESPACE\\" exec \\"rk-jenkins-0\\"'
  ]) {
    assert.equal(deployScript.includes(marker), true, `linux ssh install script must restore platform data marker ${marker}`)
  }
})
