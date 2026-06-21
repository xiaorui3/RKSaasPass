<template>
  <div class="rainbond-console">
    <section class="page-header">
      <div>
        <h2>Rainbond 控制台</h2>
        <p>按企业、团队、集群、应用、网关和用户组织 OpenAPI，支持可视化查看、快捷调用和官方 Web 端嵌入。</p>
      </div>
      <div class="header-actions">
        <el-button @click="openOfficialWeb">官方 Web</el-button>
        <el-button :loading="loading" @click="loadAll">刷新</el-button>
        <el-button type="primary" :loading="saving" @click="saveConfig">保存配置</el-button>
      </div>
    </section>

    <section class="config-panel">
      <el-form :model="config" label-position="top">
        <div class="config-grid">
          <el-form-item label="启用">
            <el-switch v-model="config.enabled" />
          </el-form-item>
          <el-form-item label="Base URL">
            <el-input v-model.trim="config.baseUrl" placeholder="https://rainbond.example.com" />
          </el-form-item>
          <el-form-item label="Token">
            <el-input v-model.trim="config.token" show-password placeholder="留空表示沿用已保存 Token" />
          </el-form-item>
          <el-form-item label="企业 ID">
            <el-input v-model.trim="config.enterpriseId" />
          </el-form-item>
          <el-form-item label="团队 ID">
            <el-input v-model.trim="config.teamId" />
          </el-form-item>
          <el-form-item label="集群/Region">
            <el-input v-model.trim="config.regionName" />
          </el-form-item>
        </div>
      </el-form>
    </section>

    <section class="summary-grid">
      <div v-for="item in discoveryCards" :key="item.key" class="summary-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.count }}</strong>
        <small>{{ item.message }}</small>
      </div>
    </section>

    <section class="visual-console">
      <div class="visual-header">
        <div>
          <h3>资源总览</h3>
          <p>发现接口返回真实数据时展示资源名称；没有返回时展示当前配置和 API 入口。</p>
        </div>
        <el-input v-model="keyword" class="visual-search" clearable placeholder="搜索资源或接口" />
      </div>
      <div class="resource-lanes">
        <div v-for="lane in resourceLanes" :key="lane.category" class="resource-lane">
          <div class="lane-head">
            <span>{{ lane.label }}</span>
            <el-tag size="small">{{ lane.items.length }}</el-tag>
          </div>
          <div class="resource-list">
            <button
              v-for="item in lane.items"
              :key="`${lane.category}-${item.key}`"
              class="resource-card"
              @click="selectResource(lane.category, item)"
            >
              <strong>{{ item.name }}</strong>
              <small>{{ item.description }}</small>
            </button>
          </div>
          <div class="lane-actions">
            <el-button
              v-for="action in lane.actions"
              :key="`${lane.category}-${action.name}`"
              size="small"
              @click="callResourceAction(action)"
            >
              {{ action.label }}
            </el-button>
          </div>
        </div>
      </div>
    </section>

    <section class="workbench">
      <aside class="category-panel">
        <el-tabs v-model="activeCategory" stretch>
          <el-tab-pane v-for="category in categories" :key="category" :label="categoryLabel(category)" :name="category" />
        </el-tabs>
        <el-scrollbar height="520px">
          <button
            v-for="endpoint in filteredEndpoints"
            :key="`${endpoint.method}-${endpoint.path}-${endpoint.name}`"
            class="endpoint-item"
            @click="selectEndpoint(endpoint)"
          >
            <el-tag size="small" :type="methodTag(endpoint.method)">{{ endpoint.method }}</el-tag>
            <span>{{ endpoint.name }}</span>
            <small>{{ fillPath(endpoint.path) }}</small>
          </button>
        </el-scrollbar>
      </aside>

      <main class="api-panel">
        <div class="api-title">
          <div>
            <h3>{{ request.name || '选择一个 Rainbond API' }}</h3>
            <p>{{ request.path || '从左侧资源或接口列表选择，也可以手动输入 OpenAPI 路径。' }}</p>
          </div>
          <el-button type="primary" :loading="calling" @click="callApi">调用接口</el-button>
        </div>
        <div class="request-grid">
          <el-select v-model="request.method">
            <el-option label="GET" value="GET" />
            <el-option label="POST" value="POST" />
            <el-option label="PUT" value="PUT" />
            <el-option label="DELETE" value="DELETE" />
            <el-option label="PATCH" value="PATCH" />
          </el-select>
          <el-input v-model.trim="request.path" placeholder="/openapi/v1/teams" />
        </div>
        <el-input v-model="request.queryText" type="textarea" :rows="3" placeholder='Query JSON，例如 {"page":1}' />
        <el-input v-model="request.bodyText" type="textarea" :rows="8" placeholder="Body JSON" />
        <div class="response-panel">
          <div class="response-meta">
            <el-tag :type="response.success ? 'success' : 'danger'">HTTP {{ response.statusCode || '-' }}</el-tag>
            <span>{{ response.message || '尚未调用' }}</span>
          </div>
          <pre>{{ response.body || discoveryText || '暂无响应' }}</pre>
        </div>
      </main>
    </section>

    <el-dialog v-model="officialWebVisible" title="Rainbond 官方 Web" width="88vw" top="5vh">
      <iframe v-if="officialWebVisible" class="official-frame" :src="config.baseUrl" />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  callRainbondApi,
  getRainbondCatalog,
  getRainbondConfig,
  getRainbondDiscovery,
  saveRainbondConfig
} from '@/api/admin-ops'

const loading = ref(false)
const saving = ref(false)
const calling = ref(false)
const officialWebVisible = ref(false)
const keyword = ref('')
const activeCategory = ref('enterprise')
const catalog = ref([])
const discovery = ref({})
const discoveryText = ref('')

const config = reactive({
  enabled: false,
  baseUrl: '',
  token: '',
  enterpriseId: '',
  teamId: '',
  regionName: '',
  timeoutSeconds: 15
})
const request = reactive({
  name: '',
  method: 'GET',
  path: '',
  queryText: '{}',
  bodyText: ''
})
const response = reactive({
  success: false,
  statusCode: 0,
  message: '',
  body: ''
})

const categories = computed(() => [...new Set(catalog.value.map((item) => item.category))])
const filteredEndpoints = computed(() => {
  const search = keyword.value.trim().toLowerCase()
  return catalog.value
    .filter((item) => item.category === activeCategory.value)
    .filter((item) => !search || `${item.name} ${item.method} ${item.path}`.toLowerCase().includes(search))
})
const discoveryCards = computed(() => [
  { key: 'enterprises', label: '企业', count: countDiscovery('enterprises'), message: discoveryMessage('enterprises') },
  { key: 'teams', label: '团队', count: countDiscovery('teams'), message: discoveryMessage('teams') },
  { key: 'regions', label: '集群', count: countDiscovery('regions'), message: discoveryMessage('regions') },
  { key: 'apps', label: '应用', count: countDiscovery('apps'), message: discoveryMessage('apps') }
])
const resourceLanes = computed(() => [
  buildLane('enterprise', '企业', 'enterprises', 'getEnterpriseList'),
  buildLane('team', '团队', 'teams', 'getTeamList'),
  buildLane('region', '集群', 'regions', 'getRegionList'),
  buildLane('application', '应用', 'apps', 'getTeamApps'),
  buildLane('gateway', '网关', 'gateways', 'getGatewayList'),
  buildLane('user', '用户', 'users', 'getUserLIst')
])

function categoryLabel(category) {
  return {
    enterprise: '企业',
    team: '团队',
    region: '集群',
    application: '应用',
    gateway: '网关',
    user: '用户'
  }[category] || category
}

function methodTag(method) {
  return { GET: 'success', POST: 'primary', PUT: 'warning', DELETE: 'danger', PATCH: 'info' }[method] || 'info'
}

function countDiscovery(key) {
  const value = discovery.value?.[key]
  const items = extractItems(value)
  if (items.length) return items.length
  return value?.success === false ? 0 : '-'
}

function discoveryMessage(key) {
  const value = discovery.value?.[key]
  if (!value) return '未发现'
  return value.message || (value.success === false ? '调用失败' : '已连接')
}

function buildLane(category, label, discoveryKey, fallbackActionName) {
  const endpoints = catalog.value.filter((item) => item.category === category)
  const action = endpoints.find((item) => item.name === fallbackActionName) || endpoints[0]
  return {
    category,
    label,
    items: normalizeResourceItems(discoveryKey, category),
    actions: endpoints.slice(0, 4).map((item) => ({
      ...item,
      label: item.name === action?.name ? `刷新${label}` : item.name
    }))
  }
}

function normalizeResourceItems(discoveryKey, category) {
  const items = extractItems(discovery.value?.[discoveryKey])
  if (items.length) {
    return items.slice(0, 8).map((item, index) => ({
      key: item.id || item.team_id || item.region_name || item.name || index,
      name: item.name || item.team_name || item.region_name || item.app_name || item.username || item.id || `${category}-${index + 1}`,
      description: item.desc || item.description || item.alias || item.status || item.id || 'Rainbond resource'
    }))
  }
  const fallbackName = {
    enterprise: config.enterpriseId || '企业配置',
    team: config.teamId || '团队配置',
    region: config.regionName || '集群配置',
    application: '应用列表',
    gateway: '网关规则',
    user: '用户列表'
  }[category]
  return [{ key: `fallback-${category}`, name: fallbackName, description: '等待发现接口返回真实资源' }]
}

function extractItems(value) {
  if (!value) return []
  if (Array.isArray(value)) return value
  if (Array.isArray(value.data)) return value.data
  if (Array.isArray(value.list)) return value.list
  if (Array.isArray(value.body)) return value.body
  if (typeof value.body === 'string') {
    try {
      return extractItems(JSON.parse(value.body))
    } catch {
      return []
    }
  }
  return []
}

async function loadAll() {
  loading.value = true
  try {
    await Promise.all([loadConfig(), loadCatalog(), loadDiscovery()])
  } finally {
    loading.value = false
  }
}

async function loadConfig() {
  const res = await getRainbondConfig()
  Object.assign(config, res.data || {})
  config.token = ''
}

async function loadCatalog() {
  const res = await getRainbondCatalog()
  const categoriesData = res.data?.categories || {}
  catalog.value = Object.entries(categoriesData).flatMap(([category, items]) =>
    (items || []).map((item) => ({ category, ...item }))
  )
  if (!categories.value.includes(activeCategory.value) && categories.value.length) {
    activeCategory.value = categories.value[0]
  }
}

async function loadDiscovery() {
  try {
    const res = await getRainbondDiscovery()
    discovery.value = res.data || {}
    discoveryText.value = JSON.stringify(discovery.value, null, 2)
  } catch (error) {
    discovery.value = {}
    discoveryText.value = error.message || String(error)
  }
}

async function saveConfig() {
  saving.value = true
  try {
    await saveRainbondConfig(config)
    ElMessage.success('Rainbond 配置已保存')
    await loadAll()
  } finally {
    saving.value = false
  }
}

function selectResource(category, item) {
  activeCategory.value = category
  keyword.value = item.name || ''
}

function selectEndpoint(endpoint) {
  request.name = endpoint.name
  request.method = endpoint.method || 'GET'
  request.path = fillPath(endpoint.path || '')
  request.queryText = '{}'
  request.bodyText = ['GET', 'DELETE'].includes(request.method) ? '' : '{}'
}

function callResourceAction(action) {
  selectEndpoint(action)
  callApi()
}

async function callApi() {
  if (!request.path.startsWith('/openapi/')) {
    return ElMessage.warning('Rainbond 路径必须以 /openapi/ 开头')
  }
  calling.value = true
  try {
    const res = await callRainbondApi({
      method: request.method,
      path: request.path,
      query: parseJson(request.queryText, {}),
      body: parseJson(request.bodyText, null)
    })
    Object.assign(response, res.data || {})
    response.body = formatBody(res.data?.body || res.data?.responseBody)
  } finally {
    calling.value = false
  }
}

function fillPath(path) {
  return path
    .replaceAll('{eid}', config.enterpriseId || '{eid}')
    .replaceAll('{team_id}', config.teamId || '{team_id}')
    .replaceAll('{team_name}', config.teamId || '{team_name}')
    .replaceAll('{region_name}', config.regionName || '{region_name}')
}

function parseJson(text, fallback) {
  if (!text || !text.trim()) return fallback
  return JSON.parse(text)
}

function formatBody(body) {
  if (!body) return ''
  try {
    return JSON.stringify(JSON.parse(body), null, 2)
  } catch {
    return String(body)
  }
}

function openOfficialWeb() {
  if (!config.baseUrl) {
    ElMessage.warning('请先配置 Rainbond Base URL')
    return
  }
  officialWebVisible.value = true
}

onMounted(loadAll)
</script>

<style scoped>
.rainbond-console { display: grid; gap: 16px; padding: 20px; }
.page-header,
.config-panel,
.summary-card,
.visual-console,
.workbench { border: 1px solid #e4e7ed; border-radius: 8px; background: #fff; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; padding: 18px 20px; }
.page-header h2 { margin: 0 0 6px; }
.page-header p { margin: 0; color: #667085; }
.header-actions { display: flex; gap: 10px; flex-wrap: wrap; justify-content: flex-end; }
.config-panel { padding: 16px 20px 0; }
.config-grid { display: grid; grid-template-columns: 120px repeat(5, minmax(0, 1fr)); gap: 12px; }
.summary-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
.summary-card { padding: 16px; }
.summary-card span { display: block; color: #667085; }
.summary-card strong { display: block; margin: 8px 0; font-size: 28px; color: #1f7a55; }
.summary-card small { color: #909399; }
.visual-console { padding: 16px; }
.visual-header { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; margin-bottom: 12px; }
.visual-header h3 { margin: 0 0 6px; }
.visual-header p { margin: 0; color: #667085; }
.visual-search { width: 320px; }
.resource-lanes { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 12px; }
.resource-lane { border: 1px solid #edf0f3; border-radius: 8px; min-height: 240px; overflow: hidden; background: #fafafa; }
.lane-head { display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-bottom: 1px solid #edf0f3; background: #fff; font-weight: 600; }
.resource-list { display: grid; gap: 8px; padding: 10px; }
.resource-card { display: grid; gap: 4px; width: 100%; border: 1px solid #e4e7ed; border-radius: 8px; padding: 10px; background: #fff; text-align: left; cursor: pointer; }
.resource-card:hover { border-color: #409eff; }
.resource-card strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.resource-card small { color: #667085; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.lane-actions { display: flex; flex-wrap: wrap; gap: 8px; padding: 10px; border-top: 1px solid #edf0f3; }
.workbench { display: grid; grid-template-columns: 360px minmax(0, 1fr); min-height: 620px; }
.category-panel { border-right: 1px solid #edf0f3; padding: 12px; }
.endpoint-item { display: grid; grid-template-columns: auto minmax(0, 1fr); gap: 6px 8px; width: 100%; padding: 10px; border: 0; border-bottom: 1px solid #f0f2f5; background: transparent; text-align: left; cursor: pointer; }
.endpoint-item:hover { background: #f6f8fb; }
.endpoint-item small { grid-column: 1 / -1; color: #909399; word-break: break-all; }
.api-panel { display: grid; gap: 12px; align-content: start; padding: 16px; }
.api-title { display: flex; justify-content: space-between; gap: 12px; }
.api-title h3 { margin: 0 0 6px; }
.api-title p { margin: 0; color: #667085; word-break: break-all; }
.request-grid { display: grid; grid-template-columns: 140px minmax(0, 1fr); gap: 10px; }
.response-panel { border: 1px solid #e4e7ed; border-radius: 8px; overflow: hidden; }
.response-meta { display: flex; gap: 10px; align-items: center; padding: 10px 12px; border-bottom: 1px solid #edf0f3; }
.response-panel pre { min-height: 220px; max-height: 420px; overflow: auto; margin: 0; padding: 12px; background: #111827; color: #d1fae5; white-space: pre-wrap; }
.official-frame { width: 100%; height: 74vh; border: 0; }
@media (max-width: 1300px) {
  .resource-lanes { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (max-width: 1100px) {
  .config-grid,
  .summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .workbench { grid-template-columns: minmax(0, 1fr); }
  .category-panel { border-right: 0; border-bottom: 1px solid #edf0f3; }
}
@media (max-width: 760px) {
  .page-header,
  .visual-header { flex-direction: column; }
  .visual-search { width: 100%; }
  .resource-lanes,
  .summary-grid,
  .config-grid { grid-template-columns: minmax(0, 1fr); }
}
</style>
