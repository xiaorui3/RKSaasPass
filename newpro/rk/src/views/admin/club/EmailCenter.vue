<template>
  <div class="admin-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>邮件发送中心</span>
        </div>
      </template>

      <el-tabs v-model="mainTab">
        <el-tab-pane label="发送" name="send">
          <el-form label-width="110px">
            <el-form-item label="发送类型">
              <el-radio-group v-model="sendType">
                <el-radio-button label="normal">普通邮件</el-radio-button>
                <el-radio-button label="invite-register">邀请注册</el-radio-button>
                <el-radio-button label="invite-join">邀请入社</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-form-item v-if="sendType === 'normal'" label="收件模式">
              <el-radio-group v-model="mode">
                <el-radio-button label="manual">手工邮箱</el-radio-button>
                <el-radio-button label="role">按角色</el-radio-button>
                <el-radio-button label="member">按成员</el-radio-button>
                <el-radio-button label="activity">按活动报名人</el-radio-button>
                <el-radio-button label="competition">按比赛报名人</el-radio-button>
                <el-radio-button label="mixed">混合发送</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-form-item v-if="mode !== 'role' && sendType === 'normal'" label="手工邮箱">
              <el-input
                v-model="manualEmailText"
                type="textarea"
                :rows="4"
                placeholder="请输入邮箱，支持逗号、空格或换行分隔"
              />
            </el-form-item>

            <el-form-item v-if="sendType !== 'normal'" label="邀请邮箱">
              <el-input
                v-model="manualEmailText"
                type="textarea"
                :rows="4"
                placeholder="请输入邀请邮箱，支持逗号、空格或换行分隔"
              />
            </el-form-item>

            <el-form-item v-if="sendType !== 'normal'" label="绑定内推码">
              <el-select
                v-model="selectedReferralCode"
                clearable
                filterable
                placeholder="可选，发送邀请时绑定一个内推码"
                style="width: 100%"
              >
                <el-option
                  v-for="item in referralOptions"
                  :key="item.id"
                  :label="`${item.code}（已用 ${item.usedCount || 0}/${item.maxUses || 0}）`"
                  :value="item.code"
                />
              </el-select>
            </el-form-item>

            <el-form-item v-if="sendType === 'normal' && (mode === 'role' || mode === 'mixed')" label="选择角色">
              <el-select v-model="selectedRoleIds" multiple placeholder="请选择角色" style="width: 100%">
                <el-option
                  v-for="role in roleOptions"
                  :key="role.id"
                  :label="role.name"
                  :value="role.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item v-if="sendType === 'normal' && (mode === 'member' || mode === 'mixed')" label="选择成员">
              <el-select v-model="selectedMemberIds" multiple filterable placeholder="请选择成员" style="width: 100%">
                <el-option
                  v-for="member in memberOptions"
                  :key="member.id"
                  :label="`${member.name} <${member.email || '无邮箱'}>`"
                  :value="member.id"
                  :disabled="!member.email"
                />
              </el-select>
            </el-form-item>

            <el-form-item v-if="sendType === 'normal' && mode === 'activity'" label="选择活动">
              <el-select v-model="selectedActivityId" filterable placeholder="请选择活动" style="width: 100%">
                <el-option
                  v-for="activity in activityOptions"
                  :key="activity.id"
                  :label="activity.activityName || activity.title"
                  :value="activity.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item v-if="sendType === 'normal' && mode === 'competition'" label="选择比赛">
              <el-select v-model="selectedCompetitionId" filterable placeholder="请选择比赛" style="width: 100%">
                <el-option
                  v-for="competition in competitionOptions"
                  :key="competition.id"
                  :label="competition.title"
                  :value="competition.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item v-if="sendType === 'normal' && mode !== 'manual'">
              <el-button :loading="resolving" @click="handleResolveRecipients">预览收件人</el-button>
            </el-form-item>

            <el-form-item v-if="sendType === 'normal' && resolvedRecipients.length" label="预览收件人">
              <el-table :data="resolvedRecipients" size="small" style="width: 100%">
                <el-table-column prop="username" label="用户名" min-width="140" />
                <el-table-column prop="name" label="姓名" min-width="120" />
                <el-table-column prop="email" label="邮箱" min-width="220" />
              </el-table>
            </el-form-item>

            <el-form-item label="主题">
              <el-input v-model="subject" placeholder="请输入邮件主题" />
            </el-form-item>

            <el-form-item label="正文">
              <el-input
                v-model="content"
                type="textarea"
                :rows="8"
                placeholder="请输入邮件正文"
              />
            </el-form-item>

            <el-form-item v-if="sendType === 'normal'" label="媒体工具">
              <div class="media-tools">
                <div class="media-toolbar">
                  <el-button @click="openImagePicker">插入图片</el-button>
                  <input ref="imageInputRef" aria-label="图片上传" class="sr-only" type="file" accept="image/*" @change="handleImageUploadChange" />

                  <el-button @click="openAttachmentPicker">插入附件链接</el-button>
                  <input ref="attachmentInputRef" aria-label="附件上传" class="sr-only" type="file" @change="handleAttachmentUploadChange" />
                </div>

                <div class="video-tools">
                  <el-input v-model="videoTitle" placeholder="请输入视频标题" />
                  <el-input v-model="videoLinkUrl" placeholder="请输入视频跳转链接" />
                  <el-input v-model="videoCoverUrl" placeholder="可选，输入视频封面图链接；默认使用最近上传图片" />
                  <el-button type="primary" plain @click="handleInsertVideoCard">插入视频卡片</el-button>
                </div>
              </div>
            </el-form-item>

            <el-form-item v-if="sendType === 'normal'">
              <el-checkbox v-model="htmlMode">按 HTML 发送</el-checkbox>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="sending" @click="handleSend">
                {{ sendType === 'normal' ? '发送邮件' : '发送邀请' }}
              </el-button>
              </el-form-item>

              <el-form-item v-if="sendType === 'normal' && htmlMode && content.trim()" label="HTML Preview">
                <div class="html-preview" v-html="content" />
              </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="模板" name="templates" lazy>
          <div class="template-actions">
            <el-input v-model="templateKeyword" placeholder="按模板名或编码搜索" clearable class="template-search" />
            <el-button @click="fetchTemplates">刷新</el-button>
            <el-button type="primary" @click="handleAddTemplate">新增模板</el-button>
            <el-button type="danger" plain :disabled="!selectedRows.length" @click="handleBatchDelete">批量删除</el-button>
          </div>

          <el-table :data="templateList" v-loading="templateLoading" size="small" style="width: 100%" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
            <el-table-column prop="templateName" label="模板名称" min-width="180" />
            <el-table-column prop="templateCode" label="模板编码" min-width="160" />
            <el-table-column prop="emailSubject" label="主题" min-width="200" />
            <el-table-column prop="htmlMode" label="模式" min-width="100">
              <template #default="{ row }">
                <el-tag :type="row.htmlMode ? 'success' : 'info'">
                  {{ row.htmlMode ? 'HTML' : '文本' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" min-width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'warning'">
                  {{ row.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" min-width="180" />
            <el-table-column label="操作" min-width="220" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" @click="handleApplyTemplate(row)">应用</el-button>
                <el-button text @click="handleEditTemplate(row)">编辑</el-button>
                <el-button text :type="row.status === 1 ? 'warning' : 'success'" @click="handleToggleTemplateStatus(row)">
                  {{ row.status === 1 ? '停用' : '启用' }}
                </el-button>
                <el-button text type="danger" @click="handleDeleteTemplate(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-card class="history-card">
      <template #header>
        <div class="card-header">
          <span>历史记录</span>
          <el-button text @click="refreshHistory">刷新</el-button>
        </div>
      </template>

      <el-tabs v-model="historyTab">
        <el-tab-pane label="发送历史" name="tasks">
          <el-table :data="sendTaskList" size="small" style="width: 100%">
            <el-table-column type="expand" width="48">
              <template #default="{ row }">
                <div class="send-task-content">
                  <div class="send-task-content__meta">
                    <strong>{{ row.subject }}</strong>
                    <span>租户 {{ row.tenantId || '-' }} · {{ row.html ? 'HTML' : '文本' }}</span>
                  </div>
                  <div v-if="row.html" class="html-preview history-html-preview" v-html="row.content || '暂无正文'" />
                  <pre v-else class="history-text-preview">{{ row.content || '暂无正文' }}</pre>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="subject" label="主题" min-width="220" />
            <el-table-column prop="recipientMode" label="收件模式" min-width="120" />
            <el-table-column prop="status" label="状态" min-width="100" />
            <el-table-column prop="queuedCount" label="总数" min-width="80" />
            <el-table-column prop="successCount" label="成功" min-width="80" />
            <el-table-column prop="failCount" label="失败" min-width="80" />
            <el-table-column prop="createTime" label="创建时间" min-width="180" />
            <el-table-column label="操作" min-width="120">
              <template #default="{ row }">
                <el-button text type="primary" @click="handleViewRecipients(row)">查看收件人</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="邀请历史" name="invitations">
          <el-table :data="invitationList" size="small" style="width: 100%">
            <el-table-column prop="targetEmail" label="目标邮箱" min-width="220" />
            <el-table-column prop="invitationType" label="邀请类型" min-width="120" />
            <el-table-column prop="referralCode" label="内推码" min-width="120" />
            <el-table-column prop="status" label="状态" min-width="100" />
            <el-table-column prop="conversionStatus" label="转化状态" min-width="140" />
            <el-table-column label="是否接受" min-width="100">
              <template #default="{ row }">
                <el-tag :type="row.accepted ? 'success' : 'info'">
                  {{ row.accepted ? '已接受' : '未接受' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" min-width="180" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="recipientDialogVisible" title="收件人详情" width="800px">
      <el-table :data="taskRecipientList" size="small" style="width: 100%">
        <el-table-column prop="recipientEmail" label="邮箱" min-width="220" />
        <el-table-column prop="recipientName" label="姓名" min-width="120" />
        <el-table-column prop="sourceType" label="来源" min-width="120" />
        <el-table-column prop="sendStatus" label="状态" min-width="100" />
        <el-table-column prop="errorMessage" label="错误信息" min-width="180" />
      </el-table>
    </el-dialog>

    <el-dialog v-model="templateDialogVisible" :title="templateDialogTitle" width="720px">
      <el-form label-width="110px">
        <el-form-item label="模板名称">
          <el-input v-model="templateForm.templateName" placeholder="请输入模板名称" aria-label="模板名称" />
        </el-form-item>
        <el-form-item label="模板编码">
          <el-input v-model="templateForm.templateCode" placeholder="请输入模板编码" aria-label="模板编码" />
        </el-form-item>
        <el-form-item label="主题">
          <el-input v-model="templateForm.emailSubject" placeholder="请输入主题" aria-label="主题" />
        </el-form-item>
        <el-form-item label="正文">
          <el-input
            v-model="templateForm.templateContent"
            type="textarea"
            :rows="8"
            placeholder="请输入正文"
            aria-label="正文"
          />
        </el-form-item>
        <el-form-item label="HTML模式">
          <el-switch v-model="templateForm.htmlMode" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="templateForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="templateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveTemplate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useBatchDelete } from '@/utils/batchDelete'
import { getRoleList } from '@/api/role'
import { getMembersList } from '@/api/member'
import { getActivityList } from '@/api/activity'
import { getCompetitionList } from '@/api/competition'
import { getReferralCodes } from '@/api/referral'
import { uploadManagedFile } from '@/utils/fileUpload'
import {
  createEmailTemplate,
  deleteEmailTemplate,
  listEmailInvitations,
  listEmailSendTasks,
  listEmailTaskRecipients,
  listEmailTemplates,
  resolveEmailRecipients,
  sendInvitationEmail,
  sendManagedEmail,
  updateEmailTemplate,
  updateEmailTemplateStatus
} from '@/api/email'

const mainTab = ref('send')
const sendType = ref('normal')
const mode = ref('manual')
const historyTab = ref('tasks')
const roleOptions = ref([])
const memberOptions = ref([])
const activityOptions = ref([])
const competitionOptions = ref([])
const referralOptions = ref([])
const selectedRoleIds = ref([])
const selectedMemberIds = ref([])
const selectedActivityId = ref(null)
const selectedCompetitionId = ref(null)
const selectedReferralCode = ref('')
const resolvedRecipients = ref([])
const invitationList = ref([])
const sendTaskList = ref([])
const taskRecipientList = ref([])
const recipientDialogVisible = ref(false)
const manualEmailText = ref('')
const subject = ref('')
const content = ref('')
const htmlMode = ref(false)
const videoTitle = ref('')
const videoLinkUrl = ref('')
const videoCoverUrl = ref('')
const lastImageUrl = ref('')
const imageInputRef = ref(null)
const attachmentInputRef = ref(null)
const resolving = ref(false)
const sending = ref(false)

const templateList = ref([])
const templateLoading = ref(false)
const templateKeyword = ref('')
const templateDialogVisible = ref(false)
const templateDialogTitle = ref('新增模板')
const templateForm = ref({
  id: null,
  templateName: '',
  templateCode: '',
  emailSubject: '',
  templateContent: '',
  htmlMode: true,
  status: 1
})

const manualEmails = computed(() =>
  manualEmailText.value
    .split(/[\n,;\s]+/)
    .map((item) => item.trim())
    .filter(Boolean)
)

const selectedMemberRecipients = computed(() =>
  memberOptions.value
    .filter((member) => selectedMemberIds.value.includes(member.id) && member.email)
    .map((member) => ({
      username: member.studentId || '',
      name: member.name,
      email: member.email
    }))
)

function resetTemplateForm() {
  templateForm.value = {
    id: null,
    templateName: '',
    templateCode: '',
    emailSubject: '',
    templateContent: '',
    htmlMode: true,
    status: 1
  }
}

function useTemplate(template) {
  subject.value = template.emailSubject || ''
  content.value = template.templateContent || ''
  htmlMode.value = !!template.htmlMode
  mainTab.value = 'send'
  ElMessage.success(`已应用模板：${template.templateName}`)
}

function appendToContent(snippet) {
  htmlMode.value = true
  content.value = content.value ? `${content.value}\n${snippet}` : snippet
}

function openImagePicker() {
  imageInputRef.value?.click()
}

function openAttachmentPicker() {
  attachmentInputRef.value?.click()
}

const fetchRoles = async () => {
  const res = await getRoleList()
  roleOptions.value = res.data || []
}

const fetchMembers = async () => {
  const res = await getMembersList()
  const raw = res.data || []
  memberOptions.value = raw
    .filter((item) => (item.agreeStatus === undefined ? true : item.agreeStatus === 1))
    .map((item) => ({
      id: item.id,
      name: item.name || '',
      studentId: item.studentId || '',
      email: item.email || ''
    }))
}

const fetchActivities = async () => {
  const res = await getActivityList()
  activityOptions.value = res.data || []
}

const fetchCompetitions = async () => {
  const res = await getCompetitionList()
  competitionOptions.value = res.data || []
}

const fetchReferralOptions = async () => {
  const res = await getReferralCodes({
    page: 1,
    size: 100,
    status: 1
  })
  referralOptions.value = res.data?.records || []
}

const fetchInvitations = async () => {
  const res = await listEmailInvitations()
  invitationList.value = res.data || []
}

const fetchSendTasks = async () => {
  const res = await listEmailSendTasks()
  sendTaskList.value = res.data || []
}

const fetchTemplates = async () => {
  templateLoading.value = true
  try {
    const res = await listEmailTemplates({
      pageNo: 1,
      pageSize: 100,
      keyword: templateKeyword.value || undefined
    })
    templateList.value = res.data?.list || []
  } catch (error) {
    ElMessage.error(error.message || '获取模板失败')
  } finally {
    templateLoading.value = false
  }
}

const refreshHistory = async () => {
  await fetchSendTasks()
  await fetchInvitations()
}

const handleResolveRecipients = async () => {
  resolving.value = true
  try {
    let recipients = []

    if ((mode.value === 'role' || mode.value === 'mixed') && selectedRoleIds.value.length) {
      const res = await resolveEmailRecipients({ roleIds: selectedRoleIds.value })
      recipients = recipients.concat(res.data || [])
    }

    if (mode.value === 'member' || mode.value === 'mixed') {
      recipients = recipients.concat(selectedMemberRecipients.value)
    }

    if (mode.value === 'activity' && selectedActivityId.value) {
      const res = await resolveEmailRecipients({ activityId: selectedActivityId.value })
      recipients = recipients.concat(res.data || [])
    }

    if (mode.value === 'competition' && selectedCompetitionId.value) {
      const res = await resolveEmailRecipients({ competitionId: selectedCompetitionId.value })
      recipients = recipients.concat(res.data || [])
    }

    const merged = new Map()
    for (const item of recipients) {
      if (item.email) {
        merged.set(item.email, item)
      }
    }
    resolvedRecipients.value = Array.from(merged.values())
    ElMessage.success(`已解析 ${resolvedRecipients.value.length} 个收件人`)
  } catch (error) {
    ElMessage.error(error.message || '收件人解析失败')
  } finally {
    resolving.value = false
  }
}

const handleSend = async () => {
  if (!subject.value.trim()) {
    ElMessage.warning('请输入邮件主题')
    return
  }
  if (!content.value.trim()) {
    ElMessage.warning('请输入邮件正文')
    return
  }

  sending.value = true
  try {
    if (sendType.value === 'normal') {
      const recipientMap = new Map()
      manualEmails.value.forEach((email) => recipientMap.set(email.toLowerCase(), email))

      if (mode.value !== 'manual' && !resolvedRecipients.value.length) {
        await handleResolveRecipients()
      }

      resolvedRecipients.value.forEach((item) => {
        if (item.email) recipientMap.set(item.email.toLowerCase(), item.email)
      })

      const res = await sendManagedEmail({
        roleIds: mode.value === 'role' || mode.value === 'mixed' ? selectedRoleIds.value : [],
        activityId: mode.value === 'activity' ? selectedActivityId.value : null,
        competitionId: mode.value === 'competition' ? selectedCompetitionId.value : null,
        manualEmails: Array.from(recipientMap.values()),
        subject: subject.value,
        content: content.value,
        html: htmlMode.value
      })
      ElMessage.success(`发送成功，已加入队列 ${res.data?.queuedCount || 0} 封`)
      await fetchSendTasks()
    } else {
      const invitationType = sendType.value === 'invite-join' ? 'JOIN' : 'REGISTER'
      const res = await sendInvitationEmail({
        manualEmails: manualEmails.value,
        invitationType,
        subject: subject.value,
        content: content.value,
        referralCode: selectedReferralCode.value || null
      })
      ElMessage.success(`邀请发送成功，已加入队列 ${res.data?.queuedCount || 0} 封`)
      await fetchInvitations()
    }
  } catch (error) {
    ElMessage.error(error.message || '邮件发送失败')
  } finally {
    sending.value = false
  }
}

const handleImageUploadChange = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  try {
    const { url } = await uploadManagedFile({ file }, 'email-image')
    if (!url) {
      throw new Error('图片上传结果缺少地址')
    }
    lastImageUrl.value = url
    if (!videoCoverUrl.value) {
      videoCoverUrl.value = url
    }
    appendToContent(`<p><img src="${url}" alt="${file.name}" style="max-width: 100%; border-radius: 8px;" /></p>`)
    ElMessage.success('图片已插入')
  } catch (error) {
    ElMessage.error(error.message || '图片上传失败')
  } finally {
    event.target.value = ''
  }
}

const handleInsertVideoCard = () => {
  if (!videoTitle.value.trim()) {
    ElMessage.warning('请输入视频标题')
    return
  }
  if (!videoLinkUrl.value.trim()) {
    ElMessage.warning('请输入视频跳转链接')
    return
  }
  const cover = videoCoverUrl.value || lastImageUrl.value || ''
  const snippet = `
<div style="border:1px solid #dfe6ec;border-radius:12px;padding:16px;margin:12px 0;">
  ${cover ? `<img src="${cover}" alt="${videoTitle.value}" style="width:100%;max-width:480px;border-radius:10px;display:block;margin-bottom:12px;" />` : ''}
  <h3 style="margin:0 0 8px 0;">${videoTitle.value}</h3>
  <a href="${videoLinkUrl.value}" target="_blank" style="display:inline-block;padding:10px 16px;background:#1f6feb;color:#fff;text-decoration:none;border-radius:8px;">点击观看</a>
</div>`.trim()
  appendToContent(snippet)
  ElMessage.success('视频卡片已插入')
}

const handleAttachmentUploadChange = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  try {
    const { url } = await uploadManagedFile({ file }, 'email-attachment')
    if (!url) {
      throw new Error('附件上传结果缺少地址')
    }
    appendToContent(`<p><a href="${url}" target="_blank">下载附件：${file.name}</a></p>`)
    ElMessage.success('附件链接已插入')
  } catch (error) {
    ElMessage.error(error.message || '附件上传失败')
  } finally {
    event.target.value = ''
  }
}

const handleViewRecipients = async (task) => {
  const res = await listEmailTaskRecipients(task.id)
  taskRecipientList.value = res.data || []
  recipientDialogVisible.value = true
}

function handleAddTemplate() {
  resetTemplateForm()
  templateDialogTitle.value = '新增模板'
  templateDialogVisible.value = true
}

function handleEditTemplate(row) {
  templateDialogTitle.value = '编辑模板'
  templateForm.value = {
    id: row.id,
    templateName: row.templateName,
    templateCode: row.templateCode,
    emailSubject: row.emailSubject,
    templateContent: row.templateContent,
    htmlMode: !!row.htmlMode,
    status: row.status
  }
  templateDialogVisible.value = true
}

async function handleSaveTemplate() {
  if (!templateForm.value.templateName?.trim()) {
    ElMessage.warning('请输入模板名称')
    return
  }
  if (!templateForm.value.templateCode?.trim()) {
    ElMessage.warning('请输入模板编码')
    return
  }
  if (!templateForm.value.emailSubject?.trim()) {
    ElMessage.warning('请输入主题')
    return
  }
  if (!templateForm.value.templateContent?.trim()) {
    ElMessage.warning('请输入正文')
    return
  }

  const payload = {
    templateName: templateForm.value.templateName,
    templateCode: templateForm.value.templateCode,
    emailSubject: templateForm.value.emailSubject,
    templateContent: templateForm.value.templateContent,
    htmlMode: templateForm.value.htmlMode,
    status: templateForm.value.status
  }

  if (templateForm.value.id) {
    await updateEmailTemplate(templateForm.value.id, payload)
    ElMessage.success('模板已更新')
  } else {
    await createEmailTemplate(payload)
    ElMessage.success('模板已创建')
  }

  templateDialogVisible.value = false
  await fetchTemplates()
}

async function handleDeleteTemplate(row) {
  await ElMessageBox.confirm(`确定删除模板 ${row.templateName} 吗？`, '删除模板', {
    type: 'warning'
  })
  await deleteEmailTemplate(row.id)
  ElMessage.success('模板已删除')
  await fetchTemplates()
}

async function handleToggleTemplateStatus(row) {
  const nextStatus = row.status === 1 ? 0 : 1
  await updateEmailTemplateStatus(row.id, nextStatus)
  ElMessage.success(`模板已${nextStatus === 1 ? '启用' : '停用'}`)
  await fetchTemplates()
}

function handleApplyTemplate(row) {
  useTemplate(row)
}


const { selectedRows, handleSelectionChange, handleBatchDelete } = useBatchDelete({
  entityName: '邮件模板',
  deleteItem: (id) => deleteEmailTemplate(id),
  fetchList: fetchTemplates
})

onMounted(async () => {
  await fetchRoles()
  await fetchMembers()
  await fetchActivities()
  await fetchCompetitions()
  await fetchReferralOptions()
  await fetchTemplates()
  await refreshHistory()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.history-card {
  margin-top: 20px;
}

.template-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
  width: 100%;
}

.template-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.template-search {
  max-width: 320px;
}

.media-tools {
  display: grid;
  gap: 16px;
  width: 100%;
}

.media-toolbar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.video-tools {
  display: grid;
  gap: 12px;
}

.html-preview {
  width: 100%;
  min-height: 120px;
  padding: 16px;
  border: 1px solid #dcdfe6;
  border-radius: 12px;
  background: #fafafa;
  color: #303133;
  overflow-wrap: anywhere;
}

.html-preview :deep(img) {
  max-width: 100%;
  height: auto;
}

.send-task-content {
  padding: 12px 16px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.send-task-content__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  color: #303133;
}

.send-task-content__meta span {
  color: #909399;
  font-size: 12px;
}

.history-html-preview {
  max-height: none;
  overflow: visible;
  background: #fff;
}

.history-text-preview {
  margin: 0;
  padding: 12px;
  white-space: pre-wrap;
  word-break: break-word;
  color: #303133;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  font-family: inherit;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
</style>
