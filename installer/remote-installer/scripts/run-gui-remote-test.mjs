#!/usr/bin/env node
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { buildConfigFromForm, runInstallerCommand } from '../gui/installer-runner.mjs'

const scriptRoot = path.dirname(fileURLToPath(import.meta.url))
const packageRoot = path.resolve(scriptRoot, '..')

export function buildRemoteTestPayload({ command = 'preflight', env = process.env, logDir = '' } = {}) {
  const sshPassword = requiredEnv(env, 'RK_TEST_SSH_PASSWORD')
  const generated = stableGeneratedValues(env)
  const sshHost = env.RK_TEST_SSH_HOST || '203.0.113.10'
  const k3sImageMode = env.RK_TEST_K3S_IMAGE_MODE || 'local-tar'
  const frontendNodePort = env.RK_TEST_FRONTEND_NODE_PORT || '30080'
  const gatewayNodePort = env.RK_TEST_GATEWAY_NODE_PORT || '30010'
  const accessMode = env.RK_TEST_ACCESS_MODE || 'local-loopback'
  const registryProvider = env.RK_TEST_REGISTRY_PROVIDER || (k3sImageMode === 'registry' ? 'harbor' : 'custom')
  const harborHttpPort = env.RK_TEST_HARBOR_HTTP_PORT || '30090'
  const registryServer = env.RK_TEST_REGISTRY_SERVER || (registryProvider === 'harbor' ? `127.0.0.1:${harborHttpPort}` : 'registry.example.invalid')
  const registryUsername = env.RK_TEST_REGISTRY_USERNAME || (registryProvider === 'harbor' ? 'admin' : '')
  const registryPassword = env.RK_TEST_REGISTRY_PASSWORD || (registryProvider === 'harbor' ? generated.registryPassword : '')
  const form = {
    sshHost,
    sshPort: env.RK_TEST_SSH_PORT || '30139',
    sshUsername: env.RK_TEST_SSH_USERNAME || 'root',
    sshAuthMode: 'password',
    sshPassword,
    connectTimeoutSeconds: env.RK_TEST_CONNECT_TIMEOUT_SECONDS || '30',
    gitProvider: env.RK_TEST_GIT_PROVIDER || 'gitee',
    repositoryUrl: env.RK_TEST_REPOSITORY_URL || 'https://github.com/example/rk-web',
    gitBranch: env.RK_TEST_GIT_BRANCH || 'cloud-master-new',
    gitUsername: env.RK_TEST_GIT_USERNAME || '',
    gitToken: env.RK_TEST_GIT_TOKEN || '',
    runtimeMode: 'k3s',
    k3sImageMode,
    installK3s: env.RK_TEST_INSTALL_K3S ? truthy(env.RK_TEST_INSTALL_K3S) : true,
    installDocker: env.RK_TEST_INSTALL_DOCKER ? truthy(env.RK_TEST_INSTALL_DOCKER) : false,
    buildOnRemote: env.RK_TEST_BUILD_ON_REMOTE ? truthy(env.RK_TEST_BUILD_ON_REMOTE) : true,
    deployServices: env.RK_TEST_DEPLOY_SERVICES ? truthy(env.RK_TEST_DEPLOY_SERVICES) : true,
    releaseId: env.RK_TEST_RELEASE_ID || '',
    targetPath: env.RK_TEST_TARGET_PATH || '/opt/rk-web',
    publicBaseUrl: env.RK_TEST_PUBLIC_BASE_URL || `http://127.0.0.1:${frontendNodePort}`,
    accessMode,
    k3sNamespace: env.RK_TEST_K3S_NAMESPACE || 'rk-web-test',
    frontendNodePort,
    gatewayNodePort,
    tenantName: env.RK_TEST_TENANT_NAME || 'RK System',
    tenantCode: env.RK_TEST_TENANT_CODE || 'system',
    adminUsername: env.RK_TEST_ADMIN_USERNAME || 'admin',
    adminPassword: env.RK_TEST_ADMIN_PASSWORD || generated.adminPassword,
    adminEmail: env.RK_TEST_ADMIN_EMAIL || 'admin@example.invalid',
    adminPhone: env.RK_TEST_ADMIN_PHONE || '',
    mysqlRootPassword: env.RK_TEST_MYSQL_ROOT_PASSWORD || generated.mysqlRootPassword,
    mysqlAppPassword: env.RK_TEST_MYSQL_APP_PASSWORD || generated.mysqlAppPassword,
    nacosPassword: env.RK_TEST_NACOS_PASSWORD || generated.nacosPassword,
    redisPassword: env.RK_TEST_REDIS_PASSWORD || generated.redisPassword,
    rabbitPassword: env.RK_TEST_RABBIT_PASSWORD || generated.rabbitPassword,
    minioRootUser: env.RK_TEST_MINIO_ROOT_USER || 'rkminio',
    minioRootPassword: env.RK_TEST_MINIO_ROOT_PASSWORD || generated.minioRootPassword,
    minioAccessKey: env.RK_TEST_MINIO_ACCESS_KEY || generated.minioAccessKey,
    minioSecretKey: env.RK_TEST_MINIO_SECRET_KEY || generated.minioSecretKey,
    minioBucket: env.RK_TEST_MINIO_BUCKET || 'rk-bucket',
    xxlToken: env.RK_TEST_XXL_TOKEN || generated.xxlToken,
    registryProvider,
    registryServer,
    registryNamespace: env.RK_TEST_REGISTRY_NAMESPACE || 'rk-web',
    registryUsername,
    registryPassword,
    registryDockerConfigJson: env.RK_TEST_REGISTRY_DOCKER_CONFIG_JSON || '',
    imagePullSecret: env.RK_TEST_IMAGE_PULL_SECRET || 'rk-registry',
    installHarbor: registryProvider === 'harbor' ? true : truthy(env.RK_TEST_INSTALL_HARBOR),
    harborHttpPort,
    harborDataPath: env.RK_TEST_HARBOR_DATA_PATH || '/data/harbor',
    harborVersion: env.RK_TEST_HARBOR_VERSION || '2.14.4',
    harborDownloadUrls: env.RK_TEST_HARBOR_DOWNLOAD_URLS || '',
    registryInsecure: registryProvider === 'harbor' ? true : truthy(env.RK_TEST_REGISTRY_INSECURE)
  }
  return {
    command,
    form,
    logDir: logDir || env.RK_TEST_LOG_DIR || path.join(packageRoot, 'logs', 'remote-test'),
    config: buildConfigFromForm(form)
  }
}

async function main() {
  const args = parseArgs(process.argv.slice(2))
  const command = args._[0] || 'preflight'
  const payload = buildRemoteTestPayload({ command, logDir: args['log-dir'] || '' })
  await runGuiRemoteTest(payload)
}

export function runGuiRemoteTest(payload) {
  return new Promise((resolve, reject) => {
    const child = runInstallerCommand({
      command: payload.command,
      form: payload.form,
      logDir: payload.logDir,
      onOutput: ({ stream, text }) => {
        if (stream === 'stderr') {
          process.stderr.write(text)
        } else {
          process.stdout.write(text)
        }
      },
      onExit: ({ code, configPath }) => {
        const result = { code, configPath, logDir: payload.logDir }
        if (code === 0) {
          resolve(result)
          return
        }
        reject(Object.assign(new Error(`remote ${payload.command} failed with exit ${code}`), result))
      }
    })
    child.once('error', reject)
  })
}

function requiredEnv(env, name) {
  const value = String(env[name] || '').trim()
  if (!value) {
    throw new Error(`${name} is required. Set it only for this process; do not put it in config files or command arguments.`)
  }
  return value
}

function stableGeneratedValues(env) {
  const seed = env.RK_TEST_SECRET_SEED || 'remote-k3s-test'
  return {
    adminPassword: `${seed}-Admin-ChangeMe-2026`,
    mysqlRootPassword: `${seed}-MysqlRoot-2026`,
    mysqlAppPassword: `${seed}-MysqlApp-2026`,
    nacosPassword: `${seed}-Nacos-2026`,
    redisPassword: `${seed}-Redis-2026`,
    rabbitPassword: `${seed}-Rabbit-2026`,
    minioRootPassword: `${seed}-MinioRoot-2026`,
    minioAccessKey: 'rkminioaccess',
    minioSecretKey: `${seed}-MinioSecret-2026`,
    xxlToken: `${seed}-XxlToken-2026`,
    registryPassword: `${seed}-Harbor-2026`
  }
}

function truthy(value) {
  return ['1', 'true', 'yes', 'y', 'on'].includes(String(value || '').trim().toLowerCase())
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

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main().catch((error) => {
    console.error(error.message || String(error))
    process.exit(1)
  })
}
