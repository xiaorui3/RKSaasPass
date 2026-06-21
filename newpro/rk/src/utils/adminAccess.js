export const SUPER_ADMIN_ROLE_IDS = [1]
export const TENANT_ADMIN_ROLE_IDS = [1, 3, 5]
export const MEMBER_ROLE_IDS = [2, 4, 6]
export const MANAGER_ROLE_IDS = [7]
export const TEACHER_ROLE_IDS = [8]
export const ADMIN_CONTENT_ROLE_IDS = [...new Set([...TENANT_ADMIN_ROLE_IDS, ...MANAGER_ROLE_IDS])]
export const ADMIN_BACKEND_ROLE_IDS = [...new Set([...ADMIN_CONTENT_ROLE_IDS, ...TEACHER_ROLE_IDS])]
export const SYSTEM_SCOPED_ADMIN_ROLE_IDS = [...new Set([...SUPER_ADMIN_ROLE_IDS, ...TENANT_ADMIN_ROLE_IDS])]

export function normalizeAdminPath(path) {
  if (!path) return ''
  const value = String(path).trim()
  if (!value) return ''
  if (value.startsWith('/admin')) {
    return value.replace(/\/+$/, '') || '/admin'
  }
  return `/admin/${value.replace(/^\/+/, '').replace(/\/+$/, '')}`
}

export function flattenAdminMenuPaths(rows = []) {
  if (!Array.isArray(rows)) {
    return []
  }
  return rows.flatMap((item) => {
    const currentPath = normalizeAdminPath(item?.path)
    const children = item?.subMenus || item?.children || []
    return [
      ...(currentPath ? [currentPath] : []),
      ...flattenAdminMenuPaths(children)
    ]
  })
}

export function isAdminRoleId(roleId) {
  return ADMIN_BACKEND_ROLE_IDS.includes(Number(roleId || 0))
}

export function hasAnyAdminMenu(adminMenusOrPaths = []) {
  const paths = adminMenusOrPaths.every?.((item) => typeof item === 'string')
    ? adminMenusOrPaths
    : flattenAdminMenuPaths(adminMenusOrPaths)
  return paths.some((path) => normalizeAdminPath(path).startsWith('/admin/'))
}

export function canEnterAdmin(roleId, adminMenusOrPaths = []) {
  return hasAnyAdminMenu(adminMenusOrPaths)
}

export function canAccessAdminRoute(path, roleId, adminMenusOrPaths = [], requiredRoles) {
  const normalized = normalizeAdminPath(path)
  if (!normalized) {
    return false
  }
  const paths = adminMenusOrPaths.every?.((item) => typeof item === 'string')
    ? adminMenusOrPaths.map(normalizeAdminPath)
    : flattenAdminMenuPaths(adminMenusOrPaths)
  if (normalized === '/admin') {
    return hasAnyAdminMenu(paths)
  }
  return paths.includes(normalized)
}
