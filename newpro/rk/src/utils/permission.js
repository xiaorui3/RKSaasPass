/**
 * Frontend permission helpers.
 * Button visibility follows the same persisted menu paths as route access.
 */

import { useUserStore } from '@/stores/user'

const permissionPathMap = {
  'content:works:view': '/admin/content/works',
  'content:works:add': '/admin/content/works',
  'content:works:edit': '/admin/content/works',
  'content:works:remove': '/admin/content/works',

  'content:news:view': '/admin/content/news',
  'content:news:add': '/admin/content/news',
  'content:news:edit': '/admin/content/news',
  'content:news:remove': '/admin/content/news',
  'content:news:approve': '/admin/content/news-approval',
  'content:news:reject': '/admin/content/news-approval',

  'content:activity:view': '/admin/activity/list',
  'content:activity:add': '/admin/activity/list',
  'content:activity:edit': '/admin/activity/list',
  'content:activity:remove': '/admin/activity/list',

  'content:notice:view': '/admin/content/notices',
  'content:notice:add': '/admin/content/notices',
  'content:notice:edit': '/admin/content/notices',
  'content:notice:remove': '/admin/content/notices',
  'content:notice:publish': '/admin/content/notices',

  'content:competition:view': '/admin/activity/competition',
  'content:competition:add': '/admin/activity/competition',
  'content:competition:edit': '/admin/activity/competition',
  'content:competition:remove': '/admin/activity/competition',

  'system:user:view': '/admin/system/users',
  'system:user:add': '/admin/system/users',
  'system:user:edit': '/admin/system/users',
  'system:user:remove': '/admin/system/users',

  'system:role:view': '/admin/system/roles',
  'system:role:add': '/admin/system/roles',
  'system:role:edit': '/admin/system/roles',
  'system:role:remove': '/admin/system/roles',

  'system:tenant:view': '/admin/system/tenants',
  'system:tenant:add': '/admin/system/tenants',
  'system:tenant:edit': '/admin/system/tenants',
  'system:tenant:remove': '/admin/system/tenants',

  'system:config:view': '/admin/system/config',
  'system:config:edit': '/admin/system/config',
  'system:log:view': '/admin/operation/logs',

  'alumni:view': '/admin/club/alumni',
  'alumni:list': '/admin/club/alumni',
  'alumni:add': '/admin/club/alumni',
  'alumni:edit': '/admin/club/alumni',
  'alumni:remove': '/admin/club/alumni'
}

export function hasPermission(permission) {
  const userStore = useUserStore()
  const path = permissionPathMap[permission]
  if (!path) {
    console.warn(`[permission] unknown permission: ${permission}`)
    return false
  }
  return userStore.canAccessAdminPath(path)
}

export const permissionDirective = {
  mounted(el, binding) {
    const { value } = binding
    if (value && !hasPermission(value)) {
      el.parentNode?.removeChild(el)
    }
  }
}

export function usePermission() {
  return {
    hasPermission
  }
}
