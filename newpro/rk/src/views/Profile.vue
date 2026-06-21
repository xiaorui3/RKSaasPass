<template>
  <div class="profile-page">
    <user-hub-shell
      eyebrow="个人中心"
      :title="displayUserName"
      :description="activeSection.description"
      :meta-items="profileMetaItems"
      :stat-items="profileStats"
    >
      <template #actions>
        <el-button @click="goHome">
          <el-icon><House /></el-icon>
          返回首页
        </el-button>
        <el-button type="danger" plain @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
          退出登录
        </el-button>
      </template>

      <template #filters>
        <div class="profile-band">
          <div class="profile-band__copy">
            <span class="profile-band__eyebrow">当前模块</span>
            <strong>{{ activeSection.title }}</strong>
            <p>{{ activeSection.helper }}</p>
          </div>
          <el-tag v-if="currentTenantMembership" type="success" effect="light" size="large">
            当前租户：{{ currentTenantMembership.tenantName }}
          </el-tag>
        </div>
      </template>

      <div class="profile-overview-grid">
        <user-hub-panel class="profile-overview-card" title="账号概览" description="把头像、身份与联系方式固定在页面顶部，减少来回查找。">
          <div class="profile-summary">
            <div class="avatar-section">
              <el-avatar :size="92" :src="userInfo?.icon || defaultAvatar" />
              <el-upload
                class="avatar-upload"
                :show-file-list="false"
                :before-upload="beforeAvatarUpload"
                :http-request="handleAvatarUpload"
              >
                <el-button size="small" type="primary" plain>
                  <el-icon><Camera /></el-icon>
                  更新头像
                </el-button>
              </el-upload>
            </div>
            <h3 class="user-name">{{ displayUserName }}</h3>
            <p class="user-role">{{ userInfo?.roleName || '普通成员' }}</p>
            <user-hub-key-value-list :items="accountFacts" />
          </div>
        </user-hub-panel>

        <user-hub-panel class="profile-overview-card" title="租户身份" description="把当前账号在不同租户下的身份集中展示，避免切换时信息断层。">
          <template #headerActions>
            <el-button size="small" type="primary" plain :disabled="tenantMemberships.length <= 1" @click="openTenantSwitchDialog">
              一键切换
            </el-button>
          </template>
          <div v-if="tenantMemberships.length === 0" class="tenant-empty">
            暂无租户成员信息
          </div>
          <div v-else class="tenant-membership-list">
            <div
              v-for="item in tenantMemberships"
              :key="buildTenantSwitchKey(item)"
              class="tenant-membership-item"
              :class="{ current: item.isCurrent }"
            >
              <div class="tenant-membership-main">
                <span class="tenant-membership-name">{{ item.tenantName }}</span>
                <el-tag size="small" :type="item.isCurrent ? 'success' : 'info'">
                  {{ item.roleName }}
                </el-tag>
              </div>
              <div class="tenant-membership-sub">
                <span>账号：{{ item.username }}</span>
                <span v-if="item.isCurrent">当前租户</span>
              </div>
            </div>
          </div>
        </user-hub-panel>

        <user-hub-panel
          class="profile-nav-panel profile-overview-card"
          title="快捷导航"
          description="资料、安全和活动记录放在同一入口区，避免还要滚动到页面底部找功能。"
        >
          <user-hub-section-nav
            v-model="activeMenu"
            :items="profileSections"
            @change="handleMenuSelect"
          />
        </user-hub-panel>
      </div>

      <user-hub-panel
        v-if="activeMenu === 'info'"
        title="个人资料"
        description="更新昵称、联系方式和个人介绍，沿用现有保存接口与表单校验。"
      >
        <el-form
          ref="infoFormRef"
          :model="infoForm"
          :rules="infoRules"
          label-position="top"
          class="profile-form"
        >
          <div class="form-grid">
            <el-form-item label="昵称" prop="name">
              <el-input v-model="infoForm.name" placeholder="请输入昵称" maxlength="20" />
            </el-form-item>
            <el-form-item label="手机号" prop="cellPhone">
              <el-input v-model="infoForm.cellPhone" placeholder="请输入手机号" maxlength="11" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="infoForm.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="QQ号" prop="qq">
              <el-input v-model="infoForm.qq" placeholder="请输入QQ号" maxlength="15" />
            </el-form-item>
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="infoForm.gender">
                <el-radio :value="0">男</el-radio>
                <el-radio :value="1">女</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="个人介绍" prop="intro" class="span-2">
              <el-input
                v-model="infoForm.intro"
                type="textarea"
                :rows="5"
                placeholder="介绍一下自己"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
          </div>
          <div class="form-actions">
            <el-button type="primary" :loading="saving" @click="handleUpdateInfo">保存修改</el-button>
          </div>
        </el-form>
      </user-hub-panel>

      <user-hub-panel
        v-if="activeMenu === 'info'"
        title="当前租户内推码"
        description="仅在当前租户下生效，可刷新生成并查看有效状态。"
      >
        <template #headerActions>
          <el-button
            size="small"
            type="primary"
            :loading="referralRefreshing"
            :disabled="referralLoading"
            @click="handleRefreshReferralCode"
          >
            <el-icon><Refresh /></el-icon>
            刷新内推码
          </el-button>
        </template>

        <div class="referral-card-body">
          <div v-if="referralLoading" class="referral-loading">
            <el-skeleton :rows="2" animated />
          </div>
          <div v-else-if="referralCode" class="referral-code-content">
            <div class="referral-code-value">{{ referralCode.code }}</div>
            <div class="referral-code-meta">
              <span>过期时间：{{ referralCode.expiresAt ? formatDate(referralCode.expiresAt) : '未设置' }}</span>
              <span>最大使用：{{ referralCode.maxUses ?? '无限' }}</span>
            </div>
            <div class="referral-code-meta">
              <span>状态：{{ referralCode.status === 1 ? '有效' : '失效' }}</span>
            </div>
          </div>
          <div v-else class="referral-empty">
            <el-empty description="当前租户暂无有效内推码">
              <el-button type="primary" :loading="referralRefreshing" @click="handleRefreshReferralCode">
                <el-icon><Refresh /></el-icon>
                生成内推码
              </el-button>
            </el-empty>
          </div>
        </div>
      </user-hub-panel>

      <user-hub-panel
        v-if="activeMenu === 'password'"
        title="修改密码"
        description="建议定期更换密码，避免与其他系统复用同一套凭据。"
      >
        <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-position="top"
          class="profile-form password-form"
        >
          <el-form-item label="原密码" prop="oldPassword">
            <el-input
              v-model="passwordForm.oldPassword"
              type="password"
              placeholder="请输入原密码"
              show-password
            />
          </el-form-item>
          <el-form-item label="新密码" prop="password">
            <el-input
              v-model="passwordForm.password"
              type="password"
              placeholder="请输入新密码"
              show-password
            />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              show-password
            />
          </el-form-item>
          <div class="form-actions">
            <el-button type="primary" :loading="changingPassword" @click="handleChangePassword">
              修改密码
            </el-button>
            <el-button @click="resetPasswordForm">重置</el-button>
          </div>
        </el-form>
      </user-hub-panel>

      <user-hub-panel
        v-if="activeMenu === 'activities'"
        title="活动记录"
        description="查看你的活动参与情况，并可直接跳转到对应活动详情。"
      >
        <template #headerActions>
          <el-tag type="info" effect="light">共 {{ activityTotal }} 条记录</el-tag>
        </template>

        <div v-if="activityLoading" class="loading-container">
          <el-icon class="is-loading" :size="32"><Loading /></el-icon>
        </div>
        <div v-else-if="activities.length === 0" class="empty-container">
          <el-empty description="暂无活动记录">
            <el-button type="primary" @click="$router.push('/activities')">去参加活动</el-button>
          </el-empty>
        </div>
        <div v-else class="record-list">
          <div
            v-for="activity in activities"
            :key="activity.id"
            class="record-item"
            :class="{ clickable: !!activity.id }"
            @click="openActivityDetail(activity)"
          >
            <div class="record-cover">
              <el-image :src="activity.cover || defaultCover" fit="cover" />
            </div>
            <div class="record-main">
              <h4>{{ activity.activityName }}</h4>
              <p class="record-meta">
                <el-icon><Clock /></el-icon>
                {{ formatDate(activity.startTime) }} ~ {{ formatDate(activity.endTime) }}
              </p>
              <p v-if="activity.location" class="record-meta">
                <el-icon><Location /></el-icon>
                {{ activity.location }}
              </p>
            </div>
            <div class="record-status">
              <el-tag :type="getActivityStatusType(activity.status)">
                {{ getActivityStatusText(activity.status) }}
              </el-tag>
            </div>
          </div>
        </div>
        <el-pagination
          v-if="activityTotal > 10"
          v-model:current-page="activityPage"
          :page-size="10"
          :total="activityTotal"
          layout="prev, pager, next"
          @current-change="fetchActivities"
        />
      </user-hub-panel>

      <user-hub-panel
        v-if="activeMenu === 'competitions'"
        title="比赛记录"
        description="查看报名进度、截止时间和对应比赛详情。"
      >
        <template #headerActions>
          <el-tag type="info" effect="light">共 {{ competitionTotal }} 条记录</el-tag>
        </template>

        <div v-if="competitionLoading" class="loading-container">
          <el-icon class="is-loading" :size="32"><Loading /></el-icon>
        </div>
        <div v-else-if="competitions.length === 0" class="empty-container">
          <el-empty description="暂无比赛记录">
            <el-button type="primary" @click="$router.push('/competition')">去参加比赛</el-button>
          </el-empty>
        </div>
        <div v-else class="record-list">
          <div
            v-for="competition in competitions"
            :key="competition.id"
            class="record-item"
            :class="{ clickable: !!competition.id }"
            @click="openCompetitionDetail(competition)"
          >
            <div class="record-cover">
              <el-image :src="competition.cover || defaultCover" fit="cover" />
            </div>
            <div class="record-main">
              <h4>{{ competition.competitionName }}</h4>
              <p class="record-meta">
                <el-icon><Clock /></el-icon>
                报名截止：{{ formatDate(competition.registrationDeadline) }}
              </p>
              <p v-if="competition.competitionType" class="record-meta">
                <el-icon><Collection /></el-icon>
                {{ competition.competitionType }}
              </p>
            </div>
            <div class="record-status">
              <el-tag :type="getCompetitionStatusType(competition.status)">
                {{ getCompetitionStatusText(competition.status) }}
              </el-tag>
              <p v-if="competition.registrationTime" class="registration-time">
                报名于 {{ formatDate(competition.registrationTime) }}
              </p>
            </div>
          </div>
        </div>
        <el-pagination
          v-if="competitionTotal > 10"
          v-model:current-page="competitionPage"
          :page-size="10"
          :total="competitionTotal"
          layout="prev, pager, next"
          @current-change="fetchCompetitions"
        />
      </user-hub-panel>

      <template #legacyAside>
        <user-hub-panel title="账户概览" description="头像、身份和基础联系信息集中展示。">
          <div class="profile-summary">
            <div class="avatar-section">
              <el-avatar :size="92" :src="userInfo?.icon || defaultAvatar" />
              <el-upload
                class="avatar-upload"
                :show-file-list="false"
                :before-upload="beforeAvatarUpload"
                :http-request="handleAvatarUpload"
              >
                <el-button size="small" type="primary" plain>
                  <el-icon><Camera /></el-icon>
                  更换头像
                </el-button>
              </el-upload>
            </div>
            <h3 class="user-name">{{ displayUserName }}</h3>
            <p class="user-role">{{ userInfo?.roleName || '普通会员' }}</p>
            <user-hub-key-value-list :items="accountFacts" />
          </div>
        </user-hub-panel>

        <user-hub-panel title="模块导航" description="在资料、安全和个人记录之间快速切换。">
          <user-hub-section-nav
            v-model="activeMenu"
            :items="profileSections"
            @change="handleMenuSelect"
          />
        </user-hub-panel>

        <user-hub-panel title="租户身份" description="当前账号可切换的租户成员身份一览。">
          <template #headerActions>
            <el-button size="small" type="primary" plain :disabled="tenantMemberships.length <= 1" @click="openTenantSwitchDialog">
              一键切换
            </el-button>
          </template>
          <div v-if="tenantMemberships.length === 0" class="tenant-empty">
            暂无租户成员信息
          </div>
          <div v-else class="tenant-membership-list">
            <div
              v-for="item in tenantMemberships"
              :key="buildTenantSwitchKey(item)"
              class="tenant-membership-item"
              :class="{ current: item.isCurrent }"
            >
              <div class="tenant-membership-main">
                <span class="tenant-membership-name">{{ item.tenantName }}</span>
                <el-tag size="small" :type="item.isCurrent ? 'success' : 'info'">
                  {{ item.roleName }}
                </el-tag>
              </div>
              <div class="tenant-membership-sub">
                <span>账号：{{ item.username }}</span>
                <span v-if="item.isCurrent">当前租户</span>
              </div>
            </div>
          </div>
        </user-hub-panel>
      </template>
    </user-hub-shell>

    <el-dialog
      v-model="tenantSwitchDialogVisible"
      title="切换身份"
      width="460px"
      :close-on-click-modal="false"
    >
      <el-radio-group v-model="selectedTenantSwitchKey" class="tenant-switch-dialog-list">
        <el-radio
          v-for="item in tenantMemberships"
          :key="buildTenantSwitchKey(item)"
          :label="buildTenantSwitchKey(item)"
          class="tenant-switch-dialog-item"
        >
          <div class="tenant-switch-dialog-title">{{ item.tenantName }} · {{ item.roleName }}</div>
          <div class="tenant-switch-dialog-meta">{{ item.username }}</div>
        </el-radio>
      </el-radio-group>

      <template #footer>
        <el-button @click="tenantSwitchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="tenantSwitching" @click="handleTenantMembershipSwitch">
          确认切换
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Camera,
  Clock,
  User,
  Lock,
  Calendar,
  Trophy,
  House,
  Location,
  Collection,
  Loading,
  Refresh,
  SwitchButton
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { normalizeUserInfoMedia } from '@/utils/mediaUrl'
import { uploadManagedFile } from '@/utils/fileUpload'
import { userApi } from '@/api/user'
import { getMyReferralCode, refreshMyReferralCode } from '@/api/referral'
import { getMyActivities } from '@/api/activity'
import { getMyCompetitions } from '@/api/competition'
import UserHubKeyValueList from '@/components/user-hub/UserHubKeyValueList.vue'
import UserHubPanel from '@/components/user-hub/UserHubPanel.vue'
import UserHubSectionNav from '@/components/user-hub/UserHubSectionNav.vue'
import UserHubShell from '@/components/user-hub/UserHubShell.vue'

const router = useRouter()
const userStore = useUserStore()

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
const defaultCover = 'https://via.placeholder.com/200x120'

const userInfo = ref(null)
const activeMenu = ref('info')
const infoFormRef = ref(null)
const passwordFormRef = ref(null)
const saving = ref(false)
const changingPassword = ref(false)
const activityLoading = ref(false)
const competitionLoading = ref(false)
const activities = ref([])
const activityPage = ref(1)
const activityTotal = ref(0)
const competitions = ref([])
const competitionPage = ref(1)
const competitionTotal = ref(0)
const referralCode = ref(null)
const referralLoading = ref(false)
const referralRefreshing = ref(false)
const tenantSwitchDialogVisible = ref(false)
const tenantSwitching = ref(false)
const selectedTenantSwitchKey = ref('')

const infoForm = reactive({
  name: '',
  cellPhone: '',
  email: '',
  qq: '',
  gender: 0,
  intro: ''
})

const passwordForm = reactive({
  oldPassword: '',
  password: '',
  confirmPassword: ''
})

const tenantMemberships = computed(() => userStore.switchableTenantViews || [])
const currentTenantMembership = computed(() =>
  tenantMemberships.value.find((item) => item.isCurrent) || null
)
const currentTenantIdKey = computed(
  () => currentTenantMembership.value?.tenantIdValue || currentTenantMembership.value?.tenantId
)
const selectedTenantSwitchItem = computed(() =>
  tenantMemberships.value.find((item) => buildTenantSwitchKey(item) === selectedTenantSwitchKey.value) || null
)
const displayUserName = computed(() => userInfo.value?.name || userInfo.value?.username || '加载中...')
const profileSections = [
  {
    key: 'info',
    title: '个人资料',
    description: '维护昵称、联系方式和租户内推码。',
    helper: '集中处理个人资料和当前租户信息。',
    icon: User
  },
  {
    key: 'password',
    title: '账号安全',
    description: '修改登录密码并完成安全维护。',
    helper: '密码修改后会沿用原有逻辑要求重新登录。',
    icon: Lock
  },
  {
    key: 'activities',
    title: '活动记录',
    description: '查看已参与活动和报名状态。',
    helper: '可直接跳转到对应活动详情页面。',
    icon: Calendar
  },
  {
    key: 'competitions',
    title: '比赛记录',
    description: '查看比赛报名、进度和当前状态。',
    helper: '保留原有比赛详情跳转行为。',
    icon: Trophy
  }
]
const activeSection = computed(
  () => profileSections.find((item) => item.key === activeMenu.value) || profileSections[0]
)
const accountFacts = computed(() => [
  { label: '用户名', value: userInfo.value?.username || '-' },
  { label: '邮箱', value: userInfo.value?.email || '-' },
  { label: '手机号', value: userInfo.value?.cellPhone || '-' },
  { label: '加入时间', value: formatDate(userInfo.value?.createTime) }
])
const profileMetaItems = computed(() => {
  const items = [`模块 ${activeSection.value.title}`]
  if (currentTenantMembership.value?.tenantName) {
    items.push(`当前租户 ${currentTenantMembership.value.tenantName}`)
  }
  if (userInfo.value?.username) {
    items.push(`账号 ${userInfo.value.username}`)
  }
  return items
})
const profileStats = computed(() => [
  {
    label: '租户身份',
    value: String(tenantMemberships.value.length),
    helper: currentTenantMembership.value?.tenantName || '暂无当前租户',
    tone: tenantMemberships.value.length > 0 ? 'info' : undefined
  },
  {
    label: '活动记录',
    value: String(activityTotal.value),
    helper: activeMenu.value === 'activities' ? '当前模块已加载' : '切换到活动记录可刷新',
    tone: activityTotal.value > 0 ? 'success' : undefined
  },
  {
    label: '比赛记录',
    value: String(competitionTotal.value),
    helper: activeMenu.value === 'competitions' ? '当前模块已加载' : '切换到比赛记录可刷新',
    tone: competitionTotal.value > 0 ? 'warning' : undefined
  },
  {
    label: '内推码',
    value: referralCode.value?.code ? '已生成' : '未生成',
    helper: referralCode.value?.expiresAt ? formatDate(referralCode.value.expiresAt) : '当前租户暂无有效内推码',
    tone: referralCode.value?.code ? 'success' : undefined
  }
])

const validatePhone = (rule, value, callback) => {
  if (value && !/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('请输入正确的手机号'))
  } else {
    callback()
  }
}

const infoRules = {
  name: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度为 2-20 个字符', trigger: 'blur' }
  ],
  cellPhone: [{ validator: validatePhone, trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }]
}

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

const getActivityStatusType = (status) => {
  const statusMap = {
    1: 'info',
    2: 'primary',
    3: 'success',
    4: 'warning'
  }
  return statusMap[status] || 'info'
}

const getActivityStatusText = (status) => {
  const statusMap = {
    1: '未开始',
    2: '报名中',
    3: '进行中',
    4: '已结束'
  }
  return statusMap[status] || '未知'
}

const getCompetitionStatusType = (status) => {
  const statusMap = {
    PENDING: 'info',
    REGISTRATION: 'primary',
    ONGOING: 'success',
    FINISHED: 'warning',
    CANCELLED: 'danger'
  }
  return statusMap[status] || 'info'
}

const getCompetitionStatusText = (status) => {
  const statusMap = {
    PENDING: '待开始',
    REGISTRATION: '报名中',
    ONGOING: '进行中',
    FINISHED: '已结束',
    CANCELLED: '已取消'
  }
  return statusMap[status] || '未知'
}

const goHome = () => {
  router.push('/')
}

const buildTenantSwitchKey = (item) =>
  `${item?.authUserId || ''}:${item?.tenantIdValue || item?.tenantId || ''}:${item?.roleId || ''}`

const openTenantSwitchDialog = () => {
  selectedTenantSwitchKey.value = currentTenantMembership.value
    ? buildTenantSwitchKey(currentTenantMembership.value)
    : buildTenantSwitchKey(tenantMemberships.value[0] || {})
  tenantSwitchDialogVisible.value = true
}

const handleTenantMembershipSwitch = async () => {
  if (!selectedTenantSwitchItem.value) {
    ElMessage.warning('请选择要切换的身份')
    return
  }
  if (selectedTenantSwitchItem.value.isCurrent) {
    tenantSwitchDialogVisible.value = false
    return
  }
  tenantSwitching.value = true
  try {
    const result = await userStore.switchTenant(selectedTenantSwitchItem.value)
    if (result.success) {
      tenantSwitchDialogVisible.value = false
      ElMessage.success('身份已切换')
      window.location.reload()
      return
    }
    ElMessage.error(result.message || '身份切换失败')
  } finally {
    tenantSwitching.value = false
  }
}

const openActivityDetail = (activity) => {
  if (!activity?.id) return
  router.push(`/activities/${activity.id}`)
}

const openCompetitionDetail = (competition) => {
  if (!competition?.id) return
  router.push(`/competition/${competition.id}`)
}

const handleMenuSelect = (index) => {
  activeMenu.value = index
  if (index === 'activities') {
    fetchActivities()
  } else if (index === 'competitions') {
    fetchCompetitions()
  }
}

const fetchUserInfo = async () => {
  try {
    const res = await userApi.getMyInfo()
    const data = res.code === 200 && res.data ? res.data : res
    const normalizedUserInfo = normalizeUserInfoMedia({
      ...data,
      name: data?.name || data?.username || ''
    })
    userInfo.value = normalizedUserInfo
    userStore.updateUserInfo(normalizedUserInfo)
    infoForm.name = data.name || data.username || ''
    infoForm.cellPhone = data.cellPhone || ''
    infoForm.email = data.email || ''
    infoForm.qq = data.qq || ''
    infoForm.gender = data.gender || 0
    infoForm.intro = data.intro || ''
  } catch (error) {
    console.error('获取用户信息失败:', error)
    ElMessage.error('获取用户信息失败')
  }
}

const handleUpdateInfo = async () => {
  if (!infoFormRef.value) return

  await infoFormRef.value.validate(async (valid) => {
    if (valid) {
      saving.value = true
      try {
        await userApi.updateUserInfo({
          name: infoForm.name,
          cellPhone: infoForm.cellPhone,
          email: infoForm.email,
          qq: infoForm.qq,
          gender: infoForm.gender,
          intro: infoForm.intro
        })
        const normalizedUserInfo = normalizeUserInfoMedia({ ...userInfo.value, ...infoForm })
        userInfo.value = normalizedUserInfo
        userStore.updateUserInfo(normalizedUserInfo)
        ElMessage.success('保存成功')
      } catch (error) {
        console.error('更新失败:', error)
        ElMessage.error('更新个人信息失败')
      } finally {
        saving.value = false
      }
    }
  })
}

const handleChangePassword = async () => {
  if (!passwordFormRef.value) return

  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      changingPassword.value = true
      try {
        await userApi.updateUserInfo({
          name: userInfo.value.name,
          oldPassword: passwordForm.oldPassword,
          password: passwordForm.password
        })
        ElMessage.success('密码修改成功，请重新登录')
        handleLogout()
      } catch (error) {
        console.error('修改密码失败:', error)
        ElMessage.error('修改密码失败，请检查旧密码是否正确')
      } finally {
        changingPassword.value = false
      }
    }
  })
}

const resetPasswordForm = () => {
  passwordForm.oldPassword = ''
  passwordForm.password = ''
  passwordForm.confirmPassword = ''
  passwordFormRef.value?.resetFields()
}

const beforeAvatarUpload = (file) => {
  const isImage = ['image/jpeg', 'image/png', 'image/gif'].includes(file.type)
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传 JPG/PNG/GIF 格式的图片')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB')
    return false
  }
  return true
}

const handleAvatarUpload = async (options) => {
  try {
    const { storedValue } = await uploadManagedFile({ file: options.file }, 'avatar')

    await userApi.updateUserInfo({
      username: userInfo.value?.username || '',
      name: infoForm.name || userInfo.value?.name || userInfo.value?.username || '',
      cellPhone: infoForm.cellPhone || userInfo.value?.cellPhone || '',
      email: infoForm.email || userInfo.value?.email || '',
      qq: infoForm.qq || userInfo.value?.qq || '',
      gender: infoForm.gender ?? userInfo.value?.gender ?? 0,
      intro: infoForm.intro || userInfo.value?.intro || '',
      icon: storedValue
    })

    const normalizedUserInfo = normalizeUserInfoMedia({
      ...userInfo.value,
      icon: storedValue
    })
    userInfo.value = normalizedUserInfo
    userStore.updateUserInfo(normalizedUserInfo)
    ElMessage.success('头像更新成功')
  } catch (error) {
    console.error('上传头像失败:', error)
    ElMessage.error('上传头像失败')
  }
}

const fetchActivities = async () => {
  activityLoading.value = true
  try {
    const res = await getMyActivities()
    if (res.code === 200 && res.data) {
      activities.value = res.data || []
    } else {
      activities.value = []
    }
    activityTotal.value = activities.value.length
  } catch (error) {
    console.error('获取活动记录失败:', error)
    activities.value = []
    activityTotal.value = 0
  } finally {
    activityLoading.value = false
  }
}

const fetchCompetitions = async () => {
  competitionLoading.value = true
  try {
    const res = await getMyCompetitions()
    if (res.code === 200 && res.data) {
      competitions.value = res.data || []
    } else {
      competitions.value = []
    }
    competitionTotal.value = competitions.value.length
  } catch (error) {
    console.error('获取比赛记录失败:', error)
    competitions.value = []
    competitionTotal.value = 0
  } finally {
    competitionLoading.value = false
  }
}

const fetchReferralCode = async () => {
  if (!currentTenantMembership.value) {
    referralCode.value = null
    referralLoading.value = false
    return
  }

  referralLoading.value = true
  try {
    const res = await getMyReferralCode()
    if (res.code === 200 && res.data) {
      referralCode.value = res.data
    } else {
      referralCode.value = null
      if (res.code !== 200) {
        ElMessage.warning(res.msg || '获取内推码失败')
      }
    }
  } catch (error) {
    console.error('获取内推码失败:', error)
    referralCode.value = null
  } finally {
    referralLoading.value = false
  }
}

const handleRefreshReferralCode = async () => {
  if (referralRefreshing.value) return

  referralRefreshing.value = true
  try {
    const res = await refreshMyReferralCode()
    if (res.code === 200 && res.data) {
      referralCode.value = res.data
      ElMessage.success('内推码已刷新')
    } else {
      referralCode.value = null
      ElMessage.warning(res.msg || '刷新内推码失败')
    }
  } catch (error) {
    console.error('刷新内推码失败:', error)
    ElMessage.error('刷新内推码失败')
  } finally {
    referralRefreshing.value = false
  }
}

watch(currentTenantIdKey, (newVal, oldVal) => {
  if (newVal && newVal !== oldVal) {
    fetchReferralCode()
  }
})

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => {
      userStore.logout()
      router.push('/login')
      ElMessage.success('已退出登录')
    })
    .catch(() => {})
}

onMounted(async () => {
  await Promise.all([
    fetchUserInfo(),
    userStore.fetchTenantDirectory(),
    userStore.fetchSwitchableTenants()
  ])
  await fetchReferralCode()
})
</script>

<style scoped>
.profile-page {
  padding: 28px 24px 40px;
}

.profile-band {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.profile-band__copy {
  display: grid;
  gap: 6px;
}

.profile-band__eyebrow {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #24548b;
}

.profile-band__copy strong {
  font-size: 16px;
  color: #102a43;
}

.profile-band__copy p {
  margin: 0;
  color: #6b7d8f;
  line-height: 1.6;
}

.profile-form {
  display: grid;
  gap: 18px;
}

.profile-overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  align-items: start;
}

.profile-overview-card {
  height: 100%;
}

.profile-nav-panel {
  align-self: stretch;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px 20px;
}

.profile-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.span-2 {
  grid-column: span 2;
}

.password-form {
  max-width: none;
}

.form-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.profile-summary {
  display: grid;
  gap: 16px;
}

.avatar-section {
  display: grid;
  justify-items: start;
  gap: 12px;
}

.user-name {
  margin: 0;
  font-size: 24px;
  color: #102a43;
}

.user-role {
  margin: -8px 0 0;
  color: #6b7d8f;
}

.tenant-empty {
  color: #6b7d8f;
  font-size: 13px;
}

.tenant-membership-list {
  display: grid;
  gap: 12px;
}

.tenant-membership-item {
  border: 1px solid #e6edf5;
  border-radius: 18px;
  padding: 14px 16px;
  background: #f8fbfe;
}

.tenant-membership-item.current {
  border-color: rgba(37, 99, 235, 0.28);
  background: linear-gradient(180deg, #eef6ff 0%, #ffffff 100%);
}

.tenant-membership-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.tenant-membership-name {
  font-size: 14px;
  font-weight: 600;
  color: #102a43;
}

.tenant-membership-sub {
  display: grid;
  gap: 4px;
  margin-top: 8px;
  color: #6b7d8f;
  font-size: 12px;
}

.tenant-switch-dialog-list {
  display: grid;
  gap: 10px;
}

.tenant-switch-dialog-item {
  width: 100%;
  min-height: 58px;
  margin-right: 0;
  padding: 12px 14px;
  border: 1px solid #e6edf5;
  border-radius: 12px;
}

.tenant-switch-dialog-item :deep(.el-radio__label) {
  width: 100%;
}

.tenant-switch-dialog-title {
  font-weight: 700;
  color: #102a43;
}

.tenant-switch-dialog-meta {
  margin-top: 4px;
  color: #6b7d8f;
  font-size: 12px;
}

.referral-card-body {
  min-height: 160px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
}

.referral-code-content {
  display: grid;
  gap: 14px;
  justify-items: center;
}

.referral-code-value {
  font-size: 34px;
  font-weight: 700;
  letter-spacing: 0.18em;
  color: #102a43;
}

.referral-code-meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px 18px;
  color: #6b7d8f;
  font-size: 13px;
}

.referral-empty {
  width: 100%;
}

.loading-container,
.empty-container {
  padding: 60px 0;
  text-align: center;
}

.record-list {
  display: grid;
  gap: 14px;
}

.record-item {
  display: grid;
  grid-template-columns: 132px minmax(0, 1fr) auto;
  gap: 18px;
  align-items: center;
  padding: 18px;
  border: 1px solid #e6edf5;
  border-radius: 20px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbfe 100%);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.record-item.clickable {
  cursor: pointer;
}

.record-item.clickable:hover {
  transform: translateY(-1px);
  border-color: rgba(37, 99, 235, 0.26);
  box-shadow: 0 16px 34px rgba(15, 53, 87, 0.08);
}

.record-cover {
  width: 132px;
  height: 88px;
  border-radius: 16px;
  overflow: hidden;
}

.record-cover :deep(.el-image) {
  width: 100%;
  height: 100%;
}

.record-main {
  min-width: 0;
}

.record-main h4 {
  margin: 0 0 10px;
  font-size: 17px;
  color: #102a43;
}

.record-meta {
  margin: 0 0 6px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #6b7d8f;
  line-height: 1.6;
}

.record-status {
  display: grid;
  justify-items: end;
  gap: 10px;
}

.registration-time {
  margin: 0;
  color: #7d8d9d;
  font-size: 12px;
}

.profile-page :deep(.el-pagination) {
  margin-top: 20px;
  justify-content: center;
}

@media (max-width: 960px) {
  .profile-page {
    padding: 20px 16px 32px;
  }

  .profile-overview-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .profile-nav-panel {
    align-self: auto;
  }

  .form-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .span-2 {
    grid-column: span 1;
  }

  .record-item {
    grid-template-columns: minmax(0, 1fr);
  }

  .record-cover {
    width: 100%;
    height: 180px;
  }

  .record-status {
    justify-items: start;
  }
}
</style>
