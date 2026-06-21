import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('./helpers/live-fixture-cleanup.cjs', import.meta.url), 'utf8')

test('cleanupRegisterFixture also removes member ledger rows for approved registration fixtures', () => {
  assert.match(
    source,
    /async function cleanupRegisterFixture\s*\(\s*\{\s*tenantId\s*=\s*1,\s*username,\s*email,\s*studentId\s*\}\s*\)/,
    'cleanupRegisterFixture should accept studentId so approved registration fixtures can be traced to member rows'
  )

  assert.match(
    source,
    /DELETE FROM rk_user\.club_members[\s\S]*student_id = \?/,
    'cleanupRegisterFixture should delete approved member ledger rows by studentId'
  )

  assert.match(
    source,
    /DELETE FROM rk_user\.club_members[\s\S]*email = \?/,
    'cleanupRegisterFixture should also delete approved member ledger rows by email fallback'
  )
})
