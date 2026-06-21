// @ts-check
const { test, expect } = require('@playwright/test')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'

async function getFirstNews(request) {
  const res = await request.get(`${API_BASE}/api/news?page=1&pageSize=8`)
  const body = await res.json()
  if (body.code !== 200 || !body.data?.records?.length) {
    throw new Error(`Failed to fetch news list: ${JSON.stringify(body)}`)
  }
  return body.data.records[0]
}

async function getFirstWork(request) {
  const res = await request.get(`${API_BASE}/api/works/list`)
  const body = await res.json()
  if (body.code !== 200 || !Array.isArray(body.data) || body.data.length === 0) {
    throw new Error(`Failed to fetch works list: ${JSON.stringify(body)}`)
  }
  return body.data[0]
}

test('home page news items should navigate to news detail pages', async ({ page, request }) => {
  const firstNews = await getFirstNews(request)

  await page.goto(`${BASE_URL}/`, { waitUntil: 'networkidle' })
  await expect(page.getByRole('link', { name: firstNews.title }).first()).toBeVisible()
  await page.getByRole('link', { name: firstNews.title }).first().click()

  await expect(page).toHaveURL(new RegExp(`/news/${firstNews.id}$`))
  await expect(page.locator('.news-detail-page, .news-detail').first()).toBeVisible()
})

test('works page should expose a liked-only filter and show only liked works', async ({ page, request }) => {
  const firstWork = await getFirstWork(request)

  await page.addInitScript((workId) => {
    localStorage.setItem('likedWorks', JSON.stringify([String(workId)]))
  }, firstWork.id)

  await page.goto(`${BASE_URL}/works`, { waitUntil: 'networkidle' })
  await expect(page.getByText(firstWork.title).first()).toBeVisible()

  await page.locator('.collection-tabs .el-radio-button').filter({ hasText: '鎴戠殑鏀惰棌' }).locator('.el-radio-button__inner').click({ force: true })

  await expect(page.getByText(firstWork.title).first()).toBeVisible()
  await expect(page.locator('.work-card')).toHaveCount(1)
})
