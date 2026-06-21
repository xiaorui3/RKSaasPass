<template>
  <div class="competition-detail-page design-competition-detail-page">
    <div v-if="loading" class="container loading-state">
      <el-icon class="loading-icon"><Loading /></el-icon>
      <p>{{ t('pages.competitionDetail.loading') }}</p>
    </div>

    <div v-else-if="competition" class="detail-flow">
      <div class="container back-strip">
        <el-button text @click="handleBack">
          <el-icon><ArrowLeft /></el-icon>
          {{ t('pages.competitionDetail.backToList') }}
        </el-button>
      </div>

      <PortalEventHero
        class="design-detail-hero-row"
        eyebrow="比赛详情"
        :title="competition.title"
        :description="competitionSummary"
        :meta-items="heroMetaItems"
        :stats="heroStats"
        :badges="heroBadges"
      >
        <template #actions>
          <el-button plain class="hero-action" @click="handleBack">
            {{ t('pages.competitionDetail.backToOverview') }}
          </el-button>
        </template>

        <template #aside>
          <div class="cta-card">
            <p class="cta-eyebrow">报名入口</p>
            <h2>{{ ctaTitle }}</h2>
            <p>{{ ctaDescription }}</p>

            <div class="cta-summary">
              <div class="cta-summary-item">
                <span>{{ t('pages.competitionDetail.registrationCount') }}</span>
                <strong>{{ competition.registrationCount || 0 }}</strong>
              </div>
              <div class="cta-summary-item">
                <span>{{ t('pages.competitionDetail.viewCount') }}</span>
                <strong>{{ competition.viewCount || 0 }}</strong>
              </div>
            </div>

            <div class="cta-notes">
              <div class="note-row">
                <span>{{ t('pages.competitionDetail.registrationWindow') }}</span>
                <strong>{{ registrationWindowText }}</strong>
              </div>
              <div class="note-row">
                <span>{{ t('pages.competitionDetail.competitionWindow') }}</span>
                <strong>{{ competitionWindowText }}</strong>
              </div>
            </div>

            <div class="cta-actions">
              <el-button v-if="canRegister" type="primary" @click="handleRegister">
                {{ t('pages.competitionDetail.registerNow') }}
              </el-button>
              <el-tag v-else-if="isRegistrationEnded" type="info">
                {{ t('pages.competitionDetail.registrationEnded') }}
              </el-tag>
              <el-tag v-else-if="isCompetitionFull" type="warning">
                {{ t('pages.competitionDetail.full') }}
              </el-tag>
              <el-tag v-else-if="competition.status === 'COMPLETED'" type="info">
                {{ t('pages.competitionDetail.completed') }}
              </el-tag>
              <el-tag v-else-if="competition.status === 'CANCELLED'" type="danger">
                {{ t('pages.competitionDetail.cancelled') }}
              </el-tag>
            </div>
          </div>
        </template>
      </PortalEventHero>

      <main class="container detail-shell">
        <section class="content-column">
          <section class="surface-panel design-content-card">
            <PortalEventSectionHeader
              :eyebrow="t('pages.competitionDetail.overviewEyebrow')"
              :title="t('pages.competitionDetail.overviewTitle')"
              :description="t('pages.competitionDetail.overviewDescription')"
            />

            <div class="overview-grid">
              <article class="overview-card">
                <span>{{ t('pages.competitionDetail.organizer') }}</span>
                <strong>{{ competition.organizer || t('common.noData') }}</strong>
              </article>
              <article class="overview-card">
                <span>{{ t('pages.competitionDetail.level') }}</span>
                <strong>{{ formatCompetitionLevel(competition.level) }}</strong>
              </article>
              <article class="overview-card">
                <span>{{ t('pages.competitionDetail.type') }}</span>
                <strong>{{ formatCompetitionType(competition.competitionType) }}</strong>
              </article>
              <article class="overview-card">
                <span>{{ t('pages.competitionDetail.executionLocation') }}</span>
                <strong>{{ competition.location || t('pages.competitionDetail.onlineEvent') }}</strong>
              </article>
            </div>
          </section>

          <section class="surface-panel">
            <PortalEventSectionHeader
              :eyebrow="t('pages.competitionDetail.contentEyebrow')"
              :title="t('pages.competitionDetail.contentTitle')"
              :description="t('pages.competitionDetail.contentDescription')"
            />

            <div class="content-stack">
              <article class="content-card">
                <h3>{{ t('pages.competitionDetail.intro') }}</h3>
                <p>{{ competitionSummary }}</p>
              </article>

              <article v-if="competition.content" class="content-card rich-card">
                <h3>{{ t('pages.competitionDetail.detailContent') }}</h3>
                <div class="content-body" v-html="competition.content"></div>
              </article>
            </div>
          </section>

          <section v-if="competition.awards || parsedTags.length || fileLinks.length" class="surface-panel">
            <PortalEventSectionHeader
              :eyebrow="t('pages.competitionDetail.supportEyebrow')"
              :title="t('pages.competitionDetail.supportTitle')"
              :description="t('pages.competitionDetail.supportDescription')"
            />

            <div class="support-stack">
              <article v-if="competition.awards" class="support-card">
                <h3>{{ t('pages.competitionDetail.awards') }}</h3>
                <p>{{ competition.awards }}</p>
              </article>

              <article v-if="parsedTags.length" class="support-card">
                <h3>{{ t('pages.competitionDetail.tags') }}</h3>
                <div class="tag-list">
                  <el-tag
                    v-for="tag in parsedTags"
                    :key="tag"
                    effect="plain"
                  >
                    {{ tag }}
                  </el-tag>
                </div>
              </article>

              <article v-if="fileLinks.length" class="support-card">
                <h3>{{ t('pages.competitionDetail.files') }}</h3>
                <div class="file-list">
                  <a
                    v-for="file in fileLinks"
                    :key="file.label"
                    :href="file.url"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="attachment-link"
                  >
                    {{ file.label }}
                  </a>
                </div>
              </article>
            </div>
          </section>
        </section>

        <aside class="sidebar-column design-info-sidebar">
          <section class="surface-panel">
            <PortalEventSectionHeader
              :eyebrow="t('pages.competitionDetail.factsEyebrow')"
              :title="t('pages.competitionDetail.factsTitle')"
              :description="t('pages.competitionDetail.factsDescription')"
              align="start"
            />

            <div class="fact-list">
              <div class="fact-row">
                <span>{{ t('pages.competitionDetail.registrationWindow') }}</span>
                <strong>{{ registrationWindowText }}</strong>
              </div>
              <div class="fact-row">
                <span>{{ t('pages.competitionDetail.competitionWindow') }}</span>
                <strong>{{ competitionWindowText }}</strong>
              </div>
              <div class="fact-row">
                <span>{{ t('pages.competitionDetail.onlineUrl') }}</span>
                <strong>{{ competition.onlineUrl || t('pages.competitionDetail.notConfigured') }}</strong>
              </div>
              <div class="fact-row">
                <span>{{ t('pages.competitionDetail.currentStatus') }}</span>
                <strong>{{ getStatusText(competition.status) }}</strong>
              </div>
            </div>
          </section>

          <section class="surface-panel">
            <PortalEventSectionHeader
              :eyebrow="t('pages.competitionDetail.metricsEyebrow')"
              :title="t('pages.competitionDetail.metricsTitle')"
              :description="t('pages.competitionDetail.metricsDescription')"
              align="start"
            />

            <div class="metric-stack">
              <article class="metric-card">
                <span>{{ t('pages.competitionDetail.viewCount') }}</span>
                <strong>{{ competition.viewCount || 0 }}</strong>
              </article>
              <article class="metric-card">
                <span>{{ t('pages.competitionDetail.registrationCount') }}</span>
                <strong>{{ competition.registrationCount || 0 }}</strong>
              </article>
              <article class="metric-card">
                <span>{{ t('pages.competitionDetail.maxParticipants') }}</span>
                <strong>{{ competition.maxParticipants || t('common.noData') }}</strong>
              </article>
            </div>
          </section>

          <section
            v-if="competition.contactPerson || competition.contactPhone || competition.contactEmail"
            class="surface-panel"
          >
            <PortalEventSectionHeader
              :eyebrow="t('pages.competitionDetail.contactEyebrow')"
              :title="t('pages.competitionDetail.contactTitle')"
              :description="t('pages.competitionDetail.contactDescription')"
              align="start"
            />

            <div class="fact-list">
              <div v-if="competition.contactPerson" class="fact-row">
                <span>{{ t('pages.competitionDetail.contactPerson') }}</span>
                <strong>{{ competition.contactPerson }}</strong>
              </div>
              <div v-if="competition.contactPhone" class="fact-row">
                <span>{{ t('pages.competitionDetail.contactPhone') }}</span>
                <strong>{{ competition.contactPhone }}</strong>
              </div>
              <div v-if="competition.contactEmail" class="fact-row">
                <span>{{ t('pages.competitionDetail.contactEmail') }}</span>
                <strong>{{ competition.contactEmail }}</strong>
              </div>
            </div>
          </section>
        </aside>
      </main>
    </div>

    <div v-else class="container empty-state">
      <el-empty :description="t('pages.competitionDetail.emptyState')">
        <el-button type="primary" @click="handleBack">{{ t('pages.competitionDetail.backToList') }}</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Loading } from '@element-plus/icons-vue'
import PortalEventHero from '@/components/portal-events/PortalEventHero.vue'
import PortalEventSectionHeader from '@/components/portal-events/PortalEventSectionHeader.vue'
import { getCompetitionDetail } from '@/api/competition'
import { resolveMediaUrl } from '@/utils/mediaUrl'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const competition = ref(null)

const competitionId = computed(() => route.params.id)

const isRegistrationOpen = computed(() => {
  if (!competition.value) return false
  const now = new Date()
  const regStart = competition.value.registrationStart ? new Date(competition.value.registrationStart) : null
  const regEnd = competition.value.registrationEnd ? new Date(competition.value.registrationEnd) : null

  if (!regStart || !regEnd) return false
  return now >= regStart && now <= regEnd
})

const isRegistrationEnded = computed(() => {
  if (!competition.value) return false
  const now = new Date()
  const regEnd = competition.value.registrationEnd ? new Date(competition.value.registrationEnd) : null
  return Boolean(regEnd && now > regEnd)
})

const isCompetitionFull = computed(() => {
  if (!competition.value) return false
  const maxParticipants = competition.value.maxParticipants
  const currentCount = competition.value.registrationCount || 0
  return Boolean(maxParticipants && currentCount >= maxParticipants)
})

const canRegister = computed(() => {
  if (!competition.value) return false
  return isRegistrationOpen.value &&
    !isCompetitionFull.value &&
    competition.value.status !== 'COMPLETED' &&
    competition.value.status !== 'CANCELLED' &&
    competition.value.isPublished
})

const registrationWindowText = computed(() => {
  if (!competition.value?.registrationStart || !competition.value?.registrationEnd) {
    return t('pages.competitionDetail.registrationPending')
  }
  return `${formatDateTime(competition.value.registrationStart)} - ${formatDateTime(competition.value.registrationEnd)}`
})

const competitionWindowText = computed(() => {
  if (!competition.value?.competitionStart || !competition.value?.competitionEnd) {
    return t('pages.competitionDetail.registrationPending')
  }
  return `${formatDateTime(competition.value.competitionStart)} - ${formatDateTime(competition.value.competitionEnd)}`
})

const parsedTags = computed(() => parseTags(competition.value?.tags))

const fileLinks = computed(() => {
  if (!competition.value) return []
  return [
    {
      label: t('pages.competitionDetail.rulesFile'),
      url: resolveMediaUrl(competition.value.rulesFile)
    },
    {
      label: t('pages.competitionDetail.materialsFile'),
      url: resolveMediaUrl(competition.value.materialsFile)
    },
    {
      label: t('pages.competitionDetail.resultsFile'),
      url: resolveMediaUrl(competition.value.resultsFile)
    }
  ].filter((item) => item.url)
})

const competitionSummary = computed(() => {
  if (!competition.value) return t('pages.competitionDetail.summaryFallback')
  return toPlainText(competition.value.subtitle || competition.value.description || competition.value.content) ||
    t('pages.competitionDetail.summaryFallback')
})

const heroMetaItems = computed(() => {
  if (!competition.value) return []
  return [
    `${t('pages.competitionDetail.organizer')}: ${competition.value.organizer || t('common.noData')}`,
    `${t('pages.competitionDetail.competitionWindow')}: ${formatDateTime(competition.value.competitionStart)}`,
    `${t('pages.competitionDetail.executionLocation')}: ${competition.value.location || t('pages.competitionDetail.onlineEvent')}`
  ]
})

const heroStats = computed(() => {
  if (!competition.value) return []
  return [
    {
      label: t('pages.competitionDetail.viewCount'),
      value: String(competition.value.viewCount || 0),
      hint: t('pages.competitionDetail.metricsTitle')
    },
    {
      label: t('pages.competitionDetail.registrationCount'),
      value: String(competition.value.registrationCount || 0),
      hint: t('pages.competitionDetail.ctaEyebrow')
    },
    {
      label: t('pages.competitionDetail.maxParticipants'),
      value: String(competition.value.maxParticipants || t('common.noData')),
      hint: t('pages.competitionDetail.metricsDescription')
    }
  ]
})

const heroBadges = computed(() => {
  if (!competition.value) return []
  const badges = [{ label: getStatusText(competition.value.status), tone: 'warm' }]
  if (competition.value.level) {
    badges.push({ label: formatCompetitionLevel(competition.value.level), tone: 'info' })
  }
  if (competition.value.competitionType) {
    badges.push({ label: formatCompetitionType(competition.value.competitionType), tone: 'info' })
  }
  if (competition.value.isFeatured) {
    badges.push({ label: t('pages.competitionDetail.recommended'), tone: 'success' })
  }
  return badges
})

const ctaTitle = computed(() => {
  if (canRegister.value) return t('pages.competitionDetail.registrationOpenTitle')
  if (competition.value?.status === 'COMPLETED') return t('pages.competitionDetail.completedTitle')
  if (competition.value?.status === 'CANCELLED') return t('pages.competitionDetail.cancelledTitle')
  if (isCompetitionFull.value) return t('pages.competitionDetail.fullTitle')
  if (isRegistrationEnded.value) return t('pages.competitionDetail.closedTitle')
  return t('pages.competitionDetail.waitingTitle')
})

const ctaDescription = computed(() => {
  if (canRegister.value) return t('pages.competitionDetail.registrationOpenDesc')
  if (competition.value?.status === 'COMPLETED') return t('pages.competitionDetail.completedDesc')
  if (competition.value?.status === 'CANCELLED') return t('pages.competitionDetail.cancelledDesc')
  if (isCompetitionFull.value) return t('pages.competitionDetail.fullDesc')
  if (isRegistrationEnded.value) return t('pages.competitionDetail.closedDesc')
  return t('pages.competitionDetail.waitingDesc')
})

function getStatusText(status) {
  const statusMap = {
    DRAFT: t('pages.competitionDetail.draftStatus'),
    REGISTRATION: t('pages.competitionDetail.registrationStatus'),
    PUBLISHED: t('pages.competitionDetail.publishedStatus'),
    UPCOMING: t('pages.competitionDetail.upcoming'),
    ONGOING: t('pages.competitionDetail.ongoing'),
    COMPLETED: t('pages.competitionDetail.completedStatus'),
    CANCELLED: t('pages.competitionDetail.cancelledStatus')
  }
  return statusMap[status] || String(status || '')
}

function normalizeCode(value) {
  return String(value || '').trim().toLowerCase()
}

function formatCompetitionLevel(level) {
  const levelMap = {
    school: t('pages.competitionDetail.levelSchool'),
    college: t('pages.competitionDetail.levelCollege'),
    university: t('pages.competitionDetail.levelSchool'),
    city: t('pages.competitionDetail.levelCity'),
    provincial: t('pages.competitionDetail.levelProvincial'),
    province: t('pages.competitionDetail.levelProvincial'),
    national: t('pages.competitionDetail.levelNational'),
    international: t('pages.competitionDetail.levelInternational')
  }
  return levelMap[normalizeCode(level)] || level || t('common.noData')
}

function formatCompetitionType(type) {
  const typeMap = {
    academic: t('pages.competitionDetail.typeAcademic'),
    innovation: t('pages.competitionDetail.typeInnovation'),
    sports: t('pages.competitionDetail.typeSports'),
    art: t('pages.competitionDetail.typeArt'),
    skill: t('pages.competitionDetail.typeSkill'),
    coding: t('pages.competitionDetail.typeCoding'),
    design: t('pages.competitionDetail.typeDesign')
  }
  return typeMap[normalizeCode(type)] || type || t('common.noData')
}

function formatDateTime(dateString) {
  if (!dateString) return t('pages.competitionDetail.registrationPending')
  const date = new Date(dateString)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

function parseTags(tagsValue) {
  if (!tagsValue) return []
  try {
    const parsed = JSON.parse(tagsValue)
    return Array.isArray(parsed) ? parsed : [parsed]
  } catch {
    return String(tagsValue).split(',').map((tag) => tag.trim()).filter(Boolean)
  }
}

function toPlainText(text) {
  return String(text || '').replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
}

async function fetchCompetitionDetail() {
  if (!competitionId.value) {
    ElMessage.error(t('pages.competitionDetail.invalidId'))
    return
  }

  loading.value = true
  try {
    const res = await getCompetitionDetail(competitionId.value)
    if (res.data) {
      competition.value = res.data
    } else {
      ElMessage.error(t('pages.competitionDetail.missing'))
    }
  } catch (error) {
    console.error('failed to load competition detail:', error)
    ElMessage.error(t('pages.competitionDetail.fetchFailed'))
  } finally {
    loading.value = false
  }
}

function handleBack() {
  router.push('/competition')
}

function handleRegister() {
  router.push(`/competition/register/${competitionId.value}`)
}

watch(
  () => route.params.id,
  (newId) => {
    if (newId) {
      fetchCompetitionDetail()
    }
  }
)

onMounted(() => {
  fetchCompetitionDetail()
})
</script>

<style lang="scss" scoped>
@use '../styles/variables.scss' as *;

.competition-detail-page {
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

.overview-card,
.content-card,
.support-card,
.fact-row,
.metric-card {
  padding: 20px;
  border-radius: 20px;
  background: rgba($bg-light, 0.74);
}

.overview-card span,
.overview-card strong,
.metric-card span,
.metric-card strong {
  display: block;
}

.overview-card span,
.metric-card span {
  font-size: 12px;
  color: $text-light;
}

.overview-card strong,
.metric-card strong {
  margin-top: 8px;
  color: $text-primary;
}

.content-stack,
.support-stack,
.metric-stack,
.fact-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 24px;
}

.content-card h3,
.content-card p,
.support-card h3,
.support-card p {
  margin: 0;
}

.content-card p,
.support-card p {
  margin-top: 10px;
  color: $text-secondary;
  line-height: 1.75;
}

.content-body {
  margin-top: 10px;
  color: $text-primary;
  line-height: 1.8;
}

.content-body :deep(p:first-child) {
  margin-top: 0;
}

.tag-list,
.file-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.attachment-link {
  display: inline-flex;
  align-items: center;
  min-height: 42px;
  padding: 0 16px;
  border-radius: $border-radius-full;
  color: $text-white;
  background: $primary-color;
  text-decoration: none;
}

.fact-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.fact-row span {
  font-size: 12px;
  color: $text-light;
}

.fact-row strong {
  color: $text-primary;
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
