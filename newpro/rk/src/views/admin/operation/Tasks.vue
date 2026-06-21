<template>
  <div class="admin-page task-center">
    <el-card class="task-card">
      <template #header>
        <div class="card-header">
          <div>
            <h1>任务中心</h1>
            <p>统一查看 XXL-Job 定时任务与 Jenkins 构建任务</p>
          </div>
          <div class="actions">
            <el-button @click="refreshAll">刷新全部</el-button>
          </div>
        </div>
      </template>

      <section class="task-section">
        <div class="section-header">
          <div>
            <h2>XXL-Job 定时任务</h2>
            <p>查看调度配置、执行状态与最近日志</p>
          </div>
          <div class="section-actions">
            <el-button @click="fetchTaskOverview">刷新</el-button>
            <el-button v-if="taskSourceUrl" type="primary" plain @click="openExternal(taskSourceUrl)">打开控制台</el-button>
          </div>
        </div>

        <el-alert
          v-if="taskSourceLabel"
          type="info"
          :closable="false"
          show-icon
          class="source-alert"
          :title="`当前数据源：${taskSourceLabel}`"
        />

        <el-table :data="tasks" v-loading="tasksLoading" stripe empty-text="暂无定时任务">
          <el-table-column prop="name" label="任务名称" min-width="180" />
          <el-table-column prop="groupName" label="任务分组" min-width="140" />
          <el-table-column prop="scheduleType" label="调度类型" width="110" />
          <el-table-column prop="cron" label="调度表达式" min-width="180">
            <template #default="{ row }">
              <code>{{ row.cron || '-' }}</code>
            </template>
          </el-table-column>
          <el-table-column prop="handler" label="执行器" min-width="170" />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="row.status === 'running' ? 'success' : 'warning'">
                {{ row.status === 'running' ? '运行中' : '已暂停' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="lastExecuteTime" label="上次执行" width="170" />
          <el-table-column prop="nextExecuteTime" label="下次执行" width="170" />
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <el-space>
                <el-button
                  v-if="row.status === 'running'"
                  size="small"
                  type="warning"
                  @click="handlePause(row)"
                >
                  暂停
                </el-button>
                <el-button
                  v-else
                  size="small"
                  type="success"
                  @click="handleResume(row)"
                >
                  恢复
                </el-button>
                <el-button size="small" type="primary" plain @click="handleTrigger(row)">
                  立即执行
                </el-button>
              </el-space>
            </template>
          </el-table-column>
        </el-table>

        <div class="logs-header">
          <h3>最近执行日志</h3>
        </div>
        <el-table :data="logs" v-loading="tasksLoading" stripe empty-text="暂无执行日志">
          <el-table-column prop="taskName" label="任务名称" min-width="180" />
          <el-table-column prop="executeTime" label="执行时间" width="200" />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="taskLogTagType(row.status)">
                {{ taskLogStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="执行信息" min-width="260" show-overflow-tooltip />
        </el-table>
      </section>

      <el-divider />

      <section class="task-section">
        <div class="section-header">
          <div>
            <h2>Jenkins 构建任务</h2>
            <p v-if="canUseJenkins">仅租户 1 可查看与触发中间件部署流水线</p>
            <p v-else>当前租户无 Jenkins 运维权限</p>
          </div>
          <div class="section-actions" v-if="canUseJenkins">
            <el-button @click="fetchJenkinsOverview">刷新</el-button>
            <el-button v-if="jenkinsSourceUrl" type="primary" plain @click="openExternal(jenkinsSourceUrl)">打开 Jenkins</el-button>
          </div>
        </div>

        <el-alert
          v-if="!canUseJenkins"
          type="warning"
          :closable="false"
          show-icon
          title="Jenkins 构建能力当前仅对租户 1 开放。"
        />

        <template v-else>
          <el-alert
            v-if="jenkinsSourceLabel"
            type="info"
            :closable="false"
            show-icon
            class="source-alert"
            :title="`当前数据源：${jenkinsSourceLabel}`"
          />

          <el-table :data="jenkinsJobs" v-loading="jenkinsLoading" stripe empty-text="暂无 Jenkins 任务">
            <el-table-column prop="name" label="任务名称" min-width="180" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="jenkinsTagType(row.status)">
                  {{ jenkinsStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="lastBuildNumber" label="最新构建" width="110">
              <template #default="{ row }">
                {{ row.lastBuildNumber ?? '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="lastCompletedBuildNumber" label="最近完成" width="110">
              <template #default="{ row }">
                {{ row.lastCompletedBuildNumber ?? '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="url" label="任务地址" min-width="260" show-overflow-tooltip />
            <el-table-column label="操作" width="260" fixed="right">
              <template #default="{ row }">
                <el-space>
                  <el-button size="small" type="primary" @click="handleTriggerJenkins(row)">
                    触发构建
                  </el-button>
                  <el-button size="small" plain @click="openExternal(row.lastBuildUrl || row.url)">
                    查看构建
                  </el-button>
                </el-space>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </section>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getJenkinsOverview,
  getTaskOverview,
  pauseTask,
  resumeTask,
  triggerJenkinsJob,
  triggerTask
} from '@/api/admin-ops'
import { useUserStore } from '@/stores/user'
import { canAccessJenkinsOps, normalizeJenkinsJobs, resolveOpsConsoleUrl } from '@/utils/adminOpsTaskCenter'

const userStore = useUserStore()

const tasksLoading = ref(false)
const jenkinsLoading = ref(false)
const taskSourceUrl = ref('')
const taskSourceLabel = ref('')
const jenkinsSourceUrl = ref('')
const jenkinsSourceLabel = ref('')
const tasks = ref([])
const logs = ref([])
const jenkinsJobs = ref([])

const canUseJenkins = computed(() => canAccessJenkinsOps(userStore.tenantId))

function taskLogTagType(status) {
  if (status === 'success') return 'success'
  if (status === 'running') return 'info'
  return 'danger'
}

function taskLogStatusLabel(status) {
  if (status === 'success') return '成功'
  if (status === 'running') return '执行中'
  return '失败'
}

function jenkinsTagType(status) {
  if (status === 'success') return 'success'
  if (status === 'running') return 'warning'
  if (status === 'failed') return 'danger'
  if (status === 'disabled') return 'info'
  return ''
}

function jenkinsStatusLabel(status) {
  if (status === 'success') return '成功'
  if (status === 'running') return '构建中'
  if (status === 'failed') return '失败'
  if (status === 'disabled') return '已禁用'
  return '未知'
}

function resolveCurrentOrigin() {
  if (typeof window === 'undefined' || !window.location?.origin) {
    return ''
  }
  return window.location.origin
}

function assignExternalSource(targetUrlRef, targetLabelRef, rawUrl, hiddenText) {
  const resolvedUrl = resolveOpsConsoleUrl(rawUrl, { currentOrigin: resolveCurrentOrigin() })
  targetUrlRef.value = resolvedUrl
  targetLabelRef.value = resolvedUrl || (rawUrl ? hiddenText : '')
}

async function fetchTaskOverview() {
  tasksLoading.value = true
  try {
    const res = await getTaskOverview()
    assignExternalSource(
      taskSourceUrl,
      taskSourceLabel,
      res.data?.sourceUrl || '',
      '通过后端代理同步（内网控制台地址已隐藏）'
    )
    tasks.value = res.data?.tasks || []
    logs.value = res.data?.logs || []
  } catch (error) {
    ElMessage.error(error.message || '获取定时任务失败')
  } finally {
    tasksLoading.value = false
  }
}

async function fetchJenkinsOverview() {
  if (!canUseJenkins.value) {
    jenkinsJobs.value = []
    jenkinsSourceUrl.value = ''
    jenkinsSourceLabel.value = ''
    return
  }
  jenkinsLoading.value = true
  try {
    const res = await getJenkinsOverview()
    assignExternalSource(
      jenkinsSourceUrl,
      jenkinsSourceLabel,
      res.data?.sourceUrl || '',
      '通过后端代理同步（内网 Jenkins 地址已隐藏）'
    )
    jenkinsJobs.value = normalizeJenkinsJobs(res.data?.jobs || [])
  } catch (error) {
    ElMessage.error(error.message || '获取 Jenkins 任务失败')
  } finally {
    jenkinsLoading.value = false
  }
}

async function refreshAll() {
  await Promise.all([
    fetchTaskOverview(),
    fetchJenkinsOverview()
  ])
}

async function handlePause(row) {
  const res = await pauseTask(row.id)
  if (res.code === 200 && res.data) {
    ElMessage.success(`已暂停任务：${row.name}`)
    await fetchTaskOverview()
    return
  }
  ElMessage.error(res.msg || '暂停任务失败')
}

async function handleResume(row) {
  const res = await resumeTask(row.id)
  if (res.code === 200 && res.data) {
    ElMessage.success(`已恢复任务：${row.name}`)
    await fetchTaskOverview()
    return
  }
  ElMessage.error(res.msg || '恢复任务失败')
}

async function handleTrigger(row) {
  const res = await triggerTask(row.id)
  if (res.code === 200 && res.data) {
    ElMessage.success(`已触发任务：${row.name}`)
    await fetchTaskOverview()
    return
  }
  ElMessage.error(res.msg || '执行任务失败')
}

async function handleTriggerJenkins(row) {
  const res = await triggerJenkinsJob(row.name)
  if (res.code === 200 && res.data) {
    ElMessage.success(`已触发 Jenkins 任务：${row.name}`)
    await fetchJenkinsOverview()
    return
  }
  ElMessage.error(res.msg || '触发 Jenkins 任务失败')
}

function openExternal(url) {
  if (!url) {
    ElMessage.warning('当前没有可打开的地址')
    return
  }
  window.open(url, '_blank')
}

onMounted(() => {
  refreshAll()
})
</script>

<style scoped>
.task-card {
  min-height: 100%;
}

.card-header,
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.card-header h1,
.section-header h2,
.logs-header h3 {
  margin: 0;
}

.card-header p,
.section-header p {
  margin: 8px 0 0;
  color: #909399;
}

.actions,
.section-actions {
  display: flex;
  gap: 12px;
}

.task-section + .task-section {
  margin-top: 8px;
}

.source-alert {
  margin: 16px 0;
}

.logs-header {
  margin: 20px 0 12px;
}

code {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 4px;
}

@media (max-width: 768px) {
  .card-header,
  .section-header {
    flex-direction: column;
  }

  .actions,
  .section-actions {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>
