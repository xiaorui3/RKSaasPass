// @ts-check
const { test, expect } = require('@playwright/test')

const pages = [
  {
    path: '/about',
    pageId: 'portal-about-page',
    sectionId: 'portal-about-history'
  },
  {
    path: '/news',
    pageId: 'portal-news-page',
    sectionId: 'portal-news-timeline'
  },
  {
    path: '/notices',
    pageId: 'portal-notices-page',
    sectionId: 'portal-notices-board'
  },
  {
    path: '/works',
    pageId: 'portal-works-page',
    sectionId: 'portal-works-grid'
  },
  {
    path: '/contact',
    pageId: 'portal-contact-page',
    sectionId: 'portal-contact-form'
  }
]

for (const item of pages) {
  test(`public portal route ${item.path} should expose enterprise page structure`, async ({ page, baseURL }) => {
    await page.goto(`${baseURL}${item.path}`, { waitUntil: 'networkidle' })
    await expect(page.getByTestId(item.pageId)).toBeVisible()
    await expect(page.getByTestId(item.sectionId)).toBeVisible()
  })
}
