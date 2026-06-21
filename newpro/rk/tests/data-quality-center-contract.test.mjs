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

test('data quality center has independent menu route and frontend API', async () => {
  const router = await readSource('newpro/rk/src/router/index.js')
  const dataApi = await readSource('newpro/rk/src/api/data.js')
  const menuSeed = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/config/AuthMenuCatalogUpdater.java')

  assert.match(router, /path:\s*'statistics\/data-quality'/)
  assert.match(router, /name:\s*'AdminDataQualityCenter'/)
  assert.match(router, /DataQualityCenter\.vue/)
  assert.match(dataApi, /function\s+getDataQualityCenter/)
  assert.match(dataApi, /function\s+repairDataQualityIssue/)
  assert.match(dataApi, /url:\s*['"`]\/api\/data\/data-quality-center['"`]/)
  assert.match(dataApi, /url:\s*['"`]\/api\/data\/data-quality-center\/repair['"`]/)

  assert.match(menuSeed, /admin_statistics_data_quality/)
  assert.match(menuSeed, /\/admin\/statistics\/data-quality/)
  assert.match(menuSeed, /"\/admin\/statistics\/data-quality"[\s\S]*backend\(\)/)
})

test('data quality center page renders diagnostics and auditable repair actions', async () => {
  const page = await readSource('newpro/rk/src/views/admin/statistics/DataQualityCenter.vue')

  for (const marker of [
    'getDataQualityCenter',
    'repairDataQualityIssue',
    'dataQualityCenter',
    'issueRows',
    'repairLogs',
    'repairable',
    'riskLevel',
    'issueType',
    'refreshDataQuality',
    'handleRepair',
    '数据质量与对账中心',
    '账号漂移',
    '角色缺失',
    '媒体缺失',
    'ES 滞后',
    'Redis 风险',
    '修复记录'
  ]) {
    assert.match(page, new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }
})

test('rk-data exposes data quality center API with audit repair contract', async () => {
  const controller = await readSource('rk-data/src/main/java/com/tianji/data/controller/DataQualityCenterController.java')
  const service = await readSource('rk-data/src/main/java/com/tianji/data/service/DataQualityCenterService.java')
  const impl = await readSource('rk-data/src/main/java/com/tianji/data/service/impl/DataQualityCenterServiceImpl.java')
  const vo = await readSource('rk-data/src/main/java/com/tianji/data/model/vo/DataQualityCenterVO.java')
  const dto = await readSource('rk-data/src/main/java/com/tianji/data/model/dto/DataQualityRepairRequestDTO.java')
  const redisConstants = await readSource('rk-data/src/main/java/com/tianji/data/constants/RedisConstants.java')

  assert.match(controller, /@RequestMapping\("\/data\/data-quality-center"\)/)
  assert.match(controller, /getDataQualityCenter\(tenantId,\s*issueType,\s*riskLevel\)/)
  assert.match(controller, /repair\(@RequestBody DataQualityRepairRequestDTO request\)/)
  assert.match(service, /DataQualityCenterVO\s+getDataQualityCenter\(Long tenantId,\s*String issueType,\s*String riskLevel\)/)
  assert.match(service, /RepairLog\s+repair\(DataQualityRepairRequestDTO request\)/)
  assert.match(redisConstants, /KEY_DATA_QUALITY_CENTER/)

  for (const marker of [
    'TenantContext',
    'StringRedisTemplate',
    'FrontendCacheWarmupService',
    'KEY_DATA_QUALITY_CENTER',
    'data_quality_repair_log',
    'orphanUserCount',
    'missingRoleCount',
    'missingMediaCount',
    'searchStaleCount',
    'redisRiskCount',
    'repairable',
    'audit'
  ]) {
    assert.match(impl + vo + dto, new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }
})
