import test from 'node:test'
import assert from 'node:assert/strict'

const moduleUrl = new URL('../src/utils/themeAccess.js', import.meta.url)

test('shouldUseCurrentTheme should allow admin roles in admin context', async () => {
  const themeAccess = await import(`${moduleUrl.href}?case=admin`)

  assert.equal(themeAccess.shouldUseCurrentTheme('admin', 1), true)
  assert.equal(themeAccess.shouldUseCurrentTheme('admin', 7), true)
})

test('shouldUseCurrentTheme should use public theme for normal members', async () => {
  const themeAccess = await import(`${moduleUrl.href}?case=member`)

  assert.equal(themeAccess.shouldUseCurrentTheme('frontend', 2), false)
  assert.equal(themeAccess.shouldUseCurrentTheme('admin', 2), false)
})
