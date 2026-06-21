// @ts-check
const { test, expect } = require('@playwright/test')
const { ADMIN, loginApi } = require('./helpers/admission-form-helpers.cjs')

const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'

test('approved admissions should be represented in current tenant club members', async ({ request }) => {
  const login = await loginApi(request, API_BASE, ADMIN)
  const headers = {
    Authorization: `Bearer ${login.token}`,
    'X-Tenant-Id': login.organizationId || '1'
  }

  const admissionsRes = await request.get(`${API_BASE}/api/admission/list`, { headers })
  expect(admissionsRes.ok()).toBeTruthy()
  const admissionsBody = await admissionsRes.json()

  const membersRes = await request.get(`${API_BASE}/api/members/list`, { headers })
  expect(membersRes.ok()).toBeTruthy()
  const membersBody = await membersRes.json()

  const brokenReviewedAdmissions = (admissionsBody.data || []).filter((item) =>
    item.reviewTime && !item.authUserId && !item.username
  )
  const memberStudentIds = new Set((membersBody.data || []).map((item) => String(item.studentId)))
  const missingMembers = brokenReviewedAdmissions.filter((item) => !memberStudentIds.has(String(item.studentId)))

  expect(missingMembers).toEqual([])
})
