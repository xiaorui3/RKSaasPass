import request from '@/utils/request'

export function getCreditTypes() {
  return request({ url: '/api/credit/types', method: 'get' })
}

export function getMyCreditRecords(params) {
  return request({ url: '/api/credit/my/records', method: 'get', params })
}

export function getMyCreditSummary() {
  return request({ url: '/api/credit/my/summary', method: 'get' })
}

export function getMyCreditBreakdown() {
  return request({ url: '/api/credit/my/breakdown', method: 'get' })
}

export function getCreditRecords(params) {
  return request({ url: '/api/credit/records', method: 'get', params })
}

export function addCreditRecord(data) {
  return request({ url: '/api/credit/records', method: 'post', data })
}

export function saveCreditType(data) {
  return request({ url: '/api/credit/types', method: 'post', data })
}

export function deleteCreditType(id) {
  return request({ url: `/api/credit/types/${id}`, method: 'delete' })
}

export function approveCreditRecord(id) {
  return request({ url: `/api/credit/records/${id}/approve`, method: 'put' })
}

export function rejectCreditRecord(id, reason) {
  return request({ url: `/api/credit/records/${id}/reject`, method: 'put', params: { reason } })
}
