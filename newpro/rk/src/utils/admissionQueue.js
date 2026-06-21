function safeJsonParse(value) {
  if (typeof value !== 'string' || !value.trim()) {
    return {}
  }
  try {
    return JSON.parse(value)
  } catch {
    return {}
  }
}

export function normalizeJoinReviewStatus(value) {
  if (value === '通过') return 1
  if (value === '未通过') return 2
  return 0
}

export function normalizeRegisterReviewStatus(value) {
  if (value === 'APPROVED') return 1
  if (value === 'REJECTED') return 2
  return 0
}

export function buildAdmissionQueue(joinApplications = [], registerReviews = []) {
  const joinItems = joinApplications.map((app) => {
    const formPayload = safeJsonParse(app.formPayloadJson)
    return {
      id: app.id,
      businessType: 'join',
      businessLabel: '入社申请',
      name: app.name || formPayload.name || '',
      studentId: app.studentId || formPayload.studentId || '',
      major: app.major || formPayload.major || '',
      phone: app.phone || formPayload.phone || '',
      email: app.email || formPayload.email || '',
      reason: app.interest || app.experience || formPayload.reason || '暂无',
      createTime: app.applicationTime || app.createTime || '',
      status: normalizeJoinReviewStatus(app.reviewStatus),
      rawStatus: app.reviewStatus,
      reviewComment: app.reviewComment || '',
      formPayload
    }
  })

  const registerItems = registerReviews.map((item) => {
    const formPayload = safeJsonParse(item.formPayloadJson)
    return {
      id: item.id,
      businessType: 'register',
      businessLabel: '注册审核',
      name: item.name || formPayload.name || '',
      studentId: formPayload.studentId || '',
      major: formPayload.major || '',
      phone: formPayload.phone || '',
      email: item.email || formPayload.email || '',
      reason: formPayload.reason || formPayload.introduction || formPayload.remark || '待审核注册账号',
      createTime: item.createTime || '',
      status: normalizeRegisterReviewStatus(item.reviewStatus),
      rawStatus: item.reviewStatus,
      reviewComment: item.reviewComment || '',
      username: item.username || formPayload.username || '',
      referralCode: item.referralCode || formPayload.referralCode || '',
      formPayload
    }
  })

  return [...joinItems, ...registerItems]
    .sort((left, right) => String(right.createTime || '').localeCompare(String(left.createTime || '')))
}
