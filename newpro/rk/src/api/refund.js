/**
 * 退款申请API
 * 对应后端Controller: RefundApplyController.java
 * 基础路径: /refund-apply
 */
import request from '@/utils/request'

// ==================== 退款申请接口 ====================

/**
 * 申请退款
 * @param {Object} data - 退款申请信息
 * @param {number} data.orderDetailId - 订单明细ID
 * @param {number} data.refundAmount - 退款金额
 * @param {string} data.refundReason - 退款原因
 * @returns {Promise} 申请结果
 */
export function applyRefund(data) {
  return request({
    url: '/refund-apply',
    method: 'post',
    data
  })
}

/**
 * 审批退款申请
 * @param {Object} data - 审批信息
 * @param {number} data.id - 退款申请ID
 * @param {number} data.status - 审批状态 2-通过 3-拒绝
 * @param {string} data.rejectReason - 拒绝原因（拒绝时必填）
 * @returns {Promise} 操作结果
 */
export function approveRefund(data) {
  return request({
    url: '/refund-apply/approval',
    method: 'put',
    data
  })
}

/**
 * 取消退款申请
 * @param {number} id - 退款申请ID
 * @returns {Promise} 操作结果
 */
export function cancelRefund(id) {
  return request({
    url: '/refund-apply/cancel',
    method: 'put',
    params: { id }
  })
}

/**
 * 分页查询退款申请
 * @param {Object} params - 查询参数
 * @param {number} params.pageNo - 页码
 * @param {number} params.pageSize - 每页数量
 * @param {number} params.status - 退款状态（可选）
 * @returns {Promise} 分页数据
 */
export function getRefundApplyPage(params) {
  return request({
    url: '/refund-apply/page',
    method: 'get',
    params
  })
}

/**
 * 查询退款申请详情
 * @param {number} id - 退款申请ID
 * @returns {Promise} 退款详情
 */
export function getRefundApplyDetail(id) {
  return request({
    url: `/refund-apply/${id}`,
    method: 'get'
  })
}

/**
 * 根据订单明细ID查询退款详情
 * @param {number} orderDetailId - 订单明细ID
 * @returns {Promise} 退款详情
 */
export function getRefundByOrderDetail(orderDetailId) {
  return request({
    url: `/refund-apply/detail/${orderDetailId}`,
    method: 'get'
  })
}

/**
 * 查询下一个待审批的退款申请
 * @returns {Promise} 退款申请
 */
export function getNextRefundToApprove() {
  return request({
    url: '/refund-apply/next',
    method: 'get'
  })
}

// ==================== 退款状态常量 ====================

/**
 * 退款申请状态
 */
export const REFUND_APPLY_STATUS = {
  PENDING: 1,    // 待审批
  APPROVED: 2,   // 已通过
  REJECTED: 3,   // 已拒绝
  CANCELLED: 4,  // 已取消
  REFUNDED: 5    // 已退款
}

/**
 * 获取退款申请状态文本
 * @param {number} status - 状态码
 * @returns {string} 状态文本
 */
export function getRefundApplyStatusText(status) {
  const statusMap = {
    [REFUND_APPLY_STATUS.PENDING]: '待审批',
    [REFUND_APPLY_STATUS.APPROVED]: '已通过',
    [REFUND_APPLY_STATUS.REJECTED]: '已拒绝',
    [REFUND_APPLY_STATUS.CANCELLED]: '已取消',
    [REFUND_APPLY_STATUS.REFUNDED]: '已退款'
  }
  return statusMap[status] || '未知'
}
