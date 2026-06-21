<template>
  <div class="config-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <div>
            <div class="card-title">入社表单配置</div>
            <p class="card-subtitle">统一控制 register / join 两条链路的租户表单字段。</p>
          </div>
          <el-button type="primary" :loading="saving" @click="handleSave">保存配置</el-button>
        </div>
      </template>

      <el-form v-loading="loading" label-width="120px" class="page-form">
        <el-form-item label="页面标题">
          <el-input v-model="form.pageTitle" placeholder="请输入页面标题" />
        </el-form-item>
        <el-form-item label="页面说明">
          <el-input v-model="form.pageDescription" type="textarea" :rows="3" placeholder="请输入页面说明" />
        </el-form-item>
        <el-form-item label="成功提示">
          <el-input v-model="form.successMessage" placeholder="请输入提交成功提示" />
        </el-form-item>
      </el-form>

      <div class="field-toolbar">
        <div>
          <h3>字段配置</h3>
          <p>内建字段可改标签/类型/必填/显示状态，自定义字段可新增和删除。</p>
        </div>
        <el-button type="primary" plain @click="handleAddCustomField">新增自定义字段</el-button>
      </div>

      <div class="field-list">
        <div
          v-for="(field, index) in form.fields"
          :key="field.uid"
          class="field-card"
          data-testid="admission-field-row"
        >
          <div class="field-card__header">
            <div class="field-meta">
              <el-tag :type="field.source === 'custom' ? 'success' : 'info'" effect="light">
                {{ field.source === 'custom' ? '自定义字段' : '内建字段' }}
              </el-tag>
              <span class="field-key">{{ field.key || '待填写 key' }}</span>
            </div>
            <el-button
              v-if="field.source === 'custom'"
              type="danger"
              text
              @click="handleRemoveCustomField(index)"
            >
              删除
            </el-button>
          </div>

          <div class="field-grid">
            <div class="field-grid__item">
              <label>字段标识</label>
              <el-input
                v-model="field.key"
                :disabled="field.source !== 'custom'"
                placeholder="自定义字段 key"
              />
            </div>

            <div class="field-grid__item">
              <label>字段标签</label>
              <el-input v-model="field.label" placeholder="字段标签" />
            </div>

            <div class="field-grid__item">
              <label>字段类型</label>
              <el-select v-model="field.type" style="width: 100%">
                <el-option
                  v-for="option in fieldTypeOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </div>

            <div class="field-grid__item">
              <label>排序</label>
              <el-input-number v-model="field.sort" :min="1" :step="10" style="width: 100%" />
            </div>

            <div class="field-grid__item field-grid__item--wide">
              <label>占位提示</label>
              <el-input v-model="field.placeholder" placeholder="字段占位提示" />
            </div>

            <div class="field-grid__item">
              <label>显示</label>
              <el-switch v-model="field.enabled" />
            </div>

            <div class="field-grid__item">
              <label>必填</label>
              <el-switch v-model="field.required" :disabled="!field.enabled" />
            </div>

            <div v-if="supportsOptions(field)" class="field-grid__item field-grid__item--full">
              <label>选项配置</label>
              <el-input
                v-model="field.optionsText"
                type="textarea"
                :rows="3"
                placeholder="每行一个选项，格式：标签:值"
              />
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getCurrentAdmissionFormConfig, saveCurrentAdmissionFormConfig } from '@/api/admission'
import { createDefaultAdmissionConfig, normalizeAdmissionFormConfig } from '@/composables/useAdmissionFormRuntime'

const loading = ref(false)
const saving = ref(false)
let uidSeed = 0

const fieldTypeOptions = [
  { label: '单行文本', value: 'text' },
  { label: '多行文本', value: 'textarea' },
  { label: '下拉选择', value: 'select' },
  { label: '单选', value: 'radio' },
  { label: '多选', value: 'checkbox' },
  { label: '数字', value: 'number' },
  { label: '日期', value: 'date' },
  { label: '链接', value: 'url' }
]

const form = reactive({
  pageTitle: '',
  pageDescription: '',
  successMessage: '',
  fields: []
})

const supportsOptions = (field) => ['select', 'radio', 'checkbox'].includes(field.type)

const formatOptions = (options) => {
  if (!Array.isArray(options) || options.length === 0) {
    return ''
  }
  return options
    .map(option => `${option.label || option.value}:${option.value || option.label}`)
    .join('\n')
}

const parseOptions = (text) => {
  return String(text || '')
    .split('\n')
    .map(line => line.trim())
    .filter(Boolean)
    .map(line => {
      const [labelPart, ...rest] = line.split(':')
      const label = labelPart.trim()
      const value = rest.join(':').trim() || label
      return { label, value }
    })
}

const toEditableField = (field) => ({
  uid: `${field.key || 'custom'}-${uidSeed++}`,
  key: field.key || '',
  label: field.label || '',
  placeholder: field.placeholder || '',
  enabled: field.enabled !== false,
  required: field.required === true,
  source: field.source || 'builtin',
  type: field.type || 'text',
  sort: field.sort || 999,
  optionsText: formatOptions(field.options)
})

const createDefaultForm = () => {
  const config = createDefaultAdmissionConfig('join')
  return {
    pageTitle: config.pageTitle,
    pageDescription: config.pageDescription,
    successMessage: config.successMessage,
    fields: config.fields.map(toEditableField)
  }
}

const normalizeForm = (data) => {
  const next = normalizeAdmissionFormConfig(data || createDefaultAdmissionConfig('join'), 'join')
  form.pageTitle = next.pageTitle
  form.pageDescription = next.pageDescription
  form.successMessage = next.successMessage
  form.fields = next.fields.map(toEditableField)
}

const buildPayload = () => {
  const seenKeys = new Set()
  const fields = form.fields.map((field, index) => {
    const key = String(field.key || '').trim()
    const label = String(field.label || '').trim()

    if (!key) {
      throw new Error('请为每个字段填写字段标识')
    }
    if (!label) {
      throw new Error('请为每个字段填写字段标签')
    }
    if (seenKeys.has(key)) {
      throw new Error(`字段标识重复：${key}`)
    }
    seenKeys.add(key)

    return {
      key,
      label,
      placeholder: String(field.placeholder || '').trim(),
      enabled: field.enabled !== false,
      required: field.required === true,
      source: field.source || 'builtin',
      type: field.type || 'text',
      sort: field.sort || (index + 1) * 10,
      options: supportsOptions(field) ? parseOptions(field.optionsText) : []
    }
  })

  return {
    pageTitle: String(form.pageTitle || '').trim(),
    pageDescription: String(form.pageDescription || '').trim(),
    successMessage: String(form.successMessage || '').trim(),
    fields
  }
}

const handleAddCustomField = () => {
  const maxSort = form.fields.reduce((current, field) => Math.max(current, Number(field.sort) || 0), 0)
  form.fields.push({
    uid: `custom-${uidSeed++}`,
    key: '',
    label: '',
    placeholder: '',
    enabled: true,
    required: false,
    source: 'custom',
    type: 'text',
    sort: maxSort + 10,
    optionsText: ''
  })
}

const handleRemoveCustomField = (index) => {
  form.fields.splice(index, 1)
}

const fetchConfig = async () => {
  loading.value = true
  try {
    const res = await getCurrentAdmissionFormConfig()
    if (res.code === 200) {
      normalizeForm(res.data)
      return
    }
    normalizeForm(createDefaultAdmissionConfig('join'))
  } catch (error) {
    console.error('获取入社表单配置失败:', error)
    normalizeForm(createDefaultAdmissionConfig('join'))
    ElMessage.error('获取入社表单配置失败')
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  saving.value = true
  try {
    const payload = buildPayload()
    const res = await saveCurrentAdmissionFormConfig(payload)
    if (res.code === 200) {
      normalizeForm(res.data)
      ElMessage.success('入社表单配置已保存')
      return
    }
    throw new Error(res.msg || '保存入社表单配置失败')
  } catch (error) {
    console.error('保存入社表单配置失败:', error)
    ElMessage.error(error.message || '保存入社表单配置失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  normalizeForm(createDefaultAdmissionConfig('join'))
  fetchConfig()
})
</script>

<style lang="scss" scoped>
.config-page {
  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 24px;
  }

  .card-title {
    font-size: 20px;
    font-weight: 700;
    color: #1f2937;
  }

  .card-subtitle {
    margin: 6px 0 0;
    color: #6b7280;
    font-size: 13px;
  }

  .page-form {
    margin-bottom: 24px;
  }

  .field-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    margin: 24px 0 16px;

    h3 {
      margin: 0;
      font-size: 18px;
      color: #111827;
    }

    p {
      margin: 6px 0 0;
      color: #6b7280;
      font-size: 13px;
    }
  }

  .field-list {
    display: grid;
    gap: 16px;
  }

  .field-card {
    border: 1px solid #e5e7eb;
    border-radius: 16px;
    padding: 18px;
    background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  }

  .field-card__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 16px;
  }

  .field-meta {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .field-key {
    color: #475569;
    font-size: 13px;
    font-family: Consolas, Monaco, monospace;
  }

  .field-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 16px;
  }

  .field-grid__item {
    display: flex;
    flex-direction: column;
    gap: 8px;

    label {
      font-size: 12px;
      font-weight: 600;
      color: #475569;
    }
  }

  .field-grid__item--wide {
    grid-column: span 2;
  }

  .field-grid__item--full {
    grid-column: 1 / -1;
  }
}
</style>
