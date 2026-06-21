<template>
  <div class="contact-page design-contact-reference-page" data-testid="portal-contact-page">
    <section class="contact-hero design-contact-hero">
      <div class="container contact-hero__inner">
        <div class="hero-copy">
          <p class="hero-eyebrow">协作与反馈</p>
          <h1>联系我们</h1>
          <p>
            入社咨询、活动合作、网站问题和内容反馈都可以在这里提交给当前社团管理员。
          </p>
        </div>

        <aside class="hero-panel">
          <p class="panel-label">当前状态</p>
          <strong>{{ userStore.isLoggedIn ? '已登录提交' : '匿名提交' }}</strong>
          <p>{{ submitHint }}</p>
        </aside>
      </div>
    </section>

    <main class="container contact-shell">
      <aside class="contact-info">
        <section class="info-card">
          <p class="info-eyebrow">提交说明</p>
          <h2>留言会投递给当前社团</h2>
          <div class="info-list">
            <article class="info-item">
              <span>路由</span>
              <div>
                <h3>按当前租户投递</h3>
                <p>系统会按当前租户解析管理员收件范围，不会误投到其他社团。</p>
              </div>
            </article>
            <article class="info-item">
              <span>响应</span>
              <div>
                <h3>建议留下联系方式</h3>
                <p>已登录用户会自动带出资料，未登录时建议填写邮箱或手机号。</p>
              </div>
            </article>
            <article class="info-item">
              <span>防刷</span>
              <div>
                <h3>重复内容会被拦截</h3>
                <p>请一次性描述完整问题，避免短时间连续刷新提交。</p>
              </div>
            </article>
          </div>
        </section>
      </aside>

      <section class="contact-form-panel design-contact-form-panel" data-testid="portal-contact-form">
        <div class="panel-header">
          <div>
            <p>留言表单</p>
            <h2>发送留言</h2>
          </div>
          <el-tag :type="shield.token ? 'success' : 'info'">
            {{ shield.token ? '校验已初始化' : '校验初始化中' }}
          </el-tag>
        </div>
        <p class="panel-intro">{{ submitHint }}</p>

        <el-form ref="formRef" :model="form" :rules="formRules" label-position="top" class="contact-form">
          <div class="form-grid">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="请输入你的姓名" />
            </el-form-item>
            <el-form-item label="主题" prop="subject">
              <el-select v-model="form.subject" placeholder="请选择留言主题" style="width: 100%">
                <el-option
                  v-for="option in subjectOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
          </div>

          <div class="form-grid">
            <el-form-item label="希望联系的邮箱" prop="targetEmail">
              <el-input v-model="form.targetEmail" placeholder="可选，指定希望联系的管理员邮箱" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="选填，用于邮件回复" />
            </el-form-item>
          </div>

          <div class="form-grid">
            <el-form-item label="手机">
              <el-input v-model="form.phone" placeholder="选填，建议填写可联系手机号" />
            </el-form-item>
            <el-form-item label="验证码">
              <div class="verify-row">
                <el-input v-model="form.verificationCode" placeholder="请输入邮箱验证码" />
                <el-button
                  :loading="sendingCode"
                  :disabled="!userStore.isLoggedIn && !sendCodeReady"
                  @click="handleSendCode"
                >
                  {{ userStore.isLoggedIn ? '已登录' : sendCodeText }}
                </el-button>
              </div>
            </el-form-item>
          </div>

          <el-form-item label="留言内容" prop="message">
            <el-input
              v-model="form.message"
              type="textarea"
              :rows="7"
              maxlength="1000"
              show-word-limit
              placeholder="请描述你遇到的问题、想咨询的事项或合作诉求"
            />
          </el-form-item>

          <div class="contact-honeypot" aria-hidden="true">
            <label for="contact-honeypot">请勿填写</label>
            <input
              id="contact-honeypot"
              v-model="form.honeypot"
              type="text"
              tabindex="-1"
              autocomplete="off"
            />
          </div>

          <div class="form-note">
            <span>内容长度：{{ messageLength }} / 1000</span>
            <span>{{ shieldHint }}</span>
          </div>

          <el-form-item class="form-actions">
            <el-button type="primary" :loading="loading" @click="handleSubmit">提交留言</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchContactPublicShield, submitPublicContactMessage } from '@/api/contact'
import { sendEmailVerificationCode } from '@/api/user'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const sendingCode = ref(false)
const sendCodeReady = ref(true)
const sendCodeText = ref('发送验证码')
const shield = ref({
  token: '',
  issuedAt: 0,
  minSubmitDelayMs: 1200,
  expiresInSeconds: 900
})

const subjectOptions = [
  { label: '入社咨询', value: '入社咨询' },
  { label: '活动报名', value: '活动报名' },
  { label: '功能反馈', value: '功能反馈' },
  { label: '合作洽谈', value: '合作洽谈' },
  { label: '其他问题', value: '其他问题' }
]

const createEmptyForm = () => ({
  name: '',
  email: '',
  targetEmail: '',
  verificationCode: '',
  phone: '',
  subject: '',
  message: '',
  honeypot: ''
})

const form = ref(createEmptyForm())

const formRules = reactive({
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }],
  targetEmail: [{ type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }],
  subject: [{ required: true, message: '请选择留言主题', trigger: 'change' }],
  message: [
    { required: true, message: '请输入留言内容', trigger: 'blur' },
    { min: 10, message: '留言内容至少需要 10 个字符', trigger: 'blur' }
  ]
})

const submitHint = computed(() => (
  userStore.isLoggedIn
    ? '当前为已登录提交，系统会优先使用你的账号资料进行回访。'
    : '当前为匿名提交，建议填写邮箱或手机号，方便管理员回复。'
))

const messageLength = computed(() => form.value.message.trim().length)
const shieldHint = computed(() => (
  shield.value.token
    ? `请完成填写后再提交，系统会拦截 ${Math.ceil(shield.value.minSubmitDelayMs / 1000)} 秒内的机器式快速提交。`
    : '正在初始化提交校验，请稍候再提交。'
))

function startCodeCountdown() {
  let seconds = 60
  sendCodeReady.value = false
  sendCodeText.value = `${seconds}s`
  const timer = window.setInterval(() => {
    seconds -= 1
    if (seconds <= 0) {
      window.clearInterval(timer)
      sendCodeReady.value = true
      sendCodeText.value = '发送验证码'
      return
    }
    sendCodeText.value = `${seconds}s`
  }, 1000)
}

async function handleSendCode() {
  if (userStore.isLoggedIn) {
    ElMessage.info('已登录用户可直接提交')
    return
  }
  const email = form.value.email?.trim()
  if (!email) {
    ElMessage.warning('请先填写邮箱')
    return
  }
  if (!sendCodeReady.value) {
    return
  }
  sendingCode.value = true
  try {
    const res = await sendEmailVerificationCode({
      email,
      tenantId: Number(userStore.tenantId || localStorage.getItem('tenantId') || 1),
      scene: 'CONTACT'
    })
    if (res.code !== 200) {
      throw new Error(res.msg || '验证码发送失败')
    }
    ElMessage.success('验证码已发送，请查收邮箱')
    startCodeCountdown()
  } catch (error) {
    sendCodeReady.value = true
    sendCodeText.value = '发送验证码'
    ElMessage.error(error.message || '验证码发送失败')
  } finally {
    sendingCode.value = false
  }
}

async function refreshShield() {
  const response = await fetchContactPublicShield()
  const data = response?.data || response || {}
  shield.value = {
    token: data.token || '',
    issuedAt: Number(data.issuedAt || 0),
    minSubmitDelayMs: Number(data.minSubmitDelayMs || 1200),
    expiresInSeconds: Number(data.expiresInSeconds || 900)
  }
}

function hydrateFromUser() {
  const info = userStore.userInfo || {}
  form.value = {
    ...form.value,
    name: form.value.name || info.name || info.realName || info.username || '',
    email: form.value.email || info.email || '',
    phone: form.value.phone || info.cellPhone || ''
  }
}

async function handleSubmit() {
  if (!formRef.value) {
    return
  }

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  if (!userStore.isLoggedIn && !form.value.verificationCode?.trim()) {
    ElMessage.warning('请先填写邮箱验证码')
    return
  }

  loading.value = true
  try {
    if (!shield.value.token) {
      await refreshShield()
    }

    await submitPublicContactMessage({
      name: form.value.name,
      email: form.value.email || '',
      targetEmail: form.value.targetEmail || undefined,
      verificationCode: form.value.verificationCode?.trim() || undefined,
      phone: form.value.phone || undefined,
      subject: form.value.subject,
      message: form.value.message,
      shieldToken: shield.value.token,
      issuedAt: shield.value.issuedAt,
      honeypot: form.value.honeypot
    })
    ElMessage.success('留言已提交，管理员会尽快查看。')
    await handleReset()
  } catch (error) {
    console.error('submit contact message failed', error)
    await refreshShield().catch(() => {})
    ElMessage.error('留言提交失败，请刷新校验后重试。')
  } finally {
    loading.value = false
  }
}

async function handleReset() {
  form.value = createEmptyForm()
  hydrateFromUser()
  formRef.value?.clearValidate()
  await refreshShield().catch(() => {})
}

onMounted(() => {
  hydrateFromUser()
  refreshShield().catch((error) => {
    console.error('init public contact shield failed', error)
  })
})
</script>

<style lang="scss" scoped>
@use '../styles/variables.scss' as *;

.contact-page {
  min-height: 100vh;
  padding-bottom: 64px;
  background:
    linear-gradient(180deg, rgba(47, 111, 237, 0.06) 0%, rgba(247, 250, 255, 0.92) 28%, #fff 100%);
}

.contact-hero {
  padding: 28px 0 0;
}

.contact-hero__inner {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(300px, 0.45fr);
  gap: 32px;
  align-items: stretch;
  padding: 52px 64px;
  border-radius: 28px;
  color: #fff;
  background:
    radial-gradient(circle at 76% 26%, rgba(255, 255, 255, 0.28), transparent 22%),
    linear-gradient(135deg, #3567ff 0%, #1d86ff 100%);
  box-shadow: 0 24px 60px rgba(39, 105, 255, 0.22);
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
  opacity: 0.86;
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
  display: block;
  font-size: 26px;
  line-height: 1.25;
}

.hero-panel p:last-child {
  margin: 16px 0 0;
  line-height: 1.7;
  opacity: 0.82;
}

.contact-shell {
  display: grid;
  grid-template-columns: minmax(280px, 0.46fr) minmax(0, 1fr);
  gap: 24px;
  padding-top: 26px;
}

.contact-info,
.contact-form-panel {
  min-width: 0;
}

.info-card,
.contact-form-panel {
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 18px 44px rgba(31, 58, 120, 0.08);
}

.info-card {
  padding: 24px;
}

.info-eyebrow,
.panel-header p {
  margin: 0 0 10px;
  color: #2f6fed;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.14em;
}

.info-card h2,
.panel-header h2 {
  margin: 0;
  color: $text-primary;
  font-size: 26px;
  line-height: 1.25;
}

.info-list {
  display: grid;
  gap: 14px;
  margin-top: 22px;
}

.info-item {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  padding: 16px;
  border-radius: 18px;
  background: #f8fbff;
  border: 1px solid rgba(226, 232, 240, 0.82);
}

.info-item > span {
  flex-shrink: 0;
  padding: 7px 11px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2f6fed;
  font-size: 13px;
  font-weight: 800;
}

.info-item h3 {
  margin: 0 0 8px;
  color: $text-primary;
  font-size: 16px;
}

.info-item p,
.panel-intro {
  margin: 0;
  color: $text-secondary;
  line-height: 1.75;
}

.contact-form-panel {
  padding: 26px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.panel-intro {
  margin: 8px 0 22px;
}

.contact-form {
  :deep(.el-form-item__label) {
    font-weight: 700;
    color: $text-primary;
  }
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.verify-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  width: 100%;
}

.form-note {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin: 4px 0 18px;
  color: $text-secondary;
  font-size: 13px;
}

.contact-honeypot {
  position: absolute;
  left: -9999px;
  width: 1px;
  height: 1px;
  overflow: hidden;
}

.form-actions {
  margin-bottom: 0;
}

@media (max-width: $breakpoint-lg) {
  .contact-hero__inner,
  .contact-shell,
  .form-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: $breakpoint-md) {
  .contact-hero__inner {
    margin: 0 16px;
    padding: 32px 24px;
  }

  .info-card,
  .contact-form-panel {
    padding: 20px;
  }

  .form-note {
    flex-direction: column;
  }
}

@media (max-width: 640px) {
  .contact-page {
    padding-bottom: 36px;
  }

  .contact-hero {
    padding-top: 18px;
  }

  .contact-hero__inner {
    margin: 0 12px;
    padding: 24px 18px;
    border-radius: 20px;
  }

  .hero-copy h1 {
    font-size: 24px;
  }

  .hero-copy p:not(.hero-eyebrow) {
    font-size: 14px;
    line-height: 1.68;
  }

  .hero-panel {
    min-height: 0;
    padding: 18px;
  }

  .contact-shell {
    gap: 16px;
    padding-top: 18px;
  }

  .info-card,
  .contact-form-panel {
    padding: 18px;
    border-radius: 18px;
  }

  .panel-header {
    flex-direction: column;
  }

  .info-card h2,
  .panel-header h2 {
    font-size: 21px;
  }

  .info-item {
    flex-direction: column;
    padding: 14px;
  }

  .verify-row {
    grid-template-columns: 1fr;
  }
}
</style>
