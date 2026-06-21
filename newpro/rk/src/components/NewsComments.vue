<template>
  <section class="comment-section">
    <div class="comment-header">
      <div>
        <p class="comment-eyebrow">交流讨论</p>
        <h3>评论 {{ comments.length ? `(${comments.length})` : '' }}</h3>
      </div>
      <el-button text :loading="loading" @click="fetchComments">刷新</el-button>
    </div>

    <div v-if="isLoggedIn" class="comment-composer">
      <el-avatar :size="40" :src="currentUserAvatar">
        {{ currentUserInitial }}
      </el-avatar>
      <div class="composer-body">
        <el-input
          v-model="newComment"
          placeholder="写下你的想法，和大家一起讨论..."
          :rows="3"
          maxlength="1000"
          show-word-limit
          type="textarea"
        />
        <div class="composer-actions">
          <span>{{ currentUserName }}</span>
          <el-button
            type="primary"
            :loading="submitting"
            :disabled="!canSubmit"
            @click="submitComment"
          >
            发布评论
          </el-button>
        </div>
      </div>
    </div>

    <el-alert
      v-else
      type="info"
      :closable="false"
      class="login-hint"
      title="登录后可以参与评论"
    />

    <div v-loading="loading" class="comment-list">
      <article v-for="comment in comments" :key="comment.id" class="comment-item">
        <el-popover trigger="hover" placement="top-start" :width="260" popper-class="comment-user-popover">
          <template #reference>
            <el-avatar class="commenter-avatar" :size="40" :src="resolveAvatar(profileForComment(comment).avatar || comment.userAvatar)">
              {{ getInitial(profileForComment(comment).name || comment.userName) }}
            </el-avatar>
          </template>
          <div class="comment-user-card">
            <div class="comment-user-card__header">
              <el-avatar :size="42" :src="resolveAvatar(profileForComment(comment).avatar || comment.userAvatar)">
                {{ getInitial(profileForComment(comment).name || comment.userName) }}
              </el-avatar>
              <div>
                <strong>{{ profileForComment(comment).name || comment.userName || '匿名用户' }}</strong>
                <span>{{ profileForComment(comment).roleName || profileForComment(comment).position || '社团成员' }}</span>
              </div>
            </div>
            <dl>
              <div v-if="profileForComment(comment).studentId">
                <dt>学号</dt>
                <dd>{{ profileForComment(comment).studentId }}</dd>
              </div>
              <div v-if="profileForComment(comment).email">
                <dt>邮箱</dt>
                <dd>{{ profileForComment(comment).email }}</dd>
              </div>
              <div v-if="profileForComment(comment).college || profileForComment(comment).major">
                <dt>院系</dt>
                <dd>{{ [profileForComment(comment).college, profileForComment(comment).major].filter(Boolean).join(' · ') }}</dd>
              </div>
            </dl>
            <p v-if="profileForComment(comment).intro">{{ profileForComment(comment).intro }}</p>
          </div>
        </el-popover>
        <div class="comment-body">
          <div class="comment-meta">
            <el-popover trigger="hover" placement="top-start" :width="260" popper-class="comment-user-popover">
              <template #reference>
                <strong class="comment-author">{{ profileForComment(comment).name || comment.userName || '匿名用户' }}</strong>
              </template>
              <div class="comment-user-card">
                <div class="comment-user-card__header">
                  <el-avatar :size="42" :src="resolveAvatar(profileForComment(comment).avatar || comment.userAvatar)">
                    {{ getInitial(profileForComment(comment).name || comment.userName) }}
                  </el-avatar>
                  <div>
                    <strong>{{ profileForComment(comment).name || comment.userName || '匿名用户' }}</strong>
                    <span>{{ profileForComment(comment).roleName || profileForComment(comment).position || '社团成员' }}</span>
                  </div>
                </div>
                <dl>
                  <div v-if="profileForComment(comment).studentId">
                    <dt>学号</dt>
                    <dd>{{ profileForComment(comment).studentId }}</dd>
                  </div>
                  <div v-if="profileForComment(comment).email">
                    <dt>邮箱</dt>
                    <dd>{{ profileForComment(comment).email }}</dd>
                  </div>
                  <div v-if="profileForComment(comment).college || profileForComment(comment).major">
                    <dt>院系</dt>
                    <dd>{{ [profileForComment(comment).college, profileForComment(comment).major].filter(Boolean).join(' · ') }}</dd>
                  </div>
                </dl>
                <p v-if="profileForComment(comment).intro">{{ profileForComment(comment).intro }}</p>
              </div>
            </el-popover>
            <span>{{ formatTime(comment.createTime) }}</span>
            <el-tag v-if="comment.featured" size="small" type="warning">精选</el-tag>
          </div>
          <p class="comment-text">{{ comment.content }}</p>
        </div>
      </article>

      <el-empty
        v-if="!loading && comments.length === 0"
        description="暂无评论，来发布第一条讨论吧"
        :image-size="72"
      />
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { createComment, getComments } from '@/api/comments'
import { getUsersByAuthIds } from '@/api/user'
import { resolveMediaUrl } from '@/utils/mediaUrl'

const props = defineProps({
  targetType: {
    type: String,
    required: true
  },
  targetId: {
    type: [String, Number],
    required: true
  }
})

const emit = defineEmits(['submit'])
const userStore = useUserStore()

const comments = ref([])
const newComment = ref('')
const loading = ref(false)
const submitting = ref(false)
const commentProfiles = ref({})

const isLoggedIn = computed(() => userStore.isLoggedIn)
const currentUserName = computed(() => userStore.userName || userStore.userInfo?.name || '匿名用户')
const currentUserAvatar = computed(() => resolveAvatar(userStore.userInfo?.avatar || userStore.userInfo?.icon || ''))
const currentUserInitial = computed(() => getInitial(currentUserName.value))
const canSubmit = computed(() => Boolean(newComment.value.trim()) && !submitting.value)

function normalizeComments(payload) {
  if (Array.isArray(payload)) {
    return payload
  }
  if (Array.isArray(payload?.records)) {
    return payload.records
  }
  return []
}

async function fetchComments() {
  if (!props.targetType || !props.targetId) {
    comments.value = []
    return
  }
  loading.value = true
  try {
    const res = await getComments(props.targetType, props.targetId)
    const nextComments = normalizeComments(res.data)
    comments.value = nextComments
    await fetchCommentProfiles(nextComments)
  } catch (error) {
    console.error('failed to load comments:', error)
    comments.value = []
  } finally {
    loading.value = false
  }
}

async function submitComment() {
  const content = newComment.value.trim()
  if (!content) return

  submitting.value = true
  try {
    const res = await createComment({
      targetType: props.targetType,
      targetId: props.targetId,
      content,
      userId: userStore.userInfo?.userId || userStore.userInfo?.id,
      userName: currentUserName.value,
      userAvatar: userStore.userInfo?.avatar || userStore.userInfo?.icon || ''
    })
    const created = res.data
    comments.value = created ? [created, ...comments.value] : comments.value
    await fetchCommentProfiles(comments.value)
    newComment.value = ''
    emit('submit', comments.value.length)
    ElMessage.success('评论已发布')
  } catch (error) {
    console.error('failed to create comment:', error)
  } finally {
    submitting.value = false
  }
}

function getCommentIdentity(comment) {
  if (!comment) {
    return null
  }
  return comment.userId || comment.authUserId || comment.commentUserId || comment.authorId || comment.createdBy || comment.createBy || null
}

async function fetchCommentProfiles(items) {
  const ids = Array.from(new Set((items || []).map(getCommentIdentity).filter(Boolean).map((id) => Number(id)).filter(Boolean)))
    .filter((id) => !commentProfiles.value[id])
  if (!ids.length) {
    return
  }
  try {
    const res = await getUsersByAuthIds(ids)
    const next = { ...commentProfiles.value }
    for (const item of res.data || []) {
      const authId = item.authUserId || item.userId || item.id
      if (!authId) continue
      next[authId] = normalizeUserProfile(item)
    }
    commentProfiles.value = next
  } catch (error) {
    console.warn('failed to load comment user profiles:', error)
  }
}

function normalizeUserProfile(item = {}) {
  return {
    id: item.id,
    authUserId: item.authUserId || item.userId,
    name: item.name || item.realName || item.nickname || item.username || '',
    username: item.username || '',
    avatar: item.icon || item.avatar || item.photo || '',
    roleName: item.roleName || '',
    position: item.position || item.job || '',
    email: item.email || '',
    studentId: item.studentId || '',
    college: item.college || item.department || '',
    major: item.major || '',
    intro: item.intro || ''
  }
}

function profileForComment(comment) {
  const identity = getCommentIdentity(comment)
  const profile = identity ? commentProfiles.value[identity] : null
  return profile || {
    name: comment?.userName || comment?.username || '',
    avatar: comment?.userAvatar || comment?.avatar || ''
  }
}

function resolveAvatar(value) {
  return value ? resolveMediaUrl(value) : ''
}

function getInitial(value) {
  return String(value || '匿').trim().charAt(0) || '匿'
}

function formatTime(value) {
  if (!value) {
    return ''
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return String(value)
  }
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

watch(
  () => [props.targetType, props.targetId],
  () => fetchComments()
)

onMounted(() => {
  fetchComments()
})
</script>

<style scoped>
.comment-section {
  display: grid;
  gap: 18px;
  margin-top: 24px;
  padding: 24px;
  border: 1px solid rgba(34, 92, 78, 0.1);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 18px 36px rgba(24, 33, 31, 0.07);
}

.comment-header,
.composer-actions,
.comment-meta {
  display: flex;
  align-items: center;
}

.comment-header {
  justify-content: space-between;
  gap: 16px;
}

.comment-eyebrow {
  margin: 0 0 6px;
  font-size: 12px;
  font-weight: 700;
  color: #297e63;
}

.comment-header h3 {
  margin: 0;
  font-size: 20px;
  color: #17231f;
}

.comment-composer {
  display: flex;
  gap: 12px;
  padding: 16px;
  border-radius: 16px;
  background: #f7faf8;
}

.composer-body {
  flex: 1;
  min-width: 0;
}

.composer-actions {
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
  color: #66736f;
  font-size: 13px;
}

.login-hint {
  margin: 0;
}

.comment-list {
  min-height: 80px;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 18px 0;
  border-bottom: 1px solid #edf1ee;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-meta {
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 6px;
}

.comment-meta strong {
  color: #17231f;
  font-size: 14px;
}

.commenter-avatar,
.comment-author {
  cursor: pointer;
}

.comment-meta span {
  color: #8a9692;
  font-size: 12px;
}

.comment-text {
  margin: 0;
  color: #2f3b37;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.comment-user-card {
  color: #17231f;
}

.comment-user-card__header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.comment-user-card__header strong {
  display: block;
  font-size: 15px;
}

.comment-user-card__header span,
.comment-user-card dt {
  color: #6b7a86;
  font-size: 12px;
}

.comment-user-card dl {
  display: grid;
  gap: 8px;
  margin: 0;
}

.comment-user-card dl > div {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 8px;
}

.comment-user-card dd {
  margin: 0;
  min-width: 0;
  color: #263646;
  word-break: break-word;
}

.comment-user-card p {
  margin: 10px 0 0;
  color: #4d5d68;
  line-height: 1.6;
}

@media (max-width: 640px) {
  .comment-section {
    padding: 18px;
    border-radius: 16px;
  }

  .comment-composer {
    padding: 14px;
  }

  .composer-actions {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
