// @ts-check
const { test, expect } = require('@playwright/test')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'

const MANAGER = {
  username: 'manager_a',
  password: '123456',
  organizationId: '1'
}

async function loginApi(request, user) {
  const response = await request.post(`${API_BASE}/auth/login`, {
    data: {
      username: user.username,
      password: user.password,
      organizationId: user.organizationId
    }
  })
  const body = await response.json()
  if (body.code !== 200 || !body.data?.token) {
    throw new Error(`Login failed: ${JSON.stringify(body)}`)
  }
  return body.data
}

function authHeaders(loginData) {
  return {
    Authorization: `Bearer ${loginData.token}`,
    'X-Tenant-Id': String(loginData.organizationId),
    'Content-Type': 'application/json'
  }
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

test('manager should see latest invitation in invitation history api and page', async ({ page, request }) => {
  const seed = Date.now()
  const inviteEmail = `history_${seed}@example.com`
  const login = await loginApi(request, MANAGER)

  const sendRes = await request.post(`${API_BASE}/api/email-center/invitations/send`, {
    headers: authHeaders(login),
    data: {
      manualEmails: [inviteEmail],
      invitationType: 'REGISTER',
      subject: `history invite ${seed}`,
      content: 'history invite content'
    }
  })
  const sendBody = await sendRes.json()
  expect(sendBody.code).toBe(200)

  const listRes = await request.get(`${API_BASE}/api/email-center/invitations`, {
    headers: authHeaders(login)
  })
  const listBody = await listRes.json()
  expect(listBody.code).toBe(200)
  expect(Array.isArray(listBody.data)).toBeTruthy()
  expect(listBody.data.some((item) => item.targetEmail === inviteEmail)).toBeTruthy()

  await applyLogin(page, login)
  await page.goto(`${BASE_URL}/admin/club/email-center`, { waitUntil: 'domcontentloaded' })
  await expect(page).not.toHaveURL(/\/login/)
  await expect(page.locator('.admin-page')).toBeVisible()
})
