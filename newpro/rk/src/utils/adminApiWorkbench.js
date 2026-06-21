export function canAccessApiWorkbench(tenantId) {
  return String(tenantId || '') === '1'
}

export function normalizeApiWorkbenchResources(items) {
  return (Array.isArray(items) ? items : [])
    .filter((item) => item && item.name && item.url)
    .map((item) => ({
      id: `${item.name}::${item.url}`,
      name: item.name,
      url: item.url
    }))
}

export function normalizeApiWorkbenchEndpoints(items) {
  return (Array.isArray(items) ? items : [])
    .filter((item) => item && item.path && item.method)
    .map((item) => ({
      path: item.path,
      method: String(item.method).toUpperCase(),
      summary: item.summary || '',
      operationId: item.operationId || '',
      tag: item.tag || '',
      deprecated: Boolean(item.deprecated),
      consumes: Array.isArray(item.consumes) ? item.consumes : [],
      produces: Array.isArray(item.produces) ? item.produces : []
    }))
    .sort((a, b) => {
      const pathCompare = a.path.localeCompare(b.path)
      return pathCompare !== 0 ? pathCompare : a.method.localeCompare(b.method)
    })
}

export function buildApiWorkbenchUrl(path, queryText) {
  const normalizedPath = String(path || '').trim()
  if (!normalizedPath) {
    return ''
  }
  const query = parseWorkbenchJson(queryText, {})
  const params = new URLSearchParams()
  Object.entries(query).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return
    params.set(key, typeof value === 'object' ? JSON.stringify(value) : String(value))
  })
  const queryString = params.toString()
  return queryString ? `${normalizedPath}?${queryString}` : normalizedPath
}

export function parseWorkbenchJson(text, fallback = null) {
  const raw = String(text || '').trim()
  if (!raw) {
    return fallback
  }
  return JSON.parse(raw)
}
