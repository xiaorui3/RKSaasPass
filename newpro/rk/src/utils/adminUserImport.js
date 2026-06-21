function trimString(value, fallback = '') {
  return String(value ?? fallback).trim()
}

function toNumber(value, fallback = 0) {
  const normalized = Number(value)
  return Number.isFinite(normalized) ? normalized : fallback
}

export function normalizeManagedUserImportRows(rows = []) {
  return rows
    .map((row) => ({
      tenantId: toNumber(row.tenantId || row['\u79df\u6237ID'] || row.tenant_id || 0),
      username: trimString(row.username || row['\u7528\u6237\u540d']),
      password: trimString(row.password || row['\u5bc6\u7801'], '123456') || '123456',
      name: trimString(row.name || row['\u59d3\u540d'] || row['\u771f\u5b9e\u59d3\u540d']),
      cellPhone: trimString(row.cellPhone || row['\u624b\u673a\u53f7'] || row.mobile),
      email: trimString(row.email || row['\u90ae\u7bb1']),
      studentId: trimString(row.studentId || row['\u5b66\u53f7']),
      college: trimString(row.college || row['\u5b66\u9662']),
      major: trimString(row.major || row['\u4e13\u4e1a']),
      grade: trimString(row.grade || row['\u5e74\u7ea7']),
      department: trimString(row.department || row['\u90e8\u95e8']),
      position: trimString(row.position || row['\u5c97\u4f4d']),
      type: toNumber(row.type || row['\u7528\u6237\u7c7b\u578b'] || 2, 2),
      roleId: toNumber(row.roleId || row['\u89d2\u8272ID'] || 0),
      status: toNumber(row.status || row['\u72b6\u6001'] || 1, 1),
    }))
    .filter((row) => row.tenantId && row.username && row.roleId)
}

export function validateManagedUserImportRows(rows = []) {
  for (let index = 0; index < rows.length; index += 1) {
    const row = rows[index] || {}
    if (Number(row.type || 2) === 2 && !trimString(row.studentId)) {
      return {
        ok: false,
        message: `\u7b2c ${index + 1} \u884c\u5b66\u751f\u7c7b\u578b\u7f3a\u5c11\u5b66\u53f7\uff0c\u8bf7\u8865\u5168\u540e\u518d\u5bfc\u5165`,
      }
    }
  }
  return {
    ok: true,
    message: '',
  }
}
