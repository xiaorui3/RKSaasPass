<template>
  <div class="admin-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>活动审批</span>
        </div>
      </template>
      
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="活动名称">
          <el-input v-model="searchForm.title" placeholder="请输入活动名称" clearable />
        </el-form-item>
        <el-form-item label="审批状态">
          <el-select v-model="searchForm.approvalStatus" placeholder="请选择" clearable>
            <el-option label="负责人待审" value="manager" />
            <el-option label="指导老师待审" value="teacher" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请人">
          <el-input v-model="searchForm.applicant" placeholder="请输入申请人" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
      
      <!-- 表格 -->
      <el-table :data="dataList" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="活动名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="type" label="活动类型" width="100">
          <template #default="{ row }">
            <el-tag :type="typeMap[row.type]?.tag || 'info'">
              {{ typeMap[row.type]?.label || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applicantName" label="申请人" width="100" />
        <el-table-column prop="startTime" label="开始时间" width="160" />
        <el-table-column prop="endTime" label="结束时间" width="160" />
        <el-table-column prop="location" label="活动地点" width="120" show-overflow-tooltip />
        <el-table-column prop="approvalStatus" label="审批状态" width="120">
          <template #default="{ row }">
            <el-tag :type="approvalStatusMap[row.approvalStatus]?.tag || 'info'">
              {{ approvalStatusMap[row.approvalStatus]?.label || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="handleView(row)">查看</el-button>
            <el-button text type="success" @click="handleApprove(row)" v-if="row.canReview">通过</el-button>
            <el-button text type="danger" @click="handleReject(row)" v-if="row.canReview">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @change="fetchList"
      />
    </el-card>
    
    <!-- 活动详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="活动详情" width="700px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="活动名称" :span="2">{{ currentActivity.title }}</el-descriptions-item>
        <el-descriptions-item label="活动类型">{{ typeMap[currentActivity.type]?.label }}</el-descriptions-item>
        <el-descriptions-item label="活动地点">{{ currentActivity.location }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ currentActivity.startTime }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ currentActivity.endTime }}</el-descriptions-item>
        <el-descriptions-item label="人数上限">{{ currentActivity.maxParticipants }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ currentActivity.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="申请时间" :span="2">{{ currentActivity.createTime }}</el-descriptions-item>
        <el-descriptions-item label="活动描述" :span="2">{{ currentActivity.description }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button type="success" @click="handleApprove(currentActivity)" v-if="currentActivity.canReview">通过</el-button>
        <el-button type="danger" @click="handleReject(currentActivity)" v-if="currentActivity.canReview">拒绝</el-button>
      </template>
    </el-dialog>

    <!-- 拒绝原因对话框 -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝原因" width="400px">
      <el-input
        v-model="rejectReason"
        type="textarea"
        :rows="4"
        placeholder="请输入拒绝原因"
      />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPendingReviewActivities, reviewActivityByManager, reviewActivityByTeacher } from '@/api/activity'
import { useUserStore } from '@/stores/user'
const typeMap = {
  1: { label: '普通活动', tag: 'primary' },
  2: { label: '比赛活动', tag: 'warning' },
  3: { label: '培训活动', tag: 'success' }
}

const approvalStatusMap = {
  manager: { label: '负责人待审', tag: 'warning' },
  teacher: { label: '指导老师待审', tag: 'primary' },
  approved: { label: '已通过', tag: 'success' },
  rejected: { label: '已拒绝', tag: 'danger' }
}

const userStore = useUserStore()
const currentRoleId = computed(() => userStore.roleId)

const loading = ref(false)
const dataList = ref([])
const detailDialogVisible = ref(false)
const rejectDialogVisible = ref(false)
const rejectReason = ref('')
const currentActivity = ref({})
const currentRejectId = ref(null)

const searchForm = reactive({
  title: '',
  approvalStatus: '',
  applicant: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const fetchList = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      title: searchForm.title || undefined,
      applicant: searchForm.applicant || undefined
    }
    const res = await getPendingReviewActivities(params)
    if (res.code === 200 && res.data) {
      const records = res.data.records || res.data || []
      let mapped = records.map(item => ({
        id: item.id,
        title: item.activityName || item.title,
        type: item.activityType || item.type,
        applicantName: item.organizer || '-',
        startTime: item.startTime,
        endTime: item.endTime,
        location: item.location,
        maxParticipants: item.maxParticipants,
        approvalStatus: item.managerReviewStatus === 0 ? 'manager'
          : item.teacherReviewStatus === 0 ? 'teacher'
          : item.teacherReviewStatus === 2 || item.managerReviewStatus === 2 ? 'rejected'
          : 'approved',
        canReview: item.managerReviewStatus === 0 || item.teacherReviewStatus === 0,
        createTime: item.createTime,
        description: item.content || item.description,
        managerReviewStatus: item.managerReviewStatus,
        teacherReviewStatus: item.teacherReviewStatus
      }))
      if (searchForm.approvalStatus) {
        mapped = mapped.filter(item => item.approvalStatus === searchForm.approvalStatus)
      }
      dataList.value = mapped
      pagination.total = res.data.total || mapped.length
    } else {
      dataList.value = []
      pagination.total = 0
    }
  } catch (e) {
    ElMessage.error('获取审批列表失败')
    dataList.value = []
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.title = ''
  searchForm.approvalStatus = ''
  searchForm.applicant = ''
  fetchList()
}

const handleView = (row) => {
  currentActivity.value = { ...row }
  detailDialogVisible.value = true
}

const handleApprove = async (row) => {
  await ElMessageBox.confirm('确定通过该活动申请吗？', '提示', { type: 'success' })
  try {
    const api = row.approvalStatus === 'manager' ? reviewActivityByManager : reviewActivityByTeacher
    const res = await api(row.id, { approved: true, reviewComment: '' })
    if (res.code === 200) {
      ElMessage.success('审批通过')
      detailDialogVisible.value = false
      fetchList()
    } else {
      ElMessage.error(res.msg || '审批失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('审批失败')
    }
  }
}

const handleReject = (row) => {
  currentRejectId.value = row.id
  rejectReason.value = ''
  rejectDialogVisible.value = true
}

const confirmReject = async () => {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }
  try {
    const row = dataList.value.find(item => item.id === currentRejectId.value) || currentActivity.value
    const api = row.approvalStatus === 'manager' ? reviewActivityByManager : reviewActivityByTeacher
    const res = await api(currentRejectId.value, { approved: false, reviewComment: rejectReason.value })
    if (res.code === 200) {
      ElMessage.success('已拒绝')
      rejectDialogVisible.value = false
      detailDialogVisible.value = false
      fetchList()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style lang="scss" scoped>
.admin-page {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .search-form {
    margin-bottom: 20px;
  }
  
  .el-pagination {
    margin-top: 20px;
    justify-content: flex-end;
  }
}
</style>
