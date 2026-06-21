<template>
  <div class="admin-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <div class="header-actions">
            <el-button type="success" @click="downloadImportTemplate">
              <el-icon><Download /></el-icon>导出模板
            </el-button>
            <el-button type="warning" @click="importDialogVisible = true">
              <el-icon><Upload /></el-icon>Excel导入
            </el-button>
            <el-button type="info" :loading="reconcileLoading" @click="handleReconcilePeopleDomain">
              修复账号/成员差额
            </el-button>
            <el-button type="danger" :disabled="!selectedRows.length" @click="handleBatchDelete">
              批量删除
            </el-button>
            <el-button type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>新增用户
            </el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="searchForm.mobile" placeholder="请输入手机号" clearable />
        </el-form-item>
        <el-form-item label="租户">
          <el-select
            v-model="searchForm.tenantId"
            placeholder="请选择租户"
            clearable
            style="width: 180px"
            :disabled="!isSuperAdmin"
          >
            <el-option
              v-for="tenant in scopedTenantOptions"
              :key="tenant.id"
              :label="tenant.tenantName"
              :value="tenant.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 150px">
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-alert
        class="people-domain-alert"
        type="info"
        :closable="false"
        show-icon
        :title="scopedPeopleDomainAlertTitle"
      />

      <el-alert
        v-if="lastReconcileResult"
        class="people-domain-alert people-domain-reconcile-result"
        data-testid="people-domain-reconcile-result"
        type="success"
        show-icon
        :closable="true"
        :title="formatReconcileSummary(lastReconcileResult)"
        :description="formatReconcileDetails(lastReconcileResult)"
        @close="lastReconcileResult = null"
      />

      <div class="people-domain-definitions" data-testid="people-domain-definitions">
        <div class="people-diagnostics__title">口径说明</div>
        <div class="people-domain-grid people-domain-grid--detail people-domain-grid--definitions">
          <div
            v-for="definition in peopleDomainDefinitions"
            :key="definition.key"
            class="people-domain-stat people-domain-stat--definition"
            :data-definition-key="definition.key"
          >
            <span class="people-domain-definition__label">{{ definition.label }}</span>
            <small>{{ definition.helper }}</small>
          </div>
        </div>
      </div>

      <div class="people-domain-grid">
        <div class="people-domain-stat">
          <strong>{{ peopleSummary.accountCount }}</strong>
          <span>账号总数</span>
        </div>
        <div class="people-domain-stat">
          <strong>{{ peopleSummary.memberCount }}</strong>
          <span>成员台账</span>
        </div>
        <div class="people-domain-stat warning">
          <strong>{{ peopleSummary.nonMemberAccountCount }}</strong>
          <span>{{ peopleDomainDeltaLabel }}</span>
        </div>
      </div>

      <div class="people-domain-grid people-domain-grid--detail">
        <div
          v-for="card in peopleBreakdownCards"
          :key="card.key"
          class="people-domain-stat"
          :class="{ warning: card.key === 'standaloneStudents' }"
        >
          <strong>{{ card.value }}</strong>
          <span>{{ card.label }}</span>
        </div>
      </div>

      <div class="people-diagnostics">
        <div class="people-diagnostics__title">差额说明</div>
        <div class="people-domain-grid people-domain-grid--detail">
          <div
            v-for="card in peopleDiagnostics"
            :key="card.key"
            class="people-domain-stat people-domain-stat--diagnostic"
          >
            <strong>{{ card.value }}</strong>
            <span>{{ card.label }}</span>
            <small>{{ card.helper }}</small>
          </div>
        </div>
      </div>

      <div v-if="standaloneStudentSamples.length" class="people-domain-samples">
        <div class="people-diagnostics__title">未绑定成员台账的纯账号样本</div>
        <div class="people-domain-samples__body">
          <el-tag
            v-for="sample in standaloneStudentSamples"
            :key="sample"
            class="people-domain-samples__tag"
            type="warning"
            effect="plain"
          >
            {{ sample }}
          </el-tag>
        </div>
      </div>

      <el-table :data="displayUsers" v-loading="loading" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="本地ID" width="90" />
        <el-table-column prop="authUserId" label="认证ID" width="90" />
        <el-table-column prop="username" label="用户名" min-width="160" />
        <el-table-column prop="realName" label="真实姓名" min-width="120" />
        <el-table-column prop="mobile" label="手机号" min-width="140" />
        <el-table-column prop="email" label="邮箱" min-width="200" />
        <el-table-column prop="tenantName" label="租户" min-width="140" />
        <el-table-column prop="userType" label="用户类型" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.userType === 1 ? 'danger' : row.userType === 3 ? 'warning' : 'info'">
              {{ userTypeMap[row.userType] || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="roleName" label="角色" min-width="140">
          <template #default="{ row }">
            {{ row.roleName || '未分配' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="handleStatusChange(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button text type="warning" @click="handleResetPassword(row)">重置密码</el-button>
            <el-button text type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="租户" prop="tenantId">
          <el-select v-model="form.tenantId" placeholder="请选择租户" style="width: 100%" :disabled="!isSuperAdmin">
            <el-option
              v-for="tenant in scopedTenantOptions"
              :key="tenant.id"
              :label="tenant.tenantName"
              :value="tenant.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="登录用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" placeholder="请输入登录用户名" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="账号密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入账号密码" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="mobile">
          <el-input v-model="form.mobile" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="用户类型" prop="userType">
          <el-select v-model="form.userType" placeholder="请选择用户类型" style="width: 100%">
            <el-option label="管理员" :value="1" />
            <el-option label="学生" :value="2" />
            <el-option label="教师" :value="3" />
          </el-select>
        </el-form-item>
        <template v-if="form.userType === 2">
          <el-form-item label="学号" prop="studentId">
            <el-input v-model="form.studentId" placeholder="请输入学号" />
          </el-form-item>
          <el-form-item label="学院">
            <el-input v-model="form.college" placeholder="请输入学院" />
          </el-form-item>
          <el-form-item label="专业">
            <el-input v-model="form.major" placeholder="请输入专业" />
          </el-form-item>
          <el-form-item label="年级">
            <el-input v-model="form.grade" placeholder="请输入年级" />
          </el-form-item>
          <el-form-item label="部门">
            <el-input v-model="form.department" placeholder="请输入部门" />
          </el-form-item>
          <el-form-item label="岗位">
            <el-input v-model="form.position" placeholder="请输入岗位" />
          </el-form-item>
        </template>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="form.roleId" placeholder="请选择角色" style="width: 100%">
            <el-option
              v-for="role in roleOptions"
              :key="role.id"
              :label="role.name"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importDialogVisible" title="Excel导入用户" width="560px" :close-on-click-modal="false">
      <div class="import-hint">
        <p>支持 `.xlsx / .xls`。模板列：</p>
        <code>tenantId, username, password, name, cellPhone, email, type, roleId, status, studentId, college, major, grade, department, position</code>
      </div>
      <el-upload
        :auto-upload="false"
        :show-file-list="true"
        accept=".xlsx,.xls"
        :limit="1"
        :on-change="handleImportFileChange"
      >
        <el-button type="primary">选择Excel文件</el-button>
      </el-upload>
      <div v-if="importPreview.length" class="import-preview">
        <el-table :data="importPreview.slice(0, 5)" size="small">
          <el-table-column prop="tenantId" label="租户ID" width="90" />
          <el-table-column prop="username" label="用户名" min-width="140" />
          <el-table-column prop="name" label="姓名" min-width="120" />
          <el-table-column prop="studentId" label="学号" min-width="120" />
          <el-table-column prop="roleId" label="角色ID" width="90" />
        </el-table>
      </div>
      <template #footer>
        <el-button @click="closeImportDialog">取消</el-button>
        <el-button type="primary" :disabled="!importPreview.length" @click="handleImportSubmit">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as XLSX from 'xlsx'
import { Download, Plus, Upload } from '@element-plus/icons-vue'
import { getApplicationStatistics as getMemberStatistics } from '@/api/member'
import { createUser, deleteUser, getScopedUserStatistics, getUserPage, importUsers, reconcilePeopleDomain, resetPassword, updateUser, updateUserStatus } from '@/api/user'
import { getRoleList } from '@/api/role'
import { getPublicTenantList } from '@/api/tenant'
import { normalizeTenantDirectory } from '@/utils/tenantBranding'
import { useUserStore } from '@/stores/user'
import { exportToExcel } from '@/utils/export'
import { normalizeManagedUserImportRows, validateManagedUserImportRows } from '@/utils/adminUserImport'
import {
  buildPeopleDomainAlertTitle,
  buildPeopleDomainDeltaLabel,
  buildPeopleDomainDiagnostics,
  buildPeopleDomainSummary,
  hasPeopleDomainUserFilters,
  resolvePeopleDomainScopeLabel
} from '@/utils/adminPeopleDomain'

const userStore = useUserStore()
const loading = ref(false)
const dialogVisible = ref(false)
const importDialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const formRef = ref(null)

const rawUsers = ref([])
const selectedRows = ref([])
const roleOptions = ref([])
const tenantOptions = ref([])
const importPreview = ref([])
const memberStats = ref(null)
const userStats = ref(null)
const reconcileLoading = ref(false)
const lastReconcileResult = ref(null)

const userTypeMap = {
  1: '管理员',
  2: '学生',
  3: '教师'
}

const searchForm = reactive({
  username: '',
  mobile: '',
  tenantId: null,
  status: null
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const form = reactive({
  id: null,
  tenantId: null,
  username: '',
  password: '',
  realName: '',
  mobile: '',
  email: '',
  studentId: '',
  college: '',
  major: '',
  grade: '',
  department: '',
  position: '',
  userType: 2,
  roleId: null,
  status: 1
})

const rules = {
  tenantId: [{ required: true, message: '请选择租户', trigger: 'change' }],
  username: [{ required: true, message: '请输入登录用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入账号密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  userType: [{ required: true, message: '请选择用户类型', trigger: 'change' }],
  studentId: [{
    validator: (_rule, value, callback) => {
      if (form.userType === 2 && !String(value || '').trim()) {
        callback(new Error('学生类型必须填写学号'))
        return
      }
      callback()
    },
    trigger: 'blur'
  }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const isSuperAdmin = computed(() => userStore.isSuperAdmin())
const currentTenantId = computed(() => Number(userStore.tenantId || 0) || null)

const scopedTenantOptions = computed(() => {
  if (isSuperAdmin.value) {
    return tenantOptions.value
  }
  return tenantOptions.value.filter((tenant) => String(tenant.id) === String(currentTenantId.value || ''))
})

const tenantNameMap = computed(() => {
  const map = new Map()
  for (const tenant of tenantOptions.value) {
    map.set(tenant.id, tenant.tenantName)
  }
  return map
})

const displayUsers = computed(() => {
  return rawUsers.value.map((item) => ({
    ...item,
    realName: item.name || '',
    mobile: item.cellPhone || '',
    userType: item.type,
    tenantName: tenantNameMap.value.get(item.tenantId) || `租户${item.tenantId || ''}`
  }))
})

const peopleSummary = computed(() => buildPeopleDomainSummary({
  userPage: { total: pagination.total },
  userStats: userStats.value,
  memberStats: memberStats.value
}))

const peopleBreakdownCards = computed(() => ([
  { key: 'accounts', value: peopleSummary.value.accountCount, label: '账号总数' },
  { key: 'members', value: peopleSummary.value.memberCount, label: '正式成员' },
  { key: 'admins', value: peopleSummary.value.adminAccountCount, label: '管理员账号' },
  { key: 'teachers', value: peopleSummary.value.teacherAccountCount, label: '指导老师账号' },
  { key: 'boundStudents', value: peopleSummary.value.studentAccountWithStudentIdCount, label: '已绑定学号账号' },
  { key: 'studentWithoutId', value: peopleSummary.value.studentAccountWithoutStudentIdCount, label: '无学号的学生账号' },
  { key: 'studentMissingMemberLedger', value: peopleSummary.value.studentAccountMissingMemberLedgerCount, label: '未进成员台账的学生账号' }
]))

const peopleDiagnostics = computed(() => buildPeopleDomainDiagnostics(peopleSummary.value, {
  isSuperAdmin: isSuperAdmin.value
}))

const standaloneStudentSamples = computed(() => peopleSummary.value.standaloneStudentAccountSamples || [])

const hasUserFilters = computed(() => hasPeopleDomainUserFilters(searchForm))
const peopleDomainDeltaLabel = computed(() => buildPeopleDomainDeltaLabel(hasUserFilters.value))
const peopleDomainScopeLabel = computed(() => resolvePeopleDomainScopeLabel(isSuperAdmin.value))
const peopleDomainDefinitions = computed(() => ([
  {
    key: 'accounts-ledger',
    label: '账号台账口径',
    helper: `${peopleDomainScopeLabel.value} rk_user 账号表，包含学生、管理员、指导老师等全部可见账号。`
  },
  {
    key: 'members-ledger',
    label: '成员台账口径',
    helper: `${peopleDomainScopeLabel.value} club_members 仅统计正式学生成员；管理员、指导老师及无学号账号不会计入成员台账。`
  },
  {
    key: 'alumni-ledger',
    label: '校友台账口径',
    helper: `${peopleDomainScopeLabel.value} club_alumni 仅统计“已毕业且展示中”的校友档案。`
  },
  {
    key: 'scope-rule',
    label: '租户范围规则',
    helper: isSuperAdmin.value
      ? '租户1（超级管理员）按平台可见范围聚合，展示跨租户汇总数据。'
      : '普通租户管理员仅展示当前租户范围，不包含其他租户数据。'
  }
]))

const peopleDomainAlertTitle = computed(() => {
  if (!peopleSummary.value.nonMemberAccountCount) {
    return '当前筛选范围内，账号台账与成员台账已对齐。'
  }
  return `当前筛选范围内共有 ${peopleSummary.value.accountCount} 个账号，其中 ${peopleSummary.value.memberCount} 个已进入成员台账，另有 ${peopleSummary.value.nonMemberAccountCount} 个无学号账号仍按账号口径展示。`
})

const scopedPeopleDomainAlertTitle = computed(() => {
  if (!hasUserFilters.value) {
    return peopleDomainAlertTitle.value
  }
  return buildPeopleDomainAlertTitle(isSuperAdmin.value, { hasUserFilters: true })
})

function formatReconcileSummary(result) {
  if (!result) {
    return ''
  }
  return result.summary || [
    `扫描账号 ${result.scannedAccountCount || 0}`,
    `扫描成员 ${result.scannedMemberCount || 0}`,
    `新增成员台账 ${result.createdMemberCount || 0}`,
    `新增用户账号 ${result.createdUserCount || 0}`,
    `新增认证账号 ${result.createdAuthAccountCount || 0}`,
    `跳过无学号账号 ${result.skippedAccountWithoutStudentIdCount || 0}`
  ].join('，')
}

function formatReconcileDetails(result) {
  const details = Array.isArray(result?.details) ? result.details.filter(Boolean) : []
  if (details.length) {
    return details.slice(0, 8).join('；')
  }
  return `作用范围：${result?.scopeLabel || '当前权限范围'}，耗时 ${result?.costTimeMs || 0}ms`
}

async function fetchScopedStatistics() {
  const [memberStatsRes, userStatsRes] = await Promise.allSettled([
    getMemberStatistics(),
    getScopedUserStatistics(searchForm.tenantId)
  ])
  const memberRes = memberStatsRes.status === 'fulfilled' ? memberStatsRes.value : null
  const statsRes = userStatsRes.status === 'fulfilled' ? userStatsRes.value : null
  memberStats.value = memberRes?.code === 200 ? memberRes.data : null
  userStats.value = statsRes?.code === 200 ? statsRes.data : null
}

async function fetchRoles(tenantId = form.tenantId || currentTenantId.value) {
  if (!tenantId) {
    roleOptions.value = []
    return
  }
  const res = await getRoleList(tenantId)
  roleOptions.value = res.data || []
  if (form.roleId && !roleOptions.value.some((role) => Number(role.id) === Number(form.roleId))) {
    form.roleId = null
  }
}

async function fetchTenants() {
  const res = await getPublicTenantList()
  tenantOptions.value = normalizeTenantDirectory(res.data || [])
  if (!isSuperAdmin.value && currentTenantId.value) {
    searchForm.tenantId = currentTenantId.value
  }
}

async function fetchUsers() {
  loading.value = true
  try {
    const [res, memberStatsRes, userStatsRes] = await Promise.allSettled([
      getUserPage({
        page: pagination.page,
        size: pagination.size,
        username: searchForm.username,
        mobile: searchForm.mobile,
        status: searchForm.status,
        tenantId: searchForm.tenantId
      }),
      getMemberStatistics(),
      getScopedUserStatistics(searchForm.tenantId)
    ])
    const pageRes = res.status === 'fulfilled' ? res.value : null
    const memberRes = memberStatsRes.status === 'fulfilled' ? memberStatsRes.value : null
    const statsRes = userStatsRes.status === 'fulfilled' ? userStatsRes.value : null
    if (pageRes?.code === 200 && pageRes.data) {
      rawUsers.value = pageRes.data.records || []
      pagination.total = pageRes.data.total || 0
      memberStats.value = memberRes?.code === 200 ? memberRes.data : null
      userStats.value = statsRes?.code === 200 ? statsRes.data : null
      return
    }
    throw new Error(pageRes?.msg || '获取用户列表失败')
  } catch (error) {
    ElMessage.error(error.message || '获取用户列表失败')
    rawUsers.value = []
    pagination.total = 0
    memberStats.value = null
    userStats.value = null
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  searchForm.username = ''
  searchForm.mobile = ''
  searchForm.tenantId = isSuperAdmin.value ? null : currentTenantId.value
  searchForm.status = null
  pagination.page = 1
  fetchUsers()
}

function handleSearch() {
  pagination.page = 1
  fetchUsers()
}

function handlePageChange(page) {
  pagination.page = page
  fetchUsers()
}

function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  fetchUsers()
}

function handleSelectionChange(selection) {
  selectedRows.value = selection
}

function resetForm() {
  Object.assign(form, {
    id: null,
    tenantId: isSuperAdmin.value ? null : currentTenantId.value,
    username: '',
    password: '',
    realName: '',
    mobile: '',
    email: '',
    studentId: '',
    college: '',
    major: '',
    grade: '',
    department: '',
    position: '',
    userType: 2,
    roleId: null,
    status: 1
  })
}

async function handleAdd() {
  resetForm()
  dialogTitle.value = '新增用户'
  await fetchRoles(form.tenantId)
  dialogVisible.value = true
}

async function handleEdit(row) {
  resetForm()
  dialogTitle.value = '编辑用户'
  Object.assign(form, {
    id: row.id,
    tenantId: row.tenantId,
    username: row.username,
    realName: row.realName,
    mobile: row.mobile,
    email: row.email,
    studentId: row.studentId || '',
    college: row.college || '',
    major: row.major || '',
    grade: row.grade || '',
    department: row.department || '',
    position: row.position || '',
    userType: row.userType,
    roleId: row.roleId,
    status: row.status
  })
  await fetchRoles(form.tenantId)
  dialogVisible.value = true
}

async function handleStatusChange(row) {
  try {
    await updateUserStatus(row.id, row.status)
    ElMessage.success(`用户状态已${row.status === 1 ? '启用' : '禁用'}`)
  } catch (error) {
    row.status = row.status === 1 ? 0 : 1
    ElMessage.error(error.message || '状态更新失败')
  }
}

async function handleResetPassword(row) {
  await ElMessageBox.confirm(`确定将 ${row.username} 的密码重置为默认值 123456 吗？`, '提示', {
    type: 'warning'
  })
  try {
    await resetPassword(row.id)
    ElMessage.success('密码已重置为 123456')
  } catch (error) {
    ElMessage.error(error.message || '重置密码失败')
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除用户 ${row.username} 吗？`, '警告', { type: 'warning' })
  try {
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    await fetchUsers()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

async function handleBatchDelete() {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择要删除的用户')
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedRows.value.length} 个用户吗？`, '批量删除用户', { type: 'warning' })
    for (const row of selectedRows.value) {
      await deleteUser(row.id)
    }
    ElMessage.success('批量删除成功')
    selectedRows.value = []
    await fetchUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '批量删除失败')
    }
  }
}

async function handleReconcilePeopleDomain() {
  try {
    await ElMessageBox.confirm('将按当前登录权限修复账号、成员和校友台账差额，执行前请确认已备份关键数据。是否继续？', '修复账号/成员差额', {
      type: 'warning'
    })
    reconcileLoading.value = true
    const res = await reconcilePeopleDomain()
    if (res.code !== 200) {
      throw new Error(res.msg || '差额修复失败')
    }
    lastReconcileResult.value = res.data || null
    ElMessage.success(formatReconcileSummary(lastReconcileResult.value) || '差额修复完成')
    await fetchUsers()
    await fetchScopedStatistics()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '差额修复失败')
    }
  } finally {
    reconcileLoading.value = false
  }
}

async function handleSubmit() {
  await formRef.value.validate()
  const payload = {
    tenantId: form.tenantId || currentTenantId.value,
    username: form.username,
    password: form.password,
    name: form.realName,
    cellPhone: form.mobile,
    email: form.email,
    studentId: form.userType === 2 ? form.studentId : '',
    college: form.userType === 2 ? form.college : '',
    major: form.userType === 2 ? form.major : '',
    grade: form.userType === 2 ? form.grade : '',
    department: form.userType === 2 ? form.department : '',
    position: form.userType === 2 ? form.position : '',
    type: form.userType,
    roleId: form.roleId,
    status: form.status
  }

  try {
    if (form.id) {
      await updateUser(form.id, payload)
      ElMessage.success('修改成功')
    } else {
      await createUser(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await fetchUsers()
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  }
}

function handleImportFileChange(file) {
  const raw = file.raw
  if (!raw) return
  const reader = new FileReader()
  reader.onload = (event) => {
    const workbook = XLSX.read(event.target.result, { type: 'array' })
    const sheetName = workbook.SheetNames[0]
    const sheet = workbook.Sheets[sheetName]
    const rows = XLSX.utils.sheet_to_json(sheet, { defval: '' })
    const normalizedRows = normalizeManagedUserImportRows(rows)
    const validation = validateManagedUserImportRows(normalizedRows)
    if (!validation.ok) {
      importPreview.value = []
      ElMessage.error(validation.message)
      return
    }
    importPreview.value = rows.map((row) => ({
      tenantId: Number(row.tenantId || row['租户ID'] || row['tenant_id'] || 0),
      username: String(row.username || row['用户名'] || '').trim(),
      password: String(row.password || row['密码'] || '123456').trim(),
      name: String(row.name || row['姓名'] || row['真实姓名'] || '').trim(),
      cellPhone: String(row.cellPhone || row['手机号'] || row.mobile || '').trim(),
      email: String(row.email || row['邮箱'] || '').trim(),
      studentId: String(row.studentId || row['学号'] || '').trim(),
      college: String(row.college || row['学院'] || '').trim(),
      major: String(row.major || row['专业'] || '').trim(),
      grade: String(row.grade || row['年级'] || '').trim(),
      department: String(row.department || row['部门'] || '').trim(),
      position: String(row.position || row['岗位'] || '').trim(),
      type: Number(row.type || row['用户类型'] || 2),
      roleId: Number(row.roleId || row['角色ID'] || 0),
      status: Number(row.status || row['状态'] || 1)
    })).filter((row) => row.tenantId && row.username && row.roleId)
    importPreview.value = normalizedRows
  }
  reader.readAsArrayBuffer(raw)
}

async function handleImportSubmit() {
  try {
    const validation = validateManagedUserImportRows(importPreview.value)
    if (!validation.ok) {
      ElMessage.error(validation.message)
      return
    }
    const res = await importUsers(importPreview.value)
    if (res.code === 200) {
      ElMessage.success(`成功导入 ${res.data} 个用户`)
      closeImportDialog()
      await fetchUsers()
      return
    }
    throw new Error(res.msg || '导入失败')
  } catch (error) {
    ElMessage.error(error.message || '导入失败')
  }
}

function closeImportDialog() {
  importDialogVisible.value = false
  importPreview.value = []
}

function downloadImportTemplate() {
  exportToExcel(
    [
      {
        tenantId: 1,
        username: 'import_user_1',
        password: 'Passw0rd!',
        name: '导入用户1',
        cellPhone: '13800138011',
        email: 'import1@example.com',
        studentId: '2026001001',
        college: '计算机学院',
        major: '软件工程',
        grade: '2026',
        department: '宣传部',
        position: '成员',
        type: 2,
        roleId: 2,
        status: 1
      }
    ],
    [
      { prop: 'tenantId', label: 'tenantId' },
      { prop: 'username', label: 'username' },
      { prop: 'password', label: 'password' },
      { prop: 'name', label: 'name' },
      { prop: 'cellPhone', label: 'cellPhone' },
      { prop: 'email', label: 'email' },
      { prop: 'studentId', label: 'studentId' },
      { prop: 'college', label: 'college' },
      { prop: 'major', label: 'major' },
      { prop: 'grade', label: 'grade' },
      { prop: 'department', label: 'department' },
      { prop: 'position', label: 'position' },
      { prop: 'type', label: 'type' },
      { prop: 'roleId', label: 'roleId' },
      { prop: 'status', label: 'status' }
    ],
    '用户导入模板'
  )
}

watch(
  () => form.tenantId,
  async (tenantId, previousTenantId) => {
    if (!dialogVisible.value || !tenantId || tenantId === previousTenantId) {
      return
    }
    await fetchRoles(tenantId)
  }
)

onMounted(async () => {
  await Promise.all([fetchTenants(), fetchUsers()])
  await fetchRoles(currentTenantId.value)
})
</script>

<style scoped>
.admin-page .card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.search-form {
  margin-bottom: 20px;
}

.people-domain-alert {
  margin-bottom: 16px;
}

.people-domain-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.people-domain-stat {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 14px;
  background: #f5f9ff;
  border: 1px solid #dbe7f3;
}

.people-domain-stat strong {
  font-size: 24px;
  color: #16324f;
}

.people-domain-stat span {
  color: #5f7183;
  font-size: 13px;
}

.people-domain-stat small {
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.people-domain-stat.warning {
  background: #fff8eb;
  border-color: #f5d08a;
}

.people-diagnostics {
  margin-bottom: 16px;
}

.people-domain-definitions {
  margin-bottom: 16px;
}

.people-domain-grid--definitions {
  margin-bottom: 0;
}

.people-domain-stat--definition {
  align-content: start;
  background: #f8fbff;
}

.people-domain-definition__label {
  color: #16324f;
  font-weight: 600;
}

.people-diagnostics__title {
  margin-bottom: 10px;
  color: #16324f;
  font-size: 14px;
  font-weight: 600;
}

.people-domain-samples {
  margin-bottom: 16px;
  padding: 14px 16px;
  border: 1px solid #f5d08a;
  border-radius: 14px;
  background: #fffaf0;
}

.people-domain-samples__body {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.people-domain-samples__tag {
  margin-right: 0;
}

.people-domain-stat--diagnostic {
  align-content: start;
}

.import-hint {
  margin-bottom: 16px;
  color: #606266;
}

.import-preview {
  margin-top: 16px;
}

.el-pagination {
  margin-top: 20px;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .admin-page .card-header {
    flex-direction: column;
    align-items: stretch;
  }

  .header-actions {
    width: 100%;
  }

  .header-actions :deep(.el-button) {
    flex: 1 1 140px;
  }

  .search-form {
    display: grid;
    grid-template-columns: minmax(0, 1fr);
    gap: 10px;
  }

  .people-domain-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .search-form :deep(.el-form-item) {
    margin-right: 0;
    margin-bottom: 0;
  }

  .search-form :deep(.el-form-item__content) {
    width: 100%;
  }

  .admin-page :deep(.el-table__body-wrapper) {
    overflow-x: auto;
  }

  .el-pagination {
    justify-content: flex-start;
  }
}
</style>
