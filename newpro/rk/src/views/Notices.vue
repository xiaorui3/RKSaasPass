<template>
  <div class="notices-page design-notice-reference-page" data-testid="portal-notices-page">
    <section class="notice-hero design-campus-hero">
      <div>
        <p class="hero-eyebrow">公告概况</p>
        <h1>公告通知</h1>
        <p>集中查看社团公告、活动通知和比赛安排，重要事项优先展示。</p>
      </div>
      <div class="hero-stats">
        <article>
          <strong>{{ notices.length }}</strong>
          <span>公告总数</span>
        </article>
        <article>
          <strong>{{ topNoticeCount }}</strong>
          <span>置顶公告</span>
        </article>
        <article>
          <strong>{{ attachmentCount }}</strong>
          <span>附件资源</span>
        </article>
      </div>
    </section>

    <main class="container notice-shell">
      <aside class="notice-rail design-notice-sidebar">
        <div class="rail-card">
          <p class="rail-eyebrow">公告分类</p>
          <button
            v-for="item in noticeTypeStats"
            :key="item.label"
            type="button"
            class="rail-row"
          >
            <span>{{ item.label }}</span>
            <strong>{{ item.count }}</strong>
          </button>
        </div>
        <div class="rail-card soft design-detail-sidebar">
          <p class="rail-eyebrow">阅读提示</p>
          <h3>优先处理置顶和带附件的通知</h3>
          <p>列表继续读取现有公告接口，封面、附件和发布时间保持原有数据来源。</p>
        </div>
      </aside>

      <section class="notice-board" data-testid="portal-notices-board">
        <div class="board-header">
          <div>
            <p class="board-eyebrow">公告列表</p>
            <h2>最新公告</h2>
          </div>
          <span>{{ loading ? '正在同步' : `已加载 ${notices.length} 条` }}</span>
        </div>

        <div v-if="loading" class="loading-block">
          <el-icon class="loading-icon"><Loading /></el-icon>
          <p>公告加载中...</p>
        </div>

        <el-empty v-else-if="notices.length === 0" description="暂无公告通知" />

        <div v-else class="notice-list">
          <article v-for="notice in notices" :key="notice.id" class="notice-card design-notice-row">
            <img
              v-if="showNoticeCover(notice)"
              :src="resolveMediaUrl(notice.coverImage)"
              :alt="notice.title"
              class="notice-cover-image"
              @error="handleCoverImageError(notice)"
            />
            <div class="notice-card__body">
              <div class="notice-card__head">
                <div>
                  <div class="notice-tags">
                    <el-tag size="small" type="primary">{{ getNoticeTypeText(notice.noticeType) }}</el-tag>
                    <el-tag v-if="notice.isTop === 1" size="small" type="danger">置顶</el-tag>
                  </div>
                  <h3>{{ notice.title }}</h3>
                  <p class="notice-meta">
                    <span>{{ formatDateTime(notice.publishTime || notice.createTime) }}</span>
                  </p>
                </div>
              </div>
              <p class="notice-content">{{ notice.content }}</p>
              <a
                v-if="notice.attachmentUrl"
                :href="resolveMediaUrl(notice.attachmentUrl)"
                target="_blank"
                rel="noopener noreferrer"
                class="notice-attachment-link"
              >
                下载附件
              </a>
            </div>
          </article>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getPublishedNoticeList } from '@/api/notice'
import { resolveMediaUrl } from '@/utils/mediaUrl'

const loading = ref(false)
const notices = ref([])
const brokenCoverNoticeIds = ref(new Set())

const topNoticeCount = computed(() => notices.value.filter((notice) => notice.isTop === 1).length)
const attachmentCount = computed(() => notices.value.filter((notice) => notice.attachmentUrl).length)
const noticeTypeStats = computed(() => {
  const types = [1, 2, 3, 4]
  return types.map((type) => ({
    label: getNoticeTypeText(type),
    count: notices.value.filter((notice) => Number(notice.noticeType || 1) === type).length
  }))
})

function getNoticeTypeText(type) {
  const map = {
    1: '系统公告',
    2: '活动通知',
    3: '比赛通知',
    4: '其他通知'
  }
  return map[type] || '公告'
}

function formatDateTime(value) {
  if (!value) return '时间待定'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  const pad = (num) => String(num).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

async function fetchNotices() {
  loading.value = true
  try {
    const res = await getPublishedNoticeList()
    brokenCoverNoticeIds.value = new Set()
    notices.value = res.code === 200 ? (res.data || []) : []
  } catch (error) {
    console.error('获取公告通知失败:', error)
    ElMessage.error(error.message || '获取公告通知失败')
    notices.value = []
  } finally {
    loading.value = false
  }
}

function showNoticeCover(notice) {
  return Boolean(notice?.coverImage) && !brokenCoverNoticeIds.value.has(notice.id)
}

function handleCoverImageError(notice) {
  if (!notice?.id) {
    return
  }
  const next = new Set(brokenCoverNoticeIds.value)
  next.add(notice.id)
  brokenCoverNoticeIds.value = next
}

onMounted(() => {
  fetchNotices()
})
</script>

<style lang="scss" scoped>
@use '../styles/variables.scss' as *;

.notices-page {
  min-height: 100vh;
  padding-bottom: 64px;
  background:
    linear-gradient(180deg, rgba(47, 111, 237, 0.06) 0%, rgba(247, 250, 255, 0.92) 28%, #fff 100%);
}

.notice-hero {
  max-width: 1200px;
  margin: 28px auto 0;
  padding: 52px 64px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(300px, 0.65fr);
  gap: 32px;
  align-items: end;
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
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  opacity: 0.82;
}

.notice-hero h1 {
  margin: 0;
  font-size: clamp(34px, 4vw, 56px);
  line-height: 1.08;
}

.notice-hero p {
  max-width: 640px;
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
  min-height: 100px;
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

.notice-shell {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 24px;
  padding-top: 26px;
}

.notice-rail,
.notice-board {
  min-width: 0;
}

.notice-rail {
  display: grid;
  gap: 18px;
  align-content: start;
}

.rail-card,
.notice-board {
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
  margin: 0 0 12px;
  color: #2f6fed;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.14em;
}

.rail-card h3 {
  margin: 0 0 10px;
  font-size: 18px;
  line-height: 1.35;
}

.rail-card p {
  margin: 0;
  color: $text-secondary;
  line-height: 1.75;
}

.rail-row {
  width: 100%;
  min-height: 44px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 0;
  border-radius: 12px;
  background: transparent;
  color: $text-primary;
  cursor: default;
}

.rail-row + .rail-row {
  margin-top: 6px;
}

.rail-row:first-of-type,
.rail-row:hover {
  background: #eff6ff;
}

.rail-row strong {
  color: #2f6fed;
}

.notice-board {
  padding: 24px;
}

.board-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.board-header h2 {
  margin: 0;
  font-size: 26px;
}

.board-header span {
  color: $text-secondary;
  font-size: 13px;
}

.loading-block {
  padding: 80px 0;
  text-align: center;
  color: $text-secondary;
}

.loading-icon {
  font-size: 36px;
  color: $primary-color;
}

.notice-list {
  display: grid;
  gap: 16px;
}

.notice-card {
  display: grid;
  grid-template-columns: minmax(200px, 280px) minmax(0, 1fr);
  gap: 22px;
  padding: 18px;
  border-radius: 20px;
  background: #fff;
  border: 1px solid rgba(226, 232, 240, 0.88);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.notice-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 32px rgba(31, 58, 120, 0.1);
}

.notice-cover-image {
  width: 100%;
  height: 168px;
  object-fit: cover;
  border-radius: 16px;
}

.notice-card__body {
  min-width: 0;
  display: grid;
  align-content: center;
}

.notice-card__head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;

  h3 {
    margin: 0 0 8px;
    font-size: 20px;
    line-height: 1.35;
    color: $text-primary;
  }
}

.notice-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.notice-meta {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin: 0;
  font-size: 13px;
  color: $text-secondary;
}

.notice-content {
  margin: 8px 0 0;
  line-height: 1.75;
  color: $text-primary;
  white-space: pre-wrap;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notice-attachment-link {
  display: inline-flex;
  margin-top: 12px;
  color: $primary-color;
  text-decoration: none;
  font-weight: 600;
}

.notice-attachment-link:hover {
  text-decoration: underline;
}

@media (max-width: $breakpoint-md) {
  .notice-hero {
    margin: 18px 16px 0;
    padding: 32px 24px;
    grid-template-columns: 1fr;
  }

  .hero-stats,
  .notice-shell {
    grid-template-columns: 1fr;
  }

  .notice-card {
    grid-template-columns: 1fr;
  }

  .notice-cover-image {
    height: 220px;
  }
}

@media (max-width: 640px) {
  .notice-hero {
    padding: 24px 18px;
    border-radius: 20px;
  }

  .notice-hero h1 {
    font-size: 28px;
  }

  .hero-stats {
    grid-template-columns: 1fr;
  }

  .notice-board {
    padding: 18px;
  }

  .loading-block {
    padding: 44px 0;
  }

  .loading-icon {
    font-size: 30px;
  }

  .notice-cover-image {
    height: 168px;
  }

  .notice-card__head {
    flex-direction: column;
    gap: 8px;

    h3 {
      font-size: 16px;
    }
  }

  .notice-meta {
    gap: 8px;
    font-size: 12px;
  }

  .notice-content {
    margin-top: 12px;
    font-size: 14px;
    line-height: 1.68;
  }

  .notice-attachment-link {
    margin-top: 10px;
    font-size: 13px;
  }
}
</style>
