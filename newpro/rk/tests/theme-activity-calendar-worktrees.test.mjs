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

async function readThemeSource(branchName, ...segments) {
  return readFile(resolve(repoRoot, '.worktrees', `rtsync-${branchName}`, 'newpro', 'rk', 'src', ...segments), 'utf8')
}

async function readSnapshotSource(branchName, ...segments) {
  return readFile(resolve(repoRoot, 'ui-branch-snapshots', branchName, 'newpro', 'rk', 'src', ...segments), 'utf8')
}

async function collectBranchSources(reader) {
  return Promise.all(
    branchNames.map(async (branchName) => ({
      branchName,
      calendar: await reader(branchName, 'views', 'ActivityCalendar.vue'),
      zh: await reader(branchName, 'i18n', 'zh.js'),
      en: await reader(branchName, 'i18n', 'en.js'),
    })),
  )
}

function assertCalendarContract(branches, label) {
  for (const branch of branches) {
    assert.equal(branch.calendar.includes("import { getPublishedCompetitions } from '@/api/competition'"), true, `${label}/${branch.branchName} should load competitions`)
    assert.equal(branch.calendar.includes("getPublishedCompetitions('competition_start', 'asc')"), true, `${label}/${branch.branchName} should sort competitions by start time`)
    assert.equal(branch.calendar.includes("type: 'competition'"), true, `${label}/${branch.branchName} should normalize competition events`)
    assert.equal(branch.calendar.includes('cell-event--competition'), true, `${label}/${branch.branchName} should style competition events separately`)
    assert.equal(branch.calendar.includes("router.push(evt.type === 'competition' ? `/competition/${evt.id}` : `/activities/${evt.id}`)"), true, `${label}/${branch.branchName} should route each event type correctly`)
    assert.equal(branch.zh.includes("calendar: '活动比赛日历'"), true, `${label}/${branch.branchName} should expose Chinese calendar nav copy`)
    assert.equal(branch.zh.includes("competitionLabel: '比赛'"), true, `${label}/${branch.branchName} should expose Chinese competition label`)
    assert.equal(branch.en.includes("calendar: 'Activity & Competition Calendar'"), true, `${label}/${branch.branchName} should expose English calendar nav copy`)
    assert.equal(branch.en.includes("competitionLabel: 'Competition'"), true, `${label}/${branch.branchName} should expose English competition label`)
  }
}

test('themed ui branches should keep the activity and competition calendar contract', async () => {
  assertCalendarContract(await collectBranchSources(readThemeSource), 'rtsync-worktree')
})

test('theme snapshots should keep the activity and competition calendar contract', async () => {
  assertCalendarContract(await collectBranchSources(readSnapshotSource), 'snapshot')
})
