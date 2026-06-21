<template>
  <div class="admin-page">
    <AdminModuleShell
      eyebrow="社团运营"
      title="成员管理"
      description="维护成员台账、观察待确认状态，并为管理员保留添加、编辑和删除能力。"
      :meta-items="moduleMeta"
      :stat-items="overviewStats"
    >
      <template #actions>
        <el-button :loading="loading" @click="fetchMembers">刷新成员库</el-button>
        <el-button v-if="canEdit" :loading="deletedLoading" @click="openDeletedDialog">查看已删除</el-button>
        <el-button v-if="canEdit" type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          添加成员
        </el-button>
      </template>

      <template #filters>
        <div class="filter-layout">
          <el-form :model="searchForm" label-position="top" class="filter-form" @submit.prevent>
            <el-form-item label="成员姓名">
              <el-input
                v-model="searchForm.name"
                placeholder="按姓名搜索"
                clearable
                @keyup.enter="applyFilters"
              />
            </el-form-item>
            <el-form-item label="成员状态">
              <el-select v-model="searchForm.status" placeholder="全部状态" clearable>
                <el-option v-for="option in statusOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
            </el-form-item>
            <el-form-item class="filter-actions">
              <el-button type="primary" @click="applyFilters">筛选</el-button>
              <el-button @click="resetSearch">重置</el-button>
            </el-form-item>
          </el-form>

          <div class="filter-side">
            <span class="filter-side-label">结构概览</span>
            <div class="quick-filters">
              <button
                v-for="option in quickStatusOptions"
                :key="option.value"
                type="button"
                class="quick-filter"
                :class="{ active: searchForm.status === option.value }"
                @click="applyQuickStatus(option.value)"
              >
                <strong>{{ option.count }}</strong>
                <span>{{ option.label }}</span>
              </button>
            </div>
          </div>
        </div>
      </template>

      <AdminPanelCard
        title="成员台账"
        description="新增和编辑统一使用表单弹窗，成员基础资料、组织信息和状态在同一个入口维护。"
      >
        <template #headerActions>
          <el-tag type="info">当前 {{ filteredMembers.length }} 人</el-tag>
        </template>

        <div class="table-shell">
          <div class="table-toolbar">
            <div class="toolbar-copy">
              <span class="toolbar-title">成员分布</span>
              <span class="toolbar-hint">
                已确认 {{ activeMemberCount }} 人，待确认 {{ pendingMemberCount }} 人，涉及 {{ departmentCount }} 个部门
              </span>
            </div>
            <el-tag :type="canEdit ? 'success' : 'info'" effect="dark">
              {{ canEdit ? '当前角色可编辑' : '当前角色只读' }}
            </el-tag>
          </div>

                <div class="batch-delete-toolbar">
        <el-button v-if="canEdit" type="danger" plain :disabled="!selectedRows.length" @click="handleBatchDelete">批量删除</el-button>
      </div>

      <el-table :data="pagedMembers" v-loading="loading" stripe class="admin-table" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="name" label="姓名" min-width="120" />
            <el-table-column prop="studentId" label="学号" min-width="140" />
            <el-table-column prop="email" label="邮箱" min-width="190" show-overflow-tooltip />
            <el-table-column prop="college" label="学院" min-width="160" show-overflow-tooltip />
            <el-table-column prop="department" label="部门" min-width="140" show-overflow-tooltip />
            <el-table-column prop="position" label="职位" min-width="140" show-overflow-tooltip />
            <el-table-column prop="major" label="专业" min-width="160" show-overflow-tooltip />
            <el-table-column prop="grade" label="年级" width="110" />
            <el-table-column prop="phone" label="联系电话" min-width="150" />
            <el-table-column prop="joinTime" label="入社时间" min-width="170">
              <template #default="{ row }">
                {{ formatDateTime(row.joinTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'warning'">
                  {{ row.status === 1 ? '已确认' : '待确认' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column v-if="canEdit" label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <div class="row-actions">
                  <el-button text type="primary" @click="handleEdit(row)">编辑</el-button>
                  <el-button text type="danger" @click="handleDelete(row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :total="filteredMembers.length"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @change="syncPageBounds"
          />
        </div>
      </AdminPanelCard>

      <template #aside>
        <AdminPanelCard
          title="成员上下文"
          description="只读角色可查看全量台账，但不会显示编辑入口。"
        >
          <div class="side-stat-list">
            <div class="side-stat-item">
              <span>成员总量</span>
              <strong>{{ allMembers.length }}</strong>
            </div>
            <div class="side-stat-item">
              <span>部门数量</span>
              <strong>{{ departmentCount }}</strong>
            </div>
            <div class="side-stat-item">
              <span>最近刷新</span>
              <strong>{{ lastUpdatedLabel }}</strong>
            </div>
          </div>
        </AdminPanelCard>

        <AdminPanelCard
          title="维护建议"
          description="用于减少成员资料漂移。"
        >
          <ul class="side-list">
            <li>新增成员后建议立即补全所属部门和岗位信息。</li>
            <li>删除会走现有后端删除接口，操作前确认对象无误。</li>
            <li>待确认成员可优先补齐基础资料，便于后续编组统计。</li>
          </ul>
        </AdminPanelCard>
      </template>
    </AdminModuleShell>

    <el-dialog
      v-model="deletedDialogVisible"
      title="已删除成员"
      width="920px"
      :close-on-click-modal="false"
    >
      <el-table :data="deletedMembers" v-loading="deletedLoading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="姓名" min-width="120" />
        <el-table-column prop="studentId" label="学号" min-width="140" />
        <el-table-column prop="department" label="部门" min-width="140" show-overflow-tooltip />
        <el-table-column prop="position" label="职位" min-width="140" show-overflow-tooltip />
        <el-table-column prop="updateTime" label="删除时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.updateTime || row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" :loading="restoringId === row.id" @click="handleRestoreMember(row)">恢复</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="deletedDialogVisible = false">关闭</el-button>
        <el-button :loading="deletedLoading" @click="fetchDeletedMembers">刷新</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="memberDialogVisible"
      :title="memberDialogTitle"
      width="720px"
      :close-on-click-modal="false"
      @close="resetMemberForm"
    >
      <el-form
        ref="memberFormRef"
        :model="memberForm"
        :rules="memberFormRules"
        label-width="92px"
        class="member-form"
      >
        <el-form-item label="姓名" prop="name">
          <el-input v-model="memberForm.name" maxlength="50" placeholder="请输入成员姓名" />
        </el-form-item>
        <el-form-item label="学号" prop="studentId">
          <el-input v-model="memberForm.studentId" maxlength="32" placeholder="请输入学号" :disabled="isEditingMember" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="memberForm.email" maxlength="100" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="memberForm.phone" maxlength="30" placeholder="请输入手机号或联系电话" />
        </el-form-item>
        <el-form-item label="学院" prop="college">
          <el-input v-model="memberForm.college" maxlength="80" placeholder="请输入学院" />
        </el-form-item>
        <el-form-item label="专业" prop="major">
          <el-input v-model="memberForm.major" maxlength="100" placeholder="请输入专业" />
        </el-form-item>
        <el-form-item label="年级" prop="grade">
          <el-input v-model="memberForm.grade" maxlength="20" placeholder="例如 2024" />
        </el-form-item>
        <el-form-item label="部门" prop="department">
          <el-input v-model="memberForm.department" maxlength="64" placeholder="请输入部门" />
        </el-form-item>
        <el-form-item label="职位" prop="position">
          <el-input v-model="memberForm.position" maxlength="64" placeholder="请输入职位" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="memberForm.status" placeholder="请选择状态">
            <el-option v-for="option in statusOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetMemberForm">取消</el-button>
        <el-button type="primary" :loading="memberSubmitLoading" @click="submitMemberForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useBatchDelete } from '@/utils/batchDelete'
import { Plus } from '@element-plus/icons-vue'
import AdminModuleShell from '@/components/admin-shell/AdminModuleShell.vue'
import AdminPanelCard from '@/components/admin-shell/AdminPanelCard.vue'
import { deleteApplication, getDeletedMembers, getMembersList, restoreMember, updateMember } from '@/api/member'
import { createUser, USER_STATUS, USER_TYPE } from '@/api/user'
import { getRoleList } from '@/api/role'
import { useUserStore } from '@/stores/user'
import { MEMBER_ROLE_IDS } from '@/utils/adminAccess'
import { normalizeMemberStatusValue } from '@/utils/memberStatus'

const userStore = useUserStore()
const canEdit = computed(() => !userStore.hasRole([8]))

const loading = ref(false)
const deletedLoading = ref(false)
const deletedDialogVisible = ref(false)
const deletedMembers = ref([])
const restoringId = ref(null)
const allMembers = ref([])
const lastUpdated = ref('')

const searchForm = reactive({
  name: '',
  status: ''
})

const pagination = reactive({
  page: 1,
  size: 10
})

const memberDialogVisible = ref(false)
const memberFormRef = ref(null)
const editingMember = ref(null)
const memberSubmitLoading = ref(false)
const memberRoleOptions = ref([])
const memberForm = reactive({
  name: '',
  studentId: '',
  email: '',
  phone: '',
  college: '',
  major: '',
  grade: '',
  department: '',
  position: '',
  status: 1
})

const statusOptions = [
  { label: '已确认', value: 1 },
  { label: '待确认', value: 0 }
]

const isEditingMember = computed(() => Boolean(editingMember.value?.id))
const memberDialogTitle = computed(() => (isEditingMember.value ? '编辑成员' : '新增成员'))

const memberFormRules = {
  name: [{ required: true, message: '请输入成员姓名', trigger: 'blur' }],
  studentId: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效邮箱', trigger: ['blur', 'change'] }
  ],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const memberRoleKeywords = ['普通成员', '成员', '学生', 'member', 'student']

const filteredMembers = computed(() => {
  const keyword = searchForm.name.trim()
  return allMembers.value.filter((member) => {
    const matchesName = !keyword || member.name?.includes(keyword)
    const matchesStatus = searchForm.status === '' || member.status === searchForm.status
    return matchesName && matchesStatus
  })
})

const pagedMembers = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  return filteredMembers.value.slice(start, start + pagination.size)
})

const activeMemberCount = computed(() => allMembers.value.filter((item) => item.status === 1).length)
const pendingMemberCount = computed(() => allMembers.value.filter((item) => item.status === 0).length)
const departmentCount = computed(() => {
  return new Set(allMembers.value.map((item) => item.department).filter(Boolean)).size
})

const overviewStats = computed(() => [
  { label: '成员总数', value: allMembers.value.length, helper: '当前成员库基数', tone: 'info' },
  { label: '已确认成员', value: activeMemberCount.value, helper: '可视作稳定编制', tone: 'success' },
  { label: '待确认成员', value: pendingMemberCount.value, helper: '建议优先完善资料', tone: 'warning' },
  { label: '部门覆盖', value: departmentCount.value, helper: '按部门维度统计', tone: 'info' }
])

const lastUpdatedLabel = computed(() => lastUpdated.value || '尚未刷新')
const moduleMeta = computed(() => [
  `最近刷新 ${lastUpdatedLabel.value}`,
  `${canEdit.value ? '当前角色支持维护' : '当前角色为只读查看'}`
])

const quickStatusOptions = computed(() => [
  { label: '全部成员', value: '', count: allMembers.value.length },
  { label: '已确认', value: 1, count: activeMemberCount.value },
  { label: '待确认', value: 0, count: pendingMemberCount.value }
])

watch(filteredMembers, () => {
  syncPageBounds()
})

function syncPageBounds() {
  const totalPages = Math.max(1, Math.ceil(filteredMembers.value.length / pagination.size))
  if (pagination.page > totalPages) {
    pagination.page = totalPages
  }
}

function formatDateTime(value) {
  return value ? new Date(value).toLocaleString('zh-CN') : '-'
}

async function fetchMembers() {
  loading.value = true
  try {
    const res = await getMembersList()
    if (res.code === 200 && Array.isArray(res.data)) {
      allMembers.value = res.data.map((item) => ({
        id: item.id,
        name: item.name || '',
        studentId: item.studentId || '',
        email: item.email || '',
        college: item.college || item.department || '',
        department: item.department || '',
        position: item.position || '',
        major: item.major || '',
        grade: item.grade || '',
        phone: item.phone || '',
        joinTime: item.joinDate || item.createTime || '',
        status: item.status === '正常' ? 1 : 0
      }))
      allMembers.value = allMembers.value.map((item, index) => ({
        ...item,
        status: normalizeMemberStatusValue(res.data[index]?.status)
      }))
      lastUpdated.value = new Date().toLocaleString('zh-CN')
      syncPageBounds()
    } else {
      allMembers.value = []
    }
  } catch (error) {
    console.error('获取成员列表失败:', error)
    allMembers.value = []
  } finally {
    loading.value = false
  }
}

async function fetchDeletedMembers() {
  deletedLoading.value = true
  try {
    const res = await getDeletedMembers()
    deletedMembers.value = res.code === 200 && Array.isArray(res.data) ? res.data : []
  } catch (error) {
    console.error('获取已删除成员失败:', error)
    deletedMembers.value = []
    ElMessage.error('获取已删除成员失败')
  } finally {
    deletedLoading.value = false
  }
}

async function openDeletedDialog() {
  deletedDialogVisible.value = true
  await fetchDeletedMembers()
}

async function handleRestoreMember(row) {
  if (!row?.id) return
  try {
    await ElMessageBox.confirm(`确认恢复成员 ${row.name || row.studentId || row.id} 吗？`, '提示', { type: 'warning' })
    restoringId.value = row.id
    const res = await restoreMember(row.id)
    if (res.code === 200) {
      ElMessage.success('恢复成功')
      await Promise.all([fetchMembers(), fetchDeletedMembers()])
      return
    }
    ElMessage.error(res.msg || '恢复失败')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '恢复失败')
    }
  } finally {
    restoringId.value = null
  }
}

function applyFilters() {
  pagination.page = 1
  syncPageBounds()
}

function applyQuickStatus(status) {
  searchForm.status = status
  pagination.page = 1
}

function resetSearch() {
  searchForm.name = ''
  searchForm.status = ''
  pagination.page = 1
}

function handleAdd() {
  editingMember.value = null
  fillMemberForm()
  memberDialogVisible.value = true
}

function handleEdit(row) {
  editingMember.value = row
  fillMemberForm(row)
  memberDialogVisible.value = true
}

function fillMemberForm(row = {}) {
  memberForm.name = row.name || ''
  memberForm.studentId = row.studentId || ''
  memberForm.email = row.email || ''
  memberForm.phone = row.phone || ''
  memberForm.college = row.college || ''
  memberForm.major = row.major || ''
  memberForm.grade = row.grade || ''
  memberForm.department = row.department || ''
  memberForm.position = row.position || ''
  memberForm.status = Number.isFinite(Number(row.status)) ? Number(row.status) : 1
}

function resetMemberForm() {
  memberDialogVisible.value = false
  editingMember.value = null
  fillMemberForm()
  memberFormRef.value?.clearValidate?.()
}

function buildMemberPayload() {
  return {
    name: memberForm.name.trim(),
    studentId: memberForm.studentId.trim(),
    email: memberForm.email.trim(),
    phone: memberForm.phone.trim(),
    college: memberForm.college.trim(),
    major: memberForm.major.trim(),
    grade: memberForm.grade.trim(),
    department: memberForm.department.trim(),
    position: memberForm.position.trim(),
    status: memberForm.status === 1 ? '正常' : '待确认'
  }
}

function getRoleId(role) {
  return Number(role?.id || role?.roleId || role?.role_id || 0)
}

function getRoleSearchText(role) {
  return [
    role?.name,
    role?.roleName,
    role?.code,
    role?.roleCode,
    role?.description
  ]
    .filter(Boolean)
    .join(' ')
    .toLowerCase()
}

async function loadMemberRoleOptions() {
  if (memberRoleOptions.value.length) {
    return memberRoleOptions.value
  }
  const tenantId = Number(userStore.tenantId || 0)
  const res = await getRoleList(tenantId || undefined)
  const rows = Array.isArray(res?.data) ? res.data : []
  memberRoleOptions.value = rows
  return rows
}

async function resolveDefaultMemberRoleId() {
  const roles = await loadMemberRoleOptions()
  const keywordRole = roles.find((role) => {
    const text = getRoleSearchText(role)
    return memberRoleKeywords.some((keyword) => text.includes(keyword.toLowerCase()))
  })
  if (keywordRole) {
    return getRoleId(keywordRole)
  }

  const knownMemberRole = roles.find((role) => MEMBER_ROLE_IDS.includes(getRoleId(role)))
  if (knownMemberRole) {
    return getRoleId(knownMemberRole)
  }

  throw new Error('当前租户没有可用的普通成员角色，请先在角色管理中配置')
}

async function submitMemberForm() {
  await memberFormRef.value?.validate?.()
  const payload = buildMemberPayload()
  memberSubmitLoading.value = true
  try {
    if (isEditingMember.value) {
      const res = await updateMember(editingMember.value.id, payload)
      if (res.code !== 200) {
        throw new Error(res.msg || '更新失败')
      }
      ElMessage.success('成员信息已更新')
    } else {
      await createUser({
        tenantId: Number(userStore.tenantId || 0) || undefined,
        username: payload.studentId,
        password: payload.studentId,
        name: payload.name,
        cellPhone: payload.phone,
        email: payload.email,
        studentId: payload.studentId,
        college: payload.college,
        major: payload.major,
        grade: payload.grade,
        department: payload.department,
        position: payload.position,
        type: USER_TYPE.STUDENT,
        roleId: await resolveDefaultMemberRoleId(),
        status: payload.status === '正常' ? USER_STATUS.NORMAL : 0
      })
      ElMessage.success('成员账号与台账已创建')
    }
    resetMemberForm()
    await fetchMembers()
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    memberSubmitLoading.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确认删除该成员吗？', '提示', { type: 'warning' })
    const res = await deleteApplication(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchMembers()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}


const { selectedRows, handleSelectionChange, handleBatchDelete } = useBatchDelete({
  entityName: '成员',
  deleteItem: (id) => deleteApplication(id),
  fetchList: fetchMembers
})

onMounted(() => {
  fetchMembers()
})
</script>

<style lang="scss" scoped>
.admin-page {
  display: grid;
  gap: 18px;
}

.filter-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.8fr) minmax(240px, 0.9fr);
  gap: 18px;
  align-items: start;
}

.filter-form {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0 14px;
}

.filter-actions {
  align-self: end;
}

.filter-side {
  display: grid;
  gap: 10px;
  padding: 14px;
  border-radius: 16px;
  background: #f6f9fc;
  border: 1px solid #e3ebf3;
}

.filter-side-label {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #587086;
}

.quick-filters {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.quick-filter {
  display: grid;
  gap: 4px;
  padding: 12px;
  border: 1px solid #dbe5ef;
  border-radius: 14px;
  background: #ffffff;
  text-align: left;
  color: #31485d;
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.quick-filter strong {
  font-size: 20px;
  color: #102a43;
}

.quick-filter.active {
  border-color: #3a7bd5;
  box-shadow: 0 10px 24px rgba(58, 123, 213, 0.16);
  transform: translateY(-1px);
}

.table-shell {
  display: grid;
  gap: 16px;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 16px;
  background: linear-gradient(135deg, #f8fbff 0%, #f2f6fb 100%);
  border: 1px solid #e2ebf5;
}

.toolbar-copy {
  display: grid;
  gap: 4px;
}

.toolbar-title {
  font-weight: 600;
  color: #102a43;
}

.toolbar-hint {
  color: #62788c;
  font-size: 13px;
}

.admin-table :deep(.el-table__header th) {
  background: #f7f9fc;
  color: #31485d;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.side-stat-list {
  display: grid;
  gap: 14px;
}

.side-stat-item {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #edf2f7;
  color: #51697d;
}

.side-stat-item:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.side-stat-item strong {
  color: #102a43;
  font-size: 18px;
}

.side-list {
  margin: 0;
  padding-left: 18px;
  color: #5d7286;
  line-height: 1.7;
}

@media (max-width: 960px) {
  .filter-layout,
  .filter-form,
  .quick-filters {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
