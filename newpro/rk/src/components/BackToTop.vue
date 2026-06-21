<template>
  <transition name="fade">
    <div v-if="showButton" class="back-to-top" @click="scrollToTop">
      <el-icon :size="20"><Top /></el-icon>
    </div>
  </transition>
  <div v-if="showProgress" class="scroll-progress" :style="{ width: progress + '%' }"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { Top } from '@element-plus/icons-vue'

const showButton = ref(false)
const showProgress = ref(false)
const progress = ref(0)

function handleScroll() {
  const scrollTop = window.scrollY
  const docHeight = document.documentElement.scrollHeight - window.innerHeight
  progress.value = docHeight > 0 ? Math.min((scrollTop / docHeight) * 100, 100) : 0
  showButton.value = scrollTop > 300
  showProgress.value = scrollTop > 0
}

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => window.addEventListener('scroll', handleScroll, { passive: true }))
onUnmounted(() => window.removeEventListener('scroll', handleScroll))
</script>

<style scoped>
.scroll-progress {
  position: fixed;
  top: 0;
  left: 0;
  height: 3px;
  background: linear-gradient(90deg, #409eff, #67c23a);
  z-index: 9999;
  transition: width 0.1s;
}
.back-to-top {
  position: fixed;
  bottom: 40px;
  right: 40px;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
  z-index: 1000;
  transition: transform 0.2s, opacity 0.2s;
}
.back-to-top:hover { transform: translateY(-3px); }
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
