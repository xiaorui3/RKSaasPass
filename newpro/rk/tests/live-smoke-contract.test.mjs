import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'

const ROOT = path.resolve('newpro/rk/tests')

function readSpec(name) {
  return fs.readFileSync(path.join(ROOT, name), 'utf8')
}

test('real smoke specs should honor PLAYWRIGHT_BASE_URL instead of hardcoded localhost', () => {
  const specs = [
    'admin-members-news-approval-regressions.spec.cjs',
    'front-news-works-regressions.spec.cjs',
    'profile-page-load.spec.cjs'
  ]

  for (const spec of specs) {
    const source = readSpec(spec)
    assert.match(
      source,
      /const BASE_URL = process\.env\.PLAYWRIGHT_BASE_URL \|\| 'http:\/\/localhost:5173'/,
      `${spec} should read BASE_URL from PLAYWRIGHT_BASE_URL first`
    )
  }
})

test('slow live smoke specs should declare an explicit timeout budget', () => {
  const specs = [
    'admin-people-pages-real.spec.cjs',
    'content-module-uploads-real.spec.cjs'
  ]

  for (const spec of specs) {
    const source = readSpec(spec)
    assert.match(
      source,
      /test\.(describe\.configure|setTimeout)\(\{?\s*timeout:\s*(90000|120000)|test\.setTimeout\((90000|120000)\)/,
      `${spec} should declare an explicit long timeout`
    )
  }
})
