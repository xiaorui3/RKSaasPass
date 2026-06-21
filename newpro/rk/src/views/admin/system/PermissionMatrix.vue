<template>
  <div class="permission-matrix-page">
    <section class="page-header">
      <div>
        <h1>权限矩阵</h1>
        <p>按租户查看角色和菜单授权关系，排查菜单管理、角色授权和后台侧边栏展示是否一致。</p>
      </div>
      <div class="header-actions">
        <el-select
          v-if="canSelectTenant"
          v-model="tenantId"
          filterable
          placeholder="请选择租户"
          class="tenant-select"
          @change="loadPermissionMatrix"
        >
          <el-option
            v-for="tenant in tenantOptions"
            :key="tenant.id"
            :label="tenant.tenantName || tenant.name || `租户 ${tenant.id}`"
            :value="Number(tenant.id)"
          />
        </el-select>
        <el-button :loading="loading" @click="loadPermissionMatrix">刷新</el-button>
        <el-button type="primary" :disabled="!matrixRows.length" @click="exportMatrix">导出矩阵</el-button>
      </div>
    </section>

    <section class="summary-grid">
      <el-card>
        <span>角色数量</span>
        <strong>{{ summary.roleCount || 0 }}</strong>
        <small>当前租户可分配角色</small>
      </el-card>
      <el-card>
        <span>菜单数量</span>
        <strong>{{ summary.menuCount || 0 }}</strong>
        <small>启用且可见菜单</small>
      </el-card>
      <el-card>
        <span>角色覆盖率</span>
        <strong>{{ summary.roleCoverageRate || '0.00%' }}</strong>
        <small>已覆盖全部菜单的角色占比</small>
      </el-card>
      <el-card>
        <span>菜单覆盖率</span>
        <strong>{{ summary.menuCoverageRate || '0.00%' }}</strong>
        <small>至少被一个角色授权的菜单占比</small>
      </el-card>
      <el-card>
        <span>未授权菜单</span>
        <strong>{{ summary.uncoveredMenuCount || 0 }}</strong>
        <small>没有任何角色拥有的菜单</small>
      </el-card>
    </section>

    <el-card class="matrix-card">
      <template #header>
        <div class="matrix-toolbar">
          <div class="filter-group">
            <el-input v-model="menuKeyword" clearable placeholder="按菜单搜索" />
            <el-select v-model="selectedRoleId" clearable placeholder="按角色筛选">
              <el-option
                v-for="role in roleColumns"
                :key="role.id"
                :label="role.name || role.code || `角色 ${role.id}`"
                :value="role.id"
              />
            </el-select>
          </div>
          <el-tag type="info">租户 {{ permissionMatrix.tenantId || tenantId || '-' }}</el-tag>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="matrixRows"
        border
        stripe
        class="matrix-table"
        max-height="640"
      >
        <el-table-column fixed prop="fullPath" label="菜单" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="menu-cell" :style="{ paddingLeft: `${row.depth * 16}px` }">
              <strong>{{ row.label }}</strong>
              <small>{{ row.path || row.menuCode || '-' }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="coverageRate" label="菜单覆盖率" width="110" />
        <el-table-column
          v-for="role in visibleRoleColumns"
          :key="role.id"
          :label="role.name || role.code || `角色 ${role.id}`"
          min-width="140"
          align="center"
        >
          <template #header>
            <div class="role-header">
              <strong>{{ role.name || role.code || `角色 ${role.id}` }}</strong>
              <small>{{ role.coverageRate || '0.00%' }}</small>
            </div>
          </template>
          <template #default="{ row }">
            <el-tag
              :type="isAssigned(role.id, row.id) ? 'success' : 'info'"
              effect="plain"
            >
              {{ isAssigned(role.id, row.id) ? '已授权' : '未授权' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getPermissionMatrix } from '@/api/role'
import { getPublicTenantList } from '@/api/tenant'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const tenantOptions = ref([])
const tenantId = ref(Number(userStore.tenantId || 1))
const menuKeyword = ref('')
const selectedRoleId = ref(null)

const permissionMatrix = reactive({
  tenantId: null,
  roles: [],
  menus: [],
  cells: [],
  summary: {}
})

const canSelectTenant = computed(() => userStore.isSuperAdmin())
const roleColumns = computed(() => permissionMatrix.roles || [])
const summary = computed(() => permissionMatrix.summary || {})
const assignedMenuIds = computed(() => {
  const map = new Set()
  for (const cell of permissionMatrix.cells || []) {
    if (cell.assigned) {
      map.add(`${cell.roleId}:${cell.menuId}`)
    }
  }
  return map
})

const visibleRoleColumns = computed(() => {
  if (!selectedRoleId.value) {
    return roleColumns.value
  }
  return roleColumns.value.filter((role) => Number(role.id) === Number(selectedRoleId.value))
})

const matrixRows = computed(() => {
  const keyword = menuKeyword.value.trim().toLowerCase()
  return (permissionMatrix.menus || []).filter((menu) => {
    if (!keyword) {
      return true
    }
    return [
      menu.label,
      menu.fullPath,
      menu.path,
      menu.menuCode
    ].some((value) => String(value || '').toLowerCase().includes(keyword))
  })
})

function assignPermissionMatrix(data = {}) {
  permissionMatrix.tenantId = data.tenantId ?? tenantId.value
  permissionMatrix.roles = Array.isArray(data.roles) ? data.roles : []
  permissionMatrix.menus = Array.isArray(data.menus) ? data.menus : []
  permissionMatrix.cells = Array.isArray(data.cells) ? data.cells : []
  permissionMatrix.summary = data.summary || {}
}

function unwrapResponse(res) {
  if (res?.code && res.code !== 200) {
    throw new Error(res.msg || res.message || '权限矩阵加载失败')
  }
  return res?.data || res || {}
}

async function loadTenants() {
  if (!canSelectTenant.value) {
    tenantOptions.value = [{ id: tenantId.value, tenantName: `租户 ${tenantId.value}` }]
    return
  }
  try {
    const res = await getPublicTenantList()
    tenantOptions.value = Array.isArray(res?.data) ? res.data : (Array.isArray(res) ? res : [])
    if (!tenantOptions.value.some((tenant) => Number(tenant.id) === Number(tenantId.value)) && tenantOptions.value.length) {
      tenantId.value = Number(tenantOptions.value[0].id)
    }
  } catch (error) {
    tenantOptions.value = [{ id: tenantId.value, tenantName: `租户 ${tenantId.value}` }]
  }
}

async function loadPermissionMatrix() {
  loading.value = true
  try {
    const res = await getPermissionMatrix(tenantId.value)
    assignPermissionMatrix(unwrapResponse(res))
  } catch (error) {
    ElMessage.error(error.message || '权限矩阵加载失败')
  } finally {
    loading.value = false
  }
}

function isAssigned(roleId, menuId) {
  return assignedMenuIds.value.has(`${roleId}:${menuId}`)
}

function csvEscape(value) {
  const text = String(value ?? '')
  if (/[",\n\r]/.test(text)) {
    return `"${text.replace(/"/g, '""')}"`
  }
  return text
}

function exportMatrix() {
  const headers = ['菜单', '路径', '菜单覆盖率', ...visibleRoleColumns.value.map((role) => role.name || role.code || `角色 ${role.id}`)]
  const rows = matrixRows.value.map((menu) => [
    menu.fullPath || menu.label,
    menu.path || '',
    menu.coverageRate || '0.00%',
    ...visibleRoleColumns.value.map((role) => (isAssigned(role.id, menu.id) ? '已授权' : '未授权'))
  ])
  const content = [headers, ...rows].map((row) => row.map(csvEscape).join(',')).join('\n')
  const blob = new Blob([`\uFEFF${content}`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `permission-matrix-tenant-${permissionMatrix.tenantId || tenantId.value}.csv`
  link.click()
  URL.revokeObjectURL(url)
}

onMounted(async () => {
  await loadTenants()
  await loadPermissionMatrix()
})
</script>

<style scoped>
.permission-matrix-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.page-header h1 {
  margin: 0;
  color: #14213d;
  font-size: 24px;
}

.page-header p {
  margin: 8px 0 0;
  color: #64748b;
}

.header-actions,
.matrix-toolbar,
.filter-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.tenant-select {
  width: 220px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.summary-grid :deep(.el-card__body) {
  display: grid;
  gap: 6px;
}

.summary-grid span {
  color: #64748b;
  font-size: 13px;
}

.summary-grid strong {
  color: #14213d;
  font-size: 26px;
}

.summary-grid small {
  color: #94a3b8;
}

.matrix-card {
  min-width: 0;
}

.matrix-toolbar {
  justify-content: space-between;
}

.filter-group {
  flex-wrap: wrap;
}

.filter-group .el-input,
.filter-group .el-select {
  width: 220px;
}

.matrix-table {
  width: 100%;
}

.menu-cell,
.role-header {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.menu-cell strong,
.role-header strong {
  overflow: hidden;
  color: #14213d;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.menu-cell small,
.role-header small {
  overflow: hidden;
  color: #94a3b8;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 960px) {
  .page-header,
  .matrix-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .header-actions,
  .filter-group {
    align-items: stretch;
    flex-direction: column;
  }

  .tenant-select,
  .filter-group .el-input,
  .filter-group .el-select {
    width: 100%;
  }
}
</style>
