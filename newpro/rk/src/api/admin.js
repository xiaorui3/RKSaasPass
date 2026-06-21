import request from '@/utils/request'

// 管理员身份验证
export function authenticateAdmin(data) {
  return request({
    url: '/api/admin/auth/authenticate',
    method: 'post',
    data
  })
}

// 验证管理员会话
export function verifyAdminSession(data) {
  return request({
    url: '/api/admin/auth/verify',
    method: 'post',
    data
  })
}

// 获取管理员信息
export function getAdminInfo(params) {
  return request({
    url: '/api/admin/auth/info',
    method: 'get',
    params
  })
}

// 管理员登出
export function adminLogout(data) {
  return request({
    url: '/api/admin/auth/logout',
    method: 'post',
    data
  })
}

// 验证管理员密钥
export function validateAdminKey(data) {
  return request({
    url: '/api/admin/auth/validate-key',
    method: 'post',
    data
  })
}