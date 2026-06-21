<template>
  <div class="credit-report" v-loading="loading">
    <div v-if="!userStore.isLoggedIn" class="login-prompt">
      <el-empty description="请先登录后查看第二课堂成绩单">
        <el-button type="primary" @click="$router.push('/login')">去登录</el-button>
      </el-empty>
    </div>

    <template v-else>
      <div class="report-top-bar"></div>

      <div class="report-container">
        <header class="report-header">
          <div class="header-badge">
            <el-icon :size="28" color="#fff"><Reading /></el-icon>
          </div>
          <h1 class="report-title">第二课堂成绩单</h1>
          <p class="report-subtitle">Second Classroom Credit Transcript</p>
        </header>

        <div class="user-info-row">
          <div class="user-info-left">
            <el-avatar :size="48" :src="userStore.userInfo?.icon || defaultAvatar" />
            <div class="user-info-text">
              <span class="user-info-name">{{ userStore.userName || '同学' }}</span>
              <span class="user-info-label">学号 / 成员档案</span>
            </div>
          </div>
          <div class="user-info-right">
            <span class="report-date">生成日期：{{ currentDate }}</span>
          </div>
        </div>

        <div class="stats-row">
          <div class="stat-card stat-card--credits">
            <div class="stat-icon-wrap stat-icon-wrap--blue">
              <el-icon :size="26"><Trophy /></el-icon>
            </div>
            <div class="stat-body">
              <span class="stat-value">{{ summary.totalCredits ?? 0 }}</span>
              <span class="stat-label">总学分</span>
            </div>
          </div>
          <div class="stat-card stat-card--hours">
            <div class="stat-icon-wrap stat-icon-wrap--green">
              <el-icon :size="26"><Clock /></el-icon>
            </div>
            <div class="stat-body">
              <span class="stat-value">{{ summary.totalHours ?? 0 }}</span>
              <span class="stat-label">总学时</span>
            </div>
          </div>
          <div class="stat-card stat-card--volunteer">
            <div class="stat-icon-wrap stat-icon-wrap--orange">
              <el-icon :size="26"><Calendar /></el-icon>
            </div>
            <div class="stat-body">
              <span class="stat-value">{{ summary.volunteerHours ?? 0 }}</span>
              <span class="stat-label">志愿时长</span>
            </div>
          </div>
          <div class="stat-card stat-card--count">
            <div class="stat-icon-wrap stat-icon-wrap--purple">
              <el-icon :size="26"><Medal /></el-icon>
            </div>
            <div class="stat-body">
              <span class="stat-value">{{ summary.activityCount ?? 0 }}</span>
              <span class="stat-label">活动参与</span>
            </div>
          </div>
        </div>

        <section class="report-section">
          <h2 class="section-title">
            <span class="section-title-bar"></span>
            学分分类汇总
          </h2>
          <div v-if="creditTypes.length === 0" class="section-empty">
            <el-empty description="暂无学分分类数据" :image-size="64" />
          </div>
          <div v-else class="breakdown-grid">
            <div
              v-for="ct in creditTypes"
              :key="ct.typeId || ct.id || ct.code"
              class="breakdown-item"
            >
              <div class="breakdown-head">
                <div class="breakdown-name">{{ ct.name || '-' }}</div>
                <el-tag v-if="ct.maxCredit !== null && ct.maxCredit !== undefined" size="small" effect="plain">
                  上限 {{ ct.maxCredit }}
                </el-tag>
              </div>
              <div class="breakdown-values">
                <div class="breakdown-metric">
                  <span class="breakdown-metric-value">{{ ct.totalCredits ?? 0 }}</span>
                  <span class="breakdown-metric-label">学分</span>
                </div>
                <div class="breakdown-divider"></div>
                <div class="breakdown-metric">
                  <span class="breakdown-metric-value">{{ ct.totalHours ?? 0 }}</span>
                  <span class="breakdown-metric-label">学时</span>
                </div>
                <div class="breakdown-divider"></div>
                <div class="breakdown-metric">
                  <span class="breakdown-metric-value">{{ ct.recordCount ?? 0 }}</span>
                  <span class="breakdown-metric-label">记录数</span>
                </div>
              </div>
              <p v-if="ct.description" class="breakdown-description">{{ ct.description }}</p>
            </div>
          </div>
        </section>

        <section class="report-section">
          <h2 class="section-title">
            <span class="section-title-bar"></span>
            学分记录
            <el-tag v-if="recordsTotal > 0" type="info" size="small" effect="plain" class="record-count-tag">
              共 {{ recordsTotal }} 条
            </el-tag>
          </h2>

          <div v-if="records.length === 0 && !loading" class="section-empty">
            <el-empty description="暂无学分记录" :image-size="64" />
          </div>
          <template v-else>
            <el-table
              :data="records"
              stripe
              class="records-table"
              :header-cell-style="{ background: '#f0f5fb', color: '#1a3a5c', fontWeight: 600 }"
            >
              <el-table-column label="日期" min-width="120">
                <template #default="{ row }">
                  {{ formatDate(row.createTime || row.date || row.createdAt || row.verifyTime) }}
                </template>
              </el-table-column>
              <el-table-column label="类型" min-width="140">
                <template #default="{ row }">
                  {{ row.typeName || row.creditTypeCode || '-' }}
                </template>
              </el-table-column>
              <el-table-column label="来源" min-width="220">
                <template #default="{ row }">
                  <el-tooltip
                    :content="creditSourceText(row)"
                    placement="top"
                    :disabled="creditSourceText(row) === '-'"
                  >
                    <span class="table-ellipsis-text">{{ creditSourceText(row) }}</span>
                  </el-tooltip>
                </template>
              </el-table-column>
              <el-table-column label="学时" width="90" align="center">
                <template #default="{ row }">
                  {{ row.hours ?? row.creditHours ?? 0 }}
                </template>
              </el-table-column>
              <el-table-column label="学分" width="90" align="center">
                <template #default="{ row }">
                  {{ row.credits ?? row.creditScore ?? 0 }}
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="statusTagType(row.status)" size="small" effect="light">
                    {{ statusText(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>

            <el-pagination
              v-if="recordsTotal > pageSize"
              v-model:current-page="currentPage"
              :page-size="pageSize"
              :total="recordsTotal"
              layout="prev, pager, next"
              class="records-pagination"
              @current-change="fetchRecords"
            />
          </template>
        </section>

        <section v-if="summary.competitionAwards" class="report-section">
          <h2 class="section-title">
            <span class="section-title-bar"></span>
            竞赛获奖
          </h2>
          <div class="awards-highlight">
            <el-icon :size="20" color="#e6a23c"><Trophy /></el-icon>
            <span>累计获得 <strong>{{ summary.competitionAwards }}</strong> 项竞赛奖项</span>
          </div>
        </section>

        <footer class="report-footer no-print">
          <el-button type="primary" size="large" @click="handlePrint">
            <el-icon><Printer /></el-icon>
            打印成绩单
          </el-button>
        </footer>
      </div>

      <div class="report-bottom-bar"></div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Trophy, Clock, Calendar, Medal, Printer, Reading } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getMyCreditSummary, getMyCreditRecords, getMyCreditBreakdown } from '@/api/credit'
import { normalizeCreditBreakdown, normalizeCreditRecords } from '@/utils/creditReport'

const userStore = useUserStore()

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const loading = ref(false)
const summary = ref({
  totalCredits: 0,
  totalHours: 0,
  volunteerHours: 0,
  activityCount: 0,
  competitionAwards: 0
})
const creditTypes = ref([])
const records = ref([])
const recordsTotal = ref(0)
const currentPage = ref(1)
const pageSize = 10

const currentDate = computed(() => {
  const now = new Date()
  return now.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
})

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

const creditSourceText = (row = {}) => row.sourceName || row.description || row.sourceType || '-'

const statusTagType = (status) => {
  const map = {
    0: 'warning',
    1: 'success',
    2: 'danger',
    APPROVED: 'success',
    PENDING: 'warning',
    REJECTED: 'danger'
  }
  return map[status] ?? 'info'
}

const statusText = (status) => {
  const map = {
    0: '待审核',
    1: '已通过',
    2: '已驳回',
    APPROVED: '已通过',
    PENDING: '待审核',
    REJECTED: '已驳回'
  }
  return map[status] ?? '未知'
}

const fetchSummary = async () => {
  const res = await getMyCreditSummary()
  if (res.code === 200 && res.data) {
    summary.value = {
      totalCredits: res.data.totalCredits ?? 0,
      totalHours: res.data.totalHours ?? 0,
      volunteerHours: res.data.volunteerHours ?? 0,
      activityCount: res.data.activityCount ?? 0,
      competitionAwards: res.data.competitionAwards ?? 0
    }
  }
}

const fetchBreakdown = async () => {
  const res = await getMyCreditBreakdown()
  creditTypes.value = res.code === 200 ? normalizeCreditBreakdown(res.data) : []
}

const fetchRecords = async () => {
  const res = await getMyCreditRecords({ page: currentPage.value, size: pageSize })
  if (res.code === 200) {
    const normalized = normalizeCreditRecords(res.data, creditTypes.value)
    records.value = normalized.records
    recordsTotal.value = normalized.total
    return
  }
  records.value = []
  recordsTotal.value = 0
}

const handlePrint = () => {
  window.print()
}

onMounted(async () => {
  if (!userStore.isLoggedIn) return

  loading.value = true
  try {
    await fetchSummary()
    await fetchBreakdown()
    await fetchRecords()
  } catch (error) {
    console.error('加载成绩单数据失败:', error)
    ElMessage.error('加载成绩单数据失败，请稍后重试')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.credit-report {
  min-height: 100vh;
  background: #eef2f7;
  padding: 32px 16px 48px;
}

.report-top-bar {
  max-width: 960px;
  margin: 0 auto 0;
  height: 6px;
  background: linear-gradient(90deg, #1a3a5c 0%, #2b6cb0 40%, #4299e1 100%);
  border-radius: 6px 6px 0 0;
}

.report-bottom-bar {
  max-width: 960px;
  margin: 0 auto;
  height: 6px;
  background: linear-gradient(90deg, #4299e1 0%, #2b6cb0 60%, #1a3a5c 100%);
  border-radius: 0 0 6px 6px;
}

.report-container {
  max-width: 960px;
  margin: 0 auto;
  background: #fff;
  padding: 0 40px 40px;
  box-shadow: 0 2px 12px rgba(26, 58, 92, 0.08);
}

.report-header {
  text-align: center;
  padding: 40px 0 28px;
  border-bottom: 2px solid #e8eef4;
}

.header-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1a3a5c, #2b6cb0);
  margin-bottom: 16px;
}

.report-title {
  margin: 0 0 6px;
  font-size: 30px;
  font-weight: 700;
  color: #1a3a5c;
  letter-spacing: 0.08em;
}

.report-subtitle {
  margin: 0;
  font-size: 13px;
  color: #8da2b5;
  letter-spacing: 0.04em;
}

.user-info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 0;
  border-bottom: 1px solid #eef2f7;
}

.user-info-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.user-info-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-info-name {
  font-size: 17px;
  font-weight: 600;
  color: #1a3a5c;
}

.user-info-label {
  font-size: 12px;
  color: #8da2b5;
}

.user-info-right {
  color: #8da2b5;
  font-size: 13px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  padding: 28px 0;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px 18px;
  border-radius: 12px;
  border: 1px solid #eef2f7;
  background: #fafcfe;
  transition: box-shadow 0.2s ease, border-color 0.2s ease;
}

.stat-card:hover {
  border-color: #d0dde8;
  box-shadow: 0 4px 16px rgba(26, 58, 92, 0.06);
}

.stat-icon-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  flex-shrink: 0;
}

.stat-icon-wrap--blue {
  background: rgba(43, 108, 176, 0.1);
  color: #2b6cb0;
}

.stat-icon-wrap--green {
  background: rgba(56, 161, 105, 0.1);
  color: #38a169;
}

.stat-icon-wrap--orange {
  background: rgba(221, 132, 46, 0.1);
  color: #dd842e;
}

.stat-icon-wrap--purple {
  background: rgba(128, 90, 213, 0.1);
  color: #805ad5;
}

.stat-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1a3a5c;
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: #8da2b5;
}

.report-section {
  padding: 24px 0;
  border-top: 1px solid #eef2f7;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 20px;
  font-size: 18px;
  font-weight: 600;
  color: #1a3a5c;
}

.section-title-bar {
  display: inline-block;
  width: 4px;
  height: 20px;
  border-radius: 2px;
  background: linear-gradient(180deg, #2b6cb0, #4299e1);
}

.record-count-tag {
  margin-left: 8px;
}

.section-empty {
  padding: 20px 0;
}

.breakdown-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 14px;
}

.breakdown-item {
  padding: 16px 18px;
  border-radius: 10px;
  border: 1px solid #eef2f7;
  background: #fafcfe;
}

.breakdown-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.breakdown-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a3a5c;
}

.breakdown-values {
  display: flex;
  align-items: center;
  gap: 12px;
}

.breakdown-metric {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.breakdown-metric-value {
  font-size: 18px;
  font-weight: 700;
  color: #2b6cb0;
}

.breakdown-metric-label {
  font-size: 11px;
  color: #8da2b5;
}

.breakdown-divider {
  width: 1px;
  height: 28px;
  background: #e2e8f0;
}

.breakdown-description {
  margin: 12px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: #8da2b5;
}

.records-table {
  width: 100%;
  border-radius: 8px;
  overflow: hidden;
}

.records-table :deep(.el-table__header th) {
  font-size: 13px;
}

.records-table :deep(.el-table__body td) {
  font-size: 13px;
}

.table-ellipsis-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  color: #334155;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}

.records-pagination {
  margin-top: 20px;
  justify-content: center;
}

.awards-highlight {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px;
  border-radius: 10px;
  border: 1px solid #faecd8;
  background: #fdf6ec;
  font-size: 14px;
  color: #7a6432;
}

.awards-highlight strong {
  color: #c8860e;
  font-size: 18px;
}

.report-footer {
  display: flex;
  justify-content: center;
  padding-top: 28px;
  border-top: 1px solid #eef2f7;
  margin-top: 8px;
}

.login-prompt {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
}

@media (max-width: 768px) {
  .credit-report {
    padding: 16px 8px 32px;
  }

  .report-container {
    padding: 0 20px 24px;
  }

  .report-title {
    font-size: 24px;
  }

  .stats-row {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .stat-card {
    padding: 14px 12px;
  }

  .stat-value {
    font-size: 20px;
  }

  .stat-icon-wrap {
    width: 40px;
    height: 40px;
  }

  .user-info-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .breakdown-grid {
    grid-template-columns: 1fr;
  }

  .records-table :deep(.el-table__body-wrapper) {
    overflow-x: auto;
  }
}

@media (max-width: 480px) {
  .stats-row {
    grid-template-columns: 1fr;
  }

  .report-container {
    padding: 0 14px 20px;
  }

  .report-header {
    padding: 28px 0 20px;
  }

  .report-title {
    font-size: 21px;
  }
}

@media print {
  .credit-report {
    background: #fff;
    padding: 0;
  }

  .report-top-bar,
  .report-bottom-bar {
    display: none;
  }

  .report-container {
    box-shadow: none;
    padding: 0 24px 24px;
    max-width: 100%;
  }

  .no-print {
    display: none !important;
  }

  .stat-card:hover {
    box-shadow: none;
  }

  .report-header {
    padding: 20px 0 16px;
  }

  .report-title {
    font-size: 24px;
  }

  .header-badge {
    width: 44px;
    height: 44px;
  }

  .records-table {
    font-size: 11px;
  }

  .records-pagination {
    display: none;
  }

  :deep(nav),
  :deep(.sidebar),
  :deep(.main-sidebar),
  :deep(.el-menu),
  :deep(.layout-sidebar),
  :deep(.site-header),
  :deep(.site-footer) {
    display: none !important;
  }
}
</style>
