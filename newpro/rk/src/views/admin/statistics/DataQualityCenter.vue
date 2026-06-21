<template>
  <div class="data-quality-center">
    <section class="page-toolbar">
      <div>
        <h1>数据质量与对账中心</h1>
        <p>检查账号漂移、角色缺失、媒体缺失、ES 滞后和 Redis 风险，修复动作全量留痕。</p>
      </div>
      <div class="toolbar-actions">
        <span>更新时间：{{ dataQualityCenter.updatedAt || '-' }}</span>
        <el-button type="primary" :loading="loading" @click="refreshDataQuality">刷新</el-button>
      </div>
    </section>

    <section class="filter-panel">
      <div class="filter-row">
        <label>问题类型</label>
        <el-segmented v-model="filters.issueType" :options="issueTypeOptions" @change="refreshDataQuality" />
      </div>
      <div class="filter-row compact">
        <label>风险</label>
        <el-select v-model="filters.riskLevel" size="small" @change="refreshDataQuality">
          <el-option label="全部" value="ALL" />
          <el-option label="高" value="HIGH" />
          <el-option label="中" value="MEDIUM" />
          <el-option label="低" value="LOW" />
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

    <el-card class="quality-card">
      <template #header>
        <div class="card-header">
          <strong>质量风险明细</strong>
          <span>{{ issueRows.length }} 条</span>
        </div>
      </template>
      <el-table v-loading="loading" :data="issueRows" size="small" empty-text="暂无数据质量问题">
        <el-table-column prop="issueTypeLabel" label="类型" width="120" />
        <el-table-column prop="title" label="问题" min-width="160" show-overflow-tooltip />
        <el-table-column prop="description" label="说明" min-width="260" show-overflow-tooltip />
        <el-table-column prop="riskLevel" label="风险" width="100">
          <template #default="{ row }">
            <el-tag :type="riskTagType(row.riskLevel)" size="small">{{ riskText(row.riskLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column prop="source" label="来源" min-width="180" show-overflow-tooltip />
        <el-table-column prop="suggestion" label="建议" min-width="220" show-overflow-tooltip />
        <el-table-column label="修复" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              :disabled="!row.repairable"
              @click="handleRepair(row)"
            >
              {{ row.repairable ? '执行' : '需复核' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="quality-card">
      <template #header>
        <div class="card-header">
          <strong>修复记录</strong>
          <span>{{ repairLogs.length }} 条</span>
        </div>
      </template>
      <el-table :data="repairLogs" size="small" empty-text="暂无修复记录">
        <el-table-column prop="issueType" label="类型" width="120" />
        <el-table-column prop="action" label="动作" min-width="160" />
        <el-table-column prop="status" label="状态" width="110" />
        <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
        <el-table-column prop="message" label="结果" min-width="260" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="时间" min-width="180" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getDataQualityCenter, repairDataQualityIssue } from '@/api/data'

const loading = ref(false)
const repairing = ref(false)
const filters = reactive({
  issueType: 'ALL',
  riskLevel: 'ALL'
})
const dataQualityCenter = reactive({
  tenantId: null,
  tenantScope: '',
  updatedAt: '',
  cache: {},
  filters: {},
  summary: {},
  issues: [],
  repairLogs: [],
  sources: []
})

const issueTypeOptions = [
  { label: '全部', value: 'ALL' },
  { label: '账号漂移', value: 'ORPHAN_USER' },
  { label: '角色缺失', value: 'MISSING_ROLE' },
  { label: '媒体缺失', value: 'MISSING_MEDIA' },
  { label: 'ES 滞后', value: 'SEARCH' },
  { label: 'Redis 风险', value: 'REDIS' }
]

const unwrapResponse = (res) => {
  if (res?.code && res.code !== 200) {
    throw new Error(res.msg || '数据质量中心加载失败')
  }
  return res?.data || res || {}
}

const assignDataQualityCenter = (data) => {
  Object.assign(dataQualityCenter, {
    tenantId: data.tenantId ?? null,
    tenantScope: data.tenantScope || '',
    updatedAt: data.updatedAt || '',
    cache: data.cache || {},
    filters: data.filters || {},
    summary: data.summary || {},
    issues: Array.isArray(data.issues) ? data.issues : [],
    repairLogs: Array.isArray(data.repairLogs) ? data.repairLogs : [],
    sources: Array.isArray(data.sources) ? data.sources : []
  })
}

async function refreshDataQuality() {
  loading.value = true
  try {
    const res = await getDataQualityCenter({
      issueType: filters.issueType,
      riskLevel: filters.riskLevel
    })
    assignDataQualityCenter(unwrapResponse(res))
  } catch (error) {
    ElMessage.error(error.message || '数据质量中心加载失败')
  } finally {
    loading.value = false
  }
}

async function handleRepair(row) {
  if (!row?.repairable || repairing.value) return
  repairing.value = true
  try {
    const res = await repairDataQualityIssue({
      issueType: row.issueType,
      reason: `后台数据质量中心手动修复：${row.title || row.issueType}`
    })
    const data = unwrapResponse(res)
    ElMessage.success(data.message || '修复动作已登记')
    await refreshDataQuality()
  } catch (error) {
    ElMessage.error(error.message || '修复动作失败')
  } finally {
    repairing.value = false
  }
}

const numberValue = (value) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

const summary = computed(() => dataQualityCenter.summary || {})
const issueRows = computed(() => dataQualityCenter.issues || [])
const repairLogs = computed(() => dataQualityCenter.repairLogs || [])

const summaryCards = computed(() => [
  { label: '总问题数', value: numberValue(summary.value.totalIssueCount), hint: `租户 ${dataQualityCenter.tenantScope || '-'}`, tone: 'primary' },
  { label: '高风险', value: numberValue(summary.value.highRiskCount), hint: '账号与租户隔离优先', tone: numberValue(summary.value.highRiskCount) ? 'danger' : 'success' },
  { label: '可修复', value: numberValue(summary.value.repairableCount), hint: '可审计动作', tone: 'warning' },
  { label: 'Redis 风险', value: numberValue(summary.value.redisRiskCount), hint: '前台缓存预热', tone: 'info' }
])

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

onMounted(refreshDataQuality)
</script>

<style scoped>
.data-quality-center {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-toolbar,
.filter-panel,
.quality-card {
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
