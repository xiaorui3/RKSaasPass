<template>
  <div class="data-export-page">
    <div class="page-header">
      <h2>数据导出中心</h2>
    </div>

    <div class="export-grid">
      <el-card v-for="item in exportItems" :key="item.key" class="export-card">
        <div class="export-icon">{{ item.icon }}</div>
        <h3>{{ item.title }}</h3>
        <p>{{ item.desc }}</p>
        <el-button type="primary" size="small" @click="handleExport(item)" :loading="item.loading">
          导出 Excel
        </el-button>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import * as XLSX from 'xlsx'
import { saveAs } from 'file-saver'
import { getNewsList } from '@/api/news'
import { getActivityList } from '@/api/activity'
import { getCompetitionList } from '@/api/competition'
import { getMembersList } from '@/api/member'
import { normalizeExportRow } from '@/utils/exportEncoding'

const exportItems = reactive([
  {
    key: 'news', icon: '新闻', title: '新闻数据', desc: '导出所有新闻标题、分类、作者、状态',
    loading: false,
    fetch: async () => {
      const res = await getNewsList({ page: 1, size: 1000 })
      const list = res.data?.records || res.data || []
      return list.map(r => ({
        ID: r.id, 标题: r.title, 分类: r.category, 作者: r.author,
        浏览量: r.viewCount || r.views, 状态: r.status, 创建时间: r.createTime
      }))
    },
    columns: ['ID', '标题', '分类', '作者', '浏览量', '状态', '创建时间']
  },
  {
    key: 'activities', icon: '活动', title: '活动数据', desc: '导出活动名称、时间、地点、参与人数',
    loading: false,
    fetch: async () => {
      const res = await getActivityList()
      const list = Array.isArray(res.data) ? res.data : res.data?.records || []
      return list.map(r => ({
        ID: r.id, 活动名称: r.activityName || r.title, 地点: r.location,
        开始时间: r.startTime, 结束时间: r.endTime, 参与人数: r.currentParticipants || 0,
        最大人数: r.maxParticipants || '', 创建时间: r.createTime
      }))
    },
    columns: ['ID', '活动名称', '地点', '开始时间', '结束时间', '参与人数', '最大人数', '创建时间']
  },
  {
    key: 'competitions', icon: '比赛', title: '比赛数据', desc: '导出比赛名称、类型、报名人数',
    loading: false,
    fetch: async () => {
      const res = await getCompetitionList()
      const list = Array.isArray(res.data) ? res.data : res.data?.records || []
      return list.map(r => ({
        ID: r.id, 比赛名称: r.competitionName || r.title, 类型: r.competitionType,
        报名开始: r.registrationStart, 报名结束: r.registrationEnd,
        比赛开始: r.competitionStart, 比赛结束: r.competitionEnd, 创建时间: r.createTime
      }))
    },
    columns: ['ID', '比赛名称', '类型', '报名开始', '报名结束', '比赛开始', '比赛结束', '创建时间']
  },
  {
    key: 'members', icon: '成员', title: '成员数据', desc: '导出社团成员信息',
    loading: false,
    fetch: async () => {
      const res = await getMembersList()
      const list = Array.isArray(res.data) ? res.data : res.data?.records || []
      return list.map(r => ({
        ID: r.id, 用户名: r.username, 姓名: r.name || r.realName,
        邮箱: r.email, 手机: r.cellPhone || r.phone, 角色: r.roleName,
        创建时间: r.createTime
      }))
    },
    columns: ['ID', '用户名', '姓名', '邮箱', '手机', '角色', '创建时间']
  },
])

async function handleExport(item) {
  item.loading = true
  try {
    const data = (await item.fetch()).map(normalizeExportRow)
    if (data.length === 0) {
      ElMessage.warning('暂无数据可导出')
      return
    }
    const ws = XLSX.utils.json_to_sheet(data)
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, item.title)
    const wbout = XLSX.write(wb, { bookType: 'xlsx', type: 'array' })
    const blob = new Blob([wbout], { type: 'application/octet-stream' })
    saveAs(blob, `${item.title}-${new Date().toISOString().split('T')[0]}.xlsx`)
    ElMessage.success(`成功导出 ${data.length} 条${item.title}`)
  } catch (e) {
    ElMessage.error('导出失败: ' + (e.message || '未知错误'))
  }
  item.loading = false
}
</script>

<style scoped>
.data-export-page { padding: 20px; }
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0; }
.export-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}
.export-card {
  text-align: center;
  padding: 8px;
}
.export-icon { font-size: 40px; margin-bottom: 12px; }
.export-card h3 { margin: 0 0 8px; font-size: 16px; }
.export-card p { margin: 0 0 16px; font-size: 13px; color: #909399; }
</style>
