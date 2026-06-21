import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

async function readSource(relativePath) {
  return readFile(new URL(relativePath, import.meta.url), 'utf8')
}

test('tenant management should expose multi-select batch delete', async () => {
  const source = await readSource('../src/views/admin/system/Tenants.vue')

  assert.equal(source.includes('type="selection"'), true)
  assert.equal(source.includes('handleBatchDelete'), true)
  assert.equal(source.includes('selectedRows'), true)
  assert.equal(source.includes(':disabled="!selectedRows.length"'), true)
})

test('competition front page should only render published competitions and provide keyword search', async () => {
  const source = await readSource('../src/views/CompetitionFront.vue')

  assert.equal(source.includes('getPublishedCompetitions'), true)
  assert.equal(source.includes('getCompetitionList'), false)
  assert.equal(source.includes('searchKeyword'), true)
  assert.equal(source.includes('赛事关键词'), true)
  assert.equal(source.includes('matchesKeyword'), true)
})

test('activity calendar should merge approved activities and published competitions', async () => {
  const source = await readSource('../src/views/ActivityCalendar.vue')
  const zh = await readSource('../src/i18n/zh.js')

  assert.equal(source.includes('getActivityList'), true)
  assert.equal(source.includes('getPublishedCompetitions'), true)
  assert.equal(source.includes("type: 'activity'"), true)
  assert.equal(source.includes("type: 'competition'"), true)
  assert.equal(source.includes('cell-event--competition'), true)
  assert.equal(source.includes('/competition/${evt.id}'), true)
  assert.equal(zh.includes("calendar: '活动比赛日历'"), true)
  assert.equal(zh.includes("title: '活动比赛日历'"), true)
})
