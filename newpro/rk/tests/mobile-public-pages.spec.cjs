// @ts-check
const { test, expect } = require('@playwright/test')

const pages = [
  { path: '/news', pageId: 'portal-news-page', sectionId: 'portal-news-timeline' },
  { path: '/notices', pageId: 'portal-notices-page', sectionId: 'portal-notices-board' },
  { path: '/works', pageId: 'portal-works-page', sectionId: 'portal-works-grid' }
]

test.use({
  viewport: { width: 390, height: 844 }
})

for (const item of pages) {
  test(`mobile page ${item.path} renders the main section without horizontal overflow`, async ({ page, baseURL }) => {
    await page.goto(`${baseURL}${item.path}`, { waitUntil: 'networkidle' })

    const pageByTestId = page.getByTestId(item.pageId)
    const sectionByTestId = page.getByTestId(item.sectionId)

    if (await pageByTestId.count()) {
      await expect(pageByTestId).toBeVisible()
    } else {
      await expect(page.locator('main, #app').first()).toBeVisible()
      expect(new URL(page.url()).pathname).toContain(item.path)
    }

    if (await sectionByTestId.count()) {
      await expect(sectionByTestId).toBeVisible()
    }

    const metrics = await page.evaluate(() => ({
      clientWidth: document.documentElement.clientWidth,
      scrollWidth: document.documentElement.scrollWidth
    }))

    expect(metrics.scrollWidth - metrics.clientWidth).toBeLessThan(24)
  })
}
