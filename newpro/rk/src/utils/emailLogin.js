const ROLE_NAME_MAP = {
  1: '超级管理员',
  2: '普通成员',
  3: '租户管理员',
  7: '社团负责人',
  8: '指导老师'
}

export function mapEmailLoginCandidates(candidates = [], tenantDirectory = []) {
  const tenantMap = new Map((tenantDirectory || []).map((item) => [String(item.id), item.tenantName]))

  return (candidates || []).map((item) => {
    const roleId = Number(item.roleId || 0)
    const tenantName = tenantMap.get(String(item.tenantId)) || `租户 ${item.tenantId}`
    const roleName = ROLE_NAME_MAP[roleId] || `角色 ${roleId || '-'}`
    const displayName = item.displayName || item.username || `账号 ${item.authUserId || ''}`.trim()

    return {
      ...item,
      tenantName,
      roleName,
      displayName,
      label: `${displayName} · ${tenantName} · ${roleName}`
    }
  })
}
