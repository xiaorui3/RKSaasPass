<template>
  <div class="tenant-self-service-config">
    <section class="page-header">
      <div>
        <h1>租户自助配置</h1>
        <p>集中维护当前租户的品牌展示、门户开关、入社申请、审核流程、通知策略和容量限制。</p>
      </div>
      <div class="header-actions">
        <el-tag type="info">租户 {{ tenantSelfServiceConfig.tenantId || '-' }}</el-tag>
        <el-button :loading="loading" @click="loadTenantSelfServiceConfig">刷新</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存配置</el-button>
      </div>
    </section>

    <el-form v-loading="loading" label-position="top" class="config-form">
      <section class="config-grid">
        <el-card class="config-panel">
          <template #header>
            <div class="panel-header">
              <strong>品牌展示</strong>
              <span>前台基础识别信息</span>
            </div>
          </template>
          <el-form-item label="租户显示名称">
            <el-input v-model="brandSettings.tenantDisplayName" maxlength="40" show-word-limit />
          </el-form-item>
          <el-form-item label="展示标语">
            <el-input v-model="brandSettings.slogan" maxlength="80" show-word-limit />
          </el-form-item>
          <el-form-item label="联系邮箱">
            <el-input v-model="brandSettings.contactEmail" />
          </el-form-item>
          <el-form-item label="联系地址">
            <el-input v-model="brandSettings.contactAddress" />
          </el-form-item>
        </el-card>

        <el-card class="config-panel">
          <template #header>
            <div class="panel-header">
              <strong>门户开关</strong>
              <span>控制公开页面展示范围</span>
            </div>
          </template>
          <div class="switch-list">
            <label><span>新闻动态</span><el-switch v-model="portalSettings.showNews" /></label>
            <label><span>活动中心</span><el-switch v-model="portalSettings.showActivities" /></label>
            <label><span>比赛活动</span><el-switch v-model="portalSettings.showCompetitions" /></label>
            <label><span>校友风采</span><el-switch v-model="portalSettings.showAlumni" /></label>
            <label><span>作品展示</span><el-switch v-model="portalSettings.showWorks" /></label>
            <label><span>公开搜索</span><el-switch v-model="portalSettings.allowPublicSearch" /></label>
          </div>
        </el-card>

        <el-card class="config-panel">
          <template #header>
            <div class="panel-header">
              <strong>入社申请</strong>
              <span>控制注册和申请入口</span>
            </div>
          </template>
          <div class="switch-list">
            <label><span>允许公开注册</span><el-switch v-model="admissionSettings.allowPublicRegister" /></label>
            <label><span>允许入社申请</span><el-switch v-model="admissionSettings.allowJoinApplication" /></label>
            <label><span>邮箱验证码</span><el-switch v-model="admissionSettings.requireEmailVerification" /></label>
            <label><span>启用内推码</span><el-switch v-model="admissionSettings.allowReferralCode" /></label>
          </div>
          <el-form-item label="待处理申请上限">
            <el-input-number v-model="admissionSettings.maxPendingApplications" :min="1" :max="5000" :step="10" />
          </el-form-item>
        </el-card>

        <el-card class="config-panel">
          <template #header>
            <div class="panel-header">
              <strong>审核流程</strong>
              <span>控制默认审核要求</span>
            </div>
          </template>
          <div class="switch-list">
            <label><span>指导老师审核</span><el-switch v-model="reviewSettings.requireTeacherReview" /></label>
            <label><span>负责人审核</span><el-switch v-model="reviewSettings.requireClubManagerReview" /></label>
            <label><span>超期自动驳回</span><el-switch v-model="reviewSettings.autoRejectExpired" /></label>
          </div>
          <el-form-item label="审核 SLA 小时">
            <el-input-number v-model="reviewSettings.reviewSlaHours" :min="1" :max="720" :step="1" />
          </el-form-item>
        </el-card>

        <el-card class="config-panel">
          <template #header>
            <div class="panel-header">
              <strong>通知策略</strong>
              <span>控制站内和邮件通知</span>
            </div>
          </template>
          <div class="switch-list">
            <label><span>邮件通知</span><el-switch v-model="notificationSettings.emailNoticeEnabled" /></label>
            <label><span>站内通知</span><el-switch v-model="notificationSettings.siteNoticeEnabled" /></label>
            <label><span>审核通知</span><el-switch v-model="notificationSettings.approvalNoticeEnabled" /></label>
            <label><span>周报摘要</span><el-switch v-model="notificationSettings.weeklyDigestEnabled" /></label>
          </div>
        </el-card>

        <el-card class="config-panel">
          <template #header>
            <div class="panel-header">
              <strong>容量限制</strong>
              <span>控制租户业务规模阈值</span>
            </div>
          </template>
          <div class="number-grid">
            <el-form-item label="成员上限">
              <el-input-number v-model="quotaSettings.maxClubMembers" :min="1" :max="100000" />
            </el-form-item>
            <el-form-item label="进行中活动上限">
              <el-input-number v-model="quotaSettings.maxActiveActivities" :min="1" :max="1000" />
            </el-form-item>
            <el-form-item label="每月新闻上限">
              <el-input-number v-model="quotaSettings.maxMonthlyNews" :min="1" :max="5000" />
            </el-form-item>
            <el-form-item label="存储上限 MB">
              <el-input-number v-model="quotaSettings.maxStorageMb" :min="100" :max="1048576" :step="100" />
            </el-form-item>
          </div>
        </el-card>
      </section>
    </el-form>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getTenantSelfServiceConfig,
  saveTenantSelfServiceConfig
} from '@/api/tenant-self-service'

const loading = ref(false)
const saving = ref(false)

const tenantSelfServiceConfig = reactive(createDefaultConfig())

const brandSettings = computed(() => tenantSelfServiceConfig.brandSettings)
const portalSettings = computed(() => tenantSelfServiceConfig.portalSettings)
const admissionSettings = computed(() => tenantSelfServiceConfig.admissionSettings)
const reviewSettings = computed(() => tenantSelfServiceConfig.reviewSettings)
const notificationSettings = computed(() => tenantSelfServiceConfig.notificationSettings)
const quotaSettings = computed(() => tenantSelfServiceConfig.quotaSettings)

function createDefaultConfig() {
  return {
    tenantId: null,
    brandSettings: {
      tenantDisplayName: '',
      slogan: '',
      contactEmail: '',
      contactAddress: ''
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
    },
    reviewSettings: {
      requireTeacherReview: true,
      requireClubManagerReview: true,
      autoRejectExpired: false,
      reviewSlaHours: 72
    },
    notificationSettings: {
      emailNoticeEnabled: true,
      siteNoticeEnabled: true,
      approvalNoticeEnabled: true,
      weeklyDigestEnabled: false
    },
    quotaSettings: {
      maxClubMembers: 500,
      maxActiveActivities: 30,
      maxMonthlyNews: 60,
      maxStorageMb: 2048
    }
  }
}

function unwrapResponse(res) {
  if (res?.code && res.code !== 200) {
    throw new Error(res.msg || '租户自助配置加载失败')
  }
  return res?.data || res || {}
}

function assignTenantSelfServiceConfig(data) {
  const defaults = createDefaultConfig()
  Object.assign(tenantSelfServiceConfig, {
    tenantId: data.tenantId ?? null,
    brandSettings: { ...defaults.brandSettings, ...(data.brandSettings || {}) },
    portalSettings: { ...defaults.portalSettings, ...(data.portalSettings || {}) },
    admissionSettings: { ...defaults.admissionSettings, ...(data.admissionSettings || {}) },
    reviewSettings: { ...defaults.reviewSettings, ...(data.reviewSettings || {}) },
    notificationSettings: { ...defaults.notificationSettings, ...(data.notificationSettings || {}) },
    quotaSettings: { ...defaults.quotaSettings, ...(data.quotaSettings || {}) }
  })
}

async function loadTenantSelfServiceConfig() {
  loading.value = true
  try {
    const res = await getTenantSelfServiceConfig()
    assignTenantSelfServiceConfig(unwrapResponse(res))
  } catch (error) {
    ElMessage.error(error.message || '租户自助配置加载失败')
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    const res = await saveTenantSelfServiceConfig(tenantSelfServiceConfig)
    assignTenantSelfServiceConfig(unwrapResponse(res))
    ElMessage.success('租户自助配置已保存')
  } catch (error) {
    ElMessage.error(error.message || '租户自助配置保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadTenantSelfServiceConfig()
})
</script>

<style scoped>
.tenant-self-service-config {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.page-header h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #1f2937;
}

.page-header p {
  margin: 8px 0 0;
  color: #667085;
  line-height: 1.6;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.config-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.config-panel {
  min-height: 100%;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.panel-header strong {
  font-size: 15px;
  color: #1f2937;
}

.panel-header span {
  color: #667085;
  font-size: 12px;
}

.switch-list {
  display: grid;
  gap: 12px;
}

.switch-list label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 32px;
  color: #344054;
}

.number-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

:deep(.el-input-number) {
  width: 100%;
}

@media (max-width: 960px) {
  .page-header,
  .panel-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    justify-content: flex-start;
  }

  .config-grid,
  .number-grid {
    grid-template-columns: 1fr;
  }
}
</style>
