// @ts-check
const { test, expect } = require('@playwright/test')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'
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

async function applyLogin(page, loginData) {
  await page.goto(BASE_URL, { waitUntil: 'domcontentloaded' })
  await page.evaluate((data) => {
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

async function mockAdminBootstrap(page) {
  const success = (data) => ({
    code: 200,
    msg: 'ok',
    state: 'success',
    data
  })

  await page.route('**/menus/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(success([]))
    })
  })

  await page.route('**/tenants/list', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(success([{ id: 1, tenantName: 'Tenant A' }]))
    })
  })

  await page.route('**/auth/switchable-tenants', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(success([{ tenantId: 1, roleId: 7, username: 'manager_a' }]))
    })
  })

  await page.route('**/api/config/theme/current', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(success({ frontendTheme: 'default', adminTheme: 'default' }))
    })
  })

  await page.route('**/api/config/theme/public**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(success({ frontendTheme: 'default', adminTheme: 'default' }))
    })
  })

  await page.route('**/api/news', async (route) => {
    if (route.request().method() !== 'GET') {
      await route.continue()
      return
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(success({ records: [], total: 0 }))
    })
  })

  await page.route('**/notifications/api/notices/list**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(success([]))
    })
  })

  await page.route('**/api/activity/admin/page**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(success({ records: [], total: 0 }))
    })
  })

  await page.route('**/api/competition/list**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(success([]))
    })
  })

  await page.route('**/api/works/list**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(success([]))
    })
  })
}

function pngFile(name = 'cover.png') {
  return {
    name,
    mimeType: 'image/png',
    buffer: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jX4kAAAAASUVORK5CYII=', 'base64')
  }
}

function textFile(name = 'attachment.txt') {
  return {
    name,
    mimeType: 'text/plain',
    buffer: Buffer.from('upload-attachment-probe', 'utf8')
  }
}

function mp4File(name = 'video.mp4') {
  return {
    name,
    mimeType: 'video/mp4',
    buffer: Buffer.from('00000020667479706d703432000000006d70343269736f6d', 'hex')
  }
}

async function mockUploads(page, uploads) {
  let call = 0
  await page.route('**/api/files/upload', async (route) => {
    const current = uploads[Math.min(call, uploads.length - 1)]
    call += 1
    const payload = typeof current === 'string'
      ? {
          fileUrl: current,
          url: current
        }
      : {
          relativePath: current.relativePath || current.path || '',
          path: current.path || current.relativePath || '',
          fileUrl: current.fileUrl || current.url || '',
          url: current.url || current.fileUrl || ''
        }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 200,
        msg: '閹垮秳缍旈幋鎰',
        state: 'success',
        data: payload
      })
    })
  })
}

function fileInputByUploadRow(scope, index) {
  return scope.locator('.upload-row input[type="file"]').nth(index)
}

test('news admin should submit uploaded cover video and attachment urls', async ({ page, request }) => {
  await mockAdminBootstrap(page)
  const login = await loginApi(request, MANAGER)
  await applyLogin(page, login)

  await mockUploads(page, [
    {
      path: 'rk-content/news-image/2026/04/29/news-cover.png',
      url: 'http://upload.test/rk-content/news-image/2026/04/29/news-cover.png'
    },
    {
      path: 'rk-content/news-video/2026/04/29/news-video.mp4',
      url: 'http://upload.test/rk-content/news-video/2026/04/29/news-video.mp4'
    },
    {
      path: 'rk-content/news-attachment/2026/04/29/news-attachment.txt',
      url: 'http://upload.test/rk-content/news-attachment/2026/04/29/news-attachment.txt'
    }
  ])

  let submitPayload = null
  await page.route('**/api/news/add', async (route) => {
    submitPayload = route.request().postDataJSON()
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, msg: '閹垮秳缍旈幋鎰', state: 'success' })
    })
  })

  await page.goto(`${BASE_URL}/admin/content/news`, { waitUntil: 'domcontentloaded' })
  await page.locator('.card-header .el-button').last().click()

  await page.locator('.el-dialog').getByPlaceholder(/璇疯緭鍏ユ柊闂绘爣棰?).fill(`鏂伴椈涓婁紶-${Date.now()}`)
  await page.locator('.el-dialog').getByPlaceholder(/璇疯緭鍏ユ柊闂诲唴瀹?).fill('鏂伴椈鍐呭')

  await page.locator('.cover-uploader input[type="file"]').setInputFiles(pngFile())
  await page.locator('.video-uploader input[type="file"]').setInputFiles(mp4File())
  await page.locator('.news-attachment-upload input[type="file"]').setInputFiles(textFile())

  await page.locator('.el-dialog').getByRole('button', { name: /纭畾/ }).click()

  await expect.poll(() => submitPayload).not.toBeNull()
  await expect.poll(() => submitPayload?.coverImage ?? null).toBe('rk-content/news-image/2026/04/29/news-cover.png')
  await expect.poll(() => submitPayload?.videoUrl ?? null).toBe('rk-content/news-video/2026/04/29/news-video.mp4')
  await expect.poll(() => submitPayload?.attachmentUrl ?? null).toBe('rk-content/news-attachment/2026/04/29/news-attachment.txt')
})

test('notice admin should submit uploaded cover and attachment urls', async ({ page, request }) => {
  await mockAdminBootstrap(page)
  const login = await loginApi(request, MANAGER)
  await applyLogin(page, login)

  await mockUploads(page, [
    {
      path: 'rk-user/notice-cover/2026/04/29/notice-cover.png',
      url: 'http://upload.test/rk-user/notice-cover/2026/04/29/notice-cover.png'
    },
    {
      path: 'rk-user/notice-attachment/2026/04/29/notice-attachment.txt',
      url: 'http://upload.test/rk-user/notice-attachment/2026/04/29/notice-attachment.txt'
    }
  ])

  let submitPayload = null
  await page.route('**/notifications/api/notices/create', async (route) => {
    submitPayload = route.request().postDataJSON()
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, msg: '鎿嶄綔鎴愬姛', state: 'success' })
    })
  })

  await page.goto(`${BASE_URL}/admin/content/notices`, { waitUntil: 'domcontentloaded' })
  await page.locator('.card-header .el-button').last().click()

  await page.locator('.el-dialog').getByPlaceholder(/璇疯緭鍏ュ叕鍛婃爣棰?).fill(`鍏憡涓婁紶-${Date.now()}`)
  await page.locator('.el-dialog').getByPlaceholder(/璇疯緭鍏ュ叕鍛婂唴瀹?).fill('鍏憡鍐呭')

  await page.locator('.notice-cover-upload input[type="file"]').setInputFiles(pngFile())
  await page.locator('.notice-attachment-upload input[type="file"]').setInputFiles(textFile())

  await page.locator('.el-dialog').getByRole('button', { name: /纭畾/ }).click()

  await expect.poll(() => submitPayload).not.toBeNull()
  await expect.poll(() => submitPayload?.coverImage ?? null).toBe('rk-user/notice-cover/2026/04/29/notice-cover.png')
  await expect.poll(() => submitPayload?.attachmentUrl ?? null).toBe('rk-user/notice-attachment/2026/04/29/notice-attachment.txt')
})

test('activity admin should submit uploaded cover and attachment urls', async ({ page, request }) => {
  await mockAdminBootstrap(page)
  const login = await loginApi(request, MANAGER)
  await applyLogin(page, login)

  await mockUploads(page, [
    {
      path: 'rk-activity/activity-cover/2026/04/29/activity-cover.png',
      url: 'http://upload.test/rk-activity/activity-cover/2026/04/29/activity-cover.png'
    },
    {
      path: 'rk-activity/activity-attachment/2026/04/29/activity-attachment.txt',
      url: 'http://upload.test/rk-activity/activity-attachment/2026/04/29/activity-attachment.txt'
    }
  ])

  let submitPayload = null
  await page.route('**/api/activity/add', async (route) => {
    submitPayload = route.request().postDataJSON()
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, msg: '鎿嶄綔鎴愬姛', state: 'success', data: 1001 })
    })
  })

  await page.goto(`${BASE_URL}/admin/activity/list`, { waitUntil: 'domcontentloaded' })
  await page.locator('.card-header .el-button').last().click()

  await page.locator('.el-dialog').getByPlaceholder(/璇疯緭鍏ユ椿鍔ㄥ悕绉?).fill(`娲诲姩涓婁紶-${Date.now()}`)
  await page.locator('.el-form-item').filter({ hasText: /寮€濮嬫椂闂? }).locator('input').first().fill('2026-05-10 10:00:00')
  await page.locator('.el-form-item').filter({ hasText: /缁撴潫鏃堕棿/ }).locator('input').first().fill('2026-05-11 10:00:00')
  await page.locator('.el-form-item').filter({ hasText: /娲诲姩鍦扮偣/ }).locator('input').first().fill('浼氳瀹')
  await page.locator('.el-dialog').getByPlaceholder(/璇疯緭鍏ユ椿鍔ㄦ弿杩?).fill('娲诲姩鎻忚堪')

  await fileInputByUploadRow(page.locator('.el-dialog').last(), 0).setInputFiles(pngFile())
  await fileInputByUploadRow(page.locator('.el-dialog').last(), 1).setInputFiles(textFile())

  await page.locator('.el-dialog').getByRole('button', { name: /纭畾/ }).click()

  await expect.poll(() => submitPayload).not.toBeNull()
  await expect.poll(() => submitPayload?.coverImage ?? null).toBe('rk-activity/activity-cover/2026/04/29/activity-cover.png')
  await expect.poll(() => submitPayload?.attachmentUrl ?? null).toBe('rk-activity/activity-attachment/2026/04/29/activity-attachment.txt')
})

test('competition admin should submit uploaded cover and file urls', async ({ page, request }) => {
  await mockAdminBootstrap(page)
  const login = await loginApi(request, MANAGER)
  await applyLogin(page, login)

  await mockUploads(page, [
    {
      path: 'rk-activity/competition-cover/2026/04/29/competition-cover.png',
      url: 'http://upload.test/rk-activity/competition-cover/2026/04/29/competition-cover.png'
    },
    {
      path: 'rk-activity/competition-rules/2026/04/29/competition-rules.txt',
      url: 'http://upload.test/rk-activity/competition-rules/2026/04/29/competition-rules.txt'
    },
    {
      path: 'rk-activity/competition-materials/2026/04/29/competition-materials.txt',
      url: 'http://upload.test/rk-activity/competition-materials/2026/04/29/competition-materials.txt'
    },
    {
      path: 'rk-activity/competition-results/2026/04/29/competition-results.txt',
      url: 'http://upload.test/rk-activity/competition-results/2026/04/29/competition-results.txt'
    }
  ])

  let submitPayload = null
  await page.route('**/api/competition', async (route) => {
    if (route.request().method() !== 'POST') {
      await route.continue()
      return
    }
    submitPayload = route.request().postDataJSON()
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, msg: '鎿嶄綔鎴愬姛', state: 'success', data: 1002 })
    })
  })

  await page.goto(`${BASE_URL}/admin/activity/competition`, { waitUntil: 'domcontentloaded' })
  await page.locator('.card-header .el-button').last().click()

  await page.locator('.el-dialog').getByPlaceholder(/璇疯緭鍏ユ瘮璧涙爣棰?).fill(`姣旇禌涓婁紶-${Date.now()}`)
  await page.locator('.el-form-item').filter({ hasText: /鎶ュ悕寮€濮? }).locator('input').first().fill('2026-05-10 10:00:00')
  await page.locator('.el-form-item').filter({ hasText: /鎶ュ悕缁撴潫/ }).locator('input').first().fill('2026-05-11 10:00:00')
  await page.locator('.el-form-item').filter({ hasText: /姣旇禌寮€濮? }).locator('input').first().fill('2026-05-20 10:00:00')
  await page.locator('.el-form-item').filter({ hasText: /姣旇禌缁撴潫/ }).locator('input').first().fill('2026-05-21 10:00:00')

  await fileInputByUploadRow(page.locator('.el-dialog').last(), 0).setInputFiles(pngFile())
  await fileInputByUploadRow(page.locator('.el-dialog').last(), 1).setInputFiles(textFile('rules.txt'))
  await fileInputByUploadRow(page.locator('.el-dialog').last(), 2).setInputFiles(textFile('materials.txt'))
  await fileInputByUploadRow(page.locator('.el-dialog').last(), 3).setInputFiles(textFile('results.txt'))

  await page.locator('.el-dialog').getByRole('button', { name: /纭畾/ }).click()

  await expect.poll(() => submitPayload).not.toBeNull()
  await expect.poll(() => submitPayload?.coverImage ?? null).toBe('rk-activity/competition-cover/2026/04/29/competition-cover.png')
  await expect.poll(() => submitPayload?.rulesFile ?? null).toBe('rk-activity/competition-rules/2026/04/29/competition-rules.txt')
  await expect.poll(() => submitPayload?.materialsFile ?? null).toBe('rk-activity/competition-materials/2026/04/29/competition-materials.txt')
  await expect.poll(() => submitPayload?.resultsFile ?? null).toBe('rk-activity/competition-results/2026/04/29/competition-results.txt')
})

test('works admin should submit managed cover and demo video paths', async ({ page, request }) => {
  await mockAdminBootstrap(page)
  const login = await loginApi(request, MANAGER)
  await applyLogin(page, login)

  await mockUploads(page, [
    {
      path: 'rk-content/works-cover/2026/04/29/works-cover.png',
      url: 'http://upload.test/rk-content/works-cover/2026/04/29/works-cover.png'
    },
    {
      path: 'rk-content/works-video/2026/04/29/works-demo.mp4',
      url: 'http://upload.test/rk-content/works-video/2026/04/29/works-demo.mp4'
    }
  ])

  let submitPayload = null
  await page.route('**/api/works', async (route) => {
    if (route.request().method() !== 'POST') {
      await route.continue()
      return
    }
    submitPayload = route.request().postDataJSON()
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, msg: '鎿嶄綔鎴愬姛', state: 'success' })
    })
  })

  await page.goto(`${BASE_URL}/admin/content/works`, { waitUntil: 'domcontentloaded' })
  await page.locator('.card-header .el-button').last().click()

  const dialog = page.locator('.el-dialog').last()
  await dialog.locator('input').first().fill(`浣滃搧涓婁紶-${Date.now()}`)
  await dialog.locator('.el-select').first().click()
  await page.getByRole('option').first().click()
  await dialog.locator('textarea').nth(1).fill('浣滃搧鍐呭')

  await dialog.locator('.works-cover-upload input[type="file"]').setInputFiles(pngFile('works-cover.png'))
  await dialog.locator('.works-video-upload input[type="file"]').setInputFiles(mp4File('works-video.mp4'))

  await dialog.getByRole('button', { name: /纭畾/ }).click()

  await expect.poll(() => submitPayload).not.toBeNull()
  await expect.poll(() => submitPayload?.coverImage ?? null).toBe('rk-content/works-cover/2026/04/29/works-cover.png')
  await expect.poll(() => submitPayload?.demoVideo ?? null).toBe('rk-content/works-video/2026/04/29/works-demo.mp4')
})
