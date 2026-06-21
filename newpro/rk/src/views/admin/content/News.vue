<template>
  <div class="admin-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>新闻管理</span>
          <el-button type="primary" @click="handleAdd" v-if="canAdd()">
            <el-icon><Plus /></el-icon>发布新闻
          </el-button>
        </div>
      </template>
      
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="新闻标题">
          <el-input v-model="searchForm.title" placeholder="请输入新闻标题" clearable />
        </el-form-item>
        <el-form-item label="新闻分类">
          <el-select v-model="searchForm.category" placeholder="请选择" clearable>
            <el-option label="社团动态" :value="1" />
            <el-option label="校园新闻" :value="2" />
            <el-option label="行业资讯" :value="3" />
            <el-option label="公告通知" :value="4" />
            <el-option label="学术交流" :value="5" />
            <el-option label="培训" :value="6" />
            <el-option label="活动" :value="7" />
            <el-option label="比赛" :value="8" />
            <el-option label="社团活动" :value="9" />
            <el-option label="成果" :value="10" />
            <el-option label="招聘" :value="11" />
            <el-option label="通知" :value="12" />
            <el-option label="社团新闻" :value="13" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.isPublished" placeholder="请选择" clearable>
            <el-option label="草稿" :value="0" />
            <el-option label="已发布" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
      
      <!-- 表格 -->
      <div class="batch-delete-toolbar">
        <el-button v-if="canRemove()" type="danger" plain :disabled="!selectedRows.length" @click="handleBatchDelete">批量删除</el-button>
      </div>

      <el-table :data="dataList" v-loading="loading" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="新闻标题" min-width="250" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="100">
          <template #default="{ row }">
            <el-tag :type="getCategoryInfo(row.category).tag">
              {{ getCategoryInfo(row.category).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="author" label="作者" width="100" />
        <el-table-column prop="viewCount" label="浏览量" width="80" />
        <el-table-column prop="likeCount" label="点赞数" width="80" />
        <el-table-column prop="isFeatured" label="置顶" width="80">
          <template #default="{ row }">
            <el-switch :model-value="Boolean(row.isFeatured)" @change="(value) => handleTopChange(row, value)" />
          </template>
        </el-table-column>
        <el-table-column prop="isPublished" label="状态" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.approvalStatus === 1" type="warning">待审核</el-tag>
            <el-tag v-else-if="row.approvalStatus === 3" type="danger">已拒绝</el-tag>
            <el-tag v-else-if="row.approvalStatus === 2" type="success">已通过</el-tag>
            <el-tag v-else :type="row.isPublished ? 'success' : 'info'">
              {{ row.isPublished ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="跨租户" width="90">
          <template #default="{ row }">
            <el-tag :type="row.isCrossTenant === 1 ? 'success' : 'info'">
              {{ row.isCrossTenant === 1 ? '共享' : '本租户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="250" fixed="right" v-if="canEditNews() || canRemove()">
          <template #default="{ row }">
            <el-button text type="primary" @click="handleEdit(row)" v-if="canEditNews()">编辑</el-button>
            <el-button text type="warning" @click="handleSubmitApproval(row)" v-if="canEditNews() && !row.isPublished && row.approvalStatus !== 1">提交审核</el-button>
            <el-button text type="success" @click="handlePublish(row)" v-if="canEditNews() && !row.isPublished && row.approvalStatus !== 1">发布</el-button>
            <el-button text type="warning" @click="handleOffline(row)" v-if="canEditNews() && row.isPublished">下架</el-button>
            <el-button text type="danger" @click="handleDelete(row)" v-if="canRemove()">删除</el-button>
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="800px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="新闻标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入新闻标题" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="新闻分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类">
            <el-option label="社团动态" :value="1" />
            <el-option label="校园新闻" :value="2" />
            <el-option label="行业资讯" :value="3" />
            <el-option label="公告通知" :value="4" />
            <el-option label="学术交流" :value="5" />
            <el-option label="培训" :value="6" />
            <el-option label="活动" :value="7" />
            <el-option label="比赛" :value="8" />
            <el-option label="社团活动" :value="9" />
            <el-option label="成果" :value="10" />
            <el-option label="招聘" :value="11" />
            <el-option label="通知" :value="12" />
            <el-option label="社团新闻" :value="13" />
          </el-select>
        </el-form-item>
        <el-form-item label="封面图片">
          <el-upload
            class="cover-uploader"
            :show-file-list="false"
            :http-request="handleCoverUpload"
          >
            <img v-if="form.coverImage" :src="resolveMediaUrl(form.coverImage)" class="cover-preview" />
            <el-icon v-else class="cover-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="视频上传">
          <el-upload
            class="video-uploader"
            :show-file-list="false"
            :http-request="handleVideoUpload"
            accept="video/*"
          >
            <video v-if="form.videoUrl" :src="resolveMediaUrl(form.videoUrl)" class="video-preview" controls></video>
            <div v-else class="video-uploader-placeholder">
              <el-icon class="video-uploader-icon"><VideoCamera /></el-icon>
              <div class="el-upload__text">点击上传视频</div>
            </div>
          </el-upload>
          <el-input v-if="form.videoUrl" v-model="form.videoUrl" placeholder="或输入视频URL" style="margin-top: 8px;" />
        </el-form-item>
        <el-form-item label="新闻附件">
          <div class="attachment-field">
            <el-upload
              class="news-attachment-upload"
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
              class="attachment-link"
            >
              查看已上传附件
            </a>
          </div>
        </el-form-item>
        <el-form-item label="新闻摘要">
          <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="请输入新闻摘要" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="新闻内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="请输入新闻内容" />
        </el-form-item>
        <el-form-item label="来源">
          <el-input v-model="form.author" placeholder="请输入新闻来源/作者" />
        </el-form-item>
        <el-form-item label="是否置顶">
          <el-switch v-model="form.isFeatured" />
        </el-form-item>
        <el-form-item label="跨租户公开">
          <el-switch v-model="form.isCrossTenant" />
        </el-form-item>
        <el-form-item label="保存为">
          <el-radio-group v-model="form.isPublished">
            <el-radio :value="false">草稿</el-radio>
            <el-radio :value="true">发布</el-radio>
          </el-radio-group>
        </el-form-item>
        <ReviewAssignmentFields
          v-model:manager-reviewer-id="form.managerReviewerId"
          v-model:teacher-reviewer-id="form.teacherReviewerId"
        />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useBatchDelete } from '@/utils/batchDelete'
import { Plus, VideoCamera } from '@element-plus/icons-vue'
import { getNewsList, getNewsDetail, addNews, updateNews, deleteNews } from '@/api/news'
import { uploadManagedFile } from '@/utils/fileUpload'
import { resolveMediaUrl } from '@/utils/mediaUrl'
import { hasPermission } from '@/utils/permission'
import { useUserStore } from '@/stores/user'
import ReviewAssignmentFields from '@/components/ReviewAssignmentFields.vue'

const userStore = useUserStore()

// Teacher role check - teachers should only view, not edit
const canEdit = computed(() => !userStore.hasRole([8]))

// Permission checks
const canAdd = () => canEdit.value && hasPermission('content:news:add')
const canEditNews = () => canEdit.value && hasPermission('content:news:edit')
const canRemove = () => canEdit.value && hasPermission('content:news:remove')

// 分类ID到名称的映射（用于下拉选择）
const categoryIdMap = {
  1: '社团动态',
  2: '校园新闻',
  3: '行业资讯',
  4: '公告通知',
  5: '学术交流',
  6: '培训',
  7: '活动',
  8: '比赛',
  9: '社团活动',
  10: '成果',
  11: '招聘',
  12: '通知',
  13: '社团新闻'
}

// 分类名称到显示信息的映射（后端返回的分类是字符串名称）
const categoryMap = {
  '社团动态': { label: '社团动态', tag: 'primary' },
  '校园新闻': { label: '校园新闻', tag: 'success' },
  '行业资讯': { label: '行业资讯', tag: 'warning' },
  '公告通知': { label: '公告通知', tag: 'info' },
  '学术交流': { label: '学术交流', tag: 'primary' },
  '培训': { label: '培训', tag: 'success' },
  '活动': { label: '活动', tag: 'primary' },
  '比赛': { label: '比赛', tag: 'warning' },
  '社团活动': { label: '社团活动', tag: 'primary' },
  '成果': { label: '成果', tag: 'success' },
  '招聘': { label: '招聘', tag: 'warning' },
  '通知': { label: '通知', tag: 'info' },
  '社团新闻': { label: '社团新闻', tag: 'success' },
  'tech': { label: '技术', tag: 'primary' },
  'notice': { label: '通知', tag: 'info' }
}

// 获取分类显示信息的辅助函数（未知分类直接显示原值）
const getCategoryInfo = (category) => {
  return categoryMap[category] || { label: category || '未知', tag: 'info' }
}

// 分类名称到ID的反向映射（编辑时将后端返回的名称转为前端下拉的数字值）
const categoryNameToId = {
  '社团动态': 1,
  '校园新闻': 2,
  '行业资讯': 3,
  '公告通知': 4,
  '学术交流': 5,
  '培训': 6,
  '活动': 7,
  '比赛': 8,
  '社团活动': 9,
  '成果': 10,
  '招聘': 11,
  '通知': 12,
  '社团新闻': 13
}

const loading = ref(false)
const submitting = ref(false)
const dataList = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('发布新闻')
const formRef = ref()

const searchForm = reactive({
  title: '',
  category: '',
  isPublished: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const form = reactive({
  id: null,
  title: '',
  category: 1,
  coverImage: '',
  videoUrl: '',
  attachmentUrl: '',
  summary: '',
  content: '',
  author: '',
  isFeatured: false,
  isCrossTenant: false,
  isPublished: false,
  managerReviewerId: null,
  teacherReviewerId: null
})

const rules = {
  title: [{ required: true, message: '请输入新闻标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  content: [{ required: true, message: '请输入新闻内容', trigger: 'blur' }]
}

rules.managerReviewerId = [{ required: true, message: '请选择负责人审批人', trigger: 'change' }]
rules.teacherReviewerId = [{ required: true, message: '请选择指导老师审批人', trigger: 'change' }]

const fetchList = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      ...searchForm
    }

    // 转换分类ID为分类名称（后端存储的是字符串名称）
    if (params.category && categoryIdMap[params.category]) {
      params.category = categoryIdMap[params.category]
    }

    const res = await getNewsList(params)
    if (res.code === 200) {
      dataList.value = res.data?.records || res.data || []
      pagination.total = res.data?.total || dataList.value.length
    } else {
      ElMessage.error(res.msg || '获取新闻列表失败')
    }
  } catch (e) {
    console.error('获取新闻列表失败:', e)
    ElMessage.error('获取新闻列表失败')
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.title = ''
  searchForm.category = ''
  searchForm.isPublished = ''
  pagination.page = 1
  fetchList()
}

const handleAdd = () => {
  dialogTitle.value = '发布新闻'
  Object.assign(form, { id: null, title: '', category: 1, coverImage: '', videoUrl: '', attachmentUrl: '', summary: '', content: '', author: '', isFeatured: false, isPublished: false })
  form.isCrossTenant = false
  form.managerReviewerId = null
  form.teacherReviewerId = null
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  dialogTitle.value = '编辑新闻'
  try {
    const res = await getNewsDetail(row.id)
    if (res.code === 200) {
      const data = res.data
      Object.assign(form, {
        id: data.id,
        title: data.title,
        category: categoryNameToId[data.category] || data.category || 1,
        coverImage: data.coverImage,
        videoUrl: data.videoUrl || '',
        attachmentUrl: data.attachmentUrl || '',
        summary: data.summary,
        content: data.content,
        author: data.author,
        isFeatured: data.isFeatured,
        isCrossTenant: data.isCrossTenant === 1,
        isPublished: data.isPublished,
        managerReviewerId: data.managerReviewerId || null,
        teacherReviewerId: data.teacherReviewerId || null
      })
      dialogVisible.value = true
    }
  } catch (e) {
    ElMessage.error('获取新闻详情失败')
  }
}

const handleSubmitApproval = async (row) => {
  await ElMessageBox.confirm('确定提交审核吗？提交后将交由指导老师审核。', '提交审核', { type: 'warning' })
  try {
    const res = await updateNews({ id: row.id, approvalStatus: 1 })
    if (res.code === 200) {
      ElMessage.success('已提交审核')
      fetchList()
    } else {
      ElMessage.error(res.msg || '提交审核失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('提交审核失败')
    }
  }
}

const handlePublish = async (row) => {
  await ElMessageBox.confirm('确定发布该新闻吗？', '提示', { type: 'success' })
  try {
    const res = await updateNews({ id: row.id, isPublished: true })
    if (res.code === 200) {
      ElMessage.success('发布成功')
      fetchList()
    } else {
      ElMessage.error(res.msg || '发布失败')
    }
  } catch (e) {
    ElMessage.error('发布失败')
  }
}

const handleOffline = async (row) => {
  await ElMessageBox.confirm('确定下架该新闻吗？', '提示', { type: 'warning' })
  try {
    const res = await updateNews({ id: row.id, isPublished: false })
    if (res.code === 200) {
      ElMessage.success('下架成功')
      fetchList()
    } else {
      ElMessage.error(res.msg || '下架失败')
    }
  } catch (e) {
    ElMessage.error('下架失败')
  }
}

const handleTopChange = async (row, nextValue) => {
  const oldValue = row.isFeatured
  row.isFeatured = nextValue ? 1 : 0
  try {
    const res = await updateNews({ id: row.id, isFeatured: row.isFeatured })
    if (res.code === 200) {
      ElMessage.success(row.isFeatured ? '已置顶' : '取消置顶')
    } else {
      row.isFeatured = oldValue
      // Response interceptor already shows error toast, no need for duplicate
    }
  } catch (e) {
    row.isFeatured = oldValue
    // Response interceptor already shows error toast, no need for duplicate
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定要删除该新闻吗？', '提示', { type: 'warning' })
  try {
    const res = await deleteNews(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchList()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const handleCoverUpload = async (options) => {
  try {
    const { storedValue, url } = await uploadManagedFile(options, 'news-image')
    form.coverImage = storedValue
    options.onSuccess?.({ code: 200, data: { path: storedValue, url } })
    ElMessage.success('图片上传成功')
  } catch (error) {
    options.onError?.(error)
    ElMessage.error(error.message || '图片上传失败')
  }
}

const handleVideoUpload = async (options) => {
  try {
    const { storedValue, url } = await uploadManagedFile(options, 'news-video')
    form.videoUrl = storedValue
    options.onSuccess?.({ code: 200, data: { path: storedValue, url } })
    ElMessage.success('视频上传成功')
  } catch (error) {
    options.onError?.(error)
    ElMessage.error(error.message || '视频上传失败')
  }
}

const handleAttachmentUpload = async (options) => {
  try {
    const { storedValue, url } = await uploadManagedFile(options, 'news-attachment')
    form.attachmentUrl = storedValue
    options.onSuccess?.({ code: 200, data: { path: storedValue, url } })
    ElMessage.success('附件上传成功')
  } catch (error) {
    options.onError?.(error)
    ElMessage.error(error.message || '附件上传失败')
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  
  submitting.value = true
  try {
    // 转换类型以匹配后端DTO（后端category字段为String类型，存储分类名称）
    const data = {
      title: form.title,
      category: categoryIdMap[form.category] || form.category || '社团动态',
      coverImage: form.coverImage,
      videoUrl: form.videoUrl,
      attachmentUrl: form.attachmentUrl,
      summary: form.summary,
      content: form.content,
      author: form.author,
      isFeatured: form.isFeatured ? 1 : 0,
      isCrossTenant: form.isCrossTenant ? 1 : 0,
      isPublished: form.isPublished ? 1 : 0,
      managerReviewerId: form.managerReviewerId,
      teacherReviewerId: form.teacherReviewerId
    }
    
    let res
    if (form.id) {
      res = await updateNews({ ...data, id: form.id })
    } else {
      res = await addNews(data)
    }
    
    if (res.code === 200) {
      ElMessage.success(form.id ? '修改成功' : '发布成功')
      dialogVisible.value = false
      fetchList()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (e) {
    console.error('提交失败:', e)
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}


const { selectedRows, handleSelectionChange, handleBatchDelete } = useBatchDelete({
  entityName: '新闻',
  deleteItem: (id) => deleteNews(id),
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

  .cover-uploader {
    :deep(.el-upload) {
      border: 1px dashed #d9d9d9;
      border-radius: 6px;
      cursor: pointer;
      position: relative;
      overflow: hidden;
      
      &:hover {
        border-color: #409eff;
      }
    }
    
    .cover-preview {
      width: 178px;
      height: 100px;
      display: block;
      object-fit: cover;
    }
    
    .cover-uploader-icon {
      font-size: 28px;
      color: #8c939d;
      width: 178px;
      height: 100px;
      text-align: center;
      line-height: 100px;
    }
  }

  .video-uploader {
    :deep(.el-upload) {
      border: 1px dashed #d9d9d9;
      border-radius: 6px;
      cursor: pointer;
      position: relative;
      overflow: hidden;

      &:hover {
        border-color: #409eff;
      }
    }

    .video-preview {
      width: 320px;
      height: 180px;
      display: block;
      object-fit: contain;
      background: #f5f5f5;
    }

    .video-uploader-placeholder {
      width: 320px;
      height: 180px;
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      background: #f5f5f5;
    }

    .video-uploader-icon {
      font-size: 48px;
      color: #8c939d;
    }
  }

  .attachment-field {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
  }

  .attachment-link {
    color: #409eff;
    text-decoration: none;
  }

  .attachment-link:hover {
    text-decoration: underline;
  }
}
</style>
