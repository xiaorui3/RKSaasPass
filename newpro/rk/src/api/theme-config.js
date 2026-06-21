import request from '@/utils/request'

export function getCurrentThemeConfig() {
  return request({
    url: '/api/config/theme/current',
    method: 'get'
  })
}

export function saveCurrentThemeConfig(data) {
  return request({
    url: '/api/config/theme/current',
    method: 'put',
    data
  })
}

export function getPublicThemeConfig(tenantId) {
  return request({
    url: '/api/config/theme/public',
    method: 'get',
    params: { tenantId }
  })
}
