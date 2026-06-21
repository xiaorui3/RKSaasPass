import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildDashboardStats,
  buildPeopleDomainAlertTitle,
  buildPeopleDomainDeltaLabel,
  buildPeopleDomainDiagnostics,
  buildPeopleDomainSourceHint,
  buildPeopleDomainSummary,
  buildStatisticsBoardCards,
  hasPeopleDomainUserFilters,
} from '../src/utils/adminPeopleDomain.js'

test('buildPeopleDomainSummary keeps account, member, join, register, and alumni counts isolated', () => {
  const summary = buildPeopleDomainSummary({
    userPage: { total: 264 },
    userStats: {
      accountCount: 264,
      adminAccountCount: 3,
      teacherAccountCount: 9,
      studentAccountCount: 252,
      studentAccountWithStudentIdCount: 46,
      studentAccountWithoutStudentIdCount: 198,
      studentAccountMissingMemberLedgerCount: 8,
      standaloneStudentAccountCount: 206,
      memberLinkedAccountCount: 46,
    },
    memberStats: { total: 46 },
    admissionStats: {
      total: 68,
      pendingCount: 22,
      approvedCount: 46,
      rejectedCount: 0,
    },
    registerReviewStats: {
      total: 16,
      pendingCount: 16,
      approvedCount: 0,
      rejectedCount: 0,
    },
    alumniOverview: {
      totalCount: 349,
      rosterCount: 349,
      currentCount: 0,
      graduatedCount: 337,
    },
  })

  assert.deepEqual(summary, {
    accountCount: 264,
    memberCount: 46,
    memberLinkedAccountCount: 46,
    nonMemberAccountCount: 218,
    adminAccountCount: 3,
    teacherAccountCount: 9,
    studentAccountCount: 252,
    studentAccountWithStudentIdCount: 46,
    studentAccountWithoutStudentIdCount: 198,
    studentAccountMissingMemberLedgerCount: 8,
    standaloneStudentAccountCount: 206,
    standaloneStudentAccountSamples: [],
    applicationTotal: 68,
    pendingApplicationCount: 22,
    approvedApplicationCount: 46,
    rawApprovedApplicationCount: 46,
    rejectedApplicationCount: 0,
    joinApplicationTotal: 68,
    pendingJoinApplicationCount: 22,
    approvedJoinApplicationCount: 46,
    rawApprovedJoinApplicationCount: 46,
    rejectedJoinApplicationCount: 0,
    registerReviewTotal: 16,
    pendingRegisterReviewCount: 16,
    approvedRegisterReviewCount: 0,
    rejectedRegisterReviewCount: 0,
    workflowTotal: 84,
    workflowPendingCount: 38,
    workflowApprovedCount: 46,
    workflowRejectedCount: 0,
    alumniCount: 337,
    alumniRosterCount: 349,
    activeAlumniCount: 0,
    graduatedAlumniCount: 337,
  })
})

test('buildPeopleDomainSummary uses unique approved applicants when backend provides it', () => {
  const summary = buildPeopleDomainSummary({
    userPage: { total: 40 },
    memberStats: { total: 32 },
    admissionStats: {
      total: 68,
      pendingCount: 22,
      approvedCount: 46,
      approvedUniqueApplicantCount: 32,
      rejectedCount: 0,
    },
    registerReviewStats: { total: 0, pendingCount: 0, approvedCount: 0, rejectedCount: 0 },
    alumniOverview: { totalCount: 0, rosterCount: 0, currentCount: 0, graduatedCount: 0 },
  })

  assert.equal(summary.approvedJoinApplicationCount, 32)
  assert.equal(summary.rawApprovedJoinApplicationCount, 46)
  assert.equal(summary.workflowApprovedCount, 32)
})

test('buildPeopleDomainSummary falls back to user page totals when user stats are absent', () => {
  const summary = buildPeopleDomainSummary({
    userPage: { total: 65 },
    memberStats: { total: 16 },
    admissionStats: { total: 5, pendingCount: 5, approvedCount: 0, rejectedCount: 0 },
    registerReviewStats: { total: 2, pendingCount: 1, approvedCount: 1, rejectedCount: 0 },
    alumniOverview: { totalCount: 349, rosterCount: 349, currentCount: 0, graduatedCount: 337 },
  })

  assert.equal(summary.memberLinkedAccountCount, 16)
  assert.equal(summary.nonMemberAccountCount, 49)
  assert.equal(summary.adminAccountCount, 0)
  assert.equal(summary.alumniCount, 337)
  assert.equal(summary.alumniRosterCount, 349)
  assert.deepEqual(summary.standaloneStudentAccountSamples, [])
  assert.equal(summary.studentAccountWithoutStudentIdCount, 0)
  assert.equal(summary.studentAccountMissingMemberLedgerCount, 0)
})

test('buildStatisticsBoardCards exposes separated join and register queues explicitly', () => {
  const summary = buildPeopleDomainSummary({
    userPage: { total: 264 },
    memberStats: { total: 46 },
    admissionStats: { total: 68, pendingCount: 22, approvedCount: 46, rejectedCount: 0 },
    registerReviewStats: { total: 16, pendingCount: 16, approvedCount: 0, rejectedCount: 0 },
    alumniOverview: { totalCount: 349, rosterCount: 349, currentCount: 0, graduatedCount: 337 },
  })

  const cards = buildStatisticsBoardCards(summary, {
    activities: 13,
    news: 9,
    works: 7,
    competitions: 4,
    notices: 3,
  })

  assert.equal(cards[0].label, '账号数')
  assert.equal(cards[0].hint, '当前租户范围 rk_user 本地账号')
  assert.equal(cards[1].label, '成员数')
  assert.equal(cards[2].label, '待审核入社')
  assert.equal(cards[2].value, 22)
  assert.equal(cards[3].label, '待审核注册')
  assert.equal(cards[3].value, 16)
  assert.equal(cards[4].label, '已通过入社')
  assert.equal(cards[5].label, '毕业校友数')
})

test('buildDashboardStats places separated queue metrics before alumni and content metrics', () => {
  const summary = buildPeopleDomainSummary({
    userPage: { total: 264 },
    memberStats: { total: 46 },
    admissionStats: { total: 68, pendingCount: 22, approvedCount: 46, rejectedCount: 0 },
    registerReviewStats: { total: 16, pendingCount: 16, approvedCount: 0, rejectedCount: 0 },
    alumniOverview: { totalCount: 349, rosterCount: 349, currentCount: 0, graduatedCount: 337 },
  })

  const cards = buildDashboardStats(summary, {
    activities: 13,
    news: 9,
  })

  assert.deepEqual(
    cards.map((item) => item.key),
    ['accounts', 'members', 'pendingJoinApplications', 'pendingRegisterReviews', 'alumni', 'activities'],
  )
  assert.equal(cards[0].label, '账号数')
  assert.equal(cards[2].value, 22)
  assert.equal(cards[3].value, 16)
})

test('people-domain copy switches between platform scope and tenant scope', () => {
  assert.equal(
    buildPeopleDomainSourceHint('club_members', true),
    '平台可见范围 club_members',
  )
  assert.equal(
    buildPeopleDomainSourceHint('club_members', false),
    '当前租户范围 club_members',
  )
  assert.match(
    buildPeopleDomainAlertTitle(false),
    /成员台账只统计正式学生成员/,
  )
  assert.match(buildPeopleDomainAlertTitle(true), /平台可见范围/)
  assert.match(buildPeopleDomainAlertTitle(false), /当前租户范围/)
})

test('people-domain helpers detect account-side filters and switch copy', () => {
  assert.equal(hasPeopleDomainUserFilters({}), false)
  assert.equal(hasPeopleDomainUserFilters({ username: 'admin_a' }), true)
  assert.equal(hasPeopleDomainUserFilters({ mobile: '13800138000' }), true)
  assert.equal(hasPeopleDomainUserFilters({ status: 1 }), true)
  assert.equal(buildPeopleDomainDeltaLabel(false), '账号差额')
  assert.equal(buildPeopleDomainDeltaLabel(true), '筛选差额')
  assert.match(
    buildPeopleDomainAlertTitle(false, { hasUserFilters: true }),
    /成员台账、注册审核、入社审核和校友统计仍按租户口径展示/,
  )
})

test('buildPeopleDomainDiagnostics explains account/member, join/member, and register backlog gaps', () => {
  const summary = buildPeopleDomainSummary({
    userPage: { total: 65 },
    userStats: {
      accountCount: 65,
      adminAccountCount: 2,
      teacherAccountCount: 8,
      studentAccountCount: 55,
      studentAccountWithStudentIdCount: 16,
      studentAccountWithoutStudentIdCount: 39,
      studentAccountMissingMemberLedgerCount: 0,
      standaloneStudentAccountCount: 39,
      standaloneStudentAccountSamples: ['member_a', 't01_user'],
      memberLinkedAccountCount: 16,
    },
    memberStats: { total: 16 },
    admissionStats: { total: 5, pendingCount: 5, approvedCount: 0, rejectedCount: 0 },
    registerReviewStats: { total: 3, pendingCount: 2, approvedCount: 1, rejectedCount: 0 },
    alumniOverview: { totalCount: 349, rosterCount: 349, currentCount: 0, graduatedCount: 337 },
  })

  const diagnostics = buildPeopleDomainDiagnostics(summary)

  assert.equal(diagnostics[0].key, 'accountMemberDelta')
  assert.equal(diagnostics[0].value, 49)
  assert.match(diagnostics[0].helper, /管理员账号 2/)
  assert.match(diagnostics[0].helper, /指导老师账号 8/)
  assert.match(diagnostics[0].helper, /无学号的学生账号 39/)
  assert.match(diagnostics[0].helper, /有学号但未进入成员台账的学生账号 0/)
  assert.match(diagnostics[0].helper, /member_a/)
  assert.match(diagnostics[0].helper, /t01_user/)
  assert.equal(diagnostics[1].key, 'approvedMemberDelta')
  assert.equal(diagnostics[1].value, 16)
  assert.match(diagnostics[1].helper, /后台导入|手工建档|统一账号同步/)
  assert.equal(diagnostics[2].key, 'registerReviewBacklog')
  assert.equal(diagnostics[2].value, 2)
  assert.equal(diagnostics[3].value, 337)
})

test('buildPeopleDomainSummary prefers graduated alumni totals over roster totals for admin graduation metrics', () => {
  const summary = buildPeopleDomainSummary({
    alumniOverview: {
      totalCount: 20,
      rosterCount: 20,
      currentCount: 4,
      graduatedCount: 16,
    },
  })

  assert.equal(summary.alumniCount, 16)
  assert.equal(summary.alumniRosterCount, 20)
  assert.equal(summary.activeAlumniCount, 4)
  assert.equal(summary.graduatedAlumniCount, 16)
})
