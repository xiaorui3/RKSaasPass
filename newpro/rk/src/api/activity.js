/**
 * 活动管理API
 * 对应后端Controller: ActivityController.java
 * 基础路径: /api/activity
 */
import request from '@/utils/request'

// ==================== 查询接口 ====================

/**
 * 获取所有活动列表
 * @returns {Promise} 活动列表
 */
export function getActivityList() {
  return request({
    url: '/api/activity/list',
    method: 'get'
  })
}

/**
 * 分页获取活动列表
 * @param {Object} params - 查询参数
 * @param {number} params.page - 页码
 * @param {number} params.size - 每页数量
 * @param {number} params.status - 活动状态（可选）
 * @param {number} params.type - 活动类型（可选）
 * @returns {Promise} 分页数据
 */
export function getActivityPage(params) {
  return request({
    url: '/api/activity',
    method: 'get',
    params
  })
}

export function getAdminActivityPage(params) {
  return request({
    url: '/api/activity/admin/page',
    method: 'get',
    params
  })
}

export function getPendingReviewActivities(params) {
  return request({
    url: '/api/activity/review/pending',
    method: 'get',
    params
  })
}

/**
 * 获取活动详情
 * @param {number} id - 活动ID
 * @returns {Promise} 活动详情
 */
export function getActivityDetail(id) {
  return request({
    url: `/api/activity/${id}`,
    method: 'get'
  })
}

/**
 * 根据状态获取活动
 * @param {number} status - 活动状态：1-未开始 2-报名中 3-进行中 4-已结束
 * @returns {Promise} 活动列表
 */
export function getActivitiesByStatus(status) {
  return request({
    url: `/api/activity/status/${status}`,
    method: 'get'
  })
}

/**
 * 获取热门活动
 * @param {number} limit - 数量限制，默认10
 * @returns {Promise} 活动列表
 */
export function getHotActivities(limit = 10) {
  return request({
    url: '/api/activity/hot',
    method: 'get',
    params: { limit }
  })
}

/**
 * 获取推荐活动（置顶）
 * @param {number} limit - 数量限制，默认5
 * @returns {Promise} 活动列表
 */
export function getTopActivities(limit = 5) {
  return request({
    url: '/api/activity/top',
    method: 'get',
    params: { limit }
  })
}

export function getSharedActivities(limit = 10) {
  return request({
    url: '/api/activity/shared',
    method: 'get',
    params: { limit }
  })
}

/**
 * 搜索活动
 * @param {string} keyword - 搜索关键词
 * @returns {Promise} 活动列表
 */
export function searchActivities(keyword) {
  return request({
    url: '/api/activity/search',
    method: 'get',
    params: { keyword }
  })
}

/**
 * 获取活动统计信息
 * @returns {Promise} 统计数据
 */
export function getActivityStatistics() {
  return request({
    url: '/api/activity/statistics',
    method: 'get'
  })
}

// ==================== 管理接口 ====================

/**
 * 创建活动
 * @param {Object} data - 活动数据
 * @returns {Promise} 活动ID
 */
export function createActivity(data) {
  return request({
    url: '/api/activity/add',
    method: 'post',
    data
  })
}

export function submitActivity(data) {
  return request({
    url: '/api/activity/submit',
    method: 'post',
    data
  })
}

/**
 * 更新活动
 * @param {Object} data - 活动数据
 * @returns {Promise} 操作结果
 */
export function updateActivity(data) {
  return request({
    url: '/api/activity/update',
    method: 'put',
    data
  })
}

export function grantActivityCredits(id) {
  return request({
    url: `/api/activity/${id}/credits/grant`,
    method: 'post'
  })
}

export function getActivityAlbums(id) {
  return request({
    url: `/api/activity/${id}/albums`,
    method: 'get'
  })
}

export function createActivityAlbum(id, data) {
  return request({
    url: `/api/activity/${id}/albums`,
    method: 'post',
    data
  })
}

export function deleteActivityAlbum(photoId) {
  return request({
    url: `/api/activity/albums/${photoId}`,
    method: 'delete'
  })
}

export function reviewActivityByManager(id, data) {
  return request({
    url: `/api/activity/${id}/review/manager`,
    method: 'post',
    data
  })
}

export function reviewActivityByTeacher(id, data) {
  return request({
    url: `/api/activity/${id}/review/teacher`,
    method: 'post',
    data
  })
}

/**
 * 删除活动
 * @param {number} id - 活动ID
 * @returns {Promise} 操作结果
 */
export function deleteActivity(id) {
  return request({
    url: `/api/activity/delete/${id}`,
    method: 'delete'
  })
}

// ==================== 报名接口 ====================

/**
 * 活动报名
 * @param {number} id - 活动ID
 * @param {string} remark - 备注（可选）
 * @returns {Promise} 报名结果
 */
export function registerActivity(id, remark = '') {
  return request({
    url: `/api/activity/${id}/register`,
    method: 'post',
    params: { remark }
  })
}

/**
 * 取消报名
 * @param {number} id - 活动ID
 * @param {string} reason - 取消原因（可选）
 * @returns {Promise} 取消结果
 */
export function cancelRegistration(id, reason = '') {
  return request({
    url: `/api/activity/${id}/cancel`,
    method: 'post',
    params: { reason }
  })
}

/**
 * 活动签到
 * @param {number} id - 活动ID
 * @returns {Promise} 签到结果
 */
export function checkIn(id) {
  return request({
    url: `/api/activity/${id}/checkin`,
    method: 'post'
  })
}

/**
 * 获取活动的报名列表
 * @param {number} id - 活动ID
 * @returns {Promise} 报名列表
 */
export function getRegistrationList(id) {
  return request({
    url: `/api/activity/${id}/registrations`,
    method: 'get'
  })
}

export function getVoteActivities(params = {}) {
  return request({
    url: '/api/activity/votes/activities',
    method: 'get',
    params
  })
}

export function getVoteActivityRegistrations(activityId) {
  return request({
    url: `/api/activity/votes/activities/${activityId}/registrations`,
    method: 'get'
  })
}

export function getActivityVotes(params = {}) {
  return request({
    url: '/api/activity/votes',
    method: 'get',
    params
  })
}

export function createActivityVote(data) {
  return request({
    url: '/api/activity/votes',
    method: 'post',
    data
  })
}

export function closeActivityVote(id) {
  return request({
    url: `/api/activity/votes/${id}/close`,
    method: 'post'
  })
}

/**
 * 检查当前用户是否已报名
 * @param {number} id - 活动ID
 * @returns {Promise} { registered: boolean }
 */
export function checkRegistered(id) {
  return request({
    url: `/api/activity/${id}/registered`,
    method: 'get'
  })
}

// ==================== 活动状态常量 ====================

/**
 * 活动状态
 */
export const ACTIVITY_STATUS = {
  NOT_STARTED: 1,  // 未开始
  REGISTERING: 2,  // 报名中
  ONGOING: 3,      // 进行中
  ENDED: 4         // 已结束
}

/**
 * 活动类型
 */
export const ACTIVITY_TYPE = {
  LECTURE: 1,         // 讲座
  COMPETITION: 2,     // 竞赛
  TRAINING: 3,        // 培训
  ENTERTAINMENT: 4,   // 娱乐
  VOLUNTEER: 5,       // 志愿服务
  OTHER: 6            // 其他
}

/**
 * 报名状态
 */
export const REGISTRATION_STATUS = {
  REGISTERED: 1,   // 已报名
  CHECKED_IN: 2,   // 已签到
  CANCELLED: 3     // 已取消
}

/**
 * 获取状态文本
 * @param {number} status - 状态码
 * @returns {string} 状态文本
 */
export function getStatusText(status) {
  const statusMap = {
    [ACTIVITY_STATUS.NOT_STARTED]: '未开始',
    [ACTIVITY_STATUS.REGISTERING]: '报名中',
    [ACTIVITY_STATUS.ONGOING]: '进行中',
    [ACTIVITY_STATUS.ENDED]: '已结束'
  }
  return statusMap[status] || '未知'
}

/**
 * 获取类型文本
 * @param {number} type - 类型码
 * @returns {string} 类型文本
 */
export function getTypeText(type) {
  const typeMap = {
    [ACTIVITY_TYPE.LECTURE]: '讲座',
    [ACTIVITY_TYPE.COMPETITION]: '竞赛',
    [ACTIVITY_TYPE.TRAINING]: '培训',
    [ACTIVITY_TYPE.ENTERTAINMENT]: '娱乐',
    [ACTIVITY_TYPE.VOLUNTEER]: '志愿服务',
    [ACTIVITY_TYPE.OTHER]: '其他'
  }
  return typeMap[type] || '未知'
}

// ==================== 用户活动记录 ====================

/**
 * 获取用户参与的活动列表
 * @param {number} page - 页码
 * @param {number} size - 每页数量
 * @returns {Promise} 活动列表
 */
export function getMyActivities(page = 1, size = 10) {
  return request({
    url: '/api/activity/my',
    method: 'get',
    params: { page, size }
  })
}

/**
 * 获取用户报名的活动
 * @returns {Promise} 报名列表
 */
export function getMyRegistrations() {
  return request({
    url: '/api/activity/my/registrations',
    method: 'get'
  })
}
