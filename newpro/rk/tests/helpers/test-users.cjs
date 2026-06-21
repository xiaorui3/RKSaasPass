// @ts-check

const SUPER_ADMIN = { username: 'admin_a', password: 'change-me', organizationId: '1' }
const TENANT1_MANAGER = { username: 'manager_a', password: '123456', organizationId: '1' }
const TENANT1_MEMBER = { username: 'member_a', password: '123456', organizationId: '1' }
const TENANT1_LIVE_TEACHER = { username: 'teacher_a', password: '123456', organizationId: '1' }
const TENANT1_TEACHER = TENANT1_LIVE_TEACHER
const TENANT2_ADMIN = { username: 't02_admin', password: '123456', organizationId: '2' }
const TENANT2_LIVE_TEACHER = { username: 't02_teacher', password: '123456', organizationId: '2' }
const TENANT3_ADMIN = { username: 'admin_c', password: '123456', organizationId: '3' }

module.exports = {
  SUPER_ADMIN,
  TENANT1_MANAGER,
  TENANT1_MEMBER,
  TENANT1_TEACHER,
  TENANT2_ADMIN,
  TENANT1_LIVE_TEACHER,
  TENANT2_LIVE_TEACHER,
  TENANT3_ADMIN
}
