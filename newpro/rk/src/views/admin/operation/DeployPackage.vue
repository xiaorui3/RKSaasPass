<template>
  <div class="deploy-package-page">
    <section class="page-header">
      <div>
        <h2>一键打包部署</h2>
        <p>生成可下载部署包，支持 Docker Compose、k3s/k8s、数据库快照、MinIO 导出脚本、镜像 tar 或阿里云镜像仓库拉取模式。</p>
      </div>
      <div class="header-actions">
        <el-button :loading="loading" @click="loadRecords">刷新</el-button>
        <el-button type="primary" :loading="creating" @click="createPackage">
          {{ form.deliveryMode === 'direct-download' ? '生成并下载' : '开始打包' }}
        </el-button>
      </div>
    </section>

    <section class="options-panel">
      <el-alert
        class="package-boundary"
        type="info"
        :closable="false"
        show-icon
        title="选择“阿里云镜像仓库”时，后台会实际导出镜像并推送到镜像仓库；部署包会生成拉取镜像部署包，不在服务器长期保留镜像 tar；上传/拉取进度会写入数据库，刷新页面仍能看到当前步骤。"
      />

      <div class="option-row wide">
        <span class="option-label">打包模式</span>
        <el-radio-group v-model="form.packageMode">
          <el-radio-button label="offline-full">离线完整包</el-radio-button>
          <el-radio-button label="bootstrap">引导包</el-radio-button>
        </el-radio-group>
      </div>

      <div class="option-row wide">
        <span class="option-label">交付方式</span>
        <el-radio-group v-model="form.deliveryMode">
          <el-radio-button label="direct-download">直接下载到客户端</el-radio-button>
          <el-radio-button label="store-minio">保留在 MinIO</el-radio-button>
        </el-radio-group>
      </div>

      <div class="option-row wide">
        <span class="option-label">镜像模式</span>
        <el-radio-group v-model="form.imageArtifactMode">
          <el-radio-button label="offline-tar">离线镜像 tar</el-radio-button>
          <el-radio-button label="registry">阿里云镜像仓库</el-radio-button>
          <el-radio-button label="both">同时生成</el-radio-button>
        </el-radio-group>
      </div>

      <el-checkbox v-model="form.includeAllCurrentData">数据库一键导出当前所有数据</el-checkbox>
      <el-checkbox v-model="form.includeDatabases">包含数据库导出和导入脚本</el-checkbox>
      <el-checkbox v-model="form.includeImages">包含镜像导出和导入脚本</el-checkbox>
      <el-checkbox v-model="form.includeK8sManifests">包含 k8s/k3s 清单目录</el-checkbox>
      <el-checkbox v-model="form.includeMinio">包含 MinIO 对象导出和导入脚本</el-checkbox>
      <el-checkbox v-model="form.includeDockerCompose">包含 Docker Compose 部署文件</el-checkbox>
      <el-checkbox v-model="form.includeSource">包含源码和服务说明</el-checkbox>
      <el-checkbox v-model="form.exportRuntimeArtifacts">后台直接导出镜像 tar</el-checkbox>
      <el-checkbox v-model="form.cleanupRuntimeArtifacts" :disabled="!form.exportRuntimeArtifacts">推送后清理源节点临时 tar</el-checkbox>
      <el-checkbox v-model="form.deleteAfterDownload">下载后清理服务器临时包</el-checkbox>

      <el-select v-model="form.targetClusterType" class="cluster-select" placeholder="目标集群">
        <el-option label="ACK" value="ack" />
        <el-option label="标准 Kubernetes" value="k8s" />
        <el-option label="k3s" value="k3s" />
        <el-option label="自定义" value="custom" />
      </el-select>
      <el-input v-model="form.storageClassName" class="storage-class-input" placeholder="StorageClass，可留空" clearable />
      <el-select v-model="form.externalExposureType" class="exposure-select" placeholder="公网入口">
        <el-option label="无公网入口" value="none" />
        <el-option label="NodePort" value="nodePort" />
        <el-option label="LoadBalancer" value="loadBalancer" />
        <el-option label="Ingress" value="ingress" />
      </el-select>
      <el-input v-model="form.registryPrefix" class="registry-input" placeholder="镜像仓库前缀" clearable />
      <el-input v-model="form.registryServer" class="registry-server-input" placeholder="镜像仓库域名" clearable />
      <el-input v-model="form.targetNamespace" class="namespace-input" placeholder="命名空间" clearable />
      <el-input v-model="form.targetDomain" class="domain-input" placeholder="域名" clearable />
      <el-input v-model="form.note" class="note-input" placeholder="备注" clearable />
    </section>

    <section class="script-panel">
      <div>
        <strong>部署包流程</strong>
        <span>
          离线 tar 模式：在源集群执行 <code>scripts/export-all-current-data.sh</code> 补齐 <code>images/*.tar</code>、<code>databases/*.sql.gz</code> 和 MinIO 数据；目标服务器执行 <code>install.sh</code>。
          阿里云仓库模式：先执行 <code>scripts/push-images-to-registry.sh</code>，成功后本地 tar 会删除；目标服务器执行 <code>scripts/pull-images.sh</code> 或直接 <code>docker compose up -d</code> 拉取镜像。
        </span>
      </div>
    </section>

    <section v-if="runningRecords.length" class="progress-panel">
      <div class="progress-title">
        <strong>打包进度</strong>
        <span>运行中的任务会每 3 秒刷新；退出页面后再进入也能从数据库恢复进度。</span>
      </div>
      <div v-for="item in runningRecords" :key="item.id" class="progress-item">
        <div class="progress-copy">
          <span>{{ item.packageName }}</span>
          <small>{{ item.currentStep || latestLogLine(item.logs) || '等待后端写入打包日志' }}</small>
          <small v-if="item.currentImage">当前镜像：{{ item.currentImage }}</small>
          <small v-if="item.totalImages">镜像进度：{{ item.uploadedImages || 0 }}/{{ item.totalImages }}，{{ item.uploadPercent || 0 }}%</small>
        </div>
        <el-progress :percentage="item.progress || 0" />
      </div>
    </section>

    <section class="table-panel">
      <el-table :data="records" v-loading="loading" stripe empty-text="暂无打包记录">
        <el-table-column prop="packageName" label="包名" min-width="210" />
        <el-table-column label="模式" width="140">
          <template #default="{ row }">
            <el-tag v-if="row.registryPullPackage" type="success">拉取镜像部署包</el-tag>
            <el-tag v-else>{{ row.packageMode === 'bootstrap' ? '引导包' : '离线完整包' }}</el-tag>
          </template>
        </el-table-column>
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
        <el-table-column label="大小" width="120">
          <template #default="{ row }">
            <span>{{ deployPackageSizeText(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="下载状态" width="180">
          <template #default="{ row }">
            <span v-if="row.downloaded">已下载 {{ row.downloadedAt || '' }}</span>
            <span v-else>未下载</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" :disabled="isDownloadDisabled(row)" @click="download(row)">
              {{ downloadActionLabel(row) }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="package-detail">
              <el-alert
                type="info"
                :closable="false"
                show-icon
                title="ZIP 内置 manifest、install.sh、offline-install.sh、export/import 脚本、docker-compose.yml、k8s/rk-web-stack.yaml、images/*.tar、databases/*.sql.gz、minio 目录。"
              />
              <pre class="package-log">{{ row.logs || '暂无日志' }}</pre>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createDeployPackage,
  downloadDeployPackage,
  listDeployPackages
} from '@/api/admin-ops'

const loading = ref(false)
const creating = ref(false)
const records = ref([])
const runningRecords = ref([])
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

const form = reactive({
  packageMode: 'offline-full',
  deliveryMode: 'direct-download',
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

watch(
  () => form.packageMode,
  (value) => {
    form.selfContained = value !== 'bootstrap'
  },
  { immediate: true }
)

watch(
  () => form.deliveryMode,
  (value) => {
    if (value === 'direct-download') {
      form.deleteAfterDownload = true
    }
  }
)

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

function isRetainedDownloadedPackage(row) {
  return Boolean(row?.downloaded && row?.downloadable && !row?.deleteAfterDownload)
}

function deployPackageSizeText(row) {
  return row?.fileSize || '-'
}

function isDownloadDisabled(row) {
  return !row?.downloadable || Boolean(row?.downloaded && row?.deleteAfterDownload)
}

function downloadActionLabel(row) {
  if (row?.downloaded && row?.deleteAfterDownload) return '已清理'
  if (isRetainedDownloadedPackage(row)) return '重新下载'
  if (row?.downloaded && !row?.downloadable) return '已下载'
  return '下载'
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

function setRecords(data) {
  records.value = data || []
  runningRecords.value = records.value.filter((item) => item.status === 'RUNNING')
  syncPolling()
}

async function loadRecords() {
  loading.value = true
  try {
    const res = await listDeployPackages()
    setRecords(res.data || [])
  } finally {
    loading.value = false
  }
}

async function createPackage() {
  creating.value = true
  try {
    const res = await createDeployPackage({ ...form })
    const created = res.data
    ElMessage.success(form.deliveryMode === 'direct-download' ? '打包任务已提交，完成后会自动下载' : '打包任务已提交')
    await loadRecords()
    if (created?.id && form.deliveryMode === 'direct-download') {
      await waitUntilDownloadable(created.id)
      const latest = records.value.find((item) => item.id === created.id) || created
      await download(latest)
      await loadRecords()
    }
  } finally {
    creating.value = false
  }
}

async function waitUntilDownloadable(id) {
  for (let i = 0; i < 120; i += 1) {
    await refreshRunningRecords()
    const row = records.value.find((item) => item.id === id)
    if (row?.status === 'FAILED') {
      throw new Error(latestLogLine(row.logs) || '打包失败')
    }
    if (row?.downloadable) {
      return
    }
    await new Promise((resolve) => window.setTimeout(resolve, 3000))
  }
  throw new Error('打包超时，请在记录列表中查看日志')
}

async function refreshRunningRecords() {
  const res = await listDeployPackages()
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

async function download(row) {
  const blob = await downloadDeployPackage(row.id)
  if (blob?.type?.includes('application/json')) {
    const text = await blob.text()
    let message = '部署包下载失败'
    try {
      const payload = JSON.parse(text)
      message = payload.msg || payload.message || message
    } catch {
      message = text || message
    }
    ElMessage.error(message)
    throw new Error(message)
  }
  if (!blob || blob.size === 0) {
    ElMessage.error('部署包下载失败：服务端返回空文件')
    throw new Error('部署包下载失败')
  }
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = row.fileName || `${row.packageName}.zip`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(link.href)
  if (row.deleteAfterDownload) {
    ElMessage.success('已下载到客户端本地，服务器临时包已清理')
  }
}

onMounted(loadRecords)
onBeforeUnmount(() => {
  if (pollTimer) window.clearInterval(pollTimer)
})
</script>

<style scoped>
.deploy-package-page { display: grid; gap: 16px; padding: 20px; }
.page-header,
.options-panel,
.script-panel,
.progress-panel,
.table-panel { border: 1px solid #e4e7ed; border-radius: 8px; background: #fff; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; padding: 18px 20px; }
.page-header h2 { margin: 0 0 6px; }
.page-header p { margin: 0; color: #667085; }
.header-actions { display: flex; gap: 10px; flex-wrap: wrap; }
.options-panel { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; padding: 14px 18px; }
.package-boundary,
.wide { flex-basis: 100%; }
.option-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.option-label { min-width: 72px; color: #344054; font-weight: 600; }
.options-panel .el-input { width: 260px; }
.cluster-select { width: 120px; }
.storage-class-input { width: 220px; }
.exposure-select { width: 160px; }
.namespace-input { width: 220px; }
.domain-input { width: 220px; }
.options-panel .registry-input { width: 340px; }
.options-panel .registry-server-input { width: 260px; }
.note-input { width: 300px; }
.script-panel { padding: 14px 18px; }
.script-panel div { display: grid; gap: 6px; color: #667085; }
.script-panel strong { color: #101828; }
.script-panel code { padding: 2px 6px; border-radius: 4px; background: #f2f4f7; color: #101828; }
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
.package-detail { display: grid; gap: 10px; }
.package-log { margin: 0; padding: 12px; border-radius: 8px; background: #111827; color: #d1fae5; white-space: pre-wrap; }
@media (max-width: 800px) {
  .page-header { flex-direction: column; }
  .options-panel .el-input,
  .header-actions .el-button,
  .cluster-select,
  .storage-class-input,
  .exposure-select,
  .registry-input,
  .registry-server-input,
  .namespace-input,
  .domain-input,
  .note-input { width: 100%; }
  .progress-item { grid-template-columns: minmax(0, 1fr); }
}
</style>
