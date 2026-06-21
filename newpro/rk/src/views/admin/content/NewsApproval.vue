<template>
  <div class="admin-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>新闻审核</span>
        </div>
      </template>
      
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="新闻标题">
          <el-input v-model="searchForm.title" placeholder="请输入新闻标题" clearable />
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="searchForm.approvalStatus" placeholder="请选择" clearable>
            <el-option label="待审核" :value="1" />
            <el-option label="已通过" :value="2" />
            <el-option label="已拒绝" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="提交人">
          <el-input v-model="searchForm.author" placeholder="请输入提交人" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
      
      <!-- 表格 -->
      <el-table :data="dataList" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="新闻标题" min-width="250" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="100">
          <template #default="{ row }">
            <el-tag :type="categoryMap[row.category]?.tag || 'info'">
              {{ categoryMap[row.category]?.label || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="author" label="提交人" width="100" />
        <el-table-column prop="summary" label="摘要" width="200" show-overflow-tooltip />
        <el-table-column prop="approvalStatus" label="审核状态" width="100">
          <template #default="{ row }">
            <el-tag :type="approvalStatusMap[row.approvalStatus]?.tag || 'info'">
              {{ approvalStatusMap[row.approvalStatus]?.label || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="160" />
        <el-table-column prop="approverName" label="审核人" width="100" />
        <el-table-column prop="approvalTime" label="审核时间" width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="handleView(row)">查看</el-button>
            <el-button text type="success" @click="handleApprove(row)" v-if="row.approvalStatus === 1">通过</el-button>
            <el-button text type="danger" @click="handleReject(row)" v-if="row.approvalStatus === 1">拒绝</el-button>
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
    
    <!-- 新闻详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="新闻详情" width="800px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="新闻标题" :span="2">{{ currentNews.title }}</el-descriptions-item>
        <el-descriptions-item label="新闻分类">{{ categoryMap[currentNews.category]?.label }}</el-descriptions-item>
        <el-descriptions-item label="提交人">{{ currentNews.author }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{ currentNews.source || '无' }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ currentNews.createTime }}</el-descriptions-item>
        <el-descriptions-item label="新闻摘要" :span="2">{{ currentNews.summary }}</el-descriptions-item>
        <el-descriptions-item label="新闻内容" :span="2">
          <div class="news-content" v-html="currentNews.content"></div>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button type="success" @click="handleApprove(currentNews)" v-if="currentNews.approvalStatus === 1">通过</el-button>
        <el-button type="danger" @click="handleReject(currentNews)" v-if="currentNews.approvalStatus === 1">拒绝</el-button>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPendingNews, rejectNews, reviewNewsByManager, reviewNewsByTeacher } from '@/api/news'

const categoryMap = {
  1: { label: '社团动态', tag: 'primary' },
  2: { label: '校园新闻', tag: 'success' },
  3: { label: '行业资讯', tag: 'warning' },
  4: { label: '公告通知', tag: 'info' }
}

const approvalStatusMap = {
  0: { label: '草稿', tag: 'info' },
  1: { label: '待审核', tag: 'warning' },
  2: { label: '已通过', tag: 'success' },
  3: { label: '已拒绝', tag: 'danger' }
}

const reviewStage = (row) => {
  if (row.managerReviewStatus === 0) return 'manager'
  if (row.teacherReviewStatus === 0) return 'teacher'
  return 'approved'
}

const loading = ref(false)
const dataList = ref([])
const detailDialogVisible = ref(false)
const rejectDialogVisible = ref(false)
const rejectReason = ref('')
const currentNews = ref({})
const currentRejectId = ref(null)

const searchForm = reactive({
  title: '',
  approvalStatus: '',
  author: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getPendingNews()
    if (res.code === 200) {
      dataList.value = res.data || []
      pagination.total = dataList.value.length
    } else {
      ElMessage.error(res.msg || '获取审核列表失败')
    }
  } catch (e) {
    console.error('获取审核列表失败:', e)
    ElMessage.error('获取审核列表失败: ' + (e.response?.data?.msg || e.message))
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.title = ''
  searchForm.approvalStatus = ''
  searchForm.author = ''
  fetchList()
}

const handleView = (row) => {
  currentNews.value = { ...row }
  detailDialogVisible.value = true
}

const handleApprove = async (row) => {
  await ElMessageBox.confirm('确定通过该新闻吗？', '提示', { type: 'success' })
  try {
    const api = reviewStage(row) === 'manager' ? reviewNewsByManager : reviewNewsByTeacher
    const res = await api(row.id, { id: row.id, remark: '' })
    if (res.code === 200) {
      ElMessage.success('审核通过')
      detailDialogVisible.value = false
      fetchList()
    } else {
      ElMessage.error(res.msg || '审核失败')
    }
  } catch (e) {
    console.error('审核失败:', e)
    ElMessage.error('审核失败: ' + (e.response?.data?.msg || e.message))
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
    const res = await rejectNews({ id: currentRejectId.value, rejectReason: rejectReason.value })
    if (res.code === 200) {
      ElMessage.success('已拒绝')
      rejectDialogVisible.value = false
      detailDialogVisible.value = false
      fetchList()
    } else {
      ElMessage.error(res.msg || '拒绝失败')
    }
  } catch (e) {
    console.error('拒绝失败:', e)
    ElMessage.error('拒绝失败: ' + (e.response?.data?.msg || e.message))
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

  .news-content {
    max-height: 300px;
    overflow-y: auto;
    padding: 10px;
    background: #f5f7fa;
    border-radius: 4px;
  }
}
</style>
