import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

async function readSource(relativePath) {
  return readFile(new URL(relativePath, import.meta.url), 'utf8')
}

test('web email login and identity switch preserve roleId for same-tenant multi-role accounts', async () => {
  const [api, store, login, profile, emailLogin] = await Promise.all([
    readSource('../src/api/user.js'),
    readSource('../src/stores/user.js'),
    readSource('../src/views/Login.vue'),
    readSource('../src/views/Profile.vue'),
    readSource('../src/utils/emailLogin.js')
  ])

  assert.match(api, /export function switchTenant\(tenantId,\s*roleId/)
  assert.match(api, /params:\s*\{[\s\S]*roleId[\s\S]*\}/)

  assert.match(store, /isCurrent:\s*String\(item\.tenantId\)\s*===\s*String\(tenantId\.value\)[\s\S]*Number\(itemRoleId/)
  assert.match(store, /const targetRoleId\s*=/)
  assert.match(store, /switchTenantApi\(targetTenantId,\s*targetRoleId\)/)

  assert.match(login, /const buildCandidateKey = \(candidate\) => `\$\{candidate\.authUserId\}:\$\{candidate\.tenantId\}:\$\{candidate\.roleId \|\| ''\}`/)
  assert.match(login, /roleId:\s*candidate\.roleId/)
  assert.match(emailLogin, /roleName/)
  assert.match(emailLogin, /label:\s*`\$\{displayName\}[\s\S]*\$\{roleName\}/)

  assert.match(profile, /tenantSwitchDialogVisible/)
  assert.match(profile, /handleTenantMembershipSwitch/)
  assert.match(profile, /selectedTenantSwitchKey/)
})
