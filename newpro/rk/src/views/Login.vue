<template>
  <div class="login-container">
    <video class="video-background" autoplay loop muted playsinline poster="/assets/visuals/rk-campus-hero.png">
      <source src="@/static/logo.mp4" type="video/mp4">
      {{ t('auth.currentBrowserNoVideo') }}
    </video>

    <div class="video-overlay"></div>

    <main class="login-shell">
      <section class="login-visual-panel" aria-hidden="true">
        <div class="visual-copy">
          <span class="visual-kicker">RK Club</span>
          <h1>{{ admissionConfig.pageTitle }}</h1>
          <p>{{ admissionConfig.pageDescription }}</p>
        </div>
        <div class="login-feature-grid">
          <div>
            <strong>15 天</strong>
            <span>移动端登录保持</span>
          </div>
          <div>
            <strong>HTTPS</strong>
            <span>默认官网入口</span>
          </div>
          <div>
            <strong>多租户</strong>
            <span>自动识别社团</span>
          </div>
        </div>
      </section>

      <section class="login-auth-panel">
        <div class="auth-heading">
          <span class="auth-brand">RK Club</span>
          <h2>{{ isPasswordMode ? t('auth.accountPasswordLogin') : t('auth.emailCodeLogin') }}</h2>
          <p>{{ t('auth.emailCodeLogin') }} / {{ t('auth.accountPasswordLogin') }}</p>
        </div>

        <div class="tenant-chip-row">
          <el-form ref="tenantFormRef" :model="loginForm" :rules="rules" label-position="top" class="tenant-inline-form">
            <el-form-item prop="organizationId" class="tenant-form-item">
              <el-popover
                placement="bottom-start"
                trigger="click"
                width="300"
                popper-class="tenant-popover"
              >
                <template #reference>
                  <button type="button" class="tenant-chip" :class="{ loading: tenantLoading }">
                    <span>{{ t('auth.tenant') }}</span>
                    <strong>{{ currentTenantName || t('auth.selectTenant') }}</strong>
                  </button>
                </template>

                <div class="tenant-menu">
                  <button
                    v-for="tenant in tenantList"
                    :key="tenant.id"
                    type="button"
                    class="tenant-menu-item"
                    :class="{ active: String(tenant.id) === String(loginForm.organizationId) }"
                    @click="selectLoginTenant(tenant)"
                  >
                    {{ tenant.tenantName }}
                  </button>
                  <div v-if="tenantList.length === 0" class="tenant-empty">
                    {{ tenantLoading ? '加载中...' : t('auth.selectTenant') }}
                  </div>
                </div>
              </el-popover>
            </el-form-item>
          </el-form>
        </div>

        <el-tabs v-model="loginMode" stretch class="login-tabs">
          <el-tab-pane :label="t('auth.accountPasswordLogin')" name="password" />
          <el-tab-pane :label="t('auth.emailCodeLogin')" name="email" />
        </el-tabs>

        <el-form ref="loginFormRef" :model="loginForm" :rules="rules" label-position="top" class="auth-form">
          <template v-if="isPasswordMode">
            <el-form-item :label="t('auth.username')" prop="username">
              <el-input
                v-model="loginForm.username"
                :placeholder="t('auth.inputUsername')"
                prefix-icon="User"
                @keyup.enter="handleLogin"
              />
            </el-form-item>

            <el-form-item :label="t('auth.password')" prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                :placeholder="t('auth.inputPassword')"
                prefix-icon="Lock"
                show-password
                @keyup.enter="handleLogin"
              />
            </el-form-item>

            <el-alert
              v-if="requiresDormantVerification"
              :title="t('auth.dormantVerificationWarning')"
              type="warning"
              show-icon
              :closable="false"
              class="verification-alert"
            />

            <el-form-item v-if="requiresDormantVerification" :label="t('auth.email')" prop="email">
              <el-input
                v-model="loginForm.email"
                :placeholder="t('auth.inputBoundEmail')"
                @keyup.enter="handleLogin"
              />
            </el-form-item>

            <el-form-item v-if="requiresDormantVerification" :label="t('auth.emailCode')" prop="emailCode">
              <div class="email-code-row">
                <el-input
                  v-model="loginForm.emailCode"
                  :placeholder="t('auth.inputEmailCode')"
                  @keyup.enter="handleLogin"
                />
                <el-button :disabled="!sendCodeReady" @click="handleSendCode('password')">
                  {{ sendCodeText }}
                </el-button>
              </div>
            </el-form-item>
          </template>

          <template v-else>
            <el-alert
              :title="t('auth.verifyEmailBeforeLogin')"
              type="info"
              show-icon
              :closable="false"
              class="verification-alert"
            />

            <el-form-item :label="t('auth.email')" prop="email">
              <el-input
                v-model="loginForm.email"
                :placeholder="t('auth.inputAccountEmail')"
                @keyup.enter="handleLogin"
              />
            </el-form-item>

            <el-form-item :label="t('auth.emailCode')" prop="emailCode">
              <div class="email-code-row">
                <el-input
                  v-model="loginForm.emailCode"
                  :placeholder="t('auth.inputEmailCode')"
                  @keyup.enter="handleLogin"
                />
                <el-button :disabled="!sendCodeReady" @click="handleSendCode('email')">
                  {{ sendCodeText }}
                </el-button>
              </div>
            </el-form-item>
          </template>

          <el-form-item>
            <el-button type="primary" class="login-submit" :loading="loading" @click="handleLogin">
              {{ loading ? t('auth.loginLoading') : isPasswordMode ? t('auth.loginButton') : t('auth.verifyAndLogin') }}
            </el-button>
          </el-form-item>

          <el-form-item>
            <div class="login-footer">
              <el-link type="primary" @click="$router.push('/register')">{{ t('auth.goRegisterLink') }}</el-link>
            </div>
          </el-form-item>
        </el-form>
      </section>
    </main>

    <el-dialog
      v-model="candidateDialogVisible"
      :title="t('auth.chooseLoginAccount')"
      width="460px"
      :close-on-click-modal="false"
    >
      <el-radio-group v-model="selectedCandidateKey" class="candidate-list">
        <el-radio
          v-for="candidate in emailLoginCandidates"
          :key="buildCandidateKey(candidate)"
          :label="buildCandidateKey(candidate)"
          class="candidate-item"
        >
          <div class="candidate-title">{{ candidate.label }}</div>
          <div class="candidate-meta">{{ candidate.username }}</div>
        </el-radio>
      </el-radio-group>

      <template #footer>
        <el-button @click="candidateDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="candidateSubmitting" @click="handleConfirmCandidate">
          {{ t('auth.confirmLogin') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPublicAdmissionFormConfig } from '@/api/admission'
import { getPublicTenantList } from '@/api/tenant'
import { prepareEmailLogin, sendEmailVerificationCode } from '@/api/user'
import { useThemeStore } from '@/stores/theme'
import { useUserStore } from '@/stores/user'
import { mapEmailLoginCandidates } from '@/utils/emailLogin'
import { getDefaultTenantId } from '@/utils/runtimeConfig'
import { normalizeTenantDirectory } from '@/utils/tenantBranding'

const { t, locale } = useI18n()
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const themeStore = useThemeStore()

const loginFormRef = ref(null)
const tenantFormRef = ref(null)
const loginMode = ref('password')
const loading = ref(false)
const tenantLoading = ref(false)
const tenantList = ref([])
const requiresDormantVerification = ref(false)
const sendCodeReady = ref(true)
const sendCodeText = ref(t('auth.sendCode'))
const candidateDialogVisible = ref(false)
const candidateSubmitting = ref(false)
const selectedCandidateKey = ref('')
const pendingLoginTicket = ref('')
const emailLoginCandidates = ref([])
let countdownTimer = null
const LAST_LOGIN_TENANT_KEY = 'lastLoginTenantId'

const defaultAdmissionConfig = () => ({
  pageTitle: t('auth.loginConfigFallbackTitle'),
  pageDescription: t('auth.loginConfigFallbackDescription')
})

const admissionConfig = ref(defaultAdmissionConfig())

const loginForm = ref({
  organizationId: '',
  username: '',
  password: '',
  email: '',
  emailCode: ''
})

const isPasswordMode = computed(() => loginMode.value === 'password')

const currentTenantName = computed(() => {
  const current = tenantList.value.find((tenant) => String(tenant.id) === String(loginForm.value.organizationId))
  return current?.tenantName || ''
})

const rules = computed(() => {
  if (loginMode.value === 'email') {
    return {
      email: [
        { required: true, message: t('auth.inputEmail'), trigger: 'blur' },
        { type: 'email', message: t('auth.inputEmail'), trigger: 'blur' }
      ],
      emailCode: [{ required: true, message: t('auth.inputEmailCode'), trigger: 'blur' }]
    }
  }

  return {
    organizationId: [{ required: true, message: t('auth.selectTenant'), trigger: 'change' }],
    username: [{ required: true, message: t('auth.inputUsername'), trigger: 'blur' }],
    password: [{ required: true, message: t('auth.inputPassword'), trigger: 'blur' }],
    ...(requiresDormantVerification.value
      ? {
          email: [
            { required: true, message: t('auth.inputEmail'), trigger: 'blur' },
            { type: 'email', message: t('auth.inputEmail'), trigger: 'blur' }
          ],
          emailCode: [{ required: true, message: t('auth.inputEmailCode'), trigger: 'blur' }]
        }
      : {})
  }
})

const buildCandidateKey = (candidate) => `${candidate.authUserId}:${candidate.tenantId}:${candidate.roleId || ''}`

const selectedCandidate = computed(() =>
  emailLoginCandidates.value.find((item) => buildCandidateKey(item) === selectedCandidateKey.value) || null
)

const isDormantVerificationMessage = (message) =>
  ['Dormant login requires email verification', 'Email verification failed'].includes(message)

const normalizeTenantCandidate = (value) => {
  const rawValue = Array.isArray(value) ? value[0] : value
  if (rawValue === undefined || rawValue === null) {
    return ''
  }
  return String(rawValue).trim()
}

const selectLoginTenant = (tenant) => {
  if (!tenant?.id) {
    return
  }
  loginForm.value.organizationId = String(tenant.id)
  tenantFormRef.value?.clearValidate?.()
}

const resolvePreferredTenantId = (tenants) => {
  const tenantIds = new Set((tenants || []).map((tenant) => String(tenant.id)))
  const candidates = [
    route.query.tenantId,
    localStorage.getItem(LAST_LOGIN_TENANT_KEY),
    localStorage.getItem('tenantId'),
    getDefaultTenantId()
  ]

  for (const candidate of candidates) {
    const tenantId = normalizeTenantCandidate(candidate)
    if (tenantId && tenantIds.has(tenantId)) {
      return tenantId
    }
  }

  return tenants.length === 1 ? String(tenants[0].id) : ''
}

const fetchTenantList = async () => {
  tenantLoading.value = true
  try {
    const res = await getPublicTenantList()
    if (res.code === 200 && Array.isArray(res.data)) {
      tenantList.value = normalizeTenantDirectory(res.data)
      const tenantIds = new Set(tenantList.value.map((tenant) => String(tenant.id)))
      const currentTenantId = normalizeTenantCandidate(loginForm.value.organizationId)
      if (!currentTenantId || !tenantIds.has(currentTenantId)) {
        loginForm.value.organizationId = resolvePreferredTenantId(tenantList.value)
      }
    }
  } catch (error) {
    console.error('failed to load tenant list:', error)
  } finally {
    tenantLoading.value = false
  }
}

const fetchAdmissionConfig = async (tenantId) => {
  if (!tenantId) {
    admissionConfig.value = defaultAdmissionConfig()
    return
  }
  try {
    const res = await getPublicAdmissionFormConfig(tenantId)
    if (res.code === 200 && res.data) {
      admissionConfig.value = {
        pageTitle: res.data.pageTitle || defaultAdmissionConfig().pageTitle,
        pageDescription: res.data.pageDescription || defaultAdmissionConfig().pageDescription
      }
      return
    }
  } catch (error) {
    console.error('failed to load login config:', error)
  }
  admissionConfig.value = defaultAdmissionConfig()
}

const startCountdown = () => {
  let seconds = 60
  sendCodeReady.value = false
  sendCodeText.value = `${seconds}s`
  countdownTimer = setInterval(() => {
    seconds -= 1
    if (seconds <= 0) {
      clearInterval(countdownTimer)
      countdownTimer = null
      sendCodeReady.value = true
      sendCodeText.value = t('auth.sendCode')
      return
    }
    sendCodeText.value = `${seconds}s`
  }, 1000)
}

const resetSendCodeState = () => {
  sendCodeReady.value = true
  sendCodeText.value = t('auth.sendCode')
}

const normalizeRedirectTarget = (target) => {
  const value = Array.isArray(target) ? target[0] : target
  if (typeof value !== 'string') {
    return ''
  }
  const redirect = value.trim()
  if (!redirect || !redirect.startsWith('/') || redirect.startsWith('//')) {
    return ''
  }
  return redirect
}

const resolveAuthenticatedLanding = () => {
  const redirect = normalizeRedirectTarget(route.query.redirect)
  if (redirect) {
    return redirect
  }
  return userStore.canEnterAdminConsole() ? '/admin' : '/'
}

const redirectAfterLogin = () => {
  router.push(resolveAuthenticatedLanding())
}

const handleSendCode = async (mode) => {
  const email = loginForm.value.email?.trim()
  if (!email) {
    ElMessage.warning(t('auth.inputEmailFirst'))
    return
  }
  if (mode === 'password' && !loginForm.value.organizationId) {
    ElMessage.warning(t('auth.inputTenantFirst'))
    return
  }
  if (!sendCodeReady.value) {
    return
  }

  try {
    sendCodeReady.value = false
    sendCodeText.value = t('auth.sending')
    const res = await sendEmailVerificationCode({
      email,
      tenantId: mode === 'password' ? Number(loginForm.value.organizationId) : 0,
      scene: 'LOGIN'
    })
    if (res.code === 200) {
      ElMessage.success(t('auth.sendCodeSuccess'))
      startCountdown()
      return
    }
    throw new Error(res.msg || t('auth.sendCodeFailed'))
  } catch (error) {
    resetSendCodeState()
    ElMessage.error(error.message || t('auth.sendCodeFailed'))
  }
}

const handlePasswordLogin = async () => {
  const result = await userStore.login({
    username: loginForm.value.username,
    password: loginForm.value.password,
    organizationId: loginForm.value.organizationId,
    email: loginForm.value.email,
    emailCode: loginForm.value.emailCode
  })

  if (result.success) {
    localStorage.setItem(LAST_LOGIN_TENANT_KEY, String(loginForm.value.organizationId))
    requiresDormantVerification.value = false
    loginForm.value.emailCode = ''
    ElMessage.success(t('auth.loginSuccess'))
    redirectAfterLogin()
    return
  }

  if (isDormantVerificationMessage(result.message)) {
    requiresDormantVerification.value = true
  }
  ElMessage.error(result.message || t('auth.loginFailed'))
}

const finishEmailLogin = async (candidate) => {
  const result = await userStore.loginWithEmailConfirm({
    loginTicket: pendingLoginTicket.value,
    authUserId: candidate.authUserId,
    tenantId: candidate.tenantId,
    roleId: candidate.roleId
  })

  if (result.success) {
    candidateDialogVisible.value = false
    pendingLoginTicket.value = ''
    emailLoginCandidates.value = []
    loginForm.value.emailCode = ''
    ElMessage.success(t('auth.loginSuccess'))
    redirectAfterLogin()
    return
  }

  ElMessage.error(result.message || t('auth.emailLoginFailed'))
}

const handleEmailModeLogin = async () => {
  const res = await prepareEmailLogin({
    email: loginForm.value.email,
    emailCode: loginForm.value.emailCode
  })

  if (res.code !== 200 || !res.data) {
    ElMessage.error(res.message || t('auth.emailLoginFailed'))
    return
  }

  pendingLoginTicket.value = res.data.loginTicket || ''
  emailLoginCandidates.value = mapEmailLoginCandidates(res.data.candidates || [], tenantList.value)
  if (emailLoginCandidates.value.length === 0) {
    ElMessage.error(t('auth.emailLoginNoCandidate'))
    return
  }

  if (emailLoginCandidates.value.length === 1) {
    await finishEmailLogin(emailLoginCandidates.value[0])
    return
  }

  selectedCandidateKey.value = buildCandidateKey(emailLoginCandidates.value[0])
  candidateDialogVisible.value = true
}

const handleConfirmCandidate = async () => {
  if (!selectedCandidate.value) {
    ElMessage.warning(t('auth.chooseOneAccount'))
    return
  }
  candidateSubmitting.value = true
  try {
    await finishEmailLogin(selectedCandidate.value)
  } finally {
    candidateSubmitting.value = false
  }
}

const handleLogin = async () => {
  if (!loginFormRef.value) {
    return
  }

  try {
    if (isPasswordMode.value && tenantFormRef.value) {
      await tenantFormRef.value.validate()
    }
    await loginFormRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    if (loginMode.value === 'email') {
      await handleEmailModeLogin()
    } else {
      await handlePasswordLogin()
    }
  } finally {
    loading.value = false
  }
}

watch(
  () => loginForm.value.organizationId,
  async (tenantId) => {
    if (!tenantId) {
      return
    }
    await Promise.all([
      themeStore.applyTenantTheme(tenantId, 'frontend'),
      fetchAdmissionConfig(tenantId)
    ])
  }
)

watch(loginMode, () => {
  requiresDormantVerification.value = false
  candidateDialogVisible.value = false
  pendingLoginTicket.value = ''
  emailLoginCandidates.value = []
  selectedCandidateKey.value = ''
  loginForm.value.emailCode = ''
  if (loginFormRef.value) {
    loginFormRef.value.clearValidate()
  }
})

watch(() => locale.value, () => {
  if (sendCodeReady.value) {
    sendCodeText.value = t('auth.sendCode')
  }
  if (!loginForm.value.organizationId) {
    admissionConfig.value = defaultAdmissionConfig()
  }
})

onMounted(() => {
  if (userStore.isLoggedIn) {
    router.replace(resolveAuthenticatedLanding())
    return
  }
  fetchTenantList()
})

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
})
</script>

<style scoped>
.login-container {
  position: relative;
  display: grid;
  place-items: center;
  min-height: 100vh;
  padding: 28px;
  overflow: hidden;
}

.login-container::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: -3;
  background-image: url('/assets/visuals/rk-campus-hero.png');
  background-size: cover;
  background-position: center;
}

.video-background {
  position: absolute;
  top: 50%;
  left: 50%;
  min-width: 100%;
  min-height: 100%;
  width: auto;
  height: auto;
  transform: translate(-50%, -50%);
  z-index: -2;
  object-fit: cover;
}

.video-overlay {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(135deg, rgba(9, 31, 38, 0.76), rgba(10, 48, 43, 0.44)),
    rgba(0, 0, 0, 0.22);
  z-index: -1;
}

.login-shell {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(300px, 0.95fr) minmax(360px, 440px);
  width: min(980px, 100%);
  min-height: 620px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 28px 80px rgba(4, 23, 31, 0.34);
}

.login-visual-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 100%;
  padding: 34px;
  color: #fff;
  background:
    linear-gradient(180deg, rgba(8, 28, 33, 0.12), rgba(8, 28, 33, 0.74)),
    url('/assets/visuals/rk-campus-hero.png') center / cover;
}

.visual-copy {
  width: min(360px, 100%);
}

.visual-kicker,
.auth-brand {
  color: #e8a923;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}

.visual-copy h1 {
  margin: 12px 0 10px;
  font-size: 34px;
  line-height: 1.18;
  letter-spacing: 0;
}

.visual-copy p {
  margin: 0;
  color: rgba(255, 255, 255, 0.92);
  font-size: 15px;
  line-height: 1.8;
}

.login-feature-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.login-feature-grid div {
  padding: 12px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.12);
}

.login-feature-grid strong,
.login-feature-grid span {
  display: block;
}

.login-feature-grid strong {
  margin-bottom: 4px;
  font-size: 17px;
}

.login-feature-grid span {
  color: rgba(255, 255, 255, 0.78);
  font-size: 12px;
}

.login-auth-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 34px 36px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.auth-heading {
  margin-bottom: 18px;
}

.auth-heading h2 {
  margin: 8px 0 4px;
  color: #0f172a;
  font-size: 26px;
  line-height: 1.22;
  letter-spacing: 0;
}

.auth-heading p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}

.tenant-chip-row {
  display: flex;
  justify-content: flex-start;
  margin-bottom: 14px;
}

.tenant-inline-form,
.tenant-form-item {
  margin: 0;
}

.tenant-form-item :deep(.el-form-item__content) {
  line-height: 1;
}

.tenant-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: 100%;
  height: 34px;
  padding: 0 12px;
  border: 1px solid #d8e3df;
  border-radius: 999px;
  color: #125e52;
  background: #f1f8f6;
  cursor: pointer;
}

.tenant-chip span {
  color: #64748b;
  font-size: 12px;
}

.tenant-chip strong {
  overflow: hidden;
  max-width: 170px;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tenant-chip.loading {
  opacity: 0.72;
}

.tenant-menu {
  display: grid;
  gap: 8px;
  max-height: 260px;
  overflow: auto;
}

.tenant-menu-item {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  color: #0f172a;
  background: #fff;
  text-align: left;
  cursor: pointer;
}

.tenant-menu-item.active {
  border-color: #125e52;
  color: #125e52;
  background: #edf7f4;
  font-weight: 700;
}

.tenant-empty {
  color: #64748b;
  font-size: 13px;
  text-align: center;
}

.login-tabs {
  margin-bottom: 16px;
}

.auth-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.auth-form :deep(.el-form-item__label) {
  height: auto;
  margin-bottom: 6px;
  padding: 0;
  color: #334155;
  font-weight: 600;
  line-height: 1.25;
}

.auth-form :deep(.el-form-item__content) {
  line-height: 1.2;
}

.auth-form :deep(.el-input__wrapper),
.auth-form :deep(.el-select__wrapper) {
  min-height: 42px;
  border-radius: 8px;
}

.login-auth-panel :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.login-auth-panel :deep(.el-tabs__item) {
  font-weight: 600;
}

.verification-alert {
  margin-bottom: 18px;
}

.email-code-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
  width: 100%;
}

.login-submit {
  width: 100%;
  height: 44px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #125e52, #1f8a70);
  font-size: 16px;
  font-weight: 600;
}

.login-footer {
  display: flex;
  justify-content: center;
  width: 100%;
}

.candidate-list {
  display: grid;
  gap: 12px;
  width: 100%;
}

.candidate-item {
  margin-right: 0;
  padding: 12px 14px;
  border: 1px solid var(--el-border-color);
  border-radius: 12px;
}

.candidate-item :deep(.el-radio__label) {
  width: 100%;
}

.candidate-title {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.candidate-meta {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

@media (max-width: 640px) {
  .login-container {
    padding: 14px;
    align-items: start;
  }

  .login-shell {
    grid-template-columns: 1fr;
    min-height: auto;
    border-radius: 14px;
  }

  .login-visual-panel {
    min-height: 180px;
    padding: 22px;
  }

  .visual-copy h1 {
    font-size: 25px;
  }

  .visual-copy p,
  .login-feature-grid {
    display: none;
  }

  .login-auth-panel {
    padding: 22px;
  }

  .email-code-row {
    grid-template-columns: 1fr;
  }

  .tenant-chip strong {
    max-width: 190px;
  }

  .candidate-item {
    padding: 10px 12px;
  }
}
</style>
