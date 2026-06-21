import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

test('profile avatar upload should reuse shared managed upload helper', async () => {
  const source = await readFile(new URL('../src/views/Profile.vue', import.meta.url), 'utf8')

  assert.match(
    source,
    /import\s+\{\s*uploadManagedFile\s*\}\s+from\s+['"]@\/utils\/fileUpload['"]/,
    'Profile.vue should import uploadManagedFile from the shared upload helper'
  )

  assert.match(
    source,
    /const\s+\{\s*storedValue\s*\}\s*=\s*await\s+uploadManagedFile\(\s*\{\s*file:\s*options\.file\s*\}\s*,\s*['"]avatar['"]\s*\)/,
    'Profile.vue should upload avatars through uploadManagedFile so avatar parsing stays aligned with other modules'
  )

  assert.doesNotMatch(
    source,
    /uploadApi\.uploadAvatar\(\s*options\.file\s*\)/,
    'Profile.vue should not keep a separate avatar upload parsing path'
  )
})
