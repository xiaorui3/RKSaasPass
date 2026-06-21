import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const MOJIBAKE_MARKERS = [
  [0x9366, 0x3126, 0x724e],
  [0x5bb8, 0x53c9, 0x762f, 0x6d93],
  [0x7ec9, 0x71b8, 0x57db],
  [0x7490, 0xfe40, 0x5f7f],
  [0x9422, 0x3126, 0x57db],
  [0x93cd, 0x2033, 0x5f38],
  [0x93c8, 0xe046, 0x7161],
  [0x8930, 0x64b3, 0x58a0],
  [0x9350, 0x546e, 0x5e39, 0x942e]
].map((codes) => String.fromCodePoint(...codes))

const FILES = [
  '../src/utils/adminPeopleDomain.js',
  '../src/views/admin/Dashboard.vue',
  '../src/views/admin/operation/Jenkins.vue',
  '../src/views/admin/operation/K8s.vue',
  '../src/views/admin/operation/NacosConfig.vue',
  '../src/views/admin/operation/Topology.vue',
  '../src/views/admin/system/Users.vue',
  '../../../rk-user/src/main/java/com/tianji/user/config/AdminOpsSchemaUpdater.java',
  '../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java',
  '../../../rk-user/src/main/java/com/tianji/user/controller/UserController.java',
  '../../../rk-user/src/main/java/com/tianji/user/service/impl/ClubAlumniServiceImpl.java',
  '../../../rk-user/src/main/java/com/tianji/user/service/impl/ReferralCodeServiceImpl.java',
  '../../../rk-message/rk-message-service/src/main/java/com/tianji/message/service/impl/ContactUsServiceImpl.java'
]

for (const relativePath of FILES) {
  test(`source file should not contain mojibake markers: ${relativePath}`, () => {
    const source = readFileSync(new URL(relativePath, import.meta.url), 'utf8')
    for (const marker of MOJIBAKE_MARKERS) {
      assert.doesNotMatch(
        source,
        new RegExp(marker),
        `${relativePath} still contains mojibake marker "${marker}"`,
      )
    }
  })
}
