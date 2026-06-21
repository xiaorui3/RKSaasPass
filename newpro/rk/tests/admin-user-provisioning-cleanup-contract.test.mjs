import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const cleanupSource = readFileSync(new URL('./helpers/live-fixture-cleanup.cjs', import.meta.url), 'utf8')
const provisioningSource = readFileSync(new URL('./admin-user-provisioning.spec.cjs', import.meta.url), 'utf8')

test('admin user provisioning smoke should clean imported and created managed users after runtime verification', () => {
  assert.match(
    cleanupSource,
    /async function cleanupManagedUserFixture\s*\(\s*\{\s*tenantId\s*=\s*1,\s*username,\s*email,\s*studentId\s*\}\s*\)/,
    'live fixture cleanup helper should expose cleanupManagedUserFixture for managed user provisioning smoke'
  )

  assert.match(
    cleanupSource,
    /DELETE FROM rk_user\.rk_user[\s\S]*DELETE FROM rk_auth\.user/,
    'cleanupManagedUserFixture should remove both local user rows and auth user rows'
  )

  assert.match(
    provisioningSource,
    /cleanupManagedUserFixture/,
    'admin-user-provisioning smoke should call cleanupManagedUserFixture so imported runtime users do not accumulate on tenant 1'
  )
})
