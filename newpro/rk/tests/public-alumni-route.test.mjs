import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

async function readSource(relativePath) {
  return readFile(new URL(relativePath, import.meta.url), 'utf8')
}

test('front alumni page is public because it uses public display APIs', async () => {
  const router = await readSource('../src/router/index.js')
  const layout = await readSource('../src/layouts/MainLayout.vue')

  assert.match(
    router,
    /path:\s*'alumni'[\s\S]{0,160}name:\s*'Alumni'[\s\S]{0,160}meta:\s*\{\s*title:\s*'校友风采'\s*\}/
  )
  assert.doesNotMatch(
    router,
    /path:\s*'alumni'[\s\S]{0,220}requiresAuth:\s*true/
  )
  assert.match(layout, /\{\s*name:\s*t\('nav\.alumni'\),\s*path:\s*'\/alumni'/)
  assert.doesNotMatch(layout, /isLoggedIn\.value\s*\?\s*\[\{\s*name:\s*t\('nav\.alumni'\)/)
})
