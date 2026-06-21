import test from 'node:test'
import assert from 'node:assert/strict'
import {
  canAccessJenkinsOps,
  resolveOpsConsoleUrl,
  normalizeJenkinsJobs,
  resolveJenkinsJobStatus
} from '../src/utils/adminOpsTaskCenter.js'

test('canAccessJenkinsOps only allows tenant 1', () => {
  assert.equal(canAccessJenkinsOps(1), true)
  assert.equal(canAccessJenkinsOps('1'), true)
  assert.equal(canAccessJenkinsOps(2), false)
  assert.equal(canAccessJenkinsOps(null), false)
})

test('resolveJenkinsJobStatus maps Jenkins colors to UI status', () => {
  assert.equal(resolveJenkinsJobStatus('blue'), 'success')
  assert.equal(resolveJenkinsJobStatus('blue_anime'), 'running')
  assert.equal(resolveJenkinsJobStatus('red'), 'failed')
  assert.equal(resolveJenkinsJobStatus('disabled'), 'disabled')
  assert.equal(resolveJenkinsJobStatus('unknown'), 'unknown')
})

test('normalizeJenkinsJobs shapes rows for the task center', () => {
  const jobs = normalizeJenkinsJobs([
    {
      name: 'rk-user',
      url: 'http://jenkins/job/rk-user/',
      color: 'blue',
      lastBuildNumber: 18,
      lastBuildUrl: 'http://jenkins/job/rk-user/18/',
      lastCompletedBuildNumber: 18
    },
    {
      name: 'rk-gateway',
      url: 'http://jenkins/job/rk-gateway/',
      color: 'red_anime',
      lastBuildNumber: 7,
      lastBuildUrl: 'http://jenkins/job/rk-gateway/7/',
      lastCompletedBuildNumber: 6
    }
  ])

  assert.deepEqual(jobs, [
    {
      name: 'rk-user',
      url: 'http://jenkins/job/rk-user/',
      color: 'blue',
      status: 'success',
      lastBuildNumber: 18,
      lastBuildUrl: 'http://jenkins/job/rk-user/18/',
      lastCompletedBuildNumber: 18
    },
    {
      name: 'rk-gateway',
      url: 'http://jenkins/job/rk-gateway/',
      color: 'red_anime',
      status: 'running',
      lastBuildNumber: 7,
      lastBuildUrl: 'http://jenkins/job/rk-gateway/7/',
      lastCompletedBuildNumber: 6
    }
  ])
})

test('resolveOpsConsoleUrl hides private console urls on public domains', () => {
  assert.equal(
    resolveOpsConsoleUrl('http://127.0.0.1:8880/xxl-job-admin', {
      currentOrigin: 'https://example.com'
    }),
    ''
  )
})

test('resolveOpsConsoleUrl keeps same-host console urls on intranet access', () => {
  assert.equal(
    resolveOpsConsoleUrl('http://127.0.0.1:8880/xxl-job-admin', {
      currentOrigin: 'http://127.0.0.1:5173'
    }),
    'http://127.0.0.1:8880/xxl-job-admin'
  )
})

test('resolveOpsConsoleUrl keeps already public console urls', () => {
  assert.equal(
    resolveOpsConsoleUrl('https://ops.example.com/xxl-job-admin', {
      currentOrigin: 'https://example.com'
    }),
    'https://ops.example.com/xxl-job-admin'
  )
})
