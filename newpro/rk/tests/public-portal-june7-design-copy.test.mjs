import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const pages = [
  { file: '../src/views/News.vue', labels: ['新闻中心', '新闻动态'] },
  { file: '../src/views/NewsDetail.vue', labels: ['新闻概况', '全文阅读'] },
  { file: '../src/views/Notices.vue', labels: ['公告概况', '公告通知'] },
  { file: '../src/views/Activities.vue', labels: ['发现精彩活动', '活动目录'] },
  { file: '../src/views/ActivityDetail.vue', labels: ['活动详情', '报名信息'] },
  { file: '../src/views/CompetitionFront.vue', labels: ['发现精彩赛事', '比赛目录'] },
  { file: '../src/views/CompetitionDetail.vue', labels: ['比赛详情', '报名入口'] },
  { file: '../src/views/WorksFront.vue', labels: ['作品展示', '作品目录'] },
  { file: '../src/views/WorkDetail.vue', labels: ['作品中心', '作品说明'] },
  { file: '../src/views/Join.vue', labels: ['申请其他社团', '立即申请'] },
  { file: '../src/views/Alumni.vue', labels: ['校友风采', '历史社员'] },
  { file: '../src/views/Contact.vue', labels: ['联系我们', '发送留言'] },
  { file: '../src/views/History.vue', labels: ['社团历程', '时间轴'] },
  { file: '../src/views/About.vue', labels: ['社团状况', '关于我们'] }
]

const mojibakeMarkers = [
  [0x7ec0, 0x60e7],
  [0x934f, 0xe100],
  [0x93c2, 0x4f34],
  [0x5a32, 0x8bf2],
  [0x59e3, 0x65c7],
  [0x6d63, 0x6ec3, 0x6427],
  [0x93b4, 0x612c],
  [0x9471, 0x65c2],
  [0x6942, 0x6a3b],
  [0x95ab, 0x6c31],
  [0x701b, 0xfe40],
  [0x9422, 0x5ba0],
  [0x93c8, 0xe046],
  [0x93c6, 0x509b]
].map((codes) => String.fromCodePoint(...codes))

async function readSource(file) {
  return readFile(new URL(file, import.meta.url), 'utf8')
}

test('June 7 public portal pages use readable Chinese copy from the design references', async () => {
  for (const page of pages) {
    const source = await readSource(page.file)
    for (const label of page.labels) {
      assert.equal(source.includes(label), true, `${page.file} should include "${label}"`)
    }
    for (const marker of mojibakeMarkers) {
      assert.equal(source.includes(marker), false, `${page.file} still contains mojibake marker "${marker}"`)
    }
  }
})
