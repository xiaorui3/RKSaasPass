<template>
  <div class="batch-toolbar" v-if="selectedIds.length > 0">
    <div class="toolbar-info">
      <el-icon><InfoFilled /></el-icon>
      已选择 <strong>{{ selectedIds.length }}</strong> 项
    </div>
    <div class="toolbar-actions">
      <el-button size="small" @click="$emit('clear')">取消选择</el-button>
      <slot name="actions">
        <el-button size="small" type="danger" @click="$emit('batch-delete')">批量删除</el-button>
        <el-button size="small" type="warning" @click="$emit('batch-export')">导出选中</el-button>
      </slot>
    </div>
  </div>
</template>

<script setup>
import { InfoFilled } from '@element-plus/icons-vue'
defineProps({ selectedIds: { type: Array, default: () => [] } })
defineEmits(['clear', 'batch-delete', 'batch-export'])
</script>

<style scoped>
.batch-toolbar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 16px; background: #ecf5ff; border: 1px solid #d9ecff;
  border-radius: 4px; margin-bottom: 12px;
}
.toolbar-info { display: flex; align-items: center; gap: 8px; font-size: 14px; color: #409eff; }
.toolbar-actions { display: flex; gap: 8px; }
</style>
