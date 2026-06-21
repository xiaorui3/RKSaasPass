<template>
  <div class="alumni-page design-alumni-reference-page" data-testid="portal-alumni-page">
    <section class="alumni-hero design-alumni-hero">
      <div class="container alumni-hero__inner">
        <div class="hero-copy">
          <p class="hero-eyebrow">历史社员</p>
          <h1>校友风采</h1>
          <p>
            按届别集中展示在册成员与毕业校友，保留现有校友公开接口和头像媒体地址处理。
          </p>
        </div>

        <div class="hero-stats">
          <article>
            <strong>{{ overview?.rosterCount || overview?.totalCount || 0 }}</strong>
            <span>档案总人数</span>
          </article>
          <article>
            <strong>{{ overview?.currentCount || 0 }}</strong>
            <span>在册成员</span>
          </article>
          <article>
            <strong>{{ overview?.alumniCount || overview?.graduatedCount || 0 }}</strong>
            <span>历史社员</span>
          </article>
        </div>
      </div>
    </section>

    <main class="container alumni-shell">
      <aside class="alumni-rail">
        <section class="rail-card">
          <p class="rail-eyebrow">分届概览</p>
          <h2>{{ overview?.generationCount || Object.keys(groupedAlumni).length }}</h2>
          <span>个届别</span>
          <p>数据来自公开校友接口，搜索时会基于已加载档案进行本地过滤。</p>
        </section>

        <section class="rail-card soft">
          <p class="rail-eyebrow">检索范围</p>
          <div class="rail-list">
            <span>姓名</span>
            <span>学号</span>
            <span>专业</span>
            <span>部门</span>
            <span>职务</span>
          </div>
        </section>
      </aside>

      <section class="alumni-board">
        <div class="board-header">
          <div>
            <p class="board-eyebrow">校友目录</p>
            <h2>历史社员风采</h2>
            <span>按届别归档，便于快速浏览成员成长轨迹。</span>
          </div>
          <div class="search-bar">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索姓名、学号、专业或部门"
              clearable
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
          </div>
        </div>

        <div class="alumni-groups" v-loading="loading">
          <div
            v-for="(alumniList, year) in groupedAlumni"
            :key="year"
            class="generation-group"
          >
            <div class="generation-header">
              <div>
                <p>届别</p>
                <h3>{{ year }} 届</h3>
              </div>
              <span class="member-count">{{ alumniList.length }} 人</span>
            </div>

            <div class="alumni-grid design-alumni-card-grid">
              <article
                v-for="alumni in alumniList"
                :key="alumni.id"
                class="alumni-card"
              >
                <div class="alumni-avatar">
                  <el-avatar :size="72" :src="resolveMediaUrl(alumni.avatar)">
                    {{ alumni.name?.charAt(0) }}
                  </el-avatar>
                </div>
                <div class="alumni-info">
                  <div class="alumni-title">
                    <h3>{{ alumni.name }}</h3>
                    <el-tag :type="isGraduated(alumni) ? 'success' : 'primary'" size="small">
                      {{ alumni.graduationStatus || '在册' }}
                    </el-tag>
                  </div>
                  <div class="alumni-meta">
                    <span v-if="alumni.studentId">学号：{{ alumni.studentId }}</span>
                    <span v-if="alumni.department">{{ alumni.department }}</span>
                    <span v-if="alumni.position">{{ alumni.position }}</span>
                  </div>
                  <p v-if="alumni.introduction" class="alumni-intro">
                    {{ alumni.introduction }}
                  </p>
                </div>
              </article>
            </div>
          </div>

          <el-empty
            v-if="!loading && Object.keys(groupedAlumni).length === 0"
            description="暂无校友风采信息"
          />
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getPublicAlumniGroupedByGeneration, getPublicAlumniOverview } from '@/api/alumni'
import { resolveMediaUrl } from '@/utils/mediaUrl'

const loading = ref(false)
const groupedAlumni = ref({})
const allGroupedAlumni = ref({})
const overview = ref(null)
const searchKeyword = ref('')

const emptyOverview = () => ({
  totalCount: 0,
  rosterCount: 0,
  alumniCount: 0,
  currentCount: 0,
  graduatedCount: 0,
  generationCount: 0
})

function isGraduated(alumni) {
  return alumni?.graduationStatus === '已毕业' || alumni?.memberStatus === '已毕业'
}

function normalizeAlumniItem(item = {}) {
  const graduationStatus = item.graduationStatus || item.memberStatus || item.status || ''
  return {
    ...item,
    name: item.name || item.realName || item.username || '校友',
    avatar: item.avatar || item.avatarUrl || item.headImage || item.photoUrl || '',
    generationYear: item.generationYear || item.year || item.graduationYear || '未知',
    graduationStatus
  }
}

function normalizeGroupedAlumni(data) {
  const source = data?.records || data?.list || data?.groupedData || data
  if (Array.isArray(source)) {
    return source.reduce((acc, item) => {
      const alumniList = Array.isArray(item?.children)
        ? item.children
        : Array.isArray(item?.list)
          ? item.list
          : Array.isArray(item?.alumniList)
            ? item.alumniList
            : [item]
      alumniList.map(normalizeAlumniItem).forEach((alumni) => {
        const year = String(item?.year || item?.generationYear || alumni.generationYear || '未知')
        if (!acc[year]) {
          acc[year] = []
        }
        acc[year].push(alumni)
      })
      return acc
    }, {})
  }
  if (!source || typeof source !== 'object') {
    return {}
  }
  return Object.entries(source).reduce((acc, [year, list]) => {
    const alumniList = Array.isArray(list) ? list : []
    if (alumniList.length) {
      acc[String(year)] = alumniList.map(normalizeAlumniItem)
    }
    return acc
  }, {})
}

function deriveOverviewFromGroups(groups) {
  const lists = Object.values(groups || {}).filter(Array.isArray)
  const alumni = lists.flat()
  const graduatedCount = alumni.filter((item) => isGraduated(item)).length
  const currentCount = alumni.length - graduatedCount
  return {
    ...emptyOverview(),
    totalCount: alumni.length,
    rosterCount: alumni.length,
    alumniCount: graduatedCount || alumni.length,
    graduatedCount: graduatedCount || alumni.length,
    currentCount,
    generationCount: lists.length
  }
}

function normalizeOverview(data, groups) {
  const fallback = deriveOverviewFromGroups(groups)
  if (!data || typeof data !== 'object') {
    return fallback
  }
  return {
    ...fallback,
    ...data,
    totalCount: data.totalCount ?? data.rosterCount ?? fallback.totalCount,
    rosterCount: data.rosterCount ?? data.totalCount ?? fallback.rosterCount,
    alumniCount: data.alumniCount ?? data.graduatedCount ?? fallback.alumniCount,
    graduatedCount: data.graduatedCount ?? data.alumniCount ?? fallback.graduatedCount,
    currentCount: data.currentCount ?? fallback.currentCount,
    generationCount: data.generationCount ?? data.generations ?? fallback.generationCount
  }
}

const fetchAlumni = async () => {
  loading.value = true
  try {
    const [groupedRes, overviewRes] = await Promise.all([
      getPublicAlumniGroupedByGeneration(),
      getPublicAlumniOverview()
    ])
    const normalizedGroups = normalizeGroupedAlumni(groupedRes.data)
    allGroupedAlumni.value = normalizedGroups
    groupedAlumni.value = normalizedGroups
    overview.value = normalizeOverview(overviewRes.data, normalizedGroups)
  } catch (error) {
    console.error('fetch alumni failed:', error)
    groupedAlumni.value = {}
    allGroupedAlumni.value = {}
    overview.value = emptyOverview()
    ElMessage.error('获取校友信息失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    fetchAlumni()
    return
  }

  const keyword = searchKeyword.value.trim().toLowerCase()
  groupedAlumni.value = Object.entries(allGroupedAlumni.value).reduce((acc, [year, list]) => {
    const matched = list.filter((alumni) =>
      [
        alumni.name,
        alumni.studentId,
        alumni.major,
        alumni.department,
        alumni.position,
        alumni.workUnit,
        alumni.introduction
      ].filter(Boolean).join(' ').toLowerCase().includes(keyword)
    )
    if (matched.length) {
      acc[year] = matched
    }
    return acc
  }, {})
  overview.value = normalizeOverview(overview.value, groupedAlumni.value)
}

onMounted(() => {
  fetchAlumni()
})
</script>

<style lang="scss" scoped>
@use '../styles/variables.scss' as *;
@use '../styles/mixins.scss' as *;

.alumni-page {
  min-height: 100vh;
  padding-bottom: 64px;
  background:
    linear-gradient(180deg, rgba(47, 111, 237, 0.06) 0%, rgba(247, 250, 255, 0.92) 28%, #fff 100%);
}

.alumni-hero {
  padding: 28px 0 0;
}

.alumni-hero__inner {
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
  max-width: 650px;
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

.alumni-shell {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 24px;
  padding-top: 26px;
}

.alumni-rail {
  display: grid;
  gap: 18px;
  align-content: start;
}

.rail-card,
.alumni-board {
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
.board-eyebrow,
.generation-header p {
  margin: 0 0 10px;
  color: #2f6fed;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.14em;
}

.rail-card h2 {
  margin: 0;
  font-size: 42px;
  line-height: 1;
  color: $text-primary;
}

.rail-card > span {
  display: block;
  margin: 8px 0 16px;
  color: $text-secondary;
}

.rail-card p:last-child {
  margin: 0;
  color: $text-secondary;
  line-height: 1.75;
}

.rail-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.rail-list span {
  padding: 8px 12px;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 700;
}

.alumni-board {
  padding: 24px;
}

.board-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 0.62fr);
  gap: 18px;
  align-items: end;
  margin-bottom: 22px;
}

.board-header h2 {
  margin: 0;
  font-size: 26px;
  color: $text-primary;
}

.board-header span {
  display: block;
  margin-top: 8px;
  color: $text-secondary;
  line-height: 1.6;
}

.search-bar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
}

.alumni-groups {
  display: grid;
  gap: 22px;
}

.generation-group {
  padding: 20px;
  border-radius: 20px;
  background: #fff;
  border: 1px solid rgba(226, 232, 240, 0.88);
}

.generation-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.generation-header h3 {
  margin: 0;
  font-size: 22px;
  color: $text-primary;
}

.member-count {
  padding: 7px 12px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2f6fed;
  font-size: 13px;
  font-weight: 800;
}

.alumni-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.alumni-card {
  display: flex;
  gap: 16px;
  padding: 16px;
  border-radius: 18px;
  background: #f8fbff;
  border: 1px solid rgba(226, 232, 240, 0.82);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.alumni-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 28px rgba(31, 58, 120, 0.08);
}

.alumni-avatar {
  flex-shrink: 0;
}

.alumni-info {
  min-width: 0;
  flex: 1;
}

.alumni-title {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: flex-start;
  margin-bottom: 10px;
}

.alumni-title h3 {
  margin: 0;
  font-size: 17px;
  color: $text-primary;
}

.alumni-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.alumni-meta span {
  font-size: 13px;
  color: $text-secondary;
  background: rgba(31, 58, 120, 0.06);
  padding: 3px 8px;
  border-radius: 999px;
}

.alumni-intro {
  margin: 0;
  font-size: 13px;
  color: $text-secondary;
  line-height: 1.6;
  @include text-ellipsis(2);
}

@media (max-width: $breakpoint-lg) {
  .alumni-hero__inner,
  .alumni-shell,
  .board-header {
    grid-template-columns: 1fr;
  }
}

@media (max-width: $breakpoint-md) {
  .alumni-hero__inner {
    margin: 0 16px;
    padding: 32px 24px;
  }

  .hero-stats {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .alumni-page {
    padding-bottom: 36px;
  }

  .alumni-hero {
    padding-top: 18px;
  }

  .alumni-hero__inner {
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

  .alumni-shell {
    gap: 16px;
    padding-top: 18px;
  }

  .rail-card,
  .alumni-board {
    padding: 18px;
    border-radius: 18px;
  }

  .search-bar {
    grid-template-columns: 1fr;
  }

  .generation-group {
    padding: 16px;
    border-radius: 16px;
  }

  .generation-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .alumni-grid {
    grid-template-columns: 1fr;
  }

  .alumni-card {
    padding: 14px;
  }
}
</style>
