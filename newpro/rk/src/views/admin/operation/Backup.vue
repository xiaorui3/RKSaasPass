<template>
  <div class="admin-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <div>
            <h1>数据备份</h1>
            <p>真实读取当前备份记录，并可触发远程 MySQL 导出</p>
          </div>
          <div class="actions">
            <el-button @click="fetchOverview">刷新</el-button>
            <el-button type="primary" @click="openManualBackupDialog">立即备份</el-button>
            <el-button type="success" plain @click="showSchedule = true">定时策略</el-button>
          </div>
        </div>
      </template>

      <div class="stats-grid">
        <div class="stat-card">
          <span class="label">备份文件数</span>
          <span class="value">{{ stats.totalFiles }}</span>
        </div>
        <div class="stat-card">
          <span class="label">总占用空间</span>
          <span class="value">{{ stats.totalSize }}</span>
        </div>
        <div class="stat-card">
          <span class="label">最近备份</span>
          <span class="value small">{{ stats.lastBackup }}</span>
        </div>
        <div class="stat-card">
          <span class="label">自动策略</span>
          <span class="value" :class="stats.autoBackupEnabled ? 'success' : 'warning'">
            {{ stats.autoBackupEnabled ? '已启用' : '未启用' }}
          </span>
        </div>
      </div>

      <el-alert
        type="warning"
        :closable="false"
        show-icon
        class="restore-alert"
        title="当前页面提供真实备份创建、下载、删除与策略配置；浏览器内直接恢复仍保持关闭，避免误覆盖共享环境。"
      />

      <el-alert
        v-if="backupProgress.visible"
        type="info"
        :closable="false"
        show-icon
        class="restore-alert"
      >
        <template #title>
          <div class="backup-progress-title">
            <span>{{ backupProgress.message }}</span>
            <span>{{ backupProgress.percentage }}%</span>
          </div>
          <el-progress :percentage="backupProgress.percentage" :indeterminate="backupProgress.percentage < 95" />
          <div class="backup-range">
            <strong>当前备份范围：</strong>{{ currentBackupDatabases.join('、') || '未选择数据库' }}
          </div>
          <div class="backup-range">
            <strong>本次备份表：</strong>{{ formatTableSummary(currentBackupTables) }}
          </div>
        </template>
      </el-alert>

      <div class="batch-delete-toolbar">
        <el-button type="danger" plain :disabled="!selectedRows.length" @click="handleBatchDelete">批量删除</el-button>
      </div>

      <el-table :data="backups" v-loading="loading" stripe empty-text="暂无备份记录" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="backup-detail">
              <div><strong>当前备份范围：</strong>{{ formatDatabaseList(row.databaseList, row.database) }}</div>
              <div><strong>本次备份表：</strong>{{ formatTableSummary(row.tableSummary) }}</div>
              <div><strong>备份文件：</strong>{{ row.fileName || row.name || '-' }}</div>
              <div><strong>备注：</strong>{{ row.note || '-' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="备份名称" min-width="220" />
        <el-table-column prop="database" label="备份范围" width="120" />
        <el-table-column prop="tableCount" label="表数量" width="90">
          <template #default="{ row }">
            {{ row.tableCount ?? countSelectedTables(row.tableSummary) }}
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="size" label="文件大小" width="120" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'warning'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-space>
              <el-button
                size="small"
                type="primary"
                plain
                :disabled="!row.downloadable"
                @click="handleDownload(row)"
              >
                下载
              </el-button>
              <el-button size="small" type="danger" plain @click="handleDelete(row)">
                删除
              </el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="showManualBackup" title="选择手动备份范围" width="720px">
      <el-form label-width="110px">
        <el-form-item label="备份库">
          <el-checkbox-group v-model="manualBackup.databases" @change="fetchManualBackupTables">
            <el-checkbox
              v-for="database in backupDatabaseOptions"
              :key="database"
              :label="database"
            />
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="表级选择">
          <div class="table-selector" v-loading="loadingManualTables">
            <div class="table-selector-hint">默认备份选中数据库的全部表；勾选表后只备份选中的表。</div>
            <el-empty
              v-if="!manualBackup.databases.length"
              description="请至少选择一个数据库"
              :image-size="80"
            />
            <div v-for="database in manualBackup.databases" :key="database" class="table-group">
              <div class="table-group-header">
                <strong>{{ database }}</strong>
                <el-space>
                  <el-button link size="small" @click="selectAllManualTables(database)">全选表</el-button>
                  <el-button link size="small" @click="clearManualTableSelection(database)">整库备份</el-button>
                </el-space>
              </div>
              <el-checkbox-group v-model="manualSelectedTables[database]" class="table-checkboxes">
                <el-checkbox
                  v-for="table in manualAvailableTables[database] || []"
                  :key="table"
                  :label="table"
                >
                  {{ table }}
                </el-checkbox>
              </el-checkbox-group>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="备份备注">
          <el-input
            v-model="backupNote"
            type="textarea"
            :rows="2"
            placeholder="可选，记录本次备份目的"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showManualBackup = false">取消</el-button>
        <el-button
          type="primary"
          :loading="loading"
          :disabled="!manualBackup.databases.length || loadingManualTables"
          @click="handleCreateBackup"
        >
          开始备份
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showSchedule" title="定时备份策略" width="520px">
      <el-form label-width="110px">
        <el-form-item label="启用自动策略">
          <el-switch v-model="schedule.enabled" />
        </el-form-item>
        <el-form-item label="备份频率">
          <el-select v-model="schedule.frequency" style="width: 100%">
            <el-option label="每天" value="daily" />
            <el-option label="每周" value="weekly" />
            <el-option label="每月" value="monthly" />
          </el-select>
        </el-form-item>
        <el-form-item label="备份时间">
          <el-time-picker v-model="scheduleTime" format="HH:mm" value-format="HH:mm" style="width: 100%" />
        </el-form-item>
        <el-form-item label="保留天数">
          <el-input-number v-model="schedule.retentionDays" :min="1" :max="365" />
        </el-form-item>
        <el-form-item label="备份库">
          <el-checkbox-group v-model="schedule.databases">
            <el-checkbox
              v-for="database in backupDatabaseOptions"
              :key="database"
              :label="database"
            />
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="表级选择">
          <div class="table-selector" v-loading="loadingTables">
            <div class="table-selector-hint">不勾选表时默认备份整个库；勾选后只备份选中的表。</div>
            <div v-for="database in schedule.databases" :key="database" class="table-group">
              <div class="table-group-header">
                <strong>{{ database }}</strong>
                <el-space>
                  <el-button link size="small" @click="selectAllTables(database)">全选表</el-button>
                  <el-button link size="small" @click="clearTableSelection(database)">整库备份</el-button>
                </el-space>
              </div>
              <el-checkbox-group v-model="selectedTables[database]" class="table-checkboxes">
                <el-checkbox
                  v-for="table in availableTables[database] || []"
                  :key="table"
                  :label="table"
                >
                  {{ table }}
                </el-checkbox>
              </el-checkbox-group>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSchedule = false">取消</el-button>
        <el-button type="primary" @click="handleSaveSchedule">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useBatchDelete } from '@/utils/batchDelete'
import { createBackup, deleteBackup, downloadBackupFile, getBackupOverview, getBackupTables, saveBackupSchedule } from '@/api/admin-ops'

const loading = ref(false)
const loadingTables = ref(false)
const loadingManualTables = ref(false)
const showManualBackup = ref(false)
const showSchedule = ref(false)
const backups = ref([])
const availableTables = ref({})
const selectedTables = ref({})
const manualAvailableTables = ref({})
const manualSelectedTables = ref({})
const backupNote = ref('')
const currentBackupTables = ref({})
const currentBackupDatabases = ref([])
const backupProgress = ref({
  visible: false,
  percentage: 0,
  message: '等待创建备份'
})
const stats = ref({
  totalFiles: 0,
  totalSize: '0 B',
  lastBackup: '-',
  autoBackupEnabled: false
})
const backupDatabaseOptions = ['rk_auth', 'rk_user', 'rk_activity', 'rk_content', 'rk_message']
const manualBackup = ref({
  databases: [...backupDatabaseOptions]
})
const schedule = ref({
  enabled: true,
  frequency: 'daily',
  time: '02:00',
  retentionDays: 30,
  databases: [...backupDatabaseOptions]
})

const scheduleTime = computed({
  get: () => schedule.value.time || '02:00',
  set: (value) => {
    schedule.value.time = value
  }
})

async function fetchOverview() {
  loading.value = true
  try {
    const res = await getBackupOverview()
    if (res.code !== 200 || !res.data) {
      throw new Error(res.msg || '获取备份信息失败')
    }
    stats.value = res.data.storageStats || stats.value
    backups.value = res.data.backups || []
    schedule.value = res.data.schedule || schedule.value
  } catch (error) {
    ElMessage.error(error.message || '获取备份信息失败')
  } finally {
    loading.value = false
  }
}

function buildBackupTablesPayload(databases, tableSelections) {
  return (databases || []).reduce((result, database) => {
    result[database] = Array.isArray(tableSelections[database]) ? tableSelections[database] : []
    return result
  }, {})
}

function countSelectedTables(tableSummary = {}) {
  return Object.values(tableSummary || {}).reduce((count, tables) => count + (Array.isArray(tables) ? tables.length : 0), 0)
}

function formatDatabaseList(databaseList = [], fallback = '') {
  const values = Array.isArray(databaseList) ? databaseList.filter(Boolean) : []
  return values.length ? values.join('、') : fallback || '-'
}

function formatTableSummary(tableSummary = {}) {
  const entries = Object.entries(tableSummary || {})
  if (!entries.length) {
    return '整库备份'
  }
  return entries
    .map(([database, tables]) => {
      const selected = Array.isArray(tables) && tables.length ? tables.join('、') : '整库'
      return `${database}: ${selected}`
    })
    .join('；')
}

function updateBackupProgress(percentage, message) {
  backupProgress.value = {
    visible: true,
    percentage,
    message
  }
}

function reconcileSelectedTables(tableMap) {
  const next = {}
  Object.entries(tableMap || {}).forEach(([database, tables]) => {
    const existing = Array.isArray(selectedTables.value[database]) ? selectedTables.value[database] : []
    next[database] = existing.filter((item) => (tables || []).includes(item))
  })
  selectedTables.value = next
}

function reconcileManualSelectedTables(tableMap) {
  const next = {}
  Object.entries(tableMap || {}).forEach(([database, tables]) => {
    const existing = Array.isArray(manualSelectedTables.value[database]) ? manualSelectedTables.value[database] : []
    next[database] = existing.filter((item) => (tables || []).includes(item))
  })
  manualSelectedTables.value = next
}

async function fetchBackupTables() {
  if (!schedule.value.databases?.length) {
    availableTables.value = {}
    selectedTables.value = {}
    return
  }
  loadingTables.value = true
  try {
    const res = await getBackupTables(schedule.value.databases)
    if (res.code !== 200 || !res.data) {
      throw new Error(res.msg || '获取备份表清单失败')
    }
    availableTables.value = res.data
    reconcileSelectedTables(res.data)
  } catch (error) {
    ElMessage.error(error.message || '获取备份表清单失败')
  } finally {
    loadingTables.value = false
  }
}

async function fetchManualBackupTables() {
  if (!manualBackup.value.databases?.length) {
    manualAvailableTables.value = {}
    manualSelectedTables.value = {}
    return
  }
  loadingManualTables.value = true
  try {
    const res = await getBackupTables(manualBackup.value.databases)
    if (res.code !== 200 || !res.data) {
      throw new Error(res.msg || '获取备份表清单失败')
    }
    manualAvailableTables.value = res.data
    reconcileManualSelectedTables(res.data)
  } catch (error) {
    ElMessage.error(error.message || '获取备份表清单失败')
  } finally {
    loadingManualTables.value = false
  }
}

function selectAllTables(database) {
  selectedTables.value = {
    ...selectedTables.value,
    [database]: [...(availableTables.value[database] || [])]
  }
}

function clearTableSelection(database) {
  selectedTables.value = {
    ...selectedTables.value,
    [database]: []
  }
}

function selectAllManualTables(database) {
  manualSelectedTables.value = {
    ...manualSelectedTables.value,
    [database]: [...(manualAvailableTables.value[database] || [])]
  }
}

function clearManualTableSelection(database) {
  manualSelectedTables.value = {
    ...manualSelectedTables.value,
    [database]: []
  }
}

async function openManualBackupDialog() {
  manualBackup.value = {
    databases: [...backupDatabaseOptions]
  }
  manualSelectedTables.value = {}
  manualAvailableTables.value = {}
  backupNote.value = ''
  showManualBackup.value = true
  await fetchManualBackupTables()
}

async function handleCreateBackup() {
  if (!manualBackup.value.databases.length) {
    ElMessage.warning('请至少选择一个数据库')
    return
  }
  loading.value = true
  currentBackupDatabases.value = [...manualBackup.value.databases]
  currentBackupTables.value = buildBackupTablesPayload(manualBackup.value.databases, manualSelectedTables.value)
  updateBackupProgress(15, '正在提交备份任务')
  try {
    updateBackupProgress(45, '正在导出数据库并上传到 MinIO')
    const res = await createBackup({
      databases: manualBackup.value.databases,
      tables: currentBackupTables.value,
      note: backupNote.value.trim() || undefined
    })
    if (res.code !== 200 || !res.data) {
      throw new Error(res.msg || '创建备份失败')
    }
    updateBackupProgress(100, '备份已完成，正在刷新记录')
    ElMessage.success('备份任务已完成')
    backupNote.value = ''
    showManualBackup.value = false
    stats.value = res.data.storageStats || stats.value
    backups.value = res.data.backups || []
  } catch (error) {
    ElMessage.error(error.message || '创建备份失败')
  } finally {
    loading.value = false
    window.setTimeout(() => {
      backupProgress.value.visible = false
    }, 1200)
  }
}

async function handleDownload(row) {
  try {
    const blob = await downloadBackupFile(row.id)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = row.name || `backup-${row.id}.tar.gz`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  } catch (error) {
    ElMessage.error(error.message || '下载备份失败')
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除备份 ${row.name} 吗？`, '删除备份', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  const res = await deleteBackup(row.id)
  if (res.code === 200 && res.data) {
    ElMessage.success('备份已删除')
    await fetchOverview()
    return
  }
  ElMessage.error(res.msg || '删除备份失败')
}

async function handleSaveSchedule() {
  const res = await saveBackupSchedule(schedule.value)
  if (res.code === 200 && res.data) {
    ElMessage.success('备份策略已保存')
    showSchedule.value = false
    stats.value = res.data.storageStats || stats.value
    schedule.value = res.data.schedule || schedule.value
    return
  }
  ElMessage.error(res.msg || '保存备份策略失败')
}


const { selectedRows, handleSelectionChange, handleBatchDelete } = useBatchDelete({
  entityName: '备份',
  deleteItem: (id) => deleteBackup(id),
  fetchList: fetchOverview
})

onMounted(() => {
  fetchOverview()
})

watch(
  () => [...schedule.value.databases],
  () => {
    fetchBackupTables()
  },
  { immediate: true }
)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.card-header h1 {
  margin: 0;
  font-size: 24px;
}

.card-header p {
  margin: 8px 0 0;
  color: #909399;
}

.actions {
  display: flex;
  gap: 12px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.label {
  color: #909399;
}

.value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.value.small {
  font-size: 16px;
}

.value.success {
  color: #67c23a;
}

.value.warning {
  color: #e6a23c;
}

.table-selector {
  width: 100%;
}

.table-selector-hint {
  margin-bottom: 12px;
  color: #909399;
  font-size: 13px;
}

.table-group {
  padding: 12px;
  margin-bottom: 12px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #fafafa;
}

.table-group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.table-checkboxes {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
}

.restore-alert {
  margin-bottom: 16px;
}

.backup-progress-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.backup-range {
  margin-top: 8px;
  line-height: 1.6;
}

.backup-detail {
  display: grid;
  gap: 8px;
  padding: 8px 16px;
  color: #606266;
}
</style>
