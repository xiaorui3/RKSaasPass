<template>
  <div class="review-page">
    <el-card class="review-card" v-loading="loading">
      <template #header>
        <div class="review-card__header">
          <div>
            <h1>{{ preview?.title || t('pages.reviewAction.processingTitle') }}</h1>
            <p>{{ previewDescription }}</p>
          </div>
          <el-tag :type="actionTagType" effect="light">
            {{ actionText }}
          </el-tag>
        </div>
      </template>

      <template v-if="errorMessage">
        <el-result icon="warning" :title="t('pages.reviewAction.linkUnavailable')" :sub-title="errorMessage">
          <template #extra>
            <el-button @click="reloadPreview">{{ t('pages.reviewAction.reload') }}</el-button>
          </template>
        </el-result>
      </template>

      <template v-else-if="successMessage">
        <el-result icon="success" :title="t('pages.reviewAction.completedTitle')" :sub-title="successMessage">
          <template #extra>
            <el-button type="primary" @click="router.push('/')">{{ t('pages.reviewAction.backHome') }}</el-button>
          </template>
        </el-result>
      </template>

      <template v-else-if="preview">
        <div class="review-meta">
          <div class="review-meta__item">
            <span class="label">{{ t('pages.reviewAction.targetType') }}</span>
            <span class="value">{{ preview.targetType }}</span>
          </div>
          <div class="review-meta__item">
            <span class="label">{{ t('pages.reviewAction.targetTenant') }}</span>
            <span class="value">{{ preview.tenantName || '-' }}</span>
          </div>
          <div class="review-meta__item">
            <span class="label">{{ t('pages.reviewAction.recordId') }}</span>
            <span class="value">#{{ preview.targetId }}</span>
          </div>
        </div>

        <div class="review-summary">
          <h2>{{ t('pages.reviewAction.summary') }}</h2>
          <div class="summary-html" v-html="preview.summaryHtml || `<p>${t('pages.reviewAction.noSummary')}</p>`"></div>
        </div>

        <div class="review-actions">
          <el-button @click="reloadPreview">{{ t('pages.reviewAction.refresh') }}</el-button>
          <el-button
            type="primary"
            :loading="submitting"
            :disabled="preview.executable === false"
            @click="handleExecute"
          >
            {{ actionButtonText }}
          </el-button>
        </div>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { executeReviewAction, previewReviewAction } from '@/api/admission'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const preview = ref(null)
const errorMessage = ref('')
const successMessage = ref('')

const token = computed(() => String(route.query.token || ''))
const actionText = computed(() => preview.value?.action === 'REJECT' ? t('pages.reviewAction.reject') : t('pages.reviewAction.approve'))
const actionTagType = computed(() => preview.value?.action === 'REJECT' ? 'danger' : 'success')
const actionButtonText = computed(() => preview.value?.action === 'REJECT' ? t('pages.reviewAction.confirmReject') : t('pages.reviewAction.confirmApprove'))
const previewDescription = computed(() => {
  if (!preview.value) {
    return t('pages.reviewAction.loadingDesc')
  }
  return `${t('pages.reviewAction.processingTitle')} ${preview.value.tenantName || '-'} / ${preview.value.title || '-'}`
})

const loadPreview = async () => {
  if (!token.value) {
    errorMessage.value = t('pages.reviewAction.missingToken')
    preview.value = null
    return
  }

  loading.value = true
  errorMessage.value = ''
  try {
    const res = await previewReviewAction(token.value)
    if (res.code !== 200 || !res.data) {
      throw new Error(res.msg || t('pages.reviewAction.previewLoadFailed'))
    }
    preview.value = res.data
  } catch (error) {
    preview.value = null
    errorMessage.value = error.message || t('pages.reviewAction.previewLoadFailed')
  } finally {
    loading.value = false
  }
}

const reloadPreview = async () => {
  successMessage.value = ''
  await loadPreview()
}

const handleExecute = async () => {
  if (!preview.value || !token.value) {
    return
  }

  const confirmText = preview.value.action === 'REJECT'
    ? t('pages.reviewAction.confirmRejectMessage')
    : t('pages.reviewAction.confirmApproveMessage')

  await ElMessageBox.confirm(confirmText, t('pages.reviewAction.executeTitle'), { type: 'warning' })

  submitting.value = true
  try {
    const res = await executeReviewAction(token.value)
    if (res.code !== 200 || res.data !== true) {
      throw new Error(res.msg || t('pages.reviewAction.executeFailed'))
    }
    successMessage.value = preview.value.action === 'REJECT'
      ? t('pages.reviewAction.rejectedMessage')
      : t('pages.reviewAction.approvedMessage')
    ElMessage.success(t('pages.reviewAction.successToast'))
  } catch (error) {
    ElMessage.error(error.message || t('pages.reviewAction.executeFailed'))
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadPreview()
})
</script>

<style scoped>
.review-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  background:
    radial-gradient(circle at top left, rgba(14, 165, 233, 0.12), transparent 28%),
    radial-gradient(circle at bottom right, rgba(16, 185, 129, 0.12), transparent 32%),
    #f8fafc;
}

.review-card {
  width: min(840px, 100%);
  border-radius: 20px;
}

.review-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.review-card__header h1 {
  margin: 0;
  font-size: 28px;
  line-height: 1.1;
  color: #0f172a;
}

.review-card__header p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
}

.review-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 24px;
}

.review-meta__item {
  padding: 14px 16px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.review-meta__item .label {
  display: block;
  margin-bottom: 6px;
  color: #64748b;
  font-size: 12px;
}

.review-meta__item .value {
  color: #0f172a;
  font-weight: 600;
}

.review-summary {
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 20px;
  background: #fff;
}

.review-summary h2 {
  margin: 0 0 16px;
  font-size: 18px;
  color: #0f172a;
}

.summary-html :deep(p) {
  margin: 0 0 12px;
  color: #334155;
}

.summary-html :deep(strong) {
  color: #0f172a;
}

.review-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}
</style>
