export const SENSITIVE_KEYS = [
  'password',
  'passphrase',
  'privateKey',
  'privateKeyPath',
  'token',
  'initialPassword',
  'rootPassword',
  'appPassword',
  'secretKey',
  'accessKey',
  'dockerConfigJson',
  'adminToken'
]

const secrets = new Set()

export function rememberSecrets(config) {
  visit(config, (key, value) => {
    if (isSensitiveKey(key) && typeof value === 'string' && value && !value.startsWith('<CHANGE_ME')) {
      secrets.add(value)
    }
  })
}

export function redactConfig(value) {
  if (Array.isArray(value)) {
    return value.map(redactConfig)
  }
  if (value && typeof value === 'object') {
    const result = {}
    for (const [key, item] of Object.entries(value)) {
      result[key] = isSensitiveKey(key) && item ? '<REDACTED>' : redactConfig(item)
    }
    return result
  }
  return value
}

export function redactText(text) {
  let output = String(text ?? '')
  for (const secret of secrets) {
    if (!secret || secret.length < 3) continue
    output = output.split(secret).join('<REDACTED>')
  }
  output = output.replace(/(password|token|secret|dockerconfigjson)=([^ \n\r\t]+)/gi, '$1=<REDACTED>')
  return output
}

function isSensitiveKey(key) {
  return SENSITIVE_KEYS.some((sensitive) => String(key).toLowerCase().includes(sensitive.toLowerCase()))
}

function visit(value, callback, key = '') {
  if (Array.isArray(value)) {
    value.forEach((item) => visit(item, callback, key))
    return
  }
  if (value && typeof value === 'object') {
    for (const [childKey, childValue] of Object.entries(value)) {
      callback(childKey, childValue)
      visit(childValue, callback, childKey)
    }
  }
}
