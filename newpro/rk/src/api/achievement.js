import request from '@/utils/request'

/**
 * 成就展示API接口
 * 对应数据库表: rk_member_achievement
 * 接口路径: /api/achievement
 */

// ==================== 查询接口 ====================

/**
 * 获取成就列表（分页）
 * @param {Object} params - 查询参数
 * @param {number} params.page - 页码
 * @param {number} params.size - 每页数量
 * @param {number} params.achievementType - 成就类型(1-比赛,2-项目,3-论文,4-专利)
 * @param {number} params.achievementLevel - 成就级别(1-国家级,2-省级,3-市级,4-校级)
 * @param {number} params.status - 审核状态(1-待审核,2-已通过,3-已拒绝)
 * @param {string} params.keyword - 搜索关键词
 */
export function getAchievementList(params) {
  return request({
    url: '/api/achievement/list',
    method: 'get',
    params
  })
}

/**
 * 获取成就详情
 * @param {number|string} id - 成就ID
 */
export function getAchievementDetail(id) {
  return request({
    url: `/api/achievement/${id}`,
    method: 'get'
  })
}

/**
 * 按类型获取成就
 * @param {number} type - 成就类型
 */
export function getAchievementsByType(type) {
  return request({
    url: `/api/achievement/type/${type}`,
    method: 'get'
  })
}

/**
 * 按级别获取成就
 * @param {number} level - 成就级别
 */
export function getAchievementsByLevel(level) {
  return request({
    url: `/api/achievement/level/${level}`,
    method: 'get'
  })
}

/**
 * 获取已通过的成就（公开展示）
 */
export function getPublicAchievements() {
  return request({
    url: '/api/achievement/public',
    method: 'get'
  })
}

/**
 * 搜索成就
 * @param {string} keyword - 搜索关键词
 */
export function searchAchievements(keyword) {
  return request({
    url: '/api/achievement/search',
    method: 'get',
    params: { keyword }
  })
}

/**
 * 获取成就统计
 */
export function getAchievementStatistics() {
  return request({
    url: '/api/achievement/statistics',
    method: 'get'
  })
}

/**
 * 获取当前用户的成就
 */
export function getMyAchievements() {
  return request({
    url: '/api/achievement/my',
    method: 'get'
  })
}

// ==================== 管理接口 ====================

/**
 * 新增成就
 * @param {Object} data - 成就数据
 */
export function addAchievement(data) {
  return request({
    url: '/api/achievement',
    method: 'post',
    data
  })
}

/**
 * 更新成就
 * @param {Object} data - 成就数据（需包含id）
 */
export function updateAchievement(data) {
  return request({
    url: '/api/achievement',
    method: 'put',
    data
  })
}

/**
 * 删除成就
 * @param {number|string} id - 成就ID
 */
export function deleteAchievement(id) {
  return request({
    url: `/api/achievement/${id}`,
    method: 'delete'
  })
}

/**
 * 批量删除成就
 * @param {Array<number|string>} ids - 成就ID数组
 */
export function deleteAchievementBatch(ids) {
  return request({
    url: '/api/achievement/batch',
    method: 'delete',
    data: ids
  })
}

// ==================== 审核接口 ====================

/**
 * 审核通过
 * @param {number|string} id - 成就ID
 * @param {string} remark - 审核备注
 */
export function approveAchievement(id, remark = '') {
  return request({
    url: `/api/achievement/${id}/approve`,
    method: 'post',
    params: { remark }
  })
}

export function reviewAchievementByManager(id, remark = '') {
  return request({
    url: `/api/achievement/${id}/review/manager`,
    method: 'post',
    params: { remark }
  })
}

export function reviewAchievementByTeacher(id, remark = '') {
  return request({
    url: `/api/achievement/${id}/review/teacher`,
    method: 'post',
    params: { remark }
  })
}

/**
 * 审核拒绝
 * @param {number|string} id - 成就ID
 * @param {string} remark - 拒绝原因
 */
export function rejectAchievement(id, remark) {
  return request({
    url: `/api/achievement/${id}/reject`,
    method: 'post',
    params: { remark }
  })
}

// ==================== 常量定义 ====================

/**
 * 成就类型
 */
export const ACHIEVEMENT_TYPE = {
  COMPETITION: 1,  // 比赛获奖
  PROJECT: 2,      // 项目成果
  PAPER: 3,        // 论文发表
  PATENT: 4        // 专利成果
}

/**
 * 成就级别
 */
export const ACHIEVEMENT_LEVEL = {
  NATIONAL: 1,   // 国家级
  PROVINCIAL: 2, // 省级
  CITY: 3,       // 市级
  SCHOOL: 4      // 校级
}

/**
 * 审核状态
 */
export const ACHIEVEMENT_STATUS = {
  PENDING: 1,   // 待审核
  APPROVED: 2,  // 已通过
  REJECTED: 3   // 已拒绝
}

/**
 * 获取成就类型文本
 * @param {number} type - 类型码
 * @returns {string} 类型文本
 */
export function getAchievementTypeText(type) {
  const typeMap = {
    [ACHIEVEMENT_TYPE.COMPETITION]: '比赛获奖',
    [ACHIEVEMENT_TYPE.PROJECT]: '项目成果',
    [ACHIEVEMENT_TYPE.PAPER]: '论文发表',
    [ACHIEVEMENT_TYPE.PATENT]: '专利成果'
  }
  return typeMap[type] || '其他'
}

/**
 * 获取成就级别文本
 * @param {number} level - 级别码
 * @returns {string} 级别文本
 */
export function getAchievementLevelText(level) {
  const levelMap = {
    [ACHIEVEMENT_LEVEL.NATIONAL]: '国家级',
    [ACHIEVEMENT_LEVEL.PROVINCIAL]: '省级',
    [ACHIEVEMENT_LEVEL.CITY]: '市级',
    [ACHIEVEMENT_LEVEL.SCHOOL]: '校级'
  }
  return levelMap[level] || '其他'
}

/**
 * 获取审核状态文本
 * @param {number} status - 状态码
 * @returns {string} 状态文本
 */
export function getAchievementStatusText(status) {
  const statusMap = {
    [ACHIEVEMENT_STATUS.PENDING]: '待审核',
    [ACHIEVEMENT_STATUS.APPROVED]: '已通过',
    [ACHIEVEMENT_STATUS.REJECTED]: '已拒绝'
  }
  return statusMap[status] || '未知'
}
