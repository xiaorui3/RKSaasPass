<template>
  <div class="achievements-page">
    <div class="page-header">
      <h1>{{ t('pages.achievements.title') }}</h1>
      <p>{{ t('pages.achievements.subtitle') }}</p>
    </div>

    <div class="container">
      <div class="stats-section">
        <div class="stats-grid">
          <div v-for="stat in statistics" :key="stat.label" class="stat-card">
            <div class="stat-icon" :style="{ background: stat.gradient }">
              <span class="icon-text">{{ stat.icon }}</span>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-label">{{ stat.label }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="filter-section">
        <div class="filter-row">
          <el-input
            v-model="searchKeyword"
            :placeholder="t('pages.achievements.searchPlaceholder')"
            prefix-icon="Search"
            clearable
            class="search-input"
            @keyup.enter="handleSearch"
          />
          <el-select v-model="currentType" :placeholder="t('pages.achievements.typePlaceholder')" clearable style="width: 140px" @change="handleFilter">
            <el-option :label="t('pages.achievements.all')" value="" />
            <el-option :label="t('pages.achievements.competitionAward')" :value="1" />
            <el-option :label="t('pages.achievements.projectResult')" :value="2" />
            <el-option :label="t('pages.achievements.paper')" :value="3" />
            <el-option :label="t('pages.achievements.patent')" :value="4" />
          </el-select>
          <el-select v-model="currentLevel" :placeholder="t('pages.achievements.levelPlaceholder')" clearable style="width: 140px" @change="handleFilter">
            <el-option :label="t('pages.achievements.all')" value="" />
            <el-option :label="t('pages.achievements.national')" :value="1" />
            <el-option :label="t('pages.achievements.provincial')" :value="2" />
            <el-option :label="t('pages.achievements.city')" :value="3" />
            <el-option :label="t('pages.achievements.school')" :value="4" />
          </el-select>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            {{ t('pages.achievements.search') }}
          </el-button>
        </div>
      </div>

      <div class="achievements-list" v-loading="loading">
        <div class="achievements-grid">
          <div
            v-for="achievement in achievementList"
            :key="achievement.id"
            class="achievement-card"
            @click="handleViewDetail(achievement)"
          >
            <div class="card-header" :class="getLevelClass(achievement.achievementLevel)">
              <div class="achievement-type">
                <el-tag :type="getTypeTagType(achievement.achievementType)" size="small" effect="dark">
                  {{ getTypeText(achievement.achievementType) }}
                </el-tag>
                <el-tag :type="getLevelTagType(achievement.achievementLevel)" size="small" effect="plain">
                  {{ getLevelText(achievement.achievementLevel) }}
                </el-tag>
              </div>
              <div class="achievement-date">{{ achievement.awardDate }}</div>
            </div>
            <div class="card-body">
              <h3 class="achievement-title">{{ achievement.achievementTitle }}</h3>
              <p class="achievement-desc">{{ achievement.description || t('pages.achievements.noDescription') }}</p>
              <div class="achievement-meta">
                <div class="meta-item">
                  <el-icon><Trophy /></el-icon>
                  <span>{{ achievement.awardOrganization || t('pages.achievements.unknownOrg') }}</span>
                </div>
                <div v-if="achievement.userName" class="meta-item">
                  <el-icon><User /></el-icon>
                  <span>{{ achievement.userName }}</span>
                </div>
              </div>
            </div>
            <div class="card-footer">
              <span class="view-detail">{{ t('pages.achievements.viewDetail') }}</span>
            </div>
          </div>
        </div>

        <el-empty v-if="!loading && achievementList.length === 0" :description="t('pages.achievements.noData')" />
      </div>

      <div v-if="total > pageSize" class="pagination-section">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[12, 24, 36, 48]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <el-dialog
      v-model="detailDialogVisible"
      :title="currentAchievement?.achievementTitle || t('pages.achievements.detailTitle')"
      width="700px"
      class="achievement-detail-dialog"
    >
      <div v-if="currentAchievement" class="detail-content">
        <div class="detail-header">
          <div class="detail-tags">
            <el-tag :type="getTypeTagType(currentAchievement.achievementType)" size="large">
              {{ getTypeText(currentAchievement.achievementType) }}
            </el-tag>
            <el-tag :type="getLevelTagType(currentAchievement.achievementLevel)" size="large">
              {{ getLevelText(currentAchievement.achievementLevel) }}
            </el-tag>
            <el-tag v-if="currentAchievement.status === 2" type="success" size="large">
              {{ t('pages.achievements.verified') }}
            </el-tag>
          </div>
        </div>

        <el-descriptions :column="2" border class="detail-info">
          <el-descriptions-item :label="t('pages.achievements.titleLabel')" :span="2">
            <span class="highlight-text">{{ currentAchievement.achievementTitle }}</span>
          </el-descriptions-item>
          <el-descriptions-item :label="t('pages.achievements.orgLabel')" :span="2">
            {{ currentAchievement.awardOrganization || '-' }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('pages.achievements.dateLabel')">
            {{ currentAchievement.awardDate || '-' }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('pages.achievements.ownerLabel')">
            {{ currentAchievement.userName || currentAchievement.userId || '-' }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('pages.achievements.descLabel')" :span="2">
            <div class="description-text">
              {{ currentAchievement.description || t('pages.achievements.descFallback') }}
            </div>
          </el-descriptions-item>
          <el-descriptions-item
            v-if="currentAchievement.proofImages && currentAchievement.proofImages.length > 0"
            :label="t('pages.achievements.proofLabel')"
            :span="2"
          >
            <div class="proof-images">
              <el-image
                v-for="(img, index) in currentAchievement.proofImages"
                :key="index"
                :src="img"
                :preview-src-list="currentAchievement.proofImages"
                fit="cover"
                class="proof-image"
              />
            </div>
          </el-descriptions-item>
          <el-descriptions-item :label="t('pages.achievements.createTimeLabel')">
            {{ currentAchievement.createTime }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('pages.achievements.statusLabel')">
            <el-tag :type="getStatusTagType(currentAchievement.status)">
              {{ getStatusText(currentAchievement.status) }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">{{ t('pages.achievements.close') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Search, Trophy, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  getAchievementList,
  getAchievementStatistics,
  getAchievementTypeText,
  getAchievementLevelText,
  getAchievementStatusText,
  ACHIEVEMENT_TYPE,
  ACHIEVEMENT_LEVEL,
  ACHIEVEMENT_STATUS
} from '@/api/achievement'

const { t } = useI18n()
const loading = ref(false)
const achievementList = ref([])
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)
const searchKeyword = ref('')
const currentType = ref('')
const currentLevel = ref('')
const detailDialogVisible = ref(false)
const currentAchievement = ref(null)

const statsData = ref({
  total: 0,
  competition: 0,
  project: 0,
  paper: 0,
  patent: 0,
  national: 0,
  provincial: 0
})

const statistics = computed(() => [
  {
    label: t('pages.achievements.total'),
    value: statsData.value.total,
    icon: '总',
    gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
  },
  {
    label: t('pages.achievements.competition'),
    value: statsData.value.competition,
    icon: '赛',
    gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)'
  },
  {
    label: t('pages.achievements.project'),
    value: statsData.value.project,
    icon: '项',
    gradient: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)'
  },
  {
    label: t('pages.achievements.paperPatent'),
    value: statsData.value.paper + statsData.value.patent,
    icon: '研',
    gradient: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)'
  }
])

const getTypeText = (type) => getAchievementTypeText(type) || t('pages.achievements.other')
const getLevelText = (level) => getAchievementLevelText(level) || t('pages.achievements.other')
const getStatusText = (status) => getAchievementStatusText(status) || t('pages.achievements.unknown')

const getTypeTagType = (type) => ({
  [ACHIEVEMENT_TYPE.COMPETITION]: 'primary',
  [ACHIEVEMENT_TYPE.PROJECT]: 'success',
  [ACHIEVEMENT_TYPE.PAPER]: 'warning',
  [ACHIEVEMENT_TYPE.PATENT]: 'info'
}[type] || 'info')

const getLevelTagType = (level) => ({
  [ACHIEVEMENT_LEVEL.NATIONAL]: 'danger',
  [ACHIEVEMENT_LEVEL.PROVINCIAL]: 'warning',
  [ACHIEVEMENT_LEVEL.CITY]: 'primary',
  [ACHIEVEMENT_LEVEL.SCHOOL]: 'info'
}[level] || 'info')

const getStatusTagType = (status) => ({
  [ACHIEVEMENT_STATUS.PENDING]: 'warning',
  [ACHIEVEMENT_STATUS.APPROVED]: 'success',
  [ACHIEVEMENT_STATUS.REJECTED]: 'danger'
}[status] || 'info')

const getLevelClass = (level) => ({
  [ACHIEVEMENT_LEVEL.NATIONAL]: 'level-national',
  [ACHIEVEMENT_LEVEL.PROVINCIAL]: 'level-provincial',
  [ACHIEVEMENT_LEVEL.CITY]: 'level-city',
  [ACHIEVEMENT_LEVEL.SCHOOL]: 'level-school'
}[level] || '')

const parseProofImages = (images) => {
  if (!images) return []
  if (Array.isArray(images)) return images
  try {
    return JSON.parse(images)
  } catch {
    return []
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.split(' ')[0].split('T')[0]
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  return dateTime.replace('T', ' ').substring(0, 19)
}

const fetchAchievements = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value,
      achievementType: currentType.value,
      achievementLevel: currentLevel.value,
      status: ACHIEVEMENT_STATUS.APPROVED
    }
    const res = await getAchievementList(params)
    const rawData = Array.isArray(res.data?.records)
      ? res.data.records
      : (Array.isArray(res.data) ? res.data : [])
    achievementList.value = rawData.map((item) => ({
      ...item,
      proofImages: parseProofImages(item.proofImages),
      awardDate: formatDate(item.awardDate),
      createTime: formatDateTime(item.createTime)
    }))
    total.value = res.data?.total || achievementList.value.length
  } catch (error) {
    achievementList.value = []
    total.value = 0
    ElMessage.error(error?.message || t('common.error'))
  } finally {
    loading.value = false
  }
}

const fetchStatistics = async () => {
  try {
    const res = await getAchievementStatistics()
    statsData.value = res.data || statsData.value
  } catch {
    statsData.value = {
      total: achievementList.value.length,
      competition: achievementList.value.filter((item) => item.achievementType === ACHIEVEMENT_TYPE.COMPETITION).length,
      project: achievementList.value.filter((item) => item.achievementType === ACHIEVEMENT_TYPE.PROJECT).length,
      paper: achievementList.value.filter((item) => item.achievementType === ACHIEVEMENT_TYPE.PAPER).length,
      patent: achievementList.value.filter((item) => item.achievementType === ACHIEVEMENT_TYPE.PATENT).length,
      national: achievementList.value.filter((item) => item.achievementLevel === ACHIEVEMENT_LEVEL.NATIONAL).length,
      provincial: achievementList.value.filter((item) => item.achievementLevel === ACHIEVEMENT_LEVEL.PROVINCIAL).length
    }
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchAchievements()
}

const handleFilter = () => {
  currentPage.value = 1
  fetchAchievements()
}

const handleViewDetail = (achievement) => {
  currentAchievement.value = achievement
  detailDialogVisible.value = true
}

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  fetchAchievements()
}

const handlePageChange = (val) => {
  currentPage.value = val
  fetchAchievements()
}

onMounted(() => {
  fetchAchievements()
  fetchStatistics()
})
</script>

<style lang="scss" scoped>
@use '../styles/variables.scss' as *;
@use '../styles/mixins.scss' as *;

.achievements-page {
  padding: 40px 0;
  min-height: 100vh;
  background: linear-gradient(180deg, #f8fafc 0%, #ffffff 100%);
}

.stats-section {
  margin-bottom: 32px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: white;
  padding: 20px 24px;
  border-radius: $border-radius-lg;
  box-shadow: $shadow-sm;
  transition: all $transition-base;

  &:hover {
    transform: translateY(-4px);
    box-shadow: $shadow-lg;
  }
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: $border-radius-base;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  .icon-text {
    font-size: 28px;
    color: #fff;
    font-weight: 800;
  }
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 800;
  color: $text-primary;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: $text-secondary;
  margin-top: 4px;
}

.filter-section {
  background: white;
  padding: 20px 24px;
  border-radius: $border-radius-lg;
  box-shadow: $shadow-sm;
  margin-bottom: 24px;
}

.filter-row {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.search-input {
  flex: 1;
  min-width: 200px;
}

.achievements-list {
  margin-bottom: 24px;
}

.achievements-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.achievement-card {
  background: white;
  border-radius: $border-radius-lg;
  overflow: hidden;
  box-shadow: $shadow-sm;
  cursor: pointer;
  transition: all $transition-base;
  display: flex;
  flex-direction: column;

  &:hover {
    transform: translateY(-6px);
    box-shadow: $shadow-xl;

    .card-footer {
      background: $primary-color;
      color: white;
    }
  }

  .card-header {
    padding: 16px 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
    border-bottom: 3px solid transparent;

    &.level-national {
      border-bottom-color: #ef4444;
    }

    &.level-provincial {
      border-bottom-color: #f59e0b;
    }

    &.level-city {
      border-bottom-color: #3b82f6;
    }

    &.level-school {
      border-bottom-color: #10b981;
    }
  }

  .achievement-type {
    display: flex;
    gap: 8px;
  }

  .achievement-date {
    font-size: 13px;
    color: $text-secondary;
    font-weight: 500;
  }

  .card-body {
    padding: 20px;
    flex: 1;
  }

  .achievement-title {
    font-size: 17px;
    font-weight: 700;
    color: $text-primary;
    margin: 0 0 12px;
    line-height: 1.4;
    @include text-ellipsis(2);
    min-height: 48px;
  }

  .achievement-desc {
    font-size: 14px;
    color: $text-secondary;
    margin: 0 0 16px;
    line-height: 1.6;
    @include text-ellipsis(3);
    min-height: 67px;
  }

  .achievement-meta {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .meta-item {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;
    color: $text-secondary;

    .el-icon {
      color: $secondary-color;
    }
  }

  .card-footer {
    padding: 14px 20px;
    background: #f8fafc;
    text-align: center;
    transition: all $transition-base;

    .view-detail {
      font-size: 14px;
      font-weight: 600;
      color: $primary-color;
    }
  }
}

.pagination-section {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

.achievement-detail-dialog {
  .detail-header {
    margin-bottom: 24px;
  }

  .detail-tags {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
  }

  .detail-info {
    .highlight-text {
      font-size: 18px;
      font-weight: 700;
      color: $primary-color;
    }

    .description-text {
      line-height: 1.8;
      color: $text-secondary;
    }
  }

  .proof-images {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;

    .proof-image {
      width: 120px;
      height: 120px;
      border-radius: $border-radius-base;
      cursor: pointer;
      transition: transform $transition-base;

      &:hover {
        transform: scale(1.05);
      }
    }
  }
}

@media (max-width: $breakpoint-lg) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .achievements-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: $breakpoint-md) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .achievements-grid {
    grid-template-columns: 1fr;
  }

  .filter-row {
    flex-direction: column;
    align-items: stretch;

    .search-input {
      width: 100%;
    }

    .el-select {
      width: 100% !important;
    }
  }
}
</style>
