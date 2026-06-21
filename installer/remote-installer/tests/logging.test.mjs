import assert from 'node:assert/strict'
import { existsSync, mkdirSync, readdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

test('installer validate writes a local log file and redacts secrets', () => {
  const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
  const cliPath = path.join(repoRoot, 'src/index.mjs')
  const logDir = path.join(os.tmpdir(), `rk-web-installer-logs-${process.pid}`)
  rmSync(logDir, { recursive: true, force: true })

  const result = spawnSync(process.execPath, [
    cliPath,
    'validate',
    '--config',
    path.join(repoRoot, 'examples/rk-remote-install.example.json'),
    '--log-dir',
    logDir
  ], {
    cwd: repoRoot,
    encoding: 'utf8'
  })

  assert.equal(result.status, 0, result.stderr || result.stdout)
  assert.match(result.stdout, /local log file:/)
  assert.ok(existsSync(logDir), 'expected log directory to be created')

  const files = readLogFiles(logDir)
  assert.equal(files.length, 1, `expected exactly one log file, got ${files.length}`)
  const logText = readFileSync(files[0], 'utf8')
  assert.match(logText, /configuration valid/)
  assert.doesNotMatch(logText, /<CHANGE_ME>|REAL_TEST_MACHINE_PASSWORD/)
})

test('installer command failures are written to the local log file', () => {
  const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
  const cliPath = path.join(repoRoot, 'src/index.mjs')
  const logDir = path.join(os.tmpdir(), `rk-web-installer-error-logs-${process.pid}`)
  const configPath = path.join(logDir, 'missing-ssh-secret.json')
  rmSync(logDir, { recursive: true, force: true })
  mkdirSync(logDir, { recursive: true })
  writeFileSync(configPath, JSON.stringify({
    ssh: { host: '203.0.113.10', port: 30139, username: 'root' },
    git: { provider: 'gitee', repositoryUrl: 'https://gitee.com/example/rk-web.git', branch: 'cloud-master-new' },
    runtime: { mode: 'k3s' }
  }, null, 2), 'utf8')

  const result = spawnSync(process.execPath, [
    cliPath,
    'preflight',
    '--config',
    configPath,
    '--log-dir',
    logDir
  ], {
    cwd: repoRoot,
    encoding: 'utf8'
  })

  assert.equal(result.status, 1)
  assert.match(result.stderr, /ssh\.password or ssh\.privateKeyPath is required/)

  const files = readLogFiles(logDir)
  assert.equal(files.length, 1, `expected exactly one log file, got ${files.length}`)
  const logText = readFileSync(files[0], 'utf8')
  assert.match(logText, /ssh\.password or ssh\.privateKeyPath is required/)
})

function readLogFiles(directory) {
  return readdirSync(directory)
    .filter((name) => name.endsWith('.log'))
    .map((name) => path.join(directory, name))
}
