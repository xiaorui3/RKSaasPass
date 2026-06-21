<template>
  <div class="admin-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>校友管理</span>
          <div class="header-actions">
            <el-button v-if="hasPermission('alumni:edit')" :loading="deletedLoading" @click="openDeletedDialog">
              查看已删除
            </el-button>
            <el-button v-if="hasPermission('alumni:add')" type="warning" @click="importDialogVisible = true">
              <el-icon><Upload /></el-icon>Excel导入
            </el-button>
            <el-button v-if="hasPermission('alumni:add')" type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>新增校友
            </el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="姓名">
          <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable />
        </el-form-item>
        <el-form-item label="毕业年份">
          <el-input v-model="searchForm.graduationYear" placeholder="如：2020" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchAlumni">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="batch-delete-toolbar">
        <el-button v-if="hasPermission('alumni:remove')" type="danger" plain :disabled="!selectedRows.length" @click="handleBatchDelete">批量删除</el-button>
      </div>

      <el-table :data="filteredAlumni" v-loading="loading" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="generationYear" label="入学年份" width="100" />
        <el-table-column prop="major" label="专业" />
        <el-table-column prop="workUnit" label="工作单位" />
        <el-table-column prop="position" label="职位" />
        <el-table-column prop="currentContact" label="联系方式" />
        <el-table-column prop="honorCertificates" label="荣誉证书" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="hasPermission('alumni:edit')" text type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="hasPermission('alumni:edit')" text type="primary" @click="handleAchievements(row)">荣誉</el-button>
            <el-button v-if="hasPermission('alumni:remove')" text type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next"
        @change="fetchAlumni"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="formData.name" placeholder="请输入姓名" maxlength="50" />
        </el-form-item>
        <el-form-item label="入学年份" prop="generationYear">
          <el-date-picker
            v-model="formData.generationYear"
            type="year"
            placeholder="选择入学年份"
            format="YYYY"
            value-format="YYYY"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="专业" prop="major">
          <el-input v-model="formData.major" placeholder="请输入专业" maxlength="100" />
        </el-form-item>
        <el-form-item label="工作单位" prop="workUnit">
          <el-input v-model="formData.workUnit" placeholder="请输入工作单位" maxlength="100" />
        </el-form-item>
        <el-form-item label="职位" prop="position">
          <el-input v-model="formData.position" placeholder="请输入职位" maxlength="50" />
        </el-form-item>
        <el-form-item label="联系方式" prop="currentContact">
          <el-input v-model="formData.currentContact" placeholder="请输入联系方式" maxlength="50" />
        </el-form-item>
        <el-form-item label="电子邮箱" prop="email">
          <el-input v-model="formData.email" placeholder="请输入电子邮箱" maxlength="100" />
        </el-form-item>
        <el-form-item label="荣誉证书" prop="honorCertificates">
          <el-input
            v-model="formData.honorCertificates"
            type="textarea"
            :rows="3"
            placeholder="请输入主要荣誉和证书"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="备注" prop="notes">
          <el-input
            v-model="formData.notes"
            type="textarea"
            :rows="3"
            placeholder="请输入备注信息"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="achievementDialogVisible" title="荣誉管理" width="500px">
      <el-form label-width="80px">
        <el-form-item label="校友姓名">
          <el-input :value="currentAlumni.name" disabled />
        </el-form-item>
        <el-form-item label="荣誉证书">
          <el-input
            v-model="achievementForm.honorCertificates"
            type="textarea"
            :rows="4"
            placeholder="请输入荣誉证书信息"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="achievementDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAchievements">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importDialogVisible" title="Excel导入校友" width="560px" :close-on-click-modal="false">
      <div class="import-hint">
        <p>支持 <code>.csv / .xlsx / .xls</code>。推荐列：</p>
        <code>name,generationYear,major,workUnit,position,currentContact,email,honorCertificates,notes</code>
      </div>
      <el-upload
        :auto-upload="false"
        :show-file-list="true"
        accept=".csv,.xlsx,.xls"
        :limit="1"
        :on-change="handleImportFileChange"
        :on-remove="handleImportFileRemove"
      >
        <el-button type="primary">选择导入文件</el-button>
      </el-upload>
      <template #footer>
        <el-button @click="closeImportDialog">取消</el-button>
        <el-button type="primary" :disabled="!importFile" :loading="importLoading" @click="handleImportSubmit">开始导入</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="deletedDialogVisible" title="已删除校友" width="920px" :close-on-click-modal="false">
      <el-table :data="deletedAlumni" v-loading="deletedLoading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="姓名" min-width="120" />
        <el-table-column prop="generationYear" label="入学年份" width="110" />
        <el-table-column prop="major" label="专业" min-width="160" show-overflow-tooltip />
        <el-table-column prop="workUnit" label="工作单位" min-width="180" show-overflow-tooltip />
        <el-table-column prop="updateTime" label="删除时间" min-width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" :loading="restoringId === row.id" @click="handleRestoreAlumni(row)">恢复</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="deletedDialogVisible = false">关闭</el-button>
        <el-button :loading="deletedLoading" @click="fetchDeletedAlumni">刷新</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useBatchDelete } from '@/utils/batchDelete'
import { Plus, Upload } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { hasPermission } from '@/utils/permission'
import { getDeletedAlumni, importAlumniExcel, restoreAlumni } from '@/api/alumni'

const loading = ref(false)
const submitLoading = ref(false)
const importLoading = ref(false)
const deletedLoading = ref(false)
const alumniList = ref([])
const deletedAlumni = ref([])
const deletedDialogVisible = ref(false)
const restoringId = ref(null)
const searchForm = reactive({ name: '', graduationYear: '' })
const pagination = reactive({ page: 1, size: 10, total: 0 })

const dialogVisible = ref(false)
const importDialogVisible = ref(false)
const formRef = ref(null)
const isEdit = ref(false)
const importFile = ref(null)

const formData = reactive({
  id: null,
  name: '',
  generationYear: null,
  major: '',
  workUnit: '',
  position: '',
  currentContact: '',
  email: '',
  honorCertificates: '',
  notes: ''
})

const formRules = {
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  generationYear: [
    { required: true, message: '请选择入学年份', trigger: 'change' }
  ],
  major: [
    { required: true, message: '请输入专业', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

const dialogTitle = computed(() => isEdit.value ? '编辑校友' : '新增校友')
const achievementDialogVisible = ref(false)
const currentAlumni = ref({})
const achievementForm = reactive({ honorCertificates: '' })

const filteredAlumni = computed(() => {
  let rows = alumniList.value
  if (searchForm.name) {
    rows = rows.filter((item) => String(item.name || '').includes(searchForm.name))
  }
  if (searchForm.graduationYear) {
    rows = rows.filter((item) => String(item.generationYear || '') === String(searchForm.graduationYear))
  }
  pagination.total = rows.length
  const start = (pagination.page - 1) * pagination.size
  return rows.slice(start, start + pagination.size)
})

const resetForm = () => {
  formData.id = null
  formData.name = ''
  formData.generationYear = null
  formData.major = ''
  formData.workUnit = ''
  formData.position = ''
  formData.currentContact = ''
  formData.email = ''
  formData.honorCertificates = ''
  formData.notes = ''
  formRef.value?.resetFields()
}

const fetchAlumni = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/alumni/list')
    if (res.code === 200 || res.code === 0) {
      alumniList.value = res.data || []
    } else {
      throw new Error(res.msg || '获取校友列表失败')
    }
  } catch (error) {
    console.error('获取校友列表失败:', error)
    alumniList.value = []
    ElMessage.error(error.message || '获取校友列表失败')
  } finally {
    loading.value = false
  }
}

const fetchDeletedAlumni = async () => {
  deletedLoading.value = true
  try {
    const res = await getDeletedAlumni()
    deletedAlumni.value = (res.code === 200 || res.code === 0) && Array.isArray(res.data) ? res.data : []
  } catch (error) {
    console.error('获取已删除校友失败:', error)
    deletedAlumni.value = []
    ElMessage.error(error.message || '获取已删除校友失败')
  } finally {
    deletedLoading.value = false
  }
}

const openDeletedDialog = async () => {
  deletedDialogVisible.value = true
  await fetchDeletedAlumni()
}

const handleRestoreAlumni = async (row) => {
  if (!row?.id) return
  try {
    await ElMessageBox.confirm(`确定要恢复校友 ${row.name || row.id} 吗？`, '提示', { type: 'warning' })
    restoringId.value = row.id
    const res = await restoreAlumni(row.id)
    if (res.code === 200 || res.code === 0) {
      ElMessage.success('恢复成功')
      await Promise.all([fetchAlumni(), fetchDeletedAlumni()])
      return
    }
    throw new Error(res.msg || '恢复失败')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '恢复失败')
    }
  } finally {
    restoringId.value = null
  }
}

const resetSearch = () => {
  searchForm.name = ''
  searchForm.graduationYear = ''
  pagination.page = 1
}

const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  resetForm()
  Object.keys(formData).forEach((key) => {
    if (row[key] !== undefined) {
      formData[key] = row[key]
    }
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const url = isEdit.value ? '/api/alumni/update' : '/api/alumni/add'
    const method = isEdit.value ? 'put' : 'post'
    const res = await request[method](url, formData)
    if (res.code === 200 || res.code === 0) {
      ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
      dialogVisible.value = false
      await fetchAlumni()
      return
    }
    throw new Error(res.msg || '保存失败')
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    submitLoading.value = false
  }
}

const handleAchievements = (row) => {
  currentAlumni.value = row
  achievementForm.honorCertificates = row.honorCertificates || ''
  achievementDialogVisible.value = true
}

const saveAchievements = async () => {
  try {
    const res = await request.put('/api/alumni/update', {
      id: currentAlumni.value.id,
      honorCertificates: achievementForm.honorCertificates
    })
    if (res.code === 200 || res.code === 0) {
      ElMessage.success('保存成功')
      achievementDialogVisible.value = false
      await fetchAlumni()
      return
    }
    throw new Error(res.msg || '保存失败')
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定要删除校友 ${row.name} 吗？`, '提示', { type: 'warning' })
  try {
    const res = await request.delete(`/api/alumni/delete/${row.id}`)
    if (res.code === 200 || res.code === 0) {
      ElMessage.success('删除成功')
      await fetchAlumni()
      return
    }
    throw new Error(res.msg || '删除失败')
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

const handleImportFileChange = (file) => {
  importFile.value = file.raw || null
}

const handleImportFileRemove = () => {
  importFile.value = null
}

const closeImportDialog = () => {
  importDialogVisible.value = false
  importFile.value = null
}

const handleImportSubmit = async () => {
  if (!importFile.value) return
  importLoading.value = true
  try {
    const res = await importAlumniExcel(importFile.value)
    if (res.code === 200 || res.code === 0) {
      ElMessage.success(res.data || res.msg || '导入成功')
      closeImportDialog()
      await fetchAlumni()
      return
    }
    throw new Error(res.msg || '导入失败')
  } catch (error) {
    ElMessage.error(error.message || '导入失败')
  } finally {
    importLoading.value = false
  }
}


const deleteAlumniRow = (id) => request.delete(`/api/alumni/delete/${id}`)
const { selectedRows, handleSelectionChange, handleBatchDelete } = useBatchDelete({
  entityName: '校友',
  deleteItem: (id) => deleteAlumniRow(id),
  fetchList: fetchAlumni
})

onMounted(() => fetchAlumni())
</script>

<style lang="scss" scoped>
.admin-page {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .header-actions {
    display: flex;
    gap: 12px;
  }

  .search-form {
    margin-bottom: 20px;
  }

  .import-hint {
    margin-bottom: 16px;
    color: #606266;
  }

  .el-pagination {
    margin-top: 20px;
    justify-content: flex-end;
  }
}
</style>
