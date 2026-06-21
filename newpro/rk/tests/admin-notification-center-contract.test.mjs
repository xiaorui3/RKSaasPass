import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

async function readSource(relativePath) {
  return readFile(new URL(relativePath, import.meta.url), 'utf8')
}

test('admin notification center should expose API wrappers', async () => {
  const source = await readSource('../src/api/admin-notifications.js')

  assert.equal(source.includes('getAdminNotificationOverview'), true)
  assert.equal(source.includes('listAdminNotifications'), true)
  assert.equal(source.includes('saveAdminNotification'), true)
  assert.equal(source.includes('deleteAdminNotification'), true)
  assert.equal(source.includes('/api/admin/notifications/overview'), true)
  assert.equal(source.includes('/api/admin/notifications'), true)
  assert.equal(source.includes("url: '/admin/notifications/overview'"), false)
  assert.equal(source.includes("url: '/admin/notifications'"), false)
})

test('admin notification center should be reachable from operations route and menu', async () => {
  const routerSource = await readSource('../src/router/index.js')
  const layoutSource = await readSource('../src/layouts/AdminLayout.vue')

  assert.equal(routerSource.includes("path: 'operation/notification-center'"), true)
  assert.equal(routerSource.includes('NotificationCenter.vue'), true)
  assert.equal(layoutSource.includes('/admin/operation/notification-center'), true)
  assert.equal(layoutSource.includes("t('admin.notificationCenter')"), true)
})

test('admin notification center page should manage normal and upgrade notices', async () => {
  const source = await readSource('../src/views/admin/operation/NotificationCenter.vue')

  assert.equal(source.includes('notificationForm'), true)
  assert.equal(source.includes('upgradeVersion'), true)
  assert.equal(source.includes('forceUpgrade'), true)
  assert.equal(source.includes('downloadUrl'), true)
  assert.equal(source.includes('releaseNotes'), true)
  assert.equal(source.includes('targetType'), true)
  assert.equal(source.includes('typeOptions'), true)
  assert.equal(source.includes('saveAdminNotification'), true)
  assert.equal(source.includes('deleteAdminNotification'), true)
  assert.equal(source.includes('notifications.value = notifications.value.filter'), true)
  assert.equal(source.includes('el-collapse'), true)
  assert.equal(source.includes('mobileReleaseHistoryCollapse'), true)
  assert.equal(source.includes('APK 发布记录'), true)
})

test('admin notification center should page notifications and history records', async () => {
  const source = await readSource('../src/views/admin/operation/NotificationCenter.vue')

  assert.equal(source.includes('const notificationPageSize = 5'), true)
  assert.equal(source.includes('const historyPageSize = 5'), true)
  assert.equal(source.includes(':data="pagedNotifications"'), true)
  assert.equal(source.includes(':data="pagedMobileReleaseBuildHistory"'), true)
  assert.equal(source.includes(':data="pagedMobileReleaseHistory"'), true)
  assert.equal(source.includes('mobileBuildHistoryCollapse'), true)
  assert.equal(source.includes('layout="prev, pager, next"'), true)
  assert.equal(source.includes('res.code === 200 && res.data === true'), true)
  assert.equal(source.includes('limit: 200'), true)
})

test('admin notification center should support multi-tenant selection for messages and apk releases', async () => {
  const source = await readSource('../src/views/admin/operation/NotificationCenter.vue')

  assert.equal(source.includes("from '@/api/tenant'"), true)
  assert.equal(source.includes('normalizeTenantDirectory'), true)
  assert.equal(source.includes('tenantOptions'), true)
  assert.equal(source.includes('notificationTenantIds'), true)
  assert.match(source, /<el-select[^>]*v-model="notificationTenantIds"[\s\S]*multiple/)
  assert.match(source, /<el-select[^>]*v-model="mobileReleaseForm\.tenantIds"[\s\S]*multiple/)
  assert.equal(source.includes('buildNotificationPayloads'), true)
  assert.equal(source.includes('Promise.all(payloads.map'), true)
  assert.equal(source.includes('parseIdList(mobileTenantIdsText.value)'), false)
})
