import test from 'node:test'
import assert from 'node:assert/strict'
import {
  createDefaultAdmissionConfig,
  normalizeAdmissionFormConfig
} from '../src/composables/useAdmissionFormRuntime.js'

function findField(config, key) {
  return (config.fields || []).find((field) => field.key === key)
}

test('register and join share the same builtin admission field catalog', () => {
  const registerConfig = createDefaultAdmissionConfig('register')
  const joinConfig = createDefaultAdmissionConfig('join')

  assert.deepEqual(
    registerConfig.fields.map((field) => field.key),
    joinConfig.fields.map((field) => field.key)
  )
})

test('email stays enabled and required for register and join even when tenant config tries to relax it', () => {
  for (const mode of ['register', 'join']) {
    const config = normalizeAdmissionFormConfig({
      fields: [
        {
          key: 'email',
          source: 'builtin',
          enabled: false,
          required: false,
          label: 'Email'
        }
      ]
    }, mode)

    const emailField = findField(config, 'email')
    assert.ok(emailField, `email field should exist for ${mode}`)
    assert.equal(emailField.enabled, true, `email should stay enabled for ${mode}`)
    assert.equal(emailField.required, true, `email should stay required for ${mode}`)
  }
})
