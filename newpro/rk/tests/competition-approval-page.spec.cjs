// @ts-check
const { test, expect } = require('@playwright/test')
const {
  TENANT1_MANAGER,
  TENANT1_LIVE_TEACHER
} = require('./helpers/test-users.cjs')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'

async function loginApi(request, user) {
  const res = await request.post(`${API_BASE}/auth/login`, { data: user })
  const body = await res.json()
  if (body.code !== 200 || !body.data?.token) {
    throw new Error(`Login failed: ${JSON.stringify(body)}`)
  }
  return body.data
}

async function applyLogin(page, loginData) {
  await page.addInitScript((data) => {
    localStorage.setItem('token', data.token)
    localStorage.setItem('tenantId', data.organizationId)
    localStorage.setItem('userInfo', JSON.stringify({
      userId: data.userId,
      username: data.username,
      organizationId: data.organizationId,
      roles: data.roles || []
    }))
  }, loginData)
}

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

test('teacher should be able to review a pending competition from the admin page', async ({ page, request }) => {
  const manager = await loginApi(request, TENANT1_MANAGER)
  const teacher = await loginApi(request, TENANT1_LIVE_TEACHER)
  const title = `competition-page-approval-${Date.now()}`

  const createRes = await request.post(`${API_BASE}/api/competition`, {
    headers: buildHeaders(manager),
    data: {
      title,
      description: 'competition approval page test',
      content: 'competition approval page test content',
      competitionType: 'coding',
      level: 'school',
      organizer: 'manager_a',
      registrationStart: future(24),
      registrationEnd: future(48),
      competitionStart: future(72),
      competitionEnd: future(96),
      maxParticipants: 30,
      isPublished: true
    }
  })
  expect(createRes.ok()).toBeTruthy()

  await applyLogin(page, teacher)
  await page.goto(`${BASE_URL}/admin/activity/competition-approval`, { waitUntil: 'networkidle' })

  await expect(page.getByText(/姣旇禌瀹℃壒/).first()).toBeVisible()
  await expect(page.getByText(title).first()).toBeVisible()

  const row = page.locator('.el-table__row').filter({ hasText: title }).first()
  await row.getByRole('button', { name: /閫氳繃/ }).click()
  await expect(page.locator('.el-message').filter({ hasText: /鎴愬姛|瀹屾垚/ }).last()).toBeVisible()
})
