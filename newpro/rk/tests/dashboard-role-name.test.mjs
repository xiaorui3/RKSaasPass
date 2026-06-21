import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

test('dashboard maps normalized tenant admin roles to admin labels', () => {
  const source = readFileSync(new URL('../src/views/admin/Dashboard.vue', import.meta.url), 'utf8')

  assert.match(source, /3:\s*'租户管理员'/)
  assert.match(source, /5:\s*'租户管理员'/)
})
