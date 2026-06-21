<template>
  <div class="admin-page history-manage-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <div>
            <h1>社团历程管理</h1>
            <p>维护前台“社团历程”时间线，支持里程碑、排序和启停展示。</p>
          </div>
          <el-button type="primary" @click="openCreate">新增历程</el-button>
        </div>
      </template>

      <div class="toolbar">
        <el-input v-model="query.keyword" clearable placeholder="搜索标题或描述" @keyup.enter="fetchPage" />
        <el-input-number v-model="query.year" :min="1900" :max="2100" clearable placeholder="年份" />
        <el-button @click="fetchPage">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>

      <div class="batch-delete-toolbar">
        <el-button type="danger" plain :disabled="!selectedRows.length" @click="handleBatchDelete">批量删除</el-button>
      </div>

      <el-table v-loading="loading" :data="records" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="year" label="年份" width="90" />
        <el-table-column prop="date" label="日期" width="130" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="importance" label="重要度" width="90" />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.isActive ? 'success' : 'info'">{{ row.isActive ? '展示' : '隐藏' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="里程碑" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.isMilestone" type="warning">是</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.current"
        v-model:page-size="query.size"
        class="pagination"
        layout="total, sizes, prev, pager, next"
        :total="total"
        @current-change="fetchPage"
        @size-change="fetchPage"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑历程' : '新增历程'" width="720px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="事件日期" prop="eventDate">
          <el-date-picker v-model="form.eventDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="年份">
          <el-input-number v-model="form.year" :min="1900" :max="2100" />
        </el-form-item>
        <el-form-item label="类型">
          <el-input v-model="form.eventType" placeholder="如：成立、活动、荣誉" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="封面图">
          <el-input v-model="form.coverImageUrl" placeholder="图片 URL，可选" />
        </el-form-item>
        <el-form-item label="展示控制">
          <el-switch v-model="form.isActive" active-text="展示" inactive-text="隐藏" />
          <el-switch v-model="form.isMilestone" class="milestone-switch" active-text="里程碑" />
        </el-form-item>
        <el-form-item label="排序/重要度">
          <el-input-number v-model="form.sortOrder" :min="0" />
          <el-input-number v-model="form.importanceLevel" class="importance-input" :min="1" :max="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useBatchDelete } from '@/utils/batchDelete'
import { deleteAdminHistoryEvent, getAdminHistoryPage, saveAdminHistoryEvent } from '@/api/history'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const records = ref([])
const total = ref(0)

const query = reactive({ keyword: '', year: undefined, current: 1, size: 10 })
const form = reactive(createEmptyForm())

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  eventDate: [{ required: true, message: '请选择事件日期', trigger: 'change' }],
  description: [{ required: true, message: '请输入描述', trigger: 'blur' }]
}

function createEmptyForm() {
  const year = new Date().getFullYear()
  return {
    id: undefined,
    title: '',
    eventDate: `${year}-01-01`,
    year,
    description: '',
    eventType: 'general',
    importanceLevel: 1,
    coverImageUrl: '',
    isMilestone: false,
    isActive: true,
    sortOrder: 0
  }
}

function applyForm(data = {}) {
  Object.assign(form, createEmptyForm(), {
    id: data.id,
    title: data.title || '',
    eventDate: data.eventDate || data.date || createEmptyForm().eventDate,
    year: data.year || (data.eventDate ? Number(String(data.eventDate).slice(0, 4)) : new Date().getFullYear()),
    description: data.description || '',
    eventType: data.eventType || data.type || 'general',
    importanceLevel: data.importanceLevel || data.importance || 1,
    coverImageUrl: data.coverImageUrl || data.coverImage || '',
    isMilestone: !!data.isMilestone,
    isActive: data.isActive !== false,
    sortOrder: data.sortOrder || 0
  })
}

async function fetchPage() {
  loading.value = true
  try {
    const res = await getAdminHistoryPage({ ...query })
    const data = res.data || {}
    records.value = data.records || []
    total.value = Number(data.total || 0)
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.keyword = ''
  query.year = undefined
  query.current = 1
  fetchPage()
}

function openCreate() {
  applyForm()
  dialogVisible.value = true
}

function openEdit(row) {
  applyForm(row)
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload = { ...form, year: form.year || Number(String(form.eventDate).slice(0, 4)) }
    const res = await saveAdminHistoryEvent(payload)
    if (res.code === 200) {
      ElMessage.success('历程已保存')
      dialogVisible.value = false
      fetchPage()
    }
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除“${row.title}”吗？`, '删除确认', { type: 'warning' })
  const res = await deleteAdminHistoryEvent(row.id)
  if (res.code === 200) {
    ElMessage.success('历程已删除')
    fetchPage()
  }
}


const { selectedRows, handleSelectionChange, handleBatchDelete } = useBatchDelete({
  entityName: '历程',
  deleteItem: (id) => deleteAdminHistoryEvent(id),
  fetchList: fetchPage
})

onMounted(fetchPage)
</script>

<style scoped>
.card-header,
.toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.card-header h1 {
  margin: 0;
  font-size: 24px;
}

.card-header p {
  margin: 8px 0 0;
  color: #909399;
}

.toolbar {
  justify-content: flex-start;
  margin-bottom: 16px;
}

.toolbar .el-input {
  max-width: 280px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.milestone-switch,
.importance-input {
  margin-left: 16px;
}
</style>
