import request from '@/utils/request'

export function getDemoDataStatus() {
  return request({
    url: '/api/demo-data/status',
    method: 'get'
  })
}

export function generateDemoData() {
  return request({
    url: '/api/demo-data/generate',
    method: 'post'
  })
}

export function cleanupDemoData() {
  return request({
    url: '/api/demo-data/cleanup',
    method: 'post'
  })
}

export function resetDemoData() {
  return request({
    url: '/api/demo-data/reset',
    method: 'post'
  })
}
