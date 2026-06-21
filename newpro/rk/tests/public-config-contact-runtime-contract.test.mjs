import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

async function readSource(relativePath) {
  return readFile(new URL(relativePath, import.meta.url), 'utf8')
}

test('gateway and resource auth should expose public contact shield without login', async () => {
  const [gatewayProps, gatewayYaml, messageYaml] = await Promise.all([
    readSource('../../../rk-gateway/src/main/java/com/tianji/gateway/config/AuthProperties.java'),
    readSource('../../../rk-gateway/src/main/resources/bootstrap.yml'),
    readSource('../../../rk-message/rk-message-service/src/main/resources/bootstrap.yml')
  ])

  assert.match(gatewayProps, /GET:\/api\/contact\/public\/shield/)
  assert.match(gatewayYaml, /GET:\/api\/contact\/public\/shield/)
  assert.match(messageYaml, /GET:\/api\/contact\/public\/shield/)
})

test('rk-user should provide api-prefixed system config compatibility endpoint', async () => {
  const controller = await readSource('../../../rk-user/src/main/java/com/tianji/user/controller/ConfigController.java')

  assert.match(controller, /@RequestMapping\(\{\"\/config\",\s*\"\/api\/config\"\}\)/)
  assert.match(controller, /@GetMapping\(\"\/system\"\)/)
})
