import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

async function readSource(relativePath) {
  return readFile(new URL(relativePath, import.meta.url), 'utf8')
}

function styleOf(source) {
  const start = source.indexOf('<style')
  const end = source.indexOf('</style>', start)
  assert.notEqual(start, -1)
  assert.notEqual(end, -1)
  return source.slice(start, end)
}

test('home recommended clubs are built from tenant directory instead of fake hard-coded clubs', async () => {
  const source = await readSource('../src/views/Home.vue')

  assert.equal(source.includes("name: '吉他社'"), false)
  assert.equal(source.includes("大学生摄影协会"), false)
  assert.equal(source.includes("青年志愿者协会"), false)
  assert.match(source, /const\s+tenantDirectoryClubs\s*=\s*computed/)
  assert.match(source, /userStore\.fetchTenantDirectory\(\)/)
  assert.match(source, /recommendedClubs\s*=\s*computed\(\(\)\s*=>\s*tenantDirectoryClubs\.value/)
})

test('home recommended clubs should page real tenant cards with clickable dots', async () => {
  const source = await readSource('../src/views/Home.vue')

  assert.equal(
    /\.filter\(Boolean\)\s*\.slice\(0,\s*4\)/.test(source),
    false,
    'tenant directory should not be truncated before pagination',
  )
  assert.match(source, /v-for="club in visibleRecommendedClubs"/)
  assert.match(source, /const\s+recommendedClubPages\s*=\s*computed/)
  assert.match(source, /const\s+activeRecommendedClubPage\s*=\s*ref\(0\)/)
  assert.match(source, /@click="setRecommendedClubPage\(index\)"/)
  assert.match(source, /recommendedClubPages\.length > 1/)
})

test('home profile volunteer hours should come from credit summary instead of a hard-coded value', async () => {
  const source = await readSource('../src/views/Home.vue')

  assert.equal(source.includes("value: '32h'"), false)
  assert.match(source, /getMyCreditSummary/)
  assert.match(source, /volunteerHours/)
  assert.match(source, /formatVolunteerHours/)
})

test('member notifications should mark an unread message as read when opened', async () => {
  const source = await readSource('../src/views/Notifications.vue')

  assert.match(source, /async function openDetail\(item\)/)
  assert.match(source, /await markOneRead\(item\.id,\s*\{[\s\S]*silent:\s*true[\s\S]*\}\)/)
  assert.match(source, /item\.read\s*=\s*true/)
  assert.match(source, /currentNotice\.value\.read\s*=\s*true/)
})

test('portal notification badge should use unread count endpoint and refresh after messages are read', async () => {
  const layout = await readSource('../src/layouts/MainLayout.vue')
  const commonApi = await readSource('../src/api/common.js')
  const notifications = await readSource('../src/views/Notifications.vue')

  assert.match(commonApi, /export function getUnreadNotificationCount\(\)/)
  assert.match(commonApi, /url:\s*'\/api\/notifications\/unread-count'/)
  assert.match(layout, /getUnreadNotificationCount/)
  assert.match(layout, /normalizeUnreadCount/)
  assert.equal(layout.includes('unreadOnly'), false)
  assert.match(layout, /window\.addEventListener\('rk-notifications-read-change',\s*fetchUnreadCount\)/)
  assert.match(notifications, /notifyUnreadCountChanged/)
  assert.match(notifications, /window\.dispatchEvent\(new CustomEvent\('rk-notifications-read-change'\)\)/)
})

test('portal history and alumni pages should use public display endpoints with robust response normalization', async () => {
  const history = await readSource('../src/views/History.vue')
  const alumni = await readSource('../src/views/Alumni.vue')
  const alumniApi = await readSource('../src/api/alumni.js')

  assert.match(history, /getPublicClubProfileConfig/)
  assert.match(history, /normalizeHistoryEvents/)
  assert.match(history, /loadProfileHistoryFallback/)
  assert.match(alumniApi, /getPublicAlumniGroupedByGeneration/)
  assert.match(alumniApi, /getPublicAlumniOverview/)
  assert.match(alumni, /getPublicAlumniGroupedByGeneration/)
  assert.match(alumni, /deriveOverviewFromGroups/)
  assert.match(alumni, /normalizeGroupedAlumni/)
})

test('admin shell exposes clickable home affordances and readable light submenu text', async () => {
  const source = await readSource('../src/layouts/AdminLayout.vue')
  const style = styleOf(source)

  assert.match(source, /class="[^"]*admin-topbar-home[^"]*"[\s\S]*@click="goAdminHome"/)
  assert.match(source, /class="sidebar-visual-link"[\s\S]*@click="goVisualScreen"/)
  assert.match(source, /const\s+goAdminHome\s*=\s*\(\)\s*=>\s*\{[\s\S]*router\.push\('\/admin'\)/)
  assert.match(source, /const\s+goVisualScreen\s*=\s*\(\)\s*=>\s*\{[\s\S]*router\.push\('\/admin\/operation\/visual-screen'\)/)
  assert.match(style, /\.role-system-admin[\s\S]*\.el-sub-menu \.el-menu-item[\s\S]*color:\s*#52617a\s*!important/)
  assert.match(style, /\.role-club-manager[\s\S]*\.el-sub-menu \.el-menu-item[\s\S]*color:\s*#52617a\s*!important/)
})

test('admin shell exposes an internal visual big screen route from the lower-left visual card', async () => {
  const layout = await readSource('../src/layouts/AdminLayout.vue')
  const router = await readSource('../src/router/index.js')
  const screen = await readSource('../src/views/admin/operation/VisualScreen.vue')

  assert.match(layout, /aria-label="打开可视化大屏"/)
  assert.match(layout, />可视化大屏</)
  assert.match(layout, /class="sidebar-visual-link"[\s\S]*@click="goVisualScreen"/)
  assert.match(router, /path:\s*'operation\/visual-screen'/)
  assert.match(router, /name:\s*'AdminVisualScreen'/)
  assert.match(router, /VisualScreen\.vue/)
  assert.match(screen, /运维可视化大屏/)
  assert.match(screen, /getK8sOverview/)
  assert.match(screen, /getMonitoringOverview/)
  assert.match(screen, /echarts\.init/)
})
