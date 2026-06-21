<template>
  <div v-if="visible" class="announcement-bar">
    <div class="announcement-content">
      <el-icon><Notification /></el-icon>
      <span class="announcement-text">{{ message }}</span>
    </div>
    <el-icon class="announcement-close" @click="dismiss"><Close /></el-icon>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Notification, Close } from '@element-plus/icons-vue'

const visible = ref(false)
const message = ref('')

onMounted(() => {
  const saved = localStorage.getItem('announcement-dismissed')
  if (saved === 'true') return
  // Check for active announcements from API
  message.value = '欢迎来到 RK-Web 社团管理平台！如有问题请联系管理员。'
  visible.value = true
})

function dismiss() {
  visible.value = false
  localStorage.setItem('announcement-dismissed', 'true')
}
</script>

<style scoped>
.announcement-bar {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: linear-gradient(90deg, #e6f7ff, #bae7ff, #e6f7ff);
  color: #1890ff;
  font-size: 14px;
  position: relative;
  z-index: 100;
}
.announcement-content {
  display: flex;
  align-items: center;
  gap: 8px;
}
.announcement-close {
  position: absolute;
  right: 16px;
  cursor: pointer;
  opacity: 0.6;
}
.announcement-close:hover { opacity: 1; }
</style>
