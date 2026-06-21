#!/usr/bin/env node
import { assertConnectReady, assertInstallReady, loadConfig, normalizeConfig } from './config.mjs'
import { createInterface } from 'node:readline'
import { stdin as input, stdout as output } from 'node:process'
import { createInstallerLogger } from './logger.mjs'
import { rememberSecrets, redactConfig, redactText } from './redaction.mjs'
import { buildRemoteEnv, buildRemoteInstallScript } from './remote-payload.mjs'

async function main() {
  const args = parseArgs(process.argv.slice(2))
  const command = args._[0] || 'wizard'
  if (command === 'help' || args.help) {
    printHelp()
    return
  }
  if (command === 'wizard') {
    const { command: wizardCommand, config } = await runWizard()
    await runConfiguredCommand(wizardCommand, config, args)
    return
  }
  const configPath = args.config
  if (!configPath) throw new Error('missing --config <file>')
  await runConfiguredCommand(command, loadConfig(configPath), args)
}

async function runConfiguredCommand(command, inputConfig, args = {}) {
  const logger = createInstallerLogger({
    command,
    logDir: args['log-dir'],
    logFile: args['log-file']
  })
  const config = normalizeConfig(inputConfig)
  rememberSecrets(config)
  logger.log(`local log file: ${logger.logFile}`)

  try {
    if (command === 'validate') {
      const summary = redactConfig({
        ssh: { host: config.ssh.host, port: config.ssh.port, username: config.ssh.username },
        git: config.git,
        runtime: config.runtime,
        k3s: config.k3s,
        deployment: config.deployment
      })
      logger.log(`configuration valid: ${JSON.stringify(summary)}`)
      return
    }

    if (command === 'script') {
      await ensureAdminPasswordHash(config)
      const script = buildRemoteInstallScript(config)
      process.stdout.write(script)
      logger.file(`generated remote script bytes: ${Buffer.byteLength(script, 'utf8')}`)
      return
    }

    if (command === 'preflight' || command === 'install') {
      if (command === 'preflight') {
        assertConnectReady(config)
      } else {
        await ensureAdminPasswordHash(config)
        assertInstallReady(config)
      }
      const { SshRunner } = await import('./ssh-runner.mjs')
      const runner = new SshRunner(config, logger)
      const script = buildRemoteInstallScript(config)
      await runner.execScript(script, buildRemoteEnv(config, command === 'preflight' ? 'preflight' : 'install'))
      logger.log(`${command} completed successfully`)
      return
    }

    throw new Error(`unknown command: ${command}`)
  } catch (error) {
    logger.error(error.message || String(error))
    throw error
  } finally {
    await logger.close()
  }
}

async function runWizard() {
  if (!input.isTTY || !output.isTTY) {
    return buildWizardConfig(await readPipedAnswers())
  }
  const rl = createPromptInterface()
  try {
    return buildWizardConfig(await collectWizardAnswers(async (prompt, defaultValue) => {
      const suffix = defaultValue ? ` (${defaultValue})` : ''
      return rl.question(`${prompt}${suffix}: `).then((answer) => answer.trim() || defaultValue)
    }))
  } finally {
    rl.close()
  }
}

async function readPipedAnswers() {
  const rl = createInterface({ input, crlfDelay: Infinity })
  const lines = []
  try {
    for await (const line of rl) {
      lines.push(line.trim())
    }
  } finally {
    rl.close()
  }
  return lines
}

function buildWizardConfig(answers) {
  let index = 0
  const take = (defaultValue = '') => {
    const raw = answers[index++] ?? ''
    const value = String(raw).trim()
    return value || defaultValue
  }
  const required = (label) => {
    const value = take('')
    if (!value) throw new Error(`${label} is required`)
    return value
  }
  const sshHost = required('SSH host')
  const sshPort = parseIntOrDefault(take('22'), 22)
  const sshUsername = take('root')
  const authMode = normalizeChoice(take('password'), ['password', 'key'], 'password')
  const ssh = {
    host: sshHost,
    port: sshPort,
    username: sshUsername,
    password: '',
    privateKeyPath: '',
    passphrase: '',
    connectTimeoutSeconds: 20
  }
  if (authMode === 'key') {
    ssh.privateKeyPath = required('SSH private key path')
    ssh.passphrase = take('')
  } else {
    ssh.password = required('SSH password')
  }

  const provider = normalizeChoice(take('gitee'), ['gitee', 'github'], 'gitee')
  const repositoryUrl = required('Git repository URL')
  const branch = take('main')
  const runtimeMode = normalizeChoice(take('docker-compose'), ['docker-compose', 'k3s'], 'docker-compose')
  const installDocker = runtimeMode === 'docker-compose' ? true : yesNo(take('n'))
  const installK3s = runtimeMode === 'k3s' ? yesNo(take('y')) : false
  const imageMode = runtimeMode === 'k3s'
    ? normalizeChoice(take('local-tar'), ['local-tar', 'registry'], 'local-tar')
    : 'local-tar'
  const targetPath = take('/opt/rk-web')
  const publicBaseUrl = take('http://localhost')
  const domesticMirror = yesNo(take('y'))
  const namespace = runtimeMode === 'k3s' ? take('rk-web') : 'rk-web'
  const composeProjectName = runtimeMode === 'docker-compose' ? take('rk-web') : 'rk-web'
  const adminUsername = take('admin')
  const adminPassword = required('Admin initial password')
  const adminEmail = take('admin@example.invalid')
  const adminPhone = take('')
  const mysqlRootPassword = required('MySQL root password')
  const mysqlAppPassword = required('MySQL app password')
  const nacosPassword = required('Nacos password')
  const redisPassword = required('Redis password')
  const rabbitPassword = required('RabbitMQ password')
  const minioRootUser = take('rkminio')
  const minioRootPassword = required('MinIO root password')
  const minioAccessKey = required('MinIO access key')
  const minioSecretKey = required('MinIO secret key')
  const xxlToken = required('XXL-Job access token')
  const registry = {
    server: 'registry.example.invalid',
    namespace: 'rk-web',
    username: '',
    password: '',
    dockerConfigJson: '',
    imagePullSecret: 'rk-registry'
  }
  if (runtimeMode === 'k3s' && imageMode === 'registry') {
    registry.server = take('registry.example.invalid')
    registry.namespace = take('rk-web')
    registry.username = take('')
    registry.password = take('')
    registry.dockerConfigJson = take('')
  }
  const action = normalizeChoice(take('validate'), ['validate', 'preflight', 'install', 'script'], 'validate')
  return {
    command: action,
    config: {
      ssh,
      git: { provider, repositoryUrl, branch, username: '', token: '' },
      deployment: { name: 'rk-web-open-source', targetPath, publicBaseUrl, timezone: 'Asia/Shanghai', domesticMirror },
      runtime: { mode: runtimeMode, installDocker, installK3s, buildOnRemote: true, deployServices: true },
      compose: { projectName: composeProjectName, gatewayPort: 10010, frontendPort: 30080, nacosPort: 8848, rabbitManagementPort: 15672, minioConsolePort: 9001 },
      k3s: { namespace, imageMode, storageClassName: 'local-path', exposureType: 'nodePort', frontendNodePort: 30080, gatewayNodePort: 30010 },
      admin: { tenantName: 'RK System', tenantCode: 'system', username: adminUsername, initialPassword: adminPassword, email: adminEmail, phone: adminPhone },
      mysql: { mode: 'managed', rootUser: 'root', rootPassword: mysqlRootPassword, appUser: 'rk_app', appPassword: mysqlAppPassword, host: 'rk-mysql', port: 3306 },
      nacos: { mode: 'managed', serverAddr: 'rk-nacos:8848', namespace: 'public', group: 'DEFAULT_GROUP', username: 'nacos', password: nacosPassword, initializeConfigs: true },
      redis: { mode: 'managed', host: 'rk-redis', port: 6379, password: redisPassword, database: 0 },
      rabbitmq: { mode: 'managed', host: 'rk-rabbitmq', port: 5672, managementPort: 15672, username: 'rk_app', password: rabbitPassword, virtualHost: '/rk' },
      minio: { mode: 'managed', endpoint: 'http://rk-minio:9000', publicBaseUrl: `${publicBaseUrl.replace(/\/+$/, '')}/minio-files/`, rootUser: minioRootUser, rootPassword: minioRootPassword, accessKey: minioAccessKey, secretKey: minioSecretKey, bucket: 'rk-bucket' },
      xxlJob: { mode: 'managed', adminAddress: 'http://rk-xxl-job:8880/xxl-job-admin', accessToken: xxlToken },
      registry
    }
  }
}

function createPromptInterface() {
  const rl = createInterface({ input, output })
  return {
    question(prompt) {
      return new Promise((resolve) => rl.question(prompt, resolve))
    },
    close() {
      rl.close()
    }
  }
}

async function collectWizardAnswers(ask) {
  const responses = []
  console.log('RK-Web remote installer wizard')
  responses.push(await ask('SSH host'))
  responses.push(await ask('SSH port', '22'))
  responses.push(await ask('SSH username', 'root'))
  responses.push(await ask('SSH auth mode [password/key]', 'password'))
  const authMode = normalizeChoice(responses[3], ['password', 'key'], 'password')
  if (authMode === 'key') {
    responses.push(await ask('SSH private key path'))
    responses.push(await ask('SSH key passphrase', ''))
  } else {
    responses.push(await ask('SSH password'))
  }
  responses.push(await ask('Git provider [gitee/github]', 'gitee'))
  responses.push(await ask('Git repository URL'))
  responses.push(await ask('Git branch', 'main'))
  responses.push(await ask('Runtime [docker-compose/k3s]', 'docker-compose'))
  const runtimeMode = normalizeChoice(responses[8], ['docker-compose', 'k3s'], 'docker-compose')
  let imageMode = 'local-tar'
  if (runtimeMode === 'k3s') {
    responses.push(await ask('Install Docker for K3s build path? [y/n]', 'n'))
    responses.push(await ask('Install K3s automatically? [y/n]', 'y'))
    imageMode = normalizeChoice(await ask('K3s image mode [local-tar/registry]', 'local-tar'), ['local-tar', 'registry'], 'local-tar')
    responses.push(imageMode)
  }
  responses.push(await ask('Target path', '/opt/rk-web'))
  responses.push(await ask('Public base URL', 'http://localhost'))
  responses.push(await ask('Use domestic mirrors? [y/n]', 'y'))
  if (runtimeMode === 'k3s') {
    responses.push(await ask('K3s namespace', 'rk-web'))
  }
  if (runtimeMode === 'docker-compose') {
    responses.push(await ask('Compose project name', 'rk-web'))
  }
  responses.push(await ask('Admin username', 'admin'))
  responses.push(await ask('Admin initial password'))
  responses.push(await ask('Admin email', 'admin@example.invalid'))
  responses.push(await ask('Admin phone', ''))
  responses.push(await ask('MySQL root password'))
  responses.push(await ask('MySQL app password'))
  responses.push(await ask('Nacos password'))
  responses.push(await ask('Redis password'))
  responses.push(await ask('RabbitMQ password'))
  responses.push(await ask('MinIO root user', 'rkminio'))
  responses.push(await ask('MinIO root password'))
  responses.push(await ask('MinIO access key'))
  responses.push(await ask('MinIO secret key'))
  responses.push(await ask('XXL-Job access token'))
  if (runtimeMode === 'k3s' && imageMode === 'registry') {
    responses.push(await ask('Registry server', 'registry.example.invalid'))
    responses.push(await ask('Registry namespace', 'rk-web'))
    responses.push(await ask('Registry username', ''))
    responses.push(await ask('Registry password', ''))
    responses.push(await ask('Registry dockerconfigjson', ''))
  }
  responses.push(await ask('Action [validate/preflight/install/script]', 'validate'))
  return responses
}

function normalizeChoice(value, choices, fallback) {
  const normalized = String(value || '').trim().toLowerCase()
  return choices.includes(normalized) ? normalized : fallback
}

function yesNo(value) {
  return ['y', 'yes', 'true', '1'].includes(String(value || '').trim().toLowerCase())
}

function parseIntOrDefault(value, fallback) {
  const parsed = Number.parseInt(String(value), 10)
  return Number.isFinite(parsed) ? parsed : fallback
}

function parseArgs(argv) {
  const result = { _: [] }
  for (let index = 0; index < argv.length; index += 1) {
    const item = argv[index]
    if (!item.startsWith('--')) {
      result._.push(item)
      continue
    }
    const key = item.slice(2)
    const value = argv[index + 1]
    if (!value || value.startsWith('--')) {
      result[key] = true
    } else {
      result[key] = value
      index += 1
    }
  }
  return result
}

async function ensureAdminPasswordHash(config) {
  if (config.adminPasswordBcrypt) {
    return config.adminPasswordBcrypt
  }
  const bcrypt = await import('bcryptjs')
  config.adminPasswordBcrypt = await bcrypt.hash(config.admin.initialPassword, 10)
  return config.adminPasswordBcrypt
}

function printHelp() {
  console.log(`RK-Web remote installer

Usage:
  rk-web-installer
  rk-web-installer wizard
  rk-web-installer validate --config rk-remote-install.json [--log-dir logs]
  rk-web-installer preflight --config rk-remote-install.json [--log-dir logs]
  rk-web-installer install --config rk-remote-install.json [--log-dir logs]
  rk-web-installer script --config rk-remote-install.json [--log-file installer.log]
`)
}

main().catch((error) => {
  console.error(redactText(error.message || String(error)))
  process.exit(1)
})
