import test from 'node:test'
import assert from 'node:assert/strict'
import { resolveCurrentTenantBranding, sortTenantDirectory } from '../src/utils/tenantBranding.js'
import { resolveUploadTarget } from '../src/utils/uploadTargets.js'

test('sortTenantDirectory orders by displayOrder then id', () => {
  const sorted = sortTenantDirectory([
    { id: 3, tenantName: 'C', displayOrder: 2 },
    { id: 1, tenantName: 'A', displayOrder: 1 },
    { id: 2, tenantName: 'B', displayOrder: 1 }
  ])

  assert.deepEqual(sorted.map((item) => item.id), [1, 2, 3])
})

test('resolveCurrentTenantBranding prefers tenant logo and name', () => {
  const branding = resolveCurrentTenantBranding({
    tenantId: '9',
    tenantDirectory: [
      { id: 9, tenantName: '软件开发社团', logoUrl: '/rk-user/tenant-logo/logo.png' }
    ]
  })

  assert.equal(branding.tenantName, '软件开发社团')
  assert.equal(branding.logoUrl, '/rk-user/tenant-logo/logo.png')
  assert.equal(branding.initials, '软件')
})

test('resolveCurrentTenantBranding falls back to neutral initials instead of RK', () => {
  const branding = resolveCurrentTenantBranding({
    tenantId: '404',
    tenantDirectory: []
  })

  assert.equal(branding.tenantName, '')
  assert.equal(branding.logoUrl, '')
  assert.equal(branding.initials, '社团')
})

test('resolveUploadTarget supports tenant-logo preset', () => {
  assert.deepEqual(resolveUploadTarget('tenant-logo'), {
    service: 'rk-user',
    bizType: 'tenant-logo'
  })
})
