import request from '@/utils/request'

/**
 * 获取公开的租户列表（无需登录）
 * 用于登录和注册页面选择租户
 */
export function getPublicTenantList() {
  return request({
    url: '/tenants/list',
    method: 'get'
  })
}

export function getTenantList(params) {
  const { page, size, ...rest } = params || {}
  return request({
    url: '/admin/tenants',
    method: 'get',
    params: {
      ...rest,
      pageNum: page || 1,
      pageSize: size || 10
    }
  })
}

export function getTenantDetail(tenantId) {
  return request({
    url: `/admin/tenants/${tenantId}`,
    method: 'get'
  })
}

export function createTenant(data) {
  return request({
    url: '/admin/tenants',
    method: 'post',
    data
  })
}

export function updateTenant(data) {
  return request({
    url: `/admin/tenants/${data.id}`,
    method: 'put',
    data
  })
}

export function updateTenantStatus(tenantId, status) {
  return request({
    url: `/admin/tenants/${tenantId}/status`,
    method: 'put',
    data: { status }
  })
}

export function deleteTenant(tenantId) {
  return request({
    url: `/admin/tenants/${tenantId}`,
    method: 'delete'
  })
}

export function getTenantInitStatus(tenantId) {
  return request({
    url: `/admin/tenants/${tenantId}/init-status`,
    method: 'get'
  })
}

export function reinitTenant(tenantId) {
  return request({
    url: `/admin/tenants/${tenantId}/reinit`,
    method: 'post'
  })
}

export function repairTenant(tenantId) {
  return request({
    url: `/admin/tenants/${tenantId}/repair`,
    method: 'post'
  })
}
