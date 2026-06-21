// @ts-check
const { test, expect } = require('@playwright/test')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'
const BROKEN_COVER = 'http://127.0.0.1:9000/rk-bucket/notices/covers/broken-cover.png'

test('public notices should hide broken cover images instead of keeping a failed image element', async ({ page }) => {
  await page.route('**/notifications/api/notices/published', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 200,
        msg: 'ok',
        state: 'success',
        data: [
          {
            id: 1,
            title: 'broken-cover-notice',
            content: 'cover should disappear after image load fails',
            noticeType: 1,
            coverImage: BROKEN_COVER,
            attachmentUrl: '',
            isTop: 0,
            createTime: '2026-04-23 23:00:00'
          }
        ]
      })
    })
  })

  await page.route('**/rk-bucket/notices/covers/broken-cover.png', async (route) => {
    await route.abort('failed')
  })

  await page.goto(`${BASE_URL}/notices`, { waitUntil: 'networkidle' })

  await expect(page.locator('.notice-card')).toHaveCount(1)
  await expect(page.locator('.notice-cover-image')).toHaveCount(0)
})
