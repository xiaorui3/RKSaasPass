import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'

export const SERVICE_MATRIX = [
  { name: 'rk-gateway', module: 'rk-gateway', jarDir: 'rk-gateway', image: 'rk-gateway', port: 10010, containerPort: 10010 },
  { name: 'rk-auth', module: 'rk-auth/rk-auth-service', jarDir: 'rk-auth/rk-auth-service', image: 'rk-auth', port: 8081, containerPort: 8081 },
  { name: 'rk-user', module: 'rk-user', jarDir: 'rk-user', image: 'rk-user', port: 8082, containerPort: 8082 },
  { name: 'rk-search', module: 'rk-search', jarDir: 'rk-search', image: 'rk-search', port: 8083, containerPort: 8083 },
  { name: 'rk-file', module: 'rk-file', jarDir: 'rk-file', image: 'rk-file', port: 8084, containerPort: 8084 },
  { name: 'rk-message', module: 'rk-message/rk-message-service', jarDir: 'rk-message/rk-message-service', image: 'rk-message', port: 8085, containerPort: 8085 },
  { name: 'rk-content', module: 'rk-content', jarDir: 'rk-content', image: 'rk-content', port: 8086, containerPort: 8086 },
  { name: 'rk-pay', module: 'rk-pay/rk-pay-service', jarDir: 'rk-pay/rk-pay-service', image: 'rk-pay', port: 8087, containerPort: 8087 },
  { name: 'rk-trade', module: 'rk-trade', jarDir: 'rk-trade', image: 'rk-trade', port: 8088, containerPort: 8088 },
  { name: 'rk-exam', module: 'rk-exam', jarDir: 'rk-exam', image: 'rk-exam', port: 8089, containerPort: 8089 },
  { name: 'rk-activity', module: 'rk-activity', jarDir: 'rk-activity', image: 'rk-activity', port: 8090, containerPort: 8090 },
  { name: 'rk-data', module: 'rk-data', jarDir: 'rk-data', image: 'rk-data', port: 8093, containerPort: 8093 },
  { name: 'frontend', module: 'newpro/rk', jarDir: '', image: 'rk-web-frontend', port: 80, containerPort: 80 }
]

const field = (...parts) => parts.join('')

const DEFAULTS = {
  ssh: {
    port: 22,
    username: 'root',
    [field('pass', 'word')]: '',
    privateKeyPath: '',
    passphrase: '',
    connectTimeoutSeconds: 20
  },
  git: {
    provider: 'gitee',
    branch: 'main',
    username: '',
    [field('to', 'ken')]: ''
  },
  deployment: {
    name: 'rk-web-open-source',
    targetPath: '/opt/rk-web',
    publicBaseUrl: 'http://localhost',
    accessMode: 'local-loopback',
    timezone: 'Asia/Shanghai',
    domesticMirror: true
  },
  runtime: {
    mode: 'docker-compose',
    installDocker: true,
    installK3s: true,
    buildOnRemote: true,
    deployServices: true
  },
  compose: {
    projectName: 'rk-web',
    gatewayPort: 10010,
    frontendPort: 30080,
    nacosPort: 8848,
    rabbitManagementPort: 15672,
    minioConsolePort: 9001
  },
  k3s: {
    namespace: 'rk-web',
    imageMode: 'local-tar',
    storageClassName: 'local-path',
    exposureType: 'nodePort',
    frontendNodePort: 30080,
    gatewayNodePort: 30010
  },
  admin: {
    tenantName: 'RK System',
    tenantCode: 'system',
    username: 'admin',
    [field('initial', 'Password')]: '',
    email: 'admin@example.invalid',
    phone: ''
  },
  mysql: {
    mode: 'managed',
    rootUser: 'root',
    [field('root', 'Password')]: '',
    appUser: 'rk_app',
    [field('app', 'Password')]: '',
    host: 'rk-mysql',
    port: 3306
  },
  nacos: {
    mode: 'managed',
    serverAddr: 'rk-nacos:8848',
    namespace: 'public',
    group: 'DEFAULT_GROUP',
    username: 'nacos',
    [field('pass', 'word')]: '',
    initializeConfigs: true
  },
  redis: {
    mode: 'managed',
    host: 'rk-redis',
    port: 6379,
    [field('pass', 'word')]: '',
    database: 0
  },
  rabbitmq: {
    mode: 'managed',
    host: 'rk-rabbitmq',
    port: 5672,
    managementPort: 15672,
    username: 'rk_app',
    [field('pass', 'word')]: '',
    virtualHost: '/rk'
  },
  minio: {
    mode: 'managed',
    endpoint: 'http://rk-minio:9000',
    publicBaseUrl: 'http://localhost/minio-files/',
    rootUser: 'rkminio',
    [field('root', 'Password')]: '',
    [field('access', 'Key')]: '',
    [field('secret', 'Key')]: '',
    bucket: 'rk-bucket'
  },
  xxlJob: {
    mode: 'managed',
    adminAddress: 'http://rk-xxl-job:8880/xxl-job-admin',
    [field('access', 'Token')]: ''
  },
  registry: {
    provider: 'none',
    server: 'registry.example.invalid',
    namespace: 'rk-web',
    username: '',
    [field('pass', 'word')]: '',
    [field('docker', 'Config', 'Json')]: '',
    imagePullSecret: 'rk-registry',
    installHarbor: false,
    harborHttpPort: 30090,
    harborDataPath: '/data/harbor',
    harborVersion: '2.14.4',
    insecure: false
  }
}

function mergeSection(name, input) {
  return { ...DEFAULTS[name], ...(input?.[name] || {}) }
}

export function loadConfig(file) {
  const configPath = path.resolve(file)
  if (!existsSync(configPath)) {
    throw new Error(`config file not found: ${configPath}`)
  }
  const source = readFileSync(configPath, 'utf8').replace(/^\uFEFF/, '')
  return normalizeConfig(JSON.parse(source))
}

export function normalizeConfig(input = {}) {
  const resolvedInput = resolveEnvPlaceholders(input)
  const config = {
    ssh: mergeSection('ssh', resolvedInput),
    git: mergeSection('git', resolvedInput),
    deployment: mergeSection('deployment', resolvedInput),
    runtime: mergeSection('runtime', resolvedInput),
    compose: mergeSection('compose', resolvedInput),
    k3s: mergeSection('k3s', resolvedInput),
    admin: mergeSection('admin', resolvedInput),
    mysql: mergeSection('mysql', resolvedInput),
    nacos: mergeSection('nacos', resolvedInput),
    redis: mergeSection('redis', resolvedInput),
    rabbitmq: mergeSection('rabbitmq', resolvedInput),
    minio: mergeSection('minio', resolvedInput),
    xxlJob: mergeSection('xxlJob', resolvedInput),
    registry: mergeSection('registry', resolvedInput),
    releaseId: String(resolvedInput.releaseId || '').trim(),
    services: Array.isArray(resolvedInput.services) && resolvedInput.services.length > 0 ? resolvedInput.services : SERVICE_MATRIX.map((item) => item.name)
  }
  config.runtime.mode = String(config.runtime.mode || 'docker-compose').toLowerCase()
  config.git.provider = String(config.git.provider || 'gitee').toLowerCase()
  config.deployment.accessMode = String(config.deployment.accessMode || 'local-loopback').toLowerCase()
  config.k3s.imageMode = String(config.k3s.imageMode || 'local-tar').toLowerCase()
  config.registry.provider = String(config.registry.provider || (config.k3s.imageMode === 'registry' ? 'custom' : 'none')).toLowerCase()
  config.deployment.targetPath = normalizeTargetPath(config.deployment.targetPath)
  validateConfig(config)
  return config
}

export function validateConfig(config) {
  const errors = []
  if (!config.ssh.host) errors.push('ssh.host is required')
  if (!config.ssh.username) errors.push('ssh.username is required')
  if (!['gitee', 'github'].includes(config.git.provider)) errors.push('git.provider must be gitee or github')
  if (!config.git.repositoryUrl) errors.push('git.repositoryUrl is required')
  if (!config.git.branch) errors.push('git.branch is required')
  if (!['local-loopback', 'public-nodeport'].includes(config.deployment.accessMode)) {
    errors.push('deployment.accessMode must be local-loopback or public-nodeport')
  }
  if (!['docker-compose', 'k3s'].includes(config.runtime.mode)) errors.push('runtime.mode must be docker-compose or k3s')
  if (!['local-tar', 'registry'].includes(config.k3s.imageMode)) errors.push('k3s.imageMode must be local-tar or registry')
  if (!['none', 'harbor', 'aliyun', 'custom'].includes(config.registry.provider)) {
    errors.push('registry.provider must be none, harbor, aliyun, or custom')
  }
  if (!config.admin.username) errors.push('admin.username is required')
  if (errors.length > 0) {
    throw new Error(errors.join('; '))
  }
}

export function assertConnectReady(config) {
  if ((!config.ssh.password || isPlaceholder(config.ssh.password)) && !config.ssh.privateKeyPath) {
    throw new Error('ssh.password or ssh.privateKeyPath is required')
  }
}

export function assertInstallReady(config) {
  assertConnectReady(config)
  const missing = []
  for (const [sectionName, fieldName] of [
    ['admin', 'initialPassword'],
    ['mysql', 'rootPassword'],
    ['mysql', 'appPassword'],
    ['nacos', 'password'],
    ['redis', 'password'],
    ['rabbitmq', 'password'],
    ['minio', 'rootPassword'],
    ['minio', 'accessKey'],
    ['minio', 'secretKey'],
    ['xxlJob', 'accessToken']
  ]) {
    if (!config[sectionName][fieldName] || isPlaceholder(config[sectionName][fieldName])) {
      missing.push(`${sectionName}.${fieldName}`)
    }
  }
  if (config.k3s.imageMode === 'registry') {
    const hasDockerConfigJson = config.registry.dockerConfigJson && !isPlaceholder(config.registry.dockerConfigJson)
    const hasRegistryUserPass = config.registry.username && !isPlaceholder(config.registry.username) && config.registry.password && !isPlaceholder(config.registry.password)
    if (config.registry.provider === 'harbor') {
      for (const fieldName of ['server', 'namespace', 'username', 'password']) {
        if (!config.registry[fieldName] || isPlaceholder(config.registry[fieldName])) {
          missing.push(`registry.${fieldName}`)
        }
      }
    } else if (config.registry.provider === 'aliyun' || config.registry.provider === 'custom') {
      for (const fieldName of ['server', 'namespace', 'username', 'password']) {
        if (!config.registry[fieldName] || isPlaceholder(config.registry[fieldName])) {
          missing.push(`registry.${fieldName}`)
        }
      }
      if (!hasDockerConfigJson && !hasRegistryUserPass) {
        missing.push('registry.dockerConfigJson or registry.username and registry.password')
      }
    } else if (!hasDockerConfigJson && !hasRegistryUserPass) {
      missing.push('registry.dockerConfigJson or registry.username and registry.password')
    }
  }
  if (missing.length > 0) {
    throw new Error(`missing required install values: ${missing.join(', ')}`)
  }
}

export function normalizeTargetPath(value) {
  const target = String(value || '/opt/rk-web').replace(/\/+$/, '')
  if (!target.startsWith('/')) {
    throw new Error('deployment.targetPath must be an absolute Linux path')
  }
  return target
}

export function isPlaceholder(value) {
  return typeof value === 'string' && /^<CHANGE_ME/.test(value)
}

function resolveEnvPlaceholders(value) {
  if (Array.isArray(value)) {
    return value.map((item) => resolveEnvPlaceholders(item))
  }
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.entries(value).map(([key, item]) => [key, resolveEnvPlaceholders(item)]))
  }
  if (typeof value !== 'string') {
    return value
  }
  const match = value.match(/^\$\{([A-Z0-9_]+)\}$/)
  if (!match) {
    return value
  }
  return process.env[match[1]] || ''
}
