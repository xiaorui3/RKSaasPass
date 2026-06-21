import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const loginVoUrl = new URL('../../../rk-auth/rk-auth-service/src/main/java/com/tianji/auth/domain/vo/LoginVO.java', import.meta.url)
const authServiceUrl = new URL('../../../rk-auth/rk-auth-service/src/main/java/com/tianji/auth/service/impl/AuthServiceImpl.java', import.meta.url)
const userServiceUrl = new URL('../../../rk-user/src/main/java/com/tianji/user/service/impl/UserServiceImpl.java', import.meta.url)

test('auth login response should expose the current user avatar for mobile session restore', async () => {
  const [loginVo, authService, userService] = await Promise.all([
    readFile(loginVoUrl, 'utf8'),
    readFile(authServiceUrl, 'utf8'),
    readFile(userServiceUrl, 'utf8')
  ])

  assert.match(loginVo, /private\s+String\s+avatar\s*;/)
  assert.match(loginVo, /private\s+String\s+avatarUrl\s*;/)
  assert.match(loginVo, /private\s+String\s+icon\s*;/)

  assert.match(userService, /userDTO\.setIcon\(user\.getAvatar\(\)\)/)
  assert.match(authService, /queryUsersByAuthIds\(/)
  assert.match(authService, /resolveLoginAvatar\(/)
  assert.match(authService, /loginVO\.setAvatar\(/)
  assert.match(authService, /loginVO\.setAvatarUrl\(/)
  assert.match(authService, /loginVO\.setIcon\(/)
})
