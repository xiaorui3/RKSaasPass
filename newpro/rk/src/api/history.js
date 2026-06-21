import request from '@/utils/request'

// 获取历史事件时间轴
export function getTimeline() {
  return request({
    url: '/api/history/timeline',
    method: 'get'
  })
}

// 根据年份获取历史事件
export function getEventsByYear(year) {
  return request({
    url: `/api/history/year/${year}`,
    method: 'get'
  })
}

// 根据类型获取历史事件
export function getEventsByType(type) {
  return request({
    url: `/api/history/type/${type}`,
    method: 'get'
  })
}

// 获取重要历史事件
export function getImportantEvents() {
  return request({
    url: '/api/history/important',
    method: 'get'
  })
}

// 获取历史事件详情
export function getEventDetail(id) {
  return request({
    url: `/api/history/${id}`,
    method: 'get'
  })
}

// 搜索历史事件
export function searchEvents(keyword) {
  return request({
    url: '/api/history/search',
    method: 'get',
    params: { keyword }
  })
}

// 获取统计数据
export function getHistoryStatistics() {
  return request({
    url: '/api/history/statistics',
    method: 'get'
  })
}

export function getAdminHistoryPage(params = {}) {
  return request({
    url: '/api/history/admin/page',
    method: 'get',
    params
  })
}

export function saveAdminHistoryEvent(data) {
  return request({
    url: '/api/history/admin/save',
    method: 'post',
    data
  })
}

export function deleteAdminHistoryEvent(id) {
  return request({
    url: `/api/history/admin/${id}/delete`,
    method: 'post'
  })
}
