import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const userApiSource = readFileSync(new URL('../src/api/user.js', import.meta.url), 'utf8')
const volunteerApiSource = readFileSync(new URL('../src/api/volunteer.js', import.meta.url), 'utf8')
const creditApiSource = readFileSync(new URL('../src/api/credit.js', import.meta.url), 'utf8')
const activityApiSource = readFileSync(new URL('../src/api/activity.js', import.meta.url), 'utf8')
const competitionApiSource = readFileSync(new URL('../src/api/competition.js', import.meta.url), 'utf8')

test('user api should wrap avatar uploads with a named file option for managed uploads', () => {
  assert.match(
    userApiSource,
    /export function uploadAvatar\(file\)\s*\{\s*return uploadManagedFile\(\{\s*file\s*\},\s*'avatar'\)\s*\}/,
  )
})

test('user api should require an explicit managed upload target instead of a broken common default', () => {
  assert.match(
    userApiSource,
    /upload:\s*\(file,\s*target\)\s*=>\s*uploadManagedFile\(\{\s*file\s*\},\s*target\)/,
  )
  assert.doesNotMatch(
    userApiSource,
    /type\s*=\s*'common'/,
  )
})

test('volunteer reject api should send reason as query params to match backend controller binding', () => {
  assert.match(
    volunteerApiSource,
    /rejectVolunteerRecord\(id,\s*reason\)\s*\{\s*return request\(\{\s*url:\s*`\/api\/volunteer\/\$\{id\}\/reject`,\s*method:\s*'put',\s*params:\s*\{\s*reason\s*\}\s*\}\)\s*\}/,
  )
})

test('credit reject api should send reason as query params to match backend controller binding', () => {
  assert.match(
    creditApiSource,
    /rejectCreditRecord\(id,\s*reason\)\s*\{\s*return request\(\{\s*url:\s*`\/api\/credit\/records\/\$\{id\}\/reject`,\s*method:\s*'put',\s*params:\s*\{\s*reason\s*\}\s*\}\)\s*\}/,
  )
})

test('activity api should expose admin credit grant endpoint', () => {
  assert.match(
    activityApiSource,
    /export function grantActivityCredits\(id\)\s*\{\s*return request\(\{\s*url:\s*`\/api\/activity\/\$\{id\}\/credits\/grant`,\s*method:\s*'post'\s*\}\)\s*\}/,
  )
})

test('competition api should expose competition credit grant endpoint', () => {
  assert.match(
    competitionApiSource,
    /export function grantCompetitionCredits\(id\)\s*\{\s*return request\(\{\s*url:\s*`\/api\/competition\/\$\{id\}\/results\/credits\/grant`,\s*method:\s*'post'\s*\}\)\s*\}/,
  )
})
