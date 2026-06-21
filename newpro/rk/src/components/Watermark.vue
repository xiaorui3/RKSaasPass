<template>
  <div v-if="show" class="watermark-overlay" :style="watermarkStyle"></div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const show = ref(false)

const watermarkStyle = computed(() => {
  const text = userStore.userName || 'User'
  const svgStr = '<svg xmlns="http://www.w3.org/2000/svg" width="200" height="150">'
    + '<text x="20" y="80" fill="rgba(0,0,0,0.06)" font-size="16" transform="rotate(-30,100,75)">'
    + text
    + '</text></svg>'
  return {
    backgroundImage: 'url("data:image/svg+xml,' + encodeURIComponent(svgStr) + '")',
    backgroundRepeat: 'repeat',
    pointerEvents: 'none'
  }
})

onMounted(() => {
  show.value = userStore.isAdmin
})
</script>

<style scoped>
.watermark-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 9998;
  pointer-events: none;
}
</style>
