import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(__dirname, '../../..')

async function readSource(relativePath) {
  return readFile(path.resolve(repoRoot, relativePath), 'utf8')
}

test('menu DTO and management VO expose the same fields used by the permission menu catalog', async () => {
  const dto = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/domain/dto/MenuDTO.java')
  const vo = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/domain/vo/MenuOptionVO.java')
  const menu = await readSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/domain/po/Menu.java')

  for (const field of ['menuCode', 'menuType', 'component', 'visible', 'status']) {
    assert.match(dto, new RegExp(`private\\s+.*\\s+${field};`), `MenuDTO missing ${field}`)
    assert.match(vo, new RegExp(`private\\s+.*\\s+${field};`), `MenuOptionVO missing ${field}`)
    assert.match(menu, new RegExp(`dto\\.get${field[0].toUpperCase()}${field.slice(1)}\\(\\)`), `Menu constructor does not map ${field}`)
    assert.match(menu, new RegExp(`dto\\.set${field[0].toUpperCase()}${field.slice(1)}\\(`), `Menu.toDTO does not map ${field}`)
  }
})
