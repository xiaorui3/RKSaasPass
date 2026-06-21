import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildAdmissionQueue,
  normalizeJoinReviewStatus,
  normalizeRegisterReviewStatus
} from '../src/utils/admissionQueue.js'

test('normalizes join and register review statuses into a shared queue status', () => {
  assert.equal(normalizeJoinReviewStatus('待审核'), 0)
  assert.equal(normalizeJoinReviewStatus('通过'), 1)
  assert.equal(normalizeJoinReviewStatus('未通过'), 2)
  assert.equal(normalizeRegisterReviewStatus('PENDING'), 0)
  assert.equal(normalizeRegisterReviewStatus('APPROVED'), 1)
  assert.equal(normalizeRegisterReviewStatus('REJECTED'), 2)
})

test('buildAdmissionQueue merges join applications and register reviews into one sorted queue', () => {
  const queue = buildAdmissionQueue(
    [
      {
        id: 11,
        name: 'Join User',
        studentId: '2024001',
        major: '软件工程',
        phone: '13800000000',
        email: 'join@example.com',
        interest: '摄影',
        applicationTime: '2026-04-27T08:00:00'
      }
    ],
    [
      {
        id: 22,
        username: 'register_user',
        email: 'register@example.com',
        name: 'Register User',
        referralCode: 'RC001',
        reviewStatus: 'PENDING',
        createTime: '2026-04-27T09:00:00',
        formPayloadJson: JSON.stringify({
          studentId: '2024999',
          major: '计算机科学',
          phone: '13900000000'
        })
      }
    ]
  )

  assert.equal(queue.length, 2)
  assert.equal(queue[0].businessType, 'register')
  assert.equal(queue[0].studentId, '2024999')
  assert.equal(queue[0].status, 0)
  assert.equal(queue[1].businessType, 'join')
  assert.equal(queue[1].reason, '摄影')
})
