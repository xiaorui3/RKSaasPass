<template>
  <div class="admin-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>系统配置</span>
          <el-button type="primary" @click="handleAdd">新增配置</el-button>
        </div>
      </template>

      <div class="batch-delete-toolbar">
        <el-button type="danger" plain :disabled="!selectedRows.length" @click="handleBatchDelete">批量删除</el-button>
      </div>

      <el-table :data="configs" v-loading="loading" stripe empty-text="暂无系统配置" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="displayName" label="配置名称" min-width="180" />
        <el-table-column prop="configKey" label="配置键" min-width="220" />
        <el-table-column prop="configValue" label="配置值" min-width="260" show-overflow-tooltip />
        <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button text type="warning" @click="handleToggle(row)">切换状态</el-button>
            <el-button text type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="配置名称">
          <el-input :model-value="resolveDisplayName(form.configKey)" disabled />
        </el-form-item>
        <el-form-item label="配置键" prop="configKey">
          <el-input v-model="form.configKey" placeholder="请输入配置键" />
        </el-form-item>
        <el-form-item label="配置值" prop="configValue">
          <el-input v-model="form.configValue" type="textarea" :rows="4" placeholder="请输入配置值" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" placeholder="请输入配置说明" />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="form.isEnabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useBatchDelete } from '@/utils/batchDelete'
import {
  createSystemConfig,
  deleteSystemConfig,
  getSystemConfigList,
  toggleSystemConfig,
  updateSystemConfig
} from '@/api/system-config'

const loading = ref(false)
const configs = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增配置')
const formRef = ref(null)

const DISPLAY_NAME_MAP = {
  'admission.form.config': '入社表单配置',
  'club.profile.config': '社团概况配置'
}

const form = reactive({
  id: null,
  configKey: '',
  configValue: '',
  description: '',
  isEnabled: true
})

const rules = {
  configKey: [{ required: true, message: '请输入配置键', trigger: 'blur' }],
  configValue: [{ required: true, message: '请输入配置值', trigger: 'blur' }]
}

function resolveDisplayName(configKey) {
  return DISPLAY_NAME_MAP[configKey] || '自定义系统配置'
}

async function fetchConfigs() {
  loading.value = true
  try {
    const res = await getSystemConfigList()
    configs.value = res.code === 200
      ? (res.data || []).map((item) => ({
          ...item,
          displayName: resolveDisplayName(item.configKey),
          status: item.isEnabled ? 1 : 0
        }))
      : []
  } catch (error) {
    ElMessage.error(error.message || '获取系统配置失败')
    configs.value = []
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    id: null,
    configKey: '',
    configValue: '',
    description: '',
    isEnabled: true
  })
}

function handleAdd() {
  resetForm()
  dialogTitle.value = '新增配置'
  dialogVisible.value = true
}

function handleEdit(row) {
  resetForm()
  dialogTitle.value = '编辑配置'
  Object.assign(form, {
    id: row.id,
    configKey: row.configKey,
    configValue: row.configValue,
    description: row.description || '',
    isEnabled: !!row.isEnabled
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate()
  const payload = {
    configKey: form.configKey,
    configValue: form.configValue,
    description: form.description,
    isEnabled: form.isEnabled
  }
  try {
    if (form.id) {
      await updateSystemConfig(form.id, payload)
      ElMessage.success('修改成功')
    } else {
      await createSystemConfig(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await fetchConfigs()
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  }
}

async function handleToggle(row) {
  try {
    await toggleSystemConfig(row.id)
    ElMessage.success('状态已切换')
    await fetchConfigs()
  } catch (error) {
    ElMessage.error(error.message || '切换状态失败')
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除配置 ${row.configKey} 吗？`, '警告', { type: 'warning' })
  try {
    await deleteSystemConfig(row.id)
    ElMessage.success('删除成功')
    await fetchConfigs()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}


const { selectedRows, handleSelectionChange, handleBatchDelete } = useBatchDelete({
  entityName: '配置',
  deleteItem: (id) => deleteSystemConfig(id),
  fetchList: fetchConfigs
})

onMounted(() => {
  fetchConfigs()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
