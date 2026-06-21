function toNumberLike(value) {
  return value ?? 0
}

export function normalizeCreditBreakdown(items = []) {
  return (Array.isArray(items) ? items : []).map((item) => ({
    typeId: item.typeId ?? item.id ?? null,
    code: item.code || item.creditTypeCode || item.type || '',
    name: item.name || item.typeName || item.code || '-',
    description: item.description || '',
    maxCredit: toNumberLike(item.maxCredit),
    totalCredits: toNumberLike(item.totalCredits ?? item.credits),
    totalHours: toNumberLike(item.totalHours ?? item.hours),
    recordCount: item.recordCount ?? item.count ?? 0
  }))
}

export function normalizeCreditRecords(pageData, creditTypes = []) {
  const typeNameMap = new Map(
    (Array.isArray(creditTypes) ? creditTypes : []).map((item) => [item.code, item.name])
  )
  const records = Array.isArray(pageData?.records)
    ? pageData.records
    : Array.isArray(pageData?.list)
      ? pageData.list
      : Array.isArray(pageData)
        ? pageData
        : []
  return {
    records: records.map((row) => {
      const code = row.creditTypeCode || row.creditType || row.type || ''
      return {
        ...row,
        typeName: row.typeName || typeNameMap.get(code) || code || '-',
        sourceName: row.sourceName || row.description || row.source || row.activityName || '-',
        hours: row.creditHours ?? row.hours ?? 0,
        credits: row.creditScore ?? row.credits ?? row.credit ?? 0
      }
    }),
    total: pageData?.total ?? records.length
  }
}
