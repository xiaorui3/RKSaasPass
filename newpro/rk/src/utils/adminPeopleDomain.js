function toNumber(value) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

export function resolvePeopleDomainScopeLabel(isSuperAdmin = false) {
  return isSuperAdmin ? '平台可见范围' : '当前租户范围'
}

export function buildPeopleDomainSourceHint(source, isSuperAdmin = false, suffix = '') {
  const base = `${resolvePeopleDomainScopeLabel(isSuperAdmin)} ${source}`
  return suffix ? `${base} ${suffix}` : base
}

export function hasPeopleDomainUserFilters(filters = {}) {
  return Boolean(
    String(filters.username || '').trim()
      || String(filters.mobile || '').trim()
      || filters.status === 0
      || filters.status === 1
  )
}

export function buildPeopleDomainDeltaLabel(hasUserFilters = false) {
  return hasUserFilters ? '筛选差额' : '账号差额'
}

export function buildPeopleDomainAlertTitle(isSuperAdmin = false, options = {}) {
  if (options.hasUserFilters) {
    return `${resolvePeopleDomainScopeLabel(isSuperAdmin)}内，当前仅对账号列表应用了关键字或状态筛选；成员台账、注册审核、入社审核和校友统计仍按租户口径展示，因此差额只表示筛选结果与正式台账的口径差，不等同于异常数据。`
  }
  return `${resolvePeopleDomainScopeLabel(isSuperAdmin)}内，账号、成员、入社申请、注册审核、校友分别来自不同业务表；成员台账只统计正式学生成员，管理员、指导老师和未绑定学号的学生账号会继续保留在账号域中展示。`
}

export function buildPeopleDomainSummary({
  userPage,
  userStats,
  memberStats,
  admissionStats,
  registerReviewStats,
  alumniOverview
} = {}) {
  const accountCount = toNumber(userStats?.accountCount ?? userPage?.total)
  const memberCount = toNumber(memberStats?.total)
  const memberLinkedAccountCount = toNumber(
    userStats?.memberLinkedAccountCount ?? Math.min(accountCount, memberCount)
  )
  const nonMemberAccountCount = Math.max(accountCount - memberLinkedAccountCount, 0)
  const adminAccountCount = toNumber(userStats?.adminAccountCount)
  const teacherAccountCount = toNumber(userStats?.teacherAccountCount)
  const studentAccountCount = toNumber(userStats?.studentAccountCount)
  const studentAccountWithStudentIdCount = toNumber(userStats?.studentAccountWithStudentIdCount)
  const studentAccountWithoutStudentIdCount = toNumber(userStats?.studentAccountWithoutStudentIdCount)
  const studentAccountMissingMemberLedgerCount = toNumber(userStats?.studentAccountMissingMemberLedgerCount)
  const standaloneStudentAccountCount = toNumber(userStats?.standaloneStudentAccountCount)
  const standaloneStudentAccountSamples = Array.isArray(userStats?.standaloneStudentAccountSamples)
    ? userStats.standaloneStudentAccountSamples.filter((item) => String(item || '').trim())
    : []

  const joinApplicationTotal = toNumber(admissionStats?.total)
  const pendingJoinApplicationCount = toNumber(admissionStats?.pendingCount)
  const rawApprovedJoinApplicationCount = toNumber(admissionStats?.approvedCount)
  const approvedJoinApplicationCount = toNumber(
    admissionStats?.approvedUniqueApplicantCount ?? rawApprovedJoinApplicationCount
  )
  const rejectedJoinApplicationCount = toNumber(admissionStats?.rejectedCount)
  const registerReviewTotal = toNumber(registerReviewStats?.total)
  const pendingRegisterReviewCount = toNumber(registerReviewStats?.pendingCount)
  const approvedRegisterReviewCount = toNumber(registerReviewStats?.approvedCount)
  const rejectedRegisterReviewCount = toNumber(registerReviewStats?.rejectedCount)
  const alumniRosterCount = toNumber(alumniOverview?.rosterCount ?? alumniOverview?.totalCount)
  const graduatedAlumniCount = toNumber(
    alumniOverview?.graduatedCount ?? alumniOverview?.alumniCount ?? alumniOverview?.totalCount
  )

  return {
    accountCount,
    memberCount,
    memberLinkedAccountCount,
    nonMemberAccountCount,
    adminAccountCount,
    teacherAccountCount,
    studentAccountCount,
    studentAccountWithStudentIdCount,
    studentAccountWithoutStudentIdCount,
    studentAccountMissingMemberLedgerCount,
    standaloneStudentAccountCount,
    standaloneStudentAccountSamples,
    applicationTotal: joinApplicationTotal,
    pendingApplicationCount: pendingJoinApplicationCount,
    approvedApplicationCount: approvedJoinApplicationCount,
    rawApprovedApplicationCount: rawApprovedJoinApplicationCount,
    rejectedApplicationCount: rejectedJoinApplicationCount,
    joinApplicationTotal,
    pendingJoinApplicationCount,
    approvedJoinApplicationCount,
    rawApprovedJoinApplicationCount,
    rejectedJoinApplicationCount,
    registerReviewTotal,
    pendingRegisterReviewCount,
    approvedRegisterReviewCount,
    rejectedRegisterReviewCount,
    workflowTotal: joinApplicationTotal + registerReviewTotal,
    workflowPendingCount: pendingJoinApplicationCount + pendingRegisterReviewCount,
    workflowApprovedCount: approvedJoinApplicationCount + approvedRegisterReviewCount,
    workflowRejectedCount: rejectedJoinApplicationCount + rejectedRegisterReviewCount,
    alumniCount: graduatedAlumniCount,
    alumniRosterCount,
    activeAlumniCount: toNumber(alumniOverview?.currentCount),
    graduatedAlumniCount
  }
}

export function buildPeopleDomainDiagnostics(summary, options = {}) {
  const isSuperAdmin = Boolean(options.isSuperAdmin)
  const accountMemberDelta = summary.nonMemberAccountCount
  const approvedMemberDelta = summary.approvedJoinApplicationCount - summary.memberCount

  let approvedMemberHelper = `${buildPeopleDomainSourceHint('join_requests', isSuperAdmin)} 与 ${buildPeopleDomainSourceHint('club_members', isSuperAdmin)} 当前一致。`
  if (approvedMemberDelta > 0) {
    approvedMemberHelper = `${buildPeopleDomainSourceHint('join_requests', isSuperAdmin)} 的历史已通过累计值高于当前正式成员台账；通常表示存在毕业、退社、历史导入或资料清理后的留痕。`
  } else if (approvedMemberDelta < 0) {
    approvedMemberHelper = `${buildPeopleDomainSourceHint('club_members', isSuperAdmin)} 高于历史已通过入社申请累计值；通常表示存在后台导入、手工建档或统一账号同步生成的成员记录。`
  }

  const accountBreakdown = [
    `管理员账号 ${summary.adminAccountCount}`,
    `指导老师账号 ${summary.teacherAccountCount}`,
    `已绑定学号的学生账号 ${summary.studentAccountWithStudentIdCount}`,
    `无学号的学生账号 ${summary.studentAccountWithoutStudentIdCount}`,
    `有学号但未进入成员台账的学生账号 ${summary.studentAccountMissingMemberLedgerCount}`
  ].join(' / ')
  const standaloneStudentSamplesHint = summary.standaloneStudentAccountSamples?.length
    ? `；样本账号：${summary.standaloneStudentAccountSamples.join('、')}`
    : ''

  return [
    {
      key: 'accountMemberDelta',
      label: '账号-成员差额',
      value: accountMemberDelta,
      helper: `${buildPeopleDomainSourceHint('rk_user', isSuperAdmin)} 减去 ${buildPeopleDomainSourceHint('club_members', isSuperAdmin)}；当前拆分为 ${accountBreakdown}${standaloneStudentSamplesHint}`
    },
    {
      key: 'approvedMemberDelta',
      label: '已通过入社-成员差额',
      value: Math.abs(approvedMemberDelta),
      helper: approvedMemberHelper
    },
    {
      key: 'registerReviewBacklog',
      label: '注册审核积压',
      value: summary.pendingRegisterReviewCount,
      helper: buildPeopleDomainSourceHint('register_review_request', isSuperAdmin, '待处理注册审核记录')
    },
    {
      key: 'graduatedAlumni',
      label: '毕业校友数',
      value: summary.graduatedAlumniCount,
      helper: buildPeopleDomainSourceHint('club_alumni', isSuperAdmin, '只统计毕业且显示中的校友档案')
    }
  ]
}

export function buildStatisticsBoardCards(summary, counts = {}, options = {}) {
  const isSuperAdmin = Boolean(options.isSuperAdmin)
  return [
    {
      key: 'accounts',
      label: '账号数',
      value: summary.accountCount,
      hint: buildPeopleDomainSourceHint('rk_user', isSuperAdmin, '本地账号')
    },
    {
      key: 'members',
      label: '成员数',
      value: summary.memberCount,
      hint: buildPeopleDomainSourceHint('club_members', isSuperAdmin, '正式学生成员台账')
    },
    {
      key: 'pendingJoinApplications',
      label: '待审核入社',
      value: summary.pendingJoinApplicationCount,
      hint: buildPeopleDomainSourceHint('join_requests', isSuperAdmin)
    },
    {
      key: 'pendingRegisterReviews',
      label: '待审核注册',
      value: summary.pendingRegisterReviewCount,
      hint: buildPeopleDomainSourceHint('register_review_request', isSuperAdmin)
    },
    {
      key: 'approvedJoinApplications',
      label: '已通过入社',
      value: summary.approvedJoinApplicationCount,
      hint: buildPeopleDomainSourceHint('join_requests', isSuperAdmin)
    },
    {
      key: 'alumni',
      label: '毕业校友数',
      value: summary.alumniCount,
      hint: buildPeopleDomainSourceHint('club_alumni', isSuperAdmin, '只统计毕业且显示中的校友档案')
    },
    {
      key: 'activities',
      label: '活动数',
      value: toNumber(counts.activities),
      hint: '当前可见活动'
    },
    {
      key: 'news',
      label: '新闻数',
      value: toNumber(counts.news),
      hint: '当前租户新闻'
    },
    {
      key: 'works',
      label: '作品数',
      value: toNumber(counts.works),
      hint: '当前可见作品'
    },
    {
      key: 'competitions',
      label: '比赛数',
      value: toNumber(counts.competitions),
      hint: '当前可见比赛'
    },
    {
      key: 'notices',
      label: '公告数',
      value: toNumber(counts.notices),
      hint: '已发布公告'
    }
  ]
}

export function buildDashboardStats(summary, counts = {}, options = {}) {
  const isSuperAdmin = Boolean(options.isSuperAdmin)
  return [
    {
      key: 'accounts',
      label: '账号数',
      value: summary.accountCount,
      icon: 'User',
      color: '#1890ff',
      helper: buildPeopleDomainSourceHint('rk_user', isSuperAdmin)
    },
    {
      key: 'members',
      label: '成员数',
      value: summary.memberCount,
      icon: 'UserFilled',
      color: '#52c41a',
      helper: buildPeopleDomainSourceHint('club_members', isSuperAdmin, '正式学生成员台账')
    },
    {
      key: 'pendingJoinApplications',
      label: '待审核入社',
      value: summary.pendingJoinApplicationCount,
      icon: 'Bell',
      color: '#fa8c16',
      helper: buildPeopleDomainSourceHint('join_requests', isSuperAdmin)
    },
    {
      key: 'pendingRegisterReviews',
      label: '待审核注册',
      value: summary.pendingRegisterReviewCount,
      icon: 'Notification',
      color: '#7c4dff',
      helper: buildPeopleDomainSourceHint('register_review_request', isSuperAdmin)
    },
    {
      key: 'alumni',
      label: '毕业校友数',
      value: summary.alumniCount,
      icon: 'CollectionTag',
      color: '#722ed1',
      helper: buildPeopleDomainSourceHint('club_alumni', isSuperAdmin, '只统计毕业且显示中的校友档案')
    },
    {
      key: 'activities',
      label: '活动数',
      value: toNumber(counts.activities),
      icon: 'Calendar',
      color: '#13c2c2'
    }
  ]
}
