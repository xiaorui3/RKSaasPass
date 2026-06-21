import request from '@/utils/request'

/**
 * 内推码管理API接口
 * 对应后端: rk-user/ReferralCodeController.java
 * 接口路径: /api/referral-codes
 */

// ==================== 查询接口 ====================

/**
 * 分页查询内推码列表
 * @param {Object} params - 查询参数
 * @param {number} params.page - 当前页码
 * @param {number} params.size - 每页大小
 * @param {string} params.code - 内推码（模糊查询）
 * @param {number} params.generatorId - 生成者ID
 * @param {number} params.status - 状态: 1-有效 0-失效
 * @param {string} params.sortBy - 排序字段
 * @param {string} params.sortOrder - 排序方向
 */
export function getReferralCodes(params) {
  return request({
    url: '/api/referral-codes',
    method: 'get',
    params
  })
}

/**
 * 获取内推码详情
 * @param {number|string} id - 内推码ID
 */
export function getReferralCodeById(id) {
  return request({
    url: `/api/referral-codes/${id}`,
    method: 'get'
  })
}

// ==================== 生成接口 ====================

/**
 * 生成内推码
 * @param {Object} data - 内推码数据
 * @param {string} data.code - 内推码
 * @param {number} data.maxUses - 最大使用次数
 * @param {string} data.expiresAt - 过期时间
 * @param {number} data.status - 状态: 1-有效 0-失效
 */
export function createReferralCode(data) {
  return request({
    url: '/api/referral-codes',
    method: 'post',
    data
  })
}

/**
 * 生成随机内推码
 */
export function generateUniqueCode() {
  return request({
    url: '/api/referral-codes/generate-code',
    method: 'get'
  })
}

// ==================== 更新接口 ====================

/**
 * 更新内推码
 * @param {number|string} id - 内推码ID
 * @param {Object} data - 更新数据
 * @param {number} data.maxUses - 最大使用次数
 * @param {string} data.expiresAt - 过期时间
 * @param {number} data.status - 状态: 1-有效 0-失效
 */
export function updateReferralCode(id, data) {
  return request({
    url: `/api/referral-codes/${id}`,
    method: 'put',
    data
  })
}

// ==================== 删除接口 ====================

/**
 * 删除内推码
 * @param {number|string} id - 内推码ID
 */
export function deleteReferralCode(id) {
  return request({
    url: `/api/referral-codes/${id}`,
    method: 'delete'
  })
}

// ==================== 验证接口 ====================

/**
 * 验证内推码
 * @param {string} code - 内推码
 */
export function validateReferralCode(code) {
  return request({
    url: '/api/referral-codes/validate',
    method: 'get',
    params: { code }
  })
}

export function getReferralConversionOverview(id) {
  return request({
    url: `/api/referral-codes/${id}/conversions`,
    method: 'get'
  })
}

// ==================== 当前用户接口 ====================

/**
 * 获取当前用户的内推码
 */
export function getMyReferralCode() {
  return request({
    url: '/api/referral-codes/current',
    method: 'get'
  })
}

/**
 * 刷新当前用户的内推码
 */
export function refreshMyReferralCode() {
  return request({
    url: '/api/referral-codes/current/refresh',
    method: 'post'
  })
}
