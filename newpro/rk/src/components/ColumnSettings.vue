<template>
  <el-popover placement="bottom" :width="200" trigger="click">
    <template #reference>
      <el-button :icon="Setting" circle size="small" title="列设置" />
    </template>
    <div class="column-settings">
      <p class="setting-title">显示列</p>
      <el-checkbox-group v-model="visibleCols">
        <div v-for="col in allColumns" :key="col.prop" class="col-item">
          <el-checkbox :label="col.prop">{{ col.label }}</el-checkbox>
        </div>
      </el-checkbox-group>
      <el-button size="small" text type="primary" @click="resetColumns" style="margin-top:8px;">恢复默认</el-button>
    </div>
  </el-popover>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Setting } from '@element-plus/icons-vue'

const props = defineProps({
  columns: { type: Array, required: true },
  storageKey: { type: String, default: '' }
})
const emit = defineEmits(['update:visible'])

const allColumns = ref(props.columns)
const defaultKeys = props.columns.map(c => c.prop)
const stored = props.storageKey ? JSON.parse(localStorage.getItem(`col-settings-${props.storageKey}`) || 'null') : null
const visibleCols = ref(stored || [...defaultKeys])

watch(visibleCols, (val) => {
  emit('update:visible', val)
  if (props.storageKey) {
    localStorage.setItem(`col-settings-${props.storageKey}`, JSON.stringify(val))
  }
}, { immediate: true })

function resetColumns() {
  visibleCols.value = [...defaultKeys]
}
</script>

<style scoped>
.setting-title { font-weight: 600; margin: 0 0 8px; }
.col-item { margin-bottom: 4px; }
</style>
