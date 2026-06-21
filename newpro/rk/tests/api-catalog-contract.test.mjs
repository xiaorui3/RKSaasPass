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

test('api catalog is a system-management page, not the old tenant-1 ops workbench', async () => {
  const router = await readSource('newpro/rk/src/router/index.js')
  const api = await readSource('newpro/rk/src/api/api-catalog.js')
  const menuSeed = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/config/AuthMenuCatalogUpdater.java')

  assert.match(router, /path:\s*'system\/api-catalog'/)
  assert.match(router, /name:\s*'AdminApiCatalog'/)
  assert.match(router, /ApiCatalog\.vue/)
  assert.match(router, /roles:\s*SYSTEM_SCOPED_ADMIN_ROLE_IDS/)

  assert.match(api, /function\s+getApiCatalog/)
  assert.match(api, /url:\s*['"`]\/api\/api-catalog['"`]/)
  assert.match(api, /function\s+debugApiCatalogEndpoint/)
  assert.match(api, /url:\s*['"`]\/api\/api-catalog\/debug['"`]/)

  assert.match(menuSeed, /admin_api_catalog/)
  assert.match(menuSeed, /\/admin\/system\/api-catalog/)
  assert.match(menuSeed, /接口目录/)
  assert.doesNotMatch(menuSeed, /admin_api_catalog[\s\S]{0,180}superAdminOnly\(\)/)
})

test('api catalog page exposes catalog and safe debug workflow without token or raw header editing', async () => {
  const page = await readSource('newpro/rk/src/views/admin/system/ApiCatalog.vue')

  for (const marker of [
    '接口目录',
    '当前登录态',
    '当前租户上下文',
    '公开 API',
    '后台 API',
    '内部 API',
    '权限点',
    '示例请求',
    'debugApiCatalogEndpoint',
    'getApiCatalog',
    '服务分组',
    '请求调试'
  ]) {
    assert.match(page, new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }

  assert.doesNotMatch(page, /复制当前 Token|copyToken|tokenPreview|localStorage\.getItem\(['"`]token['"`]\)/)
  assert.doesNotMatch(page, /headersText|附加请求头|Authorization|Rainbond|tenant 1|canAccessApiWorkbench/)
})

test('rk-user exposes independent api catalog controller and service with controlled debug proxy', async () => {
  const controller = await readSource('rk-user/src/main/java/com/tianji/user/controller/ApiCatalogController.java')
  const service = await readSource('rk-user/src/main/java/com/tianji/user/service/IApiCatalogService.java')
  const impl = await readSource('rk-user/src/main/java/com/tianji/user/service/impl/ApiCatalogServiceImpl.java')
  const dto = await readSource('rk-user/src/main/java/com/tianji/user/domain/dto/ApiCatalogDebugRequestDTO.java')
  const vo = await readSource('rk-user/src/main/java/com/tianji/user/domain/vo/ApiCatalogVO.java')

  assert.match(controller, /@RequestMapping\("\/api\/api-catalog"\)/)
  assert.match(controller, /@GetMapping/)
  assert.match(controller, /@PostMapping\("\/debug"\)/)
  assert.match(controller, /hasAnyAuthority\('system:config:view',\s*'system:config:query',\s*'system:config:list'\)/)
  assert.match(service, /ApiCatalogVO\s+getCatalog\(\)/)
  assert.match(service, /ApiCatalogVO\.DebugResult\s+debug\(ApiCatalogDebugRequestDTO\s+request\)/)

  for (const marker of [
    'loadSwaggerResources',
    'parseEndpoint',
    'classifyEndpoint',
    'resolveCurrentAuthorizationHeader',
    'X-Tenant-Id',
    'GET',
    'POST',
    'PUT',
    'DELETE'
  ]) {
    assert.match(impl, new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }

  assert.match(dto, /private\s+String\s+path/)
  assert.match(dto, /private\s+String\s+method/)
  assert.match(dto, /private\s+String\s+queryJson/)
  assert.match(dto, /private\s+String\s+bodyJson/)
  assert.doesNotMatch(dto, /headers|authorization|token/i)

  assert.match(vo, /class\s+ApiCatalogVO/)
  assert.match(vo, /class\s+ServiceGroup/)
  assert.match(vo, /class\s+Endpoint/)
  assert.match(vo, /class\s+DebugResult/)
  assert.doesNotMatch(impl, /AdminOps|Rainbond|Jenkins|kubectl|ssh|copyToken|headersText/i)
})

test('api catalog backend bounds swagger collection latency and forwards tenant context', async () => {
  const impl = await readSource('rk-user/src/main/java/com/tianji/user/service/impl/ApiCatalogServiceImpl.java')

  assert.match(impl, /CATALOG_CONNECT_TIMEOUT/)
  assert.match(impl, /CATALOG_REQUEST_TIMEOUT/)
  assert.match(impl, /connectTimeout\(CATALOG_CONNECT_TIMEOUT\)/)
  assert.match(
    impl,
    /HttpRequest\.newBuilder\(URI\.create\(sanitizeUrl\(gatewayUrl\) \+ "\/swagger-resources"\)\)[\s\S]{0,180}\.timeout\(CATALOG_REQUEST_TIMEOUT\)/
  )
  assert.match(impl, /requestBuilder\.timeout\(CATALOG_REQUEST_TIMEOUT\)/)
  assert.match(impl, /resolveCurrentTenantHeader/)
  assert.match(impl, /requestBuilder\.header\("X-Tenant-Id", tenantId\)/)
})
