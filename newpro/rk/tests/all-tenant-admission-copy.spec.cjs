// @ts-check
const { test, expect } = require('@playwright/test')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'

function textFromCodes(codes) {
  return String.fromCodePoint(...codes)
}

async function selectTenant(page, formSelector, tenantName) {
  const select = formSelector
    ? page.locator(`${formSelector} .el-select`).first()
    : page.locator('.el-select').first()
  await select.waitFor({ state: 'visible' })
  await select.click()
  const listbox = page.locator('[role="listbox"]:visible').last()
  await listbox.waitFor({ state: 'visible' })
  const option = listbox.locator('[role="option"]').filter({ hasText: tenantName }).first()
  await option.waitFor({ state: 'visible' })
  await option.click()
}

test('all tenants should expose non-default admission copy and login/register/join should stay in sync', async ({ page, request }) => {
  const tenantRes = await request.get(`${API_BASE}/tenants/list`)
  const tenantBody = await tenantRes.json()
  expect(tenantBody.code).toBe(200)

  const tenants = tenantBody.data || []
  const sampleTenants = tenants.slice(0, Math.min(3, tenants.length)).map((tenant) => ({
    id: String(tenant.id),
    tenantName: tenant.tenantName
  }))
  expect(sampleTenants.length).toBeGreaterThan(0)

  const legacyDefaultTitle = textFromCodes([0x9354, 0x72b2, 0x53c6, 0x93b4, 0x621c, 0x6ed1])
  const legacyDefaultDescription = textFromCodes([
    0x95ab, 0x590b, 0x5ae8, 0x9429, 0xe1bd, 0x7223,
    0x7ec0, 0x60e7, 0x6d1f, 0x935a, 0x5ea2, 0x5f41,
    0x6d5c, 0x3085, 0x53c6, 0x7ec0, 0x5267, 0x6575
  ])
  const copySignatures = []
  for (const tenant of tenants) {
    const cfgRes = await request.get(`${API_BASE}/api/admission/form-config/public?tenantId=${tenant.id}`)
    const cfgBody = await cfgRes.json()
    expect(cfgBody.code).toBe(200)
    expect(cfgBody.data?.pageTitle).not.toBe(legacyDefaultTitle)
    expect(cfgBody.data?.pageDescription).not.toContain(legacyDefaultDescription)
    copySignatures.push(`${cfgBody.data?.pageTitle}|${cfgBody.data?.pageDescription}`)
  }

  expect(new Set(copySignatures).size).toBeGreaterThan(0)

  for (const tenant of sampleTenants) {
    const cfgRes = await request.get(`${API_BASE}/api/admission/form-config/public?tenantId=${tenant.id}`)
    const cfgBody = await cfgRes.json()
    expect(cfgBody.code).toBe(200)

    await page.goto(`${BASE_URL}/login`, { waitUntil: 'domcontentloaded' })
    await selectTenant(page, null, tenant.tenantName)
    await expect(page.locator('.login-title')).toBeVisible()
    await expect(page.locator('.login-subtitle')).toBeVisible()

    await page.goto(`${BASE_URL}/register`, { waitUntil: 'domcontentloaded' })
    await selectTenant(page, null, tenant.tenantName)
    await expect(page.locator('.register-title')).toBeVisible()
    await expect(page.locator('.register-subtitle')).toBeVisible()

    await page.goto(`${BASE_URL}/join`, { waitUntil: 'domcontentloaded' })
    await selectTenant(page, '.join-form', tenant.tenantName)
    await expect(page.locator('.page-header h1')).toContainText(cfgBody.data.pageTitle)
    await expect(page.locator('.page-header p').first()).toContainText(cfgBody.data.pageDescription)
  }
})
