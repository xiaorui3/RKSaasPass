import { chromium } from 'playwright'

const BASE_URL = process.env.SMOKE_BASE_URL || 'http://127.0.0.1:4173'

const cases = [
  { type: 'avatar', service: 'rk-user', bizType: 'avatar' },
  { type: 'news-image', service: 'rk-content', bizType: 'news-image' },
  { type: 'competition-results', service: 'rk-activity', bizType: 'competition-results' }
]

async function run() {
  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage()
  const captured = []

  await page.route('**/api/files/upload', async (route) => {
    const request = route.request()
    const body = request.postData() || ''
    captured.push(body)
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 200,
        data: {
          url: 'http://cdn.test/mock.png',
          fileUrl: 'http://cdn.test/mock.png',
          relativePath: 'rk-user/avatar/2026/04/23/mock.png'
        }
      })
    })
  })

  await page.goto(`${BASE_URL}/upload-contract-smoke.html`)

  for (const item of cases) {
    await page.evaluate(async ({ type }) => {
      const file = new File(['demo'], 'demo.png', { type: 'image/png' })
      await window.uploadSmoke.uploadFile(file, type)
    }, item)
  }

  for (let i = 0; i < cases.length; i++) {
    const body = captured[i] || ''
    const item = cases[i]
    if (!body.includes(`name="service"` ) || !body.includes(item.service)) {
      throw new Error(`missing service mapping for ${item.type}`)
    }
    if (!body.includes(`name="bizType"` ) || !body.includes(item.bizType)) {
      throw new Error(`missing bizType mapping for ${item.type}`)
    }
  }

  console.log('Upload contract browser smoke passed')
  await browser.close()
}

run().catch((error) => {
  console.error(error)
  process.exit(1)
})
