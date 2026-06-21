<template>
  <div class="security-audit-center">
    <section class="page-toolbar">
      <div>
        <h1>安全审计中心</h1>
        <p>聚合跨租户访问、高风险后台操作、异常登录位置和权限变更记录，按菜单权限联动审计。</p>
      </div>
      <div class="toolbar-actions">
        <span>更新时间：{{ securityAuditCenter.updatedAt || '-' }}</span>
        <el-button type="primary" :loading="loading" @click="refreshSecurityAudit">刷新</el-button>
      </div>
    </section>

    <section class="filter-panel">
      <div class="filter-row">
        <label>风险类型</label>
        <el-segmented v-model="filters.riskType" :options="riskTypeOptions" @change="refreshSecurityAudit" />
      </div>
      <div class="filter-row compact">
        <label>风险等级</label>
        <el-select v-model="filters.riskLevel" size="small" @change="refreshSecurityAudit">
          <el-option label="全部" value="ALL" />
          <el-option label="高" value="HIGH" />
          <el-option label="中" value="MEDIUM" />
          <el-option label="低" value="LOW" />
        </el-select>
      </div>
      <div class="filter-row compact">
        <label>通知状态</label>
        <el-select v-model="filters.notifyStatus" size="small" @change="refreshSecurityAudit">
          <el-option label="全部" value="ALL" />
          <el-option label="待通知" value="PENDING" />
          <el-option label="已登记" value="REGISTERED" />
          <el-option label="无需通知" value="NORMAL" />
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

    <el-card class="audit-card">
      <template #header>
        <div class="card-header">
          <strong>安全风险明细</strong>
          <span>{{ riskRows.length }} 条</span>
        </div>
      </template>
      <el-table v-loading="loading" :data="riskRows" size="small" empty-text="暂无安全审计风险">
        <el-table-column prop="riskTypeLabel" label="类型" width="140" />
        <el-table-column prop="title" label="事项" min-width="180" show-overflow-tooltip />
        <el-table-column prop="description" label="说明" min-width="240" show-overflow-tooltip />
        <el-table-column prop="riskLevel" label="风险" width="90">
          <template #default="{ row }">
            <el-tag :type="riskTagType(row.riskLevel)" size="small">{{ riskText(row.riskLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="130" />
        <el-table-column prop="actorName" label="账号" width="120" show-overflow-tooltip />
        <el-table-column prop="loginLocation" label="位置/IP" min-width="150" show-overflow-tooltip />
        <el-table-column prop="menuName" label="菜单权限联动" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag v-if="row.menuPermissionLinked" size="small" type="warning">{{ row.menuName || '菜单权限联动' }}</el-tag>
            <span v-else>{{ row.menuName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="notifyStatus" label="高风险通知" width="120">
          <template #default="{ row }">{{ notifyStatusText(row.notifyStatus) }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" min-width="160" show-overflow-tooltip />
        <el-table-column label="动作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              :disabled="!row.notifyable"
              @click="handleNotify(row)"
            >
              {{ row.notifyable ? '登记通知' : '查看' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="audit-card">
      <template #header>
        <div class="card-header">
          <strong>通知登记记录</strong>
          <span>{{ notifyLogs.length }} 条</span>
        </div>
      </template>
      <el-table :data="notifyLogs" size="small" empty-text="暂无通知登记记录">
        <el-table-column prop="riskType" label="类型" width="140" />
        <el-table-column prop="riskLevel" label="等级" width="90" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column prop="audience" label="通知对象" min-width="140" />
        <el-table-column prop="reason" label="原因" min-width="220" show-overflow-tooltip />
        <el-table-column prop="message" label="结果" min-width="260" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="时间" min-width="180" show-overflow-tooltip />
      </el-table>
    </el-card>

    <el-card class="audit-card">
      <template #header>
        <div class="card-header">
          <strong>来源诊断</strong>
          <span>{{ sources.length }} 条</span>
        </div>
      </template>
      <el-table :data="sources.slice(0, 20)" size="small" empty-text="暂无来源诊断">
        <el-table-column prop="metric" label="指标" min-width="180" />
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
import { getSecurityAuditCenter, notifySecurityAuditRisk } from '@/api/data'

const loading = ref(false)
const notifying = ref(false)
const filters = reactive({
  riskType: 'ALL',
  riskLevel: 'ALL',
  notifyStatus: 'ALL'
})
const securityAuditCenter = reactive({
  tenantId: null,
  tenantScope: '',
  updatedAt: '',
  cache: {},
  filters: {},
  summary: {},
  risks: [],
  notifyLogs: [],
  sources: []
})

const riskTypeOptions = [
  { label: '全部', value: 'ALL' },
  { label: '跨租户访问', value: 'CROSS_TENANT' },
  { label: '高风险后台操作', value: 'HIGH_RISK_OPERATION' },
  { label: '异常登录位置', value: 'ABNORMAL_LOGIN' },
  { label: '权限变更记录', value: 'PERMISSION_CHANGE' }
]

const unwrapResponse = (res) => {
  if (res?.code && res.code !== 200) {
    throw new Error(res.msg || '安全审计中心加载失败')
  }
  return res?.data || res || {}
}

const assignSecurityAuditCenter = (data) => {
  Object.assign(securityAuditCenter, {
    tenantId: data.tenantId ?? null,
    tenantScope: data.tenantScope || '',
    updatedAt: data.updatedAt || '',
    cache: data.cache || {},
    filters: data.filters || {},
    summary: data.summary || {},
    risks: Array.isArray(data.risks) ? data.risks : [],
    notifyLogs: Array.isArray(data.notifyLogs) ? data.notifyLogs : [],
    sources: Array.isArray(data.sources) ? data.sources : []
  })
}

async function refreshSecurityAudit() {
  loading.value = true
  try {
    const res = await getSecurityAuditCenter({
      riskType: filters.riskType,
      riskLevel: filters.riskLevel,
      notifyStatus: filters.notifyStatus
    })
    assignSecurityAuditCenter(unwrapResponse(res))
  } catch (error) {
    ElMessage.error(error.message || '安全审计中心加载失败')
  } finally {
    loading.value = false
  }
}

async function handleNotify(row) {
  if (!row?.notifyable || notifying.value) return
  notifying.value = true
  try {
    const res = await notifySecurityAuditRisk({
      riskType: row.riskType,
      riskLevel: row.riskLevel,
      reason: `后台安全审计中心登记通知：${row.title || row.riskType}`,
      audience: 'tenant-admin'
    })
    const data = unwrapResponse(res)
    ElMessage.success(data.message || '高风险通知已登记')
    await refreshSecurityAudit()
  } catch (error) {
    ElMessage.error(error.message || '高风险通知登记失败')
  } finally {
    notifying.value = false
  }
}

const numberValue = (value) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

const summary = computed(() => securityAuditCenter.summary || {})
const riskRows = computed(() => securityAuditCenter.risks || [])
const notifyLogs = computed(() => securityAuditCenter.notifyLogs || [])
const sources = computed(() => securityAuditCenter.sources || [])

const summaryCards = computed(() => [
  { label: '风险总数', value: numberValue(summary.value.totalRiskCount), hint: `租户 ${securityAuditCenter.tenantScope || '-'}`, tone: numberValue(summary.value.totalRiskCount) ? 'danger' : 'success' },
  { label: '跨租户访问', value: numberValue(summary.value.crossTenantCount), hint: '租户隔离优先', tone: 'warning' },
  { label: '高危后台操作', value: numberValue(summary.value.highRiskOperationCount), hint: '删除/导出/授权等操作', tone: 'danger' },
  { label: '高风险通知', value: numberValue(summary.value.notifyPendingCount), hint: `已登记 ${numberValue(summary.value.notifySentCount)} 条`, tone: 'info' }
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

function notifyStatusText(value) {
  if (value === 'PENDING') return '待通知'
  if (value === 'REGISTERED') return '已登记'
  if (value === 'NORMAL') return '无需通知'
  if (value === 'UNKNOWN') return '未知'
  return value || '-'
}

onMounted(refreshSecurityAudit)
</script>

<style scoped>
.security-audit-center {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-toolbar,
.filter-panel,
.audit-card {
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
  color: #4b587c;
  font-weight: 600;
  white-space: nowrap;
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
  font-size: 28px;
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

.summary-card.danger {
  border-left-color: #e03131;
}

.summary-card.info {
  border-left-color: #1c7ed6;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #1f2a44;
}

@media (max-width: 1100px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .page-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 720px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .filter-panel,
  .filter-row {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
