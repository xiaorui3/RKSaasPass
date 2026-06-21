import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

async function readSource(relativePath) {
  return readFile(new URL(relativePath, import.meta.url), 'utf8')
}

test('public home optional system config request should not show global error to guests', async () => {
  const [commonApi, requestUtil, homeView] = await Promise.all([
    readSource('../src/api/common.js'),
    readSource('../src/utils/request.js'),
    readSource('../src/views/Home.vue')
  ])

  assert.equal(commonApi.includes("url: '/api/config/system'"), true)
  assert.equal(commonApi.includes('silentError: true'), true)
  assert.equal(requestUtil.includes('error.config?.silentError'), true)
  assert.equal(homeView.includes('userStore.isLoggedIn ? getSystemConfig().catch(() => ({})) : Promise.resolve({})'), true)
})

test('public home shows loading states before homepage data is resolved', async () => {
  const homeView = await readSource('../src/views/Home.vue')

  assert.match(homeView, /const\s+homeLoading\s*=\s*ref\(true\)/)
  assert.match(homeView, /const\s+statsLoading\s*=\s*ref\(true\)/)
  assert.match(homeView, /v-if="statsLoading"/)
  assert.match(homeView, /class="stat-loading"/)
  assert.match(homeView, /v-if="homeLoading"/)
  assert.match(homeView, /class="home-section-loading"/)
  assert.equal(homeView.includes('const stats = ref({ activities: 0, news: 0, members: 0, works: 0 })'), false)
  assert.equal(homeView.includes('<strong>{{ stats.activities }}</strong>'), false)
})
