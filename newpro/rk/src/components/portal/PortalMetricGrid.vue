<template>
  <div class="portal-metric-grid">
    <article v-for="item in items" :key="`${item.label}-${item.value}`" class="metric-panel">
      <p class="metric-label">{{ item.label }}</p>
      <p class="metric-value">{{ item.value }}</p>
      <p v-if="item.detail" class="metric-detail">{{ item.detail }}</p>
    </article>
  </div>
</template>

<script setup>
defineProps({
  items: {
    type: Array,
    default: () => []
  }
})
</script>

<style lang="scss" scoped>
@use '../../styles/variables.scss' as *;

.portal-metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 14px;
}

.metric-panel {
  min-height: 118px;
  padding: 18px;
  border-radius: 18px;
  background: $bg-white;
  border: 1px solid $border-light;
  box-shadow: $shadow-sm;
}

.metric-label {
  margin: 0 0 10px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: $text-light;
}

.metric-value {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: $text-primary;
}

.metric-detail {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: $text-secondary;
}
</style>
