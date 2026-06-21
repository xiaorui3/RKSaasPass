import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

async function readSource(relativePath) {
  return readFile(new URL(relativePath, import.meta.url), 'utf8')
}

test('high-traffic portal detail pages should route visible copy through vue-i18n', async () => {
  const [activityDetail, competitionDetail, activityCalendar, achievements, reviewAction] = await Promise.all([
    readSource('../src/views/ActivityDetail.vue'),
    readSource('../src/views/CompetitionDetail.vue'),
    readSource('../src/views/ActivityCalendar.vue'),
    readSource('../src/views/Achievements.vue'),
    readSource('../src/views/AdmissionReviewAction.vue')
  ])

  assert.equal(activityDetail.includes('useI18n'), true)
  assert.equal(activityDetail.includes("t('pages.activityDetail.loading')"), true)
  assert.equal(activityDetail.includes("t('pages.activityDetail.backToList')"), true)

  assert.equal(competitionDetail.includes('useI18n'), true)
  assert.equal(competitionDetail.includes("t('pages.competitionDetail.loading')"), true)
  assert.equal(competitionDetail.includes("t('pages.competitionDetail.backToList')"), true)

  assert.equal(activityCalendar.includes('useI18n'), true)
  assert.equal(activityCalendar.includes("t('pages.activityCalendar.title')"), true)

  assert.equal(achievements.includes('useI18n'), true)
  assert.equal(achievements.includes("t('pages.achievements.title')"), true)
  assert.equal(achievements.includes("t('pages.achievements.searchPlaceholder')"), true)

  assert.equal(reviewAction.includes('useI18n'), true)
  assert.equal(reviewAction.includes("t('pages.reviewAction.processingTitle')"), true)
  assert.equal(reviewAction.includes("t('pages.reviewAction.reload')"), true)
})
