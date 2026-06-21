<template>
  <div class="club-profile-config-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>社团概况配置</span>
          <el-button type="primary" :loading="saving" @click="handleSave">保存配置</el-button>
        </div>
      </template>

      <el-form v-loading="loading" label-width="120px">
        <el-form-item label="页面标题">
          <el-input v-model="form.pageTitle" aria-label="页面标题" />
        </el-form-item>
        <el-form-item label="页面说明">
          <el-input v-model="form.pageDescription" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="简介标题">
          <el-input v-model="form.introTitle" />
        </el-form-item>
        <el-form-item label="社团简介">
          <el-input v-model="introText" type="textarea" :rows="6" placeholder="每行一段简介" />
        </el-form-item>
        <el-form-item label="发展历程">
          <el-input v-model="historyText" type="textarea" :rows="6" placeholder="每行格式：年份|标题|描述" />
        </el-form-item>
        <el-form-item label="核心方向">
          <el-input v-model="form.missionTitle" />
        </el-form-item>
        <el-form-item label="方向卡片">
          <el-input v-model="missionText" type="textarea" :rows="6" placeholder="每行格式：标题|描述" />
        </el-form-item>
        <el-form-item label="联系邮箱">
          <el-input v-model="form.contactEmail" />
        </el-form-item>
        <el-form-item label="联系地址">
          <el-input v-model="form.contactAddress" />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getCurrentClubProfileConfig, saveCurrentClubProfileConfig } from '@/api/club-profile'

const loading = ref(false)
const saving = ref(false)

const form = reactive({
  pageTitle: '',
  pageDescription: '',
  introTitle: '',
  introParagraphs: [],
  history: [],
  missionTitle: '',
  missionCards: [],
  contactEmail: '',
  contactAddress: ''
})

const introText = computed({
  get: () => (form.introParagraphs || []).join('\n'),
  set: (value) => {
    form.introParagraphs = value.split('\n').map((item) => item.trim()).filter(Boolean)
  }
})

const historyText = computed({
  get: () => (form.history || []).map((item) => [item.year, item.title, item.description].filter(Boolean).join('|')).join('\n'),
  set: (value) => {
    form.history = value.split('\n').map((line) => line.trim()).filter(Boolean).map((line) => {
      const [year, title, description] = line.split('|')
      return {
        year: (year || '').trim(),
        title: (title || '').trim(),
        description: (description || '').trim()
      }
    })
  }
})

const missionText = computed({
  get: () => (form.missionCards || []).map((item) => [item.title, item.description].filter(Boolean).join('|')).join('\n'),
  set: (value) => {
    form.missionCards = value.split('\n').map((line) => line.trim()).filter(Boolean).map((line) => {
      const [title, description] = line.split('|')
      return {
        title: (title || '').trim(),
        description: (description || '').trim()
      }
    })
  }
})

function applyConfig(data) {
  form.pageTitle = data.pageTitle || ''
  form.pageDescription = data.pageDescription || ''
  form.introTitle = data.introTitle || ''
  form.introParagraphs = Array.isArray(data.introParagraphs) ? data.introParagraphs : []
  form.history = Array.isArray(data.history) ? data.history : []
  form.missionTitle = data.missionTitle || ''
  form.missionCards = Array.isArray(data.missionCards) ? data.missionCards : []
  form.contactEmail = data.contactEmail || ''
  form.contactAddress = data.contactAddress || ''
}

async function fetchConfig() {
  loading.value = true
  try {
    const res = await getCurrentClubProfileConfig()
    if (res.code === 200 && res.data) {
      applyConfig(res.data)
    }
  } catch (error) {
    ElMessage.error(error.message || '获取社团概况配置失败')
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    const res = await saveCurrentClubProfileConfig({
      pageTitle: form.pageTitle,
      pageDescription: form.pageDescription,
      introTitle: form.introTitle,
      introParagraphs: form.introParagraphs,
      history: form.history,
      missionTitle: form.missionTitle,
      missionCards: form.missionCards,
      contactEmail: form.contactEmail,
      contactAddress: form.contactAddress
    })
    if (res.code === 200 && res.data) {
      applyConfig(res.data)
      ElMessage.success('社团概况配置已保存')
    }
  } catch (error) {
    ElMessage.error(error.message || '保存社团概况配置失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchConfig()
})
</script>

<style scoped>
.club-profile-config-page .card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
