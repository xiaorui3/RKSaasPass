const { contextBridge, ipcRenderer } = require('electron')

contextBridge.exposeInMainWorld('rkInstaller', {
  ping: () => ipcRenderer.invoke('rk-installer:ping'),
  selectDirectory: () => ipcRenderer.invoke('rk-installer:select-directory'),
  selectFile: () => ipcRenderer.invoke('rk-installer:select-file'),
  run: (payload) => ipcRenderer.invoke('rk-installer:run', payload),
  stop: () => ipcRenderer.invoke('rk-installer:stop'),
  onOutput: (callback) => {
    const handler = (_event, payload) => callback(payload)
    ipcRenderer.on('rk-installer:output', handler)
    return () => ipcRenderer.removeListener('rk-installer:output', handler)
  },
  onStatus: (callback) => {
    const handler = (_event, payload) => callback(payload)
    ipcRenderer.on('rk-installer:status', handler)
    return () => ipcRenderer.removeListener('rk-installer:status', handler)
  }
})
