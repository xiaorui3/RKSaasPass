<template>
  <div class="admin-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>公告管理</span>
          <el-button type="primary" @click="handleAdd" v-if="canEdit">
            <el-icon><Plus /></el-icon>发布公告
          </el-button>
        </div>
      </template>
      
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="公告标题">
          <el-input v-model="searchForm.title" placeholder="请输入公告标题" clearable />
        </el-form-item>
        <el-form-item label="公告类型">
          <el-select v-model="searchForm.type" placeholder="请选择" clearable>
            <el-option label="系统公告" :value="1" />
            <el-option label="活动通知" :value="2" />
            <el-option label="比赛通知" :value="3" />
            <el-option label="其他通知" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable>
            <el-option label="草稿" :value="0" />
            <el-option label="已发布" :value="1" />
            <el-option label="已撤回" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
      
      <!-- 表格 -->
      <div class="batch-delete-toolbar">
        <el-button v-if="canEdit" type="danger" plain :disabled="!selectedRows.length" @click="handleBatchDelete">批量删除</el-button>
      </div>

      <el-table :data="dataList" v-loading="loading" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="公告标题" min-width="250" show-overflow-tooltip />
        <el-table-column prop="type" label="公告类型" width="100">
          <template #default="{ row }">
            <el-tag :type="typeMap[row.type]?.tag || 'info'">
              {{ typeMap[row.type]?.label || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publisherName" label="发布人" width="100" />
        <el-table-column prop="isTop" label="置顶" width="80">
          <template #default="{ row }">
            <el-switch v-model="row.isTop" @change="handleTopChange(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="160" />
        <el-table-column prop="expireTime" label="过期时间" width="160" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.tag || 'info'">
              {{ statusMap[row.status]?.label || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="handleEdit(row)" v-if="canEdit">编辑</el-button>
            <el-button text type="success" @click="handlePublish(row)" v-if="canEdit && row.status === 0">发布</el-button>
            <el-button text type="warning" @click="handleWithdraw(row)" v-if="canEdit && row.status === 1">撤回</el-button>
            <el-button text type="danger" @click="handleDelete(row)" v-if="canEdit">删除</el-button>
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
    
    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="公告类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择公告类型">
            <el-option label="系统公告" :value="1" />
            <el-option label="活动通知" :value="2" />
            <el-option label="比赛通知" :value="3" />
            <el-option label="其他通知" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="公告内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入公告内容" />
        </el-form-item>
        <el-form-item label="封面图片">
          <div class="notice-media-field">
            <el-upload
              class="notice-cover-upload"
              :show-file-list="false"
              :http-request="handleCoverUpload"
            >
              <el-button type="primary" plain>上传封面</el-button>
            </el-upload>
            <img v-if="form.coverImage" :src="resolveMediaUrl(form.coverImage)" class="notice-cover-preview" />
          </div>
        </el-form-item>
        <el-form-item label="公告附件">
          <div class="notice-media-field">
            <el-upload
              class="notice-attachment-upload"
              :show-file-list="false"
              :http-request="handleAttachmentUpload"
            >
              <el-button type="primary" plain>上传附件</el-button>
            </el-upload>
            <a
              v-if="form.attachmentUrl"
              :href="resolveMediaUrl(form.attachmentUrl)"
              target="_blank"
              rel="noopener noreferrer"
              class="notice-attachment-link"
            >
              查看已上传附件
            </a>
          </div>
        </el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker
            v-model="form.expireTime"
            type="datetime"
            placeholder="选择过期时间"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="是否置顶">
          <el-switch v-model="form.isTop" />
        </el-form-item>
        <el-form-item label="目标用户">
          <el-checkbox-group v-model="form.targetUsers">
            <el-checkbox :value="1">全部用户</el-checkbox>
            <el-checkbox :value="2">学生</el-checkbox>
            <el-checkbox :value="3">教师</el-checkbox>
            <el-checkbox :value="4">管理员</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <ReviewAssignmentFields
          v-model:manager-reviewer-id="form.managerReviewerId"
          v-model:teacher-reviewer-id="form.teacherReviewerId"
        />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button @click="handleSaveDraft" :loading="submitLoading" v-if="!form.id">保存草稿</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useBatchDelete } from '@/utils/batchDelete'
import { Plus } from '@element-plus/icons-vue'
import { getNoticeList, createNotice, updateNotice, deleteNotice, publishNotice, withdrawNotice, updateNoticeTop } from '@/api/notice'
import { uploadManagedFile } from '@/utils/fileUpload'
import { resolveMediaUrl } from '@/utils/mediaUrl'
import { useUserStore } from '@/stores/user'
import ReviewAssignmentFields from '@/components/ReviewAssignmentFields.vue'

const userStore = useUserStore()

const canEdit = computed(() => userStore.hasRole([1, 7, 8]))

const typeMap = {
  1: { label: '系统公告', tag: 'danger' },
  2: { label: '活动通知', tag: 'primary' },
  3: { label: '比赛通知', tag: 'warning' },
  4: { label: '其他通知', tag: 'info' }
}

const statusMap = {
  0: { label: '草稿', tag: 'info' },
  1: { label: '已发布', tag: 'success' },
  2: { label: '已撤回', tag: 'warning' }
}

// 公告类型映射: 后端noticeType整数 -> 前端数字（现在后端也用整数）
const noticeTypeMap = {
  1: 1,
  2: 2,
  3: 3,
  4: 4
}

// 后端类型整数 -> 前端数字（1:1映射）
const noticeTypeReverseMap = {
  1: 1,
  2: 2,
  3: 3,
  4: 4
}

const loading = ref(false)
const submitLoading = ref(false)
const dataList = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('发布公告')
const formRef = ref()

const searchForm = reactive({
  title: '',
  type: '',
  status: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const form = reactive({
  id: null,
  title: '',
  type: 1,
  content: '',
  coverImage: '',
  attachmentUrl: '',
  expireTime: '',
  isTop: false,
  targetUsers: [1],
  managerReviewerId: null,
  teacherReviewerId: null
})

const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  type: [{ required: true, message: '请选择公告类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

rules.managerReviewerId = [{ required: true, message: '请选择负责人审批人', trigger: 'change' }]
rules.teacherReviewerId = [{ required: true, message: '请选择指导老师审批人', trigger: 'change' }]

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getNoticeList()
    if (res.code === 200 && res.data) {
      // 将后端数据映射为前端格式
      dataList.value = (res.data || []).map(item => ({
        id: item.id,
        title: item.title || '',
        type: noticeTypeReverseMap[item.noticeType] || 4,
        publisherName: item.creator || '管理员',
        isTop: item.isTop === 1,
        publishTime: item.publishTime || item.createTime || '',
        expireTime: '',
        status: item.isPublished ? 1 : 0,
        content: item.content || '',
        coverImage: item.coverImage || '',
        attachmentUrl: item.attachmentUrl || '',
        managerReviewerId: item.managerReviewerId || null,
        teacherReviewerId: item.teacherReviewerId || null
      }))
      pagination.total = dataList.value.length
    } else {
      // API返回异常时使用空列表
      dataList.value = []
      pagination.total = 0
    }
  } catch (e) {
    console.error('获取公告列表失败:', e)
    dataList.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.title = ''
  searchForm.type = ''
  searchForm.status = ''
  fetchList()
}

const handleAdd = () => {
  dialogTitle.value = '发布公告'
  Object.assign(form, { id: null, title: '', type: 1, content: '', coverImage: '', attachmentUrl: '', expireTime: '', isTop: false, targetUsers: [1] })
  form.managerReviewerId = null
  form.teacherReviewerId = null
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑公告'
  Object.assign(form, {
    id: row.id,
    title: row.title,
    type: row.type,
    content: row.content,
    coverImage: row.coverImage || '',
    attachmentUrl: row.attachmentUrl || '',
    expireTime: row.expireTime,
    isTop: row.isTop,
    targetUsers: row.targetUsers || [1],
    managerReviewerId: row.managerReviewerId || null,
    teacherReviewerId: row.teacherReviewerId || null
  })
  dialogVisible.value = true
}

const handleCoverUpload = async (options) => {
  try {
    const { storedValue, url } = await uploadManagedFile(options, 'notice-cover')
    form.coverImage = storedValue
    options.onSuccess?.({ code: 200, data: { path: storedValue, url } })
    ElMessage.success('封面上传成功')
  } catch (error) {
    options.onError?.(error)
    ElMessage.error(error.message || '封面上传失败')
  }
}

const handleAttachmentUpload = async (options) => {
  try {
    const { storedValue, url } = await uploadManagedFile(options, 'notice-attachment')
    form.attachmentUrl = storedValue
    options.onSuccess?.({ code: 200, data: { path: storedValue, url } })
    ElMessage.success('附件上传成功')
  } catch (error) {
    options.onError?.(error)
    ElMessage.error(error.message || '附件上传失败')
  }
}

const handlePublish = async (row) => {
  try {
    await ElMessageBox.confirm('确定发布该公告吗？', '提示', { type: 'success' })
    const res = await publishNotice(row.id)
    if (res.code === 200) {
      ElMessage.success('发布成功')
      fetchList()
    } else {
      ElMessage.error(res.msg || '发布失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('发布失败')
    }
  }
}

const handleWithdraw = async (row) => {
  try {
    await ElMessageBox.confirm('确定撤回该公告吗？', '提示', { type: 'warning' })
    const res = await withdrawNotice(row.id)
    if (res.code === 200) {
      ElMessage.success('撤回成功')
      fetchList()
    } else {
      ElMessage.error(res.msg || '撤回失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('撤回失败')
    }
  }
}

const handleTopChange = async (row) => {
  const nextValue = row.isTop
  try {
    const res = await updateNoticeTop(row.id, nextValue)
    if (res.code === 200) {
      ElMessage.success(nextValue ? '已置顶' : '取消置顶')
    } else {
      row.isTop = !nextValue
      ElMessage.error(res.msg || '置顶状态更新失败')
    }
  } catch (e) {
    row.isTop = !nextValue
    ElMessage.error('置顶状态更新失败')
  }
}

const handleSaveDraft = async () => {
  try {
    await formRef.value.validate()
    submitLoading.value = true
    const data = {
      title: form.title,
      content: form.content,
      coverImage: form.coverImage,
      attachmentUrl: form.attachmentUrl,
      noticeType: form.type || 4,
      isTop: form.isTop ? 1 : 0,
      isPublished: false,
      managerReviewerId: form.managerReviewerId,
      teacherReviewerId: form.teacherReviewerId
    }
    if (form.id) {
      data.id = form.id
    }
    const res = form.id ? await updateNotice(data) : await createNotice(data)
    if (res.code === 200) {
      ElMessage.success('保存草稿成功')
      dialogVisible.value = false
      fetchList()
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } catch (e) {
    // validate failed or API error
    if (e && e.message) {
      ElMessage.error('保存失败')
    }
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该公告吗？', '提示', { type: 'warning' })
    const res = await deleteNotice(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchList()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    submitLoading.value = true
    const data = {
      title: form.title,
      content: form.content,
      coverImage: form.coverImage,
      attachmentUrl: form.attachmentUrl,
      noticeType: form.type || 4,
      isTop: form.isTop ? 1 : 0,
      isPublished: true,
      managerReviewerId: form.managerReviewerId,
      teacherReviewerId: form.teacherReviewerId
    }
    if (form.id) {
      data.id = form.id
    }
    const res = form.id ? await updateNotice(data) : await createNotice(data)
    if (res.code === 200) {
      ElMessage.success(form.id ? '修改成功' : '发布成功')
      dialogVisible.value = false
      fetchList()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (e) {
    if (e && e.message) {
      ElMessage.error('操作失败')
    }
  } finally {
    submitLoading.value = false
  }
}


const { selectedRows, handleSelectionChange, handleBatchDelete } = useBatchDelete({
  entityName: '公告',
  deleteItem: (id) => deleteNotice(id),
  fetchList: fetchList
})

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

  .notice-media-field {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
  }

  .notice-cover-preview {
    width: 120px;
    height: 80px;
    object-fit: cover;
    border-radius: 6px;
    border: 1px solid #ebeef5;
  }

  .notice-attachment-link {
    color: #409eff;
    text-decoration: none;
  }

  .notice-attachment-link:hover {
    text-decoration: underline;
  }
}
</style>
