// @ts-check
const { test, expect } = require('@playwright/test')
const {
  SUPER_ADMIN,
  TENANT1_MANAGER,
  TENANT1_MEMBER,
  TENANT1_LIVE_TEACHER,
} = require('./helpers/test-users.cjs')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'

const USERS = {
  admin: SUPER_ADMIN,
  manager: TENANT1_MANAGER,
  teacher: TENANT1_LIVE_TEACHER,
  member: TENANT1_MEMBER
}

async function loginApi(request, user) {
  const res = await request.post(`${API_BASE}/auth/login`, { data: user })
  const body = await res.json()
  if (body.code !== 200 || !body.data?.token) {
    throw new Error(`Login failed: ${JSON.stringify(body)}`)
  }
  return body.data
}

async function applyLogin(page, loginData) {
  await page.goto(`${BASE_URL}/`, { waitUntil: 'networkidle' })
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
}

test('super admin should access core system routes', async ({ page, request }) => {
  test.setTimeout(120000)
  const login = await loginApi(request, USERS.admin)
  await applyLogin(page, login)

  for (const route of [
    '/admin/system/users',
    '/admin/system/roles',
    '/admin/system/tenants',
    '/admin/system/config',
    '/admin/operation/logs',
    '/admin/operation/monitoring',
    '/admin/operation/tasks',
    '/admin/operation/backup',
    '/admin/operation/k8s'
  ]) {
    await page.goto(`${BASE_URL}${route}`, { waitUntil: 'domcontentloaded' })
    await expect(page.locator('main, .admin-page').first()).toBeVisible()
  }
})

test('manager should access core club management routes', async ({ page, request }) => {
  test.setTimeout(120000)
  const login = await loginApi(request, USERS.manager)
  await applyLogin(page, login)

  for (const route of [
    '/admin/club/members',
    '/admin/club/applications',
    '/admin/activity/list',
    '/admin/activity/competition',
    '/admin/content/news',
    '/admin/content/notices',
    '/admin/content/works',
    '/admin/statistics/board'
  ]) {
    await page.goto(`${BASE_URL}${route}`, { waitUntil: 'domcontentloaded' })
    await expect(page.locator('main, .admin-page').first()).toBeVisible()
  }
})

test('teacher should access review routes and notice management', async ({ page, request }) => {
  test.setTimeout(120000)
  const login = await loginApi(request, USERS.teacher)
  await applyLogin(page, login)

  for (const route of [
    '/admin/activity/approval',
    '/admin/content/news-approval',
    '/admin/club/members',
    '/admin/statistics/board',
    '/admin/content/notices'
  ]) {
    await page.goto(`${BASE_URL}${route}`, { waitUntil: 'domcontentloaded' })
    await expect(page.locator('main, .admin-page').first()).toBeVisible()
  }
})

test('member should access core member routes', async ({ page, request }) => {
  test.setTimeout(120000)
  const login = await loginApi(request, USERS.member)
  await applyLogin(page, login)

  for (const route of [
    '/profile',
    '/news',
    '/activities',
    '/competition',
    '/works',
    '/notifications',
    '/contact',
    '/join'
  ]) {
    await page.goto(`${BASE_URL}${route}`, { waitUntil: 'domcontentloaded' })
    await expect(page.locator('body')).toBeVisible()
  }
})
