<template>
  <el-card class="admin-panel-card" shadow="never">
    <template v-if="title || description || $slots.headerActions" #header>
      <div class="panel-header">
        <div class="panel-copy">
          <h2 v-if="title" class="panel-title">{{ title }}</h2>
          <p v-if="description" class="panel-description">{{ description }}</p>
        </div>
        <div v-if="$slots.headerActions" class="panel-actions">
          <slot name="headerActions" />
        </div>
      </div>
    </template>
    <slot />
  </el-card>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: ''
  },
  description: {
    type: String,
    default: ''
  }
})
</script>

<style lang="scss" scoped>
.admin-panel-card {
  border-radius: 18px;
  border-color: #e6edf5;
  box-shadow: 0 14px 34px rgba(15, 53, 87, 0.06);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.panel-copy {
  min-width: 0;
}

.panel-title {
  margin: 0;
  font-size: 16px;
  color: #102a43;
}

.panel-description {
  margin: 6px 0 0;
  color: #6b7d8f;
  line-height: 1.5;
}

.panel-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}
</style>
