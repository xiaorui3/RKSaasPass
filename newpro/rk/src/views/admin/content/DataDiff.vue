<template>
  <div class="data-diff-page">
    <el-alert type="warning" :closable="false" show-icon style="margin-bottom:20px;">
      <template #title>功能开发中 — 当前数据为示例数据，对接后端统计接口后生效</template>
    </el-alert>
    <div class="page-header">
      <h2>数据对比视图</h2>
    </div>
    <el-card>
      <el-form :inline="true" style="margin-bottom:20px;">
        <el-form-item label="数据类型">
          <el-select v-model="dataType" @change="loadData">
            <el-option label="活动报名" value="activity" />
            <el-option label="新闻发布" value="news" />
            <el-option label="成员统计" value="members" />
          </el-select>
        </el-form-item>
        <el-form-item label="对比周期">
          <el-select v-model="period" @change="loadData">
            <el-option label="本周 vs 上周" value="week" />
            <el-option label="本月 vs 上月" value="month" />
            <el-option label="本学期 vs 上学期" value="semester" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-row :gutter="20" v-if="diffData">
        <el-col :span="12">
          <el-card shadow="never" class="diff-card">
            <template #header>
              <span>{{ period === 'week' ? '本周' : period === 'month' ? '本月' : '本学期' }}</span>
              <el-tag type="success" size="small" style="margin-left:8px;">当前</el-tag>
            </template>
            <div class="diff-stat" v-for="(item, key) in diffData.current" :key="key">
              <span class="diff-label">{{ item.label }}</span>
              <span class="diff-value">{{ item.value }}</span>
              <span class="diff-change" :class="item.change >= 0 ? 'up' : 'down'">
                {{ item.change >= 0 ? '↑' : '↓' }} {{ Math.abs(item.change) }}%
              </span>
            </div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="never" class="diff-card">
            <template #header>
              <span>{{ period === 'week' ? '上周' : period === 'month' ? '上月' : '上学期' }}</span>
              <el-tag type="info" size="small" style="margin-left:8px;">对比</el-tag>
            </template>
            <div class="diff-stat" v-for="(item, key) in diffData.previous" :key="key">
              <span class="diff-label">{{ item.label }}</span>
              <span class="diff-value">{{ item.value }}</span>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <div ref="chartRef" style="height:350px;margin-top:20px;"></div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { echarts } from '@/utils/echarts/dataDiff'

const dataType = ref('activity')
const period = ref('week')
const chartRef = ref(null)
let chart = null

const mockData = {
  activity: {
    week: {
      current: [
        { label: '报名人数', value: 128, change: 15 },
        { label: '签到率', value: '89%', change: 3 },
        { label: '新增活动', value: 8, change: -10 },
        { label: '参与人数', value: 256, change: 22 }
      ],
      previous: [
        { label: '报名人数', value: 111 },
        { label: '签到率', value: '86%' },
        { label: '新增活动', value: 9 },
        { label: '参与人数', value: 210 }
      ]
    },
    month: {
      current: [
        { label: '报名人数', value: 520, change: 8 },
        { label: '签到率', value: '91%', change: 5 },
        { label: '新增活动', value: 32, change: 12 },
        { label: '参与人数', value: 1024, change: 18 }
      ],
      previous: [
        { label: '报名人数', value: 481 },
        { label: '签到率', value: '87%' },
        { label: '新增活动', value: 28 },
        { label: '参与人数', value: 868 }
      ]
    },
    semester: {
      current: [
        { label: '报名人数', value: 2400, change: 25 },
        { label: '签到率', value: '88%', change: 2 },
        { label: '新增活动', value: 120, change: 30 },
        { label: '参与人数', value: 4800, change: 20 }
      ],
      previous: [
        { label: '报名人数', value: 1920 },
        { label: '签到率', value: '86%' },
        { label: '新增活动', value: 92 },
        { label: '参与人数', value: 4000 }
      ]
    }
  },
  news: {
    week: {
      current: [
        { label: '发布数量', value: 15, change: 20 },
        { label: '浏览总量', value: 3200, change: 35 },
        { label: '审核通过', value: 12, change: 10 },
        { label: '平均阅读', value: 213, change: 12 }
      ],
      previous: [
        { label: '发布数量', value: 12 },
        { label: '浏览总量', value: 2370 },
        { label: '审核通过', value: 10 },
        { label: '平均阅读', value: 190 }
      ]
    },
    month: {
      current: [
        { label: '发布数量', value: 58, change: 5 },
        { label: '浏览总量', value: 12500, change: 15 },
        { label: '审核通过', value: 50, change: 8 },
        { label: '平均阅读', value: 215, change: 10 }
      ],
      previous: [
        { label: '发布数量', value: 55 },
        { label: '浏览总量', value: 10870 },
        { label: '审核通过', value: 46 },
        { label: '平均阅读', value: 195 }
      ]
    },
    semester: {
      current: [
        { label: '发布数量', value: 230, change: 18 },
        { label: '浏览总量', value: 52000, change: 30 },
        { label: '审核通过', value: 200, change: 15 },
        { label: '平均阅读', value: 226, change: 10 }
      ],
      previous: [
        { label: '发布数量', value: 194 },
        { label: '浏览总量', value: 40000 },
        { label: '审核通过', value: 170 },
        { label: '平均阅读', value: 206 }
      ]
    }
  },
  members: {
    week: {
      current: [
        { label: '新成员', value: 12, change: 50 },
        { label: '活跃成员', value: 85, change: 5 },
        { label: '申请数', value: 18, change: 20 },
        { label: '通过率', value: '75%', change: -3 }
      ],
      previous: [
        { label: '新成员', value: 8 },
        { label: '活跃成员', value: 81 },
        { label: '申请数', value: 15 },
        { label: '通过率', value: '78%' }
      ]
    },
    month: {
      current: [
        { label: '新成员', value: 45, change: 15 },
        { label: '活跃成员', value: 92, change: 8 },
        { label: '申请数', value: 60, change: 10 },
        { label: '通过率', value: '76%', change: 2 }
      ],
      previous: [
        { label: '新成员', value: 39 },
        { label: '活跃成员', value: 85 },
        { label: '申请数', value: 54 },
        { label: '通过率', value: '74%' }
      ]
    },
    semester: {
      current: [
        { label: '新成员', value: 180, change: 40 },
        { label: '活跃成员', value: 320, change: 20 },
        { label: '申请数', value: 240, change: 25 },
        { label: '通过率', value: '75%', change: 5 }
      ],
      previous: [
        { label: '新成员', value: 128 },
        { label: '活跃成员', value: 267 },
        { label: '申请数', value: 192 },
        { label: '通过率', value: '70%' }
      ]
    }
  }
}

const diffData = ref(null)

function loadData() {
  diffData.value = mockData[dataType.value]?.[period.value] || null
  renderChart()
}

function renderChart() {
  if (!diffData.value) return
  const labels = diffData.value.current.map(d => d.label)
  const curValues = diffData.value.current.map(d => typeof d.value === 'number' ? d.value : parseInt(d.value))
  const prevValues = diffData.value.previous.map(d => typeof d.value === 'number' ? d.value : parseInt(d.value))
  nextTick(() => {
    if (!chartRef.value) return
    if (!chart) chart = echarts.init(chartRef.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['当前', '对比'] },
      xAxis: { type: 'category', data: labels },
      yAxis: { type: 'value' },
      series: [
        { name: '当前', type: 'bar', data: curValues, itemStyle: { color: '#409eff' } },
        { name: '对比', type: 'bar', data: prevValues, itemStyle: { color: '#e6e6e6' } }
      ]
    })
  })
}

onMounted(() => { loadData() })
</script>

<style scoped>
.data-diff-page { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; }
.diff-stat { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #ebeef5; }
.diff-label { color: #606266; }
.diff-value { font-size: 18px; font-weight: 600; }
.diff-change { font-size: 13px; }
.diff-change.up { color: #67c23a; }
.diff-change.down { color: #f56c6c; }
</style>
