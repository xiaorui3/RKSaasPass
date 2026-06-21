import test from 'node:test'
import assert from 'node:assert/strict'
import {
  normalizeManagedUserImportRows,
  validateManagedUserImportRows,
} from '../src/utils/adminUserImport.js'

test('normalizeManagedUserImportRows normalizes excel rows into managed-user payloads', () => {
  const rows = normalizeManagedUserImportRows([
    {
      tenantId: '2',
      username: ' import_user ',
      password: '',
      name: ' 导入用户 ',
      cellPhone: '13800138011',
      email: ' import@example.com ',
      studentId: ' 2026001001 ',
      college: 'CS',
      major: 'Software',
      grade: '2026',
      department: '宣传部',
      position: '成员',
      type: '2',
      roleId: '3',
      status: '1',
    },
  ])

  assert.deepEqual(rows, [
    {
      tenantId: 2,
      username: 'import_user',
      password: '123456',
      name: '导入用户',
      cellPhone: '13800138011',
      email: 'import@example.com',
      studentId: '2026001001',
      college: 'CS',
      major: 'Software',
      grade: '2026',
      department: '宣传部',
      position: '成员',
      type: 2,
      roleId: 3,
      status: 1,
    },
  ])
})

test('validateManagedUserImportRows rejects student rows without studentId', () => {
  const result = validateManagedUserImportRows([
    {
      tenantId: 2,
      username: 'student_without_id',
      password: 'Passw0rd!',
      name: 'No Id',
      cellPhone: '13800138012',
      email: 'no-id@example.com',
      studentId: '   ',
      type: 2,
      roleId: 2,
      status: 1,
    },
  ])

  assert.equal(result.ok, false)
  assert.match(result.message, /第 1 行/)
  assert.match(result.message, /学号/)
})

test('validateManagedUserImportRows accepts non-student rows without studentId', () => {
  const result = validateManagedUserImportRows([
    {
      tenantId: 2,
      username: 'teacher_import',
      password: 'Passw0rd!',
      name: 'Teacher',
      cellPhone: '13800138013',
      email: 'teacher@example.com',
      studentId: '',
      type: 3,
      roleId: 4,
      status: 1,
    },
  ])

  assert.deepEqual(result, { ok: true, message: '' })
})
