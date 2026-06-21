<template>
  <div class="admin-page api-workbench-page">
    <el-card class="workbench-card">
      <template #header>
        <div class="card-header">
          <div>
            <h1>API 调试台</h1>
            <p>仅 tenant 1 开放。基于网关 Swagger 资源浏览接口，并使用当前登录 token 直接调试。</p>
          </div>
          <div class="actions">
            <el-button @click="refreshResources" :loading="resourcesLoading">刷新接口源</el-button>
            <el-button type="primary" plain @click="copyToken">复制当前 Token</el-button>
          </div>
        </div>
      </template>

      <el-alert
        v-if="!canUseWorkbench"
        type="warning"
        :closable="false"
        show-icon
        title="当前租户没有 API 调试台权限，仅 tenant 1 可使用。"
      />

      <template v-else>
        <div class="token-bar">
          <span class="token-label">当前 Token</span>
          <code class="token-preview">{{ tokenPreview }}</code>
        </div>

        <section class="rainbond-panel">
          <div class="panel-header">
            <div>
              <h2>Rainbond OpenAPI</h2>
              <p>Enterprise, team, region, application, gateway, and user APIs are called through the backend proxy.</p>
            </div>
            <div class="actions">
              <el-button :loading="rainbondLoading" @click="fetchRainbondConfig">Load Config</el-button>
              <el-button type="primary" :loading="rainbondSaving" @click="handleSaveRainbondConfig">Save Config</el-button>
              <el-button :loading="rainbondDiscovering" @click="handleRainbondDiscovery">Discover</el-button>
            </div>
          </div>
          <el-form label-position="top" class="rainbond-form">
            <el-form-item label="Enabled">
              <el-switch v-model="rainbondForm.enabled" />
            </el-form-item>
            <el-form-item label="Console Base URL">
              <el-input v-model.trim="rainbondForm.baseUrl" placeholder="https://rainbond.example.com" />
            </el-form-item>
            <el-form-item label="Access Token">
              <el-input v-model.trim="rainbondForm.token" placeholder="Leave empty to keep saved token" show-password />
            </el-form-item>
            <el-form-item label="Enterprise ID">
              <el-input v-model.trim="rainbondForm.enterpriseId" />
            </el-form-item>
            <el-form-item label="Team ID">
              <el-input v-model.trim="rainbondForm.teamId" />
            </el-form-item>
            <el-form-item label="Region Name">
              <el-input v-model.trim="rainbondForm.regionName" />
            </el-form-item>
          </el-form>
          <div class="rainbond-workbench">
            <section>
              <div class="sub-header">
                <strong>API Catalog</strong>
                <el-button size="small" :loading="rainbondCatalogLoading" @click="handleRainbondCatalog">Refresh</el-button>
              </div>
              <el-input v-model="rainbondKeyword" clearable placeholder="Search Rainbond API" />
              <el-scrollbar class="rainbond-catalog-list">
                <button
                  v-for="item in filteredRainbondCatalog"
                  :key="`${item.method}-${item.path}-${item.name}`"
                  class="endpoint-item"
                  @click="selectRainbondEndpoint(item)"
                >
                  <div class="endpoint-title">
                    <el-tag size="small" :type="methodTagType(item.method)">{{ item.method }}</el-tag>
                    <code>{{ item.path }}</code>
                  </div>
                  <p>{{ item.category }} / {{ item.name }}</p>
                </button>
              </el-scrollbar>
            </section>
            <section>
              <div class="sub-header">
                <strong>Rainbond Request</strong>
                <el-button type="primary" :loading="rainbondCalling" @click="handleCallRainbondApi">Call</el-button>
              </div>
              <el-form label-position="top">
                <el-form-item label="Method">
                  <el-select v-model="rainbondRequest.method">
                    <el-option v-for="item in methodOptions" :key="item" :label="item" :value="item" />
                  </el-select>
                </el-form-item>
                <el-form-item label="Path">
                  <el-input v-model.trim="rainbondRequest.path" placeholder="/openapi/v1/teams" />
                </el-form-item>
                <el-form-item label="Query JSON">
                  <el-input v-model="rainbondRequest.queryText" type="textarea" :rows="3" placeholder='{"page":1,"page_size":20}' />
                </el-form-item>
                <el-form-item label="Body JSON">
                  <el-input v-model="rainbondRequest.bodyText" type="textarea" :rows="5" placeholder="{}" />
                </el-form-item>
              </el-form>
              <div class="response-block compact">
                <div class="response-meta">
                  <strong>Rainbond Result</strong>
                  <span v-if="rainbondResponse.statusCode">HTTP {{ rainbondResponse.statusCode }}</span>
                </div>
                <pre>{{ rainbondResponse.body || rainbondDiscoveryText || 'No Rainbond API response yet' }}</pre>
              </div>
            </section>
          </div>
        </section>

        <div class="workbench-layout">
          <section class="workbench-panel resource-panel">
            <div class="panel-header">
              <h2>服务文档</h2>
              <el-input
                v-model="resourceKeyword"
                placeholder="搜索服务名"
                clearable
              />
            </div>
            <el-scrollbar class="panel-scroll">
              <el-empty v-if="!filteredResources.length && !resourcesLoading" description="暂无 Swagger 资源" />
              <button
                v-for="item in filteredResources"
                :key="item.id"
                class="resource-item"
                :class="{ active: selectedResource?.id === item.id }"
                @click="selectResource(item)"
              >
                <strong>{{ item.name }}</strong>
                <span>{{ item.url }}</span>
              </button>
            </el-scrollbar>
          </section>

          <section class="workbench-panel endpoint-panel">
            <div class="panel-header">
              <h2>接口列表</h2>
              <el-input
                v-model="endpointKeyword"
                placeholder="搜索 path / summary"
                clearable
              />
            </div>
            <el-scrollbar class="panel-scroll">
              <el-empty
                v-if="!filteredEndpoints.length && !docLoading"
                :description="selectedResource ? '当前服务没有可用接口' : '先选择左侧服务文档'"
              />
              <button
                v-for="item in filteredEndpoints"
                :key="`${item.method}-${item.path}`"
                class="endpoint-item"
                :class="{ active: selectedEndpoint && selectedEndpoint.path === item.path && selectedEndpoint.method === item.method }"
                @click="selectEndpoint(item)"
              >
                <div class="endpoint-title">
                  <el-tag size="small" :type="methodTagType(item.method)">{{ item.method }}</el-tag>
                  <code>{{ item.path }}</code>
                </div>
                <p>{{ item.summary || item.operationId || '未命名接口' }}</p>
                <small>{{ item.tag || '未分类' }}</small>
              </button>
            </el-scrollbar>
          </section>

          <section class="workbench-panel debug-panel">
            <div class="panel-header">
              <h2>请求调试</h2>
              <el-button type="primary" :loading="invoking" @click="invokeApi">发送请求</el-button>
            </div>

            <el-form label-position="top" class="debug-form">
              <el-form-item label="请求方法">
                <el-select v-model="requestForm.method">
                  <el-option v-for="item in methodOptions" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>

              <el-form-item label="请求路径">
                <el-input v-model="requestForm.path" placeholder="/api/example/path" />
              </el-form-item>

              <el-form-item label="Query JSON">
                <el-input
                  v-model="requestForm.queryText"
                  type="textarea"
                  :rows="4"
                  placeholder='{"page":1,"size":10}'
                />
              </el-form-item>

              <el-form-item label="Body JSON">
                <el-input
                  v-model="requestForm.bodyText"
                  type="textarea"
                  :rows="8"
                  placeholder='{"name":"demo"}'
                />
              </el-form-item>

              <el-form-item label="附加请求头 JSON">
                <el-input
                  v-model="requestForm.headersText"
                  type="textarea"
                  :rows="4"
                  placeholder='{"X-Debug":"true"}'
                />
              </el-form-item>
            </el-form>

            <div class="preview-block">
              <span class="preview-label">实际请求地址</span>
              <code>{{ requestUrlPreview || '-' }}</code>
            </div>

            <div class="response-block">
              <div class="response-meta">
                <strong>返回结果</strong>
                <span v-if="responseState.statusCode">HTTP {{ responseState.statusCode }}</span>
              </div>
              <pre>{{ responseState.body || '尚未调用接口' }}</pre>
            </div>
          </section>
        </div>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  callRainbondApi,
  getApiWorkbenchDoc,
  getApiWorkbenchResources,
  getRainbondCatalog,
  getRainbondConfig,
  getRainbondDiscovery,
  saveRainbondConfig
} from '@/api/admin-ops'
import {
  buildApiWorkbenchUrl,
  canAccessApiWorkbench,
  normalizeApiWorkbenchEndpoints,
  normalizeApiWorkbenchResources,
  parseWorkbenchJson
} from '@/utils/adminApiWorkbench'

const userStore = useUserStore()

const methodOptions = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH']
const resourcesLoading = ref(false)
const docLoading = ref(false)
const invoking = ref(false)
const rainbondLoading = ref(false)
const rainbondSaving = ref(false)
const rainbondCatalogLoading = ref(false)
const rainbondDiscovering = ref(false)
const rainbondCalling = ref(false)
const resourceKeyword = ref('')
const endpointKeyword = ref('')
const rainbondKeyword = ref('')
const resources = ref([])
const endpoints = ref([])
const rainbondCatalog = ref([])
const rainbondDiscoveryText = ref('')
const selectedResource = ref(null)
const selectedEndpoint = ref(null)
const responseState = reactive({
  statusCode: null,
  body: ''
})
const rainbondResponse = reactive({
  statusCode: null,
  body: ''
})

const requestForm = reactive({
  method: 'GET',
  path: '',
  queryText: '',
  bodyText: '',
  headersText: ''
})
const rainbondForm = reactive({
  enabled: false,
  baseUrl: '',
  token: '',
  enterpriseId: '',
  teamId: '',
  regionName: '',
  timeoutSeconds: 15
})
const rainbondRequest = reactive({
  method: 'GET',
  path: '/openapi/v1/teams',
  queryText: '',
  bodyText: ''
})

const canUseWorkbench = computed(() => canAccessApiWorkbench(userStore.tenantId))
const tokenPreview = computed(() => {
  const token = localStorage.getItem('token') || ''
  if (!token) return '当前未登录'
  if (token.length <= 24) return token
  return `${token.slice(0, 12)}...${token.slice(-12)}`
})

const filteredResources = computed(() => {
  const keyword = resourceKeyword.value.trim().toLowerCase()
  if (!keyword) return resources.value
  return resources.value.filter((item) => `${item.name} ${item.url}`.toLowerCase().includes(keyword))
})

const filteredEndpoints = computed(() => {
  const keyword = endpointKeyword.value.trim().toLowerCase()
  if (!keyword) return endpoints.value
  return endpoints.value.filter((item) =>
    `${item.method} ${item.path} ${item.summary} ${item.operationId} ${item.tag}`.toLowerCase().includes(keyword)
  )
})

const filteredRainbondCatalog = computed(() => {
  const keyword = rainbondKeyword.value.trim().toLowerCase()
  if (!keyword) return rainbondCatalog.value
  return rainbondCatalog.value.filter((item) =>
    `${item.category} ${item.name} ${item.method} ${item.path}`.toLowerCase().includes(keyword)
  )
})

const requestUrlPreview = computed(() => buildApiWorkbenchUrl(requestForm.path, requestForm.queryText))

function methodTagType(method) {
  if (method === 'GET') return 'success'
  if (method === 'POST') return 'primary'
  if (method === 'PUT') return 'warning'
  if (method === 'DELETE') return 'danger'
  return 'info'
}

async function refreshResources() {
  if (!canUseWorkbench.value) return
  resourcesLoading.value = true
  try {
    const res = await getApiWorkbenchResources()
    resources.value = normalizeApiWorkbenchResources(res.data || [])
    if (!selectedResource.value && resources.value.length) {
      await selectResource(resources.value[0])
    }
  } catch (error) {
    ElMessage.error(error.message || '获取 Swagger 资源失败')
  } finally {
    resourcesLoading.value = false
  }
}

async function selectResource(item) {
  selectedResource.value = item
  endpoints.value = []
  selectedEndpoint.value = null
  docLoading.value = true
  try {
    const res = await getApiWorkbenchDoc(item.url)
    endpoints.value = normalizeApiWorkbenchEndpoints(res.data?.endpoints || [])
  } catch (error) {
    ElMessage.error(error.message || '加载接口文档失败')
  } finally {
    docLoading.value = false
  }
}

function selectEndpoint(item) {
  selectedEndpoint.value = item
  requestForm.method = item.method
  requestForm.path = item.path
  requestForm.queryText = ''
  requestForm.bodyText = item.method === 'GET' || item.method === 'DELETE' ? '' : '{}'
  responseState.statusCode = null
  responseState.body = ''
}

async function copyToken() {
  const token = localStorage.getItem('token') || ''
  if (!token) {
    ElMessage.warning('当前没有可复制的 token')
    return
  }
  await navigator.clipboard.writeText(token)
  ElMessage.success('Token 已复制')
}

async function invokeApi() {
  if (!requestForm.path.trim()) {
    ElMessage.warning('请先填写请求路径')
    return
  }
  invoking.value = true
  try {
    const token = localStorage.getItem('token') || ''
    const tenantId = localStorage.getItem('tenantId') || ''
    const headers = {
      Accept: 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(tenantId ? { 'X-Tenant-Id': tenantId } : {}),
      ...parseWorkbenchJson(requestForm.headersText, {})
    }
    const bodyObject = parseWorkbenchJson(requestForm.bodyText, null)
    const method = requestForm.method.toUpperCase()
    const response = await fetch(requestUrlPreview.value, {
      method,
      headers: {
        ...headers,
        ...(bodyObject !== null && method !== 'GET' && method !== 'DELETE' ? { 'Content-Type': 'application/json' } : {})
      },
      body: bodyObject !== null && method !== 'GET' && method !== 'DELETE'
        ? JSON.stringify(bodyObject)
        : undefined
    })
    responseState.statusCode = response.status
    const rawText = await response.text()
    try {
      responseState.body = JSON.stringify(JSON.parse(rawText), null, 2)
    } catch {
      responseState.body = rawText
    }
  } catch (error) {
    responseState.statusCode = 0
    responseState.body = error.message || String(error)
    ElMessage.error('接口调用失败')
  } finally {
    invoking.value = false
  }
}

function assignRainbondConfig(data = {}) {
  Object.assign(rainbondForm, {
    enabled: Boolean(data.enabled),
    baseUrl: data.baseUrl || '',
    token: '',
    enterpriseId: data.enterpriseId || '',
    teamId: data.teamId || '',
    regionName: data.regionName || '',
    timeoutSeconds: data.timeoutSeconds || 15
  })
}

async function fetchRainbondConfig() {
  rainbondLoading.value = true
  try {
    const res = await getRainbondConfig()
    if (res.code !== 200 || !res.data) {
      throw new Error(res.msg || 'Load Rainbond config failed')
    }
    assignRainbondConfig(res.data)
  } catch (error) {
    ElMessage.error(error.message || 'Load Rainbond config failed')
  } finally {
    rainbondLoading.value = false
  }
}

async function handleSaveRainbondConfig() {
  rainbondSaving.value = true
  try {
    const payload = { ...rainbondForm }
    if (!payload.token) {
      delete payload.token
    }
    const res = await saveRainbondConfig(payload)
    if (res.code !== 200 || !res.data) {
      throw new Error(res.msg || 'Save Rainbond config failed')
    }
    assignRainbondConfig(res.data)
    ElMessage.success('Rainbond config saved')
  } catch (error) {
    ElMessage.error(error.message || 'Save Rainbond config failed')
  } finally {
    rainbondSaving.value = false
  }
}

async function handleRainbondCatalog() {
  rainbondCatalogLoading.value = true
  try {
    const res = await getRainbondCatalog()
    const categories = res.data?.categories || {}
    rainbondCatalog.value = Object.entries(categories).flatMap(([category, items]) =>
      (Array.isArray(items) ? items : []).map((item) => ({ category, ...item }))
    )
  } catch (error) {
    ElMessage.error(error.message || 'Load Rainbond catalog failed')
  } finally {
    rainbondCatalogLoading.value = false
  }
}

async function handleRainbondDiscovery() {
  rainbondDiscovering.value = true
  try {
    const res = await getRainbondDiscovery()
    rainbondDiscoveryText.value = JSON.stringify(res.data || {}, null, 2)
  } catch (error) {
    rainbondDiscoveryText.value = error.message || String(error)
    ElMessage.error(error.message || 'Rainbond discovery failed')
  } finally {
    rainbondDiscovering.value = false
  }
}

function selectRainbondEndpoint(item) {
  rainbondRequest.method = item.method || 'GET'
  rainbondRequest.path = item.path || ''
  rainbondRequest.queryText = ''
  rainbondRequest.bodyText = ['GET', 'DELETE'].includes(rainbondRequest.method) ? '' : '{}'
}

async function handleCallRainbondApi() {
  if (!rainbondRequest.path.startsWith('/openapi/')) {
    ElMessage.warning('Rainbond path must start with /openapi/')
    return
  }
  rainbondCalling.value = true
  try {
    const res = await callRainbondApi({
      method: rainbondRequest.method,
      path: rainbondRequest.path,
      query: parseWorkbenchJson(rainbondRequest.queryText, {}),
      body: parseWorkbenchJson(rainbondRequest.bodyText, null)
    })
    if (res.code !== 200 || !res.data) {
      throw new Error(res.msg || 'Rainbond API call failed')
    }
    rainbondResponse.statusCode = res.data.statusCode
    const raw = res.data.responseBody || ''
    try {
      rainbondResponse.body = JSON.stringify(JSON.parse(raw), null, 2)
    } catch {
      rainbondResponse.body = raw
    }
  } catch (error) {
    rainbondResponse.statusCode = 0
    rainbondResponse.body = error.message || String(error)
    ElMessage.error('Rainbond API call failed')
  } finally {
    rainbondCalling.value = false
  }
}

onMounted(() => {
  refreshResources()
  fetchRainbondConfig()
  handleRainbondCatalog()
})
</script>

<style scoped>
.workbench-card {
  min-height: 100%;
}

.card-header,
.panel-header,
.response-meta {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.card-header h1,
.panel-header h2 {
  margin: 0;
}

.card-header p {
  margin: 8px 0 0;
  color: #909399;
}

.actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.token-bar {
  margin: 18px 0;
  display: grid;
  gap: 8px;
}

.token-label,
.preview-label {
  font-size: 13px;
  color: #909399;
}

.token-preview,
.preview-block code {
  display: block;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f5f7fa;
  word-break: break-all;
}

.workbench-layout {
  display: grid;
  grid-template-columns: 280px 380px minmax(0, 1fr);
  gap: 18px;
}

.rainbond-panel {
  margin-bottom: 18px;
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
}

.rainbond-panel p {
  margin: 6px 0 0;
  color: #606266;
}

.rainbond-form {
  display: grid;
  grid-template-columns: 120px repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.rainbond-workbench {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 0.9fr);
  gap: 16px;
  margin-top: 12px;
}

.sub-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.rainbond-catalog-list {
  height: 320px;
  margin-top: 10px;
}

.workbench-panel {
  border: 1px solid #ebeef5;
  border-radius: 16px;
  padding: 16px;
  min-height: 680px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel-scroll {
  min-height: 0;
  flex: 1;
}

.resource-item,
.endpoint-item {
  width: 100%;
  display: grid;
  gap: 6px;
  padding: 12px;
  margin-bottom: 10px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
  text-align: left;
  cursor: pointer;
}

.resource-item.active,
.endpoint-item.active {
  border-color: #409eff;
  box-shadow: 0 0 0 1px rgba(64, 158, 255, 0.18);
}

.resource-item span,
.endpoint-item p,
.endpoint-item small {
  color: #606266;
  margin: 0;
}

.endpoint-title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.debug-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 16px;
}

.debug-form :deep(.el-form-item:nth-child(n + 3)) {
  grid-column: 1 / -1;
}

.preview-block,
.response-block {
  display: grid;
  gap: 8px;
}

.response-block pre {
  margin: 0;
  min-height: 260px;
  max-height: 520px;
  overflow: auto;
  padding: 14px;
  border-radius: 12px;
  background: #111827;
  color: #e5eefc;
  font-size: 12px;
  line-height: 1.55;
}

.response-block.compact pre {
  min-height: 180px;
  max-height: 340px;
}

@media (max-width: 1280px) {
  .workbench-layout {
    grid-template-columns: 1fr;
  }

  .rainbond-form,
  .rainbond-workbench {
    grid-template-columns: 1fr;
  }

  .workbench-panel {
    min-height: 420px;
  }
}

@media (max-width: 768px) {
  .card-header,
  .panel-header,
  .response-meta {
    flex-direction: column;
  }

  .debug-form {
    grid-template-columns: 1fr;
  }
}
</style>
