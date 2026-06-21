<template>
  <div :class="['admin-layout', adminShellRoleClass]">
    <Watermark />

    <transition name="mobile-mask-fade">
      <div
        v-if="isMobile && mobileSidebarOpen"
        class="mobile-sidebar-mask"
        @click="mobileSidebarOpen = false"
      />
    </transition>

    <aside
      class="sidebar"
      :class="{
        collapsed: !isMobile && isCollapsed,
        mobile: isMobile,
        open: isMobile && mobileSidebarOpen
      }"
    >
      <div class="sidebar-header">
        <div class="admin-sidebar-brand">
          <img
            v-if="currentTenantBranding.logoUrl"
            :src="currentTenantBranding.logoUrl"
            :alt="currentTenantBranding.tenantName || t('layout.tenantLogoAlt')"
            class="logo-image"
          >
          <span v-else class="logo-text">{{ currentTenantBranding.initials }}</span>
          <span v-if="!isCollapsed || isMobile" class="logo-title">{{ adminBrandTitle }}</span>
          <small v-if="!isCollapsed || isMobile">{{ adminBrandSubTitle }}</small>
        </div>
        <el-button
          v-if="isMobile"
          class="sidebar-close"
          text
          :icon="Close"
          @click="mobileSidebarOpen = false"
        />
      </div>

      <el-menu
        :default-active="activeMenu"
        class="sidebar-menu"
        :collapse="!isMobile && isCollapsed"
        :collapse-transition="false"
        router
      >
        <template v-for="menu in filteredMenus" :key="menu.path">
          <el-sub-menu v-if="menu.children && menu.children.length > 0" :index="menu.path">
            <template #title>
              <el-icon><component :is="menu.icon" /></el-icon>
              <span>{{ menu.name }}</span>
            </template>
            <el-menu-item v-for="child in menu.children" :key="child.path" :index="child.path">
              <el-icon><component :is="child.icon" /></el-icon>
              <span>{{ child.name }}</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="menu.path">
            <el-icon><component :is="menu.icon" /></el-icon>
            <span>{{ menu.name }}</span>
          </el-menu-item>
        </template>
      </el-menu>

      <button
        v-if="canAccessVisualScreen && (!isCollapsed || isMobile)"
        type="button"
        class="sidebar-visual-link"
        aria-label="打开可视化大屏"
        @click="goVisualScreen"
      >
        <span>可视化大屏</span>
      </button>
    </aside>

    <div class="main-container">
      <header class="header">
        <div class="header-left">
          <el-button
            v-if="isMobile"
            :icon="Menu"
            text
            @click="mobileSidebarOpen = true"
          />
          <el-button
            v-else
            :icon="isCollapsed ? 'Expand' : 'Fold'"
            text
            @click="isCollapsed = !isCollapsed"
          />
          <button v-if="!isMobile" type="button" class="admin-topbar-title admin-topbar-home" @click="goAdminHome">
            <el-icon><House /></el-icon>
            <span>{{ adminWorkbenchTitle }}</span>
          </button>
        </div>

        <div class="header-right">
          <el-button class="frontend-button" plain @click="router.push('/')">
            <el-icon><Monitor /></el-icon>{{ t('layout.backToFrontend') }}
          </el-button>
          <FullscreenToggle />

          <el-dropdown v-if="showTenantSwitch" @command="handleTenantSwitch">
            <span class="tenant-switch">
              {{ t('layout.switchTenant') }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  v-for="item in switchTenantOptions"
                  :key="`${item.tenantIdValue}-${item.roleId}`"
                  :command="item"
                >
                  {{ item.tenantName }} / {{ item.roleName }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :src="userAvatar">
                {{ userInitial }}
              </el-avatar>
              <span class="user-name">{{ userName }}</span>
              <span class="role-tag">{{ roleName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>{{ t('nav.profile') }}
                </el-dropdown-item>
                <el-dropdown-item command="frontend">
                  <el-icon><Monitor /></el-icon>{{ t('layout.frontendHome') }}
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>{{ t('nav.logout') }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content">
        <RecentVisits />
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowDown, User, SwitchButton, Monitor, Menu, Close,
  Setting, UserFilled, Document, Calendar,
  Trophy, DataLine, Message, Folder, Timer, House
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import Watermark from '@/components/Watermark.vue'
import FullscreenToggle from '@/components/FullscreenToggle.vue'
import RecentVisits from '@/components/RecentVisits.vue'
import { cleanDisplayText, resolveCurrentTenantBranding } from '@/utils/tenantBranding'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapsed = ref(false)
const isMobile = ref(false)
const mobileSidebarOpen = ref(false)
const activeMenu = computed(() => route.path)
const userName = computed(() => cleanDisplayText(userStore.userName, t('auth.username')))
const userInitial = computed(() => (userName.value || t('auth.username') || '管').trim().charAt(0) || '管')
const userAvatar = computed(() => userStore.userInfo?.avatar || userStore.userInfo?.icon || '')
const currentTenantBranding = computed(() => resolveCurrentTenantBranding({
  tenantId: userStore.tenantId,
  tenantDirectory: userStore.tenantDirectory
}))
const switchTenantOptions = computed(() => (userStore.switchableTenantViews || []).map((item) => ({
  ...item,
  tenantName: cleanDisplayText(item?.tenantName, item?.tenantId ? `租户 ${item.tenantId}` : '租户'),
  roleName: cleanDisplayText(item?.roleName, item?.roleId ? `角色 ${item.roleId}` : '角色')
})))
const showTenantSwitch = computed(() => switchTenantOptions.value.length > 1)
const userRole = computed(() => userStore.roleId)
const dynamicAdminMenus = computed(() => userStore.adminMenus || [])
const roleName = computed(() => cleanDisplayText(userStore.getRoleName(), '管理员'))
const adminShellRoleClass = computed(() => {
  if (Number(userRole.value) === 8) {
    return 'role-teacher'
  }
  if (Number(userRole.value) === 7) {
    return 'role-club-manager'
  }
  return 'role-system-admin'
})
const adminBrandTitle = computed(() => {
  if (adminShellRoleClass.value === 'role-teacher') {
    return '高校多租户社团管理系统'
  }
  return currentTenantBranding.value.tenantName || '社团管理系统'
})
const adminBrandSubTitle = computed(() => {
  if (adminShellRoleClass.value === 'role-teacher') {
    return 'Multi-tenant Club Management System'
  }
  return '高校多租户平台'
})
const adminWorkbenchTitle = computed(() => {
  if (adminShellRoleClass.value === 'role-teacher') {
    return '指导老师工作台'
  }
  if (adminShellRoleClass.value === 'role-club-manager') {
    return '社团负责人工作台'
  }
  return '系统管理员工作台'
})

const iconComponentMap = {
  Setting,
  UserFilled,
  User,
  Document,
  Calendar,
  Trophy,
  DataLine,
  Message,
  Folder,
  Timer,
  Monitor,
  Menu
}

const normalizeAdminPath = (path) => {
  if (!path) return ''
  const value = String(path).trim()
  if (!value) return ''
  if (value.startsWith('/admin')) {
    return value
  }
  return `/admin/${value.replace(/^\/+/, '')}`
}

const normalizeMenuIcon = (icon) => {
  if (typeof icon === 'string' && icon.trim()) {
    return iconComponentMap[icon.trim()] || Document
  }
  return icon || Document
}

const mapBackendMenu = (menu) => ({
  name: menu.label || menu.menuName || menu.name || '',
  path: normalizeAdminPath(menu.path || ''),
  icon: normalizeMenuIcon(menu.icon),
  children: (menu.subMenus || [])
    .map(mapBackendMenu)
    .filter((item) => (item.name && item.path) || (item.children && item.children.length > 0))
})

const filteredMenus = computed(() => {
  return dynamicAdminMenus.value
    .map(mapBackendMenu)
    .filter((item) => (item.name && item.path) || (item.children && item.children.length > 0))
})

const collectMenuPaths = (menus = []) => menus.flatMap((menu) => [
  ...(menu.path ? [menu.path] : []),
  ...collectMenuPaths(menu.children || [])
])

const canAccessVisualScreen = computed(() =>
  collectMenuPaths(filteredMenus.value).includes('/admin/operation/visual-screen')
)

const breadcrumbs = computed(() =>
  route.matched
    .filter((item) => item.meta?.title)
    .map((item) => ({ path: item.path, name: item.meta?.title || item.name }))
)

const syncViewport = () => {
  if (typeof window === 'undefined') {
    return
  }
  isMobile.value = window.innerWidth <= 960
  if (!isMobile.value) {
    mobileSidebarOpen.value = false
  }
}

watch(
  () => route.fullPath,
  () => {
    mobileSidebarOpen.value = false
  }
)

onMounted(() => {
  syncViewport()
  if (userStore.token) {
    userStore.fetchAdminMenus()
  }
  window.addEventListener('resize', syncViewport)
})

onUnmounted(() => {
  window.removeEventListener('resize', syncViewport)
  document.body.classList.remove('admin-sidebar-locked')
})

watch(mobileSidebarOpen, (open) => {
  if (typeof document === 'undefined') {
    return
  }
  document.body.classList.toggle('admin-sidebar-locked', open)
})

const goAdminHome = () => {
  router.push('/admin')
}

const goVisualScreen = () => {
  router.push('/admin/operation/visual-screen')
}

const handleCommand = async (command) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'frontend':
      router.push('/')
      break
    case 'logout':
      try {
        await ElMessageBox.confirm(t('layout.logoutConfirmMessage'), t('layout.logoutConfirmTitle'), {
          confirmButtonText: t('common.confirm'),
          cancelButtonText: t('common.cancel'),
          type: 'warning'
        })
        userStore.logout()
        ElMessage.success(t('layout.logoutSuccess'))
        router.push('/login')
      } catch {
        // cancelled
      }
      break
    default:
      break
  }
}

const handleTenantSwitch = async (tenant) => {
  const result = await userStore.switchTenant(tenant)
  const tenantName = cleanDisplayText(tenant?.tenantName, tenant?.tenantId || tenant)
  if (result.success) {
    ElMessage.success(t('layout.switchTenantSuccess', { tenant: tenantName }))
    window.location.reload()
    return
  }
  ElMessage.error(result.message || t('layout.switchTenantFailed'))
}
</script>

<style lang="scss" scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  background: #f3f7ff;
  position: relative;
  overflow: hidden;
}

.admin-layout::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: var(--rk-admin-bg-image);
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  opacity: var(--rk-admin-bg-image-opacity);
  pointer-events: none;
  z-index: 0;
}

.sidebar,
.main-container {
  position: relative;
  z-index: 1;
}

.sidebar {
  width: 306px;
  background: rgba(255, 255, 255, 0.96);
  transition: width 0.3s;
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-height: 0;
  overflow: hidden;
  box-shadow: 16px 0 40px rgba(52, 112, 185, 0.08);

  &.collapsed {
    width: 64px;

    .logo-title {
      display: none;
    }
  }

  &.mobile {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    width: min(84vw, 320px);
    transform: translateX(-100%);
    transition: transform 0.3s ease;
    box-shadow: 16px 0 40px rgba(15, 23, 42, 0.18);
    z-index: 11;
  }

  &.mobile.open {
    transform: translateX(0);
  }
}

.sidebar-header {
  height: 112px;
  flex: 0 0 112px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22px 30px 18px;
  background: transparent;
}

.admin-sidebar-brand {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr);
  grid-template-rows: auto auto;
  column-gap: 14px;
  align-items: center;
  min-width: 0;

  .logo-text,
  .logo-image {
    grid-row: 1 / 3;
    width: 52px;
    height: 52px;
    border-radius: 8px;
  }

  .logo-text {
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 20px;
    font-weight: 900;
    background: linear-gradient(135deg, #2d8cff, #1269f6);
    box-shadow: 0 12px 28px rgba(18, 105, 246, 0.22);
  }

  .logo-image {
    object-fit: cover;
    background: rgba(255, 255, 255, 0.16);
  }

  .logo-title {
    overflow: hidden;
    color: #14213d;
    font-size: 20px;
    font-weight: 900;
    line-height: 1.2;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  small {
    overflow: hidden;
    color: #8b98ae;
    font-size: 13px;
    font-weight: 800;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.sidebar-close {
  color: #fff;
}

.mobile-sidebar-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.42);
  z-index: 10;
}

.sidebar-menu {
  flex: 1;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  border-right: none;
  background: transparent;
  padding: 8px 24px 24px;
  scrollbar-width: thin;
  scrollbar-color: rgba(115, 135, 164, 0.28) transparent;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(115, 135, 164, 0.28);
    border-radius: 999px;
  }

  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    height: 58px;
    border-radius: 8px;
    color: #33415d;
    font-size: 16px;
    font-weight: 800;

    &:hover {
      color: #1473ff;
      background: #eef6ff;
    }
  }

  :deep(.el-menu-item.is-active) {
    color: #fff;
    background: linear-gradient(135deg, #2584ff 0%, #1269f6 100%);
    box-shadow: 0 14px 30px rgba(37, 132, 255, 0.22);
  }

  :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
    color: #1473ff;
  }

  :deep(.el-sub-menu .el-menu) {
    background: transparent;
  }

  :deep(.el-sub-menu .el-menu-item) {
    color: #52617a;
    background: transparent;
    min-width: unset;

    &:hover {
      color: #1473ff;
      background: #eef6ff;
    }
  }
}

.sidebar-menu :deep(.el-sub-menu .el-menu),
.sidebar-menu :deep(.el-menu--inline) {
  background: transparent !important;
}

.sidebar-menu :deep(.el-sub-menu .el-menu-item),
.sidebar-menu :deep(.el-menu--inline .el-menu-item) {
  background: transparent !important;
  min-width: unset;
}

.sidebar-menu :deep(.el-sub-menu .el-menu-item:hover),
.sidebar-menu :deep(.el-menu--inline .el-menu-item:hover) {
  background: #eef6ff !important;
  color: #1473ff;
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

.header {
  height: 84px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 1px 0 rgba(218, 228, 242, 0.82);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.frontend-button {
  border-color: #dce8f8;
  color: #1473ff;
  background: #f4f9ff;
}

.admin-topbar-title {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  border: 0;
  padding: 0;
  background: transparent;
  color: #14213d;
  font-size: 20px;
  font-weight: 900;
  cursor: pointer;

  .el-icon {
    color: #9aa8bd;
  }

  &:hover {
    color: #1473ff;

    .el-icon {
      color: #1473ff;
    }
  }
}

.tenant-switch,
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;

  .user-name {
    font-size: 14px;
    color: var(--rk-text-primary);
  }

  .role-tag {
    font-size: 12px;
    padding: 2px 8px;
    background: color-mix(in srgb, var(--rk-primary) 12%, white);
    color: var(--rk-primary);
    border-radius: 4px;
  }
}

.content {
  flex: 1;
  padding: 24px 28px 32px;
  overflow: auto;
  min-width: 0;
}

.role-teacher {
  background: #f4f8ff;

  .sidebar {
    background: linear-gradient(180deg, #0d4a92 0%, #063c80 48%, #0752b8 100%);
    box-shadow: 18px 0 42px rgba(5, 51, 112, 0.18);
  }

  .admin-sidebar-brand {
    .logo-title {
      color: #fff;
      font-size: 17px;
    }

    small {
      color: rgba(255, 255, 255, 0.72);
      font-size: 11px;
    }
  }

  .sidebar-menu {
    scrollbar-color: rgba(255, 255, 255, 0.28) transparent;

    &::-webkit-scrollbar-thumb {
      background: rgba(255, 255, 255, 0.28);
    }

    :deep(.el-menu-item),
    :deep(.el-sub-menu__title),
    :deep(.el-sub-menu .el-menu-item) {
      color: rgba(255, 255, 255, 0.82);

      &:hover {
        color: #fff;
        background: rgba(255, 255, 255, 0.1);
      }
    }

    :deep(.el-menu-item.is-active) {
      color: #fff;
      background: linear-gradient(135deg, #2584ff 0%, #1269f6 100%);
    }
  }

  .sidebar-menu :deep(.el-sub-menu .el-menu-item:hover),
  .sidebar-menu :deep(.el-menu--inline .el-menu-item:hover) {
    background: rgba(255, 255, 255, 0.1) !important;
    color: #fff;
  }
}

.role-system-admin,
.role-club-manager {
  .sidebar-menu :deep(.el-sub-menu .el-menu-item),
  .sidebar-menu :deep(.el-menu--inline .el-menu-item) {
    color: #52617a !important;
  }

  .sidebar-menu :deep(.el-sub-menu .el-menu-item:hover),
  .sidebar-menu :deep(.el-menu--inline .el-menu-item:hover) {
    color: #1473ff !important;
  }

  .sidebar-menu :deep(.el-sub-menu .el-menu-item.is-active),
  .sidebar-menu :deep(.el-menu--inline .el-menu-item.is-active) {
    color: #fff !important;
    background: linear-gradient(135deg, #2584ff 0%, #1269f6 100%) !important;
    box-shadow: 0 10px 22px rgba(37, 132, 255, 0.2);
  }
}

.sidebar-visual-link {
  flex: 0 0 118px;
  display: block;
  width: calc(100% - 56px);
  height: 118px;
  margin: 0 28px 28px;
  border: 0;
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(11, 31, 68, 0.92), rgba(22, 92, 170, 0.86)),
    repeating-linear-gradient(90deg, rgba(255, 255, 255, 0.12) 0 1px, transparent 1px 24px),
    repeating-linear-gradient(0deg, rgba(255, 255, 255, 0.08) 0 1px, transparent 1px 18px);
  background-size: cover;
  background-position: center;
  cursor: pointer;
  overflow: hidden;
  position: relative;
  box-shadow: 0 14px 28px rgba(20, 115, 255, 0.12);

  &::before {
    content: '';
    position: absolute;
    left: 18px;
    right: 18px;
    top: 24px;
    height: 34px;
    border: 1px solid rgba(125, 211, 252, 0.55);
    border-radius: 8px;
    background:
      linear-gradient(90deg, rgba(96, 165, 250, 0.18), rgba(34, 211, 238, 0.3)),
      linear-gradient(180deg, transparent 47%, rgba(125, 211, 252, 0.85) 48% 52%, transparent 53%);
  }

  &::after {
    content: 'OPS';
    position: absolute;
    top: 66px;
    right: 18px;
    color: rgba(255, 255, 255, 0.7);
    font-size: 18px;
    font-weight: 900;
    letter-spacing: 0;
  }

  span {
    position: absolute;
    left: 12px;
    bottom: 10px;
    border-radius: 999px;
    padding: 4px 10px;
    color: #fff;
    font-size: 12px;
    font-weight: 800;
    background: rgba(20, 115, 255, 0.82);
    opacity: 0;
    transform: translateY(4px);
    transition: opacity 0.2s ease, transform 0.2s ease;
  }

  &:hover span,
  &:focus-visible span {
    opacity: 1;
    transform: translateY(0);
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.mobile-mask-fade-enter-active,
.mobile-mask-fade-leave-active {
  transition: opacity 0.2s ease;
}

.mobile-mask-fade-enter-from,
.mobile-mask-fade-leave-to {
  opacity: 0;
}

@media (max-width: 960px) {
  :global(body.admin-sidebar-locked) {
    overflow: hidden;
  }

  .header {
    height: auto;
    min-height: 64px;
    padding: 12px 16px;
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  .header-left,
  .header-right {
    width: 100%;
    flex-wrap: wrap;
    gap: 12px;
  }

  .header-left {
    justify-content: space-between;
  }

  .header-right {
    justify-content: space-between;
  }

  .content {
    padding: 16px;
  }

  .tenant-switch,
  .user-info {
    flex-wrap: wrap;
  }

  .user-info .role-tag {
    display: none;
  }

  .content :deep(.el-form--inline) {
    display: grid;
    grid-template-columns: minmax(0, 1fr);
    gap: 10px;
    align-items: stretch;
  }

  .content :deep(.el-form--inline .el-form-item) {
    margin-right: 0;
    margin-bottom: 0;
    width: 100%;
  }

  .content :deep(.el-form--inline .el-form-item__content) {
    width: 100%;
  }

  .content :deep(.el-select),
  .content :deep(.el-input),
  .content :deep(.el-date-editor),
  .content :deep(.el-cascader) {
    width: 100% !important;
  }

  .content :deep(.el-dialog) {
    width: calc(100vw - 24px) !important;
    margin-top: 6vh !important;
  }

  .content :deep(.el-pagination) {
    justify-content: flex-start;
    flex-wrap: wrap;
    gap: 8px;
  }
}

@media (max-width: 640px) {
  .header-right {
    align-items: stretch;
  }

  .frontend-button {
    width: 100%;
  }

  .user-info {
    width: 100%;
    justify-content: space-between;
  }

  .user-info .user-name {
    max-width: 120px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .content {
    padding: 12px;
  }
}
</style>
