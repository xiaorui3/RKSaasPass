<template>
  <div class="admin-page release-center-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <div>
            <h2>发版中心</h2>
            <p>围绕当前运行镜像、Jenkins 构建和 K8s 更新的统一入口。</p>
          </div>
          <div class="header-actions">
            <el-radio-group v-model="publishMode" size="small">
              <el-radio-button label="dry-run">预演</el-radio-button>
              <el-radio-button label="real-run">真实发布</el-radio-button>
            </el-radio-group>
            <el-radio-group v-model="deployMode" size="small">
              <el-radio-button label="dry-run">Deploy Dry Run</el-radio-button>
              <el-radio-button label="real-run">Deploy Real Run</el-radio-button>
            </el-radio-group>
            <el-radio-group v-model="runtimeMode" size="small">
              <el-radio-button label="k3s">K3s</el-radio-button>
              <el-radio-button label="docker-compose">Docker Compose</el-radio-button>
            </el-radio-group>
            <el-button :loading="loading" @click="fetchReleaseServices">刷新</el-button>
            <el-select v-model="namespaceFilter" clearable placeholder="命名空间" style="width: 220px" @change="fetchReleaseServices">
              <el-option label="shetuanguanlixitong" value="shetuanguanlixitong" />
            </el-select>
          </div>
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="当前版本记录来源于 ops_release_version；发布当前镜像会生成系统镜像版本；更新当前微服务会对当前 K8s workload 执行 kubectl set image。"
      />

      <el-table :data="services" v-loading="loading" stripe size="small" empty-text="暂无发版服务" class="service-table">
        <el-table-column prop="serviceCode" label="服务" min-width="140" fixed="left" />
        <el-table-column prop="displayName" label="名称" min-width="140" />
        <el-table-column prop="namespaceName" label="Namespace" min-width="150" />
        <el-table-column prop="workloadType" label="类型" width="120" />
        <el-table-column prop="workloadName" label="工作负载" min-width="180" show-overflow-tooltip />
        <el-table-column prop="containerName" label="容器" min-width="140" />
        <el-table-column prop="currentImage" label="当前镜像" min-width="280" show-overflow-tooltip />
        <el-table-column prop="currentVersion" label="版本" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'enabled' ? 'success' : 'warning'">
              {{ row.status === 'enabled' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="420" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button size="small" type="primary" @click="handlePublishCurrent(row)">发布当前镜像到阿里云</el-button>
              <el-button size="small" @click="handleTriggerBuild(row)">触发 Jenkins 构建</el-button>
              <el-button size="small" type="success" @click="handleDeployCurrent(row)">更新当前微服务</el-button>
              <el-button size="small" type="warning" @click="handleRollback(row)">回滚上一版本</el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="service-expand">
              <div class="expand-head">
                <strong>selectedVersions</strong>
                <el-button size="small" text @click="loadVersions(row.serviceCode)">刷新版本</el-button>
              </div>
              <el-table :data="versionsByService[row.serviceCode] || []" stripe size="small" empty-text="暂无版本记录" class="version-table">
                <el-table-column prop="versionTag" label="版本号" width="160" />
                <el-table-column prop="sourceType" label="来源" width="120" />
                <el-table-column prop="sourceImage" label="源镜像" min-width="260" show-overflow-tooltip />
                <el-table-column prop="registryImage" label="目标镜像" min-width="280" show-overflow-tooltip />
                <el-table-column prop="status" label="状态" width="100" />
                <el-table-column prop="createTime" label="时间" min-width="160" />
                <el-table-column label="操作" width="240">
                  <template #default="{ row: version }">
                    <el-button size="small" type="success" @click="handleDeployVersion(row, version)">更新当前微服务</el-button>
                    <el-button size="small" type="warning" @click="handleRollbackVersion(row, version)">回滚上一版本</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <section class="update-panel" data-update-manifest-endpoint="/updates/manifest" data-update-apply-endpoint="/updates/apply">
        <div class="update-head">
          <div>
            <h3>Update Center</h3>
            <p>Manifest, SQL, Nacos, MinIO, image rollout and Jenkins update dry-run.</p>
          </div>
          <div class="update-actions">
            <el-select v-model="updateChannel" size="small" style="width: 120px">
              <el-option label="stable" value="stable" />
              <el-option label="beta" value="beta" />
              <el-option label="local-dev" value="local-dev" />
            </el-select>
            <el-input v-model="updateVersion" size="small" placeholder="update version" style="width: 180px" />
            <el-button size="small" type="primary" :loading="updateManifestLoading" @click="handleBuildUpdateManifest">
              Build Manifest
            </el-button>
            <el-button size="small" type="success" :loading="updateApplyLoading" :disabled="!updateManifestText" @click="handleApplyUpdateManifest">
              Dry-run Apply
            </el-button>
          </div>
        </div>
        <el-descriptions :column="4" border size="small" class="update-summary">
          <el-descriptions-item label="Version">{{ updateManifestInfo.updateVersion || '-' }}</el-descriptions-item>
          <el-descriptions-item label="Channel">{{ updateManifestInfo.channel || updateChannel }}</el-descriptions-item>
          <el-descriptions-item label="Runtime">{{ updateManifestInfo.runtimeMode || runtimeMode }}</el-descriptions-item>
          <el-descriptions-item label="Actions">{{ updateManifestInfo.actionCount ?? 0 }}</el-descriptions-item>
        </el-descriptions>
        <el-input
          v-model="updateManifestText"
          type="textarea"
          :rows="10"
          placeholder="Update manifest JSON"
          class="update-manifest-textarea"
        />
      </section>

      <el-card shadow="never" class="task-card">
        <template #header>
          <div class="task-header">
            <span>taskLog</span>
            <el-button size="small" :disabled="!activeTaskId" :loading="taskLoading" @click="refreshTask">刷新任务</el-button>
          </div>
        </template>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="任务">{{ taskInfo.taskType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="服务">{{ taskInfo.serviceCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ taskInfo.status || '-' }}</el-descriptions-item>
          <el-descriptions-item label="阶段">{{ taskInfo.currentStep || '-' }}</el-descriptions-item>
          <el-descriptions-item label="进度">{{ taskInfo.progress ?? 0 }}%</el-descriptions-item>
          <el-descriptions-item label="任务ID">{{ taskInfo.taskId || '-' }}</el-descriptions-item>
        </el-descriptions>
        <pre class="task-log">{{ taskInfo.logs || '暂无任务日志' }}</pre>
      </el-card>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  applyReleaseUpdateManifest,
  buildReleaseUpdateManifest,
  deployReleaseServiceVersion,
  getReleaseCenterTask,
  listReleaseCenterServices,
  listReleaseCenterVersions,
  publishReleaseCurrentImage,
  triggerReleaseServiceBuild
} from '@/api/admin-ops'

const loading = ref(false)
const taskLoading = ref(false)
const services = ref([])
const namespaceFilter = ref('shetuanguanlixitong')
const publishMode = ref('dry-run')
const deployMode = ref('dry-run')
const runtimeMode = ref('k3s')
const updateChannel = ref('stable')
const updateVersion = ref('')
const updateManifestText = ref('')
const updateManifestLoading = ref(false)
const updateApplyLoading = ref(false)
const versionsByService = reactive({})
const activeTaskId = ref(null)
const taskInfo = reactive({
  taskId: null,
  taskType: '',
  serviceCode: '',
  status: '',
  currentStep: '',
  progress: 0,
  logs: ''
})
const updateManifestInfo = reactive({
  updateVersion: '',
  channel: '',
  runtimeMode: '',
  actionCount: 0
})

const selectedVersions = computed(() => versionsByService)

async function fetchReleaseServices() {
  loading.value = true
  try {
    const res = await listReleaseCenterServices({ namespace: namespaceFilter.value })
    services.value = res.code === 200 ? (res.data || []) : []
    for (const service of services.value) {
      await loadVersions(service.serviceCode)
    }
  } catch (error) {
    ElMessage.error(error.message || '获取发版服务失败')
  } finally {
    loading.value = false
  }
}

async function loadVersions(serviceCode) {
  try {
    const res = await listReleaseCenterVersions(serviceCode)
    versionsByService[serviceCode] = res.code === 200 ? (res.data || []) : []
  } catch (error) {
    versionsByService[serviceCode] = []
  }
}

function hydrateTask(response) {
  const data = response?.data && (response.data.taskId || response.data.id || response.data.status)
    ? response.data
    : response || {}
  taskInfo.taskId = data.taskId || data.id || null
  taskInfo.taskType = data.taskType || ''
  taskInfo.serviceCode = data.serviceCode || ''
  taskInfo.status = data.status || ''
  taskInfo.currentStep = data.currentStep || ''
  taskInfo.progress = data.progress || 0
  taskInfo.logs = data.logs || ''
  activeTaskId.value = taskInfo.taskId
}

async function handlePublishCurrent(row) {
  const dryRun = publishMode.value === 'dry-run'
  if (!dryRun) {
    try {
      await ElMessageBox.confirm(
        `确认真实发布 ${row.serviceCode} 当前镜像到镜像仓库？`,
        '真实发布',
        {
          confirmButtonText: '真实发布',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
    } catch (error) {
      return
    }
  }
  try {
    const res = await publishReleaseCurrentImage(row.serviceCode, {
      currentImage: row.currentImage,
      versionTag: defaultVersionTag(),
      dryRun: publishMode.value === 'dry-run'
    })
    if (res.code === 200) {
      hydrateTask(res.data)
      ElMessage.success(dryRun ? '已生成当前镜像发布预演任务' : '已发布当前镜像')
      await loadVersions(row.serviceCode)
    }
  } catch (error) {
    ElMessage.error(error.message || '发布当前镜像失败')
  }
}

async function handleTriggerBuild(row) {
  try {
    const res = await triggerReleaseServiceBuild(row.serviceCode, {
      jobName: 'rk-web-cloud-master-new',
      gitSource: 'local',
      imageMode: 'registry'
    })
    if (res.code === 200) {
      hydrateTask(res.data)
      ElMessage.success('已触发 Jenkins 构建')
    }
  } catch (error) {
    ElMessage.error(error.message || '触发 Jenkins 失败')
  }
}

async function handleDeployVersion(row, version) {
  const dryRun = deployMode.value === 'dry-run'
  if (!dryRun) {
    try {
      await ElMessageBox.confirm(
        `确认使用 ${runtimeMode.value} 真实更新 ${row.serviceCode} 到 ${version.versionTag || version.id}？`,
        '真实部署',
        {
          confirmButtonText: '真实部署',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
    } catch (error) {
      return
    }
  }
  try {
    const res = await deployReleaseServiceVersion(row.serviceCode, {
      versionId: version.id,
      runtimeMode: runtimeMode.value,
      previousImage: row.currentImage,
      namespace: namespaceFilter.value || row.namespaceName,
      composeProjectName: 'rk-web',
      composeServiceName: defaultComposeServiceName(row),
      composeFile: 'docker-compose.yaml',
      confirmText: row.serviceCode,
      dryRun: deployMode.value === 'dry-run'
    })
    if (res.code === 200) {
      hydrateTask(res.data)
      ElMessage.success(dryRun ? '已生成更新预演任务' : '已更新当前微服务')
    }
  } catch (error) {
    ElMessage.error(error.message || '更新当前微服务失败')
  }
}

async function handleDeployCurrent(row) {
  const version = (versionsByService[row.serviceCode] || [])[0]
  if (!version) {
    ElMessage.warning('请先生成或选择一个版本')
    return
  }
  await handleDeployVersion(row, version)
}

async function handleRollback(row) {
  const versions = versionsByService[row.serviceCode] || []
  if (versions.length < 2) {
    ElMessage.warning('暂无上一版本可回滚')
    return
  }
  await handleDeployVersion(row, versions[1])
}

async function handleRollbackVersion(row, version) {
  await handleDeployVersion(row, version)
}

async function handleBuildUpdateManifest() {
  updateManifestLoading.value = true
  try {
    const res = await buildReleaseUpdateManifest({
      updateVersion: updateVersion.value || defaultVersionTag(),
      channel: updateChannel.value,
      runtimeMode: runtimeMode.value
    })
    if (res.code === 200) {
      const data = res.data || {}
      updateManifestInfo.updateVersion = data.updateVersion || ''
      updateManifestInfo.channel = data.channel || ''
      updateManifestInfo.runtimeMode = data.runtimeMode || ''
      updateManifestInfo.actionCount = data.actionCount || 0
      updateManifestText.value = data.manifestJson || ''
      ElMessage.success('Update manifest built')
    }
  } catch (error) {
    ElMessage.error(error.message || 'Build update manifest failed')
  } finally {
    updateManifestLoading.value = false
  }
}

async function handleApplyUpdateManifest() {
  if (!updateManifestText.value) {
    ElMessage.warning('Update manifest is required')
    return
  }
  updateApplyLoading.value = true
  try {
    const res = await applyReleaseUpdateManifest({
      manifestJson: updateManifestText.value,
      runtimeMode: runtimeMode.value,
      dryRun: true,
      confirmText: 'APPLY UPDATE'
    })
    if (res.code === 200) {
      hydrateTask(res.data)
      ElMessage.success('Update dry-run task created')
    }
  } catch (error) {
    ElMessage.error(error.message || 'Apply update manifest failed')
  } finally {
    updateApplyLoading.value = false
  }
}

function defaultVersionTag() {
  const now = new Date()
  const pad = (value) => String(value).padStart(2, '0')
  return [
    now.getFullYear(),
    pad(now.getMonth() + 1),
    pad(now.getDate())
  ].join('') + '.' + [pad(now.getHours()), pad(now.getMinutes()), pad(now.getSeconds())].join('')
}

function defaultComposeServiceName(row) {
  return row.serviceCode === 'frontend' ? 'rk-web-frontend' : row.serviceCode
}

async function refreshTask() {
  if (!activeTaskId.value) {
    return
  }
  taskLoading.value = true
  try {
    const res = await getReleaseCenterTask(activeTaskId.value)
    if (res.code === 200) {
      hydrateTask(res)
    }
  } catch (error) {
    ElMessage.error(error.message || '刷新任务失败')
  } finally {
    taskLoading.value = false
  }
}

onMounted(() => {
  fetchReleaseServices()
})
</script>

<style scoped>
.card-header,
.task-header,
.expand-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.header-actions,
.row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.service-table {
  margin-top: 12px;
}

.service-expand {
  padding: 8px 0 12px;
}

.version-table {
  margin-top: 8px;
}

.update-panel {
  margin-top: 16px;
  padding: 14px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
}

.update-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.update-head h3 {
  margin: 0 0 4px;
  font-size: 16px;
}

.update-head p {
  margin: 0;
  color: #606266;
  font-size: 13px;
}

.update-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.update-summary,
.update-manifest-textarea {
  margin-top: 12px;
}

.task-card {
  margin-top: 16px;
}

.task-log {
  margin: 12px 0 0;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 280px;
  overflow: auto;
}
</style>
