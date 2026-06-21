<template>
  <div class="admin-page topology-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <div>
            <h1>微服务拓扑</h1>
            <p>按命名空间汇总网关、微服务、中间件和注册关系，辅助排查发布链路。</p>
          </div>
          <div class="section-actions">
            <el-input v-model.trim="namespace" placeholder="命名空间" style="width: 220px" />
            <el-button type="primary" :loading="loading" @click="fetchTopology">刷新拓扑</el-button>
          </div>
        </div>
      </template>

      <div class="topology-board">
        <div class="node-list">
          <div
            v-for="node in topology.nodes"
            :key="node.id"
            class="topology-node"
            :class="[`node-${node.type}`, `status-${node.status}`]"
          >
            <strong>{{ node.label }}</strong>
            <span>{{ node.id }}</span>
            <small>{{ node.status }} · {{ node.healthyInstances ?? 0 }}/{{ node.instances ?? 0 }}</small>
            <div v-if="node.links" class="node-actions">
              <el-button v-if="node.links.logs" size="small" text @click="openNodeLink(node.links.logs)">日志</el-button>
              <el-button v-if="node.links.configCenter" size="small" text @click="openNodeLink(node.links.configCenter)">配置</el-button>
              <el-button v-if="node.links.buildHistory" size="small" text @click="openNodeLink(node.links.buildHistory)">构建</el-button>
            </div>
          </div>
        </div>
        <div class="edge-list">
          <h3>调用/注册关系</h3>
          <el-table :data="topology.edges" size="small" stripe empty-text="暂无拓扑边">
            <el-table-column prop="source" label="来源" min-width="140" />
            <el-table-column prop="target" label="目标" min-width="140" />
            <el-table-column prop="type" label="关系" width="120" />
          </el-table>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getOpsTopology } from '@/api/admin-ops'

const loading = ref(false)
const router = useRouter()
const namespace = ref('shetuanguanlixitong')
const topology = reactive({
  namespace: '',
  nodes: [],
  edges: []
})

async function fetchTopology() {
  loading.value = true
  try {
    const res = await getOpsTopology({ namespace: namespace.value })
    if (res.code !== 200 || !res.data) {
      throw new Error(res.msg || '获取微服务拓扑失败')
    }
    topology.namespace = res.data.namespace || namespace.value
    topology.nodes = Array.isArray(res.data.nodes) ? res.data.nodes : []
    topology.edges = Array.isArray(res.data.edges) ? res.data.edges : []
  } catch (error) {
    ElMessage.error(error.message || '获取微服务拓扑失败')
  } finally {
    loading.value = false
  }
}

function openNodeLink(path) {
  router.push(path)
}

onMounted(fetchTopology)
</script>

<style scoped>
.topology-page {
  display: grid;
  gap: 16px;
}

.card-header,
.section-actions {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.section-actions {
  align-items: center;
}

.card-header h1 {
  margin: 0;
  font-size: 24px;
}

.card-header p {
  margin: 8px 0 0;
  color: #909399;
}

.topology-board {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(360px, 0.9fr);
  gap: 18px;
}

.node-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}

.topology-node {
  display: grid;
  gap: 6px;
  padding: 14px;
  border-radius: 14px;
  border: 1px solid #dcdfe6;
  background: linear-gradient(135deg, #f8fafc, #eef6ff);
}

.topology-node strong {
  color: #303133;
}

.topology-node span,
.topology-node small {
  color: #606266;
}

.node-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.status-online,
.status-ready {
  border-color: #67c23a;
}

.status-offline {
  border-color: #f56c6c;
}

.node-middleware {
  background: linear-gradient(135deg, #fff7ed, #fff);
}

.edge-list h3 {
  margin: 0 0 12px;
}

@media (max-width: 900px) {
  .card-header,
  .section-actions,
  .topology-board {
    grid-template-columns: 1fr;
    flex-direction: column;
  }
}
</style>
