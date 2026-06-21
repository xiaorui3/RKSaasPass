import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'

import {
  canAccessApiWorkbench,
  normalizeApiWorkbenchResources,
  normalizeApiWorkbenchEndpoints,
  buildApiWorkbenchUrl
} from '../src/utils/adminApiWorkbench.js'

const routerSource = fs.readFileSync(
  path.resolve('src/router/index.js'),
  'utf8'
)

test('api workbench stays tenant-1 only', () => {
  assert.equal(canAccessApiWorkbench(1), true)
  assert.equal(canAccessApiWorkbench('1'), true)
  assert.equal(canAccessApiWorkbench(2), false)
})

test('normalizes swagger resources into stable rows', () => {
  assert.deepEqual(
    normalizeApiWorkbenchResources([
      { name: 'rk-user', url: '/users/v2/api-docs' },
      { name: '', url: '/broken' },
      { name: 'rk-content', url: '/content/v2/api-docs' }
    ]),
    [
      { id: 'rk-user::/users/v2/api-docs', name: 'rk-user', url: '/users/v2/api-docs' },
      { id: 'rk-content::/content/v2/api-docs', name: 'rk-content', url: '/content/v2/api-docs' }
    ]
  )
})

test('normalizes endpoint rows and keeps method/path ordering predictable', () => {
  const rows = normalizeApiWorkbenchEndpoints([
    { path: '/api/members/{id}', method: 'PUT', summary: 'Update member' },
    { path: '/admin/ops/monitoring/overview', method: 'GET', summary: 'Monitoring overview' }
  ])

  assert.deepEqual(rows.map((item) => `${item.method} ${item.path}`), [
    'GET /admin/ops/monitoring/overview',
    'PUT /api/members/{id}'
  ])
})

test('builds debug request url from query json', () => {
  assert.equal(
    buildApiWorkbenchUrl('/api/news', '{"page":1,"pageSize":8,"keyword":"test"}'),
    '/api/news?page=1&pageSize=8&keyword=test'
  )
  assert.equal(buildApiWorkbenchUrl('/api/news', ''), '/api/news')
})

test('admin router should expose api workbench entry', () => {
  assert.equal(routerSource.includes("path: 'operation/api-workbench'"), true)
  assert.equal(routerSource.includes("name: 'AdminApiWorkbench'"), true)
})
