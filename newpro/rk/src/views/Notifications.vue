<template>
  <div class="notifications-page">
    <user-hub-shell
      eyebrow="消息中心"
      title="消息通知"
      description="统一查看与你相关的系统通知、审批提醒和账号消息，并在同一控制台内完成已读处理。"
      :meta-items="notificationMetaItems"
      :stat-items="notificationStats"
    >
      <template #actions>
        <el-button type="primary" plain :disabled="unreadCount === 0" @click="markAllRead">
          全部标记为已读
        </el-button>
      </template>

      <template #filters>
        <div class="notification-toolbar">
          <div class="toolbar-copy">
            <span class="toolbar-copy__label">筛选视图</span>
            <strong>切换全部、未读和已读视图，快速定位待处理消息。</strong>
          </div>
          <div class="toolbar-controls">
            <el-radio-group v-model="readFilter" @change="fetchNotifications">
              <el-radio-button label="all">全部</el-radio-button>
              <el-radio-button label="unread">未读</el-radio-button>
              <el-radio-button label="read">已读</el-radio-button>
            </el-radio-group>
            <div class="toolbar-status">
              <el-tag type="danger" effect="light">未读 {{ unreadCount }}</el-tag>
              <el-tag type="warning" effect="light">高优先级 {{ highPriorityCount }}</el-tag>
            </div>
          </div>
        </div>
      </template>

      <user-hub-panel title="收件箱" description="点击任一消息卡片查看完整内容，并在详情中完成已读处理。">
        <div v-loading="loading" class="notice-list" data-testid="notifications-list">
          <el-empty v-if="notifications.length === 0" description="暂无通知" />
          <article
            v-for="item in notifications"
            :key="item.id"
            class="notice-card"
            :class="{ unread: !item.read }"
            @click="openDetail(item)"
          >
            <div class="notice-indicator">
              <el-icon><Bell /></el-icon>
            </div>
            <div class="notice-main">
              <div class="notice-header">
                <span class="notice-title">{{ item.title }}</span>
                <div class="meta-tags">
                  <el-tag size="small" :type="item.read ? 'info' : 'danger'">
                    {{ item.read ? '已读' : '未读' }}
                  </el-tag>
                  <el-tag size="small" :type="priorityTag(item.priority)">
                    {{ priorityText(item.priority) }}
                  </el-tag>
                </div>
              </div>
              <div class="notice-content">
                {{ getExcerpt(item.content) }}
              </div>
              <div class="notice-footer">
                <span>
                  <el-icon><MessageBox /></el-icon>
                  {{ item.senderName || '系统' }}
                </span>
                <span>
                  <el-icon><Clock /></el-icon>
                  {{ item.createTime || '-' }}
                </span>
              </div>
            </div>
          </article>
        </div>
      </user-hub-panel>

      <template #aside>
        <user-hub-panel title="处理概览" description="快速判断当前消息积压情况和处理进度。">
          <div class="aside-metrics">
            <div class="aside-metric">
              <span class="aside-metric__label">消息总数</span>
              <strong class="aside-metric__value">{{ notifications.length }}</strong>
            </div>
            <div class="aside-metric">
              <span class="aside-metric__label">未读消息</span>
              <strong class="aside-metric__value danger">{{ unreadCount }}</strong>
            </div>
            <div class="aside-metric">
              <span class="aside-metric__label">已读消息</span>
              <strong class="aside-metric__value">{{ readCount }}</strong>
            </div>
            <div class="aside-metric">
              <span class="aside-metric__label">高优先级</span>
              <strong class="aside-metric__value warning">{{ highPriorityCount }}</strong>
            </div>
          </div>
        </user-hub-panel>

        <user-hub-panel title="优先级说明" description="按优先级安排处理顺序。">
          <div class="priority-guide">
            <div class="priority-row">
              <el-tag size="small" type="danger">高优先级</el-tag>
              <span>建议优先处理，通常涉及审批、异常或系统告警。</span>
            </div>
            <div class="priority-row">
              <el-tag size="small" type="warning">中优先级</el-tag>
              <span>常规业务提醒，建议当天内处理完成。</span>
            </div>
            <div class="priority-row">
              <el-tag size="small" type="info">低优先级</el-tag>
              <span>信息播报或状态同步，可按批次集中查看。</span>
            </div>
          </div>
        </user-hub-panel>

        <user-hub-panel title="处理建议" description="帮助你在当前视图下更快完成消息清理。">
          <div class="priority-guide">
            <div class="priority-row">
              <span>当前视图：{{ currentFilterLabel }}</span>
            </div>
            <div class="priority-row">
              <span v-if="unreadCount > 0">建议先处理未读和高优先级消息，再统一清理普通通知。</span>
              <span v-else>当前未读消息已清空，可切换到全部视图做归档检查。</span>
            </div>
          </div>
        </user-hub-panel>
      </template>
    </user-hub-shell>

    <el-dialog
      v-model="detailVisible"
      :title="currentNotice.title || '通知详情'"
      width="640px"
      class="notification-dialog"
    >
      <div class="notice-detail">
        <div class="detail-meta">
          <el-tag size="small" :type="currentNotice.read ? 'info' : 'danger'">
            {{ currentNotice.read ? '已读' : '未读' }}
          </el-tag>
          <el-tag size="small" :type="priorityTag(currentNotice.priority)">
            {{ priorityText(currentNotice.priority) }}
          </el-tag>
          <span class="detail-time">{{ currentNotice.createTime || '-' }}</span>
        </div>
        <div class="detail-content">{{ currentNotice.content }}</div>
      </div>
      <template #footer>
        <el-button v-if="!currentNotice.read" type="primary" @click="markOneRead(currentNotice.id)">
          标记已读
        </el-button>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Bell, Clock, MessageBox } from '@element-plus/icons-vue'
import { getNotifications, markAllAsRead, markAsRead } from '@/api/common'
import UserHubPanel from '@/components/user-hub/UserHubPanel.vue'
import UserHubShell from '@/components/user-hub/UserHubShell.vue'

const loading = ref(false)
const detailVisible = ref(false)
const notifications = ref([])
const currentNotice = ref({})
const readFilter = ref('all')

const unreadCount = computed(() => notifications.value.filter((item) => !item.read).length)
const readCount = computed(() => notifications.value.filter((item) => item.read).length)
const highPriorityCount = computed(() => notifications.value.filter((item) => item.priority === 'HIGH').length)
const currentFilterLabel = computed(() => {
  const labelMap = {
    all: '全部',
    unread: '未读',
    read: '已读'
  }
  return labelMap[readFilter.value] || '全部'
})

const notificationMetaItems = computed(() => {
  const items = [`筛选：${currentFilterLabel.value}`]
  items.push(`消息总数 ${notifications.value.length}`)
  items.push(unreadCount.value > 0 ? `待处理 ${unreadCount.value} 条` : '当前没有未读消息')
  return items
})

const notificationStats = computed(() => [
  {
    label: '消息总数',
    value: String(notifications.value.length),
    helper: '当前筛选范围内'
  },
  {
    label: '未读消息',
    value: String(unreadCount.value),
    helper: unreadCount.value > 0 ? '建议尽快处理' : '收件箱已清空',
    tone: unreadCount.value > 0 ? 'danger' : 'success'
  },
  {
    label: '高优先级',
    value: String(highPriorityCount.value),
    helper: highPriorityCount.value > 0 ? '建议优先安排' : '暂无高优消息',
    tone: highPriorityCount.value > 0 ? 'warning' : 'info'
  },
  {
    label: '当前视图',
    value: currentFilterLabel.value,
    helper: '可切换全部 / 未读 / 已读',
    tone: 'info'
  }
])

function priorityText(priority) {
  const map = {
    HIGH: '高优先级',
    MEDIUM: '中优先级',
    LOW: '低优先级'
  }
  return map[priority] || priority || '普通'
}

function priorityTag(priority) {
  const map = { HIGH: 'danger', MEDIUM: 'warning', LOW: 'info' }
  return map[priority] || 'info'
}

function getExcerpt(content) {
  if (!content) return '暂无内容摘要'
  return content.length > 140 ? `${content.slice(0, 140)}...` : content
}

async function fetchNotifications() {
  loading.value = true
  try {
    const params = {}
    if (readFilter.value === 'read') params.read = true
    if (readFilter.value === 'unread') params.read = false
    const res = await getNotifications(params)
    notifications.value = res.code === 200 ? normalizeNotifications(res.data || []) : []
  } catch (error) {
    console.error('获取通知失败:', error)
    notifications.value = []
    ElMessage.error('获取通知失败')
  } finally {
    loading.value = false
  }
}

async function openDetail(item) {
  currentNotice.value = { ...item }
  detailVisible.value = true
  if (!item.read) {
    await markOneRead(item.id, { silent: true, keepDialogOpen: true })
  }
}

async function markOneRead(notificationId, options = {}) {
  const { silent = false, keepDialogOpen = false } = options
  try {
    const res = await markAsRead(notificationId)
    if (res.code === 200) {
      const item = notifications.value.find((notice) => String(notice.id) === String(notificationId))
      if (item) {
        item.read = true
      }
      currentNotice.value.read = true
      if (readFilter.value === 'unread') {
        notifications.value = notifications.value.filter((notice) => String(notice.id) !== String(notificationId))
      }
      if (!silent) {
        ElMessage.success('已标记为已读')
      }
      if (!keepDialogOpen) {
        detailVisible.value = false
      }
      if (!silent) {
        await fetchNotifications()
      }
      notifyUnreadCountChanged()
    } else {
      if (!silent) {
        ElMessage.error(res.msg || '标记失败')
      }
    }
  } catch (error) {
    if (!silent) {
      ElMessage.error(error.message || '标记失败')
    } else {
      console.error('标记通知已读失败:', error)
    }
  }
}

function notifyUnreadCountChanged() {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('rk-notifications-read-change'))
  }
}

function normalizeNotifications(data) {
  const records = Array.isArray(data) ? data : (data?.records || data?.list || [])
  return (Array.isArray(records) ? records : []).map((item) => ({
    ...item,
    read: Boolean(item.read ?? item.isRead ?? item.readStatus)
  }))
}

async function markAllRead() {
  try {
    const res = await markAllAsRead()
    if (res.code === 200) {
      ElMessage.success('全部通知已标记为已读')
      await fetchNotifications()
      notifyUnreadCountChanged()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

onMounted(() => {
  fetchNotifications()
})
</script>

<style scoped>
.notifications-page {
  padding: 28px 24px 40px;
}

.notification-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.toolbar-copy {
  display: grid;
  gap: 6px;
}

.toolbar-copy__label {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #24548b;
}

.toolbar-copy strong {
  color: #102a43;
  font-size: 16px;
}

.toolbar-controls,
.toolbar-status {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.notice-list {
  display: grid;
  gap: 14px;
  min-height: 220px;
}

.notice-card {
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr);
  gap: 16px;
  padding: 18px 20px;
  border: 1px solid #e4ebf2;
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #f9fbfd 100%);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.notice-card:hover {
  transform: translateY(-1px);
  border-color: rgba(37, 99, 235, 0.24);
  box-shadow: 0 16px 34px rgba(15, 53, 87, 0.08);
}

.notice-card.unread {
  border-color: rgba(220, 38, 38, 0.2);
  box-shadow: inset 3px 0 0 #dc2626;
}

.notice-indicator {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: #eff6ff;
  color: #2563eb;
  font-size: 20px;
}

.notice-main {
  min-width: 0;
}

.notice-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.notice-title {
  font-size: 16px;
  font-weight: 600;
  color: #102a43;
  line-height: 1.4;
}

.meta-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.notice-content {
  margin-top: 10px;
  color: #52667a;
  line-height: 1.7;
}

.notice-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 14px;
  color: #7d8d9d;
  font-size: 12px;
}

.notice-footer span,
.detail-meta {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.aside-metrics {
  display: grid;
  gap: 12px;
}

.aside-metric {
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid #e6edf5;
  background: #f8fbfe;
}

.aside-metric__label {
  display: block;
  font-size: 12px;
  color: #6b7d8f;
}

.aside-metric__value {
  display: block;
  margin-top: 6px;
  font-size: 26px;
  color: #102a43;
  line-height: 1;
}

.aside-metric__value.danger {
  color: #c0392b;
}

.aside-metric__value.warning {
  color: #b9770e;
}

.priority-guide {
  display: grid;
  gap: 14px;
}

.priority-row {
  display: grid;
  gap: 8px;
  color: #52667a;
  line-height: 1.6;
}

.notification-dialog :deep(.el-dialog) {
  border-radius: 22px;
}

.notice-detail {
  display: grid;
  gap: 18px;
}

.detail-meta {
  flex-wrap: wrap;
  color: #6b7d8f;
  font-size: 13px;
}

.detail-content {
  white-space: pre-wrap;
  line-height: 1.8;
  color: #102a43;
}

@media (max-width: 768px) {
  .notifications-page {
    padding: 20px 16px 32px;
  }

  .notification-toolbar {
    align-items: flex-start;
    gap: 14px;
  }

  .toolbar-controls {
    width: 100%;
    justify-content: space-between;
  }

  .notice-card {
    grid-template-columns: minmax(0, 1fr);
    padding: 16px 16px;
  }

  .notice-indicator {
    width: 40px;
    height: 40px;
    font-size: 18px;
  }

  .aside-metric__value {
    font-size: 24px;
  }
}

@media (max-width: 640px) {
  .notifications-page {
    padding: 16px 12px 28px;
  }

  .toolbar-copy {
    gap: 4px;
  }

  .toolbar-copy strong {
    font-size: 15px;
    line-height: 1.5;
  }

  .toolbar-controls,
  .toolbar-status {
    gap: 8px;
  }

  .notice-list {
    gap: 12px;
  }

  .notice-card {
    gap: 12px;
    padding: 14px;
    border-radius: 16px;
  }

  .notice-header {
    flex-direction: column;
    gap: 10px;
  }

  .meta-tags {
    justify-content: flex-start;
  }

  .notice-title {
    font-size: 15px;
  }

  .notice-content {
    margin-top: 8px;
    font-size: 13px;
    line-height: 1.65;
  }

  .notice-footer {
    margin-top: 12px;
    gap: 8px;
    font-size: 11px;
  }

  .aside-metric {
    padding: 12px 14px;
  }

  .aside-metric__value {
    font-size: 22px;
  }

  .detail-meta,
  .detail-content {
    font-size: 14px;
  }
}
</style>
