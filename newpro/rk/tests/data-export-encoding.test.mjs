import test from 'node:test'
import assert from 'node:assert/strict'
import { repairLegacyMojibake, normalizeExportRow } from '../src/utils/exportEncoding.js'

test('repairLegacyMojibake repairs legacy UTF-8-as-Latin1 strings before export', () => {
  assert.equal(repairLegacyMojibake('åæ°åä¸è®²åº§'), '创新创业讲座')
  assert.equal(repairLegacyMojibake('社团动态'), '社团动态')
  assert.equal(repairLegacyMojibake('RealNewsUpload1778400579145'), 'RealNewsUpload1778400579145')
})

test('normalizeExportRow repairs only string cells and keeps numeric cells unchanged', () => {
  assert.deepEqual(
    normalizeExportRow({
      ID: 110,
      活动名称: '2026å¹´æ¥å­£ç¼ç¨é©¬ææ¾',
      参与人数: 12
    }),
    {
      ID: 110,
      活动名称: '2026年春季编程马拉松',
      参与人数: 12
    }
  )
})
