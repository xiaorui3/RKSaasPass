<template>
  <div v-if="migrationOverlay.visible" class="global-migration-overlay">
    <div class="migration-dialog" role="alertdialog" aria-modal="true">
      <div class="migration-dialog-head">
        <div class="migration-title-block">
          <span class="migration-eyebrow"><i></i>全局服务迁移</span>
          <strong>全局迁移进行中</strong>
          <em>迁移期间其他租户也会看到此提示，所有用户暂时不能操作</em>
        </div>
        <div class="migration-progress-number">
          <b>{{ progressPercent }}%</b>
          <small>实时进度</small>
        </div>
      </div>

      <div class="migration-route">
        <div>
          <small>源集群</small>
          <b>当前 RK-Web 集群</b>
        </div>
        <i><span></span></i>
        <div>
          <small>目标集群</small>
          <b>{{ targetClusterText }}</b>
        </div>
      </div>

      <div class="migration-progress-strip">
        <el-progress :percentage="progressPercent" :stroke-width="12" :show-text="false" />
      </div>

      <div class="migration-phase-track">
        <div
          v-for="(phase, index) in migrationPhases"
          :key="phase.label"
          class="migration-phase"
          :class="phaseClass(index)"
        >
          <span>{{ index + 1 }}</span>
          <b>{{ phase.label }}</b>
          <small>{{ phase.hint }}</small>
        </div>
      </div>

      <div class="migration-stats">
        <div class="migration-stat-step">
          <small>当前步骤</small>
          <b>{{ currentStepText }}</b>
        </div>
        <div>
          <small>已用时间</small>
          <b>{{ formatElapsed(elapsedSeconds) }}</b>
        </div>
        <div>
          <small>预计剩余</small>
          <b>{{ remainingText }}</b>
        </div>
      </div>

      <div class="migration-terminal">
        <div class="migration-terminal-head">
          <span>终端日志</span>
          <small>最近日志：每 3 秒同步进度，时间本地持续计算</small>
        </div>
        <pre>{{ terminalLines.join('\n') }}</pre>
      </div>

      <p class="migration-note">
        迁移期间所有用户暂时不能操作。系统会逐个镜像导出、推送、部署并清理；关闭页面后会自动恢复当前迁移进度，迁移完成后自动恢复。
      </p>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { getActiveGlobalMigration } from '@/api/admin-ops'

const migrationOverlay = reactive({
  visible: false,
  record: null
})

const elapsedSeconds = ref(0)
const remainingSourceSeconds = ref(0)
const remainingSourceReceivedAt = ref(0)
const remainingSourceRecordId = ref('')
let migrationPollTimer = null
let elapsedTimer = null
let localStartedAtMs = 0

const progressPercent = computed(() => {
  const progress = Number(migrationOverlay.record?.progress || 0)
  return Math.max(0, Math.min(100, Math.round(progress)))
})

const migrationPhases = [
  { label: '锁定访问', hint: '同步全局迁移锁' },
  { label: '导出镜像', hint: '读取当前运行镜像' },
  { label: '迁移镜像', hint: '推送并配置拉取凭据' },
  { label: '部署清单', hint: '下发 K8s 编排资源' },
  { label: '导入数据', hint: '恢复 MySQL 与 MinIO' },
  { label: '恢复服务', hint: '校验微服务启动状态' }
]

const activePhaseIndex = computed(() => {
  const progress = progressPercent.value
  if (progress >= 95) return 5
  if (progress >= 72) return 4
  if (progress >= 50) return 3
  if (progress >= 32) return 2
  if (progress >= 12) return 1
  return 0
})

const targetClusterText = computed(() => {
  const record = migrationOverlay.record || {}
  return record.remoteClusterName || record.targetNamespace || 'kubeconfig 目标集群'
})

function phaseClass(index) {
  return {
    'is-done': index < activePhaseIndex.value,
    'is-active': index === activePhaseIndex.value
  }
}

const currentStepText = computed(() => resolveStepText(
  migrationOverlay.record?.currentStep ||
  migrationOverlay.record?.currentImage ||
  latestLogLine(migrationOverlay.record?.logs) ||
  ''
))

const terminalLines = computed(() => {
  const lines = parseLogLines(migrationOverlay.record?.logs)
    .slice(-10)
    .map((line) => `> ${resolveStepText(line, line)}`)

  if (lines.length) {
    return lines
  }
  return [`> ${currentStepText.value || '等待后端写入迁移进度'}`]
})

const remainingText = computed(() => {
  const localRemaining = remainingCountdownSeconds.value
  if (localRemaining > 0) {
    return formatElapsed(localRemaining)
  }
  if (progressPercent.value > 0 && progressPercent.value < 100 && elapsedSeconds.value > 0) {
    const estimatedTotal = Math.round(elapsedSeconds.value / (progressPercent.value / 100))
    const derived = Math.max(0, estimatedTotal - elapsedSeconds.value)
    return derived > 0 ? formatElapsed(derived) : '计算中'
  }
  return '计算中'
})

const remainingCountdownSeconds = computed(() => {
  // Keep the computed value reactive even when the backend estimate is stale.
  void elapsedSeconds.value
  if (remainingSourceSeconds.value <= 0 || remainingSourceReceivedAt.value <= 0) {
    return 0
  }
  const passed = Math.floor((Date.now() - remainingSourceReceivedAt.value) / 1000)
  return Math.max(0, remainingSourceSeconds.value - passed)
})

function parseLogLines(logs) {
  return String(logs || '')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
}

function latestLogLine(logs) {
  return parseLogLines(logs).at(-1)
}

function parseServerTime(value) {
  if (!value) return 0
  const epochMillis = parseEpochMillis(value)
  if (epochMillis > 0) return epochMillis
  const normalized = String(value).replace(' ', 'T')
  const timestamp = Date.parse(normalized)
  return Number.isFinite(timestamp) ? timestamp : 0
}

function parseEpochMillis(value) {
  if (value === null || value === undefined || value === '') return 0
  const numeric = Number(value)
  if (!Number.isFinite(numeric) || numeric <= 0) return 0
  return numeric > 100000000000 ? numeric : numeric * 1000
}

function resolveStartedAtMs(record) {
  const explicitStartedAt = parseEpochMillis(
    record?.migrationStartedAtMillis ||
    record?.deployStartedAtMillis ||
    record?.createTimeMillis ||
    record?.startTimeMillis
  )
  if (explicitStartedAt > 0) {
    return explicitStartedAt
  }
  const serverTime = parseServerTime(
    record?.migrationStartedAt ||
    record?.deployStartedAt ||
    record?.createTime ||
    record?.startTime
  )
  if (serverTime > 0) {
    return serverTime
  }
  if (!localStartedAtMs) {
    localStartedAtMs = Date.now()
  }
  return localStartedAtMs
}

function refreshElapsed() {
  if (!migrationOverlay.visible) {
    elapsedSeconds.value = 0
    return
  }
  const startedAt = resolveStartedAtMs(migrationOverlay.record)
  elapsedSeconds.value = Math.max(0, Math.floor((Date.now() - startedAt) / 1000))
}

function resetRemainingEstimate() {
  remainingSourceSeconds.value = 0
  remainingSourceReceivedAt.value = 0
  remainingSourceRecordId.value = ''
}

function syncRemainingEstimate(record) {
  const recordId = String(record?.id || '')
  const nextValue = Number(record?.estimatedRemainingSeconds || 0)
  if (!recordId || nextValue <= 0) {
    if (!recordId) resetRemainingEstimate()
    return
  }
  if (recordId !== remainingSourceRecordId.value || nextValue !== remainingSourceSeconds.value) {
    remainingSourceRecordId.value = recordId
    remainingSourceSeconds.value = nextValue
    remainingSourceReceivedAt.value = Date.now()
  }
}

function formatElapsed(seconds) {
  const value = Math.max(0, Number(seconds || 0))
  const hours = Math.floor(value / 3600)
  const minutes = Math.floor((value % 3600) / 60)
  const rest = value % 60
  if (hours > 0) {
    return `${hours}小时${minutes}分${rest}秒`
  }
  if (minutes > 0) {
    return `${minutes}分${rest}秒`
  }
  return `${rest}秒`
}

function resolveStepText(rawStep, fallback = '等待后端写入迁移进度') {
  const step = String(rawStep || '').trim()
  if (!step) return fallback
  if (/[\u4e00-\u9fa5]/.test(step)) return step

  const lower = step.toLowerCase()
  if (lower.includes('global migration lock')) {
    return '正在启用全局迁移锁'
  }
  if (lower.includes('preflight')) {
    return '正在执行部署前预检'
  }
  if (lower.includes('exporting current runtime images') || lower.includes('runtime image export')) {
    return '正在导出当前运行镜像'
  }
  if (lower.includes('image pull secret') || (lower.includes('pull') && lower.includes('secret'))) {
    return '正在配置目标集群镜像拉取凭据'
  }
  if (lower.includes('push') || lower.includes('upload') || lower.includes('image') || lower.includes('currentimage')) {
    return '正在迁移镜像'
  }
  if (lower.includes('payload') || lower.includes('staging')) {
    return '正在传输迁移数据包'
  }
  if (lower.includes('manifest') || lower.includes('apply -f') || lower.includes('applied')) {
    return '正在下发 Kubernetes 编排清单'
  }
  if (lower.includes('waiting statefulsets') || lower.includes('statefulset')) {
    return '正在等待 MySQL、Redis、MinIO 等有状态服务启动'
  }
  if (lower.includes('mysql') || lower.includes('database')) {
    return '正在导入 MySQL 数据'
  }
  if (lower.includes('minio')) {
    return '正在导入 MinIO 文件'
  }
  if (lower.includes('nacos')) {
    return '正在重启 Nacos 并加载配置'
  }
  if (lower.includes('restarting application') || lower.includes('workloads')) {
    return '正在重启业务微服务'
  }
  if (lower.includes('rollout')) {
    return '正在校验服务启动状态'
  }
  if (lower.includes('cleanup') || lower.includes('delete temporary') || lower.includes('cleaning')) {
    return '正在清理临时迁移资源'
  }
  if (lower.includes('completed') || lower.includes('success')) {
    return '迁移完成，正在恢复访问'
  }
  if (lower.includes('failed') || lower.includes('error')) {
    return '迁移失败，等待管理员处理'
  }
  return step
}

async function loadActiveMigration() {
  try {
    const res = await getActiveGlobalMigration()
    migrationOverlay.record = res.data || null
    migrationOverlay.visible = Boolean(res.data?.id)
    if (!migrationOverlay.visible) {
      localStartedAtMs = 0
      resetRemainingEstimate()
    } else {
      syncRemainingEstimate(res.data)
    }
    refreshElapsed()
  } catch {
    migrationOverlay.visible = false
    migrationOverlay.record = null
    localStartedAtMs = 0
    resetRemainingEstimate()
  }
}

onMounted(async () => {
  await loadActiveMigration()
  migrationPollTimer = window.setInterval(loadActiveMigration, 3000)
  elapsedTimer = window.setInterval(refreshElapsed, 1000)
})

onBeforeUnmount(() => {
  if (migrationPollTimer) window.clearInterval(migrationPollTimer)
  if (elapsedTimer) window.clearInterval(elapsedTimer)
})
</script>

<style scoped>
.global-migration-overlay {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: grid;
  place-items: center;
  padding: 20px;
  background:
    radial-gradient(circle at 18% 14%, rgba(56, 189, 248, 0.26), transparent 32%),
    radial-gradient(circle at 82% 22%, rgba(59, 130, 246, 0.18), transparent 34%),
    rgba(2, 6, 23, 0.82);
  backdrop-filter: blur(6px);
}

.migration-dialog {
  width: min(980px, 100%);
  max-height: calc(100vh - 40px);
  display: grid;
  gap: 16px;
  overflow: auto;
  padding: 24px;
  border: 1px solid rgba(125, 211, 252, 0.28);
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(8, 24, 52, 0.96), rgba(8, 47, 93, 0.94)),
    #07152d;
  box-shadow: 0 28px 90px rgba(0, 0, 0, 0.44);
  color: #dbeafe;
}

.migration-dialog-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
}

.migration-title-block {
  display: grid;
  gap: 7px;
  min-width: 0;
}

.migration-eyebrow {
  width: fit-content;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 4px 10px;
  border: 1px solid rgba(125, 211, 252, 0.42);
  border-radius: 999px;
  background: rgba(14, 165, 233, 0.12);
  color: #bae6fd;
  font-size: 12px;
  font-weight: 700;
}

.migration-eyebrow i {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: #22c55e;
  box-shadow: 0 0 0 6px rgba(34, 197, 94, 0.16);
  animation: migration-pulse 1.2s ease-in-out infinite;
}

.migration-dialog-head strong {
  font-size: 26px;
  line-height: 1.18;
  color: #f8fafc;
}

.migration-dialog-head em {
  color: #93c5fd;
  font-size: 13px;
  font-style: normal;
  line-height: 1.6;
}

.migration-progress-number {
  min-width: 112px;
  display: grid;
  place-items: center;
  gap: 4px;
  padding: 14px 16px;
  border: 1px solid rgba(125, 211, 252, 0.36);
  border-radius: 8px;
  background: rgba(2, 6, 23, 0.38);
  text-align: center;
}

.migration-progress-number b {
  color: #7dd3fc;
  font-size: 34px;
  line-height: 1;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.migration-progress-number small,
.migration-route small,
.migration-stats small,
.migration-phase small,
.migration-terminal-head small,
.migration-note {
  color: #93a4bd;
}

.migration-route {
  display: grid;
  grid-template-columns: 1fr 110px 1fr;
  gap: 12px;
  align-items: center;
}

.migration-route div,
.migration-stats div,
.migration-phase {
  min-width: 0;
  display: grid;
  gap: 6px;
  padding: 12px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.42);
}

.migration-route b,
.migration-stats b,
.migration-phase b {
  overflow-wrap: anywhere;
  color: #f8fafc;
  font-weight: 700;
}

.migration-route i {
  position: relative;
  height: 4px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(30, 64, 175, 0.5);
}

.migration-route i span {
  position: absolute;
  inset: 0 auto 0 0;
  width: 48%;
  border-radius: inherit;
  background: linear-gradient(90deg, #38bdf8, #22c55e);
  animation: migration-flow 1.4s linear infinite;
}

.migration-progress-strip :deep(.el-progress-bar__outer) {
  background-color: rgba(15, 23, 42, 0.72);
}

.migration-progress-strip :deep(.el-progress-bar__inner) {
  background: linear-gradient(90deg, #38bdf8, #22c55e);
}

.migration-phase-track {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 8px;
}

.migration-phase {
  position: relative;
  min-height: 92px;
}

.migration-phase span {
  width: 24px;
  height: 24px;
  display: grid;
  place-items: center;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.16);
  color: #93a4bd;
  font-size: 12px;
  font-weight: 800;
}

.migration-phase.is-done {
  border-color: rgba(34, 197, 94, 0.36);
  background: rgba(20, 83, 45, 0.24);
}

.migration-phase.is-done span {
  background: rgba(34, 197, 94, 0.2);
  color: #bbf7d0;
}

.migration-phase.is-active {
  border-color: rgba(56, 189, 248, 0.62);
  background: rgba(14, 165, 233, 0.16);
  box-shadow: 0 0 0 1px rgba(56, 189, 248, 0.12) inset;
}

.migration-phase.is-active span {
  background: #38bdf8;
  color: #082f49;
}

.migration-stats {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(130px, 0.7fr) minmax(130px, 0.7fr);
  gap: 12px;
}

.migration-stat-step b {
  line-height: 1.55;
}

.migration-terminal {
  overflow: hidden;
  border: 1px solid rgba(56, 189, 248, 0.24);
  border-radius: 8px;
  background: #020617;
}

.migration-terminal-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.22);
  color: #e5e7eb;
  font-size: 13px;
}

.migration-terminal pre {
  min-height: 138px;
  max-height: 230px;
  margin: 0;
  padding: 12px;
  overflow: auto;
  color: #bbf7d0;
  font-size: 12px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: Consolas, Monaco, 'Courier New', monospace;
}

.migration-note {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
}

@keyframes migration-pulse {
  0%,
  100% { opacity: 0.58; transform: scale(0.92); }
  50% { opacity: 1; transform: scale(1.08); }
}

@keyframes migration-flow {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(220%); }
}

@media (max-width: 760px) {
  .global-migration-overlay {
    padding: 12px;
    align-items: start;
    overflow: auto;
  }

  .migration-dialog {
    max-height: none;
    padding: 16px;
  }

  .migration-dialog-head {
    flex-direction: column;
  }

  .migration-dialog-head strong {
    font-size: 22px;
  }

  .migration-progress-number {
    width: 100%;
    min-width: 0;
    grid-template-columns: auto 1fr;
    justify-items: start;
    text-align: left;
  }

  .migration-progress-number b {
    font-size: 28px;
  }

  .migration-route,
  .migration-stats,
  .migration-phase-track {
    grid-template-columns: minmax(0, 1fr);
  }

  .migration-route i {
    width: 4px;
    height: 24px;
    justify-self: center;
  }

  .migration-route i span {
    width: 100%;
    height: 48%;
    animation-name: migration-flow-y;
  }

  .migration-phase {
    min-height: auto;
    grid-template-columns: 24px minmax(0, 1fr);
    align-items: center;
  }

  .migration-phase small {
    grid-column: 2;
  }

  .migration-terminal-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .migration-terminal pre {
    max-height: 150px;
    min-height: 108px;
  }
}

@keyframes migration-flow-y {
  0% { transform: translateY(-100%); }
  100% { transform: translateY(220%); }
}
</style>
