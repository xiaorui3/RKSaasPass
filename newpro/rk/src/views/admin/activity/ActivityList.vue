<template>
  <div class="admin-page">
    <el-card class="page-card">
      <template #header>
        <div class="card-header">
          <div>
            <div class="title">活动管理</div>
            <div class="subtitle">管理活动、签到成员和活动学分发放</div>
          </div>
          <el-button v-if="canEdit" type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增活动
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="活动名称">
          <el-input
            v-model="searchForm.title"
            placeholder="请输入活动名称"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="活动类型">
          <el-select v-model="searchForm.type" placeholder="全部类型" clearable style="width: 160px">
            <el-option label="讲座" :value="1" />
            <el-option label="比赛" :value="2" />
            <el-option label="培训" :value="3" />
            <el-option label="文娱" :value="4" />
            <el-option label="志愿服务" :value="5" />
            <el-option label="其他" :value="6" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 140px">
            <el-option label="未开始" :value="1" />
            <el-option label="报名中" :value="2" />
            <el-option label="进行中" :value="3" />
            <el-option label="已结束" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <div class="batch-delete-toolbar">
        <el-button v-if="canEdit" type="danger" plain :disabled="!selectedRows.length" @click="handleBatchDelete">批量删除</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="dataList"
        stripe
        style="width: 100%"
        @sort-change="handleSortChange"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="activityName" label="活动名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="activityType" label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getTypeTag(row.activityType ?? row.type)" size="small">
              {{ getTypeText(row.activityType ?? row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="活动时间" width="220">
          <template #default="{ row }">
            <div class="time-range">
              <span>{{ formatDisplayDate(row.startTime) }}</span>
              <span>{{ formatDisplayDate(row.endTime) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="points" label="学分" width="90" align="center">
          <template #default="{ row }">
            <span>{{ row.points ?? 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="地点" min-width="140" show-overflow-tooltip />
        <el-table-column label="报名情况" width="110" align="center">
          <template #default="{ row }">
            {{ row.currentParticipants || 0 }} / {{ row.maxParticipants || 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="activityStatus" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.activityStatus ?? row.status)" size="small">
              {{ getStatusText(row.activityStatus ?? row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="跨租户" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isCrossTenant === 1 ? 'success' : 'info'" size="small">
              {{ row.isCrossTenant === 1 ? '共享' : '本租户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canEdit" type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="info" link size="small" @click="handleParticipants(row)">报名管理</el-button>
            <el-button
              v-if="canEdit"
              type="success"
              link
              size="small"
              :disabled="!isGrantableActivity(row)"
              @click="handleGrantCredits(row)"
            >
              发放学分
            </el-button>
            <el-button v-if="canEdit" type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.page"
          :page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="680px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
        <el-form-item label="活动名称" prop="title">
          <el-input v-model="formData.title" placeholder="请输入活动名称" />
        </el-form-item>
        <el-form-item label="活动类型" prop="type">
          <el-select v-model="formData.type" placeholder="请选择活动类型" style="width: 100%">
            <el-option label="讲座" :value="1" />
            <el-option label="比赛" :value="2" />
            <el-option label="培训" :value="3" />
            <el-option label="文娱" :value="4" />
            <el-option label="志愿服务" :value="5" />
            <el-option label="其他" :value="6" />
          </el-select>
        </el-form-item>
        <el-form-item label="活动学分" prop="points">
          <el-input-number v-model="formData.points" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="formData.startTime"
            type="datetime"
            placeholder="选择开始时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="formData.endTime"
            type="datetime"
            placeholder="选择结束时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="报名开始时间">
          <el-date-picker
            v-model="formData.registrationStartTime"
            type="datetime"
            placeholder="选择报名开始时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="报名结束时间">
          <el-date-picker
            v-model="formData.registrationEndTime"
            type="datetime"
            placeholder="选择报名结束时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="活动地点" prop="location">
          <el-input v-model="formData.location" placeholder="请输入活动地点" />
        </el-form-item>
        <el-form-item label="封面图片">
          <div class="upload-row">
            <el-upload :show-file-list="false" :http-request="handleCoverUpload">
              <el-button type="primary" plain>上传封面</el-button>
            </el-upload>
            <img
              v-if="formData.coverImage"
              :src="resolveMediaUrl(formData.coverImage)"
              alt="活动封面"
              class="cover-preview"
            />
          </div>
        </el-form-item>
        <el-form-item label="活动附件">
          <div class="upload-row">
            <el-upload :show-file-list="false" :http-request="handleAttachmentUpload">
              <el-button type="primary" plain>上传附件</el-button>
            </el-upload>
            <a
              v-if="formData.attachmentUrl"
              :href="resolveMediaUrl(formData.attachmentUrl)"
              target="_blank"
              rel="noopener noreferrer"
              class="attachment-link"
            >
              查看已上传附件
            </a>
          </div>
        </el-form-item>
        <el-form-item label="人数上限" prop="maxParticipants">
          <el-input-number v-model="formData.maxParticipants" :min="1" :max="1000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="跨租户开放">
          <el-switch v-model="formData.isCrossTenant" />
        </el-form-item>
        <el-form-item label="活动描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="4"
            placeholder="请输入活动描述"
          />
        </el-form-item>
        <ReviewAssignmentFields
          v-model:manager-reviewer-id="formData.managerReviewerId"
          v-model:teacher-reviewer-id="formData.teacherReviewerId"
        />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="participantsVisible" title="报名管理" width="900px">
      <el-table :data="participantsList" v-loading="participantsLoading" stripe>
        <el-table-column prop="userId" label="用户 ID" width="100" />
        <el-table-column label="用户姓名" min-width="140">
          <template #default="{ row }">
            {{ formatParticipantName(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="studentId" label="学号" min-width="130" show-overflow-tooltip />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="cellPhone" label="手机号" min-width="130" show-overflow-tooltip />
        <el-table-column prop="registrationTime" label="报名时间" width="180" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getRegistrationTag(row.registrationStatus ?? row.status)" size="small">
              {{ getRegistrationText(row.registrationStatus ?? row.status) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useBatchDelete } from '@/utils/batchDelete'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  ACTIVITY_STATUS,
  createActivity,
  deleteActivity,
  getAdminActivityPage,
  getRegistrationList,
  grantActivityCredits,
  updateActivity
} from '@/api/activity'
import { useUserStore } from '@/stores/user'
import { uploadManagedFile } from '@/utils/fileUpload'
import { resolveMediaUrl } from '@/utils/mediaUrl'
import ReviewAssignmentFields from '@/components/ReviewAssignmentFields.vue'

const userStore = useUserStore()
const canEdit = computed(() => !userStore.hasRole([8]))

const loading = ref(false)
const dataList = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增活动')
const participantsVisible = ref(false)
const participantsLoading = ref(false)
const participantsList = ref([])
const formRef = ref(null)

const searchForm = reactive({
  title: '',
  type: null,
  status: null,
  orderBy: '',
  order: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const formData = reactive({
  id: null,
  title: '',
  type: 1,
  points: 0,
  startTime: '',
  endTime: '',
  registrationStartTime: '',
  registrationEndTime: '',
  location: '',
  coverImage: '',
  attachmentUrl: '',
  maxParticipants: 50,
  isCrossTenant: false,
  managerReviewerId: null,
  teacherReviewerId: null,
  description: ''
})

const formRules = {
  title: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择活动类型', trigger: 'change' }],
  points: [{ required: true, message: '请设置活动学分', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  location: [{ required: true, message: '请输入活动地点', trigger: 'blur' }]
}

formRules.location = formRules.location || []
formRules.managerReviewerId = [{ required: true, message: '请选择负责人审批人', trigger: 'change' }]
formRules.teacherReviewerId = [{ required: true, message: '请选择指导老师审批人', trigger: 'change' }]

const activityTypeMap = {
  1: { text: '讲座', tag: 'primary' },
  2: { text: '比赛', tag: 'danger' },
  3: { text: '培训', tag: 'warning' },
  4: { text: '文娱', tag: 'success' },
  5: { text: '志愿服务', tag: '' },
  6: { text: '其他', tag: 'info' }
}

const activityStatusMap = {
  [ACTIVITY_STATUS.NOT_STARTED]: { text: '未开始', tag: 'info' },
  [ACTIVITY_STATUS.REGISTERING]: { text: '报名中', tag: 'warning' },
  [ACTIVITY_STATUS.ONGOING]: { text: '进行中', tag: 'success' },
  [ACTIVITY_STATUS.ENDED]: { text: '已结束', tag: 'danger' }
}

const registrationStatusMap = {
  1: { text: '已报名', tag: 'info' },
  2: { text: '已签到', tag: 'success' },
  3: { text: '已取消', tag: 'danger' }
}

const getTypeText = (type) => activityTypeMap[type]?.text || '未知'
const getTypeTag = (type) => activityTypeMap[type]?.tag || 'info'
const getStatusText = (status) => activityStatusMap[status]?.text || '未知'
const getStatusType = (status) => activityStatusMap[status]?.tag || 'info'
const getRegistrationText = (status) => registrationStatusMap[status]?.text || '未知'
const getRegistrationTag = (status) => registrationStatusMap[status]?.tag || 'info'
const formatParticipantName = (row) => row.userName || row.name || row.username || `用户-${row.userId || '-'}`

const formatDisplayDate = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

const formatDateForBackend = (date) => {
  if (!date) return null
  const value = new Date(date)
  if (Number.isNaN(value.getTime())) {
    return date
  }
  const pad = (num) => String(num).padStart(2, '0')
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())} ${pad(value.getHours())}:${pad(value.getMinutes())}:${pad(value.getSeconds())}`
}

const isGrantableActivity = (row) => Number(row.activityStatus ?? row.status) === ACTIVITY_STATUS.ENDED

const fetchList = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      title: searchForm.title || undefined,
      type: searchForm.type ?? undefined,
      status: searchForm.status ?? undefined
    }
    const res = await getAdminActivityPage(params)
    dataList.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (error) {
    console.error('fetch activity list failed', error)
    dataList.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  fetchList()
}

const handleReset = () => {
  searchForm.title = ''
  searchForm.type = null
  searchForm.status = null
  searchForm.orderBy = ''
  searchForm.order = ''
  pagination.page = 1
  fetchList()
}

const resetFormData = () => {
  Object.assign(formData, {
    id: null,
    title: '',
    type: 1,
    points: 0,
    startTime: '',
    endTime: '',
    registrationStartTime: '',
    registrationEndTime: '',
    location: '',
    coverImage: '',
    attachmentUrl: '',
    maxParticipants: 50,
    isCrossTenant: false,
    managerReviewerId: null,
    teacherReviewerId: null,
    description: ''
  })
}

const handleAdd = () => {
  dialogTitle.value = '新增活动'
  resetFormData()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑活动'
  Object.assign(formData, {
    id: row.id,
    title: row.activityName || row.title || '',
    type: row.activityType ?? row.type ?? 1,
    points: row.points ?? 0,
    startTime: row.startTime || '',
    endTime: row.endTime || '',
    registrationStartTime: row.registrationStartTime || '',
    registrationEndTime: row.registrationEndTime || '',
    location: row.location || '',
    coverImage: row.coverImage || '',
    attachmentUrl: row.attachmentUrl || '',
    maxParticipants: row.maxParticipants || 50,
    isCrossTenant: row.isCrossTenant === 1,
    managerReviewerId: row.managerReviewerId || null,
    teacherReviewerId: row.teacherReviewerId || null,
    description: row.content || row.description || ''
  })
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除活动“${row.activityName || row.title}”吗？`, '提示', {
      type: 'warning'
    })
    await deleteActivity(row.id)
    ElMessage.success('删除成功')
    fetchList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const handleParticipants = async (row) => {
  participantsVisible.value = true
  participantsLoading.value = true
  try {
    const res = await getRegistrationList(row.id)
    participantsList.value = res.data || []
  } catch (error) {
    console.error('fetch activity participants failed', error)
    participantsList.value = []
  } finally {
    participantsLoading.value = false
  }
}

const handleGrantCredits = async (row) => {
  if (!isGrantableActivity(row)) {
    ElMessage.warning('活动结束后才能发放学分')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定按活动“${row.activityName || row.title}”当前配置的 ${row.points ?? 0} 学分发放给已签到成员吗？`,
      '发放学分',
      { type: 'warning' }
    )
    const res = await grantActivityCredits(row.id)
    ElMessage.success(res.data || res.msg || '学分发放成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '学分发放失败')
    }
  }
}

const handleCoverUpload = async (options) => {
  try {
    const { storedValue, url } = await uploadManagedFile(options, 'activity-cover')
    formData.coverImage = storedValue
    options.onSuccess?.({ code: 200, data: { path: storedValue, url } })
    ElMessage.success('封面上传成功')
  } catch (error) {
    options.onError?.(error)
    ElMessage.error(error.message || '封面上传失败')
  }
}

const handleAttachmentUpload = async (options) => {
  try {
    const { storedValue, url } = await uploadManagedFile(options, 'activity-attachment')
    formData.attachmentUrl = storedValue
    options.onSuccess?.({ code: 200, data: { path: storedValue, url } })
    ElMessage.success('附件上传成功')
  } catch (error) {
    options.onError?.(error)
    ElMessage.error(error.message || '附件上传失败')
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    const payload = {
      id: formData.id,
      activityName: formData.title,
      activityType: formData.type,
      organizer: userStore.userName,
      points: formData.points,
      startTime: formatDateForBackend(formData.startTime),
      endTime: formatDateForBackend(formData.endTime),
      registrationStartTime: formatDateForBackend(formData.registrationStartTime),
      registrationEndTime: formatDateForBackend(formData.registrationEndTime),
      location: formData.location,
      coverImage: formData.coverImage,
      attachmentUrl: formData.attachmentUrl,
      maxParticipants: formData.maxParticipants,
      isCrossTenant: formData.isCrossTenant ? 1 : 0,
      managerReviewerId: formData.managerReviewerId,
      teacherReviewerId: formData.teacherReviewerId,
      content: formData.description
    }
    if (payload.id) {
      await updateActivity(payload)
      ElMessage.success('活动更新成功')
    } else {
      await createActivity(payload)
      ElMessage.success('活动创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } catch (error) {
    if (error !== false) {
      ElMessage.error(error.message || '保存活动失败')
    }
  }
}

const handleSortChange = ({ prop, order }) => {
  searchForm.orderBy = prop || ''
  searchForm.order = order || ''
}

const handleSizeChange = (size) => {
  pagination.size = size
  fetchList()
}

const handlePageChange = (page) => {
  pagination.page = page
  fetchList()
}


const { selectedRows, handleSelectionChange, handleBatchDelete } = useBatchDelete({
  entityName: '活动',
  deleteItem: (id) => deleteActivity(id),
  fetchList: fetchList
})

onMounted(fetchList)
</script>

<style lang="scss" scoped>
.admin-page {
  padding: 0;
}

.page-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: #6b7280;
}

.search-form {
  margin-bottom: 16px;
  padding: 16px;
  border-radius: 10px;
  background: #f8fafc;
}

.time-range {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: #4b5563;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.upload-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.cover-preview {
  width: 120px;
  height: 80px;
  object-fit: cover;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
}

.attachment-link {
  color: #2563eb;
  text-decoration: none;
}

.attachment-link:hover {
  text-decoration: underline;
}
</style>
