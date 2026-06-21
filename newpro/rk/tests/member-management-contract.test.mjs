import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs/promises'

const membersViewUrl = new URL('../src/views/admin/club/Members.vue', import.meta.url)

test('member management add flow should use unified user provisioning instead of legacy member apply endpoint', async () => {
  const source = await fs.readFile(membersViewUrl, 'utf8')

  assert.match(source, /createUser/)
  assert.doesNotMatch(source, /submitApplication\s*\(\s*\{/)
})

test('member management should use a unified add edit dialog instead of prompt-only add flow', async () => {
  const source = await fs.readFile(membersViewUrl, 'utf8')

  assert.match(source, /memberDialogVisible/)
  assert.match(source, /memberDialogTitle/)
  assert.match(source, /memberFormRef/)
  assert.match(source, /memberFormRules/)
  assert.match(source, /resetMemberForm/)
  assert.match(source, /submitMemberForm/)
  assert.match(source, /handleAdd\(\)[\s\S]*memberDialogVisible\.value\s*=\s*true/)
  assert.match(source, /handleEdit\(row\)[\s\S]*memberDialogVisible\.value\s*=\s*true/)
  assert.doesNotMatch(source, /ElMessageBox\.prompt/)
  assert.doesNotMatch(source, /inputPattern:\s*\/\^\\d\{6,12\}\\\$\//)
})

test('member management add flow should resolve member role from current tenant instead of hardcoding system role', async () => {
  const source = await fs.readFile(membersViewUrl, 'utf8')

  assert.match(source, /getRoleList/)
  assert.match(source, /resolveDefaultMemberRoleId/)
  assert.match(source, /roleId:\s*await\s+resolveDefaultMemberRoleId\(\)/)
  assert.doesNotMatch(source, /roleId:\s*2\b/)
})
