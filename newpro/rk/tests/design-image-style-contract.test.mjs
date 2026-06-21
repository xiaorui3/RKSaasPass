import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

async function readSource(relativePath) {
  return readFile(new URL(relativePath, import.meta.url), 'utf8')
}

test('public portal should use the provided app dashboard shell while keeping real nav items', async () => {
  const layout = await readSource('../src/layouts/MainLayout.vue')
  const home = await readSource('../src/views/Home.vue')

  assert.match(layout, /class="portal-app-shell"/)
  assert.match(layout, /class="portal-sidebar"/)
  assert.match(layout, /class="portal-side-nav"/)
  assert.match(layout, /v-for="item in navItems"/)
  assert.match(layout, /class="portal-search-pill"/)
  assert.match(layout, /class="portal-topbar"/)

  assert.match(home, /class="portal-dashboard-grid"/)
  assert.match(home, /class="home-hero-banner"/)
  assert.match(home, /class="home-today-card"/)
  assert.match(home, /class="home-notice-card"/)
  assert.match(home, /class="club-recommendation-grid"/)
  assert.equal(home.includes('/assets/visuals/rk-campus-hero.png'), true)
})

test('admin console should use role-aware reference dashboard styling for system admin and other roles', async () => {
  const layout = await readSource('../src/layouts/AdminLayout.vue')
  const dashboard = await readSource('../src/views/admin/Dashboard.vue')

  assert.match(layout, /adminShellRoleClass/)
  assert.match(layout, /class="admin-sidebar-brand"/)
  assert.match(layout, /role-system-admin/)
  assert.match(layout, /role-teacher/)
  assert.match(layout, /role-club-manager/)
  assert.match(layout, /class="admin-topbar-title"/)

  assert.match(dashboard, /class="dashboard-design-shell"/)
  assert.match(dashboard, /class="dashboard-workbench-banner"/)
  assert.match(dashboard, /class="dashboard-summary-grid"/)
  assert.match(dashboard, /class="dashboard-reference-card"/)
  assert.equal(dashboard.includes('/assets/visuals/rk-admin-ops.png'), true)
})
