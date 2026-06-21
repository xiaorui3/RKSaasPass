<template>
  <div class="statistics-board">
    <el-card class="overview-card">
      <template #header>
        <div class="card-header-flex">
          <span class="card-title">数据看板</span>
          <el-button @click="fetchBoardData">刷新</el-button>
        </div>
      </template>

      <el-alert
        class="scope-alert"
        type="info"
        :closable="false"
        show-icon
        :title="peopleScopeTitle"
      />

      <div class="stat-cards" v-loading="loading">
        <div
          v-for="stat in statCards"
          :key="stat.key"
          class="stat-card"
          :data-key="stat.key"
        >
          <div class="stat-label">{{ stat.label }}</div>
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-hint">{{ stat.hint }}</div>
        </div>
      </div>
    </el-card>

    <div class="grid">
      <el-card class="panel-card">
        <template #header>
          <span class="card-title">活动趋势（近 7 天）</span>
        </template>
        <div ref="trendChartRef" class="chart-container"></div>
      </el-card>

      <el-card class="panel-card">
        <template #header>
          <span class="card-title">内容分布</span>
        </template>
        <div ref="pieChartRef" class="chart-container"></div>
      </el-card>
    </div>

    <div class="grid">
      <el-card class="panel-card">
        <template #header>
          <span class="card-title">活动状态分布</span>
        </template>
        <div class="status-list">
          <div v-for="item in activityStatusStats" :key="item.label" class="status-row">
            <span class="status-label">{{ item.label }}</span>
            <el-tag>{{ item.value }}</el-tag>
          </div>
        </div>
      </el-card>

      <el-card class="panel-card">
        <template #header>
          <span class="card-title">人员域补充口径</span>
        </template>
        <div class="status-list">
          <div v-for="item in peopleDomainStats" :key="item.label" class="status-row with-helper">
            <div>
              <div class="status-label">{{ item.label }}</div>
              <div class="status-helper">{{ item.helper }}</div>
            </div>
            <el-tag>{{ item.value }}</el-tag>
          </div>
        </div>
      </el-card>
    </div>

    <div class="grid">
      <el-card class="panel-card">
        <template #header>
          <div class="card-header-flex">
            <span class="card-title">最新活动</span>
            <el-button text type="primary" @click="router.push('/admin/activity/list')">查看全部</el-button>
          </div>
        </template>
        <el-table :data="latestActivities" size="small" empty-text="暂无活动">
          <el-table-column prop="activityName" label="活动名称" min-width="220" show-overflow-tooltip />
          <el-table-column prop="location" label="地点" min-width="120" show-overflow-tooltip />
          <el-table-column prop="registrationEndTime" label="报名截止" min-width="160" />
          <el-table-column prop="currentParticipants" label="人数" width="80" />
        </el-table>
      </el-card>

      <el-card class="panel-card">
        <template #header>
          <div class="card-header-flex">
            <span class="card-title">最新比赛</span>
            <el-button text type="primary" @click="router.push('/admin/activity/competition')">查看全部</el-button>
          </div>
        </template>
        <el-table :data="latestCompetitions" size="small" empty-text="暂无比赛">
          <el-table-column prop="title" label="比赛名称" min-width="220" show-overflow-tooltip />
          <el-table-column prop="competitionType" label="类型" width="120" />
          <el-table-column prop="registrationEnd" label="报名截止" min-width="160" />
          <el-table-column prop="registrationCount" label="人数" width="80" />
        </el-table>
      </el-card>
    </div>

    <el-card class="panel-card">
      <template #header>
        <div class="card-header-flex">
          <span class="card-title">最新内容</span>
          <el-button text type="primary" @click="router.push('/admin/content/news')">查看新闻</el-button>
        </div>
      </template>
      <el-table :data="latestNews" size="small" empty-text="暂无新闻">
        <el-table-column prop="title" label="标题" min-width="280" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" min-width="120" />
        <el-table-column prop="author" label="作者" width="120" />
        <el-table-column prop="publishTime" label="发布时间" min-width="160" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { echarts } from '@/utils/echarts/board'
import { useUserStore } from '@/stores/user'
import { getUserPage } from '@/api/user'
import { getApplicationStatistics as getMemberStatistics } from '@/api/member'
import {
  getApplicationStatistics as getAdmissionStatistics,
  getRegisterReviewStatistics
} from '@/api/admission'
import { getAlumniOverview } from '@/api/alumni'
import { getActivityList } from '@/api/activity'
import { getNewsList } from '@/api/news'
import { getWorksList } from '@/api/works'
import { getCompetitionList } from '@/api/competition'
import { getPublishedNoticeList } from '@/api/notice'
import {
  buildPeopleDomainAlertTitle,
  buildPeopleDomainDiagnostics,
  buildPeopleDomainSourceHint,
  buildPeopleDomainSummary,
  buildStatisticsBoardCards
} from '@/utils/adminPeopleDomain'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const statCards = ref([])
const activityStatusStats = ref([])
const peopleDomainStats = ref([])
const latestActivities = ref([])
const latestCompetitions = ref([])
const latestNews = ref([])
const trendChartRef = ref(null)
const pieChartRef = ref(null)
const isSuperAdmin = computed(() => userStore.isSuperAdmin())
const peopleScopeTitle = computed(() => buildPeopleDomainAlertTitle(isSuperAdmin.value))
let trendChart = null
let pieChart = null

function normalizeList(data) {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.records)) return data.records
  return []
}

function mapActivityStatusLabel(status) {
  const map = {
    1: '未开始',
    2: '报名中',
    3: '进行中',
    4: '已结束'
  }
  return map[status] || `状态 ${status ?? '-'}`
}

async function fetchBoardData() {
  loading.value = true
  try {
    const [
      userRes,
      memberStatsRes,
      admissionStatsRes,
      registerReviewStatsRes,
      alumniOverviewRes,
      activitiesRes,
      newsRes,
      worksRes,
      competitionsRes,
      noticesRes
    ] = await Promise.all([
      getUserPage({ page: 1, size: 1 }),
      getMemberStatistics(),
      getAdmissionStatistics(),
      getRegisterReviewStatistics(),
      getAlumniOverview(),
      getActivityList(),
      getNewsList({ page: 1, size: 10 }),
      getWorksList(),
      getCompetitionList(),
      getPublishedNoticeList()
    ])

    const activities = normalizeList(activitiesRes.data)
    const news = normalizeList(newsRes.data)
    const works = normalizeList(worksRes.data)
    const competitions = normalizeList(competitionsRes.data)
    const notices = normalizeList(noticesRes.data)

    const peopleSummary = buildPeopleDomainSummary({
      userPage: userRes.data,
      memberStats: memberStatsRes.data,
      admissionStats: admissionStatsRes.data,
      registerReviewStats: registerReviewStatsRes.data,
      alumniOverview: alumniOverviewRes.data
    })

    statCards.value = buildStatisticsBoardCards(peopleSummary, {
      activities: activities.length,
      news: Number(newsRes.data?.total || news.length),
      works: works.length,
      competitions: competitions.length,
      notices: notices.length
    }, {
      isSuperAdmin: isSuperAdmin.value
    })

    peopleDomainStats.value = [
      { label: '账号数', value: peopleSummary.accountCount, helper: buildPeopleDomainSourceHint('rk_user', isSuperAdmin.value, '可登录账号') },
      { label: '成员数', value: peopleSummary.memberCount, helper: buildPeopleDomainSourceHint('club_members', isSuperAdmin.value, '正式成员台账') },
      { label: '入社申请总量', value: peopleSummary.joinApplicationTotal, helper: buildPeopleDomainSourceHint('join_requests', isSuperAdmin.value, '含待审核与已处理') },
      { label: '注册审核总量', value: peopleSummary.registerReviewTotal, helper: buildPeopleDomainSourceHint('register_review_request', isSuperAdmin.value, '含待审核与已处理') },
      { label: '入社已拒绝', value: peopleSummary.rejectedJoinApplicationCount, helper: buildPeopleDomainSourceHint('join_requests', isSuperAdmin.value, '拒绝轨迹') },
      { label: '已毕业校友', value: peopleSummary.graduatedAlumniCount, helper: buildPeopleDomainSourceHint('club_alumni', isSuperAdmin.value, '毕业状态为已毕业') },
      { label: '在校校友', value: peopleSummary.activeAlumniCount, helper: buildPeopleDomainSourceHint('club_alumni', isSuperAdmin.value, '非毕业状态') }
    ]

    peopleDomainStats.value.push(
      ...buildPeopleDomainDiagnostics(peopleSummary, {
        isSuperAdmin: isSuperAdmin.value
      }).map((item) => ({
        label: item.label,
        value: item.value,
        helper: item.helper
      }))
    )

    const activityStatusMap = new Map()
    for (const activity of activities) {
      const label = mapActivityStatusLabel(activity.activityStatus)
      activityStatusMap.set(label, (activityStatusMap.get(label) || 0) + 1)
    }
    activityStatusStats.value = Array.from(activityStatusMap.entries()).map(([label, value]) => ({ label, value }))

    latestActivities.value = activities
      .slice()
      .sort((a, b) => String(b.createTime || '').localeCompare(String(a.createTime || '')))
      .slice(0, 5)

    latestCompetitions.value = competitions
      .slice()
      .sort((a, b) => String(b.createTime || '').localeCompare(String(a.createTime || '')))
      .slice(0, 5)

    latestNews.value = news.slice(0, 8)

    await nextTick()
    renderCharts(activities, news, works, competitions, notices)
  } catch (error) {
    console.error('获取数据看板失败:', error)
    ElMessage.error(error.message || '获取数据看板失败')
  } finally {
    loading.value = false
  }
}

function renderCharts(activities, news, works, competitions, notices) {
  if (trendChartRef.value) {
    trendChart?.dispose()
    trendChart = echarts.init(trendChartRef.value)
    const days = []
    const counts = []
    for (let i = 6; i >= 0; i -= 1) {
      const current = new Date()
      current.setDate(current.getDate() - i)
      const label = `${current.getMonth() + 1}/${current.getDate()}`
      days.push(label)
      const dayStr = current.toISOString().split('T')[0]
      counts.push(activities.filter((item) => (item.createTime || '').startsWith(dayStr)).length)
    }
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 20, top: 20, bottom: 30 },
      xAxis: { type: 'category', data: days },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{
        name: '新增活动',
        type: 'line',
        smooth: true,
        data: counts,
        areaStyle: { opacity: 0.15 },
        itemStyle: { color: '#409eff' }
      }]
    })
  }

  if (pieChartRef.value) {
    pieChart?.dispose()
    pieChart = echarts.init(pieChartRef.value)
    pieChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: ['35%', '65%'],
        data: [
          { value: news.length, name: '新闻' },
          { value: notices.length, name: '公告' },
          { value: works.length, name: '作品' },
          { value: competitions.length, name: '比赛' }
        ],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }]
    })
  }
}

function handleResize() {
  trendChart?.resize()
  pieChart?.resize()
}

onMounted(() => {
  fetchBoardData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped>
.statistics-board {
  display: grid;
  gap: 20px;
}

.overview-card,
.panel-card {
  border-radius: 12px;
}

.scope-alert {
  margin-bottom: 16px;
}

.card-header-flex {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.stat-card {
  padding: 18px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  background: #fbfcfd;
}

.stat-label {
  color: #606266;
  font-size: 14px;
}

.stat-value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 700;
  color: #1f3a34;
}

.stat-hint {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.status-list {
  display: grid;
  gap: 12px;
}

.status-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.with-helper {
  align-items: flex-start;
}

.status-label {
  color: #303133;
}

.status-helper {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}

.chart-container {
  width: 100%;
  height: 300px;
}

@media (max-width: 900px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
