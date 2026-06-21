import request from '@/utils/request'

export function getMyVolunteerRecords(params) {
  return request({ url: '/api/volunteer/my', method: 'get', params })
}

export function submitVolunteerRecord(data) {
  return request({ url: '/api/volunteer', method: 'post', data })
}

export function getVolunteerRecords(params) {
  return request({ url: '/api/volunteer/records', method: 'get', params })
}

export function approveVolunteerRecord(id) {
  return request({ url: `/api/volunteer/${id}/approve`, method: 'put' })
}

export function rejectVolunteerRecord(id, reason) {
  return request({ url: `/api/volunteer/${id}/reject`, method: 'put', params: { reason } })
}
