<template>
  <div class="app-container">
    <div id="app" :class="{ 'app-blur': showLoginModal && !isAuthPage }">
      <router-view />
    </div>

    <LoginModal
      v-model="showLoginModal"
      @login-success="handleLoginSuccess"
    />
    <GlobalMigrationOverlay />
    <KeyboardShortcuts />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, provide, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import LoginModal from '@/components/LoginModal.vue'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { setShowLoginCallback } from '@/utils/request'
import { shouldUseCurrentTheme } from '@/utils/themeAccess'
import { applyBrowserTabTitle, resolveBrowserTabTitle } from '@/utils/browserTitle'
import KeyboardShortcuts from '@/components/KeyboardShortcuts.vue'
import GlobalMigrationOverlay from '@/components/GlobalMigrationOverlay.vue'

const route = useRoute()
const userStore = useUserStore()
const themeStore = useThemeStore()

const showLoginModal = ref(false)

const isAuthPage = computed(() => route.path === '/login' || route.path === '/register')
const requiresAuth = computed(() => route.meta?.requiresAuth === true)
const browserTabTitle = computed(() => resolveBrowserTabTitle({
  isLoggedIn: userStore.isLoggedIn,
  tenantId: userStore.tenantId,
  tenantDirectory: userStore.tenantDirectory,
  routeTitle: route.meta?.title || route.name
}))

const cleanupLoginOverlays = () => {
  const overlays = document.querySelectorAll('.el-overlay, .el-overlay-dialog')
  overlays.forEach(overlay => {
    const dialog = overlay.querySelector('.el-dialog')
    if (!dialog || dialog.classList.contains('login-modal')) {
      overlay.remove()
    }
  })
}

const syncLoginModalState = () => {
  if (isAuthPage.value || !requiresAuth.value || !userStore.requireLogin()) {
    showLoginModal.value = false
    cleanupLoginOverlays()
    return
  }
  showLoginModal.value = true
}

const handleLoginSuccess = () => {
  showLoginModal.value = false
  cleanupLoginOverlays()

  setTimeout(() => {
    cleanupLoginOverlays()

    const token = localStorage.getItem('token')
    const tenantId = localStorage.getItem('tenantId')

    if (token && tenantId) {
      window.location.reload()
      return
    }
    ElMessage.error('登录状态保存失败，请重新登录')
    showLoginModal.value = true
  }, 500)
}

const showLogin = () => {
  if (!isAuthPage.value) {
    showLoginModal.value = true
  }
}

const hideLogin = () => {
  showLoginModal.value = false
  cleanupLoginOverlays()
  setTimeout(cleanupLoginOverlays, 150)
}

provide('showLoginModal', showLoginModal)
provide('showLogin', showLogin)
provide('hideLogin', hideLogin)

watch(
  () => route.fullPath,
  () => {
    syncLoginModalState()
  }
)

watch(
  () => userStore.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) {
      userStore.fetchSwitchableTenants()
      if (!userStore.tenantDirectory?.length) {
        userStore.fetchTenantDirectory()
      }
    }
  },
  { immediate: true }
)

watch(
  browserTabTitle,
  (value) => {
    applyBrowserTabTitle(value)
  },
  { immediate: true }
)

watch(
  () => [route.path, userStore.tenantId, userStore.isLoggedIn],
  async ([path, tenantId, loggedIn]) => {
    if (!loggedIn || !tenantId) {
      if (path !== '/login' && path !== '/register' && path !== '/join') {
        themeStore.clearTheme()
      }
      return
    }
    const context = path.startsWith('/admin') ? 'admin' : 'frontend'
    const useCurrentTheme = shouldUseCurrentTheme(context, userStore.roleId)
    await themeStore.applyTenantTheme(tenantId, context, useCurrentTheme)
  },
  { immediate: true }
)

onMounted(() => {
  setShowLoginCallback(showLogin)
  syncLoginModalState()
  if (!userStore.isLoggedIn) {
    themeStore.clearTheme()
  }
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  width: 100%;
  min-height: 100%;
  overflow-x: hidden;
}

.app-container {
  min-height: 100vh;
  position: relative;
  max-width: 100%;
  overflow-x: hidden;
}

#app {
  min-height: 100vh;
  font-family: 'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
  max-width: 100%;
  overflow-x: hidden;
}

.app-blur {
  filter: blur(8px);
  pointer-events: none;
  user-select: none;
}

/* Dark mode */
html.dark {
  --el-bg-color: #1a1a2e;
  --el-bg-color-overlay: #16213e;
  --el-text-color-primary: #e0e0e0;
  --el-text-color-regular: #c0c0c0;
  --el-fill-color-blank: #1a1a2e;
}
html.dark body, html.dark .app-container, html.dark #app {
  background: #1a1a2e;
  color: #e0e0e0;
}
html.dark .el-card {
  background: #16213e;
  border-color: #2a2a4a;
  color: #e0e0e0;
}
html.dark .el-table {
  background: #16213e;
  --el-table-bg-color: #16213e;
  --el-table-tr-bg-color: #16213e;
  --el-table-header-bg-color: #1a1a3e;
  --el-table-row-hover-bg-color: #1e2a4a;
  color: #e0e0e0;
}
html.dark .el-dialog {
  background: #16213e;
  color: #e0e0e0;
}
html.dark .el-form-item__label {
  color: #c0c0c0;
}
html.dark input, html.dark textarea, html.dark .el-input__wrapper {
  background: #0f3460 !important;
  color: #e0e0e0 !important;
  border-color: #2a2a4a !important;
}

/* Admin sidebar submenu dark background */
.sidebar-menu .el-sub-menu .el-menu {
  background: transparent !important;
}
.sidebar-menu .el-sub-menu .el-menu .el-menu-item {
  background: transparent !important;
  color: rgba(255, 255, 255, 0.72) !important;
}
.sidebar-menu .el-sub-menu .el-menu .el-menu-item:hover {
  color: #fff !important;
  background: rgba(255, 255, 255, 0.1) !important;
}

/* Mobile responsive */
@media (max-width: 768px) {
  .container { padding: 0 12px !important; }
  .page-hero h1 { font-size: 22px !important; }
  .page-hero { padding: 24px 0 18px !important; }
}
@media (max-width: 480px) {
  .el-dialog { width: 92% !important; margin: 8vh auto !important; }
  .el-card { margin-bottom: 12px; }
}
</style>
