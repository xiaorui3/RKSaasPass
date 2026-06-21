import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs/promises'

const rootUrl = new URL('../../../', import.meta.url)

async function readRepoFile(relativePath) {
  return fs.readFile(new URL(relativePath, rootUrl), 'utf8')
}

test('tenant theme config supports portal default, style one classic, and admin style isolation', async () => {
  const dto = await readRepoFile('rk-user/src/main/java/com/tianji/user/domain/dto/TenantThemeConfigDTO.java')
  const controller = await readRepoFile('rk-user/src/main/java/com/tianji/user/controller/ThemeConfigController.java')
  const store = await readRepoFile('newpro/rk/src/stores/theme.js')
  const themePage = await readRepoFile('newpro/rk/src/views/admin/system/ThemeConfig.vue')
  const mainLayout = await readRepoFile('newpro/rk/src/layouts/MainLayout.vue')
  const router = await readRepoFile('newpro/rk/src/router/index.js')
  const styles = await readRepoFile('newpro/rk/src/styles/index.scss')

  assert.match(dto, /private String frontendStyle;/)
  assert.match(dto, /private String adminStyle;/)
  assert.match(controller, /STYLE_PORTAL/)
  assert.match(controller, /STYLE_CLASSIC/)
  assert.match(controller, /STYLE_BIP/)
  assert.match(controller, /setFrontendStyle\(STYLE_PORTAL\)/)
  assert.match(controller, /setAdminStyle\(STYLE_CLASSIC\)/)
  assert.match(controller, /normalizeFrontendStyle/)
  assert.match(controller, /normalizeAdminStyle/)

  assert.match(store, /const STYLE_PORTAL = 'portal'/)
  assert.match(store, /frontendStyle:\s*'portal'/)
  assert.match(store, /adminStyle:\s*'classic'/)
  assert.match(store, /normalizeFrontendStyleMode/)
  assert.match(store, /normalizeAdminStyleMode/)
  assert.match(store, /context === ADMIN_CONTEXT \? normalizeAdminStyleMode\(styleMode\) : normalizeFrontendStyleMode\(styleMode\)/)
  assert.match(store, /data-rk-ui-style/)
  assert.match(store, /data-rk-ui-context/)
  assert.match(store, /let themeApplySeq\s*=\s*0/)
  assert.match(store, /const applySeq = nextThemeApplySeq\(\)/)
  assert.match(store, /if\s*\(!isLatestThemeApply\(applySeq\)\)\s*{\s*return config\s*}/s)
  assert.match(store, /applyPreviewStyleMode\(context, styleMode\)\s*{\s*nextThemeApplySeq\(\)/)
  assert.match(store, /clearTheme\(\)\s*{\s*nextThemeApplySeq\(\)/)

  assert.match(themePage, /frontendStyleOptions/)
  assert.match(themePage, /adminStyleOptions/)
  assert.match(themePage, /value:\s*'portal'/)
  assert.match(themePage, /value:\s*'classic'/)
  assert.match(themePage, /value:\s*'bip'/)
  assert.match(themePage, /selectFrontendStyle/)
  assert.match(themePage, /selectAdminStyle/)

  assert.match(mainLayout, /classicNavItems/)
  assert.match(mainLayout, /portalNavItems/)
  assert.match(mainLayout, /isPortalFrontendStyle/)
  assert.match(mainLayout, /\/content/)
  assert.match(mainLayout, /\/events/)
  assert.match(mainLayout, /\/community/)
  assert.match(router, /name: 'PortalContentHub'/)
  assert.match(router, /name: 'PortalEventsHub'/)
  assert.match(router, /name: 'PortalCommunityHub'/)

  assert.match(styles, /\[data-rk-ui-style="bip"\]/)
  assert.match(styles, /YonBIP/)
  assert.match(styles, /--rk-bip-accent/)
})

test('redis cache console is visible in operation routes and persisted menu seeds', async () => {
  const router = await readRepoFile('newpro/rk/src/router/index.js')
  const layout = await readRepoFile('newpro/rk/src/layouts/AdminLayout.vue')
  const redisPage = await readRepoFile('newpro/rk/src/views/admin/operation/RedisCache.vue')
  const sql = await readRepoFile('basedata/05_insert_menu_data.sql')
  const updater = await readRepoFile('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/config/AuthMenuCatalogUpdater.java')

  assert.match(router, /operation\/redis-cache/)
  assert.match(router, /AdminRedisCache/)
  assert.doesNotMatch(layout, /\/admin\/operation\/redis-cache/)
  assert.match(layout, /filteredMenus/)
  assert.match(layout, /dynamicAdminMenus/)
  assert.match(redisPage, /warmupFrontendCache/)
  assert.match(redisPage, /clearFrontendCache/)
  assert.match(redisPage, /getFrontendCacheOverview/)
  assert.match(sql, /admin_operation_redis_cache/)
  assert.match(sql, /\/admin\/operation\/redis-cache/)
  assert.match(updater, /admin_operation_redis_cache/)
  assert.match(updater, /\/admin\/operation\/redis-cache/)
  assert.match(updater, /SYNC_MENU_NAME_CODES/)
  assert.match(updater, /admin_operation_deploy_package/)
})
