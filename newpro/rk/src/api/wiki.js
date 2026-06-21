import request from '@/utils/request'

export function listWikiDocuments(params = {}) {
  return request({
    url: '/notifications/api/wiki/documents',
    method: 'get',
    params
  })
}

export function createWikiDocument(data) {
  return request({
    url: '/notifications/api/wiki/documents',
    method: 'post',
    data
  })
}

export function updateWikiDocument(id, data) {
  return request({
    url: `/notifications/api/wiki/documents/${id}`,
    method: 'put',
    data
  })
}

export function deleteWikiDocument(id) {
  return request({
    url: `/notifications/api/wiki/documents/${id}`,
    method: 'delete'
  })
}
