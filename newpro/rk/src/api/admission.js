import request from '@/utils/request'

export function submitApplication(data, tenantId) {
  const headers = {}
  if (tenantId) {
    headers['X-Tenant-Id'] = tenantId
  }

  return request({
    url: '/api/admission/submit',
    method: 'post',
    data,
    headers
  })
}

export function checkApplicationStatus(data) {
  return request({
    url: '/api/admission/check-status',
    method: 'post',
    data
  })
}

export function getAllApplications() {
  return request({
    url: '/api/admission/list',
    method: 'get'
  })
}

export function getApplicationsByStatus(status) {
  return request({
    url: `/api/admission/status/${status}`,
    method: 'get'
  })
}

export function reviewApplication(data) {
  return request({
    url: '/api/admission/review',
    method: 'post',
    data
  })
}

export function getAllRegisterReviews() {
  return request({
    url: '/api/admission/register-review/list',
    method: 'get'
  })
}

export function getRegisterReviewsByStatus(status) {
  return request({
    url: `/api/admission/register-review/status/${status}`,
    method: 'get'
  })
}

export function getRegisterReviewStatistics() {
  return request({
    url: '/api/admission/register-review/statistics',
    method: 'get'
  })
}

export function reviewRegisterReview(data) {
  return request({
    url: '/api/admission/register-review/review',
    method: 'post',
    data
  })
}

export function deleteApplication(id) {
  return request({
    url: `/api/admission/${id}`,
    method: 'delete'
  })
}

export function getApplicationStatistics() {
  return request({
    url: '/api/admission/statistics',
    method: 'get'
  })
}

export function validateEmailMatch(data) {
  return request({
    url: '/api/admission/validate-email',
    method: 'post',
    data
  })
}

export function isAdminUser(data) {
  return request({
    url: '/api/admission/check-admin',
    method: 'post',
    data
  })
}

export function getPublicAdmissionFormConfig(tenantId) {
  const headers = {}
  const params = {}
  if (tenantId) {
    headers['X-Tenant-Id'] = tenantId
    params.tenantId = tenantId
  }
  return request({
    url: '/api/admission/form-config/public',
    method: 'get',
    headers,
    params
  })
}

export function getCurrentAdmissionFormConfig() {
  return request({
    url: '/api/admission/form-config/current',
    method: 'get'
  })
}

export function saveCurrentAdmissionFormConfig(data) {
  return request({
    url: '/api/admission/form-config/current',
    method: 'put',
    data
  })
}

export function previewReviewAction(token) {
  return request({
    url: '/api/admission/review-action/preview',
    method: 'get',
    params: { token }
  })
}

export function executeReviewAction(token) {
  return request({
    url: '/api/admission/review-action/execute',
    method: 'post',
    params: { token }
  })
}
