import request from '@/utils/request'

// 提交社团加入申请
export function submitApplication(data) {
  return request({
    url: '/api/members/apply',
    method: 'post',
    data
  })
}

// 获取所有申请列表
export function getApplicationList() {
  return request({
    url: '/api/members/list',
    method: 'get'
  })
}

// 检查用户申请状态
export function checkApplicationStatus(studentId) {
  return request({
    url: '/api/members/check-user-application',
    method: 'get',
    params: { studentId }
  })
}

// 获取申请详情
export function getApplicationDetail(id) {
  return request({
    url: `/api/members/${id}`,
    method: 'get'
  })
}

// 审核申请
export function reviewApplication(id, agreeStatus, reviewComment) {
  return request({
    url: `/api/members/review/${id}`,
    method: 'post',
    params: { agreeStatus, reviewComment }
  })
}

// 删除申请
export function deleteApplication(id) {
  return request({
    url: `/api/members/${id}`,
    method: 'delete'
  })
}

// 获取申请统计数据
export function getDeletedMembers() {
  return request({
    url: '/api/members/deleted',
    method: 'get'
  })
}

export function restoreMember(id) {
  return request({
    url: `/api/members/${id}/restore`,
    method: 'post'
  })
}

export function getApplicationStatistics() {
  return request({
    url: '/api/members/statistics',
    method: 'get'
  })
}

// 更新成员信息
export function updateMember(id, data) {
  return request({
    url: `/api/members/${id}`,
    method: 'put',
    data
  })
}

// 获取成员列表（复用申请列表接口）
export function getMembersList() {
  return getApplicationList()
}
