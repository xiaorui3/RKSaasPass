// @ts-check
const { test, expect } = require('@playwright/test')

const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'
const MANAGER = { username: 'manager_a', password: '123456', organizationId: '1' }

async function loginApi(request, user) {
  const res = await request.post(`${API_BASE}/auth/login`, { data: user })
  const body = await res.json()
  if (body.code !== 200 || !body.data?.token) {
    throw new Error(`Login failed: ${JSON.stringify(body)}`)
  }
  return body.data
}

async function resolveRecipients(request, token, roleId) {
  const res = await request.post(`${API_BASE}/api/email-center/recipients/resolve`, {
    headers: {
      Authorization: `Bearer ${token}`,
      'X-Tenant-Id': '1',
      'Content-Type': 'application/json'
    },
    data: { roleIds: [roleId] }
  })
  expect(res.status()).toBe(200)
  const body = await res.json()
  expect(body.code).toBe(200)
  return body.data || []
}

test('email center role preview should resolve manager and teacher recipients with emails', async ({ request }) => {
  const login = await loginApi(request, MANAGER)

  const managerRecipients = await resolveRecipients(request, login.token, 7)
  const teacherRecipients = await resolveRecipients(request, login.token, 8)

  expect(managerRecipients.length).toBeGreaterThan(0)
  expect(teacherRecipients.length).toBeGreaterThan(0)
})
