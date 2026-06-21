import assert from 'node:assert/strict'
import { isSuccessfulResponseCode } from '../src/utils/responseCode.js'

assert.equal(isSuccessfulResponseCode(200), true, 'code 200 should be treated as success')
assert.equal(isSuccessfulResponseCode(0), true, 'legacy code 0 should be treated as success')
assert.equal(isSuccessfulResponseCode(0, 'success'), true, 'legacy code 0 with success state should pass')
assert.equal(isSuccessfulResponseCode(0, 'error'), false, 'legacy code 0 with error state should fail')
assert.equal(isSuccessfulResponseCode('200'), true, 'string code 200 should be treated as success')
assert.equal(isSuccessfulResponseCode('0'), true, 'string code 0 should be treated as success')
assert.equal(isSuccessfulResponseCode(500), false, 'code 500 should be treated as failure')
assert.equal(isSuccessfulResponseCode(undefined), true, 'responses without business code should pass through')

console.log('response-code tests passed')
