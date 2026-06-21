import test from 'node:test'
import assert from 'node:assert/strict'
import { access, readFile, stat } from 'node:fs/promises'

const repoRoot = new URL('../../../', import.meta.url)

async function readRepoSource(relativePath) {
  return readFile(new URL(relativePath, repoRoot), 'utf8')
}

test('auth service should define web one-day and mobile fifteen-day sliding session constants', async () => {
  const constants = await readRepoSource('rk-auth/rk-auth-common/src/main/java/com/tianji/auth/common/constants/JwtConstants.java')

  assert.match(constants, /JWT_TOKEN_TTL\s*=\s*Duration\.ofDays\(1\)/)
  assert.match(constants, /JWT_MOBILE_TOKEN_TTL\s*=\s*Duration\.ofDays\(15\)/)
  assert.match(constants, /SESSION_REDIS_KEY_PREFIX\s*=\s*"rk:auth:token:"/)
  assert.match(constants, /CLIENT_TYPE_HEADER\s*=\s*"X-Client-Type"/)
  assert.match(constants, /CLIENT_TYPE_MOBILE\s*=\s*"mobile"/)
})

test('login payload and JWT user payload should carry client type for mobile TTL selection', async () => {
  const loginDto = await readRepoSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/domain/dto/LoginDTO.java')
  const loginUserDto = await readRepoSource('rk-common/src/main/java/com/tianji/common/domain/dto/LoginUserDTO.java')
  const jwtTool = await readRepoSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/util/JwtTool.java')

  assert.match(loginDto, /private String clientType;/)
  assert.match(loginUserDto, /private String clientType;/)
  assert.match(jwtTool, /resolveTokenTtl\(LoginUserDTO userDTO\)/)
  assert.match(jwtTool, /JWT_MOBILE_TOKEN_TTL/)
  assert.match(jwtTool, /CLIENT_TYPE_MOBILE/)
  assert.match(jwtTool, /createToken\(LoginUserDTO userDTO, Duration ttl\)/)
  assert.match(jwtTool, /createRefreshToken[\s\S]*resolveTokenTtl\(userDetail\)/)
})

test('auth login and refresh should store sessions with the same sliding ttl as the issued client token', async () => {
  const service = await readRepoSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/service/impl/AuthServiceImpl.java')
  const accountService = await readRepoSource('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/service/impl/AccountServiceImpl.java')

  assert.equal(service.includes('TOKEN_TTL_MINUTES = 30'), false)
  assert.match(service, /setClientType\(normalizeClientType\(dto\.getClientType\(\)\)\)/)
  assert.match(service, /storeSessionToken\(loginUserDTO, token\)/)
  assert.match(service, /jwtTool\.parseRefreshToken\(normalizeBearerToken\(refreshToken\)\)/)
  assert.match(service, /Duration ttl = jwtTool\.resolveTokenTtl\(loginUserDTO\)/)
  assert.match(service, /ttl\.getSeconds\(\)[\s\S]*TimeUnit\.SECONDS/)

  assert.match(accountService, /setClientType\(JwtConstants\.CLIENT_TYPE_WEB\)/)
  assert.match(accountService, /storeSessionToken\(detail, token\)/)
  assert.match(accountService, /JwtConstants\.SESSION_REDIS_KEY_PREFIX/)
})

test('gateway should reject idle-expired sessions and renew redis ttl on each authenticated request', async () => {
  const authUtil = await readRepoSource('rk-auth/rk-auth-gateway-sdk/src/main/java/com/tianji/authsdk/gateway/util/AuthUtil.java')

  assert.match(authUtil, /renewSlidingSession\(token, userDTO\)/)
  assert.match(authUtil, /SESSION_REDIS_KEY_PREFIX \+ userDTO\.getUserId\(\)/)
  assert.match(authUtil, /JWT_REDIS_KEY_PREFIX \+ userDTO\.getUserId\(\)/)
  assert.match(authUtil, /stringRedisTemplate\.opsForValue\(\)\.get\(sessionKey\)/)
  assert.match(authUtil, /stringRedisTemplate\.expire\(sessionKey, ttl\)/)
  assert.match(authUtil, /stringRedisTemplate\.expire\(jtiKey, ttl\)/)
  assert.match(authUtil, /INVALID_TOKEN/)
})

test('frontend and android clients should identify client type and keep refreshed auth material', async () => {
  const request = await readRepoSource('newpro/rk/src/utils/request.js')
  const userStore = await readRepoSource('newpro/rk/src/stores/user.js')
  const android = await readRepoSource('android-client/rk-club-android/app/src/main/java/com/rkclub/app/MainActivity.java')

  assert.match(userStore, /clientType:\s*'web'/)
  assert.match(request, /refreshToken/)
  assert.match(request, /localStorage\.setItem\('token', refreshedToken\)/)
  assert.match(android, /\\"clientType\\":\\"mobile\\"/)
  assert.match(android, /private String refreshToken = "";/)
  assert.match(android, /connection\.setRequestProperty\("X-Client-Type", "mobile"\)/)
  assert.match(android, /refreshAuthSession\(\)/)
})

test('android APK build should use version 0.0.10 and a stable project keystore', async () => {
  const buildScript = await readRepoSource('android-client/rk-club-android/build-apk.ps1')
  const keystoreUrl = new URL('android-client/rk-club-android/signing/rkclub-release.keystore', repoRoot)

  await access(keystoreUrl)
  const keystore = await stat(keystoreUrl)
  assert.ok(keystore.size > 1024, `expected stable keystore to be committed, got ${keystore.size} bytes`)

  assert.match(buildScript, /\$VersionName\s*=\s*'0\.0\.10'/)
  assert.match(buildScript, /\$VersionCode\s*=\s*'11'/)
  assert.match(buildScript, /\$Keystore\s*=\s*Join-Path \$OriginalRootDir 'signing\\rkclub-release\.keystore'/)
  assert.equal(buildScript.includes("Join-Path $BuildDir 'debug.keystore'"), false)
  assert.equal(buildScript.includes('-genkeypair'), false)
  assert.match(buildScript, /--ks-key-alias', 'rkclub'/)
})
