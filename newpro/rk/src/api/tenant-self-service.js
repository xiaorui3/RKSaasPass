import request from '@/utils/request'

export function getTenantSelfServiceConfig() {
  return request({
    url: '/api/config/tenant-self-service/current',
    method: 'get'
  })
}

export function getPublicTenantSelfServiceConfig(tenantId) {
  return request({
    url: '/api/config/tenant-self-service/public',
    method: 'get',
    params: tenantId ? { tenantId } : {}
  })
}

export function saveTenantSelfServiceConfig(data) {
  return request({
    url: '/api/config/tenant-self-service/current',
    method: 'put',
    data
  })
}
