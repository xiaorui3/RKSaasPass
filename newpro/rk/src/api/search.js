/**
 * 搜索相关API
 * 对应后端Controller: CourseController.java, InterestsController.java, RecommendController.java
 * 基础路径: /courses/portal, /interests, /recommend
 */
import request from '@/utils/request'

// ==================== 全站统一搜索接口 ====================

/**
 * 全站统一搜索，后端由 rk-search 通过 Elasticsearch 查询。
 * @param {Object} params
 * @param {string} params.keyword 搜索关键词
 * @param {string[]|string} params.entityTypes 实体类型过滤
 * @param {number} params.pageNo 页码
 * @param {number} params.pageSize 每页数量
 */
export function searchGlobal(params) {
  return request({
    url: '/api/search/global',
    method: 'get',
    params
  })
}

// ==================== 课程搜索接口 ====================

/**
 * 用户端课程搜索
 * @param {Object} params - 搜索参数
 * @param {string} params.keyword - 搜索关键词
 * @param {number} params.categoryId - 分类ID（可选）
 * @param {string} params.sortBy - 排序字段
 * @param {string} params.sortOrder - 排序方式 asc/desc
 * @param {number} params.pageNo - 页码
 * @param {number} params.pageSize - 每页数量
 * @returns {Promise} 课程列表
 */
export function searchCourses(params) {
  return request({
    url: '/courses/portal',
    method: 'get',
    params
  })
}

/**
 * 根据关键词查询课程ID
 * @param {string} keyword - 关键词
 * @returns {Promise} 课程ID列表
 */
export function getCourseIdsByKeyword(keyword) {
  return request({
    url: '/courses/name',
    method: 'get',
    params: { keyword }
  })
}

/**
 * 处理课程上架失败
 * @param {number[]} courseIds - 课程ID列表
 * @returns {Promise} 操作结果
 */
export function handleCoursesUp(courseIds) {
  return request({
    url: '/courses/up',
    method: 'post',
    data: { courseIds }
  })
}

/**
 * 处理课程下架失败
 * @param {number[]} courseIds - 课程ID列表
 * @returns {Promise} 操作结果
 */
export function handleCoursesDown(courseIds) {
  return request({
    url: '/courses/down',
    method: 'post',
    data: { courseIds }
  })
}

// ==================== 兴趣爱好接口 ====================

/**
 * 新增兴趣爱好
 * @param {Object} data - 兴趣信息
 * @param {number} data.categoryId - 分类ID
 * @returns {Promise} 操作结果
 */
export function addInterest(data) {
  return request({
    url: '/interests',
    method: 'post',
    data
  })
}

/**
 * 查询我的兴趣爱好
 * @returns {Promise} 兴趣列表
 */
export function getMyInterests() {
  return request({
    url: '/interests',
    method: 'get'
  })
}

/**
 * 根据分类ID查询课程TOP10
 * @param {number} id - 分类ID
 * @returns {Promise} 课程列表
 */
export function getTop10CoursesByCategory(id) {
  return request({
    url: `/interests/${id}/courses`,
    method: 'get'
  })
}

/**
 * 删除兴趣爱好
 * @param {number} id - 兴趣ID
 * @returns {Promise} 操作结果
 */
export function deleteInterest(id) {
  return request({
    url: `/interests/${id}`,
    method: 'delete'
  })
}

// ==================== 推荐课程接口 ====================

/**
 * 精品好课
 * @param {number} n - 数量限制
 * @returns {Promise} 课程列表
 */
export function getBestCourses(n = 10) {
  return request({
    url: '/recommend/best',
    method: 'get',
    params: { n }
  })
}

/**
 * 新课推荐
 * @param {number} n - 数量限制
 * @returns {Promise} 课程列表
 */
export function getNewCourses(n = 10) {
  return request({
    url: '/recommend/new',
    method: 'get',
    params: { n }
  })
}

/**
 * 精品公开课（免费）
 * @param {number} n - 数量限制
 * @returns {Promise} 课程列表
 */
export function getFreeCourses(n = 10) {
  return request({
    url: '/recommend/free',
    method: 'get',
    params: { n }
  })
}
