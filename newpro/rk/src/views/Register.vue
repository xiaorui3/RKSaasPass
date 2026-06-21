<template>
  <div class="register-container">
    <video class="video-background" autoplay loop muted playsinline>
      <source src="@/static/logo.mp4" type="video/mp4">
      当前浏览器不支持视频播放。
    </video>

    <div class="video-overlay"></div>

    <el-card class="register-card">
      <template #header>
        <div class="register-header">
          <h2 class="register-title">{{ registerHeaderTitle }}</h2>
          <p class="register-subtitle">{{ registerHeaderDescription }}</p>
        </div>
      </template>

      <el-form ref="registerFormRef" :model="registerForm" :rules="rules" label-width="80px">
        <el-alert
          v-if="registrationClosed"
          title="当前租户已关闭公开注册"
          description="请联系社团负责人或租户管理员开通注册入口。"
          type="warning"
          show-icon
          :closable="false"
          class="register-closed-alert"
        />

        <el-form-item label="租户" prop="organizationId">
          <el-select
            v-model="registerForm.organizationId"
            placeholder="请选择租户"
            style="width: 100%"
            :loading="tenantLoading"
          >
            <el-option
              v-for="tenant in tenantList"
              :key="tenant.id"
              :label="tenant.tenantName"
              :value="tenant.id.toString()"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="请输入用户名" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            show-password
            placeholder="请输入密码"
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入密码"
          />
        </el-form-item>

        <DynamicAdmissionFields :fields="admissionConfig.fields" :model="registerForm" />

        <el-form-item label="内推码">
          <el-input
            v-model="registerForm.referralCode"
            :readonly="!!inviteToken"
            :placeholder="inviteToken ? '邀请链接已绑定内推码' : '可选，手动输入内推码'"
          />
        </el-form-item>

        <el-form-item label="验证码" prop="emailCode">
          <div class="email-code-row">
            <el-input v-model="registerForm.emailCode" placeholder="请输入邮箱验证码" />
            <el-button :disabled="!sendCodeReady || registrationClosed" @click="handleSendCode">
              {{ sendCodeText }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" style="width: 100%" :loading="registerLoading" :disabled="registrationClosed" @click="handleRegister">
            注册
          </el-button>
        </el-form-item>

        <el-form-item>
          <el-link type="primary" @click="$router.push('/login')">已有账号？去登录</el-link>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import DynamicAdmissionFields from '@/components/admission/DynamicAdmissionFields.vue'
import { getPublicTenantList } from '@/api/tenant'
import { getPublicAdmissionFormConfig } from '@/api/admission'
import { register, sendEmailVerificationCode } from '@/api/user'
import { acceptEmailInvitation, getEmailInvitation } from '@/api/email'
import { useThemeStore } from '@/stores/theme'
import { useTenantSelfServiceStore } from '@/stores/tenantSelfService'
import {
  buildAdmissionFieldRules,
  collectAdmissionFormPayload,
  createDefaultAdmissionConfig,
  normalizeAdmissionFormConfig,
  resolveAdmissionSuccessMessage,
  syncAdmissionFormModel,
} from '@/composables/useAdmissionFormRuntime'
import { normalizeTenantDirectory } from '@/utils/tenantBranding'

const router = useRouter()
const route = useRoute()
const themeStore = useThemeStore()
const tenantSelfServiceStore = useTenantSelfServiceStore()

const registerFormRef = ref(null)
const tenantLoading = ref(false)
const registerLoading = ref(false)
const tenantList = ref([])
const inviteToken = ref('')
const admissionConfig = ref(createDefaultAdmissionConfig('register'))
const admissionConfigRequestId = ref(0)

const registerForm = ref({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  organizationId: '',
  emailCode: '',
  inviteToken: '',
  referralCode: '',
})
syncAdmissionFormModel(registerForm.value, admissionConfig.value)

const sendCodeReady = ref(true)
const sendCodeText = ref('发送验证码')
let countdownTimer = null

const registerHeaderTitle = computed(() => admissionConfig.value.pageTitle || '注册')
const admissionSettings = computed(() => tenantSelfServiceStore.admissionSettings)
const registrationClosed = computed(() => admissionSettings.value.allowPublicRegister === false)
const registerHeaderDescription = computed(() => {
  if (inviteToken.value) {
    return '受邀注册，请确认邮箱和租户信息后完成注册'
  }
  return admissionConfig.value.pageDescription || '完成租户账号注册后即可进入对应社团链路'
})

const validatePassword = (_rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerForm.value.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const rules = computed(() => ({
  organizationId: [{ required: true, message: '请选择租户', trigger: 'change' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [{ validator: validatePassword, trigger: 'blur' }],
  ...buildAdmissionFieldRules(admissionConfig.value),
  emailCode: [{ required: true, message: '请输入邮箱验证码', trigger: 'blur' }],
}))

const fetchTenantList = async () => {
  tenantLoading.value = true
  try {
    const res = await getPublicTenantList()
    if (res.code === 200 && res.data) {
      tenantList.value = normalizeTenantDirectory(res.data)
      if (!registerForm.value.organizationId && tenantList.value.length > 0) {
        registerForm.value.organizationId = tenantList.value[0].id.toString()
      }
    }
  } catch (error) {
    console.error('获取租户列表失败:', error)
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
    admissionConfig.value = createDefaultAdmissionConfig('register')
    syncAdmissionFormModel(registerForm.value, admissionConfig.value)
    return
  }
  try {
    const [res] = await Promise.all([
      getPublicAdmissionFormConfig(tenantId),
      tenantSelfServiceStore.loadPublicConfig(tenantId, true)
    ])
    if (res.code === 200 && res.data) {
      if (requestId !== admissionConfigRequestId.value) {
        return
      }
      admissionConfig.value = normalizeAdmissionFormConfig(res.data, 'register')
      syncAdmissionFormModel(registerForm.value, admissionConfig.value)
      return
    }
  } catch (error) {
    console.error('获取注册页租户配置失败:', error)
  }
  if (requestId !== admissionConfigRequestId.value) {
    return
  }
  admissionConfig.value = createDefaultAdmissionConfig('register')
  syncAdmissionFormModel(registerForm.value, admissionConfig.value)
}

const applyInvitation = async () => {
  inviteToken.value = route.query.inviteToken ? String(route.query.inviteToken) : ''
  if (!inviteToken.value) {
    return
  }
  try {
    const res = await getEmailInvitation(inviteToken.value)
    if (res.code === 200 && res.data) {
      registerForm.value.organizationId = String(res.data.tenantId)
      registerForm.value.email = res.data.targetEmail || ''
      registerForm.value.inviteToken = inviteToken.value
      registerForm.value.referralCode = res.data.referralCode || ''
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
  if (registrationClosed.value) {
    ElMessage.warning('当前租户已关闭公开注册')
    return
  }
  if (!registerForm.value.organizationId) {
    ElMessage.warning('请先选择租户')
    return
  }
  if (!registerForm.value.email) {
    ElMessage.warning('请先填写邮箱')
    return
  }
  if (!sendCodeReady.value) {
    return
  }

  try {
    sendCodeReady.value = false
    sendCodeText.value = '发送中...'
    const res = await sendEmailVerificationCode({
      email: registerForm.value.email,
      tenantId: Number(registerForm.value.organizationId),
      scene: 'REGISTER',
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

const handleRegister = async () => {
  if (registrationClosed.value) {
    ElMessage.warning('当前租户已关闭公开注册')
    return
  }
  if (!registerFormRef.value) {
    return
  }

  await registerFormRef.value.validate(async (valid) => {
    if (!valid) {
      return
    }
    registerLoading.value = true
    try {
      const formPayload = collectAdmissionFormPayload(registerForm.value, admissionConfig.value)
      const res = await register({
        username: registerForm.value.username,
        password: registerForm.value.password,
        email: registerForm.value.email,
        name: registerForm.value.name,
        phone: registerForm.value.phone,
        organizationId: registerForm.value.organizationId,
        emailCode: registerForm.value.emailCode,
        inviteToken: registerForm.value.inviteToken,
        referralCode: registerForm.value.referralCode,
        formPayload,
      })
      if (res.code === 200) {
        if (inviteToken.value) {
          await acceptEmailInvitation(inviteToken.value)
        }
        ElMessage.success(resolveAdmissionSuccessMessage(admissionConfig.value, 'register'))
        router.push('/login')
      } else {
        ElMessage.error(res.msg || '注册失败')
      }
    } catch (error) {
      ElMessage.error(`注册失败: ${error.message || '未知错误'}`)
    } finally {
      registerLoading.value = false
    }
  })
}

watch(
  () => registerForm.value.organizationId,
  async (tenantId) => {
    await Promise.all([
      themeStore.applyTenantTheme(tenantId, 'frontend'),
      fetchAdmissionConfig(tenantId),
      tenantSelfServiceStore.loadPublicConfig(tenantId, true),
    ])
  },
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

<style scoped>
.register-container {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  overflow: hidden;
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
  background: rgba(0, 0, 0, 0.4);
  z-index: -1;
}

.register-card {
  width: 420px;
  z-index: 1;
  backdrop-filter: blur(10px);
  background: rgba(255, 255, 255, 0.9);
  border: none;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.register-card :deep(.el-card__header) {
  background: var(--rk-auth-gradient);
  padding: 20px;
}

.register-header {
  text-align: center;
}

.register-closed-alert {
  margin-bottom: 16px;
}

.register-title {
  margin: 0;
  color: #fff;
  font-size: 24px;
  font-weight: 700;
}

.register-subtitle {
  margin: 8px 0 0;
  color: rgba(255, 255, 255, 0.92);
  font-size: 13px;
}

.register-card :deep(.el-card__body) {
  padding: 30px;
}

.register-card :deep(.el-form-item__label) {
  font-weight: 500;
}

.register-card :deep(.el-input__wrapper) {
  border-radius: 8px;
}

.register-card :deep(.el-button--primary) {
  background: var(--rk-auth-gradient);
  border: none;
  border-radius: 8px;
  height: 44px;
  font-size: 16px;
  font-weight: 600;
}

.register-card :deep(.el-link) {
  font-size: 14px;
}

.email-code-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
  width: 100%;
}
</style>
