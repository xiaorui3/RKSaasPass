<template>
  <div class="admin-page notification-center-page">
    <section class="notification-header">
      <div>
        <h1>通知升级管理</h1>
        <p>统一维护站内通知、租户通知和 Android 升级通知。</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" :loading="loading" @click="fetchData">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate('system')">新建通知</el-button>
        <el-button type="warning" :icon="UploadFilled" @click="openCreate('upgrade')">升级通知</el-button>
      </div>
    </section>

    <section class="metric-row">
      <div v-for="item in metrics" :key="item.label" class="metric-item">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </section>

    <section class="delivery-panel">
      <div>
        <h2>消息通知控制台</h2>
        <p>关闭后，对应类型的新通知不会再写入站内消息，移动端也不会收到该类型推送。</p>
      </div>
      <div class="delivery-switches">
        <el-switch v-model="deliveryConfig.enabled" active-text="总开关" />
        <el-switch v-model="deliveryConfig.systemEnabled" active-text="系统" />
        <el-switch v-model="deliveryConfig.messageEnabled" active-text="业务" />
        <el-switch v-model="deliveryConfig.activityEnabled" active-text="活动" />
        <el-switch v-model="deliveryConfig.upgradeEnabled" active-text="升级" />
        <el-button type="primary" :loading="savingDeliveryConfig" @click="handleSaveDeliveryConfig">保存开关</el-button>
      </div>
    </section>

    <section class="mobile-release-panel">
      <div class="mobile-release-header">
        <div>
          <h2>移动端升级中心</h2>
          <p>上传 APK 到 MinIO，并控制所有移动端用户是否收到更新。</p>
        </div>
        <div class="header-actions">
          <el-switch
            v-model="mobileReleaseForm.enabled"
            active-text="启用"
            inactive-text="停用"
          />
          <el-button :loading="buildingMobileRelease" @click="handleAutoBuildMobileRelease">
            自动构建并上传
          </el-button>
          <el-button type="danger" plain :loading="withdrawingMobileRelease" @click="handleWithdrawMobileRelease">
            撤回更新
          </el-button>
          <el-button type="primary" :loading="savingMobileRelease" @click="handleSaveMobileRelease">
            保存配置
          </el-button>
        </div>
      </div>
      <div class="mobile-release-grid">
        <el-form-item label="版本名称">
          <el-input v-model="mobileReleaseForm.versionName" placeholder="1.0.1" />
        </el-form-item>
        <el-form-item label="版本码">
          <el-input-number v-model="mobileReleaseForm.versionCode" :min="0" :controls="false" />
        </el-form-item>
        <el-form-item label="最低可用版本码">
          <el-input-number v-model="mobileReleaseForm.minSupportedVersionCode" :min="0" :controls="false" />
        </el-form-item>
        <el-form-item label="强制升级">
          <el-switch v-model="mobileReleaseForm.forceUpgrade" />
        </el-form-item>
        <el-form-item label="投放范围">
          <el-select v-model="mobileReleaseForm.targetMode">
            <el-option label="全部用户" value="all" />
            <el-option label="指定租户" value="tenant" />
            <el-option label="指定角色" value="role" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="mobileReleaseForm.targetMode === 'tenant'" label="投放租户">
          <el-select
            v-model="mobileReleaseForm.tenantIds"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            placeholder="选择一个或多个租户"
          >
            <el-option
              v-for="tenant in tenantOptions"
              :key="tenant.id"
              :label="tenantLabel(tenant)"
              :value="Number(tenant.id)"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="mobileReleaseForm.targetMode === 'role'" label="角色 ID">
          <el-input v-model="mobileRoleIdsText" placeholder="1,2,3" />
        </el-form-item>
        <el-form-item label="APK 文件">
          <el-upload :show-file-list="false" accept=".apk" :http-request="handleMobileApkUpload">
            <el-button :icon="UploadFilled" :loading="uploadingMobileApk">上传 APK</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="发布来源">
          <el-input v-model="mobileReleaseForm.releaseSource" placeholder="manual-upload / auto-build / rollback" />
        </el-form-item>
        <el-form-item label="构建状态">
          <el-input v-model="mobileReleaseForm.buildStatus" placeholder="uploaded / queued / failed" />
        </el-form-item>
      </div>
      <el-form-item label="下载地址">
        <el-input v-model="mobileReleaseForm.downloadUrl" placeholder="上传 APK 后自动填充 MinIO 地址" />
      </el-form-item>
      <el-form-item label="发布说明">
        <el-input v-model="mobileReleaseForm.releaseNotes" type="textarea" :rows="3" />
      </el-form-item>
      <el-form-item label="构建记录">
        <el-input v-model="mobileReleaseForm.buildLog" type="textarea" :rows="2" readonly />
      </el-form-item>
      <div class="mobile-build-live">
        <div class="history-title">自动构建记录</div>
        <div class="build-live-meta">
          <el-tag :type="buildStatusTag(latestMobileReleaseBuild?.status)">
            {{ latestMobileReleaseBuild?.status || 'none' }}
          </el-tag>
          <span>Build #{{ latestMobileReleaseBuild?.id || '-' }}</span>
          <span>{{ latestMobileReleaseBuild?.gitCommit || '-' }}</span>
          <el-button size="small" :loading="buildHistoryLoading" @click="fetchMobileReleaseBuilds">刷新构建记录</el-button>
        </div>
        <pre class="mobile-build-log">{{ mobileReleaseBuildLogs || latestMobileReleaseBuild?.buildLog || '暂无构建日志' }}</pre>
        <el-collapse v-model="mobileBuildHistoryCollapse" class="release-history-collapse">
          <el-collapse-item title="查看自动构建记录" name="build-history">
            <el-table :data="pagedMobileReleaseBuildHistory" size="small" border empty-text="暂无自动构建历史">
              <el-table-column prop="id" label="Build" width="90" />
              <el-table-column prop="status" label="状态" width="110" />
              <el-table-column prop="versionName" label="版本" width="100" />
              <el-table-column prop="gitCommit" label="Commit" width="130" />
              <el-table-column prop="fileSize" label="大小" width="110" />
              <el-table-column prop="createdAt" label="时间" min-width="160" show-overflow-tooltip />
            </el-table>
            <el-pagination
              v-model:current-page="mobileBuildHistoryPage"
              class="history-pagination"
              layout="prev, pager, next"
              :page-size="historyPageSize"
              :total="mobileReleaseBuildHistory.length"
              small
            />
          </el-collapse-item>
        </el-collapse>
      </div>
      <div class="mobile-release-history">
        <div class="history-title">APK 发布记录</div>
        <el-collapse v-model="mobileReleaseHistoryCollapse" class="release-history-collapse">
          <el-collapse-item title="查看 APK 发布记录" name="apk-history">
            <el-table :data="pagedMobileReleaseHistory" size="small" border empty-text="暂无发布记录">
              <el-table-column prop="versionName" label="版本" width="100" />
              <el-table-column prop="versionCode" label="版本号" width="90" />
              <el-table-column prop="releaseSource" label="发布来源" width="130" />
              <el-table-column prop="status" label="状态" width="100" />
              <el-table-column label="下载地址" min-width="260">
                <template #default="{ row }">
                  <el-tooltip
                    :content="apkDownloadUrl(row)"
                    placement="top"
                    :disabled="!apkDownloadUrl(row)"
                  >
                    <span class="apk-url-text">{{ apkDownloadUrl(row) || '-' }}</span>
                  </el-tooltip>
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" label="时间" min-width="160" show-overflow-tooltip />
              <el-table-column label="操作" width="180" fixed="right">
                <template #default="{ row }">
                  <el-button text type="success" :disabled="!apkDownloadUrl(row)" @click="handleDownloadMobileRelease(row)">手动下载</el-button>
                  <el-button text type="primary" @click="handleRollbackMobileRelease(row)">回滚</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination
              v-model:current-page="mobileReleaseHistoryPage"
              class="history-pagination"
              layout="prev, pager, next"
              :page-size="historyPageSize"
              :total="mobileReleaseHistory.length"
              small
            />
          </el-collapse-item>
        </el-collapse>
      </div>
    </section>

    <section class="filter-bar">
      <el-input
        v-model="filters.keyword"
        clearable
        placeholder="搜索标题或内容"
        class="keyword-input"
        @keyup.enter="fetchData"
      />
      <el-select v-model="filters.type" clearable placeholder="类型" class="type-select" @change="fetchData">
        <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-input-number
        v-model="filters.tenantId"
        :min="1"
        :controls="false"
        placeholder="租户 ID"
        class="tenant-input"
        @change="fetchData"
      />
      <el-button type="primary" plain @click="fetchData">查询</el-button>
    </section>

    <section class="table-panel">
      <el-table v-loading="loading" :data="pagedNotifications" row-key="id" stripe empty-text="暂无通知">
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="tenantId" label="租户" width="90" />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="row.type === 'upgrade' ? 'warning' : 'info'" size="small">
              {{ typeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优先级" width="110">
          <template #default="{ row }">
            <el-tag :type="priorityTag(row.priority)" size="small">{{ priorityLabel(row.priority) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="目标" width="120">
          <template #default="{ row }">{{ targetLabel(row.targetType) }}</template>
        </el-table-column>
        <el-table-column prop="upgradeVersion" label="升级版本" width="120" />
        <el-table-column prop="createTime" label="发布时间" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" :icon="Edit" @click="openEdit(row)">替换</el-button>
            <el-button text type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="notificationPage"
        class="history-pagination"
        layout="prev, pager, next"
        :page-size="notificationPageSize"
        :total="notifications.length"
        small
      />
    </section>

    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="520px" class="notification-drawer">
      <el-form label-position="top" class="notification-form">
        <el-form-item label="类型">
          <el-radio-group v-model="notificationForm.type">
            <el-radio-button v-for="item in typeOptions" :key="item.value" :label="item.value">
              {{ item.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="投放租户">
            <el-select
              v-model="notificationTenantIds"
              multiple
              filterable
              collapse-tags
              collapse-tags-tooltip
              placeholder="选择一个或多个租户"
            >
              <el-option
                v-for="tenant in tenantOptions"
                :key="tenant.id"
                :label="tenantLabel(tenant)"
                :value="Number(tenant.id)"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="优先级">
            <el-select v-model="notificationForm.priority">
              <el-option label="普通" :value="0" />
              <el-option label="重要" :value="1" />
              <el-option label="紧急" :value="2" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="标题">
          <el-input v-model="notificationForm.title" maxlength="120" show-word-limit />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="notificationForm.content" type="textarea" :rows="6" maxlength="2000" show-word-limit />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="目标范围">
            <el-select v-model="notificationForm.targetType">
              <el-option label="全部用户" :value="0" />
              <el-option label="指定用户 ID" :value="1" />
              <el-option label="指定角色 ID" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="notificationForm.targetType !== 0" label="目标 ID">
            <el-input v-model="targetIdsText" placeholder="逗号分隔，如 1,2,3" />
          </el-form-item>
        </div>
        <template v-if="notificationForm.type === 'upgrade'">
          <div class="upgrade-fields">
            <el-form-item label="升级版本">
              <el-input v-model="notificationForm.upgradeVersion" placeholder="1.0.1" />
            </el-form-item>
            <el-form-item label="下载地址">
              <el-input v-model="notificationForm.downloadUrl" placeholder="https://..." />
            </el-form-item>
            <el-form-item label="强制升级">
              <el-switch v-model="notificationForm.forceUpgrade" />
            </el-form-item>
            <el-form-item label="发布说明">
              <el-input v-model="notificationForm.releaseNotes" type="textarea" :rows="4" />
            </el-form-item>
          </div>
        </template>
      </el-form>
      <template #footer>
        <div class="drawer-actions">
          <el-button @click="drawerVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="handleSave">发送 / 替换</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Refresh, UploadFilled } from '@element-plus/icons-vue'
import {
  deleteAdminNotification,
  getAdminNotificationOverview,
  getNotificationDeliveryConfig,
  listAdminNotifications,
  saveAdminNotification,
  saveNotificationDeliveryConfig
} from '@/api/admin-notifications'
import {
  autoBuildMobileRelease,
  getLatestMobileReleaseBuild,
  getMobileReleaseBuildHistory,
  getMobileReleaseBuildLogs,
  getMobileReleaseConfig,
  getMobileReleaseHistory,
  rollbackMobileRelease,
  saveMobileReleaseConfig,
  withdrawMobileRelease
} from '@/api/mobile-release'
import { getPublicTenantList } from '@/api/tenant'
import { normalizeTenantDirectory } from '@/utils/tenantBranding'
import { uploadManagedFile } from '@/utils/fileUpload'

const loading = ref(false)
const saving = ref(false)
const savingDeliveryConfig = ref(false)
const savingMobileRelease = ref(false)
const uploadingMobileApk = ref(false)
const buildingMobileRelease = ref(false)
const withdrawingMobileRelease = ref(false)
const buildHistoryLoading = ref(false)
const drawerVisible = ref(false)
const notifications = ref([])
const mobileReleaseHistory = ref([])
const mobileReleaseBuildHistory = ref([])
const latestMobileReleaseBuild = ref(null)
const mobileReleaseBuildLogs = ref('')
const overview = ref({ totalCount: 0, sentCount: 0, upgradeCount: 0, highPriorityCount: 0 })
const targetIdsText = ref('')
const mobileRoleIdsText = ref('')
const tenantOptions = ref([])
const notificationTenantIds = ref([])
const mobileBuildHistoryCollapse = ref([])
const mobileReleaseHistoryCollapse = ref([])
const notificationPage = ref(1)
const notificationPageSize = 5
const mobileBuildHistoryPage = ref(1)
const mobileReleaseHistoryPage = ref(1)
const historyPageSize = 5
const deliveryConfig = reactive({
  tenantId: 1,
  enabled: true,
  systemEnabled: true,
  messageEnabled: true,
  activityEnabled: true,
  upgradeEnabled: true
})

const filters = reactive({
  keyword: '',
  type: '',
  tenantId: 1
})

const notificationForm = reactive(defaultNotificationForm())
const mobileReleaseForm = reactive(defaultMobileReleaseForm())
let buildPollTimer = null
let lastBuildLogLine = 0

const typeOptions = [
  { label: '系统通知', value: 'system' },
  { label: '业务通知', value: 'message' },
  { label: '活动通知', value: 'activity' },
  { label: '升级通知', value: 'upgrade' }
]

const metrics = computed(() => [
  { label: '通知总数', value: overview.value.totalCount || 0 },
  { label: '已发送', value: overview.value.sentCount || 0 },
  { label: '升级通知', value: overview.value.upgradeCount || 0 },
  { label: '高优先级', value: overview.value.highPriorityCount || 0 }
])

const drawerTitle = computed(() => (notificationForm.id ? '替换通知' : '发送通知'))
const pagedNotifications = computed(() => paginate(notifications.value, notificationPage.value, notificationPageSize))
const pagedMobileReleaseBuildHistory = computed(() => paginate(mobileReleaseBuildHistory.value, mobileBuildHistoryPage.value, historyPageSize))
const pagedMobileReleaseHistory = computed(() => paginate(mobileReleaseHistory.value, mobileReleaseHistoryPage.value, historyPageSize))

function defaultNotificationForm(type = 'system') {
  return {
    id: null,
    tenantId: 1,
    title: '',
    content: '',
    type,
    priority: type === 'upgrade' ? 2 : 0,
    targetType: 0,
    targetIds: [],
    upgradeVersion: '',
    forceUpgrade: false,
    downloadUrl: '',
    releaseNotes: ''
  }
}

function defaultMobileReleaseForm() {
  return {
    enabled: false,
    versionName: '1.0.0',
    versionCode: 1,
    minSupportedVersionCode: 0,
    forceUpgrade: false,
    downloadUrl: '',
    apkPath: '',
    fileSize: 0,
    releaseNotes: '',
    releaseSource: 'manual-upload',
    buildStatus: '',
    buildLog: '',
    buildId: null,
    gitCommit: '',
    gitRange: '',
    createdAt: '',
    targetMode: 'all',
    tenantIds: [],
    roleIds: []
  }
}

function assignForm(data) {
  Object.assign(notificationForm, defaultNotificationForm(data.type || 'system'), data)
  targetIdsText.value = Array.isArray(data.targetIds) ? data.targetIds.join(',') : ''
  const tenantId = Number(notificationForm.tenantId || filters.tenantId || 1)
  notificationTenantIds.value = Number.isFinite(tenantId) && tenantId > 0 ? [tenantId] : []
}

function openCreate(type = 'system') {
  assignForm(defaultNotificationForm(type))
  const tenantId = Number(filters.tenantId || 1)
  notificationTenantIds.value = Number.isFinite(tenantId) && tenantId > 0 ? [tenantId] : []
  drawerVisible.value = true
}

function openEdit(row) {
  assignForm({ ...row })
  drawerVisible.value = true
}

function parseTargetIds() {
  return targetIdsText.value
    .split(/[,\s]+/)
    .map((item) => Number(item.trim()))
    .filter((item) => Number.isFinite(item) && item > 0)
}

function parseIdList(value) {
  return String(value || '')
    .split(/[,\s]+/)
    .map((item) => Number(item.trim()))
    .filter((item) => Number.isFinite(item) && item > 0)
}

function paginate(rows, page, size) {
  const list = Array.isArray(rows) ? rows : []
  const currentPage = Math.max(1, Number(page) || 1)
  const pageSize = Math.max(1, Number(size) || 5)
  const start = (currentPage - 1) * pageSize
  return list.slice(start, start + pageSize)
}

function selectedNotificationTenantIds() {
  return notificationTenantIds.value
    .map((item) => Number(item))
    .filter((item) => Number.isFinite(item) && item > 0)
}

function buildNotificationBasePayload() {
  return {
    ...notificationForm,
    targetIds: notificationForm.targetType === 0 ? [] : parseTargetIds()
  }
}

function buildNotificationPayloads() {
  const base = buildNotificationBasePayload()
  if (base.id) {
    const tenantId = Number(notificationTenantIds.value[0] || base.tenantId || filters.tenantId || 1)
    return [{ ...base, tenantId }]
  }
  const tenantIds = selectedNotificationTenantIds()
  if (!tenantIds.length) {
    throw new Error('请选择至少一个投放租户')
  }
  return tenantIds.map((tenantId) => ({ ...base, tenantId }))
}

async function handleSave() {
  if (!notificationForm.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  if (!notificationForm.content.trim()) {
    ElMessage.warning('请输入内容')
    return
  }
  saving.value = true
  try {
    const payloads = buildNotificationPayloads()
    const results = await Promise.all(payloads.map((payload) => saveAdminNotification(payload)))
    const failed = results.find((res) => res.code !== 200)
    if (failed) {
      ElMessage.error(failed.msg || '发送失败')
      return
    }
    if (results.length) {
      ElMessage.success(notificationForm.id ? '已替换' : `已发送到 ${payloads.length} 个租户`)
      drawerVisible.value = false
      await fetchData()
    }
  } catch (error) {
    ElMessage.error(error.message || '发送失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除“${row.title}”？`, '删除通知', { type: 'warning' })
  } catch {
    return
  }
  const res = await deleteAdminNotification(row.id)
  if (res.code === 200 && res.data === true) {
    notifications.value = notifications.value.filter((item) => item.id !== row.id)
    ElMessage.success('已删除')
    await fetchData()
  } else {
    ElMessage.error(res.msg || '删除失败，通知可能已不存在或未更新')
  }
}

function assignMobileReleaseForm(data = {}) {
  Object.assign(mobileReleaseForm, defaultMobileReleaseForm(), data)
  mobileReleaseForm.tenantIds = Array.isArray(mobileReleaseForm.tenantIds)
    ? mobileReleaseForm.tenantIds.map((item) => Number(item)).filter((item) => Number.isFinite(item) && item > 0)
    : []
  mobileRoleIdsText.value = Array.isArray(mobileReleaseForm.roleIds) ? mobileReleaseForm.roleIds.join(',') : ''
  mobileReleaseHistory.value = Array.isArray(data.history) ? data.history : mobileReleaseHistory.value
}

function buildMobileReleasePayload() {
  return {
    ...mobileReleaseForm,
    tenantIds: mobileReleaseForm.targetMode === 'tenant' ? mobileReleaseForm.tenantIds : [],
    roleIds: mobileReleaseForm.targetMode === 'role' ? parseIdList(mobileRoleIdsText.value) : []
  }
}

function tenantLabel(tenant) {
  const id = Number(tenant?.id || 0)
  const name = tenant?.tenantName || tenant?.name || tenant?.organizationName || `租户${id || ''}`
  return id ? `${name}（${id}）` : name
}

async function fetchTenantOptions() {
  const res = await getPublicTenantList()
  tenantOptions.value = normalizeTenantDirectory(res.data || [])
  if (!notificationTenantIds.value.length) {
    const tenantId = Number(filters.tenantId || 1)
    notificationTenantIds.value = Number.isFinite(tenantId) && tenantId > 0 ? [tenantId] : []
  }
}

async function fetchMobileReleaseConfig() {
  const res = await getMobileReleaseConfig()
  assignMobileReleaseForm(res.data || {})
}

async function fetchMobileReleaseHistory() {
  const res = await getMobileReleaseHistory()
  mobileReleaseHistory.value = Array.isArray(res.data) ? res.data : []
  mobileReleaseHistoryPage.value = 1
}

async function fetchMobileReleaseBuilds() {
  buildHistoryLoading.value = true
  try {
    const [latestRes, historyRes] = await Promise.all([
      getLatestMobileReleaseBuild(),
      getMobileReleaseBuildHistory()
    ])
    latestMobileReleaseBuild.value = latestRes.data || null
    mobileReleaseBuildHistory.value = Array.isArray(historyRes.data) ? historyRes.data : []
    mobileBuildHistoryPage.value = 1
    if (latestMobileReleaseBuild.value?.id) {
      await fetchMobileReleaseBuildLogs(latestMobileReleaseBuild.value.id, true)
      syncBuildPolling()
    }
  } finally {
    buildHistoryLoading.value = false
  }
}

async function fetchMobileReleaseBuildLogs(buildId, reset = false) {
  if (!buildId) return
  if (reset) {
    lastBuildLogLine = 0
    mobileReleaseBuildLogs.value = ''
  }
  const res = await getMobileReleaseBuildLogs(buildId, { afterLineNo: lastBuildLogLine })
  const rows = Array.isArray(res.data) ? res.data : []
  if (!rows.length) return
  lastBuildLogLine = Math.max(...rows.map((item) => Number(item.lineNo || 0)))
  const text = rows.map((item) => item.content || '').join('\n')
  mobileReleaseBuildLogs.value = mobileReleaseBuildLogs.value
    ? `${mobileReleaseBuildLogs.value}\n${text}`
    : text
}

function syncBuildPolling() {
  stopBuildPolling()
  if (!['queued', 'running'].includes(latestMobileReleaseBuild.value?.status)) {
    return
  }
  buildPollTimer = window.setInterval(async () => {
    const latestRes = await getLatestMobileReleaseBuild()
    latestMobileReleaseBuild.value = latestRes.data || latestMobileReleaseBuild.value
    if (latestMobileReleaseBuild.value?.id) {
      await fetchMobileReleaseBuildLogs(latestMobileReleaseBuild.value.id)
    }
    if (!['queued', 'running'].includes(latestMobileReleaseBuild.value?.status)) {
      stopBuildPolling()
      await fetchMobileReleaseConfig()
      await fetchMobileReleaseHistory()
      await fetchMobileReleaseBuilds()
    }
  }, 3000)
}

function stopBuildPolling() {
  if (buildPollTimer) {
    window.clearInterval(buildPollTimer)
    buildPollTimer = null
  }
}

async function handleMobileApkUpload(options) {
  uploadingMobileApk.value = true
  try {
    const { storedValue, url } = await uploadManagedFile(options, 'mobile-apk')
    mobileReleaseForm.apkPath = storedValue
    mobileReleaseForm.downloadUrl = url || storedValue
    mobileReleaseForm.fileSize = options.file?.size || 0
    mobileReleaseForm.releaseSource = 'manual-upload'
    mobileReleaseForm.buildStatus = 'uploaded'
    mobileReleaseForm.buildLog = `manual upload: ${options.file?.name || 'apk'}`
    ElMessage.success('APK 已上传到 MinIO')
  } finally {
    uploadingMobileApk.value = false
  }
}

async function handleSaveMobileRelease() {
  if (!mobileReleaseForm.versionName.trim()) {
    ElMessage.warning('请输入移动端版本名称')
    return
  }
  if (mobileReleaseForm.enabled && !mobileReleaseForm.downloadUrl.trim()) {
    ElMessage.warning('启用升级前请先上传 APK 或填写下载地址')
    return
  }
  if (mobileReleaseForm.targetMode === 'tenant' && !mobileReleaseForm.tenantIds.length) {
    ElMessage.warning('请选择至少一个投放租户')
    return
  }
  savingMobileRelease.value = true
  try {
    const res = await saveMobileReleaseConfig(buildMobileReleasePayload())
    assignMobileReleaseForm(res.data || {})
    await fetchMobileReleaseHistory()
    await fetchMobileReleaseBuilds()
    syncBuildPolling()
    ElMessage.success('移动端升级配置已保存')
  } finally {
    savingMobileRelease.value = false
  }
}

async function handleAutoBuildMobileRelease() {
  buildingMobileRelease.value = true
  try {
    const res = await autoBuildMobileRelease()
    assignMobileReleaseForm(res.data || {})
    await fetchMobileReleaseHistory()
    await fetchMobileReleaseBuilds()
    syncBuildPolling()
    ElMessage.success('自动构建任务已记录')
  } finally {
    buildingMobileRelease.value = false
  }
}

async function handleWithdrawMobileRelease() {
  try {
    await ElMessageBox.confirm('确认撤回当前 APK 更新？旧客户端将不会再收到该版本推送。', '撤回更新', { type: 'warning' })
  } catch {
    return
  }
  withdrawingMobileRelease.value = true
  try {
    const res = await withdrawMobileRelease()
    assignMobileReleaseForm(res.data || {})
    await fetchMobileReleaseHistory()
    ElMessage.success('更新已撤回')
  } finally {
    withdrawingMobileRelease.value = false
  }
}

async function handleRollbackMobileRelease(row) {
  if (!row?.historyId) {
    ElMessage.warning('该记录缺少 historyId，不能回滚')
    return
  }
  try {
    await ElMessageBox.confirm(`确认回滚到 ${row.versionName}(${row.versionCode})？`, '回滚 APK 更新', { type: 'warning' })
  } catch {
    return
  }
  savingMobileRelease.value = true
  try {
    const res = await rollbackMobileRelease({ historyId: row.historyId })
    assignMobileReleaseForm(res.data || {})
    await fetchMobileReleaseHistory()
    ElMessage.success('已回滚到选中版本')
  } finally {
    savingMobileRelease.value = false
  }
}

function apkDownloadUrl(row = {}) {
  return row.downloadUrl || row.apkPath || ''
}

function handleDownloadMobileRelease(row) {
  const url = apkDownloadUrl(row)
  if (!url) {
    ElMessage.warning('该发布记录没有 APK 下载地址')
    return
  }
  window.open(url, '_blank', 'noopener,noreferrer')
}

async function fetchData() {
  loading.value = true
  try {
    await fetchTenantOptions()
    const params = {
      tenantId: filters.tenantId,
      type: filters.type || undefined,
      keyword: filters.keyword || undefined,
      limit: 200
    }
    const [overviewRes, listRes] = await Promise.all([
      getAdminNotificationOverview({ tenantId: filters.tenantId }),
      listAdminNotifications(params)
    ])
    overview.value = overviewRes.data || {}
    notifications.value = listRes.data || []
    notificationPage.value = 1
    await fetchMobileReleaseConfig()
    await fetchMobileReleaseHistory()
    await fetchMobileReleaseBuilds()
    await fetchDeliveryConfig()
  } finally {
    loading.value = false
  }
}

async function fetchDeliveryConfig() {
  const res = await getNotificationDeliveryConfig({ tenantId: filters.tenantId })
  Object.assign(deliveryConfig, {
    tenantId: filters.tenantId,
    enabled: true,
    systemEnabled: true,
    messageEnabled: true,
    activityEnabled: true,
    upgradeEnabled: true
  }, res.data || {})
}

async function handleSaveDeliveryConfig() {
  savingDeliveryConfig.value = true
  try {
    const res = await saveNotificationDeliveryConfig({ ...deliveryConfig, tenantId: filters.tenantId })
    Object.assign(deliveryConfig, res.data || {})
    ElMessage.success('通知开关已保存')
  } finally {
    savingDeliveryConfig.value = false
  }
}

function typeLabel(type) {
  const found = typeOptions.find((item) => item.value === type)
  return found ? found.label : type || '-'
}

function priorityLabel(priority) {
  return priority >= 2 ? '紧急' : priority === 1 ? '重要' : '普通'
}

function priorityTag(priority) {
  return priority >= 2 ? 'danger' : priority === 1 ? 'warning' : 'info'
}

function targetLabel(targetType) {
  return targetType === 1 ? '指定用户' : targetType === 2 ? '指定角色' : '全部用户'
}

function buildStatusTag(status) {
  return {
    queued: 'info',
    running: 'warning',
    success: 'success',
    failed: 'danger'
  }[status] || 'info'
}

onMounted(fetchData)

onBeforeUnmount(() => {
  stopBuildPolling()
})
</script>

<style scoped>
.notification-center-page {
  display: grid;
  gap: 18px;
}

.notification-header,
.filter-bar,
.table-panel,
.metric-row,
.delivery-panel,
.mobile-release-panel {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fff;
}

.notification-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
}

.notification-header h1 {
  margin: 0;
  font-size: 22px;
  color: #1f2d3d;
}

.notification-header p {
  margin: 6px 0 0;
  color: #5f6b7a;
}

.header-actions,
.filter-bar,
.drawer-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.metric-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  overflow: hidden;
}

.metric-item {
  padding: 16px 18px;
  border-right: 1px solid #edf0f3;
}

.metric-item:last-child {
  border-right: 0;
}

.metric-item span {
  display: block;
  color: #697586;
  font-size: 13px;
}

.metric-item strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  line-height: 1;
  color: #1f7a55;
}

.delivery-panel {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 20px;
}

.delivery-panel h2 {
  margin: 0;
  font-size: 18px;
  color: #1f2d3d;
}

.delivery-panel p {
  margin: 6px 0 0;
  color: #5f6b7a;
}

.delivery-switches {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.mobile-release-panel {
  padding: 18px 20px 8px;
}

.mobile-release-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.mobile-release-header h2 {
  margin: 0;
  font-size: 18px;
  color: #1f2d3d;
}

.mobile-release-header p {
  margin: 6px 0 0;
  color: #5f6b7a;
}

.mobile-release-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.mobile-release-history {
  margin-top: 14px;
}

.mobile-build-live {
  margin-top: 14px;
  margin-bottom: 14px;
}

.build-live-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 8px;
  color: #5f6b7a;
}

.mobile-build-log {
  min-height: 120px;
  max-height: 280px;
  overflow: auto;
  margin: 0 0 10px;
  padding: 12px;
  border-radius: 8px;
  background: #111827;
  color: #d1fae5;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
}

.history-title {
  margin-bottom: 8px;
  font-weight: 600;
  color: #1f2d3d;
}

.history-pagination {
  margin-top: 10px;
  justify-content: flex-end;
}

.apk-url-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  color: #667085;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}

.filter-bar {
  padding: 14px 16px;
}

.keyword-input {
  width: 260px;
}

.type-select {
  width: 150px;
}

.tenant-input {
  width: 130px;
}

.table-panel {
  padding: 8px 12px 14px;
}

.notification-form {
  display: grid;
  gap: 2px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.upgrade-fields {
  padding: 12px 14px 2px;
  border: 1px solid #f1d7a1;
  border-radius: 8px;
  background: #fffaf0;
}

.drawer-actions {
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .notification-header {
    flex-direction: column;
  }

  .mobile-release-header {
    flex-direction: column;
  }

  .delivery-panel {
    flex-direction: column;
  }

  .metric-row,
  .form-grid,
  .mobile-release-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .keyword-input,
  .type-select,
  .tenant-input {
    width: 100%;
  }
}

@media (max-width: 560px) {
  .metric-row,
  .form-grid,
  .mobile-release-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
