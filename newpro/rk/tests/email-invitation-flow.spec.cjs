// @ts-check
const { test, expect } = require('@playwright/test')
const mysql = require('mysql2/promise')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'

const MANAGER = {
  username: 'manager_a',
  password: '123456',
  organizationId: '1'
}

async function loginToken(request, user) {
  const response = await request.post(`${API_BASE}/auth/login`, {
    data: {
      username: user.username,
      password: user.password,
      organizationId: user.organizationId
    }
  })
  const body = await response.json()
  if (body.code !== 200 || !body.data?.token) {
    throw new Error(`Login failed: ${JSON.stringify(body)}`)
  }
  return body.data
}

async function queryLatestInvitation(email) {
  const connection = await mysql.createConnection({
    host: '127.0.0.1',
    port: 3306,
    user: 'root',
    password: '123',
    database: 'rk_user'
  })
  try {
    const [rows] = await connection.query(
      'SELECT invite_token AS inviteToken, referral_code AS referralCode, target_email AS targetEmail FROM email_invitation WHERE target_email = ? ORDER BY id DESC LIMIT 1',
      [email]
    )
    return rows[0] || null
  } finally {
    await connection.end()
  }
}

test('register invitation should carry referral code', async ({ page, request }) => {
  const seed = Date.now()
  const inviteEmail = `invite_${seed}@example.com`
  const referralCode = `REF${seed}`
  const login = await loginToken(request, MANAGER)

  const createCodeRes = await request.post(`${API_BASE}/api/referral-codes`, {
    headers: {
      Authorization: `Bearer ${login.token}`,
      'X-Tenant-Id': String(login.organizationId),
      'Content-Type': 'application/json'
    },
    data: {
      code: referralCode,
      maxUses: 5,
      status: 1
    }
  })
  const createCodeBody = await createCodeRes.json()
  expect(createCodeBody.code).toBe(200)

  const sendRes = await request.post(`${API_BASE}/api/email-center/invitations/send`, {
    headers: {
      Authorization: `Bearer ${login.token}`,
      'X-Tenant-Id': String(login.organizationId),
      'Content-Type': 'application/json'
    },
    data: {
      manualEmails: [inviteEmail],
      invitationType: 'REGISTER',
      subject: `register invite ${seed}`,
      content: 'register invite content',
      referralCode
    }
  })
  const sendBody = await sendRes.json()
  expect(sendBody.code).toBe(200)

  const invitation = await queryLatestInvitation(inviteEmail)
  expect(invitation).toBeTruthy()
  expect(invitation.referralCode).toBe(referralCode)

  const detailRes = await request.get(`${API_BASE}/api/email-center/invitations/${invitation.inviteToken}`)
  const detailBody = await detailRes.json()
  expect(detailBody.code).toBe(200)
  expect(detailBody.data?.targetEmail).toBe(inviteEmail)
  expect(detailBody.data?.referralCode).toBe(referralCode)

  await page.goto(`${BASE_URL}/register?inviteToken=${invitation.inviteToken}`, { waitUntil: 'domcontentloaded' })
  await expect(page).toHaveURL(/inviteToken=/)
  await expect(page.locator('body')).toBeVisible()
})
