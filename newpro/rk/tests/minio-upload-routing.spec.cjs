// @ts-check
const { test, expect } = require('@playwright/test')

const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'
const MEMBER = { username: 'member_a', password: '123456', organizationId: '1' }
const MANAGER = { username: 'manager_a', password: '123456', organizationId: '1' }

function tinyPng(name = 'probe.png') {
  return {
    name,
    mimeType: 'image/png',
    buffer: Buffer.from(
      'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jX4kAAAAASUVORK5CYII=',
      'base64'
    )
  }
}

async function loginApi(request, user) {
  const res = await request.post(`${API_BASE}/auth/login`, { data: user })
  const body = await res.json()
  expect(body.code, `login should succeed for ${user.username}`).toBe(200)
  expect(body.data?.token, `token should exist for ${user.username}`).toBeTruthy()
  return body.data.token
}

test('gateway /api/files/upload should return MinIO-backed file url', async ({ request }) => {
  const token = await loginApi(request, MEMBER)

  const res = await request.post(`${API_BASE}/api/files/upload`, {
    headers: {
      Authorization: `Bearer ${token}`,
      'X-Tenant-Id': '1'
    },
    multipart: {
      file: tinyPng('avatar-probe.png'),
      service: 'rk-user',
      bizType: 'avatar'
    }
  })

  const body = await res.json()
  expect(body.code).toBe(200)

  const uploadedUrl = body.data?.fileUrl || body.data?.url || body.data?.filePath || ''
  expect(uploadedUrl).toMatch(/^https?:\/\/.+\/rk-user\/avatar\//)
})

test('gateway unified upload should return MinIO-backed content image url', async ({ request }) => {
  const token = await loginApi(request, MANAGER)

  const res = await request.post(`${API_BASE}/api/files/upload`, {
    headers: {
      Authorization: `Bearer ${token}`,
      'X-Tenant-Id': '1'
    },
    multipart: {
      file: tinyPng('news-cover.png'),
      service: 'rk-content',
      bizType: 'news-image'
    }
  })

  const body = await res.json()
  expect(body.code).toBe(200)

  const uploadedUrl = body.data?.url || body.data?.fileUrl || body.data?.filePath || ''
  expect(uploadedUrl).toMatch(/^https?:\/\/.+\/rk-content\/news-image\//)
})
