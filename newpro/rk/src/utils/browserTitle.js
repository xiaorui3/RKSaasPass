function normalizeTitlePart(value) {
  return String(value || '').trim()
}

function resolveTenantTitle({ isLoggedIn, tenantId, tenantDirectory }) {
  if (!isLoggedIn || !tenantId) {
    return 'RK-Web'
  }
  const currentTenant = Array.isArray(tenantDirectory)
    ? tenantDirectory.find((item) => String(item?.id) === String(tenantId))
    : null
  const tenantName = normalizeTitlePart(currentTenant?.tenantName)

  return tenantName || `租户${tenantId}`
}

export function resolveBrowserTabTitle({ isLoggedIn, tenantId, tenantDirectory, routeTitle = '' }) {
  const tenantTitle = resolveTenantTitle({ isLoggedIn, tenantId, tenantDirectory })
  const pageTitle = normalizeTitlePart(routeTitle)
  if (!pageTitle) {
    return tenantTitle
  }
  return pageTitle === tenantTitle ? tenantTitle : `${pageTitle} - ${tenantTitle}`
}

export function applyBrowserTabTitle(title) {
  if (typeof document === 'undefined') {
    return
  }

  document.title = title
}
