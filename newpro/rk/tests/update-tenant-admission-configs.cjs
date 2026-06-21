const API_BASE = process.env.API_BASE || process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'
const ADMIN = {
  username: process.env.ADMISSION_ADMIN_USERNAME || 'admin_a',
  password: process.env.ADMISSION_ADMIN_PASSWORD || '123456',
  organizationId: process.env.ADMISSION_ADMIN_ORG || '1'
}

const descriptionTemplates = [
  '璇峰洿缁曞熀纭€淇℃伅銆佹搮闀挎柟鍚戝拰甯屾湜鎵挎媴鐨勫伐浣滃唴瀹瑰畬鎴愮敵璇?,
  '璇风粨鍚堜釜浜虹粡鍘嗚鏄庝綘甯屾湜鍦ㄦ湰绉熸埛闀挎湡鍙備笌鐨勯」鐩柟鍚?,
  '璇峰畬鏁村～鍐欎綘鐨勫涔犺儗鏅€佸崗浣滅粡楠屽拰鍔犲叆鍔ㄦ満锛屼究浜庣鐞嗗憳瀹℃牳',
  '璇烽噸鐐硅鏄庝綘鎯冲弬涓庣殑鎶€鏈柟鍚戙€佸彲鎶曞叆鏃堕棿涓庨鏈熻础鐚?,
  '璇风粨鍚堟湰绉熸埛鐨勭粍缁囨柟鍚戯紝璇存槑浣犵殑浼樺娍涓庢兂鍔犲叆鐨勫叿浣撳師鍥?,
  '璇疯鐪熷～鍐欑敵璇疯祫鏂欙紝渚夸簬绠＄悊鍛樻牴鎹柟鍚戝尮閰嶅悗缁潰璇曚笌瀹℃牳'
]

const successTemplates = [
  '宸叉敹鍒颁綘鎻愪氦?{tenantName} 鐨勭敵璇凤紝璇风瓑寰呯鐞嗗憳瀹℃牳',
  '{tenantName} 宸叉敹鍒颁綘鐨勭敵璇凤紝鍚庣画瀹℃牳缁撴灉浼氭寜娴佺▼閫氱煡',
  '鐢宠宸叉彁浜ゅ埌 {tenantName}锛岃鐣欐剰鍚庣画瀹℃牳涓庤仈绯婚偖浠?,
  '浣犵殑 {tenantName} 鍏ョぞ鐢宠宸叉彁浜ゆ垚鍔燂紝璇疯€愬績绛夊緟瀹℃牳'
]

const defaultFields = [
  { key: 'name', label: '濮撳悕', placeholder: '璇疯緭鍏ュ', enabled: true, required: true },
  { key: 'studentId', label: '瀛﹀彿', placeholder: '璇疯緭鍏ュ', enabled: true, required: true },
  { key: 'major', label: '涓撲笟', placeholder: '璇疯緭鍏ヤ笓', enabled: true, required: true },
  { key: 'grade', label: '骞寸骇', placeholder: '璇烽€夋嫨骞寸骇', enabled: true, required: true },
  { key: 'phone', label: '鑱旂郴鐢佃瘽', placeholder: '璇疯緭鍏ヨ仈绯荤數', enabled: true, required: true },
  { key: 'email', label: '閭', placeholder: '璇疯緭鍏ュ父鐢ㄩ偖', enabled: true, required: true },
  { key: 'interests', label: '鎶€鏈柟', placeholder: '璇疯嚦灏戦€夋嫨涓€涓妧鏈柟', enabled: true, required: true },
  { key: 'intro', label: '鑷垜浠嬬粛', placeholder: '璇蜂粙缁嶄綘鐨勬妧鏈儗鏅€佸弬涓庣粡鍘嗕互鍙婂姞鍏ュ師', enabled: true, required: true }
]

async function requestJson(url, options = {}) {
  const res = await fetch(url, options)
  const body = await res.json()
  if (!res.ok || (body.code !== undefined && body.code !== 200)) {
    throw new Error(`Request failed: ${url} -> ${res.status} ${JSON.stringify(body)}`)
  }
  return body
}

function buildConfig(tenant, existingConfig, index) {
  const description = `${tenant.tenantName}?{tenant.tenantCode}锛夛細${descriptionTemplates[index % descriptionTemplates.length]}`
  const successMessage = successTemplates[index % successTemplates.length].replaceAll('{tenantName}', tenant.tenantName)

  return {
    pageTitle: `${tenant.tenantName} 鍏ョぞ鐢宠`,
    pageDescription: description,
    successMessage,
    fields: Array.isArray(existingConfig?.fields) && existingConfig.fields.length > 0 ? existingConfig.fields : defaultFields
  }
}

async function main() {
  const loginBody = await requestJson(`${API_BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(ADMIN)
  })

  const token = loginBody.data?.token
  if (!token) {
    throw new Error('Login token missing')
  }

  const tenantBody = await requestJson(`${API_BASE}/tenants/list`)
  const tenants = tenantBody.data || []
  if (tenants.length === 0) {
    throw new Error('No tenants returned from /tenants/list')
  }

  const results = []
  for (const [index, tenant] of tenants.entries()) {
    const headers = {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
      'X-Tenant-Id': String(tenant.id)
    }

    const currentBody = await requestJson(`${API_BASE}/api/admission/form-config/public?tenantId=${tenant.id}`, {
      headers
    })
    const payload = buildConfig(tenant, currentBody.data, index)

    await requestJson(`${API_BASE}/api/admission/form-config/current`, {
      method: 'PUT',
      headers,
      body: JSON.stringify(payload)
    })

    results.push({
      tenantId: tenant.id,
      tenantName: tenant.tenantName,
      pageTitle: payload.pageTitle,
      pageDescription: payload.pageDescription
    })
  }

  console.log(JSON.stringify({
    updatedCount: results.length,
    samples: results.slice(0, 5),
    last: results[results.length - 1]
  }, null, 2))
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
