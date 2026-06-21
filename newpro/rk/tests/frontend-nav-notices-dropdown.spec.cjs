// @ts-check
const { test, expect } = require('@playwright/test')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'
const SUPER_ADMIN = { username: 'admin_a', password: 'change-me', organizationId: '1' }

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

test('frontend main nav should expose activities competition and notices entries', async ({ page }) => {
  await page.goto(`${BASE_URL}/`, { waitUntil: 'networkidle' })

  await expect(page.locator('.main-nav .nav-link[href="/activities"]')).toBeVisible()
  await expect(page.locator('.main-nav .nav-link[href="/competition"]')).toBeVisible()
  await expect(page.locator('.main-nav .nav-link[href="/notices"]')).toBeVisible()
})

test('guest notices page should load published notices through a public endpoint', async ({ page }) => {
  const seen = []
  page.on('response', async (response) => {
    if (response.url().includes('/notifications/api/notices/published')) {
      seen.push(response.status())
    }
  })

  await page.goto(`${BASE_URL}/notices`, { waitUntil: 'networkidle' })
  await expect(page.getByText(/鍏憡|閫氱煡/).first()).toBeVisible()
  expect(seen).toContain(200)
})

test('admin dropdown theme should not stay pure white after ui refresh', async ({ page, request }) => {
  const login = await loginApi(request, SUPER_ADMIN)
  await applyLogin(page, login)

  await page.goto(`${BASE_URL}/admin`, { waitUntil: 'networkidle' })
  await page.locator('.user-info').click()
  await expect(page.locator('.el-dropdown-menu__item').first()).toBeVisible()

  const menuBackground = await page.locator('.el-dropdown-menu__item').first().evaluate((el) => {
    return getComputedStyle(el.parentElement).backgroundColor
  })

  expect(menuBackground).not.toBe('rgb(255, 255, 255)')
})
