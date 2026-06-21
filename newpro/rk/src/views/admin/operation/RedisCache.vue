<template>
  <div class="redis-cache-page">
    <div class="page-head">
      <div>
        <p class="eyebrow">Frontend Cache</p>
        <h1>前台缓存预热 / Redis 缓存管理</h1>
        <p>查看前台公共接口缓存状态，执行预热任务，并清理可由访问或定时任务重建的 Redis 缓存。</p>
      </div>
      <div class="head-actions">
        <span>最后刷新：{{ overview.lastUpdatedAt || overview.checkedAt || refreshedAt || '未刷新' }}</span>
        <el-button type="primary" :loading="loading" @click="loadOverview">刷新</el-button>
      </div>
    </div>

    <div class="status-grid">
      <div v-for="item in summaryCards" :key="item.label" class="status-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.hint }}</small>
      </div>
    </div>

    <div class="content-grid">
      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="card-title">
            <span>缓存保护策略</span>
            <el-tag :type="strategyEnabledCount >= 2 ? 'success' : 'warning'">
              {{ strategyEnabledCount }}/3 已启用
            </el-tag>
          </div>
        </template>
        <div class="strategy-list">
          <div v-for="item in strategyItems" :key="item.key" class="strategy-item">
            <div>
              <strong>{{ item.label }}</strong>
              <p>{{ item.description }}</p>
            </div>
            <el-tag :type="item.enabled ? 'success' : 'info'">
              {{ item.enabled ? '已启用' : '未启用' }}
            </el-tag>
          </div>
        </div>
      </el-card>

      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="card-title">
            <span>预热路径</span>
            <el-button size="small" type="primary" :loading="warming" @click="handleWarmup">
              执行预热
            </el-button>
          </div>
        </template>
        <el-table :data="warmupPaths" size="small" height="252">
          <el-table-column prop="path" label="路径" min-width="210" show-overflow-tooltip />
          <el-table-column prop="method" label="方法" width="82" />
          <el-table-column label="状态" width="108">
            <template #default="{ row }">
              <el-tag :type="warmupStatusType(row.status)" size="small">{{ row.statusText || row.status || '待预热' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="costMs" label="耗时" width="96">
            <template #default="{ row }">{{ formatMs(row.costMs || row.durationMillis) }}</template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <el-card shadow="never" class="panel-card">
      <template #header>
        <div class="card-title">
          <span>Redis 缓存分组</span>
          <el-button type="danger" plain :loading="clearing" @click="handleClear">清理可重建缓存</el-button>
        </div>
      </template>
      <el-table :data="cacheGroups" v-loading="loading" row-key="name">
        <el-table-column prop="name" label="分组" min-width="150" />
        <el-table-column prop="pattern" label="Key 规则" min-width="230" show-overflow-tooltip />
        <el-table-column prop="keyCount" label="Key 数" width="100" />
        <el-table-column prop="hitCount" label="命中" width="100" />
        <el-table-column prop="nullHitCount" label="空值命中" width="110" />
        <el-table-column prop="ttlText" label="失效策略" min-width="160" />
      </el-table>
    </el-card>

    <div class="content-grid">
      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="card-title">
            <span>最近预热结果</span>
            <small>{{ warmupResult.summary || '暂无预热记录' }}</small>
          </div>
        </template>
        <div class="result-grid">
          <div v-for="item in warmupResultItems" :key="item.label" class="result-item">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
      </el-card>

      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="card-title">
            <span>诊断信息</span>
            <small>{{ diagnostics.length }} 条</small>
          </div>
        </template>
        <div class="diagnostic-list">
          <div v-for="item in diagnostics" :key="item" class="diagnostic-item">{{ item }}</div>
          <el-empty v-if="!diagnostics.length" description="暂无诊断信息" />
        </div>
      </el-card>
    </div>

    <el-card shadow="never" class="panel-card">
      <template #header>
        <div class="card-title">
          <span>缓存 key 样本</span>
          <small>{{ sampleKeys.length }} 条</small>
        </div>
      </template>
      <el-table :data="sampleKeys" size="small" height="260">
        <el-table-column prop="key" label="Key" min-width="360" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="110" />
        <el-table-column prop="ttlSeconds" label="TTL" width="100" />
        <el-table-column prop="bytes" label="大小" width="110">
          <template #default="{ row }">{{ formatBytes(row.bytes) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { clearFrontendCache, getFrontendCacheOverview, warmupFrontendCache } from '@/api/admin-ops'
import { getFrontendCacheWarmupSummary, triggerFrontendCacheWarmup } from '@/api/data'

const loading = ref(false)
const warming = ref(false)
const clearing = ref(false)
const refreshedAt = ref('')
const overview = reactive({
  keyCount: 0,
  totalKeys: 0,
  totalSizeText: '0 B',
  totalBytes: 0,
  nullHitCount: 0,
  nullMarkerCount: 0,
  penetrationBlockedCount: 0,
  randomExpireCount: 0,
  warmupKeyCount: 0,
  ttlJitterSeconds: 0,
  nullTtlSeconds: 0,
  antiPenetrationEnabled: false,
  randomTtlEnabled: false,
  randomExpireEnabled: false,
  nullValueCacheEnabled: false,
  lastUpdatedAt: '',
  checkedAt: '',
  groups: [],
  warmupPaths: [],
  sampleKeys: [],
  diagnostics: [],
  lastWarmupResult: {},
  dataWarmupSummary: {}
})

function unwrapResponse(res) {
  if (res?.code && res.code !== 200) {
    throw new Error(res.msg || '接口请求失败')
  }
  return res?.data || res || {}
}

function numberValue(value) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

function formatMs(value) {
  const ms = numberValue(value)
  return ms ? `${ms} ms` : '-'
}

function formatBytes(value) {
  const bytes = numberValue(value)
  if (bytes <= 0) return '0 B'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

function normalizeGroup(item = {}) {
  return {
    name: item.name || item.groupName || item.cacheName || '默认分组',
    pattern: item.pattern || item.keyPattern || item.redisKey || '-',
    keyCount: numberValue(item.keyCount || item.keys || item.totalKeys),
    hitCount: numberValue(item.hitCount || item.hits),
    nullHitCount: numberValue(item.nullHitCount || item.emptyHitCount),
    ttlText: item.ttlText || item.expirePolicy || item.ttl || '随机失效'
  }
}

function normalizeWarmupPath(item = {}) {
  return {
    path: item.path || item.url || item.route || '-',
    method: item.method || 'GET',
    status: item.status || '',
    statusText: item.statusText || item.message || '',
    costMs: item.costMs || item.durationMs || item.durationMillis || item.elapsedMs
  }
}

const cacheGroups = computed(() => {
  const groups = Array.isArray(overview.groups) ? overview.groups.map(normalizeGroup) : []
  if (groups.length) return groups
  return [
    normalizeGroup({ name: '前台响应缓存', pattern: 'rk:frontend-cache:tenant:*', keyCount: overview.keyCount || overview.totalKeys }),
    normalizeGroup({ name: '预热记录', pattern: 'rk:frontend-cache:warmup:*', keyCount: overview.warmupKeyCount })
  ]
})

const warmupPaths = computed(() => {
  const rows = Array.isArray(overview.warmupPaths) ? overview.warmupPaths : overview.paths
  return Array.isArray(rows) ? rows.map((item) => normalizeWarmupPath(typeof item === 'string' ? { path: item } : item)) : []
})

const sampleKeys = computed(() => Array.isArray(overview.sampleKeys) ? overview.sampleKeys : [])
const diagnostics = computed(() => Array.isArray(overview.diagnostics) ? overview.diagnostics : [])
const warmupResult = computed(() => overview.lastWarmupResult || {})
const strategyItems = computed(() => [
  {
    key: 'null-cache',
    label: '空值缓存',
    enabled: !!overview.nullValueCacheEnabled || numberValue(overview.nullHitCount || overview.nullMarkerCount) > 0,
    description: '缓存空结果，降低重复查询不存在数据的压力。'
  },
  {
    key: 'anti-penetration',
    label: '防穿透',
    enabled: !!overview.antiPenetrationEnabled || numberValue(overview.penetrationBlockedCount) > 0,
    description: '对不存在的数据写入短 TTL 空值缓存，避免异常 Key 穿透到数据库。'
  },
  {
    key: 'random-expire',
    label: '随机失效',
    enabled: !!overview.randomTtlEnabled || !!overview.randomExpireEnabled || numberValue(overview.randomExpireCount) > 0,
    description: '为热点缓存增加随机 TTL，避免同一时刻集中失效造成雪崩。'
  },
  {
    key: 'business-snapshot',
    label: '业务快照预热',
    enabled: !!overview.dataWarmupSummary?.redisAvailable && numberValue(overview.dataWarmupSummary?.keyCount) > 0,
    description: 'rk-data 按租户写入前台可见业务快照，XXL-Job 每小时检查缺失 key。'
  }
])
const strategyEnabledCount = computed(() => strategyItems.value.filter((item) => item.enabled).length)
const summaryCards = computed(() => [
  { label: 'Key 数', value: numberValue(overview.keyCount || overview.totalKeys), hint: overview.totalSizeText || '前台可重建缓存总量' },
  { label: '空值命中', value: numberValue(overview.nullHitCount || overview.nullMarkerCount), hint: '不存在数据的缓存命中' },
  { label: '预热记录', value: numberValue(overview.warmupKeyCount), hint: 'XXL-Job 每小时检查预热' },
  { label: '业务快照', value: numberValue(overview.dataWarmupSummary?.keyCount), hint: `${numberValue(overview.dataWarmupSummary?.tenantCount)} 个租户` },
  { label: '随机失效', value: numberValue(overview.randomExpireCount), hint: `TTL 抖动 ${numberValue(overview.ttlJitterSeconds)} 秒` },
  { label: '防穿透', value: overview.antiPenetrationEnabled ? '已启用' : '待检查', hint: `空值 TTL ${numberValue(overview.nullTtlSeconds)} 秒` }
])
const warmupResultItems = computed(() => [
  { label: '成功', value: numberValue(warmupResult.value.successCount) },
  { label: '失败', value: numberValue(warmupResult.value.failedCount) },
  { label: '总耗时', value: formatMs(warmupResult.value.costMs || warmupResult.value.durationMs) },
  { label: '执行时间', value: warmupResult.value.finishedAt || warmupResult.value.warmedAt || '-' }
])

function warmupStatusType(status) {
  const value = String(status || '').toLowerCase()
  if (['success', 'ok', 'done'].includes(value)) return 'success'
  if (['failed', 'error'].includes(value)) return 'danger'
  if (['running', 'warming'].includes(value)) return 'warning'
  return 'info'
}

async function loadOverview() {
  loading.value = true
  try {
    const [cacheOverview, dataWarmupSummary] = await Promise.all([
      getFrontendCacheOverview(),
      getFrontendCacheWarmupSummary().catch((error) => ({ code: 500, msg: error.message, data: null }))
    ])
    Object.assign(overview, unwrapResponse(cacheOverview))
    const dataSummary = dataWarmupSummary?.code === 200 ? dataWarmupSummary.data : null
    overview.dataWarmupSummary = dataSummary || {
      redisAvailable: false,
      degraded: true,
      reason: dataWarmupSummary?.msg || 'rk-data 业务快照预热状态不可用',
      keyCount: 0
    }
    refreshedAt.value = new Date().toLocaleString()
  } catch (error) {
    ElMessage.error(error.message || '加载缓存概览失败')
  } finally {
    loading.value = false
  }
}

async function handleWarmup() {
  warming.value = true
  try {
    const [opsResult, dataResult] = await Promise.all([
      warmupFrontendCache(),
      triggerFrontendCacheWarmup()
    ])
    const data = unwrapResponse(opsResult)
    overview.lastWarmupResult = data
    overview.dataWarmupSummary = unwrapResponse(dataResult)
    if (Array.isArray(data.warmupPaths)) {
      overview.warmupPaths = data.warmupPaths
    }
    ElMessage.success('前台缓存预热已完成')
    await loadOverview()
  } catch (error) {
    ElMessage.error(error.message || '缓存预热失败')
  } finally {
    warming.value = false
  }
}

async function handleClear() {
  await ElMessageBox.confirm('确认清理后将由前台访问或预热任务重新生成缓存。', '清理可重建缓存', { type: 'warning' })
  clearing.value = true
  try {
    const data = unwrapResponse(await clearFrontendCache())
    ElMessage.success(`缓存清理完成，删除 ${numberValue(data.deletedCount)} 个 key`)
    await loadOverview()
  } catch (error) {
    ElMessage.error(error.message || '缓存清理失败')
  } finally {
    clearing.value = false
  }
}

onMounted(loadOverview)
</script>

<style scoped>
.redis-cache-page {
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
  color: #e53935;
  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
}

.page-head h1 {
  margin: 0;
  font-size: 22px;
}

.page-head p {
  margin: 8px 0 0;
  color: #667085;
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #667085;
  font-size: 13px;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.status-card,
.panel-card {
  border-radius: 8px;
}

.status-card {
  display: grid;
  gap: 8px;
  min-height: 108px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.status-card span,
.status-card small,
.card-title small {
  color: #667085;
  font-size: 13px;
}

.status-card strong {
  color: #101828;
  font-size: 24px;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(320px, 0.9fr) minmax(0, 1.1fr);
  gap: 16px;
}

.card-title {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  font-weight: 700;
}

.strategy-list,
.diagnostic-list {
  display: grid;
  gap: 12px;
}

.strategy-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border: 1px solid #eef0f3;
  border-radius: 8px;
  background: #fafafa;
}

.strategy-item p {
  margin: 6px 0 0;
  color: #667085;
  font-size: 13px;
  line-height: 1.5;
}

.diagnostic-item {
  padding: 10px 12px;
  border: 1px solid #eef0f3;
  border-radius: 8px;
  background: #fafafa;
  color: #475467;
  font-size: 13px;
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.result-item {
  display: grid;
  gap: 8px;
  padding: 14px;
  border-radius: 8px;
  background: #f7f9fc;
}

.result-item span {
  color: #667085;
  font-size: 13px;
}

.result-item strong {
  color: #303133;
  font-size: 20px;
}

@media (max-width: 1180px) {
  .status-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .content-grid,
  .result-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .page-head,
  .head-actions,
  .strategy-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .status-grid {
    grid-template-columns: 1fr;
  }
}
</style>
