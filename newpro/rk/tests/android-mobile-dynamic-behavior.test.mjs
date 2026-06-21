import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const androidRoot = new URL('../../../android-client/rk-club-android/', import.meta.url)

async function readAndroidSource(relativePath) {
  return readFile(new URL(relativePath, androidRoot), 'utf8')
}

function methodBody(source, methodName) {
  const marker = `private void ${methodName}(`
  const start = source.indexOf(marker)
  assert.notEqual(start, -1, `${methodName} should exist`)
  const next = source.indexOf('\n    private ', start + marker.length)
  return source.slice(start, next === -1 ? source.length : next)
}

function privateMethodBody(source, returnType, methodName) {
  const marker = `private ${returnType} ${methodName}(`
  const start = source.indexOf(marker)
  assert.notEqual(start, -1, `${methodName} should exist`)
  const next = source.indexOf('\n    private ', start + marker.length)
  return source.slice(start, next === -1 ? source.length : next)
}

test('android profile metrics should be loaded from APIs instead of hard-coded numbers', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')
  const showProfile = methodBody(source, 'showProfile')

  assert.match(source, /loadProfileMetrics\(/)
  assert.match(source, /renderProfileMetricPills\(/)
  assert.match(source, /\/api\/competition\/my/)
  assert.match(source, /\/api\/competition\/my\/registrations/)
  assert.match(source, /\/api\/notifications/)
  assert.equal(showProfile.includes('metricPill("我的活动", "12"'), false)
  assert.equal(showProfile.includes('metricPill("我的比赛", "5"'), false)
  assert.equal(showProfile.includes('metricPill("我的社团", "8"'), false)
  assert.equal(showProfile.includes('metricPill("我的关注", "256"'), false)
})

test('android profile metrics should render cached client data first and refresh in background', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')
  const showProfile = methodBody(source, 'showProfile')
  const loadMetrics = methodBody(source, 'loadProfileMetrics')

  assert.match(source, /PROFILE_METRICS_CACHE_PREFIX/)
  assert.match(source, /loadCachedProfileMetrics\(/)
  assert.match(source, /saveCachedProfileMetrics\(/)
  assert.match(showProfile, /loadCachedProfileMetrics\(\)/)
  assert.match(showProfile, /renderProfileMetricPills\(stats,\s*cachedMetrics/)
  assert.match(showProfile, /正在更新/)
  assert.match(loadMetrics, /saveCachedProfileMetrics\(metrics\)/)
  assert.match(source, /defaultProfileMetrics\(/)
  assert.equal(showProfile.includes('renderProfileMetricPills(stats, null)'), false)
  assert.equal(source.includes('metrics == null ? "--"'), false)
})

test('android profile metrics should be clickable entrances to their detail screens', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')
  const renderMetrics = methodBody(source, 'renderProfileMetricPills')

  assert.match(source, /myCompetitionsScreenAction\(/)
  assert.match(source, /showMyCompetitions\(/)
  assert.match(renderMetrics, /myServicesScreenAction\(\)/)
  assert.match(renderMetrics, /myCompetitionsScreenAction\(\)/)
  assert.match(renderMetrics, /myClubsScreenAction\(\)/)
  assert.match(renderMetrics, /messagesScreenAction\(\)/)
  assert.match(source, /setOnClickListener/)
})

test('android login should support email verification and same-email tenant-role switching', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')

  assert.match(source, /sendEmailLoginCode\(\)/)
  assert.match(source, /emailCodeLogin\(\)/)
  assert.match(source, /showEmailLoginCandidateDialog\(/)
  assert.match(source, /confirmEmailLogin\(/)
  assert.match(source, /\/auth\/email-login\/prepare/)
  assert.match(source, /\/auth\/email-login\/confirm/)
  assert.match(source, /\/api\/email-verification\/send/)
  assert.match(source, /scene\\":\\"LOGIN\\"/)
  assert.match(source, /\/auth\/switchable-tenants/)
  assert.match(source, /renderAccountTenantCards\(/)
  assert.match(source, /switchAccountTenant\(/)
  assert.match(source, /roleId/)
  assert.match(source, /\/auth\/switch-tenant\//)
  assert.match(source, /showShell\(\)/)
  assert.match(source, /profileMetricsCacheKey\(\)[\s\S]*roleId/)
  assert.match(source, /profileSectionCacheKey\(String section\)[\s\S]*roleId/)
  assert.match(source, /connection\.setRequestProperty\("X-Role-Id", String\.valueOf\(roleId\)\)/)
  assert.match(source, /data\.optString\("roleId", data\.optString\("role_id", "0"\)\)/)
  assert.doesNotMatch(source, /email login only/)
})

test('android profile detail sections should render cached client data before background refresh', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')
  const services = methodBody(source, 'showMyServices')
  const competitions = methodBody(source, 'showMyCompetitions')
  const clubs = methodBody(source, 'showMyClubs')
  const messages = methodBody(source, 'showMessages')

  assert.match(source, /PROFILE_SECTION_CACHE_PREFIX/)
  assert.match(source, /loadCachedProfileSection\(/)
  assert.match(source, /saveProfileSectionCache\(/)
  for (const body of [services, competitions, clubs, messages]) {
    assert.match(body, /loadCachedProfileSection\(/)
    assert.match(body, /正在更新/)
    assert.match(body, /saveProfileSectionCache\(/)
    assert.equal(body.includes('body.addView(loading)'), false)
    assert.equal(body.includes('正在加载'), false)
  }
  assert.match(services, /renderMyServices\(/)
  assert.match(competitions, /renderMyCompetitions\(/)
  assert.match(clubs, /renderMyClubs\(/)
  assert.match(messages, /renderNotifications\(/)
})

test('android activity and competition status tabs should be clickable filters', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')

  assert.match(source, /private enum MobileStatusFilter/)
  assert.match(source, /activityListPathForFilter\(/)
  assert.match(source, /competitionListPathForFilter\(/)
  assert.match(source, /mobileStatusTabs\(MobileStatusFilter selected, View\.OnClickListener/)
  assert.match(source, /filterItemsByStatus\(/)
  assert.match(source, /classifyActivityStatus\(/)
  assert.match(source, /classifyCompetitionStatus\(/)
  assert.match(source, /tab\.setOnClickListener/)
  assert.match(source, /showActivityHighlights\(filter\)/)
  assert.match(source, /showCompetitionCenter\(filter\)/)
  assert.match(source, /\/api\/activity\/status\//)
  assert.match(source, /\/api\/competition\/status\//)
  assert.equal(/showActivityHighlights[\s\S]*request\("GET", "\/api\/activity\/hot\?limit=20"/.test(source), false)
  assert.equal(/showCompetitionCenter[\s\S]*request\("GET", "\/api\/competition\/published"/.test(source), false)
})

test('android activity status classification should let time override stale explicit status', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')
  const classifyActivity = privateMethodBody(source, 'MobileStatusFilter', 'classifyActivityStatus')

  const explicitIndex = classifyActivity.indexOf('MobileStatusFilter explicit = classifyStatusText')
  const endedIndex = classifyActivity.indexOf('return MobileStatusFilter.ENDED')
  const ongoingIndex = classifyActivity.indexOf('return MobileStatusFilter.ONGOING')

  assert.ok(endedIndex >= 0, 'activity classifier should mark ended rows from time')
  assert.ok(ongoingIndex >= 0, 'activity classifier should mark ongoing rows from time')
  assert.ok(explicitIndex >= 0, 'activity classifier should still use explicit status as fallback')
  assert.ok(endedIndex < explicitIndex, 'expired endTime must override stale activityStatus=1 or 2')
  assert.ok(ongoingIndex < explicitIndex, 'current start/end window must override stale activityStatus=1 or 2')
})

test('android comments should open commenter public profile when identity is present', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')
  const renderComments = methodBody(source, 'renderCommentList')

  assert.match(source, /userProfileScreenAction\(/)
  assert.match(source, /showUserProfile\(/)
  assert.match(renderComments, /extractUserId\(item\)/)
  assert.match(renderComments, /row\.setOnClickListener/)
  assert.match(source, /\/users\/internal\/by-auth-ids\?authUserIds=/)
  assert.match(source, /selectCurrentTenantUserProfile\(/)
  assert.equal(source.includes('/users/list?ids='), false)
})

test('android registration action should be disabled before registration opens or after it closes', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')

  assert.match(source, /RegistrationGate/)
  assert.match(source, /buildRegistrationGate\(/)
  assert.match(source, /parseDateTimeMillis\(/)
  assert.match(source, /报名未开始|报名已截止|活动已结束|比赛已结束|已报名/)
  assert.match(source, /detailBottomActionBar\(body, buildRegistrationGate/)
  assert.match(source, /signup\.setEnabled\(gate\.enabled\)/)
})

test('android tenant directory should defensively filter current-tenant people rows', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')
  const clubDirectory = methodBody(source, 'showClubDirectory')
  const renderPeople = methodBody(source, 'renderPeopleCards')

  assert.match(clubDirectory, /tenantId=/)
  assert.match(source, /filterRowsByCurrentTenant\(/)
  assert.match(renderPeople, /filterRowsByCurrentTenant\(rows\)/)
  assert.match(source, /belongsToCurrentTenant\(/)
})

test('android activity and competition feeds should defensively filter current-tenant rows', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')
  const activityCenter = methodBody(source, 'renderActivityCenter')
  const activityHighlights = methodBody(source, 'renderActivityHighlights')
  const competitionCenter = methodBody(source, 'renderCompetitionCenter')

  assert.match(activityCenter, /filterRowsByCurrentTenant\(/)
  assert.match(activityHighlights, /filterRowsByCurrentTenant\(/)
  assert.match(competitionCenter, /filterRowsByCurrentTenant\(/)
})

test('android my clubs page should render current-tenant API data instead of static cards', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')
  const renderMyClubs = methodBody(source, 'renderMyClubs')

  assert.match(renderMyClubs, /filterRowsByCurrentTenant\(/)
  assert.match(renderMyClubs, /renderMyClubRows\(/)
  assert.equal(renderMyClubs.includes('myClubCard("'), false)
  assert.equal(renderMyClubs.includes('R.drawable.hero_mobile_activity'), false)
})
