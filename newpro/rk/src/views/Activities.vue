<template>
  <div class="activities-page design-event-reference-page">
    <PortalEventHero
      class="design-event-hero"
      eyebrow="活动项目"
      title="发现精彩活动"
      description="以项目化视角梳理公开活动，突出报名窗口、参与规模与执行节奏，帮助成员快速识别当前可参与事项。"
      :meta-items="heroMetaItems"
      :stats="heroStats"
      :badges="heroBadges"
    >
      <template #actions>
        <router-link class="hero-link primary" to="/competition">查看竞赛项目</router-link>
        <el-button
          v-if="canSubmitActivity"
          color="#c06b3e"
          class="hero-button"
          @click="openSubmitDialog"
        >
          发起活动
        </el-button>
      </template>

      <template #aside>
        <div class="hero-aside-card">
          <p class="aside-eyebrow">重点项目</p>
          <template v-if="featuredActivity">
            <div class="aside-badges">
              <span class="aside-badge">{{ getStatusText(featuredActivity.status) }}</span>
              <span class="aside-badge neutral">{{ formatType(featuredActivity.type) || '常规活动' }}</span>
            </div>
            <h2>{{ featuredActivity.title }}</h2>
            <p>{{ getShortText(toPlainText(featuredActivity.description), 120) }}</p>

            <div class="aside-meta">
              <div class="aside-meta-item">
                <span>时间</span>
                <strong>{{ formatDate(featuredActivity.startTime) }}</strong>
              </div>
              <div class="aside-meta-item">
                <span>地点</span>
                <strong>{{ featuredActivity.location || '待定' }}</strong>
              </div>
            </div>

            <div class="aside-actions">
              <el-button type="primary" @click="handleViewDetail(featuredActivity)">查看详情</el-button>
              <el-button
                v-if="canJoin(featuredActivity)"
                type="success"
                plain
                @click="handleJoin(featuredActivity)"
              >
                立即报名
              </el-button>
            </div>
          </template>
          <el-empty v-else description="暂无重点活动" :image-size="88" />
        </div>
      </template>
    </PortalEventHero>

    <main class="container activities-shell">
      <PortalEventFilterToolbar
        class="design-event-filter-pills"
        eyebrow="活动筛选"
        title="筛选公开活动与报名机会"
        description="保留原有类型和状态筛选能力，同时用更清晰的概要面板呈现当前列表规模与可报名数量。"
        :summary="filterSummary"
      >
        <template #actions>
          <el-button v-if="currentType || currentStatus" @click="handleReset">重置筛选</el-button>
          <el-button
            v-if="canSubmitActivity"
            type="primary"
            @click="openSubmitDialog"
          >
            发起活动
          </el-button>
        </template>

        <div class="filter-grid">
          <label class="filter-field">
            <span class="field-label">活动类型</span>
            <el-select v-model="currentType" placeholder="全部类型" clearable @change="handleFilter">
              <el-option label="全部" value="" />
              <el-option label="技术分享" value="技术分享" />
              <el-option label="项目实践" value="项目实践" />
              <el-option label="竞赛指导" value="竞赛指导" />
              <el-option label="团建活动" value="团建活动" />
            </el-select>
          </label>

          <label class="filter-field">
            <span class="field-label">活动状态</span>
            <el-select v-model="currentStatus" placeholder="全部状态" clearable @change="handleFilter">
              <el-option label="全部" value="" />
              <el-option label="即将开始" value="upcoming" />
              <el-option label="进行中" value="ongoing" />
              <el-option label="已结束" value="ended" />
            </el-select>
          </label>
        </div>
      </PortalEventFilterToolbar>

      <section v-if="featuredActivity" class="surface-panel spotlight-panel">
        <PortalEventSectionHeader
          eyebrow="重点活动"
          title="重点活动看板"
          description="将当前活动列表中的重点项目独立呈现，帮助成员快速进入报名与详情页。"
        />

        <article class="spotlight-card">
          <div class="spotlight-date">
            <span class="spotlight-day">{{ getDay(featuredActivity.startTime) }}</span>
            <span class="spotlight-month">{{ getMonth(featuredActivity.startTime) }}</span>
          </div>

          <div class="spotlight-body">
            <div class="spotlight-tags">
              <el-tag :type="getStatusType(featuredActivity.status)" effect="dark">
                {{ getStatusText(featuredActivity.status) }}
              </el-tag>
              <el-tag type="info" effect="plain">
                {{ formatType(featuredActivity.type) || '常规活动' }}
              </el-tag>
            </div>

            <h2>{{ featuredActivity.title }}</h2>
            <p>{{ toPlainText(featuredActivity.description) || '暂无活动简介' }}</p>

            <div class="spotlight-metrics">
              <div class="spotlight-metric">
                <span>时间</span>
                <strong>{{ formatDateTime(featuredActivity.startTime) }}</strong>
              </div>
              <div class="spotlight-metric">
                <span>地点</span>
                <strong>{{ featuredActivity.location || '待定' }}</strong>
              </div>
              <div class="spotlight-metric">
                <span>报名规模</span>
                <strong>{{ formatCapacity(featuredActivity) }}</strong>
              </div>
            </div>
          </div>

          <div class="spotlight-actions">
            <el-button type="primary" @click="handleViewDetail(featuredActivity)">查看详情</el-button>
            <el-button
              v-if="canJoin(featuredActivity)"
              type="success"
              plain
              @click="handleJoin(featuredActivity)"
            >
              立即报名
            </el-button>
          </div>
        </article>
      </section>

      <section class="surface-panel list-panel">
        <PortalEventSectionHeader
          eyebrow="活动目录"
          title="活动项目列表"
          :description="listDescription"
        />

        <div v-if="loading" class="loading-state">
          <el-icon class="loading-icon"><Loading /></el-icon>
          <p>正在加载活动项目...</p>
        </div>

        <div v-else-if="activityList.length === 0" class="empty-state">
          <el-empty description="暂无活动信息">
            <el-button type="primary" @click="handleReset">查看全部活动</el-button>
          </el-empty>
        </div>

        <div v-else class="program-grid design-event-card-grid">
          <article
            v-for="activity in activityList"
            :key="activity.id"
            class="program-card"
          >
            <div class="program-rail">
              <span class="program-day">{{ getDay(activity.startTime) }}</span>
              <span class="program-month">{{ getMonth(activity.startTime) }}</span>
            </div>

            <div class="program-main">
              <div class="program-header">
                <div>
                  <div class="program-tags">
                    <el-tag :type="getStatusType(activity.status)" size="small">
                      {{ getStatusText(activity.status) }}
                    </el-tag>
                    <el-tag size="small" effect="plain">
                      {{ formatType(activity.type) || '常规活动' }}
                    </el-tag>
                  </div>
                  <h3>{{ activity.title }}</h3>
                </div>
                <div class="program-capacity">
                  <span>参与规模</span>
                  <strong>{{ formatCapacity(activity) }}</strong>
                </div>
              </div>

              <p class="program-description">{{ toPlainText(activity.description) || '暂无活动简介' }}</p>

              <div class="program-facts">
                <div class="fact-item">
                  <el-icon><Clock /></el-icon>
                  <span>{{ formatTimeRange(activity.startTime, activity.endTime) }}</span>
                </div>
                <div class="fact-item">
                  <el-icon><Location /></el-icon>
                  <span>{{ activity.location || '待定' }}</span>
                </div>
                <div class="fact-item">
                  <el-icon><User /></el-icon>
                  <span>{{ activity.participantCount || 0 }} 人已报名</span>
                </div>
              </div>

              <div class="program-actions">
                <el-button type="primary" @click="handleViewDetail(activity)">查看详情</el-button>
                <el-button
                  v-if="canJoin(activity)"
                  type="success"
                  plain
                  @click="handleJoin(activity)"
                >
                  立即报名
                </el-button>
              </div>
            </div>
          </article>
        </div>
      </section>

      <section v-if="sharedActivityList.length > 0" class="surface-panel shared-panel">
        <PortalEventSectionHeader
          eyebrow="共享入口"
          title="跨租户共享活动"
          description="保持原有共享活动入口，统一成更清晰的公开项目卡片布局。"
        />

        <div class="program-grid shared-grid">
          <article
            v-for="activity in sharedActivityList"
            :key="`shared-${activity.id}`"
            class="program-card shared-card"
          >
            <div class="program-rail shared-rail">
              <span class="shared-label">共享</span>
              <span class="program-month">{{ getMonth(activity.startTime) }}</span>
            </div>

            <div class="program-main">
              <div class="program-header">
                <div>
                  <div class="program-tags">
                    <el-tag type="warning" size="small">跨租户</el-tag>
                    <el-tag :type="getStatusType(activity.status)" size="small">
                      {{ getStatusText(activity.status) }}
                    </el-tag>
                  </div>
                  <h3>{{ activity.title }}</h3>
                </div>
                <div class="program-capacity">
                  <span>参与规模</span>
                  <strong>{{ formatCapacity(activity) }}</strong>
                </div>
              </div>

              <p class="program-description">{{ toPlainText(activity.description) || '暂无活动简介' }}</p>

              <div class="program-facts">
                <div class="fact-item">
                  <el-icon><Clock /></el-icon>
                  <span>{{ formatTimeRange(activity.startTime, activity.endTime) }}</span>
                </div>
                <div class="fact-item">
                  <el-icon><Location /></el-icon>
                  <span>{{ activity.location || '待定' }}</span>
                </div>
                <div class="fact-item">
                  <el-icon><User /></el-icon>
                  <span>{{ activity.participantCount || 0 }} 人已报名</span>
                </div>
              </div>

              <div class="program-actions">
                <el-button type="primary" @click="handleViewDetail(activity)">查看详情</el-button>
                <el-button
                  v-if="canJoin(activity)"
                  type="success"
                  plain
                  @click="handleJoin(activity)"
                >
                  立即报名
                </el-button>
              </div>
            </div>
          </article>
        </div>
      </section>
    </main>

    <el-dialog
      v-model="submitDialogVisible"
      title="发起活动"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form ref="submitFormRef" :model="submitForm" :rules="submitRules" label-width="110px">
        <el-form-item label="活动名称" prop="title">
          <el-input v-model="submitForm.title" placeholder="请输入活动名称" />
        </el-form-item>
        <el-form-item label="活动类型" prop="type">
          <el-select v-model="submitForm.type" placeholder="请选择活动类型" style="width: 100%">
            <el-option label="讲座" :value="1" />
            <el-option label="竞赛" :value="2" />
            <el-option label="培训" :value="3" />
            <el-option label="娱乐" :value="4" />
            <el-option label="志愿服务" :value="5" />
            <el-option label="其他" :value="6" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="submitForm.startTime"
            type="datetime"
            placeholder="请选择开始时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="submitForm.endTime"
            type="datetime"
            placeholder="请选择结束时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="报名开始时间" prop="registrationStartTime">
          <el-date-picker
            v-model="submitForm.registrationStartTime"
            type="datetime"
            placeholder="请选择报名开始时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="报名结束时间" prop="registrationEndTime">
          <el-date-picker
            v-model="submitForm.registrationEndTime"
            type="datetime"
            placeholder="请选择报名结束时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="活动地点" prop="location">
          <el-input v-model="submitForm.location" placeholder="请输入活动地点" />
        </el-form-item>
        <el-form-item label="人数上限" prop="maxParticipants">
          <el-input-number
            v-model="submitForm.maxParticipants"
            :min="1"
            :max="1000"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="活动描述" prop="description">
          <el-input
            v-model="submitForm.description"
            type="textarea"
            :rows="4"
            placeholder="请输入活动描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitActivity">提交审批</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Clock, Loading, Location, User } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PortalEventFilterToolbar from '@/components/portal-events/PortalEventFilterToolbar.vue'
import PortalEventHero from '@/components/portal-events/PortalEventHero.vue'
import PortalEventSectionHeader from '@/components/portal-events/PortalEventSectionHeader.vue'
import { getActivityPage, getSharedActivities, registerActivity, submitActivity } from '@/api/activity'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const STATUS_MAP = {
  1: 'upcoming',
  2: 'upcoming',
  3: 'ongoing',
  4: 'ended'
}

const TYPE_TEXT_MAP = {
  1: '讲座',
  2: '竞赛',
  3: '培训',
  4: '娱乐',
  5: '志愿服务',
  6: '其他'
}

const loading = ref(false)
const activityList = ref([])
const sharedActivityList = ref([])
const currentType = ref('')
const currentStatus = ref('')
const submitDialogVisible = ref(false)
const submitFormRef = ref(null)

const canSubmitActivity = computed(() => userStore.isLoggedIn && userStore.hasRole([2]))

const createEmptySubmitForm = () => ({
  title: '',
  type: 1,
  startTime: '',
  endTime: '',
  registrationStartTime: '',
  registrationEndTime: '',
  location: '',
  maxParticipants: 50,
  description: ''
})

const submitForm = ref(createEmptySubmitForm())
const submitRules = {
  title: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择活动类型', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  registrationStartTime: [{ required: true, message: '请选择报名开始时间', trigger: 'change' }],
  registrationEndTime: [{ required: true, message: '请选择报名结束时间', trigger: 'change' }],
  location: [{ required: true, message: '请输入活动地点', trigger: 'blur' }],
  description: [{ required: true, message: '请输入活动描述', trigger: 'blur' }]
}

const featuredActivity = computed(() => activityList.value[0] || null)

const openActivityCount = computed(() => activityList.value.filter((item) => item.status === 'upcoming').length)

const heroMetaItems = computed(() => [
  currentType.value ? `类型：${currentType.value}` : '类型：全部',
  currentStatus.value ? `状态：${getStatusText(currentStatus.value)}` : '状态：全部',
  canSubmitActivity.value ? '支持成员发起活动申请' : '公开查看活动详情与报名窗口'
])

const heroStats = computed(() => [
  {
    label: '公开活动',
    value: String(activityList.value.length),
    hint: '当前主列表活动数'
  },
  {
    label: '可报名活动',
    value: String(openActivityCount.value),
    hint: '状态为即将开始'
  },
  {
    label: '共享活动',
    value: String(sharedActivityList.value.length),
    hint: '跨租户开放项目'
  }
])

const heroBadges = computed(() => [
  { label: '公开活动', tone: 'info' },
  { label: currentStatus.value ? `状态 ${getStatusText(currentStatus.value)}` : '开放目录', tone: 'warm' }
])

const filterSummary = computed(() => [
  {
    label: '当前显示',
    value: `${activityList.value.length}`,
    hint: '列表结果数'
  },
  {
    label: '可直接报名',
    value: `${activityList.value.filter(canJoin).length}`,
    hint: '未满且即将开始'
  },
  {
    label: '共享入口',
    value: `${sharedActivityList.value.length}`,
    hint: '独立保留共享区'
  }
])

const listDescription = computed(() => {
  const parts = ['保留原有活动列表与报名行为']
  if (currentType.value) {
    parts.push(`当前类型为“${currentType.value}”`)
  }
  if (currentStatus.value) {
    parts.push(`当前状态为“${getStatusText(currentStatus.value)}”`)
  }
  return `${parts.join('，')}。`
})

function transformActivity(item) {
  return {
    ...item,
    id: item.id,
    title: item.activityName || item.title,
    description: item.description || item.activityContent || item.content || '',
    content: item.content || item.activityContent || item.description || '',
    type: item.activityType || item.type,
    status: STATUS_MAP[item.activityStatus] || item.status || 'upcoming',
    startTime: item.startTime,
    endTime: item.endTime,
    registrationStartTime: item.registrationStartTime,
    registrationEndTime: item.registrationEndTime,
    location: item.location,
    participantCount: item.currentParticipants || item.participantCount || 0,
    maxParticipants: item.maxParticipants || 999,
    organizer: item.organizer || item.creatorName || item.creator,
    coverImage: item.coverImage
  }
}

function getDay(dateString) {
  if (!dateString) return '--'
  const date = new Date(dateString)
  return String(date.getDate()).padStart(2, '0')
}

function getMonth(dateString) {
  if (!dateString) return '--'
  const date = new Date(dateString)
  const months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
  return months[date.getMonth()]
}

function formatDate(dateString) {
  if (!dateString) return '待定'
  const date = new Date(dateString)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${month}-${day}`
}

function formatDateTime(dateString) {
  if (!dateString) return '待定'
  const date = new Date(dateString)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

function formatTime(dateString) {
  if (!dateString) return '--'
  const date = new Date(dateString)
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${hours}:${minutes}`
}

function formatTimeRange(startTime, endTime) {
  if (!startTime && !endTime) return '时间待定'
  return `${formatDateTime(startTime)} - ${formatTime(endTime)}`
}

function formatType(type) {
  if (!type && type !== 0) return ''
  if (TYPE_TEXT_MAP[type]) {
    return TYPE_TEXT_MAP[type]
  }
  return String(type)
}

function formatCapacity(activity) {
  const currentCount = activity.participantCount || 0
  const maxCount = activity.maxParticipants || '∞'
  return `${currentCount}/${maxCount}`
}

function getShortText(text, limit = 80) {
  if (!text) return '暂无项目简介'
  return text.length > limit ? `${text.slice(0, limit)}...` : text
}

function toPlainText(text) {
  return String(text || '').replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
}

function getStatusText(status) {
  const map = {
    upcoming: '即将开始',
    ongoing: '进行中',
    ended: '已结束'
  }
  return map[status] || status
}

function getStatusType(status) {
  const map = {
    upcoming: 'warning',
    ongoing: 'success',
    ended: 'info'
  }
  return map[status] || 'info'
}

function canJoin(activity) {
  const maxCount = activity.maxParticipants || 999
  return activity.status === 'upcoming' && (activity.participantCount || 0) < maxCount
}

async function fetchActivities() {
  loading.value = true
  try {
    const res = await getActivityPage({
      type: currentType.value,
      status: currentStatus.value,
      page: 1,
      size: 20
    })
    const rawData = res.data?.records || res.data || []
    activityList.value = rawData.map(transformActivity)

    const sharedRes = await getSharedActivities(10)
    sharedActivityList.value = (sharedRes.data || []).map(transformActivity)
  } catch (error) {
    console.error('获取活动失败:', error)
    activityList.value = [
      {
        id: 1,
        title: '前端开发技术分享会',
        description: '分享 Vue 3 和 React 最新特性，探讨前端开发最佳实践。',
        startTime: '2024-03-15T14:00:00',
        endTime: '2024-03-15T16:00:00',
        location: '教学楼 A201',
        type: '技术分享',
        status: 'upcoming',
        participantCount: 20,
        maxParticipants: 50
      },
      {
        id: 2,
        title: '项目开发实践活动',
        description: '小组合作开发项目，提升实战能力。',
        startTime: '2024-03-20T18:00:00',
        endTime: '2024-03-20T21:00:00',
        location: '实验室 302',
        type: '项目实践',
        status: 'upcoming',
        participantCount: 15,
        maxParticipants: 20
      }
    ]
    sharedActivityList.value = []
  } finally {
    loading.value = false
  }
}

function handleFilter() {
  fetchActivities()
}

function handleReset() {
  currentType.value = ''
  currentStatus.value = ''
  fetchActivities()
}

function handleViewDetail(activity) {
  router.push(`/activities/${activity.id}`)
}

function formatDateForBackend(date) {
  if (!date) return null
  const current = new Date(date)
  const pad = (value) => String(value).padStart(2, '0')
  return `${current.getFullYear()}-${pad(current.getMonth() + 1)}-${pad(current.getDate())} ${pad(current.getHours())}:${pad(current.getMinutes())}:${pad(current.getSeconds())}`
}

function openSubmitDialog() {
  submitForm.value = createEmptySubmitForm()
  submitDialogVisible.value = true
}

async function handleJoin(activity) {
  try {
    await ElMessageBox.confirm(
      `确定要报名参加“${activity.title}”吗？`,
      '报名确认',
      {
        confirmButtonText: '确定报名',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    const res = await registerActivity(activity.id)
    if (res.code === 200 || res.success) {
      ElMessage.success('报名成功！')
      activity.participantCount = (activity.participantCount || 0) + 1
    } else {
      ElMessage.error(res.msg || '报名失败，请稍后重试')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('报名失败:', error)
      ElMessage.error('报名失败，请稍后重试')
    }
  }
}

async function handleSubmitActivity() {
  if (!submitFormRef.value) return
  try {
    await submitFormRef.value.validate()
    const payload = {
      activityName: submitForm.value.title,
      activityType: submitForm.value.type,
      organizer: userStore.userName,
      location: submitForm.value.location,
      maxParticipants: submitForm.value.maxParticipants,
      startTime: formatDateForBackend(submitForm.value.startTime),
      endTime: formatDateForBackend(submitForm.value.endTime),
      registrationStartTime: formatDateForBackend(submitForm.value.registrationStartTime),
      registrationEndTime: formatDateForBackend(submitForm.value.registrationEndTime),
      content: submitForm.value.description
    }
    const res = await submitActivity(payload)
    if (res.code === 200) {
      ElMessage.success('提交成功，等待负责人审核')
      submitDialogVisible.value = false
    } else {
      ElMessage.error(res.msg || '提交失败')
    }
  } catch (error) {
    ElMessage.error(error?.message || '提交失败')
  }
}

onMounted(() => {
  fetchActivities()
})
</script>

<style lang="scss" scoped>
@use '../styles/variables.scss' as *;

.activities-page {
  min-height: 100vh;
  padding-bottom: 56px;
  background:
    linear-gradient(180deg, rgba($primary-dark, 0.04), rgba($bg-white, 0.86) 24%, $bg-white 100%),
    $bg-white;
}

.activities-shell {
  display: flex;
  flex-direction: column;
  gap: 28px;
  padding-top: 28px;
}

.surface-panel {
  padding: 28px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba($primary-color, 0.08);
  box-shadow: 0 22px 44px rgba(24, 33, 31, 0.08);
}

.hero-link,
.hero-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  padding: 0 18px;
  border-radius: $border-radius-full;
  font-weight: 600;
  text-decoration: none;
}

.hero-link.primary {
  color: $text-primary;
  background: $bg-white;
}

.hero-aside-card {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 24px;
  border-radius: 24px;
  background: rgba(11, 19, 17, 0.26);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: 0 18px 40px rgba(8, 14, 13, 0.22);
}

.hero-aside-card h2,
.hero-aside-card p {
  margin: 0;
}

.aside-eyebrow {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.72);
}

.aside-badges,
.aside-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.aside-badge {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: $border-radius-full;
  font-size: 12px;
  font-weight: 600;
  color: $text-white;
  background: rgba($warning-color, 0.92);
}

.aside-badge.neutral {
  background: rgba(255, 255, 255, 0.14);
}

.aside-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.aside-meta-item {
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.08);
}

.aside-meta-item span,
.aside-meta-item strong {
  display: block;
}

.aside-meta-item span {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.aside-meta-item strong {
  margin-top: 6px;
  color: $text-white;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 240px));
  gap: 16px;
}

.filter-field {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.field-label {
  font-size: 13px;
  font-weight: 600;
  color: $text-secondary;
}

.spotlight-panel {
  background:
    linear-gradient(135deg, rgba($primary-dark, 0.04), rgba($accent-color, 0.08)),
    rgba(255, 255, 255, 0.96);
}

.spotlight-card {
  display: grid;
  grid-template-columns: 132px minmax(0, 1fr) auto;
  gap: 24px;
  align-items: center;
  margin-top: 24px;
  padding: 28px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba($primary-color, 0.08);
}

.spotlight-date {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 132px;
  border-radius: 24px;
  background: linear-gradient(135deg, $accent-color, lighten($accent-color, 12%));
  color: $text-white;
}

.spotlight-day {
  font-size: 44px;
  font-weight: 800;
  line-height: 1;
}

.spotlight-month {
  font-size: 16px;
  font-weight: 600;
}

.spotlight-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.spotlight-body h2,
.spotlight-body p,
.spotlight-metric span,
.spotlight-metric strong {
  margin: 0;
}

.spotlight-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.spotlight-body h2 {
  font-size: clamp(1.7rem, 2vw, 2.2rem);
  color: $text-primary;
}

.spotlight-body p {
  color: $text-secondary;
  line-height: 1.8;
}

.spotlight-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.spotlight-metric {
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba($bg-light, 0.8);
}

.spotlight-metric span {
  display: block;
  font-size: 12px;
  color: $text-light;
}

.spotlight-metric strong {
  display: block;
  margin-top: 8px;
  color: $text-primary;
}

.spotlight-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.loading-state,
.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 240px;
}

.loading-state {
  flex-direction: column;
  gap: 14px;
  color: $text-secondary;
}

.loading-icon {
  font-size: 34px;
  color: $accent-color;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.program-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
  margin-top: 24px;
}

.program-card {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  gap: 20px;
  padding: 24px;
  border-radius: 22px;
  background:
    linear-gradient(180deg, rgba($bg-white, 0.96), rgba(255, 255, 255, 0.96)),
    #ffffff;
  border: 1px solid rgba($primary-color, 0.08);
  box-shadow: 0 16px 36px rgba(24, 33, 31, 0.06);
}

.program-rail {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: 20px;
  background: linear-gradient(180deg, rgba($primary-color, 0.12), rgba($accent-color, 0.14));
  color: $text-primary;
}

.program-day {
  font-size: 34px;
  font-weight: 800;
  line-height: 1;
}

.program-month {
  font-size: 14px;
  font-weight: 600;
}

.program-main {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.program-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.program-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}

.program-header h3,
.program-description,
.program-capacity span,
.program-capacity strong {
  margin: 0;
}

.program-header h3 {
  font-size: 20px;
  line-height: 1.35;
  color: $text-primary;
}

.program-capacity {
  min-width: 104px;
  padding: 12px 14px;
  border-radius: 16px;
  text-align: right;
  background: rgba($bg-light, 0.72);
}

.program-capacity span {
  display: block;
  font-size: 12px;
  color: $text-light;
}

.program-capacity strong {
  display: block;
  margin-top: 6px;
  color: $text-primary;
}

.program-description {
  color: $text-secondary;
  line-height: 1.75;
}

.program-facts {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
}

.fact-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 38px;
  padding: 0 14px;
  border-radius: $border-radius-full;
  background: rgba($bg-light, 0.72);
  color: $text-secondary;
}

.program-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.shared-grid {
  margin-top: 24px;
}

.shared-card {
  background:
    linear-gradient(180deg, rgba(255, 248, 241, 0.98), rgba(255, 255, 255, 0.96)),
    #ffffff;
}

.shared-rail {
  background: linear-gradient(180deg, rgba($warning-color, 0.2), rgba($accent-color, 0.16));
}

.shared-label {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 56px;
  min-height: 28px;
  padding: 0 10px;
  border-radius: $border-radius-full;
  background: rgba($warning-color, 0.18);
  font-size: 12px;
  font-weight: 700;
  color: darken($warning-color, 18%);
}

@media (max-width: $breakpoint-lg) {
  .spotlight-card,
  .program-grid {
    grid-template-columns: 1fr;
  }

  .spotlight-card {
    grid-template-columns: 1fr;
  }

  .spotlight-actions {
    flex-direction: row;
    flex-wrap: wrap;
  }

  .program-card {
    grid-template-columns: 1fr;
  }

  .program-rail {
    min-height: 88px;
  }
}

@media (max-width: $breakpoint-md) {
  .activities-shell {
    gap: 22px;
    padding-top: 22px;
  }

  .surface-panel {
    padding: 22px;
    border-radius: 20px;
  }

  .aside-meta,
  .filter-grid,
  .spotlight-metrics {
    grid-template-columns: 1fr;
  }

  .program-header {
    flex-direction: column;
  }

  .program-capacity {
    width: 100%;
    text-align: left;
  }
}
</style>
