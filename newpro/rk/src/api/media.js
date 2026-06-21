/**
 * 媒资管理API
 * 对应后端Controller: MediaController.java
 * 基础路径: /medias
 */
import request from '@/utils/request'

// ==================== 媒资管理接口 ====================

/**
 * 分页搜索媒资信息
 * @param {Object} params - 查询参数
 * @param {number} params.pageNo - 页码
 * @param {number} params.pageSize - 每页数量
 * @param {string} params.keyword - 搜索关键词（可选）
 * @param {string} params.status - 媒资状态（可选）
 * @returns {Promise} 分页数据
 */
export function getMediaPage(params) {
  return request({
    url: '/medias',
    method: 'get',
    params
  })
}

/**
 * 保存媒资信息
 * @param {Object} data - 媒资信息
 * @returns {Promise} 操作结果
 */
export function saveMedia(data) {
  return request({
    url: '/medias',
    method: 'post',
    data
  })
}

/**
 * 删除媒资视频
 * @param {string} mediaId - 媒资ID
 * @returns {Promise} 操作结果
 */
export function deleteMedia(mediaId) {
  return request({
    url: `/medias/${mediaId}`,
    method: 'delete'
  })
}

/**
 * 批量删除媒资视频
 * @param {string[]} mediaIds - 媒资ID列表
 * @returns {Promise} 操作结果
 */
export function deleteMedias(mediaIds) {
  return request({
    url: '/medias',
    method: 'delete',
    data: { mediaIds }
  })
}

// ==================== 签名接口 ====================

/**
 * 获取上传视频的授权签名
 * @param {string} filename - 文件名
 * @returns {Promise} 上传签名
 */
export function getUploadSignature(filename) {
  return request({
    url: '/medias/signature/upload',
    method: 'get',
    params: { filename }
  })
}

/**
 * 获取播放视频的授权签名
 * @param {string} mediaId - 媒资ID
 * @returns {Promise} 播放签名
 */
export function getPlaySignature(mediaId) {
  return request({
    url: '/medias/signature/play',
    method: 'get',
    params: { mediaId }
  })
}

/**
 * 管理端获取预览视频的授权签名
 * @param {string} mediaId - 媒资ID
 * @returns {Promise} 预览签名
 */
export function getPreviewSignature(mediaId) {
  return request({
    url: '/medias/signature/preview',
    method: 'get',
    params: { mediaId }
  })
}

// ==================== 媒资状态常量 ====================

/**
 * 媒资状态
 */
export const MEDIA_STATUS = {
  UPLOADING: 0,    // 上传中
  PROCESSING: 1,   // 处理中
  SUCCESS: 2,      // 成功
  FAILED: 3        // 失败
}

/**
 * 获取媒资状态文本
 * @param {number} status - 状态码
 * @returns {string} 状态文本
 */
export function getMediaStatusText(status) {
  const statusMap = {
    [MEDIA_STATUS.UPLOADING]: '上传中',
    [MEDIA_STATUS.PROCESSING]: '处理中',
    [MEDIA_STATUS.SUCCESS]: '成功',
    [MEDIA_STATUS.FAILED]: '失败'
  }
  return statusMap[status] || '未知'
}
