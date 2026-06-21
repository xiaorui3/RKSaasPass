import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('../src/views/admin/system/Roles.vue', import.meta.url), 'utf8')

assert.match(source, /getMenuTree/, 'role permission assignment must load the full menu-management tree')
assert.doesNotMatch(source, /getMyMenuTree/, 'role permission assignment must not use the current-user menu tree')

