import request from '@/utils/request'

export function getAdminNotificationOverview(params = {}) {
  return request({
    url: '/api/admin/notifications/overview',
    method: 'get',
    params
  })
}

export function getNotificationDeliveryConfig(params = {}) {
  return request({
    url: '/api/admin/notifications/delivery-config',
    method: 'get',
    params
  })
}

export function saveNotificationDeliveryConfig(data) {
  return request({
    url: '/api/admin/notifications/delivery-config',
    method: 'post',
    data
  })
}

export function listAdminNotifications(params = {}) {
  return request({
    url: '/api/admin/notifications',
    method: 'get',
    params
  })
}

export function saveAdminNotification(data) {
  return request({
    url: '/api/admin/notifications',
    method: 'post',
    data
  })
}

export function deleteAdminNotification(notificationId) {
  return request({
    url: `/api/admin/notifications/${notificationId}/delete`,
    method: 'post'
  })
}
