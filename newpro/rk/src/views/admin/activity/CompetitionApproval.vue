<template>
  <div class="admin-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>比赛审批</span>
          <el-button @click="fetchList">刷新</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="关键字">
          <el-input v-model="searchForm.title" placeholder="搜索比赛标题" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="dataList" v-loading="loading" stripe empty-text="暂无待审核比赛">
        <el-table-column prop="title" label="比赛标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="competitionType" label="类型" width="120" />
        <el-table-column prop="organizer" label="主办方" min-width="140" show-overflow-tooltip />
        <el-table-column prop="registrationStart" label="报名开始" width="170" />
        <el-table-column prop="registrationEnd" label="报名结束" width="170" />
        <el-table-column prop="status" label="状态" width="110" />
        <el-table-column label="审核状态" width="110">
          <template #default="{ row }">
            <el-tag :type="reviewTagType(row.teacherReviewStatus, row)">
              {{ reviewStatusLabel(row.teacherReviewStatus, row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button text type="success" @click="handleApprove(row)">通过</el-button>
            <el-button text type="danger" @click="openReject(row)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @change="fetchList"
      />
    </el-card>

    <el-dialog v-model="rejectDialogVisible" title="拒绝比赛审批" width="520px">
      <el-form>
        <el-form-item label="拒绝原因">
          <el-input
            v-model="rejectReason"
            type="textarea"
            :rows="4"
            placeholder="请输入拒绝原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="handleReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getPendingCompetitionReviewPage, reviewCompetitionByManager, reviewCompetitionByTeacher } from '@/api/competition'

const loading = ref(false)
const dataList = ref([])
const rejectDialogVisible = ref(false)
const rejectReason = ref('')
const currentRejectRow = ref(null)

const searchForm = reactive({
  title: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

function reviewStage(row) {
  if (row.managerReviewStatus === 0) return 'manager'
  if (row.teacherReviewStatus === 0) return 'teacher'
  if (row.managerReviewStatus === 2 || row.teacherReviewStatus === 2) return 'rejected'
  return 'approved'
}

function reviewStatusLabel(status, row = {}) {
  const stage = reviewStage(row)
  if (stage === 'manager') return '负责人待审'
  if (stage === 'teacher') return '指导老师待审'
  if (status === 0) return '待审核'
  if (status === 1) return '已通过'
  if (status === 2) return '已拒绝'
  return '未知'
}

function reviewTagType(status, row = {}) {
  const stage = reviewStage(row)
  if (stage === 'manager') return 'warning'
  if (stage === 'teacher') return 'primary'
  if (status === 0) return 'warning'
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  return 'info'
}

async function fetchList() {
  loading.value = true
  try {
    const res = await getPendingCompetitionReviewPage({
      page: pagination.page,
      size: pagination.size,
      title: searchForm.title || undefined
    })
    if (res.code !== 200 || !res.data) {
      throw new Error(res.msg || '获取待审核比赛失败')
    }
    dataList.value = res.data.records || []
    pagination.total = Number(res.data.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '获取待审核比赛失败')
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  searchForm.title = ''
  pagination.page = 1
  fetchList()
}

async function handleApprove(row) {
  const api = reviewStage(row) === 'manager' ? reviewCompetitionByManager : reviewCompetitionByTeacher
  const res = await api(row.id, {
    approved: true,
    reviewComment: ''
  })
  if (res.code === 200) {
    ElMessage.success('比赛审批完成')
    fetchList()
    return
  }
  ElMessage.error(res.msg || '比赛审批失败')
}

function openReject(row) {
  currentRejectRow.value = row
  rejectReason.value = ''
  rejectDialogVisible.value = true
}

async function handleReject() {
  if (!currentRejectRow.value) return
  const api = reviewStage(currentRejectRow.value) === 'manager' ? reviewCompetitionByManager : reviewCompetitionByTeacher
  const res = await api(currentRejectRow.value.id, {
    approved: false,
    reviewComment: rejectReason.value
  })
  if (res.code === 200) {
    ElMessage.success('比赛审批完成')
    rejectDialogVisible.value = false
    currentRejectRow.value = null
    fetchList()
    return
  }
  ElMessage.error(res.msg || '比赛审批失败')
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 16px;
}

.el-pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>
