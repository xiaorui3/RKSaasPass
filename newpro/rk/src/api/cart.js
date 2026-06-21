/**
 * 购物车API
 * 对应后端Controller: CartController.java
 * 基础路径: /carts
 */
import request from '@/utils/request'

// ==================== 购物车接口 ====================

/**
 * 添加课程到购物车
 * @param {Object} data - 购物车信息
 * @param {number} data.courseId - 课程ID
 * @returns {Promise} 操作结果
 */
export function addToCart(data) {
  return request({
    url: '/carts',
    method: 'post',
    data
  })
}

/**
 * 获取我的购物车
 * @returns {Promise} 购物车列表
 */
export function getMyCart() {
  return request({
    url: '/carts',
    method: 'get'
  })
}

/**
 * 删除购物车条目
 * @param {number} id - 购物车条目ID
 * @returns {Promise} 操作结果
 */
export function deleteCartItem(id) {
  return request({
    url: `/carts/${id}`,
    method: 'delete'
  })
}

/**
 * 批量删除购物车条目
 * @param {number[]} ids - 购物车条目ID列表
 * @returns {Promise} 操作结果
 */
export function deleteCartItems(ids) {
  return request({
    url: '/carts',
    method: 'delete',
    params: { ids: ids.join(',') }
  })
}

/**
 * 检查课程是否在购物车中
 * @param {number} courseId - 课程ID
 * @returns {Promise} 是否在购物车中
 */
export function checkInCart(courseId) {
  return request({
    url: '/carts/check',
    method: 'get',
    params: { courseId }
  })
}

/**
 * 获取购物车数量
 * @returns {Promise} 数量
 */
export function getCartCount() {
  return request({
    url: '/carts/count',
    method: 'get'
  })
}
