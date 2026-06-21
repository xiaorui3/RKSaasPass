import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  canAccessAdminRoute,
  canEnterAdmin
} from '../src/utils/adminAccess.js'
import { readFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(__dirname, '../../..')

async function readSource(relativePath) {
  return readFile(path.resolve(repoRoot, relativePath), 'utf8')
}

test('admin console entry depends on menu-management paths', () => {
  assert.equal(canEnterAdmin(1, []), false)
  assert.equal(canEnterAdmin(1, ['/admin/system/roles']), true)
})

test('admin route access is denied when menu path is not granted', () => {
  assert.equal(
    canAccessAdminRoute('/admin/operation/visual-screen', 1, [], [1]),
    false
  )
})

test('admin route access is allowed when menu-management grants the path', () => {
  assert.equal(
    canAccessAdminRoute('/admin/operation/visual-screen', 999, ['/admin/operation/visual-screen'], [1]),
    true
  )
})

test('menu-management route frontend policy matches backend tenant-admin menu audience', async () => {
  const routerSource = await readSource('newpro/rk/src/router/index.js')
  const updaterSource = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/config/AuthMenuCatalogUpdater.java')
  const routeStart = routerSource.indexOf("path: 'system/menus'")
  const routeEnd = routerSource.indexOf("path: 'system/tenants'", routeStart)
  const routeBlock = routerSource.slice(routeStart, routeEnd)

  assert.match(
    updaterSource,
    /"admin_menus"[\s\S]*"\/admin\/system\/menus"[\s\S]*tenantAdmin\(\)/
  )
  assert.match(
    routeBlock,
    /path:\s*'system\/menus'[\s\S]*roles:\s*SYSTEM_SCOPED_ADMIN_ROLE_IDS/
  )
  assert.doesNotMatch(
    routeBlock,
    /path:\s*'system\/menus'[\s\S]*roles:\s*SUPER_ADMIN_ROLE_IDS/
  )
})

test('admin dashboard quick actions are filtered by persisted menu paths instead of hard-coded role lists', async () => {
  const dashboardSource = await readSource('newpro/rk/src/views/admin/Dashboard.vue')
  const quickActionsStart = dashboardSource.indexOf('const quickActions = computed')
  assert.notEqual(quickActionsStart, -1, 'dashboard quickActions computed block should exist')
  const quickActionsEnd = dashboardSource.indexOf('const summaryCards = computed', quickActionsStart)
  const quickActionsBlock = dashboardSource.slice(quickActionsStart, quickActionsEnd)

  assert.match(quickActionsBlock, /userStore\.canAccessAdminPath\(action\.path\)/)
  assert.doesNotMatch(quickActionsBlock, /roles:\s*\[/)
  assert.doesNotMatch(quickActionsBlock, /action\.roles\.includes/)
  assert.doesNotMatch(quickActionsBlock, /userRole\.value/)
})

test('admin dashboard pending items are filtered by persisted menu paths', async () => {
  const dashboardSource = await readSource('newpro/rk/src/views/admin/Dashboard.vue')
  const pendingStart = dashboardSource.indexOf('const pendingItems = computed')
  assert.notEqual(pendingStart, -1, 'dashboard pendingItems computed block should exist')
  const pendingEnd = dashboardSource.indexOf('onMounted(async', pendingStart)
  const pendingBlock = dashboardSource.slice(pendingStart, pendingEnd)

  assert.match(pendingBlock, /userStore\.canAccessAdminPath\(item\.path\)/)
  assert.doesNotMatch(pendingBlock, /roles:\s*\[/)
  assert.doesNotMatch(pendingBlock, /item\.roles\.includes/)
})

test('button permission helper is backed by menu paths, not a hard-coded role-id matrix', async () => {
  const permissionSource = await readSource('newpro/rk/src/utils/permission.js')

  assert.doesNotMatch(permissionSource, /const permissionMap\s*=\s*\{/)
  assert.doesNotMatch(permissionSource, /allowedRoles\.includes\(roleId\)/)
  assert.match(permissionSource, /const permissionPathMap\s*=\s*\{/)
  assert.match(permissionSource, /userStore\.canAccessAdminPath\(path\)/)
  assert.match(permissionSource, /content:news:add[\s\S]*\/admin\/content\/news/)
  assert.match(permissionSource, /alumni:add[\s\S]*\/admin\/club\/alumni/)
})

test('admin router imports every role-id constant used in route metadata', async () => {
  const routerSource = await readSource('newpro/rk/src/router/index.js')
  const importMatch = routerSource.match(/^import\s*\{([^}]*)\}\s*from\s*['"]@\/utils\/adminAccess['"]/m)
  assert.ok(importMatch, 'router should import admin access constants from utils/adminAccess')

  const importedNames = new Set(
    importMatch[1]
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean)
  )
  const usedRoleConstants = new Set(
    [...routerSource.matchAll(/\b[A-Z_]+_ROLE_IDS\b/g)].map((match) => match[0])
  )

  for (const roleConstant of usedRoleConstants) {
    assert.ok(
      importedNames.has(roleConstant),
      `${roleConstant} is used in router metadata but is not imported`
    )
  }
})

test('admin menu-auth failures stay inside the admin layout instead of jumping to frontend home', async () => {
  const routerSource = await readSource('newpro/rk/src/router/index.js')
  const denialStart = routerSource.indexOf('if (!canAccessByDynamicMenu)')
  assert.notEqual(denialStart, -1, 'admin route guard should keep a menu-auth denial branch')
  const denialEnd = routerSource.indexOf('}', denialStart)
  const denialBlock = routerSource.slice(denialStart, denialEnd)

  assert.match(denialBlock, /AdminForbidden/)
  assert.doesNotMatch(denialBlock, /name:\s*['"`]Home['"`]/)
  assert.match(routerSource, /name:\s*['"`]AdminForbidden['"`][\s\S]*skipMenuAuth:\s*true/)
})

test('new admin business pages are nested under the admin route and backed by menu seed paths', async () => {
  const routerSource = await readSource('newpro/rk/src/router/index.js')
  const updaterSource = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/config/AuthMenuCatalogUpdater.java')
  const baseMenuSql = await readSource('basedata/05_insert_menu_data.sql')
  const adminRouteStart = routerSource.indexOf('const adminRoutes')
  const frontendRouteStart = routerSource.indexOf('const frontendRoutes')
  const adminRouteBlock = routerSource.slice(adminRouteStart)
  const frontendRouteBlock = routerSource.slice(frontendRouteStart, adminRouteStart)
  const newAdminPaths = [
    '/admin/system/tenant-self-service',
    '/admin/system/permission-matrix',
    '/admin/system/open-source-health',
    '/admin/system/api-catalog',
    '/admin/system/demo-data',
    '/admin/statistics/data-quality',
    '/admin/statistics/security-audit',
    '/admin/statistics/participation',
    '/admin/statistics/growth-retention',
    '/admin/statistics/content-calendar'
  ]

  for (const path of newAdminPaths) {
    const childPath = path.replace('/admin/', '')
    assert.match(
      adminRouteBlock,
      new RegExp(`path:\\s*['"\`]${childPath.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}['"\`]`),
      `${path} should be registered as an AdminLayout child route`
    )
    assert.doesNotMatch(
      frontendRouteBlock,
      new RegExp(childPath.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
      `${path} must not be registered under the frontend layout`
    )
    assert.match(
      updaterSource,
      new RegExp(path.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
      `${path} should be seeded in backend menu management`
    )
    assert.match(
      baseMenuSql,
      new RegExp(path.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
      `${path} should be present in fresh-install base menu SQL`
    )
  }
})

test('unknown admin menu paths render inside AdminLayout instead of a blank standalone page', async () => {
  const routerSource = await readSource('newpro/rk/src/router/index.js')
  const adminRouteStart = routerSource.indexOf('const adminRoutes')
  const adminRouteBlock = routerSource.slice(adminRouteStart)

  assert.match(
    adminRouteBlock,
    /path:\s*['"`]:pathMatch\(\.\*\)\*['"`][\s\S]*name:\s*['"`]AdminNotFound['"`]/,
    'admin routes should include a child catch-all page under AdminLayout'
  )
  assert.match(
    adminRouteBlock,
    /name:\s*['"`]AdminNotFound['"`][\s\S]*skipMenuAuth:\s*true/,
    'admin catch-all should skip menu auth to avoid redirect loops'
  )
  assert.doesNotMatch(
    routerSource,
    /path:\s*['"`]\/:pathMatch\(\.\*\)\*['"`][\s\S]*AdminNotFound/,
    'admin not-found must not be registered as a top-level standalone page'
  )
})
