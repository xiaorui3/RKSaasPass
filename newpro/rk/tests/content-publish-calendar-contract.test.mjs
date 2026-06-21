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

test('content publish calendar has independent menu route and frontend API', async () => {
  const router = await readSource('newpro/rk/src/router/index.js')
  const dataApi = await readSource('newpro/rk/src/api/data.js')
  const menuSeed = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/config/AuthMenuCatalogUpdater.java')

  assert.match(router, /path:\s*'statistics\/content-calendar'/)
  assert.match(router, /name:\s*'AdminContentPublishCalendar'/)
  assert.match(router, /ContentPublishCalendar\.vue/)
  assert.match(dataApi, /function\s+getContentPublishCalendar/)
  assert.match(dataApi, /url:\s*['"`]\/api\/data\/content-publish-calendar['"`]/)

  assert.match(menuSeed, /admin_statistics_content_calendar/)
  assert.match(menuSeed, /\/admin\/statistics\/content-calendar/)
  assert.match(menuSeed, /"\/admin\/statistics\/content-calendar"[\s\S]*backend\(\)/)
})

test('content publish calendar page renders calendar timeline and diagnostics', async () => {
  const page = await readSource('newpro/rk/src/views/admin/statistics/ContentPublishCalendar.vue')

  for (const marker of [
    'getContentPublishCalendar',
    'contentPublishCalendar',
    'summaryCards',
    'calendarRows',
    'timelineRows',
    'riskRows',
    'sourceRows',
    'publishedCount',
    'pendingCount',
    'scheduledCount',
    'overdueCount',
    'refreshContentCalendar',
    '内容发布日历',
    '发布排期',
    '待审核',
    '延期风险',
    '来源诊断'
  ]) {
    assert.match(page, new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }
})

test('rk-data exposes content publish calendar API with redis cache tenant scope and warmup', async () => {
  const controller = await readSource('rk-data/src/main/java/com/tianji/data/controller/ContentPublishCalendarController.java')
  const service = await readSource('rk-data/src/main/java/com/tianji/data/service/ContentPublishCalendarService.java')
  const impl = await readSource('rk-data/src/main/java/com/tianji/data/service/impl/ContentPublishCalendarServiceImpl.java')
  const vo = await readSource('rk-data/src/main/java/com/tianji/data/model/vo/ContentPublishCalendarVO.java')
  const job = await readSource('rk-data/src/main/java/com/tianji/data/handler/ContentPublishCalendarJobHandler.java')
  const redisConstants = await readSource('rk-data/src/main/java/com/tianji/data/constants/RedisConstants.java')

  assert.match(controller, /@RequestMapping\("\/data\/content-publish-calendar"\)/)
  assert.match(controller, /getContentPublishCalendar\(tenantId,\s*timeRange,\s*contentType,\s*status\)/)
  assert.match(service, /ContentPublishCalendarVO\s+getContentPublishCalendar\(Long tenantId,\s*String timeRange,\s*String contentType,\s*String status\)/)
  assert.match(redisConstants, /KEY_CONTENT_PUBLISH_CALENDAR/)
  assert.match(job, /contentPublishCalendarWarmupJobHandler/)

  for (const marker of [
    'TenantContext',
    'StringRedisTemplate',
    'KEY_CONTENT_PUBLISH_CALENDAR',
    'news',
    'notice',
    'works',
    'activity',
    'competition',
    'publishedCount',
    'pendingCount',
    'scheduledCount',
    'overdueCount',
    'calendarRows',
    'timelineRows',
    'warmupAllTenants'
  ]) {
    assert.match(impl + vo, new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }
})
