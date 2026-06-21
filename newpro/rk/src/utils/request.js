import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getGatewayBaseUrl } from './runtimeConfig.js'
import { isSuccessfulResponseCode } from './responseCode.js'

const service = axios.create({
  baseURL: getGatewayBaseUrl() || '',
  timeout: 15000
})

let showLoginCallback = null
let refreshSessionPromise = null

export function setShowLoginCallback(callback) {
  showLoginCallback = callback
}

function isAuthRefreshSkipped(config = {}) {
  const url = String(config.url || '')
  return url.includes('/auth/login') ||
    url.includes('/auth/logout') ||
    url.includes('/auth/refresh') ||
    url.includes('/auth/refresh-session')
}

async function refreshAuthSession(config = {}) {
  if (isAuthRefreshSkipped(config)) {
    return
  }
  const token = localStorage.getItem('token')
  const refreshToken = localStorage.getItem('refreshToken')
  if (!token || !refreshToken) {
    return
  }
  if (!refreshSessionPromise) {
    refreshSessionPromise = axios({
      baseURL: getGatewayBaseUrl() || '',
      url: '/auth/refresh-session',
      method: 'post',
      data: refreshToken,
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json; charset=utf-8'
      }
    }).then((response) => {
      const payload = response.data?.data || response.data || {}
      const refreshedToken = typeof payload === 'string' ? payload : payload.token
      if (refreshedToken) {
        localStorage.setItem('token', refreshedToken)
      }
      if (payload.refreshToken) {
        localStorage.setItem('refreshToken', payload.refreshToken)
      }
    }).catch((error) => {
      const status = error.response?.status || error.response?.data?.code
      if (status === 400 || status === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('tenantId')
        localStorage.removeItem('userInfo')
        window.location.href = '/login'
      }
    }).finally(() => {
      refreshSessionPromise = null
    })
  }
  await refreshSessionPromise
}

service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    const tenantId = localStorage.getItem('tenantId')

    config.headers = config.headers || {}

    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    if (tenantId && !config.headers['X-Tenant-Id']) {
      config.headers['X-Tenant-Id'] = tenantId
    }

    const rawUserInfo = localStorage.getItem('userInfo')
    if (rawUserInfo && (!config.headers['X-Super-Admin'] || !config.headers['X-Role-Id'])) {
      try {
        const parsed = JSON.parse(rawUserInfo)
        const roles = Array.isArray(parsed?.roles) ? parsed.roles : []
        const roleId = Number(roles[0]?.roleId || roles[0]?.id || roles[0]?.role_id || 0)
        if (roleId > 0 && !config.headers['X-Role-Id']) {
          config.headers['X-Role-Id'] = String(roleId)
        }
        if (roleId === 1) {
          config.headers['X-Super-Admin'] = 'true'
        }
      } catch (error) {
        console.warn('parse userInfo for super-admin header failed', error)
      }
    }

    return config
  },
  (error) => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  async (response) => {
    const res = response.data

    if (response.config.responseType === 'blob') {
      return res
    }

    if (!isSuccessfulResponseCode(res.code, res.state)) {
      if (response.config?.silentError) {
        return Promise.reject(new Error(res.msg || '请求失败'))
      }
      if (res.state === 'warning') {
        ElMessage.warning(res.msg || '操作警告')
      } else {
        ElMessage.error(res.msg || '请求失败')
      }
      return Promise.reject(new Error(res.msg || '请求失败'))
    }

    await refreshAuthSession(response.config)
    return res
  },
  (error) => {
    if (error.config?.silentError) {
      return Promise.reject(error)
    }

    console.error('Response error:', error)

    if (error.response) {
      const status = error.response.status
      const message = error.response.data?.msg || error.response.data?.message

      switch (status) {
        case 400:
          ElMessage.error(message || '请求参数错误')
          break
        case 401: {
          const hasToken = localStorage.getItem('token')
          const hasTenantId = localStorage.getItem('tenantId')

          if (hasToken || hasTenantId) {
            localStorage.removeItem('token')
            localStorage.removeItem('tenantId')
            localStorage.removeItem('userInfo')
            window.location.href = '/login'
          } else if (
            typeof showLoginCallback === 'function' &&
            error.config?.showLoginOn401 === true
          ) {
            showLoginCallback()
          }
          break
        }
        case 403:
          ElMessage.error(message || '拒绝访问，权限不足')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error(message || '服务器错误，请稍后重试')
          break
        default:
          ElMessage.error(message || '网络错误')
      }
    } else if (error.message === 'Network Error') {
      ElMessage.error('网络连接失败，请检查网络设置或确认后端服务是否启动')
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else {
      ElMessage.error('请求失败，请稍后重试')
    }

    return Promise.reject(error)
  }
)

export default service
