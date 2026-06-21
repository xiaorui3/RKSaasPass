<template>
  <div class="history-page design-history-reference-page" data-testid="portal-history-page">
    <section class="history-hero design-history-hero">
      <div class="container history-hero__inner">
        <div class="hero-copy">
          <p class="hero-eyebrow">时间轴</p>
          <h1>社团历程</h1>
          <p>
            汇总社团发展节点、活动成果、获奖记录和组织变化，按年份形成清晰的成长轨迹。
          </p>
        </div>

        <div class="hero-stats">
          <article>
            <strong>{{ events.length }}</strong>
            <span>历程节点</span>
          </article>
          <article>
            <strong>{{ timelineData.length }}</strong>
            <span>覆盖年份</span>
          </article>
          <article>
            <strong>{{ stats.totalCount || stats.eventCount || events.length }}</strong>
            <span>统计记录</span>
          </article>
        </div>
      </div>
    </section>

    <main class="container history-shell">
      <aside class="history-rail">
        <section class="rail-card">
          <p class="rail-eyebrow">检索</p>
          <h2>查找历程节点</h2>
          <p>输入年份、标题或描述关键词，继续使用现有搜索接口。</p>
          <div class="timeline-controls">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索历史事件"
              clearable
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
          </div>
        </section>

        <section class="rail-card soft">
          <p class="rail-eyebrow">展示说明</p>
          <h2>按年份归档</h2>
          <p>如果公开历程接口暂无数据，会读取社团概况配置中的历程字段作为兜底展示。</p>
        </section>
      </aside>

      <section class="timeline-board">
        <div class="board-header">
          <div>
            <p class="board-eyebrow">历程目录</p>
            <h2>社团发展时间轴</h2>
            <span>{{ loading ? '正在加载历程数据' : `已加载 ${events.length} 个节点` }}</span>
          </div>
          <el-button text @click="handleReset">查看全部</el-button>
        </div>

        <div v-if="loading" class="loading-tip">
          <el-skeleton :rows="5" animated />
        </div>

        <div v-else-if="timelineData.length === 0" class="empty-tip">
          <el-empty description="暂无社团历程记录" />
        </div>

        <div v-else class="timeline design-history-timeline">
          <section v-for="group in timelineData" :key="group.year" class="timeline-year-group">
            <div class="year-marker">
              <span class="year-text">{{ group.year }}</span>
              <strong>{{ group.events.length }} 项</strong>
            </div>

            <div class="timeline-items">
              <article v-for="event in group.events" :key="event.id" class="timeline-item">
                <div class="timeline-dot"></div>
                <div class="timeline-content">
                  <div class="event-top">
                    <span class="event-date">{{ event.eventDate || group.year }}</span>
                    <el-tag v-if="event.eventType" size="small" type="primary">
                      {{ getEventTypeText(event.eventType) }}
                    </el-tag>
                  </div>
                  <h3 class="event-title">{{ event.title }}</h3>
                  <p class="event-desc">{{ event.description }}</p>
                </div>
              </article>
            </div>
          </section>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { getTimeline, searchEvents, getHistoryStatistics } from '@/api/history'
import { getPublicClubProfileConfig } from '@/api/club-profile'

const loading = ref(false)
const searchKeyword = ref('')
const events = ref([])
const stats = ref({})
const profileHistoryFallback = ref([])
const route = useRoute()

const EVENT_TYPES = {
  1: '里程碑',
  2: '活动',
  3: '获奖',
  4: '组织变更',
  5: '其他',
  PROFILE: '概况'
}
function getEventTypeText(type) { return EVENT_TYPES[type] || '其他' }

const timelineData = computed(() => {
  const groups = {}
  for (const e of events.value) {
    const year = (e.eventDate || e.createTime || e.year || '').toString().substring(0, 4) || '未知'
    if (!groups[year]) groups[year] = { year, events: [] }
    groups[year].events.push(e)
  }
  return Object.values(groups).sort((a, b) => b.year.localeCompare(a.year))
})

function normalizeHistoryEvents(data) {
  const records = Array.isArray(data) ? data : (data?.records || data?.list || [])
  return (Array.isArray(records) ? records : []).map((item, index) => {
    const eventDate = item.eventDate || item.date || item.createTime || (item.year ? `${item.year}-01-01` : '')
    const year = item.year || String(eventDate || '').substring(0, 4) || '未知'
    return {
      ...item,
      id: item.id || `history-${year}-${index}`,
      year,
      eventDate,
      title: item.title || '未命名历程',
      description: item.description || item.content || '',
      eventType: item.eventType || item.type
    }
  }).filter((item) => item.title || item.description)
}

async function loadProfileHistoryFallback() {
  if (profileHistoryFallback.value.length) {
    return profileHistoryFallback.value
  }
  try {
    const tenantId = route.query.tenantId || localStorage.getItem('tenantId') || 1
    const res = await getPublicClubProfileConfig(tenantId)
    const history = Array.isArray(res.data?.history) ? res.data.history : []
    profileHistoryFallback.value = history.map((item, index) => {
      const year = item.year || String(item.date || '').substring(0, 4) || '未知'
      return {
        id: `profile-history-${year}-${index}`,
        year,
        eventDate: item.date || (year !== '未知' ? `${year}-01-01` : ''),
        title: item.title || '社团历程',
        description: item.description || '',
        eventType: 'PROFILE'
      }
    }).filter((item) => item.title || item.description)
    return profileHistoryFallback.value
  } catch (error) {
    console.error('获取社团概况历程兜底失败:', error)
    profileHistoryFallback.value = []
    return []
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await getTimeline()
    const normalized = normalizeHistoryEvents(res.data)
    events.value = normalized.length ? normalized : await loadProfileHistoryFallback()
  } catch {
    events.value = await loadProfileHistoryFallback()
  }
  loading.value = false
}

async function handleSearch() {
  if (!searchKeyword.value.trim()) {
    loadData()
    return
  }
  loading.value = true
  try {
    const res = await searchEvents(searchKeyword.value.trim())
    const normalized = normalizeHistoryEvents(res.data)
    if (normalized.length) {
      events.value = normalized
    } else {
      const keyword = searchKeyword.value.trim().toLowerCase()
      const fallback = await loadProfileHistoryFallback()
      events.value = fallback.filter((item) =>
        `${item.year} ${item.title} ${item.description}`.toLowerCase().includes(keyword)
      )
    }
  } catch {
    const keyword = searchKeyword.value.trim().toLowerCase()
    const fallback = await loadProfileHistoryFallback()
    events.value = fallback.filter((item) =>
      `${item.year} ${item.title} ${item.description}`.toLowerCase().includes(keyword)
    )
  }
  loading.value = false
}

function handleReset() {
  searchKeyword.value = ''
  loadData()
}

onMounted(() => {
  loadData()
  getHistoryStatistics().then(r => { stats.value = r.data || {} }).catch(() => {})
})
</script>

<style lang="scss" scoped>
@use '../styles/variables.scss' as *;

.history-page {
  min-height: 100vh;
  padding-bottom: 64px;
  background:
    linear-gradient(180deg, rgba(47, 111, 237, 0.06) 0%, rgba(247, 250, 255, 0.92) 28%, #fff 100%);
}

.history-hero {
  padding: 28px 0 0;
}

.history-hero__inner {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 0.7fr);
  gap: 32px;
  align-items: end;
  padding: 52px 64px;
  border-radius: 28px;
  color: #fff;
  background:
    radial-gradient(circle at 76% 26%, rgba(255, 255, 255, 0.28), transparent 22%),
    linear-gradient(135deg, #3567ff 0%, #1d86ff 100%);
  box-shadow: 0 24px 60px rgba(39, 105, 255, 0.22);
}

.hero-eyebrow {
  margin: 0 0 12px;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  opacity: 0.82;
}

.hero-copy h1 {
  margin: 0;
  font-size: clamp(34px, 4vw, 56px);
  line-height: 1.08;
}

.hero-copy p:not(.hero-eyebrow) {
  max-width: 680px;
  margin: 18px 0 0;
  font-size: 17px;
  line-height: 1.8;
  opacity: 0.86;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.hero-stats article {
  min-height: 108px;
  padding: 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.hero-stats strong,
.hero-stats span {
  display: block;
}

.hero-stats strong {
  font-size: 30px;
  line-height: 1;
}

.hero-stats span {
  margin-top: 10px;
  font-size: 13px;
  opacity: 0.78;
}

.history-shell {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 24px;
  padding-top: 26px;
}

.history-rail {
  display: grid;
  gap: 18px;
  align-content: start;
}

.rail-card,
.timeline-board {
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 18px 44px rgba(31, 58, 120, 0.08);
}

.rail-card {
  padding: 20px;
}

.rail-card.soft {
  background: linear-gradient(180deg, #f8fbff 0%, #fff 100%);
}

.rail-eyebrow,
.board-eyebrow {
  margin: 0 0 10px;
  color: #2f6fed;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.14em;
}

.rail-card h2,
.board-header h2 {
  margin: 0;
  color: $text-primary;
  font-size: 24px;
  line-height: 1.25;
}

.rail-card p:last-child {
  margin: 12px 0 0;
  color: $text-secondary;
  line-height: 1.75;
}

.timeline-controls {
  display: grid;
  gap: 10px;
  margin-top: 18px;
}

.timeline-board {
  padding: 24px;
}

.board-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
  margin-bottom: 24px;
}

.board-header span {
  display: block;
  margin-top: 8px;
  color: $text-secondary;
}

.loading-tip,
.empty-tip {
  padding: 60px 0;
}

.timeline {
  display: grid;
  gap: 28px;
}

.timeline-year-group {
  display: grid;
  grid-template-columns: 130px minmax(0, 1fr);
  gap: 22px;
}

.year-marker {
  position: sticky;
  top: 88px;
  align-self: start;
  display: grid;
  gap: 8px;
}

.year-text {
  display: inline-flex;
  width: fit-content;
  padding: 8px 16px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2f6fed;
  font-size: 22px;
  font-weight: 800;
}

.year-marker strong {
  color: $text-secondary;
  font-size: 13px;
}

.timeline-items {
  position: relative;
  display: grid;
  gap: 18px;
  padding-left: 28px;
}

.timeline-items::before {
  content: '';
  position: absolute;
  left: 5px;
  top: 8px;
  bottom: 8px;
  width: 2px;
  background: #dbeafe;
}

.timeline-item {
  position: relative;
}

.timeline-dot {
  position: absolute;
  left: -28px;
  top: 22px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #2f6fed;
  border: 3px solid #fff;
  box-shadow: 0 0 0 3px #dbeafe;
}

.timeline-content {
  padding: 20px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid rgba(226, 232, 240, 0.88);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.timeline-content:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 28px rgba(31, 58, 120, 0.08);
}

.event-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
}

.event-date {
  color: $text-secondary;
  font-size: 13px;
}

.event-title {
  margin: 0 0 10px;
  color: $text-primary;
  font-size: 19px;
  line-height: 1.35;
}

.event-desc {
  margin: 0;
  color: $text-secondary;
  font-size: 14px;
  line-height: 1.75;
}

@media (max-width: $breakpoint-lg) {
  .history-hero__inner,
  .history-shell {
    grid-template-columns: 1fr;
  }
}

@media (max-width: $breakpoint-md) {
  .history-hero__inner {
    margin: 0 16px;
    padding: 32px 24px;
  }

  .hero-stats,
  .timeline-year-group {
    grid-template-columns: 1fr;
  }

  .year-marker {
    position: static;
  }
}

@media (max-width: 640px) {
  .history-page {
    padding-bottom: 36px;
  }

  .history-hero {
    padding-top: 18px;
  }

  .history-hero__inner {
    margin: 0 12px;
    padding: 24px 18px;
    border-radius: 20px;
  }

  .hero-copy h1 {
    font-size: 28px;
  }

  .hero-copy p:not(.hero-eyebrow) {
    font-size: 14px;
    line-height: 1.68;
  }

  .history-shell {
    gap: 16px;
    padding-top: 18px;
  }

  .rail-card,
  .timeline-board {
    padding: 18px;
    border-radius: 18px;
  }

  .board-header {
    flex-direction: column;
  }

  .timeline {
    gap: 22px;
  }

  .timeline-items {
    gap: 14px;
    padding-left: 22px;
  }

  .timeline-dot {
    left: -24px;
  }

  .timeline-content {
    padding: 16px;
    border-radius: 16px;
  }
}
</style>
