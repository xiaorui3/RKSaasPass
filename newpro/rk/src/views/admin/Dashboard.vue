<template>
  <div class="dashboard-design-shell" v-loading="loading">
    <section class="dashboard-workbench-banner" aria-label="后台工作台横幅">
      <div class="dashboard-banner-copy">
        <span>{{ workbenchEyebrow }}</span>
        <h1>{{ workbenchGreeting }}，{{ userName }}！</h1>
        <p>{{ workbenchSubtitle }}</p>
        <div class="dashboard-banner-points">
          <span>高效协作</span>
          <span>数据驱动</span>
          <span>安全可管</span>
        </div>
      </div>
      <div class="dashboard-banner-meta">
        <strong>{{ loginDate }}</strong>
        <span>{{ roleName }}</span>
      </div>
    </section>

    <section class="dashboard-summary-grid" aria-label="核心指标">
      <article
        v-for="stat in summaryCards"
        :key="stat.key"
        class="dashboard-summary-card"
      >
        <span class="summary-icon" :style="{ background: stat.color }">
          <el-icon :size="24"><component :is="stat.icon" /></el-icon>
        </span>
        <div>
          <small>{{ stat.label }}</small>
          <strong>{{ stat.value }}</strong>
          <em>{{ stat.shortHelper }}</em>
        </div>
      </article>
    </section>

    <section class="dashboard-main-grid">
      <article class="dashboard-reference-card overview-card">
        <div class="card-title-row">
          <h2>社团概览</h2>
          <button type="button" @click="router.push('/admin/statistics/board')">查看全部</button>
        </div>
        <div class="overview-body">
          <div class="donut-chart" :style="{ '--first': `${donutPrimaryPercent}%` }">
            <strong>{{ totalOverviewCount }}</strong>
            <span>{{ overviewTotalLabel }}</span>
          </div>
          <div class="overview-legend">
            <span v-for="item in overviewSegments" :key="item.label">
              <i :style="{ background: item.color }"></i>
              {{ item.label }}
              <b>{{ item.value }}</b>
            </span>
          </div>
        </div>
        <div class="overview-footer">
          <span v-for="item in peopleDiagnostics.slice(0, 4)" :key="item.key">
            <small>{{ item.label }}</small>
            <strong>{{ item.value }}</strong>
          </span>
        </div>
      </article>

      <article class="dashboard-reference-card recent-card">
        <div class="card-title-row">
          <h2>近期活动</h2>
          <button type="button" @click="router.push(activityMorePath)">查看全部</button>
        </div>
        <div class="recent-activity-list" v-if="recentActivities.length">
          <button
            v-for="activity in recentActivities.slice(0, 4)"
            :key="activity.title"
            type="button"
            @click="router.push(activityMorePath)"
          >
            <span class="activity-thumb"></span>
            <div>
              <strong>{{ activity.title }}</strong>
              <small>时间：{{ activity.startTime || '待定' }}</small>
              <small>状态：{{ activity.statusText }}</small>
            </div>
            <em :class="{ active: activity.status === 1 }">{{ activity.statusText }}</em>
          </button>
        </div>
        <el-empty v-else description="暂无活动数据" :image-size="74" />
      </article>

      <article class="dashboard-reference-card pending-card">
        <div class="card-title-row">
          <h2>待处理事项</h2>
          <button type="button" @click="router.push('/admin/club/applications')">更多</button>
        </div>
        <div class="pending-list">
          <button
            v-for="item in pendingItems"
            :key="item.label"
            type="button"
            @click="router.push(item.path)"
          >
            <span :class="item.tone">{{ item.short }}</span>
            <strong>{{ item.label }}</strong>
            <em>{{ item.count }}</em>
          </button>
        </div>
      </article>

      <article class="dashboard-reference-card trend-card">
        <div class="card-title-row">
          <h2>活动参与趋势</h2>
          <button type="button">近30天</button>
        </div>
        <div class="trend-chart" aria-hidden="true">
          <span v-for="point in trendPoints" :key="point" :style="{ height: `${point}%` }"></span>
        </div>
        <div class="trend-axis">
          <span>04-20</span>
          <span>04-28</span>
          <span>05-06</span>
          <span>05-14</span>
          <span>05-20</span>
        </div>
      </article>

      <article class="dashboard-reference-card news-card">
        <div class="card-title-row">
          <h2>最新新闻</h2>
          <button type="button" @click="router.push(newsMorePath)">查看更多</button>
        </div>
        <div class="news-list" v-if="recentNews.length">
          <button
            v-for="news in recentNews.slice(0, 5)"
            :key="news.title"
            type="button"
            @click="router.push(newsMorePath)"
          >
            <strong>{{ news.title }}</strong>
            <span>{{ news.createTime }}</span>
          </button>
        </div>
        <el-empty v-else description="暂无新闻数据" :image-size="74" />
      </article>

      <article class="dashboard-reference-card quick-card">
        <div class="card-title-row">
          <h2>快捷操作</h2>
        </div>
        <div class="quick-action-grid">
          <button
            v-for="action in quickActions"
            :key="action.path"
            type="button"
            @click="router.push(action.path)"
          >
            <span>
              <el-icon><component :is="action.icon" /></el-icon>
            </span>
            <strong>{{ action.name }}</strong>
          </button>
        </div>
      </article>

      <article class="dashboard-reference-card">
        <div class="system-card">
          <div class="card-title-row">
            <h2>系统信息</h2>
          </div>
          <dl>
            <div>
              <dt>系统名称</dt>
              <dd>RK-Web 社团管理系统</dd>
            </div>
            <div>
              <dt>当前版本</dt>
              <dd>v1.0.0</dd>
            </div>
            <div>
              <dt>当前角色</dt>
              <dd>{{ roleName }}</dd>
            </div>
            <div>
              <dt>登录时间</dt>
              <dd>{{ loginTime }}</dd>
            </div>
          </dl>
        </div>
      </article>
    </section>

    <el-alert
      class="people-alert"
      type="info"
      :closable="false"
      show-icon
      :title="peopleScopeTitle"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getNewsList } from '@/api/news'
import { getActivityPage } from '@/api/activity'
import { getScopedUserStatistics } from '@/api/user'
import { getApplicationStatistics as getMemberStatistics } from '@/api/member'
import {
  getApplicationStatistics as getAdmissionStatistics,
  getRegisterReviewStatistics
} from '@/api/admission'
import { getAlumniOverview } from '@/api/alumni'
import {
  buildDashboardStats,
  buildPeopleDomainAlertTitle,
  buildPeopleDomainDiagnostics,
  buildPeopleDomainSummary
} from '@/utils/adminPeopleDomain'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const userName = computed(() => userStore.userName || '用户')
const isSuperAdmin = computed(() => userStore.isSuperAdmin())
const peopleScopeTitle = computed(() => buildPeopleDomainAlertTitle(isSuperAdmin.value))
const roleName = computed(() => {
  const roles = userStore.userInfo?.roles || []
  if (roles.length > 0) {
    const roleMap = { 1: '超级管理员', 3: '租户管理员', 5: '租户管理员', 7: '社团负责人', 8: '指导老师', 2: '普通成员' }
    return roleMap[roles[0].roleId] || '普通成员'
  }
  return '普通成员'
})
const loginTime = computed(() => new Date().toLocaleString('zh-CN'))
const loginDate = computed(() => new Date().toLocaleDateString('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
}))

const userRole = computed(() => userStore.userInfo?.roles?.[0]?.roleId || 0)
const activityMorePath = computed(() => (userRole.value === 8 ? '/admin/activity/approval' : '/admin/activity/list'))
const newsMorePath = computed(() => (userRole.value === 8 ? '/admin/content/news-approval' : '/admin/content/news'))
const workbenchEyebrow = computed(() => (userRole.value === 8 ? 'Teacher Workbench' : 'Admin Workbench'))
const workbenchGreeting = computed(() => (userRole.value === 8 ? '欢迎回来，指导老师' : '欢迎回来'))
const workbenchSubtitle = computed(() => (
  userRole.value === 8
    ? '集中查看社团审批、活动跟进、成员动态和内容审核。'
    : '一站式管理社团事务、运营数据、通知内容和系统配置。'
))

const stats = ref([])
const peopleDiagnostics = ref([])
const recentActivities = ref([])
const recentNews = ref([])
const trendPoints = [18, 34, 28, 46, 42, 60, 38, 52, 72, 64, 48, 78]

function withDashboardFallback(promise, fallback = { code: 500 }) {
  return promise.catch((error) => {
    console.warn('dashboard section fallback:', error)
    return fallback
  })
}

const quickActions = computed(() => {
  const actions = [
    { name: '发布新闻', path: '/admin/content/news', icon: 'Plus', type: 'primary' },
    { name: '创建活动', path: '/admin/activity/list', icon: 'Plus', type: 'success' },
    { name: '成员管理', path: '/admin/club/members', icon: 'User', type: 'warning' },
    { name: '数据统计', path: '/admin/statistics/board', icon: 'DataLine', type: 'info' }
  ]
  return actions.filter((action) => userStore.canAccessAdminPath(action.path))
})

const summaryCards = computed(() => {
  const cards = stats.value.slice(0, 4)
  return cards.map((item) => ({
    ...item,
    shortHelper: item.helper ? String(item.helper).replace(/\s+/g, ' ').slice(0, 18) : '较上月保持稳定'
  }))
})

const getStatValue = (key) => Number(stats.value.find((item) => item.key === key)?.value || 0)
const totalOverviewCount = computed(() => (
  getStatValue('members') || getStatValue('accounts') || summaryCards.value.reduce((sum, item) => sum + Number(item.value || 0), 0)
))
const overviewTotalLabel = computed(() => (isSuperAdmin.value ? '总账号' : '总社团成员'))
const overviewSegments = computed(() => [
  { label: '成员', value: getStatValue('members'), color: '#3b82f6' },
  { label: '待审入社', value: getStatValue('pendingJoinApplications'), color: '#8b5cf6' },
  { label: '待审注册', value: getStatValue('pendingRegisterReviews'), color: '#f59e0b' },
  { label: '活动', value: getStatValue('activities'), color: '#10b981' }
])
const donutPrimaryPercent = computed(() => {
  const total = overviewSegments.value.reduce((sum, item) => sum + item.value, 0)
  if (!total) return 40
  return Math.max(18, Math.round((overviewSegments.value[0].value / total) * 100))
})

const pendingItems = computed(() => {
  const items = [
    {
      label: '活动申请待审批',
      short: '审',
      count: getStatValue('pendingJoinApplications'),
      tone: 'blue',
      path: '/admin/activity/approval'
    },
    {
      label: '注册审核待处理',
      short: '注',
      count: getStatValue('pendingRegisterReviews'),
      tone: 'green',
      path: '/admin/system/users'
    },
    {
      label: '成员入社申请',
      short: '人',
      count: getStatValue('pendingJoinApplications'),
      tone: 'orange',
      path: '/admin/club/applications'
    },
    {
      label: '新闻内容审核',
      short: '文',
      count: recentNews.value.length,
      tone: 'purple',
      path: newsMorePath.value
    }
  ]
  return items.filter((item) => userStore.canAccessAdminPath(item.path))
})

onMounted(async () => {
  try {
    const [
      userRes,
      memberStatsRes,
      admissionStatsRes,
      registerReviewStatsRes,
      alumniOverviewRes,
      newsRes,
      activityRes
    ] = (await Promise.allSettled([
      withDashboardFallback(getScopedUserStatistics()),
      withDashboardFallback(getMemberStatistics()),
      withDashboardFallback(getAdmissionStatistics()),
      withDashboardFallback(getRegisterReviewStatistics()),
      withDashboardFallback(getAlumniOverview()),
      withDashboardFallback(getNewsList({ page: 1, size: 5 }), { code: 500, data: { total: 0, records: [] } }),
      withDashboardFallback(getActivityPage({ page: 1, size: 5 }), { code: 500, data: { total: 0, records: [] } })
    ])).map((item) => (item.status === 'fulfilled' ? item.value : { code: 500 }))

    const peopleSummary = buildPeopleDomainSummary({
      userStats: userRes.code === 200 ? userRes.data : null,
      memberStats: memberStatsRes.code === 200 ? memberStatsRes.data : null,
      admissionStats: admissionStatsRes.code === 200 ? admissionStatsRes.data : null,
      registerReviewStats: registerReviewStatsRes.code === 200 ? registerReviewStatsRes.data : null,
      alumniOverview: alumniOverviewRes.code === 200 ? alumniOverviewRes.data : null
    })

    peopleDiagnostics.value = buildPeopleDomainDiagnostics(peopleSummary, {
      isSuperAdmin: isSuperAdmin.value
    })

    stats.value = buildDashboardStats(peopleSummary, {
      activities: activityRes.code === 200 ? activityRes.data?.total || 0 : 0,
      news: newsRes.code === 200 ? newsRes.data?.total || 0 : 0
    }, {
      isSuperAdmin: isSuperAdmin.value
    })

    if (newsRes.code === 200) {
      recentNews.value = (newsRes.data?.records || []).map((item) => ({
        title: item.title,
        author: item.author || '管理员',
        createTime: item.createTime ? item.createTime.substring(0, 10) : ''
      }))
    }

    if (activityRes.code === 200) {
      recentActivities.value = (activityRes.data?.records || []).map((item) => ({
        title: item.title || item.name,
        status: item.status,
        statusText: item.status === 1 ? '进行中' : '已结束',
        startTime: item.startTime ? item.startTime.substring(0, 10) : ''
      }))
    }
  } catch (error) {
    console.error('加载仪表盘数据失败:', error)
    ElMessage.error('加载仪表盘数据失败')
  } finally {
    loading.value = false
  }
})
</script>

<style lang="scss" scoped>
.dashboard-design-shell {
  display: grid;
  gap: 22px;
  color: #14213d;
}

.dashboard-workbench-banner {
  min-height: 150px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 28px 34px;
  border-radius: 8px;
  background-image:
    linear-gradient(90deg, rgba(255, 255, 255, 0.95) 0%, rgba(240, 248, 255, 0.82) 42%, rgba(255, 255, 255, 0.08) 100%),
    url('/assets/visuals/rk-admin-ops.png');
  background-size: cover;
  background-position: center right;
  box-shadow: 0 18px 42px rgba(43, 102, 179, 0.08);
  overflow: hidden;
}

.dashboard-banner-copy {
  display: grid;
  gap: 10px;

  span {
    color: #1473ff;
    font-size: 13px;
    font-weight: 900;
  }

  h1 {
    margin: 0;
    color: #111f3c;
    font-size: 30px;
    font-weight: 900;
    line-height: 1.2;
  }

  p {
    margin: 0;
    color: #7f8da7;
    font-size: 15px;
    font-weight: 800;
  }
}

.dashboard-banner-points {
  display: flex;
  gap: 36px;
  margin-top: 14px;

  span {
    color: #fff;
    padding-left: 34px;
    position: relative;
  }

  span::before {
    content: '';
    position: absolute;
    left: 0;
    top: 50%;
    width: 24px;
    height: 24px;
    border-radius: 50%;
    background: rgba(20, 115, 255, 0.72);
    transform: translateY(-50%);
  }
}

.dashboard-banner-meta {
  display: grid;
  justify-items: end;
  gap: 8px;
  color: #52617a;

  strong {
    color: #14213d;
    font-size: 16px;
  }

  span {
    padding: 5px 12px;
    border-radius: 999px;
    background: #eaf3ff;
    color: #1473ff;
    font-weight: 900;
  }
}

.dashboard-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
}

.dashboard-summary-card,
.dashboard-reference-card {
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 18px 42px rgba(43, 102, 179, 0.08);
}

.dashboard-summary-card {
  min-height: 122px;
  display: grid;
  grid-template-columns: 62px minmax(0, 1fr);
  gap: 18px;
  align-items: center;
  padding: 20px 24px;
}

.summary-icon {
  width: 62px;
  height: 62px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: #fff;
}

.dashboard-summary-card div {
  display: grid;
  gap: 5px;
}

.dashboard-summary-card small,
.dashboard-summary-card em {
  color: #8b98ae;
  font-style: normal;
  font-weight: 800;
}

.dashboard-summary-card strong {
  color: #111f3c;
  font-size: 28px;
  font-weight: 900;
}

.dashboard-main-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 1.1fr) minmax(320px, 0.84fr);
  gap: 20px;
  align-items: start;
}

.dashboard-reference-card {
  padding: 24px;
}

.trend-card {
  grid-column: span 2;
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;

  h2 {
    margin: 0;
    color: #14213d;
    font-size: 19px;
    font-weight: 900;
  }

  button {
    border: none;
    background: transparent;
    color: #a0abc0;
    font-weight: 900;
    cursor: pointer;
  }
}

.overview-body {
  display: grid;
  grid-template-columns: 230px minmax(0, 1fr);
  gap: 24px;
  align-items: center;
}

.donut-chart {
  width: 206px;
  height: 206px;
  display: grid;
  place-items: center;
  place-content: center;
  gap: 4px;
  border-radius: 50%;
  background:
    radial-gradient(circle, #fff 0 54%, transparent 55%),
    conic-gradient(#3b82f6 0 var(--first), #8b5cf6 var(--first) 62%, #f59e0b 62% 78%, #10b981 78% 100%);

  strong {
    color: #111f3c;
    font-size: 34px;
    font-weight: 900;
  }

  span {
    color: #8b98ae;
    font-weight: 800;
  }
}

.overview-legend {
  display: grid;
  gap: 16px;

  span {
    display: grid;
    grid-template-columns: 10px minmax(0, 1fr) auto;
    gap: 10px;
    align-items: center;
    color: #52617a;
    font-weight: 800;
  }

  i {
    width: 10px;
    height: 10px;
    border-radius: 50%;
  }

  b {
    color: #14213d;
  }
}

.overview-footer {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 24px;

  span {
    display: grid;
    gap: 6px;
    padding: 14px;
    border-radius: 8px;
    background: #f6f9fe;
    text-align: center;
  }

  small {
    color: #8b98ae;
    font-weight: 800;
  }

  strong {
    color: #14213d;
    font-size: 20px;
    font-weight: 900;
  }
}

.recent-activity-list,
.pending-list,
.news-list {
  display: grid;
  gap: 14px;
}

.recent-activity-list button,
.pending-list button,
.news-list button,
.quick-action-grid button {
  border: none;
  background: transparent;
  cursor: pointer;
  text-align: left;
}

.recent-activity-list button {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
}

.activity-thumb {
  width: 86px;
  height: 62px;
  border-radius: 8px;
  background-image: url('/assets/visuals/rk-activity-action.png');
  background-size: cover;
  background-position: center;
}

.recent-activity-list strong,
.pending-list strong,
.news-list strong {
  overflow: hidden;
  color: #14213d;
  font-size: 15px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-activity-list small {
  display: block;
  margin-top: 4px;
  color: #8b98ae;
  font-weight: 700;
}

.recent-activity-list em {
  padding: 5px 12px;
  border-radius: 999px;
  background: #eef1f6;
  color: #7f8da7;
  font-style: normal;
  font-weight: 900;

  &.active {
    background: #dcfce7;
    color: #16a34a;
  }
}

.pending-list button {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  min-height: 54px;
}

.pending-list span {
  width: 42px;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: #fff;
  font-weight: 900;

  &.blue { background: #3b82f6; }
  &.green { background: #10b981; }
  &.orange { background: #f97316; }
  &.purple { background: #8b5cf6; }
}

.pending-list em {
  min-width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #f3f6fb;
  color: #ef4444;
  font-style: normal;
  font-weight: 900;
}

.trend-chart {
  height: 196px;
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  align-items: end;
  gap: 12px;
  padding: 18px 12px;
  border-radius: 8px;
  background:
    repeating-linear-gradient(0deg, transparent 0 47px, #e7eef9 48px 49px),
    linear-gradient(180deg, #fbfdff, #f6f9fe);
}

.trend-chart span {
  border-radius: 999px 999px 4px 4px;
  background: linear-gradient(180deg, #3b82f6, #93c5fd);
}

.trend-axis {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
  color: #9aa6bb;
  font-weight: 800;
}

.news-list button {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 70px;
  gap: 12px;
  align-items: center;
  min-height: 40px;
  border-bottom: 1px solid #edf3fb;
}

.news-list span {
  color: #9aa6bb;
  font-weight: 800;
  text-align: right;
}

.quick-action-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.quick-action-grid button {
  display: grid;
  justify-items: center;
  gap: 10px;
  color: #52617a;
  font-weight: 900;
}

.quick-action-grid span {
  width: 48px;
  height: 48px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: linear-gradient(135deg, #2d8cff, #1269f6);
  color: #fff;
}

.system-card dl {
  display: grid;
  gap: 12px;
  margin: 0;
}

.system-card div {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr);
  gap: 12px;
  align-items: start;
  padding-bottom: 12px;
  border-bottom: 1px solid #edf3fb;
}

.system-card dt {
  color: #8b98ae;
  font-weight: 800;
}

.system-card dd {
  margin: 0;
  color: #14213d;
  font-weight: 900;
}

.people-alert {
  border-radius: 8px;
}

@media (max-width: 1440px) {
  .dashboard-summary-grid,
  .quick-action-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-main-grid {
    grid-template-columns: 1fr 1fr;
  }

  .pending-card,
  .system-card {
    grid-column: span 1;
  }
}

@media (max-width: 980px) {
  .dashboard-main-grid,
  .dashboard-summary-grid {
    grid-template-columns: 1fr;
  }

  .trend-card {
    grid-column: auto;
  }

  .overview-body {
    grid-template-columns: 1fr;
    justify-items: center;
  }

  .overview-footer {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-workbench-banner {
    align-items: flex-start;
    flex-direction: column;
  }

  .dashboard-banner-meta {
    justify-items: start;
  }
}

@media (max-width: 640px) {
  .dashboard-workbench-banner,
  .dashboard-reference-card,
  .dashboard-summary-card {
    padding: 18px;
  }

  .dashboard-banner-copy h1 {
    font-size: 24px;
  }

  .dashboard-banner-points {
    gap: 12px;
    flex-wrap: wrap;
  }

  .recent-activity-list button {
    grid-template-columns: 1fr;
  }

  .overview-footer,
  .quick-action-grid {
    grid-template-columns: 1fr;
  }
}
</style>
