<template>
  <div class="admin-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>作品管理</span>
          <el-button v-if="canEdit && hasPermission('content:works:add')" type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增作品
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="作品名称">
          <el-input v-model="searchForm.title" placeholder="请输入作品名称" clearable />
        </el-form-item>
        <el-form-item label="作品类型">
          <el-select v-model="searchForm.type" placeholder="请选择" clearable>
            <el-option label="软件项目" value="软件项目" />
            <el-option label="硬件项目" value="硬件项目" />
            <el-option label="设计作品" value="设计作品" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable>
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已拒绝" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="batch-delete-toolbar">
        <el-button v-if="canEdit && hasPermission('content:works:remove')" type="danger" plain :disabled="!selectedRows.length" @click="handleBatchDelete">批量删除</el-button>
      </div>

      <el-table :data="dataList" v-loading="loading" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="coverImage" label="封面" width="100">
          <template #default="{ row }">
            <el-image :src="resolveMediaUrl(row.coverImage)" style="width: 60px; height: 40px" fit="cover">
              <template #error>
                <div class="image-placeholder">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="作品名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="category" label="作品类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getCategoryType(row.category)">
              {{ getCategoryLabel(row.category) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="authors" label="作者" width="150">
          <template #default="{ row }">
            {{ formatAuthors(row.authors) }}
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览量" width="90" />
        <el-table-column prop="likeCount" label="点赞数" width="90" />
        <el-table-column prop="isFeatured" label="精选" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.isFeatured" type="warning">是</el-tag>
            <el-tag v-else type="info">否</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="handleView(row)">查看</el-button>
            <el-button v-if="canEdit && hasPermission('content:works:edit')" text type="warning" @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="canEdit && hasPermission('content:works:remove')" text type="danger" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="formDialogVisible" :title="formTitle" width="800px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="作品标题" prop="title">
              <el-input v-model="formData.title" placeholder="请输入作品标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="作品分类" prop="category">
              <el-select v-model="formData.category" placeholder="请选择作品分类" style="width: 100%">
                <el-option label="软件项目" value="软件项目" />
                <el-option label="硬件项目" value="硬件项目" />
                <el-option label="设计作品" value="设计作品" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="作品描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入作品描述" />
        </el-form-item>

        <el-form-item label="作品内容" prop="content">
          <el-input v-model="formData.content" type="textarea" :rows="6" placeholder="请输入作品详细内容" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="作者" prop="authors">
              <el-input v-model="formData.authors" placeholder="多个作者请使用逗号分隔" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="技术栈" prop="technologies">
              <el-input v-model="formData.technologies" placeholder="多个技术请使用逗号分隔" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="封面图片" prop="coverImage">
              <div class="works-upload-row">
                <el-upload
                  class="works-cover-upload"
                  :show-file-list="false"
                  :http-request="handleCoverUpload"
                >
                  <el-button type="primary" plain>上传封面</el-button>
                </el-upload>
                <el-input v-model="formData.coverImage" placeholder="请输入封面图片 URL 或相对路径" />
              </div>
              <el-image
                v-if="formData.coverImage"
                :src="resolveMediaUrl(formData.coverImage)"
                class="works-cover-preview"
                fit="cover"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="演示视频" prop="demoVideo">
              <div class="works-upload-row">
                <el-upload
                  class="works-video-upload"
                  :show-file-list="false"
                  :http-request="handleDemoVideoUpload"
                  accept="video/*"
                >
                  <el-button type="primary" plain>上传视频</el-button>
                </el-upload>
                <el-input v-model="formData.demoVideo" placeholder="请输入演示视频 URL 或相对路径" />
              </div>
              <video
                v-if="formData.demoVideo"
                :src="resolveMediaUrl(formData.demoVideo)"
                class="works-video-preview"
                controls
              ></video>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="是否精选" prop="isFeatured">
              <el-radio-group v-model="formData.isFeatured">
                <el-radio :label="true">是</el-radio>
                <el-radio :label="false">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="展示顺序" prop="displayOrder">
              <el-input-number v-model="formData.displayOrder" :min="0" />
            </el-form-item>
          </el-col>
        </el-row>
        <ReviewAssignmentFields
          v-model:manager-reviewer-id="formData.managerReviewerId"
          v-model:teacher-reviewer-id="formData.teacherReviewerId"
        />
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="作品详情" width="700px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="作品名称" :span="2">{{ currentWork.title }}</el-descriptions-item>
        <el-descriptions-item label="作品类型">{{ getCategoryLabel(currentWork.category) }}</el-descriptions-item>
        <el-descriptions-item label="作者">{{ formatAuthors(currentWork.authors) }}</el-descriptions-item>
        <el-descriptions-item label="浏览量">{{ currentWork.viewCount }}</el-descriptions-item>
        <el-descriptions-item label="点赞数">{{ currentWork.likeCount }}</el-descriptions-item>
        <el-descriptions-item label="是否精选">{{ currentWork.isFeatured ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentWork.createTime }}</el-descriptions-item>
        <el-descriptions-item label="作品描述" :span="2">{{ currentWork.description }}</el-descriptions-item>
        <el-descriptions-item label="作品内容" :span="2">{{ currentWork.content }}</el-descriptions-item>
        <el-descriptions-item label="封面图片" :span="2">
          <el-image v-if="currentWork.coverImage" :src="resolveMediaUrl(currentWork.coverImage)" style="width: 200px" fit="cover" />
        </el-descriptions-item>
        <el-descriptions-item v-if="currentWork.demoVideo" label="演示视频" :span="2">
          <video :src="resolveMediaUrl(currentWork.demoVideo)" class="detail-video-preview" controls></video>
        </el-descriptions-item>
        <el-descriptions-item label="技术栈" :span="2">{{ currentWork.technologies }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useBatchDelete } from '@/utils/batchDelete'
import { Plus, Picture } from '@element-plus/icons-vue'
import { addWorks, deleteWorks, getWorksList, updateWorks } from '@/api/works'
import { uploadManagedFile } from '@/utils/fileUpload'
import { usePermission } from '@/utils/permission'
import { useUserStore } from '@/stores/user'
import { resolveMediaUrl } from '@/utils/mediaUrl'
import ReviewAssignmentFields from '@/components/ReviewAssignmentFields.vue'

const userStore = useUserStore()
const canEdit = computed(() => !userStore.hasRole([8]))
const { hasPermission } = usePermission()

const loading = ref(false)
const submitLoading = ref(false)
const dataList = ref([])
const detailDialogVisible = ref(false)
const formDialogVisible = ref(false)
const formTitle = ref('新增作品')
const currentWork = ref({})
const formRef = ref(null)

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

const createDefaultForm = () => ({
  id: null,
  title: '',
  description: '',
  content: '',
  category: '',
  authors: '',
  technologies: '',
  coverImage: '',
  demoVideo: '',
  isFeatured: false,
  displayOrder: 0,
  managerReviewerId: null,
  teacherReviewerId: null
})

const formData = ref(createDefaultForm())

const formRules = {
  title: [{ required: true, message: '请输入作品标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入作品内容', trigger: 'blur' }],
  category: [{ required: true, message: '请选择作品分类', trigger: 'change' }]
}

formRules.managerReviewerId = [{ required: true, message: '请选择负责人审批人', trigger: 'change' }]
formRules.teacherReviewerId = [{ required: true, message: '请选择指导老师审批人', trigger: 'change' }]

const getCategoryType = (category) => {
  const typeMap = {
    Web应用: 'primary',
    移动应用: 'success',
    人工智能: 'warning',
    桌面应用: 'info',
    软件项目: 'primary',
    硬件项目: 'success',
    设计作品: 'warning',
    其他: ''
  }
  return typeMap[category] || 'info'
}

const getCategoryLabel = (category) => category || '未分类'

const formatAuthors = (authors) => {
  if (!authors) return '-'
  try {
    const arr = JSON.parse(authors)
    return Array.isArray(arr) ? arr.join(', ') : authors
  } catch {
    return authors
  }
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getWorksList(searchForm)
    dataList.value = res.data || []
    pagination.total = dataList.value.length
  } catch (error) {
    console.error('获取作品列表失败:', error)
    ElMessage.error('获取作品列表失败')
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
  formTitle.value = '新增作品'
  formData.value = createDefaultForm()
  formDialogVisible.value = true
}

const handleEdit = (row) => {
  formTitle.value = '编辑作品'
  formData.value = {
    id: row.id,
    title: row.title,
    description: row.description,
    content: row.content,
    category: row.category,
    authors: row.authors,
    technologies: row.technologies,
    coverImage: row.coverImage,
    demoVideo: row.demoVideo,
    isFeatured: Boolean(row.isFeatured),
    displayOrder: row.displayOrder || 0,
    managerReviewerId: row.managerReviewerId || null,
    teacherReviewerId: row.teacherReviewerId || null
  }
  formDialogVisible.value = true
}

const handleCoverUpload = async (options) => {
  try {
    const { storedValue, url } = await uploadManagedFile(options, 'works-cover')
    formData.value.coverImage = storedValue
    options.onSuccess?.({ code: 200, data: { path: storedValue, url } })
    ElMessage.success('封面上传成功')
  } catch (error) {
    options.onError?.(error)
    ElMessage.error(error.message || '封面上传失败')
  }
}

const handleDemoVideoUpload = async (options) => {
  try {
    const { storedValue, url } = await uploadManagedFile(options, 'works-video')
    formData.value.demoVideo = storedValue
    options.onSuccess?.({ code: 200, data: { path: storedValue, url } })
    ElMessage.success('视频上传成功')
  } catch (error) {
    options.onError?.(error)
    ElMessage.error(error.message || '视频上传失败')
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitLoading.value = true
    try {
      if (formData.value.id) {
        await updateWorks(formData.value)
        ElMessage.success('修改成功')
      } else {
        await addWorks(formData.value)
        ElMessage.success('新增成功')
      }
      formDialogVisible.value = false
      fetchList()
    } catch (error) {
      console.error('提交失败:', error)
      ElMessage.error('提交失败')
    } finally {
      submitLoading.value = false
    }
  })
}

const handleView = (row) => {
  currentWork.value = { ...row }
  detailDialogVisible.value = true
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定要删除该作品吗？', '提示', { type: 'warning' })
  try {
    await deleteWorks(row.id)
    ElMessage.success('删除成功')
    fetchList()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}


const { selectedRows, handleSelectionChange, handleBatchDelete } = useBatchDelete({
  entityName: '作品',
  deleteItem: (id) => deleteWorks(id),
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

  .image-placeholder {
    width: 60px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f5f7fa;
    color: #909399;
  }

  .works-upload-row {
    display: flex;
    align-items: center;
    gap: 12px;
    width: 100%;
  }

  .works-cover-preview {
    width: 120px;
    height: 80px;
    margin-top: 12px;
    border-radius: 6px;
    border: 1px solid #ebeef5;
  }

  .works-video-preview {
    width: 100%;
    max-width: 260px;
    margin-top: 12px;
    border-radius: 8px;
    background: #000;
  }

  .detail-video-preview {
    width: 100%;
    max-width: 360px;
    border-radius: 8px;
    background: #000;
  }
}
</style>
