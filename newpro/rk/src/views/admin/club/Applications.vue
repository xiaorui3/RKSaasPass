<template>
  <div class="admin-page">
    <AdminModuleShell
      eyebrow="社团运营"
      title="统一审批队列"
      description="将入社申请和注册审核收拢到同一工作台，统一筛选、统一处理、统一留痕。"
      :meta-items="moduleMeta"
      :stat-items="overviewStats"
    >
      <template #actions>
        <el-button :loading="loading" @click="fetchApplications">刷新队列</el-button>
      </template>

      <template #filters>
        <div class="filter-layout">
          <el-form :model="searchForm" label-position="top" class="filter-form" @submit.prevent>
            <el-form-item label="申请人">
              <el-input
                v-model="searchForm.name"
                placeholder="按姓名、账号、邮箱检索"
                clearable
                @keyup.enter="applyFilters"
              />
            </el-form-item>
            <el-form-item label="审批状态">
              <el-select v-model="searchForm.status" placeholder="全部状态" clearable>
                <el-option
                  v-for="option in statusOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="申请类型">
              <el-select v-model="searchForm.businessType" placeholder="全部类型" clearable>
                <el-option label="全部类型" value="" />
                <el-option label="入社申请" value="join" />
                <el-option label="注册审核" value="register" />
              </el-select>
            </el-form-item>
            <el-form-item class="filter-actions">
              <el-button type="primary" @click="applyFilters">筛选</el-button>
              <el-button @click="resetSearch">重置</el-button>
            </el-form-item>
          </el-form>

          <div class="filter-side">
            <span class="filter-side-label">快速视角</span>
            <div class="quick-filters">
              <button
                v-for="option in quickStatusOptions"
                :key="option.value"
                type="button"
                class="quick-filter"
                :class="{ active: searchForm.status === option.value }"
                @click="applyQuickStatus(option.value)"
              >
                <strong>{{ option.count }}</strong>
                <span>{{ option.label }}</span>
              </button>
            </div>
          </div>
        </div>
      </template>

      <AdminPanelCard
        title="审批处理队列"
        description="统一查看当前租户的注册审核与入社申请，避免统计口径分裂。"
      >
        <template #headerActions>
          <el-tag type="info">当前 {{ filteredApplications.length }} 条</el-tag>
        </template>

        <div class="table-shell">
          <div class="table-toolbar">
            <div class="toolbar-copy">
              <span class="toolbar-title">当前筛选结果</span>
              <span class="toolbar-hint">
                待处理 {{ pendingCount }} 条，已审核 {{ approvedCount + rejectedCount }} 条
              </span>
            </div>
            <el-tag :type="pendingCount ? 'warning' : 'success'" effect="dark">
              {{ pendingCount ? '仍有待处理审批' : '当前队列已清空' }}
            </el-tag>
          </div>

          <el-table :data="pagedApplications" v-loading="loading" stripe class="admin-table">
            <el-table-column prop="businessLabel" label="类型" width="110">
              <template #default="{ row }">
                <el-tag :type="row.businessType === 'register' ? 'primary' : 'success'">
                  {{ row.businessLabel }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="name" label="姓名" min-width="120" />
            <el-table-column prop="username" label="账号" min-width="120" show-overflow-tooltip />
            <el-table-column prop="studentId" label="学号" min-width="130" />
            <el-table-column prop="major" label="专业" min-width="150" show-overflow-tooltip />
            <el-table-column prop="phone" label="联系电话" min-width="140" />
            <el-table-column prop="email" label="邮箱" min-width="220" show-overflow-tooltip />
            <el-table-column prop="reason" label="说明" min-width="220" show-overflow-tooltip />
            <el-table-column prop="createTime" label="申请时间" min-width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="getStatusTag(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <div class="row-actions">
                  <el-button
                    v-if="row.status === 0"
                    text
                    type="success"
                    @click="handleApprove(row)"
                  >
                    通过
                  </el-button>
                  <el-button
                    v-if="row.status === 0"
                    text
                    type="danger"
                    @click="handleReject(row)"
                  >
                    拒绝
                  </el-button>
                  <el-text v-if="row.status !== 0" type="info" size="small">已处理</el-text>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :total="filteredApplications.length"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @change="syncPageBounds"
          />
        </div>
      </AdminPanelCard>

      <template #aside>
        <AdminPanelCard
          title="审批概览"
          description="统一展示注册审核和入社申请的实时处理情况。"
        >
          <div class="side-stat-list">
            <div class="side-stat-item">
              <span>待处理</span>
              <strong>{{ pendingCount }}</strong>
            </div>
            <div class="side-stat-item">
              <span>通过率</span>
              <strong>{{ approvalRate }}</strong>
            </div>
            <div class="side-stat-item">
              <span>最近刷新</span>
              <strong>{{ lastUpdatedLabel }}</strong>
            </div>
          </div>
        </AdminPanelCard>

        <AdminPanelCard
          title="处理提示"
          description="注册审核和入社申请共享同一审批心智，但数据来源不同。"
        >
          <ul class="side-list">
            <li>注册审核用于开通租户账号，入社申请用于进入成员台账。</li>
            <li>同一名用户可能先经过注册审核，再发起入社申请，不应混算为同一条记录。</li>
            <li>当前页面会保留两种记录的业务类型，避免“已通过大于成员数”时完全失去解释。</li>
          </ul>
        </AdminPanelCard>
      </template>
    </AdminModuleShell>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminModuleShell from '@/components/admin-shell/AdminModuleShell.vue'
import AdminPanelCard from '@/components/admin-shell/AdminPanelCard.vue'
import {
  getAllApplications,
  getAllRegisterReviews,
  reviewApplication,
  reviewRegisterReview
} from '@/api/admission'
import { buildAdmissionQueue } from '@/utils/admissionQueue'

const loading = ref(false)
const allApplications = ref([])
const lastUpdated = ref('')

const searchForm = reactive({
  name: '',
  status: '',
  businessType: ''
})

const pagination = reactive({
  page: 1,
  size: 10
})

const statusValueMap = {
  0: '待审核',
  1: '已通过',
  2: '已拒绝'
}

const statusOptions = [
  { label: '待审核', value: 0 },
  { label: '已通过', value: 1 },
  { label: '已拒绝', value: 2 }
]

const filteredApplications = computed(() => {
  const keyword = searchForm.name.trim().toLowerCase()
  return allApplications.value.filter((app) => {
    const haystacks = [
      app.name,
      app.username,
      app.email,
      app.studentId
    ].map((item) => String(item || '').toLowerCase())
    const matchesKeyword = !keyword || haystacks.some((item) => item.includes(keyword))
    const matchesStatus = searchForm.status === '' || app.status === searchForm.status
    const matchesType = !searchForm.businessType || app.businessType === searchForm.businessType
    return matchesKeyword && matchesStatus && matchesType
  })
})

const pagedApplications = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  return filteredApplications.value.slice(start, start + pagination.size)
})

const pendingCount = computed(() => allApplications.value.filter((item) => item.status === 0).length)
const approvedCount = computed(() => allApplications.value.filter((item) => item.status === 1).length)
const rejectedCount = computed(() => allApplications.value.filter((item) => item.status === 2).length)
const registerCount = computed(() => allApplications.value.filter((item) => item.businessType === 'register').length)
const joinCount = computed(() => allApplications.value.filter((item) => item.businessType === 'join').length)

const approvalRate = computed(() => {
  const reviewed = approvedCount.value + rejectedCount.value
  if (!reviewed) return '0%'
  return `${Math.round((approvedCount.value / reviewed) * 100)}%`
})

const overviewStats = computed(() => [
  { label: '审批总量', value: allApplications.value.length, helper: '统一队列中的全部记录', tone: 'info' },
  { label: '待审核', value: pendingCount.value, helper: '需要优先处理的积压', tone: 'warning' },
  { label: '注册审核', value: registerCount.value, helper: '账号开通审批', tone: 'primary' },
  { label: '入社申请', value: joinCount.value, helper: '成员准入审批', tone: 'success' }
])

const lastUpdatedLabel = computed(() => lastUpdated.value || '尚未刷新')

const moduleMeta = computed(() => [
  `最近刷新 ${lastUpdatedLabel.value}`,
  `当前显示 ${filteredApplications.value.length} 条`
])

const quickStatusOptions = computed(() => [
  { label: '全部', value: '', count: allApplications.value.length },
  { label: '待审核', value: 0, count: pendingCount.value },
  { label: '已通过', value: 1, count: approvedCount.value },
  { label: '已拒绝', value: 2, count: rejectedCount.value }
])

watch(filteredApplications, () => {
  syncPageBounds()
})

function syncPageBounds() {
  const totalPages = Math.max(1, Math.ceil(filteredApplications.value.length / pagination.size))
  if (pagination.page > totalPages) {
    pagination.page = totalPages
  }
}

function formatDateTime(value) {
  return value ? new Date(value).toLocaleString('zh-CN') : '-'
}

function getStatusLabel(status) {
  return statusValueMap[status] || '未知'
}

function getStatusTag(status) {
  if (status === 0) return 'warning'
  if (status === 1) return 'success'
  return 'danger'
}

async function fetchApplications() {
  loading.value = true
  try {
    const [joinResponse, registerResponse] = await Promise.all([
      getAllApplications(),
      getAllRegisterReviews()
    ])

    const joinApplications = joinResponse.code === 200 && Array.isArray(joinResponse.data) ? joinResponse.data : []
    const registerReviews = registerResponse.code === 200 && Array.isArray(registerResponse.data) ? registerResponse.data : []

    allApplications.value = buildAdmissionQueue(joinApplications, registerReviews)
    lastUpdated.value = new Date().toLocaleString('zh-CN')
    syncPageBounds()
  } catch (error) {
    console.error('获取统一审批队列失败:', error)
    ElMessage.error(error.message || '获取统一审批队列失败')
    allApplications.value = []
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  pagination.page = 1
  syncPageBounds()
}

function applyQuickStatus(status) {
  searchForm.status = status
  applyFilters()
}

function resetSearch() {
  searchForm.name = ''
  searchForm.status = ''
  searchForm.businessType = ''
  pagination.page = 1
  syncPageBounds()
}

async function executeReview(row, approved, reason) {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  if (row.businessType === 'register') {
    return reviewRegisterReview({
      id: row.id,
      reviewStatus: approved ? 'APPROVED' : 'REJECTED',
      reviewComment: approved ? '注册审核通过' : reason,
      reviewerId: userInfo.userId || userInfo.id || null
    })
  }

  return reviewApplication({
    id: row.id,
    reviewStatus: approved ? '通过' : '未通过',
    reviewComment: approved ? '入社申请通过' : reason,
    reviewerId: userInfo.userId || userInfo.id || null
  })
}

async function handleApprove(row) {
  try {
    await ElMessageBox.confirm(`确定通过这条${row.businessLabel}吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'success'
    })

    const response = await executeReview(row, true)
    if (response.code === 200) {
      ElMessage.success(`${row.businessLabel}已通过`)
      await fetchApplications()
      return
    }
    throw new Error(response.msg || '审批失败')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('审批失败:', error)
      ElMessage.error(error.message || '审批失败，请稍后重试')
    }
  }
}

async function handleReject(row) {
  try {
    const { value: reason } = await ElMessageBox.prompt(`请输入${row.businessLabel}拒绝原因`, '拒绝审批', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /.+/,
      inputErrorMessage: '拒绝原因不能为空'
    })

    const response = await executeReview(row, false, reason)
    if (response.code === 200) {
      ElMessage.success(`${row.businessLabel}已拒绝`)
      await fetchApplications()
      return
    }
    throw new Error(response.msg || '审批失败')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('审批失败:', error)
      ElMessage.error(error.message || '审批失败，请稍后重试')
    }
  }
}

onMounted(() => {
  fetchApplications()
})
</script>

<style lang="scss" scoped>
.admin-page {
  display: grid;
}

.filter-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.8fr) minmax(240px, 0.9fr);
  gap: 18px;
  align-items: start;
}

.filter-form {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0 14px;
}

.filter-actions {
  align-self: end;
}

.filter-side {
  display: grid;
  gap: 10px;
  padding: 14px;
  border-radius: 16px;
  background: #f6f9fc;
  border: 1px solid #e3ebf3;
}

.filter-side-label {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #587086;
}

.quick-filters {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.quick-filter {
  display: grid;
  gap: 4px;
  padding: 12px;
  border: 1px solid #dbe5ef;
  border-radius: 14px;
  background: #ffffff;
  text-align: left;
  color: #31485d;
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.quick-filter strong {
  font-size: 20px;
  color: #102a43;
}

.quick-filter.active {
  border-color: #3a7bd5;
  box-shadow: 0 10px 24px rgba(58, 123, 213, 0.16);
  transform: translateY(-1px);
}

.table-shell {
  display: grid;
  gap: 16px;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 16px;
  background: linear-gradient(135deg, #f8fbff 0%, #f2f6fb 100%);
  border: 1px solid #e2ebf5;
}

.toolbar-copy {
  display: grid;
  gap: 4px;
}

.toolbar-title {
  font-weight: 600;
  color: #102a43;
}

.toolbar-hint {
  color: #62788c;
  font-size: 13px;
}

.admin-table :deep(.el-table__header th) {
  background: #f7f9fc;
  color: #31485d;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.side-stat-list {
  display: grid;
  gap: 14px;
}

.side-stat-item {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #edf2f7;
  color: #51697d;
}

.side-stat-item:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.side-stat-item strong {
  color: #102a43;
  font-size: 18px;
}

.side-list {
  margin: 0;
  padding-left: 18px;
  color: #5d7286;
  line-height: 1.7;
}

@media (max-width: 960px) {
  .filter-layout,
  .filter-form {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
