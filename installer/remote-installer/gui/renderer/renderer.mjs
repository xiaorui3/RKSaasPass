const form = document.querySelector('#installForm')
const logOutput = document.querySelector('#logOutput')
const statusBadge = document.querySelector('#statusBadge')
const currentTask = document.querySelector('#currentTask')
const configPath = document.querySelector('#configPath')
const runtimeMode = document.querySelector('#runtimeMode')
const sshAuthMode = document.querySelector('#sshAuthMode')
const k3sImageMode = document.querySelector('#k3sImageMode')
const registryProvider = document.querySelector('#registryProvider')
const publicBaseUrl = document.querySelector('#publicBaseUrl')
const LOCAL_LOOPBACK_HOST = '127.0.0.1'
const DEFAULT_PUBLIC_BASE_URL = 'http://127.0.0.1:30080'

const FIELD_LABELS = {
  sshHost: '主机地址',
  sshPort: '端口',
  sshUsername: '用户名',
  sshPassword: 'SSH 密码',
  sshPrivateKeyPath: '私钥路径',
  repositoryUrl: '仓库地址',
  gitBranch: '分支',
  targetPath: '目标目录',
  publicBaseUrl: '访问地址',
  registryServer: 'Registry Server',
  registryNamespace: 'Namespace',
  registryUsername: '仓库用户名',
  registryPassword: '仓库密码',
  harborHttpPort: 'Harbor HTTP 端口'
}

document.querySelectorAll('[data-command]').forEach((button) => {
  button.addEventListener('click', () => runCommand(button.dataset.command))
})

document.querySelector('#pickLogDir').addEventListener('click', async () => {
  const bridge = getInstallerBridge()
  if (!bridge) return
  const directory = await bridge.selectDirectory()
  if (directory) document.querySelector('#logDir').value = directory
})

document.querySelectorAll('[data-pick-file]').forEach((button) => {
  button.addEventListener('click', async () => {
    const bridge = getInstallerBridge()
    if (!bridge) return
    const file = await bridge.selectFile()
    if (file) form.elements[button.dataset.pickFile].value = file
  })
})

document.querySelector('#stopBtn').addEventListener('click', async () => {
  const bridge = getInstallerBridge()
  if (!bridge) return
  try {
    await withTimeout(bridge.stop(), 5000, 'GUI 主进程未响应 stop 请求')
  } catch (error) {
    appendLog(`[gui] ${error.message || String(error)}\n`)
  }
})

document.querySelector('#clearLog').addEventListener('click', () => {
  logOutput.textContent = ''
})

document.querySelector('#copyLog').addEventListener('click', async () => {
  await navigator.clipboard.writeText(logOutput.textContent)
})

runtimeMode.addEventListener('change', updateModeVisibility)
sshAuthMode.addEventListener('change', updateAuthVisibility)
k3sImageMode.addEventListener('change', () => {
  updateRegistryVisibility()
  updateInstallExpectation()
})
registryProvider.addEventListener('change', () => {
  applyRegistryDefaults()
  updateRegistryVisibility()
  updateInstallExpectation()
})
document.querySelector('#harborHttpPort').addEventListener('input', () => {
  applyRegistryDefaults({ preserveProviderValues: true })
  updateInstallExpectation()
})
document.querySelector('[name="sshHost"]').addEventListener('input', () => {
  updateDefaultPublicBaseUrl()
  applyRegistryDefaults({ preserveProviderValues: true })
  updateInstallExpectation()
})
publicBaseUrl.addEventListener('input', updateInstallExpectation)
document.querySelector('[name="frontendNodePort"]').addEventListener('input', updateInstallExpectation)
document.querySelector('[name="gatewayNodePort"]').addEventListener('input', updateInstallExpectation)
updateModeVisibility()
updateAuthVisibility()
updateDefaultPublicBaseUrl()
updateRegistryVisibility()
updateInstallExpectation()

const installerBridge = getInstallerBridge({ quiet: true })
if (installerBridge) {
  installerBridge.onOutput(({ stream, text }) => {
    appendLog(stream === 'stderr' ? `[stderr] ${text}` : text)
  })

  installerBridge.onStatus((status) => {
    setStatus(status)
  })
} else {
  appendLog('[gui] GUI 桥接未加载，请关闭旧窗口后重新打开最新安装器。\n')
}

async function runCommand(command) {
  const bridge = getInstallerBridge()
  if (!bridge) return
  if (!validateForCommand(command)) return
  const data = readForm()
  setButtonsEnabled(false)
  appendConfigSummary(data)
  appendLog(`\n> ${command} started\n`)
  if (command === 'install') {
    const expectation = buildInstallExpectation(data)
    appendLog(`[gui] 安装摘要：${JSON.stringify(expectation)}\n`)
    if (!confirmInstallExpectation(expectation)) {
      appendLog('[gui] 用户取消安装\n')
      setButtonsEnabled(true)
      return
    }
  }
  let result
  try {
    await withTimeout(bridge.ping(), 5000, 'GUI 主进程未响应 ping 请求')
    result = await withTimeout(bridge.run({
      command,
      form: data,
      logDir: document.querySelector('#logDir').value.trim()
    }), 5000, 'GUI 主进程未响应 run 请求')
  } catch (error) {
    appendLog(`[gui] ${error.message || String(error)}\n`)
    setButtonsEnabled(true)
    return
  }
  if (!result.ok) {
    appendLog(`[gui] ${result.message}\n`)
    setButtonsEnabled(true)
  }
}

function getInstallerBridge(options = {}) {
  if (window.rkInstaller) return window.rkInstaller
  if (!options.quiet) {
    appendLog('[gui] GUI 桥接未加载，请关闭旧窗口后重新打开最新安装器。\n')
  }
  return null
}

function withTimeout(promise, timeoutMs, message) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error(message)), timeoutMs)
    promise.then(
      (value) => {
        clearTimeout(timer)
        resolve(value)
      },
      (error) => {
        clearTimeout(timer)
        reject(error)
      }
    )
  })
}

function appendConfigSummary(data) {
  const expectation = buildInstallExpectation(data)
  const summary = {
    host: `${data.sshHost}:${data.sshPort}`,
    user: data.sshUsername,
    sshPassword: data.sshPassword ? '已填写' : '未填写',
    runtime: data.runtimeMode,
    imageMode: data.k3sImageMode || '-',
    registryProvider: data.registryProvider || '-',
    repo: data.repositoryUrl,
    branch: data.gitBranch,
    namespace: data.k3sNamespace || '-',
    logDir: document.querySelector('#logDir').value.trim() || 'logs',
    frontendUrl: expectation.frontendUrl,
    estimatedTime: expectation.estimatedTime
  }
  appendLog(`[gui] 配置摘要：${JSON.stringify(summary)}\n`)
}

function validateForCommand(command) {
  const data = readForm()
  const missing = []
  for (const name of ['sshHost', 'sshPort', 'sshUsername', 'repositoryUrl', 'gitBranch', 'targetPath', 'publicBaseUrl']) {
    if (!String(data[name] || '').trim()) missing.push(FIELD_LABELS[name] || name)
  }
  if (command !== 'validate') {
    if (data.sshAuthMode === 'key') {
      if (!String(data.sshPrivateKeyPath || '').trim()) missing.push(FIELD_LABELS.sshPrivateKeyPath)
    } else if (!String(data.sshPassword || '').trim()) {
      missing.push(FIELD_LABELS.sshPassword)
    }
  }
  if (command === 'install') {
    validateRegistryForInstall(data, missing)
  }
  if (missing.length > 0) {
    appendLog(`[gui] 请填写：${missing.join('、')}\n`)
    return false
  }
  if (command === 'install' && !form.reportValidity()) {
    return false
  }
  return true
}

function validateRegistryForInstall(data, missing) {
  if (data.runtimeMode !== 'k3s' || data.k3sImageMode !== 'registry') return
  const provider = data.registryProvider || 'harbor'
  if (provider === 'harbor') {
    for (const name of ['registryServer', 'registryNamespace', 'registryUsername', 'registryPassword', 'harborHttpPort']) {
      if (!String(data[name] || '').trim()) missing.push(FIELD_LABELS[name] || name)
    }
    return
  }
  for (const name of ['registryServer', 'registryNamespace', 'registryUsername', 'registryPassword']) {
    if (!String(data[name] || '').trim()) missing.push(FIELD_LABELS[name] || name)
  }
}

function readForm() {
  const data = {}
  for (const element of form.elements) {
    if (!element.name) continue
    if (element.type === 'checkbox') {
      data[element.name] = element.checked
    } else {
      data[element.name] = element.value
    }
  }
  return data
}

function appendLog(text) {
  logOutput.textContent += text
  logOutput.scrollTop = logOutput.scrollHeight
}

function setStatus(status) {
  statusBadge.className = `status ${status.state || ''}`
  if (status.state === 'running') {
    statusBadge.textContent = '运行中'
    currentTask.textContent = status.command || '任务'
    setButtonsEnabled(false)
    return
  }
  if (status.state === 'success') {
    statusBadge.textContent = '完成'
    currentTask.textContent = `${status.command} 成功`
    configPath.textContent = status.configPath || '未返回'
    appendLog(`\n[gui] ${status.command} exited with code ${status.code}\n`)
    setButtonsEnabled(true)
    return
  }
  if (status.state === 'failed') {
    statusBadge.textContent = '失败'
    currentTask.textContent = `${status.command} 失败`
    configPath.textContent = status.configPath || '未返回'
    appendLog(`\n[gui] ${status.command} exited with code ${status.code}\n`)
    setButtonsEnabled(true)
    return
  }
  if (status.state === 'stopped') {
    statusBadge.textContent = '已停止'
    currentTask.textContent = '已停止'
    appendLog('\n[gui] task stopped\n')
    setButtonsEnabled(true)
    return
  }
  statusBadge.textContent = '空闲'
}

function setButtonsEnabled(enabled) {
  document.querySelectorAll('[data-command]').forEach((button) => {
    button.disabled = !enabled
  })
}

function updateModeVisibility() {
  document.querySelectorAll('.k3s-only').forEach((item) => {
    item.classList.toggle('hidden', runtimeMode.value !== 'k3s')
  })
  updateRegistryVisibility()
  updateInstallExpectation()
}

function updateAuthVisibility() {
  const keyMode = sshAuthMode.value === 'key'
  document.querySelectorAll('.key-field').forEach((item) => item.classList.toggle('hidden', !keyMode))
  document.querySelectorAll('.password-field').forEach((item) => item.classList.toggle('hidden', keyMode))
}

function updateRegistryVisibility() {
  const registryMode = runtimeMode.value === 'k3s' && k3sImageMode.value === 'registry'
  document.querySelector('.registry-section').classList.toggle('hidden', !registryMode)
  const harborMode = registryMode && registryProvider.value === 'harbor'
  document.querySelectorAll('.harbor-field').forEach((item) => item.classList.toggle('hidden', !harborMode))
}

function updateDefaultPublicBaseUrl() {
  if (publicBaseUrl.dataset.touched === 'true') return
  const port = document.querySelector('[name="frontendNodePort"]').value.trim() || '30080'
  publicBaseUrl.value = port === '30080' ? DEFAULT_PUBLIC_BASE_URL : `http://127.0.0.1:${port}`
}

publicBaseUrl.addEventListener('input', () => {
  publicBaseUrl.dataset.touched = 'true'
})

function applyRegistryDefaults(options = {}) {
  const provider = registryProvider.value
  const harborPort = document.querySelector('#harborHttpPort').value.trim() || '30090'
  const server = document.querySelector('#registryServer')
  const username = document.querySelector('#registryUsername')
  const password = document.querySelector('#registryPassword')
  if (provider === 'harbor') {
    server.value = `127.0.0.1:${harborPort}`
    if (!options.preserveProviderValues || !username.value.trim()) username.value = 'admin'
    if (!options.preserveProviderValues || !password.value.trim()) password.value = 'Harbor12345'
    return
  }
  if (!options.preserveProviderValues) {
    server.value = provider === 'aliyun' ? 'registry.example.com' : 'registry.example.invalid'
    username.value = ''
    password.value = ''
  }
}

function buildInstallExpectation(data = readForm()) {
  const frontendPort = data.frontendNodePort || '30080'
  const gatewayPort = data.gatewayNodePort || '30010'
  const frontendUrl = data.publicBaseUrl || `http://${LOCAL_LOOPBACK_HOST}:${frontendPort}`
  const gatewayUrl = `http://${LOCAL_LOOPBACK_HOST}:${gatewayPort}`
  const accessNote = 'local-loopback (SSH only): 这些地址在目标服务器本机可访问；公网只需要 SSH 端口'
  let registry = 'local-tar 本地导入'
  let estimatedTime = '30-70 分钟'
  if (data.runtimeMode === 'k3s' && data.k3sImageMode === 'registry') {
    if (data.registryProvider === 'harbor') {
      const internalRegistry = data.registryServer || '127.0.0.1:30090'
      registry = `Harbor http://${internalRegistry}`
      estimatedTime = '40-90 分钟'
    } else if (data.registryProvider === 'aliyun') {
      registry = `阿里云 ACR ${data.registryServer || 'registry.example.com'}`
      estimatedTime = '30-80 分钟'
    } else {
      registry = `自定义 Registry ${data.registryServer || 'registry.example.invalid'}`
      estimatedTime = '30-80 分钟'
    }
  }
  registry += '；仅在目标服务器本机或 SSH 隧道内访问'
  return { frontendUrl, gatewayUrl, registry, accessNote, estimatedTime }
}

function updateInstallExpectation() {
  const expectation = buildInstallExpectation()
  document.querySelector('#expectedFrontendUrl').textContent = expectation.frontendUrl
  document.querySelector('#expectedGatewayUrl').textContent = expectation.gatewayUrl
  document.querySelector('#expectedRegistryUrl').textContent = expectation.registry
  document.querySelector('#expectedAccessNote').textContent = expectation.accessNote
  document.querySelector('#expectedInstallTime').textContent = expectation.estimatedTime
}

function confirmInstallExpectation(expectation) {
  return window.confirm([
    '确认开始安装？',
    `前端地址：${expectation.frontendUrl}`,
    `网关地址：${expectation.gatewayUrl}`,
    `镜像仓库：${expectation.registry}`,
    `访问模式：${expectation.accessNote}`,
    `预计耗时：${expectation.estimatedTime}`
  ].join('\n'))
}
