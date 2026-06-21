<template>
  <div class="approval-task-center">
    <section class="page-toolbar">
      <div>
        <h1>统一审批任务中心</h1>
        <p>汇总入驻、入社、活动、比赛、新闻、财务和通知待审事项，按租户隔离展示 SLA 风险。</p>
      </div>
      <div class="toolbar-actions">
        <span>更新时间：{{ approvalTaskCenter.updatedAt || '-' }}</span>
        <el-button type="primary" :loading="loading" @click="refreshApprovalTasks">刷新</el-button>
      </div>
    </section>

    <section class="filter-panel">
      <div class="filter-row">
        <label>来源类型</label>
        <el-segmented v-model="filters.taskType" :options="taskTypeOptions" @change="refreshApprovalTasks" />
      </div>
      <div class="filter-row">
        <label>SLA 状态</label>
        <el-segmented v-model="filters.slaStatus" :options="slaStatusOptions" @change="refreshApprovalTasks" />
      </div>
      <div class="filter-row compact">
        <label>状态</label>
        <el-select v-model="filters.status" size="small" @change="refreshApprovalTasks">
          <el-option label="全部" value="ALL" />
          <el-option label="待审核" value="PENDING" />
          <el-option label="来源缺失" value="SOURCE_MISSING" />
        </el-select>
      </div>
    </section>

    <section class="summary-grid">
      <div v-for="item in summaryCards" :key="item.label" class="summary-card" :class="item.tone">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.hint }}</small>
      </div>
    </section>

    <el-card class="task-card">
      <template #header>
        <div class="card-header">
          <strong>审批任务列表</strong>
          <span>{{ taskRows.length }} 条</span>
        </div>
      </template>
      <el-table v-loading="loading" :data="taskRows" size="small" empty-text="暂无审批任务">
        <el-table-column prop="taskTypeLabel" label="来源类型" width="120" />
        <el-table-column prop="title" label="事项" min-width="220" show-overflow-tooltip />
        <el-table-column prop="currentNode" label="当前节点" min-width="130" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="slaStatus" label="SLA" width="110">
          <template #default="{ row }">
            <el-tag :type="slaTagType(row.slaStatus)" size="small">{{ slaText(row.slaStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="waitingHours" label="等待时长" width="110">
          <template #default="{ row }">{{ Number(row.waitingHours || 0) }} 小时</template>
        </el-table-column>
        <el-table-column prop="submitterName" label="提交人" min-width="120" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="提交时间" min-width="160" show-overflow-tooltip />
        <el-table-column label="审核入口" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goSource(row.sourcePath)">打开</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="source-card">
      <template #header>
        <div class="card-header">
          <strong>来源诊断</strong>
          <span>{{ sources.length }} 条</span>
        </div>
      </template>
      <el-table :data="sources.slice(0, 20)" size="small" empty-text="暂无来源诊断">
        <el-table-column prop="metric" label="指标" min-width="160" />
        <el-table-column prop="tableName" label="来源" min-width="220" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" min-width="140" />
        <el-table-column prop="updatedAt" label="更新时间" min-width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getApprovalTaskCenter } from '@/api/data'

const router = useRouter()
const loading = ref(false)
const filters = reactive({
  tenantId: '',
  taskType: 'ALL',
  slaStatus: 'ALL',
  status: 'ALL'
})
const approvalTaskCenter = reactive({
  tenantId: null,
  tenantScope: '',
  updatedAt: '',
  cache: {},
  filters: {},
  summary: {},
  tasks: [],
  sources: []
})

const taskTypeOptions = [
  { label: '全部', value: 'ALL' },
  { label: '入驻', value: 'ADMISSION' },
  { label: '入社', value: 'JOIN' },
  { label: '活动', value: 'ACTIVITY' },
  { label: '比赛', value: 'COMPETITION' },
  { label: '新闻', value: 'CONTENT' },
  { label: '财务', value: 'FINANCE' },
  { label: '通知', value: 'NOTICE' }
]

const slaStatusOptions = [
  { label: '全部', value: 'ALL' },
  { label: '正常', value: 'NORMAL' },
  { label: '临近', value: 'DUE_SOON' },
  { label: '超时', value: 'OVERDUE' }
]

const unwrapResponse = (res) => {
  if (res?.code && res.code !== 200) {
    throw new Error(res.msg || '统一审批任务中心加载失败')
  }
  return res?.data || res || {}
}

const assignApprovalTaskCenter = (data) => {
  Object.assign(approvalTaskCenter, {
    tenantId: data.tenantId ?? null,
    tenantScope: data.tenantScope || '',
    updatedAt: data.updatedAt || '',
    cache: data.cache || {},
    filters: data.filters || {},
    summary: data.summary || {},
    tasks: Array.isArray(data.tasks) ? data.tasks : [],
    sources: Array.isArray(data.sources) ? data.sources : []
  })
}

async function refreshApprovalTasks() {
  loading.value = true
  try {
    const params = {
      taskType: filters.taskType,
      slaStatus: filters.slaStatus,
      status: filters.status
    }
    if (filters.tenantId) {
      params.tenantId = filters.tenantId
    }
    const res = await getApprovalTaskCenter(params)
    assignApprovalTaskCenter(unwrapResponse(res))
  } catch (error) {
    ElMessage.error(error.message || '统一审批任务中心加载失败')
  } finally {
    loading.value = false
  }
}

const numberValue = (value) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

const summary = computed(() => approvalTaskCenter.summary || {})
const taskRows = computed(() => approvalTaskCenter.tasks || [])
const sources = computed(() => approvalTaskCenter.sources || [])

const summaryCards = computed(() => [
  { label: '待审总数', value: numberValue(summary.value.totalPendingCount), hint: `租户 ${approvalTaskCenter.tenantScope || '-'}`, tone: 'primary' },
  { label: 'SLA 风险', value: numberValue(summary.value.overdueCount) + numberValue(summary.value.dueSoonCount), hint: `超时 ${numberValue(summary.value.overdueCount)} 项`, tone: 'warning' },
  { label: '超时任务', value: numberValue(summary.value.overdueCount), hint: '需要优先处理', tone: numberValue(summary.value.overdueCount) ? 'danger' : 'success' },
  { label: '通知待审', value: numberValue(summary.value.noticePendingCount), hint: '公告通知审核', tone: 'info' }
])

function statusText(status) {
  if (status === 'SOURCE_MISSING') return '来源缺失'
  if (status === 'MISSING_COLUMNS') return '字段缺失'
  if (status === 'FALLBACK') return '降级'
  return '待审核'
}

function statusTagType(status) {
  if (status === 'SOURCE_MISSING' || status === 'MISSING_COLUMNS') return 'info'
  if (status === 'FALLBACK') return 'warning'
  return 'primary'
}

function slaText(status) {
  if (status === 'OVERDUE') return '超时'
  if (status === 'DUE_SOON') return '临近'
  return '正常'
}

function slaTagType(status) {
  if (status === 'OVERDUE') return 'danger'
  if (status === 'DUE_SOON') return 'warning'
  return 'success'
}

function goSource(sourcePath) {
  if (!sourcePath) {
    ElMessage.warning('暂无可跳转的审核入口')
    return
  }
  router.push(sourcePath)
}

onMounted(refreshApprovalTasks)
</script>

<style scoped>
.approval-task-center {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-toolbar,
.filter-panel,
.task-card,
.source-card {
  border-radius: 8px;
}

.page-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 22px;
  background: #fff;
  border: 1px solid #edf0f7;
}

.page-toolbar h1 {
  margin: 0;
  font-size: 22px;
  color: #1f2a44;
}

.page-toolbar p {
  margin: 6px 0 0;
  color: #68758f;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #68758f;
  white-space: nowrap;
}

.filter-panel {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px 24px;
  padding: 16px 18px;
  background: #fff;
  border: 1px solid #edf0f7;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-row label {
  color: #4d5b73;
  font-weight: 600;
}

.filter-row.compact {
  min-width: 180px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.summary-card {
  padding: 18px;
  background: #fff;
  border: 1px solid #edf0f7;
  border-radius: 8px;
}

.summary-card span,
.summary-card small {
  display: block;
  color: #68758f;
}

.summary-card strong {
  display: block;
  margin: 8px 0 4px;
  font-size: 28px;
  color: #1f2a44;
}

.summary-card.warning strong {
  color: #b7791f;
}

.summary-card.danger strong {
  color: #c53030;
}

.summary-card.success strong {
  color: #2f855a;
}

.summary-card.info strong {
  color: #2b6cb0;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

@media (max-width: 1200px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .page-toolbar,
  .toolbar-actions,
  .filter-panel {
    align-items: stretch;
    flex-direction: column;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
