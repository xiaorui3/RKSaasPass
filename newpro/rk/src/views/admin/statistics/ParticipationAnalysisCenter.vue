<template>
  <div class="participation-analysis-center">
    <section class="page-toolbar">
      <div>
        <h1>参与度分析中心</h1>
        <p>按租户聚合浏览转化、报名转化、评论互动、关注与复访、活跃成员，并展示数据来源诊断。</p>
      </div>
      <div class="toolbar-actions">
        <span>更新时间：{{ participationAnalysisCenter.updatedAt || '-' }}</span>
        <el-button type="primary" :loading="loading" @click="refreshParticipationAnalysis">刷新</el-button>
      </div>
    </section>

    <section class="filter-panel">
      <div class="filter-row">
        <label>分析维度</label>
        <el-segmented v-model="filters.dimension" :options="dimensionOptions" @change="refreshParticipationAnalysis" />
      </div>
      <div class="filter-row compact">
        <label>时间范围</label>
        <el-select v-model="filters.timeRange" size="small" @change="refreshParticipationAnalysis">
          <el-option label="近 7 天" value="7D" />
          <el-option label="近 30 天" value="30D" />
          <el-option label="近 90 天" value="90D" />
          <el-option label="全部" value="ALL" />
        </el-select>
      </div>
    </section>

    <section class="summary-grid">
      <div v-for="item in metricCards" :key="item.metric" class="summary-card" :class="item.tone">
        <span>{{ item.label }}</span>
        <strong>{{ item.displayValue }}</strong>
        <small>{{ item.hint }}</small>
      </div>
    </section>

    <section class="conversion-grid">
      <article class="conversion-panel">
        <header>
          <strong>浏览转化</strong>
          <el-tag>{{ summary.viewCount || 0 }} 次</el-tag>
        </header>
        <div class="metric-list">
          <div>
            <span>总浏览</span>
            <strong>{{ summary.viewCount || 0 }}</strong>
          </div>
          <div>
            <span>报名转化</span>
            <strong>{{ summary.registrationConversionRate || 0 }}%</strong>
          </div>
          <div>
            <span>互动率</span>
            <strong>{{ summary.interactionRate || 0 }}%</strong>
          </div>
        </div>
      </article>

      <article class="conversion-panel">
        <header>
          <strong>报名转化</strong>
          <el-tag type="success">{{ totalRegistrationCount }} 人次</el-tag>
        </header>
        <div class="metric-list">
          <div>
            <span>活动报名</span>
            <strong>{{ summary.activityRegistrationCount || 0 }}</strong>
          </div>
          <div>
            <span>比赛报名</span>
            <strong>{{ summary.competitionRegistrationCount || 0 }}</strong>
          </div>
          <div>
            <span>活跃成员</span>
            <strong>{{ summary.activeMemberCount || 0 }}</strong>
          </div>
        </div>
      </article>

      <article class="conversion-panel">
        <header>
          <strong>评论互动</strong>
          <el-tag type="warning">{{ summary.commentCount || 0 }} 条</el-tag>
        </header>
        <div class="metric-list">
          <div>
            <span>评论数</span>
            <strong>{{ summary.commentCount || 0 }}</strong>
          </div>
          <div>
            <span>关注数</span>
            <strong>{{ summary.followCount || 0 }}</strong>
          </div>
          <div>
            <span>复访数</span>
            <strong>{{ summary.revisitCount || 0 }}</strong>
          </div>
        </div>
      </article>
    </section>

    <el-card class="analysis-card">
      <template #header>
        <div class="card-header">
          <strong>活跃趋势</strong>
          <span>{{ trendRows.length }} 个维度</span>
        </div>
      </template>
      <el-table v-loading="loading" :data="trendRows" size="small" empty-text="暂无参与度趋势">
        <el-table-column prop="label" label="维度" width="120" />
        <el-table-column prop="viewCount" label="浏览" width="110" />
        <el-table-column prop="registrationCount" label="报名" width="110" />
        <el-table-column prop="commentCount" label="评论互动" width="120" />
        <el-table-column prop="followCount" label="关注" width="110" />
        <el-table-column prop="revisitCount" label="复访" width="110" />
        <el-table-column label="参与结构" min-width="220">
          <template #default="{ row }">
            <div class="bar-track">
              <span :style="{ width: trendBarWidth(row) }"></span>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="analysis-card">
      <template #header>
        <div class="card-header">
          <strong>来源诊断</strong>
          <span>{{ sourceRows.length }} 条</span>
        </div>
      </template>
      <el-table :data="sourceRows.slice(0, 24)" size="small" empty-text="暂无来源诊断">
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
import { ElMessage } from 'element-plus'
import { getParticipationAnalysisCenter } from '@/api/data'

const loading = ref(false)
const filters = reactive({
  dimension: 'ALL',
  timeRange: '30D'
})
const participationAnalysisCenter = reactive({
  tenantId: null,
  tenantScope: '',
  updatedAt: '',
  cache: {},
  filters: {},
  summary: {},
  trend: {},
  engagementTrend: {},
  metrics: [],
  trendRows: [],
  sources: []
})

const dimensionOptions = [
  { label: '全部', value: 'ALL' },
  { label: '活动', value: 'ACTIVITY' },
  { label: '比赛', value: 'COMPETITION' },
  { label: '内容', value: 'CONTENT' },
  { label: '成员', value: 'MEMBER' }
]

const unwrapResponse = (res) => {
  if (res?.code && res.code !== 200) {
    throw new Error(res.msg || '参与度分析中心加载失败')
  }
  return res?.data || res || {}
}

const assignParticipationAnalysisCenter = (data) => {
  Object.assign(participationAnalysisCenter, {
    tenantId: data.tenantId ?? null,
    tenantScope: data.tenantScope || '',
    updatedAt: data.updatedAt || '',
    cache: data.cache || {},
    filters: data.filters || {},
    summary: data.summary || {},
    trend: data.trend || {},
    engagementTrend: data.engagementTrend || data.trend || {},
    metrics: Array.isArray(data.metrics) ? data.metrics : [],
    trendRows: Array.isArray(data.trendRows) ? data.trendRows : [],
    sources: Array.isArray(data.sources) ? data.sources : []
  })
}

async function refreshParticipationAnalysis() {
  loading.value = true
  try {
    const res = await getParticipationAnalysisCenter({
      dimension: filters.dimension,
      timeRange: filters.timeRange
    })
    assignParticipationAnalysisCenter(unwrapResponse(res))
  } catch (error) {
    ElMessage.error(error.message || '参与度分析中心加载失败')
  } finally {
    loading.value = false
  }
}

const numberValue = (value) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

const summary = computed(() => participationAnalysisCenter.summary || {})
const trendRows = computed(() => participationAnalysisCenter.trendRows || [])
const sourceRows = computed(() => participationAnalysisCenter.sources || [])
const totalRegistrationCount = computed(() => numberValue(summary.value.activityRegistrationCount) + numberValue(summary.value.competitionRegistrationCount))

const metricCards = computed(() => [
  { metric: 'viewCount', label: '浏览转化', displayValue: numberValue(summary.value.viewCount), hint: `租户 ${participationAnalysisCenter.tenantScope || '-'}`, tone: 'primary' },
  { metric: 'registrationConversionRate', label: '报名转化', displayValue: `${numberValue(summary.value.registrationConversionRate)}%`, hint: `${totalRegistrationCount.value} 人次报名`, tone: 'success' },
  { metric: 'commentCount', label: '评论互动', displayValue: numberValue(summary.value.commentCount), hint: '评论互动沉淀', tone: 'warning' },
  { metric: 'followCount', label: '关注与复访', displayValue: numberValue(summary.value.followCount) + numberValue(summary.value.revisitCount), hint: `${numberValue(summary.value.revisitCount)} 次复访`, tone: 'info' },
  { metric: 'activeMemberCount', label: '活跃成员', displayValue: numberValue(summary.value.activeMemberCount), hint: '成员参与底盘', tone: 'primary' }
])

function trendBarWidth(row) {
  const max = Math.max(1, ...trendRows.value.map((item) => (
    numberValue(item.viewCount) +
    numberValue(item.registrationCount) +
    numberValue(item.commentCount) +
    numberValue(item.followCount) +
    numberValue(item.revisitCount)
  )))
  const current = numberValue(row.viewCount) + numberValue(row.registrationCount) + numberValue(row.commentCount) + numberValue(row.followCount) + numberValue(row.revisitCount)
  return `${Math.max(8, Math.round((current / max) * 100))}%`
}

function sourceTagType(status) {
  const value = String(status || '').toLowerCase()
  if (value === 'ok') return 'success'
  if (value.includes('missing')) return 'info'
  return 'warning'
}

onMounted(refreshParticipationAnalysis)
</script>

<style scoped>
.participation-analysis-center {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-toolbar,
.filter-panel,
.analysis-card {
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
.conversion-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 14px;
}

.summary-card,
.conversion-panel {
  background: #fff;
  border: 1px solid #edf0f7;
  border-radius: 8px;
}

.summary-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-height: 104px;
  padding: 16px 18px;
  border-left: 4px solid #4677c8;
}

.summary-card span,
.summary-card small,
.conversion-panel span {
  color: #68758f;
}

.summary-card strong {
  font-size: 28px;
  color: #1f2a44;
}

.summary-card.success {
  border-left-color: #2f9e44;
}

.summary-card.warning {
  border-left-color: #f59f00;
}

.summary-card.info {
  border-left-color: #4c6ef5;
}

.conversion-panel {
  padding: 16px;
}

.conversion-panel header,
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.metric-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.metric-list div {
  padding: 12px;
  background: #f7f9fc;
  border-radius: 8px;
}

.metric-list span,
.metric-list strong {
  display: block;
}

.metric-list strong {
  margin-top: 4px;
  color: #1f2a44;
  font-size: 20px;
}

.bar-track {
  width: 100%;
  height: 8px;
  overflow: hidden;
  background: #eef3fb;
  border-radius: 999px;
}

.bar-track span {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #4677c8, #2f9e44);
  border-radius: inherit;
}

@media (max-width: 960px) {
  .page-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .toolbar-actions {
    width: 100%;
    justify-content: space-between;
  }

  .metric-list {
    grid-template-columns: 1fr;
  }
}
</style>
