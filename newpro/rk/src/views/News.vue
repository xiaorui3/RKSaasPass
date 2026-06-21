<template>
  <div class="news-page design-news-reference-page" data-testid="portal-news-page">
    <PortalHero
      class="design-hero-news"
      eyebrow="新闻动态"
      title="新闻中心"
      description="了解社团最新动态、校园资讯和活动报道，按分类快速浏览重点内容。"
      :metrics="heroMetrics"
      :band-eyebrow="adminEnabled ? '内容管理入口' : '内容发布状态'"
      :band-title="heroBandTitle"
      :band-description="heroBandDescription"
    >
      <template #actions>
        <router-link class="hero-link primary" to="/about">社团简介</router-link>
        <router-link class="hero-link secondary" to="/contact">联系我们</router-link>
      </template>
    </PortalHero>

    <main class="container news-shell">
      <PortalContentFilterBar
        class="design-category-pills"
        eyebrow="内容筛选"
        title="按标题、分类、发布状态和作者筛选新闻动态"
        description="按照 6 月 7 日设计图补齐新闻概况页的筛选条、卡片列表和右侧概览栏，数据继续读取现有新闻接口。"
        :tags="activeFilterTags"
      >
        <template #actions>
          <el-button text @click="handleReset">重置筛选</el-button>
        </template>

        <div class="filter-grid">
          <div class="filter-field filter-field-wide">
            <span class="filter-label">标题</span>
            <el-input
              v-model="searchForm.title"
              placeholder="请输入新闻标题"
              clearable
              @keyup.enter="handleSearch"
            />
          </div>

          <div class="filter-field">
            <span class="filter-label">分类</span>
            <el-select v-model="searchForm.category" placeholder="全部分类" clearable>
              <el-option
                v-for="item in categoryOptions"
                :key="item"
                :label="item"
                :value="item"
              />
            </el-select>
          </div>

          <div class="filter-field">
            <span class="filter-label">状态</span>
            <el-select v-model="searchForm.status" placeholder="全部状态" clearable>
              <el-option label="草稿" value="0" />
              <el-option label="已发布" value="1" />
              <el-option label="已下线" value="2" />
            </el-select>
          </div>

          <div class="filter-field">
            <span class="filter-label">作者</span>
            <el-input
              v-model="searchForm.author"
              placeholder="请输入作者"
              clearable
              @keyup.enter="handleSearch"
            />
          </div>

          <div class="filter-actions-panel">
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
            <el-button @click="handleReset">清空</el-button>
          </div>
        </div>

        <template #summary>
          <span class="summary-copy">
            当前共 {{ total || newsList.length }} 条新闻记录。
          </span>
          <span class="summary-copy">
            当前第 {{ currentPage }} 页，已加载 {{ newsList.length }} 条内容。
          </span>
        </template>
      </PortalContentFilterBar>

      <PortalContentFeatureCard
        v-if="featuredNews"
        eyebrow="重点推荐"
        :title="featuredNews.title"
        :description="getNewsExcerpt(featuredNews, 220)"
      >
        <template #meta>
          <el-tag v-if="featuredNews.category" size="small" type="primary">
            {{ featuredNews.category }}
          </el-tag>
          <el-tag v-if="isFeatured(featuredNews)" size="small" type="warning">置顶</el-tag>
          <el-tag v-if="isShared(featuredNews)" size="small" type="success">共享</el-tag>
          <span class="inline-meta">
            <el-icon><Calendar /></el-icon>
            {{ formatDateTime(featuredNews.publishTime || featuredNews.createTime) }}
          </span>
        </template>

        <div class="feature-copy">
          <p class="feature-note">
            {{ getNewsExcerpt(featuredNews, 320) }}
          </p>

          <div class="feature-stat-row">
            <span>
              <el-icon><User /></el-icon>
              {{ featuredNews.author || '新闻中心' }}
            </span>
            <span>
              <el-icon><View /></el-icon>
              浏览 {{ featuredNews.viewCount || 0 }}
            </span>
          </div>
        </div>

        <template #actions>
          <el-button type="primary" @click="goToDetail(featuredNews.id)">
            查看详情
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </template>

        <template #media>
          <el-image
            v-if="featuredNews.coverImage"
            :src="resolveMediaUrl(featuredNews.coverImage)"
            fit="cover"
            class="feature-image"
          >
            <template #error>
              <div class="feature-placeholder">
                <el-icon><Memo /></el-icon>
              </div>
            </template>
          </el-image>
          <div v-else class="feature-placeholder">
            <el-icon><Memo /></el-icon>
          </div>
        </template>

        <template #aside>
          <div class="feature-aside-block">
            <p class="feature-aside-label">栏目摘要</p>
            <h4>{{ featuredNews.category || '综合资讯' }}</h4>
            <p>
              保留原有详情路由，并将重点新闻以前台门户推荐位的形式优先展示。
            </p>
          </div>

          <div class="feature-aside-stats">
            <span>{{ total || newsList.length }} 条在列内容</span>
            <span>{{ sharedNewsList.length }} 条共享内容</span>
          </div>
        </template>
      </PortalContentFeatureCard>

      <section class="surface-panel timeline-panel" data-testid="portal-news-timeline">
        <PortalSectionHeader
          eyebrow="新闻列表"
          title="新闻动态列表"
          :description="timelineDescription"
        >
          <template #trailing>
            <el-button text @click="handleReset">重置筛选</el-button>
          </template>
        </PortalSectionHeader>

        <div v-if="loading" class="loading-container">
          <el-icon class="loading-icon"><Loading /></el-icon>
          <p>正在加载新闻列表...</p>
        </div>

        <div v-else-if="newsList.length === 0" class="empty-container">
          <el-empty description="当前筛选条件下暂无新闻记录。">
            <el-button type="primary" @click="handleReset">查看全部新闻</el-button>
          </el-empty>
        </div>

        <div v-else class="timeline-layout">
          <div class="timeline-list">
            <button
              v-for="item in newsList"
              :key="item.id"
              type="button"
              class="article-card design-horizontal-news-card"
              @click="goToDetail(item.id)"
            >
              <div class="article-card-media">
                <el-image
                  v-if="item.coverImage"
                  :src="resolveMediaUrl(item.coverImage)"
                  fit="cover"
                  :alt="item.title"
                >
                  <template #error>
                    <div class="article-card-placeholder">
                      <el-icon><Memo /></el-icon>
                    </div>
                  </template>
                </el-image>
                <div v-else class="article-card-placeholder">
                  <el-icon><Memo /></el-icon>
                </div>
              </div>

              <div class="article-card-body">
                <div class="article-card-top">
                  <div class="article-tags">
                    <el-tag v-if="item.category" size="small" type="primary">{{ item.category }}</el-tag>
                    <el-tag v-if="isFeatured(item)" size="small" type="warning">置顶</el-tag>
                    <el-tag v-if="isShared(item)" size="small" type="success">共享</el-tag>
                  </div>

                  <span class="article-time">
                    {{ formatDateTime(item.publishTime || item.createTime) }}
                  </span>
                </div>

                <h3>{{ item.title }}</h3>
                <p class="article-summary">{{ getNewsExcerpt(item, 180) }}</p>

                <div class="article-card-footer">
                  <div class="article-meta">
                    <span>
                      <el-icon><User /></el-icon>
                      {{ item.author || '新闻中心' }}
                    </span>
                    <span>
                      <el-icon><View /></el-icon>
                      {{ item.viewCount || 0 }}
                    </span>
                  </div>

                  <span class="article-cta">
                    阅读全文
                    <el-icon><ArrowRight /></el-icon>
                  </span>
                </div>
              </div>
            </button>
          </div>

          <aside class="timeline-rail">
            <PortalContentInfoCard
              eyebrow="分类视图"
              title="当前分类概览"
              description="统计结果基于当前已加载的公开新闻列表。"
              tone="soft"
            >
              <div class="category-list">
                <div
                  v-for="item in categoryBreakdown"
                  :key="item.name"
                  class="category-row"
                >
                  <span>{{ item.name }}</span>
                  <strong>{{ item.count }}</strong>
                </div>
              </div>
            </PortalContentInfoCard>

            <PortalContentInfoCard
              v-if="sharedNewsList.length"
              eyebrow="跨租户"
              title="共享新闻流"
              description="跨租户共享内容继续通过现有共享新闻接口提供。"
              tone="light"
            >
              <button
                v-for="item in sharedNewsPreview"
                :key="item.id"
                type="button"
                class="rail-link"
                @click="goToDetail(item.id)"
              >
                <span class="rail-link-title">{{ item.title }}</span>
                <span class="rail-link-meta">
                  {{ item.category || '共享内容' }} · {{ formatDate(item.publishTime || item.createTime) }}
                </span>
              </button>
            </PortalContentInfoCard>

            <PortalContentInfoCard
              v-else
              eyebrow="分发状态"
              title="共享新闻暂未启用"
              description="当前环境下暂无可用的共享新闻内容。"
              tone="contrast"
            >
              <p class="contrast-copy">
                当前门户已保留跨租户内容展示能力，但这次接口返回的共享新闻列表为空。
              </p>
            </PortalContentInfoCard>
          </aside>
        </div>

        <div class="pagination design-pagination-strip">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </section>

      <section v-if="adminEnabled" class="surface-panel operations-panel">
        <PortalSectionHeader
          eyebrow="管理操作"
          title="新闻管理台"
          description="当前路由继续保留权限控制下的新增、编辑、删除、导入与导出能力，同时与前台门户内容展示共存。"
        >
          <template #trailing>
            <span class="selection-pill">
              已选 {{ selectedRows.length }} 条
            </span>
          </template>
        </PortalSectionHeader>

        <div class="action-buttons">
          <el-button type="success" @click="handleAdd" v-if="canAdd()">
            <el-icon><Plus /></el-icon>
            新增
          </el-button>
          <el-button
            type="primary"
            @click="handleEdit"
            :disabled="selectedRows.length !== 1"
            v-if="canEdit()"
          >
            <el-icon><Edit /></el-icon>
            编辑
          </el-button>
          <el-button
            type="danger"
            @click="handleDelete"
            :disabled="selectedRows.length === 0"
            v-if="canRemove()"
          >
            <el-icon><Delete /></el-icon>
            删除
          </el-button>
          <el-button type="warning" @click="handleExport" v-if="canEdit()">
            <el-icon><Download /></el-icon>
            导出
          </el-button>
          <el-button type="info" @click="handleImport" v-if="canAdd()">
            <el-icon><Upload /></el-icon>
            导入
          </el-button>
        </div>

        <div class="table-container">
          <el-table
            :data="newsList"
            style="width: 100%"
            stripe
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="55" v-if="canEdit() || canRemove()" />
            <el-table-column prop="title" label="标题" min-width="260" show-overflow-tooltip />
            <el-table-column prop="category" label="分类" width="160" />
            <el-table-column prop="author" label="作者" width="140" />
            <el-table-column label="状态" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row)">
                  {{ statusLabel(row) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="置顶" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="isFeatured(row) ? 'warning' : 'info'">
                  {{ isFeatured(row) ? '是' : '否' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="viewCount" label="浏览量" width="100" align="center" />
            <el-table-column prop="publishTime" label="发布时间" width="180" />
            <el-table-column
              label="操作"
              width="180"
              align="center"
              fixed="right"
              v-if="canEdit() || canRemove()"
            >
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="handleEditRow(row)" v-if="canEdit()">
                  编辑
                </el-button>
                <el-button type="danger" size="small" @click="handleDeleteRow(row)" v-if="canRemove()">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </section>
    </main>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="800px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="formData.title" placeholder="请输入新闻标题" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-input v-model="formData.category" placeholder="请输入新闻分类" />
        </el-form-item>
        <el-form-item label="作者" prop="author">
          <el-input v-model="formData.author" placeholder="请输入作者名称" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio label="0">草稿</el-radio>
            <el-radio label="1">已发布</el-radio>
            <el-radio label="2">已下线</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="置顶" prop="isTop">
          <el-radio-group v-model="formData.isTop">
            <el-radio label="1">是</el-radio>
            <el-radio label="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="formData.content"
            type="textarea"
            :rows="8"
            placeholder="请输入新闻正文内容"
          />
        </el-form-item>
        <el-form-item label="发布时间" prop="publishTime">
          <el-date-picker
            v-model="formData.publishTime"
            type="datetime"
            placeholder="请选择发布时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importDialogVisible" title="导入新闻" width="500px">
      <div class="import-section">
        <el-button type="primary" @click="downloadTemplate" style="margin-bottom: 20px;">
          <el-icon><Download /></el-icon>
          下载模板
        </el-button>

        <el-upload
          ref="uploadRef"
          class="upload-demo"
          action="/ruoyi/news/importData"
          :on-success="handleUploadSuccess"
          :on-error="handleUploadError"
          :before-upload="beforeUpload"
          accept=".xlsx,.xls"
          :limit="1"
          :auto-upload="false"
        >
          <el-button type="success">选择文件</el-button>
          <template #tip>
            <div class="el-upload__tip">
              仅支持上传单个 `.xlsx` 或 `.xls` 文件，大小不超过 10 MB。
            </div>
          </template>
        </el-upload>
      </div>

      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUploadSubmit">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight,
  Calendar,
  Delete,
  Download,
  Edit,
  Loading,
  Memo,
  Plus,
  Search,
  Upload,
  User,
  View
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PortalHero from '@/components/portal/PortalHero.vue'
import PortalSectionHeader from '@/components/portal/PortalSectionHeader.vue'
import PortalContentFeatureCard from '@/components/portal-content/PortalContentFeatureCard.vue'
import PortalContentFilterBar from '@/components/portal-content/PortalContentFilterBar.vue'
import PortalContentInfoCard from '@/components/portal-content/PortalContentInfoCard.vue'
import {
  addNews,
  deleteNews,
  downloadNewsTemplate,
  exportNews,
  getNewsList,
  getSharedNews,
  updateNews
} from '@/api/news'
import { hasPermission } from '@/utils/permission'
import { resolveMediaUrl } from '@/utils/mediaUrl'

const router = useRouter()

const canAdd = () => hasPermission('content:news:add')
const canEdit = () => hasPermission('content:news:edit')
const canRemove = () => hasPermission('content:news:remove')

const adminEnabled = computed(() => canAdd() || canEdit() || canRemove())

const loading = ref(false)
const newsList = ref([])
const sharedNewsList = ref([])
const selectedRows = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = ref({
  title: '',
  category: '',
  status: '',
  author: ''
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增新闻')
const submitLoading = ref(false)
const importDialogVisible = ref(false)
const uploadRef = ref(null)
const formRef = ref(null)

function createDefaultForm() {
  return {
    id: null,
    title: '',
    category: '',
    author: '',
    status: '0',
    isTop: '0',
    content: '',
    publishTime: ''
  }
}

const formData = ref(createDefaultForm())

const formRules = {
  title: [{ required: true, message: '请输入新闻标题', trigger: 'blur' }],
  category: [{ required: true, message: '请输入新闻分类', trigger: 'blur' }],
  author: [{ required: true, message: '请输入作者名称', trigger: 'blur' }],
  content: [{ required: true, message: '请输入新闻正文内容', trigger: 'blur' }]
}

function normalizeNewsPayload(payload) {
  if (Array.isArray(payload?.records)) {
    return {
      records: payload.records,
      total: Number(payload.total || payload.records.length || 0)
    }
  }
  if (Array.isArray(payload)) {
    return {
      records: payload,
      total: payload.length
    }
  }
  return {
    records: [],
    total: 0
  }
}

function normalizeList(payload) {
  if (Array.isArray(payload?.records)) {
    return payload.records
  }
  if (Array.isArray(payload)) {
    return payload
  }
  return []
}

function stripHtml(value) {
  return String(value || '').replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim()
}

function getNewsExcerpt(item, maxLength = 160) {
  const source = item?.summary || item?.content || ''
  const clean = stripHtml(source)

  if (!clean) {
    return '当前新闻暂无摘要内容。'
  }

  return clean.length > maxLength ? `${clean.slice(0, maxLength)}...` : clean
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

function getStatusValue(row) {
  if (row?.status !== undefined && row?.status !== null && row?.status !== '') {
    return String(row.status)
  }
  if (row?.isPublished === 1 || row?.isPublished === '1' || row?.isPublished === true) {
    return '1'
  }
  return '0'
}

function isFeatured(row) {
  return row?.isFeatured === 1 || row?.isFeatured === '1' || row?.isFeatured === true || row?.isTop === '1'
}

function isShared(row) {
  return row?.isCrossTenant === 1 || row?.isCrossTenant === '1' || row?.isCrossTenant === true
}

function statusLabel(row) {
  const value = getStatusValue(row)
  if (value === '1') {
    return '已发布'
  }
  if (value === '2') {
    return '已下线'
  }
  return '草稿'
}

function statusTagType(row) {
  const value = getStatusValue(row)
  if (value === '1') {
    return 'success'
  }
  if (value === '2') {
    return 'danger'
  }
  return 'warning'
}

const featuredNews = computed(() => {
  return newsList.value.find((item) => isFeatured(item)) || newsList.value[0] || null
})

const sharedNewsPreview = computed(() => sharedNewsList.value.slice(0, 5))

const categoryOptions = computed(() => {
  return [...new Set(
    [...newsList.value, ...sharedNewsList.value]
      .map((item) => item?.category)
      .filter(Boolean)
  )].sort((left, right) => left.localeCompare(right))
})

const categoryBreakdown = computed(() => {
  const counts = new Map()
  for (const item of newsList.value) {
    const key = item.category || '未分类'
    counts.set(key, (counts.get(key) || 0) + 1)
  }
  return [...counts.entries()]
    .map(([name, count]) => ({ name, count }))
    .sort((left, right) => right.count - left.count)
    .slice(0, 6)
})

const activeFilterTags = computed(() => {
  const tags = []
  if (searchForm.value.title) {
    tags.push({ label: '标题', value: searchForm.value.title })
  }
  if (searchForm.value.category) {
    tags.push({ label: '分类', value: searchForm.value.category })
  }
  if (searchForm.value.status) {
    tags.push({
      label: '状态',
      value: searchForm.value.status === '1'
        ? '已发布'
        : searchForm.value.status === '2'
          ? '已下线'
          : '草稿'
    })
  }
  if (searchForm.value.author) {
    tags.push({ label: '作者', value: searchForm.value.author })
  }
  return tags
})

const heroMetrics = computed(() => {
  return [
    {
      label: '收录总数',
      value: String(total.value || newsList.value.length),
      detail: '来源于公开新闻列表接口的当前结果总量。'
    },
    {
      label: '当前页码',
      value: String(currentPage.value),
      detail: `当前页已加载 ${newsList.value.length} 条内容。`
    },
    {
      label: '共享新闻',
      value: String(sharedNewsList.value.length),
      detail: sharedNewsList.value.length ? '当前存在跨租户共享新闻。' : '当前未返回共享新闻内容。'
    }
  ]
})

const heroBandTitle = computed(() => {
  if (adminEnabled.value) {
    return '具备权限的编辑人员仍可在此页面完成新增、编辑、删除、导入和导出操作。'
  }
  return '前台页面聚焦新闻发现、信息层级和清晰的详情跳转体验。'
})

const heroBandDescription = computed(() => {
  if (adminEnabled.value) {
    return '管理控件仍受权限约束，并保留在公共内容展示区域下方，不会打断前台浏览体验。'
  }
  return '在不改动现有路由和接口的前提下，前台读者可获得更清晰的推荐区、筛选台与时间线布局。'
})

const timelineDescription = computed(() => {
  if (activeFilterTags.value.length) {
    return '下方时间线会根据当前筛选条件展示结果，同时保留既有分页行为。'
  }
  return '新闻列表采用更清晰的门户层级呈现，并为分类说明和共享内容分发预留展示空间。'
})

async function fetchNews() {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      ...searchForm.value
    }

    const [listRes, sharedRes] = await Promise.all([
      getNewsList(params),
      getSharedNews(10)
    ])

    const normalized = normalizeNewsPayload(listRes.data)
    newsList.value = normalized.records
    total.value = normalized.total
    sharedNewsList.value = normalizeList(sharedRes.data)
  } catch (error) {
    console.error('Failed to fetch news list:', error)
    ElMessage.error('获取新闻列表失败')
  } finally {
    loading.value = false
  }
}

function goToDetail(id) {
  router.push(`/news/${id}`)
}

function handleSearch() {
  currentPage.value = 1
  fetchNews()
}

function handleReset() {
  searchForm.value = {
    title: '',
    category: '',
    status: '',
    author: ''
  }
  currentPage.value = 1
  fetchNews()
}

function handleSelectionChange(selection) {
  selectedRows.value = selection
}

function handleAdd() {
  dialogTitle.value = '新增新闻'
  formData.value = createDefaultForm()
  dialogVisible.value = true
}

function handleEdit() {
  if (selectedRows.value.length !== 1) {
    ElMessage.warning('请选择一条需要编辑的新闻记录')
    return
  }
  handleEditRow(selectedRows.value[0])
}

function handleEditRow(row) {
  dialogTitle.value = '编辑新闻'
  formData.value = {
    ...createDefaultForm(),
    ...row,
    status: getStatusValue(row),
    isTop: isFeatured(row) ? '1' : '0'
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) {
    return
  }

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitLoading.value = true
  try {
    if (formData.value.id) {
      await updateNews(formData.value)
      ElMessage.success('新闻更新成功')
    } else {
      await addNews(formData.value)
      ElMessage.success('新闻创建成功')
    }
    dialogVisible.value = false
    fetchNews()
  } catch (error) {
    console.error('Failed to submit news:', error)
    ElMessage.error('提交新闻失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请至少选择一条需要删除的新闻记录')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认删除已选中的 ${selectedRows.value.length} 条新闻记录吗？`,
      '删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await Promise.all(selectedRows.value.map((row) => deleteNews(row.id)))
    ElMessage.success('已删除选中的新闻记录')
    fetchNews()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('Failed to delete selected news:', error)
      ElMessage.error('删除选中新闻失败')
    }
  }
}

async function handleDeleteRow(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除《${row.title}》吗？`,
      '删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await deleteNews(row.id)
    ElMessage.success('新闻删除成功')
    fetchNews()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('Failed to delete news row:', error)
      ElMessage.error('删除新闻失败')
    }
  }
}

async function handleExport() {
  try {
    ElMessage.info('正在准备导出...')
    const response = await exportNews(searchForm.value)
    const blob = new Blob([response], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `news-export-${Date.now()}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出完成')
  } catch (error) {
    console.error('Failed to export news:', error)
    ElMessage.error('导出新闻列表失败')
  }
}

function handleImport() {
  importDialogVisible.value = true
}

async function downloadTemplate() {
  try {
    ElMessage.info('正在下载模板...')
    const response = await downloadNewsTemplate()
    const blob = new Blob([response], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'news-import-template.xlsx'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('模板下载完成')
  } catch (error) {
    console.error('Failed to download the import template:', error)
    ElMessage.error('下载导入模板失败')
  }
}

function beforeUpload(file) {
  const isExcel = file.type === 'application/vnd.ms-excel'
    || file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  const isLt10M = file.size / 1024 / 1024 < 10

  if (!isExcel) {
    ElMessage.error('仅支持 Excel 文件')
    return false
  }
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过 10 MB')
    return false
  }
  return true
}

function handleUploadSubmit() {
  uploadRef.value?.submit()
}

function handleUploadSuccess(response) {
  if (response.code === 200) {
    ElMessage.success('导入完成')
    importDialogVisible.value = false
    fetchNews()
    return
  }
  ElMessage.error(response.msg || '导入失败')
}

function handleUploadError() {
  ElMessage.error('上传失败')
}

function handleSizeChange(value) {
  pageSize.value = value
  currentPage.value = 1
  fetchNews()
}

function handlePageChange(value) {
  currentPage.value = value
  fetchNews()
}

onMounted(() => {
  fetchNews()
})
</script>

<style lang="scss" scoped>
@use '../styles/variables.scss' as *;

.news-page {
  min-height: 100vh;
}

.news-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 24px;
  padding-bottom: 56px;
}

.news-shell > * {
  min-width: 0;
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

.filter-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) repeat(3, minmax(0, 0.75fr));
  gap: 16px;
  align-items: end;
}

.filter-field {
  display: grid;
  gap: 10px;
}

.filter-label {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: $text-light;
}

.filter-actions-panel {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  justify-content: flex-end;
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

.feature-image,
.feature-placeholder {
  width: 100%;
  height: 100%;
}

.feature-placeholder {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $text-light;
  font-size: 56px;
  background: linear-gradient(180deg, rgba(243, 239, 232, 0.84) 0%, rgba(233, 225, 214, 0.94) 100%);
}

.inline-meta,
.feature-stat-row span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: $text-secondary;
}

.feature-copy {
  display: grid;
  gap: 14px;
}

.feature-note {
  margin: 0;
  font-size: 15px;
  line-height: 1.85;
  color: $text-secondary;
}

.feature-stat-row {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
}

.feature-aside-block h4 {
  margin: 8px 0 10px;
  font-size: 22px;
  line-height: 1.2;
}

.feature-aside-block p,
.contrast-copy {
  margin: 0;
  font-size: 14px;
  line-height: 1.75;
  color: rgba(255, 255, 255, 0.78);
}

.feature-aside-label {
  margin: 0;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.72);
}

.feature-aside-stats {
  display: grid;
  gap: 10px;
}

.feature-aside-stats span {
  display: inline-flex;
  align-items: center;
  min-height: 38px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  color: $text-white;
  font-size: 13px;
}

.surface-panel {
  display: grid;
  gap: 20px;
  padding: 26px;
  border-radius: 24px;
  border: 1px solid rgba(215, 208, 198, 0.88);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(251, 247, 240, 0.98) 100%);
  box-shadow: $shadow-sm;
}

.timeline-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(300px, 0.78fr);
  gap: 22px;
}

.timeline-list,
.timeline-rail {
  display: grid;
  gap: 16px;
  align-content: start;
}

.article-card {
  width: 100%;
  display: grid;
  gap: 14px;
  padding: 22px;
  text-align: left;
  border: 1px solid rgba(215, 208, 198, 0.88);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: $shadow-sm;
  cursor: pointer;
  transition: transform $transition-fast, box-shadow $transition-fast, border-color $transition-fast;
}

.article-card:hover {
  transform: translateY(-2px);
  box-shadow: $shadow-base;
  border-color: rgba(31, 58, 52, 0.18);
}

.article-card-top,
.article-card-footer,
.article-meta,
.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
}

.article-tags {
  justify-content: flex-start;
}

.article-time,
.article-meta span,
.article-cta {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: $text-secondary;
}

.article-card h3 {
  margin: 0;
  font-size: 24px;
  line-height: 1.25;
  color: $text-primary;
}

.article-summary {
  margin: 0;
  font-size: 15px;
  line-height: 1.82;
  color: $text-secondary;
}

.article-cta {
  font-weight: 700;
  color: $primary-color;
}

.category-list {
  display: grid;
  gap: 10px;
}

.category-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(243, 239, 232, 0.78);
  color: $text-primary;
}

.rail-link {
  width: 100%;
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  text-align: left;
  border: 1px solid rgba(215, 208, 198, 0.88);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.86);
  cursor: pointer;
  transition: transform $transition-fast, border-color $transition-fast, box-shadow $transition-fast;
}

.rail-link:hover {
  transform: translateY(-1px);
  border-color: rgba(31, 58, 52, 0.18);
  box-shadow: $shadow-base;
}

.rail-link-title {
  font-size: 15px;
  font-weight: 700;
  line-height: 1.45;
  color: $text-primary;
}

.rail-link-meta {
  font-size: 13px;
  color: $text-secondary;
}

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.selection-pill {
  display: inline-flex;
  align-items: center;
  min-height: 36px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid rgba(215, 208, 198, 0.88);
  background: rgba(255, 255, 255, 0.88);
  color: $text-secondary;
  font-size: 13px;
}

.table-container {
  border-radius: 20px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(215, 208, 198, 0.88);
}

.loading-container,
.empty-container {
  padding: 72px 0;
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
}

.import-section {
  text-align: center;
}

.upload-demo {
  display: grid;
  gap: 12px;
  justify-items: center;
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
  .filter-grid,
  .timeline-layout {
    grid-template-columns: 1fr;
  }

  .filter-actions-panel {
    justify-content: flex-start;
  }
}

@media (max-width: $breakpoint-md) {
  .news-shell {
    gap: 18px;
    padding-bottom: 40px;
  }

  .surface-panel {
    padding: 20px;
    border-radius: 20px;
  }

  .article-card {
    padding: 18px;
    border-radius: 18px;
  }

  .article-card h3 {
    font-size: 21px;
  }

  .article-card-top,
  .article-card-footer {
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .news-shell {
    gap: 14px;
    padding-bottom: 28px;
  }

  .hero-link {
    min-height: 40px;
    padding: 0 14px;
    font-size: 13px;
  }

  .filter-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .filter-field {
    gap: 8px;
  }

  .filter-actions-panel {
    justify-content: stretch;
    gap: 8px;
  }

  .filter-actions-panel :deep(.el-button) {
    flex: 1 1 100%;
    margin-left: 0;
  }

  .summary-copy {
    min-height: 32px;
    padding: 0 12px;
    font-size: 12px;
  }

  .feature-placeholder {
    font-size: 38px;
  }

  .feature-copy {
    gap: 10px;
  }

  .feature-note {
    font-size: 14px;
    line-height: 1.7;
  }

  .feature-aside-block h4 {
    font-size: 18px;
  }

  .feature-aside-block p,
  .contrast-copy {
    font-size: 13px;
    line-height: 1.68;
  }

  .surface-panel {
    gap: 16px;
    padding: 16px;
    border-radius: 18px;
  }

  .timeline-list,
  .timeline-rail {
    gap: 12px;
  }

  .article-card {
    gap: 12px;
    padding: 14px;
    border-radius: 16px;
  }

  .article-card h3 {
    font-size: 18px;
    line-height: 1.35;
  }

  .article-summary {
    font-size: 14px;
    line-height: 1.72;
  }

  .rail-link {
    padding: 12px 14px;
  }

  .rail-link-title {
    font-size: 14px;
  }

  .pagination {
    overflow-x: auto;
    justify-content: flex-start;
    padding-bottom: 4px;
  }

  .pagination :deep(.el-pagination) {
    width: 100%;
    min-width: 0;
    flex-wrap: wrap;
    row-gap: 8px;
  }

  .pagination :deep(.el-pagination__total),
  .pagination :deep(.el-pagination__sizes),
  .pagination :deep(.el-pagination__jump) {
    display: none;
  }
}
</style>
