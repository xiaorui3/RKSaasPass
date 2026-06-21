import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(__dirname, '../../..')

async function readSource(relativePath) {
  return readFile(path.resolve(repoRoot, relativePath), 'utf8')
}

test('tenant operations center has a persisted menu route and frontend API', async () => {
  const router = await readSource('newpro/rk/src/router/index.js')
  const dataApi = await readSource('newpro/rk/src/api/data.js')
  const menuSeed = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/config/AuthMenuCatalogUpdater.java')

  assert.match(router, /path:\s*'statistics\/tenant-operations'/)
  assert.match(router, /name:\s*'AdminTenantOperationsCenter'/)
  assert.match(router, /TenantOperationsCenter\.vue/)
  assert.match(dataApi, /function\s+getTenantOperationsCenter/)
  assert.match(dataApi, /url:\s*['"`]\/api\/data\/tenant-operations-center['"`]/)
  assert.doesNotMatch(dataApi, /url:\s*['"`]\/data\/tenant-operations-center['"`]/)

  assert.match(menuSeed, /admin_statistics_tenant_operations/)
  assert.match(menuSeed, /\/admin\/statistics\/tenant-operations/)
  assert.match(menuSeed, /"\/admin\/statistics\/tenant-operations"[\s\S]*backend\(\)/)
})

test('tenant operations center page renders health, approval, quality, growth and security sections', async () => {
  const page = await readSource('newpro/rk/src/views/admin/statistics/TenantOperationsCenter.vue')

  for (const marker of [
    'getTenantOperationsCenter',
    'healthScore',
    'approvalCenter',
    'participation',
    'dataQuality',
    'growthRetention',
    'securityAudit',
    'approvalDetails',
    'qualityDetails',
    'securityDetails',
    'detailRows',
    'activeDetailTab',
    'cache',
    'sources',
    'refreshOperationsCenter',
    'el-tabs',
    'el-table',
    '租户运营中心',
    '健康评分',
    '统一审批',
    '参与度分析',
    '数据质量',
    '增长留存',
    '安全审计'
  ]) {
    assert.match(page, new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }
})

test('tenant operations center has xxl-job warmup handler and detail contracts', async () => {
  const handler = await readSource('rk-data/src/main/java/com/tianji/data/handler/TenantOperationsCenterJobHandler.java')
  const service = await readSource('rk-data/src/main/java/com/tianji/data/service/TenantOperationsCenterService.java')
  const impl = await readSource('rk-data/src/main/java/com/tianji/data/service/impl/TenantOperationsCenterServiceImpl.java')
  const vo = await readSource('rk-data/src/main/java/com/tianji/data/model/vo/TenantOperationsCenterVO.java')

  assert.match(handler, /tenantOperationsCenterWarmupJobHandler/)
  assert.match(handler, /@XxlJob\(HANDLER_NAME\)/)
  assert.match(handler, /warmupAllTenants\(\)/)
  assert.match(service, /warmupAllTenants\(\)/)
  assert.match(impl, /listTenantIds\(\)/)
  assert.match(impl, /buildDetailItems\(/)

  for (const marker of [
    'approvalDetails',
    'qualityDetails',
    'securityDetails',
    'DetailItem',
    'type',
    'title',
    'risk',
    'status',
    'source',
    'sourceTime'
  ]) {
    assert.match(vo, new RegExp(marker))
  }
})

test('rk-data exposes tenant operations center aggregation API and Redis cache key', async () => {
  const controller = await readSource('rk-data/src/main/java/com/tianji/data/controller/TenantOperationsCenterController.java')
  const service = await readSource('rk-data/src/main/java/com/tianji/data/service/TenantOperationsCenterService.java')
  const impl = await readSource('rk-data/src/main/java/com/tianji/data/service/impl/TenantOperationsCenterServiceImpl.java')
  const vo = await readSource('rk-data/src/main/java/com/tianji/data/model/vo/TenantOperationsCenterVO.java')
  const redisConstants = await readSource('rk-data/src/main/java/com/tianji/data/constants/RedisConstants.java')

  assert.match(controller, /@RequestMapping\("\/data\/tenant-operations-center"\)/)
  assert.match(controller, /getOverview\(tenantId\)/)
  assert.match(service, /TenantOperationsCenterVO\s+getOverview\(Long tenantId\)/)
  assert.match(redisConstants, /KEY_TENANT_OPERATIONS_CENTER/)

  for (const marker of [
    'TenantContext',
    'StringRedisTemplate',
    'KEY_TENANT_OPERATIONS_CENTER',
    'healthScore',
    'approvalCenter',
    'participation',
    'dataQuality',
    'growthRetention',
    'securityAudit',
    'rk_auth',
    'rk_user',
    'rk_activity',
    'rk_content',
    'rk_search'
  ]) {
    assert.match(impl + vo, new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }
})
