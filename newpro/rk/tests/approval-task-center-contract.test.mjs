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

test('approval task center has independent menu route and frontend API', async () => {
  const router = await readSource('newpro/rk/src/router/index.js')
  const dataApi = await readSource('newpro/rk/src/api/data.js')
  const menuSeed = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/config/AuthMenuCatalogUpdater.java')

  assert.match(router, /path:\s*'statistics\/approval-center'/)
  assert.match(router, /name:\s*'AdminApprovalTaskCenter'/)
  assert.match(router, /ApprovalTaskCenter\.vue/)
  assert.match(dataApi, /function\s+getApprovalTaskCenter/)
  assert.match(dataApi, /url:\s*['"`]\/api\/data\/approval-task-center['"`]/)
  assert.doesNotMatch(dataApi, /url:\s*['"`]\/data\/approval-task-center['"`]/)

  assert.match(menuSeed, /admin_statistics_approval_center/)
  assert.match(menuSeed, /\/admin\/statistics\/approval-center/)
  assert.match(menuSeed, /"\/admin\/statistics\/approval-center"[\s\S]*backend\(\)/)
})

test('approval task center page renders filters, sla summary and task list', async () => {
  const page = await readSource('newpro/rk/src/views/admin/statistics/ApprovalTaskCenter.vue')

  for (const marker of [
    'getApprovalTaskCenter',
    'tenantId',
    'taskType',
    'slaStatus',
    'status',
    'approvalTaskCenter',
    'summaryCards',
    'taskRows',
    'sourcePath',
    'refreshApprovalTasks',
    'el-segmented',
    'el-table',
    '统一审批任务中心',
    '待审总数',
    'SLA 风险',
    '超时任务',
    '来源类型',
    '审核入口'
  ]) {
    assert.match(page, new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }
})

test('rk-data exposes independent approval task center API with redis cache and tenant scope', async () => {
  const controller = await readSource('rk-data/src/main/java/com/tianji/data/controller/ApprovalTaskCenterController.java')
  const service = await readSource('rk-data/src/main/java/com/tianji/data/service/ApprovalTaskCenterService.java')
  const impl = await readSource('rk-data/src/main/java/com/tianji/data/service/impl/ApprovalTaskCenterServiceImpl.java')
  const vo = await readSource('rk-data/src/main/java/com/tianji/data/model/vo/ApprovalTaskCenterVO.java')
  const redisConstants = await readSource('rk-data/src/main/java/com/tianji/data/constants/RedisConstants.java')

  assert.match(controller, /@RequestMapping\("\/data\/approval-task-center"\)/)
  assert.match(controller, /getApprovalTaskCenter\(tenantId,\s*taskType,\s*slaStatus,\s*status\)/)
  assert.match(service, /ApprovalTaskCenterVO\s+getApprovalTaskCenter\(Long tenantId,\s*String taskType,\s*String slaStatus,\s*String status\)/)
  assert.match(redisConstants, /KEY_APPROVAL_TASK_CENTER/)

  for (const marker of [
    'TenantContext',
    'StringRedisTemplate',
    'KEY_APPROVAL_TASK_CENTER',
    'join_request',
    'activity',
    'competition',
    'news',
    'finance_reimbursement',
    'notice',
    'slaStatus',
    'sourcePath',
    'tenantScope',
    'warmupAllTenants'
  ]) {
    assert.match(impl + vo, new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }
})
