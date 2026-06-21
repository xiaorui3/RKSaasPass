import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(__dirname, '../../..')

async function readSource(relativePath) {
  return readFile(path.resolve(repoRoot, relativePath), 'utf8')
}

test('tenant self service config has menu route and frontend API', async () => {
  const router = await readSource('newpro/rk/src/router/index.js')
  const api = await readSource('newpro/rk/src/api/tenant-self-service.js')
  const menuSeed = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/config/AuthMenuCatalogUpdater.java')

  assert.match(router, /path:\s*'system\/tenant-self-service'/)
  assert.match(router, /name:\s*'AdminTenantSelfServiceConfig'/)
  assert.match(router, /TenantSelfServiceConfig\.vue/)

  assert.match(api, /function\s+getTenantSelfServiceConfig/)
  assert.match(api, /url:\s*['"`]\/api\/config\/tenant-self-service\/current['"`]/)
  assert.match(api, /function\s+saveTenantSelfServiceConfig/)
  assert.match(api, /method:\s*['"`]put['"`]/)

  assert.match(menuSeed, /admin_tenant_self_service_config/)
  assert.match(menuSeed, /\/admin\/system\/tenant-self-service/)
  assert.match(menuSeed, /租户自助配置/)
})

test('tenant self service config page exposes grouped editable settings', async () => {
  const page = await readSource('newpro/rk/src/views/admin/system/TenantSelfServiceConfig.vue')

  for (const marker of [
    'getTenantSelfServiceConfig',
    'saveTenantSelfServiceConfig',
    'tenantSelfServiceConfig',
    'brandSettings',
    'portalSettings',
    'admissionSettings',
    'reviewSettings',
    'notificationSettings',
    'quotaSettings',
    '租户自助配置',
    '品牌展示',
    '门户开关',
    '入社申请',
    '审核流程',
    '通知策略',
    '容量限制',
    '保存配置'
  ]) {
    assert.match(page, new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }
})

test('rk-user exposes tenant scoped self service config stored in system_config', async () => {
  const controller = await readSource('rk-user/src/main/java/com/tianji/user/controller/TenantSelfServiceConfigController.java')
  const dto = await readSource('rk-user/src/main/java/com/tianji/user/domain/dto/TenantSelfServiceConfigDTO.java')
  const testSource = await readSource('rk-user/src/test/java/com/tianji/user/controller/TenantSelfServiceConfigControllerTest.java')

  assert.match(controller, /@RequestMapping\("\/api\/config\/tenant-self-service"\)/)
  assert.match(controller, /@GetMapping\("\/current"\)/)
  assert.match(controller, /@PutMapping\("\/current"\)/)
  assert.match(controller, /TenantContext/)
  assert.match(controller, /SystemConfigMapper/)
  assert.match(controller, /tenant\.self-service\.config/)
  assert.match(controller, /hasAnyAuthority\('system:config:view',\s*'system:config:query',\s*'system:config:list'\)/)
  assert.match(controller, /hasAuthority\('system:config:edit'\)/)

  for (const marker of [
    'BrandSettings',
    'PortalSettings',
    'AdmissionSettings',
    'ReviewSettings',
    'NotificationSettings',
    'QuotaSettings',
    'allowPublicRegister',
    'requireTeacherReview',
    'emailNoticeEnabled',
    'maxClubMembers'
  ]) {
    assert.match(dto + testSource, new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }
})

test('tenant self service config is exposed for public frontend and internal admission policy', async () => {
  const api = await readSource('newpro/rk/src/api/tenant-self-service.js')
  const controller = await readSource('rk-user/src/main/java/com/tianji/user/controller/TenantSelfServiceConfigController.java')
  const resourceAuth = await readSource('rk-user/src/main/java/com/tianji/user/config/ResourceAuthCustomizationConfig.java')
  const gatewayAuth = await readSource('rk-gateway/src/main/java/com/tianji/gateway/config/AuthProperties.java')
  const userClient = await readSource('rk-api/src/main/java/com/tianji/api/client/user/UserClient.java')
  const fallback = await readSource('rk-api/src/main/java/com/tianji/api/client/user/fallback/UserClientFallback.java')

  assert.match(api, /function\s+getPublicTenantSelfServiceConfig/)
  assert.match(api, /url:\s*['"`]\/api\/config\/tenant-self-service\/public['"`]/)

  assert.match(controller, /@GetMapping\("\/public"\)/)
  assert.match(controller, /getPublic\(/)
  assert.match(controller, /@GetMapping\("\/internal\/admission-policy"\)/)
  assert.match(controller, /getInternalAdmissionPolicy/)
  assert.match(controller, /TenantSelfServiceAdmissionPolicyDTO/)

  assert.match(resourceAuth, /\/api\/config\/tenant-self-service\/public/)
  assert.match(resourceAuth, /\/api\/config\/tenant-self-service\/internal\/admission-policy/)
  assert.match(gatewayAuth, /GET:\/api\/config\/tenant-self-service\/public/)

  assert.match(userClient, /TenantSelfServiceAdmissionPolicyDTO/)
  assert.match(userClient, /queryTenantSelfServiceAdmissionPolicy/)
  assert.match(userClient, /\/api\/config\/tenant-self-service\/internal\/admission-policy/)
  assert.match(fallback, /queryTenantSelfServiceAdmissionPolicy/)
})

test('tenant self service policy is consumed by business services for review notification and quota rules', async () => {
  const dto = await readSource('rk-api/src/main/java/com/tianji/api/dto/user/TenantSelfServiceAdmissionPolicyDTO.java')
  const controller = await readSource('rk-user/src/main/java/com/tianji/user/controller/TenantSelfServiceConfigController.java')
  const admissionService = await readSource('rk-user/src/main/java/com/tianji/user/service/impl/AdmissionServiceImpl.java')
  const activityService = await readSource('rk-activity/src/main/java/com/tianji/activity/service/impl/ActivityServiceImpl.java')
  const competitionService = await readSource('rk-activity/src/main/java/com/tianji/activity/service/impl/CompetitionServiceImpl.java')
  const newsService = await readSource('rk-content/src/main/java/com/tianji/content/service/impl/NewsServiceImpl.java')
  const fileService = await readSource('rk-file/src/main/java/com/tianji/file/service/impl/FileServiceImpl.java')
  const fileController = await readSource('rk-file/src/main/java/com/tianji/media/controller/FileController.java')
  const fileMapper = await readSource('rk-file/src/main/java/com/tianji/file/mapper/FileMapper.java')

  for (const marker of [
    'requireTeacherReview',
    'requireClubManagerReview',
    'emailNoticeEnabled',
    'siteNoticeEnabled',
    'approvalNoticeEnabled',
    'maxClubMembers',
    'maxActiveActivities',
    'maxMonthlyNews',
    'maxStorageMb'
  ]) {
    assert.match(dto + controller, new RegExp(marker))
  }

  assert.match(admissionService, /isEmailNoticeEnabled/)
  assert.match(admissionService, /isApprovalNoticeEnabled/)
  assert.match(admissionService, /ensureClubMemberQuota/)

  for (const source of [activityService, competitionService, newsService]) {
    assert.match(source, /UserClient/)
    assert.match(source, /queryTenantSelfServiceAdmissionPolicy/)
    assert.match(source, /applyTenantSelfServiceReviewPolicy/)
  }

  assert.match(activityService, /ensureActiveActivityQuota/)
  assert.match(newsService, /ensureMonthlyNewsQuota/)
  assert.match(fileService, /UserClient/)
  assert.match(fileService, /queryTenantSelfServiceAdmissionPolicy/)
  assert.match(fileService, /ensureStorageQuota/)
  assert.match(fileService, /assertUploadAllowed/)
  assert.match(fileService, /maxStorageMb/)
  assert.match(fileService, /BadRequestException/)
  assert.match(fileController, /assertUploadAllowed\(file\.getSize\(\)\)[\s\S]*fileStorage\.uploadFile/)
  assert.match(fileController, /catch\s*\(CommonException\s+\w+\)/)
  assert.match(fileController, /fileStorage\.deleteFile\(bucketName,\s*objectName\)/)
  assert.match(fileMapper, /sumActiveFileSizeByTenant/)
  assert.match(fileMapper, /SUM\(file_size\)/)
})

test('frontend portal and admission pages consume tenant self service config', async () => {
  const store = await readSource('newpro/rk/src/stores/tenantSelfService.js')
  const layout = await readSource('newpro/rk/src/layouts/MainLayout.vue')
  const contentHub = await readSource('newpro/rk/src/views/PortalContentHub.vue')
  const eventsHub = await readSource('newpro/rk/src/views/PortalEventsHub.vue')
  const communityHub = await readSource('newpro/rk/src/views/PortalCommunityHub.vue')
  const register = await readSource('newpro/rk/src/views/Register.vue')
  const join = await readSource('newpro/rk/src/views/Join.vue')

  assert.match(store, /useTenantSelfServiceStore/)
  assert.match(store, /getPublicTenantSelfServiceConfig/)
  assert.match(store, /portalSettings/)
  assert.match(store, /admissionSettings/)
  assert.match(store, /brandSettings/)

  assert.match(layout, /useTenantSelfServiceStore/)
  assert.match(layout, /allowPublicSearch/)
  assert.match(layout, /showNews/)
  assert.match(layout, /showActivities/)
  assert.match(layout, /showCompetitions/)
  assert.match(layout, /showAlumni/)
  assert.match(layout, /showWorks/)
  assert.match(layout, /tenantBrandSettings\.tenantDisplayName/)
  assert.match(layout, /else if \(tenantAdmissionSettings\.value\.allowJoinApplication !== false\)/)

  for (const source of [contentHub, eventsHub, communityHub]) {
    assert.match(source, /useTenantSelfServiceStore/)
    assert.match(source, /portalSettings/)
  }

  assert.match(register, /useTenantSelfServiceStore/)
  assert.match(register, /allowPublicRegister/)
  assert.match(register, /registrationClosed/)
  assert.match(join, /useTenantSelfServiceStore/)
  assert.match(join, /allowJoinApplication/)
  assert.match(join, /joinApplicationClosed/)
})
