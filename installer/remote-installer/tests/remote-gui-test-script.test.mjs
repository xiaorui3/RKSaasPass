import assert from 'node:assert/strict'
import { mkdtempSync, readFileSync, rmSync } from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import test from 'node:test'

test('remote gui test script refuses to run without ssh password env', async () => {
  const { buildRemoteTestPayload } = await import('../scripts/run-gui-remote-test.mjs')

  assert.throws(
    () => buildRemoteTestPayload({ command: 'preflight', env: {} }),
    /RK_TEST_SSH_PASSWORD/
  )
})

test('remote gui test payload uses k3s target defaults without leaking ssh password to temp config', async () => {
  const { buildRemoteTestPayload } = await import('../scripts/run-gui-remote-test.mjs')
  const { writeTempConfig } = await import('../gui/installer-runner.mjs')
  const tempDir = mkdtempSync(path.join(os.tmpdir(), 'rk-remote-gui-test-'))
  try {
    const payload = buildRemoteTestPayload({
      command: 'preflight',
      env: {
        RK_TEST_SSH_PASSWORD: 'ssh-secret',
        RK_TEST_ADMIN_PASSWORD: 'admin-secret'
      },
      logDir: 'C:\\rk-web-installer-logs'
    })

    assert.equal(payload.command, 'preflight')
    assert.equal(payload.form.sshHost, '203.0.113.10')
    assert.equal(payload.form.sshPort, '30139')
    assert.equal(payload.form.sshUsername, 'root')
    assert.equal(payload.form.runtimeMode, 'k3s')
    assert.equal(payload.form.k3sImageMode, 'local-tar')
    assert.equal(payload.form.installK3s, true)
    assert.equal(payload.form.installDocker, false)
    assert.equal(payload.form.gitProvider, 'gitee')
    assert.equal(payload.form.gitBranch, 'cloud-master-new')
    assert.equal(payload.form.repositoryUrl, 'https://github.com/example/rk-web')
    assert.equal(payload.form.publicBaseUrl, 'http://127.0.0.1:30080')
    assert.equal(payload.form.accessMode, 'local-loopback')
    assert.equal(payload.config.deployment.publicBaseUrl, 'http://127.0.0.1:30080')
    assert.equal(payload.config.deployment.accessMode, 'local-loopback')
    assert.equal(payload.logDir, 'C:\\rk-web-installer-logs')

    const { configPath, env } = writeTempConfig(payload.config, tempDir)
    const text = readFileSync(configPath, 'utf8')
    assert.equal(text.includes('ssh-secret'), false)
    assert.equal(JSON.parse(text).ssh.password, '${RK_GUI_SECRET_SSH_PASSWORD}')
    assert.equal(env.RK_GUI_SECRET_SSH_PASSWORD, 'ssh-secret')
  } finally {
    rmSync(tempDir, { recursive: true, force: true })
  }
})

test('remote gui test payload can target harbor registry installs', async () => {
  const { buildRemoteTestPayload } = await import('../scripts/run-gui-remote-test.mjs')

  const payload = buildRemoteTestPayload({
    command: 'install',
    env: {
      RK_TEST_SSH_PASSWORD: 'ssh-secret',
      RK_TEST_K3S_IMAGE_MODE: 'registry'
    }
  })

  assert.equal(payload.form.k3sImageMode, 'registry')
  assert.equal(payload.form.registryProvider, 'harbor')
  assert.equal(payload.form.registryServer, '127.0.0.1:30090')
  assert.equal(payload.form.registryUsername, 'admin')
  assert.equal(payload.form.installHarbor, true)
  assert.equal(payload.form.registryInsecure, true)
  assert.equal(payload.config.registry.provider, 'harbor')
  assert.equal(payload.config.registry.server, '127.0.0.1:30090')
  assert.equal(payload.config.registry.installHarbor, true)
  assert.equal(payload.config.registry.harborVersion, '2.14.4')
})

test('remote gui test payload derives loopback public url from overridden frontend nodeport', async () => {
  const { buildRemoteTestPayload } = await import('../scripts/run-gui-remote-test.mjs')

  const payload = buildRemoteTestPayload({
    command: 'preflight',
    env: {
      RK_TEST_SSH_PASSWORD: 'ssh-secret',
      RK_TEST_FRONTEND_NODE_PORT: '30280',
      RK_TEST_GATEWAY_NODE_PORT: '30210'
    }
  })

  assert.equal(payload.form.publicBaseUrl, 'http://127.0.0.1:30280')
  assert.equal(payload.config.deployment.publicBaseUrl, 'http://127.0.0.1:30280')
  assert.equal(payload.config.k3s.frontendNodePort, 30280)
  assert.equal(payload.config.k3s.gatewayNodePort, 30210)
})

test('remote gui test payload can override harbor installer sources', async () => {
  const { buildRemoteTestPayload } = await import('../scripts/run-gui-remote-test.mjs')

  const payload = buildRemoteTestPayload({
    command: 'install',
    env: {
      RK_TEST_SSH_PASSWORD: 'ssh-secret',
      RK_TEST_K3S_IMAGE_MODE: 'registry',
      RK_TEST_HARBOR_VERSION: '2.14.4',
      RK_TEST_HARBOR_DOWNLOAD_URLS: 'https://mirror.example/harbor.tgz\nhttps://backup.example/harbor.tgz'
    }
  })

  assert.deepEqual(payload.config.registry.harborDownloadUrls, [
    'https://mirror.example/harbor.tgz',
    'https://backup.example/harbor.tgz'
  ])
})

test('remote gui test payload can reuse an existing release without rebuilding images', async () => {
  const { buildRemoteTestPayload } = await import('../scripts/run-gui-remote-test.mjs')

  const payload = buildRemoteTestPayload({
    command: 'install',
    env: {
      RK_TEST_SSH_PASSWORD: 'ssh-secret',
      RK_TEST_K3S_IMAGE_MODE: 'registry',
      RK_TEST_RELEASE_ID: 'rk-web-20260620090240',
      RK_TEST_BUILD_ON_REMOTE: 'false'
    }
  })

  assert.equal(payload.form.releaseId, 'rk-web-20260620090240')
  assert.equal(payload.form.buildOnRemote, false)
  assert.equal(payload.config.releaseId, 'rk-web-20260620090240')
  assert.equal(payload.config.runtime.buildOnRemote, false)
})
