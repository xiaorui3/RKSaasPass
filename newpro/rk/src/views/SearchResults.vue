<template>
  <div class="search-page design-search-reference-page">
    <div class="container">
      <div class="search-header">
        <p class="search-eyebrow">全站检索</p>
        <h1>全站搜索</h1>
        <p class="search-copy">
          覆盖新闻、公告、活动、比赛、作品、社团、人员、校友和历程，由搜索服务通过 Elasticsearch 统一检索。
        </p>
        <div class="search-bar">
          <el-input
            v-model="keyword"
            placeholder="搜索社团、人员姓名、活动或内容"
            size="large"
            clearable
            @keyup.enter="doSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
            <template #append>
              <el-button @click="doSearch" :loading="loading">搜索</el-button>
            </template>
          </el-input>
        </div>
      </div>

      <el-tabs v-if="hasSearched" v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane :label="`全部 (${allResults.length})`" name="all">
          <ResultList :items="allResults" empty-text="未找到相关结果" @open="goDetail" />
        </el-tab-pane>

        <el-tab-pane
          v-for="group in resultGroups"
          :key="group.name"
          :label="`${group.label} (${group.items.length})`"
          :name="group.name"
        >
          <ResultList :items="group.items" :empty-text="`暂无${group.label}结果`" @open="goDetail" />
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { computed, defineComponent, h, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { ElEmpty } from 'element-plus'
import { searchGlobal } from '@/api/search'

const router = useRouter()
const route = useRoute()

const ENTITY_GROUPS = [
  { name: 'news', label: '新闻', types: ['NEWS'], tone: 'news' },
  { name: 'notices', label: '公告', types: ['NOTICE'], tone: 'notice' },
  { name: 'activity', label: '活动', types: ['ACTIVITY'], tone: 'activity' },
  { name: 'competition', label: '比赛', types: ['COMPETITION'], tone: 'competition' },
  { name: 'works', label: '作品', types: ['WORKS', 'WORK'], tone: 'works' },
  { name: 'tenants', label: '社团', types: ['TENANT', 'CLUB'], tone: 'tenant' },
  { name: 'people', label: '人员', types: ['MEMBER', 'USER', 'PEOPLE'], tone: 'people' },
  { name: 'alumni', label: '校友', types: ['ALUMNI'], tone: 'alumni' },
  { name: 'history', label: '历程', types: ['HISTORY'], tone: 'history' }
]

const keyword = ref(route.query.q || '')
const loading = ref(false)
const hasSearched = ref(false)
const activeTab = ref('all')
const searchResults = ref([])

const ResultList = defineComponent({
  name: 'ResultList',
  props: {
    items: {
      type: Array,
      default: () => []
    },
    emptyText: {
      type: String,
      default: '暂无搜索结果'
    }
  },
  emits: ['open'],
  setup(props, { emit }) {
    return () => props.items.length === 0
      ? h('div', { class: 'empty-tip' }, [h(ElEmpty, { description: props.emptyText })])
      : h('div', { class: 'result-list' }, props.items.map((item) => h(
        'button',
        {
          key: item._key,
          type: 'button',
          class: ['result-card', { disabled: !item._route }],
          onClick: () => emit('open', item)
        },
        [
          h('div', { class: `result-type-badge tone-${item._tone}` }, item._type),
          h('div', { class: 'result-body' }, [
            h('h3', { class: 'result-title', innerHTML: item._title }),
            h('p', { class: 'result-desc', innerHTML: item._desc || '暂无摘要' }),
            h('span', { class: 'result-date' }, item._date || '时间待定')
          ])
        ]
      )))
  }
})

const allResults = computed(() => searchResults.value.map(buildResult))

const resultGroups = computed(() => ENTITY_GROUPS.map((group) => ({
  ...group,
  items: allResults.value.filter((item) => group.types.includes(normalizeEntityType(item.entityType)))
})))

function buildResult(item) {
  const entityType = normalizeEntityType(item.entityType)
  const group = ENTITY_GROUPS.find((entry) => entry.types.includes(entityType))
  const stableId = item.entityId || item.id || item.tenantId || item.title || stripText(JSON.stringify(item)).slice(0, 48)
  return {
    ...item,
    _key: `${entityType || 'GLOBAL'}-${stableId}`,
    _type: group?.label || entityType || '结果',
    _tone: group?.tone || 'history',
    _title: keepSafeHighlight(item.title || item.highlight || item.summary || item.entityType || '搜索结果'),
    _desc: keepSafeHighlight(item.summary || item.highlight || ''),
    _date: formatDate(item.updatedAt || item.updateTime || item.createTime),
    _route: item.route || buildFallbackRoute(entityType, item)
  }
}

function normalizeRecords(data) {
  if (Array.isArray(data)) return data
  if (!data || typeof data !== 'object') return []
  if (Array.isArray(data.records)) return data.records
  if (Array.isArray(data.list)) return data.list
  if (Array.isArray(data.rows)) return data.rows
  if (Array.isArray(data.data)) return data.data
  return []
}

function normalizeEntityType(value) {
  return String(value || '').trim().toUpperCase()
}

function stripText(value) {
  return String(value || '').replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim()
}

function keepSafeHighlight(value) {
  const escaped = String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
  return escaped
    .replace(/&lt;em&gt;/g, '<em>')
    .replace(/&lt;\/em&gt;/g, '</em>')
}

function formatDate(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function buildFallbackRoute(entityType, item) {
  const entityId = item.entityId || item.id
  if (entityType === 'NEWS' && entityId) return `/news/${entityId}`
  if (entityType === 'ACTIVITY' && entityId) return `/activities/${entityId}`
  if (entityType === 'COMPETITION' && entityId) return `/competition/${entityId}`
  if ((entityType === 'WORKS' || entityType === 'WORK') && entityId) return `/works/${entityId}`
  if (entityType === 'TENANT' || entityType === 'CLUB') return item.tenantId ? `/join?tenantId=${encodeURIComponent(item.tenantId)}` : '/join'
  if (entityType === 'MEMBER' || entityType === 'USER' || entityType === 'PEOPLE' || entityType === 'ALUMNI') return '/alumni'
  if (entityType === 'HISTORY') return '/history'
  if (entityType === 'NOTICE') return '/notices'
  return ''
}

async function doSearch() {
  const q = keyword.value.trim()
  if (!q) return
  loading.value = true
  hasSearched.value = true

  try {
    const res = await searchGlobal({
      keyword: q,
      pageNo: 1,
      pageSize: 50
    })
    searchResults.value = normalizeRecords(res.data)
  } catch (error) {
    console.error('全站搜索失败:', error)
    searchResults.value = []
  } finally {
    loading.value = false
    router.replace({ query: { q } })
  }
}

function goDetail(item) {
  if (item._route) {
    router.push(item._route)
  }
}

function handleTabChange() {}

if (keyword.value) {
  doSearch()
}
</script>

<style scoped>
.search-page {
  padding: 40px 0 60px;
  min-height: 60vh;
}

.search-eyebrow {
  margin: 0 0 8px;
  font-size: 13px;
  font-weight: 800;
  color: rgba(255, 255, 255, 0.78);
}

.search-header h1 {
  margin: 0 0 12px;
  font-size: 42px;
  font-weight: 900;
}

.search-copy {
  max-width: 760px;
  margin: 0 0 24px;
  color: rgba(255, 255, 255, 0.84);
  line-height: 1.8;
}

.result-list {
  display: grid;
  gap: 14px;
}

.result-card {
  width: 100%;
  display: flex;
  gap: 16px;
  padding: 18px;
  border: 1px solid #e7edf6;
  border-radius: 8px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
}

.result-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 30px rgba(31, 88, 160, 0.1);
}

.result-card.disabled {
  cursor: default;
}

.result-type-badge {
  flex-shrink: 0;
  min-width: 54px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  border-radius: 8px;
  background: #256df4;
  color: #fff;
  font-size: 12px;
  font-weight: 800;
}

.tone-notice { background: #ff5b6e; }
.tone-activity { background: #19b981; }
.tone-competition { background: #256df4; }
.tone-works { background: #7c5cff; }
.tone-tenant { background: #0ea5e9; }
.tone-people { background: #f59e0b; }
.tone-alumni { background: #10b981; }
.tone-history { background: #64748b; }

.result-body {
  min-width: 0;
}

.result-title {
  margin: 0 0 8px;
  color: #111827;
  font-size: 17px;
  font-weight: 900;
}

.result-desc {
  margin: 0 0 8px;
  overflow: hidden;
  color: #64748b;
  font-size: 14px;
  line-height: 1.6;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-title :deep(em),
.result-desc :deep(em) {
  color: #256df4;
  font-style: normal;
  font-weight: 900;
}

.result-date {
  color: #9aa6bb;
  font-size: 12px;
}

.empty-tip {
  padding: 48px 0;
}
</style>
