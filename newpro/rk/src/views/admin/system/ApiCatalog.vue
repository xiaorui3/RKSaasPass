<template>
  <div class="api-catalog-page">
    <section class="page-header">
      <div>
        <h1>接口目录</h1>
        <p>按当前登录态和当前租户上下文浏览网关 Swagger 目录，调试请求由后端代理转发，避免在浏览器里暴露敏感凭据。</p>
      </div>
      <div class="header-actions">
        <el-tag type="success" size="large">当前登录态</el-tag>
        <el-tag type="info" size="large">当前租户上下文：{{ catalog.tenantScope || '-' }}</el-tag>
        <el-button type="primary" :loading="loading" @click="loadCatalog">刷新目录</el-button>
      </div>
    </section>

    <section class="summary-grid">
      <div v-for="item in summaryCards" :key="item.label" class="summary-card" :class="item.tone">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.hint }}</small>
      </div>
    </section>

    <section class="catalog-workspace">
      <aside class="service-panel">
        <div class="panel-title">
          <strong>服务分组</strong>
          <span>{{ serviceGroups.length }} 个服务</span>
        </div>
        <el-input v-model="serviceKeyword" clearable placeholder="搜索服务" />
        <el-scrollbar class="service-list">
          <button
            v-for="service in filteredServices"
            :key="service.name"
            class="service-item"
            :class="{ active: selectedServiceName === service.name }"
            @click="selectService(service.name)"
          >
            <strong>{{ service.name }}</strong>
            <span>{{ service.endpointCount || 0 }} 个接口</span>
          </button>
          <el-empty v-if="!filteredServices.length && !loading" description="暂无服务分组" />
        </el-scrollbar>
      </aside>

      <main class="endpoint-panel">
        <div class="toolbar">
          <el-input v-model="endpointKeyword" clearable placeholder="搜索 path / summary / 权限点" />
          <el-select v-model="categoryFilter" class="category-select">
            <el-option label="全部 API" value="ALL" />
            <el-option label="公开 API" value="PUBLIC" />
            <el-option label="后台 API" value="ADMIN" />
            <el-option label="内部 API" value="INTERNAL" />
          </el-select>
        </div>

        <el-table
          v-loading="loading"
          :data="filteredEndpoints"
          size="small"
          height="420"
          highlight-current-row
          empty-text="暂无接口"
          @row-click="selectEndpoint"
        >
          <el-table-column label="方法" width="92">
            <template #default="{ row }">
              <el-tag :type="methodTagType(row.method)" size="small">{{ row.method }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="path" label="路径" min-width="260" show-overflow-tooltip />
          <el-table-column label="分类" width="110">
            <template #default="{ row }">
              <el-tag :type="categoryTagType(row.category)" size="small">{{ categoryLabel(row.category) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="summary" label="说明" min-width="180" show-overflow-tooltip />
          <el-table-column prop="permissionPoint" label="权限点" min-width="180" show-overflow-tooltip />
        </el-table>
      </main>

      <aside class="detail-panel">
        <div class="panel-title">
          <strong>示例请求</strong>
          <span>{{ selectedEndpointItem?.serviceName || '未选择接口' }}</span>
        </div>
        <template v-if="selectedEndpointItem">
          <div class="endpoint-detail">
            <div>
              <span>接口</span>
              <code>{{ selectedEndpointItem.method }} {{ selectedEndpointItem.path }}</code>
            </div>
            <div>
              <span>类型</span>
              <strong>{{ categoryLabel(selectedEndpointItem.category) }}</strong>
            </div>
            <div>
              <span>权限点</span>
              <strong>{{ selectedEndpointItem.permissionPoint || '未声明' }}</strong>
            </div>
            <div>
              <span>标签</span>
              <strong>{{ selectedEndpointItem.tag || '未分类' }}</strong>
            </div>
          </div>

          <section class="debug-box">
            <div class="debug-title">
              <strong>请求调试</strong>
              <el-button type="primary" :loading="debugging" @click="sendDebugRequest">发送</el-button>
            </div>
            <el-form label-position="top">
              <el-form-item label="Query JSON">
                <el-input
                  v-model="debugForm.queryJson"
                  type="textarea"
                  :rows="4"
                  spellcheck="false"
                  placeholder='{"page":1,"size":10}'
                />
              </el-form-item>
              <el-form-item label="Body JSON">
                <el-input
                  v-model="debugForm.bodyJson"
                  type="textarea"
                  :rows="6"
                  spellcheck="false"
                  placeholder='{"name":"demo"}'
                />
              </el-form-item>
            </el-form>
            <div class="context-note">
              <span>当前登录态</span>
              <span>当前租户上下文</span>
              <span>后端受控代理</span>
            </div>
          </section>

          <section class="response-box">
            <div class="response-meta">
              <strong>调试结果</strong>
              <span v-if="debugResult.statusCode">HTTP {{ debugResult.statusCode }}</span>
            </div>
            <pre>{{ debugResult.body || '尚未发送请求' }}</pre>
          </section>
        </template>
        <el-empty v-else description="选择一个接口查看详情" />
      </aside>
    </section>

    <section v-if="warningItems.length" class="warning-board">
      <strong>目录提示</strong>
      <span v-for="item in warningItems" :key="item">{{ item }}</span>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { debugApiCatalogEndpoint, getApiCatalog } from '@/api/api-catalog'

const loading = ref(false)
const debugging = ref(false)
const serviceKeyword = ref('')
const endpointKeyword = ref('')
const categoryFilter = ref('ALL')
const selectedServiceName = ref('ALL')
const selectedEndpointItem = ref(null)

const catalog = reactive({
  generatedAt: '',
  tenantScope: '',
  serviceCount: 0,
  endpointCount: 0,
  publicApiCount: 0,
  adminApiCount: 0,
  internalApiCount: 0,
  services: [],
  warnings: []
})

const debugForm = reactive({
  queryJson: '{}',
  bodyJson: '{}'
})

const debugResult = reactive({
  statusCode: '',
  body: ''
})

const serviceGroups = computed(() => Array.isArray(catalog.services) ? catalog.services : [])
const warningItems = computed(() => Array.isArray(catalog.warnings) ? catalog.warnings : [])

const allEndpoints = computed(() => serviceGroups.value.flatMap((service) => {
  const endpoints = Array.isArray(service.endpoints) ? service.endpoints : []
  return endpoints.map((endpoint) => ({
    ...endpoint,
    serviceName: endpoint.serviceName || service.name
  }))
}))

const summaryCards = computed(() => [
  { label: '服务分组', value: numberValue(catalog.serviceCount), hint: '来自网关文档', tone: 'neutral' },
  { label: '接口总数', value: numberValue(catalog.endpointCount), hint: 'GET/POST/PUT/DELETE/PATCH', tone: 'neutral' },
  { label: '公开 API', value: numberValue(catalog.publicApiCount), hint: '前台或开放调用', tone: 'public' },
  { label: '后台 API', value: numberValue(catalog.adminApiCount), hint: '管理端和审核流', tone: 'admin' },
  { label: '内部 API', value: numberValue(catalog.internalApiCount), hint: '服务内部链路', tone: 'internal' }
])

const filteredServices = computed(() => {
  const keyword = serviceKeyword.value.trim().toLowerCase()
  const services = serviceGroups.value
  return keyword
    ? services.filter((item) => String(item.name || '').toLowerCase().includes(keyword))
    : services
})

const filteredEndpoints = computed(() => {
  const keyword = endpointKeyword.value.trim().toLowerCase()
  return allEndpoints.value.filter((item) => {
    const serviceMatched = selectedServiceName.value === 'ALL' || item.serviceName === selectedServiceName.value
    const categoryMatched = categoryFilter.value === 'ALL' || item.category === categoryFilter.value
    const text = [
      item.path,
      item.summary,
      item.operationId,
      item.permissionPoint,
      item.tag,
      item.serviceName
    ].join(' ').toLowerCase()
    return serviceMatched && categoryMatched && (!keyword || text.includes(keyword))
  })
})

function unwrapResponse(res) {
  if (res?.code && res.code !== 200) {
    throw new Error(res.msg || '接口目录请求失败')
  }
  return res?.data || res || {}
}

function assignCatalog(data) {
  Object.assign(catalog, {
    generatedAt: data.generatedAt || '',
    tenantScope: data.tenantScope || '',
    serviceCount: data.serviceCount || 0,
    endpointCount: data.endpointCount || 0,
    publicApiCount: data.publicApiCount || 0,
    adminApiCount: data.adminApiCount || 0,
    internalApiCount: data.internalApiCount || 0,
    services: Array.isArray(data.services) ? data.services : [],
    warnings: Array.isArray(data.warnings) ? data.warnings : []
  })
  selectedEndpointItem.value = filteredEndpoints.value[0] || null
  fillDebugForm(selectedEndpointItem.value)
}

async function loadCatalog() {
  loading.value = true
  try {
    const res = await getApiCatalog()
    assignCatalog(unwrapResponse(res))
  } catch (error) {
    ElMessage.error(error.message || '接口目录加载失败')
  } finally {
    loading.value = false
  }
}

function selectService(name) {
  selectedServiceName.value = selectedServiceName.value === name ? 'ALL' : name
  selectedEndpointItem.value = filteredEndpoints.value[0] || null
  fillDebugForm(selectedEndpointItem.value)
}

function selectEndpoint(row) {
  selectedEndpointItem.value = row
  fillDebugForm(row)
}

function fillDebugForm(endpoint) {
  debugForm.queryJson = endpoint?.sampleQueryJson || '{}'
  debugForm.bodyJson = endpoint?.sampleBodyJson || '{}'
  debugResult.statusCode = ''
  debugResult.body = ''
}

async function sendDebugRequest() {
  if (!selectedEndpointItem.value) {
    ElMessage.warning('请先选择接口')
    return
  }
  debugging.value = true
  try {
    const res = await debugApiCatalogEndpoint({
      method: selectedEndpointItem.value.method,
      path: selectedEndpointItem.value.path,
      queryJson: debugForm.queryJson,
      bodyJson: debugForm.bodyJson
    })
    const data = unwrapResponse(res)
    debugResult.statusCode = data.statusCode || ''
    debugResult.body = typeof data.body === 'string' ? data.body : JSON.stringify(data.body || data, null, 2)
  } catch (error) {
    ElMessage.error(error.message || '接口调试失败')
  } finally {
    debugging.value = false
  }
}

function numberValue(value) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

function categoryLabel(category) {
  if (category === 'PUBLIC') return '公开 API'
  if (category === 'ADMIN') return '后台 API'
  if (category === 'INTERNAL') return '内部 API'
  return category || '未分类'
}

function categoryTagType(category) {
  if (category === 'PUBLIC') return 'success'
  if (category === 'ADMIN') return 'warning'
  if (category === 'INTERNAL') return 'danger'
  return 'info'
}

function methodTagType(method) {
  if (method === 'GET') return 'success'
  if (method === 'POST') return 'primary'
  if (method === 'PUT') return 'warning'
  if (method === 'DELETE') return 'danger'
  return 'info'
}

onMounted(loadCatalog)
</script>

<style scoped>
.api-catalog-page {
  min-height: 100%;
  padding: 24px;
  background: #f5f7fb;
  color: #172033;
}

.page-header,
.catalog-workspace,
.warning-board {
  background: #fff;
  border: 1px solid #e3e8f2;
  border-radius: 8px;
  box-shadow: 0 10px 30px rgba(18, 33, 62, 0.05);
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 24px;
}

.page-header h1 {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 700;
}

.page-header p {
  max-width: 800px;
  margin: 0;
  color: #607086;
  line-height: 1.7;
}

.header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(140px, 1fr));
  gap: 14px;
  margin: 16px 0;
}

.summary-card {
  min-height: 94px;
  padding: 16px;
  border: 1px solid #e3e8f2;
  border-radius: 8px;
  background: #fff;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.summary-card span,
.summary-card small {
  color: #65748b;
}

.summary-card strong {
  font-size: 28px;
  line-height: 1;
}

.summary-card.public strong {
  color: #1b8f5a;
}

.summary-card.admin strong {
  color: #b46b00;
}

.summary-card.internal strong {
  color: #c0362c;
}

.catalog-workspace {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr) 360px;
  gap: 16px;
  padding: 18px;
}

.service-panel,
.endpoint-panel,
.detail-panel {
  min-width: 0;
}

.panel-title,
.toolbar,
.debug-title,
.response-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.panel-title span,
.response-meta span {
  color: #65748b;
  font-size: 13px;
}

.service-list {
  height: 470px;
  margin-top: 12px;
}

.service-item {
  width: 100%;
  border: 1px solid #e6edf7;
  border-radius: 8px;
  background: #fff;
  padding: 12px;
  margin-bottom: 10px;
  text-align: left;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #172033;
}

.service-item span {
  color: #65748b;
  font-size: 13px;
}

.service-item.active {
  border-color: #2f6fdd;
  background: #f0f6ff;
}

.toolbar {
  align-items: stretch;
}

.category-select {
  width: 150px;
}

.endpoint-detail {
  display: grid;
  gap: 10px;
  padding: 12px;
  border: 1px solid #e6edf7;
  border-radius: 8px;
  background: #fbfdff;
}

.endpoint-detail div {
  display: grid;
  gap: 4px;
}

.endpoint-detail span {
  color: #65748b;
  font-size: 13px;
}

.endpoint-detail code {
  white-space: normal;
  word-break: break-all;
  color: #1f4f99;
}

.debug-box {
  margin-top: 14px;
  padding: 14px;
  border: 1px solid #e6edf7;
  border-radius: 8px;
}

.context-note {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.context-note span {
  padding: 4px 8px;
  border-radius: 999px;
  background: #eef4ff;
  color: #315d9f;
  font-size: 12px;
}

.response-box {
  margin-top: 14px;
  border: 1px solid #e6edf7;
  border-radius: 8px;
  overflow: hidden;
}

.response-meta {
  margin: 0;
  padding: 10px 12px;
  background: #f7f9fc;
  border-bottom: 1px solid #e6edf7;
}

.response-box pre {
  max-height: 220px;
  margin: 0;
  padding: 12px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  line-height: 1.6;
}

.warning-board {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 16px;
  padding: 16px;
  color: #8a5a00;
}

@media (max-width: 1280px) {
  .catalog-workspace {
    grid-template-columns: 220px minmax(0, 1fr);
  }

  .detail-panel {
    grid-column: 1 / -1;
  }
}

@media (max-width: 900px) {
  .page-header {
    flex-direction: column;
  }

  .summary-grid,
  .catalog-workspace {
    grid-template-columns: 1fr;
  }

  .service-list {
    height: 260px;
  }
}
</style>
