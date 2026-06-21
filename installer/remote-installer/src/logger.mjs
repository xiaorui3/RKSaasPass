import { createWriteStream, mkdirSync } from 'node:fs'
import path from 'node:path'
import { redactText } from './redaction.mjs'

export function createInstallerLogger(options = {}) {
  const command = sanitizeName(options.command || 'wizard')
  const logFile = resolveLogFile(command, options)
  mkdirSync(path.dirname(logFile), { recursive: true })
  const stream = createWriteStream(logFile, { flags: 'a', encoding: 'utf8' })

  const write = (level, message) => {
    const lines = String(message).split(/\r?\n/)
    for (const line of lines) {
      if (line === '') continue
      stream.write(`${new Date().toISOString()} ${level} ${line}\n`)
    }
  }

  return {
    logFile,
    log(message) {
      const redacted = redactText(message)
      console.log(redacted)
      write('INFO', redacted)
    },
    error(message) {
      const redacted = redactText(message)
      console.error(redacted)
      write('ERROR', redacted)
    },
    file(message, level = 'INFO') {
      write(level, redactText(message))
    },
    close() {
      return new Promise((resolve) => stream.end(resolve))
    }
  }
}

function resolveLogFile(command, options) {
  if (options.logFile) {
    return path.resolve(String(options.logFile))
  }
  const logDir = options.logDir
    ? path.resolve(String(options.logDir))
    : path.join(process.cwd(), 'logs')
  return path.join(logDir, `rk-web-installer-${command}-${timestampForFile()}.log`)
}

function timestampForFile(date = new Date()) {
  return date.toISOString().replace(/[:.]/g, '-')
}

function sanitizeName(value) {
  return String(value || 'installer').replace(/[^a-z0-9_-]+/gi, '-').replace(/^-+|-+$/g, '') || 'installer'
}
