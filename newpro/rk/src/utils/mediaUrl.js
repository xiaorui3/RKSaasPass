import { getMinioBaseUrl } from './runtimeConfig.js'

function trimSlash(value) {
  return typeof value === 'string' ? value.replace(/^\/+|\/+$/g, '') : ''
}

function looksLikeManagedMediaPath(value) {
  const normalized = trimSlash(value)
  if (!normalized || !normalized.includes('/')) {
    return false
  }
  const firstSegment = normalized.slice(0, normalized.indexOf('/'))
  return firstSegment.startsWith('rk-')
}

function looksLikeBucketRootObjectKey(value) {
  return typeof value === 'string'
    && /^[A-Za-z0-9][A-Za-z0-9._-]{7,}\.[A-Za-z0-9]{2,10}$/.test(value.trim())
}

function isAbsoluteUrl(value) {
  return typeof value === 'string' && /^(https?:)?\/\//.test(value)
}

function mediaBaseIncludesDefaultBucket(baseUrl) {
  if (typeof baseUrl !== 'string' || !baseUrl.trim()) {
    return false
  }
  try {
    const parsed = new URL(baseUrl, typeof window !== 'undefined' ? window.location.origin : 'http://localhost')
    return trimSlash(parsed.pathname).split('/').includes('rk-bucket')
  } catch {
    return trimSlash(baseUrl).split('/').includes('rk-bucket')
  }
}

function normalizeManagedStoragePath(value, options = {}) {
  const includeDefaultBucket = options.includeDefaultBucket === true
  const stripDefaultBucket = options.stripDefaultBucket === true
  let normalized = trimSlash(value)
  if (!normalized) {
    return ''
  }
  if (normalized.startsWith('minio-files/')) {
    normalized = normalized.slice('minio-files/'.length)
  }
  if (normalized.startsWith('rk-bucket/')) {
    return stripDefaultBucket ? normalized.slice('rk-bucket/'.length) : normalized
  }
  if (looksLikeManagedMediaPath(normalized)) {
    return normalized
  }
  if (looksLikeBucketRootObjectKey(normalized)) {
    return includeDefaultBucket ? `rk-bucket/${normalized}` : normalized
  }
  return ''
}

function extractNormalizedPath(value) {
  if (typeof value !== 'string') {
    return ''
  }
  const trimmed = value.trim()
  if (!trimmed) {
    return ''
  }
  if (!isAbsoluteUrl(trimmed)) {
    return trimSlash(trimmed)
  }
  try {
    return trimSlash(new URL(trimmed).pathname)
  } catch {
    return ''
  }
}

function isLegacyBrokenAvatarPath(value) {
  const normalized = extractNormalizedPath(value)
  if (!normalized) {
    return false
  }
  const withoutLegacyBucket = normalized.startsWith('rk-bucket/')
    ? normalized.slice('rk-bucket/'.length)
    : normalized
  return withoutLegacyBucket.startsWith('users/avatars/')
    || withoutLegacyBucket.startsWith('avatar/')
}

function isKnownBrokenLegacyMediaPath(value) {
  const normalized = extractNormalizedPath(value)
  if (!normalized) {
    return false
  }
  const withoutLegacyBucket = normalized.startsWith('rk-bucket/')
    ? normalized.slice('rk-bucket/'.length)
    : normalized
  return withoutLegacyBucket.startsWith('users/avatars/')
    || withoutLegacyBucket.startsWith('avatar/')
    || withoutLegacyBucket.startsWith('images/')
    || withoutLegacyBucket.startsWith('tmp/')
}

function joinUrl(baseUrl, relativePath) {
  const base = String(baseUrl || '').replace(/\/+$/, '')
  const path = trimSlash(relativePath)
  if (!base || !path) {
    return path || base
  }
  return `${base}/${path}`
}

export function resolveMediaUrl(value) {
  if (typeof value !== 'string') {
    return value
  }

  const trimmed = value.trim()
  if (!trimmed || trimmed.startsWith('/assets/') || trimmed.startsWith('data:') || trimmed.startsWith('blob:')) {
    return trimmed
  }

  if (isKnownBrokenLegacyMediaPath(trimmed)) {
    return ''
  }

  const minioBaseUrl = getMinioBaseUrl()
  if (!minioBaseUrl) {
    return trimmed
  }
  const baseIncludesDefaultBucket = mediaBaseIncludesDefaultBucket(minioBaseUrl)
  const pathOptions = {
    includeDefaultBucket: !baseIncludesDefaultBucket,
    stripDefaultBucket: baseIncludesDefaultBucket,
  }

  if (isAbsoluteUrl(trimmed)) {
    try {
      const parsed = new URL(trimmed)
      const relativePath = normalizeManagedStoragePath(parsed.pathname, pathOptions)
      if (!relativePath) {
        return trimmed
      }
      return joinUrl(minioBaseUrl, `${relativePath}${parsed.search}${parsed.hash}`)
    } catch {
      return trimmed
    }
  }

  const normalizedPath = normalizeManagedStoragePath(trimmed, pathOptions)
  if (!normalizedPath) {
    return trimmed
  }

  return joinUrl(minioBaseUrl, normalizedPath)
}

export function normalizeUserInfoMedia(userInfo) {
  if (!userInfo || typeof userInfo !== 'object') {
    return userInfo
  }

  const rawIcon = userInfo.icon || userInfo.avatar || ''
  const rawAvatar = userInfo.avatar || userInfo.icon || ''

  return {
    ...userInfo,
    icon: rawIcon && !isLegacyBrokenAvatarPath(rawIcon) ? resolveMediaUrl(rawIcon) : '',
    avatar: rawAvatar && !isLegacyBrokenAvatarPath(rawAvatar) ? resolveMediaUrl(rawAvatar) : '',
  }
}
