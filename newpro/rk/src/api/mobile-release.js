import request from '@/utils/request'

export function getMobileReleaseConfig() {
  return request({
    url: '/api/admin/mobile/releases/config',
    method: 'get'
  })
}

export function saveMobileReleaseConfig(data) {
  return request({
    url: '/api/admin/mobile/releases/config',
    method: 'post',
    data
  })
}

export function getMobileReleaseHistory() {
  return request({
    url: '/api/admin/mobile/releases/history',
    method: 'get'
  })
}

export function getMobileReleaseBuildHistory() {
  return request({
    url: '/api/admin/mobile/releases/builds',
    method: 'get'
  })
}

export function getLatestMobileReleaseBuild() {
  return request({
    url: '/api/admin/mobile/releases/builds/latest',
    method: 'get'
  })
}

export function getMobileReleaseBuildLogs(buildId, params = {}) {
  return request({
    url: `/api/admin/mobile/releases/builds/${encodeURIComponent(buildId)}/logs`,
    method: 'get',
    params
  })
}

export function withdrawMobileRelease() {
  return request({
    url: '/api/admin/mobile/releases/withdraw',
    method: 'post'
  })
}

export function rollbackMobileRelease(data) {
  return request({
    url: '/api/admin/mobile/releases/rollback',
    method: 'post',
    data
  })
}

export function autoBuildMobileRelease() {
  return request({
    url: '/api/admin/mobile/releases/auto-build',
    method: 'post'
  })
}

export function getLatestMobileRelease(params) {
  return request({
    url: '/api/mobile/releases/latest',
    method: 'get',
    params,
    silentError: true
  })
}
