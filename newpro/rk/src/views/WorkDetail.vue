<template>
  <div class="work-detail-page design-work-detail-page">
    <div v-if="loading" class="state-panel">
      <el-icon class="loading-icon"><Loading /></el-icon>
      <p>正在加载作品详情...</p>
    </div>

    <user-hub-shell
      class="design-detail-hero-row"
      v-else-if="work"
      eyebrow="作品中心"
      :title="work.title"
      :description="work.description || '当前作品暂无描述信息。'"
      :meta-items="detailMetaItems"
      :stat-items="detailStats"
    >
      <template #actions>
        <el-button @click="handleBack">
          <el-icon><ArrowLeft /></el-icon>
          返回作品列表
        </el-button>
        <el-button v-if="work.demoUrl" type="primary" @click="openDemo">
          <el-icon><Monitor /></el-icon>
          查看演示
        </el-button>
        <el-button v-if="work.githubUrl" type="success" @click="openGithub">
          <el-icon><Platform /></el-icon>
          查看源码
        </el-button>
      </template>

      <template #filters>
        <div class="status-band">
          <div class="status-tags">
            <el-tag :type="getCategoryType(work.category)" size="large">
              {{ getCategoryText(work.category) }}
            </el-tag>
            <el-tag v-if="work.featured === 1" type="warning" size="large">精选作品</el-tag>
            <el-tag v-if="work.status === 1" type="success" size="large">已发布</el-tag>
          </div>
          <p class="status-copy">{{ getShortDescription(work.description) || '暂无摘要。' }}</p>
        </div>
      </template>

      <user-hub-panel title="作品说明" description="完整介绍当前作品的背景、目标和输出内容。">
        <div class="description-content design-content-card">
          <p v-if="work.description">{{ work.description }}</p>
          <p v-else class="empty-text">暂无描述</p>
        </div>
      </user-hub-panel>

      <user-hub-panel
        v-if="work.demoVideo"
        title="演示视频"
        description="保留原有视频播放能力，便于在控制台内完成预览。"
      >
        <div class="video-container">
          <video :src="resolveMediaUrl(work.demoVideo)" controls class="demo-video" poster=""></video>
        </div>
      </user-hub-panel>

      <user-hub-panel
        v-if="screenshotList.length > 0"
        title="项目截图"
        description="集中查看作品截图，支持原有图片预览。"
      >
        <div class="screenshots-content">
          <el-image
            v-for="(screenshot, index) in screenshotList"
            :key="index"
            :src="resolveMediaUrl(screenshot)"
            fit="cover"
            class="screenshot-item"
            :preview-src-list="resolvedScreenshotList"
            :initial-index="index"
          />
        </div>
      </user-hub-panel>

      <user-hub-panel
        v-if="relatedWorks.length > 0"
        title="相关作品推荐"
        description="按同类作品提供进一步浏览入口。"
      >
        <div class="related-works-grid">
          <article
            v-for="item in relatedWorks"
            :key="item.id"
            class="related-work-item"
            @click="goToWork(item.id)"
          >
            <div class="related-cover">
              <el-image v-if="item.coverImage" :src="resolveMediaUrl(item.coverImage)" fit="cover" class="related-image" />
              <div v-else class="related-placeholder">
                <el-icon><Document /></el-icon>
              </div>
            </div>
            <div class="related-info">
              <h4 class="related-title">{{ item.title }}</h4>
              <div class="related-meta">
                <span>{{ getCategoryText(item.category) }}</span>
                <span class="related-views">
                  <el-icon><View /></el-icon>
                  {{ item.viewCount || 0 }}
                </span>
              </div>
            </div>
          </article>
        </div>
      </user-hub-panel>

      <template #aside>
        <div class="design-info-sidebar">
        <user-hub-panel title="基本信息" description="展示作品的核心属性与时间信息。">
          <div v-if="work.coverImage" class="cover-preview">
            <el-image :src="resolveMediaUrl(work.coverImage)" fit="cover" class="cover-image" :preview-src-list="[resolveMediaUrl(work.coverImage)]" />
          </div>
          <user-hub-key-value-list :items="detailInfoItems" />
        </user-hub-panel>

        <user-hub-panel title="互动操作" description="保持原有收藏逻辑和本地状态记录。">
          <div class="like-panel">
            <div class="like-panel__stats">
              <div class="mini-stat">
                <span>浏览量</span>
                <strong>{{ work.viewCount || 0 }}</strong>
              </div>
              <div class="mini-stat">
                <span>收藏数</span>
                <strong>{{ work.likeCount || 0 }}</strong>
              </div>
            </div>
            <el-button
              :type="isLiked ? 'warning' : 'primary'"
              :loading="likeLoading"
              class="like-button"
              @click="handleLike"
            >
              <el-icon><component :is="isLiked ? StarFilled : Star" /></el-icon>
              {{ isLiked ? '已收藏' : '加入收藏' }}
            </el-button>
          </div>
        </user-hub-panel>

        <user-hub-panel
          v-if="technologyList.length > 0"
          title="技术栈"
          description="按标签整理作品所使用的关键技术。"
        >
          <div class="tags-content">
            <el-tag v-for="tech in technologyList" :key="tech" class="tag-item" type="info">
              {{ tech }}
            </el-tag>
          </div>
        </user-hub-panel>

        <user-hub-panel
          v-if="authorList.length > 0"
          title="作者团队"
          description="列出作品作者，保持现有作者字段解析逻辑。"
        >
          <div class="authors-content">
            <div v-for="author in authorList" :key="author" class="author-item">
              <el-avatar :size="34" class="author-avatar">{{ author.charAt(0) }}</el-avatar>
              <span class="author-name">{{ author }}</span>
            </div>
          </div>
        </user-hub-panel>

        <user-hub-panel
          v-if="resourceLinks.length > 0"
          title="相关链接"
          description="保留源码、演示和项目站点链接。"
        >
          <div class="links-list">
            <a
              v-for="item in resourceLinks"
              :key="item.label"
              :href="item.href"
              target="_blank"
              rel="noreferrer"
              class="link-item"
              :class="item.kind"
            >
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.label }}</span>
            </a>
          </div>
        </user-hub-panel>
        </div>
      </template>
    </user-hub-shell>

    <div v-else class="state-panel">
      <el-empty description="作品不存在或已下架">
        <el-button type="primary" @click="handleBack">返回作品列表</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  Loading,
  Document,
  Star,
  StarFilled,
  Platform,
  Monitor,
  House,
  View
} from '@element-plus/icons-vue'
import { getWorkDetail, likeWork, unlikeWork, getWorksByCategory } from '@/api/works'
import UserHubKeyValueList from '@/components/user-hub/UserHubKeyValueList.vue'
import UserHubPanel from '@/components/user-hub/UserHubPanel.vue'
import UserHubShell from '@/components/user-hub/UserHubShell.vue'
import { resolveMediaUrl } from '@/utils/mediaUrl'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const likeLoading = ref(false)
const work = ref(null)
const isLiked = ref(false)
const relatedWorks = ref([])

const workId = computed(() => route.params.id)
const technologyList = computed(() => parseList(work.value?.technologies))
const authorList = computed(() => parseList(work.value?.authors))
const screenshotList = computed(() => parseList(work.value?.screenshots))
const resolvedScreenshotList = computed(() => screenshotList.value.map((item) => resolveMediaUrl(item)))
const detailMetaItems = computed(() => {
  if (!work.value) return []
  const items = []
  if (work.value.createTime) items.push(`创建于 ${formatDate(work.value.createTime)}`)
  if (work.value.updateTime) items.push(`更新于 ${formatDate(work.value.updateTime)}`)
  if (authorList.value.length) items.push(`作者 ${authorList.value.length} 人`)
  return items
})
const detailStats = computed(() => {
  if (!work.value) return []
  return [
    {
      label: '浏览量',
      value: String(work.value.viewCount || 0),
      helper: '当前公开统计'
    },
    {
      label: '收藏数',
      value: String(work.value.likeCount || 0),
      helper: isLiked.value ? '你已收藏' : '可加入收藏',
      tone: isLiked.value ? 'warning' : 'info'
    },
    {
      label: '截图数量',
      value: String(screenshotList.value.length),
      helper: screenshotList.value.length > 0 ? '支持大图预览' : '暂无截图'
    },
    {
      label: '相关推荐',
      value: String(relatedWorks.value.length),
      helper: relatedWorks.value.length > 0 ? '同类作品入口' : '暂无相关推荐'
    }
  ]
})
const detailInfoItems = computed(() => {
  if (!work.value) return []
  return [
    { label: '作品分类', value: getCategoryText(work.value.category) },
    { label: '卡片类型', value: getTypeText(work.value.type) },
    { label: '创建时间', value: formatDate(work.value.createTime) },
    { label: '更新时间', value: formatDate(work.value.updateTime) },
    { label: '发布状态', value: work.value.status === 1 ? '已发布' : '未发布', tone: work.value.status === 1 ? 'success' : 'warning' }
  ]
})
const resourceLinks = computed(() => {
  if (!work.value) return []
  return [
    work.value.githubUrl
      ? { label: 'GitHub 源码', href: work.value.githubUrl, icon: Platform, kind: 'github' }
      : null,
    work.value.demoUrl
      ? { label: '在线演示', href: work.value.demoUrl, icon: Monitor, kind: 'demo' }
      : null,
    work.value.projectUrl
      ? { label: '项目官网', href: work.value.projectUrl, icon: House, kind: 'project' }
      : null
  ].filter(Boolean)
})

const getCategoryText = (category) => {
  const categoryMap = {
    web: 'Web应用',
    mobile: '移动应用',
    ai: '人工智能',
    desktop: '桌面应用',
    other: '其他'
  }
  return categoryMap[category] || category || '未分类'
}

const getCategoryType = (category) => {
  const typeMap = {
    web: 'primary',
    mobile: 'success',
    ai: 'warning',
    desktop: 'info',
    other: 'info'
  }
  return typeMap[category] || 'info'
}

const getTypeText = (type) => {
  const typeMap = {
    small: '小型',
    medium: '中型',
    large: '大型',
    tall: '高型',
    wide: '宽型'
  }
  return typeMap[type] || type || '-'
}

const getShortDescription = (description) => {
  if (!description) return ''
  return description.length > 100 ? `${description.substring(0, 100)}...` : description
}

const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const parseList = (value) => {
  if (!value) return []
  if (Array.isArray(value)) return value.filter(Boolean)
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed.filter(Boolean) : [parsed]
  } catch {
    return String(value)
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean)
  }
}

const checkLikeStatus = () => {
  if (!workId.value) return false
  const likedWorks = JSON.parse(localStorage.getItem('likedWorks') || '[]')
  isLiked.value = likedWorks.includes(workId.value.toString())
}

const saveLikeStatus = (liked) => {
  if (!workId.value) return
  const likedWorks = JSON.parse(localStorage.getItem('likedWorks') || '[]')
  const workIdStr = workId.value.toString()

  if (liked && !likedWorks.includes(workIdStr)) {
    likedWorks.push(workIdStr)
  } else if (!liked) {
    const index = likedWorks.indexOf(workIdStr)
    if (index > -1) likedWorks.splice(index, 1)
  }

  localStorage.setItem('likedWorks', JSON.stringify(likedWorks))
  isLiked.value = liked
}

const fetchWorkDetail = async () => {
  if (!workId.value) {
    ElMessage.error('作品ID无效')
    return
  }

  loading.value = true
  try {
    const res = await getWorkDetail(workId.value)
    if (res.data) {
      work.value = res.data
      checkLikeStatus()
      fetchRelatedWorks()
    } else {
      ElMessage.error('作品不存在')
    }
  } catch (error) {
    console.error('获取作品详情失败:', error)
    ElMessage.error('获取作品详情失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const handleLike = async () => {
  if (!workId.value) return

  likeLoading.value = true
  try {
    if (isLiked.value) {
      await unlikeWork(workId.value)
      work.value.likeCount = Math.max(0, (work.value.likeCount || 1) - 1)
      saveLikeStatus(false)
      ElMessage.success('已取消收藏')
    } else {
      await likeWork(workId.value)
      work.value.likeCount = (work.value.likeCount || 0) + 1
      saveLikeStatus(true)
      ElMessage.success('收藏成功')
    }
  } catch (error) {
    console.error('点赞操作失败:', error)
    ElMessage.error('操作失败，请稍后重试')
  } finally {
    likeLoading.value = false
  }
}

const handleBack = () => {
  router.push('/works')
}

const openDemo = () => {
  if (work.value?.demoUrl) {
    window.open(work.value.demoUrl, '_blank')
  }
}

const openGithub = () => {
  if (work.value?.githubUrl) {
    window.open(work.value.githubUrl, '_blank')
  }
}

const fetchRelatedWorks = async () => {
  if (!work.value?.category) return

  try {
    const res = await getWorksByCategory(work.value.category)
    if (res.data) {
      relatedWorks.value = res.data
        .filter((item) => item.id !== work.value.id)
        .slice(0, 4)
    }
  } catch (error) {
    console.error('获取相关作品失败:', error)
  }
}

const goToWork = (id) => {
  router.push(`/works/${id}`)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  fetchWorkDetail()
})

watch(
  () => route.params.id,
  (newId) => {
    if (newId) {
      fetchWorkDetail()
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }
  }
)
</script>

<style lang="scss" scoped>
.work-detail-page {
  padding: 28px 24px 40px;
  min-height: 60vh;
}

.state-panel {
  min-height: 420px;
  display: grid;
  place-items: center;
  text-align: center;
  border: 1px solid #e6edf5;
  border-radius: 24px;
  background: linear-gradient(180deg, #f8fbfe 0%, #ffffff 100%);
  box-shadow: 0 18px 40px rgba(15, 53, 87, 0.06);
}

.loading-icon {
  font-size: 40px;
  color: #2563eb;
  animation: spin 1s linear infinite;
}

.state-panel p {
  margin: 14px 0 0;
  color: #52667a;
}

.status-band {
  display: grid;
  gap: 14px;
}

.status-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.status-copy {
  margin: 0;
  color: #52667a;
  line-height: 1.7;
}

.description-content {
  font-size: 15px;
  line-height: 1.85;
  color: #102a43;
}

.description-content p {
  margin: 0;
  white-space: pre-line;
}

.empty-text {
  color: #7d8d9d;
  font-style: italic;
}

.video-container {
  position: relative;
  width: 100%;
  background: #000;
  border-radius: 20px;
  overflow: hidden;
}

.demo-video {
  width: 100%;
  max-height: 480px;
  display: block;
}

.screenshots-content {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.screenshot-item {
  width: 100%;
  height: 170px;
  border-radius: 18px;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.screenshot-item:hover {
  transform: translateY(-2px);
}

.related-works-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.related-work-item {
  display: grid;
  gap: 14px;
  padding: 16px;
  border: 1px solid #e6edf5;
  border-radius: 20px;
  background: #f8fbfe;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.related-work-item:hover {
  transform: translateY(-1px);
  border-color: rgba(37, 99, 235, 0.28);
  box-shadow: 0 16px 34px rgba(15, 53, 87, 0.08);
}

.related-cover {
  width: 100%;
  height: 150px;
  border-radius: 16px;
  overflow: hidden;
  background: #eaf2fb;
}

.related-image {
  width: 100%;
  height: 100%;
}

.related-placeholder {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  color: #6b7d8f;
  font-size: 26px;
}

.related-title {
  margin: 0 0 8px;
  font-size: 16px;
  color: #102a43;
}

.related-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  color: #6b7d8f;
  font-size: 13px;
}

.related-views {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.cover-preview {
  margin-bottom: 14px;
}

.cover-image {
  width: 100%;
  height: 180px;
  border-radius: 18px;
}

.like-panel {
  display: grid;
  gap: 16px;
}

.like-panel__stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.mini-stat {
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid #e6edf5;
  background: #f8fbfe;
}

.mini-stat span {
  display: block;
  color: #6b7d8f;
  font-size: 12px;
}

.mini-stat strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  line-height: 1;
  color: #102a43;
}

.like-button {
  width: 100%;
}

.tags-content {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tag-item {
  border-radius: 999px;
}

.authors-content {
  display: grid;
  gap: 12px;
}

.author-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid #e6edf5;
  border-radius: 16px;
  background: #f8fbfe;
}

.author-avatar {
  background: #2563eb;
  color: #ffffff;
  font-weight: 600;
}

.author-name {
  color: #102a43;
}

.links-list {
  display: grid;
  gap: 12px;
}

.link-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid #e6edf5;
  background: #f8fbfe;
  color: #102a43;
  text-decoration: none;
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.link-item:hover {
  transform: translateY(-1px);
  border-color: rgba(37, 99, 235, 0.26);
  box-shadow: 0 12px 26px rgba(15, 53, 87, 0.08);
}

.link-item.github:hover {
  border-color: rgba(36, 41, 46, 0.22);
}

.link-item.demo:hover {
  border-color: rgba(22, 163, 74, 0.24);
}

.link-item.project:hover {
  border-color: rgba(202, 138, 4, 0.24);
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 960px) {
  .work-detail-page {
    padding: 20px 16px 32px;
  }

  .related-works-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .like-panel__stats {
    grid-template-columns: minmax(0, 1fr);
  }

  .screenshots-content {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  }
}
</style>
