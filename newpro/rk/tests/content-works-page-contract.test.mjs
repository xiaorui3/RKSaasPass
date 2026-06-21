import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs/promises'

const rootUrl = new URL('../../../', import.meta.url)

async function readRepoFile(relativePath) {
  return fs.readFile(new URL(relativePath, rootUrl), 'utf8')
}

test('works page endpoint should apply public filters and safe sorting', async () => {
  const controller = await readRepoFile('rk-content/src/main/java/com/tianji/content/controller/WorksController.java')
  const legacyPaginationTodo = String.fromCodePoint(0x9352, 0x55db, 0x3009, 0x93cc, 0x30e8, 0xe1d7)

  assert.doesNotMatch(controller, /TODO:\s*.*分页查询逻辑/)
  assert.doesNotMatch(controller, new RegExp(`TODO:\\s*.*${legacyPaginationTodo}`))
  assert.match(controller, /LambdaQueryWrapper<Work>/)
  assert.match(controller, /buildWorksPageQuery\(queryDTO\)/)
  assert.match(controller, /eq\(Work::getIsDeleted,\s*0\)/)
  assert.match(controller, /eq\(Work::getManagerReviewStatus,\s*Work\.REVIEW_APPROVED\)/)
  assert.match(controller, /eq\(Work::getTeacherReviewStatus,\s*Work\.REVIEW_APPROVED\)/)

  for (const getter of ['getCategory', 'getAuthors', 'getTechnologies', 'getIsFeatured']) {
    assert.match(controller, new RegExp(`Work::${getter}`), `missing filter for ${getter}`)
  }

  for (const getter of ['getTitle', 'getDescription', 'getContent', 'getAuthors', 'getTechnologies']) {
    assert.match(controller, new RegExp(`like\\(Work::${getter}`), `keyword search should include ${getter}`)
  }

  assert.match(controller, /applyWorksPageSorting/)
  assert.match(controller, /case "viewCount"/)
  assert.match(controller, /case "likeCount"/)
  assert.match(controller, /case "displayOrder"/)
  assert.doesNotMatch(controller, /\.last\(\s*["']ORDER BY/)
  assert.doesNotMatch(controller, /order\s+by\s*"\s*\+\s*queryDTO\.getSortBy/i)
})
