import test from 'node:test'
import assert from 'node:assert/strict'

const financeModuleUrl = new URL('../src/utils/adminFinance.js', import.meta.url)
const emailModuleUrl = new URL('../src/utils/emailMedia.js', import.meta.url)

test('admin finance helpers should map ui types and backend types consistently', async () => {
  const finance = await import(`${financeModuleUrl.href}?case=type-mapping`)

  assert.equal(finance.toFinanceApiType('income'), 1)
  assert.equal(finance.toFinanceApiType('expense'), 2)
  assert.equal(finance.fromFinanceApiType(1), 'income')
  assert.equal(finance.fromFinanceApiType(2), 'expense')
  assert.equal(finance.fromFinanceApiType('income'), 'income')
})

test('admin finance helpers should normalize backend records for table rendering', async () => {
  const finance = await import(`${financeModuleUrl.href}?case=record-normalize`)

  assert.deepEqual(
    finance.normalizeFinanceRecord({
      id: 9,
      type: 1,
      amount: 88.5,
      title: 'demo',
    }),
    {
      id: 9,
      type: 'income',
      amount: 88.5,
      title: 'demo',
    },
  )
})

test('email media helpers should extract upload url from managed upload responses', async () => {
  const emailMedia = await import(`${emailModuleUrl.href}?case=upload-url`)

  assert.equal(emailMedia.extractUploadedFileUrl('https://cdn.example.com/demo.png'), 'https://cdn.example.com/demo.png')
  assert.equal(emailMedia.extractUploadedFileUrl({ url: 'https://cdn.example.com/demo.png' }), 'https://cdn.example.com/demo.png')
  assert.equal(emailMedia.extractUploadedFileUrl({ storedValue: 'rk-user/email-image/demo.png' }), 'rk-user/email-image/demo.png')
  assert.equal(emailMedia.extractUploadedFileUrl({ path: 'rk-user/email-image/demo.png' }), 'rk-user/email-image/demo.png')
})
