<template>
  <div class="admin-page">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon" style="background: #409eff;">
              <el-icon :size="24"><OfficeBuilding /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-value">{{ stats.total }}</div>
              <div class="stats-label">租户总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon" style="background: #67c23a;">
              <el-icon :size="24"><CircleCheck /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-value">{{ stats.active }}</div>
              <div class="stats-label">正常租户</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon" style="background: #e6a23c;">
              <el-icon :size="24"><Warning /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-value">{{ stats.expiring }}</div>
              <div class="stats-label">即将到期</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-icon" style="background: #f56c6c;">
              <el-icon :size="24"><CircleClose /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-value">{{ stats.expired }}</div>
              <div class="stats-label">已过期</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-card>
      <template #header>
        <div class="card-header">
          <span>租户管理</span>
          <div class="header-actions">
            <el-button type="success" @click="handleExport">
              <el-icon><Download /></el-icon>导出
            </el-button>
            <el-button type="danger" :disabled="!selectedRows.length" @click="handleBatchDelete">
              批量删除
            </el-button>
            <el-button type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>新增租户
            </el-button>
          </div>
        </div>
      </template>
      
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="租户名称">
          <el-input v-model="searchForm.tenantName" placeholder="请输入租户名称" clearable />
        </el-form-item>
        <el-form-item label="租户编码">
          <el-input v-model="searchForm.tenantCode" placeholder="请输入租户编码" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable>
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchTenants">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
      
      <!-- 表格 -->
      <el-table :data="tenantList" v-loading="loading" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="Logo" width="88" align="center">
          <template #default="{ row }">
            <el-avatar v-if="row.resolvedLogoUrl || row.logoUrl" :src="row.resolvedLogoUrl || row.logoUrl" :size="36" />
            <el-tag v-else size="small" type="info">未配置</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="displayOrder" label="排序" width="90" />
        <el-table-column prop="tenantName" label="租户名称" />
        <el-table-column prop="tenantCode" label="租户编码" />
        <el-table-column prop="contactPerson" label="联系人" />
        <el-table-column prop="contactPhone" label="联系电话" />
        <el-table-column prop="packageId" label="租户套餐">
          <template #default="{ row }">
            <el-tag :type="packageNameMap[row.packageId] ? '' : 'info'">{{ packageNameMap[row.packageId] || '未分配' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="expireTime" label="过期时间" width="160">
          <template #default="{ row }">
            <span :class="{ 'text-danger': isExpiring(row.expireTime) }">{{ row.expireTime }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="handleStatusChange(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="handleViewDetail(row)">详情</el-button>
            <el-button text type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button text type="warning" @click="handleRenew(row)">续费</el-button>
            <el-button text type="danger" @click="handleDelete(row)">删除</el-button>
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
        @change="fetchTenants"
      />
    </el-card>
    
    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="租户名称" prop="tenantName">
              <el-input v-model="form.tenantName" placeholder="请输入租户名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="租户编码" prop="tenantCode">
              <el-input v-model="form.tenantCode" placeholder="请输入租户编码" :disabled="!!form.id" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示排序" prop="displayOrder">
              <el-input-number v-model="form.displayOrder" :min="0" :step="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="租户 Logo">
              <div class="tenant-logo-field">
                <el-avatar v-if="form.resolvedLogoUrl || form.logoUrl" :src="form.resolvedLogoUrl || form.logoUrl" :size="48" />
                <el-tag v-else size="small" type="info">未上传</el-tag>
                <el-upload :show-file-list="false" :http-request="handleLogoUpload">
                  <el-button type="primary" plain>上传 Logo</el-button>
                </el-upload>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人" prop="contactPerson">
              <el-input v-model="form.contactPerson" placeholder="请输入联系人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系邮箱">
              <el-input v-model="form.contactEmail" placeholder="请输入联系邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="租户套餐" prop="packageId">
              <el-select v-model="form.packageId" placeholder="请选择租户套餐">
                <el-option v-for="item in packageList" :key="item.id" :label="item.packageName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="过期时间">
              <el-date-picker
                v-model="form.expireTime"
                type="datetime"
                placeholder="请选择过期时间"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户限额">
              <el-input-number v-model="form.accountLimit" :min="0" :max="999999" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="地址">
              <el-input v-model="form.address" placeholder="请输入地址" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 套餐分配对话框 -->
    <el-dialog v-model="packageDialogVisible" title="套餐分配" width="500px">
      <el-form :model="packageForm" label-width="100px">
        <el-form-item label="租户套餐">
          <el-select v-model="packageForm.packageId" placeholder="请选择租户套餐">
            <el-option v-for="item in packageList" :key="item.id" :label="item.packageName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker
            v-model="packageForm.expireTime"
            type="datetime"
            placeholder="请选择过期时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="packageDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSavePackage">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="租户详情" width="600px">
      <el-descriptions :column="2" border v-if="currentTenant">
        <el-descriptions-item label="租户名称">{{ currentTenant.tenantName }}</el-descriptions-item>
        <el-descriptions-item label="租户编码">{{ currentTenant.tenantCode }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ currentTenant.contactPerson }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ currentTenant.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="联系邮箱">{{ currentTenant.contactEmail }}</el-descriptions-item>
        <el-descriptions-item label="租户套餐">
          <el-tag :type="currentTenant.packageName ? '' : 'info'">{{ currentTenant.packageName || '未分配' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="过期时间">
          <span :class="{ 'text-danger': isExpiring(currentTenant.expireTime) }">{{ currentTenant.expireTime }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="用户限额">{{ currentTenant.accountLimit }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentTenant.status === 1 ? 'success' : 'danger'">
            {{ currentTenant.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentTenant.createTime }}</el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">{{ currentTenant.address }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentTenant.remark || '无' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
    
    <!-- 续费对话框 -->
    <el-dialog v-model="renewDialogVisible" title="租户续费" width="500px">
      <el-form :model="renewForm" label-width="100px" v-if="currentTenant">
        <el-form-item label="租户名称">
          <el-input :value="currentTenant.tenantName" disabled />
        </el-form-item>
        <el-form-item label="当前到期">
          <el-input :value="currentTenant.expireTime" disabled />
        </el-form-item>
        <el-form-item label="续费时长">
          <el-select v-model="renewForm.months" placeholder="请选择续费时长">
            <el-option label="1个月" :value="1" />
            <el-option label="3个月" :value="3" />
            <el-option label="6个月" :value="6" />
            <el-option label="12个月" :value="12" />
            <el-option label="24个月" :value="24" />
            <el-option label="36个月" :value="36" />
          </el-select>
        </el-form-item>
        <el-form-item label="新到期时间">
          <el-date-picker
            v-model="renewForm.expireTime"
            type="datetime"
            placeholder="选择新的到期时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmRenew">确认续费</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Download, OfficeBuilding, CircleCheck, Warning, CircleClose } from '@element-plus/icons-vue'
import { getTenantList, createTenant, updateTenant, deleteTenant, updateTenantStatus } from '@/api/tenant'
import { uploadManagedFile } from '@/utils/fileUpload'
import { normalizeTenantDirectory } from '@/utils/tenantBranding'
import { resolveMediaUrl } from '@/utils/mediaUrl'

const loading = ref(false)
const tenantList = ref([])
const selectedRows = ref([])

// 套餐数据：静态定义（后端暂无套餐表，前端独立维护）
const packageList = ref([
  { id: 1, packageName: '基础版' },
  { id: 2, packageName: '专业版' },
  { id: 3, packageName: '企业版' }
])
const packageNameMap = ref({ 1: '基础版', 2: '专业版', 3: '企业版' })
const dialogVisible = ref(false)
const dialogTitle = ref('新增租户')
const formRef = ref()
const packageDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const renewDialogVisible = ref(false)
const currentTenant = ref(null)
const currentTenantId = ref(null)

const stats = computed(() => {
  const now = new Date()
  const thirtyDaysLater = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000)
  return {
    total: tenantList.value.length,
    active: tenantList.value.filter(t => t.status === 1).length,
    expiring: tenantList.value.filter(t => {
      const expireTime = new Date(t.expireTime)
      return expireTime > now && expireTime < thirtyDaysLater
    }).length,
    expired: tenantList.value.filter(t => new Date(t.expireTime) < now).length
  }
})

const searchForm = reactive({
  tenantName: '',
  tenantCode: '',
  status: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const form = reactive({
  id: null,
  tenantName: '',
  tenantCode: '',
  logoUrl: '',
  resolvedLogoUrl: '',
  displayOrder: 0,
  contactPerson: '',
  contactPhone: '',
  contactEmail: '',
  expireTime: '',
  status: 1
})

const packageForm = reactive({
  packageId: null,
  expireTime: ''
})

const renewForm = reactive({
  months: 12,
  expireTime: ''
})

const rules = {
  tenantName: [{ required: true, message: '请输入租户名称', trigger: 'blur' }],
  tenantCode: [{ required: true, message: '请输入租户编码', trigger: 'blur' }],
  contactPerson: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }]
}

const isExpiring = (expireTime) => {
  if (!expireTime) return false
  const now = new Date()
  const expire = new Date(expireTime)
  const thirtyDaysLater = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000)
  return expire < thirtyDaysLater
}

const fetchTenants = async () => {
  loading.value = true
  try {
    const res = await getTenantList({
      tenantName: searchForm.tenantName,
      tenantCode: searchForm.tenantCode,
      status: searchForm.status,
      page: pagination.page,
      size: pagination.size
    })
    tenantList.value = normalizeTenantDirectory(res.data?.records || [])
    pagination.total = Number(res.data?.total || 0)
  } catch (e) {
    ElMessage.error('获取租户列表失败: ' + (e.message || '未知错误'))
    tenantList.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.tenantName = ''
  searchForm.tenantCode = ''
  searchForm.status = ''
  fetchTenants()
}

const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

const handleAdd = () => {
  dialogTitle.value = '新增租户'
  Object.assign(form, { id: null, tenantName: '', tenantCode: '', logoUrl: '', resolvedLogoUrl: '', displayOrder: 0, contactPerson: '', contactPhone: '', contactEmail: '', expireTime: '', status: 1 })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑租户'
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleViewDetail = (row) => {
  currentTenant.value = { ...row, packageName: packageNameMap.value[row.packageId] }
  detailDialogVisible.value = true
}

const handleStatusChange = async (row) => {
  try {
    const res = await updateTenantStatus(row.id, row.status)
    if (res.code === 200) {
      ElMessage.success(`租户 ${row.tenantName} 状态已${row.status === 1 ? '启用' : '禁用'}`)
    } else {
      row.status = row.status === 1 ? 0 : 1
      ElMessage.error(res.msg || '状态更新失败')
    }
  } catch (e) {
    row.status = row.status === 1 ? 0 : 1
    ElMessage.error('状态更新失败')
  }
}

const handleRenew = (row) => {
  currentTenant.value = row
  renewForm.months = 12
  renewForm.expireTime = row.expireTime
  renewDialogVisible.value = true
}

const handleConfirmRenew = async () => {
  try {
    const expireDate = new Date(renewForm.expireTime)
    expireDate.setMonth(expireDate.getMonth() + renewForm.months)
    const res = await updateTenant({
      id: currentTenant.value.id,
      tenantName: currentTenant.value.tenantName,
      tenantCode: currentTenant.value.tenantCode,
      contactPerson: currentTenant.value.contactPerson,
      contactPhone: currentTenant.value.contactPhone,
      contactEmail: currentTenant.value.contactEmail,
      status: currentTenant.value.status,
      expireTime: expireDate.toISOString().slice(0, 19).replace('T', ' ')
    })
    if (res.code === 200) {
      ElMessage.success(`续费成功，已延长 ${renewForm.months} 个月`)
      renewDialogVisible.value = false
      fetchTenants()
    } else {
      ElMessage.error(res.msg || '续费失败')
    }
  } catch (e) {
    ElMessage.error('续费失败: ' + (e.message || '未知错误'))
  }
}

const handlePackage = (row) => {
  currentTenantId.value = row.id
  packageForm.packageId = row.packageId
  packageForm.expireTime = row.expireTime
  packageDialogVisible.value = true
}

const handleSavePackage = async () => {
  try {
    // 后端RKTenant无packageId字段，只更新expireTime
    const res = await updateTenant({
      id: currentTenantId.value,
      tenantName: currentTenant.value?.tenantName,
      tenantCode: currentTenant.value?.tenantCode,
      contactPerson: currentTenant.value?.contactPerson,
      contactPhone: currentTenant.value?.contactPhone,
      contactEmail: currentTenant.value?.contactEmail,
      status: currentTenant.value?.status,
      expireTime: packageForm.expireTime
    })
    if (res.code === 200) {
      ElMessage.success('套餐分配成功')
      packageDialogVisible.value = false
      fetchTenants()
    } else {
      ElMessage.error(res.msg || '套餐分配失败')
    }
  } catch (e) {
    ElMessage.error('套餐分配失败: ' + (e.message || '未知错误'))
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定要删除该租户吗？删除后将无法恢复！', '提示', { type: 'warning' })
  try {
    const res = await deleteTenant(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchTenants()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败: ' + (e.message || '未知错误'))
    }
  }
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择要删除的租户')
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedRows.value.length} 个租户吗？删除后将无法恢复！`, '批量删除租户', { type: 'warning' })
    for (const row of selectedRows.value) {
      const res = await deleteTenant(row.id)
      if (res.code !== 200) {
        throw new Error(res.msg || `删除租户 ${row.tenantName} 失败`)
      }
    }
    ElMessage.success('批量删除成功')
    selectedRows.value = []
    await fetchTenants()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('批量删除失败: ' + (e.message || '未知错误'))
    }
  }
}

const handleLogoUpload = async ({ file }) => {
  try {
    const { storedValue, url } = await uploadManagedFile({ file }, 'tenant-logo')
    if (!storedValue) {
      throw new Error('上传结果缺少文件地址')
    }
    form.logoUrl = storedValue
    form.resolvedLogoUrl = resolveMediaUrl(storedValue || url)
    ElMessage.success('Logo 上传成功')
  } catch (e) {
    ElMessage.error(e.message || 'Logo 上传失败')
  }
}

const handleSubmit = async () => {
  await formRef.value.validate()
  try {
    // 只发送后端RKTenant实体存在的字段
    const data = {
      id: form.id,
      tenantName: form.tenantName,
      tenantCode: form.tenantCode,
      logoUrl: form.logoUrl,
      displayOrder: form.displayOrder,
      contactPerson: form.contactPerson,
      contactPhone: form.contactPhone,
      contactEmail: form.contactEmail,
      status: form.status,
      expireTime: form.expireTime
    }
    let res
    if (data.id) {
      // 更新
      res = await updateTenant(data)
    } else {
      // 新增
      res = await createTenant(data)
    }

    if (res.code === 200) {
      ElMessage.success(form.id ? '修改成功' : '新增成功')
      dialogVisible.value = false
      fetchTenants()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败: ' + (e.message || '未知错误'))
  }
}

const handleExport = () => {
  const csvContent = 'ID,租户名称,租户编码,联系人,联系电话,联系邮箱,套餐,过期时间,状态,创建时间\n'
  const data = tenantList.value.map(t => 
    `${t.id},${t.tenantName},${t.tenantCode},${t.contactPerson},${t.contactPhone},${t.contactEmail},${packageNameMap.value[t.packageId]},${t.expireTime},${t.status === 1 ? '正常' : '禁用'},${t.createTime}`
  ).join('\n')
  
  const blob = new Blob(['\ufeff' + csvContent + data], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `租户数据_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  ElMessage.success('导出成功')
}

onMounted(() => {
  fetchTenants()
})
</script>

<style lang="scss" scoped>
.admin-page {
  .tenant-logo-field {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .stats-row {
    margin-bottom: 20px;
  }
  
  .stats-card {
    .stats-content {
      display: flex;
      align-items: center;
      gap: 16px;
    }
    
    .stats-icon {
      width: 48px;
      height: 48px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
    }
    
    .stats-info {
      .stats-value {
        font-size: 24px;
        font-weight: 600;
        color: #303133;
      }
      
      .stats-label {
        font-size: 14px;
        color: #909399;
      }
    }
  }
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .header-actions {
      display: flex;
      gap: 10px;
    }
  }
  
  .search-form {
    margin-bottom: 20px;
  }
  
  .el-pagination {
    margin-top: 20px;
    justify-content: flex-end;
  }
  
  .text-danger {
    color: #f56c6c;
    font-weight: 500;
  }
}
</style>
