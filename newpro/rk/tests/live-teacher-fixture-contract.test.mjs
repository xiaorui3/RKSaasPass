import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const CASES = [
  '../tests/full-role-audit.spec.cjs',
  '../tests/role-button-audit.spec.cjs',
  '../tests/role-matrix-audit.spec.cjs',
  '../tests/news-approval-api-auth.spec.cjs',
  '../tests/role-gap-fixes.spec.cjs',
  '../tests/test-teacher-activity-approval.spec.cjs',
  '../tests/competition-approval-flow.spec.cjs',
  '../tests/competition-approval-page.spec.cjs',
  '../tests/cross-tenant-content.spec.cjs',
  '../tests/activity-registration-deadline.spec.cjs',
]

for (const relativePath of CASES) {
  test(`live role audit spec should use shared active teacher fixture: ${relativePath}`, () => {
    const source = readFileSync(new URL(relativePath, import.meta.url), 'utf8')
    assert.match(
      source,
      /TENANT1_LIVE_TEACHER/,
      `${relativePath} should use the shared active tenant-1 teacher fixture`,
    )
    assert.doesNotMatch(
      source,
      /teacher_a['"]/,
      `${relativePath} should not hardcode the dormant teacher_a credential`,
    )
  })
}
