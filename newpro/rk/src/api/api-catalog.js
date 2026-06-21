import request from '@/utils/request'

export function getApiCatalog() {
  return request({
    url: '/api/api-catalog',
    method: 'get'
  })
}

export function debugApiCatalogEndpoint(data) {
  return request({
    url: '/api/api-catalog/debug',
    method: 'post',
    data
  })
}
