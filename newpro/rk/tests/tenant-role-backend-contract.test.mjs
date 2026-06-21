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

test('role controller delegates tenant-scoped create and update to role service', async () => {
  const source = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/controller/RoleController.java')

  assert.match(source, /roleService\.listAssignableRoles\(null\)/)
  assert.match(source, /roleService\.createTenantRole\(roleDTO\)/)
  assert.match(source, /roleService\.updateTenantRole\(id,\s*roleDTO\)/)
  assert.doesNotMatch(source, /roleService\.save\(role\)/)
  assert.doesNotMatch(source, /roleService\.lambdaUpdate\(\)/)
  assert.doesNotMatch(source, /queryRoleByIdAcrossTenants\(id\)/)
})

test('role service creates custom roles inside the resolved tenant scope', async () => {
  const source = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/service/impl/RoleServiceImpl.java')
  const role = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/domain/po/Role.java')

  assert.match(source, /createTenantRole\(RoleDTO roleDTO\)/)
  assert.match(source, /role\.setTenantId\(scopedTenantId\)/)
  assert.match(source, /role\.setDepId\(scopedTenantId\)/)
  assert.match(source, /role\.setType\(Role\.RoleType\.CUSTOM\.getValue\(\)\)/)
  assert.match(source, /normalizeCustomRoleCode/)
  assert.match(role, /BUILT_IN_ROLE_CODES/)
  assert.match(role, /BUILT_IN_ROLE_CODES\.contains\(normalizedCode\)/)
})

test('role service repairs legacy custom roles with empty tenant scope before listing', async () => {
  const source = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/service/impl/RoleServiceImpl.java')

  assert.match(source, /repairLegacyRoleTenantScope\(scopedTenantId\)/)
  assert.match(source, /private void repairLegacyRoleTenantScope\(Long tenantId\)/)
  assert.match(source, /\.eq\(Role::getTenantId,\s*tenantId\)/)
  assert.match(source, /\.set\(Role::getDepId,\s*tenantId\)/)
})

test('role service ensures four default roles for every tenant before roles are listed or created', async () => {
  const service = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/service/impl/RoleServiceImpl.java')
  const role = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/domain/po/Role.java')

  assert.match(service, /REQUIRED_TENANT_ROLES\s*=\s*List\.of\(/)
  for (const marker of [
    'new RoleSeedDefinition("ADMIN", "管理员")',
    'new RoleSeedDefinition("USER", "普通用户")',
    'new RoleSeedDefinition("CLUB_MANAGER", "社团负责人")',
    'new RoleSeedDefinition("TEACHER", "指导老师")'
  ]) {
    assert.equal(service.includes(marker), true, `missing tenant default role seed ${marker}`)
  }

  assert.match(service, /listAssignableRoles\(Long tenantId\)[\s\S]*ensureTenantBaseRoles\(scopedTenantId\)/)
  assert.match(service, /createTenantRole\(RoleDTO roleDTO\)[\s\S]*ensureTenantBaseRoles\(scopedTenantId\)/)
  assert.match(service, /private void ensureTenantBaseRoles\(Long tenantId\)[\s\S]*ensureTenantRole\(tenantId,\s*seed\)/)
  assert.match(service, /role\.setTenantId\(tenantId\)/)
  assert.match(service, /role\.setDepId\(tenantId\)/)
  assert.match(role, /dto\.setBuiltIn\(/)
})

test('menu role binding verifies current-tenant role ownership', async () => {
  const service = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/service/impl/MenuServiceImpl.java')
  const roleService = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/service/IRoleService.java')

  assert.match(roleService, /Role requireAccessibleRole\(Long roleId\)/)
  assert.match(service, /roleService\.requireAccessibleRole\(roleId\)/)
  assert.doesNotMatch(service, /lambdaQuery\(\)\.eq\(com\.tianji\.auth\.domain\.po\.Role::getId,\s*roleId\)/)
})

test('tenant role menu binding cannot grant super-admin-only operation menus', async () => {
  const service = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/service/impl/MenuServiceImpl.java')

  assert.match(service, /ensureMenusAssignableToRole\(role,\s*menuIds\)/)
  assert.match(service, /private void ensureMenusAssignableToRole\(Role role,\s*List<Long> menuIds\)/)
  assert.match(service, /TenantContext\.isSuperAdmin\(\)/)
  assert.match(service, /isSuperAdminOnlyOperationMenu\(menu\)/)
  assert.match(service, /throw new BadRequestException\("Tenant roles cannot bind super-admin operation menus"\)/)
  assert.match(service, /\/admin\/operation\//)
})

test('custom role permission assignment replaces menus and linked privileges from the selected menu tree', async () => {
  const controller = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/controller/MenuController.java')
  const service = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/service/impl/MenuServiceImpl.java')
  const frontend = await readSource('newpro/rk/src/views/admin/system/Roles.vue')
  const roleApi = await readSource('newpro/rk/src/api/role.js')

  assert.match(controller, /@PostMapping\("\/role\/\{roleId\}"\)[\s\S]*menuService\.replaceRoleMenus\(roleId,\s*menuIds\)/)
  assert.match(service, /public void replaceRoleMenus\(Long roleId,\s*List<Long> menuIds\)/)
  assert.match(service, /roleMenuService\.removeByRoleId\(roleId\)/)
  assert.match(service, /rolePrivilegeService\.removeByRoleId\(roleId\)/)
  assert.match(service, /roleMenuService\.saveBatch\(roleMenus\)/)
  assert.match(service, /privilegeService\.lambdaQuery\(\)[\s\S]*\.in\(Privilege::getMenuId,\s*distinctMenuIds\)/)
  assert.match(service, /rolePrivilegeService\.saveBatch\(rolePrivileges\)/)
  assert.match(service, /privilegeCache\.initPrivilegesCache\(privilegeService\.listPrivilegeRoles\(\)\)/)

  assert.match(frontend, /getMenuTree/)
  assert.match(frontend, /getRoleMenuIds\(row\.id\)/)
  assert.match(frontend, /menuTreeRef\.value\.getCheckedKeys\(\)/)
  assert.match(frontend, /assignRoleMenus\(currentRoleId\.value,\s*checkedIds\)/)
  assert.match(roleApi, /url:\s*`\/menus\/role\/\$\{roleId\}`[\s\S]*method:\s*'post'[\s\S]*data:\s*menuIds/)
})

test('removing role menus also removes linked privileges and refreshes privilege cache', async () => {
  const service = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/service/impl/MenuServiceImpl.java')
  const deleteStart = service.indexOf('public void deleteRoleMenus')
  assert.notEqual(deleteStart, -1, 'deleteRoleMenus method should exist')
  const deleteEnd = service.indexOf('private Role ensureRoleAccessible', deleteStart)
  const deleteBlock = service.slice(deleteStart, deleteEnd)

  assert.match(deleteBlock, /privilegeService\.lambdaQuery\(\)[\s\S]*\.in\(Privilege::getMenuId,\s*menuIds\)/)
  assert.match(deleteBlock, /rolePrivilegeService\.deleteRolePrivileges\(roleId,\s*privilegeIds\)/)
  assert.match(deleteBlock, /roleMenuService\.deleteRoleMenus\(roleId,\s*menuIds\)/)
  assert.match(deleteBlock, /privilegeCache\.initPrivilegesCache\(privilegeService\.listPrivilegeRoles\(\)\)/)
})

test('current user menu lookup is scoped to the active session role', async () => {
  const menuService = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/service/impl/MenuServiceImpl.java')
  const tenantFilter = await readSource('rk-common/src/main/java/com/tianji/common/filters/TenantHeaderFilter.java')
  const frontendRequest = await readSource('newpro/rk/src/utils/request.js')

  assert.match(menuService, /Long currentRoleId = RoleContext\.getRoleId\(\)/)
  assert.match(menuService, /Collections\.singletonList\(currentRoleId\)/)
  assert.match(menuService, /else if \(currentRoleId != null\) \{\s*return Collections\.emptyList\(\);/s)
  assert.match(tenantFilter, /roleId > 0/)
  assert.match(frontendRequest, /config\.headers\['X-Role-Id'\] = String\(roleId\)/)
})

test('menu catalog create update and delete are guarded by super admin access', async () => {
  const controller = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/controller/MenuController.java')
  const service = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/service/impl/MenuServiceImpl.java')
  const serviceApi = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/service/IMenuService.java')

  assert.match(serviceApi, /void updateMenu\(Menu menu\)/)
  assert.match(controller, /menuService\.updateMenu\(new Menu\(menuDTO\)\)/)
  assert.doesNotMatch(controller, /menuService\.updateById/)
  assert.match(service, /public void updateMenu\(Menu menu\)/)
  assert.match(service, /ensureSuperAdminMenuCatalogAccess\(\);\s*updateById\(menu\);/s)
})
