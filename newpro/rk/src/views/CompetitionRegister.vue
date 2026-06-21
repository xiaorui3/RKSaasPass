<template>
  <div class="competition-register-page">
    <div class="container">
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-container">
        <el-icon class="loading-icon"><Loading /></el-icon>
        <p>加载中...</p>
      </div>

      <!-- 内容区域 -->
      <div v-else-if="competition" class="register-content">
        <!-- 返回按钮 -->
        <div class="back-nav">
          <el-button text @click="handleBack">
            <el-icon><ArrowLeft /></el-icon>
            返回比赛详情
          </el-button>
        </div>

        <!-- 页面标题 -->
        <div class="page-header">
          <h1>比赛报名</h1>
          <div class="competition-info">
            <h2>{{ competition.title }}</h2>
            <div class="status-tags">
              <el-tag :type="getStatusType(competition.status)" size="large">
                {{ getStatusText(competition.status) }}
              </el-tag>
              <el-tag v-if="competition.competitionType" type="info" size="large">
                {{ competition.competitionType }}
              </el-tag>
            </div>
          </div>
        </div>

        <!-- 报名状态提示 -->
        <el-alert
          v-if="!canRegister"
          :title="registrationAlert.title"
          :type="registrationAlert.type"
          :description="registrationAlert.description"
          show-icon
          :closable="false"
          class="status-alert"
        />

        <!-- 已报名状态 -->
        <div v-if="registered" class="registered-info">
          <el-result
            icon="success"
            title="报名成功"
            :sub-title="`您已成功报名【${competition.title}】，请按时参加比赛`"
          >
            <template #extra>
              <div class="registration-details">
                <el-descriptions :column="2" border>
                  <el-descriptions-item label="报名人">{{ registrationInfo.name }}</el-descriptions-item>
                  <el-descriptions-item label="学号">{{ registrationInfo.studentId }}</el-descriptions-item>
                  <el-descriptions-item label="手机号">{{ registrationInfo.phone }}</el-descriptions-item>
                  <el-descriptions-item label="邮箱">{{ registrationInfo.email }}</el-descriptions-item>
                  <el-descriptions-item v-if="registrationInfo.teamName" label="队伍名称" :span="2">
                    {{ registrationInfo.teamName }}
                  </el-descriptions-item>
                  <el-descriptions-item label="报名时间" :span="2">
                    {{ formatDateTime(registrationInfo.registrationTime) }}
                  </el-descriptions-item>
                  <el-descriptions-item v-if="registrationInfo.remark" label="备注" :span="2">
                    {{ registrationInfo.remark }}
                  </el-descriptions-item>
                </el-descriptions>
              </div>
              <div class="action-buttons">
                <el-button @click="handleBack">返回详情</el-button>
                <el-popconfirm
                  title="确定要取消报名吗？"
                  confirm-button-text="确定"
                  cancel-button-text="取消"
                  @confirm="handleCancelRegistration"
                >
                  <template #reference>
                    <el-button type="danger" :loading="cancelLoading">取消报名</el-button>
                  </template>
                </el-popconfirm>
              </div>
            </template>
          </el-result>
        </div>

        <!-- 报名表单 -->
        <div v-else-if="canRegister && !registered" class="register-form-container">
          <!-- 比赛信息摘要 -->
          <el-card class="info-summary-card">
            <template #header>
              <div class="card-header">
                <el-icon><InfoFilled /></el-icon>
                <span>比赛信息</span>
              </div>
            </template>
            <div class="info-summary">
              <div class="info-row">
                <div class="info-item">
                  <label>主办方：</label>
                  <span>{{ competition.organizer || '暂无' }}</span>
                </div>
                <div class="info-item">
                  <label>比赛级别：</label>
                  <span>{{ competition.level || '暂无' }}</span>
                </div>
              </div>
              <div class="info-row">
                <div class="info-item">
                  <label>报名时间：</label>
                  <span>{{ formatDateTime(competition.registrationStart) }} 至 {{ formatDateTime(competition.registrationEnd) }}</span>
                </div>
              </div>
              <div class="info-row">
                <div class="info-item">
                  <label>比赛时间：</label>
                  <span>{{ formatDateTime(competition.competitionStart) }} 至 {{ formatDateTime(competition.competitionEnd) }}</span>
                </div>
              </div>
              <div class="info-row">
                <div class="info-item">
                  <label>比赛地点：</label>
                  <span>{{ competition.location || '线上比赛' }}</span>
                </div>
              </div>
              <div v-if="competition.maxParticipants" class="info-row">
                <div class="info-item highlight">
                  <label>报名人数：</label>
                  <span>{{ competition.registrationCount || 0 }} / {{ competition.maxParticipants }}</span>
                  <el-progress 
                    :percentage="registrationPercentage" 
                    :status="registrationPercentage >= 100 ? 'exception' : ''"
                    :stroke-width="8"
                    style="width: 200px; margin-left: 16px;"
                  />
                </div>
              </div>
            </div>
          </el-card>

          <!-- 报名表单 -->
          <el-card class="form-card">
            <template #header>
              <div class="card-header">
                <el-icon><Edit /></el-icon>
                <span>填写报名信息</span>
              </div>
            </template>

            <el-form
              ref="formRef"
              :model="formData"
              :rules="formRules"
              label-width="100px"
              label-position="right"
              class="register-form"
            >
              <!-- 个人信息 -->
              <div class="form-section">
                <h3 class="section-title">个人信息</h3>
                <el-row :gutter="24">
                  <el-col :span="12">
                    <el-form-item label="姓名" prop="name">
                      <el-input v-model="formData.name" placeholder="请输入姓名" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="学号" prop="studentId">
                      <el-input v-model="formData.studentId" placeholder="请输入学号" />
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row :gutter="24">
                  <el-col :span="12">
                    <el-form-item label="手机号" prop="phone">
                      <el-input v-model="formData.phone" placeholder="请输入手机号" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="邮箱" prop="email">
                      <el-input v-model="formData.email" placeholder="请输入邮箱" />
                    </el-form-item>
                  </el-col>
                </el-row>
              </div>

              <!-- 团队信息（如果是团队赛） -->
              <div v-if="isTeamCompetition" class="form-section">
                <h3 class="section-title">
                  团队信息
                  <el-tag type="warning" size="small">团队赛</el-tag>
                </h3>
                <el-row :gutter="24">
                  <el-col :span="12">
                    <el-form-item label="队伍名称" prop="teamName">
                      <el-input v-model="formData.teamName" placeholder="请输入队伍名称" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="队伍人数">
                      <el-input-number 
                        v-model="formData.teamSize" 
                        :min="1" 
                        :max="maxTeamSize" 
                        @change="handleTeamSizeChange"
                      />
                      <span class="team-size-hint">（含队长）</span>
                    </el-form-item>
                  </el-col>
                </el-row>

                <!-- 队员信息 -->
                <div class="members-section">
                  <div class="members-header">
                    <span>队员信息（队长请填写个人信息）</span>
                  </div>
                  <div 
                    v-for="(member, index) in formData.members" 
                    :key="index" 
                    class="member-item"
                  >
                    <div class="member-index">
                      <el-tag>{{ index === 0 ? '队长' : `队员${index}` }}</el-tag>
                    </div>
                    <el-row :gutter="16">
                      <el-col :span="8">
                        <el-form-item 
                          :label="index === 0 ? '' : '姓名'"
                          :prop="`members.${index}.name`"
                          :rules="index === 0 ? [] : [{ required: true, message: '请输入队员姓名', trigger: 'blur' }]"
                        >
                          <el-input 
                            v-model="member.name" 
                            :placeholder="index === 0 ? '队长姓名' : '队员姓名'"
                            :disabled="index === 0"
                          />
                        </el-form-item>
                      </el-col>
                      <el-col :span="8">
                        <el-form-item 
                          :label="index === 0 ? '' : '学号'"
                          :prop="`members.${index}.studentId`"
                          :rules="index === 0 ? [] : [{ required: true, message: '请输入队员学号', trigger: 'blur' }]"
                        >
                          <el-input 
                            v-model="member.studentId" 
                            :placeholder="index === 0 ? '队长学号' : '队员学号'"
                            :disabled="index === 0"
                          />
                        </el-form-item>
                      </el-col>
                      <el-col :span="8">
                        <el-form-item 
                          :label="index === 0 ? '' : '手机号'"
                          :prop="`members.${index}.phone`"
                          :rules="index === 0 ? [] : [{ required: false, message: '请输入队员手机号', trigger: 'blur' }]"
                        >
                          <el-input 
                            v-model="member.phone" 
                            :placeholder="index === 0 ? '队长手机号' : '队员手机号（选填）'"
                            :disabled="index === 0"
                          />
                        </el-form-item>
                      </el-col>
                    </el-row>
                  </div>
                </div>
              </div>

              <!-- 备注信息 -->
              <div class="form-section">
                <h3 class="section-title">其他信息</h3>
                <el-form-item label="备注" prop="remark">
                  <el-input
                    v-model="formData.remark"
                    type="textarea"
                    :rows="3"
                    placeholder="如有特殊需求或说明，请在此填写"
                    maxlength="500"
                    show-word-limit
                  />
                </el-form-item>
              </div>

              <!-- 提交按钮 -->
              <div class="form-actions">
                <el-button @click="handleBack">取消</el-button>
                <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
                  提交报名
                </el-button>
              </div>
            </el-form>
          </el-card>
        </div>

        <!-- 错误状态 -->
        <div v-else class="cannot-register">
          <el-empty description="当前无法报名">
            <el-button type="primary" @click="handleBack">返回比赛详情</el-button>
          </el-empty>
        </div>
      </div>

      <!-- 错误状态 -->
      <div v-else class="error-container">
        <el-empty description="比赛不存在或已下架">
          <el-button type="primary" @click="handleBackList">返回比赛列表</el-button>
        </el-empty>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  Loading,
  InfoFilled,
  Edit
} from '@element-plus/icons-vue'
import { getCompetitionDetail, registerCompetition, checkRegistered, cancelRegistration, getMyRegistration } from '@/api/competition'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const submitLoading = ref(false)
const cancelLoading = ref(false)
const competition = ref(null)
const registered = ref(false)
const registrationInfo = ref(null)
const formRef = ref(null)

// 获取比赛ID
const competitionId = computed(() => route.params.id)

// 判断是否为团队赛
const isTeamCompetition = computed(() => {
  if (!competition.value) return false
  const type = competition.value.competitionType || ''
  return type.includes('团队') || type.includes('组队') || type.toLowerCase().includes('team')
})

// 最大队伍人数
const maxTeamSize = computed(() => {
  return competition.value?.maxTeamSize || 5
})

// 报名进度百分比
const registrationPercentage = computed(() => {
  if (!competition.value || !competition.value.maxParticipants) return 0
  const current = competition.value.registrationCount || 0
  const max = competition.value.maxParticipants
  return Math.min(Math.round((current / max) * 100), 100)
})

// 表单数据
const formData = reactive({
  name: '',
  studentId: '',
  phone: '',
  email: '',
  teamName: '',
  teamSize: 1,
  members: [
    { name: '', studentId: '', phone: '' }
  ],
  remark: ''
})

// 表单验证规则
const formRules = {
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度在2到20个字符', trigger: 'blur' }
  ],
  studentId: [
    { required: true, message: '请输入学号', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9]{6,20}$/, message: '学号格式不正确', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  teamName: [
    { required: true, message: '请输入队伍名称', trigger: 'blur' }
  ]
}

// 比赛状态文本
const getStatusText = (status) => {
  const statusMap = {
    'UPCOMING': '即将开始',
    'ONGOING': '进行中',
    'COMPLETED': '已结束',
    'CANCELLED': '已取消'
  }
  return statusMap[status] || status || '未知'
}

// 比赛状态类型
const getStatusType = (status) => {
  const typeMap = {
    'UPCOMING': 'info',
    'ONGOING': 'warning',
    'COMPLETED': 'success',
    'CANCELLED': 'danger'
  }
  return typeMap[status] || 'info'
}

// 报名状态判断
const isRegistrationOpen = computed(() => {
  if (!competition.value) return false
  const now = new Date()
  const regStart = competition.value.registrationStart ? new Date(competition.value.registrationStart) : null
  const regEnd = competition.value.registrationEnd ? new Date(competition.value.registrationEnd) : null
  
  if (!regStart || !regEnd) return false
  return now >= regStart && now <= regEnd
})

const isRegistrationEnded = computed(() => {
  if (!competition.value) return false
  const now = new Date()
  const regEnd = competition.value.registrationEnd ? new Date(competition.value.registrationEnd) : null
  return regEnd && now > regEnd
})

const isRegistrationNotStarted = computed(() => {
  if (!competition.value) return false
  const now = new Date()
  const regStart = competition.value.registrationStart ? new Date(competition.value.registrationStart) : null
  return regStart && now < regStart
})

const isCompetitionFull = computed(() => {
  if (!competition.value) return false
  const maxParticipants = competition.value.maxParticipants
  const currentCount = competition.value.registrationCount || 0
  return maxParticipants && currentCount >= maxParticipants
})

// 是否可以报名
const canRegister = computed(() => {
  if (!competition.value) return false
  return isRegistrationOpen.value && 
         !isCompetitionFull.value && 
         competition.value.status !== 'COMPLETED' && 
         competition.value.status !== 'CANCELLED' &&
         competition.value.isPublished
})

// 报名状态提示
const registrationAlert = computed(() => {
  if (registered.value) {
    return { title: '您已报名', type: 'success', description: '您可以查看报名信息或取消报名' }
  }
  if (isRegistrationNotStarted.value) {
    return { title: '报名未开始', type: 'warning', description: `报名将于 ${formatDateTime(competition.value?.registrationStart)} 开始` }
  }
  if (isRegistrationEnded.value) {
    return { title: '报名已结束', type: 'error', description: '该比赛报名已截止' }
  }
  if (isCompetitionFull.value) {
    return { title: '名额已满', type: 'warning', description: '该比赛报名名额已满' }
  }
  if (competition.value?.status === 'CANCELLED') {
    return { title: '比赛已取消', type: 'error', description: '该比赛已取消' }
  }
  if (competition.value?.status === 'COMPLETED') {
    return { title: '比赛已结束', type: 'info', description: '该比赛已结束' }
  }
  return { title: '', type: 'info', description: '' }
})

// 格式化日期时间
const formatDateTime = (dateString) => {
  if (!dateString) return '待定'
  const date = new Date(dateString)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

// 处理队伍人数变化
const handleTeamSizeChange = (size) => {
  // 调整队员数组
  while (formData.members.length < size) {
    formData.members.push({ name: '', studentId: '', phone: '' })
  }
  while (formData.members.length > size) {
    formData.members.pop()
  }
}

// 监听个人姓名学号，自动同步到队长位置
watch([() => formData.name, () => formData.studentId, () => formData.phone], ([name, studentId, phone]) => {
  if (isTeamCompetition.value && formData.members.length > 0) {
    formData.members[0].name = name
    formData.members[0].studentId = studentId
    formData.members[0].phone = phone
  }
})

// 获取比赛详情
const fetchCompetitionDetail = async () => {
  if (!competitionId.value) {
    ElMessage.error('比赛ID无效')
    return
  }

  loading.value = true
  try {
    const res = await getCompetitionDetail(competitionId.value)
    if (res.data) {
      competition.value = res.data
      // 检查是否已登录，如果已登录则检查报名状态
      if (userStore.isLoggedIn) {
        await checkRegistrationStatus()
      }
    } else {
      ElMessage.error('比赛不存在')
    }
  } catch (error) {
    console.error('获取比赛详情失败:', error)
    ElMessage.error('获取比赛详情失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 检查报名状态
const checkRegistrationStatus = async () => {
  try {
    // 检查是否已报名
    const checkRes = await checkRegistered(competitionId.value)
    if (checkRes.data?.registered) {
      registered.value = true
      // 获取报名详情
      const regRes = await getMyRegistration(competitionId.value)
      if (regRes.data) {
        registrationInfo.value = regRes.data
      }
    }
  } catch (error) {
    console.error('检查报名状态失败:', error)
  }
}

// 预填充用户信息
const prefillUserInfo = () => {
  if (userStore.userInfo) {
    const info = userStore.userInfo
    formData.name = info.realName || info.username || ''
    formData.studentId = info.studentId || ''
    formData.phone = info.phone || ''
    formData.email = info.email || ''
    
    // 如果是团队赛，同步到队长信息
    if (isTeamCompetition.value && formData.members.length > 0) {
      formData.members[0].name = formData.name
      formData.members[0].studentId = formData.studentId
      formData.members[0].phone = formData.phone
    }
  }
}

// 返回比赛详情
const handleBack = () => {
  router.push(`/competition/${competitionId.value}`)
}

// 返回比赛列表
const handleBackList = () => {
  router.push('/competition')
}

// 提交报名
const handleSubmit = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再报名')
    router.push('/login?redirect=' + encodeURIComponent(route.fullPath))
    return
  }

  // 表单验证
  try {
    await formRef.value.validate()
  } catch (error) {
    ElMessage.error('请完善必填信息')
    return
  }

  submitLoading.value = true
  try {
    // 构建提交数据
    const submitData = {
      name: formData.name,
      studentId: formData.studentId,
      phone: formData.phone,
      email: formData.email,
      remark: formData.remark
    }

    // 如果是团队赛，添加团队信息
    if (isTeamCompetition.value) {
      submitData.teamName = formData.teamName
      submitData.teamSize = formData.teamSize
      submitData.members = formData.members.map(m => ({
        name: m.name,
        studentId: m.studentId,
        phone: m.phone
      }))
    }

    const res = await registerCompetition(competitionId.value, submitData)
    if (res.code === 200) {
      ElMessage.success('报名成功！')
      registered.value = true
      // 重新获取报名信息
      await checkRegistrationStatus()
    } else {
      ElMessage.error(res.message || '报名失败')
    }
  } catch (error) {
    console.error('报名失败:', error)
    ElMessage.error(error.response?.data?.message || '报名失败，请稍后重试')
  } finally {
    submitLoading.value = false
  }
}

// 取消报名
const handleCancelRegistration = async () => {
  cancelLoading.value = true
  try {
    const res = await cancelRegistration(competitionId.value, '用户主动取消')
    if (res.code === 200) {
      ElMessage.success('取消报名成功')
      registered.value = false
      registrationInfo.value = null
      // 刷新比赛信息以更新报名人数
      await fetchCompetitionDetail()
    } else {
      ElMessage.error(res.message || '取消报名失败')
    }
  } catch (error) {
    console.error('取消报名失败:', error)
    ElMessage.error(error.response?.data?.message || '取消报名失败')
  } finally {
    cancelLoading.value = false
  }
}

onMounted(() => {
  fetchCompetitionDetail().then(() => {
    // 预填充用户信息
    if (userStore.isLoggedIn && !registered.value) {
      prefillUserInfo()
    }
  })
})
</script>

<style lang="scss" scoped>
@use '../styles/variables.scss' as *;
@use '../styles/mixins.scss' as *;

.competition-register-page {
  padding: 40px 0;
  min-height: 60vh;
}

.loading-container {
  @include flex-center;
  flex-direction: column;
  padding: 100px 0;
  color: $text-secondary;

  .loading-icon {
    font-size: 40px;
    color: $primary-color;
    animation: spin 1s linear infinite;
  }

  p {
    margin-top: 16px;
    font-size: 16px;
  }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.back-nav {
  margin-bottom: 20px;
  
  .el-button {
    font-size: 14px;
    color: $text-secondary;
    
    &:hover {
      color: $primary-color;
    }
  }
}

.page-header {
  background: white;
  border-radius: $border-radius-lg;
  padding: 32px;
  box-shadow: $shadow-base;
  margin-bottom: 24px;

  h1 {
    font-size: 28px;
    font-weight: 700;
    color: $text-primary;
    margin: 0 0 20px;
  }

  .competition-info {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20px;

    h2 {
      font-size: 20px;
      font-weight: 600;
      color: $text-primary;
      margin: 0;
      flex: 1;
    }

    .status-tags {
      display: flex;
      gap: 8px;
      flex-shrink: 0;
    }
  }
}

.status-alert {
  margin-bottom: 24px;
}

.registered-info {
  background: white;
  border-radius: $border-radius-lg;
  padding: 40px;
  box-shadow: $shadow-base;

  .registration-details {
    margin: 24px 0;
  }

  .action-buttons {
    display: flex;
    justify-content: center;
    gap: 16px;
    margin-top: 24px;
  }
}

.register-form-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.info-summary-card {
  border-radius: $border-radius-lg;
  box-shadow: $shadow-sm;

  :deep(.el-card__header) {
    padding: 16px 24px;
    border-bottom: 1px solid $border-light;
  }
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;

  .el-icon {
    font-size: 18px;
    color: $primary-color;
  }
}

.info-summary {
  .info-row {
    display: flex;
    gap: 32px;
    padding: 12px 0;
    border-bottom: 1px dashed $border-light;

    &:last-child {
      border-bottom: none;
      padding-bottom: 0;
    }

    &:first-child {
      padding-top: 0;
    }
  }

  .info-item {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 1;

    label {
      flex-shrink: 0;
      font-size: 14px;
      color: $text-secondary;
    }

    span {
      font-size: 14px;
      color: $text-primary;
    }

    &.highlight {
      label {
        color: $primary-color;
        font-weight: 600;
      }

      span {
        color: $primary-color;
        font-weight: 600;
      }
    }
  }
}

.form-card {
  border-radius: $border-radius-lg;
  box-shadow: $shadow-sm;

  :deep(.el-card__header) {
    padding: 16px 24px;
    border-bottom: 1px solid $border-light;
  }

  :deep(.el-card__body) {
    padding: 32px;
  }
}

.register-form {
  .form-section {
    margin-bottom: 32px;
    padding-bottom: 24px;
    border-bottom: 1px solid $border-light;

    &:last-of-type {
      margin-bottom: 0;
      padding-bottom: 0;
      border-bottom: none;
    }
  }

  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: $text-primary;
    margin: 0 0 20px;
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.members-section {
  background: $bg-light;
  border-radius: $border-radius-base;
  padding: 20px;
  margin-top: 16px;

  .members-header {
    font-size: 14px;
    color: $text-secondary;
    margin-bottom: 16px;
  }
}

.member-item {
  padding: 16px;
  background: white;
  border-radius: $border-radius-base;
  margin-bottom: 12px;

  &:last-child {
    margin-bottom: 0;
  }

  .member-index {
    margin-bottom: 12px;
  }
}

.team-size-hint {
  margin-left: 8px;
  font-size: 12px;
  color: $text-light;
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding-top: 24px;
  margin-top: 24px;
  border-top: 1px solid $border-light;

  .el-button {
    min-width: 120px;
  }
}

.cannot-register,
.error-container {
  padding: 60px 0;
}

// 响应式设计
@media (max-width: $breakpoint-md) {
  .page-header {
    padding: 20px;

    h1 {
      font-size: 24px;
    }

    .competition-info {
      flex-direction: column;
      align-items: flex-start;
    }
  }

  .info-summary {
    .info-row {
      flex-direction: column;
      gap: 12px;
    }

    .info-item {
      width: 100%;
    }
  }

  .form-card {
    :deep(.el-card__body) {
      padding: 20px;
    }
  }

  .register-form {
    .el-col {
      width: 100% !important;
      max-width: 100% !important;
    }
  }

  .form-actions {
    flex-direction: column;

    .el-button {
      width: 100%;
    }
  }
}
</style>
