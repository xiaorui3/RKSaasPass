<template>
  <div class="portal-hub">
    <section class="portal-hub-hero">
      <div>
        <span class="portal-hub-kicker">Content Hub</span>
        <h1>内容聚合</h1>
        <p>集中查看新闻、公告、作品和社团历程，原新闻、公告、作品详情页入口全部保留。</p>
      </div>
      <router-link v-if="portalSettings.allowPublicSearch !== false" class="portal-hub-action" to="/search">全局搜索</router-link>
    </section>

    <section class="portal-hub-grid">
      <article v-if="portalSettings.showNews !== false" class="portal-hub-card">
        <div class="portal-hub-card-head">
          <h2>最新新闻</h2>
          <router-link to="/news">更多</router-link>
        </div>
        <div v-if="loading.news" class="portal-hub-empty">正在加载</div>
        <router-link v-for="item in news" :key="`news-${item.id}`" class="portal-hub-row" :to="`/news/${item.id}`">
          <strong>{{ item.title || item.newsTitle || '未命名新闻' }}</strong>
          <span>{{ formatDate(item.publishTime || item.createTime) }}</span>
        </router-link>
        <div v-if="!loading.news && news.length === 0" class="portal-hub-empty">暂无新闻</div>
      </article>

      <article class="portal-hub-card">
        <div class="portal-hub-card-head">
          <h2>公告通知</h2>
          <router-link to="/notices">更多</router-link>
        </div>
        <div v-if="loading.notices" class="portal-hub-empty">正在加载</div>
        <router-link v-for="item in notices" :key="`notice-${item.id}`" class="portal-hub-row" to="/notices">
          <strong>{{ item.title || item.noticeTitle || '未命名公告' }}</strong>
          <span>{{ formatDate(item.publishTime || item.createTime) }}</span>
        </router-link>
        <div v-if="!loading.notices && notices.length === 0" class="portal-hub-empty">暂无公告</div>
      </article>

      <article v-if="portalSettings.showWorks !== false" class="portal-hub-card">
        <div class="portal-hub-card-head">
          <h2>作品展示</h2>
          <router-link to="/works">更多</router-link>
        </div>
        <div v-if="loading.works" class="portal-hub-empty">正在加载</div>
        <router-link v-for="item in works" :key="`work-${item.id}`" class="portal-hub-row" :to="`/works/${item.id}`">
          <strong>{{ item.title || item.workTitle || '未命名作品' }}</strong>
          <span>{{ item.category || item.author || '作品' }}</span>
        </router-link>
        <div v-if="!loading.works && works.length === 0" class="portal-hub-empty">暂无作品</div>
      </article>

      <article class="portal-hub-card">
        <div class="portal-hub-card-head">
          <h2>社团历程</h2>
          <router-link to="/history">更多</router-link>
        </div>
        <div v-if="loading.history" class="portal-hub-empty">正在加载</div>
        <router-link v-for="item in history" :key="`history-${item.id}`" class="portal-hub-row" to="/history">
          <strong>{{ item.title || item.eventTitle || '社团事件' }}</strong>
          <span>{{ formatDate(item.eventDate || item.createTime) }}</span>
        </router-link>
        <div v-if="!loading.history && history.length === 0" class="portal-hub-empty">暂无历程</div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getNewsList } from '@/api/news'
import { getPublishedNoticeList } from '@/api/notice'
import { getFeaturedWorks } from '@/api/works'
import { getImportantEvents } from '@/api/history'
import { useTenantSelfServiceStore } from '@/stores/tenantSelfService'

const tenantSelfServiceStore = useTenantSelfServiceStore()
const portalSettings = computed(() => tenantSelfServiceStore.portalSettings)
const news = ref([])
const notices = ref([])
const works = ref([])
const history = ref([])
const loading = reactive({
  news: true,
  notices: true,
  works: true,
  history: true
})

function pickRecords(data) {
  if (Array.isArray(data)) return data
  return data?.records || data?.list || data?.rows || []
}

function formatDate(value) {
  if (!value) return '最近更新'
  return String(value).slice(0, 10)
}

async function loadSection(key, request, target, limit = 6) {
  loading[key] = true
  try {
    const res = await request()
    target.value = pickRecords(res.data).slice(0, limit)
  } catch {
    target.value = []
  } finally {
    loading[key] = false
  }
}

onMounted(async () => {
  await tenantSelfServiceStore.loadPublicConfig()
  if (portalSettings.value.showNews !== false) {
    loadSection('news', () => getNewsList({ page: 1, pageSize: 6, size: 6 }), news)
  } else {
    loading.news = false
  }
  loadSection('notices', getPublishedNoticeList, notices)
  if (portalSettings.value.showWorks !== false) {
    loadSection('works', getFeaturedWorks, works)
  } else {
    loading.works = false
  }
  loadSection('history', getImportantEvents, history)
})
</script>

<style lang="scss" scoped>
.portal-hub {
  display: grid;
  gap: 22px;
}

.portal-hub-hero,
.portal-hub-card {
  border: 1px solid rgba(194, 214, 242, 0.86);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 14px 36px rgba(46, 92, 150, 0.08);
}

.portal-hub-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 28px;

  h1 {
    margin: 6px 0 10px;
    color: #10284f;
    font-size: 30px;
  }

  p {
    margin: 0;
    color: #62738f;
  }
}

.portal-hub-kicker {
  color: #1473ff;
  font-size: 13px;
  font-weight: 900;
}

.portal-hub-action {
  flex: 0 0 auto;
  padding: 12px 18px;
  border-radius: 8px;
  background: #1473ff;
  color: #fff;
  font-weight: 900;
  text-decoration: none;
}

.portal-hub-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.portal-hub-card {
  min-height: 280px;
  padding: 20px;
}

.portal-hub-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;

  h2 {
    margin: 0;
    color: #162c52;
    font-size: 18px;
  }

  a {
    color: #1473ff;
    font-weight: 800;
    text-decoration: none;
  }
}

.portal-hub-row {
  display: grid;
  gap: 4px;
  padding: 12px 0;
  border-top: 1px solid #edf2fa;
  color: #213857;
  text-decoration: none;

  strong {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  span {
    color: #8492a8;
    font-size: 13px;
  }
}

.portal-hub-empty {
  padding: 18px 0;
  color: #8492a8;
}

@media (max-width: 760px) {
  .portal-hub-hero {
    display: grid;
    padding: 22px;
  }

  .portal-hub-grid {
    grid-template-columns: 1fr;
  }
}
</style>
