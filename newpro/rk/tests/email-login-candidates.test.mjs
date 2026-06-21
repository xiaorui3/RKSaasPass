import test from 'node:test'
import assert from 'node:assert/strict'

import { mapEmailLoginCandidates } from '../src/utils/emailLogin.js'

test('mapEmailLoginCandidates should attach tenant names and readable labels', () => {
  const mapped = mapEmailLoginCandidates(
    [
      { authUserId: 201, tenantId: 1, username: 'admin_a', displayName: '管理员A' },
      { authUserId: 202, tenantId: 2, username: 'admin_b', displayName: '管理员B' }
    ],
    [
      { id: 1, tenantName: '租户一' },
      { id: 2, tenantName: '租户二' }
    ]
  )

  assert.equal(mapped[0].tenantName, '租户一')
  assert.equal(mapped[0].label, '管理员A · 租户一')
  assert.equal(mapped[1].label, '管理员B · 租户二')
})

test('mapEmailLoginCandidates should fall back to username and tenant id', () => {
  const mapped = mapEmailLoginCandidates(
    [{ authUserId: 203, tenantId: 9, username: 'member_a' }],
    []
  )

  assert.equal(mapped[0].tenantName, '租户 9')
  assert.equal(mapped[0].label, 'member_a · 租户 9')
})
