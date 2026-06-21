<template>
  <div class="user-hub-section-nav" role="tablist" aria-orientation="vertical">
    <button
      v-for="item in items"
      :key="item.key"
      type="button"
      class="section-button"
      :class="{ 'is-active': item.key === modelValue }"
      @click="handleSelect(item.key)"
    >
      <span class="section-icon">
        <el-icon v-if="item.icon">
          <component :is="item.icon" />
        </el-icon>
      </span>
      <span class="section-copy">
        <strong>{{ item.title }}</strong>
        <small v-if="item.description">{{ item.description }}</small>
      </span>
      <el-icon class="section-arrow"><ArrowRight /></el-icon>
    </button>
  </div>
</template>

<script setup>
import { ArrowRight } from '@element-plus/icons-vue'

defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  items: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

function handleSelect(key) {
  emit('update:modelValue', key)
  emit('change', key)
}
</script>

<style scoped>
.user-hub-section-nav {
  display: grid;
  gap: 10px;
}

.section-button {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr) 16px;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 14px 16px;
  border: 1px solid #e4ebf2;
  border-radius: 16px;
  background: #f8fbfe;
  color: #102a43;
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.section-button:hover {
  transform: translateY(-1px);
  border-color: rgba(37, 99, 235, 0.28);
  box-shadow: 0 12px 26px rgba(15, 53, 87, 0.08);
}

.section-button.is-active {
  border-color: rgba(37, 99, 235, 0.36);
  background: linear-gradient(180deg, rgba(239, 246, 255, 0.94) 0%, rgba(255, 255, 255, 1) 100%);
  box-shadow: 0 16px 32px rgba(15, 53, 87, 0.1);
}

.section-icon {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: #eaf2fb;
  color: #24548b;
  font-size: 18px;
}

.section-button.is-active .section-icon {
  background: #2563eb;
  color: #ffffff;
}

.section-copy {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.section-copy strong {
  font-size: 14px;
}

.section-copy small {
  color: #6b7d8f;
  line-height: 1.5;
}

.section-arrow {
  color: #8aa0b6;
}
</style>
