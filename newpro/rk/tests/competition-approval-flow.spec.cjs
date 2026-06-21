// @ts-check
const { test, expect } = require('@playwright/test')
const {
  TENANT1_MANAGER,
  TENANT1_LIVE_TEACHER
} = require('./helpers/test-users.cjs')
const { loginApi } = require('./helpers/live-login.cjs')

const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'

function buildHeaders(loginData) {
  return {
    Authorization: `Bearer ${loginData.token}`,
    'X-Tenant-Id': loginData.organizationId || '1',
    'Content-Type': 'application/json'
  }
}

function future(offsetHours) {
  return new Date(Date.now() + offsetHours * 60 * 60 * 1000).toISOString().slice(0, 19).replace('T', ' ')
}

test('competition should require teacher approval before becoming published', async ({ request }) => {
  const manager = await loginApi(request, TENANT1_MANAGER, API_BASE)
  const teacher = await loginApi(request, TENANT1_LIVE_TEACHER, API_BASE)
  const managerHeaders = buildHeaders(manager)
  const teacherHeaders = buildHeaders(teacher)
  const title = `competition-approval-${Date.now()}`

  const createRes = await request.post(`${API_BASE}/api/competition`, {
    headers: managerHeaders,
    data: {
      title,
      description: 'competition approval flow test',
      content: 'competition approval flow test content',
      competitionType: 'coding',
      level: 'school',
      organizer: 'manager_a',
      registrationStart: future(24),
      registrationEnd: future(48),
      competitionStart: future(72),
      competitionEnd: future(96),
      maxParticipants: 50,
      isPublished: true,
      isCrossTenant: false,
      managerReviewerId: manager.userId,
      teacherReviewerId: teacher.userId
    }
  })
  expect(createRes.ok()).toBeTruthy()
  const createBody = await createRes.json()
  expect(createBody.code).toBe(200)
  const competitionId = createBody.data

  const publishedBeforeRes = await request.get(`${API_BASE}/api/competition/published`, { headers: managerHeaders })
  expect(publishedBeforeRes.ok()).toBeTruthy()
  const publishedBefore = await publishedBeforeRes.json()
  expect((publishedBefore.data || []).some((item) => item.id === competitionId || item.title === title)).toBeFalsy()

  const managerPendingRes = await request.get(`${API_BASE}/api/competition/review/pending?page=1&size=50`, { headers: managerHeaders })
  expect(managerPendingRes.ok()).toBeTruthy()
  const managerPendingBody = await managerPendingRes.json()
  expect(managerPendingBody.code).toBe(200)
  const managerPendingRecords = managerPendingBody.data?.records || managerPendingBody.data || []
  expect(managerPendingRecords.some((item) => item.id === competitionId || item.title === title)).toBeTruthy()

  const managerApproveRes = await request.post(`${API_BASE}/api/competition/${competitionId}/review/manager`, {
    headers: managerHeaders,
    data: {
      approved: true,
      reviewComment: 'manager approval for competition flow test'
    }
  })
  expect(managerApproveRes.ok()).toBeTruthy()
  const managerApproveBody = await managerApproveRes.json()
  expect(managerApproveBody.code).toBe(200)

  const teacherPendingRes = await request.get(`${API_BASE}/api/competition/review/pending?page=1&size=50`, { headers: teacherHeaders })
  expect(teacherPendingRes.ok()).toBeTruthy()
  const teacherPendingBody = await teacherPendingRes.json()
  expect(teacherPendingBody.code).toBe(200)
  const teacherPendingRecords = teacherPendingBody.data?.records || teacherPendingBody.data || []
  expect(teacherPendingRecords.some((item) => item.id === competitionId || item.title === title)).toBeTruthy()

  const approveRes = await request.post(`${API_BASE}/api/competition/${competitionId}/review/teacher`, {
    headers: teacherHeaders,
    data: {
      approved: true,
      reviewComment: 'approval for competition flow test'
    }
  })
  expect(approveRes.ok()).toBeTruthy()
  const approveBody = await approveRes.json()
  expect(approveBody.code).toBe(200)

  const publishedAfterRes = await request.get(`${API_BASE}/api/competition/published`, { headers: managerHeaders })
  expect(publishedAfterRes.ok()).toBeTruthy()
  const publishedAfter = await publishedAfterRes.json()
  expect((publishedAfter.data || []).some((item) => item.id === competitionId || item.title === title)).toBeTruthy()
})
