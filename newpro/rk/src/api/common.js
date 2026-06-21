import request from '@/utils/request'
import { resolveUploadTarget } from '@/utils/uploadTargets'

export function getNavigation() {
  return request({
    url: '/api/config/navigation',
    method: 'get'
  })
}

export function getBanners(params) {
  return request({
    url: '/api/banners',
    method: 'get',
    params
  })
}

export function getSystemConfig() {
  return request({
    url: '/api/config/system',
    method: 'get',
    silentError: true
  })
}

export function getNotifications(params) {
  return request({
    url: '/api/notifications',
    method: 'get',
    params
  })
}

export function getUnreadNotificationCount() {
  return request({
    url: '/api/notifications/unread-count',
    method: 'get',
    silentError: true
  })
}

export function markAsRead(notificationId) {
  return request({
    url: `/api/notifications/${notificationId}/read`,
    method: 'put'
  })
}

export function markAllAsRead() {
  return request({
    url: '/api/notifications/read-all',
    method: 'put'
  })
}

export function uploadFile(file, options) {
  const { service, bizType } = resolveUploadTarget(options)
  const formData = new FormData()
  formData.append('file', file)
  formData.append('service', service)
  formData.append('bizType', bizType)

  return request({
    url: '/api/files/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
