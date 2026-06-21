import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile, stat } from 'node:fs/promises'

async function readSource(relativePath) {
  return readFile(new URL(relativePath, import.meta.url), 'utf8')
}

test('home page should use generated campus hero image as first-viewport visual asset', async () => {
  const home = await readSource('../src/views/Home.vue')
  const asset = await stat(new URL('../public/assets/visuals/rk-campus-hero.png', import.meta.url))

  assert.ok(asset.size > 100 * 1024)
  assert.equal(home.includes('/assets/visuals/rk-campus-hero.png'), true)
  assert.equal(home.includes('hero-image-credit'), false)
})

test('login page should reuse generated campus image as video fallback visual', async () => {
  const login = await readSource('../src/views/Login.vue')
  const asset = await stat(new URL('../public/assets/visuals/rk-campus-hero.png', import.meta.url))

  assert.ok(asset.size > 100 * 1024)
  assert.equal(login.includes('poster="/assets/visuals/rk-campus-hero.png"'), true)
  assert.equal(login.includes("url('/assets/visuals/rk-campus-hero.png')"), true)
})

test('android client should use generated mobile campus hero asset', async () => {
  const source = await readFile(new URL('../../../android-client/rk-club-android/app/src/main/java/com/rkclub/app/MainActivity.java', import.meta.url), 'utf8')
  const asset = await stat(new URL('../../../android-client/rk-club-android/app/src/main/res/drawable-nodpi/hero_mobile_campus_v2.png', import.meta.url))

  assert.ok(asset.size > 100 * 1024)
  assert.equal(source.includes('R.drawable.hero_mobile_campus_v2'), true)
})

test('admin dashboard should use generated operations visual asset', async () => {
  const dashboard = await readSource('../src/views/admin/Dashboard.vue')
  const asset = await stat(new URL('../public/assets/visuals/rk-admin-ops.png', import.meta.url))

  assert.ok(asset.size > 80 * 1024)
  assert.equal(dashboard.includes('/assets/visuals/rk-admin-ops.png'), true)
})

test('android services should use generated mobile services visual asset', async () => {
  const source = await readFile(new URL('../../../android-client/rk-club-android/app/src/main/java/com/rkclub/app/MainActivity.java', import.meta.url), 'utf8')
  const asset = await stat(new URL('../../../android-client/rk-club-android/app/src/main/res/drawable-nodpi/hero_mobile_services.png', import.meta.url))

  assert.ok(asset.size > 50 * 1024)
  assert.equal(source.includes('R.drawable.hero_mobile_services'), true)
})
