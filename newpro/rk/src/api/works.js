import request from '@/utils/request'

/**
 * 作品展示API接口
 * 对应后端: rk-content/WorksController.java
 * 接口路径: /api/works
 */

// ==================== 查询接口 ====================

/**
 * 获取所有作品
 * @param {Object} params - 查询参数
 */
export function getWorksList(params) {
  return request({
    url: '/api/works/list',
    method: 'get',
    params
  })
}

/**
 * 按分类获取作品
 * @param {string} category - 分类名称
 */
export function getWorksByCategory(category) {
  return request({
    url: `/api/works/category/${category}`,
    method: 'get'
  })
}

/**
 * 获取精选作品
 */
export function getFeaturedWorks() {
  return request({
    url: '/api/works/featured',
    method: 'get'
  })
}

/**
 * 搜索作品
 * @param {string} title - 标题关键词
 * @param {string} category - 分类
 */
export function searchWorks(title, category) {
  return request({
    url: '/api/works/search',
    method: 'get',
    params: { title, category }
  })
}

/**
 * 获取作品详情
 * @param {number|string} id - 作品ID
 */
export function getWorkDetail(id) {
  return request({
    url: `/api/works/${id}`,
    method: 'get'
  })
}

/**
 * 获取热门作品
 */
export function getPopularWorks() {
  return request({
    url: '/api/works/popular',
    method: 'get'
  })
}

/**
 * 获取最新作品
 */
export function getLatestWorks() {
  return request({
    url: '/api/works/latest',
    method: 'get'
  })
}

/**
 * 获取作品统计
 */
export function getWorkStatistics() {
  return request({
    url: '/api/works/statistics',
    method: 'get'
  })
}

// ==================== 交互接口 ====================

/**
 * 点赞作品
 * @param {number|string} id - 作品ID
 */
export function likeWork(id) {
  return request({
    url: `/api/works/${id}/like`,
    method: 'post'
  })
}

/**
 * 取消点赞
 * @param {number|string} id - 作品ID
 */
export function unlikeWork(id) {
  return request({
    url: `/api/works/${id}/unlike`,
    method: 'post'
  })
}

// ==================== 管理接口 ====================

/**
 * 新增作品
 * @param {Object} data - 作品数据
 */
export function addWorks(data) {
  return request({
    url: '/api/works',
    method: 'post',
    data
  })
}

/**
 * 更新作品
 * @param {Object} data - 作品数据（需包含id）
 */
export function updateWorks(data) {
  return request({
    url: '/api/works',
    method: 'put',
    data
  })
}

/**
 * 删除作品
 * @param {number|string} id - 作品ID
 */
export function deleteWorks(id) {
  return request({
    url: `/api/works/${id}`,
    method: 'delete'
  })
}

/**
 * 批量删除作品
 * @param {Array<number|string>} ids - 作品ID数组
 */
export function deleteWorksBatch(ids) {
  return request({
    url: '/api/works/batch',
    method: 'delete',
    data: ids
  })
}

export function reviewWorkByManager(id, data = {}) {
  return request({
    url: `/api/works/${id}/review/manager`,
    method: 'post',
    data
  })
}

export function reviewWorkByTeacher(id, data = {}) {
  return request({
    url: `/api/works/${id}/review/teacher`,
    method: 'post',
    data
  })
}

/**
 * 导出作品列表
 * @param {Object} params - 查询参数
 */
export function exportWorks(params) {
  return request({
    url: '/ruoyi/works/export',
    method: 'post',
    params,
    responseType: 'blob'
  })
}
