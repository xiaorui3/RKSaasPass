import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

async function readSource(relativePath) {
  return readFile(new URL(relativePath, import.meta.url), 'utf8')
}

test('backend ops schema and controller should expose build history, service registry, nacos, k8s actions and topology', async () => {
  const schema = await readSource('../../../rk-user/src/main/java/com/tianji/user/config/AdminOpsSchemaUpdater.java')
  const controller = await readSource('../../../rk-user/src/main/java/com/tianji/user/controller/AdminOpsController.java')
  const service = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/IAdminOpsService.java')
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const k8sDeployScript = await readSource('../../../ci/remote/build_deploy_service_k8s.sh')

  assert.equal(schema.includes('ops_build_record'), true)
  assert.equal(schema.includes('ops_build_log'), true)
  assert.equal(schema.includes('ops_service_registry'), true)
  assert.equal(schema.includes('ensureOpsBuildRecordColumns'), true)
  assert.equal(schema.includes('ensureOpsServiceRegistryColumns'), true)
  assert.equal(controller.includes('/jenkins/build-history'), true)
  assert.equal(controller.includes('/jenkins/services'), true)
  assert.equal(controller.includes('/nacos/configs'), true)
  assert.equal(controller.includes('/k8s/workloads'), true)
  assert.equal(controller.includes('/topology'), true)
  assert.equal(service.includes('getJenkinsBuildHistory'), true)
  assert.equal(service.includes('listServiceRegistry'), true)
  assert.equal(service.includes('listNacosConfigs'), true)
  assert.equal(service.includes('runK8sWorkloadAction'), true)
  assert.equal(service.includes('getServiceTopology'), true)
  assert.equal(impl.includes('loadK8sWorkloadsFromPods'), true)
  assert.equal(k8sDeployScript.includes('deployments/scale'), true)
  assert.equal(k8sDeployScript.includes('statefulsets/scale'), true)
})

test('frontend ops api should wrap the new operation platform endpoints', async () => {
  const api = await readSource('../src/api/admin-ops.js')

  assert.equal(api.includes('getJenkinsBuildHistory'), true)
  assert.equal(api.includes('listOpsServices'), true)
  assert.equal(api.includes('saveOpsService'), true)
  assert.equal(api.includes('deleteOpsService'), true)
  assert.equal(api.includes('listNacosConfigs'), true)
  assert.equal(api.includes('getNacosConfig'), true)
  assert.equal(api.includes('saveNacosConfig'), true)
  assert.equal(api.includes('getK8sWorkloads'), true)
  assert.equal(api.includes('runK8sWorkloadAction'), true)
  assert.equal(api.includes('getOpsTopology'), true)
})

test('jenkins console should render persisted build history and editable service registry', async () => {
  const source = await readSource('../src/views/admin/operation/Jenkins.vue')

  assert.equal(source.includes('构建历史'), true)
  assert.equal(source.includes('微服务注册'), true)
  assert.equal(source.includes('getJenkinsBuildHistory'), true)
  assert.equal(source.includes('listOpsServices'), true)
  assert.equal(source.includes('row.buildUrl'), true)
  assert.equal(source.includes('serviceRegistryDialogVisible'), true)
  assert.equal(source.includes('historyFilters'), true)
})

test('k8s page should support workload restart and scale controls through backend api', async () => {
  const source = await readSource('../src/views/admin/operation/K8s.vue')

  assert.equal(source.includes('工作负载操作'), true)
  assert.equal(source.includes('getK8sWorkloads'), true)
  assert.equal(source.includes('runK8sWorkloadAction'), true)
  assert.equal(source.includes('RESTART'), true)
  assert.equal(source.includes('SCALE'), true)
})

test('admin routes and menu should expose nacos config center and topology pages', async () => {
  const router = await readSource('../src/router/index.js')
  const layout = await readSource('../src/layouts/AdminLayout.vue')
  const nacosPage = await readSource('../src/views/admin/operation/NacosConfig.vue')
  const topologyPage = await readSource('../src/views/admin/operation/Topology.vue')

  assert.equal(router.includes("path: 'operation/nacos'"), true)
  assert.equal(router.includes("path: 'operation/topology'"), true)
  assert.equal(layout.includes('/admin/operation/nacos'), true)
  assert.equal(layout.includes('/admin/operation/topology'), true)
  assert.equal(nacosPage.includes('Nacos 配置中心'), true)
  assert.equal(nacosPage.includes('saveNacosConfig'), true)
  assert.equal(topologyPage.includes('微服务拓扑'), true)
  assert.equal(topologyPage.includes('getOpsTopology'), true)
})
