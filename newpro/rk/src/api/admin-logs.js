import request from '@/utils/request'

export function getOperLogPage(params) {
  return request({
    url: '/admin/logs/operlog/list',
    method: 'post',
    params
  })
}

export function getLoginLogPage(params) {
  return request({
    url: '/admin/logs/logininfor/list',
    method: 'post',
    params
  })
}
