<template>
  <div class="activity-detail-page design-event-detail-page">
    <div v-if="loading" class="container loading-state">
      <el-icon class="loading-icon"><Loading /></el-icon>
      <p>{{ t('pages.activityDetail.loading') }}</p>
    </div>

    <div v-else-if="activity" class="detail-flow">
      <div class="container back-strip">
        <el-button text @click="handleBack">
          <el-icon><ArrowLeft /></el-icon>
          {{ t('pages.activityDetail.backToList') }}
        </el-button>
      </div>

      <PortalEventHero
        class="design-detail-hero-row"
        eyebrow="活动详情"
        :title="activity.title"
        :description="activitySummary"
        :meta-items="heroMetaItems"
        :stats="heroStats"
        :badges="heroBadges"
      >
        <template #actions>
          <el-button plain class="hero-action" @click="handleBack">{{ t('pages.activityDetail.backToOverview') }}</el-button>
        </template>

        <template #aside>
          <div class="cta-card">
            <p class="cta-eyebrow">报名信息</p>
            <h2>{{ ctaTitle }}</h2>
            <p>{{ ctaDescription }}</p>

            <div class="cta-summary">
              <div class="cta-summary-item">
                <span>{{ t('pages.activityDetail.registeredCount') }}</span>
                <strong>{{ activity.participantCount || 0 }}</strong>
              </div>
              <div class="cta-summary-item">
                <span>{{ t('pages.activityDetail.remainingSlots') }}</span>
                <strong>{{ remainingSlots }}</strong>
              </div>
            </div>

            <el-progress
              :percentage="registrationPercentage"
              :stroke-width="10"
              :format="() => `${registrationPercentage}%`"
            />

            <div class="cta-notes">
              <div class="note-row">
                <span>{{ t('pages.activityDetail.registrationWindow') }}</span>
                <strong>{{ registrationWindowText }}</strong>
              </div>
              <div class="note-row">
                <span>{{ t('pages.activityDetail.location') }}</span>
                <strong>{{ activity.location || t('pages.activityDetail.pendingLocation') }}</strong>
              </div>
            </div>

            <div class="cta-actions">
              <el-button
                v-if="canRegister"
                type="success"
                :loading="registering"
                @click="handleRegister"
              >
                {{ t('pages.activityDetail.registerNow') }}
              </el-button>
              <el-button
                v-if="isRegistered && activity.status !== 'ended'"
                type="danger"
                plain
                :loading="registering"
                @click="handleCancelRegister"
              >
                {{ t('pages.activityDetail.cancelRegistration') }}
              </el-button>
              <el-button
                v-if="isRegistered && activity.status === 'ongoing' && !isCheckedIn"
                type="warning"
                :loading="checkingIn"
                @click="handleCheckIn"
              >
                {{ t('pages.activityDetail.checkIn') }}
              </el-button>
              <el-tag v-if="isCheckedIn" type="success">{{ t('pages.activityDetail.checkedIn') }}</el-tag>
              <el-tag v-if="activity.status === 'ended'" type="info">{{ t('pages.activityDetail.ended') }}</el-tag>
              <el-tag v-else-if="isFull && !isRegistered" type="warning">{{ t('pages.activityDetail.full') }}</el-tag>
            </div>
          </div>
        </template>
      </PortalEventHero>

      <main class="container detail-shell">
        <section class="content-column">
          <section class="surface-panel design-content-card">
            <PortalEventSectionHeader
              :eyebrow="t('pages.activityDetail.overviewEyebrow')"
              :title="t('pages.activityDetail.overviewTitle')"
              :description="t('pages.activityDetail.overviewDescription')"
            />

            <div class="overview-grid">
              <article class="overview-card">
                <span>{{ t('pages.activityDetail.activityType') }}</span>
                <strong>{{ formatActivityType(activity.type) }}</strong>
              </article>
              <article class="overview-card">
                <span>{{ t('pages.activityDetail.executionTime') }}</span>
                <strong>{{ executionWindowText }}</strong>
              </article>
              <article class="overview-card">
                <span>{{ t('pages.activityDetail.organizer') }}</span>
                <strong>{{ activity.organizer || activity.creator || t('pages.activityDetail.emptyOrganizer') }}</strong>
              </article>
              <article class="overview-card">
                <span>{{ t('pages.activityDetail.currentStatus') }}</span>
                <strong>{{ getStatusText(activity.status) }}</strong>
              </article>
            </div>
          </section>

          <section class="surface-panel">
            <PortalEventSectionHeader
              :eyebrow="t('pages.activityDetail.detailEyebrow')"
              :title="t('pages.activityDetail.detailTitle')"
              :description="t('pages.activityDetail.detailDescription')"
            />

            <div v-if="contentHtml" class="content-body" v-html="contentHtml"></div>
            <el-empty v-else :description="t('pages.activityDetail.emptyContent')" />
          </section>

          <section v-if="activity.requirements || activity.attachmentUrl" class="surface-panel">
            <PortalEventSectionHeader
              :eyebrow="t('pages.activityDetail.supportEyebrow')"
              :title="t('pages.activityDetail.supportTitle')"
              :description="t('pages.activityDetail.supportDescription')"
            />

            <div class="support-stack">
              <article v-if="activity.requirements" class="support-card">
                <h3>{{ t('pages.activityDetail.requirements') }}</h3>
                <p>{{ activity.requirements }}</p>
              </article>

              <article v-if="activity.attachmentUrl" class="support-card">
                <h3>{{ t('pages.activityDetail.attachment') }}</h3>
                <a
                  :href="resolveMediaUrl(activity.attachmentUrl)"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="attachment-link"
                >
                  {{ t('pages.activityDetail.downloadAttachment') }}
                </a>
              </article>
            </div>
          </section>

          <NewsComments target-type="activity" :target-id="activityId" />
        </section>

        <aside class="sidebar-column design-info-sidebar">
          <section class="surface-panel">
            <PortalEventSectionHeader
              :eyebrow="t('pages.activityDetail.factsEyebrow')"
              :title="t('pages.activityDetail.factsTitle')"
              :description="t('pages.activityDetail.factsDescription')"
              align="start"
            />

            <div class="fact-list">
              <div class="fact-row">
                <span>{{ t('pages.activityDetail.executionTime') }}</span>
                <strong>{{ executionWindowText }}</strong>
              </div>
              <div class="fact-row">
                <span>{{ t('pages.activityDetail.registrationWindow') }}</span>
                <strong>{{ registrationWindowText }}</strong>
              </div>
              <div class="fact-row">
                <span>{{ t('pages.activityDetail.location') }}</span>
                <strong>{{ activity.location || t('pages.activityDetail.pendingLocation') }}</strong>
              </div>
              <div class="fact-row">
                <span>{{ t('pages.activityDetail.organizer') }}</span>
                <strong>{{ activity.organizer || activity.creator || t('pages.activityDetail.emptyOrganizer') }}</strong>
              </div>
            </div>
          </section>

          <section class="surface-panel">
            <PortalEventSectionHeader
              :eyebrow="t('pages.activityDetail.metricsEyebrow')"
              :title="t('pages.activityDetail.metricsTitle')"
              :description="t('pages.activityDetail.metricsDescription')"
              align="start"
            />

            <div class="metric-stack">
              <article class="metric-card">
                <span>{{ t('pages.activityDetail.participantCount') }}</span>
                <strong>{{ activity.participantCount || 0 }}</strong>
              </article>
              <article class="metric-card">
                <span>{{ t('pages.activityDetail.capacityLimit') }}</span>
                <strong>{{ activity.maxParticipants || t('pages.activityDetail.unlimited') }}</strong>
              </article>
              <article class="metric-card">
                <span>{{ t('pages.activityDetail.remainingSlots') }}</span>
                <strong>{{ remainingSlots }}</strong>
              </article>
            </div>
          </section>

          <section v-if="relatedActivities.length > 0" class="surface-panel">
            <PortalEventSectionHeader
              :eyebrow="t('pages.activityDetail.relatedEyebrow')"
              :title="t('pages.activityDetail.relatedTitle')"
              :description="t('pages.activityDetail.relatedDescription')"
              align="start"
            />

            <div class="related-list">
              <button
                v-for="item in relatedActivities"
                :key="item.id"
                type="button"
                class="related-item"
                @click="handleActivityClick(item.id)"
              >
                <span class="related-date">{{ formatDate(item.startTime) }}</span>
                <strong>{{ item.title }}</strong>
                <span>{{ item.location || t('pages.activityDetail.pendingLocation') }}</span>
              </button>
            </div>
          </section>
        </aside>
      </main>
    </div>

    <div v-else class="container empty-state">
      <el-empty :description="t('pages.activityDetail.emptyState')">
        <el-button type="primary" @click="handleBack">{{ t('pages.activityDetail.backToList') }}</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Loading } from '@element-plus/icons-vue'
import PortalEventHero from '@/components/portal-events/PortalEventHero.vue'
import PortalEventSectionHeader from '@/components/portal-events/PortalEventSectionHeader.vue'
import NewsComments from '@/components/NewsComments.vue'
import { useUserStore } from '@/stores/user'
import { resolveMediaUrl } from '@/utils/mediaUrl'
import {
  cancelRegistration,
  checkRegistered,
  checkIn,
  getActivityDetail,
  getActivityList,
  registerActivity
} from '@/api/activity'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const STATUS_MAP = {
  1: 'upcoming',
  2: 'upcoming',
  3: 'ongoing',
  4: 'ended'
}

const loading = ref(false)
const registering = ref(false)
const checkingIn = ref(false)
const activity = ref(null)
const isRegistered = ref(false)
const isCheckedIn = ref(false)
const relatedActivities = ref([])

const activityId = computed(() => route.params.id)

const remainingSlots = computed(() => {
  if (!activity.value) return 0
  const max = activity.value.maxParticipants
  if (!max) return t('pages.activityDetail.unlimited')
  const remaining = max - (activity.value.participantCount || 0)
  return remaining > 0 ? remaining : 0
})

const registrationPercentage = computed(() => {
  if (!activity.value || !activity.value.maxParticipants) return 0
  const count = activity.value.participantCount || 0
  const max = activity.value.maxParticipants
  return Math.min(Math.round((count / max) * 100), 100)
})

const isRegistrationOpen = computed(() => {
  if (!activity.value) return false
  const now = new Date()
  const regStart = activity.value.registrationStartTime ? new Date(activity.value.registrationStartTime) : null
  const regEnd = activity.value.registrationEndTime ? new Date(activity.value.registrationEndTime) : null
  if (!regStart || !regEnd) return false
  return now >= regStart && now <= regEnd
})

const isFull = computed(() => {
  if (!activity.value || !activity.value.maxParticipants) return false
  return (activity.value.participantCount || 0) >= activity.value.maxParticipants
})

const canRegister = computed(() => {
  if (!activity.value) return false
  if (isRegistered.value || !isRegistrationOpen.value || activity.value.status === 'ended' || isFull.value) {
    return false
  }
  return true
})

const executionWindowText = computed(() => {
  if (!activity.value) return t('pages.activityDetail.pendingLocation')
  return `${formatDateTime(activity.value.startTime)} - ${formatDateTime(activity.value.endTime)}`
})

const registrationWindowText = computed(() => {
  if (!activity.value) return t('pages.activityDetail.pendingLocation')
  if (!activity.value.registrationStartTime || !activity.value.registrationEndTime) {
    return t('pages.activityDetail.registrationPending')
  }
  return `${formatDateTime(activity.value.registrationStartTime)} - ${formatDateTime(activity.value.registrationEndTime)}`
})

const contentHtml = computed(() => activity.value?.content || activity.value?.description || '')

const activitySummary = computed(() => {
  if (!activity.value) return t('pages.activityDetail.summaryFallback')
  return toPlainText(activity.value.description || activity.value.content) || t('pages.activityDetail.summaryFallback')
})

const heroMetaItems = computed(() => {
  if (!activity.value) return []
  return [
    `${t('pages.activityDetail.executionTime')}：${formatDateTime(activity.value.startTime)}`,
    `${t('pages.activityDetail.location')}：${activity.value.location || t('pages.activityDetail.pendingLocation')}`,
    `${t('pages.activityDetail.organizer')}：${activity.value.organizer || activity.value.creator || t('pages.activityDetail.emptyOrganizer')}`
  ]
})

const heroStats = computed(() => {
  if (!activity.value) return []
  return [
    {
      label: t('pages.activityDetail.participantCount'),
      value: String(activity.value.participantCount || 0),
      hint: t('pages.activityDetail.registeredCount')
    },
    {
      label: t('pages.activityDetail.remainingSlots'),
      value: String(remainingSlots.value),
      hint: t('pages.activityDetail.capacityLimit')
    },
    {
      label: t('pages.activityDetail.metricsTitle'),
      value: `${registrationPercentage.value}%`,
      hint: t('pages.activityDetail.capacityLimit')
    }
  ]
})

const heroBadges = computed(() => {
  if (!activity.value) return []
  const badges = [{ label: getStatusText(activity.value.status), tone: 'warm' }]
  if (activity.value.type) {
    badges.push({ label: formatActivityType(activity.value.type), tone: 'info' })
  }
  if (isRegistered.value) {
    badges.push({ label: t('pages.activityDetail.registeredCount'), tone: 'success' })
  }
  return badges
})

const ctaTitle = computed(() => {
  if (isRegistered.value) return t('pages.activityDetail.registeredTitle')
  if (activity.value?.status === 'ended') return t('pages.activityDetail.endedTitle')
  if (isFull.value) return t('pages.activityDetail.fullTitle')
  if (isRegistrationOpen.value) return t('pages.activityDetail.openTitle')
  return t('pages.activityDetail.closedTitle')
})

const ctaDescription = computed(() => {
  if (isRegistered.value) return t('pages.activityDetail.registeredDesc')
  if (activity.value?.status === 'ended') return t('pages.activityDetail.endedDesc')
  if (isFull.value) return t('pages.activityDetail.fullDesc')
  if (isRegistrationOpen.value) return t('pages.activityDetail.openDesc')
  return t('pages.activityDetail.closedDesc')
})

function getStatusText(status) {
  const map = {
    upcoming: t('pages.activityDetail.upcoming'),
    ongoing: t('pages.activityDetail.ongoing'),
    ended: t('pages.activityDetail.endedStatus')
  }
  return map[status] || String(status || '')
}

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
    requirements: item.requirements,
    attachmentUrl: item.attachmentUrl,
    coverImage: item.coverImage
  }
}

function formatDate(dateString) {
  if (!dateString) return t('pages.activityDetail.pendingLocation')
  const date = new Date(dateString)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${month}-${day}`
}

function formatDateTime(dateString) {
  if (!dateString) return t('pages.activityDetail.pendingLocation')
  const date = new Date(dateString)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

function formatActivityType(type) {
  const normalized = String(type || '').trim().toLowerCase()
  const typeMap = {
    1: t('pages.activityDetail.typeLecture'),
    2: t('pages.activityDetail.typeCompetition'),
    3: t('pages.activityDetail.typeTraining'),
    4: t('pages.activityDetail.typeCulture'),
    5: t('pages.activityDetail.typeVolunteer'),
    6: t('pages.activityDetail.typeOther'),
    lecture: t('pages.activityDetail.typeLecture'),
    competition: t('pages.activityDetail.typeCompetition'),
    training: t('pages.activityDetail.typeTraining'),
    culture: t('pages.activityDetail.typeCulture'),
    volunteer: t('pages.activityDetail.typeVolunteer'),
    other: t('pages.activityDetail.typeOther'),
    regular: t('pages.activityDetail.regularType'),
    normal: t('pages.activityDetail.regularType')
  }
  return typeMap[normalized] || t('pages.activityDetail.regularType')
}

function toPlainText(text) {
  return String(text || '').replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
}

async function fetchActivityDetail() {
  if (!activityId.value) {
    ElMessage.error(t('pages.activityDetail.invalidId'))
    return
  }

  loading.value = true
  try {
    const res = await getActivityDetail(activityId.value)
    if (res.data) {
      activity.value = transformActivity(res.data)
      checkRegistrationStatus()
      fetchRelatedActivities()
    } else {
      ElMessage.error(t('pages.activityDetail.missing'))
    }
  } catch (error) {
    console.error('failed to load activity detail:', error)
    ElMessage.error(t('pages.activityDetail.fetchFailed'))
  } finally {
    loading.value = false
  }
}

async function checkRegistrationStatus() {
  if (!userStore.isLoggedIn) {
    isRegistered.value = false
    return
  }
  try {
    const res = await checkRegistered(activityId.value)
    isRegistered.value = res.data?.registered || false
  } catch (error) {
    console.error('failed to check registration status:', error)
  }
}

async function fetchRelatedActivities() {
  try {
    const res = await getActivityList()
    if (Array.isArray(res.data)) {
      relatedActivities.value = res.data
        .map(transformActivity)
        .filter((item) => String(item.id) !== String(activityId.value))
        .slice(0, 4)
    }
  } catch (error) {
    console.error('failed to load related activities:', error)
  }
}

function handleBack() {
  router.push('/activities')
}

function handleActivityClick(id) {
  router.push(`/activities/${id}`)
}

async function handleRegister() {
  try {
    await ElMessageBox.confirm(
      t('pages.activityDetail.registerConfirmMessage', { title: activity.value.title }),
      t('pages.activityDetail.registerConfirmTitle'),
      {
        confirmButtonText: t('pages.activityDetail.confirmRegister'),
        cancelButtonText: t('common.cancel'),
        type: 'info'
      }
    )

    registering.value = true
    const res = await registerActivity(activityId.value)
    if (res.code === 200 || res.success) {
      ElMessage.success(t('pages.activityDetail.registerSuccess'))
      isRegistered.value = true
      activity.value.participantCount = (activity.value.participantCount || 0) + 1
    } else {
      ElMessage.error(res.msg || t('pages.activityDetail.registerFailed'))
    }
  } catch (error) {
    if (error !== 'cancel' && error?.message) {
      ElMessage.error(error.message)
    }
  } finally {
    registering.value = false
  }
}

async function handleCheckIn() {
  checkingIn.value = true
  try {
    const res = await checkIn(activityId.value)
    if (res.code === 200 || res.success) {
      isCheckedIn.value = true
      ElMessage.success(t('pages.activityDetail.checkInSuccess'))
    } else {
      ElMessage.error(res.msg || t('pages.activityDetail.checkInFailed'))
    }
  } catch {
    ElMessage.error(t('pages.activityDetail.checkInFailed'))
  } finally {
    checkingIn.value = false
  }
}

async function handleCancelRegister() {
  try {
    await ElMessageBox.confirm(
      t('pages.activityDetail.cancelConfirmMessage', { title: activity.value.title }),
      t('pages.activityDetail.cancelConfirmTitle'),
      {
        confirmButtonText: t('pages.activityDetail.confirmCancel'),
        cancelButtonText: t('pages.activityDetail.back'),
        type: 'warning'
      }
    )

    registering.value = true
    const res = await cancelRegistration(activityId.value)
    if (res.code === 200 || res.success) {
      ElMessage.success(t('pages.activityDetail.cancelSuccess'))
      isRegistered.value = false
      activity.value.participantCount = Math.max((activity.value.participantCount || 1) - 1, 0)
    } else {
      ElMessage.error(res.msg || t('pages.activityDetail.cancelFailed'))
    }
  } catch (error) {
    if (error !== 'cancel' && error?.message) {
      ElMessage.error(error.message)
    }
  } finally {
    registering.value = false
  }
}

watch(
  () => route.params.id,
  (newId) => {
    if (newId) {
      fetchActivityDetail()
    }
  }
)

onMounted(() => {
  fetchActivityDetail()
})
</script>

<style lang="scss" scoped>
@use '../styles/variables.scss' as *;

.activity-detail-page {
  min-height: 100vh;
  padding-bottom: 56px;
  background:
    linear-gradient(180deg, rgba($primary-dark, 0.04), rgba($bg-white, 0.86) 24%, $bg-white 100%),
    $bg-white;
}

.detail-flow {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.back-strip {
  padding-top: 18px;
}

.hero-action {
  border-radius: $border-radius-full;
}

.loading-state,
.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 320px;
}

.loading-state {
  flex-direction: column;
  gap: 14px;
  color: $text-secondary;
}

.loading-icon {
  font-size: 36px;
  color: $accent-color;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.cta-card {
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

.cta-card h2,
.cta-card p,
.cta-summary-item span,
.cta-summary-item strong,
.note-row span,
.note-row strong {
  margin: 0;
}

.cta-eyebrow {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.72);
}

.cta-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.cta-summary-item {
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.08);
}

.cta-summary-item span {
  display: block;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
}

.cta-summary-item strong {
  display: block;
  margin-top: 6px;
  color: $text-white;
}

.cta-notes {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.note-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.note-row span {
  color: rgba(255, 255, 255, 0.7);
}

.note-row strong {
  color: $text-white;
  text-align: right;
}

.cta-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.detail-shell {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.8fr);
  gap: 24px;
  padding-top: 28px;
}

.content-column,
.sidebar-column {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.surface-panel {
  padding: 28px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba($primary-color, 0.08);
  box-shadow: 0 22px 44px rgba(24, 33, 31, 0.08);
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 24px;
}

.overview-card {
  padding: 20px;
  border-radius: 20px;
  background: rgba($bg-light, 0.74);
}

.overview-card span,
.overview-card strong {
  display: block;
  margin: 0;
}

.overview-card span {
  font-size: 12px;
  color: $text-light;
}

.overview-card strong {
  margin-top: 8px;
  color: $text-primary;
}

.content-body {
  margin-top: 24px;
  color: $text-primary;
  line-height: 1.8;
}

.content-body :deep(p:first-child) {
  margin-top: 0;
}

.support-stack,
.metric-stack,
.related-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 24px;
}

.support-card {
  padding: 20px;
  border-radius: 20px;
  background: rgba($bg-light, 0.74);
}

.support-card h3,
.support-card p {
  margin: 0;
}

.support-card p {
  margin-top: 10px;
  color: $text-secondary;
  line-height: 1.75;
}

.attachment-link {
  display: inline-flex;
  align-items: center;
  min-height: 42px;
  padding: 0 16px;
  margin-top: 10px;
  border-radius: $border-radius-full;
  color: $text-white;
  background: $primary-color;
  text-decoration: none;
}

.fact-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 24px;
}

.fact-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 18px;
  border-radius: 18px;
  background: rgba($bg-light, 0.74);
}

.fact-row span {
  font-size: 12px;
  color: $text-light;
}

.fact-row strong {
  color: $text-primary;
}

.metric-card {
  padding: 18px;
  border-radius: 18px;
  background: rgba($bg-light, 0.74);
}

.metric-card span,
.metric-card strong {
  display: block;
}

.metric-card span {
  font-size: 12px;
  color: $text-light;
}

.metric-card strong {
  margin-top: 8px;
  color: $text-primary;
}

.related-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  padding: 18px;
  border: none;
  border-radius: 18px;
  text-align: left;
  background: rgba($bg-light, 0.74);
  cursor: pointer;
  transition: transform $transition-base ease, box-shadow $transition-base ease;
}

.related-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 28px rgba(24, 33, 31, 0.08);
}

.related-date {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: $accent-color;
}

.related-item strong {
  color: $text-primary;
}

.related-item span:last-child {
  color: $text-secondary;
}

@media (max-width: $breakpoint-lg) {
  .detail-shell,
  .overview-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: $breakpoint-md) {
  .surface-panel {
    padding: 22px;
    border-radius: 20px;
  }

  .cta-summary {
    grid-template-columns: 1fr;
  }

  .note-row {
    flex-direction: column;
  }
}
</style>
