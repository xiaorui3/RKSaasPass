import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

test('finance page should store managed upload relative paths through shared helper', async () => {
  const source = await readFile(new URL('../src/views/admin/club/Finance.vue', import.meta.url), 'utf8')

  assert.match(
    source,
    /import\s+\{\s*uploadManagedFile\s*\}\s+from\s+['"]@\/utils\/fileUpload['"]/,
    'Finance.vue should use the shared managed upload helper'
  )

  assert.match(
    source,
    /const\s+\{\s*storedValue\s*\}\s*=\s*await\s+uploadManagedFile\(\s*\{\s*file:\s*proofFile\.value\s*\}\s*,\s*['"]finance['"]\s*\)/,
    'Finance.vue should persist the managed relative path returned by uploadManagedFile'
  )
})

test('tenant logo upload should reuse managed upload helper and keep relative storage', async () => {
  const source = await readFile(new URL('../src/views/admin/system/Tenants.vue', import.meta.url), 'utf8')

  assert.match(
    source,
    /import\s+\{\s*uploadManagedFile\s*\}\s+from\s+['"]@\/utils\/fileUpload['"]/,
    'Tenants.vue should use the shared managed upload helper'
  )

  assert.match(
    source,
    /const\s+\{\s*storedValue,\s*url\s*\}\s*=\s*await\s+uploadManagedFile\(\s*\{\s*file\s*\}\s*,\s*['"]tenant-logo['"]\s*\)/,
    'Tenants.vue should normalize tenant logo uploads through uploadManagedFile'
  )

  assert.match(
    source,
    /form\.logoUrl\s*=\s*storedValue/,
    'Tenants.vue should store the relative logo path instead of a public absolute URL'
  )
})
