import { defineStore } from 'pinia'
import { getCurrentThemeConfig, getPublicThemeConfig } from '@/api/theme-config'
import { THEME_PRESETS } from '@/config/theme-presets'
import { resolveMediaUrl } from '@/utils/mediaUrl'

const FRONTEND_CONTEXT = 'frontend'
const ADMIN_CONTEXT = 'admin'
const STYLE_PORTAL = 'portal'
const STYLE_CLASSIC = 'classic'
const STYLE_BIP = 'bip'

let themeApplySeq = 0

function nextThemeApplySeq() {
  themeApplySeq += 1
  return themeApplySeq
}

function isLatestThemeApply(applySeq) {
  return applySeq === themeApplySeq
}

const DEFAULT_CONFIG = {
  frontendTheme: 'default',
  adminTheme: 'default',
  frontendStyle: 'portal',
  adminStyle: 'classic',
  frontendBgColor: '',
  frontendBgImage: '',
  frontendBgOpacity: 15,
  adminBgColor: '',
  adminBgImage: '',
  adminBgOpacity: 15
}

function normalizeFrontendStyleMode(value) {
  const style = String(value || '').toLowerCase()
  if (style === STYLE_CLASSIC || style === STYLE_BIP) {
    return style
  }
  return STYLE_PORTAL
}

function normalizeAdminStyleMode(value) {
  return String(value || '').toLowerCase() === STYLE_BIP ? STYLE_BIP : STYLE_CLASSIC
}

function normalizeConfig(data) {
  return {
    frontendTheme: data?.frontendTheme || 'default',
    adminTheme: data?.adminTheme || 'default',
    frontendStyle: normalizeFrontendStyleMode(data?.frontendStyle),
    adminStyle: normalizeAdminStyleMode(data?.adminStyle),
    frontendBgColor: data?.frontendBgColor || '',
    frontendBgImage: data?.frontendBgImage || '',
    frontendBgOpacity: data?.frontendBgOpacity ?? 15,
    adminBgColor: data?.adminBgColor || '',
    adminBgImage: data?.adminBgImage || '',
    adminBgOpacity: data?.adminBgOpacity ?? 15
  }
}

function applyUiStyleMode(context, styleMode) {
  const root = document.documentElement
  const normalized = context === ADMIN_CONTEXT ? normalizeAdminStyleMode(styleMode) : normalizeFrontendStyleMode(styleMode)
  root.dataset.rkUiStyle = normalized
  root.dataset.rkUiContext = context
  if (context === ADMIN_CONTEXT) {
    root.dataset.rkAdminUiStyle = normalized
  } else {
    root.dataset.rkFrontendUiStyle = normalized
  }
  root.setAttribute('data-rk-ui-style', normalized)
  root.setAttribute('data-rk-ui-context', context)
}

function applyCssVars(vars) {
  const root = document.documentElement
  Object.entries(vars).forEach(([key, value]) => {
    root.style.setProperty(key, value)
  })
}

function applyAdminBg(bgColor, bgImage, bgOpacity) {
  const root = document.documentElement
  if (bgColor) {
    root.style.setProperty('--rk-admin-body-bg', bgColor)
  } else {
    root.style.removeProperty('--rk-admin-body-bg')
  }
  if (bgImage) {
    root.style.setProperty('--rk-admin-bg-image', `url(${resolveMediaUrl(bgImage)})`)
    root.style.setProperty('--rk-admin-bg-image-opacity', String((bgOpacity ?? 15) / 100))
  } else {
    root.style.removeProperty('--rk-admin-bg-image')
    root.style.removeProperty('--rk-admin-bg-image-opacity')
  }
}

function applyFrontendBg(bgColor, bgImage, bgOpacity) {
  const root = document.documentElement
  if (bgColor) {
    root.style.setProperty('--rk-body-bg', bgColor)
  }
  if (bgImage) {
    root.style.setProperty('--rk-frontend-bg-image', `url(${resolveMediaUrl(bgImage)})`)
    root.style.setProperty('--rk-frontend-bg-image-opacity', String((bgOpacity ?? 15) / 100))
  } else {
    root.style.removeProperty('--rk-frontend-bg-image')
    root.style.removeProperty('--rk-frontend-bg-image-opacity')
  }
}

function applyThemePreset(context, presetKey) {
  const preset = THEME_PRESETS[presetKey] || THEME_PRESETS.default
  const vars = context === ADMIN_CONTEXT ? preset.admin : preset.frontend
  applyCssVars(vars)
  document.documentElement.dataset.themeContext = context
  document.documentElement.dataset.themePreset = preset.key
  document.documentElement.style.setProperty('--el-color-primary', vars['--rk-primary'])
  document.documentElement.style.setProperty('--el-color-primary-dark-2', vars['--rk-primary-dark'])
}

export const useThemeStore = defineStore('theme', {
  state: () => ({
    configByTenant: {},
    activeTenantId: '',
    activeContext: FRONTEND_CONTEXT,
    activeConfig: { ...DEFAULT_CONFIG }
  }),
  actions: {
    async fetchPublicConfig(tenantId) {
      if (!tenantId) {
        return { ...DEFAULT_CONFIG }
      }
      const key = String(tenantId)
      if (this.configByTenant[key]) {
        return this.configByTenant[key]
      }
      try {
        const res = await getPublicThemeConfig(key)
        if (res.code === 200) {
          const config = normalizeConfig(res.data)
          this.configByTenant[key] = config
          return config
        }
      } catch (error) {
        console.error('fetch public theme config failed:', error)
      }
      this.configByTenant[key] = { ...DEFAULT_CONFIG }
      return this.configByTenant[key]
    },
    async fetchCurrentConfig(tenantId) {
      const key = tenantId ? String(tenantId) : ''
      try {
        const res = await getCurrentThemeConfig()
        if (res.code === 200) {
          const config = normalizeConfig(res.data)
          if (key) {
            this.configByTenant[key] = config
          }
          return config
        }
      } catch (error) {
        console.error('fetch current theme config failed:', error)
      }
      return this.fetchPublicConfig(key)
    },
    async applyTenantTheme(tenantId, context = FRONTEND_CONTEXT, useCurrent = false) {
      const applySeq = nextThemeApplySeq()
      const key = tenantId ? String(tenantId) : ''
      const config = useCurrent
        ? await this.fetchCurrentConfig(key)
        : await this.fetchPublicConfig(key)
      if (!isLatestThemeApply(applySeq)) {
        return config
      }
      this.activeTenantId = key
      this.activeContext = context
      this.activeConfig = config
      applyThemePreset(context, context === ADMIN_CONTEXT ? config.adminTheme : config.frontendTheme)
      applyUiStyleMode(context, context === ADMIN_CONTEXT ? config.adminStyle : config.frontendStyle)
      if (context === ADMIN_CONTEXT) {
        applyAdminBg(config.adminBgColor, config.adminBgImage, config.adminBgOpacity)
      } else {
        applyFrontendBg(config.frontendBgColor, config.frontendBgImage, config.frontendBgOpacity)
      }
    },
    applyPreviewTheme(context, presetKey) {
      nextThemeApplySeq()
      applyThemePreset(context, presetKey)
    },
    applyPreviewStyleMode(context, styleMode) {
      nextThemeApplySeq()
      applyUiStyleMode(context, styleMode)
    },
    clearTheme() {
      nextThemeApplySeq()
      this.activeTenantId = ''
      this.activeContext = FRONTEND_CONTEXT
      this.activeConfig = { ...DEFAULT_CONFIG }
      applyThemePreset(FRONTEND_CONTEXT, 'default')
      applyUiStyleMode(FRONTEND_CONTEXT, STYLE_PORTAL)
      document.documentElement.style.removeProperty('--rk-admin-bg-image')
      document.documentElement.style.removeProperty('--rk-admin-bg-image-opacity')
      document.documentElement.style.removeProperty('--rk-frontend-bg-image')
      document.documentElement.style.removeProperty('--rk-frontend-bg-image-opacity')
    }
  }
})
