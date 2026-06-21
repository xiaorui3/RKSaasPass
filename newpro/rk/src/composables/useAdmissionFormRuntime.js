const SOURCE_BUILTIN = 'builtin'
const SOURCE_CUSTOM = 'custom'
const LOCKED_REQUIRED_BUILTIN_FIELDS = new Set(['email'])

const BUILTIN_FIELD_CATALOG = [
  { key: 'name', label: '姓名', placeholder: '请输入姓名', source: SOURCE_BUILTIN, type: 'text', enabled: true, required: true, sort: 10, options: [] },
  { key: 'studentId', label: '学号', placeholder: '请输入学号', source: SOURCE_BUILTIN, type: 'text', enabled: true, required: true, sort: 20, options: [] },
  { key: 'college', label: '学院', placeholder: '请输入所在学院', source: SOURCE_BUILTIN, type: 'text', enabled: true, required: false, sort: 30, options: [] },
  { key: 'major', label: '专业', placeholder: '请输入专业', source: SOURCE_BUILTIN, type: 'text', enabled: true, required: true, sort: 40, options: [] },
  {
    key: 'grade',
    label: '年级',
    placeholder: '请选择年级',
    source: SOURCE_BUILTIN,
    type: 'select',
    enabled: true,
    required: true,
    sort: 50,
    options: [
      { label: '大一', value: '1' },
      { label: '大二', value: '2' },
      { label: '大三', value: '3' },
      { label: '大四', value: '4' }
    ]
  },
  { key: 'phone', label: '联系电话', placeholder: '请输入联系电话', source: SOURCE_BUILTIN, type: 'text', enabled: true, required: true, sort: 60, options: [] },
  { key: 'email', label: '邮箱', placeholder: '请输入常用邮箱', source: SOURCE_BUILTIN, type: 'text', enabled: true, required: true, sort: 70, options: [] },
  {
    key: 'interests',
    label: '技术方向',
    placeholder: '请至少选择一个技术方向',
    source: SOURCE_BUILTIN,
    type: 'checkbox',
    enabled: true,
    required: true,
    sort: 80,
    options: [
      { label: '前端开发', value: 'frontend' },
      { label: '后端开发', value: 'backend' },
      { label: '移动开发', value: 'mobile' },
      { label: '数据分析', value: 'data' },
      { label: '人工智能', value: 'ai' }
    ]
  },
  { key: 'positionIntent', label: '职位意向', placeholder: '请输入想参与的职位或方向', source: SOURCE_BUILTIN, type: 'text', enabled: true, required: false, sort: 90, options: [] },
  { key: 'specialty', label: '个人特长', placeholder: '请输入你的特长或优势', source: SOURCE_BUILTIN, type: 'textarea', enabled: true, required: false, sort: 100, options: [] },
  { key: 'availableTime', label: '可投入时间', placeholder: '请输入每周可投入的时间', source: SOURCE_BUILTIN, type: 'text', enabled: true, required: false, sort: 110, options: [] },
  { key: 'portfolioUrl', label: '作品链接', placeholder: '请输入作品或个人主页链接', source: SOURCE_BUILTIN, type: 'url', enabled: true, required: false, sort: 120, options: [] },
  { key: 'intro', label: '自我介绍', placeholder: '请介绍你的技术背景、参与经历以及加入原因', source: SOURCE_BUILTIN, type: 'textarea', enabled: true, required: true, sort: 130, options: [] },
  { key: 'remark', label: '补充说明', placeholder: '可补充其他说明', source: SOURCE_BUILTIN, type: 'textarea', enabled: true, required: false, sort: 140, options: [] }
]

const DEFAULT_COPY = {
  register: {
    pageTitle: '注册',
    pageDescription: '完成租户账号注册后即可进入对应社团链路',
    successMessage: '注册成功，请登录'
  },
  join: {
    pageTitle: '加入我们',
    pageDescription: '选择目标社团后提交入社申请，审核通过后即可进一步参与社团活动。',
    successMessage: '申请提交成功，请等待负责人审核'
  }
}

function cloneOptions(options) {
  return Array.isArray(options) ? options.map(option => ({ ...option })) : []
}

function cloneField(field) {
  return {
    ...field,
    enabled: field.enabled !== false,
    required: field.required === true,
    options: cloneOptions(field.options)
  }
}

function isBlank(value) {
  return value === null || value === undefined || String(value).trim() === ''
}

function normalizeField(field, fallbackSort) {
  const source = !isBlank(field.source)
    ? String(field.source).toLowerCase()
    : BUILTIN_FIELD_CATALOG.some(item => item.key === field.key)
      ? SOURCE_BUILTIN
      : SOURCE_CUSTOM

  return {
    key: field.key,
    label: isBlank(field.label) ? field.key : field.label,
    placeholder: isBlank(field.placeholder) ? '' : field.placeholder,
    enabled: field.enabled !== false,
    required: field.required === true,
    source,
    type: isBlank(field.type) ? 'text' : field.type,
    sort: Number.isFinite(field.sort) ? field.sort : fallbackSort,
    options: cloneOptions(field.options)
  }
}

function mergeBuiltinField(base, field) {
  const lockedRequired = LOCKED_REQUIRED_BUILTIN_FIELDS.has(base.key)
  return {
    ...base,
    label: isBlank(field.label) ? base.label : field.label,
    placeholder: isBlank(field.placeholder) ? base.placeholder : field.placeholder,
    enabled: lockedRequired ? true : field.enabled === undefined || field.enabled === null ? base.enabled : field.enabled !== false,
    required: lockedRequired ? true : field.required === undefined || field.required === null ? base.required : field.required === true,
    source: SOURCE_BUILTIN,
    type: isBlank(field.type) ? base.type : field.type,
    sort: Number.isFinite(field.sort) ? field.sort : base.sort,
    options: Array.isArray(field.options) && field.options.length > 0 ? cloneOptions(field.options) : cloneOptions(base.options)
  }
}

function applyLockedBuiltinFieldContract(field) {
  if (!LOCKED_REQUIRED_BUILTIN_FIELDS.has(field.key)) {
    return field
  }
  return {
    ...field,
    enabled: true,
    required: true
  }
}

function defaultValueForField(field) {
  return field.type === 'checkbox' ? [] : ''
}

function fieldTrigger(field) {
  return ['select', 'radio', 'checkbox', 'date'].includes(field.type) ? 'change' : 'blur'
}

function isEmptyValue(value) {
  if (Array.isArray(value)) {
    return value.length === 0
  }
  return value === undefined || value === null || value === ''
}

export function createDefaultAdmissionConfig(mode = 'join') {
  return normalizeAdmissionFormConfig({
    ...DEFAULT_COPY[mode],
    fields: BUILTIN_FIELD_CATALOG
  }, mode)
}

export function normalizeAdmissionFormConfig(raw, mode = 'join') {
  const builtin = new Map(BUILTIN_FIELD_CATALOG.map(field => [field.key, cloneField(field)]))
  const customFields = []
  const incomingFields = Array.isArray(raw?.fields) ? raw.fields : []

  for (const field of incomingFields) {
    if (!field?.key) {
      continue
    }
    const source = !isBlank(field.source)
      ? String(field.source).toLowerCase()
      : builtin.has(field.key)
        ? SOURCE_BUILTIN
        : SOURCE_CUSTOM

    if (source === SOURCE_CUSTOM || !builtin.has(field.key)) {
      customFields.push(normalizeField(field, customFields.length + 1000))
      continue
    }

    builtin.set(field.key, mergeBuiltinField(builtin.get(field.key), field))
  }

  return {
    pageTitle: isBlank(raw?.pageTitle) ? DEFAULT_COPY[mode].pageTitle : raw.pageTitle,
    pageDescription: isBlank(raw?.pageDescription) ? DEFAULT_COPY[mode].pageDescription : raw.pageDescription,
    successMessage: isBlank(raw?.successMessage) ? DEFAULT_COPY[mode].successMessage : raw.successMessage,
    fields: [...builtin.values(), ...customFields]
      .map(applyLockedBuiltinFieldContract)
      .sort((left, right) => (left.sort ?? 9999) - (right.sort ?? 9999))
  }
}

export function buildAdmissionFieldMap(config) {
  const map = {}
  for (const field of config?.fields || []) {
    map[field.key] = field
  }
  return map
}

export function isAdmissionFieldEnabled(config, key) {
  return buildAdmissionFieldMap(config)[key]?.enabled !== false
}

export function syncAdmissionFormModel(model, config) {
  for (const field of config?.fields || []) {
    if (field.type === 'checkbox') {
      if (!Array.isArray(model[field.key])) {
        model[field.key] = Array.isArray(model[field.key]) ? model[field.key] : []
      }
      continue
    }
    if (model[field.key] === undefined || model[field.key] === null) {
      model[field.key] = defaultValueForField(field)
    }
  }
  return model
}

export function buildAdmissionFieldRules(config) {
  const rules = {}
  for (const field of config?.fields || []) {
    if (field.enabled === false) {
      continue
    }

    const validators = []
    if (field.required) {
      validators.push({
        validator: (_rule, value, callback) => {
          callback(isEmptyValue(value) ? new Error(`请填写${field.label}`) : undefined)
        },
        trigger: fieldTrigger(field)
      })
    }

    if (field.key === 'email') {
      validators.push({
        validator: (_rule, value, callback) => {
          if (isEmptyValue(value)) {
            callback()
            return
          }
          const valid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(value))
          callback(valid ? undefined : new Error('请输入正确的邮箱格式'))
        },
        trigger: 'blur'
      })
    }

    if (field.type === 'url') {
      validators.push({
        validator: (_rule, value, callback) => {
          if (isEmptyValue(value)) {
            callback()
            return
          }
          try {
            new URL(String(value))
            callback()
          } catch (_error) {
            callback(new Error('请输入正确的链接地址'))
          }
        },
        trigger: 'blur'
      })
    }

    rules[field.key] = validators
  }
  return rules
}

export function collectAdmissionFormPayload(model, config) {
  const payload = {}
  for (const field of config?.fields || []) {
    if (field.enabled === false) {
      continue
    }
    const value = model[field.key]
    if (isEmptyValue(value)) {
      continue
    }
    payload[field.key] = Array.isArray(value) ? [...value] : value
  }
  return payload
}

export function resolveAdmissionSuccessMessage(config, mode = 'join') {
  return isBlank(config?.successMessage) ? DEFAULT_COPY[mode].successMessage : config.successMessage
}
