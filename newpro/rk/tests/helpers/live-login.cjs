// @ts-check
const { execFile } = require('child_process')
const net = require('net')
const path = require('path')
const { promisify } = require('util')

const DEFAULT_API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'
const REDIS_HOST = process.env.PLAYWRIGHT_REDIS_HOST || '127.0.0.1'
const REDIS_PORT = Number(process.env.PLAYWRIGHT_REDIS_PORT || 6379)
const REDIS_PASSWORD = process.env.PLAYWRIGHT_REDIS_PASSWORD || 'change-me'
const USE_K8S_REDIS = process.env.PLAYWRIGHT_REDIS_VIA_K8S === '1' || /:30001/.test(DEFAULT_API_BASE)
const execFileAsync = promisify(execFile)

function buildRedisCommand(parts) {
  const payload = [`*${parts.length}`]
  for (const part of parts) {
    const value = String(part)
    payload.push(`$${Buffer.byteLength(value)}`)
    payload.push(value)
  }
  return `${payload.join('\r\n')}\r\n`
}

function parseBulkString(response) {
  if (!response || response.startsWith('$-1')) return null
  const segments = response.split('\r\n')
  return segments[1] || null
}

async function redisRaw(parts) {
  return new Promise((resolve, reject) => {
    const client = net.createConnection({ host: REDIS_HOST, port: REDIS_PORT }, () => {
      client.write(buildRedisCommand(['AUTH', REDIS_PASSWORD]))
    })

    let authed = false
    let buffer = ''
    const deadline = setTimeout(() => {
      client.destroy()
      reject(new Error(`Timed out reading Redis response for ${parts[0]}`))
    }, 10000)

    client.on('data', (chunk) => {
      buffer += chunk.toString()
      if (!authed) {
        if (!buffer.includes('\r\n')) return
        if (!buffer.startsWith('+OK')) {
          clearTimeout(deadline)
          client.destroy()
          reject(new Error(`Redis AUTH failed: ${buffer.trim()}`))
          return
        }
        authed = true
        buffer = buffer.replace('+OK\r\n', '')
        client.write(buildRedisCommand(parts))
        return
      }
      if (buffer.includes('\r\n')) {
        clearTimeout(deadline)
        client.end()
        resolve(buffer)
      }
    })

    client.on('error', (error) => {
      clearTimeout(deadline)
      reject(error)
    })
    client.on('close', () => clearTimeout(deadline))
  })
}

async function redisGet(key) {
  if (USE_K8S_REDIS) {
    return redisGetViaK8s(key)
  }
  return parseBulkString(await redisRaw(['GET', key]))
}

function shellQuote(value) {
  return `'${String(value).replace(/'/g, "'\\''")}'`
}

async function redisGetViaK8s(key) {
  const scriptPath = path.resolve(__dirname, '../../../../remote_ssh_master01.py')
  const kubectl = '/var/lib/rancher/rke2/bin/kubectl'
  const remoteCommand = [
    'export KUBECONFIG=/etc/rancher/rke2/rke2.yaml',
    `${kubectl} -n shetuanguanlixitong exec rk-server-rk-redis-0 -- redis-cli GET ${shellQuote(key)}`
  ].join('; ')
  const { stdout } = await execFileAsync(process.env.PYTHON || 'python', ['-X', 'utf8', scriptPath, remoteCommand], {
    timeout: 20000,
    windowsHide: true
  })
  const value = stdout.trim()
  return value && value !== '(nil)' ? value : null
}

async function waitForEmailCode(tenantId, email, scene = 'LOGIN') {
  const normalizedEmail = String(email).trim().toLowerCase()
  const key = `email:verify:code:${scene}:${tenantId}:${normalizedEmail}`
  const deadline = Date.now() + 15000
  while (Date.now() < deadline) {
    const code = await redisGet(key)
    if (code) return code
    await new Promise((resolve) => setTimeout(resolve, 500))
  }
  throw new Error(`Timed out waiting for email code in Redis key ${key}`)
}

async function requestEmailCode(request, apiBase, tenantId, email, scene = 'LOGIN') {
  const sendRes = await request.post(`${apiBase}/api/email-verification/send`, {
    data: {
      email,
      tenantId: Number(tenantId),
      scene
    }
  })
  const sendBody = await sendRes.json()
  if (sendBody.code !== 200) {
    throw new Error(`Email code send failed: ${JSON.stringify(sendBody)}`)
  }
  return waitForEmailCode(tenantId, email, scene)
}

function buildDormantVerificationEmail(user) {
  if (user.email) return user.email
  const username = String(user.username || 'user').replace(/[^a-zA-Z0-9_.-]/g, '_')
  return `${username}.login.${Date.now()}@example.com`
}

async function loginApi(request, user, apiBase = DEFAULT_API_BASE) {
  const firstRes = await request.post(`${apiBase}/auth/login`, { data: user })
  const firstBody = await firstRes.json()
  if (firstBody.code === 200 && firstBody.data?.token) {
    return firstBody.data
  }

  const message = String(firstBody.msg || '')
  if (!message.includes('Dormant login requires email verification')) {
    throw new Error(`Login failed: ${JSON.stringify(firstBody)}`)
  }

  const tenantId = user.organizationId || user.tenantId || '1'
  const email = buildDormantVerificationEmail(user)
  const emailCode = await requestEmailCode(request, apiBase, tenantId, email, 'LOGIN')
  const verifiedRes = await request.post(`${apiBase}/auth/login`, {
    data: {
      ...user,
      email,
      emailCode
    }
  })
  const verifiedBody = await verifiedRes.json()
  if (verifiedBody.code !== 200 || !verifiedBody.data?.token) {
    throw new Error(`Verified login failed: ${JSON.stringify(verifiedBody)}`)
  }
  return verifiedBody.data
}

module.exports = {
  loginApi,
  requestEmailCode,
  waitForEmailCode
}
