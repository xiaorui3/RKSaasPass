<template>
  <div class="admin-not-found">
    <el-empty
      image-size="120"
      description="当前后台页面未接入或前端版本未包含该路由"
    >
      <div class="not-found-actions">
        <el-button type="primary" @click="router.push('/admin')">返回后台首页</el-button>
        <el-button @click="router.back()">返回上一页</el-button>
      </div>
    </el-empty>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped>
.admin-not-found {
  min-height: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 16px;
  background: #fff;
  border: 1px solid #edf0f7;
  border-radius: 8px;
}

.not-found-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}
</style>
