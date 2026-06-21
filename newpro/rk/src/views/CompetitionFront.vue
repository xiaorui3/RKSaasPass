<template>
  <div class="competition-front-page design-competition-reference-page">
    <PortalEventHero
      class="design-trophy-hero"
      eyebrow="发现精彩赛事"
      title="比赛目录与公开报名计划"
      description="将竞赛活动重组为更清晰的比赛目录，突出项目级别、报名状态与参与规模，帮助成员快速锁定适合的报名窗口。"
      :meta-items="heroMetaItems"
      :stats="heroStats"
      :badges="heroBadges"
    >
      <template #actions>
        <router-link class="hero-link primary" to="/activities">查看活动项目</router-link>
      </template>

      <template #aside>
        <div class="hero-aside-card">
          <p class="aside-eyebrow">重点赛事</p>
          <template v-if="spotlightCompetition">
            <div class="aside-badges">
              <span class="aside-badge">{{ getStatusText(spotlightCompetition.status) }}</span>
              <span class="aside-badge neutral">{{ spotlightCompetition.level || '公开赛道' }}</span>
            </div>
            <h2>{{ spotlightCompetition.title }}</h2>
            <p>{{ getShortText(toPlainText(spotlightCompetition.description), 120) }}</p>

            <div class="aside-meta">
              <div class="aside-meta-item">
                <span>报名人数</span>
                <strong>{{ spotlightCompetition.registrationCount || 0 }}</strong>
              </div>
              <div class="aside-meta-item">
                <span>开始时间</span>
                <strong>{{ formatDate(spotlightCompetition.startTime) }}</strong>
              </div>
            </div>

            <el-button type="primary" @click="goToDetail(spotlightCompetition.id)">查看详情</el-button>
          </template>
          <el-empty v-else description="暂无重点赛事" :image-size="88" />
        </div>
      </template>
    </PortalEventHero>

    <main class="container competition-shell">
      <PortalEventFilterToolbar
        class="design-event-filter-pills"
        eyebrow="比赛筛选"
        title="筛选比赛级别与项目状态"
        description="保留原有状态切换和级别筛选逻辑，使用更清晰的目录式工具栏和结果概览强化公开入口。"
        :summary="filterSummary"
      >
        <template #actions>
          <el-button v-if="searchKeyword || filterStatus || filterLevel" @click="handleReset">重置筛选</el-button>
        </template>

        <div class="filter-layout">
          <label class="filter-field">
            <span class="field-label">赛事关键词</span>
            <el-input
              v-model.trim="searchKeyword"
              placeholder="搜索赛事标题、简介、主办方"
              clearable
              @input="handleFilterChange"
            />
          </label>

          <div class="filter-block">
            <span class="field-label">项目状态</span>
            <el-radio-group v-model="filterStatus" @change="handleFilterChange">
              <el-radio-button label="">全部赛事</el-radio-button>
              <el-radio-button label="UPCOMING">即将开始</el-radio-button>
              <el-radio-button label="ONGOING">进行中</el-radio-button>
              <el-radio-button label="COMPLETED">已结束</el-radio-button>
            </el-radio-group>
          </div>

          <label class="filter-field">
            <span class="field-label">赛事级别</span>
            <el-select v-model="filterLevel" placeholder="全部级别" clearable @change="handleFilterChange">
              <el-option label="全部级别" value="" />
              <el-option label="校级" value="校级" />
              <el-option label="市级" value="市级" />
              <el-option label="省级" value="省级" />
              <el-option label="国家级" value="国家级" />
            </el-select>
          </label>
        </div>
      </PortalEventFilterToolbar>

      <section v-if="spotlightCompetition" class="surface-panel spotlight-panel">
        <PortalEventSectionHeader
          eyebrow="重点赛事"
          title="重点赛事看板"
          description="将当前竞赛目录中的重点项目单独提炼，强化级别、状态和报名入口。"
        />

        <article class="spotlight-card" @click="goToDetail(spotlightCompetition.id)">
          <div class="spotlight-cover">
            <img
              v-if="spotlightCompetition.coverImage"
              :src="resolveMediaUrl(spotlightCompetition.coverImage)"
              :alt="spotlightCompetition.title"
            >
            <div v-else class="cover-placeholder">
              <el-icon><Trophy /></el-icon>
            </div>
          </div>

          <div class="spotlight-body">
            <div class="spotlight-tags">
              <el-tag :type="getStatusType(spotlightCompetition.status)" effect="dark">
                {{ getStatusText(spotlightCompetition.status) }}
              </el-tag>
              <el-tag type="warning" effect="plain">
                {{ spotlightCompetition.level || '公开赛道' }}
              </el-tag>
              <el-tag v-if="spotlightCompetition.competitionType" type="info" effect="plain">
                {{ spotlightCompetition.competitionType }}
              </el-tag>
            </div>

            <h2>{{ spotlightCompetition.title }}</h2>
            <p>{{ toPlainText(spotlightCompetition.description) || '暂无赛事简介' }}</p>

            <div class="spotlight-metrics">
              <div class="spotlight-metric">
                <span>开始时间</span>
                <strong>{{ formatDate(spotlightCompetition.startTime) }}</strong>
              </div>
              <div class="spotlight-metric">
                <span>报名人数</span>
                <strong>{{ spotlightCompetition.registrationCount || 0 }}</strong>
              </div>
              <div class="spotlight-metric">
                <span>浏览量</span>
                <strong>{{ spotlightCompetition.viewCount || 0 }}</strong>
              </div>
            </div>
          </div>

          <div class="spotlight-actions">
            <el-button type="primary" @click.stop="goToDetail(spotlightCompetition.id)">查看详情</el-button>
          </div>
        </article>
      </section>

      <section class="surface-panel list-panel">
        <PortalEventSectionHeader
          eyebrow="比赛目录"
          title="公开比赛列表"
          :description="listDescription"
        />

        <div v-if="loading" class="loading-state">
          <el-icon class="loading-icon"><Loading /></el-icon>
          <p>正在加载赛事项目...</p>
        </div>

        <div v-else-if="filteredCompetitions.length === 0" class="empty-state">
          <el-empty description="暂无赛事数据">
            <el-button type="primary" @click="handleReset">查看全部赛事</el-button>
          </el-empty>
        </div>

        <div v-else class="competition-grid design-competition-card-grid">
          <article
            v-for="competition in filteredCompetitions"
            :key="competition.id"
            class="competition-card"
            @click="goToDetail(competition.id)"
          >
            <div class="card-cover">
              <img v-if="competition.coverImage" :src="resolveMediaUrl(competition.coverImage)" :alt="competition.title">
              <div v-else class="cover-placeholder">
                <el-icon><Trophy /></el-icon>
              </div>

              <div class="cover-tags">
                <span class="cover-badge status">{{ getStatusText(competition.status) }}</span>
                <span v-if="competition.level" class="cover-badge level">{{ competition.level }}</span>
              </div>
            </div>

            <div class="card-body">
              <div class="card-header">
                <div>
                  <h3>{{ competition.title }}</h3>
                  <p>{{ toPlainText(competition.description) || '暂无赛事简介' }}</p>
                </div>
                <div class="card-stat">
                  <span>报名人数</span>
                  <strong>{{ competition.registrationCount || 0 }}</strong>
                </div>
              </div>

              <div class="card-facts">
                <div class="fact-item">
                  <el-icon><Calendar /></el-icon>
                  <span>{{ formatDate(competition.startTime) }}</span>
                </div>
                <div class="fact-item">
                  <el-icon><User /></el-icon>
                  <span>{{ competition.registrationCount || 0 }} 人参与</span>
                </div>
                <div class="fact-item">
                  <el-icon><View /></el-icon>
                  <span>{{ competition.viewCount || 0 }} 浏览</span>
                </div>
              </div>

              <div class="card-actions">
                <el-button type="primary" @click.stop="goToDetail(competition.id)">查看详情</el-button>
              </div>
            </div>
          </article>
        </div>
      </section>

      <section v-if="sharedCompetitions.length > 0" class="surface-panel shared-panel">
        <PortalEventSectionHeader
          eyebrow="共享入口"
          title="跨租户共享赛事"
          description="保留原有共享赛事列表，统一为同一套企业化赛事卡片框架。"
        />

        <div class="competition-grid">
          <article
            v-for="competition in sharedCompetitions"
            :key="`shared-${competition.id}`"
            class="competition-card shared-card"
            @click="goToDetail(competition.id)"
          >
            <div class="card-cover">
              <img v-if="competition.coverImage" :src="resolveMediaUrl(competition.coverImage)" :alt="competition.title">
              <div v-else class="cover-placeholder">
                <el-icon><Trophy /></el-icon>
              </div>

              <div class="cover-tags">
                <span class="cover-badge shared">跨租户</span>
                <span class="cover-badge status">{{ getStatusText(competition.status) }}</span>
              </div>
            </div>

            <div class="card-body">
              <div class="card-header">
                <div>
                  <h3>{{ competition.title }}</h3>
                  <p>{{ toPlainText(competition.description) || '暂无赛事简介' }}</p>
                </div>
                <div class="card-stat">
                  <span>报名人数</span>
                  <strong>{{ competition.registrationCount || 0 }}</strong>
                </div>
              </div>

              <div class="card-facts">
                <div class="fact-item">
                  <el-icon><Calendar /></el-icon>
                  <span>{{ formatDate(competition.startTime) }}</span>
                </div>
                <div class="fact-item">
                  <el-icon><User /></el-icon>
                  <span>{{ competition.registrationCount || 0 }} 人参与</span>
                </div>
                <div class="fact-item">
                  <el-icon><View /></el-icon>
                  <span>{{ competition.viewCount || 0 }} 浏览</span>
                </div>
              </div>

              <div class="card-actions">
                <el-button type="primary" @click.stop="goToDetail(competition.id)">查看详情</el-button>
              </div>
            </div>
          </article>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Calendar, Loading, Trophy, User, View } from '@element-plus/icons-vue'
import PortalEventFilterToolbar from '@/components/portal-events/PortalEventFilterToolbar.vue'
import PortalEventHero from '@/components/portal-events/PortalEventHero.vue'
import PortalEventSectionHeader from '@/components/portal-events/PortalEventSectionHeader.vue'
import { getPublishedCompetitions, getSharedCompetitions } from '@/api/competition'
import { resolveMediaUrl } from '@/utils/mediaUrl'

const router = useRouter()

const loading = ref(false)
const competitions = ref([])
const sharedCompetitions = ref([])
const searchKeyword = ref('')
const filterStatus = ref('')
const filterLevel = ref('')

const filteredCompetitions = computed(() => {
  let result = competitions.value

  if (searchKeyword.value) {
    result = result.filter(matchesKeyword)
  }

  if (filterStatus.value) {
    result = result.filter((item) => item.status === filterStatus.value)
  }

  if (filterLevel.value) {
    result = result.filter((item) => item.level === filterLevel.value)
  }

  return result
})

const spotlightCompetition = computed(() => {
  const visibleList = filteredCompetitions.value
  return visibleList.find((item) => item.isFeatured) || visibleList[0] || competitions.value[0] || null
})

const heroMetaItems = computed(() => [
  searchKeyword.value ? `关键词：${searchKeyword.value}` : '关键词：全部',
  filterStatus.value ? `状态：${getStatusText(filterStatus.value)}` : '状态：全部',
  filterLevel.value ? `级别：${filterLevel.value}` : '级别：全部',
  '保留原有公开赛事详情路由'
])

const heroStats = computed(() => [
  {
    label: '公开比赛',
    value: String(competitions.value.length),
    hint: '主比赛目录'
  },
  {
    label: '目录结果',
    value: String(filteredCompetitions.value.length),
    hint: '筛选后显示'
  },
  {
    label: '共享赛事',
    value: String(sharedCompetitions.value.length),
    hint: '跨租户开放'
  }
])

const heroBadges = computed(() => [
  { label: '比赛目录', tone: 'info' },
  { label: filterStatus.value ? getStatusText(filterStatus.value) : '开放访问', tone: 'warm' }
])

const filterSummary = computed(() => [
  {
    label: '目录结果',
    value: `${filteredCompetitions.value.length}`,
    hint: '当前列表规模'
  },
  {
    label: '进行中',
    value: `${filteredCompetitions.value.filter((item) => item.status === 'ONGOING').length}`,
    hint: '可持续关注项目'
  },
  {
    label: '共享入口',
    value: `${sharedCompetitions.value.length}`,
    hint: '单独保留共享区'
  }
])

const listDescription = computed(() => {
  const parts = ['保留原有比赛列表、状态筛选和详情跳转']
  if (searchKeyword.value) {
    parts.push(`当前关键词为“${searchKeyword.value}”`)
  }
  if (filterStatus.value) {
    parts.push(`当前状态为“${getStatusText(filterStatus.value)}”`)
  }
  if (filterLevel.value) {
    parts.push(`当前级别为“${filterLevel.value}”`)
  }
  return `${parts.join('，')}。`
})

function getStatusText(status) {
  const textMap = {
    UPCOMING: '即将开始',
    ONGOING: '进行中',
    COMPLETED: '已结束',
    CANCELLED: '已取消'
  }
  return textMap[status] || status
}

function getStatusType(status) {
  const typeMap = {
    UPCOMING: 'warning',
    ONGOING: 'success',
    COMPLETED: 'info',
    CANCELLED: 'danger'
  }
  return typeMap[status] || 'info'
}

function formatDate(dateStr) {
  if (!dateStr) return '待定'
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function getShortText(text, limit = 80) {
  if (!text) return '暂无赛事简介'
  return text.length > limit ? `${text.slice(0, limit)}...` : text
}

function toPlainText(text) {
  return String(text || '').replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
}

function matchesKeyword(item) {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) return true
  return [
    item.title,
    item.subtitle,
    item.description,
    item.content,
    item.organizer,
    item.level,
    item.competitionType
  ].some((value) => toPlainText(value).toLowerCase().includes(keyword))
}

function handleFilterChange() {
}

function handleReset() {
  searchKeyword.value = ''
  filterStatus.value = ''
  filterLevel.value = ''
}

function goToDetail(id) {
  router.push(`/competition/${id}`)
}

async function fetchCompetitions() {
  loading.value = true
  try {
    const res = await getPublishedCompetitions()
    competitions.value = res.data || []

    const sharedRes = await getSharedCompetitions()
    sharedCompetitions.value = sharedRes.data || []
  } catch (error) {
    console.error('获取比赛列表失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchCompetitions()
})
</script>

<style lang="scss" scoped>
@use '../styles/variables.scss' as *;

.competition-front-page {
  min-height: 100vh;
  padding-bottom: 56px;
  background:
    linear-gradient(180deg, rgba($primary-dark, 0.04), rgba($bg-white, 0.86) 24%, $bg-white 100%),
    $bg-white;
}

.competition-shell {
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

.hero-link.primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  padding: 0 18px;
  border-radius: $border-radius-full;
  color: $text-primary;
  background: $bg-white;
  font-weight: 600;
  text-decoration: none;
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

.aside-badges {
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

.filter-layout {
  display: grid;
  grid-template-columns: minmax(220px, 0.85fr) minmax(0, 1.6fr) minmax(240px, 0.7fr);
  gap: 18px;
  align-items: end;
}

.filter-block,
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
  grid-template-columns: minmax(240px, 0.9fr) minmax(0, 1.35fr) auto;
  gap: 24px;
  align-items: center;
  margin-top: 24px;
  padding: 24px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba($primary-color, 0.08);
  cursor: pointer;
}

.spotlight-cover,
.card-cover {
  position: relative;
  overflow: hidden;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba($primary-color, 0.18), rgba($accent-color, 0.18));
}

.spotlight-cover {
  min-height: 220px;
}

.spotlight-cover img,
.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  min-height: 220px;
  color: $text-white;
  font-size: 54px;
  background: linear-gradient(135deg, $primary-color, lighten($primary-color, 8%));
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
  background: rgba($bg-light, 0.82);
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

.competition-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
  margin-top: 24px;
}

.competition-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid rgba($primary-color, 0.08);
  box-shadow: 0 16px 36px rgba(24, 33, 31, 0.06);
  cursor: pointer;
  transition: transform $transition-base ease, box-shadow $transition-base ease;
}

.competition-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 40px rgba(24, 33, 31, 0.12);
}

.card-cover {
  min-height: 220px;
}

.cover-tags {
  position: absolute;
  top: 14px;
  left: 14px;
  right: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.cover-badge {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: $border-radius-full;
  font-size: 12px;
  font-weight: 700;
  color: $text-white;
}

.cover-badge.status {
  background: rgba(21, 38, 34, 0.72);
}

.cover-badge.level {
  background: rgba($accent-color, 0.88);
}

.cover-badge.shared {
  background: rgba($warning-color, 0.92);
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 22px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.card-header h3,
.card-header p,
.card-stat span,
.card-stat strong {
  margin: 0;
}

.card-header h3 {
  font-size: 20px;
  line-height: 1.35;
  color: $text-primary;
}

.card-header p {
  margin-top: 10px;
  color: $text-secondary;
  line-height: 1.7;
}

.card-stat {
  min-width: 108px;
  padding: 12px 14px;
  border-radius: 16px;
  text-align: right;
  background: rgba($bg-light, 0.78);
}

.card-stat span {
  display: block;
  font-size: 12px;
  color: $text-light;
}

.card-stat strong {
  display: block;
  margin-top: 8px;
  color: $text-primary;
}

.card-facts {
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

.card-actions {
  display: flex;
  justify-content: flex-start;
}

.shared-card {
  background:
    linear-gradient(180deg, rgba(255, 248, 241, 0.98), rgba(255, 255, 255, 0.96)),
    #ffffff;
}

@media (max-width: $breakpoint-lg) {
  .filter-layout,
  .spotlight-card,
  .competition-grid,
  .spotlight-metrics {
    grid-template-columns: 1fr;
  }

  .spotlight-actions {
    flex-direction: row;
    flex-wrap: wrap;
  }
}

@media (max-width: $breakpoint-md) {
  .competition-shell {
    gap: 22px;
    padding-top: 22px;
  }

  .surface-panel {
    padding: 22px;
    border-radius: 20px;
  }

  .aside-meta {
    grid-template-columns: 1fr;
  }

  .card-header {
    flex-direction: column;
  }

  .card-stat {
    width: 100%;
    text-align: left;
  }
}
</style>
