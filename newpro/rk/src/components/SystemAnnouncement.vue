<template>
  <div class="system-announcement" v-if="visible">
    <el-alert
      :title="announcement.title"
      :type="announcement.type || 'warning'"
      show-icon
      :closable="true"
      @close="dismiss"
    >
      <p>{{ announcement.content }}</p>
      <p class="announcement-time">{{ announcement.time }}</p>
    </el-alert>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getPublishedNoticeList } from '@/api/notice'

const visible = ref(false)
const announcement = ref({ id: '', title: '', content: '', type: 'warning', time: '' })

onMounted(async () => {
  const dismissedKey = 'dismissed-announcement'
  try {
    const res = await getPublishedNoticeList()
    const latest = Array.isArray(res.data) ? res.data[0] : res.data?.records?.[0]

    if (latest) {
      const dismissedId = localStorage.getItem(dismissedKey)
      if (dismissedId !== String(latest.id)) {
        announcement.value = {
          id: latest.id,
          title: latest.title || '系统公告',
          content: latest.content || latest.body || '',
          type: 'warning',
          time: latest.publishTime || latest.createTime || ''
        }
        visible.value = true
      }
    }
  } catch {}
})

function dismiss() {
  visible.value = false
  localStorage.setItem('dismissed-announcement', String(announcement.value?.id || ''))
}
</script>

<style scoped>
.system-announcement { margin-bottom: 0; }
.announcement-time { font-size: 12px; color: #909399; margin-top: 4px; }
</style>
