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

test('join page should switch to the selected tenant admission-form config', async ({ page, request }) => {
  const login = await loginApi(request, TENANT_ADMIN)
  const title = `tenant2-admission-${Date.now()}`

  const saveRes = await request.put(`${API_BASE}/api/admission/form-config/current`, {
    headers: {
      Authorization: `Bearer ${login.token}`,
      'X-Tenant-Id': '2'
    },
    data: {
      pageTitle: title,
      pageDescription: 'tenant2 description',
      successMessage: 'tenant2 success',
      fields: [
        { key: 'name', label: '濮撳悕', placeholder: '璇疯緭鍏ュ', enabled: true, required: true },
        { key: 'studentId', label: '瀛﹀彿', placeholder: '璇疯緭鍏ュ', enabled: true, required: true },
        { key: 'major', label: '涓撲笟', placeholder: '璇疯緭鍏ヤ笓', enabled: true, required: true },
        { key: 'grade', label: '骞寸骇', placeholder: '璇烽€夋嫨骞寸骇', enabled: true, required: true },
        { key: 'phone', label: '鑱旂郴鐢佃瘽', placeholder: '璇疯緭鍏ヨ仈绯荤數', enabled: true, required: true },
        { key: 'email', label: '閭', placeholder: '璇疯緭鍏ュ父鐢ㄩ偖', enabled: true, required: true },
        { key: 'interests', label: '鎶€鏈柟', placeholder: '璇烽€夋嫨鎶€鏈柟', enabled: true, required: true },
        { key: 'intro', label: '鑷垜浠嬬粛', placeholder: '璇峰～鍐欒嚜鎴戜粙', enabled: true, required: true }
      ]
    }
  })
  const saveBody = await saveRes.json()
  expect(saveBody.code).toBe(200)

  const tenantRes = await request.get(`${API_BASE}/tenants/list`)
  const tenantBody = await tenantRes.json()
  const tenant2 = (tenantBody.data || []).find((item) => String(item.id) === '2')
  expect(tenant2).toBeTruthy()

  await page.goto(`${BASE_URL}/join`, { waitUntil: 'domcontentloaded' })
  const tenantSelect = page.locator('.join-form .el-select').first()
  await tenantSelect.waitFor({ state: 'visible' })
  await tenantSelect.click()
  const listbox = page.locator('[role="listbox"]:visible').last()
  await listbox.waitFor({ state: 'visible' })
  await listbox.locator('[role="option"]').filter({ hasText: tenant2.tenantName }).first().click()
  await expect(page.locator('.page-header h1')).toContainText(title)
})
