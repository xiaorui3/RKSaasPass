<template>
  <div class="content-publish-calendar">
    <section class="page-toolbar">
      <div>
        <h1>内容发布日历</h1>
        <p>按租户聚合新闻、公告、作品、活动和比赛的发布排期，统一查看审核状态、延期风险和来源诊断。</p>
      </div>
      <div class="toolbar-actions">
        <span>更新时间：{{ contentPublishCalendar.updatedAt || '-' }}</span>
        <el-button type="primary" :loading="loading" @click="refreshContentCalendar">刷新</el-button>
      </div>
    </section>

    <section class="filter-panel">
      <div class="filter-row">
        <label>时间范围</label>
        <el-segmented v-model="filters.timeRange" :options="timeRangeOptions" @change="refreshContentCalendar" />
      </div>
      <div class="filter-row compact">
        <label>内容类型</label>
        <el-select v-model="filters.contentType" size="small" @change="refreshContentCalendar">
          <el-option label="全部" value="ALL" />
          <el-option label="新闻" value="NEWS" />
          <el-option label="公告" value="NOTICE" />
          <el-option label="作品" value="WORKS" />
          <el-option label="活动" value="ACTIVITY" />
          <el-option label="比赛" value="COMPETITION" />
        </el-select>
      </div>
      <div class="filter-row compact">
        <label>状态</label>
        <el-select v-model="filters.status" size="small" @change="refreshContentCalendar">
          <el-option label="全部" value="ALL" />
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="待审核" value="PENDING" />
          <el-option label="计划发布" value="SCHEDULED" />
          <el-option label="延期风险" value="OVERDUE" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
      </div>
    </section>

    <section class="summary-grid">
      <div v-for="item in summaryCards" :key="item.metric" class="summary-card" :class="item.tone">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.hint }}</small>
      </div>
    </section>

    <section class="calendar-grid">
      <article class="calendar-panel">
        <header>
          <strong>发布排期</strong>
          <el-tag>{{ calendarRows.length }} 天</el-tag>
        </header>
        <div class="day-list" v-loading="loading">
          <div v-for="row in calendarRows.slice(0, 14)" :key="row.date" class="day-row">
            <div>
              <strong>{{ row.date }}</strong>
              <span>新闻 {{ row.newsCount || 0 }} / 活动 {{ row.activityCount || 0 }} / 比赛 {{ row.competitionCount || 0 }}</span>
            </div>
            <div class="day-tags">
              <el-tag size="small" type="success">已发布 {{ row.publishedCount || 0 }}</el-tag>
              <el-tag size="small" type="warning">待审核 {{ row.pendingCount || 0 }}</el-tag>
              <el-tag size="small">计划 {{ row.scheduledCount || 0 }}</el-tag>
            </div>
          </div>
          <el-empty v-if="!loading && !calendarRows.length" description="暂无发布排期" />
        </div>
      </article>

      <article class="calendar-panel">
        <header>
          <strong>内容结构</strong>
          <el-tag type="info">{{ summary.totalCount || 0 }} 条</el-tag>
        </header>
        <div class="type-metrics">
          <div><span>新闻</span><strong>{{ summary.newsCount || 0 }}</strong></div>
          <div><span>公告</span><strong>{{ summary.noticeCount || 0 }}</strong></div>
          <div><span>作品</span><strong>{{ summary.worksCount || 0 }}</strong></div>
          <div><span>活动</span><strong>{{ summary.activityCount || 0 }}</strong></div>
          <div><span>比赛</span><strong>{{ summary.competitionCount || 0 }}</strong></div>
        </div>
      </article>
    </section>

    <el-card class="content-card">
      <template #header>
        <div class="card-header">
          <strong>发布明细</strong>
          <span>{{ timelineRows.length }} 条</span>
        </div>
      </template>
      <el-table v-loading="loading" :data="timelineRows" size="small" empty-text="暂无发布明细">
        <el-table-column prop="publishAt" label="发布时间" min-width="170" />
        <el-table-column prop="contentTypeName" label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small">{{ row.contentTypeName || row.contentType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="260" show-overflow-tooltip />
        <el-table-column prop="statusName" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ row.statusName || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button link type="primary" @click="openTarget(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="content-card">
      <template #header>
        <div class="card-header">
          <strong>延期风险</strong>
          <span>{{ riskRows.length }} 条</span>
        </div>
      </template>
      <el-table :data="riskRows" size="small" empty-text="暂无延期风险">
        <el-table-column prop="title" label="风险" min-width="180" show-overflow-tooltip />
        <el-table-column prop="riskLevel" label="等级" width="100">
          <template #default="{ row }">
            <el-tag :type="riskTagType(row.riskLevel)" size="small">{{ riskText(row.riskLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="130" />
        <el-table-column prop="suggestion" label="建议" min-width="260" show-overflow-tooltip />
        <el-table-column prop="detectedAt" label="检测时间" min-width="180" />
      </el-table>
    </el-card>

    <el-card class="content-card">
      <template #header>
        <div class="card-header">
          <strong>来源诊断</strong>
          <span>{{ sourceRows.length }} 条</span>
        </div>
      </template>
      <el-table :data="sourceRows.slice(0, 28)" size="small" empty-text="暂无来源诊断">
        <el-table-column prop="metric" label="指标" min-width="190" />
        <el-table-column prop="tableName" label="来源" min-width="220" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="140">
          <template #default="{ row }">
            <el-tag :type="sourceTagType(row.status)" size="small">{{ row.status || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" min-width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getContentPublishCalendar } from '@/api/data'

const router = useRouter()
const loading = ref(false)
const filters = reactive({
  timeRange: '30D',
  contentType: 'ALL',
  status: 'ALL'
})

const contentPublishCalendar = reactive({
  tenantId: null,
  tenantScope: '',
  updatedAt: '',
  cache: {},
  filters: {},
  summary: {},
  summaryCards: [],
  calendarRows: [],
  timelineRows: [],
  riskRows: [],
  sources: []
})

const timeRangeOptions = [
  { label: '近 7 天', value: '7D' },
  { label: '近 30 天', value: '30D' },
  { label: '近 90 天', value: '90D' },
  { label: '近 180 天', value: '180D' },
  { label: '全部', value: 'ALL' }
]

const unwrapResponse = (res) => {
  if (res?.code && res.code !== 200) {
    throw new Error(res.msg || '内容发布日历加载失败')
  }
  return res?.data || res || {}
}

const assignContentPublishCalendar = (data) => {
  Object.assign(contentPublishCalendar, {
    tenantId: data.tenantId ?? null,
    tenantScope: data.tenantScope || '',
    updatedAt: data.updatedAt || '',
    cache: data.cache || {},
    filters: data.filters || {},
    summary: data.summary || {},
    summaryCards: Array.isArray(data.summaryCards) ? data.summaryCards : [],
    calendarRows: Array.isArray(data.calendarRows) ? data.calendarRows : [],
    timelineRows: Array.isArray(data.timelineRows) ? data.timelineRows : [],
    riskRows: Array.isArray(data.riskRows) ? data.riskRows : [],
    sources: Array.isArray(data.sources) ? data.sources : []
  })
}

async function refreshContentCalendar() {
  loading.value = true
  try {
    const res = await getContentPublishCalendar({
      timeRange: filters.timeRange,
      contentType: filters.contentType,
      status: filters.status
    })
    assignContentPublishCalendar(unwrapResponse(res))
  } catch (error) {
    ElMessage.error(error.message || '内容发布日历加载失败')
  } finally {
    loading.value = false
  }
}

const summary = computed(() => contentPublishCalendar.summary || {})
const summaryCards = computed(() => {
  const cards = contentPublishCalendar.summaryCards || []
  if (cards.length) return cards
  return [
    { metric: 'publishedCount', label: '已发布', value: summary.value.publishedCount || 0, hint: '已通过并展示', tone: 'success' },
    { metric: 'pendingCount', label: '待审核', value: summary.value.pendingCount || 0, hint: '等待处理', tone: 'warning' },
    { metric: 'scheduledCount', label: '计划发布', value: summary.value.scheduledCount || 0, hint: '未来排期', tone: 'info' },
    { metric: 'overdueCount', label: '延期风险', value: summary.value.overdueCount || 0, hint: '需要复核', tone: 'danger' }
  ]
})
const calendarRows = computed(() => contentPublishCalendar.calendarRows || [])
const timelineRows = computed(() => contentPublishCalendar.timelineRows || [])
const riskRows = computed(() => contentPublishCalendar.riskRows || [])
const sourceRows = computed(() => contentPublishCalendar.sources || [])

function openTarget(row) {
  if (row?.targetPath) {
    router.push(row.targetPath)
  }
}

function statusTagType(status) {
  if (status === 'PUBLISHED') return 'success'
  if (status === 'PENDING') return 'warning'
  if (status === 'OVERDUE') return 'danger'
  if (status === 'REJECTED') return 'info'
  return ''
}

function riskText(value) {
  if (value === 'HIGH') return '高'
  if (value === 'MEDIUM') return '中'
  return '低'
}

function riskTagType(value) {
  if (value === 'HIGH') return 'danger'
  if (value === 'MEDIUM') return 'warning'
  return 'success'
}

function sourceTagType(status) {
  const value = String(status || '').toLowerCase()
  if (value === 'ok') return 'success'
  if (value.includes('missing')) return 'info'
  return 'warning'
}

onMounted(refreshContentCalendar)
</script>

<style scoped>
.content-publish-calendar {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-toolbar,
.filter-panel,
.content-card {
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
  white-space: nowrap;
}

.filter-row.compact {
  min-width: 180px;
}

.summary-grid,
.calendar-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 14px;
}

.summary-card,
.calendar-panel {
  padding: 16px;
  background: #fff;
  border: 1px solid #edf0f7;
  border-radius: 8px;
}

.summary-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.summary-card span,
.summary-card small {
  color: #68758f;
}

.summary-card strong {
  font-size: 28px;
  color: #1f2a44;
}

.summary-card.primary {
  border-top: 3px solid #2f6bff;
}

.summary-card.success {
  border-top: 3px solid #18a058;
}

.summary-card.warning {
  border-top: 3px solid #d98b00;
}

.summary-card.info {
  border-top: 3px solid #6b7a90;
}

.summary-card.danger {
  border-top: 3px solid #d92d20;
}

.calendar-panel header,
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.day-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 14px;
}

.day-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  background: #f7f9fc;
  border-radius: 8px;
}

.day-row div:first-child {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.day-row span {
  color: #68758f;
}

.day-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: flex-end;
}

.type-metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(110px, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.type-metrics div {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px;
  background: #f7f9fc;
  border-radius: 8px;
}

.type-metrics span {
  color: #68758f;
}

.type-metrics strong {
  font-size: 24px;
  color: #1f2a44;
}

@media (max-width: 768px) {
  .page-toolbar,
  .day-row {
    align-items: stretch;
    flex-direction: column;
  }

  .toolbar-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .filter-panel {
    align-items: stretch;
  }

  .filter-row {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
