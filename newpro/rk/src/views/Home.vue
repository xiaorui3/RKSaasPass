<template>
  <div class="portal-dashboard-grid">
    <div class="portal-dashboard-main">
      <section class="home-hero-banner" aria-label="首页横幅">
        <div class="home-hero-copy">
          <span>高校多租户社团平台</span>
          <h1>你好，{{ displayName }}！</h1>
          <p>探索多彩社团生活，遇见更好的自己</p>
          <div class="home-hero-actions">
            <router-link to="/activities">浏览活动</router-link>
            <router-link to="/join">加入社团</router-link>
          </div>
        </div>
      </section>

      <section class="home-panel quick-entry-panel">
        <div class="panel-title-row">
          <h2>快捷入口</h2>
        </div>
        <div class="quick-entry-list">
          <router-link
            v-for="entry in dashboardEntryModules"
            :key="entry.key"
            :to="entry.route"
            class="quick-entry-item"
            :class="entry.className"
          >
            <span class="quick-entry-icon">{{ entry.iconText }}</span>
            <strong>{{ entry.title }}</strong>
            <small>{{ entry.description }}</small>
          </router-link>
        </div>
      </section>

      <section class="home-panel recommendation-panel">
        <div class="panel-title-row">
          <h2>推荐社团</h2>
          <router-link to="/about">查看更多</router-link>
        </div>

        <div v-if="homeLoading" class="home-section-loading">
          <el-icon class="section-loading-icon"><Loading /></el-icon>
          <p>正在加载推荐内容...</p>
        </div>

        <div v-else-if="recommendedClubs.length" class="club-recommendation-grid">
          <article
            v-for="club in visibleRecommendedClubs"
            :key="club.id || club.name"
            class="club-card"
          >
            <div class="club-cover" :style="{ backgroundImage: `url(${club.cover})` }">
              <img :src="club.logo" :alt="club.name" class="club-logo">
            </div>
            <div class="club-body">
              <div class="club-title-row">
                <h3>{{ club.name }}</h3>
                <span>{{ club.category }}</span>
              </div>
              <p>{{ club.description }}</p>
              <div class="club-footer">
                <span>{{ club.members }} 人</span>
                <router-link :to="{ path: '/join', query: club.id ? { tenantId: club.id } : {} }">关注</router-link>
              </div>
            </div>
          </article>
        </div>

        <el-empty v-else description="暂无推荐社团" :image-size="96" />

        <div v-if="recommendedClubPages.length > 1" class="carousel-dots" aria-label="切换推荐社团">
          <button
            v-for="(_, index) in recommendedClubPages"
            :key="index"
            type="button"
            :class="{ active: index === activeRecommendedClubPage }"
            :aria-label="`第 ${index + 1} 页推荐社团`"
            @click="setRecommendedClubPage(index)"
          ></button>
        </div>
      </section>

      <section class="home-panel my-club-panel">
        <div class="panel-title-row">
          <h2>我的社团</h2>
          <router-link to="/profile">管理的社团</router-link>
        </div>
        <div class="my-club-card">
          <img :src="tenantLogo" alt="当前社团">
          <div>
            <h3>{{ currentClubName }}</h3>
            <p>{{ currentClubDescription }}</p>
          </div>
          <div class="my-club-stats">
            <span>
              <el-icon v-if="statsLoading" class="stat-loading"><Loading /></el-icon>
              <strong v-else>{{ stats.activities ?? '--' }}</strong>
              活动
            </span>
            <span>
              <el-icon v-if="statsLoading" class="stat-loading"><Loading /></el-icon>
              <strong v-else>{{ stats.works ?? '--' }}</strong>
              作品
            </span>
            <span>
              <el-icon v-if="statsLoading" class="stat-loading"><Loading /></el-icon>
              <strong v-else>{{ stats.members ?? '--' }}</strong>
              成员
            </span>
            <span>
              <el-icon v-if="statsLoading" class="stat-loading"><Loading /></el-icon>
              <strong v-else>{{ stats.news ?? '--' }}</strong>
              动态
            </span>
          </div>
          <router-link to="/profile">进入社团</router-link>
        </div>
      </section>
    </div>

    <aside class="portal-dashboard-aside">
      <section class="home-today-card">
        <div class="panel-title-row">
          <h2>今日活动</h2>
          <router-link to="/activities">查看更多</router-link>
        </div>
        <div v-if="homeLoading" class="home-section-loading compact">
          <el-icon class="section-loading-icon"><Loading /></el-icon>
        </div>
        <div v-else-if="todayActivities.length" class="today-activity-list">
          <router-link
            v-for="activity in todayActivities"
            :key="activity.id || activity.title"
            class="today-activity-item"
            :to="activity.id ? `/activities/${activity.id}` : '/activities'"
          >
            <span :class="activity.colorClass"></span>
            <div>
              <strong>{{ activity.time }}</strong>
              <small>{{ activity.duration }}</small>
            </div>
            <div>
              <b>{{ activity.title }}</b>
              <em>{{ activity.location }}</em>
            </div>
          </router-link>
        </div>
        <el-empty v-else description="暂无活动" :image-size="72" />
      </section>

      <section class="home-notice-card">
        <div class="panel-title-row">
          <h2>社团公告</h2>
          <router-link to="/notices">查看更多</router-link>
        </div>
        <div v-if="homeLoading" class="home-section-loading compact">
          <el-icon class="section-loading-icon"><Loading /></el-icon>
        </div>
        <div v-else-if="noticeList.length" class="notice-list">
          <router-link
            v-for="notice in noticeList.slice(0, 5)"
            :key="notice.id || notice.title"
            to="/notices"
          >
            <span></span>
            <strong>{{ notice.title }}</strong>
            <time>{{ notice.dateText }}</time>
          </router-link>
        </div>
        <el-empty v-else description="暂无公告" :image-size="72" />
      </section>

      <section class="home-side-card profile-summary-card">
        <div class="panel-title-row">
          <h2>个人中心</h2>
          <router-link to="/profile">></router-link>
        </div>
        <div class="profile-stat-grid">
          <router-link v-for="item in profileStats" :key="item.label" :to="item.path">
            <span>{{ item.icon }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.label }}</small>
          </router-link>
        </div>
      </section>
    </aside>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { getSystemConfig } from '@/api/common'
import { getNewsList } from '@/api/news'
import { getPublishedNoticeList } from '@/api/notice'
import { getFeaturedWorks } from '@/api/works'
import { getActivityPage } from '@/api/activity'
import { getMembersList } from '@/api/member'
import { getMyCreditSummary } from '@/api/credit'
import { useUserStore } from '@/stores/user'
import { cleanDisplayText, resolveCurrentTenantBranding } from '@/utils/tenantBranding'
import {
  PORTAL_HOME_LAYOUT_CONFIG_KEY,
  normalizePortalHomeLayoutConfig,
  orderedVisibleItems
} from '@/utils/portalLayoutConfig'

const homeLoading = ref(true)
const statsLoading = ref(true)
const stats = ref({ activities: null, news: null, members: null, works: null, volunteerHours: null })
const recentActivities = ref([])
const newsList = ref([])
const noticeList = ref([])
const activeRecommendedClubPage = ref(0)
const recommendedClubPageSize = 4
const userStore = useUserStore()
const portalLayoutConfig = ref(normalizePortalHomeLayoutConfig())

const visualAssets = [
  '/assets/visuals/rk-content-news.png',
  '/assets/visuals/rk-activity-action.png',
  '/assets/visuals/rk-competition-arena.png',
  '/assets/visuals/rk-mobile-services.png'
]

const defaultClubLogos = [
  '/assets/visuals/rk-admin-ops.png',
  '/assets/visuals/rk-campus-hero.png',
  '/assets/visuals/rk-admin-content-ops.png',
  '/assets/visuals/rk-activity-action.png'
]

const orderedEntryModules = computed(() => orderedVisibleItems(portalLayoutConfig.value.entryModules))
const displayName = computed(() => userStore.userName || '同学')
const currentTenantBranding = computed(() => resolveCurrentTenantBranding({
  tenantId: userStore.tenantId,
  tenantDirectory: userStore.tenantDirectory
}))
const tenantLogo = computed(() => currentTenantBranding.value.logoUrl || defaultClubLogos[0])
const currentClubName = computed(() => currentTenantBranding.value.tenantName || '当前社团')
const currentClubDescription = computed(() => {
  const tenant = (userStore.tenantDirectory || []).find((item) => String(item?.id) === String(userStore.tenantId))
  return cleanDisplayText(tenant?.description || tenant?.remark || tenant?.introduction, '登录后展示当前社团信息')
})

const dashboardEntryModules = computed(() => {
  const fallback = [
    { key: 'clubs', title: '浏览社团', description: '发现更多社团', route: '/about', iconText: '社' },
    { key: 'calendar', title: '活动日历', description: '查看活动安排', route: '/calendar', iconText: '历' },
    { key: 'create', title: '创建活动', description: '发起社团活动', route: '/activities', iconText: '创' },
    { key: 'apply', title: '我的申请', description: '查看申请进度', route: '/join', iconText: '申' },
    { key: 'checkin', title: '社团签到', description: '活动扫码签到', route: '/notifications', iconText: '签' }
  ]
  const configured = orderedEntryModules.value.slice(0, 5).map((entry, index) => ({
    ...entry,
    iconText: String(entry.icon || entry.title || fallback[index]?.iconText || '入').slice(0, 2)
  }))
  return configured.length ? configured : fallback
})

const tenantDirectoryClubs = computed(() => {
  const directory = Array.isArray(userStore.tenantDirectory) ? userStore.tenantDirectory : []
  return directory
    .map((tenant, index) => {
      const name = cleanDisplayText(tenant?.tenantName || tenant?.name || tenant?.clubName, '')
      if (!name) {
        return null
      }
      const isCurrentTenant = String(tenant?.id) === String(userStore.tenantId || '')
      const members = isCurrentTenant
        ? (stats.value.members ?? tenant?.memberCount ?? tenant?.members ?? '--')
        : (tenant?.memberCount ?? tenant?.members ?? '--')
      return {
        id: tenant?.id,
        name,
        category: cleanDisplayText(
          tenant?.category || tenant?.categoryName || tenant?.typeName || tenant?.tenantTypeName || tenant?.schoolName,
          '社团租户'
        ),
        description: cleanDisplayText(
          tenant?.description || tenant?.remark || tenant?.introduction || tenant?.tenantDesc,
          '点击查看社团详情'
        ),
        members,
        cover: visualAssets[index % visualAssets.length],
        logo: tenant?.resolvedLogoUrl || tenant?.logoUrl || defaultClubLogos[index % defaultClubLogos.length]
      }
    })
    .filter(Boolean)
})

const recommendedClubs = computed(() => tenantDirectoryClubs.value.map((club, index) => ({
  ...club,
  cover: visualAssets[index % visualAssets.length],
  logo: club.logo || defaultClubLogos[index % defaultClubLogos.length]
})))

const recommendedClubPages = computed(() => {
  const pages = []
  for (let index = 0; index < recommendedClubs.value.length; index += recommendedClubPageSize) {
    pages.push(recommendedClubs.value.slice(index, index + recommendedClubPageSize))
  }
  return pages
})

const visibleRecommendedClubs = computed(() => {
  if (!recommendedClubPages.value.length) {
    return []
  }
  const maxPage = recommendedClubPages.value.length - 1
  const page = Math.min(activeRecommendedClubPage.value, maxPage)
  return recommendedClubPages.value[page] || []
})

function setRecommendedClubPage(index) {
  if (index >= 0 && index < recommendedClubPages.value.length) {
    activeRecommendedClubPage.value = index
  }
}

const todayActivities = computed(() => {
  const colors = ['blue', 'green', 'orange', 'purple']
  return recentActivities.value.slice(0, 3).map((item, index) => {
    const start = item.startTime ? new Date(item.startTime) : null
    const end = item.endTime ? new Date(item.endTime) : null
    const time = start && !Number.isNaN(start.getTime())
      ? `${String(start.getHours()).padStart(2, '0')}:${String(start.getMinutes()).padStart(2, '0')}`
      : '待定'
    const duration = end && !Number.isNaN(end.getTime())
      ? `- ${String(end.getHours()).padStart(2, '0')}:${String(end.getMinutes()).padStart(2, '0')}`
      : item.startTime?.substring(0, 10) || ''
    return {
      id: item.id,
      title: item.title || item.name || '社团活动',
      time,
      duration,
      location: item.location || item.address || '校内场地',
      colorClass: colors[index % colors.length]
    }
  })
})

const profileStats = computed(() => [
  { icon: '星', label: '我的积分', value: stats.value.activities ?? '--', path: '/credit' },
  { icon: '誉', label: '我的荣誉', value: stats.value.works ?? '--', path: '/works' },
  { icon: '参', label: '参与活动', value: stats.value.activities ?? '--', path: '/activities' },
  { icon: '时', label: '志愿时长', value: formatVolunteerHours(stats.value.volunteerHours), path: '/credit' }
])

const formatVolunteerHours = (value) => {
  if (value === null || value === undefined || value === '') {
    return '--'
  }
  const number = Number(value)
  if (!Number.isFinite(number)) {
    return `${value}h`
  }
  return `${Number.isInteger(number) ? number : number.toFixed(1)}h`
}

const formatShortDate = (value) => {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return String(value).slice(5, 10)
  }
  return `${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

onMounted(async () => {
  try {
    const tenantDirectoryPromise = userStore.tenantDirectory?.length
      ? Promise.resolve(userStore.tenantDirectory)
      : userStore.fetchTenantDirectory().catch(() => [])
    const [, systemConfigRes, newsRes, noticeRes, actRes, membersRes, worksRes, creditSummaryRes] = await Promise.all([
      tenantDirectoryPromise,
      userStore.isLoggedIn ? getSystemConfig().catch(() => ({})) : Promise.resolve({}),
      getNewsList({ page: 1, pageSize: 8 }).catch(() => ({})),
      getPublishedNoticeList().catch(() => ({})),
      getActivityPage({ page: 1, size: 6 }).catch(() => ({})),
      userStore.isLoggedIn ? getMembersList().catch(() => ({})) : Promise.resolve({}),
      getFeaturedWorks({ page: 1, pageSize: 1 }).catch(() => ({})),
      userStore.isLoggedIn ? getMyCreditSummary().catch(() => ({})) : Promise.resolve({})
    ])

    if (systemConfigRes.code === 200 && systemConfigRes.data) {
      portalLayoutConfig.value = normalizePortalHomeLayoutConfig(systemConfigRes.data[PORTAL_HOME_LAYOUT_CONFIG_KEY])
    }

    if (newsRes.code === 200 || newsRes.data) {
      const records = newsRes.data?.records || newsRes.data || []
      newsList.value = (Array.isArray(records) ? records : []).map(item => ({
        ...item,
        date: item.createTime || item.date
      }))
      stats.value.news = newsRes.data?.total || newsList.value.length
    }

    if (noticeRes.code === 200 && Array.isArray(noticeRes.data)) {
      noticeList.value = noticeRes.data.map(item => ({
        id: item.id,
        title: item.title,
        date: item.publishTime || item.createTime,
        dateText: formatShortDate(item.publishTime || item.createTime)
      }))
    }

    if (actRes.code === 200 || actRes.data) {
      const records = actRes.data?.records || []
      recentActivities.value = Array.isArray(records) ? records : []
      stats.value.activities = actRes.data?.total || recentActivities.value.length
    }

    if (membersRes.code === 200 || membersRes.data) {
      const list = membersRes.data?.records || membersRes.data || []
      stats.value.members = Array.isArray(list) ? list.length : (membersRes.data?.total || 0)
    }

    if (worksRes.code === 200 || worksRes.data) {
      stats.value.works = worksRes.data?.total || 0
    }

    if (creditSummaryRes.code === 200 || creditSummaryRes.data) {
      stats.value.volunteerHours = creditSummaryRes.data?.volunteerHours ?? 0
    }
  } catch (e) {
    console.error('获取首页数据失败:', e)
  } finally {
    homeLoading.value = false
    statsLoading.value = false
  }
})
</script>

<style lang="scss" scoped>
.portal-dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 474px;
  gap: 24px;
  align-items: start;
}

.portal-dashboard-main,
.portal-dashboard-aside {
  display: grid;
  gap: 24px;
}

.home-panel,
.home-today-card,
.home-notice-card,
.profile-summary-card {
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 18px 42px rgba(43, 102, 179, 0.08);
}

.home-hero-banner {
  min-height: 224px;
  display: flex;
  align-items: center;
  padding: 34px 52px;
  border-radius: 8px;
  background-image:
    linear-gradient(90deg, rgba(28, 122, 255, 0.94) 0%, rgba(58, 148, 255, 0.7) 42%, rgba(76, 152, 255, 0.2) 100%),
    url('/assets/visuals/rk-campus-hero.png');
  background-size: cover;
  background-position: center right;
  color: #fff;
  overflow: hidden;
}

.home-hero-copy {
  display: grid;
  gap: 10px;

  span {
    width: max-content;
    padding: 6px 14px;
    border-radius: 8px;
    background: rgba(255, 255, 255, 0.16);
    font-size: 13px;
    font-weight: 800;
  }

  h1 {
    margin: 0;
    font-size: 36px;
    font-weight: 900;
    line-height: 1.2;
  }

  p {
    margin: 0;
    font-size: 20px;
    font-weight: 700;
    color: rgba(255, 255, 255, 0.86);
  }
}

.home-hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 12px;

  a {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 96px;
    height: 38px;
    padding: 0 18px;
    border-radius: 8px;
    background: #fff;
    color: #1473ff;
    font-weight: 900;
    text-decoration: none;

    + a {
      color: #fff;
      background: rgba(255, 255, 255, 0.2);
    }
  }
}

.home-panel {
  padding: 24px;
}

.panel-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;

  h2 {
    margin: 0;
    color: #1b2b48;
    font-size: 20px;
    font-weight: 900;
  }

  a {
    color: #a1adc2;
    font-size: 14px;
    font-weight: 800;
    text-decoration: none;
  }
}

.quick-entry-list {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 18px;
}

.quick-entry-item {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr);
  grid-template-rows: auto auto;
  column-gap: 16px;
  align-items: center;
  min-height: 74px;
  text-decoration: none;
  color: #223253;
}

.quick-entry-icon {
  grid-row: 1 / 3;
  width: 54px;
  height: 54px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: #fff;
  font-size: 15px;
  font-weight: 900;
  background: linear-gradient(135deg, #3b8dff, #116cff);
  box-shadow: 0 12px 26px rgba(37, 132, 255, 0.18);
}

.quick-entry-item:nth-child(2) .quick-entry-icon {
  background: linear-gradient(135deg, #35d99c, #13bd7a);
}

.quick-entry-item:nth-child(3) .quick-entry-icon {
  background: linear-gradient(135deg, #8d72ff, #6550ea);
}

.quick-entry-item:nth-child(4) .quick-entry-icon {
  background: linear-gradient(135deg, #ff9c53, #ff7844);
}

.quick-entry-item:nth-child(5) .quick-entry-icon {
  background: linear-gradient(135deg, #23c8ed, #0aa6d6);
}

.quick-entry-item strong {
  overflow: hidden;
  font-size: 16px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quick-entry-item small {
  overflow: hidden;
  color: #93a0b8;
  font-size: 13px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.club-recommendation-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
}

.club-card {
  overflow: hidden;
  border: 1px solid #eef4fd;
  border-radius: 8px;
  background: #fff;
}

.club-cover {
  position: relative;
  height: 118px;
  background-size: cover;
  background-position: center;

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(180deg, transparent 30%, rgba(11, 28, 58, 0.32));
  }
}

.club-logo {
  position: absolute;
  left: 50%;
  bottom: -32px;
  z-index: 1;
  width: 66px;
  height: 66px;
  border: 4px solid #fff;
  border-radius: 50%;
  object-fit: cover;
  transform: translateX(-50%);
  box-shadow: 0 14px 24px rgba(26, 62, 115, 0.16);
}

.club-body {
  display: grid;
  gap: 10px;
  padding: 42px 20px 18px;
}

.club-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;

  h3 {
    overflow: hidden;
    margin: 0;
    color: #1b2b48;
    font-size: 17px;
    font-weight: 900;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  span {
    flex: 0 0 auto;
    padding: 3px 8px;
    border-radius: 8px;
    background: #eaf3ff;
    color: #3d8bff;
    font-size: 12px;
    font-weight: 900;
  }
}

.club-body p {
  min-height: 42px;
  margin: 0;
  color: #8b98ae;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.62;
}

.club-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #7f8da7;
  font-weight: 800;

  a {
    min-width: 62px;
    height: 32px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    background: #eaf3ff;
    color: #1473ff;
    text-decoration: none;
  }
}

.carousel-dots {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 18px;

  button {
    width: 9px;
    height: 9px;
    padding: 0;
    border: 0;
    border-radius: 999px;
    background: #d9e4f5;
    cursor: pointer;
    transition: width 0.18s ease, background-color 0.18s ease;

    &.active {
      width: 22px;
      background: #1473ff;
    }
  }
}

.my-club-card {
  display: grid;
  grid-template-columns: 68px minmax(0, 1fr) minmax(280px, auto) auto;
  gap: 24px;
  align-items: center;
  padding: 18px;
  border: 1px solid #eef4fd;
  border-radius: 8px;

  img {
    width: 68px;
    height: 68px;
    border-radius: 50%;
    object-fit: cover;
  }

  h3,
  p {
    margin: 0;
  }

  h3 {
    color: #1b2b48;
    font-size: 20px;
    font-weight: 900;
  }

  p {
    margin-top: 6px;
    color: #8b98ae;
    font-weight: 700;
  }

  > a {
    min-width: 104px;
    height: 38px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border: 1px solid #1473ff;
    border-radius: 8px;
    color: #1473ff;
    font-weight: 900;
    text-decoration: none;
  }
}

.my-club-stats {
  display: grid;
  grid-template-columns: repeat(4, 74px);
  gap: 8px;
  text-align: center;

  span {
    color: #8b98ae;
    font-size: 13px;
    font-weight: 800;
  }

  strong {
    display: block;
    margin-bottom: 4px;
    color: #1b2b48;
    font-size: 20px;
  }
}

.stat-loading {
  display: block;
  margin: 0 auto 6px;
  color: #1473ff;
  font-size: 20px;
  animation: spin 0.9s linear infinite;
}

.home-today-card,
.home-notice-card,
.profile-summary-card {
  padding: 24px;
}

.today-activity-list {
  display: grid;
  gap: 22px;
}

.today-activity-item {
  display: grid;
  grid-template-columns: 10px 76px minmax(0, 1fr);
  gap: 14px;
  align-items: start;
  color: #1b2b48;
  text-decoration: none;

  > span {
    width: 10px;
    height: 10px;
    margin-top: 8px;
    border-radius: 50%;
    background: #3d8bff;

    &.green {
      background: #31d093;
    }

    &.orange {
      background: #ff8a45;
    }

    &.purple {
      background: #8366ff;
    }
  }

  strong,
  b {
    display: block;
    font-size: 16px;
    font-weight: 900;
  }

  small,
  em {
    display: block;
    margin-top: 4px;
    color: #8d9ab0;
    font-style: normal;
    font-weight: 700;
  }
}

.notice-list {
  display: grid;
}

.notice-list a {
  display: grid;
  grid-template-columns: 8px minmax(0, 1fr) 54px;
  gap: 12px;
  align-items: center;
  min-height: 52px;
  border-bottom: 1px solid #eef4fd;
  color: #1b2b48;
  text-decoration: none;

  span {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #9ec7ff;
  }

  strong {
    overflow: hidden;
    font-size: 15px;
    font-weight: 900;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  time {
    color: #9aa6bb;
    font-size: 13px;
    font-weight: 700;
    text-align: right;
  }
}

.profile-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;

  a {
    display: grid;
    justify-items: center;
    gap: 6px;
    color: #1b2b48;
    text-align: center;
    text-decoration: none;
  }

  span {
    width: 46px;
    height: 46px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    background: #eaf3ff;
    color: #1473ff;
    font-weight: 900;
  }

  strong {
    font-size: 18px;
    font-weight: 900;
  }

  small {
    color: #7f8da7;
    font-weight: 800;
  }
}

.home-section-loading {
  min-height: 132px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #73839d;

  &.compact {
    min-height: 96px;
  }

  p {
    margin: 0;
    font-size: 14px;
    font-weight: 700;
  }
}

.section-loading-icon {
  font-size: 30px;
  color: #1473ff;
  animation: spin 0.9s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@media (max-width: 1500px) {
  .portal-dashboard-grid {
    grid-template-columns: minmax(0, 1fr) 390px;
  }

  .quick-entry-list {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .club-recommendation-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .my-club-card {
    grid-template-columns: 68px minmax(0, 1fr);

    .my-club-stats,
    > a {
      grid-column: 1 / -1;
    }
  }
}

@media (max-width: 1180px) {
  .portal-dashboard-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .home-hero-banner {
    min-height: 196px;
    padding: 26px;
  }

  .home-hero-copy h1 {
    font-size: 28px;
  }

  .home-hero-copy p {
    font-size: 16px;
  }

  .quick-entry-list,
  .club-recommendation-grid,
  .profile-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .quick-entry-item {
    grid-template-columns: 46px minmax(0, 1fr);
  }

  .quick-entry-icon {
    width: 46px;
    height: 46px;
  }

  .today-activity-item {
    grid-template-columns: 10px 62px minmax(0, 1fr);
  }
}

@media (max-width: 520px) {
  .home-panel,
  .home-today-card,
  .home-notice-card,
  .profile-summary-card {
    padding: 18px;
  }

  .quick-entry-list,
  .club-recommendation-grid,
  .profile-stat-grid {
    grid-template-columns: 1fr;
  }

  .my-club-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
