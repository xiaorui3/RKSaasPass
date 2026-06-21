<template>
  <div class="gallery-page">
    <el-card class="gallery-toolbar">
      <div class="toolbar-content">
        <div>
          <h2>活动相册</h2>
          <p>选择已创建活动后上传照片，照片记录会保存到后端并关联到对应活动。</p>
        </div>
        <div class="toolbar-actions">
          <el-select
            v-model="selectedActivityId"
            placeholder="请选择活动"
            filterable
            class="activity-select"
            @change="fetchPhotos"
          >
            <el-option
              v-for="activity in activityOptions"
              :key="activity.id"
              :label="formatActivityOption(activity)"
              :value="activity.id"
            />
          </el-select>
          <el-upload
            :http-request="handleUpload"
            :show-file-list="false"
            accept="image/*"
            multiple
            :disabled="!selectedActivityId"
          >
            <el-button type="primary" :disabled="!selectedActivityId">上传照片</el-button>
          </el-upload>
          <el-button type="danger" :disabled="!selectedPhotos.length" @click="handleBatchDelete">
            批量删除
          </el-button>
        </div>
      </div>
      <div class="gallery-filters">
        <el-input
          v-model.trim="filters.keyword"
          class="filter-keyword"
          clearable
          placeholder="按活动名称、组织者、地点、日期模糊搜索"
          @keyup.enter="fetchActivities"
          @clear="fetchActivities"
        />
        <el-date-picker
          v-model="filters.dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          range-separator="至"
          class="filter-date"
          @change="fetchActivities"
        />
        <el-select
          v-model="filters.status"
          class="filter-status"
          clearable
          placeholder="活动状态"
          @change="fetchActivities"
        >
          <el-option label="未开始" :value="1" />
          <el-option label="报名中" :value="2" />
          <el-option label="进行中" :value="3" />
          <el-option label="已结束" :value="4" />
        </el-select>
        <el-select
          v-model="filters.recentMode"
          class="filter-recent"
          placeholder="近期范围"
          @change="fetchActivities"
        >
          <el-option label="全部活动" value="all" />
          <el-option label="近 7 天" value="recent7" />
          <el-option label="近 30 天" value="recent30" />
          <el-option label="近 90 天" value="recent90" />
        </el-select>
        <el-button type="primary" @click="fetchActivities">搜索</el-button>
        <el-button @click="resetFilters">重置</el-button>
        <span class="filter-summary">匹配 {{ activityOptions.length }} 个活动</span>
      </div>
    </el-card>

    <div v-loading="loading || uploading" class="waterfall-grid">
      <div
        v-for="photo in photos"
        :key="photo.id"
        class="photo-card"
        :class="{ selected: isPhotoSelected(photo) }"
      >
        <label class="photo-selector" @click.stop>
          <el-checkbox :model-value="isPhotoSelected(photo)" @change="() => togglePhotoSelection(photo)" />
        </label>
        <img :src="photo.previewUrl" :alt="photo.fileName || '活动照片'" loading="lazy" @click="preview(photo)" />
        <div class="photo-overlay">
          <span>{{ photo.fileName || '活动照片' }}</span>
          <el-button type="danger" size="small" circle @click.stop="removePhoto(photo)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
    </div>

    <el-empty
      v-if="photos.length === 0 && !loading && !uploading"
      :description="activityOptions.length === 0 ? '没有匹配的活动' : (selectedActivityId ? '当前活动暂无照片' : '请先选择活动')"
    />

    <el-dialog v-model="previewVisible" width="700px" destroy-on-close>
      <img
        v-if="previewPhoto"
        :src="previewPhoto.previewUrl"
        :alt="previewPhoto.fileName || '预览'"
        style="width: 100%;"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import {
  createActivityAlbum,
  deleteActivityAlbum,
  getActivityAlbums,
  getAdminActivityPage
} from '@/api/activity'
import { uploadManagedFile } from '@/utils/fileUpload'
import { resolveMediaUrl } from '@/utils/mediaUrl'
import { buildGalleryActivityQuery, filterGalleryActivities } from '@/utils/activityGallery'

const activityOptions = ref([])
const selectedActivityId = ref(null)
const photos = ref([])
const loading = ref(false)
const uploading = ref(false)
const previewVisible = ref(false)
const previewPhoto = ref(null)
const selectedPhotos = ref([])
const filters = reactive({
  keyword: '',
  dateRange: [],
  status: '',
  recentMode: 'all',
  size: 500
})

function formatActivityDate(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 10)
}

function formatActivityOption(activity = {}) {
  const name = activity.activityName || activity.title || `活动 ${activity.id}`
  const date = formatActivityDate(activity.startTime)
  const location = activity.location ? ` / ${activity.location}` : ''
  return `${date ? `${date} · ` : ''}${name}${location}`
}

function normalizePhoto(photo) {
  const storedValue = photo.photoUrl || photo.url || ''
  return {
    ...photo,
    previewUrl: resolveMediaUrl(storedValue)
  }
}

function preview(photo) {
  previewPhoto.value = photo
  previewVisible.value = true
}

async function fetchActivities() {
  loading.value = true
  try {
    const res = await getAdminActivityPage(buildGalleryActivityQuery(filters))
    activityOptions.value = filterGalleryActivities(res.data?.records || [], filters)
    const selectedStillVisible = activityOptions.value.some((item) => item.id === selectedActivityId.value)
    if (activityOptions.value.length === 0) {
      selectedActivityId.value = null
      photos.value = []
      selectedPhotos.value = []
      return
    }
    if (!selectedActivityId.value || !selectedStillVisible) {
      selectedActivityId.value = activityOptions.value[0].id
      await fetchPhotos()
    }
  } catch (error) {
    console.error('获取活动列表失败:', error)
    ElMessage.error('获取活动列表失败')
  } finally {
    loading.value = false
  }
}

async function resetFilters() {
  filters.keyword = ''
  filters.dateRange = []
  filters.status = ''
  filters.recentMode = 'all'
  await fetchActivities()
}

async function fetchPhotos() {
  if (!selectedActivityId.value) {
    photos.value = []
    return
  }
  loading.value = true
  try {
    const res = await getActivityAlbums(selectedActivityId.value)
    photos.value = (Array.isArray(res.data) ? res.data : []).map(normalizePhoto)
    selectedPhotos.value = selectedPhotos.value.filter((selected) =>
      photos.value.some((photo) => photo.id === selected.id)
    )
  } catch (error) {
    console.error('获取活动相册失败:', error)
    photos.value = []
    selectedPhotos.value = []
    ElMessage.error('获取活动相册失败')
  } finally {
    loading.value = false
  }
}

function isPhotoSelected(photo) {
  return selectedPhotos.value.some((item) => item.id === photo.id)
}

function togglePhotoSelection(photo) {
  if (isPhotoSelected(photo)) {
    selectedPhotos.value = selectedPhotos.value.filter((item) => item.id !== photo.id)
    return
  }
  selectedPhotos.value = [...selectedPhotos.value, photo]
}

async function handleUpload({ file }) {
  if (!selectedActivityId.value) {
    ElMessage.warning('请先选择活动')
    return
  }
  uploading.value = true
  try {
    const { storedValue, url } = await uploadManagedFile({ file }, 'gallery')
    const res = await createActivityAlbum(selectedActivityId.value, {
      photoUrl: storedValue || url,
      fileName: file.name,
      sortOrder: photos.value.length
    })
    if (res.code === 200) {
      await fetchPhotos()
      ElMessage.success('上传成功')
      return
    }
    ElMessage.error(res.msg || '保存相册失败')
  } catch (error) {
    console.error('上传活动照片失败:', error)
    ElMessage.error(error.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

async function removePhoto(photo) {
  try {
    await ElMessageBox.confirm('确定删除这张照片吗？', '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await deleteActivityAlbum(photo.id)
    if (res.code === 200) {
      await fetchPhotos()
      ElMessage.success('已删除')
      return
    }
    ElMessage.error(res.msg || '删除失败')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

async function handleBatchDelete() {
  if (!selectedPhotos.value.length) {
    ElMessage.warning('请先选择要删除的照片')
    return
  }

  try {
    const photosToDelete = [...selectedPhotos.value]
    await ElMessageBox.confirm(
      `确定删除选中的 ${photosToDelete.length} 张照片吗？删除后将无法恢复。`,
      '批量删除活动照片',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const results = await Promise.allSettled(photosToDelete.map((photo) => deleteActivityAlbum(photo.id)))
    const successCount = results.filter((result) => result.status === 'fulfilled').length
    const failureCount = results.length - successCount

    selectedPhotos.value = []
    await fetchPhotos()

    if (failureCount > 0) {
      ElMessage.warning(`已删除 ${successCount} 张，${failureCount} 张删除失败`)
      return
    }
    ElMessage.success(`已删除 ${successCount} 张照片`)
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '批量删除失败')
    }
  }
}

onMounted(fetchActivities)
</script>

<style scoped>
.gallery-page {
  padding: 20px;
}

.gallery-toolbar {
  margin-bottom: 20px;
}

.toolbar-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.toolbar-content h2 {
  margin: 0;
}

.toolbar-content p {
  margin: 8px 0 0;
  color: #64748b;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.activity-select {
  width: 320px;
}

.gallery-filters {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.filter-keyword {
  width: 320px;
}

.filter-date {
  width: 300px;
}

.filter-status {
  width: 150px;
}

.filter-recent {
  width: 130px;
}

.filter-summary {
  color: #64748b;
  font-size: 13px;
}

.waterfall-grid {
  columns: 3;
  column-gap: 16px;
}

.photo-card {
  break-inside: avoid;
  margin-bottom: 16px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #ebeef5;
  position: relative;
  cursor: pointer;
  transition: transform 0.2s;
}

.photo-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.photo-card.selected {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.18);
}

.photo-selector {
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 3px 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.18);
}

.photo-card img {
  width: 100%;
  display: block;
}

.photo-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 8px 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #fff;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.6));
  opacity: 0;
  transition: opacity 0.2s;
}

.photo-card:hover .photo-overlay {
  opacity: 1;
}

@media (max-width: 768px) {
  .toolbar-content {
    flex-direction: column;
  }

  .toolbar-actions,
  .activity-select,
  .filter-keyword,
  .filter-date,
  .filter-status,
  .filter-recent {
    width: 100%;
  }

  .waterfall-grid {
    columns: 2;
  }
}
</style>
