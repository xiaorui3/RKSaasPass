<template>
  <div class="admin-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>内推码管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            生成内推码
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="内推码">
          <el-input v-model="searchForm.code" placeholder="请输入内推码" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="有效" :value="1" />
            <el-option label="失效" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchReferralCodes">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="batch-delete-toolbar">
        <el-button type="danger" plain :disabled="!selectedRows.length" @click="handleBatchDelete">批量删除</el-button>
      </div>

      <el-table :data="referralCodes" v-loading="loading" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="code" label="内推码" width="160">
          <template #default="{ row }">
            <el-tag type="success">{{ row.code }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="maxUses" label="最大使用次数" width="140" />
        <el-table-column label="已使用次数" width="180">
          <template #default="{ row }">
            <el-progress
              :percentage="Math.round(((row.usedCount || 0) / Math.max(row.maxUses || 1, 1)) * 100)"
              :color="getProgressColor(row.usedCount || 0, row.maxUses || 1)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="expiresAt" label="过期时间" width="180">
          <template #default="{ row }">
            {{ row.expiresAt ? formatDate(row.expiresAt) : '永久有效' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '有效' : '失效' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button
              text
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button text type="info" @click="handleViewConversions(row)">转化记录</el-button>
            <el-button text type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @change="fetchReferralCodes"
      />
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
        <el-form-item label="内推码" prop="code">
          <el-input
            v-model="formData.code"
            placeholder="请输入内推码"
            maxlength="20"
            :disabled="isEdit"
          >
            <template #append>
              <el-button @click="handleGenerateCode" :disabled="isEdit">生成随机码</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="最大使用次数" prop="maxUses">
          <el-input-number v-model="formData.maxUses" :min="1" :max="1000" />
        </el-form-item>
        <el-form-item label="过期时间" prop="expiresAt">
          <el-date-picker
            v-model="formData.expiresAt"
            type="datetime"
            placeholder="选择过期时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :label="1">有效</el-radio>
            <el-radio :label="0">失效</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="conversionDialogVisible"
      title="内推转化记录"
      width="900px"
      :close-on-click-modal="false"
    >
      <div v-if="conversionOverview" class="conversion-summary">
        <el-row :gutter="16">
          <el-col :span="6">
            <el-statistic title="打开数" :value="conversionOverview.openedCount || 0" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="注册成功数" :value="conversionOverview.registerSuccessCount || 0" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="入社通过数" :value="conversionOverview.joinApprovedCount || 0" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="已使用次数" :value="conversionOverview.usedCount || 0" />
          </el-col>
        </el-row>
      </div>

      <el-table :data="conversionRecords" v-loading="conversionLoading" stripe style="margin-top: 20px">
        <el-table-column prop="targetEmail" label="目标邮箱" min-width="220" />
        <el-table-column prop="conversionType" label="类型" width="120" />
        <el-table-column prop="conversionStatus" label="状态" width="160" />
        <el-table-column prop="authUserId" label="Auth用户ID" width="120" />
        <el-table-column prop="joinRequestId" label="申请ID" width="120" />
        <el-table-column prop="convertedTime" label="时间" min-width="180" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useBatchDelete } from '@/utils/batchDelete'
import {
  getReferralCodes,
  createReferralCode,
  updateReferralCode,
  deleteReferralCode,
  generateUniqueCode,
  getReferralConversionOverview
} from '@/api/referral'

const loading = ref(false)
const referralCodes = ref([])
const dialogVisible = ref(false)
const conversionDialogVisible = ref(false)
const conversionLoading = ref(false)
const conversionOverview = ref(null)
const conversionRecords = ref([])
const isEdit = ref(false)
const formRef = ref(null)

const searchForm = reactive({
  code: '',
  status: null
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const formData = reactive({
  id: null,
  code: '',
  maxUses: 10,
  expiresAt: null,
  status: 1
})

const formRules = {
  code: [
    { required: true, message: '内推码不能为空', trigger: 'blur' },
    { min: 3, max: 20, message: '内推码长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  maxUses: [
    { required: true, message: '最大使用次数不能为空', trigger: 'blur' }
  ]
}

const dialogTitle = computed(() => (isEdit.value ? '编辑内推码' : '生成内推码'))

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

const getProgressColor = (used, max) => {
  const percentage = (used / max) * 100
  if (percentage >= 80) return '#f56c6c'
  if (percentage >= 50) return '#e6a23c'
  return '#67c23a'
}

const fetchReferralCodes = async () => {
  loading.value = true
  try {
    const response = await getReferralCodes({
      page: pagination.page,
      size: pagination.size,
      ...searchForm
    })
    if (response.code === 200) {
      referralCodes.value = response.data.records || []
      pagination.total = response.data.total || 0
    } else {
      ElMessage.error(response.msg || '获取内推码列表失败')
    }
  } catch (error) {
    console.error('获取内推码列表失败:', error)
    ElMessage.error('获取内推码列表失败')
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.code = ''
  searchForm.status = null
  pagination.page = 1
  fetchReferralCodes()
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(formData, {
    id: null,
    code: '',
    maxUses: 10,
    expiresAt: null,
    status: 1
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(formData, {
    id: row.id,
    code: row.code,
    maxUses: row.maxUses,
    expiresAt: row.expiresAt,
    status: row.status
  })
  dialogVisible.value = true
}

const handleGenerateCode = async () => {
  try {
    const response = await generateUniqueCode()
    if (response.code === 200) {
      formData.code = response.data
      ElMessage.success('生成随机内推码成功')
    } else {
      ElMessage.error(response.msg || '生成随机内推码失败')
    }
  } catch (error) {
    console.error('生成随机内推码失败:', error)
    ElMessage.error('生成随机内推码失败')
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      const response = isEdit.value
        ? await updateReferralCode(formData.id, {
            maxUses: formData.maxUses,
            expiresAt: formData.expiresAt,
            status: formData.status
          })
        : await createReferralCode(formData)

      if (response.code === 200) {
        ElMessage.success(isEdit.value ? '更新成功' : '生成成功')
        dialogVisible.value = false
        fetchReferralCodes()
      } else {
        ElMessage.error(response.msg || '操作失败')
      }
    } catch (error) {
      console.error('操作失败:', error)
      ElMessage.error('操作失败')
    }
  })
}

const handleToggleStatus = async (row) => {
  const action = row.status === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}内推码 ${row.code} 吗？`, '提示', {
      type: 'warning'
    })

    const response = await updateReferralCode(row.id, {
      status: row.status === 1 ? 0 : 1
    })

    if (response.code === 200) {
      ElMessage.success(`${action}成功`)
      fetchReferralCodes()
    } else {
      ElMessage.error(response.msg || `${action}失败`)
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error(`${action}失败:`, error)
      ElMessage.error(`${action}失败`)
    }
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除内推码 ${row.code} 吗？此操作不可恢复。`, '警告', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })

    const response = await deleteReferralCode(row.id)
    if (response.code === 200) {
      ElMessage.success('删除成功')
      fetchReferralCodes()
    } else {
      ElMessage.error(response.msg || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleViewConversions = async (row) => {
  conversionDialogVisible.value = true
  conversionLoading.value = true
  try {
    const response = await getReferralConversionOverview(row.id)
    if (response.code === 200) {
      conversionOverview.value = response.data
      conversionRecords.value = response.data?.records || []
    } else {
      ElMessage.error(response.msg || '获取转化记录失败')
    }
  } catch (error) {
    console.error('获取转化记录失败:', error)
    ElMessage.error('获取转化记录失败')
  } finally {
    conversionLoading.value = false
  }
}


const { selectedRows, handleSelectionChange, handleBatchDelete } = useBatchDelete({
  entityName: '内推码',
  deleteItem: (id) => deleteReferralCode(id),
  fetchList: fetchReferralCodes
})

onMounted(() => {
  fetchReferralCodes()
})
</script>

<style scoped>
.admin-page {
  padding: 20px;
}

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

.conversion-summary {
  margin-bottom: 12px;
}
</style>
