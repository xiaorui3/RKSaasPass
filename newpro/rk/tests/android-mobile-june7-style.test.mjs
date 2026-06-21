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

test('android home should match the June 7 mobile reference shell', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')
  const home = methodBody(source, 'showHome')
  const nav = methodBody(source, 'renderBottomNavigation')

  assert.match(source, /private static final int PRIMARY = Color\.rgb\(30,\s*111,\s*255\)/)
  assert.match(source, /private static final int PAGE_BACKGROUND = Color\.rgb\(244,\s*248,\s*255\)/)
  assert.match(home, /screenScaffold\(tenantName/)
  assert.match(home, /mobileSearchBar\(/)
  assert.match(home, /mobileBannerCard\(/)
  assert.match(home, /mobileQuickGrid\(/)
  assert.match(home, /mobileSectionHeader\("热门活动"/)
  for (const label of ['活动', '比赛', '新闻', '社团', '签到', '我的学分', '排行榜', '更多']) {
    assert.equal(source.includes(`"${label}"`), true, `missing quick entry ${label}`)
  }
  for (const label of ['首页', '社团', '+', '消息', '我的']) {
    assert.equal(nav.includes(`"${label}"`), true, `missing bottom nav label ${label}`)
  }
})

test('android list and detail pages should use the mobile reference vocabulary', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')

  for (const label of ['全部', '进行中', '即将开始', '已结束']) {
    assert.equal(source.includes(`"${label}"`), true, `missing mobile tab ${label}`)
  }
  for (const label of ['收藏', '分享', '立即报名', '报名时间', '活动人数', '活动标签']) {
    assert.equal(source.includes(`"${label}"`), true, `missing detail label ${label}`)
  }
  for (const label of ['我的活动', '我的比赛', '我的社团', '我的关注', '我的学分', '总学分', '学分类别', '发现更多社团']) {
    assert.equal(source.includes(`"${label}"`), true, `missing profile/credit/club label ${label}`)
  }
  assert.match(source, /detailBottomActionBar\(/)
  assert.match(source, /mobileStatusTabs\(/)
  assert.match(source, /showMyCredits\(/)
  assert.match(source, /showMyClubs\(/)
})
