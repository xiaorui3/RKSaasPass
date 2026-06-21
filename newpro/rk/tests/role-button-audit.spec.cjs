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

async function prepareAdminPage(page, request, user) {
  const login = await loginApi(request, user)
  page.on('dialog', async (dialog) => {
    await dialog.accept()
  })
  await applyLogin(page, login)
  await page.goto(`${BASE_URL}/admin`, { waitUntil: 'domcontentloaded' })
}

test('super admin should see real ops actions including K8s and backup controls', async ({ page, request }) => {
  await prepareAdminPage(page, request, USERS.admin)

  await page.goto(`${BASE_URL}/admin/operation/k8s`, { waitUntil: 'domcontentloaded' })
  await expect(page.getByText(/K8s 绠＄悊|Kubernetes/).first()).toBeVisible()

  await page.goto(`${BASE_URL}/admin/system/users`, { waitUntil: 'domcontentloaded' })
  await expect(page.getByRole('button', { name: /鏂板鐢ㄦ埛/ })).toBeVisible()
  await expect(page.getByRole('button', { name: /Excel瀵煎叆/ })).toBeVisible()

  await page.goto(`${BASE_URL}/admin/operation/backup`, { waitUntil: 'domcontentloaded' })
  await expect(page.getByRole('button', { name: /绔嬪嵆澶囦唤/ })).toBeVisible()
  await expect(page.getByRole('button', { name: /瀹氭椂绛栫暐/ })).toBeVisible()
})

test('manager should see club management actions but be blocked from system and ops routes', async ({ page, request }) => {
  await prepareAdminPage(page, request, USERS.manager)

  await page.goto(`${BASE_URL}/admin/activity/list`, { waitUntil: 'domcontentloaded' })
  await expect(page.getByRole('button', { name: /鏂板娲诲姩/ })).toBeVisible()

  await page.goto(`${BASE_URL}/admin/content/news`, { waitUntil: 'domcontentloaded' })
  await expect(page.getByRole('button', { name: /鍙戝竷鏂伴椈/ })).toBeVisible()

  await page.goto(`${BASE_URL}/admin/content/notices`, { waitUntil: 'domcontentloaded' })
  await expect(page.getByRole('button', { name: /鍙戝竷鍏憡/ })).toBeVisible()

  await page.goto(`${BASE_URL}/admin/system/users`, { waitUntil: 'domcontentloaded' })
  await expect(page).not.toHaveURL(/\/admin\/system\/users$/)

  await page.goto(`${BASE_URL}/admin/operation/tasks`, { waitUntil: 'domcontentloaded' })
  await expect(page).not.toHaveURL(/\/admin\/operation\/tasks$/)
})

test('teacher should see approval and notice menu, but members page must stay read-only', async ({ page, request }) => {
  await prepareAdminPage(page, request, USERS.teacher)

  await page.goto(`${BASE_URL}/admin/activity/approval`, { waitUntil: 'domcontentloaded' })
  await expect(page.getByText(/娲诲姩瀹℃壒/).first()).toBeVisible()

  await page.goto(`${BASE_URL}/admin/content/news-approval`, { waitUntil: 'domcontentloaded' })
  await expect(page.getByText(/鏂伴椈瀹℃牳/).first()).toBeVisible()

  await page.goto(`${BASE_URL}/admin/content/notices`, { waitUntil: 'domcontentloaded' })
  await expect(page.getByRole('button', { name: /鍙戝竷鍏憡/ })).toBeVisible()

  await page.goto(`${BASE_URL}/admin/club/members`, { waitUntil: 'domcontentloaded' })
  await expect(page.getByRole('button', { name: /鏂板鎴愬憳/ })).toHaveCount(0)

  await page.goto(`${BASE_URL}/admin/system/users`, { waitUntil: 'domcontentloaded' })
  await expect(page).not.toHaveURL(/\/admin\/system\/users$/)
})

test('member should keep frontend access but be blocked from admin routes', async ({ page, request }) => {
  const login = await loginApi(request, USERS.member)
  page.on('dialog', async (dialog) => {
    await dialog.accept()
  })
  await applyLogin(page, login)

  await page.goto(`${BASE_URL}/notifications`, { waitUntil: 'domcontentloaded' })
  await expect(page.getByText(/娑堟伅閫氱煡|閫氱煡涓績/).first()).toBeVisible()

  await page.goto(`${BASE_URL}/join`, { waitUntil: 'domcontentloaded' })
  await expect(page.getByText(/鍔犲叆娴佺▼|鎻愪氦鐢宠/).first()).toBeVisible()

  await page.goto(`${BASE_URL}/admin/activity/list`, { waitUntil: 'domcontentloaded' })
  await expect(page).not.toHaveURL(/\/admin\/activity\/list$/)
})
