<template>
  <div class="service-monitor">
    <div class="page-header">
      <div>
        <h2>微服务监控</h2>
        <p>通过后端聚合 Nacos 实例、主机指标和中间件状态，避免浏览器跨域直连导致误判。</p>
      </div>
      <el-button type="primary" :loading="loading" @click="checkAllHealth">刷新状态</el-button>
    </div>

    <div class="summary-grid">
      <el-card v-for="item in summaryCards" :key="item.label" shadow="never" class="summary-card">
        <span class="label">{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </el-card>
    </div>

    <el-card class="topology-card">
      <template #header>
        <span class="card-title">系统架构拓扑</span>
      </template>
      <div class="topology-svg">
        <svg viewBox="0 0 960 520" class="arch-diagram">
          <text x="480" y="28" text-anchor="middle" font-size="14" font-weight="bold" fill="#606266">前端访问入口</text>
          <rect x="360" y="36" width="240" height="36" rx="6" fill="#409eff" opacity="0.15" stroke="#409eff" stroke-width="1.5" />
          <text x="480" y="60" text-anchor="middle" font-size="13" fill="#409eff">{{ frontendEntryLabel }}</text>

          <line x1="480" y1="72" x2="480" y2="95" stroke="#c0c4cc" stroke-width="1.5" marker-end="url(#arrow)" />

          <rect x="330" y="95" width="300" height="48" rx="8" :fill="getServiceColor('rk-gateway')" stroke="#303133" stroke-width="1.5" />
          <text x="480" y="115" text-anchor="middle" font-size="13" font-weight="bold" fill="#fff">API Gateway</text>
          <text x="480" y="133" text-anchor="middle" font-size="11" fill="rgba(255,255,255,0.8)">{{ gatewayTopologyLabel }}</text>

          <line x1="280" y1="143" x2="140" y2="190" stroke="#c0c4cc" stroke-width="1" marker-end="url(#arrow)" />
          <line x1="380" y1="143" x2="340" y2="190" stroke="#c0c4cc" stroke-width="1" marker-end="url(#arrow)" />
          <line x1="480" y1="143" x2="480" y2="190" stroke="#c0c4cc" stroke-width="1" marker-end="url(#arrow)" />
          <line x1="580" y1="143" x2="620" y2="190" stroke="#c0c4cc" stroke-width="1" marker-end="url(#arrow)" />
          <line x1="680" y1="143" x2="790" y2="190" stroke="#c0c4cc" stroke-width="1" marker-end="url(#arrow)" />

          <g v-for="svc in servicesRow1" :key="svc.name">
            <rect :x="svc.x" y="190" width="155" height="58" rx="6" :fill="getServiceColor(svc.name)" stroke="#303133" stroke-width="1" />
            <text :x="svc.x + 78" y="212" text-anchor="middle" font-size="12" font-weight="bold" fill="#fff">{{ svc.name }}</text>
            <text :x="svc.x + 78" y="230" text-anchor="middle" font-size="10" fill="rgba(255,255,255,0.8)">:{{ svc.port }} {{ svc.desc }}</text>
            <circle :cx="svc.x + 143" cy="198" r="5" :fill="getStatusColor(svc.name)" />
          </g>

          <g v-for="svc in servicesRow2" :key="svc.name">
            <rect :x="svc.x" y="268" width="155" height="58" rx="6" :fill="getServiceColor(svc.name)" stroke="#303133" stroke-width="1" />
            <text :x="svc.x + 78" y="290" text-anchor="middle" font-size="12" font-weight="bold" fill="#fff">{{ svc.name }}</text>
            <text :x="svc.x + 78" y="308" text-anchor="middle" font-size="10" fill="rgba(255,255,255,0.8)">:{{ svc.port }} {{ svc.desc }}</text>
            <circle :cx="svc.x + 143" cy="276" r="5" :fill="getStatusColor(svc.name)" />
          </g>

          <line x1="180" y1="326" x2="120" y2="380" stroke="#e6a23c" stroke-width="1.2" stroke-dasharray="4" />
          <line x1="380" y1="326" x2="320" y2="380" stroke="#f56c6c" stroke-width="1.2" stroke-dasharray="4" />
          <line x1="580" y1="326" x2="540" y2="380" stroke="#67c23a" stroke-width="1.2" stroke-dasharray="4" />
          <line x1="780" y1="326" x2="740" y2="380" stroke="#909399" stroke-width="1.2" stroke-dasharray="4" />

          <g v-for="mw in middlewareTopology" :key="mw.name">
            <rect :x="mw.x" y="380" width="170" height="52" rx="8" :fill="mw.color" opacity="0.2" :stroke="mw.color" stroke-width="1.5" />
            <text :x="mw.x + 85" y="402" text-anchor="middle" font-size="12" font-weight="bold" :fill="mw.color">{{ mw.name }}</text>
            <text :x="mw.x + 85" y="420" text-anchor="middle" font-size="10" fill="#606266">{{ mw.desc }}</text>
          </g>

          <circle cx="30" cy="480" r="6" fill="#67c23a" />
          <text x="42" y="484" font-size="11" fill="#606266">在线</text>
          <circle cx="90" cy="480" r="6" fill="#e6a23c" />
          <text x="102" y="484" font-size="11" fill="#606266">未知</text>
          <circle cx="150" cy="480" r="6" fill="#f56c6c" />
          <text x="162" y="484" font-size="11" fill="#606266">离线</text>

          <defs>
            <marker id="arrow" markerWidth="8" markerHeight="6" refX="8" refY="3" orient="auto">
              <path d="M0,0 L8,3 L0,6" fill="#c0c4cc" />
            </marker>
          </defs>
        </svg>
      </div>
    </el-card>

    <el-card>
      <template #header>
        <span class="card-title">服务状态详情</span>
      </template>
      <el-table :data="allServices" v-loading="loading" stripe row-key="name" empty-text="暂无服务监控数据">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="instance-panel">
              <div class="instance-summary">
                <span>健康实例 {{ row.healthyInstances ?? 0 }}/{{ row.instances ?? 0 }}</span>
                <span>响应时间 {{ row.responseTime ?? row.latency ?? '-' }}</span>
                <span>QPS {{ row.qps ?? 0 }}</span>
              </div>
              <el-empty v-if="!row.instanceDetails || !row.instanceDetails.length" description="当前没有实例详情" />
              <el-table v-else :data="row.instanceDetails" size="small" border>
                <el-table-column prop="ip" label="IP" min-width="140" />
                <el-table-column prop="port" label="端口" width="90" />
                <el-table-column label="健康状态" width="110">
                  <template #default="{ row: instance }">
                    <el-tag :type="instance.healthy ? 'success' : 'danger'" size="small">
                      {{ instance.healthy ? '健康' : '异常' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="启用" width="90">
                  <template #default="{ row: instance }">
                    <el-tag :type="instance.enabled ? 'success' : 'info'" size="small">
                      {{ instance.enabled ? '是' : '否' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="weight" label="权重" width="90" />
                <el-table-column prop="clusterName" label="集群" min-width="120" />
                <el-table-column label="终端" width="110" fixed="right">
                  <template #default="{ row: instance }">
                    <el-button
                      text
                      type="warning"
                      :disabled="!instance.podName || !instance.containerName"
                      @click="handleOpenPodTerminal(instance)"
                    >
                      Terminal
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="服务名" min-width="160" />
        <el-table-column prop="port" label="端口" width="90" />
        <el-table-column prop="package" label="模块路径" min-width="190" />
        <el-table-column prop="routes" label="路由规则" min-width="190" />
        <el-table-column label="实例" width="110">
          <template #default="{ row }">
            {{ row.healthyInstances ?? 0 }}/{{ row.instances ?? 0 }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'online' ? 'success' : row.status === 'offline' ? 'danger' : 'warning'" size="small">
              {{ row.status === 'online' ? '在线' : row.status === 'offline' ? '离线' : '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="响应" width="110">
          <template #default="{ row }">
            <span>{{ formatLatency(row.responseTime ?? row.latency) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card>
      <template #header>
        <span class="card-title">中间件状态</span>
      </template>
      <div class="mw-grid">
        <div v-for="mw in middlewareStatus" :key="mw.name" class="mw-card" :class="{ 'mw-ok': mw.ok, 'mw-err': !mw.ok }">
          <div class="mw-icon">{{ mw.icon }}</div>
          <div class="mw-info">
            <h4>{{ mw.name }}</h4>
            <p>{{ mw.version }}</p>
            <p class="mw-detail">{{ mw.detail }}</p>
          </div>
          <el-tag :type="mw.ok ? 'success' : 'danger'" size="small">{{ mw.ok ? '正常' : '异常' }}</el-tag>
        </div>
      </div>
    </el-card>

    <el-dialog
      v-model="terminalDialogVisible"
      title="Pod Terminal"
      width="860px"
      class="service-terminal-dialog"
    >
      <div class="terminal-target">
        {{ activeTerminalTarget.namespace }}/{{ activeTerminalTarget.podName }}/{{ activeTerminalTarget.containerName }}
      </div>
      <el-input
        v-model="terminalCommand"
        type="textarea"
        :rows="4"
        placeholder="pwd && ls -lah"
      />
      <div class="terminal-toolbar">
        <span>Timeout</span>
        <el-input-number v-model="terminalTimeoutSeconds" :min="3" :max="120" />
        <el-button type="primary" :loading="terminalLoading" @click="handleRunPodTerminal">Run Command</el-button>
      </div>
      <pre v-loading="terminalLoading" class="terminal-output">{{ terminalOutput || 'No output yet' }}</pre>
      <template #footer>
        <el-button @click="terminalDialogVisible = false">Close</el-button>
        <el-button type="primary" :loading="terminalLoading" @click="handleRunPodTerminal">Run Command</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getMonitoringOverview, runK8sPodExec } from '@/api/admin-ops'
import { getGatewayBaseUrl, getMinioBaseUrl } from '@/utils/runtimeConfig'

const serviceMeta = [
  { name: 'rk-gateway', port: 10010, package: 'com.tianji.gateway', desc: 'API网关', routes: '全局路由/CORS' },
  { name: 'rk-auth', port: 8081, package: 'com.tianji.auth', desc: '认证授权', routes: '/auth/**' },
  { name: 'rk-user', port: 8082, package: 'com.tianji.user', desc: '用户租户', routes: '/users/**,/tenants/**' },
  { name: 'rk-search', port: 8083, package: 'com.tianji.search', desc: '搜索服务', routes: '/search/**' },
  { name: 'rk-file', port: 8084, package: 'com.tianji.file', desc: '文件存储', routes: '/files/**' },
  { name: 'rk-message', port: 8085, package: 'com.tianji.message', desc: '消息通知', routes: '/notifications/**' },
  { name: 'rk-content', port: 8086, package: 'com.tianji.course', desc: '内容管理', routes: '/api/news/**,/api/works/**' },
  { name: 'rk-pay', port: 8087, package: 'com.tianji.pay', desc: '支付服务', routes: '/pay/**' },
  { name: 'rk-trade', port: 8088, package: 'com.tianji.trade', desc: '交易服务', routes: '/trade/**' },
  { name: 'rk-exam', port: 8089, package: 'com.tianji.exam', desc: '考试服务', routes: '/exam/**' },
  { name: 'rk-activity', port: 8090, package: 'com.tianji.activity', desc: '活动管理', routes: '/api/activity/**' },
  { name: 'rk-data', port: 8093, package: 'com.tianji.data', desc: '数据统计', routes: '/data/**' }
]

const middlewareMeta = [
  { name: 'MySQL', icon: 'DB', version: '8.0', color: '#e6a23c', desc: '业务数据库' },
  { name: 'Redis', icon: 'CA', version: '7.x', color: '#f56c6c', desc: '缓存/会话/JTI' },
  { name: 'MinIO', icon: 'OS', version: 'RELEASE.2024', color: '#67c23a', desc: '对象存储' },
  { name: 'RabbitMQ', icon: 'MQ', version: '3.x', color: '#909399', desc: '异步消息/通知投递' },
  { name: 'Nacos', icon: 'RG', version: '2.x', color: '#409eff', desc: '注册中心/配置中心' }
]

const loading = ref(false)
const overview = ref({
  systemStatus: {},
  services: [],
  databases: []
})
const allServices = ref(serviceMeta.map((service) => ({ ...service, status: 'unknown', instances: 0, healthyInstances: 0, instanceDetails: [] })))
const healthMap = ref({})
const terminalDialogVisible = ref(false)
const terminalLoading = ref(false)
const terminalCommand = ref('pwd && ls -lah')
const terminalTimeoutSeconds = ref(20)
const terminalOutput = ref('')
const activeTerminalTarget = reactive({
  namespace: '',
  podName: '',
  containerName: ''
})

const servicesRow1 = [
  { name: 'rk-auth', x: 20, port: 8081, desc: '认证授权' },
  { name: 'rk-user', x: 195, port: 8082, desc: '用户租户' },
  { name: 'rk-search', x: 370, port: 8083, desc: '搜索服务' },
  { name: 'rk-file', x: 545, port: 8084, desc: '文件存储' },
  { name: 'rk-message', x: 720, port: 8085, desc: '消息通知' }
]

const servicesRow2 = [
  { name: 'rk-content', x: 20, port: 8086, desc: '内容管理' },
  { name: 'rk-pay', x: 195, port: 8087, desc: '支付服务' },
  { name: 'rk-trade', x: 370, port: 8088, desc: '交易服务' },
  { name: 'rk-exam', x: 545, port: 8089, desc: '考试服务' },
  { name: 'rk-activity', x: 720, port: 8090, desc: '活动管理' }
]

const frontendEntryLabel = resolveFrontendEntryLabel()
const gatewayTopologyLabel = resolveGatewayTopologyLabel()

const summaryCards = computed(() => {
  const system = overview.value.systemStatus || {}
  return [
    { label: 'CPU', value: `${system.cpu ?? 0}%` },
    { label: '内存', value: `${system.memory ?? 0}%` },
    { label: '磁盘', value: `${system.disk ?? 0}%` },
    { label: '网络', value: system.network || '-' }
  ]
})

const middlewareTopology = computed(() =>
  middlewareMeta.slice(0, 4).map((item, index) => ({
    ...item,
    x: [20, 220, 420, 620][index],
    desc: item.name === 'MinIO' ? (getMinioBaseUrl() || item.desc) : item.desc
  }))
)

const middlewareStatus = computed(() => {
  const statusByName = new Map((overview.value.databases || []).map((item) => [String(item.name || '').toLowerCase(), item]))
  return middlewareMeta.map((item) => {
    const source = statusByName.get(item.name.toLowerCase())
    const ok = source ? source.status === 'online' : item.name === 'MinIO' && Boolean(getMinioBaseUrl())
    return {
      ...item,
      ok,
      detail: source?.detail || source?.status || (item.name === 'MinIO' ? getMinioBaseUrl() || '未配置' : '等待后端上报')
    }
  })
})

function resolveFrontendEntryLabel() {
  if (typeof window !== 'undefined' && window.location?.origin) {
    return window.location.origin
  }
  return 'browser origin'
}

function resolveGatewayTopologyLabel() {
  const gatewayUrl = getGatewayBaseUrl()
  if (!gatewayUrl) {
    return 'rk-gateway 运行时配置/路由/CORS/JWT解析/租户注入'
  }
  try {
    return `rk-gateway ${new URL(gatewayUrl).host} 路由/CORS/JWT解析/租户注入`
  } catch {
    return `rk-gateway ${gatewayUrl} 路由/CORS/JWT解析/租户注入`
  }
}

function normalizeServiceName(name) {
  const value = String(name || '')
  return value.startsWith('rk-server-') ? value.replace('rk-server-', '') : value
}

function enrichServices(rawServices) {
  const sourceByName = new Map((rawServices || []).map((item) => [normalizeServiceName(item.name), item]))
  const known = serviceMeta.map((meta) => ({
    ...meta,
    ...(sourceByName.get(meta.name) || {}),
    name: meta.name
  }))
  const extra = (rawServices || [])
    .map((item) => ({ ...item, name: normalizeServiceName(item.name) }))
    .filter((item) => !serviceMeta.some((meta) => meta.name === item.name))
  return [...known, ...extra].map((item) => ({
    ...item,
    status: item.status || 'unknown',
    instances: item.instances ?? 0,
    healthyInstances: item.healthyInstances ?? 0,
    instanceDetails: Array.isArray(item.instanceDetails) ? item.instanceDetails : []
  }))
}

function syncHealthMap(services) {
  healthMap.value = services.reduce((result, service) => {
    result[service.name] = service.status || 'unknown'
    return result
  }, {})
}

function getServiceColor(name) {
  const status = healthMap.value[name]
  return status === 'online' ? '#67c23a' : status === 'offline' ? '#f56c6c' : '#909399'
}

function getStatusColor(name) {
  return getServiceColor(name)
}

function formatLatency(value) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  return String(value).endsWith('ms') ? value : `${value}ms`
}

async function checkAllHealth() {
  loading.value = true
  try {
    const res = await getMonitoringOverview()
    if (res.code !== 200 || !res.data) {
      throw new Error(res.msg || '获取微服务监控数据失败')
    }
    overview.value = {
      systemStatus: res.data.systemStatus || {},
      services: Array.isArray(res.data.services) ? res.data.services : [],
      databases: Array.isArray(res.data.databases) ? res.data.databases : []
    }
    allServices.value = enrichServices(overview.value.services)
    syncHealthMap(allServices.value)
  } catch (error) {
    ElMessage.error(error.message || '获取微服务监控数据失败')
  } finally {
    loading.value = false
  }
}

function resolvePodTarget(instance) {
  return {
    namespace: instance?.namespace || '',
    podName: instance?.podName || '',
    containerName: instance?.containerName || ''
  }
}

function handleOpenPodTerminal(instance) {
  const target = resolvePodTarget(instance)
  if (!target.namespace || !target.podName || !target.containerName) {
    ElMessage.warning('Current instance is missing pod/container metadata')
    return
  }
  Object.assign(activeTerminalTarget, target)
  terminalOutput.value = ''
  terminalDialogVisible.value = true
}

async function handleRunPodTerminal() {
  if (!activeTerminalTarget.namespace || !activeTerminalTarget.podName || !activeTerminalTarget.containerName) {
    ElMessage.warning('Please select a pod/container first')
    return
  }
  if (!terminalCommand.value.trim()) {
    ElMessage.warning('Please enter command')
    return
  }
  terminalLoading.value = true
  try {
    const res = await runK8sPodExec({
      ...activeTerminalTarget,
      command: terminalCommand.value,
      timeoutSeconds: terminalTimeoutSeconds.value,
      reason: 'service monitor pod terminal'
    })
    if (res.code !== 200 || !res.data) {
      throw new Error(res.msg || 'Pod exec failed')
    }
    terminalOutput.value = res.data.output || res.data.message || ''
    if (res.data.success) {
      ElMessage.success(res.data.message || 'Pod command executed')
    } else {
      ElMessage.error(res.data.message || 'Pod command failed')
    }
  } catch (error) {
    terminalOutput.value = error.message || 'Pod exec failed'
    ElMessage.error(terminalOutput.value)
  } finally {
    terminalLoading.value = false
  }
}

onMounted(() => {
  checkAllHealth()
})
</script>

<style scoped>
.service-monitor {
  display: grid;
  gap: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.page-header h2 {
  margin: 0;
}

.page-header p {
  margin: 8px 0 0;
  color: #909399;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.summary-card {
  display: grid;
  gap: 8px;
}

.summary-card .label {
  color: #909399;
}

.summary-card strong {
  font-size: 24px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.topology-svg {
  width: 100%;
  overflow-x: auto;
}

.arch-diagram {
  width: 100%;
  min-width: 960px;
  height: auto;
}

.instance-panel {
  display: grid;
  gap: 12px;
  padding: 4px 0;
}

.instance-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  color: #606266;
}

.mw-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.mw-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  background: #fafafa;
}

.mw-card.mw-ok {
  border-color: #e1f3d8;
  background: #f0f9eb;
}

.mw-card.mw-err {
  border-color: #fde2e2;
  background: #fef0f0;
}

.mw-icon {
  font-size: 28px;
  font-weight: 700;
}

.mw-info {
  flex: 1;
}

.mw-info h4 {
  margin: 0;
  font-size: 14px;
}

.mw-info p {
  margin: 2px 0 0;
  font-size: 12px;
  color: #909399;
}

.mw-detail {
  font-size: 11px !important;
  color: #606266 !important;
}

.terminal-target {
  margin-bottom: 12px;
  color: #606266;
  font-size: 13px;
}

.terminal-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 12px 0;
}

.terminal-output {
  min-height: 360px;
  max-height: 58vh;
  margin: 0;
  padding: 14px;
  overflow: auto;
  border-radius: 12px;
  background: #101827;
  color: #d7e3f4;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 900px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .page-header {
    flex-direction: column;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
