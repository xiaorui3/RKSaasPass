const ADMIN_THEME_ROLE_IDS = new Set([1, 3, 5, 7, 8])

export function shouldUseCurrentTheme(context, roleId) {
  return context === 'admin' && ADMIN_THEME_ROLE_IDS.has(Number(roleId || 0))
}
