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

test('open source health page is routed, menu-managed, and backed by rk-data aggregate API', async () => {
  const router = await readSource('newpro/rk/src/router/index.js')
  const api = await readSource('newpro/rk/src/api/data.js')
  const menuSeed = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/config/AuthMenuCatalogUpdater.java')

  assert.match(router, /path:\s*'system\/open-source-health'/)
  assert.match(router, /name:\s*'AdminOpenSourceHealth'/)
  assert.match(router, /OpenSourceHealth\.vue/)
  assert.match(router, /roles:\s*SYSTEM_SCOPED_ADMIN_ROLE_IDS/)

  assert.match(api, /function\s+getOpenSourceHealth/)
  assert.match(api, /url:\s*['"`]\/api\/data\/open-source-health['"`]/)

  assert.match(menuSeed, /admin_open_source_health/)
  assert.match(menuSeed, /\/admin\/system\/open-source-health/)
  assert.match(menuSeed, /系统自检/)
})

test('open source health page shows every business application self-check category', async () => {
  const page = await readSource('newpro/rk/src/views/admin/system/OpenSourceHealth.vue')

  for (const marker of [
    'getOpenSourceHealth',
    '系统自检',
    '开源部署',
    '菜单 seed',
    '默认角色',
    'ES 索引',
    'Redis 缓存',
    '系统配置',
    '演示数据',
    '文件存储',
    'PASS',
    'WARN',
    'FAIL',
    'UNKNOWN',
    'refreshHealth',
    'healthChecks',
    '诊断建议'
  ]) {
    assert.match(page, new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }

  assert.doesNotMatch(page, /ssh|kubeconfig|kubectl|Jenkins/i)
})

test('rk-data exposes open source health controller, service, and value object without ops deploy coupling', async () => {
  const controller = await readSource('rk-data/src/main/java/com/tianji/data/controller/OpenSourceHealthController.java')
  const service = await readSource('rk-data/src/main/java/com/tianji/data/service/OpenSourceHealthService.java')
  const impl = await readSource('rk-data/src/main/java/com/tianji/data/service/impl/OpenSourceHealthServiceImpl.java')
  const vo = await readSource('rk-data/src/main/java/com/tianji/data/model/vo/OpenSourceHealthVO.java')

  assert.match(controller, /@RequestMapping\("\/data\/open-source-health"\)/)
  assert.match(controller, /OpenSourceHealthService/)
  assert.match(service, /OpenSourceHealthVO\s+check\(Long tenantId\)/)

  for (const marker of [
    'checkMenuSeed',
    'checkDefaultRoles',
    'checkSearchIndex',
    'checkRedisCache',
    'checkSystemConfig',
    'checkDemoData',
    'checkFileStorage'
  ]) {
    assert.match(impl, new RegExp(marker))
  }

  assert.match(vo, /class\s+OpenSourceHealthVO/)
  assert.match(vo, /class\s+CheckItem/)
  assert.match(vo, /class\s+Summary/)
  assert.match(vo, /status/)
  assert.match(vo, /suggestion/)
  assert.doesNotMatch(impl, /AdminOps|DeployPackage|kubectl|Jenkins|SSH/i)
})

test('open source health uses lightweight bounded checks for low-memory rk-data runtime', async () => {
  const impl = await readSource('rk-data/src/main/java/com/tianji/data/service/impl/OpenSourceHealthServiceImpl.java')
  const serviceMatrix = await readSource('ci/remote/service_matrix.sh')

  assert.match(impl, /countRequiredValues/)
  assert.doesNotMatch(impl, /SELECT path FROM/)
  assert.doesNotMatch(impl, /SELECT code FROM/)
  assert.doesNotMatch(impl, /frontendCacheWarmupService\.latestSummary\(\)/)
  assert.match(serviceMatrix, /rk-data\) echo "512m"\s+;;/)
  assert.match(serviceMatrix, /rk-data\) echo "1024m"\s+;;/)
  assert.match(serviceMatrix, /rk-data\) echo "-Xms128m -Xmx320m -XX:MaxMetaspaceSize=128m/)
})
