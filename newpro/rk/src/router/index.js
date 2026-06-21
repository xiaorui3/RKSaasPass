import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  ADMIN_BACKEND_ROLE_IDS,
  ADMIN_CONTENT_ROLE_IDS,
  SUPER_ADMIN_ROLE_IDS,
  SYSTEM_SCOPED_ADMIN_ROLE_IDS,
  TENANT_ADMIN_ROLE_IDS,
  TEACHER_ROLE_IDS,
  canAccessAdminRoute
} from '@/utils/adminAccess'

const frontendRoutes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { guest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { guest: true }
  },
  {
    path: '/alumni/profile-form',
    name: 'AlumniProfileForm',
    component: () => import('@/views/AlumniProfileForm.vue')
  },
  {
    path: '/review/admission',
    name: 'AdmissionReviewAction',
    component: () => import('@/views/AdmissionReviewAction.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      { path: 'content', name: 'PortalContentHub', component: () => import('@/views/PortalContentHub.vue'), meta: { title: '内容聚合' } },
      { path: 'events', name: 'PortalEventsHub', component: () => import('@/views/PortalEventsHub.vue'), meta: { title: '活动赛事' } },
      { path: 'community', name: 'PortalCommunityHub', component: () => import('@/views/PortalCommunityHub.vue'), meta: { title: '社团与成员' } },
      { path: '', name: 'Home', component: () => import('@/views/Home.vue'), meta: { title: '首页' } },
      { path: 'news', name: 'News', component: () => import('@/views/News.vue'), meta: { title: '新闻动态' } },
      { path: 'news/:id', name: 'NewsDetail', component: () => import('@/views/NewsDetail.vue'), meta: { title: '新闻详情' } },
      { path: 'profile', name: 'Profile', component: () => import('@/views/Profile.vue'), meta: { requiresAuth: true, title: '个人中心' } },
      { path: 'notices', name: 'Notices', component: () => import('@/views/Notices.vue'), meta: { title: '公告通知' } },
      { path: 'works', name: 'Works', component: () => import('@/views/WorksFront.vue'), meta: { title: '作品展示' } },
      { path: 'works/:id', name: 'WorkDetail', component: () => import('@/views/WorkDetail.vue') },
      { path: 'about', name: 'About', component: () => import('@/views/About.vue'), meta: { title: '社团概况' } },
      { path: 'join', name: 'Join', component: () => import('@/views/Join.vue'), meta: { title: '加入我们' } },
      { path: 'alumni', name: 'Alumni', component: () => import('@/views/Alumni.vue'), meta: { title: '校友风采' } },
      { path: 'contact', name: 'Contact', component: () => import('@/views/Contact.vue'), meta: { title: '联系我们' } },
      { path: 'achievements', name: 'Achievements', component: () => import('@/views/Achievements.vue'), meta: { title: '成就展示' } },
      { path: 'activities', name: 'Activities', component: () => import('@/views/Activities.vue'), meta: { title: '活动中心' } },
      { path: 'activities/:id', name: 'ActivityDetail', component: () => import('@/views/ActivityDetail.vue') },
      { path: 'search', name: 'SearchResults', component: () => import('@/views/SearchResults.vue') },
      { path: 'history', name: 'History', component: () => import('@/views/History.vue'), meta: { title: '社团历程' } },
      { path: 'calendar', name: 'ActivityCalendar', component: () => import('@/views/ActivityCalendar.vue'), meta: { title: '活动比赛日历' } },
      { path: 'competition', name: 'Competition', component: () => import('@/views/CompetitionFront.vue'), meta: { title: '比赛活动' } },
      {
        path: 'competition/:id',
        name: 'CompetitionDetail',
        component: () => import('@/views/CompetitionDetail.vue')
      },
      {
        path: 'competition/register/:id',
        name: 'CompetitionRegister',
        component: () => import('@/views/CompetitionRegister.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'notifications',
        name: 'Notifications',
        component: () => import('@/views/Notifications.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'credit',
        name: 'CreditReport',
        component: () => import('@/views/CreditReport.vue'),
        meta: { requiresAuth: true, title: '第二课堂成绩单' }
      }
    ]
  }
]

const adminRoutes = [
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true, roles: ADMIN_BACKEND_ROLE_IDS },
    children: [
      {
        path: '',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: '仪表盘', roles: ADMIN_BACKEND_ROLE_IDS }
      },
      {
        path: 'forbidden',
        name: 'AdminForbidden',
        component: () => import('@/views/admin/Forbidden.vue'),
        meta: { title: '无权限访问', requiresAuth: true, skipMenuAuth: true }
      },
      {
        path: 'system/users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/system/Users.vue'),
        meta: { title: '用户管理', roles: SYSTEM_SCOPED_ADMIN_ROLE_IDS }
      },
      {
        path: 'system/roles',
        name: 'AdminRoles',
        component: () => import('@/views/admin/system/Roles.vue'),
        meta: { title: '角色管理', roles: SYSTEM_SCOPED_ADMIN_ROLE_IDS }
      },
      {
        path: 'system/menus',
        name: 'AdminMenus',
        component: () => import('@/views/admin/system/Menus.vue'),
        meta: { title: '菜单管理', roles: SYSTEM_SCOPED_ADMIN_ROLE_IDS }
      },
      {
        path: 'system/tenants',
        name: 'AdminTenants',
        component: () => import('@/views/admin/system/Tenants.vue'),
        meta: { title: '租户管理', roles: SYSTEM_SCOPED_ADMIN_ROLE_IDS }
      },
      {
        path: 'system/config',
        name: 'AdminConfig',
        component: () => import('@/views/admin/system/Config.vue'),
        meta: { title: '系统配置', roles: SYSTEM_SCOPED_ADMIN_ROLE_IDS }
      },
      {
        path: 'system/theme',
        name: 'AdminThemeConfig',
        component: () => import('@/views/admin/system/ThemeConfig.vue'),
        meta: { title: '主题配置', roles: SYSTEM_SCOPED_ADMIN_ROLE_IDS }
      },
      {
        path: 'system/tenant-self-service',
        name: 'AdminTenantSelfServiceConfig',
        component: () => import('@/views/admin/system/TenantSelfServiceConfig.vue'),
        meta: { title: '租户自助配置', roles: SYSTEM_SCOPED_ADMIN_ROLE_IDS }
      },
      {
        path: 'system/permission-matrix',
        name: 'AdminPermissionMatrix',
        component: () => import('@/views/admin/system/PermissionMatrix.vue'),
        meta: { title: '权限矩阵', roles: SYSTEM_SCOPED_ADMIN_ROLE_IDS }
      },
      {
        path: 'system/open-source-health',
        name: 'AdminOpenSourceHealth',
        component: () => import('@/views/admin/system/OpenSourceHealth.vue'),
        meta: { title: '系统自检', roles: SYSTEM_SCOPED_ADMIN_ROLE_IDS }
      },
      {
        path: 'system/api-catalog',
        name: 'AdminApiCatalog',
        component: () => import('@/views/admin/system/ApiCatalog.vue'),
        meta: { title: '接口目录', roles: SYSTEM_SCOPED_ADMIN_ROLE_IDS }
      },
      {
        path: 'system/demo-data',
        name: 'AdminDemoDataCenter',
        component: () => import('@/views/admin/system/DemoDataCenter.vue'),
        meta: { title: '演示数据中心', roles: SUPER_ADMIN_ROLE_IDS }
      },
      {
        path: 'club/members',
        name: 'AdminMembers',
        component: () => import('@/views/admin/club/Members.vue'),
        meta: { title: '成员管理', roles: ADMIN_BACKEND_ROLE_IDS }
      },
      {
        path: 'club/applications',
        name: 'AdminApplications',
        component: () => import('@/views/admin/club/Applications.vue'),
        meta: { title: '入社申请', roles: ADMIN_CONTENT_ROLE_IDS }
      },
      {
        path: 'club/admission-form',
        name: 'AdminAdmissionFormConfig',
        component: () => import('@/views/admin/club/AdmissionFormConfig.vue'),
        meta: { title: '入社表单配置', roles: ADMIN_CONTENT_ROLE_IDS }
      },
      {
        path: 'club/profile-config',
        name: 'AdminClubProfileConfig',
        component: () => import('@/views/admin/club/ClubProfileConfig.vue'),
        meta: { title: '社团概况配置', roles: ADMIN_CONTENT_ROLE_IDS }
      },
      {
        path: 'club/history',
        name: 'AdminClubHistory',
        component: () => import('@/views/admin/club/HistoryManage.vue'),
        meta: { title: '社团历程管理', roles: ADMIN_CONTENT_ROLE_IDS }
      },
      {
        path: 'club/alumni',
        name: 'AdminAlumni',
        component: () => import('@/views/admin/club/Alumni.vue'),
        meta: { title: '校友管理', roles: ADMIN_CONTENT_ROLE_IDS }
      },
      {
        path: 'club/referral-codes',
        name: 'AdminReferralCodes',
        component: () => import('@/views/admin/club/ReferralCodes.vue'),
        meta: { title: '内推码管理', roles: ADMIN_CONTENT_ROLE_IDS }
      },
      {
        path: 'club/email-center',
        name: 'AdminEmailCenter',
        component: () => import('@/views/admin/club/EmailCenter.vue'),
        meta: { title: '邮件发送中心', roles: ADMIN_BACKEND_ROLE_IDS }
      },
      {
        path: 'club/contact-messages',
        name: 'AdminContactMessages',
        component: () => import('@/views/admin/club/ContactMessages.vue'),
        meta: { title: '留言管理', roles: ADMIN_CONTENT_ROLE_IDS }
      },
      {
        path: 'activity/list',
        name: 'AdminActivityList',
        component: () => import('@/views/admin/activity/ActivityList.vue'),
        meta: { title: '活动列表', roles: ADMIN_CONTENT_ROLE_IDS }
      },
      {
        path: 'activity/approval',
        name: 'AdminActivityApproval',
        component: () => import('@/views/admin/activity/ActivityApproval.vue'),
        meta: { title: '活动审批', roles: ADMIN_BACKEND_ROLE_IDS }
      },
      {
        path: 'activity/competition',
        name: 'AdminCompetition',
        component: () => import('@/views/admin/activity/Competition.vue'),
        meta: { title: '比赛管理', roles: ADMIN_CONTENT_ROLE_IDS }
      },
      {
        path: 'activity/competition-approval',
        name: 'AdminCompetitionApproval',
        component: () => import('@/views/admin/activity/CompetitionApproval.vue'),
        meta: { title: '比赛审批', roles: ADMIN_BACKEND_ROLE_IDS }
      },
      {
        path: 'content/news',
        name: 'AdminNews',
        component: () => import('@/views/admin/content/News.vue'),
        meta: { title: '新闻管理', roles: ADMIN_CONTENT_ROLE_IDS }
      },
      {
        path: 'content/news-approval',
        name: 'AdminNewsApproval',
        component: () => import('@/views/admin/content/NewsApproval.vue'),
        meta: { title: '新闻审核', roles: [...TENANT_ADMIN_ROLE_IDS, ...TEACHER_ROLE_IDS] }
      },
      {
        path: 'content/works',
        name: 'AdminWorks',
        component: () => import('@/views/admin/content/Works.vue'),
        meta: { title: '作品管理', roles: ADMIN_CONTENT_ROLE_IDS }
      },
      {
        path: 'content/notices',
        name: 'AdminNotices',
        component: () => import('@/views/admin/content/Notices.vue'),
        meta: { title: '公告管理', roles: ADMIN_BACKEND_ROLE_IDS }
      },
      {
        path: 'content/achievements',
        name: 'AdminAchievements',
        component: () => import('@/views/admin/content/Achievements.vue'),
        meta: { title: '成就管理', roles: ADMIN_CONTENT_ROLE_IDS }
      },
      {
        path: 'statistics/board',
        name: 'AdminStatistics',
        component: () => import('@/views/admin/statistics/Board.vue'),
        meta: { title: '数据看板', roles: ADMIN_BACKEND_ROLE_IDS }
      },
      {
        path: 'statistics/tenant-operations',
        name: 'AdminTenantOperationsCenter',
        component: () => import('@/views/admin/statistics/TenantOperationsCenter.vue'),
        meta: { title: '租户运营中心', roles: ADMIN_BACKEND_ROLE_IDS }
      },
      {
        path: 'statistics/approval-center',
        name: 'AdminApprovalTaskCenter',
        component: () => import('@/views/admin/statistics/ApprovalTaskCenter.vue'),
        meta: { title: '统一审批任务中心', roles: ADMIN_BACKEND_ROLE_IDS }
      },
      {
        path: 'statistics/data-quality',
        name: 'AdminDataQualityCenter',
        component: () => import('@/views/admin/statistics/DataQualityCenter.vue'),
        meta: { title: '数据质量与对账中心', roles: ADMIN_BACKEND_ROLE_IDS }
      },
      {
        path: 'statistics/security-audit',
        name: 'AdminSecurityAuditCenter',
        component: () => import('@/views/admin/statistics/SecurityAuditCenter.vue'),
        meta: { title: '安全审计中心', roles: ADMIN_BACKEND_ROLE_IDS }
      },
      {
        path: 'statistics/participation',
        name: 'AdminParticipationAnalysisCenter',
        component: () => import('@/views/admin/statistics/ParticipationAnalysisCenter.vue'),
        meta: { title: '参与度分析中心', roles: ADMIN_BACKEND_ROLE_IDS }
      },
      {
        path: 'statistics/growth-retention',
        name: 'AdminGrowthRetentionCenter',
        component: () => import('@/views/admin/statistics/GrowthRetentionCenter.vue'),
        meta: { title: '租户增长与留存看板', roles: ADMIN_BACKEND_ROLE_IDS }
      },
      {
        path: 'statistics/content-calendar',
        name: 'AdminContentPublishCalendar',
        component: () => import('@/views/admin/statistics/ContentPublishCalendar.vue'),
        meta: { title: '内容发布日历', roles: ADMIN_BACKEND_ROLE_IDS }
      },
      {
        path: 'operation/logs',
        name: 'AdminLogs',
        component: () => import('@/views/admin/operation/Logs.vue'),
        meta: { title: '日志管理', roles: SUPER_ADMIN_ROLE_IDS }
      },
      {
        path: 'operation/monitoring',
        name: 'AdminMonitoring',
        component: () => import('@/views/admin/operation/Monitoring.vue'),
        meta: { title: '监控中心', roles: SUPER_ADMIN_ROLE_IDS }
      },
      {
        path: 'operation/tasks',
        name: 'AdminTasks',
        component: () => import('@/views/admin/operation/Tasks.vue'),
        meta: { title: '定时任务', roles: SUPER_ADMIN_ROLE_IDS }
      },
      {
        path: 'operation/jenkins',
        name: 'AdminJenkinsConsole',
        component: () => import('@/views/admin/operation/Jenkins.vue'),
        meta: { title: 'Jenkins 控制台', roles: SUPER_ADMIN_ROLE_IDS }
      },
      {
        path: 'operation/release-center',
        name: 'AdminReleaseCenter',
        component: () => import('@/views/admin/operation/ReleaseCenter.vue'),
        meta: { title: '发版中心', roles: SUPER_ADMIN_ROLE_IDS }
      },
      {
        path: 'operation/backup',
        name: 'AdminBackup',
        component: () => import('@/views/admin/operation/Backup.vue'),
        meta: { title: '数据备份', roles: SUPER_ADMIN_ROLE_IDS }
      },
      {
        path: 'operation/k8s',
        name: 'AdminK8s',
        component: () => import('@/views/admin/operation/K8s.vue'),
        meta: { title: 'K8s 管理', roles: SUPER_ADMIN_ROLE_IDS }
      },
      {
        path: 'operation/nacos',
        name: 'AdminNacosConfig',
        component: () => import('@/views/admin/operation/NacosConfig.vue'),
        meta: { title: 'Nacos 配置中心', roles: SUPER_ADMIN_ROLE_IDS }
      },
      {
        path: 'operation/topology',
        name: 'AdminTopology',
        component: () => import('@/views/admin/operation/Topology.vue'),
        meta: { title: '微服务拓扑', roles: SUPER_ADMIN_ROLE_IDS }
      },
      {
        path: 'operation/traffic-center',
        name: 'AdminTrafficCenter',
        component: () => import('@/views/admin/operation/TrafficCenter.vue'),
        meta: { title: '全球流量中心', roles: SUPER_ADMIN_ROLE_IDS }
      },
      {
        path: 'operation/notification-center',
        name: 'AdminNotificationCenter',
        component: () => import('@/views/admin/operation/NotificationCenter.vue'),
        meta: { title: '通知升级管理', roles: SUPER_ADMIN_ROLE_IDS }
      },
      {
        path: 'operation/minio-browser',
        name: 'AdminMinioBrowser',
        component: () => import('@/views/admin/operation/MinioBrowser.vue'),
        meta: { title: 'MinIO 存储浏览器', roles: SUPER_ADMIN_ROLE_IDS }
      },
      {
        path: 'operation/api-workbench',
        name: 'AdminApiWorkbench',
        component: () => import('@/views/admin/operation/ApiWorkbench.vue'),
        meta: { title: 'API 调试台', roles: SUPER_ADMIN_ROLE_IDS }
      },
      {
        path: 'operation/rainbond',
        name: 'AdminRainbondConsole',
        component: () => import('@/views/admin/operation/RainbondConsole.vue'),
        meta: { title: 'Rainbond 控制台', roles: SUPER_ADMIN_ROLE_IDS }
      },
      {
        path: 'operation/deploy-package',
        name: 'AdminDeployPackage',
        component: () => import('@/views/admin/operation/DeployPackage.vue'),
        meta: { title: '一键打包部署', roles: SUPER_ADMIN_ROLE_IDS }
      },
      {
        path: 'operation/remote-migration',
        name: 'AdminRemoteMigration',
        component: () => import('@/views/admin/operation/RemoteMigration.vue'),
        meta: { title: '远程迁移部署', roles: SUPER_ADMIN_ROLE_IDS }
      },
      {
        path: 'operation/visual-screen',
        name: 'AdminVisualScreen',
        component: () => import('@/views/admin/operation/VisualScreen.vue'),
        meta: { title: '运维可视化大屏', roles: SUPER_ADMIN_ROLE_IDS }
      },
      {
        path: 'operation/service-monitor',
        name: 'AdminServiceMonitor',
        component: () => import('@/views/admin/operation/ServiceMonitor.vue'),
        meta: { title: '微服务监控', roles: SUPER_ADMIN_ROLE_IDS }
      },
      {
        path: 'operation/data-export',
        name: 'AdminDataExport',
        component: () => import('@/views/admin/operation/DataExport.vue'),
        meta: { title: '数据导出', roles: ADMIN_CONTENT_ROLE_IDS }
      },
      {
        path: 'operation/redis-cache',
        name: 'AdminRedisCache',
        component: () => import('@/views/admin/operation/RedisCache.vue'),
        meta: { title: 'Redis 缓存管理', roles: SUPER_ADMIN_ROLE_IDS }
      },
      {
        path: 'content/wiki',
        name: 'AdminWiki',
        component: () => import('@/views/admin/content/Wiki.vue'),
        meta: { title: '文档中心', roles: ADMIN_CONTENT_ROLE_IDS }
      },
      {
        path: 'activity/photo-gallery',
        name: 'AdminPhotoGallery',
        component: () => import('@/views/admin/activity/PhotoGallery.vue'),
        meta: { title: '活动相册', roles: ADMIN_CONTENT_ROLE_IDS }
      },
      {
        path: 'club/member-graph',
        name: 'AdminMemberGraph',
        component: () => import('@/views/admin/club/MemberGraph.vue'),
        meta: { title: '成员关系图', roles: ADMIN_BACKEND_ROLE_IDS }
      },
      {
        path: 'activity/vote',
        name: 'AdminActivityVote',
        component: () => import('@/views/admin/activity/Vote.vue'),
        meta: { title: '活动投票', roles: ADMIN_CONTENT_ROLE_IDS }
      },
      {
        path: 'club/resources',
        name: 'AdminResourceBooking',
        component: () => import('@/views/admin/club/ResourceBooking.vue'),
        meta: { title: '资源预约', roles: ADMIN_CONTENT_ROLE_IDS }
      },
      {
        path: 'content/data-diff',
        name: 'AdminDataDiff',
        component: () => import('@/views/admin/content/DataDiff.vue'),
        meta: { title: '数据对比', roles: ADMIN_BACKEND_ROLE_IDS }
      },
      {
        path: 'activity/credit',
        name: 'AdminCreditConfig',
        component: () => import('@/views/admin/activity/CreditConfig.vue'),
        meta: { title: '学分管理', roles: ADMIN_BACKEND_ROLE_IDS }
      },
      {
        path: 'activity/volunteer',
        name: 'AdminVolunteerVerify',
        component: () => import('@/views/admin/activity/VolunteerVerify.vue'),
        meta: { title: '志愿服务审核', roles: ADMIN_BACKEND_ROLE_IDS }
      },
      {
        path: 'finance',
        name: 'AdminFinance',
        component: () => import('@/views/admin/finance/Dashboard.vue'),
        meta: { title: '财务管理', roles: ADMIN_BACKEND_ROLE_IDS }
      },
      {
        path: 'finance/budget',
        name: 'AdminFinanceBudget',
        component: () => import('@/views/admin/finance/Budget.vue'),
        meta: { title: '预算管理', roles: ADMIN_BACKEND_ROLE_IDS }
      },
      {
        path: 'finance/allocation',
        name: 'AdminFinanceAllocation',
        component: () => import('@/views/admin/finance/Allocation.vue'),
        meta: { title: '经费拨款', roles: ADMIN_BACKEND_ROLE_IDS }
      },
      {
        path: 'finance/reimbursement',
        name: 'AdminFinanceReimbursement',
        component: () => import('@/views/admin/finance/Reimbursement.vue'),
        meta: { title: '报销审批', roles: ADMIN_BACKEND_ROLE_IDS }
      },
      {
        path: 'finance/voucher',
        name: 'AdminFinanceVoucher',
        component: () => import('@/views/admin/finance/Voucher.vue'),
        meta: { title: '凭证中心', roles: ADMIN_BACKEND_ROLE_IDS }
      },
      {
        path: 'finance/ledger',
        name: 'AdminFinanceLedger',
        component: () => import('@/views/admin/finance/Ledger.vue'),
        meta: { title: '总账账簿', roles: ADMIN_BACKEND_ROLE_IDS }
      },
      {
        path: 'finance/report',
        name: 'AdminFinanceReport',
        component: () => import('@/views/admin/finance/Report.vue'),
        meta: { title: '报表中心', roles: ADMIN_BACKEND_ROLE_IDS }
      },
      {
        path: 'finance/audit',
        name: 'AdminFinanceAudit',
        component: () => import('@/views/admin/finance/Audit.vue'),
        meta: { title: '审计合规', roles: ADMIN_BACKEND_ROLE_IDS }
      },
      {
        path: 'club/finance',
        redirect: '/admin/finance'
      },
      {
        path: ':pathMatch(.*)*',
        name: 'AdminNotFound',
        component: () => import('@/views/admin/NotFound.vue'),
        meta: { title: '页面未接入', requiresAuth: true, skipMenuAuth: true }
      }
    ]
  }
]

const routes = [...frontendRoutes, ...adminRoutes]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    return { top: 0 }
  }
})

const normalizeRedirectTarget = (target) => {
  const value = Array.isArray(target) ? target[0] : target
  if (typeof value !== 'string') {
    return ''
  }
  const redirect = value.trim()
  if (!redirect || !redirect.startsWith('/') || redirect.startsWith('//')) {
    return ''
  }
  return redirect
}

const resolveAuthenticatedLanding = (to, userStore) => {
  const redirect = normalizeRedirectTarget(to.query?.redirect)
  if (redirect) {
    return redirect
  }
  return userStore.canEnterAdminConsole() ? '/admin' : '/'
}

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  const isLoggedIn = userStore.isLoggedIn

  if (to.meta.requiresAuth && !isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  if (to.meta.requiresAdmin) {
    const userRoleId = userStore.roleId
    if (isLoggedIn && !userStore.adminMenuPaths.length) {
      await userStore.fetchAdminMenus()
    }
    if (to.meta.skipMenuAuth) {
      next()
      return
    }
    let requiredRoles = to.meta.roles
    if (!requiredRoles) {
      for (const record of to.matched) {
        if (record.meta?.roles) {
          requiredRoles = record.meta.roles
          break
        }
      }
    }

    if (!requiredRoles) {
      requiredRoles = SYSTEM_SCOPED_ADMIN_ROLE_IDS
    }

    const canAccessByDynamicMenu = canAccessAdminRoute(
      to.path,
      userRoleId,
      userStore.adminMenuPaths,
      requiredRoles
    )

    if (!canAccessByDynamicMenu) {
      ElMessage.warning('您没有权限访问此页面')
      next({ name: 'AdminForbidden', query: { from: to.fullPath } })
      return
    }
  }

  if (to.meta.guest && isLoggedIn) {
    next(resolveAuthenticatedLanding(to, userStore))
    return
  }

  next()
})

export default router
