import request from '@/utils/request'

/**
 * 新闻管理API接口
 * 对应后端: rk-content/NewsController.java
 * 接口路径: /api/news
 */

// ==================== 查询接口 ====================

/**
 * 获取新闻列表（分页）
 * @param {Object} params - 查询参数
 * @param {number} params.page - 页码
 * @param {number} params.size - 每页数量
 * @param {string} params.category - 分类
 * @param {string} params.search - 搜索关键词
 */
export function getNewsList(params) {
  return request({
    url: '/api/news',
    method: 'get',
    params
  })
}

/**
 * 获取新闻详情
 * @param {number|string} id - 新闻ID
 */
export function getNewsDetail(id) {
  return request({
    url: `/api/news/${id}`,
    method: 'get'
  })
}

/**
 * 获取最新新闻
 * @param {number} limit - 数量限制，默认5
 */
export function getLatestNews(limit = 5) {
  return request({
    url: '/api/news/latest',
    method: 'get',
    params: { limit }
  })
}

/**
 * 获取置顶新闻
 * @param {number} limit - 数量限制，默认3
 */
export function getTopNews(limit = 3) {
  return request({
    url: '/api/news/top',
    method: 'get',
    params: { limit }
  })
}

export function getSharedNews(limit = 10) {
  return request({
    url: '/api/news/shared',
    method: 'get',
    params: { limit }
  })
}

/**
 * 根据分类获取新闻
 * @param {string} category - 分类名称
 * @param {number} limit - 数量限制，默认10
 */
export function getNewsByCategory(category, limit = 10) {
  return request({
    url: `/api/news/category/${category}`,
    method: 'get',
    params: { limit }
  })
}

/**
 * 搜索新闻
 * @param {string} keyword - 搜索关键词
 * @param {number} limit - 数量限制，默认20
 */
export function searchNews(keyword, limit = 20) {
  return request({
    url: '/api/news/search',
    method: 'get',
    params: { keyword, limit }
  })
}

/**
 * 获取新闻统计
 */
export function getNewsStatistics() {
  return request({
    url: '/api/news/statistics',
    method: 'get'
  })
}

// ==================== 管理接口 ====================

/**
 * 添加新闻
 * @param {Object} data - 新闻数据
 */
export function addNews(data) {
  return request({
    url: '/api/news/add',
    method: 'post',
    data
  })
}

/**
 * 更新新闻
 * @param {Object} data - 新闻数据（包含id）
 */
export function updateNews(data) {
  return request({
    url: '/api/news/update',
    method: 'put',
    data
  })
}

/**
 * 删除新闻
 * @param {number|string} id - 新闻ID
 */
export function deleteNews(id) {
  return request({
    url: `/api/news/delete/${id}`,
    method: 'delete'
  })
}

/**
 * 批量删除新闻
 * @param {Array<number>} ids - 新闻ID数组
 */
export function deleteNewsBatch(ids) {
  return request({
    url: '/api/news/delete',
    method: 'delete',
    data: ids
  })
}

/**
 * 导出新闻列表
 * @param {Object} params - 查询参数
 */
export function exportNews(params) {
  return request({
    url: '/ruoyi/news/export',
    method: 'post',
    params,
    responseType: 'blob'
  })
}

/**
 * 导入新闻数据
 * @param {FormData} formData - 包含文件的表单数据
 */
export function importNews(formData) {
  return request({
    url: '/ruoyi/news/importData',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 下载导入模板
 */
export function downloadNewsTemplate() {
  return request({
    url: '/ruoyi/news/importTemplate',
    method: 'post',
    responseType: 'blob'
  })
}

// ==================== 审核接口 ====================

/**
 * 获取待审核新闻列表
 */
export function getPendingNews() {
  return request({
    url: '/api/news/pending',
    method: 'get'
  })
}

/**
 * 审核通过新闻
 * @param {Object} data - 审核数据
 * @param {number} data.id - 新闻ID
 * @param {string} data.remark - 审核备注（可选）
 */
export function approveNews(data) {
  return request({
    url: '/api/news/approve',
    method: 'put',
    data
  })
}

export function reviewNewsByManager(id, data = {}) {
  return request({
    url: `/api/news/${id}/review/manager`,
    method: 'post',
    data
  })
}

export function reviewNewsByTeacher(id, data = {}) {
  return request({
    url: `/api/news/${id}/review/teacher`,
    method: 'post',
    data
  })
}

/**
 * 审核拒绝新闻
 * @param {Object} data - 拒绝数据
 * @param {number} data.id - 新闻ID
 * @param {string} data.rejectReason - 拒绝原因（必填）
 */
export function rejectNews(data) {
  return request({
    url: '/api/news/reject',
    method: 'put',
    data
  })
}
