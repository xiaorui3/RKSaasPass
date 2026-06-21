// @ts-check
const path = require('path')
const { spawn } = require('child_process')

const REMOTE_SSH_SCRIPT = path.resolve(__dirname, '..', '..', '..', '..', 'remote_ssh.py')
const PYTHON_BIN = process.env.PLAYWRIGHT_REMOTE_PYTHON || 'python'
const MYSQL_CONTAINER = process.env.PLAYWRIGHT_MYSQL_CONTAINER || 'mysql'
const MYSQL_USER = process.env.PLAYWRIGHT_MYSQL_USER || 'root'
const MYSQL_PASSWORD = process.env.PLAYWRIGHT_MYSQL_PASSWORD || '123'

function shellSingleQuote(value) {
  return `'${String(value).replace(/'/g, `'\"'\"'`)}'`
}

function validateDatabase(database) {
  if (!database) return ''
  if (!/^[A-Za-z0-9_]+$/.test(database)) {
    throw new Error(`Unsupported database name: ${database}`)
  }
  return ` -D ${database}`
}

function execRemote(command) {
  return new Promise((resolve, reject) => {
    const args = ['-X', 'utf8', REMOTE_SSH_SCRIPT, command]
    const child = spawn(PYTHON_BIN, args, {
      cwd: path.resolve(__dirname, '..', '..', '..', '..')
    })

    let stdout = ''
    let stderr = ''

    child.stdout.on('data', (chunk) => {
      stdout += chunk.toString()
    })
    child.stderr.on('data', (chunk) => {
      stderr += chunk.toString()
    })
    child.on('error', reject)
    child.on('close', (code) => {
      if (code === 0) {
        resolve({ stdout, stderr })
        return
      }
      reject(new Error(`remote_ssh failed with code ${code}: ${stderr || stdout}`))
    })
  })
}

async function execRemoteMysql(sql, options = {}) {
  const databaseFlag = validateDatabase(options.database)
  const remoteCommand = [
    `docker exec -i ${MYSQL_CONTAINER}`,
    'mysql',
    `-u${MYSQL_USER}`,
    `-p${MYSQL_PASSWORD}`,
    '--batch',
    '--raw',
    '--skip-column-names',
    databaseFlag.trim(),
    '-e',
    shellSingleQuote(sql)
  ].filter(Boolean).join(' ')

  return execRemote(remoteCommand)
}

module.exports = {
  execRemoteMysql
}
