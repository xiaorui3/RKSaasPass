/**
 * 用户API
 * 对应后端Controller: UserController.java
 * 基础路径: /users
 */
import request from '@/utils/request'
import { uploadFile as uploadManagedFile } from '@/api/common'

// ==================== 认证接口 ====================

/**
 * 用户登录
 * @param {Object} data - 登录信息
 * @param {string} data.username - 用户名
 * @param {string} data.password - 密码
 * @param {string} data.organizationId - 租户ID（社团/组织ID）
 * @returns {Promise} 登录结果
 */
export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

export function prepareEmailLogin(data) {
  return request({
    url: '/auth/email-login/prepare',
    method: 'post',
    data
  })
}

export function confirmEmailLogin(data) {
  return request({
    url: '/auth/email-login/confirm',
    method: 'post',
    data
  })
}

/**
 * 用户注册
 * @param {Object} data - 注册信息
 * @returns {Promise} 注册结果
 */
export function register(data) {
  return request({
    url: '/auth/register',
    method: 'post',
    data
  })
}

export function sendEmailVerificationCode(data) {
  return request({
    url: '/api/email-verification/send',
    method: 'post',
    data
  })
}

/**
 * 用户登出
 * @returns {Promise} 登出结果
 */
export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

/**
 * 刷新Token
 * @param {string} refreshToken - 刷新令牌
 * @returns {Promise} 新的Token信息
 */
export function refreshToken(refreshToken) {
  return request({
    url: '/auth/refresh',
    method: 'post',
    data: refreshToken
  })
}

export function getSwitchableTenants() {
  return request({
    url: '/auth/switchable-tenants',
    method: 'get'
  })
}

export function switchTenant(tenantId, roleId) {
  return request({
    url: `/auth/switch-tenant/${tenantId}`,
    method: 'post',
    params: {
      roleId
    }
  })
}

// ==================== 用户信息接口 ====================

/**
 * 获取当前用户信息
 * @returns {Promise} 用户信息
 */
export function getUserInfo() {
  return request({
    url: '/users/me',
    method: 'get'
  })
}

/**
 * 更新当前用户信息
 * @param {Object} data - 用户信息
 * @returns {Promise} 操作结果
 */
export function updateUserInfo(data) {
  return request({
    url: '/users',
    method: 'put',
    data
  })
}

/**
 * 根据ID获取用户信息
 * @param {number} id - 用户ID
 * @returns {Promise} 用户信息
 */
export function getUserById(id) {
  return request({
    url: `/users/${id}`,
    method: 'get'
  })
}

/**
 * 批量获取用户信息
 * @param {number[]} ids - 用户ID列表
 * @returns {Promise} 用户信息列表
 */
export function getUsersByIds(ids) {
  return request({
    url: '/users/list',
    method: 'get',
    params: { ids: ids.join(',') }
  })
}

export function getUsersByAuthIds(authUserIds) {
  return request({
    url: '/users/internal/by-auth-ids',
    method: 'get',
    params: { authUserIds: authUserIds.join(',') },
    silentError: true
  })
}

/**
 * 分页查询用户列表
 * @param {Object} params - 查询参数
 * @param {number} params.page - 页码
 * @param {number} params.size - 页大小
 * @param {string} params.username - 用户名（可选）
 * @param {string} params.mobile - 手机号（可选）
 * @param {number} params.status - 状态（可选）
 * @returns {Promise} 分页结果
 */
export function getUserPage(params) {
  const { page, size, ...rest } = params || {}
  return request({
    url: '/users/page/scoped',
    method: 'get',
    params: {
      ...rest,
      pageNo: page || 1,
      size: size || 10
    }
  })
}

export function getScopedUserStatistics(tenantId) {
  return request({
    url: '/users/statistics/scoped',
    method: 'get',
    params: {
      tenantId
    }
  })
}

export function reconcilePeopleDomain() {
  return request({
    url: '/users/reconcile/people-domain',
    method: 'post'
  })
}

/**
 * 删除用户
 * @param {number} id - 用户ID
 * @returns {Promise} 操作结果
 */
export function deleteUser(id) {
  return request({
    url: `/users/${id}`,
    method: 'delete'
  })
}

// ==================== 用户管理接口 ====================

/**
 * 新增用户
 * @param {Object} data - 用户信息
 * @returns {Promise} 用户ID
 */
export function createUser(data) {
  return request({
    url: '/users',
    method: 'post',
    data
  })
}

export function importUsers(data) {
  return request({
    url: '/users/import',
    method: 'post',
    data
  })
}

/**
 * 更新用户信息（管理员）
 * @param {number} id - 用户ID
 * @param {Object} data - 用户信息
 * @returns {Promise} 操作结果
 */
export function updateUser(id, data) {
  return request({
    url: `/users/${id}`,
    method: 'put',
    data
  })
}

/**
 * 重置用户密码
 * @param {number} id - 用户ID
 * @returns {Promise} 操作结果
 */
export function resetPassword(id) {
  return request({
    url: `/users/${id}/password/default`,
    method: 'put'
  })
}

/**
 * 修改用户状态
 * @param {number} id - 用户ID
 * @param {number} status - 状态：0-禁用 1-正常
 * @returns {Promise} 操作结果
 */
export function updateUserStatus(id, status) {
  return request({
    url: `/users/${id}/status/${status}`,
    method: 'put'
  })
}

/**
 * 检查手机号是否存在
 * @param {string} cellphone - 手机号
 * @returns {Promise} 是否存在
 */
export function checkCellphone(cellphone) {
  return request({
    url: '/users/checkCellphone',
    method: 'get',
    params: { cellphone }
  })
}

// ==================== 文件上传接口 ====================

/**
 * 上传头像
 * @param {File} file - 头像文件
 * @returns {Promise} 头像URL
 */
export function uploadAvatar(file) {
  return uploadManagedFile({ file }, 'avatar')
}

// ==================== 用户类型常量 ====================

/**
 * 用户类型
 */
export const USER_TYPE = {
  ADMIN: 1,     // 管理员
  STUDENT: 2,   // 学生
  TEACHER: 3    // 教师
}

/**
 * 用户状态
 */
export const USER_STATUS = {
  DISABLED: 0,  // 禁用
  NORMAL: 1     // 正常
}

/**
 * 性别
 */
export const GENDER = {
  MALE: 1,    // 男
  FEMALE: 2   // 女
}

/**
 * 获取用户类型文本
 * @param {number} type - 类型码
 * @returns {string} 类型文本
 */
export function getUserTypeText(type) {
  const typeMap = {
    [USER_TYPE.STUDENT]: '学生',
    [USER_TYPE.TEACHER]: '教师',
    [USER_TYPE.ADMIN]: '管理员'
  }
  return typeMap[type] || '未知'
}

/**
 * 获取用户状态文本
 * @param {number} status - 状态码
 * @returns {string} 状态文本
 */
export function getUserStatusText(status) {
  return status === USER_STATUS.NORMAL ? '正常' : '禁用'
}

/**
 * 获取性别文本
 * @param {number} gender - 性别码
 * @returns {string} 性别文本
 */
export function getGenderText(gender) {
  const genderMap = {
    [GENDER.MALE]: '男',
    [GENDER.FEMALE]: '女'
  }
  return genderMap[gender] || '未知'
}

// ==================== API对象导出（用于Profile.vue等组件） ====================

/**
 * 获取当前用户信息（getUserInfo的别名）
 */
export function getMyInfo() {
  return getUserInfo()
}

/**
 * 用户API对象
 */
export const userApi = {
  getMyInfo,
  getUserInfo,
  getUsersByAuthIds,
  updateUserInfo,
  createUser,
  updateUser,
  resetPassword,
  updateUserStatus,
  checkCellphone,
  login,
  prepareEmailLogin,
  confirmEmailLogin,
  register,
  sendEmailVerificationCode,
  logout,
  refreshToken
}

/**
 * 活动API对象（用户相关）
 */
export const activityApi = {
  // 用户活动记录
  getMyActivities: (page = 1, size = 10) => request({ url: '/api/activity/my', method: 'get', params: { page, size } }),
  getMyRegistrations: () => request({ url: '/api/activity/my/registrations', method: 'get' })
}

/**
 * 比赛API对象（用户相关）
 */
export const competitionApi = {
  // 用户比赛记录
  getMyCompetitions: (page = 1, size = 10) => request({ url: '/api/competition/my', method: 'get', params: { page, size } }),
  getMyRegistrations: () => request({ url: '/api/competition/my/registrations', method: 'get' })
}

/**
 * 上传API对象
 */
export const uploadApi = {
  uploadAvatar,
  // 通用上传
  upload: (file, target) => uploadManagedFile({ file }, target)
}
