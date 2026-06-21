// @ts-check

const ADMIN = { username: 'admin_a', password: 'change-me', organizationId: '1' }

const STABLE_ADMISSION_FIELDS = [
  { key: 'name', source: 'builtin', type: 'text', label: '濮撳悕', placeholder: '璇疯緭鍏ュ鍚?, enabled: true, required: true, sort: 10, options: [] },
  { key: 'studentId', source: 'builtin', type: 'text', label: '瀛﹀彿', placeholder: '璇疯緭鍏ュ鍙?, enabled: true, required: true, sort: 20, options: [] },
  { key: 'college', source: 'builtin', type: 'text', label: '瀛﹂櫌', placeholder: '璇疯緭鍏ユ墍鍦ㄥ闄?, enabled: false, required: false, sort: 30, options: [] },
  { key: 'major', source: 'builtin', type: 'text', label: '涓撲笟', placeholder: '璇疯緭鍏ヤ笓涓?, enabled: true, required: true, sort: 40, options: [] },
  {
    key: 'grade',
    source: 'builtin',
    type: 'select',
    label: '骞寸骇',
    placeholder: '璇烽€夋嫨骞寸骇',
    enabled: true,
    required: true,
    sort: 50,
    options: [
      { label: '澶т竴', value: '1' },
      { label: '澶т簩', value: '2' },
      { label: '澶т笁', value: '3' },
      { label: '澶у洓', value: '4' }
    ]
  },
  { key: 'phone', source: 'builtin', type: 'text', label: '鑱旂郴鐢佃瘽', placeholder: '璇疯緭鍏ヨ仈绯荤數璇?, enabled: false, required: false, sort: 60, options: [] },
  { key: 'email', source: 'builtin', type: 'text', label: '閭', placeholder: '璇疯緭鍏ュ父鐢ㄩ偖绠?, enabled: true, required: true, sort: 70, options: [] },
  {
    key: 'interests',
    source: 'builtin',
    type: 'checkbox',
    label: '鎶€鏈柟鍚?,
    placeholder: '璇疯嚦灏戦€夋嫨涓€涓妧鏈柟鍚?,
    enabled: true,
    required: true,
    sort: 80,
    options: [
      { label: '鍓嶇寮€鍙?, value: 'frontend' },
      { label: '鍚庣寮€鍙?, value: 'backend' }
    ]
  },
  { key: 'positionIntent', source: 'builtin', type: 'text', label: '鑱屼綅鎰忓悜', placeholder: '璇疯緭鍏ユ兂鍙備笌鐨勮亴浣嶆垨鏂瑰悜', enabled: false, required: false, sort: 90, options: [] },
  { key: 'specialty', source: 'builtin', type: 'textarea', label: '涓汉鐗归暱', placeholder: '璇疯緭鍏ヤ綘鐨勭壒闀挎垨浼樺娍', enabled: false, required: false, sort: 100, options: [] },
  { key: 'availableTime', source: 'builtin', type: 'text', label: '鍙姇鍏ユ椂闂?, placeholder: '璇疯緭鍏ユ瘡鍛ㄥ彲鎶曞叆鐨勬椂闂?, enabled: false, required: false, sort: 110, options: [] },
  { key: 'portfolioUrl', source: 'builtin', type: 'url', label: '浣滃搧閾炬帴', placeholder: '璇疯緭鍏ヤ綔鍝佹垨涓汉涓婚〉閾炬帴', enabled: false, required: false, sort: 120, options: [] },
  { key: 'intro', source: 'builtin', type: 'textarea', label: '鑷垜浠嬬粛', placeholder: '璇蜂粙缁嶄綘鐨勬妧鏈儗鏅€佸弬涓庣粡鍘嗕互鍙婂姞鍏ュ師鍥?, enabled: true, required: true, sort: 130, options: [] },
  { key: 'remark', source: 'builtin', type: 'textarea', label: '琛ュ厖璇存槑', placeholder: '鍙ˉ鍏呭叾浠栬鏄?, enabled: false, required: false, sort: 140, options: [] }
]

async function loginApi(request, apiBase, user = ADMIN) {
  const res = await request.post(`${apiBase}/auth/login`, { data: user })
  const raw = await res.text()
  if (!raw) {
    throw new Error(`Login returned empty response: status=${res.status()}`)
  }

  let body
  try {
    body = JSON.parse(raw)
  } catch (error) {
    throw new Error(`Login returned non-JSON response: status=${res.status()}, body=${raw.slice(0, 300)}`)
  }

  if (body.code !== 200 || !body.data?.token) {
    throw new Error(`Login failed: ${JSON.stringify(body)}`)
  }
  return body.data
}

async function ensureStableAdmissionConfig(request, apiBase, tenantId = '1') {
  const login = await loginApi(request, apiBase, { ...ADMIN, organizationId: String(tenantId) })
  const headers = {
    Authorization: `Bearer ${login.token}`,
    'X-Tenant-Id': String(tenantId)
  }

  const saveRes = await request.put(`${apiBase}/api/admission/form-config/current`, {
    headers,
    data: {
      pageTitle: `缁熶竴鐢宠鍏ュ彛-${tenantId}`,
      pageDescription: '璇峰畬鏁村～鍐欑敵璇疯祫鏂欏悗鎻愪氦瀹℃牳銆?,
      successMessage: '鐢宠宸叉彁浜わ紝璇风瓑寰呭鏍搞€?,
      fields: STABLE_ADMISSION_FIELDS
    }
  })
  if (!saveRes.ok()) {
    throw new Error(`Failed to save admission config for tenant ${tenantId}`)
  }
  return login
}

async function fillStableRegisterFields(page, seed) {
  await page.getByPlaceholder('璇疯緭鍏ュ鍚?).fill(`娴嬭瘯鐢ㄦ埛${seed}`)
  await page.getByPlaceholder('璇疯緭鍏ュ鍙?).fill(`2026${String(seed).slice(-6)}`)
  await page.getByPlaceholder('璇疯緭鍏ヤ笓涓?).fill('杞欢宸ョ▼')

  await page.locator('.el-select').nth(1).click()
  await page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: '澶т竴' }).click()

  await page.getByText('鍓嶇寮€鍙?).click()
  await page.getByPlaceholder('璇蜂粙缁嶄綘鐨勬妧鏈儗鏅€佸弬涓庣粡鍘嗕互鍙婂姞鍏ュ師鍥?).fill('杩欐槸鑷姩鍖栨祴璇曞～鍐欑殑鑷垜浠嬬粛鍐呭锛岀敤浜庨獙璇佸姩鎬佽〃鍗曟敞鍐岄摼璺€?)
}

module.exports = {
  ADMIN,
  STABLE_ADMISSION_FIELDS,
  loginApi,
  ensureStableAdmissionConfig,
  fillStableRegisterFields
}
