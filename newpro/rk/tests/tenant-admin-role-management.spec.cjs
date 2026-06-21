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

async function applyLogin(page, loginData, adminMenus = []) {
  await page.addInitScript((data) => {
    localStorage.setItem('token', data.token)
    localStorage.setItem('tenantId', data.organizationId)
    localStorage.setItem('userInfo', JSON.stringify({
      userId: data.userId,
      username: data.username,
      organizationId: data.organizationId,
      roles: data.roles || []
    }))
    localStorage.setItem('adminMenus', JSON.stringify(data.adminMenus || []))
  }, { ...loginData, adminMenus })
}

test('tenant admin should fetch current menu tree without 500', async ({ request }) => {
  const login = await loginApi(request, TENANT_ADMIN)
  const res = await request.get(`${API_BASE}/menus/me`, {
    headers: buildHeaders(login)
  })

  expect(res.status()).toBe(200)
  const body = await res.json()
  expect(body.code).toBe(200)
  expect(Array.isArray(body.data)).toBeTruthy()
})

test('tenant admin should access role management page', async ({ page, request }) => {
  const login = await loginApi(request, TENANT_ADMIN)
  await applyLogin(page, login)

  await page.goto(`${BASE_URL}/admin/system/roles`, { waitUntil: 'networkidle' })
  await expect(page.getByText(/瑙掕壊绠＄悊/).first()).toBeVisible()
})

test('tenant admin should create a custom role and persist menu bindings inside current tenant', async ({ request }) => {
  const login = await loginApi(request, TENANT_ADMIN)
  const headers = buildHeaders(login)
  const seed = Date.now()

  const createRes = await request.post(`${API_BASE}/roles`, {
    headers,
    data: {
      name: `绉熸埛浜岃嚜瀹氫箟瑙掕壊${seed}`,
      code: `tenant2_custom_${seed}`
    }
  })

  expect(createRes.status()).toBe(200)
  const createBody = await createRes.json()
  expect(createBody.id || createBody.data?.id).toBeTruthy()
  const roleId = Number(createBody.id || createBody.data?.id)

  const menuRes = await request.get(`${API_BASE}/menus/me`, { headers })
  expect(menuRes.status()).toBe(200)
  const menuBody = await menuRes.json()
  expect(menuBody.code).toBe(200)

  const walk = (rows) => rows.flatMap((item) => [item, ...walk(item.subMenus || [])])
  const flatMenus = walk(menuBody.data || [])
  const leafMenu = flatMenus.find((item) => item.id && (!item.subMenus || item.subMenus.length === 0))
  expect(leafMenu).toBeTruthy()

  const bindRes = await request.post(`${API_BASE}/menus/role/${roleId}`, {
    headers,
    data: [leafMenu.id]
  })
  expect(bindRes.status()).toBe(200)

  const roleMenuRes = await request.get(`${API_BASE}/menus/role/${roleId}`, { headers })
  expect(roleMenuRes.status()).toBe(200)
  const roleMenuBody = await roleMenuRes.json()
  expect(roleMenuBody.code).toBe(200)
  expect(roleMenuBody.data).toContain(leafMenu.id)
})

test('custom tenant role user should access the admin page that was bound to the role menu', async ({ page, request }) => {
  const adminLogin = await loginApi(request, TENANT_ADMIN)
  const headers = buildHeaders(adminLogin)
  const seed = Date.now()

  const createRoleRes = await request.post(`${API_BASE}/roles`, {
    headers,
    data: {
      name: `绉熸埛浜岃繍琛屾€佽?{seed}`,
      code: `tenant2_runtime_${seed}`
    }
  })
  const createRoleBody = await createRoleRes.json()
  const roleId = Number(createRoleBody.id || createRoleBody.data?.id)
  expect(roleId).toBeTruthy()

  const menuRes = await request.get(`${API_BASE}/menus/me`, { headers })
  const menuBody = await menuRes.json()
  const walk = (rows) => rows.flatMap((item) => [item, ...walk(item.subMenus || [])])
  const flatMenus = walk(menuBody.data || [])
  const targetMenu = flatMenus.find((item) => item.id && item.path && String(item.path).startsWith('/admin'))
  expect(targetMenu).toBeTruthy()

  await request.post(`${API_BASE}/menus/role/${roleId}`, {
    headers,
    data: [targetMenu.id]
  })

  const username = `tenant2_runtime_user_${seed}`
  const password = 'Passw0rd!'
  const createUserRes = await request.post(`${API_BASE}/users`, {
    headers,
    data: {
      tenantId: 2,
      username,
      password,
      name: `绉熸埛2杩愯鎬佺敤?{seed}`,
      cellPhone: `137${String(seed).slice(-8)}`,
      email: `${username}@example.com`,
      type: 1,
      roleId,
      status: 1
    }
  })
  expect(createUserRes.status()).toBe(200)

  const userLogin = await loginApi(request, {
    username,
    password,
    organizationId: '2'
  })
  const userMenuRes = await request.get(`${API_BASE}/menus/me`, {
    headers: buildHeaders(userLogin)
  })
  const userMenuBody = await userMenuRes.json()
  await applyLogin(page, userLogin, userMenuBody.data || [])

  await page.goto(`${BASE_URL}${targetMenu.path}`, { waitUntil: 'networkidle' })
  await expect(page).toHaveURL(new RegExp(`${String(targetMenu.path).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}$`))
  await expect(page.getByText(new RegExp(targetMenu.label)).first()).toBeVisible()
})
