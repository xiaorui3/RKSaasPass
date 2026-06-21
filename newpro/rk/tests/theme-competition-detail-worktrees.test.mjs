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
  'ui-v10-future-youth'
]

test('themed ui branches should keep competition detail on the shared i18n/media contract', async () => {
  const pages = await Promise.all(
    branchNames.map(async (branchName) => {
      const filePath = resolve(
        repoRoot,
        '.worktrees',
        `rtsync-${branchName}`,
        'newpro',
        'rk',
        'src',
        'views',
        'CompetitionDetail.vue'
      )
      return {
        branchName,
        source: await readFile(filePath, 'utf8')
      }
    })
  )

  for (const page of pages) {
    assert.equal(page.source.includes('useI18n'), true, `${page.branchName} should use vue-i18n`)
    assert.equal(page.source.includes('resolveMediaUrl'), true, `${page.branchName} should resolve media links`)
    assert.equal(page.source.includes("t('pages.competitionDetail.loading')"), true, `${page.branchName} should localize loading text`)
    assert.equal(page.source.includes("t('pages.competitionDetail.backToList')"), true, `${page.branchName} should localize back text`)
    assert.equal(page.source.includes("t('pages.competitionDetail.rulesFile')"), true, `${page.branchName} should localize file labels`)
  }
})
