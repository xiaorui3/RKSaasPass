// @ts-check
const { test, expect } = require('@playwright/test')
const { TENANT2_ADMIN } = require('./helpers/test-users.cjs')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'
const TENANT1_MANAGER = { username: 'manager_a', password: '123456', organizationId: '1' }
const TENANT1_MEMBER = { username: 'member_a', password: '123456', organizationId: '1' }

function buildDefaultFields() {
  return [
    { key: 'name', label: '濮撳悕', placeholder: '璇疯緭鍏ュ鍚?, enabled: true, required: true },
    { key: 'studentId', label: '瀛﹀彿', placeholder: '璇疯緭鍏ュ鍙?, enabled: true, required: true },
    { key: 'major', label: '涓撲笟', placeholder: '璇疯緭鍏ヤ笓涓?, enabled: true, required: true },
    { key: 'grade', label: '骞寸骇', placeholder: '璇烽€夋嫨骞寸骇', enabled: true, required: true },
    { key: 'phone', label: '鑱旂郴鐢佃瘽', placeholder: '璇疯緭鍏ヨ仈绯荤數璇?, enabled: true, required: true },
    { key: 'email', label: '閭', placeholder: '璇疯緭鍏ュ父鐢ㄩ偖绠?, enabled: true, required: true },
    { key: 'interests', label: '鎶€鏈柟鍚?, placeholder: '璇烽€夋嫨鑷冲皯涓€涓妧鏈柟鍚?, enabled: true, required: true },
    { key: 'intro', label: '鑷垜浠嬬粛', placeholder: '璇蜂粙缁嶄綘鐨勬妧鏈儗鏅€佸弬涓庣粡鍘嗕互鍙婂姞鍏ュ師鍥?, enabled: true, required: true }
  ]
}

async function loginApi(request, user) {
  const res = await request.post(`${API_BASE}/auth/login`, {
    data: {
      username: user.username,
      password: user.password,
      organizationId: user.organizationId
    }
  })
  const body = await res.json()
  if (body.code !== 200 || !body.data?.token) {
    throw new Error(`Login failed: ${JSON.stringify(body)}`)
  }
  return body.data
}

function authHeaders(loginData, tenantId) {
  return {
    Authorization: `Bearer ${loginData.token}`,
    'X-Tenant-Id': String(tenantId),
    'Content-Type': 'application/json'
  }
}

async function saveAdmissionConfig(request, loginData, tenantId, config) {
  const res = await request.put(`${API_BASE}/api/admission/form-config/current`, {
    headers: authHeaders(loginData, tenantId),
    data: config
  })
  const body = await res.json()
  expect(body.code).toBe(200)
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

async function selectTenant(page, tenantName) {
  const select = page.locator('.el-select').first()
  await select.waitFor({ state: 'visible' })
  await select.click()
  const listbox = page.locator('[role="listbox"]:visible').last()
  await listbox.waitFor({ state: 'visible' })
  const option = listbox.locator('[role="option"]').filter({ hasText: tenantName }).first()
  await option.waitFor({ state: 'visible' })
  await option.click()
}

async function expectWorkflowHero(page, title, description) {
  await expect(page.locator('.page-header h1').first()).toContainText(title)
  await expect(page.locator('.page-header p').first()).toContainText(description)
}

test('manager can update admission form config and guest join page reflects it', async ({ browser, request }) => {
  const marker = `绉熸埛鍏ョぞ閰嶇疆-${Date.now()}`
  const description = `${marker} 璇存槑`
  const managerLogin = await loginApi(request, TENANT1_MANAGER)

  await saveAdmissionConfig(request, managerLogin, 1, {
    pageTitle: marker,
    pageDescription: description,
    successMessage: '鐢宠鎻愪氦鎴愬姛锛岃绛夊緟璐熻矗浜哄鏍搞€?,
    fields: buildDefaultFields()
  })

  const tenantListRes = await request.get(`${API_BASE}/tenants/list`)
  const tenantListBody = await tenantListRes.json()
  const tenant1 = (tenantListBody.data || []).find((item) => String(item.id) === '1')
  expect(tenant1).toBeTruthy()

  const guestContext = await browser.newContext()
  const guestPage = await guestContext.newPage()
  await guestPage.goto(`${BASE_URL}/join`, { waitUntil: 'domcontentloaded' })
  await selectTenant(guestPage, tenant1.tenantName)
  await expectWorkflowHero(guestPage, marker, description)
  await guestContext.close()
})

test('logged-in user selecting another tenant should see that tenant admission form config', async ({ browser, request }) => {
  const marker = `璺ㄧ鎴疯〃鍗?${Date.now()}`
  const description = `${marker} 璇存槑`
  const tenant2Login = await loginApi(request, TENANT2_ADMIN)

  await saveAdmissionConfig(request, tenant2Login, 2, {
    pageTitle: marker,
    pageDescription: description,
    successMessage: '鐢宠鎻愪氦鎴愬姛锛岃绛夊緟璐熻矗浜哄鏍搞€?,
    fields: buildDefaultFields()
  })

  const memberLogin = await loginApi(request, TENANT1_MEMBER)
  const tenantListRes = await request.get(`${API_BASE}/tenants/list`)
  const tenantListBody = await tenantListRes.json()
  const tenant2 = (tenantListBody.data || []).find((item) => String(item.id) === '2')
  expect(tenant2).toBeTruthy()

  const memberContext = await browser.newContext()
  const memberPage = await memberContext.newPage()
  await applyLogin(memberPage, memberLogin)
  await memberPage.goto(`${BASE_URL}/join`, { waitUntil: 'domcontentloaded' })
  await selectTenant(memberPage, tenant2.tenantName)
  await expectWorkflowHero(memberPage, marker, description)
  await memberContext.close()
})

test('custom admission field should be visible in register and join pages', async ({ browser, request }) => {
  const customLabel = `鎶€鑳芥柟鍚?${Date.now()}`
  const managerLogin = await loginApi(request, TENANT1_MANAGER)

  await saveAdmissionConfig(request, managerLogin, 1, {
    pageTitle: '鍔犲叆鎴戜滑',
    pageDescription: '璇峰～鍐欎俊鎭悗鎻愪氦鐢宠',
    successMessage: '鐢宠鎻愪氦鎴愬姛锛岃绛夊緟璐熻矗浜哄鏍搞€?,
    fields: [
      ...buildDefaultFields(),
      {
        key: `direction_${Date.now()}`,
        label: customLabel,
        placeholder: '璇烽€夋嫨',
        enabled: true,
        required: false,
        componentType: 'select',
        options: [
          { label: '浜у搧', value: 'product' },
          { label: '鐮斿彂', value: 'engineering' }
        ]
      }
    ]
  })

  const registerContext = await browser.newContext()
  const registerPage = await registerContext.newPage()
  await registerPage.goto(`${BASE_URL}/register`, { waitUntil: 'domcontentloaded' })
  await expect(registerPage.locator('.el-form-item__label').filter({ hasText: customLabel }).first()).toBeVisible()
  await registerContext.close()

  const joinContext = await browser.newContext()
  const joinPage = await joinContext.newPage()
  await joinPage.goto(`${BASE_URL}/join`, { waitUntil: 'domcontentloaded' })
  await expect(joinPage.locator('.el-form-item__label').filter({ hasText: customLabel }).first()).toBeVisible()
  await joinContext.close()
})
