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

test('security audit center has independent menu route and frontend API', async () => {
  const router = await readSource('newpro/rk/src/router/index.js')
  const dataApi = await readSource('newpro/rk/src/api/data.js')
  const menuSeed = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/config/AuthMenuCatalogUpdater.java')

  assert.match(router, /path:\s*'statistics\/security-audit'/)
  assert.match(router, /name:\s*'AdminSecurityAuditCenter'/)
  assert.match(router, /SecurityAuditCenter\.vue/)
  assert.match(dataApi, /function\s+getSecurityAuditCenter/)
  assert.match(dataApi, /function\s+notifySecurityAuditRisk/)
  assert.match(dataApi, /url:\s*['"`]\/api\/data\/security-audit-center['"`]/)
  assert.match(dataApi, /url:\s*['"`]\/api\/data\/security-audit-center\/notify['"`]/)

  assert.match(menuSeed, /admin_statistics_security_audit/)
  assert.match(menuSeed, /\/admin\/statistics\/security-audit/)
  assert.match(menuSeed, /"\/admin\/statistics\/security-audit"[\s\S]*backend\(\)/)
})

test('security audit center page renders tenant scoped risks and notification actions', async () => {
  const page = await readSource('newpro/rk/src/views/admin/statistics/SecurityAuditCenter.vue')

  for (const marker of [
    'getSecurityAuditCenter',
    'notifySecurityAuditRisk',
    'securityAuditCenter',
    'riskRows',
    'notifyLogs',
    'riskType',
    'riskLevel',
    'notifyStatus',
    'refreshSecurityAudit',
    'handleNotify',
    '安全审计中心',
    '跨租户访问',
    '高风险后台操作',
    '异常登录位置',
    '权限变更记录',
    '高风险通知',
    '菜单权限联动'
  ]) {
    assert.match(page, new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }
})

test('rk-data exposes security audit center API with redis cache and tenant scope', async () => {
  const controller = await readSource('rk-data/src/main/java/com/tianji/data/controller/SecurityAuditCenterController.java')
  const service = await readSource('rk-data/src/main/java/com/tianji/data/service/SecurityAuditCenterService.java')
  const impl = await readSource('rk-data/src/main/java/com/tianji/data/service/impl/SecurityAuditCenterServiceImpl.java')
  const vo = await readSource('rk-data/src/main/java/com/tianji/data/model/vo/SecurityAuditCenterVO.java')
  const dto = await readSource('rk-data/src/main/java/com/tianji/data/model/dto/SecurityAuditNotifyRequestDTO.java')
  const handler = await readSource('rk-data/src/main/java/com/tianji/data/handler/SecurityAuditCenterJobHandler.java')
  const redisConstants = await readSource('rk-data/src/main/java/com/tianji/data/constants/RedisConstants.java')

  assert.match(controller, /@RequestMapping\("\/data\/security-audit-center"\)/)
  assert.match(controller, /getSecurityAuditCenter\(tenantId,\s*riskType,\s*riskLevel,\s*notifyStatus\)/)
  assert.match(controller, /notifyRisk\(@RequestBody SecurityAuditNotifyRequestDTO request\)/)
  assert.match(service, /SecurityAuditCenterVO\s+getSecurityAuditCenter\(Long tenantId,\s*String riskType,\s*String riskLevel,\s*String notifyStatus\)/)
  assert.match(service, /SecurityAuditCenterVO\.NotifyLog\s+notifyRisk\(SecurityAuditNotifyRequestDTO request\)/)
  assert.match(service, /List<SecurityAuditCenterVO>\s+warmupAllTenants\(\)/)
  assert.match(redisConstants, /KEY_SECURITY_AUDIT_CENTER/)

  for (const marker of [
    'TenantContext',
    'StringRedisTemplate',
    'KEY_SECURITY_AUDIT_CENTER',
    'sys_oper_log',
    'sys_logininfor',
    'role_menu',
    'security_audit_notify_log',
    'cross_tenant',
    'high_risk_operation',
    'abnormal_login',
    'permission_change',
    'notifyStatus',
    'menuPermissionLinked',
    'securityAuditCenterWarmupJobHandler'
  ]) {
    assert.match(impl + vo + dto + handler, new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }
})
