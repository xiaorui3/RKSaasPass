import { copyFileSync, mkdirSync, readdirSync, rmSync, writeFileSync } from 'node:fs'
import { spawnSync } from 'node:child_process'
import path from 'node:path'

const root = process.cwd()
const buildDir = path.join(root, '.tmp-cli-build')
const output = path.join(root, 'dist', 'rk-web-installer.exe')
const pkgCommand = path.join(root, 'node_modules', '.bin', process.platform === 'win32' ? 'pkg.cmd' : 'pkg')

rmSync(output, { force: true })
rmSync(buildDir, { recursive: true, force: true })
mkdirSync(buildDir, { recursive: true })
copyDirectoryFiles(path.join(root, 'src'), path.join(buildDir, 'src'))
writeFileSync(path.join(buildDir, 'package.json'), JSON.stringify({
  name: 'rk-web-remote-installer-cli',
  version: '0.1.0',
  private: true,
  type: 'module',
  bin: {
    'rk-web-installer': './src/index.mjs'
  },
  dependencies: {
    bcryptjs: '^3.0.3',
    ssh2: '^1.17.0'
  }
}, null, 2), 'utf8')

const result = spawnSync(pkgCommand, [
  buildDir,
  '--sea',
  '--targets',
  'node24-win-x64',
  '--output',
  output
], {
  stdio: 'inherit',
  shell: process.platform === 'win32',
  cwd: root
})

if (result.error) {
  console.error(result.error.message)
}

process.exit(result.status ?? 1)

function copyDirectoryFiles(sourceDir, targetDir) {
  mkdirSync(targetDir, { recursive: true })
  for (const entry of readdirSync(sourceDir, { withFileTypes: true })) {
    const source = path.join(sourceDir, entry.name)
    const target = path.join(targetDir, entry.name)
    if (entry.isDirectory()) {
      copyDirectoryFiles(source, target)
    } else if (entry.isFile()) {
      copyFileSync(source, target)
    }
  }
}
