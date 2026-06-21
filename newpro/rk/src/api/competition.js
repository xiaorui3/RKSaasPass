import request from '@/utils/request'

/**
 * 比赛管理API接口
 * 对应后端: rk-activity/CompetitionController.java
 * 接口路径: /api/competition
 */

// ==================== 查询接口 ====================

/**
 * 获取所有比赛
 */
export function getCompetitionList() {
  return request({
    url: '/api/competition/list',
    method: 'get'
  })
}

/**
 * 获取已发布的比赛
 * @param {string} sortBy - 排序字段，默认create_time
 * @param {string} sortOrder - 排序方式，默认desc
 */
export function getPublishedCompetitions(sortBy = 'create_time', sortOrder = 'desc') {
  return request({
    url: '/api/competition/published',
    method: 'get',
    params: { sortBy, sortOrder }
  })
}

/**
 * 获取推荐比赛
 */
export function getFeaturedCompetitions() {
  return request({
    url: '/api/competition/featured',
    method: 'get'
  })
}

export function getSharedCompetitions() {
  return request({
    url: '/api/competition/shared',
    method: 'get'
  })
}

/**
 * 根据状态获取比赛
 * @param {string} status - 比赛状态
 */
export function getCompetitionsByStatus(status) {
  return request({
    url: `/api/competition/status/${status}`,
    method: 'get'
  })
}

/**
 * 获取比赛详情
 * @param {number|string} id - 比赛ID
 */
export function getCompetitionDetail(id) {
  return request({
    url: `/api/competition/${id}`,
    method: 'get'
  })
}

/**
 * 搜索比赛
 * @param {string} keyword - 搜索关键词
 */
export function searchCompetitions(keyword) {
  return request({
    url: '/api/competition/search',
    method: 'get',
    params: { keyword }
  })
}

// ==================== 管理接口 ====================

/**
 * 创建比赛
 * @param {Object} data - 比赛数据
 */
export function createCompetition(data) {
  return request({
    url: '/api/competition',
    method: 'post',
    data
  })
}

/**
 * 更新比赛
 * @param {number|string} id - 比赛ID
 * @param {Object} data - 比赛数据
 */
export function updateCompetition(id, data) {
  return request({
    url: `/api/competition/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除比赛
 * @param {number|string} id - 比赛ID
 */
export function deleteCompetition(id) {
  return request({
    url: `/api/competition/${id}`,
    method: 'delete'
  })
}

// ==================== 报名接口 ====================

/**
 * 比赛报名
 * @param {number|string} id - 比赛ID
 * @param {Object} data - 报名信息
 * @param {string} data.name - 姓名
 * @param {string} data.studentId - 学号
 * @param {string} data.phone - 手机号
 * @param {string} data.email - 邮箱
 * @param {string} data.teamName - 队伍名称（团队赛必填）
 * @param {Array} data.members - 队员信息（团队赛）
 * @param {string} data.remark - 备注
 */
export function registerCompetition(id, data) {
  return request({
    url: `/api/competition/${id}/register`,
    method: 'post',
    data
  })
}

/**
 * 取消报名
 * @param {number|string} id - 比赛ID
 * @param {string} reason - 取消原因
 */
export function cancelRegistration(id, reason) {
  return request({
    url: `/api/competition/${id}/register/cancel`,
    method: 'post',
    data: { reason }
  })
}

export function adminCancelCompetitionRegistration(id, registrationId, reason) {
  return request({
    url: `/api/competition/${id}/registrations/${registrationId}/cancel`,
    method: 'post',
    data: { reason }
  })
}

/**
 * 获取比赛报名列表
 * @param {number|string} id - 比赛ID
 */
export function getRegistrationList(id) {
  return request({
    url: `/api/competition/${id}/registrations`,
    method: 'get'
  })
}

export function getPendingCompetitionReviewPage(params) {
  return request({
    url: '/api/competition/review/pending',
    method: 'get',
    params
  })
}

export function reviewCompetitionByTeacher(id, data) {
  return request({
    url: `/api/competition/${id}/review/teacher`,
    method: 'post',
    data
  })
}

export function reviewCompetitionByManager(id, data) {
  return request({
    url: `/api/competition/${id}/review/manager`,
    method: 'post',
    data
  })
}

/**
 * 获取比赛成绩列表
 * @param {number|string} id - 比赛ID
 */
export function getCompetitionResults(id) {
  return request({
    url: `/api/competition/${id}/results`,
    method: 'get'
  })
}

/**
 * 保存比赛成绩
 * @param {number|string} id - 比赛ID
 * @param {Array<Object>} data - 成绩列表
 */
export function saveCompetitionResults(id, data) {
  return request({
    url: `/api/competition/${id}/results`,
    method: 'post',
    data
  })
}

export function grantCompetitionCredits(id) {
  return request({
    url: `/api/competition/${id}/results/credits/grant`,
    method: 'post'
  })
}

/**
 * 检查当前用户是否已报名
 * @param {number|string} id - 比赛ID
 */
export function checkRegistered(id) {
  return request({
    url: `/api/competition/${id}/registered`,
    method: 'get'
  })
}

/**
 * 获取当前用户的报名详情
 * @param {number|string} id - 比赛ID
 */
export function getMyRegistration(id) {
  return request({
    url: `/api/competition/${id}/register/my`,
    method: 'get'
  })
}

/**
 * 导出比赛列表
 * @param {Object} params - 查询参数
 */
export function exportCompetitions(params) {
  return request({
    url: '/system/competition/export',
    method: 'post',
    params,
    responseType: 'blob'
  })
}

// ==================== 用户比赛记录 ====================

/**
 * 获取用户参与的比赛列表
 * @param {number} page - 页码
 * @param {number} size - 每页数量
 * @returns {Promise} 比赛列表
 */
export function getMyCompetitions(page = 1, size = 10) {
  return request({
    url: '/api/competition/my',
    method: 'get',
    params: { page, size }
  })
}

/**
 * 获取用户报名的比赛
 * @returns {Promise} 报名列表
 */
export function getMyCompetitionRegistrations() {
  return request({
    url: '/api/competition/my/registrations',
    method: 'get'
  })
}
