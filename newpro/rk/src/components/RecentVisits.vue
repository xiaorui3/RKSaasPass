<template>
  <div class="recent-visits" v-if="visits.length > 0">
    <span class="recent-label">最近访问：</span>
    <router-link v-for="v in visits" :key="v.path" :to="v.path" class="recent-link">{{ v.name }}</router-link>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const visits = ref(JSON.parse(localStorage.getItem('recent-visits') || '[]'))

const namedRoutes = {
  '/': '首页', '/news': '新闻', '/activities': '活动', '/competition': '比赛',
  '/works': '作品', '/about': '概况', '/calendar': '日历', '/history': '历程',
  '/notifications': '通知', '/profile': '个人中心', '/admin': '后台',
}

watch(() => route.path, (path) => {
  if (!path || path.startsWith('/login') || path.startsWith('/register')) return
  const name = namedRoutes[path] || route.meta?.title || path.split('/').pop()
  const list = visits.value.filter(v => v.path !== path)
  list.unshift({ path, name })
  visits.value = list.slice(0, 8)
  localStorage.setItem('recent-visits', JSON.stringify(visits.value))
})
</script>

<style scoped>
.recent-visits {
  display: flex; align-items: center; gap: 8px; padding: 6px 16px;
  background: #f5f7fa; border-bottom: 1px solid #ebeef5; font-size: 12px;
}
.recent-label { color: #909399; flex-shrink: 0; }
.recent-link {
  color: #606266; text-decoration: none; padding: 2px 8px; border-radius: 4px;
  background: #fff; border: 1px solid #e4e7ed; transition: all 0.15s;
}
.recent-link:hover { color: #409eff; border-color: #409eff; }
</style>
