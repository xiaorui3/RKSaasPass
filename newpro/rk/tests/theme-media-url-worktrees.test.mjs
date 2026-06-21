import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { resolve } from 'node:path'

const repoRoot = resolve(import.meta.dirname, '..', '..', '..')
const branchNames = [
  'ui-v01-campus-atlas',
  'ui-v02-editorial-newsroom',
  'ui-v03-apple-campus',
  'ui-v04-huawei-enterprise',
  'ui-v05-bento-ops',
  'ui-v06-swiss-grid',
  'ui-v07-industrial-slate',
  'ui-v08-campus-festival',
  'ui-v09-academic-prestige',
  'ui-v10-future-youth',
]

test('themed ui branches should keep the shared legacy-media cleanup contract', async () => {
  const files = await Promise.all(
    branchNames.map(async (branchName) => {
      const filePath = resolve(
        repoRoot,
        '.worktrees',
        `rtsync-${branchName}`,
        'newpro',
        'rk',
        'src',
        'utils',
        'mediaUrl.js',
      )
      return {
        branchName,
        source: await readFile(filePath, 'utf8'),
      }
    }),
  )

  for (const file of files) {
    assert.equal(file.source.includes('function isKnownBrokenLegacyMediaPath(value)'), true, `${file.branchName} should define legacy media guard`)
    assert.equal(file.source.includes("withoutLegacyBucket.startsWith('images/')"), true, `${file.branchName} should drop legacy /images paths`)
    assert.equal(file.source.includes("withoutLegacyBucket.startsWith('tmp/')"), true, `${file.branchName} should drop legacy tmp paths`)
    assert.equal(file.source.includes("withoutLegacyBucket.startsWith('users/avatars/')"), true, `${file.branchName} should drop legacy users/avatars paths`)
    assert.equal(file.source.includes("withoutLegacyBucket.startsWith('avatar/')"), true, `${file.branchName} should drop legacy avatar paths`)
    assert.equal(file.source.includes('if (isKnownBrokenLegacyMediaPath(trimmed)) {'), true, `${file.branchName} should suppress broken legacy media at resolve time`)
  }
})
