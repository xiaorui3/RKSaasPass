import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

async function readSource(relativePath) {
  return readFile(new URL(relativePath, import.meta.url), 'utf8')
}

function assertNoBrokenText(source) {
  assert.doesNotMatch(source, /\?{4,}/)
  assert.doesNotMatch(source, /[\uE000-\uF8FF]/)
}

function textFromCodes(codes) {
  return String.fromCodePoint(...codes)
}

test('login page source should bind shared visible copy through vue-i18n', async () => {
  const source = await readSource('../src/views/Login.vue')

  assert.equal(source.includes('useI18n'), true)
  assert.equal(source.includes("t('auth.accountPasswordLogin')"), true)
  assert.equal(source.includes("t('auth.emailCodeLogin')"), true)
  assert.equal(source.includes("t('auth.inputUsername')"), true)
  assert.equal(source.includes("t('auth.inputPassword')"), true)
})

test('login page should use compact auth form spacing instead of large blank form rows', async () => {
  const source = await readSource('../src/views/Login.vue')

  assert.equal(source.includes('class="auth-form"'), true)
  assert.equal(source.includes('.auth-form :deep(.el-form-item)'), true)
  assert.equal(source.includes('label-position="top"'), true)
})

test('login page should keep verification-code login and make tenant choice subtle', async () => {
  const source = await readSource('../src/views/Login.vue')

  assert.equal(source.includes("name=\"email\""), true)
  assert.equal(source.includes('handleSendCode(\'email\')'), true)
  assert.equal(source.includes('prepareEmailLogin'), true)
  assert.equal(source.includes('sendEmailVerificationCode'), true)
  assert.equal(source.includes('class="tenant-chip-row"'), true)
  assert.equal(source.includes('class="tenant-chip"'), true)
  assert.equal(source.includes('tenant-popover'), true)
  assert.equal(source.includes('.tenant-chip'), true)
  assert.equal(source.includes('v-if="loginMode === \'password\'"'), false)
  assert.equal(source.includes('style="width: 100%"'), false)
})

test('login page should use a modern compact split-panel visual treatment', async () => {
  const source = await readSource('../src/views/Login.vue')

  assert.equal(source.includes('login-shell'), true)
  assert.equal(source.includes('login-visual-panel'), true)
  assert.equal(source.includes('login-auth-panel'), true)
  assert.equal(source.includes('login-feature-grid'), true)
  assert.equal(source.includes('.login-card'), false)
  assert.equal(source.includes('width: min(460px, 100%)'), false)
  assert.equal(source.includes('backdrop-filter: blur(12px)'), false)
})

test('news and works pages should keep clean Chinese portal and admin copy', async () => {
  const [newsSource, worksFrontSource, adminWorksSource] = await Promise.all([
    readSource('../src/views/News.vue'),
    readSource('../src/views/WorksFront.vue'),
    readSource('../src/views/admin/content/Works.vue')
  ])

  assert.match(newsSource, /新闻/)
  assert.match(worksFrontSource, /作品/)
  assert.match(adminWorksSource, /作品/)

  assertNoBrokenText(newsSource)
  assertNoBrokenText(worksFrontSource)
  assertNoBrokenText(adminWorksSource)
})

test('shared layouts and entry pages should not keep old english fallback copy', async () => {
  const [mainLayoutSource, aboutSource, registerSource, loginModalSource] = await Promise.all([
    readSource('../src/layouts/MainLayout.vue'),
    readSource('../src/views/About.vue'),
    readSource('../src/views/Register.vue'),
    readSource('../src/components/LoginModal.vue')
  ])

  assert.equal(mainLayoutSource.includes('Tenant Portal'), false)
  assert.equal(mainLayoutSource.includes('Unified Portal'), false)
  assert.equal(mainLayoutSource.includes('Current tenant'), false)
  assert.equal(mainLayoutSource.includes('All rights reserved.'), false)
  assert.equal(aboutSource.includes('About the club'), false)
  assert.equal(registerSource.includes('Your browser does not support video playback.'), false)
  assert.equal(loginModalSource.includes('RK-Web Login'), false)
  assert.equal(registerSource.includes('请输入用户名'), true)
  assert.equal(registerSource.includes('发送验证码'), true)
  assert.equal(registerSource.includes('已有账号？去登录'), true)
  assert.equal(registerSource.includes('当前浏览器不支持视频播放。'), true)
  assert.equal(registerSource.includes(textFromCodes([0x9359, 0x6226, 0x20ac, 0x4f80, 0x7359, 0x7487, 0x4f7a, 0x721c])), false)
  assert.equal(registerSource.includes(textFromCodes([
    0x8930, 0x64b3, 0x58a0, 0x5a34, 0x5fda, 0xe74d,
    0x9363, 0x3124, 0x7b09, 0x93c0, 0xe21b, 0x5bd4,
    0x7459, 0x55db, 0xe576, 0x93be, 0xe15f, 0x6581
  ])), false)
})

test('tenant management source should retain tenant ordering and logo management fields', async () => {
  const source = await readSource('../src/views/admin/system/Tenants.vue')

  assert.equal(source.includes('displayOrder'), true)
  assert.equal(source.includes('logoUrl'), true)
  assert.equal(source.includes('tenantName'), true)
  assertNoBrokenText(source)
})

test('admin users source should keep people-domain diagnostics helper wired in', async () => {
  const source = await readSource('../src/views/admin/system/Users.vue')
  const apiSource = await readSource('../src/api/user.js')

  assert.equal(source.includes('buildPeopleDomainDiagnostics'), true)
  assert.equal(source.includes('diagnostics'), true)
  assert.equal(source.includes('standaloneStudentSamples'), true)
  assert.equal(source.includes('people-domain-samples'), true)
  assert.equal(source.includes('handleReconcilePeopleDomain'), true)
  assert.equal(source.includes('handleBatchDelete'), true)
  assert.equal(apiSource.includes('reconcilePeopleDomain'), true)
  assertNoBrokenText(source)
})

test('admin people pages should not keep broken text fragments', async () => {
  const [usersSource, memberGraphSource, memberGraphUtilSource] = await Promise.all([
    readSource('../src/views/admin/system/Users.vue'),
    readSource('../src/views/admin/club/MemberGraph.vue'),
    readSource('../src/utils/memberGraph.js')
  ])

  assertNoBrokenText(usersSource)
  assertNoBrokenText(memberGraphSource)
  assertNoBrokenText(memberGraphUtilSource)
  assert.match(memberGraphSource, /成员关系图/)
  assert.match(memberGraphUtilSource, /普通成员/)
})

test('contact pages should use clean contact and moderation copy', async () => {
  const [contactSource, adminContactSource] = await Promise.all([
    readSource('../src/views/Contact.vue'),
    readSource('../src/views/admin/club/ContactMessages.vue')
  ])

  assert.equal(contactSource.includes('当前社团管理员'), true)
  assert.equal(contactSource.includes('重复内容会被拦截'), true)
  assert.equal(adminContactSource.includes('留言管理台'), true)

  assertNoBrokenText(contactSource)
  assertNoBrokenText(adminContactSource)
})

test('about page should keep clean Chinese portal copy', async () => {
  const source = await readSource('../src/views/About.vue')

  assert.equal(source.includes('社团概况'), true)
  assert.equal(source.includes('联系社团'), true)
  assert.equal(source.includes('查看历程'), true)
  assertNoBrokenText(source)
})
