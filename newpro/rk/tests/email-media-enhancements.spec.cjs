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

test('manager should send html-rich email task via email center', async ({ page, request }) => {
  const seed = Date.now()
  const login = await loginApi(request, MANAGER)

  const htmlContent = `
<h2>濯掍綋澧炲己閭欢 ${seed}</h2>
<p>杩欐槸涓€灏佸寘鍚浘鐗囧拰瑙嗛閾炬帴鍗＄墖鐨勬祴璇曢偖浠躲€?/p>
<p><img src="https://example.com/demo.png" alt="demo" style="max-width:100%" /></p>
<p><a href="https://example.com/video/${seed}">鐐瑰嚮瑙傜湅瑙嗛</a></p>
`.trim()

  const sendRes = await request.post(`${API_BASE}/api/email-center/send`, {
    headers: authHeaders(login),
    data: {
      roleIds: [],
      activityId: null,
      competitionId: null,
      manualEmails: [`media_${seed}@example.com`],
      subject: `濯掍綋澧炲己閭欢-${seed}`,
      content: htmlContent,
      html: true
    }
  })
  const sendBody = await sendRes.json()
  expect(sendBody.code).toBe(200)
  expect(typeof sendBody.data?.queuedCount).toBe('number')
  expect(sendBody.data.queuedCount).toBeGreaterThanOrEqual(1)

  await applyLogin(page, login)
  await page.goto(`${BASE_URL}/admin/club/email-center`, { waitUntil: 'domcontentloaded' })
  await expect(page).not.toHaveURL(/\/login/)
  await expect(page.locator('.admin-page')).toBeVisible()
})
