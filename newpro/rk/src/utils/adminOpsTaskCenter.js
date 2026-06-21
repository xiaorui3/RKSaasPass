export function canAccessJenkinsOps(tenantId) {
  return String(tenantId || '') === '1'
}

function parseUrl(value) {
  try {
    return new URL(String(value || ''))
  } catch {
    return null
  }
}

function isPrivateHostname(hostname) {
  const value = String(hostname || '').trim().toLowerCase()
  if (!value) {
    return false
  }
  if (value === 'localhost' || value === '127.0.0.1' || value === '::1') {
    return true
  }
  if (/^10\.\d{1,3}\.\d{1,3}\.\d{1,3}$/.test(value)) {
    return true
  }
  if (/^192\.168\.\d{1,3}\.\d{1,3}$/.test(value)) {
    return true
  }
  return /^172\.(1[6-9]|2\d|3[0-1])\.\d{1,3}\.\d{1,3}$/.test(value)
}

export function resolveOpsConsoleUrl(rawUrl, options = {}) {
  const target = parseUrl(rawUrl)
  if (!target) {
    return ''
  }

  const current = parseUrl(options.currentOrigin)
  if (!current) {
    return rawUrl
  }

  if (target.origin === current.origin || target.hostname === current.hostname) {
    return rawUrl
  }

  if (isPrivateHostname(target.hostname) && !isPrivateHostname(current.hostname)) {
    return ''
  }

  return rawUrl
}

export function resolveJenkinsJobStatus(color) {
  const value = String(color || '').toLowerCase()
  if (value.endsWith('_anime')) {
    return 'running'
  }
  if (value.startsWith('blue') || value.startsWith('green')) {
    return 'success'
  }
  if (value.startsWith('red')) {
    return 'failed'
  }
  if (value.startsWith('disabled') || value.startsWith('grey') || value.startsWith('gray')) {
    return 'disabled'
  }
  return 'unknown'
}

export function normalizeJenkinsJobs(items) {
  return (Array.isArray(items) ? items : []).map((item) => ({
    ...item,
    status: resolveJenkinsJobStatus(item?.color)
  }))
}
