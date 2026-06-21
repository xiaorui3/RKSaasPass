<template>
  <div class="join-page design-join-reference-page" data-testid="portal-join-page">
    <section class="join-hero design-join-hero">
      <div class="container join-hero__inner">
        <div class="hero-copy">
          <p class="hero-eyebrow">申请入口</p>
          <h1>申请其他社团</h1>
          <p>
            面向已登录成员和访客开放跨社团申请，租户、邀请码、动态字段和邮箱验证码均继续读取现有配置。
          </p>
          <div class="hero-actions">
            <a class="hero-link primary" href="#join-form">立即申请</a>
            <span class="hero-link secondary">{{ isLoggedInJoinMode ? '已登录申请' : '访客申请' }}</span>
          </div>
        </div>

        <aside class="hero-panel">
          <p class="panel-label">申请状态</p>
          <strong>{{ selectableTenantList.length }}</strong>
          <span>个可选择社团</span>
          <p>{{ inviteToken ? '当前为邀请入社链接，已为你预填邮箱和内推码。' : '请选择目标社团后填写申请资料。' }}</p>
        </aside>
      </div>
    </section>

    <main class="container join-shell">
      <section class="surface-panel intro-panel">
        <div class="section-heading">
          <p>申请说明</p>
          <h2>{{ admissionConfig.pageTitle || '申请其他社团' }}</h2>
          <span>{{ admissionConfig.pageDescription || '请按当前社团的申请表单配置填写信息，提交后等待负责人审核。' }}</span>
        </div>

        <div class="benefit-grid design-tenant-card-grid">
          <article v-for="benefit in benefits" :key="benefit.title" class="benefit-card">
            <div class="benefit-icon">{{ benefit.icon }}</div>
            <h3>{{ benefit.title }}</h3>
            <p>{{ benefit.description }}</p>
          </article>
        </div>

        <div class="steps-panel">
          <div class="section-heading compact">
            <p>流程</p>
            <h2>申请流程</h2>
            <span>保留原有提交接口和动态字段规则，审核通过后按社团流程完成加入。</span>
          </div>
          <el-steps :active="100" finish-status="success">
            <el-step title="提交申请" description="选择目标社团并填写申请资料" />
            <el-step title="负责人审核" description="社团负责人查看申请内容" />
            <el-step title="成为成员" description="审核通过后进入社团流程" />
          </el-steps>
        </div>
      </section>

      <aside class="form-column">
        <section id="join-form" class="surface-panel form-panel">
          <div class="form-panel__header">
            <div>
              <p>申请表单</p>
              <h2>立即申请</h2>
            </div>
            <el-tag v-if="configLoading" type="info">配置加载中</el-tag>
            <el-tag v-else-if="joinApplicationClosed" type="warning">申请已关闭</el-tag>
            <el-tag v-else type="success">表单已就绪</el-tag>
          </div>

          <el-alert
            v-if="joinApplicationClosed"
            title="当前租户已关闭入社申请"
            description="请联系社团负责人或租户管理员开通申请入口。"
            type="warning"
            show-icon
            :closable="false"
            class="join-closed-alert"
          />

          <p v-if="inviteToken" class="invite-tip">
            当前为邀请入社链接，系统已为你绑定邮箱和内推码。
          </p>

          <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="join-form">
            <el-form-item label="目标社团" prop="tenantId">
              <el-select
                v-model="form.tenantId"
                placeholder="请选择社团或租户"
                style="width: 100%"
                :loading="tenantLoading"
                @change="handleTenantChange"
              >
                <el-option
                  v-for="tenant in selectableTenantList"
                  :key="tenant.id"
                  :label="tenant.tenantName"
                  :value="tenant.id.toString()"
                />
              </el-select>
            </el-form-item>

            <div v-if="!isLoggedInJoinMode" class="form-grid">
              <el-form-item label="申请账号" prop="username">
                <el-input v-model="form.username" placeholder="审核通过后用于登录的账号" />
              </el-form-item>

              <el-form-item label="登录密码" prop="password">
                <el-input v-model="form.password" type="password" show-password placeholder="请输入登录密码" />
              </el-form-item>
            </div>

            <el-form-item v-if="!isLoggedInJoinMode" label="确认密码" prop="confirmPassword">
              <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次输入登录密码" />
            </el-form-item>

            <DynamicAdmissionFields :fields="admissionConfig.fields" :model="form" />

            <el-form-item label="内推码">
              <el-input
                v-model="form.referralCode"
                :readonly="!!inviteToken"
                :placeholder="inviteToken ? '邀请链接已绑定内推码' : '可选，可手动输入内推码'"
              />
            </el-form-item>

            <el-form-item v-if="emailFieldEnabled" label="邮箱验证码" prop="emailCode">
              <div class="email-code-row">
                <el-input v-model="form.emailCode" placeholder="请输入邮箱验证码" />
                <el-button :disabled="!sendCodeReady || joinApplicationClosed" @click="handleSendCode">
                  {{ sendCodeText }}
                </el-button>
              </div>
            </el-form-item>

            <el-form-item class="form-actions">
              <el-button type="primary" :loading="loading" :disabled="joinApplicationClosed" @click="handleSubmit">提交申请</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </section>

        <section class="surface-panel side-note">
          <p class="side-note__eyebrow">提示</p>
          <h3>资料会提交到所选社团</h3>
          <p>
            当前页面只负责收集申请资料，审核、成员同步和邀请码确认继续由原有接口处理。
          </p>
        </section>
      </aside>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import DynamicAdmissionFields from '@/components/admission/DynamicAdmissionFields.vue'
import { useUserStore } from '@/stores/user'
import { getPublicAdmissionFormConfig, submitApplication } from '@/api/admission'
import { getPublicTenantList } from '@/api/tenant'
import { sendEmailVerificationCode } from '@/api/user'
import { acceptEmailInvitation, getEmailInvitation } from '@/api/email'
import { useThemeStore } from '@/stores/theme'
import { useTenantSelfServiceStore } from '@/stores/tenantSelfService'
import {
  buildAdmissionFieldRules,
  collectAdmissionFormPayload,
  createDefaultAdmissionConfig,
  isAdmissionFieldEnabled,
  normalizeAdmissionFormConfig,
  resolveAdmissionSuccessMessage,
  syncAdmissionFormModel
} from '@/composables/useAdmissionFormRuntime'
import { normalizeTenantDirectory } from '@/utils/tenantBranding'

const route = useRoute()
const userStore = useUserStore()
const themeStore = useThemeStore()
const tenantSelfServiceStore = useTenantSelfServiceStore()
const inviteToken = ref('')
const getStoredUserInfo = () => {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || 'null')
  } catch (error) {
    return null
  }
}
const currentAuthUserId = computed(() => userStore.userInfo?.userId || getStoredUserInfo()?.userId || null)
const currentTenantId = computed(() => String(userStore.tenantId || getStoredUserInfo()?.organizationId || localStorage.getItem('tenantId') || ''))
const isLoggedInJoinMode = computed(() => !!currentAuthUserId.value)

const formRef = ref(null)
const loading = ref(false)
const tenantLoading = ref(false)
const configLoading = ref(false)
const tenantList = ref([])
const admissionConfig = ref(createDefaultAdmissionConfig('join'))
const admissionConfigRequestId = ref(0)

const createInitialForm = () => ({
  tenantId: '',
  name: '',
  studentId: '',
  username: '',
  password: '',
  confirmPassword: '',
  major: '',
  grade: '',
  phone: '',
  email: '',
  emailCode: '',
  inviteToken: '',
  referralCode: '',
  interests: [],
  intro: ''
})

const form = ref(createInitialForm())
syncAdmissionFormModel(form.value, admissionConfig.value)
const sendCodeReady = ref(true)
const sendCodeText = ref('发送验证码')
let countdownTimer = null

const selectableTenantList = computed(() => {
  const list = tenantList.value || []
  if (!isLoggedInJoinMode.value || !currentTenantId.value) {
    return list
  }
  return list.filter((tenant) => String(tenant.id) !== currentTenantId.value)
})

const emailFieldEnabled = computed(() => isAdmissionFieldEnabled(admissionConfig.value, 'email'))
const admissionSettings = computed(() => tenantSelfServiceStore.admissionSettings)
const joinApplicationClosed = computed(() => admissionSettings.value.allowJoinApplication === false)

const rules = computed(() => ({
  tenantId: [{ required: true, message: '请选择社团或租户', trigger: 'change' }],
  ...(!isLoggedInJoinMode.value
    ? {
        username: [{ required: true, message: '请输入申请账号', trigger: 'blur' }],
        password: [
          { required: true, message: '请输入登录密码', trigger: 'blur' },
          { min: 6, message: '密码至少 6 位', trigger: 'blur' }
        ],
        confirmPassword: [
          {
            validator: (_rule, value, callback) => {
              if (!value) {
                callback(new Error('请再次输入登录密码'))
                return
              }
              if (value !== form.value.password) {
                callback(new Error('两次输入的密码不一致'))
                return
              }
              callback()
            },
            trigger: 'blur'
          }
        ]
      }
    : {}),
  ...buildAdmissionFieldRules(admissionConfig.value),
  ...(emailFieldEnabled.value
    ? {
        emailCode: [{ required: true, message: '请输入邮箱验证码', trigger: 'blur' }]
      }
    : {})
}))

const benefits = [
  { icon: '01', title: '按配置填写', description: '申请字段由目标社团的入社表单配置生成，页面不写死业务资料。' },
  { icon: '02', title: '支持邀请', description: '邀请链接会自动带入邮箱、内推码和目标社团上下文。' },
  { icon: '03', title: '邮箱校验', description: '启用邮箱字段时，继续使用现有验证码发送接口完成校验。' },
  { icon: '04', title: '统一提交', description: '资料仍提交到原有申请接口，审核流由后台负责人处理。' }
]

const fetchTenantList = async () => {
  tenantLoading.value = true
  try {
    const res = await getPublicTenantList()
    if (res.code === 200 && Array.isArray(res.data)) {
      tenantList.value = normalizeTenantDirectory(res.data)
      const candidates = selectableTenantList.value
      if (candidates.length > 0) {
        const nextTenantId = !form.value.tenantId || !candidates.some(item => String(item.id) === String(form.value.tenantId))
          ? candidates[0].id.toString()
          : form.value.tenantId
        form.value.tenantId = nextTenantId
        await fetchAdmissionConfig(nextTenantId)
      }
    }
  } catch (error) {
    console.error('获取租户列表失败:', error)
    ElMessage.error('获取社团列表失败，请稍后重试')
  } finally {
    tenantLoading.value = false
  }
}

const fetchAdmissionConfig = async (tenantId) => {
  const requestId = ++admissionConfigRequestId.value
  if (!tenantId) {
    if (requestId !== admissionConfigRequestId.value) {
      return
    }
    admissionConfig.value = createDefaultAdmissionConfig('join')
    syncAdmissionFormModel(form.value, admissionConfig.value)
    return
  }
  configLoading.value = true
  try {
    const [res] = await Promise.all([
      getPublicAdmissionFormConfig(tenantId),
      tenantSelfServiceStore.loadPublicConfig(tenantId, true)
    ])
    if (requestId !== admissionConfigRequestId.value) {
      return
    }
    admissionConfig.value = res.code === 200 && res.data
      ? normalizeAdmissionFormConfig(res.data, 'join')
      : createDefaultAdmissionConfig('join')
    syncAdmissionFormModel(form.value, admissionConfig.value)
  } catch (error) {
    console.error('获取入社表单配置失败:', error)
    if (requestId !== admissionConfigRequestId.value) {
      return
    }
    admissionConfig.value = createDefaultAdmissionConfig('join')
    syncAdmissionFormModel(form.value, admissionConfig.value)
  } finally {
    if (requestId === admissionConfigRequestId.value) {
      configLoading.value = false
    }
  }
}

const applyInvitation = async () => {
  inviteToken.value = route.query.inviteToken ? String(route.query.inviteToken) : ''
  if (!inviteToken.value) {
    return
  }
  try {
    const res = await getEmailInvitation(inviteToken.value)
    if (res.code === 200 && res.data) {
      form.value.tenantId = String(res.data.tenantId)
      form.value.email = res.data.targetEmail || ''
      form.value.inviteToken = inviteToken.value
      form.value.referralCode = res.data.referralCode || ''
      ElMessage.success('已加载邀请信息')
    }
  } catch (error) {
    ElMessage.error(error.message || '邀请信息加载失败')
  }
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
      sendCodeText.value = '发送验证码'
      return
    }
    sendCodeText.value = `${seconds}s`
  }, 1000)
}

const handleSendCode = async () => {
  if (joinApplicationClosed.value) {
    ElMessage.warning('当前租户已关闭入社申请')
    return
  }
  if (!form.value.tenantId) {
    ElMessage.warning('请先选择社团或租户')
    return
  }
  if (!form.value.email) {
    ElMessage.warning('请先填写常用邮箱')
    return
  }
  if (!sendCodeReady.value) return

  try {
    sendCodeReady.value = false
    sendCodeText.value = '发送中...'
    const res = await sendEmailVerificationCode({
      email: form.value.email,
      tenantId: Number(form.value.tenantId),
      scene: 'JOIN'
    })
    if (res.code === 200) {
      ElMessage.success('验证码已发送，请注意查收')
      startCountdown()
      return
    }
    throw new Error(res.msg || '验证码发送失败')
  } catch (error) {
    sendCodeReady.value = true
    sendCodeText.value = '发送验证码'
    ElMessage.error(error.message || '验证码发送失败')
  }
}

const handleTenantChange = async (tenantId) => {
  await Promise.all([
    themeStore.applyTenantTheme(tenantId, 'frontend'),
    tenantSelfServiceStore.loadPublicConfig(tenantId, true),
    fetchAdmissionConfig(tenantId)
  ])
}

const handleReset = () => {
  const candidates = selectableTenantList.value
  const nextForm = {
    ...createInitialForm(),
    tenantId: candidates.length > 0 ? candidates[0].id.toString() : '',
    username: isLoggedInJoinMode.value ? (userStore.userInfo?.username || '') : '',
    email: inviteToken.value ? form.value.email : '',
    inviteToken: inviteToken.value || '',
    referralCode: inviteToken.value ? form.value.referralCode : ''
  }
  syncAdmissionFormModel(nextForm, admissionConfig.value)
  form.value = nextForm
  formRef.value?.clearValidate()
}

const handleSubmit = async () => {
  if (joinApplicationClosed.value) {
    ElMessage.warning('当前租户已关闭入社申请')
    return
  }
  if (!formRef.value || configLoading.value) return

  try {
    await formRef.value.validate()
    loading.value = true

    const formPayload = collectAdmissionFormPayload(form.value, admissionConfig.value)
    const payload = {
      tenantId: Number(form.value.tenantId),
      name: form.value.name,
      studentId: form.value.studentId,
      username: isLoggedInJoinMode.value ? (userStore.userInfo?.username || form.value.username) : form.value.username,
      sourceAuthUserId: currentAuthUserId.value,
      sourceTenantId: isLoggedInJoinMode.value && currentTenantId.value ? Number(currentTenantId.value) : null,
      sourceRoleName: isLoggedInJoinMode.value ? userStore.getRoleName() : '',
      password: isLoggedInJoinMode.value ? '' : form.value.password,
      major: form.value.major,
      grade: form.value.grade,
      phone: form.value.phone,
      email: form.value.email,
      emailCode: form.value.emailCode,
      inviteToken: form.value.inviteToken,
      referralCode: form.value.referralCode,
      interest: Array.isArray(form.value.interests) ? form.value.interests.join(',') : '',
      experience: form.value.intro || '',
      formPayload
    }

    const res = await submitApplication(payload, form.value.tenantId)
    if (res.code === 200) {
      if (inviteToken.value) {
        await acceptEmailInvitation(inviteToken.value)
      }
      ElMessage.success(resolveAdmissionSuccessMessage(admissionConfig.value, 'join'))
      handleReset()
      return
    }
    ElMessage.error(res.msg || '申请提交失败')
  } catch (error) {
    if (error?.message) {
      console.error('提交入社申请失败:', error)
    }
  } finally {
    loading.value = false
  }
}

watch(
  () => form.value.tenantId,
  (tenantId) => {
    if (!tenantId) return
    themeStore.applyTenantTheme(tenantId, 'frontend')
    tenantSelfServiceStore.loadPublicConfig(tenantId, true)
  }
)

watch(
  selectableTenantList,
  (list) => {
    if (!list.length) {
      form.value.tenantId = ''
      return
    }
    if (!form.value.tenantId || !list.some(item => String(item.id) === String(form.value.tenantId))) {
      form.value.tenantId = String(list[0].id)
    }
  },
  { immediate: true }
)

onMounted(async () => {
  await fetchTenantList()
  await applyInvitation()
})

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
})
</script>

<style lang="scss" scoped>
@use '../styles/variables.scss' as *;

.join-page {
  min-height: 100vh;
  padding-bottom: 64px;
  background:
    linear-gradient(180deg, rgba(47, 111, 237, 0.06) 0%, rgba(247, 250, 255, 0.92) 30%, #fff 100%);
}

.join-hero {
  padding: 28px 0 0;
}

.join-hero__inner {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 0.36fr);
  gap: 28px;
  align-items: stretch;
  padding: 52px 64px;
  border-radius: 28px;
  color: #fff;
  background:
    radial-gradient(circle at 78% 22%, rgba(255, 255, 255, 0.26), transparent 24%),
    linear-gradient(135deg, #3567ff 0%, #1d86ff 100%);
  box-shadow: 0 24px 60px rgba(39, 105, 255, 0.22);
}

.hero-copy h1 {
  margin: 0;
  font-size: clamp(34px, 4vw, 56px);
  line-height: 1.08;
}

.hero-copy p:not(.hero-eyebrow) {
  max-width: 680px;
  margin: 18px 0 0;
  font-size: 17px;
  line-height: 1.8;
  opacity: 0.88;
}

.hero-eyebrow,
.panel-label {
  margin: 0 0 12px;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  opacity: 0.82;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 26px;
}

.hero-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  padding: 0 18px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 700;
  text-decoration: none;
}

.hero-link.primary {
  background: #fff;
  color: #2368f5;
}

.hero-link.secondary {
  border: 1px solid rgba(255, 255, 255, 0.34);
  color: #fff;
}

.hero-panel {
  display: grid;
  align-content: center;
  min-height: 220px;
  padding: 24px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.22);
}

.hero-panel strong {
  font-size: 48px;
  line-height: 1;
}

.hero-panel span {
  margin-top: 8px;
  font-size: 14px;
  opacity: 0.78;
}

.hero-panel p:last-child {
  margin: 18px 0 0;
  line-height: 1.7;
  opacity: 0.82;
}

.join-shell {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(360px, 0.75fr);
  gap: 24px;
  padding-top: 26px;
}

.surface-panel {
  min-width: 0;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 18px 44px rgba(31, 58, 120, 0.08);
}

.intro-panel,
.form-panel,
.side-note {
  padding: 26px;
}

.section-heading {
  margin-bottom: 22px;
}

.section-heading p,
.form-panel__header p,
.side-note__eyebrow {
  margin: 0 0 10px;
  color: #2f6fed;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.14em;
}

.section-heading h2,
.form-panel__header h2,
.side-note h3 {
  margin: 0;
  color: $text-primary;
  font-size: 26px;
  line-height: 1.25;
}

.section-heading span {
  display: block;
  margin-top: 10px;
  color: $text-secondary;
  line-height: 1.75;
}

.section-heading.compact {
  margin-bottom: 18px;
}

.benefit-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.benefit-card {
  padding: 20px;
  border-radius: 18px;
  background: #f8fbff;
  border: 1px solid rgba(226, 232, 240, 0.88);
}

.benefit-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  margin-bottom: 14px;
  border-radius: 50%;
  background: #eff6ff;
  color: #2f6fed;
  font-weight: 800;
}

.benefit-card h3 {
  margin: 0 0 8px;
  font-size: 17px;
  color: $text-primary;
}

.benefit-card p,
.side-note p {
  margin: 0;
  color: $text-secondary;
  line-height: 1.75;
}

.steps-panel {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid rgba(226, 232, 240, 0.9);
}

.form-column {
  display: grid;
  gap: 18px;
  align-content: start;
}

.form-panel__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 18px;
}

.invite-tip {
  margin: 0 0 18px;
  padding: 12px 14px;
  border-radius: 14px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 14px;
  line-height: 1.6;
}

.join-closed-alert {
  margin-bottom: 16px;
}

.join-form {
  :deep(.el-form-item__label) {
    font-weight: 700;
    color: $text-primary;
  }
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.email-code-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  width: 100%;
}

.form-actions {
  margin-bottom: 0;
}

@media (max-width: $breakpoint-lg) {
  .join-hero__inner,
  .join-shell {
    grid-template-columns: 1fr;
  }
}

@media (max-width: $breakpoint-md) {
  .join-hero__inner {
    margin: 0 16px;
    padding: 32px 24px;
  }

  .benefit-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .join-page {
    padding-bottom: 36px;
  }

  .join-hero {
    padding-top: 18px;
  }

  .join-hero__inner {
    margin: 0 12px;
    padding: 24px 18px;
    border-radius: 20px;
  }

  .hero-copy h1 {
    font-size: 28px;
  }

  .hero-copy p:not(.hero-eyebrow) {
    font-size: 14px;
    line-height: 1.68;
  }

  .hero-actions {
    margin-top: 20px;
  }

  .hero-link {
    min-height: 40px;
    padding: 0 14px;
    font-size: 13px;
  }

  .hero-panel {
    min-height: 0;
    padding: 18px;
  }

  .join-shell {
    gap: 16px;
    padding-top: 18px;
  }

  .intro-panel,
  .form-panel,
  .side-note {
    padding: 18px;
    border-radius: 18px;
  }

  .section-heading h2,
  .form-panel__header h2,
  .side-note h3 {
    font-size: 21px;
  }

  .form-panel__header {
    flex-direction: column;
  }

  .email-code-row {
    grid-template-columns: 1fr;
  }
}
</style>
