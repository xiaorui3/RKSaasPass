<template>
  <div class="member-graph-page">
    <div class="page-header">
      <div>
        <h2>成员关系图</h2>
        <p>按成员台账与校友档案分开展示，可按姓名、学院、年级和性别快速筛选。</p>
      </div>
      <el-button :loading="loading" @click="loadData">刷新</el-button>
    </div>

    <el-card class="filter-card">
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item label="人员范围">
          <el-select v-model="filters.personType" style="width: 140px">
            <el-option label="全部人员" value="all" />
            <el-option label="仅看成员" value="member" />
            <el-option label="仅看校友" value="alumni" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            clearable
            placeholder="姓名、学号、部门、学院"
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="filters.department" clearable placeholder="全部部门" style="width: 160px">
            <el-option
              v-for="item in filterOptions.departments"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="学院">
          <el-select v-model="filters.college" clearable placeholder="全部学院" style="width: 160px">
            <el-option
              v-for="item in filterOptions.colleges"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="年级">
          <el-select v-model="filters.grade" clearable placeholder="全部年级" style="width: 140px">
            <el-option
              v-for="item in filterOptions.grades"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="filters.gender" clearable placeholder="全部性别" style="width: 120px">
            <el-option
              v-for="item in filterOptions.genders"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button @click="resetFilters">重置筛选</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="summary-grid">
      <div class="summary-card">
        <strong>{{ summary.totalPeople }}</strong>
        <span>当前图谱人数</span>
      </div>
      <div class="summary-card">
        <strong>{{ summary.memberCount }}</strong>
        <span>成员台账</span>
      </div>
      <div class="summary-card">
        <strong>{{ summary.alumniCount }}</strong>
        <span>校友档案</span>
      </div>
      <div class="summary-card">
        <strong>{{ summary.departmentCount }}/{{ summary.collegeCount }}</strong>
        <span>部门/学院维度</span>
      </div>
    </div>

    <el-card>
      <el-empty
        v-if="!summary.totalPeople && !loading"
        description="当前筛选条件下暂无可展示的成员或校友数据"
      />
      <div v-else ref="chartRef" class="graph-container"></div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { echarts } from '@/utils/echarts/memberGraph'
import { getMembersList } from '@/api/member'
import { getAlumniList } from '@/api/alumni'
import { isActiveMemberStatus } from '@/utils/memberStatus'
import {
  buildMemberGraphData,
  buildMemberGraphSummary,
  extractMemberGraphFilterOptions,
  filterMemberGraphPeople
} from '@/utils/memberGraph'

const loading = ref(false)
const chartRef = ref(null)
const rawMembers = ref([])
const rawAlumni = ref([])
let chart = null

const filters = reactive({
  personType: 'all',
  keyword: '',
  department: '',
  college: '',
  grade: '',
  gender: ''
})

const normalizeList = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.records)) return payload.records
  if (Array.isArray(payload?.list)) return payload.list
  return []
}

const filterOptions = computed(() => extractMemberGraphFilterOptions(rawMembers.value, rawAlumni.value))

const filteredPeople = computed(() => filterMemberGraphPeople(rawMembers.value, rawAlumni.value, filters))

const summary = computed(() => buildMemberGraphSummary(
  filteredPeople.value.members,
  filteredPeople.value.alumni
))

async function renderChart() {
  await nextTick()
  if (!chartRef.value) {
    return
  }

  if (!chart) {
    chart = echarts.init(chartRef.value)
  }

  if (!summary.value.totalPeople) {
    chart.clear()
    return
  }

  const { nodes, links } = buildMemberGraphData(
    filteredPeople.value.members,
    filteredPeople.value.alumni
  )

  chart.setOption({
    tooltip: {},
    legend: {
      data: ['成员关系图', '一级分组', '二级分组', '人员']
    },
    series: [
      {
        type: 'graph',
        layout: 'force',
        roam: true,
        draggable: true,
        force: {
          repulsion: 280,
          edgeLength: [90, 180]
        },
        data: nodes,
        links,
        categories: [
          { name: '成员关系图' },
          { name: '一级分组' },
          { name: '二级分组' },
          { name: '人员' }
        ],
        label: { show: true, position: 'right' },
        lineStyle: { color: '#94a3b8', curveness: 0.18 }
      }
    ]
  }, true)
}

async function loadData() {
  loading.value = true
  try {
    const [memberRes, alumniRes] = await Promise.all([getMembersList(), getAlumniList()])
    rawMembers.value = normalizeList(memberRes.data).filter((member) => isActiveMemberStatus(member?.status))
    rawAlumni.value = normalizeList(alumniRes.data)
    await renderChart()
  } catch (error) {
    console.error('load member graph failed', error)
    ElMessage.error('加载成员关系图失败')
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.personType = 'all'
  filters.keyword = ''
  filters.department = ''
  filters.college = ''
  filters.grade = ''
  filters.gender = ''
}

const handleResize = () => {
  chart?.resize()
}

watch(filteredPeople, () => {
  renderChart()
}, { deep: true })

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.member-graph-page {
  display: grid;
  gap: 16px;
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.page-header h2 {
  margin: 0 0 8px;
}

.page-header p {
  margin: 0;
  color: #64748b;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 8px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.summary-card {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 16px;
  background: linear-gradient(135deg, #eff6ff 0%, #f8fafc 100%);
  border: 1px solid #dbeafe;
}

.summary-card strong {
  font-size: 26px;
  color: #0f172a;
}

.summary-card span {
  color: #475569;
}

.graph-container {
  width: 100%;
  min-height: 620px;
}

@media (max-width: 960px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .member-graph-page {
    padding: 12px;
  }

  .summary-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .filter-form :deep(.el-form-item) {
    width: 100%;
    margin-right: 0;
    margin-bottom: 0;
  }

  .filter-form :deep(.el-form-item__content) {
    width: 100%;
  }

  .filter-form :deep(.el-select),
  .filter-form :deep(.el-input) {
    width: 100% !important;
  }

  .graph-container {
    min-height: 520px;
  }
}
</style>
