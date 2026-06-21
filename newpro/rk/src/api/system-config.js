import request from '@/utils/request'

export function getSystemConfigList() {
  return request({
    url: '/admin/config/list',
    method: 'get'
  })
}

export function createSystemConfig(data) {
  return request({
    url: '/admin/config',
    method: 'post',
    data
  })
}

export function updateSystemConfig(id, data) {
  return request({
    url: `/admin/config/${id}`,
    method: 'put',
    data
  })
}

export function deleteSystemConfig(id) {
  return request({
    url: `/admin/config/${id}`,
    method: 'delete'
  })
}

export function toggleSystemConfig(id) {
  return request({
    url: `/admin/config/${id}/toggle`,
    method: 'put'
  })
}
