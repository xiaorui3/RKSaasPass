import assert from 'node:assert/strict'
import { mkdtempSync, readFileSync, rmSync } from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

test('gui runner builds installer config from form values', async () => {
  const { buildConfigFromForm } = await import('../gui/installer-runner.mjs')
  const config = buildConfigFromForm({
    sshHost: '203.0.113.10',
    sshPort: '30139',
    sshUsername: 'root',
    sshAuthMode: 'password',
    sshPassword: 'ssh-secret',
    gitProvider: 'gitee',
    repositoryUrl: 'https://gitee.com/example/rk-web.git',
    gitBranch: 'cloud-master-new',
    runtimeMode: 'docker-compose',
    targetPath: '/opt/rk-web',
    publicBaseUrl: 'http://203.0.113.10',
    composeProjectName: 'rk-web',
    adminUsername: 'admin',
    adminPassword: 'admin-secret',
    adminEmail: 'admin@example.invalid',
    mysqlRootPassword: 'mysql-root',
    mysqlAppPassword: 'mysql-app',
    nacosPassword: 'nacos-secret',
    redisPassword: 'redis-secret',
    rabbitPassword: 'mq-secret',
    minioRootUser: 'rkminio',
    minioRootPassword: 'minio-root',
    minioAccessKey: 'minio-access',
    minioSecretKey: 'minio-secret',
    xxlToken: 'xxl-secret',
    logDir: 'C:\\temp\\rk-logs'
  })

  assert.equal(config.ssh.host, '203.0.113.10')
  assert.equal(config.ssh.port, 30139)
  assert.equal(config.git.provider, 'gitee')
  assert.equal(config.git.branch, 'cloud-master-new')
  assert.equal(config.runtime.mode, 'docker-compose')
  assert.equal(config.deployment.accessMode, 'local-loopback')
  assert.equal(config.admin.initialPassword, 'admin-secret')
  assert.equal(config.mysql.rootPassword, 'mysql-root')
  assert.equal(config.minio.publicBaseUrl, 'http://203.0.113.10/minio-files/')
})

test('gui runner builds harbor registry defaults for k3s registry installs', async () => {
  const { buildConfigFromForm } = await import('../gui/installer-runner.mjs')
  const config = buildConfigFromForm({
    sshHost: '203.0.113.10',
    sshPort: '30139',
    sshUsername: 'root',
    runtimeMode: 'k3s',
    k3sImageMode: 'registry',
    registryProvider: 'harbor',
    publicBaseUrl: '',
    repositoryUrl: 'https://gitee.com/example/rk-web.git',
    gitBranch: 'cloud-master-new'
  })

  assert.equal(config.deployment.publicBaseUrl, 'http://127.0.0.1:30080')
  assert.equal(config.deployment.accessMode, 'local-loopback')
  assert.equal(config.registry.provider, 'harbor')
  assert.equal(config.registry.server, '127.0.0.1:30090')
  assert.equal(config.registry.namespace, 'rk-web')
  assert.equal(config.registry.username, 'admin')
  assert.equal(config.registry.password, 'Harbor12345')
  assert.equal(config.registry.installHarbor, true)
  assert.equal(config.registry.harborHttpPort, 30090)
  assert.equal(config.registry.harborVersion, '2.14.4')
  assert.equal(config.registry.insecure, true)
})

test('gui runner passes custom harbor download urls as a newline list', async () => {
  const { buildConfigFromForm } = await import('../gui/installer-runner.mjs')
  const config = buildConfigFromForm({
    sshHost: '203.0.113.10',
    runtimeMode: 'k3s',
    k3sImageMode: 'registry',
    registryProvider: 'harbor',
    repositoryUrl: 'https://gitee.com/example/rk-web.git',
    harborVersion: '2.14.4',
    harborDownloadUrls: 'https://mirror.example/harbor.tgz\n\nhttps://backup.example/harbor.tgz'
  })

  assert.deepEqual(config.registry.harborDownloadUrls, [
    'https://mirror.example/harbor.tgz',
    'https://backup.example/harbor.tgz'
  ])
})

test('gui runner can reuse an existing release image set for deployment retries', async () => {
  const { buildConfigFromForm } = await import('../gui/installer-runner.mjs')
  const config = buildConfigFromForm({
    sshHost: '203.0.113.10',
    runtimeMode: 'k3s',
    k3sImageMode: 'registry',
    registryProvider: 'harbor',
    repositoryUrl: 'https://gitee.com/example/rk-web.git',
    releaseId: 'rk-web-20260620090240',
    buildOnRemote: false
  })

  assert.equal(config.releaseId, 'rk-web-20260620090240')
  assert.equal(config.runtime.buildOnRemote, false)
  assert.equal(config.runtime.deployServices, true)
})

test('installer config requires registry credentials for aliyun provider', async () => {
  const { assertInstallReady, normalizeConfig } = await import('../src/config.mjs')
  const config = normalizeConfig({
    ssh: { host: '203.0.113.10', port: 30139, username: 'root', password: 'ssh-secret' },
    git: { provider: 'gitee', repositoryUrl: 'https://gitee.com/example/rk-web.git', branch: 'cloud-master-new' },
    runtime: { mode: 'k3s', installK3s: true, installDocker: false },
    k3s: { imageMode: 'registry', namespace: 'rk-web-test' },
    deployment: { targetPath: '/opt/rk-web', publicBaseUrl: 'http://203.0.113.10:30080' },
    admin: { username: 'admin', initialPassword: 'admin-secret' },
    mysql: { rootPassword: 'mysql-root', appPassword: 'mysql-app' },
    nacos: { password: 'nacos-secret' },
    redis: { password: 'redis-secret' },
    rabbitmq: { password: 'rabbit-secret' },
    minio: { rootPassword: 'minio-root', accessKey: 'minio-access', secretKey: 'minio-secret' },
    xxlJob: { accessToken: 'xxl-secret' },
    registry: { provider: 'aliyun', server: 'registry.example.com', namespace: '', username: '', password: '' }
  })

  assert.throws(
    () => assertInstallReady(config),
    /registry.namespace, registry.username, registry.password/
  )
})

test('gui runner builds command arguments without leaking secrets in command line', async () => {
  const { buildInstallerArgs, resolveCliEntrypoint } = await import('../gui/installer-runner.mjs')
  const cli = resolveCliEntrypoint(repoRoot)
  const args = buildInstallerArgs({
    command: 'install',
    configPath: 'C:\\temp\\rk-remote-install.json',
    logDir: 'C:\\temp\\rk-logs'
  })

  assert.equal(cli.endsWith(path.join('src', 'index.mjs')) || cli.endsWith(path.join('dist', 'rk-web-installer.exe')), true)
  assert.deepEqual(args, [
    'install',
    '--config',
    'C:\\temp\\rk-remote-install.json',
    '--log-dir',
    'C:\\temp\\rk-logs'
  ])
  assert.doesNotMatch(args.join(' '), /ssh-secret|mysql-root|admin-secret/)
})

test('gui runner uses a real working directory when packaged in electron', async () => {
  const { resolveCliWorkingDirectory } = await import('../gui/installer-runner.mjs')

  assert.equal(
    resolveCliWorkingDirectory({
      resourcesPath: 'C:\\Users\\tester\\AppData\\Local\\Temp\\rk-portable\\resources',
      execPath: 'E:\\apps\\rk-web-installer-gui.exe',
      env: { PORTABLE_EXECUTABLE_DIR: 'E:\\apps' }
    }),
    'E:\\apps'
  )
  assert.equal(
    resolveCliWorkingDirectory({
      resourcesPath: 'C:\\Users\\tester\\AppData\\Local\\Temp\\rk-portable\\resources',
      execPath: 'E:\\apps\\rk-web-installer-gui.exe',
      env: {}
    }),
    'E:\\apps'
  )
})

test('gui runner reports child process startup errors and releases the task', async () => {
  const { EventEmitter } = await import('node:events')
  const { runInstallerCommand } = await import('../gui/installer-runner.mjs')
  const events = []
  const fakeChild = new EventEmitter()
  fakeChild.stdout = new EventEmitter()
  fakeChild.stderr = new EventEmitter()
  const child = runInstallerCommand({
    command: 'preflight',
    form: {
      sshHost: '203.0.113.10',
      sshPort: '30139',
      sshUsername: 'root',
      sshAuthMode: 'password',
      sshPassword: 'ssh-secret',
      repositoryUrl: 'https://gitee.com/example/rk-web.git',
      gitBranch: 'cloud-master-new'
    },
    spawnImpl: () => fakeChild,
    onOutput: (item) => events.push(['output', item]),
    onExit: (item) => events.push(['exit', item])
  })

  assert.equal(child, fakeChild)
  fakeChild.emit('error', new Error('spawn EINVAL'))

  assert.equal(events.length, 2)
  assert.equal(events[0][0], 'output')
  assert.equal(events[0][1].stream, 'stderr')
  assert.match(events[0][1].text, /failed to start installer process: spawn EINVAL/)
  assert.deepEqual(events[1][0], 'exit')
  assert.equal(events[1][1].code, 1)
})

test('gui runner reports synchronous spawn failures and releases the task', async () => {
  const { runInstallerCommand } = await import('../gui/installer-runner.mjs')
  const events = []
  const child = runInstallerCommand({
    command: 'preflight',
    form: {
      sshHost: '203.0.113.10',
      sshPort: '30139',
      sshUsername: 'root',
      sshAuthMode: 'password',
      sshPassword: 'ssh-secret',
      repositoryUrl: 'https://gitee.com/example/rk-web.git',
      gitBranch: 'cloud-master-new'
    },
    spawnImpl: () => {
      throw new Error('invalid cwd')
    },
    onOutput: (item) => events.push(['output', item]),
    onExit: (item) => events.push(['exit', item])
  })

  assert.equal(child, null)
  assert.equal(events.length, 2)
  assert.equal(events[0][1].stream, 'stderr')
  assert.match(events[0][1].text, /failed to start installer process: invalid cwd/)
  assert.equal(events[1][1].code, 1)
})

test('gui runner writes temp config without plaintext secrets and returns env overrides', async () => {
  const { buildConfigFromForm, writeTempConfig } = await import('../gui/installer-runner.mjs')
  const tempDir = mkdtempSync(path.join(os.tmpdir(), 'rk-gui-runner-test-'))
  try {
    const config = buildConfigFromForm({
      sshHost: '203.0.113.10',
      sshPort: '30139',
      sshUsername: 'root',
      sshAuthMode: 'password',
      sshPassword: 'ssh-secret',
      gitProvider: 'gitee',
      repositoryUrl: 'https://gitee.com/example/rk-web.git',
      gitUsername: 'gitee-user',
      gitToken: 'git-secret',
      gitBranch: 'cloud-master-new',
      runtimeMode: 'k3s',
      k3sImageMode: 'local-tar',
      installK3s: true,
      targetPath: '/opt/rk-web',
      publicBaseUrl: 'http://203.0.113.10',
      k3sNamespace: 'rk-web-test',
      adminPassword: 'admin-secret',
      mysqlRootPassword: 'mysql-root',
      mysqlAppPassword: 'mysql-app',
      nacosPassword: 'nacos-secret',
      redisPassword: 'redis-secret',
      rabbitPassword: 'mq-secret',
      minioRootPassword: 'minio-root',
      minioAccessKey: 'minio-access',
      minioSecretKey: 'minio-secret',
      xxlToken: 'xxl-secret',
      registryPassword: 'registry-secret'
    })

    const { configPath, env } = writeTempConfig(config, tempDir)
    const text = readFileSync(configPath, 'utf8')

    for (const secret of [
      'ssh-secret',
      'git-secret',
      'admin-secret',
      'mysql-root',
      'mysql-app',
      'nacos-secret',
      'redis-secret',
      'mq-secret',
      'minio-root',
      'minio-access',
      'minio-secret',
      'xxl-secret',
      'registry-secret'
    ]) {
      assert.equal(text.includes(secret), false, `temp config leaked ${secret}`)
    }

    const parsed = JSON.parse(text)
    assert.equal(parsed.ssh.password, '${RK_GUI_SECRET_SSH_PASSWORD}')
    assert.equal(parsed.git.username, 'gitee-user')
    assert.equal(parsed.git.token, '${RK_GUI_SECRET_GIT_TOKEN}')
    assert.equal(parsed.admin.initialPassword, '${RK_GUI_SECRET_ADMIN_INITIAL_PASSWORD}')
    assert.equal(parsed.mysql.rootPassword, '${RK_GUI_SECRET_MYSQL_ROOT_PASSWORD}')
    assert.equal(env.RK_GUI_SECRET_SSH_PASSWORD, 'ssh-secret')
    assert.equal(env.RK_GUI_SECRET_GIT_TOKEN, 'git-secret')
    assert.equal(env.RK_GUI_SECRET_ADMIN_INITIAL_PASSWORD, 'admin-secret')
    assert.equal(env.RK_GUI_SECRET_MYSQL_ROOT_PASSWORD, 'mysql-root')
  } finally {
    rmSync(tempDir, { recursive: true, force: true })
  }
})

test('installer config resolves gui secret placeholders from process env', async () => {
  const { loadConfig } = await import('../src/config.mjs')
  const { buildConfigFromForm, writeTempConfig } = await import('../gui/installer-runner.mjs')
  const tempDir = mkdtempSync(path.join(os.tmpdir(), 'rk-gui-runner-test-'))
  const previous = process.env.RK_GUI_SECRET_SSH_PASSWORD
  try {
    const config = buildConfigFromForm({
      sshHost: '203.0.113.10',
      sshPort: '30139',
      sshUsername: 'root',
      sshAuthMode: 'password',
      sshPassword: 'ssh-secret',
      gitProvider: 'gitee',
      repositoryUrl: 'https://gitee.com/example/rk-web.git',
      gitBranch: 'cloud-master-new',
      runtimeMode: 'k3s',
      adminPassword: 'admin-secret',
      mysqlRootPassword: 'mysql-root',
      mysqlAppPassword: 'mysql-app',
      nacosPassword: 'nacos-secret',
      redisPassword: 'redis-secret',
      rabbitPassword: 'mq-secret',
      minioRootPassword: 'minio-root',
      minioAccessKey: 'minio-access',
      minioSecretKey: 'minio-secret',
      xxlToken: 'xxl-secret'
    })

    const { configPath, env } = writeTempConfig(config, tempDir)
    Object.assign(process.env, env)
    const loaded = loadConfig(configPath)

    assert.equal(loaded.ssh.password, 'ssh-secret')
    assert.equal(loaded.admin.initialPassword, 'admin-secret')
    assert.equal(loaded.mysql.rootPassword, 'mysql-root')
  } finally {
    if (previous === undefined) {
      delete process.env.RK_GUI_SECRET_SSH_PASSWORD
    } else {
      process.env.RK_GUI_SECRET_SSH_PASSWORD = previous
    }
    rmSync(tempDir, { recursive: true, force: true })
  }
})
