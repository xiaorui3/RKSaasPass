<template>
  <div class="admin-contact-messages">
    <div class="page-header">
      <div>
        <h2>留言管理台</h2>
        <p>集中查看当前租户的站内咨询、合作留言和管理员回复处理进度。</p>
      </div>
      <div class="header-actions">
        <el-input
          v-model="keyword"
          clearable
          placeholder="搜索姓名、邮箱、主题或内容"
          style="width: 260px"
        />
        <el-select v-model="filterStatus" clearable placeholder="处理状态" style="width: 140px">
          <el-option
            v-for="option in statusOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
        <el-button :loading="loading" @click="loadData">刷新</el-button>
      </div>
    </div>

    <section class="stats-panel">
      <div class="stats-header">
        <h3>统计概览</h3>
        <span>用于快速判断当天消息量和待处理积压</span>
      </div>
      <div class="stats-grid">
        <article class="stat-card">
          <span>总留言</span>
          <strong>{{ stats.total }}</strong>
        </article>
        <article class="stat-card">
          <span>待处理</span>
          <strong>{{ stats.pendingCount }}</strong>
        </article>
        <article class="stat-card">
          <span>今日新增</span>
          <strong>{{ stats.todayCount }}</strong>
        </article>
        <article class="stat-card">
          <span>近 7 天</span>
          <strong>{{ stats.weekCount }}</strong>
        </article>
      </div>
    </section>

          <div class="batch-delete-toolbar">
        <el-button type="danger" plain :disabled="!selectedRows.length" @click="handleBatchDelete">批量删除</el-button>
      </div>

      <el-table :data="filteredMessages" v-loading="loading" stripe class="messages-table" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
      <template #empty>
        <el-empty description="当前没有匹配的留言记录" />
      </template>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="联系人" min-width="120" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column prop="subject" label="主题" min-width="160" />
      <el-table-column label="处理状态" width="120">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="提交时间" min-width="170" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <div class="row-actions">
            <el-button size="small" @click="handleView(row)">查看</el-button>
            <el-button size="small" type="primary" @click="handleReply(row)">回复</el-button>
            <el-dropdown @command="(command) => handleStatusChange(row, command)">
              <el-button size="small">
                更新状态
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="option in statusOptions"
                    :key="option.value"
                    :command="option.value"
                  >
                    {{ option.label }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="detailVisible" title="留言详情" width="720px">
      <div v-if="currentMsg" class="msg-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="联系人">{{ currentMsg.name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ currentMsg.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机">{{ currentMsg.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理状态">
            <el-tag :type="getStatusType(currentMsg.status)">
              {{ getStatusText(currentMsg.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="主题" :span="2">{{ currentMsg.subject || '-' }}</el-descriptions-item>
          <el-descriptions-item label="留言内容" :span="2">{{ currentMsg.message || '-' }}</el-descriptions-item>
          <el-descriptions-item label="管理员回复" :span="2">
            {{ currentMsg.response || '暂无回复' }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

    <el-dialog v-model="replyVisible" title="回复留言" width="560px">
      <el-form label-position="top">
        <el-form-item label="回复内容">
          <el-input
            v-model="replyContent"
            type="textarea"
            :rows="5"
            maxlength="1000"
            show-word-limit
            placeholder="请输入管理员回复内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="replying" @click="submitReply">发送回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useBatchDelete } from '@/utils/batchDelete'
import {
  deleteContact,
  getContactList,
  getContactStatistics,
  getContactsByStatus,
  getStatusText,
  getStatusType,
  replyContact,
  updateContactStatus
} from '@/api/contact'

const statusOptions = [
  { label: '待处理', value: 'pending' },
  { label: '处理中', value: 'processing' },
  { label: '已回复', value: 'replied' },
  { label: '已关闭', value: 'closed' }
]

const loading = ref(false)
const replying = ref(false)
const messages = ref([])
const filterStatus = ref('')
const keyword = ref('')
const detailVisible = ref(false)
const replyVisible = ref(false)
const currentMsg = ref(null)
const replyContent = ref('')
const stats = ref({
  total: 0,
  pendingCount: 0,
  todayCount: 0,
  weekCount: 0
})

const filteredMessages = computed(() => {
  const rawKeyword = keyword.value.trim().toLowerCase()

  return messages.value.filter((item) => {
    const matchStatus = !filterStatus.value || item.status === filterStatus.value
    if (!matchStatus) {
      return false
    }
    if (!rawKeyword) {
      return true
    }
    const haystack = [
      item.name,
      item.email,
      item.subject,
      item.message,
      item.response
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()
    return haystack.includes(rawKeyword)
  })
})

async function loadData() {
  loading.value = true
  try {
    const [listRes, statsRes] = await Promise.all([
      filterStatus.value ? getContactsByStatus(filterStatus.value) : getContactList(),
      getContactStatistics()
    ])
    messages.value = Array.isArray(listRes?.data) ? listRes.data : []
    stats.value = {
      total: Number(statsRes?.data?.total || 0),
      pendingCount: Number(statsRes?.data?.pendingCount || 0),
      todayCount: Number(statsRes?.data?.todayCount || 0),
      weekCount: Number(statsRes?.data?.weekCount || 0)
    }
  } catch (error) {
    console.error('load contact messages failed', error)
    messages.value = []
  } finally {
    loading.value = false
  }
}

function handleView(row) {
  currentMsg.value = row
  detailVisible.value = true
}

function handleReply(row) {
  currentMsg.value = row
  replyContent.value = row.response || ''
  replyVisible.value = true
}

async function submitReply() {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  if (!currentMsg.value?.id) {
    return
  }

  replying.value = true
  try {
    await replyContact(currentMsg.value.id, replyContent.value.trim())
    ElMessage.success('回复已发送')
    replyVisible.value = false
    await loadData()
  } catch (error) {
    console.error('reply contact failed', error)
    ElMessage.error('回复失败，请稍后重试')
  } finally {
    replying.value = false
  }
}

async function handleStatusChange(row, status) {
  if (!row?.id || !status || row.status === status) {
    return
  }
  try {
    await updateContactStatus(row.id, status)
    ElMessage.success(`状态已更新为${getStatusText(status)}`)
    await loadData()
  } catch (error) {
    console.error('update contact status failed', error)
    ElMessage.error('状态更新失败')
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除这条留言记录吗？删除后不可恢复。', '删除确认', {
      type: 'warning'
    })
    await deleteContact(row.id)
    ElMessage.success('删除成功')
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('delete contact failed', error)
    }
  }
}


const { selectedRows, handleSelectionChange, handleBatchDelete } = useBatchDelete({
  entityName: '留言',
  deleteItem: (id) => deleteContact(id),
  fetchList: loadData
})

onMounted(loadData)
</script>

<style scoped>
.admin-contact-messages {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 8px;
}

.page-header p {
  margin: 0;
  color: #6b7280;
  line-height: 1.6;
}

.header-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.stats-panel {
  margin-bottom: 20px;
  border-radius: 18px;
  border: 1px solid #e5e7eb;
  background: linear-gradient(180deg, #ffffff, #f8fafc);
  padding: 18px 20px;
}

.stats-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.stats-header h3 {
  margin: 0;
  font-size: 16px;
}

.stats-header span {
  color: #6b7280;
  font-size: 13px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.stat-card {
  border-radius: 14px;
  background: #ffffff;
  border: 1px solid #edf2f7;
  padding: 16px;
}

.stat-card span {
  display: block;
  color: #6b7280;
  font-size: 13px;
}

.stat-card strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  color: #111827;
}

.messages-table {
  border-radius: 16px;
  overflow: hidden;
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.msg-detail {
  padding-top: 6px;
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .admin-contact-messages {
    padding: 12px;
  }

  .page-header,
  .stats-header {
    flex-direction: column;
  }

  .header-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
