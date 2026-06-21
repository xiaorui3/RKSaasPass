import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const currentDir = path.dirname(fileURLToPath(import.meta.url))
const sqlDir = path.resolve(currentDir, '../../../ci/sql')

function readSql(fileName) {
  return readFileSync(path.join(sqlDir, fileName), 'utf8')
}

test('people-domain audit sql assets exist for all drift classes', () => {
  const overview = readSql('people_domain_audit_overview.sql')
  const membersWithoutUser = readSql('people_domain_members_without_user.sql')
  const standalone = readSql('people_domain_standalone_student_accounts.sql')
  const overlap = readSql('people_domain_alumni_member_overlap.sql')
  const tenant2LegacyFix = readSql('fix_tenant2_legacy_nonmember_user_types.sql')
  const cleanupFixtures = readSql('cleanup_tenant1_people_domain_fixtures.sql')

  assert.match(overview, /student_users_without_member/i)
  assert.match(overview, /members_without_user/i)
  assert.match(overview, /alumni_member_overlap_by_student_id/i)
  assert.match(overview, /alumni_member_overlap_by_name_blank_student_id/i)

  assert.match(overview, /rk_user\.rk_user/i)
  assert.match(overview, /rk_user\.club_members/i)
  assert.match(overview, /rk_user\.club_alumni/i)
  assert.match(overview, /tenant_id/i)
  assert.match(overview, /is_deleted/i)
  assert.doesNotMatch(membersWithoutUser, /user_type\s*=\s*2/i)
  assert.match(overview, /SELECT 'members_without_user' AS metric, COUNT\(\*\) AS total[\s\S]*?LEFT JOIN rk_user\.rk_user u[\s\S]*?AND u\.student_id COLLATE utf8mb4_unicode_ci = m\.student_id COLLATE utf8mb4_unicode_ci/i)
  assert.match(overview, /m\.position/i)
  assert.match(overview, /m\.grade/i)
  assert.match(overview, /current_timestamp|curdate|current_date|year\s*\(/i)

  assert.match(standalone, /standalone_student_accounts/i)
  assert.match(standalone, /student_id/i)
  assert.match(standalone, /user_type/i)
  assert.match(standalone, /limit 50/i)

  assert.match(overlap, /m\.position/i)
  assert.match(overlap, /m\.grade/i)
  assert.match(overlap, /student_id_match/i)
  assert.match(overlap, /blank_student_id_name_match/i)

  assert.match(tenant2LegacyFix, /tenant_id\s*=\s*2/i)
  assert.match(tenant2LegacyFix, /admin_b/i)
  assert.match(tenant2LegacyFix, /t02_user/i)
  assert.match(tenant2LegacyFix, /update\s+rk_user\.rk_user/i)
  assert.match(tenant2LegacyFix, /user_type\s*=\s*1/i)

  assert.match(cleanupFixtures, /gw_manual_check/i)
  assert.match(cleanupFixtures, /gw_manual_check@example\.com/i)
  assert.match(cleanupFixtures, /admin_created_%/i)
  assert.match(cleanupFixtures, /import_created_%/i)
  assert.match(
    cleanupFixtures,
    /DELETE FROM rk_user\.club_members[\s\S]*?admin-created-%@example\.[\s\S]*?import-created-%@example\./i
  )
  assert.match(cleanupFixtures, /import-created-%@example\./i)
  assert.match(cleanupFixtures, /register_review_request/i)
})
