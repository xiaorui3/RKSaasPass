<template>
  <div class="portal-app-shell">
    <GlobalAnnouncement />
    <SystemAnnouncement />

    <div class="portal-layout-frame">
      <aside class="portal-sidebar">
        <div class="portal-sidebar-brand">
          <div class="portal-logo">
            <img
              v-if="currentTenantBranding.logoUrl"
              :src="currentTenantBranding.logoUrl"
              :alt="currentTenantBranding.tenantName || t('layout.tenantLogoAlt')"
              class="portal-logo-image"
            >
            <span v-else>{{ currentTenantBranding.initials }}</span>
          </div>
          <div class="portal-brand-copy">
            <strong>{{ tenantBrandSettings.tenantDisplayName || currentTenantBranding.tenantName || '绀惧洟绠＄悊绯荤粺' }}</strong>
            <span>{{ tenantBrandSettings.slogan || '楂樻牎澶氱鎴峰钩鍙? }}</span>
          </div>
        </div>

        <nav class="portal-side-nav" aria-label="鍓嶅彴瀵艰埅">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="portal-side-link"
            :class="{ active: isNavItemActive(item) }"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.name }}</span>
            <el-badge
              v-if="item.path === '/notifications' && unreadCount > 0"
              :value="unreadCount"
              :max="99"
              class="portal-side-badge"
            />
          </router-link>
        </nav>

        <div class="portal-sidebar-card">
          <span>褰撳墠鑱旂洘</span>
          <strong>{{ tenantBrandSettings.tenantDisplayName || currentTenantBranding.tenantName || '鏈€夋嫨绉熸埛' }}</strong>
          <button v-if="showTenantSwitch" type="button" @click="openTenantMenu = !openTenantMenu">
            鍒囨崲
            <el-icon><ArrowDown /></el-icon>
          </button>
          <div v-if="openTenantMenu" class="portal-tenant-popover">
            <button
              v-for="item in switchTenantOptions"
              :key="`${item.tenantIdValue}-${item.roleId}`"
              type="button"
              @click="handleTenantSwitch(item)"
            >
              <span>{{ item.tenantName }}</span>
              <small>{{ item.roleName }}</small>
            </button>
          </div>
        </div>
      </aside>

      <div class="portal-workspace">
      <header class="portal-topbar">
        <div class="portal-topbar-left">
          <button
            class="mobile-nav-toggle"
            type="button"
            :aria-expanded="isMobileNavOpen"
            aria-label="Toggle navigation"
            @click="isMobileNavOpen = !isMobileNavOpen"
          >
            <el-icon :size="22"><Menu /></el-icon>
          </button>
          <div class="portal-route-title">
            <span>褰撳墠浣嶇疆</span>
            <strong>{{ currentRouteTitle }}</strong>
          </div>
        </div>

        <form class="portal-search-pill" @submit.prevent="goSearch">
          <input
            v-model="searchKeyword"
            type="search"
            :placeholder="`${t('common.search')}绀惧洟銆佹椿鍔ㄦ垨鍐呭`"
          >
          <button type="submit" aria-label="鎼滅储">
            <el-icon><Search /></el-icon>
          </button>
        </form>

        <div class="portal-topbar-actions">
          <div class="lang-switch">
            <a href="#" :class="{ active: currentLang === 'zh' }" @click.prevent="switchLang('zh')">涓枃</a>
            <span>/</span>
            <a href="#" :class="{ active: currentLang === 'en' }" @click.prevent="switchLang('en')">EN</a>
          </div>

          <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="notification-badge">
            <router-link to="/notifications" class="icon-circle">
              <el-icon :size="20"><Bell /></el-icon>
            </router-link>
          </el-badge>

          <button class="icon-circle" type="button" @click="toggleDark(!isDark)">
            <el-icon><Switch /></el-icon>
          </button>

          <template v-if="!isLoggedIn">
            <router-link to="/login" class="portal-login-link">{{ t('nav.login') }}</router-link>
            <router-link to="/register" class="portal-register-link">{{ t('nav.register') }}</router-link>
          </template>

          <el-dropdown v-else class="user-dropdown" trigger="hover" @command="handleUserCommand">
            <span class="portal-user-chip">
              <el-avatar :size="42" :src="userAvatar">
                {{ userInitial }}
              </el-avatar>
              <span>{{ userName || '鍚屽' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">{{ t('nav.profile') }}</el-dropdown-item>
                <el-dropdown-item v-if="canEnterAdmin" command="admin">{{ t('nav.admin') }}</el-dropdown-item>
                <el-dropdown-item command="logout">{{ t('nav.logout') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <transition name="mobile-nav-fade">
        <div
          v-if="isMobile && isMobileNavOpen"
          class="mobile-nav-overlay"
          @click="isMobileNavOpen = false"
        />
      </transition>

      <transition name="mobile-nav-slide">
        <nav v-if="isMobile && isMobileNavOpen" class="mobile-nav-drawer">
          <div class="mobile-nav-header">
            <div class="mobile-nav-brand">
              <strong>{{ tenantBrandSettings.tenantDisplayName || currentTenantBranding.tenantName || '绀惧洟绠＄悊绯荤粺' }}</strong>
              <span>{{ userName || t('common.visitor') }}</span>
            </div>
            <button class="mobile-nav-close" type="button" @click="isMobileNavOpen = false">
              <el-icon :size="18"><Close /></el-icon>
            </button>
          </div>
          <div class="mobile-nav-links">
            <router-link
              v-for="item in navItems"
              :key="`mobile-${item.path}`"
              :to="item.path"
              class="mobile-nav-link"
              :class="{ active: isNavItemActive(item) }"
              @click="isMobileNavOpen = false"
            >
              <el-icon><component :is="item.icon" /></el-icon>
              {{ item.name }}
            </router-link>
          </div>
        </nav>
      </transition>

      <main class="main-content portal-main-content">
        <RecentVisits />
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>

      <footer class="portal-footer">
        <span>&copy; {{ currentYear }} {{ tenantBrandSettings.tenantDisplayName || currentTenantBranding.tenantName || 'RK-Web' }}</span>
        <span>{{ tenantBrandSettings.contactAddress || currentTenantContact.address }}</span>
        <span>{{ tenantBrandSettings.contactEmail || currentTenantContact.email }}</span>
      </footer>
    </div>
  </div>
    <BackToTop />
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowDown,
  Search,
  Bell,
  Menu,
  Close,
  House,
  Compass,
  Calendar,
  Trophy,
  Picture,
  User,
  Message,
  Setting,
  Notebook,
  Promotion,
  CollectionTag,
  Switch,
  OfficeBuilding,
  Tickets
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { useTenantSelfServiceStore } from '@/stores/tenantSelfService'
import { getNotifications, getUnreadNotificationCount } from '@/api/common'
import GlobalAnnouncement from '@/components/GlobalAnnouncement.vue'
import BackToTop from '@/components/BackToTop.vue'
import RecentVisits from '@/components/RecentVisits.vue'
import SystemAnnouncement from '@/components/SystemAnnouncement.vue'
import { setLanguage } from '@/i18n'
import { cleanDisplayText, resolveCurrentTenantBranding } from '@/utils/tenantBranding'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()
const tenantSelfServiceStore = useTenantSelfServiceStore()

const searchKeyword = ref('')
const unreadCount = ref(0)
const isDark = ref(localStorage.getItem('theme-dark') === 'true')
const currentLang = ref(localStorage.getItem('rk-lang') || 'zh')
const isMobile = ref(false)
const isMobileNavOpen = ref(false)
const openTenantMenu = ref(false)
let pollTimer = null

function normalizeUnreadCount(data) {
  if (!data) {
    return 0
  }
  if (typeof data.unread === 'number') {
    return data.unread
  }
  const records = Array.isArray(data) ? data : (data.records || data.list || [])
  return Array.isArray(records)
    ? records.filter((item) => !Boolean(item.read ?? item.isRead ?? item.readStatus)).length
    : 0
}

function syncViewportState() {
  if (typeof window === 'undefined') {
    return
  }
  isMobile.value = window.innerWidth <= 900
  if (!isMobile.value) {
    isMobileNavOpen.value = false
  }
}

function toggleDark(val) {
  isDark.value = val
  document.documentElement.classList.toggle('dark', val)
  localStorage.setItem('theme-dark', val)
}

if (isDark.value) {
  document.documentElement.classList.add('dark')
}

async function fetchUnreadCount() {
  if (!userStore.isLoggedIn) {
    unreadCount.value = 0
    return
  }
  try {
    const res = await getUnreadNotificationCount()
    if (res.code === 200) {
      unreadCount.value = normalizeUnreadCount(res.data)
      return
    }
    const fallback = await getNotifications({ read: false, limit: 200 })
    unreadCount.value = fallback.code === 200 ? normalizeUnreadCount(fallback.data) : 0
  } catch {
    // ignore polling noise
  }
}

function goSearch() {
  const q = searchKeyword.value.trim()
  if (q) {
    router.push({ path: '/search', query: { q } })
  }
}

function switchLang(lang) {
  currentLang.value = lang
  setLanguage(lang)
}

onMounted(() => {
  syncViewportState()
  window.addEventListener('resize', syncViewportState)
  window.addEventListener('rk-notifications-read-change', fetchUnreadCount)
  fetchUnreadCount()
  tenantSelfServiceStore.loadPublicConfig(userStore.tenantId)
  pollTimer = setInterval(fetchUnreadCount, 60000)
})

onUnmounted(() => {
  window.removeEventListener('resize', syncViewportState)
  window.removeEventListener('rk-notifications-read-change', fetchUnreadCount)
  document.body.classList.remove('mobile-nav-locked')
  if (pollTimer) {
    clearInterval(pollTimer)
  }
})

watch(
  () => route.fullPath,
  () => {
    isMobileNavOpen.value = false
    openTenantMenu.value = false
    fetchUnreadCount()
  }
)

watch(
  () => userStore.tenantId,
  (tenantId) => {
    tenantSelfServiceStore.loadPublicConfig(tenantId, true)
  }
)

watch(isMobileNavOpen, (open) => {
  if (typeof document === 'undefined') {
    return
  }
  document.body.classList.toggle('mobile-nav-locked', open)
})

const currentYear = computed(() => new Date().getFullYear())
const isLoggedIn = computed(() => userStore.isLoggedIn)
const userName = computed(() => cleanDisplayText(userStore.userName, ''))
const userInitial = computed(() => (userName.value || '鍚?).trim().charAt(0) || '鍚?)
const userAvatar = computed(() => userStore.userInfo?.avatar || userStore.userInfo?.icon || '')
const currentTenantBranding = computed(() => resolveCurrentTenantBranding({
  tenantId: userStore.tenantId,
  tenantDirectory: userStore.tenantDirectory
}))
const currentTenantContact = computed(() => {
  const tenant = (userStore.tenantDirectory || []).find((item) => String(item?.id) === String(userStore.tenantId))
  return {
    email: tenant?.contactEmail || 'example.com',
    address: tenant?.description || '褰撳墠绉熸埛闂ㄦ埛'
  }
})
const switchTenantOptions = computed(() => (userStore.switchableTenantViews || []).map((item) => ({
  ...item,
  tenantName: cleanDisplayText(item?.tenantName, item?.tenantId ? `绉熸埛 ${item.tenantId}` : '绉熸埛'),
  roleName: cleanDisplayText(item?.roleName, item?.roleId ? `瑙掕壊 ${item.roleId}` : '瑙掕壊')
})))
const showTenantSwitch = computed(() => switchTenantOptions.value.length > 1)
const canEnterAdmin = computed(() => userStore.canEnterAdminConsole())
const isPortalFrontendStyle = computed(() => themeStore.activeConfig?.frontendStyle === 'portal')
const tenantPortalSettings = computed(() => tenantSelfServiceStore.portalSettings)
const tenantBrandSettings = computed(() => tenantSelfServiceStore.brandSettings)
const tenantAdmissionSettings = computed(() => tenantSelfServiceStore.admissionSettings)

const classicNavItems = computed(() => [
  { name: t('nav.home'), path: '/', icon: House },
  { name: t('nav.about'), path: '/about', icon: Compass },
  { name: t('nav.news'), path: '/news', icon: Notebook },
  { name: t('nav.notices'), path: '/notices', icon: Message },
  { name: t('nav.activities'), path: '/activities', icon: Calendar },
  { name: t('nav.calendar'), path: '/calendar', icon: Calendar },
  { name: t('nav.competition'), path: '/competition', icon: Trophy },
  { name: t('nav.works'), path: '/works', icon: Picture },
  { name: isLoggedIn.value ? t('nav.applyOtherTenant') : t('nav.join'), path: '/join', icon: Promotion },
  { name: t('nav.alumni'), path: '/alumni', icon: OfficeBuilding },
  { name: t('nav.history'), path: '/history', icon: CollectionTag },
  { name: t('nav.notifications'), path: '/notifications', icon: Bell },
  { name: t('nav.contact'), path: '/contact', icon: User },
  ...(isLoggedIn.value ? [{ name: t('nav.profile'), path: '/profile', icon: Setting }] : [])
])

const portalNavItems = computed(() => {
  const settings = tenantPortalSettings.value
  const items = [
    { name: t('nav.home'), path: '/', icon: House }
  ]
  if (settings.showNews !== false || settings.showWorks !== false) {
    items.push({ name: '鍐呭鑱氬悎', path: '/content', icon: Notebook, matchPaths: ['/content', '/news', '/notices', '/works', '/history'] })
  }
  if (settings.showActivities !== false || settings.showCompetitions !== false) {
    items.push({ name: '娲诲姩璧涗簨', path: '/events', icon: Tickets, matchPaths: ['/events', '/activities', '/calendar', '/competition'] })
  }
  const communityMatchPaths = ['/community', '/about', '/contact']
  if (tenantAdmissionSettings.value.allowJoinApplication !== false) {
    communityMatchPaths.push('/join')
  }
  if (settings.showAlumni !== false) {
    communityMatchPaths.push('/alumni')
  }
  items.push({ name: '绀惧洟涓庢垚鍛?, path: '/community', icon: OfficeBuilding, matchPaths: communityMatchPaths })
  if (settings.allowPublicSearch !== false) {
    items.push({ name: t('common.search'), path: '/search', icon: Search })
  }
  if (isLoggedIn.value) {
    items.push({ name: t('nav.notifications'), path: '/notifications', icon: Bell })
    items.push({ name: t('nav.profile'), path: '/profile', icon: Setting })
  } else if (tenantAdmissionSettings.value.allowJoinApplication !== false) {
    items.push({ name: t('nav.join'), path: '/join', icon: Promotion })
  }
  return items
})

const navItems = computed(() => (isPortalFrontendStyle.value ? portalNavItems.value : classicNavItems.value))

const currentRouteTitle = computed(() => {
  const matched = [...navItems.value, ...classicNavItems.value]
    .sort((left, right) => right.path.length - left.path.length)
    .find((item) => isActive(item.path))
  return matched?.name || '棣栭〉'
})

const isActive = (path) => {
  if (path === '/') {
    return route.path === '/'
  }
  return route.path === path || route.path.startsWith(`${path}/`)
}

const isNavItemActive = (item) => {
  const paths = item.matchPaths || [item.path]
  return paths.some((path) => isActive(path))
}

const handleUserCommand = async (command) => {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm(t('layout.logoutConfirmMessage'), t('layout.logoutConfirmTitle'), {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: 'warning'
      })
      userStore.logout()
      ElMessage.success(t('layout.logoutSuccess'))
      router.push('/')
    } catch {
      // cancelled
    }
    return
  }

  if (command === 'profile') {
    router.push('/profile')
    return
  }

  if (command === 'admin') {
    router.push('/admin')
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
@use '../styles/variables.scss' as *;

.portal-app-shell {
  min-height: 100vh;
  background: #f3f8ff;
  color: #12213f;
}

.portal-layout-frame {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 290px minmax(0, 1fr);
}

.portal-sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: 18px;
  padding: 34px 24px 28px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 16px 0 40px rgba(52, 112, 185, 0.08);
  z-index: 8;
}

.portal-sidebar-brand {
  display: flex;
  align-items: center;
  gap: 14px;
}

.portal-logo {
  width: 54px;
  height: 54px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #2a7cff 0%, #1263f1 100%);
  color: #fff;
  font-size: 22px;
  font-weight: 900;
  box-shadow: 0 14px 28px rgba(18, 99, 241, 0.22);
  overflow: hidden;
}

.portal-logo-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.portal-brand-copy {
  display: grid;
  gap: 4px;
  min-width: 0;

  strong {
    overflow: hidden;
    color: #1674ff;
    font-size: 22px;
    font-weight: 900;
    line-height: 1.16;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  span {
    color: #7f8da7;
    font-size: 14px;
    font-weight: 700;
  }
}

.portal-side-nav {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
  overflow-y: auto;
  padding: 8px 2px;
}

.portal-side-link {
  position: relative;
  display: flex;
  align-items: center;
  gap: 16px;
  min-height: 56px;
  padding: 0 18px;
  border-radius: 8px;
  color: #33415d;
  font-size: 16px;
  font-weight: 800;
  text-decoration: none;
  transition: background 0.2s ease, color 0.2s ease, transform 0.2s ease;

  .el-icon {
    font-size: 22px;
  }

  &:hover {
    color: #1473ff;
    background: rgba(20, 115, 255, 0.08);
  }

  &.active {
    color: #fff;
    background: linear-gradient(135deg, #2584ff 0%, #1269f6 100%);
    box-shadow: 0 14px 30px rgba(37, 132, 255, 0.24);
  }
}

.portal-side-badge {
  margin-left: auto;
}

.portal-sidebar-card {
  position: relative;
  display: grid;
  gap: 7px;
  padding: 18px;
  border-radius: 8px;
  background: linear-gradient(135deg, #f1f7ff 0%, #e7f0ff 100%);
  color: #7a88a1;

  span {
    font-size: 13px;
    font-weight: 700;
  }

  strong {
    color: #1c2d4d;
    font-size: 16px;
  }

  button {
    width: max-content;
    display: inline-flex;
    align-items: center;
    gap: 4px;
    margin-top: 6px;
    border: none;
    background: transparent;
    color: #1473ff;
    font-weight: 800;
    cursor: pointer;
  }
}

.portal-tenant-popover {
  position: absolute;
  left: 18px;
  right: 18px;
  bottom: calc(100% + 8px);
  display: grid;
  gap: 6px;
  padding: 10px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 18px 42px rgba(21, 45, 82, 0.18);

  button {
    width: 100%;
    display: grid;
    justify-items: start;
    margin: 0;
    padding: 8px 10px;
    border-radius: 8px;
    color: #263b61;

    &:hover {
      background: #f2f7ff;
    }
  }

  small {
    color: #8b98ae;
  }
}

.portal-workspace {
  min-width: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  min-height: 100vh;
}

.portal-topbar {
  position: sticky;
  top: 0;
  z-index: 7;
  min-height: 92px;
  display: grid;
  grid-template-columns: minmax(180px, auto) minmax(260px, 420px) auto;
  align-items: center;
  gap: 20px;
  padding: 18px 34px;
  background: rgba(246, 250, 255, 0.86);
  backdrop-filter: blur(18px);
  border-bottom: 1px solid rgba(208, 222, 243, 0.72);
}

.portal-topbar-left,
.portal-topbar-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.portal-route-title {
  display: grid;
  gap: 4px;

  span {
    color: #8a98ad;
    font-size: 12px;
    font-weight: 700;
  }

  strong {
    color: #1b2b48;
    font-size: 18px;
    font-weight: 900;
  }
}

.portal-search-pill {
  height: 48px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 46px;
  align-items: center;
  overflow: hidden;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 12px 32px rgba(45, 112, 199, 0.08);

  input {
    width: 100%;
    min-width: 0;
    border: none;
    outline: none;
    padding: 0 18px 0 24px;
    background: transparent;
    color: #1b2b48;
    font-size: 14px;
  }

  button {
    height: 100%;
    border: none;
    background: transparent;
    color: #a3afc2;
    cursor: pointer;
  }
}

.portal-topbar-actions {
  justify-content: flex-end;
}

.lang-switch {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #a0aabd;
  font-weight: 800;

  a {
    color: #9aa6bb;
    text-decoration: none;

    &.active,
    &:hover {
      color: #1473ff;
    }
  }
}

.icon-circle {
  width: 42px;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 50%;
  background: #fff;
  color: #1f2c44;
  box-shadow: 0 10px 28px rgba(35, 69, 113, 0.08);
  cursor: pointer;
}

.portal-login-link,
.portal-register-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  padding: 0 18px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 800;
  text-decoration: none;
}

.portal-login-link {
  color: #1473ff;
  background: #fff;
}

.portal-register-link {
  color: #fff;
  background: #1473ff;
}

.portal-user-chip {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 4px 10px 4px 4px;
  border-radius: 999px;
  color: #263b61;
  font-weight: 900;
  cursor: pointer;
}

.mobile-nav-toggle {
  display: none;
  width: 42px;
  height: 42px;
  border: none;
  border-radius: 8px;
  background: #fff;
  color: #1f2c44;
  cursor: pointer;
  box-shadow: 0 10px 28px rgba(35, 69, 113, 0.08);
}

.portal-main-content {
  min-width: 0;
  overflow: auto;
  padding: 24px 34px 32px;
}

.portal-footer {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 0 34px 18px;
  color: #9aa6bb;
  font-size: 13px;
}

.mobile-nav-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.42);
  z-index: $z-index-modal-backdrop;
}

.mobile-nav-drawer {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: min(84vw, 320px);
  padding: 20px;
  background: #fff;
  box-shadow: 16px 0 42px rgba(15, 23, 42, 0.18);
  z-index: $z-index-modal;
  display: grid;
  grid-template-rows: auto 1fr;
  gap: 18px;
}

.mobile-nav-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.mobile-nav-brand {
  display: grid;
  gap: 4px;

  strong {
    color: #1674ff;
    font-size: 18px;
  }

  span {
    color: #7f8da7;
    font-size: 12px;
  }
}

.mobile-nav-close {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 8px;
  background: #eef5ff;
  color: #1f2c44;
  cursor: pointer;
}

.mobile-nav-links {
  display: grid;
  align-content: start;
  gap: 8px;
  overflow-y: auto;
}

.mobile-nav-link {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 46px;
  padding: 0 12px;
  border-radius: 8px;
  color: #33415d;
  font-weight: 800;
  text-decoration: none;

  &.active {
    color: #fff;
    background: #1473ff;
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity $transition-base;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.mobile-nav-fade-enter-active,
.mobile-nav-fade-leave-active {
  transition: opacity $transition-fast;
}

.mobile-nav-fade-enter-from,
.mobile-nav-fade-leave-to {
  opacity: 0;
}

.mobile-nav-slide-enter-active,
.mobile-nav-slide-leave-active {
  transition: transform $transition-base ease, opacity $transition-base ease;
}

.mobile-nav-slide-enter-from,
.mobile-nav-slide-leave-to {
  transform: translateX(-100%);
  opacity: 0;
}

@media (max-width: 1200px) {
  .portal-layout-frame {
    grid-template-columns: 250px minmax(0, 1fr);
  }

  .portal-sidebar {
    padding: 26px 16px 22px;
  }

  .portal-topbar {
    grid-template-columns: auto minmax(220px, 1fr);
  }

  .portal-search-pill {
    order: 3;
    grid-column: 1 / -1;
  }
}

@media (max-width: 900px) {
  :global(body.mobile-nav-locked) {
    overflow: hidden;
  }

  .portal-layout-frame {
    display: block;
  }

  .portal-sidebar {
    display: none;
  }

  .portal-topbar {
    position: sticky;
    min-height: auto;
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    padding: 14px 16px;
  }

  .portal-topbar-left {
    gap: 10px;
  }

  .mobile-nav-toggle {
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }

  .portal-route-title strong {
    font-size: 16px;
  }

  .portal-topbar-actions {
    gap: 10px;
  }

  .lang-switch,
  .portal-topbar-actions .icon-circle:nth-of-type(2) {
    display: none;
  }

  .portal-user-chip span {
    display: none;
  }

  .portal-main-content {
    padding: 16px;
  }

  .portal-footer {
    padding: 0 16px 16px;
    flex-direction: column;
  }
}

@media (max-width: 560px) {
  .portal-topbar {
    grid-template-columns: 1fr;
  }

  .portal-search-pill {
    height: 44px;
  }

  .portal-topbar-actions {
    justify-content: space-between;
  }

  .portal-login-link,
  .portal-register-link {
    padding: 0 12px;
  }
}
</style>
