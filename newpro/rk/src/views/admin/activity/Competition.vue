<template>
  <div class="admin-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <div>
            <div class="title">比赛管理</div>
            <div class="subtitle">创建比赛时配置学分，成绩录入后按配置发放</div>
          </div>
          <el-button v-if="canEdit" type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增比赛
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="搜索标题或描述" clearable />
        </el-form-item>
        <el-form-item label="比赛类型">
          <el-select v-model="searchForm.competitionType" placeholder="全部类型" clearable style="width: 180px">
            <el-option
              v-for="item in competitionTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 160px">
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="batch-delete-toolbar">
        <el-button v-if="canEdit" type="danger" plain :disabled="!selectedRows.length" @click="handleBatchDelete">批量删除</el-button>
      </div>

      <el-table :data="dataList" v-loading="loading" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="比赛标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="competitionType" label="比赛类型" width="120">
          <template #default="{ row }">
            <el-tag :type="typeMap[row.competitionType]?.tag || 'info'">
              {{ row.competitionTypeName || typeMap[row.competitionType]?.label || row.competitionType || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="organizer" label="主办方" min-width="140" show-overflow-tooltip />
        <el-table-column label="学分配置" min-width="240">
          <template #default="{ row }">
            <div class="credit-summary">
              <span>参赛 {{ row.participationPoints ?? 0 }}</span>
              <span>一等奖 {{ row.firstPrizePoints ?? 0 }}</span>
              <span>二等奖 {{ row.secondPrizePoints ?? 0 }}</span>
              <span>三等奖 {{ row.thirdPrizePoints ?? 0 }}</span>
              <span>优秀奖 {{ row.excellentPrizePoints ?? 0 }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="registrationCount" label="报名人数" width="100" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.tag || 'info'">
              {{ row.statusName || statusMap[row.status]?.label || row.status || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="跨租户" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isCrossTenant ? 'success' : 'info'">
              {{ row.isCrossTenant ? '共享' : '本租户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canEdit" text type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button text type="success" @click="handleViewTeams(row)">报名管理</el-button>
            <el-button text type="warning" @click="handleViewResults(row)">成绩管理</el-button>
            <el-button
              v-if="canEdit"
              text
              type="success"
              :disabled="!isGrantableCompetition(row)"
              @click="handleGrantCredits(row)"
            >
              发放学分
            </el-button>
            <el-button v-if="canEdit" text type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="pagination"
        @change="fetchList"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="860px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="比赛标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入比赛标题" />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.subtitle" placeholder="请输入副标题" />
        </el-form-item>
        <el-form-item label="比赛描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入比赛描述" />
        </el-form-item>
        <el-form-item label="比赛详情">
          <el-input v-model="form.content" type="textarea" :rows="5" placeholder="请输入比赛详情" />
        </el-form-item>
        <el-form-item label="主办方">
          <el-input v-model="form.organizer" placeholder="请输入主办方" />
        </el-form-item>
        <el-form-item label="比赛类型" prop="competitionType">
          <el-select v-model="form.competitionType" placeholder="请选择比赛类型" style="width: 100%">
            <el-option
              v-for="item in competitionTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="比赛级别">
          <el-select v-model="form.level" placeholder="请选择比赛级别" style="width: 100%">
            <el-option label="校级" value="school" />
            <el-option label="市级" value="city" />
            <el-option label="省级" value="province" />
            <el-option label="国家级" value="national" />
            <el-option label="国际级" value="international" />
          </el-select>
        </el-form-item>
        <el-form-item label="报名开始" prop="registrationStart">
          <el-date-picker
            v-model="form.registrationStart"
            type="datetime"
            placeholder="选择报名开始时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="报名结束" prop="registrationEnd">
          <el-date-picker
            v-model="form.registrationEnd"
            type="datetime"
            placeholder="选择报名结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="比赛开始" prop="competitionStart">
          <el-date-picker
            v-model="form.competitionStart"
            type="datetime"
            placeholder="选择比赛开始时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="比赛结束" prop="competitionEnd">
          <el-date-picker
            v-model="form.competitionEnd"
            type="datetime"
            placeholder="选择比赛结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="比赛地点">
          <el-input v-model="form.location" placeholder="请输入比赛地点" />
        </el-form-item>
        <el-form-item label="人数上限">
          <el-input-number v-model="form.maxParticipants" :min="1" :max="10000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="参赛学分">
          <el-input-number v-model="form.participationPoints" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="一等奖学分">
          <el-input-number v-model="form.firstPrizePoints" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="二等奖学分">
          <el-input-number v-model="form.secondPrizePoints" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="三等奖学分">
          <el-input-number v-model="form.thirdPrizePoints" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="优秀奖学分">
          <el-input-number v-model="form.excellentPrizePoints" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="封面图片">
          <div class="upload-row">
            <el-upload :show-file-list="false" :http-request="handleCoverUpload">
              <el-button type="primary" plain>上传封面</el-button>
            </el-upload>
            <el-input v-model="form.coverImage" placeholder="封面路径或上传后自动回填" />
          </div>
        </el-form-item>
        <el-form-item label="规则文件">
          <div class="upload-row">
            <el-upload :show-file-list="false" :http-request="handleRulesUpload">
              <el-button type="primary" plain>上传规则</el-button>
            </el-upload>
            <el-input v-model="form.rulesFile" placeholder="规则文件路径" />
          </div>
        </el-form-item>
        <el-form-item label="材料文件">
          <div class="upload-row">
            <el-upload :show-file-list="false" :http-request="handleMaterialsUpload">
              <el-button type="primary" plain>上传材料</el-button>
            </el-upload>
            <el-input v-model="form.materialsFile" placeholder="材料文件路径" />
          </div>
        </el-form-item>
        <el-form-item label="结果文件">
          <div class="upload-row">
            <el-upload :show-file-list="false" :http-request="handleResultsUpload">
              <el-button type="primary" plain>上传结果</el-button>
            </el-upload>
            <el-input v-model="form.resultsFile" placeholder="结果文件路径" />
          </div>
        </el-form-item>
        <el-form-item label="奖项说明">
          <el-input v-model="form.awards" type="textarea" :rows="3" placeholder="请输入奖项说明" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contactPerson" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="联系邮箱">
          <el-input v-model="form.contactEmail" placeholder="请输入联系邮箱" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="form.priority" :min="0" :max="999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="推荐展示">
          <el-switch v-model="form.isFeatured" />
        </el-form-item>
        <el-form-item label="跨租户开放">
          <el-switch v-model="form.isCrossTenant" />
        </el-form-item>
        <el-form-item label="立即发布">
          <el-switch v-model="form.isPublished" />
        </el-form-item>
        <ReviewAssignmentFields
          v-model:manager-reviewer-id="form.managerReviewerId"
          v-model:teacher-reviewer-id="form.teacherReviewerId"
        />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="teamsDialogVisible" title="报名管理" width="940px">
      <el-table :data="teamsList" v-loading="teamsLoading" stripe>
        <el-table-column prop="id" label="报名 ID" width="90" />
        <el-table-column prop="userId" label="用户 ID" width="90" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="studentId" label="学号" width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="teamName" label="队伍名称" width="160" />
        <el-table-column prop="registrationTime" label="报名时间" width="170" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'registered' ? 'success' : 'warning'">
              {{ row.status === 'registered' ? '已报名' : '已取消' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="canEdit" label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button text type="danger" size="small" @click="handleRemoveTeam(row)">取消报名</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="resultsDialogVisible" title="成绩管理" width="860px">
      <el-table :data="resultsList" v-loading="resultsLoading">
        <el-table-column prop="ranking" label="排名" width="90">
          <template #default="{ row }">
            <el-input-number v-model="row.ranking" :min="1" size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="teamName" label="队伍名称" min-width="180" />
        <el-table-column prop="score" label="得分" width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.score" :min="0" size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="award" label="奖项" width="140">
          <template #default="{ row }">
            <el-select v-model="row.award" size="small" placeholder="选择奖项">
              <el-option label="一等奖" :value="1" />
              <el-option label="二等奖" :value="2" />
              <el-option label="三等奖" :value="3" />
              <el-option label="优秀奖" :value="4" />
            </el-select>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="resultsDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveResults">保存成绩</el-button>
        <el-button v-if="canEdit" type="success" @click="saveResultsAndGrant">保存并发放学分</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useBatchDelete } from '@/utils/batchDelete'
import { Plus } from '@element-plus/icons-vue'
import {
  adminCancelCompetitionRegistration,
  createCompetition,
  deleteCompetition,
  getCompetitionList,
  getCompetitionResults,
  getRegistrationList,
  grantCompetitionCredits,
  saveCompetitionResults,
  updateCompetition
} from '@/api/competition'
import { uploadManagedFile } from '@/utils/fileUpload'
import { useUserStore } from '@/stores/user'
import ReviewAssignmentFields from '@/components/ReviewAssignmentFields.vue'

const userStore = useUserStore()
const canEdit = computed(() => !userStore.hasRole([8]))

const competitionTypeOptions = [
  { label: '学术竞赛', value: 'academic' },
  { label: '创新创业', value: 'innovation' },
  { label: '体育竞技', value: 'sports' },
  { label: '艺术比赛', value: 'art' },
  { label: '技能竞赛', value: 'skill' },
  { label: '编程竞赛', value: 'coding' },
  { label: '设计竞赛', value: 'design' }
]

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '报名中', value: 'REGISTRATION' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '进行中', value: 'ONGOING' },
  { label: '已结束', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' }
]

const typeMap = {
  academic: { label: '学术竞赛', tag: 'primary' },
  innovation: { label: '创新创业', tag: 'success' },
  sports: { label: '体育竞技', tag: 'warning' },
  art: { label: '艺术比赛', tag: 'danger' },
  skill: { label: '技能竞赛', tag: 'info' },
  coding: { label: '编程竞赛', tag: 'primary' },
  design: { label: '设计竞赛', tag: 'warning' }
}

const statusMap = {
  DRAFT: { label: '草稿', tag: 'info' },
  PUBLISHED: { label: '已发布', tag: 'success' },
  REGISTRATION: { label: '报名中', tag: 'primary' },
  ONGOING: { label: '进行中', tag: 'warning' },
  COMPLETED: { label: '已结束', tag: 'danger' },
  CANCELLED: { label: '已取消', tag: 'info' }
}

const loading = ref(false)
const dataList = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增比赛')
const formRef = ref()
const teamsDialogVisible = ref(false)
const teamsLoading = ref(false)
const teamsList = ref([])
const resultsDialogVisible = ref(false)
const resultsLoading = ref(false)
const resultsList = ref([])
const currentCompetitionId = ref(null)

const searchForm = reactive({
  keyword: '',
  competitionType: '',
  status: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const form = reactive({
  id: null,
  title: '',
  subtitle: '',
  description: '',
  content: '',
  competitionType: 'academic',
  level: 'school',
  organizer: '',
  location: '',
  registrationStart: null,
  registrationEnd: null,
  competitionStart: null,
  competitionEnd: null,
  maxParticipants: 100,
  coverImage: '',
  rulesFile: '',
  materialsFile: '',
  resultsFile: '',
  awards: '',
  participationPoints: 0,
  firstPrizePoints: 0,
  secondPrizePoints: 0,
  thirdPrizePoints: 0,
  excellentPrizePoints: 0,
  contactPerson: '',
  contactPhone: '',
  contactEmail: '',
  isFeatured: false,
  isCrossTenant: false,
  isPublished: false,
  priority: 0,
  managerReviewerId: null,
  teacherReviewerId: null
})

const parseDateTimeValue = (value) => {
  if (!value) return null
  const time = new Date(value).getTime()
  return Number.isNaN(time) ? null : time
}

const validateLaterThan = (startField, message) => ({
  validator: (_rule, value, callback) => {
    if (!value || !form[startField]) {
      callback()
      return
    }
    const startTime = parseDateTimeValue(form[startField])
    const endTime = parseDateTimeValue(value)
    if (startTime == null || endTime == null || endTime > startTime) {
      callback()
      return
    }
    callback(new Error(message))
  },
  trigger: 'change'
})

const rules = {
  title: [{ required: true, message: '请输入比赛标题', trigger: 'blur' }],
  competitionType: [{ required: true, message: '请选择比赛类型', trigger: 'change' }],
  registrationStart: [{ required: true, message: '请选择报名开始时间', trigger: 'change' }],
  registrationEnd: [
    { required: true, message: '请选择报名结束时间', trigger: 'change' },
    validateLaterThan('registrationStart', '报名结束时间必须晚于报名开始时间')
  ],
  competitionStart: [{ required: true, message: '请选择比赛开始时间', trigger: 'change' }],
  competitionEnd: [
    { required: true, message: '请选择比赛结束时间', trigger: 'change' },
    validateLaterThan('competitionStart', '比赛结束时间必须晚于比赛开始时间')
  ]
}

rules.managerReviewerId = [{ required: true, message: '请选择负责人审批人', trigger: 'change' }]
rules.teacherReviewerId = [{ required: true, message: '请选择指导老师审批人', trigger: 'change' }]

const resetForm = () => {
  Object.assign(form, {
    id: null,
    title: '',
    subtitle: '',
    description: '',
    content: '',
    competitionType: 'academic',
    level: 'school',
    organizer: '',
    location: '',
    registrationStart: null,
    registrationEnd: null,
    competitionStart: null,
    competitionEnd: null,
    maxParticipants: 100,
    coverImage: '',
    rulesFile: '',
    materialsFile: '',
    resultsFile: '',
    awards: '',
    participationPoints: 0,
    firstPrizePoints: 0,
    secondPrizePoints: 0,
    thirdPrizePoints: 0,
    excellentPrizePoints: 0,
    contactPerson: '',
    contactPhone: '',
    contactEmail: '',
    isFeatured: false,
    isCrossTenant: false,
    isPublished: false,
    priority: 0,
    managerReviewerId: null,
    teacherReviewerId: null
  })
}

const isGrantableCompetition = (row) => row.status === 'COMPLETED'

const fetchList = async () => {
  loading.value = true
  try {
    const response = await getCompetitionList()
    let data = response.data || []
    if (searchForm.keyword) {
      data = data.filter((item) =>
        item.title?.includes(searchForm.keyword) || item.description?.includes(searchForm.keyword)
      )
    }
    if (searchForm.competitionType) {
      data = data.filter((item) => item.competitionType === searchForm.competitionType)
    }
    if (searchForm.status) {
      data = data.filter((item) => item.status === searchForm.status)
    }
    pagination.total = data.length
    const start = (pagination.page - 1) * pagination.size
    const end = start + pagination.size
    dataList.value = data.slice(start, end)
  } catch (error) {
    console.error('fetch competition list failed', error)
    dataList.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.competitionType = ''
  searchForm.status = ''
  pagination.page = 1
  fetchList()
}

const handleAdd = () => {
  dialogTitle.value = '新增比赛'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑比赛'
  Object.assign(form, {
    id: row.id,
    title: row.title || '',
    subtitle: row.subtitle || '',
    description: row.description || '',
    content: row.content || '',
    competitionType: row.competitionType || 'academic',
    level: row.level || 'school',
    organizer: row.organizer || '',
    location: row.location || '',
    registrationStart: row.registrationStart || null,
    registrationEnd: row.registrationEnd || null,
    competitionStart: row.competitionStart || null,
    competitionEnd: row.competitionEnd || null,
    maxParticipants: row.maxParticipants || 100,
    coverImage: row.coverImage || '',
    rulesFile: row.rulesFile || '',
    materialsFile: row.materialsFile || '',
    resultsFile: row.resultsFile || '',
    awards: row.awards || '',
    participationPoints: row.participationPoints ?? 0,
    firstPrizePoints: row.firstPrizePoints ?? 0,
    secondPrizePoints: row.secondPrizePoints ?? 0,
    thirdPrizePoints: row.thirdPrizePoints ?? 0,
    excellentPrizePoints: row.excellentPrizePoints ?? 0,
    contactPerson: row.contactPerson || '',
    contactPhone: row.contactPhone || '',
    contactEmail: row.contactEmail || '',
    isFeatured: Boolean(row.isFeatured),
    isCrossTenant: Boolean(row.isCrossTenant),
    isPublished: Boolean(row.isPublished),
    priority: row.priority || 0,
    managerReviewerId: row.managerReviewerId || null,
    teacherReviewerId: row.teacherReviewerId || null
  })
  dialogVisible.value = true
}

const handleViewTeams = async (row) => {
  currentCompetitionId.value = row.id
  teamsDialogVisible.value = true
  teamsLoading.value = true
  try {
    const response = await getRegistrationList(row.id)
    teamsList.value = response.data || []
  } catch (error) {
    console.error('fetch competition registrations failed', error)
    teamsList.value = []
  } finally {
    teamsLoading.value = false
  }
}

const handleRemoveTeam = async (row) => {
  try {
    await ElMessageBox.confirm('确定取消该用户的比赛报名吗？', '提示', { type: 'warning' })
    await adminCancelCompetitionRegistration(currentCompetitionId.value, row.id, '后台取消报名')
    ElMessage.success('取消报名成功')
    const response = await getRegistrationList(currentCompetitionId.value)
    teamsList.value = response.data || []
    fetchList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '取消报名失败')
    }
  }
}

const handleViewResults = async (row) => {
  currentCompetitionId.value = row.id
  resultsDialogVisible.value = true
  resultsLoading.value = true
  try {
    const response = await getCompetitionResults(row.id)
    resultsList.value = (response.data || []).map((item, index) => ({
      ...item,
      ranking: item.ranking || index + 1
    }))
  } catch (error) {
    console.error('fetch competition results failed', error)
    resultsList.value = []
    ElMessage.error(error.message || '获取成绩失败')
  } finally {
    resultsLoading.value = false
  }
}

const persistResults = async () => {
  const payload = resultsList.value.map((item) => ({
    participantId: item.id,
    score: item.score,
    award: item.award,
    ranking: item.ranking
  }))
  return saveCompetitionResults(currentCompetitionId.value, payload)
}

const saveResults = async () => {
  try {
    await persistResults()
    ElMessage.success('成绩保存成功')
    resultsDialogVisible.value = false
  } catch (error) {
    console.error('save competition results failed', error)
    ElMessage.error(error.message || '保存成绩失败')
  }
}

const saveResultsAndGrant = async () => {
  try {
    await persistResults()
    const grantResponse = await grantCompetitionCredits(currentCompetitionId.value)
    ElMessage.success(grantResponse.data || grantResponse.msg || '成绩保存并发放学分成功')
    resultsDialogVisible.value = false
  } catch (error) {
    console.error('save and grant competition credits failed', error)
    ElMessage.error(error.message || '保存成绩或发放学分失败')
  }
}

const handleGrantCredits = async (row) => {
  if (!isGrantableCompetition(row)) {
    ElMessage.warning('比赛结束后才能发放学分')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定按比赛“${row.title}”当前学分配置发放成绩学分吗？`,
      '发放学分',
      { type: 'warning' }
    )
    const response = await grantCompetitionCredits(row.id)
    ElMessage.success(response.data || response.msg || '比赛学分发放成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '比赛学分发放失败')
    }
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除比赛“${row.title}”吗？`, '提示', { type: 'warning' })
    await deleteCompetition(row.id)
    ElMessage.success('删除成功')
    fetchList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const handleManagedUpload = async (options, type, field, successMessage) => {
  try {
    const { storedValue, url } = await uploadManagedFile(options, type)
    form[field] = storedValue
    options.onSuccess?.({ code: 200, data: { path: storedValue, url } })
    ElMessage.success(successMessage)
  } catch (error) {
    options.onError?.(error)
    ElMessage.error(error.message || `${successMessage}失败`)
  }
}

const handleCoverUpload = (options) => handleManagedUpload(options, 'competition-cover', 'coverImage', '封面上传成功')
const handleRulesUpload = (options) => handleManagedUpload(options, 'competition-rules', 'rulesFile', '规则上传成功')
const handleMaterialsUpload = (options) => handleManagedUpload(options, 'competition-materials', 'materialsFile', '材料上传成功')
const handleResultsUpload = (options) => handleManagedUpload(options, 'competition-results', 'resultsFile', '结果上传成功')

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    loading.value = true
    const submitData = {
      title: form.title,
      subtitle: form.subtitle,
      description: form.description,
      content: form.content,
      competitionType: form.competitionType,
      level: form.level,
      organizer: form.organizer,
      location: form.location,
      registrationStart: form.registrationStart,
      registrationEnd: form.registrationEnd,
      competitionStart: form.competitionStart,
      competitionEnd: form.competitionEnd,
      maxParticipants: form.maxParticipants,
      coverImage: form.coverImage,
      rulesFile: form.rulesFile,
      materialsFile: form.materialsFile,
      resultsFile: form.resultsFile,
      awards: form.awards,
      participationPoints: form.participationPoints,
      firstPrizePoints: form.firstPrizePoints,
      secondPrizePoints: form.secondPrizePoints,
      thirdPrizePoints: form.thirdPrizePoints,
      excellentPrizePoints: form.excellentPrizePoints,
      contactPerson: form.contactPerson,
      contactPhone: form.contactPhone,
      contactEmail: form.contactEmail,
      isFeatured: form.isFeatured,
      isCrossTenant: form.isCrossTenant,
      isPublished: form.isPublished,
      priority: form.priority,
      managerReviewerId: form.managerReviewerId,
      teacherReviewerId: form.teacherReviewerId
    }

    if (form.id) {
      await updateCompetition(form.id, { ...submitData, id: form.id })
      ElMessage.success('比赛更新成功')
    } else {
      await createCompetition(submitData)
      ElMessage.success('比赛创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } catch (error) {
    if (error !== false) {
      console.error('submit competition failed', error)
      ElMessage.error(error.message || '保存比赛失败')
    }
  } finally {
    loading.value = false
  }
}


const { selectedRows, handleSelectionChange, handleBatchDelete } = useBatchDelete({
  entityName: '比赛',
  deleteItem: (id) => deleteCompetition(id),
  fetchList: fetchList
})

onMounted(fetchList)
</script>

<style lang="scss" scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: #6b7280;
}

.search-form {
  margin-bottom: 20px;
  padding: 16px;
  border-radius: 10px;
  background: #f8fafc;
}

.credit-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  font-size: 12px;
  color: #475569;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}

.upload-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}
</style>
