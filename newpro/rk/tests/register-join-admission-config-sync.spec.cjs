// @ts-check
const { test, expect } = require('@playwright/test')
const { SUPER_ADMIN } = require('./helpers/test-users.cjs')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'
const ADMIN = SUPER_ADMIN

async function loginApi(request, user) {
  const res = await request.post(`${API_BASE}/auth/login`, { data: user })
  const body = await res.json()
  if (body.code !== 200 || !body.data?.token) {
    throw new Error(`Login failed: ${JSON.stringify(body)}`)
  }
  return body.data
}

async function selectTenant(page, tenantName) {
  const select = page.locator('.el-select').first()
  if (await select.count() === 0) return
  await select.waitFor({ state: 'visible' })
  await select.click()
  const listbox = page.locator('[role="listbox"]:visible').last()
  if (await listbox.count() === 0) {
    const input = select.locator('input').first()
    await input.press('ArrowDown')
    await input.press('Enter')
    return
  }
  await listbox.waitFor({ state: 'visible' })
  const target = listbox.locator('[role="option"]').filter({ hasText: tenantName }).first()
  if (await target.count()) {
    await target.click({ force: true })
  } else {
    await listbox.locator('[role="option"]').first().click({ force: true })
  }
}

test('register page and join page should both reflect the selected tenant admission copy', async ({ page, request }) => {
  const login = await loginApi(request, ADMIN)
  const title = `tenant-copy-${Date.now()}`
  const headers = {
    Authorization: `Bearer ${login.token}`,
    'X-Tenant-Id': login.organizationId || '1'
  }

  const currentRes = await request.get(`${API_BASE}/api/admission/form-config/current`, { headers })
  const currentBody = await currentRes.json()
  const savePayload = {
    ...(currentBody.data || {}),
    pageTitle: title,
    pageDescription: `${title}-description`
  }
  const saveRes = await request.put(`${API_BASE}/api/admission/form-config/current`, {
    headers,
    data: savePayload
  })
  expect(saveRes.ok()).toBeTruthy()
  const tenantsRes = await request.get(`${API_BASE}/tenants/list`)
  const tenantsBody = await tenantsRes.json()
  const tenant = (tenantsBody.data || []).find((item) => String(item.id) === '1') || (tenantsBody.data || [])[0]
  expect(tenant).toBeTruthy()
  const publicCfgRes = await request.get(`${API_BASE}/api/admission/form-config/public?tenantId=${tenant.id}`)
  const publicCfgBody = await publicCfgRes.json()
  expect(publicCfgBody.code).toBe(200)
  expect(publicCfgBody.data?.pageTitle).toBe(title)

  await page.goto(`${BASE_URL}/register`, { waitUntil: 'domcontentloaded' })
  await selectTenant(page, tenant.tenantName)
  await expect(page.locator('.register-card, .register-title').first()).toBeVisible()

  await page.goto(`${BASE_URL}/join`, { waitUntil: 'domcontentloaded' })
  await selectTenant(page, tenant.tenantName)
  await expect(page.locator('.join-form, .page-header').first()).toBeVisible()
})

test('register page and join page should render the same enabled builtin and custom admission fields', async ({ browser, request }) => {
  const login = await loginApi(request, ADMIN)
  const marker = `admission-sync-${Date.now()}`
  const headers = {
    Authorization: `Bearer ${login.token}`,
    'X-Tenant-Id': login.organizationId || '1'
  }

  const saveRes = await request.put(`${API_BASE}/api/admission/form-config/current`, {
    headers,
    data: {
      pageTitle: marker,
      pageDescription: `${marker}-description`,
      successMessage: '鐢宠鎻愪氦鎴愬姛锛岃绛夊緟璐熻矗浜哄',
      fields: [
        { key: 'name', source: 'builtin', type: 'text', label: '鐪熷疄濮撳悕', placeholder: '璇疯緭鍏ョ湡瀹炲', enabled: true, required: true, sort: 10, options: [] },
        { key: 'studentId', source: 'builtin', type: 'text', label: '瀛﹀彿', placeholder: '璇疯緭鍏ュ', enabled: true, required: true, sort: 20, options: [] },
        { key: 'college', source: 'builtin', type: 'text', label: '瀛﹂櫌', placeholder: '璇疯緭鍏ユ墍鍦ㄥ', enabled: true, required: false, sort: 30, options: [] },
        { key: 'major', source: 'builtin', type: 'text', label: '涓撲笟', placeholder: '璇疯緭鍏ヤ笓', enabled: true, required: true, sort: 40, options: [] },
        { key: 'grade', source: 'builtin', type: 'select', label: '骞寸骇', placeholder: '璇烽€夋嫨骞寸骇', enabled: true, required: true, sort: 50, options: [
          { label: '澶т竴', value: '1' },
          { label: '澶т簩', value: '2' },
          { label: '澶т笁', value: '3' },
          { label: '澶у洓', value: '4' }
        ] },
        { key: 'phone', source: 'builtin', type: 'text', label: '鑱旂郴鐢佃瘽', placeholder: '璇疯緭鍏ヨ仈绯荤數', enabled: false, required: false, sort: 60, options: [] },
        { key: 'email', source: 'builtin', type: 'text', label: '鑱旂郴閭', placeholder: '璇疯緭鍏ヨ仈绯婚偖', enabled: true, required: true, sort: 70, options: [] },
        { key: 'interests', source: 'builtin', type: 'checkbox', label: '鎶€鏈柟', placeholder: '璇烽€夋嫨鎶€鏈柟', enabled: true, required: true, sort: 80, options: [
          { label: '鍓嶇寮€', value: 'frontend' },
          { label: '鍚庣寮€', value: 'backend' }
        ] },
        { key: 'positionIntent', source: 'builtin', type: 'text', label: '鑱屼綅鎰忓悜', placeholder: '璇疯緭鍏ヨ亴浣嶆剰', enabled: true, required: false, sort: 90, options: [] },
        { key: 'specialty', source: 'builtin', type: 'textarea', label: '涓汉鐗归暱', placeholder: '璇疯緭鍏ヤ釜浜虹壒', enabled: true, required: false, sort: 100, options: [] },
        { key: 'availableTime', source: 'builtin', type: 'text', label: '鍙姇鍏ユ椂', placeholder: '璇疯緭鍏ュ彲鎶曞叆鏃堕棿', enabled: true, required: false, sort: 110, options: [] },
        { key: 'portfolioUrl', source: 'builtin', type: 'url', label: '浣滃搧閾炬帴', placeholder: '璇疯緭鍏ヤ綔鍝侀摼', enabled: true, required: false, sort: 120, options: [] },
        { key: 'intro', source: 'builtin', type: 'textarea', label: '鑷垜浠嬬粛', placeholder: '璇蜂粙缁嶄綘鐨勬妧鏈儗', enabled: true, required: true, sort: 130, options: [] },
        { key: 'remark', source: 'builtin', type: 'textarea', label: '琛ュ厖璇存槑', placeholder: '鍙ˉ鍏呭叾浠栬', enabled: true, required: false, sort: 140, options: [] },
        { key: 'skillDirection', source: 'custom', type: 'select', label: '鎶€鑳芥柟', placeholder: '璇烽€夋嫨鎶€鑳芥柟', enabled: true, required: true, sort: 150, options: [
          { label: '浜у搧', value: 'product' },
          { label: '鐮斿彂', value: 'engineering' }
        ] }
      ]
    }
  })
  expect(saveRes.ok()).toBeTruthy()
  const tenantsRes = await request.get(`${API_BASE}/tenants/list`)
  const tenantsBody = await tenantsRes.json()
  const tenant = (tenantsBody.data || []).find((item) => String(item.id) === '1') || (tenantsBody.data || [])[0]
  expect(tenant).toBeTruthy()
  const publicCfgRes = await request.get(`${API_BASE}/api/admission/form-config/public?tenantId=${tenant.id}`)
  const publicCfgBody = await publicCfgRes.json()
  expect(publicCfgBody.code).toBe(200)
  expect(publicCfgBody.data?.pageTitle).toBe(marker)

  const registerContext = await browser.newContext()
  const registerPage = await registerContext.newPage()
  await registerPage.goto(`${BASE_URL}/register`, { waitUntil: 'domcontentloaded' })
  await selectTenant(registerPage, tenant.tenantName)
  await expect(registerPage.locator('.register-card, .register-title, form').first()).toBeVisible()

  const joinContext = await browser.newContext()
  const joinPage = await joinContext.newPage()
  await joinPage.goto(`${BASE_URL}/join`, { waitUntil: 'domcontentloaded' })
  await selectTenant(joinPage, tenant.tenantName)
  await expect(joinPage.locator('.join-form, .page-header, form').first()).toBeVisible()

  await registerContext.close()
  await joinContext.close()
})
