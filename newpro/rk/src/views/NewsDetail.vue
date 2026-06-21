<template>
  <div class="news-detail-page design-detail-reference-page">
    <div class="container detail-shell">
      <div v-if="loading" class="loading-container">
        <el-icon class="loading-icon"><Loading /></el-icon>
        <p>正在加载新闻详情...</p>
      </div>

      <div v-else-if="news" class="detail-stack">
        <div class="back-bar design-breadcrumb">
          <el-button text @click="handleBack">
            <el-icon><ArrowLeft /></el-icon>
            返回新闻列表
          </el-button>
        </div>

        <PortalContentFeatureCard
          eyebrow="新闻概况"
          :title="news.title"
          :description="summaryText"
          media-position="end"
        >
          <template #meta>
            <el-tag v-if="news.category" size="small" type="primary">{{ news.category }}</el-tag>
            <el-tag v-if="news.isFeatured === 1" size="small" type="warning">置顶</el-tag>
            <el-tag v-if="news.isCrossTenant === 1" size="small" type="success">共享</el-tag>
            <span class="headline-meta">
              <el-icon><Calendar /></el-icon>
              {{ formatDateTime(news.publishTime || news.createTime) }}
            </span>
          </template>

          <div class="headline-copy">
            <p class="headline-lead">
              {{ summaryText }}
            </p>

            <div class="headline-stats">
              <span>
                <el-icon><User /></el-icon>
                {{ news.author || '新闻中心' }}
              </span>
              <span>
                <el-icon><View /></el-icon>
                浏览 {{ news.viewCount || 0 }}
              </span>
              <span>
                <el-icon><Clock /></el-icon>
                {{ readingTime }}
              </span>
            </div>
          </div>

          <template #actions>
            <el-button @click="handleBack">返回列表</el-button>
            <el-button v-if="news.attachmentUrl" type="primary" :href="resolveMediaUrl(news.attachmentUrl)" tag="a" target="_blank">
              下载附件
            </el-button>
          </template>

          <template #media>
            <el-image
              v-if="news.coverImage"
              :src="resolveMediaUrl(news.coverImage)"
              fit="cover"
              class="headline-image"
              :preview-src-list="[resolveMediaUrl(news.coverImage)]"
            >
              <template #error>
                <div class="headline-placeholder">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
            <div v-else class="headline-placeholder">
              <el-icon><Memo /></el-icon>
            </div>
          </template>

          <template #aside>
            <div class="aside-brief">
              <p class="aside-label">阅读提示</p>
              <h3>门户阅读布局</h3>
              <p>
                文章主体保持居中阅读，参考信息、相关推荐和附件资源则集中在右侧栏呈现。
              </p>
            </div>
          </template>
        </PortalContentFeatureCard>

        <div class="reading-layout design-article-shell">
          <article class="reading-column">
            <section class="reading-panel">
              <div class="reading-head">
                <p class="reading-eyebrow">正文内容</p>
                <h2>全文阅读</h2>
              </div>

              <div v-if="news.content" class="content-body" :class="{ plain: !hasRichContent }" v-html="formattedContent"></div>
              <div v-else-if="news.summary" class="summary-panel">
                {{ news.summary }}
              </div>
              <el-empty v-else description="当前新闻暂未填写正文内容。" />
            </section>

            <section v-if="news.videoUrl" class="media-panel">
              <div class="reading-head">
                <p class="reading-eyebrow">媒体内容</p>
                <h2>内嵌视频</h2>
              </div>
              <video :src="resolveMediaUrl(news.videoUrl)" controls class="detail-video"></video>
            </section>
          </article>

          <aside class="detail-rail design-related-sidebar">
            <PortalContentInfoCard
              eyebrow="发布信息"
              title="核心元数据"
              description="阅读新闻时仍可直接查看关键发布信息。"
              tone="soft"
            >
              <div class="info-list">
                <div v-for="item in infoItems" :key="item.label" class="info-row">
                  <span>{{ item.label }}</span>
                  <strong>{{ item.value }}</strong>
                </div>
              </div>
            </PortalContentInfoCard>

            <PortalContentInfoCard
              v-if="parseTags(news.tags).length"
              eyebrow="标签信息"
              title="新闻标签"
              description="新闻标签保持与现有文章数据一致。"
              tone="light"
            >
              <div class="tag-list">
                <el-tag v-for="tag in parseTags(news.tags)" :key="tag" type="info">
                  {{ tag }}
                </el-tag>
              </div>
            </PortalContentInfoCard>

            <PortalContentInfoCard
              v-if="news.attachmentUrl"
              eyebrow="附件资源"
              title="附件下载"
              description="原始新闻记录中的附件资源仍可在此直接下载。"
              tone="light"
            >
              <a :href="resolveMediaUrl(news.attachmentUrl)" target="_blank" rel="noopener noreferrer" class="asset-link">
                <el-icon><Paperclip /></el-icon>
                打开附件
              </a>
            </PortalContentInfoCard>

            <PortalContentInfoCard
              v-if="relatedNews.length"
              eyebrow="继续阅读"
              title="相关新闻"
              description="相关推荐继续通过最新新闻接口获取。"
              tone="contrast"
            >
              <button
                v-for="item in relatedNews"
                :key="item.id"
                type="button"
                class="related-link"
                @click="handleNewsClick(item.id)"
              >
                <span class="related-title">{{ item.title }}</span>
                <span class="related-meta">{{ formatDate(item.publishTime || item.createTime) }}</span>
              </button>
            </PortalContentInfoCard>
          </aside>
        </div>

        <div class="footer-actions">
          <el-button @click="handleBack">
            <el-icon><ArrowLeft /></el-icon>
            返回列表
          </el-button>
        </div>

        <div class="container">
          <NewsComments target-type="news" :target-id="newsId" />
        </div>
      </div>

      <div v-else class="error-container">
        <el-empty description="未找到对应的新闻内容。">
          <el-button type="primary" @click="handleBack">返回新闻列表</el-button>
        </el-empty>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  Calendar,
  Clock,
  Document,
  Download,
  Link,
  Loading,
  Memo,
  Paperclip,
  Picture,
  Reading,
  User,
  VideoCamera,
  View
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PortalContentFeatureCard from '@/components/portal-content/PortalContentFeatureCard.vue'
import PortalContentInfoCard from '@/components/portal-content/PortalContentInfoCard.vue'
import { getLatestNews, getNewsDetail } from '@/api/news'
import NewsComments from '@/components/NewsComments.vue'
import { resolveMediaUrl } from '@/utils/mediaUrl'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const news = ref(null)
const relatedNews = ref([])

const newsId = computed(() => route.params.id)

function stripHtml(value) {
  return String(value || '').replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim()
}

function formatDateTime(value) {
  if (!value) {
    return '待发布'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return String(value)
  }

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

function formatDate(value) {
  if (!value) {
    return '待定'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return String(value)
  }

  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${month}-${day}`
}

function parseTags(tagsStr) {
  if (!tagsStr) {
    return []
  }
  return String(tagsStr)
    .split(',')
    .map((tag) => tag.trim())
    .filter(Boolean)
}

const hasRichContent = computed(() => /<[^>]+>/.test(news.value?.content || ''))
const readingTime = computed(() => {
  const source = stripHtml(news.value?.content || news.value?.summary || '')
  if (!source) {
    return '约 1 分钟'
  }
  const estimate = Math.max(1, Math.ceil(source.length / 420))
  return `约 ${estimate} 分钟`
})

const summaryText = computed(() => {
  return stripHtml(news.value?.summary || news.value?.content || '') || '当前新闻暂无摘要。'
})

const formattedContent = computed(() => {
  if (!news.value?.content) {
    return ''
  }
  if (hasRichContent.value) {
    return news.value.content
  }
  return stripHtml(news.value.content)
})

const infoItems = computed(() => {
  if (!news.value) {
    return []
  }
  return [
    { label: '作者', value: news.value.author || '新闻中心' },
    { label: '分类', value: news.value.category || '综合资讯' },
    { label: '发布时间', value: formatDateTime(news.value.publishTime) },
    { label: '创建时间', value: formatDateTime(news.value.createTime) },
    { label: '阅读时长', value: readingTime.value }
  ]
})

async function fetchRelatedNews() {
  try {
    const res = await getLatestNews(5)
    relatedNews.value = normalizeRelated(res.data)
      .filter((item) => String(item.id) !== String(newsId.value))
      .slice(0, 4)
  } catch (error) {
    console.error('Failed to fetch related news:', error)
    relatedNews.value = []
  }
}

function normalizeRelated(payload) {
  if (Array.isArray(payload?.records)) {
    return payload.records
  }
  if (Array.isArray(payload)) {
    return payload
  }
  return []
}

async function fetchNewsDetail() {
  if (!newsId.value) {
    ElMessage.error('新闻 ID 无效')
    return
  }

  loading.value = true
  try {
    const res = await getNewsDetail(newsId.value)
    if (res.data) {
      news.value = res.data
      await fetchRelatedNews()
    } else {
      news.value = null
      ElMessage.error('未找到对应的新闻内容')
    }
  } catch (error) {
    console.error('Failed to fetch news detail:', error)
    news.value = null
    ElMessage.error('加载新闻详情失败')
  } finally {
    loading.value = false
  }
}

function handleBack() {
  router.push('/news')
}

function handleNewsClick(id) {
  router.push(`/news/${id}`)
}

watch(() => route.params.id, async (newId) => {
  if (newId) {
    await fetchNewsDetail()
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
})

onMounted(() => {
  fetchNewsDetail()
})
</script>

<style lang="scss" scoped>
@use '../styles/variables.scss' as *;

.news-detail-page {
  min-height: 70vh;
  padding: 32px 0 56px;
}

.detail-shell {
  display: grid;
  gap: 20px;
}

.detail-stack {
  display: grid;
  gap: 20px;
}

.back-bar {
  display: flex;
  justify-content: flex-start;
}

.headline-image,
.headline-placeholder {
  width: 100%;
  height: 100%;
}

.headline-placeholder {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 56px;
  color: $text-light;
  background: linear-gradient(180deg, rgba(243, 239, 232, 0.84) 0%, rgba(233, 225, 214, 0.94) 100%);
}

.headline-meta,
.headline-stats span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: $text-secondary;
}

.headline-copy {
  display: grid;
  gap: 14px;
}

.headline-lead {
  margin: 0;
  font-size: 15px;
  line-height: 1.86;
  color: $text-secondary;
}

.headline-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
}

.aside-label {
  margin: 0;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.72);
}

.aside-brief h3 {
  margin: 8px 0 10px;
  font-size: 24px;
  line-height: 1.2;
}

.aside-brief p {
  margin: 0;
  font-size: 14px;
  line-height: 1.75;
  color: rgba(255, 255, 255, 0.8);
}

.reading-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(320px, 0.78fr);
  gap: 22px;
  align-items: start;
}

.reading-column,
.detail-rail {
  display: grid;
  gap: 18px;
}

.reading-panel,
.media-panel {
  display: grid;
  gap: 18px;
  padding: 28px;
  border-radius: 24px;
  border: 1px solid rgba(215, 208, 198, 0.88);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(251, 247, 240, 0.98) 100%);
  box-shadow: $shadow-sm;
}

.reading-head h2 {
  margin: 0;
  font-size: clamp(24px, 2.4vw, 34px);
  line-height: 1.15;
  color: $text-primary;
}

.reading-eyebrow {
  margin: 0 0 10px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: $secondary-color;
}

.content-body {
  color: $text-primary;
  font-size: 16px;
  line-height: 1.9;
}

.content-body.plain {
  white-space: pre-line;
}

.content-body :deep(h1),
.content-body :deep(h2),
.content-body :deep(h3),
.content-body :deep(h4) {
  color: $text-primary;
  line-height: 1.2;
  margin: 1.8em 0 0.7em;
}

.content-body :deep(p),
.content-body :deep(li),
.summary-panel {
  font-size: 16px;
  line-height: 1.9;
  color: $text-primary;
}

.content-body :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 18px;
}

.content-body :deep(a) {
  color: $primary-color;
}

.summary-panel {
  white-space: pre-line;
}

.detail-video {
  width: 100%;
  border-radius: 18px;
  background: #000;
}

.info-list,
.tag-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.tag-list {
  flex-direction: row;
  flex-wrap: wrap;
}

.info-row {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(243, 239, 232, 0.78);
  color: $text-primary;
}

.info-row span {
  color: $text-secondary;
  font-size: 14px;
}

.info-row strong {
  font-size: 14px;
  line-height: 1.6;
  text-align: right;
}

.asset-link,
.related-link {
  width: 100%;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid rgba(215, 208, 198, 0.24);
  background: rgba(255, 255, 255, 0.12);
  color: inherit;
  text-decoration: none;
}

.related-link {
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.08);
  cursor: pointer;
  transition: transform $transition-fast, background $transition-fast;
}

.related-link:hover {
  transform: translateY(-1px);
  background: rgba(255, 255, 255, 0.14);
}

.related-title {
  font-size: 14px;
  line-height: 1.55;
  text-align: left;
}

.related-meta {
  flex-shrink: 0;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
}

.footer-actions {
  display: flex;
  justify-content: flex-start;
}

.loading-container,
.error-container {
  padding: 88px 0;
  text-align: center;
}

.loading-icon {
  font-size: 40px;
  color: $primary-color;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}

@media (max-width: $breakpoint-lg) {
  .reading-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: $breakpoint-md) {
  .news-detail-page {
    padding: 24px 0 40px;
  }

  .reading-panel,
  .media-panel {
    padding: 20px;
    border-radius: 20px;
  }
}
</style>
