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
  const res = await request.post(`${API_BASE}/auth/login`, {
    data: {
      username: user.username,
      password: user.password,
      organizationId: user.organizationId
    }
  })
  const body = await res.json()
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

async function openEmailCenter(page, loginData) {
  await applyLogin(page, loginData)
  await page.goto(`${BASE_URL}/admin/club/email-center`, { waitUntil: 'domcontentloaded' })
  await expect(page).not.toHaveURL(/\/login/)
  await expect(page.locator('.admin-page .el-tabs').first()).toBeVisible()
}

test('manager should open email center page', async ({ page, request }) => {
  const login = await loginApi(request, MANAGER)
  await openEmailCenter(page, login)
  await expect(page.locator('.history-card, .el-card').first()).toBeVisible()
})

test('manager should resolve recipients by role through email-center api', async ({ request }) => {
  const login = await loginApi(request, MANAGER)

  const roleRes = await request.get(`${API_BASE}/roles/list`, {
    headers: authHeaders(login)
  })
  const roleBody = await roleRes.json()
  expect(roleBody.code).toBe(200)
  expect(Array.isArray(roleBody.data)).toBeTruthy()
  expect(roleBody.data.length).toBeGreaterThan(0)

  const firstRoleId = roleBody.data[0].id
  const resolveRes = await request.post(`${API_BASE}/api/email-center/recipients/resolve`, {
    headers: authHeaders(login),
    data: {
      roleIds: [firstRoleId]
    }
  })
  const resolveBody = await resolveRes.json()
  expect(resolveBody.code).toBe(200)
  expect(Array.isArray(resolveBody.data)).toBeTruthy()
})

test('manager should queue a managed email send task', async ({ request }) => {
  const login = await loginApi(request, MANAGER)
  const seed = Date.now()

  const sendRes = await request.post(`${API_BASE}/api/email-center/send`, {
    headers: authHeaders(login),
    data: {
      roleIds: [],
      activityId: null,
      competitionId: null,
      manualEmails: [`e2e_email_center_${seed}@example.com`],
      subject: `E2E Email Center ${seed}`,
      content: 'E2E managed email content.',
      html: false
    }
  })
  const sendBody = await sendRes.json()
  expect(sendBody.code).toBe(200)
  expect(typeof sendBody.data?.queuedCount).toBe('number')
  expect(sendBody.data.queuedCount).toBeGreaterThanOrEqual(1)
})
