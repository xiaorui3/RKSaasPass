import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'

const requestSource = fs.readFileSync(
  path.resolve('newpro/rk/src/utils/request.js'),
  'utf8'
)

test('request utility should import runtime gateway resolver', () => {
  assert.match(
    requestSource,
    /import\s+\{\s*getGatewayBaseUrl\s*\}\s+from\s+['"]\.\/runtimeConfig\.js['"]/,
    'request.js should import getGatewayBaseUrl from runtimeConfig.js'
  )
})

test('request utility should initialize axios baseURL from runtime gateway config', () => {
  assert.match(
    requestSource,
    /baseURL:\s*getGatewayBaseUrl\(\)\s*\|\|\s*['"]['"]/,
    'axios baseURL should prefer runtime gateway base url'
  )
})
