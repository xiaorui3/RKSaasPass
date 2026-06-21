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

test('participation analysis center has independent menu route and frontend API', async () => {
  const router = await readSource('newpro/rk/src/router/index.js')
  const dataApi = await readSource('newpro/rk/src/api/data.js')
  const menuSeed = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/config/AuthMenuCatalogUpdater.java')

  assert.match(router, /path:\s*'statistics\/participation'/)
  assert.match(router, /name:\s*'AdminParticipationAnalysisCenter'/)
  assert.match(router, /ParticipationAnalysisCenter\.vue/)
  assert.match(dataApi, /function\s+getParticipationAnalysisCenter/)
  assert.match(dataApi, /url:\s*['"`]\/api\/data\/participation-analysis-center['"`]/)

  assert.match(menuSeed, /admin_statistics_participation/)
  assert.match(menuSeed, /\/admin\/statistics\/participation/)
  assert.match(menuSeed, /"\/admin\/statistics\/participation"[\s\S]*backend\(\)/)
})

test('participation analysis center page renders engagement metrics and drilldowns', async () => {
  const page = await readSource('newpro/rk/src/views/admin/statistics/ParticipationAnalysisCenter.vue')

  for (const marker of [
    'getParticipationAnalysisCenter',
    'participationAnalysisCenter',
    'metricCards',
    'trendRows',
    'sourceRows',
    'commentCount',
    'viewCount',
    'registrationConversionRate',
    'activeMemberCount',
    'followCount',
    'revisitCount',
    'refreshParticipationAnalysis',
    '参与度分析中心',
    '浏览转化',
    '报名转化',
    '评论互动',
    '关注与复访',
    '活跃趋势'
  ]) {
    assert.match(page, new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }
})

test('rk-data exposes participation analysis API with redis cache and tenant scope', async () => {
  const controller = await readSource('rk-data/src/main/java/com/tianji/data/controller/ParticipationAnalysisCenterController.java')
  const service = await readSource('rk-data/src/main/java/com/tianji/data/service/ParticipationAnalysisCenterService.java')
  const impl = await readSource('rk-data/src/main/java/com/tianji/data/service/impl/ParticipationAnalysisCenterServiceImpl.java')
  const vo = await readSource('rk-data/src/main/java/com/tianji/data/model/vo/ParticipationAnalysisCenterVO.java')
  const redisConstants = await readSource('rk-data/src/main/java/com/tianji/data/constants/RedisConstants.java')

  assert.match(controller, /@RequestMapping\("\/data\/participation-analysis-center"\)/)
  assert.match(controller, /getParticipationAnalysisCenter\(tenantId,\s*dimension,\s*timeRange\)/)
  assert.match(service, /ParticipationAnalysisCenterVO\s+getParticipationAnalysisCenter\(Long tenantId,\s*String dimension,\s*String timeRange\)/)
  assert.match(redisConstants, /KEY_PARTICIPATION_ANALYSIS_CENTER/)

  for (const marker of [
    'TenantContext',
    'StringRedisTemplate',
    'KEY_PARTICIPATION_ANALYSIS_CENTER',
    'rk_activity_registration',
    'competition_participants',
    'news',
    'works',
    'comments',
    'rk_notification',
    'rk_user_inbox',
    'followCount',
    'revisitCount',
    'engagementTrend',
    'warmupAllTenants'
  ]) {
    assert.match(impl + vo, new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }
})
