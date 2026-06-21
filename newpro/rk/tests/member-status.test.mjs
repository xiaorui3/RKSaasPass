import test from 'node:test'
import assert from 'node:assert/strict'

const moduleUrl = new URL('../src/utils/memberStatus.js', import.meta.url)

test('normalizeMemberStatusValue should treat 正常 as active member', async () => {
  const memberStatus = await import(`${moduleUrl.href}?case=normal-cn`)

  assert.equal(memberStatus.normalizeMemberStatusValue('正常'), 1)
})

test('normalizeMemberStatusValue should treat 活跃 as active member', async () => {
  const memberStatus = await import(`${moduleUrl.href}?case=active-cn`)

  assert.equal(memberStatus.normalizeMemberStatusValue('活跃'), 1)
})

test('normalizeMemberStatusValue should keep historical mojibake active values compatible', async () => {
  const memberStatus = await import(`${moduleUrl.href}?case=active-mojibake`)

  assert.equal(memberStatus.normalizeMemberStatusValue('姝ｅ父'), 1)
  assert.equal(memberStatus.normalizeMemberStatusValue('娲昏穬'), 1)
})

test('normalizeMemberStatusValue should treat active as active member', async () => {
  const memberStatus = await import(`${moduleUrl.href}?case=active-en`)

  assert.equal(memberStatus.normalizeMemberStatusValue('active'), 1)
})

test('normalizeMemberStatusValue should keep pending values inactive', async () => {
  const memberStatus = await import(`${moduleUrl.href}?case=pending`)

  assert.equal(memberStatus.normalizeMemberStatusValue('待确认'), 0)
})

test('normalizeMemberStatusValue should keep unknown values inactive', async () => {
  const memberStatus = await import(`${moduleUrl.href}?case=unknown`)

  assert.equal(memberStatus.normalizeMemberStatusValue('未知状态'), 0)
})
