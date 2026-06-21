// @ts-check
const { test, expect } = require('@playwright/test')
const {
  TENANT1_MANAGER,
  TENANT1_MEMBER
} = require('./helpers/test-users.cjs')

const BASE_URL = 'http://localhost:5173'
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

test('activity admin page should render readable Chinese copy', async ({ page, request }) => {
  const login = await loginApi(request, TENANT1_MANAGER)
  await applyLogin(page, login)

  await page.goto(`${BASE_URL}/admin/activity/list`, { waitUntil: 'networkidle' })
  await expect(page.getByText(/娲诲姩绠＄悊/).first()).toBeVisible()
  await expect(page.getByRole('button', { name: /鏂板娲诲姩/ })).toBeVisible()
  await expect(page.getByText(/鎶ュ悕绠＄悊/).first()).toBeVisible()
})

test('notifications page should render readable Chinese copy', async ({ page, request }) => {
  const login = await loginApi(request, TENANT1_MEMBER)
  await applyLogin(page, login)

  await page.goto(`${BASE_URL}/notifications`, { waitUntil: 'networkidle' })
  await expect(page.getByText(/娑堟伅閫氱煡/).first()).toBeVisible()
  await expect(page.getByRole('button', { name: /鍏ㄩ儴.*宸茶/ })).toBeVisible()
  await expect(page.getByText(/鏀朵欢绠?).first()).toBeVisible()
})