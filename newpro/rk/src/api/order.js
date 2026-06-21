/**
 * 订单管理API
 * 对应后端Controller: OrderController.java, OrderDetailController.java
 * 基础路径: /orders, /order-details
 */
import request from '@/utils/request'

// ==================== 订单管理接口 ====================

/**
 * 分页查询我的订单
 * @param {Object} params - 查询参数
 * @param {number} params.pageNo - 页码
 * @param {number} params.pageSize - 每页数量
 * @param {number} params.status - 订单状态（可选）
 * @returns {Promise} 分页数据
 */
export function getMyOrderPage(params) {
  return request({
    url: '/orders/page',
    method: 'get',
    params
  })
}

/**
 * 根据ID查询订单详情
 * @param {number} id - 订单ID
 * @returns {Promise} 订单详情
 */
export function getOrderById(id) {
  return request({
    url: `/orders/${id}`,
    method: 'get'
  })
}

/**
 * 查询订单支付状态
 * @param {number} id - 订单ID
 * @returns {Promise} 支付状态
 */
export function getOrderStatus(id) {
  return request({
    url: `/orders/${id}/status`,
    method: 'get'
  })
}

/**
 * 预下单接口
 * @param {Object} data - 预下单信息
 * @param {number[]} data.courseIds - 课程ID列表
 * @returns {Promise} 预下单信息
 */
export function prePlaceOrder(data) {
  return request({
    url: '/orders/prePlaceOrder',
    method: 'get',
    params: data
  })
}

/**
 * 下单接口
 * @param {Object} data - 订单信息
 * @returns {Promise} 订单ID
 */
export function placeOrder(data) {
  return request({
    url: '/orders/placeOrder',
    method: 'post',
    data
  })
}

/**
 * 免费课立刻报名
 * @param {number} courseId - 课程ID
 * @returns {Promise} 报名结果
 */
export function enrollFreeCourse(courseId) {
  return request({
    url: `/orders/freeCourse/${courseId}`,
    method: 'post'
  })
}

/**
 * 取消订单
 * @param {number} id - 订单ID
 * @returns {Promise} 操作结果
 */
export function cancelOrder(id) {
  return request({
    url: `/orders/${id}/cancel`,
    method: 'put'
  })
}

/**
 * 删除订单
 * @param {number} id - 订单ID
 * @returns {Promise} 操作结果
 */
export function deleteOrder(id) {
  return request({
    url: `/orders/${id}`,
    method: 'delete'
  })
}

// ==================== 订单明细接口 ====================

/**
 * 分页查询订单明细
 * @param {Object} params - 查询参数
 * @returns {Promise} 分页数据
 */
export function getOrderDetailPage(params) {
  return request({
    url: '/order-details/page',
    method: 'get',
    params
  })
}

/**
 * 获取订单明细详情和学习进度
 * @param {number} id - 明细ID
 * @returns {Promise} 明细详情
 */
export function getOrderDetailProgress(id) {
  return request({
    url: `/order-details/${id}`,
    method: 'get'
  })
}

/**
 * 校验课程是否购买
 * @param {number} courseId - 课程ID
 * @returns {Promise} 购买信息
 */
export function checkCourseOrder(courseId) {
  return request({
    url: `/order-details/course/${courseId}`,
    method: 'get'
  })
}

/**
 * 统计课程报名人数
 * @param {number[]} courseIds - 课程ID列表
 * @returns {Promise} 报名人数
 */
export function countCourseEnrollNum(courseIds) {
  return request({
    url: '/order-details/enrollNum',
    method: 'get',
    params: { courseIds: courseIds.join(',') }
  })
}

/**
 * 统计学生报名课程数量
 * @param {number} studentId - 学生ID
 * @returns {Promise} 报名课程数量
 */
export function countStudentEnrollCourse(studentId) {
  return request({
    url: '/order-details/enrollCourse',
    method: 'get',
    params: { studentId }
  })
}

/**
 * 获取课程购买信息
 * @param {number} courseId - 课程ID
 * @returns {Promise} 购买信息
 */
export function getCoursePurchaseInfo(courseId) {
  return request({
    url: '/order-details/purchaseInfo',
    method: 'get',
    params: { courseId }
  })
}

// ==================== 订单状态常量 ====================

/**
 * 订单状态
 */
export const ORDER_STATUS = {
  NO_PAY: 1,       // 未支付
  PAYING: 2,       // 支付中
  PAY_SUCCESS: 3,  // 支付成功
  PAY_FAILED: 4,   // 支付失败
  CANCELLED: 5     // 已取消
}

/**
 * 获取订单状态文本
 * @param {number} status - 状态码
 * @returns {string} 状态文本
 */
export function getOrderStatusText(status) {
  const statusMap = {
    [ORDER_STATUS.NO_PAY]: '未支付',
    [ORDER_STATUS.PAYING]: '支付中',
    [ORDER_STATUS.PAY_SUCCESS]: '支付成功',
    [ORDER_STATUS.PAY_FAILED]: '支付失败',
    [ORDER_STATUS.CANCELLED]: '已取消'
  }
  return statusMap[status] || '未知'
}
