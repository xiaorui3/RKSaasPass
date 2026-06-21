<template>
  <div class="admin-achievements">
    <div class="page-header">
      <h2>成就管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="handleAdd">新增成就</el-button>
        <el-select v-model="filterType" placeholder="类型筛选" clearable style="width: 130px;" @change="loadData">
          <el-option label="比赛获奖" :value="1" />
          <el-option label="项目成果" :value="2" />
          <el-option label="论文发表" :value="3" />
          <el-option label="专利成果" :value="4" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="审核状态" clearable style="width: 130px;" @change="loadData">
          <el-option label="待审核" :value="1" />
          <el-option label="已通过" :value="2" />
          <el-option label="已拒绝" :value="3" />
        </el-select>
        <el-button @click="loadData" :loading="loading">刷新</el-button>
      </div>
    </div>

    <div class="batch-delete-toolbar">
      <el-button type="danger" plain :disabled="!selectedRows.length" @click="handleBatchDelete">批量删除</el-button>
    </div>

    <el-table :data="achievements" v-loading="loading" stripe @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="48" />
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="achievementTitle" label="成就名称" min-width="180" />
      <el-table-column prop="achievementType" label="类型" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ getTypeText(row.achievementType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="achievementLevel" label="级别" width="100">
        <template #default="{ row }">
          <el-tag size="small" type="warning">{{ getLevelText(row.achievementLevel) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="审核状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="userName" label="成员" width="110" />
      <el-table-column prop="awardDate" label="获奖日期" width="120" />
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 1" size="small" type="success" @click="handleApprove(row)">通过</el-button>
          <el-button v-if="row.status === 1" size="small" type="danger" @click="handleReject(row)">拒绝</el-button>
          <el-button size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑成就' : '新增成就'" width="680px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="110px">
        <el-form-item label="成员ID" prop="userId">
          <el-input-number v-model="form.userId" :min="1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="成就名称" prop="achievementTitle">
          <el-input v-model="form.achievementTitle" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="成就类型" prop="achievementType">
          <el-select v-model="form.achievementType" style="width: 100%;">
            <el-option label="比赛获奖" :value="1" />
            <el-option label="项目成果" :value="2" />
            <el-option label="论文发表" :value="3" />
            <el-option label="专利成果" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="成就级别" prop="achievementLevel">
          <el-select v-model="form.achievementLevel" style="width: 100%;">
            <el-option label="国家级" :value="1" />
            <el-option label="省级" :value="2" />
            <el-option label="市级" :value="3" />
            <el-option label="校级" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="颁发单位">
          <el-input v-model="form.awardOrganization" maxlength="100" />
        </el-form-item>
        <el-form-item label="获奖日期">
          <el-date-picker v-model="form.awardDate" type="date" value-format="YYYY-MM-DD" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="证明材料">
          <el-input v-model="form.proofImages" placeholder="图片或文件地址，多个用逗号分隔" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="4" maxlength="1000" show-word-limit />
        </el-form-item>
        <ReviewAssignmentFields
          v-model:manager-reviewer-id="form.managerReviewerId"
          v-model:teacher-reviewer-id="form.teacherReviewerId"
        />
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="formSubmitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="remarkVisible" :title="remarkTitle" width="420px">
      <el-input v-model="remarkContent" type="textarea" :rows="3" placeholder="请输入审核备注" />
      <template #footer>
        <el-button @click="remarkVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRemark" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useBatchDelete } from '@/utils/batchDelete'
import {
  getAchievementList, addAchievement, updateAchievement, deleteAchievement,
  rejectAchievement, reviewAchievementByManager, reviewAchievementByTeacher,
  getAchievementTypeText, getAchievementLevelText, getAchievementStatusText
} from '@/api/achievement'
import ReviewAssignmentFields from '@/components/ReviewAssignmentFields.vue'

const loading = ref(false)
const achievements = ref([])
const filterType = ref(null)
const filterStatus = ref(null)
const remarkVisible = ref(false)
const remarkTitle = ref('')
const remarkContent = ref('')
const submitting = ref(false)
const currentAction = ref(null)
const currentItem = ref(null)
const formVisible = ref(false)
const formSubmitting = ref(false)
const formRef = ref()

const form = reactive({
  id: null,
  userId: null,
  achievementTitle: '',
  achievementType: 1,
  achievementLevel: 4,
  awardOrganization: '',
  awardDate: '',
  proofImages: '',
  description: '',
  managerReviewerId: null,
  teacherReviewerId: null
})

const rules = {
  userId: [{ required: true, message: '请输入成员ID', trigger: 'change' }],
  achievementTitle: [{ required: true, message: '请输入成就名称', trigger: 'blur' }],
  achievementType: [{ required: true, message: '请选择成就类型', trigger: 'change' }],
  achievementLevel: [{ required: true, message: '请选择成就级别', trigger: 'change' }],
  managerReviewerId: [{ required: true, message: '请选择负责人审批人', trigger: 'change' }],
  teacherReviewerId: [{ required: true, message: '请选择指导老师审批人', trigger: 'change' }]
}

function getTypeText(t) { return getAchievementTypeText(t) }
function getLevelText(l) { return getAchievementLevelText(l) }
function getStatusText(s) { return getAchievementStatusText(s) }
function statusType(s) {
  if (s === 1) return 'warning'
  if (s === 2) return 'success'
  return 'danger'
}

function reviewStage(row) {
  if (row.managerReviewStatus === 0) return 'manager'
  if (row.teacherReviewStatus === 0) return 'teacher'
  return 'approved'
}

async function loadData() {
  loading.value = true
  try {
    const params = {}
    if (filterType.value) params.achievementType = filterType.value
    if (filterStatus.value) params.status = filterStatus.value
    const res = await getAchievementList(params)
    achievements.value = res.data?.records || res.data || []
  } catch {
    achievements.value = []
  }
  loading.value = false
}

function resetForm(row = {}) {
  Object.assign(form, {
    id: row.id || null,
    userId: row.userId || null,
    achievementTitle: row.achievementTitle || '',
    achievementType: row.achievementType || 1,
    achievementLevel: row.achievementLevel || 4,
    awardOrganization: row.awardOrganization || '',
    awardDate: row.awardDate || '',
    proofImages: row.proofImages || '',
    description: row.description || '',
    managerReviewerId: row.managerReviewerId || null,
    teacherReviewerId: row.teacherReviewerId || null
  })
}

function handleAdd() {
  resetForm()
  formVisible.value = true
}

function handleEdit(row) {
  resetForm(row)
  formVisible.value = true
}

async function submitForm() {
  if (!formRef.value) return
  await formRef.value.validate()
  formSubmitting.value = true
  try {
    const payload = { ...form }
    if (payload.id) {
      await updateAchievement(payload)
    } else {
      delete payload.id
      await addAchievement(payload)
    }
    ElMessage.success('保存成功')
    formVisible.value = false
    loadData()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    formSubmitting.value = false
  }
}

function handleApprove(row) {
  currentItem.value = row
  currentAction.value = 'approve'
  remarkTitle.value = '审核通过'
  remarkContent.value = ''
  remarkVisible.value = true
}

function handleReject(row) {
  currentItem.value = row
  currentAction.value = 'reject'
  remarkTitle.value = '拒绝审核'
  remarkContent.value = ''
  remarkVisible.value = true
}

async function submitRemark() {
  if (currentAction.value === 'reject' && !remarkContent.value.trim()) {
    return ElMessage.warning('请填写拒绝原因')
  }
  submitting.value = true
  try {
    if (currentAction.value === 'approve') {
      const api = reviewStage(currentItem.value) === 'manager' ? reviewAchievementByManager : reviewAchievementByTeacher
      await api(currentItem.value.id, remarkContent.value)
      ElMessage.success('审核通过')
    } else {
      await rejectAchievement(currentItem.value.id, remarkContent.value)
      ElMessage.success('已拒绝')
    }
    remarkVisible.value = false
    loadData()
  } catch {
    ElMessage.error('操作失败')
  }
  submitting.value = false
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该成就吗？', '提示', { type: 'warning' })
    await deleteAchievement(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}

const { selectedRows, handleSelectionChange, handleBatchDelete } = useBatchDelete({
  entityName: '成就',
  deleteItem: (id) => deleteAchievement(id),
  fetchList: loadData
})

onMounted(loadData)
</script>

<style scoped>
.admin-achievements { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; gap: 16px; }
.page-header h2 { margin: 0; }
.header-actions { display: flex; gap: 12px; flex-wrap: wrap; }
@media (max-width: 760px) {
  .page-header { align-items: stretch; flex-direction: column; }
  .header-actions :deep(.el-select),
  .header-actions :deep(.el-button) { width: 100%; }
}
</style>
