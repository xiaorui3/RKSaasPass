<template>
  <div class="works-front-page design-works-reference-page" data-testid="portal-works-page">
    <PortalHero
      class="design-works-hero"
      eyebrow="作品展示"
      title="作品展示与项目成果"
      description="围绕社团项目、创意作品与成果展示，保留原有数据接口，用更清晰的中文门户结构展示精选作品、分类筛选和个人收藏。"
      :metrics="heroMetrics"
      band-eyebrow="展示模式"
      band-title="在全部作品与我的收藏之间自由切换。"
      band-description="收藏筛选仍然沿用本地存储逻辑，现有个人收藏行为不会改变。"
    >
      <template #actions>
        <router-link class="hero-link primary" to="/about">社团简介</router-link>
        <router-link class="hero-link secondary" to="/contact">项目咨询</router-link>
      </template>
    </PortalHero>

    <main class="container works-shell">
      <PortalContentFilterBar
        class="design-category-pills"
        eyebrow="作品筛选"
        title="按分类、收藏和关键词浏览作品"
        description="保留现有分类和收藏能力，用统一的中文筛选入口组织作品发现流程。"
        :tags="activeFilterTags"
      >
        <template #actions>
          <el-button text @click="handleReset">重置筛选</el-button>
        </template>
        <div class="filter-bar">
          <div class="filter-group">
            <span class="filter-label">分类</span>
            <el-radio-group v-model="currentCategory" @change="handleCategoryChange">
              <el-radio-button label="">全部</el-radio-button>
              <el-radio-button
                v-for="item in availableCategories"
                :key="item"
                :label="item"
              >
                {{ getCategoryText(item) }}
              </el-radio-button>
            </el-radio-group>
          </div>

          <div class="filter-group collection-tabs">
            <span class="filter-label">收藏</span>
            <el-radio-group v-model="collectionFilter" @change="handleCollectionChange">
              <el-radio-button label="all">全部作品</el-radio-button>
              <el-radio-button label="liked">我的收藏</el-radio-button>
            </el-radio-group>
          </div>

          <div class="search-box">
            <span class="filter-label">关键词</span>
            <el-input
              v-model="searchKeyword"
              placeholder="请输入作品名称或关键词"
              clearable
              @keyup.enter="handleSearch"
            >
              <template #append>
                <el-button @click="handleSearch">
                  <el-icon><Search /></el-icon>
                </el-button>
              </template>
            </el-input>
          </div>
        </div>
        <template #summary>
          <span class="summary-copy">
            当前展示 {{ filteredWorksList.length }} 个作品。
          </span>
          <span class="summary-copy">
            当前共识别 {{ availableCategories.length }} 个作品分类。
          </span>
        </template>
      </PortalContentFilterBar>

      <PortalContentFeatureCard
        v-if="featuredWork"
        eyebrow="精选推荐"
        :title="featuredWork.title"
        :description="getShortDescription(featuredWork.description, 220)"
      >
        <template #meta>
          <el-tag size="small" :type="getCategoryType(featuredWork.category)">
            {{ getCategoryText(featuredWork.category) }}
          </el-tag>
          <el-tag v-if="featuredWork.isFeatured" size="small" type="warning">精选</el-tag>
        </template>

        <div class="spotlight-copy">
          <p>{{ getShortDescription(featuredWork.description, 220) }}</p>

          <div class="spotlight-meta">
            <span>{{ formatAuthors(featuredWork.authors) }}</span>
            <span>浏览 {{ featuredWork.viewCount || 0 }}</span>
            <span>点赞 {{ featuredWork.likeCount || 0 }}</span>
          </div>

          <div class="spotlight-tech">
            <el-tag
              v-for="tech in parseList(featuredWork.technologies).slice(0, 4)"
              :key="tech"
              size="small"
              effect="plain"
            >
              {{ tech }}
            </el-tag>
          </div>
        </div>

        <template #actions>
          <el-button type="primary" @click="goToDetail(featuredWork.id)">
            查看作品
          </el-button>
        </template>

        <template #media>
          <div class="spotlight-cover">
            <el-image
              v-if="featuredWork.coverImage"
              :src="resolveMediaUrl(featuredWork.coverImage)"
              fit="cover"
              class="spotlight-image"
            >
              <template #error>
                <div class="image-placeholder">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
            <div v-else class="image-placeholder">
              <el-icon><Picture /></el-icon>
            </div>
          </div>
        </template>

        <template #aside>
          <div class="spotlight-aside">
            <p class="spotlight-aside-label">展示说明</p>
            <h3>{{ collectionFilter === 'liked' ? '我的收藏作品' : '公开作品集' }}</h3>
            <p>
              {{ collectionFilter === 'liked'
                ? '当前推荐位会优先基于你本地收藏的作品进行展示，并在收藏变化后同步更新。'
                : '精选作品会固定展示在作品页顶部，方便访客快速看到社团代表性成果。' }}
            </p>
          </div>
        </template>
      </PortalContentFeatureCard>

      <section class="surface-panel works-panel works-gallery-panel" data-testid="portal-works-grid">
        <PortalSectionHeader
          eyebrow="作品目录"
          title="已发布作品目录"
          :description="gridDescription"
        >
          <template #trailing>
            <el-button text @click="handleReset">重置筛选</el-button>
          </template>
        </PortalSectionHeader>

        <div v-if="loading" class="loading-container">
          <el-icon class="loading-icon"><Loading /></el-icon>
          <p>正在加载作品列表...</p>
        </div>

        <div v-else-if="filteredWorksList.length === 0" class="empty-container">
          <el-empty description="当前暂无已发布作品">
            <el-button type="primary" @click="handleReset">查看全部作品</el-button>
          </el-empty>
        </div>

        <div v-else class="works-layout">
          <div class="works-grid design-works-card-grid">
            <article
              v-for="work in pagedWorksList"
              :key="work.id"
              class="work-card"
              @click="goToDetail(work.id)"
            >
              <div class="work-cover">
                <el-image
                  v-if="work.coverImage"
                  :src="resolveMediaUrl(work.coverImage)"
                  fit="cover"
                  class="cover-image"
                >
                  <template #error>
                    <div class="image-placeholder">
                      <el-icon><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
                <div v-else class="image-placeholder">
                  <el-icon><Picture /></el-icon>
                </div>

                <div class="work-overlay">
                  <el-tag :type="getCategoryType(work.category)" size="small">
                    {{ getCategoryText(work.category) }}
                  </el-tag>
                  <el-tag v-if="work.isFeatured" size="small" type="warning">精选</el-tag>
                </div>
              </div>

              <div class="work-info">
                <h3 class="work-title">{{ work.title }}</h3>
                <p class="work-description">{{ getShortDescription(work.description) }}</p>

                <div class="work-meta">
                  <div class="meta-item">
                    <el-icon><User /></el-icon>
                    <span>{{ formatAuthors(work.authors) }}</span>
                  </div>
                  <div class="meta-item">
                    <el-icon><View /></el-icon>
                    <span>{{ work.viewCount || 0 }}</span>
                  </div>
                  <div class="meta-item">
                    <el-icon><Star /></el-icon>
                    <span>{{ work.likeCount || 0 }}</span>
                  </div>
                </div>

                <div v-if="work.technologies" class="work-tags">
                  <el-tag
                    v-for="tech in parseList(work.technologies).slice(0, 3)"
                    :key="tech"
                    size="small"
                    effect="plain"
                  >
                    {{ tech }}
                  </el-tag>
                  <el-tag v-if="parseList(work.technologies).length > 3" size="small" effect="plain">
                    +{{ parseList(work.technologies).length - 3 }}
                  </el-tag>
                </div>
              </div>
            </article>
          </div>

          <aside class="works-rail">
            <PortalContentInfoCard
              eyebrow="作品概览"
              title="分类分布"
              description="侧边栏会基于当前已加载的数据集展示分类统计，不改变原有路由行为。"
              tone="soft"
            >
              <div class="rail-list">
                <div v-for="item in categoryCounts" :key="item.name" class="rail-row">
                  <span>{{ item.name }}</span>
                  <strong>{{ item.count }}</strong>
                </div>
              </div>
            </PortalContentInfoCard>

            <PortalContentInfoCard
              eyebrow="收藏状态"
              title="我的收藏概览"
              description="仅看收藏模式仍读取本地存储数据，并在这里汇总展示当前状态。"
              :tone="collectionFilter === 'liked' ? 'contrast' : 'light'"
            >
              <div class="rail-list">
                <div class="rail-row">
                  <span>收藏数量</span>
                  <strong>{{ likedWorkIds.length }}</strong>
                </div>
                <div class="rail-row">
                  <span>当前模式</span>
                  <strong>{{ collectionFilter === 'liked' ? '我的收藏' : '全部作品' }}</strong>
                </div>
              </div>
            </PortalContentInfoCard>
          </aside>
        </div>

        <div v-if="filteredWorksList.length > pageSize" class="pagination design-pagination-strip">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="filteredWorksList.length"
            :page-sizes="[12, 24, 36, 48]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Loading, Picture, Search, Star, User, View } from '@element-plus/icons-vue'
import PortalHero from '@/components/portal/PortalHero.vue'
import PortalSectionHeader from '@/components/portal/PortalSectionHeader.vue'
import PortalContentFeatureCard from '@/components/portal-content/PortalContentFeatureCard.vue'
import PortalContentFilterBar from '@/components/portal-content/PortalContentFilterBar.vue'
import PortalContentInfoCard from '@/components/portal-content/PortalContentInfoCard.vue'
import { getWorksList } from '@/api/works'
import { resolveMediaUrl } from '@/utils/mediaUrl'

const router = useRouter()

const loading = ref(false)
const worksList = ref([])
const currentPage = ref(1)
const pageSize = ref(12)
const currentCategory = ref('')
const collectionFilter = ref('all')
const searchKeyword = ref('')
const likedWorkIds = ref([])

function normalizeWorks(payload) {
  if (Array.isArray(payload?.records)) {
    return payload.records
  }
  if (Array.isArray(payload)) {
    return payload
  }
  return []
}

const filteredWorksList = computed(() => {
  if (collectionFilter.value !== 'liked') {
    return worksList.value
  }
  return worksList.value.filter((work) => likedWorkIds.value.includes(String(work.id)))
})

const pagedWorksList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredWorksList.value.slice(start, start + pageSize.value)
})

const featuredWork = computed(() => {
  return filteredWorksList.value.find((item) => item.isFeatured) || filteredWorksList.value[0] || null
})

const availableCategories = computed(() => {
  return [...new Set(
    worksList.value
      .map((item) => item?.category)
      .filter(Boolean)
  )].sort((left, right) => left.localeCompare(right))
})

const heroMetrics = computed(() => {
  return [
    {
      label: '已发布作品',
      value: String(worksList.value.length),
      detail: '来源于公开作品接口的当前数据总量。'
    },
    {
      label: '收藏作品',
      value: String(likedWorkIds.value.length),
      detail: '仅看收藏模式继续基于本地收藏记录工作。'
    },
    {
      label: '当前筛选',
      value: currentCategory.value ? getCategoryText(currentCategory.value) : '全部',
      detail: searchKeyword.value ? `关键词：${searchKeyword.value}` : '可按分类或收藏状态浏览作品。'
    }
  ]
})

const gridDescription = computed(() => {
  if (collectionFilter.value === 'liked') {
    return '当前仅展示你已收藏的作品，保留既有本地收藏逻辑。'
  }
  if (searchKeyword.value) {
    return `当前结果已应用关键词“${searchKeyword.value}”和所选分类筛选。`
  }
  return '作品卡片保持紧凑清晰，并继续支持原有详情页跳转。'
})

const activeFilterTags = computed(() => {
  const tags = []
  if (currentCategory.value) {
    tags.push({ label: '分类', value: getCategoryText(currentCategory.value) })
  }
  tags.push({
    label: '收藏',
    value: collectionFilter.value === 'liked' ? '我的收藏' : '全部作品'
  })
  if (searchKeyword.value) {
    tags.push({ label: '关键词', value: searchKeyword.value })
  }
  return tags
})

const categoryCounts = computed(() => {
  const counts = new Map()
  for (const item of filteredWorksList.value) {
    const key = getCategoryText(item.category)
    counts.set(key, (counts.get(key) || 0) + 1)
  }
  return [...counts.entries()]
    .map(([name, count]) => ({ name, count }))
    .sort((left, right) => right.count - left.count)
    .slice(0, 6)
})

const getCategoryText = (category) => {
  const categoryMap = {
    web: 'Web 项目',
    mobile: '移动应用',
    ai: '人工智能',
    desktop: '桌面应用',
    other: '其他'
  }
  return categoryMap[category] || category || '综合'
}

const getCategoryType = (category) => {
  const normalized = String(category || '').toLowerCase()
  const typeMap = {
    web: 'primary',
    mobile: 'success',
    ai: 'warning',
    desktop: 'info',
    other: ''
  }
  if (typeMap[normalized]) {
    return typeMap[normalized]
  }
  if (normalized.includes('ai') || normalized.includes('智能')) {
    return 'warning'
  }
  if (normalized.includes('web')) {
    return 'primary'
  }
  if (normalized.includes('mobile') || normalized.includes('移动')) {
    return 'success'
  }
  if (normalized.includes('desktop') || normalized.includes('桌面')) {
    return 'info'
  }
  return typeMap[category] || 'info'
}

const getShortDescription = (description, maxLength = 88) => {
  if (!description) {
    return '当前作品暂无简介内容。'
  }
  return description.length > maxLength ? `${description.slice(0, maxLength)}...` : description
}

const formatAuthors = (authors) => {
  if (!authors) {
    return '未知团队'
  }

  if (typeof authors === 'string') {
    try {
      const parsed = JSON.parse(authors)
      return Array.isArray(parsed) ? parsed.join(', ') : authors
    } catch {
      return authors
    }
  }

  if (Array.isArray(authors)) {
    return authors.join(', ')
  }

  return String(authors)
}

const parseList = (value) => {
  if (!value) {
    return []
  }
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : [parsed]
  } catch {
    return String(value)
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean)
  }
}

const syncLikedWorks = () => {
  try {
    const stored = JSON.parse(localStorage.getItem('likedWorks') || '[]')
    likedWorkIds.value = Array.isArray(stored) ? stored.map((item) => String(item)) : []
  } catch {
    likedWorkIds.value = []
  }
}

const fetchWorks = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      category: currentCategory.value,
      title: searchKeyword.value,
      status: 1
    }
    const res = await getWorksList(params)
    worksList.value = normalizeWorks(res.data)
    syncLikedWorks()
  } catch (error) {
    console.error('Failed to fetch works list:', error)
    worksList.value = []
  } finally {
    loading.value = false
  }
}

const handleCategoryChange = () => {
  currentPage.value = 1
  fetchWorks()
}

const handleCollectionChange = () => {
  currentPage.value = 1
  syncLikedWorks()
}

const handleSearch = () => {
  currentPage.value = 1
  fetchWorks()
}

const handleReset = () => {
  currentCategory.value = ''
  collectionFilter.value = 'all'
  searchKeyword.value = ''
  currentPage.value = 1
  fetchWorks()
}

const handleSizeChange = (value) => {
  pageSize.value = value
  currentPage.value = 1
  fetchWorks()
}

const handlePageChange = (value) => {
  currentPage.value = value
  fetchWorks()
}

const goToDetail = (id) => {
  router.push(`/works/${id}`)
}

onMounted(() => {
  syncLikedWorks()
  fetchWorks()
})
</script>

<style lang="scss" scoped>
@use '../styles/variables.scss' as *;

.works-front-page {
  min-height: 100vh;
}

.works-shell {
  display: grid;
  gap: 24px;
  padding-bottom: 56px;
}

.surface-panel {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.95) 0%, rgba(251, 247, 240, 0.98) 100%);
  border: 1px solid $border-light;
  border-radius: 24px;
  padding: 26px;
  box-shadow: $shadow-sm;
}

.works-panel {
  display: grid;
  gap: 20px;
}

.filter-bar {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 0.9fr) minmax(260px, 320px);
  gap: 18px;
  align-items: end;
}

.filter-group,
.search-box {
  display: grid;
  gap: 10px;
}

.filter-label {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: $text-light;
}

.summary-copy {
  display: inline-flex;
  align-items: center;
  min-height: 36px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(215, 208, 198, 0.92);
  color: $text-secondary;
  font-size: 13px;
}

.hero-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  padding: 0 16px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 700;
}

.hero-link.primary {
  background: $primary-color;
  color: $text-white;
}

.hero-link.secondary {
  border: 1px solid rgba(31, 58, 52, 0.2);
  color: $text-primary;
}

.spotlight-cover,
.work-cover {
  position: relative;
  overflow: hidden;
  border-radius: 22px;
  background: $bg-light;
}

.spotlight-cover {
  min-height: 320px;
}

.spotlight-image,
.cover-image {
  width: 100%;
  height: 100%;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  min-height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $text-light;
  font-size: 44px;
  background: linear-gradient(180deg, rgba(243, 239, 232, 0.8) 0%, rgba(235, 228, 217, 0.9) 100%);
}

.spotlight-copy,
.spotlight-aside {
  display: grid;
  gap: 16px;
}

.spotlight-copy {
  align-content: center;
}

.spotlight-aside-label {
  margin: 0;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.72);
}

.spotlight-aside h3 {
  margin: 0;
  font-size: 24px;
  line-height: 1.2;
  color: $text-white;
}

.spotlight-aside p {
  margin: 0;
  font-size: 14px;
  line-height: 1.75;
  color: rgba(255, 255, 255, 0.78);
}

.spotlight-tags,
.spotlight-meta,
.spotlight-tech,
.work-tags,
.work-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.spotlight-copy h3,
.work-title {
  margin: 0;
  color: $text-primary;
}

.spotlight-copy h3 {
  font-size: 32px;
  line-height: 1.15;
}

.spotlight-copy p,
.work-description {
  margin: 0;
  font-size: 15px;
  line-height: 1.8;
  color: $text-secondary;
}

.spotlight-meta span,
.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: $text-secondary;
}

.works-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(290px, 1fr));
  gap: 18px;
}

.works-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(300px, 0.8fr);
  gap: 20px;
  align-items: start;
}

.works-rail {
  display: grid;
  gap: 16px;
}

.work-card {
  display: grid;
  gap: 0;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(215, 208, 198, 0.88);
  border-radius: 22px;
  cursor: pointer;
  transition: transform $transition-fast, box-shadow $transition-fast, border-color $transition-fast;
}

.work-card:hover {
  transform: translateY(-2px);
  box-shadow: $shadow-base;
  border-color: rgba(31, 58, 52, 0.2);
}

.work-cover {
  height: 210px;
}

.work-overlay {
  position: absolute;
  inset: 0 auto auto 0;
  display: flex;
  gap: 8px;
  padding: 14px;
  background: linear-gradient(180deg, rgba(28, 37, 34, 0.42) 0%, rgba(28, 37, 34, 0) 72%);
}

.work-info {
  display: grid;
  gap: 12px;
  padding: 18px;
}

.work-title {
  font-size: 20px;
  line-height: 1.25;
}

.work-description {
  min-height: 54px;
}

.meta-item {
  color: $text-light;
}

.rail-list {
  display: grid;
  gap: 10px;
}

.rail-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(243, 239, 232, 0.78);
  color: $text-primary;
}

.loading-container,
.empty-container {
  padding: 70px 0;
  text-align: center;
}

.loading-icon {
  font-size: 38px;
  color: $primary-color;
  animation: spin 1s linear infinite;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 12px;
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
  .filter-bar,
  .works-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: $breakpoint-md) {
  .works-shell {
    gap: 18px;
    padding-bottom: 40px;
  }

  .surface-panel {
    padding: 20px;
    border-radius: 20px;
  }

  .work-cover {
    height: 190px;
  }

  .spotlight-copy h3 {
    font-size: 26px;
  }
}

@media (max-width: 640px) {
  .works-shell {
    gap: 14px;
    padding-bottom: 28px;
  }

  .surface-panel {
    padding: 16px;
    border-radius: 18px;
  }

  .filter-bar {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .hero-link {
    min-height: 40px;
    padding: 0 14px;
    font-size: 13px;
  }

  .summary-copy {
    min-height: 32px;
    padding: 0 12px;
    font-size: 12px;
  }

  .spotlight-cover {
    min-height: 220px;
    border-radius: 18px;
  }

  .spotlight-copy,
  .spotlight-aside {
    gap: 12px;
  }

  .spotlight-copy h3 {
    font-size: 22px;
    line-height: 1.2;
  }

  .spotlight-copy p,
  .work-description {
    font-size: 14px;
    line-height: 1.68;
  }

  .works-grid {
    grid-template-columns: 1fr;
    gap: 14px;
  }

  .works-rail {
    gap: 12px;
  }

  .work-card {
    border-radius: 18px;
  }

  .work-cover {
    height: 176px;
  }

  .work-info {
    gap: 10px;
    padding: 14px;
  }

  .work-title {
    font-size: 18px;
    line-height: 1.35;
  }

  .work-description {
    min-height: 0;
  }

  .pagination {
    overflow-x: auto;
    justify-content: flex-start;
    padding-bottom: 4px;
  }
}
</style>
