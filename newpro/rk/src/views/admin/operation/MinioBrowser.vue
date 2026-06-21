<template>
  <div class="minio-browser-page">
    <section class="page-head">
      <div>
        <p class="eyebrow">Object Storage</p>
        <h1>MinIO 存储浏览器</h1>
        <p>按 bucket 和前缀查看当前对象存储内容，可直接下载 APK、图片、备份包等对象。</p>
      </div>
      <div class="head-actions">
        <el-button :loading="bucketLoading" @click="fetchBuckets">刷新 Bucket</el-button>
        <el-button type="primary" :loading="objectLoading" :disabled="!selectedBucket" @click="fetchObjects">刷新对象</el-button>
      </div>
    </section>

    <section class="browser-layout">
      <aside class="bucket-panel">
        <div class="panel-title">Bucket</div>
        <el-empty v-if="!buckets.length && !bucketLoading" description="暂无 Bucket" :image-size="90" />
        <button
          v-for="bucket in buckets"
          :key="bucket.name"
          class="bucket-item"
          :class="{ active: selectedBucket === bucket.name }"
          type="button"
          @click="selectBucket(bucket)"
        >
          <span class="bucket-name">{{ bucket.name }}</span>
          <span class="bucket-meta">{{ bucket.objectCount || 0 }} 个对象 / {{ bucket.totalSize || '-' }}</span>
        </button>
      </aside>

      <main class="object-panel">
        <div class="object-toolbar">
          <el-input v-model.trim="query.prefix" clearable placeholder="对象前缀，例如 mobile-release/" @keyup.enter="fetchObjects" />
          <el-input-number v-model="query.limit" :min="20" :max="1000" :step="20" controls-position="right" />
          <el-switch v-model="query.recursive" active-text="递归" inactive-text="当前层" />
          <el-button type="primary" :disabled="!selectedBucket" :loading="objectLoading" @click="fetchObjects">查询</el-button>
        </div>

        <el-table :data="objects" v-loading="objectLoading" stripe border empty-text="暂无对象">
          <el-table-column prop="objectName" label="对象名" min-width="280" show-overflow-tooltip />
          <el-table-column prop="size" label="大小" width="120" />
          <el-table-column prop="lastModified" label="更新时间" min-width="190" show-overflow-tooltip />
          <el-table-column label="下载地址" min-width="260">
            <template #default="{ row }">
              <el-tooltip
                :content="objectDownloadUrl(row)"
                placement="top"
                :disabled="!objectDownloadUrl(row)"
              >
                <span class="download-url">{{ objectDownloadUrl(row) }}</span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="handleDownloadObject(row)">下载</el-button>
            </template>
          </el-table-column>
        </el-table>
      </main>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  downloadMinioObject,
  getMinioObjectDownloadUrl,
  listMinioBuckets,
  listMinioObjects
} from '@/api/admin-ops'

const bucketLoading = ref(false)
const objectLoading = ref(false)
const buckets = ref([])
const objects = ref([])
const selectedBucket = ref('')
const query = reactive({
  prefix: '',
  recursive: true,
  limit: 200
})

async function fetchBuckets() {
  bucketLoading.value = true
  try {
    const res = await listMinioBuckets()
    if (res.code !== 200 || !Array.isArray(res.data)) {
      throw new Error(res.msg || '读取 MinIO Bucket 失败')
    }
    buckets.value = res.data
    if (!selectedBucket.value && buckets.value.length) {
      selectedBucket.value = buckets.value[0].name
      await fetchObjects()
    }
  } catch (error) {
    ElMessage.error(error.message || '读取 MinIO Bucket 失败')
  } finally {
    bucketLoading.value = false
  }
}

function selectBucket(bucket) {
  selectedBucket.value = bucket?.name || ''
  objects.value = []
  if (selectedBucket.value) {
    fetchObjects()
  }
}

async function fetchObjects() {
  if (!selectedBucket.value) {
    return
  }
  objectLoading.value = true
  try {
    const res = await listMinioObjects({
      bucket: selectedBucket.value,
      prefix: query.prefix || undefined,
      recursive: query.recursive,
      limit: query.limit
    })
    if (res.code !== 200 || !Array.isArray(res.data)) {
      throw new Error(res.msg || '读取 MinIO 对象失败')
    }
    objects.value = res.data
  } catch (error) {
    ElMessage.error(error.message || '读取 MinIO 对象失败')
  } finally {
    objectLoading.value = false
  }
}

function objectDownloadUrl(row) {
  return row?.downloadUrl || getMinioObjectDownloadUrl(row?.bucket || selectedBucket.value, row?.objectName || '')
}

async function handleDownloadObject(row) {
  if (!row?.objectName) {
    ElMessage.warning('对象名为空，无法下载')
    return
  }
  try {
    const blob = await downloadMinioObject(row.bucket || selectedBucket.value, row.objectName)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = row.fileName || row.objectName.split('/').pop() || 'minio-object'
    link.click()
    URL.revokeObjectURL(url)
  } catch (error) {
    ElMessage.error(error.message || '下载 MinIO 对象失败')
  }
}

onMounted(fetchBuckets)
</script>

<style scoped>
.minio-browser-page {
  display: grid;
  gap: 18px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  padding: 18px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.eyebrow {
  margin: 0 0 6px;
  color: #0f766e;
  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
}

.page-head h1 {
  margin: 0;
  font-size: 22px;
  color: #111827;
}

.page-head p {
  margin: 8px 0 0;
  color: #667085;
}

.head-actions,
.object-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.browser-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 16px;
}

.bucket-panel,
.object-panel {
  min-height: 520px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.panel-title {
  margin-bottom: 12px;
  font-weight: 700;
  color: #111827;
}

.bucket-item {
  display: grid;
  width: 100%;
  gap: 6px;
  margin-bottom: 8px;
  padding: 12px;
  text-align: left;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
}

.bucket-item.active {
  border-color: #0f766e;
  background: #ecfdf5;
}

.bucket-name {
  font-weight: 700;
  color: #111827;
}

.bucket-meta,
.download-url {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  color: #667085;
  font-size: 12px;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}

.object-toolbar {
  margin-bottom: 12px;
}

.object-toolbar .el-input {
  max-width: 360px;
}

@media (max-width: 900px) {
  .page-head,
  .browser-layout {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
