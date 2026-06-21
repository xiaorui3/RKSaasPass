<template>
  <div class="resource-booking-page">
    <el-alert type="warning" :closable="false" show-icon style="margin-bottom:20px;">
      <template #title>功能开发中 — 当前数据仅作展示，不会持久保存</template>
    </el-alert>
    <div class="page-header">
      <h2>社团资源预约</h2>
      <el-button type="primary" @click="showCreateDialog = true">添加资源</el-button>
    </div>
    <el-row :gutter="16">
      <el-col :span="8" v-for="res in resources" :key="res.id">
        <el-card shadow="hover" class="resource-card">
          <div class="resource-icon">{{ res.icon }}</div>
          <h3>{{ res.name }}</h3>
          <p class="resource-desc">{{ res.description }}</p>
          <div class="resource-status">
            <el-tag :type="res.available ? 'success' : 'danger'" size="small">
              {{ res.available ? '可预约' : '已占用' }}
            </el-tag>
            <span class="resource-capacity">容量: {{ res.capacity }}</span>
          </div>
          <el-button
            type="primary"
            size="small"
            style="margin-top:12px;width:100%;"
            :disabled="!res.available"
            @click="openBooking(res)"
          >预约</el-button>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="resources.length === 0" description="暂无资源" />

    <el-dialog v-model="showBookingDialog" title="预约资源" width="450px">
      <el-form :model="booking" label-width="80px">
        <el-form-item label="资源名称">
          <el-input :model-value="booking.resourceName" disabled />
        </el-form-item>
        <el-form-item label="预约日期">
          <el-date-picker v-model="booking.date" type="date" placeholder="选择日期" style="width:100%;" />
        </el-form-item>
        <el-form-item label="时间段">
          <el-time-picker v-model="booking.startTime" placeholder="开始时间" style="width:48%;" />
          <span style="margin:0 4px;">至</span>
          <el-time-picker v-model="booking.endTime" placeholder="结束时间" style="width:48%;" />
        </el-form-item>
        <el-form-item label="用途说明">
          <el-input v-model="booking.purpose" type="textarea" :rows="2" placeholder="说明用途..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showBookingDialog = false">取消</el-button>
        <el-button type="primary" @click="submitBooking">确认预约</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showCreateDialog" title="添加资源" width="450px">
      <el-form :model="newResource" label-width="80px">
        <el-form-item label="资源名称">
          <el-input v-model="newResource.name" placeholder="如：会议室A" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="newResource.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="容量">
          <el-input-number v-model="newResource.capacity" :min="1" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="newResource.icon" placeholder="标识文本" style="width:80px;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="addResource">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const resources = ref([
  { id: 1, name: '多功能会议室', description: '配备投影仪和白板', capacity: 30, icon: '会议', available: true },
  { id: 2, name: '实验室 A', description: '编程实验室，含40台电脑', capacity: 40, icon: '机房', available: true },
  { id: 3, name: '活动大厅', description: '大型活动场地', capacity: 200, icon: '场馆', available: false },
])

const showBookingDialog = ref(false)
const showCreateDialog = ref(false)
const booking = ref({ resourceName: '', date: '', startTime: '', endTime: '', purpose: '' })
const newResource = ref({ name: '', description: '', capacity: 10, icon: '通用' })

function openBooking(res) {
  booking.value = { resourceId: res.id, resourceName: res.name, date: '', startTime: '', endTime: '', purpose: '' }
  showBookingDialog.value = true
}

function submitBooking() {
  if (!booking.value.date) return ElMessage.warning('请选择日期')
  showBookingDialog.value = false
  ElMessage.success('预约成功')
}

function addResource() {
  if (!newResource.value.name) return ElMessage.warning('请输入名称')
  resources.value.push({ id: Date.now(), ...newResource.value, available: true })
  newResource.value = { name: '', description: '', capacity: 10, icon: '通用' }
  showCreateDialog.value = false
  ElMessage.success('资源添加成功')
}
</script>

<style scoped>
.resource-booking-page { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; }
.resource-card { text-align: center; margin-bottom: 16px; }
.resource-icon { font-size: 40px; margin-bottom: 8px; }
.resource-card h3 { margin: 8px 0; }
.resource-desc { color: #909399; font-size: 13px; margin: 8px 0; }
.resource-status { display: flex; justify-content: center; align-items: center; gap: 8px; }
.resource-capacity { font-size: 12px; color: #909399; }
</style>
