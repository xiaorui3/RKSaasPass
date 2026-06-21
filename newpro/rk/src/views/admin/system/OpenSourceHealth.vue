<template>
  <div class="open-source-health">
    <section class="page-header">
      <div>
        <h1>系统自检</h1>
        <p>面向开源部署的业务应用层检查，只读确认菜单 seed、默认角色、ES 索引、Redis 缓存、系统配置、演示数据和文件存储。</p>
      </div>
      <div class="header-actions">
        <el-tag :type="statusTagType(openSourceHealth.overallStatus)" size="large">
          {{ openSourceHealth.overallStatus || 'UNKNOWN' }}
        </el-tag>
        <span>检查时间：{{ openSourceHealth.checkedAt || '-' }}</span>
        <el-button type="primary" :loading="loading" @click="refreshHealth">刷新</el-button>
      </div>
    </section>

    <section class="summary-grid">
      <div v-for="item in summaryCards" :key="item.label" class="summary-card" :class="item.tone">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.hint }}</small>
      </div>
    </section>

    <section class="health-board">
      <div class="board-title">
        <strong>开源部署自检项</strong>
        <span>当前租户：{{ openSourceHealth.tenantScope || 'ALL' }}</span>
      </div>
      <el-table v-loading="loading" :data="healthChecks" size="small" empty-text="暂无自检结果">
        <el-table-column prop="category" label="类别" width="120" />
        <el-table-column prop="title" label="检查项" min-width="140" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ row.status || 'UNKNOWN' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="diagnostics" label="诊断结果" min-width="280" show-overflow-tooltip />
        <el-table-column prop="suggestion" label="诊断建议" min-width="280" show-overflow-tooltip />
        <el-table-column prop="source" label="来源" min-width="180" show-overflow-tooltip />
      </el-table>
    </section>

    <section class="check-guide">
      <div v-for="item in guideItems" :key="item.title" class="guide-item">
        <strong>{{ item.title }}</strong>
        <span>{{ item.text }}</span>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getOpenSourceHealth } from '@/api/data'

const loading = ref(false)
const openSourceHealth = reactive({
  tenantId: null,
  tenantScope: 'ALL',
  overallStatus: 'UNKNOWN',
  checkedAt: '',
  summary: {},
  checks: []
})

const guideItems = [
  { title: '菜单 seed', text: '确认后台真实展示入口已经由菜单管理落库，不依赖前端写死。' },
  { title: '默认角色', text: '确认 ADMIN、USER、CLUB_MANAGER、TEACHER 四类基础角色完整。' },
  { title: 'ES 索引', text: '确认全局搜索的业务索引有可检索文档，搜索链路继续走 rk-search。' },
  { title: 'Redis 缓存', text: '确认前台可展示内容缓存有预热摘要，Redis 异常时给出降级建议。' },
  { title: '系统配置', text: '确认当前租户有启用配置，避免开源部署后门户显示缺少基础配置。' },
  { title: '演示数据', text: '确认租户和公开内容存在，便于开源体验者直接验证页面。' },
  { title: '文件存储', text: '确认文件台账存在记录，便于排查上传、Logo、新闻封面等链路。' }
]

const healthChecks = computed(() => Array.isArray(openSourceHealth.checks) ? openSourceHealth.checks : [])

const summaryCards = computed(() => {
  const summary = openSourceHealth.summary || {}
  return [
    { label: '总检查项', value: numberValue(summary.totalCount), hint: '业务层只读检查', tone: 'neutral' },
    { label: 'PASS', value: numberValue(summary.passCount), hint: '可用项', tone: 'pass' },
    { label: 'WARN', value: numberValue(summary.warnCount), hint: '建议处理', tone: 'warn' },
    { label: 'FAIL', value: numberValue(summary.failCount), hint: '必须修复', tone: 'fail' },
    { label: 'UNKNOWN', value: numberValue(summary.unknownCount), hint: '需确认来源', tone: 'unknown' }
  ]
})

const unwrapResponse = (res) => {
  if (res?.code && res.code !== 200) {
    throw new Error(res.msg || '系统自检加载失败')
  }
  return res?.data || res || {}
}

const assignHealth = (data) => {
  Object.assign(openSourceHealth, {
    tenantId: data.tenantId ?? null,
    tenantScope: data.tenantScope || 'ALL',
    overallStatus: data.overallStatus || 'UNKNOWN',
    checkedAt: data.checkedAt || '',
    summary: data.summary || {},
    checks: Array.isArray(data.checks) ? data.checks : []
  })
}

async function refreshHealth() {
  loading.value = true
  try {
    const res = await getOpenSourceHealth()
    assignHealth(unwrapResponse(res))
  } catch (error) {
    ElMessage.error(error.message || '系统自检加载失败')
  } finally {
    loading.value = false
  }
}

function numberValue(value) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

function statusTagType(status) {
  if (status === 'PASS') return 'success'
  if (status === 'WARN') return 'warning'
  if (status === 'FAIL') return 'danger'
  return 'info'
}

onMounted(refreshHealth)
</script>

<style scoped>
.open-source-health {
  min-height: 100%;
  padding: 24px;
  background: #f5f7fb;
  color: #172033;
}

.page-header,
.health-board,
.check-guide {
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
  max-width: 760px;
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
  color: #607086;
  font-size: 13px;
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

.summary-card.pass strong {
  color: #1b8f5a;
}

.summary-card.warn strong {
  color: #b46b00;
}

.summary-card.fail strong {
  color: #c0362c;
}

.summary-card.unknown strong {
  color: #5f6b7a;
}

.health-board {
  padding: 18px;
}

.board-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  color: #607086;
}

.board-title strong {
  color: #172033;
  font-size: 16px;
}

.check-guide {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
  padding: 18px;
}

.guide-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-height: 78px;
  padding: 14px;
  border: 1px solid #e7edf6;
  border-radius: 8px;
  background: #fafcff;
}

.guide-item span {
  color: #607086;
  line-height: 1.6;
}

@media (max-width: 980px) {
  .page-header {
    flex-direction: column;
  }

  .summary-grid,
  .check-guide {
    grid-template-columns: 1fr;
  }
}
</style>
