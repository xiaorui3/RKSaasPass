import request from '@/utils/request'

export function getComments(targetType, targetId) {
  return request({
    url: '/api/comments',
    method: 'get',
    params: { targetType, targetId },
    silentError: true
  })
}

export function createComment(data) {
  return request({
    url: '/api/comments',
    method: 'post',
    data
  })
}
