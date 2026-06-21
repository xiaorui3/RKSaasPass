/**
 * 公告/通知管理API
 * 对应后端Controller: NoticeController.java (rk-message服务)
 * 基础路径: /notifications/api/notices (通过网关路由)
 */
import request from '@/utils/request'

// 网关路由: /notifications/** -> StripPrefix=1 -> rk-message
// NoticeController: @RequestMapping("/api/notices")

/**
 * 获取通知列表
 * @returns {Promise} 通知列表
 */
export function getNoticeList() {
  return request({
    url: '/notifications/api/notices/list',
    method: 'get'
  })
}

export function getPublishedNoticeList() {
  return request({
    url: '/notifications/api/notices/published',
    method: 'get'
  })
}

export function getNoticeDetail(id) {
  return request({
    url: `/notifications/api/notices/${id}`,
    method: 'get'
  })
}

/**
 * 创建并发布公告
 * @param {Object} data - 公告数据
 * @param {string} data.title - 公告标题
 * @param {string} data.content - 公告内容
 * @param {string} data.noticeType - 公告类型
 * @returns {Promise} 操作结果
 */
export function createNotice(data) {
  return request({
    url: '/notifications/api/notices/create',
    method: 'post',
    data
  })
}

/**
 * 更新公告
 * @param {Object} data - 公告数据
 * @param {number} data.id - 公告ID
 * @returns {Promise} 操作结果
 */
export function updateNotice(data) {
  return request({
    url: '/notifications/api/notices/update',
    method: 'put',
    data
  })
}

/**
 * 删除公告
 * @param {number} id - 公告ID
 * @returns {Promise} 操作结果
 */
export function deleteNotice(id) {
  return request({
    url: `/notifications/api/notices/${id}`,
    method: 'delete'
  })
}

export function publishNotice(id) {
  return request({
    url: `/notifications/api/notices/${id}/publish`,
    method: 'put'
  })
}

export function withdrawNotice(id) {
  return request({
    url: `/notifications/api/notices/${id}/withdraw`,
    method: 'put'
  })
}

export function updateNoticeTop(id, isTop) {
  return request({
    url: `/notifications/api/notices/${id}/top`,
    method: 'put',
    params: { isTop }
  })
}

export function reviewNoticeByManager(id, data = {}) {
  return request({
    url: `/notifications/api/notices/${id}/review/manager`,
    method: 'post',
    data
  })
}

export function reviewNoticeByTeacher(id, data = {}) {
  return request({
    url: `/notifications/api/notices/${id}/review/teacher`,
    method: 'post',
    data
  })
}
