<template>
  <div class="remote-migration-page">
    <section class="page-header">
      <div>
        <h2>远程迁移部署</h2>
        <p>独立管理在线 Kubernetes 迁移、Linux SSH 部署、远程诊断修复和迁移日志，不再与一键打包下载共用页面。</p>
      </div>
      <div class="header-actions">
        <el-button :loading="loading" @click="loadRecords">刷新</el-button>
        <el-button type="primary" plain :loading="preflighting" @click="runPreflight">部署前预检</el-button>
        <el-button type="danger" :loading="remoteDeploying" @click="submitRemoteMigration">开始迁移</el-button>
      </div>
    </section>

    <section class="remote-deploy-panel">
      <div class="panel-copy">
        <strong>在线 Kubernetes 迁移</strong>
        <span>粘贴 kubeconfig 后，后台会临时写入文件执行 kubectl，全量 apply 当前微服务和中间件清单；不会保存 kubeconfig 明文。</span>
        <span>持久化数据会通过目标集群临时导入 Pod 执行：MySQL 导入 Job、MinIO 导入 Job，导入完成后自动清理临时资源。</span>
      </div>

      <el-input
        v-model="remoteForm.kubeconfigText"
        type="textarea"
        :rows="8"
        placeholder="粘贴 kubeconfig"
        class="kubeconfig-input"
      />

      <div class="remote-deploy-options">
        <el-select v-model="form.targetClusterType" class="cluster-select" placeholder="目标集群">
          <el-option label="ACK" value="ack" />
          <el-option label="标准 Kubernetes" value="k8s" />
          <el-option label="k3s" value="k3s" />
          <el-option label="自定义" value="custom" />
        </el-select>
        <el-input v-model="remoteForm.kubeContext" placeholder="kube context，可留空" clearable />
        <el-input v-model="form.targetNamespace" placeholder="目标命名空间" clearable />
        <el-input v-model="form.storageClassName" placeholder="StorageClass，可留空" clearable />
        <el-select v-model="form.externalExposureType" class="exposure-select" placeholder="公网入口">
          <el-option label="无公网入口" value="none" />
          <el-option label="NodePort" value="nodePort" />
          <el-option label="LoadBalancer" value="loadBalancer" />
          <el-option label="Ingress" value="ingress" />
        </el-select>
        <el-input v-model="form.registryPrefix" class="registry-input" placeholder="镜像仓库前缀" clearable />
        <el-input v-model="form.registryServer" placeholder="镜像仓库域名" clearable />
        <el-input v-model="form.targetDomain" placeholder="域名" clearable />
        <el-input-number
          v-if="form.externalExposureType === 'nodePort'"
          v-model="form.frontendNodePort"
          :min="30000"
          :max="32767"
          controls-position="right"
          placeholder="前端 NodePort"
        />
        <el-input-number
          v-if="form.externalExposureType === 'nodePort'"
          v-model="form.gatewayNodePort"
          :min="30000"
          :max="32767"
          controls-position="right"
          placeholder="网关 NodePort"
        />
        <el-input
          v-if="form.externalExposureType === 'ingress'"
          v-model="form.ingressClassName"
          placeholder="IngressClass，可留空"
          clearable
        />
        <el-input
          v-if="form.externalExposureType === 'ingress'"
          v-model="form.ingressHost"
          placeholder="Ingress Host"
          clearable
        />
        <el-checkbox v-model="remoteForm.waitRollout">等待 rollout 完成</el-checkbox>
        <el-checkbox v-model="remoteForm.dryRun">只做 dry-run</el-checkbox>
        <el-checkbox v-model="remoteForm.applyDatabaseSnapshot">导入当前数据库快照</el-checkbox>
        <el-checkbox v-model="remoteForm.applyMinioSnapshot">导入 MinIO 对象快照</el-checkbox>
        <el-input v-model="remoteForm.confirmText" placeholder="输入 REMOTE_DEPLOY 确认" clearable />
      </div>

      <el-alert
        v-if="form.targetClusterType === 'ack' && form.externalExposureType === 'loadBalancer'"
        type="warning"
        :closable="false"
        show-icon
        title="ACK LoadBalancer 会创建云 SLB 并产生云资源费用，确认需要公网负载均衡后再部署。"
      />
      <div v-if="preflightResult" class="preflight-result">
        <el-tag :type="preflightResult.passed ? 'success' : 'danger'">
          {{ preflightResult.passed ? '预检通过' : '预检未通过' }}
        </el-tag>
        <span>集群：{{ preflightResult.targetClusterType || form.targetClusterType }}</span>
        <span>StorageClass：{{ preflightResult.storageClassName || '默认' }}</span>
        <span>公网入口：{{ preflightResult.externalExposureType || form.externalExposureType }}</span>
        <small v-for="item in preflightResult.checks || []" :key="`check-${item}`">{{ item }}</small>
        <small v-for="item in preflightResult.warnings || []" :key="`warn-${item}`" class="warning-text">{{ item }}</small>
        <small v-for="item in preflightResult.errors || []" :key="`err-${item}`" class="error-text">{{ item }}</small>
      </div>
    </section>

    <section class="linux-ssh-panel">
      <div class="panel-copy">
        <strong>Linux SSH 部署</strong>
        <span>给出 Linux root SSH 后，后端会自动连接目标服务器，按国内源安装运行环境、上传部署包并执行 K8s 或 Docker Compose 部署。</span>
      </div>
      <div class="ssh-options">
        <el-input v-model="sshForm.sshHost" placeholder="服务器 IP/域名" clearable />
        <el-input-number v-model="sshForm.sshPort" :min="1" :max="65535" controls-position="right" />
        <el-input v-model="sshForm.sshUsername" placeholder="root 用户名" clearable />
        <el-input v-model="sshForm.sshPassword" type="password" show-password placeholder="root 密码" clearable />
        <el-select v-model="sshForm.linuxDeployMode" placeholder="部署模式">
          <el-option label="K8s 环境安装" value="k8s" />
          <el-option label="Docker Compose" value="docker-compose" />
          <el-option label="只安装运行环境" value="runtime-only" />
        </el-select>
        <el-checkbox v-model="sshForm.domesticMirror">国内源</el-checkbox>
        <el-checkbox v-model="sshForm.installK8s">安装 K8s 环境</el-checkbox>
        <el-checkbox v-model="sshForm.installDocker">安装 Docker</el-checkbox>
        <el-checkbox v-model="sshForm.copyServices">服务拷贝</el-checkbox>
        <el-input v-model="sshForm.targetPath" placeholder="目标目录" clearable />
        <el-input v-model="sshForm.confirmText" placeholder="输入 SSH_DEPLOY 确认" clearable />
        <el-button type="primary" :loading="linuxDeploying" @click="submitLinuxSshMigration">开始 SSH 部署任务</el-button>
      </div>
    </section>

    <section class="script-panel">
      <div>
        <strong>迁移日志</strong>
        <span>
          Kubernetes 模式会逐个镜像导出、推送、部署并清理，迁移状态写入独立远程迁移记录，关闭页面后会自动恢复。
          Linux SSH 模式会执行 SSH 连通性检查、运行环境安装、部署包上传、服务启动和健康检查，输出会实时追加到远程迁移日志。
        </span>
      </div>
    </section>

    <section v-if="runningRecords.length" class="progress-panel">
      <div class="progress-title">
        <strong>迁移进度</strong>
        <span>运行中的迁移任务会每 3 秒刷新。</span>
      </div>
      <div v-for="item in runningRecords" :key="item.id" class="progress-item">
        <div class="progress-copy">
          <span>{{ item.packageName }}</span>
          <small>{{ item.currentStep || latestLogLine(item.logs) || '等待后端写入迁移日志' }}</small>
          <small v-if="item.currentImage">当前镜像：{{ item.currentImage }}</small>
          <small v-if="item.totalImages">镜像进度：{{ imageProgressText(item) }}，{{ item.uploadPercent || 0 }}%</small>
        </div>
        <el-progress :percentage="item.progress || 0" />
      </div>
    </section>

    <section class="table-panel">
      <el-table :data="records" v-loading="loading" stripe empty-text="暂无远程迁移记录">
        <el-table-column prop="packageName" label="任务" min-width="230" />
        <el-table-column prop="deployMode" label="模式" width="140" />
        <el-table-column prop="remoteClusterName" label="目标" min-width="150" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="220">
          <template #default="{ row }">
            <el-progress :percentage="row.progress || 0" />
            <small class="muted">{{ row.currentStep || latestLogLine(row.logs) || '-' }}</small>
          </template>
        </el-table-column>
        <el-table-column label="镜像进度" width="210">
          <template #default="{ row }">
            <div class="image-progress">
              <span>{{ imageProgressText(row) }}</span>
              <el-progress :percentage="row.uploadPercent || 0" :show-text="false" />
              <small class="muted">{{ imageProgressTitle(row) }}</small>
              <small v-if="row.currentImage" class="muted">{{ row.currentImage }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openMigrationDetail(row)">详情</el-button>
            <el-button v-if="isKubeconfigMigration(row)" text type="warning" :loading="diagnosing === row.id" @click="runRemoteDiagnosis(row)">诊断</el-button>
            <el-dropdown v-if="isKubeconfigMigration(row)" trigger="click" @command="(action) => repairRemoteMigrationAction(row, action)">
              <el-button text type="danger" :loading="repairingAction.startsWith(`${row.id}:`)">修复</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-for="item in repairActions" :key="item.value" :command="item.value">
                    {{ item.label }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
        <el-table-column type="expand">
          <template #default="{ row }">
            <pre class="remote-migration-log">{{ row.logs || '暂无日志' }}</pre>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-drawer v-model="migrationHistoryDrawer" title="部署历史" size="52%" class="migration-history-drawer">
      <div v-loading="detailLoading" class="history-detail">
        <el-descriptions v-if="selectedMigrationDetail?.record" :column="2" border>
          <el-descriptions-item label="任务">{{ selectedMigrationDetail.record.packageName }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ selectedMigrationDetail.record.status }}</el-descriptions-item>
          <el-descriptions-item label="目标集群">{{ selectedMigrationDetail.record.remoteClusterName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="kubeconfig 指纹">{{ selectedMigrationDetail.kubeconfigFingerprint || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div class="history-actions">
          <el-button type="primary" plain :disabled="!selectedMigrationDetail?.kubeconfigFingerprint" @click="showKubeconfig">查看 kubeconfig 明文</el-button>
          <el-button plain :disabled="!isKubeconfigMigration(selectedMigrationDetail?.record)" @click="runRemoteDiagnosis(selectedMigrationDetail.record)">重新诊断</el-button>
        </div>
        <h4>请求摘要</h4>
        <pre class="remote-migration-log">{{ JSON.stringify(selectedMigrationDetail?.requestSummary || {}, null, 2) }}</pre>
        <h4>remoteDiagnosis</h4>
        <pre class="remote-migration-log">{{ JSON.stringify(remoteDiagnosis || selectedMigrationDetail?.lastDiagnosis || {}, null, 2) }}</pre>
        <h4>修复记录</h4>
        <pre class="remote-migration-log">{{ JSON.stringify(selectedMigrationDetail?.lastRepair || {}, null, 2) }}</pre>
      </div>
    </el-drawer>

    <el-dialog v-model="kubeconfigDialog" title="kubeconfig 明文" width="760px" class="kubeconfigDialog">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="这里展示的是远程集群 kubeconfig 明文，查看动作会写入运维审计。"
      />
      <el-input v-model="kubeconfigPlaintext" type="textarea" :rows="18" readonly class="kubeconfig-plaintext" />
    </el-dialog>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createLinuxSshMigration,
  createRemoteMigration,
  diagnoseRemoteMigration,
  getRemoteMigrationDetail,
  getRemoteMigrationKubeconfig,
  listRemoteMigrations,
  preflightRemoteMigration,
  repairRemoteMigration
} from '@/api/admin-ops'

const loading = ref(false)
const remoteDeploying = ref(false)
const linuxDeploying = ref(false)
const preflighting = ref(false)
const preflightResult = ref(null)
const records = ref([])
const runningRecords = ref([])
const migrationHistoryDrawer = ref(false)
const kubeconfigDialog = ref(false)
const selectedMigrationDetail = ref(null)
const remoteDiagnosis = ref(null)
const kubeconfigPlaintext = ref('')
const detailLoading = ref(false)
const diagnosing = ref(null)
const repairingAction = ref('')
let pollTimer = null

const DEFAULT_TARGET_DOMAIN = resolveDefaultTargetDomain()

function resolveDefaultTargetDomain() {
  if (typeof window === 'undefined' || !window.location) {
    return ''
  }
  const hostname = String(window.location.hostname || '').trim()
  if (!hostname || hostname === 'localhost' || /^\d{1,3}(\.\d{1,3}){3}$/.test(hostname)) {
    return ''
  }
  return hostname
}

const repairActions = [
  { label: '重启工作负载', value: 'REPAIR_RESTART_WORKLOADS' },
  { label: '清理异常 Pod', value: 'REPAIR_DELETE_STUCK_PODS' },
  { label: '修复 MinIO 公开策略', value: 'REPAIR_MINIO_PUBLIC_POLICY' },
  { label: '重跑 MySQL 导入', value: 'REPAIR_RERUN_MYSQL_IMPORT' },
  { label: '重跑 MinIO 导入', value: 'REPAIR_RERUN_MINIO_IMPORT' }
]

const form = reactive({
  packageMode: 'offline-full',
  deliveryMode: 'store-minio',
  selfContained: true,
  includeSource: true,
  includeDatabases: true,
  includeImages: true,
  includeK8sManifests: true,
  includeMinio: true,
  includeDockerCompose: true,
  includeAllCurrentData: true,
  exportRuntimeArtifacts: true,
  cleanupRuntimeArtifacts: true,
  deleteAfterDownload: true,
  imageArtifactMode: 'registry',
  registryPrefix: 'registry.example.com/rk-web',
  registryServer: 'registry.example.com',
  targetClusterType: 'ack',
  targetNamespace: 'shetuanguanlixitong',
  targetDomain: DEFAULT_TARGET_DOMAIN,
  storageClassName: 'alicloud-disk-essd',
  externalExposureType: 'loadBalancer',
  frontendNodePort: 30080,
  gatewayNodePort: 30010,
  ingressClassName: '',
  ingressHost: DEFAULT_TARGET_DOMAIN,
  note: ''
})

const remoteForm = reactive({
  kubeconfigText: '',
  kubeContext: '',
  waitRollout: true,
  rolloutTimeoutSeconds: 600,
  dryRun: false,
  applyDatabaseSnapshot: true,
  applyMinioSnapshot: true,
  confirmText: 'REMOTE_DEPLOY'
})

const sshForm = reactive({
  sshHost: '',
  sshPort: 22,
  sshUsername: 'root',
  sshPassword: '',
  linuxDeployMode: 'k8s',
  domesticMirror: true,
  installK8s: true,
  installDocker: true,
  copyServices: true,
  targetPath: '/opt/rk-web',
  confirmText: 'SSH_DEPLOY',
  imageArtifactMode: 'registry',
  exportRuntimeArtifacts: false,
  cleanupRuntimeArtifacts: true
})

watch(
  () => form.targetClusterType,
  (value) => {
    if (value === 'ack') {
      form.storageClassName = 'alicloud-disk-essd'
    } else if (value === 'k3s') {
      form.storageClassName = 'local-path'
    } else if (value === 'k8s' || value === 'custom') {
      form.storageClassName = ''
    }
    preflightResult.value = null
  }
)

watch(
  () => form.targetDomain,
  (value) => {
    if (!form.ingressHost || form.ingressHost === DEFAULT_TARGET_DOMAIN) {
      form.ingressHost = value || ''
    }
  }
)

watch(
  () => form.externalExposureType,
  () => {
    preflightResult.value = null
  }
)

function statusTag(status) {
  return { SUCCESS: 'success', RUNNING: 'warning', FAILED: 'danger' }[status] || 'info'
}

function latestLogLine(logs) {
  return String(logs || '')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .at(-1)
}

function numericValue(value) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

function imageProgressCount(row) {
  const total = numericValue(row?.totalImages)
  const uploaded = numericValue(row?.uploadedImages)
  const percent = numericValue(row?.uploadPercent)
  if (row?.status === 'SUCCESS' && total > 0 && (uploaded === 0 || percent >= 100)) {
    return total
  }
  return uploaded
}

function imageProgressTitle(row) {
  const mode = String(row?.imageArtifactMode || '')
  if (mode === 'offline-tar') return '镜像打包'
  if (mode === 'both') return '镜像打包/推送'
  if (mode === 'registry' || row?.registryPullPackage) return '镜像推送/拉取'
  return '镜像进度'
}

function imageProgressText(row) {
  const total = numericValue(row?.totalImages)
  if (total <= 0) return '-'
  return `${imageProgressCount(row)}/${total}`
}

function isKubeconfigMigration(row) {
  return Boolean(row && row.deployMode !== 'linux-ssh')
}

function setRecords(data) {
  records.value = data || []
  runningRecords.value = records.value.filter((item) => item.status === 'RUNNING')
  syncPolling()
}

async function loadRecords() {
  loading.value = true
  try {
    const res = await listRemoteMigrations()
    setRecords(res.data || [])
  } finally {
    loading.value = false
  }
}

function buildRemoteDeployPayload() {
  return {
    ...form,
    remoteDeploy: true,
    kubeconfig: remoteForm.kubeconfigText,
    kubeContext: remoteForm.kubeContext,
    waitRollout: remoteForm.waitRollout,
    rolloutTimeoutSeconds: remoteForm.rolloutTimeoutSeconds,
    dryRun: remoteForm.dryRun,
    confirmText: remoteForm.confirmText,
    includeDatabases: true,
    includeAllCurrentData: true,
    includeMinio: true,
    applyDatabaseSnapshot: true,
    applyMinioSnapshot: true
  }
}

async function runPreflight() {
  if (!remoteForm.kubeconfigText.trim()) {
    ElMessage.warning('请先粘贴 kubeconfig')
    return
  }
  preflighting.value = true
  try {
    const res = await preflightRemoteMigration(buildRemoteDeployPayload())
    preflightResult.value = res.data || null
    if (preflightResult.value?.passed) {
      ElMessage.success('部署前预检通过')
    } else {
      ElMessage.warning('部署前预检未通过，请查看结果')
    }
  } finally {
    preflighting.value = false
  }
}

async function submitRemoteMigration() {
  if (!remoteForm.kubeconfigText.trim()) {
    ElMessage.warning('请先粘贴 kubeconfig')
    return
  }
  if (remoteForm.confirmText.trim() !== 'REMOTE_DEPLOY') {
    ElMessage.warning('请输入 REMOTE_DEPLOY 确认远程迁移部署')
    return
  }
  remoteDeploying.value = true
  try {
    const res = await createRemoteMigration(buildRemoteDeployPayload())
    remoteForm.kubeconfigText = ''
    remoteForm.confirmText = 'REMOTE_DEPLOY'
    preflightResult.value = null
    ElMessage.success(res.data?.id ? '远程迁移任务已提交' : '远程迁移请求已提交')
    await loadRecords()
  } finally {
    remoteDeploying.value = false
  }
}

async function submitLinuxSshMigration() {
  if (!sshForm.sshHost.trim()) {
    ElMessage.warning('请填写 SSH 服务器地址')
    return
  }
  if (!sshForm.sshUsername.trim() || !sshForm.sshPassword) {
    ElMessage.warning('请填写 SSH 用户名和密码')
    return
  }
  if (sshForm.confirmText.trim() !== 'SSH_DEPLOY') {
    ElMessage.warning('请输入 SSH_DEPLOY 确认 Linux SSH 部署')
    return
  }
  linuxDeploying.value = true
  try {
    const res = await createLinuxSshMigration({
      ...form,
      ...sshForm,
      remoteDeploy: true,
      deployMode: 'linux-ssh',
      imageArtifactMode: 'registry',
      exportRuntimeArtifacts: false,
      cleanupRuntimeArtifacts: true,
      confirmText: sshForm.confirmText
    })
    ElMessage.success(res.data?.id ? 'Linux SSH 部署任务已提交' : 'Linux SSH 部署请求已提交')
    await loadRecords()
  } finally {
    linuxDeploying.value = false
  }
}

async function openMigrationDetail(row) {
  if (!row?.id) return
  migrationHistoryDrawer.value = true
  detailLoading.value = true
  remoteDiagnosis.value = null
  try {
    const res = await getRemoteMigrationDetail(row.id)
    selectedMigrationDetail.value = res.data || null
  } finally {
    detailLoading.value = false
  }
}

async function showKubeconfig() {
  const id = selectedMigrationDetail.value?.record?.id
  if (!id) return
  await ElMessageBox.confirm('将显示远程集群 kubeconfig 明文，并写入运维审计。确认继续？', '查看 kubeconfig', {
    confirmButtonText: '查看明文',
    cancelButtonText: '取消',
    type: 'warning'
  })
  const res = await getRemoteMigrationKubeconfig(id)
  kubeconfigPlaintext.value = res.data?.kubeconfig || ''
  kubeconfigDialog.value = true
}

async function runRemoteDiagnosis(row) {
  if (!row?.id) return
  diagnosing.value = row.id
  try {
    const res = await diagnoseRemoteMigration(row.id)
    remoteDiagnosis.value = res.data || null
    if (selectedMigrationDetail.value?.record?.id === row.id) {
      await openMigrationDetail(row)
      remoteDiagnosis.value = res.data || null
    }
    ElMessage.success('远程集群诊断已完成')
  } finally {
    diagnosing.value = null
  }
}

async function repairRemoteMigrationAction(row, action) {
  if (!row?.id || !action) return
  const label = repairActions.find((item) => item.value === action)?.label || action
  await ElMessageBox.confirm(`将执行远程修复：${label}。确认继续？`, '远程迁移修复', {
    confirmButtonText: '执行修复',
    cancelButtonText: '取消',
    type: 'warning'
  })
  repairingAction.value = `${row.id}:${action}`
  try {
    const res = await repairRemoteMigration(row.id, {
      repairAction: action,
      confirmText: 'REPAIR_REMOTE_DEPLOY',
      reason: label
    })
    ElMessage.success(res.data?.success ? '远程修复已执行' : '远程修复执行完成，请查看输出')
    await loadRecords()
    if (selectedMigrationDetail.value?.record?.id === row.id) {
      await openMigrationDetail(row)
    }
  } finally {
    repairingAction.value = ''
  }
}

async function refreshRunningRecords() {
  const res = await listRemoteMigrations()
  setRecords(res.data || [])
}

function syncPolling() {
  if (runningRecords.value.length && !pollTimer) {
    pollTimer = window.setInterval(refreshRunningRecords, 3000)
  }
  if (!runningRecords.value.length && pollTimer) {
    window.clearInterval(pollTimer)
    pollTimer = null
  }
}

onMounted(loadRecords)
onBeforeUnmount(() => {
  if (pollTimer) window.clearInterval(pollTimer)
})
</script>

<style scoped>
.remote-migration-page { display: grid; gap: 16px; padding: 20px; }
.page-header,
.remote-deploy-panel,
.linux-ssh-panel,
.script-panel,
.progress-panel,
.table-panel { border: 1px solid #e4e7ed; border-radius: 8px; background: #fff; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; padding: 18px 20px; }
.page-header h2 { margin: 0 0 6px; }
.page-header p { margin: 0; color: #667085; }
.header-actions { display: flex; gap: 10px; flex-wrap: wrap; }
.remote-deploy-panel,
.linux-ssh-panel { display: grid; gap: 12px; padding: 16px 18px; }
.remote-deploy-panel { border-color: #b7d4ff; }
.linux-ssh-panel { border-color: #b7ebc6; }
.panel-copy { display: grid; gap: 4px; color: #667085; }
.panel-copy strong { color: #101828; }
.kubeconfig-input { width: 100%; }
.remote-deploy-options,
.ssh-options { display: flex; flex-wrap: wrap; gap: 12px; align-items: center; }
.remote-deploy-options .el-input,
.ssh-options .el-input { width: 240px; }
.remote-deploy-options .registry-input { width: 340px; }
.remote-deploy-options .el-input-number,
.ssh-options .el-input-number { width: 170px; }
.cluster-select { width: 140px; }
.exposure-select { width: 160px; }
.preflight-result { display: flex; flex-wrap: wrap; gap: 8px 14px; align-items: center; padding: 10px 12px; border: 1px solid #d0d5dd; border-radius: 8px; background: #f9fafb; color: #344054; }
.preflight-result small { flex-basis: 100%; color: #667085; }
.preflight-result .warning-text { color: #b54708; }
.preflight-result .error-text { color: #b42318; }
.script-panel { padding: 14px 18px; }
.script-panel div { display: grid; gap: 6px; color: #667085; }
.script-panel strong { color: #101828; }
.progress-panel { display: grid; gap: 12px; padding: 14px 18px; }
.progress-title { display: flex; align-items: center; gap: 12px; }
.progress-title span,
.muted { color: #667085; font-size: 13px; }
.progress-item { display: grid; grid-template-columns: 320px minmax(0, 1fr); gap: 12px; align-items: center; }
.progress-copy { display: grid; gap: 4px; min-width: 0; }
.progress-copy span,
.progress-copy small,
.image-progress small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.table-panel { padding: 8px 12px 14px; }
.image-progress { display: grid; gap: 4px; }
.remote-migration-log { margin: 0; padding: 12px; border-radius: 8px; background: #111827; color: #d1fae5; white-space: pre-wrap; }
.history-detail { display: grid; gap: 14px; }
.history-actions { display: flex; flex-wrap: wrap; gap: 10px; }
.migration-history-drawer .remote-migration-log { max-height: 260px; overflow: auto; }
.kubeconfig-plaintext { margin-top: 12px; }
@media (max-width: 800px) {
  .page-header { flex-direction: column; }
  .header-actions .el-button,
  .remote-deploy-options .el-input,
  .remote-deploy-options .el-button,
  .ssh-options .el-input,
  .ssh-options .el-button,
  .cluster-select,
  .exposure-select { width: 100%; }
  .progress-item { grid-template-columns: minmax(0, 1fr); }
}
</style>
