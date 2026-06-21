import request from '@/utils/request'

// 发送文本邮件
export function sendTextEmail(data) {
  return request({
    url: '/api/email/send-text',
    method: 'post',
    data
  })
}

// 发送HTML邮件
export function sendHtmlEmail(data) {
  return request({
    url: '/api/email/send-html',
    method: 'post',
    data
  })
}

// 发送申请通知邮件
export function sendApplicationNotice(data) {
  return request({
    url: '/api/email/send-application-notice',
    method: 'post',
    data
  })
}

// 发送审核结果邮件
export function sendReviewResultEmail(data) {
  return request({
    url: '/api/email/send-review-result',
    method: 'post',
    data
  })
}

// 批量发送邮件
export function sendBulkEmails(data) {
  return request({
    url: '/api/email/send-bulk',
    method: 'post',
    data
  })
}

// 发送带附件的邮件
export function sendEmailWithAttachment(data) {
  return request({
    url: '/api/email/send-with-attachment',
    method: 'post',
    data
  })
}

export function resolveEmailRecipients(data) {
  return request({
    url: '/api/email-center/recipients/resolve',
    method: 'post',
    data
  })
}

export function sendManagedEmail(data) {
  return request({
    url: '/api/email-center/send',
    method: 'post',
    data
  })
}

export function sendInvitationEmail(data) {
  return request({
    url: '/api/email-center/invitations/send',
    method: 'post',
    data
  })
}

export function getEmailInvitation(inviteToken) {
  return request({
    url: `/api/email-center/invitations/${inviteToken}`,
    method: 'get'
  })
}

export function acceptEmailInvitation(inviteToken) {
  return request({
    url: `/api/email-center/invitations/${inviteToken}/accept`,
    method: 'post'
  })
}

export function listEmailInvitations() {
  return request({
    url: '/api/email-center/invitations',
    method: 'get'
  })
}

export function listEmailSendTasks() {
  return request({
    url: '/api/email-center/tasks',
    method: 'get'
  })
}

export function listEmailTaskRecipients(taskId) {
  return request({
    url: `/api/email-center/tasks/${taskId}/recipients`,
    method: 'get'
  })
}

export function listEmailTemplates(params) {
  return request({
    url: '/api/email-templates',
    method: 'get',
    params
  })
}

export function getEmailTemplate(id) {
  return request({
    url: `/api/email-templates/${id}`,
    method: 'get'
  })
}

export function createEmailTemplate(data) {
  return request({
    url: '/api/email-templates',
    method: 'post',
    data
  })
}

export function updateEmailTemplate(id, data) {
  return request({
    url: `/api/email-templates/${id}`,
    method: 'put',
    data
  })
}

export function updateEmailTemplateStatus(id, status) {
  return request({
    url: `/api/email-templates/${id}/status/${status}`,
    method: 'put'
  })
}

export function deleteEmailTemplate(id) {
  return request({
    url: `/api/email-templates/${id}`,
    method: 'delete'
  })
}
