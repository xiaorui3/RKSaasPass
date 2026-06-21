<template>
  <div class="admin-forbidden-page">
    <el-result
      icon="warning"
      title="无权限访问"
      sub-title="当前账号没有该后台页面的菜单授权，页面已保留在后台工作台内。"
    >
      <template #extra>
        <el-button :loading="refreshing" @click="refreshMenus">刷新菜单权限</el-button>
        <el-button type="primary" @click="router.push('/admin')">返回后台首页</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const refreshing = ref(false)

async function refreshMenus() {
  refreshing.value = true
  try {
    await userStore.fetchAdminMenus()
    ElMessage.success('菜单权限已刷新')
  } finally {
    refreshing.value = false
  }
}
</script>

<style scoped>
.admin-forbidden-page {
  display: grid;
  min-height: calc(100vh - 150px);
  place-items: center;
  padding: 24px;
}
</style>
