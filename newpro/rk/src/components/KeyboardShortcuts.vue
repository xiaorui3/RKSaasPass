<template>
  <div class="shortcuts-overlay" v-if="showHelp" @click="showHelp = false">
    <div class="shortcuts-panel" @click.stop>
      <h3>快捷键</h3>
      <div class="shortcut-row"><kbd>Ctrl</kbd>+<kbd>K</kbd><span>打开搜索</span></div>
      <div class="shortcut-row"><kbd>Ctrl</kbd>+<kbd>/</kbd><span>显示帮助</span></div>
      <div class="shortcut-row"><kbd>Esc</kbd><span>关闭弹窗</span></div>
      <div class="shortcut-row"><kbd>Ctrl</kbd>+<kbd>Enter</kbd><span>提交表单</span></div>
    </div>
  </div>
  <Teleport to="body">
    <div v-if="showSearch" class="command-palette" @click="showSearch = false">
      <div class="command-box" @click.stop>
        <input v-model="query" ref="searchInputRef" placeholder="输入关键词搜索..." @keyup.enter="doSearch" @keyup.esc="showSearch = false" />
        <div class="command-results" v-if="query">
          <div v-for="r in results" :key="r.path" class="command-item" @click="navigate(r.path)">
            <span class="command-icon">{{ r.shortLabel }}</span>
            <span>{{ r.name }}</span>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const showHelp = ref(false)
const showSearch = ref(false)
const query = ref('')
const searchInputRef = ref(null)

const pages = [
  { name: '首页', path: '/', shortLabel: '首' },
  { name: '新闻动态', path: '/news', shortLabel: '新' },
  { name: '活动中心', path: '/activities', shortLabel: '活' },
  { name: '活动比赛日历', path: '/calendar', shortLabel: '历' },
  { name: '比赛活动', path: '/competition', shortLabel: '赛' },
  { name: '作品展示', path: '/works', shortLabel: '作' },
  { name: '社团概况', path: '/about', shortLabel: '介' },
  { name: '社团历程', path: '/history', shortLabel: '史' },
  { name: '消息通知', path: '/notifications', shortLabel: '讯' },
  { name: '个人中心', path: '/profile', shortLabel: '我' },
  { name: '后台管理', path: '/admin', shortLabel: '管' },
  { name: '全站搜索', path: '/search', shortLabel: '搜' },
]

const results = ref([])

watch(query, (q) => {
  if (!q) { results.value = []; return }
  results.value = pages.filter(p => p.name.includes(q))
})

function navigate(path) {
  showSearch.value = false
  router.push(path)
}

function doSearch() {
  if (query.value) {
    showSearch.value = false
    router.push({ path: '/search', query: { q: query.value } })
  }
}

watch(showSearch, async (v) => {
  if (v) {
    query.value = ''
    await nextTick()
    searchInputRef.value?.focus()
  }
})

function handleKeydown(e) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
    e.preventDefault()
    showSearch.value = !showSearch.value
  }
  if ((e.ctrlKey || e.metaKey) && e.key === '/') {
    e.preventDefault()
    showHelp.value = !showHelp.value
  }
}

onMounted(() => document.addEventListener('keydown', handleKeydown))
onUnmounted(() => document.removeEventListener('keydown', handleKeydown))
</script>

<style scoped>
.shortcuts-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.4); z-index: 10000;
  display: flex; align-items: center; justify-content: center;
}
.shortcuts-panel {
  background: #fff; padding: 24px 32px; border-radius: 12px; min-width: 320px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
}
.shortcuts-panel h3 { margin: 0 0 16px; }
.shortcut-row { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
kbd {
  background: #f5f5f5; border: 1px solid #ddd; border-radius: 4px;
  padding: 2px 8px; font-size: 12px; font-family: monospace;
}
.shortcut-row span { margin-left: auto; color: #606266; font-size: 14px; }

.command-palette {
  position: fixed; inset: 0; background: rgba(0,0,0,0.3); z-index: 10001;
  display: flex; align-items: flex-start; justify-content: center; padding-top: 120px;
}
.command-box {
  background: #fff; border-radius: 12px; width: 520px; box-shadow: 0 20px 60px rgba(0,0,0,0.3); overflow: hidden;
}
.command-box input {
  width: 100%; border: none; padding: 16px 20px; font-size: 16px; outline: none; box-sizing: border-box;
}
.command-results { border-top: 1px solid #ebeef5; max-height: 300px; overflow-y: auto; }
.command-item {
  display: flex; align-items: center; gap: 12px; padding: 12px 20px; cursor: pointer;
  transition: background 0.15s;
}
.command-item:hover { background: #f5f7fa; }
.command-icon {
  width: 24px;
  color: #409eff;
  font-size: 12px;
  font-weight: 700;
  text-align: center;
}
</style>
