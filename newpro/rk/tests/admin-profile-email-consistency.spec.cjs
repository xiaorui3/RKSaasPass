// @ts-check
const { test, expect } = require('@playwright/test')
const { SUPER_ADMIN } = require('./helpers/test-users.cjs')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'

async function loginApi(request, user) {
  const res = await request.post(`${API_BASE}/auth/login`, { data: user })
  const body = await res.json()
  expect(body.code).toBe(200)
  expect(body.data?.token).toBeTruthy()
  return body.data
}

async function fetchMyInfo(request, loginData) {
  const res = await request.get(`${API_BASE}/users/me`, {
    headers: {
      Authorization: `Bearer ${loginData.token}`,
      'X-Tenant-Id': String(loginData.organizationId || '1'),
      'X-Super-Admin': 'true'
    }
  })
  const body = await res.json()
  expect(body.code).toBe(200)
  expect(body.data?.email).toBeTruthy()
  return body.data
}

test('profile email field should stay consistent with /users/me for super admin', async ({ page, request }) => {
  test.setTimeout(120000)
  const loginData = await loginApi(request, SUPER_ADMIN)
  const me = await fetchMyInfo(request, loginData)

  await page.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' })
  await page.evaluate((data) => {
    localStorage.setItem('token', data.token)
    localStorage.setItem('tenantId', data.organizationId)
    localStorage.setItem('userInfo', JSON.stringify({
      userId: data.userId,
      username: data.username,
      organizationId: data.organizationId,
      roles: data.roles || []
    }))
  }, loginData)

  await page.goto(`${BASE_URL}/profile`, { waitUntil: 'domcontentloaded' })
  await page.waitForFunction(() => {
    const userName = document.querySelector('.user-name')?.textContent?.trim() || ''
    const inputs = document.querySelectorAll('.profile-form input')
    const email = inputs[2] ? String(inputs[2].value || '').trim() : ''
    return inputs.length >= 3 && userName && !userName.includes('鍔犺浇涓?) && email.length > 0
  }, { timeout: 30000 })

  const profileEmail = await page.evaluate(() => {
    const inputs = document.querySelectorAll('.profile-form input')
    const field = inputs[2]
    return field ? field.value.trim() : null
  })

  expect(profileEmail).toBe(String(me.email || '').trim())
})
