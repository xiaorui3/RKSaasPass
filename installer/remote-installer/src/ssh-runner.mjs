import { readFileSync } from 'node:fs'
import { Client } from 'ssh2'
import { redactText } from './redaction.mjs'

export class SshRunner {
  constructor(config, logger = console, ClientClass = Client) {
    this.config = config
    this.logger = logger
    this.ClientClass = ClientClass
  }

  connect() {
    const ssh = new this.ClientClass()
    const sshConfig = {
      host: this.config.ssh.host,
      port: this.config.ssh.port,
      username: this.config.ssh.username,
      readyTimeout: (this.config.ssh.connectTimeoutSeconds || 20) * 1000
    }
    if (this.config.ssh.privateKeyPath) {
      sshConfig.privateKey = readFileSync(this.config.ssh.privateKeyPath)
      if (this.config.ssh.passphrase) {
        sshConfig.passphrase = this.config.ssh.passphrase
      }
    } else {
      sshConfig.password = this.config.ssh.password
    }
    return new Promise((resolve, reject) => {
      let settled = false
      ssh.on('error', (error) => {
        if (!settled) {
          settled = true
          reject(error)
          return
        }
        this.logger.error?.(redactText(error.message || String(error)))
      })
      ssh.once('ready', () => {
        settled = true
        resolve(ssh)
      })
      ssh.connect(sshConfig)
    })
  }

  async execScript(script, env = {}) {
    const ssh = await this.connect()
    try {
      return await new Promise((resolve, reject) => {
        const envExports = Object.entries(env)
          .filter(([, value]) => value !== undefined && value !== null && value !== '')
          .map(([key, value]) => `export ${key}=${quoteForShell(value)}`)
          .join('\n')
        const payload = `${envExports ? `${envExports}\n` : ''}${script}`
        const command = 'bash -s'
        ssh.exec(command, (error, stream) => {
          if (error) {
            reject(error)
            return
          }
          let output = ''
          stream.on('close', (code) => {
            if (code === 0) {
              resolve(output)
            } else {
              reject(new Error(`remote command failed with exit ${code}: ${redactText(output)}`))
            }
          })
          stream.on('data', (chunk) => {
            const text = chunk.toString('utf8')
            output += text
            this.logger.log(redactText(text).trimEnd())
          })
          stream.stderr.on('data', (chunk) => {
            const text = chunk.toString('utf8')
            output += text
            this.logger.error(redactText(text).trimEnd())
          })
          stream.end(payload)
        })
      })
    } finally {
      ssh.end()
    }
  }
}

function quoteForShell(value) {
  return `'${String(value).replace(/'/g, `'\\''`)}'`
}
