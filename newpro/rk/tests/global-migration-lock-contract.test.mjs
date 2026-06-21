import { readFile } from 'node:fs/promises'
import { test } from 'node:test'
import assert from 'node:assert/strict'

async function readSource(path) {
  return readFile(new URL(path, import.meta.url), 'utf8')
}

test('global migration lock should expose public status and block non-allowlisted gateway requests', async () => {
  const controller = await readSource('../../../rk-user/src/main/java/com/tianji/user/controller/AdminOpsController.java')
  const service = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const gateway = await readSource('../../../rk-gateway/src/main/java/com/tianji/gateway/filter/GlobalMigrationLockFilter.java')
  const gatewayConfig = await readSource('../../../rk-gateway/src/main/resources/bootstrap.yml')
  const gatewayAuthProperties = await readSource('../../../rk-gateway/src/main/java/com/tianji/gateway/config/AuthProperties.java')

  for (const marker of [
    '@GetMapping("/public/deploy-package/active-migration")',
    'getPublicActiveGlobalMigration',
    'GLOBAL_MIGRATION_LOCK_KEY',
    'GLOBAL_MIGRATION_LOCK_TTL = Duration.ofMinutes(30)',
    'publishGlobalMigrationLock',
    'clearGlobalMigrationLock',
    'refreshGlobalMigrationLock',
    'rk:ops:global-migration:active'
  ]) {
    assert.equal(
      controller.includes(marker) || service.includes(marker),
      true,
      `backend public migration lock contract missing ${marker}`
    )
  }

  for (const marker of [
    'class GlobalMigrationLockFilter',
    'StringRedisTemplate',
    'GLOBAL_MIGRATION_LOCK_KEY',
    'isMigrationStatusPath',
    'isAuthPath',
    'isReadinessPath',
    'SERVICE_MIGRATION_IN_PROGRESS',
    'setRawStatusCode(423)'
  ]) {
    assert.equal(gateway.includes(marker), true, `gateway migration lock contract missing ${marker}`)
  }

  for (const marker of [
    '/api/ops/migration/active',
    'RewritePath=/api/ops/migration/active, /admin/ops/public/deploy-package/active-migration',
    'id: rk-minio-files',
    'Path=/minio-files,/minio-files/**',
    'RewritePath=/minio-files/?(?<segment>.*), /${segment}',
    'GET:/api/ops/migration/active'
  ]) {
    assert.equal(gatewayConfig.includes(marker), true, `gateway config missing ${marker}`)
  }

  assert.equal(
    gatewayAuthProperties.includes('GET:/api/ops/migration/active'),
    true,
    'gateway auth default whitelist must include public migration status path'
  )
  assert.equal(
    gatewayAuthProperties.includes('GET:/minio-files/**'),
    true,
    'gateway auth default whitelist must include public MinIO media path'
  )
  assert.equal(
    gateway.includes('isPublicMediaPath'),
    true,
    'migration lock should allow public MinIO media GET requests through'
  )
})

test('web and android clients should poll public migration status and block interaction', async () => {
  const frontendApi = await readSource('../src/api/admin-ops.js')
  const overlay = await readSource('../src/components/GlobalMigrationOverlay.vue')
  const android = await readSource('../../../android-client/rk-club-android/app/src/main/java/com/rkclub/app/MainActivity.java')

  for (const marker of [
    '/api/ops/migration/active',
    'silentError: true',
    'showLoginOn401: false'
  ]) {
    assert.equal(frontendApi.includes(marker) || overlay.includes(marker), true, `web migration polling missing ${marker}`)
  }

  for (const marker of [
    'MIGRATION_STATUS_PATH',
    '/api/ops/migration/active',
    'startMigrationStatusPolling',
    'fetchActiveMigration',
    'showMigrationBlockingDialog',
    'serviceMigrationDialog',
    '服务迁移正在进行',
    '迁移完成后会自动关闭',
    'migrationStartedAtMillis',
    'migrationElapsedText',
    'migrationLogText',
    'formatMigrationElapsed',
    'formatMigrationStep'
  ]) {
    assert.equal(android.includes(marker), true, `android migration overlay missing ${marker}`)
  }
})

test('migration overlays should keep local elapsed time moving and show Chinese step logs', async () => {
  const overlay = await readSource('../src/components/GlobalMigrationOverlay.vue')
  const android = await readSource('../../../android-client/rk-club-android/app/src/main/java/com/rkclub/app/MainActivity.java')

  for (const marker of [
    'elapsedSeconds',
    'elapsedTimer',
    'remainingSourceSeconds',
    'remainingSourceReceivedAt',
    'remainingCountdownSeconds',
    'record?.migrationStartedAtMillis',
    'record?.deployStartedAtMillis',
    'syncRemainingEstimate',
    'formatElapsed',
    'resolveStepText',
    'terminalLines',
    'migrationPhases',
    'phaseClass',
    'migration-progress-strip',
    'migration-phase-track',
    '当前步骤',
    '已用时间',
    '预计剩余',
    '终端日志',
    '实时进度',
    '最近日志：',
    '正在迁移镜像',
    '正在导入 MySQL 数据',
    '正在导入 MinIO 文件',
    '迁移期间所有用户暂时不能操作'
  ]) {
    assert.equal(overlay.includes(marker), true, `web migration overlay missing ${marker}`)
  }

  assert.equal(overlay.includes('鍏ㄥ眬杩佺Щ'), false, 'web migration overlay must not keep mojibake copy')
  assert.equal(overlay.includes('杩佺Щ杩涘害'), false, 'web migration overlay must not keep mojibake progress copy')

  for (const marker of [
    'migrationStartedAtMillis',
    'migrationStartedAtMillis", "deployStartedAtMillis',
    'migrationRemainingBaseSeconds',
    'migrationRemainingBaseAtMillis',
    'syncMigrationRemainingEstimate',
    'updateMigrationRemainingText',
    'migrationElapsedText',
    'migrationLogText',
    'migrationTickTimer',
    'startMigrationElapsedTicker',
    'formatMigrationElapsed',
    'formatMigrationStep',
    'formatMigrationLogSnippet',
    'latestMigrationLogLine',
    '当前集群 →',
    '最近日志：\\n> 等待迁移日志',
    'setMaxLines(5)',
    '已用时间',
    '当前步骤',
    '最近日志',
    '正在迁移镜像',
    '正在导入 MySQL 数据',
    '正在导入 MinIO 文件'
  ]) {
    assert.equal(android.includes(marker), true, `android migration overlay missing ${marker}`)
  }

  assert.equal(android.includes('鏈嶅姟杩佺Щ'), false, 'android migration overlay must not keep mojibake migration copy')
})
