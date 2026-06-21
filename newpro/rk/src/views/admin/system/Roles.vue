<template>
  <div class="admin-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <div>
            <span>角色管理</span>
            <small class="card-subtitle">按租户维护默认角色和自定义角色权限</small>
          </div>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>新增角色
          </el-button>
        </div>
      </template>
      
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="角色名称">
          <el-input v-model="searchForm.roleName" placeholder="请输入角色名称" clearable />
        </el-form-item>
        <el-form-item label="角色标识">
          <el-input v-model="searchForm.roleKey" placeholder="请输入角色标识" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable>
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="canSelectTenant" label="租户">
          <el-select v-model="selectedTenantId" placeholder="请选择租户" filterable @change="fetchRoles">
            <el-option
              v-for="tenant in tenantOptions"
              :key="tenant.id"
              :label="tenant.tenantName || tenant.name || `租户 ${tenant.id}`"
              :value="tenant.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchRoles">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
      
      <!-- 表格 -->
      <div class="batch-delete-toolbar">
        <el-button type="danger" plain :disabled="!selectedRows.length" @click="handleBatchDelete">批量删除</el-button>
      </div>

      <el-table :data="roleList" v-loading="loading" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="tenantId" label="租户" width="90" />
        <el-table-column prop="roleName" label="角色名称" />
        <el-table-column prop="roleKey" label="角色标识" />
        <el-table-column prop="builtIn" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.builtIn ? 'warning' : 'success'">
              {{ row.builtIn ? '默认角色' : '自定义' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="roleSort" label="排序" width="80" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" :disabled="row.builtIn" @click="handleEdit(row)">编辑</el-button>
            <el-button text type="primary" @click="handleAssignMenu(row)">分配权限</el-button>
            <el-button text type="danger" :disabled="row.builtIn" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @change="fetchRoles"
      />
    </el-card>
    
    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色标识" prop="roleKey">
          <el-input v-model="form.roleKey" placeholder="请输入角色标识" />
        </el-form-item>
        <el-form-item label="排序" prop="roleSort">
          <el-input-number v-model="form.roleSort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 分配权限对话框 -->
    <el-dialog v-model="menuDialogVisible" title="分配权限" width="500px" class="admin-permission-dialog">
      <div class="role-menu-tree-panel">
        <el-tree
          ref="menuTreeRef"
          :data="menuTree"
          :props="{ label: 'label', children: 'subMenus' }"
          show-checkbox
          node-key="id"
          :default-checked-keys="checkedMenuIds"
          :default-expand-all="true"
        />
      </div>
      <template #footer>
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveMenu">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useBatchDelete } from '@/utils/batchDelete'
import { Plus } from '@element-plus/icons-vue'
import { getRoleList, createRole, updateRole, deleteRole, getMenuTree, getRoleMenuIds, assignRoleMenus } from '@/api/role'
import { getPublicTenantList } from '@/api/tenant'
import { useUserStore } from '@/stores/user'

const loading = ref(false)
const roleList = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增角色')
const formRef = ref()
const menuDialogVisible = ref(false)
const menuTreeRef = ref()
const menuTree = ref([])
const checkedMenuIds = ref([])
const currentRoleId = ref(null)
const userStore = useUserStore()
const tenantOptions = ref([])
const selectedTenantId = ref(Number(userStore.tenantId || 1))
const canSelectTenant = computed(() => userStore.isSuperAdmin())

const searchForm = reactive({
  roleName: '',
  roleKey: '',
  status: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const form = reactive({
  id: null,
  roleName: '',
  roleKey: '',
  roleSort: 0,
  status: 1,
  remark: ''
})

const rules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleKey: [{ required: true, message: '请输入角色标识', trigger: 'blur' }],
  roleSort: [{ required: true, message: '请输入排序', trigger: 'blur' }]
}

const fetchRoles = async () => {
  loading.value = true
  try {
    const res = await getRoleList(selectedTenantId.value)
    const rows = Array.isArray(res?.data) ? res.data : (Array.isArray(res) ? res : [])
    roleList.value = rows.map((role) => ({
      id: role.id,
      roleName: role.name,
      roleKey: role.code,
      tenantId: role.tenantId || role.depId || selectedTenantId.value,
      builtIn: Boolean(role.builtIn || role.type === 0),
      roleSort: role.sort || 0,
      status: role.status || 1,
      remark: role.remark || '',
      createTime: role.createTime || ''
    }))
    pagination.total = roleList.value.length
  } catch (e) {
    console.error('获取角色列表失败:', e)
    ElMessage.error('获取角色列表失败')
  } finally {
    loading.value = false
  }
}

const fetchTenants = async () => {
  if (!canSelectTenant.value) {
    tenantOptions.value = [{ id: selectedTenantId.value, tenantName: `租户 ${selectedTenantId.value}` }]
    return
  }
  try {
    const res = await getPublicTenantList()
    tenantOptions.value = Array.isArray(res?.data) ? res.data : (Array.isArray(res) ? res : [])
    if (!tenantOptions.value.some((tenant) => Number(tenant.id) === Number(selectedTenantId.value)) && tenantOptions.value.length) {
      selectedTenantId.value = Number(tenantOptions.value[0].id)
    }
  } catch (e) {
    tenantOptions.value = [{ id: selectedTenantId.value, tenantName: `租户 ${selectedTenantId.value}` }]
  }
}

const fetchMenuTree = async () => {
  try {
    const res = await getMenuTree()
    menuTree.value = Array.isArray(res?.data) ? res.data : (Array.isArray(res) ? res : [])
  } catch (e) {
    ElMessage.error('获取菜单树失败')
  }
}

const resetSearch = () => {
  searchForm.roleName = ''
  searchForm.roleKey = ''
  searchForm.status = ''
  fetchRoles()
}

const handleAdd = () => {
  dialogTitle.value = '新增角色'
  Object.assign(form, { id: null, roleName: '', roleKey: '', roleSort: 0, status: 1, remark: '' })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  if (row.builtIn) {
    ElMessage.warning('默认角色不能编辑角色标识')
    return
  }
  dialogTitle.value = '编辑角色'
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleAssignMenu = async (row) => {
  currentRoleId.value = row.id
  await fetchMenuTree()
  // 获取该角色已有的菜单权限
  try {
    const res = await getRoleMenuIds(row.id)
    checkedMenuIds.value = Array.isArray(res?.data) ? res.data : (Array.isArray(res) ? res : [])
  } catch (e) {
    checkedMenuIds.value = []
  }
  menuDialogVisible.value = true
}

const handleSaveMenu = async () => {
  const checkedIds = menuTreeRef.value.getCheckedKeys()
  try {
    await assignRoleMenus(currentRoleId.value, checkedIds)
    ElMessage.success('分配权限成功')
    menuDialogVisible.value = false
  } catch (e) {
    ElMessage.error('分配权限失败')
  }
}

const handleDelete = async (row) => {
  if (row.builtIn) {
    ElMessage.warning('默认角色不能删除')
    return
  }
  await ElMessageBox.confirm('确定要删除该角色吗？', '提示', { type: 'warning' })
  try {
    await deleteRole(row.id)
    ElMessage.success('删除成功')
    fetchRoles()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('删除角色失败:', e)
      ElMessage.error('删除角色失败')
    }
  }
}

const handleSubmit = async () => {
  await formRef.value.validate()
  try {
    // 数据格式映射：前端字段 → 后端字段
    const data = {
      name: form.roleName,        // 前端: roleName → 后端: name
      code: form.roleKey,         // 前端: roleKey → 后端: code
      tenantId: selectedTenantId.value,
      sort: form.roleSort,        // 前端: roleSort → 后端: sort
      remark: form.remark
      // 注意：不传status字段，后端会使用默认值
    }

    if (form.id) {
      // 修改角色
      await updateRole(form.id, data)
    } else {
      // 新增角色
      await createRole(data)
    }

    ElMessage.success(form.id ? '修改成功' : '新增成功')
    dialogVisible.value = false
    fetchRoles()
  } catch (e) {
    console.error('保存角色失败:', e)
    ElMessage.error('保存角色失败')
  }
}


const { selectedRows, handleSelectionChange, handleBatchDelete } = useBatchDelete({
  entityName: '角色',
  deleteItem: (id) => deleteRole(id),
  fetchList: fetchRoles
})

onMounted(() => {
  fetchTenants().finally(fetchRoles)
})
</script>

<style lang="scss" scoped>
.admin-page {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .card-subtitle {
    display: block;
    margin-top: 4px;
    color: var(--rk-text-muted);
    font-size: 12px;
  }
  
  .search-form {
    margin-bottom: 20px;
  }
  
  .el-pagination {
    margin-top: 20px;
    justify-content: flex-end;
  }

  .role-menu-tree-panel {
    max-height: min(420px, 62vh);
    overflow: auto;
    padding: 8px 4px;
    border: 1px solid var(--rk-border-color);
    border-radius: 8px;
    background: var(--rk-surface-bg);
  }
}
</style>
