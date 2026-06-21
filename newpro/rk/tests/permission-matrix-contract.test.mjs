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

test('permission matrix is routed, menu-managed, and backed by a single aggregate API', async () => {
  const router = await readSource('newpro/rk/src/router/index.js')
  const api = await readSource('newpro/rk/src/api/role.js')
  const menuSeed = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/config/AuthMenuCatalogUpdater.java')

  assert.match(router, /path:\s*'system\/permission-matrix'/)
  assert.match(router, /name:\s*'AdminPermissionMatrix'/)
  assert.match(router, /PermissionMatrix\.vue/)
  assert.match(router, /roles:\s*SYSTEM_SCOPED_ADMIN_ROLE_IDS/)

  assert.match(api, /function\s+getPermissionMatrix/)
  assert.match(api, /url:\s*`?['"]?\/menus\/permission-matrix/)
  assert.match(api, /tenantId/)

  assert.match(menuSeed, /admin_permission_matrix/)
  assert.match(menuSeed, /\/admin\/system\/permission-matrix/)
  assert.match(menuSeed, /权限矩阵/)
})

test('permission matrix page renders roles, menus, summaries, filters, and export action', async () => {
  const page = await readSource('newpro/rk/src/views/admin/system/PermissionMatrix.vue')

  for (const marker of [
    'getPermissionMatrix',
    '权限矩阵',
    '角色覆盖率',
    '菜单覆盖率',
    '未授权菜单',
    '按角色筛选',
    '按菜单搜索',
    '导出矩阵',
    'matrixRows',
    'roleColumns',
    'assignedMenuIds',
    'coverageRate',
    'tenantId'
  ]) {
    assert.match(page, new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }
})

test('rk-auth exposes permission matrix service and value objects', async () => {
  const controller = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/controller/MenuController.java')
  const service = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/service/IPermissionMatrixService.java')
  const impl = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/service/impl/PermissionMatrixServiceImpl.java')
  const vo = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/domain/vo/PermissionMatrixVO.java')

  assert.match(controller, /@GetMapping\("\/permission-matrix"\)/)
  assert.match(controller, /PermissionMatrixService/)
  assert.match(service, /PermissionMatrixVO\s+buildMatrix\(Long tenantId\)/)
  assert.match(impl, /listAssignableRoles\([^)]+\)/)
  assert.match(impl, /roleMenuService/)
  assert.match(impl, /coverageRate/)

  for (const marker of [
    'RoleColumn',
    'MenuRow',
    'MatrixCell',
    'Summary',
    'assigned',
    'assignedMenuIds',
    'menuCoverageRate',
    'roleCoverageRate'
  ]) {
    assert.match(vo, new RegExp(marker))
  }
})
