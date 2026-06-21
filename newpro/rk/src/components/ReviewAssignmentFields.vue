<template>
  <el-row :gutter="16">
    <el-col :xs="24" :sm="12">
      <el-form-item label="负责人审批人" :prop="managerProp">
        <el-select
          :model-value="managerReviewerId"
          filterable
          clearable
          :loading="loading"
          placeholder="请选择负责人审批人"
          style="width: 100%"
          @update:model-value="emit('update:managerReviewerId', normalizeId($event))"
        >
          <el-option
            v-for="item in managerOptions"
            :key="item.id"
            :label="formatUserLabel(item)"
            :value="Number(item.id)"
          />
        </el-select>
      </el-form-item>
    </el-col>
    <el-col :xs="24" :sm="12">
      <el-form-item label="指导老师审批人" :prop="teacherProp">
        <el-select
          :model-value="teacherReviewerId"
          filterable
          clearable
          :loading="loading"
          placeholder="请选择指导老师审批人"
          style="width: 100%"
          @update:model-value="emit('update:teacherReviewerId', normalizeId($event))"
        >
          <el-option
            v-for="item in teacherOptions"
            :key="item.id"
            :label="formatUserLabel(item)"
            :value="Number(item.id)"
          />
        </el-select>
      </el-form-item>
    </el-col>
  </el-row>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getUserPage } from '@/api/user'

defineProps({
  managerReviewerId: {
    type: [Number, String],
    default: null
  },
  teacherReviewerId: {
    type: [Number, String],
    default: null
  },
  managerProp: {
    type: String,
    default: 'managerReviewerId'
  },
  teacherProp: {
    type: String,
    default: 'teacherReviewerId'
  }
})

const emit = defineEmits(['update:managerReviewerId', 'update:teacherReviewerId'])

const loading = ref(false)
const managerOptions = ref([])
const teacherOptions = ref([])

const normalizeId = (value) => {
  if (value === '' || value == null) return null
  const numberValue = Number(value)
  return Number.isNaN(numberValue) ? value : numberValue
}

const normalizeRecords = (data) => data?.records || data?.list || (Array.isArray(data) ? data : [])

const loadReviewers = async (roleId) => {
  const res = await getUserPage({ page: 1, size: 500, roleId, status: 1 })
  if (res.code !== 200) return []
  return normalizeRecords(res.data).filter((item) => Number(item.roleId) === Number(roleId))
}

const formatUserLabel = (item) => {
  const name = item.name || item.realName || item.nickname || item.username || `用户${item.id}`
  const username = item.username && item.username !== name ? ` / ${item.username}` : ''
  return `${name}${username}（ID:${item.id}）`
}

onMounted(async () => {
  loading.value = true
  try {
    const [managers, teachers] = await Promise.all([loadReviewers(7), loadReviewers(8)])
    managerOptions.value = managers
    teacherOptions.value = teachers
  } catch {
    managerOptions.value = []
    teacherOptions.value = []
  } finally {
    loading.value = false
  }
})
</script>
