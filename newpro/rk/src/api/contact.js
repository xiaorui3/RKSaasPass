import request from '@/utils/request'

export function fetchContactPublicShield() {
  return request({
    url: '/api/contact/public/shield',
    method: 'get'
  })
}

export function submitPublicContactMessage(data) {
  return request({
    url: '/api/contact/public/submit',
    method: 'post',
    data
  })
}

export function submitContact(data) {
  return request({
    url: '/api/contact/submit',
    method: 'post',
    data
  })
}

export const submitContactMessage = submitContact

export function submitContactForm(data) {
  return request({
    url: '/api/contact/submit-form',
    method: 'post',
    params: data
  })
}

export function getContactList() {
  return request({
    url: '/api/contact/list',
    method: 'get'
  })
}

export function getContactsByStatus(status) {
  return request({
    url: `/api/contact/status/${status}`,
    method: 'get'
  })
}

export function getContactDetail(id) {
  return request({
    url: `/api/contact/${id}`,
    method: 'get'
  })
}

export function replyContact(id, response) {
  return request({
    url: `/api/contact/${id}/reply`,
    method: 'post',
    params: { response }
  })
}

export function updateContactStatus(id, status) {
  return request({
    url: `/api/contact/${id}/status`,
    method: 'put',
    params: { status }
  })
}

export function deleteContact(id) {
  return request({
    url: `/api/contact/${id}`,
    method: 'delete'
  })
}

export function getContactStatistics() {
  return request({
    url: '/api/contact/statistics',
    method: 'get'
  })
}

export const CONTACT_STATUS = {
  PENDING: 'pending',
  PROCESSING: 'processing',
  REPLIED: 'replied',
  CLOSED: 'closed'
}

export function getStatusText(status) {
  const statusMap = {
    [CONTACT_STATUS.PENDING]: '待处理',
    [CONTACT_STATUS.PROCESSING]: '处理中',
    [CONTACT_STATUS.REPLIED]: '已回复',
    [CONTACT_STATUS.CLOSED]: '已关闭'
  }
  return statusMap[status] || '未知'
}

export function getStatusType(status) {
  const typeMap = {
    [CONTACT_STATUS.PENDING]: 'warning',
    [CONTACT_STATUS.PROCESSING]: 'primary',
    [CONTACT_STATUS.REPLIED]: 'success',
    [CONTACT_STATUS.CLOSED]: 'info'
  }
  return typeMap[status] || ''
}
