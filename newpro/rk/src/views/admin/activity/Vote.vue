<template>
  <div class="vote-page">
    <div class="page-header">
      <div>
        <h2>活动投票</h2>
        <p>投票会绑定当前租户活动，默认通知该活动报名人员，也可以附加其他用户ID。</p>
      </div>
      <div class="header-actions">
        <el-select v-model="activityFilter" clearable filterable placeholder="按活动筛选" style="width: 260px;" @change="loadVotes">
          <el-option v-for="activity in activities" :key="activity.id" :label="activity.activityName" :value="activity.id" />
        </el-select>
        <el-button @click="loadAll" :loading="loading">刷新</el-button>
        <el-button type="primary" @click="openCreateDialog">创建投票</el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="8" v-for="vote in votes" :key="vote.id">
        <el-card class="vote-card" shadow="hover">
          <template #header>
            <div class="vote-title-row">
              <span>{{ vote.title }}</span>
              <el-tag :type="vote.status === 1 ? 'success' : 'info'" size="small">
                {{ vote.status === 1 ? '进行中' : '已结束' }}
              </el-tag>
            </div>
          </template>
          <p class="vote-activity">{{ vote.activityName || `活动 ${vote.activityId}` }}</p>
          <p class="vote-desc">{{ vote.description || '暂无描述' }}</p>
          <div v-for="opt in vote.options" :key="opt.id" class="vote-option">
            <div class="option-label">
              <span>{{ opt.label }}</span>
              <span class="option-count">{{ opt.count || 0 }} 票 ({{ calcPercent(vote, opt) }}%)</span>
            </div>
            <el-progress :percentage="calcPercent(vote, opt)" :stroke-width="8" :show-text="false" />
          </div>
          <div class="vote-footer">
            <span class="vote-total">目标 {{ vote.targetCount || 0 }} 人 / 共 {{ vote.totalVotes || 0 }} 票</span>
            <el-button size="small" @click="closeVote(vote)" v-if="vote.status === 1">结束投票</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="votes.length === 0 && !loading" description="暂无投票" />

    <el-dialog v-model="showCreateDialog" title="创建投票" width="660px">
      <el-form :model="newVote" label-width="110px">
        <el-form-item label="绑定活动">
          <el-select v-model="newVote.activityId" filterable placeholder="选择当前租户活动" style="width: 100%;" @change="loadRegistrations">
            <el-option v-for="activity in activities" :key="activity.id" :label="activity.activityName" :value="activity.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="报名人员">
          <el-tag v-if="registrationCount > 0" type="success">{{ registrationCount }} 人将收到通知</el-tag>
          <el-tag v-else type="info">当前活动暂无报名人员</el-tag>
        </el-form-item>
        <el-form-item label="附加用户ID">
          <el-select
            v-model="newVote.extraUserIds"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="输入用户ID后回车，可多个"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="投票标题">
          <el-input v-model="newVote.title" placeholder="输入投票标题" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="投票描述">
          <el-input v-model="newVote.description" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="选项">
          <div v-for="(_, i) in newVote.options" :key="i" class="option-editor-row">
            <el-input v-model="newVote.options[i]" :placeholder="`选项 ${i + 1}`" />
            <el-button text @click="newVote.options.splice(i, 1)" :disabled="newVote.options.length <= 2">删除</el-button>
          </div>
          <el-button size="small" @click="newVote.options.push('')">添加选项</el-button>
        </el-form-item>
        <el-form-item label="发送通知">
          <el-switch v-model="newVote.notifyUsers" active-text="创建后通知报名人员和附加人员" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="createVote">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  closeActivityVote,
  createActivityVote,
  getActivityVotes,
  getVoteActivities,
  getVoteActivityRegistrations
} from '@/api/activity'

const loading = ref(false)
const creating = ref(false)
const votes = ref([])
const activities = ref([])
const registrations = ref([])
const activityFilter = ref(null)
const showCreateDialog = ref(false)
const registrationCount = ref(0)
const newVote = ref({
  activityId: null,
  title: '',
  description: '',
  options: ['', ''],
  extraUserIds: [],
  notifyUsers: true
})

function calcPercent(vote, opt) {
  const total = vote.totalVotes || 0
  return total === 0 ? 0 : Math.round(((opt.count || 0) / total) * 100)
}

async function loadActivities() {
  const res = await getVoteActivities()
  activities.value = res.data || []
}

async function loadVotes() {
  loading.value = true
  try {
    const res = await getActivityVotes({ activityId: activityFilter.value || undefined })
    votes.value = res.data || []
  } catch {
    votes.value = []
    ElMessage.error('投票列表加载失败')
  } finally {
    loading.value = false
  }
}

async function loadAll() {
  await loadActivities()
  await loadVotes()
}

function openCreateDialog() {
  newVote.value = {
    activityId: activityFilter.value || null,
    title: '',
    description: '',
    options: ['', ''],
    extraUserIds: [],
    notifyUsers: true
  }
  registrationCount.value = 0
  showCreateDialog.value = true
  if (newVote.value.activityId) {
    loadRegistrations(newVote.value.activityId)
  }
}

async function loadRegistrations(activityId) {
  registrationCount.value = 0
  registrations.value = []
  if (!activityId) return
  try {
    const res = await getVoteActivityRegistrations(activityId)
    registrations.value = res.data || []
    registrationCount.value = registrations.value.length
  } catch {
    ElMessage.error('报名人员加载失败')
  }
}

async function createVote() {
  if (!newVote.value.activityId) return ElMessage.warning('请选择活动')
  if (!newVote.value.title.trim()) return ElMessage.warning('请输入标题')
  const opts = newVote.value.options.filter((o) => o.trim())
  if (opts.length < 2) return ElMessage.warning('至少需要 2 个选项')
  creating.value = true
  try {
    await createActivityVote({
      activityId: newVote.value.activityId,
      title: newVote.value.title,
      description: newVote.value.description,
      options: opts,
      extraUserIds: newVote.value.extraUserIds.map((item) => Number(item)).filter((item) => Number.isFinite(item)),
      notifyUsers: newVote.value.notifyUsers
    })
    showCreateDialog.value = false
    ElMessage.success('投票创建成功')
    await loadVotes()
  } catch {
    ElMessage.error('投票创建失败')
  } finally {
    creating.value = false
  }
}

async function closeVote(vote) {
  try {
    await ElMessageBox.confirm('确定结束该投票吗？', '提示', { type: 'warning' })
    await closeActivityVote(vote.id)
    ElMessage.success('投票已结束')
    loadVotes()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('结束投票失败')
    }
  }
}

onMounted(loadAll)
</script>

<style scoped>
.vote-page { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 20px; }
.page-header h2 { margin: 0 0 6px; }
.page-header p { margin: 0; color: #909399; }
.header-actions { display: flex; gap: 10px; flex-wrap: wrap; justify-content: flex-end; }
.vote-card { margin-bottom: 16px; }
.vote-title-row { display: flex; justify-content: space-between; align-items: center; gap: 10px; }
.vote-activity { color: #409eff; font-size: 13px; margin: 0 0 8px; }
.vote-desc { color: #909399; font-size: 13px; margin: 0 0 12px; min-height: 20px; }
.vote-option { margin-bottom: 12px; }
.option-label { display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 4px; gap: 10px; }
.option-count { color: #909399; white-space: nowrap; }
.vote-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 12px; gap: 10px; }
.vote-total { font-size: 12px; color: #909399; }
.option-editor-row { display: flex; gap: 8px; margin-bottom: 8px; width: 100%; }
@media (max-width: 900px) {
  .page-header { flex-direction: column; }
  .header-actions,
  .header-actions :deep(.el-select),
  .header-actions :deep(.el-button) { width: 100%; }
}
</style>
