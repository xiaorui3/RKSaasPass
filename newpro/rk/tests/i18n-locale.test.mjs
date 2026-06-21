import test from 'node:test'
import assert from 'node:assert/strict'
import { resolveInitialLanguage } from '../src/i18n/language.js'
import zh from '../src/i18n/zh.js'
import en from '../src/i18n/en.js'

test('i18n falls back to zh when saved language is unsupported', async () => {
  const fakeStorage = {
    getItem(key) {
      return key === 'rk-lang' ? 'de' : null
    }
  }

  assert.equal(resolveInitialLanguage(fakeStorage), 'zh')
})

test('zh locale dictionary should provide readable Chinese labels for shared shells', async () => {
  assert.equal(zh.common.search, '搜索')
  assert.equal(zh.common.loading, '加载中...')
  assert.equal(zh.nav.login, '登录')
  assert.equal(zh.nav.register, '注册')
  assert.equal(zh.admin.dashboard, '仪表盘')
  assert.equal(zh.admin.users, '用户管理')
  assert.equal(zh.auth.username, '用户名')
  assert.equal(zh.auth.password, '密码')
})

test('zh locale dictionary should not inherit English copy on event detail pages', async () => {
  assert.equal(zh.pages.activityDetail.loading, '活动详情加载中...')
  assert.equal(zh.pages.activityDetail.backToList, '返回活动列表')
  assert.equal(zh.pages.activityDetail.heroEyebrow, '活动项目')
  assert.equal(zh.pages.activityDetail.registrationWindow, '报名时间')
  assert.equal(zh.pages.activityDetail.downloadAttachment, '下载附件')
  assert.equal(zh.pages.activityDetail.registerSuccess, '报名成功')
  assert.equal(zh.pages.activityDetail.typeLecture, '讲座')
  assert.equal(zh.pages.activityDetail.typeVolunteer, '志愿服务')

  assert.equal(zh.pages.competitionDetail.loading, '比赛详情加载中...')
  assert.equal(zh.pages.competitionDetail.backToList, '返回比赛列表')
  assert.equal(zh.pages.competitionDetail.heroEyebrow, '比赛项目')
  assert.equal(zh.pages.competitionDetail.registrationWindow, '报名时间')
  assert.equal(zh.pages.competitionDetail.competitionWindow, '比赛时间')
  assert.equal(zh.pages.competitionDetail.registerNow, '立即报名')
  assert.equal(zh.pages.competitionDetail.publishedStatus, '已发布')
  assert.equal(zh.pages.competitionDetail.levelSchool, '校级')
  assert.equal(zh.pages.competitionDetail.typeCoding, '编程竞赛')
})

test('en locale dictionary should remain readable for shared shells', async () => {
  assert.equal(en.common.search, 'Search')
  assert.equal(en.nav.login, 'Login')
  assert.equal(en.admin.dashboard, 'Dashboard')
  assert.equal(en.auth.password, 'Password')
})
