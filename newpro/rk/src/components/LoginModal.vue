<template>
  <el-dialog
    v-model="visible"
    :close-on-click-modal="true"
    :close-on-press-escape="true"
    :show-close="true"
    width="420px"
    class="login-modal"
    :destroy-on-close="true"
    :append-to-body="true"
    @close="handleClose"
  >
    <template #header>
      <div class="login-header">
        <h2 class="login-title">{{ t('loginModal.title') }}</h2>
        <p class="login-subtitle">{{ t('loginModal.subtitle') }}</p>
      </div>
    </template>

    <el-form
      ref="loginFormRef"
      :model="loginForm"
      :rules="rules"
      label-position="top"
      class="modal-auth-form"
    >
      <el-form-item :label="t('auth.tenant')" prop="organizationId">
        <el-select
          v-model="loginForm.organizationId"
          :placeholder="t('auth.selectTenantOrg')"
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

      <el-form-item :label="t('auth.username')" prop="username">
        <el-input
          v-model="loginForm.username"
          :placeholder="t('auth.inputUsername')"
          prefix-icon="User"
          autocomplete="off"
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
          autocomplete="off"
          @keyup.enter="handleLogin"
        />
      </el-form-item>

      <el-form-item>
        <el-button
          type="primary"
          style="width: 100%"
          :loading="loading"
          size="large"
          @click="handleLogin"
        >
          {{ loading ? t('auth.loginLoading') : t('auth.loginButton') }}
        </el-button>
      </el-form-item>

      <el-form-item>
        <div class="login-footer">
          <el-link type="primary" @click="handleRegister">{{ t('auth.goRegister') }}</el-link>
        </div>
      </el-form-item>
    </el-form>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getPublicTenantList } from '@/api/tenant'
import { normalizeTenantDirectory } from '@/utils/tenantBranding'

const { t } = useI18n()

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'login-success'])

const router = useRouter()
const loginFormRef = ref(null)
const userStore = useUserStore()
const loading = ref(false)
const tenantLoading = ref(false)
const tenantList = ref([])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const loginForm = ref({
  username: '',
  password: '',
  organizationId: ''
})

const rules = computed(() => ({
  organizationId: [{ required: true, message: t('auth.selectTenantOrg'), trigger: 'change' }],
  username: [{ required: true, message: t('auth.inputUsername'), trigger: 'blur' }],
  password: [{ required: true, message: t('auth.inputPassword'), trigger: 'blur' }]
}))

const fetchTenantList = async () => {
  tenantLoading.value = true
  try {
    const res = await getPublicTenantList()
    if (res.code === 200 && res.data) {
      tenantList.value = normalizeTenantDirectory(res.data)
      if (tenantList.value.length > 0 && !loginForm.value.organizationId) {
        loginForm.value.organizationId = tenantList.value[0].id.toString()
      }
    }
  } catch (error) {
    console.error('failed to load tenant list:', error)
  } finally {
    tenantLoading.value = false
  }
}

const handleLogin = async () => {
  if (!loginFormRef.value) {
    return
  }

  await loginFormRef.value.validate(async (valid) => {
    if (!valid) {
      return
    }

    loading.value = true
    try {
      const result = await userStore.login({
        username: loginForm.value.username,
        password: loginForm.value.password,
        organizationId: loginForm.value.organizationId
      })

      if (result.success) {
        ElMessage.success(t('auth.loginSuccess'))
        visible.value = false
        cleanupOverlays()
        setTimeout(() => {
          cleanupOverlays()
          emit('login-success')
        }, 350)
      } else {
        ElMessage.error(result.message || t('auth.loginFailed'))
      }
    } catch (error) {
      console.error('login failed:', error)
      ElMessage.error(t('auth.loginFailed'))
    } finally {
      loading.value = false
    }
  })
}

const cleanupOverlays = () => {
  const overlays = document.querySelectorAll('.el-overlay')
  overlays.forEach((overlay) => {
    const dialog = overlay.querySelector('.el-dialog')
    if (!dialog || dialog.classList.contains('login-modal')) {
      overlay.remove()
    }
  })

  const dialogs = document.querySelectorAll('.el-overlay-dialog')
  dialogs.forEach((dialog) => {
    if (!dialog.querySelector('.el-dialog:not(.login-modal)')) {
      dialog.remove()
    }
  })
}

const handleRegister = () => {
  visible.value = false
  router.push('/register')
}

const handleClose = () => {
  if (loading.value) {
    ElMessage.warning(t('auth.loginLoading'))
    return false
  }
  loginFormRef.value?.resetFields()
  cleanupOverlays()
  setTimeout(() => {
    cleanupOverlays()
  }, 150)
  return true
}

watch(visible, (val) => {
  if (!val) {
    loginFormRef.value?.resetFields()
  }
})

onMounted(() => {
  fetchTenantList()
})
</script>

<style scoped>
.login-header {
  text-align: center;
}

.login-title {
  margin: 0 0 8px;
  color: #333;
  font-size: 24px;
  font-weight: 700;
}

.login-subtitle {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.modal-auth-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.modal-auth-form :deep(.el-form-item__label) {
  height: auto;
  margin-bottom: 6px;
  padding: 0;
  color: #334155;
  font-weight: 500;
  line-height: 1.25;
}

.modal-auth-form :deep(.el-form-item__content) {
  line-height: 1.2;
}

.modal-auth-form :deep(.el-input__wrapper),
.modal-auth-form :deep(.el-select__wrapper) {
  min-height: 42px;
  border-radius: 10px;
}

.login-footer {
  display: flex;
  justify-content: center;
  width: 100%;
}

:deep(.el-dialog__header) {
  padding-bottom: 0;
}

:deep(.el-dialog__body) {
  padding-top: 16px;
}

:deep(.el-overlay) {
  &[style*='display: none'],
  &[style*='display:none'] {
    pointer-events: none !important;
    display: none !important;
  }
}

.login-modal[style*='display: none'],
.login-modal[style*='display:none'] {
  display: none !important;
  pointer-events: none !important;
}

:deep(.el-overlay:empty) {
  display: none !important;
}
</style>
