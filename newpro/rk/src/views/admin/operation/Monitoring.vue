<template>
  <div class="admin-page monitoring-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <div>
            <h2>监控中心</h2>
            <p>聚合展示当前主机负载、Nacos 服务实例和实时健康状态。</p>
          </div>
          <el-button :loading="loading" @click="fetchOverview">刷新</el-button>
        </div>
      </template>

      <div class="overview-grid">
        <div class="overview-card">
          <span class="label">CPU</span>
          <span class="value">{{ overview.systemStatus.cpu }}%</span>
        </div>
        <div class="overview-card">
          <span class="label">内存</span>
          <span class="value">{{ overview.systemStatus.memory }}%</span>
        </div>
        <div class="overview-card">
          <span class="label">磁盘</span>
          <span class="value">{{ overview.systemStatus.disk }}%</span>
        </div>
        <div class="overview-card">
          <span class="label">网络</span>
          <span class="value">{{ overview.systemStatus.network || '-' }}</span>
        </div>
      </div>

      <el-table
        :data="overview.services"
        v-loading="loading"
        stripe
        row-key="name"
        style="margin-top: 20px"
      >
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="instance-panel">
              <div class="instance-summary">
                <span>健康实例 {{ row.healthyInstances ?? 0 }}/{{ row.instances ?? 0 }}</span>
                <span>QPS {{ row.qps ?? 0 }}</span>
                <span>响应时间 {{ row.responseTime ?? '-' }}</span>
              </div>

              <el-empty
                v-if="!row.instanceDetails || !row.instanceDetails.length"
                description="当前没有实例详情"
              />

              <el-table
                v-else
                :data="row.instanceDetails"
                size="small"
                border
                class="instance-table"
              >
                <el-table-column prop="ip" label="IP" min-width="140" />
                <el-table-column prop="port" label="端口" width="90" />
                <el-table-column label="健康状态" width="110">
                  <template #default="{ row: instance }">
                    <el-tag :type="instance.healthy ? 'success' : 'danger'" size="small">
                      {{ instance.healthy ? '健康' : '异常' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="启用" width="90">
                  <template #default="{ row: instance }">
                    <el-tag :type="instance.enabled ? 'success' : 'info'" size="small">
                      {{ instance.enabled ? '是' : '否' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="weight" label="权重" width="90" />
                <el-table-column prop="clusterName" label="集群" min-width="120" />
                <el-table-column label="日志" width="180" fixed="right">
                  <template #default="{ row: instance }">
                    <el-button
                      text
                      type="primary"
                      :disabled="!instance.podName || !instance.containerName"
                      @click="handleViewLogs(instance)"
                    >
                      查看日志
                    </el-button>
                    <el-button
                      text
                      type="warning"
                      :disabled="!instance.podName || !instance.containerName"
                      @click="handleOpenPodTerminal(instance)"
                    >
                      Terminal
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="name" label="服务" min-width="140" />
        <el-table-column prop="instances" label="实例数" width="90" />
        <el-table-column label="健康实例" width="100">
          <template #default="{ row }">
            {{ row.healthyInstances ?? 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'online' ? 'success' : 'danger'">
              {{ row.status === 'online' ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="responseTime" label="响应时间" width="120" />
        <el-table-column prop="qps" label="QPS" width="100" />
      </el-table>
    </el-card>

    <el-dialog
      v-model="logDialogVisible"
      title="容器日志"
      width="860px"
      class="monitoring-log-dialog"
    >
      <div class="log-target">
        {{ activeLogTarget.namespace }}/{{ activeLogTarget.podName }}/{{ activeLogTarget.containerName }}
      </div>
      <pre v-loading="logLoading" class="log-content">{{ logContent || '暂无日志内容' }}</pre>
      <template #footer>
        <el-button @click="logDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="logLoading" @click="handleViewLogs(activeLogTarget)">刷新日志</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="terminalDialogVisible"
      title="Pod Terminal"
      width="860px"
      class="monitoring-log-dialog"
    >
      <div class="log-target">
        {{ activeTerminalTarget.namespace }}/{{ activeTerminalTarget.podName }}/{{ activeTerminalTarget.containerName }}
      </div>
      <el-input
        v-model="terminalCommand"
        type="textarea"
        :rows="4"
        placeholder="pwd && ls -lah"
      />
      <div class="terminal-toolbar">
        <span>Timeout</span>
        <el-input-number v-model="terminalTimeoutSeconds" :min="3" :max="120" />
        <el-button type="primary" :loading="terminalLoading" @click="handleRunPodTerminal">Run Command</el-button>
      </div>
      <pre v-loading="terminalLoading" class="log-content">{{ terminalOutput || 'No output yet' }}</pre>
      <template #footer>
        <el-button @click="terminalDialogVisible = false">Close</el-button>
        <el-button type="primary" :loading="terminalLoading" @click="handleRunPodTerminal">Run Command</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getMonitoringLogs, getMonitoringOverview, runK8sPodExec } from '@/api/admin-ops'

const loading = ref(false)
const logDialogVisible = ref(false)
const logLoading = ref(false)
const logContent = ref('')
const terminalDialogVisible = ref(false)
const terminalLoading = ref(false)
const terminalCommand = ref('pwd && ls -lah')
const terminalTimeoutSeconds = ref(20)
const terminalOutput = ref('')
const activeLogTarget = reactive({
  namespace: '',
  podName: '',
  containerName: ''
})
const activeTerminalTarget = reactive({
  namespace: '',
  podName: '',
  containerName: ''
})
const overview = reactive({
  systemStatus: {
    cpu: 0,
    memory: 0,
    disk: 0,
    network: '-'
  },
  services: []
})

async function fetchOverview() {
  loading.value = true
  try {
    const res = await getMonitoringOverview()
    if (res.code === 200 && res.data) {
      overview.systemStatus = {
        ...overview.systemStatus,
        ...(res.data.systemStatus || {})
      }
      overview.services = Array.isArray(res.data.services) ? res.data.services : []
      return
    }
    throw new Error(res.msg || '获取监控数据失败')
  } catch (error) {
    ElMessage.error(error.message || '获取监控数据失败')
  } finally {
    loading.value = false
  }
}

async function handleViewLogs(instance) {
  const target = {
    namespace: instance?.namespace || '',
    podName: instance?.podName || '',
    containerName: instance?.containerName || ''
  }
  if (!target.namespace || !target.podName || !target.containerName) {
    ElMessage.warning('当前实例缺少 Pod 日志定位信息')
    return
  }
  Object.assign(activeLogTarget, target)
  logDialogVisible.value = true
  logLoading.value = true
  try {
    const res = await getMonitoringLogs({
      ...target,
      tailLines: 300
    })
    if (res.code === 200) {
      logContent.value = res.data?.logs || res.data?.message || ''
      if (res.data?.message && res.data.message !== '读取成功') {
        ElMessage.warning(res.data.message)
      }
      return
    }
    throw new Error(res.msg || '读取容器日志失败')
  } catch (error) {
    logContent.value = error.message || '读取容器日志失败'
    ElMessage.error(logContent.value)
  } finally {
    logLoading.value = false
  }
}

function resolvePodTarget(instance) {
  return {
    namespace: instance?.namespace || '',
    podName: instance?.podName || '',
    containerName: instance?.containerName || ''
  }
}

function handleOpenPodTerminal(instance) {
  const target = resolvePodTarget(instance)
  if (!target.namespace || !target.podName || !target.containerName) {
    ElMessage.warning('Current instance is missing pod/container metadata')
    return
  }
  Object.assign(activeTerminalTarget, target)
  terminalOutput.value = ''
  terminalDialogVisible.value = true
}

async function handleRunPodTerminal() {
  if (!activeTerminalTarget.namespace || !activeTerminalTarget.podName || !activeTerminalTarget.containerName) {
    ElMessage.warning('Please select a pod/container first')
    return
  }
  if (!terminalCommand.value.trim()) {
    ElMessage.warning('Please enter command')
    return
  }
  terminalLoading.value = true
  try {
    const res = await runK8sPodExec({
      ...activeTerminalTarget,
      command: terminalCommand.value,
      timeoutSeconds: terminalTimeoutSeconds.value,
      reason: 'monitoring pod terminal'
    })
    if (res.code !== 200 || !res.data) {
      throw new Error(res.msg || 'Pod exec failed')
    }
    terminalOutput.value = res.data.output || res.data.message || ''
    if (res.data.success) {
      ElMessage.success(res.data.message || 'Pod command executed')
    } else {
      ElMessage.error(res.data.message || 'Pod command failed')
    }
  } catch (error) {
    terminalOutput.value = error.message || 'Pod exec failed'
    ElMessage.error(terminalOutput.value)
  } finally {
    terminalLoading.value = false
  }
}

onMounted(() => {
  fetchOverview()
})
</script>

<style scoped>
.monitoring-page {
  display: grid;
  gap: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.card-header h2 {
  margin: 0;
  font-size: 20px;
}

.card-header p {
  margin: 8px 0 0;
  color: #909399;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.overview-card {
  border: 1px solid #ebeef5;
  border-radius: 14px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: #fff;
}

.label {
  color: #909399;
}

.value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.instance-panel {
  display: grid;
  gap: 12px;
}

.instance-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  color: #606266;
}

.instance-table {
  width: 100%;
}

.log-target {
  margin-bottom: 12px;
  color: #606266;
  font-size: 13px;
}

.log-content {
  min-height: 360px;
  max-height: 58vh;
  margin: 0;
  padding: 14px;
  overflow: auto;
  border-radius: 12px;
  background: #101827;
  color: #d7e3f4;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.terminal-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 12px 0;
}

@media (max-width: 900px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .card-header {
    flex-direction: column;
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
