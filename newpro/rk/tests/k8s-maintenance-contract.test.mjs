import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs/promises'

const rootUrl = new URL('../../../', import.meta.url)

async function readRepoFile(relativePath) {
  return fs.readFile(new URL(relativePath, rootUrl), 'utf8')
}

test('admin ops exposes k8s abnormal pod and cluster maintenance APIs', async () => {
  const controller = await readRepoFile('rk-user/src/main/java/com/tianji/user/controller/AdminOpsController.java')
  const service = await readRepoFile('rk-user/src/main/java/com/tianji/user/service/IAdminOpsService.java')
  const impl = await readRepoFile('rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const api = await readRepoFile('newpro/rk/src/api/admin-ops.js')

  const requiredRoutes = [
    '/admin/ops/k8s/abnormal-pods',
    '/admin/ops/k8s/abnormal-pods/cleanup',
    '/admin/ops/k8s/maintenance/run',
    '/admin/ops/k8s/maintenance/schedule'
  ]

  for (const route of requiredRoutes) {
    const backendRoute = route.replace('/admin/ops', '')
    assert.match(controller, new RegExp(backendRoute.replaceAll('/', '\\/')), `controller missing ${route}`)
    assert.match(api, new RegExp(route.replaceAll('/', '\\/')), `frontend API missing ${route}`)
  }

  for (const method of [
    'listK8sAbnormalPods',
    'cleanupK8sAbnormalPods',
    'runK8sClusterMaintenance',
    'ensureK8sMaintenanceXxlJob'
  ]) {
    assert.match(service, new RegExp(`${method}\\s*\\(`), `service interface missing ${method}`)
    assert.match(impl, new RegExp(`${method}\\s*\\(`), `service impl missing ${method}`)
  }
})

test('k8s maintenance cleanup script covers disk pressure sources used in production', async () => {
  const impl = await readRepoFile('rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  const requiredSnippets = [
    'crictl --runtime-endpoint',
    'ps -a --state Exited',
    'rmi --prune',
    'journalctl --vacuum-size=200M',
    'dnf clean all',
    'find /var/lib/rancher/rke2/agent/containerd -path "*ingest*"'
  ]

  for (const snippet of requiredSnippets) {
    assert.ok(impl.includes(snippet), `cleanup script missing ${snippet}`)
  }
})

test('abnormal pod cleanup keeps controller-managed running workloads safe by default', async () => {
  const impl = await readRepoFile('rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const vo = await readRepoFile('rk-user/src/main/java/com/tianji/user/domain/vo/adminops/AdminK8sAbnormalPodVO.java')
  const dto = await readRepoFile('rk-user/src/main/java/com/tianji/user/domain/dto/adminops/AdminK8sAbnormalPodCleanupDTO.java')
  const handler = await readRepoFile('rk-user/src/main/java/com/tianji/user/handler/K8sMaintenanceJobHandler.java')

  for (const status of ['Failed', 'Evicted', 'Error', 'Completed', 'ImagePullBackOff', 'CrashLoopBackOff']) {
    assert.ok(impl.includes(status), `abnormal pod status missing ${status}`)
  }

  assert.match(vo, /private Boolean controllerManaged;/)
  assert.match(vo, /private Boolean deletable;/)
  assert.match(vo, /private String suggestion;/)
  assert.match(dto, /private Boolean includeControllerManaged;/)
  assert.match(dto, /private String confirmText;/)
  assert.match(impl, /includeControllerManaged/)
  assert.match(impl, /CLEAN_ABNORMAL_PODS/)
  assert.match(impl, /controllerManaged/)
  assert.match(handler, /@XxlJob\("rkK8sMaintenanceJobHandler"\)/)
})

test('admin k8s page exposes manual and half-month maintenance controls', async () => {
  const page = await readRepoFile('newpro/rk/src/views/admin/operation/K8s.vue')

  for (const text of [
    '集群巡检与清理',
    '检测异常 Pod',
    '预览删除异常 Pod',
    '删除异常 Pod',
    '预览全节点清理',
    '执行全节点清理',
    '同步半月 XXL-Job',
    '异常 Pod 列表'
  ]) {
    assert.ok(page.includes(text), `K8s page missing ${text}`)
  }

  assert.match(page, /fetchAbnormalPods/)
  assert.match(page, /handleCleanupAbnormalPods/)
  assert.match(page, /handleRunClusterMaintenance/)
  assert.match(page, /handleSyncMaintenanceSchedule/)
})
