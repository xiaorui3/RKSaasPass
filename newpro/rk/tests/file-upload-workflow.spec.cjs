// @ts-check
const { test, expect } = require('@playwright/test')
const helpers = require('./crud/helpers.cjs')

const { BASE_URL, API_URL, loginByApi, gotoAdmin, clickButton, selectOption, dismissSuccessMessage, uniqueName } = helpers

// 1x1 transparent PNG as base64 for upload tests
const TEST_IMAGE_BASE64 = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jX4kAAAAASUVORK5CYII='
const TEST_IMAGE_BUFFER = Buffer.from(TEST_IMAGE_BASE64, 'base64')

// Small text file for attachment tests
const TEST_TEXT_BUFFER = Buffer.from('This is a test attachment file for RK-Web upload testing.', 'utf-8')

const now = new Date()
const tomorrow = new Date(now.getTime() + 24 * 60 * 60 * 1000)
const dayAfter = new Date(now.getTime() + 48 * 60 * 60 * 1000)
const oneHourAgo = new Date(now.getTime() - 60 * 60 * 1000)
const oneHourLater = new Date(now.getTime() + 60 * 60 * 1000)

function fmt(d) {
  const pad = v => String(v).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function uploadTarget(type) {
  const mapping = {
    'test-upload': { service: 'rk-user', bizType: 'avatar' },
    'news-image': { service: 'rk-content', bizType: 'news-image' },
    'activity-cover': { service: 'rk-activity', bizType: 'activity-cover' },
    'competition-cover': { service: 'rk-activity', bizType: 'competition-cover' },
    'news-attachment': { service: 'rk-content', bizType: 'news-attachment' },
  }
  return mapping[type]
}

async function cleanupByTitle(request, token, endpoint, title) {
  try {
    const list = await request.get(`${API_URL}${endpoint}?search=${encodeURIComponent(title)}`, {
      headers: { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1' },
    })
    const body = await list.json()
    const records = body.data?.records || body.data || []
    for (const r of records) {
      if (r.title === title || r.activityName === title || r.competitionName === title) {
        await request.delete(`${API_URL}${endpoint.includes('/api/') ? endpoint : endpoint + '/delete'}/${r.id}`, {
          headers: { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1' },
        }).catch(() => {})
      }
    }
  } catch {}
}

// 鈹€鈹€ News with cover image 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

test('create news with cover image upload', async ({ page, request }) => {
  const loginData = await loginByApi(page, request, 'admin')
  const token = loginData.token
  const title = uniqueName('銆愪笂浼犳祴璇曘€戝浘鏂囨柊闂?)

  await cleanupByTitle(request, token, '/api/news', title)

  await gotoAdmin(page, '/admin/content/news')
  await page.waitForTimeout(1000)

  // Dismiss any vite error overlay
  await page.evaluate(() => {
    document.querySelectorAll('vite-error-overlay').forEach(el => el.remove())
  })

  // Click create button
  await clickButton(page, '鍙戝竷鏂伴椈')

  const dialog = page.locator('.el-dialog:visible').first()
  await expect(dialog).toBeVisible()

  // Fill title
  const titleInput = dialog.locator('.el-form-item:has-text("鏂伴椈鏍囬") input.el-input__inner').first()
  await titleInput.fill(title)

  // Select category
  const categorySelect = dialog.locator('.el-form-item:has-text("鏂伴椈鍒嗙被") .el-select').first()
  if (await categorySelect.count() > 0) {
    await categorySelect.click()
    await page.waitForTimeout(500)
    const opt = page.locator('.el-select-dropdown__item:visible').first()
    if (await opt.count() > 0) await opt.click({ force: true })
    await page.waitForTimeout(300)
  }

  // Fill content
  const contentArea = dialog.locator('.el-form-item:has-text("鏂伴椈鍐呭") textarea').first()
  if (await contentArea.count() > 0) {
    await contentArea.fill('杩欐槸涓€鏉″甫灏侀潰鍥剧墖涓婁紶鐨勮嚜鍔ㄥ寲娴嬭瘯鏂伴椈鍐呭銆?)
  }

  // Upload cover image via el-upload
  const coverUpload = dialog.locator('.cover-uploader input[type="file"]').first()
  if (await coverUpload.count() > 0) {
    await coverUpload.setInputFiles({
      name: 'test-cover.png',
      mimeType: 'image/png',
      buffer: TEST_IMAGE_BUFFER,
    })
    await page.waitForTimeout(3000) // wait for upload to complete
  }

  // Upload attachment
  const attachUpload = dialog.locator('.news-attachment-upload input[type="file"]').first()
  if (await attachUpload.count() > 0) {
    await attachUpload.setInputFiles({
      name: 'test-attachment.txt',
      mimeType: 'text/plain',
      buffer: TEST_TEXT_BUFFER,
    })
    await page.waitForTimeout(3000)
  }

  // Set to publish
  const publishRadio = dialog.locator('.el-radio:has-text("鍙戝竷")').first()
  if (await publishRadio.count() > 0) {
    await publishRadio.click()
  }

  // Submit
  const submitBtn = dialog.locator('.el-button--primary:has-text("纭畾")').first()
  await submitBtn.click()
  await page.waitForTimeout(3000)

  // Check for any error message in dialog (validation)
  const dialogError = dialog.locator('.el-form-item__error').first()
  if (await dialogError.isVisible().catch(() => false)) {
    // Validation error - log but continue to API verification
    const errorText = await dialogError.textContent()
    console.log('Form validation error:', errorText)
  }

  // Dismiss success message
  await dismissSuccessMessage(page)

  // Verify via API that news was created
  await page.waitForTimeout(1000)
  const newsCheck = await request.get(`${API_URL}/api/news?title=${encodeURIComponent(title)}`, {
    headers: { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1' },
  })
  const newsBody = await newsCheck.json()
  const records = newsBody.data?.records || newsBody.data || []
  const found = records.some(r => r.title === title)
  expect(found || await page.locator('.el-message--success').isVisible().catch(() => false)).toBeTruthy()

  // Cleanup
  await cleanupByTitle(request, token, '/api/news', title)
})

// 鈹€鈹€ Activity with cover image 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

test('create activity with cover image and attachment upload', async ({ page, request }) => {
  const loginData = await loginByApi(page, request, 'admin')
  const token = loginData.token
  const title = uniqueName('銆愪笂浼犳祴璇曘€戠ぞ鍥㈡椿鍔?)

  await gotoAdmin(page, '/admin/activity/list')
  await page.waitForTimeout(1000)

  await clickButton(page, '鏂板娲诲姩')

  const dialog = page.locator('.el-dialog:visible').first()
  await expect(dialog).toBeVisible()

  // Fill activity name
  const nameInput = dialog.locator('input.el-input__inner').first()
  await nameInput.fill(title)

  // Select type
  const typeSelect = dialog.locator('.el-select').first()
  if (await typeSelect.count() > 0) {
    await typeSelect.click()
    await page.waitForTimeout(500)
    const opt = page.locator('.el-select-dropdown__item:visible').first()
    if (await opt.count() > 0) await opt.click({ force: true })
    await page.waitForTimeout(300)
  }

  // Fill dates using date pickers - set via keyboard on the inputs
  const dateInputs = dialog.locator('.el-date-editor input.el-input__inner, .el-date-editor .el-input__inner')
  const dateCount = await dateInputs.count()
  const dates = [fmt(oneHourAgo), fmt(oneHourLater), fmt(tomorrow), fmt(dayAfter)]
  for (let i = 0; i < Math.min(dateCount, 4); i++) {
    const inp = dateInputs.nth(i)
    await inp.click()
    await page.waitForTimeout(300)
    await inp.fill(dates[i])
    await page.keyboard.press('Enter')
    await page.waitForTimeout(300)
  }

  // Fill location
  const locInput = dialog.locator('.el-form-item:has-text("娲诲姩鍦扮偣") input').first()
  if (await locInput.count() > 0) {
    await locInput.fill('娴嬭瘯娲诲姩鍦扮偣')
  }

  // Upload cover image
  const coverUpload = dialog.locator('.activity-cover-upload input[type="file"]').first()
  if (await coverUpload.count() > 0) {
    await coverUpload.setInputFiles({
      name: 'activity-cover.png',
      mimeType: 'image/png',
      buffer: TEST_IMAGE_BUFFER,
    })
    await page.waitForTimeout(3000)
  }

  // Upload attachment
  const attachUpload = dialog.locator('.activity-attachment-upload input[type="file"]').first()
  if (await attachUpload.count() > 0) {
    await attachUpload.setInputFiles({
      name: 'activity-info.txt',
      mimeType: 'text/plain',
      buffer: TEST_TEXT_BUFFER,
    })
    await page.waitForTimeout(3000)
  }

  // Fill description
  const descArea = dialog.locator('textarea').first()
  if (await descArea.count() > 0) {
    await descArea.fill('杩欐槸甯﹂檮浠朵笂浼犵殑娲诲姩鑷姩鍖栨祴璇曟弿杩般€?)
  }

  // Submit
  const submitBtn = dialog.locator('.el-button--primary:has-text("纭畾")').first()
  await submitBtn.click()
  await page.waitForTimeout(2000)
  await dismissSuccessMessage(page)

  // Verify
  const successMsg = await page.locator('.el-message--success').isVisible().catch(() => false)
  const rowVisible = await page.getByText(title).first().isVisible().catch(() => false)
  expect(successMsg || rowVisible).toBeTruthy()

  // Cleanup
  try {
    const list = await request.get(`${API_URL}/api/activity/list?search=${encodeURIComponent(title)}`, {
      headers: { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1' },
    })
    const body = await list.json()
    const records = body.data?.records || body.data || []
    for (const r of records) {
      if ((r.activityName || r.title || '') === title) {
        await request.delete(`${API_URL}/api/activity/delete/${r.id}`, {
          headers: { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1' },
        }).catch(() => {})
      }
    }
  } catch {}
})

// 鈹€鈹€ Competition with cover image 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

test('create competition with cover image and rules file upload', async ({ page, request }) => {
  const loginData = await loginByApi(page, request, 'admin')
  const token = loginData.token
  const title = uniqueName('銆愪笂浼犳祴璇曘€戠紪绋嬪ぇ璧?)

  await gotoAdmin(page, '/admin/activity/competition')
  await page.waitForTimeout(1000)

  await clickButton(page, '鏂板姣旇禌')

  const dialog = page.locator('.el-dialog:visible').first()
  await expect(dialog).toBeVisible()

  // Fill title
  const titleInput = dialog.locator('input.el-input__inner').first()
  await titleInput.fill(title)

  // Select competition type
  const typeSelect = dialog.locator('.el-select').first()
  if (await typeSelect.count() > 0) {
    await typeSelect.click()
    await page.waitForTimeout(500)
    const opt = page.locator('.el-select-dropdown__item:visible').first()
    if (await opt.count() > 0) await opt.click({ force: true })
    await page.waitForTimeout(300)
  }

  // Fill dates
  const dateInputs = dialog.locator('.el-date-editor input.el-input__inner, .el-date-editor .el-input__inner')
  const dateCount = await dateInputs.count()
  const dates = [fmt(oneHourAgo), fmt(oneHourLater), fmt(tomorrow), fmt(dayAfter)]
  for (let i = 0; i < Math.min(dateCount, 4); i++) {
    const inp = dateInputs.nth(i)
    await inp.click()
    await page.waitForTimeout(300)
    await inp.fill(dates[i])
    await page.keyboard.press('Enter')
    await page.waitForTimeout(300)
  }

  // Upload cover image
  const coverUpload = dialog.locator('.competition-cover-upload input[type="file"]').first()
  if (await coverUpload.count() > 0) {
    await coverUpload.setInputFiles({
      name: 'competition-cover.png',
      mimeType: 'image/png',
      buffer: TEST_IMAGE_BUFFER,
    })
    await page.waitForTimeout(3000)
  }

  // Upload rules file
  const rulesUpload = dialog.locator('.competition-rules-upload input[type="file"]').first()
  if (await rulesUpload.count() > 0) {
    await rulesUpload.setInputFiles({
      name: 'competition-rules.txt',
      mimeType: 'text/plain',
      buffer: TEST_TEXT_BUFFER,
    })
    await page.waitForTimeout(3000)
  }

  // Upload materials file
  const materialsUpload = dialog.locator('.competition-materials-upload input[type="file"]').first()
  if (await materialsUpload.count() > 0) {
    await materialsUpload.setInputFiles({
      name: 'competition-materials.txt',
      mimeType: 'text/plain',
      buffer: TEST_TEXT_BUFFER,
    })
    await page.waitForTimeout(3000)
  }

  // Fill description
  const descArea = dialog.locator('textarea').first()
  if (await descArea.count() > 0) {
    await descArea.fill('杩欐槸甯﹀鏂囦欢涓婁紶鐨勬瘮璧涜嚜鍔ㄥ寲娴嬭瘯鎻忚堪銆?)
  }

  // Submit
  const submitBtn = dialog.locator('.el-button--primary:has-text("纭畾")').first()
  await submitBtn.click()
  await page.waitForTimeout(2000)
  await dismissSuccessMessage(page)

  // Verify
  const successMsg = await page.locator('.el-message--success').isVisible().catch(() => false)
  const rowVisible = await page.getByText(title).first().isVisible().catch(() => false)
  expect(successMsg || rowVisible).toBeTruthy()

  // Cleanup
  try {
    const list = await request.get(`${API_URL}/api/competition?search=${encodeURIComponent(title)}`, {
      headers: { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1' },
    })
    const body = await list.json()
    const records = body.data?.records || body.data || []
    for (const r of records) {
      if ((r.competitionName || r.title || '') === title) {
        await request.delete(`${API_URL}/api/competition/${r.id}`, {
          headers: { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1' },
        }).catch(() => {})
      }
    }
  } catch {}
})

// 鈹€鈹€ API-level file upload verification 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

test('file upload API returns accessible MinIO URL', async ({ request }) => {
  const loginResp = await request.post(`${API_URL}/auth/login`, {
    data: { username: 'admin_a', password: 'change-me', organizationId: 1 },
  })
  const loginBody = await loginResp.json()
  const token = loginBody.data.token

  // Upload a test image via API
  const uploadResp = await request.post(`${API_URL}/api/files/upload`, {
    headers: {
      Authorization: `Bearer ${token}`,
      'X-Tenant-Id': '1',
    },
    multipart: {
      file: {
        name: 'api-test-image.png',
        mimeType: 'image/png',
        buffer: TEST_IMAGE_BUFFER,
      },
      ...uploadTarget('test-upload'),
    },
  })

  expect(uploadResp.ok()).toBeTruthy()
  const body = await uploadResp.json()
  expect(body.code).toBe(200)
  expect(body.data).toBeTruthy()

  // Verify the returned URL contains MinIO domain
  const fileUrl = body.data.fileUrl || body.data.url || body.data
  expect(fileUrl).toMatch(/^https?:\/\/.+\/rk-user\/avatar\//)

  // Verify the file is accessible
  const fileResp = await request.get(fileUrl)
  expect(fileResp.ok()).toBeTruthy()
  expect(fileResp.headers()['content-length']).toBeTruthy()
})

test('upload multiple files and verify each is stored separately', async ({ request }) => {
  const loginResp = await request.post(`${API_URL}/auth/login`, {
    data: { username: 'admin_a', password: 'change-me', organizationId: 1 },
  })
  const loginBody = await loginResp.json()
  const token = loginBody.data.token

  const urls = []
  const types = ['news-image', 'activity-cover', 'competition-cover', 'news-attachment']

  for (const type of types) {
    const resp = await request.post(`${API_URL}/api/files/upload`, {
      headers: {
        Authorization: `Bearer ${token}`,
        'X-Tenant-Id': '1',
      },
      multipart: {
        file: {
          name: `${type}-test.txt`,
          mimeType: 'text/plain',
          buffer: Buffer.from(`Content for ${type}`, 'utf-8'),
        },
        ...uploadTarget(type),
      },
    })

    expect(resp.ok()).toBeTruthy()
    const body = await resp.json()
    expect(body.code).toBe(200)
    const url = body.data.fileUrl || body.data.url || body.data
    urls.push(url)
  }

  // All URLs should be unique
  const uniqueUrls = new Set(urls)
  expect(uniqueUrls.size).toBe(urls.length)

  // All should be accessible
  for (const url of urls) {
    const resp = await request.get(url)
    expect(resp.ok()).toBeTruthy()
  }
})
