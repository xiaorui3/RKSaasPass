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

test('demo data center is routed, menu-managed, and backed by rk-user API', async () => {
  const router = await readSource('newpro/rk/src/router/index.js')
  const api = await readSource('newpro/rk/src/api/demo-data.js')
  const menuSeed = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/config/AuthMenuCatalogUpdater.java')

  assert.match(router, /path:\s*'system\/demo-data'/)
  assert.match(router, /name:\s*'AdminDemoDataCenter'/)
  assert.match(router, /DemoDataCenter\.vue/)
  assert.match(router, /roles:\s*SUPER_ADMIN_ROLE_IDS/)

  for (const marker of [
    'getDemoDataStatus',
    'generateDemoData',
    'cleanupDemoData',
    'resetDemoData',
    '/api/demo-data/status',
    '/api/demo-data/generate',
    '/api/demo-data/cleanup',
    '/api/demo-data/reset'
  ]) {
    assert.match(api, new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }

  assert.match(menuSeed, /admin_demo_data_center/)
  assert.match(menuSeed, /\/admin\/system\/demo-data/)
  assert.match(menuSeed, /演示数据中心/)
  assert.match(menuSeed, /superAdminOnly\(\)/)
})

test('demo data center page exposes safe repeatable generate cleanup reset workflow', async () => {
  const page = await readSource('newpro/rk/src/views/admin/system/DemoDataCenter.vue')

  for (const marker of [
    '演示数据中心',
    '生成演示数据',
    '清理演示数据',
    '重置演示数据',
    '脱敏',
    '可重复执行',
    'DEMO_OPEN_SOURCE',
    'demoBatchId',
    'demoTenantCode',
    'sampleItems',
    'generateDemoData',
    'cleanupDemoData',
    'resetDemoData'
  ]) {
    assert.match(page, new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }

  assert.doesNotMatch(page, /ssh|kubeconfig|kubectl|Jenkins|MinIO|Nacos/i)
})

test('rk-user exposes demo data controller service and safe manifest objects', async () => {
  const controller = await readSource('rk-user/src/main/java/com/tianji/user/controller/DemoDataController.java')
  const service = await readSource('rk-user/src/main/java/com/tianji/user/service/IDemoDataService.java')
  const impl = await readSource('rk-user/src/main/java/com/tianji/user/service/impl/DemoDataServiceImpl.java')
  const vo = await readSource('rk-user/src/main/java/com/tianji/user/domain/vo/DemoDataCenterVO.java')

  assert.match(controller, /@RequestMapping\("\/api\/demo-data"\)/)
  assert.match(controller, /@GetMapping\("\/status"\)/)
  assert.match(controller, /@PostMapping\("\/generate"\)/)
  assert.match(controller, /@PostMapping\("\/cleanup"\)/)
  assert.match(controller, /@PostMapping\("\/reset"\)/)
  assert.match(controller, /hasAnyAuthority\('system:config:view',\s*'system:config:query',\s*'system:config:list'\)/)
  assert.match(controller, /hasAuthority\('system:config:edit'\)/)

  assert.match(service, /DemoDataCenterVO\s+getStatus\(\)/)
  assert.match(service, /DemoDataCenterVO\s+generate\(\)/)
  assert.match(service, /DemoDataCenterVO\s+cleanup\(\)/)
  assert.match(service, /DemoDataCenterVO\s+reset\(\)/)

  for (const marker of [
    'CONFIG_KEY',
    'open.source.demo.data',
    'DEMO_OPEN_SOURCE',
    'demoBatchId',
    'demoTenantCode',
    'cleanupDemoRows',
    'seedDemoTenant',
    'seedDemoUsers',
    'seedDemoClubMembers',
    'seedDemoNews',
    'seedDemoActivity',
    'seedDemoCompetition'
  ]) {
    assert.match(impl, new RegExp(marker))
  }

  assert.match(vo, /class\s+DemoDataCenterVO/)
  assert.match(vo, /class\s+SampleItem/)
  assert.match(vo, /class\s+ModuleStatus/)
  assert.match(vo, /Map<String,\s*Integer>\s+counts/)
  assert.doesNotMatch(impl, /AdminOps|DeployPackage|kubectl|Jenkins|SSH|kubeconfig/i)
})
