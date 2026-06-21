import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

function source(relativePath) {
  return readFileSync(new URL(relativePath, import.meta.url), 'utf8')
}

test('activity album page must persist photos through rk-activity APIs', () => {
  const api = source('../src/api/activity.js')
  const page = source('../src/views/admin/activity/PhotoGallery.vue')

  assert.match(api, /export function getActivityAlbums\(/)
  assert.match(api, /export function createActivityAlbum\(/)
  assert.match(api, /export function deleteActivityAlbum\(/)
  assert.match(page, /getActivityAlbums/)
  assert.match(page, /createActivityAlbum/)
  assert.match(page, /deleteActivityAlbum/)
  assert.match(page, /selectedActivityId/)
  assert.doesNotMatch(page, /photos\.value\.push\(/, 'album page must not keep uploaded photos only in local memory')
})

test('activity participants dialog must show user identity fields with fallbacks', () => {
  const page = source('../src/views/admin/activity/ActivityList.vue')

  assert.match(page, /formatParticipantName/)
  assert.match(page, /studentId/)
  assert.match(page, /email/)
  assert.match(page, /cellPhone/)
})

test('credit manual record dialog must use a scoped user picker instead of raw user id entry', () => {
  const page = source('../src/views/admin/activity/CreditConfig.vue')

  assert.match(page, /getUserPage/)
  assert.match(page, /userPickerVisible/)
  assert.match(page, /selectCreditUser/)
  assert.doesNotMatch(page, /placeholder="[^"]*用户\s*ID[^"]*"/, 'manual raw userId input should be replaced by a picker')
})

test('backend menu catalog must include every active admin route that appears in the runtime sidebar', () => {
  const catalog = source('../../../rk-auth/rk-auth-service/src/main/java/com/tianji/auth/config/AuthMenuCatalogUpdater.java')
  const layout = source('../src/layouts/AdminLayout.vue')

  const expectedPaths = [
    '/admin/club/contact-messages',
    '/admin/operation/service-monitor',
    '/admin/operation/data-export',
    '/admin/system/theme',
    '/admin/activity/photo-gallery',
    '/admin/activity/credit',
    '/admin/club/finance',
    '/admin/content/data-diff'
  ]

  for (const path of expectedPaths) {
    assert.match(catalog, new RegExp(path.replace(/\//g, '\\/')), `catalog missing ${path}`)
    assert.match(layout, new RegExp(path.replace(/\//g, '\\/')), `layout fallback missing ${path}`)
  }
})
