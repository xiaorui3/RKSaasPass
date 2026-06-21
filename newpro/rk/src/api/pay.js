/**
 * 支付管理API
 * 对应后端Controller: PayOrderController.java, PayChannelController.java, RefundOrderController.java
 * 基础路径: /pay-orders, /pay-channels, /refund-orders
 */
import request from '@/utils/request'

// ==================== 支付订单接口 ====================

/**
 * 扫码支付申请支付单
 * @param {Object} data - 支付信息
 * @param {string} data.bizOrderId - 业务订单号
 * @param {number} data.amount - 支付金额（分）
 * @param {string} data.payChannel - 支付渠道
 * @returns {Promise} 支付二维码信息
 */
export function applyPayOrder(data) {
  return request({
    url: '/pay-orders',
    method: 'post',
    data
  })
}

/**
 * 根据业务订单ID查询支付结果
 * @param {string} bizOrderId - 业务订单号
 * @returns {Promise} 支付结果
 */
export function queryPayResult(bizOrderId) {
  return request({
    url: `/pay-orders/${bizOrderId}/status`,
    method: 'get'
  })
}

// ==================== 支付渠道接口 ====================

/**
 * 获取支付渠道列表
 * @returns {Promise} 渠道列表
 */
export function getPayChannelList() {
  return request({
    url: '/pay-channels/list',
    method: 'get'
  })
}

/**
 * 添加支付渠道
 * @param {Object} data - 渠道信息
 * @returns {Promise} 操作结果
 */
export function addPayChannel(data) {
  return request({
    url: '/pay-channels',
    method: 'post',
    data
  })
}

/**
 * 修改支付渠道
 * @param {number} id - 渠道ID
 * @param {Object} data - 渠道信息
 * @returns {Promise} 操作结果
 */
export function updatePayChannel(id, data) {
  return request({
    url: `/pay-channels/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除支付渠道
 * @param {number} id - 渠道ID
 * @returns {Promise} 操作结果
 */
export function deletePayChannel(id) {
  return request({
    url: `/pay-channels/${id}`,
    method: 'delete'
  })
}

// ==================== 退款订单接口 ====================

/**
 * 申请退款
 * @param {Object} data - 退款信息
 * @param {string} data.bizRefundOrderId - 业务退款单号
 * @param {string} data.bizOrderId - 原业务订单号
 * @param {number} data.refundAmount - 退款金额（分）
 * @param {string} data.refundReason - 退款原因
 * @returns {Promise} 退款结果
 */
export function applyRefund(data) {
  return request({
    url: '/refund-orders',
    method: 'post',
    data
  })
}

/**
 * 查询退款结果
 * @param {string} bizRefundOrderId - 业务退款单号
 * @returns {Promise} 退款结果
 */
export function queryRefundResult(bizRefundOrderId) {
  return request({
    url: `/refund-orders/${bizRefundOrderId}/status`,
    method: 'get'
  })
}

// ==================== 支付接口（前端调用） ====================

/**
 * 支付申请（前端直接调用）
 * @param {Object} data - 支付信息
 * @returns {Promise} 支付信息
 */
export function applyPayment(data) {
  return request({
    url: '/pay/order',
    method: 'post',
    data
  })
}

/**
 * 获取支付渠道列表（前端）
 * @returns {Promise} 渠道列表
 */
export function queryPayChannels() {
  return request({
    url: '/pay/channels',
    method: 'get'
  })
}

// ==================== 支付渠道常量 ====================

/**
 * 支付渠道
 */
export const PAY_CHANNEL = {
  ALIPAY_PC: 'Ali_PC',      // 支付宝PC
  ALIPAY_H5: 'Ali_H5',      // 支付宝H5
  ALIPAY_APP: 'Ali_APP',    // 支付宝APP
  WXPAY_NATIVE: 'WX_Native', // 微信扫码
  WXPAY_H5: 'WX_H5',        // 微信H5
  WXPAY_APP: 'WX_APP'       // 微信APP
}

/**
 * 支付状态
 */
export const PAY_STATUS = {
  NOT_PAY: 1,      // 未支付
  PAYING: 2,       // 支付中
  PAY_SUCCESS: 3,  // 支付成功
  PAY_FAILED: 4    // 支付失败
}

/**
 * 退款状态
 */
export const REFUND_STATUS = {
  NOT_REFUND: 1,      // 未退款
  REFUNDING: 2,       // 退款中
  REFUND_SUCCESS: 3,  // 退款成功
  REFUND_FAILED: 4    // 退款失败
}

/**
 * 获取支付渠道文本
 * @param {string} channel - 渠道标识
 * @returns {string} 渠道文本
 */
export function getPayChannelText(channel) {
  const channelMap = {
    [PAY_CHANNEL.ALIPAY_PC]: '支付宝PC',
    [PAY_CHANNEL.ALIPAY_H5]: '支付宝H5',
    [PAY_CHANNEL.ALIPAY_APP]: '支付宝APP',
    [PAY_CHANNEL.WXPAY_NATIVE]: '微信扫码',
    [PAY_CHANNEL.WXPAY_H5]: '微信H5',
    [PAY_CHANNEL.WXPAY_APP]: '微信APP'
  }
  return channelMap[channel] || '未知'
}

/**
 * 获取支付状态文本
 * @param {number} status - 状态码
 * @returns {string} 状态文本
 */
export function getPayStatusText(status) {
  const statusMap = {
    [PAY_STATUS.NOT_PAY]: '未支付',
    [PAY_STATUS.PAYING]: '支付中',
    [PAY_STATUS.PAY_SUCCESS]: '支付成功',
    [PAY_STATUS.PAY_FAILED]: '支付失败'
  }
  return statusMap[status] || '未知'
}
