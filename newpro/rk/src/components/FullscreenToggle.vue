<template>
  <el-button circle size="small" @click="toggle" :title="isFullscreen ? '退出全屏' : '全屏模式'">
    <el-icon><FullScreen v-if="!isFullscreen" /><Close v-else /></el-icon>
  </el-button>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { FullScreen, Close } from '@element-plus/icons-vue'

const isFullscreen = ref(false)

function toggle() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}

function onFsChange() {
  isFullscreen.value = !!document.fullscreenElement
}

onMounted(() => document.addEventListener('fullscreenchange', onFsChange))
onUnmounted(() => document.removeEventListener('fullscreenchange', onFsChange))
</script>
