import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import {
  getMyMenuTree as getMyMenuTreeApi,
} from '@/api/role'
import {
  confirmEmailLogin as confirmEmailLoginApi,
  getSwitchableTenants as getSwitchableTenantsApi,
  getUserInfo as getUserInfoApi,
  login as loginApi,
  logout as logoutApi,
  switchTenant as switchTenantApi
} from '@/api/user'
import { getPublicTenantList } from '@/api/tenant'
import {
  ADMIN_BACKEND_ROLE_IDS,
  ADMIN_CONTENT_ROLE_IDS,
  MEMBER_ROLE_IDS,
  MANAGER_ROLE_IDS,
  SUPER_ADMIN_ROLE_IDS,
  TENANT_ADMIN_ROLE_IDS,
  TEACHER_ROLE_IDS,
  canAccessAdminRoute,
  canEnterAdmin,
  flattenAdminMenuPaths,
  isAdminRoleId
} from '@/utils/adminAccess'
import { normalizeUserInfoMedia } from '@/utils/mediaUrl'
import { normalizeTenantDirectory } from '@/utils/tenantBranding'

const ROLE_NAME_MAP = {
  1: '超级管理员',
  2: '普通成员',
  3: '租户管理员',
  4: '普通成员',
  5: '租户管理员',
  6: '普通成员',
  7: '社团负责人',
  8: '指导老师'
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(normalizeUserInfoMedia(JSON.parse(localStorage.getItem('userInfo') || 'null')))
  const tenantId = ref(localStorage.getItem('tenantId') || '')
  const switchableTenants = ref([])
  const tenantDirectory = ref([])
  const adminMenus = ref(JSON.parse(localStorage.getItem('adminMenus') || '[]'))
  const adminMenusError = ref(null)
  const adminMenusLastLoadedAt = ref(localStorage.getItem('adminMenusLastLoadedAt') || '')

  watch(token, (value) => {
    if (value) {
      localStorage.setItem('token', value)
    } else {
      localStorage.removeItem('token')
    }
  })

  watch(
    userInfo,
    (value) => {
      if (value) {
        localStorage.setItem('userInfo', JSON.stringify(value))
      } else {
        localStorage.removeItem('userInfo')
      }
    },
    { deep: true }
  )

  watch(tenantId, (value) => {
    if (value) {
      localStorage.setItem('tenantId', value)
    } else {
      localStorage.removeItem('tenantId')
    }
  })

  watch(
    adminMenus,
    (value) => {
      localStorage.setItem('adminMenus', JSON.stringify(value || []))
    },
    { deep: true }
  )

  watch(adminMenusLastLoadedAt, (value) => {
    if (value) {
      localStorage.setItem('adminMenusLastLoadedAt', value)
    } else {
      localStorage.removeItem('adminMenusLastLoadedAt')
    }
  })

  const isLoggedIn = computed(() => !!token.value && !!tenantId.value)
  const userName = computed(() => userInfo.value?.username || '')
  const roleId = computed(() => {
    const roles = userInfo.value?.roles || []
    if (!roles.length) return 0
    const role = roles[0]
    return Number(role?.roleId || role?.id || role?.role_id || 0)
  })

  const switchableTenantViews = computed(() => {
    const tenantMap = new Map((tenantDirectory.value || []).map((item) => [String(item.id), item.tenantName]))
    const currentRoleId = Number(roleId.value || 0)
    return (switchableTenants.value || []).map((item) => {
      const itemRoleId = Number(item.roleId || 0)
      const tenantName = tenantMap.get(String(item.tenantId)) || `租户 ${item.tenantId}`
      return {
        ...item,
        tenantIdValue: item.tenantId,
        tenantId: tenantName,
        tenantName,
        roleName: ROLE_NAME_MAP[itemRoleId] || `角色 ${itemRoleId || '-'}`,
        isCurrent: String(item.tenantId) === String(tenantId.value) && Number(itemRoleId || 0) === Number(currentRoleId || 0)
      }
    })
  })

  const adminMenuPaths = computed(() => flattenAdminMenuPaths(adminMenus.value || []))

  function applyAuthSession(data, fallbackTenantId = '') {
    const normalizedToken = data.token || ''
    const normalizedTenantId = data.organizationId || fallbackTenantId || ''
    const normalizedUserInfo = normalizeUserInfoMedia({
      userId: data.userId,
      username: data.username,
      userType: data.userType,
      organizationId: data.organizationId,
      roles: data.roles || []
    })
    token.value = normalizedToken
    tenantId.value = normalizedTenantId
    userInfo.value = normalizedUserInfo
    if (normalizedToken) {
      localStorage.setItem('token', normalizedToken)
    }
    if (normalizedTenantId) {
      localStorage.setItem('tenantId', String(normalizedTenantId))
    }
    if (normalizedUserInfo) {
      localStorage.setItem('userInfo', JSON.stringify(normalizedUserInfo))
    }
    if (data.refreshToken) {
      localStorage.setItem('refreshToken', data.refreshToken)
    }
  }

  async function bootstrapAuthSession(data, fallbackTenantId = '') {
    applyAuthSession(data, fallbackTenantId)
    await fetchUserInfo()
    await fetchSwitchableTenants()
    await fetchAdminMenus()
    return { success: true }
  }

  async function fetchTenantDirectory() {
    try {
      const res = await getPublicTenantList()
      if (res.code === 200 && Array.isArray(res.data)) {
        tenantDirectory.value = normalizeTenantDirectory(res.data)
        return tenantDirectory.value
      }
      tenantDirectory.value = []
      return []
    } catch (error) {
      console.error('获取租户目录失败:', error)
      tenantDirectory.value = []
      return []
    }
  }

  async function fetchSwitchableTenants() {
    if (!token.value) {
      switchableTenants.value = []
      return []
    }
    try {
      if (!tenantDirectory.value.length) {
        await fetchTenantDirectory()
      }
      const res = await getSwitchableTenantsApi()
      if (res.code === 200) {
        switchableTenants.value = res.data || []
        return switchableTenants.value
      }
      switchableTenants.value = []
      return []
    } catch (error) {
      console.error('获取可切换租户失败:', error)
      switchableTenants.value = []
      return []
    }
  }

  async function fetchAdminMenus() {
    if (!token.value) {
      adminMenus.value = []
      adminMenusError.value = null
      adminMenusLastLoadedAt.value = ''
      return []
    }
    try {
      const res = await getMyMenuTreeApi({ silentError: true })
      const nextMenus = Array.isArray(res?.data) ? res.data : (Array.isArray(res) ? res : [])
      adminMenus.value = nextMenus
      adminMenusError.value = null
      adminMenusLastLoadedAt.value = new Date().toISOString()
      return adminMenus.value
    } catch (error) {
      console.error('获取后台菜单失败:', error)
      adminMenusError.value = {
        message: error.response?.data?.message || error.message || '获取后台菜单失败',
        occurredAt: new Date().toISOString()
      }
      return adminMenus.value
    }
  }

  async function login(loginForm) {
    token.value = ''
    userInfo.value = null
    tenantId.value = ''
    switchableTenants.value = []
    adminMenus.value = []
    adminMenusError.value = null
    adminMenusLastLoadedAt.value = ''
    localStorage.removeItem('refreshToken')

    try {
      const res = await loginApi({ ...loginForm, clientType: 'web' })
      if (res.code === 200 && res.data) {
        return await bootstrapAuthSession(res.data, loginForm.organizationId || '')
      }
      return { success: false, message: res.message || '登录失败' }
    } catch (error) {
      console.error('登录失败:', error)
      return {
        success: false,
        message: error.response?.data?.message || error.message || '登录失败，请稍后重试'
      }
    }
  }

  async function loginWithEmailConfirm(payload) {
    token.value = ''
    userInfo.value = null
    tenantId.value = ''
    switchableTenants.value = []
    adminMenus.value = []
    adminMenusError.value = null
    adminMenusLastLoadedAt.value = ''
    localStorage.removeItem('refreshToken')

    try {
      const res = await confirmEmailLoginApi(payload)
      if (res.code === 200 && res.data) {
        return await bootstrapAuthSession(res.data, String(payload.tenantId || ''))
      }
      return { success: false, message: res.message || '邮箱登录失败' }
    } catch (error) {
      console.error('邮箱验证码登录失败:', error)
      return {
        success: false,
        message: error.response?.data?.message || error.message || '邮箱登录失败，请稍后重试'
      }
    }
  }

  async function switchTenant(tenant) {
    const targetTenantId = typeof tenant === 'object' ? (tenant.tenantIdValue || tenant.tenantId) : tenant
    const targetRoleId = typeof tenant === 'object' ? tenant.roleId : undefined
    try {
      const res = await switchTenantApi(targetTenantId, targetRoleId)
      if (res.code === 200 && res.data) {
        applyAuthSession(res.data, String(targetTenantId))
        await fetchUserInfo()
        await fetchSwitchableTenants()
        await fetchAdminMenus()
        return { success: true }
      }
      return { success: false, message: res.msg || '切换租户失败' }
    } catch (error) {
      console.error('切换租户失败:', error)
      return { success: false, message: error.message || '切换租户失败' }
    }
  }

  async function fetchUserInfo() {
    try {
      const res = await getUserInfoApi()
      if (res.code === 200 && res.data) {
        userInfo.value = normalizeUserInfoMedia({ ...userInfo.value, ...res.data })
        return { success: true }
      }
      return { success: false }
    } catch (error) {
      console.error('获取用户信息失败:', error)
      return { success: false }
    }
  }

  async function logout() {
    try {
      await logoutApi()
    } catch (error) {
      console.error('登出接口调用失败:', error)
    } finally {
      token.value = ''
      userInfo.value = null
      tenantId.value = ''
      switchableTenants.value = []
      tenantDirectory.value = []
      adminMenus.value = []
      adminMenusError.value = null
      adminMenusLastLoadedAt.value = ''
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('userName')
      localStorage.removeItem('adminMenus')
      localStorage.removeItem('adminMenusLastLoadedAt')
    }
  }

  function updateUserInfo(info) {
    userInfo.value = normalizeUserInfoMedia({ ...userInfo.value, ...info })
  }

  function setTenantId(id) {
    tenantId.value = String(id)
  }

  function requireLogin() {
    return !token.value || !tenantId.value
  }

  function hasRole(allowedRoles) {
    if (!allowedRoles || allowedRoles.length === 0) return false
    return allowedRoles.includes(roleId.value)
  }

  function isAdmin() {
    return isAdminRoleId(roleId.value) || hasDynamicAdminAccess()
  }

  function hasDynamicAdminAccess() {
    return canEnterAdmin(0, adminMenuPaths.value)
  }

  function canAccessAdminPath(path) {
    return canAccessAdminRoute(path, roleId.value, adminMenuPaths.value)
  }

  function canEnterAdminConsole() {
    return canEnterAdmin(roleId.value, adminMenuPaths.value)
  }

  function isSuperAdmin() {
    return SUPER_ADMIN_ROLE_IDS.includes(roleId.value)
  }

  function getRoleDisplayName() {
    return ROLE_NAME_MAP[roleId.value] || `未知角色(${roleId.value})`
  }

  return {
    token,
    userInfo,
    tenantId,
    switchableTenants,
    tenantDirectory,
    adminMenus,
    adminMenusError,
    adminMenusLastLoadedAt,
    adminMenuPaths,
    switchableTenantViews,
    isLoggedIn,
    userName,
    roleId,
    login,
    loginWithEmailConfirm,
    switchTenant,
    logout,
    fetchUserInfo,
    fetchSwitchableTenants,
    fetchTenantDirectory,
    fetchAdminMenus,
    updateUserInfo,
    setTenantId,
    requireLogin,
    hasRole,
    isAdmin,
    hasDynamicAdminAccess,
    canAccessAdminPath,
    canEnterAdminConsole,
    isSuperAdmin,
    SUPER_ADMIN_ROLE_IDS,
    TENANT_ADMIN_ROLE_IDS,
    MEMBER_ROLE_IDS,
    MANAGER_ROLE_IDS,
    TEACHER_ROLE_IDS,
    ADMIN_CONTENT_ROLE_IDS,
    ADMIN_BACKEND_ROLE_IDS,
    getRoleName: getRoleDisplayName
  }
})
