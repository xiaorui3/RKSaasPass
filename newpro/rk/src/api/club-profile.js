import request from '@/utils/request'

export function getCurrentClubProfileConfig() {
  return request({
    url: '/api/config/club-profile/current',
    method: 'get'
  })
}

export function saveCurrentClubProfileConfig(data) {
  return request({
    url: '/api/config/club-profile/current',
    method: 'put',
    data
  })
}

export function getPublicClubProfileConfig(tenantId) {
  return request({
    url: '/api/config/club-profile/public',
    method: 'get',
    params: { tenantId }
  })
}
