<template>
  <div class="admin-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <div>
            <h1>K8s 管理</h1>
            <p>真实探测 kubectl、集群可达性，以及当前远程容器环境</p>
          </div>
          <el-button @click="fetchOverview">刷新</el-button>
        </div>
      </template>

      <div class="stats-grid">
        <div class="stat-card">
          <span class="label">kubectl</span>
          <span class="value" :class="overview.kubectlInstalled ? 'success' : 'danger'">
            {{ overview.kubectlInstalled ? '已安装' : '未安装' }}
          </span>
        </div>
        <div class="stat-card">
          <span class="label">集群连接</span>
          <span class="value" :class="overview.clusterReachable ? 'success' : 'warning'">
            {{ overview.clusterReachable ? '已连接' : '未连接' }}
          </span>
        </div>
        <div class="stat-card">
          <span class="label">客户端版本</span>
          <span class="value small">{{ overview.clientVersion || '-' }}</span>
        </div>
        <div class="stat-card">
          <span class="label">运行容器数</span>
          <span class="value">{{ overview.containers.length }}</span>
        </div>
      </div>

      <el-alert
        :type="overview.clusterReachable ? 'success' : 'warning'"
        :closable="false"
        show-icon
        class="cluster-alert"
        :title="overview.clusterMessage || '未获取到集群信息'"
      />

      <el-card shadow="never" class="topology-card">
        <template #header>
          <div class="cleanup-header">
            <span>节点 Pod 端口拓扑</span>
            <div class="topology-summary">
              <el-tag effect="plain">Node {{ overview.nodes.length }}</el-tag>
              <el-tag type="success" effect="plain">Pod {{ podCount }}</el-tag>
              <el-tag type="warning" effect="plain">端口 {{ portCount }}</el-tag>
            </div>
          </div>
        </template>
        <div v-if="topologyGraph.nodes.length" ref="topologyChartRef" class="topology-chart" />
        <el-empty v-else description="当前未获取到 Pod 与端口拓扑数据" />
      </el-card>

      <el-card shadow="never" class="cleanup-card">
        <template #header>
          <div class="cleanup-header">
            <span>清理空间控制台</span>
            <el-tag type="warning" effect="plain">按机器 IP 执行</el-tag>
          </div>
        </template>
        <el-form label-width="110px" class="cleanup-form">
          <el-form-item label="节点 IP">
            <el-select
              v-model="cleanupForm.nodeIp"
              placeholder="请选择要清理的机器 IP"
              filterable
              style="width: 100%"
              @change="handleCleanupNodeChange"
            >
              <el-option
                v-for="node in overview.nodes"
                :key="node.internalIp || node.name"
                :label="`${node.internalIp || '-'} / ${node.name || '-'}`"
                :value="node.internalIp"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="清理动作">
            <el-checkbox-group v-model="cleanupForm.actions">
              <el-checkbox label="IMAGE_PRUNE">清理无用镜像</el-checkbox>
              <el-checkbox label="LOG_CLEAN">清理日志</el-checkbox>
              <el-checkbox label="NODE_CACHE_CLEAN">清理节点缓存</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <el-form-item label="执行模式">
            <el-switch
              v-model="cleanupForm.dryRun"
              active-text="仅预览"
              inactive-text="实际清理"
            />
          </el-form-item>
          <el-form-item label="操作原因">
            <el-input v-model.trim="cleanupForm.reason" placeholder="记录到审计日志" />
          </el-form-item>
          <el-form-item>
            <el-button
              type="danger"
              :loading="cleanupLoading"
              :disabled="!cleanupForm.nodeIp || !cleanupForm.actions.length"
              @click="handleRunCleanup"
            >
              执行清理
            </el-button>
            <span class="cleanup-hint">实际清理只会清理未使用镜像，并截断容器日志内容，不删除业务数据。</span>
          </el-form-item>
        </el-form>
        <div v-if="cleanupResult" class="cleanup-result">
          <el-alert
            :type="cleanupResult.success ? 'success' : 'error'"
            :closable="false"
            show-icon
            :title="cleanupResult.message || '清理结果'"
          />
          <pre>{{ cleanupResult.output || '无输出' }}</pre>
        </div>
      </el-card>

      <el-card shadow="never" class="image-registry-card">
        <template #header>
          <div class="cleanup-header">
            <span>镜像仓库清理</span>
            <div class="section-actions">
              <el-select v-model="imageFilterForm.nodeName" clearable filterable placeholder="全部节点" style="width: 220px">
                <el-option v-for="node in overview.nodes" :key="node.name" :label="`${node.name || '-'} / ${node.internalIp || '-'}`" :value="node.name" />
              </el-select>
              <el-input v-model.trim="imageFilterForm.keyword" clearable placeholder="镜像关键字" style="width: 220px" @keyup.enter="fetchK8sImages" />
              <el-switch v-model="imageFilterForm.unusedOnly" active-text="只看可清理" />
              <el-button :loading="imageLoading" @click="fetchK8sImages">刷新镜像</el-button>
              <el-button type="warning" plain :loading="imageCleanupLoading === 'dry-run'" @click="handleCleanupK8sImages(true)">预览清理</el-button>
              <el-button type="danger" :loading="imageCleanupLoading === 'run'" @click="handleCleanupK8sImages(false)">清理未使用镜像</el-button>
            </div>
          </div>
        </template>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="默认排除 Rainbond、Kube 系统组件和正在被工作负载使用的镜像；先预览再执行实际删除。"
        />
        <el-table :data="k8sImages" v-loading="imageLoading" stripe size="small" class="image-table" empty-text="暂无镜像数据">
          <el-table-column prop="nodeName" label="节点" min-width="150" show-overflow-tooltip />
          <el-table-column prop="image" label="镜像" min-width="280" show-overflow-tooltip />
          <el-table-column prop="tag" label="Tag" width="110" show-overflow-tooltip />
          <el-table-column prop="size" label="大小" width="110" />
          <el-table-column label="状态" width="180">
            <template #default="{ row }">
              <el-tag v-if="row.usedByWorkloads" type="success">使用中</el-tag>
              <el-tag v-else-if="row.platformImage" type="warning">平台镜像</el-tag>
              <el-tag v-else type="danger">可清理</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="引用工作负载" min-width="220">
            <template #default="{ row }">
              <el-tooltip
                :content="imageWorkloadRefsText(row)"
                placement="top"
                :disabled="imageWorkloadRefsText(row) === '-'"
              >
                <span class="table-ellipsis-text">{{ imageWorkloadRefsText(row) }}</span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="handleDownloadImageScript(row)">下载导出脚本</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="imageCleanupResult" class="cleanup-result">
          <el-alert
            :type="imageCleanupResult.success ? 'success' : 'warning'"
            :closable="false"
            show-icon
            :title="imageCleanupResult.message || '镜像清理结果'"
          />
          <pre>{{ imageCleanupResult.output || JSON.stringify(imageCleanupResult, null, 2) }}</pre>
        </div>
      </el-card>

      <el-card shadow="never" class="maintenance-card">
        <template #header>
          <div class="cleanup-header">
            <span>集群巡检与清理</span>
            <div class="topology-summary">
              <el-tag effect="plain">异常 Pod {{ abnormalPods.length }}</el-tag>
              <el-tag type="danger" effect="plain">可删除 {{ deletableAbnormalPodCount }}</el-tag>
              <el-tag type="warning" effect="plain">半月 XXL-Job</el-tag>
            </div>
          </div>
        </template>
        <el-form label-width="120px" class="cleanup-form">
          <el-form-item label="维护动作">
            <el-checkbox-group v-model="maintenanceForm.actions">
              <el-checkbox label="IMAGE_PRUNE">清理无用镜像</el-checkbox>
              <el-checkbox label="LOG_CLEAN">压缩清理日志</el-checkbox>
              <el-checkbox label="NODE_CACHE_CLEAN">清理节点缓存</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <el-form-item label="异常 Pod">
            <el-switch
              v-model="maintenanceForm.includeControllerManaged"
              active-text="允许包含控制器托管终止 Pod"
              inactive-text="默认保护控制器托管 Pod"
            />
          </el-form-item>
          <el-form-item>
            <el-button :loading="abnormalPodLoading" @click="fetchAbnormalPods">检测异常 Pod</el-button>
            <el-button :loading="maintenanceLoading === 'abnormal-dry'" @click="handleCleanupAbnormalPods(true)">预览删除异常 Pod</el-button>
            <el-button type="danger" plain :loading="maintenanceLoading === 'abnormal-run'" @click="handleCleanupAbnormalPods(false)">删除异常 Pod</el-button>
            <el-button :loading="maintenanceLoading === 'cluster-dry'" @click="handleRunClusterMaintenance(true)">预览全节点清理</el-button>
            <el-button type="danger" :loading="maintenanceLoading === 'cluster-run'" @click="handleRunClusterMaintenance(false)">执行全节点清理</el-button>
            <el-button type="primary" plain :loading="scheduleLoading" @click="handleSyncMaintenanceSchedule">同步半月 XXL-Job</el-button>
          </el-form-item>
        </el-form>
        <div class="subsection-title">异常 Pod 列表</div>
        <el-table :data="abnormalPods" v-loading="abnormalPodLoading" stripe size="small" empty-text="暂无异常 Pod" class="abnormal-pod-table">
          <el-table-column prop="namespace" label="Namespace" min-width="150" show-overflow-tooltip />
          <el-table-column prop="name" label="Pod" min-width="220" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="150">
            <template #default="{ row }">
              <el-tag :type="row.deletable ? 'danger' : 'warning'">{{ row.status || row.phase || '-' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="原因" min-width="160" show-overflow-tooltip />
          <el-table-column prop="nodeName" label="节点" min-width="150" show-overflow-tooltip />
          <el-table-column prop="age" label="存活" width="90" />
          <el-table-column label="控制器" min-width="180">
            <template #default="{ row }">
              <el-tooltip
                :content="podControllerText(row)"
                placement="top"
                :disabled="!podControllerText(row)"
              >
                <span class="table-ellipsis-text">{{ podControllerText(row) }}</span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column prop="suggestion" label="建议" min-width="240" show-overflow-tooltip />
        </el-table>
        <div v-if="clusterMaintenanceResult || abnormalCleanupResult || maintenanceScheduleResult" class="cleanup-result">
          <el-alert
            :type="(clusterMaintenanceResult?.success ?? abnormalCleanupResult?.success ?? maintenanceScheduleResult?.success) ? 'success' : 'warning'"
            :closable="false"
            show-icon
            :title="clusterMaintenanceResult?.message || abnormalCleanupResult?.message || maintenanceScheduleResult?.message || '维护结果'"
          />
          <pre>{{ maintenanceResultOutput }}</pre>
        </div>
      </el-card>

      <el-card shadow="never" class="workload-card">
        <template #header>
          <div class="cleanup-header">
            <span>工作负载操作</span>
            <div class="section-actions">
              <el-select v-model="workloadNamespace" placeholder="命名空间" filterable style="width: 220px" @change="fetchWorkloads">
                <el-option v-for="item in overview.namespaces" :key="item" :label="item" :value="item" />
              </el-select>
              <el-button :loading="workloadLoading" @click="fetchWorkloads">刷新工作负载</el-button>
            </div>
          </div>
        </template>
        <el-table :data="workloads" v-loading="workloadLoading" stripe empty-text="暂无工作负载">
          <el-table-column prop="kind" label="类型" width="120" />
          <el-table-column prop="name" label="名称" min-width="180" />
          <el-table-column label="副本" width="100">
            <template #default="{ row }">{{ row.readyReplicas ?? 0 }}/{{ row.replicas ?? 0 }}</template>
          </el-table-column>
          <el-table-column prop="containers" label="容器" min-width="180" show-overflow-tooltip />
          <el-table-column prop="images" label="镜像" min-width="240" show-overflow-tooltip />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ready' ? 'success' : 'warning'">{{ row.status === 'ready' ? '就绪' : '更新中' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="warning" plain :loading="workloadActionLoading === `${row.kind}/${row.name}/RESTART`" @click="handleWorkloadAction(row, 'RESTART')">
                重启
              </el-button>
              <el-button size="small" type="primary" plain :loading="workloadActionLoading === `${row.kind}/${row.name}/SCALE`" @click="openScalePrompt(row)">
                缩放
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="workloadActionResult" class="cleanup-result">
          <el-alert
            :type="workloadActionResult.success ? 'success' : 'error'"
            :closable="false"
            show-icon
            :title="workloadActionResult.message || '工作负载操作结果'"
          />
          <pre>{{ workloadActionResult.output || '无输出' }}</pre>
        </div>
      </el-card>

      <el-card shadow="never" class="pod-terminal-card">
        <template #header>
          <div class="cleanup-header">
            <span>Pod Terminal</span>
            <el-tag type="danger" effect="plain">kubectl exec</el-tag>
          </div>
        </template>
        <el-form label-width="110px" class="pod-terminal-form">
          <el-form-item label="Pod">
            <el-select v-model="selectedPodKey" filterable placeholder="Select pod/container" style="width: 100%" @change="syncSelectedPod">
              <el-option
                v-for="item in podContainerOptions"
                :key="item.key"
                :label="item.label"
                :value="item.key"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="Namespace">
            <el-input v-model.trim="podTerminalForm.namespace" />
          </el-form-item>
          <el-form-item label="Pod Name">
            <el-input v-model.trim="podTerminalForm.podName" />
          </el-form-item>
          <el-form-item label="Container">
            <el-input v-model.trim="podTerminalForm.containerName" />
          </el-form-item>
          <el-form-item label="Command">
            <el-input
              v-model="podTerminalForm.command"
              type="textarea"
              :rows="3"
              placeholder="pwd && ls -lah"
            />
          </el-form-item>
          <el-form-item label="Timeout">
            <el-input-number v-model="podTerminalForm.timeoutSeconds" :min="3" :max="120" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="execLoading" @click="handleRunPodExec">Run Command</el-button>
            <span class="cleanup-hint">Commands are recorded in operation audit logs.</span>
          </el-form-item>
        </el-form>
        <div v-if="execOutput" class="cleanup-result">
          <el-alert
            :type="execResult?.success ? 'success' : 'error'"
            :closable="false"
            show-icon
            :title="execResult?.message || 'Pod exec result'"
          />
          <pre>{{ execOutput }}</pre>
        </div>
      </el-card>

      <el-card shadow="never" class="audit-card">
        <template #header>
          <div class="cleanup-header">
            <span>操作审计</span>
            <el-button @click="fetchAuditLogs">刷新审计</el-button>
          </div>
        </template>
        <el-table :data="auditLogs" stripe size="small" empty-text="暂无审计记录">
          <el-table-column prop="createTime" label="时间" min-width="160" />
          <el-table-column prop="operationType" label="类型" min-width="130" />
          <el-table-column prop="targetName" label="目标" min-width="180" show-overflow-tooltip />
          <el-table-column prop="action" label="动作" width="110" />
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
          <el-table-column prop="resultOutput" label="结果" min-width="240" show-overflow-tooltip />
        </el-table>
      </el-card>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-card shadow="never">
            <template #header>
              <span>节点列表</span>
            </template>
            <el-table :data="overview.nodes" v-loading="loading" stripe empty-text="当前未检测到可访问节点">
              <el-table-column prop="name" label="节点名" min-width="120" />
              <el-table-column prop="status" label="状态" width="100" />
              <el-table-column prop="roles" label="角色" min-width="120" />
              <el-table-column prop="version" label="版本" width="120" />
              <el-table-column prop="internalIp" label="内网 IP" min-width="120" />
            </el-table>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="never">
            <template #header>
              <span>命名空间</span>
            </template>
            <el-empty v-if="!overview.namespaces.length" description="当前未检测到命名空间" />
            <el-tag v-for="item in overview.namespaces" :key="item" class="namespace-tag">{{ item }}</el-tag>
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="never" class="container-card">
        <template #header>
          <span>远程容器环境</span>
        </template>
        <el-table :data="overview.containers" v-loading="loading" stripe>
          <el-table-column prop="name" label="容器名" min-width="120" />
          <el-table-column prop="image" label="镜像" min-width="220" />
          <el-table-column prop="ports" label="端口映射" min-width="220" />
        </el-table>
      </el-card>
    </el-card>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  cleanupK8sAbnormalPods,
  ensureK8sMaintenanceSchedule,
  getK8sOverview,
  getK8sWorkloads,
  listK8sImages,
  listK8sAbnormalPods,
  listOpsAuditLogs,
  cleanupK8sImages,
  downloadK8sImageExportScript,
  runK8sCleanup,
  runK8sClusterMaintenance,
  runK8sPodExec,
  runK8sWorkloadAction
} from '@/api/admin-ops'
import { echarts } from '@/utils/echarts'
import { buildK8sTopologyGraph, buildK8sTopologyOption } from '@/utils/k8sTopology'

const loading = ref(false)
const cleanupLoading = ref(false)
const workloadLoading = ref(false)
const workloadActionLoading = ref('')
const abnormalPodLoading = ref(false)
const maintenanceLoading = ref('')
const scheduleLoading = ref(false)
const execLoading = ref(false)
const imageLoading = ref(false)
const imageCleanupLoading = ref('')
const cleanupResult = ref(null)
const imageCleanupResult = ref(null)
const abnormalCleanupResult = ref(null)
const clusterMaintenanceResult = ref(null)
const maintenanceScheduleResult = ref(null)
const workloadActionResult = ref(null)
const execResult = ref(null)
const execOutput = ref('')
const auditLogs = ref([])
const abnormalPods = ref([])
const dangerConfirmText = ref('')
const workloadNamespace = ref('shetuanguanlixitong')
const workloads = ref([])
const k8sImages = ref([])
const selectedPodKey = ref('')
const topologyChartRef = ref(null)
let topologyChart = null
const overview = reactive({
  kubectlInstalled: false,
  clusterReachable: false,
  clientVersion: '-',
  clusterMessage: '',
  nodes: [],
  namespaces: [],
  containers: []
})
const cleanupForm = reactive({
  nodeName: '',
  nodeIp: '',
  actions: ['IMAGE_PRUNE'],
  dryRun: true,
  confirmText: '',
  reason: ''
})
const maintenanceForm = reactive({
  actions: ['IMAGE_PRUNE', 'LOG_CLEAN', 'NODE_CACHE_CLEAN'],
  includeControllerManaged: false
})
const imageFilterForm = reactive({
  nodeName: '',
  keyword: '',
  unusedOnly: true
})
const podTerminalForm = reactive({
  namespace: 'shetuanguanlixitong',
  podName: '',
  containerName: '',
  command: 'pwd && ls -lah',
  timeoutSeconds: 20,
  reason: 'admin pod terminal'
})

const podContainerOptions = computed(() =>
  (overview.containers || [])
    .filter((item) => item.namespace && item.podName && item.containerName)
    .map((item) => ({
      key: `${item.namespace}/${item.podName}/${item.containerName}`,
      label: `${item.namespace} / ${item.podName} / ${item.containerName}`,
      item
    }))
)

const topologyGraph = computed(() => buildK8sTopologyGraph({
  nodes: overview.nodes || [],
  pods: overview.containers || []
}))

const podCount = computed(() => new Set((overview.containers || []).map((item) => `${item.namespace}/${item.podName}`)).size)
const portCount = computed(() => (overview.containers || []).reduce((sum, item) => {
  if (!item.ports || item.ports === '-') return sum
  return sum + String(item.ports).split(',').filter(Boolean).length
}, 0))
const deletableAbnormalPodCount = computed(() => (abnormalPods.value || []).filter((item) => item.deletable).length)
const maintenanceResultOutput = computed(() => {
  const payload = clusterMaintenanceResult.value || abnormalCleanupResult.value || maintenanceScheduleResult.value
  if (!payload) return ''
  if (payload.output) return payload.output
  return JSON.stringify(payload, null, 2)
})

function imageWorkloadRefsText(row = {}) {
  const refs = Array.isArray(row.workloadRefs) ? row.workloadRefs.filter(Boolean) : []
  return refs.length ? refs.join('，') : '-'
}

function podControllerText(row = {}) {
  return row.controllerManaged ? `${row.ownerKind || '-'} / ${row.ownerName || '-'}` : '独立 Pod'
}

async function renderTopologyChart() {
  await nextTick()
  if (!topologyChartRef.value || !topologyGraph.value.nodes.length) {
    return
  }
  if (!topologyChart) {
    topologyChart = echarts.init(topologyChartRef.value)
  }
  topologyChart.setOption(buildK8sTopologyOption(topologyGraph.value), true)
  topologyChart.resize()
}

async function fetchOverview() {
  loading.value = true
  try {
    const res = await getK8sOverview()
    if (res.code !== 200 || !res.data) {
      throw new Error(res.msg || '获取 K8s 环境失败')
    }
    Object.assign(overview, {
      kubectlInstalled: !!res.data.kubectlInstalled,
      clusterReachable: !!res.data.clusterReachable,
      clientVersion: res.data.clientVersion || '-',
      clusterMessage: res.data.clusterMessage || '',
      nodes: res.data.nodes || [],
      namespaces: res.data.namespaces || [],
      containers: res.data.containers || []
    })
    if (!cleanupForm.nodeIp && overview.nodes.length) {
      cleanupForm.nodeIp = overview.nodes[0].internalIp || ''
      cleanupForm.nodeName = overview.nodes[0].name || ''
    }
    if (!overview.namespaces.includes(workloadNamespace.value) && overview.namespaces.length) {
      workloadNamespace.value = overview.namespaces.includes('shetuanguanlixitong') ? 'shetuanguanlixitong' : overview.namespaces[0]
    }
    if (!selectedPodKey.value && podContainerOptions.value.length) {
      selectedPodKey.value = podContainerOptions.value[0].key
      syncSelectedPod()
    }
    await renderTopologyChart()
    await fetchWorkloads()
    await fetchK8sImages()
  } catch (error) {
    ElMessage.error(error.message || '获取 K8s 环境失败')
  } finally {
    loading.value = false
  }
}

async function fetchK8sImages() {
  imageLoading.value = true
  try {
    const res = await listK8sImages({
      nodeName: imageFilterForm.nodeName || undefined,
      keyword: imageFilterForm.keyword || undefined,
      unusedOnly: imageFilterForm.unusedOnly
    })
    if (res.code !== 200 || !Array.isArray(res.data)) {
      throw new Error(res.msg || '获取 K8s 镜像失败')
    }
    k8sImages.value = res.data
  } catch (error) {
    ElMessage.error(error.message || '获取 K8s 镜像失败')
  } finally {
    imageLoading.value = false
  }
}

async function handleCleanupK8sImages(dryRun) {
  if (!dryRun && !imageFilterForm.nodeName) {
    ElMessage.warning('实际清理前请先选择节点')
    return
  }
  let confirmText = ''
  let reason = dryRun ? 'preview image cleanup' : 'cleanup unused runtime images'
  const node = overview.nodes.find((item) => item.name === imageFilterForm.nodeName)
  if (!dryRun) {
    const expectedText = node?.internalIp || imageFilterForm.nodeName
    try {
      const confirmed = await requestDangerConfirmation(expectedText, `确认清理 ${imageFilterForm.nodeName} 的未使用镜像`)
      confirmText = confirmed.confirmText
      reason = confirmed.reason
    } catch {
      return
    }
  }
  imageCleanupLoading.value = dryRun ? 'dry-run' : 'run'
  try {
    const res = await cleanupK8sImages({
      nodeName: imageFilterForm.nodeName || undefined,
      nodeIp: node?.internalIp || undefined,
      keyword: imageFilterForm.keyword || undefined,
      unusedOnly: true,
      dryRun,
      confirmText,
      reason
    })
    if (res.code !== 200 || !res.data) {
      throw new Error(res.msg || '镜像清理失败')
    }
    imageCleanupResult.value = res.data
    if (res.data.success) {
      ElMessage.success(res.data.message || (dryRun ? '镜像清理预览完成' : '镜像清理已执行'))
      await fetchK8sImages()
      await fetchAuditLogs()
    } else {
      ElMessage.warning(res.data.message || '镜像清理存在失败项')
    }
  } catch (error) {
    ElMessage.error(error.message || '镜像清理失败')
  } finally {
    imageCleanupLoading.value = ''
  }
}

async function handleDownloadImageScript(row) {
  if (!row?.image) {
    ElMessage.warning('该镜像缺少名称')
    return
  }
  try {
    const blob = await downloadK8sImageExportScript({
      nodeName: row.nodeName,
      nodeIp: row.nodeIp,
      image: row.image
    })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `export-${String(row.image).replace(/[/:@]/g, '_')}.sh`
    link.click()
    URL.revokeObjectURL(url)
  } catch (error) {
    ElMessage.error(error.message || '下载镜像导出脚本失败')
  }
}

function syncSelectedPod() {
  const selected = podContainerOptions.value.find((item) => item.key === selectedPodKey.value)?.item
  if (!selected) return
  podTerminalForm.namespace = selected.namespace || podTerminalForm.namespace
  podTerminalForm.podName = selected.podName || ''
  podTerminalForm.containerName = selected.containerName || ''
}

async function handleRunPodExec() {
  if (!podTerminalForm.namespace || !podTerminalForm.podName || !podTerminalForm.containerName) {
    ElMessage.warning('Please select namespace, pod, and container')
    return
  }
  if (!podTerminalForm.command.trim()) {
    ElMessage.warning('Please enter command')
    return
  }
  execLoading.value = true
  execOutput.value = ''
  try {
    const res = await runK8sPodExec({ ...podTerminalForm })
    if (res.code !== 200 || !res.data) {
      throw new Error(res.msg || 'Pod exec failed')
    }
    execResult.value = res.data
    execOutput.value = res.data.output || ''
    if (res.data.success) {
      ElMessage.success(res.data.message || 'Pod exec completed')
    } else {
      ElMessage.error(res.data.message || 'Pod exec failed')
    }
    await fetchAuditLogs()
  } catch (error) {
    ElMessage.error(error.message || 'Pod exec failed')
  } finally {
    execLoading.value = false
  }
}

async function fetchWorkloads() {
  workloadLoading.value = true
  try {
    const res = await getK8sWorkloads({ namespace: workloadNamespace.value })
    if (res.code !== 200 || !Array.isArray(res.data)) {
      throw new Error(res.msg || '获取 K8s 工作负载失败')
    }
    workloads.value = res.data
  } catch (error) {
    ElMessage.error(error.message || '获取 K8s 工作负载失败')
  } finally {
    workloadLoading.value = false
  }
}

async function fetchAbnormalPods() {
  abnormalPodLoading.value = true
  try {
    const res = await listK8sAbnormalPods()
    if (res.code !== 200 || !Array.isArray(res.data)) {
      throw new Error(res.msg || '检测异常 Pod 失败')
    }
    abnormalPods.value = res.data
  } catch (error) {
    ElMessage.error(error.message || '检测异常 Pod 失败')
  } finally {
    abnormalPodLoading.value = false
  }
}

async function handleCleanupAbnormalPods(dryRun) {
  let confirmText = ''
  let reason = dryRun ? 'preview abnormal pod cleanup' : 'delete abnormal pods'
  if (!dryRun) {
    try {
      const confirmed = await requestDangerConfirmation('CLEAN_ABNORMAL_PODS', '确认删除异常 Pod')
      confirmText = confirmed.confirmText
      reason = confirmed.reason
    } catch {
      return
    }
  }
  maintenanceLoading.value = dryRun ? 'abnormal-dry' : 'abnormal-run'
  try {
    const res = await cleanupK8sAbnormalPods({
      dryRun,
      includeControllerManaged: maintenanceForm.includeControllerManaged,
      confirmText,
      reason
    })
    if (res.code !== 200 || !res.data) {
      throw new Error(res.msg || '异常 Pod 清理失败')
    }
    abnormalCleanupResult.value = res.data
    clusterMaintenanceResult.value = null
    maintenanceScheduleResult.value = null
    if (res.data.success) {
      ElMessage.success(res.data.message || '异常 Pod 清理已提交')
      await fetchAbnormalPods()
      await fetchAuditLogs()
    } else {
      ElMessage.error(res.data.message || '异常 Pod 清理失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '异常 Pod 清理失败')
  } finally {
    maintenanceLoading.value = ''
  }
}

async function handleRunClusterMaintenance(dryRun) {
  let confirmText = ''
  let reason = dryRun ? 'preview cluster maintenance' : 'run cluster maintenance'
  if (!dryRun) {
    try {
      const confirmed = await requestDangerConfirmation('CLEAN_CLUSTER', '确认执行全节点清理')
      confirmText = confirmed.confirmText
      reason = confirmed.reason
    } catch {
      return
    }
  }
  maintenanceLoading.value = dryRun ? 'cluster-dry' : 'cluster-run'
  try {
    const res = await runK8sClusterMaintenance({
      actions: maintenanceForm.actions,
      cleanupNodeDisk: true,
      cleanupAbnormalPods: true,
      includeControllerManaged: maintenanceForm.includeControllerManaged,
      dryRun,
      confirmText,
      reason
    })
    if (res.code !== 200 || !res.data) {
      throw new Error(res.msg || '集群巡检与清理失败')
    }
    clusterMaintenanceResult.value = res.data
    abnormalCleanupResult.value = null
    maintenanceScheduleResult.value = null
    if (res.data.success) {
      ElMessage.success(res.data.message || '集群巡检与清理已执行')
    } else {
      ElMessage.warning(res.data.message || '集群巡检与清理存在失败项')
    }
    await fetchAbnormalPods()
    await fetchAuditLogs()
  } catch (error) {
    ElMessage.error(error.message || '集群巡检与清理失败')
  } finally {
    maintenanceLoading.value = ''
  }
}

async function handleSyncMaintenanceSchedule() {
  scheduleLoading.value = true
  try {
    const res = await ensureK8sMaintenanceSchedule({
      actions: maintenanceForm.actions,
      cleanupNodeDisk: true,
      cleanupAbnormalPods: true,
      includeControllerManaged: maintenanceForm.includeControllerManaged
    })
    if (res.code !== 200 || !res.data) {
      throw new Error(res.msg || '同步半月 XXL-Job 失败')
    }
    maintenanceScheduleResult.value = res.data
    clusterMaintenanceResult.value = null
    abnormalCleanupResult.value = null
    ElMessage.success(res.data.message || '半月 XXL-Job 已同步')
  } catch (error) {
    ElMessage.error(error.message || '同步半月 XXL-Job 失败')
  } finally {
    scheduleLoading.value = false
  }
}

async function handleWorkloadAction(row, action, replicas) {
  let confirmText = ''
  let reason = ''
  if (action === 'RESTART') {
    try {
      const confirmed = await requestDangerConfirmation(row.name, `确认重启 ${row.kind}/${row.name}`)
      confirmText = confirmed.confirmText
      reason = confirmed.reason
    } catch {
      return
    }
  }
  if (action === 'SCALE' && replicas === 0) {
    try {
      const confirmed = await requestDangerConfirmation(row.name, `确认将 ${row.kind}/${row.name} 缩放到 0`)
      confirmText = confirmed.confirmText
      reason = confirmed.reason
    } catch {
      return
    }
  }
  workloadActionLoading.value = `${row.kind}/${row.name}/${action}`
  try {
    const res = await runK8sWorkloadAction({
      namespace: workloadNamespace.value,
      kind: row.kind,
      name: row.name,
      action,
      replicas,
      confirmText,
      reason
    })
    if (res.code !== 200 || !res.data) {
      throw new Error(res.msg || '执行 K8s 操作失败')
    }
    workloadActionResult.value = res.data
    if (res.data.success) {
      ElMessage.success(res.data.message || 'K8s 操作已提交')
      await fetchWorkloads()
    } else {
      ElMessage.error(res.data.message || 'K8s 操作失败')
    }
    await fetchAuditLogs()
  } catch (error) {
    ElMessage.error(error.message || '执行 K8s 操作失败')
  } finally {
    workloadActionLoading.value = ''
  }
}

async function requestDangerConfirmation(expectedText, title) {
  const { value } = await ElMessageBox.prompt(`请输入 ${expectedText} 确认执行`, title, {
    confirmButtonText: '确认执行',
    cancelButtonText: '取消',
    inputPattern: new RegExp(`^${escapeRegExp(expectedText)}$`),
    inputErrorMessage: '确认文本不匹配',
    type: 'warning'
  })
  dangerConfirmText.value = value
  return {
    confirmText: value,
    reason: `${title}: ${expectedText}`
  }
}

function escapeRegExp(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

async function fetchAuditLogs() {
  try {
    const res = await listOpsAuditLogs({ limit: 30 })
    if (res.code === 200 && Array.isArray(res.data)) {
      auditLogs.value = res.data
    }
  } catch {
    // audit list is supplementary
  }
}

async function openScalePrompt(row) {
  try {
    const { value } = await ElMessageBox.prompt(`请输入 ${row.kind}/${row.name} 的目标副本数`, '缩放工作负载', {
      confirmButtonText: '执行缩放',
      cancelButtonText: '取消',
      inputValue: String(row.replicas ?? 1),
      inputPattern: /^([0-9]|1[0-9]|20)$/,
      inputErrorMessage: '副本数必须在 0 到 20 之间'
    })
    await handleWorkloadAction(row, 'SCALE', Number(value))
  } catch {
    // canceled
  }
}

function handleCleanupNodeChange(nodeIp) {
  const node = overview.nodes.find((item) => item.internalIp === nodeIp)
  cleanupForm.nodeName = node?.name || ''
}

async function handleRunCleanup() {
  if (!cleanupForm.nodeIp) {
    ElMessage.warning('请先选择节点 IP')
    return
  }
  if (!cleanupForm.actions.length) {
    ElMessage.warning('请至少选择一个清理动作')
    return
  }
  if (!cleanupForm.dryRun) {
    try {
      const confirmed = await requestDangerConfirmation(cleanupForm.nodeIp, `确认对 ${cleanupForm.nodeIp} 执行实际清理`)
      cleanupForm.confirmText = confirmed.confirmText
      cleanupForm.reason = cleanupForm.reason || confirmed.reason
    } catch {
      return
    }
  } else {
    cleanupForm.confirmText = ''
  }
  cleanupLoading.value = true
  try {
    const res = await runK8sCleanup({ ...cleanupForm })
    if (res.code !== 200 || !res.data) {
      throw new Error(res.msg || '执行清理失败')
    }
    cleanupResult.value = res.data
    if (res.data.success) {
      ElMessage.success(res.data.message || '清理命令已执行')
    } else {
      ElMessage.error(res.data.message || '清理执行失败')
    }
    await fetchAuditLogs()
  } catch (error) {
    ElMessage.error(error.message || '执行清理失败')
  } finally {
    cleanupLoading.value = false
  }
}

onMounted(() => {
  fetchOverview()
  fetchAbnormalPods()
  fetchAuditLogs()
  window.addEventListener('resize', handleResize)
})

function handleResize() {
  topologyChart?.resize()
}

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  topologyChart?.dispose()
  topologyChart = null
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.card-header h1 {
  margin: 0;
  font-size: 24px;
}

.card-header p {
  margin: 8px 0 0;
  color: #909399;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.label {
  color: #909399;
}

.value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.value.small {
  font-size: 16px;
}

.value.success {
  color: #67c23a;
}

.value.warning {
  color: #e6a23c;
}

.value.danger {
  color: #f56c6c;
}

.cluster-alert,
.container-card {
  margin-top: 16px;
}

.cleanup-card {
  margin-top: 16px;
}

.image-registry-card {
  margin-top: 16px;
}

.image-table {
  margin-top: 12px;
}

.maintenance-card {
  margin-top: 16px;
}

.abnormal-pod-table {
  margin-top: 8px;
}

.table-ellipsis-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  color: #334155;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}

.topology-card {
  margin-top: 16px;
}

.topology-chart {
  width: 100%;
  height: 430px;
}

.topology-summary {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.workload-card {
  margin-top: 16px;
}

.pod-terminal-card {
  margin-top: 16px;
}

.pod-terminal-form {
  max-width: 860px;
}

.cleanup-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-weight: 700;
}

.section-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.cleanup-form {
  max-width: 760px;
}

.cleanup-hint {
  margin-left: 12px;
  color: #909399;
  font-size: 13px;
}

.cleanup-result {
  margin-top: 12px;
}

.cleanup-result pre {
  max-height: 320px;
  overflow: auto;
  margin: 12px 0 0;
  padding: 12px;
  border-radius: 10px;
  background: #1f2933;
  color: #d8dee9;
  white-space: pre-wrap;
}

.namespace-tag {
  margin-right: 8px;
  margin-bottom: 8px;
}

@media (max-width: 768px) {
  .cleanup-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .cleanup-hint {
    display: block;
    margin: 8px 0 0;
  }
}
</style>
