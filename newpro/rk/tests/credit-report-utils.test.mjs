import test from 'node:test'
import assert from 'node:assert/strict'

import { normalizeCreditBreakdown, normalizeCreditRecords } from '../src/utils/creditReport.js'

test('normalizeCreditBreakdown should normalize totals and labels', () => {
  const result = normalizeCreditBreakdown([
    { code: 'activity', name: '活动学分', totalCredits: 3, totalHours: 2, recordCount: 1 }
  ])

  assert.equal(result.length, 1)
  assert.equal(result[0].code, 'activity')
  assert.equal(result[0].name, '活动学分')
  assert.equal(result[0].totalCredits, 3)
  assert.equal(result[0].totalHours, 2)
  assert.equal(result[0].recordCount, 1)
})

test('normalizeCreditRecords should map type names from configured types', () => {
  const result = normalizeCreditRecords(
    {
      records: [
        {
          id: 1,
          creditTypeCode: 'volunteer',
          description: '志愿服务《清洁》学分',
          creditHours: 4.5,
          creditScore: 4.5
        }
      ],
      total: 1
    },
    [{ code: 'volunteer', name: '志愿服务' }]
  )

  assert.equal(result.total, 1)
  assert.equal(result.records.length, 1)
  assert.equal(result.records[0].typeName, '志愿服务')
  assert.equal(result.records[0].sourceName, '志愿服务《清洁》学分')
  assert.equal(result.records[0].hours, 4.5)
  assert.equal(result.records[0].credits, 4.5)
})
