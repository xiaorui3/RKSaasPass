<template>
  <div class="theme-config-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>主题配置</span>
          <el-button type="primary" :loading="saving" @click="handleSave">保存主题</el-button>
        </div>
      </template>

      <section class="style-mode-panel">
        <div class="panel-header">
          <h3>界面风格</h3>
          <p>这里控制当前租户所有成员看到的前台和后台界面风格。当前样式保持现有页面不变，BIP 样式只启用隔离后的企业工作台视觉。</p>
        </div>
        <div class="style-mode-grid">
          <div class="style-mode-card">
            <div class="style-mode-title">前台界面风格</div>
            <el-radio-group v-model="form.frontendStyle" @change="selectFrontendStyle">
              <el-radio-button
                v-for="option in frontendStyleOptions"
                :key="`frontend-style-${option.value}`"
                :label="option.value"
              >
                {{ option.label }}
              </el-radio-button>
            </el-radio-group>
            <p>{{ styleDescription(form.frontendStyle, 'frontend') }}</p>
          </div>
          <div class="style-mode-card">
            <div class="style-mode-title">后台界面风格</div>
            <el-radio-group v-model="form.adminStyle" @change="selectAdminStyle">
              <el-radio-button
                v-for="option in adminStyleOptions"
                :key="`admin-style-${option.value}`"
                :label="option.value"
              >
                {{ option.label }}
              </el-radio-button>
            </el-radio-group>
            <p>{{ styleDescription(form.adminStyle, 'admin') }}</p>
          </div>
        </div>
      </section>

      <div class="theme-grid" v-loading="loading">
        <section class="theme-panel">
          <div class="panel-header">
            <h3>前台主题</h3>
            <p>公开页面、登录、注册、加入页、个人中心都会跟随这里</p>
          </div>
          <div class="theme-options">
            <div v-for="preset in presets" :key="`front-${preset.key}`"
              class="theme-card" :class="{ active: form.frontendTheme === preset.key }"
              @click="selectFrontend(preset.key)">
              <div class="check-badge" v-if="form.frontendTheme === preset.key">已选</div>
              <div class="theme-preview" :style="previewStyle(preset)">
                <div class="preview-header"></div>
                <div class="preview-body">
                  <div class="preview-hero"></div>
                  <div class="preview-row">
                    <span class="swatch primary" :style="{ background: preset.preview.primary }"></span>
                    <span class="swatch accent" :style="{ background: preset.preview.accent }"></span>
                  </div>
                </div>
              </div>
              <div class="theme-meta">
                <strong>{{ preset.label }}</strong>
                <span>{{ preset.description }}</span>
              </div>
            </div>
          </div>
        </section>

        <section class="theme-panel">
          <div class="panel-header">
            <h3>后台主题</h3>
            <p>管理后台的侧栏、头部、卡片和主色会跟随这里</p>
          </div>
          <div class="theme-options">
            <div v-for="preset in presets" :key="`admin-${preset.key}`"
              class="theme-card" :class="{ active: form.adminTheme === preset.key }"
              @click="selectAdmin(preset.key)">
              <div class="check-badge" v-if="form.adminTheme === preset.key">已选</div>
              <div class="theme-preview admin-preview" :style="previewStyle(preset)">
                <div class="preview-sidebar"></div>
                <div class="preview-main">
                  <div class="preview-topbar"></div>
                  <div class="preview-cards">
                    <span class="swatch primary" :style="{ background: preset.preview.primary }"></span>
                    <span class="swatch accent" :style="{ background: preset.preview.accent }"></span>
                  </div>
                </div>
              </div>
              <div class="theme-meta">
                <strong>{{ preset.label }}</strong>
                <span>{{ preset.description }}</span>
              </div>
            </div>
          </div>
        </section>
      </div>
    </el-card>

    <el-card style="margin-top:20px;">
      <template #header>
        <div class="card-header">
          <span>后台背景自定义</span>
          <el-button type="primary" :loading="savingBg" @click="handleSaveBg">保存背景设置</el-button>
        </div>
      </template>

      <div class="bg-config">
        <el-form label-width="100px">
          <el-form-item label="背景颜色">
            <el-color-picker v-model="bgForm.bgColor" show-alpha @change="previewBg" />
            <el-button size="small" text @click="bgForm.bgColor = ''; previewBg()">重置</el-button>
          </el-form-item>
          <el-form-item label="背景图片">
            <div class="bg-image-row">
              <el-upload
                :http-request="handleBgUpload"
                :show-file-list="false"
                accept="image/*"
              >
                <el-button size="small">上传图片</el-button>
              </el-upload>
              <el-button v-if="bgForm.bgImage" size="small" type="danger" text @click="bgForm.bgImage = ''; previewBg()">移除图片</el-button>
              <div v-if="bgForm.bgImage" class="bg-thumb">
                <img :src="resolveMediaUrl(bgForm.bgImage)" alt="bg" />
              </div>
            </div>
          </el-form-item>
          <el-form-item label="图片透明度" v-if="bgForm.bgImage">
            <el-slider v-model="bgForm.bgOpacity" :min="0" :max="100" :step="5" style="width:300px;" @change="previewBg" />
            <span style="margin-left:12px;color:#909399;">{{ bgForm.bgOpacity }}%</span>
          </el-form-item>
        </el-form>
        <div class="bg-preview-box" :style="bgPreviewStyle">
          <span>背景预览</span>
        </div>
      </div>
    </el-card>

    <el-card style="margin-top:20px;">
      <template #header>
        <div class="card-header">
          <span>前台背景自定义</span>
          <el-button type="primary" :loading="savingBg" @click="handleSaveBg">保存背景设置</el-button>
        </div>
      </template>

      <div class="bg-config">
        <el-form label-width="100px">
          <el-form-item label="背景颜色">
            <el-color-picker v-model="frontendBgForm.bgColor" show-alpha @change="previewFrontendBg" />
            <el-button size="small" text @click="frontendBgForm.bgColor = ''; previewFrontendBg()">重置</el-button>
          </el-form-item>
          <el-form-item label="背景图片">
            <div class="bg-image-row">
              <el-upload
                :http-request="handleFrontendBgUpload"
                :show-file-list="false"
                accept="image/*"
              >
                <el-button size="small">上传图片</el-button>
              </el-upload>
              <el-button v-if="frontendBgForm.bgImage" size="small" type="danger" text @click="frontendBgForm.bgImage = ''; previewFrontendBg()">移除图片</el-button>
              <div v-if="frontendBgForm.bgImage" class="bg-thumb">
                <img :src="resolveMediaUrl(frontendBgForm.bgImage)" alt="frontend bg" />
              </div>
            </div>
          </el-form-item>
          <el-form-item label="图片透明度" v-if="frontendBgForm.bgImage">
            <el-slider v-model="frontendBgForm.bgOpacity" :min="0" :max="100" :step="5" style="width:300px;" @change="previewFrontendBg" />
            <span style="margin-left:12px;color:#909399;">{{ frontendBgForm.bgOpacity }}%</span>
          </el-form-item>
        </el-form>
        <div class="bg-preview-box" :style="frontendBgPreviewStyle">
          <span>前台背景预览</span>
        </div>
      </div>
    </el-card>

    <el-card style="margin-top:20px;">
      <template #header>
        <div class="card-header">
          <span>前台首页模块编排</span>
          <div class="card-actions">
            <el-button @click="resetPortalLayoutConfig">恢复默认</el-button>
            <el-button type="primary" :loading="savingPortal" @click="savePortalLayoutConfig">保存首页编排</el-button>
          </div>
        </div>
      </template>

      <div class="portal-layout-config" v-loading="portalLoading">
        <section class="layout-config-panel">
          <div class="panel-header">
            <h3>首页区域</h3>
            <p>控制首页各区域的名称、显示状态和上下顺序。</p>
          </div>
          <el-table :data="portalLayoutForm.sections" border>
            <el-table-column label="显示" width="90">
              <template #default="{ row }">
                <el-switch v-model="row.visible" />
              </template>
            </el-table-column>
            <el-table-column prop="key" label="模块" width="140" />
            <el-table-column label="显示名称" min-width="180">
              <template #default="{ row }">
                <el-input v-model="row.title" placeholder="请输入显示名称" />
              </template>
            </el-table-column>
            <el-table-column label="排序" width="130">
              <template #default="{ row }">
                <el-input-number v-model="row.order" :min="1" :step="10" controls-position="right" />
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section class="layout-config-panel">
          <div class="panel-header">
            <h3>快捷入口卡片</h3>
            <p>控制首页快捷功能入口的名称、说明、图标文字、显示状态和顺序。</p>
          </div>
          <el-table :data="portalLayoutForm.entryModules" border>
            <el-table-column label="显示" width="90">
              <template #default="{ row }">
                <el-switch v-model="row.visible" />
              </template>
            </el-table-column>
            <el-table-column prop="key" label="功能" width="140" />
            <el-table-column label="显示名称" min-width="170">
              <template #default="{ row }">
                <el-input v-model="row.title" placeholder="请输入显示名称" />
              </template>
            </el-table-column>
            <el-table-column label="说明" min-width="220">
              <template #default="{ row }">
                <el-input v-model="row.description" placeholder="请输入入口说明" />
              </template>
            </el-table-column>
            <el-table-column label="图标文字" width="130">
              <template #default="{ row }">
                <el-input v-model="row.icon" maxlength="4" />
              </template>
            </el-table-column>
            <el-table-column label="排序" width="130">
              <template #default="{ row }">
                <el-input-number v-model="row.order" :min="1" :step="10" controls-position="right" />
              </template>
            </el-table-column>
          </el-table>
        </section>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { createSystemConfig, getSystemConfigList, updateSystemConfig } from '@/api/system-config'
import { getCurrentThemeConfig, saveCurrentThemeConfig } from '@/api/theme-config'
import { THEME_PRESET_LIST } from '@/config/theme-presets'
import { useThemeStore } from '@/stores/theme'
import { uploadManagedFile } from '@/utils/fileUpload'
import { resolveMediaUrl } from '@/utils/mediaUrl'
import {
  PORTAL_HOME_LAYOUT_CONFIG_KEY,
  normalizePortalHomeLayoutConfig,
  serializePortalHomeLayoutConfig,
  findConfigRecord
} from '@/utils/portalLayoutConfig'

const themeStore = useThemeStore()

const loading = ref(false)
const saving = ref(false)
const savingBg = ref(false)
const portalLoading = ref(false)
const savingPortal = ref(false)
const portalConfigRecord = ref(null)
const presets = THEME_PRESET_LIST
const frontendStyleOptions = [
  { label: '\u9ed8\u8ba4\u95e8\u6237', value: 'portal', description: '\u9996\u9875\u4fdd\u7559\u95e8\u6237\u5de5\u4f5c\u53f0\uff0c\u5bfc\u822a\u805a\u5408\u4e3a\u5185\u5bb9\u3001\u6d3b\u52a8\u8d5b\u4e8b\u3001\u793e\u56e2\u6210\u5458\u548c\u4e2a\u4eba\u5165\u53e3\u3002' },
  { label: '\u6837\u5f0f\u4e00', value: 'classic', description: '\u4fdd\u7559\u5f53\u524d\u524d\u53f0\u5b8c\u6574\u5bfc\u822a\u3001\u9875\u9762\u5e03\u5c40\u548c\u539f\u6709\u8bbf\u95ee\u8def\u5f84\u3002' },
  { label: 'BIP \u6837\u5f0f', value: 'bip', description: '\u542f\u7528\u4f01\u4e1a\u5de5\u4f5c\u53f0\u89c6\u89c9\uff0c\u540c\u65f6\u4fdd\u7559\u524d\u53f0\u4e1a\u52a1\u8def\u7531\u548c\u4ea4\u4e92\u3002' }
]

const adminStyleOptions = [
  { label: '\u6837\u5f0f\u4e00', value: 'classic', description: '\u4fdd\u7559\u5f53\u524d\u540e\u53f0\u83dc\u5355\u3001\u8868\u683c\u548c\u7ba1\u7406\u9875\u9762\u89c6\u89c9\u3002' },
  { label: 'BIP \u6837\u5f0f', value: 'bip', description: '\u542f\u7528\u540e\u53f0\u4f01\u4e1a\u5de5\u4f5c\u53f0\u89c6\u89c9\uff0c\u4e0d\u6539\u53d8\u540e\u53f0\u6743\u9650\u548c\u83dc\u5355\u6765\u6e90\u3002' }
]

const form = reactive({
  frontendTheme: 'default',
  adminTheme: 'default',
  frontendStyle: 'portal',
  adminStyle: 'classic'
})

const bgForm = reactive({
  bgColor: '',
  bgImage: '',
  bgOpacity: 15
})

const frontendBgForm = reactive({
  bgColor: '',
  bgImage: '',
  bgOpacity: 15
})

const portalLayoutForm = reactive(normalizePortalHomeLayoutConfig())

function applyPortalLayoutConfig(config) {
  const normalized = normalizePortalHomeLayoutConfig(config)
  portalLayoutForm.sections.splice(0, portalLayoutForm.sections.length, ...normalized.sections)
  portalLayoutForm.entryModules.splice(0, portalLayoutForm.entryModules.length, ...normalized.entryModules)
}

async function uploadFile(file, type) {
  const { storedValue } = await uploadManagedFile({ file }, type)
  return {
    code: 200,
    data: {
      fileUrl: storedValue,
      url: storedValue
    }
  }
}

function previewStyle(preset) {
  return {
    background: preset.preview.surface,
    color: preset.preview.text
  }
}

function selectFrontend(key) {
  form.frontendTheme = key
  themeStore.applyPreviewTheme('frontend', key)
}

function selectAdmin(key) {
  form.adminTheme = key
  themeStore.applyPreviewTheme('admin', key)
}

function normalizeFrontendStyleMode(value) {
  return ['portal', 'classic', 'bip'].includes(value) ? value : 'portal'
}

function normalizeAdminStyleMode(value) {
  return value === 'bip' ? 'bip' : 'classic'
}

function styleDescription(value, context = 'frontend') {
  const options = context === 'admin' ? adminStyleOptions : frontendStyleOptions
  return options.find((item) => item.value === value)?.description || options[0].description
}

function selectFrontendStyle(value) {
  form.frontendStyle = normalizeFrontendStyleMode(value)
}

function selectAdminStyle(value) {
  form.adminStyle = normalizeAdminStyleMode(value)
  themeStore.applyPreviewStyleMode('admin', form.adminStyle)
}

const bgPreviewStyle = computed(() => {
  const style = {
    minHeight: '120px',
    borderRadius: '12px',
    border: '1px solid #e5e7eb',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    color: '#909399',
    fontSize: '14px',
    position: 'relative',
    overflow: 'hidden'
  }
  if (bgForm.bgColor) {
    style.background = bgForm.bgColor
  }
  return style
})

const frontendBgPreviewStyle = computed(() => {
  const style = {
    minHeight: '120px',
    borderRadius: '12px',
    border: '1px solid #e5e7eb',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    color: '#909399',
    fontSize: '14px',
    position: 'relative',
    overflow: 'hidden'
  }
  if (frontendBgForm.bgColor) {
    style.background = frontendBgForm.bgColor
  }
  if (frontendBgForm.bgImage) {
    style.backgroundImage = `url(${resolveMediaUrl(frontendBgForm.bgImage)})`
    style.backgroundSize = 'cover'
    style.backgroundPosition = 'center'
  }
  return style
})

function previewBg() {
  const root = document.documentElement
  if (bgForm.bgColor) {
    root.style.setProperty('--rk-admin-body-bg', bgForm.bgColor)
  } else {
    root.style.removeProperty('--rk-admin-body-bg')
  }
  if (bgForm.bgImage) {
    root.style.setProperty('--rk-admin-bg-image', `url(${resolveMediaUrl(bgForm.bgImage)})`)
    root.style.setProperty('--rk-admin-bg-image-opacity', String(bgForm.bgOpacity / 100))
  } else {
    root.style.setProperty('--rk-admin-bg-image', 'none')
    root.style.setProperty('--rk-admin-bg-image-opacity', '0.15')
  }
}

function previewFrontendBg() {
  const root = document.documentElement
  if (frontendBgForm.bgColor) {
    root.style.setProperty('--rk-body-bg', frontendBgForm.bgColor)
  }
  if (frontendBgForm.bgImage) {
    root.style.setProperty('--rk-frontend-bg-image', `url(${resolveMediaUrl(frontendBgForm.bgImage)})`)
    root.style.setProperty('--rk-frontend-bg-image-opacity', String(frontendBgForm.bgOpacity / 100))
  } else {
    root.style.removeProperty('--rk-frontend-bg-image')
    root.style.removeProperty('--rk-frontend-bg-image-opacity')
  }
}

async function handleBgUpload({ file }) {
  try {
    const res = await uploadFile(file, 'background')
    if (res.code === 200) {
      bgForm.bgImage = res.data?.fileUrl || res.data?.url || res.data
      previewBg()
      ElMessage.success('背景图片上传成功')
    }
  } catch (error) {
    ElMessage.error(error.message || '上传失败')
  }
}

async function handleFrontendBgUpload({ file }) {
  try {
    const res = await uploadFile(file, 'background')
    if (res.code === 200) {
      frontendBgForm.bgImage = res.data?.fileUrl || res.data?.url || res.data
      previewFrontendBg()
      ElMessage.success('前台背景图片上传成功')
    }
  } catch (error) {
    ElMessage.error(error.message || '上传失败')
  }
}

function buildThemePayload() {
  return {
    frontendTheme: form.frontendTheme,
    adminTheme: form.adminTheme,
    frontendStyle: normalizeFrontendStyleMode(form.frontendStyle),
    adminStyle: normalizeAdminStyleMode(form.adminStyle),
    frontendBgColor: frontendBgForm.bgColor,
    frontendBgImage: frontendBgForm.bgImage,
    frontendBgOpacity: frontendBgForm.bgOpacity,
    adminBgColor: bgForm.bgColor,
    adminBgImage: bgForm.bgImage,
    adminBgOpacity: bgForm.bgOpacity
  }
}

async function handleSave() {
  saving.value = true
  try {
    const res = await saveCurrentThemeConfig(buildThemePayload())
    if (res.code === 200) {
      themeStore.applyPreviewTheme('admin', form.adminTheme)
      themeStore.applyPreviewStyleMode('admin', form.adminStyle)
      ElMessage.success('主题配置已保存，实时预览中')
      return
    }
    throw new Error(res.msg || '保存主题配置失败')
  } catch (error) {
    ElMessage.error(error.message || '保存主题配置失败')
  } finally {
    saving.value = false
  }
}

async function handleSaveBg() {
  savingBg.value = true
  try {
    previewBg()
    previewFrontendBg()
    const res = await saveCurrentThemeConfig(buildThemePayload())
    if (res.code !== 200) {
      throw new Error(res.msg || '保存背景设置失败')
    }
    ElMessage.success('背景设置已保存')
  } catch (error) {
    ElMessage.error(error.message || '保存背景设置失败')
  } finally {
    savingBg.value = false
  }
}

async function fetchConfig() {
  loading.value = true
  try {
    const res = await getCurrentThemeConfig()
    if (res.code === 200 && res.data) {
      form.frontendTheme = res.data.frontendTheme || 'default'
      form.adminTheme = res.data.adminTheme || 'default'
      form.frontendStyle = normalizeFrontendStyleMode(res.data.frontendStyle)
      form.adminStyle = normalizeAdminStyleMode(res.data.adminStyle)
      frontendBgForm.bgColor = res.data.frontendBgColor || ''
      frontendBgForm.bgImage = res.data.frontendBgImage || ''
      frontendBgForm.bgOpacity = res.data.frontendBgOpacity ?? 15
      bgForm.bgColor = res.data.adminBgColor || ''
      bgForm.bgImage = res.data.adminBgImage || ''
      bgForm.bgOpacity = res.data.adminBgOpacity ?? 15
      previewBg()
      previewFrontendBg()
      return
    }
    throw new Error(res.msg || '获取主题配置失败')
  } catch (error) {
    ElMessage.error(error.message || '获取主题配置失败')
  } finally {
    loading.value = false
  }
}

async function fetchPortalLayoutConfig() {
  portalLoading.value = true
  try {
    const res = await getSystemConfigList()
    const configs = res.code === 200 ? res.data : []
    portalConfigRecord.value = findConfigRecord(configs, PORTAL_HOME_LAYOUT_CONFIG_KEY)
    applyPortalLayoutConfig(portalConfigRecord.value?.configValue)
  } catch (error) {
    ElMessage.error(error.message || '获取首页编排配置失败')
  } finally {
    portalLoading.value = false
  }
}

async function savePortalLayoutConfig() {
  savingPortal.value = true
  try {
    const payload = {
      configKey: PORTAL_HOME_LAYOUT_CONFIG_KEY,
      configValue: serializePortalHomeLayoutConfig(portalLayoutForm),
      description: '前台首页模块显示、名称和排序配置',
      isEnabled: true
    }
    if (portalConfigRecord.value?.id) {
      await updateSystemConfig(portalConfigRecord.value.id, payload)
    } else {
      const res = await createSystemConfig(payload)
      portalConfigRecord.value = res.data || null
    }
    ElMessage.success('首页编排配置已保存')
    await fetchPortalLayoutConfig()
  } catch (error) {
    ElMessage.error(error.message || '保存首页编排配置失败')
  } finally {
    savingPortal.value = false
  }
}

function resetPortalLayoutConfig() {
  applyPortalLayoutConfig()
}

onMounted(() => {
  fetchConfig()
  fetchPortalLayoutConfig()
})
</script>

<style scoped>
.theme-config-page .card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-actions {
  display: flex;
  gap: 10px;
}

.style-mode-panel {
  display: grid;
  gap: 16px;
  margin-bottom: 24px;
  padding: 18px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fbfdff;
}

.style-mode-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.style-mode-card {
  display: grid;
  gap: 12px;
  padding: 16px;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  background: #fff;
}

.style-mode-title {
  font-size: 14px;
  font-weight: 700;
  color: #303133;
}

.style-mode-card p {
  margin: 0;
  color: #737b8c;
  font-size: 13px;
  line-height: 1.6;
}

.theme-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 24px;
}

.theme-panel {
  display: grid;
  gap: 16px;
}

.panel-header h3 {
  margin: 0;
  font-size: 18px;
}

.panel-header p {
  margin: 6px 0 0;
  color: #909399;
  font-size: 13px;
}

.theme-options {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.theme-card {
  display: grid;
  gap: 10px;
  cursor: pointer;
  position: relative;
  padding: 10px;
  border-radius: 12px;
  border: 2px solid transparent;
  transition: all 0.2s;
}

.theme-card:hover {
  border-color: #c0c4cc;
  background: rgba(0, 0, 0, 0.02);
}

.theme-card.active {
  border-color: var(--el-color-primary, #409eff);
  background: rgba(64, 158, 255, 0.06);
  box-shadow: 0 0 0 1px var(--el-color-primary, #409eff);
}

.check-badge {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--el-color-primary, #409eff);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  z-index: 2;
}

.theme-card input {
  display: none;
}

.theme-preview {
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  overflow: hidden;
  min-height: 130px;
}

.preview-header {
  height: 34px;
  background: rgba(255, 255, 255, 0.18);
}

.preview-body {
  padding: 14px;
  display: grid;
  gap: 12px;
}

.preview-hero {
  height: 40px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.28);
}

.preview-row,
.preview-cards {
  display: flex;
  gap: 10px;
}

.swatch {
  display: inline-block;
  width: 44px;
  height: 18px;
  border-radius: 999px;
}

.admin-preview {
  display: grid;
  grid-template-columns: 46px 1fr;
}

.preview-sidebar {
  background: rgba(0, 0, 0, 0.18);
}

.preview-main {
  display: grid;
  grid-template-rows: 30px 1fr;
}

.preview-topbar {
  background: rgba(255, 255, 255, 0.22);
}

.preview-cards {
  padding: 14px;
}

.theme-meta {
  display: grid;
  gap: 4px;
}

.theme-meta strong {
  font-size: 14px;
}

.theme-meta span {
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
}

@media (max-width: 1080px) {
  .style-mode-grid,
  .theme-grid,
  .theme-options {
    grid-template-columns: 1fr;
  }
}

.bg-config {
  max-width: 600px;
}

.bg-image-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.bg-thumb {
  width: 60px;
  height: 40px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
}

.bg-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.portal-layout-config {
  display: grid;
  gap: 22px;
}

.layout-config-panel {
  display: grid;
  gap: 14px;
}

.layout-config-panel :deep(.el-input-number) {
  width: 100%;
}

@media (max-width: 760px) {
  .theme-config-page .card-header,
  .card-actions {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
