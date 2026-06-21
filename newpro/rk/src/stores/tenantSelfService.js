import { defineStore } from 'pinia'
import { getPublicTenantSelfServiceConfig } from '@/api/tenant-self-service'

const DEFAULT_CONFIG = {
  tenantId: null,
  brandSettings: {
    tenantDisplayName: '当前租户',
    slogan: '社团业务自助运营配置',
    contactEmail: 'contact@rk-web.org',
    contactAddress: '黑河学院'
  },
  portalSettings: {
    showNews: true,
    showActivities: true,
    showCompetitions: true,
    showAlumni: true,
    showWorks: true,
    allowPublicSearch: true
  },
  admissionSettings: {
    allowPublicRegister: true,
    allowJoinApplication: true,
    requireEmailVerification: true,
    allowReferralCode: true,
    maxPendingApplications: 200
  }
}

function normalizeConfig(data) {
  return {
    tenantId: data?.tenantId ?? null,
    brandSettings: {
      ...DEFAULT_CONFIG.brandSettings,
      ...(data?.brandSettings || {})
    },
    portalSettings: {
      ...DEFAULT_CONFIG.portalSettings,
      ...(data?.portalSettings || {})
    },
    admissionSettings: {
      ...DEFAULT_CONFIG.admissionSettings,
      ...(data?.admissionSettings || {})
    }
  }
}

export const useTenantSelfServiceStore = defineStore('tenantSelfService', {
  state: () => ({
    activeTenantId: null,
    config: normalizeConfig(),
    loading: false,
    loaded: false
  }),
  getters: {
    brandSettings: (state) => state.config.brandSettings,
    portalSettings: (state) => state.config.portalSettings,
    admissionSettings: (state) => state.config.admissionSettings
  },
  actions: {
    async loadPublicConfig(tenantId, force = false) {
      const key = tenantId ? String(tenantId) : ''
      if (!force && this.loaded && String(this.activeTenantId || '') === key) {
        return this.config
      }
      this.loading = true
      try {
        const res = await getPublicTenantSelfServiceConfig(tenantId)
        const data = res?.data || res || {}
        this.config = normalizeConfig(data)
        this.activeTenantId = key
        this.loaded = true
      } catch (error) {
        this.config = normalizeConfig({ tenantId })
        this.activeTenantId = key
        this.loaded = true
      } finally {
        this.loading = false
      }
      return this.config
    }
  }
})
