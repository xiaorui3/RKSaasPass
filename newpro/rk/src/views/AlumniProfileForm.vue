<template>
  <div class="alumni-profile-page">
    <section class="hero-panel">
      <div class="hero-panel__copy">
        <p class="eyebrow">校友信息采集</p>
        <h1>完善校友信息</h1>
        <p>
          你已由社团成员自动转入校友档案。请补充毕业后的联系方式和工作信息，
          该链接只能提交一次。
        </p>
      </div>
      <div class="hero-panel__badge">
        <span>一次性链接</span>
        <strong>{{ formData.name || '校友' }}</strong>
      </div>
    </section>

    <el-card class="form-card" v-loading="loading">
      <template v-if="errorMessage">
        <el-result icon="warning" title="链接不可用" :sub-title="errorMessage">
          <template #extra>
            <el-button type="primary" @click="loadForm">重新加载</el-button>
            <el-button @click="router.push('/')">返回首页</el-button>
          </template>
        </el-result>
      </template>

      <template v-else-if="submitSuccess || submitted">
        <el-result
          icon="success"
          title="校友信息已提交"
          sub-title="感谢补充校友资料，管理员可在校友管理中查看更新后的档案。"
        >
          <template #extra>
            <el-button type="primary" @click="router.push('/')">返回首页</el-button>
          </template>
        </el-result>
      </template>

      <template v-else>
        <div class="identity-grid">
          <div>
            <span>姓名</span>
            <strong>{{ formData.name || '-' }}</strong>
          </div>
          <div>
            <span>学号</span>
            <strong>{{ formData.studentId || '-' }}</strong>
          </div>
          <div>
            <span>邮箱</span>
            <strong>{{ formData.email || '-' }}</strong>
          </div>
          <div>
            <span>原部门/方向</span>
            <strong>{{ formData.department || formData.major || '-' }}</strong>
          </div>
        </div>

        <el-form
          ref="formRef"
          class="profile-form"
          :model="formData"
          :rules="rules"
          label-position="top"
        >
          <el-row :gutter="18">
            <el-col :xs="24" :md="12">
              <el-form-item label="工作城市" prop="workCity">
                <el-input v-model="formData.workCity" placeholder="例如：杭州" maxlength="80" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="当前联系方式" prop="currentContact">
                <el-input v-model="formData.currentContact" placeholder="手机号、邮箱或微信均可" maxlength="120" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="工作单位" prop="workUnit">
                <el-input v-model="formData.workUnit" placeholder="例如：某科技公司" maxlength="120" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="岗位/研究方向" prop="jobContent">
                <el-input v-model="formData.jobContent" placeholder="例如：后端开发、产品经理、研究生" maxlength="120" />
              </el-form-item>
            </el-col>
            <el-col :xs="24">
              <el-form-item label="技能标签">
                <el-input v-model="formData.skills" placeholder="多个标签可用逗号分隔" maxlength="255" />
              </el-form-item>
            </el-col>
            <el-col :xs="24">
              <el-form-item label="荣誉证书/代表成果">
                <el-input
                  v-model="formData.honorCertificates"
                  type="textarea"
                  :rows="3"
                  maxlength="500"
                  show-word-limit
                  placeholder="可填写竞赛奖项、证书、代表作品等"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24">
              <el-form-item label="给社团的建议">
                <el-input
                  v-model="formData.advice"
                  type="textarea"
                  :rows="3"
                  maxlength="500"
                  show-word-limit
                  placeholder="欢迎给学弟学妹或社团发展留下建议"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24">
              <el-form-item label="备注">
                <el-input
                  v-model="formData.notes"
                  type="textarea"
                  :rows="3"
                  maxlength="500"
                  show-word-limit
                  placeholder="其他希望管理员了解的信息"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <div class="visibility-row">
            <div>
              <strong>在校友风采中展示</strong>
              <p>关闭后资料仍会保存到校友管理，但前台不会展示你的档案。</p>
            </div>
            <el-switch v-model="formData.showTable" />
          </div>

          <div class="form-actions">
            <el-button @click="router.push('/')">取消</el-button>
            <el-button type="primary" :loading="submitting" @click="handleSubmit">
              提交校友信息
            </el-button>
          </div>
        </el-form>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAlumniProfileForm, submitAlumniProfileForm } from '@/api/alumni'

const route = useRoute()
const router = useRouter()

const formRef = ref(null)
const loading = ref(false)
const submitting = ref(false)
const submitted = ref(false)
const submitSuccess = ref(false)
const errorMessage = ref('')

const token = computed(() => String(route.query.token || '').trim())

const formData = reactive({
  token: '',
  name: '',
  studentId: '',
  email: '',
  major: '',
  department: '',
  position: '',
  workCity: '',
  workUnit: '',
  jobContent: '',
  currentContact: '',
  skills: '',
  honorCertificates: '',
  notes: '',
  advice: '',
  showTable: true
})

const rules = {
  currentContact: [
    { required: true, message: '请填写当前联系方式', trigger: 'blur' },
    { min: 5, message: '联系方式至少 5 个字符', trigger: 'blur' }
  ],
  workCity: [
    { required: true, message: '请填写工作或常驻城市', trigger: 'blur' }
  ]
}

function fillForm(data = {}) {
  Object.assign(formData, {
    token: data.token || '',
    name: data.name || '',
    studentId: data.studentId || '',
    email: data.email || '',
    major: data.major || '',
    department: data.department || '',
    position: data.position || '',
    workCity: data.workCity || '',
    workUnit: data.workUnit || '',
    jobContent: data.jobContent || '',
    currentContact: data.currentContact || '',
    skills: data.skills || '',
    honorCertificates: data.honorCertificates || '',
    notes: data.notes || '',
    advice: data.advice || '',
    showTable: data.showTable !== false
  })
  submitted.value = data.submitted === true
}

async function loadForm() {
  if (!token.value) {
    errorMessage.value = '缺少校友资料表单 token，请从邮件中的完整链接打开。'
    return
  }

  loading.value = true
  errorMessage.value = ''
  submitSuccess.value = false
  try {
    const res = await getAlumniProfileForm(token.value)
    if (res.code !== 200 || !res.data) {
      throw new Error(res.msg || '校友信息加载失败')
    }
    fillForm(res.data)
  } catch (error) {
    errorMessage.value = error.message || '校友信息加载失败'
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (!token.value || submitting.value) {
    return
  }
  await formRef.value?.validate()

  submitting.value = true
  try {
    const payload = {
      workCity: formData.workCity,
      workUnit: formData.workUnit,
      jobContent: formData.jobContent,
      currentContact: formData.currentContact,
      skills: formData.skills,
      honorCertificates: formData.honorCertificates,
      notes: formData.notes,
      advice: formData.advice,
      showTable: formData.showTable
    }
    const res = await submitAlumniProfileForm(token.value, payload)
    if (res.code !== 200 || res.data !== true) {
      throw new Error(res.msg || '提交校友信息失败')
    }
    submitSuccess.value = true
    ElMessage.success('校友信息提交成功')
  } catch (error) {
    ElMessage.error(error.message || '提交校友信息失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadForm()
})
</script>

<style scoped>
.alumni-profile-page {
  min-height: 100vh;
  padding: 42px 16px 56px;
  background:
    radial-gradient(circle at 8% 12%, rgba(15, 118, 110, 0.16), transparent 28%),
    radial-gradient(circle at 88% 0, rgba(234, 179, 8, 0.16), transparent 26%),
    linear-gradient(135deg, #f7f4ec 0%, #f8fafc 52%, #eef6f3 100%);
}

.hero-panel,
.form-card {
  width: min(980px, 100%);
  margin: 0 auto;
}

.hero-panel {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: stretch;
  margin-bottom: 18px;
  padding: 32px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 24px 80px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(18px);
}

.hero-panel__copy {
  max-width: 660px;
}

.eyebrow {
  margin: 0 0 10px;
  color: #0f766e;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.18em;
}

.hero-panel h1 {
  margin: 0;
  color: #10211d;
  font-size: clamp(30px, 5vw, 52px);
  line-height: 1.05;
  letter-spacing: -0.04em;
}

.hero-panel p {
  margin: 16px 0 0;
  color: #475569;
  font-size: 16px;
  line-height: 1.8;
}

.hero-panel__badge {
  min-width: 180px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  padding: 20px;
  border-radius: 22px;
  color: #fff;
  background: linear-gradient(160deg, #0f766e, #134e4a);
}

.hero-panel__badge span {
  color: rgba(255, 255, 255, 0.76);
  font-size: 13px;
}

.hero-panel__badge strong {
  font-size: 22px;
}

.form-card {
  border: 0;
  border-radius: 26px;
  box-shadow: 0 22px 70px rgba(15, 23, 42, 0.1);
}

.identity-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 24px;
}

.identity-grid div {
  min-width: 0;
  padding: 15px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  background: #f8fafc;
}

.identity-grid span {
  display: block;
  color: #64748b;
  font-size: 12px;
  margin-bottom: 6px;
}

.identity-grid strong {
  display: block;
  overflow: hidden;
  color: #0f172a;
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-form :deep(.el-form-item__label) {
  color: #1e293b;
  font-weight: 700;
}

.visibility-row {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
  margin: 8px 0 24px;
  padding: 18px 20px;
  border-radius: 18px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.visibility-row strong {
  color: #0f172a;
}

.visibility-row p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 760px) {
  .alumni-profile-page {
    padding: 18px 10px 32px;
  }

  .hero-panel {
    flex-direction: column;
    padding: 22px;
    border-radius: 22px;
  }

  .hero-panel p {
    font-size: 14px;
  }

  .hero-panel__badge {
    min-width: 0;
  }

  .identity-grid {
    grid-template-columns: 1fr;
  }

  .visibility-row,
  .form-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .form-actions :deep(.el-button) {
    width: 100%;
    margin-left: 0;
  }
}
</style>
