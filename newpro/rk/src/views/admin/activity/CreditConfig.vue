<template>
  <div class="admin-page credit-config-page">
    <el-card class="credit-type-card">
      <template #header>
        <div class="card-header">
          <div>
            <h2>学分类型配置</h2>
            <p>维护当前租户的学分分类、编码、学分上限与说明。</p>
          </div>
          <el-button type="primary" @click="openTypeDialog()">新增类型</el-button>
        </div>
      </template>

      <div class="batch-delete-toolbar">
        <el-button type="danger" plain :disabled="!selectedRows.length" @click="handleBatchDelete">批量删除</el-button>
      </div>

      <el-table :data="creditTypes" v-loading="typesLoading" stripe empty-text="暂无学分类型配置" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="name" label="类型名称" min-width="160" />
        <el-table-column prop="code" label="类型编码" min-width="120" />
        <el-table-column prop="maxCredit" label="学分上限" width="110" />
        <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openTypeDialog(row)">编辑</el-button>
            <el-button text type="danger" @click="handleDeleteType(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card>
      <template #header>
        <div class="card-header">
          <div>
            <h2>学分记录管理</h2>
            <p>查看、审核和补录当前租户的学分记录。</p>
          </div>
          <el-button type="primary" @click="openAddDialog">新增记录</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已驳回" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="账号ID">
          <el-input v-model="searchForm.userId" placeholder="可输入账号ID筛选" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="dataList" v-loading="loading" stripe empty-text="暂无学分记录">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="userId" label="用户 ID" width="100" />
        <el-table-column prop="creditTypeCode" label="学分类型" width="140">
          <template #default="{ row }">
            {{ getTypeLabel(row.creditTypeCode) }}
          </template>
        </el-table-column>
        <el-table-column prop="sourceType" label="来源类型" width="120" />
        <el-table-column prop="description" label="来源说明" min-width="220" show-overflow-tooltip />
        <el-table-column prop="creditHours" label="学时" width="90" />
        <el-table-column prop="creditScore" label="学分" width="90" />
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

    <el-dialog
      v-model="typeDialogVisible"
      :title="typeForm.id ? '编辑学分类型' : '新增学分类型'"
      width="520px"
      @close="resetTypeForm"
    >
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="100px">
        <el-form-item label="类型名称" prop="name">
          <el-input v-model="typeForm.name" placeholder="请输入类型名称" />
        </el-form-item>
        <el-form-item label="类型编码" prop="code">
          <el-input
            v-model="typeForm.code"
            placeholder="请输入英文编码，例如 volunteer"
            :disabled="Boolean(typeForm.id)"
          />
        </el-form-item>
        <el-form-item label="学分上限" prop="maxCredit">
          <el-input-number v-model="typeForm.maxCredit" :min="0" :precision="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input
            v-model="typeForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入该学分类型说明"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="typeSubmitLoading" @click="submitType">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="addDialogVisible" title="新增学分记录" width="500px" @close="resetRecordForm">
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="100px">
        <el-form-item label="选择用户" prop="userId">
          <div class="user-picker-row">
            <el-input
              :model-value="selectedCreditUserLabel"
              placeholder="请选择成员"
              readonly
            />
            <el-button type="primary" plain @click="openUserPicker">选择</el-button>
          </div>
        </el-form-item>
        <el-form-item label="学分类型" prop="creditTypeCode">
          <el-select v-model="addForm.creditTypeCode" placeholder="请选择学分类型" style="width: 100%">
            <el-option
              v-for="item in creditTypes"
              :key="item.code"
              :label="item.name"
              :value="item.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="来源类型" prop="sourceType">
          <el-input v-model="addForm.sourceType" placeholder="请输入来源类型，例如 activity" />
        </el-form-item>
        <el-form-item label="来源说明" prop="description">
          <el-input
            v-model="addForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入来源说明"
          />
        </el-form-item>
        <el-form-item label="学时" prop="creditHours">
          <el-input-number v-model="addForm.creditHours" :min="0" :precision="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="学分" prop="creditScore">
          <el-input-number v-model="addForm.creditScore" :min="0" :precision="1" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitAdd">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="userPickerVisible" title="选择学分用户" width="760px">
      <el-form :inline="true" :model="userSearchForm" class="search-form">
        <el-form-item label="姓名">
          <el-input v-model="userSearchForm.username" placeholder="请输入姓名或账号" clearable @keyup.enter="fetchCreditUsers" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchCreditUsers">搜索</el-button>
          <el-button @click="resetUserSearch">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="userOptions" v-loading="userPickerLoading" stripe height="360">
        <el-table-column prop="authUserId" label="登录账号ID" width="110">
          <template #default="{ row }">
            {{ row.authUserId || row.id }}
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" min-width="120">
          <template #default="{ row }">
            {{ row.name || row.username || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="studentId" label="学号" min-width="120" show-overflow-tooltip />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="cellPhone" label="手机号" min-width="130" show-overflow-tooltip />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="selectCreditUser(row)">选择</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="rejectDialogVisible" title="驳回原因" width="400px">
      <el-input
        v-model="rejectReason"
        type="textarea"
        :rows="4"
        placeholder="请输入驳回原因"
      />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useBatchDelete } from '@/utils/batchDelete'
import {
  getCreditTypes,
  getCreditRecords,
  addCreditRecord,
  approveCreditRecord,
  rejectCreditRecord,
  saveCreditType,
  deleteCreditType
} from '@/api/credit'
import { getUserPage } from '@/api/user'

const statusMap = {
  0: { label: '待审核', tag: 'warning' },
  1: { label: '已通过', tag: 'success' },
  2: { label: '已驳回', tag: 'danger' }
}

const loading = ref(false)
const submitLoading = ref(false)
const dataList = ref([])

const typesLoading = ref(false)
const typeDialogVisible = ref(false)
const typeSubmitLoading = ref(false)
const typeFormRef = ref(null)
const creditTypes = ref([])

const searchForm = reactive({
  status: '',
  userId: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const typeForm = reactive({
  id: null,
  name: '',
  code: '',
  maxCredit: 0,
  description: ''
})

const typeRules = {
  name: [{ required: true, message: '请输入类型名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入类型编码', trigger: 'blur' }]
}

const addDialogVisible = ref(false)
const addFormRef = ref(null)
const addForm = reactive({
  userId: '',
  creditTypeCode: '',
  sourceType: '',
  creditHours: 0,
  creditScore: 0,
  description: ''
})
const selectedCreditUser = ref(null)
const userPickerVisible = ref(false)
const userPickerLoading = ref(false)
const userOptions = ref([])
const userSearchForm = reactive({
  username: ''
})
const selectedCreditUserLabel = computed(() => {
  if (!addForm.userId) return ''
  const user = selectedCreditUser.value
  if (!user) return `用户-${addForm.userId}`
  const name = user.name || user.username || `用户-${user.id}`
  const studentId = user.studentId ? ` / ${user.studentId}` : ''
  return `${name}${studentId}`
})

const addRules = {
  userId: [{ required: true, message: '请选择用户', trigger: 'change' }],
  creditTypeCode: [{ required: true, message: '请选择学分类型', trigger: 'change' }],
  sourceType: [{ required: true, message: '请输入来源类型', trigger: 'blur' }],
  description: [{ required: true, message: '请输入来源说明', trigger: 'blur' }],
  creditHours: [{ required: true, message: '请输入学时', trigger: 'blur' }],
  creditScore: [{ required: true, message: '请输入学分', trigger: 'blur' }]
}

const rejectDialogVisible = ref(false)
const rejectReason = ref('')
const currentRejectId = ref(null)

const getTypeLabel = (code) => {
  const found = creditTypes.value.find((item) => item.code === code)
  return found ? found.name : code || '-'
}

const fetchList = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size
    }
    if (searchForm.status !== '' && searchForm.status !== null) {
      params.status = searchForm.status
    }
    if (searchForm.userId) {
      params.userId = searchForm.userId
    }
    const res = await getCreditRecords(params)
    if (res.code === 200 && res.data) {
      dataList.value = Array.isArray(res.data.records) ? res.data.records : []
      pagination.total = res.data.total ?? dataList.value.length
      return
    }
    dataList.value = []
    pagination.total = 0
  } catch (error) {
    console.error('获取学分记录失败:', error)
    ElMessage.error('获取学分记录失败')
    dataList.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const fetchCreditTypes = async () => {
  typesLoading.value = true
  try {
    const res = await getCreditTypes()
    creditTypes.value = res.code === 200 && Array.isArray(res.data) ? res.data : []
  } catch (error) {
    console.error('获取学分类型失败:', error)
    ElMessage.error('获取学分类型失败')
    creditTypes.value = []
  } finally {
    typesLoading.value = false
  }
}

const resetSearch = () => {
  searchForm.status = ''
  searchForm.userId = ''
  pagination.page = 1
  fetchList()
}

const openTypeDialog = (row = null) => {
  if (row) {
    typeForm.id = row.id
    typeForm.name = row.name || ''
    typeForm.code = row.code || ''
    typeForm.maxCredit = Number(row.maxCredit ?? 0)
    typeForm.description = row.description || ''
  } else {
    resetTypeForm()
  }
  typeDialogVisible.value = true
}

const resetTypeForm = () => {
  typeForm.id = null
  typeForm.name = ''
  typeForm.code = ''
  typeForm.maxCredit = 0
  typeForm.description = ''
  typeFormRef.value?.resetFields()
}

const submitType = async () => {
  const valid = await typeFormRef.value?.validate().catch(() => false)
  if (!valid) return

  typeSubmitLoading.value = true
  try {
    const payload = {
      id: typeForm.id,
      name: typeForm.name.trim(),
      code: typeForm.code.trim(),
      maxCredit: typeForm.maxCredit,
      description: typeForm.description?.trim() || ''
    }
    const res = await saveCreditType(payload)
    if (res.code === 200) {
      ElMessage.success('学分类型已保存')
      typeDialogVisible.value = false
      await fetchCreditTypes()
      return
    }
    ElMessage.error(res.msg || '保存学分类型失败')
  } catch (error) {
    console.error('保存学分类型失败:', error)
    ElMessage.error('保存学分类型失败')
  } finally {
    typeSubmitLoading.value = false
  }
}

const handleDeleteType = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除学分类型“${row.name}”吗？`, '提示', { type: 'warning' })
    const res = await deleteCreditType(row.id)
    if (res.code === 200) {
      ElMessage.success('学分类型已删除')
      await fetchCreditTypes()
    } else {
      ElMessage.error(res.msg || '删除学分类型失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除学分类型失败')
    }
  }
}

const openAddDialog = () => {
  addDialogVisible.value = true
}

const fetchCreditUsers = async () => {
  userPickerLoading.value = true
  try {
    const res = await getUserPage({
      page: 1,
      size: 100,
      username: userSearchForm.username || undefined,
      status: 1
    })
    userOptions.value = Array.isArray(res.data?.records) ? res.data.records : []
  } catch (error) {
    console.error('获取用户列表失败:', error)
    userOptions.value = []
    ElMessage.error('获取用户列表失败')
  } finally {
    userPickerLoading.value = false
  }
}

const openUserPicker = async () => {
  userPickerVisible.value = true
  await fetchCreditUsers()
}

const resetUserSearch = () => {
  userSearchForm.username = ''
  fetchCreditUsers()
}

const selectCreditUser = (row) => {
  selectedCreditUser.value = row
  addForm.userId = row.authUserId || row.id
  userPickerVisible.value = false
  addFormRef.value?.validateField?.('userId')
}

const resetRecordForm = () => {
  addForm.userId = ''
  selectedCreditUser.value = null
  addForm.creditTypeCode = ''
  addForm.sourceType = ''
  addForm.creditHours = 0
  addForm.creditScore = 0
  addForm.description = ''
  addFormRef.value?.resetFields()
}

const submitAdd = async () => {
  const valid = await addFormRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const res = await addCreditRecord({ ...addForm })
    if (res.code === 200) {
      ElMessage.success('学分记录已添加')
      addDialogVisible.value = false
      fetchList()
      return
    }
    ElMessage.error(res.msg || '添加学分记录失败')
  } catch (error) {
    console.error('添加学分记录失败:', error)
    ElMessage.error('添加学分记录失败')
  } finally {
    submitLoading.value = false
  }
}

const handleApprove = async (row) => {
  try {
    await ElMessageBox.confirm('确定通过该学分记录吗？', '提示', { type: 'success' })
    const res = await approveCreditRecord(row.id)
    if (res.code === 200) {
      ElMessage.success('学分记录已通过')
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
    const res = await rejectCreditRecord(currentRejectId.value, rejectReason.value)
    if (res.code === 200) {
      ElMessage.success('学分记录已驳回')
      rejectDialogVisible.value = false
      fetchList()
      return
    }
    ElMessage.error(res.msg || '操作失败')
  } catch (error) {
    console.error('驳回学分记录失败:', error)
    ElMessage.error('操作失败')
  }
}


const { selectedRows, handleSelectionChange, handleBatchDelete } = useBatchDelete({
  entityName: '学分类型',
  deleteItem: (id) => deleteCreditType(id),
  fetchList: fetchCreditTypes
})

onMounted(() => {
  fetchCreditTypes()
  fetchList()
})
</script>

<style lang="scss" scoped>
.credit-config-page {
  display: grid;
  gap: 16px;
}

.credit-type-card {
  margin-bottom: 0;
}

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

.search-form {
  margin-bottom: 18px;
}

.user-picker-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  width: 100%;
}

.text-muted {
  color: #909399;
}

.el-pagination {
  margin-top: 20px;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
