<template>
  <div class="admin-page traffic-center-page">
    <section class="traffic-toolbar">
      <div>
        <h1>全球流量中心</h1>
        <p>按租户、国家、省份和 IP 聚合后台登录与访问行为。</p>
      </div>
      <div class="toolbar-controls">
        <el-select
          v-model="selectedTenantId"
          class="tenant-select"
          filterable
          placeholder="选择租户"
          @change="fetchOverview"
        >
          <el-option
            v-for="item in tenantOptions"
            :key="item.tenantId"
            :label="item.tenantName"
            :value="item.tenantId"
          />
        </el-select>
        <el-date-picker
          v-model="timeRange"
          type="datetimerange"
          value-format="YYYY-MM-DD HH:mm:ss"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          range-separator="至"
          class="time-range"
          @change="fetchOverview"
        />
        <el-switch
          v-model="defaultSwitch"
          active-text="默认展示当前租户"
          inactive-text="仅查看"
          :disabled="!selectedTenantId || savingDefault"
          @change="handleDefaultSwitchChange"
        />
        <el-button :icon="Refresh" :loading="loading" @click="fetchOverview">刷新</el-button>
      </div>
    </section>

    <section class="metric-strip">
      <div v-for="item in metrics" :key="item.label" class="metric-tile">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </section>

    <section class="traffic-workspace">
      <div class="globe-panel">
        <div class="panel-heading">
          <div>
            <h2>实时访问地球</h2>
            <span>当前租户：{{ selectedTenantName }}</span>
          </div>
          <el-tag size="small" :type="webglReady ? 'success' : 'warning'">
            {{ webglReady ? 'WebGL 已启用' : '地球兜底视图' }}
          </el-tag>
        </div>

        <div class="traffic-stage">
          <div class="globe-wrap" :class="{ 'webgl-ready': webglReady && !webglError }">
            <div ref="globeContainer" class="globe-container" />
            <div class="fallback-earth" aria-hidden="true">
              <div class="fallback-earth-map" />
              <span class="fallback-earth-ring ring-one" />
              <span class="fallback-earth-ring ring-two" />
            </div>
          </div>
          <div class="stage-caption">
            <strong>{{ selectedCountry || '全部国家' }}</strong>
            <span>{{ currentOriginLabel }} -> {{ selectedProvince || '点击国家查看省份，再点省份查看 IP' }}</span>
          </div>
        </div>
      </div>

      <aside class="traffic-side">
        <section class="side-section">
          <div class="side-heading">
            <h3>国家排行</h3>
            <span>{{ countryRankings.length }} 个国家/地区</span>
          </div>
          <div v-loading="loading" class="ranking-list">
            <button
              v-for="item in countryRankings"
              :key="`country-${item.country}`"
              class="ranking-row"
              :class="{ active: selectedCountry === item.country }"
              type="button"
              @click="handleCountryClick(item)"
            >
              <span class="ranking-name">{{ item.country }}</span>
              <span class="ranking-count">{{ formatNumber(item.visitCount) }}</span>
            </button>
            <el-empty v-if="!loading && countryRankings.length === 0" description="暂无国家访问数据" />
          </div>
        </section>

        <section class="side-section">
          <div class="side-heading">
            <h3>省份钻取</h3>
            <span>{{ selectedCountry || '未选择国家' }}</span>
          </div>
          <div v-loading="drilldownLoading" class="ranking-list province-list">
            <button
              v-for="item in provinceRankings"
              :key="`province-${item.country}-${item.province}`"
              class="ranking-row"
              :class="{ active: selectedProvince === item.province }"
              type="button"
              @click="handleProvinceClick(item)"
            >
              <span class="ranking-name">{{ item.province }}</span>
              <span class="ranking-count">{{ formatNumber(item.visitCount) }}</span>
            </button>
            <el-empty v-if="!drilldownLoading && provinceRankings.length === 0" description="暂无省份数据" />
          </div>
        </section>
      </aside>
    </section>

    <section class="online-section">
      <div class="panel-heading">
        <div>
          <h2>谁在线</h2>
          <span>最近 10 分钟活跃用户，区分电脑端和移动端</span>
        </div>
        <el-button size="small" :icon="Refresh" :loading="onlineLoading" @click="() => fetchOnlineUsers()">刷新在线</el-button>
      </div>
      <div class="online-summary">
        <el-tag type="success">在线 {{ formatNumber(overview.onlineUserCount) }}</el-tag>
        <el-tag type="primary">电脑端 {{ formatNumber(overview.onlinePcCount) }}</el-tag>
        <el-tag type="success" effect="plain">移动端 {{ formatNumber(overview.onlineMobileCount) }}</el-tag>
      </div>
      <el-table
        v-loading="loading || onlineLoading"
        :data="onlineUsers"
        size="small"
        stripe
        :row-key="onlineRowKey"
        empty-text="暂无在线用户"
      >
        <el-table-column prop="userName" label="用户" min-width="120" />
        <el-table-column label="终端" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="deviceTagType(row.deviceType)">
              {{ deviceTypeLabel(row.deviceType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="clientType" label="客户端" min-width="140" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" min-width="130" />
        <el-table-column prop="country" label="国家" min-width="100" />
        <el-table-column prop="province" label="省份" min-width="110" />
        <el-table-column label="活跃" width="90" align="right">
          <template #default="{ row }">{{ formatNumber(row.activityCount) }}</template>
        </el-table-column>
        <el-table-column prop="lastSeenAt" label="最近时间" min-width="160" />
        <el-table-column prop="samplePath" label="最近入口" min-width="180" show-overflow-tooltip />
      </el-table>
    </section>

    <section class="ip-section">
      <div class="panel-heading">
        <div>
          <h2>IP 明细</h2>
          <span>{{ ipTableTitle }}</span>
        </div>
        <el-tag size="small">默认租户：{{ defaultTenantName }}</el-tag>
      </div>
      <el-table
        v-loading="loading || drilldownLoading"
        :data="visibleIpRows"
        size="small"
        stripe
        row-key="ip"
        empty-text="暂无 IP 明细"
      >
        <el-table-column prop="ip" label="IP" min-width="130" />
        <el-table-column prop="country" label="国家" min-width="100" />
        <el-table-column prop="province" label="省份" min-width="110" />
        <el-table-column prop="userName" label="用户" min-width="120" />
        <el-table-column label="访问" width="90" align="right">
          <template #default="{ row }">{{ formatNumber(row.visitCount) }}</template>
        </el-table-column>
        <el-table-column label="登录" width="90" align="right">
          <template #default="{ row }">{{ formatNumber(row.loginCount) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="right">
          <template #default="{ row }">{{ formatNumber(row.operationCount) }}</template>
        </el-table-column>
        <el-table-column prop="lastSeenAt" label="最近时间" min-width="160" />
        <el-table-column prop="samplePath" label="访问路径" min-width="180" show-overflow-tooltip />
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import Globe from 'globe.gl'
import {
  getTrafficCurrentOrigin,
  getTrafficDefaultTenant,
  getTrafficDrilldown,
  getTrafficOnlineUsers,
  getTrafficOverview,
  saveTrafficDefaultTenant
} from '@/api/admin-ops'

const EARTH_TEXTURE = '/assets/traffic-globe/earth-blue-marble.jpg'
const NIGHT_SKY_TEXTURE = '/assets/traffic-globe/night-sky.png'

const globeContainer = ref(null)
const loading = ref(false)
const drilldownLoading = ref(false)
const onlineLoading = ref(false)
const savingDefault = ref(false)
const webglReady = ref(false)
const webglError = ref(false)
const selectedTenantId = ref(null)
const defaultTenantId = ref(null)
const defaultSwitch = ref(false)
const selectedCountry = ref('')
const selectedProvince = ref('')
const timeRange = ref([])

let globeInstance = null
let resizeObserver = null

const overview = reactive({
  tenantOptions: [],
  countryRankings: [],
  provinceRankings: [],
  recentIps: [],
  onlineUsers: [],
  totalVisits: 0,
  totalLogins: 0,
  totalOperations: 0,
  countryCount: 0,
  abnormalIpCount: 0,
  onlineUserCount: 0,
  onlinePcCount: 0,
  onlineMobileCount: 0,
  currentOrigin: null
})

const tenantOptions = computed(() => overview.tenantOptions)
const countryRankings = computed(() => overview.countryRankings)
const provinceRankings = computed(() => overview.provinceRankings)
const recentIps = computed(() => overview.recentIps)
const onlineUsers = computed(() => overview.onlineUsers)
const currentOrigin = computed(() => overview.currentOrigin)

const selectedTenantName = computed(() => {
  const found = tenantOptions.value.find((item) => item.tenantId === selectedTenantId.value)
  return found?.tenantName || `tenant ${selectedTenantId.value || '-'}`
})

const defaultTenantName = computed(() => {
  const found = tenantOptions.value.find((item) => item.tenantId === defaultTenantId.value)
  return found?.tenantName || `tenant ${defaultTenantId.value || '-'}`
})

const currentOriginLabel = computed(() => {
  const origin = currentOrigin.value
  if (!origin) {
    return '当前来源定位中'
  }
  const location = [origin.country, origin.province, origin.city].filter(Boolean).join(' / ')
  return location ? `当前来源 ${location}` : '当前来源未知'
})

const metrics = computed(() => [
  { label: '总访问', value: formatNumber(overview.totalVisits) },
  { label: '登录次数', value: formatNumber(overview.totalLogins) },
  { label: '操作次数', value: formatNumber(overview.totalOperations) },
  { label: '在线用户', value: formatNumber(overview.onlineUserCount) },
  { label: '电脑端在线', value: formatNumber(overview.onlinePcCount) },
  { label: '移动端在线', value: formatNumber(overview.onlineMobileCount) },
  { label: '国家/地区', value: formatNumber(overview.countryCount) },
  { label: '内网/未知 IP', value: formatNumber(overview.abnormalIpCount) }
])

const visibleIpRows = computed(() => {
  let rows = recentIps.value
  if (selectedCountry.value) {
    rows = rows.filter((row) => row.country === selectedCountry.value)
  }
  if (selectedProvince.value) {
    rows = rows.filter((row) => row.province === selectedProvince.value)
  }
  return rows.slice(0, 80)
})

const ipTableTitle = computed(() => {
  if (selectedCountry.value && selectedProvince.value) {
    return `${selectedCountry.value} / ${selectedProvince.value}`
  }
  if (selectedCountry.value) {
    return selectedCountry.value
  }
  return '全部 IP'
})

function normalizeNumber(value) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

function formatNumber(value) {
  return new Intl.NumberFormat('zh-CN').format(normalizeNumber(value))
}

function normalizeTenant(item) {
  return {
    tenantId: Number(item?.tenantId ?? item?.id ?? 0),
    tenantName: item?.tenantName || item?.name || `tenant ${item?.tenantId ?? item?.id ?? '-'}`,
    status: Number(item?.status ?? 1)
  }
}

function normalizeLocation(item) {
  const visitCount = normalizeNumber(item?.visitCount)
  return {
    tenantId: Number(item?.tenantId ?? selectedTenantId.value ?? 1),
    tenantName: item?.tenantName || '',
    country: item?.country || '未知国家',
    province: item?.province || '未知省份',
    city: item?.city || '',
    ip: item?.ip || '',
    userName: item?.userName || item?.userDisplayName || '',
    visitCount,
    loginCount: normalizeNumber(item?.loginCount),
    operationCount: normalizeNumber(item?.operationCount),
    lastSeenAt: item?.lastSeenAt || '',
    samplePath: item?.samplePath || '',
    sampleAction: item?.sampleAction || '',
    rawLocation: item?.rawLocation || '',
    lat: normalizeCoordinate(item?.lat),
    lng: normalizeCoordinate(item?.lng)
  }
}

function normalizeOnlineUser(item) {
  return {
    tenantId: Number(item?.tenantId ?? selectedTenantId.value ?? 1),
    tenantName: item?.tenantName || '',
    userName: item?.userName || item?.userDisplayName || '',
    userDisplayName: item?.userDisplayName || item?.userName || '',
    deviceType: normalizeDeviceType(item?.deviceType),
    clientType: item?.clientType || 'Unknown',
    ip: item?.ip || '',
    country: item?.country || '未知国家',
    province: item?.province || '未知省份',
    city: item?.city || '',
    lastSeenAt: item?.lastSeenAt || '',
    activityCount: normalizeNumber(item?.activityCount),
    samplePath: item?.samplePath || '',
    sampleAction: item?.sampleAction || '',
    rawLocation: item?.rawLocation || '',
    lat: normalizeCoordinate(item?.lat),
    lng: normalizeCoordinate(item?.lng)
  }
}

function normalizeDeviceType(value) {
  const lower = String(value || '').toLowerCase()
  if (lower.includes('mobile') || lower.includes('android') || lower.includes('ios') || lower.includes('iphone')) {
    return 'mobile'
  }
  if (lower.includes('pc') || lower.includes('web') || lower.includes('desktop') || lower.includes('windows') || lower.includes('mac')) {
    return 'pc'
  }
  return 'unknown'
}

function applyOnlineUsers(rows) {
  overview.onlineUsers = Array.isArray(rows) ? rows.map(normalizeOnlineUser) : []
  overview.onlineUserCount = overview.onlineUsers.length
  overview.onlinePcCount = overview.onlineUsers.filter((item) => item.deviceType === 'pc').length
  overview.onlineMobileCount = overview.onlineUsers.filter((item) => item.deviceType === 'mobile').length
}

function deviceTypeLabel(value) {
  if (value === 'mobile') return '移动端'
  if (value === 'pc') return '电脑端'
  return '未知'
}

function deviceTagType(value) {
  if (value === 'mobile') return 'success'
  if (value === 'pc') return 'primary'
  return 'info'
}

function onlineRowKey(row) {
  return `${row?.tenantId || '-'}-${row?.userName || '-'}-${row?.deviceType || '-'}-${row?.ip || '-'}`
}

function normalizeCoordinate(value) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

function hasValidCoordinate(item) {
  return Number.isFinite(Number(item?.lat)) && Number.isFinite(Number(item?.lng))
}

function applyTrafficData(data) {
  const payload = data || {}
  const tenants = Array.isArray(payload.tenantOptions) ? payload.tenantOptions.map(normalizeTenant) : []
  overview.tenantOptions = tenants
  defaultTenantId.value = Number(payload.defaultTenantId || defaultTenantId.value || tenants[0]?.tenantId || 1)
  selectedTenantId.value = Number(payload.selectedTenantId || selectedTenantId.value || defaultTenantId.value)
  defaultSwitch.value = selectedTenantId.value === defaultTenantId.value
  overview.countryRankings = Array.isArray(payload.countryRankings) ? payload.countryRankings.map(normalizeLocation) : []
  overview.provinceRankings = Array.isArray(payload.provinceRankings) ? payload.provinceRankings.map(normalizeLocation) : []
  overview.recentIps = Array.isArray(payload.recentIps) ? payload.recentIps.map(normalizeLocation) : []
  if (payload.currentOrigin) {
    overview.currentOrigin = normalizeLocation(payload.currentOrigin)
  }
  applyOnlineUsers(payload.onlineUsers)
  overview.totalVisits = normalizeNumber(payload.totalVisits)
  overview.totalLogins = normalizeNumber(payload.totalLogins)
  overview.totalOperations = normalizeNumber(payload.totalOperations)
  overview.countryCount = normalizeNumber(payload.countryCount)
  overview.abnormalIpCount = normalizeNumber(payload.abnormalIpCount)
  overview.onlineUserCount = normalizeNumber(payload.onlineUserCount) || overview.onlineUsers.length
  overview.onlinePcCount = normalizeNumber(payload.onlinePcCount) || overview.onlineUsers.filter((item) => item.deviceType === 'pc').length
  overview.onlineMobileCount = normalizeNumber(payload.onlineMobileCount) || overview.onlineUsers.filter((item) => item.deviceType === 'mobile').length

  if (!overview.countryRankings.some((item) => item.country === selectedCountry.value)) {
    selectedCountry.value = overview.countryRankings[0]?.country || ''
    selectedProvince.value = ''
  }
  refreshGlobeData()
}

function buildRequestParams() {
  const params = {}
  if (selectedTenantId.value) {
    params.tenantId = selectedTenantId.value
  }
  if (Array.isArray(timeRange.value) && timeRange.value.length === 2) {
    params.startTime = timeRange.value[0]
    params.endTime = timeRange.value[1]
  }
  return params
}

async function fetchDefaultTenant() {
  try {
    const res = await getTrafficDefaultTenant()
    if (res.code === 200 && res.data) {
      const tenants = Array.isArray(res.data.tenantOptions) ? res.data.tenantOptions.map(normalizeTenant) : []
      overview.tenantOptions = tenants
      defaultTenantId.value = Number(res.data.defaultTenantId || tenants[0]?.tenantId || 1)
      selectedTenantId.value = Number(res.data.selectedTenantId || defaultTenantId.value)
      defaultSwitch.value = selectedTenantId.value === defaultTenantId.value
    }
  } catch (error) {
    console.warn('load traffic default tenant failed', error)
  }
}

async function fetchOverview() {
  loading.value = true
  try {
    const res = await getTrafficOverview(buildRequestParams())
    if (res.code !== 200 || !res.data) {
      throw new Error(res.msg || '获取全球流量数据失败')
    }
    selectedProvince.value = ''
    applyTrafficData(res.data)
    await fetchOnlineUsers(false)
  } catch (error) {
    ElMessage.error(error.message || '获取全球流量数据失败')
  } finally {
    loading.value = false
  }
}

async function fetchOnlineUsers(showMessage = true) {
  onlineLoading.value = true
  try {
    const res = await getTrafficOnlineUsers({
      tenantId: selectedTenantId.value,
      windowMinutes: 10
    })
    if (res.code !== 200) {
      throw new Error(res.msg || '获取在线用户失败')
    }
    applyOnlineUsers(res.data || [])
    if (showMessage) {
      ElMessage.success('在线用户已刷新')
    }
  } catch (error) {
    if (showMessage) {
      ElMessage.error(error.message || '获取在线用户失败')
    }
  } finally {
    onlineLoading.value = false
  }
}

async function fetchCurrentOrigin() {
  try {
    const res = await getTrafficCurrentOrigin()
    if (res.code === 200 && res.data) {
      overview.currentOrigin = normalizeLocation(res.data)
      refreshGlobeData()
    }
  } catch (error) {
    console.warn('load traffic current origin failed', error)
  }
}

async function fetchDrilldown() {
  if (!selectedCountry.value) {
    return
  }
  drilldownLoading.value = true
  try {
    const res = await getTrafficDrilldown({
      ...buildRequestParams(),
      country: selectedCountry.value,
      province: selectedProvince.value || undefined
    })
    if (res.code !== 200 || !res.data) {
      throw new Error(res.msg || '获取钻取数据失败')
    }
    applyTrafficData(res.data)
  } catch (error) {
    ElMessage.error(error.message || '获取钻取数据失败')
  } finally {
    drilldownLoading.value = false
  }
}

async function handleCountryClick(item) {
  selectedCountry.value = item?.country || ''
  selectedProvince.value = ''
  focusGlobePoint(item)
  await fetchDrilldown()
}

async function handleProvinceClick(item) {
  selectedProvince.value = item?.province || ''
  focusGlobePoint(item)
  await fetchDrilldown()
}

async function handleDefaultSwitchChange(value) {
  if (!value) {
    defaultSwitch.value = selectedTenantId.value === defaultTenantId.value
    return
  }
  if (!selectedTenantId.value) {
    ElMessage.warning('请先选择租户')
    defaultSwitch.value = false
    return
  }
  savingDefault.value = true
  try {
    const res = await saveTrafficDefaultTenant({ tenantId: selectedTenantId.value })
    if (res.code !== 200 || !res.data) {
      throw new Error(res.msg || '保存默认租户失败')
    }
    applyTrafficData({
      ...res.data,
      countryRankings: overview.countryRankings,
      provinceRankings: overview.provinceRankings,
      recentIps: overview.recentIps,
      totalVisits: overview.totalVisits,
      totalLogins: overview.totalLogins,
      totalOperations: overview.totalOperations,
      countryCount: overview.countryCount,
      abnormalIpCount: overview.abnormalIpCount
    })
    ElMessage.success('默认展示租户已更新')
  } catch (error) {
    defaultSwitch.value = selectedTenantId.value === defaultTenantId.value
    ElMessage.error(error.message || '保存默认租户失败')
  } finally {
    savingDefault.value = false
  }
}

function buildGlobeTargets() {
  const rows = selectedCountry.value && provinceRankings.value.length
    ? provinceRankings.value
    : (recentIps.value.length ? recentIps.value : countryRankings.value)
  const buckets = new Map()
  rows
    .filter((item) => hasValidCoordinate(item))
    .forEach((item) => {
      const key = `${item.country}|${item.province}|${item.city}|${item.lat}|${item.lng}`
      const current = buckets.get(key)
      if (!current) {
        buckets.set(key, { ...item })
        return
      }
      current.visitCount += item.visitCount
      current.loginCount += item.loginCount
      current.operationCount += item.operationCount
    })
  return Array.from(buckets.values()).slice(0, 80)
}

function getArcOrigin() {
  const origin = currentOrigin.value
  if (hasValidCoordinate(origin)) {
    return origin
  }
  return { country: '中国', province: '北京', city: '北京', lat: 39.9042, lng: 116.4074 }
}

function isSameCoordinate(a, b) {
  if (!hasValidCoordinate(a) || !hasValidCoordinate(b)) {
    return false
  }
  return Math.abs(Number(a?.lat) - Number(b?.lat)) < 0.1 && Math.abs(Number(a?.lng) - Number(b?.lng)) < 0.1
}

function buildTrafficArcs() {
  const origin = getArcOrigin()
  return buildGlobeTargets()
    .filter((target) => !isSameCoordinate(origin, target))
    .map((target) => ({
      startLat: origin.lat,
      startLng: origin.lng,
      endLat: target.lat,
      endLng: target.lng,
      target,
      origin,
      altitude: Math.min(0.62, 0.2 + Math.log10(Math.max(target.visitCount, 1) + 1) / 8),
      stroke: Math.min(0.9, 0.28 + Math.sqrt(Math.max(target.visitCount, 1)) / 34),
      color: [
        selectedCountry.value && target.country === selectedCountry.value ? 'rgba(245, 158, 11, 0.96)' : 'rgba(25, 195, 125, 0.86)',
        'rgba(59, 130, 246, 0.88)'
      ]
    }))
}

function buildTrafficRings() {
  const origin = getArcOrigin()
  return [
    { ...origin, ringColor: 'rgba(59, 130, 246, 0.95)', ringMaxRadius: 3.8 },
    ...buildGlobeTargets().map((target) => ({
      ...target,
      ringColor: target.country === selectedCountry.value ? 'rgba(245, 158, 11, 0.95)' : 'rgba(25, 195, 125, 0.78)',
      ringMaxRadius: Math.min(5.8, 1.8 + Math.sqrt(Math.max(target.visitCount, 1)) / 6)
    }))
  ]
}

function initGlobe() {
  if (!globeContainer.value || globeInstance) {
    return
  }
  try {
    globeInstance = Globe({ rendererConfig: { antialias: true, alpha: true } })(globeContainer.value)
      .globeImageUrl(EARTH_TEXTURE)
      .backgroundImageUrl(NIGHT_SKY_TEXTURE)
      .showAtmosphere(true)
      .atmosphereColor('#8bd3ff')
      .atmosphereAltitude(0.18)
      .arcStartLat('startLat')
      .arcStartLng('startLng')
      .arcEndLat('endLat')
      .arcEndLng('endLng')
      .arcAltitude('altitude')
      .arcStroke('stroke')
      .arcColor('color')
      .arcDashLength(0.34)
      .arcDashGap(0.18)
      .arcDashInitialGap(() => Math.random())
      .arcDashAnimateTime(2200)
      .arcLabel(arcLabel)
      .onArcClick((arc) => handleCountryClick(arc.target))
      .ringLat('lat')
      .ringLng('lng')
      .ringColor('ringColor')
      .ringMaxRadius('ringMaxRadius')
      .ringPropagationSpeed(1.8)
      .ringRepeatPeriod(1800)

    const controls = globeInstance.controls()
    controls.autoRotate = true
    controls.autoRotateSpeed = 0.45
    controls.enableDamping = true
    controls.minDistance = 180
    controls.maxDistance = 520

    resizeGlobe()
    globeInstance.pointOfView({ lat: 29, lng: 105, altitude: 1.9 }, 900)
    refreshGlobeData()
    resizeObserver = new ResizeObserver(resizeGlobe)
    resizeObserver.observe(globeContainer.value)
    webglReady.value = true
    webglError.value = false
  } catch (error) {
    console.warn('traffic globe init failed', error)
    webglReady.value = false
    webglError.value = true
  }
}

function resizeGlobe() {
  if (!globeInstance || !globeContainer.value) {
    return
  }
  const width = Math.max(globeContainer.value.clientWidth, 320)
  const height = Math.max(globeContainer.value.clientHeight, 420)
  globeInstance.width(width).height(height)
}

function refreshGlobeData() {
  if (!globeInstance) {
    return
  }
  globeInstance
    .arcsData(buildTrafficArcs())
    .ringsData(buildTrafficRings())
}

function focusGlobePoint(item) {
  if (!globeInstance || !hasValidCoordinate(item)) {
    refreshGlobeData()
    return
  }
  refreshGlobeData()
  globeInstance.pointOfView({ lat: item.lat, lng: item.lng, altitude: 1.65 }, 800)
}

function arcLabel(item) {
  const target = item?.target || item
  const origin = item?.origin || currentOrigin.value || {}
  return `
    <div class="traffic-point-label">
      <strong>${escapeHtml(target.country || '未知国家')} / ${escapeHtml(target.province || '未知省份')}</strong>
      <span>${escapeHtml(origin.country || '当前来源')} -> ${formatNumber(target.visitCount)} 次访问</span>
    </div>
  `
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}

watch(selectedCountry, refreshGlobeData)
watch(selectedTenantId, () => {
  defaultSwitch.value = selectedTenantId.value === defaultTenantId.value
})

onMounted(async () => {
  await nextTick()
  initGlobe()
  await fetchDefaultTenant()
  await fetchCurrentOrigin()
  await fetchOverview()
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  resizeObserver = null
  if (globeContainer.value) {
    globeContainer.value.innerHTML = ''
  }
  globeInstance = null
})
</script>

<style scoped>
.traffic-center-page {
  display: grid;
  gap: 16px;
  color: #263238;
}

.traffic-toolbar,
.metric-strip,
.globe-panel,
.traffic-side,
.online-section,
.ip-section {
  border: 1px solid #d9e2ec;
  border-radius: 8px;
  background: #ffffff;
}

.traffic-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
}

.traffic-toolbar h1,
.panel-heading h2,
.side-heading h3 {
  margin: 0;
  letter-spacing: 0;
}

.traffic-toolbar h1 {
  font-size: 24px;
  line-height: 1.25;
}

.traffic-toolbar p,
.panel-heading span,
.side-heading span,
.stage-caption span {
  color: #697386;
}

.traffic-toolbar p {
  margin: 6px 0 0;
}

.toolbar-controls {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.tenant-select {
  width: 220px;
}

.time-range {
  width: 360px;
}

.metric-strip {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 0;
  overflow: hidden;
}

.metric-tile {
  display: grid;
  gap: 4px;
  min-height: 76px;
  padding: 14px 16px;
  border-right: 1px solid #e6ebf1;
}

.metric-tile:last-child {
  border-right: 0;
}

.metric-tile span {
  color: #697386;
  font-size: 13px;
}

.metric-tile strong {
  color: #16202a;
  font-size: 24px;
  line-height: 1.2;
  letter-spacing: 0;
}

.traffic-workspace {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 16px;
}

.globe-panel,
.traffic-side,
.online-section,
.ip-section {
  padding: 14px;
}

.panel-heading,
.side-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.panel-heading h2 {
  font-size: 18px;
}

.side-heading h3 {
  font-size: 16px;
}

.traffic-stage {
  position: relative;
  min-height: 560px;
  overflow: hidden;
  border-radius: 8px;
  background: #0c1524;
}

.globe-wrap,
.globe-container,
.fallback-earth {
  position: absolute;
  inset: 0;
}

.globe-container {
  z-index: 2;
}

.fallback-earth {
  z-index: 1;
  display: grid;
  place-items: center;
  background-image: url('/assets/traffic-globe/night-sky.png');
  background-size: cover;
  opacity: 1;
  transition: opacity 180ms ease;
}

.webgl-ready .fallback-earth {
  opacity: 0;
}

.fallback-earth-map {
  width: min(62vmin, 440px);
  aspect-ratio: 1;
  border-radius: 50%;
  background:
    radial-gradient(circle at 38% 35%, rgba(255, 255, 255, 0.32), transparent 0 18%, rgba(255, 255, 255, 0) 28%),
    url('/assets/traffic-globe/earth-blue-marble.jpg');
  background-size: cover;
  box-shadow:
    inset -42px -20px 80px rgba(0, 0, 0, 0.52),
    0 0 46px rgba(94, 234, 212, 0.28);
}

.fallback-earth-ring {
  position: absolute;
  width: min(68vmin, 500px);
  aspect-ratio: 1;
  border: 1px solid rgba(125, 211, 252, 0.35);
  border-radius: 50%;
}

.ring-one {
  transform: rotateX(62deg) rotateZ(-20deg);
}

.ring-two {
  transform: rotateX(70deg) rotateZ(28deg);
}

.stage-caption {
  position: absolute;
  left: 16px;
  bottom: 16px;
  z-index: 3;
  display: flex;
  align-items: center;
  gap: 10px;
  max-width: calc(100% - 32px);
  padding: 10px 12px;
  border: 1px solid rgba(148, 163, 184, 0.38);
  border-radius: 8px;
  background: rgba(12, 21, 36, 0.78);
  color: #ffffff;
  backdrop-filter: blur(8px);
}

.stage-caption strong {
  white-space: nowrap;
}

.traffic-side {
  display: grid;
  gap: 14px;
  align-content: start;
}

.side-section {
  min-height: 250px;
}

.ranking-list {
  display: grid;
  gap: 8px;
  align-content: start;
  grid-auto-rows: min-content;
  min-height: 190px;
}

.ranking-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
  width: 100%;
  min-height: 42px;
  padding: 9px 10px;
  border: 1px solid #d9e2ec;
  border-radius: 8px;
  background: #f8fafc;
  color: #263238;
  cursor: pointer;
  text-align: left;
}

.ranking-row:hover,
.ranking-row.active {
  border-color: #19c37d;
  background: #effcf6;
}

.ranking-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ranking-count {
  font-variant-numeric: tabular-nums;
  color: #0f766e;
}

.province-list {
  max-height: 310px;
  overflow: auto;
  padding-right: 2px;
}

.ip-section {
  overflow: hidden;
}

.online-section {
  overflow: hidden;
}

.online-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

:deep(.traffic-point-label) {
  display: grid;
  gap: 3px;
  min-width: 120px;
  color: #ffffff;
}

:deep(.traffic-point-label span) {
  color: #bfdbfe;
}

@media (max-width: 1180px) {
  .traffic-toolbar,
  .traffic-workspace {
    grid-template-columns: 1fr;
  }

  .traffic-toolbar {
    display: grid;
  }

  .traffic-workspace {
    display: grid;
  }

  .traffic-side {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .toolbar-controls,
  .traffic-side,
  .panel-heading,
  .side-heading,
  .stage-caption {
    display: grid;
    grid-template-columns: 1fr;
  }

  .tenant-select,
  .time-range {
    width: 100%;
  }

  .metric-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .metric-tile {
    border-right: 0;
    border-bottom: 1px solid #e6ebf1;
  }

  .traffic-stage {
    min-height: 470px;
  }
}
</style>
