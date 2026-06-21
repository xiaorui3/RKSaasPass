<template>
  <div class="admin-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <div>
            <h2>日志管理</h2>
            <p>查看后台操作记录和登录记录，帮助定位运行问题与审计行为。</p>
          </div>
          <el-button :loading="loading" @click="fetchCurrentTab">刷新</el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="fetchCurrentTab">
        <el-tab-pane label="操作日志" name="operlog">
          <el-table :data="operLogs" v-loading="loading" stripe empty-text="暂无操作日志">
            <el-table-column prop="operId" label="ID" width="90" />
            <el-table-column prop="title" label="模块" min-width="140" />
            <el-table-column prop="operName" label="操作人" min-width="120" />
            <el-table-column prop="requestMethod" label="请求方式" width="100" />
            <el-table-column prop="operUrl" label="请求地址" min-width="220" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 0 ? 'success' : 'danger'">
                  {{ row.status === 0 ? '成功' : '异常' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="operTime" label="时间" min-width="180" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="登录日志" name="logininfor">
          <el-table :data="loginLogs" v-loading="loading" stripe empty-text="暂无登录日志">
            <el-table-column prop="infoId" label="ID" width="90" />
            <el-table-column prop="loginName" label="登录账号" min-width="140" />
            <el-table-column prop="ipaddr" label="IP" min-width="140" />
            <el-table-column prop="browser" label="浏览器" min-width="120" />
            <el-table-column prop="os" label="系统" min-width="120" />
            <el-table-column prop="status" label="状态" width="90" />
            <el-table-column prop="msg" label="消息" min-width="180" show-overflow-tooltip />
            <el-table-column prop="loginTime" label="时间" min-width="180" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getLoginLogPage, getOperLogPage } from '@/api/admin-logs'

const activeTab = ref('operlog')
const loading = ref(false)
const operLogs = ref([])
const loginLogs = ref([])

async function fetchOperLogs() {
  const res = await getOperLogPage({ page: 1, size: 20 })
  operLogs.value = res.code === 200 ? (res.data?.records || []) : []
}

async function fetchLoginLogs() {
  const res = await getLoginLogPage({ page: 1, size: 20 })
  loginLogs.value = res.code === 200 ? (res.data?.records || []) : []
}

async function fetchCurrentTab() {
  loading.value = true
  try {
    if (activeTab.value === 'operlog') {
      await fetchOperLogs()
    } else {
      await fetchLoginLogs()
    }
  } catch (error) {
    ElMessage.error(error.message || '获取日志失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchCurrentTab()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.card-header h2 {
  margin: 0;
  font-size: 20px;
}

.card-header p {
  margin: 8px 0 0;
  color: #909399;
}
</style>
