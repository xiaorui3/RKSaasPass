// @ts-check
const { test, expect } = require('@playwright/test')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'
const MANAGER = { username: 'manager_a', password: '123456', organizationId: '1' }

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

test('manager should be blocked when registration end time is earlier than registration start time', async ({ page, request }) => {
  const login = await loginApi(request, MANAGER)
  await applyLogin(page, login)

  const createRequests = []
  page.on('request', (req) => {
    if (req.url().includes('/api/competition') && req.method() === 'POST') {
      createRequests.push(req.url())
    }
  })

  await page.goto(`${BASE_URL}/admin/activity/competition`, { waitUntil: 'networkidle' })
  await page.getByRole('button', { name: /鏂板姣旇禌/ }).click()

  await page.locator('.el-dialog').getByPlaceholder(/璇疯緭鍏ユ瘮璧涙爣/).fill(`invalid-time-${Date.now()}`)
  await page.locator('.el-form-item').filter({ hasText: '鎶ュ悕寮€' }).locator('input').first().fill('2026-05-10 10:00:00')
  await page.locator('.el-form-item').filter({ hasText: '鎶ュ悕缁撴潫' }).locator('input').first().fill('2026-05-09 10:00:00')
  await page.locator('.el-form-item').filter({ hasText: '姣旇禌寮€' }).locator('input').first().fill('2026-05-20 10:00:00')
  await page.locator('.el-form-item').filter({ hasText: '姣旇禌缁撴潫' }).locator('input').first().fill('2026-05-21 10:00:00')

  await page.locator('.el-dialog').getByRole('button', { name: '纭畾' }).click()

  await expect(page.getByText(/鎶ュ悕缁撴潫鏃堕棿蹇呴』鏅氫簬鎶ュ悕寮€濮嬫椂/)).toBeVisible()
  expect(createRequests).toHaveLength(0)
})
