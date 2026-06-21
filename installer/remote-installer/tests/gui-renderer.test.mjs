import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

test('gui renderer uses readable Chinese copy and remote k3s test defaults', () => {
  const html = readFileSync(path.join(repoRoot, 'gui/renderer/index.html'), 'utf8')
  const main = readFileSync(path.join(repoRoot, 'gui/main.mjs'), 'utf8')
  const renderer = readFileSync(path.join(repoRoot, 'gui/renderer/renderer.mjs'), 'utf8')

  assert.match(html, /RK-Web 安装器/)
  assert.match(html, /43\.167\.243\.59/)
  assert.match(html, /30139/)
  assert.match(html, /cloud-master-new/)
  assert.match(html, /rk-web-test/)
  assert.match(html, /logs\/remote-test/)
  assert.match(html, /<option value="registry" selected>registry<\/option>/)
  assert.match(main, /RK-Web 安装器/)
  assert.match(renderer, /运行中/)
  assert.match(renderer, /配置摘要/)
  assert.match(renderer, /sshPassword: data\.sshPassword \? '已填写' : '未填写'/)
  assert.match(renderer, /JSON\.stringify\(summary\)/)
  assert.match(renderer, /GUI 主进程未响应 run 请求/)
  assert.match(renderer, /withTimeout/)
  assert.doesNotMatch(`${html}\n${main}\n${renderer}`, /瀹夎|绌洪|杩愯|鎵ц|闀滃|澶辫|宸插/)
})

test('gui exposes registry provider choices and install expectation summary', () => {
  const html = readFileSync(path.join(repoRoot, 'gui/renderer/index.html'), 'utf8')
  const renderer = readFileSync(path.join(repoRoot, 'gui/renderer/renderer.mjs'), 'utf8')

  assert.match(html, /仓库模式/)
  assert.match(html, /Harbor 私有仓库/)
  assert.match(html, /阿里云 ACR/)
  assert.match(html, /自定义 Registry/)
  assert.match(html, /预计访问/)
  assert.match(html, /预计耗时/)
  assert.match(renderer, /buildInstallExpectation/)
  assert.match(renderer, /validateRegistryForInstall/)
  assert.match(renderer, /LOCAL_LOOPBACK_HOST/)
  assert.match(renderer, /accessNote/)
  assert.match(renderer, /http:\/\/127\.0\.0\.1:30080/)
  assert.doesNotMatch(renderer, /externalRegistry/)
  assert.match(renderer, /40-90 分钟/)
})

test('gui packaged runtime uses a CommonJS preload bridge with IPC health checks', () => {
  const main = readFileSync(path.join(repoRoot, 'gui/main.mjs'), 'utf8')
  const renderer = readFileSync(path.join(repoRoot, 'gui/renderer/renderer.mjs'), 'utf8')
  const preloadPath = path.join(repoRoot, 'gui/preload.cjs')

  assert.equal(existsSync(preloadPath), true)

  const preload = readFileSync(preloadPath, 'utf8')
  assert.match(main, /preload\.cjs/)
  assert.match(main, /requestSingleInstanceLock/)
  assert.match(main, /rk-installer:ping/)
  assert.match(preload, /require\('electron'\)/)
  assert.match(preload, /ping: \(\) => ipcRenderer\.invoke\('rk-installer:ping'\)/)
  assert.match(renderer, /getInstallerBridge/)
  assert.match(renderer, /GUI 桥接未加载/)
  assert.match(renderer, /GUI 主进程未响应 ping 请求/)
  assert.doesNotMatch(preload, /^\s*import\s/m)
})
