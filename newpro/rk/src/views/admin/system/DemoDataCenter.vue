<template>
  <div class="demo-data-center">
    <section class="page-header">
      <div>
        <h1>演示数据中心</h1>
        <p>生成脱敏、可重复执行、可清理的开源演示数据。所有样例都会带上 DEMO_OPEN_SOURCE 标记，避免和真实生产数据混在一起。</p>
      </div>
      <div class="header-actions">
        <el-tag :type="statusTagType(demoData.status)" size="large">{{ demoData.status || 'NOT_GENERATED' }}</el-tag>
        <el-button :loading="loading" @click="loadDemoDataStatus">刷新</el-button>
        <el-button type="primary" :loading="operating === 'generate'" @click="handleGenerate">生成演示数据</el-button>
        <el-button type="warning" :loading="operating === 'reset'" @click="handleReset">重置演示数据</el-button>
        <el-button type="danger" plain :loading="operating === 'cleanup'" @click="handleCleanup">清理演示数据</el-button>
      </div>
    </section>

    <section class="status-grid">
      <div class="status-card">
        <span>安全标记</span>
        <strong>{{ demoData.safeTag || 'DEMO_OPEN_SOURCE' }}</strong>
        <small>清理动作只处理带标记的演示数据</small>
      </div>
      <div class="status-card">
        <span>演示租户</span>
        <strong>{{ demoData.demoTenantCode || 'open-source-demo' }}</strong>
        <small>生成后可用于开源体验</small>
      </div>
      <div class="status-card">
        <span>批次</span>
        <strong>{{ demoData.demoBatchId || '-' }}</strong>
        <small>demoBatchId 用于定位最近一次生成</small>
      </div>
      <div class="status-card">
        <span>执行策略</span>
        <strong>{{ demoData.repeatable ? '可重复执行' : '不可重复' }}</strong>
        <small>{{ demoData.cleanupSupported ? '支持一键清理' : '不支持清理' }}</small>
      </div>
    </section>

    <section class="module-board">
      <div class="board-title">
        <strong>演示模块</strong>
        <span>生成时间：{{ demoData.generatedAt || '-' }}</span>
      </div>
      <el-table v-loading="loading" :data="moduleStatuses" size="small" empty-text="暂无模块状态">
        <el-table-column prop="title" label="模块" min-width="140" />
        <el-table-column prop="module" label="编码" width="120" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ row.status || 'UNKNOWN' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="count" label="数量" width="90" />
        <el-table-column prop="message" label="说明" min-width="280" show-overflow-tooltip />
      </el-table>
    </section>

    <section class="sample-board">
      <div class="board-title">
        <strong>样例清单</strong>
        <span>脱敏邮箱域：open-source.local</span>
      </div>
      <el-table :data="sampleItems" size="small" empty-text="暂无样例清单">
        <el-table-column prop="module" label="模块" width="120" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="description" label="说明" min-width="260" show-overflow-tooltip />
        <el-table-column prop="status" label="标记" width="150" />
        <el-table-column prop="source" label="来源" min-width="180" show-overflow-tooltip />
      </el-table>
    </section>

    <section class="safety-panel">
      <div>
        <strong>安全边界</strong>
        <span>演示数据只使用脱敏姓名、脱敏邮箱、固定租户编码和 DEMO_OPEN_SOURCE 标记。清理时只处理标记命中的演示记录，不会批量删除真实业务数据。</span>
      </div>
      <div>
        <strong>覆盖内容</strong>
        <span>租户、演示用户、社团成员、新闻、活动、比赛。跨服务表不可用时会记录为跳过，页面仍可展示清单。</span>
      </div>
      <div>
        <strong>开源体验</strong>
        <span>适合新部署后快速验证前台新闻、活动、比赛、成员台账和搜索展示，不包含真实联系方式。</span>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  cleanupDemoData,
  generateDemoData,
  getDemoDataStatus,
  resetDemoData
} from '@/api/demo-data'

const loading = ref(false)
const operating = ref('')
const demoData = reactive(createEmptyDemoData())

const moduleStatuses = computed(() => Array.isArray(demoData.moduleStatuses) ? demoData.moduleStatuses : [])
const sampleItems = computed(() => Array.isArray(demoData.sampleItems) ? demoData.sampleItems : [])

function createEmptyDemoData() {
  return {
    status: 'NOT_GENERATED',
    safeTag: 'DEMO_OPEN_SOURCE',
    demoBatchId: '',
    demoTenantCode: 'open-source-demo',
    tenantId: null,
    repeatable: true,
    cleanupSupported: true,
    generatedAt: '',
    cleanedAt: '',
    updatedAt: '',
    modules: [],
    counts: {},
    sampleItems: [],
    moduleStatuses: [],
    warnings: []
  }
}

function unwrapResponse(res) {
  if (res?.code && res.code !== 200) {
    throw new Error(res.msg || '演示数据中心请求失败')
  }
  return res?.data || res || {}
}

function assignDemoData(data) {
  Object.assign(demoData, {
    ...createEmptyDemoData(),
    ...data,
    modules: Array.isArray(data.modules) ? data.modules : [],
    counts: data.counts || {},
    sampleItems: Array.isArray(data.sampleItems) ? data.sampleItems : [],
    moduleStatuses: Array.isArray(data.moduleStatuses) ? data.moduleStatuses : [],
    warnings: Array.isArray(data.warnings) ? data.warnings : []
  })
}

async function loadDemoDataStatus() {
  loading.value = true
  try {
    const res = await getDemoDataStatus()
    assignDemoData(unwrapResponse(res))
  } catch (error) {
    ElMessage.error(error.message || '演示数据状态加载失败')
  } finally {
    loading.value = false
  }
}

async function runOperation(type, action, message) {
  operating.value = type
  try {
    const res = await action()
    assignDemoData(unwrapResponse(res))
    ElMessage.success(message)
  } catch (error) {
    ElMessage.error(error.message || message)
  } finally {
    operating.value = ''
  }
}

function handleGenerate() {
  runOperation('generate', generateDemoData, '演示数据已生成')
}

async function handleCleanup() {
  await ElMessageBox.confirm('清理动作只会处理带 DEMO_OPEN_SOURCE 标记的演示数据，确认继续？', '清理演示数据', {
    type: 'warning',
    confirmButtonText: '确认清理',
    cancelButtonText: '取消'
  })
  runOperation('cleanup', cleanupDemoData, '演示数据已清理')
}

async function handleReset() {
  await ElMessageBox.confirm('重置会先清理旧演示数据，再生成一批新的演示清单，确认继续？', '重置演示数据', {
    type: 'warning',
    confirmButtonText: '确认重置',
    cancelButtonText: '取消'
  })
  runOperation('reset', resetDemoData, '演示数据已重置')
}

function statusTagType(status) {
  if (status === 'GENERATED' || status === 'PASS') return 'success'
  if (status === 'CLEANED' || status === 'SKIPPED') return 'warning'
  if (status === 'WARN') return 'warning'
  if (status === 'FAIL') return 'danger'
  return 'info'
}

onMounted(loadDemoDataStatus)
</script>

<style scoped>
.demo-data-center {
  min-height: 100%;
  padding: 24px;
  background: #f5f7fb;
  color: #172033;
}

.page-header,
.module-board,
.sample-board,
.safety-panel {
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
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin: 16px 0;
}

.status-card {
  min-width: 0;
  padding: 18px;
  background: #fff;
  border: 1px solid #e3e8f2;
  border-radius: 8px;
}

.status-card span,
.status-card small {
  display: block;
  color: #607086;
}

.status-card strong {
  display: block;
  margin: 8px 0;
  font-size: 20px;
  color: #172033;
  word-break: break-word;
}

.module-board,
.sample-board,
.safety-panel {
  margin-top: 16px;
  padding: 20px;
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

.safety-panel {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.safety-panel strong {
  display: block;
  margin-bottom: 8px;
  color: #172033;
}

.safety-panel span {
  color: #607086;
  line-height: 1.7;
}

@media (max-width: 1100px) {
  .status-grid,
  .safety-panel {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .demo-data-center {
    padding: 14px;
  }

  .page-header {
    flex-direction: column;
  }

  .header-actions {
    justify-content: flex-start;
  }

  .status-grid,
  .safety-panel {
    grid-template-columns: 1fr;
  }
}
</style>
