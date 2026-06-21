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

test('gateway caches frontend public GET responses with tenant-scoped keys and anti avalanche safeguards', async () => {
  const pom = await readSource('rk-gateway/pom.xml')
  const filter = await readSource('rk-gateway/src/main/java/com/tianji/gateway/filter/PublicFrontendCacheFilter.java')
  const properties = await readSource('rk-gateway/src/main/java/com/tianji/gateway/config/PublicFrontendCacheProperties.java')

  assert.match(pom, /spring-boot-starter-data-redis/)
  assert.match(properties, /rk\.frontend-cache/)
  assert.match(properties, /ttlJitterSeconds/)
  assert.match(properties, /nullTtlSeconds/)
  assert.match(properties, /cacheableExactPaths/)
  assert.match(properties, /cacheablePathPrefixes/)
  assert.doesNotMatch(properties, /cacheablePathPrefixes[\s\S]*"\/"[\s\S]*"\/index\.html"/)

  for (const marker of [
    'implements GlobalFilter, Ordered',
    'StringRedisTemplate',
    'DataBufferUtils.join',
    'NettyWriteResponseFilter',
    'HttpHeaders.AUTHORIZATION',
    'containsLoginCookie',
    'Cache-Control',
    'X-RK-Cache',
    'HIT',
    'MISS',
    'NULL',
    'tenantId',
    'DigestUtils.md5DigestAsHex',
    'ttlWithJitter',
    'isCacheableResponse',
    'storeNullMarker',
    'fail open'
  ]) {
    assert.match(filter, new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }

  assert.match(filter, /NettyWriteResponseFilter\.WRITE_RESPONSE_FILTER_ORDER\s*-\s*1/)

  for (const pathPrefix of [
    '/api/news',
    '/api/works',
    '/api/comments',
    '/api/activity',
    '/api/competitions',
    '/api/notices',
    '/api/history',
    '/api/alumni/overview',
    '/api/alumni/statistics',
    '/tenants/list'
  ]) {
    assert.equal(filter.includes(pathPrefix) || properties.includes(pathPrefix), true, `missing cached path ${pathPrefix}`)
  }
  assert.doesNotMatch(properties, /"\/api\/alumni"/)
  assert.doesNotMatch(properties, /"\/"/)
})

test('rk-user exposes frontend cache overview, warmup endpoints, and hourly XXL-Job handler', async () => {
  const controller = await readSource('rk-user/src/main/java/com/tianji/user/controller/AdminOpsController.java')
  const service = await readSource('rk-user/src/main/java/com/tianji/user/service/IAdminOpsService.java')
  const impl = await readSource('rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const handler = await readSource('rk-user/src/main/java/com/tianji/user/handler/FrontendCacheWarmupJobHandler.java')
  const api = await readSource('newpro/rk/src/api/admin-ops.js')
  const page = await readSource('newpro/rk/src/views/admin/operation/RedisCache.vue')

  for (const marker of [
    '@GetMapping("/frontend-cache/overview")',
    '@PostMapping("/frontend-cache/warmup")',
    '@PostMapping("/frontend-cache/clear")',
    '@GetMapping("/visual-screen/overview")'
  ]) {
    assert.match(controller, new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }

  for (const marker of [
    'getFrontendCacheOverview()',
    'warmupFrontendCache()',
    'clearFrontendCache()',
    'getVisualScreenOverview()'
  ]) {
    assert.match(service, new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }

  for (const marker of [
    'rk:frontend-cache:',
    'publicFrontendWarmupPaths',
    'RestTemplate',
    'stringRedisTemplate',
    'ScanOptions.scanOptions',
    'connection.scan',
    'deleteFrontendCacheKeyBatch',
    'Redis unavailable',
    'warmupFrontendCache',
    'getVisualScreenOverview',
    'appendFrontendCacheSchedulerSync',
    'rk_tenant',
    'rk_user',
    'rk_activity',
    'rk_content',
    'rk_message',
    'onlineUsers',
    'registrationStats',
    'cacheSnapshot'
  ]) {
    assert.match(impl, new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }
  assert.doesNotMatch(impl, /stringRedisTemplate\.keys\(/)
  const overviewBody = impl.slice(
    impl.indexOf('public Map<String, Object> getFrontendCacheOverview()'),
    impl.indexOf('public Map<String, Object> warmupFrontendCache()')
  )
  assert.doesNotMatch(overviewBody, /findXxlJobByHandler|ensureFrontendCacheWarmupXxlJob|invokeXxlForm|invokeXxlAction/)
  assert.match(overviewBody, /概览接口不写调度中心/)
  assert.match(impl, /warmupFrontendCache\(\)[\s\S]*appendFrontendCacheSchedulerSync\(diagnostics\)/)

  assert.match(handler, /@XxlJob\("rkFrontendCacheWarmup"\)/)
  assert.match(handler, /warmupFrontendCache/)
  assert.match(handler, /XxlJobHelper/)

  for (const marker of [
    'getFrontendCacheOverview',
    'warmupFrontendCache',
    'clearFrontendCache',
    'getVisualScreenOverview'
  ]) {
    assert.match(api, new RegExp(marker))
  }

  for (const marker of [
    '前台缓存预热',
    '防穿透',
    '随机失效',
    'warmupFrontendCache',
    'clearFrontendCache',
    'getFrontendCacheOverview'
  ]) {
    assert.match(page, new RegExp(marker))
  }
})

test('visual screen uses tenant business metrics instead of only ops charts', async () => {
  const page = await readSource('newpro/rk/src/views/admin/operation/VisualScreen.vue')
  const dataApi = await readSource('newpro/rk/src/api/data.js')
  const vo = await readSource('rk-data/src/main/java/com/tianji/data/model/vo/SaasVisualScreenVO.java')
  const impl = await readSource('rk-data/src/main/java/com/tianji/data/service/impl/SaasVisualScreenServiceImpl.java')

  assert.match(dataApi, /getSaasVisualScreen/)
  assert.match(dataApi, /\/api\/data\/saas-visual-screen/)
  assert.match(dataApi, /\/api\/data\/cache\/frontend\/summary/)
  assert.doesNotMatch(dataApi, /url:\s*['"`]\/data\//)
  for (const marker of [
    '租户运营大屏',
    '在线用户',
    '报名统计',
    '内容总览',
    '缓存命中',
    'tenantCards',
    'onlineUsers',
    'registrationStats',
    'cacheSnapshot',
    'searchHealth',
    'Elasticsearch',
    'ES',
    'tenantId',
    'cache',
    'getSaasVisualScreen'
  ]) {
    assert.match(page, new RegExp(marker))
  }
  assert.doesNotMatch(page, /@\/api\/admin-ops/)

  assert.match(vo, /private SearchHealth searchHealth = new SearchHealth\(\)/)
  assert.match(vo, /private CacheSnapshot cacheSnapshot = new CacheSnapshot\(\)/)
  assert.match(vo, /public static class CacheSnapshot/)
  assert.match(vo, /public static class SearchHealth/)
  assert.match(vo, /private long documentCount/)
  assert.match(vo, /private long staleDocumentCount/)
  assert.match(vo, /private String indexStatus/)
  assert.match(page, /hasObjectValues\(overview\.onlineUsers\)/)
  assert.match(page, /hasObjectValues\(overview\.registrationStats\)/)
  assert.match(page, /hasObjectValues\(overview\.contentOverview\)/)
  assert.match(page, /hasObjectValues\(overview\.cacheSnapshot\)/)
  assert.match(page, /hasObjectValues\(overview\.searchHealth\)/)
  assert.match(impl, /vo\.getSearchHealth\(\)/)
  assert.match(impl, /vo\.getCacheSnapshot\(\)/)
  assert.match(impl, /resolveTable\("search_document"\)/)
  assert.match(impl, /Map\.entry\("search_document"/)
  assert.match(impl, /ScanOptions\.scanOptions\(\)\.match\("rk:frontend-cache:\*"\)/)
  assert.match(impl, /connection\.scan/)
  assert.doesNotMatch(impl, /redisTemplate\.keys\(/)
})

test('gateway exposes new data and search APIs behind the public api prefix', async () => {
  const gateway = await readSource('rk-gateway/src/main/resources/bootstrap.yml')

  assert.match(gateway, /id:\s*rk-search-api[\s\S]*Path=\/api\/search\/\*\*[\s\S]*RewritePath=\/api\/search\/\(\?<segment>\.\*\), \/\$\{segment\}/)
  assert.match(gateway, /id:\s*rk-data-api[\s\S]*Path=\/api\/data\/\*\*[\s\S]*RewritePath=\/api\/data\/\(\?<segment>\.\*\), \/data\/\$\{segment\}/)
})

test('remote migration stays as an independent menu/page separate from package export', async () => {
  const router = await readSource('newpro/rk/src/router/index.js')
  const updater = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/config/AuthMenuCatalogUpdater.java')
  const sql = await readSource('basedata/05_insert_menu_data.sql')
  const remotePage = await readSource('newpro/rk/src/views/admin/operation/RemoteMigration.vue')
  const deployPage = await readSource('newpro/rk/src/views/admin/operation/DeployPackage.vue')

  assert.match(router, /operation\/remote-migration/)
  assert.match(router, /AdminRemoteMigration/)
  assert.match(router, /RemoteMigration\.vue/)

  for (const marker of [
    'admin_operation_visual_screen',
    'admin_operation_redis_cache',
    'admin_operation_remote_migration'
  ]) {
    assert.match(updater, new RegExp(marker))
    assert.match(sql, new RegExp(marker))
  }

  assert.match(updater, /"远程迁移部署"[\s\S]*"admin_operation_remote_migration"[\s\S]*"\/admin\/operation\/remote-migration"/)
  assert.match(sql, /'远程迁移部署'[\s\S]*'admin_operation_remote_migration'[\s\S]*'\/admin\/operation\/remote-migration'/)

  assert.match(remotePage, /远程迁移部署/)
  assert.match(remotePage, /Linux SSH 部署/)
  assert.match(remotePage, /listRemoteMigrations/)
  assert.match(remotePage, /createLinuxSshMigration/)
  assert.match(remotePage, /createRemoteMigration/)

  assert.doesNotMatch(deployPage, /createRemoteMigration/)
  assert.doesNotMatch(deployPage, /createLinuxSshMigration/)
})
