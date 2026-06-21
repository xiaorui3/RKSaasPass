import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(
  new URL('../../../rk-gateway/src/main/resources/bootstrap.yml', import.meta.url),
  'utf8'
)

test('gateway keeps api admin auth route before generic api admin rewrite', () => {
  const authRouteIndex = source.indexOf('id: rk-user-admin-auth-api')
  const genericRouteIndex = source.indexOf('id: rk-user-api')

  assert.ok(authRouteIndex > -1)
  assert.ok(genericRouteIndex > -1)
  assert.ok(authRouteIndex < genericRouteIndex)

  const authRouteBlock = source.slice(authRouteIndex, genericRouteIndex)
  assert.match(authRouteBlock, /Path=\/api\/admin\/auth\/\*\*/)
  assert.doesNotMatch(authRouteBlock, /RewritePath=\/api\/admin/)
})

test('gateway routes new system pages api endpoints to their owning services', () => {
  const userRouteIndex = source.indexOf('id: rk-user-api')
  const mobileRouteIndex = source.indexOf('id: rk-user-mobile-api')
  const dataRouteIndex = source.indexOf('id: rk-data-api')
  const dataRouteEnd = source.indexOf('\n        - id: rk-data\n', dataRouteIndex)

  assert.ok(userRouteIndex > -1)
  assert.ok(mobileRouteIndex > userRouteIndex)
  assert.ok(dataRouteIndex > -1)
  assert.ok(dataRouteEnd > dataRouteIndex)

  const userRouteBlock = source.slice(userRouteIndex, mobileRouteIndex)
  assert.match(userRouteBlock, /\/api\/api-catalog/)
  assert.match(userRouteBlock, /\/api\/api-catalog\/\*\*/)
  assert.match(userRouteBlock, /\/api\/demo-data/)
  assert.match(userRouteBlock, /\/api\/demo-data\/\*\*/)
  assert.match(userRouteBlock, /\/api\/config\/\*\*/)

  const dataRouteBlock = source.slice(dataRouteIndex, dataRouteEnd)
  assert.match(dataRouteBlock, /Path=\/api\/data\/\*\*/)
  assert.match(dataRouteBlock, /RewritePath=\/api\/data\/\(\?<segment>\.\*\), \/data\/\$\{segment\}/)
})
