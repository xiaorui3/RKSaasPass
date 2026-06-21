<template>
  <dl class="user-hub-key-value-list">
    <div
      v-for="item in normalizedItems"
      :key="item.label"
      class="kv-row"
      :class="item.tone ? `is-${item.tone}` : ''"
    >
      <dt>{{ item.label }}</dt>
      <dd>{{ item.value }}</dd>
    </div>
  </dl>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  items: {
    type: Array,
    default: () => []
  }
})

const normalizedItems = computed(() =>
  props.items.filter(
    (item) =>
      item &&
      item.label &&
      item.value !== undefined &&
      item.value !== null &&
      item.value !== ''
  )
)
</script>

<style scoped>
.user-hub-key-value-list {
  display: grid;
  gap: 12px;
  margin: 0;
}

.kv-row {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid #e6edf5;
  border-radius: 16px;
  background: #f8fbfe;
}

.kv-row dt {
  margin: 0;
  color: #6b7d8f;
  font-size: 12px;
}

.kv-row dd {
  margin: 0;
  color: #102a43;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.6;
  word-break: break-word;
}

.kv-row.is-warning {
  background: #fff9ef;
}

.kv-row.is-success {
  background: #f3fbf6;
}

.kv-row.is-danger {
  background: #fff6f4;
}
</style>
