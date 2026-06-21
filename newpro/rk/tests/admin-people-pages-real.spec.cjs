// @ts-check
const { test, expect } = require('@playwright/test')
const { SUPER_ADMIN } = require('./helpers/test-users.cjs')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'

const COPY = {
  usersTitle: '\u7528\u6237\u7ba1\u7406',
  usersDelta: '\u5dee\u989d\u8bf4\u660e',
  memberLedger: '\u6210\u5458\u53f0\u8d26',
  adminAccounts: '\u7ba1\u7406\u5458\u8d26\u53f7',
  teacherAccounts: '\u6307\u5bfc\u8001\u5e08\u8d26\u53f7',
  graphTitle: '\u6210\u5458\u5173\u7cfb\u56fe',
  graphFilter: '\u4eba\u5458\u8303\u56f4',
  graphSummary: '\u5f53\u524d\u56fe\u8c31\u4eba\u6570'
}

async function loginApi(request, user) {
  const res = await request.post(`${API_BASE}/auth/login`, { data: user })
  const body = await res.json()
  if (body.code !== 200 || !body.data?.token) {
    throw new Error(`login failed: ${JSON.stringify(body)}`)
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
      userType: data.userType,
      roles: data.roles || []
    }))
  }, loginData)
}

test.describe.configure({ timeout: 120000 })

test('admin users page should render people diagnostics with live data', async ({ page, request }) => {
  const login = await loginApi(request, SUPER_ADMIN)
  const consoleErrors = []
  page.on('console', (msg) => {
    if (msg.type() === 'error') {
      consoleErrors.push(msg.text())
    }
  })

  await applyLogin(page, login)
  const scopedPagePromise = page.waitForResponse((response) => response.url().includes('/users/page/scoped') && response.status() === 200)
  const scopedStatsPromise = page.waitForResponse((response) => response.url().includes('/users/statistics/scoped') && response.status() === 200)
  const memberStatsPromise = page.waitForResponse((response) => response.url().includes('/api/members/statistics') && response.status() === 200)
  await page.goto(`${BASE_URL}/admin/system/users`, { waitUntil: 'domcontentloaded' })
  await Promise.all([scopedPagePromise, scopedStatsPromise, memberStatsPromise])

  const main = page.getByRole('main')
  await expect(main.getByText(COPY.usersTitle)).toBeVisible()
  await expect(main.getByText(COPY.usersDelta)).toBeVisible()
  await expect(main.getByText(COPY.memberLedger, { exact: true })).toBeVisible()
  await expect(main.getByText(COPY.adminAccounts, { exact: true })).toBeVisible()
  await expect(main.getByText(COPY.teacherAccounts, { exact: true })).toBeVisible()

  expect(consoleErrors).toEqual([])
})

test('member graph page should render filters and graph shell with live data', async ({ page, request }) => {
  const login = await loginApi(request, SUPER_ADMIN)
  const consoleErrors = []
  page.on('console', (msg) => {
    if (msg.type() === 'error') {
      consoleErrors.push(msg.text())
    }
  })

  await applyLogin(page, login)
  const membersPromise = page.waitForResponse((response) => response.url().includes('/api/members/list') && response.status() === 200)
  const alumniPromise = page.waitForResponse((response) => response.url().includes('/api/alumni/list') && response.status() === 200)
  await page.goto(`${BASE_URL}/admin/club/member-graph`, { waitUntil: 'domcontentloaded' })
  await Promise.all([membersPromise, alumniPromise])

  await expect(page.getByRole('heading', { name: COPY.graphTitle })).toBeVisible()
  await expect(page.getByText(COPY.graphFilter, { exact: true })).toBeVisible()
  await expect(page.getByText(COPY.graphSummary, { exact: true })).toBeVisible()
  await expect(page.locator('.graph-container')).toBeVisible()

  expect(consoleErrors).toEqual([])
})
