import request from '@/utils/request'

/**
 * 校友管理API接口
 * 对应后端: rk-user/ClubAlumniController.java
 * 接口路径: /api/alumni
 */

// ==================== 查询接口 ====================

/**
 * 获取所有校友信息
 */
export function getAlumniList() {
  return request({
    url: '/api/alumni/list',
    method: 'get'
  })
}

/**
 * 根据ID获取校友信息
 * @param {number|string} id - 校友ID
 */
export function getAlumniById(id) {
  return request({
    url: `/api/alumni/${id}`,
    method: 'get'
  })
}

/**
 * 根据届数获取校友信息
 * @param {number} year - 届数年份
 */
export function getAlumniByGeneration(year) {
  return request({
    url: `/api/alumni/generation/${year}`,
    method: 'get'
  })
}

/**
 * 根据毕业状态获取校友信息
 * @param {string} graduationStatus - 毕业状态
 */
export function getAlumniByStatus(graduationStatus) {
  return request({
    url: `/api/alumni/status/${graduationStatus}`,
    method: 'get'
  })
}

/**
 * 根据部门获取校友信息
 * @param {string} department - 部门名称
 */
export function getAlumniByDepartment(department) {
  return request({
    url: `/api/alumni/department/${department}`,
    method: 'get'
  })
}

/**
 * 搜索校友
 * @param {string} keyword - 搜索关键词
 */
export function searchAlumni(keyword) {
  return request({
    url: '/api/alumni/search',
    method: 'get',
    params: { keyword }
  })
}

// ==================== 统计接口 ====================

/**
 * 获取毕业状态统计
 */
export function getAlumniStatistics() {
  return request({
    url: '/api/alumni/statistics',
    method: 'get'
  })
}

/**
 * 获取各届统计信息
 */
export function getGenerationStatistics() {
  return request({
    url: '/api/alumni/generation-statistics',
    method: 'get'
  })
}

/**
 * 获取按届数分组的校友数据
 */
export function getAlumniGroupedByGeneration() {
  return request({
    url: '/api/alumni/grouped-by-generation',
    method: 'get'
  })
}

/**
 * 获取前台公开展示的按届数分组校友数据
 */
export function getPublicAlumniGroupedByGeneration() {
  return request({
    url: '/api/alumni/show-grouped-by-generation',
    method: 'get'
  })
}

/**
 * 获取校友数据概览
 */
export function getAlumniOverview() {
  return request({
    url: '/api/alumni/overview',
    method: 'get'
  })
}

/**
 * 获取前台公开展示的校友数据概览
 */
export function getPublicAlumniOverview() {
  return request({
    url: '/api/alumni/show-overview',
    method: 'get'
  })
}

/**
 * 获取毕业状态分布
 */
export function getGraduationDistribution() {
  return request({
    url: '/api/alumni/graduation-distribution',
    method: 'get'
  })
}

// ==================== 公开校友资料表单 ====================

/**
 * 读取一次性校友资料表单
 * @param {string} token - 邮件中的一次性 token
 */
export function getAlumniProfileForm(token) {
  return request({
    url: `/api/alumni/profile-form/${encodeURIComponent(token)}`,
    method: 'get'
  })
}

/**
 * 提交一次性校友资料表单
 * @param {string} token - 邮件中的一次性 token
 * @param {Object} data - 校友补充资料
 */
export function submitAlumniProfileForm(token, data) {
  return request({
    url: `/api/alumni/profile-form/${encodeURIComponent(token)}`,
    method: 'post',
    data
  })
}

// ==================== 管理接口 ====================

/**
 * 添加校友信息
 * @param {Object} data - 校友数据
 */
export function addAlumni(data) {
  return request({
    url: '/api/alumni/add',
    method: 'post',
    data
  })
}

/**
 * 更新校友信息
 * @param {Object} data - 校友数据（包含id）
 */
export function updateAlumni(data) {
  return request({
    url: '/api/alumni/update',
    method: 'put',
    data
  })
}

/**
 * 删除校友信息
 * @param {number|string} id - 校友ID
 */
export function deleteAlumni(id) {
  return request({
    url: `/api/alumni/delete/${id}`,
    method: 'delete'
  })
}

/**
 * 批量更新毕业状态
 */
export function getDeletedAlumni() {
  return request({
    url: '/api/alumni/deleted',
    method: 'get'
  })
}

export function restoreAlumni(id) {
  return request({
    url: `/api/alumni/${id}/restore`,
    method: 'post'
  })
}

export function batchUpdateGraduationStatus() {
  return request({
    url: '/api/alumni/update-graduation-status',
    method: 'post'
  })
}

/**
 * 计算毕业状态
 * @param {Object} params - 参数
 * @param {number} params.enrollmentYear - 入学年份
 * @param {number} params.generationYear - 届数年份
 * @param {string} params.gradeClass - 班级
 */
export function calculateGraduationStatus(params) {
  return request({
    url: '/api/alumni/calculate-graduation-status',
    method: 'post',
    params
  })
}

/**
 * Excel导入校友信息
 * @param {File} file - Excel文件
 */
export function importAlumniExcel(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/api/alumni/import-excel',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
