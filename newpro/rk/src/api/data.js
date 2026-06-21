import request from '@/utils/request'

export function getSaasVisualScreen(params = {}) {
  return request({
    url: '/api/data/saas-visual-screen',
    method: 'get',
    params
  })
}

export function getTenantOperationsCenter(params = {}) {
  return request({
    url: '/api/data/tenant-operations-center',
    method: 'get',
    params
  })
}

export function getApprovalTaskCenter(params = {}) {
  return request({
    url: '/api/data/approval-task-center',
    method: 'get',
    params
  })
}

export function getDataQualityCenter(params = {}) {
  return request({
    url: '/api/data/data-quality-center',
    method: 'get',
    params
  })
}

export function getParticipationAnalysisCenter(params = {}) {
  return request({
    url: '/api/data/participation-analysis-center',
    method: 'get',
    params
  })
}

export function getGrowthRetentionCenter(params = {}) {
  return request({
    url: '/api/data/growth-retention-center',
    method: 'get',
    params
  })
}

export function getContentPublishCalendar(params = {}) {
  return request({
    url: '/api/data/content-publish-calendar',
    method: 'get',
    params
  })
}

export function getOpenSourceHealth(params = {}) {
  return request({
    url: '/api/data/open-source-health',
    method: 'get',
    params
  })
}

export function repairDataQualityIssue(data = {}) {
  return request({
    url: '/api/data/data-quality-center/repair',
    method: 'post',
    data
  })
}

export function getSecurityAuditCenter(params = {}) {
  return request({
    url: '/api/data/security-audit-center',
    method: 'get',
    params
  })
}

export function notifySecurityAuditRisk(data = {}) {
  return request({
    url: '/api/data/security-audit-center/notify',
    method: 'post',
    data
  })
}

export function getBoardData() {
  return request({
    url: '/api/data/board',
    method: 'get'
  })
}

export function setBoardData(data) {
  return request({
    url: '/api/data/board/set',
    method: 'put',
    data
  })
}

export function getTodayData() {
  return request({
    url: '/api/data/today',
    method: 'get'
  })
}

export function setTodayData(data) {
  return request({
    url: '/api/data/today/set',
    method: 'put',
    data
  })
}

export function getTop10Data(type) {
  return request({
    url: '/api/data/top10',
    method: 'get',
    params: { type }
  })
}

export function setTop10Data(data) {
  return request({
    url: '/api/data/top10/set',
    method: 'put',
    data
  })
}

export function getDataOverview() {
  return request({
    url: '/api/data/overview',
    method: 'get'
  })
}

export function getTrendData(params) {
  return request({
    url: '/api/data/trend',
    method: 'get',
    params
  })
}

export function getDistributionData(dimension) {
  return request({
    url: '/api/data/distribution',
    method: 'get',
    params: { dimension }
  })
}

export function getFrontendCacheWarmupSummary() {
  return request({
    url: '/api/data/cache/frontend/summary',
    method: 'get'
  })
}

export function triggerFrontendCacheWarmup() {
  return request({
    url: '/api/data/cache/frontend/warmup',
    method: 'post'
  })
}
