<template>
  <div class="visual-screen">
    <section class="screen-header">
      <div>
        <span class="eyebrow">Tenant Operation</span>
        <h1>租户运营大屏</h1>
        <p>按租户聚合在线用户、报名统计、内容总览、缓存命中和系统活跃数据。</p>
      </div>
      <div class="screen-actions">
        <span>最后刷新：{{ overview.updatedAt || overview.generatedAt || refreshedAt || '未刷新' }}</span>
        <el-button type="primary" :loading="loading" @click="loadScreenData">刷新</el-button>
      </div>
    </section>

    <section class="metric-grid">
      <div v-for="item in metricItems" :key="item.label" class="metric-tile">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.hint }}</small>
      </div>
    </section>

    <section class="screen-grid">
      <div class="screen-panel wide">
        <div class="panel-title">
          <strong>租户运营卡片</strong>
          <span>{{ tenantCards.length }} 个租户</span>
        </div>
        <div class="tenant-grid">
          <div v-for="tenant in tenantCards" :key="tenant.id || tenant.tenantId || tenant.code" class="tenant-card">
            <div class="tenant-head">
              <div>
                <strong>{{ tenant.name || tenant.tenantName || '未命名租户' }}</strong>
                <small>{{ tenant.code || tenant.tenantCode || '-' }}</small>
              </div>
              <el-tag :type="tenant.enabled === false ? 'info' : 'success'" size="small">
                {{ tenant.enabled === false ? '停用' : '运行中' }}
              </el-tag>
            </div>
            <div class="tenant-stats">
              <span>用户 {{ numberValue(tenant.userCount) }}</span>
              <span>在线 {{ numberValue(tenant.onlineUsers || tenant.onlineUserCount) }}</span>
              <span>报名 {{ numberValue(tenant.registrationCount || tenant.admissionCount) }}</span>
              <span>活动 {{ numberValue(tenant.activityCount) }}</span>
              <span>比赛 {{ numberValue(tenant.competitionCount) }}</span>
              <span>内容 {{ numberValue(tenant.contentCount) }}</span>
            </div>
          </div>
          <el-empty v-if="!tenantCards.length" description="暂无租户数据" />
        </div>
      </div>

      <div class="screen-panel">
        <div class="panel-title">
          <strong>在线用户</strong>
          <span>最近 {{ numberValue(onlineUsers.windowMinutes) || 10 }} 分钟</span>
        </div>
        <div ref="onlineChartRef" class="chart-box" />
      </div>

      <div class="screen-panel">
        <div class="panel-title">
          <strong>报名统计</strong>
          <span>报名 / 签到 / 取消</span>
        </div>
        <div ref="registrationChartRef" class="chart-box" />
      </div>

      <div class="screen-panel">
        <div class="panel-title">
          <strong>内容总览</strong>
          <span>{{ numberValue(contentOverview.total || contentOverview.totalCount) }} 条内容</span>
        </div>
        <div class="content-list">
          <div v-for="item in contentItems" :key="item.label" class="data-row">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.hint }}</small>
          </div>
        </div>
      </div>

      <div class="screen-panel">
        <div class="panel-title">
          <strong>缓存命中</strong>
          <span>cacheSnapshot / Redis 前台缓存</span>
        </div>
        <div class="content-list">
          <div v-for="item in cacheItems" :key="item.label" class="data-row">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.hint }}</small>
          </div>
        </div>
      </div>

      <div class="screen-panel">
        <div class="panel-title">
          <strong>在线明细</strong>
          <span>{{ onlineRows.length }} 条</span>
        </div>
        <div class="online-list">
          <div v-for="row in onlineRows.slice(0, 8)" :key="`${row.userName}-${row.ip}-${row.lastSeenAt}`" class="online-row">
            <div>
              <strong>{{ row.userName || '-' }}</strong>
              <small>{{ row.tenantName || '默认租户' }}</small>
            </div>
            <span>{{ row.ip || '-' }}</span>
          </div>
          <el-empty v-if="!onlineRows.length" description="暂无在线用户" />
        </div>
      </div>

      <div class="screen-panel">
        <div class="panel-title">
          <strong>ES 索引健康</strong>
          <span>searchHealth / Elasticsearch</span>
        </div>
        <div class="content-list">
          <div v-for="item in searchHealthItems" :key="item.label" class="data-row">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.hint }}</small>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { echarts } from '@/utils/echarts'
import { getSaasVisualScreen } from '@/api/data'

const loading = ref(false)
const refreshedAt = ref('')
const onlineChartRef = ref(null)
const registrationChartRef = ref(null)
let onlineChart = null
let registrationChart = null

const overview = reactive({
  tenantId: null,
  tenantScope: '',
  tenantName: '',
  updatedAt: '',
  generatedAt: '',
  tenantCards: [],
  tenants: [],
  onlineUsers: {},
  registrationStats: {},
  admissions: {},
  contentOverview: {},
  content: {},
  cacheSnapshot: {},
  cache: {},
  metrics: [],
  tenantOverview: {},
  people: {},
  activity: {},
  approvals: {},
  interactions: {},
  growth: {},
  finance: {},
  notifications: {},
  searchHealth: {},
  serviceHealth: {},
  sources: []
})

function unwrapResponse(res) {
  if (res?.code && res.code !== 200) {
    throw new Error(res.msg || '接口请求失败')
  }
  return res?.data || res || {}
}

function numberValue(value) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

function hasObjectValues(value) {
  return Boolean(
    value
    && typeof value === 'object'
    && !Array.isArray(value)
    && Object.values(value).some((item) => {
      if (Array.isArray(item)) return item.length > 0
      return item !== null && item !== undefined && item !== '' && item !== 0
    })
  )
}

const tenantCards = computed(() => {
  const rows = Array.isArray(overview.tenantCards) ? overview.tenantCards : overview.tenants
  if (Array.isArray(rows) && rows.length) {
    return rows
  }
  return [{
    id: overview.tenantId || overview.tenantScope || 'ALL',
    tenantId: overview.tenantId,
    tenantName: overview.tenantName || (overview.tenantScope === 'ALL' ? '全平台' : `租户 ${overview.tenantId || '-'}`),
    code: overview.tenantScope,
    enabled: overview.serviceHealth?.status !== 'DOWN',
    userCount: overview.tenantOverview?.currentTenantUsers,
    onlineUsers: overview.people?.onlineCount,
    registrationCount: overview.activity?.registrationCount,
    activityCount: overview.activity?.activityCount,
    competitionCount: overview.activity?.competitionCount,
    contentCount: numberValue(overview.content?.newsCount) + numberValue(overview.content?.noticeCount)
  }]
})
const onlineUsers = computed(() => hasObjectValues(overview.onlineUsers) ? overview.onlineUsers : {
  total: overview.people?.onlineCount,
  count: overview.people?.onlineCount,
  windowMinutes: 10,
  rows: []
})
const registrationStats = computed(() => hasObjectValues(overview.registrationStats) ? overview.registrationStats : (hasObjectValues(overview.admissions) ? overview.admissions : {
  total: overview.activity?.registrationCount,
  registered: overview.activity?.registrationCount,
  checkedIn: 0,
  cancelled: 0,
  absent: 0
}))
const contentOverview = computed(() => hasObjectValues(overview.contentOverview) ? overview.contentOverview : {
  total: numberValue(overview.content?.newsCount) + numberValue(overview.content?.noticeCount)
    + numberValue(overview.activity?.activityCount) + numberValue(overview.activity?.competitionCount),
  newsCount: overview.content?.newsCount,
  publishedNewsCount: overview.content?.newsCount,
  activityCount: overview.activity?.activityCount,
  competitionCount: overview.activity?.competitionCount,
  noticeCount: overview.content?.noticeCount,
  workCount: 0
})
const cacheSnapshot = computed(() => hasObjectValues(overview.cacheSnapshot) ? overview.cacheSnapshot : {
  keyCount: overview.cache?.key ? 1 : 0,
  totalKeys: overview.cache?.key ? 1 : 0,
  hitCount: overview.cache?.hit ? 1 : 0,
  nullHitCount: 0,
  nullMarkerCount: 0,
  randomExpireCount: overview.cache?.ttlSeconds ? 1 : 0,
  warmupKeyCount: 0,
  totalSizeText: overview.cache?.mode || ''
})
const onlineRows = computed(() => {
  if (Array.isArray(onlineUsers.value.rows)) {
    return onlineUsers.value.rows
  }
  return []
})

const metricItems = computed(() => {
  if (Array.isArray(overview.metrics) && overview.metrics.length) {
    return overview.metrics
  }
  return [
    { label: '租户数', value: tenantCards.value.length, hint: '当前纳入统计的租户' },
    { label: '在线用户', value: numberValue(onlineUsers.value.total || onlineUsers.value.count), hint: '最近登录活跃人数' },
    { label: '报名总数', value: numberValue(registrationStats.value.total), hint: '活动和比赛报名' },
    { label: '内容总量', value: numberValue(contentOverview.value.total || contentOverview.value.totalCount), hint: '新闻、活动、公告、作品' },
    { label: '待审批', value: numberValue(overview.approvals?.totalPendingCount), hint: '入社、内容、财务待办' },
    { label: '缓存 Key', value: numberValue(cacheSnapshot.value.totalKeys || cacheSnapshot.value.keyCount), hint: '前台 Redis 缓存快照' }
  ]
})

const contentItems = computed(() => [
  { label: '新闻', value: numberValue(contentOverview.value.newsCount), hint: `已发布 ${numberValue(contentOverview.value.publishedNewsCount)} 条` },
  { label: '活动', value: numberValue(contentOverview.value.activityCount), hint: '普通活动' },
  { label: '比赛', value: numberValue(contentOverview.value.competitionCount), hint: '竞赛活动' },
  { label: '公告', value: numberValue(contentOverview.value.noticeCount), hint: '站内公告' },
  { label: '作品', value: numberValue(contentOverview.value.workCount), hint: '成果与作品' }
])

const cacheItems = computed(() => [
  { label: 'Key 数', value: numberValue(cacheSnapshot.value.totalKeys || cacheSnapshot.value.keyCount), hint: cacheSnapshot.value.totalSizeText || '前台缓存总量' },
  { label: '缓存命中', value: numberValue(cacheSnapshot.value.hitCount), hint: 'Redis 直接返回次数' },
  { label: '空值命中', value: numberValue(cacheSnapshot.value.nullHitCount || cacheSnapshot.value.nullMarkerCount), hint: '防穿透缓存' },
  { label: '随机失效', value: numberValue(cacheSnapshot.value.randomExpireCount), hint: '避免同一时间雪崩' },
  { label: '预热记录', value: numberValue(cacheSnapshot.value.warmupKeyCount), hint: 'XXL-Job 每小时预热' }
])

const searchHealth = computed(() => hasObjectValues(overview.searchHealth) ? overview.searchHealth : {
  indexName: 'rk_global_search',
  indexStatus: 'UNKNOWN',
  documentCount: 0,
  staleDocumentCount: 0,
  lastIndexedAt: ''
})

const searchHealthItems = computed(() => [
  { label: 'ES 状态', value: searchHealth.value.indexStatus || 'UNKNOWN', hint: searchHealth.value.indexName || 'rk_global_search' },
  { label: '索引文档', value: numberValue(searchHealth.value.documentCount), hint: 'Elasticsearch 全局搜索文档' },
  { label: '过期文档', value: numberValue(searchHealth.value.staleDocumentCount), hint: '超过 24 小时未更新' },
  { label: '最近同步', value: searchHealth.value.lastIndexedAt || '-', hint: 'ES 索引最后更新时间' }
])

function tenantNames() {
  return tenantCards.value.map((item) => item.name || item.tenantName || item.code || '-').slice(0, 8)
}

function renderCharts() {
  if (onlineChartRef.value) {
    onlineChart = onlineChart || echarts.init(onlineChartRef.value)
    onlineChart.setOption({
      tooltip: {},
      grid: { left: 42, right: 18, top: 28, bottom: 56 },
      xAxis: { type: 'category', data: tenantNames(), axisLabel: { color: '#9ec5ff', rotate: 24 } },
      yAxis: { type: 'value', axisLabel: { color: '#9ec5ff' }, splitLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.18)' } } },
      series: [{
        name: '在线用户',
        type: 'bar',
        data: tenantCards.value.slice(0, 8).map((item) => numberValue(item.onlineUsers || item.onlineUserCount)),
        itemStyle: { color: '#22d3ee' }
      }]
    })
  }

  if (registrationChartRef.value) {
    registrationChart = registrationChart || echarts.init(registrationChartRef.value)
    registrationChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0, textStyle: { color: '#dbeafe' } },
      series: [{
        name: '报名统计',
        type: 'pie',
        radius: ['46%', '70%'],
        center: ['50%', '44%'],
        data: [
          { name: '已报名', value: numberValue(registrationStats.value.registered || registrationStats.value.approvedCount) },
          { name: '已签到', value: numberValue(registrationStats.value.checkedIn) },
          { name: '已取消', value: numberValue(registrationStats.value.cancelled || registrationStats.value.rejectedCount) },
          { name: '异常状态', value: numberValue(registrationStats.value.absent) }
        ],
        label: { color: '#dbeafe' },
        itemStyle: { borderColor: '#061b3a', borderWidth: 2 }
      }]
    })
  }
}

async function loadScreenData() {
  loading.value = true
  try {
    Object.assign(overview, unwrapResponse(await getSaasVisualScreen()))
    refreshedAt.value = new Date().toLocaleString()
    await nextTick()
    renderCharts()
  } catch (error) {
    ElMessage.error(error.message || '加载运营大屏失败')
  } finally {
    loading.value = false
  }
}

function resizeCharts() {
  onlineChart?.resize()
  registrationChart?.resize()
}

onMounted(() => {
  loadScreenData()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  onlineChart?.dispose()
  registrationChart?.dispose()
})
</script>

<style scoped>
.visual-screen {
  min-height: calc(100vh - 132px);
  padding: 22px;
  color: #dbeafe;
  background:
    radial-gradient(circle at 16% 14%, rgba(34, 211, 238, 0.14), transparent 26%),
    linear-gradient(135deg, #06162f 0%, #09254f 52%, #0d3f73 100%);
}

.screen-header,
.screen-panel,
.metric-tile,
.tenant-card {
  border: 1px solid rgba(125, 211, 252, 0.26);
  border-radius: 8px;
  background: rgba(6, 24, 52, 0.74);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08), 0 18px 36px rgba(2, 8, 23, 0.22);
}

.screen-header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding: 18px 20px;
}

.eyebrow {
  color: #67e8f9;
  font-size: 12px;
  font-weight: 900;
}

.screen-header h1 {
  margin: 6px 0;
  font-size: 30px;
  letter-spacing: 0;
}

.screen-header p,
.screen-actions span,
.panel-title span,
.metric-tile span,
.metric-tile small,
.tenant-card small,
.tenant-stats span,
.data-row span,
.data-row small,
.online-row span,
.online-row small {
  color: #9ec5ff;
}

.screen-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
  margin-top: 14px;
}

.metric-tile {
  display: grid;
  gap: 6px;
  padding: 16px;
}

.metric-tile strong {
  color: #fff;
  font-size: 26px;
}

.screen-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr);
  gap: 14px;
  margin-top: 14px;
}

.screen-panel {
  min-height: 280px;
  padding: 16px;
}

.screen-panel.wide {
  grid-row: span 2;
}

.panel-title {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.chart-box {
  height: 230px;
}

.tenant-grid,
.content-list,
.online-list {
  display: grid;
  gap: 10px;
}

.tenant-card {
  padding: 12px;
}

.tenant-head,
.online-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.tenant-head > div,
.online-row > div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.tenant-head strong,
.online-row strong {
  overflow: hidden;
  color: #fff;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tenant-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;
}

.tenant-stats span,
.online-row {
  padding: 8px;
  border-radius: 6px;
  background: rgba(8, 47, 94, 0.52);
}

.data-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) max-content;
  gap: 4px 12px;
  padding: 12px;
  border: 1px solid rgba(125, 211, 252, 0.18);
  border-radius: 8px;
  background: rgba(8, 47, 94, 0.42);
}

.data-row strong {
  color: #67e8f9;
  font-size: 20px;
}

.data-row small {
  grid-column: 1 / -1;
}

:deep(.el-empty__description p) {
  color: #9ec5ff;
}

@media (max-width: 1180px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .screen-grid {
    grid-template-columns: 1fr;
  }

  .screen-panel.wide {
    grid-row: span 1;
  }
}

@media (max-width: 760px) {
  .screen-header,
  .screen-actions,
  .tenant-head,
  .online-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .metric-grid,
  .tenant-stats {
    grid-template-columns: 1fr;
  }
}
</style>
