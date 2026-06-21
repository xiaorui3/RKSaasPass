// @ts-check
const { test, expect } = require('@playwright/test')
const { SUPER_ADMIN } = require('./helpers/test-users.cjs')

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

test('super admin k8s page should call real environment api and render cluster status', async ({ page, request }) => {
  const login = await loginApi(request, SUPER_ADMIN)
  const seen = []

  page.on('response', async (response) => {
    if (response.url().includes('/admin/ops/k8s/overview')) {
      seen.push({ url: response.url(), status: response.status() })
    }
  })

  await applyLogin(page, login)
  await page.goto(`${BASE_URL}/admin/operation/k8s`, { waitUntil: 'networkidle' })

  await expect(page.getByText(/K8s 绠＄悊|K8s鐜|Kubernetes/).first()).toBeVisible()
  await expect(page.locator('.admin-page, main').first()).toBeVisible()
  expect(seen.some((item) => item.status === 200)).toBeTruthy()
})
