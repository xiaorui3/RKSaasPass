export function isSuccessfulResponseCode(code, state) {
  if (String(state || '').toLowerCase() === 'error') {
    return false
  }
  if (code === undefined || code === null || code === '') {
    return true
  }
  const normalized = Number(code)
  return normalized === 200 || normalized === 0
}
