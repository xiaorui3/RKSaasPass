<template>
  <div class="tenant-operations-center">
    <section class="page-toolbar">
      <div>
        <h1>租户运营中心</h1>
        <p>按租户聚合健康评分、统一审批、参与度分析、数据质量、增长留存和安全审计。</p>
      </div>
      <div class="toolbar-actions">
        <span>更新时间：{{ overview.updatedAt || '-' }}</span>
        <el-button type="primary" :loading="loading" @click="refreshOperationsCenter">刷新</el-button>
      </div>
    </section>

    <section class="summary-grid">
      <div class="score-panel">
        <span>健康评分</span>
        <strong>{{ healthScore.score }}</strong>
        <small>{{ healthScore.level }} · 租户 {{ overview.tenantScope || '-' }}</small>
      </div>
      <div v-for="item in summaryCards" :key="item.label" class="summary-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.hint }}</small>
      </div>
    </section>

    <section class="operations-grid">
      <article class="ops-panel">
        <header>
          <strong>统一审批</strong>
          <el-tag type="warning">{{ approvalCenter.totalPendingCount || 0 }} 待处理</el-tag>
        </header>
        <div class="metric-list">
          <div v-for="item in approvalItems" :key="item.label">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
      </article>

      <article class="ops-panel">
        <header>
          <strong>参与度分析</strong>
          <el-tag>{{ participation.registrationConversionRate || 0 }}%</el-tag>
        </header>
        <div class="metric-list">
          <div v-for="item in participationItems" :key="item.label">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
      </article>

      <article class="ops-panel">
        <header>
          <strong>数据质量</strong>
          <el-tag :type="dataQuality.issueCount ? 'danger' : 'success'">{{ dataQuality.issueCount || 0 }} 项</el-tag>
        </header>
        <div class="metric-list">
          <div v-for="item in qualityItems" :key="item.label">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
      </article>

      <article class="ops-panel">
        <header>
          <strong>增长留存</strong>
          <el-tag type="success">{{ growthRetention.newMemberCount || 0 }} 新增</el-tag>
        </header>
        <div class="metric-list">
          <div v-for="item in growthItems" :key="item.label">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
      </article>

      <article class="ops-panel">
        <header>
          <strong>安全审计</strong>
          <el-tag :type="securityAudit.totalRiskCount ? 'danger' : 'success'">{{ securityAudit.totalRiskCount || 0 }} 风险</el-tag>
        </header>
        <div class="metric-list">
          <div v-for="item in securityItems" :key="item.label">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
      </article>

      <article class="ops-panel">
        <header>
          <strong>缓存与来源</strong>
          <el-tag>{{ cache.mode || 'computed' }}</el-tag>
        </header>
        <div class="metric-list">
          <div>
            <span>缓存命中</span>
            <strong>{{ cache.hit ? '是' : '否' }}</strong>
          </div>
          <div>
            <span>TTL</span>
            <strong>{{ cache.ttlSeconds || 0 }} 秒</strong>
          </div>
          <div>
            <span>数据来源</span>
            <strong>{{ sources.length }}</strong>
          </div>
        </div>
      </article>
    </section>

    <el-card class="detail-card">
      <template #header>
        <div class="card-header">
          <strong>运营明细下钻</strong>
          <span>{{ detailRows.length }} 条</span>
        </div>
      </template>
      <el-tabs v-model="activeDetailTab">
        <el-tab-pane label="审批明细" name="approval" />
        <el-tab-pane label="质量明细" name="quality" />
        <el-tab-pane label="安全明细" name="security" />
      </el-tabs>
      <el-table :data="detailRows" size="small" empty-text="暂无明细数据">
        <el-table-column prop="title" label="事项" min-width="220" show-overflow-tooltip />
        <el-table-column prop="risk" label="风险" width="100">
          <template #default="{ row }">
            <el-tag :type="riskTagType(row.risk)" size="small">{{ row.risk || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="120" show-overflow-tooltip />
        <el-table-column prop="source" label="来源" min-width="180" show-overflow-tooltip />
        <el-table-column prop="sourceTime" label="时间" min-width="180" show-overflow-tooltip />
      </el-table>
    </el-card>

    <el-card class="source-card">
      <template #header>
        <div class="card-header">
          <strong>数据来源诊断</strong>
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
import { ElMessage } from 'element-plus'
import { getTenantOperationsCenter } from '@/api/data'

const loading = ref(false)
const activeDetailTab = ref('approval')
const overview = reactive({
  tenantId: null,
  tenantScope: '',
  tenantName: '',
  updatedAt: '',
  cache: {},
  healthScore: {},
  approvalCenter: {},
  participation: {},
  dataQuality: {},
  growthRetention: {},
  securityAudit: {},
  approvalDetails: [],
  qualityDetails: [],
  securityDetails: [],
  sources: []
})

const unwrapResponse = (res) => {
  if (res?.code && res.code !== 200) {
    throw new Error(res.msg || '租户运营中心加载失败')
  }
  return res?.data || res || {}
}

const assignOverview = (data) => {
  Object.assign(overview, {
    tenantId: data.tenantId ?? null,
    tenantScope: data.tenantScope || '',
    tenantName: data.tenantName || '',
    updatedAt: data.updatedAt || '',
    cache: data.cache || {},
    healthScore: data.healthScore || {},
    approvalCenter: data.approvalCenter || {},
    participation: data.participation || {},
    dataQuality: data.dataQuality || {},
    growthRetention: data.growthRetention || {},
    securityAudit: data.securityAudit || {},
    approvalDetails: Array.isArray(data.approvalDetails) ? data.approvalDetails : [],
    qualityDetails: Array.isArray(data.qualityDetails) ? data.qualityDetails : [],
    securityDetails: Array.isArray(data.securityDetails) ? data.securityDetails : [],
    sources: Array.isArray(data.sources) ? data.sources : []
  })
}

async function refreshOperationsCenter() {
  loading.value = true
  try {
    const res = await getTenantOperationsCenter()
    assignOverview(unwrapResponse(res))
  } catch (error) {
    ElMessage.error(error.message || '租户运营中心加载失败')
  } finally {
    loading.value = false
  }
}

const numberValue = (value) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

const healthScore = computed(() => overview.healthScore || {})
const approvalCenter = computed(() => overview.approvalCenter || {})
const participation = computed(() => overview.participation || {})
const dataQuality = computed(() => overview.dataQuality || {})
const growthRetention = computed(() => overview.growthRetention || {})
const securityAudit = computed(() => overview.securityAudit || {})
const approvalDetails = computed(() => overview.approvalDetails || [])
const qualityDetails = computed(() => overview.qualityDetails || [])
const securityDetails = computed(() => overview.securityDetails || [])
const cache = computed(() => overview.cache || {})
const sources = computed(() => overview.sources || [])

const summaryCards = computed(() => [
  { label: '待审批', value: numberValue(approvalCenter.value.totalPendingCount), hint: `超时 ${numberValue(approvalCenter.value.overdueCount)} 项` },
  { label: '报名参与', value: numberValue(participation.value.activityRegistrationCount) + numberValue(participation.value.competitionRegistrationCount), hint: '活动 + 比赛' },
  { label: '质量风险', value: numberValue(dataQuality.value.issueCount), hint: '账号、媒体、ES、Redis' },
  { label: '安全风险', value: numberValue(securityAudit.value.totalRiskCount), hint: '跨租户和高危操作' }
])

const approvalItems = computed(() => [
  { label: '入社待审', value: numberValue(approvalCenter.value.joinPendingCount) },
  { label: '活动待审', value: numberValue(approvalCenter.value.activityPendingCount) },
  { label: '比赛待审', value: numberValue(approvalCenter.value.competitionPendingCount) },
  { label: '内容待审', value: numberValue(approvalCenter.value.contentPendingCount) },
  { label: '财务待审', value: numberValue(approvalCenter.value.financePendingCount) },
  { label: 'SLA 风险率', value: `${numberValue(approvalCenter.value.slaRiskRate)}%` }
])

const participationItems = computed(() => [
  { label: '活动报名', value: numberValue(participation.value.activityRegistrationCount) },
  { label: '比赛报名', value: numberValue(participation.value.competitionRegistrationCount) },
  { label: '评论互动', value: numberValue(participation.value.commentCount) },
  { label: '新闻内容', value: numberValue(participation.value.newsCount) },
  { label: '公告通知', value: numberValue(participation.value.noticeCount) },
  { label: '活跃成员', value: numberValue(participation.value.activeMemberCount) }
])

const qualityItems = computed(() => [
  { label: '孤儿用户', value: numberValue(dataQuality.value.orphanUserCount) },
  { label: '缺失角色', value: numberValue(dataQuality.value.missingRoleCount) },
  { label: '媒体缺失', value: numberValue(dataQuality.value.missingMediaCount) },
  { label: 'ES 滞后', value: numberValue(dataQuality.value.searchStaleCount) },
  { label: 'Redis 风险', value: numberValue(dataQuality.value.redisRiskCount) }
])

const growthItems = computed(() => [
  { label: '新增成员', value: numberValue(growthRetention.value.newMemberCount) },
  { label: '活跃成员', value: numberValue(growthRetention.value.activeMemberCount) },
  { label: '沉默成员', value: numberValue(growthRetention.value.dormantMemberCount) },
  { label: '志愿时长', value: numberValue(growthRetention.value.volunteerHours).toFixed(1) },
  { label: '积分总额', value: numberValue(growthRetention.value.creditTotal).toFixed(1) }
])

const securityItems = computed(() => [
  { label: '高危操作', value: numberValue(securityAudit.value.highRiskOperationCount) },
  { label: '跨租户尝试', value: numberValue(securityAudit.value.crossTenantAttemptCount) },
  { label: '异常登录', value: numberValue(securityAudit.value.abnormalLoginCount) },
  { label: '权限变更', value: numberValue(securityAudit.value.permissionChangeCount) }
])

const detailRows = computed(() => {
  if (activeDetailTab.value === 'quality') {
    return qualityDetails.value
  }
  if (activeDetailTab.value === 'security') {
    return securityDetails.value
  }
  return approvalDetails.value
})

const riskTagType = (risk) => {
  const value = String(risk || '').toUpperCase()
  if (value === 'HIGH') {
    return 'danger'
  }
  if (value === 'MEDIUM') {
    return 'warning'
  }
  return 'success'
}

onMounted(refreshOperationsCenter)
</script>

<style scoped>
.tenant-operations-center {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
}

.page-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.page-toolbar h1 {
  margin: 0;
  font-size: 24px;
  color: #1f2937;
}

.page-toolbar p,
.toolbar-actions span,
.summary-card small,
.score-panel small {
  color: #64748b;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.summary-grid,
.operations-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.score-panel,
.summary-card,
.ops-panel {
  border: 1px solid #d8e2ef;
  border-radius: 8px;
  background: #fff;
  padding: 16px;
}

.score-panel {
  background: #0f62fe;
  color: #fff;
}

.score-panel small {
  color: rgba(255, 255, 255, 0.82);
}

.score-panel strong {
  display: block;
  margin: 8px 0;
  font-size: 42px;
  line-height: 1;
}

.summary-card strong {
  display: block;
  margin: 8px 0;
  font-size: 26px;
  color: #0f172a;
}

.ops-panel header,
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.metric-list {
  display: grid;
  gap: 8px;
}

.metric-list div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid #edf2f7;
}

.metric-list div:last-child {
  border-bottom: 0;
}

.metric-list span {
  color: #64748b;
}

.metric-list strong {
  color: #0f172a;
}

.source-card,
.detail-card {
  border-radius: 8px;
}

@media (max-width: 760px) {
  .page-toolbar,
  .toolbar-actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
