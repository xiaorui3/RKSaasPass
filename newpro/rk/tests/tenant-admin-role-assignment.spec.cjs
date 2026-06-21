// @ts-check
const { test, expect } = require('@playwright/test')
const { TENANT2_ADMIN } = require('./helpers/test-users.cjs')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'
const TENANT_ADMIN = TENANT2_ADMIN

async function loginApi(request, user) {
  const res = await request.post(`${API_BASE}/auth/login`, { data: user })
  const body = await res.json()
  if (body.code !== 200 || !body.data?.token) {
    throw new Error(`Login failed: ${JSON.stringify(body)}`)
  }
  return body.data
}

function buildHeaders(loginData) {
  return {
    Authorization: `Bearer ${loginData.token}`,
    'X-Tenant-Id': loginData.organizationId || '1'
  }
}

async function fetchRoleList(request, loginData) {
  const res = await request.get(`${API_BASE}/roles/list`, {
    headers: buildHeaders(loginData)
  })
  expect(res.status()).toBe(200)
  const body = await res.json()
  expect(body.code).toBe(200)
  return body.data || []
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

test('tenant admin role list should include manager and teacher roles for current tenant', async ({ request }) => {
  const login = await loginApi(request, TENANT_ADMIN)
  const roles = await fetchRoleList(request, login)
  const roleNames = roles.map((item) => item.name)

  expect(roleNames.some((name) => String(name).includes('绀惧洟璐熻矗'))).toBeTruthy()
  expect(roleNames).toContain('鎸囧鑰佸笀')
})

test('tenant admin should create a tenant teacher account that can open news approval', async ({ page, request }) => {
  const login = await loginApi(request, TENANT_ADMIN)
  const roles = await fetchRoleList(request, login)
  const teacherRole = roles.find((item) => item.code === 'TEACHER' || item.name === '鎸囧鑰佸笀')

  expect(teacherRole).toBeTruthy()

  const seed = Date.now()
  const username = `tenant2_teacher_${seed}`
  const password = 'Passw0rd!'
  const createRes = await request.post(`${API_BASE}/users`, {
    headers: buildHeaders(login),
    data: {
      tenantId: 2,
      username,
      password,
      name: `绉熸埛2鑰佸笀${seed}`,
      cellPhone: `139${String(seed).slice(-8)}`,
      email: `tenant2-teacher-${seed}@example.com`,
      type: 3,
      roleId: Number(teacherRole.id),
      status: 1
    }
  })

  expect(createRes.status()).toBe(200)

  const teacherLogin = await loginApi(request, {
    username,
    password,
    organizationId: '2'
  })

  await applyLogin(page, teacherLogin)
  await page.goto(`${BASE_URL}/admin/content/news-approval`, { waitUntil: 'networkidle' })
  await expect(page.getByText(/鏂伴椈瀹℃牳/).first()).toBeVisible()
})
