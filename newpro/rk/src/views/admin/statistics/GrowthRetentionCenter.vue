<template>
  <div class="growth-retention-center">
    <section class="page-toolbar">
      <div>
        <h1>租户增长与留存看板</h1>
        <p>按租户聚合成员增长、活跃留存、沉默成员、志愿时长和第二课堂积分，缓存预热并展示来源诊断。</p>
      </div>
      <div class="toolbar-actions">
        <span>更新时间：{{ growthRetentionCenter.updatedAt || '-' }}</span>
        <el-button type="primary" :loading="loading" @click="refreshGrowthRetention">刷新</el-button>
      </div>
    </section>

    <section class="filter-panel">
      <div class="filter-row">
        <label>时间范围</label>
        <el-segmented v-model="filters.timeRange" :options="timeRangeOptions" @change="refreshGrowthRetention" />
      </div>
      <div class="filter-row compact">
        <label>风险等级</label>
        <el-select v-model="filters.riskLevel" size="small" @change="refreshGrowthRetention">
          <el-option label="全部" value="ALL" />
          <el-option label="高" value="HIGH" />
          <el-option label="中" value="MEDIUM" />
          <el-option label="低" value="LOW" />
        </el-select>
      </div>
    </section>

    <section class="summary-grid">
      <div v-for="item in summaryCards" :key="item.metric" class="summary-card" :class="item.tone">
        <span>{{ item.label }}</span>
        <strong>{{ item.displayValue }}</strong>
        <small>{{ item.hint }}</small>
      </div>
    </section>

    <section class="retention-grid">
      <article class="retention-panel">
        <header>
          <strong>留存结构</strong>
          <el-tag type="success">{{ summary.retentionRate || 0 }}%</el-tag>
        </header>
        <div class="metric-list">
          <div>
            <span>新增成员</span>
            <strong>{{ summary.newMemberCount || 0 }}</strong>
          </div>
          <div>
            <span>活跃成员</span>
            <strong>{{ summary.activeMemberCount || 0 }}</strong>
          </div>
          <div>
            <span>沉默成员</span>
            <strong>{{ summary.dormantMemberCount || 0 }}</strong>
          </div>
        </div>
      </article>

      <article class="retention-panel">
        <header>
          <strong>成长贡献</strong>
          <el-tag>{{ summary.activeClubCount || 0 }} 个活跃社团</el-tag>
        </header>
        <div class="metric-list">
          <div>
            <span>志愿时长</span>
            <strong>{{ numberValue(summary.volunteerHours).toFixed(1) }}</strong>
          </div>
          <div>
            <span>积分总额</span>
            <strong>{{ numberValue(summary.creditTotal).toFixed(1) }}</strong>
          </div>
          <div>
            <span>积分记录</span>
            <strong>{{ summary.creditRecordCount || 0 }}</strong>
          </div>
        </div>
      </article>

      <article class="retention-panel">
        <header>
          <strong>月度趋势</strong>
          <el-tag type="warning">{{ trendRows.length }} 个月</el-tag>
        </header>
        <div class="metric-list">
          <div>
            <span>月新增</span>
            <strong>{{ monthlyTrend.totalNewMemberCount || 0 }}</strong>
          </div>
          <div>
            <span>月活跃</span>
            <strong>{{ monthlyTrend.totalActiveMemberCount || 0 }}</strong>
          </div>
          <div>
            <span>月积分</span>
            <strong>{{ numberValue(monthlyTrend.totalCreditTotal).toFixed(1) }}</strong>
          </div>
        </div>
      </article>
    </section>

    <el-card class="growth-card">
      <template #header>
        <div class="card-header">
          <strong>月度趋势</strong>
          <span>{{ trendRows.length }} 条</span>
        </div>
      </template>
      <el-table v-loading="loading" :data="trendRows" size="small" empty-text="暂无月度趋势">
        <el-table-column prop="month" label="月份" width="120" />
        <el-table-column prop="newMemberCount" label="新增成员" width="110" />
        <el-table-column prop="activeMemberCount" label="活跃成员" width="110" />
        <el-table-column prop="dormantMemberCount" label="沉默成员" width="110" />
        <el-table-column prop="volunteerHours" label="志愿时长" width="120">
          <template #default="{ row }">{{ numberValue(row.volunteerHours).toFixed(1) }}</template>
        </el-table-column>
        <el-table-column prop="creditTotal" label="积分总额" width="120">
          <template #default="{ row }">{{ numberValue(row.creditTotal).toFixed(1) }}</template>
        </el-table-column>
        <el-table-column label="增长结构" min-width="220">
          <template #default="{ row }">
            <div class="bar-track">
              <span :style="{ width: trendBarWidth(row) }"></span>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="growth-card">
      <template #header>
        <div class="card-header">
          <strong>留存风险</strong>
          <span>{{ riskRows.length }} 条</span>
        </div>
      </template>
      <el-table :data="riskRows" size="small" empty-text="暂无留存风险">
        <el-table-column prop="title" label="风险" min-width="180" show-overflow-tooltip />
        <el-table-column prop="riskLevel" label="等级" width="100">
          <template #default="{ row }">
            <el-tag :type="riskTagType(row.riskLevel)" size="small">{{ riskText(row.riskLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="130" />
        <el-table-column prop="source" label="来源" min-width="200" show-overflow-tooltip />
        <el-table-column prop="suggestion" label="建议" min-width="260" show-overflow-tooltip />
        <el-table-column prop="detectedAt" label="检测时间" min-width="180" show-overflow-tooltip />
      </el-table>
    </el-card>

    <el-card class="growth-card">
      <template #header>
        <div class="card-header">
          <strong>来源诊断</strong>
          <span>{{ sourceRows.length }} 条</span>
        </div>
      </template>
      <el-table :data="sourceRows.slice(0, 28)" size="small" empty-text="暂无来源诊断">
        <el-table-column prop="metric" label="指标" min-width="200" />
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
import { getGrowthRetentionCenter } from '@/api/data'

const loading = ref(false)
const filters = reactive({
  timeRange: '90D',
  riskLevel: 'ALL'
})
const growthRetentionCenter = reactive({
  tenantId: null,
  tenantScope: '',
  updatedAt: '',
  cache: {},
  filters: {},
  summary: {},
  monthlyTrend: {},
  metrics: [],
  trendRows: [],
  riskRows: [],
  sources: []
})

const timeRangeOptions = [
  { label: '近 30 天', value: '30D' },
  { label: '近 90 天', value: '90D' },
  { label: '近 180 天', value: '180D' },
  { label: '全部', value: 'ALL' }
]

const unwrapResponse = (res) => {
  if (res?.code && res.code !== 200) {
    throw new Error(res.msg || '租户增长与留存看板加载失败')
  }
  return res?.data || res || {}
}

const assignGrowthRetentionCenter = (data) => {
  Object.assign(growthRetentionCenter, {
    tenantId: data.tenantId ?? null,
    tenantScope: data.tenantScope || '',
    updatedAt: data.updatedAt || '',
    cache: data.cache || {},
    filters: data.filters || {},
    summary: data.summary || {},
    monthlyTrend: data.monthlyTrend || {},
    metrics: Array.isArray(data.metrics) ? data.metrics : [],
    trendRows: Array.isArray(data.trendRows) ? data.trendRows : [],
    riskRows: Array.isArray(data.riskRows) ? data.riskRows : [],
    sources: Array.isArray(data.sources) ? data.sources : []
  })
}

async function refreshGrowthRetention() {
  loading.value = true
  try {
    const res = await getGrowthRetentionCenter({
      timeRange: filters.timeRange,
      riskLevel: filters.riskLevel
    })
    assignGrowthRetentionCenter(unwrapResponse(res))
  } catch (error) {
    ElMessage.error(error.message || '租户增长与留存看板加载失败')
  } finally {
    loading.value = false
  }
}

const numberValue = (value) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

const summary = computed(() => growthRetentionCenter.summary || {})
const monthlyTrend = computed(() => growthRetentionCenter.monthlyTrend || {})
const trendRows = computed(() => growthRetentionCenter.trendRows || [])
const riskRows = computed(() => growthRetentionCenter.riskRows || [])
const sourceRows = computed(() => growthRetentionCenter.sources || [])

const summaryCards = computed(() => [
  { metric: 'newMemberCount', label: '新增成员', displayValue: numberValue(summary.value.newMemberCount), hint: `租户 ${growthRetentionCenter.tenantScope || '-'}`, tone: 'primary' },
  { metric: 'activeClubCount', label: '活跃社团', displayValue: numberValue(summary.value.activeClubCount), hint: '部门/组织活跃度', tone: 'success' },
  { metric: 'dormantMemberCount', label: '沉默成员', displayValue: numberValue(summary.value.dormantMemberCount), hint: '需要召回触达', tone: 'warning' },
  { metric: 'retentionRate', label: '留存率', displayValue: `${numberValue(summary.value.retentionRate)}%`, hint: `${numberValue(summary.value.activeMemberCount)} 活跃`, tone: 'info' },
  { metric: 'volunteerHours', label: '志愿时长', displayValue: numberValue(summary.value.volunteerHours).toFixed(1), hint: '已审核服务时长', tone: 'primary' },
  { metric: 'creditTotal', label: '积分总额', displayValue: numberValue(summary.value.creditTotal).toFixed(1), hint: `${numberValue(summary.value.creditRecordCount)} 条记录`, tone: 'success' }
])

function trendBarWidth(row) {
  const max = Math.max(1, ...trendRows.value.map((item) => (
    numberValue(item.newMemberCount) +
    numberValue(item.activeMemberCount) +
    numberValue(item.volunteerHours) +
    numberValue(item.creditTotal)
  )))
  const current = numberValue(row.newMemberCount) + numberValue(row.activeMemberCount) + numberValue(row.volunteerHours) + numberValue(row.creditTotal)
  return `${Math.max(8, Math.round((current / max) * 100))}%`
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

onMounted(refreshGrowthRetention)
</script>

<style scoped>
.growth-retention-center {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-toolbar,
.filter-panel,
.growth-card {
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

.summary-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
}

.summary-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-height: 96px;
  padding: 16px 18px;
  background: #fff;
  border: 1px solid #edf0f7;
  border-left: 4px solid #6f7d95;
  border-radius: 8px;
}

.summary-card span {
  color: #68758f;
}

.summary-card strong {
  font-size: 26px;
  color: #1f2a44;
}

.summary-card small {
  color: #8a96ad;
}

.summary-card.success {
  border-left-color: #2f9e44;
}

.summary-card.warning {
  border-left-color: #f59f00;
}

.summary-card.info {
  border-left-color: #228be6;
}

.retention-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.retention-panel {
  padding: 18px;
  background: #fff;
  border: 1px solid #edf0f7;
  border-radius: 8px;
}

.retention-panel header,
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.metric-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 16px;
}

.metric-list div {
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
}

.metric-list span {
  display: block;
  color: #68758f;
}

.metric-list strong {
  display: block;
  margin-top: 6px;
  font-size: 22px;
  color: #1f2a44;
}

.bar-track {
  height: 8px;
  overflow: hidden;
  background: #edf2f7;
  border-radius: 999px;
}

.bar-track span {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #2b6cb0, #38a169);
  border-radius: inherit;
}

@media (max-width: 1280px) {
  .summary-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .retention-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .page-toolbar,
  .toolbar-actions,
  .filter-panel {
    align-items: stretch;
    flex-direction: column;
  }

  .summary-grid,
  .metric-list {
    grid-template-columns: 1fr;
  }
}
</style>
