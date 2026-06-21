<template>
  <div class="admin-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>菜单管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>新增菜单
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="菜单名称">
          <el-input v-model="searchForm.menuName" placeholder="请输入菜单名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="applyFilters">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="batch-delete-toolbar">
        <el-button type="danger" plain :disabled="!selectedRows.length" @click="handleBatchDelete">批量删除</el-button>
      </div>

      <el-table
        :data="filteredMenuList"
        v-loading="loading"
        stripe
        row-key="id"
        :expand-row-keys="expandedRowKeys"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column prop="menuName" label="菜单名称" min-width="220" />
        <el-table-column prop="path" label="路由路径" min-width="220" show-overflow-tooltip />
        <el-table-column prop="icon" label="图标" min-width="160" show-overflow-tooltip />
        <el-table-column prop="menuSort" label="排序" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button text type="primary" @click="handleAddChild(row)">新增子菜单</el-button>
            <el-button text type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="上级菜单">
          <el-tree-select
            v-model="form.parentId"
            :data="menuTreeOptions"
            :props="{ label: 'menuName', children: 'children', value: 'id' }"
            :teleported="true"
            check-strictly
            clearable
            filterable
            popper-class="admin-menu-tree-select-popper"
            placeholder="请选择上级菜单"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="form.menuName" placeholder="请输入菜单名称" />
        </el-form-item>
        <el-form-item label="路由路径">
          <el-input v-model="form.path" placeholder="请输入路由路径" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="请输入图标名称" />
        </el-form-item>
        <el-form-item label="排序" prop="menuSort">
          <el-input-number v-model="form.menuSort" :min="0" :max="999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useBatchDelete } from '@/utils/batchDelete'
import { Plus } from '@element-plus/icons-vue'
import { createMenu, deleteMenu, getMenuTree, updateMenu } from '@/api/role'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增菜单')
const formRef = ref()
const menuList = ref([])
const menuTreeOptions = ref([])
const searchForm = reactive({
  menuName: ''
})

const form = reactive({
  id: null,
  parentId: 0,
  menuName: '',
  path: '',
  icon: '',
  menuSort: 0
})

const rules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuSort: [{ required: true, message: '请输入排序', trigger: 'blur' }]
}

const normalizeMenu = (item) => ({
  id: item.id,
  parentId: item.parentId ?? 0,
  menuName: item.label || '',
  path: item.path || '',
  icon: item.icon || '',
  menuSort: item.priority ?? 0,
  hasChildren: Boolean(item.subMenus?.length),
  children: (item.subMenus || []).map(normalizeMenu),
  createTime: item.createTime || ''
})

const buildTreeOptions = (rows) => [
  {
    id: 0,
    menuName: '根目录',
    children: rows
  }
]

const flattenMenus = (rows) => rows.flatMap((item) => [item, ...flattenMenus(item.children || [])])

const collectExpandableIds = (rows) => rows.flatMap((item) => {
  const children = item.children || []
  return children.length ? [item.id, ...collectExpandableIds(children)] : []
})

const filteredMenuList = computed(() => {
  if (!searchForm.menuName.trim()) {
    return menuList.value
  }
  const keyword = searchForm.menuName.trim().toLowerCase()
  const matches = new Set(
    flattenMenus(menuList.value)
      .filter((item) => item.menuName.toLowerCase().includes(keyword))
      .map((item) => item.id)
  )

  const filterTree = (rows) => rows
    .map((item) => {
      const children = filterTree(item.children || [])
      if (matches.has(item.id) || children.length) {
        return { ...item, children }
      }
      return null
    })
    .filter(Boolean)

  return filterTree(menuList.value)
})

const expandedRowKeys = computed(() => {
  if (!searchForm.menuName.trim()) {
    return []
  }
  return collectExpandableIds(filteredMenuList.value)
})

const fetchMenus = async () => {
  loading.value = true
  try {
    const res = await getMenuTree()
    const rows = (res?.data || res || []).map(normalizeMenu)
    menuList.value = rows
    menuTreeOptions.value = buildTreeOptions(rows)
  } catch (error) {
    console.error('获取菜单列表失败:', error)
    ElMessage.error('获取菜单列表失败')
  } finally {
    loading.value = false
  }
}

const refreshAdminMenus = async () => {
  await fetchMenus()
  await userStore.fetchAdminMenus()
}

const resetSearch = () => {
  searchForm.menuName = ''
}

const applyFilters = () => {
  // computed table filtering is enough; this handler keeps the current UI behavior
}

const resetForm = () => {
  form.id = null
  form.parentId = 0
  form.menuName = ''
  form.path = ''
  form.icon = ''
  form.menuSort = 0
}

const handleAdd = () => {
  dialogTitle.value = '新增菜单'
  resetForm()
  dialogVisible.value = true
}

const handleAddChild = (row) => {
  dialogTitle.value = '新增子菜单'
  resetForm()
  form.parentId = row.id
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑菜单'
  resetForm()
  form.id = row.id
  form.parentId = row.parentId ?? 0
  form.menuName = row.menuName
  form.path = row.path || ''
  form.icon = row.icon || ''
  form.menuSort = row.menuSort ?? 0
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该菜单吗？', '提示', { type: 'warning' })
    await deleteMenu(row.id)
    ElMessage.success('删除成功')
    await refreshAdminMenus()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除菜单失败:', error)
      ElMessage.error('删除菜单失败')
    }
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const payload = {
      parentId: form.parentId || 0,
      label: form.menuName,
      path: form.path || null,
      icon: form.icon || null,
      priority: form.menuSort ?? 0
    }

    if (form.id) {
      await updateMenu(form.id, payload)
      ElMessage.success('修改成功')
    } else {
      await createMenu(payload)
      ElMessage.success('新增成功')
    }

    dialogVisible.value = false
    await refreshAdminMenus()
  } catch (error) {
    console.error('保存菜单失败:', error)
    ElMessage.error(error.message || '保存菜单失败')
  } finally {
    submitLoading.value = false
  }
}


const { selectedRows, handleSelectionChange, handleBatchDelete } = useBatchDelete({
  entityName: '菜单',
  deleteItem: (id) => deleteMenu(id),
  fetchList: refreshAdminMenus
})

onMounted(() => {
  fetchMenus()
})
</script>

<style lang="scss" scoped>
.admin-page {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .search-form {
    margin-bottom: 20px;
  }
}
</style>
