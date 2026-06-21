const testEnv = globalThis.__RK_TEST_IMPORT_META_ENV__

function readViteEnv(name) {
  if (testEnv && typeof testEnv === 'object' && name in testEnv) {
    return testEnv[name]
  }
  if (typeof import.meta !== 'undefined' && import.meta.env) {
    return import.meta.env[name]
  }
  return undefined
}

function trimTrailingSlash(value) {
  return typeof value === 'string' ? value.replace(/\/+$/, '') : ''
}

function normalizeRuntimeUrl(value) {
  if (typeof value !== 'string') {
    return null
  }
  const trimmed = value.trim()
  if (!trimmed) {
    return ''
  }
  if (trimmed.startsWith('/')) {
    return trimTrailingSlash(trimmed)
  }
  return trimTrailingSlash(trimmed)
}

function readRuntimeConfig() {
  if (typeof window === 'undefined') {
    return {}
  }
  return window.__RK_RUNTIME_CONFIG__ || {}
}

export function getGatewayBaseUrl() {
  const runtimeValue = normalizeRuntimeUrl(readRuntimeConfig().gatewayBaseUrl)
  if (runtimeValue !== null) {
    return runtimeValue
  }
  return trimTrailingSlash(readViteEnv('VITE_GATEWAY_BASE_URL') || '')
}

export function getMinioBaseUrl() {
  const runtimeValue = normalizeRuntimeUrl(readRuntimeConfig().minioBaseUrl)
  if (runtimeValue !== null) {
    return runtimeValue
  }
  return trimTrailingSlash(readViteEnv('VITE_MINIO_BASE_URL') || '')
}

function normalizeTenantId(value) {
  if (value === undefined || value === null) {
    return ''
  }
  return String(value).trim()
}

export function getDefaultTenantId() {
  const runtimeConfig = readRuntimeConfig()
  const hostMap = runtimeConfig.tenantHostMap || {}
  if (typeof window !== 'undefined' && window.location && hostMap && typeof hostMap === 'object') {
    const hostCandidates = [window.location.host, window.location.hostname].filter(Boolean)
    for (const host of hostCandidates) {
      const tenantId = normalizeTenantId(hostMap[host])
      if (tenantId) {
        return tenantId
      }
    }
  }

  const runtimeTenantId = normalizeTenantId(runtimeConfig.defaultTenantId)
  if (runtimeTenantId) {
    return runtimeTenantId
  }
  return normalizeTenantId(readViteEnv('VITE_DEFAULT_TENANT_ID'))
}
