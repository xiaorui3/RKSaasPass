<template>
  <div class="wiki-page">
    <div class="wiki-toolbar">
      <div>
        <h2>文档中心</h2>
        <p>社团制度、技术文档和会议纪要统一保存到数据库。</p>
      </div>
      <div class="toolbar-actions">
        <el-input v-model="keyword" clearable placeholder="搜索文档" @keyup.enter="loadDocuments" />
        <el-button @click="loadDocuments" :loading="loading">刷新</el-button>
        <el-button type="primary" @click="createDoc">新建文档</el-button>
      </div>
    </div>

    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="side-card">
          <template #header>文档分类</template>
          <el-menu :default-active="activeCategory" @select="handleCategorySelect">
            <el-menu-item index="">全部文档</el-menu-item>
            <el-menu-item v-for="item in categories" :key="item" :index="item">
              {{ item }}
            </el-menu-item>
          </el-menu>
        </el-card>
      </el-col>
      <el-col :span="18">
        <el-card>
          <template #header>
            <div class="doc-header">
              <span>{{ currentDoc ? currentDoc.title : '文档列表' }}</span>
              <div v-if="currentDoc" class="doc-actions">
                <el-button size="small" @click="editDoc = !editDoc">{{ editDoc ? '预览' : '编辑' }}</el-button>
                <el-button size="small" type="danger" plain @click="removeDoc">删除</el-button>
              </div>
            </div>
          </template>

          <el-table
            v-if="!currentDoc"
            :data="documents"
            v-loading="loading"
            stripe
            empty-text="暂无文档"
            @row-click="openDoc"
          >
            <el-table-column prop="title" label="标题" min-width="180" />
            <el-table-column prop="category" label="分类" width="130" />
            <el-table-column prop="updateTime" label="更新时间" width="180" />
          </el-table>

          <div v-else-if="!editDoc" class="doc-preview" v-html="renderedContent"></div>

          <el-form v-else :model="currentDoc" label-width="80px">
            <el-form-item label="标题">
              <el-input v-model="currentDoc.title" maxlength="200" show-word-limit />
            </el-form-item>
            <el-form-item label="分类">
              <el-input v-model="currentDoc.category" maxlength="100" />
            </el-form-item>
            <el-form-item label="内容">
              <el-input v-model="currentDoc.content" type="textarea" :rows="18" placeholder="支持 Markdown 标题、加粗和换行" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="saveDoc">保存</el-button>
              <el-button @click="currentDoc = null">返回列表</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createWikiDocument, deleteWikiDocument, listWikiDocuments, updateWikiDocument } from '@/api/wiki'

const loading = ref(false)
const saving = ref(false)
const editDoc = ref(false)
const keyword = ref('')
const activeCategory = ref('')
const documents = ref([])
const currentDoc = ref(null)

const categories = computed(() => {
  const values = documents.value.map((item) => item.category || 'default')
  return [...new Set(values)].sort()
})

const renderedContent = computed(() => {
  if (!currentDoc.value) return ''
  return escapeHtml(currentDoc.value.content || '')
    .replace(/^### (.+)$/gm, '<h3>$1</h3>')
    .replace(/^## (.+)$/gm, '<h2>$1</h2>')
    .replace(/^# (.+)$/gm, '<h1>$1</h1>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
})

async function loadDocuments() {
  loading.value = true
  try {
    const res = await listWikiDocuments({
      category: activeCategory.value || undefined,
      keyword: keyword.value || undefined
    })
    documents.value = res.data || []
    if (currentDoc.value?.id) {
      const latest = documents.value.find((item) => item.id === currentDoc.value.id)
      if (latest) currentDoc.value = { ...latest }
    }
  } catch (error) {
    documents.value = []
    ElMessage.error('文档列表加载失败')
  } finally {
    loading.value = false
  }
}

function handleCategorySelect(category) {
  activeCategory.value = category
  currentDoc.value = null
  loadDocuments()
}

function openDoc(row) {
  currentDoc.value = { ...row }
  editDoc.value = false
}

function createDoc() {
  currentDoc.value = {
    id: null,
    title: `新文档-${new Date().getTime()}`,
    category: activeCategory.value || 'default',
    content: '# 新文档\n\n在这里编写内容。'
  }
  editDoc.value = true
}

async function saveDoc() {
  if (!currentDoc.value?.title?.trim()) {
    return ElMessage.warning('请输入文档标题')
  }
  saving.value = true
  try {
    const payload = {
      title: currentDoc.value.title,
      category: currentDoc.value.category || 'default',
      content: currentDoc.value.content || '',
      status: 1
    }
    const res = currentDoc.value.id
      ? await updateWikiDocument(currentDoc.value.id, payload)
      : await createWikiDocument(payload)
    currentDoc.value = { ...(res.data || currentDoc.value) }
    editDoc.value = false
    await loadDocuments()
    ElMessage.success('文档已保存')
  } catch (error) {
    ElMessage.error('文档保存失败')
  } finally {
    saving.value = false
  }
}

async function removeDoc() {
  if (!currentDoc.value?.id) {
    currentDoc.value = null
    return
  }
  try {
    await ElMessageBox.confirm('确定删除该文档吗？', '提示', { type: 'warning' })
    await deleteWikiDocument(currentDoc.value.id)
    currentDoc.value = null
    await loadDocuments()
    ElMessage.success('文档已删除')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('文档删除失败')
    }
  }
}

function escapeHtml(text) {
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

onMounted(loadDocuments)
</script>

<style scoped>
.wiki-page { padding: 20px; }
.wiki-toolbar { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; margin-bottom: 18px; }
.wiki-toolbar h2 { margin: 0 0 6px; }
.wiki-toolbar p { margin: 0; color: #909399; }
.toolbar-actions { display: flex; gap: 10px; align-items: center; }
.toolbar-actions .el-input { width: 220px; }
.side-card { min-height: 420px; }
.doc-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.doc-actions { display: flex; gap: 8px; }
.doc-preview { min-height: 420px; line-height: 1.8; color: #303133; }
.doc-preview :deep(h1) { font-size: 22px; border-bottom: 1px solid #ebeef5; padding-bottom: 8px; }
.doc-preview :deep(h2) { font-size: 18px; margin-top: 20px; }
.doc-preview :deep(h3) { font-size: 16px; margin-top: 16px; }
@media (max-width: 900px) {
  .wiki-toolbar { flex-direction: column; }
  .toolbar-actions { width: 100%; flex-wrap: wrap; }
  .toolbar-actions .el-input { width: 100%; }
}
</style>
