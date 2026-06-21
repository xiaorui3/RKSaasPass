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

test('growth retention center has independent menu route and frontend API', async () => {
  const router = await readSource('newpro/rk/src/router/index.js')
  const dataApi = await readSource('newpro/rk/src/api/data.js')
  const menuSeed = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/config/AuthMenuCatalogUpdater.java')

  assert.match(router, /path:\s*'statistics\/growth-retention'/)
  assert.match(router, /name:\s*'AdminGrowthRetentionCenter'/)
  assert.match(router, /GrowthRetentionCenter\.vue/)
  assert.match(dataApi, /function\s+getGrowthRetentionCenter/)
  assert.match(dataApi, /url:\s*['"`]\/api\/data\/growth-retention-center['"`]/)

  assert.match(menuSeed, /admin_statistics_growth_retention/)
  assert.match(menuSeed, /\/admin\/statistics\/growth-retention/)
  assert.match(menuSeed, /"\/admin\/statistics\/growth-retention"[\s\S]*backend\(\)/)
})

test('growth retention center page renders member growth retention and credit metrics', async () => {
  const page = await readSource('newpro/rk/src/views/admin/statistics/GrowthRetentionCenter.vue')

  for (const marker of [
    'getGrowthRetentionCenter',
    'growthRetentionCenter',
    'summaryCards',
    'trendRows',
    'riskRows',
    'sourceRows',
    'newMemberCount',
    'activeMemberCount',
    'dormantMemberCount',
    'retentionRate',
    'volunteerHours',
    'creditTotal',
    'monthlyTrend',
    'refreshGrowthRetention',
    '租户增长与留存看板',
    '新增成员',
    '活跃社团',
    '沉默成员',
    '志愿时长',
    '积分总额',
    '月度趋势'
  ]) {
    assert.match(page, new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }
})

test('rk-data exposes growth retention API with redis cache tenant scope and monthly trend', async () => {
  const controller = await readSource('rk-data/src/main/java/com/tianji/data/controller/GrowthRetentionCenterController.java')
  const service = await readSource('rk-data/src/main/java/com/tianji/data/service/GrowthRetentionCenterService.java')
  const impl = await readSource('rk-data/src/main/java/com/tianji/data/service/impl/GrowthRetentionCenterServiceImpl.java')
  const vo = await readSource('rk-data/src/main/java/com/tianji/data/model/vo/GrowthRetentionCenterVO.java')
  const redisConstants = await readSource('rk-data/src/main/java/com/tianji/data/constants/RedisConstants.java')

  assert.match(controller, /@RequestMapping\("\/data\/growth-retention-center"\)/)
  assert.match(controller, /getGrowthRetentionCenter\(tenantId,\s*timeRange,\s*riskLevel\)/)
  assert.match(service, /GrowthRetentionCenterVO\s+getGrowthRetentionCenter\(Long tenantId,\s*String timeRange,\s*String riskLevel\)/)
  assert.match(redisConstants, /KEY_GROWTH_RETENTION_CENTER/)

  for (const marker of [
    'TenantContext',
    'StringRedisTemplate',
    'KEY_GROWTH_RETENTION_CENTER',
    'club_members',
    'sys_logininfor',
    'volunteer_record',
    'user_credit_record',
    'user_credit_summary',
    'newMemberCount',
    'activeClubCount',
    'dormantMemberCount',
    'retentionRate',
    'monthlyTrend',
    'warmupAllTenants'
  ]) {
    assert.match(impl + vo, new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }
})
