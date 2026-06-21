import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const mainActivityUrl = new URL('../../../android-client/rk-club-android/app/src/main/java/com/rkclub/app/MainActivity.java', import.meta.url)

test('android profile should persist and render the real user avatar after app updates', async () => {
  const source = await readFile(mainActivityUrl, 'utf8')

  assert.match(source, /KEY_AVATAR_URL/)
  assert.match(source, /private String avatarUrl/)
  assert.match(source, /extractAvatarUrl\s*\(/)
  assert.match(source, /putString\(KEY_AVATAR_URL,\s*avatarUrl/)
  assert.match(source, /preferences\.getString\(KEY_AVATAR_URL/)
  assert.match(source, /avatarImageView\(displayName,\s*avatarUrl,\s*dp\(58\)\)/)
  assert.match(source, /avatarImageView\(displayName,\s*avatarUrl,\s*dp\(44\)\)/)
  assert.match(source, /loadAvatarBitmap/)
  assert.match(source, /resolveManagedMediaUrl\s*\(/)
  assert.match(source, /\/minio-files\//)
  assert.match(source, /extractAvatarUrl\(data\)/)
  assert.match(source, /syncProfileIdentity\s*\(/)
  assert.match(source, /\/users\/me/)
  assert.match(source, /restoreAuthSession\(\)\)\s*\{\s*showShell\(\);\s*syncProfileIdentityAsync\(\);/s)
  assert.match(source, /private void syncProfileIdentityAsync\(\)/)
  assert.doesNotMatch(source, /profileRow\.addView\(avatarView\(displayName,\s*dp\(58\)\)/)
  assert.doesNotMatch(source, /profileRow\.addView\(avatarView\(displayName,\s*dp\(44\)\)/)
})

test('android restored sessions should refresh profile name avatar and tenant identity', async () => {
  const source = await readFile(mainActivityUrl, 'utf8')

  assert.match(source, /restoreAuthSession\(\)\)\s*\{\s*showShell\(\);\s*syncProfileIdentityAsync\(\);/s)
  assert.match(source, /private boolean syncProfileIdentity\(\)/)
  assert.match(source, /String syncedName = firstValue\(data,\s*new String\[\]\{"displayName", "realName", "nickname", "username", "userName", "name"\}/)
  assert.match(source, /String syncedTenantName = firstValue\(data,\s*new String\[\]\{"tenantName", "organizationName", "orgName"\}/)
  assert.match(source, /String previousName = trim\(displayName\)/)
  assert.match(source, /String previousTenantName = trim\(tenantName\)/)
  assert.match(source, /return !trim\(displayName\)\.equals\(previousName\)/)
  assert.match(source, /private void syncProfileIdentityAsync\(\)/)
  assert.doesNotMatch(
    source,
    /private void syncProfileAvatar\(\)\s*\{\s*if \(!trim\(avatarUrl\)\.isEmpty\(\) \|\| token\.isEmpty\(\)\)/s,
    'restored sessions must not skip profile sync only because a stale local avatar exists'
  )
})
