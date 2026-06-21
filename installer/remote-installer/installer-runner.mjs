import { spawn } from 'node:child_process'
import { existsSync, mkdirSync, writeFileSync } from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const guiRoot = path.dirname(fileURLToPath(import.meta.url))
const packageRoot = path.resolve(guiRoot, '..')

export function buildConfigFromForm(form = {}) {
  const publicBaseUrl = stringValue(form.publicBaseUrl, 'http://localhost').replace(/\/+$/, '')
  const runtimeMode = choice(form.runtimeMode, ['docker-compose', 'k3s'], 'docker-compose')
  const imageMode = runtimeMode === 'k3s' ? choice(form.k3sImageMode, ['local-tar', 'registry'], 'local-tar') : 'local-tar'
  const minioRootUser = stringValue(form.minioRootUser, 'rkminio')

  const ssh = {
    host: stringValue(form.sshHost),
    port: numberValue(form.sshPort, 22),
    username: stringValue(form.sshUsername, 'root'),
    password: '',
    privateKeyPath: '',
    passphrase: '',
    connectTimeoutSeconds: numberValue(form.connectTimeoutSeconds, 20)
  }
  if (choice(form.sshAuthMode, ['password', 'key'], 'password') === 'key') {
    ssh.privateKeyPath = stringValue(form.sshPrivateKeyPath)
    ssh.passphrase = stringValue(form.sshPassphrase)
  } else {
    ssh.password = stringValue(form.sshPassword)
  }

  return {
    ssh,
    git: {
      provider: choice(form.gitProvider, ['gitee', 'github'], 'gitee'),
      providerOptions: ['gitee', 'github'],
      repositoryUrl: stringValue(form.repositoryUrl),
      branch: stringValue(form.gitBranch, 'main'),
      username: stringValue(form.gitUsername),
      token: stringValue(form.gitToken),
      githubPending: true
    },
    deployment: {
      name: stringValue(form.deploymentName, 'rk-web-open-source'),
      targetPath: stringValue(form.targetPath, '/opt/rk-web'),
      publicBaseUrl,
      timezone: stringValue(form.timezone, 'Asia/Shanghai'),
      domesticMirror: booleanValue(form.domesticMirror, true)
    },
    runtime: {
      mode: runtimeMode,
      supportedModes: ['docker-compose', 'k3s'],
      installDocker: runtimeMode === 'docker-compose' ? true : booleanValue(form.installDocker, false),
      installK3s: runtimeMode === 'k3s' ? booleanValue(form.installK3s, true) : false,
      buildOnRemote: true,
      deployServices: true
    },
    compose: {
      projectName: stringValue(form.composeProjectName, 'rk-web'),
      gatewayPort: numberValue(form.gatewayPort, 10010),
      frontendPort: numberValue(form.frontendPort, 30080),
      nacosPort: numberValue(form.nacosPort, 8848),
      rabbitManagementPort: numberValue(form.rabbitManagementPort, 15672),
      minioConsolePort: numberValue(form.minioConsolePort, 9001)
    },
    k3s: {
      namespace: stringValue(form.k3sNamespace, 'rk-web'),
      imageMode,
      imageModeOptions: ['local-tar', 'registry'],
      storageClassName: stringValue(form.storageClassName, 'local-path'),
      exposureType: 'nodePort',
      frontendNodePort: numberValue(form.frontendNodePort, 30080),
      gatewayNodePort: numberValue(form.gatewayNodePort, 30010)
    },
    admin: {
      tenantName: stringValue(form.tenantName, 'RK System'),
      tenantCode: stringValue(form.tenantCode, 'system'),
      username: stringValue(form.adminUsername, 'admin'),
      initialPassword: stringValue(form.adminPassword),
      email: stringValue(form.adminEmail, 'admin@example.invalid'),
      phone: stringValue(form.adminPhone)
    },
    mysql: {
      mode: 'managed',
      rootUser: 'root',
      rootPassword: stringValue(form.mysqlRootPassword),
      appUser: 'rk_app',
      appPassword: stringValue(form.mysqlAppPassword),
      host: 'rk-mysql',
      port: 3306
    },
    nacos: {
      mode: 'managed',
      serverAddr: 'rk-nacos:8848',
      namespace: stringValue(form.nacosNamespace, 'public'),
      group: stringValue(form.nacosGroup, 'DEFAULT_GROUP'),
      username: 'nacos',
      password: stringValue(form.nacosPassword),
      initializeConfigs: true
    },
    redis: {
      mode: 'managed',
      host: 'rk-redis',
      port: 6379,
      password: stringValue(form.redisPassword),
      database: 0
    },
    rabbitmq: {
      mode: 'managed',
      host: 'rk-rabbitmq',
      port: 5672,
      managementPort: 15672,
      username: 'rk_app',
      password: stringValue(form.rabbitPassword),
      virtualHost: '/rk'
    },
    minio: {
      mode: 'managed',
      endpoint: 'http://rk-minio:9000',
      publicBaseUrl: `${publicBaseUrl}/minio-files/`,
      rootUser: minioRootUser,
      rootPassword: stringValue(form.minioRootPassword),
      accessKey: stringValue(form.minioAccessKey),
      secretKey: stringValue(form.minioSecretKey),
      bucket: stringValue(form.minioBucket, 'rk-bucket')
    },
    xxlJob: {
      mode: 'managed',
      adminAddress: 'http://rk-xxl-job:8880/xxl-job-admin',
      accessToken: stringValue(form.xxlToken)
    },
    registry: {
      server: stringValue(form.registryServer, 'registry.example.invalid'),
      namespace: stringValue(form.registryNamespace, 'rk-web'),
      username: stringValue(form.registryUsername),
      password: stringValue(form.registryPassword),
      dockerConfigJson: stringValue(form.registryDockerConfigJson),
      imagePullSecret: stringValue(form.imagePullSecret, 'rk-registry')
    }
  }
}

export function resolveCliEntrypoint(root = packageRoot, runtime = process) {
  if (runtime.resourcesPath) {
    const resourceExe = path.join(runtime.resourcesPath, 'rk-web-installer.exe')
    if (existsSync(resourceExe)) return resourceExe
    const unpackedExe = path.join(runtime.resourcesPath, 'app.asar.unpacked', 'dist', 'rk-web-installer.exe')
    if (existsSync(unpackedExe)) return unpackedExe
  }
  const exePath = path.join(root, 'dist', 'rk-web-installer.exe')
  if (existsSync(exePath)) return exePath
  return path.join(root, 'src', 'index.mjs')
}

export function resolveCliWorkingDirectory(runtime = process) {
  if (runtime.resourcesPath) {
    const portableDir = runtime.env?.PORTABLE_EXECUTABLE_DIR
    if (portableDir) return portableDir
    if (runtime.execPath) return path.dirname(runtime.execPath)
    return path.dirname(runtime.resourcesPath)
  }
  return packageRoot
}

export function buildInstallerArgs({ command, configPath, logDir, logFile }) {
  const args = [choice(command, ['validate', 'preflight', 'install'], 'validate'), '--config', configPath]
  if (logFile) {
    args.push('--log-file', logFile)
  } else if (logDir) {
    args.push('--log-dir', logDir)
  }
  return args
}

export function writeTempConfig(config, directory = path.join(os.tmpdir(), 'rk-web-installer-gui')) {
  mkdirSync(directory, { recursive: true })
  const file = path.join(directory, `rk-remote-install-${Date.now()}.json`)
  const { config: sanitizedConfig, env } = externalizeSecrets(config)
  writeFileSync(file, JSON.stringify(sanitizedConfig, null, 2), 'utf8')
  return { configPath: file, env }
}

export function runInstallerCommand({ command, form, logDir, logFile, onOutput, onExit, spawnImpl = spawn, runtime = process }) {
  const config = buildConfigFromForm(form)
  const { configPath, env } = writeTempConfig(config)
  const cliPath = resolveCliEntrypoint(packageRoot, runtime)
  const args = buildInstallerArgs({ command, configPath, logDir, logFile })
  const isExe = cliPath.toLowerCase().endsWith('.exe')
  let child
  try {
    child = spawnImpl(isExe ? cliPath : runtime.execPath, isExe ? args : [cliPath, ...args], {
      cwd: resolveCliWorkingDirectory(runtime),
      env: { ...runtime.env, ...env },
      windowsHide: false
    })
  } catch (error) {
    onOutput?.({ stream: 'stderr', text: `[gui] failed to start installer process: ${error.message || String(error)}\n` })
    onExit?.({ code: 1, configPath })
    return null
  }

  child.stdout.on('data', (chunk) => onOutput?.({ stream: 'stdout', text: chunk.toString('utf8') }))
  child.stderr.on('data', (chunk) => onOutput?.({ stream: 'stderr', text: chunk.toString('utf8') }))
  child.on('error', (error) => {
    onOutput?.({ stream: 'stderr', text: `[gui] failed to start installer process: ${error.message || String(error)}\n` })
    onExit?.({ code: 1, configPath })
  })
  child.on('close', (code) => onExit?.({ code, configPath }))
  return child
}

function stringValue(value, fallback = '') {
  const text = String(value ?? '').trim()
  return text || fallback
}

function numberValue(value, fallback) {
  const parsed = Number.parseInt(String(value ?? ''), 10)
  return Number.isFinite(parsed) ? parsed : fallback
}

function booleanValue(value, fallback = false) {
  if (value === undefined || value === null || value === '') return fallback
  if (typeof value === 'boolean') return value
  return ['1', 'true', 'yes', 'y', 'on'].includes(String(value).toLowerCase())
}

function choice(value, choices, fallback) {
  const normalized = String(value ?? '').trim().toLowerCase()
  return choices.includes(normalized) ? normalized : fallback
}

const SECRET_PATHS = [
  ['ssh', 'password', 'RK_GUI_SECRET_SSH_PASSWORD'],
  ['ssh', 'passphrase', 'RK_GUI_SECRET_SSH_PASSPHRASE'],
  ['git', 'token', 'RK_GUI_SECRET_GIT_TOKEN'],
  ['admin', 'initialPassword', 'RK_GUI_SECRET_ADMIN_INITIAL_PASSWORD'],
  ['mysql', 'rootPassword', 'RK_GUI_SECRET_MYSQL_ROOT_PASSWORD'],
  ['mysql', 'appPassword', 'RK_GUI_SECRET_MYSQL_APP_PASSWORD'],
  ['nacos', 'password', 'RK_GUI_SECRET_NACOS_PASSWORD'],
  ['redis', 'password', 'RK_GUI_SECRET_REDIS_PASSWORD'],
  ['rabbitmq', 'password', 'RK_GUI_SECRET_RABBITMQ_PASSWORD'],
  ['minio', 'rootPassword', 'RK_GUI_SECRET_MINIO_ROOT_PASSWORD'],
  ['minio', 'accessKey', 'RK_GUI_SECRET_MINIO_ACCESS_KEY'],
  ['minio', 'secretKey', 'RK_GUI_SECRET_MINIO_SECRET_KEY'],
  ['xxlJob', 'accessToken', 'RK_GUI_SECRET_XXL_JOB_ACCESS_TOKEN'],
  ['registry', 'password', 'RK_GUI_SECRET_REGISTRY_PASSWORD'],
  ['registry', 'dockerConfigJson', 'RK_GUI_SECRET_REGISTRY_DOCKER_CONFIG_JSON']
]

function externalizeSecrets(config) {
  const sanitized = structuredClone(config)
  const env = {}
  for (const [section, field, envName] of SECRET_PATHS) {
    const value = sanitized?.[section]?.[field]
    if (typeof value !== 'string' || value.length === 0) continue
    env[envName] = value
    sanitized[section][field] = `\${${envName}}`
  }
  return { config: sanitized, env }
}
