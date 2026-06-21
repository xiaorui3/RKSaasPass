import { app, BrowserWindow, dialog, ipcMain } from 'electron'
import { appendFileSync, mkdirSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { runInstallerCommand } from './installer-runner.mjs'

const guiRoot = path.dirname(fileURLToPath(import.meta.url))
const packageRoot = path.resolve(guiRoot, '..')
let mainWindow
let activeProcess

const gotSingleInstanceLock = app.requestSingleInstanceLock()
if (!gotSingleInstanceLock) {
  app.quit()
}

async function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 860,
    minWidth: 1040,
    minHeight: 720,
    title: 'RK-Web 安装器',
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(guiRoot, 'preload.cjs'),
      contextIsolation: true,
      nodeIntegration: false
    }
  })

  await mainWindow.loadFile(path.join(guiRoot, 'renderer', 'index.html'))
}

app.whenReady().then(async () => {
  logGuiEvent(`app ready: execPath=${process.execPath} resourcesPath=${process.resourcesPath || ''}`)
  registerIpc()
  await createWindow()
  logGuiEvent('window loaded')
})

app.on('window-all-closed', () => {
  if (activeProcess) activeProcess.kill()
  if (process.platform !== 'darwin') app.quit()
})

app.on('activate', async () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    await createWindow()
  }
})

app.on('second-instance', () => {
  if (!mainWindow) return
  if (mainWindow.isMinimized()) mainWindow.restore()
  mainWindow.focus()
})

function registerIpc() {
  ipcMain.handle('rk-installer:ping', async () => {
    logGuiEvent('ping received')
    return {
      ok: true,
      pid: process.pid,
      resourcesPath: process.resourcesPath || '',
      portableDir: process.env.PORTABLE_EXECUTABLE_DIR || ''
    }
  })

  ipcMain.handle('rk-installer:select-directory', async () => {
    const result = await dialog.showOpenDialog(mainWindow, { properties: ['openDirectory', 'createDirectory'] })
    return result.canceled ? '' : result.filePaths[0]
  })

  ipcMain.handle('rk-installer:select-file', async () => {
    const result = await dialog.showOpenDialog(mainWindow, { properties: ['openFile'] })
    return result.canceled ? '' : result.filePaths[0]
  })

  ipcMain.handle('rk-installer:run', async (_event, payload) => {
    if (activeProcess) {
      logGuiEvent('run rejected: active process exists')
      return { ok: false, message: '已有任务正在运行' }
    }

    const command = payload?.command || 'validate'
    const form = payload?.form || {}
    const summary = buildSafeSummary(form, payload?.logDir || payload?.logFile || '')
    logGuiEvent(`run received: ${command} ${JSON.stringify(summary)}`)
    mainWindow.webContents.send('rk-installer:output', {
      stream: 'stdout',
      text: `[gui-main] received ${command}: ${JSON.stringify(summary)}\n`
    })
    mainWindow.webContents.send('rk-installer:status', { state: 'running', command })
    activeProcess = runInstallerCommand({
      command,
      form,
      logDir: payload?.logDir || '',
      logFile: payload?.logFile || '',
      onOutput: (item) => {
        logGuiEvent(`cli ${item.stream}: ${String(item.text || '').trimEnd()}`)
        mainWindow.webContents.send('rk-installer:output', item)
      },
      onExit: ({ code, configPath }) => {
        logGuiEvent(`cli exit: command=${command} code=${code} configPath=${configPath || ''}`)
        mainWindow.webContents.send('rk-installer:status', {
          state: code === 0 ? 'success' : 'failed',
          command,
          code,
          configPath
        })
        activeProcess = null
      }
    })
    if (!activeProcess) {
      logGuiEvent(`run failed before child process was created: ${command}`)
      return { ok: false, message: '安装器进程启动失败，已写入 GUI 主进程日志' }
    }
    logGuiEvent(`child started: ${command}`)
    return { ok: true }
  })

  ipcMain.handle('rk-installer:stop', async () => {
    if (!activeProcess) return { ok: true }
    logGuiEvent('stop requested')
    activeProcess.kill()
    activeProcess = null
    mainWindow.webContents.send('rk-installer:status', { state: 'stopped' })
    return { ok: true }
  })
}

function buildSafeSummary(form, logTarget) {
  return {
    host: `${form.sshHost || ''}:${form.sshPort || ''}`,
    user: form.sshUsername || '',
    sshPassword: form.sshPassword ? 'set' : 'empty',
    runtime: form.runtimeMode || '',
    repo: form.repositoryUrl || '',
    branch: form.gitBranch || '',
    namespace: form.k3sNamespace || '',
    logTarget: logTarget || ''
  }
}

function logGuiEvent(message) {
  try {
    const logDir = resolveGuiLogDir()
    mkdirSync(logDir, { recursive: true })
    appendFileSync(path.join(logDir, 'gui-main.log'), `${new Date().toISOString()} ${message}\n`, 'utf8')
  } catch {
    // GUI diagnostics must never break installer execution.
  }
}

function resolveGuiLogDir() {
  if (process.resourcesPath) {
    return path.join(process.env.PORTABLE_EXECUTABLE_DIR || path.dirname(process.execPath), 'logs')
  }
  return path.join(packageRoot, 'logs')
}
