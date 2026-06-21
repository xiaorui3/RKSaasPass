<template>
  <div class="admin-page nacos-config-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <div>
            <h1>Nacos 配置中心</h1>
            <p>通过后端代理查看和修改 Nacos 配置，避免公网直接暴露 Nacos 控制台。</p>
          </div>
          <el-button type="primary" :loading="loading" @click="fetchConfigs">刷新配置</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="filters" class="filters">
        <el-form-item label="命名空间">
          <el-input v-model.trim="filters.namespaceId" placeholder="默认使用后端配置" style="width: 280px" />
        </el-form-item>
        <el-form-item label="分组">
          <el-input v-model.trim="filters.groupName" placeholder="DEFAULT_GROUP" style="width: 180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchConfigs">查询</el-button>
          <el-button @click="openEditor()">新增配置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="configs" v-loading="loading" stripe empty-text="暂无 Nacos 配置">
        <el-table-column prop="dataId" label="Data ID" min-width="220" />
        <el-table-column prop="groupName" label="分组" min-width="140" />
        <el-table-column prop="type" label="类型" width="100" />
        <el-table-column prop="md5" label="MD5" min-width="180" show-overflow-tooltip />
        <el-table-column prop="message" label="状态" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" :disabled="!row.dataId" @click="openEditor(row)">查看/编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="editorVisible" title="Nacos 配置编辑" width="860px">
      <el-form :model="editor" label-width="110px">
        <el-form-item label="命名空间">
          <el-input v-model.trim="editor.namespaceId" />
        </el-form-item>
        <el-form-item label="分组">
          <el-input v-model.trim="editor.groupName" />
        </el-form-item>
        <el-form-item label="Data ID">
          <el-input v-model.trim="editor.dataId" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="editor.type">
            <el-option label="YAML" value="yaml" />
            <el-option label="Properties" value="properties" />
            <el-option label="JSON" value="json" />
          </el-select>
        </el-form-item>
        <el-form-item label="配置内容">
          <el-input
            v-model="editor.content"
            type="textarea"
            :rows="18"
            placeholder="请输入配置内容"
          />
        </el-form-item>
      </el-form>
      <el-alert
        v-if="saveError"
        class="save-error"
        type="error"
        :closable="false"
        show-icon
        :title="saveError"
      />
      <el-card shadow="never" class="history-card">
        <template #header>
          <div class="card-header compact">
            <span>配置历史</span>
            <el-button :disabled="!editor.dataId" @click="fetchConfigHistory">刷新历史</el-button>
          </div>
        </template>
        <el-table :data="configHistory" size="small" stripe empty-text="暂无历史记录">
          <el-table-column prop="lastModifiedTime" label="时间" min-width="160" />
          <el-table-column prop="operator" label="操作人" width="120" />
          <el-table-column prop="message" label="状态" min-width="160" show-overflow-tooltip />
          <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
        </el-table>
      </el-card>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存到 Nacos</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getNacosConfig, getNacosConfigHistory, listNacosConfigs, saveNacosConfig } from '@/api/admin-ops'

const loading = ref(false)
const saving = ref(false)
const editorVisible = ref(false)
const configs = ref([])
const configHistory = ref([])
const saveError = ref('')
const filters = reactive({
  namespaceId: '',
  groupName: 'DEFAULT_GROUP'
})
const editor = reactive({
  namespaceId: '',
  groupName: 'DEFAULT_GROUP',
  dataId: '',
  type: 'yaml',
  content: ''
})

async function fetchConfigs() {
  loading.value = true
  try {
    const res = await listNacosConfigs({ ...filters })
    if (res.code !== 200 || !Array.isArray(res.data)) {
      throw new Error(res.msg || '读取 Nacos 配置失败')
    }
    configs.value = res.data
  } catch (error) {
    ElMessage.error(error.message || '读取 Nacos 配置失败')
  } finally {
    loading.value = false
  }
}

async function openEditor(row = {}) {
  saveError.value = ''
  configHistory.value = []
  Object.assign(editor, {
    namespaceId: row.namespaceId || filters.namespaceId || '',
    groupName: row.groupName || filters.groupName || 'DEFAULT_GROUP',
    dataId: row.dataId || '',
    type: row.type || 'yaml',
    content: row.content || ''
  })
  editorVisible.value = true
  if (!row.dataId) {
    return
  }
  try {
    const res = await getNacosConfig({
      namespaceId: editor.namespaceId,
      groupName: editor.groupName,
      dataId: editor.dataId
    })
    if (res.code !== 200 || !res.data) {
      throw new Error(res.msg || '读取配置详情失败')
    }
    Object.assign(editor, {
      namespaceId: res.data.namespaceId || editor.namespaceId,
      groupName: res.data.groupName || editor.groupName,
      dataId: res.data.dataId || editor.dataId,
      type: res.data.type || editor.type,
      content: res.data.content || ''
    })
    await fetchConfigHistory()
  } catch (error) {
    ElMessage.error(error.message || '读取配置详情失败')
  }
}

async function fetchConfigHistory() {
  if (!editor.dataId) {
    configHistory.value = []
    return
  }
  try {
    const res = await getNacosConfigHistory({
      namespaceId: editor.namespaceId,
      groupName: editor.groupName,
      dataId: editor.dataId
    })
    configHistory.value = res.code === 200 && Array.isArray(res.data) ? res.data : []
  } catch (error) {
    configHistory.value = [{
      dataId: editor.dataId,
      groupName: editor.groupName,
      message: error.message || '读取历史失败'
    }]
  }
}

async function handleSave() {
  if (!editor.dataId || !editor.groupName) {
    ElMessage.warning('请填写 Data ID 和分组')
    return
  }
  try {
    await ElMessageBox.confirm('确认发布该 Nacos 配置吗？保存后会影响读取该配置的微服务。', '发布配置', {
      confirmButtonText: '确认发布',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  saving.value = true
  saveError.value = ''
  try {
    const res = await saveNacosConfig({ ...editor })
    if (res.code !== 200 || !res.data) {
      throw new Error(res.msg || '保存 Nacos 配置失败')
    }
    ElMessage.success('Nacos 配置已保存')
    editorVisible.value = false
    await fetchConfigs()
    await fetchConfigHistory()
  } catch (error) {
    saveError.value = error.message || '保存 Nacos 配置失败'
    ElMessage.error(error.message || '保存 Nacos 配置失败')
  } finally {
    saving.value = false
  }
}

onMounted(fetchConfigs)
</script>

<style scoped>
.nacos-config-page {
  display: grid;
  gap: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.card-header h1 {
  margin: 0;
  font-size: 24px;
}

.card-header p {
  margin: 8px 0 0;
  color: #909399;
}

.filters {
  margin-bottom: 12px;
}

@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
  }
}
</style>
