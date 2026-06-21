import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const projectRoot = resolve(import.meta.dirname, '..')
const echartsConsumers = [
  ['src/views/admin/statistics/Board.vue', '@/utils/echarts/board'],
  ['src/views/admin/content/DataDiff.vue', '@/utils/echarts/dataDiff'],
  ['src/views/admin/club/MemberGraph.vue', '@/utils/echarts/memberGraph']
]

test('admin chart views use the modular echarts entry instead of the full package', () => {
  for (const [relativePath, helperPath] of echartsConsumers) {
    const source = readFileSync(resolve(projectRoot, relativePath), 'utf8')
    assert.doesNotMatch(
      source,
      /from\s+['"]echarts['"]/,
      `${relativePath} should not import the full echarts package`
    )
    assert.match(
      source,
      new RegExp(`from\\s+['"]${helperPath.replace('/', '\\/')}['"]`),
      `${relativePath} should import the dedicated modular echarts helper`
    )
  }
})
