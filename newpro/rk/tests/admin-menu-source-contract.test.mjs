import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(__dirname, '../../..')

async function readSource(relativePath) {
  return readFile(path.resolve(repoRoot, relativePath), 'utf8')
}

test('admin sidebar is driven by /menus/me instead of frontend menu injection', async () => {
  const source = await readSource('newpro/rk/src/layouts/AdminLayout.vue')

  assert.match(source, /dynamicAdminMenus/)
  assert.match(source, /mapBackendMenu/)
  assert.doesNotMatch(source, /ensureOperationMenuEntries/)
  assert.doesNotMatch(source, /ensureFinanceMenuEntry/)
  assert.doesNotMatch(source, /const\s+allMenus\s*=/)
  assert.doesNotMatch(source, /filterByRole/)
})

test('visual screen shortcut is visible only when the menu-management permission exists', async () => {
  const source = await readSource('newpro/rk/src/layouts/AdminLayout.vue')

  assert.match(source, /canAccessVisualScreen/)
  assert.match(source, /v-if="canAccessVisualScreen && \(!isCollapsed \|\| isMobile\)"/)
})

test('visual screen is registered in menu catalog and base SQL', async () => {
  const updater = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/config/AuthMenuCatalogUpdater.java')
  const seedSql = await readSource('basedata/05_insert_menu_data.sql')

  for (const source of [updater, seedSql]) {
    assert.match(source, /admin_operation_visual_screen/)
    assert.match(source, /\/admin\/operation\/visual-screen/)
    assert.match(source, /可视化大屏/)
  }
})
