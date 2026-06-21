<template>
  <div class="admin-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <div>
            <h2>志愿服务审核</h2>
            <p>审核志愿服务记录并同步当前租户的志愿服务时长。</p>
          </div>
        </div>
      </template>

      <div class="status-tabs">
        <el-radio-group v-model="currentStatus" @change="handleStatusChange">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="0">待审核</el-radio-button>
          <el-radio-button label="1">已通过</el-radio-button>
          <el-radio-button label="2">已驳回</el-radio-button>
        </el-radio-group>
      </div>

      <el-table :data="dataList" v-loading="loading" stripe empty-text="暂无志愿服务记录">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="userId" label="申请人 ID" width="100" />
        <el-table-column prop="title" label="服务标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="hours" label="服务时长(h)" width="110" />
        <el-table-column prop="serviceDate" label="服务日期" width="120" />
        <el-table-column label="认证图片" width="100">
          <template #default="{ row }">
            <el-image
              v-if="row.certImageUrl"
              :src="row.certImageUrl"
              :preview-src-list="[row.certImageUrl]"
              fit="cover"
              style="width: 60px; height: 60px; border-radius: 4px; cursor: pointer;"
              preview-teleported
            />
            <span v-else class="text-muted">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.tag || 'info'">
              {{ statusMap[row.status]?.label || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button text type="success" @click="handleApprove(row)">通过</el-button>
              <el-button text type="danger" @click="handleReject(row)">驳回</el-button>
            </template>
            <span v-else class="text-muted">已处理</span>
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

    <el-dialog v-model="rejectDialogVisible" title="驳回原因" width="400px" :close-on-click-modal="false">
      <el-input
        v-model="rejectReason"
        type="textarea"
        :rows="4"
        placeholder="请输入驳回原因"
        maxlength="200"
        show-word-limit
      />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getVolunteerRecords, approveVolunteerRecord, rejectVolunteerRecord } from '@/api/volunteer'

const statusMap = {
  0: { label: '待审核', tag: 'warning' },
  1: { label: '已通过', tag: 'success' },
  2: { label: '已驳回', tag: 'danger' }
}

const loading = ref(false)
const dataList = ref([])
const currentStatus = ref('')

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const rejectDialogVisible = ref(false)
const rejectReason = ref('')
const currentRejectId = ref(null)

const fetchList = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size
    }
    if (currentStatus.value !== '') {
      params.status = Number(currentStatus.value)
    }
    const res = await getVolunteerRecords(params)
    if (res.code === 200 && res.data) {
      const records = res.data.records || res.data || []
      dataList.value = records
      pagination.total = res.data.total || records.length
    } else {
      dataList.value = []
      pagination.total = 0
    }
  } catch (error) {
    console.error('获取志愿记录失败:', error)
    ElMessage.error('获取志愿记录失败')
    dataList.value = []
  } finally {
    loading.value = false
  }
}

const handleStatusChange = () => {
  pagination.page = 1
  fetchList()
}

const handleApprove = async (row) => {
  await ElMessageBox.confirm('确定通过该志愿服务记录吗？', '提示', { type: 'success' })
  try {
    const res = await approveVolunteerRecord(row.id)
    if (res.code === 200) {
      ElMessage.success('审核通过')
      fetchList()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
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
    ElMessage.warning('请输入驳回原因')
    return
  }
  try {
    const res = await rejectVolunteerRecord(currentRejectId.value, rejectReason.value)
    if (res.code === 200) {
      ElMessage.success('已驳回')
      rejectDialogVisible.value = false
      fetchList()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch {
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style lang="scss" scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.card-header h2 {
  margin: 0;
  font-size: 20px;
}

.card-header p {
  margin: 8px 0 0;
  color: #909399;
}

.status-tabs {
  margin-bottom: 20px;
}

.text-muted {
  color: #c0c4cc;
  font-size: 13px;
}

.el-pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>
