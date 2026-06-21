<template>
  <div class="portal-event-section-header" :class="alignClass">
    <p v-if="eyebrow" class="section-eyebrow">{{ eyebrow }}</p>
    <div class="section-copy">
      <div>
        <h2 class="section-title">{{ title }}</h2>
        <p v-if="description" class="section-description">{{ description }}</p>
      </div>
      <div v-if="$slots.actions" class="section-actions">
        <slot name="actions" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  eyebrow: {
    type: String,
    default: ''
  },
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    default: ''
  },
  align: {
    type: String,
    default: 'between'
  }
})

const alignClass = computed(() => `align-${props.align}`)
</script>

<style lang="scss" scoped>
@use '../../styles/variables.scss' as *;

.portal-event-section-header {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.section-eyebrow {
  margin: 0;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: $accent-color;
}

.section-copy {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 18px;
}

.section-title {
  margin: 0;
  font-size: clamp(1.55rem, 2vw, 2.15rem);
  color: $text-primary;
}

.section-description {
  margin: 10px 0 0;
  max-width: 720px;
  font-size: 14px;
  line-height: 1.8;
  color: $text-secondary;
}

.section-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: flex-end;
}

.align-start .section-copy {
  align-items: flex-start;
}

@media (max-width: $breakpoint-md) {
  .section-copy {
    flex-direction: column;
    align-items: flex-start;
  }

  .section-actions {
    justify-content: flex-start;
  }
}
</style>
